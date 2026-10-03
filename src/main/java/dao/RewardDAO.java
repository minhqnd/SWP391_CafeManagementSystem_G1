package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import context.DBContext;
import model.RewardItem;

public class RewardDAO {

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

    public boolean redeemReward(int userId, int rewardId) {
        String checkSql = "SELECT points_cost FROM reward_items WHERE id = ? AND status = 'ACTIVE' AND stock != 0";
        String checkUserSql = "SELECT total_points FROM users WHERE id = ?";
        String updatePointsSql = "UPDATE users SET total_points = total_points - ? WHERE id = ?";
        String insertTxnSql = "INSERT INTO loyalty_transactions (customer_id, points_change, transaction_type) VALUES (?, ?, 'REDEEM')";
        String insertRewardSql = "INSERT INTO user_rewards (customer_id, reward_id) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            int cost = -1;
            try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setInt(1, rewardId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) cost = rs.getInt("points_cost");
                }
            }
            if (cost == -1) { conn.rollback(); return false; }

            int currentPoints = 0;
            try (PreparedStatement psCheckUser = conn.prepareStatement(checkUserSql)) {
                psCheckUser.setInt(1, userId);
                try (ResultSet rs = psCheckUser.executeQuery()) {
                    if (rs.next()) currentPoints = rs.getInt("total_points");
                }
            }
            if (currentPoints < cost) { conn.rollback(); return false; }

            try (PreparedStatement psUpdate = conn.prepareStatement(updatePointsSql)) {
                psUpdate.setInt(1, cost);
                psUpdate.setInt(2, userId);
                psUpdate.executeUpdate();
            }

            try (PreparedStatement psTxn = conn.prepareStatement(insertTxnSql)) {
                psTxn.setInt(1, userId);
                psTxn.setInt(2, -cost);
                psTxn.executeUpdate();
            }

            try (PreparedStatement psReward = conn.prepareStatement(insertRewardSql)) {
                psReward.setInt(1, userId);
                psReward.setInt(2, rewardId);
                psReward.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); conn.close(); } catch (Exception ex) {}
        }
        return false;
    }
}
