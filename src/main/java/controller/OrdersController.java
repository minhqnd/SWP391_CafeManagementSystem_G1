package controller;

import dao.OrderDAO;
import model.CustomerOrder;
import model.OrderDetail;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * OrdersController - Quản lý và theo dõi đơn hàng thời gian thực
 * Ported từ OrdersPage.tsx
 * Mapped: /orders
 */
@WebServlet(name = "OrdersController", urlPatterns = {"/orders"})
public class OrdersController extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        String orderIdStr = request.getParameter("orderId");
        int targetOrderId = -1;

        if (orderIdStr != null && !orderIdStr.trim().isEmpty()) {
            try {
                targetOrderId = Integer.parseInt(orderIdStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        // Hỗ trợ hành động Hủy đơn hàng (UC16: Cancel Online Order)
        String action = request.getParameter("action");
        if ("cancel".equalsIgnoreCase(action) && targetOrderId > 0) {
            Integer customerId = user != null ? user.getId() : null;
            boolean success = orderDAO.cancelOrder(targetOrderId, customerId);
            if (success) {
                session.setAttribute("toastMsg", "Đã hủy đơn hàng thành công!");
            } else {
                session.setAttribute("errorMsg", "Không thể hủy đơn hàng (Đơn đã qua giai đoạn chuẩn bị hoặc không thuộc quyền quản lý)!");
            }
            response.sendRedirect(request.getContextPath() + "/orders?orderId=" + targetOrderId);
            return;
        }

        List<CustomerOrder> orders;
        if (user != null) {
            // Lấy danh sách đơn của khách hàng đăng nhập
            orders = orderDAO.getOrdersByCustomerId(user.getId(), 20);
            if (orders.isEmpty()) {
                orders = orderDAO.getRecentOrders(10);
            }
        } else {
            orders = orderDAO.getRecentOrders(10);
        }

        CustomerOrder currentOrder = null;

        if (targetOrderId > 0) {
            currentOrder = orderDAO.getOrderById(targetOrderId);
            if (currentOrder != null) {
                // Đảm bảo đơn hàng đang theo dõi luôn xuất hiện ở đầu danh sách
                boolean exists = false;
                for (CustomerOrder o : orders) {
                    if (o.getId() == currentOrder.getId()) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    orders.add(0, currentOrder);
                }
            }
        } else if (!orders.isEmpty()) {
            currentOrder = orders.get(0);
        }

        List<OrderDetail> activeOrderDetails = null;
        if (currentOrder != null) {
            activeOrderDetails = orderDAO.getOrderDetails(currentOrder.getId());
        }

        request.setAttribute("orders", orders);
        request.setAttribute("currentOrder", currentOrder);
        request.setAttribute("activeOrderDetails", activeOrderDetails);

        request.getRequestDispatcher("/orders.jsp").forward(request, response);
    }
}
