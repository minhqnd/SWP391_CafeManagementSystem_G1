package controller;

import dao.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.*;
import service.InventoryRules;
import java.io.IOException;
import java.math.*;
import java.util.*;

@WebServlet(name="AdminPurchaseController",urlPatterns={"/admin/purchases"})
public class AdminPurchaseController extends HttpServlet {
    private final PurchaseOrderDAO dao=new PurchaseOrderDAO();
    private final PurchaseReceiptDAO receipts=new PurchaseReceiptDAO();
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException {
        if(InventoryAccess.require(r,p,true)==null)return;InventoryAccess.token(r.getSession());InventoryAccess.flash(r);
        try{
            List<PurchaseOrder> orders=dao.findAll("ALL");receipts.populateReceived(orders);r.setAttribute("purchaseOrders",orders);
            r.setAttribute("activeSuppliers",new SupplierDAO().getActiveSuppliers());r.setAttribute("allIngredients",new IngredientDAO().getActiveIngredientsWithDetails());
            r.setAttribute("approvedRequests",new RestockRequestDAO().getRestockRequests("APPROVED"));
            r.setAttribute("history",new InventoryWorkflowDAO().history("PURCHASE",null));
            r.getRequestDispatcher("/admin-purchases.jsp").forward(r,p);
        }catch(Exception e){throw new ServletException("Không tải được đơn mua.",e);}
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse p)throws IOException {
        r.setCharacterEncoding("UTF-8");User u=InventoryAccess.require(r,p,true);if(u==null||!InventoryAccess.csrf(r,p))return;
        try{
            String action=r.getParameter("action");
            if("create".equals(action)||"edit".equals(action)){
                BigDecimal qty=InventoryRules.quantity(new BigDecimal(r.getParameter("quantity")),true);
                BigDecimal total=new BigDecimal(r.getParameter("totalPrice"));
                if(total.signum()<0||total.scale()>2||total.compareTo(new BigDecimal("9999999999.99"))>0)throw new IllegalArgumentException("Tổng tiền không hợp lệ.");
                int supplier=Integer.parseInt(r.getParameter("supplierId"));
                if("edit".equals(action))dao.editDraft(Integer.parseInt(r.getParameter("id")),u.getId(),supplier,qty,total,r.getParameter("note"),r.getParameter("editReason"));
                else {
                    PurchaseOrder po=new PurchaseOrder();po.setSupplierId(supplier);po.setCreatorId(u.getId());po.setNote(InventoryRules.text(r.getParameter("note"),false));
                    String rr=r.getParameter("restockRequestId");if(rr!=null&&!rr.isBlank())po.setRestockRequestId(Integer.parseInt(rr));
                    dao.create(po,List.of(new PurchaseOrderItem(Integer.parseInt(r.getParameter("ingredientId")),qty,total.divide(qty,2,RoundingMode.HALF_UP))));
                }
            }else dao.changeStatus(Integer.parseInt(r.getParameter("id")),u.getId(),action,r.getParameter("reason"));
            r.getSession().setAttribute("workflowMessage","Đã cập nhật đơn mua.");
        }catch(Exception e){getServletContext().log("Purchase action failed",e);r.getSession().setAttribute("workflowError",e instanceof IllegalArgumentException?e.getMessage():"Không thể lưu đơn mua. Hãy kiểm tra dữ liệu hoặc đề xuất đã được sử dụng.");}
        p.sendRedirect(r.getContextPath()+"/admin/purchases");
    }
}
