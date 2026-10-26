package service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Shared contract for the local gateway simulator and a future real QR callback. */
public final class PaymentService {
    private PaymentService() {}

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

    /** Registers a pending order to the external Payment Gateway (Cloudflare Worker). */
    public static void registerOrderToGateway(String gatewayUrl, String appId, String orderCode,
                                              double amount, String transactionCode, String secret) {
        if (gatewayUrl == null || gatewayUrl.isBlank()) return;
        try {
            String baseUrl = gatewayUrl.trim().replaceAll("/+$", "");
            String url = baseUrl + "/api/orders/register";
            String safeAppId = (appId != null && !appId.isBlank()) ? appId : "default";
            String safeOrderCode = clean(orderCode);
            String safeTx = clean(transactionCode);
            String formattedAmount = (amount == Math.floor(amount))
                    ? String.valueOf((long) amount)
                    : String.format(java.util.Locale.US, "%.2f", amount);
            String signature = sign(secret, safeAppId + "|" + safeOrderCode + "|" + formattedAmount);

            String jsonBody = String.format(java.util.Locale.US,
                    "{\"appId\":\"%s\",\"orderCode\":\"%s\",\"amount\":%s,\"transactionCode\":\"%s\",\"signature\":\"%s\"}",
                    safeAppId, safeOrderCode, formattedAmount, safeTx, signature
            );

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(3))
                    .build();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(4))
                    .header("Content-Type", "application/json")
                    .header("X-Signature", signature)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            // Run in background thread so it never blocks page load
            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> {
                        if (res.statusCode() >= 400) {
                            System.err.println("[PaymentService] Gateway registration returned " + res.statusCode() + ": " + res.body());
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("[PaymentService] Failed to notify gateway: " + ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            System.err.println("[PaymentService] Error initiating gateway registration: " + e.getMessage());
        }
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
