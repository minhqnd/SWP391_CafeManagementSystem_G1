package controller;

import dao.CategoryDAO;
import dao.OrderDAO;
import dao.PaymentDAO;
import dao.ProductDAO;
import dao.ShopSettingDAO;
import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.CartItem;
import model.Category;
import model.Order;
import model.OrderDetail;
import model.Product;
import model.ShopSettings;
import model.User;
import service.CartService;
import service.PaymentService;
import utils.SimpleJsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CounterOrderController - Màn hình bán hàng tại quầy dành cho Thu ngân (POS)
 * URLs: /cashier, /counter-order
 */
@WebServlet(name = "CounterOrderController", urlPatterns = {"/cashier", "/counter-order"})
public class CounterOrderController extends HttpServlet {

    // Giới hạn kiểm tra dữ liệu đầu vào cho đơn tại quầy
    static final int MAX_ITEM_LINES = 50;
    static final int MAX_ITEM_QUANTITY = 99;
    static final int MAX_TOTAL_QUANTITY = 200;
    static final int MAX_ORDER_NOTE_LENGTH = 500;
    static final int MAX_CUSTOMIZATION_LENGTH = 300;
    static final int MAX_TABLE_LENGTH = 20;
    static final int MAX_PHONE_LENGTH = 20;
    static final double MAX_ORDER_AMOUNT = 9_999_999_999.99;

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final CartService cartService = new CartService();

    // =========================================================================
    // 1. Kiểm tra đăng nhập & quyền Thu ngân (CASHIER)
    // =========================================================================

    private boolean isAuthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = session != null ? (User) session.getAttribute("user") : null;

        String acceptHeader = request.getHeader("Accept");
        String contentType = request.getContentType();
        boolean isApiRequest = (acceptHeader != null && acceptHeader.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"))
                || request.getParameter("action") != null;

        if (currentUser == null) {
            if (isApiRequest) {
                sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return false;
        }

        boolean isActiveCashier = "ACTIVE".equalsIgnoreCase(currentUser.getStatus())
                && "STAFF".equalsIgnoreCase(currentUser.getRole())
                && "CASHIER".equalsIgnoreCase(currentUser.getPosition());

        if (!isActiveCashier) {
            if (isApiRequest) {
                sendJsonError(response, HttpServletResponse.SC_FORBIDDEN,
                        "Chỉ nhân viên hoặc quản trị viên được phép truy cập POS.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login?error=unauthorized");
            }
            return false;
        }

        return true;
    }

    // =========================================================================
    // 2. Điều hướng GET / POST
    // =========================================================================

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAuthorized(request, response)) {
            return;
        }

        String action = request.getParameter("action");

        if ("search-customer".equals(action)) {
            handleSearchCustomer(request, response);
            return;
        }
        if ("order-detail".equals(action)) {
            handleOrderDetail(request, response);
            return;
        }
        if ("orders".equals(action)) {
            handleGetOrders(request, response);
            return;
        }
        if ("check-payment".equals(action)) {
            handleCheckPayment(request, response);
            return;
        }

        // Mặc định: tải danh mục, sản phẩm và đơn hôm nay rồi mở giao diện POS
        User cashierUser = (User) request.getSession().getAttribute("user");
        List<Category> categoryList = categoryDAO.getAllActiveCategoriesWithCount();
        List<Product> productList = productDAO.filterProducts(null, null);
        List<Order> todayOrders = orderDAO.getCounterOrdersToday(cashierUser.getId());

        request.setAttribute("categories", categoryList);
        request.setAttribute("products", productList);
        request.setAttribute("counterOrders", todayOrders);
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
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Hành động không hợp lệ.");
        }
    }

    // =========================================================================
    // 3. Các chức năng API cho Thu ngân
    // =========================================================================

    /** Tìm khách hàng hội viên theo số điện thoại để tích điểm / đổi điểm. */
    private void handleSearchCustomer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String phoneInput = request.getParameter("phone");
        String phoneError = validateSearchPhone(phoneInput);
        if (phoneError != null) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, phoneError);
            return;
        }

        User customer = userDAO.findCustomerByPhone(phoneInput.trim());
        if (customer == null) {
            sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy thông tin hội viên.");
            return;
        }

        String phoneText = customer.getPhone() != null ? customer.getPhone() : "";
        String emailText = customer.getEmail() != null ? customer.getEmail() : "";

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"success\":true,\"customer\":{");
        jsonBuilder.append("\"id\":").append(customer.getId()).append(",");
        jsonBuilder.append("\"fullName\":\"").append(SimpleJsonParser.escape(customer.getFullName())).append("\",");
        jsonBuilder.append("\"phone\":\"").append(SimpleJsonParser.escape(phoneText)).append("\",");
        jsonBuilder.append("\"email\":\"").append(SimpleJsonParser.escape(emailText)).append("\",");
        jsonBuilder.append("\"totalPoints\":").append(customer.getTotalPoints());
        jsonBuilder.append("}}");

        sendJson(response, HttpServletResponse.SC_OK, jsonBuilder.toString());
    }

    /** Lấy chi tiết 1 đơn tại quầy (chỉ xem được đơn do chính thu ngân đang đăng nhập tạo). */
    private void handleOrderDetail(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int orderId = parseIntOrDefault(request.getParameter("orderId"), -1);
        if (orderId <= 0) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Mã đơn không hợp lệ.");
            return;
        }

        Order order = orderDAO.getOrderById(orderId);
        if (order == null || !isOwnedCounterOrder(request, order)) {
            sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy đơn hàng.");
            return;
        }

        StringBuilder itemsBuilder = new StringBuilder("[");
        List<OrderDetail> orderItems = order.getItems();
        for (int i = 0; i < orderItems.size(); i++) {
            OrderDetail item = orderItems.get(i);
            if (i > 0) {
                itemsBuilder.append(",");
            }
            String customizationText = item.getCustomization() != null ? item.getCustomization() : "";
            itemsBuilder.append("{");
            itemsBuilder.append("\"id\":").append(item.getId()).append(",");
            itemsBuilder.append("\"productName\":\"").append(SimpleJsonParser.escape(item.getProductName())).append("\",");
            itemsBuilder.append("\"quantity\":").append(item.getQuantity()).append(",");
            itemsBuilder.append("\"unitPrice\":").append(item.getUnitPrice()).append(",");
            itemsBuilder.append("\"subtotal\":").append(item.getSubtotal()).append(",");
            itemsBuilder.append("\"customization\":\"").append(SimpleJsonParser.escape(customizationText)).append("\"");
            itemsBuilder.append("}");
        }
        itemsBuilder.append("]");

        String tableText = order.getTableNumber() != null ? order.getTableNumber() : "";
        String recipientNameText = order.getRecipientName() != null ? order.getRecipientName() : "";
        String recipientPhoneText = order.getRecipientPhone() != null ? order.getRecipientPhone() : "";
        String createdAtText = order.getCreatedAt() != null ? order.getCreatedAt().toString() : "";

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"success\":true,\"order\":{");
        jsonBuilder.append("\"id\":").append(order.getId()).append(",");
        jsonBuilder.append("\"orderCode\":\"").append(SimpleJsonParser.escape(order.getOrderCode())).append("\",");
        jsonBuilder.append("\"orderType\":\"").append(SimpleJsonParser.escape(order.getOrderType())).append("\",");
        jsonBuilder.append("\"status\":\"").append(SimpleJsonParser.escape(order.getStatus())).append("\",");
        jsonBuilder.append("\"statusLabel\":\"").append(SimpleJsonParser.escape(order.getStatusLabel())).append("\",");
        jsonBuilder.append("\"tableNumber\":\"").append(SimpleJsonParser.escape(tableText)).append("\",");
        jsonBuilder.append("\"recipientName\":\"").append(SimpleJsonParser.escape(recipientNameText)).append("\",");
        jsonBuilder.append("\"recipientPhone\":\"").append(SimpleJsonParser.escape(recipientPhoneText)).append("\",");
        jsonBuilder.append("\"totalAmount\":").append(order.getTotalAmount()).append(",");
        jsonBuilder.append("\"discountAmount\":").append(order.getDiscountAmount()).append(",");
        jsonBuilder.append("\"finalAmount\":").append(order.getFinalAmount()).append(",");
        jsonBuilder.append("\"createdAt\":\"").append(SimpleJsonParser.escape(createdAtText)).append("\",");
        jsonBuilder.append("\"paymentMethod\":\"").append(SimpleJsonParser.escape(order.getPaymentMethodLabel())).append("\",");
        jsonBuilder.append("\"items\":").append(itemsBuilder);
        jsonBuilder.append("}}");

        sendJson(response, HttpServletResponse.SC_OK, jsonBuilder.toString());
    }

    /** Tạo đơn hàng mới tại quầy (tiền mặt hoặc chuyển khoản VietQR). */
    private void handleCreateOrder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User cashierUser = (User) request.getSession().getAttribute("user");

        // Bước 1: Đọc dữ liệu đơn hàng từ JSON body (POS gửi fetch) hoặc form parameter
        String rawBody = readRequestBody(request);
        String orderType = "DINE_IN";
        String tableNumber = "";
        Integer customerId = null;
        String recipientName = "Khách tại quầy";
        String recipientPhone = "";
        String orderNote = "";
        String paymentMethod = "CASH";
        int pointsUsed = 0;
        double cashGiven = Double.NaN;
        List<OrderDetail> selectedItems = new ArrayList<>();

        if (rawBody != null && !rawBody.trim().isEmpty() && rawBody.trim().startsWith("{")) {
            orderType = SimpleJsonParser.getString(rawBody, "orderType", "DINE_IN");
            tableNumber = SimpleJsonParser.getString(rawBody, "tableNumber", "");
            int parsedCustomerId = SimpleJsonParser.getInt(rawBody, "customerId", -1);
            if (parsedCustomerId > 0) {
                customerId = parsedCustomerId;
            }
            recipientName = SimpleJsonParser.getString(rawBody, "recipientName", "Khách tại quầy");
            recipientPhone = SimpleJsonParser.getString(rawBody, "recipientPhone", "");
            orderNote = SimpleJsonParser.getString(rawBody, "orderNote", "");
            paymentMethod = SimpleJsonParser.getString(rawBody, "paymentMethod", "CASH");
            pointsUsed = SimpleJsonParser.getInt(rawBody, "pointsUsed", 0);
            cashGiven = SimpleJsonParser.getDouble(rawBody, "cashGiven", Double.NaN);
            selectedItems = parseOrderItemsFromJson(rawBody);
        } else {
            String paramOrderType = request.getParameter("orderType");
            if (paramOrderType != null && !paramOrderType.isEmpty()) {
                orderType = paramOrderType;
            }
            tableNumber = request.getParameter("tableNumber");
            int parsedCustomerId = parseIntOrDefault(request.getParameter("customerId"), -1);
            if (parsedCustomerId > 0) {
                customerId = parsedCustomerId;
            }
            recipientName = request.getParameter("recipientName");
            recipientPhone = request.getParameter("recipientPhone");
            orderNote = request.getParameter("orderNote");
            paymentMethod = request.getParameter("paymentMethod");
            pointsUsed = parseIntOrDefault(request.getParameter("pointsUsed"), 0);

            String rawCashGiven = request.getParameter("cashGiven");
            if (rawCashGiven != null && !rawCashGiven.isBlank()) {
                cashGiven = parseDoubleOrDefault(rawCashGiven, -1.0);
            }
        }

        // Bước 2: Kiểm tra hợp lệ ghi chú, số bàn và danh sách món
        String validationError = validateOrderNote(orderNote);
        if (validationError == null) {
            validationError = validateTableNumber(tableNumber);
        }
        if (validationError == null) {
            validationError = validateItemQuantities(selectedItems);
        }
        if (validationError != null) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, validationError);
            return;
        }

        // Bước 3: Tra lại giá gốc/size/topping từ DB để tránh client sửa giá ở F12
        List<OrderDetail> normalizedItems;
        try {
            normalizedItems = normalizeCounterItems(selectedItems);
        } catch (IllegalArgumentException e) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        double totalAmount = calculateSubtotal(normalizedItems);
        String totalError = validateOrderTotal(totalAmount);
        if (totalError != null) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, totalError);
            return;
        }

        ShopSettings shopSettings = shopSettingDAO.getSettings();
        if (customerId != null) {
            User customer = userDAO.findCustomerByPhone(recipientPhone);
            if (customer == null || customer.getId() != customerId) {
                sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Khách hàng không hợp lệ hoặc đã bị khóa.");
                return;
            }
            int maxPoints = shopSettings.getMaxRedeemablePoints(customer.getTotalPoints(), totalAmount);
            pointsUsed = Math.max(0, Math.min(pointsUsed, maxPoints));
            pointsUsed = (pointsUsed / shopSettings.getPointsStep()) * shopSettings.getPointsStep();
        } else {
            pointsUsed = 0;
        }

        double discountAmount = shopSettings.getPointsDiscount(pointsUsed);
        double finalAmount = Math.max(0, totalAmount - discountAmount);
        String normalizedPayMethod = "BANK_TRANSFER".equalsIgnoreCase(paymentMethod) ? "BANK_TRANSFER" : "CASH";

        if ("CASH".equals(normalizedPayMethod) && !Double.isNaN(cashGiven)) {
            if (Double.isInfinite(cashGiven) || cashGiven < 0 || cashGiven > MAX_ORDER_AMOUNT) {
                sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Số tiền khách đưa vượt quá giới hạn cho phép.");
                return;
            }
            if (cashGiven + 0.001 < finalAmount) {
                sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Số tiền khách đưa chưa đủ để thanh toán đơn hàng.");
                return;
            }
        }

        // Bước 4: Đóng gói đối tượng Order và lưu xuống DB (có kiểm tra đủ nguyên liệu tồn kho)
        String orderCode = CheckoutController.newOrderCode();
        Order newOrder = new Order();
        newOrder.setOrderCode(orderCode);
        newOrder.setCustomerId(customerId);
        newOrder.setCashierId(cashierUser.getId());
        newOrder.setOrderType("TAKEAWAY".equalsIgnoreCase(orderType) ? "TAKEAWAY" : "DINE_IN");
        newOrder.setTotalAmount(totalAmount);
        newOrder.setDiscountAmount(discountAmount);
        newOrder.setDeliveryFee(0);
        newOrder.setFinalAmount(finalAmount);
        newOrder.setRecipientName(recipientName != null && !recipientName.isEmpty() ? recipientName : "Khách tại quầy");
        newOrder.setRecipientPhone(recipientPhone != null ? recipientPhone : "");
        newOrder.setTableNumber(tableNumber != null && !tableNumber.trim().isEmpty() ? tableNumber.trim() : null);
        newOrder.setOrderNote(orderNote != null ? orderNote.trim() : "");

        String paymentNote = "Thanh toán tại quầy qua "
                + ("BANK_TRANSFER".equals(normalizedPayMethod) ? "VietQR" : "Tiền mặt");

        int orderId = orderDAO.createCounterOrder(
                newOrder, normalizedItems, normalizedPayMethod, pointsUsed, paymentNote, shopSettings
        );

        if (orderId == OrderDAO.NOT_ENOUGH_STOCK) {
            String stockDetail = null;
            try {
                stockDetail = orderDAO.getCounterOrderStockError(normalizedItems);
            } catch (Exception ignored) {
            }
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                    counterCreateErrorMessage(orderId, stockDetail));
            return;
        }
        if (orderId <= 0) {
            sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    counterCreateErrorMessage(orderId, null));
            return;
        }

        // Bước 5: Nếu chọn chuyển khoản, đăng ký đơn lên Payment Gateway và tạo link ảnh VietQR
        String qrImageUrl = "";
        if ("BANK_TRANSFER".equals(normalizedPayMethod)) {
            PaymentService.registerOrderToGateway(
                    shopSettings.getPaymentGatewayUrl(),
                    "aura_cafe",
                    orderCode,
                    finalAmount,
                    "TX-" + orderCode
            );
            qrImageUrl = buildVietQrUrl(shopSettings, orderCode, finalAmount);
        }

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"success\":true,");
        jsonBuilder.append("\"orderId\":").append(orderId).append(",");
        jsonBuilder.append("\"orderCode\":\"").append(SimpleJsonParser.escape(orderCode)).append("\",");
        jsonBuilder.append("\"qrUrl\":\"").append(SimpleJsonParser.escape(qrImageUrl)).append("\",");
        jsonBuilder.append("\"finalAmount\":").append(Math.round(finalAmount)).append(",");
        jsonBuilder.append("\"message\":\"Tạo đơn tại quầy thành công!\"}");

        sendJson(response, HttpServletResponse.SC_OK, jsonBuilder.toString());
    }

    /** Kiểm tra xem khách đã chuyển khoản thành công qua VietQR hay chưa. */
    private void handleCheckPayment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int orderId = parseIntOrDefault(request.getParameter("orderId"), -1);
        if (orderId <= 0) {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"paid\":false,\"error\":\"Mã đơn không hợp lệ.\"}");
            return;
        }

        Order order = orderDAO.getOrderById(orderId);
        if (order == null || !isOwnedCounterOrder(request, order)) {
            sendJson(response, HttpServletResponse.SC_NOT_FOUND,
                    "{\"paid\":false,\"error\":\"Không tìm thấy đơn hàng.\"}");
            return;
        }

        if (!order.isPending()) {
            String json = "{\"paid\":" + isVerifiedQrPayment(order)
                    + ",\"status\":\"" + SimpleJsonParser.escape(order.getStatus()) + "\""
                    + ",\"orderCode\":\"" + SimpleJsonParser.escape(order.getOrderCode()) + "\"}";
            sendJson(response, HttpServletResponse.SC_OK, json);
            return;
        }

        // Đơn đang PENDING: hỏi Payment Gateway xem SePay webhook đã nhận được tiền chưa
        ShopSettings shopSettings = shopSettingDAO.getSettings();
        boolean isPaidOnGateway = PaymentService.isOrderPaidOnGateway(
                shopSettings.getPaymentGatewayUrl(), order.getOrderCode()
        );

        if (isPaidOnGateway) {
            String transactionCode = "QR-" + order.getOrderCode();
            BigDecimal paidAmount = BigDecimal.valueOf(Math.round(order.getFinalAmount()));
            String callbackPayload = "{\"orderCode\":\"" + SimpleJsonParser.escape(order.getOrderCode())
                    + "\",\"status\":\"SUCCESS\",\"source\":\"pos-polling\"}";

            PaymentDAO.CallbackResult result = paymentDAO.applyGatewayCallback(
                    order.getOrderCode(), transactionCode, paidAmount, "SUCCESS",
                    "SEPAY-POS-QR", callbackPayload, shopSettings.getPointsEarnUnit()
            );
            if (result.successful()) {
                String json = "{\"paid\":true,\"status\":\"CONFIRMED\",\"orderCode\":\""
                        + SimpleJsonParser.escape(order.getOrderCode()) + "\"}";
                sendJson(response, HttpServletResponse.SC_OK, json);
                return;
            }
        }

        String json = "{\"paid\":false,\"status\":\"PENDING\",\"orderCode\":\""
                + SimpleJsonParser.escape(order.getOrderCode()) + "\"}";
        sendJson(response, HttpServletResponse.SC_OK, json);
    }

    /** Lấy danh sách đơn tại quầy trong ngày hôm nay (dùng cho tự động làm mới danh sách). */
    private void handleGetOrders(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User cashierUser = (User) request.getSession().getAttribute("user");
        List<Order> todayOrders = orderDAO.getCounterOrdersToday(cashierUser.getId());

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"success\":true,\"todayCount\":").append(todayOrders.size()).append(",\"orders\":[");

        for (int i = 0; i < todayOrders.size(); i++) {
            Order order = todayOrders.get(i);
            if (i > 0) {
                jsonBuilder.append(",");
            }
            String recipientNameText = order.getRecipientName() != null ? order.getRecipientName() : "Khách tại quầy";
            String recipientPhoneText = order.getRecipientPhone() != null ? order.getRecipientPhone() : "";
            String addressText = order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "";
            String tableText = order.getTableNumber() != null ? order.getTableNumber() : "";
            String noteText = order.getOrderNote() != null ? order.getOrderNote() : "";

            jsonBuilder.append("{");
            jsonBuilder.append("\"id\":").append(order.getId()).append(",");
            jsonBuilder.append("\"orderCode\":\"").append(SimpleJsonParser.escape(order.getOrderCode())).append("\",");
            jsonBuilder.append("\"createdTimeText\":\"").append(SimpleJsonParser.escape(order.getCreatedTimeText())).append("\",");
            jsonBuilder.append("\"orderType\":\"").append(SimpleJsonParser.escape(order.getOrderType())).append("\",");
            jsonBuilder.append("\"status\":\"").append(SimpleJsonParser.escape(order.getStatus())).append("\",");
            jsonBuilder.append("\"statusLabel\":\"").append(SimpleJsonParser.escape(order.getStatusLabel())).append("\",");
            jsonBuilder.append("\"recipientName\":\"").append(SimpleJsonParser.escape(recipientNameText)).append("\",");
            jsonBuilder.append("\"recipientPhone\":\"").append(SimpleJsonParser.escape(recipientPhoneText)).append("\",");
            jsonBuilder.append("\"deliveryAddress\":\"").append(SimpleJsonParser.escape(addressText)).append("\",");
            jsonBuilder.append("\"tableNumber\":\"").append(SimpleJsonParser.escape(tableText)).append("\",");
            jsonBuilder.append("\"itemSummary\":\"").append(SimpleJsonParser.escape(order.getItemSummary())).append("\",");
            jsonBuilder.append("\"itemCount\":").append(order.getItemCount()).append(",");
            jsonBuilder.append("\"totalAmount\":").append(order.getTotalAmount()).append(",");
            jsonBuilder.append("\"finalAmount\":").append(order.getFinalAmount()).append(",");
            jsonBuilder.append("\"formattedFinalAmount\":\"").append(SimpleJsonParser.escape(order.getFormattedFinalAmount())).append("\",");
            jsonBuilder.append("\"paymentMethodLabel\":\"").append(SimpleJsonParser.escape(order.getPaymentMethodLabel())).append("\",");
            jsonBuilder.append("\"orderNote\":\"").append(SimpleJsonParser.escape(noteText)).append("\"");
            jsonBuilder.append("}");
        }

        jsonBuilder.append("]}");
        sendJson(response, HttpServletResponse.SC_OK, jsonBuilder.toString());
    }

    /** Hủy đơn tại quầy (chỉ hủy được khi đơn chưa pha chế xong). */
    private void handleCancelOrder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int orderId = parseIntOrDefault(request.getParameter("orderId"), -1);
        if (orderId <= 0) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Mã đơn không hợp lệ.");
            return;
        }

        Order order = orderDAO.getOrderById(orderId);
        if (order == null || !isOwnedCounterOrder(request, order)) {
            sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy đơn hàng.");
            return;
        }

        String errorMsg = orderDAO.cancelCounterOrderWithReason(orderId);
        if (errorMsg == null) {
            sendJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":true,\"message\":\"Đã hủy đơn hàng thành công.\"}");
        } else {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, errorMsg);
        }
    }

    /** Đánh dấu đơn đã giao cho khách sau khi Barista pha chế xong. */
    private void handleCompleteOrder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int orderId = parseIntOrDefault(request.getParameter("orderId"), -1);
        if (orderId <= 0) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Mã đơn không hợp lệ.");
            return;
        }

        Order order = orderDAO.getOrderById(orderId);
        if (order == null || !isOwnedCounterOrder(request, order)) {
            sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy đơn hàng.");
            return;
        }

        boolean completed = orderDAO.completeCounterOrder(orderId);
        if (completed) {
            sendJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":true,\"message\":\"Đã đánh dấu hoàn tất đơn hàng và giao khách thành công.\"}");
        } else {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Không thể hoàn tất đơn hàng này (đơn phải được Barista pha chế xong hoặc đang giao).");
        }
    }

    // =========================================================================
    // 4. Các hàm kiểm tra hợp lệ (Validation) & tính toán đơn giản
    // =========================================================================

    static boolean isVerifiedQrPayment(Order order) {
        return order.isPaid() && "BANK_TRANSFER".equals(order.getPaymentMethod()) && order.getPaidAt() != null;
    }

    // Kiểm tra số điện thoại tìm hội viên: bắt buộc, tối đa 20 ký tự, 8-15 chữ số.
    static String validateSearchPhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return "Vui lòng nhập số điện thoại khách hàng.";
        }
        String phone = rawPhone.trim();
        if (phone.length() > MAX_PHONE_LENGTH) {
            return "Số điện thoại không được vượt quá 20 ký tự.";
        }
        int digitCount = 0;
        for (int i = 0; i < phone.length(); i++) {
            char ch = phone.charAt(i);
            if (ch >= '0' && ch <= '9') {
                digitCount++;
            } else if (ch == '+' && i == 0) {
                // Cho phép dấu '+' ở đầu số quốc tế
            } else if (ch != ' ' && ch != '-' && ch != '.') {
                return "Số điện thoại không hợp lệ (chỉ gồm số, từ 8 đến 15 chữ số).";
            }
        }
        if (digitCount < 8 || digitCount > 15) {
            return "Số điện thoại không hợp lệ (chỉ gồm số, từ 8 đến 15 chữ số).";
        }
        return null;
    }

    // Kiểm tra ghi chú chung của đơn (tối đa 500 ký tự).
    static String validateOrderNote(String rawNote) {
        if (rawNote != null && rawNote.trim().length() > MAX_ORDER_NOTE_LENGTH) {
            return "Ghi chú đơn hàng không được vượt quá 500 ký tự.";
        }
        return null;
    }

    // Kiểm tra số bàn (nếu có, tối đa 20 ký tự).
    static String validateTableNumber(String rawTable) {
        if (rawTable != null && rawTable.trim().length() > MAX_TABLE_LENGTH) {
            return "Số bàn không được vượt quá 20 ký tự.";
        }
        return null;
    }

    // Kiểm tra tùy chọn / ghi chú từng món (tối đa 300 ký tự).
    static String validateCustomization(String rawCustomization) {
        if (rawCustomization != null && rawCustomization.trim().length() > MAX_CUSTOMIZATION_LENGTH) {
            return "Tùy chọn / ghi chú món không được vượt quá 300 ký tự.";
        }
        return null;
    }

    // Kiểm tra danh sách món: 1-50 dòng, mỗi món 1-99 ly, tổng toàn đơn <= 200 ly.
    static String validateItemQuantities(List<OrderDetail> items) {
        if (items == null || items.isEmpty()) {
            return "Vui lòng chọn ít nhất 1 món ăn/thức uống.";
        }
        if (items.size() > MAX_ITEM_LINES) {
            return "Đơn hàng không được vượt quá 50 dòng món.";
        }
        int totalQuantity = 0;
        for (OrderDetail item : items) {
            if (item.getQuantity() < 1 || item.getQuantity() > MAX_ITEM_QUANTITY) {
                return "Số lượng mỗi món phải từ 1 đến 99.";
            }
            String custError = validateCustomization(item.getCustomization());
            if (custError != null) {
                return custError;
            }
            totalQuantity += item.getQuantity();
            if (totalQuantity > MAX_TOTAL_QUANTITY) {
                return "Tổng số lượng món trong một đơn không được vượt quá 200.";
            }
        }
        return null;
    }

    // Kiểm tra tổng tiền đơn hàng nằm trong giới hạn lưu trữ DB.
    static String validateOrderTotal(double totalAmount) {
        if (Double.isNaN(totalAmount) || Double.isInfinite(totalAmount)
                || totalAmount < 0 || totalAmount > MAX_ORDER_AMOUNT) {
            return "Tổng tiền đơn hàng vượt quá giới hạn cho phép.";
        }
        return null;
    }

    // Trả thông báo lỗi phù hợp khi tạo đơn tại quầy thất bại (hết nguyên liệu hoặc lỗi hệ thống)
    static String counterCreateErrorMessage(int orderId, String stockDetail) {
        if (orderId == OrderDAO.NOT_ENOUGH_STOCK) {
            if (stockDetail != null && !stockDetail.isBlank()) {
                return stockDetail;
            }
            return "Có món trong đơn đã hết hoặc không đủ nguyên liệu trong kho. Vui lòng giảm số lượng hoặc chọn món khác.";
        }
        return "Không thể tạo đơn hàng. Vui lòng thử lại.";
    }

    // =========================================================================
    // 5. Các hàm đọc JSON & chuẩn hóa món (gọn nhẹ, dùng vòng lặp đơn giản)
    // =========================================================================

    static String extractJsonString(String json, String key, String defaultValue) {
        return SimpleJsonParser.getString(json, key, defaultValue);
    }

    static double extractJsonDouble(String json, String key, double defaultValue) {
        return SimpleJsonParser.getDouble(json, key, defaultValue);
    }

    static List<OrderDetail> parseOrderItemsFromJson(String json) {
        List<OrderDetail> itemList = new ArrayList<>();
        List<String> rawObjects = SimpleJsonParser.getArrayObjects(json, "items");

        for (String objText : rawObjects) {
            int productId = SimpleJsonParser.getInt(objText, "productId", 0);
            if (productId <= 0) {
                continue;
            }
            String productName = SimpleJsonParser.getString(objText, "productName", "Sản phẩm");
            int quantity = SimpleJsonParser.getInt(objText, "quantity", 0);
            double unitPrice = SimpleJsonParser.getDouble(objText, "unitPrice", 0.0);
            double subtotal = SimpleJsonParser.getDouble(objText, "subtotal", unitPrice * quantity);
            String customization = SimpleJsonParser.getString(objText, "customization", "");

            OrderDetail item = new OrderDetail();
            item.setProductId(productId);
            item.setProductName(productName);
            item.setQuantity(quantity);
            item.setUnitPrice(BigDecimal.valueOf(unitPrice));
            item.setSubtotal(BigDecimal.valueOf(subtotal));
            item.setCustomization(customization);
            itemList.add(item);
        }
        return itemList;
    }

    private List<OrderDetail> normalizeCounterItems(List<OrderDetail> submittedItems) {
        List<OrderDetail> normalizedList = new ArrayList<>();

        for (OrderDetail rawItem : submittedItems) {
            String selectedSize = null;
            List<String> selectedToppings = new ArrayList<>();
            String customization = rawItem.getCustomization() == null ? "" : rawItem.getCustomization();

            String[] parts = customization.split("\\|");
            for (String part : parts) {
                String text = part.trim();
                // Dừng tách size/topping khi tới phần ghi chú riêng để ghi chú không làm đổi giá
                if (text.startsWith("Lưu ý:")) {
                    break;
                }
                if (text.startsWith("Size ")) {
                    selectedSize = text.substring(5).trim();
                }
                if (text.startsWith("+ ")) {
                    String[] toppingNames = text.substring(2).split(",");
                    for (String topping : toppingNames) {
                        if (!topping.isBlank()) {
                            selectedToppings.add(topping.trim());
                        }
                    }
                }
            }

            CartItem pricedItem = cartService.createItem(
                    rawItem.getProductId(), selectedSize, selectedToppings, rawItem.getQuantity()
            );
            if (pricedItem == null) {
                throw new IllegalArgumentException("Có món không tồn tại hoặc đã ngừng bán.");
            }

            OrderDetail normalizedItem = new OrderDetail();
            normalizedItem.setProductId(pricedItem.getProductId());
            normalizedItem.setProductName(pricedItem.getProductName());
            normalizedItem.setQuantity(pricedItem.getQuantity());
            normalizedItem.setUnitPrice(BigDecimal.valueOf(pricedItem.getUnitPrice()));
            normalizedItem.setSubtotal(BigDecimal.valueOf(pricedItem.getSubtotal()));
            normalizedItem.setCustomization(customization);
            normalizedList.add(normalizedItem);
        }

        return normalizedList;
    }

    // Cộng tổng tiền hàng của tất cả các dòng món bằng vòng lặp for đơn giản
    private double calculateSubtotal(List<OrderDetail> itemList) {
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderDetail item : itemList) {
            if (item.getSubtotal() != null) {
                sum = sum.add(item.getSubtotal());
            }
        }
        return sum.doubleValue();
    }

    // =========================================================================
    // 6. Các hàm hỗ trợ chung (trả JSON, đọc body, tạo link VietQR)
    // =========================================================================

    private boolean isOwnedCounterOrder(HttpServletRequest request, Order order) {
        User cashierUser = (User) request.getSession().getAttribute("user");
        return cashierUser != null
                && order.getCashierId() != null
                && order.getCashierId() == cashierUser.getId()
                && ("DINE_IN".equalsIgnoreCase(order.getOrderType())
                    || "TAKEAWAY".equalsIgnoreCase(order.getOrderType()));
    }

    private String buildVietQrUrl(ShopSettings shopSettings, String orderCode, double finalAmount) {
        try {
            return "https://vietqr.app/img?bank=" + URLEncoder.encode(shopSettings.getPaymentBankId(), StandardCharsets.UTF_8)
                    + "&acc=" + URLEncoder.encode(shopSettings.getPaymentAccountNo(), StandardCharsets.UTF_8)
                    + "&template=compact"
                    + "&amount=" + Math.round(finalAmount)
                    + "&des=" + URLEncoder.encode(orderCode, StandardCharsets.UTF_8)
                    + "&showinfo=true&fullacc=true"
                    + "&holder=" + URLEncoder.encode(shopSettings.getPaymentAccountName(), StandardCharsets.UTF_8)
                    + "&store=" + URLEncoder.encode("AURA Cafe", StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private void sendJsonError(HttpServletResponse response, int statusCode, String message) throws IOException {
        sendJson(response, statusCode, "{\"success\":false,\"error\":\"" + SimpleJsonParser.escape(message) + "\"}");
    }

    private void sendJson(HttpServletResponse response, int statusCode, String jsonBody) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.print(jsonBody);
        writer.flush();
    }

    private String readRequestBody(HttpServletRequest request) {
        StringBuilder bodyBuilder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line);
            }
        } catch (Exception ignored) {
        }
        return bodyBuilder.toString();
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double parseDoubleOrDefault(String value, double defaultValue) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
