package dao;

import context.DBContext;
import model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO extends BaseDAO {

    /** Returns every category, including inactive ones, with the number of linked products. */
    public List<Category> getAllCategoriesWithCount() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT c.id, c.name, c.status, c.image_url, COUNT(p.id) AS product_count "
                + "FROM categories c LEFT JOIN products p ON p.category_id = c.id "
                + "GROUP BY c.id, c.name, c.status, c.image_url ORDER BY c.id ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Category(rs.getInt("id"), rs.getString("name"), rs.getString("status"),
                        rs.getString("image_url"),
                        rs.getInt("product_count")));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load categories", e);
        }
        return list;
    }

    public int getProductCount() {
        String sql = "SELECT COUNT(*) FROM products";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new IllegalStateException("Could not count products", e);
        }
    }

    public int getProductCountByCategory(int categoryId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM products WHERE category_id = ?")) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not count products in category", e);
        }
    }

    public boolean categoryNameExists(String name, int exceptId) {
        String sql = "SELECT 1 FROM categories WHERE LOWER(name) = LOWER(?) AND id <> ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, exceptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not check category name", e);
        }
    }

    public boolean saveCategory(Category category) throws SQLException, ClassNotFoundException {
        if (category.getId() > 0) {
            String sql = "UPDATE categories SET name = ?, image_url = ?, status = ? WHERE id = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                bindCategory(ps, category);
                ps.setInt(4, category.getId());
                int updated = ps.executeUpdate();
                if (updated > 0) return true;
                try (PreparedStatement exists = conn.prepareStatement("SELECT 1 FROM categories WHERE id = ?")) {
                    exists.setInt(1, category.getId());
                    try (ResultSet rs = exists.executeQuery()) {
                        return rs.next();
                    }
                }
            }
        }
        String sql = "INSERT INTO categories (name, image_url, status) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindCategory(ps, category);
            if (ps.executeUpdate() == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) category.setId(keys.getInt(1));
            }
            return category.getId() > 0;
        }
    }

    private void bindCategory(PreparedStatement ps, Category category) throws SQLException {
        ps.setString(1, category.getName());
        ps.setString(2, category.getImageUrl());
        ps.setString(3, category.getStatus());
    }

    public boolean updateCategoryStatus(int id, String status) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE categories SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteCategory(int id) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM categories WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Category> getAllActiveCategoriesWithCount() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT c.id, c.name, c.status, c.image_url, COUNT(p.id) AS product_count " +
                     "FROM categories c " +
                     "LEFT JOIN products p ON c.id = p.category_id AND p.is_available = 1 " +
                     "WHERE c.status = 'ACTIVE' " +
                     "GROUP BY c.id, c.name, c.status, c.image_url " +
                     "ORDER BY c.id ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Category cat = new Category(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("status"),
                        rs.getString("image_url"),
                        rs.getInt("product_count")
                );
                list.add(cat);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Category getCategoryById(int id) {
        String sql = "SELECT id, name, status, image_url FROM categories WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Category(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("status"),
                            rs.getString("image_url"),
                            0
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
