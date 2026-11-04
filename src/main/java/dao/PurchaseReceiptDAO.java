package dao;

import model.PurchaseReceipt;
import model.PurchaseReceipt.Item;
import model.PurchaseOrder;
import model.PurchaseOrderItem;
import service.InventoryRules;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Lock the PO before every receipt/approval/correction so concurrent deliveries cannot over-write stock. */
public class PurchaseReceiptDAO extends BaseDAO {
    public List<PurchaseReceipt> findAll() throws SQLException,ClassNotFoundException {
        List<PurchaseReceipt> list=new ArrayList<>();Map<Integer,PurchaseReceipt> map=new LinkedHashMap<>();
        try(Connection c=getConnection();PreparedStatement p=c.prepareStatement("SELECT r.*,u.full_name receiver_name,v.full_name reviewer_name FROM purchase_receipts r JOIN users u ON u.id=r.receiver_id LEFT JOIN users v ON v.id=r.reviewer_id ORDER BY r.id DESC");ResultSet r=p.executeQuery()){
            while(r.next()){PurchaseReceipt receipt=new PurchaseReceipt();receipt.setId(r.getInt("id"));receipt.setPoId(r.getInt("po_id"));receipt.setReceiverId(r.getInt("receiver_id"));receipt.setReceiverName(r.getString("receiver_name"));receipt.setReviewerName(r.getString("reviewer_name"));receipt.setStatus(r.getString("status"));receipt.setNote(r.getString("note"));receipt.setReviewNote(r.getString("review_note"));receipt.setCreatedAt(r.getTimestamp("created_at"));list.add(receipt);map.put(receipt.getId(),receipt);}
            try(PreparedStatement ip=c.prepareStatement("SELECT ri.*,i.name,i.unit,COALESCE((SELECT SUM(a.quantity_delta) FROM purchase_receipt_adjustments a WHERE a.receipt_item_id=ri.id),0) adjustment FROM purchase_receipt_items ri JOIN purchase_order_items pi ON pi.id=ri.po_item_id JOIN ingredients i ON i.id=pi.ingredient_id ORDER BY ri.id");ResultSet rs=ip.executeQuery()){
                while(rs.next()){PurchaseReceipt receipt=map.get(rs.getInt("receipt_id"));if(receipt==null)continue;Item i=new Item(rs.getInt("po_item_id"),rs.getBigDecimal("delivered_quantity"),rs.getBigDecimal("accepted_quantity"));i.setId(rs.getInt("id"));i.setIngredientName(rs.getString("name"));i.setUnit(rs.getString("unit"));i.setAdjustment(rs.getBigDecimal("adjustment"));receipt.getItems().add(i);}
            }
        }return list;
    }
    public void populateReceived(List<PurchaseOrder> orders)throws SQLException,ClassNotFoundException {
        try(Connection c=getConnection()){
            for(PurchaseOrder po:orders)for(PurchaseOrderItem item:po.getItems())item.setReceivedQuantity(received(c,po.getId(),item.getId(),item.getIngredientId()));
        }
    }
    static BigDecimal received(Connection c,int poId,int itemId,int ingredient)throws SQLException {
        // Include legacy IN_PURCHASE entries; corrections are append-only, signed quantities.
        String sql="SELECT COALESCE((SELECT SUM(quantity) FROM inventory_transactions WHERE reference_type='PURCHASE_ORDER' AND reference_id=? AND ingredient_id=? AND transaction_type='IN_PURCHASE'),0) + COALESCE((SELECT SUM(a.quantity_delta) FROM purchase_receipt_adjustments a JOIN purchase_receipt_items ri ON ri.id=a.receipt_item_id WHERE ri.po_item_id=?),0)";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,poId);p.setInt(2,ingredient);p.setInt(3,itemId);try(ResultSet r=p.executeQuery()){r.next();return r.getBigDecimal(1);}}
    }
    public int submit(int poId,int actor,String key,String note,List<Item> items)throws SQLException,ClassNotFoundException {
        note=InventoryRules.text(note,true);validateKey(key);
        if(items==null||items.isEmpty()||items.size()>100)throw new IllegalArgumentException("Phiếu kiểm nhận phải có nguyên liệu.");
        Set<Integer> ids=new HashSet<>();BigDecimal delivered=BigDecimal.ZERO;
        for(Item i:items){InventoryRules.receipt(i.getDelivered(),i.getAccepted());if(!ids.add(i.getPoItemId()))throw new IllegalArgumentException("Nguyên liệu bị trùng.");delivered=delivered.add(i.getDelivered());}
        if(delivered.signum()==0)throw new IllegalArgumentException("Số lượng giao phải lớn hơn 0.");
        try(Connection c=getConnection()){c.setAutoCommit(false);try {
            InventoryWorkflowDAO.requireActor(c,actor,false);String state=lockPo(c,poId);
            try(PreparedStatement p=c.prepareStatement("SELECT id,po_id,receiver_id FROM purchase_receipts WHERE submission_key=?")){p.setString(1,key);try(ResultSet r=p.executeQuery()){if(r.next()){if(r.getInt(2)!=poId||r.getInt(3)!=actor)throw new IllegalArgumentException("Mã gửi phiếu không hợp lệ.");c.rollback();return r.getInt(1);}}}
            if(!List.of("ORDERED","PARTIALLY_RECEIVED").contains(state))throw new IllegalArgumentException("Đơn mua chưa đặt hoặc đã kết thúc.");
            for(Item item:items)try(PreparedStatement p=c.prepareStatement("SELECT id FROM purchase_order_items WHERE id=? AND po_id=?")){p.setInt(1,item.getPoItemId());p.setInt(2,poId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Nguyên liệu không thuộc đơn mua.");}}
            int id;try(PreparedStatement p=c.prepareStatement("INSERT INTO purchase_receipts(po_id,receiver_id,submission_key,note) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){p.setInt(1,poId);p.setInt(2,actor);p.setString(3,key);p.setString(4,note);p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){if(!r.next())throw new SQLException("Không tạo được phiếu kiểm nhận.");id=r.getInt(1);}}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO purchase_receipt_items(receipt_id,po_item_id,delivered_quantity,accepted_quantity) VALUES(?,?,?,?)")){
                for(Item i:items){p.setInt(1,id);p.setInt(2,i.getPoItemId());p.setBigDecimal(3,i.getDelivered());p.setBigDecimal(4,i.getAccepted());p.addBatch();}p.executeBatch();}
            InventoryWorkflowDAO.audit(c,"PURCHASE",poId,actor,"INSPECT","Phiếu #"+id+"; "+note);c.commit();return id;
        }catch(Exception e){c.rollback();throw e;}}
    }
    public void review(int id,int actor,boolean approve,String note)throws SQLException,ClassNotFoundException {review(id,actor,approve,note,false);}
    public void review(int id,int actor,boolean approve,String note,boolean acceptExcess)throws SQLException,ClassNotFoundException {
        note=InventoryRules.text(note,true);
        try(Connection c=getConnection()){c.setAutoCommit(false);try{
            InventoryWorkflowDAO.requireActor(c,actor,true);int poId=receiptPo(c,id);String poState=lockPo(c,poId);
            try(PreparedStatement p=c.prepareStatement("SELECT status FROM purchase_receipts WHERE id=? FOR UPDATE")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next()||!"PENDING".equals(r.getString(1)))throw new IllegalArgumentException("Phiếu đã được xử lý. Hãy tải lại.");}}
            if(approve&&!List.of("ORDERED","PARTIALLY_RECEIVED").contains(poState))throw new IllegalArgumentException("Đơn mua đã kết thúc, không thể cộng kho.");
            if(approve){
                try(PreparedStatement p=c.prepareStatement("SELECT pi.ingredient_id,ri.accepted_quantity,pi.id,pi.quantity FROM purchase_receipt_items ri JOIN purchase_order_items pi ON pi.id=ri.po_item_id WHERE ri.receipt_id=? ORDER BY pi.ingredient_id")){
                    p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next()){
                        BigDecimal qty=r.getBigDecimal(2);if(qty.signum()==0)continue;
                        if(received(c,poId,r.getInt(3),r.getInt(1)).add(qty).compareTo(r.getBigDecimal(4))>0&&!acceptExcess)
                            throw new IllegalArgumentException("Số lượng vượt đơn đặt. Quản lý phải xác nhận nhận phần thừa.");
                        changeStock(c,r.getInt(1),qty,"IN_PURCHASE","PURCHASE_ORDER",poId,"Phiếu kiểm nhận #"+id+"; "+note);
                    }}
                }
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE purchase_receipts SET status=?,reviewer_id=?,review_note=?,reviewed_at=NOW() WHERE id=? AND status='PENDING'")){
                p.setString(1,approve?"APPROVED":"REJECTED");p.setInt(2,actor);p.setString(3,note);p.setInt(4,id);if(p.executeUpdate()!=1)throw new SQLException("Phiếu đã thay đổi.");}
            if(approve)refreshPo(c,poId,poState);
            InventoryWorkflowDAO.audit(c,"PURCHASE",poId,actor,approve?"RECEIPT_APPROVE":"RECEIPT_REJECT","Phiếu #"+id+"; "+note);c.commit();
        }catch(Exception e){c.rollback();throw e;}}
    }
    public void adjust(int receiptItem,int actor,String key,BigDecimal delta,String reason)throws SQLException,ClassNotFoundException {
        reason=InventoryRules.text(reason,true);validateKey(key);
        if(delta==null||delta.signum()==0)throw new IllegalArgumentException("Số lượng điều chỉnh phải khác 0.");InventoryRules.quantity(delta.abs(),true);
        try(Connection c=getConnection()){c.setAutoCommit(false);try {
            InventoryWorkflowDAO.requireActor(c,actor,true);
            int poId,receipt;try(PreparedStatement p=c.prepareStatement("SELECT r.po_id,r.id FROM purchase_receipt_items ri JOIN purchase_receipts r ON r.id=ri.receipt_id WHERE ri.id=?")){p.setInt(1,receiptItem);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Không tìm thấy dòng kiểm nhận.");poId=r.getInt(1);receipt=r.getInt(2);}}
            String state=lockPo(c,poId);
            try(PreparedStatement p=c.prepareStatement("SELECT receipt_item_id,actor_id FROM purchase_receipt_adjustments WHERE submission_key=?")){p.setString(1,key);try(ResultSet r=p.executeQuery()){if(r.next()){if(r.getInt(1)!=receiptItem||r.getInt(2)!=actor)throw new IllegalArgumentException("Mã gửi điều chỉnh không hợp lệ.");c.rollback();return;}}}
            int ingredient;BigDecimal accepted;
            try(PreparedStatement p=c.prepareStatement("SELECT pi.ingredient_id,ri.accepted_quantity+COALESCE((SELECT SUM(a.quantity_delta) FROM purchase_receipt_adjustments a WHERE a.receipt_item_id=ri.id),0),r.status FROM purchase_receipt_items ri JOIN purchase_order_items pi ON pi.id=ri.po_item_id JOIN purchase_receipts r ON r.id=ri.receipt_id WHERE ri.id=? FOR UPDATE")){
                p.setInt(1,receiptItem);try(ResultSet r=p.executeQuery()){if(!r.next()||!"APPROVED".equals(r.getString(3)))throw new IllegalArgumentException("Chỉ điều chỉnh phiếu đã xác nhận.");ingredient=r.getInt(1);accepted=r.getBigDecimal(2);}}
            if(accepted.add(delta).signum()<0)throw new IllegalArgumentException("Không được giảm vượt số lượng đã nhận của phiếu.");
            int adjustmentId;try(PreparedStatement p=c.prepareStatement("INSERT INTO purchase_receipt_adjustments(receipt_item_id,actor_id,submission_key,quantity_delta,reason) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                p.setInt(1,receiptItem);p.setInt(2,actor);p.setString(3,key);p.setBigDecimal(4,delta);p.setString(5,reason);p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){if(!r.next())throw new SQLException("Không lưu được điều chỉnh.");adjustmentId=r.getInt(1);}}
            changeStock(c,ingredient,delta,"ADJUSTMENT","RECEIPT_ADJUSTMENT",adjustmentId,reason);
            refreshPo(c,poId,state);
            InventoryWorkflowDAO.audit(c,"PURCHASE",poId,actor,"RECEIPT_ADJUST","Phiếu #"+receipt+", dòng #"+receiptItem+"; delta="+delta+"; "+reason);c.commit();
        }catch(Exception e){c.rollback();throw e;}}
    }
    private static void validateKey(String key){try{UUID.fromString(key);}catch(Exception e){throw new IllegalArgumentException("Mã gửi phiếu không hợp lệ. Hãy tải lại trang.");}}
    private static int receiptPo(Connection c,int id)throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT po_id FROM purchase_receipts WHERE id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Không tìm thấy phiếu.");return r.getInt(1);}}
    }
    private static String lockPo(Connection c,int id)throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT status FROM purchase_orders WHERE id=? FOR UPDATE")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Không tìm thấy đơn mua.");return r.getString(1);}}
    }
    private static void refreshPo(Connection c,int poId,String previous)throws SQLException {
        if("CLOSED".equals(previous))return; // Explicitly closed remainder stays closed after corrections.
        boolean complete=true;BigDecimal total=BigDecimal.ZERO;
        try(PreparedStatement p=c.prepareStatement("SELECT id,ingredient_id,quantity FROM purchase_order_items WHERE po_id=?")){
            p.setInt(1,poId);try(ResultSet r=p.executeQuery()){while(r.next()){BigDecimal qty=received(c,poId,r.getInt(1),r.getInt(2));total=total.add(qty);if(qty.compareTo(r.getBigDecimal(3))<0)complete=false;}}
        }
        String state=complete?"RECEIVED":(total.signum()>0?"PARTIALLY_RECEIVED":"ORDERED");
        try(PreparedStatement p=c.prepareStatement("UPDATE purchase_orders SET status=?,received_at=IF(?='RECEIVED',NOW(),NULL) WHERE id=?")){p.setString(1,state);p.setString(2,state);p.setInt(3,poId);p.executeUpdate();}
    }
    private static void changeStock(Connection c,int ingredient,BigDecimal delta,String type,String ref,int refId,String reason)throws SQLException {
        // UPDATE locks the ingredient row and rejects returns exceeding available stock.
        try(PreparedStatement p=c.prepareStatement("UPDATE ingredients SET current_stock=current_stock+? WHERE id=? AND current_stock+?>=0")){
            p.setBigDecimal(1,delta);p.setInt(2,ingredient);p.setBigDecimal(3,delta);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Không đủ tồn kho để điều chỉnh/trả hàng.");}
        try(PreparedStatement p=c.prepareStatement("INSERT INTO inventory_transactions(ingredient_id,transaction_type,quantity,reference_type,reference_id,reason) VALUES(?,?,?,?,?,?)")){
            p.setInt(1,ingredient);p.setString(2,type);p.setBigDecimal(3,delta.abs());p.setString(4,ref);p.setInt(5,refId);p.setString(6,((delta.signum()<0?"Giảm: ":"Tăng: ")+reason).substring(0,Math.min(500,((delta.signum()<0?"Giảm: ":"Tăng: ")+reason).length())));p.executeUpdate();}
    }
}
