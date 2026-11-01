package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.User;

@WebServlet(name = "ProfileController", urlPatterns = {"/profile"})
public class ProfileController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            // Require login
            response.sendRedirect(request.getContextPath() + "/?login=true");
            return;
        }

        dao.UserDAO userDAO = new dao.UserDAO();
        java.util.List<model.LoyaltyTransaction> history = userDAO.getLoyaltyHistoryByUserId(((User)session.getAttribute("user")).getId());
        request.setAttribute("loyaltyHistory", history);

        // Mốc điểm Admin cài đặt: điểm Aura trừ thẳng vào đơn ở Giỏ hàng
        request.setAttribute("shopSettings", new dao.ShopSettingDAO().getSettings());

        request.getRequestDispatcher("/profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/?login=true");
            return;
        }

        User user = (User) session.getAttribute("user");
        
        String action = request.getParameter("action");
        if ("update".equals(action)) {
            String fullName = request.getParameter("fullName");
            String phone = request.getParameter("phone");
            if (phone == null) {
                phone = user.getPhone(); // Giữ nguyên sđt cũ do đã ẩn field này
            }
            String email = request.getParameter("email");
            if (email == null) {
                email = user.getEmail(); // Giữ nguyên email
            }
            String birthday = request.getParameter("birthday");
            String address = request.getParameter("address");
            
            // Backend validation
            if (fullName == null || fullName.trim().isEmpty()) {
                request.setAttribute("errorMsg", "Họ và tên không được để trống!");
            } else if (phone != null && !phone.trim().isEmpty() && !phone.matches("^[0-9]{9,15}$")) {
                request.setAttribute("errorMsg", "Số điện thoại không hợp lệ!");
            } else if (email != null && !email.trim().isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                request.setAttribute("errorMsg", "Địa chỉ email không hợp lệ!");
            } else {
                dao.UserDAO userDAO = new dao.UserDAO();
                String updateResult = userDAO.updateUserProfile(user.getId(), fullName, phone, email, birthday, address);
                
                if ("success".equals(updateResult)) {
                    user.setFullName(fullName);
                    user.setPhone(phone);
                    user.setEmail(email);
                    user.setBirthday(birthday);
                    user.setAddress(address);
                    session.setAttribute("user", user);
                    request.setAttribute("successMsg", "Cập nhật thông tin thành công!");
                } else {
                    request.setAttribute("errorMsg", updateResult);
                }
            }
        } else if ("changePassword".equals(action)) {
            String oldPassword = request.getParameter("oldPassword");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");
            
            if (newPassword == null || newPassword.length() < 6) {
                request.setAttribute("errorMsg", "Mật khẩu mới phải dài ít nhất 6 ký tự!");
            } else if (!newPassword.equals(confirmPassword)) {
                request.setAttribute("errorMsg", "Mật khẩu xác nhận không khớp!");
            } else {
                dao.UserDAO userDAO = new dao.UserDAO();
                // Verify old password by trying to log in
                if (userDAO.login(user.getEmail(), oldPassword) != null) {
                    boolean success = userDAO.updatePassword(user.getEmail(), newPassword);
                    if (success) {
                        request.setAttribute("successMsg", "Đổi mật khẩu thành công!");
                    } else {
                        request.setAttribute("errorMsg", "Đổi mật khẩu thất bại. Vui lòng thử lại!");
                    }
                } else {
                    request.setAttribute("errorMsg", "Mật khẩu hiện tại không đúng!");
                }
            }
        }
        
        java.util.List<model.LoyaltyTransaction> history = new dao.UserDAO().getLoyaltyHistoryByUserId(((User)session.getAttribute("user")).getId());
        request.setAttribute("loyaltyHistory", history);
        
        request.setAttribute("shopSettings", new dao.ShopSettingDAO().getSettings());
        
        request.getRequestDispatcher("/profile.jsp").forward(request, response);
    }
}
