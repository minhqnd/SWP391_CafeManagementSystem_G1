import { Hono } from 'hono';
import type { Bindings } from '../types';
import { clean, computeHmacSha256Hex, buildCanonicalPayload } from '../lib/signatures';

const app = new Hono<{ Bindings: Bindings }>();

app.post('/api/orders/register', async (c) => {
    try {
        const body = await c.req.json<{
            appId?: string;
            orderCode: string;
            amount: number | string;
            transactionCode?: string;
            signature?: string;
        }>();

        const appId = clean(body.appId) || 'default';
        const orderCode = clean(body.orderCode);
        const transactionCode = clean(body.transactionCode) || `TX-${orderCode}-${Date.now()}`;
        const amount = Number(body.amount);

        if (!orderCode || !Number.isSafeInteger(amount) || amount <= 0) {
            return c.json({ success: false, message: 'Invalid orderCode or amount' }, 400);
        }

        const secret = c.env.SECRET_KEY || 'AURA-LOCAL-DEMO-SECRET';

        // Optional server-to-server signature verification (resilient against numeric formatting)
        const clientSig = c.req.header('X-Signature') || body.signature;
        if (clientSig) {
            const rawAmountStr = String(body.amount);
            const numAmount = typeof body.amount === 'string' ? parseFloat(body.amount) : body.amount;
            const candidates = [
                `${appId}|${orderCode}|${rawAmountStr}`,
                `${appId}|${orderCode}|${numAmount}`,
                `${appId}|${orderCode}|${Math.round(numAmount)}`,
                `${appId}|${orderCode}|${Number(numAmount).toFixed(1)}`,
                `${appId}|${orderCode}|${Number(numAmount).toFixed(2)}`
            ];
            let matched = false;
            for (const cand of candidates) {
                const candSig = await computeHmacSha256Hex(secret, cand);
                if (candSig.toLowerCase() === clientSig.toLowerCase()) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return c.json({ success: false, message: 'Invalid registration signature' }, 401);
            }
        }

        // Upsert order into D1
        await c.env.DB.prepare(
            `INSERT INTO orders (app_id, order_code, transaction_code, amount, status)
             VALUES (?, ?, ?, ?, 'PENDING')
             ON CONFLICT(app_id, order_code) DO NOTHING`
        ).bind(appId, orderCode, transactionCode, amount).run();

        const registered = await c.env.DB.prepare(
            'SELECT amount, transaction_code FROM orders WHERE app_id = ? AND order_code = ?'
        ).bind(appId, orderCode).first<{ amount: number; transaction_code: string }>();
        if (!registered || registered.amount !== amount || registered.transaction_code !== transactionCode) {
            return c.json({ success: false, message: 'Order already registered with different payment details' }, 409);
        }

        return c.json({
            success: true,
            message: 'Order registered successfully',
            orderCode,
            transactionCode,
            amount
        });
    } catch (e: any) {
        return c.json({ success: false, message: e.message || 'Internal error' }, 500);
    }
});

app.get('/api/orders/:orderCode/status', async (c) => {
    const orderCode = c.req.param('orderCode');
    const appId = c.req.query('appId') || 'default';

    const order: any = await c.env.DB.prepare(
        `SELECT * FROM orders WHERE (order_code = ? OR order_code = ?) LIMIT 1`
    ).bind(orderCode, `#${orderCode.replace(/^#/, '')}`).first();

    if (!order) {
        return c.json({
            success: true,
            orderCode,
            status: 'PENDING',
            message: 'Waiting for payment confirmation'
        }, 200);
    }

    return c.json({
        success: true,
        orderCode: order.order_code,
        transactionCode: order.transaction_code,
        amount: order.amount,
        status: order.status,
        signature: order.signature,
        gatewayReference: order.gateway_reference,
        paidAt: order.paid_at
    });
});

app.post('/api/orders/:orderCode/simulate-pay', async (c) => {
    if (c.env.ALLOW_TEST_SIMULATION !== 'true') {
        return c.json({ success: false, message: 'Simulation is disabled in production' }, 403);
    }

    const orderCode = c.req.param('orderCode');
    const order: any = await c.env.DB.prepare(
        `SELECT * FROM orders WHERE (order_code = ? OR order_code = ?) LIMIT 1`
    ).bind(orderCode, `#${orderCode.replace(/^#/, '')}`).first();

    if (!order) {
        return c.json({ success: false, message: 'Order not found' }, 404);
    }

    const secret = c.env.SECRET_KEY || 'AURA-LOCAL-DEMO-SECRET';
    const canonical = buildCanonicalPayload(order.order_code, order.transaction_code, order.amount, 'SUCCESS');
    const signature = await computeHmacSha256Hex(secret, canonical);

    await c.env.DB.prepare(
        `UPDATE orders SET
            status = 'SUCCESS',
            gateway_reference = ?,
            signature = ?,
            paid_at = CURRENT_TIMESTAMP
         WHERE id = ?`
    ).bind(`LOCAL-SIM-${Date.now()}`, signature, order.id).run();

    return c.json({
        success: true,
        message: 'Order simulated as SUCCESS',
        orderCode: order.order_code,
        transactionCode: order.transaction_code,
        amount: order.amount,
        status: 'SUCCESS',
        signature
    });
});

export default app;
