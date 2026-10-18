package controller;

import dao.AdminCustomerDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.AdminCustomerSummary;
import model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "AdminCustomersController", urlPatterns = {"/admin/customers"})
public class AdminCustomersController extends HttpServlet {
    private final AdminCustomerDAO customerDAO = new AdminCustomerDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAdmin(request, response)) return;
        HttpSession session = request.getSession();
        if (session.getAttribute("customerCsrfToken") == null)
            session.setAttribute("customerCsrfToken", UUID.randomUUID().toString());
        moveFlash(session, request, "customerMessage");
        moveFlash(session, request, "customerError");
        try {
            List<AdminCustomerSummary> customers = customerDAO.findAll();
            request.setAttribute("customers", customers);
            request.setAttribute("customerTotal", customers.size());
            request.setAttribute("customerActive", customers.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count());
            request.setAttribute("pointsTotal", customers.stream().mapToLong(AdminCustomerSummary::getTotalPoints).sum());
            request.setAttribute("memberRevenue", customers.stream().mapToDouble(AdminCustomerSummary::getTotalSpent).sum());
            request.getRequestDispatcher("/admin-customers.jsp").forward(request, response);
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Failed to load admin customers", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không tải được danh sách khách hàng.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) return;
        HttpSession session = request.getSession();
        if (!String.valueOf(session.getAttribute("customerCsrfToken")).equals(request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên làm việc đã hết hạn.");
            return;
        }
        try {
            if ("create".equals(request.getParameter("action"))) createCustomer(request, session);
            else if ("toggle".equals(request.getParameter("action"))) toggleStatus(request, session);
            else session.setAttribute("customerError", "Thao tác không hợp lệ.");
        } catch (IllegalArgumentException e) {
            session.setAttribute("customerError", e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Customer database operation failed", e);
            session.setAttribute("customerError", "Không thể lưu thay đổi. Số điện thoại hoặc email có thể đã tồn tại.");
        } catch (ClassNotFoundException e) {
            session.setAttribute("customerError", "Không thể kết nối cơ sở dữ liệu.");
        }
        response.sendRedirect(request.getContextPath() + "/admin/customers");
    }

    private void createCustomer(HttpServletRequest request, HttpSession session)
            throws SQLException, ClassNotFoundException {
        String fullName = required(request.getParameter("fullName"), "Họ và tên bắt buộc.");
        String phone = required(request.getParameter("phone"), "Số điện thoại bắt buộc.");
        String email = optional(request.getParameter("email"));
        String address = optional(request.getParameter("address"));
        if (fullName.length() > 120 || phone.length() > 20 || (email != null && email.length() > 150))
            throw new IllegalArgumentException("Thông tin khách hàng vượt quá độ dài cho phép.");
        if (!phone.matches("^[0-9+][0-9 .-]{7,19}$")) throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        if (customerDAO.phoneExists(phone)) throw new IllegalArgumentException("Số điện thoại đã tồn tại.");
        int initialPoints = parseNonNegative(request.getParameter("initialPoints"), "Điểm khởi tạo không hợp lệ.");
        LocalDate birthday = null;
        try {
            String value = optional(request.getParameter("birthday"));
            if (value != null) birthday = LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ.");
        }
        customerDAO.createCustomer(fullName, phone, email, birthday, address, initialPoints);
        session.setAttribute("customerMessage", "Đã thêm khách hàng '" + fullName + "'.");
    }

    private void toggleStatus(HttpServletRequest request, HttpSession session)
            throws SQLException, ClassNotFoundException {
        int id = parseNonNegative(request.getParameter("id"), "Mã khách hàng không hợp lệ.");
        if (id == 0) throw new IllegalArgumentException("Mã khách hàng không hợp lệ.");
        String status = request.getParameter("status");
        if (!"ACTIVE".equals(status) && !"LOCKED".equals(status)) throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        if (!customerDAO.updateStatus(id, status)) throw new IllegalArgumentException("Không tìm thấy khách hàng.");
        session.setAttribute("customerMessage", "Đã cập nhật trạng thái khách hàng.");
    }

    private int parseNonNegative(String value, String message) {
        try { int number = Integer.parseInt(value == null || value.isBlank() ? "0" : value); if (number < 0) throw new NumberFormatException(); return number; }
        catch (NumberFormatException e) { throw new IllegalArgumentException(message); }
    }
    private String required(String value, String message) { String result = optional(value); if (result == null) throw new IllegalArgumentException(message); return result; }
    private String optional(String value) { if (value == null || value.trim().isEmpty()) return null; return value.trim(); }
    private void moveFlash(HttpSession session, HttpServletRequest request, String key) { Object value = session.getAttribute(key); if (value != null) { request.setAttribute(key, value); session.removeAttribute(key); } }
    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) { response.sendRedirect(request.getContextPath() + "/?tab=login"); return false; }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) { response.sendError(403, "Chỉ Admin được quản lý khách hàng."); return false; }
        return true;
    }
}
