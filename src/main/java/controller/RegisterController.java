package controller;

import dao.UserDAO;
import model.User;
import utils.RedirectUtil;
import utils.AccountValidation;
import utils.AccountCsrf;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "RegisterController", urlPatterns = {"/register"})
public class RegisterController extends HttpServlet {

    private final UserDAO userDAO;
    public RegisterController() { this(new UserDAO()); }
    RegisterController(UserDAO userDAO) { this.userDAO = userDAO; }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // RG02: . Nhận POST /register từ form RG01 và kiểm tra csrfToken trước khi xử lý.
        // .. Token đúng thì tiếp tục đọc dữ liệu; token sai thì dừng request.
        // ... AccountCsrf.require() đối chiếu token trong form với session.
        request.setCharacterEncoding("UTF-8");
        if (!AccountCsrf.require(request, response)) return;
        
        // . Nhận fullname, phone, email, password và confirm_password từ form RG01.
        // .. Họ tên, email và SĐT được chuẩn hóa; mật khẩu giữ nguyên để kiểm tra chính xác.
        // ... AccountValidation kiểm tra định dạng từng trường trước khi kiểm tra trùng.
        String fullName = AccountValidation.text(request.getParameter("fullname"));
        String email = request.getParameter("email");
        if (email == null || email.trim().isEmpty()) {
            email = request.getParameter("username");
        }
        email = AccountValidation.email(email);
        String phone = AccountValidation.phone(request.getParameter("phone"));
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirm_password");
        
        String redirect = request.getParameter("redirect");
        String backPath = RedirectUtil.isSafeLocalPath(redirect) ? redirect : "/";
        // . Gom lỗi của họ tên, SĐT, email, mật khẩu và xác nhận theo đúng tên trường trên form.
        // .. Trường sai được đưa vào errors; server vẫn kiểm tra dù người dùng bỏ qua JavaScript.
        // ... Quy tắc kiểm tra nằm trong AccountValidation.java.
        java.util.Map<String,String> errors = new java.util.LinkedHashMap<>();
        String nameError = AccountValidation.nameError(fullName);
        String phoneError = AccountValidation.phoneError(phone);
        String emailError = AccountValidation.emailError(email);
        if (nameError != null) errors.put("fullname", nameError);
        if (phoneError != null) errors.put("phone", phoneError);
        if (emailError != null) errors.put("email", emailError);
        errors.putAll(AccountValidation.passwordErrors(password, confirmPassword, "password", "confirm_password"));
        // . Trường đúng định dạng mới được kiểm tra trùng trong database.
        // ... Email gọi UserDAO.checkEmailExists() [RG03]; SĐT gọi UserDAO.checkPhoneExists() [RG06].
        if (phoneError == null && userDAO.checkPhoneExists(phone)) errors.put("phone", "Số điện thoại đã tồn tại trong hệ thống.");
        if (emailError == null && userDAO.checkEmailExists(email)) errors.put("email", "Email đã tồn tại trong hệ thống.");
        // RG04: . Xử lý danh sách lỗi sau validation và kiểm tra trùng.
        // .. Còn lỗi thì không tạo tài khoản; giữ họ tên/SĐT/email và mở lại form RG01.
        // ... AccountFormFeedback.flash() giữ lỗi để auth_modal.jsp hiển thị lại đúng trường.
        if (!errors.isEmpty()) {
            utils.AccountFormFeedback.flash(request, "register", errors,
                    java.util.Map.of("fullname", fullName, "phone", phone, "email", email));
            request.getSession().setAttribute("errorMsg", "Vui lòng kiểm tra các trường được đánh dấu bên dưới.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
            return;
        }

        // . Gán role=CUSTOMER và position=null tại server; không nhận vai trò từ form.
        // .. Dữ liệu hợp lệ được đóng gói thành User trước khi lưu.
        String dbRole = "CUSTOMER";
        String dbPosition = null;

        User newUser = new User();
        newUser.setFullName(fullName.trim());
        newUser.setEmail(email.trim());
        newUser.setPhone(phone);
        newUser.setRole(dbRole);
        newUser.setPosition(dbPosition);

        // ... UserDAO.register() lưu User và mật khẩu rồi trả kết quả [RG05].
        boolean isSuccess = userDAO.register(newUser, password);

        // RG05: . Nhận kết quả UserDAO.register() sau khi bấm Tạo tài khoản mới.
        // .. DAO trả true thì tài khoản đã được tạo và mở tab đăng nhập [LG01].
        // .. DAO trả false thì tài khoản chưa được tạo và mở lại form đăng ký [RG01].
        if (isSuccess) {
            request.getSession().setAttribute("successMsg", "Đăng ký thành công! Vui lòng đăng nhập.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=login");
        } else {
            request.getSession().setAttribute("errorMsg", "Lỗi hệ thống khi đăng ký. Vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
        }
    }
}
