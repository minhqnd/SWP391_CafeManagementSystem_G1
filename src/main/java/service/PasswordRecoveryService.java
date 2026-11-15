package service;

import dao.UserDAO;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.LongSupplier;
import model.User;
import utils.AccountValidation;
import utils.EmailUtil;

/** Account-bound, single-use recovery; expiry and rate limits are enforced on the server. */
public final class PasswordRecoveryService {
    public static final long TTL = 300000L, COOLDOWN = 60000L;
    private static final String KEY = "passwordRecovery";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final PasswordRecoveryService SHARED = new PasswordRecoveryService(new UserDAO(), EmailUtil::sendOTP, System::currentTimeMillis);
    private final UserDAO users;
    private final BiPredicate<String,String> sender;
    private final LongSupplier clock;
    private final Map<String,Long> emailSends = new HashMap<>();
    private final Map<String,Window> sourceSends = new HashMap<>();
    private record Window(long start, int count) {}
    private static final class State implements Serializable {
        private static final long serialVersionUID=1L;
        final int userId; final String email; final long sentAt;
        String otp; int failures; Long verifiedAt;
        State(User user,String otp,long now){userId=user.getId();email=AccountValidation.email(user.getEmail());this.otp=otp;sentAt=now;}
    }
    public PasswordRecoveryService(UserDAO users,BiPredicate<String,String> sender,LongSupplier clock){this.users=users;this.sender=sender;this.clock=clock;}
    public static PasswordRecoveryService shared(){return SHARED;}
    // OT09: . Xóa trạng thái OTP và quyền khôi phục khi bắt đầu lại, hết hạn, quá lượt hoặc reset xong.
    private void clear(HttpSession session){session.removeAttribute(KEY);session.removeAttribute("canResetPassword");session.removeAttribute("forgotOtp");session.removeAttribute("forgotEmail");}
    // OT10: . Xóa phiên khôi phục cũ, chuẩn hóa email và giới hạn số lần yêu cầu.
    // ... reserve() kiểm tra giới hạn [OT16], sau đó UserDAO.findActiveAccountByEmail() tìm tài khoản [OT11].
    public void start(HttpSession session,String rawEmail,String source){
        synchronized(session){
            clear(session);
            String email=AccountValidation.email(rawEmail);
            String error=AccountValidation.emailError(email);
            if(error!=null)throw new utils.RecoveryFailure("email",error);
            long now=clock.getAsLong();reserve(email,source,now);session.setAttribute("lastOtpSentTime",now);
            // OT11: . Tìm một tài khoản ACTIVE theo email đã chuẩn hóa.
            // .. Có tài khoản thì tiếp tục tạo OTP; không có thì trả lỗi tại trường email.
            User user=users.findActiveAccountByEmail(email);
            if(user==null)throw new utils.RecoveryFailure("email","Không tìm thấy tài khoản sử dụng email này. Vui lòng nhập đúng email đã liên kết với tài khoản.");
            // . Tạo OTP ngẫu nhiên 6 chữ số cho tài khoản tìm được.
            // ... Gửi email và mã qua EmailUtil.sendOTP() [OT03].
            // .. Gửi thành công mới lưu mã, userId, email và thời điểm trong session; chưa đổi mật khẩu.
            String code=String.format("%06d",RANDOM.nextInt(1000000));
            boolean sent;try{sent=sender.test(email,code);}catch(RuntimeException e){sent=false;}
            if(!sent)throw new utils.RecoveryFailure(null,"Không gửi được email OTP. Vui lòng kiểm tra cấu hình SMTP và thử lại sau 60 giây.");
            session.setAttribute(KEY,new State(user,code,now));
        }
    }
    // OT15:
    // . Đọc email từ phiên khôi phục; chưa có phiên hoặc chưa đủ 60 giây thì từ chối.
    // ... [OT10] Đủ thời gian thì gọi start() để tạo và gửi mã mới.
    public void resend(HttpSession session,String source){
        synchronized(session){
            State previous=(State)session.getAttribute(KEY);
            if(previous==null)throw expired();
            if(clock.getAsLong()-previous.sentAt<COOLDOWN){
                if(previous.verifiedAt!=null)clear(session);
                throw new IllegalArgumentException("Vui lòng đợi đủ 60 giây trước khi yêu cầu mã mới.");
            }
            start(session,previous.email,source);
        }
    }
    // OT16:
    // . Giới hạn mỗi email 60 giây, mỗi IP tối đa 10 yêu cầu/10 phút.
    // .. Yêu cầu được tính trước tra DB/gửi mail; tra cứu hoặc gửi thất bại vẫn tính lượt.
    private synchronized void reserve(String email,String source,long now){
        emailSends.entrySet().removeIf(e->now-e.getValue()>=COOLDOWN);
        sourceSends.entrySet().removeIf(e->now-e.getValue().start()>=600000L);
        if(emailSends.containsKey(email))throw new IllegalArgumentException("Vui lòng đợi đủ 60 giây trước khi yêu cầu mã mới.");
        Window window=sourceSends.getOrDefault(source,new Window(now,0));
        if(window.count()>=10 || emailSends.size()>=10000 || sourceSends.size()>=10000)throw new IllegalArgumentException("Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.");
        emailSends.put(email,now);sourceSends.put(source,new Window(window.start(),window.count()+1));
    }
    // OT12: . Kiểm tra mã của phiên chưa dùng, chưa quá 5 phút và khớp mã nhập vào.
    // .. Sai tăng lượt, lần thứ 5 xóa phiên khôi phục; đúng xóa mã và cấp quyền đặt mật khẩu trong 5 phút.
    // ... Trả kết quả về VerifyOtpController.doPost() [OT05].
    public void verify(HttpSession session,String input){
        synchronized(session){
            State state=state(session);long now=clock.getAsLong();
            if(state.otp==null || state.verifiedAt!=null || now-state.sentAt>=TTL){clear(session);throw expired();}
            boolean match=input!=null && input.matches("[0-9]{6}") && MessageDigest.isEqual(state.otp.getBytes(StandardCharsets.UTF_8),input.getBytes(StandardCharsets.UTF_8));
            if(!match){state.failures++;if(state.failures>=5){clear(session);throw new IllegalArgumentException("Bạn đã nhập sai 5 lần. Vui lòng yêu cầu mã mới.");}throw new IllegalArgumentException("Mã OTP không chính xác. Còn "+(5-state.failures)+" lần thử.");}
            state.otp=null;state.verifiedAt=now;
        }
    }
    // OT13:
    // . Yêu cầu đã xác minh OTP còn hạn; kiểm tra mật khẩu mới và xác nhận.
    // ... [OT14] UserDAO.resetPasswordById() lưu mật khẩu theo ID/email đã xác minh.
    // .. Xóa trạng thái khôi phục sau lần gọi DAO; không dùng lại quyền đã cấp.
    public void reset(HttpSession session,String password,String confirmation){
        synchronized(session){
            State state=state(session);long now=clock.getAsLong();
            if(state.verifiedAt==null || now-state.verifiedAt>=TTL){clear(session);throw expired();}
            String error=AccountValidation.passwordError(password,confirmation);if(error!=null)throw new IllegalArgumentException(error);
            boolean updated=users.resetPasswordById(state.userId,state.email,password);clear(session);
            if(!updated)throw new IllegalArgumentException("Không thể đổi mật khẩu. Tài khoản có thể đã thay đổi; vui lòng yêu cầu lại.");
        }
    }
    private State state(HttpSession session){State state=(State)session.getAttribute(KEY);if(state==null)throw expired();return state;}
    private IllegalArgumentException expired(){return new IllegalArgumentException("Phiên xác thực đã hết hạn hoặc chưa được xác minh. Vui lòng yêu cầu mã mới.");}
}
