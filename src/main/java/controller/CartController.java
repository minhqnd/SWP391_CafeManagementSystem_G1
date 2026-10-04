package controller;

import dao.VoucherDAO;
import model.Cart;
import model.CartItem;
import model.Voucher;
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
@WebServlet(name = "CartController", urlPatterns = {"/cart", "/cart/add"})
public class CartController extends HttpServlet {

    private final CartService cartService = new CartService();
    private final VoucherDAO voucherDAO = new VoucherDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        Cart cart = getCart(session);

        // Cập nhật giá & trạng thái món theo Database, kiểm tra voucher còn hiệu lực
        String voucherCode = cart.getVoucher() != null ? cart.getVoucher().getCode() : null;
        if (!cartService.refreshCart(cart)) {
            session.setAttribute("cartError", "Mã " + voucherCode + " đã hết hạn hoặc hết lượt sử dụng.");
        }

        moveFlashMessage(session, request, "cartMessage");
        moveFlashMessage(session, request, "cartError");

        request.setAttribute("cart", cart);
        request.setAttribute("vouchers", voucherDAO.getUsableVouchers());
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
            cart.changeQuantity(lineId, -1);
        } else if ("toggle".equals(action)) {
            cart.toggleSelected(lineId);
        } else if ("selectAll".equals(action)) {
            cart.setAllSelected(!cart.isAllSelected());
        } else if ("remove".equals(action)) {
            cart.removeItem(lineId);
        } else if ("removeSelected".equals(action)) {
            cart.removeSelectedItems();
        } else if ("applyVoucher".equals(action)) {
            applyVoucher(session, cart, request.getParameter("voucherCode"));
        } else if ("removeVoucher".equals(action)) {
            cart.setVoucher(null);
        }

        // PRG: quay lại trang giỏ hàng để tránh gửi lại form khi F5
        response.sendRedirect(request.getContextPath() + "/cart");
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

    private void applyVoucher(HttpSession session, Cart cart, String code) {
        if (code == null || code.trim().isEmpty()) {
            session.setAttribute("cartError", "Vui lòng nhập mã khuyến mãi.");
            return;
        }

        String voucherCode = code.trim().toUpperCase();
        Voucher voucher = voucherDAO.getUsableVoucherByCode(voucherCode);
        if (voucher == null) {
            session.setAttribute("cartError", "Mã " + voucherCode + " không tồn tại hoặc đã hết hạn.");
            return;
        }

        cartService.refreshCart(cart);
        if (!voucher.isEligible(cart.getSelectedSubtotal())) {
            session.setAttribute("cartError", "Mã " + voucher.getCode() + " áp dụng cho đơn từ "
                    + formatMoney(voucher.getMinOrderValue()) + ". Hãy chọn thêm món nhé!");
            return;
        }

        cart.setVoucher(voucher);
        session.setAttribute("cartMessage", "Đã áp dụng mã " + voucher.getCode() + ".");
    }

    static Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(Cart.SESSION_KEY);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(Cart.SESSION_KEY, cart);
        }
        return cart;
    }

    private void moveFlashMessage(HttpSession session, HttpServletRequest request, String key) {
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
