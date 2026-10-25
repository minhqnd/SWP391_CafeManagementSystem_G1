package dao;

import model.Delivery;
import model.DeliveryIssue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DeliveryDAO - Giao hàng đơn online (bảng deliveries + delivery_issues, UC26-UC30).
 * Mọi thao tác của shipper đều kèm điều kiện shipper_id và trạng thái hiện tại ngay trong câu SQL
 * để không thao tác nhầm đơn của người khác hoặc bấm 2 lần.
 */
public class DeliveryDAO extends BaseDAO {

    public static final int OK = 1;
    public static final int NOT_ALLOWED = 0;
    public static final int COD_MISMATCH = -2;

    private static final String SELECT_DELIVERY =
            "SELECT d.*, o.order_code, o.status AS order_status, o.recipient_name, o.recipient_phone, " +
            "       o.delivery_address, o.order_note, o.scheduled_at, o.created_at AS order_created_at, " +
            "       o.final_amount, o.delivery_fee, c.full_name AS customer_name, s.full_name AS shipper_name, " +
            "       r.full_name AS cod_receiver_name, " +
            "       (SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi WHERE oi.order_id = o.id) AS item_count " +
            "FROM deliveries d " +
            "JOIN orders o ON o.id = d.order_id " +
            "JOIN users s ON s.id = d.shipper_id " +
            "LEFT JOIN users c ON c.id = o.customer_id " +
            "LEFT JOIN users r ON r.id = d.cod_receiver_id ";

    /** UC26: đơn đang được phân công cho shipper (chưa giao xong), đơn đang giao lên đầu */
    public List<Delivery> getActiveDeliveries(int shipperId) throws SQLException, ClassNotFoundException {
        String sql = SELECT_DELIVERY +
                "WHERE d.shipper_id = ? AND d.status IN ('ASSIGNED', 'ACCEPTED', 'DELIVERING') " +
                "AND o.status <> 'CANCELLED' " +
                "ORDER BY FIELD(d.status, 'DELIVERING', 'ACCEPTED', 'ASSIGNED'), " +
                "COALESCE(o.scheduled_at, o.created_at), d.id";
        return query(sql, shipperId);
    }

    /** Đơn shipper đã giao xong trong 1 ngày */
    public List<Delivery> getDeliveredOn(int shipperId, LocalDate date) throws SQLException, ClassNotFoundException {
        String sql = SELECT_DELIVERY +
                "WHERE d.shipper_id = ? AND d.status = 'DELIVERED' AND DATE(d.delivered_at) = ? " +
                "ORDER BY d.delivered_at DESC";
        List<Delivery> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shipperId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** UC30: tiền COD đã thu nhưng chưa nộp về quỹ */
    public List<Delivery> getCodHolding(int shipperId) throws SQLException, ClassNotFoundException {
        String sql = SELECT_DELIVERY +
                "WHERE d.shipper_id = ? AND d.status = 'DELIVERED' AND d.cod_amount > 0 " +
                "AND d.cod_handed_over_at IS NULL ORDER BY d.delivered_at";
        return query(sql, shipperId);
    }

    /** Lịch sử nộp tiền COD gần đây */
    public List<Delivery> getCodHistory(int shipperId) throws SQLException, ClassNotFoundException {
        String sql = SELECT_DELIVERY +
                "WHERE d.shipper_id = ? AND d.cod_handed_over_at IS NOT NULL " +
                "ORDER BY d.cod_handed_over_at DESC, d.id DESC LIMIT 30";
        return query(sql, shipperId);
    }

    /** UC28: chi tiết 1 lần giao của chính shipper này (null nếu không thuộc shipper) */
    public Delivery getForShipper(int id, int shipperId) throws SQLException, ClassNotFoundException {
        String sql = SELECT_DELIVERY + "WHERE d.id = ? AND d.shipper_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, shipperId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Thông tin giao hàng của 1 đơn (màn Quản lý đơn của Admin) */
    public Delivery getByOrderId(int orderId) throws SQLException, ClassNotFoundException {
        List<Delivery> list = query(SELECT_DELIVERY + "WHERE d.order_id = ?", orderId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<DeliveryIssue> getIssues(int deliveryId) throws SQLException, ClassNotFoundException {
        String sql = "SELECT * FROM delivery_issues WHERE delivery_id = ? ORDER BY created_at DESC, id DESC";
        List<DeliveryIssue> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deliveryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DeliveryIssue issue = new DeliveryIssue();
                    issue.setId(rs.getInt("id"));
                    issue.setDeliveryId(rs.getInt("delivery_id"));
                    issue.setIssueType(rs.getString("issue_type"));
                    issue.setDescription(rs.getString("description"));
                    issue.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(issue);
                }
            }
        }
        return list;
    }

    /** UC27: shipper nhận đơn được phân công */
    public boolean accept(int id, int shipperId) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE deliveries d JOIN orders o ON o.id = d.order_id " +
                     "SET d.status = 'ACCEPTED', d.accepted_at = NOW() " +
                     "WHERE d.id = ? AND d.shipper_id = ? AND d.status = 'ASSIGNED' AND o.status <> 'CANCELLED'";
        return update(sql, id, shipperId) > 0;
    }

    /** UC27: lấy hàng tại quán và bắt đầu giao - chỉ khi Barista đã pha xong (READY) */
    public boolean startDelivery(int id, int shipperId) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE deliveries d JOIN orders o ON o.id = d.order_id " +
                     "SET d.status = 'DELIVERING', d.picked_up_at = NOW(), o.status = 'DELIVERING' " +
                     "WHERE d.id = ? AND d.shipper_id = ? AND d.status = 'ACCEPTED' AND o.status = 'READY'";
        return update(sql, id, shipperId) > 0;
    }

    /**
     * UC27 + UC30: giao thành công. Trong 1 transaction:
     * - đơn COD: số tiền thu phải đúng số phải thu; payments COD PENDING -> SUCCESS, cộng điểm Aura cho khách
     * - deliveries -> DELIVERED, orders -> COMPLETED
     */
    public int completeDelivery(int id, int shipperId, BigDecimal codCollected, String note)
            throws SQLException, ClassNotFoundException {
        String lockSql = "SELECT d.status, d.cod_amount, d.order_id, o.customer_id, o.total_amount, o.discount_amount " +
                         "FROM deliveries d JOIN orders o ON o.id = d.order_id " +
                         "WHERE d.id = ? AND d.shipper_id = ? FOR UPDATE";
        String payCodSql = "UPDATE payments SET status = 'SUCCESS', paid_at = NOW() " +
                           "WHERE order_id = ? AND status = 'PENDING' AND transaction_code LIKE 'COD-%'";
        String deliveredSql = "UPDATE deliveries SET status = 'DELIVERED', delivered_at = NOW(), cod_collected = ?, " +
                              "delivery_note = ? WHERE id = ?";
        String completeOrderSql = "UPDATE orders SET status = 'COMPLETED' WHERE id = ? AND status = 'DELIVERING'";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int orderId;
                Integer customerId;
                BigDecimal codAmount;
                BigDecimal goods;
                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setInt(1, id);
                    ps.setInt(2, shipperId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !"DELIVERING".equals(rs.getString("status"))) {
                            conn.rollback();
                            return NOT_ALLOWED;
                        }
                        orderId = rs.getInt("order_id");
                        customerId = rs.getObject("customer_id", Integer.class);
                        codAmount = rs.getBigDecimal("cod_amount");
                        goods = rs.getBigDecimal("total_amount").subtract(rs.getBigDecimal("discount_amount"));
                    }
                }

                boolean isCod = codAmount.signum() > 0;
                if (isCod) {
                    if (codCollected == null || codCollected.compareTo(codAmount) != 0) {
                        conn.rollback();
                        return COD_MISMATCH;
                    }
                    try (PreparedStatement ps = conn.prepareStatement(payCodSql)) {
                        ps.setInt(1, orderId);
                        ps.executeUpdate();
                    }
                    // Đơn COD chưa được cộng điểm lúc đặt -> cộng khi khách đã trả tiền (không cộng trùng)
                    if (customerId != null) {
                        PaymentDAO.earnPointsOnce(conn, customerId, orderId, goods,
                                new ShopSettingDAO().getSettings().getPointsEarnUnit());
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(deliveredSql)) {
                    if (isCod) ps.setBigDecimal(1, codCollected); else ps.setNull(1, java.sql.Types.DECIMAL);
                    ps.setString(2, note == null || note.isEmpty() ? null : note);
                    ps.setInt(3, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(completeOrderSql)) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }
                conn.commit();
                return OK;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** UC29: báo sự cố cho đơn chưa giao xong */
    public boolean reportIssue(int id, int shipperId, String issueType, String description)
            throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO delivery_issues (delivery_id, issue_type, description) " +
                     "SELECT d.id, ?, ? FROM deliveries d " +
                     "WHERE d.id = ? AND d.shipper_id = ? AND d.status IN ('ASSIGNED', 'ACCEPTED', 'DELIVERING')";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, issueType);
            ps.setString(2, description);
            ps.setInt(3, id);
            ps.setInt(4, shipperId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * UC30: nộp toàn bộ tiền COD đang giữ cho Thu ngân.
     * Trả về tổng tiền đã nộp, hoặc null nếu không có khoản nào đang giữ.
     */
    public BigDecimal handOverCod(int shipperId, int cashierId) throws SQLException, ClassNotFoundException {
        String sumSql = "SELECT COALESCE(SUM(cod_collected), 0) AS total, COUNT(*) AS cnt FROM deliveries " +
                        "WHERE shipper_id = ? AND status = 'DELIVERED' AND cod_amount > 0 " +
                        "AND cod_handed_over_at IS NULL FOR UPDATE";
        String updateSql = "UPDATE deliveries SET cod_handed_over_at = NOW(), cod_receiver_id = ? " +
                           "WHERE shipper_id = ? AND status = 'DELIVERED' AND cod_amount > 0 " +
                           "AND cod_handed_over_at IS NULL";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal total;
                try (PreparedStatement ps = conn.prepareStatement(sumSql)) {
                    ps.setInt(1, shipperId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt("cnt") == 0) {
                            conn.rollback();
                            return null;
                        }
                        total = rs.getBigDecimal("total");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, cashierId);
                    ps.setInt(2, shipperId);
                    ps.executeUpdate();
                }
                conn.commit();
                return total;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private List<Delivery> query(String sql, int param) throws SQLException, ClassNotFoundException {
        List<Delivery> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private int update(String sql, int id, int shipperId) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, shipperId);
            return ps.executeUpdate();
        }
    }

    private Delivery map(ResultSet rs) throws SQLException {
        Delivery d = new Delivery();
        d.setId(rs.getInt("id"));
        d.setOrderId(rs.getInt("order_id"));
        d.setShipperId(rs.getInt("shipper_id"));
        d.setStatus(rs.getString("status"));
        d.setCodAmount(rs.getBigDecimal("cod_amount"));
        d.setCodCollected(rs.getBigDecimal("cod_collected"));
        d.setCodHandedOverAt(rs.getTimestamp("cod_handed_over_at"));
        d.setCodReceiverName(rs.getString("cod_receiver_name"));
        d.setAssignedAt(rs.getTimestamp("assigned_at"));
        d.setAcceptedAt(rs.getTimestamp("accepted_at"));
        d.setPickedUpAt(rs.getTimestamp("picked_up_at"));
        d.setDeliveredAt(rs.getTimestamp("delivered_at"));
        d.setDeliveryNote(rs.getString("delivery_note"));
        d.setOrderCode(rs.getString("order_code"));
        d.setOrderStatus(rs.getString("order_status"));
        d.setCustomerName(rs.getString("customer_name"));
        d.setRecipientName(rs.getString("recipient_name"));
        d.setRecipientPhone(rs.getString("recipient_phone"));
        d.setDeliveryAddress(rs.getString("delivery_address"));
        d.setOrderNote(rs.getString("order_note"));
        d.setScheduledAt(rs.getTimestamp("scheduled_at"));
        d.setOrderCreatedAt(rs.getTimestamp("order_created_at"));
        d.setFinalAmount(rs.getBigDecimal("final_amount"));
        d.setDeliveryFee(rs.getBigDecimal("delivery_fee"));
        d.setShipperName(rs.getString("shipper_name"));
        d.setItemCount(rs.getInt("item_count"));
        return d;
    }
}
