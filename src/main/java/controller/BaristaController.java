package controller;

import dao.CategoryDAO;
import dao.OrderDAO;
import dao.ProductDAO;
import dao.ProductIngredientDAO;
import model.Category;
import model.Order;
import model.OrderDetail;
import model.Product;
import model.ProductIngredient;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@WebServlet(name = "BaristaController", urlPatterns = { "/barista" })
public class BaristaController extends HttpServlet {
    private final OrderDAO orderDAO;
    private final ProductDAO productDAO;
    private final CategoryDAO categoryDAO;
    private final ProductIngredientDAO productIngredientDAO;

    public BaristaController() {
        this(new OrderDAO(), new ProductDAO(), new CategoryDAO(), new ProductIngredientDAO());
    }

    // Package access allows controller tests without a live database.
    BaristaController(OrderDAO orderDAO) {
        this(orderDAO, new ProductDAO(), new CategoryDAO(), new ProductIngredientDAO());
    }

    BaristaController(OrderDAO orderDAO, ProductDAO productDAO, CategoryDAO categoryDAO, ProductIngredientDAO productIngredientDAO) {
        this.orderDAO = orderDAO;
        this.productDAO = productDAO;
        this.categoryDAO = categoryDAO;
        this.productIngredientDAO = productIngredientDAO;
    }

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
        try {
            String action = request.getParameter("action");
            boolean isApi = "orders".equals(action);
            if (!authorize(request, response, isApi)) {
                return;
            }
            if (action == null || action.isBlank()) {
                HttpSession session = request.getSession(false);
                synchronized (session) {
                    if (session.getAttribute("baristaCsrfToken") == null) {
                        session.setAttribute("baristaCsrfToken", UUID.randomUUID().toString());
                    }
                }
                request.setAttribute("baristaDashboard", true);
                request.getRequestDispatcher("/barista-dashboard.jsp").forward(request, response);
                return;
            }
            if ("recipes".equals(action)) {
                handleRecipesView(request, response);
                return;
            }
            if (!"orders".equals(action)) {
                error(response, 400, "Chức năng chưa được hỗ trợ.");
                return;
            }
            try {
                writeJson(response, 200, dashboardData(orderDAO.getOrdersForBarista()));
            } catch (SQLException | ClassNotFoundException e) {
                getServletContext().log("Cannot load barista orders", e);
                error(response, 500, "Không tải được đơn hàng: " + e.getMessage());
            } catch (RuntimeException e) {
                getServletContext().log("Unexpected error in barista orders", e);
                error(response, 500, "Lỗi máy chủ: " + e.getMessage());
            }
        } catch (Exception e) {
            // Lưới an toàn cuối cùng: bất kỳ exception nào chưa được bắt
            // đều trả JSON thay vì để Tomcat redirect về index.jsp
            getServletContext().log("Unhandled error in BaristaController.doGet", e);
            if (!response.isCommitted()) {
                try {
                    error(response, 500, "Lỗi không mong muốn: " + e.getMessage());
                } catch (IOException ignored) { }
            }
        }
    }

    private void handleRecipesView(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            List<Category> categories = categoryDAO.getAllActiveCategoriesWithCount();
            List<Product> products = productDAO.getAllAvailableProducts();
            Map<Integer, List<ProductIngredient>> ingredientsMap = productIngredientDAO.getAllProductIngredientsMap();

            for (Product p : products) {
                p.setIngredients(ingredientsMap.getOrDefault(p.getId(), Collections.emptyList()));
            }

            request.setAttribute("categories", categories);
            request.setAttribute("products", products);

            String productIdParam = request.getParameter("productId");
            if (productIdParam != null && !productIdParam.isBlank()) {
                try {
                    int selectedId = Integer.parseInt(productIdParam.trim());
                    request.setAttribute("selectedProductId", selectedId);
                } catch (NumberFormatException ignored) {
                }
            }

            request.setAttribute("baristaRecipes", true);
            request.getRequestDispatcher("/barista-recipes.jsp").forward(request, response);
        } catch (Exception e) {
            getServletContext().log("Cannot load recipes for barista", e);
            response.sendError(500, "Không thể tải danh sách công thức & định lượng.");
        }
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
        if (!"update-order-status".equals(request.getParameter("action"))) {
            error(response, 400, "Thao tác không hợp lệ.");
            return;
        }
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            String status = request.getParameter("status");
            if (!orderDAO.updateBaristaOrderStatus(orderId, status)) {
                error(response, 409,
                        "Đơn đã thay đổi hoặc chưa đến lượt. Vui lòng tải lại hàng đợi.");
                return;
            }
            writeJson(response, 200, Map.of("success", true));
        } catch (IllegalArgumentException e) {
            error(response, 400, "Mã đơn hoặc trạng thái không hợp lệ.");
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Cannot update barista order", e);
            error(response, 500, "Không cập nhật được đơn hàng. Vui lòng tải lại và thử lại.");
        }
    }

    // UI projection: no payment status, item status or completion time is fabricated.
    static Map<String, Object> dashboardData(List<Order> orders) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<Integer, String> categories = new LinkedHashMap<>();
        for (Order order : orders) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", order.getId());
            row.put("orderCode", order.getOrderCode());
            row.put("orderType", order.getOrderType());
            row.put("status", order.getStatus());
            row.put("createdAt", order.getCreatedAt());
            row.put("tableNumber", order.getTableNumber());
            row.put("recipientName", order.getRecipientName());
            row.put("orderNote", order.getOrderNote());
            row.put("itemCount", order.getItemCount());
            List<Map<String, Object>> items = new ArrayList<>();
            for (OrderDetail item : order.getItems()) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("id", item.getId());
                detail.put("productId", item.getProductId());
                detail.put("productName", item.getProductName());
                detail.put("quantity", item.getQuantity());
                detail.put("customization", item.getCustomization());
                detail.put("categoryId", item.getCategoryId());
                detail.put("categoryName", item.getCategoryName());
                items.add(detail);
                categories.put(item.getCategoryId(), item.getCategoryName());
            }
            row.put("items", items);
            rows.add(row);
        }
        data.put("orders", rows);
        data.put("statistics", Map.of(
                "waiting", orders.stream().filter(o -> "CONFIRMED".equals(o.getStatus())).count(),
                "preparing", orders.stream().filter(o -> "PREPARING".equals(o.getStatus())).count(),
                "ready", orders.stream().filter(o -> "READY".equals(o.getStatus())).count()));
        data.put("categories", categories.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> Map.of("id", e.getKey(), "name", e.getValue()))
                .collect(Collectors.toList()));
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
            } else if (ch < 0x20) {
                result.append(String.format("\\u%04x", (int) ch));
            } else {
                result.append(ch);
            }
        }
        return result.append('"').toString();
    }
}
