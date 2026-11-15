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

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        
        String action = request.getParameter("action");
        if ("logout".equals(action)) {
            // LG05: . Nhận yêu cầu GET /login?action=logout từ nút Đăng xuất.
            // .. Session đăng nhập được hủy để kết thúc phiên hiện tại.
            // ... Chuyển trình duyệt về trang chủ.
            session.invalidate(); // Xóa session
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

        // LG02: . Nhận username là email/SĐT và password từ form đăng nhập.
        // .. Định danh được loại khoảng trắng ở đầu/cuối; mật khẩu giữ nguyên để đối chiếu chính xác.
        // ... Dữ liệu được chuyển vào UserDAO.login() sau khi kiểm tra redirect nội bộ.
        String userIdentifier = request.getParameter("username");
        if (userIdentifier != null) {
            userIdentifier = userIdentifier.trim();
        }
        String pass = request.getParameter("password");
        String redirect = request.getParameter("redirect");
        boolean hasRedirect = RedirectUtil.isSafeLocalPath(redirect);

        // ... Gửi định danh và mật khẩu tới UserDAO.login() [LG03].
        User loginUser = userDAO.login(userIdentifier, pass);

        // LG04: . Xử lý kết quả UserDAO.login().
        // .. Đúng thông tin thì tạo session mới và lưu user; sai thì giữ form và gắn lỗi vào mật khẩu.
        // ... Thành công chuyển tới redirectByUser() [LG06]; thất bại quay lại auth_modal.jsp [LG01].
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
            session.setAttribute("user", loginUser); // Lưu object User vào session

            if (hasRedirect && "CUSTOMER".equalsIgnoreCase(loginUser.getRole())) {
                response.sendRedirect(request.getContextPath() + redirect);
                return;
            }

            redirectByUser(loginUser, request, response);
        } else {
            // .. Đăng nhập sai hoặc tài khoản bị khóa thì không tạo phiên đăng nhập và gắn lỗi vào ô mật khẩu.
            // ... Quay lại auth_modal.jsp với tab=login [LG01].
            utils.AccountFormFeedback.flash(request,"login",java.util.Map.of("password","Thông tin đăng nhập không đúng hoặc tài khoản đã bị khóa."),java.util.Map.of());
            request.getSession().setAttribute("errorMsg", "Tên đăng nhập hoặc mật khẩu không chính xác, hoặc tài khoản đã bị khóa!");
            response.sendRedirect(request.getContextPath() + (hasRedirect ? redirect : "/") + "?tab=login");
        }
    }

    // LG06: . Chọn trang theo role và position của tài khoản đã đăng nhập.
    // .. ADMIN vào quản trị, STAFF vào màn hình vị trí, CUSTOMER về trang chủ.
    // ... sendRedirect() đưa trình duyệt đến đường dẫn đã chọn.
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
                response.sendRedirect(request.getContextPath() + "/"); // Fallback for other staff
            }
        } else {
            response.sendRedirect(request.getContextPath() + "/");
        }
    }
}
