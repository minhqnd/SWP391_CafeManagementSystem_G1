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

    /**
     * Khách chọn thanh toán khi nhận hàng (COD) cho đơn giao tận nơi. Trong cùng 1 transaction:
     * 1. Khóa đơn (đúng chủ, đang PENDING, là đơn giao tận nơi)
     * 2. Ghi payments: CASH, PENDING, mã COD-<mã đơn> (shipper thu tiền khi giao - màn Giao hàng)
     * 3. Đơn chuyển sang CONFIRMED để Barista pha chế. Điểm Aura được cộng khi shipper thu tiền.
     */
    public boolean chooseCashOnDelivery(int orderId, int customerId) {
        String lockOrderSql = "SELECT order_code, final_amount FROM orders WHERE id = ? AND customer_id = ? " +
                              "AND status = 'PENDING' AND order_type = 'ONLINE' AND delivery_address IS NOT NULL FOR UPDATE";
        String insertPaymentSql = "INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, payment_note) " +
                                  "VALUES (?, 'CASH', ?, ?, 'PENDING', 'Thanh toán tiền mặt khi nhận hàng (COD)')";
        String confirmOrderSql = "UPDATE orders SET status = 'CONFIRMED' WHERE id = ? AND status = 'PENDING'";

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String orderCode;
                double finalAmount;
                try (PreparedStatement ps = conn.prepareStatement(lockOrderSql)) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        orderCode = rs.getString("order_code");
                        finalAmount = rs.getDouble("final_amount");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(insertPaymentSql)) {
                    ps.setInt(1, orderId);
                    ps.setString(2, "COD-" + orderCode);
                    ps.setDouble(3, finalAmount);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(confirmOrderSql)) {
                    ps.setInt(1, orderId);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy thông tin thanh toán của đơn: ưu tiên bản ghi thành công, nếu chưa có thì lấy khoản COD đang chờ thu
     */
    public model.Payment getPaymentByOrderId(int orderId) {
        String sql = "SELECT * FROM payments WHERE order_id = ? AND status <> 'FAILED' " +
                     "ORDER BY status = 'SUCCESS' DESC, id DESC LIMIT 1";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    model.Payment p = new model.Payment();
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
