package dao;

import context.DBContext;
import model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * UserDAO - Tương tác dữ liệu người dùng (tài khoản, phân quyền)
 */
public class UserDAO extends BaseDAO {

    public User login(String identifier, String password) {
        String sql = "SELECT id, full_name, phone, email, role, position, status, birthday, address, tier_name, total_points " +
                     "FROM users " +
                     "WHERE (phone = ? OR email = ?) AND password_hash = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, identifier);
            ps.setString(2, identifier);
            ps.setString(3, password); // TODO: Hash password before comparing
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    
                    if (!"ACTIVE".equalsIgnoreCase(status)) {
                        return null; 
                    }

                    String birthdayStr = rs.getString("birthday");
                    if (birthdayStr != null && birthdayStr.length() >= 10) {
                        birthdayStr = birthdayStr.substring(0, 10);
                    } else if (birthdayStr == null) {
                        birthdayStr = "";
                    }

                    return new User(
                            rs.getInt("id"),
                            rs.getString("full_name"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("position"),
                            status,
                            birthdayStr,
                            rs.getString("address") != null ? rs.getString("address") : "",
                            rs.getString("tier_name"),
                            rs.getInt("total_points")
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean register(User user, String password) {
        String sql = "INSERT INTO users (full_name, phone, email, role, position, status, password_hash) " +
                     "VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getPosition());
            ps.setString(6, password); // TODO: Hash password before saving
            
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public boolean checkEmailExists(String email) {
        String sql = "SELECT id FROM users WHERE email = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updatePassword(String email, String newPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE email = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword); // TODO: Hash password
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public boolean updateUserProfile(int userId, String fullName, String phone, String email, String birthday, String address) {
        String sql = "UPDATE users SET full_name = ?, phone = ?, email = ?, birthday = ?, address = ? WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setString(3, email);
            
            if (birthday != null && !birthday.isEmpty()) {
                ps.setString(4, birthday);
            } else {
                ps.setNull(4, java.sql.Types.DATE);
            }
            
            ps.setString(5, address);
            ps.setInt(6, userId);
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public java.util.List<model.LoyaltyTransaction> getLoyaltyHistoryByUserId(int userId) {
        java.util.List<model.LoyaltyTransaction> list = new java.util.ArrayList<>();
        String sql = "SELECT id, customer_id, order_id, points_change, transaction_type, created_at " +
                     "FROM loyalty_transactions WHERE customer_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new model.LoyaltyTransaction(
                        rs.getInt("id"),
                        rs.getInt("customer_id"),
                        rs.getInt("order_id"),
                        rs.getInt("points_change"),
                        rs.getString("transaction_type"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
