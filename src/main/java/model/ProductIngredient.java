package model;

public class ProductIngredient {
    private int productId;
    private int ingredientId;
    private double quantityRequired;
    
    // Additional fields for display
    private String ingredientName;
    private String unit;

    public ProductIngredient() {}

    public ProductIngredient(int productId, int ingredientId, double quantityRequired, String ingredientName, String unit) {
        this.productId = productId;
        this.ingredientId = ingredientId;
        this.quantityRequired = quantityRequired;
        this.ingredientName = ingredientName;
        this.unit = unit;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }
    public double getQuantityRequired() { return quantityRequired; }
    public void setQuantityRequired(double quantityRequired) { this.quantityRequired = quantityRequired; }
    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
