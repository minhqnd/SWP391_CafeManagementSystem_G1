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

    /**
     * Ca qua đêm: giờ kết thúc nhỏ hơn giờ bắt đầu nghĩa là ca kết thúc vào sáng hôm sau
     * (VD 23:00 - 02:00). Ca luôn tính theo ngày bắt đầu (work_date của lịch phân ca).
     */
    public boolean isOvernight() {
        return isOvernight(startTime, endTime);
    }

    public static boolean isOvernight(LocalTime start, LocalTime end) {
        return start != null && end != null && end.isBefore(start);
    }

    /** Số phút của ca (ca qua đêm tính sang ngày hôm sau), VD 23:00 - 02:00 = 180 */
    public int getDurationMinutes() {
        if (startTime == null || endTime == null) return 0;
        int minutes = toMinutes(endTime) - toMinutes(startTime);
        return minutes <= 0 ? minutes + MINUTES_PER_DAY : minutes;
    }

    /** VD "5 tiếng", "4 tiếng 30 phút", "45 phút" */
    public String getDurationText() {
        int minutes = getDurationMinutes();
        int hours = minutes / 60;
        int rest = minutes % 60;
        if (hours == 0) return rest + " phút";
        return rest == 0 ? hours + " tiếng" : hours + " tiếng " + rest + " phút";
    }

    /** VD "07:00-12:00", ca qua đêm "23:00-02:00 hôm sau" */
    public String getTimeRange() {
        return getStartTimeText() + "-" + getEndTimeText() + (isOvernight() ? " hôm sau" : "");
    }

    /** VD "Ca sáng (07:00-12:00)" */
    public String getLabel() {
        return shiftName + " (" + getTimeRange() + ")";
    }

    /**
     * Hai ca có chồng giờ không. Khung ca áp dụng cho mọi ngày trong tuần, nên ca qua đêm
     * (VD 23:00 - 02:00) còn chồng với ca sáng sớm của ngày hôm sau (VD 01:00 - 06:00).
     */
    public boolean overlaps(WorkShift other) {
        int start = toMinutes(startTime);
        int end = start + getDurationMinutes();
        int otherStart = toMinutes(other.startTime);
        int otherEnd = otherStart + other.getDurationMinutes();
        for (int shift = -MINUTES_PER_DAY; shift <= MINUTES_PER_DAY; shift += MINUTES_PER_DAY) {
            if (start < otherEnd + shift && otherStart + shift < end) return true;
        }
        return false;
    }

    private static final int MINUTES_PER_DAY = 24 * 60;

    private static int toMinutes(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }
}
