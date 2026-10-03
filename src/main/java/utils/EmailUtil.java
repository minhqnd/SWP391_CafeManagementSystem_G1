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

    // TODO: BẠN CẦN THAY ĐỔI EMAIL VÀ MẬT KHẨU ỨNG DỤNG CỦA BẠN VÀO ĐÂY
    private static final String SENDER_EMAIL = "your.email@gmail.com"; 
    private static final String SENDER_PASSWORD = "your_app_password_here"; // Lấy mật khẩu ứng dụng gồm 16 ký tự từ tài khoản Google

    public static boolean sendNewPassword(String recipientEmail, String newPassword) {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "Aura Café System"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject("Yêu cầu khôi phục mật khẩu - Aura Café");
            
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
}
