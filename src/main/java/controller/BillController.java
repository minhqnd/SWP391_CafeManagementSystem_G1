package controller;

import dao.OrderDAO;
import dao.PaymentDAO;
import model.Order;
import model.OrderDetail;
import model.Payment;
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
 * BillController - Hiển thị màn hình Hóa đơn điện tử (e-Bill) sau khi thanh toán thành công
 * URL: /bill
 */
@WebServlet(name = "BillController", urlPatterns = {"/bill"})
public class BillController extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        String orderIdStr = request.getParameter("orderId");
        int orderId = -1;

        if (orderIdStr != null && !orderIdStr.trim().isEmpty()) {
            try {
                orderId = Integer.parseInt(orderIdStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        if (orderId <= 0) {
            List<Order> recent = orderDAO.getRecentOrders(1);
            if (!recent.isEmpty()) {
                orderId = recent.get(0).getId();
            } else {
                response.sendRedirect(request.getContextPath() + "/menu");
                return;
            }
        }

        // Lấy thông tin đơn hàng
        Order order = orderDAO.getOrderById(orderId);
        if (order == null) {
            session.setAttribute("errorMsg", "Không tìm thấy hóa đơn cho đơn hàng #" + orderId);
            response.sendRedirect(request.getContextPath() + "/menu");
            return;
        }

        // Kiểm tra phân quyền truy cập hóa đơn (Nếu đăng nhập là Customer thì chỉ xem đơn của mình, trừ khi là Admin/Staff)
        if (user != null && order.getCustomerId() != null 
                && !"ADMIN".equalsIgnoreCase(user.getRole()) 
                && !"STAFF".equalsIgnoreCase(user.getRole()) 
                && !"CASHIER".equalsIgnoreCase(user.getRole()) 
                && user.getId() != order.getCustomerId().intValue()) {
            session.setAttribute("errorMsg", "Bạn không có quyền truy cập hóa đơn của đơn hàng này!");
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }

        // Lấy danh sách chi tiết các món trong đơn hàng
        List<OrderDetail> orderDetails = order.getItems();
        if (orderDetails == null || orderDetails.isEmpty()) {
            orderDetails = orderDAO.getOrderDetails(orderId);
        }

        // Lấy thông tin giao dịch thanh toán
        Payment payment = paymentDAO.getPaymentByOrderId(orderId);

        // Gán dữ liệu sang View
        request.setAttribute("order", order);
        request.setAttribute("orderDetails", orderDetails);
        request.setAttribute("payment", payment);

        // Chuyển tiếp tới giao diện bill.jsp
        request.getRequestDispatcher("/bill.jsp").forward(request, response);
    }
}
