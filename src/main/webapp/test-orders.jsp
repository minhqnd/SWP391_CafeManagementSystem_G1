<%@ page import="dao.OrderDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Order" %>
<%@ page import="java.util.Map" %>
<%@ page import="controller.BaristaController" %>
<%@ page import="java.lang.reflect.Method" %>
<%
    try {
        OrderDAO dao = new OrderDAO();
        List<Order> orders = dao.getOrdersForBarista();
        
        Method m = BaristaController.class.getDeclaredMethod("dashboardData", List.class);
        m.setAccessible(true);
        Map<String, Object> data = (Map<String, Object>) m.invoke(null, orders);
        
        out.println("SUCCESS. Dashboard data categories count: " + ((List)data.get("categories")).size());
    } catch (Exception e) {
        out.println("ERROR: " + e.getMessage());
        e.printStackTrace(new java.io.PrintWriter(out));
    }
%>
