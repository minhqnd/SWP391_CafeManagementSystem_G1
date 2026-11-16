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

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

// BF11 - DANH MỤC: admin-categories.jsp -> GET xem/sửa, POST action=save/toggle.
@WebServlet(name = "AdminCategoriesController", urlPatterns = {"/admin/categories"})
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class AdminCategoriesController extends HttpServlet {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
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
            // BF11 - MỞ TRANG: lấy danh sách và thống kê, gán request để JSP hiển thị.
            // CA02: . nạp danh sách danh mục khi Admin mở trang hoặc bấm sửa.
            // .. Danh sách và danh mục đang chọn đã sẵn sàng để hiển thị.
            // ... CategoryDAO.getAllCategoriesWithCount() [CA03] → request → admin-categories.jsp [CA04].
            List<Category> categories = categoryDAO.getAllCategoriesWithCount();
            request.setAttribute("categories", categories);
            request.setAttribute("categoryTotal", categories.size());
            request.setAttribute("categoryActive", categories.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count());
            request.setAttribute("categoryInactive", categories.stream().filter(c -> "INACTIVE".equals(c.getStatus())).count());
            request.setAttribute("productTotal", categoryDAO.getProductCount());

            // Khi lưu lỗi, ưu tiên bản nháp trong session để form không bị mất dữ liệu vừa nhập.
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
                // Nút bút chì gửi editId bằng GET; selectedCategory điền dữ liệu cũ vào form bên phải.
                // CA02b: . Nhận editId từ nút Sửa [CA02a] khi không có bản nháp cần phục hồi.
                // .. Tìm thấy thì đặt selectedCategory; mã sai định dạng hoặc không tìm thấy thì không chọn danh mục.
                // ... CategoryDAO.getCategoryById() [CA02c] → form [CA04] → nút Lưu [CA09] → kiểm tra [CA11] → lưu [CA16].
                String editId = request.getParameter("editId");
                if (editId != null && !editId.isBlank()) {
                try {
                    Category selected = categoryDAO.getCategoryById(Integer.parseInt(editId));
                    if (selected != null) request.setAttribute("selectedCategory", selected);
                } catch (NumberFormatException ignored) {
                    // Ignore malformed selection and show a blank form.
                }
                }
            }
            request.getRequestDispatcher("/admin-categories.jsp").forward(request, response);
        } catch (RuntimeException e) {
            getServletContext().log("Failed to load admin categories", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không tải được danh mục. Hãy kiểm tra kết nối cơ sở dữ liệu.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) return;

        HttpSession session = request.getSession();
        String expected = (String) session.getAttribute("categoryCsrfToken");
        String submitted = request.getParameter("csrfToken");
        if (expected == null || !expected.equals(submitted)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên làm việc đã hết hạn. Tải lại trang rồi thử lại.");
            return;
        }

        // BF11 - NHẬN FORM: save thêm/sửa, toggle ẩn/hiện hoặc delete cả danh mục và sản phẩm liên quan.
        String action = request.getParameter("action");
        // CA10: . xem Admin vừa bấm Lưu hay nút ẩn, hiện sau khi kiểm tra quyền và mã bảo vệ.
        // .. Mỗi thao tác được chuyển sang đúng hàm xử lý.
        // ... action=save → save() [CA11]; action=toggle → toggle() [CA18]; action=delete → delete() [CA20].
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
            if ("delete".equals(action) && "23000".equals(e.getSQLState())) {
                session.setAttribute("categoryError", "Không thể xóa sản phẩm trong lịch sử \u0111ơn hàng; lịch sử \u0111ơn hàng \u0111ược giữ lại.");
            } else if ("23000".equals(e.getSQLState())) {
                session.setAttribute("categoryError", "Tên danh mục đã tồn tại hoặc danh mục đang được sản phẩm sử dụng.");
            } else {
                session.setAttribute("categoryError", "Không thể lưu thay đổi vào database. Vui lòng kiểm tra database.");
            }
        } catch (ClassNotFoundException | IllegalArgumentException e) {
            preserveDraftAfterSaveError(action, request, session);
            session.setAttribute("categoryError", e.getMessage() == null ? "Dữ liệu danh mục không hợp lệ." : e.getMessage());
        } catch (IllegalStateException e) {
            preserveDraftAfterSaveError(action, request, session);
            getServletContext().log("Category upload exceeded the configured limit", e);
            session.setAttribute("categoryError", "Ảnh danh mục không được lớn hơn 5 MB.");
        } catch (RuntimeException e) {
            preserveDraftAfterSaveError(action, request, session);
            getServletContext().log("Category operation failed", e);
            session.setAttribute("categoryError", "Không thể hoàn tất thao tác danh mục.");
        }
        String redirect = request.getContextPath() + "/admin/categories";
        if ("save".equals(action) && session.getAttribute("categoryDraft") != null) redirect += "?draft=1";
        response.sendRedirect(redirect);
    }

    private void preserveDraftAfterSaveError(String action, HttpServletRequest request, HttpSession session) {
        if (!"save".equals(action)) return;
        int id;
        try { id = Integer.parseInt(request.getParameter("id")); }
        catch (NumberFormatException e) { id = 0; }
        session.setAttribute("categoryDraft", new Category(id,
                request.getParameter("name"), request.getParameter("status"), null, 0));
    }

    private void save(HttpServletRequest request, HttpSession session)
            throws SQLException, ClassNotFoundException, ServletException, IOException {
        // BF11 - LƯU: id=0 tạo mới, id>0 cập nhật; form multipart gửi kèm ảnh dưới dạng Part.
        int id = parseId(request.getParameter("id"), true);
        // CA11: [SLBF11] Check category information
        // . kiểm tra tên, trạng thái, ảnh và tên danh mục có bị trùng không.
        // .. Mã bằng không thì thêm mới; có mã thì sửa danh mục đó.
        // ... CategoryDAO.saveCategory(danh mục, ảnh và thông tin tệp) [CA16] → tải lại [CA02].
        // CA12: . Đọc name từ [CA05], bỏ khoảng trắng đầu/cuối; kiểm tra bắt buộc và giới hạn tên.
        // ... categoryNameExists() [CA15] kiểm tra trùng trước khi gọi [CA16].
        String name = trimToNull(request.getParameter("name"));
        String status = request.getParameter("status");
        boolean removeImage = "true".equals(request.getParameter("removeImage"));
        // CA13: . Đọc imageFile và removeImage từ [CA06], [CA07].
        // .. Có ảnh mới thì kiểm tra dung lượng, MIME và chữ ký; không có ảnh mới thì giữ/xóa theo removeImage.
        // ... CategoryDAO.saveCategory() [CA16] lưu cùng thông tin danh mục.
        Part imagePart = request.getPart("imageFile");
        byte[] imageData = null;
        String imageType = null;
        String imageName = null;

        // Kiểm tra tên, ảnh tối đa 5 MB, loại MIME/chữ ký ảnh, trạng thái và tên trùng trước khi gọi DAO.
        if (name == null || name.length() > 100) throw new IllegalArgumentException("Tên danh mục bắt buộc và tối đa 100 ký tự.");
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
        // CA14: . Kiểm tra status từ [CA08]; chỉ nhận ACTIVE hoặc INACTIVE.
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        // CA15: . Kiểm tra tên trùng, loại trừ id đang sửa.
        // .. Sai thì trả thông báo; hợp lệ mới lưu tại [CA16].
        if (categoryDAO.categoryNameExists(name, id)) throw new IllegalArgumentException("Tên danh mục đã tồn tại.");

        Category category = new Category(id, name, status, null, 0);
        if (!categoryDAO.saveCategory(category, imageData, imageType, imageName, removeImage)) {
            throw new IllegalArgumentException("Không tìm thấy danh mục cần cập nhật.");
        }
        // CA17: . Lưu thành công thì bỏ bản nháp, đặt thông báo và redirect về danh sách [CA02].
        // .. Lưu lỗi được doPost() giữ bản nháp để sửa lại; không tự đi tiếp sang sản phẩm.
        session.removeAttribute("categoryDraft");
        session.setAttribute("categoryMessage", id > 0 ? "Đã cập nhật danh mục '" + name + "'." : "Đã tạo danh mục '" + name + "'.");
    }

    private void toggle(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {
        // CA18: . nhận danh mục và trạng thái Admin muốn chuyển sang.
        // .. chỉ đổi hiện hoặc ẩn, không xóa danh mục.
        // ... CategoryDAO.updateCategoryStatus(mã danh mục, trạng thái) [CA19] → tải lại [CA02].
        // BF11 - NÚT MẮT: đổi ACTIVE/INACTIVE, không xóa dòng danh mục hay lịch sử liên quan.
        int id = parseId(request.getParameter("id"), false);
        String status = request.getParameter("status");
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        if (!categoryDAO.updateCategoryStatus(id, status)) throw new IllegalArgumentException("Không tìm thấy danh mục.");
        session.setAttribute("categoryMessage", "Đã cập nhật trạng thái danh mục.");
    }

    // CA20: . Nhận nút xóa danh mục; kiểm tra id, tồn tại và sản phẩm có lịch sử đơn hàng.
    // ... CategoryDAO.deleteCategoryAndProducts() [CA21]; có lỗi thì giữ dữ liệu và trả thông báo.
    private void delete(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {
        int id = parseId(request.getParameter("id"), false);
        Category category = categoryDAO.getCategoryById(id);
        if (category == null) throw new IllegalArgumentException("Không tìm thấy danh mục.");
        if (categoryDAO.getOrderedProductCountByCategory(id) > 0) {
            throw new IllegalArgumentException("Không thể xóa vì danh mục có sản phẩm đã xuất hiện trong lịch sử đơn hàng.");
        }
        if (!categoryDAO.deleteCategoryAndProducts(id)) throw new IllegalArgumentException("Không thể xóa danh mục.");
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
        String trimmed = value.trim();
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
