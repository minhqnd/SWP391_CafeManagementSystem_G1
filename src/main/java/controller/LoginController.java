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
            // LG10: [SLBF10] End the login session
            // . Xử lý GET /login?action=logout từ nút Đăng xuất [LG09].
            // .. Hủy session hiện tại để kết thúc phiên đăng nhập.
            // ... sendRedirect() đưa trình duyệt về Homepage.
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
        
        // LG05: . Nhận POST /login từ JavaScript [LG04], kiểm tra CSRF và đọc username/password.
        // .. Định danh được trim; mật khẩu giữ nguyên; kiểm tra đường dẫn quay lại có hợp lệ không.
        // ... UserDAO.login() trong UserDAO.java [LG06] đối chiếu tài khoản.
        request.setCharacterEncoding("UTF-8");
        if (!AccountCsrf.require(request, response)) return;

        // LG05a: . Đọc username là email hoặc SĐT từ ô định danh [LG02a]; bỏ khoảng trắng đầu/cuối.
        // ... userIdentifier chuyển vào UserDAO.login() [LG06].
        String userIdentifier = request.getParameter("username");
        if (userIdentifier != null) {
            userIdentifier = userIdentifier.trim();
        }
        // LG05b: . Đọc password từ ô mật khẩu đăng nhập [LG02b], giữ nguyên ký tự.
        // ... pass chuyển cùng userIdentifier tới UserDAO.login() [LG06].
        String pass = request.getParameter("password");
        String redirect = request.getParameter("redirect");
        boolean hasRedirect = RedirectUtil.isSafeLocalPath(redirect);

        // LG05c: Kiểm tra dữ liệu bắt buộc trước khi gọi DAO để báo đúng ô đang thiếu hoặc vượt giới hạn.
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
            utils.AccountFormFeedback.flash(request, "login", inputErrors, java.util.Map.of());
            request.getSession().setAttribute("errorMsg", "Vui lòng kiểm tra các trường đăng nhập.");
            response.sendRedirect(request.getContextPath() + (hasRedirect ? redirect : "/") + "?tab=login");
            return;
        }

        // . Gửi email/SĐT và mật khẩu xuống DAO để kiểm tra tài khoản.
        // ... UserDAO.login() trong UserDAO.java [LG06] trả User hoặc null.
        User loginUser = userDAO.login(userIdentifier, pass);

        // LG07: [SLBF10] Allow access to the system
        // . Nhận kết quả từ DAO [LG06]; có User thì tạo session mới, giữ giỏ hàng và lưu user.
        // .. Không có User thì báo lỗi [LG07a]; Customer có redirect nội bộ hợp lệ thì quay lại đường dẫn đó.
        // ... Trường hợp còn lại gọi redirectByUser() [LG08] để chọn trang theo vai trò.
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
            // LG07a: . Gắn lỗi đăng nhập vào ô password khi DAO không trả về User.
            // ... AccountFormFeedback.flash() → auth_modal.jsp [LG02] → feedback() trong account-validation.js.
            utils.AccountFormFeedback.flash(request,"login",java.util.Map.of("password","Thông tin đăng nhập không đúng hoặc tài khoản đã bị khóa."),java.util.Map.of());
            request.getSession().setAttribute("errorMsg", "Tên đăng nhập hoặc mật khẩu không chính xác, hoặc tài khoản đã bị khóa!");
            response.sendRedirect(request.getContextPath() + (hasRedirect ? redirect : "/") + "?tab=login");
        }
    }

    // LG08: . Chọn trang theo role và position sau đăng nhập.
    // .. ADMIN vào quản trị; STAFF theo vị trí; CUSTOMER và trường hợp còn lại về Homepage.
    // ... Customer chọn Hồ sơ trong header.jsp [PF01] để bắt đầu xem/cập nhật thông tin.
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
