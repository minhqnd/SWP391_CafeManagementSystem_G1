<%@ page import="dao.UserDAO" %>
<%@ page import="model.User" %>
<%
    UserDAO dao = new UserDAO();
    User u = dao.getUserById(2); // Assuming ID 2 is a Barista, or I'll just create a dummy Barista User
    if (u == null) {
        u = new User();
        u.setId(999);
        u.setRole("STAFF");
        u.setPosition("BARISTA");
        u.setStatus("ACTIVE");
    }
    session.setAttribute("user", u);
    out.println("Session initialized with Barista.");
%>
