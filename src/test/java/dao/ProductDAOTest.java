package dao;

import model.Product;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("database")
@DisplayName("ProductDAO Integration & Unit Tests")
public class ProductDAOTest {

    private ProductDAO productDAO;

    @BeforeEach
    void setUp() {
        productDAO = new ProductDAO();
    }

    @Test
    @DisplayName("Test get all available products")
    void testGetAllAvailableProducts() {
        List<Product> products = productDAO.getAllAvailableProducts();
        assertNotNull(products, "Product list should not be null");
        Assumptions.assumeTrue(!products.isEmpty(), "Skipping assertions because database is not seeded or unavailable in CI environment");
        assertTrue(products.size() >= 12, "Should return at least 12 sample products");

        for (Product p : products) {
            assertTrue(p.isAvailable(), "Each returned product must be available");
            assertNotNull(p.getName(), "Product name must not be null");
            assertTrue(p.getBasePrice() > 0, "Base price must be greater than 0");
        }
    }

    @Test
    @DisplayName("Test get products by valid category")
    void testGetProductsByCategory() {
        // Category 1: Cà phê máy
        List<Product> products = productDAO.getProductsByCategory(1);
        assertNotNull(products);
        Assumptions.assumeTrue(!products.isEmpty(), "Skipping because database has no category 1 products");
        for (Product p : products) {
            assertEquals(1, p.getCategoryId(), "Product category ID must be 1");
        }
    }

    @Test
    @DisplayName("Test get products by non-existent category")
    void testGetProductsByNonExistentCategory() {
        List<Product> products = productDAO.getProductsByCategory(999999);
        assertNotNull(products);
        assertTrue(products.isEmpty(), "Should return empty list for non-existent category");
    }

    @Test
    @DisplayName("Test search products with Vietnamese keyword")
    void testSearchProducts() {
        List<Product> results = productDAO.searchProducts("Muối");
        assertNotNull(results);
        Assumptions.assumeTrue(!results.isEmpty(), "Skipping because database is not seeded with sample products");
        assertTrue(results.get(0).getName().contains("Muối"));
    }

    @Test
    @DisplayName("Test get product by ID")
    void testGetProductById() {
        Product p = productDAO.getProductById(1);
        Assumptions.assumeTrue(p != null, "Skipping because product ID 1 is not in database");
        assertEquals("Cà Phê Muối Aura Đặc Biệt", p.getName());
        assertEquals("45.000đ", p.getFormattedPrice());
    }

    @Test
    @DisplayName("Test get non-existent product by ID")
    void testGetNonExistentProductById() {
        Product p = productDAO.getProductById(999999);
        assertNull(p, "Should return null for non-existent product ID");
    }

    @Test
    @DisplayName("Test total available count")
    void testGetTotalAvailableCount() {
        int count = productDAO.getTotalAvailableCount();
        Assumptions.assumeTrue(count > 0, "Skipping because database has 0 products");
        assertTrue(count >= 12, "Total count should be at least 12");
    }
}
