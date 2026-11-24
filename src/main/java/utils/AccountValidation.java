package utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class AccountValidation {
    private AccountValidation() {}

    public static String text(String raw) { return FormInput.trim(raw); }

    public static String email(String raw) { return text(raw).toLowerCase(Locale.ROOT); }

    public static String phone(String raw) { return text(raw).replaceAll("[\\s\\p{Z}\\uFEFF.()-]", ""); }

    // [KTHT01]
    // . nameError trong AccountValidation.java nhận họ tên đã được controller bỏ khoảng trắng đầu/cuối.
    //   Em kiểm tra tên dài 1–100 ký tự và không có ký tự điều khiển, như xuống dòng hoặc tab.
    // . Đúng thì trả null, nghĩa là ô này chưa có lỗi; sai thì trả lời báo để RegisterController hoặc
    //   ProfileController hiện tại ô Họ tên.
    // ... Trả về đúng lời gọi nameError. Nếu đăng ký, đọc bước gom lỗi tại RegisterController.java, tìm
    //     mã DKL04; nếu sửa hồ sơ, đọc bước gom lỗi tại ProfileController.java, tìm mã HSL03.
    public static String nameError(String name) {
        if (text(name).isEmpty()) return "Vui lòng nhập họ và tên.";
        if (name.length() > 100) return "Họ và tên tối đa 100 ký tự.";
        if (name.chars().anyMatch(Character::isISOControl)) return "Họ và tên chứa ký tự không được hỗ trợ.";
        return null;
    }

    // [KTEM01]
    // . emailError trong AccountValidation.java kiểm tra email có dạng tên@gmail.com và dài không quá 150 ký
    //   tự. Chữ hoa hoặc chữ thường đều được nhận.
    // . Trả null nghĩa là email đúng định dạng; trả lời báo nghĩa là ô này có lỗi. Phần đang gọi hàm nhận kết
    //   quả đó để quyết định có tiếp tục tìm trùng và xử lý yêu cầu hay không.
    // ... Quay lại lời gọi AccountValidation.emailError trong RegisterController.java, ProfileController.java,
    //   AccountAvailabilityController.java hoặc PasswordRecoveryService.java; có lỗi thì dừng nhánh đang xét,
    //   đúng mới đọc dữ liệu tiếp.
    public static String emailError(String email) {
        if (email == null || email.isEmpty()) return "Vui lòng nhập email.";
        if (email != null && email.length() > 150) return "Email tối đa 150 ký tự.";
        // [SUA10-05] Luyện sửa: Cho phép thêm đuôi email khác ngoài gmail.com.
        // . Sửa điều kiện nhận email tại đây và phần validate email trong account-validation.js; kiểm tra
        //   pattern/lời nhắc trong JSP nếu có.
        // . Vẫn cần kiểm tra định dạng và email trùng; thay đuôi được nhận không thay thế việc tìm trùng trong
        //   users.
        if (email == null || !email.matches("(?i)[A-Za-z0-9_+%-]+(?:\\.[A-Za-z0-9_+%-]+)*@gmail\\.com"))
            return "Sai định dạng email.";
        return null;
    }

    // [KTDT01]
    // . Trong hàm phoneError của AccountValidation.java, em nhận số điện thoại từ controller tài khoản. Em
    //   yêu cầu đúng 10 chữ số và bắt đầu bằng 0, rồi trả null nếu hợp lệ hoặc lời báo nếu sai để
    //   controller chưa tìm trùng hay lưu số không đúng.
    // ... Quay lại lời gọi AccountValidation.phoneError trong RegisterController.java,
    //     ProfileController.java, AccountAvailabilityController.java hoặc AdminLoyaltyController.java; số
    //     sai thì báo lỗi, số đúng mới tìm trùng hoặc tra khách.
    public static String phoneError(String phone) {
        if (phone == null || phone.isEmpty()) return "Vui lòng nhập số điện thoại.";
        return phone != null && phone.matches("0[0-9]{9}") ? null : "Số điện thoại sai định dạng.";
    }

    // [KTMK01]
    // . passwordError trong AccountValidation.java kiểm tra mật khẩu dài 8–255 ký tự, có chữ và số, không có ký
    //   tự điều khiển; mật khẩu xác nhận phải khớp.
    // . Đúng thì trả null; sai thì trả lời báo. RegisterController, ProfileController hoặc
    //   PasswordRecoveryService nhận kết quả để quyết định có tiếp tục lưu mật khẩu hay trả lỗi về form.
    // ... Quay lại lời gọi passwordError hoặc passwordErrors trong RegisterController.java khi đăng ký,
    //   ProfileController.java khi đổi mật khẩu, hoặc PasswordRecoveryService.java trong reset khi khôi phục;
    //   có lời báo thì dừng ghi mật khẩu.
    // ... Nếu UserDAO.java gọi kiểm tra lại trước khi ghi, quay về changePasswordById hoặc resetPasswordById
    //   ngay sau AccountValidation.passwordError; lỗi thì trả false, hợp lệ mới thực hiện phần ghi bên dưới.
    public static String passwordError(String password, String confirmation) {
        if (password == null || password.isEmpty()) return "Vui lòng nhập mật khẩu.";
        if (text(password).isEmpty()) return "Mật khẩu không được chỉ có khoảng trắng.";
        // [SUA10-03] Luyện sửa: Mật khẩu tối thiểu 10 ký tự thay vì 8.
        // . Sửa điều kiện và lời báo ở đây; sửa passwordError trong account-validation.js cùng quy tắc.
        // . Các ô mật khẩu trong auth_modal.jsp, login.jsp, profile.jsp có minlength/lời nhắc; kiểm tra lại khi
        //   đổi giới hạn.
        if (password.length() < 8 || password.length() > 255) return "Mật khẩu phải từ 8 đến 255 ký tự.";
        if (password.chars().anyMatch(Character::isISOControl))
            return "Mật khẩu không được chứa ký tự điều khiển.";
        if (!password.chars().anyMatch(c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
                || !password.chars().anyMatch(c -> c >= '0' && c <= '9'))
            return "Mật khẩu phải có ít nhất một chữ cái (a-z, A-Z) và một chữ số (0-9).";
        // [OTXN02]
        // . Trong passwordError, em đối chiếu ô xác nhận confirmation với mật khẩu password. Xác nhận trống
        //   hoặc khác thì trả lời báo; PasswordRecoveryService.reset nhận lỗi này sẽ dừng trước khi ghi mật
        //   khẩu mới.
        // . Khi đăng ký hoặc đổi mật khẩu qua passwordErrors, hàm đó kiểm tra riêng ô xác nhận và trả lỗi theo
        //   từng ô về RegisterController hoặc ProfileController.
        // ... Quay lại PasswordRecoveryService.java, trong reset sau AccountValidation.passwordError; không
        //   khớp thì báo lỗi về ResetPasswordController, khớp mới gọi users.resetPasswordById.
        // ... Nếu được gọi qua passwordErrors khi đăng ký hoặc đổi mật khẩu, quay lại passwordErrors cùng file;
        //   hàm đó kiểm tra riêng ô xác nhận rồi trả danh sách lỗi về RegisterController hoặc
        //   ProfileController.
        if (text(confirmation).isEmpty()) return "Vui lòng nhập mật khẩu xác nhận.";
        if (!password.equals(confirmation)) return "Mật khẩu xác nhận không khớp.";
        return null;
    }

    public static java.util.Map<String,String> passwordErrors(String password, String confirmation,
                                                              String passwordField, String confirmationField) {
        java.util.Map<String,String> errors = new java.util.LinkedHashMap<>();
        String error = passwordError(password, password);
        if (error != null) errors.put(passwordField, error);
        if (text(confirmation).isEmpty())
            errors.put(confirmationField, "Vui lòng nhập mật khẩu xác nhận.");
        else if (!confirmation.equals(password))
            errors.put(confirmationField, "Mật khẩu xác nhận không khớp.");
        return errors;
    }

    // [KTNS01]
    // . Trong hàm birthdayError của AccountValidation.java, em nhận ngày sinh đã ghép từ
    //   ProfileController.update. Nếu để trống thì được phép; nếu nhập thì phải là ngày có thật, từ năm
    //   1900 đến hôm nay. Em trả kết quả về update để báo đúng ô khi sai.
    // ... Quay về ProfileController.update sau birthdayError để đưa lỗi vào ô ngày sinh. Đọc bước xét
    //     errors tại ProfileController.java, tìm mã HSL03; không còn lỗi mới cho cập nhật.
    public static String birthdayError(String birthday) {
        if (birthday == null || birthday.isEmpty()) return null;
        try {
            LocalDate day = LocalDate.parse(birthday);
            if (!day.toString().equals(birthday) || day.getYear() < 1900
                    || day.isAfter(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")))) throw new DateTimeParseException("date", birthday, 0);
            return null;
        } catch (DateTimeParseException e) { return "Ngày sinh phải là ngày có thật, từ năm 1900 đến hôm nay."; }
    }

    // [KTDC01]
    // . Trong hàm addressError của AccountValidation.java, em nhận địa chỉ từ ProfileController.update và
    //   kiểm tra ký tự điều khiển. Nếu có thì em trả lời báo về update để hiện tại ô Địa chỉ. Giới hạn 500
    //   ký tự được hàm update kiểm tra riêng trước khi lưu.
    // ... Quay về ProfileController.update sau addressError, kiểm tra thêm độ dài rồi xét errors tại
    //     ProfileController.java, tìm mã HSL03. Có lỗi thì mở lại form, đúng hết mới lưu.
    public static String addressError(String address) {
        if (address == null || address.isEmpty()) return null;
        return address.chars().anyMatch(Character::isISOControl)
                ? "Địa chỉ chứa ký tự không được hỗ trợ."
                : null;
    }
}
