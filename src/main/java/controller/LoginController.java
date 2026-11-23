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
        // . Khi bấm Đăng xuất, yêu cầu /login?action=logout đến doGet trong LoginController.java. Em gọi
        //   session.invalidate (kết thúc lần đăng nhập hiện tại) để xóa thông tin phiên đăng nhập rồi
        //   sendRedirect (chuyển sang đường dẫn khác) về Trang chủ, kết thúc lần dùng tài khoản hiện tại.
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
        // . Khi nhận form tại doPost trong LoginController.java, em lấy email hoặc số điện thoại và mật khẩu. Em
        //   bỏ khoảng trắng đầu cuối của định danh, kiểm tra hai ô có nội dung và đúng giới hạn rồi mới chuyển
        //   sang bước tìm tài khoản.
        // ... Đọc tiếp trong LoginController.java, tìm [DNL03] sau phần kiểm tra hai ô để xem cách đối chiếu tài
        //     khoản.
        String userIdentifier = request.getParameter("username");
        if (userIdentifier != null) {
            userIdentifier = userIdentifier.trim();
        }

        String pass = request.getParameter("password");
        String redirect = request.getParameter("redirect");
        boolean hasRedirect = RedirectUtil.isSafeLocalPath(redirect);

        // [DNTK03]
        // . Trong doPost của LoginController.java, em kiểm tra email hoặc số điện thoại không trống và tối đa
        //   150 ký tự, mật khẩu không trống và tối đa 255 ký tự. Nếu sai, em gọi AccountFormFeedback.flash (giữ
        //   lỗi và các ô qua lần chuyển trang) để giữ thông tin cần điền lại rồi mở form Đăng nhập kèm lỗi.
        // ... Trong LoginController.java, còn lỗi thì doPost mở lại form Đăng nhập kèm lỗi; hết lỗi thì đọc tiếp
        //     phần gọi userDAO.login ngay bên dưới.
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
        // . Kiểm tra hai ô xong, em gọi login trong UserDAO.java để đối chiếu tài khoản. Em truyền email hoặc số
    //   điện thoại cùng mật khẩu; hàm trả User nếu đúng hoặc null nếu thông tin không khớp. Nếu việc đọc dữ
    //   liệu bị lỗi, em giữ ô tài khoản và báo lỗi hệ thống, không báo rằng người dùng nhập sai mật khẩu.
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
        //   phiên đăng nhập mới và đặt session.user, đồng thời giữ giỏ hàng. Sau đó em dùng đường dẫn quay lại
        //   hợp lệ hoặc gọi redirectByUser để chọn trang phù hợp với tài khoản.
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
    // . Nếu cần chọn trang theo tài khoản, em gọi redirectByUser ngay trong LoginController.java. Hàm xem
    //   vai trò và vị trí của User vừa đăng nhập rồi chuyển tới màn hình dành cho quản trị, nhân viên hoặc
    //   khách hàng.
    // . Kết thúc nhánh đăng nhập này khi chuyển tới đường dẫn phù hợp với tài khoản vừa đăng nhập.
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
