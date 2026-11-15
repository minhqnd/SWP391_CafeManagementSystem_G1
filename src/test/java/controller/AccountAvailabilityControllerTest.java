package controller;

import dao.UserDAO;
import model.User;
import org.junit.jupiter.api.Test;
import support.ServletMocks;
import static org.junit.jupiter.api.Assertions.*;

class AccountAvailabilityControllerTest {
    @Test void rejectsNineAndElevenDigitPhoneNumbersBeforeQuerying()throws Exception{
        for(String phone:new String[]{"098711122","09871112223"}){
            ServletMocks mock=request("phone",phone);
            new AccountAvailabilityController(new UserDAO(){
                @Override public boolean contactExists(String f,String v,int id){fail("Invalid phone reached database");return false;}
            }).doPost(mock.request,mock.response);
            assertEquals(400,mock.error);
        }
    }
    private ServletMocks request(String field,String value){
        ServletMocks mock=new ServletMocks();mock.csrf();
        mock.parameters.put("field",field);mock.parameters.put("value",value);return mock;
    }
    @Test void requiresCsrfBeforeLookingUpContacts()throws Exception{
        ServletMocks mock=request("email","qa@example.com");mock.parameters.remove("csrfToken");
        new AccountAvailabilityController(new UserDAO(){
            @Override public boolean contactExists(String f,String v,int id){fail("Missing CSRF reached lookup");return false;}
        }).doPost(mock.request,mock.response);assertEquals(403,mock.error);
    }
    @Test void normalizesBothContactsAndNeverTrustsSubmittedExclusionId()throws Exception{
        for(String field:new String[]{"email","phone"}){
            ServletMocks mock=request(field,field.equals("email")?" QA@EXAMPLE.COM ":"0987.111-222");
            mock.parameters.put("userId","88");
            new AccountAvailabilityController(new UserDAO(){
                @Override public boolean contactExists(String f,String value,int id){
                    assertEquals(field,f);assertEquals(field.equals("email")?"qa@example.com":"0987111222",value);assertEquals(0,id);return true;
                }
            }).doPost(mock.request,mock.response);
            assertEquals("{\"exists\":true}",mock.body.toString());
        }
    }
    @Test void profileExcludesOnlyTheActiveSessionUser()throws Exception{
        ServletMocks mock=request("email","qa@example.com");mock.parameters.put("scope","profile");mock.parameters.put("userId","88");
        User user=new User();user.setId(7);mock.sessionValues.put("user",user);
        new AccountAvailabilityController(new UserDAO(){
            @Override public User getActiveUserById(int id){assertEquals(7,id);return user;}
            @Override public boolean contactExists(String f,String v,int id){assertEquals(7,id);return false;}
        }).doPost(mock.request,mock.response);
        assertEquals("{\"exists\":false}",mock.body.toString());
    }
    @Test void invalidFieldsAndUnavailableDatabaseDoNotReturnAvailable()throws Exception{
        ServletMocks invalid=request("password_hash","secret123");
        new AccountAvailabilityController().doPost(invalid.request,invalid.response);assertEquals(400,invalid.error);
        ServletMocks missing=request("email","qa@example.com");missing.parameters.put("scope","profile");
        new AccountAvailabilityController().doPost(missing.request,missing.response);assertEquals(401,missing.error);
        ServletMocks unavailable=request("email","qa@example.com");
        new AccountAvailabilityController(new UserDAO(){
            @Override public boolean contactExists(String f,String v,int id){throw new IllegalStateException("Unavailable");}
        }).doPost(unavailable.request,unavailable.response);assertEquals(503,unavailable.error);assertEquals("",unavailable.body.toString());
    }
}
