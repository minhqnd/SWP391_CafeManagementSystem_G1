package controller;

import dao.OrderDAO;
import dao.PaymentDAO;
import dao.ShopSettingDAO;
import model.Order;
import model.Payment;
import model.ShopSettings;
import model.User;
import service.PaymentService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "PaymentController", urlPatterns = {"/payment", "/payment/callback"})
public class PaymentController extends HttpServlet {
    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final ShopSettingDAO shopSettingDAO = new ShopSettingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/payment/callback".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        HttpSession session=request.getSession();
        User user=(User)session.getAttribute("user");
        if(!CartController.isCustomer(user)){response.sendRedirect(request.getContextPath()+"/?login=true");return;}
        int orderId=parseInt(request.getParameter("orderId"));
        Order order=orderDAO.getCustomerOrder(orderId,user.getId());
        if(order==null){session.setAttribute("ordersError","Không tìm thấy đơn hàng cần thanh toán.");response.sendRedirect(request.getContextPath()+"/orders");return;}
        if(!order.isPending()){response.sendRedirect(request.getContextPath()+"/bill?orderId="+orderId);return;}

        Payment payment=paymentDAO.ensurePendingBankTransfer(orderId,user.getId());
        if(payment==null){session.setAttribute("paymentError","Không thể khởi tạo giao dịch thanh toán.");response.sendRedirect(request.getContextPath()+"/orders");return;}
        ShopSettings settings=shopSettingDAO.getSettings();
        PaymentService.registerOrderToGateway(
                settings.getPaymentGatewayUrl(),
                "aura_cafe",
                order.getOrderCode(),
                order.getFinalAmount(),
                payment.getTransactionCode(),
                settings.getPaymentCallbackSecret()
        );
        CartController.moveFlashMessage(session,request,"paymentError");
        request.setAttribute("order",order);
        request.setAttribute("payment",payment);
        request.setAttribute("bankName",settings.getPaymentBankName());
        request.setAttribute("accountNo",settings.getPaymentAccountNo());
        request.setAttribute("accountName",settings.getPaymentAccountName());
        request.setAttribute("qrUrl",buildQrUrl(order,settings));
        request.setAttribute("storeAddress",CheckoutController.STORE_ADDRESS);
        request.setAttribute("deliveryMinutes",settings.getDeliveryMinutes());
        request.setAttribute("paymentGatewayUrl",settings.getPaymentGatewayUrl());
        request.setAttribute("expectedRewardPoints",settings.calculateEarnedPoints(order.getTotalAmount()-order.getDiscountAmount()));
        request.getRequestDispatcher("/payment.jsp").forward(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException,ServletException{
        request.setCharacterEncoding("UTF-8");
        if("/payment/callback".equals(request.getServletPath())){handleCallback(request,response);return;}
        HttpSession session=request.getSession();
        User user=(User)session.getAttribute("user");
        if(!CartController.isCustomer(user)){response.sendRedirect(request.getContextPath()+"/?login=true");return;}
        int orderId=parseInt(request.getParameter("orderId"));
        Order order=orderDAO.getCustomerOrder(orderId,user.getId());
        if(order==null){session.setAttribute("ordersError","Không tìm thấy đơn hàng cần thanh toán.");response.sendRedirect(request.getContextPath()+"/orders");return;}
        String action=trim(request.getParameter("action"));
        if("cod".equals(action)){
            if(paymentDAO.acceptCod(orderId,user.getId())){
                // Đơn đã chốt COD: lúc này mới bỏ các món khỏi giỏ hàng
                CheckoutController.removePaidItems(session,orderId);
                session.setAttribute("ordersMessage","Đã xác nhận thanh toán COD cho đơn #"+order.getOrderCode()+". Điểm sẽ được cộng sau khi giao hàng và thu tiền.");
                response.sendRedirect(request.getContextPath()+"/bill?orderId="+orderId);
            }else{
                session.setAttribute("paymentError","Không thể chọn COD cho trạng thái hiện tại của đơn.");
                response.sendRedirect(request.getContextPath()+"/payment?orderId="+orderId);
            }
            return;
        }

        // Không còn mô phỏng thanh toán: chuyển khoản QR chỉ được xác nhận qua callback của cổng thanh toán
        session.setAttribute("paymentError","Giao dịch đang chờ cổng thanh toán xác nhận tự động.");
        response.sendRedirect(request.getContextPath()+"/payment?orderId="+orderId);
    }

    private void handleCallback(HttpServletRequest request,HttpServletResponse response)throws IOException{
        // EA01: . Ở đây em nhận thông báo kết quả thanh toán từ cổng thanh toán.
        // .. Em kiểm tra chữ ký trước khi cho phép ghi nhận giao dịch.
        // ... handleCallback() gọi processCallback() [EA04] → PaymentDAO.applyGatewayCallback() [EA02].
        ShopSettings settings=shopSettingDAO.getSettings();
        PaymentDAO.CallbackResult result=processCallback(trim(request.getParameter("orderCode")),
                trim(request.getParameter("transactionCode")),trim(request.getParameter("amount")),
                trim(request.getParameter("status")),trim(request.getParameter("gatewayReference")),
                trim(request.getParameter("signature")),settings);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(result.accepted()?HttpServletResponse.SC_OK:HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("{\"accepted\":"+result.accepted()+",\"successful\":"+result.successful()
                +",\"message\":\""+escapeJson(result.message())+"\"}");
    }

    private PaymentDAO.CallbackResult processCallback(String orderCode,String transactionCode,String amountText,
                                                       String status,String gatewayReference,String signature,
                                                       ShopSettings settings){
        String canonical=PaymentService.canonical(orderCode,transactionCode,amountText,status);
        if(!PaymentService.verify(settings.getPaymentCallbackSecret(),canonical,signature)){
            return new PaymentDAO.CallbackResult(false,false,0,0,orderCode,"Invalid callback signature");
        }
        try{
            BigDecimal amount=new BigDecimal(amountText);
            String payload="{\"orderCode\":\""+escapeJson(orderCode)+"\",\"transactionCode\":\""
                    +escapeJson(transactionCode)+"\",\"amount\":\""+escapeJson(amountText)
                    +"\",\"status\":\""+escapeJson(status)+"\"}";
            // EA04: . Ở đây em chỉ chuyển kết quả thanh toán xuống phần lưu sau khi chữ ký và số tiền đọc được hợp lệ.
            // .. Phần lưu trả kết quả có chấp nhận thông báo và thanh toán có thành công không.
            // ... applyGatewayCallback(thông tin giao dịch, quy tắc tích điểm) [EA02] → kết quả về handleCallback() [EA01].
            return paymentDAO.applyGatewayCallback(orderCode,transactionCode,amount,status,gatewayReference,payload,settings.getPointsEarnUnit());
        }catch(Exception e){return new PaymentDAO.CallbackResult(false,false,0,0,orderCode,"Invalid callback amount");}
    }

    private String buildQrUrl(Order order,ShopSettings settings){
        return "https://vietqr.app/img?bank="+URLEncoder.encode(settings.getPaymentBankId(),StandardCharsets.UTF_8)
                +"&acc="+URLEncoder.encode(settings.getPaymentAccountNo(),StandardCharsets.UTF_8)
                +"&template=compact"
                +"&amount="+Math.round(order.getFinalAmount())
                +"&des="+URLEncoder.encode(order.getOrderCode(),StandardCharsets.UTF_8)
                +"&showinfo=true&fullacc=true"
                +"&holder="+URLEncoder.encode(settings.getPaymentAccountName(),StandardCharsets.UTF_8)
                +"&store="+URLEncoder.encode("AURA Cafe",StandardCharsets.UTF_8);
    }
    private int parseInt(String value){try{return Integer.parseInt(value.trim());}catch(Exception e){return -1;}}
    private String trim(String value){return value==null?"":value.trim();}
    private static String escapeJson(String value){return value==null?"":value.replace("\\","\\\\").replace("\"","\\\"");}
}
