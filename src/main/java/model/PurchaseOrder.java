package model;
import java.math.BigDecimal; import java.time.LocalDateTime;
public record PurchaseOrder(int id,int supplierId,int creatorId,BigDecimal totalCost,String status,LocalDateTime createdAt) {}
