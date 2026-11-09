package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.Ingredient;

public class IngredientDAO extends BaseDAO {

    public List<Ingredient> getAllIngredients() {
        // BF11: lấy tên, đơn vị và trạng thái cho form BOM; JSP quyết định lựa chọn thêm mới/giữ dòng cũ.
        List<Ingredient> list = new ArrayList<>();
        String sql = "SELECT id, name, unit, is_active FROM ingredients ORDER BY name ASC";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Ingredient item = new Ingredient(
                    rs.getInt("id"), 
                    rs.getString("name"), 
                    rs.getString("unit")
                );
                item.setActive(rs.getBoolean("is_active"));
                list.add(item);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load ingredients", e);
        }
        return list;
    }

    /** Latest purchase unit price per ingredient (prioritizing received POs, with fallback to latest PO). */
    public Map<Integer, BigDecimal> getLatestPurchasePrices() {
        Map<Integer, BigDecimal> prices = new HashMap<>();
        String sql = "SELECT ingredient_id, unit_price FROM (" +
                     "  SELECT poi.ingredient_id, poi.unit_price, " +
                     "         ROW_NUMBER() OVER (PARTITION BY poi.ingredient_id " +
                     "             ORDER BY CASE WHEN po.status IN ('RECEIVED','PARTIALLY_RECEIVED','CLOSED') THEN 0 ELSE 1 END ASC, " +
                     "                      COALESCE(tx.created_at, po.created_at) DESC, " +
                     "                      COALESCE(tx.id, 0) DESC, poi.id DESC) AS row_num " +
                     "  FROM purchase_order_items poi " +
                     "  JOIN purchase_orders po ON po.id = poi.po_id AND po.status <> 'CANCELLED' " +
                     "  LEFT JOIN inventory_transactions tx " +
                     "    ON tx.reference_type = 'PURCHASE_ORDER' AND tx.reference_id = po.id " +
                     "   AND tx.ingredient_id = poi.ingredient_id AND tx.transaction_type = 'IN_PURCHASE'" +
                     ") ranked WHERE row_num = 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                prices.put(rs.getInt("ingredient_id"), rs.getBigDecimal("unit_price"));
            }
        } catch (Exception ignored) {
        }
        return prices;
    }

    public List<Ingredient> getAllIngredientsWithDetails(String keyword, Integer supplierId) {
        List<Ingredient> list = new ArrayList<>();
        Map<Integer, BigDecimal> lastPrices = getLatestPurchasePrices();

        StringBuilder sql = new StringBuilder(
            "SELECT i.id, i.supplier_id, i.name, i.unit, i.is_active, i.current_stock, i.min_stock_level, " +
            "s.name AS supplier_name, " +
            "COALESCE(rec.recipe_count, 0) AS used_in_recipes " +
            "FROM ingredients i " +
            "LEFT JOIN suppliers s ON i.supplier_id = s.id " +
            "LEFT JOIN (" +
            "    SELECT ingredient_id, COUNT(DISTINCT product_id) AS recipe_count " +
            "    FROM product_ingredients GROUP BY ingredient_id" +
            ") rec ON i.id = rec.ingredient_id WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(i.name) LIKE ? OR LOWER(COALESCE(s.name, '')) LIKE ?) ");
            String term = "%" + keyword.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
        }
        if (supplierId != null && supplierId > 0) {
            sql.append(" AND i.supplier_id = ? ");
            params.add(supplierId);
        }
        sql.append(" ORDER BY i.id ASC");

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int idx = 0; idx < params.size(); idx++) {
                ps.setObject(idx + 1, params.get(idx));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ingredient item = new Ingredient(
                        rs.getInt("id"),
                        rs.getObject("supplier_id") != null ? rs.getInt("supplier_id") : null,
                        rs.getString("name"),
                        rs.getString("unit"),
                        rs.getBigDecimal("current_stock"),
                        rs.getBigDecimal("min_stock_level")
                    );
                    item.setSupplierName(rs.getString("supplier_name"));
                    item.setActive(rs.getBoolean("is_active"));
                    item.setUsedInRecipesCount(rs.getInt("used_in_recipes"));
                    item.setLastPurchasePrice(lastPrices.get(item.getId()));
                    list.add(item);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load ingredients with details", e);
        }
        return list;
    }

    public Ingredient getIngredientById(int id) {
        String sql = "SELECT i.id, i.supplier_id, i.name, i.unit, i.is_active, i.current_stock, i.min_stock_level, " +
                     "s.name AS supplier_name, " +
                     "COALESCE(rec.recipe_count, 0) AS used_in_recipes " +
                     "FROM ingredients i " +
                     "LEFT JOIN suppliers s ON i.supplier_id = s.id " +
                     "LEFT JOIN (" +
                     "    SELECT ingredient_id, COUNT(DISTINCT product_id) AS recipe_count " +
                     "    FROM product_ingredients GROUP BY ingredient_id" +
                     ") rec ON i.id = rec.ingredient_id " +
                     "WHERE i.id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Ingredient item = new Ingredient(
                        rs.getInt("id"),
                        rs.getObject("supplier_id") != null ? rs.getInt("supplier_id") : null,
                        rs.getString("name"),
                        rs.getString("unit"),
                        rs.getBigDecimal("current_stock"),
                        rs.getBigDecimal("min_stock_level")
                    );
                    item.setSupplierName(rs.getString("supplier_name"));
                    item.setActive(rs.getBoolean("is_active"));
                    item.setUsedInRecipesCount(rs.getInt("used_in_recipes"));
                    Map<Integer, BigDecimal> lastPrices = getLatestPurchasePrices();
                    item.setLastPurchasePrice(lastPrices.get(item.getId()));
                    return item;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not get ingredient by id: " + id, e);
        }
        return null;
    }

    public int insertIngredient(Ingredient item) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO ingredients (supplier_id, name, unit, current_stock, min_stock_level) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (item.getSupplierId() != null && item.getSupplierId() > 0) {
                ps.setInt(1, item.getSupplierId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, item.getName());
            ps.setString(3, item.getUnit());
            ps.setBigDecimal(4, item.getCurrentStock() != null ? item.getCurrentStock() : BigDecimal.ZERO);
            ps.setBigDecimal(5, item.getMinStockLevel() != null ? item.getMinStockLevel() : BigDecimal.ZERO);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean updateIngredient(Ingredient item) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE ingredients SET supplier_id = ?, name = ?, unit = ?, min_stock_level = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (item.getSupplierId() != null && item.getSupplierId() > 0) {
                ps.setInt(1, item.getSupplierId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, item.getName());
            ps.setString(3, item.getUnit());
            ps.setBigDecimal(4, item.getMinStockLevel() != null ? item.getMinStockLevel() : BigDecimal.ZERO);
            ps.setInt(5, item.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateStock(int id, BigDecimal newStock) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE ingredients SET current_stock = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newStock);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean isIngredientUsedInRecipes(int id) {
        String sql = "SELECT COUNT(*) FROM product_ingredients WHERE ingredient_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public List<Ingredient> getActiveIngredientsWithDetails() {
        return getAllIngredientsWithDetails(null, null).stream().filter(Ingredient::isActive).toList();
    }

    public boolean updateIngredientActive(int id, boolean active) throws SQLException, ClassNotFoundException {
        if (id <= 0) throw new IllegalArgumentException("Mã nguyên liệu không hợp lệ.");
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE ingredients SET is_active = ? WHERE id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            if (ps.executeUpdate() > 0) return true;
            try (PreparedStatement exists = conn.prepareStatement("SELECT 1 FROM ingredients WHERE id = ?")) {
                exists.setInt(1, id);
                try (ResultSet rs = exists.executeQuery()) { return rs.next(); }
            }
        }
    }

    /** Locks the state until the caller commits its new purchase/restock/BOM. */
    static void requireActive(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT is_active FROM ingredients WHERE id = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !rs.getBoolean("is_active")) {
                    throw new SQLException("Nguyên liệu không tồn tại hoặc đã ngừng sử dụng. Hãy chọn nguyên liệu đang hoạt động.", "45000");
                }
            }
        }
    }

    public int[] getIngredientStats() {
        int[] stats = new int[4]; 
        // [0]: Total ingredients
        // [1]: Total active suppliers
        // [2]: Ingredients under min stock
        // [3]: Ingredients not used in recipes

        String sql = "SELECT " +
                     " (SELECT COUNT(*) FROM ingredients) AS total_ing, " +
                     " (SELECT COUNT(DISTINCT supplier_id) FROM ingredients WHERE supplier_id IS NOT NULL) AS total_sup, " +
                     " (SELECT COUNT(*) FROM ingredients WHERE is_active = 1 AND current_stock <= min_stock_level) AS low_stock, " +
                     " (SELECT COUNT(*) FROM ingredients i " +
                     "  WHERE NOT EXISTS (SELECT 1 FROM product_ingredients pi WHERE pi.ingredient_id = i.id)) AS unused_recipes";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats[0] = rs.getInt("total_ing");
                stats[1] = rs.getInt("total_sup");
                stats[2] = rs.getInt("low_stock");
                stats[3] = rs.getInt("unused_recipes");
            }
        } catch (Exception ignored) {
        }
        return stats;
    }
}
