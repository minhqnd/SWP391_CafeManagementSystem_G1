package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import service.PasswordRecoveryService;
import utils.AccountCsrf;

@WebServlet(name="VerifyOtpController",urlPatterns={"/verify-otp"})
public class VerifyOtpController extends HttpServlet {
    // OT05: . Nhận OTP từ nút Xác nhận và kiểm tra token biểu mẫu.
    // ... PasswordRecoveryService.verify() kiểm tra mã, thời hạn và số lần sai [OT12].
    // .. Đúng thì mở tab đặt lại mật khẩu; sai thì giữ tab OTP và báo lỗi dưới ô mã.
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{
        request.setCharacterEncoding("UTF-8");if(!AccountCsrf.require(request,response))return;
        try{
            PasswordRecoveryService.shared().verify(request.getSession(),request.getParameter("otp"));
            response.sendRedirect(request.getContextPath()+"/?tab=reset_password");
        }catch(IllegalArgumentException e){
            utils.AccountFormFeedback.flash(request,"verify-otp",java.util.Map.of("otp",e.getMessage()),java.util.Map.of());
            request.getSession().setAttribute("errorMsg",e.getMessage());response.sendRedirect(request.getContextPath()+"/?tab=otp");
        }
    }
}
