package controller;

import dao.ShiftHandoverDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.ShiftHandover;
import model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

/**
 * AdminHandoverController - Xem lại biên bản bàn giao ca (SRS 2.5 / UC60, chỉ tài khoản ADMIN)
 * GET  /admin/handovers?status=SUBMITTED|CONFIRMED|REJECTED|ALL : danh sách biên bản (mặc định chờ duyệt)
 * POST action=confirm | reject : xác nhận / từ chối biên bản đang chờ duyệt (từ chối bắt buộc nêu lý do)
 * Biên bản bị từ chối sẽ quay về Thu ngân để giải trình và gửi lại.
 */
@WebServlet(name = "AdminHandoverController", urlPatterns = {"/admin/handovers"})
public class AdminHandoverController extends HttpServlet {

    private static final Set<String> STATUSES = Set.of("SUBMITTED", "CONFIRMED", "REJECTED");
    private static final int MAX_NOTE_LENGTH = 500;

    private final ShiftHandoverDAO shiftHandoverDAO = new ShiftHandoverDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        CartController.moveFlashMessage(session, request, "handoverSuccessMsg");
        CartController.moveFlashMessage(session, request, "handoverErrorMsg");
        if (session.getAttribute("handoverCsrfToken") == null) {
            session.setAttribute("handoverCsrfToken", UUID.randomUUID().toString());
        }

        String status = normalizeStatus(request.getParameter("status"));
        try {
            request.setAttribute("handovers", shiftHandoverDAO.getHandovers("ALL".equals(status) ? null : status));
            request.setAttribute("statusCounts", shiftHandoverDAO.countByStatus());
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Failed to load shift handovers", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được biên bản bàn giao. Hãy chạy lại database/aura_cafe.sql (bảng shift_handovers).");
            return;
        }
        request.setAttribute("statusFilter", status);
        request.getRequestDispatcher("/admin-handovers.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        String status = normalizeStatus(request.getParameter("status"));
        String expected = (String) session.getAttribute("handoverCsrfToken");
        if (expected == null || !expected.equals(request.getParameter("csrfToken"))) {
            session.setAttribute("handoverErrorMsg", "Phiên làm việc đã được làm mới. Vui lòng thao tác lại.");
            redirect(request, response, status);
            return;
        }

        String action = request.getParameter("action");
        try {
            boolean confirm = "confirm".equals(action);
            if (!confirm && !"reject".equals(action)) {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            int id = parseId(request.getParameter("id"));
            ShiftHandover handover = shiftHandoverDAO.getById(id);
            if (handover == null) throw new IllegalArgumentException("Không tìm thấy biên bản bàn giao.");

            String reviewNote = request.getParameter("reviewNote");
            reviewNote = reviewNote == null ? "" : reviewNote.trim();
            if (reviewNote.length() > MAX_NOTE_LENGTH) {
                throw new IllegalArgumentException("Ý kiến duyệt tối đa " + MAX_NOTE_LENGTH + " ký tự.");
            }
            if (!confirm && reviewNote.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập lý do từ chối để Thu ngân giải trình.");
            }

            User admin = (User) session.getAttribute("user");
            if (!shiftHandoverDAO.review(id, admin.getId(), confirm, reviewNote)) {
                throw new IllegalArgumentException("Biên bản đã được xử lý trước đó, vui lòng tải lại trang.");
            }
            String label = handover.getShiftName() + " ngày " + handover.getWorkDateDisplay()
                    + " (" + handover.getCashierName() + ")";
            session.setAttribute("handoverSuccessMsg",
                    (confirm ? "Đã xác nhận biên bản " : "Đã từ chối biên bản ") + label + ".");
        } catch (IllegalArgumentException e) {
            session.setAttribute("handoverErrorMsg", e.getMessage());
        } catch (SQLException | ClassNotFoundException e) {
            getServletContext().log("Shift handover review failed", e);
            session.setAttribute("handoverErrorMsg", "Không thể lưu kết quả duyệt vào database. Vui lòng thử lại.");
        }
        redirect(request, response, status);
    }

    /** Mặc định hiện biên bản chờ duyệt; ALL = tất cả */
    private String normalizeStatus(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase();
        if ("ALL".equals(value) || STATUSES.contains(value)) return value;
        return "SUBMITTED";
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, String status)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/admin/handovers?status=" + status);
    }

    private int parseId(String raw) {
        try {
            int id = Integer.parseInt(raw == null ? "" : raw.trim());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mã biên bản không hợp lệ.");
        }
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
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
