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
            // . Khi nhận yêu cầu tại doPost trong ResendOtpController.java, em gọi resend trong
            //   PasswordRecoveryService.java. Em truyền session và địa chỉ gửi yêu cầu để kiểm tra lần gửi trước,
            //   bảo đảm đã đủ thời gian chờ rồi mới gửi lại OTP.
            // ... Đọc tiếp tại PasswordRecoveryService.java, tìm [OTGL03].
            PasswordRecoveryService.shared().resend(request.getSession(),request.getRemoteAddr());

            // [OTGL04]
            // . Khi gửi lại mã thành công, chương trình quay về doPost trong ResendOtpController.java. Em đặt thông
            //   báo rồi mở lại tab=otp để người dùng nhập mã mới trong auth_modal.jsp.
            // ... Kết thúc lần Gửi lại khi auth_modal.jsp mở lại ô OTP cùng thông báo; muốn xác minh mã mới thì
            //     người dùng bấm Xác nhận OTP.
            request.getSession().setAttribute("successMsg","Mã OTP mới đã được gửi đến email của bạn.");
        }catch(IllegalArgumentException e){request.getSession().setAttribute("errorMsg",e.getMessage());}
        // . Em gọi nextTab trong PasswordRecoveryService.java để mở lại bước còn dùng được: gửi thành công
        //   thì nhập OTP mới; không còn lần xác minh hợp lệ thì quay về email để yêu cầu lại.
        response.sendRedirect(request.getContextPath()+"/?tab="+PasswordRecoveryService.shared().nextTab(request.getSession()));
    }
}
