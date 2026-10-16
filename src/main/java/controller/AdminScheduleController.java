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
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                int userId = Integer.parseInt(request.getParameter("userId"));
                String workDateStr = request.getParameter("workDate");
                String shiftName = request.getParameter("shiftName");
                String note = request.getParameter("note");
                String startingCashStr = request.getParameter("startingCash");

                if (workDateStr == null || shiftName == null) {
                    session.setAttribute("scheduleErrorMsg", "Vui lòng chọn ngày và ca làm việc hợp lệ!");
                    redirectToPage(request, response, returnDate, returnView);
                    return;
                }

                Date workDate = Date.valueOf(workDateStr.trim());
                WorkSchedule ws = new WorkSchedule();
                ws.setUserId(userId);
                ws.setWorkDate(workDate);
                ws.setShiftName(shiftName.trim().toUpperCase());
                ws.setStatus("SCHEDULED");
                ws.setNote(note != null ? note.trim() : "");

                if (startingCashStr != null && !startingCashStr.trim().isEmpty()) {
                    try {
                        ws.setStartingCash(Double.parseDouble(startingCashStr.replaceAll("[.,\\s]", "")));
                    } catch (Exception ignored) {
                    }
                }

                boolean ok = workScheduleDAO.addSchedule(ws);
                if (ok) {
                    session.setAttribute("scheduleSuccessMsg", "Phân ca làm việc thành công!");
                } else {
                    session.setAttribute("scheduleErrorMsg", "Nhân viên này đã được xếp vào ca " + shiftName + " trong ngày " + workDateStr + "!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                session.setAttribute("scheduleErrorMsg", "Lỗi dữ liệu: " + e.getMessage());
            }
        } else if ("update".equalsIgnoreCase(action)) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                int userId = Integer.parseInt(request.getParameter("userId"));
                String workDateStr = request.getParameter("workDate");
                String shiftName = request.getParameter("shiftName");
                String status = request.getParameter("status");
                String note = request.getParameter("note");
                String startingCashStr = request.getParameter("startingCash");
                String endingCashActualStr = request.getParameter("endingCashActual");

                Date workDate = Date.valueOf(workDateStr.trim());
                WorkSchedule ws = new WorkSchedule();
                ws.setId(id);
                ws.setUserId(userId);
                ws.setWorkDate(workDate);
                ws.setShiftName(shiftName.trim().toUpperCase());
                ws.setStatus(status != null ? status.trim().toUpperCase() : "SCHEDULED");
                ws.setNote(note != null ? note.trim() : "");

                if (startingCashStr != null && !startingCashStr.trim().isEmpty()) {
                    try {
                        ws.setStartingCash(Double.parseDouble(startingCashStr.replaceAll("[.,\\s]", "")));
                    } catch (Exception ignored) {
                    }
                }
                if (endingCashActualStr != null && !endingCashActualStr.trim().isEmpty()) {
                    try {
                        ws.setEndingCashActual(Double.parseDouble(endingCashActualStr.replaceAll("[.,\\s]", "")));
                    } catch (Exception ignored) {
                    }
                }

                boolean ok = workScheduleDAO.updateSchedule(ws);
                if (ok) {
                    session.setAttribute("scheduleSuccessMsg", "Cập nhật ca làm việc thành công!");
                } else {
                    session.setAttribute("scheduleErrorMsg", "Không thể cập nhật hoặc nhân viên bị trùng ca!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                session.setAttribute("scheduleErrorMsg", "Lỗi cập nhật: " + e.getMessage());
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
