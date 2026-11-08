import { Hono } from 'hono';
import type { Bindings } from '../types';
import { computeHmacSha256Hex, buildCanonicalPayload } from '../lib/signatures';
import { paymentCodes, reconcile, validBankEvent, type RegisteredOrder } from '../services/reconciliation';

const app = new Hono<{ Bindings: Bindings }>();

interface SePayWebhookPayload {
    id: number;
    gateway: string;
    transactionDate: string;
    accountNumber: string;
    subAccount?: string;
    code?: string | null;
    content: string;
    transferType: 'in' | 'out' | string;
    description?: string;
    transferAmount: number;
    accumulated?: number;
    referenceCode?: string;
}

app.post('/api/webhooks/sepay', async (c) => {
    const rawBody = await c.req.text();
    let payload: SePayWebhookPayload;

    try {
        payload = JSON.parse(rawBody);
    } catch {
        return c.json({ success: false, message: 'Invalid JSON body' }, 400);
    }

    const secret = c.env.SECRET_KEY || 'AURA-LOCAL-DEMO-SECRET';
    const sepayHmacSecret = c.env.SEPAY_WEBHOOK_SECRET;
    const sepayApiKey = c.env.SEPAY_API_KEY;

    // A. Authenticate SePay Webhook
    if (sepayHmacSecret) {
        const signatureHeader = c.req.header('X-SePay-Signature') || '';
        const timestampHeader = c.req.header('X-SePay-Timestamp') || '0';
        const timestamp = parseInt(timestampHeader, 10);

        // Replay attack prevention (+/- 5 minutes)
        if (Math.abs(Math.floor(Date.now() / 1000) - timestamp) > 300) {
            return c.json({ success: false, message: 'Request expired' }, 401);
        }

        const stringToSign = `${timestamp}.${rawBody}`;
        const calculatedSig = 'sha256=' + await computeHmacSha256Hex(sepayHmacSecret, stringToSign);

        if (signatureHeader.toLowerCase() !== calculatedSig.toLowerCase()) {
            return c.json({ success: false, message: 'Invalid SePay signature' }, 401);
        }
    } else if (sepayApiKey) {
        const authHeader = c.req.header('Authorization') || '';
        if (authHeader !== `Apikey ${sepayApiKey}`) {
            return c.json({ success: false, message: 'Unauthorized API key' }, 401);
        }
    }

    if (!validBankEvent(payload) || payload.content.length > 2000) {
        return c.json({ success: false, message: 'Invalid bank event: positive ID and integer VND amount required' }, 400);
    }

    // Only incoming transfers belong to the payment reconciliation ledger.
    if (payload.transferType.trim().toLowerCase() !== 'in') {
        return c.json({ success: true });
    }

    try {
        const codes = paymentCodes(payload.code || '', payload.content);
        let initialStatus = 'UNMATCHED';
        let order: RegisteredOrder | null = null;
        if (codes.length) {
            const matches = await c.env.DB.prepare(
                `SELECT id, order_code, transaction_code, amount, status FROM orders
                 WHERE UPPER(LTRIM(order_code, '#')) IN (SELECT value FROM json_each(?)) LIMIT 2`
            ).bind(JSON.stringify(codes)).all<RegisteredOrder>();
            if (matches.results.length === 1) {
                order = matches.results[0];
                initialStatus = 'MATCH';
            } else if (matches.results.length > 1) {
                initialStatus = 'AMBIGUOUS';
            }
        }
        const signature = order ? await computeHmacSha256Hex(secret,
            buildCanonicalPayload(order.order_code, order.transaction_code, order.amount, 'SUCCESS')) : '';
        await reconcile(c.env.DB, payload, rawBody, order, initialStatus, signature);
        return c.json({ success: true });
    } catch (error) {
        console.error('Bank event reconciliation failed', { eventId: payload.id, error: String(error) });
        // A storage failure is retryable; never acknowledge it as a duplicate.
        return c.json({ success: false, message: 'Unable to persist bank event; retry required' }, 503);
    }
});

export default app;
