package model;
import java.math.BigDecimal;import java.sql.Timestamp;import java.util.ArrayList;import java.util.List;
public class PurchaseOrder{
 private int id,supplierId,creatorId;private Integer restockRequestId;private String supplierName,creatorName,status,note;private BigDecimal totalCost=BigDecimal.ZERO;private Timestamp createdAt,orderedAt,receivedAt;private List<PurchaseOrderItem> items=new ArrayList<>();
 public int getId(){return id;}public void setId(int v){id=v;}public int getSupplierId(){return supplierId;}public void setSupplierId(int v){supplierId=v;}public int getCreatorId(){return creatorId;}public void setCreatorId(int v){creatorId=v;}
 public Integer getRestockRequestId(){return restockRequestId;}public void setRestockRequestId(Integer v){restockRequestId=v;}public String getSupplierName(){return supplierName;}public void setSupplierName(String v){supplierName=v;}public String getCreatorName(){return creatorName;}public void setCreatorName(String v){creatorName=v;}
 public String getStatus(){return status;}public void setStatus(String v){status=v;}public String getNote(){return note;}public void setNote(String v){note=v;}public BigDecimal getTotalCost(){return totalCost;}public void setTotalCost(BigDecimal v){totalCost=v;}
 public Timestamp getCreatedAt(){return createdAt;}public void setCreatedAt(Timestamp v){createdAt=v;}public Timestamp getOrderedAt(){return orderedAt;}public void setOrderedAt(Timestamp v){orderedAt=v;}public Timestamp getReceivedAt(){return receivedAt;}public void setReceivedAt(Timestamp v){receivedAt=v;}public List<PurchaseOrderItem> getItems(){return items;}public void setItems(List<PurchaseOrderItem> v){items=v;}
}
