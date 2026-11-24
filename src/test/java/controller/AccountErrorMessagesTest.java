package controller;

import dao.UserDAO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.User;
import org.junit.jupiter.api.Test;
import support.ServletMocks;
import utils.AccountDataException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AccountErrorMessagesTest {
    @Test void loginDatabaseFailureDoesNotClaimWrongPasswordAndKeepsIdentifier() throws Exception {
        ServletMocks mock = loginForm();
        mock.sessionValues.put("successMsg", "old success");
        new LoginController(new UserDAO() {
            @Override public User login(String name, String password) { throw dataFailure(); }
        }).doPost(mock.request, mock.response);
        assertTrue(mock.sessionValues.get("errorMsg").toString().contains("lỗi hệ thống"));
        assertNull(mock.sessionValues.get("successMsg"));
        assertTrue(errors(mock).isEmpty());
        assertEquals("qa@gmail.com", values(mock).get("username"));
        assertFalse(values(mock).containsKey("password"));
    }

    @Test void invalidCredentialsHaveOneGeneralErrorWithoutBlamingOneField() throws Exception {
        ServletMocks mock = loginForm();
        new LoginController(new UserDAO() {
            @Override public User login(String name, String password) { return null; }
        }).doPost(mock.request, mock.response);
        assertTrue(mock.sessionValues.get("errorMsg").toString().contains("không đúng"));
        assertTrue(errors(mock).isEmpty());
    }

    @Test void failedRegistrationLookupKeepsValuesAndDoesNotMarkValidContactsInvalid() throws Exception {
        ServletMocks mock = registrationForm();
        new RegisterController(new UserDAO() {
            @Override public boolean checkPhoneExists(String phone) { throw dataFailure(); }
            @Override public boolean register(User user, String password) { fail("Unavailable lookup must not create account"); return false; }
        }).doPost(mock.request, mock.response);
        assertTrue(errors(mock).isEmpty());
        assertEquals("QA", values(mock).get("fullname"));
        assertEquals("qa@gmail.com", values(mock).get("email"));
        assertTrue(mock.sessionValues.get("errorMsg").toString().contains("Chưa tạo tài khoản"));
    }

    @Test void failedRegistrationSaveKeepsAllNonSecretFields() throws Exception {
        ServletMocks mock = registrationForm();
        new RegisterController(new UserDAO() {
            @Override public boolean checkPhoneExists(String phone) { return false; }
            @Override public boolean checkEmailExists(String email) { return false; }
            @Override public boolean register(User user, String password) { throw dataFailure(); }
        }).doPost(mock.request, mock.response);
        assertEquals(Map.of("fullname", "QA", "phone", "0965999999", "email", "qa@gmail.com"), values(mock));
        assertNull(mock.sessionValues.get("successMsg"));
    }

    @Test void failedProfileLoadDoesNotInvalidateAnOtherwiseValidSession() throws Exception {
        ServletMocks mock = profileForm();
        profile(new UserDAO() {
            @Override public User getActiveUserById(int id) { throw dataFailure(); }
        }).doPost(mock.request, mock.response);
        assertEquals(503, mock.error);
        assertFalse(mock.invalidated);
        assertNotNull(mock.sessionValues.get("user"));
    }

    @Test void failedPasswordWriteIsNotShownAsIncorrectCurrentPassword() throws Exception {
        ServletMocks mock = profileForm();
        mock.parameters.putAll(Map.of("action", "changePassword", "oldPassword", "Current123", "newPassword", "Secret123", "confirmPassword", "Secret123"));
        profile(new UserDAO() {
            @Override public User getActiveUserById(int id) { return user(); }
            @Override public boolean changePasswordById(int id, String current, String password) { throw dataFailure(); }
        }).doPost(mock.request, mock.response);
        Map<?,?> feedback = (Map<?,?>) mock.attributes.get("accountFeedback");
        assertTrue(((Map<?,?>) feedback.get("errors")).isEmpty());
        assertTrue(mock.attributes.get("errorMsg").toString().contains("Không đổi được mật khẩu"));
        assertNull(mock.attributes.get("successMsg"));
    }

    @Test void genericContactConflictKeepsDraftWithoutMarkingBothFieldsAsDefinitelyDuplicate() throws Exception {
        ServletMocks mock = profileForm();
        mock.parameters.putAll(Map.of("action", "update", "fullName", "New name", "address", "123 Pho Hue"));
        profile(new UserDAO() {
            @Override public User getActiveUserById(int id) { return user(); }
            @Override public String updateUserProfile(int id, String name, String phone, String email, String birthday, String address) {
                return "Thông tin liên hệ vừa được tài khoản khác sử dụng. Vui lòng kiểm tra lại email và số điện thoại.";
            }
        }).doPost(mock.request, mock.response);
        Map<?,?> feedback = (Map<?,?>) mock.attributes.get("accountFeedback");
        assertTrue(((Map<?,?>) feedback.get("errors")).isEmpty());
        assertEquals("New name", ((Map<?,?>) feedback.get("values")).get("fullName"));
        assertEquals("QA", ((User) mock.sessionValues.get("user")).getFullName());
        assertNull(mock.attributes.get("successMsg"));
    }

    private static AccountDataException dataFailure() { return new AccountDataException(new Exception("private database information")); }
    private static User user() { return new User(7, "QA", "0965999999", "qa@gmail.com", "CUSTOMER", null, "ACTIVE"); }
    private ServletMocks loginForm() {
        ServletMocks mock = new ServletMocks(); mock.csrf();
        mock.parameters.putAll(Map.of("username", "qa@gmail.com", "password", "Secret123")); return mock;
    }
    private ServletMocks registrationForm() {
        ServletMocks mock = new ServletMocks(); mock.csrf();
        mock.parameters.putAll(Map.of("fullname", "QA", "phone", "0965999999", "email", "qa@gmail.com", "password", "Secret123", "confirm_password", "Secret123")); return mock;
    }
    private ServletMocks profileForm() { ServletMocks mock = new ServletMocks(); mock.csrf(); mock.sessionValues.put("user", user()); return mock; }
    private ProfileController profile(UserDAO dao) {
        return new ProfileController(dao) {
            @Override protected void renderProfile(HttpServletRequest request, HttpServletResponse response, User user) { }
        };
    }
    private Map<?,?> errors(ServletMocks mock) { return (Map<?,?>) ((Map<?,?>) mock.sessionValues.get("accountFeedback")).get("errors"); }
    private Map<?,?> values(ServletMocks mock) { return (Map<?,?>) ((Map<?,?>) mock.sessionValues.get("accountFeedback")).get("values"); }
}
