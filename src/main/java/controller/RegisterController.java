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

        request.setCharacterEncoding("UTF-8");
        if (!AccountCsrf.require(request, response)) return;
        utils.AccountFormFeedback.clear(request);

        // [DKL02]
        // . Khi nhận form đăng ký, doPost trong RegisterController kiểm tra các ô và xem email, số điện
        //   thoại đã được dùng chưa. Nếu có lỗi, em trả lời báo cùng các ô được phép giữ lại để người
        //   dùng sửa. Khi thông tin đều đúng, em gọi register trong UserDAO để ghi tài khoản mới.
        //
        // Tra tiếp: UserDAO.java / DKL03.
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

        String nameError = AccountValidation.nameError(fullName);

        String phoneError = AccountValidation.phoneError(phone);

        String emailError = AccountValidation.emailError(email);
        if (nameError != null) errors.put("fullname", nameError);
        if (phoneError != null) errors.put("phone", phoneError);
        if (emailError != null) errors.put("email", emailError);

        errors.putAll(AccountValidation.passwordErrors(password, confirmPassword, "password", "confirm_password"));

        boolean lookupFailed = false;
        try {
            if (phoneError == null && userDAO.checkPhoneExists(phone)) errors.put("phone", "Số điện thoại đã tồn tại trong hệ thống.");
            if (emailError == null && userDAO.checkEmailExists(email)) errors.put("email", "Email đã tồn tại trong hệ thống.");
        } catch (utils.AccountDataException e) {
            lookupFailed = true;
        }

        if (!errors.isEmpty() || lookupFailed) {

            utils.AccountFormFeedback.flash(request, "register", errors,
                    java.util.Map.of("fullname", fullName, "phone", phone, "email", email));
            request.getSession().setAttribute("errorMsg", lookupFailed
                    ? "Không kiểm tra được thông tin tài khoản lúc này. Chưa tạo tài khoản. Vui lòng thử lại sau."
                    : "Vui lòng kiểm tra các trường được đánh dấu bên dưới.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
            return;
        }

        String dbRole = "CUSTOMER";
        String dbPosition = null;

        User newUser = new User();
        newUser.setFullName(fullName.trim());
        newUser.setEmail(email.trim());
        newUser.setPhone(phone);
        newUser.setRole(dbRole);
        newUser.setPosition(dbPosition);

        boolean isSuccess;
        try {
            isSuccess = userDAO.register(newUser, password);
        } catch (utils.AccountDataException e) {
            isSuccess = false;
        }

        // [DKL04]
        // . Sau khi register trả kết quả, em quay lại doPost của RegisterController để chọn thông báo phù
        //   hợp. Tạo được tài khoản thì mở phần đăng nhập; nếu không thì giữ phần đăng ký để người dùng xử lý
        //   tiếp.
        //
        // Kết thúc: đăng ký: thành công mở Đăng nhập, có lỗi giữ phần đăng ký để sửa.
        if (isSuccess) {
            request.getSession().setAttribute("successMsg", "Đăng ký thành công! Vui lòng đăng nhập.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=login");
        } else {
            try {
                if (userDAO.checkPhoneExists(phone)) errors.put("phone", "Số điện thoại đã tồn tại trong hệ thống.");
                if (userDAO.checkEmailExists(email)) errors.put("email", "Email đã tồn tại trong hệ thống.");
            } catch (utils.AccountDataException ignored) { }
            utils.AccountFormFeedback.flash(request, "register", errors,
                    java.util.Map.of("fullname", fullName, "phone", phone, "email", email));
            request.getSession().setAttribute("errorMsg", errors.isEmpty()
                    ? "Không tạo được tài khoản lúc này. Vui lòng thử lại sau."
                    : "Thông tin vừa được tài khoản khác sử dụng. Vui lòng kiểm tra các ô được đánh dấu.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
        }
    }
}
