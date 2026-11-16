package utils;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailUtil {

    private static final Properties SMTP_CONFIG = loadSmtpConfig();
    private static final String SENDER_EMAIL = smtpValue("AURA_SMTP_EMAIL", "smtp.email");
    private static final String SMTP_PASSWORD = smtpValue("AURA_SMTP_PASSWORD", "smtp.password");

    // . Đọc cấu hình SMTP riêng trên máy; biến môi trường được ưu tiên, không đưa mật khẩu vào comment.
    private static Properties loadSmtpConfig() {
        Properties config = new Properties();
        String configuredPath = System.getenv("AURA_SMTP_CONFIG");
        java.nio.file.Path path = configuredPath == null || configuredPath.isBlank()
                ? java.nio.file.Path.of(System.getProperty("user.home"), ".auracafe", "smtp.properties")
                : java.nio.file.Path.of(configuredPath);
        if (java.nio.file.Files.isRegularFile(path)) {
            try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(path, java.nio.charset.StandardCharsets.UTF_8)) {
                config.load(reader);
            } catch (java.io.IOException e) {
                System.err.println("Unable to read private SMTP configuration: " + e.getClass().getSimpleName());
            }
        }
        return config;
    }

    private static String smtpValue(String environmentName, String propertyName) {
        String value = System.getenv(environmentName);
        return value == null || value.isBlank() ? SMTP_CONFIG.getProperty(propertyName) : value;
    }

    public static boolean sendNewPassword(String recipientEmail, String newPassword) {
        if (SENDER_EMAIL == null || SENDER_EMAIL.isBlank() || SMTP_PASSWORD == null || SMTP_PASSWORD.isBlank()) return false;
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SMTP_PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "Aura Café System", "UTF-8"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject("Yêu cầu khôi phục mật khẩu - Aura Café", "UTF-8");
            
            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333;'>"
                    + "<h2 style='color: #b2292e;'>Khôi phục mật khẩu Aura Café</h2>"
                    + "<p>Xin chào,</p>"
                    + "<p>Hệ thống đã nhận được yêu cầu cấp lại mật khẩu của bạn.</p>"
                    + "<p>Mật khẩu mới của bạn là: <strong style='font-size: 18px; color: #d9534f;'>" + newPassword + "</strong></p>"
                    + "<p>Vui lòng đăng nhập lại và đổi mật khẩu sớm nhất có thể để đảm bảo an toàn.</p>"
                    + "<br><hr>"
                    + "<p style='font-size: 12px; color: #999;'>Đây là email tự động, vui lòng không phản hồi.</p>"
                    + "</div>";
            
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            Transport.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // OT06e: . Nhận email và mã từ service rồi gửi thư OTP qua SMTP Gmail bằng Jakarta Mail.
    // .. Trả true khi gửi được, false khi thiếu cấu hình hoặc gửi lỗi.
    // ... Kết quả về PasswordRecoveryService.start() [OT06].
    public static boolean sendOTP(String recipientEmail, String otp) {
        if (SENDER_EMAIL == null || SENDER_EMAIL.isBlank() || SMTP_PASSWORD == null || SMTP_PASSWORD.isBlank()) return false;
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SMTP_PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "Aura Café System", "UTF-8"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject("Mã xác thực khôi phục mật khẩu - Aura Café", "UTF-8");
            
            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333;'>"
                    + "<h2 style='color: #b2292e;'>Khôi phục mật khẩu Aura Café</h2>"
                    + "<p>Xin chào,</p>"
                    + "<p>Mã xác thực (OTP) của bạn là: <strong style='font-size: 24px; color: #d9534f;'>" + otp + "</strong></p>"
                    + "<p>Vui lòng nhập mã này trên website để tạo mật khẩu mới.</p>"
                    + "<p style='color: #e74c3c;'>Lưu ý: Tuyệt đối không chia sẻ mã này cho bất kỳ ai.</p>"
                    + "<br><hr>"
                    + "<p style='font-size: 12px; color: #999;'>Đây là email tự động, vui lòng không phản hồi.</p>"
                    + "</div>";
            
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            Transport.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
