package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Lớp hỗ trợ kết nối sang Cloudflare Worker Payment Gateway để đăng ký đơn và kiểm tra thanh toán QR.
 */
public final class PaymentService {

    private PaymentService() {
    }

    /**
     * 1. Gửi thông tin đơn hàng mới lên Cloudflare Worker để chờ nhận chuyển khoản SePay.
     */
    public static void registerOrderToGateway(String gatewayUrl, String appId, String orderCode,
                                              double amount, String transactionCode) {
        if (gatewayUrl == null || gatewayUrl.trim().isEmpty()) {
            return;
        }
        try {
            String baseUrl = gatewayUrl.trim();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            long roundedAmount = Math.round(amount);
            String jsonBody = "{"
                    + "\"appId\":\"" + clean(appId) + "\","
                    + "\"orderCode\":\"" + clean(orderCode) + "\","
                    + "\"amount\":" + roundedAmount + ","
                    + "\"transactionCode\":\"" + clean(transactionCode) + "\""
                    + "}";

            URL url = new URL(baseUrl + "/api/orders/register");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {
            System.err.println("[PaymentService] Không thể đăng ký đơn lên Gateway: " + e.getMessage());
        }
    }

    /**
     * 2. Hỏi Cloudflare Worker xem đơn hàng đã được chuyển khoản thành công (status = SUCCESS) chưa.
     */
    public static boolean isOrderPaidOnGateway(String gatewayUrl, String orderCode) {
        if (gatewayUrl == null || gatewayUrl.trim().isEmpty() || orderCode == null || orderCode.trim().isEmpty()) {
            return false;
        }
        try {
            String baseUrl = gatewayUrl.trim();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            String encodedCode = URLEncoder.encode(clean(orderCode), StandardCharsets.UTF_8);
            URL url = new URL(baseUrl + "/api/orders/" + encodedCode + "/status");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                return false;
            }

            StringBuilder responseText = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseText.append(line);
                }
            }
            conn.disconnect();

            String json = responseText.toString();
            // Kiểm tra xem JSON trả về có chứa "status":"SUCCESS" hay không
            return json.contains("\"status\":\"SUCCESS\"") || json.contains("\"status\": \"SUCCESS\"");
        } catch (Exception e) {
            return false;
        }
    }

    // Các hàm dưới đây giữ lại để tương thích với endpoint /payment/callback
    public static String canonical(String orderCode, String transactionCode, String amount, String status) {
        return clean(orderCode) + "|" + clean(transactionCode) + "|" + clean(amount) + "|" + clean(status).toUpperCase();
    }

    public static String sign(String secret, String canonicalPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) result.append(String.format("%02x", b & 0xff));
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign payment callback", e);
        }
    }

    public static boolean verify(String secret, String canonicalPayload, String suppliedSignature) {
        if (suppliedSignature == null) return false;
        return MessageDigest.isEqual(sign(secret, canonicalPayload).getBytes(StandardCharsets.UTF_8),
                suppliedSignature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
