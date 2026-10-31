package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String orderCode;
    private Integer customerId;
    private Integer cashierId;
    private String orderType;
    private String status;
    private double totalAmount;
    private double discountAmount;
    private double deliveryFee;
    private double finalAmount;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private Integer shipperId;
    private String tableNumber;
    private String orderNote;
    private LocalDateTime createdAt;
    // Transient Barista UI state; not a database column.
    private LocalDateTime readyAt;

    public LocalDateTime getReadyAt() { return readyAt; }

    public void setReadyAt(LocalDateTime readyAt) { this.readyAt = readyAt; }

    private List<OrderDetail> items = new ArrayList<>();

    public Order() {
    }

    public Order(int id,
            String orderCode,
            Integer customerId,
            Integer cashierId,
            String orderType,
            String status,
            double totalAmount,
            double discountAmount,
            double deliveryFee,
            double finalAmount,
            String recipientName,
            String recipientPhone,
            String deliveryAddress,
            Integer shipperId,
            String tableNumber,
            String orderNote,
            LocalDateTime createdAt) {
        this.id = id;
        this.orderCode = orderCode;
        this.customerId = customerId;
        this.cashierId = cashierId;
        this.orderType = orderType;
        this.status = status;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.deliveryFee = deliveryFee;
        this.finalAmount = finalAmount;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.deliveryAddress = deliveryAddress;
        this.shipperId = shipperId;
        this.tableNumber = tableNumber;
        this.orderNote = orderNote;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getCashierId() {
        return cashierId;
    }

    public void setCashierId(Integer cashierId) {
        this.cashierId = cashierId;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(double finalAmount) {
        this.finalAmount = finalAmount;
    }

    // BigDecimal compatibility helpers
    public BigDecimal getTotalAmountBigDecimal() {
        return BigDecimal.valueOf(totalAmount);
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount.doubleValue() : 0.0;
    }

    public BigDecimal getDiscountAmountBigDecimal() {
        return BigDecimal.valueOf(discountAmount);
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount != null ? discountAmount.doubleValue() : 0.0;
    }

    public BigDecimal getDeliveryFeeBigDecimal() {
        return BigDecimal.valueOf(deliveryFee);
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee != null ? deliveryFee.doubleValue() : 0.0;
    }

    public BigDecimal getFinalAmountBigDecimal() {
        return BigDecimal.valueOf(finalAmount);
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount != null ? finalAmount.doubleValue() : 0.0;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Integer getShipperId() {
        return shipperId;
    }

    public void setShipperId(Integer shipperId) {
        this.shipperId = shipperId;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getOrderNote() {
        return orderNote;
    }

    public void setOrderNote(String orderNote) {
        this.orderNote = orderNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderDetail> getItems() {
        return items;
    }

    public void setItems(List<OrderDetail> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public int getItemCount() {
        return items != null ? items.stream().mapToInt(OrderDetail::getQuantity).sum() : 0;
    }

    // ===== Thông tin cho khách xem (màn Thanh toán, Đơn hàng của tôi) =====

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private int pointsUsed;         // điểm Aura khách đã dùng để giảm giá đơn này
    private int rewardPoints;       // điểm EARN thực tế đã ghi cho đơn
    private String paymentMethod;   // phương thức của lần thanh toán thành công
    private LocalDateTime paidAt;   // null = chưa thanh toán
    private LocalDateTime scheduledAt; // giờ khách hẹn nhận; null = càng sớm càng tốt

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getScheduledAtDisplay() {
        return scheduledAt == null ? "Càng sớm càng tốt" : scheduledAt.format(TIME_FORMAT) + " " + scheduledAt.format(DATE_FORMAT);
    }

    /** Hình thức nhận hàng: ONLINE = giao tận nơi, TAKEAWAY = tự đến lấy / mang đi, DINE_IN = dùng tại quán */
    public String getFulfillmentLabel() {
        if ("ONLINE".equals(orderType)) return "Giao tận nơi";
        if ("TAKEAWAY".equals(orderType)) return customerId != null && cashierId == null ? "Tự đến lấy" : "Mang đi";
        if ("DINE_IN".equals(orderType)) return "Dùng tại quán";
        return orderType != null ? orderType : "";
    }

    public int getPointsUsed() { return pointsUsed; }
    public void setPointsUsed(int pointsUsed) { this.pointsUsed = pointsUsed; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public boolean isDelivery() { return "ONLINE".equals(orderType); }

    /** Đơn giao tận nơi chọn COD, shipper chưa thu tiền (payment CASH chưa có paid_at) */
    public boolean isCodUnpaid() { return isDelivery() && "CASH".equals(paymentMethod) && paidAt == null; }
    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isCompleted() { return "COMPLETED".equals(status); }
    public boolean isCancelled() { return "CANCELLED".equals(status); }
    public boolean isPaid() {
        return "CONFIRMED".equals(status) || "PREPARING".equals(status)
                || "READY".equals(status) || "DELIVERING".equals(status) || "COMPLETED".equals(status);
    }

    public String getFormattedTotalAmount() {
        return new DecimalFormat("###,###,###").format(totalAmount).replace(",", ".") + " đ";
    }

    public String getFormattedDeliveryFee() {
        return new DecimalFormat("###,###,###").format(deliveryFee).replace(",", ".") + " đ";
    }

    public String getFormattedDiscountAmount() {
        return new DecimalFormat("###,###,###").format(discountAmount).replace(",", ".") + " đ";
    }


    public String getFormattedFinalAmount() {
        return new DecimalFormat("###,###,###").format(finalAmount).replace(",", ".") + " đ";
    }

    /** Đã thanh toán và đang được pha chế / giao */
    public boolean isInProgress() {
        return "CONFIRMED".equals(status) || "PREPARING".equals(status)
                || "READY".equals(status) || "DELIVERING".equals(status);
    }

    /** Bước trên thanh tiến trình: 1 Đã thanh toán, 2 Pha chế, 3 Giao hàng / Sẵn sàng tại quầy, 4 Hoàn thành */
    public int getProgressStep() {
        if ("PREPARING".equals(status)) {
            return 2;
        }
        if ("READY".equals(status) || "DELIVERING".equals(status)) {
            return 3;
        }
        if ("COMPLETED".equals(status)) {
            return 4;
        }
        return 1;
    }

    public String getStatusLabel() {
        if (status == null) {
            return "";
        }
        switch (status) {
            case "PENDING": return "Chờ thanh toán";
            case "CONFIRMED": return isCodUnpaid() ? "Đã đặt - trả tiền khi nhận" : "Đã thanh toán";
            case "PREPARING": return "Đang chuẩn bị món";
            case "READY": return isDelivery() ? "Chờ giao hàng" : "Sẵn sàng tại quầy";
            case "DELIVERING": return "Đang giao hàng";
            case "COMPLETED": return isDelivery() ? "Đã giao thành công" : "Đã nhận món";
            case "CANCELLED": return "Đã hủy";
            default: return status;
        }
    }

    /** Nhóm trạng thái dùng làm class CSS cho badge: pending, processing, delivering, completed, cancelled */
    public String getStatusGroup() {
        if (isPending() || isCompleted() || isCancelled()) {
            return status.toLowerCase();
        }
        return "DELIVERING".equals(status) ? "delivering" : "processing";
    }

    public String getPaymentMethodLabel() {
        if (paymentMethod == null) {
            return "Chưa thanh toán";
        }
        switch (paymentMethod) {
            case "BANK_TRANSFER": return "Chuyển khoản VietQR";
            case "CASH": return "Tiền mặt";
            case "VNPAY": return "VNPAY";
            case "MOMO": return "Ví MoMo";
            default: return paymentMethod;
        }
    }

    public int getRewardPoints() {
        return rewardPoints > 0 ? rewardPoints
                : new ShopSettings().calculateEarnedPoints(totalAmount - discountAmount);
    }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

    /**
     * Giờ nhận dự kiến (HH:mm) = lúc thanh toán (chưa thanh toán thì lúc đặt) + thời gian giao Admin cài đặt.
     * Gọi trong JSP: ${o.estimateReceiveTime(deliveryMinutes)}
     */
    public String estimateReceiveTime(int deliveryMinutes) {
        LocalDateTime start = paidAt != null ? paidAt : createdAt;
        return start != null ? start.plusMinutes(deliveryMinutes).format(TIME_FORMAT) : "";
    }

    public String getCreatedTimeText() {
        return createdAt != null ? createdAt.format(TIME_FORMAT) : "";
    }

    public String getCreatedDateText() {
        return createdAt != null ? createdAt.format(DATE_FORMAT) : "";
    }

    public String getPaidTimeText() {
        return paidAt != null ? paidAt.format(TIME_FORMAT) : "";
    }

    public String getPaidAtText() {
        return paidAt != null ? paidAt.format(TIME_FORMAT) + " " + paidAt.format(DATE_FORMAT) : "";
    }

    /** VD: "2x Cà Phê Muối, 1x Bánh Croissant" */
    public String getItemSummary() {
        StringBuilder summary = new StringBuilder();
        for (OrderDetail item : items) {
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append(item.getQuantity()).append("x ").append(item.getProductName());
        }
        return summary.toString();
    }

    public String getFirstImageUrl() {
        return items.isEmpty() ? null : items.get(0).getImageUrl();
    }
}
