package dao;

import context.DBContext;
import model.CustomerOrder;
import model.OrderDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderDAO - Tương tác dữ liệu đơn hàng và chi tiết đơn hàng
 */
public class OrderDAO extends BaseDAO {

    /**
     * Lấy thông tin đơn hàng theo ID kèm thông tin khách hàng và voucher
     */
    public CustomerOrder getOrderById(int orderId) {
        String sql = "SELECT o.*, u.full_name AS user_full_name, u.phone AS user_phone, v.code AS voucher_code " +
                     "FROM orders o " +
                     "LEFT JOIN users u ON o.customer_id = u.id " +
                     "LEFT JOIN vouchers v ON o.voucher_id = v.id " +
                     "WHERE o.id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CustomerOrder order = new CustomerOrder();
                    order.setId(rs.getInt("id"));
                    order.setOrderCode(rs.getString("order_code"));

                    int custId = rs.getInt("customer_id");
                    if (!rs.wasNull()) {
                        order.setCustomerId(custId);
                    }

                    int cashId = rs.getInt("cashier_id");
                    if (!rs.wasNull()) {
                        order.setCashierId(cashId);
                    }

                    int vId = rs.getInt("voucher_id");
                    if (!rs.wasNull()) {
                        order.setVoucherId(vId);
                    }

                    order.setOrderType(rs.getString("order_type"));
                    order.setStatus(rs.getString("status"));
                    order.setTotalAmount(rs.getDouble("total_amount"));
                    order.setDiscountAmount(rs.getDouble("discount_amount"));
                    order.setDeliveryFee(rs.getDouble("delivery_fee"));
                    order.setFinalAmount(rs.getDouble("final_amount"));

                    // Tên và SĐT người nhận: ưu tiên recipient_name, fallback sang tên tài khoản user
                    String rName = rs.getString("recipient_name");
                    if (rName == null || rName.trim().isEmpty()) {
                        rName = rs.getString("user_full_name");
                    }
                    order.setRecipientName(rName != null ? rName : "Khách vãng lai");

                    String rPhone = rs.getString("recipient_phone");
                    if (rPhone == null || rPhone.trim().isEmpty()) {
                        rPhone = rs.getString("user_phone");
                    }
                    order.setRecipientPhone(rPhone != null ? rPhone : "---");

                    order.setDeliveryAddress(rs.getString("delivery_address"));

                    int sId = rs.getInt("shipper_id");
                    if (!rs.wasNull()) {
                        order.setShipperId(sId);
                    }

                    order.setTableNumber(rs.getString("table_number"));
                    order.setOrderNote(rs.getString("order_note"));
                    order.setCreatedAt(rs.getTimestamp("created_at"));
                    order.setVoucherCode(rs.getString("voucher_code"));

                    // Gán chi tiết món ăn
                    order.setOrderDetails(getOrderDetails(orderId));

                    return order;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<CustomerOrder> getRecentOrders(int limit) {
        List<CustomerOrder> list = new ArrayList<>();
        String sql = "SELECT o.id FROM orders o ORDER BY o.id DESC LIMIT ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 10);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CustomerOrder order = getOrderById(rs.getInt("id"));
                    if (order != null) {
                        list.add(order);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy danh sách chi tiết các món trong đơn hàng
     */
    public List<OrderDetail> getOrderDetails(int orderId) {
        List<OrderDetail> list = new ArrayList<>();
        String sql = "SELECT oi.id, oi.order_id, oi.product_id, oi.product_name_at_time, " +
                     "       oi.unit_price, oi.customization, oi.quantity, oi.subtotal " +
                     "FROM order_items oi " +
                     "WHERE oi.order_id = ? " +
                     "ORDER BY oi.id ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderDetail detail = new OrderDetail();
                    detail.setId(rs.getInt("id"));
                    detail.setOrderId(rs.getInt("order_id"));
                    detail.setProductId(rs.getInt("product_id"));
                    detail.setProductName(rs.getString("product_name_at_time"));
                    detail.setUnitPrice(rs.getDouble("unit_price"));

                    // Làm sạch chuỗi customization JSON nếu có (ví dụ: {"size":"L"} -> Size L)
                    String rawCust = rs.getString("customization");
                    detail.setCustomization(formatCustomization(rawCust));

                    detail.setQuantity(rs.getInt("quantity"));
                    detail.setSubtotal(rs.getDouble("subtotal"));

                    list.add(detail);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private String formatCustomization(String raw) {
        if (raw == null || raw.trim().isEmpty() || "null".equalsIgnoreCase(raw.trim())) {
            return "Mặc định";
        }
        // Làm sạch đơn giản dấu ngoặc nhọn, ngoặc kép từ JSON
        return raw.replace("{", "")
                  .replace("}", "")
                  .replace("\"", "")
                  .replace("note:", "")
                  .replace("size:", "Size ")
                  .replace("topping:", "Topping: ")
                  .replace("toppings:", "Topping: ")
                  .replace(",", " • ")
                  .replace("[", "")
                  .replace("]", "")
                  .trim();
    }

    private String toJsonCustomization(String raw) {
        if (raw == null || raw.trim().isEmpty() || "null".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        String trimmed = raw.trim();
        // Nếu đã là chuỗi JSON Object hoặc JSON Array thì giữ nguyên
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }
        // Bao bọc chuỗi văn bản thông thường thành JSON Object hợp lệ: {"note":"..."}
        String escaped = trimmed.replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\n", " ")
                                .replace("\r", " ")
                                .replace("\t", " ");
        return "{\"note\":\"" + escaped + "\"}";
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
        String insertSql = "INSERT INTO orders (order_code, customer_id, order_type, status, total_amount, discount_amount, delivery_fee, final_amount) " +
                           "VALUES (?, ?, 'ONLINE', 'PENDING', 0, 0, 0, 0)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement psInsert = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            psInsert.setString(1, "AUR-" + (System.currentTimeMillis() % 100000));
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
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        boolean itemAddedOrUpdated = false;
        String checkItemSql = "SELECT id, quantity FROM order_items WHERE order_id = ? AND product_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement psCheck = conn.prepareStatement(checkItemSql)) {
            psCheck.setInt(1, orderId);
            psCheck.setInt(2, productId);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
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
                    double subtotal = quantity * unitPrice;
                    String insertItemSql = "INSERT INTO order_items (order_id, product_id, product_name_at_time, unit_price, quantity, subtotal) " +
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

            // Lấy delivery_fee và discount_amount hiện tại của đơn hàng
            double deliveryFee = 0;
            double discountAmount = 0;
            String feesSql = "SELECT delivery_fee, discount_amount FROM orders WHERE id = ?";
            try (PreparedStatement psFees = conn.prepareStatement(feesSql)) {
                psFees.setInt(1, orderId);
                try (ResultSet rs2 = psFees.executeQuery()) {
                    if (rs2.next()) {
                        deliveryFee = rs2.getDouble("delivery_fee");
                        discountAmount = rs2.getDouble("discount_amount");
                    }
                }
            }

            double finalAmount = totalAmount + deliveryFee - discountAmount;
            String updateOrderSql = "UPDATE orders SET total_amount = ?, final_amount = ? WHERE id = ?";
            try (PreparedStatement psUpdateOrder = conn.prepareStatement(updateOrderSql)) {
                psUpdateOrder.setDouble(1, totalAmount);
                psUpdateOrder.setDouble(2, finalAmount);
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

    public int createOrder(CustomerOrder order, List<OrderDetail> details) {
        String insertOrderSql = "INSERT INTO orders (order_code, customer_id, order_type, status, " +
                                "total_amount, discount_amount, delivery_fee, final_amount, " +
                                "recipient_name, recipient_phone, delivery_address, order_note) " +
                                "VALUES (?, ?, ?, 'PENDING', ?, ?, ?, ?, ?, ?, ?, ?)";

        String insertItemSql = "INSERT INTO order_items (order_id, product_id, product_name_at_time, unit_price, customization, quantity, subtotal) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            int orderId = -1;
            try (PreparedStatement psOrder = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                String code = order.getOrderCode();
                if (code == null || code.trim().isEmpty()) {
                    code = "AUR-" + (System.currentTimeMillis() % 100000);
                }
                psOrder.setString(1, code);

                if (order.getCustomerId() != null && order.getCustomerId() > 0) {
                    psOrder.setInt(2, order.getCustomerId());
                } else {
                    psOrder.setNull(2, java.sql.Types.INTEGER);
                }

                psOrder.setString(3, order.getOrderType() != null ? order.getOrderType() : "ONLINE");
                psOrder.setDouble(4, order.getTotalAmount());
                psOrder.setDouble(5, order.getDiscountAmount());
                psOrder.setDouble(6, order.getDeliveryFee());
                psOrder.setDouble(7, order.getFinalAmount());
                psOrder.setString(8, order.getRecipientName() != null ? order.getRecipientName() : "Khách hàng Aura");
                psOrder.setString(9, order.getRecipientPhone() != null ? order.getRecipientPhone() : "0908 123 456");
                psOrder.setString(10, order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "28 Phố Tràng Tiền, Hoàn Kiếm, Hà Nội");
                psOrder.setString(11, order.getOrderNote());

                int affected = psOrder.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return -1;
                }
                try (ResultSet rs = psOrder.getGeneratedKeys()) {
                    if (rs.next()) {
                        orderId = rs.getInt(1);
                    }
                }
            }

            if (orderId == -1) {
                conn.rollback();
                return -1;
            }

            if (details != null && !details.isEmpty()) {
                try (PreparedStatement psItem = conn.prepareStatement(insertItemSql)) {
                    for (OrderDetail item : details) {
                        psItem.setInt(1, orderId);
                        psItem.setInt(2, item.getProductId() > 0 ? item.getProductId() : 1);
                        psItem.setString(3, item.getProductName() != null ? item.getProductName() : "Đồ uống Aura");
                        psItem.setDouble(4, item.getUnitPrice());
                        psItem.setString(5, toJsonCustomization(item.getCustomization()));
                        psItem.setInt(6, item.getQuantity() > 0 ? item.getQuantity() : 1);
                        double sub = item.getSubtotal() > 0 ? item.getSubtotal() : (item.getUnitPrice() * item.getQuantity());
                        psItem.setDouble(7, sub);
                        psItem.addBatch();
                    }
                    psItem.executeBatch();
                }
            }

            conn.commit();
            return orderId;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return -1;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Lấy danh sách đơn hàng của một khách hàng cụ thể (UC17: View Order History)
     */
    public List<CustomerOrder> getOrdersByCustomerId(int customerId, int limit) {
        List<CustomerOrder> list = new ArrayList<>();
        String sql = "SELECT id FROM orders WHERE customer_id = ? ORDER BY id DESC LIMIT ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, limit > 0 ? limit : 20);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CustomerOrder order = getOrderById(rs.getInt("id"));
                    if (order != null) {
                        list.add(order);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Cập nhật trạng thái đơn hàng (UC25: Update Order Status)
     * Bám sát các giá trị ENUM DB: PENDING, CONFIRMED, PREPARING, READY, DELIVERING, COMPLETED, CANCELLED
     */
    public boolean updateOrderStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Hủy đơn hàng trực tuyến bởi khách hàng (UC16: Cancel Online Order)
     * Chỉ được phép hủy khi đơn ở trạng thái PENDING hoặc CONFIRMED
     */
    public boolean cancelOrder(int orderId, Integer customerId) {
        String sql = "UPDATE orders SET status = 'CANCELLED' WHERE id = ? AND (status = 'PENDING' OR status = 'CONFIRMED')";
        if (customerId != null && customerId > 0) {
            sql += " AND customer_id = ?";
        }
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            if (customerId != null && customerId > 0) {
                ps.setInt(2, customerId);
            }
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

