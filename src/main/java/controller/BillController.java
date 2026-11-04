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

        // Hoá đơn có tên, SĐT, địa chỉ khách: bắt buộc đăng nhập
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?login=true");
            return;
        }
        boolean isStaff = "ADMIN".equalsIgnoreCase(user.getRole()) || "STAFF".equalsIgnoreCase(user.getRole());

        int orderId = -1;
        String orderIdStr = request.getParameter("orderId");
        if (orderIdStr != null && !orderIdStr.trim().isEmpty()) {
            try {
                orderId = Integer.parseInt(orderIdStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        // Không có mã đơn thì về danh sách đơn (không tự mở đơn mới nhất của người khác)
        Order order = orderId > 0 ? orderDAO.getOrderById(orderId) : null;
        if (order == null) {
            session.setAttribute("ordersError", "Không tìm thấy hóa đơn cần xem.");
            response.sendRedirect(request.getContextPath() + (isStaff ? "/" : "/orders"));
            return;
        }

        // Khách chỉ xem hoá đơn của chính mình; Admin / nhân viên xem được mọi hoá đơn
        if (!isStaff && (order.getCustomerId() == null || user.getId() != order.getCustomerId().intValue())) {
            session.setAttribute("ordersError", "Bạn không có quyền truy cập hóa đơn của đơn hàng này!");
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }

        // Thanh toán QR xong trang thanh toán chuyển sang đây: lúc này mới bỏ các món đã trả khỏi giỏ
        if (order.isPaid()) {
            CheckoutController.removePaidItems(session, orderId);
        }

        // Lấy danh sách chi tiết các món trong đơn hàng
        List<OrderDetail> orderDetails = order.getItems();
        if (orderDetails == null || orderDetails.isEmpty()) {
            orderDetails = orderDAO.getOrderDetails(orderId);
        }

        // Lấy thông tin giao dịch thanh toán
        Payment payment = paymentDAO.getBillPayment(orderId);

        // Gán dữ liệu sang View
        request.setAttribute("order", order);
        request.setAttribute("orderDetails", orderDetails);
        request.setAttribute("payment", payment);
        request.setAttribute("billStatus", billStatus(order, payment));

        // Chuyển tiếp tới giao diện bill.jsp
        request.getRequestDispatcher("/bill.jsp").forward(request, response);
    }

    /**
     * Nhãn trạng thái trên hoá đơn theo thanh toán thực tế:
     * PAID đã trả, COD trả khi nhận hàng (chưa thu), UNPAID chờ thanh toán, CANCELLED đã huỷ
     */
    static String billStatus(Order order, Payment payment) {
        String payStatus = payment == null ? "" : payment.getStatus();
        if (order.isCancelled()) return "CANCELLED";
        if ("SUCCESS".equals(payStatus)) return "PAID";
        if ("PENDING".equals(payStatus) && "CASH".equals(payment.getPaymentMethod())) return "COD";
        return "UNPAID";
    }
}
