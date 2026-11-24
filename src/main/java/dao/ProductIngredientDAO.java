package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import model.ProductIngredient;

public class ProductIngredientDAO extends BaseDAO {

    // [CTL06]
    // . AdminRecipeController.doPost gọi saveRecipe trong ProductIngredientDAO.java để lưu công thức của
    //   món đã chọn. Bảng product_ingredients giữ từng dòng gồm mã món, mã nguyên liệu và lượng cần dùng
    //   cho một phần.
    // . Em kiểm tra nguyên liệu mới còn hoạt động, xóa các dòng công thức cũ rồi thêm danh sách mới của
    //   đúng món. Ghi đủ mới xác nhận lưu; có lỗi thì hủy thay đổi để giữ công thức cũ và báo về
    //   controller.
    // ... Lưu đủ công thức thì về controller tại AdminRecipeController.java, tìm [CTL07]. Nếu có lỗi,
    //     DAO hủy thay đổi; doPost giữ các dòng đang nhập tại AdminRecipeController.java, tìm mã CTL09
    //     rồi mở lại hộp để sửa.
    public void saveRecipe(int productId, List<ProductIngredient> ingredients) throws Exception {

        // [SQL11-36] Nút Lưu công thức: xóa các dòng công thức cũ của món.
        // . Dấu ? từ trái sang phải: 1=mã sản phẩm.
        // . Chưa xác nhận riêng bước xóa; phải thêm đủ công thức mới mới commit, lỗi thì rollback để giữ công
        //   thức cũ.
        String deleteOldBomSql = "DELETE FROM product_ingredients WHERE product_id = ?";
        // [SQL11-37] Nút Lưu công thức: thêm từng dòng nguyên liệu và định lượng mới.
        // . Dấu ? từ trái sang phải: 1=mã sản phẩm; 2=mã nguyên liệu; 3=định lượng.
        // . Java điền lại 3 giá trị cho từng dòng rồi executeBatch; không phải chỉ lưu một nguyên liệu.
        String insertBomSql = "INSERT INTO product_ingredients (product_id, ingredient_id, quantity_required) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement psDelete = conn.prepareStatement(deleteOldBomSql);
                 PreparedStatement psInsert = conn.prepareStatement(insertBomSql)) {

                java.util.Set<Integer> oldIds = new java.util.HashSet<>();
                // [SQL11-38] Trước khi lưu công thức: đọc các nguyên liệu đã có.
                // . Dấu ? từ trái sang phải: 1=mã sản phẩm.
                // . DAO dùng danh sách này để nhận biết nguyên liệu mới; controller vẫn kiểm tra dữ liệu được
                //   gửi trước khi gọi DAO.
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
        // [SQL11-39] Đọc riêng công thức một món kèm tên và đơn vị nguyên liệu.
        // . Dấu ? từ trái sang phải: 1=mã sản phẩm.
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
    // ... Quay lại doGet trong AdminRecipeController.java để dùng công thức theo mã món; đọc tiếp tại
    //     AdminRecipeController.java, tìm [CTTONG04] ở bước lấy đơn giá nguyên liệu.
    public java.util.Map<Integer, List<ProductIngredient>> getAllProductIngredientsMap() {

        java.util.Map<Integer, List<ProductIngredient>> map = new java.util.LinkedHashMap<>();
        // [SQL11-40] Trang công thức: đọc các dòng định mức của toàn bộ món.
        // . Java nhóm các dòng theo product_id để mở form, đếm món đã có công thức và tính giá vốn.
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
