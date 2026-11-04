package controller;

import dao.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@WebServlet(name="PurchaseReceiptController",urlPatterns={"/admin/purchase-receipts","/barista/purchase-receipts"})
public class PurchaseReceiptController extends HttpServlet {
    private final PurchaseReceiptDAO dao=new PurchaseReceiptDAO();
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException {
        User u=InventoryAccess.require(r,p,r.getServletPath().startsWith("/admin/"));if(u==null)return;
        boolean admin="ADMIN".equals(u.getRole());InventoryAccess.token(r.getSession());InventoryAccess.flash(r);
        try{
            List<PurchaseOrder> orders=new PurchaseOrderDAO().findAll("ALL");dao.populateReceived(orders);
            r.setAttribute("purchaseOrders",orders.stream().filter(po->List.of("ORDERED","PARTIALLY_RECEIVED").contains(po.getStatus())).toList());
            r.setAttribute("receipts",dao.findAll().stream().filter(receipt->admin||receipt.getReceiverId()==u.getId()).toList());
            r.setAttribute("workflowAdmin",admin);r.setAttribute("submissionKey",UUID.randomUUID().toString());
            r.setAttribute("history",admin?new InventoryWorkflowDAO().history("PURCHASE",null):List.of());
            r.getRequestDispatcher("/purchase-receipts.jsp").forward(r,p);
        }catch(Exception e){throw new ServletException("Không tải được phiếu kiểm nhận.",e);}
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse p)throws IOException {
        r.setCharacterEncoding("UTF-8");User u=InventoryAccess.require(r,p,r.getServletPath().startsWith("/admin/"));if(u==null||!InventoryAccess.csrf(r,p))return;
        boolean admin="ADMIN".equals(u.getRole());String action=r.getParameter("action");
        if(!"receive".equals(action)&&!admin){p.sendError(403);return;}
        try{
            if("receive".equals(action)){
                String[] itemIds=r.getParameterValues("itemId");if(itemIds==null)throw new IllegalArgumentException("Thiếu nguyên liệu kiểm nhận.");
                List<PurchaseReceipt.Item> items=new ArrayList<>();
                for(String value:itemIds){int item=Integer.parseInt(value);items.add(new PurchaseReceipt.Item(item,new BigDecimal(r.getParameter("delivered_"+item)),new BigDecimal(r.getParameter("accepted_"+item))));}
                dao.submit(Integer.parseInt(r.getParameter("poId")),u.getId(),r.getParameter("submissionKey"),r.getParameter("note"),items);
            }else if("approve".equals(action)||"reject".equals(action))dao.review(Integer.parseInt(r.getParameter("id")),u.getId(),"approve".equals(action),r.getParameter("reviewNote"),"yes".equals(r.getParameter("acceptExcess")));
            else if("adjust".equals(action))dao.adjust(Integer.parseInt(r.getParameter("receiptItemId")),u.getId(),r.getParameter("submissionKey"),new BigDecimal(r.getParameter("delta")),r.getParameter("reason"));
            else throw new IllegalArgumentException("Thao tác không hợp lệ.");
            r.getSession().setAttribute("workflowMessage","Đã cập nhật phiếu kiểm nhận.");
        }catch(Exception e){getServletContext().log("Receipt action failed",e);r.getSession().setAttribute("workflowError",e instanceof IllegalArgumentException?e.getMessage():"Không thể xử lý phiếu. Hãy tải lại và kiểm tra dữ liệu.");}
        p.sendRedirect(r.getContextPath()+(admin?"/admin/purchase-receipts":"/barista/purchase-receipts"));
    }
}
