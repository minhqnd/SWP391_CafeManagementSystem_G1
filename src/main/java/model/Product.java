package model;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int categoryId;
    private String categoryName;
    private String name;
    private double basePrice;
    private String description;
    private String badge;
    private String subtitle;
    private String variantsConfig;
    private String imageUrl;
    private boolean isAvailable;

    public Product() {
    }

    public Product(int id, int categoryId, String categoryName, String name, double basePrice,
                   String description, String badge, String subtitle, String variantsConfig,
                   String imageUrl, boolean isAvailable) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.name = name;
        this.basePrice = basePrice;
        this.description = description;
        this.badge = badge;
        this.subtitle = subtitle;
        this.variantsConfig = variantsConfig;
        this.imageUrl = imageUrl;
        this.isAvailable = isAvailable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getVariantsConfig() {
        return variantsConfig;
    }

    public void setVariantsConfig(String variantsConfig) {
        this.variantsConfig = variantsConfig;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    /**
     * Định dạng giá tiền dạng 45.000đ
     */
    public String getFormattedPrice() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format(basePrice) + "đ";
    }
}
