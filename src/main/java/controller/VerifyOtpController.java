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
            // . Khi nhận otp tại doPost trong VerifyOtpController.java, em gọi verify trong
            //   PasswordRecoveryService.java để xác minh mã. Em truyền session và mã vừa nhập để hàm đối chiếu với
            //   mã đã giữ, kiểm tra thời hạn và số lần nhập sai.
            // ... Đọc tiếp tại PasswordRecoveryService.java, tìm [OTXM03].
            PasswordRecoveryService.shared().verify(request.getSession(),request.getParameter("otp"));

            // [OTXM04]
            // . Khi verify xác nhận mã đúng, chương trình quay về doPost trong VerifyOtpController.java. Em chuyển
            //   về Trang chủ với tab=reset_password để auth_modal.jsp hiện form đặt mật khẩu mới cho bước tiếp theo.
            // ... Đọc tiếp tại auth_modal.jsp, tìm [OTXM05].
            response.sendRedirect(request.getContextPath()+"/?tab=reset_password");
        }catch(IllegalArgumentException e){
            // . Nếu chưa xác minh được, em gọi nextTab để chọn bước còn dùng được rồi đưa lời báo sang đó.
            //   Chỉ khi còn ở bước OTP, lỗi mới được gắn vào ô mã; hết hạn thì quay về nhập email xin mã mới.
            // ... Đọc tiếp tại PasswordRecoveryService.java, tìm [OTBUOC01].
            String tab = PasswordRecoveryService.shared().nextTab(request.getSession());
            if ("otp".equals(tab)) utils.AccountFormFeedback.flash(request,"verify-otp",java.util.Map.of("otp",e.getMessage()),java.util.Map.of());
            request.getSession().setAttribute("errorMsg",e.getMessage());response.sendRedirect(request.getContextPath()+"/?tab="+tab);
        }
    }
}
