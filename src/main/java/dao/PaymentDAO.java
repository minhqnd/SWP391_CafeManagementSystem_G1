package dao;

import model.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Transactional payment persistence used by QR callbacks and COD collection. */
public class PaymentDAO extends BaseDAO {
    public static final int FAILED = -1;

    public record CallbackResult(boolean accepted, boolean successful, int earnedPoints,
                                 int orderId, String orderCode, String message) {}

    public Payment ensurePendingBankTransfer(int orderId, int customerId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                String orderCode;
                BigDecimal amount;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT order_code,final_amount FROM orders WHERE id=? AND customer_id=? AND status='PENDING' FOR UPDATE")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { conn.rollback(); return null; }
                        orderCode = rs.getString(1);
                        amount = rs.getBigDecimal(2);
                    }
                }
                Payment existing = findLatestPayment(conn, orderId);
                if (existing != null && ("PENDING".equals(existing.getStatus())
                        || "SUCCESS".equals(existing.getStatus()))) {
                    conn.commit();
                    return existing;
                }
                String code = "QR-" + orderCode;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO payments(order_id,payment_method,transaction_code,amount,status,payment_note) " +
                        "VALUES(?,'BANK_TRANSFER',?,?,'PENDING','Waiting for verified gateway callback') " +
                        "ON DUPLICATE KEY UPDATE amount=VALUES(amount),status='PENDING',paid_at=NULL,payment_note=VALUES(payment_note)")) {
                    ps.setInt(1, orderId);
                    ps.setString(2, code);
                    ps.setBigDecimal(3, amount);
                    ps.executeUpdate();
                }
                conn.commit();
                return findLatestPayment(orderId);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create pending payment", e);
        }
    }

    /** Chooses COD; money and points stay pending until the assigned shipper collects it. */
    public boolean acceptCod(int orderId, int customerId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                String orderCode;
                BigDecimal amount;
                String status;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT order_code,final_amount,status FROM orders WHERE id=? AND customer_id=? AND order_type='ONLINE' FOR UPDATE")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { conn.rollback(); return false; }
                        orderCode=rs.getString(1); amount=rs.getBigDecimal(2); status=rs.getString(3);
                    }
                }
                Payment existing = findLatestPayment(conn, orderId);
                if (existing != null && "CASH".equals(existing.getPaymentMethod())
                        && "PENDING".equals(existing.getStatus()) && !"CANCELLED".equals(status)) {
                    conn.commit();
                    return true;
                }
                if (!"PENDING".equals(status)) { conn.rollback(); return false; }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE payments SET status='FAILED',payment_note=CONCAT(COALESCE(payment_note,''),' [Switched to COD]') " +
                        "WHERE order_id=? AND status='PENDING'")) {
                    ps.setInt(1, orderId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO payments(order_id,payment_method,transaction_code,amount,status,payment_note) " +
                        "VALUES(?,'CASH',?,?,'PENDING','Collect on delivery')")) {
                    ps.setInt(1, orderId); ps.setString(2, "COD-"+orderCode); ps.setBigDecimal(3, amount); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE orders SET status='CONFIRMED' WHERE id=? AND status='PENDING'")) {
                    ps.setInt(1, orderId);
                    if (ps.executeUpdate()!=1) { conn.rollback(); return false; }
                }
                conn.commit();
                return true;
            } catch (Exception e) { conn.rollback(); throw e; }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot select COD", e);
        }
    }

    /** Applies an authenticated provider callback once. Duplicate callbacks are harmless. */
    public CallbackResult applyGatewayCallback(String orderCode, String transactionCode, BigDecimal amount,
                                                String callbackStatus, String gatewayReference,
                                                String callbackPayload, double pointsEarnUnit) {
        if (orderCode == null || transactionCode == null || amount == null) {
            return new CallbackResult(false,false,0,0,orderCode,"Missing callback fields");
        }
        boolean success = "SUCCESS".equalsIgnoreCase(callbackStatus);
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int orderId, customerId;
                BigDecimal expected, goodsAfterDiscount;
                String orderStatus;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id,customer_id,final_amount,total_amount-discount_amount AS goods,status " +
                        "FROM orders WHERE order_code=? FOR UPDATE")) {
                    ps.setString(1, orderCode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { conn.rollback(); return new CallbackResult(false,false,0,0,orderCode,"Order not found"); }
                        orderId=rs.getInt("id"); customerId=rs.getInt("customer_id");
                        expected=rs.getBigDecimal("final_amount"); goodsAfterDiscount=rs.getBigDecimal("goods");
                        orderStatus=rs.getString("status");
                    }
                }
                Payment payment=findPaymentByTransaction(conn,transactionCode);
                if (payment==null || payment.getOrderId()!=orderId || !"BANK_TRANSFER".equals(payment.getPaymentMethod())) {
                    conn.rollback(); return new CallbackResult(false,false,0,orderId,orderCode,"Payment reference does not match order");
                }
                if (BigDecimal.valueOf(payment.getAmount()).compareTo(amount)!=0 || expected.compareTo(amount)!=0) {
                    conn.rollback(); return new CallbackResult(false,false,0,orderId,orderCode,"Payment amount does not match order");
                }
                if ("SUCCESS".equals(payment.getStatus())) {
                    conn.commit(); return new CallbackResult(true,true,0,orderId,orderCode,"Callback already processed");
                }
                String newStatus=success?"SUCCESS":"FAILED";
                try (PreparedStatement ps=conn.prepareStatement(
                        "UPDATE payments SET status=?,gateway_reference=?,callback_payload=?,callback_received_at=NOW()," +
                        "paid_at=CASE WHEN ?='SUCCESS' THEN NOW() ELSE NULL END,payment_note=? WHERE id=?")) {
                    ps.setString(1,newStatus); ps.setString(2,gatewayReference); ps.setString(3,callbackPayload);
                    ps.setString(4,newStatus); ps.setString(5,success?"Verified gateway callback":"Gateway reported failure");
                    ps.setInt(6,payment.getId()); ps.executeUpdate();
                }
                int earned=0;
                if (success) {
                    if (!"PENDING".equals(orderStatus)) {
                        conn.rollback(); return new CallbackResult(false,false,0,orderId,orderCode,"Order is no longer payable");
                    }
                    try (PreparedStatement ps=conn.prepareStatement(
                            "UPDATE orders SET status='CONFIRMED' WHERE id=? AND status='PENDING'")) {
                        ps.setInt(1,orderId);
                        if(ps.executeUpdate()!=1){conn.rollback();return new CallbackResult(false,false,0,orderId,orderCode,"Order state changed");}
                    }
                    earned=earnPointsOnce(conn,customerId,orderId,goodsAfterDiscount,pointsEarnUnit);
                }
                conn.commit();
                return new CallbackResult(true,success,earned,orderId,orderCode,success?"Payment confirmed":"Payment failed");
            } catch(Exception e){conn.rollback();throw e;}
        } catch(Exception e){
            return new CallbackResult(false,false,0,0,orderCode,e.getMessage());
        }
    }

    public static int earnPointsOnce(Connection conn,int customerId,int orderId,BigDecimal goods,double unit)
            throws java.sql.SQLException {
        if(customerId<=0 || unit<=0) return 0;
        try(PreparedStatement ps=conn.prepareStatement(
                "SELECT 1 FROM loyalty_transactions WHERE order_id=? AND transaction_type='EARN' LIMIT 1")){
            ps.setInt(1,orderId); try(ResultSet rs=ps.executeQuery()){if(rs.next())return 0;}
        }
        int points=(int)Math.floor(goods.doubleValue()/unit);
        if(points<=0)return 0;
        try(PreparedStatement ps=conn.prepareStatement(
                "INSERT INTO loyalty_transactions(customer_id,order_id,points_change,transaction_type) VALUES(?,?,?,'EARN')")){
            ps.setInt(1,customerId);ps.setInt(2,orderId);ps.setInt(3,points);ps.executeUpdate();
        }
        try(PreparedStatement ps=conn.prepareStatement("UPDATE users SET total_points=total_points+? WHERE id=?")){
            ps.setInt(1,points);ps.setInt(2,customerId);ps.executeUpdate();
        }
        return points;
    }

    public Payment getPaymentByOrderId(int orderId){ return findLatestPayment(orderId); }

    public Payment findLatestPayment(int orderId){
        try(Connection conn=getConnection()){return findLatestPayment(conn,orderId);}catch(Exception e){return null;}
    }

    private Payment findLatestPayment(Connection conn,int orderId)throws java.sql.SQLException{
        try(PreparedStatement ps=conn.prepareStatement("SELECT * FROM payments WHERE order_id=? ORDER BY id DESC LIMIT 1")){
            ps.setInt(1,orderId);try(ResultSet rs=ps.executeQuery()){return rs.next()?map(rs):null;}
        }
    }
    private Payment findPaymentByTransaction(Connection conn,String code)throws java.sql.SQLException{
        try(PreparedStatement ps=conn.prepareStatement("SELECT * FROM payments WHERE transaction_code=? FOR UPDATE")){
            ps.setString(1,code);try(ResultSet rs=ps.executeQuery()){return rs.next()?map(rs):null;}
        }
    }
    private Payment map(ResultSet rs)throws java.sql.SQLException{
        Payment p=new Payment();p.setId(rs.getInt("id"));p.setOrderId(rs.getInt("order_id"));
        p.setPaymentMethod(rs.getString("payment_method"));p.setTransactionCode(rs.getString("transaction_code"));
        p.setAmount(rs.getDouble("amount"));p.setStatus(rs.getString("status"));p.setPaidAt(rs.getTimestamp("paid_at"));
        p.setPaymentNote(rs.getString("payment_note"));return p;
    }
}
