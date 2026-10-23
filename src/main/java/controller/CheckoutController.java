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
import java.time.LocalDateTime;
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
        String recipientName = trim(request.getParameter("recipientName"));
        String recipientPhone = trim(request.getParameter("recipientPhone")).replaceAll("[\\s.]", "");
        String deliveryAddress = trim(request.getParameter("deliveryAddress"));
        String orderNote = trim(request.getParameter("orderNote"));

        String error = null;
        if (recipientName.isEmpty() || recipientName.length() > 100) {
            error = "Vui lòng nhập họ tên người nhận (tối đa 100 ký tự).";
        } else if (!recipientPhone.matches(PHONE_PATTERN)) {
            error = "Số điện thoại không hợp lệ (10 - 11 chữ số, bắt đầu bằng số 0).";
        } else if (deliveryAddress.length() < 10 || deliveryAddress.length() > 255) {
            error = "Vui lòng nhập địa chỉ giao hàng chi tiết (số nhà, đường, phường/xã, quận/huyện).";
        } else if (orderNote.length() > 500) {
            error = "Ghi chú tối đa 500 ký tự.";
        }

        if (error != null) {
            request.setAttribute("checkoutError", error);
            showCheckout(request, response, cart);
            return;
        }

        List<CartItem> items = cart.getSelectedItems();
        int pointsUsed = cart.getPointsUsed();

        Order order = new Order();
        order.setOrderCode(
                "AURA" + LocalDateTime.now(SHOP_ZONE).format(DateTimeFormatter.ofPattern("yyMMddHHmmssSSS")));
        order.setCustomerId(user.getId());
        order.setOrderType("ONLINE");
        order.setStatus("PENDING");
        order.setTotalAmount(cart.getSelectedSubtotal());
        order.setDiscountAmount(cart.getDiscount());
        order.setDeliveryFee(cart.getDeliveryFee());
        order.setFinalAmount(cart.getGrandTotal());
        order.setRecipientName(recipientName);
        order.setRecipientPhone(recipientPhone);
        order.setDeliveryAddress(deliveryAddress);
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
        request.setAttribute("deliveryAddress", paramOrDefault(request, "deliveryAddress", ""));
        request.setAttribute("orderNote", paramOrDefault(request, "orderNote", ""));
        // Đăng nhập / đăng ký từ trang này xong sẽ quay lại đúng trang Checkout
        request.setAttribute("authRedirect", "/checkout");

        request.getRequestDispatcher("/checkout.jsp").forward(request, response);
    }

    private String paramOrDefault(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getParameter(name);
        return value != null ? value : defaultValue;
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }
}
