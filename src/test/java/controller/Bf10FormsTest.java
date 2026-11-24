package controller;

import dao.UserDAO;
import jakarta.servlet.http.*;
import model.User;
import org.junit.jupiter.api.Test;
import support.ServletMocks;
import static org.junit.jupiter.api.Assertions.*;

class Bf10FormsTest {
    @Test void registrationAndProfileRejectNineAndElevenDigitPhoneNumbers()throws Exception{
        for(String invalid:new String[]{"098711122","09871112223"}){
            UserDAO dao=new UserDAO(){
                @Override public User getActiveUserById(int id){return user();}
                @Override public boolean checkEmailExists(String email){return false;}
                @Override public boolean checkPhoneExists(String phone){fail("Invalid phone reached duplicate lookup");return false;}
                @Override public boolean register(User u,String p){fail("Invalid phone registered");return false;}
                @Override public String updateUserProfile(int id,String n,String p,String e,String b,String a){fail("Invalid phone saved to profile");return null;}
            };
            ServletMocks signup=new ServletMocks();signup.csrf();
            signup.parameters.putAll(java.util.Map.of("fullname","QA","phone",invalid,"email","qa@gmail.com","password","Secret123","confirm_password","Secret123"));
            new RegisterController(dao).doPost(signup.request,signup.response);
            java.util.Map<?,?> signupFeedback=(java.util.Map<?,?>)signup.sessionValues.get("accountFeedback");
            assertEquals("Số điện thoại sai định dạng.",((java.util.Map<?,?>)signupFeedback.get("errors")).get("phone"));
            ServletMocks update=new ServletMocks();update.csrf();update.sessionValues.put("user",user());
            update.parameters.putAll(java.util.Map.of("action","update","fullName","QA","phone",invalid));
            profile(dao).doPost(update.request,update.response);
            java.util.Map<?,?> profileFeedback=(java.util.Map<?,?>)update.attributes.get("accountFeedback");
            assertEquals("Số điện thoại sai định dạng.",((java.util.Map<?,?>)profileFeedback.get("errors")).get("phone"));
        }
    }
    private User user(){return new User(7,"QA","0987111222","qa@gmail.com","CUSTOMER",null,"ACTIVE");}
    @Test void registrationRejectsMissingConfirmationBeforeCallingDao()throws Exception{
        ServletMocks mock=new ServletMocks();mock.csrf();
        mock.parameters.put("fullname","QA");mock.parameters.put("email","qa@gmail.com");mock.parameters.put("phone","0987111222");mock.parameters.put("password","secret123");
        UserDAO dao=new UserDAO(){
            @Override public boolean checkPhoneExists(String phone){return false;}
            @Override public boolean checkEmailExists(String email){return false;}
            @Override public boolean register(User user,String password){fail("Invalid form wrote account");return false;}
        };
        new RegisterController(dao).doPost(mock.request,mock.response);
        assertEquals("/cafe/?tab=register",mock.redirect);assertNotNull(mock.sessionValues.get("errorMsg"));
    }
    @Test void registrationRejectsCsrfWithoutWritingAnything()throws Exception{
        ServletMocks mock=new ServletMocks();new RegisterController().doPost(mock.request,mock.response);assertEquals(403,mock.error);
    }
    @Test void registrationReturnsEveryFieldErrorAndNeverPersistsPasswords()throws Exception{
        ServletMocks mock=new ServletMocks();mock.csrf();
        mock.parameters.put("fullname"," ");mock.parameters.put("phone","bad");mock.parameters.put("email","bad");
        mock.parameters.put("password","1234567");mock.parameters.put("confirm_password","different");
        new RegisterController(new UserDAO(){
            @Override public boolean register(User u,String p){fail("Invalid registration must not write");return false;}
        }).doPost(mock.request,mock.response);
        java.util.Map<?,?> feedback=(java.util.Map<?,?>)mock.sessionValues.get("accountFeedback");
        java.util.Map<?,?> errors=(java.util.Map<?,?>)feedback.get("errors");
        assertEquals(java.util.Set.of("fullname","phone","email","password","confirm_password"),errors.keySet());
        java.util.Map<?,?> values=(java.util.Map<?,?>)feedback.get("values");
        assertFalse(values.containsKey("password"));assertFalse(values.containsKey("confirm_password"));
    }
    @Test void registrationReportsBothDuplicateContactsEvenWithInvalidPassword()throws Exception{
        ServletMocks mock=new ServletMocks();mock.csrf();
        mock.parameters.put("fullname","QA");mock.parameters.put("phone","0987111222");mock.parameters.put("email","qa@gmail.com");
        mock.parameters.put("password","1234567");mock.parameters.put("confirm_password","different");
        new RegisterController(new UserDAO(){
            @Override public boolean checkPhoneExists(String phone){return true;}
            @Override public boolean checkEmailExists(String email){return true;}
            @Override public boolean register(User u,String p){fail("Duplicate registration must not write");return false;}
        }).doPost(mock.request,mock.response);
        java.util.Map<?,?> feedback=(java.util.Map<?,?>)mock.sessionValues.get("accountFeedback");
        java.util.Map<?,?> errors=(java.util.Map<?,?>)feedback.get("errors");
        assertEquals(java.util.Set.of("phone","email","password","confirm_password"),errors.keySet());
    }
    @Test void registrationNormalizesAndCreatesCustomerOnly()throws Exception{
        ServletMocks mock=new ServletMocks();mock.csrf();
        mock.parameters.put("fullname"," QA ");mock.parameters.put("email"," QA@GMAIL.COM ");mock.parameters.put("phone","0987.111-222");mock.parameters.put("password"," secret123 ");mock.parameters.put("confirm_password"," secret123 ");mock.parameters.put("role","ADMIN");
        UserDAO dao=new UserDAO(){
            @Override public boolean checkPhoneExists(String phone){assertEquals("0987111222",phone);return false;}
            @Override public boolean checkEmailExists(String email){assertEquals("qa@gmail.com",email);return false;}
            @Override public boolean register(User user,String password){assertEquals("CUSTOMER",user.getRole());assertEquals("QA",user.getFullName());assertEquals(" secret123 ",password);return true;}
        };
        new RegisterController(dao).doPost(mock.request,mock.response);assertEquals("/cafe/?tab=login",mock.redirect);
    }
    private ProfileController profile(UserDAO users){return new ProfileController(users){
        @Override protected void renderProfile(HttpServletRequest r,HttpServletResponse p,User u){}
    };}

    @Test void profileNameAndAddressCheckBothSidesOfTheLimitAndRetainInvalidInput() throws Exception {
        for (String field : new String[]{"fullName", "address"}) {
            int max = field.equals("fullName") ? 100 : 500;
            for (int size : new int[]{max - 1, max, max + 1}) {
                ServletMocks mock = new ServletMocks(); mock.csrf(); mock.sessionValues.put("user", user());
                mock.parameters.putAll(java.util.Map.of("action", "update", "fullName", "QA", "address", "123 Pho Hue"));
                String value = "x".repeat(size); mock.parameters.put(field, value);
                int[] saves = {0};
                profile(new UserDAO() {
                    @Override public User getActiveUserById(int id) { return user(); }
                    @Override public String updateUserProfile(int id,String n,String p,String e,String b,String a) { saves[0]++; return "success"; }
                }).doPost(mock.request, mock.response);
                assertEquals(size <= max ? 1 : 0, saves[0]);
                if (size > max) {
                    java.util.Map<?,?> feedback = (java.util.Map<?,?>)mock.attributes.get("accountFeedback");
                    assertEquals((field.equals("fullName") ? "Họ và tên" : "Địa chỉ") + " tối đa " + max + " ký tự.", ((java.util.Map<?,?>)feedback.get("errors")).get(field));
                    assertEquals(value, ((java.util.Map<?,?>)feedback.get("values")).get(field));
                    assertNull(mock.attributes.get("successMsg"));
                }
            }
        }
    }

    @Test void registrationWhitespaceFieldsAndMissingConfirmationHaveExactRequiredErrors() throws Exception {
        ServletMocks mock = new ServletMocks(); mock.csrf();
        mock.parameters.putAll(java.util.Map.of("fullname", "\u00a0", "email", "\ufeff", "phone", "().-", "password", "Secret123", "confirm_password", "\u2003"));
        new RegisterController(new UserDAO() {
            @Override public boolean checkPhoneExists(String phone) { fail("Empty phone must not query"); return false; }
            @Override public boolean checkEmailExists(String email) { fail("Empty email must not query"); return false; }
            @Override public boolean register(User u,String p) { fail("Invalid form must not save"); return false; }
        }).doPost(mock.request, mock.response);
        java.util.Map<?,?> feedback = (java.util.Map<?,?>)mock.sessionValues.get("accountFeedback");
        assertEquals(java.util.Map.of("fullname", "Vui lòng nhập họ và tên.", "email", "Vui lòng nhập email.",
                "phone", "Vui lòng nhập số điện thoại.", "confirm_password", "Vui lòng nhập mật khẩu xác nhận."), feedback.get("errors"));
        assertNull(mock.sessionValues.get("successMsg"));
    }
    @Test void profileRejectsMissingCsrf()throws Exception{
        ServletMocks mock=new ServletMocks();mock.sessionValues.put("user",user());profile(new UserDAO()).doPost(mock.request,mock.response);assertEquals(403,mock.error);
    }
    @Test void profileReloadsActiveStatusAndInvalidatesLockedSession()throws Exception{
        ServletMocks mock=new ServletMocks();mock.csrf();mock.sessionValues.put("user",user());
        profile(new UserDAO(){@Override public User getActiveUserById(int id){return null;}}).doPost(mock.request,mock.response);
        assertTrue(mock.invalidated);assertEquals("/cafe/?tab=login",mock.redirect);
    }
    @Test void profileRejectsFutureBirthdayAndBlankPhone()throws Exception{
        for(String bad:new String[]{"birthday","phone"}){
            ServletMocks mock=new ServletMocks();mock.csrf();mock.sessionValues.put("user",user());mock.parameters.put("action","update");mock.parameters.put("fullName","QA");
            mock.parameters.put(bad,bad.equals("birthday")?"2999-01-01":"");
            profile(new UserDAO(){@Override public User getActiveUserById(int id){return user();}
                @Override public String updateUserProfile(int id,String n,String p,String e,String b,String a){fail("Invalid form wrote profile");return null;}
            }).doPost(mock.request,mock.response);assertNotNull(mock.attributes.get("errorMsg"));
        }
    }
    @Test void changingEmailSavesForSessionUserWithoutAnExtraPasswordField()throws Exception{
        ServletMocks mock=new ServletMocks();mock.csrf();mock.sessionValues.put("user",user());mock.parameters.put("action","update");mock.parameters.put("fullName","QA");mock.parameters.put("email","new@gmail.com");
        profile(new UserDAO(){@Override public User getActiveUserById(int id){return user();}
            @Override public String updateUserProfile(int id,String n,String p,String e,String b,String a){assertEquals(7,id);assertEquals("new@gmail.com",e);return "success";}
        }).doPost(mock.request,mock.response);assertNotNull(mock.attributes.get("successMsg"));
        assertEquals("new@gmail.com",((User)mock.sessionValues.get("user")).getEmail());
    }
    @Test void registrationAndProfileRejectMistypedGmailBeforeSaving()throws Exception{
        for(String email:new String[]{"nguyen@gmaill.comm","nguyen@gmail.comm","nguyen@gmail.com.evil.com"}){
            UserDAO dao=new UserDAO(){
                @Override public User getActiveUserById(int id){return user();}
                @Override public boolean checkPhoneExists(String phone){return false;}
                @Override public boolean checkEmailExists(String value){fail("Invalid email reached duplicate lookup");return false;}
                @Override public boolean register(User u,String p){fail("Invalid email registered");return false;}
                @Override public String updateUserProfile(int id,String n,String p,String e,String b,String a){fail("Invalid email saved");return null;}
            };
            ServletMocks signup=new ServletMocks();signup.csrf();
            signup.parameters.putAll(java.util.Map.of("fullname","QA","phone","0987111222","email",email,"password","Secret123","confirm_password","Secret123"));
            new RegisterController(dao).doPost(signup.request,signup.response);
            java.util.Map<?,?> feedback=(java.util.Map<?,?>)signup.sessionValues.get("accountFeedback");
            assertEquals("Sai định dạng email.",((java.util.Map<?,?>)feedback.get("errors")).get("email"));
            ServletMocks update=new ServletMocks();update.csrf();update.sessionValues.put("user",user());
            update.parameters.putAll(java.util.Map.of("action","update","fullName","QA","email",email));
            profile(dao).doPost(update.request,update.response);
            feedback=(java.util.Map<?,?>)update.attributes.get("accountFeedback");
            assertEquals("Sai định dạng email.",((java.util.Map<?,?>)feedback.get("errors")).get("email"));
        }
    }
    @Test void profileRejectsIncompleteAndImpossibleBirthdaySelections()throws Exception{
        for(String day:new String[]{"", "31"}){
            ServletMocks mock=new ServletMocks();mock.csrf();mock.sessionValues.put("user",user());
            mock.parameters.put("action","update");mock.parameters.put("fullName","QA");
            mock.parameters.put("birthDay",day);mock.parameters.put("birthMonth","2");mock.parameters.put("birthYear","2001");
            mock.parameters.put("birthday","2001-02-28");
            profile(new UserDAO(){
                @Override public User getActiveUserById(int id){return user();}
                @Override public String updateUserProfile(int id,String n,String p,String e,String b,String a){
                    fail("Incomplete or impossible birthday must not overwrite the profile");return null;
                }
            }).doPost(mock.request,mock.response);
            assertNotNull(mock.attributes.get("errorMsg"));
        }
    }
    @Test void profileSavesValidSelectedBirthdayInsteadOfStaleHiddenValue()throws Exception{
        ServletMocks mock=new ServletMocks();mock.csrf();mock.sessionValues.put("user",user());
        mock.parameters.put("action","update");mock.parameters.put("fullName","QA");
        mock.parameters.put("birthDay","29");mock.parameters.put("birthMonth","2");mock.parameters.put("birthYear","2000");
        mock.parameters.put("birthday","1999-01-01");
        profile(new UserDAO(){
            @Override public User getActiveUserById(int id){return user();}
            @Override public String updateUserProfile(int id,String n,String p,String e,String b,String a){
                assertEquals("2000-02-29",b);return "success";
            }
        }).doPost(mock.request,mock.response);
        assertNotNull(mock.attributes.get("successMsg"));
        assertEquals("2000-02-29",((User)mock.sessionValues.get("user")).getBirthday());
    }
    @Test void passwordChangeUsesSessionUserIdAndRotatesCsrf()throws Exception{
        ServletMocks mock=new ServletMocks();mock.csrf();String oldToken=mock.parameters.get("csrfToken");mock.sessionValues.put("user",user());
        mock.parameters.put("action","changePassword");mock.parameters.put("userId","99");mock.parameters.put("oldPassword","current123");mock.parameters.put("newPassword"," newSecret1 ");mock.parameters.put("confirmPassword"," newSecret1 ");
        profile(new UserDAO(){@Override public User getActiveUserById(int id){return user();}
            @Override public boolean changePasswordById(int id,String old,String next){assertEquals(7,id);assertEquals("current123",old);assertEquals(" newSecret1 ",next);return true;}
        }).doPost(mock.request,mock.response);
        assertNotNull(mock.attributes.get("successMsg"));assertNotEquals(oldToken,mock.sessionValues.get("accountCsrfToken"));
    }
}
