package controller;

import dao.WorkScheduleDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import model.WorkSchedule;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Màn hình quản lý chấm công dựa trên dữ liệu bảng work_schedules.
 * URL: /admin/attendance
 */
@WebServlet(name = "AdminAttendanceController", urlPatterns = {"/admin/attendance"})
public class AdminAttendanceController extends HttpServlet {

    /*
     * BF06 - Schedule and Attendance, bước C (chấm công):
     * 1. GET tải lịch theo ngày, tính KPI và đánh dấu đi muộn so với shift_start.
     * 2. POST đổi giờ HH:mm thành LocalDateTime đúng ngày làm (hỗ trợ ca qua đêm), rồi validate trạng thái.
     * 3. WorkScheduleDAO lưu check-in/check-out/status/note; controller redirect về đúng ngày đang xem.
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> VALID_STATUSES =
            Set.of("SCHEDULED", "PRESENT", "ABSENT", "COMPLETED");
    private static final int MAX_NOTE_LENGTH = 500;

    private final WorkScheduleDAO workScheduleDAO = new WorkScheduleDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) {
            return;
        }

        LocalDate selectedDate = parseDateOrDefault(request.getParameter("date"), LocalDate.now());
        Date sqlDate = Date.valueOf(selectedDate);
        // Mỗi lịch đã mang sẵn giờ ca của đúng tuần đó (work_shifts) - tính đi muộn theo giờ bắt đầu ca
        List<WorkSchedule> attendanceList =
                workScheduleDAO.getSchedules(sqlDate, sqlDate, null, null, null, null);

        int presentCompletedCount = 0;
        int lateCount = 0;
        int absentCount = 0;

        for (WorkSchedule schedule : attendanceList) {
            // BF06.C1: Thông tin đi muộn là dữ liệu suy diễn để hiển thị, không lưu thành cột riêng.
            applyLateInformation(schedule);

            if ("PRESENT".equalsIgnoreCase(schedule.getStatus())
                    || "COMPLETED".equalsIgnoreCase(schedule.getStatus())) {
                presentCompletedCount++;
            }
            if (schedule.isLate()) {
                lateCount++;
            }
            if ("ABSENT".equalsIgnoreCase(schedule.getStatus())) {
                absentCount++;
            }
        }

        request.setAttribute("selectedDate", selectedDate.format(DATE_FORMATTER));
        request.setAttribute("selectedDateDisplay", selectedDate.format(DATE_DISPLAY_FORMATTER));
        request.setAttribute("attendanceList", attendanceList);
        request.setAttribute("totalShiftCount", attendanceList.size());
        request.setAttribute("presentCompletedCount", presentCompletedCount);
        request.setAttribute("lateCount", lateCount);
        request.setAttribute("absentCount", absentCount);

        moveFlashMessage(request, "attendanceSuccessMsg");
        moveFlashMessage(request, "attendanceErrorMsg");
        request.getRequestDispatcher("/admin-attendance.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) {
            return;
        }

        HttpSession session = request.getSession();
        LocalDate redirectDate = parseDateOrDefault(request.getParameter("returnDate"), LocalDate.now());

        try {
            if (!"update".equalsIgnoreCase(request.getParameter("action"))) {
                throw new IllegalArgumentException("Thao tác chấm công không hợp lệ.");
            }

            int scheduleId = parsePositiveId(request.getParameter("id"));
            WorkSchedule schedule = workScheduleDAO.getScheduleById(scheduleId);
            if (schedule == null) {
                throw new IllegalArgumentException("Ca làm việc không tồn tại hoặc đã bị xóa.");
            }
            redirectDate = schedule.getWorkDate().toLocalDate();

            String status = normalizeStatus(request.getParameter("status"));
            String note = normalizeNote(request.getParameter("note"));
            LocalTime checkIn = parseOptionalTime(request.getParameter("checkInTime"), "Giờ vào");
            LocalTime checkOut = parseOptionalTime(request.getParameter("checkOutTime"), "Giờ ra");
            // Ca qua đêm (VD 23:00 - 02:00): giờ ra 01:50 được hiểu là sáng hôm sau
            LocalDateTime checkInAt = checkIn == null ? null : schedule.atShiftTime(checkIn);
            LocalDateTime checkOutAt = checkOut == null ? null : schedule.atShiftTime(checkOut);
            // BF06.C2: Trạng thái quyết định cặp thời gian hợp lệ trước khi dữ liệu chạm DB.
            validateAttendance(status, checkInAt, checkOutAt);

            Timestamp checkInTimestamp = checkInAt == null ? null : Timestamp.valueOf(checkInAt);
            Timestamp checkOutTimestamp = checkOutAt == null ? null : Timestamp.valueOf(checkOutAt);

            boolean updated = workScheduleDAO.updateAttendance(
                    scheduleId, checkInTimestamp, checkOutTimestamp, status, note);
            if (!updated) {
                throw new IllegalStateException("Không thể lưu dữ liệu chấm công.");
            }
            session.setAttribute("attendanceSuccessMsg", "Đã cập nhật chấm công cho "
                    + schedule.getUserName() + ".");
        } catch (IllegalArgumentException e) {
            session.setAttribute("attendanceErrorMsg", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("attendanceErrorMsg",
                    "Không thể cập nhật chấm công do lỗi hệ thống. Vui lòng thử lại.");
        }

        response.sendRedirect(request.getContextPath() + "/admin/attendance?date="
                + redirectDate.format(DATE_FORMATTER));
    }

    /** Đi muộn = giờ check-in sau giờ bắt đầu ca (giờ ca của đúng tuần chứa ngày làm việc) */
    private void applyLateInformation(WorkSchedule schedule) {
        schedule.setLate(false);
        schedule.setLateMinutes(0);
        LocalTime shiftStart = schedule.getShiftStart();
        if (schedule.getCheckInTime() == null || shiftStart == null) {
            return;
        }

        // So theo ngày + giờ để ca qua đêm cũng đúng (VD ca 23:00, vào 00:10 hôm sau = muộn 70 phút)
        LocalDateTime shiftStartAt = LocalDateTime.of(schedule.getWorkDate().toLocalDate(), shiftStart);
        long lateMinutes = Duration.between(shiftStartAt, schedule.getCheckInTime().toLocalDateTime()).toMinutes();
        if (lateMinutes > 0) {
            schedule.setLate(true);
            schedule.setLateMinutes((int) Math.min(lateMinutes, Integer.MAX_VALUE));
        }
    }

    private void validateAttendance(String status, LocalDateTime checkIn, LocalDateTime checkOut) {
        if ("SCHEDULED".equals(status) || "ABSENT".equals(status)) {
            if (checkIn != null || checkOut != null) {
                throw new IllegalArgumentException(
                        "Ca đã xếp lịch hoặc vắng mặt không được có giờ vào/ra.");
            }
            return;
        }

        if (checkIn == null) {
            throw new IllegalArgumentException("Vui lòng nhập giờ vào.");
        }

        if ("PRESENT".equals(status)) {
            if (checkOut != null) {
                throw new IllegalArgumentException("Nhân viên đang có mặt chưa được nhập giờ ra.");
            }
            return;
        }

        if (checkOut == null) {
            throw new IllegalArgumentException("Vui lòng nhập giờ ra khi ca đã hoàn thành.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Giờ ra phải sau giờ vào.");
        }
    }

    private String normalizeStatus(String rawStatus) {
        String status = rawStatus == null ? "" : rawStatus.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Trạng thái chấm công không hợp lệ.");
        }
        return status;
    }

    private String normalizeNote(String rawNote) {
        String note = rawNote == null ? "" : rawNote.trim();
        if (note.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 500 ký tự.");
        }
        return note;
    }

    private LocalTime parseOptionalTime(String rawTime, String fieldName) {
        if (rawTime == null || rawTime.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(rawTime.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " không đúng định dạng HH:mm.");
        }
    }


    private int parsePositiveId(String rawId) {
        try {
            int id = Integer.parseInt(rawId == null ? "" : rawId.trim());
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ca làm việc không hợp lệ.");
        }
    }

    private LocalDate parseDateOrDefault(String rawDate, LocalDate fallback) {
        try {
            return rawDate == null || rawDate.trim().isEmpty()
                    ? fallback : LocalDate.parse(rawDate.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private void moveFlashMessage(HttpServletRequest request, String attributeName) {
        HttpSession session = request.getSession();
        Object message = session.getAttribute(attributeName);
        if (message != null) {
            request.setAttribute(attributeName, message);
            session.removeAttribute(attributeName);
        }
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
