package controller;

import dao.AccountDAO;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "LoginController", urlPatterns = {"/login"})
public class LoginController extends HttpServlet {

    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        
        // Xử lý Đăng xuất
        String action = request.getParameter("action");
        if ("logout".equals(action)) {
            session.invalidate(); // Xóa session
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Kiểm tra nếu đã login thì redirect luôn, không cho vào trang login nữa
        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            redirectByRole(currentUser.getRole(), response);
            return;
        }

        // Nếu chưa đăng nhập thì forward tới trang giao diện login.jsp
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Nhận dữ liệu từ form (login.jsp)
        String user = request.getParameter("username");
        String pass = request.getParameter("password");
        
        // Gọi DAO để xác thực
        User loginUser = accountDAO.login(user, pass);
        
        if (loginUser != null) {
            // Đăng nhập thành công -> Tạo Session
            HttpSession session = request.getSession();
            session.setAttribute("user", loginUser); // Lưu object User (chứa thông tin và Role) vào session
            
            // Dựa vào Role để chuyển hướng đến trang tương ứng
            redirectByRole(loginUser.getRole(), response);
        } else {
            // Đăng nhập thất bại -> Trả về thông báo lỗi
            request.setAttribute("error", "Tên đăng nhập hoặc mật khẩu không chính xác, hoặc tài khoản đã bị khóa!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }

    // Hàm helper để chia luồng redirect tùy thuộc vào Role
    private void redirectByRole(String role, HttpServletResponse response) throws IOException {
        switch (role) {
            case "Admin":
                response.sendRedirect("admin-dashboard.jsp");
                break;
            case "Cashier":
                response.sendRedirect("cashier-dashboard.jsp");
                break;
            case "Barista":
                response.sendRedirect("barista-dashboard.jsp");
                break;
            case "Delivery Staff":
                response.sendRedirect("delivery-dashboard.jsp");
                break;
            case "Customer":
                response.sendRedirect("home.jsp");
                break;
            default:
                response.sendRedirect("home.jsp"); // Fallback
                break;
        }
    }
}
