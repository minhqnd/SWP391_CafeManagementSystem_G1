package controller;

import dao.UserDAO;
import utils.EmailUtil;
import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "ForgotPasswordController", urlPatterns = {"/forgot-password"})
public class ForgotPasswordController extends HttpServlet {
    
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String email = request.getParameter("email");
        
        if (email == null || email.trim().isEmpty()) {
            request.getSession().setAttribute("errorMsg", "Vui lòng nhập địa chỉ email.");
            response.sendRedirect(request.getContextPath() + "/?tab=forgot");
            return;
        }

        // 1. Kiểm tra email có tồn tại không
        boolean exists = userDAO.checkEmailExists(email);
        if (!exists) {
            request.getSession().setAttribute("errorMsg", "Email không tồn tại trong hệ thống.");
            response.sendRedirect(request.getContextPath() + "/?tab=forgot");
            return;
        }

        // 2. Tạo mật khẩu mới (Reset về mặc định luôn cho đơn giản)
        String newPassword = "Aura@" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(); // Ví dụ: Aura@A1B2

        // 3. Cập nhật mật khẩu mới vào Database
        boolean isUpdated = userDAO.updatePassword(email, newPassword);
        
        if (isUpdated) {
            // Hiển thị trực tiếp mật khẩu mới lên màn hình thông báo thay vì gửi qua Email
            request.getSession().setAttribute("successMsg", "Thành công! Mật khẩu mới của bạn là: " + newPassword + " (Vui lòng đăng nhập và đổi lại mật khẩu)");
            response.sendRedirect(request.getContextPath() + "/?tab=login");
        } else {
            request.getSession().setAttribute("errorMsg", "Lỗi hệ thống khi cập nhật mật khẩu.");
            response.sendRedirect(request.getContextPath() + "/?tab=forgot");
        }
    }
}
