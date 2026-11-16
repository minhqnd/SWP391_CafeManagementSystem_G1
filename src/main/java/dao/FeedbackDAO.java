package dao;

import context.DBContext;
import model.Feedback;
import model.FeedbackStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * FeedbackDAO - Quản lý phiếu phản ánh và khiếu nại của khách hàng
 * Sử dụng trực tiếp bảng `customer_reviews` từ cơ sở dữ liệu `aura_cafe.sql`
 */
public class FeedbackDAO extends BaseDAO {

    /*
     * BF09 persistence flow:
     * customer_reviews là nguồn dữ liệu duy nhất cho feedback. Phiếu mới ở PENDING;
     * Admin có thể chuyển sang PROCESSING/RESOLVED/REJECTED. Phiếu terminal không được mở lại.
     */
    public enum ResponseUpdateResult {
        UPDATED,
        NOT_FOUND,
        ALREADY_CLOSED
    }

    private Feedback mapResultSetToFeedback(ResultSet rs) throws Exception {
        Feedback fb = new Feedback();
        int id = rs.getInt("id");
        fb.setId(id);
        try {
            String ticketCode = rs.getString("ticket_code");
            fb.setTicketCode(ticketCode != null && !ticketCode.isBlank()
                    ? ticketCode : "#" + id);
        } catch (Exception e) {
            fb.setTicketCode("#" + id);
        }
        fb.setCustomerId(rs.getInt("customer_id"));
        fb.setOrderId(rs.getInt("order_id"));

        String issueType = rs.getString("issue_type");
        fb.setIssueType(issueType);

        int rating = rs.getInt("rating");
        if (rs.wasNull()) {
            fb.setRating(0);
        } else {
            fb.setRating(rating);
        }

        fb.setContent(rs.getString("content"));
        fb.setStatus(rs.getString("status"));
        fb.setAdminResponse(rs.getString("admin_response"));
        fb.setCreatedAt(rs.getTimestamp("created_at"));

        // Thông tin người dùng & đơn hàng được JOIN
        fb.setCustomerName(rs.getString("resolved_customer_name"));
        fb.setCustomerPhone(rs.getString("resolved_customer_phone"));
        fb.setOrderCode(rs.getString("resolved_order_code"));

        return fb;
    }

    public List<Feedback> getAllFeedbacks(int limit) {
        List<Feedback> list = new ArrayList<>();
        String sql = "SELECT cr.*, " +
                     "COALESCE(NULLIF(TRIM(u.full_name), ''), NULLIF(TRIM(cr.customer_name), ''), CONCAT('Khách hàng #', cr.customer_id)) AS resolved_customer_name, " +
                     "COALESCE(NULLIF(TRIM(u.phone), ''), NULLIF(TRIM(cr.customer_phone), '')) AS resolved_customer_phone, " +
                     "COALESCE(NULLIF(TRIM(o.order_code), ''), NULLIF(TRIM(cr.order_code), '')) AS resolved_order_code " +
                     "FROM customer_reviews cr " +
                     "JOIN users u ON cr.customer_id = u.id " +
                     "LEFT JOIN orders o ON cr.order_id = o.id " +
                     "ORDER BY cr.created_at DESC, cr.id DESC LIMIT ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 20);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToFeedback(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Feedback> getAdminFeedbacks(String issueType, String status) throws Exception {
        List<Feedback> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT cr.*, " +
                "COALESCE(NULLIF(TRIM(u.full_name), ''), NULLIF(TRIM(cr.customer_name), ''), CONCAT('Khách hàng #', cr.customer_id)) AS resolved_customer_name, " +
                "COALESCE(NULLIF(TRIM(u.phone), ''), NULLIF(TRIM(cr.customer_phone), '')) AS resolved_customer_phone, " +
                "COALESCE(NULLIF(TRIM(o.order_code), ''), NULLIF(TRIM(cr.order_code), '')) AS resolved_order_code " +
                "FROM customer_reviews cr " +
                "JOIN users u ON cr.customer_id = u.id " +
                "LEFT JOIN orders o ON cr.order_id = o.id " +
                "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (issueType != null && !issueType.isBlank() && !"ALL".equalsIgnoreCase(issueType)) {
            sql.append("AND cr.issue_type = ? ");
            params.add(issueType.trim().toUpperCase());
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND cr.status = ? ");
            params.add(status.trim().toUpperCase());
        }
        sql.append("ORDER BY cr.created_at DESC, cr.id DESC");

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToFeedback(rs));
                }
            }
        }
        return list;
    }

    public ResponseUpdateResult updateFeedbackResponse(
            int id, FeedbackStatus targetStatus, String adminResponse) throws Exception {
        // BF09.5: Validate dữ liệu trước khi mở transaction để giảm thời gian giữ khóa DB.
        if (id <= 0) {
            throw new IllegalArgumentException("Mã phiếu không hợp lệ.");
        }
        if (targetStatus == null || targetStatus == FeedbackStatus.PENDING) {
            throw new IllegalArgumentException("Phản hồi phải chuyển phiếu sang trạng thái đang xử lý hoặc kết thúc.");
        }
        if (adminResponse == null || adminResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung phản hồi không được để trống.");
        }
        String responseText = adminResponse.trim();
        if (responseText.length() > 2000) {
            throw new IllegalArgumentException("Nội dung phản hồi không được vượt quá 2.000 ký tự.");
        }

        String selectSql = "SELECT status FROM customer_reviews WHERE id = ? FOR UPDATE";
        String updateSql = "UPDATE customer_reviews SET status = ?, admin_response = ? WHERE id = ?";

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                FeedbackStatus currentStatus;
                // BF09.6: FOR UPDATE chống hai Admin cùng trả lời/đóng một phiếu tại cùng thời điểm.
                try (PreparedStatement select = conn.prepareStatement(selectSql)) {
                    select.setInt(1, id);
                    try (ResultSet rs = select.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return ResponseUpdateResult.NOT_FOUND;
                        }
                        currentStatus = FeedbackStatus.from(rs.getString("status"));
                    }
                }

                if (currentStatus.isTerminal()) {
                    // BF09.7: RESOLVED/REJECTED là trạng thái kết thúc, không cho chỉnh sửa hoặc mở lại.
                    conn.rollback();
                    return ResponseUpdateResult.ALREADY_CLOSED;
                }
                if (!currentStatus.canReplyWith(targetStatus)) {
                    throw new IllegalArgumentException("Chuyển trạng thái phiếu không hợp lệ.");
                }

                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setString(1, targetStatus.name());
                    update.setString(2, responseText);
                    update.setInt(3, id);
                    update.executeUpdate();
                }
                conn.commit();
                return ResponseUpdateResult.UPDATED;
            } catch (Exception ex) {
                // BF09.8: Lỗi ở bất kỳ bước nào đều rollback để trạng thái và nội dung không lệch nhau.
                conn.rollback();
                throw ex;
            }
        }
    }

    public List<Feedback> getFeedbacksByCustomerId(int customerId) {
        List<Feedback> list = new ArrayList<>();
        String sql = "SELECT cr.*, " +
                     "COALESCE(NULLIF(TRIM(u.full_name), ''), NULLIF(TRIM(cr.customer_name), ''), CONCAT('Khách hàng #', cr.customer_id)) AS resolved_customer_name, " +
                     "COALESCE(NULLIF(TRIM(u.phone), ''), NULLIF(TRIM(cr.customer_phone), '')) AS resolved_customer_phone, " +
                     "COALESCE(NULLIF(TRIM(o.order_code), ''), NULLIF(TRIM(cr.order_code), '')) AS resolved_order_code " +
                     "FROM customer_reviews cr " +
                     "JOIN users u ON cr.customer_id = u.id " +
                     "LEFT JOIN orders o ON cr.order_id = o.id " +
                     "WHERE cr.customer_id = ? " +
                     "ORDER BY cr.created_at DESC, cr.id DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToFeedback(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean createFeedback(Feedback fb) {
        if (fb == null || fb.getCustomerId() <= 0 || fb.getOrderId() <= 0
                || fb.getContent() == null || fb.getContent().trim().isEmpty()
                || fb.getContent().trim().length() > 2000
                || (!"RATING".equalsIgnoreCase(fb.getIssueType())
                    && !"COMPLAINT".equalsIgnoreCase(fb.getIssueType()))
                || ("RATING".equalsIgnoreCase(fb.getIssueType())
                    && (fb.getRating() < 1 || fb.getRating() > 5))) {
            return false;
        }

        String lockOrderSql = "SELECT id FROM orders "
                + "WHERE id = ? AND customer_id = ? AND status = 'COMPLETED' FOR UPDATE";
        String duplicateSql = "SELECT 1 FROM customer_reviews WHERE customer_id = ? AND order_id = ? LIMIT 1";
        // BF09.4: Khóa đơn hàng rồi kiểm tra trùng và INSERT trong cùng transaction.
        String sql = "INSERT INTO customer_reviews (customer_id, order_id, issue_type, rating, content, status, admin_response) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement lockOrder = conn.prepareStatement(lockOrderSql)) {
                    lockOrder.setInt(1, fb.getOrderId());
                    lockOrder.setInt(2, fb.getCustomerId());
                    try (ResultSet rs = lockOrder.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                    }
                }
                try (PreparedStatement duplicate = conn.prepareStatement(duplicateSql)) {
                    duplicate.setInt(1, fb.getCustomerId());
                    duplicate.setInt(2, fb.getOrderId());
                    try (ResultSet rs = duplicate.executeQuery()) {
                        if (rs.next()) {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                int feedbackId = 0;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, fb.getCustomerId());
                    ps.setInt(2, fb.getOrderId());
                    ps.setString(3, fb.getIssueType().trim().toUpperCase());
                    if ("COMPLAINT".equalsIgnoreCase(fb.getIssueType())) {
                        ps.setNull(4, Types.INTEGER);
                    } else {
                        ps.setInt(4, fb.getRating());
                    }
                    ps.setString(5, fb.getContent().trim());
                    // Trạng thái và phản hồi Admin không bao giờ được tin từ request tạo mới.
                    ps.setString(6, "PENDING");
                    ps.setNull(7, Types.LONGVARCHAR);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) feedbackId = rs.getInt(1);
                    }
                }
                if (feedbackId <= 0) {
                    throw new IllegalStateException("Không nhận được mã phiếu phản hồi vừa tạo.");
                }
                fb.setId(feedbackId);
                fb.setTicketCode("#CR-" + String.format("%04d", feedbackId));
                try (PreparedStatement updateTicket = conn.prepareStatement(
                        "UPDATE customer_reviews SET ticket_code = ? WHERE id = ?")) {
                    updateTicket.setString(1, fb.getTicketCode());
                    updateTicket.setInt(2, feedbackId);
                    updateTicket.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String[]> getRecentOrdersForCustomer(int customerId) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT o.id, o.order_code, o.created_at, o.final_amount, o.status " +
                     "FROM orders o " +
                     "WHERE o.customer_id = ? AND o.status = 'COMPLETED' " +
                     "AND NOT EXISTS (" +
                     "    SELECT 1 FROM customer_reviews cr " +
                     "    WHERE cr.customer_id = o.customer_id AND cr.order_id = o.id" +
                     ") " +
                     "ORDER BY o.id DESC LIMIT 10";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("order_code"),
                            rs.getString("status"),
                            String.valueOf(Math.round(rs.getDouble("final_amount")))
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public int findOrderId(String orderCodeOrId, int customerId) {
        if (orderCodeOrId == null || orderCodeOrId.trim().isEmpty()) {
            return 0;
        }
        String clean = orderCodeOrId.replace("#", "").trim();

        // Chỉ cho phép chọn đơn thuộc khách hàng và chưa từng phản hồi.
        try {
            int orderId = Integer.parseInt(clean);
            String sql = "SELECT o.id FROM orders o " +
                         "WHERE o.id = ? AND o.customer_id = ? AND o.status = 'COMPLETED' " +
                         "AND NOT EXISTS (" +
                         "    SELECT 1 FROM customer_reviews cr " +
                         "    WHERE cr.customer_id = o.customer_id AND cr.order_id = o.id" +
                         ")";
            try (Connection conn = DBContext.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                ps.setInt(2, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("id");
                }
            }
        } catch (Exception ignored) {}

        String sql = "SELECT o.id FROM orders o " +
                     "WHERE o.customer_id = ? AND o.status = 'COMPLETED' " +
                     "AND (o.order_code = ? OR o.order_code = ?) " +
                     "AND NOT EXISTS (" +
                     "    SELECT 1 FROM customer_reviews cr " +
                     "    WHERE cr.customer_id = o.customer_id AND cr.order_id = o.id" +
                     ")";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setString(2, clean);
            ps.setString(3, orderCodeOrId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (Exception ignored) {}

        return 0;
    }
}
