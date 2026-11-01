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
        String sql = "INSERT INTO customer_reviews (customer_id, order_id, issue_type, rating, content, status, admin_response) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, fb.getCustomerId());
            ps.setInt(2, fb.getOrderId());
            ps.setString(3, fb.getIssueType()); // 'RATING' or 'COMPLAINT'

            // Ràng buộc ràng buộc kiểm tra check constraint chk_review_type trong MySQL:
            // Nếu COMPLAINT -> rating BẮT BUỘC là NULL
            // Nếu RATING -> rating từ 1 đến 5
            if ("COMPLAINT".equalsIgnoreCase(fb.getIssueType())) {
                ps.setNull(4, Types.INTEGER);
            } else {
                int r = fb.getRating();
                if (r < 1) r = 1;
                if (r > 5) r = 5;
                ps.setInt(4, r);
            }

            ps.setString(5, fb.getContent());
            ps.setString(6, fb.getStatus() != null ? fb.getStatus() : "PENDING");
            ps.setString(7, fb.getAdminResponse());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                int feedbackId = 0;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        feedbackId = rs.getInt(1);
                    }
                }
                if (feedbackId > 0) {
                    fb.setId(feedbackId);
                    fb.setTicketCode("#CR-" + String.format("%04d", feedbackId));
                    try (PreparedStatement updateTicket = conn.prepareStatement(
                            "UPDATE customer_reviews SET ticket_code = ? WHERE id = ?")) {
                        updateTicket.setString(1, fb.getTicketCode());
                        updateTicket.setInt(2, feedbackId);
                        updateTicket.executeUpdate();
                    }
                }
                return true;
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
                     "WHERE o.customer_id = ? " +
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
                         "WHERE o.id = ? AND o.customer_id = ? " +
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
                     "WHERE o.customer_id = ? AND (o.order_code = ? OR o.order_code = ?) " +
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
