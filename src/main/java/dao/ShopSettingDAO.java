package dao;

import model.ShopSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * ShopSettingDAO - Đọc / lưu cấu hình bán hàng online (bảng shop_settings, dạng khoá - giá trị)
 */
public class ShopSettingDAO extends BaseDAO {

    /**
     * Cấu hình hiện tại. Khoá nào chưa có trong Database (hoặc chưa chạy update_shop_settings.sql)
     * thì dùng giá trị mặc định của ShopSettings.
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

    /**
     * Khung giờ ca đang lưu trong Database (khoá shift_morning, shift_afternoon, shift_evening -> "HH:mm-HH:mm").
     * Khác getSettings(): khoá nào chưa có thì không có trong Map (không lấy giá trị mặc định),
     * để màn hình biết ca đó "Chưa cấu hình". Lỗi Database thì trả về Map rỗng.
     */
    public Map<String, String> getShiftHours() {
        Map<String, String> hours = new HashMap<>();
        String sql = "SELECT setting_key, setting_value FROM shop_settings WHERE setting_key IN (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ShopSettings.KEY_SHIFT_MORNING);
            ps.setString(2, ShopSettings.KEY_SHIFT_AFTERNOON);
            ps.setString(3, ShopSettings.KEY_SHIFT_EVENING);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String value = rs.getString("setting_value");
                    if (value != null && !value.trim().isEmpty()) {
                        hours.put(rs.getString("setting_key"), value.trim());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ShopSettingDAO] Không đọc được khung giờ ca: " + e.getMessage());
        }
        return hours;
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
