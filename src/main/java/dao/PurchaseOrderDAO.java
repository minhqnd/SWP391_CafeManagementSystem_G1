package dao;
import model.PurchaseOrder; import model.PurchaseOrderItem; import java.sql.SQLException; import java.util.List;
/** Persistence contract for purchase_orders and purchase_order_items. */
public class PurchaseOrderDAO {
 public List<PurchaseOrder> findAll(String status)throws SQLException{throw new UnsupportedOperationException("TODO");}
 public List<PurchaseOrderItem> findItems(int poId)throws SQLException{throw new UnsupportedOperationException("TODO");}
 public int create(PurchaseOrder order,List<PurchaseOrderItem> items)throws SQLException{throw new UnsupportedOperationException("TODO");}
 public boolean updateStatus(int id,String status)throws SQLException{throw new UnsupportedOperationException("TODO");}
 public boolean receiveIntoInventory(int id)throws SQLException{throw new UnsupportedOperationException("TODO: transaction");}
}
