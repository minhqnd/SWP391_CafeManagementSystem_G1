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

    static class OrderSummary {
        int id;
        String orderCode;
        double finalAmount;
        String status;
        String paymentStatus;
        String recipientName;

        OrderSummary() {}

        OrderSummary(int id, String orderCode, double finalAmount, String status) {
            this.id = id;
            this.orderCode = orderCode;
            this.finalAmount = finalAmount;
            this.status = status;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        ShopSettings settings = shopSettingDAO.getSettings();
        String gatewayUrl = settings.getPaymentGatewayUrl();

        List<BankTransaction> transactions = fetchBankTransactions(gatewayUrl, settings);

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
        reconcileTransactionsWithOrders(transactions, orders);
    }

    void reconcileTransactionsWithOrders(List<BankTransaction> transactions, Map<String, OrderSummary> orders) {
        if (transactions == null || transactions.isEmpty()) return;
        DecimalFormat df = new DecimalFormat("###,###,###");

        // Duyệt theo thứ tự thời gian từ cũ nhất đến mới nhất để tính luỹ kế chính xác
        List<BankTransaction> chronologicalList = new ArrayList<>(transactions);
        Collections.reverse(chronologicalList);

        Map<String, Double> cumulativeMap = new HashMap<>();

        for (BankTransaction tx : chronologicalList) {
            String code = tx.getMatchedOrderCode();
            if (code == null || code.isEmpty()) {
                code = extractOrderCode(tx.getContent());
                tx.setMatchedOrderCode(code);
            }
            if (code == null || code.isEmpty()) {
                tx.setReconcileStatus("NO_CODE");
                tx.setReconcileNote("Sai nội dung: Không chứa mã đơn hàng");
                continue;
            }

            OrderSummary order = orders != null ? orders.get(code) : null;
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

    private List<BankTransaction> fetchBankTransactions(String gatewayUrl, ShopSettings settings) {
        List<BankTransaction> list = new ArrayList<>();
        Set<String> seenRefs = new HashSet<>();
        Set<String> seenOrders = new HashSet<>();

        if (gatewayUrl != null && !gatewayUrl.trim().isEmpty()) {
            try {
                String baseUrl = gatewayUrl.trim().replaceAll("/+$", "");
                URL url = new URL(baseUrl + "/api/transactions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Java-AuraCafe/1.0");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }

                    String json = sb.toString();
                    List<BankTransaction> ordersFromGateway = parseOrdersFromJson(json, settings);
                    List<BankTransaction> webhooksFromGateway = parseWebhooksFromJson(json);

                    for (BankTransaction tx : ordersFromGateway) {
                        String ref = tx.getReferenceCode() != null ? tx.getReferenceCode().trim().toUpperCase() : "";
                        if (!ref.isEmpty()) seenRefs.add(ref);
                        String code = extractOrderCode(tx.getContent());
                        if (code != null) seenOrders.add(code.trim().toUpperCase());
                        list.add(tx);
                    }

                    for (BankTransaction tx : webhooksFromGateway) {
                        String ref = tx.getReferenceCode() != null ? tx.getReferenceCode().trim().toUpperCase() : "";
                        if (!ref.isEmpty() && seenRefs.contains(ref)) {
                            continue;
                        }
                        list.add(tx);
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                System.err.println("[AdminBankTransactionsController] Lỗi tải giao dịch ngân hàng từ gateway: " + e.getMessage());
            }
        }

        // Bổ sung các đơn đã thanh toán BANK_TRANSFER từ local MySQL nếu chưa có trong danh sách
        List<BankTransaction> localPayments = fetchLocalBankPayments(settings);
        for (BankTransaction tx : localPayments) {
            String ref = tx.getReferenceCode() != null ? tx.getReferenceCode().trim().toUpperCase() : "";
            String code = extractOrderCode(tx.getContent());
            code = code != null ? code.trim().toUpperCase() : "";

            if (!ref.isEmpty() && seenRefs.contains(ref)) {
                continue;
            }
            if (!code.isEmpty() && seenOrders.contains(code)) {
                continue;
            }
            list.add(tx);
        }

        // Sắp xếp giảm dần theo thời gian (giao dịch mới nhất lên đầu bảng)
        list.sort((a, b) -> {
            String da = a.getTransactionDate() != null ? a.getTransactionDate() : "";
            String db = b.getTransactionDate() != null ? b.getTransactionDate() : "";
            return db.compareTo(da);
        });

        return list;
    }

    private List<BankTransaction> parseOrdersFromJson(String json, ShopSettings settings) {
        List<BankTransaction> list = new ArrayList<>();
        int ordersIndex = json.indexOf("\"orders\":");
        if (ordersIndex == -1) return list;

        int startBracket = json.indexOf("[", ordersIndex);
        if (startBracket == -1) return list;

        int webhooksIndex = json.indexOf("\"webhooks\":", ordersIndex);
        int endBracket;
        if (webhooksIndex != -1) {
            endBracket = json.lastIndexOf("]", webhooksIndex);
        } else {
            endBracket = json.indexOf("]", startBracket);
        }
        if (endBracket == -1 || startBracket >= endBracket) return list;

        String arrayContent = json.substring(startBracket + 1, endBracket);
        Pattern objPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher objMatcher = objPattern.matcher(arrayContent);

        String bankName = (settings != null && settings.getPaymentBankName() != null && !settings.getPaymentBankName().isEmpty())
                ? settings.getPaymentBankName() : "MBBank";
        String bankAcc = (settings != null && settings.getPaymentAccountNo() != null && !settings.getPaymentAccountNo().isEmpty())
                ? settings.getPaymentAccountNo() : "234556688";

        while (objMatcher.find()) {
            String objStr = objMatcher.group(1);
            String status = extractString(objStr, "status", "");
            String gatewayRef = extractString(objStr, "gateway_reference", "");

            // Chỉ lấy các đơn đã thanh toán thành công (có status SUCCESS hoặc có mã tham chiếu)
            if (!"SUCCESS".equalsIgnoreCase(status) && (gatewayRef == null || gatewayRef.isEmpty() || "null".equals(gatewayRef))) {
                continue;
            }

            long id = extractLong(objStr, "id", 0);
            String orderCode = extractString(objStr, "order_code", "");
            if (orderCode == null || orderCode.isEmpty() || "null".equals(orderCode)) {
                continue;
            }
            double amount = extractDouble(objStr, "amount", 0.0);
            String paidAt = extractString(objStr, "paid_at", "");
            if (paidAt == null || paidAt.isEmpty() || "null".equals(paidAt)) {
                paidAt = extractString(objStr, "created_at", "");
            }

            BankTransaction tx = new BankTransaction();
            tx.setSepayId(id > 0 ? (98000000L + id) : 0);
            tx.setGateway(bankName);
            tx.setAccountNumber(bankAcc);
            tx.setTransferType("in");
            tx.setTransferAmount(amount);
            tx.setContent(orderCode + " CHUYEN TIEN THANH TOAN DON HANG");
            tx.setReferenceCode(gatewayRef != null && !gatewayRef.isEmpty() && !"null".equals(gatewayRef)
                    ? gatewayRef
                    : ("FT" + (id > 0 ? id : orderCode)));
            tx.setTransactionDate(adjustUtcToVietnamTime(paidAt));

            list.add(tx);
        }
        return list;
    }

    private List<BankTransaction> fetchLocalBankPayments(ShopSettings settings) {
        List<BankTransaction> list = new ArrayList<>();
        String sql = "SELECT p.id, p.order_id, p.amount, p.status, p.paid_at, p.gateway_reference, " +
                     "o.order_code " +
                     "FROM payments p " +
                     "JOIN orders o ON p.order_id = o.id " +
                     "WHERE p.payment_method = 'BANK_TRANSFER' AND p.status = 'SUCCESS' " +
                     "ORDER BY p.id DESC LIMIT 50";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            String bankName = (settings != null && settings.getPaymentBankName() != null && !settings.getPaymentBankName().isEmpty())
                    ? settings.getPaymentBankName() : "MBBank";
            String bankAcc = (settings != null && settings.getPaymentAccountNo() != null && !settings.getPaymentAccountNo().isEmpty())
                    ? settings.getPaymentAccountNo() : "234556688";

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            while (rs.next()) {
                long pId = rs.getLong("id");
                String orderCode = rs.getString("order_code");
                double amount = rs.getDouble("amount");
                java.sql.Timestamp paidAt = rs.getTimestamp("paid_at");
                String ref = rs.getString("gateway_reference");

                BankTransaction tx = new BankTransaction();
                tx.setSepayId(99000000L + pId);
                tx.setGateway(bankName);
                tx.setAccountNumber(bankAcc);
                tx.setTransferType("in");
                tx.setTransferAmount(amount);
                tx.setContent(orderCode + " CHUYEN TIEN THANH TOAN DON HANG");
                tx.setReferenceCode(ref != null && !ref.isEmpty() ? ref : ("FT" + pId));
                tx.setTransactionDate(paidAt != null ? sdf.format(paidAt) : "");

                list.add(tx);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private String adjustUtcToVietnamTime(String utcStr) {
        if (utcStr == null || utcStr.trim().isEmpty() || "null".equals(utcStr)) {
            return "";
        }
        try {
            String clean = utcStr.trim().replace("T", " ");
            int dotIdx = clean.indexOf('.');
            if (dotIdx != -1) {
                clean = clean.substring(0, dotIdx);
            }
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(clean, dtf);
            java.time.LocalDateTime vnTime = ldt.plusHours(7);
            return vnTime.format(dtf);
        } catch (Exception e) {
            return utcStr;
        }
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
