package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps order_items. productName is product_name_at_time (the sale snapshot).
 * categoryId/categoryName are joined from products/categories for UI filtering.
 */
public class OrderDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    // Cột customization lưu dạng {"size":"L","toppings":["Kem whipping"]}
    private static final Pattern SIZE_FIELD = Pattern.compile("\"size\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern TOPPINGS_FIELD = Pattern.compile("\"toppings?\"\\s*:\\s*\\[([^\\]]*)\\]");
    private static final Pattern QUOTED_TEXT = Pattern.compile("\"([^\"]+)\"");

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
    private String imageUrl;

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /** Size khách đã chọn, null nếu không có */
    public String getSizeOption() {
        if (customization == null) {
            return null;
        }
        Matcher size = SIZE_FIELD.matcher(customization);
        return size.find() ? size.group(1) : null;
    }

    /** Các topping khách đã chọn */
    public List<String> getToppingOptions() {
        List<String> toppings = new ArrayList<>();
        if (customization == null) {
            return toppings;
        }
        Matcher block = TOPPINGS_FIELD.matcher(customization);
        if (block.find()) {
            Matcher name = QUOTED_TEXT.matcher(block.group(1));
            while (name.find()) {
                toppings.add(name.group(1));
            }
        }
        return toppings;
    }

    /** Mô tả tuỳ chọn để hiển thị, VD: "Size L • Kem whipping, Hạt chia" */
    public String getOptionsText() {
        StringBuilder text = new StringBuilder();
        String size = getSizeOption();
        if (size != null && !size.isEmpty()) {
            // Size dạng chữ cái (M, L) thì thêm chữ "Size", còn "Chai 500ml", "Tiêu chuẩn" giữ nguyên
            text.append(size.length() <= 2 ? "Size " + size : size);
        }
        List<String> toppings = getToppingOptions();
        if (!toppings.isEmpty()) {
            if (text.length() > 0) {
                text.append(" • ");
            }
            text.append(String.join(", ", toppings));
        }
        return text.toString();
    }
}
