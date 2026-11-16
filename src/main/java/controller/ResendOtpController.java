package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import service.PasswordRecoveryService;
import utils.AccountCsrf;

@WebServlet(name="ResendOtpController",urlPatterns={"/resend-otp"})
public class ResendOtpController extends HttpServlet {
    // OT07c: . Nhận POST /resend-otp từ nút Gửi lại mã [OT07b].
    // .. Trở lại tab OTP với thông báo gửi thành công hoặc lý do từ chối.
    // ... PasswordRecoveryService.resend() [OT07d] → thông báo auth_modal.jsp vùng thông báo trong auth_modal.jsp.
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{
        if(!AccountCsrf.require(request,response))return;
        try{
            PasswordRecoveryService.shared().resend(request.getSession(),request.getRemoteAddr());
            request.getSession().setAttribute("successMsg","Mã OTP mới đã được gửi đến email của bạn.");
        }catch(IllegalArgumentException e){request.getSession().setAttribute("errorMsg",e.getMessage());}
        response.sendRedirect(request.getContextPath()+"/?tab=otp");
    }
}
