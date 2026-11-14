package controller;

import dao.DeliveryDAO;
import dao.OrderDAO;
import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Delivery;
import model.DeliveryIssue;
import model.User;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DeliveryController - Màn hình Giao hàng của Shipper (Screen 14: UC26, UC27, UC28, UC29, UC30)
 * Quy trình 3 bước, mỗi bước 1 tab:
 * GET  /delivery?tab=available  : (1) Chờ nhận - đơn Barista đã pha xong, chưa ai nhận
 * GET  /delivery?tab=delivering : (2) Đang giao - đơn của mình, giao xong + thu COD, báo sự cố
 * GET  /delivery?tab=cod&date= : (3) Nộp tiền - tiền COD đang giữ + đơn đã giao theo ngày (mặc định hôm nay)
 * POST action=claim&orderId=    : nhận đơn & đi giao luôn (ai bấm trước người đó giao)
 * POST action=start             : đơn Admin gán sẵn - nhận & đi giao khi quán pha xong
 * POST action=deliver           : giao thành công (đơn COD phải tick "đã thu đủ tiền")
 * POST action=report            : báo sự cố giao hàng (UC29)
 * POST action=handover          : nộp tiền COD đang giữ cho Thu ngân (UC30)
 * Chỉ tài khoản STAFF có chức vụ SHIPPER đang hoạt động.
 */
@WebServlet(name = "DeliveryController", urlPatterns = {"/delivery"})
public class DeliveryController extends HttpServlet {

    private static final int MAX_TEXT_LENGTH = 500;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User shipper = getAuthorizedShipper(request, response);
        if (shipper == null) return;

        HttpSession session = request.getSession();
        CartController.moveFlashMessage(session, request, "deliverySuccessMsg");
        CartController.moveFlashMessage(session, request, "deliveryErrorMsg");
        if (session.getAttribute("deliveryCsrfToken") == null) {
            session.setAttribute("deliveryCsrfToken", UUID.randomUUID().toString());
        }

        try {
            int shipperId = shipper.getId();
            List<Delivery> active = deliveryDAO.getActiveDeliveries(shipperId);
            List<Delivery> available = deliveryDAO.getAvailableOrders();
            List<Delivery> codHolding = deliveryDAO.getCodHolding(shipperId);
            BigDecimal codHoldingTotal = codHolding.stream()
                    .map(d -> d.getCodCollected() == null ? BigDecimal.ZERO : d.getCodCollected())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Không chọn tab: đang giữ đơn thì mở "Đang giao", chưa có thì mở "Chờ nhận"
            String tab = request.getParameter("tab");
            if (!"available".equals(tab) && !"delivering".equals(tab) && !"cod".equals(tab)) {
                tab = active.isEmpty() ? "available" : "delivering";
            }

            if ("available".equals(tab)) {
                for (Delivery d : available) d.setItems(orderDAO.getOrderDetails(d.getOrderId()));
            } else if ("delivering".equals(tab)) {
                for (Delivery d : active) {
                    d.setItems(orderDAO.getOrderDetails(d.getOrderId()));
                    d.setIssues(deliveryDAO.getIssues(d.getId()));
                }
            } else {
                // Đơn đã giao xem theo từng ngày (mặc định hôm nay) để khỏi lẫn đơn các ngày khác
                LocalDate date = parseDateOrToday(request.getParameter("date"));
                List<Delivery> delivered = deliveryDAO.getDeliveredOn(shipperId, date);
                request.setAttribute("deliveredList", delivered);
                request.setAttribute("deliveredCodTotal", delivered.stream()
                        .map(d -> d.getCodCollected() == null ? BigDecimal.ZERO : d.getCodCollected())
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                request.setAttribute("selectedDate", date.toString());
                request.setAttribute("selectedDateDisplay", date.format(DISPLAY_DATE));
                request.setAttribute("isToday", date.equals(LocalDate.now()));
                request.setAttribute("cashiers", userDAO.getStaffEmployees().stream()
                        .filter(u -> "CASHIER".equals(u.getPosition()))
                        .collect(Collectors.toList()));
            }

            request.setAttribute("tab", tab);
            request.setAttribute("availableOrders", available);
            request.setAttribute("activeDeliveries", active);
            request.setAttribute("codHolding", codHolding);
            request.setAttribute("codHoldingTotal", codHoldingTotal);
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Failed to load deliveries", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được danh sách giao hàng. Hãy chạy lại database/aura_cafe.sql (bảng deliveries).");
            return;
        }

        request.setAttribute("today", LocalDate.now().toString());
        request.setAttribute("issueTypes", DeliveryIssue.TYPES);
        request.getRequestDispatcher("/delivery-dashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        User shipper = getAuthorizedShipper(request, response);
        if (shipper == null) return;

        HttpSession session = request.getSession();
        String action = request.getParameter("action");
        String back = request.getContextPath() + "/delivery?tab="
                + ("handover".equals(action) ? "cod" : "claim".equals(action) ? "available" : "delivering");

        String expected = (String) session.getAttribute("deliveryCsrfToken");
        if (expected == null || !expected.equals(request.getParameter("csrfToken"))) {
            session.setAttribute("deliveryErrorMsg", "Phiên làm việc đã được làm mới. Vui lòng thao tác lại.");
            response.sendRedirect(back);
            return;
        }

        try {
            int shipperId = shipper.getId();
            if ("handover".equals(action)) {
                handOverCod(request, session, shipperId);
            } else if ("claim".equals(action)) {
                int deliveryId = deliveryDAO.claim(parseId(request.getParameter("orderId")), shipperId);
                if (deliveryId <= 0) {
                    throw new IllegalStateException("Đơn này đã có shipper khác nhận hoặc không còn chờ giao.");
                }
                Delivery claimed = deliveryDAO.getForShipper(deliveryId, shipperId);
                session.setAttribute("deliverySuccessMsg", "Đã nhận đơn #" + claimed.getOrderCode()
                        + ". Mang hàng đi giao, giao xong bấm \"Đã giao xong\".");
                back = request.getContextPath() + "/delivery?tab=delivering";
            } else {
                int id = parseId(request.getParameter("id"));
                Delivery delivery = id > 0 ? deliveryDAO.getForShipper(id, shipperId) : null;
                if (delivery == null) {
                    throw new IllegalArgumentException("Đơn giao hàng không tồn tại hoặc không thuộc về bạn.");
                }
                String code = "#" + delivery.getOrderCode();
                switch (action == null ? "" : action) {
                    case "start":
                        if (!deliveryDAO.startDelivery(id, shipperId)) {
                            throw new IllegalStateException("Chỉ nhận đi giao khi quán đã pha xong đơn " + code + ".");
                        }
                        session.setAttribute("deliverySuccessMsg", "Đã nhận đơn " + code + ", bắt đầu đi giao.");
                        break;
                    case "deliver":
                        completeDelivery(request, session, delivery, shipperId);
                        break;
                    case "report":
                        reportIssue(request, session, delivery, shipperId);
                        break;
                    default:
                        throw new IllegalArgumentException("Thao tác không hợp lệ.");
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            session.setAttribute("deliveryErrorMsg", e.getMessage());
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Delivery operation failed", e);
            session.setAttribute("deliveryErrorMsg", "Không thể lưu thao tác giao hàng vào database. Vui lòng thử lại.");
        }
        response.sendRedirect(back);
    }

    /** Đơn COD: shipper tick "đã thu đủ tiền" -> ghi nhận đã thu đúng số tiền phải thu */
    private void completeDelivery(HttpServletRequest request, HttpSession session, Delivery delivery, int shipperId)
            throws SQLException, ClassNotFoundException {
        BigDecimal collected = null;
        if (delivery.isCod()) {
            if (request.getParameter("codConfirmed") == null) {
                throw new IllegalArgumentException("Hãy tick \"Đã thu đủ " + formatMoney(delivery.getCodAmount())
                        + "\". Nếu khách không trả đủ, hãy báo sự cố.");
            }
            collected = delivery.getCodAmount();
        }
        // CD02: . Ở đây em kiểm tra shipper đã xác nhận thu đủ tiền trước khi kết thúc đơn COD.
        // .. Em gửi mã phiếu, mã shipper và số tiền đã thu xuống phần cập nhật.
        // ... DeliveryDAO.completeDelivery(mã phiếu, mã shipper, tiền thu, ghi chú) [CD03].
        int result = deliveryDAO.completeDelivery(delivery.getId(), shipperId, collected, null);
        if (result != DeliveryDAO.OK) {
            throw new IllegalStateException("Chỉ xác nhận giao xong khi đơn đang được giao.");
        }
        session.setAttribute("deliverySuccessMsg", "Đã giao xong đơn #" + delivery.getOrderCode()
                + (delivery.isCod() ? ", thu " + formatMoney(collected) + ". Nhớ nộp tiền ở bước 3." : "."));
    }

    private void reportIssue(HttpServletRequest request, HttpSession session, Delivery delivery, int shipperId)
            throws SQLException, ClassNotFoundException {
        String type = request.getParameter("issueType");
        if (type == null || !DeliveryIssue.TYPES.containsKey(type)) {
            throw new IllegalArgumentException("Vui lòng chọn loại sự cố.");
        }
        String description = normalizeText(request.getParameter("description"), "Mô tả sự cố");
        int result = deliveryDAO.reportIssue(delivery.getId(), shipperId, type, description);
        if (result == DeliveryDAO.NOT_ALLOWED) {
            throw new IllegalStateException("Chỉ báo sự cố cho đơn chưa giao xong.");
        }
        session.setAttribute("deliverySuccessMsg", "Đã báo sự cố và huỷ đơn #" + delivery.getOrderCode()
                + ". Mang hàng về quán" + (delivery.isCod() ? ", không thu tiền của khách." : "."));
    }

    private void handOverCod(HttpServletRequest request, HttpSession session, int shipperId)
            throws SQLException, ClassNotFoundException {
        int cashierId = parseId(request.getParameter("cashierId"));
        User cashier = cashierId > 0 ? userDAO.getActiveStaffById(cashierId) : null;
        if (cashier == null || !"CASHIER".equals(cashier.getPosition())) {
            throw new IllegalArgumentException("Vui lòng chọn Thu ngân nhận tiền.");
        }
        BigDecimal total = deliveryDAO.handOverCod(shipperId, cashierId);
        if (total == null) throw new IllegalStateException("Bạn không giữ khoản tiền COD nào cần nộp.");
        session.setAttribute("deliverySuccessMsg", "Đã ghi nhận nộp " + formatMoney(total)
                + " tiền COD cho Thu ngân " + cashier.getFullName() + ".");
    }

    private User getAuthorizedShipper(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return null;
        }
        if (!"STAFF".equalsIgnoreCase(user.getRole()) || !"SHIPPER".equalsIgnoreCase(user.getPosition())
                || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            // Hay gặp khi test: cùng 1 trình duyệt đăng nhập tài khoản khác ở tab khác -> phiên bị thay
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ nhân viên giao hàng được truy cập màn hình này. "
                    + "Trình duyệt đang đăng nhập bằng tài khoản " + user.getFullName()
                    + " - mỗi trình duyệt chỉ giữ 1 phiên đăng nhập, hãy dùng trình duyệt khác / cửa sổ ẩn danh cho Shipper.");
            return null;
        }
        return user;
    }

    private String normalizeText(String raw, String fieldName) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập " + fieldName.toLowerCase() + ".");
        if (value.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(fieldName + " tối đa " + MAX_TEXT_LENGTH + " ký tự.");
        }
        return value;
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%,d", value == null ? 0 : value.longValue()).replace(',', '.') + "đ";
    }

    /** Ngày xem đơn đã giao: sai định dạng hoặc ở tương lai thì lấy hôm nay */
    private LocalDate parseDateOrToday(String raw) {
        try {
            LocalDate date = raw == null || raw.isBlank() ? LocalDate.now() : LocalDate.parse(raw.trim());
            return date.isAfter(LocalDate.now()) ? LocalDate.now() : date;
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    private int parseId(String raw) {
        try {
            int id = Integer.parseInt(raw == null ? "" : raw.trim());
            return Math.max(id, 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
