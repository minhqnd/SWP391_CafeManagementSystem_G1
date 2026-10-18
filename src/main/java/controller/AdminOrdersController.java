package controller;

import dao.AdminOrderDAO;
import dao.OrderDAO;
import dao.PaymentDAO;
import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.AdminOrderSummary;
import model.Order;
import model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@WebServlet(name = "AdminOrdersController", urlPatterns = {"/admin/orders"})
public class AdminOrdersController extends HttpServlet {
    private static final Set<String> STATUSES = Set.of("PENDING", "CONFIRMED", "PREPARING", "READY", "DELIVERING", "COMPLETED", "CANCELLED");
    private final AdminOrderDAO adminOrderDAO = new AdminOrderDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAdmin(request, response)) return;
        HttpSession session = request.getSession();
        if (session.getAttribute("orderAdminCsrfToken") == null)
            session.setAttribute("orderAdminCsrfToken", UUID.randomUUID().toString());
        moveFlash(session, request, "orderAdminMessage");
        moveFlash(session, request, "orderAdminError");
        try {
            List<AdminOrderSummary> orders = adminOrderDAO.findAll();
            request.setAttribute("orders", orders);
            request.setAttribute("orderTotal", orders.size());
            request.setAttribute("orderProcessing", orders.stream().filter(AdminOrderSummary::isInProgress).count());
            request.setAttribute("orderCancelled", orders.stream().filter(o -> "CANCELLED".equals(o.getStatus())).count());
            request.setAttribute("completedRevenue", orders.stream().filter(o -> "COMPLETED".equals(o.getStatus())).mapToDouble(AdminOrderSummary::getFinalAmount).sum());
            request.setAttribute("shippers", userDAO.getStaffEmployees().stream().filter(u -> "SHIPPER".equals(u.getPosition())).collect(Collectors.toList()));

            int detailId = parseId(request.getParameter("detailId"), true);
            if (detailId > 0) {
                Order detail = orderDAO.getOrderById(detailId);
                if (detail != null) {
                    request.setAttribute("detailOrder", detail);
                    request.setAttribute("detailSummary", orders.stream().filter(o -> o.getId() == detailId).findFirst().orElse(null));
                    request.setAttribute("detailPayment", paymentDAO.getPaymentByOrderId(detailId));
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
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) return;
        HttpSession session = request.getSession();
        if (!String.valueOf(session.getAttribute("orderAdminCsrfToken")).equals(request.getParameter("csrfToken"))) {
            response.sendError(403, "Phiên làm việc đã hết hạn."); return;
        }
        int detailId = 0;
        try {
            detailId = parseId(request.getParameter("id"), false);
            String action = request.getParameter("action");
            if ("status".equals(action)) updateStatus(request, detailId, session);
            else if ("shipper".equals(action)) assignShipper(request, detailId, session);
            else session.setAttribute("orderAdminError", "Thao tác không hợp lệ.");
        } catch (IllegalArgumentException | SQLException | ClassNotFoundException e) {
            getServletContext().log("Admin order operation failed", e);
            session.setAttribute("orderAdminError", e.getMessage() == null ? "Không thể cập nhật đơn hàng." : e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/admin/orders" + (detailId > 0 ? "?detailId=" + detailId : ""));
    }

    private void updateStatus(HttpServletRequest request, int id, HttpSession session) throws SQLException, ClassNotFoundException {
        String status = request.getParameter("status");
        if (!STATUSES.contains(status)) throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ.");
        if (!adminOrderDAO.updateStatus(id, status)) throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
        session.setAttribute("orderAdminMessage", "Đã cập nhật trạng thái đơn hàng.");
    }

    private void assignShipper(HttpServletRequest request, int id, HttpSession session) throws SQLException, ClassNotFoundException {
        String raw = request.getParameter("shipperId");
        Integer shipperId = raw == null || raw.isBlank() ? null : parseId(raw, false);
        if (shipperId != null) {
            User shipper = userDAO.getActiveStaffById(shipperId);
            if (shipper == null || !"SHIPPER".equals(shipper.getPosition())) throw new IllegalArgumentException("Shipper không hợp lệ.");
        }
        if (!adminOrderDAO.assignShipper(id, shipperId)) throw new IllegalArgumentException("Chỉ đơn ONLINE mới có thể gán shipper.");
        session.setAttribute("orderAdminMessage", "Đã cập nhật shipper.");
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
