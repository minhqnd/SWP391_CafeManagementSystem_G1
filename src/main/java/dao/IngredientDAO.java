package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import model.Ingredient;

public class IngredientDAO extends BaseDAO {

    public List<Ingredient> getAllIngredients() {
        List<Ingredient> list = new ArrayList<>();
        String sql = "SELECT id, name, unit FROM ingredients ORDER BY name ASC";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                list.add(new Ingredient(
                    rs.getInt("id"), 
                    rs.getString("name"), 
                    rs.getString("unit")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
