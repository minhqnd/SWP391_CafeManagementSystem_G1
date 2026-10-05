package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * CartItem - Một dòng món trong giỏ hàng (lưu trong session)
 */
public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private int lineId;
    private int productId;
    private String productName;
    private String categoryName;
    private String imageUrl;
    private String size;
    private List<String> toppings = new ArrayList<>();
    private double unitPrice;
    private int quantity;
    private boolean selected = true;
    private boolean available = true;

    public CartItem() {
    }

    public CartItem(int productId, String size, List<String> toppings, int quantity) {
        this.productId = productId;
        this.size = size;
        this.toppings = toppings != null ? toppings : new ArrayList<>();
        this.quantity = quantity;
    }

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
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

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public List<String> getToppings() {
        return toppings;
    }

    public void setToppings(List<String> toppings) {
        this.toppings = toppings != null ? toppings : new ArrayList<>();
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public double getSubtotal() {
        return unitPrice * quantity;
    }

    /**
     * Nhãn size hiển thị: "Size M" với size ngắn, giữ nguyên với size dạng chữ (VD: "Chai 500ml")
     */
    public String getSizeLabel() {
        if (size == null || size.isEmpty()) {
            return "";
        }
        return size.length() <= 2 ? "Size " + size : size;
    }

    /**
     * Hai dòng cùng món, cùng size, cùng topping thì được gộp số lượng
     */
    public boolean hasSameOptions(CartItem other) {
        return productId == other.productId
                && String.valueOf(size).equals(String.valueOf(other.size))
                && toppings.equals(other.toppings);
    }
}
