package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import service.PasswordRecoveryService;
import utils.AccountCsrf;

@WebServlet(name="ResendOtpController",urlPatterns={"/resend-otp"})
public class ResendOtpController extends HttpServlet {
    // OT08: . Nhận POST từ nút Gửi lại mã OTP và kiểm tra token biểu mẫu.
    // ... PasswordRecoveryService.resend() tạo và gửi mã mới cho email trong phiên [OT15].
    // .. Quay lại tab OTP với thông báo gửi thành công hoặc lý do chưa gửi được.
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{
        if(!AccountCsrf.require(request,response))return;
        try{
            PasswordRecoveryService.shared().resend(request.getSession(),request.getRemoteAddr());
            request.getSession().setAttribute("successMsg","Mã OTP mới đã được gửi đến email của bạn.");
        }catch(IllegalArgumentException e){request.getSession().setAttribute("errorMsg",e.getMessage());}
        response.sendRedirect(request.getContextPath()+"/?tab=otp");
    }
}
