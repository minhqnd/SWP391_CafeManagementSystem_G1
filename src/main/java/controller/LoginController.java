package controller;

import dao.UserDAO;
import model.Cart;
import model.User;
import utils.RedirectUtil;
import utils.AccountCsrf;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "LoginController", urlPatterns = {"/login"})
public class LoginController extends HttpServlet {

    private final UserDAO userDAO;
    public LoginController() { this(new UserDAO()); }
    LoginController(UserDAO userDAO) { this.userDAO = userDAO; }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();

        String action = request.getParameter("action");
        // [DNX02]
        // . Đến doGet trong LoginController.java với action=logout, em gọi session.invalidate để xóa thông tin
        //   của lần đăng nhập hiện tại. Sau đó, em chuyển về Trang chủ để kết thúc Đăng xuất.
        // ... Kết thúc Đăng xuất khi trình duyệt mở Trang chủ và phiên đăng nhập cũ đã bị hủy.
        if ("logout".equals(action)) {

            // [SQL10-18] Nút Đăng xuất: hủy phiên đăng nhập rồi chuyển về Trang chủ.
            // . Không DELETE tài khoản trong users; thao tác này không có câu SQL riêng.
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            redirectByUser(currentUser, request, response);
            return;
        }

        String tab = request.getParameter("tab");
        response.sendRedirect(request.getContextPath() + "/?tab=" + (tab != null ? tab : "login"));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        if (!AccountCsrf.require(request, response)) return;
        utils.AccountFormFeedback.clear(request);

        // [DNL02]
        // . Đến doPost trong LoginController.java, em nhận email hoặc số điện thoại cùng mật khẩu. Em bỏ
        //   khoảng trắng đầu và cuối của ô tài khoản, kiểm tra hai ô đã nhập và độ dài cho phép rồi mới gọi
        //   phần tìm tài khoản.
        // ... Đọc tiếp trong LoginController.java, tìm [DNL03] sau phần kiểm tra hai ô để xem cách đối chiếu
        //     tài khoản.
        String userIdentifier = request.getParameter("username");
        if (userIdentifier != null) {
            userIdentifier = utils.AccountValidation.text(userIdentifier);
        }

        String pass = request.getParameter("password");
        String redirect = request.getParameter("redirect");
        boolean hasRedirect = RedirectUtil.isSafeLocalPath(redirect);

        // [DNTK03]
        // . Trong hàm doPost của LoginController.java, em kiểm tra email hoặc số điện thoại không trống và tối
        //   đa 150 ký tự, mật khẩu không trống và tối đa 255 ký tự. Nếu sai, em gọi AccountFormFeedback.flash
        //   để giữ thông tin cần điền lại rồi mở form Đăng nhập kèm lỗi.
        // ... Có lỗi thì doPost trả form và dừng đăng nhập. Hai ô đã đúng thì đọc bước gọi userDAO.login tại
        //     LoginController.java, tìm mã DNL03.
        java.util.Map<String,String> inputErrors = new java.util.LinkedHashMap<>();
        if (userIdentifier == null || userIdentifier.isEmpty()) {
            inputErrors.put("username", "Vui lòng nhập email hoặc số điện thoại.");
        } else if (userIdentifier.length() > 150) {
            inputErrors.put("username", "Email hoặc số điện thoại tối đa 150 ký tự.");
        }
        if (pass == null || pass.isEmpty()) {
            inputErrors.put("password", "Vui lòng nhập mật khẩu.");
        } else if (pass.length() > 255) {
            inputErrors.put("password", "Mật khẩu không được vượt quá 255 ký tự.");
        }
        if (!inputErrors.isEmpty()) {
            utils.AccountFormFeedback.flash(request, "login", inputErrors, java.util.Map.of("username", userIdentifier == null ? "" : userIdentifier));
            request.getSession().setAttribute("errorMsg", "Vui lòng kiểm tra các trường đăng nhập.");
            response.sendRedirect(request.getContextPath() + (hasRedirect ? redirect : "/") + "?tab=login");
            return;
        }

        // [DNL03]
        // . Hai ô đã đúng thì em gọi login trong UserDAO.java, truyền email hoặc số điện thoại cùng mật khẩu.
        //   Hàm trả User khi thông tin khớp, trả null khi không khớp. Nếu lỗi đọc dữ liệu, em giữ ô tài khoản
        //   và báo lỗi hệ thống để người dùng thử lại.
        // ... Đọc tiếp tại UserDAO.java, tìm [DNL04].
        User loginUser;
        try {
            loginUser = userDAO.login(userIdentifier, pass);
        } catch (utils.AccountDataException e) {
            utils.AccountFormFeedback.flash(request, "login", java.util.Map.of(), java.util.Map.of("username", userIdentifier));
            request.getSession().setAttribute("errorMsg", "Không đăng nhập được lúc này do lỗi hệ thống. Vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + (hasRedirect ? redirect : "/") + "?tab=login");
            return;
        }

        // [DNL05]
        // . Khi UserDAO.login trả về tài khoản hợp lệ, em quay lại doPost trong LoginController.java để tạo
        //   lần đăng nhập mới và lưu tài khoản vào session.user, đồng thời giữ giỏ hàng. Sau đó, em dùng đường
        //   dẫn quay lại hợp lệ hoặc gọi redirectByUser để mở trang phù hợp.
        // ... Đọc tiếp tại LoginController.java, tìm [DNL06].
        if (loginUser != null) {
            HttpSession oldSession = request.getSession(false);
            Cart cart = null;
            if (oldSession != null) {
                Object sessionCart = oldSession.getAttribute(Cart.SESSION_KEY);
                if (sessionCart instanceof Cart) {
                    cart = (Cart) sessionCart;
                    cart.setAvailablePoints(0);
                    cart.setPointsUsed(0);
                }
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);
            if (cart != null) {
                session.setAttribute(Cart.SESSION_KEY, cart);
            }
            session.setAttribute("user", loginUser);

            if (hasRedirect && "CUSTOMER".equalsIgnoreCase(loginUser.getRole())) {
                // . Kết thúc đăng nhập thành công bằng cách đưa khách về đường dẫn hợp lệ đã chọn trước đó.
                response.sendRedirect(request.getContextPath() + redirect);
                return;
            }

            redirectByUser(loginUser, request, response);
        } else {

            // . Kết thúc lần đăng nhập chưa thành công bằng cách mở lại form kèm thông báo, giữ ô tài khoản để sửa.
            utils.AccountFormFeedback.flash(request,"login",java.util.Map.of(),java.util.Map.of("username",userIdentifier));
            request.getSession().setAttribute("errorMsg", "Email, số điện thoại hoặc mật khẩu không đúng, hoặc tài khoản đã ngừng hoạt động.");
            response.sendRedirect(request.getContextPath() + (hasRedirect ? redirect : "/") + "?tab=login");
        }
    }

    // [DNL06]
    // . Hàm redirectByUser xem vai trò và vị trí của tài khoản vừa đăng nhập. Từ đó, em chuyển người dùng
    //   tới màn hình dành cho quản trị, nhân viên hoặc khách hàng. Trang tương ứng đã mở là kết thúc đăng
    //   nhập thành công.
    private void redirectByUser(User user, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String role = user.getRole();
        String position = user.getPosition();

        if ("ADMIN".equalsIgnoreCase(role)) {
            response.sendRedirect(request.getContextPath() + "/admin-dashboard.jsp");
        } else if ("STAFF".equalsIgnoreCase(role)) {
            if ("CASHIER".equalsIgnoreCase(position)) {
                response.sendRedirect(request.getContextPath() + "/cashier");
            } else if ("BARISTA".equalsIgnoreCase(position)) {
                response.sendRedirect(request.getContextPath() + "/barista");
            } else if ("SHIPPER".equalsIgnoreCase(position)) {
                response.sendRedirect(request.getContextPath() + "/delivery");
            } else {
                response.sendRedirect(request.getContextPath() + "/");
            }
        } else {
            response.sendRedirect(request.getContextPath() + "/");
        }
    }
}
