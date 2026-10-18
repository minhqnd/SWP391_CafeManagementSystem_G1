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
    private int cashCount;
    private BigDecimal cashSales = BigDecimal.ZERO;
    private int bankCount;
    private BigDecimal bankSales = BigDecimal.ZERO;
    private int vnpayCount;
    private BigDecimal vnpaySales = BigDecimal.ZERO;
    private int momoCount;
    private BigDecimal momoSales = BigDecimal.ZERO;
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

    public int getCashCount() {
        return cashCount;
    }

    public void setCashCount(int cashCount) {
        this.cashCount = cashCount;
    }

    public BigDecimal getCashSales() {
        return cashSales;
    }

    public void setCashSales(BigDecimal cashSales) {
        this.cashSales = cashSales != null ? cashSales : BigDecimal.ZERO;
    }

    public int getBankCount() {
        return bankCount;
    }

    public void setBankCount(int bankCount) {
        this.bankCount = bankCount;
    }

    public BigDecimal getBankSales() {
        return bankSales;
    }

    public void setBankSales(BigDecimal bankSales) {
        this.bankSales = bankSales != null ? bankSales : BigDecimal.ZERO;
    }

    public int getVnpayCount() {
        return vnpayCount;
    }

    public void setVnpayCount(int vnpayCount) {
        this.vnpayCount = vnpayCount;
    }

    public BigDecimal getVnpaySales() {
        return vnpaySales;
    }

    public void setVnpaySales(BigDecimal vnpaySales) {
        this.vnpaySales = vnpaySales != null ? vnpaySales : BigDecimal.ZERO;
    }

    public int getMomoCount() {
        return momoCount;
    }

    public void setMomoCount(int momoCount) {
        this.momoCount = momoCount;
    }

    public BigDecimal getMomoSales() {
        return momoSales;
    }

    public void setMomoSales(BigDecimal momoSales) {
        this.momoSales = momoSales != null ? momoSales : BigDecimal.ZERO;
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
