import { Hono } from 'hono';
import type { Bindings } from '../types';
import { dashboardHtml } from '../views/dashboard';

const app = new Hono<{ Bindings: Bindings }>();

/**
 * API: Lấy 50 đơn hàng và 50 thông báo webhook gần nhất để hiển thị lên trang Dashboard
 * Đường dẫn: GET /api/transactions
 */
app.get('/api/transactions', async (c) => {
    // Lấy danh sách 50 đơn hàng mới nhất
    const orders = await c.env.DB.prepare(
        `SELECT id, app_id, order_code, transaction_code, amount, status, gateway_reference, signature, created_at, paid_at
         FROM orders ORDER BY id DESC LIMIT 50`
    ).all();

    // Lấy danh sách 50 log biến động số dư ngân hàng mới nhất
    const webhooks = await c.env.DB.prepare(
        `SELECT * FROM webhook_logs ORDER BY id DESC LIMIT 50`
    ).all();

    return c.json({
        orders: orders.results,
        webhooks: webhooks.results
    });
});

/**
 * Giao diện trực quan Dashboard (HTML) xem tại trang chủ Worker: http://localhost:8787/ hoặc tên miền cloudflare worker
 */
app.get('/', (c) => c.html(dashboardHtml));

export default app;
