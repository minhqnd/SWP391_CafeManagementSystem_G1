package dao;

import model.ShiftHandoverSummary;
import model.WorkSchedule;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * WorkScheduleDAO - Thao tác dữ liệu bảng work_schedules.
 * Mỗi lịch gắn với 1 ca (shift_id) của bảng work_shifts - tên và giờ ca lấy theo đúng tuần của lịch.
 */
public class WorkScheduleDAO extends BaseDAO {

    /** Cột chung: lịch + nhân viên + ca của tuần đó */
    private static final String SELECT_SCHEDULE =
            "SELECT ws.id, ws.user_id, ws.work_date, ws.shift_id, ws.check_in_time, ws.check_out_time, " +
            "       ws.status, ws.note, " +
            "       s.shift_name, s.start_time AS shift_start, s.end_time AS shift_end, " +
            "       u.full_name AS user_name, u.phone AS user_phone, u.position AS user_position " +
            "FROM work_schedules ws " +
            "JOIN work_shifts s ON s.id = ws.shift_id " +
            "JOIN users u ON ws.user_id = u.id ";

    /**
     * Lấy danh sách ca làm việc theo bộ lọc ngày, nhân viên, ca (shift_id), trạng thái, vị trí
     */
    public List<WorkSchedule> getSchedules(Date fromDate, Date toDate, Integer userId,
                                           Integer shiftId, String status, String position) {
        List<WorkSchedule> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_SCHEDULE + "WHERE 1=1 ");

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
        if (shiftId != null && shiftId > 0) {
            sql.append("AND ws.shift_id = ? ");
            params.add(shiftId);
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND ws.status = ? ");
            params.add(status.trim());
        }
        if (position != null && !position.trim().isEmpty() && !"ALL".equalsIgnoreCase(position)) {
            sql.append("AND u.position = ? ");
            params.add(position.trim());
        }

        sql.append("ORDER BY ws.work_date ASC, s.start_time ASC, u.full_name ASC");

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
        String sql = SELECT_SCHEDULE + "WHERE ws.id = ?";

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
    public boolean isShiftExists(int userId, Date workDate, int shiftId, Integer excludeId) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM work_schedules WHERE user_id = ? AND work_date = ? AND shift_id = ? "
        );
        if (excludeId != null && excludeId > 0) {
            sql.append("AND id <> ? ");
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            ps.setInt(1, userId);
            ps.setDate(2, workDate);
            ps.setInt(3, shiftId);
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
        if (isShiftExists(ws.getUserId(), ws.getWorkDate(), ws.getShiftId(), null)) {
            return false;
        }

        String sql = "INSERT INTO work_schedules (user_id, work_date, shift_id, status, note) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ws.getUserId());
            ps.setDate(2, ws.getWorkDate());
            ps.setInt(3, ws.getShiftId());
            ps.setString(4, ws.getStatus() != null ? ws.getStatus() : "SCHEDULED");
            ps.setString(5, ws.getNote());

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
        if (isShiftExists(ws.getUserId(), ws.getWorkDate(), ws.getShiftId(), ws.getId())) {
            return false;
        }

        String sql = "UPDATE work_schedules SET user_id = ?, work_date = ?, shift_id = ?, " +
                     "       status = ?, note = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ws.getUserId());
            ps.setDate(2, ws.getWorkDate());
            ps.setInt(3, ws.getShiftId());
            ps.setString(4, ws.getStatus());
            ps.setString(5, ws.getNote());
            ps.setInt(6, ws.getId());

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
     * Cập nhật dữ liệu chấm công do quản trị viên xác nhận.
     */
    public boolean updateAttendance(int id, Timestamp checkInTime, Timestamp checkOutTime,
                                    String status, String note) {
        String sql = "UPDATE work_schedules " +
                     "SET check_in_time = ?, check_out_time = ?, status = ?, note = ? " +
                     "WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (checkInTime != null) {
                ps.setTimestamp(1, checkInTime);
            } else {
                ps.setNull(1, java.sql.Types.TIMESTAMP);
            }

            if (checkOutTime != null) {
                ps.setTimestamp(2, checkOutTime);
            } else {
                ps.setNull(2, java.sql.Types.TIMESTAMP);
            }

            ps.setString(3, status);
            ps.setString(4, note);
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tổng hợp các giao dịch thành công do chính thu ngân xử lý trong thời gian ca.
     */
    public ShiftHandoverSummary getShiftHandoverSummary(WorkSchedule schedule, Timestamp fallbackEndTime) {
        ShiftHandoverSummary summary = new ShiftHandoverSummary();
        summary.setSchedule(schedule);

        if (schedule == null) {
            return summary;
        }

        Timestamp startTime = schedule.getCheckInTime();
        if (startTime == null && schedule.getWorkDate() != null) {
            startTime = Timestamp.valueOf(schedule.getWorkDate().toLocalDate().atStartOfDay());
        }
        Timestamp endTime = schedule.getCheckOutTime() != null
                ? schedule.getCheckOutTime()
                : fallbackEndTime;
        if (endTime == null && schedule.getWorkDate() != null) {
            endTime = Timestamp.valueOf(schedule.getWorkDate().toLocalDate().atTime(23, 59, 59));
        }
        if (schedule.getCheckOutTime() == null && schedule.getWorkDate() != null) {
            java.time.LocalDate wDate = schedule.getWorkDate().toLocalDate();
            if (wDate.isBefore(java.time.LocalDate.now())) {
                endTime = Timestamp.valueOf(wDate.atTime(23, 59, 59));
            }
        }
        if (startTime == null || endTime == null) {
            return summary;
        }

        String sql = "SELECT COUNT(DISTINCT o.id) AS order_count, " +
                     "       COALESCE(SUM(CASE WHEN p.payment_method = 'CASH' THEN 1 ELSE 0 END), 0) AS cash_count, " +
                     "       COALESCE(SUM(CASE WHEN p.payment_method = 'CASH' THEN p.amount ELSE 0 END), 0) AS cash_sales, " +
                     "       COALESCE(SUM(CASE WHEN p.payment_method = 'BANK_TRANSFER' THEN 1 ELSE 0 END), 0) AS bank_count, " +
                     "       COALESCE(SUM(CASE WHEN p.payment_method = 'BANK_TRANSFER' THEN p.amount ELSE 0 END), 0) AS bank_sales, " +
                     "       COALESCE(SUM(CASE WHEN p.payment_method = 'VNPAY' THEN 1 ELSE 0 END), 0) AS vnpay_count, " +
                     "       COALESCE(SUM(CASE WHEN p.payment_method = 'VNPAY' THEN p.amount ELSE 0 END), 0) AS vnpay_sales, " +
                     "       COALESCE(SUM(CASE WHEN p.payment_method = 'MOMO' THEN 1 ELSE 0 END), 0) AS momo_count, " +
                     "       COALESCE(SUM(CASE WHEN p.payment_method = 'MOMO' THEN p.amount ELSE 0 END), 0) AS momo_sales, " +
                     "       COALESCE(SUM(CASE WHEN p.payment_method <> 'CASH' THEN p.amount ELSE 0 END), 0) AS non_cash_sales, " +
                     "       COALESCE(SUM(p.amount), 0) AS total_sales " +
                     "FROM orders o " +
                     "JOIN payments p ON p.order_id = o.id " +
                     "WHERE o.cashier_id = ? AND o.status <> 'CANCELLED' " +
                     "  AND p.status = 'SUCCESS' AND p.paid_at >= ? AND p.paid_at <= ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, schedule.getUserId());
            ps.setTimestamp(2, startTime);
            ps.setTimestamp(3, endTime);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    summary.setSuccessfulOrderCount(rs.getInt("order_count"));
                    summary.setCashCount(rs.getInt("cash_count"));
                    summary.setCashSales(rs.getBigDecimal("cash_sales"));
                    summary.setBankCount(rs.getInt("bank_count"));
                    summary.setBankSales(rs.getBigDecimal("bank_sales"));
                    summary.setVnpayCount(rs.getInt("vnpay_count"));
                    summary.setVnpaySales(rs.getBigDecimal("vnpay_sales"));
                    summary.setMomoCount(rs.getInt("momo_count"));
                    summary.setMomoSales(rs.getBigDecimal("momo_sales"));
                    summary.setNonCashSales(rs.getBigDecimal("non_cash_sales"));
                    summary.setTotalSales(rs.getBigDecimal("total_sales"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return summary;
    }

    /**
     * Lấy các ca làm việc gần đây của 1 nhân viên
     */
    public List<WorkSchedule> getRecentSchedulesForUser(int userId, int limit) {
        List<WorkSchedule> list = new java.util.ArrayList<>();
        String sql = SELECT_SCHEDULE +
                     "WHERE ws.user_id = ? " +
                     "ORDER BY ws.work_date DESC, ws.id DESC LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
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
        ws.setShiftId(rs.getInt("shift_id"));
        ws.setShiftName(rs.getString("shift_name"));
        Time shiftStart = rs.getTime("shift_start");
        Time shiftEnd = rs.getTime("shift_end");
        if (shiftStart != null && shiftEnd != null) {
            ws.setShiftStart(shiftStart.toLocalTime());
            ws.setShiftTime(shiftStart.toString().substring(0, 5) + "-" + shiftEnd.toString().substring(0, 5));
        }
        ws.setCheckInTime(rs.getTimestamp("check_in_time"));
        ws.setCheckOutTime(rs.getTimestamp("check_out_time"));

        ws.setStatus(rs.getString("status"));
        ws.setNote(rs.getString("note"));

        return ws;
    }
}
