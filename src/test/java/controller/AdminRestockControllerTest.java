package controller;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
