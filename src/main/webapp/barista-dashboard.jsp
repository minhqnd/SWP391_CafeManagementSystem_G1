<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Barista Dashboard</title></head>
<body>
    <h1>Trang Pha chế (Barista Dashboard)</h1>
    <p>Xin chào Barista: ${sessionScope.user.fullName}</p>
    <a href="${pageContext.request.contextPath}/login?action=logout">Đăng xuất</a>
</body>
</html>
