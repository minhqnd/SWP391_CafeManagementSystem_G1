package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import model.ProductIngredient;

public class ProductIngredientDAO extends BaseDAO {

    public void saveRecipe(int productId, List<ProductIngredient> ingredients) throws Exception {
        String deleteOldBomSql = "DELETE FROM product_ingredients WHERE product_id = ?";
        String insertBomSql = "INSERT INTO product_ingredients (product_id, ingredient_id, quantity_required) VALUES (?, ?, ?)";
        
        try (Connection conn = getConnection()) {
            // Transaction management
            conn.setAutoCommit(false);
            
            try (PreparedStatement psDelete = conn.prepareStatement(deleteOldBomSql);
                 PreparedStatement psInsert = conn.prepareStatement(insertBomSql)) {

                // Replace the existing BOM for this product.
                java.util.Set<Integer> oldIds = new java.util.HashSet<>();
                try (PreparedStatement old = conn.prepareStatement("SELECT ingredient_id FROM product_ingredients WHERE product_id = ?")) {
                    old.setInt(1, productId);
                    try (java.sql.ResultSet rs = old.executeQuery()) {
                        while (rs.next()) oldIds.add(rs.getInt("ingredient_id"));
                    }
                }
                if (ingredients != null) {
                    for (ProductIngredient line : ingredients) {
                        if (!oldIds.contains(line.getIngredientId())) IngredientDAO.requireActive(conn, line.getIngredientId());
                    }
                }

                psDelete.setInt(1, productId);
                psDelete.executeUpdate();

                // Insert the new BOM rows, if any.
                if (ingredients != null && !ingredients.isEmpty()) {
                    for (ProductIngredient pi : ingredients) {
                        psInsert.setInt(1, productId);
                        psInsert.setInt(2, pi.getIngredientId());
                        psInsert.setDouble(3, pi.getQuantityRequired());
                        psInsert.addBatch();
                    }
                    psInsert.executeBatch();
                }
                
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<ProductIngredient> getIngredientsByProductId(int productId) {
        List<ProductIngredient> list = new java.util.ArrayList<>();
        String sql = "SELECT pi.product_id, pi.ingredient_id, pi.quantity_required, i.name AS ingredient_name, i.unit "
                   + "FROM product_ingredients pi "
                   + "JOIN ingredients i ON pi.ingredient_id = i.id "
                   + "WHERE pi.product_id = ? "
                   + "ORDER BY i.name ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ProductIngredient(
                        rs.getInt("product_id"),
                        rs.getInt("ingredient_id"),
                        rs.getDouble("quantity_required"),
                        rs.getString("ingredient_name"),
                        rs.getString("unit")
                    ));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load recipe ingredients", e);
        }
        return list;
    }

    public java.util.Map<Integer, List<ProductIngredient>> getAllProductIngredientsMap() {
        java.util.Map<Integer, List<ProductIngredient>> map = new java.util.LinkedHashMap<>();
        String sql = "SELECT pi.product_id, pi.ingredient_id, pi.quantity_required, i.name AS ingredient_name, i.unit "
                   + "FROM product_ingredients pi "
                   + "JOIN ingredients i ON pi.ingredient_id = i.id "
                   + "ORDER BY pi.product_id, i.name ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int pId = rs.getInt("product_id");
                map.computeIfAbsent(pId, k -> new java.util.ArrayList<>()).add(new ProductIngredient(
                    pId,
                    rs.getInt("ingredient_id"),
                    rs.getDouble("quantity_required"),
                    rs.getString("ingredient_name"),
                    rs.getString("unit")
                ));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load product recipe ingredients", e);
        }
        return map;
    }
}
