package dao;

import context.DBContext;
import model.Cart;
import model.CartItem;
import model.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Order;
import model.OrderDetail;
import model.ShopSettings;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderDAO - Tương tác dữ liệu đơn hàng
 */
public class OrderDAO extends BaseDAO {

    // All channels share the paid-order queue; checkbox state is kept outside the database.
    private static final String BARISTA_TYPES =
            "o.order_type IN ('DINE_IN', 'TAKEAWAY', 'ONLINE')";
    private static final String BARISTA_PAID =
            "EXISTS (SELECT 1 FROM payments pay WHERE pay.order_id = o.id AND (" +
            "pay.status = 'SUCCESS' OR (o.order_type = 'ONLINE' AND pay.payment_method = 'CASH' AND pay.status = 'PENDING')))";

    public List<Order> getOrdersForBarista() throws SQLException, ClassNotFoundException {
        // EXISTS avoids duplicating items when an order has multiple successful payments.
        String sql = "WITH eligible_orders AS (SELECT o.* FROM orders o WHERE "
                + BARISTA_TYPES + " AND " + BARISTA_PAID
                + " AND o.status IN ('CONFIRMED', 'PREPARING', 'READY') "
                + "AND EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.id)) "
                + "SELECT o.*, i.id AS item_id, i.product_id, i.product_name_at_time, "
                + "i.unit_price, i.customization, i.quantity, i.subtotal, "
                + "p.category_id, c.name AS category_name "
                + "FROM eligible_orders o "
                + "JOIN order_items i ON i.order_id = o.id "
                + "JOIN products p ON p.id = i.product_id "
                + "JOIN categories c ON c.id = p.category_id "
                + "ORDER BY CASE WHEN o.status = 'READY' THEN 1 ELSE 0 END, "
                + "CASE WHEN o.status <> 'READY' THEN o.created_at END ASC, "
                + "CASE WHEN o.status <> 'READY' THEN o.id END ASC, "
                + "CASE WHEN o.status = 'READY' THEN o.created_at END DESC, "
                + "CASE WHEN o.status = 'READY' THEN o.id END DESC, i.id ASC";
        Map<Integer, Order> orders = new LinkedHashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                Order order = orders.get(id);
                if (order == null) {
                    order = new Order();
                    order.setId(id);
                    order.setOrderCode(rs.getString("order_code"));
                    order.setCustomerId(rs.getObject("customer_id", Integer.class));
                    order.setCashierId(rs.getObject("cashier_id", Integer.class));
                    order.setShipperId(rs.getObject("shipper_id", Integer.class));
                    order.setOrderType(rs.getString("order_type"));
                    order.setStatus(rs.getString("status"));
                    order.setTotalAmount(rs.getBigDecimal("total_amount"));
                    order.setDiscountAmount(rs.getBigDecimal("discount_amount"));
                    order.setDeliveryFee(rs.getBigDecimal("delivery_fee"));
                    order.setFinalAmount(rs.getBigDecimal("final_amount"));
                    order.setRecipientName(rs.getString("recipient_name"));
                    order.setRecipientPhone(rs.getString("recipient_phone"));
                    order.setDeliveryAddress(rs.getString("delivery_address"));
                    order.setTableNumber(rs.getString("table_number"));
                    order.setOrderNote(rs.getString("order_note"));
                    order.setCreatedAt(rs.getObject("created_at", java.time.LocalDateTime.class));
                    orders.put(id, order);
                }
                OrderDetail item = new OrderDetail(
                        rs.getInt("item_id"), id, rs.getInt("product_id"),
                        rs.getString("product_name_at_time"), rs.getBigDecimal("unit_price"),
                        rs.getString("customization"), rs.getInt("quantity"),
                        rs.getBigDecimal("subtotal"), rs.getInt("category_id"),
                        rs.getString("category_name"));
                order.getItems().add(item);
            }
        }
        return new ArrayList<>(orders.values());
    }

    /**
     * Cập nhật trạng thái pha chế của Barista (CONFIRMED -> PREPARING -> READY).
     * Cho phép Barista chủ động bắt đầu làm đơn bất kỳ mà không bị gò bó bởi thứ tự FIFO.
     */
    public boolean updateBaristaOrderStatus(int orderId, String targetStatus)
            throws SQLException, ClassNotFoundException {
        if (orderId <= 0) {
            throw new IllegalArgumentException("Invalid order ID");
        }
        String sql;
        if ("PREPARING".equals(targetStatus)) {
            sql = "UPDATE orders o SET status = 'PREPARING' WHERE o.id = ? AND o.status = 'CONFIRMED' "
                    + "AND " + BARISTA_TYPES + " AND " + BARISTA_PAID;
        } else if ("READY".equals(targetStatus)) {
            sql = "UPDATE orders o SET status = 'READY' WHERE o.id = ? AND o.status IN ('CONFIRMED', 'PREPARING') "
                    + "AND " + BARISTA_TYPES + " AND " + BARISTA_PAID;
        } else {
            throw new IllegalArgumentException("Unsupported barista status");
        }
        try (Connection conn = getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                String currentStatus;
                try (PreparedStatement lock = conn.prepareStatement("SELECT status FROM orders WHERE id = ? FOR UPDATE")) {
                    lock.setInt(1, orderId);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        currentStatus = rs.getString("status");
                    }
                }
                if ("READY".equals(targetStatus)
                        || ("PREPARING".equals(targetStatus) && "CONFIRMED".equals(currentStatus))) {
                    InventoryMovementDAO.consumeOrder(conn, orderId);
                }
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, orderId);
                    boolean updated = ps.executeUpdate() > 0;
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

    public int getOrCreateCartOrder(int customerId) {
        String checkSql = "SELECT id FROM orders WHERE customer_id = ? AND status = 'PENDING' LIMIT 1";
        try (Connection conn = DBContext.getConnection();
                PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setInt(1, customerId);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create new pending order
        String insertSql = "INSERT INTO orders (order_code, customer_id, order_type, status, total_amount, discount_amount, delivery_fee, final_amount) "
                +
                "VALUES (?, ?, 'ONLINE', 'PENDING', 0, 0, 0, 0)";
        try (Connection conn = DBContext.getConnection();
                PreparedStatement psInsert = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            psInsert.setString(1, "CART-" + System.currentTimeMillis());
            psInsert.setInt(2, customerId);
            psInsert.executeUpdate();
            try (ResultSet rs = psInsert.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean addProductToCart(int orderId, int productId, int quantity) {
        // 1. Get product info
        String prodSql = "SELECT name, base_price FROM products WHERE id = ?";
        String productName = "";
        double unitPrice = 0;
        try (Connection conn = DBContext.getConnection();
                PreparedStatement psProd = conn.prepareStatement(prodSql)) {
            psProd.setInt(1, productId);
            try (ResultSet rs = psProd.executeQuery()) {
                if (rs.next()) {
                    productName = rs.getString("name");
                    unitPrice = rs.getDouble("base_price");
                } else {
                    return false; // Product not found
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        boolean itemAddedOrUpdated = false;
        // 2. Check if order_item exists
        String checkItemSql = "SELECT id, quantity FROM order_items WHERE order_id = ? AND product_id = ?";
        try (Connection conn = DBContext.getConnection();
                PreparedStatement psCheck = conn.prepareStatement(checkItemSql)) {
            psCheck.setInt(1, orderId);
            psCheck.setInt(2, productId);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    // Update
                    int newQty = rs.getInt("quantity") + quantity;
                    double newSubtotal = newQty * unitPrice;
                    String updateItemSql = "UPDATE order_items SET quantity = ?, subtotal = ? WHERE id = ?";
                    try (PreparedStatement psUpdate = conn.prepareStatement(updateItemSql)) {
                        psUpdate.setInt(1, newQty);
                        psUpdate.setDouble(2, newSubtotal);
                        psUpdate.setInt(3, rs.getInt("id"));
                        itemAddedOrUpdated = psUpdate.executeUpdate() > 0;
                    }
                } else {
                    // Insert
                    double subtotal = quantity * unitPrice;
                    String insertItemSql = "INSERT INTO order_items (order_id, product_id, product_name_at_time, unit_price, quantity, subtotal) "
                            +
                            "VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement psInsert = conn.prepareStatement(insertItemSql)) {
                        psInsert.setInt(1, orderId);
                        psInsert.setInt(2, productId);
                        psInsert.setString(3, productName);
                        psInsert.setDouble(4, unitPrice);
                        psInsert.setInt(5, quantity);
                        psInsert.setDouble(6, subtotal);
                        itemAddedOrUpdated = psInsert.executeUpdate() > 0;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        // 3. Update order totals
        if (itemAddedOrUpdated) {
            updateOrderTotals(orderId);
            return true;
        }
        return false;
    }

    private void updateOrderTotals(int orderId) {
        String sumSql = "SELECT SUM(subtotal) FROM order_items WHERE order_id = ?";
        double totalAmount = 0;
        try (Connection conn = DBContext.getConnection();
                PreparedStatement psSum = conn.prepareStatement(sumSql)) {
            psSum.setInt(1, orderId);
            try (ResultSet rs = psSum.executeQuery()) {
                if (rs.next()) {
                    totalAmount = rs.getDouble(1);
                }
            }

            String updateOrderSql = "UPDATE orders SET total_amount = ?, final_amount = ? + delivery_fee - discount_amount WHERE id = ?";
            try (PreparedStatement psUpdateOrder = conn.prepareStatement(updateOrderSql)) {
                psUpdateOrder.setDouble(1, totalAmount);
                psUpdateOrder.setDouble(2, totalAmount);
                psUpdateOrder.setInt(3, orderId);
                psUpdateOrder.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getCartItemTotalQuantity(int orderId) {
        String sql = "SELECT SUM(quantity) FROM order_items WHERE order_id = ?";
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** placeOrder: khách không còn đủ điểm Aura để trừ (VD vừa dùng điểm cho đơn khác) */
    public static final int NOT_ENOUGH_POINTS = -2;
    public static final int NOT_ENOUGH_STOCK = -3;

    public String getCheckoutStockError(List<CartItem> items) throws SQLException, ClassNotFoundException {
        try (Connection conn = DBContext.getConnection()) {
            return InventoryMovementDAO.checkoutStockError(conn, items, false);
        }
    }

    /**
     * Tạo đơn hàng từ các món đã chọn ở Checkout trong cùng một transaction:
     * orders + order_items + trừ điểm Aura khách dùng (users.total_points + loyalty_transactions REDEEM gắn với đơn).
     * Trả về id đơn hàng mới, NOT_ENOUGH_POINTS nếu không còn đủ điểm, hoặc -1 nếu lỗi.
     */
    public int placeOrder(Order order, List<CartItem> items, int pointsUsed) {
        String insertOrderSql = "INSERT INTO orders (order_code, customer_id, order_type, status, " +
                                "total_amount, discount_amount, delivery_fee, final_amount, " +
                                "recipient_name, recipient_phone, delivery_address, order_note, table_number, scheduled_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO order_items (order_id, product_id, product_name_at_time, unit_price, " +
                               "customization, quantity, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?)";
        // Chỉ trừ khi còn đủ điểm (tránh 2 đơn đặt cùng lúc dùng chung số điểm)
        String usePointsSql = "UPDATE users SET total_points = total_points - ? WHERE id = ? AND total_points >= ?";
        String redeemSql = "INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type) " +
                           "VALUES (?, ?, ?, 'REDEEM')";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            // 1. Tạo đơn hàng
            if (InventoryMovementDAO.checkoutStockError(conn, items, true) != null) {
                conn.rollback();
                return NOT_ENOUGH_STOCK;
            }
            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, order.getOrderCode());
                ps.setInt(2, order.getCustomerId());
                ps.setString(3, order.getOrderType());
                ps.setString(4, order.getStatus());
                ps.setDouble(5, order.getTotalAmount());
                ps.setDouble(6, order.getDiscountAmount());
                ps.setDouble(7, order.getDeliveryFee());
                ps.setDouble(8, order.getFinalAmount());
                ps.setString(9, order.getRecipientName());
                ps.setString(10, order.getRecipientPhone());
                ps.setString(11, order.getDeliveryAddress());
                ps.setString(12, order.getOrderNote());
                ps.setString(13, order.getTableNumber());
                ps.setTimestamp(14, order.getScheduledAt() == null ? null : Timestamp.valueOf(order.getScheduledAt()));
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return -1;
                    }
                    orderId = rs.getInt(1);
                }
            }

            // 2. Trừ điểm Aura dùng cho đơn (thất bại nếu không còn đủ điểm)
            if (pointsUsed > 0) {
                try (PreparedStatement ps = conn.prepareStatement(usePointsSql)) {
                    ps.setInt(1, pointsUsed);
                    ps.setInt(2, order.getCustomerId());
                    ps.setInt(3, pointsUsed);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return NOT_ENOUGH_POINTS;
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(redeemSql)) {
                    ps.setInt(1, order.getCustomerId());
                    ps.setInt(2, orderId);
                    ps.setInt(3, -pointsUsed);
                    ps.executeUpdate();
                }
            }

            // 3. Lưu các món của đơn
            try (PreparedStatement ps = conn.prepareStatement(insertItemSql)) {
                for (CartItem item : items) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, item.getProductId());
                    ps.setString(3, item.getProductName());
                    ps.setDouble(4, item.getUnitPrice());
                    ps.setString(5, buildCustomizationJson(item));
                    ps.setInt(6, item.getQuantity());
                    ps.setDouble(7, item.getSubtotal());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return orderId;
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            return -1;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    // ===================== Đơn hàng của khách (Thanh toán / Đơn hàng của tôi) =====================

    // Kèm số điểm Aura đã dùng cho đơn và lần thanh toán thành công gần nhất (nếu có)
    private static final String CUSTOMER_ORDER_SELECT =
            "SELECT o.*, pay.payment_method, pay.paid_at, " +
            "       (SELECT COALESCE(-SUM(lt.points_change), 0) FROM loyalty_transactions lt " +
            "        WHERE lt.order_id = o.id AND lt.transaction_type = 'REDEEM') AS points_used, " +
            "       (SELECT COALESCE(SUM(lt.points_change), 0) FROM loyalty_transactions lt " +
            "        WHERE lt.order_id = o.id AND lt.transaction_type = 'EARN') AS earned_points " +
            "FROM orders o " +
            "LEFT JOIN payments pay ON pay.id = (SELECT MAX(p2.id) FROM payments p2 " +
            "                                    WHERE p2.order_id = o.id) ";

    /**
     * Lấy 1 đơn kèm các món theo orderId (cho màn Bill, Payment)
     */
    public Order getOrderById(int orderId) {
        String sql = CUSTOMER_ORDER_SELECT + "WHERE o.id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            Map<Integer, Order> orders = new LinkedHashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = mapCustomerOrder(rs);
                    orders.put(order.getId(), order);
                }
            }
            loadOrderItems(conn, orders);
            return orders.get(orderId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh sách OrderDetail của 1 đơn hàng
     */
    public List<OrderDetail> getOrderDetails(int orderId) {
        Order order = getOrderById(orderId);
        return order != null ? order.getItems() : new ArrayList<>();
    }

    /**
     * Lấy các đơn hàng gần nhất
     */
    public List<Order> getRecentOrders(int limit) {
        String sql = CUSTOMER_ORDER_SELECT + "ORDER BY o.created_at DESC, o.id DESC LIMIT ?";
        List<Order> list = new ArrayList<>();
        Map<Integer, Order> orders = new LinkedHashMap<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order order = mapCustomerOrder(rs);
                    orders.put(order.getId(), order);
                    list.add(order);
                }
            }
            loadOrderItems(conn, orders);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy 1 đơn kèm các món, chỉ khi đơn thuộc về khách này (null nếu không có)
     */
    public Order getCustomerOrder(int orderId, int customerId) {
        String sql = CUSTOMER_ORDER_SELECT + "WHERE o.id = ? AND o.customer_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, customerId);
            Map<Integer, Order> orders = new LinkedHashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = mapCustomerOrder(rs);
                    orders.put(order.getId(), order);
                }
            }
            loadOrderItems(conn, orders);
            return orders.get(orderId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Danh sách đơn của khách, mới nhất trước.
     * statuses rỗng = mọi trạng thái; keyword: tìm theo mã đơn; date: lọc theo ngày đặt (null = mọi ngày)
     */
    public List<Order> getCustomerOrders(int customerId, List<String> statuses, String keyword,
                                         LocalDate date, int limit) {
        List<Object> params = new ArrayList<>();
        String sql = CUSTOMER_ORDER_SELECT + buildCustomerFilter(customerId, statuses, keyword, date, params)
                + " ORDER BY o.created_at DESC, o.id DESC LIMIT ?";
        params.add(limit);

        Map<Integer, Order> orders = new LinkedHashMap<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order order = mapCustomerOrder(rs);
                    orders.put(order.getId(), order);
                }
            }
            loadOrderItems(conn, orders);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>(orders.values());
    }

    /** Tổng số đơn khớp bộ lọc (để hiển thị "Hiển thị x / y đơn hàng") */
    public int countCustomerOrders(int customerId, List<String> statuses, String keyword, LocalDate date) {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM orders o " + buildCustomerFilter(customerId, statuses, keyword, date, params);
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** Số đơn của khách theo từng trạng thái (cho các tab lọc) */
    public Map<String, Integer> countCustomerOrdersByStatus(int customerId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) AS total FROM orders WHERE customer_id = ? GROUP BY status";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("status"), rs.getInt("total"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return counts;
    }

    /**
     * Khách hủy đơn chưa thanh toán: đổi sang CANCELLED và hoàn lại điểm Aura đã dùng cho đơn (nếu có)
     * trong cùng transaction. Lần trừ điểm của đơn bị xoá khỏi lịch sử điểm nên khách thấy như chưa từng dùng.
     */
    public boolean cancelPendingOrder(int orderId, int customerId) {
        String cancelSql = "UPDATE orders SET status = 'CANCELLED' " +
                           "WHERE id = ? AND customer_id = ? AND status = 'PENDING' " +
                           "AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.order_id = orders.id AND p.status = 'SUCCESS')";
        String usedPointsSql = "SELECT COALESCE(-SUM(points_change), 0) FROM loyalty_transactions " +
                               "WHERE order_id = ? AND customer_id = ? AND transaction_type = 'REDEEM'";
        String refundPointsSql = "UPDATE users SET total_points = total_points + ? WHERE id = ?";
        String insertRefundSql = "INSERT INTO loyalty_transactions(customer_id,order_id,points_change,transaction_type) " +
                                 "VALUES(?,?,?,'REFUND')";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(cancelSql)) {
                ps.setInt(1, orderId);
                ps.setInt(2, customerId);
                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            int usedPoints = 0;
            try (PreparedStatement ps = conn.prepareStatement(usedPointsSql)) {
                ps.setInt(1, orderId);
                ps.setInt(2, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        usedPoints = rs.getInt(1);
                    }
                }
            }
            if (usedPoints > 0) {
                try (PreparedStatement ps = conn.prepareStatement(refundPointsSql)) {
                    ps.setInt(1, usedPoints);
                    ps.setInt(2, customerId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(insertRefundSql)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, orderId);
                    ps.setInt(3, usedPoints);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE payments SET status='FAILED',payment_note=CONCAT(COALESCE(payment_note,''),' [Order cancelled]') " +
                    "WHERE order_id=? AND status='PENDING'")) {
                ps.setInt(1,orderId);
                ps.executeUpdate();
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

    private String buildCustomerFilter(int customerId, List<String> statuses, String keyword,
                                       LocalDate date, List<Object> params) {
        StringBuilder where = new StringBuilder("WHERE o.customer_id = ?");
        params.add(customerId);
        if (statuses != null && !statuses.isEmpty()) {
            where.append(" AND o.status IN (");
            for (int i = 0; i < statuses.size(); i++) {
                where.append(i > 0 ? ", ?" : "?");
                params.add(statuses.get(i));
            }
            where.append(")");
        }
        if (keyword != null && !keyword.isEmpty()) {
            where.append(" AND o.order_code LIKE ?");
            params.add("%" + keyword + "%");
        }
        if (date != null) {
            where.append(" AND DATE(o.created_at) = ?");
            params.add(java.sql.Date.valueOf(date));
        }
        return where.toString();
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private Order mapCustomerOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setOrderCode(rs.getString("order_code"));
        order.setCustomerId(rs.getObject("customer_id", Integer.class));
        order.setOrderType(rs.getString("order_type"));
        order.setStatus(rs.getString("status"));
        order.setTotalAmount(rs.getDouble("total_amount"));
        order.setDiscountAmount(rs.getDouble("discount_amount"));
        order.setDeliveryFee(rs.getDouble("delivery_fee"));
        order.setFinalAmount(rs.getDouble("final_amount"));
        order.setRecipientName(rs.getString("recipient_name"));
        order.setRecipientPhone(rs.getString("recipient_phone"));
        order.setDeliveryAddress(rs.getString("delivery_address"));
        order.setTableNumber(rs.getString("table_number"));
        order.setOrderNote(rs.getString("order_note"));
        order.setScheduledAt(rs.getObject("scheduled_at", LocalDateTime.class));
        order.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        order.setPointsUsed(rs.getInt("points_used"));
        order.setRewardPoints(rs.getInt("earned_points"));
        order.setPaymentMethod(rs.getString("payment_method"));
        order.setPaidAt(rs.getObject("paid_at", LocalDateTime.class));
        return order;
    }

    /** Nạp các món (kèm ảnh món) cho các đơn đã lấy, bằng 1 câu truy vấn */
    private void loadOrderItems(Connection conn, Map<Integer, Order> orders) throws SQLException {
        if (orders.isEmpty()) {
            return;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT oi.*, CASE WHEN pi.product_id IS NOT NULL THEN CONCAT('product-image?id=', p.id) ELSE p.image_url END AS image_url FROM order_items oi " +
                "LEFT JOIN products p ON p.id = oi.product_id " +
                "LEFT JOIN product_images pi ON pi.product_id = p.id WHERE oi.order_id IN (");
        int index = 0;
        for (int i = 0; i < orders.size(); i++) {
            sql.append(i > 0 ? ", ?" : "?");
        }
        sql.append(") ORDER BY oi.id");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (Integer orderId : orders.keySet()) {
                ps.setInt(++index, orderId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderDetail item = new OrderDetail();
                    item.setId(rs.getInt("id"));
                    item.setOrderId(rs.getInt("order_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name_at_time"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setCustomization(rs.getString("customization"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setSubtotal(rs.getBigDecimal("subtotal"));
                    item.setImageUrl(rs.getString("image_url"));
                    orders.get(item.getOrderId()).getItems().add(item);
                }
            }
        }
    }

    /**
     * Ghi tuỳ chọn món vào cột customization (JSON), VD: {"size":"L","toppings":["Kem whipping"]}
     */
    private String buildCustomizationJson(CartItem item) {
        StringBuilder json = new StringBuilder("{\"size\":").append(toJsonString(item.getSize()));
        json.append(",\"toppings\":[");
        for (int i = 0; i < item.getToppings().size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(toJsonString(item.getToppings().get(i)));
        }
        return json.append("]}").toString();
    }

    private String toJsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * Định dạng chuỗi tuỳ chọn món tại quầy thành chuỗi JSON hợp lệ với cột customization (JSON) của MySQL.
     */
    private String formatCustomizationJson(String customStr) {
        if (customStr == null || customStr.trim().isEmpty()) {
            return null;
        }
        String trimmed = customStr.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        String size = null;
        List<String> toppings = new ArrayList<>();
        List<String> extras = new ArrayList<>();

        String[] parts = trimmed.split("\\s*\\|\\s*");
        for (String part : parts) {
            String p = part.trim();
            if (p.toLowerCase().startsWith("size ")) {
                size = p.substring(5).trim();
            } else if (p.startsWith("+ ")) {
                String toppingsPart = p.substring(2).trim();
                for (String t : toppingsPart.split("\\s*,\\s*")) {
                    if (!t.isEmpty()) {
                        toppings.add(t);
                    }
                }
            } else if (!p.isEmpty()) {
                extras.add(p);
            }
        }

        StringBuilder sb = new StringBuilder("{");
        if (size != null) {
            sb.append("\"size\":").append(toJsonString(size));
        } else {
            sb.append("\"size\":null");
        }

        sb.append(",\"toppings\":[");
        for (int i = 0; i < toppings.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(toJsonString(toppings.get(i)));
        }
        sb.append("]");

        if (!extras.isEmpty()) {
            sb.append(",\"notes\":").append(toJsonString(String.join(", ", extras)));
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Tạo và thanh toán đơn hàng tại quầy (Counter Order POS)
     */
    public int createCounterOrder(Order order, List<OrderDetail> items, String paymentMethod,
                                  int pointsUsed, String paymentNote) {
        return createCounterOrder(order, items, paymentMethod, pointsUsed, paymentNote,
                new ShopSettingDAO().getSettings());
    }

    public int createCounterOrder(Order order, List<OrderDetail> items, String paymentMethod,
                                  int pointsUsed, String paymentNote, ShopSettings settings) {
        boolean bankTransfer = "BANK_TRANSFER".equalsIgnoreCase(paymentMethod);
        String orderStatus = bankTransfer ? "PENDING" : "CONFIRMED";
        String paymentStatus = bankTransfer ? "PENDING" : "SUCCESS";
        String insertOrderSql = "INSERT INTO orders (order_code, customer_id, cashier_id, " +
                "order_type, status, total_amount, discount_amount, delivery_fee, final_amount, " +
                "recipient_name, recipient_phone, table_number, order_note, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, NOW())";

        String insertItemSql = "INSERT INTO order_items (order_id, product_id, product_name_at_time, " +
                "unit_price, quantity, subtotal, customization) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        String insertPaymentSql = "INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at, payment_note) " +
                "VALUES (?, ?, ?, ?, ?, CASE WHEN ?='SUCCESS' THEN NOW() ELSE NULL END, ?)";

        String redeemPointsSql = "INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type) " +
                "VALUES (?, ?, ?, 'REDEEM')";
        String deductUserPointsSql = "UPDATE users SET total_points = total_points - ? WHERE id = ?";

        String earnPointsSql = "INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type) " +
                "VALUES (?, ?, ?, 'EARN')";
        String addUserPointsSql = "UPDATE users SET total_points = total_points + ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            if (order.getCustomerId() != null && pointsUsed > 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT total_points FROM users WHERE id=? AND role='CUSTOMER' AND status='ACTIVE' FOR UPDATE")) {
                    ps.setInt(1, order.getCustomerId());
                    try (ResultSet rs=ps.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) < pointsUsed
                                || pointsUsed % settings.getPointsStep() != 0
                                || settings.getPointsDiscount(pointsUsed) > order.getTotalAmount()) {
                            conn.rollback();
                            return -1;
                        }
                    }
                }
            }

            int orderId = 0;
            try (PreparedStatement ps = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, order.getOrderCode());
                ps.setObject(2, order.getCustomerId(), Types.INTEGER);
                ps.setObject(3, order.getCashierId(), Types.INTEGER);
                ps.setString(4, order.getOrderType());
                ps.setString(5, orderStatus);
                ps.setDouble(6, order.getTotalAmount());
                ps.setDouble(7, order.getDiscountAmount());
                ps.setDouble(8, order.getFinalAmount());
                ps.setString(9, order.getRecipientName());
                ps.setString(10, order.getRecipientPhone());
                ps.setString(11, order.getTableNumber());
                ps.setString(12, order.getOrderNote());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        orderId = rs.getInt(1);
                    }
                }
            }

            if (orderId <= 0) {
                conn.rollback();
                return -1;
            }

            try (PreparedStatement ps = conn.prepareStatement(insertItemSql)) {
                for (OrderDetail item : items) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, item.getProductId());
                    ps.setString(3, item.getProductName());
                    ps.setBigDecimal(4, item.getUnitPrice());
                    ps.setInt(5, item.getQuantity());
                    ps.setBigDecimal(6, item.getSubtotal());
                    String customJson = formatCustomizationJson(item.getCustomization());
                    if (customJson != null) {
                        ps.setString(7, customJson);
                    } else {
                        ps.setNull(7, Types.VARCHAR);
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = conn.prepareStatement(insertPaymentSql)) {
                ps.setInt(1, orderId);
                ps.setString(2, paymentMethod != null ? paymentMethod : "CASH");
                ps.setString(3, (bankTransfer ? "QR-" : "POS-") + order.getOrderCode());
                ps.setDouble(4, order.getFinalAmount());
                ps.setString(5, paymentStatus);
                ps.setString(6, paymentStatus);
                ps.setString(7, paymentNote);
                ps.executeUpdate();
            }

            if (order.getCustomerId() != null && order.getCustomerId() > 0) {
                if (pointsUsed > 0) {
                    try (PreparedStatement ps = conn.prepareStatement(redeemPointsSql)) {
                        ps.setInt(1, order.getCustomerId());
                        ps.setInt(2, orderId);
                        ps.setInt(3, -pointsUsed);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(deductUserPointsSql)) {
                        ps.setInt(1, pointsUsed);
                        ps.setInt(2, order.getCustomerId());
                        if (ps.executeUpdate() != 1) {
                            conn.rollback();
                            return -1;
                        }
                    }
                }

                int earnedPoints = bankTransfer ? 0
                        : settings.calculateEarnedPoints(order.getTotalAmount() - order.getDiscountAmount());
                if (earnedPoints > 0) {
                    try (PreparedStatement ps = conn.prepareStatement(earnPointsSql)) {
                        ps.setInt(1, order.getCustomerId());
                        ps.setInt(2, orderId);
                        ps.setInt(3, earnedPoints);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(addUserPointsSql)) {
                        ps.setInt(1, earnedPoints);
                        ps.setInt(2, order.getCustomerId());
                        ps.executeUpdate();
                    }
                }
            }

            conn.commit();
            return orderId;
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return -1;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Danh sách các đơn quầy tạo trong ngày hôm nay
     */
    public List<Order> getCounterOrdersToday() {
        String sql = CUSTOMER_ORDER_SELECT + 
                     "WHERE DATE(o.created_at) = CURDATE() " +
                     "  AND o.cashier_id IS NOT NULL AND o.order_type IN ('DINE_IN', 'TAKEAWAY') " +
                     "ORDER BY o.created_at DESC, o.id DESC";

        Map<Integer, Order> orders = new LinkedHashMap<>();
        List<Order> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Order order = mapCustomerOrder(rs);
                orders.put(order.getId(), order);
                list.add(order);
            }
            loadOrderItems(conn, orders);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Hủy đơn hàng bởi Thu ngân với lý do và hoàn trả điểm
     * Chỉ cho phép hủy khi đơn ở trạng thái PENDING hoặc CONFIRMED (chưa pha chế).
     * Trả về null nếu thành công, hoặc chuỗi thông báo lỗi cụ thể.
     */
    public String cancelCounterOrderWithReason(int orderId) {
        String querySql = "SELECT o.id, o.status, o.customer_id FROM orders o WHERE o.id = ? FOR UPDATE";
        String cancelSql = "UPDATE orders SET status = 'CANCELLED' WHERE id = ? AND status IN ('PENDING', 'CONFIRMED')";
        
        String queryRedeemPointsSql = "SELECT points_change FROM loyalty_transactions WHERE order_id = ? AND transaction_type = 'REDEEM' LIMIT 1";
        String refundPointsSql = "INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type) VALUES (?, ?, ?, 'REFUND')";
        String addBackUserPointsSql = "UPDATE users SET total_points = total_points + ? WHERE id = ?";

        String queryEarnPointsSql = "SELECT points_change FROM loyalty_transactions WHERE order_id = ? AND transaction_type = 'EARN' LIMIT 1";
        String adjustEarnedPointsSql = "INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type) VALUES (?, ?, ?, 'ADJUST')";
        String deductAwardedUserPointsSql = "UPDATE users SET total_points = GREATEST(0, total_points - ?) WHERE id = ?";

        String cancelPaymentSql = "UPDATE payments SET status = 'FAILED', payment_note = CONCAT(COALESCE(payment_note, ''), ' [Đã hủy đơn bởi Thu ngân]') WHERE order_id = ?";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            String currentStatus;
            Integer customerId;
            try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return "Không tìm thấy đơn hàng #" + orderId + ".";
                    }
                    currentStatus = rs.getString("status");
                    customerId = rs.getObject("customer_id", Integer.class);
                }
            }

            if ("CANCELLED".equals(currentStatus)) {
                conn.rollback();
                return "Đơn hàng này đã được hủy trước đó.";
            }
            if ("COMPLETED".equals(currentStatus)) {
                conn.rollback();
                return "Đơn hàng đã hoàn thành, không thể hủy.";
            }
            if ("PREPARING".equals(currentStatus) || "READY".equals(currentStatus) || "DELIVERING".equals(currentStatus)) {
                conn.rollback();
                return "Đơn hàng đang được Barista pha chế hoặc đang giao, không thể hủy.";
            }
            if (!"PENDING".equals(currentStatus) && !"CONFIRMED".equals(currentStatus)) {
                conn.rollback();
                return "Trạng thái đơn hàng không hợp lệ để hủy (" + currentStatus + ").";
            }

            // Thực hiện hủy đơn
            try (PreparedStatement ps = conn.prepareStatement(cancelSql)) {
                ps.setInt(1, orderId);
                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return "Không thể cập nhật trạng thái hủy đơn.";
                }
            }

            // Hoàn lại điểm tích lũy và trừ điểm thưởng nếu có
            if (customerId != null && customerId > 0) {
                // 1. Điểm đã dùng (REDEEM - mang giá trị âm) -> Hoàn lại (REFUND - mang giá trị dương)
                int redeemedPoints = 0;
                try (PreparedStatement ps = conn.prepareStatement(queryRedeemPointsSql)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            redeemedPoints = Math.abs(rs.getInt("points_change"));
                        }
                    }
                }
                if (redeemedPoints > 0) {
                    try (PreparedStatement ps = conn.prepareStatement(refundPointsSql)) {
                        ps.setInt(1, customerId);
                        ps.setInt(2, orderId);
                        ps.setInt(3, redeemedPoints);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(addBackUserPointsSql)) {
                        ps.setInt(1, redeemedPoints);
                        ps.setInt(2, customerId);
                        ps.executeUpdate();
                    }
                }

                // 2. Điểm thưởng đã tích (EARN - mang giá trị dương) -> Thu hồi lại (ADJUST - mang giá trị âm)
                int earnedPoints = 0;
                try (PreparedStatement ps = conn.prepareStatement(queryEarnPointsSql)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            earnedPoints = rs.getInt("points_change");
                        }
                    }
                }
                if (earnedPoints > 0) {
                    try (PreparedStatement ps = conn.prepareStatement(adjustEarnedPointsSql)) {
                        ps.setInt(1, customerId);
                        ps.setInt(2, orderId);
                        ps.setInt(3, -earnedPoints);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(deductAwardedUserPointsSql)) {
                        ps.setInt(1, earnedPoints);
                        ps.setInt(2, customerId);
                        ps.executeUpdate();
                    }
                }
            }

            // Cập nhật payment
            try (PreparedStatement ps = conn.prepareStatement(cancelPaymentSql)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            conn.commit();
            return null; // Thành công
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return "Lỗi máy chủ khi hủy đơn: " + e.getMessage();
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public boolean cancelCounterOrder(int orderId) {
        return cancelCounterOrderWithReason(orderId) == null;
    }

    /**
     * Thu ngân chuyển đơn Online sang trạng thái đang giao hàng
     */
    public boolean deliverCounterOrder(int orderId) {
        String sql = "UPDATE orders SET status = 'DELIVERING' WHERE id = ? AND status = 'READY'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Thu ngân đánh dấu đơn tại quầy / giao hàng đã hoàn tất (khách đã nhận món).
     * Chỉ cho phép hoàn tất khi Barista đã làm xong (READY) hoặc đơn đang giao (DELIVERING).
     */
    public boolean completeCounterOrder(int orderId) {
        String sql = "UPDATE orders SET status = 'COMPLETED' WHERE id = ? AND status = 'READY' " +
                "AND order_type IN ('DINE_IN','TAKEAWAY')";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
