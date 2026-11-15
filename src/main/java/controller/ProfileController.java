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

    // PF06: . Lấy user hiện tại từ session rồi kiểm tra lại tài khoản còn ACTIVE.
    // .. Không hợp lệ thì hủy session và chuyển về đăng nhập; hợp lệ cập nhật user mới vào session.
    // ... UserDAO.getActiveUserById() đọc dữ liệu tài khoản [PF07].
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
    // PF02: . Nhận GET /profile khi người dùng bấm Hồ sơ.
    // .. Phiên hợp lệ thì chuẩn bị dữ liệu hồ sơ; phiên không hợp lệ thì chuyển về đăng nhập.
    // ... authorize() kiểm tra phiên [PF06], sau đó renderProfile() trả giao diện [PF05].
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException {
        User user=authorize(request,response);if(user==null)return;
        AccountCsrf.token(request.getSession());renderProfile(request,response,user);
    }
    // PF03: . Nhận POST /profile, kiểm tra CSRF và xác thực tài khoản trước khi đọc action.
    // .. action=update cập nhật hồ sơ; action=changePassword chuyển sang nhánh đổi mật khẩu [PW02].
    // ... Dữ liệu của từng nhánh được xử lý trong update() hoặc UserDAO tương ứng.
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException {
        request.setCharacterEncoding("UTF-8");
        if(!AccountCsrf.require(request,response))return;
        User user=authorize(request,response);if(user==null)return;
        String action=request.getParameter("action");
        if("update".equals(action))update(request,user);
        // PW02: . Nhận oldPassword, newPassword và confirmPassword từ form đổi mật khẩu.
        // .. Sai trường thì giữ form và báo lỗi; hợp lệ mới chuyển sang cập nhật database.
        // ... UserDAO.changePasswordById() xử lý đổi mật khẩu theo ID trong session [PW03].
        else if("changePassword".equals(action)){
            String password=request.getParameter("newPassword");
            java.util.Map<String,String> errors=AccountValidation.passwordErrors(password,request.getParameter("confirmPassword"),"newPassword","confirmPassword");
            String current=request.getParameter("oldPassword");
            if(current==null || current.isEmpty()) errors.put("oldPassword","Vui lòng nhập mật khẩu hiện tại.");
            if(errors.isEmpty() && !users.changePasswordById(user.getId(),current,password))
                errors.put("oldPassword","Mật khẩu hiện tại không đúng hoặc tài khoản không còn hoạt động.");
            // PW04: . Xử lý kết quả đổi mật khẩu.
            // .. Thành công thì đổi session ID, xoay CSRF và báo thành công; lỗi được gắn vào đúng trường.
            // ... renderProfile() trả lại giao diện hồ sơ [PF05].
            if(errors.isEmpty()){request.changeSessionId();AccountCsrf.rotate(request.getSession());request.setAttribute("successMsg","Đổi mật khẩu thành công!");}
            else {
                utils.AccountFormFeedback.show(request,"profile-password",errors,java.util.Map.of());
                request.setAttribute("errorMsg","Vui lòng kiểm tra các trường được đánh dấu bên dưới.");
            }
        }else request.setAttribute("errorMsg","Thao tác hồ sơ không hợp lệ.");
        renderProfile(request,response,(User)request.getSession().getAttribute("user"));
    }
    // PF08: . Đọc họ tên, SĐT, email, ngày sinh và địa chỉ từ form cập nhật hồ sơ.
    // .. Chuẩn hóa dữ liệu rồi kiểm tra từng trường; ngày sinh không đủ/sai và địa chỉ quá dài sẽ tạo lỗi.
    private void update(HttpServletRequest request,User user){
        String name=AccountValidation.text(request.getParameter("fullName"));
        String phone=AccountValidation.phone(request.getParameter("phone")==null?user.getPhone():request.getParameter("phone"));
        String email=AccountValidation.email(request.getParameter("email")==null?user.getEmail():request.getParameter("email"));
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
        String address=AccountValidation.text(request.getParameter("address"));
        java.util.Map<String,String> errors=new java.util.LinkedHashMap<>();
        if(AccountValidation.nameError(name)!=null)errors.put("fullName",AccountValidation.nameError(name));
        if(AccountValidation.phoneError(phone)!=null)errors.put("phone",AccountValidation.phoneError(phone));
        if(AccountValidation.emailError(email)!=null)errors.put("email",AccountValidation.emailError(email));
        if(hasBirthdayParts && birthdayParts!=0 && birthdayParts!=3)
            errors.put("birthDay","Vui lòng chọn đủ ngày, tháng và năm sinh hoặc để trống cả ba.");
        else if(AccountValidation.birthdayError(birthday)!=null)errors.put("birthDay",AccountValidation.birthdayError(birthday));
        if(address.length()>500)errors.put("address","Địa chỉ tối đa 500 ký tự.");
        // . Chỉ khi email thay đổi mới yêu cầu profilePassword; giữ email hiện tại thì không cần mật khẩu.
        // ... UserDAO.verifyPasswordById() đối chiếu mật khẩu hiện tại [PF09].
        boolean changingEmail=!Objects.equals(email,AccountValidation.email(user.getEmail()));
        if(changingEmail && !errors.containsKey("email") && !users.verifyPasswordById(user.getId(),request.getParameter("profilePassword")))
            errors.put("profilePassword","Vui lòng nhập đúng mật khẩu hiện tại để thay đổi email.");
        if(errors.isEmpty()){
            // ... Không còn lỗi thì gửi thông tin tới UserDAO.updateUserProfile() [PF04].
            String result=users.updateUserProfile(user.getId(),name,phone,email,birthday,address);
            if(!"success".equals(result)){
                if(result.contains("Email"))errors.put("email",result);
                if(result.contains("điện thoại"))errors.put("phone",result);
                if(errors.isEmpty()){request.setAttribute("errorMsg",result);return;}
            }
        }
        // .. Có lỗi thì giữ dữ liệu an toàn và không báo cập nhật thành công.
        // ... AccountFormFeedback.show() đưa lỗi và giá trị an toàn về trang hồ sơ.
        if(!errors.isEmpty()){
            java.util.Map<String,String> values=new java.util.LinkedHashMap<>(java.util.Map.of("fullName",name,"phone",phone,"email",email,"address",address));
            for(String part:new String[]{"birthDay","birthMonth","birthYear"})
                if(request.getParameter(part)!=null)values.put(part,request.getParameter(part));
            utils.AccountFormFeedback.show(request,"profile-update",errors,values);
            request.setAttribute("errorMsg","Vui lòng kiểm tra các trường được đánh dấu bên dưới.");return;
        }
        // .. Chỉ sau khi DAO lưu thành công mới cập nhật user trong session và hiện thông báo thành công.
        user.setFullName(name);user.setPhone(phone);user.setEmail(email);user.setBirthday(birthday);user.setAddress(address);
        request.getSession().setAttribute("user",user);request.setAttribute("successMsg","Cập nhật thông tin thành công!");
    }
    // PF05: . Nạp dữ liệu hồ sơ, lịch sử điểm và cấu hình hiển thị.
    // ... forward /profile.jsp để giao diện hiển thị dữ liệu và thông báo trong cùng request [PF01].
    protected void renderProfile(HttpServletRequest request,HttpServletResponse response,User user)throws ServletException,IOException{
        request.setAttribute("loyaltyHistory",users.getLoyaltyHistoryByUserId(user.getId()));
        request.setAttribute("shopSettings",new dao.ShopSettingDAO().getSettings());
        request.getRequestDispatcher("/profile.jsp").forward(request,response);
    }
}
