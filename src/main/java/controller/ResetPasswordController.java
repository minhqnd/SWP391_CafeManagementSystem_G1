package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import service.PasswordRecoveryService;
import utils.AccountCsrf;

@WebServlet(name="ResetPasswordController",urlPatterns={"/reset-password"})
public class ResetPasswordController extends HttpServlet {
    // OT15: . Nhận password/confirm_password từ JavaScript [OT14] và kiểm tra CSRF.
    // .. Lỗi thì giữ form, không giữ mật khẩu; thành công đi tiếp bước [OT18].
    // ... PasswordRecoveryService.reset() [OT16] kiểm tra quyền và cập nhật dữ liệu.
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{
        request.setCharacterEncoding("UTF-8");if(!AccountCsrf.require(request,response))return;
        try{
            PasswordRecoveryService.shared().reset(request.getSession(),request.getParameter("password"),request.getParameter("confirm_password"));
            // OT18: . Sau khi lưu mật khẩu thành công, thay token và đặt thông báo thành công.
            // ... Chuyển trình duyệt sang form đăng nhập [OT19].
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
