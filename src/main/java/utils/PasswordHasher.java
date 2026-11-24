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
    // [BMKB01]
    // . Ở hash (tạo chuỗi băm mật khẩu để lưu) trong PasswordHasher.java, em nhận mật khẩu từ các hàm tạo
    //   hoặc đổi tài khoản của UserDAO.java. Em tạo chuỗi băm có phần ngẫu nhiên rồi trả lại hàm đang lưu để
    //   ghi password_hash, thay vì ghi nguyên mật khẩu người dùng nhập.
    // ... Quay lại hàm register, changePasswordById hoặc resetPasswordById trong UserDAO.java, ngay sau
    //     PasswordHasher.hash; chuỗi trả về được gán cho password_hash trước khi thực hiện lệnh ghi.
    public static String hash(String password) {
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        return "pbkdf2_sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }
    // [BMKXM01]
    // . Ở verify (so mật khẩu nhập với giá trị đã lưu) trong PasswordHasher.java, em nhận mật khẩu vừa nhập
    //   và giá trị đang lưu từ UserDAO.login (đối chiếu tài khoản để đăng nhập) hoặc changePasswordById (so
    //   mật khẩu cũ rồi ghi mật khẩu mới). Em đối chiếu rồi trả đúng hoặc sai để hàm gọi quyết định cho đăng
    //   nhập hoặc cho đổi mật khẩu.
    // ... Quay lại UserDAO.java sau PasswordHasher.verify: trong login xét có trả User hay null; trong
    //     changePasswordById xét có cho ghi mật khẩu mới hay báo mật khẩu cũ sai.
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
