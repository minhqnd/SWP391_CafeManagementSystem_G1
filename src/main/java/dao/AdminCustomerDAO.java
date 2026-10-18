package dao;

import model.AdminCustomerSummary;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Truy vấn dành riêng cho màn hình quản lý khách hàng. */
public class AdminCustomerDAO extends BaseDAO {

    public List<AdminCustomerSummary> findAll() throws SQLException, ClassNotFoundException {
        String sql = "SELECT u.id, u.full_name, u.phone, u.email, u.birthday, u.address, u.status, "
                + "u.total_points, u.created_at, COUNT(o.id) AS order_count, "
                + "COALESCE(SUM(CASE WHEN o.status = 'COMPLETED' THEN o.final_amount ELSE 0 END), 0) AS total_spent "
                + "FROM users u LEFT JOIN orders o ON o.customer_id = u.id "
                + "WHERE u.role = 'CUSTOMER' "
                + "GROUP BY u.id, u.full_name, u.phone, u.email, u.birthday, u.address, u.status, u.total_points, u.created_at "
                + "ORDER BY u.created_at DESC, u.id DESC";
        List<AdminCustomerSummary> customers = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AdminCustomerSummary customer = new AdminCustomerSummary();
                customer.setId(rs.getInt("id"));
                customer.setFullName(rs.getString("full_name"));
                customer.setPhone(rs.getString("phone"));
                customer.setEmail(rs.getString("email"));
                customer.setBirthday(rs.getObject("birthday", LocalDate.class));
                customer.setAddress(rs.getString("address"));
                customer.setStatus(rs.getString("status"));
                customer.setTotalPoints(rs.getInt("total_points"));
                customer.setOrderCount(rs.getInt("order_count"));
                customer.setTotalSpent(rs.getDouble("total_spent"));
                customer.setCreatedAt(rs.getObject("created_at", java.time.LocalDateTime.class));
                customers.add(customer);
            }
        }
        return customers;
    }

    public boolean phoneExists(String phone) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE phone = ? LIMIT 1")) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public int createCustomer(String fullName, String phone, String email, LocalDate birthday,
                              String address, int initialPoints) throws SQLException, ClassNotFoundException {
        String insertUser = "INSERT INTO users (full_name, phone, email, password_hash, birthday, address, role, status, position, base_salary, total_points) "
                + "VALUES (?, ?, ?, 'pending_hash', ?, ?, 'CUSTOMER', 'ACTIVE', NULL, 0, ?)";
        String insertPoints = "INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type) VALUES (?, NULL, ?, 'EARN')";
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            int customerId;
            try (PreparedStatement ps = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, fullName);
                ps.setString(2, phone);
                ps.setString(3, email);
                if (birthday == null) ps.setNull(4, java.sql.Types.DATE); else ps.setDate(4, Date.valueOf(birthday));
                ps.setString(5, address);
                ps.setInt(6, initialPoints);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã khách hàng mới.");
                    customerId = keys.getInt(1);
                }
            }
            if (initialPoints > 0) {
                try (PreparedStatement ps = conn.prepareStatement(insertPoints)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, initialPoints);
                    ps.executeUpdate();
                }
            }
            conn.commit();
            return customerId;
        } catch (SQLException | ClassNotFoundException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) { }
            throw e;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) { }
        }
    }

    public boolean updateStatus(int id, String status) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE users SET status = ? WHERE id = ? AND role = 'CUSTOMER'";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }
}
