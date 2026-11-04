package controller;

import dao.UserDAO;
import model.Cart;
import model.User;
import utils.RedirectUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

// BF10 - ĐĂNG NHẬP/ĐĂNG XUẤT: POST nhận form; GET mở popup hoặc xử lý action=logout.
@WebServlet(name = "LoginController", urlPatterns = {"/login"})
public class LoginController extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        
        // Xử lý Đăng xuất
        String action = request.getParameter("action");
        if ("logout".equals(action)) {
            // BF10: hủy phiên chứa user; các trang bảo vệ phải kiểm tra lại khi truy cập lần sau.
            session.invalidate(); // Xóa session
            // Redirect về trang chủ thay vì /login
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        // Kiểm tra nếu đã login thì redirect luôn, không cho vào trang login nữa
        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            redirectByUser(currentUser, request, response);
            return;
        }

        String tab = request.getParameter("tab");
        // Nếu truy cập trực tiếp /login thì điều hướng về trang chủ kèm bật popup đăng nhập
        response.sendRedirect(request.getContextPath() + "/?tab=" + (tab != null ? tab : "login"));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");

        // Nhận dữ liệu từ form (login modal / login.jsp)
        String userIdentifier = request.getParameter("username");
        if (userIdentifier != null) {
            userIdentifier = userIdentifier.trim();
        }
        String pass = request.getParameter("password");
        if (pass != null) {
            pass = pass.trim();
        }
        // Trang cần quay lại sau khi đăng nhập (VD: /checkout), chỉ nhận đường dẫn nội bộ
        String redirect = request.getParameter("redirect");
        boolean hasRedirect = RedirectUtil.isSafeLocalPath(redirect);

        // Gọi DAO để xác thực
        // BF10: DAO trả User nếu đúng thông tin và ACTIVE; trả null nếu không xác thực được.
        User loginUser = userDAO.login(userIdentifier, pass);

        if (loginUser != null) {
            // Renew the session after authentication so an older role/CSRF state cannot leak
            // into the newly authenticated account. The anonymous shopping cart is safe to keep.
            // BF10: tạo phiên mới sau đăng nhập, chỉ mang giỏ hàng sang và xóa điểm của danh tính cũ.
            HttpSession oldSession = request.getSession(false);
            Cart cart = null;
            if (oldSession != null) {
                Object sessionCart = oldSession.getAttribute(Cart.SESSION_KEY);
                if (sessionCart instanceof Cart) {
                    cart = (Cart) sessionCart;
                    // Loyalty balances belong to the previous identity, not the browser cart.
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

            // Khách hàng đăng nhập từ trang Checkout -> quay lại tiếp tục đặt hàng
            if (hasRedirect && "CUSTOMER".equalsIgnoreCase(loginUser.getRole())) {
                response.sendRedirect(request.getContextPath() + redirect);
                return;
            }

            // Dựa vào Role/Position để chuyển hướng đến trang tương ứng
            redirectByUser(loginUser, request, response);
        } else {
            // Đăng nhập thất bại -> Lưu lỗi vào session và điều hướng về trang trước đó (mặc định trang chủ) bật popup
            request.getSession().setAttribute("errorMsg", "Tên đăng nhập hoặc mật khẩu không chính xác, hoặc tài khoản đã bị khóa!");
            response.sendRedirect(request.getContextPath() + (hasRedirect ? redirect : "/") + "?tab=login");
        }
    }

    // Hàm helper để chia luồng redirect tùy thuộc vào Role và Position
    private void redirectByUser(User user, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // BF10 - PHÂN LUỒNG: ADMIN -> quản trị; STAFF theo vị trí; CUSTOMER -> trang chủ.
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
            // CUSTOMER
            response.sendRedirect(request.getContextPath() + "/");
        }
    }
}
