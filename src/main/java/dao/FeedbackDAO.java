package dao;

import context.DBContext;
import model.Feedback;

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

    private Feedback mapResultSetToFeedback(ResultSet rs) throws Exception {
        Feedback fb = new Feedback();
        int id = rs.getInt("id");
        fb.setId(id);
        String ticketCode = rs.getString("ticket_code");
        fb.setTicketCode(ticketCode != null && !ticketCode.isBlank()
                ? ticketCode : "#CR-" + String.format("%04d", id));
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
        try {
            fb.setCustomerName(rs.getString("customer_name"));
            fb.setCustomerPhone(rs.getString("customer_phone"));
        } catch (Exception ignored) {}

        try {
            fb.setOrderCode(rs.getString("order_code"));
        } catch (Exception ignored) {}

        return fb;
    }

    public List<Feedback> getAllFeedbacks(int limit) {
        List<Feedback> list = new ArrayList<>();
        String sql = "SELECT cr.*, u.full_name AS customer_name, u.phone AS customer_phone, COALESCE(o.order_code, cr.order_code) AS order_code " +
                     "FROM customer_reviews cr " +
                     "JOIN users u ON cr.customer_id = u.id " +
                     "LEFT JOIN orders o ON cr.order_id = o.id " +
                     "ORDER BY cr.created_at DESC LIMIT ?";
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

    public List<Feedback> getFeedbacksByCustomerId(int customerId) {
        List<Feedback> list = new ArrayList<>();
        String sql = "SELECT cr.*, u.full_name AS customer_name, u.phone AS customer_phone, COALESCE(o.order_code, cr.order_code) AS order_code " +
                     "FROM customer_reviews cr " +
                     "JOIN users u ON cr.customer_id = u.id " +
                     "LEFT JOIN orders o ON cr.order_id = o.id " +
                     "WHERE cr.customer_id = ? " +
                     "ORDER BY cr.created_at DESC";
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
