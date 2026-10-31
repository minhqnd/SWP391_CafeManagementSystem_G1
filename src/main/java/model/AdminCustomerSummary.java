package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Dữ liệu tổng hợp cho màn hình quản lý khách hàng của Admin.
 */
public class AdminCustomerSummary implements Serializable {

    private int id;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate birthday;
    private String address;
    private String status;
    private int totalPoints;
    private int orderCount;
    private double totalSpent;
    private LocalDateTime createdAt;

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

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCustomerCode() {
        return String.format("KH%04d", id);
    }

    public String getBirthdayText() {
        return birthday == null ? "—" : birthday.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getCreatedAtText() {
        return createdAt == null ? "—" : createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
