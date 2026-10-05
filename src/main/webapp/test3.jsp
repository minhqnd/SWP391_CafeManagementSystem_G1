<%@ page import="dao.OrderDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Order" %>
<%
    try {
        OrderDAO dao = new OrderDAO();
        List<Order> orders = dao.getOrdersForBarista();
        out.println("Orders count: " + orders.size());
        for (Order o : orders) {
            out.println("ID: " + o.getId() + ", Status: " + o.getStatus() + ", Items: " + o.getItems().size());
        }
    } catch (Exception e) {
        out.println("ERROR: " + e.getMessage());
    }
%>
