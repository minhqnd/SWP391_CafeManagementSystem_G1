package model;

import java.io.Serializable;

/**
 * ShiftStat - Thống kê số lượng lượt xếp ca và trạng thái theo từng ca làm việc
 * Dùng cho các truy vấn thống kê lịch theo từng ca làm việc.
 */
public class ShiftStat implements Serializable {
    private static final long serialVersionUID = 1L;

    private String shiftName;     // MORNING, AFTERNOON, EVENING
    private String shiftKey;      // shift_morning, shift_afternoon, shift_evening
    private String displayName;   // Ca sáng, Ca chiều, Ca tối
    private String timeRange;     // 07:00-12:00
    private int totalCount;       // Tổng lượt xếp ca
    private int scheduledCount;   // Đã xếp lịch
    private int presentCount;     // Có mặt
    private int completedCount;   // Hoàn thành
    private int absentCount;      // Vắng

    public ShiftStat() {
    }

    public ShiftStat(String shiftName, String shiftKey, String displayName, String timeRange) {
        this.shiftName = shiftName;
        this.shiftKey = shiftKey;
        this.displayName = displayName;
        this.timeRange = timeRange;
        this.totalCount = 0;
        this.scheduledCount = 0;
        this.presentCount = 0;
        this.completedCount = 0;
        this.absentCount = 0;
    }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public String getShiftKey() { return shiftKey; }
    public void setShiftKey(String shiftKey) { this.shiftKey = shiftKey; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public int getScheduledCount() { return scheduledCount; }
    public void setScheduledCount(int scheduledCount) { this.scheduledCount = scheduledCount; }

    public int getPresentCount() { return presentCount; }
    public void setPresentCount(int presentCount) { this.presentCount = presentCount; }

    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }

    public int getAbsentCount() { return absentCount; }
    public void setAbsentCount(int absentCount) { this.absentCount = absentCount; }

    public void incrementStatus(String status, int count) {
        this.totalCount += count;
        if ("SCHEDULED".equalsIgnoreCase(status)) {
            this.scheduledCount += count;
        } else if ("PRESENT".equalsIgnoreCase(status)) {
            this.presentCount += count;
        } else if ("COMPLETED".equalsIgnoreCase(status)) {
            this.completedCount += count;
        } else if ("ABSENT".equalsIgnoreCase(status)) {
            this.absentCount += count;
        }
    }
}
