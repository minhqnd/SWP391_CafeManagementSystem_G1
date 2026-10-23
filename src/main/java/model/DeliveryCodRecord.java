package model;
import java.math.BigDecimal; import java.time.LocalDateTime;
/** TODO: add matching table or derive reconciliation from payments. */
public record DeliveryCodRecord(int id,int orderId,int shipperId,BigDecimal amount,String status,LocalDateTime collectedAt) {}
