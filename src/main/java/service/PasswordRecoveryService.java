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
    // [SUA10-01] Luyện sửa: OTP từ 5 phút thành 2 phút; chờ gửi lại từ 60 giây thành 30 giây.
    // . TTL/COOLDOWN tính bằng mili giây: 2 phút = 120000L; 30 giây = 30000L.
    // . Đổi COOLDOWN còn phải sửa số 60000 ở auth_modal.jsp và login.jsp, cùng lời báo 60 giây trong file này.
    // . TTL cũng giới hạn thời gian đặt lại mật khẩu sau xác minh; cập nhật lời nhắc 5 phút trong
    //   auth_modal.jsp và các comment mô tả thời hạn.
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
    // . Trong start của PasswordRecoveryService.java, em kiểm tra email và giới hạn gửi, rồi gọi
    //   UserDAO.findActiveAccountByEmail để tìm tài khoản phù hợp. Tìm được thì tạo mã sáu chữ số và gọi
    //   EmailUtil.sendOTP để gửi; gửi được mới giữ thông tin cho bước xác minh.
    // .. Mã dùng trong 5 phút, mỗi lần gửi cách nhau ít nhất 60 giây. Em chỉ giữ mã sau khi gửi thư thành
    //    công.
    // ... Đọc tiếp tại UserDAO.java, tìm [OTG04].
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
            // . Trong PasswordRecoveryService.start (tạo và gửi mã OTP), em gọi EmailUtil.sendOTP (gửi email chứa mã
            //   xác minh) qua sender, truyền email và mã sáu số để gửi thư xác minh. Em nhận sent để chỉ giữ mã cho
            //   bước xác minh khi thư đã gửi được.
            // ... Đọc tiếp tại EmailUtil.java, tìm [OTG05].
            boolean sent;try{sent=sender.test(email,code);}catch(RuntimeException e){sent=false;}
            if(!sent)throw new utils.RecoveryFailure(null,"Không gửi được email OTP lúc này. Vui lòng thử lại sau 60 giây.");
            session.setAttribute(KEY,new State(user,code,now));
            // . Khi tạo và gửi mã xong, start (tạo và gửi mã OTP) trong PasswordRecoveryService.java quay về
            //   ForgotPasswordController.doPost để mở ô OTP. Nếu được gọi từ resend (kiểm tra thời gian chờ rồi gửi
            //   lại OTP) thì kết quả quay về ResendOtpController.doPost để báo đã gửi lại mã.
            // ... Đọc tiếp tại ForgotPasswordController.java, tìm [OTG06].
        }
    }

    // [OTGL03]
    // . Trong resend của PasswordRecoveryService.java, em đọc lần gửi trước và kiểm tra thời gian chờ. Nếu
    //   được gửi lại, em gọi start cùng file bằng email đã giữ để tạo mã mới; gửi xong thì quay về
    //   ResendOtpController.doPost.
    // ... Đọc tiếp tại ResendOtpController.java, tìm [OTGL04].
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

    // [OTGH01]
    // . Trong start (tạo và gửi mã OTP) của PasswordRecoveryService.java, em gọi reserve (kiểm tra giới
    //   hạn số lần gửi OTP) cùng file để kiểm tra giới hạn gửi mã. Em truyền email, nguồn yêu cầu và
    //   thời gian hiện tại; nếu gửi quá nhiều thì hàm báo phải chờ, còn trong giới hạn thì tiếp tục gửi
    //   OTP.
    // ... Quay về PasswordRecoveryService.start sau reserve. Vượt giới hạn thì dừng và báo lỗi; được
    //     phép thì tìm tài khoản tại UserDAO.java, tìm mã OTG04, rồi mới tạo mã và gửi thư.
    private synchronized void reserve(String email,String source,long now){
        emailSends.entrySet().removeIf(e->now-e.getValue()>=COOLDOWN);
        sourceSends.entrySet().removeIf(e->now-e.getValue().start()>=600000L);
        if(emailSends.containsKey(email))throw new IllegalArgumentException("Vui lòng đợi đủ 60 giây trước khi yêu cầu mã mới.");
        Window window=sourceSends.getOrDefault(source,new Window(now,0));
        if(window.count()>=10 || emailSends.size()>=10000 || sourceSends.size()>=10000)throw new IllegalArgumentException("Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.");
        emailSends.put(email,now);sourceSends.put(source,new Window(window.start(),window.count()+1));
    }

    // [OTXM03]
    // . Trong verify của PasswordRecoveryService.java, em đọc dữ liệu lần gửi rồi kiểm tra OTP, thời hạn và
    //   số lần sai. Mã đúng thì ghi nhận thời điểm đã xác minh để controller mở bước mật khẩu mới; mã sai
    //   hoặc hết hạn thì báo lỗi và chưa cho đổi mật khẩu.
    // .. Mã hết hạn sau 5 phút. Nội dung không đủ sáu chữ số bị báo sai định dạng và chưa tính là một lần thử
    //    mã. Đủ năm lần nhập mã sai thì hủy lần xác minh; mã đúng mới cho phép đặt lại mật khẩu.
    // ... Đọc tiếp tại VerifyOtpController.java, tìm [OTXM04].
    public void verify(HttpSession session,String input){
        synchronized(session){
            State state=state(session);long now=clock.getAsLong();
            if(state.otp==null || state.verifiedAt!=null || now-state.sentAt>=TTL){clear(session);throw expired();}
            if (AccountValidation.text(input).isEmpty())
                throw new utils.RecoveryFailure("otp", "Vui lòng nhập mã OTP.");
            if (!input.matches("[0-9]{6}"))
                throw new utils.RecoveryFailure("otp", "Mã OTP phải gồm đúng 6 chữ số.");
            // [OTN03]
            // . Ở verify (xác minh mã OTP) trong PasswordRecoveryService.java, em so sáu chữ số vừa nhập với OTP
            //   đang giữ của lần yêu cầu này. Sai thì tăng số lần thử và báo lỗi; đủ năm lần sai thì hủy lần xác
            //   minh. Đúng thì bỏ OTP, giữ thời điểm xác minh để controller mở bước nhập mật khẩu mới.
            // ... verify trả về VerifyOtpController.doPost: đúng thì mở bước đặt mật khẩu, sai thì catch hiện
            //     lỗi. Đọc điểm nhận kết quả tại VerifyOtpController.java, tìm mã OTXM04; nhánh lỗi nằm trong
            //     catch bên dưới.

            // [SQL10-17] Nút Xác minh OTP: Java đối chiếu mã và trạng thái khôi phục giữ trong phiên đăng nhập.
            // . Không có câu SELECT OTP trong MySQL; hạn dùng và số lần nhập sai cũng được kiểm tra trong dịch
            //   vụ này.
            boolean match=input!=null && input.matches("[0-9]{6}") && MessageDigest.isEqual(state.otp.getBytes(StandardCharsets.UTF_8),input.getBytes(StandardCharsets.UTF_8));
            // [SUA10-02] Luyện sửa: Chỉ cho nhập sai OTP 3 lần thay vì 5 lần.
            // . Sửa điều kiện state.failures>=5, phép tính 5-state.failures và lời báo số lần trong cùng nhánh.
            // . Thử sai liên tiếp đến giới hạn mới để kiểm tra mã bị hủy và phải yêu cầu gửi lại.
            if(!match){state.failures++;if(state.failures>=5){clear(session);throw new IllegalArgumentException("Bạn đã nhập sai 5 lần. Vui lòng yêu cầu mã mới.");}throw new IllegalArgumentException("Mã OTP không chính xác. Còn "+(5-state.failures)+" lần thử.");}
            state.otp=null;state.verifiedAt=now;
        }
    }

    // [OTBUOC01]
    // . Khi bước xác minh hoặc đặt lại mật khẩu gặp lỗi, controller gọi nextTab trong file này để chọn
    //   màn hình có thể tiếp tục. Mã còn hạn thì về ô OTP, đã xác minh còn hạn thì về mật khẩu mới; hết hạn
    //   hoặc không còn dữ liệu xác minh thì quay về nhập email để yêu cầu mã mới.
    // ... Quay lại nhánh catch trong VerifyOtpController.java, ResetPasswordController.java hoặc
    //     ResendOtpController.java sau service.nextTab; controller ghép tab vào đường dẫn và mở lại form tương
    //     ứng.
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
    // . Trong reset của PasswordRecoveryService.java, em kiểm tra lần xác minh còn hiệu lực và hai ô mật
    //   khẩu hợp lệ. Sau đó em gọi UserDAO.resetPasswordById bằng mã tài khoản, email đã xác minh và mật
    //   khẩu mới. Ghi được hoặc tài khoản không còn khớp thì xóa dữ liệu xác minh rồi quay về controller;
    //   riêng lỗi kết nối khi ghi sẽ giữ lần xác minh còn hạn để người dùng thử lại.
    // .. Lần xác minh phải còn trong thời hạn 5 phút trước khi được đặt lại mật khẩu.
    // ... Đọc tiếp tại UserDAO.java, tìm [OTDM04].
    public void reset(HttpSession session,String password,String confirmation){
        synchronized(session){
            State state=state(session);long now=clock.getAsLong();
            if(state.verifiedAt==null || now-state.verifiedAt>=TTL){clear(session);throw expired();}
            // [OTMK02]
            // . Sau khi kiểm tra lần xác minh còn hiệu lực, trong reset (đặt lại mật khẩu sau khi xác minh OTP)
            //   của PasswordRecoveryService.java em gọi AccountValidation.passwordError (kiểm tra điều kiện mật
            //   khẩu) bằng mật khẩu mới và xác nhận. Hàm kiểm tra 8–255 ký tự, có chữ và số, không có ký tự điều
            //   khiển, hai ô phải khớp. Có lỗi thì dừng trước khi gọi users.resetPasswordById (ghi mật khẩu mới
            //   cho tài khoản đã xác minh) để ghi mật khẩu mới.
            // ... reset nhận kết quả passwordError; có lỗi thì dừng, đúng mới gọi UserDAO.resetPasswordById. Đọc
            //     bước ghi mật khẩu tại UserDAO.java, tìm mã OTDM04.
            String error=AccountValidation.passwordError(password,confirmation);if(error!=null)throw new IllegalArgumentException(error);
            boolean updated;
            try {
                updated=users.resetPasswordById(state.userId,state.email,password);
            } catch (utils.AccountDataException e) {
                throw new utils.RecoveryFailure(null,"Không lưu được mật khẩu mới lúc này. Vui lòng thử lại trước khi phiên xác minh hết hạn.");
            }
            // . Sau khi UserDAO trả kết quả, em xóa dữ liệu xác minh; chưa đổi được thì báo lỗi,
            //   đổi được thì reset trả về controller đã gọi để mở lại form Đăng nhập.
            // ... Khi đổi thành công, đọc tiếp tại ResetPasswordController.java, tìm [OTDM05].
            clear(session);
            if(!updated)throw new IllegalArgumentException("Không thể đổi mật khẩu. Tài khoản có thể đã thay đổi; vui lòng yêu cầu lại.");
        }
    }

    private State state(HttpSession session){State state=(State)session.getAttribute(KEY);if(state==null)throw expired();return state;}
    private IllegalArgumentException expired(){return new IllegalArgumentException("Phiên xác thực đã hết hạn hoặc chưa được xác minh. Vui lòng yêu cầu mã mới.");}
}
