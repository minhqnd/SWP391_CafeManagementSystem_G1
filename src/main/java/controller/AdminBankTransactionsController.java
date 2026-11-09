package controller;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(name = "AdminBankTransactionsController", urlPatterns = {"/admin/bank-transactions"})
public class AdminBankTransactionsController extends HttpServlet {
    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request, response)) return;

        ShopSettings settings = shopSettingDAO.getSettings();
        String gatewayUrl = settings.getPaymentGatewayUrl();

        List<BankTransaction> transactions = fetchBankTransactions(gatewayUrl);

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

        Set<String> seenOrderCodes = new HashSet<>();

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

            // Dò tìm mã đơn hàng trong nội dung chuyển khoản
            String matchedCode = extractOrderCode(tx.getContent());
            if (matchedCode != null && !matchedCode.isEmpty()) {
                tx.setMatchedOrderCode(matchedCode);
                if (seenOrderCodes.contains(matchedCode)) {
                    tx.setReconcileStatus("DUPLICATE"); // Chuyển trùng / chuyển thừa lần 2
                } else {
                    tx.setReconcileStatus("MATCHED"); // Khớp đơn
                    seenOrderCodes.add(matchedCode);
                }
            } else {
                tx.setReconcileStatus("UNMATCHED"); // Chưa xác định đơn
            }

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
