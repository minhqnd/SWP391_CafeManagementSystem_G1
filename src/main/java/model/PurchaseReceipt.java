package model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

public class PurchaseReceipt {
    private int id,poId,receiverId;private String status,note,receiverName,reviewNote,reviewerName;
    private Timestamp createdAt;private List<Item> items=new ArrayList<>();
    public static class Item {
        private int id,poItemId;private String ingredientName,unit;
        private BigDecimal delivered,accepted,adjustment=BigDecimal.ZERO;
        public Item(){}
        public Item(int poItemId,BigDecimal delivered,BigDecimal accepted){this.poItemId=poItemId;this.delivered=delivered;this.accepted=accepted;}
        public int getId(){return id;}public void setId(int v){id=v;}
        public int getPoItemId(){return poItemId;}public void setPoItemId(int v){poItemId=v;}
        public String getIngredientName(){return ingredientName;}public void setIngredientName(String v){ingredientName=v;}
        public String getUnit(){return unit;}public void setUnit(String v){unit=v;}
        public BigDecimal getDelivered(){return delivered;}public void setDelivered(BigDecimal v){delivered=v;}
        public BigDecimal getAccepted(){return accepted;}public void setAccepted(BigDecimal v){accepted=v;}
        public BigDecimal getRejected(){return delivered.subtract(accepted);}
        public BigDecimal getAdjustment(){return adjustment;}public void setAdjustment(BigDecimal v){adjustment=v;}
    }
    public int getId(){return id;}public void setId(int v){id=v;}public int getPoId(){return poId;}public void setPoId(int v){poId=v;}
    public int getReceiverId(){return receiverId;}public void setReceiverId(int v){receiverId=v;}
    public String getStatus(){return status;}public void setStatus(String v){status=v;}public String getNote(){return note;}public void setNote(String v){note=v;}
    public String getReceiverName(){return receiverName;}public void setReceiverName(String v){receiverName=v;}
    public String getReviewNote(){return reviewNote;}public void setReviewNote(String v){reviewNote=v;}
    public String getReviewerName(){return reviewerName;}public void setReviewerName(String v){reviewerName=v;}
    public Timestamp getCreatedAt(){return createdAt;}public void setCreatedAt(Timestamp v){createdAt=v;}
    public List<Item> getItems(){return items;}
}
