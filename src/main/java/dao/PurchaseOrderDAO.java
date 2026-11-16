package dao;
import model.PurchaseOrder;import model.PurchaseOrderItem;import java.math.BigDecimal;import java.sql.*;import java.util.*;
public class PurchaseOrderDAO extends BaseDAO{
 public List<PurchaseOrder> findAll(String status)throws SQLException{
  List<PurchaseOrder> list=new ArrayList<>();Map<Integer,PurchaseOrder> map=new LinkedHashMap<>();String f=status==null||status.isBlank()?"ALL":status.toUpperCase();
  try(Connection c=getConnection();PreparedStatement ps=c.prepareStatement("SELECT po.*,s.name supplier_name,u.full_name creator_name FROM purchase_orders po JOIN suppliers s ON s.id=po.supplier_id JOIN users u ON u.id=po.creator_id WHERE (?='ALL' OR po.status=?) ORDER BY po.created_at DESC")){
   ps.setString(1,f);ps.setString(2,f);try(ResultSet rs=ps.executeQuery()){while(rs.next()){PurchaseOrder o=map(rs);list.add(o);map.put(o.getId(),o);}}
   try(PreparedStatement ip=c.prepareStatement("SELECT pi.*,i.name ingredient_name,i.unit FROM purchase_order_items pi JOIN ingredients i ON i.id=pi.ingredient_id ORDER BY pi.id");ResultSet rs=ip.executeQuery()){while(rs.next()){PurchaseOrder o=map.get(rs.getInt("po_id"));if(o!=null)o.getItems().add(mapItem(rs));}}
  }catch(ClassNotFoundException e){throw new SQLException(e);}return list;
 }
 public List<PurchaseOrderItem> findItems(int poId)throws SQLException {
  try(Connection c=getConnection()){return findItems(c,poId);}
  catch(ClassNotFoundException e){throw new SQLException(e);}
 }
 private List<PurchaseOrderItem> findItems(Connection c,int poId)throws SQLException {
  List<PurchaseOrderItem> items=new ArrayList<>();
  try(PreparedStatement p=c.prepareStatement("SELECT pi.*,i.name ingredient_name,i.unit FROM purchase_order_items pi JOIN ingredients i ON i.id=pi.ingredient_id WHERE po_id=? ORDER BY pi.id")){
   p.setInt(1,poId);
   try(ResultSet r=p.executeQuery()){while(r.next())items.add(mapItem(r));}
  }
  return items;
 }
 public int create(PurchaseOrder order,List<PurchaseOrderItem> items)throws SQLException {
  return create(order,items,false);
 }
 public int create(PurchaseOrder order,List<PurchaseOrderItem> items,boolean placed)throws SQLException {
  if(items==null||items.isEmpty())throw new IllegalArgumentException("Phiếu nhập phải có nguyên liệu.");
  order.setNote(service.InventoryRules.text(order.getNote(),false));
  Set<Integer> unique=new HashSet<>();for(PurchaseOrderItem i:items){if(!unique.add(i.getIngredientId()))throw new IllegalArgumentException("Nguyên liệu bị trùng.");if(i.getUnitPrice()==null||i.getUnitPrice().signum()<0||i.getUnitPrice().scale()>service.InventoryRules.PURCHASE_PRICE_SCALE||i.getUnitPrice().compareTo(new BigDecimal("100000000000000"))>=0)throw new IllegalArgumentException("Đơn giá không hợp lệ.");}
  BigDecimal total=items.stream().map(PurchaseOrderItem::getSubtotal).reduce(BigDecimal.ZERO,BigDecimal::add);
  service.InventoryRules.money(total);
  try(Connection c=getConnection()){c.setAutoCommit(false);try{int id;
   InventoryWorkflowDAO.requireActor(c,order.getCreatorId(),true);requireSupplier(c,order.getSupplierId());
   for(PurchaseOrderItem item:items) {IngredientDAO.requireActive(c,item.getIngredientId());service.InventoryRules.quantity(item.getQuantity(),true);}
   if(order.getRestockRequestId()!=null){try(PreparedStatement check=c.prepareStatement("SELECT status FROM restock_requests WHERE id=? FOR UPDATE")){check.setInt(1,order.getRestockRequestId());try(ResultSet rs=check.executeQuery()){if(!rs.next()||!"APPROVED".equals(rs.getString(1)))throw new IllegalArgumentException("Chỉ đề xuất đã duyệt mới được chuyển thành phiếu nhập.");}}}
   if(order.getRestockRequestId()!=null){
    try(PreparedStatement p=c.prepareStatement("SELECT id FROM purchase_orders WHERE restock_request_id=? FOR UPDATE")){
     p.setInt(1,order.getRestockRequestId());
     try(ResultSet r=p.executeQuery()){
      if(r.next())throw new IllegalArgumentException("Đề xuất này đã có phiếu mua PO-"+r.getInt(1)+".");
     }
    }
    Set<Integer> requested=new HashSet<>();try(PreparedStatement p=c.prepareStatement("SELECT ingredient_id FROM restock_request_items WHERE request_id=?")){p.setInt(1,order.getRestockRequestId());try(ResultSet r=p.executeQuery()){while(r.next())requested.add(r.getInt(1));}}
    if(!requested.equals(unique))throw new IllegalArgumentException("Đơn mua phải chứa đúng các nguyên liệu trong đề xuất đã duyệt.");
   }
   try(PreparedStatement ps=c.prepareStatement("INSERT INTO purchase_orders(supplier_id,creator_id,restock_request_id,total_cost,status,note) VALUES(?,?,?,?,'DRAFT',?)",Statement.RETURN_GENERATED_KEYS)){ps.setInt(1,order.getSupplierId());ps.setInt(2,order.getCreatorId());if(order.getRestockRequestId()==null)ps.setNull(3,Types.INTEGER);else ps.setInt(3,order.getRestockRequestId());ps.setBigDecimal(4,total);ps.setString(5,order.getNote());ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(!rs.next())throw new SQLException("Không tạo được phiếu nhập.");id=rs.getInt(1);}}
   try(PreparedStatement ps=c.prepareStatement("INSERT INTO purchase_order_items(po_id,ingredient_id,quantity,unit_price) VALUES(?,?,?,?)")){for(PurchaseOrderItem i:items){if(i.getQuantity().signum()<=0||i.getUnitPrice().signum()<0)throw new IllegalArgumentException("Số lượng hoặc đơn giá không hợp lệ.");ps.setInt(1,id);ps.setInt(2,i.getIngredientId());ps.setBigDecimal(3,i.getQuantity());ps.setBigDecimal(4,i.getUnitPrice());ps.addBatch();}ps.executeBatch();}InventoryWorkflowDAO.audit(c,"PURCHASE",id,order.getCreatorId(),"CREATE","supplier="+order.getSupplierId()+"; total="+total+"; request="+order.getRestockRequestId()+"; items="+items.stream().map(i->i.getIngredientId()+":"+i.getQuantity()+"@"+i.getUnitPrice()).toList());
   if(placed)changeStatus(c,id,order.getCreatorId(),"order","Đã đặt hàng với nhà cung cấp");
   c.commit();return id;
  }catch(Exception e){c.rollback();if(e instanceof SQLException)throw(SQLException)e;if(e instanceof RuntimeException)throw(RuntimeException)e;throw new SQLException(e);}}
  catch(ClassNotFoundException e){throw new SQLException(e);}
 }
 @Deprecated public boolean updateStatus(int id,String status)throws SQLException {
  throw new IllegalArgumentException("Hãy dùng thao tác quản lý có kiểm tra quyền và lịch sử.");
 }
 /** Receipt quantities must be inspected and approved; the old all-at-once shortcut is disabled. */
 @Deprecated public boolean receiveIntoInventory(int id)throws SQLException {
  throw new IllegalArgumentException("Hãy lập phiếu kiểm nhận với số lượng thực nhận.");
 }
 public void editDraft(int id,int actor,int supplierId,BigDecimal quantity,BigDecimal total,String note,String reason)throws SQLException,ClassNotFoundException {
  service.InventoryRules.quantity(quantity,true);note=service.InventoryRules.text(note,false);reason=service.InventoryRules.text(reason,true);
  if(total==null||total.signum()<0||total.scale()>2||total.compareTo(new BigDecimal("9999999999.99"))>0)throw new IllegalArgumentException("Tổng tiền không hợp lệ.");
  try(Connection c=getConnection()){c.setAutoCommit(false);try{
   InventoryWorkflowDAO.requireActor(c,actor,true);String before;
   try(PreparedStatement p=c.prepareStatement("SELECT status,supplier_id,total_cost,note FROM purchase_orders WHERE id=? FOR UPDATE")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next()||!"DRAFT".equals(r.getString(1)))throw new IllegalArgumentException("Chỉ được sửa đơn mua nháp.");before=r.getInt(2)+"; "+r.getBigDecimal(3)+"; "+r.getString(4);}}
   requireSupplier(c,supplierId);
   List<PurchaseOrderItem> lines=new ArrayList<>();try(PreparedStatement p=c.prepareStatement("SELECT id,ingredient_id,quantity,unit_price FROM purchase_order_items WHERE po_id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next()){PurchaseOrderItem i=new PurchaseOrderItem(r.getInt(2),r.getBigDecimal(3),r.getBigDecimal(4));i.setId(r.getInt(1));lines.add(i);}}}
   if(lines.size()!=1)throw new IllegalArgumentException("Đơn nhiều nguyên liệu cần giữ nguyên; hãy hủy nháp và lập lại.");
   PurchaseOrderItem line=lines.get(0);IngredientDAO.requireActive(c,line.getIngredientId());before+="; quantity="+line.getQuantity()+"; unitPrice="+line.getUnitPrice();
   BigDecimal price=service.InventoryRules.purchaseUnitPrice(quantity,total);BigDecimal storedTotal=service.InventoryRules.money(total);
   try(PreparedStatement p=c.prepareStatement("UPDATE purchase_order_items SET quantity=?,unit_price=? WHERE id=?")){p.setBigDecimal(1,quantity);p.setBigDecimal(2,price);p.setInt(3,line.getId());p.executeUpdate();}
   try(PreparedStatement p=c.prepareStatement("UPDATE purchase_orders SET supplier_id=?,total_cost=?,note=? WHERE id=?")){p.setInt(1,supplierId);p.setBigDecimal(2,storedTotal);p.setString(3,note);p.setInt(4,id);p.executeUpdate();}
   InventoryWorkflowDAO.audit(c,"PURCHASE",id,actor,"EDIT",reason+" | Trước: "+before+" | Sau: supplier="+supplierId+"; quantity="+quantity+"; total="+storedTotal+"; "+note);c.commit();
  }catch(Exception e){c.rollback();throw e;}}
 }
 public void updatePendingOrder(int id,int actor,int supplierId,BigDecimal quantity,BigDecimal total,String note,String reason)throws SQLException,ClassNotFoundException {
  service.InventoryRules.quantity(quantity,true);note=service.InventoryRules.text(note,false);
  reason=(reason==null||reason.isBlank())?"Cập nhật thông tin phiếu nhập":service.InventoryRules.text(reason,false);
  if(total==null||total.signum()<0||total.scale()>2||total.compareTo(new BigDecimal("9999999999.99"))>0)throw new IllegalArgumentException("Tổng tiền không hợp lệ.");
  try(Connection c=getConnection()){c.setAutoCommit(false);try{
   InventoryWorkflowDAO.requireActor(c,actor,true);String before;
   try(PreparedStatement p=c.prepareStatement("SELECT status,supplier_id,total_cost,note FROM purchase_orders WHERE id=? FOR UPDATE")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next()||!List.of("DRAFT","ORDERED").contains(r.getString(1)))throw new IllegalArgumentException("Chỉ được sửa phiếu nhập khi chưa nhận hàng.");before=r.getInt(2)+"; "+r.getBigDecimal(3)+"; "+r.getString(4);}}
   requireSupplier(c,supplierId);
   List<PurchaseOrderItem> lines=new ArrayList<>();try(PreparedStatement p=c.prepareStatement("SELECT id,ingredient_id,quantity,unit_price FROM purchase_order_items WHERE po_id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next()){PurchaseOrderItem i=new PurchaseOrderItem(r.getInt(2),r.getBigDecimal(3),r.getBigDecimal(4));i.setId(r.getInt(1));lines.add(i);}}}
   if(lines.size()!=1)throw new IllegalArgumentException("Chỉ sửa được phiếu nhập có một nguyên liệu.");
   PurchaseOrderItem line=lines.get(0);IngredientDAO.requireActive(c,line.getIngredientId());before+="; quantity="+line.getQuantity()+"; unitPrice="+line.getUnitPrice();
   BigDecimal price=service.InventoryRules.purchaseUnitPrice(quantity,total);BigDecimal storedTotal=service.InventoryRules.money(total);
   try(PreparedStatement p=c.prepareStatement("UPDATE purchase_order_items SET quantity=?,unit_price=? WHERE id=?")){p.setBigDecimal(1,quantity);p.setBigDecimal(2,price);p.setInt(3,line.getId());p.executeUpdate();}
   try(PreparedStatement p=c.prepareStatement("UPDATE purchase_orders SET supplier_id=?,total_cost=?,note=? WHERE id=?")){p.setInt(1,supplierId);p.setBigDecimal(2,storedTotal);p.setString(3,note);p.setInt(4,id);p.executeUpdate();}
   InventoryWorkflowDAO.audit(c,"PURCHASE",id,actor,"EDIT",reason+" | Trước: "+before+" | Sau: supplier="+supplierId+"; quantity="+quantity+"; total="+storedTotal+"; "+note);c.commit();
  }catch(Exception e){c.rollback();throw e;}}
 }
 static void requireSupplier(Connection c,int id)throws SQLException {
  try(PreparedStatement p=c.prepareStatement("SELECT status FROM suppliers WHERE id=? FOR UPDATE")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next()||!"ACTIVE".equals(r.getString(1)))throw new IllegalArgumentException("Nhà cung cấp phải đang hoạt động.");}}
 }
 public boolean changeStatus(int id,int actor,String action,String reason)throws SQLException,ClassNotFoundException {
  try(Connection c=getConnection()){c.setAutoCommit(false);try{
   changeStatus(c,id,actor,action,reason);
   c.commit();return true;
  }catch(Exception e){c.rollback();throw e;}}
 }
 static void changeStatus(Connection c,int id,int actor,String action,String reason)throws SQLException {
   reason=service.InventoryRules.text(reason,!"order".equals(action));
   InventoryWorkflowDAO.requireActor(c,actor,true);String state;int supplier;
   try(PreparedStatement p=c.prepareStatement("SELECT status,supplier_id FROM purchase_orders WHERE id=? FOR UPDATE")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Không tìm thấy đơn mua.");state=r.getString(1);supplier=r.getInt(2);}}
   String next;
   if("order".equals(action)&&"DRAFT".equals(state)){requireSupplier(c,supplier);next="ORDERED";}
   else if("cancel".equals(action)&&List.of("DRAFT","ORDERED").contains(state))next="CANCELLED";
   else if("close".equals(action)&&List.of("ORDERED","PARTIALLY_RECEIVED").contains(state))next="CLOSED";
   else throw new IllegalArgumentException("Không thể thay đổi đơn ở trạng thái hiện tại.");
   try(PreparedStatement p=c.prepareStatement("SELECT id FROM purchase_receipts WHERE po_id=? AND status='PENDING'")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(r.next())throw new IllegalArgumentException("Hãy xử lý các phiếu kiểm nhận đang chờ trước.");}}
   try(PreparedStatement p=c.prepareStatement("UPDATE purchase_orders SET status=?,ordered_at=IF(?='ORDERED',NOW(),ordered_at) WHERE id=?")){p.setString(1,next);p.setString(2,next);p.setInt(3,id);p.executeUpdate();}
   InventoryWorkflowDAO.audit(c,"PURCHASE",id,actor,action,state+" -> "+next+"; "+reason);
 }
 private PurchaseOrder map(ResultSet rs)throws SQLException{PurchaseOrder o=new PurchaseOrder();o.setId(rs.getInt("id"));o.setSupplierId(rs.getInt("supplier_id"));o.setCreatorId(rs.getInt("creator_id"));o.setRestockRequestId((Integer)rs.getObject("restock_request_id"));o.setSupplierName(rs.getString("supplier_name"));o.setCreatorName(rs.getString("creator_name"));o.setTotalCost(rs.getBigDecimal("total_cost"));o.setStatus(rs.getString("status"));o.setNote(rs.getString("note"));o.setCreatedAt(rs.getTimestamp("created_at"));o.setOrderedAt(rs.getTimestamp("ordered_at"));o.setReceivedAt(rs.getTimestamp("received_at"));return o;}
 private PurchaseOrderItem mapItem(ResultSet rs)throws SQLException{PurchaseOrderItem i=new PurchaseOrderItem();i.setId(rs.getInt("id"));i.setPoId(rs.getInt("po_id"));i.setIngredientId(rs.getInt("ingredient_id"));i.setIngredientName(rs.getString("ingredient_name"));i.setUnit(rs.getString("unit"));i.setQuantity(rs.getBigDecimal("quantity"));i.setUnitPrice(rs.getBigDecimal("unit_price"));return i;}
}
