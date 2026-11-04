package dao;

import model.WorkShift;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * WorkShiftDAO - Khung giờ ca theo từng tuần (bảng work_shifts) cho màn /admin/shifts.
 * Mọi thao tác chỉ tác động tới đúng tuần (week_start) được chọn.
 */
public class WorkShiftDAO extends BaseDAO {

    private static final String SELECT_SHIFT =
            "SELECT s.id, s.week_start, s.shift_name, s.start_time, s.end_time, "
            + "(SELECT COUNT(*) FROM work_schedules ws WHERE ws.shift_id = s.id) AS schedule_count "
            + "FROM work_shifts s ";

    /** Các ca của một tuần, sắp theo giờ bắt đầu */
    public List<WorkShift> getShiftsByWeek(LocalDate weekStart) throws SQLException, ClassNotFoundException {
        List<WorkShift> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_SHIFT
                     + "WHERE s.week_start = ? ORDER BY s.start_time, s.shift_name")) {
            ps.setDate(1, Date.valueOf(weekStart));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public WorkShift getById(int id) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_SHIFT + "WHERE s.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Tên ca đã có trong tuần chưa (không phân biệt hoa thường) */
    public boolean nameExists(LocalDate weekStart, String shiftName, int excludeId)
            throws SQLException, ClassNotFoundException {
        String sql = "SELECT 1 FROM work_shifts WHERE week_start = ? AND LOWER(shift_name) = LOWER(?) AND id <> ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(weekStart));
            ps.setString(2, shiftName);
            ps.setInt(3, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Ca khác trong cùng tuần bị chồng giờ với ca start - end; null nếu không có.
     * So trong Java (WorkShift.overlaps) để tính đúng cả ca qua đêm (VD 23:00 - 02:00).
     */
    public WorkShift findOverlap(LocalDate weekStart, LocalTime start, LocalTime end, int excludeId)
            throws SQLException, ClassNotFoundException {
        WorkShift candidate = new WorkShift(excludeId, weekStart, "", start, end);
        for (WorkShift other : getShiftsByWeek(weekStart)) {
            if (other.getId() != excludeId && candidate.overlaps(other)) {
                return other;
            }
        }
        return null;
    }

    public int insert(WorkShift shift) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO work_shifts (week_start, shift_name, start_time, end_time) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(shift.getWeekStart()));
            ps.setString(2, shift.getShiftName());
            ps.setTime(3, Time.valueOf(shift.getStartTime()));
            ps.setTime(4, Time.valueOf(shift.getEndTime()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    /** Chỉ sửa tên / giờ của đúng ca (thuộc đúng tuần), tuần khác không bị ảnh hưởng */
    public boolean update(WorkShift shift) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE work_shifts SET shift_name = ?, start_time = ?, end_time = ? WHERE id = ? AND week_start = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, shift.getShiftName());
            ps.setTime(2, Time.valueOf(shift.getStartTime()));
            ps.setTime(3, Time.valueOf(shift.getEndTime()));
            ps.setInt(4, shift.getId());
            ps.setDate(5, Date.valueOf(shift.getWeekStart()));
            return ps.executeUpdate() > 0;
        }
    }

    /** Xoá ca chưa có lịch phân công (điều kiện nằm ngay trong câu DELETE) */
    public boolean deleteIfUnused(int id) throws SQLException, ClassNotFoundException {
        String sql = "DELETE FROM work_shifts WHERE id = ? "
                + "AND NOT EXISTS (SELECT 1 FROM work_schedules ws WHERE ws.shift_id = ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Tuần gần nhất trước weekStart đã có khung ca (để gợi ý sao chép); null nếu chưa có */
    public LocalDate findLatestWeekBefore(LocalDate weekStart) throws SQLException, ClassNotFoundException {
        String sql = "SELECT MAX(week_start) FROM work_shifts WHERE week_start < ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(weekStart));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getDate(1) != null) {
                    return rs.getDate(1).toLocalDate();
                }
            }
        }
        return null;
    }

    /** Sao chép toàn bộ ca từ tuần nguồn sang tuần đích (tuần đích phải đang trống). Trả về số ca đã chép. */
    public int copyWeek(LocalDate fromWeek, LocalDate toWeek) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO work_shifts (week_start, shift_name, start_time, end_time) "
                + "SELECT ?, shift_name, start_time, end_time FROM work_shifts WHERE week_start = ? "
                + "AND NOT EXISTS (SELECT 1 FROM (SELECT id FROM work_shifts WHERE week_start = ?) existing)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(toWeek));
            ps.setDate(2, Date.valueOf(fromWeek));
            ps.setDate(3, Date.valueOf(toWeek));
            return ps.executeUpdate();
        }
    }

    private WorkShift map(ResultSet rs) throws SQLException {
        WorkShift shift = new WorkShift(
                rs.getInt("id"),
                rs.getDate("week_start").toLocalDate(),
                rs.getString("shift_name"),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime());
        shift.setScheduleCount(rs.getInt("schedule_count"));
        return shift;
    }
}
