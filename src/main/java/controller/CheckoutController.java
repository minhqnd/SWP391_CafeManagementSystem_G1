package controller;

import dao.OrderDAO;
import model.Cart;
import model.CartItem;
import model.Order;
import model.User;
import service.CartService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * CheckoutController - Bước 2: Xác nhận đơn & Giao nhận
 * GET  /checkout : Ai cũng xem được, nhưng chưa đăng nhập thì các lựa chọn bị khoá
 * POST /checkout : Bắt buộc đăng nhập - tạo đơn hàng rồi chuyển sang bước Thanh toán
 */
@WebServlet(name = "CheckoutController", urlPatterns = {"/checkout"})
public class CheckoutController extends HttpServlet {

    public static final String METHOD_DELIVERY = "DELIVERY";
    public static final String METHOD_TAKEAWAY = "TAKEAWAY";
    public static final String STORE_ADDRESS = "28 Phố Tràng Tiền, Hoàn Kiếm, Hà Nội";

    private static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MIN_SCHEDULE_MINUTES = 30;
    private static final String PHONE_PATTERN = "0\\d{9,10}";

    private final CartService cartService = new CartService();
    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        Cart cart = CartController.getCart(session);
        cartService.refreshCart(cart, CartController.getCustomerId(session));

        if (cart.getSelectedItems().isEmpty()) {
            session.setAttribute("cartError", "Vui lòng chọn ít nhất một món để đặt hàng.");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        String method = METHOD_TAKEAWAY.equals(request.getParameter("method")) ? METHOD_TAKEAWAY : METHOD_DELIVERY;
        showCheckout(request, response, cart, method);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Cart cart = CartController.getCart(session);
        cartService.refreshCart(cart, CartController.getCustomerId(session));

        if (cart.getSelectedItems().isEmpty()) {
            session.setAttribute("cartError", "Vui lòng chọn ít nhất một món để đặt hàng.");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        String method = METHOD_TAKEAWAY.equals(request.getParameter("method")) ? METHOD_TAKEAWAY : METHOD_DELIVERY;

        // Đổi hình thức nhận hàng: hiển thị lại trang và giữ nguyên thông tin đã nhập
        String switchTo = request.getParameter("switchTo");
        if (switchTo != null) {
            method = METHOD_TAKEAWAY.equals(switchTo) ? METHOD_TAKEAWAY : METHOD_DELIVERY;
            showCheckout(request, response, cart, method);
            return;
        }

        // Bắt buộc đăng nhập mới được đặt hàng
        User user = (User) session.getAttribute("user");
        if (user == null) {
            session.setAttribute("errorMsg", "Vui lòng đăng nhập để tiếp tục đặt hàng.");
            response.sendRedirect(request.getContextPath() + "/checkout");
            return;
        }



        boolean homeDelivery = METHOD_DELIVERY.equals(method);
        String recipientName = trim(request.getParameter("recipientName"));
        String recipientPhone = trim(request.getParameter("recipientPhone")).replaceAll("[\\s.]", "");
        String deliveryAddress = trim(request.getParameter("deliveryAddress"));
        String scheduledTime = trim(request.getParameter("scheduledTime"));
        String orderNote = trim(request.getParameter("orderNote"));
        boolean scheduled = "SCHEDULED".equals(request.getParameter("deliveryTime"));

        String error = null;
        if (recipientName.isEmpty() || recipientName.length() > 100) {
            error = "Vui lòng nhập họ tên người nhận (tối đa 100 ký tự).";
        } else if (!recipientPhone.matches(PHONE_PATTERN)) {
            error = "Số điện thoại không hợp lệ (10 - 11 chữ số, bắt đầu bằng số 0).";
        } else if (homeDelivery && (deliveryAddress.length() < 10 || deliveryAddress.length() > 255)) {
            error = "Vui lòng nhập địa chỉ giao hàng chi tiết (số nhà, đường, phường/xã, quận/huyện).";
        } else if (orderNote.length() > 500) {
            error = "Ghi chú tối đa 500 ký tự.";
        } else if (scheduled) {
            error = validateScheduledTime(scheduledTime);
        }

        if (error != null) {
            request.setAttribute("checkoutError", error);
            showCheckout(request, response, cart, method);
            return;
        }

        List<CartItem> items = cart.getSelectedItems();
        double deliveryFee = cart.getDeliveryFee(homeDelivery);

        Order order = new Order();
        order.setOrderCode("AURA" + LocalDateTime.now(SHOP_ZONE).format(DateTimeFormatter.ofPattern("yyMMddHHmmssSSS")));
        order.setCustomerId(user.getId());
        order.setOrderType(homeDelivery ? "ONLINE" : "TAKEAWAY");
        order.setStatus("PENDING");
        order.setTotalAmount(cart.getSelectedSubtotal());
        order.setDiscountAmount(cart.getDiscount());
        order.setDeliveryFee(deliveryFee);
        order.setFinalAmount(cart.getTotal() + deliveryFee);
        order.setRecipientName(recipientName);
        order.setRecipientPhone(recipientPhone);
        order.setDeliveryAddress(homeDelivery ? deliveryAddress : null);
        order.setOrderNote(buildOrderNote(homeDelivery, scheduled ? scheduledTime : null, orderNote));

        int orderId = orderDAO.placeOrder(order, items, cart.getPointsUsed());
        if (orderId <= 0) {
            request.setAttribute("checkoutError", "Không thể tạo đơn hàng lúc này. Vui lòng thử lại sau ít phút.");
            showCheckout(request, response, cart, method);
            return;
        }

        // Đặt hàng thành công: bỏ các món đã đặt khỏi giỏ và chuyển sang bước Thanh toán
        cart.getItems().removeAll(items);
        cart.setPointsUsed(0);
        response.sendRedirect(request.getContextPath() + "/payment?orderId=" + orderId);
    }

    private void showCheckout(HttpServletRequest request, HttpServletResponse response, Cart cart, String method)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("user");
        boolean homeDelivery = METHOD_DELIVERY.equals(method);
        double deliveryFee = cart.getDeliveryFee(homeDelivery);

        // Lần đầu mở trang: điền sẵn thông tin từ tài khoản; khi gửi lại form: giữ nguyên dữ liệu khách đã nhập
        String defaultName = user != null ? user.getFullName() : "";
        String defaultPhone = user != null && user.getPhone() != null && user.getPhone().matches(PHONE_PATTERN)
                ? user.getPhone() : "";

        request.setAttribute("cart", cart);
        request.setAttribute("method", method);
        request.setAttribute("deliveryFee", deliveryFee);
        request.setAttribute("grandTotal", cart.getTotal() + deliveryFee);
        request.setAttribute("storeAddress", STORE_ADDRESS);
        request.setAttribute("recipientName", paramOrDefault(request, "recipientName", defaultName));
        request.setAttribute("recipientPhone", paramOrDefault(request, "recipientPhone", defaultPhone));
        request.setAttribute("deliveryAddress", paramOrDefault(request, "deliveryAddress", ""));
        request.setAttribute("deliveryTime", paramOrDefault(request, "deliveryTime", "NOW"));
        request.setAttribute("scheduledTime", paramOrDefault(request, "scheduledTime", ""));
        request.setAttribute("orderNote", paramOrDefault(request, "orderNote", ""));
        // Đăng nhập / đăng ký từ trang này xong sẽ quay lại đúng trang Checkout
        request.setAttribute("authRedirect", "/checkout");

        request.getRequestDispatcher("/checkout.jsp").forward(request, response);
    }

    private String validateScheduledTime(String value) {
        LocalTime time;
        try {
            time = LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            return "Vui lòng chọn giờ hẹn nhận hàng.";
        }
        LocalDateTime scheduledAt = LocalDate.now(SHOP_ZONE).atTime(time);
        if (scheduledAt.isBefore(LocalDateTime.now(SHOP_ZONE).plusMinutes(MIN_SCHEDULE_MINUTES))) {
            return "Giờ hẹn phải sau thời điểm hiện tại ít nhất " + MIN_SCHEDULE_MINUTES + " phút (trong ngày hôm nay).";
        }
        return null;
    }

    private String buildOrderNote(boolean homeDelivery, String scheduledTime, String note) {
        StringBuilder result = new StringBuilder();
        if (scheduledTime != null) {
            result.append(homeDelivery ? "Hẹn giờ giao: " : "Hẹn giờ lấy: ").append(scheduledTime);
        }
        if (!note.isEmpty()) {
            if (result.length() > 0) {
                result.append(" | ");
            }
            result.append(note);
        }
        return result.length() > 0 ? result.toString() : null;
    }

    private String paramOrDefault(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getParameter(name);
        return value != null ? value : defaultValue;
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }
}
