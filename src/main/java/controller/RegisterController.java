package controller;

import dao.UserDAO;
import model.User;
import utils.RedirectUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "RegisterController", urlPatterns = {"/register"})
public class RegisterController extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        
        String fullName = request.getParameter("fullname");
        String email = request.getParameter("email");
        if (email == null || email.trim().isEmpty()) {
            email = request.getParameter("username");
        }
        String password = request.getParameter("password");
        
        // Mặc định tất cả user đăng ký mới từ web đều là Khách hàng
        String dbRole = "CUSTOMER";
        String dbPosition = null;
        
        String phone = String.valueOf(System.currentTimeMillis()); // Generate a placeholder phone since DB requires it to be UNIQUE

        // Create new User object
        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setRole(dbRole);
        newUser.setPosition(dbPosition);
        
        // Kiểm tra email đã tồn tại chưa
        if (userDAO.checkEmailExists(email)) {
            request.getSession().setAttribute("errorMsg", "Email đã tồn tại trong hệ thống.");
            String redirect = request.getParameter("redirect");
            String backPath = RedirectUtil.isSafeLocalPath(redirect) ? redirect : "/";
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
            return;
        }

        boolean isSuccess = userDAO.register(newUser, password);

        // Đăng ký từ trang Checkout -> quay lại đúng trang đó để đăng nhập và đặt hàng tiếp
        String redirect = request.getParameter("redirect");
        String backPath = RedirectUtil.isSafeLocalPath(redirect) ? redirect : "/";

        if (isSuccess) {
            request.getSession().setAttribute("successMsg", "Đăng ký thành công! Vui lòng đăng nhập.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=login");
        } else {
            request.getSession().setAttribute("errorMsg", "Lỗi hệ thống khi đăng ký. Vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
        }
    }
}
