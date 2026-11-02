package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import utils.EmailUtil;
import java.io.IOException;

// BF10 - GỬI LẠI OTP: dùng forgotEmail trong session; chờ đủ 60 giây giữa các lần gửi lại.
@WebServlet(name = "ResendOtpController", urlPatterns = {"/resend-otp"})
public class ResendOtpController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String forgotEmail = (String) session.getAttribute("forgotEmail");
        
        if (forgotEmail == null) {
            session.setAttribute("errorMsg", "Phiên làm việc đã hết hạn. Vui lòng thử lại.");
            response.sendRedirect(request.getContextPath() + "/?tab=forgot");
            return;
        }

        // Giới hạn gửi lại OTP: Cooldown 60 giây
        Long lastOtpSentTime = (Long) session.getAttribute("lastOtpSentTime");
        long currentTime = System.currentTimeMillis();
        if (lastOtpSentTime != null && (currentTime - lastOtpSentTime) < 60000) {
            long waitTime = 60 - ((currentTime - lastOtpSentTime) / 1000);
            session.setAttribute("errorMsg", "Vui lòng đợi " + waitTime + " giây trước khi gửi yêu cầu mới.");
            response.sendRedirect(request.getContextPath() + "/?tab=otp");
            return;
        }

        // Tạo OTP mới
        String newOtp = String.format("%06d", new java.util.Random().nextInt(999999));

        // Gửi OTP qua email (Đồng bộ)
        boolean isSent = EmailUtil.sendOTP(forgotEmail, newOtp);
        
        // Luôn luôn cập nhật mã mới vào Session để phục vụ test
        // Mã mới thay mã cũ trong phiên; VerifyOtpController chỉ so sánh với mã đang lưu này.
        session.setAttribute("forgotOtp", newOtp);
        session.setAttribute("lastOtpSentTime", currentTime);
        
        if (isSent) {
            session.setAttribute("successMsg", "Mã OTP mới đã được gửi đến email của bạn.");
        } else {
            session.setAttribute("errorMsg", "Lỗi gửi Email. Mã OTP mới của bạn là: " + newOtp);
        }
        
        response.sendRedirect(request.getContextPath() + "/?tab=otp");
    }
}
