package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import service.PasswordRecoveryService;
import utils.AccountCsrf;

@WebServlet(name="ForgotPasswordController",urlPatterns={"/forgot-password"})
public class ForgotPasswordController extends HttpServlet {
    private final PasswordRecoveryService recovery;
    public ForgotPasswordController(){this(PasswordRecoveryService.shared());}
    ForgotPasswordController(PasswordRecoveryService recovery){this.recovery=recovery;}
    // OT05: . Nhận email khôi phục từ JavaScript [OT04] và kiểm tra CSRF.
    // .. Thành công mở tab OTP; lỗi thì giữ email an toàn và trả thông báo.
    // ... PasswordRecoveryService.start() [OT06] → form nhập OTP [OT07].
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{
        request.setCharacterEncoding("UTF-8");
        if(!AccountCsrf.require(request,response))return;
        HttpSession session=request.getSession();
        try{
            recovery.start(session,request.getParameter("email"),request.getRemoteAddr());
            session.setAttribute("successMsg","Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra cả thư rác.");
            response.sendRedirect(request.getContextPath()+"/?tab=otp");
        }catch(IllegalArgumentException e){
            String email=utils.AccountValidation.email(request.getParameter("email"));
            java.util.Map<String,String> errors=new java.util.LinkedHashMap<>();
            if(e instanceof utils.RecoveryFailure failure && failure.field()!=null)errors.put(failure.field(),e.getMessage());
            utils.AccountFormFeedback.flash(request,"forgot-password",errors,java.util.Map.of("email",email));
            session.setAttribute("errorMsg",errors.isEmpty()?e.getMessage():"Chưa gửi được OTP. Vui lòng kiểm tra email bên dưới.");
            response.sendRedirect(request.getContextPath()+"/?tab=forgot");
        }
    }
}
