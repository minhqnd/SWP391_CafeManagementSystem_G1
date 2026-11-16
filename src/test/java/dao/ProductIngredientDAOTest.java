package dao;

import model.Product;
import model.ProductIngredient;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("database")
@DisplayName("ProductIngredientDAO Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductIngredientDAOTest {

    private ProductIngredientDAO productIngredientDAO;
    private ProductDAO productDAO;

    private static final int TEST_PRODUCT_ID = 1;

    @BeforeEach
    void setUp() {
        productIngredientDAO = new ProductIngredientDAO();
        productDAO = new ProductDAO();
    }

    @Test
    @Order(1)
    @DisplayName("TC-BOM-01: Lấy danh sách nguyên liệu theo Product ID (getIngredientsByProductId)")
    void testGetIngredientsByProductId() {
        List<ProductIngredient> list = productIngredientDAO.getIngredientsByProductId(TEST_PRODUCT_ID);
        assertNotNull(list);
        Assumptions.assumeTrue(!list.isEmpty(), "Bỏ qua nếu DB chưa seed dữ liệu BOM");
        for (ProductIngredient pi : list) {
            assertEquals(TEST_PRODUCT_ID, pi.getProductId());
            assertTrue(pi.getQuantityRequired() > 0);
            assertNotNull(pi.getIngredientName());
            assertNotNull(pi.getUnit());
        }
    }

    @Test
    @Order(2)
    @DisplayName("TC-BOM-02: Lấy Map toàn bộ nguyên liệu của mọi sản phẩm (getAllProductIngredientsMap)")
    void testGetAllProductIngredientsMap() {
        Map<Integer, List<ProductIngredient>> map = productIngredientDAO.getAllProductIngredientsMap();
        assertNotNull(map);
        Assumptions.assumeTrue(!map.isEmpty(), "Bỏ qua nếu DB chưa seed dữ liệu");
        assertTrue(map.containsKey(TEST_PRODUCT_ID), "Map phải chứa sản phẩm ID = 1");
        List<ProductIngredient> items = map.get(TEST_PRODUCT_ID);
        assertFalse(items.isEmpty());
    }

    @Test
    @Order(3)
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="cafe.database.writeTests", matches="true")
    @DisplayName("TC-BOM-03: Cập nhật định mức nguyên liệu thành công")
    void testSaveRecipe_HappyPath() {
        Product beforeProduct = productDAO.getProductById(TEST_PRODUCT_ID);
        Assumptions.assumeTrue(beforeProduct != null, "Bỏ qua test nếu DB chưa khởi chạy");

        List<ProductIngredient> ingredients = new ArrayList<>();
        ingredients.add(new ProductIngredient(TEST_PRODUCT_ID, 1, 18.0, null, null));

        assertDoesNotThrow(() -> {
            productIngredientDAO.saveRecipe(TEST_PRODUCT_ID, ingredients);
        });

        List<ProductIngredient> saved = productIngredientDAO.getIngredientsByProductId(TEST_PRODUCT_ID);
        assertNotNull(saved);
        assertFalse(saved.isEmpty());
        assertEquals(18.0, saved.get(0).getQuantityRequired(), 0.001);
    }
}
