package dao;
import model.LoyaltyTransaction; import java.sql.SQLException; import java.util.List;
/** Earn-only phone loyalty persistence contract. */
public class LoyaltyTransactionDAO {
 public List<LoyaltyTransaction> findByCustomer(int customerId)throws SQLException{throw new UnsupportedOperationException("TODO");}
 public List<LoyaltyTransaction> findRecent(int limit)throws SQLException{throw new UnsupportedOperationException("TODO");}
 public boolean earnForCompletedOrder(int customerId,int orderId,int points)throws SQLException{throw new UnsupportedOperationException("TODO: transaction");}
}
