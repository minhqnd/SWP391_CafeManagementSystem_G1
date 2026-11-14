package controller;

import dao.OrderDAO;
import dao.PaymentDAO;
import dao.ShopSettingDAO;
import model.Order;
import model.Payment;
import model.ShopSettings;
import model.User;
import service.PaymentService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * PaymentController - Servlet điều khiển luồng thanh toán đơn hàng online.
 * 
 * Các chức năng chính:
 * 1. GET  /payment               : Hiển thị giao diện thanh toán chuyển khoản VietQR hoặc chọn COD.
 * 2. GET  /payment?action=status : API để trang JSP gọi định kỳ (polling) kiểm tra đơn đã thanh toán xong chưa.
 * 3. POST /payment?action=cod    : Khách hàng chọn hình thức thanh toán khi nhận hàng (COD).
 */
@WebServlet(name = "PaymentController", urlPatterns = {"/payment"})
public class PaymentController extends HttpServlet {

    // Khởi tạo các DAO để thao tác với cơ sở dữ liệu
    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();

    /**
     * Xử lý request GET: Hiển thị trang thanh toán hoặc trả về kết quả kiểm tra trạng thái thanh toán.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Bước 1: Kiểm tra xem người dùng đã đăng nhập tài khoản khách hàng chưa
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (!CartController.isCustomer(user)) {
            // Chưa đăng nhập thì chuyển hướng về trang chủ và mở popup đăng nhập
            response.sendRedirect(request.getContextPath() + "/?login=true");
            return;
        }

        // Bước 2: Lấy orderId từ tham số trên URL
        int orderId = parseInt(request.getParameter("orderId"));

        // Bước 3: Nếu trang payment.jsp đang polling kiểm tra trạng thái thanh toán (?action=status)
        String action = trim(request.getParameter("action"));
        if ("status".equals(action)) {
            handleStatusCheck(orderId, user.getId(), session, response);
            return;
        }

        // Bước 4: Tìm đơn hàng trong cơ sở dữ liệu (chỉ lấy đơn đúng của khách này)
        Order order = orderDAO.getCustomerOrder(orderId, user.getId());
        if (order == null) {
            session.setAttribute("ordersError", "Không tìm thấy đơn hàng cần thanh toán.");
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }

        // Bước 5: Nếu đơn không còn ở trạng thái chờ thanh toán (PENDING) -> Chuyển thẳng sang hóa đơn
        if (!order.isPending()) {
            response.sendRedirect(request.getContextPath() + "/bill?orderId=" + orderId);
            return;
        }

        // Bước 6: Đảm bảo đã có bản ghi thanh toán chuyển khoản ngân hàng (bảng payments)
        Payment payment = paymentDAO.ensurePendingBankTransfer(orderId, user.getId());
        if (payment == null) {
            session.setAttribute("paymentError", "Không thể khởi tạo giao dịch thanh toán.");
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }

        // Bước 7: Lấy cấu hình quán (thông tin tài khoản ngân hàng, gateway URL,...)
        ShopSettings settings = shopSettingDAO.getSettings();

        // Bước 8: Đăng ký đơn hàng lên Payment Gateway (Cloudflare Worker) để chờ nhận tiền
        PaymentService.registerOrderToGateway(
                settings.getPaymentGatewayUrl(),
                "aura_cafe",
                order.getOrderCode(),
                order.getFinalAmount(),
                payment.getTransactionCode()
        );

        // Chuyển thông báo lỗi từ session sang request (nếu có)
        CartController.moveFlashMessage(session, request, "paymentError");

        // Bước 9: Đưa dữ liệu vào request attribute để truyền sang trang payment.jsp
        request.setAttribute("order", order);
        request.setAttribute("payment", payment);
        request.setAttribute("bankName", settings.getPaymentBankName());
        request.setAttribute("accountNo", settings.getPaymentAccountNo());
        request.setAttribute("accountName", settings.getPaymentAccountName());
        request.setAttribute("qrUrl", buildQrUrl(order, settings));
        request.setAttribute("storeAddress", CheckoutController.STORE_ADDRESS);
        request.setAttribute("deliveryMinutes", settings.getDeliveryMinutes());

        // Điểm thưởng tích lũy dự kiến nhận được sau khi hoàn thành đơn
        double netAmount = order.getTotalAmount() - order.getDiscountAmount();
        int expectedPoints = settings.calculateEarnedPoints(netAmount);
        request.setAttribute("expectedRewardPoints", expectedPoints);

        // Bước 10: Chuyển tiếp tới giao diện payment.jsp
        request.getRequestDispatcher("/payment.jsp").forward(request, response);
    }

    /**
     * Xử lý request POST: Chọn thanh toán COD hoặc kiểm tra trạng thái thanh toán.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // Thao tác của khách hàng tại giao diện thanh toán
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (!CartController.isCustomer(user)) {
            response.sendRedirect(request.getContextPath() + "/?login=true");
            return;
        }

        int orderId = parseInt(request.getParameter("orderId"));
        String action = trim(request.getParameter("action"));

        // Nếu gọi kiểm tra trạng thái bằng POST
        if ("status".equals(action)) {
            handleStatusCheck(orderId, user.getId(), session, response);
            return;
        }

        // Kiểm tra đơn hàng có tồn tại không
        Order order = orderDAO.getCustomerOrder(orderId, user.getId());
        if (order == null) {
            session.setAttribute("ordersError", "Không tìm thấy đơn hàng cần thanh toán.");
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }

        // Khách hàng bấm chọn "Thanh toán khi nhận hàng (COD)"
        if ("cod".equals(action)) {
            boolean success = paymentDAO.acceptCod(orderId, user.getId());
            if (success) {
                // Xóa các món trong đơn này ra khỏi giỏ hàng
                CheckoutController.removePaidItems(session, orderId);
                session.setAttribute("ordersMessage",
                        "Đã xác nhận thanh toán COD cho đơn #" + order.getOrderCode()
                                + ". Điểm tích lũy sẽ được cộng sau khi giao hàng và thu tiền.");
                response.sendRedirect(request.getContextPath() + "/bill?orderId=" + orderId);
            } else {
                session.setAttribute("paymentError", "Không thể chọn COD cho trạng thái hiện tại của đơn.");
                response.sendRedirect(request.getContextPath() + "/payment?orderId=" + orderId);
            }
            return;
        }

        // Với chuyển khoản ngân hàng: không xác nhận thủ công bằng nút submit,
        // hệ thống chỉ tự động duyệt qua Webhook hoặc Polling Gateway
        session.setAttribute("paymentError", "Giao dịch đang chờ cổng thanh toán xác nhận tự động.");
        response.sendRedirect(request.getContextPath() + "/payment?orderId=" + orderId);
    }

    /**
     * Xử lý Polling: Kiểm tra trạng thái đơn hàng và trả về JSON cho frontend.
     * URL gọi: /payment?action=status&orderId=...
     */
    private void handleStatusCheck(int orderId, int customerId, HttpSession session, HttpServletResponse response)
            throws IOException {

        // Thiết lập kiểu dữ liệu trả về là JSON UTF-8
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 1. Tìm đơn hàng
        Order order = orderDAO.getCustomerOrder(orderId, customerId);
        if (order == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"paid\":false,\"status\":\"NOT_FOUND\"}");
            return;
        }

        // 2. Nếu đơn hàng đã được cập nhật xong trước đó rồi (không còn PENDING)
        if (!order.isPending()) {
            CheckoutController.removePaidItems(session, orderId);
            response.getWriter().write("{\"paid\":true,\"status\":\"" + escapeJson(order.getStatus()) + "\"}");
            return;
        }

        // 3. Nếu đơn vẫn đang PENDING: Gọi sang Payment Gateway kiểm tra xem khách đã chuyển khoản chưa
        ShopSettings settings = shopSettingDAO.getSettings();
        boolean isPaidOnGateway = PaymentService.isOrderPaidOnGateway(settings.getPaymentGatewayUrl(), order.getOrderCode());

        if (isPaidOnGateway) {
            // Khách đã chuyển khoản thành công -> Tiến hành chốt đơn trong DB của quán
            Payment payment = paymentDAO.ensurePendingBankTransfer(orderId, customerId);
            String txCode = (payment != null) ? payment.getTransactionCode() : "";
            BigDecimal amount = BigDecimal.valueOf(Math.round(order.getFinalAmount()));
            String payload = "{\"orderCode\":\"" + escapeJson(order.getOrderCode()) + "\",\"status\":\"SUCCESS\"}";

            PaymentDAO.CallbackResult result = paymentDAO.applyGatewayCallback(
                    order.getOrderCode(),
                    txCode,
                    amount,
                    "SUCCESS",
                    "SEPAY-QR",
                    payload,
                    settings.getPointsEarnUnit()
            );

            if (result.successful()) {
                // Xóa món đã thanh toán khỏi giỏ hàng
                CheckoutController.removePaidItems(session, orderId);
                // Báo cho giao diện biết thanh toán thành công
                response.getWriter().write("{\"paid\":true,\"status\":\"CONFIRMED\"}");
                return;
            }
        }

        // Vẫn chưa nhận được tiền: trả về paid = false để frontend tiếp tục chờ
        response.getWriter().write("{\"paid\":false,\"status\":\"PENDING\"}");
    }

    /**
     * Tạo URL ảnh mã VietQR với các thông tin của quán và đơn hàng.
     */
    private String buildQrUrl(Order order, ShopSettings settings) {
        String bankId = URLEncoder.encode(settings.getPaymentBankId(), StandardCharsets.UTF_8);
        String accountNo = URLEncoder.encode(settings.getPaymentAccountNo(), StandardCharsets.UTF_8);
        String accountName = URLEncoder.encode(settings.getPaymentAccountName(), StandardCharsets.UTF_8);
        String orderCode = URLEncoder.encode(order.getOrderCode(), StandardCharsets.UTF_8);
        String storeName = URLEncoder.encode("AURA Cafe", StandardCharsets.UTF_8);
        long amount = Math.round(order.getFinalAmount());

        return "https://vietqr.app/img?bank=" + bankId
                + "&acc=" + accountNo
                + "&template=compact"
                + "&amount=" + amount
                + "&des=" + orderCode
                + "&showinfo=true&fullacc=true"
                + "&holder=" + accountName
                + "&store=" + storeName;
    }

    /**
     * Chuyển chuỗi sang số nguyên an toàn, tránh lỗi NumberFormatException.
     */
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Cắt khoảng trắng đầu cuối và tránh lỗi NullPointerException.
     */
    private String trim(String value) {
        return (value == null) ? "" : value.trim();
    }

    /**
     * Escape các ký tự đặc biệt trong chuỗi để tạo JSON hợp lệ.
     */
    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

