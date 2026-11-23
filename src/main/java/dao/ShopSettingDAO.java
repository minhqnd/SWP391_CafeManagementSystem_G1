package dao;

import model.ShopSettings;
import model.ShopSettingEntry;
import utils.LoyaltyValidation;

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
     * Cấu hình hiện tại. Khoá nào chưa có trong Database thì dùng giá trị mặc định của ShopSettings.
     * (Khung giờ ca theo tuần nằm ở bảng work_shifts - xem WorkShiftDAO.)
     */
    // [TDQT07]
    // . Ở lần mở lại trang, doGet gọi getSettingsStrict trong ShopSettingDAO.java để đọc các thiết lập đang lưu.
    //   Em đọc shop_settings, dùng applyValue điền từng quy tắc vào ShopSettings rồi trả bộ này về
    //   controller để điền form và tính tiền quy đổi.
    // .. Khóa chưa được tạo dùng giá trị mặc định; giá trị không đổi được sang số cũng giữ mặc định của khóa đó.
    //    Lỗi kết nối hoặc câu lệnh đọc thì báo lỗi về controller, không trả một bộ mặc định thay cho kết quả đọc.
    // ... Đọc tiếp tại AdminLoyaltyController.java, tìm [TDQT08].
    public ShopSettings getSettingsStrict() {

        ShopSettings settings = new ShopSettings();
        String sql = "SELECT setting_key, setting_value FROM shop_settings";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                applyValue(settings, rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load shop settings", e);
        }
        return settings;
    }

    public ShopSettings getSettings() {
        try { return getSettingsStrict(); }
        catch (IllegalStateException e) {
            System.err.println("[ShopSettingDAO] Không đọc được shop_settings, dùng cấu hình mặc định.");
            return new ShopSettings();
        }
    }

    // [TDQT05]
    // . Trong saveLoyaltyRules của ShopSettingDAO.java, em nhận ba số từ controller và thêm hoặc cập nhật
    //   các quy tắc trong shop_settings. Ghi được cả ba rồi xác nhận mới trả true; nếu ghi lỗi thì hủy các
    //   thay đổi của lần lưu và trả false. updateRules trong AdminLoyaltyController.java nhận false sẽ báo chưa lưu được.
    // ... Trong ShopSettingDAO.java, đọc tiếp conn.commit và return true của saveLoyaltyRules. Lỗi ghi dữ liệu trả false
    //     về ngay sau settingDAO.saveLoyaltyRules trong AdminLoyaltyController.updateRules để xử lý thất bại.
    public boolean saveLoyaltyRules(long earnUnit, int pointsStep, long stepValue) {
        // . Trước khi ghi quy tắc, trong ShopSettingDAO.saveLoyaltyRules (ghi cả ba quy tắc vào cơ sở dữ liệu)
        //   em gọi LoyaltyValidation.requireRange (kiểm tra số nằm trong khoảng cho phép) để kiểm tra lại cả ba
        //   giới hạn. Em chỉ ghi khi các số đều trong khoảng cho phép.
        LoyaltyValidation.requireRange(earnUnit, "Số tiền chi tiêu", 1, LoyaltyValidation.MAX_RULE_MONEY);
        LoyaltyValidation.requireRange(pointsStep, "Mốc đổi điểm", 1, LoyaltyValidation.MAX_POINTS);
        LoyaltyValidation.requireRange(stepValue, "Tiền giảm", 0, LoyaltyValidation.MAX_RULE_MONEY);
        String sql = "INSERT INTO shop_settings (setting_key, setting_value) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE setting_value = ?";
        try (Connection conn = getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                // . Trong ShopSettingDAO.saveLoyaltyRules (ghi cả ba quy tắc vào cơ sở dữ liệu), em gọi addValue (đưa
                //   tên và giá trị quy tắc vào lệnh lưu) cùng file để đưa tên và giá trị của từng quy tắc vào danh sách
                //   câu lệnh. Em gọi executeBatch (thực hiện cả nhóm lệnh đã chuẩn bị) để ghi cả ba trong cùng lần lưu.
                addValue(ps, ShopSettings.KEY_POINTS_EARN_UNIT, earnUnit);
                addValue(ps, ShopSettings.KEY_POINTS_STEP, pointsStep);
                addValue(ps, ShopSettings.KEY_POINTS_STEP_VALUE, stepValue);
                ps.executeBatch();
                // . Khi cả ba quy tắc đều ghi được, trong ShopSettingDAO.saveLoyaltyRules (ghi cả ba quy tắc vào cơ sở
                //   dữ liệu) em gọi conn.commit (xác nhận lưu toàn bộ thay đổi) để xác nhận bộ mới. Em chỉ trả thành
                //   công sau bước này để controller không thông báo khi dữ liệu còn chưa lưu xong.
                conn.commit();
                // . Hàm ShopSettingDAO.saveLoyaltyRules (ghi cả ba quy tắc vào cơ sở dữ liệu) trả true về updateRules
                //   (kiểm tra và lưu quy tắc tích/đổi điểm) trong AdminLoyaltyController.java khi ghi xong cả bộ. Sau đó
                //   chương trình quay về doPost để đặt loyaltyMessage báo lưu thành công.
                // ... Đọc tiếp tại AdminLoyaltyController.java, tìm [TDQT06].
                return true;
            } catch (SQLException e) {
                // . Nếu một quy tắc ghi lỗi, trong ShopSettingDAO.saveLoyaltyRules (ghi cả ba quy tắc vào cơ sở dữ liệu)
                //   em gọi conn.rollback (hủy các thay đổi của lần lưu) để hủy cả bộ thay đổi rồi trả thất bại. Nhờ vậy
                //   không xảy ra trường hợp chỉ một hoặc hai quy tắc được đổi.
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException | ClassNotFoundException e) {
            return false;
        }
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
                addValue(ps, ShopSettings.KEY_POINTS_EARN_UNIT, Math.round(settings.getPointsEarnUnit()));
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

    // . Ở addValue (đưa tên và giá trị quy tắc vào lệnh lưu) trong ShopSettingDAO.java, em nhận tên và số từ
    //   saveLoyaltyRules (ghi cả ba quy tắc vào cơ sở dữ liệu) rồi điền vào câu lệnh chờ ghi. Em quay lại
    //   hàm gọi để ghép các giá trị còn lại trước khi ghi cả bộ cùng nhau.
    private void addValue(PreparedStatement ps, String key, long value) throws SQLException {
        ps.setString(1, key);
        ps.setString(2, String.valueOf(value));
        ps.setString(3, String.valueOf(value));
        ps.addBatch();
    }

    // . Khi đọc quy tắc trong ShopSettingDAO.getSettingsStrict (đọc các thiết lập đang lưu), em gọi applyValue
    //   (điền giá trị vào đúng thiết lập) cùng file bằng tên quy tắc và giá trị đọc được để điền vào
    //   ShopSettings. Nếu không đổi được giá trị sang số thì ghi lỗi vào nhật ký và giữ mặc định của khóa đó;
    //   sau đó tiếp tục đọc các dòng còn lại, rồi trả bộ settings về controller để hiển thị.
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
                case ShopSettings.KEY_POINTS_EARN_UNIT:
                    settings.setPointsEarnUnit(Double.parseDouble(value.trim()));
                    break;
                case ShopSettings.KEY_PAYMENT_TEST_MODE:
                    settings.setPaymentTestMode(Boolean.parseBoolean(value.trim()));
                    break;
                case ShopSettings.KEY_PAYMENT_GATEWAY_URL:
                    settings.setPaymentGatewayUrl(value.trim());
                    break;
                case ShopSettings.KEY_PAYMENT_CALLBACK_SECRET:
                    settings.setPaymentCallbackSecret(value.trim());
                    break;
                case ShopSettings.KEY_PAYMENT_BANK_ID:
                    settings.setPaymentBankId(value.trim());
                    break;
                case ShopSettings.KEY_PAYMENT_BANK_NAME:
                    settings.setPaymentBankName(value.trim());
                    break;
                case ShopSettings.KEY_PAYMENT_ACCOUNT_NO:
                    settings.setPaymentAccountNo(value.trim());
                    break;
                case ShopSettings.KEY_PAYMENT_ACCOUNT_NAME:
                    settings.setPaymentAccountName(value.trim());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ShopSettingDAO] Giá trị cấu hình không hợp lệ " + key + " = " + value);
        }
    }
}
