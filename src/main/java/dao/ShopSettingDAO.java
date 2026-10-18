package dao;

import model.ShopSettings;
import model.ShopSettingEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ShopSettingDAO - Đọc / lưu cấu hình bán hàng online (bảng shop_settings, dạng khoá - giá trị)
 */
public class ShopSettingDAO extends BaseDAO {

    /** Lấy toàn bộ khóa để hiển thị trên màn hình quản trị. */
    public List<ShopSettingEntry> findAll() {
        List<ShopSettingEntry> entries = new ArrayList<>();
        String sql = "SELECT setting_key, setting_value, updated_at FROM shop_settings ORDER BY setting_key";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                entries.add(new ShopSettingEntry(rs.getString("setting_key"), rs.getString("setting_value"),
                        rs.getTimestamp("updated_at").toLocalDateTime()));
            }
        } catch (Exception e) {
            System.err.println("[ShopSettingDAO] Không tải được danh sách cài đặt: " + e.getMessage());
        }
        return entries;
    }

    /** Cập nhật nhiều khóa hiện có trong cùng một transaction. */
    public boolean updateAll(Map<String, String> values) {
        if (values == null || values.isEmpty()) return true;
        String sql = "UPDATE shop_settings SET setting_value = ? WHERE setting_key = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    ps.setString(1, entry.getValue());
                    ps.setString(2, entry.getKey());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) { }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) { }
        }
    }

    /** Thêm khóa mới; trả về false nếu khóa đã tồn tại hoặc database báo lỗi. */
    public boolean insert(String key, String value) {
        String sql = "INSERT INTO shop_settings (setting_key, setting_value) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            System.err.println("[ShopSettingDAO] Không thêm được khóa " + key + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Cấu hình hiện tại. Khoá nào chưa có trong Database thì dùng giá trị mặc định của ShopSettings,
     * riêng khung giờ ca không có mặc định (để null = "Chưa cấu hình").
     */
    public ShopSettings getSettings() {
        ShopSettings settings = new ShopSettings();
        String sql = "SELECT setting_key, setting_value FROM shop_settings";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                applyValue(settings, rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (Exception e) {
            System.err.println("[ShopSettingDAO] Không đọc được shop_settings, dùng cấu hình mặc định: " + e.getMessage());
        }
        return settings;
    }

    /** Lưu toàn bộ cấu hình trong 1 transaction. Trả về false nếu lỗi (VD chưa tạo bảng shop_settings). */
    public boolean saveSettings(ShopSettings settings) {
        String sql = "INSERT INTO shop_settings (setting_key, setting_value) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE setting_value = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                addValue(ps, ShopSettings.KEY_FREESHIP_THRESHOLD, Math.round(settings.getFreeshipThreshold()));
                addValue(ps, ShopSettings.KEY_DELIVERY_FEE, Math.round(settings.getDeliveryFee()));
                addValue(ps, ShopSettings.KEY_DELIVERY_MINUTES, settings.getDeliveryMinutes());
                addValue(ps, ShopSettings.KEY_POINTS_STEP, settings.getPointsStep());
                addValue(ps, ShopSettings.KEY_POINTS_STEP_VALUE, Math.round(settings.getPointsStepValue()));
                ps.executeBatch();
            }
            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private void addValue(PreparedStatement ps, String key, long value) throws SQLException {
        ps.setString(1, key);
        ps.setString(2, String.valueOf(value));
        ps.setString(3, String.valueOf(value));
        ps.addBatch();
    }

    /** Bỏ qua giá trị sai định dạng (giữ mặc định) */
    private void applyValue(ShopSettings settings, String key, String value) {
        try {
            switch (key) {
                case ShopSettings.KEY_FREESHIP_THRESHOLD:
                    settings.setFreeshipThreshold(Double.parseDouble(value.trim()));
                    break;
                case ShopSettings.KEY_DELIVERY_FEE:
                    settings.setDeliveryFee(Double.parseDouble(value.trim()));
                    break;
                case ShopSettings.KEY_DELIVERY_MINUTES:
                    settings.setDeliveryMinutes(Integer.parseInt(value.trim()));
                    break;
                case ShopSettings.KEY_POINTS_STEP:
                    settings.setPointsStep(Integer.parseInt(value.trim()));
                    break;
                case ShopSettings.KEY_POINTS_STEP_VALUE:
                    settings.setPointsStepValue(Double.parseDouble(value.trim()));
                    break;
                case ShopSettings.KEY_SHIFT_MORNING:
                    settings.setShiftMorning(value.trim());
                    break;
                case ShopSettings.KEY_SHIFT_AFTERNOON:
                    settings.setShiftAfternoon(value.trim());
                    break;
                case ShopSettings.KEY_SHIFT_EVENING:
                    settings.setShiftEvening(value.trim());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ShopSettingDAO] Giá trị cấu hình không hợp lệ " + key + " = " + value);
        }
    }

    /** Cập nhật khung giờ ca làm việc theo key (shift_morning, shift_afternoon, shift_evening) */
    public boolean updateShiftHours(String shiftKey, String timeRange) {
        if (shiftKey == null || timeRange == null || timeRange.trim().isEmpty()) {
            return false;
        }
        String sql = "INSERT INTO shop_settings (setting_key, setting_value) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE setting_value = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, shiftKey.trim());
            ps.setString(2, timeRange.trim());
            ps.setString(3, timeRange.trim());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
