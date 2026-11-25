package utils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/** One-use feedback. Passwords and OTPs are never persisted in form values. */
public final class AccountFormFeedback {
    private AccountFormFeedback() {}

    public static void clear(HttpServletRequest request) {
        for (String key : new String[]{"successMsg", "errorMsg", "accountFeedback"})
            request.getSession().removeAttribute(key);
    }

    // [TKTB04]
    // . Em dùng AccountFormFeedback để giữ các ô được phép điền lại cùng lỗi của từng ô khi xử lý tài
    //   khoản. Nếu cần chuyển trang thì giữ thông tin này tạm thời; nếu hiển thị lại hồ sơ ngay thì đưa
    //   trực tiếp vào trang, để form nhận đúng nội dung cần sửa.
    //
    // Tra cứu: Hiển thị qua account_feedback.jsp / TKTB02 rồi account-validation.js / TKTB03. Lời gọi từ
    //          RegisterController.java / DKL02, LoginController.java / DNL02, ProfileController.java / HSL02
    //          hoặc ProfileController.java / MKL02 tiếp tục ngay sau hàm flash hay show tương ứng.
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
        for (String key : new String[]{"username", "fullname", "fullName", "phone", "email", "address", "birthDay", "birthMonth", "birthYear"}) {
            if (values.containsKey(key)) safeValues.put(key, values.get(key));
        }
        return Map.of("form", form, "errors", new LinkedHashMap<>(errors), "values", safeValues);
    }
}
