package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import model.ProductIngredient;

public class ProductIngredientDAO extends BaseDAO {

    public void saveRecipe(int productId, List<ProductIngredient> ingredients) throws Exception {
        // RC06: . Ở đây em thay công thức cũ bằng các dòng nguyên liệu vừa kiểm tra.
        // .. Em lưu cả lần thay đổi cùng nhau; lỗi thì khôi phục công thức cũ, chưa trừ tồn kho ở bước này.
        // ... Trở về AdminRecipeController.doPost() để chuẩn bị chuyển trang [RC07].
        // BF11 - BOM: thay toàn bộ dòng product_ingredients của một món trong cùng transaction.
        // Đây là lưu định mức; KHÔNG trừ tồn kho khi Admin bấm Lưu công thức.
        String deleteOldBomSql = "DELETE FROM product_ingredients WHERE product_id = ?";
        String insertBomSql = "INSERT INTO product_ingredients (product_id, ingredient_id, quantity_required) VALUES (?, ?, ?)";
        
        try (Connection conn = getConnection()) {
            // Transaction management
            conn.setAutoCommit(false);
            
            try (PreparedStatement psDelete = conn.prepareStatement(deleteOldBomSql);
                 PreparedStatement psInsert = conn.prepareStatement(insertBomSql)) {

                // Replace the existing BOM for this product.
                // Cho giữ nguyên liệu ngừng sử dụng đã có ở BOM cũ; nguyên liệu thêm mới phải ACTIVE.
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

                // Xóa BOM cũ rồi thêm BOM mới; nếu INSERT lỗi thì rollback khôi phục dữ liệu cũ.
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
        // BF11: JOIN ingredients lấy tên/đơn vị cho các dòng định lượng của đúng món.
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
        // RC03: . Ở đây em đọc công thức của các món kèm tên và đơn vị nguyên liệu.
        // .. Em gom các dòng nguyên liệu theo từng mã món để tra nhanh.
        // ... Map công thức về AdminRecipeController.doGet() [RC02].
        // BF11: đọc BOM toàn bộ món một lần, gom theo productId để dựng các thẻ trên trang công thức.
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
