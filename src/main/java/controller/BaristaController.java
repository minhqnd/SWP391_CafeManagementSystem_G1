package controller;

import dao.OrderDAO;
import service.BaristaPreparationService;
import model.Order;
import model.OrderDetail;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@WebServlet(name = "BaristaController", urlPatterns = { "/barista", "/barista/orders" })
public class BaristaController extends HttpServlet {
    private final BaristaPreparationService preparation;
    public BaristaController() { this(new OrderDAO()); }

    // Package access allows controller tests without a live database.
    BaristaController(OrderDAO orderDAO) { this.preparation = new BaristaPreparationService(orderDAO); }

    private boolean authorize(HttpServletRequest request, HttpServletResponse response,
                              boolean api) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        User user = null;
        if (session != null) {
            try {
                user = (User) session.getAttribute("user");
            } catch (ClassCastException e) {
                // Session object cũ sau khi redeploy – xóa session và yêu cầu đăng nhập lại
                session.invalidate();
            }
        }
        if (user == null) {
            if (api) {
                error(response, 401, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return false;
        }
        String status = user.getStatus();
        String role = user.getRole();
        String position = user.getPosition();
        if (!"ACTIVE".equals(status) || !"STAFF".equals(role) || !"BARISTA".equals(position)) {
            error(response, 403, "Chỉ nhân viên Barista đang hoạt động được truy cập.");
            return false;
        }
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        boolean api = "/barista/orders".equals(request.getServletPath()) || "orders".equals(action);
        if (!authorize(request, response, api)) return;
        // Existing bookmarks redirect; the old recipe functionality has been removed.
        if (!api && "recipes".equals(action)) {
            response.sendRedirect(request.getContextPath() + "/barista?action=completed");
            return;
        }
        boolean completed = "completed".equals(action);
        if (!api && action != null && !action.isBlank() && !completed) {
            error(response, 400, "Chức năng chưa được hỗ trợ.");
            return;
        }
        if (!api) {
            HttpSession session = request.getSession(false);
            synchronized (session) {
                if (session.getAttribute("baristaCsrfToken") == null) {
                    session.setAttribute("baristaCsrfToken", UUID.randomUUID().toString());
                }
            }
        }
        try {
            Map<String, Object> data = dashboardData(preparation.getOrders());
            if (api) {
                writeJson(response, 200, data);
                return;
            }
            request.setAttribute("baristaInitialData", json(data));
        } catch (SQLException | ClassNotFoundException | RuntimeException e) {
            getServletContext().log("Cannot load barista orders", e);
            if (api) {
                error(response, 500, "Không tải được đơn hàng từ cơ sở dữ liệu.");
                return;
            }
            request.setAttribute("baristaInitialError", "Không tải được đơn hàng từ cơ sở dữ liệu.");
        }
        request.setAttribute(completed ? "baristaCompleted" : "baristaDashboard", true);
        request.getRequestDispatcher(completed ? "/barista-recipes.jsp" : "/barista-dashboard.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        if (!authorize(request, response, true)) {
            return;
        }
        Object token = request.getSession(false).getAttribute("baristaCsrfToken");
        if (!(token instanceof String) || !token.equals(request.getParameter("csrfToken"))) {
            error(response, 403, "Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
            return;
        }
        try {
            boolean updated;
            String action = request.getParameter("action");
            if ("update-items".equals(action)) {
                String raw = request.getParameter("itemIds");
                String prepared = request.getParameter("isPrepared");
                if (raw == null || raw.length() > 12000
                        || !("true".equals(prepared) || "false".equals(prepared))) {
                    throw new IllegalArgumentException("Invalid item data");
                }
                List<Integer> ids = new ArrayList<>();
                for (String id : raw.split(",", -1)) ids.add(Integer.parseInt(id.trim()));
                updated = preparation.updateItems(ids, Boolean.parseBoolean(prepared));
            } else if ("update-order-status".equals(action)) {
                int orderId = Integer.parseInt(request.getParameter("orderId"));
                updated = preparation.updateStatus(orderId, request.getParameter("status"));
            } else {
                error(response, 400, "Thao tác không hợp lệ.");
                return;
            }
            if (!updated) {
                error(response, 409, "Đơn đã thay đổi hoặc không còn được pha chế. Vui lòng kiểm tra lại.");
                return;
            }
            writeJson(response, 200, Map.of("success", true));
        } catch (IllegalArgumentException e) {
            error(response, 400, "Mã đơn hoặc trạng thái không hợp lệ.");
        } catch (SQLException e) {
            getServletContext().log("Cannot update barista order", e);
            if (e.getMessage() != null && e.getMessage().startsWith("Không đủ tồn kho")) {
                error(response, 409, e.getMessage());
                return;
            }
            error(response, 500, "Không cập nhật được đơn hàng. Vui lòng tải lại và thử lại.");
        } catch (ClassNotFoundException e) {
            getServletContext().log("Cannot update barista order", e);
            error(response, 500, "Không cập nhật được đơn hàng. Vui lòng tải lại và thử lại.");
        }
    }

    // Projection shared by the active queue and read-only completed page.
    static Map<String, Object> dashboardData(List<Order> orders) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", order.getId());
            row.put("orderCode", order.getOrderCode());
            row.put("orderType", order.getOrderType());
            row.put("status", order.getStatus());
            row.put("createdAt", order.getCreatedAt());
            row.put("readyAt", order.getReadyAt());
            row.put("tableNumber", order.getTableNumber());
            row.put("recipientName", order.getRecipientName());
            row.put("orderNote", order.getOrderNote());
            row.put("itemCount", order.getItemCount());
            List<Map<String, Object>> items = new ArrayList<>();
            for (OrderDetail item : order.getItems()) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("id", item.getId());
                detail.put("itemId", item.getId());
                detail.put("isPrepared", item.isPrepared());
                detail.put("productId", item.getProductId());
                detail.put("productName", item.getProductName());
                detail.put("quantity", item.getQuantity());
                detail.put("customization", item.getCustomization());
                items.add(detail);
            }
            row.put("items", items);
            rows.add(row);
        }
        data.put("orders", rows);
        return data;
    }

    private void error(HttpServletResponse response, int status, String message)
            throws IOException {
        writeJson(response, status, Map.of("message", message));
    }

    private void writeJson(HttpServletResponse response, int status, Object data)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json(data));
    }

    // Minimal JSON writer for the explicit Map/List/scalar projection above.
    static String json(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(e -> json(e.getKey().toString()) + ":" + json(e.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (value instanceof Iterable<?> values) {
            List<String> parts = new ArrayList<>();
            for (Object item : values) {
                parts.add(json(item));
            }
            return "[" + String.join(",", parts) + "]";
        }
        StringBuilder result = new StringBuilder("\"");
        for (char ch : value.toString().toCharArray()) {
            if (ch == '"' || ch == '\\') {
                result.append('\\').append(ch);
            } else if (ch < 0x20 || ch == '<' || ch == '>' || ch == '&') {
                result.append(String.format("\\u%04x", (int) ch));
            } else {
                result.append(ch);
            }
        }
        return result.append('"').toString();
    }
}
