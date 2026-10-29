package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ReportDashboard - Số liệu màn Báo cáo (UC66 View Business Reports) cho một khoảng ngày.
 * Doanh thu = tổng các thanh toán thành công (payments.status = SUCCESS) có paid_at trong khoảng ngày.
 */
public class ReportDashboard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Một dòng số liệu dạng nhãn + số lượng + số tiền (theo ngày, theo hình thức, theo phương thức...) */
    public static class Row implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String key;
        private final String label;
        private final long count;
        private final double amount;
        private int percent;

        public Row(String key, String label, long count, double amount) {
            this.key = key;
            this.label = label;
            this.count = count;
            this.amount = amount;
        }

        public String getKey() { return key; }
        public String getLabel() { return label; }
        public long getCount() { return count; }
        public double getAmount() { return amount; }
        /** Tỉ lệ so với giá trị lớn nhất trong nhóm (độ dài thanh biểu đồ, 0-100) */
        public int getPercent() { return percent; }
        public void setPercent(int percent) { this.percent = percent; }
    }

    /** Nguyên liệu dưới mức tối thiểu */
    public static class StockRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String name;
        private final String unit;
        private final double currentStock;
        private final double minStockLevel;

        public StockRow(String name, String unit, double currentStock, double minStockLevel) {
            this.name = name;
            this.unit = unit;
            this.currentStock = currentStock;
            this.minStockLevel = minStockLevel;
        }

        public String getName() { return name; }
        public String getUnit() { return unit; }
        public double getCurrentStock() { return currentStock; }
        public double getMinStockLevel() { return minStockLevel; }
    }

    private double revenue;
    private long paidOrderCount;
    private long totalOrders;
    private long completedOrders;
    private long cancelledOrders;
    private long processingOrders;
    private List<Row> revenueByDate = new ArrayList<>();
    private List<Row> revenueByType = new ArrayList<>();
    private List<Row> paymentTotals = new ArrayList<>();
    private List<Row> topProducts = new ArrayList<>();
    private Map<String, Long> ordersByStatus = new LinkedHashMap<>();
    private List<Row> topIngredientsUsed = new ArrayList<>();
    private List<StockRow> lowStock = new ArrayList<>();
    private long pendingRestockRequests;
    private long pendingFeedback;

    public double getRevenue() { return revenue; }
    public void setRevenue(double revenue) { this.revenue = revenue; }

    public long getPaidOrderCount() { return paidOrderCount; }
    public void setPaidOrderCount(long paidOrderCount) { this.paidOrderCount = paidOrderCount; }

    /** Giá trị trung bình mỗi đơn đã thanh toán */
    public double getAverageOrderValue() { return paidOrderCount == 0 ? 0 : revenue / paidOrderCount; }

    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }

    public long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(long completedOrders) { this.completedOrders = completedOrders; }

    public long getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(long cancelledOrders) { this.cancelledOrders = cancelledOrders; }

    public long getProcessingOrders() { return processingOrders; }
    public void setProcessingOrders(long processingOrders) { this.processingOrders = processingOrders; }

    public List<Row> getRevenueByDate() { return revenueByDate; }
    public void setRevenueByDate(List<Row> revenueByDate) { this.revenueByDate = revenueByDate; }

    public List<Row> getRevenueByType() { return revenueByType; }
    public void setRevenueByType(List<Row> revenueByType) { this.revenueByType = revenueByType; }

    public List<Row> getPaymentTotals() { return paymentTotals; }
    public void setPaymentTotals(List<Row> paymentTotals) { this.paymentTotals = paymentTotals; }

    public List<Row> getTopProducts() { return topProducts; }
    public void setTopProducts(List<Row> topProducts) { this.topProducts = topProducts; }

    public Map<String, Long> getOrdersByStatus() { return ordersByStatus; }
    public void setOrdersByStatus(Map<String, Long> ordersByStatus) { this.ordersByStatus = ordersByStatus; }

    public List<Row> getTopIngredientsUsed() { return topIngredientsUsed; }
    public void setTopIngredientsUsed(List<Row> topIngredientsUsed) { this.topIngredientsUsed = topIngredientsUsed; }

    public List<StockRow> getLowStock() { return lowStock; }
    public void setLowStock(List<StockRow> lowStock) { this.lowStock = lowStock; }

    public long getPendingRestockRequests() { return pendingRestockRequests; }
    public void setPendingRestockRequests(long pendingRestockRequests) { this.pendingRestockRequests = pendingRestockRequests; }

    public long getPendingFeedback() { return pendingFeedback; }
    public void setPendingFeedback(long pendingFeedback) { this.pendingFeedback = pendingFeedback; }
}
