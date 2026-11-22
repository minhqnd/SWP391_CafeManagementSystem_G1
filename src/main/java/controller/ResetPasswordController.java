package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import service.PasswordRecoveryService;
import utils.AccountCsrf;

@WebServlet(name="ResetPasswordController",urlPatterns={"/reset-password"})
public class ResetPasswordController extends HttpServlet {

    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{
        request.setCharacterEncoding("UTF-8");if(!AccountCsrf.require(request,response))return;
        utils.AccountFormFeedback.clear(request);
        try{
            // [OTDM02]
            // . Khi nhận hai ô tại doPost trong ResetPasswordController.java, em gọi reset trong
            //   PasswordRecoveryService.java để đặt lại mật khẩu. Em truyền session, mật khẩu mới và xác nhận để chỉ
            //   xử lý cho tài khoản đã xác minh OTP trong lần yêu cầu này.
            // ... Đọc tiếp tại PasswordRecoveryService.java, tìm [OTDM03].
            PasswordRecoveryService.shared().reset(request.getSession(),request.getParameter("password"),request.getParameter("confirm_password"));

            // [OTDM05]
            // . Khi reset đổi mật khẩu thành công, chương trình quay về doPost trong ResetPasswordController.java.
            //   Em đặt thông báo rồi mở tab Đăng nhập để người dùng đăng nhập bằng mật khẩu mới.
            // ... Đọc tiếp tại auth_modal.jsp, tìm [DNM01].
            AccountCsrf.rotate(request.getSession());
            request.getSession().setAttribute("successMsg","Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            response.sendRedirect(request.getContextPath()+"/?tab=login");
        }catch(IllegalArgumentException e){
            // . Nếu chưa đổi được, em gọi nextTab trong PasswordRecoveryService.java để biết còn được sửa
            //   mật khẩu mới hay phải xin lại OTP. Lỗi mật khẩu chỉ đưa về hai ô khi lần xác minh vẫn còn hạn.
            java.util.Map<String,String> errors=utils.AccountValidation.passwordErrors(request.getParameter("password"),request.getParameter("confirm_password"),"password","confirm_password");
            String tab = PasswordRecoveryService.shared().nextTab(request.getSession());
            if(!errors.isEmpty() && "reset_password".equals(tab))utils.AccountFormFeedback.flash(request,"reset-password",errors,java.util.Map.of());
            request.getSession().setAttribute("errorMsg",e.getMessage());response.sendRedirect(request.getContextPath()+"/?tab="+tab);
        }
    }
}
