package dao;

import model.Ingredient;
import model.RestockRequest;
import model.RestockRequest.RestockItem;
import java.math.BigDecimal;
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
        try(Connection c=getConnection()){c.setAutoCommit(false);try{
            for(RestockItem item:items) IngredientDAO.requireActive(c,item.getIngredientId());
            int id;try(PreparedStatement ps=c.prepareStatement("INSERT INTO restock_requests(requester_id,status,reason,note) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){ps.setInt(1,requesterId);ps.setString(2,initial);ps.setString(3,reason);ps.setString(4,note);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(!rs.next())throw new SQLException("Không tạo được đề xuất.");id=rs.getInt(1);}}
            try(PreparedStatement ps=c.prepareStatement("INSERT INTO restock_request_items(request_id,ingredient_id,quantity,item_note) VALUES(?,?,?,?)")){for(RestockItem i:items){if(i.getQuantity()==null||i.getQuantity().signum()<=0)throw new IllegalArgumentException("Số lượng đề xuất phải lớn hơn 0.");ps.setInt(1,id);ps.setInt(2,i.getIngredientId());ps.setBigDecimal(3,i.getQuantity());ps.setString(4,i.getItemNote());ps.addBatch();}ps.executeBatch();}
            c.commit();return id;}catch(Exception e){c.rollback();throw e;}}
    }
    public boolean transition(int id,int reviewerId,String action,String reviewNote)throws SQLException,ClassNotFoundException{
        String sql;
        switch(action){
            case "submit":sql="UPDATE restock_requests SET status='PENDING_APPROVAL' WHERE id=? AND status='DRAFT'";break;
            case "approve":sql="UPDATE restock_requests SET status='APPROVED',reviewer_id=?,review_note=?,reviewed_at=NOW() WHERE id=? AND status='PENDING_APPROVAL'";break;
            case "reject":sql="UPDATE restock_requests SET status='REJECTED',reviewer_id=?,review_note=?,reviewed_at=NOW() WHERE id=? AND status='PENDING_APPROVAL'";break;
            case "cancel":sql="UPDATE restock_requests SET status='CANCELLED' WHERE id=? AND status IN('DRAFT','PENDING_APPROVAL')";break;
            default:throw new IllegalArgumentException("Chuyển trạng thái không hợp lệ.");
        }
        try(Connection c=getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            if("approve".equals(action)||"reject".equals(action)){ps.setInt(1,reviewerId);ps.setString(2,reviewNote);ps.setInt(3,id);}else ps.setInt(1,id);
            return ps.executeUpdate()==1;
        }
    }
    public Map<String,Object> getRestockStats(){
        Map<String,Object> m=new HashMap<>();try(Connection c=getConnection();PreparedStatement ps=c.prepareStatement("SELECT (SELECT COUNT(*) FROM ingredients WHERE is_active=1 AND current_stock<=min_stock_level) low_stock,(SELECT COUNT(*) FROM restock_requests WHERE status='PENDING_APPROVAL') pending_count,(SELECT COUNT(*) FROM restock_requests WHERE status='APPROVED') approved_count,(SELECT COUNT(*) FROM restock_requests WHERE status='DRAFT') draft_count");ResultSet rs=ps.executeQuery()){if(rs.next()){m.put("lowStock",rs.getInt(1));m.put("pendingCount",rs.getInt(2));m.put("approvedCount",rs.getInt(3));m.put("draftCount",rs.getInt(4));}}catch(Exception e){throw new IllegalStateException(e);}return m;
    }
}
