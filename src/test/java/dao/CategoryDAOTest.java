package dao;

import model.Category;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CategoryDAO Integration & Unit Tests")
public class CategoryDAOTest {

    private CategoryDAO categoryDAO;

    @BeforeEach
    void setUp() {
        categoryDAO = new CategoryDAO();
    }

    @Test
    @DisplayName("Test get all active categories with product count")
    void testGetAllActiveCategoriesWithCount() {
        List<Category> categories = categoryDAO.getAllActiveCategoriesWithCount();
        assertNotNull(categories, "Category list should not be null");
        Assumptions.assumeTrue(!categories.isEmpty(), "Skipping assertions because database is not seeded or unavailable in CI environment");
        assertEquals(6, categories.size(), "Should have 6 active categories matching UI");

        for (Category c : categories) {
            assertEquals("ACTIVE", c.getStatus());
            assertNotNull(c.getName());
            assertTrue(c.getProductCount() >= 0, "Product count must be non-negative");
        }
    }

    @Test
    @DisplayName("Test get category by ID")
    void testGetCategoryById() {
        Category c = categoryDAO.getCategoryById(1);
        Assumptions.assumeTrue(c != null, "Skipping because sample category ID 1 is not in database");
        assertEquals(1, c.getId());
        assertEquals("Cà phê máy", c.getName());
        assertEquals("ACTIVE", c.getStatus());
    }

    @Test
    @DisplayName("Test get non-existent category")
    void testGetNonExistentCategory() {
        Category c = categoryDAO.getCategoryById(999999);
        assertNull(c, "Non-existent category should return null");
    }
}
