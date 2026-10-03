package controller;

import dao.OrderDAO;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "CartController", urlPatterns = {"/cart/add"})
public class CartController extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

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
        int quantity = 1; // Default add 1
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
}
