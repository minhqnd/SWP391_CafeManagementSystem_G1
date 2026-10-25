package controller;

import dao.OrderDAO;
import dao.UserDAO;
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
import java.util.List;

/**
 * CheckoutController - Bước 2: Xác nhận đơn & Giao nhận
 * GET /checkout : Ai cũng xem được, nhưng chưa đăng nhập thì các lựa chọn bị
 * khoá
 * POST /checkout : Bắt buộc đăng nhập - tạo đơn hàng rồi chuyển sang bước Thanh
 * toán
 */
@WebServlet(name = "CheckoutController", urlPatterns = { "/checkout" })
public class CheckoutController extends HttpServlet {

    public static final String STORE_ADDRESS = "28 Phố Tràng Tiền, Hoàn Kiếm, Hà Nội";

    private static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String PHONE_PATTERN = "0\\d{9,10}";

    // Quán ở Hà Nội: chỉ giao trong các quận nội thành
    public static final String CITY = "Hà Nội";
    public static final List<String> HANOI_DISTRICTS = List.of(
            "Ba Đình", "Hoàn Kiếm", "Tây Hồ", "Long Biên", "Cầu Giấy", "Đống Đa",
            "Hai Bà Trưng", "Hoàng Mai", "Thanh Xuân", "Nam Từ Liêm", "Bắc Từ Liêm", "Hà Đông");
    // Giờ nhận hẹn trước: từ giờ mở cửa đến 30 phút trước giờ đóng cửa (22:00)
    private static final LocalTime OPEN_TIME = LocalTime.of(7, 0);
    private static final LocalTime LAST_ORDER_TIME = LocalTime.of(21, 30);
    private static final int MIN_SCHEDULE_MINUTES = 30;

    private final CartService cartService = new CartService();
    private final OrderDAO orderDAO = new OrderDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        Cart cart = CartController.getCart(session);
        cart.setPointsUsed(0);
        boolean pointsStillValid = cartService.refreshCart(cart, CartController.getCustomerId(session));

        if (cart.getSelectedItems().isEmpty()) {
            session.setAttribute("cartError", "Vui lòng chọn ít nhất một món để đặt hàng.");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        if (!pointsStillValid) {
            request.setAttribute("checkoutError", CartController.pointsAdjustedMessage(cart));
        }
        showCheckout(request, response, cart);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Cart cart = CartController.getCart(session);
        cart.setPointsUsed(0);
        boolean pointsStillValid = cartService.refreshCart(cart, CartController.getCustomerId(session));

        if (cart.getSelectedItems().isEmpty()) {
            session.setAttribute("cartError", "Vui lòng chọn ít nhất một món để đặt hàng.");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        // Chỉ tài khoản khách hàng được tạo đơn online. ADMIN/STAFF không được
        // dùng ID nội bộ làm customer_id của đơn hàng.
        User user = (User) session.getAttribute("user");
        if (!CartController.isCustomer(user)) {
            request.setAttribute("checkoutError", user == null
                    ? "Vui lòng đăng nhập bằng tài khoản khách hàng để tiếp tục đặt hàng."
                    : "Tài khoản quản trị/nhân viên không thể đặt hàng online. Vui lòng đăng nhập bằng tài khoản khách hàng.");
            showCheckout(request, response, cart);
            return;
        }

        if (!pointsStillValid) {
            request.setAttribute("checkoutError", CartController.pointsAdjustedMessage(cart)
                    + " Tổng tiền đã được cập nhật lại, vui lòng kiểm tra trước khi xác nhận.");
            showCheckout(request, response, cart);
            return;
        }
        // Hình thức nhận (SRS 2.3.2) quyết định phí giao hàng và các trường bắt buộc
        cart.setFulfillment(request.getParameter("fulfillment"));
        String recipientName = trim(request.getParameter("recipientName"));
        String recipientPhone = trim(request.getParameter("recipientPhone")).replaceAll("[\\s.]", "");
        String district = trim(request.getParameter("district"));
        String ward = trim(request.getParameter("ward"));
        String addressDetail = trim(request.getParameter("addressDetail"));
        String tableNumber = trim(request.getParameter("tableNumber"));
        String timeOption = trim(request.getParameter("timeOption"));
        String scheduledTime = trim(request.getParameter("scheduledTime"));
        String orderNote = trim(request.getParameter("orderNote"));

        String error = null;
        LocalDateTime scheduledAt = null;
        if (recipientName.isEmpty() || recipientName.length() > 100) {
            error = "Vui lòng nhập họ tên người nhận (tối đa 100 ký tự).";
        } else if (!recipientPhone.matches(PHONE_PATTERN)) {
            error = "Số điện thoại không hợp lệ (10 - 11 chữ số, bắt đầu bằng số 0).";
        } else if (cart.isDelivery() && !HANOI_DISTRICTS.contains(district)) {
            error = "Vui lòng chọn Quận/Huyện giao hàng (quán chỉ giao trong nội thành Hà Nội).";
        } else if (cart.isDelivery() && (ward.length() < 2 || ward.length() > 100)) {
            error = "Vui lòng nhập Phường/Xã giao hàng.";
        } else if (cart.isDelivery() && (addressDetail.length() < 5 || addressDetail.length() > 150)) {
            error = "Vui lòng nhập địa chỉ chi tiết (số nhà, tên đường, toà nhà...).";
        } else if (Cart.FULFILLMENT_DINE_IN.equals(cart.getFulfillment()) && !tableNumber.matches("[A-Za-z0-9 -]{0,20}")) {
            error = "Số bàn chỉ gồm chữ, số (tối đa 20 ký tự).";
        } else if (orderNote.length() > 500) {
            error = "Ghi chú tối đa 500 ký tự.";
        } else if ("SCHEDULED".equals(timeOption) && !Cart.FULFILLMENT_DINE_IN.equals(cart.getFulfillment())) {
            try {
                scheduledAt = parseScheduledTime(scheduledTime);
            } catch (IllegalArgumentException e) {
                error = e.getMessage();
            }
        }

        if (error != null) {
            request.setAttribute("checkoutError", error);
            showCheckout(request, response, cart);
            return;
        }
        String deliveryAddress = cart.isDelivery()
                ? addressDetail + ", " + ward + ", " + district + ", " + CITY
                : null;

        List<CartItem> items = cart.getSelectedItems();
        int pointsUsed = cart.getPointsUsed();

        Order order = new Order();
        order.setOrderCode(
                "AURA" + LocalDateTime.now(SHOP_ZONE).format(DateTimeFormatter.ofPattern("yyMMddHHmmssSSS")));
        order.setCustomerId(user.getId());
        order.setOrderType(cart.getOrderType());
        order.setStatus("PENDING");
        order.setTotalAmount(cart.getSelectedSubtotal());
        order.setDiscountAmount(cart.getDiscount());
        order.setDeliveryFee(cart.getDeliveryFee());
        order.setFinalAmount(cart.getGrandTotal());
        order.setRecipientName(recipientName);
        order.setRecipientPhone(recipientPhone);
        order.setDeliveryAddress(deliveryAddress);
        order.setTableNumber(Cart.FULFILLMENT_DINE_IN.equals(cart.getFulfillment()) && !tableNumber.isEmpty() ? tableNumber : null);
        order.setScheduledAt(scheduledAt);
        order.setOrderNote(orderNote.isEmpty() ? null : orderNote);

        int orderId = orderDAO.placeOrder(order, items, pointsUsed);
        if (orderId == OrderDAO.NOT_ENOUGH_STOCK) {
            request.setAttribute("checkoutError", "Có món đã hết hoặc không đủ số lượng. Vui lòng sửa giỏ hàng.");
            showCheckout(request, response, cart);
            return;
        }
        if (orderId == OrderDAO.NOT_ENOUGH_POINTS) {
            // Điểm vừa bị dùng ở đơn khác: đọc lại số điểm và đưa về mốc hợp lệ
            cartService.refreshCart(cart, user.getId());
            user.setTotalPoints(cart.getAvailablePoints());
            request.setAttribute("checkoutError", "Số điểm Aura của bạn vừa thay đổi nên chưa thể trừ " + pointsUsed
                    + " điểm. Tổng tiền đã được cập nhật lại, vui lòng kiểm tra trước khi xác nhận.");
            showCheckout(request, response, cart);
            return;
        }
        if (orderId <= 0) {
            request.setAttribute("checkoutError", "Không thể tạo đơn hàng lúc này. Vui lòng thử lại sau ít phút.");
            showCheckout(request, response, cart);
            return;
        }

        // Đặt hàng thành công: bỏ các món đã đặt khỏi giỏ và chuyển sang bước Thanh
        // toán
        cart.getItems().removeAll(items);
        cart.setPointsUsed(0);
        if (pointsUsed > 0) {
            user.setTotalPoints(userDAO.getTotalPoints(user.getId()));
        }
        response.sendRedirect(request.getContextPath() + "/payment?orderId=" + orderId);
    }

    private void showCheckout(HttpServletRequest request, HttpServletResponse response, Cart cart)
            throws ServletException, IOException {

        User sessionUser = (User) request.getSession().getAttribute("user");
        User user = CartController.isCustomer(sessionUser) ? sessionUser : null;

        // Lần đầu mở trang: điền sẵn thông tin từ tài khoản; khi gửi lại form: giữ
        // nguyên dữ liệu khách đã nhập
        String defaultName = user != null ? user.getFullName() : "";
        String defaultPhone = user != null && user.getPhone() != null && user.getPhone().matches(PHONE_PATTERN)
                ? user.getPhone()
                : "";

        request.setAttribute("cart", cart);
        try {
            String stockError = orderDAO.getCheckoutStockError(cart.getSelectedItems());
            request.setAttribute("checkoutStockBlocked", stockError != null);
            if (stockError != null) request.setAttribute("checkoutError", stockError);
        } catch (java.sql.SQLException | ClassNotFoundException e) {
            getServletContext().log("Cannot check checkout stock", e);
            request.setAttribute("checkoutStockBlocked", true);
            request.setAttribute("checkoutError", "Chưa kiểm tra được số lượng món. Vui lòng thử lại sau.");
        }
        request.setAttribute("recipientName", paramOrDefault(request, "recipientName", defaultName));
        request.setAttribute("recipientPhone", paramOrDefault(request, "recipientPhone", defaultPhone));
        request.setAttribute("district", paramOrDefault(request, "district", ""));
        request.setAttribute("ward", paramOrDefault(request, "ward", ""));
        request.setAttribute("addressDetail", paramOrDefault(request, "addressDetail", ""));
        request.setAttribute("tableNumber", paramOrDefault(request, "tableNumber", ""));
        request.setAttribute("timeOption", paramOrDefault(request, "timeOption", "ASAP"));
        request.setAttribute("scheduledTime", paramOrDefault(request, "scheduledTime", ""));
        request.setAttribute("orderNote", paramOrDefault(request, "orderNote", ""));
        request.setAttribute("hanoiDistricts", HANOI_DISTRICTS);
        request.setAttribute("city", CITY);
        request.setAttribute("storeAddress", STORE_ADDRESS);
        request.setAttribute("deliveryMinutes", cart.getSettings().getDeliveryMinutes());
        request.setAttribute("openTime", OPEN_TIME.toString());
        request.setAttribute("lastOrderTime", LAST_ORDER_TIME.toString());
        // Đăng nhập / đăng ký từ trang này xong sẽ quay lại đúng trang Checkout
        request.setAttribute("authRedirect", "/checkout");

        request.getRequestDispatcher("/checkout.jsp").forward(request, response);
    }

    /**
     * Giờ hẹn nhận trong hôm nay (SRS 2.3.3 "Scheduled Delivery"): HH:mm, trong giờ mở cửa
     * và cách hiện tại ít nhất 30 phút để quán kịp pha chế / giao hàng.
     */
    private LocalDateTime parseScheduledTime(String raw) {
        if (raw == null || !raw.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
            throw new IllegalArgumentException("Vui lòng chọn giờ hẹn nhận hợp lệ (HH:mm).");
        }
        LocalTime time = LocalTime.parse(raw);
        if (time.isBefore(OPEN_TIME) || time.isAfter(LAST_ORDER_TIME)) {
            throw new IllegalArgumentException("Giờ hẹn phải trong khoảng " + OPEN_TIME + " - " + LAST_ORDER_TIME + ".");
        }
        LocalDateTime now = LocalDateTime.now(SHOP_ZONE);
        LocalDateTime scheduled = LocalDateTime.of(LocalDate.now(SHOP_ZONE), time);
        if (scheduled.isBefore(now.plusMinutes(MIN_SCHEDULE_MINUTES))) {
            throw new IllegalArgumentException("Giờ hẹn phải sau thời điểm hiện tại ít nhất " + MIN_SCHEDULE_MINUTES + " phút.");
        }
        return scheduled;
    }

    private String paramOrDefault(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getParameter(name);
        return value != null ? value : defaultValue;
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }
}
