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
        // . Đến doPost trong RegisterController.java, em nhận các ô đăng ký. Em bỏ khoảng trắng đầu và cuối,
        //   chuyển email về chữ thường và làm gọn số điện thoại, rồi gọi AccountValidation để kiểm tra lại
        //   từng ô trước khi tìm trùng và tạo tài khoản.
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
        // . Em gom lời báo của các ô vào errors, gồm cả lỗi định dạng và lỗi email hoặc số điện thoại đã có
        //   người dùng. Còn lỗi thì em trả form để sửa; không còn lỗi mới chuẩn bị tài khoản mới để lưu.
        // ... Còn lỗi thì doPost giữ các ô và chuyển lại form; đọc nhánh đó tại RegisterController.java, tìm
        //     [DKLOI01]. Nếu errors rỗng, đọc bước tạo User và gọi ghi tại RegisterController.java, tìm
        //     [DKL05].
        java.util.Map<String,String> errors = new java.util.LinkedHashMap<>();

        // [DKHT03]
        // . Để kiểm tra họ tên, ở doPost trong RegisterController.java em truyền fullName vào nameError trong
        //   AccountValidation.java. Hàm trả về lời báo nếu tên sai hoặc null nếu hợp lệ để em quyết định có
        //   tiếp tục tạo tài khoản không.
        // ... Đọc tiếp tại AccountValidation.java, tìm [KTHT01].
        String nameError = AccountValidation.nameError(fullName);

        // [DKDT03]
        // . Để kiểm tra số điện thoại, ở doPost trong RegisterController.java em truyền phone đã chuẩn hóa vào
        //   AccountValidation.phoneError. Số phải đủ 10 chữ số và bắt đầu bằng 0; đúng định dạng rồi em mới
        //   tìm xem số đã có tài khoản chưa.
        // ... Đọc tiếp tại AccountValidation.java, tìm [KTDT01].
        String phoneError = AccountValidation.phoneError(phone);

        // [DKEM03]
        // . Để kiểm tra email, ở doPost trong RegisterController.java em truyền địa chỉ đã chuyển về chữ
        //   thường vào AccountValidation.emailError. Hàm kiểm tra định dạng và độ dài; email đúng rồi em mới
        //   tìm xem đã có người dùng chưa.
        // ... Đọc tiếp tại AccountValidation.java, tìm [KTEM01].
        String emailError = AccountValidation.emailError(email);
        if (nameError != null) errors.put("fullname", nameError);
        if (phoneError != null) errors.put("phone", phoneError);
        if (emailError != null) errors.put("email", emailError);

        // [DKMK03]
        // . Để kiểm tra mật khẩu, ở doPost trong RegisterController.java em gọi passwordErrors trong
        //   AccountValidation.java. Em truyền mật khẩu, xác nhận và tên hai ô để kiểm tra điều kiện mật khẩu,
        //   độ khớp, rồi đưa lỗi về đúng ô cần sửa.
        // ... Đọc tiếp tại AccountValidation.java, tìm [KTMK01].
        errors.putAll(AccountValidation.passwordErrors(password, confirmPassword, "password", "confirm_password"));

        // [DKTR01]
        // . Sau khi số điện thoại và email đúng định dạng, ở doPost trong RegisterController.java em gọi
        //   checkPhoneExists và checkEmailExists trong UserDAO.java. Em truyền từng liên hệ để tìm tài khoản
        //   đã dùng; nếu trùng thì báo tại ô tương ứng và chưa tạo tài khoản.
        // ... Xem hàm tra email tại UserDAO.java, tìm [DKEM04], hàm tra số điện thoại tại UserDAO.java, tìm
        //     [DKDT04]. Hai kết quả được thêm vào errors; cả hai trả về xong thì doPost xét errors trước khi
        //     tạo User.
        boolean lookupFailed = false;
        try {
            if (phoneError == null && userDAO.checkPhoneExists(phone)) errors.put("phone", "Số điện thoại đã tồn tại trong hệ thống.");
            if (emailError == null && userDAO.checkEmailExists(email)) errors.put("email", "Email đã tồn tại trong hệ thống.");
        } catch (utils.AccountDataException e) {
            lookupFailed = true;
        }

        // [DKLOI01]
        // . Khi đăng ký có lỗi, ở doPost trong RegisterController.java em gọi flash trong
        //   AccountFormFeedback.java, truyền lỗi cùng họ tên, số điện thoại và email. Dữ liệu được giữ qua lần
        //   mở lại trang để account_feedback.jsp và account-validation.js điền lại ô, hiện lỗi cho người dùng
        //   sửa.
        // ... doPost giữ lỗi và các ô rồi chuyển về tab Đăng ký. Đọc phần nhận dữ liệu để điền lại form tại
        //     account_feedback.jsp, tìm mã BMTV02; hiện xong là kết thúc lần gửi lỗi.
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
        //   UserDAO.register để ghi. Em không tạo tài khoản trước bước kiểm tra vì thông tin sai phải được sửa
        //   rồi mới lưu.
        String dbRole = "CUSTOMER";
        String dbPosition = null;

        User newUser = new User();
        newUser.setFullName(fullName.trim());
        newUser.setEmail(email.trim());
        newUser.setPhone(phone);
        newUser.setRole(dbRole);
        newUser.setPosition(dbPosition);

        // [DKL05]
        // . Thông tin đã đúng thì em tạo một User có vai trò khách hàng. Sau đó, em gọi register trong
        //   UserDAO.java, truyền User và mật khẩu vừa nhập để thêm tài khoản vào database. Kết quả trả về cho
        //   em biết có tạo được tài khoản hay phải báo lỗi.
        // ... Đọc tiếp tại UserDAO.java, tìm [DKL06].
        boolean isSuccess;
        try {
            isSuccess = userDAO.register(newUser, password);
        } catch (utils.AccountDataException e) {
            isSuccess = false;
        }

        // [DKL07]
        // . Kết quả từ UserDAO.register quay về doPost trong RegisterController.java. Tạo được tài khoản thì
        //   em đặt thông báo thành công và chuyển sang tab Đăng nhập. Chưa tạo được thì mở lại tab Đăng ký
        //   cùng lời báo để người dùng sửa hoặc thử lại.
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
