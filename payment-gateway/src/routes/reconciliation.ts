import { Hono } from 'hono';
import type { Bindings } from '../types';

const app = new Hono<{ Bindings: Bindings }>();

app.get('/api/reconciliation/events', async (c) => {
    c.header('Cache-Control', 'no-store');
    const key = c.env.RECONCILIATION_API_KEY;
    if (!key || c.req.header('Authorization') !== `Bearer ${key}`) {
        return c.json({ success: false, message: 'Unauthorized' }, 401);
    }
    const afterId = Number(c.req.query('afterId') || '0');
    if (!Number.isSafeInteger(afterId) || afterId < 0) {
        return c.json({ success: false, message: 'Invalid cursor' }, 400);
    }
    const events = await c.env.DB.prepare(`SELECT b.id, b.sepay_id, o.app_id, o.order_code,
            b.gateway, b.account_number, b.transfer_type, b.received_amount, b.expected_amount,
            b.allocated_amount, b.excess_amount, b.reconciliation_status, b.content,
            b.reference_code, b.transaction_date, b.created_at
        FROM bank_transactions b LEFT JOIN orders o ON o.id = b.order_id
        WHERE b.id > ? ORDER BY b.id LIMIT 100`).bind(afterId).all<{ id: number }>();
    return c.json({ success: true, events: events.results,
        nextCursor: events.results.at(-1)?.id ?? afterId });
});

export default app;
