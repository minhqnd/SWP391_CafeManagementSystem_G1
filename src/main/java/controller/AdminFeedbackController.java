package controller;

import dao.FeedbackDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Feedback;
import model.FeedbackStatus;
import model.User;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Set;

/**
 * AdminFeedbackController - Quản lý đánh giá và khiếu nại (bảng customer_reviews)
 * GET  /admin/feedback : Xem danh sách, bộ lọc, và thống kê 4 chỉ số KPI
 * POST /admin/feedback : Gửi / cập nhật phản hồi và trạng thái xử lý
 */
@WebServlet(name = "AdminFeedbackController", urlPatterns = {"/admin/feedback"})
public class AdminFeedbackController extends HttpServlet {

    /*
     * BF09 - Feedback Resolution (phía Admin)
     * 1. GET kiểm tra quyền Admin, tạo CSRF token, tải KPI và danh sách theo bộ lọc.
     * 2. POST kiểm tra CSRF, nội dung phản hồi và trạng thái đích.
     * 3. FeedbackDAO khóa phiếu, kiểm tra chuyển trạng thái rồi cập nhật trong transaction.
     * 4. Kết quả được lưu dưới dạng flash message và redirect về danh sách (PRG pattern).
     */
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private static final Set<String> VALID_TYPES = Set.of("ALL", "RATING", "COMPLAINT");
    private static final Set<String> VALID_STATUSES =
            Set.of("ALL", "PENDING", "PROCESSING", "RESOLVED", "REJECTED");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAdmin(request, response)) {
            return;
        }

        HttpSession session = request.getSession();
        // BF09.1: Token gắn với session bảo vệ thao tác trả lời/đóng phiếu khỏi CSRF.
        if (session.getAttribute("feedbackCsrfToken") == null) {
            session.setAttribute("feedbackCsrfToken", UUID.randomUUID().toString());
        }
        moveFlash(session, request, "feedbackMessage");
        moveFlash(session, request, "feedbackError");

        String typeFilter = normalizeFilter(request.getParameter("type"), VALID_TYPES);
        String statusFilter = normalizeFilter(request.getParameter("status"), VALID_STATUSES);

        try {
            // Lấy toàn bộ danh sách để tính thống kê 4 chỉ số KPI tổng thể
            // BF09.2: KPI luôn tính trên toàn bộ dữ liệu; bộ lọc chỉ tác động danh sách hiển thị.
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
        // BF09.3: Dừng trước khi đọc/cập nhật phiếu nếu request không thuộc form hợp lệ của session.
        if (sessionCsrf == null || paramCsrf == null || !sessionCsrf.equals(paramCsrf)) {
            session.setAttribute("feedbackError", "Phiên biểu mẫu đã hết hạn. Vui lòng thao tác lại.");
            response.sendRedirect(request.getContextPath() + "/admin/feedback");
            return;
        }

        String action = request.getParameter("action");
        try {
            if ("reply".equalsIgnoreCase(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                String adminResponse = request.getParameter("adminResponse");

                if (adminResponse == null || adminResponse.trim().isEmpty()) {
                    throw new IllegalArgumentException("Nội dung phản hồi không được để trống.");
                }
                String responseText = adminResponse.trim();
                if (responseText.length() > 2000) {
                    throw new IllegalArgumentException("Nội dung phản hồi không được vượt quá 2.000 ký tự.");
                }

                FeedbackStatus targetStatus = FeedbackStatus.from(request.getParameter("status"));
                // BF09.4: DAO quyết định kết quả UPDATED/NOT_FOUND/ALREADY_CLOSED một cách nguyên tử.
                FeedbackDAO.ResponseUpdateResult result = feedbackDAO.updateFeedbackResponse(
                        id, targetStatus, responseText);

                if (result == FeedbackDAO.ResponseUpdateResult.UPDATED) {
                    if (targetStatus == FeedbackStatus.RESOLVED) {
                        session.setAttribute("feedbackMessage",
                                "Đã gửi phản hồi và đóng phiếu #" + id + " với trạng thái Đã giải quyết.");
                    } else if (targetStatus == FeedbackStatus.REJECTED) {
                        session.setAttribute("feedbackMessage",
                                "Đã gửi phản hồi và đóng phiếu #" + id + " với trạng thái Từ chối.");
                    } else {
                        session.setAttribute("feedbackMessage",
                                "Đã gửi phản hồi cho phiếu #" + id + ". Phiếu vẫn đang được xử lý.");
                    }
                } else if (result == FeedbackDAO.ResponseUpdateResult.ALREADY_CLOSED) {
                    session.setAttribute("feedbackError",
                            "Phiếu #" + id + " đã kết thúc nên không thể chỉnh sửa hoặc mở lại.");
                } else {
                    session.setAttribute("feedbackError", "Không tìm thấy phiếu #" + id + ".");
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
            session.setAttribute("feedbackError", "Không thể lưu phản hồi. Vui lòng thử lại.");
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

    private String normalizeFilter(String rawValue, Set<String> allowedValues) {
        String value = rawValue == null || rawValue.isBlank()
                ? "ALL" : rawValue.trim().toUpperCase(Locale.ROOT);
        return allowedValues.contains(value) ? value : "ALL";
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            request.getSession(true).setAttribute("errorMsg",
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập bằng tài khoản Admin.");
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole()) || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            session.setAttribute("errorMsg",
                    "Tài khoản hiện tại không có quyền Admin hoặc đã bị khóa. Vui lòng đăng nhập lại bằng tài khoản Admin đang hoạt động.");
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        return true;
    }
}
