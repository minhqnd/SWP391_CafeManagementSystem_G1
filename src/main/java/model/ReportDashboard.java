package model;
import java.math.BigDecimal; import java.util.List; import java.util.Map;
public record ReportDashboard(BigDecimal revenue,long orderCount,BigDecimal averageOrderValue,long processingCount,
 List<Map<String,Object>> revenueByDate,List<Map<String,Object>> topProducts,Map<String,BigDecimal> paymentTotals) {}
