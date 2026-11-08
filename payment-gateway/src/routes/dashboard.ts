import { Hono } from 'hono';
import type { Bindings } from '../types';
import { dashboardHtml } from '../views/dashboard';

const app = new Hono<{ Bindings: Bindings }>();

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

app.get('/', (c) => c.html(dashboardHtml));

export default app;
