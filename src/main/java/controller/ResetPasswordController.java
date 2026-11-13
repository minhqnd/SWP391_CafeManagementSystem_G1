package controller;

import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

// BF10 - ĐẶT LẠI MẬT KHẨU: chỉ nhận sau khi VerifyOtpController đã đánh dấu canResetPassword.
@WebServlet(name = "ResetPasswordController", urlPatterns = {"/reset-password"})
public class ResetPasswordController extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        String newPassword = request.getParameter("password");
        // OT07: . Ở đây em kiểm tra phiên đã xác minh mã và hai mật khẩu có khớp nhau không.
        // .. Lưu thành công thì em xóa quyền khôi phục và yêu cầu đăng nhập lại.
        // ... UserDAO.updatePassword(email trong phiên, mật khẩu mới) [PW03].
        String confirmPassword = request.getParameter("confirm_password");
        
        HttpSession session = request.getSession();
        Boolean canReset = (Boolean) session.getAttribute("canResetPassword");
        String forgotEmail = (String) session.getAttribute("forgotEmail");
        
        if (canReset == null || !canReset || forgotEmail == null) {
            session.setAttribute("errorMsg", "Phiên đổi mật khẩu không hợp lệ hoặc đã hết hạn.");
            response.sendRedirect(request.getContextPath() + "/?tab=forgot");
            return;
        }

        if (newPassword == null || newPassword.length() < 6 || !newPassword.equals(confirmPassword)) {
            session.setAttribute("errorMsg", "Mật khẩu xác nhận không khớp.");
            response.sendRedirect(request.getContextPath() + "/?tab=reset_password");
            return;
        }

        // Email đích lấy từ phiên khôi phục, không nhận tùy ý từ form reset.
        // Hàm hiện kiểm tra xác nhận khớp; không tự giả định đã có kiểm tra độ dài ở server tại đây.
        boolean isUpdated = userDAO.updatePassword(forgotEmail, newPassword);
        
        if (isUpdated) {
            // Clean up session
            session.removeAttribute("canResetPassword");
            session.removeAttribute("forgotEmail");
            
            session.setAttribute("successMsg", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            response.sendRedirect(request.getContextPath() + "/?tab=login");
        } else {
            session.setAttribute("errorMsg", "Đã xảy ra lỗi khi cập nhật mật khẩu.");
            response.sendRedirect(request.getContextPath() + "/?tab=reset_password");
        }
    }
}
