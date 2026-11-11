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

// BF10 - ĐĂNG KÝ: popup auth_modal.jsp gửi POST /register; đây không phải trang GET riêng.
@WebServlet(name = "RegisterController", urlPatterns = {"/register"})
public class RegisterController extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        
        String fullName = request.getParameter("fullname");
        // RG02: . Ở đây em kiểm tra thông tin đăng ký có đầy đủ và đúng định dạng không.
        // .. Hợp lệ thì em kiểm tra trùng rồi tạo tài khoản khách hàng.
        // ... checkPhoneExists()/checkEmailExists() [RG03] → register() [RG04].
        String email = request.getParameter("email");
        if (email == null || email.trim().isEmpty()) {
            email = request.getParameter("username");
        }
        String phone = request.getParameter("phone");
        if (phone != null) {
            phone = phone.trim().replaceAll("[\\s.]", "");
        }
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirm_password");
        
        String redirect = request.getParameter("redirect");
        String backPath = RedirectUtil.isSafeLocalPath(redirect) ? redirect : "/";

        // Kiểm tra hợp lệ dữ liệu đầu vào
        if (fullName == null || fullName.trim().isEmpty()) {
            request.getSession().setAttribute("errorMsg", "Họ và tên không được để trống.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
            return;
        }

        if (phone == null || phone.isEmpty()) {
            request.getSession().setAttribute("errorMsg", "Số điện thoại không được để trống.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
            return;
        }

        if (!phone.matches("^(84|0)[3|5|7|8|9][0-9]{8}$") && !phone.matches("^[0-9]{9,11}$")) {
            request.getSession().setAttribute("errorMsg", "Số điện thoại không hợp lệ (VD: 0912345678).");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
            return;
        }

        if (email == null || email.trim().isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            request.getSession().setAttribute("errorMsg", "Địa chỉ email không hợp lệ.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
            return;
        }

        if (password == null || password.length() < 6) {
            request.getSession().setAttribute("errorMsg", "Mật khẩu phải từ 6 ký tự trở lên.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
            return;
        }

        if (confirmPassword != null && !confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
            request.getSession().setAttribute("errorMsg", "Mật khẩu xác nhận không khớp.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
            return;
        }

        // Kiểm tra số điện thoại đã tồn tại chưa
        // Kiểm tra trùng trước khi INSERT; DAO trả boolean, Controller chọn thông báo và tab mở lại.
        if (userDAO.checkPhoneExists(phone)) {
            request.getSession().setAttribute("errorMsg", "Số điện thoại đã tồn tại trong hệ thống.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
            return;
        }

        // Kiểm tra email đã tồn tại chưa
        if (userDAO.checkEmailExists(email)) {
            request.getSession().setAttribute("errorMsg", "Email đã tồn tại trong hệ thống.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
            return;
        }

        // Mặc định tất cả user đăng ký mới từ web đều là Khách hàng
        String dbRole = "CUSTOMER";
        String dbPosition = null;

        // Create new User object
        User newUser = new User();
        newUser.setFullName(fullName.trim());
        newUser.setEmail(email.trim());
        newUser.setPhone(phone);
        newUser.setRole(dbRole);
        newUser.setPosition(dbPosition);

        // Đầu vào hợp lệ -> DAO thêm users với vai trò CUSTOMER; không lấy role từ form.
        boolean isSuccess = userDAO.register(newUser, password);
        // RG05: . Ở đây em nhận kết quả tạo tài khoản và chuẩn bị thông báo.
        // .. Thành công thì mở đăng nhập; lỗi thì mở lại đăng ký.
        // ... sendRedirect() về trang chứa auth_modal.jsp [RG01].

        // KẾT QUẢ: redirect về trang gốc kèm tab=login/register để popup hiển thị đúng bước.
        if (isSuccess) {
            request.getSession().setAttribute("successMsg", "Đăng ký thành công! Vui lòng đăng nhập.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=login");
        } else {
            request.getSession().setAttribute("errorMsg", "Lỗi hệ thống khi đăng ký. Vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
        }
    }
}
