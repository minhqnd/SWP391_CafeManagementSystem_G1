package controller;

import dao.FeedbackDAO;
import model.Feedback;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * FeedbackController - Trung tâm hỗ trợ & Phản ánh khách hàng (Customer Care &
 * Service Hub)
 * Thiết kế chuẩn hóa theo bảng `customer_reviews` trong cơ sở dữ liệu
 * `aura_cafe.sql`
 * URL: /feedback
 */
@WebServlet(name = "FeedbackController", urlPatterns = { "/feedback" })
public class FeedbackController extends HttpServlet {

    /*
     * BF09 - Feedback Resolution (phía khách hàng)
     * 1. GET tải lịch sử feedback và các đơn hàng có thể phản hồi.
     * 2. POST xác thực đăng nhập, chuẩn hóa loại feedback/rating và kiểm tra quyền sở hữu đơn.
     * 3. FeedbackDAO tạo phiếu PENDING; controller trả JSON cho AJAX hoặc redirect cho form thường.
     * 4. Phần xử lý/đóng phiếu tiếp tục tại AdminFeedbackController.
     */
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private static final int MAX_CONTENT_LENGTH = 2000;

    @Override
    // lấy lịch sử phản hồi, đơn hàng
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        List<Feedback> feedbacks;
        if (isActiveCustomer(user)) {
            // BF09.1: Khách đã đăng nhập chỉ thấy phiếu và đơn hàng thuộc chính tài khoản của mình.
            feedbacks = feedbackDAO.getFeedbacksByCustomerId(user.getId());
            List<String[]> userOrders = feedbackDAO.getRecentOrdersForCustomer(user.getId());
            request.setAttribute("userOrders", userOrders);
            if (session.getAttribute("feedbackSubmitCsrfToken") == null) {
                session.setAttribute("feedbackSubmitCsrfToken", UUID.randomUUID().toString());
            }
        } else {
            // Lịch sử phiếu là dữ liệu riêng tư; khách chưa đăng nhập/nhân viên không được xem phiếu của người khác.
            feedbacks = List.of();
        }

        request.setAttribute("feedbacks", feedbacks);
        request.getRequestDispatcher("/feedback.jsp").forward(request, response);
    }

    @Override
    // nhận yêu cầu gửi form và kiểm tra người dùng đã đăng nhập.
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String isAjax = request.getHeader("X-Requested-With");
        boolean ajax = "XMLHttpRequest".equalsIgnoreCase(isAjax) ||
                (request.getContentType() != null && request.getContentType().contains("application/json")) ||
                "1".equals(request.getParameter("ajax"));

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (!isActiveCustomer(user)) {
            // BF09.2: Tạo feedback làm phát sinh dữ liệu khách hàng nên bắt buộc có phiên đăng nhập.
            sendError(request, response, ajax, HttpServletResponse.SC_UNAUTHORIZED,
                    "Vui lòng đăng nhập trước khi gửi phản hồi!");
            return;
        }
        String expectedCsrf = (String) session.getAttribute("feedbackSubmitCsrfToken");
        String submittedCsrf = request.getParameter("csrfToken");
        if (expectedCsrf == null || submittedCsrf == null || !expectedCsrf.equals(submittedCsrf)) {
            sendError(request, response, ajax, HttpServletResponse.SC_FORBIDDEN,
                    "Phiên biểu mẫu đã hết hạn. Vui lòng tải lại trang và thao tác lại.");
            return;
        }
        // lấy dữ liệu form, kiểm tra nội dung, loại phản hồi, số sao và xác minh đơn
        // hàng.
        String orderCode = request.getParameter("orderCode");
        String issueTypeParam = request.getParameter("issueType");
        String ratingStr = request.getParameter("rating");
        String content = request.getParameter("content");

        if (content == null || content.trim().isEmpty()) {
            if (ajax) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Vui lòng nhập nội dung góp ý hoặc phản ánh của bạn!\"}");
                return;
            } else {
                request.setAttribute("errorMessage", "Vui lòng nhập nội dung góp ý hoặc phản ánh của bạn!");
                doGet(request, response);
                return;
            }
        }
        content = content.trim();
        if (content.length() > MAX_CONTENT_LENGTH) {
            sendError(request, response, ajax, HttpServletResponse.SC_BAD_REQUEST,
                    "Nội dung phản hồi không được vượt quá 2.000 ký tự.");
            return;
        }

        // Xác định loại vấn đề theo schema database: issue_type ENUM('RATING',
        // 'COMPLAINT')
        if (!"RATING".equalsIgnoreCase(issueTypeParam)
                && !"COMPLAINT".equalsIgnoreCase(issueTypeParam)) {
            sendError(request, response, ajax, HttpServletResponse.SC_BAD_REQUEST,
                    "Loại phản hồi không hợp lệ.");
            return;
        }
        String issueType = issueTypeParam.trim().toUpperCase(java.util.Locale.ROOT);

        int rating = 0;
        if ("RATING".equals(issueType)) {
            try {
                rating = Integer.parseInt(ratingStr == null ? "" : ratingStr.trim());
                if (rating < 1 || rating > 5) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sendError(request, response, ajax, HttpServletResponse.SC_BAD_REQUEST,
                        "Số sao đánh giá phải từ 1 đến 5.");
                return;
            }
        } else {
            rating = 0; // Khiếu nại (COMPLAINT) không có rating (lưu NULL theo DB chk_review_type)
        }

        // Xác định customer_id (yêu cầu NOT NULL bởi FK fk_review_customer trong
        // aura_cafe.sql)
        int customerId = user.getId();

        // Xác định order_id (yêu cầu NOT NULL bởi FK fk_review_order trong
        // aura_cafe.sql)
        int orderId = feedbackDAO.findOrderId(orderCode, customerId);
        if (orderId <= 0) {
            // BF09.3: Không cho phản hồi đơn của người khác hoặc phản hồi lặp lại cùng một đơn.
            sendError(request, response, ajax, HttpServletResponse.SC_CONFLICT,
                    "Đơn hàng không hợp lệ hoặc đã được bạn phản hồi trước đó.");
            return;
        }
        // ạo đối tượng Feedback rồi gọi feedbackDAO.createFeedback(fb) để lưu.
        Feedback fb = new Feedback();
        fb.setCustomerId(customerId);
        fb.setOrderId(orderId);
        fb.setOrderCode(orderCode);
        fb.setIssueType(issueType);
        fb.setRating(rating);
        fb.setContent(content);
        fb.setStatus("PENDING");
        // BF09.4: Phiếu mới luôn đi vào hàng chờ PENDING để Admin tiếp nhận và xử lý.
        boolean saved = feedbackDAO.createFeedback(fb);

        if (ajax) {
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            if (saved) {
                response.getWriter().write("{\"success\":true,\"redirectUrl\":\""
                        + request.getContextPath() + "/feedback?success=1\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Có lỗi xảy ra khi lưu vào cơ sở dữ liệu. Vui lòng thử lại!\"}");
            }
            return;
        }

        if (saved) {
            response.sendRedirect(request.getContextPath() + "/feedback?success=1");
        } else {
            request.setAttribute("errorMessage", "Có lỗi xảy ra khi lưu vào cơ sở dữ liệu. Vui lòng thử lại!");
            doGet(request, response);
        }
    }

    private void sendError(HttpServletRequest request, HttpServletResponse response, boolean ajax,
            int status, String message) throws ServletException, IOException {
        if (ajax) {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
            return;
        }

        request.setAttribute("errorMessage", message);
        doGet(request, response);
    }

    private boolean isActiveCustomer(User user) {
        return user != null
                && "CUSTOMER".equalsIgnoreCase(user.getRole())
                && "ACTIVE".equalsIgnoreCase(user.getStatus());
    }
}
