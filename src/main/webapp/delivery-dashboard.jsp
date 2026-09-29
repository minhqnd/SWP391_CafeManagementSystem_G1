<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Delivery Dashboard</title></head>
<body>
    <h1>Trang Giao hàng (Delivery Dashboard)</h1>
    <p>Xin chào Shipper: ${sessionScope.user.fullName}</p>
    <a href="${pageContext.request.contextPath}/login?action=logout">Đăng xuất</a>
</body>
</html>
