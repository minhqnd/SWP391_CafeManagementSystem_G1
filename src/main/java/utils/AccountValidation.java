package utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Shared server rules for BF10 forms. Password characters are never trimmed. */
public final class AccountValidation {
    private AccountValidation() {}
    // . Chuẩn hóa văn bản null thành chuỗi rỗng và bỏ khoảng trắng đầu/cuối.
    // .. Dùng cho họ tên/địa chỉ; không dùng để cắt ký tự mật khẩu.
    public static String text(String raw) { return raw == null ? "" : raw.trim(); }
    // . Bỏ khoảng trắng đầu/cuối và đổi email sang chữ thường.
    // .. Email viết hoa được chuẩn hóa trước khi kiểm tra và lưu.
    public static String email(String raw) { return text(raw).toLowerCase(Locale.ROOT); }
    // . Bỏ khoảng trắng, dấu chấm, ngoặc và dấu gạch ngang trong số điện thoại.
    // .. Trả chuỗi đã chuẩn hóa; kiểm tra đủ 10 số tại AccountValidation.phoneError().
    public static String phone(String raw) { return text(raw).replaceAll("[\\s.()-]", ""); }
    // . Kiểm tra họ tên bắt buộc, dài từ 1 đến 100 ký tự sau chuẩn hóa.
    // .. Hợp lệ trả null; sai trả thông báo để Controller gắn lỗi fullname/fullName.
    // ... Đăng ký [RG06a]; hồ sơ [PF12a].
    public static String nameError(String name) {
        if (name == null || name.isEmpty() || name.length() > 100
                || name.chars().anyMatch(Character::isISOControl)) {
            return "Họ và tên phải từ 1 đến 100 ký tự và không chứa ký tự điều khiển.";
        }
        return null;
    }
    // . Kiểm tra định dạng email và giới hạn tối đa 150 ký tự.
    // .. Hợp lệ trả null; sai trả thông báo lỗi trường email.
    // ... Đăng ký [RG06c], hồ sơ [PF12c], khôi phục [OT06].
    public static String emailError(String email) {
        if (email == null || email.length() > 150 || !email.matches("[A-Za-z0-9_+%-]+(?:\\.[A-Za-z0-9_+%-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+"))
            return "Địa chỉ email không hợp lệ hoặc dài quá 150 ký tự.";
        return null;
    }
    // . Kiểm tra SĐT có đúng 10 chữ số và bắt đầu bằng 0.
    // .. Hợp lệ trả null; sai trả thông báo lỗi trường phone.
    // ... Đăng ký [RG06b]; hồ sơ [PF12b].
    public static String phoneError(String phone) {
        return phone != null && phone.matches("0[0-9]{9}") ? null : "Số điện thoại sai định dạng.";
    }
    // . Kiểm tra mật khẩu 8–255 ký tự, không chỉ khoảng trắng, có chữ a-z/A-Z và số 0-9.
    // .. Xác nhận phải khớp; hợp lệ trả null, sai trả thông báo.
    // ... AccountValidation.passwordErrors() tách lỗi từng ô; khôi phục gọi từ [OT16].
    public static String passwordError(String password, String confirmation) {
        if (password == null || password.length() < 8 || password.length() > 255 || password.isBlank())
            return "Mật khẩu phải từ 8 đến 255 ký tự và không được chỉ có khoảng trắng.";
        if (password.chars().anyMatch(Character::isISOControl))
            return "Mật khẩu không được chứa ký tự điều khiển.";
        if (!password.chars().anyMatch(c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
                || !password.chars().anyMatch(c -> c >= '0' && c <= '9'))
            return "Mật khẩu phải có ít nhất một chữ cái (a-z, A-Z) và một chữ số (0-9).";
        if (confirmation == null || confirmation.isEmpty() || !password.equals(confirmation))
            return "Mật khẩu xác nhận không khớp hoặc chưa được nhập.";
        return null;
    }
    // . Tách lỗi mật khẩu và xác nhận bằng tên trường do Controller truyền vào.
    // .. Mật khẩu sai và xác nhận sai có thể được báo riêng cùng lúc.
    // ... Đăng ký [RG06d], đổi mật khẩu [PW05], đặt lại mật khẩu [OT15].
    public static java.util.Map<String,String> passwordErrors(String password, String confirmation,
                                                              String passwordField, String confirmationField) {
        java.util.Map<String,String> errors = new java.util.LinkedHashMap<>();
        String error = passwordError(password, password);
        if (error != null) errors.put(passwordField, error);
        if (confirmation == null || confirmation.isEmpty())
            errors.put(confirmationField, "Vui lòng nhập mật khẩu xác nhận.");
        else if (!confirmation.equals(password))
            errors.put(confirmationField, "Mật khẩu xác nhận không khớp.");
        return errors;
    }
    // . Cho phép bỏ trống ngày sinh; nếu có phải là ngày thật, từ năm 1900 và không ở tương lai.
    // .. Hợp lệ trả null; lỗi trả thông báo cho birthDay.
    // ... ProfileController.update() nhận kết quả tại [PF12d].
    public static String birthdayError(String birthday) {
        if (birthday == null || birthday.isEmpty()) return null;
        try {
            LocalDate day = LocalDate.parse(birthday);
            if (!day.toString().equals(birthday) || day.getYear() < 1900
                    || day.isAfter(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")))) throw new DateTimeParseException("date", birthday, 0);
            return null;
        } catch (DateTimeParseException e) { return "Ngày sinh không hợp lệ hoặc ở trong tương lai."; }
    }

    /** Reject control characters in free-text profile fields before they reach JSP or the database. */
    public static String addressError(String address) {
        if (address == null || address.isEmpty()) return null;
        return address.chars().anyMatch(Character::isISOControl)
                ? "Địa chỉ chứa ký tự không được hỗ trợ."
                : null;
    }
}
