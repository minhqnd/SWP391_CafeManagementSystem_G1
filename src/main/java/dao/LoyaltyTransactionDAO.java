package dao;

import model.LoyaltyTransaction;
import model.User;
import utils.LoyaltyValidation;

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
        // HI02: . Yêu cầu danh sách toàn hệ thống, không lọc theo khách đang tra.
        // ... findDetailed() [HI03] JOIN dữ liệu và giới hạn số dòng.
        return findDetailed("", null, limit);
    }

    private List<LoyaltyTransaction> findDetailed(String whereClause, Integer customerId, int limit)
            throws SQLException {
        // HI03: . JOIN loyalty_transactions với users và LEFT JOIN orders, sắp mới nhất trước.
        // .. Giao dịch điều chỉnh không gắn đơn vẫn được giữ trong danh sách.
        // ... Tính số dư sau từng dòng tại [HI04], trả transactions về controller [HI01] → JSP [HI05].
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
                    // HI04: . Bắt đầu từ số dư hiện tại, suy ngược biến động mới hơn theo từng khách.
                    // .. balanceAfter là số dư sau giao dịch đang duyệt, không cập nhật users.
                    // ... Danh sách trả về JSP [HI05], cột số dư hiển thị tại [HI08].
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
        // ST01: . đếm số khách có điểm lớn hơn không.
        // .. Kết quả là số khách có điểm, không phải số điểm.
        // ... Trả số đếm về Controller → ô thống kê [ST05].
        return queryLong("SELECT COUNT(*) FROM users WHERE role='CUSTOMER' AND total_points > 0");
    }

    public long getTotalPoints() throws SQLException {
        // ST02: . cộng số dư điểm của tất cả khách.
        // .. Chưa có dữ liệu thì kết quả bằng không.
        // ... Trả tổng về Controller → ô thống kê [ST06].
        return queryLong("SELECT COALESCE(SUM(total_points),0) FROM users WHERE role='CUSTOMER'");
    }

    public long countTransactions() throws SQLException {
        // ST04: . đếm các dòng lịch sử điểm.
        // .. Bao gồm tích, đổi, hoàn và điều chỉnh.
        // ... Trả số đếm về Controller → ô thống kê [ST08].
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
        // CU05: . SELECT đúng phone đã chuẩn hóa và role=CUSTOMER.
        // .. Có dữ liệu trả User; không có trả null; lỗi SQL ném về controller.
        // ... doGet() [CU04] nhận khách, gọi getCustomerActivity() [CU07]; không tìm thấy báo [CU12].
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
        // CU06: . Chỉ khi mở trang không truyền phone mới chọn khách có điểm cao nhất, bằng điểm lấy id nhỏ nhất.
        // .. Không dùng khách mặc định khi Admin gửi SĐT rỗng hoặc sai.
        // ... Trả User về doGet() [CU04], rồi đọc hoạt động tại [CU07].
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
        // CU07: . Lấy tổng final_amount của đơn COMPLETED và ngày tạo đơn gần nhất của khách.
        // .. Tổng chi tiêu gồm phí giao; ngày gần nhất xét đơn ở mọi trạng thái.
        // ... doGet() quy đổi điểm ở [CU08], rồi gửi customerActivity sang ô [CU11].
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
        // LP09: . Kiểm tra lại id và giới hạn điểm trước khi mở transaction.
        // .. Điều chỉnh 0 hoặc ngoài +/-1.000.000 bị từ chối ngay.
        // ... Mở transaction và khóa dòng khách tại [LP10].
        LoyaltyValidation.requireRange(customerId, "Khách hàng", 1, Integer.MAX_VALUE);
        if (pointsChange == 0 || pointsChange < -LoyaltyValidation.MAX_POINTS
                || pointsChange > LoyaltyValidation.MAX_POINTS) {
            throw new IllegalArgumentException("Số điểm điều chỉnh phải từ 1 đến 1.000.000.");
        }
        String lockSql = "SELECT total_points FROM users WHERE id=? AND role='CUSTOMER' FOR UPDATE";
        String updateSql = "UPDATE users SET total_points=? WHERE id=?";
        String insertSql = "INSERT INTO loyalty_transactions(customer_id,order_id,points_change,transaction_type) "
                + "VALUES (?,NULL,?,'ADJUST')";
        try (Connection conn = getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                // LP10: . SELECT số dư theo id và role=CUSTOMER với FOR UPDATE.
                // .. Không có khách thì báo lỗi; khóa dòng giữ hai điều chỉnh không dùng cùng số dư cũ.
                // ... Tính số dư sau điều chỉnh và xét hợp lệ tại [LP11].
                int current;
                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setInt(1, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy khách hàng.");
                        current = rs.getInt(1);
                    }
                }
                // LP11: [SLBF12] Adjustment is valid?
                // . Tính số dư mới bằng long, yêu cầu từ 0 đến Integer.MAX_VALUE.
                // .. Yes: tiếp tục cập nhật; No: ném lỗi trước khi ghi và rollback tại [LP15].
                // ... Hợp lệ UPDATE số dư [LP12], rồi INSERT lịch sử [LP13].
                long next = (long) current + pointsChange;
                if (next < 0) throw new IllegalArgumentException("Không thể trừ nhiều hơn số điểm hiện có.");
                if (next > Integer.MAX_VALUE) throw new IllegalArgumentException("Số điểm vượt giới hạn cho phép.");
                // LP12: [SLBF12] Update point balance
                // . UPDATE total_points của khách đang khóa; yêu cầu đúng một dòng cập nhật.
                // .. Thay đổi chưa commit, vẫn phải ghi lịch sử thành công.
                // ... INSERT giao dịch ADJUST tại [LP13].
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, (int) next);
                    ps.setInt(2, customerId);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không cập nhật được số dư khách hàng.");
                }
                // LP13: [SLBF12] Record adjustment history
                // . INSERT customer_id, điểm có dấu, transaction_type=ADJUST và order_id=NULL.
                // .. Yêu cầu đúng một dòng lịch sử; lỗi thì rollback cả cập nhật số dư.
                // ... Commit tại [LP14] hoặc hoàn tác tại [LP15].
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, pointsChange);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không ghi được lịch sử điều chỉnh điểm.");
                }
                // LP14: . Commit cả số dư và dòng ADJUST trong cùng transaction.
                // .. Trả true sau khi commit, không ghi thêm giao dịch ở controller.
                // ... Controller đặt thông báo rồi redirect tại [LP16].
                conn.commit();
                return true;
            } catch (Exception e) {
                // LP15: [SLBF12] Keep point balance unchanged
                // . Lỗi kiểm tra hoặc ghi SQL thì rollback transaction.
                // .. Số dư và lịch sử trở về trước thao tác; không giữ phần UPDATE chưa đủ lịch sử.
                // ... Ném lỗi về controller để báo tại [MS04] và redirect [LP16].
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
