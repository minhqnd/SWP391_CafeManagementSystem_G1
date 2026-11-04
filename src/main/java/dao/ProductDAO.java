package dao;

import context.DBContext;
import model.Product;
import model.ProductImage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * ProductDAO - Tương tác dữ liệu sản phẩm / thực đơn
 */
public class ProductDAO extends BaseDAO {

    /** All products for the admin catalog, including temporarily unavailable items. */
    public List<Product> getProductsForAdmin(Integer categoryId, String keyword) throws SQLException, ClassNotFoundException {
        // BF11: quản trị cần thấy cả món đang bán/ngừng bán để sửa hoặc bật lại; không giống bộ lọc công khai.
        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image FROM products p LEFT JOIN categories c ON c.id = p.category_id WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND p.category_id = ?");
            params.add(categoryId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (p.name LIKE ? OR CAST(p.id AS CHAR) LIKE ? OR COALESCE(p.description, '') LIKE ? OR COALESCE(c.name, '') LIKE ?)");
            String term = "%" + keyword.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }
        sql.append(" ORDER BY p.id DESC");
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToProduct(rs));
            }
        }
        return list;
    }

    public int[] getProductAdminStats() throws SQLException, ClassNotFoundException {
        String sql = "SELECT COUNT(*) AS total, COALESCE(SUM(is_available = 1), 0) AS available FROM products";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return new int[]{rs.getInt("total"), rs.getInt("available")};
        }
        return new int[]{0, 0};
    }

    public Product getProductForAdmin(int id) throws SQLException, ClassNotFoundException {
        String sql = "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image FROM products p LEFT JOIN categories c ON c.id = p.category_id WHERE p.id = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapResultSetToProduct(rs) : null;
            }
        }
    }

    public boolean saveProduct(Product product) throws SQLException, ClassNotFoundException {
        return saveProduct(product, null, null, null, false);
    }

    public boolean saveProduct(Product product, byte[] imageData, String contentType,
                               String fileName, boolean removeImage)
            throws SQLException, ClassNotFoundException {
        // BF11: lưu products (gồm variants_config) và product_images cùng transaction, lỗi thì rollback.
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean saved = saveProduct(conn, product);
                if (!saved) {
                    conn.rollback();
                    return false;
                }
                if (imageData != null) {
                    saveImage(conn, product.getId(), imageData, contentType, fileName);
                    clearLegacyImageUrl(conn, product.getId());
                } else if (removeImage) {
                    deleteImage(conn, product.getId());
                    clearLegacyImageUrl(conn, product.getId());
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

    private boolean saveProduct(Connection conn, Product product) throws SQLException {
        // BF11: id>0 UPDATE, tạo mới thì INSERT rồi nhận ID để lưu ảnh vào đúng món.
        if (product.getId() > 0) {
            String sql = "UPDATE products SET category_id = ?, name = ?, base_price = ?, variants_config = ?, description = ?, is_available = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindProduct(ps, product);
                ps.setInt(7, product.getId());
                if (ps.executeUpdate() > 0) return true;
                try (PreparedStatement exists = conn.prepareStatement("SELECT 1 FROM products WHERE id = ?")) {
                    exists.setInt(1, product.getId());
                    try (ResultSet rs = exists.executeQuery()) { return rs.next(); }
                }
            }
        }
        String sql = "INSERT INTO products (category_id, name, base_price, variants_config, description, is_available) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindProduct(ps, product);
            if (ps.executeUpdate() == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) product.setId(keys.getInt(1));
            }
            return product.getId() > 0;
        }
    }

    private void bindProduct(PreparedStatement ps, Product product) throws SQLException {
        ps.setInt(1, product.getCategoryId());
        ps.setString(2, product.getName());
        ps.setDouble(3, product.getBasePrice());
        ps.setString(4, product.getVariantsConfig());
        ps.setString(5, product.getDescription());
        ps.setBoolean(6, product.isAvailable());
    }

    private void saveImage(Connection conn, int productId, byte[] data, String contentType, String fileName)
            throws SQLException {
        String sql = "INSERT INTO product_images (product_id, image_data, content_type, file_name) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE image_data=VALUES(image_data), content_type=VALUES(content_type), "
                + "file_name=VALUES(file_name), updated_at=CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setBytes(2, data);
            ps.setString(3, contentType);
            ps.setString(4, fileName);
            ps.executeUpdate();
        }
    }

    private void deleteImage(Connection conn, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM product_images WHERE product_id = ?")) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }

    private void clearLegacyImageUrl(Connection conn, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE products SET image_url = NULL WHERE id = ?")) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }

    public boolean updateProductAvailability(int id, boolean available) throws SQLException, ClassNotFoundException {
        // BF11: nút mắt chỉ đổi is_available; không xóa món hay các order_items đã tham chiếu.
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE products SET is_available = ? WHERE id = ?")) {
            ps.setBoolean(1, available);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        java.sql.ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equalsIgnoreCase(rsmd.getColumnName(x)) || 
                columnName.equalsIgnoreCase(rsmd.getColumnLabel(x))) {
                return true;
            }
        }
        return false;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        String desc = hasColumn(rs, "description") ? rs.getString("description") : null;
        
        Product product = new Product(
                rs.getInt("id"),
                rs.getInt("category_id"),
                hasColumn(rs, "category_name") ? rs.getString("category_name") : null,
                rs.getString("name"),
                rs.getDouble("base_price"),
                desc,
                rs.getString("variants_config"),
                rs.getString("image_url"),
                rs.getBoolean("is_available")
        );
        product.setStoredImage(hasColumn(rs, "has_stored_image") && rs.getBoolean("has_stored_image"));
        return product;
    }

    public ProductImage getProductImage(int productId) {
        String sql = "SELECT image_data, content_type FROM product_images WHERE product_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new ProductImage(rs.getBytes("image_data"), rs.getString("content_type"));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load product image", e);
        }
        return null;
    }

    /**
     * Dùng cho trang chủ (Homepage) lấy tối đa 8 sản phẩm
     */
    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image " +
                     "FROM products p " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.is_available = 1 " +
                     "LIMIT 8";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToProduct(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy toàn bộ sản phẩm khả dụng cho trang Menu
     */
    public List<Product> getAllAvailableProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image " +
                     "FROM products p " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.is_available = 1 " +
                     "ORDER BY p.id ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToProduct(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Product> getProductsByCategory(int categoryId) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image " +
                     "FROM products p " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.is_available = 1 AND p.category_id = ? " +
                     "ORDER BY p.id ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Product> searchProducts(String keyword) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image " +
                     "FROM products p " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.is_available = 1 " +
                     "  AND (p.name LIKE ? OR c.name LIKE ? OR p.description LIKE ?) " +
                     "ORDER BY p.id ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String term = "%" + keyword.trim() + "%";
            ps.setString(1, term);
            ps.setString(2, term);
            ps.setString(3, term);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Product> filterProducts(Integer categoryId, String keyword) {
        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image " +
                "FROM products p " +
                "JOIN categories c ON p.category_id = c.id " +
                "WHERE p.is_available = 1 "
        );
        List<Object> params = new ArrayList<>();

        if (categoryId != null && categoryId > 0) {
            sql.append(" AND p.category_id = ? ");
            params.add(categoryId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (p.name LIKE ? OR c.name LIKE ? OR p.description LIKE ?) ");
            String term = "%" + keyword.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        sql.append(" ORDER BY p.id ASC");

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Product getProductById(int id) {
        // BF11: lấy thông tin/JSON cấu hình của một món; hàm này chỉ lọc ID, không tự lọc is_available.
        String sql = "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image " +
                     "FROM products p " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getTotalAvailableCount() {
        String sql = "SELECT COUNT(*) FROM products WHERE is_available = 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Lấy danh sách sản phẩm liên quan (cùng danh mục), loại trừ sản phẩm hiện tại
     */
    public List<Product> getRelatedProducts(int categoryId, int excludeProductId, int limit) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image " +
                     "FROM products p " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.is_available = 1 AND p.category_id = ? AND p.id != ? " +
                     "ORDER BY RAND() " +
                     "LIMIT ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            ps.setInt(2, excludeProductId);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Product> getBestSellingProducts(int limit) {
        // BF10/BF11: index.jsp lấy món đang bán, xếp theo lượng bán ở các trạng thái đơn trong SQL.
        // Đọc điều kiện WHERE thực tế: hàm này lọc is_available, không có điều kiện category.status.
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image, COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN oi.quantity ELSE 0 END), 0) as total_sold " +
                     "FROM products p " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "LEFT JOIN order_items oi ON p.id = oi.product_id " +
                     "LEFT JOIN orders o ON oi.order_id = o.id AND o.status IN ('CONFIRMED', 'PREPARING', 'READY', 'DELIVERING', 'COMPLETED') " +
                     "WHERE p.is_available = 1 " +
                     "GROUP BY p.id " +
                     "ORDER BY total_sold DESC, p.id ASC " +
                     "LIMIT ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
