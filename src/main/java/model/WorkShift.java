package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * WorkShift - Một khung ca làm việc của một tuần (bảng work_shifts).
 * Mỗi tuần (weekStart = Thứ 2) có bộ ca riêng, sửa tuần này không ảnh hưởng tuần khác.
 */
public class WorkShift implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private int id;
    private LocalDate weekStart;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private int scheduleCount; // Số lượt đã xếp lịch vào ca (chặn xoá ca đang được dùng)

    public WorkShift() {
    }

    public WorkShift(int id, LocalDate weekStart, String shiftName, LocalTime startTime, LocalTime endTime) {
        this.id = id;
        this.weekStart = weekStart;
        this.shiftName = shiftName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public int getScheduleCount() { return scheduleCount; }
    public void setScheduleCount(int scheduleCount) { this.scheduleCount = scheduleCount; }

    // Helper methods hiển thị
    public String getStartTimeText() {
        return startTime == null ? "" : startTime.format(TIME_FORMAT);
    }

    public String getEndTimeText() {
        return endTime == null ? "" : endTime.format(TIME_FORMAT);
    }

    /** VD "07:00-12:00" */
    public String getTimeRange() {
        return getStartTimeText() + "-" + getEndTimeText();
    }

    /** VD "Ca sáng (07:00-12:00)" */
    public String getLabel() {
        return shiftName + " (" + getTimeRange() + ")";
    }
}
