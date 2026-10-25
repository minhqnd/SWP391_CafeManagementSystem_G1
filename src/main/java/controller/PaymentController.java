package controller;

import dao.OrderDAO;
import dao.PaymentDAO;
import dao.ShopSettingDAO;
import dao.UserDAO;
import model.Order;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * PaymentController - Bước 3: Thanh toán chuyển khoản VietQR
 * GET  /payment?orderId= : Mã QR + thông tin chuyển khoản của đơn đang chờ thanh toán
 * POST /payment          : Khách xác nhận đã chuyển khoản -> ghi nhận thanh toán, cộng điểm, chuyển sang Hóa đơn điện tử (Bill)
 * POST /payment action=cod: Đơn giao tận nơi trả tiền mặt khi nhận hàng (COD) - shipper thu khi giao
 */
@WebServlet(name = "PaymentController", urlPatterns = {"/payment"})
public class PaymentController extends HttpServlet {

    // Tài khoản nhận tiền của quán (chuẩn VietQR)
    public static final String BANK_ID = "VCB";   // mã ngân hàng theo chuẩn VietQR
    public static final String BANK_NAME = "Vietcombank";
    public static final String ACCOUNT_NO = "9888823456";
    public static final String ACCOUNT_NAME = "CONG TY TNHH AURA CAFE";

    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (!CartController.isCustomer(user)) {
            response.sendRedirect(request.getContextPath() + "/?login=true");
            return;
        }

        Order order = orderDAO.getCustomerOrder(parseInt(request.getParameter("orderId")), user.getId());
        if (order == null) {
            session.setAttribute("ordersError", "Không tìm thấy đơn hàng cần thanh toán.");
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }
        if (!order.isPending()) {
            // Đơn đã thanh toán -> chuyển sang màn hình Hóa đơn điện tử
            response.sendRedirect(request.getContextPath() + "/bill?orderId=" + order.getId());
            return;
        }

        CartController.moveFlashMessage(session, request, "paymentError");
        request.setAttribute("order", order);
        request.setAttribute("bankName", BANK_NAME);
        request.setAttribute("accountNo", ACCOUNT_NO);
        request.setAttribute("accountName", ACCOUNT_NAME);
        request.setAttribute("qrUrl", buildQrUrl(order));
        request.setAttribute("storeAddress", CheckoutController.STORE_ADDRESS);
        request.setAttribute("deliveryMinutes", shopSettingDAO.getSettings().getDeliveryMinutes());
        request.getRequestDispatcher("/payment.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (!CartController.isCustomer(user)) {
            response.sendRedirect(request.getContextPath() + "/?login=true");
            return;
        }

        int orderId = parseInt(request.getParameter("orderId"));
        Order order = orderDAO.getCustomerOrder(orderId, user.getId());
        if (order == null) {
            session.setAttribute("ordersError", "Không tìm thấy đơn hàng cần thanh toán.");
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }

        if ("cod".equals(request.getParameter("action"))) {
            if (!paymentDAO.chooseCashOnDelivery(orderId, user.getId())) {
                session.setAttribute("paymentError", "Chỉ đơn giao tận nơi đang chờ thanh toán mới chọn được COD.");
                response.sendRedirect(request.getContextPath() + "/payment?orderId=" + orderId);
                return;
            }
            CheckoutController.removePaidItems(session, orderId);
            session.setAttribute("ordersMessage", "Đã xác nhận đơn #" + order.getOrderCode()
                    + ". Vui lòng chuẩn bị tiền mặt khi nhận hàng.");
            response.sendRedirect(request.getContextPath() + "/bill?orderId=" + orderId);
            return;
        }

        int earnedPoints = paymentDAO.payByBankTransfer(orderId, user.getId());
        if (earnedPoints == PaymentDAO.FAILED) {
            // Đơn có thể vừa được thanh toán / hủy ở tab khác
            if (!order.isPending()) {
                response.sendRedirect(request.getContextPath() + "/bill?orderId=" + orderId);
            } else {
                session.setAttribute("paymentError", "Chưa ghi nhận được thanh toán, vui lòng thử lại sau ít phút.");
                response.sendRedirect(request.getContextPath() + "/payment?orderId=" + orderId);
            }
            return;
        }

        // Đơn đã thanh toán: lúc này mới bỏ các món khỏi giỏ hàng
        CheckoutController.removePaidItems(session, orderId);
        // Cập nhật điểm Aura trong session theo Database
        user.setTotalPoints(userDAO.getTotalPoints(user.getId()));
        session.setAttribute("ordersMessage", "Thanh toán thành công đơn #" + order.getOrderCode()
                + (earnedPoints > 0 ? ". Bạn được cộng " + earnedPoints + " điểm Aura." : "."));
        // Chuyển hướng sang màn hình Hóa đơn điện tử (Bill Screen)
        response.sendRedirect(request.getContextPath() + "/bill?orderId=" + orderId);
    }

    /**
     * Ảnh mã VietQR (quét bằng app ngân hàng nào cũng được) đã điền sẵn số tiền và nội dung = mã đơn
     */
    private String buildQrUrl(Order order) {
        return "https://img.vietqr.io/image/" + BANK_ID + "-" + ACCOUNT_NO + "-qr_only.png"
                + "?amount=" + Math.round(order.getFinalAmount())
                + "&addInfo=" + URLEncoder.encode(order.getOrderCode(), StandardCharsets.UTF_8)
                + "&accountName=" + URLEncoder.encode(ACCOUNT_NAME, StandardCharsets.UTF_8);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
