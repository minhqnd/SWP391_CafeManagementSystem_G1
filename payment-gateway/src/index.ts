import { Hono } from 'hono';
import { cors } from 'hono/cors';

export type Bindings = {
    DB: D1Database;
    SECRET_KEY?: string;
    SEPAY_API_KEY?: string;
    SEPAY_WEBHOOK_SECRET?: string;
    ALLOW_TEST_SIMULATION?: string;
};

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

const app = new Hono<{ Bindings: Bindings }>();

// Enable CORS for all clients (including localhost Java and frontends)
app.use('/*', cors({
    origin: '*',
    allowMethods: ['GET', 'POST', 'OPTIONS'],
    allowHeaders: ['Content-Type', 'Authorization', 'X-Signature', 'X-SePay-Signature', 'X-SePay-Timestamp'],
}));

// ==========================================
// Crypto & Signature Helpers (Web Crypto API)
// ==========================================

async function computeHmacSha256Hex(secret: string, data: string): Promise<string> {
    const enc = new TextEncoder();
    const key = await crypto.subtle.importKey(
        'raw',
        enc.encode(secret),
        { name: 'HMAC', hash: 'SHA-256' },
        false,
        ['sign']
    );
    const signature = await crypto.subtle.sign('HMAC', key, enc.encode(data));
    const bytes = new Uint8Array(signature);
    let hex = '';
    for (let i = 0; i < bytes.length; i++) {
        hex += bytes[i].toString(16).padStart(2, '0');
    }
    return hex;
}

function clean(val: any): string {
    return val === null || val === undefined ? '' : String(val).trim();
}

/** Matches canonical format in PaymentService.java: orderCode|transactionCode|amount|status */
function buildCanonicalPayload(orderCode: string, transactionCode: string, amount: number | string, status: string): string {
    const formattedAmount = typeof amount === 'number'
        ? (Number.isInteger(amount) ? amount.toString() : amount.toString())
        : amount;
    return `${clean(orderCode)}|${clean(transactionCode)}|${clean(formattedAmount)}|${clean(status).toUpperCase()}`;
}

// ==========================================
// 1. Health Check
// ==========================================
app.get('/api/health', (c) => {
    return c.json({
        status: 'ok',
        service: 'payment-gateway',
        timestamp: new Date().toISOString()
    });
});

// ==========================================
// 2. Register Order (Server-to-Server from Java)
// ==========================================
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
        const amount = typeof body.amount === 'string' ? parseFloat(body.amount) : body.amount;

        if (!orderCode || isNaN(amount) || amount <= 0) {
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
             ON CONFLICT(app_id, order_code) DO UPDATE SET
                amount = excluded.amount,
                transaction_code = excluded.transaction_code,
                status = 'PENDING'`
        ).bind(appId, orderCode, transactionCode, amount).run();

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

// ==========================================
// 3. SePay Webhook Endpoint
// ==========================================
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

    // B. Idempotency Check (prevent duplicate processing)
    if (payload.id) {
        try {
            await c.env.DB.prepare(
                `INSERT INTO webhook_logs (sepay_id, gateway, account_number, transfer_type, transfer_amount, code, content, reference_code, transaction_date)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
            ).bind(
                payload.id,
                clean(payload.gateway),
                clean(payload.accountNumber),
                clean(payload.transferType),
                payload.transferAmount || 0,
                clean(payload.code),
                clean(payload.content),
                clean(payload.referenceCode),
                clean(payload.transactionDate)
            ).run();
        } catch (e: any) {
            // SePay ID already recorded -> Duplicate webhook, return 200 OK immediately
            return c.json({ success: true, message: 'Duplicate webhook acknowledged' });
        }
    }

    // C. Process only incoming money ('in')
    if (clean(payload.transferType).toLowerCase() !== 'in') {
        return c.json({ success: true, message: 'Ignored outgoing transfer' });
    }

    const transferAmount = Number(payload.transferAmount) || 0;
    const content = clean(payload.content);
    const code = clean(payload.code);

    // D. Locate Order in D1
    // First, try exact code match. If not found, try matching order_code contained inside content
    let order: any = null;

    if (code) {
        order = await c.env.DB.prepare(
            `SELECT * FROM orders WHERE order_code = ? AND status = 'PENDING' LIMIT 1`
        ).bind(code).first();
    }

    if (!order && content) {
        // Query pending orders to see if content contains any of the pending order codes
        const pendingOrders = await c.env.DB.prepare(
            `SELECT * FROM orders WHERE status = 'PENDING' ORDER BY id DESC LIMIT 50`
        ).all();

        for (const o of (pendingOrders.results as any[])) {
            // Resilient matching: handles uppercase/lowercase, hyphens, spaces and bank formatting
            const normContent = content.toUpperCase().replace(/[^A-Z0-9]/g, '');
            const normOrderCode = o.order_code.toUpperCase().replace(/[^A-Z0-9]/g, '');
            const plainCode = o.order_code.replace(/^[#\s]+/, '').toUpperCase();

            if (content.toUpperCase().includes(o.order_code.toUpperCase()) ||
                (plainCode.length >= 4 && content.toUpperCase().includes(plainCode)) ||
                (normOrderCode.length >= 4 && normContent.includes(normOrderCode))) {
                order = o;
                break;
            }
        }
    }

    if (!order) {
        // Fallback: Check if transfer narration explicitly mentions an order pattern (e.g. AURA...)
        const tokens = content.match(/[A-Za-z0-9_-]{6,30}/g) || [];
        const matchedCode = tokens.find(token => token.toUpperCase().startsWith('AURA'));
        if (matchedCode && transferAmount > 0) {
            const autoTx = `TX-${matchedCode}-${Date.now()}`;
            const canonical = buildCanonicalPayload(matchedCode, autoTx, transferAmount, 'SUCCESS');
            const signature = await computeHmacSha256Hex(secret, canonical);
            await c.env.DB.prepare(
                `INSERT INTO orders (app_id, order_code, transaction_code, amount, status, gateway_reference, signature, raw_webhook, paid_at)
                 VALUES ('aura_cafe', ?, ?, ?, 'SUCCESS', ?, ?, ?, CURRENT_TIMESTAMP)
                 ON CONFLICT(app_id, order_code) DO UPDATE SET
                    status = 'SUCCESS',
                    gateway_reference = excluded.gateway_reference,
                    signature = excluded.signature,
                    raw_webhook = excluded.raw_webhook,
                    paid_at = CURRENT_TIMESTAMP`
            ).bind(
                matchedCode,
                autoTx,
                transferAmount,
                clean(payload.referenceCode) || `SEPAY-${payload.id}`,
                signature,
                rawBody
            ).run();

            return c.json({ success: true, message: `Auto-confirmed order ${matchedCode}` });
        }

        // No pending order found, but webhook received successfully
        return c.json({ success: true, message: 'No matching pending order found' });
    }

    // E. Verify Amount & Mark Order SUCCESS
    if (transferAmount >= order.amount) {
        const canonical = buildCanonicalPayload(order.order_code, order.transaction_code, order.amount, 'SUCCESS');
        const signature = await computeHmacSha256Hex(secret, canonical);

        await c.env.DB.prepare(
            `UPDATE orders SET
                status = 'SUCCESS',
                gateway_reference = ?,
                signature = ?,
                raw_webhook = ?,
                paid_at = CURRENT_TIMESTAMP
             WHERE id = ?`
        ).bind(
            clean(payload.referenceCode) || `SEPAY-${payload.id}`,
            signature,
            rawBody,
            order.id
        ).run();
    }

    // Always respond 200 {"success": true} to SePay within 30s
    return c.json({ success: true });
});

// ==========================================
// 4. Polling Order Status (Client / Java)
// ==========================================
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

// ==========================================
// 5. Test Mode Simulator (Demo / Testing)
// ==========================================
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

// ==========================================
// 6. View Transactions / History
// ==========================================
app.get('/api/transactions', async (c) => {
    const orders = await c.env.DB.prepare(
        `SELECT id, app_id, order_code, transaction_code, amount, status, gateway_reference, signature, created_at, paid_at
         FROM orders ORDER BY id DESC LIMIT 50`
    ).all();

    const webhooks = await c.env.DB.prepare(
        `SELECT * FROM webhook_logs ORDER BY id DESC LIMIT 50`
    ).all();

    return c.json({
        orders: orders.results,
        webhooks: webhooks.results
    });
});

// ==========================================
// 7. Visual Gateway Dashboard
// ==========================================
app.get('/', (c) => {
    return c.html(`
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Universal Payment Gateway Hub</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #0f172a; color: #e2e8f0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        .card-custom { background: #1e293b; border: 1px solid #334155; border-radius: 12px; }
        .badge-success { background: #10b981; color: #fff; }
        .badge-pending { background: #f59e0b; color: #fff; }
        code { color: #38bdf8; background: #0f172a; padding: 2px 6px; border-radius: 4px; }
    </style>
</head>
<body class="p-4">
    <div class="container" style="max-w: 960px;">
        <div class="d-flex justify-content-between align-items-center mb-4 pb-3 border-bottom border-secondary">
            <div>
                <h1 class="h3 fw-bold mb-1 text-light">⚡ Payment Gateway & Webhook Hub</h1>
                <p class="text-secondary small mb-0">Hệ thống Cổng thanh toán & Điều phối Webhook SePay trên Cloudflare Workers</p>
            </div>
            <span class="badge bg-success px-3 py-2">Operational (Active)</span>
        </div>

        <div class="row g-3 mb-4">
            <div class="col-md-4">
                <div class="card-custom p-3">
                    <div class="text-secondary small">SePay Webhook URL</div>
                    <code class="d-block mt-1 text-truncate">POST /api/webhooks/sepay</code>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card-custom p-3">
                    <div class="text-secondary small">Register Order API</div>
                    <code class="d-block mt-1 text-truncate">POST /api/orders/register</code>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card-custom p-3">
                    <div class="text-secondary small">Polling Status API</div>
                    <code class="d-block mt-1 text-truncate">GET /api/orders/:code/status</code>
                </div>
            </div>
        </div>

        <div class="card-custom p-4">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h2 class="h5 fw-bold mb-0">Danh sách đơn hàng gần đây (Cloudflare D1)</h2>
                <button class="btn btn-sm btn-outline-info" onclick="location.reload()">Làm mới</button>
            </div>
            <div class="table-responsive">
                <table class="table table-dark table-hover mb-0" style="background: transparent;">
                    <thead>
                        <tr class="text-secondary">
                            <th>App</th>
                            <th>Mã đơn</th>
                            <th>Số tiền</th>
                            <th>Trạng thái</th>
                            <th>Mã tham chiếu</th>
                            <th>Thời gian</th>
                        </tr>
                    </thead>
                    <tbody id="ordersBody">
                        <tr><td colspan="6" class="text-center text-secondary py-3">Đang tải dữ liệu từ D1...</td></tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <script>
        fetch('/api/transactions')
            .then(res => res.json())
            .then(data => {
                const tbody = document.getElementById('ordersBody');
                if (!data.orders || data.orders.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="6" class="text-center text-secondary py-3">Chưa có giao dịch nào</td></tr>';
                    return;
                }
                tbody.innerHTML = data.orders.map(o => \`
                    <tr>
                        <td><span class="badge bg-secondary">\${o.app_id || 'default'}</span></td>
                        <td class="fw-bold text-info">\${o.order_code}</td>
                        <td>\${Number(o.amount).toLocaleString('vi-VN')} đ</td>
                        <td>
                            <span class="badge \${o.status === 'SUCCESS' ? 'badge-success' : 'badge-pending'}">
                                \${o.status}
                            </span>
                        </td>
                        <td><small class="text-secondary">\${o.gateway_reference || '-'}</small></td>
                        <td><small class="text-secondary">\${o.created_at}</small></td>
                    </tr>
                \`).join('');
            })
            .catch(() => {
                document.getElementById('ordersBody').innerHTML = '<tr><td colspan="6" class="text-center text-danger py-3">Lỗi tải dữ liệu</td></tr>';
            });
    </script>
</body>
</html>
    `);
});

export default app;
