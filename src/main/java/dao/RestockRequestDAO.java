package dao;

import model.Ingredient;
import model.RestockRequest;
import model.RestockRequest.RestockItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestockRequestDAO extends BaseDAO {

    public List<Ingredient> getLowStockIngredients() {
        List<Ingredient> list = new ArrayList<>();
        String sql = "SELECT i.id, i.supplier_id, i.name, i.unit, i.current_stock, i.min_stock_level, " +
                     "s.name AS supplier_name " +
                     "FROM ingredients i " +
                     "LEFT JOIN suppliers s ON i.supplier_id = s.id " +
                     "WHERE i.current_stock <= i.min_stock_level " +
                     "ORDER BY (i.min_stock_level - i.current_stock) DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Ingredient item = new Ingredient(
                    rs.getInt("id"),
                    rs.getObject("supplier_id") != null ? rs.getInt("supplier_id") : null,
                    rs.getString("name"),
                    rs.getString("unit"),
                    rs.getBigDecimal("current_stock"),
                    rs.getBigDecimal("min_stock_level")
                );
                item.setSupplierName(rs.getString("supplier_name"));
                list.add(item);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load low stock ingredients", e);
        }
        return list;
    }

    public List<RestockRequest> getRestockRequests(String statusFilter) {
        List<RestockRequest> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT po.id, po.supplier_id, po.creator_id, po.total_cost, po.status, po.created_at, " +
            "s.name AS supplier_name, u.full_name AS creator_name " +
            "FROM purchase_orders po " +
            "LEFT JOIN suppliers s ON po.supplier_id = s.id " +
            "LEFT JOIN users u ON po.creator_id = u.id " +
            "WHERE 1=1 "
        );

        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append(" AND po.status = ? ");
        }
        sql.append(" ORDER BY po.created_at DESC, po.id DESC");

        Map<Integer, RestockRequest> map = new HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
                ps.setString(1, statusFilter.toUpperCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RestockRequest req = new RestockRequest();
                    req.setId(rs.getInt("id"));
                    req.setSupplierId(rs.getInt("supplier_id"));
                    req.setSupplierName(rs.getString("supplier_name"));
                    req.setCreatorId(rs.getInt("creator_id"));
                    req.setCreatorName(rs.getString("creator_name"));
                    req.setTotalCost(rs.getBigDecimal("total_cost"));
                    req.setStatus(rs.getString("status"));
                    req.setCreatedAt(rs.getTimestamp("created_at"));
                    map.put(req.getId(), req);
                    list.add(req);
                }
            }

            if (!list.isEmpty()) {
                boolean filterByStatus = statusFilter != null && !statusFilter.isBlank()
                        && !"ALL".equalsIgnoreCase(statusFilter);
                String itemsSql = "SELECT poi.id, poi.po_id, poi.ingredient_id, poi.quantity, poi.unit_price, " +
                                  "i.name AS ingredient_name, i.unit " +
                                  "FROM purchase_order_items poi " +
                                  "JOIN ingredients i ON poi.ingredient_id = i.id " +
                                  "WHERE poi.po_id IN (SELECT id FROM purchase_orders " +
                                  (filterByStatus ? "WHERE status = ? " : "") +
                                  ") ORDER BY poi.id ASC";

                try (PreparedStatement psItems = conn.prepareStatement(itemsSql)) {
                    if (filterByStatus) {
                        psItems.setString(1, statusFilter.toUpperCase());
                    }
                    try (ResultSet rsItems = psItems.executeQuery()) {
                        while (rsItems.next()) {
                            int poId = rsItems.getInt("po_id");
                            RestockRequest req = map.get(poId);
                            if (req != null) {
                                RestockItem it = new RestockItem(
                                    rsItems.getInt("ingredient_id"),
                                    rsItems.getString("ingredient_name"),
                                    rsItems.getString("unit"),
                                    rsItems.getBigDecimal("quantity"),
                                    rsItems.getBigDecimal("unit_price")
                                );
                                it.setId(rsItems.getInt("id"));
                                it.setPoId(poId);
                                req.getItems().add(it);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("Could not load restock requests", e);
        }
        return list;
    }

    public int createRestockRequest(int supplierId, int creatorId, List<RestockItem> items)
            throws SQLException, ClassNotFoundException {

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Đề xuất nhập hàng phải có ít nhất một nguyên liệu");
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        for (RestockItem it : items) {
            totalCost = totalCost.add(it.getSubtotal());
        }

        String sqlPO = "INSERT INTO purchase_orders (supplier_id, creator_id, total_cost, status) " +
                       "VALUES (?, ?, ?, 'DRAFT')";
        String sqlPOI = "INSERT INTO purchase_order_items (po_id, ingredient_id, quantity, unit_price) " +
                        "VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int poId;
                try (PreparedStatement psPO = conn.prepareStatement(sqlPO, Statement.RETURN_GENERATED_KEYS)) {
                    psPO.setInt(1, supplierId);
                    psPO.setInt(2, creatorId);
                    psPO.setBigDecimal(3, totalCost);
                    psPO.executeUpdate();
                    try (ResultSet rs = psPO.getGeneratedKeys()) {
                        if (rs.next()) {
                            poId = rs.getInt(1);
                        } else {
                            throw new SQLException("Không tạo được phiếu purchase_order");
                        }
                    }
                }

                try (PreparedStatement psPOI = conn.prepareStatement(sqlPOI)) {
                    for (RestockItem item : items) {
                        psPOI.setInt(1, poId);
                        psPOI.setInt(2, item.getIngredientId());
                        psPOI.setBigDecimal(3, item.getQuantity());
                        psPOI.setBigDecimal(4, item.getUnitPrice());
                        psPOI.addBatch();
                    }
                    psPOI.executeBatch();
                }

                conn.commit();
                return poId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    public boolean updateStatus(int poId, String newStatus) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE purchase_orders SET status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, poId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Receive an ordered PO once, add its quantities to stock, and write matching ledger entries atomically. */
    public boolean receivePurchaseOrder(int poId) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                String status;
                try (PreparedStatement lock = conn.prepareStatement(
                        "SELECT status FROM purchase_orders WHERE id = ? FOR UPDATE")) {
                    lock.setInt(1, poId);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        status = rs.getString("status");
                    }
                }
                if (!"ORDERED".equals(status)) {
                    conn.rollback();
                    return false;
                }

                InventoryMovementDAO.receivePurchaseOrder(conn, poId);
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE purchase_orders SET status = 'RECEIVED' WHERE id = ? AND status = 'ORDERED'")) {
                    update.setInt(1, poId);
                    boolean updated = update.executeUpdate() == 1;
                    if (updated) conn.commit(); else conn.rollback();
                    return updated;
                }
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException) throw (SQLException) e;
                throw new SQLException("Could not receive purchase order", e);
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    public Map<String, Object> getRestockStats() {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT " +
                     "(SELECT COUNT(*) FROM ingredients WHERE current_stock <= min_stock_level) AS low_stock, " +
                     "(SELECT COUNT(*) FROM purchase_orders WHERE status = 'DRAFT') AS draft_count, " +
                     "(SELECT COUNT(*) FROM purchase_orders WHERE status = 'ORDERED') AS ordered_count, " +
                     "(SELECT COALESCE(SUM(total_cost), 0) FROM purchase_orders WHERE status = 'DRAFT') AS draft_total_cost";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.put("lowStockCount", rs.getInt("low_stock"));
                stats.put("draftCount", rs.getInt("draft_count"));
                stats.put("orderedCount", rs.getInt("ordered_count"));
                stats.put("draftTotalCost", rs.getBigDecimal("draft_total_cost"));
            }
        } catch (Exception ignored) {
        }
        return stats;
    }
}
