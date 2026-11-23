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
    //   AdminCategoriesController.java. Em đếm sản phẩm thuộc danh mục đã xuất hiện trong order_items rồi
    //   trả số về delete, để chặn xóa nếu có lịch sử đơn hàng.
    // ... Quay lại AdminCategoriesController.java, trong delete sau getOrderedProductCountByCategory; có món đã
    //     bán thì báo lỗi, đủ điều kiện mới gọi deleteCategoryAndProducts trong CategoryDAO.java.
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
    // . Ở categoryNameExists (tìm danh mục khác trùng tên) trong CategoryDAO.java, em nhận tên và mã danh
    //   mục đang sửa từ AdminCategoriesController.save (kiểm tra và lưu danh mục). Em tìm tên trong
    //   categories và bỏ qua mã hiện tại, rồi trả đúng hoặc sai để hàm save biết có bị trùng với danh mục
    //   khác không.
    // ... Quay lại AdminCategoriesController.java, trong save ngay sau categoryNameExists; tên trùng thì báo lỗi,
    //     tên hợp lệ mới tiếp tục kiểm tra và gọi saveCategory.
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
        // . Khi tạo danh mục mới, em dùng INSERT trong CategoryDAO.saveCategory (ghi danh mục) rồi lấy mã được
        //   tạo bằng getGeneratedKeys (lấy mã của dòng vừa thêm). Em đặt mã vào category.id để phần lưu ảnh biết
        //   ảnh thuộc danh mục nào.
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
    // . Ở updateCategoryStatus (lưu trạng thái ẩn/hiện danh mục) trong CategoryDAO.java, em nhận mã và trạng
    //   thái từ AdminCategoriesController.toggle (ẩn hoặc hiện danh mục). Em sửa categories.status theo đúng
    //   mã rồi trả kết quả về toggle để controller thông báo và mở lại danh sách đã cập nhật.
    // ... Quay lại AdminCategoriesController.java, trong toggle sau updateCategoryStatus; đặt categoryMessage hoặc
    //     báo lỗi rồi doPost mở lại admin-categories.jsp để hiện kết quả.
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
    //   nhận mã danh mục từ AdminCategoriesController.delete (kiểm tra điều kiện rồi xóa). Em xóa các sản
    //   phẩm thuộc nhóm rồi xóa danh mục, hoàn tất cả hai mới xác nhận. Nếu gặp lỗi thì hủy thay đổi và báo
    //   kết quả về hàm delete.
    // ... Quay lại AdminCategoriesController.java, trong delete sau deleteCategoryAndProducts; đặt kết quả xóa rồi
    //     doPost mở lại danh sách trong admin-categories.jsp và hiện thông báo.
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
    // . Hàm getAllActiveCategoriesWithCount (đọc danh mục hoạt động kèm số món đang bán) trong
    //   CategoryDAO.java được MenuController.doGet và index.jsp gọi để đọc danh mục cho khách xem. Em chỉ
    //   lấy nhóm ACTIVE kèm số món đang bán, rồi trả danh sách về nơi gọi để dựng Menu hoặc Trang chủ.
    // ... Nếu MenuController.doGet gọi thì quay lại MenuController.java sau getAllActiveCategoriesWithCount để
    //     chuẩn bị menu.jsp; nếu index.jsp hoặc footer.jsp gọi thì quay đúng lời gọi này để dựng danh mục ngay
    //     trên trang.
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
    // . Hàm getCategoryById (đọc danh mục theo mã) trong CategoryDAO.java nhận mã từ phần sửa, xóa danh mục
    //   hoặc lưu sản phẩm. Em đọc danh mục cùng ảnh rồi trả Category về controller đang gọi để điền form,
    //   kiểm tra trước khi xóa hoặc gán sản phẩm vào nhóm.
    // ... Quay lại lời gọi getCategoryById: trong AdminCategoriesController.java, doGet điền form Sửa hoặc delete
    //     kiểm tra nhóm cần xóa; trong AdminProductsController.java, save kiểm tra danh mục trước khi gán món.
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
