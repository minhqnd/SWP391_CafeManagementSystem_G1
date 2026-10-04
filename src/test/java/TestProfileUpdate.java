import context.DBContext;
import dao.UserDAO;
import model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestProfileUpdate {
    public static void main(String[] args) {
        UserDAO dao = new UserDAO();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE email = ?")) {
            ps.setString(1, "nguyen@gmail.com");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    System.out.println("Found user with ID: " + id);
                    boolean success = dao.updateUserProfile(id, "Nguyễn Quốc Nguyên Update", "1791139394351", "nguyen@gmail.com", "", "Test address");
                    System.out.println("Update success: " + success);
                } else {
                    System.out.println("User not found!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
