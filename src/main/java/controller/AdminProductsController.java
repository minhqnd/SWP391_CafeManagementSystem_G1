package controller;

import dao.CategoryDAO;
import dao.ProductDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
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
import java.util.List;
import java.util.UUID;

@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class AdminProductsController extends HttpServlet {
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

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
            Integer categoryId = parseOptionalId(request.getParameter("categoryId"));
            String keyword = request.getParameter("q");
            List<Product> products = productDAO.getProductsForAdmin(categoryId, keyword);
            int[] stats = productDAO.getProductAdminStats();
            request.setAttribute("categories", categories);
            request.setAttribute("products", products);
            request.setAttribute("productTotal", stats[0]);
            request.setAttribute("productAvailable", stats[1]);
            request.setAttribute("productPaused", stats[0] - stats[1]);
            request.setAttribute("selectedCategoryId", categoryId == null ? 0 : categoryId);
            request.setAttribute("searchKeyword", keyword == null ? "" : keyword);

            String editId = request.getParameter("editId");
            if (editId != null && !editId.isBlank()) {
                try {
                    Product selected = productDAO.getProductForAdmin(Integer.parseInt(editId));
                    if (selected != null) request.setAttribute("selectedProduct", selected);
                } catch (NumberFormatException ignored) {
                    // Keep the blank form when the edit id is malformed.
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
        try {
            String action = request.getParameter("action");
            if ("save".equals(action)) save(request, session);
            else if ("toggle".equals(action)) toggle(request, session);
            else session.setAttribute("productError", "Thao tác không hợp lệ.");
        } catch (SQLException e) {
            getServletContext().log("Product database operation failed", e);
            session.setAttribute("productError", "Không thể lưu thay đổi vào database. Kiểm tra danh mục và dữ liệu sản phẩm.");
        } catch (ClassNotFoundException | IllegalArgumentException e) {
            session.setAttribute("productError", e.getMessage() == null ? "Dữ liệu sản phẩm không hợp lệ." : e.getMessage());
        } catch (IllegalStateException e) {
            getServletContext().log("Product upload exceeded the configured limit", e);
            session.setAttribute("productError", "Ảnh sản phẩm không được lớn hơn 5 MB.");
        } catch (RuntimeException e) {
            getServletContext().log("Product operation failed", e);
            session.setAttribute("productError", "Không thể hoàn tất thao tác sản phẩm.");
        }
        response.sendRedirect(request.getContextPath() + "/admin/products");
    }

    private void save(HttpServletRequest request, HttpSession session)
            throws SQLException, ClassNotFoundException, ServletException, IOException {
        int id = parseId(request.getParameter("id"), true);
        int categoryId = parseId(request.getParameter("categoryId"), false);
        String name = trimToNull(request.getParameter("name"));
        String description = trimToNull(request.getParameter("description"));
        boolean removeImage = "true".equals(request.getParameter("removeImage"));
        Part imagePart = request.getPart("imageFile");
        byte[] imageData = null;
        String imageType = null;
        String imageName = null;
        String recipeInstructions = null;
        boolean available = true;
        if (name == null || name.length() > 150) throw new IllegalArgumentException("Tên món bắt buộc và tối đa 150 ký tự.");
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
        if (id > 0) {
            Product existing = productDAO.getProductForAdmin(id);
            if (existing == null) throw new IllegalArgumentException("Không tìm thấy sản phẩm cần cập nhật.");
            recipeInstructions = existing.getRecipeInstructions();
            available = existing.isAvailable();
        }

        double basePrice;
        try {
            BigDecimal price = new BigDecimal(request.getParameter("basePrice"));
            if (price.signum() < 0 || price.scale() > 2) throw new NumberFormatException();
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
        product.setAvailable(available);
        product.setVariantsConfig(buildVariantsConfig(request, recipeInstructions));
        if (!productDAO.saveProduct(product, imageData, imageType, imageName, removeImage)) {
            throw new IllegalArgumentException("Không tìm thấy sản phẩm cần cập nhật.");
        }
        session.setAttribute("productMessage", id > 0 ? "Đã cập nhật món '" + name + "'." : "Đã thêm món '" + name + "'.");
    }

    private String buildVariantsConfig(HttpServletRequest request, String recipeInstructions) {
        String[] names = request.getParameterValues("sizeName");
        String[] deltas = request.getParameterValues("sizeDelta");
        String[] toppingNames = request.getParameterValues("toppingName");
        String[] toppingPrices = request.getParameterValues("toppingPrice");
        List<String> sizeEntries = new ArrayList<>();
        List<String> toppingEntries = new ArrayList<>();
        boolean hasBaseSize = false;
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                String name = trimToNull(names[i]);
                if (name == null) continue;
                if (name.length() > 30) throw new IllegalArgumentException("Tên size tối đa 30 ký tự.");
                BigDecimal delta = parseNonNegative(takeAt(deltas, i), "Phụ thu size");
                if (delta.signum() == 0) hasBaseSize = true;
                sizeEntries.add(jsonString(name) + ":" + delta.stripTrailingZeros().toPlainString());
            }
        }
        if (sizeEntries.isEmpty()) throw new IllegalArgumentException("Sản phẩm cần có ít nhất một size.");
        if (!hasBaseSize) {
            throw new IllegalArgumentException("Cần có một size phụ thu 0đ để giá mặc định đúng bằng giá bán cơ bản.");
        }
        if (toppingNames != null) {
            for (int i = 0; i < toppingNames.length; i++) {
                String name = trimToNull(toppingNames[i]);
                if (name == null) continue;
                if (name.length() > 50) throw new IllegalArgumentException("Tên topping tối đa 50 ký tự.");
                BigDecimal price = parseNonNegative(takeAt(toppingPrices, i), "Giá topping");
                toppingEntries.add("{\"name\":" + jsonString(name) + ",\"price\":" + price.stripTrailingZeros().toPlainString() + "}");
            }
        }
        String instructions = trimToNull(recipeInstructions);
        String extras = instructions == null ? "" : ",\"instructions\":" + jsonString(instructions);
        return "{\"sizes\":{" + String.join(",", sizeEntries) + "},\"toppings\":[" + String.join(",", toppingEntries) + "]" + extras + "}";
    }

    private BigDecimal parseNonNegative(String value, String label) {
        try {
            BigDecimal amount = new BigDecimal(value == null || value.isBlank() ? "0" : value);
            if (amount.signum() < 0 || amount.scale() > 2) throw new NumberFormatException();
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " phải là số không âm, tối đa 2 chữ số thập phân.");
        }
    }

    private String takeAt(String[] values, int index) {
        return values != null && index < values.length ? values[index] : "0";
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
        int id = parseId(request.getParameter("id"), false);
        boolean available = "true".equals(request.getParameter("available"));
        if (!productDAO.updateProductAvailability(id, available)) throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
        session.setAttribute("productMessage", "Đã cập nhật trạng thái bán sản phẩm.");
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
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ tài khoản Admin được quản lý sản phẩm."); return false; }
        return true;
    }
}
