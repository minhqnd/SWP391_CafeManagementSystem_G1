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

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
