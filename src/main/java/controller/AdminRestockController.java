package controller;

import dao.IngredientDAO;
import dao.RestockRequestDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.Ingredient;
import model.RestockRequest.RestockItem;
import model.User;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@WebServlet(name="AdminRestockController",urlPatterns={"/admin/restock-requests"})
public class AdminRestockController extends HttpServlet{
    private final RestockRequestDAO dao=new RestockRequestDAO();private final IngredientDAO ingredientDAO=new IngredientDAO();
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException{
        if(!ScaffoldAccess.requireAdmin(r,p))return;HttpSession s=r.getSession();if(s.getAttribute("restockCsrfToken")==null)s.setAttribute("restockCsrfToken",UUID.randomUUID().toString());
        move(s,r,"restockMessage");move(s,r,"restockError");
        r.setAttribute("lowStockList",dao.getLowStockIngredients());r.setAttribute("requests",dao.getRestockRequests("ALL"));
        r.setAttribute("allIngredients",ingredientDAO.getAllIngredientsWithDetails(null,null));r.setAttribute("stats",dao.getRestockStats());
        r.getRequestDispatcher("/admin-restock.jsp").forward(r,p);
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse p)throws IOException{
        if(!ScaffoldAccess.requireAdmin(r,p))return;r.setCharacterEncoding("UTF-8");HttpSession s=r.getSession();
        if(!String.valueOf(s.getAttribute("restockCsrfToken")).equals(r.getParameter("csrfToken"))){s.setAttribute("restockError","Phiên thao tác không hợp lệ.");p.sendRedirect(r.getContextPath()+"/admin/restock-requests");return;}
        User u=(User)s.getAttribute("user");String action=trim(r.getParameter("action"));
        try{
            boolean ok=true;
            if("create".equals(action)||"saveDraft".equals(action)){
                int ingredientId=Integer.parseInt(r.getParameter("ingredientId"));BigDecimal qty=new BigDecimal(r.getParameter("quantity"));
                RestockItem item=new RestockItem(ingredientId,"","",qty);item.setItemNote(trim(r.getParameter("itemNote")));
                dao.createRestockRequest(u.getId(),"saveDraft".equals(action)?"DRAFT":"PENDING_APPROVAL",trim(r.getParameter("reason")),trim(r.getParameter("note")),List.of(item));
            }else{
                int id=Integer.parseInt(r.getParameter("id"));ok=dao.transition(id,u.getId(),action,trim(r.getParameter("reviewNote")));
            }
            if(ok)s.setAttribute("restockMessage","Đã cập nhật đề xuất nhập hàng.");
            else s.setAttribute("restockError","Không thể cập nhật vì trạng thái đề xuất đã thay đổi.");
        }catch(Exception e){getServletContext().log("Restock action failed",e);s.setAttribute("restockError",e.getMessage());}
        p.sendRedirect(r.getContextPath()+"/admin/restock-requests");
    }
    private void move(HttpSession s,HttpServletRequest r,String k){Object v=s.getAttribute(k);if(v!=null){r.setAttribute(k,v);s.removeAttribute(k);}}
    private String trim(String v){return v==null?"":v.trim();}
    static BigDecimal calculateUnitPrice(BigDecimal quantity,BigDecimal totalPrice){
        if(quantity==null||totalPrice==null||quantity.signum()<=0||totalPrice.signum()<0)throw new IllegalArgumentException("Số lượng và tổng tiền không hợp lệ.");
        return totalPrice.divide(quantity,2,RoundingMode.HALF_UP);
    }
}
