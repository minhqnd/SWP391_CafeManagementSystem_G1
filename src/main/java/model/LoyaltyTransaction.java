package model;

import java.util.Date;

public class LoyaltyTransaction {
    private int id;
    private int customerId;
    private int orderId;
    private int pointsChange;
    private String transactionType;
    private Date createdAt;

    public LoyaltyTransaction() {
    }

    public LoyaltyTransaction(int id, int customerId, int orderId, int pointsChange, String transactionType, Date createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.orderId = orderId;
        this.pointsChange = pointsChange;
        this.transactionType = transactionType;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getPointsChange() {
        return pointsChange;
    }

    public void setPointsChange(int pointsChange) {
        this.pointsChange = pointsChange;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
