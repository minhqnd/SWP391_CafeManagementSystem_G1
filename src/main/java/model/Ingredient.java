package model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Ingredient implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private Integer supplierId;
    private String supplierName;
    private String name;
    private String unit;
    private BigDecimal currentStock = BigDecimal.ZERO;
    private BigDecimal minStockLevel = BigDecimal.ZERO;
    private BigDecimal lastPurchasePrice;
    private int usedInRecipesCount;
    private boolean active = true;

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Ingredient() {}

    public Ingredient(int id, String name, String unit) {
        this.id = id;
        this.name = name;
        this.unit = unit;
    }

    public Ingredient(int id, Integer supplierId, String name, String unit,
                      BigDecimal currentStock, BigDecimal minStockLevel) {
        this.id = id;
        this.supplierId = supplierId;
        this.name = name;
        this.unit = unit;
        this.currentStock = currentStock != null ? currentStock : BigDecimal.ZERO;
        this.minStockLevel = minStockLevel != null ? minStockLevel : BigDecimal.ZERO;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getCurrentStock() { return currentStock; }
    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock != null ? currentStock : BigDecimal.ZERO;
    }

    public BigDecimal getMinStockLevel() { return minStockLevel; }
    public void setMinStockLevel(BigDecimal minStockLevel) {
        this.minStockLevel = minStockLevel != null ? minStockLevel : BigDecimal.ZERO;
    }

    public BigDecimal getLastPurchasePrice() { return lastPurchasePrice; }
    public void setLastPurchasePrice(BigDecimal lastPurchasePrice) { this.lastPurchasePrice = lastPurchasePrice; }

    public int getUsedInRecipesCount() { return usedInRecipesCount; }
    public void setUsedInRecipesCount(int usedInRecipesCount) { this.usedInRecipesCount = usedInRecipesCount; }

    public String getStockStatus() {
        if (currentStock == null || currentStock.compareTo(BigDecimal.ZERO) <= 0) {
            return "Hết hàng";
        }
        if (minStockLevel != null && currentStock.compareTo(minStockLevel) <= 0) {
            return "Sắp hết";
        }
        return "Đủ hàng";
    }

    public String getStockStatusBadgeClass() {
        String status = getStockStatus();
        if ("Hết hàng".equals(status)) {
            return "badge-danger";
        }
        if ("Sắp hết".equals(status)) {
            return "badge-warning";
        }
        return "badge-success";
    }
}
