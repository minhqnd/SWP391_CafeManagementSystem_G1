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
            // . Trong doPost của ResetPasswordController, em nhận hai ô mật khẩu rồi gọi reset trong
            //   PasswordRecoveryService. Hàm này dùng lần xác nhận email đang giữ để kiểm tra người dùng còn
            //   được phép đặt mật khẩu mới hay không.
            //
            // Tra tiếp: PasswordRecoveryService.java / OTDM03.
            PasswordRecoveryService.shared().reset(request.getSession(),request.getParameter("password"),request.getParameter("confirm_password"));

            // [OTDM05]
            // . Khi reset hoàn tất thành công, em quay lại doPost của ResetPasswordController để báo đã đổi
            //   mật khẩu và mở phần đăng nhập. Người dùng có thể đăng nhập bằng mật khẩu mới ngay sau bước
            //   này.
            //
            // Kết thúc: khôi phục khi phần đăng nhập được mở kèm thông báo.
            AccountCsrf.rotate(request.getSession());
            request.getSession().setAttribute("successMsg","Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            response.sendRedirect(request.getContextPath()+"/?tab=login");
        }catch(IllegalArgumentException e){

            java.util.Map<String,String> errors=utils.AccountValidation.passwordErrors(request.getParameter("password"),request.getParameter("confirm_password"),"password","confirm_password");
            String tab = PasswordRecoveryService.shared().nextTab(request.getSession());
            if(!errors.isEmpty() && "reset_password".equals(tab))utils.AccountFormFeedback.flash(request,"reset-password",errors,java.util.Map.of());
            request.getSession().setAttribute("errorMsg",e.getMessage());response.sendRedirect(request.getContextPath()+"/?tab="+tab);
        }
    }
}
