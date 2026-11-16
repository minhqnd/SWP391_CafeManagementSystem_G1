package controller;

import dao.WorkShiftDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import model.WorkShift;

import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * AdminShiftController - Màn hình Khung giờ ca làm việc theo tuần (UC55, chỉ tài khoản ADMIN)
 * GET  /admin/shifts?date=yyyy-MM-dd : các ca của tuần chứa ngày đó (Thứ 2 - Chủ nhật, mặc định tuần hiện tại)
 * POST action=add | update | delete : thêm (nút +) / sửa / xoá 1 ca của tuần đang chọn
 * POST action=copy : sao chép các ca từ tuần gần nhất đã có khung ca sang tuần đang chọn (đang trống)
 * Mỗi tuần lưu riêng trong work_shifts: sửa tuần này không làm thay đổi giờ ca của các tuần khác.
 * Tuần đã qua chỉ được xem. Không thống kê có mặt / hoàn thành (thuộc phần tính lương).
 */
@WebServlet(name = "AdminShiftController", urlPatterns = {"/admin/shifts"})
public class AdminShiftController extends HttpServlet {

    /*
     * BF06 - Schedule and Attendance, bước A (định nghĩa khung ca):
     * Admin tạo bộ work_shifts riêng cho từng tuần, kiểm tra trùng tên/chồng giờ rồi mới lưu.
     * Các ca này là dữ liệu đầu vào bắt buộc cho bước phân lịch tại AdminScheduleController.
     */
    private static final Pattern TIME_PATTERN = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_NAME_LENGTH = 50;

    private final WorkShiftDAO workShiftDAO = new WorkShiftDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        CartController.moveFlashMessage(session, request, "shiftSuccessMsg");
        CartController.moveFlashMessage(session, request, "shiftErrorMsg");
        showPage(request, response, weekStartOf(parseDateOrToday(request.getParameter("date"))));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        LocalDate weekStart = weekStartOf(parseDateOrToday(request.getParameter("weekStart")));
        String expected = (String) session.getAttribute("shiftCsrfToken");
        if (expected == null || !expected.equals(request.getParameter("csrfToken"))) {
            session.setAttribute("shiftErrorMsg", "Phiên làm việc đã được làm mới. Vui lòng thao tác lại.");
            redirectToWeek(request, response, weekStart);
            return;
        }

        String action = request.getParameter("action");
        Map<String, String> form = readForm(request);
        try {
            if (isPastWeek(weekStart)) {
                throw new IllegalArgumentException("Tuần " + weekStart.format(DISPLAY_DATE)
                        + " đã qua, không thể thay đổi khung ca (giữ nguyên giờ ca đã dùng).");
            }
            if ("add".equals(action) || "update".equals(action)) {
                save(weekStart, form, "update".equals(action), session);
            } else if ("delete".equals(action)) {
                delete(weekStart, request.getParameter("id"), session);
            } else if ("copy".equals(action)) {
                copyFromLatestWeek(weekStart, session);
            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
        } catch (IllegalArgumentException e) {
            if ("add".equals(action) || "update".equals(action)) {
                // Mở lại form với dữ liệu đã nhập
                request.setAttribute("shiftErrorMsg", e.getMessage());
                request.setAttribute("shiftForm", form);
                showPage(request, response, weekStart);
                return;
            }
            session.setAttribute("shiftErrorMsg", e.getMessage());
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Work shift operation failed", e);
            session.setAttribute("shiftErrorMsg", "Không thể lưu khung ca vào database. Vui lòng thử lại.");
        }
        redirectToWeek(request, response, weekStart);
    }

    private void save(LocalDate weekStart, Map<String, String> form, boolean isUpdate, HttpSession session)
            throws SQLException, ClassNotFoundException {
        // BF06.A1: Validate ca và bảo đảm thao tác update chỉ tác động đúng tuần đang xem.
        int id = 0;
        WorkShift existing = null;
        if (isUpdate) {
            id = parseId(form.get("id"));
            existing = workShiftDAO.getById(id);
            if (existing == null || !existing.getWeekStart().equals(weekStart)) {
                throw new IllegalArgumentException("Không tìm thấy ca cần sửa trong tuần này.");
            }
        }

        String name = form.get("shiftName");
        if (name.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập tên ca.");
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Tên ca tối đa " + MAX_NAME_LENGTH + " ký tự.");
        }
        LocalTime start = parseTime(form.get("startTime"), "Giờ bắt đầu");
        LocalTime end = parseTime(form.get("endTime"), "Giờ kết thúc");
        // Giờ kết thúc nhỏ hơn giờ bắt đầu = ca qua đêm, kết thúc sáng hôm sau (VD 23:00 - 02:00)
        if (end.equals(start)) throw new IllegalArgumentException("Giờ kết thúc không được trùng giờ bắt đầu.");
        if (existing != null && existing.getScheduleCount() > 0
                && (!existing.getStartTime().equals(start) || !existing.getEndTime().equals(end))) {
            throw new IllegalArgumentException(
                    "Ca đã có nhân viên được xếp lịch nên không thể đổi giờ. "
                            + "Hãy gỡ các lịch liên quan trước hoặc chỉ đổi tên ca.");
        }

        if (workShiftDAO.nameExists(weekStart, name, id)) {
            throw new IllegalArgumentException("Tuần này đã có ca tên \"" + name + "\".");
        }
        WorkShift overlap = workShiftDAO.findOverlap(weekStart, start, end, id);
        // BF06.A2: Chặn chồng giờ, kể cả phần sau nửa đêm của ca qua đêm.
        if (overlap != null) {
            throw new IllegalArgumentException("Bị chồng giờ với " + overlap.getLabel() + ".");
        }

        WorkShift shift = new WorkShift(id, weekStart, name, start, end);
        if (isUpdate) {
            workShiftDAO.update(shift);
            session.setAttribute("shiftSuccessMsg", "Đã cập nhật " + shift.getLabel() + ".");
        } else {
            workShiftDAO.insert(shift);
            session.setAttribute("shiftSuccessMsg", "Đã thêm " + shift.getLabel() + ".");
        }
    }

    private void delete(LocalDate weekStart, String rawId, HttpSession session)
            throws SQLException, ClassNotFoundException {
        int id = parseId(rawId);
        WorkShift shift = workShiftDAO.getById(id);
        if (shift == null || !shift.getWeekStart().equals(weekStart)) {
            throw new IllegalArgumentException("Không tìm thấy ca cần xoá trong tuần này.");
        }
        if (!workShiftDAO.deleteIfUnused(id)) {
            throw new IllegalArgumentException(shift.getShiftName()
                    + " đã có nhân viên được xếp lịch. Hãy gỡ lịch ở màn Lịch phân ca trước khi xoá ca.");
        }
        session.setAttribute("shiftSuccessMsg", "Đã xoá " + shift.getLabel() + ".");
    }

    private void copyFromLatestWeek(LocalDate weekStart, HttpSession session)
            throws SQLException, ClassNotFoundException {
        if (!workShiftDAO.getShiftsByWeek(weekStart).isEmpty()) {
            throw new IllegalArgumentException("Tuần này đã có khung ca, chỉ sao chép được vào tuần đang trống.");
        }
        LocalDate source = workShiftDAO.findLatestWeekBefore(weekStart);
        if (source == null) throw new IllegalArgumentException("Chưa có tuần nào trước đó để sao chép.");
        int copied = workShiftDAO.copyWeek(source, weekStart);
        session.setAttribute("shiftSuccessMsg", "Đã sao chép " + copied + " ca từ tuần "
                + source.format(DISPLAY_DATE) + ". Có thể sửa giờ riêng cho tuần này.");
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response, LocalDate weekStart)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (session.getAttribute("shiftCsrfToken") == null) {
            session.setAttribute("shiftCsrfToken", UUID.randomUUID().toString());
        }
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate currentWeek = weekStartOf(LocalDate.now());
        try {
            request.setAttribute("shifts", workShiftDAO.getShiftsByWeek(weekStart));
            LocalDate copySource = workShiftDAO.findLatestWeekBefore(weekStart);
            request.setAttribute("copySourceDisplay", copySource == null ? null : copySource.format(DISPLAY_DATE));
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Failed to load work shifts", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được khung ca. Hãy chạy lại database/aura_cafe.sql (bảng work_shifts).");
            return;
        }
        request.setAttribute("weekStart", weekStart.toString());
        request.setAttribute("weekStartDisplay", weekStart.format(DISPLAY_DATE));
        request.setAttribute("weekEndDisplay", weekEnd.format(DISPLAY_DATE));
        request.setAttribute("prevWeekDate", weekStart.minusWeeks(1).toString());
        request.setAttribute("nextWeekDate", weekStart.plusWeeks(1).toString());
        request.setAttribute("isCurrentWeek", weekStart.equals(currentWeek));
        request.setAttribute("isPastWeek", weekStart.isBefore(currentWeek));
        request.getRequestDispatcher("/admin-shifts.jsp").forward(request, response);
    }

    private Map<String, String> readForm(HttpServletRequest request) {
        Map<String, String> form = new LinkedHashMap<>();
        for (String field : new String[]{"id", "shiftName", "startTime", "endTime"}) {
            String value = request.getParameter(field);
            form.put(field, value == null ? "" : value.trim());
        }
        return form;
    }

    private void redirectToWeek(HttpServletRequest request, HttpServletResponse response, LocalDate weekStart)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/admin/shifts?date=" + weekStart);
    }

    private boolean isPastWeek(LocalDate weekStart) {
        return weekStart.isBefore(weekStartOf(LocalDate.now()));
    }

    private LocalDate weekStartOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Ngày yyyy-MM-dd; rỗng hoặc sai định dạng thì lấy hôm nay */
    private LocalDate parseDateOrToday(String rawDate) {
        try {
            return rawDate == null || rawDate.isBlank() ? LocalDate.now() : LocalDate.parse(rawDate.trim());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    /** Chỉ nhận đúng định dạng 24h HH:mm (VD 07:00) */
    private LocalTime parseTime(String raw, String fieldName) {
        if (raw == null || raw.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập " + fieldName.toLowerCase() + ".");
        if (!TIME_PATTERN.matcher(raw).matches()) {
            throw new IllegalArgumentException(fieldName + " không đúng định dạng HH:mm.");
        }
        return LocalTime.parse(raw);
    }

    private int parseId(String raw) {
        try {
            int id = Integer.parseInt(raw == null ? "" : raw.trim());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mã ca không hợp lệ.");
        }
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole()) || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin có quyền truy cập trang này.");
            return false;
        }
        return true;
    }
}
