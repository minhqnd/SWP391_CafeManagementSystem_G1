package controller;

import dao.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.RestockRequest.RestockItem;
import model.User;
import java.io.IOException;
import java.math.*;
import java.util.*;

@WebServlet(name="AdminRestockController",urlPatterns={"/admin/restock-requests","/barista/restock-requests"})
public class AdminRestockController extends HttpServlet {
    private final RestockRequestDAO dao=new RestockRequestDAO();
    private final IngredientDAO ingredients=new IngredientDAO();
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException {
        boolean adminRoute=r.getServletPath().startsWith("/admin/");
        User u=InventoryAccess.require(r,p,adminRoute);if(u==null)return;
        boolean admin=adminRoute&&"ADMIN".equals(u.getRole());InventoryAccess.token(r.getSession());InventoryAccess.flash(r);
        try{
            r.setAttribute("workflowAdmin",admin);
            r.setAttribute("requests",dao.findForActor("ADMIN".equals(u.getRole()),u.getId()));
            r.setAttribute("restockStats",dao.getRestockStats());
            r.setAttribute("allIngredients",ingredients.getActiveIngredientsWithDetails());
            r.setAttribute("lowStockList",dao.getLowStockIngredients());
            r.setAttribute("history",new InventoryWorkflowDAO().history("RESTOCK",admin?null:u.getId()));
            r.getRequestDispatcher("/admin-restock.jsp").forward(r,p);
        }catch(Exception e){throw new ServletException("Không tải được đề xuất nhập hàng.",e);}
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse p)throws IOException {
        r.setCharacterEncoding("UTF-8");
        boolean adminRoute=r.getServletPath().startsWith("/admin/");
        User u=InventoryAccess.require(r,p,adminRoute);if(u==null||!InventoryAccess.csrf(r,p))return;
        boolean admin="ADMIN".equals(u.getRole());String action=r.getParameter("action");
        try{
            if(List.of("create","saveDraft","edit").contains(action==null?"":action)){
                String[] ingredientIds=r.getParameterValues("ingredientId"),quantities=r.getParameterValues("quantity");
                if(ingredientIds==null||quantities==null||ingredientIds.length!=quantities.length||ingredientIds.length>100)throw new IllegalArgumentException("Danh sách nguyên liệu không hợp lệ.");
                List<RestockItem> items=new ArrayList<>();
                for(int i=0;i<ingredientIds.length;i++)items.add(new RestockItem(Integer.parseInt(ingredientIds[i]),"","",new BigDecimal(quantities[i])));
                String reason=r.getParameter("reason");
                if(reason==null||reason.trim().length()<3)reason="Bổ sung nguyên liệu phục vụ pha chế";
                String editReason=r.getParameter("editReason");
                if(editReason==null||editReason.trim().length()<3)editReason="Cập nhật số lượng đề xuất";
                if("edit".equals(action))dao.edit(Integer.parseInt(r.getParameter("id")),u.getId(),admin,reason,r.getParameter("note"),items,editReason);
                else dao.createRestockRequest(u.getId(),"saveDraft".equals(action)?"DRAFT":"PENDING_APPROVAL",reason,r.getParameter("note"),items);
            }else if("approveAndOrder".equals(action)&&admin){
                int reqId=Integer.parseInt(r.getParameter("id"));
                dao.transitionFromUi(reqId,u.getId(),true,"approve",r.getParameter("reviewNote"));
                r.getSession().setAttribute("workflowMessage","Đã duyệt đề xuất #RR-"+reqId+". Hãy kiểm tra và lưu phiếu nhập hàng.");
                p.sendRedirect(r.getContextPath()+"/admin/purchases?fromRequestId="+reqId);
                return;
            }else dao.transitionFromUi(Integer.parseInt(r.getParameter("id")),u.getId(),admin,action,r.getParameter("reviewNote"));
            r.getSession().setAttribute("workflowMessage","Đã cập nhật đề xuất nhập hàng thành công!");
        }catch(Exception e){getServletContext().log("Restock action failed",e);r.getSession().setAttribute("workflowError",e instanceof IllegalArgumentException?e.getMessage():"Không thể lưu đề xuất. Hãy tải lại và kiểm tra dữ liệu.");}
        p.sendRedirect(r.getContextPath()+(adminRoute?"/admin/restock-requests":"/barista/restock-requests"));
    }
    static BigDecimal calculateUnitPrice(BigDecimal qty,BigDecimal total){if(qty==null||total==null||qty.signum()<=0||total.signum()<0)throw new IllegalArgumentException("Số lượng và tổng tiền không hợp lệ.");return total.divide(qty,2,RoundingMode.HALF_UP);}
}
