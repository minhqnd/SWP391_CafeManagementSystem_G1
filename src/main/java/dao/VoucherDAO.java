package dao;

import context.DBContext;
import model.Voucher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * VoucherDAO - Truy vấn mã khuyến mãi còn hiệu lực
 */
public class VoucherDAO extends BaseDAO {

    // Còn hoạt động, đang trong thời hạn và chưa hết lượt sử dụng
    private static final String USABLE_CONDITION =
            "status = 'ACTIVE' " +
            "AND (start_date IS NULL OR start_date <= NOW()) " +
            "AND (valid_until IS NULL OR valid_until >= NOW()) " +
            "AND (usage_limit IS NULL OR used_count < usage_limit)";

    private static final String SELECT_COLUMNS =
            "SELECT id, code, discount_type, discount_value, min_order_value, max_discount FROM vouchers ";

    private Voucher mapResultSetToVoucher(ResultSet rs) throws Exception {
        Object maxDiscount = rs.getObject("max_discount"); // NULL = không giới hạn mức giảm
        return new Voucher(
                rs.getInt("id"),
                rs.getString("code"),
                rs.getString("discount_type"),
                rs.getDouble("discount_value"),
                rs.getDouble("min_order_value"),
                maxDiscount != null ? ((Number) maxDiscount).doubleValue() : null
        );
    }

    /**
     * Danh sách ưu đãi khả dụng hiển thị trong giỏ hàng
     */
    public List<Voucher> getUsableVouchers() {
        List<Voucher> list = new ArrayList<>();
        String sql = SELECT_COLUMNS + "WHERE " + USABLE_CONDITION + " ORDER BY min_order_value ASC, id ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToVoucher(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Tìm mã khuyến mãi còn dùng được theo code, trả về null nếu không hợp lệ / hết hạn
     */
    public Voucher getUsableVoucherByCode(String code) {
        String sql = SELECT_COLUMNS + "WHERE code = ? AND " + USABLE_CONDITION;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVoucher(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
