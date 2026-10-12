package dao;

import model.ShopSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
                default:
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ShopSettingDAO] Giá trị cấu hình không hợp lệ " + key + " = " + value);
        }
    }
}
