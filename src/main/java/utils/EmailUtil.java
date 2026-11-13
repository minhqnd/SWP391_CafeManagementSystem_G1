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
    private static final String SENDER_EMAIL = System.getenv("AURA_SMTP_EMAIL");
    private static final String SMTP_PASSWORD = System.getenv("AURA_SMTP_PASSWORD");
    private static final String SENDER_PASSWORD = "ozhupnrjrrlvlkii"; // Lấy mật khẩu ứng dụng gồm 16 ký tự từ tài khoản Google (đã xóa dấu cách)

    public static boolean sendNewPassword(String recipientEmail, String newPassword) {
        if (SENDER_EMAIL == null || SENDER_EMAIL.isBlank() || SMTP_PASSWORD == null || SMTP_PASSWORD.isBlank()) return false;
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

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

    public static boolean sendOTP(String recipientEmail, String otp) {
        if (SENDER_EMAIL == null || SENDER_EMAIL.isBlank() || SMTP_PASSWORD == null || SMTP_PASSWORD.isBlank()) return false;
        // OT03: . Ở đây em gửi mã xác nhận đến email được cung cấp.
        // .. Hàm trả kết quả gửi thư thành công hay thất bại.
        // ... Kết quả về ForgotPasswordController [OT02] hoặc ResendOtpController [OT08].
        // BF10: helper gửi email qua SMTP, trả true/false cho ForgotPassword/ResendOtp; không xác minh OTP.
        System.out.println("\n=========================================");
        System.out.println("YÊU CẦU GỬI OTP KHÔI PHỤC MẬT KHẨU");
        System.out.println("Gửi đến Email: " + recipientEmail);
        System.out.println("MÃ OTP CỦA BẠN LÀ: " + otp);
        System.out.println("=========================================\n");

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

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
