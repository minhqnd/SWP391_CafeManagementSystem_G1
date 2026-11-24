package controller;

import dao.UserDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.User;
import utils.AccountCsrf;
import utils.AccountValidation;

@WebServlet(name = "AccountAvailabilityController", urlPatterns = {"/account-availability"})
public class AccountAvailabilityController extends HttpServlet {
    private final UserDAO users;
    public AccountAvailabilityController() { this(new UserDAO()); }
    AccountAvailabilityController(UserDAO users) { this.users = users; }

    // [BMLH02]
    // . Trong hàm doPost của AccountAvailabilityController.java, em nhận thông tin ô email hoặc số điện
    //   thoại mà checkContact gửi từ account-validation.js. Em gọi UserDAO.contactExists, truyền loại ô,
    //   giá trị và mã tài khoản cần bỏ qua, để tìm xem liên hệ này đã được người khác dùng chưa.
    // ... Đọc tiếp tại UserDAO.java, tìm [BMLH03].
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        if (!AccountCsrf.require(request, response)) return;
        String field = request.getParameter("field");
        if (!"email".equals(field) && !"phone".equals(field)) { response.sendError(400); return; }
        String value = "email".equals(field) ? AccountValidation.email(request.getParameter("value"))
                : AccountValidation.phone(request.getParameter("value"));
        String error = "email".equals(field) ? AccountValidation.emailError(value) : AccountValidation.phoneError(value);
        if (error != null) { response.sendError(400); return; }
        int excludedId = 0;
        if ("profile".equals(request.getParameter("scope"))) {
            HttpSession session = request.getSession(false);
            User user = session == null ? null : (User)session.getAttribute("user");
            try {
                if (user == null || users.getActiveUserById(user.getId()) == null) { response.sendError(401); return; }
            } catch (utils.AccountDataException e) { response.sendError(503); return; }
            excludedId = user.getId();
        }
        try {
            boolean exists = users.contactExists(field, value, excludedId);
            response.getWriter().write("{\"exists\":" + exists + "}");
        } catch (IllegalStateException e) { response.sendError(503); }
    }
}
