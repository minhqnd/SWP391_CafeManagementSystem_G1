package controller;

import jakarta.servlet.http.*;
import model.User;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InventoryAccessTest {
    final Map<String,Object> session=new HashMap<>();int status;String redirect;
    @SuppressWarnings("unchecked") <T>T proxy(Class<T> type,java.lang.reflect.InvocationHandler h){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},h);}
    HttpServletRequest request(String token){HttpSession s=proxy(HttpSession.class,(p,m,a)->switch(m.getName()){case "getAttribute"->session.get(a[0]);case "setAttribute"->{session.put((String)a[0],a[1]);yield null;}default->null;});return proxy(HttpServletRequest.class,(p,m,a)->switch(m.getName()){case "getSession"->s;case "getContextPath"->"/cafe";case "getParameter"->token;default->null;});}
    HttpServletResponse response(){return proxy(HttpServletResponse.class,(p,m,a)->{if(m.getName().equals("sendError"))status=(int)a[0];if(m.getName().equals("sendRedirect"))redirect=(String)a[0];return null;});}
    User user(String role,String position,String state){User u=new User();u.setRole(role);u.setPosition(position);u.setStatus(state);return u;}
    @Test void baristaCannotUseAdminRoutes()throws Exception {session.put("user",user("STAFF","BARISTA","ACTIVE"));assertNull(InventoryAccess.require(request(null),response(),true));assertEquals(403,status);assertNotNull(InventoryAccess.require(request(null),response(),false));}
    @Test void cashierAndInactiveAdminCannotUseInventoryWorkflow()throws Exception {session.put("user",user("STAFF","CASHIER","ACTIVE"));assertNull(InventoryAccess.require(request(null),response(),false));session.put("user",user("ADMIN","MANAGER","LOCKED"));assertNull(InventoryAccess.require(request(null),response(),true));}
    @Test void guestRedirectsAndMissingOrWrongCsrfFails()throws Exception {assertNull(InventoryAccess.require(request(null),response(),true));assertEquals("/cafe/login",redirect);assertFalse(InventoryAccess.csrf(request("null"),response()));session.put("inventoryWorkflowToken","expected");assertFalse(InventoryAccess.csrf(request("wrong"),response()));assertTrue(InventoryAccess.csrf(request("expected"),response()));}
}
