<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    // Legacy development URL: never replace the authenticated session user.
    // The normal login controller keeps an existing user on the correct dashboard
    // and shows the login form when no authenticated session exists.
    response.sendRedirect(request.getContextPath() + "/login");
%>
