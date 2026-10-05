<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Admin Dashboard</title></head>
<body>
    <h1>Trang quản lý (Admin Dashboard)</h1>
    <p>Xin chào Admin: ${sessionScope.user.fullName}</p>
    <a href="${pageContext.request.contextPath}/login?action=logout">Đăng xuất (Về trang Login)</a>
</body>
</html>
