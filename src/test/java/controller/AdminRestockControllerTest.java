package controller;

import model.RestockRequest.RestockItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminRestockControllerTest {

    @Test
    void calculatesStoredUnitPriceFromBatchTotal() {
        assertEquals(new BigDecimal("100.00"), AdminRestockController.calculateUnitPrice(
                new BigDecimal("1000"), new BigDecimal("100000")));
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> AdminRestockController.calculateUnitPrice(
                BigDecimal.ZERO, new BigDecimal("100000")));
    }

    @Test
    void validateQuantityChecksRangeAndScale() {
        assertEquals("Số lượng đề xuất phải lớn hơn 0.",
                AdminRestockController.validateQuantity(BigDecimal.ZERO));
        assertEquals("Số lượng đề xuất quá lớn (tối đa 999,999,999.999).",
                AdminRestockController.validateQuantity(new BigDecimal("1000000000000")));
        assertEquals("Số lượng đề xuất chỉ được có tối đa 3 chữ số thập phân.",
                AdminRestockController.validateQuantity(new BigDecimal("1.2345")));
        assertNull(AdminRestockController.validateQuantity(new BigDecimal("10.5000")));
    }

    @Test
    void validateReasonAndOptionalTextCheckLengths() {
        assertEquals("Lý do đề xuất phải từ 3 đến 500 ký tự.",
                AdminRestockController.validateReason("ab"));
        assertEquals("Lý do đề xuất phải từ 3 đến 500 ký tự.",
                AdminRestockController.validateReason("a".repeat(501)));
        assertNull(AdminRestockController.validateReason("Bổ sung cà phê hạt"));

        assertEquals("Ghi chú thêm không được vượt quá 500 ký tự.",
                AdminRestockController.validateOptionalText("a".repeat(501), "Ghi chú thêm"));
        assertNull(AdminRestockController.validateOptionalText("Ưu tiên giao sáng", "Ghi chú thêm"));
    }

    @Test
    void parseRestockItemsRejectsInvalidOrDuplicateLines() {
        List<RestockItem> items = AdminRestockController.parseRestockItems(
                new String[]{"1", "2"}, new String[]{"10.5000", "5"});
        assertEquals(2, items.size());

        assertThrows(IllegalArgumentException.class, () ->
                AdminRestockController.parseRestockItems(new String[]{"1", "1"}, new String[]{"5", "6"}));
        assertThrows(IllegalArgumentException.class, () ->
                AdminRestockController.parseRestockItems(new String[]{"0"}, new String[]{"5"}));
        assertThrows(IllegalArgumentException.class, () ->
                AdminRestockController.parseRestockItems(new String[]{"1"}, new String[]{"abc"}));
    }

    @Test
    void restockErrorMessageHidesTechnicalDetails() {
        assertTrue(AdminRestockController.restockErrorMessage(new SQLException("Out of range", "22003"))
                .contains("999,999,999.999"));
        assertTrue(AdminRestockController.restockErrorMessage(new SQLException("Data too long", "22001"))
                .contains("500 ký tự"));
        assertFalse(AdminRestockController.restockErrorMessage(new SQLException("secret SQL detail", "08000"))
                .contains("secret"));
    }
}
