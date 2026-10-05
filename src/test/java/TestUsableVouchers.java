import context.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestUsableVouchers {
    public static void main(String[] args) {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT id, code, start_date, valid_until, status, usage_limit, used_count FROM vouchers WHERE customer_id = 4"
             );
             ResultSet rs = ps.executeQuery()) {
            
            System.out.println("--- Vouchers for customer 4 ---");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + ", Code: " + rs.getString("code") + 
                    ", Start: " + rs.getTimestamp("start_date") + 
                    ", Until: " + rs.getTimestamp("valid_until") + 
                    ", Status: " + rs.getString("status") +
                    ", usage_limit: " + rs.getInt("usage_limit") +
                    ", used_count: " + rs.getInt("used_count"));
            }
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs2 = stmt.executeQuery("SELECT NOW() as mysql_now")) {
                if (rs2.next()) {
                    System.out.println("MySQL NOW(): " + rs2.getTimestamp("mysql_now"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
