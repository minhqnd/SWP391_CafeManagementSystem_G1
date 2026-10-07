package controller;

import dao.OrderDAO;
import model.Cart;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OrdersController - Đơn hàng của tôi
 * GET  /orders : Theo dõi đơn đang thực hiện + lịch sử đơn (lọc theo tab, mã đơn, ngày đặt)
 * POST /orders : action=cancel (hủy đơn chưa thanh toán), action=reorder (đặt lại đơn cũ)
 */
@WebServlet(name = "OrdersController", urlPatterns = {"/orders"})
public class OrdersController extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    private static final int MAX_LIMIT = 100;

    // Tab lọc -> các trạng thái đơn trong Database (rỗng = tất cả)
    private static final Map<String, List<String>> TABS = new LinkedHashMap<>();

    static {
        TABS.put("all", Collections.emptyList());
        TABS.put("pending", Arrays.asList("PENDING"));
        TABS.put("processing", Arrays.asList("CONFIRMED", "PREPARING", "READY"));
        TABS.put("delivering", Arrays.asList("DELIVERING"));
        TABS.put("completed", Arrays.asList("COMPLETED"));
        TABS.put("cancelled", Arrays.asList("CANCELLED"));
    }

    private final OrderDAO orderDAO = new OrderDAO();
    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?login=true");
            return;
        }

        String tab = TABS.containsKey(request.getParameter("tab")) ? request.getParameter("tab") : "all";
        String keyword = trim(request.getParameter("q")).toUpperCase();
        if (keyword.length() > 30) {
            keyword = keyword.substring(0, 30);
        }
        LocalDate date = parseDate(request.getParameter("date"));
        int limit = Math.min(MAX_LIMIT, Math.max(PAGE_SIZE, parseInt(request.getParameter("limit"), PAGE_SIZE)));

        List<String> statuses = TABS.get(tab);
        List<Order> orders = orderDAO.getCustomerOrders(user.getId(), statuses, keyword, date, limit);
        int totalOrders = orderDAO.countCustomerOrders(user.getId(), statuses, keyword, date);

        // Đơn đã thanh toán đang pha chế / giao: hiển thị thẻ theo dõi lớn; còn lại vào lịch sử
        List<Order> activeOrders = new ArrayList<>();
        List<Order> historyOrders = new ArrayList<>();
        for (Order order : orders) {
            if (order.isInProgress()) {
                activeOrders.add(order);
            } else {
                historyOrders.add(order);
            }
        }

        CartController.moveFlashMessage(session, request, "ordersMessage");
        CartController.moveFlashMessage(session, request, "ordersError");

        request.setAttribute("tab", tab);
        request.setAttribute("keyword", keyword);
        request.setAttribute("date", date != null ? date.toString() : "");
        request.setAttribute("limit", limit);
        request.setAttribute("pageSize", PAGE_SIZE);
        request.setAttribute("tabCounts", countByTab(orderDAO.countCustomerOrdersByStatus(user.getId())));
        request.setAttribute("activeOrders", activeOrders);
        request.setAttribute("historyOrders", historyOrders);
        request.setAttribute("shownOrders", orders.size());
        request.setAttribute("totalOrders", totalOrders);
        request.setAttribute("storeAddress", CheckoutController.STORE_ADDRESS);
        request.getRequestDispatcher("/orders.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?login=true");
            return;
        }

        String action = request.getParameter("action");
        int orderId = parseInt(request.getParameter("orderId"), -1);
        Order order = orderDAO.getCustomerOrder(orderId, user.getId());
        String tab = TABS.containsKey(request.getParameter("tab")) ? request.getParameter("tab") : "all";

        if (order == null) {
            session.setAttribute("ordersError", "Không tìm thấy đơn hàng.");
        } else if ("cancel".equals(action)) {
            if (orderDAO.cancelPendingOrder(orderId, user.getId())) {
                session.setAttribute("ordersMessage", "Đã hủy đơn #" + order.getOrderCode()
                        + (order.getVoucherCode() != null ? ". Mã " + order.getVoucherCode() + " đã được hoàn lại lượt dùng." : "."));
            } else {
                session.setAttribute("ordersError", "Chỉ hủy được đơn chưa thanh toán.");
            }
        } else if ("reorder".equals(action)) {
            Cart cart = CartController.getCart(session);
            int added = cartService.addOrderItemsToCart(cart, order.getItems());
            if (added > 0) {
                session.setAttribute("cartMessage", "Đã thêm " + added + " món của đơn #" + order.getOrderCode()
                        + " vào giỏ hàng (giá theo thực đơn hiện tại).");
                response.sendRedirect(request.getContextPath() + "/cart");
                return;
            }
            session.setAttribute("ordersError", "Các món trong đơn #" + order.getOrderCode() + " hiện đã ngừng phục vụ.");
        }

        response.sendRedirect(request.getContextPath() + "/orders?tab=" + tab);
    }

    /** Số đơn của từng tab từ số đơn theo trạng thái */
    private Map<String, Integer> countByTab(Map<String, Integer> statusCounts) {
        Map<String, Integer> tabCounts = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> tab : TABS.entrySet()) {
            int count = 0;
            for (Map.Entry<String, Integer> status : statusCounts.entrySet()) {
                if (tab.getValue().isEmpty() || tab.getValue().contains(status.getKey())) {
                    count += status.getValue();
                }
            }
            tabCounts.put(tab.getKey(), count);
        }
        return tabCounts;
    }

    private LocalDate parseDate(String value) {
        try {
            return value != null && !value.isEmpty() ? LocalDate.parse(value) : null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
