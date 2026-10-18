package model;

import java.math.BigDecimal;
import java.util.List;

/** View data for one product card on the admin recipe screen. */
public class RecipeProductSummary {
    private final Product product;
    private final List<IngredientLine> ingredientLines;
    private final BigDecimal recipeCost;
    private final boolean costComplete;
    private final BigDecimal grossMarginPercent;

    public RecipeProductSummary(Product product, List<IngredientLine> ingredientLines,
                                BigDecimal recipeCost, boolean costComplete,
                                BigDecimal grossMarginPercent) {
        this.product = product;
        this.ingredientLines = ingredientLines;
        this.recipeCost = recipeCost;
        this.costComplete = costComplete;
        this.grossMarginPercent = grossMarginPercent;
    }

    public Product getProduct() { return product; }
    public List<IngredientLine> getIngredientLines() { return ingredientLines; }
    public BigDecimal getRecipeCost() { return recipeCost; }
    public boolean isCostComplete() { return costComplete; }
    public BigDecimal getGrossMarginPercent() { return grossMarginPercent; }
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
        public BigDecimal getLineCost() {
            return unitCost == null
                    ? null
                    : unitCost.multiply(BigDecimal.valueOf(ingredient.getQuantityRequired()));
        }
    }
}
