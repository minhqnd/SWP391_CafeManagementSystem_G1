package controller;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class AdminIngredientMessageTest {
    @Test void preservesUserFacingReferenceExplanation() {
        String message = "Không thể xóa nguyên liệu vì đã có lịch sử giao dịch kho.";
        assertEquals(message, AdminIngredientController.ingredientErrorMessage(new SQLException(message, "45000")));
    }
    @Test void hidesForeignKeySqlDetails() {
        String message = AdminIngredientController.ingredientErrorMessage(
                new SQLException("Cannot delete parent aura_cafe_erp inventory_transactions fk_inventory_ingredient", "23000"));
        assertTrue(message.contains("liên kết"));
        assertFalse(message.contains("fk_inventory"));
        assertFalse(message.contains("aura_cafe_erp"));
    }
    @Test void hidesUnexpectedDatabaseError() {
        assertFalse(AdminIngredientController.ingredientErrorMessage(new SQLException("secret database details", "08000"))
                .contains("secret"));
    }
    @Test void malformedInputHasReadableMessage() {
        assertTrue(AdminIngredientController.ingredientErrorMessage(new NumberFormatException("For input string"))
                .contains("không hợp lệ"));
    }

    @Test void validateTextChecksBlankAndMaxLength() {
        assertEquals("Tên nguyên liệu không được để trống.",
                AdminIngredientController.validateText("   ", "Tên nguyên liệu", 150));
        assertEquals("Đơn vị tính (ĐVT) không được vượt quá 20 ký tự.",
                AdminIngredientController.validateText("a".repeat(21), "Đơn vị tính (ĐVT)", 20));
        assertNull(AdminIngredientController.validateText("  Cà phê hạt  ", "Tên nguyên liệu", 150));
    }

    @Test void validateStockNumberChecksRangeAndScale() {
        assertEquals("Tồn kho ban đầu không được nhỏ hơn 0.",
                AdminIngredientController.validateStockNumber(new BigDecimal("-1"), "Tồn kho ban đầu"));
        assertEquals("Tồn kho ban đầu quá lớn (tối đa 999,999,999.999).",
                AdminIngredientController.validateStockNumber(new BigDecimal("100000000000000000000000"), "Tồn kho ban đầu"));
        assertEquals("Ngưỡng tối thiểu chỉ được có tối đa 3 chữ số thập phân.",
                AdminIngredientController.validateStockNumber(new BigDecimal("1.2345"), "Ngưỡng tối thiểu"));

        assertNull(AdminIngredientController.validateStockNumber(BigDecimal.ZERO, "Tồn kho ban đầu"));
        assertNull(AdminIngredientController.validateStockNumber(new BigDecimal("999999999.999"), "Tồn kho ban đầu"));
        assertNull(AdminIngredientController.validateStockNumber(new BigDecimal("10.5000"), "Ngưỡng tối thiểu"));
    }

    @Test void databaseDataErrorsShowSpecificMessages() {
        assertTrue(AdminIngredientController.ingredientErrorMessage(new SQLException("Out of range", "22003"))
                .contains("999,999,999.999"));
        assertTrue(AdminIngredientController.ingredientErrorMessage(new SQLException("Data too long", "22001"))
                .contains("150 ký tự"));
        assertTrue(AdminIngredientController.ingredientErrorMessage(new SQLException("Check constraint violated", "HY000", 3819))
                .contains("lớn hơn hoặc bằng 0"));
    }
}
