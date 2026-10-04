package model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Maps order_items. productName is product_name_at_time (the sale snapshot).
 * categoryId/categoryName are joined from products/categories for UI filtering.
 */
public class OrderDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int orderId;
    private int productId;
    private String productName;
    private BigDecimal unitPrice;
    private String customization;
    private int quantity;
    private BigDecimal subtotal;
    private int categoryId;
    private String categoryName;

    public OrderDetail() {
    }

    public OrderDetail(int id,
                 int orderId,
                 int productId,
                 String productName,
                 BigDecimal unitPrice,
                 String customization,
                 int quantity,
                 BigDecimal subtotal,
                 int categoryId,
                 String categoryName) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.customization = customization;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getCustomization() {
        return customization;
    }

    public void setCustomization(String customization) {
        this.customization = customization;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

}
