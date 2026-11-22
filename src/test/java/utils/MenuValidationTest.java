package utils;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MenuValidationTest {
    @Test void moneyAcceptsExactBoundariesAndSeparatesTheReasonsForRejection() {
        assertEquals(BigDecimal.ZERO, MenuValidation.money("0", "Giá"));
        assertEquals(MenuValidation.MAX_MONEY, MenuValidation.money("9999999999.99", "Giá"));
        assertEquals("Vui lòng nhập giá.", error("", false));
        assertEquals("Giá không được âm.", error("-0.01", false));
        assertTrue(error("10000000000", false).contains("9.999.999.999,99"));
        assertTrue(error("1.001", false).contains("2 chữ số thập phân"));
        for (String raw : new String[]{"NaN", "Infinity", "1e3", "1,5", "+1", "abc", "9".repeat(10000)})
            assertTrue(error(raw, false).contains("phải là số"), raw);
    }

    @Test void quantityAcceptsDecimalAndRejectsZeroOverflowAndExtraDecimalPlaces() {
        assertEquals(new BigDecimal("0.001"), MenuValidation.quantity("0.001", "Định lượng"));
        assertEquals(MenuValidation.MAX_QUANTITY, MenuValidation.quantity("999999999.999", "Định lượng"));
        assertTrue(error("0", true).contains("lớn hơn 0"));
        assertTrue(error("-1", true).contains("lớn hơn 0"));
        assertTrue(error("1000000000", true).contains("999.999.999,999"));
        assertTrue(error("1.0001", true).contains("3 chữ số thập phân"));
    }

    @Test void namesRejectBlankControlCharactersAndOverlongValues() {
        assertEquals("Cà phê", MenuValidation.requiredName(" Cà phê ", "Tên món", 150));
        assertDoesNotThrow(() -> MenuValidation.requiredName("x".repeat(150), "Tên món", 150));
        for (String raw : new String[]{"  ", "x".repeat(151), "A\u0085B", "A\nB"})
            assertThrows(IllegalArgumentException.class, () -> MenuValidation.requiredName(raw, "Tên món", 150));
    }

    private String error(String value, boolean quantity) {
        return assertThrows(IllegalArgumentException.class, () -> {
            if (quantity) MenuValidation.quantity(value, "Định lượng");
            else MenuValidation.money(value, "Giá");
        }).getMessage();
    }
}
