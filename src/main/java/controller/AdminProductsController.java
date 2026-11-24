package controller;

import dao.CategoryDAO;
import dao.ProductDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import model.Category;
import model.Product;
import model.User;
import utils.AdminFormFeedback;
import utils.MenuValidation;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@WebServlet(name = "AdminProductsController", urlPatterns = {"/admin/products"})
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class AdminProductsController extends HttpServlet {
    // [SUA11-05] Luyện sửa: Ảnh sản phẩm tối đa 2 MB thay vì 5 MB.
    // . Sửa MAX_IMAGE_SIZE và maxFileSize/maxRequestSize trong @MultipartConfig ở đầu file; giới hạn cả form
    //   phải đủ chứa ảnh và các ô.
    // . Sửa kiểm tra ảnh trong admin-form-validation.js, lời nhắc ở admin-products.jsp và lời báo dung lượng
    //   trong AdminFormFeedback.readMultipart.
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    // [SUA11-03] Luyện sửa: Một sản phẩm chỉ được tối đa 5 size thay vì 50.
    // . Đổi MAX_SIZES và giới hạn dòng trong admin-form-validation.js; kiểm tra lời báo đi cùng.
    // . Thử lưu đúng 5 và 6 dòng. Quy định ít nhất một size và một size phụ thu 0 vẫn được kiểm tra riêng.
    private static final int MAX_SIZES = 50;
    // [SUA11-04] Luyện sửa: Một sản phẩm chỉ được tối đa 5 topping thay vì 50.
    // . Đổi MAX_TOPPINGS và giới hạn dòng trong admin-form-validation.js; size và topping đang dùng chung một
    //   bước kiểm tra trên form.
    // . Nếu đổi riêng topping thì tách giới hạn theo loại, giữ topping không bắt buộc nếu đề không yêu cầu đổi
    //   quy định đó.
    private static final int MAX_TOPPINGS = 50;
    private final ProductDAO productDAO;
    private final CategoryDAO categoryDAO;

    public AdminProductsController() { this(new ProductDAO(), new CategoryDAO()); }
    AdminProductsController(ProductDAO productDAO, CategoryDAO categoryDAO) {
        this.productDAO = productDAO;
        this.categoryDAO = categoryDAO;
    }

    private static final class ProductDraft {
        private final int id;
        private final int categoryId;
        private final String name;
        private final String basePrice;
        private final String description;
        private final String variantsConfig;

        private ProductDraft(int id, int categoryId, String name, String basePrice,
                             String description, String variantsConfig) {
            this.id = id;
            this.categoryId = categoryId;
            this.name = name;
            this.basePrice = basePrice;
            this.description = description;
            this.variantsConfig = variantsConfig;
        }
    }

    @Override
    // [SPM02]
    // . Khi mở Quản lý sản phẩm, doGet trong AdminProductsController.java nhận danh mục và từ khóa đang lọc.
    //   Em truyền hai giá trị vào getProductsForAdmin (đọc danh sách sản phẩm để quản lý) trong
    //   ProductDAO.java để lấy danh sách phù hợp, rồi đưa về admin-products.jsp để hiển thị.
    // ... Đọc tiếp tại ProductDAO.java, tìm [SPM03].
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAdmin(request, response)) return;
        HttpSession session = request.getSession();
        if (session.getAttribute("productCsrfToken") == null) session.setAttribute("productCsrfToken", UUID.randomUUID().toString());
        moveFlash(session, request, "productMessage");
        moveFlash(session, request, "productError");
        List<Category> categories = new ArrayList<>();
        try {
            categories = categoryDAO.getAllCategoriesWithCount();

            Integer categoryId = parseOptionalId(request.getParameter("categoryId"));
            String keyword = request.getParameter("q");

            List<Product> products = productDAO.getProductsForAdmin(categoryId, keyword);
            int[] stats = productDAO.getProductAdminStats();
            request.setAttribute("categories", categories);
            request.setAttribute("products", products);
            // [SPTS02]
            // . Để hiện ba thẻ thống kê, ở doGet trong AdminProductsController.java em gọi
            //   ProductDAO.getProductAdminStats (đọc tổng món và số đang bán) để lấy tổng món và số đang bán. Em
            //   tính số tạm ngừng từ hai số đó, rồi đưa productTotal, productAvailable và productPaused về
            //   admin-products.jsp.
            // ... Đọc tiếp tại admin-products.jsp, tìm [SPM04].

            request.setAttribute("productTotal", stats[0]);
            request.setAttribute("productAvailable", stats[1]);
            // [SQL11-48] Thẻ Tạm ngừng sản phẩm: Java lấy tổng món trừ số đang bán.
            // . Hai số được getProductAdminStats trả về; không đếm từ các hàng đang thấy sau khi lọc.
            request.setAttribute("productPaused", stats[0] - stats[1]);
            request.setAttribute("selectedCategoryId", categoryId == null ? 0 : categoryId);
            request.setAttribute("searchKeyword", keyword == null ? "" : keyword);

            ProductDraft draft = (ProductDraft) session.getAttribute("productDraft");
            if (draft != null) {
                // . Nếu lần lưu sản phẩm trước bị lỗi, em lấy productDraft mà preserveDraftAfterSaveError (giữ form đang
                //   nhập khi lưu lỗi) đã giữ để doGet điền lại thông tin, size và topping vào admin-products.jsp. Người
                //   dùng có thể sửa tiếp thay vì nhập lại từ đầu.
                session.removeAttribute("productDraft");
                Product selected = draft.id > 0 ? productDAO.getProductForAdmin(draft.id) : new Product();
                if (selected == null) selected = new Product();
                selected.setId(draft.id);
                selected.setCategoryId(draft.categoryId);
                selected.setName(draft.name);
                selected.setDescription(draft.description);
                selected.setVariantsConfig(draft.variantsConfig);
                request.setAttribute("selectedProduct", selected);
                request.setAttribute("productDraftBasePrice", draft.basePrice);
                request.setAttribute("hasProductDraft", true);
            } else {

                // [SPS02]
                // . Ở doGet trong AdminProductsController.java, em lấy editId từ nút Sửa rồi gọi getProductForAdmin
                //   (đọc một sản phẩm để quản lý) trong ProductDAO.java bằng mã đó. Hàm trả về sản phẩm để em điền
                //   tên, giá, danh mục, size và topping vào form Sửa trong admin-products.jsp.
                // ... doGet gọi ProductDAO.getProductForAdmin để đọc món trước khi điền form. Đọc hàm đó tại
                //     ProductDAO.java, tìm mã CTCH03; kết quả quay về doGet rồi mới mở form Sửa.
                String editId = request.getParameter("editId");
                if (editId != null && !editId.isBlank()) {
                try {
                    Product selected = productDAO.getProductForAdmin(Integer.parseInt(editId));
                    if (selected != null) request.setAttribute("selectedProduct", selected);
                    else request.setAttribute("productError", "Không tìm thấy sản phẩm cần sửa. Sản phẩm có thể đã bị xóa.");
                } catch (NumberFormatException ignored) {
                    request.setAttribute("productError", "Mã sản phẩm cần sửa không hợp lệ.");
                }
                }
            }
            request.getRequestDispatcher("/admin-products.jsp").forward(request, response);
        } catch (SQLException | ClassNotFoundException | RuntimeException e) {
            getServletContext().log("Failed to load admin products", e);
            request.setAttribute("categories", categories);
            request.setAttribute("products", Collections.emptyList());
            request.setAttribute("productTotal", 0);
            request.setAttribute("productAvailable", 0);
            request.setAttribute("productPaused", 0);
            request.setAttribute("selectedCategoryId", 0);
            request.setAttribute("searchKeyword", "");
            request.setAttribute("productError", "Không tải được danh sách sản phẩm lúc này. Vui lòng thử lại sau.");
            request.getRequestDispatcher("/admin-products.jsp").forward(request, response);
        }
    }

    @Override
    // [SPL03]
    // . doPost trong AdminProductsController.java nhận action=save từ nút Thêm sản phẩm hoặc Lưu thay đổi. Em
    //   gọi save trong cùng file, truyền request và session để đọc thông tin sản phẩm, kiểm tra rồi lưu.
    // ... Đọc tiếp tại AdminProductsController.java, tìm [SPL04].
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) return;
        HttpSession session = request.getSession();
        AdminFormFeedback.clear(session, "product");
        if (!AdminFormFeedback.readMultipart(request, session, "product")) {
            response.sendRedirect(request.getContextPath() + "/admin/products");
            return;
        }
        String expected = (String) session.getAttribute("productCsrfToken");
        if (expected == null || !expected.equals(request.getParameter("csrfToken"))) {
            session.setAttribute("productError", "Không xác nhận được yêu cầu. Vui lòng thử lại trên trang vừa tải. Dữ liệu chưa được lưu.");
            response.sendRedirect(request.getContextPath() + "/admin/products");
            return;
        }
        String action = request.getParameter("action");
        try {

            if ("save".equals(action)) save(request, session);
            else if ("toggle".equals(action)) toggle(request, session);
            else if ("delete".equals(action)) delete(request, session);
            else session.setAttribute("productError", "Thao tác không hợp lệ.");
        } catch (SQLException e) {
            getServletContext().log("Product database operation failed", e);
            preserveDraftAfterSaveError(action, request, session);
            session.setAttribute("productError", AdminFormFeedback.databaseError(e, action, "sản phẩm"));
        } catch (IllegalArgumentException e) {
            preserveDraftAfterSaveError(action, request, session);
            session.setAttribute("productError", e.getMessage() == null ? "Dữ liệu sản phẩm không hợp lệ." : e.getMessage());
        } catch (ClassNotFoundException | ServletException | IOException | RuntimeException e) {
            preserveDraftAfterSaveError(action, request, session);
            getServletContext().log("Product operation failed", e);
            session.setAttribute("productError", "Không thể hoàn tất thao tác sản phẩm.");
        }
        String redirect = request.getContextPath() + "/admin/products";
        if ("save".equals(action) && session.getAttribute("productDraft") != null) redirect += "?draft=1#productEditor";
        response.sendRedirect(redirect);
    }

    // [SPL09]
    // . Khi lưu lỗi sau khi đọc được form, doPost gọi preserveDraftAfterSaveError để giữ mã món, danh
    //   mục, tên, giá, mô tả và từng dòng size/topping vào productDraft.
    // . doGet lấy bản này điền lại form để người dùng sửa tiếp. Đây chỉ là dữ liệu nhập dở, chưa lưu vào
    //   products; ảnh mới cần chọn lại.
    // ... Quay về catch của doPost sau preserveDraftAfterSaveError. Controller đặt lỗi và chuyển trang;
    //     doGet đưa bản nhập dở về form. Xem kết quả hiển thị tại admin-products.jsp, tìm mã SPL08.
    private void preserveDraftAfterSaveError(String action, HttpServletRequest request, HttpSession session) {
        if (!"save".equals(action)) return;
        int id = parseDraftInt(request.getParameter("id"));
        int categoryId = parseDraftInt(request.getParameter("categoryId"));
        session.setAttribute("productDraft", new ProductDraft(id, categoryId,
                request.getParameter("name"), request.getParameter("basePrice"),
                request.getParameter("description"), buildDraftVariantsConfig(request)));
    }

    private int parseDraftInt(String value) {
        try { return Math.max(0, Integer.parseInt(value == null ? "0" : value)); }
        catch (NumberFormatException e) { return 0; }
    }

    // . Khi giữ form sau lỗi, em gọi buildDraftVariantsConfig để dựng lại các dòng size và topping
    //   từ tên, giá vừa gửi. Em giữ riêng từng dòng, kể cả tên trùng hoặc giá sai, để người dùng sửa đúng
    //   nội dung đã nhập. Bản này chỉ để hiện lại form, chưa được ghi vào sản phẩm.
    private String buildDraftVariantsConfig(HttpServletRequest request) {
        return "{\"draftSizes\":" + draftRows(request, "sizeName", "sizeDelta")
                + ",\"draftToppings\":" + draftRows(request, "toppingName", "toppingPrice") + "}";
    }

    private String draftRows(HttpServletRequest request, String nameField, String amountField) {
        // . Em ghép lại từng cặp tên và giá để hiện đúng dòng vừa nhập, kể cả tên trống, tên trùng hoặc giá
        //   sai. Bản giữ có tối đa 101 dòng để tránh giữ một danh sách gửi quá lớn; dữ liệu này chưa được lưu.
        String[] names = request.getParameterValues(nameField);
        String[] amounts = request.getParameterValues(amountField);
        int count = Math.min(101, Math.max(names == null ? 0 : names.length, amounts == null ? 0 : amounts.length));
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String name = names != null && i < names.length && names[i] != null ? names[i] : "";
            String amount = amounts != null && i < amounts.length && amounts[i] != null ? amounts[i] : "";
            rows.add("{\"name\":" + jsonString(name) + ",\"amount\":" + jsonString(amount) + "}");
        }
        return "[" + String.join(",", rows) + "]";
    }

    // [SPL04]
    // . Trong AdminProductsController.save, em đọc id cùng thông tin món, ảnh, size và topping từ form.
    //   id=0 là thêm mới; id lớn hơn 0 là sửa sản phẩm có mã đó.
    // . Em kiểm tra các ô rồi đặt dữ liệu vào Product, gồm cả id để DAO chọn thêm hay sửa. Món mới được
    //   đặt đang bán; món đang sửa giữ trạng thái hiện tại.
    // ... Kiểm tra các ô xong, save gọi buildVariantsConfig để ghép size và topping. Đọc tại
    //     AdminProductsController.java, tìm [SPBT02]; hàm trả cấu hình về Product trước khi gọi ghi sản
    //     phẩm.
    private void save(HttpServletRequest request, HttpSession session)
            throws SQLException, ClassNotFoundException, ServletException, IOException {

        int id = parseId(request.getParameter("id"), true);

        String rawCategoryId = request.getParameter("categoryId");
        if (rawCategoryId == null || rawCategoryId.isBlank()) throw new IllegalArgumentException("Vui lòng chọn danh mục.");
        int categoryId = parseId(rawCategoryId, false);

        String name = trimToNull(request.getParameter("name"));

        String description = trimToNull(request.getParameter("description"));
        boolean removeImage = "true".equals(request.getParameter("removeImage"));

        Part imagePart = request.getPart("imageFile");
        byte[] imageData = null;
        String imageType = null;
        String imageName = null;
        String recipeInstructions = null;
        boolean available = true;
        // [SPT02]
        // . Trong save (kiểm tra và lưu sản phẩm) của AdminProductsController.java, em nhận tên món từ name.
        //   Em kiểm tra tên không trống, không quá 150 ký tự và không chứa ký tự điều khiển bằng
        //   MenuValidation.requiredName (kiểm tra tên bắt buộc, độ dài và ký tự). Tên đúng mới được dùng để
        //   tìm trùng trước khi lưu.
        // ... save nhận tên xong thì kiểm tra tên trùng. Đọc điểm gọi productNameExists tại
        //     AdminProductsController.java, tìm [SPTR01]; tên đúng mới tiếp tục tạo Product.
        name = MenuValidation.requiredName(name, "Tên món", 150);
        // [SPMT02]
        // . Trong save (kiểm tra và lưu sản phẩm) của AdminProductsController.java, em nhận mô tả từ
        //   description và kiểm tra tối đa 5.000 ký tự. Nếu quá dài thì báo lỗi về doPost để giữ lại form
        //   đang nhập; nếu hợp lệ thì đưa mô tả vào sản phẩm để lưu.
        // ... Mô tả hợp lệ thì save gán vào Product rồi tiếp tục các phần còn lại. Đọc điểm ghi sản phẩm tại
        //     AdminProductsController.java, tìm [SPL05]; mô tả sai thì doPost báo lỗi, chưa ghi.
        if (description != null && description.length() > 5000) {
            throw new IllegalArgumentException("Mô tả không được dài quá 5000 ký tự.");
        }
        // [SPAN02]
        // . AdminProductsController.save nhận tệp imageFile và lựa chọn removeImage từ form. Em kiểm tra
        //   dung lượng, gọi normalizeImageType để nhận loại ảnh và matchesImageSignature để kiểm tra dữ liệu
        //   tệp có đúng loại đó không.
        // . Tệp hợp lệ được chuyển tới ProductDAO.saveProduct để lưu cùng món. Ảnh chỉ nhận JPG, PNG, GIF
        //   hoặc WebP tối đa 5 MB; sai thì báo lỗi để người dùng sửa form.
        // ... Kiểm tra ảnh xong, save tiếp tục kiểm tra các ô và ghép Product. Đọc điểm gọi ghi món cùng ảnh
        //     tại AdminProductsController.java, tìm mã SPL05; ảnh sai thì dừng và doPost báo lỗi, chưa ghi.
        if (imagePart != null && imagePart.getSize() == 0
                && trimToNull(imagePart.getSubmittedFileName()) != null) {
            throw new IllegalArgumentException("Tệp ảnh đã chọn trống. Vui lòng chọn ảnh khác.");
        }
        if (imagePart != null && imagePart.getSize() > 0) {
            if (imagePart.getSize() > MAX_IMAGE_SIZE) {
                throw new IllegalArgumentException("Ảnh sản phẩm không được lớn hơn 5 MB.");
            }
            imageType = normalizeImageType(imagePart.getContentType());
            if (imageType == null) throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG, PNG, GIF hoặc WebP.");
            imageData = imagePart.getInputStream().readAllBytes();
            if (!matchesImageSignature(imageData, imageType)) {
                throw new IllegalArgumentException("Nội dung tệp không đúng định dạng ảnh đã chọn.");
            }
            imageName = trimToNull(imagePart.getSubmittedFileName());
            if (imageName != null && imageName.length() > 255) imageName = imageName.substring(imageName.length() - 255);
            removeImage = false;
        }

        // [SPDM02]
        // . Để biết món được gán vào danh mục hợp lệ, ở save (kiểm tra và lưu sản phẩm) trong
        //   AdminProductsController.java em truyền categoryId vào getCategoryById (đọc danh mục theo mã)
        //   trong CategoryDAO.java. Em chỉ tiếp tục lưu khi danh mục còn tồn tại và đang hoạt động; trường
        //   hợp khác sẽ báo lỗi.
        // ... Danh mục hợp lệ thì save gán categoryId vào Product, tiếp tục ghép các ô và size/topping. Đọc
        //     điểm ghi sau đó tại AdminProductsController.java, tìm mã SPL05; danh mục không hợp lệ thì trả
        //     form lỗi.
        Category category = categoryDAO.getCategoryById(categoryId);
        if (category == null) throw new IllegalArgumentException("Hãy chọn một danh mục đang tồn tại.");
        if (!"ACTIVE".equalsIgnoreCase(category.getStatus())) {
            throw new IllegalArgumentException("Chỉ được gán sản phẩm vào danh mục đang hoạt động.");
        }
        if (id > 0) {
            Product existing = productDAO.getProductForAdmin(id);
            if (existing == null) throw new IllegalArgumentException("Không tìm thấy sản phẩm cần cập nhật.");
            recipeInstructions = existing.getRecipeInstructions();
            available = existing.isAvailable();
        }

        // [SPTR01]
        // . Để tránh trùng tên món, ở save (kiểm tra và lưu sản phẩm) trong AdminProductsController.java em gọi
        //   productNameExists (tìm sản phẩm khác trùng tên) trong ProductDAO.java. Em truyền tên và mã sản phẩm
        //   đang sửa để tìm món khác cùng tên. Nếu có thì dừng lưu và báo lỗi tên sản phẩm.
        // ... Đọc tiếp tại ProductDAO.java, tìm [SPTR02].
        if (productDAO.productNameExists(name, id)) {
            throw new IllegalArgumentException("Tên sản phẩm đã tồn tại trong hệ thống.");
        }

        // [SPG02]
    // . Trong save của AdminProductsController.java, em nhận basePrice rồi gọi money trong
    //   MenuValidation.java để kiểm tra giá. Em truyền cả tên ô để nếu sai thì báo đúng Giá bán cơ bản;
    //   số không âm, tối đa hai số thập phân và 9.999.999.999,99 mới được đưa vào sản phẩm để lưu.
    // ... Đọc tiếp tại MenuValidation.java, tìm [SPG03].

        // [SUA11-02] Luyện sửa: Nhập giá sản phẩm 10000đ, khi lưu cộng thêm 20000đ.
        // . Đây là giá cơ bản được gán vào Product và lưu database; sửa ở đây sẽ ảnh hưởng các nơi đọc giá món.
        // . Nếu cộng thêm tiền thì phải kiểm tra cả giá sau cộng. Yêu cầu chỉ tăng giá trong giỏ phải xử lý ở
        //   CartService, không đổi giá lưu ở đây.
        double basePrice = MenuValidation.money(request.getParameter("basePrice"), "Giá bán cơ bản").doubleValue();

        Product product = new Product();
        product.setId(id);
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setBasePrice(basePrice);
        product.setDescription(description);
        product.setImageUrl(null);

        product.setAvailable(available);

        product.setVariantsConfig(buildVariantsConfig(request, recipeInstructions));
        // [SPL05]
        // . Kiểm tra xong, em gọi saveProduct trong ProductDAO.java, truyền Product, dữ liệu ảnh và lựa chọn
        //   xóa ảnh. Hàm này lưu thông tin món cùng ảnh, rồi trả kết quả về AdminProductsController.save để báo
        //   đã lưu hay chưa lưu được.
        // ... Đọc tiếp tại ProductDAO.java, tìm [SPL06].
        if (!productDAO.saveProduct(product, imageData, imageType, imageName, removeImage)) {
            throw new IllegalArgumentException("Không tìm thấy sản phẩm cần cập nhật.");
        }

        // [SPL07]
        // . Khi ProductDAO.saveProduct trả thành công, AdminProductsController.save xóa bản nhập dở và đặt
        //   productMessage. doPost mở lại trang quản lý; doGet đọc danh sách mới rồi chuyển sang
        //   admin-products.jsp để hiện thông báo và sản phẩm vừa lưu.
        // ... Đọc tiếp tại admin-products.jsp, tìm [SPL08].
        session.removeAttribute("productDraft");
        session.setAttribute("productMessage", id > 0 ? "Đã cập nhật món '" + name + "'." : "Đã thêm món '" + name + "'.");
    }

    // [SPBT02]
    // . Trong save (kiểm tra và lưu sản phẩm) của AdminProductsController.java, em gọi
    //   buildVariantsConfig (kiểm tra và ghép size, topping để lưu) để xử lý size và topping. Hàm đọc
    //   từng cặp tên, giá từ sizeName/sizeDelta và toppingName/toppingPrice, kiểm tra rồi ghép thành
    //   thông tin lựa chọn để lưu cùng sản phẩm.
    // .. Em yêu cầu có ít nhất một size và một size phụ thu 0 để có lựa chọn theo giá cơ bản. Trong mỗi
    //    loại, tên không được trùng, giá không âm và tối đa 50 dòng; topping được phép không thêm.
    // ... Xét hết size và topping, buildVariantsConfig trả cấu hình về save ở product.setVariantsConfig.
    //     Đọc điểm gọi DAO sau đó tại AdminProductsController.java, tìm mã SPL05. Có dòng sai thì báo
    //     lỗi về doPost, chưa ghi sản phẩm.
    private String buildVariantsConfig(HttpServletRequest request, String recipeInstructions) {

        String[] names = request.getParameterValues("sizeName");
        String[] deltas = request.getParameterValues("sizeDelta");
        String[] toppingNames = request.getParameterValues("toppingName");
        String[] toppingPrices = request.getParameterValues("toppingPrice");
        List<String> sizeEntries = new ArrayList<>();
        List<String> toppingEntries = new ArrayList<>();
        Set<String> sizeNames = new HashSet<>();
        Set<String> toppingNamesSeen = new HashSet<>();
        boolean hasBaseSize = false;
        if ((names == null) != (deltas == null) || (names != null && names.length != deltas.length)) {
            throw new IllegalArgumentException("Mỗi size phải có đúng một mức phụ thu.");
        }
        if (names != null && names.length > MAX_SIZES) {
            throw new IllegalArgumentException("Không được thêm quá " + MAX_SIZES + " size.");
        }
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                String name = trimToNull(names[i]);
                String rawDelta = utils.FormInput.trim(deltas[i]);
                if (name == null) {
                    if (!rawDelta.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập tên size dòng " + (i + 1) + ".");
                    continue;
                }
                if (rawDelta.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập phụ thu size dòng " + (i + 1) + "; nhập 0 nếu không phụ thu.");
                if (name.length() > 30) throw new IllegalArgumentException("Tên size dòng " + (i + 1) + " tối đa 30 ký tự.");
                validateVariantName(name, "Tên size dòng " + (i + 1));
                if (!sizeNames.add(name.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Không được trùng tên size (dòng " + (i + 1) + ").");
                BigDecimal delta = parseNonNegative(rawDelta, "Phụ thu size dòng " + (i + 1));
                if (delta.signum() == 0) hasBaseSize = true;
                sizeEntries.add(jsonString(name) + ":" + delta.stripTrailingZeros().toPlainString());
            }
        }
        if (sizeEntries.isEmpty()) throw new IllegalArgumentException("Sản phẩm cần có ít nhất một size.");
        if (!hasBaseSize) {
            throw new IllegalArgumentException("Cần có một size phụ thu 0đ để giá mặc định đúng bằng giá bán cơ bản.");
        }
        if ((toppingNames == null) != (toppingPrices == null)
                || (toppingNames != null && toppingNames.length != toppingPrices.length)) {
            throw new IllegalArgumentException("Mỗi topping phải có đúng một mức giá.");
        }
        if (toppingNames != null && toppingNames.length > MAX_TOPPINGS) {
            throw new IllegalArgumentException("Không được thêm quá " + MAX_TOPPINGS + " topping.");
        }
        if (toppingNames != null) {
            for (int i = 0; i < toppingNames.length; i++) {
                String name = trimToNull(toppingNames[i]);
                String rawPrice = utils.FormInput.trim(toppingPrices[i]);
                if (name == null) {
                    if (!rawPrice.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập tên topping dòng " + (i + 1) + ".");
                    continue;
                }
                if (rawPrice.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập giá topping dòng " + (i + 1) + "; nhập 0 nếu miễn phí.");
                if (name.length() > 50) throw new IllegalArgumentException("Tên topping dòng " + (i + 1) + " tối đa 50 ký tự.");
                validateVariantName(name, "Tên topping dòng " + (i + 1));
                if (!toppingNamesSeen.add(name.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Không được trùng tên topping (dòng " + (i + 1) + ").");
                BigDecimal price = parseNonNegative(rawPrice, "Giá topping dòng " + (i + 1));
                toppingEntries.add("{\"name\":" + jsonString(name) + ",\"price\":" + price.stripTrailingZeros().toPlainString() + "}");
            }
        }
        String instructions = trimToNull(recipeInstructions);
        String extras = instructions == null ? "" : ",\"instructions\":" + jsonString(instructions);
        return "{\"sizes\":{" + String.join(",", sizeEntries) + "},\"toppings\":[" + String.join(",", toppingEntries) + "]" + extras + "}";
    }

    // [SPTIEN02]
    // . Trong parseNonNegative (kiểm tra và đọc số tiền không âm) của AdminProductsController.java, em
    //   nhận tiền từ từng dòng size hoặc topping do buildVariantsConfig (kiểm tra và ghép size, topping
    //   để lưu) truyền vào. Em gọi MenuValidation.money trong MenuValidation.java, kèm tên ô và số dòng
    //   để kiểm tra số tiền. Số hợp lệ trả về buildVariantsConfig để ghép cùng tên size/topping; sai thì
    //   chưa lưu.
    // ... Quay về buildVariantsConfig ngay sau parseNonNegative để ghép tiền với tên của chính dòng đó
    //     và xét các dòng còn lại. Đọc điểm trả cấu hình về save tại AdminProductsController.java, tìm
    //     mã SPBT02, phần return ở cuối hàm.
    private BigDecimal parseNonNegative(String value, String label) {
        return MenuValidation.money(value, label);
    }

    // [SPTEN02]
    // . Sau bước kiểm tra độ dài, buildVariantsConfig (kiểm tra và ghép size, topping để lưu) gọi
    //   validateVariantName (kiểm tra ký tự trong tên size/topping) cùng file, truyền tên size hoặc
    //   topping và tên ô. Em loại ký tự điều khiển, dấu nháy và các ký tự đặc biệt bị cấm trong tên; nếu
    //   sai thì báo lỗi trước khi kiểm tra tên trùng, ghép cấu hình và lưu sản phẩm.
    // ... Quay về buildVariantsConfig sau validateVariantName để xét trùng tên, nhận tiền và ghép dòng.
    //     Xét hết dòng thì đọc phần return tại AdminProductsController.java, tìm mã SPBT02; sau đó save
    //     mới gọi DAO.
    private void validateVariantName(String value, String label) {
        if (value.chars().anyMatch(ch -> Character.isISOControl(ch) || "\\\"'{}<>`".indexOf(ch) >= 0)) {
            throw new IllegalArgumentException(label + " chứa ký tự không được hỗ trợ.");
        }
    }

    private String jsonString(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if (ch < 0x20) out.append(String.format("\\u%04x", (int) ch)); else out.append(ch); }
            }
        }
        return out.append('"').toString();
    }

    // [SPAH02]
    // . Từ doPost, em gọi toggle (đổi trạng thái bán của sản phẩm) trong AdminProductsController.java để
    //   nhận mã món và trạng thái available mới. Em truyền hai giá trị vào updateProductAvailability (lưu
    //   trạng thái đang bán/tạm ngừng) trong ProductDAO.java để đổi trạng thái của đúng món, rồi nhận kết
    //   quả để báo lại trên trang.
    // ... Đọc tiếp tại ProductDAO.java, tìm [SPAH03].
    private void toggle(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {

        int id = parseId(request.getParameter("id"), false);
        String rawAvailable = request.getParameter("available");
        if (!"true".equals(rawAvailable) && !"false".equals(rawAvailable)) {
            throw new IllegalArgumentException("Trạng thái bán không hợp lệ.");
        }
        boolean available = Boolean.parseBoolean(rawAvailable);
        if (!productDAO.updateProductAvailability(id, available)) throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
        // [SPAH04]
        // . updateProductAvailability trả thành công thì toggle đặt productMessage rồi trở về doPost. doPost
        //   chuyển trang để doGet đọc trạng thái mới.
        // ... Xem kết quả tại admin-products.jsp, tìm mã SPL08; thông báo và trạng thái mới đã hiện là kết
        //     thúc đổi trạng thái bán.
        session.setAttribute("productMessage", "Đã cập nhật trạng thái bán sản phẩm.");
    }

    // [SPX02]
    // . Từ doPost, em gọi delete trong AdminProductsController.java để nhận mã món muốn xóa. Em gọi
    //   getProductForAdmin trong ProductDAO.java để lấy món và kiểm tra món còn tồn tại, đã ngừng bán.
    //   Đủ điều kiện thì em truyền mã cho deleteProduct cùng file để xóa; hàm này kiểm tra lại món chưa
    //   có trong đơn hàng. Kết quả trả về giúp em báo đã xóa hoặc chưa thể xóa, giữ nguyên lịch sử đơn.
    // ... delete gọi getProductForAdmin để đọc món và kiểm tra trước khi xóa. Đọc tại ProductDAO.java,
    //     tìm mã CTCH03; kết quả quay về delete, món hợp lệ mới tới deleteProduct.
    private void delete(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {
        int id = parseId(request.getParameter("id"), false);
        Product product = productDAO.getProductForAdmin(id);
        if (product == null) throw new IllegalArgumentException("Không tìm thấy sản phẩm cần xóa. Sản phẩm có thể đã bị xóa trước đó.");
        if (product.isAvailable()) throw new IllegalArgumentException("Vui lòng ngừng bán sản phẩm trước khi xóa.");
        if (!productDAO.deleteProduct(id)) {
            throw new IllegalArgumentException("Chưa xóa được sản phẩm: sản phẩm có thể đã thay đổi hoặc đã xuất hiện trong đơn hàng. Vui lòng tải lại danh sách; lịch sử đơn hàng được giữ nguyên.");
        }
        // [SPX04]
        // . deleteProduct trả thành công thì delete đặt productMessage rồi trở về doPost để chuyển trang.
        //   doGet đọc danh sách mới; lỗi trước đó đi vào catch và không báo đã xóa.
        // ... Xem kết quả tại admin-products.jsp, tìm mã SPL08; thông báo cùng danh sách không còn món vừa
        //     xóa là kết thúc xóa sản phẩm.
        session.setAttribute("productMessage", "Đã xóa sản phẩm khỏi danh sách.");
    }

    private Integer parseOptionalId(String value) {
        if (value == null || value.isBlank() || "all".equals(value)) return null;
        try { return Math.max(0, Integer.parseInt(value)); } catch (NumberFormatException ignored) { return null; }
    }

    private int parseId(String value, boolean allowZero) {
        try {
            int id = Integer.parseInt(value == null ? "" : value);
            if (id < (allowZero ? 0 : 1)) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mã sản phẩm hoặc danh mục không hợp lệ.");
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
        Object value = session.getAttribute(name);
        if (value != null) { request.setAttribute(name, value); session.removeAttribute(name); }
    }

    // [SPQ01]
    // . Trước khi mở hoặc xử lý sản phẩm, doGet và doPost gọi isAdmin (kiểm tra tài khoản Admin còn hoạt
    //   động) trong AdminProductsController.java. Hàm kiểm tra tài khoản phải là Admin còn hoạt động; đúng
    //   thì tiếp tục, sai thì trả lỗi và dừng yêu cầu.
    // ... Quay lại đầu doGet hoặc doPost trong AdminProductsController.java sau isAdmin; đúng thì tiếp tục phần
    //     sản phẩm, sai thì đã trả lỗi và kết thúc yêu cầu.
    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) { response.sendRedirect(request.getContextPath() + "/?tab=login"); return false; }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()) || !"ADMIN".equalsIgnoreCase(user.getRole())) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản không có quyền quản lý sản phẩm."); return false; }
        return true;
    }
}
