package controller;

import dao.ShopSettingDAO;
import dao.WorkScheduleDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.ShiftHandoverSummary;
import model.ShopSettings;
import model.User;
import model.WorkSchedule;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Màn hình chốt két và bàn giao ca dành riêng cho Thu ngân.
 * URL: /cashier/handover, dùng mode=close để mở bước kiểm đếm/chốt ca.
 */
@WebServlet(name = "CashierShiftHandoverController", urlPatterns = {"/cashier/handover"})
public class CashierShiftHandoverController extends HttpServlet {

    private static final BigDecimal MAX_CASH_AMOUNT = new BigDecimal("9999999999");
    private static final BigDecimal CASH_STEP = new BigDecimal("1000");
    private static final int MAX_NOTE_LENGTH = 500;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final WorkScheduleDAO workScheduleDAO = new WorkScheduleDAO();
    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User cashier = getAuthorizedCashier(request, response);
        if (cashier == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        List<WorkSchedule> schedules = workScheduleDAO.getSchedules(
                Date.valueOf(today), Date.valueOf(today), cashier.getId(), null, null, "CASHIER");

        WorkSchedule activeShift = findLatestShift(schedules, "PRESENT");
        if (activeShift == null) {
            activeShift = findLatestShift(schedules, "SCHEDULED");
        }
        WorkSchedule completedShift = findLatestHandoverShift(schedules);

        ShopSettings settings = shopSettingDAO.getSettings();
        applyShiftTime(activeShift, settings);
        applyShiftTime(completedShift, settings);

        boolean closeMode = "close".equalsIgnoreCase(request.getParameter("mode"));
        ShiftHandoverSummary closePreview = activeShift != null
                ? workScheduleDAO.getShiftHandoverSummary(activeShift, Timestamp.valueOf(LocalDateTime.now()))
                : null;
        ShiftHandoverSummary handoverSummary = completedShift != null
                ? workScheduleDAO.getShiftHandoverSummary(completedShift, completedShift.getCheckOutTime())
                : null;

        WorkSchedule headerShift = activeShift != null ? activeShift : completedShift;
        request.setAttribute("activeShift", activeShift);
        request.setAttribute("completedShift", completedShift);
        request.setAttribute("headerShift", headerShift);
        request.setAttribute("closePreview", closePreview);
        request.setAttribute("handoverSummary", handoverSummary);
        request.setAttribute("closeMode", closeMode);
        request.setAttribute("currentTime", LocalDateTime.now().format(TIME_FORMATTER));
        request.setAttribute("currentDate", today.format(DATE_FORMATTER));
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
        try {
            if (!"close-shift".equalsIgnoreCase(request.getParameter("action"))) {
                throw new IllegalArgumentException("Thao tác bàn giao ca không hợp lệ.");
            }

            int scheduleId = parsePositiveId(request.getParameter("scheduleId"));
            WorkSchedule schedule = workScheduleDAO.getScheduleById(scheduleId);
            validateOwnedActiveShift(schedule, cashier);

            BigDecimal actualCash = parseCashAmount(request.getParameter("endingCashActual"));
            String note = normalizeNote(request.getParameter("note"));
            Timestamp closingTime = Timestamp.valueOf(LocalDateTime.now());
            ShiftHandoverSummary preview = workScheduleDAO.getShiftHandoverSummary(schedule, closingTime);
            BigDecimal expectedCash = preview.getStartingCash().add(preview.getCashSales());

            boolean closed = workScheduleDAO.closeCashierShift(
                    schedule.getId(), cashier.getId(), actualCash, expectedCash, note);
            if (!closed) {
                throw new IllegalStateException("Ca đã được chốt hoặc trạng thái ca vừa thay đổi. Vui lòng tải lại trang.");
            }

            session.setAttribute("handoverSuccessMsg", "Đã chốt két và tạo biên bản bàn giao ca thành công.");
            response.sendRedirect(request.getContextPath() + "/cashier/handover");
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

    private WorkSchedule findLatestShift(List<WorkSchedule> schedules, String status) {
        for (int i = schedules.size() - 1; i >= 0; i--) {
            WorkSchedule schedule = schedules.get(i);
            if (status.equalsIgnoreCase(schedule.getStatus())) {
                return schedule;
            }
        }
        return null;
    }

    private WorkSchedule findLatestHandoverShift(List<WorkSchedule> schedules) {
        for (int i = schedules.size() - 1; i >= 0; i--) {
            WorkSchedule schedule = schedules.get(i);
            if ("COMPLETED".equalsIgnoreCase(schedule.getStatus())
                    && schedule.getEndingCashSystem() != null
                    && schedule.getEndingCashActual() != null) {
                return schedule;
            }
        }
        return null;
    }

    private void applyShiftTime(WorkSchedule schedule, ShopSettings settings) {
        if (schedule == null) {
            return;
        }
        if ("MORNING".equalsIgnoreCase(schedule.getShiftName())) {
            schedule.setShiftTime(settings.getShiftMorning());
        } else if ("AFTERNOON".equalsIgnoreCase(schedule.getShiftName())) {
            schedule.setShiftTime(settings.getShiftAfternoon());
        } else if ("EVENING".equalsIgnoreCase(schedule.getShiftName())) {
            schedule.setShiftTime(settings.getShiftEvening());
        }
    }

    private void validateOwnedActiveShift(WorkSchedule schedule, User cashier) {
        if (schedule == null || schedule.getUserId() != cashier.getId()
                || !"CASHIER".equalsIgnoreCase(schedule.getUserPosition())) {
            throw new IllegalArgumentException("Ca làm việc không tồn tại hoặc không thuộc tài khoản này.");
        }
        if (schedule.getWorkDate() == null
                || !LocalDate.now().equals(schedule.getWorkDate().toLocalDate())) {
            throw new IllegalArgumentException("Chỉ có thể chốt ca làm việc trong ngày hôm nay.");
        }
        if (!"PRESENT".equalsIgnoreCase(schedule.getStatus())) {
            throw new IllegalStateException("Chỉ ca đang làm việc mới có thể chốt và bàn giao.");
        }
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
