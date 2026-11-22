package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import model.ProductIngredient;

public class ProductIngredientDAO extends BaseDAO {

    // [CTL06]
    // . Trong saveRecipe của ProductIngredientDAO.java, em kiểm tra nguyên liệu mới còn hoạt động rồi thay
    //   các dòng công thức của món trong product_ingredients. Ghi đủ danh sách mới thì xác nhận lưu và quay
    //   về doPost; có lỗi thì hủy thay đổi để giữ công thức cũ rồi báo lỗi về controller.
    // ... Đọc tiếp tại AdminRecipeController.java, tìm [CTL07].
    public void saveRecipe(int productId, List<ProductIngredient> ingredients) throws Exception {

        String deleteOldBomSql = "DELETE FROM product_ingredients WHERE product_id = ?";
        String insertBomSql = "INSERT INTO product_ingredients (product_id, ingredient_id, quantity_required) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement psDelete = conn.prepareStatement(deleteOldBomSql);
                 PreparedStatement psInsert = conn.prepareStatement(insertBomSql)) {

                java.util.Set<Integer> oldIds = new java.util.HashSet<>();
                try (PreparedStatement old = conn.prepareStatement("SELECT ingredient_id FROM product_ingredients WHERE product_id = ?")) {
                    old.setInt(1, productId);
                    try (java.sql.ResultSet rs = old.executeQuery()) {
                        while (rs.next()) oldIds.add(rs.getInt("ingredient_id"));
                    }
                }
                // . Trước khi thay công thức, trong ProductIngredientDAO.saveRecipe (lưu công thức của sản phẩm) em gọi
                //   IngredientDAO.requireActive (kiểm tra nguyên liệu còn hoạt động) bằng kết nối và mã từng nguyên liệu
                //   mới. Em kiểm tra nguyên liệu còn dùng được; nguyên liệu đã có trong công thức cũ thì được phép giữ
                //   lại.
                if (ingredients != null) {
                    for (ProductIngredient line : ingredients) {
                        if (!oldIds.contains(line.getIngredientId())) IngredientDAO.requireActive(conn, line.getIngredientId());
                    }
                }

                // . Trong ProductIngredientDAO.saveRecipe (lưu công thức của sản phẩm), em dùng mã sản phẩm để xóa các
                //   dòng công thức cũ, chuẩn bị ghi danh sách mới. Bước này chưa được xác nhận riêng; phải ghi đủ công
                //   thức mới thì thay đổi mới có hiệu lực.
                psDelete.setInt(1, productId);
                psDelete.executeUpdate();

                // . Sau khi xóa các dòng cũ, trong ProductIngredientDAO.saveRecipe (lưu công thức của sản phẩm) em đưa
                //   mã sản phẩm, mã nguyên liệu và định lượng của từng dòng vào INSERT product_ingredients. Em ghi cả
                //   danh sách để thay công thức cho đúng món đang sửa.
                if (ingredients != null && !ingredients.isEmpty()) {
                    for (ProductIngredient pi : ingredients) {
                        psInsert.setInt(1, productId);
                        psInsert.setInt(2, pi.getIngredientId());
                        psInsert.setDouble(3, pi.getQuantityRequired());
                        psInsert.addBatch();
                    }
                    psInsert.executeBatch();
                }

                // . Khi tất cả dòng công thức đều ghi được, trong ProductIngredientDAO.saveRecipe (lưu công thức của sản
                //   phẩm) em gọi conn.commit (xác nhận lưu toàn bộ thay đổi) để xác nhận. Hàm kết thúc và chương trình
                //   trở về AdminRecipeController.doPost để đặt thông báo đã lưu công thức.
                conn.commit();
            } catch (Exception e) {
                // . Nếu có lỗi khi ghi công thức, trong ProductIngredientDAO.saveRecipe (lưu công thức của sản phẩm) em
                //   gọi conn.rollback (hủy các thay đổi của lần lưu) để khôi phục công thức cũ rồi báo lỗi về
                //   AdminRecipeController.doPost. Controller giữ bản nhập dở và mở lại form để người dùng sửa tiếp.
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

    // [CTM03]
    // . Ở getAllProductIngredientsMap (đọc công thức và nhóm theo mã sản phẩm) trong
    //   ProductIngredientDAO.java, em đọc các dòng công thức kèm tên và đơn vị nguyên liệu, rồi nhóm theo mã
    //   món. Em trả dữ liệu về AdminRecipeController.doGet để mở đúng công thức, tính giá vốn và thống kê
    //   món đã có định mức.
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
