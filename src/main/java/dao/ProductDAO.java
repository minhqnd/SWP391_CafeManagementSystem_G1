package dao;

import context.DBContext;
import model.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * ProductDAO - Tương tác dữ liệu sản phẩm / thực đơn
 */
public class ProductDAO extends BaseDAO {

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT id, category_id, name, base_price, variants_config, image_url, is_available FROM products WHERE is_available = 1 LIMIT 8";
        
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
             
            while (rs.next()) {
                Product p = new Product(
                    rs.getInt("id"),
                    rs.getInt("category_id"),
                    rs.getString("name"),
                    rs.getDouble("base_price"),
                    rs.getString("variants_config"),
                    rs.getString("image_url"),
                    rs.getBoolean("is_available")
                );
                list.add(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
