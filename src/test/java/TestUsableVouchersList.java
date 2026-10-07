import context.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestUsableVouchersList {
    public static void main(String[] args) {
        String USABLE_CONDITION =
            "status = 'ACTIVE' " +
            "AND (start_date IS NULL OR start_date <= NOW()) " +
            "AND (valid_until IS NULL OR valid_until >= NOW()) " +
            "AND (usage_limit IS NULL OR used_count < usage_limit)";
        
        String OWNER_CONDITION = "(customer_id IS NULL OR customer_id = ?)";
        
        String sql = "SELECT id, code, customer_id FROM vouchers WHERE " + USABLE_CONDITION + " AND " + OWNER_CONDITION;
        
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            ps.setInt(1, 4);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("--- Usable Vouchers for customer 4 ---");
                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.println("ID: " + rs.getInt("id") + ", Code: " + rs.getString("code") + ", Customer: " + rs.getObject("customer_id"));
                }
                if (count == 0) {
                    System.out.println("No usable vouchers found.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
