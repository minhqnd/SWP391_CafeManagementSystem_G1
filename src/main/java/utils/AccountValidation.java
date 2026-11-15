package utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Shared server rules for BF10 forms. Password characters are never trimmed. */
public final class AccountValidation {
    private AccountValidation() {}
    // . Dùng chung quy tắc backend để chuẩn hóa văn bản, email và SĐT; mật khẩu giữ nguyên.
    // .. Hàm kiểm tra trả null khi hợp lệ hoặc trả nội dung lỗi khi sai để controller gắn theo tên trường.
    public static String text(String raw) { return raw == null ? "" : raw.trim(); }
    public static String email(String raw) { return text(raw).toLowerCase(Locale.ROOT); }
    public static String phone(String raw) { return text(raw).replaceAll("[\\s.()-]", ""); }
    // . Họ tên bắt buộc, 1–100 ký tự.
    public static String nameError(String name) {
        return name.isEmpty() || name.length() > 100 ? "Họ và tên phải từ 1 đến 100 ký tự." : null;
    }
    // . Email đúng định dạng và tối đa 150 ký tự.
    public static String emailError(String email) {
        if (email.length() > 150 || !email.matches("[A-Za-z0-9_+%-]+(?:\\.[A-Za-z0-9_+%-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+"))
            return "Địa chỉ email không hợp lệ hoặc dài quá 150 ký tự.";
        return null;
    }
    // . SĐT sau chuẩn hóa phải có đúng 10 chữ số và bắt đầu bằng 0.
    public static String phoneError(String phone) {
        return phone.matches("0[0-9]{9}") ? null : "Số điện thoại sai định dạng.";
    }
    // . Mật khẩu 8–255 ký tự, có chữ và số; xác nhận phải khớp.
    public static String passwordError(String password, String confirmation) {
        if (password == null || password.length() < 8 || password.length() > 255 || password.isBlank())
            return "Mật khẩu phải từ 8 đến 255 ký tự và không được chỉ có khoảng trắng.";
        if (!password.chars().anyMatch(c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
                || !password.chars().anyMatch(c -> c >= '0' && c <= '9'))
            return "Mật khẩu phải có ít nhất một chữ cái (a-z, A-Z) và một chữ số (0-9).";
        if (confirmation == null || confirmation.isEmpty() || !password.equals(confirmation))
            return "Mật khẩu xác nhận không khớp hoặc chưa được nhập.";
        return null;
    }
    // . Tách lỗi mật khẩu và xác nhận để giao diện báo đúng ô.
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
    // . Ngày sinh được bỏ trống; nếu nhập phải đúng ngày, từ 1900 và không ở tương lai.
    public static String birthdayError(String birthday) {
        if (birthday.isEmpty()) return null;
        try {
            LocalDate day = LocalDate.parse(birthday);
            if (!day.toString().equals(birthday) || day.getYear() < 1900
                    || day.isAfter(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")))) throw new DateTimeParseException("date", birthday, 0);
            return null;
        } catch (DateTimeParseException e) { return "Ngày sinh không hợp lệ hoặc ở trong tương lai."; }
    }
}
