package dao;

import model.BankTransaction;
import model.ShopSettings;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * BankTransactionDAO - Đọc/ghi dữ liệu đối soát đơn hàng, thanh toán chuyển khoản và lịch sử hoàn tiền.
 */
public class BankTransactionDAO extends BaseDAO {

    /** Thông tin đơn hàng dùng để đối soát số tiền chuyển khoản. */
    public static class OrderInfo {
        public int id;
        public String orderCode;
        public double finalAmount;
        public String status;
        public String paymentStatus;
        public String recipientName;

        public OrderInfo() {
        }

        public OrderInfo(int id, String orderCode, double finalAmount, String status) {
            this.id = id;
            this.orderCode = orderCode;
            this.finalAmount = finalAmount;
            this.status = status;
        }
    }

    /** Bản ghi hoàn tiền đã lưu trong bảng bank_transaction_refunds. */
    public static class RefundRecord {
        public double amount;
        public String note;
        public String refundedAt;
    }

    /** Lấy tên ngân hàng cấu hình, mặc định MBBank nếu chưa cài đặt. */
    public static String resolveBankName(ShopSettings settings) {
        if (settings != null && settings.getPaymentBankName() != null && !settings.getPaymentBankName().isEmpty()) {
            return settings.getPaymentBankName();
        }
        return "MBBank";
    }

    /** Lấy số tài khoản thụ hưởng cấu hình, mặc định 234556688 nếu chưa cài đặt. */
    public static String resolveBankAccount(ShopSettings settings) {
        if (settings != null && settings.getPaymentAccountNo() != null && !settings.getPaymentAccountNo().isEmpty()) {
            return settings.getPaymentAccountNo();
        }
        return "234556688";
    }

    /** Lưu hoặc cập nhật bản ghi hoàn tiền; nếu hoàn đơn đã hủy/đã khớp thì chuyển payment thành FAILED. */
    public void saveRefund(
            String txKey,
            long sepayId,
            String referenceCode,
            String orderCode,
            BigDecimal refundAmount,
            String refundNote,
            int adminId,
            String reconcileStatus
    ) throws SQLException, ClassNotFoundException {
        String upsertSql =
                "INSERT INTO bank_transaction_refunds (tx_key, sepay_id, reference_code, order_code, refund_amount, refund_note, refunded_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE refund_amount = VALUES(refund_amount), refund_note = VALUES(refund_note), " +
                "refunded_by = VALUES(refunded_by), refunded_at = CURRENT_TIMESTAMP";

        try (Connection conn = getConnection()) {
            ensureRefundTable(conn);

            try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
                ps.setString(1, txKey);
                ps.setLong(2, sepayId);
                ps.setString(3, referenceCode);
                ps.setString(4, orderCode);
                ps.setBigDecimal(5, refundAmount);
                ps.setString(6, refundNote);
                ps.setInt(7, adminId);
                ps.executeUpdate();
            }

            // Nếu hoàn cả giao dịch của đơn bị hủy hoặc đơn hoàn toàn bộ -> đánh dấu payment FAILED để không tính doanh thu
            if (!orderCode.isEmpty() && ("ORDER_CANCELLED".equals(reconcileStatus) || "MATCHED".equals(reconcileStatus))) {
                String cancelPaySql =
                        "UPDATE payments p JOIN orders o ON p.order_id = o.id " +
                        "SET p.status = 'FAILED', p.payment_note = CONCAT(COALESCE(p.payment_note, ''), ' [Đã hoàn tiền]') " +
                        "WHERE o.order_code = ? AND p.status = 'SUCCESS'";
                try (PreparedStatement ps = conn.prepareStatement(cancelPaySql)) {
                    ps.setString(1, orderCode);
                    ps.executeUpdate();
                }
            }
        }
    }

    /** Tải toàn bộ bản ghi đã hoàn tiền theo tx_key. */
    public Map<String, RefundRecord> findAllRefunds() {
        Map<String, RefundRecord> refundMap = new HashMap<>();
        String sql = "SELECT tx_key, refund_amount, refund_note, DATE_FORMAT(refunded_at, '%Y-%m-%d %H:%i') AS refunded_at " +
                     "FROM bank_transaction_refunds";

        try (Connection conn = getConnection()) {
            ensureRefundTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RefundRecord rec = new RefundRecord();
                    rec.amount = rs.getDouble("refund_amount");
                    rec.note = rs.getString("refund_note");
                    rec.refundedAt = rs.getString("refunded_at");
                    refundMap.put(rs.getString("tx_key"), rec);
                }
            }
        } catch (Exception e) {
            System.err.println("[BankTransactionDAO] Lỗi tải lịch sử hoàn tiền: " + e.getMessage());
        }
        return refundMap;
    }

    /** Tra cứu thông tin đơn hàng theo tập mã đơn để đối soát số tiền. */
    public Map<String, OrderInfo> findOrderSummaries(Set<String> orderCodes) {
        Map<String, OrderInfo> map = new HashMap<>();
        if (orderCodes == null || orderCodes.isEmpty()) {
            return map;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT o.id, o.order_code, o.final_amount, o.status, o.recipient_name, " +
                "COALESCE(p.status, 'PENDING') AS payment_status " +
                "FROM orders o " +
                "LEFT JOIN payments p ON p.order_id = o.id " +
                "WHERE o.order_code IN ("
        );
        for (int i = 0; i < orderCodes.size(); i++) {
            sql.append(i > 0 ? ",?" : "?");
        }
        sql.append(")");

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String code : orderCodes) {
                ps.setString(idx++, code);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderInfo info = new OrderInfo();
                    info.id = rs.getInt("id");
                    info.orderCode = rs.getString("order_code");
                    info.finalAmount = rs.getDouble("final_amount");
                    info.status = rs.getString("status");
                    info.paymentStatus = rs.getString("payment_status");
                    info.recipientName = rs.getString("recipient_name");
                    if (info.orderCode != null) {
                        map.put(info.orderCode.toUpperCase(Locale.ROOT), info);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[BankTransactionDAO] Lỗi tra cứu đơn hàng đối soát: " + e.getMessage());
        }
        return map;
    }

    /** Lấy tối đa 50 thanh toán chuyển khoản thành công trong DB cục bộ để bù nếu Gateway chưa có. */
    public List<BankTransaction> findLocalBankPayments(ShopSettings settings) {
        List<BankTransaction> list = new ArrayList<>();
        String sql = "SELECT p.id, p.order_id, p.amount, p.status, p.paid_at, p.gateway_reference, o.order_code " +
                     "FROM payments p " +
                     "JOIN orders o ON p.order_id = o.id " +
                     "WHERE p.payment_method = 'BANK_TRANSFER' AND p.status = 'SUCCESS' " +
                     "ORDER BY p.id DESC LIMIT 50";

        String bankName = resolveBankName(settings);
        String bankAcc = resolveBankAccount(settings);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long pId = rs.getLong("id");
                String orderCode = rs.getString("order_code");
                double amount = rs.getDouble("amount");
                Timestamp paidAt = rs.getTimestamp("paid_at");
                String ref = rs.getString("gateway_reference");

                BankTransaction tx = new BankTransaction();
                tx.setSepayId(99000000L + pId);
                tx.setGateway(bankName);
                tx.setAccountNumber(bankAcc);
                tx.setTransferType("in");
                tx.setTransferAmount(amount);
                tx.setContent(orderCode + " CHUYEN TIEN THANH TOAN DON HANG");
                tx.setReferenceCode(ref != null && !ref.isEmpty() ? ref : ("FT" + pId));
                tx.setTransactionDate(paidAt != null ? sdf.format(paidAt) : "");

                list.add(tx);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    /** Đảm bảo bảng bank_transaction_refunds tồn tại trước khi đọc/ghi. */
    private void ensureRefundTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS bank_transaction_refunds (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "tx_key VARCHAR(120) NOT NULL UNIQUE, " +
                "sepay_id BIGINT DEFAULT 0, " +
                "reference_code VARCHAR(150) DEFAULT NULL, " +
                "order_code VARCHAR(100) DEFAULT NULL, " +
                "refund_amount DECIMAL(12,2) NOT NULL, " +
                "refund_note VARCHAR(255) DEFAULT NULL, " +
                "refunded_by INT DEFAULT NULL, " +
                "refunded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }
}
