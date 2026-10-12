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
        
        String rawPhone = request.getParameter("phone");
        String phone = (rawPhone != null && !rawPhone.trim().isEmpty()) ? rawPhone.trim().replaceAll("\\s+", "") : null;

        if (phone != null && !phone.isEmpty()) {
            String[] outPasswordHash = new String[1];
            User existingUserByPhone = userDAO.findUserByPhoneWithPassword(phone, outPasswordHash);

            if (existingUserByPhone != null) {
                // Kiểm tra xem đây có phải tài khoản tạo nhanh tại quầy chưa kích hoạt không
                if (UserDAO.UNACTIVATED_PASSWORD_HASH.equals(outPasswordHash[0])) {
                    if (email != null && !email.trim().isEmpty() && userDAO.checkEmailExists(email)) {
                        request.getSession().setAttribute("errorMsg", "Email đã tồn tại trong hệ thống.");
                        String redirect = request.getParameter("redirect");
                        String backPath = RedirectUtil.isSafeLocalPath(redirect) ? redirect : "/";
                        response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
                        return;
                    }

                    boolean activated = userDAO.activateCounterCustomer(existingUserByPhone.getId(), fullName, email, password);
                    String redirect = request.getParameter("redirect");
                    String backPath = RedirectUtil.isSafeLocalPath(redirect) ? redirect : "/";

                    if (activated) {
                        int pts = existingUserByPhone.getTotalPoints();
                        String msg = pts > 0 
                            ? ("Đăng ký thành công! Số điện thoại của bạn đã có " + pts + " điểm tích lũy tại quầy và đã được đồng bộ vào tài khoản.")
                            : "Đăng ký thành công! Tài khoản của bạn đã được kích hoạt. Vui lòng đăng nhập.";
                        request.getSession().setAttribute("successMsg", msg);
                        response.sendRedirect(request.getContextPath() + backPath + "?tab=login");
                        return;
                    } else {
                        request.getSession().setAttribute("errorMsg", "Lỗi khi kích hoạt tài khoản. Vui lòng thử lại sau.");
                        response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
                        return;
                    }
                } else {
                    request.getSession().setAttribute("errorMsg", "Số điện thoại này đã được đăng ký tài khoản. Vui lòng đăng nhập.");
                    String redirect = request.getParameter("redirect");
                    String backPath = RedirectUtil.isSafeLocalPath(redirect) ? redirect : "/";
                    response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
                    return;
                }
            }
        } else {
            phone = String.valueOf(System.currentTimeMillis()); // Generate a placeholder phone since DB requires it to be UNIQUE
        }

        // Create new User object
        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setRole(dbRole);
        newUser.setPosition(dbPosition);
        
        // Kiểm tra email đã tồn tại chưa
        if (email != null && !email.trim().isEmpty() && userDAO.checkEmailExists(email)) {
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
