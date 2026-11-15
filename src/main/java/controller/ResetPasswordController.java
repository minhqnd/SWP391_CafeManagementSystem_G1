package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import service.PasswordRecoveryService;
import utils.AccountCsrf;

@WebServlet(name="ResetPasswordController",urlPatterns={"/reset-password"})
public class ResetPasswordController extends HttpServlet {
    // OT07: . Nhận password và confirm_password từ nút Cập nhật mật khẩu.
    // ... PasswordRecoveryService.reset() kiểm tra quyền khôi phục và đặt mật khẩu mới [OT13].
    // .. Thành công mở tab đăng nhập; thất bại giữ form đặt mật khẩu kèm lỗi.
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{
        request.setCharacterEncoding("UTF-8");if(!AccountCsrf.require(request,response))return;
        try{
            PasswordRecoveryService.shared().reset(request.getSession(),request.getParameter("password"),request.getParameter("confirm_password"));
            AccountCsrf.rotate(request.getSession());
            request.getSession().setAttribute("successMsg","Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            response.sendRedirect(request.getContextPath()+"/?tab=login");
        }catch(IllegalArgumentException e){
            java.util.Map<String,String> errors=utils.AccountValidation.passwordErrors(request.getParameter("password"),request.getParameter("confirm_password"),"password","confirm_password");
            if(!errors.isEmpty())utils.AccountFormFeedback.flash(request,"reset-password",errors,java.util.Map.of());
            request.getSession().setAttribute("errorMsg",e.getMessage());response.sendRedirect(request.getContextPath()+"/?tab=reset_password");
        }
    }
}
