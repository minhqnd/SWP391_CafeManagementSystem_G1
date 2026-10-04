package model;

import java.io.Serializable;
import java.text.DecimalFormat;

/**
 * Entity OrderDetail - Bám sát mục 3.2 Entity Descriptions trong tài liệu SRS.
 * Lưu trữ danh sách món ăn, số lượng, đơn giá và tùy chỉnh đi kèm trong đơn hàng.
 */
public class OrderDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int orderId;
    private int productId;
    private String productName;
    private double unitPrice;
    private String customization; // Ví dụ: Size M • Ít ngọt (50%) • Kem béo
    private int quantity;
    private double subtotal;

    public OrderDetail() {
    }

    public OrderDetail(int id, int orderId, int productId, String productName,
                       double unitPrice, String customization, int quantity, double subtotal) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.customization = customization;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    // Getters and Setters
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

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
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

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    // Helper formatters
    public String getFormattedUnitPrice() {
        return new DecimalFormat("###,###,###").format(unitPrice).replace(",", ".") + " đ";
    }

    public String getFormattedSubtotal() {
        return new DecimalFormat("###,###,###").format(subtotal).replace(",", ".") + " đ";
    }

    public String getFormattedQuantity() {
        return String.format("%02d", quantity);
    }
}
