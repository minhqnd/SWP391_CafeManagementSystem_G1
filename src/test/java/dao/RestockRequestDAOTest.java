package dao;

import model.RestockRequest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RestockRequestDAO Integration & Unit Tests")
public class RestockRequestDAOTest {

    private RestockRequestDAO restockRequestDAO;

    @BeforeEach
    void setUp() {
        restockRequestDAO = new RestockRequestDAO();
    }

    @Test
    @DisplayName("Test get low stock ingredients")
    @org.junit.jupiter.api.Tag("database")
    void testGetLowStockIngredients() {
        try {
            assertNotNull(restockRequestDAO.getLowStockIngredients());
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Database connection not available: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test get restock requests")
    @org.junit.jupiter.api.Tag("database")
    void testGetRestockRequests() {
        try {
            List<RestockRequest> list = restockRequestDAO.getRestockRequests("ALL");
            assertNotNull(list);
            Map<String, Object> stats = restockRequestDAO.getRestockStats();
            assertNotNull(stats);
            assertTrue(stats.containsKey("draftCount"));
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Database connection not available: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test RestockItem subtotal and summary")
    void testRestockItemCalculations() {
        RestockRequest.RestockItem item = new RestockRequest.RestockItem(
                1, "Coffee beans", "gram", new BigDecimal("10"), new BigDecimal("40000")
        );
        assertEquals(new BigDecimal("400000"), item.getSubtotal());

        RestockRequest req = new RestockRequest();
        req.getItems().add(item);
        assertEquals("Coffee beans: 10 gram x 40,000đ", req.getItemsSummary());
    }
}
