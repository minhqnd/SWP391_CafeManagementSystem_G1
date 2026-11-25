package dao;

import context.DBContext;
import model.Category;
import model.CategoryImage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO extends BaseDAO {

    // [DMM03]
    // . Hàm getAllCategoriesWithCount trong CategoryDAO đọc danh mục kèm số món của mỗi nhóm. Danh sách quay
    //   về hàm doGet đang gọi để dựng bảng danh mục hoặc các lựa chọn nhóm trên trang quản lý sản phẩm.
    //
    // Tra tiếp: admin-categories.jsp / DMM04.
    public List<Category> getAllCategoriesWithCount() {

        List<Category> list = new ArrayList<>();
        // [SQL11-01] Bảng danh mục và lựa chọn danh mục ở quản lý sản phẩm: đọc nhóm, trạng thái, ảnh và số
        //   món.

        String sql = "SELECT c.id, c.name, c.status, c.image_url, "
                + "(ci.category_id IS NOT NULL) AS has_stored_image, COUNT(p.id) AS product_count "
                + "FROM categories c LEFT JOIN products p ON p.category_id = c.id "
                + "LEFT JOIN category_images ci ON ci.category_id = c.id "
                + "GROUP BY c.id, c.name, c.status, c.image_url, ci.category_id ORDER BY c.id ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapCategory(rs, rs.getInt("product_count")));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load categories", e);
        }
        return list;
    }

    public int getProductCount() {
        // [SQL11-02] Thẻ Tổng sản phẩm trên trang danh mục: đếm toàn bộ dòng products.
        String sql = "SELECT COUNT(*) FROM products";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new IllegalStateException("Could not count products", e);
        }
    }

    public int getProductCountByCategory(int categoryId) {
        // [SQL11-03] Đếm số sản phẩm thuộc một danh mục.

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM products WHERE category_id = ?")) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not count products in category", e);
        }
    }

    public int getOrderedProductCountByCategory(int categoryId) {
        // [SQL11-04] Nút Xóa danh mục: đếm món đã xuất hiện trong đơn hàng.

        String sql = "SELECT COUNT(DISTINCT p.id) "
                + "FROM products p JOIN order_items oi ON oi.product_id = p.id "
                + "WHERE p.category_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not check product order history", e);
        }
    }

    public boolean categoryNameExists(String name, int exceptId) {
        // [SQL11-05] Ô Tên danh mục: kiểm tra trùng tên, bỏ qua danh mục đang sửa.

        String sql = "SELECT 1 FROM categories WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) AND id <> ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, exceptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not check category name", e);
        }
    }

    public boolean saveCategory(Category category) throws SQLException, ClassNotFoundException {
        return saveCategory(category, null, null, null, false);
    }

    // [DML05]
    // . Hàm saveCategory trong CategoryDAO thêm hoặc sửa danh mục và xử lý ảnh trong cùng một lần lưu. Ghi đủ
    //   mới xác nhận; gặp lỗi thì hủy thay đổi. Kết quả quay về hàm save của AdminCategoriesController để báo
    //   đã lưu hoặc báo lỗi.
    //
    // Tra tiếp: AdminCategoriesController.java / DML06.
    public boolean saveCategory(Category category, byte[] imageData, String contentType,
                                String fileName, boolean removeImage)
            throws SQLException, ClassNotFoundException {

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {

                boolean saved = saveCategory(conn, category);
                if (!saved) {
                    conn.rollback();
                    return false;
                }
                if (imageData != null) {

                    saveImage(conn, category.getId(), imageData, contentType, fileName);
                    clearLegacyImageUrl(conn, category.getId());
                } else if (removeImage) {

                    deleteImage(conn, category.getId());
                    clearLegacyImageUrl(conn, category.getId());
                }
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private boolean saveCategory(Connection conn, Category category) throws SQLException {

        if (category.getId() > 0) {
            // [SQL11-06] Nút Lưu danh mục: cập nhật tên và trạng thái.

            String sql = "UPDATE categories SET name = ?, status = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindCategory(ps, category);
                ps.setInt(3, category.getId());
                int updated = ps.executeUpdate();
                if (updated > 0) return true;
                // [SQL11-07] Lưu danh mục không đổi giá trị: kiểm tra danh mục vẫn tồn tại.

                try (PreparedStatement exists = conn.prepareStatement("SELECT 1 FROM categories WHERE id = ?")) {
                    exists.setInt(1, category.getId());
                    try (ResultSet rs = exists.executeQuery()) {
                        return rs.next();
                    }
                }
            }
        }

        // [SQL11-08] Nút Tạo danh mục: thêm nhóm mới và lấy mã vừa tạo.

        String sql = "INSERT INTO categories (name, status) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindCategory(ps, category);
            if (ps.executeUpdate() == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) category.setId(keys.getInt(1));
            }
            return category.getId() > 0;
        }
    }

    private void bindCategory(PreparedStatement ps, Category category) throws SQLException {
        ps.setString(1, category.getName());
        ps.setString(2, category.getStatus());
    }

    private void saveImage(Connection conn, int categoryId, byte[] data, String contentType, String fileName)
            throws SQLException {
        // [SQL11-09] Ảnh danh mục: thêm ảnh mới hoặc thay ảnh của nhóm đã có.

        String sql = "INSERT INTO category_images (category_id, image_data, content_type, file_name) "
                + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE image_data = VALUES(image_data), "
                + "content_type = VALUES(content_type), file_name = VALUES(file_name), updated_at = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.setBytes(2, data);
            ps.setString(3, contentType);
            ps.setString(4, fileName);
            ps.executeUpdate();
        }
    }

    private void deleteImage(Connection conn, int categoryId) throws SQLException {
        // [SQL11-10] Ô Xóa ảnh hiện tại của danh mục: xóa ảnh đang lưu.

        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM category_images WHERE category_id = ?")) {
            ps.setInt(1, categoryId);
            ps.executeUpdate();
        }
    }

    private void clearLegacyImageUrl(Connection conn, int categoryId) throws SQLException {
        // [SQL11-11] Thay hoặc xóa ảnh danh mục: bỏ đường dẫn ảnh cũ.

        try (PreparedStatement ps = conn.prepareStatement("UPDATE categories SET image_url = NULL WHERE id = ?")) {
            ps.setInt(1, categoryId);
            ps.executeUpdate();
        }
    }

    // [DMAH03]
    // . Hàm updateCategoryStatus trong CategoryDAO ghi trạng thái mới cho đúng danh mục trong bảng
    //   categories. Kết quả được trả về hàm toggle của AdminCategoriesController để xử lý thông báo.
    //
    // Tra tiếp: AdminCategoriesController.java / DMAH04.
    public boolean updateCategoryStatus(int id, String status) throws SQLException, ClassNotFoundException {

        // [SQL11-12] Nút Ẩn/Hiện danh mục: đổi trạng thái của đúng nhóm.

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE categories SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // [DMX03]
    // . Hàm deleteCategoryAndProducts trong CategoryDAO kiểm tra lại liên kết đơn hàng rồi xóa sản phẩm và
    //   danh mục cùng một lần. Có lỗi thì hủy cả lần xóa. Kết quả quay về hàm delete của
    //   AdminCategoriesController để báo thao tác đã thực hiện được hay chưa.
    //
    // Tra tiếp: AdminCategoriesController.java / DMX04.
    public boolean deleteCategoryAndProducts(int id) throws SQLException, ClassNotFoundException {
        if (id <= 0) throw new IllegalArgumentException("Category id must be positive.");
        try (Connection conn = getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            // [SQL11-13] Xóa danh mục: xóa các sản phẩm thuộc nhóm trước.

            try (PreparedStatement deleteProducts = conn.prepareStatement("DELETE FROM products WHERE category_id = ?");
                 // [SQL11-14] Xóa danh mục: xóa nhóm sau khi xóa được các sản phẩm.

                 PreparedStatement deleteCategory = conn.prepareStatement("DELETE FROM categories WHERE id = ?")) {
                deleteProducts.setInt(1, id);
                deleteProducts.executeUpdate();

                deleteCategory.setInt(1, id);
                boolean deleted = deleteCategory.executeUpdate() > 0;
                if (deleted) conn.commit();
                else conn.rollback();
                return deleted;
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ignored) { }
                throw e;
            } finally {
                try { conn.setAutoCommit(previousAutoCommit); } catch (SQLException ignored) { }
            }
        }
    }

    // [MNDM01]
    // . Em đọc các danh mục đang hoạt động kèm số món đang bán để khách chọn nhóm có trong thực đơn.
    //
    // Tra cứu: Trở về MenuController.java / MNM02 sau getAllActiveCategoriesWithCount. Trang chủ tại
    //          index.jsp / TCM01 và chân trang tại footer.jsp / TCFT01 cũng dùng nguồn này.
    public List<Category> getAllActiveCategoriesWithCount() {

        List<Category> list = new ArrayList<>();
        // [SQL11-15] Trang chủ/Menu: đọc danh mục ACTIVE và số món đang bán của từng nhóm.

        String sql = "SELECT c.id, c.name, c.status, c.image_url, " +
                     "(ci.category_id IS NOT NULL) AS has_stored_image, COUNT(p.id) AS product_count " +
                     "FROM categories c " +
                     "LEFT JOIN products p ON c.id = p.category_id AND p.is_available = 1 " +
                     "LEFT JOIN category_images ci ON ci.category_id = c.id " +
                     "WHERE c.status = 'ACTIVE' " +
                     "GROUP BY c.id, c.name, c.status, c.image_url, ci.category_id " +
                     "ORDER BY c.id ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapCategory(rs, rs.getInt("product_count")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // [DMDB01]
    // . Hàm getCategoryById trong CategoryDAO đọc một danh mục theo mã để điền form hoặc kiểm tra nhóm của
    //   sản phẩm. Các hàm đọc khác trong class lấy số món, tìm tên trùng và kiểm tra món đã bán. Đọc xong,
    //   kết quả quay về đúng hàm vừa yêu cầu để tiếp tục xử lý.
    //
    // Tra cứu: Trả về đúng lời gọi getCategoryById tại AdminCategoriesController.java / DMS02,
    //          AdminCategoriesController.java / DML04 hoặc AdminProductsController.java / SPL04. Các câu đọc
    //          kiểm tra khác được gọi từ AdminCategoriesController.java / DML04 và
    //          AdminCategoriesController.java / DMX02; đọc xong tiếp tục ngay sau lời gọi ở đó.
    public Category getCategoryById(int id) {

        // [SQL11-16] Nút Sửa danh mục hoặc chọn danh mục cho món: đọc đúng một nhóm.

        String sql = "SELECT c.id, c.name, c.status, c.image_url, "
                + "(ci.category_id IS NOT NULL) AS has_stored_image "
                + "FROM categories c LEFT JOIN category_images ci ON ci.category_id = c.id WHERE c.id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCategory(rs, 0);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load category", e);
        }
        return null;
    }

    // [DMAN03]
    // . Hàm getCategoryImage trong CategoryDAO đọc nội dung và loại ảnh theo mã danh mục từ category_images.
    //   Dữ liệu quay về doGet của CategoryImageController để trả ảnh cho giao diện, chứ bước đọc này không
    //   thay ảnh đang lưu.
    //
    // Tra cứu: Trở về CategoryImageController.java / DMAN02 ngay sau getCategoryImage để trả ảnh cho giao
    //          diện.
    public CategoryImage getCategoryImage(int categoryId) {

        // [SQL11-17] Ảnh danh mục trên web: đọc dữ liệu ảnh và loại ảnh.

        String sql = "SELECT image_data, content_type FROM category_images WHERE category_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CategoryImage(rs.getBytes("image_data"), rs.getString("content_type"));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load category image", e);
        }
        return null;
    }

    private Category mapCategory(ResultSet rs, int productCount) throws SQLException {
        Category category = new Category(rs.getInt("id"), rs.getString("name"), rs.getString("status"),
                rs.getString("image_url"), productCount);
        category.setStoredImage(rs.getBoolean("has_stored_image"));
        return category;
    }
}
