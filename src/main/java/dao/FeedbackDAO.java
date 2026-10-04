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
        fb.setTicketCode("#CR-" + String.format("%04d", id));
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

        // Thiết lập trường hiển thị cho giao diện
        if ("RATING".equalsIgnoreCase(issueType)) {
            fb.setCategory("rating");
            fb.setCategoryLabel("Đánh giá & Góp ý (" + rating + "★)");
            fb.setTitle("Đánh giá đơn hàng #" + fb.getOrderCode());
        } else {
            fb.setCategory("complaint");
            fb.setCategoryLabel("Khiếu nại sự cố đơn hàng");
            fb.setTitle("Khiếu nại đơn hàng #" + fb.getOrderCode());
        }

        if ("RESOLVED".equalsIgnoreCase(fb.getStatus())) {
            fb.setStatusLabel("Đã giải quyết xong");
            fb.setProgressPercent(100);
            fb.setResolution(fb.getAdminResponse());
            fb.setAgentNote(fb.getAdminResponse());
        } else if ("REJECTED".equalsIgnoreCase(fb.getStatus())) {
            fb.setStatusLabel("Từ chối xử lý");
            fb.setProgressPercent(100);
            fb.setResolution(fb.getAdminResponse());
            fb.setAgentNote(fb.getAdminResponse());
        } else {
            fb.setStatusLabel("Đang chờ tiếp nhận");
            fb.setProgressPercent(35);
            fb.setAgentNote("Bộ phận CSKH đang tiếp nhận và rà soát lại đơn hàng #" + fb.getOrderCode() + ".");
        }

        fb.setAgentName("Ban Quản lý Aura Café");
        return fb;
    }

    public List<Feedback> getAllFeedbacks(int limit) {
        List<Feedback> list = new ArrayList<>();
        String sql = "SELECT cr.*, u.full_name AS customer_name, u.phone AS customer_phone, o.order_code " +
                     "FROM customer_reviews cr " +
                     "JOIN users u ON cr.customer_id = u.id " +
                     "JOIN orders o ON cr.order_id = o.id " +
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
        String sql = "SELECT cr.*, u.full_name AS customer_name, u.phone AS customer_phone, o.order_code " +
                     "FROM customer_reviews cr " +
                     "JOIN users u ON cr.customer_id = u.id " +
                     "JOIN orders o ON cr.order_id = o.id " +
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
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        fb.setId(rs.getInt(1));
                        fb.setTicketCode("#CR-" + String.format("%04d", fb.getId()));
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
        String sql = "SELECT id, order_code, created_at, final_amount, status FROM orders WHERE customer_id = ? ORDER BY id DESC LIMIT 10";
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
            return getDefaultOrderForCustomer(customerId);
        }
        String clean = orderCodeOrId.replace("#", "").trim();

        // 1. Thử parse trực tiếp số ID đơn hàng
        try {
            int orderId = Integer.parseInt(clean);
            String sql = "SELECT id FROM orders WHERE id = ?";
            try (Connection conn = DBContext.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("id");
                }
            }
        } catch (Exception ignored) {}

        // 2. Tìm kiếm theo order_code
        String sql = "SELECT id FROM orders WHERE order_code = ? OR order_code = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clean);
            ps.setString(2, orderCodeOrId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (Exception ignored) {}

        // Fallback đơn hàng gần nhất của khách hàng hoặc đơn hàng mặc định
        return getDefaultOrderForCustomer(customerId);
    }

    private int getDefaultOrderForCustomer(int customerId) {
        String sql = "SELECT id FROM orders WHERE customer_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (Exception ignored) {}

        String fallbackSql = "SELECT id FROM orders ORDER BY id ASC LIMIT 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(fallbackSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("id");
        } catch (Exception ignored) {}

        return 1;
    }
}
