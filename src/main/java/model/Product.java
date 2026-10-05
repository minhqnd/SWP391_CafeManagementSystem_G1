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

    // 7-param constructor used by Homepage
    public Product(int id, int categoryId, String name, double basePrice, String variantsConfig, String imageUrl, boolean isAvailable) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.basePrice = basePrice;
        this.variantsConfig = variantsConfig;
        this.imageUrl = imageUrl;
        this.isAvailable = isAvailable;
    }

    // Full constructor used by Menu & detailed catalog
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

    private java.util.List<ProductIngredient> ingredients = new java.util.ArrayList<>();

    public java.util.List<ProductIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(java.util.List<ProductIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    /**
     * Trích xuất hướng dẫn pha chế SOP từ variantsConfig (JSON)
     */
    public String getRecipeInstructions() {
        if (variantsConfig == null || !variantsConfig.contains("\"instructions\"")) {
            return "";
        }
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"instructions\"\\s*:\\s*\"([^\"]*)\"").matcher(variantsConfig);
            if (m.find()) {
                return m.group(1).replace("\\n", "\n").replace("\\\"", "\"");
            }
        } catch (Exception ignored) {
        }
        return "";
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
