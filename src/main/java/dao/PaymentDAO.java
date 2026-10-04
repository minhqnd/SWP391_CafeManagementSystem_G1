package dao;

import context.DBContext;
import model.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * PaymentDAO - Xử lý nghiệp vụ thanh toán và quản lý giao dịch
 */
public class PaymentDAO extends BaseDAO {

    /**
     * Xử lý giao dịch thanh toán sử dụng JDBC Transaction.
     * Đảm bảo tính toàn vẹn (ACID):
     * 1. Thêm bản ghi mới vào bảng `payments`
     * 2. Cập nhật trạng thái của đơn hàng trong bảng `orders` sang 'CONFIRMED'
     * Nếu xảy ra bất kỳ lỗi nào, toàn bộ thao tác sẽ được rollback.
     *
     * @param payment Thông tin thanh toán cần xử lý
     * @return true nếu thanh toán và cập nhật thành công, false nếu thất bại
     */
    public boolean processPayment(Payment payment) {
        String insertPaymentSql = "INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at, payment_note) " +
                                  "VALUES (?, ?, ?, ?, ?, ?, ?)";

        String updateOrderStatusSql = "UPDATE orders SET status = 'CONFIRMED' WHERE id = ?";

        Connection conn = null;
        PreparedStatement psPayment = null;
        PreparedStatement psOrder = null;

        try {
            conn = DBContext.getConnection();

            // BƯỚC 1: Bật Transaction thủ công (tắt chế độ tự động commit)
            conn.setAutoCommit(false);

            // BƯỚC 2: Thêm bản ghi thanh toán
            psPayment = conn.prepareStatement(insertPaymentSql, Statement.RETURN_GENERATED_KEYS);
            psPayment.setInt(1, payment.getOrderId());
            psPayment.setString(2, payment.getPaymentMethod());
            psPayment.setString(3, payment.getTransactionCode());
            psPayment.setDouble(4, payment.getAmount());
            psPayment.setString(5, payment.getStatus() != null ? payment.getStatus() : "SUCCESS");
            psPayment.setTimestamp(6, payment.getPaidAt());
            psPayment.setString(7, payment.getPaymentNote());

            int paymentRows = psPayment.executeUpdate();
            if (paymentRows == 0) {
                // Không ghi được bản ghi thanh toán -> Rollback
                conn.rollback();
                return false;
            }

            // Lấy ID tự sinh của bản ghi Payment vừa thêm
            try (ResultSet rs = psPayment.getGeneratedKeys()) {
                if (rs.next()) {
                    payment.setId(rs.getInt(1));
                }
            }

            // BƯỚC 3: Cập nhật trạng thái đơn hàng sang CONFIRMED (Đã thanh toán / Chờ barista chuẩn bị)
            psOrder = conn.prepareStatement(updateOrderStatusSql);
            psOrder.setInt(1, payment.getOrderId());

            int orderRows = psOrder.executeUpdate();
            if (orderRows == 0) {
                // Không tìm thấy đơn hàng để cập nhật trạng thái -> Rollback
                conn.rollback();
                return false;
            }

            // BƯỚC 3.5: Nếu thanh toán thành công và khách hàng là thành viên đã đăng ký, tích lũy điểm Loyalty (SRS BR-12, BR-13)
            if ("SUCCESS".equalsIgnoreCase(payment.getStatus())) {
                String findCustomerSql = "SELECT customer_id FROM orders WHERE id = ?";
                try (PreparedStatement psFindCust = conn.prepareStatement(findCustomerSql)) {
                    psFindCust.setInt(1, payment.getOrderId());
                    try (ResultSet rsCust = psFindCust.executeQuery()) {
                        if (rsCust.next()) {
                            int customerId = rsCust.getInt("customer_id");
                            if (!rsCust.wasNull() && customerId > 0) {
                                int earnedPoints = (int) Math.floor(payment.getAmount() / 10000.0);
                                if (earnedPoints > 0) {
                                    String insertLoyaltySql = "INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type) " +
                                                              "VALUES (?, ?, ?, 'EARN')";
                                    try (PreparedStatement psLoyalty = conn.prepareStatement(insertLoyaltySql)) {
                                        psLoyalty.setInt(1, customerId);
                                        psLoyalty.setInt(2, payment.getOrderId());
                                        psLoyalty.setInt(3, earnedPoints);
                                        psLoyalty.executeUpdate();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BƯỚC 4: Hoàn tất toàn bộ giao dịch thành công -> Commit
            conn.commit();
            return true;

        } catch (Exception e) {
            // Xử lý khi có lỗi xảy ra -> Rollback toàn bộ transaction
            if (conn != null) {
                try {
                    System.err.println("Transaction failed! Rolling back changes for Order ID: " + payment.getOrderId());
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            // Đóng resources và khôi phục lại auto-commit cho connection
            try {
                if (psPayment != null) psPayment.close();
                if (psOrder != null) psOrder.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Lấy thông tin thanh toán thành công theo mã đơn hàng
     */
    public Payment getPaymentByOrderId(int orderId) {
        String sql = "SELECT * FROM payments WHERE order_id = ? AND status = 'SUCCESS' ORDER BY id DESC LIMIT 1";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Payment p = new Payment();
                    p.setId(rs.getInt("id"));
                    p.setOrderId(rs.getInt("order_id"));
                    p.setPaymentMethod(rs.getString("payment_method"));
                    p.setTransactionCode(rs.getString("transaction_code"));
                    p.setAmount(rs.getDouble("amount"));
                    p.setStatus(rs.getString("status"));
                    p.setPaidAt(rs.getTimestamp("paid_at"));
                    p.setPaymentNote(rs.getString("payment_note"));
                    return p;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
