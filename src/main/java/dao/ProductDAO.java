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
    // . Hàm getProductsForAdmin trong ProductDAO đọc danh sách món theo từ khóa và danh mục được chọn. Trang
    //   quản trị nhận cả món đang bán và tạm ngừng. Đọc xong, danh sách quay về hàm doGet của màn hình đang
    //   gọi để hiển thị.
    //
    // Tra tiếp: admin-products.jsp / SPM04.
    public List<Product> getProductsForAdmin(Integer categoryId, String keyword) throws SQLException, ClassNotFoundException {

        List<Product> list = new ArrayList<>();
        // [SQL11-18] Bảng sản phẩm và bộ lọc quản trị: ghép câu đọc theo danh mục, từ khóa.

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
    // . Hàm getProductAdminStats trong ProductDAO lấy tổng số sản phẩm và số đang bán. Em trả hai số này về
    //   doGet của AdminProductsController để tính thêm số tạm ngừng và hiện các thẻ thống kê.
    //
    // Tra cứu: Trả về AdminProductsController.java / SPM02 ngay sau getProductAdminStats để hiển thị tại
    //          admin-products.jsp / SPM04.
    public int[] getProductAdminStats() throws SQLException, ClassNotFoundException {
        // [SQL11-19] Ba thẻ sản phẩm: SQL trả tổng món và số đang bán.

        String sql = "SELECT COUNT(*) AS total, COALESCE(SUM(is_available = 1), 0) AS available FROM products";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return new int[]{rs.getInt("total"), rs.getInt("available")};
        }
        return new int[]{0, 0};
    }

    // [SPDB01]
    // . Hàm getProductForAdmin trong ProductDAO đọc một sản phẩm theo mã, kể cả món tạm ngừng. Hàm đang yêu
    //   cầu sẽ nhận lại món này để điền form sửa, kiểm tra trước thao tác hoặc lưu công thức cho đúng món.
    //
    // Tra cứu: Trở về đúng lời gọi getProductForAdmin tại AdminProductsController.java / SPS02,
    //          AdminProductsController.java / SPL04, AdminProductsController.java / SPAH02,
    //          AdminProductsController.java / SPX02 hoặc AdminRecipeController.java / CTL03. Các hàm đọc khác
    //          trong ProductDAO hỗ trợ tìm tên trùng và số liệu quản trị.
    public Product getProductForAdmin(int id) throws SQLException, ClassNotFoundException {

        // [SQL11-20] Nút Sửa sản phẩm/chọn món làm công thức: đọc món theo mã, kể cả món tạm ngừng.

        String sql = "SELECT p.*, c.name AS category_name, EXISTS(SELECT 1 FROM product_images pi WHERE pi.product_id=p.id) AS has_stored_image FROM products p LEFT JOIN categories c ON c.id = p.category_id WHERE p.id = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapResultSetToProduct(rs) : null;
            }
        }
    }

    /** Checks product-name uniqueness while allowing the current product during an edit. */

    public boolean productNameExists(String name, int excludedId) throws SQLException, ClassNotFoundException {
        // [SQL11-21] Ô Tên sản phẩm: tìm tên trùng, bỏ qua món đang sửa.

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

    // [SPL05]
    // . Hàm saveProduct trong ProductDAO ghi thông tin món và xử lý ảnh trong cùng một lần lưu. Ghi đủ mới
    //   xác nhận thành công; nếu gặp lỗi thì hủy các thay đổi. Kết quả quay về hàm save trong
    //   AdminProductsController để quyết định báo đã lưu hay báo lỗi.
    //
    // Tra tiếp: AdminProductsController.java / SPL06.
    public boolean saveProduct(Product product, byte[] imageData, String contentType,
                               String fileName, boolean removeImage)
            throws SQLException, ClassNotFoundException {

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

        if (product.getId() > 0) {
            // [SQL11-22] Nút Lưu sản phẩm: cập nhật món hiện có.

            String sql = "UPDATE products SET category_id = ?, name = ?, base_price = ?, variants_config = ?, description = ?, is_available = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindProduct(ps, product);
                ps.setInt(7, product.getId());
                if (ps.executeUpdate() > 0) return true;
                // [SQL11-23] Lưu sản phẩm không đổi giá trị: kiểm tra món vẫn tồn tại.

                try (PreparedStatement exists = conn.prepareStatement("SELECT 1 FROM products WHERE id = ?")) {
                    exists.setInt(1, product.getId());
                    try (ResultSet rs = exists.executeQuery()) { return rs.next(); }
                }
            }
        }

        // [SQL11-24] Nút Thêm sản phẩm: tạo món mới và lấy mã vừa tạo.

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
        // [SQL11-25] Ảnh sản phẩm: thêm hoặc thay ảnh lưu trong database.

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
        // [SQL11-26] Ô Xóa ảnh hiện tại của sản phẩm: xóa ảnh đang lưu.

        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM product_images WHERE product_id = ?")) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }

    private void clearLegacyImageUrl(Connection conn, int productId) throws SQLException {
        // [SQL11-27] Thay hoặc xóa ảnh sản phẩm: bỏ đường dẫn ảnh cũ.

        try (PreparedStatement ps = conn.prepareStatement("UPDATE products SET image_url = NULL WHERE id = ?")) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }

    // [SPAH03]
    // . Hàm updateProductAvailability trong ProductDAO cập nhật trạng thái bán của đúng món trong bảng
    //   products. Kết quả quay về hàm toggle của AdminProductsController để tiếp tục báo kết quả.
    //
    // Tra tiếp: AdminProductsController.java / SPAH04.
    public boolean updateProductAvailability(int id, boolean available) throws SQLException, ClassNotFoundException {

        // [SQL11-28] Nút Đang bán/Tạm ngừng: đổi trạng thái bán của món.

        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE products SET is_available = ? WHERE id = ?")) {
            ps.setBoolean(1, available);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // [SPX03]
    // . Hàm deleteProduct trong ProductDAO chỉ xóa món đã tạm ngừng và chưa thuộc đơn hàng. Nếu món đã được
    //   đặt trong đơn thì từ chối xóa. Kết quả quay về hàm delete của AdminProductsController để báo cho
    //   người dùng.
    //
    // Tra tiếp: AdminProductsController.java / SPX04.
    public boolean deleteProduct(int id) throws SQLException, ClassNotFoundException {
        // [SQL11-29] Nút Xóa sản phẩm: chỉ xóa món tạm ngừng và chưa có trong order_items.

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

    // [SPAN03]
    // . Hàm getProductImage trong ProductDAO lấy nội dung và loại ảnh theo mã món từ product_images. Dữ liệu
    //   được trả về doGet của ProductImageController để hiển thị ảnh của đúng sản phẩm.
    //
    // Tra cứu: Trở về ProductImageController.java / SPAN02 ngay sau getProductImage để trả ảnh cho giao diện.
    public ProductImage getProductImage(int productId) {

        // [SQL11-30] Ảnh sản phẩm trên web: đọc ảnh đang lưu theo mã món.

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

    // [MNM03]
    // . Hàm filterProducts trong ProductDAO đọc các món đang bán thuộc danh mục hoạt động, rồi lọc theo nhóm
    //   và từ khóa. Danh sách quay về doGet của MenuController để hiển thị đúng kết quả khách đang tìm.
    //
    // Tra tiếp: menu.jsp / MNM04.
    public List<Product> filterProducts(Integer categoryId, String keyword) {

        List<Product> list = new ArrayList<>();
        // [SQL11-31] Tìm/lọc trên Menu: chỉ đọc món đang bán trong danh mục ACTIVE.

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
    // [MNCT04]
    // . Hàm getAvailableProductById trong ProductDAO đọc món còn được bán và thuộc danh mục hoạt động. Kết
    //   quả quay về controller đang gọi để dựng trang hoặc hộp chi tiết, nên món hiển thị ở đây vẫn tuân theo
    //   điều kiện của menu.
    //
    // Tra cứu: Trở về MenuController.java / MNCT03 hoặc ProductDetailController.java / MNTHE02 ngay sau
    //          getAvailableProductById, tùy đang mở hộp hay trang chi tiết.

    public Product getAvailableProductById(int id) {
        // [SQL11-32] Mở chi tiết món trên Menu: chỉ trả món đang bán thuộc nhóm ACTIVE.

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
        // [SQL11-33] Con số tổng món ở Menu: đếm products có is_available=1.

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
        // [SQL11-34] Phần Món liên quan: chọn món cùng nhóm, bỏ món đang xem.

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

    // [TCSP02]
    // . Em chọn các món đang bán theo lượng đã bán trong đơn hàng, giới hạn số món mà trang chủ yêu cầu.
    //
    // Tra cứu: Trở về index.jsp / TCSP01 ngay sau getBestSellingProducts để dựng các thẻ món. Kết thúc phần
    //          đọc món giới thiệu.
    public List<Product> getBestSellingProducts(int limit) {

        List<Product> list = new ArrayList<>();
        // [SQL11-35] Banner/Sản phẩm yêu thích: xếp món theo tổng số lượng bán.

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
