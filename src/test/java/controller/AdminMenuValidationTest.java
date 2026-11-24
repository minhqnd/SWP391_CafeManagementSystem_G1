package controller;

import dao.CategoryDAO;
import dao.ProductDAO;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import model.Category;
import model.Product;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import support.ServletMocks;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminMenuValidationTest {
    private ServletMocks mock;
    private final Map<String,String[]> arrays = new HashMap<>();
    private int productSaves, categorySaves;
    private boolean hiddenCategory, duplicateName;
    private Exception failure;
    private boolean productExists = true, productAvailable;
    private Part imagePart;
    private AdminProductsController products;
    private AdminCategoriesController categories;

    @BeforeEach void setup() {
        mock = new ServletMocks();
        User admin = new User(); admin.setRole("ADMIN"); admin.setStatus("ACTIVE");
        mock.sessionValues.put("user", admin);
        mock.sessionValues.put("productCsrfToken", "csrf");
        mock.sessionValues.put("categoryCsrfToken", "csrf");
        mock.parameters.putAll(Map.of("csrfToken", "csrf", "action", "save", "id", "0", "categoryId", "1",
                "name", "Cà phê", "basePrice", "25000", "status", "ACTIVE"));
        arrays.put("sizeName", new String[]{"M"}); arrays.put("sizeDelta", new String[]{"0"});
        CategoryDAO categoryDAO = new CategoryDAO() {
            @Override public Category getCategoryById(int id) {
                return new Category(id, "Cà phê", hiddenCategory ? "INACTIVE" : "ACTIVE", null, 0);
            }
            @Override public boolean categoryNameExists(String name, int id) {
                if (failure instanceof IllegalStateException state) throw state;
                return duplicateName;
            }
            @Override public boolean saveCategory(Category c, byte[] data, String type, String file, boolean remove) throws SQLException {
                categorySaves++;
                if (failure instanceof SQLException sql) throw sql;
                return true;
            }
        };
        ProductDAO productDAO = new ProductDAO() {
            @Override public Product getProductForAdmin(int id) {
                if (!productExists) return null;
                Product product = new Product(); product.setId(id); product.setAvailable(productAvailable); return product;
            }
            @Override public boolean deleteProduct(int id) { return false; }
            @Override public boolean productNameExists(String name, int id) { return duplicateName; }
            @Override public boolean saveProduct(Product p, byte[] data, String type, String file, boolean remove) throws SQLException {
                productSaves++;
                if (failure instanceof SQLException sql) throw sql;
                if (failure instanceof RuntimeException runtime) throw runtime;
                return true;
            }
        };
        ServletContext context = ServletMocks.proxy(ServletContext.class, (p,m,a) -> null);
        products = new AdminProductsController(productDAO, categoryDAO) {
            @Override public ServletContext getServletContext() { return context; }
        };
        categories = new AdminCategoriesController(categoryDAO) {
            @Override public ServletContext getServletContext() { return context; }
        };
    }

    @Test void badPricesProduceSpecificErrorsAndNeverReachSave() throws Exception {
        for (String value : List.of("", "-1", "1.001", "10000000000", "NaN", "1e3")) {
            mock.parameters.put("basePrice", value); productPost();
            assertEquals(0, productSaves);
            assertNotNull(mock.sessionValues.get("productError"));
            assertNotNull(mock.sessionValues.get("productDraft"));
            assertNull(mock.sessionValues.get("productMessage"));
        }
    }

    @Test void hiddenCategoryAndDuplicateNameCannotBeSaved() throws Exception {
        hiddenCategory = true; productPost();
        assertTrue(error("product").contains("đang hoạt động"));
        hiddenCategory = false; duplicateName = true; productPost();
        assertTrue(error("product").contains("đã tồn tại"));
        assertEquals(0, productSaves);
    }

    @Test void invalidVariantsAndMissingZeroSurchargeCannotBeSaved() throws Exception {
        arrays.put("sizeDelta", new String[]{""}); productPost();
        assertTrue(error("product").contains("phụ thu"));
        arrays.put("sizeDelta", new String[]{"1"}); productPost();
        assertTrue(error("product").contains("0đ"));
        arrays.put("sizeName", new String[]{"M", "m"}); arrays.put("sizeDelta", new String[]{"0", "1"}); productPost();
        assertTrue(error("product").contains("trùng tên size"));
        assertEquals(0, productSaves);
    }

    @Test void failedDraftKeepsDuplicateRowsMissingNamesAndInvalidPricesExactly() throws Exception {
        arrays.put("sizeName", new String[]{"M", "M", ""});
        arrays.put("sizeDelta", new String[]{"0", "abc", "9"}); productPost();
        Object draft = mock.sessionValues.get("productDraft");
        var config = draft.getClass().getDeclaredField("variantsConfig"); config.setAccessible(true);
        assertEquals("{\"draftSizes\":[{\"name\":\"M\",\"amount\":\"0\"},{\"name\":\"M\",\"amount\":\"abc\"},{\"name\":\"\",\"amount\":\"9\"}],\"draftToppings\":[]}", config.get(draft));
    }

    @Test void technicalFailureMustNotBeReportedAsOversizedImageOrBadInput() throws Exception {
        for (Exception cause : List.of(new IllegalStateException("private connection data"), new SQLException("private SQL", "08001"))) {
            failure = cause; mock.sessionValues.put("productMessage", "old success"); productPost();
            assertFalse(error("product").contains("5 MB"));
            assertFalse(error("product").contains("private"));
            assertNull(mock.sessionValues.get("productMessage"));
            assertNotNull(mock.sessionValues.get("productDraft"));
        }
    }

    @Test void categoryNameControlCharactersAndDuplicateAreReportedCorrectly() throws Exception {
        mock.parameters.put("name", " "); categoryPost();
        assertEquals("Vui lòng nhập tên danh mục.", error("category"));
        mock.parameters.put("name", "A\u0085B"); categoryPost();
        assertTrue(error("category").contains("ký tự"));
        mock.parameters.put("name", "Cà phê"); duplicateName = true; categoryPost();
        assertTrue(error("category").contains("đã tồn tại"));
        assertEquals(0, categorySaves);
    }

    @Test void categoryLookupFailureIsNotAnImageErrorAndKeepsDraft() throws Exception {
        failure = new IllegalStateException("private database failure"); categoryPost();
        assertFalse(error("category").contains("5 MB"));
        assertFalse(error("category").contains("private"));
        assertNotNull(mock.sessionValues.get("categoryDraft"));
        assertNull(mock.sessionValues.get("categoryMessage"));
    }

    @Test void successAndFollowingFailureCannotLeaveBothNotifications() throws Exception {
        productPost(); assertNotNull(mock.sessionValues.get("productMessage"));
        mock.parameters.put("name", ""); productPost();
        assertNull(mock.sessionValues.get("productMessage")); assertNotNull(mock.sessionValues.get("productError"));
        mock.parameters.put("name", "Cà phê"); productPost();
        assertNull(mock.sessionValues.get("productError")); assertNull(mock.sessionValues.get("productDraft"));
    }

    @Test void deleteErrorsDistinguishMissingSellingAndReferencedProducts() throws Exception {
        mock.parameters.put("action", "delete"); mock.parameters.put("id", "13");
        productExists = false; productPost(); assertTrue(error("product").contains("Không tìm thấy"));
        productExists = true; productAvailable = true; productPost(); assertTrue(error("product").contains("ngừng bán"));
        productAvailable = false; productPost(); assertTrue(error("product").contains("lịch sử đơn hàng được giữ nguyên"));
        assertNull(mock.sessionValues.get("productMessage"));
    }

    @Test void invalidImageSizeTypeAndSignatureCannotSaveProduct() throws Exception {
        imagePart = image(5L * 1024 * 1024 + 1, "image/png", new byte[0]); productPost();
        assertTrue(error("product").contains("5 MB"));
        imagePart = image(3, "text/html", new byte[]{1,2,3}); productPost();
        assertTrue(error("product").contains("JPG, PNG, GIF hoặc WebP"));
        imagePart = image(3, "image/png", new byte[]{1,2,3}); productPost();
        assertTrue(error("product").contains("Nội dung tệp"));
        assertEquals(0, productSaves);
    }

    private Part image(long size, String type, byte[] bytes) {
        return ServletMocks.proxy(Part.class, (p,m,a) -> switch (m.getName()) {
            case "getSize" -> size;
            case "getContentType" -> type;
            case "getInputStream" -> new java.io.ByteArrayInputStream(bytes);
            case "getSubmittedFileName" -> "image.png";
            default -> null;
        });
    }

    private void productPost() throws Exception { products.doPost(request(), mock.response); }
    private void categoryPost() throws Exception { categories.doPost(request(), mock.response); }
    private String error(String prefix) { return Objects.toString(mock.sessionValues.get(prefix + "Error")); }
    private HttpServletRequest request() {
        return ServletMocks.proxy(HttpServletRequest.class, (p,m,a) -> switch (m.getName()) {
            case "getParameterValues" -> arrays.get(a[0]);
            case "getPart" -> imagePart;
            default -> m.invoke(mock.request, a);
        });
    }
}
