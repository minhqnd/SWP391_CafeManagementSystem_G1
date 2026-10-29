package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShiftHandoverTest {
    @Test
    void handoverIsClosedWithoutAdminReview() {
        ShiftHandover handover = new ShiftHandover();
        handover.setStatus("CONFIRMED");

        assertEquals("Đã chốt ca", handover.getStatusLabel());
    }
}
