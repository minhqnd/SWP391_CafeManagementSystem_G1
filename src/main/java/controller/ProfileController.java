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
    // . Hàm authorize trong ProfileController lấy mã tài khoản đang đăng nhập rồi gọi getActiveUserById của
    //   UserDAO. Em đọc lại dữ liệu ở đây để các bước mở hoặc cập nhật hồ sơ dùng thông tin tài khoản hiện
    //   còn hoạt động.
    //
    // Tra tiếp: UserDAO.java / HSM03.
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

        if("update".equals(action))update(request,user);

        // [MKL02]
        // . Trong doPost của ProfileController, em nhận ba ô đổi mật khẩu và kiểm tra mật khẩu mới cùng xác
        //   nhận. Đúng điều kiện thì em gọi changePasswordById của UserDAO, truyền tài khoản đang dùng và hai
        //   mật khẩu để đối chiếu rồi đổi.
        //
        // Tra tiếp: UserDAO.java / MKL03.
        else if("changePassword".equals(action)){
            String password=request.getParameter("newPassword");
            java.util.Map<String,String> errors=AccountValidation.passwordErrors(password,request.getParameter("confirmPassword"),"newPassword","confirmPassword");
            String current=request.getParameter("oldPassword");
            if(current==null || current.isEmpty()) errors.put("oldPassword","Vui lòng nhập mật khẩu hiện tại.");
            else if(current.length()>255) errors.put("oldPassword","Mật khẩu hiện tại tối đa 255 ký tự.");

            boolean passwordSystemError = false;
            try {
                if(errors.isEmpty() && !users.changePasswordById(user.getId(),current,password))
                    errors.put("oldPassword","Mật khẩu hiện tại không đúng hoặc tài khoản không còn hoạt động.");
            } catch (utils.AccountDataException e) {
                passwordSystemError = true;
            }

            // [MKL04]
            // . Sau khi nhận kết quả đổi mật khẩu, em báo thành công hoặc đưa lời báo về đúng ô cần sửa. Em
            //   gọi renderProfile để chuẩn bị lại trang hồ sơ cho người dùng tiếp tục.
            //
            // Kết thúc: đổi mật khẩu khi trang hồ sơ được chuẩn bị lại kèm kết quả.
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
    // . Trong update của ProfileController, em nhận các ô, ghép ngày sinh và kiểm tra thông tin; sai thì giữ
    //   các ô kèm lời báo để sửa. Hợp lệ thì em chuyển mã tài khoản đang dùng cùng hồ sơ cho
    //   updateUserProfile trong UserDAO để lưu đúng người.
    //
    // Tra tiếp: UserDAO.java / HSL03.
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

        if(AccountValidation.addressError(address)!=null)errors.put("address",AccountValidation.addressError(address));
        if(address.length()>500)errors.put("address","Địa chỉ tối đa 500 ký tự.");
        if(errors.isEmpty()){

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

        if(!errors.isEmpty()){
            java.util.Map<String,String> values=new java.util.LinkedHashMap<>(java.util.Map.of("fullName",name,"phone",phone,"email",email,"address",address));
            for(String part:new String[]{"birthDay","birthMonth","birthYear"})
                if(request.getParameter(part)!=null)values.put(part,request.getParameter(part));
            utils.AccountFormFeedback.show(request,"profile-update",errors,values);
            request.setAttribute("errorMsg","Vui lòng kiểm tra các trường được đánh dấu bên dưới.");return;
        }

        // [HSL04]
        // . Khi updateUserProfile lưu hồ sơ thành công, em quay lại update trong ProfileController để cập
        //   nhật thông tin đang giữ cho lần đăng nhập. Nhờ vậy tên tài khoản và hồ sơ hiển thị cùng khớp
        //   với nội dung vừa sửa. Sau đó em gọi renderProfile để đưa hồ sơ cùng thông báo thành công về
        //   profile.jsp.
        //
        // Tra tiếp: profile.jsp / HSL05.
        user.setFullName(name);user.setPhone(phone);user.setEmail(email);user.setBirthday(birthday);user.setAddress(address);
        request.getSession().setAttribute("user",user);request.setAttribute("successMsg","Cập nhật thông tin thành công!");
    }

    // Gom các ô hồ sơ được phép giữ để người dùng sửa tiếp sau lỗi.
    private java.util.Map<String,String> profileValues(HttpServletRequest request,String name,String phone,String email,String address) {
        java.util.Map<String,String> values=new java.util.LinkedHashMap<>(java.util.Map.of("fullName",name,"phone",phone,"email",email,"address",address));
        for(String part:new String[]{"birthDay","birthMonth","birthYear"})
            if(request.getParameter(part)!=null)values.put(part,request.getParameter(part));
        return values;
    }

    // [HSM04]
    // . Hàm renderProfile trong ProfileController chuẩn bị hồ sơ, lịch sử điểm và quy tắc trước khi đưa về
    //   profile.jsp. Lịch sử được đọc từ UserDAO, còn quy tắc từ ShopSettingDAO; nếu riêng lịch sử đọc lỗi
    //   thì em báo rõ phần đó để người dùng vẫn xem được hồ sơ.
    //
    // Tra tiếp: profile.jsp / HSM05.
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
