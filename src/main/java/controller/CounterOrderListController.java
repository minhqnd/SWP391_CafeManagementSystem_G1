package controller;

import dao.OrderDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Order;
import model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CounterOrderListController - Danh sách đơn tại quầy (Screen 12, UC20 View Counter Orders)
 * GET /counter-orders?date=yyyy-MM-dd&status=...&type=DINE_IN|TAKEAWAY
 * Barista / Thu ngân xem các đơn tại quầy (tại quán, mang đi) do Thu ngân tạo trong ngày, lọc theo trạng thái.
 * Chỉ xem - cập nhật trạng thái pha chế ở màn Pha chế (/barista), tạo / huỷ đơn ở màn Bán tại quầy (/cashier).
 */
@WebServlet(name = "CounterOrderListController", urlPatterns = {"/counter-orders"})
public class CounterOrderListController extends HttpServlet {

    private static final Set<String> STATUSES =
            Set.of("PENDING", "CONFIRMED", "PREPARING", "READY", "COMPLETED", "CANCELLED");
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrderDAO orderDAO;

    public CounterOrderListController() { this(new OrderDAO()); }

    CounterOrderListController(OrderDAO orderDAO) { this.orderDAO = orderDAO; }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return;
        }
        String position = user.getPosition() == null ? "" : user.getPosition();
        if (!"STAFF".equalsIgnoreCase(user.getRole()) || !"ACTIVE".equalsIgnoreCase(user.getStatus())
                || !("BARISTA".equalsIgnoreCase(position) || "CASHIER".equalsIgnoreCase(position))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Barista và Thu ngân được xem danh sách đơn tại quầy.");
            return;
        }

        LocalDate date;
        try {
            String raw = request.getParameter("date");
            date = raw == null || raw.isBlank() ? LocalDate.now() : LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            date = LocalDate.now();
        }
        String status = request.getParameter("status");
        if (status == null || !STATUSES.contains(status)) status = "ALL";
        String type = request.getParameter("type");
        if (!"DINE_IN".equals(type) && !"TAKEAWAY".equals(type)) type = "ALL";
        String rawQuery = request.getParameter("q");
        String query = rawQuery == null ? "" : rawQuery.trim();
        String search = query.toLowerCase(Locale.ROOT);

        List<Order> all;
        try {
            all = orderDAO.getCounterOrdersByDate(date);
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Cannot load counter orders", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không tải được danh sách đơn tại quầy.");
            return;
        }

        // Số đơn theo trạng thái (sau khi lọc hình thức) cho các tab lọc
        String typeFilter = type;
        List<Order> byType = all.stream()
                .filter(o -> "ALL".equals(typeFilter) || typeFilter.equals(o.getOrderType()))
                .filter(o -> search.isEmpty()
                        || (o.getOrderCode() != null && o.getOrderCode().toLowerCase(Locale.ROOT).contains(search))
                        || (o.getTableNumber() != null && o.getTableNumber().toLowerCase(Locale.ROOT).contains(search)))
                .collect(Collectors.toList());
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("ALL", (long) byType.size());
        for (String s : new String[]{"PENDING", "CONFIRMED", "PREPARING", "READY", "COMPLETED", "CANCELLED"}) {
            counts.put(s, byType.stream().filter(o -> s.equals(o.getStatus())).count());
        }
        String statusFilter = status;
        List<Order> shown = byType.stream()
                .filter(o -> "ALL".equals(statusFilter) || statusFilter.equals(o.getStatus()))
                .collect(Collectors.toList());

        request.setAttribute("orders", shown);
        request.setAttribute("searchQuery", query);
        request.setAttribute("statusCounts", counts);
        request.setAttribute("statusFilter", status);
        request.setAttribute("typeFilter", type);
        request.setAttribute("date", date.toString());
        request.setAttribute("dateDisplay", date.format(DISPLAY));
        request.setAttribute("isToday", date.equals(LocalDate.now()));
        request.setAttribute("isBarista", "BARISTA".equalsIgnoreCase(position));
        request.getRequestDispatcher("/counter-orders.jsp").forward(request, response);
    }
}
