package dao;

import model.DeliveryArea;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DeliveryAreaDAO - Quận/Huyện quán giao tới + phí giao cố định từng quận (bảng delivery_areas).
 */
public class DeliveryAreaDAO extends BaseDAO {

    /** Các quận đang nhận giao (bước Checkout), phí thấp trước */
    public List<DeliveryArea> getActiveAreas() throws SQLException, ClassNotFoundException {
        return query("SELECT * FROM delivery_areas WHERE is_active = 1 ORDER BY delivery_fee, district");
    }

    /** Tất cả quận (màn Admin), phí thấp trước */
    public List<DeliveryArea> getAllAreas() throws SQLException, ClassNotFoundException {
        return query("SELECT * FROM delivery_areas ORDER BY delivery_fee, district");
    }

    /** Quận đang nhận giao theo tên; null nếu quán không giao tới quận này */
    public DeliveryArea findActive(String district) throws SQLException, ClassNotFoundException {
        String sql = "SELECT * FROM delivery_areas WHERE district = ? AND is_active = 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, district);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean districtExists(String district) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM delivery_areas WHERE district = ?")) {
            ps.setString(1, district);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insert(String district, double deliveryFee) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO delivery_areas (district, delivery_fee, is_active) VALUES (?, ?, 1)")) {
            ps.setString(1, district);
            ps.setDouble(2, deliveryFee);
            ps.executeUpdate();
        }
    }

    /** Lưu phí giao + trạng thái nhận giao của nhiều quận trong 1 transaction */
    public void updateAll(List<DeliveryArea> areas) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE delivery_areas SET delivery_fee = ?, is_active = ? WHERE id = ?";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (DeliveryArea area : areas) {
                    ps.setDouble(1, area.getDeliveryFee());
                    ps.setBoolean(2, area.isActive());
                    ps.setInt(3, area.getId());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private List<DeliveryArea> query(String sql) throws SQLException, ClassNotFoundException {
        List<DeliveryArea> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private DeliveryArea map(ResultSet rs) throws SQLException {
        return new DeliveryArea(rs.getInt("id"), rs.getString("district"),
                rs.getDouble("delivery_fee"), rs.getBoolean("is_active"));
    }
}
