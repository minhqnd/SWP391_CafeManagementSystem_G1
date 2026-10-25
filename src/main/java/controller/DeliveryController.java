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
 * GET  /delivery?tab=active|done|cod      : đơn đang được phân công / đã giao / tiền COD
 * GET  /delivery?id=                      : chi tiết 1 lần giao (UC28)
 * POST action=accept | pickup | deliver   : nhận đơn, lấy hàng bắt đầu giao, giao thành công (+ thu COD)
 * POST action=report                      : báo sự cố giao hàng (UC29)
 * POST action=handover                    : nộp tiền COD đang giữ cho Thu ngân (UC30)
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

        String tab = request.getParameter("tab");
        if (!"done".equals(tab) && !"cod".equals(tab)) tab = "active";
        LocalDate date = parseDateOrToday(request.getParameter("date"));

        try {
            int shipperId = shipper.getId();
            List<Delivery> active = deliveryDAO.getActiveDeliveries(shipperId);
            for (Delivery d : active) {
                d.setItems(orderDAO.getOrderDetails(d.getOrderId()));
            }
            List<Delivery> deliveredToday = deliveryDAO.getDeliveredOn(shipperId, LocalDate.now());
            List<Delivery> codHolding = deliveryDAO.getCodHolding(shipperId);
            BigDecimal codHoldingTotal = codHolding.stream()
                    .map(d -> d.getCodCollected() == null ? BigDecimal.ZERO : d.getCodCollected())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            request.setAttribute("activeDeliveries", active);
            request.setAttribute("assignedCount", active.stream().filter(Delivery::isAssigned).count());
            request.setAttribute("inProgressCount", active.stream().filter(d -> !d.isAssigned()).count());
            request.setAttribute("deliveredTodayCount", deliveredToday.size());
            request.setAttribute("codHolding", codHolding);
            request.setAttribute("codHoldingTotal", codHoldingTotal);

            if ("done".equals(tab)) {
                request.setAttribute("deliveredList", date.equals(LocalDate.now())
                        ? deliveredToday : deliveryDAO.getDeliveredOn(shipperId, date));
            } else if ("cod".equals(tab)) {
                request.setAttribute("codHistory", deliveryDAO.getCodHistory(shipperId));
                request.setAttribute("cashiers", userDAO.getStaffEmployees().stream()
                        .filter(u -> "CASHIER".equals(u.getPosition()))
                        .collect(Collectors.toList()));
            }

            int detailId = parseId(request.getParameter("id"));
            if (detailId > 0) {
                Delivery detail = deliveryDAO.getForShipper(detailId, shipperId);
                if (detail == null) {
                    request.setAttribute("deliveryErrorMsg", "Không tìm thấy đơn giao hàng hoặc đơn không được phân công cho bạn.");
                } else {
                    detail.setItems(orderDAO.getOrderDetails(detail.getOrderId()));
                    detail.setIssues(deliveryDAO.getIssues(detail.getId()));
                    request.setAttribute("detail", detail);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Failed to load deliveries", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được danh sách giao hàng. Hãy chạy lại database/aura_cafe.sql (bảng deliveries).");
            return;
        }

        request.setAttribute("tab", tab);
        request.setAttribute("selectedDate", date.toString());
        request.setAttribute("selectedDateDisplay", date.format(DISPLAY_DATE));
        request.setAttribute("todayDisplay", LocalDate.now().format(DISPLAY_DATE));
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
        int id = parseId(request.getParameter("id"));
        String back = request.getContextPath() + "/delivery"
                + ("handover".equals(action) ? "?tab=cod" : id > 0 ? "?id=" + id : "");

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
            } else {
                if (id <= 0) throw new IllegalArgumentException("Mã đơn giao hàng không hợp lệ.");
                Delivery delivery = deliveryDAO.getForShipper(id, shipperId);
                if (delivery == null) {
                    throw new IllegalArgumentException("Đơn giao hàng không tồn tại hoặc không được phân công cho bạn.");
                }
                String code = "#" + delivery.getOrderCode();
                switch (action == null ? "" : action) {
                    case "accept":
                        if (!deliveryDAO.accept(id, shipperId)) {
                            throw new IllegalStateException("Đơn " + code + " không còn ở trạng thái chờ nhận.");
                        }
                        session.setAttribute("deliverySuccessMsg", "Đã nhận đơn " + code + ".");
                        break;
                    case "pickup":
                        if (!deliveryDAO.startDelivery(id, shipperId)) {
                            throw new IllegalStateException("Chỉ lấy hàng khi đã nhận đơn và quán đã pha chế xong (Sẵn sàng).");
                        }
                        session.setAttribute("deliverySuccessMsg", "Đã lấy hàng, bắt đầu giao đơn " + code + ".");
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

    private void completeDelivery(HttpServletRequest request, HttpSession session, Delivery delivery, int shipperId)
            throws SQLException, ClassNotFoundException {
        BigDecimal collected = null;
        if (delivery.isCod()) {
            collected = parseMoney(request.getParameter("codCollected"));
        }
        String note = normalizeText(request.getParameter("deliveryNote"), false, "Ghi chú giao hàng");
        int result = deliveryDAO.completeDelivery(delivery.getId(), shipperId, collected, note);
        if (result == DeliveryDAO.COD_MISMATCH) {
            throw new IllegalArgumentException("Số tiền thu hộ phải đúng " + formatMoney(delivery.getCodAmount())
                    + ". Nếu khách không trả đủ, hãy báo sự cố thay vì xác nhận giao thành công.");
        }
        if (result != DeliveryDAO.OK) {
            throw new IllegalStateException("Chỉ xác nhận giao thành công khi đơn đang được giao.");
        }
        session.setAttribute("deliverySuccessMsg", "Đã giao thành công đơn #" + delivery.getOrderCode()
                + (delivery.isCod() ? ", thu COD " + formatMoney(collected) + "." : "."));
    }

    private void reportIssue(HttpServletRequest request, HttpSession session, Delivery delivery, int shipperId)
            throws SQLException, ClassNotFoundException {
        String type = request.getParameter("issueType");
        if (type == null || !DeliveryIssue.TYPES.containsKey(type)) {
            throw new IllegalArgumentException("Vui lòng chọn loại sự cố.");
        }
        String description = normalizeText(request.getParameter("description"), true, "Mô tả sự cố");
        if (!deliveryDAO.reportIssue(delivery.getId(), shipperId, type, description)) {
            throw new IllegalStateException("Chỉ báo sự cố cho đơn chưa giao xong.");
        }
        session.setAttribute("deliverySuccessMsg", "Đã gửi báo cáo sự cố cho đơn #" + delivery.getOrderCode()
                + ". Quản lý sẽ liên hệ xử lý.");
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
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ nhân viên giao hàng được truy cập màn hình này.");
            return null;
        }
        return user;
    }

    /** Tiền mặt thu hộ: số nguyên đồng, 0 .. 9.999.999.999 */
    private BigDecimal parseMoney(String raw) {
        try {
            BigDecimal value = new BigDecimal(raw == null ? "" : raw.trim().replaceAll("[.,\\s]", ""));
            if (value.signum() < 0 || value.compareTo(new BigDecimal("9999999999")) > 0
                    || value.stripTrailingZeros().scale() > 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền COD đã thu hợp lệ.");
        }
    }

    private String normalizeText(String raw, boolean required, String fieldName) {
        String value = raw == null ? "" : raw.trim();
        if (required && value.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập " + fieldName.toLowerCase() + ".");
        if (value.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(fieldName + " tối đa " + MAX_TEXT_LENGTH + " ký tự.");
        }
        return value;
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%,d", value == null ? 0 : value.longValue()).replace(',', '.') + "đ";
    }

    private LocalDate parseDateOrToday(String raw) {
        try {
            return raw == null || raw.isBlank() ? LocalDate.now() : LocalDate.parse(raw.trim());
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
