package utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;

// [TKKT01]
// . Em dùng AccountValidation để các chức năng tài khoản cùng kiểm tra tên, điện thoại, email, mật khẩu, ngày
//   sinh và địa chỉ. Mỗi hàm kiểm tra một loại thông tin và trả về lời báo nếu có lỗi; hàm đang gọi dùng kết
//   quả đó để quyết định lưu hay đưa form về cho người dùng sửa.
//
// Tra cứu: Đây là phần tham khảo điều kiện. Kiểm tra xong quay về ngay sau lời gọi tương ứng tại
//          RegisterController.java / DKL02, ProfileController.java / HSL02, ProfileController.java / MKL02,
//          PasswordRecoveryService.java / OTG03, PasswordRecoveryService.java / OTDM03 hoặc
//          AdminLoyaltyController.java / TDTK03; tiếp tục đường review ghi ở nơi gọi.
public final class AccountValidation {
    private AccountValidation() {}

    // Bỏ khoảng trắng ngoài trước khi kiểm tra hoặc lưu nội dung.
    public static String text(String raw) { return FormInput.trim(raw); }

    // Làm gọn và đưa email về chữ thường để kiểm tra, tìm trùng cùng một cách.
    public static String email(String raw) { return text(raw).toLowerCase(Locale.ROOT); }

    // Bỏ các dấu phân cách được hỗ trợ trước khi kiểm tra số điện thoại.
    public static String phone(String raw) { return text(raw).replaceAll("[\\s\\p{Z}\\uFEFF.()-]", ""); }

    // Kiểm tra họ tên có nội dung, không quá dài và không có ký tự điều khiển.
    public static String nameError(String name) {
        if (text(name).isEmpty()) return "Vui lòng nhập họ và tên.";
        if (name.length() > 100) return "Họ và tên tối đa 100 ký tự.";
        if (name.chars().anyMatch(Character::isISOControl)) return "Họ và tên chứa ký tự không được hỗ trợ.";
        return null;
    }

    // Kiểm tra email theo dạng Gmail mà chức năng tài khoản đang áp dụng.
    public static String emailError(String email) {
        if (email == null || email.isEmpty()) return "Vui lòng nhập email.";
        if (email != null && email.length() > 150) return "Email tối đa 150 ký tự.";
        if (email == null || !email.matches("(?i)[A-Za-z0-9_+%-]+(?:\\.[A-Za-z0-9_+%-]+)*@gmail\\.com"))
            return "Sai định dạng email.";
        return null;
    }

    // Kiểm tra số điện thoại đủ mười chữ số và bắt đầu bằng số không.
    public static String phoneError(String phone) {
        if (phone == null || phone.isEmpty()) return "Vui lòng nhập số điện thoại.";
        return phone != null && phone.matches("0[0-9]{9}") ? null : "Số điện thoại sai định dạng.";
    }

    // Kiểm tra mật khẩu mới và xác nhận trước khi cho tạo hoặc đổi tài khoản.
    public static String passwordError(String password, String confirmation) {
        if (password == null || password.isEmpty()) return "Vui lòng nhập mật khẩu.";
        if (text(password).isEmpty()) return "Mật khẩu không được chỉ có khoảng trắng.";
        if (password.length() < 8 || password.length() > 255) return "Mật khẩu phải từ 8 đến 255 ký tự.";
        if (password.chars().anyMatch(Character::isISOControl))
            return "Mật khẩu không được chứa ký tự điều khiển.";
        if (!password.chars().anyMatch(c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
                || !password.chars().anyMatch(c -> c >= '0' && c <= '9'))
            return "Mật khẩu phải có ít nhất một chữ cái (a-z, A-Z) và một chữ số (0-9).";

        if (text(confirmation).isEmpty()) return "Vui lòng nhập mật khẩu xác nhận.";
        if (!password.equals(confirmation)) return "Mật khẩu xác nhận không khớp.";
        return null;
    }

    // Tách lỗi mật khẩu và xác nhận để trả lời báo về đúng ô.
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

    // Kiểm tra ngày sinh có thật và nằm trong khoảng ngày được phép.
    public static String birthdayError(String birthday) {
        if (birthday == null || birthday.isEmpty()) return null;
        try {
            LocalDate day = LocalDate.parse(birthday);
            if (!day.toString().equals(birthday) || day.getYear() < 1900
                    || day.isAfter(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")))) throw new DateTimeParseException("date", birthday, 0);
            return null;
        } catch (DateTimeParseException e) { return "Ngày sinh phải là ngày có thật, từ năm 1900 đến hôm nay."; }
    }

    // Kiểm tra địa chỉ không chứa ký tự điều khiển trước khi ghi hồ sơ.
    public static String addressError(String address) {
        if (address == null || address.isEmpty()) return null;
        return address.chars().anyMatch(Character::isISOControl)
                ? "Địa chỉ chứa ký tự không được hỗ trợ."
                : null;
    }
}
