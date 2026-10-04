package dao;

import context.DBContext;
import model.Cart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PaymentDAO - Ghi nhận thanh toán chuyển khoản VietQR cho đơn hàng của khách
 */
public class PaymentDAO extends BaseDAO {

    public static final int FAILED = -1;

    /**
     * Khách xác nhận đã chuyển khoản. Trong cùng 1 transaction:
     * 1. Khóa đơn (phải đúng chủ, đang PENDING)
     * 2. Ghi payments: BANK_TRANSFER, SUCCESS, số tiền = final_amount của đơn
     * 3. Đơn chuyển sang CONFIRMED (Barista bắt đầu thấy đơn)
     * 4. Cộng điểm Aura: (tiền hàng - giảm giá) / 10.000đ -> loyalty_transactions (EARN) + users.total_points
     *
     * Trả về số điểm được cộng, hoặc FAILED nếu đơn không hợp lệ / đã thanh toán / lỗi.
     */
    public int payByBankTransfer(int orderId, int customerId) {
        String lockOrderSql = "SELECT order_code, total_amount, discount_amount, final_amount FROM orders " +
                              "WHERE id = ? AND customer_id = ? AND status = 'PENDING' FOR UPDATE";
        String insertPaymentSql = "INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at, payment_note) " +
                                  "VALUES (?, 'BANK_TRANSFER', ?, ?, 'SUCCESS', NOW(), ?)";
        String confirmOrderSql = "UPDATE orders SET status = 'CONFIRMED' WHERE id = ? AND status = 'PENDING'";
        String earnPointsSql = "INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type) " +
                               "VALUES (?, ?, ?, 'EARN')";
        String addUserPointsSql = "UPDATE users SET total_points = total_points + ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            String orderCode;
            double finalAmount;
            int points;
            try (PreparedStatement ps = conn.prepareStatement(lockOrderSql)) {
                ps.setInt(1, orderId);
                ps.setInt(2, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return FAILED;
                    }
                    orderCode = rs.getString("order_code");
                    finalAmount = rs.getDouble("final_amount");
                    double goodsAfterDiscount = rs.getDouble("total_amount") - rs.getDouble("discount_amount");
                    points = (int) Math.floor(goodsAfterDiscount / Cart.MONEY_PER_POINT);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertPaymentSql)) {
                ps.setInt(1, orderId);
                ps.setString(2, "VIETQR-" + orderCode);
                ps.setDouble(3, finalAmount);
                ps.setString(4, "Khách chuyển khoản VietQR, nội dung " + orderCode);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(confirmOrderSql)) {
                ps.setInt(1, orderId);
                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return FAILED;
                }
            }

            if (points > 0) {
                try (PreparedStatement ps = conn.prepareStatement(earnPointsSql)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, orderId);
                    ps.setInt(3, points);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(addUserPointsSql)) {
                    ps.setInt(1, points);
                    ps.setInt(2, customerId);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return points;
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            return FAILED;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
