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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ShiftHandoverDAO - Biên bản bàn giao ca (bảng shift_handovers, SRS 2.5 / UC60).
 * Thu ngân chốt ca -> lập biên bản SUBMITTED; Admin xem lại -> CONFIRMED / REJECTED.
 */
public class ShiftHandoverDAO extends BaseDAO {

    private static final String SELECT_HANDOVER =
            "SELECT h.*, ws.work_date, s.shift_name, s.start_time, s.end_time, " +
            "       c.full_name AS cashier_name, r.full_name AS reviewer_name " +
            "FROM shift_handovers h " +
            "JOIN work_schedules ws ON ws.id = h.schedule_id " +
            "JOIN work_shifts s ON s.id = ws.shift_id " +
            "JOIN users c ON c.id = h.cashier_id " +
            "LEFT JOIN users r ON r.id = h.reviewed_by ";

    /**
     * Chốt ca + lập biên bản trong 1 transaction:
     * 1. work_schedules: PRESENT -> COMPLETED (chỉ đúng ca của thu ngân này, đang làm)
     * 2. INSERT shift_handovers (SUBMITTED) với số tiền hệ thống tính và tiền thực tế.
     * Trả về id biên bản; 0 nếu ca không còn ở trạng thái PRESENT (đã chốt / gửi 2 lần).
     */
    public int submitHandover(int scheduleId, int cashierId, BigDecimal openingCash, BigDecimal cashSales,
                              BigDecimal actualCash, String note) throws SQLException, ClassNotFoundException {
        BigDecimal expected = openingCash.add(cashSales);
        BigDecimal difference = actualCash.subtract(expected);

        String closeSql = "UPDATE work_schedules SET status = 'COMPLETED', check_out_time = NOW(), " +
                          "ending_cash_actual = ?, ending_cash_system = ?, note = ? " +
                          "WHERE id = ? AND user_id = ? AND status = 'PRESENT'";
        String insertSql = "INSERT INTO shift_handovers (schedule_id, cashier_id, opening_cash, cash_sales, " +
                           "expected_cash, actual_cash, difference, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(closeSql)) {
                    ps.setBigDecimal(1, actualCash);
                    ps.setBigDecimal(2, expected);
                    ps.setString(3, note);
                    ps.setInt(4, scheduleId);
                    ps.setInt(5, cashierId);
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

    /** Số biên bản theo trạng thái (đủ 3 trạng thái) */
    public Map<String, Integer> countByStatus() throws SQLException, ClassNotFoundException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("SUBMITTED", 0);
        counts.put("CONFIRMED", 0);
        counts.put("REJECTED", 0);
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status, COUNT(*) AS cnt FROM shift_handovers GROUP BY status");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) counts.put(rs.getString("status"), rs.getInt("cnt"));
        }
        return counts;
    }

    /**
     * Admin xác nhận / từ chối - chỉ biên bản đang chờ duyệt (điều kiện nằm trong câu UPDATE,
     * chống 2 admin duyệt cùng lúc). Trả về false nếu biên bản đã được xử lý.
     */
    public boolean review(int id, int adminId, boolean confirm, String reviewNote)
            throws SQLException, ClassNotFoundException {
        String sql = "UPDATE shift_handovers SET status = ?, reviewed_by = ?, review_note = ?, reviewed_at = NOW() " +
                     "WHERE id = ? AND status = 'SUBMITTED'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, confirm ? "CONFIRMED" : "REJECTED");
            ps.setInt(2, adminId);
            ps.setString(3, reviewNote == null || reviewNote.isEmpty() ? null : reviewNote);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Thu ngân giải trình biên bản bị từ chối và gửi lại để Admin duyệt */
    public boolean resubmit(int id, int cashierId, String explanation) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE shift_handovers SET status = 'SUBMITTED', note = ?, reviewed_by = NULL, reviewed_at = NULL " +
                     "WHERE id = ? AND cashier_id = ? AND status = 'REJECTED'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, explanation);
            ps.setInt(2, id);
            ps.setInt(3, cashierId);
            return ps.executeUpdate() > 0;
        }
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
        h.setReviewedBy(rs.getObject("reviewed_by", Integer.class));
        h.setReviewerName(rs.getString("reviewer_name"));
        h.setReviewNote(rs.getString("review_note"));
        h.setCreatedAt(rs.getTimestamp("created_at"));
        h.setReviewedAt(rs.getTimestamp("reviewed_at"));
        return h;
    }
}
