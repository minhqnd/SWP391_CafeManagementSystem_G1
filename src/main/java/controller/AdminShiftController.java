package controller;

import dao.ShopSettingDAO;
import dao.WorkScheduleDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.ShiftStat;
import model.ShopSettings;
import model.User;

import java.io.IOException;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * AdminShiftController - Màn hình Khung giờ ca làm việc (chỉ tài khoản ADMIN)
 * GET  /admin/shifts?date=yyyy-MM-dd : 3 ca cố định (ENUM work_schedules.shift_name) kèm giờ trong shop_settings
 *                      và thống kê lượt xếp ca theo trạng thái trong tuần chứa ngày đó (Thứ 2 - Chủ nhật,
 *                      mặc định tuần hiện tại) - cùng cách tính tuần với màn Lịch phân ca
 * POST /admin/shifts : Sửa giờ 1 ca (shift, start, end) - lưu shop_settings dạng "HH:mm-HH:mm"
 */
@WebServlet(name = "AdminShiftController", urlPatterns = {"/admin/shifts"})
public class AdminShiftController extends HttpServlet {

    // Ca -> khoá giờ ca trong shop_settings / tên hiển thị
    private static final Map<String, String> SHIFT_KEYS = Map.of(
            "MORNING", ShopSettings.KEY_SHIFT_MORNING,
            "AFTERNOON", ShopSettings.KEY_SHIFT_AFTERNOON,
            "EVENING", ShopSettings.KEY_SHIFT_EVENING);
    private static final Map<String, String> SHIFT_NAMES = Map.of(
            "MORNING", "Ca sáng",
            "AFTERNOON", "Ca chiều",
            "EVENING", "Ca tối");
    private static final Pattern TIME_PATTERN = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();
    private final WorkScheduleDAO workScheduleDAO = new WorkScheduleDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) {
            return;
        }

        // Tuần đang xem (Thứ 2 - Chủ nhật) chứa ngày được chọn, giống màn Lịch phân ca
        LocalDate selectedDate = parseDateOrToday(request.getParameter("date"));
        LocalDate monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        LocalDate today = LocalDate.now();

        // Thống kê work_schedules của tuần đó bằng 1 câu GROUP BY shift_name, status.
        // Giờ ca lấy đúng theo shop_settings: ca chưa có dòng cấu hình để trống -> "Chưa cấu hình"
        Map<String, ShiftStat> shiftStats = workScheduleDAO.getShiftStats(shopSettingDAO.getSettings(),
                Date.valueOf(monday), Date.valueOf(sunday));

        request.setAttribute("shiftStats", shiftStats.values());
        request.setAttribute("selectedDate", selectedDate.toString());
        request.setAttribute("weekStartDisplay", monday.format(DISPLAY_DATE));
        request.setAttribute("weekEndDisplay", sunday.format(DISPLAY_DATE));
        request.setAttribute("prevWeekDate", monday.minusWeeks(1).toString());
        request.setAttribute("nextWeekDate", monday.plusWeeks(1).toString());
        request.setAttribute("isCurrentWeek", !today.isBefore(monday) && !today.isAfter(sunday));

        HttpSession session = request.getSession();
        CartController.moveFlashMessage(session, request, "shiftSuccessMsg");
        CartController.moveFlashMessage(session, request, "shiftErrorMsg");
        request.getRequestDispatcher("/admin-shifts.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) {
            return;
        }

        HttpSession session = request.getSession();
        try {
            String shiftName = normalizeShift(request.getParameter("shift"));
            String start = parseTime(request.getParameter("start"), "Giờ bắt đầu");
            String end = parseTime(request.getParameter("end"), "Giờ kết thúc");
            if (!LocalTime.parse(end).isAfter(LocalTime.parse(start))) {
                throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu.");
            }

            if (!shopSettingDAO.updateShiftHours(SHIFT_KEYS.get(shiftName), start + "-" + end)) {
                throw new IllegalStateException("Không lưu được giờ ca vào shop_settings.");
            }
            session.setAttribute("shiftSuccessMsg", "Đã cập nhật giờ " + SHIFT_NAMES.get(shiftName) + ".");
        } catch (IllegalArgumentException e) {
            session.setAttribute("shiftErrorMsg", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("shiftErrorMsg", "Không lưu được giờ ca. Hãy kiểm tra database aura_cafe_erp "
                    + "và xác nhận bảng shop_settings tồn tại.");
        }

        // PRG: tránh lưu lại khi F5, quay lại đúng tuần đang xem
        String returnDate = request.getParameter("returnDate");
        String url = request.getContextPath() + "/admin/shifts";
        if (returnDate != null && !returnDate.isBlank()) {
            url += "?date=" + parseDateOrToday(returnDate);
        }
        response.sendRedirect(url);
    }

    /** Ngày yyyy-MM-dd; rỗng hoặc sai định dạng thì lấy hôm nay */
    private LocalDate parseDateOrToday(String rawDate) {
        try {
            return rawDate == null || rawDate.isBlank() ? LocalDate.now() : LocalDate.parse(rawDate.trim());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    private String normalizeShift(String rawShift) {
        String shift = rawShift == null ? "" : rawShift.trim().toUpperCase(Locale.ROOT);
        if (!SHIFT_KEYS.containsKey(shift)) {
            throw new IllegalArgumentException("Ca làm việc không hợp lệ.");
        }
        return shift;
    }

    /** Chỉ nhận đúng định dạng 24h HH:mm (VD 07:00) */
    private String parseTime(String rawTime, String fieldName) {
        String time = rawTime == null ? "" : rawTime.trim();
        if (time.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập " + fieldName.toLowerCase(Locale.ROOT) + ".");
        }
        if (!TIME_PATTERN.matcher(time).matches()) {
            throw new IllegalArgumentException(fieldName + " không đúng định dạng HH:mm.");
        }
        return time;
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }

        User user = (User) session.getAttribute("user");
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ Admin có quyền truy cập trang này.");
            return false;
        }
        return true;
    }
}
