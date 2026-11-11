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
                int supplier=Integer.parseInt(r.getParameter("supplierId"));
                if("edit".equals(action)) {
                    BigDecimal qty=InventoryRules.quantity(new BigDecimal(r.getParameter("quantity")),true);
                    BigDecimal total=InventoryRules.money(new BigDecimal(r.getParameter("totalPrice")));
                    dao.editDraft(Integer.parseInt(r.getParameter("id")),u.getId(),supplier,qty,total,r.getParameter("note"),r.getParameter("editReason"));
                }
                else {
                    PurchaseOrder po=new PurchaseOrder();po.setSupplierId(supplier);po.setCreatorId(u.getId());po.setNote(InventoryRules.text(r.getParameter("note"),false));
                    String rr=r.getParameter("restockRequestId");if(rr!=null&&!rr.isBlank())po.setRestockRequestId(Integer.parseInt(rr));
                    dao.create(po,parseItems(r));
                }
            }else dao.changeStatus(Integer.parseInt(r.getParameter("id")),u.getId(),action,r.getParameter("reason"));
            r.getSession().setAttribute("workflowMessage","Đã cập nhật đơn mua.");
        }catch(Exception e){getServletContext().log("Purchase action failed",e);r.getSession().setAttribute("workflowError",e instanceof IllegalArgumentException?e.getMessage():"Không thể lưu đơn mua. Hãy kiểm tra dữ liệu hoặc đề xuất đã được sử dụng.");}
        p.sendRedirect(r.getContextPath()+"/admin/purchases");
    }
    static List<PurchaseOrderItem> parseItems(HttpServletRequest request) {
        String[] ids=request.getParameterValues("ingredientId");
        String[] quantities=request.getParameterValues("quantity");
        String[] totals=request.getParameterValues("totalPrice");
        if(ids==null || quantities==null || totals==null || ids.length==0 || ids.length>100
                || ids.length!=quantities.length || ids.length!=totals.length)
            throw new IllegalArgumentException("Danh sách nguyên liệu, số lượng và tiền lô không khớp.");
        List<PurchaseOrderItem> items=new ArrayList<>();
        Set<Integer> seen=new HashSet<>();
        BigDecimal grandTotal=BigDecimal.ZERO;
        try {
            for(int i=0;i<ids.length;i++) {
                int id=Integer.parseInt(ids[i]);
                if(id<=0 || !seen.add(id))throw new IllegalArgumentException("Nguyên liệu không hợp lệ hoặc bị trùng.");
                BigDecimal quantity=InventoryRules.quantity(new BigDecimal(quantities[i]),true);
                BigDecimal total=InventoryRules.money(new BigDecimal(totals[i]));
                grandTotal=grandTotal.add(total);
                items.add(new PurchaseOrderItem(id,quantity,InventoryRules.purchaseUnitPrice(quantity,total)));
            }
        } catch(NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Số lượng hoặc tiền lô không hợp lệ.",e);
        }
        InventoryRules.money(grandTotal);
        return items;
    }
}
