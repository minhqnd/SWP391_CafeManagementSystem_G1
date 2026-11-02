package controller;

import jakarta.servlet.http.*;
import model.User;
import java.io.IOException;
import java.util.UUID;

final class InventoryAccess {
    private InventoryAccess() {}
    static User require(HttpServletRequest r,HttpServletResponse p,boolean adminOnly)throws IOException {
        HttpSession s=r.getSession(false);User u=s==null?null:(User)s.getAttribute("user");
        if(u==null){p.sendRedirect(r.getContextPath()+"/login");return null;}
        if(!"ACTIVE".equals(u.getStatus()) || !("ADMIN".equals(u.getRole()) || (!adminOnly&&"STAFF".equals(u.getRole())&&"BARISTA".equals(u.getPosition())))){
            p.sendError(403);return null;
        }return u;
    }
    static String token(HttpSession s){if(s.getAttribute("inventoryWorkflowToken")==null)s.setAttribute("inventoryWorkflowToken",UUID.randomUUID().toString());return (String)s.getAttribute("inventoryWorkflowToken");}
    static boolean csrf(HttpServletRequest r,HttpServletResponse p)throws IOException {
        HttpSession s=r.getSession(false);String expected=s==null?null:(String)s.getAttribute("inventoryWorkflowToken");
        if(expected==null||!expected.equals(r.getParameter("csrfToken"))){p.sendError(403,"Phiên thao tác không hợp lệ.");return false;}return true;
    }
    static void flash(HttpServletRequest r){for(String k:new String[]{"workflowMessage","workflowError"}){Object v=r.getSession().getAttribute(k);r.setAttribute(k,v);r.getSession().removeAttribute(k);}}
}
