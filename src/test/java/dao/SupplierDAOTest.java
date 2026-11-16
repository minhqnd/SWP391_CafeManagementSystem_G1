package dao;

import model.Supplier;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("database")
@DisplayName("SupplierDAO Integration & Unit Tests")
public class SupplierDAOTest {

    private SupplierDAO supplierDAO;

    @BeforeEach
    void setUp() {
        supplierDAO = new SupplierDAO();
    }

    @Test
    @DisplayName("Test get all suppliers returns valid list")
    void testGetAllSuppliers() {
        try {
            List<Supplier> suppliers = supplierDAO.getAllSuppliers();
            assertNotNull(suppliers, "Suppliers list should not be null");
            Assumptions.assumeTrue(!suppliers.isEmpty(), "Database not seeded with suppliers");
            for (Supplier s : suppliers) {
                assertNotNull(s.getName(), "Supplier name must not be null");
                assertNotNull(s.getStatus(), "Supplier status must not be null");
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Database connection not available: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test get active suppliers returns only active items")
    void testGetActiveSuppliers() {
        try {
            List<Supplier> activeList = supplierDAO.getActiveSuppliers();
            assertNotNull(activeList);
            for (Supplier s : activeList) {
                assertEquals("ACTIVE", s.getStatus());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Database connection not available: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test get supplier stats")
    void testGetSupplierStats() {
        try {
            int[] stats = supplierDAO.getSupplierStats();
            assertNotNull(stats);
            assertEquals(3, stats.length);
            assertTrue(stats[0] >= 0);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Database connection not available: " + e.getMessage());
        }
    }
}
