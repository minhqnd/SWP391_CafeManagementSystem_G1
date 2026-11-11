package model;
import java.math.BigDecimal;
public class PurchaseOrderItem{
 private int id,poId,ingredientId;private String ingredientName,unit;private BigDecimal quantity=BigDecimal.ZERO,unitPrice=BigDecimal.ZERO;
 public PurchaseOrderItem(){}public PurchaseOrderItem(int ingredientId,BigDecimal quantity,BigDecimal unitPrice){this.ingredientId=ingredientId;this.quantity=quantity;this.unitPrice=unitPrice;}
 public int getId(){return id;}public void setId(int v){id=v;}public int getPoId(){return poId;}public void setPoId(int v){poId=v;}public int getIngredientId(){return ingredientId;}public void setIngredientId(int v){ingredientId=v;}
 public String getIngredientName(){return ingredientName;}public void setIngredientName(String v){ingredientName=v;}public String getUnit(){return unit;}public void setUnit(String v){unit=v;}public BigDecimal getQuantity(){return quantity;}public void setQuantity(BigDecimal v){quantity=v;}public BigDecimal getUnitPrice(){return unitPrice;}public void setUnitPrice(BigDecimal v){unitPrice=v;}
 private BigDecimal receivedQuantity=BigDecimal.ZERO;
 public BigDecimal getReceivedQuantity(){return receivedQuantity;}public void setReceivedQuantity(BigDecimal v){receivedQuantity=v;}
 public BigDecimal getRemainingQuantity(){return quantity.subtract(receivedQuantity).max(BigDecimal.ZERO);}
 public BigDecimal getSubtotal(){return quantity.multiply(unitPrice).setScale(2,java.math.RoundingMode.HALF_UP);}
}
