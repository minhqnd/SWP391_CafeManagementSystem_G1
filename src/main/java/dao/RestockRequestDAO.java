package dao;

import model.Ingredient;
import service.InventoryRules;
import model.RestockRequest;
import model.RestockRequest.RestockItem;
import java.sql.*;
import java.util.*;

public class RestockRequestDAO extends BaseDAO{
    public List<Ingredient> getLowStockIngredients(){
        List<Ingredient> list=new ArrayList<>();String sql="SELECT i.*,s.name supplier_name FROM ingredients i LEFT JOIN suppliers s ON s.id=i.supplier_id WHERE i.is_active=1 AND i.current_stock<=i.min_stock_level ORDER BY (i.min_stock_level-i.current_stock) DESC";
        try(Connection c=getConnection();PreparedStatement ps=c.prepareStatement(sql);ResultSet rs=ps.executeQuery()){
            while(rs.next()){Ingredient i=new Ingredient(rs.getInt("id"),(Integer)rs.getObject("supplier_id"),rs.getString("name"),rs.getString("unit"),rs.getBigDecimal("current_stock"),rs.getBigDecimal("min_stock_level"));i.setSupplierName(rs.getString("supplier_name"));list.add(i);}
        }catch(Exception e){throw new IllegalStateException("Cannot load low stock",e);}return list;
    }
    public List<RestockRequest> getRestockRequests(String status){
        List<RestockRequest> list=new ArrayList<>();Map<Integer,RestockRequest> map=new LinkedHashMap<>();
        String sql="SELECT r.*,u.full_name requester_name,rv.full_name reviewer_name FROM restock_requests r JOIN users u ON u.id=r.requester_id LEFT JOIN users rv ON rv.id=r.reviewer_id WHERE (?='ALL' OR r.status=?) ORDER BY r.created_at DESC";
        try(Connection c=getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            String filter=status==null||status.isBlank()?"ALL":status.toUpperCase();ps.setString(1,filter);ps.setString(2,filter);
            try(ResultSet rs=ps.executeQuery()){while(rs.next()){RestockRequest r=new RestockRequest();r.setId(rs.getInt("id"));r.setRequesterId(rs.getInt("requester_id"));r.setRequesterName(rs.getString("requester_name"));r.setReviewerId((Integer)rs.getObject("reviewer_id"));r.setReviewerName(rs.getString("reviewer_name"));r.setStatus(rs.getString("status"));r.setReason(rs.getString("reason"));r.setNote(rs.getString("note"));r.setReviewNote(rs.getString("review_note"));r.setCreatedAt(rs.getTimestamp("created_at"));r.setReviewedAt(rs.getTimestamp("reviewed_at"));list.add(r);map.put(r.getId(),r);}}
            if(!map.isEmpty()){try(PreparedStatement ip=c.prepareStatement("SELECT ri.*,i.name ingredient_name,i.unit FROM restock_request_items ri JOIN ingredients i ON i.id=ri.ingredient_id ORDER BY ri.id");ResultSet rs=ip.executeQuery()){while(rs.next()){RestockRequest r=map.get(rs.getInt("request_id"));if(r!=null){RestockItem it=new RestockItem(rs.getInt("ingredient_id"),rs.getString("ingredient_name"),rs.getString("unit"),rs.getBigDecimal("quantity"));it.setId(rs.getInt("id"));it.setRequestId(rs.getInt("request_id"));it.setItemNote(rs.getString("item_note"));r.getItems().add(it);}}}}
        }catch(Exception e){throw new IllegalStateException("Cannot load restock requests",e);}return list;
    }
    public int createRestockRequest(int requesterId,String status,String reason,String note,List<RestockItem> items)throws SQLException,ClassNotFoundException{
        if(items==null||items.isEmpty())throw new IllegalArgumentException("Đề xuất phải có ít nhất một nguyên liệu.");
        String initial="DRAFT".equals(status)?"DRAFT":"PENDING_APPROVAL";
        reason=InventoryRules.text(reason,true);note=InventoryRules.text(note,false);
        for(RestockItem item:items) InventoryRules.quantity(item.getQuantity(),true);
        try(Connection c=getConnection()){c.setAutoCommit(false);try{
            InventoryWorkflowDAO.requireActor(c,requesterId,false);
            for(RestockItem item:items) IngredientDAO.requireActive(c,item.getIngredientId());
            int id;try(PreparedStatement ps=c.prepareStatement("INSERT INTO restock_requests(requester_id,status,reason,note) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){ps.setInt(1,requesterId);ps.setString(2,initial);ps.setString(3,reason);ps.setString(4,note);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(!rs.next())throw new SQLException("Không tạo được đề xuất.");id=rs.getInt(1);}}
            try(PreparedStatement ps=c.prepareStatement("INSERT INTO restock_request_items(request_id,ingredient_id,quantity,item_note) VALUES(?,?,?,?)")){for(RestockItem i:items){if(i.getQuantity()==null||i.getQuantity().signum()<=0)throw new IllegalArgumentException("Số lượng đề xuất phải lớn hơn 0.");ps.setInt(1,id);ps.setInt(2,i.getIngredientId());ps.setBigDecimal(3,i.getQuantity());ps.setString(4,i.getItemNote());ps.addBatch();}ps.executeBatch();}
            InventoryWorkflowDAO.audit(c,"RESTOCK",id,requesterId,"CREATE",initial+"; "+reason+"; "+note+"; "+itemsDescription(items));
            c.commit();return id;}catch(Exception e){c.rollback();throw e;}}
    }
    public List<RestockRequest> findForActor(boolean admin,int actor) {
        // Filtering also protects direct links to another employee's requests.
        return getRestockRequests("ALL").stream().filter(r->admin || r.getRequesterId()==actor).toList();
    }
    public boolean edit(int id,int actor,boolean admin,String reason,String note,List<RestockItem> items,String editReason)
            throws SQLException,ClassNotFoundException {
        reason=InventoryRules.text(reason,true);note=InventoryRules.text(note,false);
        editReason=InventoryRules.text(editReason,true);
        if(items==null||items.isEmpty())throw new IllegalArgumentException("Đề xuất phải có nguyên liệu.");
        for(RestockItem i:items)InventoryRules.quantity(i.getQuantity(),true);
        try(Connection c=getConnection()){c.setAutoCommit(false);try {
            lockRequest(c,id,actor,admin,false);
            String before=describe(c,id);
            for(RestockItem i:items)IngredientDAO.requireActive(c,i.getIngredientId());
            try(PreparedStatement p=c.prepareStatement("DELETE FROM restock_request_items WHERE request_id=?")){p.setInt(1,id);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO restock_request_items(request_id,ingredient_id,quantity) VALUES(?,?,?)")){
                for(RestockItem i:items){p.setInt(1,id);p.setInt(2,i.getIngredientId());p.setBigDecimal(3,i.getQuantity());p.addBatch();}p.executeBatch();}
            try(PreparedStatement p=c.prepareStatement("UPDATE restock_requests SET reason=?,note=? WHERE id=?")){p.setString(1,reason);p.setString(2,note);p.setInt(3,id);p.executeUpdate();}
            InventoryWorkflowDAO.audit(c,"RESTOCK",id,actor,"EDIT",editReason+" | Trước: "+before+" | Sau: "+reason+"; "+note+"; "+itemsDescription(items));
            c.commit();return true;
        }catch(Exception e){c.rollback();throw e;}}
    }
    public boolean transition(int id,int actor,String action,String reviewNote)throws SQLException,ClassNotFoundException {
        // Legacy caller is an Admin; use the same guarded transition implementation.
        return transition(id,actor,true,action,reviewNote);
    }
    public boolean transition(int id,int actor,boolean admin,String action,String reviewNote)throws SQLException,ClassNotFoundException {
        boolean review=List.of("approve","reject","requestChanges").contains(action);
        reviewNote=InventoryRules.text(reviewNote,!"approve".equals(action) && review);
        if(review&&!admin)throw new IllegalArgumentException("Chỉ quản lý được duyệt đề xuất.");
        try(Connection c=getConnection()){c.setAutoCommit(false);try{
            String current=lockRequest(c,id,actor,admin,review);
            String target;
            if(review&&"PENDING_APPROVAL".equals(current))target=switch(action){case "approve"->"APPROVED";case "reject"->"REJECTED";default->"CHANGES_REQUESTED";};
            else if("submit".equals(action)&&List.of("DRAFT","CHANGES_REQUESTED").contains(current))target="PENDING_APPROVAL";
            else if("cancel".equals(action)&&InventoryRules.editableRequest(current))target="CANCELLED";
            else throw new IllegalArgumentException("Trạng thái đề xuất không cho phép thao tác này.");
            try(PreparedStatement p=c.prepareStatement("UPDATE restock_requests SET status=?,reviewer_id=?,review_note=?,reviewed_at=NOW() WHERE id=?")){
                p.setString(1,target);if(review)p.setInt(2,actor);else p.setNull(2,Types.INTEGER);p.setString(3,reviewNote);p.setInt(4,id);p.executeUpdate();}
            InventoryWorkflowDAO.audit(c,"RESTOCK",id,actor,action,current+" -> "+target+"; "+reviewNote);
            c.commit();return true;
        }catch(Exception e){c.rollback();throw e;}}
    }
    private String lockRequest(Connection c,int id,int actor,boolean admin,boolean review)throws SQLException {
        InventoryWorkflowDAO.requireActor(c,actor,admin);
        try(PreparedStatement p=c.prepareStatement("SELECT requester_id,status FROM restock_requests WHERE id=? FOR UPDATE")){
            p.setInt(1,id);try(ResultSet r=p.executeQuery()){
                if(!r.next())throw new IllegalArgumentException("Không tìm thấy đề xuất.");
                int owner=r.getInt("requester_id");String status=r.getString("status");
                if(!admin && owner!=actor)throw new IllegalArgumentException("Bạn chỉ được sửa đề xuất của mình.");
                if(review&&owner==actor)throw new IllegalArgumentException("Không được tự duyệt đề xuất của mình.");
                if(!InventoryRules.editableRequest(status))throw new IllegalArgumentException("Đề xuất đã kết thúc, không thể thay đổi.");
                if(!admin&&"PENDING_APPROVAL".equals(status))throw new IllegalArgumentException("Đề xuất đang chờ quản lý duyệt.");
                return status;
            }
        }
    }
    private String describe(Connection c,int id)throws SQLException {
        StringBuilder s=new StringBuilder();
        try(PreparedStatement p=c.prepareStatement("SELECT r.reason,r.note,i.ingredient_id,i.quantity FROM restock_requests r JOIN restock_request_items i ON i.request_id=r.id WHERE r.id=?")){
            p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next())s.append(r.getString(1)).append("; ").append(r.getString(2)).append("; ").append(r.getInt(3)).append(": ").append(r.getBigDecimal(4)).append("; ");}}
        return s.toString();
    }
    private static String itemsDescription(List<RestockItem> items){return items.stream().map(i->i.getIngredientId()+": "+i.getQuantity()).toList().toString();}
    public boolean transitionFromUi(int id,int actor,boolean admin,String action,String reviewNote)throws SQLException,ClassNotFoundException {
        boolean review=List.of("approve","reject","requestChanges").contains(action);
        if(reviewNote==null||reviewNote.trim().length()<3){
            reviewNote=switch(action==null?"":action){
                case "approve"->"Đã duyệt đề xuất nhập hàng";
                case "reject"->"Từ chối đề xuất nhập hàng";
                case "requestChanges"->"Yêu cầu điều chỉnh lại số lượng đề xuất";
                default->"Cập nhật trạng thái đề xuất";
            };
        }
        reviewNote=InventoryRules.text(reviewNote,false);
        if(review&&!admin)throw new IllegalArgumentException("Chỉ quản lý được duyệt đề xuất.");
        try(Connection c=getConnection()){c.setAutoCommit(false);try{
            InventoryWorkflowDAO.requireActor(c,actor,admin);
            String current;
            try(PreparedStatement p=c.prepareStatement("SELECT requester_id,status FROM restock_requests WHERE id=? FOR UPDATE")){
                p.setInt(1,id);try(ResultSet r=p.executeQuery()){
                    if(!r.next())throw new IllegalArgumentException("Không tìm thấy đề xuất.");
                    int owner=r.getInt("requester_id");current=r.getString("status");
                    if(!admin && owner!=actor)throw new IllegalArgumentException("Bạn chỉ được thao tác trên đề xuất của mình.");
                    if(!InventoryRules.editableRequest(current))throw new IllegalArgumentException("Đề xuất đã kết thúc, không thể thay đổi.");
                }
            }
            String target;
            if(review&&List.of("PENDING_APPROVAL","DRAFT","CHANGES_REQUESTED").contains(current))target=switch(action){case "approve"->"APPROVED";case "reject"->"REJECTED";default->"CHANGES_REQUESTED";};
            else if("submit".equals(action)&&List.of("DRAFT","CHANGES_REQUESTED").contains(current))target="PENDING_APPROVAL";
            else if("cancel".equals(action)&&InventoryRules.editableRequest(current))target="CANCELLED";
            else throw new IllegalArgumentException("Trạng thái đề xuất không cho phép thao tác này.");
            try(PreparedStatement p=c.prepareStatement("UPDATE restock_requests SET status=?,reviewer_id=?,review_note=?,reviewed_at=NOW() WHERE id=?")){
                p.setString(1,target);if(review)p.setInt(2,actor);else p.setNull(2,Types.INTEGER);p.setString(3,reviewNote);p.setInt(4,id);p.executeUpdate();}
            InventoryWorkflowDAO.audit(c,"RESTOCK",id,actor,action,current+" -> "+target+"; "+reviewNote);
            c.commit();return true;
        }catch(Exception e){c.rollback();throw e;}}
    }
    public Map<String,Object> getRestockStats(){
        Map<String,Object> m=new HashMap<>();try(Connection c=getConnection();PreparedStatement ps=c.prepareStatement("SELECT (SELECT COUNT(*) FROM ingredients WHERE is_active=1 AND current_stock<=min_stock_level) low_stock,(SELECT COUNT(*) FROM restock_requests WHERE status='PENDING_APPROVAL') pending_count,(SELECT COUNT(*) FROM restock_requests WHERE status='APPROVED') approved_count,(SELECT COUNT(*) FROM restock_requests WHERE status='DRAFT') draft_count");ResultSet rs=ps.executeQuery()){if(rs.next()){m.put("lowStock",rs.getInt(1));m.put("pendingCount",rs.getInt(2));m.put("approvedCount",rs.getInt(3));m.put("draftCount",rs.getInt(4));}}catch(Exception e){throw new IllegalStateException(e);}return m;
    }
}
