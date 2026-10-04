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

/**
 * FeedbackController - Trung tâm hỗ trợ & Phản ánh khách hàng (Customer Care & Service Hub)
 * Thiết kế chuẩn hóa theo bảng `customer_reviews` trong cơ sở dữ liệu `aura_cafe.sql`
 * URL: /feedback
 */
@WebServlet(name = "FeedbackController", urlPatterns = {"/feedback"})
public class FeedbackController extends HttpServlet {

    private final FeedbackDAO feedbackDAO = new FeedbackDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        List<Feedback> feedbacks;
        if (user != null) {
            feedbacks = feedbackDAO.getFeedbacksByCustomerId(user.getId());
            if (feedbacks.isEmpty()) {
                // Hiển thị các phản ánh mẫu đã có trong DB
                feedbacks = feedbackDAO.getAllFeedbacks(10);
            }
            List<String[]> userOrders = feedbackDAO.getRecentOrdersForCustomer(user.getId());
            request.setAttribute("userOrders", userOrders);
        } else {
            feedbacks = feedbackDAO.getAllFeedbacks(10);
        }

        request.setAttribute("feedbacks", feedbacks);
        request.getRequestDispatcher("/feedback.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String isAjax = request.getHeader("X-Requested-With");
        boolean ajax = "XMLHttpRequest".equalsIgnoreCase(isAjax) ||
                       (request.getContentType() != null && request.getContentType().contains("application/json")) ||
                       "1".equals(request.getParameter("ajax"));

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        String orderCode = request.getParameter("orderCode");
        String issueTypeParam = request.getParameter("issueType");
        String ratingStr = request.getParameter("rating");
        String content = request.getParameter("content");

        if (content == null || content.trim().isEmpty()) {
            if (ajax) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng nhập nội dung góp ý hoặc phản ánh của bạn!\"}");
                return;
            } else {
                request.setAttribute("errorMessage", "Vui lòng nhập nội dung góp ý hoặc phản ánh của bạn!");
                doGet(request, response);
                return;
            }
        }

        // Xác định loại vấn đề theo schema database: issue_type ENUM('RATING', 'COMPLAINT')
        String issueType = "RATING".equalsIgnoreCase(issueTypeParam) ? "RATING" : "COMPLAINT";

        int rating = 5;
        if ("RATING".equals(issueType)) {
            try {
                if (ratingStr != null && !ratingStr.trim().isEmpty()) {
                    rating = Integer.parseInt(ratingStr.trim());
                    if (rating < 1) rating = 1;
                    if (rating > 5) rating = 5;
                }
            } catch (NumberFormatException ignored) {}
        } else {
            rating = 0; // Khiếu nại (COMPLAINT) không có rating (lưu NULL theo DB chk_review_type)
        }

        // Xác định customer_id (yêu cầu NOT NULL bởi FK fk_review_customer trong aura_cafe.sql)
        int customerId;
        if (user != null) {
            customerId = user.getId();
        } else {
            // Mặc định tài khoản khách hàng mẫu trong DB
            customerId = 4;
        }

        // Xác định order_id (yêu cầu NOT NULL bởi FK fk_review_order trong aura_cafe.sql)
        int orderId = feedbackDAO.findOrderId(orderCode, customerId);

        Feedback fb = new Feedback();
        fb.setCustomerId(customerId);
        fb.setOrderId(orderId);
        fb.setOrderCode(orderCode);
        fb.setIssueType(issueType);
        fb.setRating(rating);
        fb.setContent(content.trim());
        fb.setStatus("PENDING");

        boolean saved = feedbackDAO.createFeedback(fb);

        if (ajax) {
            response.setContentType("application/json;charset=UTF-8");
            if (saved) {
                String safeContent = fb.getContent().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
                response.getWriter().write("{\"success\":true,\"ticketCode\":\"" + fb.getTicketCode() + "\",\"orderCode\":\"" + (orderCode != null ? orderCode : "") + "\",\"issueType\":\"" + fb.getIssueType() + "\",\"rating\":" + fb.getRating() + ",\"content\":\"" + safeContent + "\",\"message\":\"Đã gửi thành công! Phiếu " + fb.getTicketCode() + " đã được ghi nhận vào hệ thống.\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"success\":false,\"message\":\"Có lỗi xảy ra khi lưu vào cơ sở dữ liệu. Vui lòng thử lại!\"}");
            }
            return;
        }

        response.sendRedirect(request.getContextPath() + "/feedback?success=1");
    }
}
