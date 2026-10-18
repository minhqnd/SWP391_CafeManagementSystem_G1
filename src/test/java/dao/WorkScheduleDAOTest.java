package dao;

import model.ShiftStat;
import model.ShopSettings;
import model.User;
import model.WorkSchedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WorkScheduleDAOTest {

    private WorkScheduleDAO workScheduleDAO;
    private ShopSettingDAO shopSettingDAO;
    private UserDAO userDAO;

    @BeforeEach
    public void setUp() {
        workScheduleDAO = new WorkScheduleDAO();
        shopSettingDAO = new ShopSettingDAO();
        userDAO = new UserDAO();
    }

    @Test
    public void testShiftSettings() {
        ShopSettings settings = shopSettingDAO.getSettings();
        assertNotNull(settings);
        assertNotNull(settings.getShiftMorning());
        assertNotNull(settings.getShiftAfternoon());
        assertNotNull(settings.getShiftEvening());

        // Ghi lại đúng giờ đang lưu để không làm mất giờ ca Admin đã sửa khi chạy test trên Database thật
        boolean updated = shopSettingDAO.updateShiftHours(ShopSettings.KEY_SHIFT_MORNING, settings.getShiftMorning());
        assertTrue(updated);
    }

    @Test
    public void testGetShiftStats() {
        ShopSettings settings = shopSettingDAO.getSettings();
        Map<String, ShiftStat> stats = workScheduleDAO.getShiftStats(settings);

        assertNotNull(stats);
        assertTrue(stats.containsKey("MORNING"));
        assertTrue(stats.containsKey("AFTERNOON"));
        assertTrue(stats.containsKey("EVENING"));

        ShiftStat morning = stats.get("MORNING");
        assertEquals("Ca sáng", morning.getDisplayName());
        assertNotNull(morning.getTimeRange());
    }

    @Test
    public void testGetStaffEmployees() {
        List<User> staffList = userDAO.getStaffEmployees();
        assertNotNull(staffList);
        assertFalse(staffList.isEmpty(), "Should find staff members from seed data");
    }

    @Test
    public void testScheduleCrudCycle() {
        List<User> staff = userDAO.getStaffEmployees();
        if (staff.isEmpty()) return;

        int testUserId = staff.get(0).getId();
        Date testDate = Date.valueOf(LocalDate.now().plusDays(20)); // Future date to avoid conflicts
        String shiftName = "EVENING";

        // 1. Add
        WorkSchedule ws = new WorkSchedule();
        ws.setUserId(testUserId);
        ws.setWorkDate(testDate);
        ws.setShiftName(shiftName);
        ws.setStatus("SCHEDULED");
        ws.setNote("Test shift schedule");
        ws.setStartingCash(200000.0);

        boolean added = workScheduleDAO.addSchedule(ws);
        assertTrue(added, "Should successfully add test schedule");

        // 2. Duplicate prevention
        boolean duplicateAdded = workScheduleDAO.addSchedule(ws);
        assertFalse(duplicateAdded, "Duplicate schedule should be rejected");

        // 3. Read & Verify
        List<WorkSchedule> list = workScheduleDAO.getSchedules(testDate, testDate, testUserId, shiftName, null, null);
        assertFalse(list.isEmpty());
        WorkSchedule created = list.get(0);
        assertEquals("SCHEDULED", created.getStatus());
        assertEquals("Test shift schedule", created.getNote());

        // 4. Update status
        boolean statusUpdated = workScheduleDAO.updateScheduleStatus(created.getId(), "PRESENT");
        assertTrue(statusUpdated);

        WorkSchedule afterStatus = workScheduleDAO.getScheduleById(created.getId());
        assertEquals("PRESENT", afterStatus.getStatus());
        assertNotNull(afterStatus.getCheckInTime());

        // 5. Delete cleanup
        boolean deleted = workScheduleDAO.deleteSchedule(created.getId());
        assertTrue(deleted);

        WorkSchedule afterDelete = workScheduleDAO.getScheduleById(created.getId());
        assertNull(afterDelete);
    }
}
