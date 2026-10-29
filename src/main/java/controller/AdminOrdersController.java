package controller;

import dao.AdminOrderDAO;
import dao.DeliveryDAO;
import dao.OrderDAO;
import dao.PaymentDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.AdminOrderSummary;
import model.Delivery;
import model.Order;
import model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@WebServlet(name = "AdminOrdersController", urlPatterns = {"/admin/orders"})
public class AdminOrdersController extends HttpServlet {
    private final AdminOrderDAO adminOrderDAO = new AdminOrderDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAdmin(request, response)) return;
        HttpSession session = request.getSession();
        moveFlash(session, request, "orderAdminMessage");
        moveFlash(session, request, "orderAdminError");
        try {
            LocalDate from;
            LocalDate to;
            try {
                from = parseDate(request.getParameter("from"));
                to = parseDate(request.getParameter("to"));
                if (from != null && to != null && to.isBefore(from)) throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Khoảng ngày không hợp lệ.");
                return;
            }
            request.setAttribute("dateFrom", from == null ? "" : from.toString());
            request.setAttribute("dateTo", to == null ? "" : to.toString());
            List<AdminOrderSummary> orders = adminOrderDAO.findAll(from, to);
            request.setAttribute("orders", orders);
            request.setAttribute("orderTotal", orders.size());
            request.setAttribute("orderProcessing", orders.stream().filter(AdminOrderSummary::isInProgress).count());
            request.setAttribute("orderCancelled", orders.stream().filter(o -> "CANCELLED".equals(o.getStatus())).count());
            request.setAttribute("completedRevenue", orders.stream().filter(o -> "COMPLETED".equals(o.getStatus())).mapToDouble(AdminOrderSummary::getFinalAmount).sum());

            int detailId = parseId(request.getParameter("detailId"), true);
            if (detailId > 0) {
                Order detail = orderDAO.getOrderById(detailId);
                if (detail != null) {
                    request.setAttribute("detailOrder", detail);
                    request.setAttribute("detailSummary", orders.stream().filter(o -> o.getId() == detailId).findFirst().orElse(null));
                    request.setAttribute("detailPayment", paymentDAO.getPaymentByOrderId(detailId));
                    // Tiến độ giao hàng + sự cố shipper báo (màn Giao hàng - Screen 14)
                    Delivery delivery = deliveryDAO.getByOrderId(detailId);
                    if (delivery != null) delivery.setIssues(deliveryDAO.getIssues(delivery.getId()));
                    request.setAttribute("detailDelivery", delivery);
                }
            }
            request.getRequestDispatcher("/admin-orders.jsp").forward(request, response);
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Failed to load admin orders", e);
            response.sendError(500, "Không tải được danh sách đơn hàng.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isAdmin(request, response)) return;
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Trang quản lý đơn hàng chỉ cho phép xem.");
    }

    static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            if (!raw.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException();
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) { throw new IllegalArgumentException(e); }
    }

    private int parseId(String value, boolean allowEmpty) {
        if (allowEmpty && (value == null || value.isBlank())) return 0;
        try { int id = Integer.parseInt(value); if (id < 1) throw new NumberFormatException(); return id; }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Mã đơn hàng không hợp lệ."); }
    }
    private void moveFlash(HttpSession session, HttpServletRequest request, String key) { Object value = session.getAttribute(key); if (value != null) { request.setAttribute(key, value); session.removeAttribute(key); } }
    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false); User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) { response.sendRedirect(request.getContextPath() + "/?tab=login"); return false; }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) { response.sendError(403, "Chỉ Admin được quản lý đơn hàng."); return false; }
        return true;
    }
}
