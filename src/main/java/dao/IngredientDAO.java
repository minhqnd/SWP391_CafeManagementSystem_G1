package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            throw new IllegalStateException("Could not load ingredients", e);
        }
        return list;
    }

    /** Weighted-average purchase cost per ingredient unit, using received purchase orders only. */
    public Map<Integer, BigDecimal> getAverageReceivedUnitCosts() throws SQLException, ClassNotFoundException {
        Map<Integer, BigDecimal> costs = new HashMap<>();
        String sql = "SELECT poi.ingredient_id, "
                + "SUM(poi.quantity * poi.unit_price) / NULLIF(SUM(poi.quantity), 0) AS average_unit_cost "
                + "FROM purchase_order_items poi "
                + "JOIN purchase_orders po ON po.id = poi.po_id "
                + "WHERE po.status = 'RECEIVED' "
                + "GROUP BY poi.ingredient_id";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BigDecimal averageCost = rs.getBigDecimal("average_unit_cost");
                if (averageCost != null) costs.put(rs.getInt("ingredient_id"), averageCost);
            }
        }
        return costs;
    }
}
