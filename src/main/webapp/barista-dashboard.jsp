<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${empty requestScope.baristaDashboard}">
    <c:redirect url="/barista"/>
</c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Aura Café | Đơn pha chế</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/barista.css" rel="stylesheet">
</head>
<body data-context-path="${pageContext.request.contextPath}"
      data-csrf-token="${sessionScope.baristaCsrfToken}" data-barista-view="active">
    <jsp:include page="/components/header.jsp">
        <jsp:param name="navbarMode" value="barista"/>
        <jsp:param name="showCart" value="false"/>
        <jsp:param name="activePage" value="barista-orders"/>
    </jsp:include>
    <main class="barista-main">
        <h1>Đơn pha chế</h1>
        <p id="barista-error" class="alert alert-danger" role="alert"<c:if test="${empty requestScope.baristaInitialError}"> hidden</c:if>><c:out value="${requestScope.baristaInitialError}"/></p>
        <div class="kds-layout">
            <aside class="group-panel" aria-labelledby="group-heading">
                <h2 id="group-heading">Món đang pha</h2>
                <div id="group-list" class="group-list"></div>
            </aside>
            <section class="orders-panel" aria-labelledby="orders-heading">
                <h2 id="orders-heading">Đơn đang pha</h2>
                <div id="order-list" class="order-grid"></div>
            </section>
        </div>
    </main>
    <script id="barista-initial-data" type="application/json"><c:out value="${requestScope.baristaInitialData}" escapeXml="false" default="{}"/></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/barista.js"></script>
</body>
</html>
