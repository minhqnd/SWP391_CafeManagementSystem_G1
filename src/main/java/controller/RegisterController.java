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
        
        // RG05: . Nhận POST /register từ JavaScript [RG04], kiểm tra CSRF rồi đọc dữ liệu.
        // .. Sai token thì dừng; đúng token mới chuẩn hóa các giá trị từ form.
        // ... Kiểm tra họ tên, SĐT, email, mật khẩu và xác nhận tại [RG06].
        request.setCharacterEncoding("UTF-8");
        if (!AccountCsrf.require(request, response)) return;
        
        // . Đọc fullname, email, phone, password và confirm_password từ request trong bước [RG05].
        // .. Chuẩn hóa tên/email/SĐT; mật khẩu giữ nguyên. Email trống thì thử tham số username cũ.
        // ... Kiểm tra từng trường [RG06] rồi kiểm tra trùng [RG07].
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
        java.util.Map<String,String> errors = new java.util.LinkedHashMap<>();
        // RG06: . Kiểm tra dữ liệu đăng ký trên server, bắt đầu từ họ tên.
        // .. Mỗi lỗi được gắn theo tên ô; server kiểm tra lại dù đã qua JavaScript.
        // RG06a: . fullname từ ô [RG02a] được kiểm tra bằng AccountValidation.nameError().
        // ... Các trường tiếp theo ở [RG06b], [RG06c], [RG06d]; sau đó kiểm tra trùng [RG07].
        String nameError = AccountValidation.nameError(fullName);
        // RG06b: . Kiểm tra phone từ ô Số điện thoại [RG02b].
        // ... AccountValidation.phoneError(); đúng định dạng mới kiểm tra trùng [RG07a].
        String phoneError = AccountValidation.phoneError(phone);
        // RG06c: . Kiểm tra email từ ô Email [RG02c].
        // ... AccountValidation.emailError(); đúng định dạng mới kiểm tra trùng [RG07b].
        String emailError = AccountValidation.emailError(email);
        if (nameError != null) errors.put("fullname", nameError);
        if (phoneError != null) errors.put("phone", phoneError);
        if (emailError != null) errors.put("email", emailError);
        // RG06d: . Kiểm tra password và confirm_password từ [RG02d], [RG02e].
        // .. Lỗi mật khẩu và xác nhận được gắn riêng cho từng ô.
        // ... AccountValidation.passwordErrors() gọi quy tắc mật khẩu AccountValidation.passwordError().
        errors.putAll(AccountValidation.passwordErrors(password, confirmPassword, "password", "confirm_password"));
        // RG07: . Chỉ kiểm tra trùng khi SĐT/email đúng định dạng.
        // .. Có bản ghi trùng thì thêm lỗi vào phone hoặc email.
        // ... UserDAO.checkPhoneExists() [RG07a] và checkEmailExists() [RG07b] trong UserDAO.java.
        if (phoneError == null && userDAO.checkPhoneExists(phone)) errors.put("phone", "Số điện thoại đã tồn tại trong hệ thống.");
        if (emailError == null && userDAO.checkEmailExists(email)) errors.put("email", "Email đã tồn tại trong hệ thống.");
        // RG08: [SLBF10] Information is valid?
        // . Nhánh "Information is valid?" xét errors sau khi kiểm tra từng trường và kiểm tra trùng.
        // .. No: trả lỗi và giữ dữ liệu an toàn, cho sửa rồi bấm lại [RG03]. Yes: đi tiếp tạo tài khoản [RG09].
        // ... AccountFormFeedback.flash() → account_feedback.jsp → feedback() hiển thị lỗi dưới ô.
        if (!errors.isEmpty()) {
            // [SLBF10] Show error for invalid field
            // . Nhánh No trả lỗi về từng ô; người dùng sửa dữ liệu rồi bấm lại [RG03].
            utils.AccountFormFeedback.flash(request, "register", errors,
                    java.util.Map.of("fullname", fullName, "phone", phone, "email", email));
            request.getSession().setAttribute("errorMsg", "Vui lòng kiểm tra các trường được đánh dấu bên dưới.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
            return;
        }

        // RG09: [SLBF10] Create account
        // . Đóng gói dữ liệu hợp lệ vào User và gán role=CUSTOMER tại server.
        // ... UserDAO.register() trong UserDAO.java [RG09a] băm và INSERT users; kết quả về [RG10].
        String dbRole = "CUSTOMER";
        String dbPosition = null;

        User newUser = new User();
        newUser.setFullName(fullName.trim());
        newUser.setEmail(email.trim());
        newUser.setPhone(phone);
        newUser.setRole(dbRole);
        newUser.setPosition(dbPosition);

        boolean isSuccess = userDAO.register(newUser, password);

        // RG10: . Nhận kết quả tạo tài khoản từ DAO [RG09a].
        // .. True: đặt successMsg và mở tab Login; false: báo lỗi và quay lại đăng ký.
        // ... auth_modal.jsp hiển thị thông báo thành công [RG11].
        if (isSuccess) {
            request.getSession().setAttribute("successMsg", "Đăng ký thành công! Vui lòng đăng nhập.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=login");
        } else {
            request.getSession().setAttribute("errorMsg", "Lỗi hệ thống khi đăng ký. Vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
        }
    }
}
