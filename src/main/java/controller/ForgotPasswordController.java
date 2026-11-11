package controller;

import dao.UserDAO;
import utils.EmailUtil;
import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// BF10 - QUÊN MẬT KHẨU: nhận email từ popup -> kiểm tra users -> gửi OTP -> mở tab otp.
@WebServlet(name = "ForgotPasswordController", urlPatterns = { "/forgot-password" })
public class ForgotPasswordController extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        // OT02: . Ở đây em kiểm tra email có tài khoản rồi tạo mã xác nhận.
        // .. Em gửi mã qua email và giữ email, mã trong phiên để kiểm tra ở bước sau.
        // ... EmailUtil.sendOTP(email, mã) [OT03] → mở form nhập mã [OT04].

        if (email == null || email.trim().isEmpty()) {
            request.getSession().setAttribute("errorMsg", "Vui lòng nhập địa chỉ email.");
            response.sendRedirect(request.getContextPath() + "/?tab=forgot");
            return;
        }

        // 1. Kiểm tra email có tồn tại không
        boolean exists = userDAO.checkEmailExists(email);
        if (!exists) {
            request.getSession().setAttribute("errorMsg", "Email không tồn tại trong hệ thống.");
            response.sendRedirect(request.getContextPath() + "/?tab=forgot");
            return;
        }

        // 2. Tạo OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        // 3. Gửi OTP qua email (Chạy đồng bộ để bắt lỗi hiển thị lên màn hình)
        boolean isSent = EmailUtil.sendOTP(email, otp);

        // LUÔN LUÔN cho phép đi tiếp để phục vụ quá trình test/làm đồ án (có thể lấy
        // OTP trong Console)
        // Phiên giữ email, mã OTP và thời điểm gửi để VerifyOtp/ResendOtp dùng ở bước sau.
        // Lưu ý bản hiện tại có fallback hiển thị OTP khi gửi mail lỗi; không phải cơ chế dùng production.
        request.getSession().setAttribute("forgotEmail", email);
        request.getSession().setAttribute("forgotOtp", otp);
        request.getSession().setAttribute("lastOtpSentTime", System.currentTimeMillis());

        if (isSent) {
            request.getSession().setAttribute("successMsg",
                    "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư (cả mục thư rác).");
        } else {
            request.getSession().setAttribute("errorMsg",
                    "Gửi Email thất bại do lỗi cấu hình SMTP. Mã OTP của bạn là: " + otp);
        }

        response.sendRedirect(request.getContextPath() + "/?tab=otp");
    }
}
