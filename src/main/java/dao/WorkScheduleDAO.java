package dao;

import model.ShiftStat;
import model.ShopSettings;
import model.WorkSchedule;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkScheduleDAO - Thao tác dữ liệu bảng work_schedules
 */
public class WorkScheduleDAO extends BaseDAO {

    /**
     * Lấy thống kê số lượt xếp ca và trạng thái cho 3 ca làm việc (MORNING, AFTERNOON, EVENING)
     */
    public Map<String, ShiftStat> getShiftStats(ShopSettings settings) {
        Map<String, ShiftStat> stats = new LinkedHashMap<>();
        stats.put("MORNING", new ShiftStat("MORNING", ShopSettings.KEY_SHIFT_MORNING, "Ca sáng", settings.getShiftMorning()));
        stats.put("AFTERNOON", new ShiftStat("AFTERNOON", ShopSettings.KEY_SHIFT_AFTERNOON, "Ca chiều", settings.getShiftAfternoon()));
        stats.put("EVENING", new ShiftStat("EVENING", ShopSettings.KEY_SHIFT_EVENING, "Ca tối", settings.getShiftEvening()));

        String sql = "SELECT shift_name, status, COUNT(*) AS cnt FROM work_schedules GROUP BY shift_name, status";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String shiftName = rs.getString("shift_name");
                String status = rs.getString("status");
                int count = rs.getInt("cnt");

                ShiftStat stat = stats.get(shiftName);
                if (stat != null) {
                    stat.incrementStatus(status, count);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return stats;
    }

    /**
     * Lấy danh sách ca làm việc theo bộ lọc ngày, nhân viên, ca, trạng thái, vị trí
     */
    public List<WorkSchedule> getSchedules(Date fromDate, Date toDate, Integer userId,
                                           String shiftName, String status, String position) {
        List<WorkSchedule> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT ws.id, ws.user_id, ws.work_date, ws.shift_name, ws.check_in_time, ws.check_out_time, " +
                "       ws.starting_cash, ws.ending_cash_system, ws.ending_cash_actual, ws.status, ws.note, " +
                "       u.full_name AS user_name, u.phone AS user_phone, u.position AS user_position " +
                "FROM work_schedules ws " +
                "JOIN users u ON ws.user_id = u.id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (fromDate != null) {
            sql.append("AND ws.work_date >= ? ");
            params.add(fromDate);
        }
        if (toDate != null) {
            sql.append("AND ws.work_date <= ? ");
            params.add(toDate);
        }
        if (userId != null && userId > 0) {
            sql.append("AND ws.user_id = ? ");
            params.add(userId);
        }
        if (shiftName != null && !shiftName.trim().isEmpty() && !"ALL".equalsIgnoreCase(shiftName)) {
            sql.append("AND ws.shift_name = ? ");
            params.add(shiftName.trim());
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND ws.status = ? ");
            params.add(status.trim());
        }
        if (position != null && !position.trim().isEmpty() && !"ALL".equalsIgnoreCase(position)) {
            sql.append("AND u.position = ? ");
            params.add(position.trim());
        }

        sql.append("ORDER BY ws.work_date ASC, " +
                   "CASE ws.shift_name WHEN 'MORNING' THEN 1 WHEN 'AFTERNOON' THEN 2 WHEN 'EVENING' THEN 3 ELSE 4 END ASC, " +
                   "u.full_name ASC");

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WorkSchedule ws = mapResultSet(rs);
                    list.add(ws);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Lấy chi tiết 1 ca làm việc theo ID
     */
    public WorkSchedule getScheduleById(int id) {
        String sql = "SELECT ws.id, ws.user_id, ws.work_date, ws.shift_name, ws.check_in_time, ws.check_out_time, " +
                     "       ws.starting_cash, ws.ending_cash_system, ws.ending_cash_actual, ws.status, ws.note, " +
                     "       u.full_name AS user_name, u.phone AS user_phone, u.position AS user_position " +
                     "FROM work_schedules ws " +
                     "JOIN users u ON ws.user_id = u.id " +
                     "WHERE ws.id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Kiểm tra xem nhân viên đã được phân ca vào khung giờ đó chưa
     */
    public boolean isShiftExists(int userId, Date workDate, String shiftName, Integer excludeId) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM work_schedules WHERE user_id = ? AND work_date = ? AND shift_name = ? "
        );
        if (excludeId != null && excludeId > 0) {
            sql.append("AND id <> ? ");
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            ps.setInt(1, userId);
            ps.setDate(2, workDate);
            ps.setString(3, shiftName);
            if (excludeId != null && excludeId > 0) {
                ps.setInt(4, excludeId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Thêm mới ca làm việc
     */
    public boolean addSchedule(WorkSchedule ws) {
        if (isShiftExists(ws.getUserId(), ws.getWorkDate(), ws.getShiftName(), null)) {
            return false;
        }

        String sql = "INSERT INTO work_schedules (user_id, work_date, shift_name, status, note, starting_cash) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ws.getUserId());
            ps.setDate(2, ws.getWorkDate());
            ps.setString(3, ws.getShiftName());
            ps.setString(4, ws.getStatus() != null ? ws.getStatus() : "SCHEDULED");
            ps.setString(5, ws.getNote());
            if (ws.getStartingCash() != null) {
                ps.setDouble(6, ws.getStartingCash());
            } else {
                ps.setNull(6, java.sql.Types.DECIMAL);
            }

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật thông tin ca làm việc
     */
    public boolean updateSchedule(WorkSchedule ws) {
        if (isShiftExists(ws.getUserId(), ws.getWorkDate(), ws.getShiftName(), ws.getId())) {
            return false;
        }

        String sql = "UPDATE work_schedules SET user_id = ?, work_date = ?, shift_name = ?, " +
                     "       status = ?, note = ?, starting_cash = ?, ending_cash_actual = ? " +
                     "WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ws.getUserId());
            ps.setDate(2, ws.getWorkDate());
            ps.setString(3, ws.getShiftName());
            ps.setString(4, ws.getStatus());
            ps.setString(5, ws.getNote());

            if (ws.getStartingCash() != null) {
                ps.setDouble(6, ws.getStartingCash());
            } else {
                ps.setNull(6, java.sql.Types.DECIMAL);
            }

            if (ws.getEndingCashActual() != null) {
                ps.setDouble(7, ws.getEndingCashActual());
            } else {
                ps.setNull(7, java.sql.Types.DECIMAL);
            }

            ps.setInt(8, ws.getId());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Điểm danh / Đổi trạng thái nhanh cho ca làm việc
     */
    public boolean updateScheduleStatus(int id, String status) {
        String sql;
        if ("PRESENT".equalsIgnoreCase(status)) {
            sql = "UPDATE work_schedules SET status = ?, check_in_time = COALESCE(check_in_time, NOW()) WHERE id = ?";
        } else if ("COMPLETED".equalsIgnoreCase(status)) {
            sql = "UPDATE work_schedules SET status = ?, check_out_time = COALESCE(check_out_time, NOW()) WHERE id = ?";
        } else if ("SCHEDULED".equalsIgnoreCase(status)) {
            sql = "UPDATE work_schedules SET status = ?, check_in_time = NULL, check_out_time = NULL WHERE id = ?";
        } else {
            sql = "UPDATE work_schedules SET status = ? WHERE id = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, id);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa ca làm việc
     */
    public boolean deleteSchedule(int id) {
        String sql = "DELETE FROM work_schedules WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private WorkSchedule mapResultSet(ResultSet rs) throws SQLException {
        WorkSchedule ws = new WorkSchedule();
        ws.setId(rs.getInt("id"));
        ws.setUserId(rs.getInt("user_id"));
        ws.setUserName(rs.getString("user_name"));
        ws.setUserPhone(rs.getString("user_phone"));
        ws.setUserPosition(rs.getString("user_position"));
        ws.setWorkDate(rs.getDate("work_date"));
        ws.setShiftName(rs.getString("shift_name"));
        ws.setCheckInTime(rs.getTimestamp("check_in_time"));
        ws.setCheckOutTime(rs.getTimestamp("check_out_time"));

        double startingCash = rs.getDouble("starting_cash");
        if (!rs.wasNull()) ws.setStartingCash(startingCash);

        double endingCashSystem = rs.getDouble("ending_cash_system");
        if (!rs.wasNull()) ws.setEndingCashSystem(endingCashSystem);

        double endingCashActual = rs.getDouble("ending_cash_actual");
        if (!rs.wasNull()) ws.setEndingCashActual(endingCashActual);

        ws.setStatus(rs.getString("status"));
        ws.setNote(rs.getString("note"));

        return ws;
    }
}
