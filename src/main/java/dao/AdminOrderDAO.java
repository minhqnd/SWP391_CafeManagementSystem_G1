package dao;

import model.AdminOrderSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Truy vấn dành riêng cho màn hình quản lý đơn hàng của Admin. */
public class AdminOrderDAO extends BaseDAO {
    public List<AdminOrderSummary> findAll() throws SQLException, ClassNotFoundException {
        String sql = "SELECT o.id, o.order_code, o.order_type, o.status, o.final_amount, o.created_at, o.table_number, "
                + "COALESCE(c.full_name, o.recipient_name, 'Khách vãng lai') AS customer_name, "
                + "COALESCE(c.phone, o.recipient_phone, '') AS customer_phone, "
                + "COALESCE(cashier.full_name, 'Online') AS cashier_name, o.shipper_id, shipper.full_name AS shipper_name, "
                + "pay.payment_method, pay.status AS payment_status "
                + "FROM orders o LEFT JOIN users c ON c.id = o.customer_id "
                + "LEFT JOIN users cashier ON cashier.id = o.cashier_id LEFT JOIN users shipper ON shipper.id = o.shipper_id "
                + "LEFT JOIN payments pay ON pay.id = (SELECT p.id FROM payments p WHERE p.order_id = o.id ORDER BY p.id DESC LIMIT 1) "
                + "ORDER BY o.created_at DESC, o.id DESC";
        List<AdminOrderSummary> orders = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AdminOrderSummary order = new AdminOrderSummary();
                order.setId(rs.getInt("id"));
                order.setOrderCode(rs.getString("order_code"));
                order.setOrderType(rs.getString("order_type"));
                order.setStatus(rs.getString("status"));
                order.setFinalAmount(rs.getDouble("final_amount"));
                order.setCreatedAt(rs.getObject("created_at", java.time.LocalDateTime.class));
                order.setTableNumber(rs.getString("table_number"));
                order.setCustomerName(rs.getString("customer_name"));
                order.setCustomerPhone(rs.getString("customer_phone"));
                order.setCashierName(rs.getString("cashier_name"));
                order.setShipperId(rs.getObject("shipper_id", Integer.class));
                order.setShipperName(rs.getString("shipper_name"));
                order.setPaymentMethod(rs.getString("payment_method"));
                order.setPaymentStatus(rs.getString("payment_status"));
                orders.add(order);
            }
        }
        return orders;
    }

    public boolean updateStatus(int id, String status) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                String currentStatus;
                try (PreparedStatement lock = conn.prepareStatement("SELECT status FROM orders WHERE id = ? FOR UPDATE")) {
                    lock.setInt(1, id);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        currentStatus = rs.getString("status");
                    }
                }
                boolean notYetPrepared = "PENDING".equals(currentStatus) || "CONFIRMED".equals(currentStatus);
                boolean startsPreparation = "PREPARING".equals(status) || "READY".equals(status)
                        || "DELIVERING".equals(status) || "COMPLETED".equals(status);
                if (notYetPrepared && startsPreparation) {
                    InventoryMovementDAO.consumeOrder(conn, id);
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET status = ? WHERE id = ?")) {
                    ps.setString(1, status);
                    ps.setInt(2, id);
                    boolean updated = ps.executeUpdate() == 1;
                    if (updated) conn.commit(); else conn.rollback();
                    return updated;
                }
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException) throw (SQLException) e;
                throw new SQLException("Could not update order inventory", e);
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    public boolean assignShipper(int id, Integer shipperId) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE orders SET shipper_id = ? WHERE id = ? AND order_type = 'ONLINE'";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (shipperId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, shipperId);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }
}
