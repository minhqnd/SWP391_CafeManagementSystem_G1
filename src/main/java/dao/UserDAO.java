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

    // [DNL04]
    // . Trong UserDAO.login, em tìm dòng users có email hoặc số điện thoại vừa nhập. Tài khoản phải ACTIVE, sau
    //   đó PasswordHasher.verify đối chiếu mật khẩu nhập với mật khẩu đang lưu.
    // . Đúng thì em trả User về LoginController.doPost; sai thì trả null. Lỗi đọc database được báo riêng để
    //   controller hiện lời báo lỗi hệ thống.
    // ... Đọc tiếp tại LoginController.java, tìm [DNL05].
    public User login(String identifier, String password) {
        if (identifier == null || password == null || identifier.length() > 150 || password.length() > 255) {
            return null;
        }
        identifier = identifier.trim();
        // [SQL10-01] Đăng nhập: tìm tài khoản theo email hoặc số điện thoại.
        // . Dấu ? từ trái sang phải: 1=định danh đăng nhập; 2=cùng định danh đăng nhập.
        // . SQL chỉ tìm tài khoản; Java kiểm tra ACTIVE và gọi PasswordHasher.verify để đối chiếu mật khẩu.
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
                        // . Dấu ? từ trái sang phải: 1=chuỗi băm mới; 2=mã người dùng; 3=giá trị mật khẩu cũ
                        //   trong database.
                        // . Chỉ chạy sau khi Java đối chiếu mật khẩu thành công; không lấy mật khẩu nhập để so
                        //   trực tiếp bằng SQL.
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

    // [DKL06]
    // . RegisterController.doPost truyền User và mật khẩu đến hàm register trong UserDAO.java. Em gọi
    //   PasswordHasher.hash để tạo chuỗi băm từ mật khẩu, rồi thêm tài khoản ACTIVE vào bảng users.
    // . Thêm được thì trả true về controller; trùng dữ liệu hoặc không thêm được thì trả false. Lỗi đọc/ghi
    //   khác được báo riêng để controller giữ form và hiện kết quả chưa tạo được tài khoản.
    // ... Đọc tiếp tại RegisterController.java, tìm [DKL07].
    public boolean register(User user, String password) {
        // [SQL10-03] Tạo tài khoản: thêm tên, liên hệ, vai trò, vị trí và mật khẩu đã băm.
        // . Dấu ? từ trái sang phải: 1=họ tên; 2=số điện thoại; 3=email; 4=vai trò; 5=vị trí, có thể NULL;
        //   6=chuỗi băm mật khẩu.
        // . Trạng thái ACTIVE được ghi cố định; chuỗi băm do PasswordHasher.hash tạo, không phải mật khẩu gõ
        //   trên web.
        String sql = "INSERT INTO users (full_name, phone, email, role, position, status, password_hash) " +
                     "VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getPosition());
            // . Trong UserDAO.register (tạo tài khoản khách hàng), em gọi PasswordHasher.hash (tạo chuỗi băm mật
            //   khẩu để lưu) bằng mật khẩu người dùng nhập. Em dùng chuỗi trả về để ghi cột password_hash thay vì
            //   ghi nguyên mật khẩu.
            ps.setString(6, PasswordHasher.hash(password));

            // . Trong UserDAO.register (tạo tài khoản khách hàng), em gọi executeUpdate (thực hiện lệnh thêm, sửa
            //   hoặc xóa dữ liệu) để thực hiện INSERT users. Nếu affectedRows lớn hơn 0 thì trả true, còn không thì
            //   trả false về RegisterController.doPost để báo kết quả tạo tài khoản.
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;

        } catch (Exception e) {
            if (e instanceof java.sql.SQLException sqlError && sqlError.getErrorCode() == 1062) return false;
            throw new utils.AccountDataException(e);
        }
    }

    // [DKEM04]
    // . Ở checkEmailExists (tìm email đã được dùng) trong UserDAO.java, em nhận email từ
    //   RegisterController.doPost và tìm trong users. Em trả đúng hoặc sai về controller để nếu địa chỉ đã
    //   được dùng thì báo lỗi tại ô Email trước khi tạo tài khoản. Nếu truy vấn bị lỗi thì báo không kiểm tra
    //   được thông tin; em không coi email đó là còn trống để tiếp tục đăng ký.
    // ... Quay lại RegisterController.java, trong doPost ngay sau userDAO.checkEmailExists: trùng thì thêm lỗi
    //     email, rồi xét errors trước khi tạo tài khoản.
    public boolean checkEmailExists(String email) {
        // [SQL10-04] Ô Email đăng ký: tìm email đã được dùng.
        // . Dấu ? từ trái sang phải: 1=email đã làm gọn và chuyển chữ thường.
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

    // [DKDT04]
    // . Ở checkPhoneExists (tìm số điện thoại đã được dùng) trong UserDAO.java, em nhận số điện thoại từ
    //   RegisterController.doPost và tìm trong users. Em trả kết quả về controller để nếu số đã có tài khoản
    //   thì báo tại ô Số điện thoại và chưa tạo tài khoản mới. Nếu không đọc được dữ liệu thì báo lỗi riêng,
    //   để controller dừng đăng ký và giữ các ô đang nhập.
    // ... Quay lại RegisterController.java, trong doPost ngay sau userDAO.checkPhoneExists: trùng thì thêm lỗi
    //     phone, rồi kiểm tra email và xét errors.
    public boolean checkPhoneExists(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        // [SQL10-05] Ô Số điện thoại đăng ký: tìm số đã được dùng.
        // . Dấu ? từ trái sang phải: 1=số điện thoại đã làm gọn.
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

    // [BMLH03]
    // . Ở contactExists (tìm email hoặc số điện thoại trùng) trong UserDAO.java, em nhận loại ô, giá trị và
    //   mã tài khoản cần bỏ qua từ AccountAvailabilityController.doPost. Em tìm liên hệ trùng rồi trả kết
    //   quả về controller. Controller trả exists cho checkContact (kiểm tra liên hệ trùng ngay trên form)
    //   trong account-validation.js để báo ngay tại ô nhập.
    // .. Nếu việc đọc dữ liệu gặp lỗi thì em báo không kiểm tra được, thay vì coi liên hệ chắc chắn chưa có
    //    người dùng.
    // ... Quay lại AccountAvailabilityController.java, trong doPost sau users.contactExists; controller gửi exists
    //     về checkContact trong account-validation.js để hiện lỗi liên hệ hoặc cho gửi form.
    public boolean contactExists(String field, String value, int excludedUserId) {
        if (!"email".equals(field) && !"phone".equals(field)) throw new IllegalArgumentException("Invalid contact field");
        String column = "email".equals(field) ? "LOWER(TRIM(email))" : "phone";
        // [SQL10-06] Kiểm tra liên hệ ngay trên form: tìm trùng và bỏ qua tài khoản đang sửa.
        // . Dấu ? từ trái sang phải: 1=liên hệ cần kiểm tra; 2=mã tài khoản bỏ qua; đăng ký dùng 0.
        // . Tên cột do Java chọn: email dùng LOWER(TRIM(email)); điện thoại dùng phone.
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
    // . Ở findActiveAccountByEmail trong UserDAO.java, em nhận email từ PasswordRecoveryService.start và tìm
    //   tài khoản tương ứng. Có đúng một tài khoản còn hoạt động thì trả User về start để gửi OTP; không có
    //   thì trả null. Tài khoản bị khóa, email gắn nhiều tài khoản hoặc lỗi tra cứu được báo riêng, chưa gửi mã.
    // ... Quay lại PasswordRecoveryService.java, trong start sau users.findActiveAccountByEmail: không có tài
    //     khoản thì báo lỗi; có tài khoản mới kiểm tra thời gian chờ và gửi OTP.
    public User findActiveAccountByEmail(String email) {
        // [SQL10-07] Quên mật khẩu: tìm tài khoản của email trước khi gửi OTP.
        // . Dấu ? từ trái sang phải: 1=email đã làm gọn và chuyển chữ thường.
        // . Câu SQL lấy tối đa 2 dòng; Java từ chối email trùng nhiều tài khoản hoặc tài khoản không ACTIVE.
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
    // . Ở getActiveUserById (đọc tài khoản còn hoạt động theo mã) trong UserDAO.java, em nhận mã người đăng
    //   nhập từ ProfileController.authorize (đọc và kiểm tra người đang đăng nhập). Em đọc tài khoản ACTIVE
    //   cùng số điểm rồi trả User về authorize, để trang hồ sơ dùng thông tin hiện tại của đúng người. Không
    //   tìm thấy thì trả null; lỗi đọc dữ liệu được báo riêng để không xóa nhầm phiên đang đăng nhập.
    // ... Nếu đang mở hồ sơ và tìm được tài khoản, quay lại ProfileController.java, tìm [HSTV01] để cập nhật thông tin vừa đọc.
    // ... Nếu AccountAvailabilityController.java gọi kiểm tra tài khoản trước khi tra trùng liên hệ, quay lại
    //     doPost sau users.getActiveUserById để tiếp tục tìm liên hệ; lỗi hoặc không có tài khoản thì dừng yêu cầu đó.
    public User getActiveUserById(int id) {
        // [SQL10-08] Mở hồ sơ: đọc thông tin và số điểm hiện tại của người đang đăng nhập.
        // . Dấu ? từ trái sang phải: 1=mã người dùng trong phiên đăng nhập.
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
    // . Ở resetPasswordById trong UserDAO.java, em nhận mã tài khoản, email đã xác minh và mật khẩu mới từ
    //   PasswordRecoveryService.reset. Em băm mật khẩu rồi cập nhật đúng tài khoản ACTIVE trong users, trả
    //   true nếu ghi được đúng một tài khoản, false nếu tài khoản không còn khớp. Nếu không đọc/ghi được dữ
    //   liệu thì báo lỗi riêng về reset; người dùng có thể thử lại khi lần xác minh vẫn còn hạn.
    // ... Quay lại PasswordRecoveryService.java, trong reset sau users.resetPasswordById để xét updated và xóa dữ
    //     liệu xác minh; reset xong mới về ResetPasswordController.doPost.
    public boolean resetPasswordById(int id, String verifiedEmail, String password) {
        if (AccountValidation.passwordError(password, password) != null) return false;
        // [SQL10-09] Đặt lại mật khẩu sau OTP: ghi mật khẩu mới cho đúng tài khoản.
        // . Dấu ? từ trái sang phải: 1=chuỗi băm mật khẩu mới; 2=mã tài khoản đã xác minh; 3=email đã xác minh.
        // . Java phải xác minh OTP trước khi gọi hàm này; bảng users không lưu mã OTP.
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET password_hash=? WHERE id=? AND LOWER(TRIM(email))=? AND status='ACTIVE'")) {
            ps.setString(1, PasswordHasher.hash(password)); ps.setInt(2,id); ps.setString(3,AccountValidation.email(verifiedEmail));
            return ps.executeUpdate() == 1;
        } catch (Exception e) { throw new utils.AccountDataException(e); }
    }

    // [MKL03]
    // . Trong changePasswordById của UserDAO.java, em gọi PasswordHasher.verify để so mật khẩu hiện tại với
    //   giá trị đang lưu. Nếu đúng, em băm mật khẩu mới và cập nhật users, rồi trả kết quả về
    //   ProfileController.doPost để thông báo cho người dùng. Mật khẩu hiện tại sai thì trả false; lỗi đọc
    //   hoặc ghi sẽ hủy thay đổi và báo riêng, không gắn nhầm lỗi vào ô Mật khẩu hiện tại.
    // ... Đọc tiếp tại ProfileController.java, tìm [MKL04].
    public boolean changePasswordById(int id, String current, String password) {
        if (AccountValidation.passwordError(password, password) != null) return false;
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // [SQL10-10] Đổi mật khẩu: đọc mật khẩu đang lưu của tài khoản ACTIVE.
                // . Dấu ? từ trái sang phải: 1=mã người dùng.
                // . FOR UPDATE giữ dòng trong lần đổi mật khẩu; Java dùng PasswordHasher.verify để so mật khẩu
                //   hiện tại.
                try (PreparedStatement read = conn.prepareStatement("SELECT password_hash FROM users WHERE id=? AND status='ACTIVE' FOR UPDATE")) {
                    read.setInt(1,id);
                    try (ResultSet rs=read.executeQuery()) {
                        if (!rs.next() || !PasswordHasher.verify(current,rs.getString(1))) { conn.rollback(); return false; }
                    }
                }
                // [SQL10-11] Đổi mật khẩu: ghi chuỗi băm mới sau khi mật khẩu hiện tại đã đúng.
                // . Dấu ? từ trái sang phải: 1=chuỗi băm mật khẩu mới; 2=mã người dùng.
                // . Đọc và ghi nằm trong cùng lần lưu; chỉ xác nhận khi cập nhật đúng một tài khoản.
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

    // [HSL05]
    // . ProfileController.update gọi updateUserProfile trong UserDAO.java, truyền mã người đăng nhập và các ô
    //   hồ sơ. Em tìm email, số điện thoại thuộc tài khoản khác trước; không trùng mới cập nhật dòng users còn
    //   ACTIVE theo mã này.
    // . Hàm trả chuỗi success nếu lưu được hoặc lời báo lỗi nếu chưa lưu được. ProfileController.update đọc kết
    //   quả để cập nhật thông tin trên trang hoặc giữ form cho người dùng sửa.
    // ... Khi result là success, đọc tiếp tại ProfileController.java, tìm [HSL06] để cập nhật phiên đăng nhập.
    // ... Khi result là lỗi, quay lại update trong ProfileController.java ngay sau users.updateUserProfile;
    //   nhánh kiểm tra result giữ các ô, hiện lỗi rồi renderProfile mở lại trang để kết thúc lần lưu lỗi.
    public String updateUserProfile(int userId, String fullName, String phone, String email, String birthday, String address) {
        // [SQL10-12] Lưu thay đổi hồ sơ: cập nhật các trường của đúng tài khoản ACTIVE.
        // . Dấu ? từ trái sang phải: 1=họ tên; 2=số điện thoại; 3=email; 4=ngày sinh hoặc NULL; 5=địa chỉ; 6=mã
        //   người dùng.
        // . Java kiểm tra định dạng và tra trùng liên hệ trước khi thực hiện lệnh cập nhật này.
        String sql = "UPDATE users SET full_name = ?, phone = ?, email = ?, birthday = ?, address = ? WHERE id = ? AND status='ACTIVE'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // . Trước khi cập nhật hồ sơ trong UserDAO.updateUserProfile (kiểm tra liên hệ rồi ghi hồ sơ mới), em
            //   dùng email và mã người dùng để tìm tài khoản khác đã dùng địa chỉ đó. Nếu trùng thì trả lý do lỗi về
            //   ProfileController.update (kiểm tra và cập nhật hồ sơ) và chưa ghi thay đổi.

            // [SQL10-13] Lưu hồ sơ: kiểm tra email có thuộc người khác không.
            // . Dấu ? từ trái sang phải: 1=email đã chuẩn hóa; 2=mã người dùng hiện tại.
            try (PreparedStatement duplicate=conn.prepareStatement("SELECT id FROM users WHERE LOWER(TRIM(email))=? AND id<>? LIMIT 1")) {
                duplicate.setString(1,AccountValidation.email(email));duplicate.setInt(2,userId);
                try(ResultSet rs=duplicate.executeQuery()){if(rs.next())return "Email đã được sử dụng bởi tài khoản khác!";}
            }
            // [SQL10-14] Lưu hồ sơ: kiểm tra số điện thoại có thuộc người khác không.
            // . Dấu ? từ trái sang phải: 1=số điện thoại; 2=mã người dùng hiện tại.
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

            // . Trong UserDAO.updateUserProfile (kiểm tra liên hệ rồi ghi hồ sơ mới), em ghi tên, số điện thoại,
            //   email, ngày sinh và địa chỉ theo mã tài khoản còn ACTIVE. Em trả kết quả về ProfileController.update
            //   (kiểm tra và cập nhật hồ sơ) để nếu thành công thì cập nhật thông tin đang hiện, còn lỗi thì báo
            //   chưa lưu được.
            int updated = ps.executeUpdate();
            return updated > 0 ? "success" : "Không cập nhật được hồ sơ. Tài khoản không còn tồn tại hoặc đã ngừng hoạt động.";
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            return "Thông tin liên hệ vừa được tài khoản khác sử dụng. Vui lòng kiểm tra lại email và số điện thoại.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Không thể lưu hồ sơ. Vui lòng thử lại sau.";
        }
    }

    /** Điểm Aura hiện tại (đọc lại sau khi thanh toán / đổi điểm để cập nhật user trong session) */
    public int getTotalPoints(int userId) {

        // [SQL10-15] Đọc riêng số điểm của tài khoản khi cần làm mới thông tin đăng nhập.
        // . Dấu ? từ trái sang phải: 1=mã người dùng.
        // . Trang hồ sơ còn nhận total_points từ getActiveUserById; không phải mọi lần mở hồ sơ đều gọi hàm
        //   này.
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
    // . Ở getLoyaltyHistoryByUserId (đọc lịch sử điểm của người đang đăng nhập) trong UserDAO.java, em nhận
    //   mã người đăng nhập từ ProfileController.renderProfile (chuẩn bị dữ liệu và mở trang hồ sơ). Em đọc
    //   lịch sử của người đó trong loyalty_transactions rồi trả danh sách về renderProfile để đưa vào
    //   loyaltyHistory và hiện trên profile.jsp. Nếu truy vấn lỗi thì báo về renderProfile để hiện cảnh báo
    //   chưa tải được lịch sử, tránh hiển thị nhầm rằng khách chưa có giao dịch điểm.
    // ... Quay qua renderProfile trong ProfileController.java để đặt loyaltyHistory; xem kết quả trên profile.jsp,
    //     tìm [HSLS03].
    public java.util.List<model.LoyaltyTransaction> getLoyaltyHistoryByUserId(int userId) {

        java.util.List<model.LoyaltyTransaction> list = new java.util.ArrayList<>();
        // [SQL10-16] Tab Lịch sử tích điểm trong hồ sơ: đọc giao dịch của người đang đăng nhập.
        // . Dấu ? từ trái sang phải: 1=mã người dùng.
        // . Sắp xếp theo thời gian mới trước; truy vấn này không tự tính số dư sau mỗi giao dịch.
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
