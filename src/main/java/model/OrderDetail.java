package model;

import java.io.Serializable;

/**
 * Represents a single product line item within an order.
 */
public class OrderDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int orderId;
    private int productId;
    private String productNameAtTime;
    private double unitPrice;
    private String customization;
    private int quantity;
    private double subtotal;

    // Transient fields for UI/join data (not strictly in order_items table)
    private int categoryId;
    private String categoryName;

    public OrderDetail() {
    }

    public OrderDetail(int id,
                 int orderId,
                 int productId,
                 String productNameAtTime,
                 double unitPrice,
                 String customization,
                 int quantity,
                 double subtotal,
                 int categoryId,
                 String categoryName) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.productNameAtTime = productNameAtTime;
        this.unitPrice = unitPrice;
        this.customization = customization;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductNameAtTime() { return productNameAtTime; }
    public void setProductNameAtTime(String productNameAtTime) { this.productNameAtTime = productNameAtTime; }

    public String getProductName() { return productNameAtTime; }
    public void setProductName(String productName) { this.productNameAtTime = productName; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public String getCustomization() { return customization; }
    public void setCustomization(String customization) { this.customization = customization; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
