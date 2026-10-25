package model;
import java.time.LocalDateTime;
/** TODO: add matching table to aura_cafe.sql before persistence is implemented. */
public record DeliveryIncident(int id,int orderId,int shipperId,String type,String description,String status,LocalDateTime createdAt) {}
