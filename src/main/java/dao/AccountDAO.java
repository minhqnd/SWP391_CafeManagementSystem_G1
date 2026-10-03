package dao;

import context.DBContext;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AccountDAO {

    /**
     * Xác thực người dùng qua username và password
     * Trả về đối tượng User (chứa thông tin Role để lưu Session) nếu thành công,
     * null nếu thất bại
     */
    public User login(String username, String password) {
        // Query kết hợp 3 bảng: account, staff, customer để lấy đầy đủ thông tin Role
        // và Full Name
        String sql = "SELECT " +
                "    a.account_id, " +
                "    a.username, " +
                "    a.status, " +
                "    COALESCE(s.full_name, c.full_name) AS full_name, " +
                "    COALESCE(s.role, 'Customer') AS role " +
                "FROM account a " +
                "LEFT JOIN staff s ON a.account_id = s.account_id " +
                "LEFT JOIN customer c ON a.account_id = c.account_id " +
                "WHERE a.username = ? AND a.password_hash = ?";

        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password); // Tạm thời dùng raw text, sau này ghép SHA-256 thì hash trước khi truyền vào
                                       // đây

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");

                    // Nếu tài khoản bị khóa, bạn có thể quăng Exception hoặc xử lý tuỳ ý
                    // Tạm thời chỉ cho phép Active đăng nhập
                    if (!"Active".equalsIgnoreCase(status)) {
                        return null;
                    }

                    User u = new User();
                    u.setId(rs.getInt("account_id"));
                    u.setPhone(rs.getString("username"));
                    u.setFullName(rs.getString("full_name"));
                    u.setRole(rs.getString("role"));
                    u.setStatus(status);
                    return u;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
