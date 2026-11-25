package service;

import dao.BankTransactionDAO;
import dao.BankTransactionDAO.OrderInfo;
import dao.BankTransactionDAO.RefundRecord;
import model.BankTransaction;
import model.ShopSettings;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BankTransactionService - Xử lý tải giao dịch từ SePay Gateway/MySQL, đối soát đơn hàng và tính tổng thực nhận.
 */
public class BankTransactionService {

    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile(
            "(AURA[0-9][A-Z0-9]*|ORD-(\\d{6})-([A-Z0-9]+)|ORD(\\d{6})([A-F0-9]{8}))(?![A-Z0-9])"
    );
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    private final BankTransactionDAO bankTransactionDAO;

    public BankTransactionService() {
        this(new BankTransactionDAO());
    }

    public BankTransactionService(BankTransactionDAO bankTransactionDAO) {
        this.bankTransactionDAO = bankTransactionDAO;
    }

    // =========================================================================
    // Nhóm 1: Luồng tải, đối soát và gắn trạng thái hoàn tiền cho trang admin
    // =========================================================================

    /** Tải toàn bộ giao dịch (Gateway + DB cục bộ), đối soát đơn và gắn thông tin hoàn tiền. */
    public List<BankTransaction> loadTransactions(ShopSettings settings) {
        String gatewayUrl = settings != null ? settings.getPaymentGatewayUrl() : "";
        List<BankTransaction> transactions = fetchAllTransactions(gatewayUrl, settings);
        reconcileTransactions(transactions);
        applyRefundRecords(transactions);
        return transactions;
    }

    private List<BankTransaction> fetchAllTransactions(String gatewayUrl, ShopSettings settings) {
        List<BankTransaction> list = new ArrayList<>();
        Set<String> seenRefs = new HashSet<>();
        Set<String> seenOrders = new HashSet<>();

        String json = fetchGatewayJson(gatewayUrl);
        if (json != null) {
            List<BankTransaction> ordersFromGateway = parseOrdersFromJson(json, settings);
            List<BankTransaction> webhooksFromGateway = parseWebhooksFromJson(json);

            for (BankTransaction tx : ordersFromGateway) {
                String ref = normalizeKey(tx.getReferenceCode());
                if (!ref.isEmpty()) {
                    seenRefs.add(ref);
                }
                String code = normalizeKey(extractOrderCode(tx.getContent()));
                if (!code.isEmpty()) {
                    seenOrders.add(code);
                }
                list.add(tx);
            }

            // Chỉ thêm webhook chưa trùng mã tham chiếu với danh sách đơn từ Gateway
            for (BankTransaction tx : webhooksFromGateway) {
                String ref = normalizeKey(tx.getReferenceCode());
                if (!ref.isEmpty() && seenRefs.contains(ref)) {
                    continue;
                }
                list.add(tx);
            }
        }

        // Bổ sung các đơn BANK_TRANSFER đã SUCCESS trong MySQL cục bộ nếu chưa có ở trên
        List<BankTransaction> localPayments = bankTransactionDAO.findLocalBankPayments(settings);
        for (BankTransaction tx : localPayments) {
            String ref = normalizeKey(tx.getReferenceCode());
            String code = normalizeKey(extractOrderCode(tx.getContent()));
            if (!ref.isEmpty() && seenRefs.contains(ref)) {
                continue;
            }
            if (!code.isEmpty() && seenOrders.contains(code)) {
                continue;
            }
            list.add(tx);
        }

        // Sắp xếp giao dịch mới nhất lên đầu bảng
        list.sort((a, b) -> {
            String da = a.getTransactionDate() != null ? a.getTransactionDate() : "";
            String db = b.getTransactionDate() != null ? b.getTransactionDate() : "";
            return db.compareTo(da);
        });
        return list;
    }

    private void reconcileTransactions(List<BankTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }
        Set<String> orderCodes = new HashSet<>();
        for (BankTransaction tx : transactions) {
            String code = extractOrderCode(tx.getContent());
            if (code != null && !code.isEmpty()) {
                tx.setMatchedOrderCode(code);
                orderCodes.add(code);
            }
        }
        Map<String, OrderInfo> orders = bankTransactionDAO.findOrderSummaries(orderCodes);
        reconcileTransactionsWithOrders(transactions, orders);
    }

    private void applyRefundRecords(List<BankTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }
        Map<String, RefundRecord> refundMap = bankTransactionDAO.findAllRefunds();
        for (BankTransaction tx : transactions) {
            RefundRecord rec = refundMap.get(tx.getTxKey());
            if (rec != null) {
                tx.setRefunded(true);
                tx.setRefundedAmount(rec.amount);
                tx.setRefundNote(rec.note);
                tx.setRefundedAt(rec.refundedAt);
            }
        }
    }

    // =========================================================================
    // Nhóm 2: Tính tổng tiền vào thực nhận và tổng tiền đã hoàn
    // =========================================================================

    /** Tính tổng tiền vào sau khi trừ phần đã hoàn của từng giao dịch. */
    public double calculateNetInAmount(List<BankTransaction> transactions) {
        if (transactions == null) {
            return 0;
        }
        double total = 0;
        for (BankTransaction tx : transactions) {
            if (tx.isRefunded()) {
                total += Math.max(0, tx.getTransferAmount() - tx.getRefundedAmount());
            } else {
                total += tx.getTransferAmount();
            }
        }
        return total;
    }

    /** Tính tổng số tiền đã đánh dấu hoàn tiền. */
    public double calculateTotalRefunded(List<BankTransaction> transactions) {
        if (transactions == null) {
            return 0;
        }
        double total = 0;
        for (BankTransaction tx : transactions) {
            if (tx.isRefunded()) {
                total += tx.getRefundedAmount();
            }
        }
        return total;
    }

    // =========================================================================
    // Nhóm 3: Thuật toán đối soát giao dịch với đơn hàng
    // =========================================================================

    /** Đối soát danh sách giao dịch với map đơn hàng, tính luỹ kế từ giao dịch cũ nhất đến mới nhất. */
    public void reconcileTransactionsWithOrders(List<BankTransaction> transactions, Map<String, OrderInfo> orders) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }

        // Duyệt từ cũ đến mới để phát hiện đúng giao dịch chuyển đủ trước và giao dịch chuyển trùng sau
        List<BankTransaction> chronologicalList = new ArrayList<>(transactions);
        Collections.reverse(chronologicalList);

        Map<String, Double> cumulativeMap = new HashMap<>();
        for (BankTransaction tx : chronologicalList) {
            reconcileSingleTransaction(tx, orders, cumulativeMap);
        }
    }

    private void reconcileSingleTransaction(
            BankTransaction tx,
            Map<String, OrderInfo> orders,
            Map<String, Double> cumulativeMap
    ) {
        String code = tx.getMatchedOrderCode();
        if (code == null || code.isEmpty()) {
            code = extractOrderCode(tx.getContent());
            tx.setMatchedOrderCode(code);
        }
        if (code == null || code.isEmpty()) {
            tx.setReconcileStatus("NO_CODE");
            tx.setReconcileNote("Sai nội dung: Không chứa mã đơn hàng");
            return;
        }

        OrderInfo order = findOrderIgnoreCase(orders, code);
        if (order == null) {
            tx.setReconcileStatus("ORDER_NOT_FOUND");
            tx.setReconcileNote("Mã #" + code + " không có trên hệ thống");
            return;
        }

        tx.setOrderFinalAmount(order.finalAmount);
        tx.setOrderStatus(order.status);
        tx.setRecipientName(order.recipientName);

        if ("CANCELLED".equalsIgnoreCase(order.status)) {
            tx.setReconcileStatus("ORDER_CANCELLED");
            tx.setReconcileNote("Đơn #" + code + " đã bị hủy");
            return;
        }

        double expected = order.finalAmount;
        double prevAccumulated = cumulativeMap.getOrDefault(code, 0.0);
        double currentAccumulated = prevAccumulated + tx.getTransferAmount();
        cumulativeMap.put(code, currentAccumulated);

        if (prevAccumulated >= expected) {
            tx.setReconcileStatus("DUPLICATE");
            tx.setDiffAmount(tx.getTransferAmount());
            tx.setReconcileNote("Trùng đơn: Khách chuyển lặp lại (Đơn đã thanh toán)");
        } else if (Math.abs(currentAccumulated - expected) < 1.0) {
            tx.setReconcileStatus("MATCHED");
            tx.setDiffAmount(0);
            if (prevAccumulated == 0) {
                tx.setReconcileNote("Khớp chuẩn 100% (" + formatVnd(expected) + " đ)");
            } else {
                tx.setReconcileNote("Bù đủ tiền: Đợt 2 (Tổng nhận " + formatVnd(currentAccumulated) + " đ)");
            }
        } else if (currentAccumulated > expected) {
            double over = currentAccumulated - expected;
            tx.setReconcileStatus("OVERPAID");
            tx.setDiffAmount(over);
            tx.setReconcileNote("Thừa +" + formatVnd(over) + " đ (Đơn " + formatVnd(expected) + " đ)");
        } else {
            double under = expected - currentAccumulated;
            tx.setReconcileStatus("UNDERPAID");
            tx.setDiffAmount(-under);
            tx.setReconcileNote("Thiếu -" + formatVnd(under) + " đ (Đơn " + formatVnd(expected) + " đ)");
        }
    }

    /** Trích mã đơn hàng (dạng AURA... hoặc ORD...) từ nội dung chuyển khoản. */
    public String extractOrderCode(String content) {
        if (content == null) {
            return null;
        }
        Matcher m = ORDER_CODE_PATTERN.matcher(content.toUpperCase(Locale.ROOT));
        if (m.find()) {
            return m.group(4) == null ? m.group(1) : "ORD-" + m.group(4) + "-" + m.group(5);
        }
        return null;
    }

    private OrderInfo findOrderIgnoreCase(Map<String, OrderInfo> orders, String code) {
        if (orders == null || code == null) {
            return null;
        }
        OrderInfo exact = orders.get(code);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, OrderInfo> entry : orders.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(code)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String formatVnd(double amount) {
        return new DecimalFormat("###,###,###").format(amount).replace(",", ".");
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    // =========================================================================
    // Nhóm 4: Gọi API Cloudflare Worker Gateway và parse JSON trả về
    // =========================================================================

    private String fetchGatewayJson(String gatewayUrl) {
        if (gatewayUrl == null || gatewayUrl.trim().isEmpty()) {
            return null;
        }
        HttpURLConnection conn = null;
        try {
            String baseUrl = gatewayUrl.trim().replaceAll("/+$", "");
            URL url = new URL(baseUrl + "/api/transactions");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Java-AuraCafe/1.0");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            if (conn.getResponseCode() != 200) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            System.err.println("[BankTransactionService] Lỗi tải giao dịch ngân hàng từ gateway: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private List<BankTransaction> parseOrdersFromJson(String json, ShopSettings settings) {
        List<BankTransaction> list = new ArrayList<>();
        int ordersIndex = json.indexOf("\"orders\":");
        if (ordersIndex == -1) {
            return list;
        }

        int startBracket = json.indexOf("[", ordersIndex);
        if (startBracket == -1) {
            return list;
        }

        int webhooksIndex = json.indexOf("\"webhooks\":", ordersIndex);
        int endBracket = webhooksIndex != -1
                ? json.lastIndexOf("]", webhooksIndex)
                : json.indexOf("]", startBracket);
        if (endBracket == -1 || startBracket >= endBracket) {
            return list;
        }

        String arrayContent = json.substring(startBracket + 1, endBracket);
        Matcher objMatcher = JSON_OBJECT_PATTERN.matcher(arrayContent);
        String bankName = BankTransactionDAO.resolveBankName(settings);
        String bankAcc = BankTransactionDAO.resolveBankAccount(settings);

        while (objMatcher.find()) {
            String objStr = objMatcher.group(1);
            String status = extractString(objStr, "status", "");
            String gatewayRef = extractString(objStr, "gateway_reference", "");

            // Chỉ lấy đơn đã thanh toán thành công hoặc đã có mã tham chiếu ngân hàng
            boolean hasGatewayRef = gatewayRef != null && !gatewayRef.isEmpty() && !"null".equals(gatewayRef);
            if (!"SUCCESS".equalsIgnoreCase(status) && !hasGatewayRef) {
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
            tx.setReferenceCode(hasGatewayRef ? gatewayRef : ("FT" + (id > 0 ? id : orderCode)));
            tx.setTransactionDate(adjustUtcToVietnamTime(paidAt));

            list.add(tx);
        }
        return list;
    }

    private List<BankTransaction> parseWebhooksFromJson(String json) {
        List<BankTransaction> list = new ArrayList<>();
        int webhooksIndex = json.indexOf("\"webhooks\":");
        if (webhooksIndex == -1) {
            return list;
        }

        String webhooksPart = json.substring(webhooksIndex);
        int startBracket = webhooksPart.indexOf("[");
        int endBracket = webhooksPart.lastIndexOf("]");
        if (startBracket == -1 || endBracket == -1 || startBracket >= endBracket) {
            return list;
        }

        String arrayContent = webhooksPart.substring(startBracket + 1, endBracket);
        Matcher objMatcher = JSON_OBJECT_PATTERN.matcher(arrayContent);

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

    /** Chuyển thời gian UTC từ Gateway sang giờ Việt Nam (+7 tiếng). */
    public String adjustUtcToVietnamTime(String utcStr) {
        if (utcStr == null || utcStr.trim().isEmpty() || "null".equals(utcStr)) {
            return "";
        }
        try {
            String clean = utcStr.trim().replace("T", " ");
            int dotIdx = clean.indexOf('.');
            if (dotIdx != -1) {
                clean = clean.substring(0, dotIdx);
            }
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime ldt = LocalDateTime.parse(clean, dtf);
            return ldt.plusHours(7).format(dtf);
        } catch (Exception e) {
            return utcStr;
        }
    }

    private String extractString(String src, String key, String def) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(src);
        return m.find() ? m.group(1) : def;
    }

    private long extractLong(String src, String key, long def) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(src);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (Exception ignored) {
            }
        }
        return def;
    }

    private double extractDouble(String src, String key, double def) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+(?:\\.\\d+)?)").matcher(src);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (Exception ignored) {
            }
        }
        return def;
    }
}
