import { Hono } from 'hono';
import type { Bindings } from '../types';

const app = new Hono<{ Bindings: Bindings }>();

/**
 * API: Nhận thông báo biến động số dư tài khoản ngân hàng từ dịch vụ SePay (Webhook)
 * Đường dẫn: POST /api/webhooks/sepay
 * Khi có khách quét mã QR chuyển khoản, SePay sẽ tự động gọi API này để báo tiền về
 */
app.post('/api/webhooks/sepay', async (c) => {
    // 1. Đọc dữ liệu JSON do SePay gửi sang
    let payload: any;
    try {
        payload = await c.req.json();
    } catch {
        return c.json({ success: false, message: 'Dữ liệu JSON không hợp lệ' }, 400);
    }

    // Trích xuất các trường thông tin cần thiết từ thông báo của SePay
    const sepayId = Number(payload.id || 0);
    const transferType = String(payload.transferType || '').trim().toLowerCase();
    const transferAmount = Math.round(Number(payload.transferAmount || 0));
    const content = String(payload.content || '').trim();
    const gateway = String(payload.gateway || '');
    const accountNumber = String(payload.accountNumber || '');
    const referenceCode = String(payload.referenceCode || `SEPAY-${sepayId}`);
    const transactionDate = String(payload.transactionDate || '');

    // Kiểm tra dữ liệu: Phải có mã giao dịch SePay và số tiền phải lớn hơn 0
    if (sepayId <= 0 || transferAmount <= 0) {
        return c.json({ success: false, message: 'Dữ liệu giao dịch không hợp lệ' }, 400);
    }

    // Quán cà phê chỉ quan tâm đến giao dịch tiền vào tài khoản ('in')
    // Nếu là giao dịch chuyển tiền đi ('out') thì hệ thống bỏ qua và xác nhận OK
    if (transferType !== 'in') {
        return c.json({ success: true, message: 'Đã bỏ qua giao dịch chuyển tiền đi' });
    }

    try {
        // ============================================================
        // BƯỚC 1: Lưu lịch sử giao dịch vào bảng webhook_logs
        // Dùng "ON CONFLICT(sepay_id) DO NOTHING" để nếu SePay gửi lặp
        // cùng một giao dịch thì cơ sở dữ liệu sẽ không bị ghi trùng lặp
        // ============================================================
        await c.env.DB.prepare(
            `INSERT INTO webhook_logs
             (sepay_id, gateway, account_number, transfer_type, transfer_amount, code, content, reference_code, transaction_date)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
             ON CONFLICT(sepay_id) DO NOTHING`
        ).bind(
            sepayId,
            gateway,
            accountNumber,
            transferType,
            transferAmount,
            payload.code || null,
            content,
            referenceCode,
            transactionDate
        ).run();

        // ============================================================
        // BƯỚC 2: Tìm đơn hàng tương ứng đang chờ thanh toán (PENDING)
        // ============================================================
        const pendingOrders = await c.env.DB.prepare(
            `SELECT * FROM orders WHERE status = 'PENDING' ORDER BY id DESC LIMIT 100`
        ).all<any>();

        // Chuẩn hóa nội dung chuyển khoản sang chữ hoa để so khớp không phân biệt hoa/thường
        const upperContent = content.toUpperCase();
        // Một số ứng dụng ngân hàng tự động xoá dấu gạch ngang '-' hoặc khoảng trắng,
        // nên ta tạo thêm bản nội dung đã rút gọn để đối chiếu linh hoạt
        const compactContent = upperContent.replace(/[-_\s]/g, '');

        let matchedOrder: any = null;

        // Quét từng đơn hàng đang PENDING xem mã đơn có nằm trong nội dung chuyển khoản không
        for (const ord of pendingOrders.results) {
            const rawCode = String(ord.order_code || '').replace(/^#/, '').toUpperCase();
            const compactCode = rawCode.replace(/[-_\s]/g, '');

            // Nếu nội dung chứa mã gốc (ví dụ "AUR-101") hoặc mã rút gọn (ví dụ "AUR101")
            const isMatchOriginal = rawCode.length > 0 && upperContent.includes(rawCode);
            const isMatchCompact = compactCode.length >= 4 && compactContent.includes(compactCode);

            if (isMatchOriginal || isMatchCompact) {
                matchedOrder = ord;
                break; // Đã tìm thấy đơn hàng tương ứng, dừng vòng lặp
            }
        }

        // ============================================================
        // BƯỚC 3: Nếu tìm thấy đơn hàng, kiểm tra số tiền đã chuyển
        // Hệ thống hỗ trợ trường hợp khách chuyển làm nhiều lần cộng dồn
        // ============================================================
        if (matchedOrder) {
            const targetCode = String(matchedOrder.order_code || '').replace(/^#/, '').toUpperCase();
            const targetCompact = targetCode.replace(/[-_\s]/g, '');

            // Tính tổng tất cả các lần chuyển tiền cho đơn này từ bảng webhook_logs
            const sumResult = await c.env.DB.prepare(
                `SELECT SUM(transfer_amount) as total_paid
                 FROM webhook_logs
                 WHERE transfer_type = 'in' AND (UPPER(content) LIKE ? OR UPPER(content) LIKE ?)`
            ).bind(`%${targetCode}%`, `%${targetCompact}%`).first<any>();

            const totalPaid = Number(sumResult?.total_paid || 0);

            // Khi tổng số tiền khách đã chuyển >= số tiền đơn hàng -> Cập nhật SUCCESS
            if (totalPaid >= Number(matchedOrder.amount)) {
                await c.env.DB.prepare(
                    `UPDATE orders
                     SET status = 'SUCCESS',
                         gateway_reference = ?,
                         raw_webhook = ?,
                         paid_at = CURRENT_TIMESTAMP
                     WHERE id = ? AND status = 'PENDING'`
                ).bind(referenceCode, JSON.stringify(payload), matchedOrder.id).run();
            }
        }

        // Trả kết quả thành công cho SePay để SePay biết đã nhận webhook
        return c.json({ success: true });
    } catch (error: any) {
        console.error('Lỗi khi xử lý SePay webhook:', error);
        return c.json({ success: false, message: 'Lỗi cơ sở dữ liệu' }, 500);
    }
});

export default app;
