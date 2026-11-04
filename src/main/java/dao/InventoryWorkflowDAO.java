package dao;

import java.sql.*;
import java.util.*;

/** Append-only business audit, committed with the business operation. */
public class InventoryWorkflowDAO extends BaseDAO {
    static void audit(Connection c, String type, int id, int actor, String action, String details) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("INSERT INTO inventory_workflow_history(entity_type,entity_id,actor_id,action,details) VALUES(?,?,?,?,?)")) {
            p.setString(1,type); p.setInt(2,id); p.setInt(3,actor); p.setString(4,action); p.setString(5,details); p.executeUpdate();
        }
    }
    static void requireActor(Connection c, int actor, boolean admin) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT role,position,status FROM users WHERE id=? FOR UPDATE")) {
            p.setInt(1,actor);
            try (ResultSet r=p.executeQuery()) {
                if (!r.next() || !"ACTIVE".equals(r.getString("status")) ||
                    !("ADMIN".equals(r.getString("role")) || (!admin && "STAFF".equals(r.getString("role")) && "BARISTA".equals(r.getString("position")))))
                    throw new IllegalArgumentException("Bạn không có quyền thực hiện thao tác này.");
            }
        }
    }
    public List<Map<String,Object>> history(String type, Integer actor) throws SQLException,ClassNotFoundException {
        List<Map<String,Object>> list=new ArrayList<>();
        String sql="SELECT h.*,u.full_name FROM inventory_workflow_history h JOIN users u ON u.id=h.actor_id WHERE h.entity_type=? " +
            (actor==null ? "" : "AND EXISTS(SELECT 1 FROM restock_requests r WHERE r.id=h.entity_id AND r.requester_id=?) ")+"ORDER BY h.id DESC LIMIT 200";
        try(Connection c=getConnection();PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,type);if(actor!=null)p.setInt(2,actor);
            try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> row=new LinkedHashMap<>();
                for(String key:List.of("entity_id","action","details","created_at","full_name"))row.put(key,r.getObject(key));list.add(row);}}
        }return list;
    }
}
