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
    // ... Quay qua doGet trong AdminCategoriesController.java để đặt categories; xem bảng trả về tại
    //     admin-categories.jsp, tìm [DMD01].
    public List<Category> getAllCategoriesWithCount() {

        List<Category> list = new ArrayList<>();
        // [SQL11-01] Bảng danh mục và lựa chọn danh mục ở quản lý sản phẩm: đọc nhóm, trạng thái, ảnh và số
        //   món.
        // . COUNT(p.id) đếm sản phẩm của từng nhóm, gồm cả món tạm ngừng; LEFT JOIN giữ cả nhóm chưa có món.
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
        // . Dấu ? từ trái sang phải: 1=mã danh mục.
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
    //   AdminCategoriesController.java. Em đếm sản phẩm thuộc danh mục đã xuất hiện trong order_items
    //   rồi trả số về delete, để chặn xóa nếu có lịch sử đơn hàng.
    // ... Trả số món đã có trong đơn về AdminCategoriesController.delete. Số lớn hơn 0 thì báo lỗi và
    //     dừng; bằng 0 mới gọi hàm xóa tại CategoryDAO.java, tìm [DMX03].
    public int getOrderedProductCountByCategory(int categoryId) {
        // [SQL11-04] Nút Xóa danh mục: đếm món đã xuất hiện trong đơn hàng.
        // . Dấu ? từ trái sang phải: 1=mã danh mục.
        // . COUNT DISTINCT tránh đếm một món nhiều lần; có kết quả lớn hơn 0 thì controller chặn xóa để giữ
        //   lịch sử.
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
    // . Ở categoryNameExists (tìm danh mục khác trùng tên) trong CategoryDAO.java, em nhận tên và mã
    //   danh mục đang sửa từ AdminCategoriesController.save (kiểm tra và lưu danh mục). Em tìm tên trong
    //   categories và bỏ qua mã hiện tại, rồi trả đúng hoặc sai để hàm save biết có bị trùng với danh
    //   mục khác không.
    // ... Quay về AdminCategoriesController.save sau categoryNameExists. Trùng thì báo lỗi; đúng thì
    //     tiếp tục tới điểm lưu tại AdminCategoriesController.java, tìm mã DML04.
    public boolean categoryNameExists(String name, int exceptId) {
        // [SQL11-05] Ô Tên danh mục: kiểm tra trùng tên, bỏ qua danh mục đang sửa.
        // . Dấu ? từ trái sang phải: 1=tên danh mục; 2=mã danh mục đang sửa; thêm mới dùng 0.
        // . LOWER/TRIM bỏ khác biệt hoa thường và khoảng trắng đầu/cuối.
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
    // . AdminCategoriesController.save gọi hàm này để lưu thông tin danh mục và ảnh. Phần lưu thông tin
    //   bên dưới dùng id để chọn INSERT khi thêm mới hoặc UPDATE khi sửa; ảnh được xử lý trong
    //   category_images.
    // . Cả thông tin và ảnh được lưu trong cùng lần xử lý. Hoàn tất thì trả true về controller; không
    //   lưu được danh mục thì trả false, còn lỗi phát sinh thì hủy thay đổi và báo lỗi về controller.
    // ... Ghi thành công thì về controller tại AdminCategoriesController.java, tìm [DML06]. Trả false
    //     hoặc có lỗi thì doPost giữ form; đọc phần giữ tại AdminCategoriesController.java, tìm mã DML08
    //     rồi xem thông báo trên trang.
    public boolean saveCategory(Category category, byte[] imageData, String contentType,
                                String fileName, boolean removeImage)
            throws SQLException, ClassNotFoundException {

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // . Em gọi saveCategory(conn, category) cùng file để ghi tên và trạng thái trước.
                //   Kết nối conn được dùng tiếp cho ảnh để thông tin và ảnh cùng thành công hoặc cùng bị hủy.
                boolean saved = saveCategory(conn, category);
                if (!saved) {
                    conn.rollback();
                    return false;
                }
                if (imageData != null) {
                    // . Có ảnh mới thì em gọi saveImage cùng file, truyền mã danh mục và dữ liệu tệp.
                    //   Hàm thêm hoặc thay ảnh trong category_images cho đúng danh mục vừa lưu.
                    saveImage(conn, category.getId(), imageData, contentType, fileName);
                    clearLegacyImageUrl(conn, category.getId());
                } else if (removeImage) {
                    // . Người dùng chọn xóa ảnh và không gửi ảnh mới thì em gọi deleteImage cùng file.
                    //   Hàm nhận mã danh mục để xóa ảnh của đúng nhóm trong category_images.
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

        // . id lớn hơn 0 nghĩa là sửa danh mục đã có. Em dùng UPDATE với điều kiện WHERE id để sửa đúng nhóm;
        //   mã danh mục giữ nguyên để bước lưu ảnh tiếp tục dùng.
        if (category.getId() > 0) {
            // [SQL11-06] Nút Lưu danh mục: cập nhật tên và trạng thái.
            // . Dấu ? từ trái sang phải: 1=tên danh mục; 2=ACTIVE hoặc INACTIVE; 3=mã danh mục.
            String sql = "UPDATE categories SET name = ?, status = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindCategory(ps, category);
                ps.setInt(3, category.getId());
                int updated = ps.executeUpdate();
                if (updated > 0) return true;
                // [SQL11-07] Lưu danh mục không đổi giá trị: kiểm tra danh mục vẫn tồn tại.
                // . Dấu ? từ trái sang phải: 1=mã danh mục.
                // . Được dùng khi UPDATE báo không đổi dòng nào; có dòng vẫn được xem là lưu đúng danh mục.
                try (PreparedStatement exists = conn.prepareStatement("SELECT 1 FROM categories WHERE id = ?")) {
                    exists.setInt(1, category.getId());
                    try (ResultSet rs = exists.executeQuery()) {
                        return rs.next();
                    }
                }
            }
        }
        // . id bằng 0 nghĩa là tạo danh mục mới. Em dùng INSERT rồi gọi getGeneratedKeys để lấy mã database vừa tạo.
        //   Em đặt mã này vào category.id để ảnh được lưu cho đúng danh mục mới.

        // [SQL11-08] Nút Tạo danh mục: thêm nhóm mới và lấy mã vừa tạo.
        // . Dấu ? từ trái sang phải: 1=tên danh mục; 2=ACTIVE hoặc INACTIVE.
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
        // . Dấu ? từ trái sang phải: 1=mã danh mục; 2=dữ liệu nhị phân của ảnh; 3=loại ảnh; 4=tên tệp.
        // . Tham số ảnh là dữ liệu tệp do Java đọc; khi kiểm chứng trên MySQL nên SELECT thông tin ảnh thay vì
        //   dán dữ liệu nhị phân.
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
        // . Dấu ? từ trái sang phải: 1=mã danh mục.
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM category_images WHERE category_id = ?")) {
            ps.setInt(1, categoryId);
            ps.executeUpdate();
        }
    }

    private void clearLegacyImageUrl(Connection conn, int categoryId) throws SQLException {
        // [SQL11-11] Thay hoặc xóa ảnh danh mục: bỏ đường dẫn ảnh cũ.
        // . Dấu ? từ trái sang phải: 1=mã danh mục.
        try (PreparedStatement ps = conn.prepareStatement("UPDATE categories SET image_url = NULL WHERE id = ?")) {
            ps.setInt(1, categoryId);
            ps.executeUpdate();
        }
    }

    // [DMAH03]
    // . Ở updateCategoryStatus (lưu trạng thái ẩn/hiện danh mục) trong CategoryDAO.java, em nhận mã và
    //   trạng thái từ AdminCategoriesController.toggle (ẩn hoặc hiện danh mục). Em sửa categories.status
    //   theo đúng mã rồi trả kết quả về toggle để controller thông báo và mở lại danh sách đã cập nhật.
    // ... Trả true thì controller đặt thông báo tại AdminCategoriesController.java, tìm [DMAH04]. Trả
    //     false hoặc có lỗi thì doPost bắt lỗi và mở lại trang; xem kết quả lỗi tại
    //     admin-categories.jsp, tìm mã DML07.
    public boolean updateCategoryStatus(int id, String status) throws SQLException, ClassNotFoundException {

        // [SQL11-12] Nút Ẩn/Hiện danh mục: đổi trạng thái của đúng nhóm.
        // . Dấu ? từ trái sang phải: 1=ACTIVE hoặc INACTIVE; 2=mã danh mục.
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE categories SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // [DMX03]
    // . Ở deleteCategoryAndProducts (xóa danh mục cùng sản phẩm đủ điều kiện) trong CategoryDAO.java, em
    //   nhận mã danh mục từ AdminCategoriesController.delete (kiểm tra điều kiện rồi xóa). Em xóa các
    //   sản phẩm thuộc nhóm rồi xóa danh mục, hoàn tất cả hai mới xác nhận. Nếu gặp lỗi thì hủy thay đổi
    //   và báo kết quả về hàm delete.
    // ... Xóa thành công thì controller đặt thông báo tại AdminCategoriesController.java, tìm [DMX04].
    //     Không xóa được hoặc có lỗi thì doPost mở lại trang cùng lỗi tại admin-categories.jsp, tìm mã
    //     DML07; chưa báo đã xóa.
    public boolean deleteCategoryAndProducts(int id) throws SQLException, ClassNotFoundException {
        if (id <= 0) throw new IllegalArgumentException("Category id must be positive.");
        try (Connection conn = getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            // [SQL11-13] Xóa danh mục: xóa các sản phẩm thuộc nhóm trước.
            // . Dấu ? từ trái sang phải: 1=mã danh mục.
            // . Controller đã kiểm tra lịch sử đơn hàng; lệnh này và lệnh xóa danh mục phải cùng thành công mới
            //   xác nhận.
            try (PreparedStatement deleteProducts = conn.prepareStatement("DELETE FROM products WHERE category_id = ?");
                 // [SQL11-14] Xóa danh mục: xóa nhóm sau khi xóa được các sản phẩm.
                 // . Dấu ? từ trái sang phải: 1=mã danh mục.
                 // . Lỗi thì hủy cả lần xóa; controller kiểm tra điều kiện trước khi gọi hàm này.
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
    // . MenuController.doGet, index.jsp và footer.jsp gọi getAllActiveCategoriesWithCount trong
    //   CategoryDAO.java để đọc danh mục cho khách xem. Em lấy nhóm ACTIVE kèm số món đang bán của từng nhóm.
    // . Nếu MenuController gọi thì danh sách được đưa sang menu.jsp. Nếu index.jsp hoặc footer.jsp gọi thì danh
    //   sách được dùng ngay để dựng các mục danh mục trên trang đó.
    // ... Nếu MenuController.doGet gọi thì quay lại MenuController.java sau getAllActiveCategoriesWithCount để
    //   chuẩn bị menu.jsp; nếu index.jsp hoặc footer.jsp gọi thì quay đúng lời gọi này để dựng danh mục ngay
    //   trên trang.
    public List<Category> getAllActiveCategoriesWithCount() {

        List<Category> list = new ArrayList<>();
        // [SQL11-15] Trang chủ/Menu: đọc danh mục ACTIVE và số món đang bán của từng nhóm.
        // . Điều kiện món đang bán được đặt ở JOIN; nhóm ACTIVE chưa có món vẫn có thể được trả về.
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
    // . Hàm getCategoryById (đọc danh mục theo mã) trong CategoryDAO.java nhận mã từ phần sửa, xóa danh
    //   mục hoặc lưu sản phẩm. Em đọc danh mục cùng ảnh rồi trả Category về controller đang gọi để điền
    //   form, kiểm tra trước khi xóa hoặc gán sản phẩm vào nhóm.
    // ... Quay về đúng lời gọi getCategoryById. Nếu mở Sửa danh mục, xem form tại admin-categories.jsp,
    //     tìm [DMS03]. Nếu đang xóa, tiếp tục bước đếm đơn tại CategoryDAO.java, tìm [DMDH01]. Với form
    //     sản phẩm, quay về save sau getCategoryById để kiểm tra nhóm rồi gán vào Product.
    public Category getCategoryById(int id) {

        // [SQL11-16] Nút Sửa danh mục hoặc chọn danh mục cho món: đọc đúng một nhóm.
        // . Dấu ? từ trái sang phải: 1=mã danh mục.
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

        // [SQL11-17] Ảnh danh mục trên web: đọc dữ liệu ảnh và loại ảnh.
        // . Dấu ? từ trái sang phải: 1=mã danh mục.
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
