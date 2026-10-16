package controller;

import dao.ShopSettingDAO;
import dao.WorkScheduleDAO;
import model.ShiftStat;
import model.ShopSettings;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

/**
 * AdminShiftController - Màn hình Admin quản lý Khung giờ ca làm việc
 * URL: /admin/shifts
 */
@WebServlet(name = "AdminShiftController", urlPatterns = {"/admin/shifts"})
public class AdminShiftController extends HttpServlet {

    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();
    private final WorkScheduleDAO workScheduleDAO = new WorkScheduleDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
        }

        ShopSettings settings = shopSettingDAO.getSettings();
        Map<String, ShiftStat> shiftStats = workScheduleDAO.getShiftStats(settings);

        request.setAttribute("shiftStats", shiftStats);
        request.setAttribute("settings", settings);

        // Flash messages
        HttpSession session = request.getSession();
        if (session.getAttribute("shiftSuccessMsg") != null) {
            request.setAttribute("shiftSuccessMsg", session.getAttribute("shiftSuccessMsg"));
            session.removeAttribute("shiftSuccessMsg");
        }
        if (session.getAttribute("shiftErrorMsg") != null) {
            request.setAttribute("shiftErrorMsg", session.getAttribute("shiftErrorMsg"));
            session.removeAttribute("shiftErrorMsg");
        }

        request.getRequestDispatcher("/admin-shifts.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) {
            return;
        }

        String action = request.getParameter("action");
        HttpSession session = request.getSession();

        if ("update_hours".equalsIgnoreCase(action)) {
            String shiftKey = request.getParameter("shiftKey");
            String startTime = request.getParameter("startTime");
            String endTime = request.getParameter("endTime");

            if (shiftKey == null || shiftKey.trim().isEmpty() ||
                startTime == null || startTime.trim().isEmpty() ||
                endTime == null || endTime.trim().isEmpty()) {
                session.setAttribute("shiftErrorMsg", "Vui lòng nhập đầy đủ giờ bắt đầu và giờ kết thúc!");
                response.sendRedirect(request.getContextPath() + "/admin/shifts");
                return;
            }

            String timeRange = startTime.trim() + "-" + endTime.trim();
            boolean ok = shopSettingDAO.updateShiftHours(shiftKey.trim(), timeRange);

            if (ok) {
                session.setAttribute("shiftSuccessMsg", "Đã cập nhật khung giờ ca (" + timeRange + ") thành công!");
            } else {
                session.setAttribute("shiftErrorMsg", "Không thể lưu khung giờ ca. Vui lòng thử lại!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/shifts");
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
