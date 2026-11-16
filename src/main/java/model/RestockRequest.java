package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RestockRequest implements Serializable {
    private int id;
    private int requesterId;
    private String requesterName;
    private Integer reviewerId;
    private Integer purchaseOrderId;
    private String reviewerName;
    private String status;
    private String reason;
    private String note;
    private String reviewNote;
    private Timestamp createdAt;
    private Timestamp reviewedAt;
    private List<RestockItem> items=new ArrayList<>();

    public static class RestockItem implements Serializable{
        private int id,requestId,ingredientId;private String ingredientName,unit,itemNote;private BigDecimal quantity=BigDecimal.ZERO,unitPrice=BigDecimal.ZERO;
        public RestockItem(){}
        public RestockItem(int ingredientId,String ingredientName,String unit,BigDecimal quantity){this.ingredientId=ingredientId;this.ingredientName=ingredientName;this.unit=unit;this.quantity=quantity;}
        public RestockItem(int ingredientId,String ingredientName,String unit,BigDecimal quantity,BigDecimal unitPrice){this(ingredientId,ingredientName,unit,quantity);this.unitPrice=unitPrice;}
        public int getId(){return id;}public void setId(int v){id=v;}public int getRequestId(){return requestId;}public void setRequestId(int v){requestId=v;}
        public int getIngredientId(){return ingredientId;}public void setIngredientId(int v){ingredientId=v;}public String getIngredientName(){return ingredientName;}public void setIngredientName(String v){ingredientName=v;}
        public String getUnit(){return unit;}public void setUnit(String v){unit=v;}public BigDecimal getQuantity(){return quantity;}public void setQuantity(BigDecimal v){quantity=v;}
        public String getItemNote(){return itemNote;}public void setItemNote(String v){itemNote=v;}
        public BigDecimal getUnitPrice(){return unitPrice;}public void setUnitPrice(BigDecimal v){unitPrice=v;}
        public BigDecimal getSubtotal(){return quantity.multiply(unitPrice);}
    }
    public int getId(){return id;}public void setId(int v){id=v;}public int getRequesterId(){return requesterId;}public void setRequesterId(int v){requesterId=v;}
    public String getRequesterName(){return requesterName;}public void setRequesterName(String v){requesterName=v;}public Integer getReviewerId(){return reviewerId;}public void setReviewerId(Integer v){reviewerId=v;}
    public String getReviewerName(){return reviewerName;}public void setReviewerName(String v){reviewerName=v;}public String getStatus(){return status;}public void setStatus(String v){status=v;}
    public Integer getPurchaseOrderId(){return purchaseOrderId;}public void setPurchaseOrderId(Integer v){purchaseOrderId=v;}
    public String getReason(){return reason;}public void setReason(String v){reason=v;}public String getNote(){return note;}public void setNote(String v){note=v;}
    public String getReviewNote(){return reviewNote;}public void setReviewNote(String v){reviewNote=v;}public Timestamp getCreatedAt(){return createdAt;}public void setCreatedAt(Timestamp v){createdAt=v;}
    public Timestamp getReviewedAt(){return reviewedAt;}public void setReviewedAt(Timestamp v){reviewedAt=v;}public List<RestockItem> getItems(){return items;}public void setItems(List<RestockItem> v){items=v;}
    public String getItemsSummary(){StringBuilder s=new StringBuilder();for(RestockItem i:items){if(s.length()>0)s.append(", ");s.append(i.getIngredientName()).append(": ").append(i.getQuantity().stripTrailingZeros().toPlainString()).append(" ").append(i.getUnit());if(i.getUnitPrice()!=null&&i.getUnitPrice().signum()>0)s.append(" x ").append(String.format(java.util.Locale.US,"%,.0f",i.getUnitPrice())).append("đ");}return s.toString();}
}
