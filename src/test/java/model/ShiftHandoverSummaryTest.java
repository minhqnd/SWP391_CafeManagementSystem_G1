package model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShiftHandoverSummaryTest {

    @Test
    void calculatesExpectedCashForAnOpenShift() {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setStartingCash(500_000d);

        ShiftHandoverSummary summary = new ShiftHandoverSummary();
        summary.setSchedule(schedule);
        summary.setCashSales(new BigDecimal("725000"));

        assertEquals(0, new BigDecimal("1225000").compareTo(summary.getExpectedCash()));
    }

    @Test
    void usesLockedSystemCashAndReportsShortageForAClosedShift() {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setStartingCash(500_000d);
        schedule.setEndingCashSystem(1_225_000d);
        schedule.setEndingCashActual(1_200_000d);

        ShiftHandoverSummary summary = new ShiftHandoverSummary();
        summary.setSchedule(schedule);
        summary.setCashSales(new BigDecimal("999999"));

        assertEquals(0, new BigDecimal("1225000").compareTo(summary.getExpectedCash()));
        assertEquals(0, new BigDecimal("-25000").compareTo(summary.getCashDifference()));
        assertEquals("SHORTAGE", summary.getDifferenceType());
    }

    @Test
    void reportsBalancedAndSurplusCash() {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setEndingCashSystem(1_000_000d);
        schedule.setEndingCashActual(1_000_000d);

        ShiftHandoverSummary summary = new ShiftHandoverSummary();
        summary.setSchedule(schedule);
        assertEquals("BALANCED", summary.getDifferenceType());

        schedule.setEndingCashActual(1_010_000d);
        assertEquals("SURPLUS", summary.getDifferenceType());
    }
}
