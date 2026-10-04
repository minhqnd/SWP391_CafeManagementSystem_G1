package dao;

import context.DBContext;
import model.Feedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * FeedbackDAO - Quản lý phiếu phản ánh và khiếu nại của khách hàng
 */
public class FeedbackDAO extends BaseDAO {

    private Feedback mapResultSetToFeedback(ResultSet rs) throws Exception {
        Feedback fb = new Feedback();
        fb.setId(rs.getInt("id"));
        fb.setTicketCode(rs.getString("ticket_code"));
        int custId = rs.getInt("customer_id");
        if (!rs.wasNull()) {
            fb.setCustomerId(custId);
        }
        fb.setCustomerName(rs.getString("customer_name"));
        fb.setCustomerPhone(rs.getString("customer_phone"));
        fb.setOrderCode(rs.getString("order_code"));
        fb.setCategory(rs.getString("category"));
        fb.setCategoryLabel(rs.getString("category_label"));
        fb.setRating(rs.getInt("rating"));
        fb.setTitle(rs.getString("title"));
        fb.setContent(rs.getString("content"));
        fb.setImages(rs.getString("images"));
        fb.setContactMethod(rs.getString("contact_method"));
        fb.setStatus(rs.getString("status"));
        fb.setStatusLabel(rs.getString("status_label"));
        fb.setProgressPercent(rs.getInt("progress_percent"));
        fb.setAgentName(rs.getString("agent_name"));
        fb.setAgentNote(rs.getString("agent_note"));
        fb.setResolution(rs.getString("resolution"));
        fb.setGiftVoucher(rs.getString("gift_voucher"));
        fb.setCreatedAt(rs.getTimestamp("created_at"));
        fb.setUpdatedAt(rs.getTimestamp("updated_at"));
        return fb;
    }

    public List<Feedback> getAllFeedbacks(int limit) {
        List<Feedback> list = new ArrayList<>();
        String sql = "SELECT * FROM customer_feedbacks ORDER BY created_at DESC LIMIT ?";
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
        String sql = "SELECT * FROM customer_feedbacks WHERE customer_id = ? ORDER BY created_at DESC";
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
        String sql = "INSERT INTO customer_feedbacks " +
                "(ticket_code, customer_id, customer_name, customer_phone, order_code, category, category_label, " +
                "rating, title, content, images, contact_method, status, status_label, progress_percent, agent_name, agent_note) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fb.getTicketCode());
            if (fb.getCustomerId() != null) {
                ps.setInt(2, fb.getCustomerId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, fb.getCustomerName());
            ps.setString(4, fb.getCustomerPhone());
            ps.setString(5, fb.getOrderCode());
            ps.setString(6, fb.getCategory());
            ps.setString(7, fb.getCategoryLabel());
            ps.setInt(8, fb.getRating());
            ps.setString(9, fb.getTitle());
            ps.setString(10, fb.getContent());
            ps.setString(11, fb.getImages());
            ps.setString(12, fb.getContactMethod());
            ps.setString(13, fb.getStatus());
            ps.setString(14, fb.getStatusLabel());
            ps.setInt(15, fb.getProgressPercent());
            ps.setString(16, fb.getAgentName());
            ps.setString(17, fb.getAgentNote());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        fb.setId(rs.getInt(1));
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
        String sql = "SELECT order_code, created_at, final_amount, status FROM orders WHERE customer_id = ? ORDER BY id DESC LIMIT 5";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{
                            rs.getString("order_code"),
                            rs.getString("status"),
                            String.valueOf(rs.getDouble("final_amount"))
                    });
                }
            }
        } catch (Exception e) {
            // ignore if orders table schema differs
        }
        return list;
    }
}
