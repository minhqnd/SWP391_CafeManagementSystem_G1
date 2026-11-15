package dao;

import model.User;
import model.WorkSchedule;
import model.WorkShift;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("database")
public class WorkScheduleDAOTest {

    private WorkScheduleDAO workScheduleDAO;
    private WorkShiftDAO workShiftDAO;
    private UserDAO userDAO;

    @BeforeEach
    public void setUp() {
        workScheduleDAO = new WorkScheduleDAO();
        workShiftDAO = new WorkShiftDAO();
        userDAO = new UserDAO();
    }

    @Test
    public void testSeedWeekHasShifts() throws Exception {
        // Dữ liệu mẫu: tuần 28/09/2026 có 3 ca, giờ Ca sáng 07:00-12:00
        List<WorkShift> shifts = workShiftDAO.getShiftsByWeek(LocalDate.of(2026, 9, 28));
        assertEquals(3, shifts.size());
        assertEquals("07:00-12:00", shifts.get(0).getTimeRange());
    }

    @Test
    public void testShiftHoursAreStoredPerWeek() throws Exception {
        // Tuần 26/10 đổi giờ Ca chiều nhưng tuần 19/10 vẫn giữ giờ cũ
        List<WorkShift> week1 = workShiftDAO.getShiftsByWeek(LocalDate.of(2026, 10, 19));
        List<WorkShift> week2 = workShiftDAO.getShiftsByWeek(LocalDate.of(2026, 10, 26));
        assertEquals("12:00-17:00", week1.get(1).getTimeRange());
        assertEquals("12:30-17:30", week2.get(1).getTimeRange());
    }

    @Test
    public void testGetStaffEmployees() {
        List<User> staffList = userDAO.getStaffEmployees();
        assertNotNull(staffList);
        assertFalse(staffList.isEmpty(), "Should find staff members from seed data");
        assertTrue(staffList.stream().allMatch(user -> "STAFF".equals(user.getRole())),
                "Schedule employee list must not include ADMIN accounts");
        assertNull(userDAO.getActiveStaffById(1), "ADMIN accounts must not be assignable to shifts");
    }

    @Test
    public void testAdminScheduleIsRejectedByDao() throws Exception {
        LocalDate workDay = LocalDate.of(2026, 10, 29);
        List<WorkShift> shifts = workShiftDAO.getShiftsByWeek(
                workDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        assertFalse(shifts.isEmpty(), "Seed week should contain work shifts");

        WorkSchedule adminSchedule = new WorkSchedule();
        adminSchedule.setUserId(1);
        adminSchedule.setWorkDate(Date.valueOf(workDay));
        adminSchedule.setShiftId(shifts.get(0).getId());
        adminSchedule.setStatus("SCHEDULED");

        assertFalse(workScheduleDAO.addSchedule(adminSchedule),
                "DAO must reject shift assignments for ADMIN accounts");
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="cafe.database.writeTests", matches="true")
    public void testScheduleCrudCycle() throws Exception {
        List<User> staff = userDAO.getStaffEmployees();
        if (staff.isEmpty()) return;

        int testUserId = staff.get(0).getId();
        LocalDate testDay = LocalDate.now().plusYears(5); // Tuần chưa có dữ liệu thật
        Date testDate = Date.valueOf(testDay);
        LocalDate weekStart = testDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 0. Khung ca riêng cho tuần test
        int shiftId = workShiftDAO.insert(new WorkShift(0, weekStart, "Ca test", LocalTime.of(6, 0), LocalTime.of(7, 0)));
        assertTrue(shiftId > 0);

        try {
            // 1. Add
            WorkSchedule ws = new WorkSchedule();
            ws.setUserId(testUserId);
            ws.setWorkDate(testDate);
            ws.setShiftId(shiftId);
            ws.setStatus("SCHEDULED");
            ws.setNote("Test shift schedule");

            assertTrue(workScheduleDAO.addSchedule(ws), "Should successfully add test schedule");

            // 2. Duplicate prevention
            assertFalse(workScheduleDAO.addSchedule(ws), "Duplicate schedule should be rejected");

            // 3. Read & Verify (tên + giờ ca lấy từ work_shifts)
            List<WorkSchedule> list = workScheduleDAO.getSchedules(testDate, testDate, testUserId, shiftId, null, null);
            assertEquals(1, list.size());
            WorkSchedule created = list.get(0);
            assertEquals("Ca test", created.getShiftName());
            assertEquals("06:00-07:00", created.getShiftTime());

            // 4. Ca đang có lịch thì không xoá được
            assertFalse(workShiftDAO.deleteIfUnused(shiftId));

            // 5. Update status
            assertTrue(workScheduleDAO.updateScheduleStatus(created.getId(), "PRESENT"));
            WorkSchedule afterStatus = workScheduleDAO.getScheduleById(created.getId());
            assertEquals("PRESENT", afterStatus.getStatus());
            assertNotNull(afterStatus.getCheckInTime());

            // 6. Delete schedule
            assertTrue(workScheduleDAO.deleteSchedule(created.getId()));
            assertNull(workScheduleDAO.getScheduleById(created.getId()));
        } finally {
            // Dọn khung ca test
            assertTrue(workShiftDAO.deleteIfUnused(shiftId));
        }
    }
}
