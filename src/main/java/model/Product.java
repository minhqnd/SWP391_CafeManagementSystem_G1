package model;

import java.io.Serializable;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int categoryId;
    private String name;
    private double basePrice;
    private String variantsConfig;
    private String imageUrl;
    private boolean isAvailable;

    public Product() {
    }

    public Product(int id, int categoryId, String name, double basePrice, String variantsConfig, String imageUrl, boolean isAvailable) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.basePrice = basePrice;
        this.variantsConfig = variantsConfig;
        this.imageUrl = imageUrl;
        this.isAvailable = isAvailable;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }

    public String getVariantsConfig() { return variantsConfig; }
    public void setVariantsConfig(String variantsConfig) { this.variantsConfig = variantsConfig; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean isAvailable) { this.isAvailable = isAvailable; }
}
