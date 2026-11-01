package controller;

import dao.CategoryDAO;
import dao.OrderDAO;
import dao.ProductDAO;
import dao.UserDAO;
import model.Category;
import model.Order;
import model.OrderDetail;
import model.Product;
import model.User;
import model.CartItem;
import model.ShopSettings;
import dao.ShopSettingDAO;
import dao.PaymentDAO;
import service.CartService;
import service.PaymentService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CounterOrderController - Màn hình tạo và xử lý đơn tại quầy (Counter POS)
 * URLs: /cashier, /counter-order
 */
@WebServlet(name = "CounterOrderController", urlPatterns = {"/cashier", "/counter-order"})
public class CounterOrderController extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final CartService cartService = new CartService();

    private boolean isAuthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        String accept = request.getHeader("Accept");
        String contentType = request.getContentType();
        boolean isApi = (accept != null && accept.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"))
                || request.getParameter("action") != null;

        if (user == null) {
            if (isApi) {
                sendJson(response, HttpServletResponse.SC_UNAUTHORIZED, "{\"success\":false,\"error\":\"Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return false;
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()) || !"STAFF".equalsIgnoreCase(user.getRole())
                || !"CASHIER".equalsIgnoreCase(user.getPosition())) {
            if (isApi) {
                sendJson(response, HttpServletResponse.SC_FORBIDDEN, "{\"success\":false,\"error\":\"Chỉ nhân viên hoặc quản trị viên được phép truy cập POS.\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/login?error=unauthorized");
            }
            return false;
        }

        return true;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAuthorized(request, response)) {
            return;
        }

        String action = request.getParameter("action");

        // API: Tìm khách hàng hội viên theo SĐT
        if ("search-customer".equals(action)) {
            handleSearchCustomer(request, response);
            return;
        }

        // API: Lấy chi tiết đơn hàng
        if ("order-detail".equals(action)) {
            handleOrderDetail(request, response);
            return;
        }

        // API: Lấy danh sách đơn hàng hôm nay (cho realtime polling)
        if ("orders".equals(action)) {
            handleGetOrders(request, response);
            return;
        }

        // Mặc định: Hiển thị giao diện POS
        List<Category> categories = categoryDAO.getAllActiveCategoriesWithCount();
        List<Product> products = productDAO.filterProducts(null, null);
        List<Order> counterOrders = orderDAO.getCounterOrdersToday();

        request.setAttribute("categories", categories);
        request.setAttribute("products", products);
        request.setAttribute("counterOrders", counterOrders);
        request.setAttribute("shopSettings", shopSettingDAO.getSettings());
        request.setAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        request.setAttribute("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        request.getRequestDispatcher("/cashier-dashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if (!isAuthorized(request, response)) {
            return;
        }

        String action = request.getParameter("action");

        if ("create-order".equals(action)) {
            handleCreateOrder(request, response);
        } else if ("cancel-order".equals(action)) {
            handleCancelOrder(request, response);
        } else if ("complete-order".equals(action)) {
            handleCompleteOrder(request, response);
        } else {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"success\":false,\"error\":\"Hành động không hợp lệ.\"}");
        }
    }

    private void handleSearchCustomer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String phone = request.getParameter("phone");
        if (phone == null || phone.trim().isEmpty()) {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"success\":false,\"error\":\"Vui lòng nhập số điện thoại.\"}");
            return;
        }

        User customer = userDAO.findCustomerByPhone(phone.trim());
        if (customer != null) {
            String json = String.format(
                    "{\"success\":true,\"customer\":{\"id\":%d,\"fullName\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\",\"totalPoints\":%d}}",
                    customer.getId(),
                    escapeJson(customer.getFullName()),
                    escapeJson(customer.getPhone() != null ? customer.getPhone() : ""),
                    escapeJson(customer.getEmail() != null ? customer.getEmail() : ""),
                    customer.getTotalPoints()
            );
            sendJson(response, HttpServletResponse.SC_OK, json);
        } else {
            sendJson(response, HttpServletResponse.SC_NOT_FOUND, "{\"success\":false,\"error\":\"Không tìm thấy thông tin hội viên.\"}");
        }
    }

    private void handleOrderDetail(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int orderId = parseInt(request.getParameter("orderId"), -1);
        if (orderId <= 0) {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"success\":false,\"error\":\"Mã đơn không hợp lệ.\"}");
            return;
        }

        Order order = orderDAO.getOrderById(orderId);
        if (order == null) {
            sendJson(response, HttpServletResponse.SC_NOT_FOUND, "{\"success\":false,\"error\":\"Không tìm thấy đơn hàng.\"}");
            return;
        }

        StringBuilder itemsJson = new StringBuilder("[");
        for (int i = 0; i < order.getItems().size(); i++) {
            OrderDetail item = order.getItems().get(i);
            if (i > 0) itemsJson.append(",");
            itemsJson.append(String.format(
                    "{\"id\":%d,\"productName\":\"%s\",\"quantity\":%d,\"unitPrice\":%s,\"subtotal\":%s,\"customization\":\"%s\"}",
                    item.getId(),
                    escapeJson(item.getProductName()),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal(),
                    escapeJson(item.getCustomization() != null ? item.getCustomization() : "")
            ));
        }
        itemsJson.append("]");

        String json = String.format(
                "{\"success\":true,\"order\":{\"id\":%d,\"orderCode\":\"%s\",\"orderType\":\"%s\",\"status\":\"%s\",\"statusLabel\":\"%s\",\"tableNumber\":\"%s\",\"recipientName\":\"%s\",\"recipientPhone\":\"%s\",\"totalAmount\":%s,\"discountAmount\":%s,\"finalAmount\":%s,\"createdAt\":\"%s\",\"paymentMethod\":\"%s\",\"items\":%s}}",
                order.getId(),
                order.getOrderCode(),
                order.getOrderType(),
                order.getStatus(),
                escapeJson(order.getStatusLabel()),
                escapeJson(order.getTableNumber() != null ? order.getTableNumber() : ""),
                escapeJson(order.getRecipientName() != null ? order.getRecipientName() : ""),
                escapeJson(order.getRecipientPhone() != null ? order.getRecipientPhone() : ""),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : "",
                escapeJson(order.getPaymentMethodLabel()),
                itemsJson.toString()
        );

        sendJson(response, HttpServletResponse.SC_OK, json);
    }

    private void handleCreateOrder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User cashier = (User) request.getSession().getAttribute("user");

        // Đọc JSON payload từ request body nếu có, hoặc đọc parameter
        String rawBody = readRequestBody(request);
        
        String orderType = "DINE_IN";
        String tableNumber = "";
        Integer customerId = null;
        String recipientName = "Khách tại quầy";
        String recipientPhone = "";
        String orderNote = "";
        String paymentMethod = "CASH";
        int pointsUsed = 0;
        double discountAmount = 0.0;
        double finalAmount = 0.0;
        double totalAmount = 0.0;
        List<OrderDetail> items = new ArrayList<>();

        if (rawBody != null && !rawBody.trim().isEmpty() && rawBody.startsWith("{")) {
            // Parse thủ công từ JSON payload (nhẹ và an toàn)
            orderType = extractJsonString(rawBody, "orderType", "DINE_IN");
            tableNumber = extractJsonString(rawBody, "tableNumber", "");
            int cId = extractJsonInt(rawBody, "customerId", -1);
            if (cId > 0) customerId = cId;
            recipientName = extractJsonString(rawBody, "recipientName", "Khách tại quầy");
            recipientPhone = extractJsonString(rawBody, "recipientPhone", "");
            orderNote = extractJsonString(rawBody, "orderNote", "");
            paymentMethod = extractJsonString(rawBody, "paymentMethod", "CASH");
            pointsUsed = extractJsonInt(rawBody, "pointsUsed", 0);
            discountAmount = extractJsonDouble(rawBody, "discountAmount", 0.0);
            totalAmount = extractJsonDouble(rawBody, "totalAmount", 0.0);
            finalAmount = extractJsonDouble(rawBody, "finalAmount", 0.0);

            items = parseOrderItemsFromJson(rawBody);
        } else {
            orderType = request.getParameter("orderType");
            if (orderType == null || orderType.isEmpty()) orderType = "DINE_IN";
            tableNumber = request.getParameter("tableNumber");
            int cId = parseInt(request.getParameter("customerId"), -1);
            if (cId > 0) customerId = cId;
            recipientName = request.getParameter("recipientName");
            recipientPhone = request.getParameter("recipientPhone");
            orderNote = request.getParameter("orderNote");
            paymentMethod = request.getParameter("paymentMethod");
            pointsUsed = parseInt(request.getParameter("pointsUsed"), 0);
            discountAmount = parseDouble(request.getParameter("discountAmount"), 0.0);
            totalAmount = parseDouble(request.getParameter("totalAmount"), 0.0);
            finalAmount = parseDouble(request.getParameter("finalAmount"), 0.0);
        }

        if (items.isEmpty()) {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"success\":false,\"error\":\"Vui lòng chọn ít nhất 1 món ăn/thức uống.\"}");
            return;
        }

        try {
            items = normalizeCounterItems(items);
        } catch (IllegalArgumentException e) {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            return;
        }
        ShopSettings settings = shopSettingDAO.getSettings();
        if (customerId != null) {
            User customer = userDAO.findCustomerByPhone(recipientPhone);
            if (customer == null || customer.getId() != customerId) {
                sendJson(response, HttpServletResponse.SC_BAD_REQUEST,
                        "{\"success\":false,\"error\":\"Khách hàng không hợp lệ hoặc đã bị khóa.\"}");
                return;
            }
            double serverSubtotal = items.stream().map(OrderDetail::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
            int max = settings.getMaxRedeemablePoints(customer.getTotalPoints(), serverSubtotal);
            pointsUsed = Math.max(0, Math.min(pointsUsed, max));
            pointsUsed = (pointsUsed / settings.getPointsStep()) * settings.getPointsStep();
        } else {
            pointsUsed = 0;
        }
        totalAmount = items.stream().map(OrderDetail::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
        discountAmount = settings.getPointsDiscount(pointsUsed);
        finalAmount = Math.max(0, totalAmount - discountAmount);
        paymentMethod = "BANK_TRANSFER".equalsIgnoreCase(paymentMethod) ? "BANK_TRANSFER" : "CASH";

        // Tạo mã đơn hàng độc nhất dạng ORD-YYMMDD-XXXX
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String timeSuffix = String.format("%04d", (int) (System.currentTimeMillis() % 10000));
        String orderCode = "ORD-" + datePrefix + "-" + timeSuffix;

        Order order = new Order();
        order.setOrderCode(orderCode);
        order.setCustomerId(customerId);
        order.setCashierId(cashier.getId());
        order.setOrderType("TAKEAWAY".equalsIgnoreCase(orderType) ? "TAKEAWAY" : "DINE_IN");
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setDeliveryFee(0);
        order.setFinalAmount(finalAmount);
        order.setRecipientName(recipientName != null && !recipientName.isEmpty() ? recipientName : "Khách tại quầy");
        order.setRecipientPhone(recipientPhone != null ? recipientPhone : "");
        order.setTableNumber(tableNumber != null && !tableNumber.trim().isEmpty() ? tableNumber.trim() : null);
        order.setOrderNote(orderNote);

        String paymentNote = "Thanh toán tại quầy qua " + ("BANK_TRANSFER".equals(paymentMethod) ? "VietQR" : "Tiền mặt");

        int orderId = orderDAO.createCounterOrder(order, items, paymentMethod, pointsUsed, paymentNote, settings);

        if (orderId > 0 && "BANK_TRANSFER".equals(paymentMethod) && settings.isPaymentTestMode()) {
            String amount = BigDecimal.valueOf(finalAmount).stripTrailingZeros().toPlainString();
            String txCode = "QR-" + orderCode;
            String status = "SUCCESS";
            String canonical = PaymentService.canonical(orderCode, txCode, amount, status);
            String signature = PaymentService.sign(settings.getPaymentCallbackSecret(), canonical);
            if (PaymentService.verify(settings.getPaymentCallbackSecret(), canonical, signature)) {
                paymentDAO.applyGatewayCallback(orderCode, txCode, new BigDecimal(amount), status,
                        "POS-LOCAL-" + System.currentTimeMillis(), "{\"source\":\"counter-simulator\"}",
                        settings.getPointsEarnUnit());
            }
        }

        if (orderId > 0) {
            String json = String.format("{\"success\":true,\"orderId\":%d,\"orderCode\":\"%s\",\"message\":\"Tạo đơn tại quầy thành công!\"}", orderId, orderCode);
            sendJson(response, HttpServletResponse.SC_OK, json);
        } else {
            sendJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "{\"success\":false,\"error\":\"Không thể tạo đơn hàng. Vui lòng thử lại.\"}");
        }
    }

    private void handleGetOrders(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<Order> orders = orderDAO.getCounterOrdersToday();
        StringBuilder sb = new StringBuilder("{\"success\":true,\"todayCount\":").append(orders.size()).append(",\"orders\":[");
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"id\":").append(o.getId()).append(",")
              .append("\"orderCode\":\"").append(escapeJson(o.getOrderCode())).append("\",")
              .append("\"createdTimeText\":\"").append(escapeJson(o.getCreatedTimeText())).append("\",")
              .append("\"orderType\":\"").append(escapeJson(o.getOrderType())).append("\",")
              .append("\"status\":\"").append(escapeJson(o.getStatus())).append("\",")
              .append("\"statusLabel\":\"").append(escapeJson(o.getStatusLabel())).append("\",")
              .append("\"recipientName\":\"").append(escapeJson(o.getRecipientName() != null ? o.getRecipientName() : "Khách tại quầy")).append("\",")
              .append("\"recipientPhone\":\"").append(escapeJson(o.getRecipientPhone() != null ? o.getRecipientPhone() : "")).append("\",")
              .append("\"deliveryAddress\":\"").append(escapeJson(o.getDeliveryAddress() != null ? o.getDeliveryAddress() : "")).append("\",")
              .append("\"tableNumber\":\"").append(escapeJson(o.getTableNumber() != null ? o.getTableNumber() : "")).append("\",")
              .append("\"itemSummary\":\"").append(escapeJson(o.getItemSummary())).append("\",")
              .append("\"itemCount\":").append(o.getItemCount()).append(",")
              .append("\"totalAmount\":").append(o.getTotalAmount()).append(",")
              .append("\"finalAmount\":").append(o.getFinalAmount()).append(",")
              .append("\"formattedFinalAmount\":\"").append(escapeJson(o.getFormattedFinalAmount())).append("\",")
              .append("\"paymentMethodLabel\":\"").append(escapeJson(o.getPaymentMethodLabel())).append("\",")
              .append("\"orderNote\":\"").append(escapeJson(o.getOrderNote() != null ? o.getOrderNote() : ""))
              .append("\"}");
        }
        sb.append("]}");
        sendJson(response, HttpServletResponse.SC_OK, sb.toString());
    }

    private void handleCancelOrder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int orderId = parseInt(request.getParameter("orderId"), -1);
        if (orderId <= 0) {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"success\":false,\"error\":\"Mã đơn không hợp lệ.\"}");
            return;
        }

        String errorMsg = orderDAO.cancelCounterOrderWithReason(orderId);
        if (errorMsg == null) {
            sendJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"message\":\"Đã hủy đơn hàng thành công.\"}");
        } else {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"success\":false,\"error\":\"" + escapeJson(errorMsg) + "\"}");
        }
    }

    private void handleDeliverOrder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int orderId = parseInt(request.getParameter("orderId"), -1);
        if (orderId <= 0) {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"success\":false,\"error\":\"Mã đơn không hợp lệ.\"}");
            return;
        }

        boolean success = orderDAO.deliverCounterOrder(orderId);
        if (success) {
            sendJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"message\":\"Đã chuyển đơn sang trạng thái Đang giao hàng.\"}");
        } else {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"success\":false,\"error\":\"Không thể chuyển trạng thái giao hàng (chỉ áp dụng cho đơn Online đã pha chế xong).\"}");
        }
    }

    private void handleCompleteOrder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int orderId = parseInt(request.getParameter("orderId"), -1);
        if (orderId <= 0) {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"success\":false,\"error\":\"Mã đơn không hợp lệ.\"}");
            return;
        }

        boolean success = orderDAO.completeCounterOrder(orderId);
        if (success) {
            sendJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"message\":\"Đã đánh dấu hoàn tất đơn hàng và giao khách thành công.\"}");
        } else {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"success\":false,\"error\":\"Không thể hoàn tất đơn hàng này (đơn phải được Barista pha chế xong hoặc đang giao).\"}");
        }
    }

    // Helper functions
    private void sendJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }

    private String readRequestBody(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    private String extractJsonString(String json, String key, String defaultValue) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return defaultValue;
    }

    private int extractJsonInt(String json, String key, int defaultValue) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try { return Integer.parseInt(matcher.group(1)); } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    private double extractJsonDouble(String json, String key, double defaultValue) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*([0-9.]+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try { return Double.parseDouble(matcher.group(1)); } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    private List<OrderDetail> parseOrderItemsFromJson(String json) {
        List<OrderDetail> items = new ArrayList<>();
        // Tìm mảng "items": [...]
        int itemsIndex = json.indexOf("\"items\"");
        if (itemsIndex == -1) return items;

        int startBracket = json.indexOf("[", itemsIndex);
        int endBracket = json.lastIndexOf("]");
        if (startBracket == -1 || endBracket == -1 || startBracket >= endBracket) return items;

        String itemsArrayStr = json.substring(startBracket + 1, endBracket);
        // Tách các object {...}
        Matcher m = Pattern.compile("\\{([^}]+)\\}").matcher(itemsArrayStr);
        while (m.find()) {
            String obj = m.group(1);
            int productId = extractJsonInt(obj, "productId", 0);
            String productName = extractJsonString(obj, "productName", "Sản phẩm");
            int quantity = extractJsonInt(obj, "quantity", 1);
            double unitPrice = extractJsonDouble(obj, "unitPrice", 0.0);
            double subtotal = extractJsonDouble(obj, "subtotal", unitPrice * quantity);
            String customization = extractJsonString(obj, "customization", "");

            if (productId > 0 && quantity > 0) {
                OrderDetail item = new OrderDetail();
                item.setProductId(productId);
                item.setProductName(productName);
                item.setQuantity(quantity);
                item.setUnitPrice(BigDecimal.valueOf(unitPrice));
                item.setSubtotal(BigDecimal.valueOf(subtotal));
                item.setCustomization(customization);
                items.add(item);
            }
        }
        return items;
    }

    private List<OrderDetail> normalizeCounterItems(List<OrderDetail> submitted) {
        List<OrderDetail> normalized = new ArrayList<>();
        for (OrderDetail raw : submitted) {
            String size = null;
            List<String> toppings = new ArrayList<>();
            String customization = raw.getCustomization() == null ? "" : raw.getCustomization();
            for (String part : customization.split("\\|")) {
                String value = part.trim();
                if (value.startsWith("Size ")) size = value.substring(5).trim();
                if (value.startsWith("+ ")) {
                    for (String topping : value.substring(2).split(",")) {
                        if (!topping.isBlank()) toppings.add(topping.trim());
                    }
                }
            }
            CartItem priced = cartService.createItem(raw.getProductId(), size, toppings, raw.getQuantity());
            if (priced == null) throw new IllegalArgumentException("Có món không tồn tại hoặc đã ngừng bán.");
            OrderDetail item = new OrderDetail();
            item.setProductId(priced.getProductId());
            item.setProductName(priced.getProductName());
            item.setQuantity(priced.getQuantity());
            item.setUnitPrice(BigDecimal.valueOf(priced.getUnitPrice()));
            item.setSubtotal(BigDecimal.valueOf(priced.getSubtotal()));
            item.setCustomization(customization);
            normalized.add(item);
        }
        return normalized;
    }

    private int parseInt(String value, int defaultValue) {
        try { return Integer.parseInt(value.trim()); } catch (Exception e) { return defaultValue; }
    }

    private double parseDouble(String value, double defaultValue) {
        try { return Double.parseDouble(value.trim()); } catch (Exception e) { return defaultValue; }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
