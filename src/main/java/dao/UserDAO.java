package dao;

import context.DBContext;
import model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import utils.AccountValidation;
import utils.PasswordHasher;

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

    // LG03: . Tìm users theo email hoặc SĐT, kiểm tra ACTIVE và đối chiếu password_hash.
    // .. Sai thông tin trả null; đúng trả User. Mật khẩu rõ cũ được băm lại khi đăng nhập thành công.
    // ... Kết quả trả về LoginController.doPost() [LG04].
    public User login(String identifier, String password) {
        if (identifier == null || password == null || password.length() > 255) {
            return null;
        }
        identifier = identifier.trim();
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

                    String dbHash = rs.getString("password_hash");
                    if (!PasswordHasher.verify(password, dbHash)) {
                        return null;
                    }
                    if (!PasswordHasher.encoded(dbHash)) {
                        try (PreparedStatement upgrade = conn.prepareStatement("UPDATE users SET password_hash=? WHERE id=? AND password_hash=? AND status='ACTIVE'")) {
                            upgrade.setString(1, PasswordHasher.hash(password));
                            upgrade.setInt(2, rs.getInt("id")); upgrade.setString(3, dbHash);
                            if (upgrade.executeUpdate() != 1) return null;
                        }
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

    // RG04: . Nhận User và mật khẩu đã được RegisterController kiểm tra.
    // .. Băm mật khẩu rồi INSERT tài khoản ACTIVE vào bảng users; thêm được trả true, lỗi trả false.
    // ... Kết quả trả về RegisterController.doPost() [RG05] để chọn thông báo và tab.
    public boolean register(User user, String password) {
        String sql = "INSERT INTO users (full_name, phone, email, role, position, status, password_hash) " +
                     "VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getPosition());
            ps.setString(6, PasswordHasher.hash(password));
            
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    // RG03: . Truy vấn bảng users theo email đã được RegisterController chuẩn hóa.
    // .. Có bản ghi trả true để báo email đã tồn tại; không có bản ghi trả false để tiếp tục.
    // ... Kết quả trả về RegisterController.doPost() [RG02] để gắn lỗi hoặc cho phép lưu.
    public boolean checkEmailExists(String email) {
        String sql = "SELECT id FROM users WHERE email = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, AccountValidation.email(email));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // RG06: . Truy vấn bảng users theo SĐT đã được RegisterController chuẩn hóa.
    // .. Có bản ghi trả true để báo SĐT đã tồn tại; không có bản ghi trả false để tiếp tục.
    // ... Kết quả trả về RegisterController.doPost() [RG02] để gắn lỗi hoặc cho phép lưu.
    public boolean checkPhoneExists(String phone) {
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

    /** Read-only form check; exclusion is supplied from the authenticated session only. */
    // RG07/PF10: . Tìm email hoặc SĐT, loại trừ ID đang sửa và chỉ cho phép hai cột đã định nghĩa.
    // .. Có bản ghi trả true; lỗi database được ném lên để controller trả HTTP 503.
    // ... Kết quả trả về AccountAvailabilityController.doPost().
    public boolean contactExists(String field, String value, int excludedUserId) {
        if (!"email".equals(field) && !"phone".equals(field)) throw new IllegalArgumentException("Invalid contact field");
        String column = "email".equals(field) ? "LOWER(TRIM(email))" : "phone";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM users WHERE " + column + "=? AND id<>? LIMIT 1")) {
            ps.setString(1, value);
            ps.setInt(2, excludedUserId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (Exception e) {
            throw new IllegalStateException("Không kiểm tra được thông tin. Vui lòng thử lại.");
        }
    }

    /** Resolve exactly one active account; ambiguous legacy emails fail closed. */
    // OT11:
    // . SELECT theo email chuẩn hóa, lấy tối đa hai bản ghi để phát hiện email trùng dữ liệu cũ.
    // .. Không có trả null; nhiều bản ghi/không ACTIVE/lỗi DB báo nguyên nhân riêng.
    // ... [OT10] Trả tài khoản hợp lệ về PasswordRecoveryService.start().
    public User findActiveAccountByEmail(String email) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT id,email,status FROM users WHERE LOWER(TRIM(email))=? LIMIT 2")) {
            ps.setString(1, AccountValidation.email(email));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setEmail(rs.getString("email"));
                user.setStatus(rs.getString("status"));
                if (rs.next()) throw new utils.RecoveryFailure(null,
                        "Email đang được liên kết với nhiều tài khoản. Vui lòng liên hệ quản trị viên để kiểm tra dữ liệu.");
                if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) throw new utils.RecoveryFailure(null,
                        "Tài khoản đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ quản trị viên.");
                return user;
            }
        } catch (utils.RecoveryFailure e) { throw e; }
        catch (Exception e) {
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.WARNING,
                    "Password recovery account lookup failed", e);
            throw new utils.RecoveryFailure(null,"Không thể kết nối hoặc tra cứu dữ liệu tài khoản. Vui lòng thử lại sau.");
        }
    }
    // PF07: . Tìm tài khoản ACTIVE theo ID trong session.
    // .. Có bản ghi trả User; không có hoặc xảy ra lỗi thì trả null.
    // ... Kết quả về ProfileController.authorize() [PF06].
    public User getActiveUserById(int id) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT id,full_name,phone,email,role,position,status,birthday,address,total_points FROM users WHERE id=? AND status='ACTIVE'")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? accountFromRow(rs) : null; }
        } catch (Exception e) { return null; }
    }
    private User accountFromRow(ResultSet rs) throws java.sql.SQLException {
        return new User(rs.getInt("id"),rs.getString("full_name"),rs.getString("phone"),rs.getString("email"),
                rs.getString("role"),rs.getString("position"),rs.getString("status"),rs.getString("birthday"),rs.getString("address"),rs.getInt("total_points"));
    }
    // OT14:
    // . Băm mật khẩu mới và UPDATE users theo ID + email đã xác minh + ACTIVE.
    // .. Đúng một bản ghi được cập nhật mới trả true.
    // ... [OT13] Kết quả về PasswordRecoveryService.reset().
    public boolean resetPasswordById(int id, String verifiedEmail, String password) {
        if (AccountValidation.passwordError(password, password) != null) return false;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET password_hash=? WHERE id=? AND LOWER(TRIM(email))=? AND status='ACTIVE'")) {
            ps.setString(1, PasswordHasher.hash(password)); ps.setInt(2,id); ps.setString(3,AccountValidation.email(verifiedEmail));
            return ps.executeUpdate() == 1;
        } catch (Exception e) { return false; }
    }
    // PW03: . Trong transaction, khóa tài khoản ACTIVE, đối chiếu mật khẩu cũ rồi lưu mật khẩu mới đã băm.
    // .. Thành công commit; sai mật khẩu hoặc lỗi thì rollback và trả false.
    // ... Kết quả về nhánh đổi mật khẩu trong ProfileController.doPost() [PW04].
    public boolean changePasswordById(int id, String current, String password) {
        if (AccountValidation.passwordError(password, password) != null) return false;
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement read = conn.prepareStatement("SELECT password_hash FROM users WHERE id=? AND status='ACTIVE' FOR UPDATE")) {
                    read.setInt(1,id);
                    try (ResultSet rs=read.executeQuery()) {
                        if (!rs.next() || !PasswordHasher.verify(current,rs.getString(1))) { conn.rollback(); return false; }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET password_hash=? WHERE id=? AND status='ACTIVE'")) {
                    ps.setString(1,PasswordHasher.hash(password));ps.setInt(2,id);
                    if(ps.executeUpdate()!=1){conn.rollback();return false;}
                }
                conn.commit();return true;
            } catch(Exception e){conn.rollback();return false;}
        } catch(Exception e){return false;}
    }
    // PF09: . Tìm password_hash theo ID ACTIVE và đối chiếu mật khẩu xác nhận đổi email.
    // .. Chỉ trả đúng hoặc sai, không thay đổi mật khẩu.
    // ... Kết quả về ProfileController.update() [PF08].
    public boolean verifyPasswordById(int id,String password) {
        try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement("SELECT password_hash FROM users WHERE id=? AND status='ACTIVE'")){
            ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next() && PasswordHasher.verify(password,rs.getString(1));}
        }catch(Exception e){return false;}
    }
    /** Compatibility wrapper: never update several accounts sharing an old email. */
    public boolean updatePassword(String email, String password) {
        User account=findActiveAccountByEmail(email);
        return account!=null && resetPasswordById(account.getId(),account.getEmail(),password);
    }
    // PF04: . Kiểm tra email thuộc tài khoản khác rồi UPDATE hồ sơ theo ID ACTIVE.
    // .. Lưu được thì trả success; trùng email/SĐT hoặc lỗi database thì trả thông báo lỗi.
    // ... Kết quả về ProfileController.update() để cập nhật session hoặc báo lỗi trường [PF08].
    public String updateUserProfile(int userId, String fullName, String phone, String email, String birthday, String address) {
        String sql = "UPDATE users SET full_name = ?, phone = ?, email = ?, birthday = ?, address = ? WHERE id = ? AND status='ACTIVE'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (PreparedStatement duplicate=conn.prepareStatement("SELECT id FROM users WHERE LOWER(TRIM(email))=? AND id<>? LIMIT 1")) {
                duplicate.setString(1,AccountValidation.email(email));duplicate.setInt(2,userId);
                try(ResultSet rs=duplicate.executeQuery()){if(rs.next())return "Email đã được sử dụng bởi tài khoản khác!";}
            }
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
            return "Không thể lưu hồ sơ. Vui lòng thử lại sau.";
        }
    }

    /** Điểm Aura hiện tại (đọc lại sau khi thanh toán / đổi điểm để cập nhật user trong session) */
    public int getTotalPoints(int userId) {
        // PH04: . Truy vấn số điểm hiện có của khách trong cơ sở dữ liệu.
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
        // PH02: . Truy vấn lịch sử điểm theo mã người dùng.
        // .. Tạo danh sách giao dịch, sắp giao dịch mới trước.
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
