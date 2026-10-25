package controller;

import dao.ReportDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * AdminReportsController - Màn Báo cáo (UC66 View Business Reports, chỉ ADMIN)
 * GET /admin/reports?range=today|7d|30d|month|custom&from=yyyy-MM-dd&to=yyyy-MM-dd
 * Mặc định 30 ngày gần nhất. Khoảng tự chọn: ngày bắt đầu không sau ngày kết thúc, tối đa 366 ngày.
 */
@WebServlet(name = "AdminReportsController", urlPatterns = {"/admin/reports"})
public class AdminReportsController extends HttpServlet {

    private static final int MAX_RANGE_DAYS = 366;
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReportDAO reportDAO = new ReportDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!ScaffoldAccess.requireAdmin(request, response)) return;

        LocalDate today = LocalDate.now();
        String range = request.getParameter("range");
        LocalDate from;
        LocalDate to = today;
        if ("today".equals(range)) {
            from = today;
        } else if ("7d".equals(range)) {
            from = today.minusDays(6);
        } else if ("month".equals(range)) {
            from = today.withDayOfMonth(1);
        } else if ("custom".equals(range)) {
            try {
                from = LocalDate.parse(request.getParameter("from"));
                to = LocalDate.parse(request.getParameter("to"));
                if (from.isAfter(to)) {
                    throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
                }
                if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
                    throw new IllegalArgumentException("Chỉ xem tối đa " + MAX_RANGE_DAYS + " ngày mỗi lần.");
                }
            } catch (DateTimeParseException | NullPointerException | IllegalArgumentException e) {
                request.setAttribute("reportError", e instanceof IllegalArgumentException
                        ? e.getMessage() : "Vui lòng chọn đủ ngày bắt đầu và ngày kết thúc hợp lệ.");
                range = "30d";
                from = today.minusDays(29);
                to = today;
            }
        } else {
            range = "30d";
            from = today.minusDays(29);
        }

        try {
            request.setAttribute("report", reportDAO.loadDashboard(from, to));
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Failed to load reports", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không tải được số liệu báo cáo.");
            return;
        }
        request.setAttribute("range", range);
        request.setAttribute("from", from.toString());
        request.setAttribute("to", to.toString());
        request.setAttribute("fromDisplay", from.format(DISPLAY));
        request.setAttribute("toDisplay", to.format(DISPLAY));
        request.getRequestDispatcher("/admin-reports.jsp").forward(request, response);
    }
}
