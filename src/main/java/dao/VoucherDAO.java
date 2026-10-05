package dao;

import context.DBContext;
import model.Voucher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDateTime;
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

    // Mã công khai hoặc voucher đổi điểm của chính khách đang đặt (khách chưa đăng nhập chỉ thấy mã công khai)
    private static final String OWNER_CONDITION = "(customer_id IS NULL OR customer_id = ?)";

    private static final String SELECT_COLUMNS =
            "SELECT id, code, customer_id, discount_type, discount_value, min_order_value, max_discount, valid_until " +
            "FROM vouchers ";

    private Voucher mapResultSetToVoucher(ResultSet rs) throws Exception {
        Object maxDiscount = rs.getObject("max_discount"); // NULL = không giới hạn mức giảm
        Voucher voucher = new Voucher(
                rs.getInt("id"),
                rs.getString("code"),
                rs.getString("discount_type"),
                rs.getDouble("discount_value"),
                rs.getDouble("min_order_value"),
                maxDiscount != null ? ((Number) maxDiscount).doubleValue() : null
        );
        voucher.setCustomerId(rs.getObject("customer_id", Integer.class));
        voucher.setValidUntil(rs.getObject("valid_until", LocalDateTime.class));
        return voucher;
    }

    private void setCustomerId(PreparedStatement ps, int index, Integer customerId) throws Exception {
        if (customerId != null) {
            ps.setInt(index, customerId);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    /**
     * Danh sách ưu đãi khả dụng hiển thị trong giỏ hàng: voucher riêng của khách lên trước, sau đó tới mã công khai
     */
    public List<Voucher> getUsableVouchers(Integer customerId) {
        List<Voucher> list = new ArrayList<>();
        String sql = SELECT_COLUMNS + "WHERE " + USABLE_CONDITION + " AND " + OWNER_CONDITION
                + " ORDER BY customer_id IS NULL, min_order_value ASC, id ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setCustomerId(ps, 1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToVoucher(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Tìm mã khuyến mãi còn dùng được theo code, trả về null nếu không hợp lệ / hết hạn
     * hoặc là voucher đổi điểm của khách khác
     */
    public Voucher getUsableVoucherByCode(String code, Integer customerId) {
        String sql = SELECT_COLUMNS + "WHERE code = ? AND " + USABLE_CONDITION + " AND " + OWNER_CONDITION;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code);
            setCustomerId(ps, 2, customerId);
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
