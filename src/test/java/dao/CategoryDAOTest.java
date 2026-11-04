package dao;

import model.Category;
import context.DBContext;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
    void testGetAllActiveCategoriesWithCount() throws Exception {
        Map<Integer, Integer> expectedCounts = new HashMap<>();
        String sql = "SELECT c.id, (SELECT COUNT(*) FROM products p "
                + "WHERE p.category_id=c.id AND p.is_available=1) AS expected_count "
                + "FROM categories c WHERE c.status='ACTIVE'";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                expectedCounts.put(results.getInt("id"), results.getInt("expected_count"));
            }
        }
        List<Category> categories = categoryDAO.getAllActiveCategoriesWithCount();
        assertNotNull(categories, "Category list should not be null");
        assertEquals(expectedCounts.size(), categories.size(), "Should return every active category in the current database");

        for (Category c : categories) {
            assertEquals("ACTIVE", c.getStatus());
            assertNotNull(c.getName());
            Integer expectedCount = expectedCounts.remove(c.getId());
            assertNotNull(expectedCount, "Unexpected or duplicate category ID: " + c.getId());
            assertEquals(expectedCount.intValue(), c.getProductCount(), "Available product count must match the database");
        }
        assertTrue(expectedCounts.isEmpty(), "No active categories should be omitted");
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
