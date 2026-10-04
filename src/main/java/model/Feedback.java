package model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Model Feedback / Customer Review - Phản ánh & Khiếu nại của khách hàng
 * Ánh xạ trực tiếp từ bảng `customer_reviews` trong database `aura_cafe.sql`
 *
 * CREATE TABLE customer_reviews (
 *     id INT AUTO_INCREMENT PRIMARY KEY,
 *     customer_id INT NOT NULL,
 *     order_id INT NOT NULL,
 *     issue_type ENUM('RATING', 'COMPLAINT') NOT NULL,
 *     rating INT NULL,
 *     content TEXT NOT NULL,
 *     status ENUM('PENDING', 'RESOLVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
 *     admin_response TEXT NULL,
 *     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *     CONSTRAINT fk_review_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
 *     CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
 *     CONSTRAINT chk_review_type CHECK (((issue_type = 'RATING' AND rating BETWEEN 1 AND 5) OR (issue_type = 'COMPLAINT' AND rating IS NULL)))
 * );
 */
public class Feedback implements Serializable {
    private static final long serialVersionUID = 1L;

    // Các trường tương ứng 1-1 với cột trong bảng customer_reviews
    private int id;
    private String ticketCode;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private int orderId;
    private String orderCode;
    
    // Core database field: issue_type ENUM('RATING', 'COMPLAINT')
    private String issueType = "COMPLAINT";
    
    // Core database field: rating INT NULL (1..5 khi RATING, NULL khi COMPLAINT)
    private int rating = 0;
    
    // Core database field: content TEXT NOT NULL
    private String content;
    
    // Core database field: status ENUM('PENDING', 'RESOLVED', 'REJECTED')
    private String status = "PENDING";
    
    // Core database field: admin_response TEXT NULL
    private String adminResponse;
    
    // Core database field: created_at DATETIME
    private Timestamp createdAt;

    public Feedback() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTicketCode() {
        if (ticketCode != null && !ticketCode.trim().isEmpty()) {
            return ticketCode;
        }
        return "#CR-" + String.format("%04d", id);
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getOrderCode() {
        if (orderCode != null && !orderCode.trim().isEmpty()) {
            return orderCode;
        }
        return orderId > 0 ? ("AURA-00" + orderId) : "";
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getIssueType() {
        return issueType != null ? issueType : "COMPLAINT";
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status != null ? status : "PENDING";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusLabel() {
        if ("RESOLVED".equalsIgnoreCase(status)) {
            return "Đã giải quyết";
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            return "Từ chối";
        }
        return "Đang chờ xử lý";
    }

    public String getAdminResponse() {
        return adminResponse;
    }

    public void setAdminResponse(String adminResponse) {
        this.adminResponse = adminResponse;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public boolean isResolved() {
        return "RESOLVED".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(status);
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) {
            return "Vừa xong - Hôm nay";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy");
        return sdf.format(createdAt);
    }
}
