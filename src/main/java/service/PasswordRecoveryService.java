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

    private void clear(HttpSession session){session.removeAttribute(KEY);session.removeAttribute("canResetPassword");session.removeAttribute("forgotOtp");session.removeAttribute("forgotEmail");}

    // [OTG03]
    // . Trong start của PasswordRecoveryService, em kiểm tra email và giới hạn gửi trước khi tạo mã xác nhận.
    //   Trước hết em gọi findActiveAccountByEmail trong UserDAO để tìm tài khoản còn hoạt động gắn với email
    //   vừa nhập; có tài khoản phù hợp mới tiếp tục gửi mã.
    //
    // Tra tiếp: UserDAO.java / OTG04.
    public void start(HttpSession session,String rawEmail,String source){
        synchronized(session){
            clear(session);
            String email=AccountValidation.email(rawEmail);
            String error=AccountValidation.emailError(email);
            if(error!=null)throw new utils.RecoveryFailure("email",error);
            long now=clock.getAsLong();reserve(email,source,now);session.setAttribute("lastOtpSentTime",now);

            User user;
            try {
                user=users.findActiveAccountByEmail(email);
            } catch (utils.AccountDataException e) {
                throw new utils.RecoveryFailure(null,"Không kiểm tra được tài khoản lúc này. Chưa gửi OTP. Vui lòng thử lại sau 60 giây.");
            }
            if(user==null)throw new utils.RecoveryFailure("email","Không tìm thấy tài khoản sử dụng email này. Vui lòng nhập đúng email đã liên kết với tài khoản.");

            String code=String.format("%06d",RANDOM.nextInt(1000000));

            boolean sent;try{sent=sender.test(email,code);}catch(RuntimeException e){sent=false;}
            if(!sent)throw new utils.RecoveryFailure(null,"Không gửi được email OTP lúc này. Vui lòng thử lại sau 60 giây.");
            session.setAttribute(KEY,new State(user,code,now));

        }
    }

    // [OTGL03]
    // . Trong resend của PasswordRecoveryService, em kiểm tra đã đủ thời gian chờ rồi gọi lại start để gửi mã
    //   mới cho cùng email. Gửi được thì mã mới được giữ cho lần xác nhận tiếp theo, sau đó kết quả quay về
    //   ResendOtpController để thông báo.
    //
    // Tra tiếp: ResendOtpController.java / OTGL04.
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

    private synchronized void reserve(String email,String source,long now){
        emailSends.entrySet().removeIf(e->now-e.getValue()>=COOLDOWN);
        sourceSends.entrySet().removeIf(e->now-e.getValue().start()>=600000L);
        if(emailSends.containsKey(email))throw new IllegalArgumentException("Vui lòng đợi đủ 60 giây trước khi yêu cầu mã mới.");
        Window window=sourceSends.getOrDefault(source,new Window(now,0));
        if(window.count()>=10 || emailSends.size()>=10000 || sourceSends.size()>=10000)throw new IllegalArgumentException("Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.");
        emailSends.put(email,now);sourceSends.put(source,new Window(window.start(),window.count()+1));
    }

    // [OTXM03]
    // . Trong verify của PasswordRecoveryService, em kiểm tra mã còn hạn, chưa vượt số lần thử và khớp với mã
    //   đã gửi. Đúng mã thì em ghi nhận email đã được xác nhận rồi trả về VerifyOtpController, để controller
    //   mở bước đặt mật khẩu mới.
    //
    // Tra tiếp: VerifyOtpController.java / OTXM04.
    public void verify(HttpSession session,String input){
        synchronized(session){
            State state=state(session);long now=clock.getAsLong();
            if(state.otp==null || state.verifiedAt!=null || now-state.sentAt>=TTL){clear(session);throw expired();}
            if (AccountValidation.text(input).isEmpty())
                throw new utils.RecoveryFailure("otp", "Vui lòng nhập mã OTP.");
            if (!input.matches("[0-9]{6}"))
                throw new utils.RecoveryFailure("otp", "Mã OTP phải gồm đúng 6 chữ số.");

            // [SQL10-17] Nút Xác minh OTP: Java đối chiếu mã và trạng thái khôi phục giữ trong phiên đăng nhập.

            boolean match=input!=null && input.matches("[0-9]{6}") && MessageDigest.isEqual(state.otp.getBytes(StandardCharsets.UTF_8),input.getBytes(StandardCharsets.UTF_8));
            if(!match){state.failures++;if(state.failures>=5){clear(session);throw new IllegalArgumentException("Bạn đã nhập sai 5 lần. Vui lòng yêu cầu mã mới.");}throw new IllegalArgumentException("Mã OTP không chính xác. Còn "+(5-state.failures)+" lần thử.");}
            state.otp=null;state.verifiedAt=now;
        }
    }

    // [OTBUOC01]
    // . Khi khôi phục mật khẩu gặp lỗi, em xác định người dùng còn được tiếp tục ở bước nào. Nếu đang chờ
    //   xác nhận thì mở lại form nhập mã; nếu đã xác nhận thì mở form mật khẩu mới. Khi lần khôi phục hết
    //   hiệu lực, em đưa người dùng về bước nhập email để bắt đầu lại.
    //
    // Tra cứu: Controller đang gọi hàm nextTab nhận tên bước rồi mở lại auth_modal.jsp / ATM03. Kết quả hiển
    //          thị tại auth_modal.jsp / OTG01, auth_modal.jsp / OTXM01 hoặc auth_modal.jsp / OTDM01.
    public String nextTab(HttpSession session) {
        synchronized (session) {
            State state = (State) session.getAttribute(KEY);
            if (state == null) return "forgot";
            long now = clock.getAsLong();
            if (state.verifiedAt != null && now - state.verifiedAt < TTL) return "reset_password";
            if (state.otp != null && now - state.sentAt < TTL) return "otp";
            clear(session);
            return "forgot";
        }
    }

    // [OTDM03]
    // . Trong reset của PasswordRecoveryService, em kiểm tra lần xác nhận còn hiệu lực và hai ô mật khẩu hợp
    //   lệ. Sau đó em gọi resetPasswordById trong UserDAO, truyền tài khoản và email đã xác nhận để ghi mật
    //   khẩu cho đúng người.
    //
    // Tra tiếp: UserDAO.java / OTDM04.
    public void reset(HttpSession session,String password,String confirmation){
        synchronized(session){
            State state=state(session);long now=clock.getAsLong();
            if(state.verifiedAt==null || now-state.verifiedAt>=TTL){clear(session);throw expired();}

            String error=AccountValidation.passwordError(password,confirmation);if(error!=null)throw new IllegalArgumentException(error);
            boolean updated;
            try {
                updated=users.resetPasswordById(state.userId,state.email,password);
            } catch (utils.AccountDataException e) {
                throw new utils.RecoveryFailure(null,"Không lưu được mật khẩu mới lúc này. Vui lòng thử lại trước khi phiên xác minh hết hạn.");
            }

            clear(session);
            if(!updated)throw new IllegalArgumentException("Không thể đổi mật khẩu. Tài khoản có thể đã thay đổi; vui lòng yêu cầu lại.");
        }
    }

    private State state(HttpSession session){State state=(State)session.getAttribute(KEY);if(state==null)throw expired();return state;}
    private IllegalArgumentException expired(){return new IllegalArgumentException("Phiên xác thực đã hết hạn hoặc chưa được xác minh. Vui lòng yêu cầu mã mới.");}
}
