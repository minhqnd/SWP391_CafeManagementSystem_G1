package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Delivery - Một lần giao hàng của đơn online (bảng deliveries, SRS entity Delivery, UC26-UC30).
 * Shipper nhận đơn đã pha xong -> DELIVERING (nhận + lấy hàng 1 bước) -> DELIVERED.
 * Đơn Admin gán sẵn: ASSIGNED -> DELIVERING khi shipper bấm "Nhận đơn & đi giao".
 * Kèm thông tin đơn hàng cần cho shipper: người nhận, SĐT, địa chỉ, món, tiền thu hộ (COD).
 */
public class Delivery implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int orderId;
    private int shipperId;
    private String status;
    private BigDecimal codAmount = BigDecimal.ZERO;
    private BigDecimal codCollected;
    private Timestamp codHandedOverAt;
    private String codReceiverName;
    private Timestamp assignedAt;
    private Timestamp acceptedAt;
    private Timestamp pickedUpAt;
    private Timestamp deliveredAt;
    private String deliveryNote;

    // Thông tin đơn hàng
    private String orderCode;
    private String orderStatus;
    private String customerName;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private String orderNote;
    private Timestamp scheduledAt;
    private Timestamp orderCreatedAt;
    private BigDecimal finalAmount = BigDecimal.ZERO;
    private BigDecimal deliveryFee = BigDecimal.ZERO;
    private String shipperName;
    private int itemCount;

    private List<OrderDetail> items = new ArrayList<>();
    private List<DeliveryIssue> issues = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getShipperId() { return shipperId; }
    public void setShipperId(int shipperId) { this.shipperId = shipperId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getCodAmount() { return codAmount; }
    public void setCodAmount(BigDecimal codAmount) { this.codAmount = codAmount; }

    public BigDecimal getCodCollected() { return codCollected; }
    public void setCodCollected(BigDecimal codCollected) { this.codCollected = codCollected; }

    public Timestamp getCodHandedOverAt() { return codHandedOverAt; }
    public void setCodHandedOverAt(Timestamp codHandedOverAt) { this.codHandedOverAt = codHandedOverAt; }

    public String getCodReceiverName() { return codReceiverName; }
    public void setCodReceiverName(String codReceiverName) { this.codReceiverName = codReceiverName; }

    public Timestamp getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Timestamp assignedAt) { this.assignedAt = assignedAt; }

    public Timestamp getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Timestamp acceptedAt) { this.acceptedAt = acceptedAt; }

    public Timestamp getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(Timestamp pickedUpAt) { this.pickedUpAt = pickedUpAt; }

    public Timestamp getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Timestamp deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getDeliveryNote() { return deliveryNote; }
    public void setDeliveryNote(String deliveryNote) { this.deliveryNote = deliveryNote; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getOrderNote() { return orderNote; }
    public void setOrderNote(String orderNote) { this.orderNote = orderNote; }

    public Timestamp getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Timestamp scheduledAt) { this.scheduledAt = scheduledAt; }

    public Timestamp getOrderCreatedAt() { return orderCreatedAt; }
    public void setOrderCreatedAt(Timestamp orderCreatedAt) { this.orderCreatedAt = orderCreatedAt; }

    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public String getShipperName() { return shipperName; }
    public void setShipperName(String shipperName) { this.shipperName = shipperName; }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public List<OrderDetail> getItems() { return items; }
    public void setItems(List<OrderDetail> items) { this.items = items; }

    public List<DeliveryIssue> getIssues() { return issues; }
    public void setIssues(List<DeliveryIssue> issues) { this.issues = issues; }

    // ===== Helper hiển thị =====

    /** Đơn trả tiền mặt khi nhận hàng: shipper phải thu hộ */
    public boolean isCod() { return codAmount != null && codAmount.signum() > 0; }

    public boolean isAssigned() { return "ASSIGNED".equals(status); }
    public boolean isAccepted() { return "ACCEPTED".equals(status); }
    public boolean isDelivering() { return "DELIVERING".equals(status); }
    public boolean isDelivered() { return "DELIVERED".equals(status); }

    /** Quán đã pha chế xong, shipper có thể lấy hàng */
    public boolean isOrderReady() { return "READY".equals(orderStatus); }

    /** Đã thu COD nhưng chưa nộp về quỹ */
    public boolean isCodHolding() { return isDelivered() && isCod() && codHandedOverAt == null; }

    public String getStatusLabel() {
        if (status == null) return "";
        switch (status) {
            case "ASSIGNED": return "Chờ nhận đơn";
            case "ACCEPTED": return "Đã nhận đơn";
            case "DELIVERING": return "Đang giao";
            case "DELIVERED": return "Đã giao";
            default: return status;
        }
    }

    public String getOrderStatusLabel() {
        if (orderStatus == null) return "";
        switch (orderStatus) {
            case "PENDING": return "Chờ thanh toán";
            case "CONFIRMED": return "Chờ pha chế";
            case "PREPARING": return "Đang pha chế";
            case "READY": return "Sẵn sàng lấy hàng";
            case "DELIVERING": return "Đang giao";
            case "COMPLETED": return "Hoàn thành";
            case "CANCELLED": return "Đã huỷ";
            default: return orderStatus;
        }
    }

    public String getAssignedAtDisplay() { return format(assignedAt, "HH:mm dd/MM"); }
    public String getAcceptedAtDisplay() { return format(acceptedAt, "HH:mm dd/MM"); }
    public String getPickedUpAtDisplay() { return format(pickedUpAt, "HH:mm dd/MM"); }
    public String getDeliveredAtDisplay() { return format(deliveredAt, "HH:mm dd/MM/yyyy"); }
    public String getCodHandedOverAtDisplay() { return format(codHandedOverAt, "HH:mm dd/MM/yyyy"); }
    public String getOrderCreatedAtDisplay() { return format(orderCreatedAt, "HH:mm dd/MM"); }
    public String getScheduledAtDisplay() { return format(scheduledAt, "HH:mm dd/MM"); }

    private static String format(Timestamp time, String pattern) {
        return time == null ? "" : new SimpleDateFormat(pattern).format(time);
    }
}
