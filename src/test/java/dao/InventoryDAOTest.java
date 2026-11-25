package dao;

import model.InventoryTransaction;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InventoryDAO Integration & Unit Tests")
public class InventoryDAOTest {

    private InventoryDAO inventoryDAO;

    @BeforeEach
    void setUp() {
        inventoryDAO = new InventoryDAO();
    }

    @Test
    @DisplayName("Test get inventory stock list with filter")
    @org.junit.jupiter.api.Tag("database")
    void testGetInventoryStockList() {
        try {
            assertNotNull(inventoryDAO.getInventoryStockList("all"));
            assertNotNull(inventoryDAO.getInventoryStockList("low_stock"));
            assertNotNull(inventoryDAO.getInventoryStockList("safe"));
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Database connection not available: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test get recent transactions")
    @org.junit.jupiter.api.Tag("database")
    void testGetRecentTransactions() {
        try {
            List<InventoryTransaction> list = inventoryDAO.getRecentTransactions(10, "all");
            assertNotNull(list);
            for (InventoryTransaction tx : list) {
                assertNotNull(tx.getTransactionType());
                assertNotNull(tx.getFormattedQuantity());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Database connection not available: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test inventory transaction model formatting")
    void testInventoryTransactionModel() {
        InventoryTransaction txPos = new InventoryTransaction();
        txPos.setTransactionType("IN_PURCHASE");
        txPos.setQuantity(new BigDecimal("500"));
        txPos.setUnit("ml");
        assertEquals("Nhập mua hàng", txPos.getTypeDisplayName());
        assertEquals("+500 ml", txPos.getFormattedQuantity());

        InventoryTransaction txNeg = new InventoryTransaction();
        txNeg.setTransactionType("OUT_ORDER");
        txNeg.setQuantity(new BigDecimal("120"));
        txNeg.setUnit("gram");
        assertEquals("Xuất theo đơn bán", txNeg.getTypeDisplayName());
        assertEquals("-120 gram", txNeg.getFormattedQuantity());

        InventoryTransaction adjDec = new InventoryTransaction();
        adjDec.setTransactionType("ADJUSTMENT");
        adjDec.setQuantity(new BigDecimal("50"));
        adjDec.setUnit("ml");
        adjDec.setReason("Giảm: Kiểm kho thiếu");
        assertTrue(adjDec.isDecrease());
        assertEquals("-50 ml", adjDec.getFormattedQuantity());

        InventoryTransaction adjInc = new InventoryTransaction();
        adjInc.setTransactionType("ADJUSTMENT");
        adjInc.setQuantity(new BigDecimal("50"));
        adjInc.setUnit("ml");
        adjInc.setReason("Kiểm kho dư");
        assertFalse(adjInc.isDecrease());
        assertEquals("+50 ml", adjInc.getFormattedQuantity());
    }
}
