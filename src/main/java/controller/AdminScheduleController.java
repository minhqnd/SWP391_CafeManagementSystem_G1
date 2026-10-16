package controller;

import dao.ShopSettingDAO;
import dao.UserDAO;
import dao.WorkScheduleDAO;
import model.ShopSettings;
import model.User;
import model.WorkSchedule;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * AdminScheduleController - Màn hình Lịch phân ca nhân viên (Employee Schedule Screen)
 * URL: /admin/schedules
 */
@WebServlet(name = "AdminScheduleController", urlPatterns = {"/admin/schedules"})
public class AdminScheduleController extends HttpServlet {

    private final WorkScheduleDAO workScheduleDAO = new WorkScheduleDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Set<String> VALID_SHIFTS = Set.of("MORNING", "AFTERNOON", "EVENING");
    private static final Set<String> VALID_STATUSES = Set.of("SCHEDULED", "PRESENT", "ABSENT", "COMPLETED");
    private static final int MAX_NOTE_LENGTH = 500;
    private static final BigDecimal MAX_CASH_AMOUNT = new BigDecimal("9999999999");
    private static final BigDecimal CASH_STEP = new BigDecimal("1000");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
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

        // Bộ lọc bổ sung
        String shiftFilter = request.getParameter("shift");
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
        Date fromDate = "board".equalsIgnoreCase(view) ? Date.valueOf(selectedDate) : Date.valueOf(monday);
        Date toDate = "board".equalsIgnoreCase(view) ? Date.valueOf(selectedDate) : Date.valueOf(sunday);
        List<WorkSchedule> scheduleList = workScheduleDAO.getSchedules(fromDate, toDate, staffId, shiftFilter, statusFilter, positionFilter);

        // Tính toán các chỉ số KPI theo dõi ca và két tiền
        int totalShifts = scheduleList.size();
        int liveActiveCount = 0;
        int completedCount = 0;
        int absentCount = 0;
        int scheduledCount = 0;
        int cashDiscrepancyCount = 0;
        double totalCashHandover = 0.0;

        for (WorkSchedule ws : scheduleList) {
            if ("PRESENT".equalsIgnoreCase(ws.getStatus())) liveActiveCount++;
            else if ("COMPLETED".equalsIgnoreCase(ws.getStatus())) completedCount++;
            else if ("ABSENT".equalsIgnoreCase(ws.getStatus())) absentCount++;
            else if ("SCHEDULED".equalsIgnoreCase(ws.getStatus())) scheduledCount++;

            if (ws.hasCashDiscrepancy()) cashDiscrepancyCount++;
            if (ws.getEndingCashActual() != null) totalCashHandover += ws.getEndingCashActual();
            else if (ws.getStartingCash() != null) totalCashHandover += ws.getStartingCash();
        }

        // Tổ chức map theo dạng: dateStr + "_" + shiftName -> List<WorkSchedule> để render nhanh trên Calendar Grid
        Map<String, List<WorkSchedule>> calendarSlotMap = new HashMap<>();
        for (WorkSchedule ws : scheduleList) {
            String key = ws.getWorkDateFormatted() + "_" + ws.getShiftName().toUpperCase();
            calendarSlotMap.computeIfAbsent(key, k -> new ArrayList<>()).add(ws);
        }

        // Dữ liệu hỗ trợ giao diện
        ShopSettings settings = shopSettingDAO.getSettings();
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
        request.setAttribute("calendarSlotMap", calendarSlotMap);
        request.setAttribute("staffList", staffList);
        request.setAttribute("settings", settings);
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
        request.setAttribute("cashDiscrepancyCount", cashDiscrepancyCount);
        request.setAttribute("totalCashHandover", totalCashHandover);

        // Flash message
        HttpSession session = request.getSession();
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

        if ("add".equalsIgnoreCase(action)) {
            try {
                int userId = parsePositiveId(request.getParameter("userId"), "Vui lòng chọn nhân viên.");
                User staff = requireActiveStaff(userId);
                Date workDate = parseWorkDate(request.getParameter("workDate"));
                String shiftName = requireAllowedValue(request.getParameter("shiftName"), VALID_SHIFTS,
                        "Ca làm việc không hợp lệ.");
                String note = validateNote(request.getParameter("note"));
                Double startingCash = parseCashAmount(request.getParameter("startingCash"), "Tiền két đầu ca");
                validateCashierCash(staff, startingCash, null);

                if (workScheduleDAO.isShiftExists(userId, workDate, shiftName, null)) {
                    throw new IllegalArgumentException("Nhân viên đã được xếp vào ca này trong ngày đã chọn.");
                }

                WorkSchedule ws = new WorkSchedule();
                ws.setUserId(userId);
                ws.setWorkDate(workDate);
                ws.setShiftName(shiftName);
                ws.setStatus("SCHEDULED");
                ws.setNote(note);
                ws.setStartingCash(startingCash);

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
                if (workScheduleDAO.getScheduleById(id) == null) {
                    throw new IllegalArgumentException("Ca làm việc không tồn tại hoặc đã bị xóa.");
                }

                int userId = parsePositiveId(request.getParameter("userId"), "Vui lòng chọn nhân viên.");
                User staff = requireActiveStaff(userId);
                Date workDate = parseWorkDate(request.getParameter("workDate"));
                String shiftName = requireAllowedValue(request.getParameter("shiftName"), VALID_SHIFTS,
                        "Ca làm việc không hợp lệ.");
                String status = requireAllowedValue(request.getParameter("status"), VALID_STATUSES,
                        "Trạng thái ca làm việc không hợp lệ.");
                String note = validateNote(request.getParameter("note"));
                Double startingCash = parseCashAmount(request.getParameter("startingCash"), "Tiền két đầu ca");
                Double endingCashActual = parseCashAmount(request.getParameter("endingCashActual"), "Tiền két cuối ca");
                validateCashierCash(staff, startingCash, endingCashActual);

                if (workScheduleDAO.isShiftExists(userId, workDate, shiftName, id)) {
                    throw new IllegalArgumentException("Nhân viên đã được xếp vào ca này trong ngày đã chọn.");
                }

                WorkSchedule ws = new WorkSchedule();
                ws.setId(id);
                ws.setUserId(userId);
                ws.setWorkDate(workDate);
                ws.setShiftName(shiftName);
                ws.setStatus(status);
                ws.setNote(note);
                ws.setStartingCash(startingCash);
                ws.setEndingCashActual(endingCashActual);

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
        } else if ("quick_status".equalsIgnoreCase(action)) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                String status = request.getParameter("status");
                boolean ok = workScheduleDAO.updateScheduleStatus(id, status);
                if (ok) {
                    session.setAttribute("scheduleSuccessMsg", "Đã cập nhật trạng thái ca làm việc!");
                } else {
                    session.setAttribute("scheduleErrorMsg", "Không thể cập nhật trạng thái ca!");
                }
            } catch (Exception e) {
                session.setAttribute("scheduleErrorMsg", "Lỗi: " + e.getMessage());
            }
        } else if ("check_in".equalsIgnoreCase(action)) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                String startCashStr = request.getParameter("startingCash");
                Double startCash = null;
                if (startCashStr != null && !startCashStr.trim().isEmpty()) {
                    startCash = Double.parseDouble(startCashStr.replaceAll("[.,\\s]", ""));
                }
                boolean ok = workScheduleDAO.checkInStaff(id, startCash);
                if (ok) {
                    session.setAttribute("scheduleSuccessMsg", "Check-in ca trực thành công!");
                } else {
                    session.setAttribute("scheduleErrorMsg", "Không thể check-in ca trực!");
                }
            } catch (Exception e) {
                session.setAttribute("scheduleErrorMsg", "Lỗi check-in: " + e.getMessage());
            }
        } else if ("close_shift".equalsIgnoreCase(action)) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                double actualCash = Double.parseDouble(request.getParameter("endingCashActual").replaceAll("[.,\\s]", ""));
                Double systemCash = null;
                String sysCashStr = request.getParameter("endingCashSystem");
                if (sysCashStr != null && !sysCashStr.trim().isEmpty()) {
                    systemCash = Double.parseDouble(sysCashStr.replaceAll("[.,\\s]", ""));
                }
                String note = request.getParameter("note");
                boolean ok = workScheduleDAO.closeShiftAndHandoverCash(id, actualCash, systemCash, note != null ? note.trim() : "");
                if (ok) {
                    session.setAttribute("scheduleSuccessMsg", "Đã chốt ca và bàn giao két tiền thành công!");
                } else {
                    session.setAttribute("scheduleErrorMsg", "Không thể chốt ca!");
                }
            } catch (Exception e) {
                session.setAttribute("scheduleErrorMsg", "Lỗi chốt ca: " + e.getMessage());
            }
        } else if ("delete".equalsIgnoreCase(action)) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                boolean ok = workScheduleDAO.deleteSchedule(id);
                if (ok) {
                    session.setAttribute("scheduleSuccessMsg", "Đã hủy ca làm việc thành công!");
                } else {
                    session.setAttribute("scheduleErrorMsg", "Không thể xóa ca làm việc.");
                }
            } catch (Exception e) {
                session.setAttribute("scheduleErrorMsg", "Lỗi xóa ca: " + e.getMessage());
            }
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

    private String requireAllowedValue(String rawValue, Set<String> allowedValues, String errorMessage) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return normalized;
    }

    private String validateNote(String rawValue) {
        String note = rawValue == null ? "" : rawValue.trim();
        if (note.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá " + MAX_NOTE_LENGTH + " ký tự.");
        }
        return note;
    }

    private Double parseCashAmount(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.trim().isEmpty()) return null;

        try {
            BigDecimal amount = new BigDecimal(rawValue.trim());
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(fieldName + " không được là số âm.");
            }
            if (amount.compareTo(MAX_CASH_AMOUNT) > 0) {
                throw new IllegalArgumentException(fieldName + " không được vượt quá 9.999.999.999đ.");
            }
            if (amount.stripTrailingZeros().scale() > 0 || amount.remainder(CASH_STEP).compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException(fieldName + " phải là bội số của 1.000đ.");
            }
            return amount.doubleValue();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " phải là một số hợp lệ.");
        }
    }

    private void validateCashierCash(User staff, Double startingCash, Double endingCashActual) {
        boolean hasCashValue = startingCash != null || endingCashActual != null;
        if (hasCashValue && !"CASHIER".equalsIgnoreCase(staff.getPosition())) {
            throw new IllegalArgumentException("Chỉ nhân viên Thu ngân mới được nhập thông tin tiền két.");
        }
    }

    private void redirectToPage(HttpServletRequest request, HttpServletResponse response,
                                String returnDate, String returnView) throws IOException {
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
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin có quyền truy cập trang này.");
            return false;
        }
        return true;
    }
}
