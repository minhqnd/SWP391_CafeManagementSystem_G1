package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RestockRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int supplierId;
    private String supplierName;
    private int creatorId;
    private String creatorName;
    private BigDecimal totalCost = BigDecimal.ZERO;
    private String status; // 'DRAFT', 'ORDERED', 'RECEIVED', 'CANCELLED'
    private Timestamp createdAt;
    private String notes;
    private List<RestockItem> items = new ArrayList<>();

    public static class RestockItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private int id;
        private int poId;
        private int ingredientId;
        private String ingredientName;
        private String unit;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal unitPrice = BigDecimal.ZERO;

        public RestockItem() {}

        public RestockItem(int ingredientId, String ingredientName, String unit,
                           BigDecimal quantity, BigDecimal unitPrice) {
            this.ingredientId = ingredientId;
            this.ingredientName = ingredientName;
            this.unit = unit;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getPoId() { return poId; }
        public void setPoId(int poId) { this.poId = poId; }

        public int getIngredientId() { return ingredientId; }
        public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

        public String getIngredientName() { return ingredientName; }
        public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

        public BigDecimal getSubtotal() {
            if (quantity == null || unitPrice == null) return BigDecimal.ZERO;
            return quantity.multiply(unitPrice);
        }
    }

    public RestockRequest() {
        this.status = "DRAFT";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public int getCreatorId() { return creatorId; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<RestockItem> getItems() { return items; }
    public void setItems(List<RestockItem> items) { this.items = items; }

    public String getItemsSummary() {
        if (items == null || items.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            RestockItem it = items.get(i);
            sb.append(it.getIngredientName()).append(": ")
              .append(it.getQuantity().stripTrailingZeros().toPlainString()).append(" ")
              .append(it.getUnit() != null ? it.getUnit() : "")
              .append(" x ").append(String.format("%,.0fđ", it.getUnitPrice()));
            if (i < items.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }
}
