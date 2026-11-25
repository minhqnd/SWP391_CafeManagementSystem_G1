package controller;

import dao.CategoryDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import model.Category;
import model.User;
import utils.AdminFormFeedback;
import utils.MenuValidation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "AdminCategoriesController", urlPatterns = {"/admin/categories"})
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class AdminCategoriesController extends HttpServlet {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private final CategoryDAO categoryDAO;

    public AdminCategoriesController() { this(new CategoryDAO()); }
    AdminCategoriesController(CategoryDAO categoryDAO) { this.categoryDAO = categoryDAO; }

    @Override
    // [DMM02]
    // . Hàm doGet trong AdminCategoriesController chuẩn bị trang quản lý danh mục. Em gọi CategoryDAO đọc
    //   danh sách và số món, tính các số tổng rồi đưa về trang. Nếu có chọn Sửa, em đọc thêm danh mục đang
    //   chọn để điền vào form.
    //
    // Tra tiếp: CategoryDAO.java / DMM03.
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;
        HttpSession session = request.getSession();
        if (session.getAttribute("categoryCsrfToken") == null) {
            session.setAttribute("categoryCsrfToken", UUID.randomUUID().toString());
        }
        moveFlash(session, request, "categoryMessage");
        moveFlash(session, request, "categoryError");
        try {

            List<Category> categories = categoryDAO.getAllCategoriesWithCount();
            request.setAttribute("categories", categories);

            // [SQL11-47] Thẻ Tổng/Hoạt động/Tạm ẩn danh mục: Java đếm từ danh sách categories đã đọc.

            request.setAttribute("categoryTotal", categories.size());
            request.setAttribute("categoryActive", categories.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count());
            request.setAttribute("categoryInactive", categories.stream().filter(c -> "INACTIVE".equals(c.getStatus())).count());
            request.setAttribute("productTotal", categoryDAO.getProductCount());

            Category draft = (Category) session.getAttribute("categoryDraft");
            if (draft != null) {

                session.removeAttribute("categoryDraft");
                if (draft.getId() > 0) {
                    Category existing = categoryDAO.getCategoryById(draft.getId());
                    if (existing != null) {
                        existing.setName(draft.getName());
                        existing.setStatus(draft.getStatus());
                        draft = existing;
                    }
                }
                request.setAttribute("selectedCategory", draft);
            } else {

                // [DMS02]
                // . Trong doGet của AdminCategoriesController, em lấy mã từ nút Sửa rồi gọi getCategoryById
                //   của CategoryDAO để đọc đúng danh mục. Thông tin đọc được dùng điền form; chưa có thao tác
                //   ghi cho đến khi người dùng bấm Lưu.
                //
                // Kết thúc: mở sửa khi thông tin danh mục được đưa vào form; bấm Lưu mới ghi dữ liệu.
                String editId = request.getParameter("editId");
                if (editId != null && !editId.isBlank()) {
                try {
                    Category selected = categoryDAO.getCategoryById(Integer.parseInt(editId));
                    if (selected != null) request.setAttribute("selectedCategory", selected);
                    else request.setAttribute("categoryError", "Không tìm thấy danh mục cần sửa. Danh mục có thể đã bị xóa.");
                } catch (NumberFormatException ignored) {
                    request.setAttribute("categoryError", "Mã danh mục cần sửa không hợp lệ.");
                }
                }
            }
            request.getRequestDispatcher("/admin-categories.jsp").forward(request, response);
        } catch (RuntimeException e) {
            getServletContext().log("Failed to load admin categories", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được danh mục lúc này. Vui lòng thử lại sau.");
        }
    }

    @Override
    // [DML03]
    // . Khi form từ nút Tạo hoặc Lưu danh mục được gửi tới, doPost của AdminCategoriesController nhận các giá
    //   trị vừa nhập. Em gọi save ngay trong class này để kiểm tra tên, trạng thái và ảnh trước khi lưu danh
    //   mục.
    //
    // Tra tiếp: DML04.
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        AdminFormFeedback.clear(session, "category");
        if (!AdminFormFeedback.readMultipart(request, session, "category")) {
            response.sendRedirect(request.getContextPath() + "/admin/categories");
            return;
        }
        String expected = (String) session.getAttribute("categoryCsrfToken");
        String submitted = request.getParameter("csrfToken");
        if (expected == null || !expected.equals(submitted)) {
            session.setAttribute("categoryError", "Không xác nhận được yêu cầu. Vui lòng thử lại trên trang vừa tải. Dữ liệu chưa được lưu.");
            response.sendRedirect(request.getContextPath() + "/admin/categories");
            return;
        }

        String action = request.getParameter("action");

        try {
            if ("save".equals(action)) {
                save(request, session);
            } else if ("toggle".equals(action)) {
                toggle(request, session);
            } else if ("delete".equals(action)) {
                delete(request, session);
            } else {
                session.setAttribute("categoryError", "Thao tác không hợp lệ.");
            }
        } catch (SQLException e) {
            getServletContext().log("Category database operation failed", e);
            preserveDraftAfterSaveError(action, request, session);
            session.setAttribute("categoryError", AdminFormFeedback.databaseError(e, action, "danh mục"));
        } catch (IllegalArgumentException e) {
            preserveDraftAfterSaveError(action, request, session);
            session.setAttribute("categoryError", e.getMessage() == null ? "Dữ liệu danh mục không hợp lệ." : e.getMessage());
        } catch (ClassNotFoundException | ServletException | IOException | RuntimeException e) {
            preserveDraftAfterSaveError(action, request, session);
            getServletContext().log("Category operation failed", e);
            session.setAttribute("categoryError", "Không thể hoàn tất thao tác danh mục lúc này. Vui lòng thử lại sau.");
        }
        String redirect = request.getContextPath() + "/admin/categories";
        if ("save".equals(action) && session.getAttribute("categoryDraft") != null) redirect += "?draft=1";
        response.sendRedirect(redirect);
    }

    // Giữ tên và trạng thái đang nhập để điền lại sau lỗi lưu danh mục.
    private void preserveDraftAfterSaveError(String action, HttpServletRequest request, HttpSession session) {
        if (!"save".equals(action)) return;
        int id;
        try { id = Integer.parseInt(request.getParameter("id")); }
        catch (NumberFormatException e) { id = 0; }
        session.setAttribute("categoryDraft", new Category(id,
                request.getParameter("name"), request.getParameter("status"), null, 0));
    }

    // [DML04]
    // . Trong save của AdminCategoriesController, em nhận tên, trạng thái và ảnh; có mã danh mục thì sửa,
    //   chưa có thì thêm mới. Sau khi kiểm tra thông tin và tên trùng, em gọi saveCategory trong CategoryDAO
    //   để ghi danh mục cùng lựa chọn ảnh.
    //
    // Tra tiếp: CategoryDAO.java / DML05.
    private void save(HttpServletRequest request, HttpSession session)
            throws SQLException, ClassNotFoundException, ServletException, IOException {

        int id = parseId(request.getParameter("id"), true);

        String name = trimToNull(request.getParameter("name"));
        String status = request.getParameter("status");
        boolean removeImage = "true".equals(request.getParameter("removeImage"));

        Part imagePart = request.getPart("imageFile");
        byte[] imageData = null;
        String imageType = null;
        String imageName = null;

        name = MenuValidation.requiredName(name, "Tên danh mục", 100);

        if (imagePart != null && imagePart.getSize() == 0
                && trimToNull(imagePart.getSubmittedFileName()) != null) {
            throw new IllegalArgumentException("Tệp ảnh đã chọn trống. Vui lòng chọn ảnh khác.");
        }
        if (imagePart != null && imagePart.getSize() > 0) {
            if (imagePart.getSize() > MAX_IMAGE_SIZE) {
                throw new IllegalArgumentException("Ảnh danh mục không được lớn hơn 5 MB.");
            }
            imageType = normalizeImageType(imagePart.getContentType());
            if (imageType == null) {
                throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG, PNG, GIF hoặc WebP.");
            }
            imageData = imagePart.getInputStream().readAllBytes();
            if (!matchesImageSignature(imageData, imageType)) {
                throw new IllegalArgumentException("Nội dung tệp không đúng định dạng ảnh đã chọn.");
            }
            imageName = trimToNull(imagePart.getSubmittedFileName());
            if (imageName != null && imageName.length() > 255) imageName = imageName.substring(imageName.length() - 255);
            removeImage = false;
        }

        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) throw new IllegalArgumentException("Vui lòng chọn trạng thái Hoạt động hoặc Tạm ẩn.");

        if (categoryDAO.categoryNameExists(name, id)) throw new IllegalArgumentException("Tên danh mục đã tồn tại.");

        Category category = new Category(id, name, status, null, 0);

        if (!categoryDAO.saveCategory(category, imageData, imageType, imageName, removeImage)) {
            throw new IllegalArgumentException("Không tìm thấy danh mục cần cập nhật.");
        }

        // [DML06]
        // . Khi saveCategory lưu thành công, em quay lại save trong AdminCategoriesController để bỏ bản
        //   nhập dở và đặt thông báo có tên danh mục. Sau đó doPost mở lại trang; doGet đọc danh sách
        //   hiện tại rồi đưa về admin-categories.jsp để Admin thấy kết quả vừa lưu.
        //
        // Tra tiếp: admin-categories.jsp / DML07.
        session.removeAttribute("categoryDraft");
        session.setAttribute("categoryMessage", id > 0 ? "Đã cập nhật danh mục '" + name + "'." : "Đã tạo danh mục '" + name + "'.");
    }

    // [DMAH02]
    // . Hàm toggle trong AdminCategoriesController kiểm tra danh mục và trạng thái cần đổi. Em gọi
    //   updateCategoryStatus của CategoryDAO để ghi thay đổi, rồi mới báo đã ẩn hoặc hiện danh mục.
    //
    // Tra tiếp: CategoryDAO.java / DMAH03.
    private void toggle(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {

        int id = parseId(request.getParameter("id"), false);
        String status = request.getParameter("status");
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        if (!categoryDAO.updateCategoryStatus(id, status)) throw new IllegalArgumentException("Không tìm thấy danh mục.");
        // [DMAH04]
        // . Khi trạng thái đã được ghi, em đặt thông báo rồi để doPost mở lại trang. Hàm doGet đọc danh sách
        //   mới để người dùng thấy đúng trạng thái vừa thay đổi.
        //
        // Kết thúc: đổi trạng thái khi danh sách mở lại kèm kết quả.
        session.setAttribute("categoryMessage", "Đã cập nhật trạng thái danh mục.");
    }

    // [DMX02]
    // . Hàm delete trong AdminCategoriesController kiểm tra danh mục và các món đã bán trước khi xóa. Nếu có
    //   món thuộc đơn hàng thì em chặn để giữ lịch sử. Đủ điều kiện em mới gọi deleteCategoryAndProducts
    //   trong CategoryDAO để xóa cả nhóm và các món thuộc nhóm.
    //
    // Tra tiếp: CategoryDAO.java / DMX03.
    private void delete(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {
        int id = parseId(request.getParameter("id"), false);
        Category category = categoryDAO.getCategoryById(id);
        if (category == null) throw new IllegalArgumentException("Không tìm thấy danh mục.");
        if (categoryDAO.getOrderedProductCountByCategory(id) > 0) {
            throw new IllegalArgumentException("Không thể xóa vì danh mục có sản phẩm đã xuất hiện trong lịch sử đơn hàng.");
        }
        if (!categoryDAO.deleteCategoryAndProducts(id)) throw new IllegalArgumentException("Không thể xóa danh mục.");
        // [DMX04]
        // . Khi deleteCategoryAndProducts xác nhận đã xóa, em quay lại AdminCategoriesController để đặt
        //   thông báo rồi cho doPost mở lại trang. doGet đọc danh sách hiện tại, nên danh mục vừa xóa
        //   cùng các món thuộc nhóm không còn xuất hiện trong danh sách quản lý.
        //
        // Kết thúc: xóa khi danh sách mở lại kèm kết quả.
        session.setAttribute("categoryMessage", "Đã xóa danh mục '" + category.getName() + "' và các sản phẩm thuộc danh mục.");
    }

    private int parseId(String value, boolean allowZero) {
        try {
            int id = Integer.parseInt(value == null ? (allowZero ? "0" : "") : value);
            if (id < (allowZero ? 0 : 1)) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mã danh mục không hợp lệ.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = utils.FormInput.trim(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeImageType(String contentType) {
        if (contentType == null) return null;
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/png", "image/gif", "image/webp" -> contentType.toLowerCase();
            default -> null;
        };
    }

    private boolean matchesImageSignature(byte[] data, String contentType) {
        if (data == null) return false;
        return switch (contentType) {
            case "image/jpeg" -> data.length >= 3 && (data[0] & 0xff) == 0xff
                    && (data[1] & 0xff) == 0xd8 && (data[2] & 0xff) == 0xff;
            case "image/png" -> data.length >= 8 && (data[0] & 0xff) == 0x89
                    && data[1] == 0x50 && data[2] == 0x4e && data[3] == 0x47
                    && data[4] == 0x0d && data[5] == 0x0a && data[6] == 0x1a && data[7] == 0x0a;
            case "image/gif" -> data.length >= 6 && data[0] == 'G' && data[1] == 'I'
                    && data[2] == 'F' && data[3] == '8' && (data[4] == '7' || data[4] == '9') && data[5] == 'a';
            case "image/webp" -> data.length >= 12 && data[0] == 'R' && data[1] == 'I'
                    && data[2] == 'F' && data[3] == 'F' && data[8] == 'W'
                    && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
            default -> false;
        };
    }

    private void moveFlash(HttpSession session, HttpServletRequest request, String name) {
        Object message = session.getAttribute(name);
        if (message != null) {
            request.setAttribute(name, message);
            session.removeAttribute(name);
        }
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()) || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ tài khoản Admin được quản lý danh mục.");
            return false;
        }
        return true;
    }
}
