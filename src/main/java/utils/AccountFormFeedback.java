package utils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/** One-use feedback. Passwords and OTPs are never persisted in form values. */
public final class AccountFormFeedback {
    private AccountFormFeedback() {}

    // . flash() giữ lỗi qua redirect; show() giữ lỗi trong request khi forward trang hồ sơ.
    // .. Chỉ giữ tên, liên hệ, ngày sinh và địa chỉ; không giữ mật khẩu hoặc OTP.
    // ... account_feedback.jsp đọc payload và đưa lỗi, giá trị an toàn ra giao diện.
    public static void flash(HttpServletRequest request, String form, Map<String,String> errors,
                             Map<String,String> values) {
        request.getSession().setAttribute("accountFeedback", feedback(form, errors, values));
    }

    public static void show(HttpServletRequest request, String form, Map<String,String> errors,
                            Map<String,String> values) {
        request.setAttribute("accountFeedback", feedback(form, errors, values));
    }

    private static Map<String,Object> feedback(String form, Map<String,String> errors, Map<String,String> values) {
        Map<String,String> safeValues = new LinkedHashMap<>();
        for (String key : new String[]{"fullname", "fullName", "phone", "email", "address", "birthDay", "birthMonth", "birthYear"}) {
            if (values.containsKey(key)) safeValues.put(key, values.get(key));
        }
        return Map.of("form", form, "errors", new LinkedHashMap<>(errors), "values", safeValues);
    }
}
