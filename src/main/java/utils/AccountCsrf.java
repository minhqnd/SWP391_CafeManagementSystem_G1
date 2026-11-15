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
    // . Tạo hoặc lấy accountCsrfToken trong session để đưa vào input ẩn csrfToken.
    // ... Controller đọc token gửi lại và đối chiếu tại AccountCsrf.require().
    public static String token(HttpSession session) {
        synchronized (session) {
            Object token = session.getAttribute(KEY);
            if (token == null) { token = UUID.randomUUID().toString(); session.setAttribute(KEY, token); }
            return token.toString();
        }
    }
    // . Đối chiếu csrfToken của request với accountCsrfToken trong session.
    // .. Thiếu hoặc sai thì trả HTTP 403; đúng thì Controller tiếp tục xử lý form.
    public static boolean require(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object expected = session == null ? null : session.getAttribute(KEY);
        String submitted = request.getParameter("csrfToken");
        if (expected == null || submitted == null || !MessageDigest.isEqual(expected.toString().getBytes(StandardCharsets.UTF_8), submitted.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(403, "Phiên biểu mẫu đã hết hạn. Tải lại trang rồi thử lại."); return false;
        }
        return true;
    }
    // . Thay token của form sau khi đặt lại mật khẩu thành công.
    // .. Form dùng token cũ sẽ phải tải lại trước khi gửi tiếp.
    public static void rotate(HttpSession session) { synchronized(session) { session.setAttribute(KEY, UUID.randomUUID().toString()); } }
}
