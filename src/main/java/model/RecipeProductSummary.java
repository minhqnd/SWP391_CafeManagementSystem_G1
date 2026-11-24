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
        // [CTG02]
        // . Em dùng RecipeProductSummary để giữ món, các dòng nguyên liệu và chi phí cho giao diện. Chi phí
        //   một dòng là lượng dùng nhân đơn giá; thiếu đơn giá thì chưa có chi phí đầy đủ.
        //
        // Tra cứu: Các thẻ món tại admin-recipes.jsp / CTM04 đọc thông tin này. Kết thúc tính và hiển thị chi
        //          phí dòng, không ghi giá bán.
        public BigDecimal getLineCost() {

            return unitCost == null
                    ? null
                    : unitCost.multiply(BigDecimal.valueOf(ingredient.getQuantityRequired()));
        }
    }
}
