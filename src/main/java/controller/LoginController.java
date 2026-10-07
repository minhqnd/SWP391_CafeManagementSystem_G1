package controller;

import dao.UserDAO;
import model.User;
import utils.RedirectUtil;

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
        
        // Xử lý Đăng xuất
        String action = request.getParameter("action");
        if ("logout".equals(action)) {
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
        User loginUser = userDAO.login(userIdentifier, pass);

        if (loginUser != null) {
            // Đăng nhập thành công -> Tạo Session
            HttpSession session = request.getSession();
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
                response.sendRedirect(request.getContextPath() + "/delivery-dashboard.jsp");
            } else {
                response.sendRedirect(request.getContextPath() + "/"); // Fallback for other staff
            }
        } else {
            // CUSTOMER
            response.sendRedirect(request.getContextPath() + "/");
        }
    }
}
