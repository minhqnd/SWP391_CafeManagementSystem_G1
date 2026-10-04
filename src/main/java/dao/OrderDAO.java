package dao;

import context.DBContext;
import model.CartItem;
import model.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

/**
 * OrderDAO - Tương tác dữ liệu đơn hàng
 */
public class OrderDAO extends BaseDAO {

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
        String useVoucherSql = "UPDATE vouchers SET used_count = used_count + 1 " +
                               "WHERE id = ? AND (usage_limit IS NULL OR used_count < usage_limit)";
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
}
