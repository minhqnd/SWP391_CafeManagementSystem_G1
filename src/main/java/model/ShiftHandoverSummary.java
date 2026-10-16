package model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Tổng hợp doanh thu và số tiền cần bàn giao của một ca thu ngân.
 */
public class ShiftHandoverSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    private WorkSchedule schedule;
    private int successfulOrderCount;
    private BigDecimal cashSales = BigDecimal.ZERO;
    private BigDecimal nonCashSales = BigDecimal.ZERO;
    private BigDecimal totalSales = BigDecimal.ZERO;

    public WorkSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(WorkSchedule schedule) {
        this.schedule = schedule;
    }

    public int getSuccessfulOrderCount() {
        return successfulOrderCount;
    }

    public void setSuccessfulOrderCount(int successfulOrderCount) {
        this.successfulOrderCount = successfulOrderCount;
    }

    public BigDecimal getCashSales() {
        return cashSales;
    }

    public void setCashSales(BigDecimal cashSales) {
        this.cashSales = cashSales != null ? cashSales : BigDecimal.ZERO;
    }

    public BigDecimal getNonCashSales() {
        return nonCashSales;
    }

    public void setNonCashSales(BigDecimal nonCashSales) {
        this.nonCashSales = nonCashSales != null ? nonCashSales : BigDecimal.ZERO;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales != null ? totalSales : BigDecimal.ZERO;
    }

    public BigDecimal getStartingCash() {
        if (schedule == null || schedule.getStartingCash() == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(schedule.getStartingCash());
    }

    public BigDecimal getExpectedCash() {
        if (schedule != null && schedule.getEndingCashSystem() != null) {
            return BigDecimal.valueOf(schedule.getEndingCashSystem());
        }
        return getStartingCash().add(cashSales);
    }

    public BigDecimal getActualCash() {
        if (schedule == null || schedule.getEndingCashActual() == null) {
            return null;
        }
        return BigDecimal.valueOf(schedule.getEndingCashActual());
    }

    public BigDecimal getCashDifference() {
        BigDecimal actualCash = getActualCash();
        return actualCash == null ? null : actualCash.subtract(getExpectedCash());
    }

    public String getDifferenceType() {
        BigDecimal difference = getCashDifference();
        if (difference == null || difference.compareTo(BigDecimal.ZERO) == 0) {
            return "BALANCED";
        }
        return difference.signum() > 0 ? "SURPLUS" : "SHORTAGE";
    }
}
