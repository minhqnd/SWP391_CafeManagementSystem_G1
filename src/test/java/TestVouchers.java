import context.DBContext;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestVouchers {
    public static void main(String[] args) {
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM vouchers")) {
            System.out.println("--- Vouchers in DB ---");
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println("ID: " + rs.getInt("id") + ", Code: " + rs.getString("code") + ", Customer: " + rs.getObject("customer_id"));
            }
            if (count == 0) {
                System.out.println("No vouchers found in DB.");
            }
            
            System.out.println("\n--- Users in DB ---");
            try (ResultSet rs2 = stmt.executeQuery("SELECT id, email, total_points FROM users LIMIT 5")) {
                while (rs2.next()) {
                    System.out.println("User ID: " + rs2.getInt("id") + ", Email: " + rs2.getString("email") + ", Points: " + rs2.getInt("total_points"));
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
