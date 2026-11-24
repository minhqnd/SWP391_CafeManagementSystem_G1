package controller;

import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import model.User;
import utils.AccountCsrf;
import utils.AccountValidation;

@WebServlet(name="ProfileController",urlPatterns={"/profile"})
public class ProfileController extends HttpServlet {
    private final UserDAO users;
    public ProfileController(){this(new UserDAO());}
    ProfileController(UserDAO users){this.users=users;}

    // [HSM02]
    // . Khi mở hoặc gửi form hồ sơ, doGet và doPost trong ProfileController.java gọi authorize để đọc lại
    //   người đang đăng nhập. Trong đó em gọi UserDAO.getActiveUserById bằng mã tài khoản, lấy thông tin
    //   và điểm mới nhất trước khi dựng trang.
    // ... Đọc tiếp tại UserDAO.java, tìm [HSM03].
    private User authorize(HttpServletRequest request,HttpServletResponse response)throws IOException {
        HttpSession session=request.getSession(false);
        User previous=session==null?null:(User)session.getAttribute("user");
        User current;
        try {
            current=previous==null?null:users.getActiveUserById(previous.getId());
        } catch (utils.AccountDataException e) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Không tải được tài khoản lúc này. Vui lòng thử lại sau.");
            return null;
        }
        if(current==null){
            if(session!=null)session.invalidate();
            response.sendRedirect(request.getContextPath()+"/?tab=login");return null;
        }
        // [HSTV01]
        // . Khi UserDAO.getActiveUserById trả về tài khoản, trong authorize của ProfileController.java em cập
        //   nhật lại session.user. Vì profile.jsp đọc thông tin từ đây nên trang sẽ dùng tên, liên hệ và số
        //   điểm mới nhất của người đang đăng nhập.
        // ... Khi theo luồng mở hồ sơ, quay về doGet sau authorize rồi đọc tiếp tại ProfileController.java,
        //     tìm [HSXEM01].
        // ... Nếu đang gửi form, quay về doPost sau authorize để đọc action rồi gọi update khi lưu hồ sơ, hoặc
        //     xử lý nhánh changePassword khi đổi mật khẩu.

        session.setAttribute("user",current);return current;
    }

    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException {
        User user=authorize(request,response);if(user==null)return;
        AccountCsrf.token(request.getSession());renderProfile(request,response,user);
    }

    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException {
        request.setCharacterEncoding("UTF-8");
        if(!AccountCsrf.require(request,response))return;
        utils.AccountFormFeedback.clear(request);
        User user=authorize(request,response);if(user==null)return;
        String action=request.getParameter("action");
        // . Với action=update, doPost trong ProfileController.java gọi hàm update ngay cùng file. Em truyền
        //   request và tài khoản đang đăng nhập để hàm nhận các ô hồ sơ, kiểm tra rồi lưu đúng người.
        if("update".equals(action))update(request,user);

        // [MKL02]
        // . Đến doPost trong ProfileController.java, em kiểm tra các ô mật khẩu một lần nữa. Đúng thì gọi
        //   changePasswordById trong UserDAO.java, truyền mã tài khoản, mật khẩu hiện tại và mật khẩu mới để
        //   kiểm tra người đổi có nhập đúng mật khẩu cũ không rồi mới ghi mật khẩu mới.
        // ... Đọc tiếp tại UserDAO.java, tìm [MKL03].
        else if("changePassword".equals(action)){
            String password=request.getParameter("newPassword");
            java.util.Map<String,String> errors=AccountValidation.passwordErrors(password,request.getParameter("confirmPassword"),"newPassword","confirmPassword");
            String current=request.getParameter("oldPassword");
            if(current==null || current.isEmpty()) errors.put("oldPassword","Vui lòng nhập mật khẩu hiện tại.");
            else if(current.length()>255) errors.put("oldPassword","Mật khẩu hiện tại tối đa 255 ký tự.");
            // . Khi các ô đổi mật khẩu đều hợp lệ, ở doPost trong ProfileController.java em gọi changePasswordById
            //   trong UserDAO.java. Em truyền mã tài khoản, mật khẩu hiện tại và mật khẩu mới để đối chiếu rồi ghi
            //   nếu đúng mật khẩu cũ.
            boolean passwordSystemError = false;
            try {
                if(errors.isEmpty() && !users.changePasswordById(user.getId(),current,password))
                    errors.put("oldPassword","Mật khẩu hiện tại không đúng hoặc tài khoản không còn hoạt động.");
            } catch (utils.AccountDataException e) {
                passwordSystemError = true;
            }

            // [MKL04]
            // . Kết quả quay về doPost trong ProfileController.java. Đổi được thì em đặt successMsg; ô nhập sai
            //   thì trả lời báo về ô đó. Nếu gặp lỗi hệ thống, em hiện thông báo chung và giữ phần đổi mật khẩu mở
            //   để thử lại. Cuối cùng, renderProfile đưa kết quả về profile.jsp.
            // ... Đọc tiếp tại profile.jsp, tìm [MKL05].
            if(passwordSystemError) {
                utils.AccountFormFeedback.show(request,"profile-password",java.util.Map.of(),java.util.Map.of());
                request.setAttribute("errorMsg","Không đổi được mật khẩu lúc này. Vui lòng thử lại sau.");
            }
            else if(errors.isEmpty()){request.changeSessionId();AccountCsrf.rotate(request.getSession());request.setAttribute("successMsg","Đổi mật khẩu thành công!");}
            else {
                utils.AccountFormFeedback.show(request,"profile-password",errors,java.util.Map.of());
                request.setAttribute("errorMsg","Vui lòng kiểm tra các trường được đánh dấu bên dưới.");
            }
        }else request.setAttribute("errorMsg","Thao tác hồ sơ không hợp lệ.");
        renderProfile(request,response,(User)request.getSession().getAttribute("user"));
    }

    // [HSL02]
    // . Đến doPost trong ProfileController.java, em nhận form Hồ sơ và đọc action=update để biết người
    //   dùng vừa bấm Lưu thay đổi. Em gọi hàm update ngay trong file này, truyền thông tin vừa nhập cùng
    //   tài khoản đang đăng nhập để cập nhật đúng người.
    // ... Đọc tiếp tại ProfileController.java, tìm [HSL03].
    private void update(HttpServletRequest request,User user){

        // [HSHT02]
        // . Trong hàm update của ProfileController.java, em lấy fullName rồi gọi AccountValidation.text để bỏ
        //   khoảng trắng đầu cuối. Em dùng tên đã làm gọn để kiểm tra và lưu, tránh giữ khoảng trắng thừa theo
        //   họ tên.
        // ... Nhận tên xong, update gọi AccountValidation.nameError. Đọc tại AccountValidation.java, tìm mã
        //     KTHT01; kết quả quay về errors trong update.
        String name=AccountValidation.text(request.getParameter("fullName"));

        // [HSDT02]
        // . Trong update, em lấy phone rồi dùng AccountValidation.phone để bỏ các dấu phân cách được hỗ trợ
        //   trước khi kiểm tra số. Nếu form không gửi ô này, em lấy số điện thoại hiện tại của tài khoản để
        //   giữ liên hệ đang lưu.
        // ... Làm gọn số xong, update gọi AccountValidation.phoneError. Đọc tại AccountValidation.java, tìm mã
        //     KTDT01; lỗi được giữ trong errors, số đúng mới được chuẩn bị lưu.
        String phone=AccountValidation.phone(request.getParameter("phone")==null?user.getPhone():request.getParameter("phone"));

        // [HSEM02]
        // . Với email trong update, em gọi AccountValidation.email để bỏ khoảng trắng đầu cuối và chuyển về
        //   chữ thường. Em dùng cùng dạng địa chỉ này cho bước kiểm tra định dạng, tìm trùng và lưu hồ sơ.
        // ... Làm gọn email xong, update gọi AccountValidation.emailError. Đọc tại AccountValidation.java,
        //     tìm mã KTEM01; kết quả quay về errors trước khi ghi hồ sơ.
        String email=AccountValidation.email(request.getParameter("email")==null?user.getEmail():request.getParameter("email"));
        // [HSNS04]
        // . Trong hàm update của ProfileController.java, em nhận ngày, tháng và năm từ ba ô của profile.jsp
        //   rồi ghép thành ngày sinh. Em đưa ngày này cho AccountValidation.birthdayError để kiểm tra ngày có
        //   thật và nằm trong khoảng cho phép trước khi lưu.
        // ... Ghép ba ô xong, update gọi AccountValidation.birthdayError. Đọc phần kiểm tra ngày sinh tại
        //     AccountValidation.java, tìm mã KTNS01.
        String birthday=AccountValidation.text(request.getParameter("birthday"));

        String day=request.getParameter("birthDay");
        String month=request.getParameter("birthMonth");
        String year=request.getParameter("birthYear");
        boolean hasBirthdayParts=day!=null || month!=null || year!=null;
        int birthdayParts=(AccountValidation.text(day).isEmpty()?0:1)
                +(AccountValidation.text(month).isEmpty()?0:1)
                +(AccountValidation.text(year).isEmpty()?0:1);
        if(hasBirthdayParts && birthdayParts==3){
            try{
                birthday=java.time.LocalDate.of(Integer.parseInt(year),Integer.parseInt(month),Integer.parseInt(day)).toString();
            }catch(java.time.DateTimeException | NumberFormatException e){birthday="invalid";}
        }else if(hasBirthdayParts && birthdayParts==0){birthday="";}

        // [HSDC03]
        // . Trong hàm update của ProfileController.java, em nhận address rồi gọi AccountValidation.text để bỏ
        //   khoảng trắng đầu cuối. Em kiểm tra ký tự và độ dài trước khi truyền địa chỉ cùng các thông tin
        //   khác cho UserDAO.updateUserProfile để lưu.
        // ... update gọi AccountValidation.addressError. Đọc tại AccountValidation.java, tìm mã KTDC01; sau đó
        //     update còn kiểm tra độ dài và xét errors.
        String address=AccountValidation.text(request.getParameter("address"));
        java.util.Map<String,String> errors=new java.util.LinkedHashMap<>();
        // [HSL03]
        // . Trong update, em gọi các hàm của AccountValidation để kiểm tra từng ô hồ sơ. Ô nào sai thì em giữ
        //   lời báo theo đúng tên ô đó trong errors để hiện lại cho người dùng sửa. Chỉ khi tất cả các ô đều
        //   đúng, em mới chuyển sang phần ghi hồ sơ.
        // ... Còn errors thì update mở lại hồ sơ và đưa lỗi về từng ô; xem cách nhận tại account_feedback.jsp,
        //     tìm mã BMTV02. Nếu không còn lỗi, đọc điểm gọi lưu tại ProfileController.java, tìm [HSL04].
        if(AccountValidation.nameError(name)!=null)errors.put("fullName",AccountValidation.nameError(name));
        if(AccountValidation.phoneError(phone)!=null)errors.put("phone",AccountValidation.phoneError(phone));
        if(AccountValidation.emailError(email)!=null)errors.put("email",AccountValidation.emailError(email));
        if(hasBirthdayParts && birthdayParts!=0 && birthdayParts!=3)
            errors.put("birthDay","Vui lòng chọn đủ ngày, tháng và năm sinh hoặc để trống cả ba.");
        // . Để kiểm tra ngày sinh trước khi lưu hồ sơ, trong ProfileController.update em gọi birthdayError của
        //   AccountValidation.java bằng ngày vừa ghép. Em nhận lời báo nếu ngày không có thật hoặc ngoài
        //   khoảng cho phép.
        // ... Đọc tiếp tại AccountValidation.java, tìm [KTNS01].
        else if(AccountValidation.birthdayError(birthday)!=null)errors.put("birthDay",AccountValidation.birthdayError(birthday));
        // . Để kiểm tra địa chỉ, trong ProfileController.update em truyền address vào addressError của
        //   AccountValidation.java. Em giữ lời báo vào errors.address nếu có ký tự không hợp lệ, rồi kiểm tra
        //   thêm độ dài ở đoạn tiếp theo.
        // ... Đọc tiếp tại AccountValidation.java, tìm [KTDC01].
        if(AccountValidation.addressError(address)!=null)errors.put("address",AccountValidation.addressError(address));
        if(address.length()>500)errors.put("address","Địa chỉ tối đa 500 ký tự.");
        if(errors.isEmpty()){

            // [HSL04]
            // . Các ô đã đúng thì em gọi updateUserProfile trong UserDAO.java. Em truyền mã người đang đăng nhập
            //   cùng họ tên, số điện thoại, email, ngày sinh và địa chỉ để hàm ghi đúng dòng tài khoản đó trong
            //   database, rồi trả kết quả về đây.
            // ... Đọc tiếp tại UserDAO.java, tìm [HSL05].
            String result=users.updateUserProfile(user.getId(),name,phone,email,birthday,address);
            if(!"success".equals(result)){
                if("Email đã được sử dụng bởi tài khoản khác!".equals(result))errors.put("email",result);
                if("Số điện thoại đã được sử dụng bởi tài khoản khác!".equals(result))errors.put("phone",result);
                if(errors.isEmpty()){
                    utils.AccountFormFeedback.show(request,"profile-update",java.util.Map.of(),profileValues(request,name,phone,email,address));
                    request.setAttribute("errorMsg",result);return;
                }
            }
        }

        // . Nếu còn lỗi trong ProfileController.update, em gọi AccountFormFeedback.show bằng lỗi và các ô đang
        //   nhập. Sau đó renderProfile mở lại profile.jsp để người dùng thấy lỗi đúng ô và giữ được thông tin
        //   cần sửa.
        if(!errors.isEmpty()){
            java.util.Map<String,String> values=new java.util.LinkedHashMap<>(java.util.Map.of("fullName",name,"phone",phone,"email",email,"address",address));
            for(String part:new String[]{"birthDay","birthMonth","birthYear"})
                if(request.getParameter(part)!=null)values.put(part,request.getParameter(part));
            utils.AccountFormFeedback.show(request,"profile-update",errors,values);
            request.setAttribute("errorMsg","Vui lòng kiểm tra các trường được đánh dấu bên dưới.");return;
        }

        // [HSL06]
        // . Khi nhận success từ UserDAO, em quay lại update trong ProfileController.java để cập nhật thông tin
        //   đang giữ trong session.user và đặt thông báo thành công. Sau đó, doPost gọi renderProfile để lấy
        //   dữ liệu cho trang hồ sơ và hiện thông tin mới.
        // ... Đọc tiếp tại profile.jsp, tìm [HSL07].
        user.setFullName(name);user.setPhone(phone);user.setEmail(email);user.setBirthday(birthday);user.setAddress(address);
        request.getSession().setAttribute("user",user);request.setAttribute("successMsg","Cập nhật thông tin thành công!");
    }

    // . Khi cập nhật hồ sơ chưa thành công, em gom các ô được phép giữ vào profileValues để
    //   AccountFormFeedback.show điền lại đúng nội dung đang sửa. Hàm này không lưu dữ liệu vào tài khoản.
    private java.util.Map<String,String> profileValues(HttpServletRequest request,String name,String phone,String email,String address) {
        java.util.Map<String,String> values=new java.util.LinkedHashMap<>(java.util.Map.of("fullName",name,"phone",phone,"email",email,"address",address));
        for(String part:new String[]{"birthDay","birthMonth","birthYear"})
            if(request.getParameter(part)!=null)values.put(part,request.getParameter(part));
        return values;
    }

    // [HSXEM01]
    // . Từ doGet hoặc doPost trong ProfileController.java, em gọi renderProfile để chuẩn bị trang hồ sơ.
    //   Em truyền tài khoản hiện tại, gọi UserDAO.getLoyaltyHistoryByUserId để đọc lịch sử của người đó và
    //   ShopSettingDAO.getSettings để đọc quy tắc, rồi đưa dữ liệu về profile.jsp. Nếu lịch sử điểm chưa
    //   tải được, em đưa loyaltyHistoryError sang trang để báo rõ phần đang lỗi; thông tin hồ sơ vẫn được
    //   hiển thị.
    // ... Sau khi renderProfile đặt dữ liệu và forward, đọc tiếp tại profile.jsp, tìm [HSM04] ở đầu phần
    //     hồ sơ để thấy điểm kết thúc hiển thị.
    protected void renderProfile(HttpServletRequest request,HttpServletResponse response,User user)throws ServletException,IOException{
        try {
            request.setAttribute("loyaltyHistory",users.getLoyaltyHistoryByUserId(user.getId()));
        } catch (utils.AccountDataException e) {
            request.setAttribute("loyaltyHistoryError","Không tải được lịch sử điểm lúc này. Vui lòng thử lại sau.");
        }
        request.setAttribute("shopSettings",new dao.ShopSettingDAO().getSettings());
        request.getRequestDispatcher("/profile.jsp").forward(request,response);
    }
}
