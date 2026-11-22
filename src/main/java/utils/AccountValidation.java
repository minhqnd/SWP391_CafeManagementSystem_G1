package utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class AccountValidation {
    private AccountValidation() {}

    public static String text(String raw) { return raw == null ? "" : raw.trim(); }

    public static String email(String raw) { return text(raw).toLowerCase(Locale.ROOT); }

    public static String phone(String raw) { return text(raw).replaceAll("[\\s.()-]", ""); }

    // [KTHT01]
    // . Ở nameError (kiểm tra họ tên và trả lời báo lỗi) trong AccountValidation.java, em nhận họ tên từ
    //   phần đăng ký hoặc cập nhật hồ sơ. Em kiểm tra tên sau khi bỏ khoảng trắng phải dài 1–100 ký tự và
    //   không có ký tự điều khiển. Em trả null nếu đúng, hoặc lời báo để controller gắn vào ô Họ tên.
    public static String nameError(String name) {
        if (name == null || name.isBlank()) return "Vui lòng nhập họ và tên.";
        if (name.length() > 100) return "Họ và tên tối đa 100 ký tự.";
        if (name.chars().anyMatch(Character::isISOControl)) return "Họ và tên chứa ký tự không được hỗ trợ.";
        return null;
    }

    // [KTEM01]
    // . Ở emailError (kiểm tra định dạng và độ dài email) trong AccountValidation.java, em kiểm tra email mà
    //   phần đăng ký, hồ sơ hoặc tìm trùng truyền vào. Địa chỉ phải đúng dạng tên@gmail.com, tối đa 150 ký
    //   tự; chữ hoa và chữ thường đều được nhận. Em trả null hoặc lời báo về nơi gọi để biết có tiếp tục tìm
    //   trùng, lưu hay không.
    public static String emailError(String email) {
        if (email != null && email.isEmpty()) return "Vui lòng nhập email.";
        if (email != null && email.length() > 150) return "Email tối đa 150 ký tự.";
        if (email == null || !email.matches("(?i)[A-Za-z0-9_+%-]+(?:\\.[A-Za-z0-9_+%-]+)*@gmail\\.com"))
            return "Sai định dạng email.";
        return null;
    }

    // [KTDT01]
    // . Ở phoneError (kiểm tra định dạng số điện thoại) trong AccountValidation.java, em nhận số điện thoại
    //   từ controller tài khoản. Em yêu cầu đúng 10 chữ số và bắt đầu bằng 0, rồi trả null nếu hợp lệ hoặc
    //   lời báo nếu sai để controller chưa tìm trùng hay lưu số không đúng.
    public static String phoneError(String phone) {
        if (phone != null && phone.isEmpty()) return "Vui lòng nhập số điện thoại.";
        return phone != null && phone.matches("0[0-9]{9}") ? null : "Số điện thoại sai định dạng.";
    }

    // [KTMK01]
    // . Ở passwordError (kiểm tra điều kiện mật khẩu) trong AccountValidation.java, em kiểm tra mật khẩu và
    //   xác nhận mà phần đăng ký, đổi hoặc đặt lại mật khẩu truyền vào. Mật khẩu phải dài 8–255 ký tự, có
    //   chữ và số, không có ký tự điều khiển, xác nhận phải khớp. Em trả lời báo hoặc null để nơi gọi quyết
    //   định có tiếp tục không.
    public static String passwordError(String password, String confirmation) {
        if (password == null || password.isEmpty()) return "Vui lòng nhập mật khẩu.";
        if (password.isBlank()) return "Mật khẩu không được chỉ có khoảng trắng.";
        if (password.length() < 8 || password.length() > 255) return "Mật khẩu phải từ 8 đến 255 ký tự.";
        if (password.chars().anyMatch(Character::isISOControl))
            return "Mật khẩu không được chứa ký tự điều khiển.";
        if (!password.chars().anyMatch(c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
                || !password.chars().anyMatch(c -> c >= '0' && c <= '9'))
            return "Mật khẩu phải có ít nhất một chữ cái (a-z, A-Z) và một chữ số (0-9).";
        // [OTXN02]
        // . Trong passwordError (kiểm tra điều kiện mật khẩu) của AccountValidation.java, em đối chiếu
        //   confirmation với password sau khi kiểm tra điều kiện mật khẩu. Nếu ô xác nhận trống hoặc khác thì
        //   trả lời báo về nơi gọi; PasswordRecoveryService.reset (đặt lại mật khẩu sau khi xác minh OTP) nhận
        //   lỗi này sẽ chưa cập nhật mật khẩu.
        if (confirmation == null || confirmation.isEmpty() || !password.equals(confirmation))
            return "Mật khẩu xác nhận không khớp hoặc chưa được nhập.";
        return null;
    }

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

    // [KTNS01]
    // . Ở birthdayError (kiểm tra ngày sinh hợp lệ) trong AccountValidation.java, em nhận ngày sinh đã ghép
    //   từ ProfileController.update (kiểm tra và cập nhật hồ sơ). Nếu để trống thì được phép; nếu nhập thì
    //   phải là ngày có thật, từ năm 1900 đến hôm nay. Em trả kết quả về update để báo đúng ô khi sai.
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
    // . Ở addressError (kiểm tra ký tự địa chỉ) trong AccountValidation.java, em nhận địa chỉ từ
    //   ProfileController.update (kiểm tra và cập nhật hồ sơ) và kiểm tra ký tự điều khiển. Nếu có thì em
    //   trả lời báo về update để hiện tại ô Địa chỉ. Giới hạn 500 ký tự được hàm update kiểm tra riêng trước
    //   khi lưu.
    public static String addressError(String address) {
        if (address == null || address.isEmpty()) return null;
        return address.chars().anyMatch(Character::isISOControl)
                ? "Địa chỉ chứa ký tự không được hỗ trợ."
                : null;
    }
}
