import { Hono } from 'hono';
import type { Bindings } from '../types';

const app = new Hono<{ Bindings: Bindings }>();

// Nhận Webhook từ SePay khi có biến động số dư ngân hàng
app.post('/api/webhooks/sepay', async (c) => {
    let payload: any;
    try {
        payload = await c.req.json();
    } catch {
        return c.json({ success: false, message: 'Invalid JSON' }, 400);
    }

    const sepayId = Number(payload.id || 0);
    const transferType = String(payload.transferType || '').trim().toLowerCase();
    const transferAmount = Math.round(Number(payload.transferAmount || 0));
    const content = String(payload.content || '').trim();
    const gateway = String(payload.gateway || '');
    const accountNumber = String(payload.accountNumber || '');
    const referenceCode = String(payload.referenceCode || `SEPAY-${sepayId}`);
    const transactionDate = String(payload.transactionDate || '');

    if (sepayId <= 0 || transferAmount <= 0) {
        return c.json({ success: false, message: 'Invalid transaction data' }, 400);
    }

    // Chỉ xử lý giao dịch tiền vào ('in')
    if (transferType !== 'in') {
        return c.json({ success: true, message: 'Ignored outgoing transfer' });
    }

    try {
        // 1. Lưu log webhook vào bảng webhook_logs (bỏ qua nếu trùng sepay_id)
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

        // 2. Lấy danh sách các đơn đang chờ thanh toán (PENDING) để dò mã đơn trong nội dung chuyển khoản
        const pendingOrders = await c.env.DB.prepare(
            `SELECT * FROM orders WHERE status = 'PENDING' ORDER BY id DESC LIMIT 100`
        ).all<any>();

        const upperContent = content.toUpperCase();
        // Tạo thêm bản nội dung đã bỏ dấu gạch ngang vì nhiều ngân hàng tự xóa dấu '-' khi chuyển khoản
        const compactContent = upperContent.replace(/[-_\s]/g, '');

        let matchedOrder: any = null;
        for (const ord of pendingOrders.results) {
            const code = String(ord.order_code || '').replace(/^#/, '').toUpperCase();
            const compactCode = code.replace(/[-_\s]/g, '');
            if (code && (upperContent.includes(code) || (compactCode.length >= 6 && compactContent.includes(compactCode)))) {
                matchedOrder = ord;
                break;
            }
        }

        // 3. Nếu tìm thấy đơn, tính tổng tiền khách đã chuyển cho mã đơn này (hỗ trợ chuyển nhiều lần cộng dồn)
        if (matchedOrder) {
            const matchedCode = String(matchedOrder.order_code || '').replace(/^#/, '').toUpperCase();
            const matchedCompact = matchedCode.replace(/[-_\s]/g, '');

            const sumRow = await c.env.DB.prepare(
                `SELECT SUM(transfer_amount) as total_paid
                 FROM webhook_logs
                 WHERE transfer_type = 'in' AND (UPPER(content) LIKE ? OR UPPER(content) LIKE ?)`
            ).bind(`%${matchedCode}%`, `%${matchedCompact}%`).first<any>();

            const totalPaid = Number(sumRow?.total_paid || 0);

            // Khi tổng các lần chuyển đã đủ tiền đơn hàng -> Cập nhật SUCCESS
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

        return c.json({ success: true });
    } catch (error: any) {
        console.error('Webhook error:', error);
        return c.json({ success: false, message: 'Database error' }, 500);
    }
});

export default app;
