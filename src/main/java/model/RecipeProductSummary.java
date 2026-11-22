package model;

import java.math.BigDecimal;
import java.util.List;

public class RecipeProductSummary {

    private final Product product;
    private final List<IngredientLine> ingredientLines;
    private final BigDecimal recipeCost;
    private final boolean costComplete;

    public RecipeProductSummary(Product product, List<IngredientLine> ingredientLines,
                                BigDecimal recipeCost, boolean costComplete) {
        this.product = product;
        this.ingredientLines = ingredientLines;
        this.recipeCost = recipeCost;
        this.costComplete = costComplete;
    }

    public Product getProduct() { return product; }
    public List<IngredientLine> getIngredientLines() { return ingredientLines; }
    public BigDecimal getRecipeCost() { return recipeCost; }
    public boolean isCostComplete() { return costComplete; }
    public boolean isHasRecipe() { return !ingredientLines.isEmpty(); }

    public static class IngredientLine {
        private final ProductIngredient ingredient;
        private final BigDecimal unitCost;

        public IngredientLine(ProductIngredient ingredient, BigDecimal unitCost) {
            this.ingredient = ingredient;
            this.unitCost = unitCost;
        }

        public String getName() { return ingredient.getIngredientName(); }
        public String getUnit() { return ingredient.getUnit(); }
        public double getQuantity() { return ingredient.getQuantityRequired(); }
        public boolean isCostAvailable() { return unitCost != null; }
        // [CTGT03]
        // . Khi admin-recipes.jsp đọc line.lineCost, hàm getLineCost (tính chi phí của một dòng nguyên liệu) lấy
        //   đơn giá nhân định lượng của nguyên liệu mà controller đã đưa vào IngredientLine. Có giá thì trả chi
        //   phí để hiện trên thẻ món; thiếu đơn giá thì trả null và isCostAvailable (kiểm tra nguyên liệu đã có
        //   đơn giá chưa) giúp trang hiện chưa có giá.
        public BigDecimal getLineCost() {

            return unitCost == null
                    ? null
                    : unitCost.multiply(BigDecimal.valueOf(ingredient.getQuantityRequired()));
        }
    }
}
