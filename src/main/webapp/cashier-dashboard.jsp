<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Cashier Dashboard</title></head>
<body>
    <h1>Trang Thu ngân (Cashier Dashboard)</h1>
    <p>Xin chào Thu ngân: ${sessionScope.user.fullName}</p>
    <a href="${pageContext.request.contextPath}/login?action=logout">Đăng xuất</a>
</body>
</html>
