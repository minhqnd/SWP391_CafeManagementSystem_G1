package controller;

import dao.DeliveryDAO;
import dao.ShopSettingDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.DeliveryIncident;
import model.Order;
import model.User;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet(name="DeliveryController",urlPatterns={"/delivery"})
public class DeliveryController extends HttpServlet{
    private final DeliveryDAO deliveryDAO=new DeliveryDAO();
    private final ShopSettingDAO settingDAO=new ShopSettingDAO();

    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException{
        User user=requireShipper(request,response);if(user==null)return;
        moveFlash(request,"deliveryMessage");moveFlash(request,"deliveryError");
        try{
            List<Order> orders=deliveryDAO.findAssignedOrders(user.getId());
            request.setAttribute("deliveryOrders",orders);
            request.setAttribute("codRecords",deliveryDAO.findCodRecords(user.getId()));
            request.setAttribute("incidents",deliveryDAO.findIncidents(user.getId()));
            request.getRequestDispatcher("/delivery-dashboard.jsp").forward(request,response);
        }catch(Exception e){getServletContext().log("Cannot load delivery dashboard",e);response.sendError(500,"Không tải được dữ liệu giao hàng.");}
    }

    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{
        request.setCharacterEncoding("UTF-8");
        User user=requireShipper(request,response);if(user==null)return;
        HttpSession session=request.getSession();
        int orderId=parseInt(request.getParameter("orderId"));String action=trim(request.getParameter("action"));
        try{
            boolean ok;
            switch(action){
                case "claim": ok=deliveryDAO.claimOrder(orderId,user.getId());break;
                case "start": ok=deliveryDAO.markDelivering(orderId,user.getId());break;
                case "complete":
                    BigDecimal amount=parseMoney(request.getParameter("collectedAmount"));
                    ok=deliveryDAO.completeDelivery(orderId,user.getId(),amount,settingDAO.getSettings().getPointsEarnUnit());break;
                case "incident":
                    String type=trim(request.getParameter("incidentType"));
                    String description=trim(request.getParameter("description"));
                    if(!List.of("CUSTOMER_UNAVAILABLE","WRONG_ADDRESS","DAMAGED","PAYMENT_PROBLEM","OTHER").contains(type)
                            || description.length()<5 || description.length()>1000)throw new IllegalArgumentException("Thông tin sự cố không hợp lệ.");
                    ok=deliveryDAO.reportIncident(new DeliveryIncident(0,orderId,user.getId(),type,description,"OPEN",LocalDateTime.now()))>0;break;
                default:throw new IllegalArgumentException("Thao tác giao hàng không được hỗ trợ.");
            }
            if(ok)session.setAttribute("deliveryMessage","Đã cập nhật đơn hàng thành công.");
            else session.setAttribute("deliveryError","Không thể cập nhật vì đơn đã đổi trạng thái, chưa được nhận hoặc số tiền COD không khớp.");
        }catch(Exception e){getServletContext().log("Delivery action failed",e);session.setAttribute("deliveryError",e.getMessage());}
        response.sendRedirect(request.getContextPath()+"/delivery");
    }

    private User requireShipper(HttpServletRequest request,HttpServletResponse response)throws IOException{
        User user=(User)request.getSession().getAttribute("user");
        if(user==null){response.sendRedirect(request.getContextPath()+"/?tab=login");return null;}
        if(!"STAFF".equalsIgnoreCase(user.getRole())||!"SHIPPER".equalsIgnoreCase(user.getPosition())
                ||!"ACTIVE".equalsIgnoreCase(user.getStatus())){response.sendError(403,"Chỉ nhân viên giao hàng đang hoạt động được truy cập.");return null;}
        return user;
    }
    private void moveFlash(HttpServletRequest r,String key){Object v=r.getSession().getAttribute(key);if(v!=null){r.setAttribute(key,v);r.getSession().removeAttribute(key);}}
    private int parseInt(String v){try{return Integer.parseInt(v);}catch(Exception e){return -1;}}
    private BigDecimal parseMoney(String v){try{return new BigDecimal(v.replace(".","").replace(",","").trim());}catch(Exception e){return null;}}
    private String trim(String v){return v==null?"":v.trim();}
}
