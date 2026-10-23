package dao;
import model.*; import java.sql.SQLException; import java.util.List;
public class DeliveryDAO {
 public List<Order> findAssignedOrders(int shipperId)throws SQLException{throw new UnsupportedOperationException("TODO");}
 public boolean claimOrder(int orderId,int shipperId)throws SQLException{throw new UnsupportedOperationException("TODO");}
 public boolean markDelivering(int orderId,int shipperId)throws SQLException{throw new UnsupportedOperationException("TODO");}
 public boolean completeDelivery(int orderId,int shipperId)throws SQLException{throw new UnsupportedOperationException("TODO: update order/payment transactionally");}
 public int reportIncident(DeliveryIncident incident)throws SQLException{throw new UnsupportedOperationException("TODO");}
 public List<DeliveryCodRecord> findCodRecords(int shipperId)throws SQLException{throw new UnsupportedOperationException("TODO");}
}
