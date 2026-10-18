package dao;

import model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO extends BaseDAO {

    public List<Supplier> getAllSuppliers() {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT s.id, s.name, s.phone, s.address, s.status, " +
                     "COUNT(i.id) AS ingredient_count " +
                     "FROM suppliers s " +
                     "LEFT JOIN ingredients i ON s.id = i.supplier_id " +
                     "GROUP BY s.id, s.name, s.phone, s.address, s.status " +
                     "ORDER BY s.id DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Supplier s = new Supplier(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getString("status")
                );
                s.setIngredientCount(rs.getInt("ingredient_count"));
                list.add(s);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load suppliers", e);
        }
        return list;
    }

    public List<Supplier> getActiveSuppliers() {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT id, name, phone, address, status FROM suppliers " +
                     "WHERE status = 'ACTIVE' ORDER BY name ASC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Supplier(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getString("status")
                ));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load active suppliers", e);
        }
        return list;
    }

    public Supplier getSupplierById(int id) {
        String sql = "SELECT s.id, s.name, s.phone, s.address, s.status, " +
                     "COUNT(i.id) AS ingredient_count " +
                     "FROM suppliers s " +
                     "LEFT JOIN ingredients i ON s.id = i.supplier_id " +
                     "WHERE s.id = ? " +
                     "GROUP BY s.id, s.name, s.phone, s.address, s.status";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Supplier s = new Supplier(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("phone"),
                            rs.getString("address"),
                            rs.getString("status")
                    );
                    s.setIngredientCount(rs.getInt("ingredient_count"));
                    return s;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not get supplier by id: " + id, e);
        }
        return null;
    }

    public int insertSupplier(Supplier s) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO suppliers (name, phone, address, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, s.getName());
            ps.setString(2, s.getPhone());
            ps.setString(3, s.getAddress());
            ps.setString(4, s.getStatus() != null ? s.getStatus() : "ACTIVE");

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean updateSupplier(Supplier s) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE suppliers SET name = ?, phone = ?, address = ?, status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, s.getName());
            ps.setString(2, s.getPhone());
            ps.setString(3, s.getAddress());
            ps.setString(4, s.getStatus());
            ps.setInt(5, s.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean toggleStatus(int id, String currentStatus) throws SQLException, ClassNotFoundException {
        String newStatus = "ACTIVE".equalsIgnoreCase(currentStatus) ? "INACTIVE" : "ACTIVE";
        String sql = "UPDATE suppliers SET status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int[] getSupplierStats() {
        int[] stats = new int[3]; // [total, active, inactive]
        String sql = "SELECT COUNT(*) AS total, " +
                     "SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_cnt, " +
                     "SUM(CASE WHEN status = 'INACTIVE' THEN 1 ELSE 0 END) AS inactive_cnt " +
                     "FROM suppliers";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                stats[0] = rs.getInt("total");
                stats[1] = rs.getInt("active_cnt");
                stats[2] = rs.getInt("inactive_cnt");
            }
        } catch (Exception e) {
            // Return zeros if empty or error
        }
        return stats;
    }
}
