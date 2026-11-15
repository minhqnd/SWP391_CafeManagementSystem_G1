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
    // OT02: . Nhận email từ nút Gửi mã OTP và kiểm tra token biểu mẫu.
    // .. Email hợp lệ mới được xử lý; lỗi được giữ lại để hiển thị ngay dưới trường email.
    // ... PasswordRecoveryService.start() tìm tài khoản và gửi mã [OT10].
    // .. Thành công mở tab=otp; thất bại quay lại form email với thông báo phù hợp.
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
