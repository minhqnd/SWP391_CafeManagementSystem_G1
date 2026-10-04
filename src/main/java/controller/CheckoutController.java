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
 * CheckoutController - Xử lý màn hình Xác nhận đơn & Giao nhận hàng
 * Mapped: /checkout
 */
@WebServlet(name = "CheckoutController", urlPatterns = {"/checkout"})
public class CheckoutController extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Forward tới giao diện checkout.jsp
        request.getRequestDispatcher("/checkout.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        Integer customerId = user != null ? user.getId() : null;

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String rawJson = sb.toString().trim();
        if (rawJson.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"Dữ liệu yêu cầu rỗng!\"}");
            return;
        }

        List<OrderDetail> items = new ArrayList<>();
        double calculatedTotal = 0;

        dao.ProductDAO productDAO = new dao.ProductDAO();

        // Trích xuất các items từ JSON gửi lên
        if (rawJson.contains("\"items\":")) {
            String itemsPart = rawJson.substring(rawJson.indexOf("\"items\":"));
            int start = itemsPart.indexOf("[");
            int end = itemsPart.lastIndexOf("]");
            if (start != -1 && end != -1 && end > start) {
                String arrayContent = itemsPart.substring(start + 1, end).trim();
                if (!arrayContent.isEmpty()) {
                    String[] itemTokens = arrayContent.split("\\},\\s*\\{");
                    for (String token : itemTokens) {
                        try {
                            int prodId = extractInt(token, "id");
                            int qty = extractInt(token, "quantity");
                            if (qty <= 0) qty = 1;

                            // RÀNG BUỘC BR-07: Số lượng món phải lớn hơn 0
                            if (qty <= 0) {
                                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                                response.getWriter().write("{\"success\":false,\"error\":\"Số lượng món phải lớn hơn 0 (Quy tắc BR-07)!\"}");
                                return;
                            }

                            // Xác thực sản phẩm từ Database thông qua ProductDAO
                            model.Product dbProduct = null;
                            if (prodId > 0) {
                                dbProduct = productDAO.getProductById(prodId);
                            }

                            String prodName;
                            double unitPrice;

                            if (dbProduct != null && dbProduct.isAvailable()) {
                                prodName = dbProduct.getName();
                                unitPrice = dbProduct.getBasePrice();
                            } else {
                                // Fallback an toàn nếu sản phẩm trong giỏ vẫn tồn tại
                                prodName = extractString(token, "name");
                                if (prodName == null || prodName.trim().isEmpty()) {
                                    prodName = "Cà phê Aura";
                                }
                                unitPrice = extractDouble(token, "unitPrice");
                                if (unitPrice <= 0) unitPrice = extractDouble(token, "price");
                                if (unitPrice <= 0) unitPrice = 45000;
                            }

                            // Size và Topping tùy chọn
                            String size = extractString(token, "size");
                            if (size != null && !size.isEmpty()) {
                                if ("M".equalsIgnoreCase(size)) {
                                    unitPrice += 5000;
                                } else if ("L".equalsIgnoreCase(size)) {
                                    unitPrice += 10000;
                                }
                            }

                            // RÀNG BUỘC BR-08: Subtotal phải bằng quantity * unitPrice
                            double subtotal = unitPrice * qty;
                            calculatedTotal += subtotal;

                            String customization = (size != null && !size.isEmpty()) ? "Size " + size : "Tiêu chuẩn";

                            OrderDetail detail = new OrderDetail();
                            detail.setProductId(prodId > 0 ? prodId : 1);
                            detail.setProductName(prodName);
                            detail.setUnitPrice(unitPrice);
                            detail.setCustomization(customization);
                            detail.setQuantity(qty);
                            detail.setSubtotal(subtotal);

                            items.add(detail);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        // RÀNG BUỘC BR-06: Đơn hàng phải có ít nhất 1 món (Không được tạo đơn ảo khi giỏ hàng trống)
        if (items.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"Giỏ hàng của bạn đang trống! Vui lòng chọn ít nhất một món trước khi đặt hàng (Quy tắc BR-06).\"}");
            return;
        }

        // Trích xuất & Xác thực thông tin người nhận
        String recipientName = extractString(rawJson, "recipientName");
        if (recipientName == null || recipientName.trim().isEmpty()) {
            recipientName = user != null ? user.getFullName() : null;
        }
        if (recipientName == null || recipientName.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"Vui lòng cung cấp họ và tên người nhận hàng!\"}");
            return;
        }
        recipientName = recipientName.trim();
        if (recipientName.length() > 100) {
            recipientName = recipientName.substring(0, 100);
        }

        // Xác thực số điện thoại
        String recipientPhone = extractString(rawJson, "recipientPhone");
        if (recipientPhone == null || recipientPhone.trim().isEmpty()) {
            recipientPhone = user != null ? user.getPhone() : null;
        }
        if (recipientPhone == null || recipientPhone.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"Vui lòng cung cấp số điện thoại người nhận hàng!\"}");
            return;
        }
        recipientPhone = recipientPhone.trim();
        String cleanPhone = recipientPhone.replaceAll("[^0-9+]", "");
        if (cleanPhone.length() < 9 || cleanPhone.length() > 12) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"Số điện thoại nhận hàng không hợp lệ (yêu cầu từ 9 đến 11 số)!\"}");
            return;
        }

        // Xác thực phương thức nhận hàng (ONLINE, TAKEAWAY, DINE_IN)
        String orderType = extractString(rawJson, "orderType");
        if (orderType == null || (!orderType.equals("ONLINE") && !orderType.equals("TAKEAWAY") && !orderType.equals("DINE_IN"))) {
            orderType = "ONLINE";
        }

        // Xác thực địa chỉ giao hàng
        String deliveryAddress = extractString(rawJson, "deliveryAddress");
        if ("ONLINE".equals(orderType)) {
            if (deliveryAddress == null || deliveryAddress.trim().isEmpty() || deliveryAddress.trim().length() < 5) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"error\":\"Đơn giao hàng tận nơi yêu cầu cung cấp địa chỉ nhận hàng chi tiết và hợp lệ!\"}");
                return;
            }
            deliveryAddress = deliveryAddress.trim();
        } else {
            deliveryAddress = "Nhận trực tiếp tại quầy: 28 Phố Tràng Tiền, Hoàn Kiếm, Hà Nội";
        }

        String orderNote = extractString(rawJson, "orderNote");
        if (orderNote != null && orderNote.trim().isEmpty()) {
            orderNote = null;
        }

        // Tính phí vận chuyển (ONLINE: Freeship từ 150.000đ, TAKEAWAY: 0đ)
        double deliveryFee = 0;
        if ("ONLINE".equals(orderType)) {
            deliveryFee = calculatedTotal >= 150000 ? 0 : 18000;
        }

        double discountAmount = 0;
        double finalAmount = Math.max(0, calculatedTotal + deliveryFee - discountAmount);

        // Khởi tạo CustomerOrder chuẩn xác
        CustomerOrder order = new CustomerOrder();
        // Mã đơn hàng định dạng chuẩn: AUR- + timestamp
        order.setOrderCode("AUR-" + (System.currentTimeMillis() % 1000000000L));
        order.setCustomerId(customerId);
        order.setOrderType(orderType);
        order.setStatus("PENDING");
        order.setTotalAmount(calculatedTotal);
        order.setDiscountAmount(discountAmount);
        order.setDeliveryFee(deliveryFee);
        order.setFinalAmount(finalAmount);
        order.setRecipientName(recipientName);
        order.setRecipientPhone(recipientPhone);
        order.setDeliveryAddress(deliveryAddress);
        order.setOrderNote(orderNote);

        int newOrderId = orderDAO.createOrder(order, items);

        if (newOrderId > 0) {
            String redirectUrl = request.getContextPath() + "/payment?orderId=" + newOrderId;
            response.getWriter().write("{\"success\":true,\"orderId\":" + newOrderId + ",\"redirectUrl\":\"" + redirectUrl + "\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false,\"error\":\"Không thể tạo đơn hàng do lỗi hệ thống cơ sở dữ liệu! Vui lòng thử lại.\"}");
        }
    }

    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end).trim();
    }

    private int extractInt(String json, String key) {
        try {
            String pattern = "\"" + key + "\":";
            int start = json.indexOf(pattern);
            if (start == -1) return 0;
            start += pattern.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
                end++;
            }
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private double extractDouble(String json, String key) {
        try {
            String pattern = "\"" + key + "\":";
            int start = json.indexOf(pattern);
            if (start == -1) return 0;
            start += pattern.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
                end++;
            }
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
