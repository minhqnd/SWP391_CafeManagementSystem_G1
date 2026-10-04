package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import model.ProductIngredient;

public class ProductIngredientDAO extends BaseDAO {

    public void saveRecipe(int productId, String variantsConfig, List<ProductIngredient> ingredients) throws Exception {
        String updateProductSql = "UPDATE products SET variants_config = ? WHERE id = ?";
        String deleteOldBomSql = "DELETE FROM product_ingredients WHERE product_id = ?";
        String insertBomSql = "INSERT INTO product_ingredients (product_id, ingredient_id, quantity_required) VALUES (?, ?, ?)";
        
        try (Connection conn = getConnection()) {
            // Transaction management
            conn.setAutoCommit(false);
            
            try (PreparedStatement psUpdate = conn.prepareStatement(updateProductSql);
                 PreparedStatement psDelete = conn.prepareStatement(deleteOldBomSql);
                 PreparedStatement psInsert = conn.prepareStatement(insertBomSql)) {
                
                // 1. Update product toggle configurations (online, app) stored in variants_config
                psUpdate.setString(1, variantsConfig);
                psUpdate.setInt(2, productId);
                psUpdate.executeUpdate();
                
                // 2. Delete existing BOM to replace with new ones
                psDelete.setInt(1, productId);
                psDelete.executeUpdate();
                
                // 3. Insert new ingredients BOM using batch
                for (ProductIngredient pi : ingredients) {
                    psInsert.setInt(1, productId);
                    psInsert.setInt(2, pi.getIngredientId());
                    psInsert.setDouble(3, pi.getQuantityRequired());
                    psInsert.addBatch();
                }
                psInsert.executeBatch();
                
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
