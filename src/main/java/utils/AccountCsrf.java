package utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public final class AccountCsrf {
    private static final String KEY = "accountCsrfToken";
    private AccountCsrf() {}
    // . Các form tài khoản gọi token (lấy mã bảo vệ dùng cho form) trong AccountCsrf.java bằng session để
    //   nhận mã dùng cho form. Em đặt mã vào ô ẩn csrfToken để khi form gửi tới controller, require (đối
    //   chiếu mã bảo vệ của form) có thể đối chiếu với mã đã cấp.
    public static String token(HttpSession session) {
        synchronized (session) {
            Object token = session.getAttribute(KEY);
            if (token == null) { token = UUID.randomUUID().toString(); session.setAttribute(KEY, token); }
            return token.toString();
        }
    }
    // [BMVE01]
    // . Khi controller tài khoản gọi require (đối chiếu mã bảo vệ của form) trong AccountCsrf.java, em lấy
    //   csrfToken của form để so với mã đã cấp cho lần dùng trang này. Hàm trả đúng hoặc sai để controller
    //   tiếp tục xử lý hoặc dừng yêu cầu không hợp lệ.
    // ... Quay lại đầu doPost trong LoginController.java, RegisterController.java, ProfileController.java,
    //     AccountAvailabilityController.java, ForgotPasswordController.java, VerifyOtpController.java,
    //     ResendOtpController.java hoặc ResetPasswordController.java; sau require, true mới đọc form tiếp, false
    //     thì dừng.
    public static boolean require(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object expected = session == null ? null : session.getAttribute(KEY);
        String submitted = request.getParameter("csrfToken");
        if (expected == null || submitted == null || !MessageDigest.isEqual(expected.toString().getBytes(StandardCharsets.UTF_8), submitted.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(403, "Phiên biểu mẫu đã hết hạn. Tải lại trang rồi thử lại."); return false;
        }
        return true;
    }
    // . Sau khi đặt lại mật khẩu, ResetPasswordController.doPost gọi rotate (tạo mã bảo vệ mới cho form)
    //   trong AccountCsrf.java bằng session. Em tạo mã form mới để các lần gửi tiếp theo dùng mã mới này.
    public static void rotate(HttpSession session) { synchronized(session) { session.setAttribute(KEY, UUID.randomUUID().toString()); } }
}
