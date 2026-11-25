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
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_SIZES = 50;
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
    // . Hàm doGet trong AdminProductsController chuẩn bị dữ liệu cho trang quản lý sản phẩm. Em gọi
    //   ProductDAO lấy danh sách và số tổng, gọi CategoryDAO lấy danh mục, rồi đưa về trang. Nếu người dùng
    //   đang sửa hoặc vừa lưu lỗi, em điền thêm thông tin của món vào form để họ tiếp tục.
    //
    // Tra tiếp: ProductDAO.java / SPM03.
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

            request.setAttribute("productTotal", stats[0]);
            request.setAttribute("productAvailable", stats[1]);
            // [SQL11-48] Thẻ Tạm ngừng sản phẩm: Java lấy tổng món trừ số đang bán.

            request.setAttribute("productPaused", stats[0] - stats[1]);
            request.setAttribute("selectedCategoryId", categoryId == null ? 0 : categoryId);
            request.setAttribute("searchKeyword", keyword == null ? "" : keyword);

            ProductDraft draft = (ProductDraft) session.getAttribute("productDraft");
            if (draft != null) {

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
                // . Trong doGet của AdminProductsController, em nhận mã món từ nút Sửa rồi gọi
                //   getProductForAdmin của ProductDAO để đọc thông tin hiện tại. Có món thì điền vào form;
                //   không có thì báo không tìm thấy. Người dùng phải bấm Lưu mới cập nhật dữ liệu.
                //
                // Kết thúc: mở sửa khi thông tin món được đưa vào form; bấm Lưu mới ghi dữ liệu.
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
    // . Khi form từ nút Thêm sản phẩm hoặc Lưu thay đổi được gửi tới, doPost của AdminProductsController nhận
    //   thông tin món vừa nhập. Em gọi save ngay trong class này để kiểm tra và gom đủ thông tin sản phẩm
    //   trước khi chuyển sang bước ghi dữ liệu.
    //
    // Tra tiếp: SPL04.
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

    // Giữ các ô và dòng size/topping đang nhập để điền lại form khi lưu lỗi.
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

    // Giữ nguyên các dòng chưa hợp lệ để người dùng sửa tiếp, chưa dùng làm sản phẩm đã lưu.
    private String buildDraftVariantsConfig(HttpServletRequest request) {
        return "{\"draftSizes\":" + draftRows(request, "sizeName", "sizeDelta")
                + ",\"draftToppings\":" + draftRows(request, "toppingName", "toppingPrice") + "}";
    }

    private String draftRows(HttpServletRequest request, String nameField, String amountField) {

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
    // . Trong save của AdminProductsController, em lấy thông tin món, kiểm tra danh mục, tên trùng và ảnh;
    //   món mới được bán ngay, còn sửa thì giữ trạng thái cũ. Em gọi buildVariantsConfig để ghép size,
    //   topping vào Product, rồi chuyển cả sản phẩm và ảnh cho saveProduct trong ProductDAO để lưu.
    //
    // Tra tiếp: ProductDAO.java / SPL05.
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

        name = MenuValidation.requiredName(name, "Tên món", 150);

        if (description != null && description.length() > 5000) {
            throw new IllegalArgumentException("Mô tả không được dài quá 5000 ký tự.");
        }

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

        if (productDAO.productNameExists(name, id)) {
            throw new IllegalArgumentException("Tên sản phẩm đã tồn tại trong hệ thống.");
        }

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

        if (!productDAO.saveProduct(product, imageData, imageType, imageName, removeImage)) {
            throw new IllegalArgumentException("Không tìm thấy sản phẩm cần cập nhật.");
        }

        // [SPL06]
        // . Khi saveProduct lưu thành công, em quay lại save trong AdminProductsController để bỏ bản nhập
        //   dở và đặt thông báo có tên món. Sau đó doPost mở lại trang; doGet đọc dữ liệu hiện tại rồi
        //   đưa về admin-products.jsp để Admin thấy sản phẩm vừa thêm hoặc sửa.
        //
        // Tra tiếp: admin-products.jsp / SPL07.
        session.removeAttribute("productDraft");
        session.setAttribute("productMessage", id > 0 ? "Đã cập nhật món '" + name + "'." : "Đã thêm món '" + name + "'.");
    }

    // [SPBT03]
    // . Hàm buildVariantsConfig trong AdminProductsController nhận các cặp tên và tiền của size, topping rồi
    //   kiểm tra từng dòng. Em ghép các dòng đúng vào thông tin lựa chọn của món và trả về hàm save. Size cần
    //   có một lựa chọn không phụ thu; topping có thể để trống.
    //
    // Tra cứu: Trở về SPL04 ngay sau buildVariantsConfig để gọi lưu món. Phần kiểm tra số tiền dùng
    //          MenuValidation.java / QLKT01; xem xong quay lại sau parseNonNegative trong SPBT03.
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

    // Kiểm tra tiền của một dòng size/topping trước khi ghép vào món.
    private BigDecimal parseNonNegative(String value, String label) {
        return MenuValidation.money(value, label);
    }

    // Kiểm tra tên lựa chọn để nó dùng được trong thông tin size/topping.
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
    // . Hàm toggle trong AdminProductsController nhận món cần đổi trạng thái và kiểm tra món còn tồn tại. Em
    //   gọi updateProductAvailability trong ProductDAO để ghi trạng thái mới. Chỉ khi ghi được em mới đặt
    //   thông báo cho người dùng.
    //
    // Tra tiếp: ProductDAO.java / SPAH03.
    private void toggle(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {

        int id = parseId(request.getParameter("id"), false);
        String rawAvailable = request.getParameter("available");
        if (!"true".equals(rawAvailable) && !"false".equals(rawAvailable)) {
            throw new IllegalArgumentException("Trạng thái bán không hợp lệ.");
        }
        boolean available = Boolean.parseBoolean(rawAvailable);
        if (!productDAO.updateProductAvailability(id, available)) throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
        // [SPAH04]
        // . Khi trạng thái đã được lưu, em đặt thông báo rồi để doPost mở lại danh sách. Hàm doGet đọc lại dữ
        //   liệu để nhãn đang bán hoặc tạm ngừng trên trang khớp với lần thay đổi vừa làm.
        //
        // Kết thúc: đổi trạng thái khi danh sách mở lại kèm kết quả.
        session.setAttribute("productMessage", "Đã cập nhật trạng thái bán sản phẩm.");
    }

    // [SPX02]
    // . Hàm delete trong AdminProductsController đọc lại món và kiểm tra món đã ngừng bán trước khi gọi
    //   ProductDAO xóa. Phần dữ liệu tiếp tục kiểm tra món có trong đơn hàng không, để việc xóa sản phẩm giữ
    //   được lịch sử mua.
    //
    // Tra tiếp: ProductDAO.java / SPX03.
    private void delete(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {
        int id = parseId(request.getParameter("id"), false);
        Product product = productDAO.getProductForAdmin(id);
        if (product == null) throw new IllegalArgumentException("Không tìm thấy sản phẩm cần xóa. Sản phẩm có thể đã bị xóa trước đó.");
        if (product.isAvailable()) throw new IllegalArgumentException("Vui lòng ngừng bán sản phẩm trước khi xóa.");
        if (!productDAO.deleteProduct(id)) {
            throw new IllegalArgumentException("Chưa xóa được sản phẩm: sản phẩm có thể đã thay đổi hoặc đã xuất hiện trong đơn hàng. Vui lòng tải lại danh sách; lịch sử đơn hàng được giữ nguyên.");
        }
        // [SPX04]
        // . Sau khi deleteProduct xác nhận đã xóa, em đặt thông báo rồi để doPost mở lại danh sách. Hàm doGet
        //   đọc lại sản phẩm, nên món vừa xóa không còn xuất hiện trên trang.
        //
        // Kết thúc: xóa khi danh sách mở lại kèm kết quả.
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

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) { response.sendRedirect(request.getContextPath() + "/?tab=login"); return false; }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()) || !"ADMIN".equalsIgnoreCase(user.getRole())) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản không có quyền quản lý sản phẩm."); return false; }
        return true;
    }
}
