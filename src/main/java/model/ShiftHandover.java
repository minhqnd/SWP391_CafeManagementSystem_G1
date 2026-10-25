package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * ShiftHandover - Biên bản bàn giao ca của Thu ngân (bảng shift_handovers, SRS 2.5 / UC60).
 * SUBMITTED (chờ Admin xem lại) -> CONFIRMED / REJECTED; bị từ chối thì Thu ngân giải trình và gửi lại.
 */
public class ShiftHandover implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int scheduleId;
    private int cashierId;
    private String cashierName;
    private Date workDate;
    private String shiftName;
    private String shiftTime;
    private BigDecimal openingCash = BigDecimal.ZERO;
    private BigDecimal cashSales = BigDecimal.ZERO;
    private BigDecimal expectedCash = BigDecimal.ZERO;
    private BigDecimal actualCash = BigDecimal.ZERO;
    private BigDecimal difference = BigDecimal.ZERO;
    private String note;
    private String status;
    private Integer reviewedBy;
    private String reviewerName;
    private String reviewNote;
    private Timestamp createdAt;
    private Timestamp reviewedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getScheduleId() { return scheduleId; }
    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }

    public int getCashierId() { return cashierId; }
    public void setCashierId(int cashierId) { this.cashierId = cashierId; }

    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    public Date getWorkDate() { return workDate; }
    public void setWorkDate(Date workDate) { this.workDate = workDate; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public String getShiftTime() { return shiftTime; }
    public void setShiftTime(String shiftTime) { this.shiftTime = shiftTime; }

    public BigDecimal getOpeningCash() { return openingCash; }
    public void setOpeningCash(BigDecimal openingCash) { this.openingCash = openingCash; }

    public BigDecimal getCashSales() { return cashSales; }
    public void setCashSales(BigDecimal cashSales) { this.cashSales = cashSales; }

    public BigDecimal getExpectedCash() { return expectedCash; }
    public void setExpectedCash(BigDecimal expectedCash) { this.expectedCash = expectedCash; }

    public BigDecimal getActualCash() { return actualCash; }
    public void setActualCash(BigDecimal actualCash) { this.actualCash = actualCash; }

    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Timestamp reviewedAt) { this.reviewedAt = reviewedAt; }

    // Helper methods hiển thị
    public boolean isSubmitted() { return "SUBMITTED".equals(status); }
    public boolean isRejected() { return "REJECTED".equals(status); }

    public String getStatusLabel() {
        if ("SUBMITTED".equals(status)) return "Chờ Admin duyệt";
        if ("CONFIRMED".equals(status)) return "Đã xác nhận";
        if ("REJECTED".equals(status)) return "Bị từ chối";
        return status != null ? status : "";
    }

    /** Khớp / Thừa / Thiếu theo chênh lệch thực tế - hệ thống */
    public String getDifferenceType() {
        int sign = difference == null ? 0 : difference.signum();
        if (sign > 0) return "Thừa";
        if (sign < 0) return "Thiếu";
        return "Khớp";
    }

    public String getWorkDateDisplay() {
        return workDate == null ? "" : new SimpleDateFormat("dd/MM/yyyy").format(workDate);
    }

    public String getCreatedAtDisplay() {
        return createdAt == null ? "" : new SimpleDateFormat("HH:mm dd/MM/yyyy").format(createdAt);
    }

    public String getReviewedAtDisplay() {
        return reviewedAt == null ? "" : new SimpleDateFormat("HH:mm dd/MM/yyyy").format(reviewedAt);
    }
}
