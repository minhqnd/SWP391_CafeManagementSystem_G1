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
    // OT06f: . Xóa trạng thái OTP và quyền khôi phục trong session.
    // .. Dùng khi bắt đầu lại, hết hạn, sai quá lượt hoặc đã gọi lưu mật khẩu mới.
    private void clear(HttpSession session){session.removeAttribute(KEY);session.removeAttribute("canResetPassword");session.removeAttribute("forgotOtp");session.removeAttribute("forgotEmail");}
    // OT06: . Xóa phiên khôi phục cũ, chuẩn hóa email, giới hạn yêu cầu rồi tìm tài khoản/gửi OTP.
    // .. Gửi mail thành công mới lưu OTP, userId, email và thời điểm trong session.
    // ... reserve() [OT06a] → tìm tài khoản [OT06b] → gửi mail [OT06d]; Controller mở form [OT07].
    public void start(HttpSession session,String rawEmail,String source){
        synchronized(session){
            clear(session);
            String email=AccountValidation.email(rawEmail);
            String error=AccountValidation.emailError(email);
            if(error!=null)throw new utils.RecoveryFailure("email",error);
            long now=clock.getAsLong();reserve(email,source,now);session.setAttribute("lastOtpSentTime",now);
            // OT06b: . Tìm tài khoản theo email đã chuẩn hóa.
            // .. Không tìm thấy thì báo lỗi email; trùng dữ liệu cũ/không ACTIVE thì DAO trả lỗi riêng.
            // ... UserDAO.findActiveAccountByEmail() trong UserDAO.java [OT06c].
            User user=users.findActiveAccountByEmail(email);
            if(user==null)throw new utils.RecoveryFailure("email","Không tìm thấy tài khoản sử dụng email này. Vui lòng nhập đúng email đã liên kết với tài khoản.");
            // OT06d: . Sinh OTP ngẫu nhiên 6 chữ số và gửi tới email đã tra cứu.
            // .. Gửi lỗi thì chưa lưu State khôi phục; gửi được mới lưu mã và thời điểm.
            // ... sender dùng EmailUtil.sendOTP() trong EmailUtil.java [OT06e].
            String code=String.format("%06d",RANDOM.nextInt(1000000));
            boolean sent;try{sent=sender.test(email,code);}catch(RuntimeException e){sent=false;}
            if(!sent)throw new utils.RecoveryFailure(null,"Không gửi được email OTP. Vui lòng kiểm tra cấu hình SMTP và thử lại sau 60 giây.");
            session.setAttribute(KEY,new State(user,code,now));
        }
    }
    // OT07d: . Lấy email từ phiên khôi phục và kiểm tra đã qua 60 giây chưa.
    // .. Chưa có phiên hoặc chưa đủ thời gian thì từ chối; đủ thời gian mới tạo mã mới.
    // ... start() [OT06] gửi lại và thay trạng thái khôi phục cũ.
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
    // OT06a: . Giới hạn mỗi email một yêu cầu/60 giây và mỗi IP tối đa 10 yêu cầu/10 phút.
    // .. Tính lượt trước khi tra cứu/gửi mail; tra cứu hay gửi mail thất bại vẫn tính lượt.
    private synchronized void reserve(String email,String source,long now){
        emailSends.entrySet().removeIf(e->now-e.getValue()>=COOLDOWN);
        sourceSends.entrySet().removeIf(e->now-e.getValue().start()>=600000L);
        if(emailSends.containsKey(email))throw new IllegalArgumentException("Vui lòng đợi đủ 60 giây trước khi yêu cầu mã mới.");
        Window window=sourceSends.getOrDefault(source,new Window(now,0));
        if(window.count()>=10 || emailSends.size()>=10000 || sourceSends.size()>=10000)throw new IllegalArgumentException("Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.");
        emailSends.put(email,now);sourceSends.put(source,new Window(window.start(),window.count()+1));
    }
    // OT11: . Kiểm tra OTP chưa dùng, chưa quá 5 phút và khớp mã 6 chữ số.
    // .. Sai tăng lượt, sai lần thứ 5 xóa phiên; đúng xóa mã và cấp quyền đặt mật khẩu trong 5 phút.
    // ... VerifyOtpController nhận kết quả và mở form đặt mật khẩu [OT12].
    public void verify(HttpSession session,String input){
        synchronized(session){
            State state=state(session);long now=clock.getAsLong();
            if(state.otp==null || state.verifiedAt!=null || now-state.sentAt>=TTL){clear(session);throw expired();}
            boolean match=input!=null && input.matches("[0-9]{6}") && MessageDigest.isEqual(state.otp.getBytes(StandardCharsets.UTF_8),input.getBytes(StandardCharsets.UTF_8));
            if(!match){state.failures++;if(state.failures>=5){clear(session);throw new IllegalArgumentException("Bạn đã nhập sai 5 lần. Vui lòng yêu cầu mã mới.");}throw new IllegalArgumentException("Mã OTP không chính xác. Còn "+(5-state.failures)+" lần thử.");}
            state.otp=null;state.verifiedAt=now;
        }
    }
    // OT16: . Kiểm tra quyền khôi phục đã xác minh còn hạn, kiểm tra mật khẩu mới và xác nhận.
    // .. Mật khẩu sai thì chưa gọi DAO; gọi DAO xong sẽ xóa quyền khôi phục.
    // ... UserDAO.resetPasswordById() [OT17] → ResetPasswordController xử lý thành công [OT18].
    public void reset(HttpSession session,String password,String confirmation){
        synchronized(session){
            State state=state(session);long now=clock.getAsLong();
            if(state.verifiedAt==null || now-state.verifiedAt>=TTL){clear(session);throw expired();}
            String error=AccountValidation.passwordError(password,confirmation);if(error!=null)throw new IllegalArgumentException(error);
            boolean updated=users.resetPasswordById(state.userId,state.email,password);clear(session);
            if(!updated)throw new IllegalArgumentException("Không thể đổi mật khẩu. Tài khoản có thể đã thay đổi; vui lòng yêu cầu lại.");
        }
    }
    // OT06g: . Đọc State khôi phục trong session.
    // .. Không có State thì trả lỗi phiên hết hạn/chưa được xác minh cho Controller.
    private State state(HttpSession session){State state=(State)session.getAttribute(KEY);if(state==null)throw expired();return state;}
    private IllegalArgumentException expired(){return new IllegalArgumentException("Phiên xác thực đã hết hạn hoặc chưa được xác minh. Vui lòng yêu cầu mã mới.");}
}
