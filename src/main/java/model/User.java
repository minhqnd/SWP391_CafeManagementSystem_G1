package model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String fullName;
    private String phone;
    private String email;
    private String role;
    private String position;
    private String status;
    private String birthday; // Stored as YYYY-MM-DD or standard Date string
    private String address;
    private int totalPoints;
    private String hireDate;       // Ngày vào làm dạng YYYY-MM-DD

    public User() {
    }

    public User(int id, String fullName, String phone, String email, String role, String position, String status, String birthday, String address, int totalPoints) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.position = position;
        this.status = status;
        this.birthday = birthday;
        this.address = address;
        this.totalPoints = totalPoints;
    }

    public User(int id, String fullName, String phone, String email, String role, String position, String status, String birthday, String address) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.position = position;
        this.status = status;
        this.birthday = birthday;
        this.address = address;
    }

    public User(int id, String fullName, String phone, String email, String role, String position, String status) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.position = position;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public String getHireDate() {
        return hireDate;
    }

    public void setHireDate(String hireDate) {
        this.hireDate = hireDate;
    }

    // Helper methods hiển thị cho màn Quản lý nhân viên
    public String getHireDateDisplay() {
        if (hireDate == null || hireDate.length() < 10) return "";
        return hireDate.substring(8, 10) + "/" + hireDate.substring(5, 7) + "/" + hireDate.substring(0, 4);
    }

    public String getRoleDisplayName() {
        if ("ADMIN".equalsIgnoreCase(role)) return "Quản trị";
        if ("STAFF".equalsIgnoreCase(role)) return "Nhân viên";
        if ("CUSTOMER".equalsIgnoreCase(role)) return "Khách hàng";
        return role != null ? role : "";
    }

    public String getPositionDisplayName() {
        if ("MANAGER".equalsIgnoreCase(position)) return "Quản lý nhân viên";
        if ("BARISTA".equalsIgnoreCase(position)) return "Pha chế";
        if ("CASHIER".equalsIgnoreCase(position)) return "Thu ngân";
        if ("SHIPPER".equalsIgnoreCase(position)) return "Giao hàng";
        return position != null ? position : "";
    }

    public String getStatusDisplayName() {
        if ("ACTIVE".equalsIgnoreCase(status)) return "Hoạt động";
        if ("INACTIVE".equalsIgnoreCase(status)) return "Ngừng hoạt động";
        if ("LOCKED".equalsIgnoreCase(status)) return "Đã khóa";
        return status != null ? status : "";
    }
}
