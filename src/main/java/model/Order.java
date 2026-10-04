package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps orders. Nullable foreign keys use Integer; amounts preserve DECIMAL precision with double helpers.
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String orderCode;
    private Integer customerId;
    private Integer cashierId;
    private Integer voucherId;
    private String orderType;   // DINE_IN, TAKEAWAY, ONLINE
    private String status;      // PENDING, CONFIRMED, PREPARING, READY, DELIVERING, COMPLETED, CANCELLED
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal deliveryFee = BigDecimal.ZERO;
    private BigDecimal finalAmount = BigDecimal.ZERO;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private Integer shipperId;
    private String tableNumber;
    private String orderNote;
    private LocalDateTime createdAt;

    // UI relationship, loaded from order_items; not a column in orders.
    private List<OrderDetail> items = new ArrayList<>();

    public Order() {
    }

    public Order(int id,
                 String orderCode,
                 Integer customerId,
                 Integer cashierId,
                 Integer voucherId,
                 String orderType,
                 String status,
                 BigDecimal totalAmount,
                 BigDecimal discountAmount,
                 BigDecimal deliveryFee,
                 BigDecimal finalAmount,
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
        this.voucherId = voucherId;
        this.orderType = orderType;
        this.status = status;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.deliveryFee = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        this.finalAmount = finalAmount != null ? finalAmount : BigDecimal.ZERO;
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

    public Integer getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Integer voucherId) {
        this.voucherId = voucherId;
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
        return totalAmount != null ? totalAmount.doubleValue() : 0.0;
    }

    public BigDecimal getTotalAmountBigDecimal() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = BigDecimal.valueOf(totalAmount);
    }

    public double getDiscountAmount() {
        return discountAmount != null ? discountAmount.doubleValue() : 0.0;
    }

    public BigDecimal getDiscountAmountBigDecimal() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = BigDecimal.valueOf(discountAmount);
    }

    public double getDeliveryFee() {
        return deliveryFee != null ? deliveryFee.doubleValue() : 0.0;
    }

    public BigDecimal getDeliveryFeeBigDecimal() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
    }

    public void setDeliveryFee(double deliveryFee) {
        this.deliveryFee = BigDecimal.valueOf(deliveryFee);
    }

    public double getFinalAmount() {
        return finalAmount != null ? finalAmount.doubleValue() : 0.0;
    }

    public BigDecimal getFinalAmountBigDecimal() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount != null ? finalAmount : BigDecimal.ZERO;
    }

    public void setFinalAmount(double finalAmount) {
        this.finalAmount = BigDecimal.valueOf(finalAmount);
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

    // Total units, not the number of order_items rows.
    public int getItemCount() {
        return items.stream().mapToInt(OrderDetail::getQuantity).sum();
    }
}
