package controller;


import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "VerifyOtpController", urlPatterns = {"/verify-otp"})
public class VerifyOtpController extends HttpServlet {


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        String inputOtp = request.getParameter("otp");
        
        HttpSession session = request.getSession();
        String sessionOtp = (String) session.getAttribute("forgotOtp");
        String forgotEmail = (String) session.getAttribute("forgotEmail");
        
        if (sessionOtp == null || forgotEmail == null) {
            session.setAttribute("errorMsg", "Phiên xác thực đã hết hạn. Vui lòng yêu cầu lại.");
            response.sendRedirect(request.getContextPath() + "/?tab=forgot");
            return;
        }

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
