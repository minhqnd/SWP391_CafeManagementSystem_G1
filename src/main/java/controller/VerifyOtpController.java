package controller;


import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

// BF10 - XÁC MINH OTP: nhận POST /verify-otp, so với mã của phiên yêu cầu khôi phục.
@WebServlet(name = "VerifyOtpController", urlPatterns = {"/verify-otp"})
public class VerifyOtpController extends HttpServlet {


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        String inputOtp = request.getParameter("otp");
        // OT05: . Ở đây em so sánh mã vừa nhập với mã đã giữ trong phiên.
        // .. Khớp thì cho phép đặt lại mật khẩu; sai thì yêu cầu nhập lại.
        // ... Đặt canResetPassword=true rồi mở form mật khẩu mới [OT06].
        
        HttpSession session = request.getSession();
        String sessionOtp = (String) session.getAttribute("forgotOtp");
        String forgotEmail = (String) session.getAttribute("forgotEmail");
        Long sentAt = (Long) session.getAttribute("lastOtpSentTime");
        
        if (sessionOtp == null || forgotEmail == null || sentAt == null
                || System.currentTimeMillis() - sentAt > 300000L) {
            session.setAttribute("errorMsg", "Phiên xác thực đã hết hạn. Vui lòng yêu cầu lại.");
            response.sendRedirect(request.getContextPath() + "/?tab=forgot");
            return;
        }

        // Đúng mã -> xóa OTP đã dùng, đánh dấu quyền reset; chưa cập nhật mật khẩu ở bước này.
        // Hiện hàm kiểm tra trạng thái session và khớp mã, chưa có kiểm tra TTL riêng cho OTP.
        if (sessionOtp.equals(inputOtp)) {
            // OTP matches, allow resetting password
            session.removeAttribute("forgotOtp");
            session.setAttribute("canResetPassword", true);
            response.sendRedirect(request.getContextPath() + "/?tab=reset_password");
        } else {
            // Invalid OTP
            session.setAttribute("errorMsg", "Mã OTP không chính xác. Vui lòng kiểm tra lại.");
            response.sendRedirect(request.getContextPath() + "/?tab=otp");
        }
    }
}
