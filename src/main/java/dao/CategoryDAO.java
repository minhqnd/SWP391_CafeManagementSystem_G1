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
    // . Hàm getAllCategoriesWithCount (đọc danh mục kèm số sản phẩm) trong CategoryDAO.java được doGet của
    //   AdminCategoriesController.java gọi để lấy danh sách danh mục. Em đọc categories kèm số sản phẩm từng
    //   nhóm rồi trả danh sách về doGet để đưa lên bảng trong admin-categories.jsp.
    public List<Category> getAllCategoriesWithCount() {

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

    // [DMTS01]
    // . Trong getProductCount (đếm toàn bộ sản phẩm) của CategoryDAO.java, em đếm toàn bộ sản phẩm để trả về
    //   doGet của AdminCategoriesController.java. Controller đặt số này vào productTotal rồi đưa sang thẻ
    //   Tổng sản phẩm trên trang quản lý danh mục.
    // ... Đọc tiếp tại AdminCategoriesController.java, tìm [DMTS02].

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

    // [DMDH01]
    // . Trong getOrderedProductCountByCategory (đếm món trong danh mục đã có ở đơn hàng) của
    //   CategoryDAO.java, em nhận mã danh mục từ hàm delete (kiểm tra điều kiện rồi xóa) của
    //   AdminCategoriesController.java. Em đếm sản phẩm thuộc danh mục đã xuất hiện trong order_items rồi
    //   trả số về delete, để chặn xóa nếu có lịch sử đơn hàng.
    public int getOrderedProductCountByCategory(int categoryId) {
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

    // [DMTR02]
    // . Ở categoryNameExists (tìm danh mục khác trùng tên) trong CategoryDAO.java, em nhận tên và mã danh
    //   mục đang sửa từ AdminCategoriesController.save (kiểm tra và lưu danh mục). Em tìm tên trong
    //   categories và bỏ qua mã hiện tại, rồi trả đúng hoặc sai để hàm save biết có bị trùng với danh mục
    //   khác không.
    public boolean categoryNameExists(String name, int exceptId) {
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
    // . Trong saveCategory của CategoryDAO.java, em thêm hoặc sửa categories rồi xử lý ảnh trong
    //   category_images trên cùng kết nối. Hoàn tất thì xác nhận lưu và trả true về controller. Nếu không
    //   ghi được danh mục thì trả false; nếu phát sinh lỗi thì hủy thay đổi và báo lỗi về controller.
    // ... Đọc tiếp tại AdminCategoriesController.java, tìm [DML06].
    public boolean saveCategory(Category category, byte[] imageData, String contentType,
                                String fileName, boolean removeImage)
            throws SQLException, ClassNotFoundException {

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // . Trong CategoryDAO.java, em gọi saveCategory (ghi danh mục) bằng kết nối đang dùng và đối tượng danh
                //   mục để ghi tên, trạng thái. Em tiếp tục xử lý ảnh trên cùng kết nối để thông tin và ảnh được lưu
                //   cùng nhau.
                boolean saved = saveCategory(conn, category);
                if (!saved) {
                    conn.rollback();
                    return false;
                }
                if (imageData != null) {
                    // . Nếu có ảnh mới, trong CategoryDAO.saveCategory (ghi danh mục) em gọi saveImage (thêm hoặc thay ảnh
                    //   đang lưu) cùng file, truyền mã danh mục và dữ liệu ảnh. Hàm thêm hoặc thay ảnh trong
                    //   category_images để ảnh gắn đúng danh mục vừa lưu.
                    saveImage(conn, category.getId(), imageData, contentType, fileName);
                    clearLegacyImageUrl(conn, category.getId());
                } else if (removeImage) {
                    // . Nếu không có ảnh mới nhưng người dùng chọn xóa ảnh, trong CategoryDAO.saveCategory (ghi danh mục)
                    //   em gọi deleteImage (xóa ảnh đang lưu) cùng file bằng mã danh mục. Hàm xóa ảnh đang lưu của đúng
                    //   danh mục đó.
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

        // . Khi danh mục đã có mã, em dùng UPDATE trong CategoryDAO.saveCategory (ghi danh mục) để sửa đúng dòng
        //   categories. Mã vẫn giữ trên category để phần ảnh dùng tiếp, nên thao tác này không tạo thêm danh
        //   mục.
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
        // . Khi tạo danh mục mới, em dùng INSERT trong CategoryDAO.saveCategory (ghi danh mục) rồi lấy mã được
        //   tạo bằng getGeneratedKeys (lấy mã của dòng vừa thêm). Em đặt mã vào category.id để phần lưu ảnh biết
        //   ảnh thuộc danh mục nào.
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

    // [DMAH03]
    // . Ở updateCategoryStatus (lưu trạng thái ẩn/hiện danh mục) trong CategoryDAO.java, em nhận mã và trạng
    //   thái từ AdminCategoriesController.toggle (ẩn hoặc hiện danh mục). Em sửa categories.status theo đúng
    //   mã rồi trả kết quả về toggle để controller thông báo và mở lại danh sách đã cập nhật.
    public boolean updateCategoryStatus(int id, String status) throws SQLException, ClassNotFoundException {

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE categories SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // [DMX03]
    // . Ở deleteCategoryAndProducts (xóa danh mục cùng sản phẩm đủ điều kiện) trong CategoryDAO.java, em
    //   nhận mã danh mục từ AdminCategoriesController.delete (kiểm tra điều kiện rồi xóa). Em xóa các sản
    //   phẩm thuộc nhóm rồi xóa danh mục, hoàn tất cả hai mới xác nhận. Nếu gặp lỗi thì hủy thay đổi và báo
    //   kết quả về hàm delete.
    public boolean deleteCategoryAndProducts(int id) throws SQLException, ClassNotFoundException {
        if (id <= 0) throw new IllegalArgumentException("Category id must be positive.");
        try (Connection conn = getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement deleteProducts = conn.prepareStatement("DELETE FROM products WHERE category_id = ?");
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
    // . Hàm getAllActiveCategoriesWithCount (đọc danh mục hoạt động kèm số món đang bán) trong
    //   CategoryDAO.java được MenuController.doGet và index.jsp gọi để đọc danh mục cho khách xem. Em chỉ
    //   lấy nhóm ACTIVE kèm số món đang bán, rồi trả danh sách về nơi gọi để dựng Menu hoặc Trang chủ.
    public List<Category> getAllActiveCategoriesWithCount() {

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

    // [DMDOC01]
    // . Hàm getCategoryById (đọc danh mục theo mã) trong CategoryDAO.java nhận mã từ phần sửa, xóa danh mục
    //   hoặc lưu sản phẩm. Em đọc danh mục cùng ảnh rồi trả Category về controller đang gọi để điền form,
    //   kiểm tra trước khi xóa hoặc gán sản phẩm vào nhóm.
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
            throw new IllegalStateException("Could not load category", e);
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
