package controller;

import dao.UserDAO;
import dao.WorkScheduleDAO;
import dao.WorkShiftDAO;
import model.User;
import model.WorkSchedule;
import model.WorkShift;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AdminScheduleController - Màn hình Lịch phân ca nhân viên (Employee Schedule Screen)
 * URL: /admin/schedules
 */
@WebServlet(name = "AdminScheduleController", urlPatterns = {"/admin/schedules"})
public class AdminScheduleController extends HttpServlet {

    /*
     * BF06 - Schedule and Attendance, bước B (phân lịch):
     * 1. GET tải khung ca đúng tuần, nhân viên ACTIVE và lịch theo bộ lọc để dựng calendar/board/table.
     * 2. POST add/update kiểm tra nhân viên, ngày làm, ca thuộc đúng tuần và chống phân trùng.
     * 3. Lịch mới bắt đầu ở SCHEDULED; bước chấm công được xử lý tại AdminAttendanceController.
     */
    private final WorkScheduleDAO workScheduleDAO = new WorkScheduleDAO();
    private final UserDAO userDAO = new UserDAO();
    private final WorkShiftDAO workShiftDAO = new WorkShiftDAO();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_NOTE_LENGTH = 500;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
        }

        HttpSession session = request.getSession();
        if (session.getAttribute("scheduleCsrfToken") == null) {
            session.setAttribute("scheduleCsrfToken", UUID.randomUUID().toString());
        }

        // View mode: calendar (mặc định) hoặc table
        String view = request.getParameter("view");
        if (view == null || (!"calendar".equalsIgnoreCase(view) && !"table".equalsIgnoreCase(view) && !"board".equalsIgnoreCase(view))) {
            view = "calendar";
        }

        // Ngày được chọn (mặc định hôm nay)
        String dateParam = request.getParameter("date");
        LocalDate selectedDate;
        try {
            selectedDate = (dateParam != null && !dateParam.trim().isEmpty())
                    ? LocalDate.parse(dateParam.trim(), DATE_FORMATTER)
                    : LocalDate.now();
        } catch (Exception e) {
            selectedDate = LocalDate.now();
        }

        // Xác định Thứ 2 (Start of week) và Chủ Nhật (End of week)
        LocalDate monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        LocalDate prevWeekDate = monday.minusWeeks(1);
        LocalDate nextWeekDate = monday.plusWeeks(1);

        // Danh sách 7 ngày trong tuần
        List<LocalDate> weekDays = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            weekDays.add(monday.plusDays(i));
        }

        // Bộ lọc bổ sung (shift = id ca của tuần)
        Integer shiftFilter = null;
        try {
            String shiftParam = request.getParameter("shift");
            if (shiftParam != null && !shiftParam.isBlank()) shiftFilter = Integer.valueOf(shiftParam.trim());
        } catch (NumberFormatException ignored) {
        }
        String positionFilter = request.getParameter("position");
        String statusFilter = request.getParameter("status");
        String staffIdParam = request.getParameter("staffId");
        Integer staffId = null;
        if (staffIdParam != null && !staffIdParam.trim().isEmpty()) {
            try {
                staffId = Integer.parseInt(staffIdParam.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        // Lấy danh sách lịch theo tuần (hoặc ngày nếu là board view)
        // BF06.B1: Một truy vấn chung cung cấp dữ liệu cho cả ba kiểu hiển thị.
        Date fromDate = "board".equalsIgnoreCase(view) ? Date.valueOf(selectedDate) : Date.valueOf(monday);
        Date toDate = "board".equalsIgnoreCase(view) ? Date.valueOf(selectedDate) : Date.valueOf(sunday);
        List<WorkSchedule> scheduleList = workScheduleDAO.getSchedules(fromDate, toDate, staffId, shiftFilter, statusFilter, positionFilter);

        // Danh sách không áp dụng bộ lọc dùng riêng cho validation phía giao diện.
        // Nhờ đó dropdown luôn ẩn đúng nhân viên đã có lịch ở ngày + ca đang chọn,
        // kể cả khi bảng hiện tại đang lọc theo nhân viên, vị trí hoặc trạng thái.
        List<WorkSchedule> occupiedWeekSchedules = workScheduleDAO.getSchedules(
                Date.valueOf(monday), Date.valueOf(sunday), null, null, null, null);

        // Tính toán các chỉ số theo dõi trạng thái lịch làm việc
        int totalShifts = scheduleList.size();
        int liveActiveCount = 0;
        int completedCount = 0;
        int absentCount = 0;
        int scheduledCount = 0;

        for (WorkSchedule ws : scheduleList) {
            if ("PRESENT".equalsIgnoreCase(ws.getStatus())) liveActiveCount++;
            else if ("COMPLETED".equalsIgnoreCase(ws.getStatus())) completedCount++;
            else if ("ABSENT".equalsIgnoreCase(ws.getStatus())) absentCount++;
            else if ("SCHEDULED".equalsIgnoreCase(ws.getStatus())) scheduledCount++;
        }

        // Tổ chức map theo dạng: dateStr + "_" + shiftId -> List<WorkSchedule> để render nhanh trên Calendar Grid
        Map<String, List<WorkSchedule>> calendarSlotMap = new HashMap<>();
        for (WorkSchedule ws : scheduleList) {
            String key = ws.getWorkDateFormatted() + "_" + ws.getShiftId();
            calendarSlotMap.computeIfAbsent(key, k -> new ArrayList<>()).add(ws);
        }

        // Các ca của tuần đang xem (work_shifts) - mỗi tuần có bộ ca riêng
        List<WorkShift> weekShifts;
        try {
            weekShifts = workShiftDAO.getShiftsByWeek(monday);
        } catch (Exception e) {
            e.printStackTrace();
            weekShifts = new ArrayList<>();
        }
        List<User> staffList = userDAO.getStaffEmployees();

        // Gán thuộc tính vào request
        request.setAttribute("view", view);
        request.setAttribute("selectedDate", selectedDate.format(DATE_FORMATTER));
        request.setAttribute("monday", monday.format(DATE_FORMATTER));
        request.setAttribute("sunday", sunday.format(DATE_FORMATTER));
        request.setAttribute("prevWeekDate", prevWeekDate.format(DATE_FORMATTER));
        request.setAttribute("nextWeekDate", nextWeekDate.format(DATE_FORMATTER));
        request.setAttribute("weekDays", weekDays);
        request.setAttribute("scheduleList", scheduleList);
        request.setAttribute("occupiedWeekSchedules", occupiedWeekSchedules);
        request.setAttribute("calendarSlotMap", calendarSlotMap);
        request.setAttribute("staffList", staffList);
        request.setAttribute("weekShifts", weekShifts);
        request.setAttribute("weekStartLabel", monday.format(DISPLAY_FORMATTER));
        request.setAttribute("weekEndLabel", sunday.format(DISPLAY_FORMATTER));
        request.setAttribute("shiftFilter", shiftFilter != null ? shiftFilter : "ALL");
        request.setAttribute("positionFilter", positionFilter != null ? positionFilter : "ALL");
        request.setAttribute("statusFilter", statusFilter != null ? statusFilter : "ALL");
        request.setAttribute("staffIdFilter", staffId);

        // Gán KPI metrics
        request.setAttribute("totalShifts", totalShifts);
        request.setAttribute("liveActiveCount", liveActiveCount);
        request.setAttribute("completedCount", completedCount);
        request.setAttribute("absentCount", absentCount);
        request.setAttribute("scheduledCount", scheduledCount);

        // Flash message
        if (session.getAttribute("scheduleSuccessMsg") != null) {
            request.setAttribute("scheduleSuccessMsg", session.getAttribute("scheduleSuccessMsg"));
            session.removeAttribute("scheduleSuccessMsg");
        }
        if (session.getAttribute("scheduleErrorMsg") != null) {
            request.setAttribute("scheduleErrorMsg", session.getAttribute("scheduleErrorMsg"));
            session.removeAttribute("scheduleErrorMsg");
        }

        request.getRequestDispatcher("/admin-schedules.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) {
            return;
        }

        String action = request.getParameter("action");
        String returnDate = request.getParameter("returnDate");
        String returnView = request.getParameter("returnView");
        if (returnView == null || returnView.trim().isEmpty()) returnView = "calendar";

        HttpSession session = request.getSession();
        String expectedCsrf = (String) session.getAttribute("scheduleCsrfToken");
        String submittedCsrf = request.getParameter("csrfToken");
        if (expectedCsrf == null || submittedCsrf == null || !expectedCsrf.equals(submittedCsrf)) {
            session.setAttribute("scheduleErrorMsg", "Phiên biểu mẫu đã hết hạn. Vui lòng tải lại trang và thao tác lại.");
            redirectToPage(request, response, returnDate, returnView);
            return;
        }

        if ("add".equalsIgnoreCase(action)) {
            try {
                // BF06.B2: Xác thực toàn bộ quan hệ user-date-shift trước khi INSERT work_schedules.
                int userId = parsePositiveId(request.getParameter("userId"), "Vui lòng chọn nhân viên.");
                requireActiveStaff(userId);
                Date workDate = parseWorkDate(request.getParameter("workDate"));
                WorkShift shift = requireShiftOfWeek(request.getParameter("shiftId"), workDate);
                String note = validateNote(request.getParameter("note"));

                if (workScheduleDAO.isShiftExists(userId, workDate, shift.getId(), null)) {
                    throw new IllegalArgumentException("Nhân viên đã được xếp vào ca này trong ngày đã chọn.");
                }

                WorkSchedule ws = new WorkSchedule();
                ws.setUserId(userId);
                ws.setWorkDate(workDate);
                ws.setShiftId(shift.getId());
                ws.setStatus("SCHEDULED");
                ws.setNote(note);

                boolean ok = workScheduleDAO.addSchedule(ws);
                if (ok) {
                    session.setAttribute("scheduleSuccessMsg", "Phân ca làm việc thành công!");
                } else {
                    session.setAttribute("scheduleErrorMsg", "Không thể phân ca. Vui lòng kiểm tra lại dữ liệu hoặc ca bị trùng.");
                }
            } catch (IllegalArgumentException e) {
                session.setAttribute("scheduleErrorMsg", e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                session.setAttribute("scheduleErrorMsg", "Không thể phân ca do lỗi hệ thống. Vui lòng thử lại.");
            }
        } else if ("update".equalsIgnoreCase(action)) {
            try {
                int id = parsePositiveId(request.getParameter("id"), "Ca làm việc không hợp lệ.");
                WorkSchedule existing = workScheduleDAO.getScheduleById(id);
                if (existing == null) {
                    throw new IllegalArgumentException("Ca làm việc không tồn tại hoặc đã bị xóa.");
                }
                if (!"SCHEDULED".equalsIgnoreCase(existing.getStatus())) {
                    throw new IllegalArgumentException(
                            "Chỉ được đổi nhân viên/ngày/ca khi lịch còn ở trạng thái Đã xếp lịch. "
                                    + "Hãy dùng màn Chấm công để cập nhật trạng thái làm việc.");
                }

                int userId = parsePositiveId(request.getParameter("userId"), "Vui lòng chọn nhân viên.");
                requireActiveStaff(userId);
                Date workDate = parseWorkDate(request.getParameter("workDate"));
                WorkShift shift = requireShiftOfWeek(request.getParameter("shiftId"), workDate);
                String note = validateNote(request.getParameter("note"));

                if (workScheduleDAO.isShiftExists(userId, workDate, shift.getId(), id)) {
                    throw new IllegalArgumentException("Nhân viên đã được xếp vào ca này trong ngày đã chọn.");
                }

                WorkSchedule ws = new WorkSchedule();
                ws.setId(id);
                ws.setUserId(userId);
                ws.setWorkDate(workDate);
                ws.setShiftId(shift.getId());
                // Phân lịch không được tự tạo trạng thái PRESENT/COMPLETED thiếu giờ vào/ra.
                ws.setStatus(existing.getStatus());
                ws.setNote(note);

                boolean ok = workScheduleDAO.updateSchedule(ws);
                if (ok) {
                    session.setAttribute("scheduleSuccessMsg", "Cập nhật ca làm việc thành công!");
                } else {
                    session.setAttribute("scheduleErrorMsg", "Không thể cập nhật ca làm việc. Vui lòng thử lại.");
                }
            } catch (IllegalArgumentException e) {
                session.setAttribute("scheduleErrorMsg", e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                session.setAttribute("scheduleErrorMsg", "Không thể cập nhật do lỗi hệ thống. Vui lòng thử lại.");
            }
        } else if ("delete".equalsIgnoreCase(action)) {
            try {
                int id = parsePositiveId(request.getParameter("id"), "Ca làm việc không hợp lệ.");
                WorkSchedule existing = workScheduleDAO.getScheduleById(id);
                if (existing == null) {
                    throw new IllegalArgumentException("Ca làm việc không tồn tại hoặc đã bị xóa.");
                }
                if (!"SCHEDULED".equalsIgnoreCase(existing.getStatus())
                        || existing.getCheckInTime() != null || existing.getCheckOutTime() != null) {
                    throw new IllegalArgumentException(
                            "Chỉ được hủy lịch chưa bắt đầu. Lịch đã chấm công phải được lưu để đối soát.");
                }
                boolean ok = workScheduleDAO.deleteSchedule(id);
                if (ok) {
                    session.setAttribute("scheduleSuccessMsg", "Đã hủy ca làm việc thành công!");
                } else {
                    session.setAttribute("scheduleErrorMsg", "Không thể xóa ca làm việc.");
                }
            } catch (IllegalArgumentException e) {
                session.setAttribute("scheduleErrorMsg", e.getMessage());
            } catch (Exception e) {
                session.setAttribute("scheduleErrorMsg", "Không thể xóa ca làm việc do lỗi hệ thống.");
            }
        } else {
            session.setAttribute("scheduleErrorMsg", "Thao tác phân lịch không hợp lệ.");
        }

        redirectToPage(request, response, returnDate, returnView);
    }

    private int parsePositiveId(String rawValue, String errorMessage) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        try {
            int value = Integer.parseInt(rawValue.trim());
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private User requireActiveStaff(int userId) {
        User staff = userDAO.getActiveStaffById(userId);
        if (staff == null || staff.getPosition() == null) {
            throw new IllegalArgumentException("Nhân viên không tồn tại, đã ngừng hoạt động hoặc chưa có vị trí làm việc.");
        }
        return staff;
    }

    /** Ca được chọn phải thuộc đúng tuần (Thứ 2 - Chủ nhật) chứa ngày làm việc */
    private WorkShift requireShiftOfWeek(String rawShiftId, Date workDate) throws Exception {
        // BF06.B3: Không cho gắn lịch tuần này với một work_shift được định nghĩa cho tuần khác.
        int shiftId = parsePositiveId(rawShiftId, "Vui lòng chọn ca làm việc.");
        WorkShift shift = workShiftDAO.getById(shiftId);
        LocalDate weekStart = workDate.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (shift == null || !shift.getWeekStart().equals(weekStart)) {
            throw new IllegalArgumentException("Ca làm việc không thuộc tuần của ngày đã chọn. "
                    + "Hãy tạo khung ca cho tuần " + weekStart.format(DISPLAY_FORMATTER) + " ở màn Khung giờ ca.");
        }
        return shift;
    }

    private Date parseWorkDate(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ngày làm việc.");
        }
        try {
            return Date.valueOf(LocalDate.parse(rawValue.trim(), DATE_FORMATTER));
        } catch (Exception e) {
            throw new IllegalArgumentException("Ngày làm việc không đúng định dạng.");
        }
    }

    private String validateNote(String rawValue) {
        String note = rawValue == null ? "" : rawValue.trim();
        if (note.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá " + MAX_NOTE_LENGTH + " ký tự.");
        }
        return note;
    }

    private void redirectToPage(HttpServletRequest request, HttpServletResponse response,
                                String returnDate, String returnView) throws IOException {
        if (!"calendar".equalsIgnoreCase(returnView)
                && !"table".equalsIgnoreCase(returnView)
                && !"board".equalsIgnoreCase(returnView)) {
            returnView = "calendar";
        }
        try {
            returnDate = LocalDate.parse(returnDate, DATE_FORMATTER).format(DATE_FORMATTER);
        } catch (Exception e) {
            returnDate = LocalDate.now().format(DATE_FORMATTER);
        }
        StringBuilder redirectUrl = new StringBuilder(request.getContextPath() + "/admin/schedules?");
        if (returnView != null && !returnView.isEmpty()) {
            redirectUrl.append("view=").append(returnView).append("&");
        }
        if (returnDate != null && !returnDate.isEmpty()) {
            redirectUrl.append("date=").append(returnDate);
        }
        response.sendRedirect(redirectUrl.toString());
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        User user = (User) session.getAttribute("user");
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
