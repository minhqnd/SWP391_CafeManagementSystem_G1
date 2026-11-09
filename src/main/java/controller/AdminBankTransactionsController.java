package controller;

import context.DBContext;
import dao.ShopSettingDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.BankTransaction;
import model.ShopSettings;
import model.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(name = "AdminBankTransactionsController", urlPatterns = {"/admin/bank-transactions"})
public class AdminBankTransactionsController extends HttpServlet {
    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();

    private static class OrderSummary {
        int id;
        String orderCode;
        double finalAmount;
        String status;
        String paymentStatus;
        String recipientName;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        ShopSettings settings = shopSettingDAO.getSettings();
        String gatewayUrl = settings.getPaymentGatewayUrl();

        List<BankTransaction> transactions = fetchBankTransactions(gatewayUrl);

        // Đối soát chi tiết từng giao dịch với hệ thống đơn hàng
        reconcileTransactions(transactions);

        // Tính tổng tiền vào và số giao dịch
        double totalInAmount = 0;
        for (BankTransaction tx : transactions) {
            totalInAmount += tx.getTransferAmount();
        }

        request.setAttribute("transactions", transactions);
        request.setAttribute("totalInAmount", totalInAmount);
        request.setAttribute("totalCount", transactions.size());
        request.setAttribute("bankAccountNo", settings.getPaymentAccountNo());
        request.setAttribute("bankName", settings.getPaymentBankName());
        request.setAttribute("gatewayUrl", gatewayUrl);

        request.getRequestDispatcher("/admin-bank-transactions.jsp").forward(request, response);
    }

    private void reconcileTransactions(List<BankTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) return;

        Set<String> orderCodes = new HashSet<>();
        for (BankTransaction tx : transactions) {
            String code = extractOrderCode(tx.getContent());
            if (code != null && !code.isEmpty()) {
                tx.setMatchedOrderCode(code);
                orderCodes.add(code);
            }
        }

        Map<String, OrderSummary> orders = fetchOrderSummaries(orderCodes);
        DecimalFormat df = new DecimalFormat("###,###,###");

        // Duyệt theo thứ tự thời gian từ cũ nhất đến mới nhất để tính luỹ kế chính xác
        List<BankTransaction> chronologicalList = new ArrayList<>(transactions);
        Collections.reverse(chronologicalList);

        Map<String, Double> cumulativeMap = new HashMap<>();

        for (BankTransaction tx : chronologicalList) {
            String code = tx.getMatchedOrderCode();
            if (code == null || code.isEmpty()) {
                tx.setReconcileStatus("NO_CODE");
                tx.setReconcileNote("Sai nội dung: Không chứa mã đơn hàng");
                continue;
            }

            OrderSummary order = orders.get(code);
            if (order == null) {
                tx.setReconcileStatus("ORDER_NOT_FOUND");
                tx.setReconcileNote("Mã #" + code + " không có trên hệ thống");
                continue;
            }

            tx.setOrderFinalAmount(order.finalAmount);
            tx.setOrderStatus(order.status);
            tx.setRecipientName(order.recipientName);

            if ("CANCELLED".equalsIgnoreCase(order.status)) {
                tx.setReconcileStatus("ORDER_CANCELLED");
                tx.setReconcileNote("Đơn #" + code + " đã bị hủy");
                continue;
            }

            double expected = order.finalAmount;
            double prevAccumulated = cumulativeMap.getOrDefault(code, 0.0);
            double currentAccumulated = prevAccumulated + tx.getTransferAmount();
            cumulativeMap.put(code, currentAccumulated);

            if (prevAccumulated >= expected) {
                // Đơn hàng đã được thanh toán đủ ở giao dịch trước, giao dịch này là thừa/trùng lặp
                tx.setReconcileStatus("DUPLICATE");
                tx.setDiffAmount(tx.getTransferAmount());
                tx.setReconcileNote("Trùng đơn: Khách chuyển lặp lại (Đơn đã thanh toán)");
            } else if (Math.abs(currentAccumulated - expected) < 1.0) {
                // Khớp chính xác 100%
                tx.setReconcileStatus("MATCHED");
                tx.setDiffAmount(0);
                if (prevAccumulated == 0) {
                    tx.setReconcileNote("Khớp chuẩn 100% (" + df.format(expected).replace(",", ".") + " đ)");
                } else {
                    tx.setReconcileNote("Bù đủ tiền: Đợt 2 (Tổng nhận " + df.format(currentAccumulated).replace(",", ".") + " đ)");
                }
            } else if (currentAccumulated > expected) {
                // Chuyển thừa tiền
                double over = currentAccumulated - expected;
                tx.setReconcileStatus("OVERPAID");
                tx.setDiffAmount(over);
                tx.setReconcileNote("Thừa +" + df.format(over).replace(",", ".") + " đ (Đơn " + df.format(expected).replace(",", ".") + " đ)");
            } else {
                // Chuyển thiếu tiền
                double under = expected - currentAccumulated;
                tx.setReconcileStatus("UNDERPAID");
                tx.setDiffAmount(-under);
                tx.setReconcileNote("Thiếu -" + df.format(under).replace(",", ".") + " đ (Đơn " + df.format(expected).replace(",", ".") + " đ)");
            }
        }
    }

    private Map<String, OrderSummary> fetchOrderSummaries(Set<String> orderCodes) {
        Map<String, OrderSummary> map = new HashMap<>();
        if (orderCodes == null || orderCodes.isEmpty()) return map;

        StringBuilder sql = new StringBuilder(
            "SELECT o.id, o.order_code, o.final_amount, o.status, o.recipient_name, " +
            "COALESCE(p.status, 'PENDING') AS payment_status " +
            "FROM orders o " +
            "LEFT JOIN payments p ON p.order_id = o.id " +
            "WHERE o.order_code IN ("
        );
        for (int i = 0; i < orderCodes.size(); i++) {
            sql.append(i > 0 ? ",?" : "?");
        }
        sql.append(")");

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String code : orderCodes) {
                ps.setString(idx++, code);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderSummary os = new OrderSummary();
                    os.id = rs.getInt("id");
                    os.orderCode = rs.getString("order_code");
                    os.finalAmount = rs.getDouble("final_amount");
                    os.status = rs.getString("status");
                    os.paymentStatus = rs.getString("payment_status");
                    os.recipientName = rs.getString("recipient_name");
                    map.put(os.orderCode, os);
                }
            }
        } catch (Exception e) {
            System.err.println("[AdminBankTransactionsController] Lỗi tra cứu đơn hàng đối soát: " + e.getMessage());
        }
        return map;
    }

    private List<BankTransaction> fetchBankTransactions(String gatewayUrl) {
        List<BankTransaction> list = new ArrayList<>();
        if (gatewayUrl == null || gatewayUrl.trim().isEmpty()) {
            return list;
        }

        try {
            String baseUrl = gatewayUrl.trim().replaceAll("/+$", "");
            URL url = new URL(baseUrl + "/api/transactions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                return list;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            conn.disconnect();

            String json = sb.toString();
            list = parseWebhooksFromJson(json);
        } catch (Exception e) {
            System.err.println("[AdminBankTransactionsController] Lỗi tải giao dịch ngân hàng: " + e.getMessage());
        }

        return list;
    }

    private List<BankTransaction> parseWebhooksFromJson(String json) {
        List<BankTransaction> list = new ArrayList<>();
        int webhooksIndex = json.indexOf("\"webhooks\":");
        if (webhooksIndex == -1) return list;

        String webhooksPart = json.substring(webhooksIndex);
        int startBracket = webhooksPart.indexOf("[");
        int endBracket = webhooksPart.lastIndexOf("]");
        if (startBracket == -1 || endBracket == -1 || startBracket >= endBracket) return list;

        String arrayContent = webhooksPart.substring(startBracket + 1, endBracket);

        // Tách các đối tượng JSON trong mảng
        Pattern objPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher objMatcher = objPattern.matcher(arrayContent);

        while (objMatcher.find()) {
            String objStr = objMatcher.group(1);
            BankTransaction tx = new BankTransaction();

            tx.setSepayId(extractLong(objStr, "sepay_id", 0));
            tx.setGateway(extractString(objStr, "gateway", "MBBank"));
            tx.setAccountNumber(extractString(objStr, "account_number", ""));
            tx.setTransferType(extractString(objStr, "transfer_type", "in"));
            tx.setTransferAmount(extractDouble(objStr, "transfer_amount", 0.0));
            tx.setContent(extractString(objStr, "content", ""));
            tx.setReferenceCode(extractString(objStr, "reference_code", ""));
            tx.setTransactionDate(extractString(objStr, "transaction_date", ""));

            list.add(tx);
        }

        return list;
    }

    private String extractOrderCode(String content) {
        if (content == null) return null;
        // Bắt mã đơn dạng AURA26... hoặc ORD-...
        Matcher m = Pattern.compile("(AURA\\d+|ORD-[A-Z0-9-]+)").matcher(content.toUpperCase());
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private String extractString(String src, String key, String def) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(src);
        if (m.find()) return m.group(1);
        return def;
    }

    private long extractLong(String src, String key, long def) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(src);
        if (m.find()) {
            try { return Long.parseLong(m.group(1)); } catch (Exception ignored) {}
        }
        return def;
    }

    private double extractDouble(String src, String key, double def) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
        Matcher m = p.matcher(src);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
        }
        return def;
    }

    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/?tab=login");
            return false;
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendError(403, "Chỉ quản trị viên mới có quyền xem giao dịch ngân hàng.");
            return false;
        }
        return true;
    }
}
