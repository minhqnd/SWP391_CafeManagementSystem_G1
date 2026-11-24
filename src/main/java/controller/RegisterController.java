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

        // [DKL03]
        // . doPost trong RegisterController.java nhận các ô đăng ký. Em bỏ khoảng trắng đầu/cuối, chuyển email
        //   về chữ thường và làm gọn số điện thoại, rồi gọi AccountValidation để kiểm tra từng ô trước khi tìm
        //   trùng và tạo tài khoản.
        // ... Đọc tiếp tại RegisterController.java, tìm [DKL04].
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
        // [DKL04]
        // . Ở bước kiểm tra, em gom lời báo của từng ô vào errors, gồm lỗi định dạng và lỗi email, số điện thoại
        //   đã được dùng. Nếu còn lỗi thì trả form để sửa; chỉ khi errors rỗng em mới chuẩn bị tài khoản mới.
        // ... Đọc tiếp trong RegisterController.java, tìm [DKL05] khi errors rỗng; nhánh còn lỗi giữ form và mở
        //     lại tab Đăng ký.
        java.util.Map<String,String> errors = new java.util.LinkedHashMap<>();

        // [DKHT03]
        // . Để kiểm tra họ tên, ở doPost trong RegisterController.java em truyền fullName vào nameError (kiểm
        //   tra họ tên và trả lời báo lỗi) trong AccountValidation.java. Hàm trả về lời báo nếu tên sai hoặc
        //   null nếu hợp lệ để em quyết định có tiếp tục tạo tài khoản không.
        // ... Đọc tiếp tại AccountValidation.java, tìm [KTHT01].
        String nameError = AccountValidation.nameError(fullName);

        // [DKDT03]
        // . Để kiểm tra số điện thoại, ở doPost trong RegisterController.java em truyền phone đã chuẩn hóa vào
        //   AccountValidation.phoneError (kiểm tra định dạng số điện thoại). Số phải đủ 10 chữ số và bắt đầu
        //   bằng 0; đúng định dạng rồi em mới tìm xem số đã có tài khoản chưa.
        // ... Đọc tiếp tại AccountValidation.java, tìm [KTDT01].
        String phoneError = AccountValidation.phoneError(phone);

        // [DKEM03]
        // . Để kiểm tra email, ở doPost trong RegisterController.java em truyền địa chỉ đã chuyển về chữ thường
        //   vào AccountValidation.emailError (kiểm tra định dạng và độ dài email). Hàm kiểm tra định dạng và độ
        //   dài; email đúng rồi em mới tìm xem đã có người dùng chưa.
        // ... Đọc tiếp tại AccountValidation.java, tìm [KTEM01].
        String emailError = AccountValidation.emailError(email);
        if (nameError != null) errors.put("fullname", nameError);
        if (phoneError != null) errors.put("phone", phoneError);
        if (emailError != null) errors.put("email", emailError);

        // [DKMK03]
        // . Để kiểm tra mật khẩu, ở doPost trong RegisterController.java em gọi passwordErrors (kiểm tra mật
        //   khẩu và xác nhận theo từng ô) trong AccountValidation.java. Em truyền mật khẩu, xác nhận và tên hai
        //   ô để kiểm tra điều kiện mật khẩu, độ khớp, rồi đưa lỗi về đúng ô cần sửa.
        // ... Đọc tiếp tại AccountValidation.java, tìm [KTMK01].
        errors.putAll(AccountValidation.passwordErrors(password, confirmPassword, "password", "confirm_password"));

        // [DKTR01]
        // . Sau khi số điện thoại và email đúng định dạng, ở doPost trong RegisterController.java em gọi
        //   checkPhoneExists (tìm số điện thoại đã được dùng) và checkEmailExists (tìm email đã được dùng) trong
        //   UserDAO.java. Em truyền từng liên hệ để tìm tài khoản đã dùng; nếu trùng thì báo tại ô tương ứng và
        //   chưa tạo tài khoản.
        // ... Trong RegisterController.java, đọc tiếp hai lời gọi checkPhoneExists và checkEmailExists bên dưới;
        //     kết quả được thêm vào errors rồi xét trước khi tạo User.
        boolean lookupFailed = false;
        try {
            if (phoneError == null && userDAO.checkPhoneExists(phone)) errors.put("phone", "Số điện thoại đã tồn tại trong hệ thống.");
            if (emailError == null && userDAO.checkEmailExists(email)) errors.put("email", "Email đã tồn tại trong hệ thống.");
        } catch (utils.AccountDataException e) {
            lookupFailed = true;
        }

        // [DKLOI01]
        // . Khi đăng ký có lỗi, ở doPost trong RegisterController.java em gọi flash (giữ lỗi và các ô qua lần
        //   chuyển trang) trong AccountFormFeedback.java, truyền lỗi cùng họ tên, số điện thoại và email. Dữ
        //   liệu được giữ qua lần mở lại trang để account_feedback.jsp và account-validation.js điền lại ô, hiện
        //   lỗi cho người dùng sửa.
        // ... Trong RegisterController.java, đọc tiếp sendRedirect ngay dưới phần flash: trình duyệt mở lại tab
        //     Đăng ký, hiện lỗi và dừng lần gửi này.
        if (!errors.isEmpty() || lookupFailed) {

            utils.AccountFormFeedback.flash(request, "register", errors,
                    java.util.Map.of("fullname", fullName, "phone", phone, "email", email));
            request.getSession().setAttribute("errorMsg", lookupFailed
                    ? "Không kiểm tra được thông tin tài khoản lúc này. Chưa tạo tài khoản. Vui lòng thử lại sau."
                    : "Vui lòng kiểm tra các trường được đánh dấu bên dưới.");
            response.sendRedirect(request.getContextPath() + backPath + "?tab=register");
            return;
        }

        // . Khi đăng ký không còn lỗi, em tạo User với vai trò CUSTOMER rồi truyền tài khoản và mật khẩu vào
        //   UserDAO.register (tạo tài khoản khách hàng) để ghi. Em không tạo tài khoản trước bước kiểm tra vì
        //   thông tin sai phải được sửa rồi mới lưu.
        String dbRole = "CUSTOMER";
        String dbPosition = null;

        User newUser = new User();
        newUser.setFullName(fullName.trim());
        newUser.setEmail(email.trim());
        newUser.setPhone(phone);
        newUser.setRole(dbRole);
        newUser.setPosition(dbPosition);

        // [DKL05]
        // . Khi thông tin đều hợp lệ, em tạo User khách hàng rồi gọi register trong UserDAO.java để ghi tài
        //   khoản. Em truyền User và mật khẩu vừa nhập, nhận isSuccess để biết có tạo được tài khoản hay cần báo
        //   lỗi.
        // ... Đọc tiếp tại UserDAO.java, tìm [DKL06].
        boolean isSuccess;
        try {
            isSuccess = userDAO.register(newUser, password);
        } catch (utils.AccountDataException e) {
            isSuccess = false;
        }

        // [DKL07]
        // . Khi nhận kết quả từ UserDAO.register, em quay lại doPost trong RegisterController.java để đặt thông
        //   báo. Tạo được thì chuyển về tab Đăng nhập; chưa tạo được thì mở lại Đăng ký và báo lỗi cho người
        //   dùng.
        // ... Đọc tiếp tại auth_modal.jsp, tìm [DKL08].
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
