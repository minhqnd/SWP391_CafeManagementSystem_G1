package controller;

import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Objects;
import model.User;
import utils.AccountCsrf;
import utils.AccountValidation;

@WebServlet(name="ProfileController",urlPatterns={"/profile"})
public class ProfileController extends HttpServlet {
    private final UserDAO users;
    public ProfileController(){this(new UserDAO());}
    ProfileController(UserDAO users){this.users=users;}

    // PF03: . Lấy user trong session rồi đọc lại tài khoản ACTIVE theo ID.
    // .. Không hợp lệ: hủy session và chuyển về đăng nhập; hợp lệ: cập nhật User mới trong session.
    // ... UserDAO.getActiveUserById() trong UserDAO.java [PF04] trả thông tin tài khoản.
    private User authorize(HttpServletRequest request,HttpServletResponse response)throws IOException {
        HttpSession session=request.getSession(false);
        User previous=session==null?null:(User)session.getAttribute("user");
        User current=previous==null?null:users.getActiveUserById(previous.getId());
        if(current==null){
            if(session!=null)session.invalidate();
            response.sendRedirect(request.getContextPath()+"/?tab=login");return null;
        }
        session.setAttribute("user",current);return current;
    }
    // PF02: . Nhận GET /profile từ mục Hồ sơ & Điểm tích luỹ trong header.jsp [PF01].
    // .. Phiên hợp lệ mới được xem hồ sơ; tạo token cho các form trên trang.
    // ... authorize() [PF03] → renderProfile() [PF05] → profile.jsp [PF06].
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException {
        User user=authorize(request,response);if(user==null)return;
        AccountCsrf.token(request.getSession());renderProfile(request,response,user);
    }
    // PF11: . Nhận POST /profile?action=update từ JavaScript [PF10], kiểm tra CSRF và tài khoản ACTIVE.
    // ... action=update gọi update() [PF12]; xong trả trang qua [PF16].
    // PW04: . Nhận POST /profile?action=changePassword từ JavaScript [PW03], kiểm tra cùng CSRF/session.
    // ... action=changePassword vào nhánh đổi mật khẩu [PW05]; xong trả trang qua [PW08].
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException {
        request.setCharacterEncoding("UTF-8");
        if(!AccountCsrf.require(request,response))return;
        User user=authorize(request,response);if(user==null)return;
        String action=request.getParameter("action");
        if("update".equals(action))update(request,user);
        // PW05: . Đọc oldPassword, newPassword và confirmPassword; kiểm tra mật khẩu mới/xác nhận.
        // .. Còn lỗi thì chưa gọi DAO; đủ dữ liệu mới xác minh mật khẩu cũ và lưu mật khẩu mới.
        // ... AccountValidation.passwordErrors() → UserDAO.changePasswordById() [PW06].
        else if("changePassword".equals(action)){
            String password=request.getParameter("newPassword");
            java.util.Map<String,String> errors=AccountValidation.passwordErrors(password,request.getParameter("confirmPassword"),"newPassword","confirmPassword");
            String current=request.getParameter("oldPassword");
            if(current==null || current.isEmpty()) errors.put("oldPassword","Vui lòng nhập mật khẩu hiện tại.");
            if(errors.isEmpty() && !users.changePasswordById(user.getId(),current,password))
                errors.put("oldPassword","Mật khẩu hiện tại không đúng hoặc tài khoản không còn hoạt động.");
            // PW07: . Xử lý kết quả đổi mật khẩu từ [PW06].
            // .. Thành công đổi session ID, xoay CSRF và đặt successMsg; lỗi thì gắn theo từng ô.
            // ... renderProfile() [PW08] trả trang → thông báo kết quả [PW09].
            if(errors.isEmpty()){request.changeSessionId();AccountCsrf.rotate(request.getSession());request.setAttribute("successMsg","Đổi mật khẩu thành công!");}
            else {
                utils.AccountFormFeedback.show(request,"profile-password",errors,java.util.Map.of());
                request.setAttribute("errorMsg","Vui lòng kiểm tra các trường được đánh dấu bên dưới.");
            }
        }else request.setAttribute("errorMsg","Thao tác hồ sơ không hợp lệ.");
        renderProfile(request,response,(User)request.getSession().getAttribute("user"));
    }
    // PF12: . Đọc, chuẩn hóa và kiểm tra dữ liệu hồ sơ khi action=update.
    // .. Kiểm tra từng trường, ngày sinh và quyền đổi email; còn lỗi thì không lưu.
    // ... Các trường chi tiết [PF12a]–[PF12f]; hết lỗi gọi UserDAO.updateUserProfile() [PF13].
    private void update(HttpServletRequest request,User user){
        // PF12a: . Đọc fullName từ ô Họ và tên [PF06a], bỏ khoảng trắng đầu/cuối.
        // ... AccountValidation.nameError() kiểm tra giới hạn họ tên.
        String name=AccountValidation.text(request.getParameter("fullName"));
        // PF12b: . Đọc phone từ ô SĐT [PF06b]; thiếu tham số thì dùng SĐT hiện tại.
        // ... AccountValidation.phone() chuẩn hóa; AccountValidation.phoneError() kiểm tra định dạng.
        String phone=AccountValidation.phone(request.getParameter("phone")==null?user.getPhone():request.getParameter("phone"));
        // PF12c: . Đọc email từ ô Email [PF06c]; thiếu tham số thì dùng email hiện tại.
        // ... AccountValidation.email() chuẩn hóa; AccountValidation.emailError() kiểm tra định dạng.
        String email=AccountValidation.email(request.getParameter("email")==null?user.getEmail():request.getParameter("email"));
        String birthday=AccountValidation.text(request.getParameter("birthday"));
        // PF12d: . Đọc birthDay, birthMonth, birthYear từ ba ô ngày sinh [PF06d].
        // .. Đủ ba phần thì ghép thành ngày; cả ba trống thì bỏ ngày sinh; thiếu phần thì báo lỗi.
        // ... AccountValidation.birthdayError() kiểm tra ngày đã ghép.
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
        // PF12e: . Đọc address từ ô địa chỉ [PF06e] và bỏ khoảng trắng đầu/cuối.
        // .. Cho phép bỏ trống; dài quá 500 ký tự thì thêm lỗi address.
        String address=AccountValidation.text(request.getParameter("address"));
        java.util.Map<String,String> errors=new java.util.LinkedHashMap<>();
        if(AccountValidation.nameError(name)!=null)errors.put("fullName",AccountValidation.nameError(name));
        if(AccountValidation.phoneError(phone)!=null)errors.put("phone",AccountValidation.phoneError(phone));
        if(AccountValidation.emailError(email)!=null)errors.put("email",AccountValidation.emailError(email));
        if(hasBirthdayParts && birthdayParts!=0 && birthdayParts!=3)
            errors.put("birthDay","Vui lòng chọn đủ ngày, tháng và năm sinh hoặc để trống cả ba.");
        else if(AccountValidation.birthdayError(birthday)!=null)errors.put("birthDay",AccountValidation.birthdayError(birthday));
        if(AccountValidation.addressError(address)!=null)errors.put("address",AccountValidation.addressError(address));
        if(address.length()>500)errors.put("address","Địa chỉ tối đa 500 ký tự.");
        // PF12f: . So sánh email vừa nhập với email hiện tại để quyết định có cần profilePassword không.
        // .. Email đổi và đúng định dạng thì phải nhập đúng mật khẩu hiện tại; giữ email thì bỏ bước này.
        // ... UserDAO.verifyPasswordById() [PF12g] kiểm tra dữ liệu từ ô [PF06f].
        boolean changingEmail=!Objects.equals(email,AccountValidation.email(user.getEmail()));
        if(changingEmail && !errors.containsKey("email") && !users.verifyPasswordById(user.getId(),request.getParameter("profilePassword")))
            errors.put("profilePassword","Vui lòng nhập đúng mật khẩu hiện tại để thay đổi email.");
        if(errors.isEmpty()){
            // . Chỉ khi errors rỗng mới gọi DAO ở bước [PF13].
            // .. DAO trả lỗi trùng thì gắn vào email/phone; lỗi chung thì đặt errorMsg rồi dừng.
            // ... Nhánh có lỗi [PF14]; lưu thành công cập nhật session [PF15].
            String result=users.updateUserProfile(user.getId(),name,phone,email,birthday,address);
            if(!"success".equals(result)){
                if(result.contains("Email"))errors.put("email",result);
                if(result.contains("điện thoại"))errors.put("phone",result);
                if(errors.isEmpty()){request.setAttribute("errorMsg",result);return;}
            }
        }
        // PF14: . Xử lý kết quả kiểm tra/lưu hồ sơ.
        // .. Có lỗi thì giữ dữ liệu an toàn, không cập nhật user trong session; không còn lỗi thì sang [PF15].
        // ... AccountFormFeedback.show() → account_feedback.jsp → hiển thị lỗi dưới ô; trả trang qua [PF16].
        if(!errors.isEmpty()){
            java.util.Map<String,String> values=new java.util.LinkedHashMap<>(java.util.Map.of("fullName",name,"phone",phone,"email",email,"address",address));
            for(String part:new String[]{"birthDay","birthMonth","birthYear"})
                if(request.getParameter(part)!=null)values.put(part,request.getParameter(part));
            utils.AccountFormFeedback.show(request,"profile-update",errors,values);
            request.setAttribute("errorMsg","Vui lòng kiểm tra các trường được đánh dấu bên dưới.");return;
        }
        // PF15: . Sau khi DAO lưu thành công, cập nhật thông tin mới vào User session và đặt successMsg.
        // ... renderProfile() [PF16] trả trang → thông báo thành công [PF17].
        user.setFullName(name);user.setPhone(phone);user.setEmail(email);user.setBirthday(birthday);user.setAddress(address);
        request.getSession().setAttribute("user",user);request.setAttribute("successMsg","Cập nhật thông tin thành công!");
    }
    // PF05: . Nạp lịch sử điểm/cấu hình và forward trang hồ sơ lần đầu sau bước [PF04].
    // ... profile.jsp hiển thị thông tin từ session.user tại [PF06].
    // PF16: . Sau cập nhật, trả lại trang hồ sơ cùng thông báo hoặc lỗi.
    // ... profile.jsp hiển thị thông báo thành công tại [PF17].
    // PW08: . Sau đổi mật khẩu, renderProfile() trả lại cùng trang hồ sơ.
    // ... Thông báo đổi mật khẩu thành công tại [PW09].
    protected void renderProfile(HttpServletRequest request,HttpServletResponse response,User user)throws ServletException,IOException{
        request.setAttribute("loyaltyHistory",users.getLoyaltyHistoryByUserId(user.getId()));
        request.setAttribute("shopSettings",new dao.ShopSettingDAO().getSettings());
        request.getRequestDispatcher("/profile.jsp").forward(request,response);
    }
}
