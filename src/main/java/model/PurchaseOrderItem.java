package model;
import java.math.BigDecimal;
public record PurchaseOrderItem(int id,int poId,int ingredientId,BigDecimal quantity,BigDecimal unitPrice) {}
