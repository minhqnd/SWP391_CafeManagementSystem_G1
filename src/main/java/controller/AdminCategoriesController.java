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

    // [SUA11-06] Luyện sửa: Ảnh danh mục tối đa 2 MB thay vì 5 MB.
    // . Sửa MAX_IMAGE_SIZE, @MultipartConfig, kiểm tra trong admin-form-validation.js và lời nhắc ở
    //   admin-categories.jsp.
    // . AdminFormFeedback.readMultipart đang báo chung 5/6 MB cho cả hai trang; lời báo phải phù hợp nếu đặt
    //   hai giới hạn khác nhau.
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private final CategoryDAO categoryDAO;

    public AdminCategoriesController() { this(new CategoryDAO()); }
    AdminCategoriesController(CategoryDAO categoryDAO) { this.categoryDAO = categoryDAO; }

    @Override
    // [DMM02]
    // . Khi mở Quản lý danh mục, doGet trong AdminCategoriesController.java nhận yêu cầu. Em gọi
    //   getAllCategoriesWithCount (đọc danh mục kèm số sản phẩm) trong CategoryDAO.java để đọc danh mục kèm
    //   số sản phẩm, rồi đưa danh sách về admin-categories.jsp để hiển thị.
    // ... Đọc tiếp tại CategoryDAO.java, tìm [DMM03].
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
            // [DMTS02]
            // . Để hiện bốn thẻ thống kê, ở doGet trong AdminCategoriesController.java em đếm các danh mục vừa đọc
            //   và gọi CategoryDAO.getProductCount (đếm toàn bộ sản phẩm) để lấy tổng sản phẩm. Em đưa các số này về
            //   admin-categories.jsp qua categoryTotal, categoryActive, categoryInactive và productTotal.
            // ... Đọc tiếp tại admin-categories.jsp, tìm [DMM04].

            // [SQL11-47] Thẻ Tổng/Hoạt động/Tạm ẩn danh mục: Java đếm từ danh sách categories đã đọc.
            // . Tổng là categories.size; hoạt động và tạm ẩn đếm theo status. Nguồn SQL là
            //   getAllCategoriesWithCount.
            request.setAttribute("categoryTotal", categories.size());
            request.setAttribute("categoryActive", categories.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count());
            request.setAttribute("categoryInactive", categories.stream().filter(c -> "INACTIVE".equals(c.getStatus())).count());
            request.setAttribute("productTotal", categoryDAO.getProductCount());

            Category draft = (Category) session.getAttribute("categoryDraft");
            if (draft != null) {
                // . Nếu lần lưu danh mục trước bị lỗi, em lấy categoryDraft mà preserveDraftAfterSaveError (giữ form
                //   đang nhập khi lưu lỗi) đã giữ để doGet điền lại form trong admin-categories.jsp. Nhờ vậy người dùng
                //   sửa tiếp mà không phải nhập lại tên và trạng thái.
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
                // . Ở doGet trong AdminCategoriesController.java, em lấy editId của hàng vừa bấm Sửa. Em truyền mã này
                //   vào getCategoryById (đọc danh mục theo mã) trong CategoryDAO.java để đọc đúng danh mục, rồi đưa
                //   thông tin về form Sửa trong admin-categories.jsp.
                // ... Đọc tiếp tại CategoryDAO.java, tìm [DMDOC01].
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
    // [DML02]
    // . doPost trong AdminCategoriesController.java nhận action=save từ form Tạo hoặc Sửa danh mục. Em gọi save
    //   trong cùng file, truyền request và session để đọc các ô, kiểm tra rồi lưu danh mục.
    // ... Đọc tiếp tại AdminCategoriesController.java, tìm [DML03].
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

    // [DML08]
    // . Nếu lưu danh mục bị lỗi sau khi đọc được form, em gọi preserveDraftAfterSaveError để giữ mã, tên và trạng thái
    //   đang nhập vào categoryDraft. Khi trang mở lại, doGet đưa bản này về form để sửa tiếp;
    //   tệp ảnh mới không nằm trong bản giữ lại nên người dùng cần chọn lại nếu muốn gửi lại.
    // ... Quay lại nhánh catch của doPost trong AdminCategoriesController.java sau preserveDraftAfterSaveError;
    //     đặt categoryError rồi chuyển trang, doGet đưa categoryDraft về form để kết thúc lần lưu lỗi.
    private void preserveDraftAfterSaveError(String action, HttpServletRequest request, HttpSession session) {
        if (!"save".equals(action)) return;
        int id;
        try { id = Integer.parseInt(request.getParameter("id")); }
        catch (NumberFormatException e) { id = 0; }
        session.setAttribute("categoryDraft", new Category(id,
                request.getParameter("name"), request.getParameter("status"), null, 0));
    }

    // [DML03]
    // . Trong AdminCategoriesController.save, em đọc id, tên, trạng thái và ảnh từ form. id=0 là tạo danh mục
    //   mới; id lớn hơn 0 là sửa danh mục có mã đó. Em kiểm tra tên, tên trùng, trạng thái và ảnh trước khi gọi
    //   CategoryDAO.saveCategory.
    // ... Đọc tiếp tại AdminCategoriesController.java, tìm [DML04].
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

        // [DMT02]
        // . Trong hàm save (kiểm tra và lưu danh mục) của AdminCategoriesController.java, em nhận tên từ ô name
        //   rồi gọi requiredName trong MenuValidation.java để làm gọn và kiểm tra tên bắt buộc, tối đa 100 ký tự,
        //   không có ký tự không hỗ trợ. Đúng mới tìm tên trùng và lưu; sai thì doPost giữ tên, trạng thái
        //   và chuyển lời báo về phần thông báo trên form để người dùng sửa tiếp.
        // ... Đọc tiếp tại MenuValidation.java, tìm [DMT03].
        name = MenuValidation.requiredName(name, "Tên danh mục", 100);
        // [DMAN02]
        // . AdminCategoriesController.save nhận tệp imageFile từ form. Em kiểm tra dung lượng, gọi
        //   normalizeImageType để nhận loại ảnh rồi dùng matchesImageSignature để kiểm tra dữ liệu tệp có đúng
        //   loại đó không.
        // . Ảnh hợp lệ được chuyển cùng thông tin danh mục tới CategoryDAO.saveCategory để lưu. Em chỉ nhận
        //   JPG, PNG, GIF hoặc WebP tối đa 5 MB; sai thì báo lỗi và dừng lưu.
        // .. Trường hợp chưa đọc được cả form đã được AdminFormFeedback.readMultipart xử lý trước khi vào save.
        // ... Nếu cần xem cách nhận biểu mẫu trước khi vào save, đọc AdminFormFeedback.java, tìm [DMAN04].
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

        // [DMTT02]
        // . Trong hàm save (kiểm tra và lưu danh mục) của AdminCategoriesController.java, em lấy status từ ô
        //   Trạng thái. Em chỉ chấp nhận ACTIVE hoặc INACTIVE vì đây là hai trạng thái danh mục đang dùng; giá
        //   trị khác sẽ bị báo lỗi trước khi lưu.
        // ... Trong AdminCategoriesController.java, tiếp tục save ngay dưới bước kiểm tra status; tạo Category rồi
        //     gọi categoryDAO.saveCategory nếu các phần đều hợp lệ.
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) throw new IllegalArgumentException("Vui lòng chọn trạng thái Hoạt động hoặc Tạm ẩn.");

        // [DMTR01]
        // . Để tránh trùng tên danh mục, ở hàm save (kiểm tra và lưu danh mục) trong
        //   AdminCategoriesController.java em gọi categoryNameExists (tìm danh mục khác trùng tên) trong
        //   CategoryDAO.java. Em truyền tên và mã danh mục đang sửa để tìm danh mục khác cùng tên, không tính
        //   chính danh mục đó là trùng.
        // ... Đọc tiếp tại CategoryDAO.java, tìm [DMTR02].
        if (categoryDAO.categoryNameExists(name, id)) throw new IllegalArgumentException("Tên danh mục đã tồn tại.");

        Category category = new Category(id, name, status, null, 0);
        // [DML04]
        // . Kiểm tra xong, em gọi saveCategory trong CategoryDAO.java để ghi danh mục cùng ảnh. Em truyền thông
        //   tin danh mục, dữ liệu ảnh và lựa chọn xóa ảnh, rồi nhận kết quả để biết lưu được hay cần báo lỗi.
        // ... Đọc tiếp tại CategoryDAO.java, tìm [DML05].
        if (!categoryDAO.saveCategory(category, imageData, imageType, imageName, removeImage)) {
            throw new IllegalArgumentException("Không tìm thấy danh mục cần cập nhật.");
        }

        // [DML06]
        // . Khi CategoryDAO.saveCategory trả thành công, em quay lại save trong AdminCategoriesController.java
        //   để xóa bản nhập dở và đặt categoryMessage. Sau đó doPost mở lại trang để doGet đọc danh sách mới,
        //   đưa về admin-categories.jsp hiển thị kết quả.
        // ... Đọc tiếp tại admin-categories.jsp, tìm [DML07].
        session.removeAttribute("categoryDraft");
        session.setAttribute("categoryMessage", id > 0 ? "Đã cập nhật danh mục '" + name + "'." : "Đã tạo danh mục '" + name + "'.");
    }

    // [DMAH02]
    // . Khi doPost gọi toggle (ẩn hoặc hiện danh mục) trong AdminCategoriesController.java, em lấy mã danh
    //   mục và trạng thái mới từ form. Em truyền hai giá trị vào updateCategoryStatus (lưu trạng thái
    //   ẩn/hiện danh mục) trong CategoryDAO.java để đổi đúng danh mục vừa bấm, rồi dùng kết quả để thông báo
    //   trên trang.
    // ... Đọc tiếp tại CategoryDAO.java, tìm [DMAH03].
    private void toggle(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {

        int id = parseId(request.getParameter("id"), false);
        String status = request.getParameter("status");
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        if (!categoryDAO.updateCategoryStatus(id, status)) throw new IllegalArgumentException("Không tìm thấy danh mục.");
        session.setAttribute("categoryMessage", "Đã cập nhật trạng thái danh mục.");
    }

    // [DMX02]
    // . Trong AdminCategoriesController.delete, em nhận mã danh mục cần xóa. Em gọi CategoryDAO.getCategoryById
    //   để đọc đúng nhóm, rồi getOrderedProductCountByCategory để đếm món của nhóm đã xuất hiện trong đơn hàng.
    // . Có món nằm trong đơn thì báo lỗi và dừng. Đủ điều kiện mới gọi CategoryDAO.deleteCategoryAndProducts để
    //   xóa danh mục cùng các món thuộc nhóm.
    // ... Đọc tiếp tại CategoryDAO.java, tìm [DMDH01].
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

    // [DMQ01]
    // . Trước khi mở hoặc xử lý danh mục, doGet và doPost gọi isAdmin (kiểm tra tài khoản Admin còn hoạt
    //   động) trong AdminCategoriesController.java. Hàm kiểm tra tài khoản đang dùng có phải Admin còn hoạt
    //   động không; đúng thì tiếp tục, sai thì trả lỗi và dừng yêu cầu.
    // ... Quay lại đầu doGet hoặc doPost trong AdminCategoriesController.java sau isAdmin; đúng thì đọc hoặc xử lý
    //     danh mục, sai thì đã trả lỗi và kết thúc yêu cầu.
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
