package dao;

import model.DeliveryArea;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DeliveryAreaDAO - Quận/Huyện quán giao tới + phí giao cố định từng quận (bảng delivery_areas).
 */
public class DeliveryAreaDAO extends BaseDAO {

    /**
     * Khu vực mặc định khớp với database/aura_cafe.sql. Checkout dùng danh sách
     * này khi một database cũ chưa có bảng delivery_areas hoặc truy vấn tạm thời
     * thất bại, tránh khóa hoàn toàn luồng đặt hàng.
     */
    public static List<DeliveryArea> getDefaultAreas() {
        return new ArrayList<>(Arrays.asList(
                new DeliveryArea(0, "Hoàn Kiếm", 10000, true),
                new DeliveryArea(0, "Ba Đình", 15000, true),
                new DeliveryArea(0, "Hai Bà Trưng", 15000, true),
                new DeliveryArea(0, "Đống Đa", 15000, true),
                new DeliveryArea(0, "Tây Hồ", 20000, true),
                new DeliveryArea(0, "Long Biên", 20000, true),
                new DeliveryArea(0, "Thanh Xuân", 20000, true),
                new DeliveryArea(0, "Hoàng Mai", 20000, true),
                new DeliveryArea(0, "Cầu Giấy", 25000, true),
                new DeliveryArea(0, "Nam Từ Liêm", 30000, true),
                new DeliveryArea(0, "Bắc Từ Liêm", 30000, true),
                new DeliveryArea(0, "Hà Đông", 30000, true)
        ));
    }

    public static DeliveryArea findDefault(String district) {
        if (district == null) return null;
        for (DeliveryArea area : getDefaultAreas()) {
            if (area.getDistrict().equalsIgnoreCase(district.trim())) return area;
        }
        return null;
    }

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
