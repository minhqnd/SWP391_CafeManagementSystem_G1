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

        return findDetailed("", null, limit);
    }

    // [TDLS02]
    // . Các hàm đọc lịch sử trong LoyaltyTransactionDAO lấy giao dịch kèm khách và đơn hàng, rồi tính số dư
    //   sau từng lần thay đổi. Danh sách được trả về doGet của AdminLoyaltyController để hiển thị bảng lịch
    //   sử.
    //
    // Tra cứu: Trả về AdminLoyaltyController.java / TDM02 sau findRecent hoặc hàm đọc tương ứng; hiển thị tại
    //          admin-loyalty.jsp / TDLS01. Bản ghi mới do điều chỉnh được tạo tại TDDC04.
    private List<LoyaltyTransaction> findDetailed(String whereClause, Integer customerId, int limit)
            throws SQLException {

        // [SQL12-01] Bảng lịch sử điểm: ghép khách, giao dịch và mã đơn, mới nhất trước.

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

                    // [SQL12-15] Cột Số dư sau: Java tính ngược từ số dư hiện tại theo lịch sử mới tới cũ.

                    int balanceAfter = runningBalance.computeIfAbsent(ownerId,
                            ignored -> getIntQuietly(rs, "total_points"));
                    LoyaltyTransaction row = new LoyaltyTransaction(
                            rs.getInt("id"), ownerId, rs.getInt("order_id"),
                            rs.getInt("points_change"), rs.getString("transaction_type"),
                            rs.getTimestamp("created_at"));
                    row.setCustomerName(rs.getString("full_name"));
                    row.setCustomerPhone(rs.getString("phone"));
                    row.setOrderCode(rs.getString("order_code"));

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

    // [TDTS01]
    // . Các hàm đếm và cộng trong LoyaltyTransactionDAO lấy số khách có điểm, tổng điểm và số giao dịch. Kết
    //   quả quay về doGet của AdminLoyaltyController để tính tiền quy đổi và hiện các số tổng trên màn hình.
    //
    // Tra cứu: Trở về AdminLoyaltyController.java / TDM02 sau từng lời gọi lấy số tổng, hiển thị tại
    //          admin-loyalty.jsp / TDM03.
    public long countCustomersWithPoints() throws SQLException {

        // [SQL12-02] Thẻ Khách hàng có điểm: đếm CUSTOMER có total_points lớn hơn 0.
        return queryLong("SELECT COUNT(*) FROM users WHERE role='CUSTOMER' AND total_points > 0");
    }

    public long getTotalPoints() throws SQLException {

        // [SQL12-03] Thẻ Tổng điểm lưu hành: cộng số dư hiện tại của các CUSTOMER.

        return queryLong("SELECT COALESCE(SUM(total_points),0) FROM users WHERE role='CUSTOMER'");
    }

    public long countTransactions() throws SQLException {

        // [SQL12-04] Thẻ Giao dịch điểm: đếm toàn bộ dòng lịch sử điểm.
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

    // [TDTK04]
    // . Hàm findCustomerByPhone trong LoyaltyTransactionDAO tìm khách theo số điện thoại trong bảng users. Em
    //   trả thông tin cá nhân và số điểm về doGet của AdminLoyaltyController để màn hình hiển thị đúng người
    //   vừa tra cứu.
    //
    // Tra tiếp: admin-loyalty.jsp / TDTK05.
    public User findCustomerByPhone(String phone) throws SQLException {

        // [SQL12-05] Nút Tìm kiếm theo số điện thoại: đọc thông tin và điểm của CUSTOMER.

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

        // [SQL12-06] Khách hiển thị ban đầu: chọn CUSTOMER có điểm cao nhất.

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

    // [TDCT01]
    // . Hàm getCustomerActivity trong LoyaltyTransactionDAO dùng mã khách để đọc tổng chi tiêu, số đơn và lần
    //   mua gần nhất. Thông tin được trả về doGet của AdminLoyaltyController rồi hiển thị cùng khách đang tra
    //   cứu.
    //
    // Tra cứu: Trở về AdminLoyaltyController.java / TDM02 ngay sau getCustomerActivity, hiển thị cùng khách
    //          tại admin-loyalty.jsp / TDTK05.
    public CustomerActivity getCustomerActivity(int customerId) throws SQLException {

        // [SQL12-07] Thông tin khách: tổng tiền đã mua và ngày đặt đơn gần nhất.

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

    // [TDDC04]
    // . Trong adjustPoints của LoyaltyTransactionDAO, em đọc số dư của khách rồi tính lại sau khi cộng hoặc
    //   trừ, kiểm tra số dư không âm và không vượt giới hạn. Em ghi số dư và lịch sử cùng một lần; chỉ khi cả
    //   hai ghi được mới trả thành công về AdminLoyaltyController, còn lỗi thì hủy thay đổi.
    //
    // Tra tiếp: AdminLoyaltyController.java / TDDC05.
    public boolean adjustPoints(int customerId, int pointsChange) throws SQLException {

        LoyaltyValidation.requireRange(customerId, "Khách hàng", 1, Integer.MAX_VALUE);
        if (pointsChange == 0 || pointsChange < -LoyaltyValidation.MAX_POINTS
                || pointsChange > LoyaltyValidation.MAX_POINTS) {
            throw new IllegalArgumentException("Số điểm điều chỉnh phải từ 1 đến 1.000.000.");
        }
        // [SQL12-08] Nút Cộng/Trừ điểm: đọc số dư hiện tại và giữ dòng đang cập nhật.

        String lockSql = "SELECT total_points FROM users WHERE id=? AND role='CUSTOMER' FOR UPDATE";
        // [SQL12-09] Nút Cộng/Trừ điểm: ghi số dư mới đã kiểm tra.

        String updateSql = "UPDATE users SET total_points=? WHERE id=?";
        // [SQL12-10] Nút Cộng/Trừ điểm: ghi giao dịch điều chỉnh ADJUST.

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
                if (next < 0) throw new IllegalArgumentException("Khách hàng chỉ có " + current + " điểm. Không thể trừ " + Math.abs((long) pointsChange) + " điểm.");
                if (next > Integer.MAX_VALUE) throw new IllegalArgumentException("Tổng điểm sau điều chỉnh không được vượt quá 2.147.483.647 điểm.");

                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, (int) next);
                    ps.setInt(2, customerId);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không cập nhật được số dư khách hàng.");
                }

                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, pointsChange);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không ghi được lịch sử điều chỉnh điểm.");
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
