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
    // [SPM03]
    // . Ở getProductsForAdmin (đọc danh sách sản phẩm để quản lý) trong ProductDAO.java, em nhận mã danh mục
    //   và từ khóa từ AdminProductsController.doGet. Em đọc các món khớp bộ lọc, gồm cả món tạm ngừng, rồi
    //   trả danh sách về controller để dựng bảng quản lý.
    public List<Product> getProductsForAdmin(Integer categoryId, String keyword) throws SQLException, ClassNotFoundException {

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

    // [SPTS01]
    // . Trong getProductAdminStats (đọc tổng món và số đang bán) của ProductDAO.java, em đọc tổng sản phẩm
    //   và số đang bán từ bảng products. Em trả hai số về AdminProductsController.doGet để tính thêm số tạm
    //   ngừng rồi đưa lên ba thẻ thống kê.
    // ... Đọc tiếp tại AdminProductsController.java, tìm [SPTS02].
    public int[] getProductAdminStats() throws SQLException, ClassNotFoundException {
        String sql = "SELECT COUNT(*) AS total, COALESCE(SUM(is_available = 1), 0) AS available FROM products";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return new int[]{rs.getInt("total"), rs.getInt("available")};
        }
        return new int[]{0, 0};
    }

    // [CTCH03]
    // . Hàm getProductForAdmin (đọc một sản phẩm để quản lý) trong ProductDAO.java nhận mã sản phẩm để đọc
    //   món kèm danh mục và thông tin ảnh. Em trả kết quả về AdminProductsController.doGet để điền form Sửa,
    //   hoặc AdminRecipeController.doPost để kiểm tra món vẫn tồn tại trước khi lưu công thức.
    public Product getProductForAdmin(int id) throws SQLException, ClassNotFoundException {

        String sql = "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image FROM products p LEFT JOIN categories c ON c.id = p.category_id WHERE p.id = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapResultSetToProduct(rs) : null;
            }
        }
    }

    /** Checks product-name uniqueness while allowing the current product during an edit. */
    // [SPTR02]
    // . Ở productNameExists (tìm sản phẩm khác trùng tên) trong ProductDAO.java, em nhận tên món và mã đang
    //   sửa từ AdminProductsController.save (kiểm tra và lưu sản phẩm). Em tìm sản phẩm khác cùng tên trong
    //   products rồi trả đúng hoặc sai, để controller chặn tên trùng trước khi lưu.
    public boolean productNameExists(String name, int excludedId) throws SQLException, ClassNotFoundException {
        String sql = "SELECT 1 FROM products WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) AND id <> ? LIMIT 1";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, Math.max(0, excludedId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean saveProduct(Product product) throws SQLException, ClassNotFoundException {
        return saveProduct(product, null, null, null, false);
    }

    // [SPL06]
    // . Trong saveProduct của ProductDAO.java, em thêm hoặc sửa dòng products rồi xử lý product_images trên
    //   cùng kết nối. Ghi xong thì xác nhận và trả true về controller. Nếu không ghi được sản phẩm thì trả
    //   false; nếu có lỗi thì hủy thay đổi rồi báo lỗi về controller.
    // ... Đọc tiếp tại AdminProductsController.java, tìm [SPL07].
    public boolean saveProduct(Product product, byte[] imageData, String contentType,
                               String fileName, boolean removeImage)
            throws SQLException, ClassNotFoundException {

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // . Trong ProductDAO.java, em gọi saveProduct (ghi sản phẩm) bằng kết nối đang dùng và đối tượng sản
                //   phẩm để ghi thông tin món. Em xử lý ảnh trên cùng kết nối để toàn bộ thay đổi của sản phẩm được lưu
                //   cùng nhau.
                boolean saved = saveProduct(conn, product);
                if (!saved) {
                    conn.rollback();
                    return false;
                }
                if (imageData != null) {
                    // . Nếu có ảnh mới, trong ProductDAO.saveProduct (ghi sản phẩm) em gọi saveImage (thêm hoặc thay ảnh
                    //   đang lưu) cùng file bằng mã sản phẩm và dữ liệu ảnh. Hàm thêm hoặc thay product_images để ảnh gắn
                    //   đúng món vừa lưu.
                    saveImage(conn, product.getId(), imageData, contentType, fileName);
                    clearLegacyImageUrl(conn, product.getId());
                } else if (removeImage) {
                    // . Nếu không có ảnh mới và người dùng chọn xóa ảnh, trong ProductDAO.saveProduct (ghi sản phẩm) em gọi
                    //   deleteImage (xóa ảnh đang lưu) cùng file bằng mã sản phẩm. Hàm xóa ảnh cũ của đúng món đó.
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

        // . Khi sản phẩm đã có mã, em dùng UPDATE trong ProductDAO.saveProduct (ghi sản phẩm) để sửa đúng dòng
        //   products. Em giữ mã trên product để bước xử lý ảnh tiếp tục dùng mà không tạo sản phẩm mới.
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
        // . Khi tạo món mới, em dùng INSERT trong ProductDAO.saveProduct (ghi sản phẩm) rồi lấy mã bằng
        //   getGeneratedKeys (lấy mã của dòng vừa thêm). Em đặt mã vào product.id để phần lưu ảnh gắn ảnh với
        //   đúng sản phẩm vừa tạo.
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

    // [SPAH03]
    // . Ở updateProductAvailability (lưu trạng thái đang bán/tạm ngừng) trong ProductDAO.java, em nhận mã
    //   món và trạng thái từ AdminProductsController.toggle (đổi trạng thái bán của sản phẩm). Em sửa
    //   products.is_available theo mã đó rồi trả kết quả về toggle để controller thông báo và tải lại danh
    //   sách.
    public boolean updateProductAvailability(int id, boolean available) throws SQLException, ClassNotFoundException {

        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE products SET is_available = ? WHERE id = ?")) {
            ps.setBoolean(1, available);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // [SPX03]
    // . Ở deleteProduct (kiểm tra điều kiện rồi xóa sản phẩm) trong ProductDAO.java, em nhận mã món từ
    //   AdminProductsController.delete (kiểm tra điều kiện rồi xóa). Em chỉ xóa khi món đã ngừng bán và chưa
    //   có trong order_items. Kết quả trả về cho controller biết đã xóa được hay phải báo chưa đủ điều kiện.
    public boolean deleteProduct(int id) throws SQLException, ClassNotFoundException {
        String sql = "DELETE FROM products "
                + "WHERE id = ? AND is_available = 0 "
                + "AND NOT EXISTS (SELECT 1 FROM order_items WHERE product_id = ?)";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
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
                     "WHERE p.is_available = 1 AND c.status = 'ACTIVE' " +
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
                     "WHERE p.is_available = 1 AND c.status = 'ACTIVE' " +
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
                     "WHERE p.is_available = 1 AND c.status = 'ACTIVE' AND p.category_id = ? " +
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
                     "WHERE p.is_available = 1 AND c.status = 'ACTIVE' " +
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

    // [MNDS02]
    // . Ở filterProducts (lọc món còn bán theo danh mục và từ khóa) trong ProductDAO.java, em nhận danh mục
    //   và từ khóa từ MenuController.doGet. Em chỉ đọc món đang bán thuộc danh mục ACTIVE và khớp bộ lọc,
    //   rồi trả danh sách về controller để đưa lên menu.jsp.
    public List<Product> filterProducts(Integer categoryId, String keyword) {

        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image " +
                "FROM products p " +
                "JOIN categories c ON p.category_id = c.id " +
                "WHERE p.is_available = 1 AND c.status = 'ACTIVE' "
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

    /** Public product lookup: only an available product in an active category is exposed. */
    // [MNTHE03]
    // . Ở getAvailableProductById (đọc món còn bán theo mã) trong ProductDAO.java, em nhận mã món từ
    //   ProductDetailController.doGet. Em chỉ trả sản phẩm còn được bán để controller mở product-detail.jsp;
    //   nếu không tìm được thì controller đưa người dùng về Menu.
    // ... Đọc tiếp tại product-detail.jsp, tìm [MNTHE04].

    public Product getAvailableProductById(int id) {
        String sql = "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image "
                + "FROM products p JOIN categories c ON p.category_id = c.id "
                + "WHERE p.id = ? AND p.is_available = 1 AND c.status = 'ACTIVE'";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapResultSetToProduct(rs) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load available product", e);
        }
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
                     "WHERE p.is_available = 1 AND c.status = 'ACTIVE' AND p.category_id = ? AND p.id != ? " +
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

    // [MNBC01]
    // . Ở getBestSellingProducts (đọc các món nổi bật theo lượng bán) trong ProductDAO.java, em nhận giới
    //   hạn 4 từ index.jsp. Em chọn các món còn bán theo lượng bán rồi trả danh sách về Trang chủ để hiện
    //   banner và phần sản phẩm yêu thích.
    public List<Product> getBestSellingProducts(int limit) {

        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image, COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN oi.quantity ELSE 0 END), 0) as total_sold " +
                     "FROM products p " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "LEFT JOIN order_items oi ON p.id = oi.product_id " +
                     "LEFT JOIN orders o ON oi.order_id = o.id AND o.status IN ('CONFIRMED', 'PREPARING', 'READY', 'DELIVERING', 'COMPLETED') " +
                     "WHERE p.is_available = 1 AND c.status = 'ACTIVE' " +
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
