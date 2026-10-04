package dao;

import context.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

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
}
