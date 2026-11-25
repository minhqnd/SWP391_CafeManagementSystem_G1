package dao;

import model.Ingredient;
import model.InventoryTransaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventoryDAO extends BaseDAO {

    private static final BigDecimal MAX_STOCK_VALUE = new BigDecimal("999999999.999");

    private final IngredientDAO ingredientDAO = new IngredientDAO();

    public List<Ingredient> getInventoryStockList(String filterStatus) {
        List<Ingredient> all = ingredientDAO.getAllIngredientsWithDetails(null, null);
        if (filterStatus == null || "all".equalsIgnoreCase(filterStatus) || filterStatus.isBlank()) {
            return all;
        }

        List<Ingredient> filtered = new ArrayList<>();
        for (Ingredient item : all) {
            String status = item.getStockStatus();
            if ("low_stock".equalsIgnoreCase(filterStatus) && "Sắp hết".equals(status)) {
                filtered.add(item);
            } else if ("out_of_stock".equalsIgnoreCase(filterStatus) && "Hết hàng".equals(status)) {
                filtered.add(item);
            } else if ("safe".equalsIgnoreCase(filterStatus) && "Đủ hàng".equals(status)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public List<InventoryTransaction> getRecentTransactions(int limit, String typeFilter) {
        List<InventoryTransaction> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT t.id, t.ingredient_id, t.transaction_type, t.quantity, " +
            "t.reference_type, t.reference_id, t.reason, t.created_at, a.quantity_delta AS adjustment_delta, " +
            "i.name AS ingredient_name, i.unit " +
            "FROM inventory_transactions t " +
            "JOIN ingredients i ON t.ingredient_id = i.id " +
            "LEFT JOIN purchase_receipt_adjustments a ON t.reference_type = 'RECEIPT_ADJUSTMENT' AND a.id = t.reference_id WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (typeFilter != null && !typeFilter.isBlank() && !"all".equalsIgnoreCase(typeFilter)) {
            sql.append(" AND t.transaction_type = ? ");
            params.add(typeFilter.toUpperCase());
        }
        sql.append(" ORDER BY t.created_at DESC, t.id DESC LIMIT ?");
        params.add(limit > 0 ? limit : 20);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InventoryTransaction tx = new InventoryTransaction(
                        rs.getInt("id"),
                        rs.getInt("ingredient_id"),
                        rs.getString("transaction_type"),
                        rs.getBigDecimal("quantity"),
                        rs.getString("reference_type"),
                        rs.getObject("reference_id") != null ? rs.getInt("reference_id") : null,
                        rs.getTimestamp("created_at")
                    );
                    tx.setIngredientName(rs.getString("ingredient_name"));
                    tx.setUnit(rs.getString("unit"));
                    tx.setReason(rs.getString("reason"));
                    tx.setAdjustmentDelta(rs.getBigDecimal("adjustment_delta"));
                    list.add(tx);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load inventory transactions", e);
        }
        return list;
    }

    /**
     * Record a stock adjustment or spoilage transaction and update ingredient current_stock.
     * @param ingredientId The ingredient id
     * @param transactionType 'ADJUSTMENT' or 'SPOILAGE' or 'IN_PURCHASE' or 'OUT_ORDER'
     * @param quantity The absolute quantity to adjust
     * @param isIncrease true if stock increases, false if stock decreases
     * @param referenceType 'ADJUSTMENT' or 'ORDER' or 'PURCHASE_ORDER'
     * @param referenceId optional ID
     */
    public boolean recordStockAdjustment(int ingredientId, String transactionType,
                                         BigDecimal quantity, boolean isIncrease,
                                         String referenceType, Integer referenceId)
            throws SQLException, ClassNotFoundException {
        return recordStockAdjustment(ingredientId, transactionType, quantity, isIncrease, referenceType, referenceId, null);
    }

    public boolean recordStockAdjustment(int ingredientId, String transactionType,
                                         BigDecimal quantity, boolean isIncrease,
                                         String referenceType, Integer referenceId, String reason)
            throws SQLException, ClassNotFoundException {

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số lượng điều chỉnh phải lớn hơn 0");
        }

        String selectSql = "SELECT current_stock FROM ingredients WHERE id = ? FOR UPDATE";
        String updateSql = "UPDATE ingredients SET current_stock = ? WHERE id = ?";
        String insertTxSql = "INSERT INTO inventory_transactions " +
                             "(ingredient_id, transaction_type, quantity, reference_type, reference_id, reason) " +
                             "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                BigDecimal currentStock;
                try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                    psSelect.setInt(1, ingredientId);
                    try (ResultSet rs = psSelect.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        currentStock = rs.getBigDecimal("current_stock");
                    }
                }

                BigDecimal newStock = isIncrease ? currentStock.add(quantity) : currentStock.subtract(quantity);
                if (newStock.compareTo(BigDecimal.ZERO) < 0) {
                    throw new SQLException("Lượng trừ vượt quá tồn kho hiện tại (" + currentStock.stripTrailingZeros().toPlainString() + ").", "45000");
                }
                if (newStock.compareTo(MAX_STOCK_VALUE) > 0) {
                    throw new SQLException("Tồn kho sau khi cộng thêm vượt quá giới hạn lưu trữ (tối đa 999,999,999.999).", "45000");
                }

                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setBigDecimal(1, newStock);
                    psUpdate.setInt(2, ingredientId);
                    psUpdate.executeUpdate();
                }

                try (PreparedStatement psTx = conn.prepareStatement(insertTxSql)) {
                    psTx.setInt(1, ingredientId);
                    psTx.setString(2, transactionType.toUpperCase());
                    psTx.setBigDecimal(3, quantity);
                    psTx.setString(4, referenceType != null ? referenceType : "ADJUSTMENT");
                    if (referenceId != null && referenceId > 0) {
                        psTx.setInt(5, referenceId);
                    } else {
                        psTx.setNull(5, java.sql.Types.INTEGER);
                    }
                    psTx.setString(6, reason);
                    psTx.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    public BigDecimal getTotalInventoryEstimatedValue() {
        BigDecimal totalValue = BigDecimal.ZERO;
        List<Ingredient> ingredients = ingredientDAO.getAllIngredientsWithDetails(null, null);
        for (Ingredient item : ingredients) {
            if (item.getCurrentStock() != null && item.getLastPurchasePrice() != null) {
                totalValue = totalValue.add(item.getCurrentStock().multiply(item.getLastPurchasePrice()));
            }
        }
        return totalValue;
    }

    public int getTotalTransactionCount() {
        String sql = "SELECT COUNT(*) FROM inventory_transactions";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
