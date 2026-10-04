package model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Model Feedback / Customer Review - Phản ánh & Khiếu nại của khách hàng
 * Ánh xạ trực tiếp từ bảng `customer_reviews` trong database `aura_cafe.sql`
 */
public class Feedback implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String ticketCode;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private int orderId;
    private String orderCode;
    
    // Core database field: issue_type ENUM('RATING', 'COMPLAINT')
    private String issueType = "COMPLAINT";
    
    private String category;
    private String categoryLabel;
    private int rating = 5;
    private String title;
    private String content;
    private String images;
    private String contactMethod = "phone";
    
    // Core database field: status ENUM('PENDING', 'RESOLVED', 'REJECTED')
    private String status = "PENDING";
    private String statusLabel = "Đang chờ tiếp nhận";
    private int progressPercent = 35;
    
    // Core database field: admin_response TEXT
    private String adminResponse;
    private String agentName = "Ban Quản lý Aura Café";
    private String agentNote;
    private String resolution;
    private String giftVoucher;
    
    private Timestamp createdAt;
    private Timestamp updatedAt;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryLabel() {
        if (categoryLabel != null && !categoryLabel.trim().isEmpty()) {
            return categoryLabel;
        }
        if ("RATING".equalsIgnoreCase(issueType)) {
            return "Đánh giá chất lượng (" + rating + "★)";
        }
        return "Khiếu nại đơn hàng";
    }

    public void setCategoryLabel(String categoryLabel) {
        this.categoryLabel = categoryLabel;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }
        String oc = getOrderCode();
        if ("RATING".equalsIgnoreCase(issueType)) {
            return "Đánh giá đơn hàng #" + (oc.isEmpty() ? id : oc);
        }
        return "Khiếu nại đơn hàng #" + (oc.isEmpty() ? id : oc);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getContactMethod() {
        return contactMethod;
    }

    public void setContactMethod(String contactMethod) {
        this.contactMethod = contactMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusLabel() {
        if (statusLabel != null && !statusLabel.isEmpty()) {
            return statusLabel;
        }
        if ("RESOLVED".equalsIgnoreCase(status)) {
            return "Đã giải quyết xong";
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            return "Từ chối xử lý";
        }
        return "Đang chờ tiếp nhận";
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public int getProgressPercent() {
        if (progressPercent > 0) {
            return progressPercent;
        }
        if ("RESOLVED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)) {
            return 100;
        }
        return 35;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getAdminResponse() {
        return adminResponse;
    }

    public void setAdminResponse(String adminResponse) {
        this.adminResponse = adminResponse;
    }

    public String getAgentName() {
        return agentName != null ? agentName : "Ban Quản lý Aura Café";
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentNote() {
        if (agentNote != null && !agentNote.trim().isEmpty()) {
            return agentNote;
        }
        if (adminResponse != null && !adminResponse.trim().isEmpty()) {
            return adminResponse;
        }
        return "Bộ phận Chăm sóc khách hàng đang tiếp nhận và rà soát lại đơn hàng.";
    }

    public void setAgentNote(String agentNote) {
        this.agentNote = agentNote;
    }

    public String getResolution() {
        if (resolution != null && !resolution.trim().isEmpty()) {
            return resolution;
        }
        return adminResponse;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getGiftVoucher() {
        return giftVoucher;
    }

    public void setGiftVoucher(String giftVoucher) {
        this.giftVoucher = giftVoucher;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isProcessing() {
        return "PROCESSING".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status);
    }

    public boolean isResolved() {
        return "RESOLVED".equalsIgnoreCase(status);
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) {
            return "Vừa xong - Hôm nay";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy");
        return sdf.format(createdAt);
    }

    public List<String> getImageList() {
        List<String> list = new ArrayList<>();
        if (images != null && !images.trim().isEmpty()) {
            String[] parts = images.split(";");
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    list.add(p.trim());
                }
            }
        }
        return list;
    }
}
