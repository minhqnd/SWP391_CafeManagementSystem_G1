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

    public static String token(HttpSession session) {
        synchronized (session) {
            Object token = session.getAttribute(KEY);
            if (token == null) { token = UUID.randomUUID().toString(); session.setAttribute(KEY, token); }
            return token.toString();
        }
    }

    public static boolean require(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object expected = session == null ? null : session.getAttribute(KEY);
        String submitted = request.getParameter("csrfToken");
        if (expected == null || submitted == null || !MessageDigest.isEqual(expected.toString().getBytes(StandardCharsets.UTF_8), submitted.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(403, "Phiên biểu mẫu đã hết hạn. Tải lại trang rồi thử lại."); return false;
        }
        return true;
    }

    public static void rotate(HttpSession session) { synchronized(session) { session.setAttribute(KEY, UUID.randomUUID().toString()); } }
}
