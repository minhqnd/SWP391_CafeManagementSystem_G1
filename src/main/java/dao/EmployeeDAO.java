package dao;

import model.User;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EmployeeDAO - Quản lý tài khoản nội bộ (bảng users, role ADMIN / STAFF) cho màn /admin/employees.
 * Không đọc / trả về password_hash ra ngoài. Không xoá cứng nhân viên (work_schedules ON DELETE CASCADE).
 */
public class EmployeeDAO extends BaseDAO {

    public static final String LAST_ADMIN_MESSAGE = "Phải còn ít nhất một tài khoản Quản trị đang hoạt động.";

    private static final String EMPLOYEE_COLUMNS =
            "id, full_name, phone, email, role, position, status, birthday, address, hire_date";

    /** Danh sách nhân viên, position = null thì lấy tất cả chức vụ */
    public List<User> getEmployees(String position) throws SQLException, ClassNotFoundException {
        StringBuilder sql = new StringBuilder("SELECT " + EMPLOYEE_COLUMNS
                + " FROM users WHERE role IN ('ADMIN', 'STAFF') ");
        if (position != null) {
            sql.append("AND position = ? ");
        }
        sql.append("ORDER BY CASE role WHEN 'ADMIN' THEN 1 ELSE 2 END, "
                + "CASE position WHEN 'MANAGER' THEN 1 WHEN 'BARISTA' THEN 2 WHEN 'CASHIER' THEN 3 "
                + "WHEN 'SHIPPER' THEN 4 ELSE 5 END, full_name");

        List<User> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (position != null) {
                ps.setString(1, position);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapEmployee(rs));
                }
            }
        }
        return list;
    }

    /**
     * Thống kê trên toàn bộ nhân viên (không tính khách hàng):
     * total, active, notActive (INACTIVE + LOCKED), admins (số tài khoản Quản trị)
     */
    public Map<String, Object> getStats() throws SQLException, ClassNotFoundException {
        String sql = "SELECT COUNT(*) AS total, "
                + "COALESCE(SUM(status = 'ACTIVE'), 0) AS active, "
                + "COALESCE(SUM(status <> 'ACTIVE'), 0) AS not_active, "
                + "COALESCE(SUM(role = 'ADMIN'), 0) AS admins "
                + "FROM users WHERE role IN ('ADMIN', 'STAFF')";
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            stats.put("total", rs.getInt("total"));
            stats.put("active", rs.getInt("active"));
            stats.put("notActive", rs.getInt("not_active"));
            stats.put("admins", rs.getInt("admins"));
        }
        return stats;
    }

    /** Nhân viên theo id - trả về null nếu không có hoặc là tài khoản khách hàng */
    public User getEmployeeById(int id) throws SQLException, ClassNotFoundException {
        String sql = "SELECT " + EMPLOYEE_COLUMNS + " FROM users WHERE id = ? AND role IN ('ADMIN', 'STAFF')";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapEmployee(rs) : null;
            }
        }
    }

    /** SĐT đã thuộc tài khoản khác chưa (xét mọi vai trò, kể cả khách hàng) */
    public boolean phoneExists(String phone, int excludeId) throws SQLException, ClassNotFoundException {
        return valueExists("phone", phone, excludeId);
    }

    /** Email đã thuộc tài khoản khác chưa - email cũng dùng để đăng nhập nên không cho trùng */
    public boolean emailExists(String email, int excludeId) throws SQLException, ClassNotFoundException {
        return valueExists("email", email, excludeId);
    }

    private boolean valueExists(String column, String value, int excludeId)
            throws SQLException, ClassNotFoundException {
        String sql = "SELECT 1 FROM users WHERE " + column + " = ? AND id <> ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Tạo tài khoản nhân viên. Mật khẩu lưu đúng dạng mà UserDAO.login() đang so sánh với password_hash,
     * để tài khoản mới đăng nhập được ngay. Trả về id mới.
     */
    public int createEmployee(User employee, String password) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO users (full_name, phone, email, password_hash, birthday, address, role, "
                + "position, hire_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, employee.getFullName());
            ps.setString(2, employee.getPhone());
            setNullableString(ps, 3, employee.getEmail());
            ps.setString(4, password);
            setNullableDate(ps, 5, employee.getBirthday());
            setNullableString(ps, 6, employee.getAddress());
            ps.setString(7, employee.getRole());
            ps.setString(8, employee.getPosition());
            setNullableDate(ps, 9, employee.getHireDate());
            ps.setString(10, employee.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    /**
     * Cập nhật nhân viên trong 1 transaction. newPassword = null thì giữ nguyên password_hash.
     * Ném IllegalArgumentException nếu không tìm thấy nhân viên hoặc thao tác hạ quyền / khoá admin cuối cùng.
     */
    public void updateEmployee(User employee, String newPassword) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE users SET full_name = ?, phone = ?, email = ?, birthday = ?, address = ?, role = ?, "
                + "position = ?, hire_date = ?, status = ?"
                + (newPassword != null ? ", password_hash = ?" : "")
                + " WHERE id = ? AND role IN ('ADMIN', 'STAFF')";
        inTransaction(conn -> {
            checkAdminRemains(conn, employee.getId(), employee.getRole(), employee.getStatus());
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int i = 1;
                ps.setString(i++, employee.getFullName());
                ps.setString(i++, employee.getPhone());
                setNullableString(ps, i++, employee.getEmail());
                setNullableDate(ps, i++, employee.getBirthday());
                setNullableString(ps, i++, employee.getAddress());
                ps.setString(i++, employee.getRole());
                ps.setString(i++, employee.getPosition());
                setNullableDate(ps, i++, employee.getHireDate());
                ps.setString(i++, employee.getStatus());
                if (newPassword != null) {
                    ps.setString(i++, newPassword);
                }
                ps.setInt(i, employee.getId());
                ps.executeUpdate();
            }
        });
    }

    /** Khoá / mở khoá (đổi status) trong 1 transaction, có kiểm tra admin cuối cùng */
    public void updateStatus(int id, String status) throws SQLException, ClassNotFoundException {
        inTransaction(conn -> {
            checkAdminRemains(conn, id, null, status);
            try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET status = ? WHERE id = ?")) {
                ps.setString(1, status);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
        });
    }

    /**
     * Khoá dòng nhân viên cần sửa và các ADMIN đang hoạt động (SELECT ... FOR UPDATE) rồi kiểm tra:
     * nếu nhân viên đang là ADMIN ACTIVE mà sắp mất quyền / không còn ACTIVE thì phải còn ADMIN ACTIVE khác.
     * newRole = null nghĩa là giữ nguyên vai trò.
     */
    private void checkAdminRemains(Connection conn, int id, String newRole, String newStatus) throws SQLException {
        String sql = "SELECT id, role, status FROM users "
                + "WHERE id = ? OR (role = 'ADMIN' AND status = 'ACTIVE') FOR UPDATE";
        String currentRole = null;
        String currentStatus = null;
        int otherActiveAdmins = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getInt("id") == id) {
                        currentRole = rs.getString("role");
                        currentStatus = rs.getString("status");
                    } else {
                        otherActiveAdmins++;
                    }
                }
            }
        }
        if (currentRole == null || "CUSTOMER".equals(currentRole)) {
            throw new IllegalArgumentException("Không tìm thấy nhân viên.");
        }

        boolean isActiveAdmin = "ADMIN".equals(currentRole) && "ACTIVE".equals(currentStatus);
        boolean losesAdmin = (newRole != null && !"ADMIN".equals(newRole)) || !"ACTIVE".equals(newStatus);
        if (isActiveAdmin && losesAdmin && otherActiveAdmins == 0) {
            throw new IllegalArgumentException(LAST_ADMIN_MESSAGE);
        }
    }

    private interface TransactionWork {
        void run(Connection conn) throws SQLException;
    }

    private void inTransaction(TransactionWork work) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                work.run(conn);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private User mapEmployee(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("role"),
                rs.getString("position"),
                rs.getString("status"),
                dateText(rs.getDate("birthday")),
                rs.getString("address"));
        user.setHireDate(dateText(rs.getDate("hire_date")));
        return user;
    }

    private String dateText(Date date) {
        return date == null ? null : date.toString();
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isEmpty()) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    /** value dạng YYYY-MM-DD, rỗng thì lưu NULL */
    private void setNullableDate(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isEmpty()) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(value));
        }
    }
}
