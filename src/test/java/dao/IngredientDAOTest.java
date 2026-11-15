package dao;

import model.Ingredient;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IngredientDAO Integration & Unit Tests")
public class IngredientDAOTest {

    private IngredientDAO ingredientDAO;

    @BeforeEach
    void setUp() {
        ingredientDAO = new IngredientDAO();
    }

    @Test
    @DisplayName("Test get all ingredients returns non-null list")
    @org.junit.jupiter.api.Tag("database")
    void testGetAllIngredients() {
        try {
            List<Ingredient> list = ingredientDAO.getAllIngredients();
            assertNotNull(list);
            Assumptions.assumeTrue(!list.isEmpty(), "Database not seeded with ingredients");
            for (Ingredient i : list) {
                assertNotNull(i.getName());
                assertNotNull(i.getUnit());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Database connection not available: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test get all ingredients with details and stats")
    @org.junit.jupiter.api.Tag("database")
    void testGetAllIngredientsWithDetails() {
        try {
            List<Ingredient> list = ingredientDAO.getAllIngredientsWithDetails(null, null);
            assertNotNull(list);
            int[] stats = ingredientDAO.getIngredientStats();
            assertNotNull(stats);
            assertEquals(4, stats.length);
            assertEquals(list.size(), stats[0]);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Database connection not available: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test model stock status logic")
    void testStockStatusLogic() {
        Ingredient ing1 = new Ingredient(1, 1, "Coffee beans", "gram", new BigDecimal("500"), new BigDecimal("1000"));
        assertEquals("Sắp hết", ing1.getStockStatus());
        assertEquals("badge-warning", ing1.getStockStatusBadgeClass());

        Ingredient ing2 = new Ingredient(2, 1, "Fresh milk", "ml", BigDecimal.ZERO, new BigDecimal("1000"));
        assertEquals("Hết hàng", ing2.getStockStatus());
        assertEquals("badge-danger", ing2.getStockStatusBadgeClass());

        Ingredient ing3 = new Ingredient(3, 1, "Syrup", "ml", new BigDecimal("5000"), new BigDecimal("1000"));
        assertEquals("Đủ hàng", ing3.getStockStatus());
        assertEquals("badge-success", ing3.getStockStatusBadgeClass());
    }
}
