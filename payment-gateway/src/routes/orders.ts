import { Hono } from 'hono';
import type { Bindings } from '../types';

const app = new Hono<{ Bindings: Bindings }>();

// 1. Server Java đăng ký đơn hàng mới chờ thanh toán QR
app.post('/api/orders/register', async (c) => {
    try {
        const body = await c.req.json();
        const appId = String(body.appId || 'default').trim();
        const orderCode = String(body.orderCode || '').trim();
        const transactionCode = String(body.transactionCode || `TX-${orderCode}`).trim();
        const amount = Math.round(Number(body.amount || 0));

        if (!orderCode || amount <= 0) {
            return c.json({ success: false, message: 'Invalid orderCode or amount' }, 400);
        }

        // Lưu đơn vào bảng orders với trạng thái PENDING (nếu đã có thì giữ nguyên)
        await c.env.DB.prepare(
            `INSERT INTO orders (app_id, order_code, transaction_code, amount, status)
             VALUES (?, ?, ?, ?, 'PENDING')
             ON CONFLICT(app_id, order_code) DO NOTHING`
        ).bind(appId, orderCode, transactionCode, amount).run();

        return c.json({
            success: true,
            orderCode: orderCode,
            transactionCode: transactionCode,
            amount: amount
        });
    } catch (e: any) {
        return c.json({ success: false, message: e.message || 'Internal error' }, 500);
    }
});

// 2. Server Java hỏi trạng thái thanh toán của đơn hàng
app.get('/api/orders/:orderCode/status', async (c) => {
    const orderCode = c.req.param('orderCode').trim();
    const cleanCode = orderCode.replace(/^#/, '');

    const order: any = await c.env.DB.prepare(
        `SELECT * FROM orders WHERE order_code = ? OR order_code = ? LIMIT 1`
    ).bind(cleanCode, `#${cleanCode}`).first();

    if (!order) {
        return c.json({
            success: true,
            orderCode: orderCode,
            status: 'PENDING'
        });
    }

    return c.json({
        success: true,
        orderCode: order.order_code,
        transactionCode: order.transaction_code,
        amount: order.amount,
        status: order.status,
        gatewayReference: order.gateway_reference,
        paidAt: order.paid_at
    });
});

// 3. API giả lập thanh toán khi test ở local (chỉ chạy khi ALLOW_TEST_SIMULATION = true)
app.post('/api/orders/:orderCode/simulate-pay', async (c) => {
    if (c.env.ALLOW_TEST_SIMULATION !== 'true') {
        return c.json({ success: false, message: 'Simulation is disabled' }, 403);
    }

    const orderCode = c.req.param('orderCode').trim();
    const cleanCode = orderCode.replace(/^#/, '');

    const order: any = await c.env.DB.prepare(
        `SELECT * FROM orders WHERE order_code = ? OR order_code = ? LIMIT 1`
    ).bind(cleanCode, `#${cleanCode}`).first();

    if (!order) {
        return c.json({ success: false, message: 'Order not found' }, 404);
    }

    await c.env.DB.prepare(
        `UPDATE orders
         SET status = 'SUCCESS',
             gateway_reference = ?,
             paid_at = CURRENT_TIMESTAMP
         WHERE id = ?`
    ).bind(`LOCAL-SIM-${Date.now()}`, order.id).run();

    return c.json({
        success: true,
        orderCode: order.order_code,
        status: 'SUCCESS'
    });
});

export default app;
