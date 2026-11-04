<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="errorException" value="${requestScope['jakarta.servlet.error.exception']}" />
<c:set var="errorMessage" value="${requestScope['jakarta.servlet.error.message']}" />
<%
    Throwable rootCause = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
    while (rootCause != null && rootCause.getCause() != null && rootCause.getCause() != rootCause) {
        rootCause = rootCause.getCause();
    }
    request.setAttribute("errorRootCause", rootCause);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Không thể mở trang</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { min-height:100vh; display:grid; place-items:center; margin:0; padding:24px; background:#f8fafc; color:#1e293b; font-family:system-ui,sans-serif; }
        .error-card { width:min(100%,520px); padding:32px; border:1px solid #e2e8f0; border-radius:18px; background:#fff; box-shadow:0 12px 32px rgba(15,23,42,.07); text-align:center; }
        .error-code { color:#a30d20; font-size:14px; font-weight:800; letter-spacing:.12em; }
    </style>
</head>
<body>
<main class="error-card">
    <div class="error-code">LỖI <c:out value="${pageContext.errorData.statusCode}" /></div>
    <h1 class="h4 fw-bold mt-2">Không thể mở trang này</h1>
    <p class="text-secondary mb-4">Trang đang gặp lỗi hoặc đường dẫn không tồn tại. Hãy quay lại trang quản trị và thử mở lại chức năng.</p>
    <c:if test="${not empty errorException or not empty errorMessage}">
        <details class="text-start small mb-4">
            <summary class="text-secondary">Chi tiết lỗi để sửa</summary>
            <pre class="mt-2 mb-0 p-3 rounded bg-light text-wrap"><c:out value="${errorRootCause}" /><c:if test="${errorMessage ne errorRootCause.message and not empty errorMessage}">
Request: <c:out value="${errorMessage}" /></c:if><c:if test="${empty errorRootCause and not empty errorMessage}"><c:out value="${errorMessage}" /></c:if></pre>
        </details>
    </c:if>
    <div class="d-flex justify-content-center flex-wrap gap-2">
        <a class="btn btn-danger" href="${ctx}/admin-dashboard.jsp">Về trang quản trị</a>
        <a class="btn btn-outline-secondary" href="${ctx}/">Về trang chủ</a>
    </div>
</main>
</body>
</html>
