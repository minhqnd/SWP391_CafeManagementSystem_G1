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

    // [DNL03]
    // . Hàm login trong UserDAO tìm tài khoản bằng email hoặc số điện thoại rồi đối chiếu mật khẩu. Đúng mật
    //   khẩu và tài khoản còn hoạt động thì trả thông tin người dùng về doPost của LoginController.
    //
    // Tra tiếp: LoginController.java / DNL04.
    public User login(String identifier, String password) {
        if (identifier == null || password == null || identifier.length() > 150 || password.length() > 255) {
            return null;
        }
        identifier = identifier.trim();
        // [SQL10-01] Đăng nhập: tìm tài khoản theo email hoặc số điện thoại.

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
                        // [SQL10-02] Đăng nhập bằng mật khẩu cũ: thay giá trị cũ bằng chuỗi băm.

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
            throw new utils.AccountDataException(e);
        }
        return null;
    }

    // [DKL03]
    // . Hàm register trong UserDAO thêm tài khoản mới vào bảng users ở trạng thái hoạt động. Em chuẩn bị mật
    //   khẩu trước khi ghi rồi trả kết quả về doPost của RegisterController để báo đã tạo được tài khoản hay
    //   chưa.
    //
    // Tra tiếp: RegisterController.java / DKL04.
    public boolean register(User user, String password) {
        // [SQL10-03] Tạo tài khoản: thêm tên, liên hệ, vai trò, vị trí và mật khẩu đã băm.

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
            if (e instanceof java.sql.SQLException sqlError && sqlError.getErrorCode() == 1062) return false;
            throw new utils.AccountDataException(e);
        }
    }

    public boolean checkEmailExists(String email) {
        // [SQL10-04] Ô Email đăng ký: tìm email đã được dùng.

        String sql = "SELECT id FROM users WHERE email = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, AccountValidation.email(email));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new utils.AccountDataException(e);
        }
    }

    public boolean checkPhoneExists(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        // [SQL10-05] Ô Số điện thoại đăng ký: tìm số đã được dùng.

        String sql = "SELECT id FROM users WHERE phone = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new utils.AccountDataException(e);
        }
    }

    // [TKTR03]
    // . Hàm contactExists trong UserDAO tìm email hoặc số điện thoại đã có trong users và bỏ qua tài khoản
    //   đang sửa. Kết quả quay về AccountAvailabilityController rồi trả cho form, để lời báo chỉ hiện khi một
    //   tài khoản khác đang dùng liên hệ đó.
    //
    // Tra cứu: Trở về AccountAvailabilityController.java / TKTR02 ngay sau contactExists rồi trả kết quả cho
    //          account-validation.js / TKTR01. Các hàm checkEmailExists và checkPhoneExists phía trên phục vụ
    //          kiểm tra lại khi đăng ký tại RegisterController.java / DKL02.
    public boolean contactExists(String field, String value, int excludedUserId) {
        if (!"email".equals(field) && !"phone".equals(field)) throw new IllegalArgumentException("Invalid contact field");
        String column = "email".equals(field) ? "LOWER(TRIM(email))" : "phone";
        // [SQL10-06] Kiểm tra liên hệ ngay trên form: tìm trùng và bỏ qua tài khoản đang sửa.

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM users WHERE " + column + "=? AND id<>? LIMIT 1")) {
            ps.setString(1, value);
            ps.setInt(2, excludedUserId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (Exception e) {
            throw new IllegalStateException("Không kiểm tra được thông tin. Vui lòng thử lại.");
        }
    }

    // [OTG04]
    // . Hàm findActiveAccountByEmail trong UserDAO tìm tài khoản còn hoạt động theo email và trả kết quả về
    //   start của PasswordRecoveryService. Nếu tìm được, start tạo mã xác nhận rồi dùng EmailUtil gửi tới
    //   email này; không có tài khoản thì báo để người dùng kiểm tra lại email.
    //
    // Tra tiếp: EmailUtil.java / OTG05.
    public User findActiveAccountByEmail(String email) {
        // [SQL10-07] Quên mật khẩu: tìm tài khoản của email trước khi gửi OTP.

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

    // [HSM03]
    // . Hàm getActiveUserById trong UserDAO đọc hồ sơ và số điểm của tài khoản còn hoạt động theo mã vừa
    //   nhận. Kết quả quay về authorize trong ProfileController; tài khoản hợp lệ được giữ lại rồi hàm mở
    //   trang gọi renderProfile để chuẩn bị nội dung.
    //
    // Tra tiếp: ProfileController.java / HSM04.
    public User getActiveUserById(int id) {
        // [SQL10-08] Mở hồ sơ: đọc thông tin và số điểm hiện tại của người đang đăng nhập.

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT id,full_name,phone,email,role,position,status,birthday,address,total_points FROM users WHERE id=? AND status='ACTIVE'")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? accountFromRow(rs) : null; }
        } catch (Exception e) { throw new utils.AccountDataException(e); }
    }
    private User accountFromRow(ResultSet rs) throws java.sql.SQLException {
        return new User(rs.getInt("id"),rs.getString("full_name"),rs.getString("phone"),rs.getString("email"),
                rs.getString("role"),rs.getString("position"),rs.getString("status"),rs.getString("birthday"),rs.getString("address"),rs.getInt("total_points"));
    }

    // [OTDM04]
    // . Hàm resetPasswordById trong UserDAO chuẩn bị mật khẩu mới rồi cập nhật tài khoản có đúng mã và email
    //   đã xác nhận. Kết quả quay về reset của PasswordRecoveryService; sau khi hàm này xử lý xong,
    //   ResetPasswordController mới thông báo kết quả cho người dùng.
    //
    // Tra tiếp: ResetPasswordController.java / OTDM05.
    public boolean resetPasswordById(int id, String verifiedEmail, String password) {
        if (AccountValidation.passwordError(password, password) != null) return false;
        // [SQL10-09] Đặt lại mật khẩu sau OTP: ghi mật khẩu mới cho đúng tài khoản.

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET password_hash=? WHERE id=? AND LOWER(TRIM(email))=? AND status='ACTIVE'")) {
            ps.setString(1, PasswordHasher.hash(password)); ps.setInt(2,id); ps.setString(3,AccountValidation.email(verifiedEmail));
            return ps.executeUpdate() == 1;
        } catch (Exception e) { throw new utils.AccountDataException(e); }
    }

    // [MKL03]
    // . Hàm changePasswordById trong UserDAO đối chiếu mật khẩu hiện tại của tài khoản trước khi thay bằng
    //   mật khẩu mới. Kết quả quay về doPost của ProfileController để biết đã đổi được, mật khẩu cũ sai hoặc
    //   cần báo lỗi khác.
    //
    // Tra tiếp: ProfileController.java / MKL04.
    public boolean changePasswordById(int id, String current, String password) {
        if (AccountValidation.passwordError(password, password) != null) return false;
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // [SQL10-10] Đổi mật khẩu: đọc mật khẩu đang lưu của tài khoản ACTIVE.

                try (PreparedStatement read = conn.prepareStatement("SELECT password_hash FROM users WHERE id=? AND status='ACTIVE' FOR UPDATE")) {
                    read.setInt(1,id);
                    try (ResultSet rs=read.executeQuery()) {
                        if (!rs.next() || !PasswordHasher.verify(current,rs.getString(1))) { conn.rollback(); return false; }
                    }
                }
                // [SQL10-11] Đổi mật khẩu: ghi chuỗi băm mới sau khi mật khẩu hiện tại đã đúng.

                try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET password_hash=? WHERE id=? AND status='ACTIVE'")) {
                    ps.setString(1,PasswordHasher.hash(password));ps.setInt(2,id);
                    if(ps.executeUpdate()!=1){conn.rollback();return false;}
                }
                conn.commit();return true;
            } catch(Exception e){conn.rollback();throw e;}
        } catch(Exception e){throw new utils.AccountDataException(e);}
    }
    /** Compatibility wrapper: never update several accounts sharing an old email. */
    public boolean updatePassword(String email, String password) {
        User account=findActiveAccountByEmail(email);
        return account!=null && resetPasswordById(account.getId(),account.getEmail(),password);
    }

    // [HSL03]
    // . Hàm updateUserProfile trong UserDAO kiểm tra lại liên hệ trùng rồi cập nhật hồ sơ của đúng tài khoản
    //   trong users. Kết quả quay về hàm update trong ProfileController để tiếp tục cập nhật màn hình hoặc
    //   báo phần chưa đúng.
    //
    // Tra tiếp: ProfileController.java / HSL04.
    public String updateUserProfile(int userId, String fullName, String phone, String email, String birthday, String address) {
        // [SQL10-12] Lưu thay đổi hồ sơ: cập nhật các trường của đúng tài khoản ACTIVE.

        String sql = "UPDATE users SET full_name = ?, phone = ?, email = ?, birthday = ?, address = ? WHERE id = ? AND status='ACTIVE'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // [SQL10-13] Lưu hồ sơ: kiểm tra email có thuộc người khác không.

            try (PreparedStatement duplicate=conn.prepareStatement("SELECT id FROM users WHERE LOWER(TRIM(email))=? AND id<>? LIMIT 1")) {
                duplicate.setString(1,AccountValidation.email(email));duplicate.setInt(2,userId);
                try(ResultSet rs=duplicate.executeQuery()){if(rs.next())return "Email đã được sử dụng bởi tài khoản khác!";}
            }
            // [SQL10-14] Lưu hồ sơ: kiểm tra số điện thoại có thuộc người khác không.

            try (PreparedStatement duplicate=conn.prepareStatement("SELECT id FROM users WHERE phone=? AND id<>? LIMIT 1")) {
                duplicate.setString(1,phone);duplicate.setInt(2,userId);
                try(ResultSet rs=duplicate.executeQuery()){if(rs.next())return "Số điện thoại đã được sử dụng bởi tài khoản khác!";}
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
            return updated > 0 ? "success" : "Không cập nhật được hồ sơ. Tài khoản không còn tồn tại hoặc đã ngừng hoạt động.";
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            if (e.getErrorCode() == 1062)
                return "Thông tin liên hệ vừa được tài khoản khác sử dụng. Vui lòng kiểm tra lại email và số điện thoại.";
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.WARNING,
                    "Profile update constraint failed", e);
            return "Không thể lưu hồ sơ. Vui lòng thử lại sau.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Không thể lưu hồ sơ. Vui lòng thử lại sau.";
        }
    }

    /** Điểm Aura hiện tại (đọc lại sau khi thanh toán / đổi điểm để cập nhật user trong session) */
    public int getTotalPoints(int userId) {

        // [SQL10-15] Đọc riêng số điểm của tài khoản khi cần làm mới thông tin đăng nhập.

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

    // [HSLS02]
    // . Hàm getLoyaltyHistoryByUserId trong UserDAO đọc các giao dịch điểm và đơn hàng liên quan theo mã tài
    //   khoản. Danh sách quay về renderProfile của ProfileController rồi được đưa sang profile.jsp để người
    //   dùng xem lịch sử của mình.
    //
    // Tra tiếp: profile.jsp / HSLS03.
    public java.util.List<model.LoyaltyTransaction> getLoyaltyHistoryByUserId(int userId) {

        java.util.List<model.LoyaltyTransaction> list = new java.util.ArrayList<>();
        // [SQL10-16] Tab Lịch sử tích điểm trong hồ sơ: đọc giao dịch của người đang đăng nhập.

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
            throw new utils.AccountDataException(e);
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
