package context;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;

public class FixDB {
    public static void main(String[] args) {
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement()) {
            
            try {
                stmt.execute("ALTER TABLE products ADD COLUMN description TEXT");
                System.out.println("Added description column.");
            } catch (Exception e) {
                System.out.println("description column might already exist: " + e.getMessage());
            }

            // Also update the image_urls and descriptions just in case
            String update1 = "UPDATE products SET description = 'Espresso nguyên chất pha loãng, giữ trọn hương vị đậm đà mộc mạc.', image_url = 'https://images.unsplash.com/photo-1551030173-122aabc4489c?auto=format&fit=crop&q=80&w=800' WHERE id = 1";
            stmt.executeUpdate(update1);
            
            String update2 = "UPDATE products SET description = 'Sự cân bằng hoàn hảo giữa Espresso đậm đà và bọt sữa nóng mịn màng.', image_url = 'https://images.unsplash.com/photo-1570968915860-54d5c301fa9f?auto=format&fit=crop&q=80&w=800' WHERE id = 2";
            stmt.executeUpdate(update2);

            String update3 = "UPDATE products SET description = 'Phiên bản đậm đà hơn của Latte với lớp bọt sữa dày dặn hấp dẫn.', image_url = 'https://images.unsplash.com/photo-1572442388796-11668a67e53d?auto=format&fit=crop&q=80&w=800' WHERE id = 3";
            stmt.executeUpdate(update3);
            
            String update4 = "UPDATE products SET description = 'Trà đào mát lạnh cùng những miếng đào tươi giòn sần sật xua tan oi bức.', image_url = 'https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&q=80&w=800' WHERE id = 4";
            stmt.executeUpdate(update4);

            String update5 = "UPDATE products SET description = 'Trà chanh truyền thống với hương chanh tươi mát, chua ngọt thanh dịu.', image_url = 'https://images.unsplash.com/photo-1596704017254-9eb10a3e8ff6?auto=format&fit=crop&q=80&w=800' WHERE id = 5";
            stmt.executeUpdate(update5);

            String update6 = "UPDATE products SET description = 'Trà sữa vị lá trà đen tự nhiên, ngọt ngào quyện cùng trân châu dai giòn.', image_url = 'https://images.unsplash.com/photo-1558857563-b371032bda98?auto=format&fit=crop&q=80&w=800' WHERE id = 6";
            stmt.executeUpdate(update6);

            String update7 = "UPDATE products SET description = 'Bánh phô mai nướng mềm mịn, tan chảy trong miệng với độ béo ngậy khó cưỡng.', image_url = 'https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&q=80&w=800' WHERE id = 7";
            stmt.executeUpdate(update7);

            String update8 = "UPDATE products SET description = 'Nước cam vắt nguyên chất, giàu vitamin C và năng lượng cho ngày mới.', image_url = 'https://images.unsplash.com/photo-1621506289937-a8e4df240d0b?auto=format&fit=crop&q=80&w=800' WHERE id = 8";
            stmt.executeUpdate(update8);

            System.out.println("Updated products with images and descriptions.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
