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

    /** Returns every category, including inactive ones, with the number of linked products. */
    public List<Category> getAllCategoriesWithCount() {
        // BF11: danh sách quản trị gồm mọi trạng thái và số món; khác danh sách công khai chỉ ACTIVE.
        List<Category> list = new ArrayList<>();
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

    public boolean categoryNameExists(String name, int exceptId) {
        String sql = "SELECT 1 FROM categories WHERE LOWER(name) = LOWER(?) AND id <> ? LIMIT 1";
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

    public boolean saveCategory(Category category, byte[] imageData, String contentType,
                                String fileName, boolean removeImage)
            throws SQLException, ClassNotFoundException {
        // BF11: lưu categories và category_images cùng transaction; lỗi ảnh thì hoàn tác cả danh mục.
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
        // id>0 UPDATE; id=0 INSERT và lấy generated key để liên kết ảnh theo category_id.
        if (category.getId() > 0) {
            String sql = "UPDATE categories SET name = ?, status = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindCategory(ps, category);
                ps.setInt(3, category.getId());
                int updated = ps.executeUpdate();
                if (updated > 0) return true;
                try (PreparedStatement exists = conn.prepareStatement("SELECT 1 FROM categories WHERE id = ?")) {
                    exists.setInt(1, category.getId());
                    try (ResultSet rs = exists.executeQuery()) {
                        return rs.next();
                    }
                }
            }
        }
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
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM category_images WHERE category_id = ?")) {
            ps.setInt(1, categoryId);
            ps.executeUpdate();
        }
    }

    private void clearLegacyImageUrl(Connection conn, int categoryId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE categories SET image_url = NULL WHERE id = ?")) {
            ps.setInt(1, categoryId);
            ps.executeUpdate();
        }
    }

    public boolean updateCategoryStatus(int id, String status) throws SQLException, ClassNotFoundException {
        // BF11: chỉ UPDATE status, không xóa danh mục/sản phẩm.
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE categories SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteCategory(int id) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM categories WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Category> getAllActiveCategoriesWithCount() {
        // BF10/BF11: danh mục công khai lấy ACTIVE; dùng ở trang chủ và menu.
        List<Category> list = new ArrayList<>();
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

    public Category getCategoryById(int id) {
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
            e.printStackTrace();
        }
        return null;
    }

    public CategoryImage getCategoryImage(int categoryId) {
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
