package controller;

import org.junit.jupiter.api.Test;
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
}
