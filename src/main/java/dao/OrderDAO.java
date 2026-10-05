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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Order;
import model.OrderDetail;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderDAO - Tương tác dữ liệu đơn hàng
 */
public class OrderDAO extends BaseDAO {

    // All channels share orders. No per-item preparation status exists in the schema.
    private static final String BARISTA_TYPES =
            "o.order_type IN ('DINE_IN', 'TAKEAWAY', 'ONLINE')";
    private static final String BARISTA_PAID =
            "EXISTS (SELECT 1 FROM payments pay WHERE pay.order_id = o.id AND pay.status = 'SUCCESS')";

    public List<Order> getOrdersForBarista() throws SQLException, ClassNotFoundException {
        // Limit orders BEFORE joining items; multiple successful payments must not duplicate items.
        String sql = "WITH eligible_orders AS (SELECT o.* FROM orders o WHERE "
                + BARISTA_TYPES + " AND " + BARISTA_PAID
                + " AND o.status IN ('CONFIRMED', 'PREPARING', 'READY') "
                + "AND EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.id)), "
                + "recent_ready AS (SELECT * FROM eligible_orders WHERE status = 'READY' "
                + "ORDER BY created_at DESC, id DESC LIMIT 3) "
                + "SELECT o.*, i.id AS item_id, i.product_id, i.product_name_at_time, "
                + "i.unit_price, i.customization, i.quantity, i.subtotal, "
                + "p.category_id, c.name AS category_name "
                + "FROM (SELECT * FROM eligible_orders WHERE status IN ('CONFIRMED', 'PREPARING') "
                + "UNION ALL SELECT * FROM recent_ready) o "
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
                    order.setVoucherId(rs.getObject("voucher_id", Integer.class));
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
                order.getItems().add(new OrderDetail(
                        rs.getInt("item_id"), id, rs.getInt("product_id"),
                        rs.getString("product_name_at_time"), rs.getBigDecimal("unit_price"),
                        rs.getString("customization"), rs.getInt("quantity"),
                        rs.getBigDecimal("subtotal"), rs.getInt("category_id"),
                        rs.getString("category_name")));
            }
        }
        return new ArrayList<>(orders.values());
    }

    /**
     * Preserve the dashboard's global FIFO rule across all category filters.
     * Lock the queue and compare status so concurrent clicks cannot skip a stage.
     */
    public boolean updateBaristaOrderStatus(int orderId, String targetStatus)
            throws SQLException, ClassNotFoundException {
        String expectedStatus;
        if ("PREPARING".equals(targetStatus)) {
            expectedStatus = "CONFIRMED";
        } else if ("READY".equals(targetStatus)) {
            expectedStatus = "PREPARING";
        } else {
            throw new IllegalArgumentException("Unsupported barista status");
        }
        if (orderId <= 0) {
            throw new IllegalArgumentException("Invalid order ID");
        }
        String queueSql = "SELECT o.id, o.status FROM orders o WHERE " + BARISTA_TYPES
                + " AND " + BARISTA_PAID
                + " AND o.status IN ('CONFIRMED', 'PREPARING') "
                + "AND EXISTS (SELECT 1 FROM order_items i WHERE i.order_id = o.id) "
                + "ORDER BY o.created_at, o.id FOR UPDATE";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(queueSql);
                     ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt("id") != orderId
                            || !expectedStatus.equals(rs.getString("status"))) {
                        conn.rollback();
                        return false;
                    }
                }
                String updateSql = "UPDATE orders o SET status = ? WHERE o.id = ? AND o.status = ? "
                        + "AND " + BARISTA_PAID;
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, targetStatus);
                    ps.setInt(2, orderId);
                    ps.setString(3, expectedStatus);
                    if (ps.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
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

    /**
     * Tạo đơn hàng từ các món đã chọn ở Checkout (orders + order_items + trừ lượt voucher)
     * trong cùng một transaction. Trả về id đơn hàng mới, hoặc -1 nếu thất bại.
     */
    public int placeOrder(Order order, List<CartItem> items) {
        // Voucher phải còn hạn, còn lượt và là mã công khai hoặc voucher đổi điểm của chính khách đặt
        String useVoucherSql = "UPDATE vouchers SET used_count = used_count + 1 " +
                               "WHERE id = ? AND status = 'ACTIVE' " +
                               "AND (start_date IS NULL OR start_date <= NOW()) " +
                               "AND (valid_until IS NULL OR valid_until >= NOW()) " +
                               "AND (usage_limit IS NULL OR used_count < usage_limit) " +
                               "AND (customer_id IS NULL OR customer_id = ?)";
        String insertOrderSql = "INSERT INTO orders (order_code, customer_id, voucher_id, order_type, status, " +
                                "total_amount, discount_amount, delivery_fee, final_amount, " +
                                "recipient_name, recipient_phone, delivery_address, order_note) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO order_items (order_id, product_id, product_name_at_time, unit_price, " +
                               "customization, quantity, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            // 1. Trừ lượt sử dụng voucher (thất bại nếu voucher vừa hết lượt)
            if (order.getVoucherId() != null) {
                try (PreparedStatement ps = conn.prepareStatement(useVoucherSql)) {
                    ps.setInt(1, order.getVoucherId());
                    ps.setInt(2, order.getCustomerId());
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return -1;
                    }
                }
            }

            // 2. Tạo đơn hàng
            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, order.getOrderCode());
                ps.setInt(2, order.getCustomerId());
                if (order.getVoucherId() != null) {
                    ps.setInt(3, order.getVoucherId());
                } else {
                    ps.setNull(3, Types.INTEGER);
                }
                ps.setString(4, order.getOrderType());
                ps.setString(5, order.getStatus());
                ps.setDouble(6, order.getTotalAmount());
                ps.setDouble(7, order.getDiscountAmount());
                ps.setDouble(8, order.getDeliveryFee());
                ps.setDouble(9, order.getFinalAmount());
                ps.setString(10, order.getRecipientName());
                ps.setString(11, order.getRecipientPhone());
                ps.setString(12, order.getDeliveryAddress());
                ps.setString(13, order.getOrderNote());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return -1;
                    }
                    orderId = rs.getInt(1);
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

    // Kèm mã voucher và lần thanh toán thành công gần nhất (nếu có)
    private static final String CUSTOMER_ORDER_SELECT =
            "SELECT o.*, v.code AS voucher_code, pay.payment_method, pay.paid_at " +
            "FROM orders o " +
            "LEFT JOIN vouchers v ON v.id = o.voucher_id " +
            "LEFT JOIN payments pay ON pay.id = (SELECT MAX(p2.id) FROM payments p2 " +
            "                                    WHERE p2.order_id = o.id AND p2.status = 'SUCCESS') ";

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
     * Khách hủy đơn chưa thanh toán: đổi sang CANCELLED và trả lại lượt dùng voucher (nếu có) trong cùng transaction
     */
    public boolean cancelPendingOrder(int orderId, int customerId) {
        String cancelSql = "UPDATE orders SET status = 'CANCELLED' " +
                           "WHERE id = ? AND customer_id = ? AND status = 'PENDING' " +
                           "AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.order_id = orders.id AND p.status = 'SUCCESS')";
        String returnVoucherSql = "UPDATE vouchers v JOIN orders o ON o.voucher_id = v.id " +
                                  "SET v.used_count = v.used_count - 1 WHERE o.id = ? AND v.used_count > 0";

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
            try (PreparedStatement ps = conn.prepareStatement(returnVoucherSql)) {
                ps.setInt(1, orderId);
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
        order.setVoucherId(rs.getObject("voucher_id", Integer.class));
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
        order.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        order.setVoucherCode(rs.getString("voucher_code"));
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
                "SELECT oi.*, p.image_url FROM order_items oi " +
                "LEFT JOIN products p ON p.id = oi.product_id WHERE oi.order_id IN (");
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
        String insertOrderSql = "INSERT INTO orders (order_code, customer_id, cashier_id, voucher_id, " +
                "order_type, status, total_amount, discount_amount, delivery_fee, final_amount, " +
                "recipient_name, recipient_phone, table_number, order_note, created_at) " +
                "VALUES (?, ?, ?, ?, ?, 'CONFIRMED', ?, ?, 0, ?, ?, ?, ?, ?, NOW())";

        String insertItemSql = "INSERT INTO order_items (order_id, product_id, product_name_at_time, " +
                "unit_price, quantity, subtotal, customization) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        String insertPaymentSql = "INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at, payment_note) " +
                "VALUES (?, ?, ?, ?, 'SUCCESS', NOW(), ?)";

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

            int orderId = 0;
            try (PreparedStatement ps = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, order.getOrderCode());
                ps.setObject(2, order.getCustomerId(), Types.INTEGER);
                ps.setObject(3, order.getCashierId(), Types.INTEGER);
                ps.setObject(4, order.getVoucherId(), Types.INTEGER);
                ps.setString(5, order.getOrderType());
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
                ps.setString(3, "POS-" + order.getOrderCode());
                ps.setDouble(4, order.getFinalAmount());
                ps.setString(5, paymentNote);
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
                        ps.executeUpdate();
                    }
                }

                int earnedPoints = (int) Math.floor((order.getTotalAmount() - order.getDiscountAmount()) / Cart.MONEY_PER_POINT);
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
                     "  AND (o.cashier_id IS NOT NULL OR o.order_type IN ('DINE_IN', 'TAKEAWAY')) " +
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
     * Hủy đơn tại quầy (chỉ khi đơn chưa hoàn thành)
     */
    public boolean cancelCounterOrder(int orderId) {
        String sql = "UPDATE orders SET status = 'CANCELLED' WHERE id = ? AND status IN ('CONFIRMED', 'PENDING', 'PREPARING', 'READY')";
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
     * Thu ngân đánh dấu đơn tại quầy đã hoàn tất (khách đã nhận món)
     */
    public boolean completeCounterOrder(int orderId) {
        String sql = "UPDATE orders SET status = 'COMPLETED' WHERE id = ? AND status IN ('CONFIRMED', 'PREPARING', 'READY')";
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
