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

// BF11 - SẢN PHẨM: admin-products.jsp -> GET danh sách/lọc/editId; POST save hoặc toggle.
@WebServlet(name = "AdminProductsController", urlPatterns = {"/admin/products"})
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class AdminProductsController extends HttpServlet {
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final BigDecimal MAX_MONEY = new BigDecimal("9999999999.99");
    private static final int MAX_SIZES = 50;
    private static final int MAX_TOPPINGS = 50;
    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAdmin(request, response)) return;
        HttpSession session = request.getSession();
        if (session.getAttribute("productCsrfToken") == null) session.setAttribute("productCsrfToken", UUID.randomUUID().toString());
        moveFlash(session, request, "productMessage");
        moveFlash(session, request, "productError");
        List<Category> categories = new ArrayList<>();
        try {
            categories = categoryDAO.getAllCategoriesWithCount();
            // BF11: lọc theo categoryId và q; editId chọn món để nạp selectedProduct vào form sửa.
            Integer categoryId = parseOptionalId(request.getParameter("categoryId"));
            String keyword = request.getParameter("q");
            // PR02: . đọc điều kiện tìm món và kiểm tra Admin có chọn món để sửa không.
            // .. Danh sách, thống kê và thông tin món đang chọn đã sẵn sàng để hiển thị.
            // ... ProductDAO.getProductsForAdmin() [PR03] → request → admin-products.jsp [PR04].
            List<Product> products = productDAO.getProductsForAdmin(categoryId, keyword);
            int[] stats = productDAO.getProductAdminStats();
            request.setAttribute("categories", categories);
            request.setAttribute("products", products);
            request.setAttribute("productTotal", stats[0]);
            request.setAttribute("productAvailable", stats[1]);
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
            } else {
                String editId = request.getParameter("editId");
                if (editId != null && !editId.isBlank()) {
                try {
                    Product selected = productDAO.getProductForAdmin(Integer.parseInt(editId));
                    if (selected != null) request.setAttribute("selectedProduct", selected);
                } catch (NumberFormatException ignored) {
                    // Keep the blank form when the edit id is malformed.
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
            request.setAttribute("productError", "Không tải được danh sách sản phẩm. Kiểm tra kết nối database rồi tải lại trang.");
            request.getRequestDispatcher("/admin-products.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isAdmin(request, response)) return;
        HttpSession session = request.getSession();
        String expected = (String) session.getAttribute("productCsrfToken");
        if (expected == null || !expected.equals(request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên làm việc đã hết hạn. Tải lại trang rồi thử lại.");
            return;
        }
        String action = request.getParameter("action");
        try {
            // BF11: form gửi action=save để thêm/sửa, action=toggle để đổi trạng thái, action=delete để xóa món đủ điều kiện.
            // PR15: . chọn xử lý lưu món hoặc bật, tắt bán theo nút Admin vừa bấm.
            // .. Yêu cầu chỉ được xử lý sau khi kiểm tra Admin và mã bảo vệ form.
            // ... action=save → save() [PR16]; action=toggle → toggle() [PR27]; action=delete → delete() [PR29].
            if ("save".equals(action)) save(request, session);
            else if ("toggle".equals(action)) toggle(request, session);
            else if ("delete".equals(action)) delete(request, session);
            else session.setAttribute("productError", "Thao tác không hợp lệ.");
        } catch (SQLException e) {
            getServletContext().log("Product database operation failed", e);
            preserveDraftAfterSaveError(action, request, session);
            session.setAttribute("productError", "Không thể lưu thay đổi vào database. Kiểm tra danh mục và dữ liệu sản phẩm.");
        } catch (ClassNotFoundException | IllegalArgumentException e) {
            preserveDraftAfterSaveError(action, request, session);
            session.setAttribute("productError", e.getMessage() == null ? "Dữ liệu sản phẩm không hợp lệ." : e.getMessage());
        } catch (IllegalStateException e) {
            preserveDraftAfterSaveError(action, request, session);
            getServletContext().log("Product upload exceeded the configured limit", e);
            session.setAttribute("productError", "Ảnh sản phẩm không được lớn hơn 5 MB.");
        } catch (RuntimeException e) {
            preserveDraftAfterSaveError(action, request, session);
            getServletContext().log("Product operation failed", e);
            session.setAttribute("productError", "Không thể hoàn tất thao tác sản phẩm.");
        }
        String redirect = request.getContextPath() + "/admin/products";
        if ("save".equals(action) && session.getAttribute("productDraft") != null) redirect += "?draft=1#productEditor";
        response.sendRedirect(redirect);
    }

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

    private String buildDraftVariantsConfig(HttpServletRequest request) {
        List<String> sizes = new ArrayList<>();
        String[] sizeNames = request.getParameterValues("sizeName");
        String[] sizeDeltas = request.getParameterValues("sizeDelta");
        if (sizeNames != null) {
            for (int i = 0; i < sizeNames.length; i++) {
                String name = trimToNull(sizeNames[i]);
                if (name == null) continue;
                String amount = sizeDeltas != null && i < sizeDeltas.length ? sizeDeltas[i] : "0";
                if (amount == null || !amount.trim().matches("\\d+(?:\\.\\d{1,2})?")) amount = "0";
                sizes.add(jsonString(name) + ":" + amount.trim());
            }
        }
        List<String> toppings = new ArrayList<>();
        String[] toppingNames = request.getParameterValues("toppingName");
        String[] toppingPrices = request.getParameterValues("toppingPrice");
        if (toppingNames != null) {
            for (int i = 0; i < toppingNames.length; i++) {
                String name = trimToNull(toppingNames[i]);
                if (name == null) continue;
                String price = toppingPrices != null && i < toppingPrices.length ? toppingPrices[i] : "0";
                if (price == null || !price.trim().matches("\\d+(?:\\.\\d{1,2})?")) price = "0";
                toppings.add("{\"name\":" + jsonString(name) + ",\"price\":" + price.trim() + "}");
            }
        }
        return "{\"sizes\":{" + String.join(",", sizes) + "},\"toppings\":[" + String.join(",", toppings) + "]}";
    }

    private void save(HttpServletRequest request, HttpSession session)
            throws SQLException, ClassNotFoundException, ServletException, IOException {
        // BF11 - KIỂM TRA: tên, danh mục tồn tại, giá không âm, ảnh hợp lệ rồi mới lưu.
        // id=0 thêm mới; id>0 sửa món và giữ trạng thái bán/hướng dẫn công thức đã có.
        int id = parseId(request.getParameter("id"), true);
        // PR16: [SLBF11] Check product information
        // . kiểm tra tên món, danh mục, giá bán và ảnh trước khi lưu.
        // .. Khi sửa giữ trạng thái bán cũ, rồi ghép size và topping vào thông tin món.
        // ... buildVariantsConfig() [PR24] → ProductDAO.saveProduct(món, ảnh và thông tin tệp) [PR25].
        int categoryId = parseId(request.getParameter("categoryId"), false);
        // PR17: . Đọc name từ [PR05], trim và kiểm tra bắt buộc, tối đa 150 ký tự, không có ký tự điều khiển.
        String name = trimToNull(request.getParameter("name"));
        // PR18: . Đọc description từ [PR08], trim và giới hạn 5000 ký tự.
        String description = trimToNull(request.getParameter("description"));
        boolean removeImage = "true".equals(request.getParameter("removeImage"));
        // PR19: . Đọc imageFile/removeImage từ [PR09]; kiểm tra ảnh tối đa 5 MB, MIME và chữ ký.
        // ... Ảnh hợp lệ chuyển cùng Product tới DAO [PR25].
        Part imagePart = request.getPart("imageFile");
        byte[] imageData = null;
        String imageType = null;
        String imageName = null;
        String recipeInstructions = null;
        boolean available = true;
        if (name == null || name.length() > 150 || containsControlCharacter(name)) {
            throw new IllegalArgumentException("Tên món bắt buộc, tối đa 150 ký tự và không chứa ký tự điều khiển.");
        }
        if (description != null && description.length() > 5000) {
            throw new IllegalArgumentException("Mô tả không được dài quá 5000 ký tự.");
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
        // PR20: . Kiểm tra categoryId từ [PR06] tồn tại và ACTIVE; sai thì dừng lưu.
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
        // PR21: . Kiểm tra tên món trùng, loại trừ id đang sửa.
        // .. Trùng thì báo lỗi; không trùng mới kiểm tra giá và lưu.
        if (productDAO.productNameExists(name, id)) {
            throw new IllegalArgumentException("Tên sản phẩm đã tồn tại trong hệ thống.");
        }

        // PR22: . Kiểm tra basePrice từ [PR07]: không âm, tối đa hai số lẻ và không vượt 9999999999.99.
        double basePrice;
        try {
            String rawBasePrice = request.getParameter("basePrice");
            if (rawBasePrice == null || !rawBasePrice.trim().matches("\\d+(?:\\.\\d{1,2})?")) {
                throw new NumberFormatException();
            }
            BigDecimal price = new BigDecimal(rawBasePrice.trim());
            if (price.signum() < 0 || price.scale() > 2 || price.compareTo(MAX_MONEY) > 0) throw new NumberFormatException();
            basePrice = price.doubleValue();
        } catch (Exception e) {
            throw new IllegalArgumentException("Giá bán phải là số không âm, tối đa 2 chữ số thập phân.");
        }

        Product product = new Product();
        product.setId(id);
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setBasePrice(basePrice);
        product.setDescription(description);
        product.setImageUrl(null);
        // PR23: . Món mới đặt available=true; khi sửa giữ trạng thái bán hiện tại.
        // ... ProductDAO.saveProduct() [PR25] ghi is_available; trạng thái không phụ thuộc việc đã có BOM.
        product.setAvailable(available);
        // Size và topping là cấu hình JSON trong products.variants_config, không phải dòng BOM.
        product.setVariantsConfig(buildVariantsConfig(request, recipeInstructions));
        if (!productDAO.saveProduct(product, imageData, imageType, imageName, removeImage)) {
            throw new IllegalArgumentException("Không tìm thấy sản phẩm cần cập nhật.");
        }
        // PR26: . Lưu thành công thì bỏ bản nháp, đặt thông báo rồi redirect về [PR02].
        // .. Nếu lưu lỗi, doPost() giữ bản nháp; người dùng cần chọn lại file ảnh.
        session.removeAttribute("productDraft");
        session.setAttribute("productMessage", id > 0 ? "Đã cập nhật món '" + name + "'." : "Đã thêm món '" + name + "'.");
    }

    private String buildVariantsConfig(HttpServletRequest request, String recipeInstructions) {
        // PR24: . kiểm tra tên và phụ thu của các size, topping vừa nhập.
        // .. ghép thành cấu hình của món; phải có một size không phụ thu để khớp giá gốc.
        // ... Chuỗi JSON trở về save() [PR16], gắn vào Product.variantsConfig rồi đưa tới DAO [PR25].
        // BF11 - SIZE/TOPPING: đọc các mảng tên/giá từ form, kiểm tra phụ thu rồi dựng JSON lưu cùng món.
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
                String rawDelta = deltas[i] == null ? "" : deltas[i].trim();
                if (name == null) {
                    if (!rawDelta.isEmpty()) throw new IllegalArgumentException("Dòng size có phụ thu nhưng chưa có tên size.");
                    continue;
                }
                if (rawDelta.isEmpty()) throw new IllegalArgumentException("Mỗi size phải có phụ thu; nhập 0 nếu không phụ thu.");
                if (name.length() > 30) throw new IllegalArgumentException("Tên size tối đa 30 ký tự.");
                validateVariantName(name, "Tên size");
                if (!sizeNames.add(name.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Không được trùng tên size.");
                BigDecimal delta = parseNonNegative(rawDelta, "Phụ thu size");
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
                String rawPrice = toppingPrices[i] == null ? "" : toppingPrices[i].trim();
                if (name == null) {
                    if (!rawPrice.isEmpty()) throw new IllegalArgumentException("Dòng topping có giá nhưng chưa có tên topping.");
                    continue;
                }
                if (rawPrice.isEmpty()) throw new IllegalArgumentException("Mỗi topping phải có giá; nhập 0 nếu miễn phí.");
                if (name.length() > 50) throw new IllegalArgumentException("Tên topping tối đa 50 ký tự.");
                validateVariantName(name, "Tên topping");
                if (!toppingNamesSeen.add(name.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Không được trùng tên topping.");
                BigDecimal price = parseNonNegative(rawPrice, "Giá topping");
                toppingEntries.add("{\"name\":" + jsonString(name) + ",\"price\":" + price.stripTrailingZeros().toPlainString() + "}");
            }
        }
        String instructions = trimToNull(recipeInstructions);
        String extras = instructions == null ? "" : ",\"instructions\":" + jsonString(instructions);
        return "{\"sizes\":{" + String.join(",", sizeEntries) + "},\"toppings\":[" + String.join(",", toppingEntries) + "]" + extras + "}";
    }

    private BigDecimal parseNonNegative(String value, String label) {
        try {
            if (value == null || !value.trim().matches("\\d+(?:\\.\\d{1,2})?")) throw new NumberFormatException();
            BigDecimal amount = new BigDecimal(value.trim());
            if (amount.signum() < 0 || amount.scale() > 2 || amount.compareTo(MAX_MONEY) > 0) throw new NumberFormatException();
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " phải là số không âm, tối đa 2 chữ số thập phân.");
        }
    }

    private void validateVariantName(String value, String label) {
        if (value.chars().anyMatch(ch -> Character.isISOControl(ch) || "\\\"'{}<>`".indexOf(ch) >= 0)) {
            throw new IllegalArgumentException(label + " chứa ký tự không được hỗ trợ.");
        }
    }

    private boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(Character::isISOControl);
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

    private void toggle(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {
        // PR27: . nhận mã món và trạng thái bán Admin muốn đổi.
        // .. chỉ bật hoặc tắt bán để giữ nguyên lịch sử đơn hàng.
        // ... ProductDAO.updateProductAvailability(mã món, trạng thái bán) [PR28] → tải lại [PR02].
        // BF11 - NÚT MẮT: đổi is_available, không DELETE sản phẩm; lịch sử đơn vẫn còn.
        int id = parseId(request.getParameter("id"), false);
        String rawAvailable = request.getParameter("available");
        if (!"true".equals(rawAvailable) && !"false".equals(rawAvailable)) {
            throw new IllegalArgumentException("Trạng thái bán không hợp lệ.");
        }
        boolean available = Boolean.parseBoolean(rawAvailable);
        if (!productDAO.updateProductAvailability(id, available)) throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
        session.setAttribute("productMessage", "Đã cập nhật trạng thái bán sản phẩm.");
    }

    // PR29: . Nhận nút xóa món; chỉ cho xóa món đã ngừng bán và chưa có lịch sử đơn hàng.
    // ... ProductDAO.deleteProduct() [PR30] trả kết quả để báo thành công hoặc lỗi.
    private void delete(HttpServletRequest request, HttpSession session) throws SQLException, ClassNotFoundException {
        int id = parseId(request.getParameter("id"), false);
        if (!productDAO.deleteProduct(id)) {
            throw new IllegalArgumentException("Chỉ xóa được sản phẩm đã ngừng bán và chưa xuất hiện trong đơn hàng.");
        }
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
