package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import service.PasswordRecoveryService;
import utils.AccountCsrf;

@WebServlet(name="ResendOtpController",urlPatterns={"/resend-otp"})
public class ResendOtpController extends HttpServlet {

    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{
        if(!AccountCsrf.require(request,response))return;
        utils.AccountFormFeedback.clear(request);
        try{
            // [OTGL02]
            // . Trong doPost của ResendOtpController, em gọi resend của PasswordRecoveryService để xử lý yêu
            //   cầu gửi lại. Em dùng class khôi phục đang có để kiểm tra thời gian chờ và giữ lần gửi mới gắn
            //   với cùng tài khoản.
            //
            // Tra tiếp: PasswordRecoveryService.java / OTGL03.
            PasswordRecoveryService.shared().resend(request.getSession(),request.getRemoteAddr());

            // [OTGL04]
            // . Khi resend hoàn tất, em quay lại doPost của ResendOtpController để báo đã gửi mã mới và mở
            //   lại form nhập mã. Người dùng tiếp tục bằng mã mới vừa nhận, thay vì bắt đầu lại từ bước nhập
            //   email.
            //
            // Kết thúc: gửi lại khi form nhập mã được mở kèm thông báo.
            request.getSession().setAttribute("successMsg","Mã OTP mới đã được gửi đến email của bạn.");
        }catch(IllegalArgumentException e){request.getSession().setAttribute("errorMsg",e.getMessage());}

        response.sendRedirect(request.getContextPath()+"/?tab="+PasswordRecoveryService.shared().nextTab(request.getSession()));
    }
}
