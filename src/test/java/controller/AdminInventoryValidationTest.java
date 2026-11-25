package controller;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class AdminInventoryValidationTest {

    @Test
    void validateQuantityChecksPositiveRangeAndScale() {
        assertEquals("Số lượng thay đổi không hợp lệ.",
                AdminInventoryController.validateQuantity(null));
        assertEquals("Số lượng thay đổi phải lớn hơn 0.",
                AdminInventoryController.validateQuantity(BigDecimal.ZERO));
        assertEquals("Số lượng thay đổi phải lớn hơn 0.",
                AdminInventoryController.validateQuantity(new BigDecimal("-5")));
        assertEquals("Số lượng thay đổi quá lớn (tối đa 999,999,999.999).",
                AdminInventoryController.validateQuantity(new BigDecimal("100000000000000000000000")));
        assertEquals("Số lượng thay đổi chỉ được có tối đa 3 chữ số thập phân.",
                AdminInventoryController.validateQuantity(new BigDecimal("1.2345")));

        assertNull(AdminInventoryController.validateQuantity(new BigDecimal("0.001")));
        assertNull(AdminInventoryController.validateQuantity(new BigDecimal("999999999.999")));
        assertNull(AdminInventoryController.validateQuantity(new BigDecimal("10.5000")));
    }

    @Test
    void validateReasonChecksLengthBounds() {
        assertEquals("Vui lòng nhập lý do điều chỉnh từ 3 đến 500 ký tự.",
                AdminInventoryController.validateReason(null));
        assertEquals("Vui lòng nhập lý do điều chỉnh từ 3 đến 500 ký tự.",
                AdminInventoryController.validateReason("   "));
        assertEquals("Vui lòng nhập lý do điều chỉnh từ 3 đến 500 ký tự.",
                AdminInventoryController.validateReason("ab"));
        assertEquals("Vui lòng nhập lý do điều chỉnh từ 3 đến 500 ký tự.",
                AdminInventoryController.validateReason("a".repeat(501)));

        assertNull(AdminInventoryController.validateReason("Kiểm kho cuối ngày"));
    }

    @Test
    void parseTransactionTypeAllowsOnlyAdjustmentOrSpoilage() {
        assertEquals("ADJUSTMENT", AdminInventoryController.parseTransactionType("ADJUSTMENT"));
        assertEquals("SPOILAGE", AdminInventoryController.parseTransactionType(" spoilage "));

        assertThrows(IllegalArgumentException.class,
                () -> AdminInventoryController.parseTransactionType("IN_PURCHASE"));
        assertThrows(IllegalArgumentException.class,
                () -> AdminInventoryController.parseTransactionType(null));
    }

    @Test
    void parseDirectionHandlesSpoilageAndAdjustment() {
        assertFalse(AdminInventoryController.parseDirection("SPOILAGE", "INCREASE"));
        assertFalse(AdminInventoryController.parseDirection("SPOILAGE", null));

        assertTrue(AdminInventoryController.parseDirection("ADJUSTMENT", "INCREASE"));
        assertFalse(AdminInventoryController.parseDirection("ADJUSTMENT", "decrease"));

        assertThrows(IllegalArgumentException.class,
                () -> AdminInventoryController.parseDirection("ADJUSTMENT", "INVALID"));
        assertThrows(IllegalArgumentException.class,
                () -> AdminInventoryController.parseDirection("ADJUSTMENT", null));
    }

    @Test
    void inventoryErrorMessageHidesTechnicalDetailsAndShowsFriendlyErrors() {
        String businessMsg = "Lượng trừ vượt quá tồn kho hiện tại (5).";
        assertEquals(businessMsg, AdminInventoryController.inventoryErrorMessage(new SQLException(businessMsg, "45000")));

        assertTrue(AdminInventoryController.inventoryErrorMessage(new SQLException("Out of range", "22003"))
                .contains("999,999,999.999"));
        assertTrue(AdminInventoryController.inventoryErrorMessage(new SQLException("Data too long", "22001"))
                .contains("500 ký tự"));
        assertTrue(AdminInventoryController.inventoryErrorMessage(new SQLException("Check violated", "HY000", 3819))
                .contains("lớn hơn 0"));

        String unexpected = AdminInventoryController.inventoryErrorMessage(new SQLException("secret SQL detail", "08000"));
        assertFalse(unexpected.contains("secret"));

        assertTrue(AdminInventoryController.inventoryErrorMessage(new NumberFormatException("For input string"))
                .contains("không hợp lệ"));
    }
}
