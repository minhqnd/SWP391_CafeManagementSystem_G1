package utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final int ITERATIONS = 210000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private PasswordHasher() {}
    public static boolean encoded(String stored) { return stored != null && stored.startsWith("pbkdf2_sha256$"); }
    // . Băm mật khẩu bằng PBKDF2 và salt ngẫu nhiên trước khi DAO lưu.
    // .. Trả chuỗi chứa thuật toán, số vòng, salt và hash; không trả mật khẩu rõ.
    public static String hash(String password) {
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        return "pbkdf2_sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }
    // . Đối chiếu mật khẩu nhập với dữ liệu đã lưu; hỗ trợ dữ liệu mật khẩu rõ cũ.
    // .. Trả true/false cho DAO; không tạo session hoặc tự cho phép đăng nhập.
    public static boolean verify(String password, String stored) {
        if (password == null || password.isBlank() || password.length() > 255 || stored == null || stored.matches("[A-Za-z]+_hash")) return false;
        if (!encoded(stored)) {
            return MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
        }
        try {
            String[] parts = stored.split("\\$", -1);
            if (parts.length != 4) return false;
            int rounds = Integer.parseInt(parts[1]);
            if (rounds < 100000 || rounds > 1000000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return salt.length == 16 && expected.length == 32 && MessageDigest.isEqual(expected, derive(password, salt, rounds));
        } catch (IllegalArgumentException e) { return false; }
    }
    private static byte[] derive(String password, byte[] salt, int rounds) {
        PBEKeySpec key = new PBEKeySpec(password.toCharArray(), salt, rounds, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(key).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("Không khởi tạo được bộ mã hóa mật khẩu.", e); }
        finally { key.clearPassword(); }
    }
}
