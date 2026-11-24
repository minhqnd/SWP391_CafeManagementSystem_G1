package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import service.PasswordRecoveryService;
import utils.AccountCsrf;

@WebServlet(name="VerifyOtpController",urlPatterns={"/verify-otp"})
public class VerifyOtpController extends HttpServlet {

    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{
        request.setCharacterEncoding("UTF-8");if(!AccountCsrf.require(request,response))return;
        utils.AccountFormFeedback.clear(request);
        try{
            // [OTXM02]
            // . Trong doPost của VerifyOtpController, em nhận mã vừa nhập rồi gọi verify trong
            //   PasswordRecoveryService. Hàm này kiểm tra mã dựa trên thông tin đã giữ khi gửi email, nên kết
            //   quả xác nhận luôn gắn với đúng lần khôi phục.
            //
            // Tra tiếp: PasswordRecoveryService.java / OTXM03.
            PasswordRecoveryService.shared().verify(request.getSession(),request.getParameter("otp"));

            // [OTXM04]
            // . Khi verify xác nhận mã đúng, em quay lại doPost của VerifyOtpController để mở form đặt mật
            //   khẩu mới. Bước xác nhận kết thúc tại đây; người dùng nhập mật khẩu mới rồi bấm Cập nhật để
            //   tiếp tục.
            //
            // Kết thúc: xác nhận khi form mật khẩu mới được mở.
            response.sendRedirect(request.getContextPath()+"/?tab=reset_password");
        }catch(IllegalArgumentException e){

            String tab = PasswordRecoveryService.shared().nextTab(request.getSession());
            if ("otp".equals(tab)) utils.AccountFormFeedback.flash(request,"verify-otp",java.util.Map.of("otp",e.getMessage()),java.util.Map.of());
            request.getSession().setAttribute("errorMsg",e.getMessage());response.sendRedirect(request.getContextPath()+"/?tab="+tab);
        }
    }
}
