package model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DeliveryIssue - Sự cố shipper báo khi giao hàng (bảng delivery_issues, UC29).
 */
public class DeliveryIssue implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Loại sự cố -> nhãn hiển thị (giữ thứ tự cho dropdown) */
    public static final Map<String, String> TYPES = new LinkedHashMap<>();
    static {
        TYPES.put("CUSTOMER_UNREACHABLE", "Không liên lạc được khách");
        TYPES.put("WRONG_ADDRESS", "Sai / không tìm thấy địa chỉ");
        TYPES.put("CUSTOMER_REFUSED", "Khách từ chối nhận hàng");
        TYPES.put("DAMAGED", "Hàng bị đổ vỡ / hư hỏng");
        TYPES.put("TRAFFIC", "Kẹt xe / thời tiết, giao trễ");
        TYPES.put("OTHER", "Khác");
    }

    private int id;
    private int deliveryId;
    private String issueType;
    private String description;
    private Timestamp createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDeliveryId() { return deliveryId; }
    public void setDeliveryId(int deliveryId) { this.deliveryId = deliveryId; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getIssueTypeLabel() {
        return TYPES.getOrDefault(issueType, issueType);
    }

    public String getCreatedAtDisplay() {
        return createdAt == null ? "" : new SimpleDateFormat("HH:mm dd/MM/yyyy").format(createdAt);
    }
}
