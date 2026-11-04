package controller;

import dao.ShiftHandoverDAO;
import dao.WorkScheduleDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.ShiftHandover;
import model.ShiftHandoverSummary;
import model.User;
import model.WorkSchedule;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Màn hình chốt két và bàn giao ca dành riêng cho Thu ngân (SRS 2.5 / UC60).
 * URL: /cashier/handover, dùng mode=close để mở bước kiểm đếm/chốt ca.
 * Chốt ca sẽ hoàn tất biên bản shift_handovers ngay; Admin chỉ xem lịch sử.
 */
@WebServlet(name = "CashierShiftHandoverController", urlPatterns = {"/cashier/handover"})
public class CashierShiftHandoverController extends HttpServlet {

    private static final BigDecimal MAX_CASH_AMOUNT = new BigDecimal("9999999999");
    private static final BigDecimal CASH_STEP = new BigDecimal("1000");
    private static final int MAX_NOTE_LENGTH = 500;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final WorkScheduleDAO workScheduleDAO = new WorkScheduleDAO();
    private final ShiftHandoverDAO shiftHandoverDAO = new ShiftHandoverDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User cashier = getAuthorizedCashier(request, response);
        if (cashier == null) {
            return;
        }

        String scheduleIdStr = request.getParameter("scheduleId");
        WorkSchedule activeShift = null;
        WorkSchedule completedShift = null;
        LocalDate today = LocalDate.now();

        if (scheduleIdStr != null && !scheduleIdStr.trim().isEmpty()) {
            try {
                int sId = Integer.parseInt(scheduleIdStr.trim());
                WorkSchedule s = workScheduleDAO.getScheduleById(sId);
                if (s != null && s.getUserId() == cashier.getId()) {
                    if ("COMPLETED".equalsIgnoreCase(s.getStatus())) {
                        completedShift = s;
                    } else if ("PRESENT".equalsIgnoreCase(s.getStatus())
                            && isShiftOnDate(s, today)) {
                        activeShift = s;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        List<WorkSchedule> recentShifts = workScheduleDAO.getRecentSchedulesForUser(cashier.getId(), 15);
        if (activeShift == null) {
            for (WorkSchedule s : recentShifts) {
                if ("PRESENT".equalsIgnoreCase(s.getStatus())
                        && isShiftOnDate(s, today)) {
                    activeShift = s;
                    break;
                }
            }
        }
        // Giờ ca (shiftTime) đã được WorkScheduleDAO lấy từ work_shifts của đúng tuần làm việc

        String modeParam = request.getParameter("mode");
        boolean closeMode;
        if ("close".equalsIgnoreCase(modeParam)) {
            closeMode = true;
        } else if ("handover".equalsIgnoreCase(modeParam)) {
            closeMode = false;
        } else {
            if (activeShift != null && "PRESENT".equalsIgnoreCase(activeShift.getStatus())) {
                closeMode = true;
            } else if (completedShift != null) {
                closeMode = false;
            } else {
                closeMode = true;
            }
        }

        ShiftHandoverSummary closePreview = activeShift != null
                ? workScheduleDAO.getShiftHandoverSummary(activeShift, Timestamp.valueOf(LocalDateTime.now()))
                : null;
        ShiftHandover previousHandover = null;
        BigDecimal openingCash = BigDecimal.ZERO;
        if (activeShift != null) {
            try {
                previousHandover = shiftHandoverDAO.getPreviousHandoverForSchedule(activeShift.getId());
                if (previousHandover != null && previousHandover.getActualCash() != null) {
                    openingCash = previousHandover.getActualCash();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        BigDecimal expectedCash = closePreview != null
                ? openingCash.add(closePreview.getCashSales())
                : openingCash;
        ShiftHandover handoverRecord = null;
        if (completedShift != null) {
            try {
                handoverRecord = shiftHandoverDAO.getBySchedule(completedShift.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (handoverRecord == null) {
            completedShift = null;
            for (WorkSchedule candidate : recentShifts) {
                if (!"COMPLETED".equalsIgnoreCase(candidate.getStatus())) continue;
                try {
                    ShiftHandover candidateRecord = shiftHandoverDAO.getBySchedule(candidate.getId());
                    if (candidateRecord != null) {
                        completedShift = candidate;
                        handoverRecord = candidateRecord;
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Dữ liệu tiền chỉ thuộc biên bản bàn giao; hydrate vào view model để tái sử dụng phần tổng hợp hiển thị.
        if (completedShift != null && handoverRecord != null) {
            completedShift.setStartingCash(handoverRecord.getOpeningCash().doubleValue());
            completedShift.setEndingCashSystem(handoverRecord.getExpectedCash().doubleValue());
            completedShift.setEndingCashActual(handoverRecord.getActualCash().doubleValue());
        }
        ShiftHandoverSummary handoverSummary = completedShift != null
                ? workScheduleDAO.getShiftHandoverSummary(completedShift, completedShift.getCheckOutTime())
                : null;

        WorkSchedule headerShift = closeMode ? (activeShift != null ? activeShift : completedShift)
                                            : (completedShift != null ? completedShift : activeShift);

        request.setAttribute("activeShift", activeShift);
        request.setAttribute("completedShift", completedShift);
        request.setAttribute("headerShift", headerShift);
        request.setAttribute("closePreview", closePreview);
        request.setAttribute("previousHandover", previousHandover);
        request.setAttribute("openingCash", openingCash);
        request.setAttribute("expectedCash", expectedCash);
        request.setAttribute("handoverSummary", handoverSummary);
        request.setAttribute("handoverRecord", handoverRecord);
        request.setAttribute("closeMode", closeMode);
        request.setAttribute("recentShifts", recentShifts);
        request.setAttribute("currentTime", LocalDateTime.now().format(TIME_FORMATTER));
        request.setAttribute("currentDate", LocalDate.now().format(DATE_FORMATTER));
        request.setAttribute("cashierInitials", initialsOf(cashier.getFullName()));
        moveFlashMessage(request, "handoverSuccessMsg");
        moveFlashMessage(request, "handoverErrorMsg");

        request.getRequestDispatcher("/cashier-shift-handover.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        User cashier = getAuthorizedCashier(request, response);
        if (cashier == null) {
            return;
        }

        HttpSession session = request.getSession();
        String action = request.getParameter("action");
        try {
            if (!"close-shift".equalsIgnoreCase(action)) {
                throw new IllegalArgumentException("Thao tác bàn giao ca không hợp lệ.");
            }

            int scheduleId = parsePositiveId(request.getParameter("scheduleId"));
            WorkSchedule schedule = workScheduleDAO.getScheduleById(scheduleId);
            validateOwnedActiveShift(schedule, cashier);

            ShiftHandover previousHandover =
                    shiftHandoverDAO.getPreviousHandoverForSchedule(schedule.getId());
            BigDecimal openingCash = previousHandover != null && previousHandover.getActualCash() != null
                    ? previousHandover.getActualCash()
                    : BigDecimal.ZERO;
            BigDecimal actualCash = parseCashAmount(request.getParameter("endingCashActual"));
            String note = normalizeNote(request.getParameter("note"));
            Timestamp closingTime = Timestamp.valueOf(LocalDateTime.now());
            ShiftHandoverSummary preview = workScheduleDAO.getShiftHandoverSummary(schedule, closingTime);
            BigDecimal expectedCash = openingCash.add(preview.getCashSales());
            // Lệch két (thừa / thiếu) thì bắt buộc giải trình để Admin xem lại
            if (actualCash.compareTo(expectedCash) != 0 && note.isEmpty()) {
                throw new IllegalArgumentException("Tiền két thực tế lệch so với hệ thống, vui lòng nhập ghi chú giải trình.");
            }

            int handoverId = shiftHandoverDAO.submitHandover(schedule.getId(), cashier.getId(),
                    openingCash, preview.getCashSales(), actualCash, note);
            if (handoverId == 0) {
                throw new IllegalStateException("Ca đã được chốt hoặc trạng thái ca vừa thay đổi. Vui lòng tải lại trang.");
            }

            session.setAttribute("handoverSuccessMsg", "Đã chốt két và hoàn tất bàn giao ca.");
            response.sendRedirect(request.getContextPath() + "/cashier/handover?scheduleId=" + schedule.getId() + "&mode=handover");
        } catch (IllegalArgumentException | IllegalStateException e) {
            session.setAttribute("handoverErrorMsg", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/cashier/handover?mode=close");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("handoverErrorMsg", "Không thể chốt ca do lỗi hệ thống. Vui lòng thử lại.");
            response.sendRedirect(request.getContextPath() + "/cashier/handover?mode=close");
        }
    }

    private User getAuthorizedCashier(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())
                || !"STAFF".equalsIgnoreCase(user.getRole())
                || !"CASHIER".equalsIgnoreCase(user.getPosition())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ nhân viên Thu ngân được phép truy cập màn hình bàn giao ca.");
            return null;
        }
        return user;
    }

    private void validateOwnedActiveShift(WorkSchedule schedule, User cashier) {
        if (schedule == null || schedule.getUserId() != cashier.getId()
                || !"CASHIER".equalsIgnoreCase(schedule.getUserPosition())) {
            throw new IllegalArgumentException("Ca làm việc không tồn tại hoặc không thuộc tài khoản này.");
        }
        if (!isShiftOnDate(schedule, LocalDate.now())) {
            throw new IllegalArgumentException("Chỉ có thể chốt ca làm việc trong ngày hôm nay.");
        }
        if (!"PRESENT".equalsIgnoreCase(schedule.getStatus())) {
            throw new IllegalStateException("Chỉ ca đang làm việc mới có thể chốt và bàn giao.");
        }
    }

    /** Ca thuộc ngày date; ca qua đêm (VD 23:00 - 02:00) còn tính cả sáng hôm sau để thu ngân chốt ca */
    private boolean isShiftOnDate(WorkSchedule schedule, LocalDate date) {
        if (schedule == null || schedule.getWorkDate() == null) return false;
        LocalDate workDate = schedule.getWorkDate().toLocalDate();
        return workDate.equals(date) || (schedule.isOvernightShift() && workDate.plusDays(1).equals(date));
    }

    private int parsePositiveId(String rawValue) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalArgumentException("Mã ca làm việc không hợp lệ.");
    }

    private BigDecimal parseCashAmount(String rawValue) {
        try {
            BigDecimal amount = new BigDecimal(rawValue == null ? "" : rawValue.trim());
            if (amount.compareTo(BigDecimal.ZERO) < 0
                    || amount.compareTo(MAX_CASH_AMOUNT) > 0
                    || amount.stripTrailingZeros().scale() > 0
                    || amount.remainder(CASH_STEP).compareTo(BigDecimal.ZERO) != 0) {
                throw new NumberFormatException();
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Tiền két thực tế phải từ 0 đến 9.999.999.999đ và là bội số của 1.000đ.");
        }
    }

    private String normalizeNote(String rawNote) {
        String note = rawNote == null ? "" : rawNote.trim();
        if (note.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Ghi chú bàn giao không được vượt quá 500 ký tự.");
        }
        return note;
    }

    private String initialsOf(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "TN";
        }
        String[] words = fullName.trim().split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(2, words[0].length())).toUpperCase();
        }
        return (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)).toUpperCase();
    }

    private void moveFlashMessage(HttpServletRequest request, String attributeName) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        Object message = session.getAttribute(attributeName);
        if (message != null) {
            request.setAttribute(attributeName, message);
            session.removeAttribute(attributeName);
        }
    }
}
