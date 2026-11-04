package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class InventoryTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int ingredientId;
    private String ingredientName;
    private String unit;
    private String transactionType; // 'IN_PURCHASE', 'OUT_ORDER', 'ADJUSTMENT', 'SPOILAGE'
    private BigDecimal quantity;
    private BigDecimal adjustmentDelta;
    public void setAdjustmentDelta(BigDecimal value){adjustmentDelta=value;}
    public boolean isDecrease(){return adjustmentDelta!=null ? adjustmentDelta.signum()<0 : "OUT_ORDER".equalsIgnoreCase(transactionType)||"SPOILAGE".equalsIgnoreCase(transactionType);}
    private String referenceType;   // 'ORDER', 'PURCHASE_ORDER', 'ADJUSTMENT'
    private Integer referenceId;
    private String reason;
    private Timestamp createdAt;

    public InventoryTransaction() {}

    public InventoryTransaction(int id, int ingredientId, String transactionType,
                                BigDecimal quantity, String referenceType,
                                Integer referenceId, Timestamp createdAt) {
        this.id = id;
        this.ingredientId = ingredientId;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(int ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getTypeDisplayName() {
        if ("IN_PURCHASE".equalsIgnoreCase(transactionType)) return "Nhập mua hàng";
        if ("OUT_ORDER".equalsIgnoreCase(transactionType)) return "Xuất theo đơn bán";
        if ("ADJUSTMENT".equalsIgnoreCase(transactionType)) return "Điều chỉnh kho";
        if ("SPOILAGE".equalsIgnoreCase(transactionType)) return "Hao hụt / Hủy";
        return transactionType;
    }

    public String getFormattedQuantity() {
        if (quantity == null) return "0";
        if (isDecrease()) {
            return "-" + quantity.stripTrailingZeros().toPlainString() + " " + (unit != null ? unit : "");
        }
        return "+" + quantity.stripTrailingZeros().toPlainString() + " " + (unit != null ? unit : "");
    }
}
