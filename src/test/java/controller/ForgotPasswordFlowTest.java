package controller;

import dao.UserDAO;
import java.util.concurrent.atomic.AtomicReference;
import model.User;
import org.junit.jupiter.api.Test;
import service.PasswordRecoveryService;
import support.ServletMocks;
import utils.RecoveryFailure;
import static org.junit.jupiter.api.Assertions.*;

class ForgotPasswordFlowTest {
    private ServletMocks form(){
        ServletMocks mock=new ServletMocks();mock.csrf();
        mock.parameters.put("email","nguyen.nguyenquoc2004@gmail.com");return mock;
    }
    @Test void registeredEmailSendsOtpInsteadOfBeingRejectedAsDuplicate()throws Exception{
        ServletMocks mock=form();AtomicReference<String> recipient=new AtomicReference<>();
        PasswordRecoveryService recovery=new PasswordRecoveryService(new UserDAO(){
            @Override public User findActiveAccountByEmail(String email){
                return new User(7,"QA","0987111222",email,"CUSTOMER",null,"ACTIVE");
            }
            @Override public boolean checkEmailExists(String email){fail("Forgot password must not apply registration duplicate validation");return true;}
        },(email,otp)->{recipient.set(email);assertTrue(otp.matches("[0-9]{6}"));return true;},()->1000000L);
        new ForgotPasswordController(recovery).doPost(mock.request,mock.response);
        assertEquals("nguyen.nguyenquoc2004@gmail.com",recipient.get());
        assertEquals("/cafe/?tab=otp",mock.redirect);assertNull(mock.sessionValues.get("errorMsg"));
    }
    @Test void missingAccountHasEmailFeedbackButDatabaseFailureDoesNotMarkEmailInvalid()throws Exception{
        for(boolean databaseFailure:new boolean[]{false,true}){
            ServletMocks mock=form();
            PasswordRecoveryService recovery=new PasswordRecoveryService(new UserDAO(){
                @Override public User findActiveAccountByEmail(String email){
                    if(databaseFailure)throw new RecoveryFailure(null,"Không tra cứu được dữ liệu tài khoản.");
                    return null;
                }
            },(email,otp)->{fail("No resolved account must not send mail");return false;},()->1000000L);
            new ForgotPasswordController(recovery).doPost(mock.request,mock.response);
            java.util.Map<?,?> feedback=(java.util.Map<?,?>)mock.sessionValues.get("accountFeedback");
            java.util.Map<?,?> errors=(java.util.Map<?,?>)feedback.get("errors");
            assertEquals(!databaseFailure,errors.containsKey("email"));assertEquals("/cafe/?tab=forgot",mock.redirect);
        }
    }
    @Test void smtpFailureDoesNotClaimTheRegisteredEmailIsInvalid()throws Exception{
        ServletMocks mock=form();
        PasswordRecoveryService recovery=new PasswordRecoveryService(new UserDAO(){
            @Override public User findActiveAccountByEmail(String email){return new User(7,"QA","0987111222",email,"CUSTOMER",null,"ACTIVE");}
        },(email,otp)->false,()->1000000L);
        new ForgotPasswordController(recovery).doPost(mock.request,mock.response);
        java.util.Map<?,?> feedback=(java.util.Map<?,?>)mock.sessionValues.get("accountFeedback");
        assertTrue(((java.util.Map<?,?>)feedback.get("errors")).isEmpty());
        assertTrue(mock.sessionValues.get("errorMsg").toString().contains("SMTP"));
    }
}
