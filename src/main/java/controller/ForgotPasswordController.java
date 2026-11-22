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
            // . Khi nhận email tại doPost trong ForgotPasswordController.java, em gọi start trong
            //   PasswordRecoveryService.java để tạo và gửi OTP. Em truyền session, email và địa chỉ gửi yêu cầu để
            //   kiểm tra tài khoản cùng giới hạn gửi mã trước khi gửi thư.
            // ... Đọc tiếp tại PasswordRecoveryService.java, tìm [OTG03].

            recovery.start(session,request.getParameter("email"),request.getRemoteAddr());

            // [OTG06]
            // . Khi gửi mã thành công, chương trình quay về doPost trong ForgotPasswordController.java. Em đặt thông
            //   báo và mở Trang chủ với tab=otp để auth_modal.jsp hiện bước nhập mã vừa gửi.
            // ... Đọc tiếp tại auth_modal.jsp, tìm [OTG07].
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
