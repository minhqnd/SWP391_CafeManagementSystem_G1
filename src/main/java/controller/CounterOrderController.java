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

    private boolean isAuthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            String accept = request.getHeader("Accept");
            if (accept != null && accept.contains("application/json")) {
                sendJson(response, HttpServletResponse.SC_UNAUTHORIZED, "{\"success\":false,\"error\":\"Vui lòng đăng nhập.\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/?login=true");
            }
            return false;
        }

        String role = user.getRole();
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()) || (!"STAFF".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role))) {
            sendJson(response, HttpServletResponse.SC_FORBIDDEN, "{\"success\":false,\"error\":\"Chỉ nhân viên hoặc quản trị viên được phép truy cập POS.\"}");
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

        // Mặc định: Hiển thị giao diện POS
        List<Category> categories = categoryDAO.getAllActiveCategoriesWithCount();
        List<Product> products = productDAO.filterProducts(null, null);
        List<Order> counterOrders = orderDAO.getCounterOrdersToday();

        request.setAttribute("categories", categories);
        request.setAttribute("products", products);
        request.setAttribute("counterOrders", counterOrders);
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
                    "{\"success\":true,\"customer\":{\"id\":%d,\"fullName\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\",\"tierName\":\"%s\",\"totalPoints\":%d}}",
                    customer.getId(),
                    escapeJson(customer.getFullName()),
                    escapeJson(customer.getPhone() != null ? customer.getPhone() : ""),
                    escapeJson(customer.getEmail() != null ? customer.getEmail() : ""),
                    escapeJson(customer.getTierName() != null ? customer.getTierName() : "MEMBER"),
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
        order.setTableNumber("DINE_IN".equalsIgnoreCase(order.getOrderType()) ? tableNumber : null);
        order.setOrderNote(orderNote);

        String paymentNote = "Thanh toán tại quầy qua " + ("BANK_TRANSFER".equals(paymentMethod) ? "VietQR" : "Tiền mặt");

        int orderId = orderDAO.createCounterOrder(order, items, paymentMethod, pointsUsed, paymentNote);

        if (orderId > 0) {
            String json = String.format("{\"success\":true,\"orderId\":%d,\"orderCode\":\"%s\",\"message\":\"Tạo đơn tại quầy thành công!\"}", orderId, orderCode);
            sendJson(response, HttpServletResponse.SC_OK, json);
        } else {
            sendJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "{\"success\":false,\"error\":\"Không thể tạo đơn hàng. Vui lòng thử lại.\"}");
        }
    }

    private void handleCancelOrder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int orderId = parseInt(request.getParameter("orderId"), -1);
        if (orderId <= 0) {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"success\":false,\"error\":\"Mã đơn không hợp lệ.\"}");
            return;
        }

        boolean success = orderDAO.cancelCounterOrder(orderId);
        if (success) {
            sendJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"message\":\"Đã hủy đơn hàng thành công.\"}");
        } else {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"success\":false,\"error\":\"Không thể hủy đơn hàng này (đơn đã hoàn thành hoặc không tồn tại).\"}");
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
