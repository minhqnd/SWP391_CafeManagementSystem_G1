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
        // . Trong doGet của LoginController, em kết thúc lần đăng nhập hiện tại để các trang không tiếp tục
        //   dùng thông tin tài khoản đó. Sau đó em mở lại trang chủ; muốn dùng chức năng dành cho tài khoản
        //   thì người dùng cần đăng nhập lại.
        //
        // Kết thúc: đăng xuất khi trang chủ được mở lại.
        if ("logout".equals(action)) {

            // [SQL10-18] Nút Đăng xuất: hủy phiên đăng nhập rồi chuyển về Trang chủ.

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
        // . Hàm doPost trong LoginController nhận email hoặc số điện thoại cùng mật khẩu. Sau khi kiểm tra
        //   thông tin đã nhập, em gọi login trong UserDAO để đối chiếu tài khoản đang lưu.
        //
        // Tra tiếp: UserDAO.java / DNL03.
        String userIdentifier = request.getParameter("username");
        if (userIdentifier != null) {
            userIdentifier = utils.AccountValidation.text(userIdentifier);
        }

        String pass = request.getParameter("password");
        String redirect = request.getParameter("redirect");
        boolean hasRedirect = RedirectUtil.isSafeLocalPath(redirect);

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

        User loginUser;
        try {
            loginUser = userDAO.login(userIdentifier, pass);
        } catch (utils.AccountDataException e) {
            utils.AccountFormFeedback.flash(request, "login", java.util.Map.of(), java.util.Map.of("username", userIdentifier));
            request.getSession().setAttribute("errorMsg", "Không đăng nhập được lúc này do lỗi hệ thống. Vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + (hasRedirect ? redirect : "/") + "?tab=login");
            return;
        }

        // [DNL04]
        // . Sau khi UserDAO trả kết quả, doPost trong LoginController báo lỗi nếu thông tin không khớp
        //   với tài khoản hợp lệ. Nếu đăng nhập được, em giữ thông tin người dùng cho lần đăng nhập này
        //   rồi gọi redirectByUser để mở trang phù hợp với tài khoản.
        //
        // Tra tiếp: DNL05.
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

                response.sendRedirect(request.getContextPath() + redirect);
                return;
            }

            redirectByUser(loginUser, request, response);
        } else {

            utils.AccountFormFeedback.flash(request,"login",java.util.Map.of(),java.util.Map.of("username",userIdentifier));
            request.getSession().setAttribute("errorMsg", "Email, số điện thoại hoặc mật khẩu không đúng, hoặc tài khoản đã ngừng hoạt động.");
            response.sendRedirect(request.getContextPath() + (hasRedirect ? redirect : "/") + "?tab=login");
        }
    }

    // [DNL05]
    // . Hàm redirectByUser trong LoginController dùng vai trò và vị trí của tài khoản để chọn trang sau đăng
    //   nhập. Quản trị, nhân viên và khách hàng được mở đúng phần họ cần dùng; mở trang xong là kết thúc đăng
    //   nhập.
    //
    // Kết thúc: đăng nhập khi trang tương ứng được mở.
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
