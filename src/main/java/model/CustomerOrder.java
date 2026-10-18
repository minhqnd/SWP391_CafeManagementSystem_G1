package model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity CustomerOrder - Bám sát mục 3.2 Entity Descriptions trong tài liệu SRS.
 * Đại diện cho đơn hàng của khách hàng (Online hoặc tại quầy).
 */
public class CustomerOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String orderCode;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private Integer cashierId;
    private String orderType; // DINE_IN, TAKEAWAY, ONLINE
    private String status;    // PENDING, CONFIRMED, PREPARING, READY, DELIVERING, COMPLETED, CANCELLED
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
    private Timestamp createdAt;

    // Danh sách chi tiết món ăn trong đơn
    private List<OrderDetail> orderDetails = new ArrayList<>();

    public CustomerOrder() {
    }

    public CustomerOrder(int id, String orderCode, Integer customerId, Integer cashierId,
                         String orderType, String status, double totalAmount, double discountAmount,
                         double deliveryFee, double finalAmount, String recipientName, String recipientPhone,
                         String deliveryAddress, Integer shipperId, String tableNumber, String orderNote,
                         Timestamp createdAt) {
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

    // Getters and Setters
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderDetail> getOrderDetails() {
        return orderDetails;
    }

    public void setOrderDetails(List<OrderDetail> orderDetails) {
        this.orderDetails = orderDetails;
    }

    // Helper formatters phục vụ hiển thị trên JSP
    public String getFormattedFinalAmount() {
        return new DecimalFormat("###,###,###").format(finalAmount).replace(",", ".") + " đ";
    }

    public String getFormattedTotalAmount() {
        return new DecimalFormat("###,###,###").format(totalAmount).replace(",", ".") + " đ";
    }

    public String getFormattedDiscountAmount() {
        return new DecimalFormat("###,###,###").format(discountAmount).replace(",", ".") + " đ";
    }

    public String getFormattedDeliveryFee() {
        return new DecimalFormat("###,###,###").format(deliveryFee).replace(",", ".") + " đ";
    }

    public double getVatAmount() {
        return Math.round(finalAmount * 0.08);
    }

    public String getFormattedVatAmount() {
        return new DecimalFormat("###,###,###").format(getVatAmount()).replace(",", ".") + " đ";
    }

    public int getEarnedPoints() {
        return (int) Math.floor(finalAmount / 10000.0);
    }

    public String getStatusDisplay() {
        if (status == null) return "Chờ thanh toán";
        switch (status.toUpperCase()) {
            case "PENDING":
                return "Chờ thanh toán";
            case "CONFIRMED":
                return "Đã thanh toán / Chờ chuẩn bị";
            case "PREPARING":
                return "Barista đang pha chế";
            case "READY":
                return "Đã pha chế xong";
            case "DELIVERING":
                return "Đang giao hàng";
            case "COMPLETED":
                return "Hoàn thành";
            case "CANCELLED":
                return "Đã hủy";
            default:
                return status;
        }
    }

    public String getOrderTypeDisplay() {
        if (orderType == null) return "Giao tận nơi";
        switch (orderType.toUpperCase()) {
            case "ONLINE":
                return "Giao tận nơi";
            case "TAKEAWAY":
                return "Mang về (Takeaway)";
            case "DINE_IN":
                return "Dùng tại bàn (Dine-in)";
            default:
                return orderType;
        }
    }

    public boolean isPaid() {
        return status != null && !status.equalsIgnoreCase("PENDING") && !status.equalsIgnoreCase("CANCELLED");
    }

    public boolean isCanCancel() {
        return status != null && (status.equalsIgnoreCase("PENDING") || status.equalsIgnoreCase("CONFIRMED"));
    }
}
