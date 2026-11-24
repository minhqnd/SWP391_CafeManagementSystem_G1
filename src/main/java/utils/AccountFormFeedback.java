package utils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/** One-use feedback. Passwords and OTPs are never persisted in form values. */
public final class AccountFormFeedback {
    private AccountFormFeedback() {}

    // . Trước một lần gửi form mới, em gọi clear từ controller để bỏ kết quả của lần xử lý trước.
    //   Lần gửi này sẽ đặt lỗi hoặc thông báo mới, tránh hiện kết quả cũ cùng với kết quả vừa nhận.
    public static void clear(HttpServletRequest request) {
        for (String key : new String[]{"successMsg", "errorMsg", "accountFeedback"})
            request.getSession().removeAttribute(key);
    }

    // [BMGC01]
    // . Ở flash (giữ lỗi và các ô qua lần chuyển trang) trong AccountFormFeedback.java, em nhận tên form,
    //   lỗi và các giá trị được phép giữ từ controller. Em lưu accountFeedback qua lần chuyển trang để
    //   account_feedback.jsp và account-validation.js khôi phục đúng form, điền lại ô và hiển thị lỗi cho
    //   người dùng sửa.
    // ... Controller chuyển trang rồi account_feedback.jsp nhận dữ liệu đã giữ; đọc tiếp tại account_feedback.jsp,
    //     tìm [BMTV02].
    public static void flash(HttpServletRequest request, String form, Map<String,String> errors,
                             Map<String,String> values) {
        request.getSession().setAttribute("accountFeedback", feedback(form, errors, values));
    }

    // [BMHT01]
    // . Ở show (hiện lỗi trên trang đang mở) trong AccountFormFeedback.java, em nhận tên form, lỗi và
    //   giá trị từ ProfileController.java. Em đưa chúng vào accountFeedback của trang đang mở để
    //   account_feedback.jsp và account-validation.js hiện lại thông tin đang sửa cùng lỗi đúng ô.
    // ... Quay về ProfileController sau AccountFormFeedback.show để gọi renderProfile và mở lại trang.
    //     Đọc account_feedback.jsp, tìm mã BMTV02 để xem account_feedback.jsp đưa các ô và lỗi cho phần
    //     điền lại form.
    public static void show(HttpServletRequest request, String form, Map<String,String> errors,
                            Map<String,String> values) {
        request.setAttribute("accountFeedback", feedback(form, errors, values));
    }

    // . Ở feedback (chuẩn bị lỗi và dữ liệu để điền lại form) trong AccountFormFeedback.java, em nhận tên
    //   form, lỗi và giá trị từ flash (giữ lỗi và các ô qua lần chuyển trang) hoặc show (hiện lỗi trên trang
    //   đang mở). Em chỉ giữ các ô được phép rồi trả dữ liệu để account_feedback.jsp và
    //   account-validation.js khôi phục form sau lỗi.
    private static Map<String,Object> feedback(String form, Map<String,String> errors, Map<String,String> values) {
        Map<String,String> safeValues = new LinkedHashMap<>();
        for (String key : new String[]{"username", "fullname", "fullName", "phone", "email", "address", "birthDay", "birthMonth", "birthYear"}) {
            if (values.containsKey(key)) safeValues.put(key, values.get(key));
        }
        return Map.of("form", form, "errors", new LinkedHashMap<>(errors), "values", safeValues);
    }
}
