package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import model.CartItem;

/** Inventory movements that must commit atomically with order or purchase-order status changes. */
final class InventoryMovementDAO {
    private InventoryMovementDAO() {}

    // Combine all cart lines: different drinks can consume the same ingredient.
    static String checkoutStockError(Connection conn, List<CartItem> items, boolean lock)
            throws SQLException {
        Map<Integer, BigDecimal> required = new TreeMap<>();
        Map<Integer, String> productNames = new TreeMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ingredient_id, quantity_required FROM product_ingredients WHERE product_id = ?")) {
            for (CartItem item : items) {
                if (item.getQuantity() <= 0) return "Số lượng món không hợp lệ. Vui lòng sửa giỏ hàng.";
                ps.setInt(1, item.getProductId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("ingredient_id");
                        BigDecimal quantity = rs.getBigDecimal("quantity_required")
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                        required.merge(id, quantity, BigDecimal::add);
                        productNames.put(id, item.getProductName());
                    }
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT current_stock FROM ingredients WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            for (Map.Entry<Integer, BigDecimal> entry : required.entrySet()) {
                ps.setInt(1, entry.getKey());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getBigDecimal(1).compareTo(entry.getValue()) < 0) {
                        return "Món " + productNames.get(entry.getKey())
                                + " đã hết hoặc không đủ số lượng cho giỏ hàng này. Vui lòng giảm số lượng hoặc chọn món khác.";
                    }
                }
            }
        }
        return null;
    }

    static void consumeOrder(Connection conn, int orderId) throws SQLException {
        String quantitiesSql = "SELECT pi.ingredient_id, i.name, SUM(pi.quantity_required * oi.quantity) AS required_quantity "
                + "FROM order_items oi JOIN product_ingredients pi ON pi.product_id = oi.product_id "
                + "JOIN ingredients i ON i.id = pi.ingredient_id WHERE oi.order_id = ? "
                + "GROUP BY pi.ingredient_id, i.name ORDER BY pi.ingredient_id";
        List<Movement> movements = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(quantitiesSql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movements.add(new Movement(rs.getInt("ingredient_id"), rs.getString("name"),
                            rs.getBigDecimal("required_quantity")));
                }
            }
        }

        for (Movement movement : movements) {
            if (hasMovement(conn, "OUT_ORDER", "ORDER", orderId, movement.ingredientId)) continue;
            BigDecimal currentStock;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT current_stock FROM ingredients WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, movement.ingredientId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Không tìm thấy nguyên liệu: " + movement.name);
                    currentStock = rs.getBigDecimal("current_stock");
                }
            }
            if (currentStock.compareTo(movement.quantity) < 0) {
                throw new SQLException("Không đủ tồn kho " + movement.name + ": còn "
                        + currentStock.stripTrailingZeros().toPlainString() + ", can "
                        + movement.quantity.stripTrailingZeros().toPlainString());
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE ingredients SET current_stock = current_stock - ? WHERE id = ?")) {
                ps.setBigDecimal(1, movement.quantity);
                ps.setInt(2, movement.ingredientId);
                ps.executeUpdate();
            }
            insertMovement(conn, movement.ingredientId, "OUT_ORDER", movement.quantity, "ORDER", orderId);
        }
    }

    private static boolean hasMovement(Connection conn, String type, String referenceType,
                                       int referenceId, int ingredientId) throws SQLException {
        String sql = "SELECT 1 FROM inventory_transactions WHERE transaction_type = ? "
                + "AND reference_type = ? AND reference_id = ? AND ingredient_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setString(2, referenceType);
            ps.setInt(3, referenceId);
            ps.setInt(4, ingredientId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static void insertMovement(Connection conn, int ingredientId, String type,
                                       BigDecimal quantity, String referenceType, int referenceId)
            throws SQLException {
        String sql = "INSERT INTO inventory_transactions "
                + "(ingredient_id, transaction_type, quantity, reference_type, reference_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            ps.setString(2, type);
            ps.setBigDecimal(3, quantity);
            ps.setString(4, referenceType);
            ps.setInt(5, referenceId);
            ps.executeUpdate();
        }
    }

    private static final class Movement {
        private final int ingredientId;
        private final String name;
        private final BigDecimal quantity;

        private Movement(int ingredientId, String name, BigDecimal quantity) {
            this.ingredientId = ingredientId;
            this.name = name;
            this.quantity = quantity;
        }
    }
}
