package model;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * WorkSchedule - Đại diện cho 1 ca làm việc của nhân viên (bảng work_schedules)
 */
public class WorkSchedule implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private String userName;
    private String userPhone;
    private String userPosition;
    private Date workDate;
    private String shiftName; // MORNING, AFTERNOON, EVENING
    private Timestamp checkInTime;
    private Timestamp checkOutTime;
    private Double startingCash;
    private Double endingCashSystem;
    private Double endingCashActual;
    private String status; // SCHEDULED, PRESENT, ABSENT, COMPLETED
    private String note;
    private String shiftTime; // e.g. "07:00-12:00" from shop_settings

    public WorkSchedule() {
    }

    public WorkSchedule(int id, int userId, String userName, String userPhone, String userPosition,
                        Date workDate, String shiftName, Timestamp checkInTime, Timestamp checkOutTime,
                        Double startingCash, Double endingCashSystem, Double endingCashActual,
                        String status, String note) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userPhone = userPhone;
        this.userPosition = userPosition;
        this.workDate = workDate;
        this.shiftName = shiftName;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.startingCash = startingCash;
        this.endingCashSystem = endingCashSystem;
        this.endingCashActual = endingCashActual;
        this.status = status;
        this.note = note;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getUserPosition() { return userPosition; }
    public void setUserPosition(String userPosition) { this.userPosition = userPosition; }

    public Date getWorkDate() { return workDate; }
    public void setWorkDate(Date workDate) { this.workDate = workDate; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public Timestamp getCheckInTime() { return checkInTime; }
    public void setCheckInTime(Timestamp checkInTime) { this.checkInTime = checkInTime; }

    public Timestamp getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(Timestamp checkOutTime) { this.checkOutTime = checkOutTime; }

    public Double getStartingCash() { return startingCash; }
    public void setStartingCash(Double startingCash) { this.startingCash = startingCash; }

    public Double getEndingCashSystem() { return endingCashSystem; }
    public void setEndingCashSystem(Double endingCashSystem) { this.endingCashSystem = endingCashSystem; }

    public Double getEndingCashActual() { return endingCashActual; }
    public void setEndingCashActual(Double endingCashActual) { this.endingCashActual = endingCashActual; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getShiftTime() { return shiftTime; }
    public void setShiftTime(String shiftTime) { this.shiftTime = shiftTime; }

    // Helper methods
    public String getShiftDisplayName() {
        if ("MORNING".equalsIgnoreCase(shiftName)) return "Ca sáng";
        if ("AFTERNOON".equalsIgnoreCase(shiftName)) return "Ca chiều";
        if ("EVENING".equalsIgnoreCase(shiftName)) return "Ca tối";
        return shiftName != null ? shiftName : "";
    }

    public String getStatusDisplayName() {
        if ("SCHEDULED".equalsIgnoreCase(status)) return "Đã xếp lịch";
        if ("PRESENT".equalsIgnoreCase(status)) return "Có mặt";
        if ("COMPLETED".equalsIgnoreCase(status)) return "Hoàn thành";
        if ("ABSENT".equalsIgnoreCase(status)) return "Vắng mặt";
        return status != null ? status : "";
    }

    public String getStatusBadgeClass() {
        if ("SCHEDULED".equalsIgnoreCase(status)) return "status-badge status-scheduled";
        if ("PRESENT".equalsIgnoreCase(status)) return "status-badge status-present";
        if ("COMPLETED".equalsIgnoreCase(status)) return "status-badge status-completed";
        if ("ABSENT".equalsIgnoreCase(status)) return "status-badge status-absent";
        return "status-badge";
    }

    public String getPositionDisplayName() {
        if ("BARISTA".equalsIgnoreCase(userPosition)) return "Pha chế";
        if ("CASHIER".equalsIgnoreCase(userPosition)) return "Thu ngân";
        if ("SHIPPER".equalsIgnoreCase(userPosition)) return "Giao hàng";
        if ("MANAGER".equalsIgnoreCase(userPosition)) return "Quản lý";
        return userPosition != null ? userPosition : "Nhân viên";
    }

    public String getWorkDateFormatted() {
        if (workDate == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd").format(workDate);
    }

    public String getWorkDateDisplay() {
        if (workDate == null) return "";
        return new SimpleDateFormat("dd/MM/yyyy").format(workDate);
    }

    public String getCheckInFormatted() {
        if (checkInTime == null) return "--:--";
        return new SimpleDateFormat("HH:mm").format(checkInTime);
    }

    public String getCheckOutFormatted() {
        if (checkOutTime == null) return "--:--";
        return new SimpleDateFormat("HH:mm").format(checkOutTime);
    }
}
