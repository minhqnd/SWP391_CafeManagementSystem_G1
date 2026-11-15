import { Hono } from 'hono';
import { cors } from 'hono/cors';
import type { Bindings } from './types';
import orders from './routes/orders';
import webhooks from './routes/webhooks';
import dashboard from './routes/dashboard';

export type { Bindings } from './types';

// Khởi tạo ứng dụng Hono (Web framework siêu nhẹ chạy trên Cloudflare Workers)
const app = new Hono<{ Bindings: Bindings }>();

// Cấu hình CORS để cho phép Server Java và trình duyệt gọi API
app.use('/*', cors({
    origin: '*',
    allowMethods: ['GET', 'POST', 'OPTIONS'],
    allowHeaders: ['Content-Type', 'Authorization'],
}));

// API kiểm tra trạng thái hoạt động của hệ thống (Health Check)
app.get('/api/health', (c) => {
    return c.json({
        status: 'ok',
        service: 'payment-gateway',
        timestamp: new Date().toISOString()
    });
});

// Gắn các nhóm route xử lý nghiệp vụ vào ứng dụng chính:
// 1. Quản lý đơn hàng (đăng ký đơn, kiểm tra trạng thái thanh toán)
app.route('/', orders);

// 2. Nhận thông báo biến động số dư ngân hàng từ SePay
app.route('/', webhooks);

// 3. Giao diện trực quan Dashboard xem lịch sử đơn hàng và log chuyển khoản
app.route('/', dashboard);

export default app;
