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

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            java.sql.ResultSetMetaData rsmd = rs.getMetaData();
            int columns = rsmd.getColumnCount();
            for (int x = 1; x <= columns; x++) {
                if (columnName.equalsIgnoreCase(rsmd.getColumnName(x)) || 
                    columnName.equalsIgnoreCase(rsmd.getColumnLabel(x))) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public User login(String identifier, String password) {
        // LG03: . Ở đây em tìm tài khoản bằng email hoặc số điện thoại, kiểm tra trạng thái và mật khẩu.
        // .. Đúng thông tin và tài khoản hoạt động thì em trả về người dùng.
        // ... Trả User về LoginController [LG04]; hồ sơ cũng gọi để kiểm tra mật khẩu cũ [PW02].
        // BF10: xác thực và dựng User trả về LoginController để tạo session.
        // Lưu ý code hiện tìm theo email HOẶC phone; không mô tả backend này là email-only.
        if (identifier == null || password == null) {
            return null;
        }
        identifier = identifier.trim();
        password = password.trim();
        String sql = "SELECT * FROM users WHERE (email = ? OR phone = ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, identifier);
            ps.setString(2, identifier);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    
                    if (!"ACTIVE".equalsIgnoreCase(status)) {
                        return null; 
                    }

                    // Lưu ý: tên cột là password_hash nhưng hiện so trực tiếp và có nhánh demo_hash.
                    // Đây chưa phải xác minh mật khẩu bằng thuật toán hash an toàn.
                    String dbHash = rs.getString("password_hash");
                    boolean passwordMatches = false;
                    if (dbHash != null) {
                        if (dbHash.equals(password)) {
                            passwordMatches = true;
                        } else if ("demo_hash".equals(dbHash) && 
                                  ("123456".equals(password) || "1234".equals(password) || "demo_hash".equals(password) || "admin123".equals(password) || "password".equals(password) || "an123".equals(password))) {
                            passwordMatches = true;
                        }
                    }

                    if (!passwordMatches) {
                        return null;
                    }

                    String birthdayStr = "";
                    if (hasColumn(rs, "birthday")) {
                        birthdayStr = rs.getString("birthday");
                        if (birthdayStr != null && birthdayStr.length() >= 10) {
                            birthdayStr = birthdayStr.substring(0, 10);
                        } else if (birthdayStr == null) {
                            birthdayStr = "";
                        }
                    }

                    String addressStr = "";
                    if (hasColumn(rs, "address")) {
                        addressStr = rs.getString("address");
                        if (addressStr == null) addressStr = "";
                    }

                    int totalPoints = hasColumn(rs, "total_points") ? rs.getInt("total_points") : 0;

                    return new User(
                            rs.getInt("id"),
                            rs.getString("full_name"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("position"),
                            status,
                            birthdayStr,
                            addressStr,
                            totalPoints
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean register(User user, String password) {
        // RG04: . Ở đây em thêm tài khoản khách vào cơ sở dữ liệu.
        // .. Em trả kết quả lưu được hay không để chọn thông báo.
        // ... register(User, mật khẩu) trả boolean về RegisterController [RG05].
        // BF10: INSERT tài khoản ACTIVE; Controller đã đặt role CUSTOMER cho đăng ký từ web.
        // Kết quả true/false được Controller chuyển thành thông báo; mật khẩu hiện chưa hash.
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
        // RG03: . Ở đây em kiểm tra email hoặc số điện thoại đã có tài khoản chưa.
        // .. Kết quả giúp máy chủ báo trùng trước khi thêm tài khoản.
        // ... Trả boolean về RegisterController [RG02]; kiểm tra email cũng dùng khi quên mật khẩu [OT02].
        // BF10: kiểm tra email có bản ghi users để chống trùng hoặc xác định luồng quên mật khẩu.
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

    public boolean checkPhoneExists(String phone) {
        // BF10: kiểm tra trùng số điện thoại khi đăng ký, không đồng nghĩa đây là thao tác đăng nhập.
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        String sql = "SELECT id FROM users WHERE phone = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updatePassword(String email, String newPassword) {
        // PW03: . Ở đây em cập nhật mật khẩu cho tài khoản có email được truyền vào.
        // .. Em trả kết quả có cập nhật được tài khoản không.
        // ... Trả boolean về đúng nơi gọi: đặt lại mật khẩu [OT07] hoặc đổi tại hồ sơ [PW02].
        // BF10: dùng chung cho reset sau OTP và đổi mật khẩu trong hồ sơ; UPDATE theo email.
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
    public String updateUserProfile(int userId, String fullName, String phone, String email, String birthday, String address) {
        // PF04: . Ở đây em lưu thông tin hồ sơ theo đúng mã người dùng.
        // .. Em trả thông báo thành công hoặc nguyên nhân không lưu được.
        // ... Kết quả về ProfileController [PF05].
        // BF10: UPDATE đúng ID của phiên đăng nhập; lỗi khóa duy nhất được đổi thành thông báo trùng.
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
            
            int updated = ps.executeUpdate();
            return updated > 0 ? "success" : "Cập nhật thất bại, không có thay đổi nào.";
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            return "Số điện thoại hoặc Email đã được sử dụng bởi tài khoản khác!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }

    /** Điểm Aura hiện tại (đọc lại sau khi thanh toán / đổi điểm để cập nhật user trong session) */
    public int getTotalPoints(int userId) {
        // PH04: . Ở đây em đọc số điểm hiện có của khách từ cơ sở dữ liệu.
        // .. Hàm chỉ trả số dư để nơi gọi sử dụng, không thưởng thêm điểm.
        // ... UserDAO.getTotalPoints(mã khách) trả số dư; giỏ nhận tại [RD03].
        String sql = "SELECT total_points FROM users WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_points");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public java.util.List<model.LoyaltyTransaction> getLoyaltyHistoryByUserId(int userId) {
        // PH02: . Ở đây em đọc lịch sử điểm của đúng người đang đăng nhập.
        // .. Em tạo danh sách giao dịch, sắp giao dịch mới trước.
        // ... List về ProfileController [PF02] → vòng lặp lịch sử [PH03].
        // BF10/BF12: đọc lịch sử riêng theo customer_id; ProfileController đặt vào loyaltyHistory cho JSP.
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

    /**
     * Tìm kiếm khách hàng theo SĐT (hoặc email) để tích/đổi điểm tại quầy POS
     */
    public model.User findCustomerByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return null;
        String sql = "SELECT id, full_name, phone, email, role, position, status, total_points " +
                     "FROM users WHERE role = 'CUSTOMER' AND (phone = ? OR email = ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            ps.setString(2, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new model.User(
                            rs.getInt("id"),
                            rs.getString("full_name"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("position"),
                            rs.getString("status"),
                            "",
                            "",
                            rs.getInt("total_points")
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public model.User getActiveCustomerById(int id) {
        String sql = "SELECT id,full_name,phone,email,role,position,status,total_points FROM users " +
                "WHERE id=? AND role='CUSTOMER' AND status='ACTIVE'";
        try (Connection conn=DBContext.getConnection(); PreparedStatement ps=conn.prepareStatement(sql)) {
            ps.setInt(1,id);
            try(ResultSet rs=ps.executeQuery()){
                if(rs.next()) return new model.User(rs.getInt("id"),rs.getString("full_name"),rs.getString("phone"),
                        rs.getString("email"),rs.getString("role"),rs.getString("position"),rs.getString("status"),
                        "","",rs.getInt("total_points"));
            }
        } catch(Exception e){e.printStackTrace();}
        return null;
    }

    /**
     * Lấy nhân viên đang hoạt động để phân ca (chỉ STAFF; ADMIN không được xếp lịch).
     */
    public model.User getActiveStaffById(int userId) {
        if (userId <= 0) return null;

        String sql = "SELECT id, full_name, phone, email, role, position, status, birthday, address " +
                     "FROM users " +
                     "WHERE id = ? AND role = 'STAFF' AND status = 'ACTIVE'";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new model.User(
                            rs.getInt("id"),
                            rs.getString("full_name"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("position"),
                            rs.getString("status"),
                            rs.getString("birthday"),
                            rs.getString("address")
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public java.util.List<model.User> getStaffEmployees() {
        java.util.List<model.User> list = new java.util.ArrayList<>();
        String sql = "SELECT id, full_name, phone, email, role, position, status, birthday, address " +
                     "FROM users WHERE role = 'STAFF' AND status = 'ACTIVE' " +
                     "ORDER BY CASE position " +
                     "  WHEN 'MANAGER' THEN 1 " +
                     "  WHEN 'BARISTA' THEN 2 " +
                     "  WHEN 'CASHIER' THEN 3 " +
                     "  WHEN 'SHIPPER' THEN 4 " +
                     "  ELSE 5 END ASC, full_name ASC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new model.User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("position"),
                        rs.getString("status"),
                        rs.getString("birthday"),
                        rs.getString("address")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
