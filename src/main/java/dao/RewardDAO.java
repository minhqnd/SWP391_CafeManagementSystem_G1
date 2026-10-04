package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import context.DBContext;
import model.RewardItem;

public class RewardDAO {

    private static final SecureRandom RANDOM = new SecureRandom();

    public List<RewardItem> getAllRewards() {
        List<RewardItem> list = new ArrayList<>();
        String sql = "SELECT * FROM reward_items WHERE status = 'ACTIVE'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new RewardItem(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("points_cost"),
                    rs.getString("category"),
                    rs.getString("image_icon"),
                    rs.getInt("stock"),
                    rs.getString("status")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Đổi điểm lấy voucher giảm giá. Trong cùng 1 transaction:
     * trừ điểm (users.total_points + loyalty_transactions REDEEM), trừ tồn kho quà (nếu có giới hạn),
     * tạo voucher riêng của khách (FIXED_AMOUNT, dùng 1 lần, hết hạn sau valid_days ngày) và lưu vào user_rewards.
     * Trả về mã voucher vừa tạo, hoặc null nếu không đủ điểm / quà hết / lỗi.
     */
    public String redeemReward(int userId, int rewardId) {
        String checkSql = "SELECT points_cost, discount_value, min_order_value, valid_days, stock FROM reward_items " +
                          "WHERE id = ? AND status = 'ACTIVE' AND stock != 0 AND discount_value > 0 FOR UPDATE";
        // Chỉ trừ khi còn đủ điểm (tránh 2 lần bấm cùng lúc làm âm điểm)
        String updatePointsSql = "UPDATE users SET total_points = total_points - ? WHERE id = ? AND total_points >= ?";
        String updateStockSql = "UPDATE reward_items SET stock = stock - 1 WHERE id = ? AND stock > 0";
        String insertVoucherSql = "INSERT INTO vouchers (code, customer_id, discount_type, discount_value, min_order_value, " +
                                  "max_discount, start_date, valid_until, usage_limit, used_count, status) " +
                                  "VALUES (?, ?, 'FIXED_AMOUNT', ?, ?, NULL, NOW(), DATE_ADD(NOW(), INTERVAL ? DAY), 1, 0, 'ACTIVE')";
        String insertTxnSql = "INSERT INTO loyalty_transactions (customer_id, points_change, transaction_type) VALUES (?, ?, 'REDEEM')";
        String insertRewardSql = "INSERT INTO user_rewards (customer_id, reward_id, voucher_id) VALUES (?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            int cost;
            double discountValue;
            double minOrderValue;
            int validDays;
            int stock;
            try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setInt(1, rewardId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (!rs.next()) { conn.rollback(); return null; }
                    cost = rs.getInt("points_cost");
                    discountValue = rs.getDouble("discount_value");
                    minOrderValue = rs.getDouble("min_order_value");
                    validDays = rs.getInt("valid_days");
                    stock = rs.getInt("stock");
                }
            }

            try (PreparedStatement psUpdate = conn.prepareStatement(updatePointsSql)) {
                psUpdate.setInt(1, cost);
                psUpdate.setInt(2, userId);
                psUpdate.setInt(3, cost);
                if (psUpdate.executeUpdate() == 0) { conn.rollback(); return null; }
            }

            // stock = -1 nghĩa là không giới hạn số lượng
            if (stock > 0) {
                try (PreparedStatement psStock = conn.prepareStatement(updateStockSql)) {
                    psStock.setInt(1, rewardId);
                    psStock.executeUpdate();
                }
            }

            String voucherCode = generateVoucherCode();
            int voucherId;
            try (PreparedStatement psVoucher = conn.prepareStatement(insertVoucherSql, Statement.RETURN_GENERATED_KEYS)) {
                psVoucher.setString(1, voucherCode);
                psVoucher.setInt(2, userId);
                psVoucher.setDouble(3, discountValue);
                psVoucher.setDouble(4, minOrderValue);
                psVoucher.setInt(5, validDays);
                psVoucher.executeUpdate();
                try (ResultSet rs = psVoucher.getGeneratedKeys()) {
                    if (!rs.next()) { conn.rollback(); return null; }
                    voucherId = rs.getInt(1);
                }
            }

            try (PreparedStatement psTxn = conn.prepareStatement(insertTxnSql)) {
                psTxn.setInt(1, userId);
                psTxn.setInt(2, -cost);
                psTxn.executeUpdate();
            }

            try (PreparedStatement psReward = conn.prepareStatement(insertRewardSql)) {
                psReward.setInt(1, userId);
                psReward.setInt(2, rewardId);
                psReward.setInt(3, voucherId);
                psReward.executeUpdate();
            }

            conn.commit();
            return voucherCode;
        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (Exception ex) {}
        }
        return null;
    }

    /** Mã voucher đổi điểm dạng RW + 8 ký tự, VD: RW7K2M9QXA */
    private String generateVoucherCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder("RW");
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return code.toString();
    }
}
