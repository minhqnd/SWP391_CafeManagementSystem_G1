package model;

import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** Ca qua đêm (giờ kết thúc nhỏ hơn giờ bắt đầu, VD 23:00 - 02:00) */
class WorkShiftOvernightTest {

    private static final LocalDate WEEK = LocalDate.of(2026, 11, 2);

    private static WorkShift shift(String start, String end) {
        return new WorkShift(0, WEEK, "Ca", LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    void overnightShiftHasDurationAcrossMidnight() {
        WorkShift night = shift("23:00", "02:00");
        assertTrue(night.isOvernight());
        assertEquals(180, night.getDurationMinutes());
        assertEquals("3 tiếng", night.getDurationText());
        assertEquals("23:00-02:00 hôm sau", night.getTimeRange());

        WorkShift morning = shift("07:00", "12:30");
        assertFalse(morning.isOvernight());
        assertEquals("5 tiếng 30 phút", morning.getDurationText());
        assertEquals("07:00-12:00", shift("07:00", "12:00").getTimeRange());
    }

    @Test
    void overlapAcrossMidnightAndNextDay() {
        WorkShift night = shift("23:00", "02:00");
        assertTrue(night.overlaps(shift("22:00", "23:30")), "chồng phần trước nửa đêm");
        assertTrue(night.overlaps(shift("01:00", "06:00")), "chồng sáng sớm hôm sau");
        assertTrue(shift("01:00", "06:00").overlaps(night), "đối xứng");
        assertTrue(night.overlaps(shift("22:00", "03:00")), "hai ca qua đêm");
        assertFalse(night.overlaps(shift("17:00", "23:00")), "nối tiếp lúc 23:00 không tính chồng");
        assertFalse(night.overlaps(shift("02:00", "07:00")), "nối tiếp lúc 02:00 không tính chồng");
        assertFalse(night.overlaps(shift("07:00", "12:00")));
        assertFalse(shift("07:00", "12:00").overlaps(shift("12:00", "17:00")));
        assertTrue(shift("07:00", "12:00").overlaps(shift("11:00", "13:00")));
    }

    @Test
    void attendanceTimesOfOvernightShiftFallOnTheRightDay() {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setWorkDate(Date.valueOf("2026-11-04"));
        schedule.setShiftStart(LocalTime.parse("23:00"));
        schedule.setShiftEnd(LocalTime.parse("02:00"));
        assertTrue(schedule.isOvernightShift());
        assertEquals(LocalDateTime.parse("2026-11-04T22:50"), schedule.atShiftTime(LocalTime.parse("22:50")));
        assertEquals(LocalDateTime.parse("2026-11-04T23:10"), schedule.atShiftTime(LocalTime.parse("23:10")));
        assertEquals(LocalDateTime.parse("2026-11-05T01:55"), schedule.atShiftTime(LocalTime.parse("01:55")));
        assertEquals(LocalDateTime.parse("2026-11-05T02:20"), schedule.atShiftTime(LocalTime.parse("02:20")));

        WorkSchedule day = new WorkSchedule();
        day.setWorkDate(Date.valueOf("2026-11-04"));
        day.setShiftStart(LocalTime.parse("07:00"));
        day.setShiftEnd(LocalTime.parse("12:00"));
        assertFalse(day.isOvernightShift());
        assertEquals(LocalDateTime.parse("2026-11-04T01:55"), day.atShiftTime(LocalTime.parse("01:55")));
    }
}
