package dao;

import model.DeliveryCodRecord;
import model.DeliveryIncident;
import model.Order;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DeliveryDAO extends BaseDAO {
    public List<Order> findAssignedOrders(int shipperId)throws SQLException{
        List<Order> list=new ArrayList<>();
        String sql="SELECT o.*, (SELECT p.payment_method FROM payments p WHERE p.order_id=o.id ORDER BY p.id DESC LIMIT 1) AS payment_method " +
                "FROM orders o WHERE o.order_type='ONLINE' AND " +
                "((o.status='READY' AND (o.shipper_id IS NULL OR o.shipper_id=?)) OR " +
                "(o.shipper_id=? AND o.status IN('CONFIRMED','PREPARING','READY','DELIVERING'))) " +
                "ORDER BY FIELD(o.status,'DELIVERING','READY','PREPARING','CONFIRMED'),o.created_at";
        try(Connection c=getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,shipperId);ps.setInt(2,shipperId);
            try(ResultSet rs=ps.executeQuery()){while(rs.next())list.add(mapOrder(rs));}
        }catch(ClassNotFoundException e){throw new SQLException(e);}
        return list;
    }

    public boolean claimOrder(int orderId,int shipperId)throws SQLException{
        String sql="UPDATE orders SET shipper_id=? WHERE id=? AND order_type='ONLINE' AND status='READY' " +
                "AND (shipper_id IS NULL OR shipper_id=?)";
        try(Connection c=getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,shipperId);ps.setInt(2,orderId);ps.setInt(3,shipperId);return ps.executeUpdate()==1;
        }catch(ClassNotFoundException e){throw new SQLException(e);}
    }

    public boolean markDelivering(int orderId,int shipperId)throws SQLException{
        String sql="UPDATE orders SET status='DELIVERING' WHERE id=? AND shipper_id=? AND order_type='ONLINE' AND status='READY'";
        try(Connection c=getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,orderId);ps.setInt(2,shipperId);return ps.executeUpdate()==1;
        }catch(ClassNotFoundException e){throw new SQLException(e);}
    }

    public boolean completeDelivery(int orderId,int shipperId)throws SQLException{
        return completeDelivery(orderId,shipperId,null,new ShopSettingDAO().getSettings().getPointsEarnUnit());
    }

    /** Completes delivery and settles a pending COD payment in the same transaction. */
    public boolean completeDelivery(int orderId,int shipperId,BigDecimal collectedAmount,double pointsEarnUnit)throws SQLException{
        try(Connection c=getConnection()){
            c.setAutoCommit(false);
            try{
                int customerId;BigDecimal finalAmount,goods;String status;
                try(PreparedStatement ps=c.prepareStatement(
                        "SELECT customer_id,final_amount,total_amount-discount_amount AS goods,status FROM orders " +
                        "WHERE id=? AND shipper_id=? AND order_type='ONLINE' FOR UPDATE")){
                    ps.setInt(1,orderId);ps.setInt(2,shipperId);
                    try(ResultSet rs=ps.executeQuery()){
                        if(!rs.next()){c.rollback();return false;}
                        customerId=rs.getInt("customer_id");finalAmount=rs.getBigDecimal("final_amount");
                        goods=rs.getBigDecimal("goods");status=rs.getString("status");
                    }
                }
                if(!"DELIVERING".equals(status)){c.rollback();return false;}
                boolean cod=false;
                try(PreparedStatement ps=c.prepareStatement(
                        "SELECT id,amount FROM payments WHERE order_id=? AND payment_method='CASH' AND status='PENDING' FOR UPDATE")){
                    ps.setInt(1,orderId);
                    try(ResultSet rs=ps.executeQuery()){
                        if(rs.next()){
                            cod=true;
                            BigDecimal due=rs.getBigDecimal("amount");
                            if(collectedAmount==null || due.compareTo(collectedAmount)!=0 || finalAmount.compareTo(collectedAmount)!=0){
                                c.rollback();return false;
                            }
                            try(PreparedStatement up=c.prepareStatement(
                                    "UPDATE payments SET status='SUCCESS',paid_at=NOW(),payment_note='COD collected by assigned shipper' WHERE id=?")){
                                up.setInt(1,rs.getInt("id"));up.executeUpdate();
                            }
                        }
                    }
                }
                try(PreparedStatement ps=c.prepareStatement(
                        "UPDATE orders SET status='COMPLETED' WHERE id=? AND status='DELIVERING'")){
                    ps.setInt(1,orderId);if(ps.executeUpdate()!=1){c.rollback();return false;}
                }
                if(cod)PaymentDAO.earnPointsOnce(c,customerId,orderId,goods,pointsEarnUnit);
                c.commit();return true;
            }catch(Exception e){c.rollback();if(e instanceof SQLException)throw(SQLException)e;throw new SQLException(e);}
        }catch(ClassNotFoundException e){throw new SQLException(e);}
    }

    public int reportIncident(DeliveryIncident incident)throws SQLException{
        String sql="INSERT INTO delivery_incidents(order_id,shipper_id,incident_type,description,status) " +
                "SELECT id,?,?,?,'OPEN' FROM orders WHERE id=? AND shipper_id=? AND order_type='ONLINE' " +
                "AND status IN('READY','DELIVERING')";
        try(Connection c=getConnection();PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            ps.setInt(1,incident.shipperId());ps.setString(2,incident.type());ps.setString(3,incident.description());
            ps.setInt(4,incident.orderId());ps.setInt(5,incident.shipperId());
            if(ps.executeUpdate()!=1)return 0;
            try(ResultSet rs=ps.getGeneratedKeys()){return rs.next()?rs.getInt(1):0;}
        }catch(ClassNotFoundException e){throw new SQLException(e);}
    }

    public List<DeliveryIncident> findIncidents(int shipperId)throws SQLException{
        List<DeliveryIncident> list=new ArrayList<>();
        try(Connection c=getConnection();PreparedStatement ps=c.prepareStatement(
                "SELECT * FROM delivery_incidents WHERE shipper_id=? ORDER BY created_at DESC LIMIT 50")){
            ps.setInt(1,shipperId);try(ResultSet rs=ps.executeQuery()){while(rs.next())list.add(new DeliveryIncident(
                    rs.getInt("id"),rs.getInt("order_id"),rs.getInt("shipper_id"),rs.getString("incident_type"),
                    rs.getString("description"),rs.getString("status"),rs.getTimestamp("created_at").toLocalDateTime()));}
        }catch(ClassNotFoundException e){throw new SQLException(e);}
        return list;
    }

    public List<DeliveryCodRecord> findCodRecords(int shipperId)throws SQLException{
        List<DeliveryCodRecord> list=new ArrayList<>();
        String sql="SELECT p.id,p.order_id,o.shipper_id,p.amount,p.status,p.paid_at FROM payments p JOIN orders o ON o.id=p.order_id " +
                "WHERE o.shipper_id=? AND p.payment_method='CASH' AND o.order_type='ONLINE' ORDER BY p.id DESC";
        try(Connection c=getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,shipperId);try(ResultSet rs=ps.executeQuery()){while(rs.next()){
                Timestamp paid=rs.getTimestamp("paid_at");list.add(new DeliveryCodRecord(rs.getInt("id"),rs.getInt("order_id"),
                        rs.getInt("shipper_id"),rs.getBigDecimal("amount"),rs.getString("status"),paid==null?null:paid.toLocalDateTime()));
            }}
        }catch(ClassNotFoundException e){throw new SQLException(e);}
        return list;
    }

    private Order mapOrder(ResultSet rs)throws SQLException{
        Order o=new Order();o.setId(rs.getInt("id"));o.setOrderCode(rs.getString("order_code"));o.setCustomerId((Integer)rs.getObject("customer_id"));
        o.setOrderType(rs.getString("order_type"));o.setStatus(rs.getString("status"));o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setDiscountAmount(rs.getBigDecimal("discount_amount"));o.setDeliveryFee(rs.getBigDecimal("delivery_fee"));o.setFinalAmount(rs.getBigDecimal("final_amount"));
        o.setRecipientName(rs.getString("recipient_name"));o.setRecipientPhone(rs.getString("recipient_phone"));
        o.setDeliveryAddress(rs.getString("delivery_address"));o.setShipperId((Integer)rs.getObject("shipper_id"));
        o.setOrderNote(rs.getString("order_note"));o.setPaymentMethod(rs.getString("payment_method"));
        Timestamp t=rs.getTimestamp("created_at");if(t!=null)o.setCreatedAt(t.toLocalDateTime());return o;
    }
}
