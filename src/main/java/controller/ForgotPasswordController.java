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

    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{
        request.setCharacterEncoding("UTF-8");
        if(!AccountCsrf.require(request,response))return;
        utils.AccountFormFeedback.clear(request);
        HttpSession session=request.getSession();
        try{
            // [OTG02]
            // . Trong doPost của ForgotPasswordController, em nhận email rồi gọi start trong
            //   PasswordRecoveryService. Em giao việc khôi phục cho class này để quản lý email, mã xác nhận
            //   và bước người dùng đang làm trong cùng một lần khôi phục.
            //
            // Tra tiếp: PasswordRecoveryService.java / OTG03.

            recovery.start(session,request.getParameter("email"),request.getRemoteAddr());

            // [OTG06]
            // . Khi start hoàn tất, em quay lại doPost của ForgotPasswordController để đặt thông báo đã gửi
            //   mã và mở form nhập mã. Người dùng đọc email rồi tiếp tục xác nhận trên form này.
            //
            // Kết thúc: gửi mã khi form nhập mã được mở.
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
