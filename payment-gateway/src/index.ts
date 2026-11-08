import { Hono } from 'hono';
import { cors } from 'hono/cors';
import type { Bindings } from './types';
import orders from './routes/orders';
import webhooks from './routes/webhooks';
import reconciliation from './routes/reconciliation';
import dashboard from './routes/dashboard';

export type { Bindings } from './types';

const app = new Hono<{ Bindings: Bindings }>();

app.use('/*', cors({
    origin: '*',
    allowMethods: ['GET', 'POST', 'OPTIONS'],
    allowHeaders: ['Content-Type', 'Authorization', 'X-Signature', 'X-SePay-Signature', 'X-SePay-Timestamp'],
}));

app.get('/api/health', (c) => {
    return c.json({
        status: 'ok',
        service: 'payment-gateway',
        timestamp: new Date().toISOString()
    });
});

app.route('/', orders);
app.route('/', webhooks);
app.route('/', reconciliation);
app.route('/', dashboard);

export default app;
