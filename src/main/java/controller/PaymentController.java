package controller;

import dao.OrderDAO;
import dao.PaymentDAO;
import model.CustomerOrder;
import model.OrderDetail;
import model.Payment;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

/**
 * PaymentController - Điều hướng và xử lý thanh toán cho đơn hàng
 * URL: /payment
 */
@WebServlet(name = "PaymentController", urlPatterns = {"/payment"})
public class PaymentController extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String orderIdStr = request.getParameter("orderId");
        int orderId = 1; // Mặc định đơn hàng số 1 nếu chưa truyền orderId để tiện trải nghiệm

        if (orderIdStr != null && !orderIdStr.trim().isEmpty()) {
            try {
                orderId = Integer.parseInt(orderIdStr.trim());
            } catch (NumberFormatException e) {
                orderId = 1;
            }
        }

        // Truy vấn dữ liệu đơn hàng và chi tiết các món
        CustomerOrder order = orderDAO.getOrderById(orderId);

        if (order == null) {
            request.getSession().setAttribute("errorMsg", "Không tìm thấy đơn hàng với mã ID: " + orderId);
            response.sendRedirect(request.getContextPath() + "/menu");
            return;
        }

        // Nếu đơn hàng ĐÃ ĐƯỢC THANH TOÁN -> Chuyển thẳng sang màn hình Hóa đơn (Bill)
        if (order.isPaid()) {
            response.sendRedirect(request.getContextPath() + "/bill?orderId=" + orderId);
            return;
        }

        // Nếu đơn hàng ĐÃ BỊ HỦY -> Không cho phép thanh toán tiếp
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            request.getSession().setAttribute("errorMsg", "Đơn hàng #" + order.getOrderCode() + " đã bị hủy, không thể thanh toán!");
            response.sendRedirect(request.getContextPath() + "/orders?orderId=" + orderId);
            return;
        }

        List<OrderDetail> orderDetails = orderDAO.getOrderDetails(orderId);

        // Đẩy dữ liệu vào request attributes
        request.setAttribute("order", order);
        request.setAttribute("orderDetails", orderDetails);

        // Chuyển tiếp tới giao diện payment.jsp
        request.getRequestDispatcher("/payment.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String orderIdStr = request.getParameter("orderId");
        String paymentMethodParam = request.getParameter("payment_method");
        String amountStr = request.getParameter("amount");

        if (orderIdStr == null || orderIdStr.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/menu");
            return;
        }

        int orderId;
        double amountPaid;

        try {
            orderId = Integer.parseInt(orderIdStr.trim());
            amountPaid = Double.parseDouble(amountStr.trim());
        } catch (Exception e) {
            request.setAttribute("error", "Dữ liệu thanh toán không hợp lệ!");
            doGet(request, response);
            return;
        }

        // Lấy thông tin đơn hàng hiện tại từ cơ sở dữ liệu
        CustomerOrder order = orderDAO.getOrderById(orderId);
        if (order == null) {
            response.sendRedirect(request.getContextPath() + "/menu");
            return;
        }

        // Nếu đơn hàng đã được thanh toán, chuyển thẳng sang màn hình Hóa đơn (tránh trừ tiền trùng lặp)
        if (order.isPaid()) {
            response.sendRedirect(request.getContextPath() + "/bill?orderId=" + orderId);
            return;
        }

        // =========================================================================
        // KIỂM TRA RÀNG BUỘC NGHIỆP VỤ BR-09 (Data Business Rules trong tài liệu SRS)
        // BR-09: "Payment amount must match the order amount." (Enforced On: Payment.amount, CustomerOrder.total_amount/final_amount)
        // =========================================================================
        double expectedAmount = order.getFinalAmount();
        if (Math.abs(amountPaid - expectedAmount) > 0.01) {
            request.setAttribute("error", "Lỗi vi phạm quy tắc BR-09: Số tiền thanh toán (" 
                    + String.format("%,.0f", amountPaid) + "đ) không khớp với giá trị cần thanh toán của đơn hàng (" 
                    + String.format("%,.0f", expectedAmount) + "đ)!");
            request.setAttribute("order", order);
            request.setAttribute("orderDetails", orderDAO.getOrderDetails(orderId));
            request.getRequestDispatcher("/payment.jsp").forward(request, response);
            return;
        }

        // Chuẩn hóa phương thức thanh toán bám sát ENUM trong Database: 'CASH', 'VNPAY', 'MOMO', 'BANK_TRANSFER'
        String dbPaymentMethod = "BANK_TRANSFER"; // Default: VietQR / Chuyển khoản
        if ("momo".equalsIgnoreCase(paymentMethodParam)) {
            dbPaymentMethod = "MOMO";
        } else if ("card".equalsIgnoreCase(paymentMethodParam)) {
            dbPaymentMethod = "VNPAY";
        } else if ("cod".equalsIgnoreCase(paymentMethodParam) || "cash".equalsIgnoreCase(paymentMethodParam)) {
            dbPaymentMethod = "CASH";
        } else if ("vietqr".equalsIgnoreCase(paymentMethodParam) || "bank_transfer".equalsIgnoreCase(paymentMethodParam)) {
            dbPaymentMethod = "BANK_TRANSFER";
        }

        // Khởi tạo đối tượng Payment
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPaymentMethod(dbPaymentMethod);
        payment.setTransactionCode("PAY-" + order.getOrderCode().replace("#", "") + "-" + (System.currentTimeMillis() % 1000000));
        payment.setAmount(amountPaid);
        payment.setStatus("SUCCESS");
        payment.setPaidAt(new Timestamp(System.currentTimeMillis()));
        payment.setPaymentNote("Thanh toán đơn hàng " + order.getOrderCode() + " qua " + dbPaymentMethod);

        // Gọi DAO thực thi JDBC Transaction (Ghi vào payments & đổi trạng thái orders sang CONFIRMED)
        boolean paymentSuccess = paymentDAO.processPayment(payment);

        if (paymentSuccess) {
            // Thanh toán thành công -> Chuyển hướng sang màn hình Hóa đơn (Bill Screen)
            response.sendRedirect(request.getContextPath() + "/bill?orderId=" + orderId);
        } else {
            // Thất bại -> Báo lỗi và cho phép thử lại
            request.setAttribute("error", "Giao dịch thanh toán thất bại! Vui lòng thử lại hoặc chọn phương thức khác.");
            request.setAttribute("order", order);
            request.setAttribute("orderDetails", orderDAO.getOrderDetails(orderId));
            request.getRequestDispatcher("/payment.jsp").forward(request, response);
        }
    }
}
