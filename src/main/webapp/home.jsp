<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Customer Home</title></head>
<body>
    <h1>Trang Khách hàng (Home)</h1>
    <p>Xin chào Quý khách: ${sessionScope.user.fullName}</p>
    <a href="${pageContext.request.contextPath}/login?action=logout">Đăng xuất</a>
</body>
</html>
