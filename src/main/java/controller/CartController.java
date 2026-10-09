package controller;

import dao.UserDAO;
import model.Cart;
import model.CartItem;
import model.User;
import service.CartService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

/**
 * CartController - Giỏ hàng lưu trong session (không bắt buộc đăng nhập)
 * GET  /cart      : Xem giỏ hàng
 * POST /cart      : Tăng/giảm số lượng, chọn món, xoá món, áp dụng voucher
 * POST /cart/add  : Thêm món từ trang Menu / Chi tiết món / Trang chủ (trả về JSON)
 */
@WebServlet(name = "CartController", urlPatterns = {"/cart", "/cart/add", "/cart/api"})
public class CartController extends HttpServlet {

    private final CartService cartService = new CartService();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        Cart cart = getCart(session);
        Integer customerId = getCustomerId(session);

        if ("/cart/api".equals(request.getServletPath()) || "json".equals(request.getParameter("action"))) {
            writeCartJsonResponse(response, cart);
            return;
        }

        // Cập nhật giá & trạng thái món theo Database
        cartService.refreshCart(cart, customerId);

        moveFlashMessage(session, request, "cartMessage");
        moveFlashMessage(session, request, "cartError");

        request.setAttribute("cart", cart);
        
        if (customerId != null) {
            request.setAttribute("totalPoints", userDAO.getTotalPoints(customerId));
        }
        request.getRequestDispatcher("/cart.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if ("/cart/add".equals(request.getServletPath())) {
            handleAddToCart(request, response);
            return;
        }

        HttpSession session = request.getSession();
        Cart cart = getCart(session);
        String action = request.getParameter("action");
        int lineId = parseInt(request.getParameter("lineId"), -1);

        if ("increase".equals(action)) {
            cart.changeQuantity(lineId, 1);
        } else if ("decrease".equals(action)) {
            CartItem item = cart.findItem(lineId);
            if (item != null && item.getQuantity() <= 1) {
                cart.removeItem(lineId);
            } else {
                cart.changeQuantity(lineId, -1);
            }
        } else if ("toggle".equals(action)) {
            cart.toggleSelected(lineId);
        } else if ("selectAll".equals(action)) {
            cart.setAllSelected(!cart.isAllSelected());
        } else if ("remove".equals(action)) {
            cart.removeItem(lineId);
        } else if ("clear".equals(action)) {
            cart.getItems().clear();
        } else if ("removeSelected".equals(action)) {
            cart.removeSelectedItems();
        } else if ("applyPoints".equals(action)) {
            applyPoints(session, cart, request.getParameter("pointsToUse"));
        } else if ("removePoints".equals(action)) {
            cart.setPointsUsed(0);
        }

        if ("/cart/api".equals(request.getServletPath()) || "json".equals(request.getParameter("format"))) {
            writeCartJsonResponse(response, cart);
            return;
        }

        // PRG: quay lại trang giỏ hàng để tránh gửi lại form khi F5
        response.sendRedirect(request.getContextPath() + "/cart");
    }

    private void writeCartJsonResponse(HttpServletResponse response, Cart cart) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"success\":true,");
        json.append("\"totalQuantity\":").append(cart.getTotalQuantity()).append(",");
        double totalAmount = 0;
        for (CartItem it : cart.getItems()) {
            totalAmount += it.getSubtotal();
        }
        json.append("\"totalAmount\":").append((long) totalAmount).append(",");
        json.append("\"items\":[");
        List<CartItem> items = cart.getItems();
        for (int i = 0; i < items.size(); i++) {
            CartItem it = items.get(i);
            if (i > 0) json.append(",");
            json.append("{");
            json.append("\"lineId\":").append(it.getLineId()).append(",");
            json.append("\"productId\":").append(it.getProductId()).append(",");
            json.append("\"name\":").append(escapeJson(it.getProductName())).append(",");
            json.append("\"imageUrl\":").append(escapeJson(it.getImageUrl() != null ? it.getImageUrl() : "")).append(",");
            json.append("\"size\":").append(escapeJson(it.getSize() != null ? it.getSize() : "")).append(",");
            json.append("\"sizeLabel\":").append(escapeJson(it.getSizeLabel())).append(",");
            json.append("\"unitPrice\":").append((long) it.getUnitPrice()).append(",");
            json.append("\"quantity\":").append(it.getQuantity()).append(",");
            json.append("\"subtotal\":").append((long) it.getSubtotal()).append(",");
            json.append("\"toppings\":[");
            List<String> tops = it.getToppings();
            for (int j = 0; j < tops.size(); j++) {
                if (j > 0) json.append(",");
                json.append(escapeJson(tops.get(j)));
            }
            json.append("]");
            json.append("}");
        }
        json.append("]");
        json.append("}");
        response.getWriter().write(json.toString());
    }

    private String escapeJson(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private void handleAddToCart(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        int productId = parseInt(request.getParameter("productId"), -1);
        int quantity = parseInt(request.getParameter("quantity"), 1);
        if (productId <= 0 || quantity <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"Thông tin món không hợp lệ.\"}");
            return;
        }

        List<String> toppings = new ArrayList<>();
        String[] toppingParams = request.getParameterValues("topping");
        if (toppingParams != null) {
            for (String topping : toppingParams) {
                toppings.add(topping.trim());
            }
        }

        CartItem item = cartService.createItem(productId, request.getParameter("size"), toppings, quantity);
        if (item == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"success\":false,\"error\":\"Món này hiện đã ngừng phục vụ.\"}");
            return;
        }

        Cart cart = getCart(request.getSession());
        cart.addItem(item);
        response.getWriter().write("{\"success\":true,\"totalItems\":" + cart.getTotalQuantity() + "}");
    }

    private void applyPoints(HttpSession session, Cart cart, String pointsStr) {
        if (pointsStr == null || pointsStr.trim().isEmpty()) {
            session.setAttribute("cartError", "Vui lòng nhập số điểm muốn đổi.");
            return;
        }

        Integer customerId = getCustomerId(session);
        if (customerId == null) {
            session.setAttribute("cartError", "Bạn cần đăng nhập để đổi điểm thưởng.");
            return;
        }

        int pointsToUse = parseInt(pointsStr, 0);
        if (pointsToUse <= 0) {
            session.setAttribute("cartError", "Số điểm quy đổi phải lớn hơn 0.");
            return;
        }

        int availablePoints = userDAO.getTotalPoints(customerId);
        if (pointsToUse > availablePoints) {
            session.setAttribute("cartError", "Bạn chỉ có " + availablePoints + " điểm.");
            return;
        }

        double maxDiscount = cart.getSelectedSubtotal();
        double discountAmount = pointsToUse * Cart.DISCOUNT_PER_POINT;

        if (discountAmount > maxDiscount) {
            int maxPoints = (int) Math.ceil(maxDiscount / Cart.DISCOUNT_PER_POINT);
            session.setAttribute("cartError", "Đơn hàng này chỉ cần tối đa " + maxPoints + " điểm.");
            return;
        }

        cart.setPointsUsed(pointsToUse);
        session.setAttribute("cartMessage", "Đã áp dụng " + pointsToUse + " điểm.");
    }

    static Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(Cart.SESSION_KEY);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(Cart.SESSION_KEY, cart);
        }
        return cart;
    }

    /** Khách đang đăng nhập (null nếu chưa) - dùng để lấy voucher đổi điểm của riêng khách */
    static Integer getCustomerId(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null ? user.getId() : null;
    }

    /** Chuyển thông báo 1 lần (flash) từ session sang request sau khi redirect */
    static void moveFlashMessage(HttpSession session, HttpServletRequest request, String key) {
        Object message = session.getAttribute(key);
        if (message != null) {
            request.setAttribute(key, message);
            session.removeAttribute(key);
        }
    }

    private String formatMoney(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount) + "đ";
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
