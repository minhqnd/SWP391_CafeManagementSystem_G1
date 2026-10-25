package controller;

import dao.FeedbackDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Feedback;
import model.User;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * AdminFeedbackController - Quản lý đánh giá và khiếu nại (bảng customer_reviews)
 * GET  /admin/feedback : Xem danh sách, bộ lọc, và thống kê 4 chỉ số KPI
 * POST /admin/feedback : Gửi / cập nhật phản hồi và trạng thái xử lý
 */
@WebServlet(name = "AdminFeedbackController", urlPatterns = {"/admin/feedback"})
public class AdminFeedbackController extends HttpServlet {

    private final FeedbackDAO feedbackDAO = new FeedbackDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAdmin(request, response)) {
            return;
        }

        HttpSession session = request.getSession();
        if (session.getAttribute("feedbackCsrfToken") == null) {
            session.setAttribute("feedbackCsrfToken", UUID.randomUUID().toString());
        }
        moveFlash(session, request, "feedbackMessage");
        moveFlash(session, request, "feedbackError");

        String typeFilter = request.getParameter("type");
        String statusFilter = request.getParameter("status");

        try {
            // Lấy toàn bộ danh sách để tính thống kê 4 chỉ số KPI tổng thể
            List<Feedback> allFeedbacks = feedbackDAO.getAdminFeedbacks(null, null);

            long totalCount = allFeedbacks.size();
            long pendingCount = allFeedbacks.stream()
                    .filter(f -> "PENDING".equalsIgnoreCase(f.getStatus()) || "PROCESSING".equalsIgnoreCase(f.getStatus()))
                    .count();
            long complaintCount = allFeedbacks.stream()
                    .filter(f -> "COMPLAINT".equalsIgnoreCase(f.getIssueType()))
                    .count();

            double avgRating = allFeedbacks.stream()
                    .filter(f -> "RATING".equalsIgnoreCase(f.getIssueType()) && f.getRating() > 0)
                    .mapToInt(Feedback::getRating)
                    .average()
                    .orElse(0.0);

            // Lọc danh sách theo filter nếu có
            List<Feedback> displayList;
            if ((typeFilter != null && !typeFilter.isBlank() && !"ALL".equalsIgnoreCase(typeFilter)) ||
                (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter))) {
                displayList = feedbackDAO.getAdminFeedbacks(typeFilter, statusFilter);
            } else {
                displayList = allFeedbacks;
            }

            request.setAttribute("feedbacks", displayList);
            request.setAttribute("feedbackTotal", totalCount);
            request.setAttribute("pendingTotal", pendingCount);
            request.setAttribute("complaintTotal", complaintCount);
            request.setAttribute("avgRatingFormatted", avgRating > 0 ? String.format(Locale.US, "%.1f", avgRating) : "--");
            request.setAttribute("selectedType", typeFilter != null ? typeFilter : "ALL");
            request.setAttribute("selectedStatus", statusFilter != null ? statusFilter : "ALL");

            request.getRequestDispatcher("/admin-feedback.jsp").forward(request, response);
        } catch (Exception e) {
            getServletContext().log("Failed to load admin feedbacks", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không tải được danh sách đánh giá & khiếu nại.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) {
            return;
        }

        HttpSession session = request.getSession();
        String sessionCsrf = (String) session.getAttribute("feedbackCsrfToken");
        String paramCsrf = request.getParameter("csrfToken");
        if (sessionCsrf != null && paramCsrf != null && !sessionCsrf.equals(paramCsrf)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên làm việc đã hết hạn.");
            return;
        }

        String action = request.getParameter("action");
        try {
            if ("reply".equalsIgnoreCase(action) || action == null) {
                int id = Integer.parseInt(request.getParameter("id"));
                String status = request.getParameter("status");
                String adminResponse = request.getParameter("adminResponse");

                if (adminResponse == null || adminResponse.trim().isEmpty()) {
                    throw new IllegalArgumentException("Nội dung phản hồi không được để trống.");
                }

                if (status == null || (!status.equals("PENDING") && !status.equals("PROCESSING") && !status.equals("RESOLVED") && !status.equals("REJECTED"))) {
                    status = "RESOLVED";
                }

                boolean success = feedbackDAO.updateFeedbackResponse(id, status, adminResponse.trim());
                if (success) {
                    session.setAttribute("feedbackMessage", "Đã gửi phản hồi thành công cho phiếu #" + id + ".");
                } else {
                    session.setAttribute("feedbackError", "Không tìm thấy phiếu hoặc cập nhật không thành công.");
                }
            } else {
                session.setAttribute("feedbackError", "Hành động không hợp lệ.");
            }
        } catch (NumberFormatException e) {
            session.setAttribute("feedbackError", "Mã phiếu không hợp lệ.");
        } catch (IllegalArgumentException e) {
            session.setAttribute("feedbackError", e.getMessage());
        } catch (Exception e) {
            getServletContext().log("Failed to update feedback response", e);
            session.setAttribute("feedbackError", "Lỗi cập nhật: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/admin/feedback");
    }

    private void moveFlash(HttpSession session, HttpServletRequest request, String key) {
        Object value = session.getAttribute(key);
        if (value != null) {
            request.setAttribute(key, value);
            session.removeAttribute(key);
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
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (Admin) mới có quyền truy cập trang này.");
            return false;
        }
        return true;
    }
}
