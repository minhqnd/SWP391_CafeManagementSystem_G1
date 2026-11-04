package dao;

import model.ShiftHandover;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

/**
 * ShiftHandoverDAO - Biên bản bàn giao ca (bảng shift_handovers, SRS 2.5 / UC60).
 * Thu ngân chốt ca -> hoàn tất biên bản ngay, Admin chỉ xem lịch sử.
 */
public class ShiftHandoverDAO extends BaseDAO {

    private static final String SELECT_HANDOVER =
            "SELECT h.*, ws.work_date, s.shift_name, s.start_time, s.end_time, " +
            "       c.full_name AS cashier_name " +
            "FROM shift_handovers h " +
            "JOIN work_schedules ws ON ws.id = h.schedule_id " +
            "JOIN work_shifts s ON s.id = ws.shift_id " +
            "JOIN users c ON c.id = h.cashier_id ";

    /**
     * Chốt ca + lập biên bản trong 1 transaction:
     * 1. work_schedules: PRESENT -> COMPLETED (chỉ đúng ca của thu ngân này, đang làm)
     * 2. INSERT shift_handovers (CONFIRMED) với số tiền hệ thống tính và tiền thực tế.
     * Trả về id biên bản; 0 nếu ca không còn ở trạng thái PRESENT (đã chốt / gửi 2 lần).
     */
    public int submitHandover(int scheduleId, int cashierId, BigDecimal openingCash, BigDecimal cashSales,
                              BigDecimal actualCash, String note) throws SQLException, ClassNotFoundException {
        BigDecimal expected = openingCash.add(cashSales);
        BigDecimal difference = actualCash.subtract(expected);

        String closeSql = "UPDATE work_schedules SET status = 'COMPLETED', check_out_time = NOW(), note = ? " +
                          "WHERE id = ? AND user_id = ? AND status = 'PRESENT'";
        String insertSql = "INSERT INTO shift_handovers (schedule_id, cashier_id, opening_cash, cash_sales, " +
                           "expected_cash, actual_cash, difference, note, status) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'CONFIRMED')";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(closeSql)) {
                    ps.setString(1, note);
                    ps.setInt(2, scheduleId);
                    ps.setInt(3, cashierId);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return 0;
                    }
                }
                int id;
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, scheduleId);
                    ps.setInt(2, cashierId);
                    ps.setBigDecimal(3, openingCash);
                    ps.setBigDecimal(4, cashSales);
                    ps.setBigDecimal(5, expected);
                    ps.setBigDecimal(6, actualCash);
                    ps.setBigDecimal(7, difference);
                    ps.setString(8, note == null || note.isEmpty() ? null : note);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        id = keys.next() ? keys.getInt(1) : 0;
                    }
                }
                conn.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Biên bản của 1 ca (null nếu ca chưa chốt) */
    public ShiftHandover getBySchedule(int scheduleId) throws SQLException, ClassNotFoundException {
        return findOne(SELECT_HANDOVER + "WHERE h.schedule_id = ?", scheduleId);
    }

    public ShiftHandover getById(int id) throws SQLException, ClassNotFoundException {
        return findOne(SELECT_HANDOVER + "WHERE h.id = ?", id);
    }

    /**
     * Finds the latest completed handover before the supplied scheduled shift.
     * Its actual cash is the physical cash carried into the next cashier shift.
     */
    public ShiftHandover getPreviousHandoverForSchedule(int scheduleId)
            throws SQLException, ClassNotFoundException {
        String sql = "SELECT h.*, previous_ws.work_date, previous_shift.shift_name, "
                + "previous_shift.start_time, previous_shift.end_time, cashier.full_name AS cashier_name "
                + "FROM work_schedules current_ws "
                + "JOIN work_shifts current_shift ON current_shift.id = current_ws.shift_id "
                + "JOIN shift_handovers h ON h.status = 'CONFIRMED' "
                + "JOIN work_schedules previous_ws ON previous_ws.id = h.schedule_id "
                + "JOIN work_shifts previous_shift ON previous_shift.id = previous_ws.shift_id "
                + "JOIN users cashier ON cashier.id = h.cashier_id "
                + "WHERE current_ws.id = ? AND ("
                + "previous_ws.work_date < current_ws.work_date OR ("
                + "previous_ws.work_date = current_ws.work_date "
                // ca qua đêm (end < start) kết thúc sáng hôm sau nên không đứng trước ca nào cùng ngày
                + "AND previous_shift.end_time > previous_shift.start_time "
                + "AND previous_shift.end_time <= current_shift.start_time)) "
                + "ORDER BY previous_ws.work_date DESC, (previous_shift.end_time < previous_shift.start_time) DESC, "
                + "previous_shift.end_time DESC, "
                + "h.created_at DESC, h.id DESC LIMIT 1";
        return findOne(sql, scheduleId);
    }

    /** Danh sách biên bản cho Admin, mới nhất lên đầu; status = null thì lấy tất cả */
    public List<ShiftHandover> getHandovers(String status) throws SQLException, ClassNotFoundException {
        String sql = SELECT_HANDOVER + (status != null ? "WHERE h.status = ? " : "")
                + "ORDER BY h.created_at DESC, h.id DESC";
        List<ShiftHandover> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (status != null) ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private ShiftHandover findOne(String sql, int param) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private ShiftHandover map(ResultSet rs) throws SQLException {
        ShiftHandover h = new ShiftHandover();
        h.setId(rs.getInt("id"));
        h.setScheduleId(rs.getInt("schedule_id"));
        h.setCashierId(rs.getInt("cashier_id"));
        h.setCashierName(rs.getString("cashier_name"));
        h.setWorkDate(rs.getDate("work_date"));
        h.setShiftName(rs.getString("shift_name"));
        Time start = rs.getTime("start_time");
        Time end = rs.getTime("end_time");
        if (start != null && end != null) {
            h.setShiftTime(start.toString().substring(0, 5) + "-" + end.toString().substring(0, 5));
        }
        h.setOpeningCash(rs.getBigDecimal("opening_cash"));
        h.setCashSales(rs.getBigDecimal("cash_sales"));
        h.setExpectedCash(rs.getBigDecimal("expected_cash"));
        h.setActualCash(rs.getBigDecimal("actual_cash"));
        h.setDifference(rs.getBigDecimal("difference"));
        h.setNote(rs.getString("note"));
        h.setStatus(rs.getString("status"));
        h.setCreatedAt(rs.getTimestamp("created_at"));
        return h;
    }
}
