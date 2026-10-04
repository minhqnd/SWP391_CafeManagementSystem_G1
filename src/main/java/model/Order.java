package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String orderCode;
    private String orderType;
    private String status;
    private String tableNumber;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private String orderNote;
    private LocalDateTime createdAt;
    private int itemCount;
    private int readyItemCount;
    private List<OrderDetail> items = new ArrayList<>();

    public Order() {
    }

    public Order(int id, String orderCode, String orderType, String status,
                 String tableNumber, String recipientName,
                 String recipientPhone, String deliveryAddress,
                 String orderNote, LocalDateTime createdAt,
                 int itemCount, int readyItemCount) {
        this.id = id;
        this.orderCode = orderCode;
        this.orderType = orderType;
        this.status = status;
        this.tableNumber = tableNumber;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.deliveryAddress = deliveryAddress;
        this.orderNote = orderNote;
        this.createdAt = createdAt;
        this.itemCount = itemCount;
        this.readyItemCount = readyItemCount;
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

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
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

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getReadyItemCount() {
        return readyItemCount;
    }

    public void setReadyItemCount(int readyItemCount) {
        this.readyItemCount = readyItemCount;
    }

    public List<OrderDetail> getItems() {
        return items;
    }

    public void setItems(List<OrderDetail> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
