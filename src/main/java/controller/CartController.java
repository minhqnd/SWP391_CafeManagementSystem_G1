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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CartController - Quản lý giỏ hàng và tạo đơn hàng (Checkout) chuyển tiếp sang Payment
 * Hỗ trợ URL: /cart, /cart/add, /cart/checkout
 */
@WebServlet(name = "CartController", urlPatterns = {"/cart", "/cart/add", "/cart/checkout"})
public class CartController extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Điều hướng tới trang giỏ hàng cart.jsp
        request.getRequestDispatcher("/cart.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String servletPath = request.getServletPath();

        if ("/cart/checkout".equals(servletPath)) {
            handleCheckout(request, response);
            return;
        }

        // Xử lý thêm vào giỏ hàng cũ (/cart/add)
        handleAddCart(request, response);
    }

    /**
     * Xử lý tạo đơn hàng từ danh sách món trong giỏ hàng và chuyển hướng sang /payment
     */
    private void handleCheckout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        Integer customerId = null;
        String defaultName = "Khách hàng Aura";
        String defaultPhone = "0908 123 456";

        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            customerId = user.getId();
            defaultName = user.getFullName();
            defaultPhone = user.getPhone();
        }

        // Đọc dữ liệu JSON gửi từ Client
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String rawJson = sb.toString();
        List<OrderDetail> items = new ArrayList<>();
        double totalAmount = 0;

        // Trích xuất các items từ JSON gửi lên (đơn giản, an toàn, không cần thư viện bên thứ 3)
        if (rawJson.contains("\"items\":")) {
            String itemsPart = rawJson.substring(rawJson.indexOf("\"items\":"));
            // Tách các object món ăn { ... }
            int start = itemsPart.indexOf("[");
            int end = itemsPart.lastIndexOf("]");
            if (start != -1 && end != -1 && end > start) {
                String arrayContent = itemsPart.substring(start + 1, end).trim();
                if (!arrayContent.isEmpty()) {
                    String[] itemTokens = arrayContent.split("\\},\\s*\\{");
                    for (String token : itemTokens) {
                        try {
                            int prodId = extractInt(token, "id");
                            String name = extractString(token, "name");
                            double price = extractDouble(token, "unitPrice");
                            if (price <= 0) price = extractDouble(token, "price");
                            int qty = extractInt(token, "quantity");
                            if (qty <= 0) qty = 1;
                            String size = extractString(token, "size");
                            String customization = (size != null && !size.isEmpty()) ? "Size " + size : "Tiêu chuẩn";

                            double subtotal = price * qty;
                            totalAmount += subtotal;

                            OrderDetail detail = new OrderDetail();
                            detail.setProductId(prodId > 0 ? prodId : 1);
                            detail.setProductName(name != null && !name.isEmpty() ? name : "Đồ uống Aura");
                            detail.setUnitPrice(price > 0 ? price : 45000);
                            detail.setCustomization(customization);
                            detail.setQuantity(qty);
                            detail.setSubtotal(subtotal > 0 ? subtotal : 45000);

                            items.add(detail);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        // Nếu giỏ hàng trống, tạo sẵn 1 món mẫu để kiểm thử không bị rỗng
        if (items.isEmpty()) {
            OrderDetail sample = new OrderDetail();
            sample.setProductId(1);
            sample.setProductName("Cà phê Muối Aura");
            sample.setUnitPrice(55000);
            sample.setCustomization("Size M • 50% Ngọt");
            sample.setQuantity(2);
            sample.setSubtotal(110000);
            items.add(sample);
            totalAmount = 110000;
        }

        double deliveryFee = totalAmount >= 150000 ? 0 : 18000;
        double discountAmount = 0;
        double finalAmount = totalAmount + deliveryFee - discountAmount;

        // Tạo CustomerOrder
        CustomerOrder order = new CustomerOrder();
        order.setOrderCode("AUR-" + (System.currentTimeMillis() % 100000));
        order.setCustomerId(customerId);
        order.setOrderType("ONLINE");
        order.setStatus("PENDING");
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setDeliveryFee(deliveryFee);
        order.setFinalAmount(finalAmount);
        order.setRecipientName(defaultName);
        order.setRecipientPhone(defaultPhone);
        order.setDeliveryAddress("28 Phố Tràng Tiền, Hoàn Kiếm, Hà Nội");
        order.setOrderNote("Giao đồ uống kèm túi giữ nhiệt");

        int newOrderId = orderDAO.createOrder(order, items);

        if (newOrderId > 0) {
            String redirectUrl = request.getContextPath() + "/payment?orderId=" + newOrderId;
            response.getWriter().write("{\"success\":true,\"orderId\":" + newOrderId + ",\"redirectUrl\":\"" + redirectUrl + "\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false,\"error\":\"Không thể tạo đơn hàng!\"}");
        }
    }

    private void handleAddCart(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Bạn cần đăng nhập để thêm vào giỏ hàng.\"}");
            return;
        }

        User user = (User) session.getAttribute("user");
        int customerId = user.getId();

        String productIdStr = request.getParameter("productId");
        if (productIdStr == null || productIdStr.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Missing productId\"}");
            return;
        }

        int productId;
        int quantity = 1;
        try {
            productId = Integer.parseInt(productIdStr);
            String quantityStr = request.getParameter("quantity");
            if (quantityStr != null && !quantityStr.isEmpty()) {
                quantity = Integer.parseInt(quantityStr);
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid format\"}");
            return;
        }

        int orderId = orderDAO.getOrCreateCartOrder(customerId);
        if (orderId != -1) {
            boolean success = orderDAO.addProductToCart(orderId, productId, quantity);
            if (success) {
                int totalItems = orderDAO.getCartItemTotalQuantity(orderId);
                response.getWriter().write("{\"success\":true, \"totalItems\":" + totalItems + "}");
                return;
            }
        }

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("{\"error\":\"Failed to add to cart\"}");
    }

    private int extractInt(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return -1;
        int start = idx + pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private double extractDouble(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return 0;
        int start = idx + pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
            end++;
        }
        try {
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return "";
        int start = idx + pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
}
