package dao;

import model.LoyaltyTransaction;
import model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoyaltyTransactionDAO extends BaseDAO {

    public List<LoyaltyTransaction> findByCustomer(int customerId) throws SQLException {
        return findDetailed("WHERE lt.customer_id = ?", customerId, 100);
    }

    public List<LoyaltyTransaction> findRecent(int limit) throws SQLException {
        // HI07: . Ở đây em nhận yêu cầu lấy 50 giao dịch gần nhất.
        // .. Em chọn lịch sử toàn hệ thống, không lọc theo khách đang tra.
        // ... findDetailed("", null, limit) [HI02] trả list về Controller [HI08].
        return findDetailed("", null, limit);
    }

    private List<LoyaltyTransaction> findDetailed(String whereClause, Integer customerId, int limit)
            throws SQLException {
        // HI02: . Ở đây em lấy giao dịch cùng thông tin khách và đơn liên quan.
        // .. Em giữ cả giao dịch không gắn đơn và xếp mới nhất trước.
        // ... List về Controller → các dòng nhật ký [HI03]; tính số dư tại [HI06].
        String sql = "SELECT lt.id, lt.customer_id, lt.order_id, lt.points_change, "
                + "lt.transaction_type, lt.created_at, u.full_name, u.phone, u.total_points, o.order_code "
                + "FROM loyalty_transactions lt JOIN users u ON u.id = lt.customer_id "
                + "LEFT JOIN orders o ON o.id = lt.order_id "
                + whereClause + " ORDER BY lt.created_at DESC, lt.id DESC LIMIT ?";

        List<LoyaltyTransaction> rows = new ArrayList<>();
        Map<Integer, Integer> runningBalance = new HashMap<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            if (customerId != null) ps.setInt(index++, customerId);
            ps.setInt(index, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int ownerId = rs.getInt("customer_id");
                    int balanceAfter = runningBalance.computeIfAbsent(ownerId,
                            ignored -> getIntQuietly(rs, "total_points"));
                    LoyaltyTransaction row = new LoyaltyTransaction(
                            rs.getInt("id"), ownerId, rs.getInt("order_id"),
                            rs.getInt("points_change"), rs.getString("transaction_type"),
                            rs.getTimestamp("created_at"));
                    row.setCustomerName(rs.getString("full_name"));
                    row.setCustomerPhone(rs.getString("phone"));
                    row.setOrderCode(rs.getString("order_code"));
                    // HI06: . Ở đây em bắt đầu từ số dư hiện tại rồi lần ngược lịch sử từng khách.
                    // .. Mỗi giao dịch có số dư sau; số dư trước được suy ngược bằng số dư sau trừ điểm biến động.
                    // ... LoyaltyTransaction.balanceAfter → cột số dư sau [HI05].
                    row.setBalanceAfter(balanceAfter);
                    runningBalance.put(ownerId, balanceAfter - row.getPointsChange());
                    rows.add(row);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
        return rows;
    }

    private int getIntQuietly(ResultSet rs, String column) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            return 0;
        }
    }

    public long countCustomersWithPoints() throws SQLException {
        // ST11: . Ở đây em đếm số khách có điểm lớn hơn không.
        // .. Kết quả là số khách có điểm, không phải số điểm.
        // ... Trả số đếm về Controller → ô thống kê [ST01].
        return queryLong("SELECT COUNT(*) FROM users WHERE role='CUSTOMER' AND total_points > 0");
    }

    public long getTotalPoints() throws SQLException {
        // ST12: . Ở đây em cộng số dư điểm của tất cả khách.
        // .. Chưa có dữ liệu thì kết quả bằng không.
        // ... Trả tổng về Controller → ô thống kê [ST02].
        return queryLong("SELECT COALESCE(SUM(total_points),0) FROM users WHERE role='CUSTOMER'");
    }

    public long countTransactions() throws SQLException {
        // ST14: . Ở đây em đếm các dòng lịch sử điểm.
        // .. Bao gồm tích, đổi, hoàn và điều chỉnh.
        // ... Trả số đếm về Controller → ô thống kê [ST04].
        return queryLong("SELECT COUNT(*) FROM loyalty_transactions");
    }

    private long queryLong(String sql) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
    }

    public User findCustomerByPhone(String phone) throws SQLException {
        // CU03: . Ở đây em tìm khách bằng số điện thoại sau khi bỏ dấu cách, chấm và gạch ngang.
        // .. Tìm thấy thì trả thông tin khách; không có thì trả kết quả rỗng.
        // ... User về Controller [CU02] → thẻ khách [CU04]; không tìm thấy đi vào [CU08].
        String sql = "SELECT id, full_name, phone, email, role, position, status, total_points "
                + "FROM users WHERE role='CUSTOMER' AND phone=? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizePhone(phone));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapCustomer(rs) : null;
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
    }

    public User findDefaultCustomer() throws SQLException {
        // CU06: . Ở đây em chọn sẵn khách khi Admin chưa nhập số điện thoại.
        // .. Em lấy khách nhiều điểm nhất; bằng điểm thì lấy mã nhỏ hơn.
        // ... User về doGet() [CU02] → giao diện [CU04].
        String sql = "SELECT id, full_name, phone, email, role, position, status, total_points "
                + "FROM users WHERE role='CUSTOMER' ORDER BY total_points DESC, id LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapCustomer(rs) : null;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
    }

    private User mapCustomer(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("full_name"), rs.getString("phone"),
                rs.getString("email"), rs.getString("role"), rs.getString("position"),
                rs.getString("status"), "", "", rs.getInt("total_points"));
    }

    public CustomerActivity getCustomerActivity(int customerId) throws SQLException {
        // CU15: . Ở đây em tính chi tiêu từ các đơn đã hoàn thành của khách.
        // .. Tổng tiền gồm phí giao; ngày gần nhất lấy ngày tạo đơn mới nhất ở mọi trạng thái.
        // ... CustomerActivity về doGet() → giao diện [CU05].
        String sql = "SELECT COALESCE(SUM(CASE WHEN status='COMPLETED' THEN final_amount ELSE 0 END),0) total_spent, "
                + "MAX(created_at) last_order_at FROM orders WHERE customer_id=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CustomerActivity(rs.getBigDecimal("total_spent"), rs.getTimestamp("last_order_at"));
                }
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
        return new CustomerActivity(BigDecimal.ZERO, null);
    }

    public boolean adjustPoints(int customerId, int pointsChange) throws SQLException {
        // LP04: . Ở đây em khóa số dư khách để tránh hai yêu cầu cùng sửa trên điểm cũ.
        // .. Em lưu điểm và lịch sử cùng nhau; lỗi thì hoàn tác cả hai, không để số dư âm.
        // ... Kết quả về Controller để báo thành công hoặc lỗi [LP05].
        if (pointsChange == 0) throw new IllegalArgumentException("Số điểm điều chỉnh phải khác 0.");
        String lockSql = "SELECT total_points FROM users WHERE id=? AND role='CUSTOMER' FOR UPDATE";
        String updateSql = "UPDATE users SET total_points=? WHERE id=?";
        String insertSql = "INSERT INTO loyalty_transactions(customer_id,order_id,points_change,transaction_type) "
                + "VALUES (?,NULL,?,'ADJUST')";
        try (Connection conn = getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int current;
                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setInt(1, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy khách hàng.");
                        current = rs.getInt(1);
                    }
                }
                long next = (long) current + pointsChange;
                if (next < 0) throw new IllegalArgumentException("Không thể trừ nhiều hơn số điểm hiện có.");
                if (next > Integer.MAX_VALUE) throw new IllegalArgumentException("Số điểm vượt giới hạn cho phép.");
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, (int) next);
                    ps.setInt(2, customerId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, pointsChange);
                    ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException) throw (SQLException) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new SQLException(e);
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
    }

    public boolean earnForCompletedOrder(int customerId, int orderId, int points) throws SQLException {
        // EA00: . Ở đây em giữ một hàm cộng điểm riêng nhưng hiện chưa có chỗ gọi hàm này trong luồng đang chạy.
        // .. Khi trình bày, em lần theo lời gọi thật của thao tác đang demo.
        // Tham khảo: QR hoặc COD gọi PaymentDAO.earnPointsOnce() [EA03]; tiền mặt tại quầy xử lý trong [CT02].
        if (points <= 0) return false;
        String existsSql = "SELECT 1 FROM loyalty_transactions WHERE order_id=? AND transaction_type='EARN'";
        String updateSql = "UPDATE users SET total_points=total_points+? WHERE id=? AND role='CUSTOMER'";
        String insertSql = "INSERT INTO loyalty_transactions(customer_id,order_id,points_change,transaction_type) "
                + "VALUES (?,?,?,'EARN')";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement exists = conn.prepareStatement(existsSql)) {
                exists.setInt(1, orderId);
                try (ResultSet rs = exists.executeQuery()) {
                    if (rs.next()) return false;
                }
                try (PreparedStatement update = conn.prepareStatement(updateSql);
                     PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    update.setInt(1, points);
                    update.setInt(2, customerId);
                    if (update.executeUpdate() != 1) throw new SQLException("Không tìm thấy khách hàng.");
                    insert.setInt(1, customerId);
                    insert.setInt(2, orderId);
                    insert.setInt(3, points);
                    insert.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException) throw (SQLException) e;
                throw new SQLException(e);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s.-]", "").trim();
    }

    public static final class CustomerActivity {
        private final BigDecimal totalSpent;
        private final Timestamp lastOrderAt;

        public CustomerActivity(BigDecimal totalSpent, Timestamp lastOrderAt) {
            this.totalSpent = totalSpent != null ? totalSpent : BigDecimal.ZERO;
            this.lastOrderAt = lastOrderAt;
        }

        public BigDecimal getTotalSpent() { return totalSpent; }
        public Timestamp getLastOrderAt() { return lastOrderAt; }
    }
}
