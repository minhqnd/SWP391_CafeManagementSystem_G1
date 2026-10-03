package controller;

import dao.UserDAO;
import model.User;

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
        String usernameInput = request.getParameter("username");
        String password = request.getParameter("password");
        
        // Mặc định tất cả user đăng ký mới từ web đều là Khách hàng
        String dbRole = "CUSTOMER";
        String dbPosition = null;
        
        String email = null;
        String phone = null;
        
        if (usernameInput != null && usernameInput.contains("@")) {
            email = usernameInput;
            phone = String.valueOf(System.currentTimeMillis()); // Generate a placeholder phone since DB requires it to be UNIQUE
        } else {
            phone = usernameInput;
        }

        // Create new User object
        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setRole(dbRole);
        newUser.setPosition(dbPosition);
        
        boolean isSuccess = userDAO.register(newUser, password);
        
        if (isSuccess) {
            request.getSession().setAttribute("successMsg", "Đăng ký thành công! Vui lòng đăng nhập.");
            response.sendRedirect(request.getContextPath() + "/?tab=login");
        } else {
            request.getSession().setAttribute("errorMsg", "Đăng ký thất bại. Email hoặc Số điện thoại có thể đã tồn tại.");
            response.sendRedirect(request.getContextPath() + "/?tab=register");
        }
    }
}
