package utils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/** One-use feedback. Passwords and OTPs are never persisted in form values. */
public final class AccountFormFeedback {
    private AccountFormFeedback() {}

    // . Lưu lỗi và giá trị an toàn vào session để giữ qua redirect.
    // ... AccountFormFeedback.feedback() tạo payload → payload trong account_feedback.jsp → JavaScript phần đọc payload trong account-validation.js.
    public static void flash(HttpServletRequest request, String form, Map<String,String> errors,
                             Map<String,String> values) {
        request.getSession().setAttribute("accountFeedback", feedback(form, errors, values));
    }

    // . Đặt lỗi và giá trị an toàn trong request khi trả lại trang hồ sơ bằng forward.
    // ... AccountFormFeedback.feedback() → payload trong account_feedback.jsp → hiển thị đúng ô feedback() trong account-validation.js.
    public static void show(HttpServletRequest request, String form, Map<String,String> errors,
                            Map<String,String> values) {
        request.setAttribute("accountFeedback", feedback(form, errors, values));
    }

    // . Chỉ giữ các trường tên, liên hệ, địa chỉ và ba phần ngày sinh được cho phép.
    // .. Payload gồm form, errors và values; không giữ giá trị mật khẩu hoặc OTP.
    private static Map<String,Object> feedback(String form, Map<String,String> errors, Map<String,String> values) {
        Map<String,String> safeValues = new LinkedHashMap<>();
        for (String key : new String[]{"fullname", "fullName", "phone", "email", "address", "birthDay", "birthMonth", "birthYear"}) {
            if (values.containsKey(key)) safeValues.put(key, values.get(key));
        }
        return Map.of("form", form, "errors", new LinkedHashMap<>(errors), "values", safeValues);
    }
}
