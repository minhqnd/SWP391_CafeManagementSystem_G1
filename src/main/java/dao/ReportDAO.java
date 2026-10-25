package dao;

import model.ReportDashboard;
import model.ReportDashboard.Row;
import model.ReportDashboard.StockRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ReportDAO - Tổng hợp số liệu màn Báo cáo (UC66) trong khoảng ngày [from, to].
 * - Doanh thu / phương thức / hình thức / món bán chạy: theo thanh toán thành công có paid_at trong khoảng ngày.
 * - Số đơn theo trạng thái: theo ngày tạo đơn.
 * - Kho: nguyên liệu xuất cho đơn trong khoảng ngày + nguyên liệu đang dưới mức tối thiểu (hiện tại).
 */
public class ReportDAO extends BaseDAO {

    /** Khoảng dài hơn số ngày này thì biểu đồ doanh thu gộp theo tháng */
    private static final int MAX_DAILY_BARS = 62;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");

    private static final String PAID_IN_RANGE = "p.status = 'SUCCESS' AND p.paid_at >= ? AND p.paid_at < ?";

    public ReportDashboard loadDashboard(LocalDate from, LocalDate to) throws SQLException, ClassNotFoundException {
        Timestamp start = Timestamp.valueOf(from.atStartOfDay());
        Timestamp end = Timestamp.valueOf(to.plusDays(1).atStartOfDay());
        ReportDashboard report = new ReportDashboard();

        try (Connection conn = getConnection()) {
            // 1. Doanh thu + số đơn đã thanh toán
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(p.amount), 0) AS revenue, COUNT(DISTINCT p.order_id) AS paid_orders " +
                    "FROM payments p WHERE " + PAID_IN_RANGE)) {
                bindRange(ps, start, end);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        report.setRevenue(rs.getDouble("revenue"));
                        report.setPaidOrderCount(rs.getLong("paid_orders"));
                    }
                }
            }

            // 2. Số đơn theo trạng thái (theo ngày tạo đơn)
            Map<String, Long> byStatus = new LinkedHashMap<>();
            for (String s : new String[]{"PENDING", "CONFIRMED", "PREPARING", "READY", "DELIVERING", "COMPLETED", "CANCELLED"}) {
                byStatus.put(s, 0L);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT o.status, COUNT(*) AS cnt FROM orders o WHERE o.created_at >= ? AND o.created_at < ? GROUP BY o.status")) {
                bindRange(ps, start, end);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) byStatus.put(rs.getString("status"), rs.getLong("cnt"));
                }
            }
            report.setOrdersByStatus(byStatus);
            report.setTotalOrders(byStatus.values().stream().mapToLong(Long::longValue).sum());
            report.setCompletedOrders(byStatus.get("COMPLETED"));
            report.setCancelledOrders(byStatus.get("CANCELLED"));
            report.setProcessingOrders(byStatus.get("CONFIRMED") + byStatus.get("PREPARING")
                    + byStatus.get("READY") + byStatus.get("DELIVERING"));

            // 3. Doanh thu theo ngày (khoảng dài thì theo tháng), điền đủ cả ngày không có doanh thu
            boolean monthly = from.plusDays(MAX_DAILY_BARS).isBefore(to);
            String bucket = monthly ? "DATE_FORMAT(p.paid_at, '%Y-%m')" : "DATE_FORMAT(p.paid_at, '%Y-%m-%d')";
            Map<String, double[]> buckets = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT " + bucket + " AS bucket, COUNT(DISTINCT p.order_id) AS cnt, SUM(p.amount) AS amount " +
                    "FROM payments p WHERE " + PAID_IN_RANGE + " GROUP BY bucket")) {
                bindRange(ps, start, end);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        buckets.put(rs.getString("bucket"), new double[]{rs.getLong("cnt"), rs.getDouble("amount")});
                    }
                }
            }
            List<Row> byDate = new ArrayList<>();
            if (monthly) {
                for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
                    double[] v = buckets.getOrDefault(m.toString(), new double[]{0, 0});
                    byDate.add(new Row(m.toString(), m.format(MONTH_LABEL), (long) v[0], v[1]));
                }
            } else {
                for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                    double[] v = buckets.getOrDefault(d.toString(), new double[]{0, 0});
                    byDate.add(new Row(d.toString(), d.format(DAY_LABEL), (long) v[0], v[1]));
                }
            }
            report.setRevenueByDate(withPercent(byDate));

            // 4. Doanh thu theo hình thức (tại quán / mang đi / giao tận nơi)
            Map<String, Row> byType = new LinkedHashMap<>();
            byType.put("DINE_IN", new Row("DINE_IN", "Tại quán", 0, 0));
            byType.put("TAKEAWAY", new Row("TAKEAWAY", "Mang đi / tự đến lấy", 0, 0));
            byType.put("ONLINE", new Row("ONLINE", "Giao tận nơi", 0, 0));
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT o.order_type, COUNT(DISTINCT o.id) AS cnt, SUM(p.amount) AS amount " +
                    "FROM payments p JOIN orders o ON o.id = p.order_id WHERE " + PAID_IN_RANGE + " GROUP BY o.order_type")) {
                bindRange(ps, start, end);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("order_type");
                        Row base = byType.get(type);
                        byType.put(type, new Row(type, base != null ? base.getLabel() : type, rs.getLong("cnt"), rs.getDouble("amount")));
                    }
                }
            }
            report.setRevenueByType(withPercent(new ArrayList<>(byType.values())));

            // 5. Thanh toán thành công theo phương thức
            List<Row> payments = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.payment_method, COUNT(*) AS cnt, SUM(p.amount) AS amount FROM payments p WHERE " + PAID_IN_RANGE +
                    " GROUP BY p.payment_method ORDER BY amount DESC")) {
                bindRange(ps, start, end);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String method = rs.getString("payment_method");
                        payments.add(new Row(method, paymentLabel(method), rs.getLong("cnt"), rs.getDouble("amount")));
                    }
                }
            }
            report.setPaymentTotals(withPercent(payments));

            // 6. Top 5 món bán chạy (theo số phần) trong các đơn đã thanh toán
            List<Row> products = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT oi.product_id, MAX(oi.product_name_at_time) AS name, SUM(oi.quantity) AS qty, SUM(oi.subtotal) AS amount " +
                    "FROM order_items oi JOIN orders o ON o.id = oi.order_id " +
                    "WHERE o.status <> 'CANCELLED' AND EXISTS (SELECT 1 FROM payments p WHERE p.order_id = o.id AND " + PAID_IN_RANGE + ") " +
                    "GROUP BY oi.product_id ORDER BY qty DESC, amount DESC LIMIT 5")) {
                bindRange(ps, start, end);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        products.add(new Row(rs.getString("product_id"), rs.getString("name"), rs.getLong("qty"), rs.getDouble("amount")));
                    }
                }
            }
            report.setTopProducts(withCountPercent(products));

            // 7. Nguyên liệu xuất cho đơn nhiều nhất trong khoảng ngày
            List<Row> ingredients = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT i.name, i.unit, SUM(t.quantity) AS qty FROM inventory_transactions t " +
                    "JOIN ingredients i ON i.id = t.ingredient_id " +
                    "WHERE t.transaction_type = 'OUT_ORDER' AND t.created_at >= ? AND t.created_at < ? " +
                    "GROUP BY i.id, i.name, i.unit ORDER BY qty DESC LIMIT 5")) {
                bindRange(ps, start, end);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ingredients.add(new Row(rs.getString("unit"), rs.getString("name"), 0, rs.getDouble("qty")));
                    }
                }
            }
            report.setTopIngredientsUsed(withPercent(ingredients));

            // 8. Nguyên liệu đang dưới mức tối thiểu (tồn kho hiện tại)
            List<StockRow> low = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT name, unit, current_stock, min_stock_level FROM ingredients " +
                    "WHERE current_stock <= min_stock_level ORDER BY current_stock / GREATEST(min_stock_level, 0.001), name");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    low.add(new StockRow(rs.getString("name"), rs.getString("unit"),
                            rs.getDouble("current_stock"), rs.getDouble("min_stock_level")));
                }
            }
            report.setLowStock(low);

            // 9. Việc chờ Admin xử lý
            report.setPendingRestockRequests(count(conn, "SELECT COUNT(*) FROM restock_requests WHERE status = 'PENDING_APPROVAL'"));
            report.setPendingFeedback(count(conn, "SELECT COUNT(*) FROM customer_reviews WHERE status IN ('PENDING', 'PROCESSING')"));
        }
        return report;
    }

    private static void bindRange(PreparedStatement ps, Timestamp start, Timestamp end) throws SQLException {
        ps.setTimestamp(1, start);
        ps.setTimestamp(2, end);
    }

    private static long count(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    /** Độ dài thanh (0-100) theo số tiền so với dòng lớn nhất */
    private static List<Row> withPercent(List<Row> rows) {
        double max = rows.stream().mapToDouble(Row::getAmount).max().orElse(0);
        for (Row r : rows) r.setPercent(max <= 0 ? 0 : (int) Math.round(r.getAmount() * 100 / max));
        return rows;
    }

    /** Độ dài thanh (0-100) theo số lượng so với dòng lớn nhất */
    private static List<Row> withCountPercent(List<Row> rows) {
        long max = rows.stream().mapToLong(Row::getCount).max().orElse(0);
        for (Row r : rows) r.setPercent(max <= 0 ? 0 : (int) Math.round(r.getCount() * 100.0 / max));
        return rows;
    }

    private static String paymentLabel(String method) {
        if (method == null) return "";
        switch (method) {
            case "CASH": return "Tiền mặt";
            case "BANK_TRANSFER": return "Chuyển khoản / VietQR";
            case "VNPAY": return "VNPAY";
            case "MOMO": return "Ví MoMo";
            default: return method;
        }
    }
}
