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
import java.util.Random;

/**
 * FeedbackController - Trung tâm hỗ trợ & Phản ánh khách hàng (Customer Care & Service Hub)
 * URL: /feedback
 */
@WebServlet(name = "FeedbackController", urlPatterns = {"/feedback"})
public class FeedbackController extends HttpServlet {

    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final Random random = new Random();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        List<Feedback> feedbacks;
        if (user != null) {
            feedbacks = feedbackDAO.getFeedbacksByCustomerId(user.getId());
            if (feedbacks.isEmpty()) {
                // Hiển thị các phản ánh mẫu để giao diện trực quan
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
        String category = request.getParameter("category");
        String ratingStr = request.getParameter("rating");
        String content = request.getParameter("content");
        String images = request.getParameter("images");
        String contactMethod = request.getParameter("contactMethod");

        if (orderCode == null || orderCode.trim().isEmpty()) {
            orderCode = "#AUR-86410";
        }
        if (category == null || category.trim().isEmpty()) {
            category = "other";
        }
        if (contactMethod == null || contactMethod.trim().isEmpty()) {
            contactMethod = "phone";
        }

        int rating = 5;
        try {
            if (ratingStr != null && !ratingStr.trim().isEmpty()) {
                rating = Integer.parseInt(ratingStr.trim());
                if (rating < 1) rating = 1;
                if (rating > 5) rating = 5;
            }
        } catch (NumberFormatException ignored) {}

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

        // Map label & title
        String categoryLabel;
        String title;
        switch (category) {
            case "wrong_size":
                categoryLabel = "Sai kích cỡ / Món";
                title = "Sai kích thước hoặc tùy chọn ly";
                break;
            case "quality":
                categoryLabel = "Hương vị / Nhiệt độ";
                title = "Chất lượng hương vị chưa đạt chuẩn";
                break;
            case "delivery":
                categoryLabel = "Giao trễ / Đổ vỡ";
                title = "Giao hàng chậm / Khâu vận chuyển";
                break;
            case "attitude":
                categoryLabel = "Thái độ phục vụ";
                title = "Thái độ phục vụ của nhân viên";
                break;
            case "packaging":
                categoryLabel = "Thiếu ống hút/đồ ăn";
                title = "Thiếu ống hút và khăn giấy phụ kiện";
                break;
            default:
                categoryLabel = "Vấn đề khác";
                title = "Ý kiến đóng góp dịch vụ";
                break;
        }

        // Generate Ticket Code: #FB-2024-XXX or #FB-XXXX
        int randNum = 1000 + random.nextInt(9000);
        String ticketCode = "#FB-" + randNum;

        Feedback fb = new Feedback();
        fb.setTicketCode(ticketCode);
        if (user != null) {
            fb.setCustomerId(user.getId());
            fb.setCustomerName(user.getFullName());
            fb.setCustomerPhone(user.getPhone());
        } else {
            fb.setCustomerName("Khách hàng");
        }
        fb.setOrderCode(orderCode);
        fb.setCategory(category);
        fb.setCategoryLabel(categoryLabel);
        fb.setRating(rating);
        fb.setTitle(title);
        fb.setContent(content.trim());
        fb.setImages(images != null ? images.trim() : "");
        fb.setContactMethod(contactMethod);
        fb.setStatus("PROCESSING");
        fb.setStatusLabel("Đã tiếp nhận");
        fb.setProgressPercent(25);
        fb.setAgentName("Thanh Trúc (Trưởng ca CSKH)");
        fb.setAgentNote("Bộ phận CSKH Aura đã tiếp nhận và đang liên hệ cửa hàng chi nhánh để giải quyết ngay cho bạn.");

        boolean saved = feedbackDAO.createFeedback(fb);

        if (ajax) {
            response.setContentType("application/json;charset=UTF-8");
            if (saved) {
                response.getWriter().write("{\"success\":true,\"ticketCode\":\"" + ticketCode + "\",\"message\":\"Đã gửi phản ánh thành công! Chuyên viên CSKH Aura sẽ phản hồi trong 15 phút.\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"success\":false,\"message\":\"Có lỗi xảy ra khi lưu phản ánh. Vui lòng thử lại!\"}");
            }
            return;
        }

        response.sendRedirect(request.getContextPath() + "/feedback?success=1");
    }
}
