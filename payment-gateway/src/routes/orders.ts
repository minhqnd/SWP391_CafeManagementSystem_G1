import { Hono } from 'hono';
import type { Bindings } from '../types';

const app = new Hono<{ Bindings: Bindings }>();

/**
 * API 1: Đăng ký đơn hàng mới cần thanh toán qua mã VietQR
 * Đường dẫn: POST /api/orders/register
 * Được gọi bởi: Backend Java khi khách hàng bấm Đặt hàng và chọn chuyển khoản
 */
app.post('/api/orders/register', async (c) => {
    try {
        // Lấy dữ liệu gửi lên từ body JSON
        const body = await c.req.json();
        const appId = String(body.appId || 'default').trim();
        const orderCode = String(body.orderCode || '').trim();
        const transactionCode = String(body.transactionCode || `TX-${orderCode}`).trim();
        const amount = Math.round(Number(body.amount || 0));

        // Kiểm tra dữ liệu hợp lệ: bắt buộc phải có mã đơn và số tiền lớn hơn 0
        if (!orderCode || amount <= 0) {
            return c.json({ success: false, message: 'Mã đơn hàng hoặc số tiền không hợp lệ' }, 400);
        }

        // Lưu đơn hàng vào bảng orders với trạng thái ban đầu là PENDING (chờ thanh toán)
        // Nếu đơn hàng này đã được đăng ký trước đó rồi thì bỏ qua không tạo trùng (ON CONFLICT DO NOTHING)
        await c.env.DB.prepare(
            `INSERT INTO orders (app_id, order_code, transaction_code, amount, status)
             VALUES (?, ?, ?, ?, 'PENDING')
             ON CONFLICT(app_id, order_code) DO NOTHING`
        ).bind(appId, orderCode, transactionCode, amount).run();

        // Trả kết quả đăng ký thành công về cho Server Java
        return c.json({
            success: true,
            orderCode: orderCode,
            transactionCode: transactionCode,
            amount: amount
        });
    } catch (error: any) {
        return c.json({ success: false, message: error.message || 'Lỗi hệ thống khi đăng ký đơn' }, 500);
    }
});

/**
 * API 2: Kiểm tra trạng thái thanh toán của một đơn hàng
 * Đường dẫn: GET /api/orders/:orderCode/status
 * Được gọi bởi: Backend Java (hoặc trang thanh toán) để biết khách đã chuyển khoản hay chưa
 */
app.get('/api/orders/:orderCode/status', async (c) => {
    const rawCode = c.req.param('orderCode').trim();
    // Loại bỏ dấu # ở đầu mã đơn nếu có (ví dụ #AUR-101 -> AUR-101)
    const cleanCode = rawCode.replace(/^#/, '');

    // Tìm đơn hàng trong cơ sở dữ liệu
    const order: any = await c.env.DB.prepare(
        `SELECT * FROM orders WHERE order_code = ? OR order_code = ? LIMIT 1`
    ).bind(cleanCode, `#${cleanCode}`).first();

    // Nếu chưa tìm thấy đơn trong CSDL thì mặc định coi như đang chờ thanh toán
    if (!order) {
        return c.json({
            success: true,
            orderCode: rawCode,
            status: 'PENDING'
        });
    }

    // Trả về thông tin trạng thái đơn hàng (PENDING hoặc SUCCESS)
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

/**
 * API 3: Giả lập thanh toán thành công (chỉ dùng cho môi trường kiểm thử / chạy thử local)
 * Đường dẫn: POST /api/orders/:orderCode/simulate-pay
 */
app.post('/api/orders/:orderCode/simulate-pay', async (c) => {
    // Chỉ cho phép chạy khi cấu hình bật giả lập
    if (c.env.ALLOW_TEST_SIMULATION !== 'true') {
        return c.json({ success: false, message: 'Tính năng giả lập đang bị tắt' }, 403);
    }

    const rawCode = c.req.param('orderCode').trim();
    const cleanCode = rawCode.replace(/^#/, '');

    const order: any = await c.env.DB.prepare(
        `SELECT * FROM orders WHERE order_code = ? OR order_code = ? LIMIT 1`
    ).bind(cleanCode, `#${cleanCode}`).first();

    if (!order) {
        return c.json({ success: false, message: 'Không tìm thấy đơn hàng để giả lập' }, 404);
    }

    // Cập nhật ngay đơn hàng sang trạng thái SUCCESS
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
