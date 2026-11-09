<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- Trang chỉ hiển thị qua CounterOrderListController (/counter-orders) --%>
<c:if test="${empty statusCounts}"><c:redirect url="/counter-orders" /></c:if>
<c:url var="baseUrl" value="/counter-orders"><c:param name="date" value="${date}" /><c:param name="type" value="${typeFilter}" /><c:param name="q" value="${searchQuery}" /></c:url>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đơn tại quầy | Aura Café</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <link href="${ctx}/assets/css/counter-orders.css" rel="stylesheet">
</head>
<body>
    <c:choose>
        <c:when test="${isBarista}">
            <header class="co-top">
                <a class="co-brand" href="${ctx}/barista">Aura Café · <span class="muted" style="font-size:14px"><c:out value="${sessionScope.user.fullName}" /></span></a>
                <nav class="co-nav">
                    <a href="${ctx}/barista"><span class="material-symbols-outlined">coffee_maker</span>Đơn pha chế</a>
                    <a href="${ctx}/barista?action=completed"><span class="material-symbols-outlined">task_alt</span>Đơn đã pha</a>
                    <a class="active" href="${ctx}/counter-orders"><span class="material-symbols-outlined">receipt_long</span>Đơn tại quầy</a>
                    <a href="${ctx}/login?action=logout"><span class="material-symbols-outlined">logout</span>Đăng xuất</a>
                </nav>
            </header>
        </c:when>
        <c:otherwise>
            <%-- Thu ngân: cùng thanh header với màn Bán tại quầy / Chốt ca --%>
            <jsp:include page="/components/cashier-header.jsp">
                <jsp:param name="active" value="counter" />
            </jsp:include>
        </c:otherwise>
    </c:choose>

    <main class="co-page">
        <section class="panel">
            <h1>Tra cứu đơn tại quầy</h1>
            <p class="muted" style="margin:0">Các đơn Tại quán / Mang đi do Thu ngân tạo ngày <strong>${dateDisplay}</strong>. Chọn ngày và bộ lọc để tra cứu lịch sử đơn hàng.</p>

            <div class="filters">
                <c:forEach var="entry" items="${statusCounts}">
                    <c:set var="label">
                        <c:choose>
                            <c:when test="${entry.key eq 'ALL'}">Tất cả</c:when>
                            <c:when test="${entry.key eq 'PENDING'}">Chờ thanh toán</c:when>
                            <c:when test="${entry.key eq 'CONFIRMED'}">Chờ pha</c:when>
                            <c:when test="${entry.key eq 'PREPARING'}">Đang pha</c:when>
                            <c:when test="${entry.key eq 'READY'}">Sẵn sàng</c:when>
                            <c:when test="${entry.key eq 'COMPLETED'}">Đã giao khách</c:when>
                            <c:otherwise>Đã huỷ</c:otherwise>
                        </c:choose>
                    </c:set>
                    <a class="chip ${statusFilter eq entry.key ? 'active' : ''}" href="<c:out value="${baseUrl}"/>&amp;status=${entry.key}">${label} <span>${entry.value}</span></a>
                </c:forEach>
            </div>

            <form class="filters" method="get" action="${ctx}/counter-orders">
                <input type="hidden" name="status" value="${statusFilter}">
                <label class="muted">Ngày <input class="filter-input" type="date" name="date" value="${date}"></label>
                <label class="muted">Hình thức
                    <select class="filter-input" name="type">
                        <option value="ALL" ${typeFilter eq 'ALL' ? 'selected' : ''}>Tất cả</option>
                        <option value="DINE_IN" ${typeFilter eq 'DINE_IN' ? 'selected' : ''}>Tại quán</option>
                        <option value="TAKEAWAY" ${typeFilter eq 'TAKEAWAY' ? 'selected' : ''}>Mang đi</option>
                    </select>
                </label>
                <input class="filter-input" type="search" id="orderSearch" name="q" value="<c:out value="${searchQuery}"/>" placeholder="Tìm mã đơn / số bàn" aria-label="Tìm mã đơn hoặc số bàn">
                <button class="chip active" type="submit" style="cursor:pointer">Xem</button>
                <c:if test="${not isToday}"><a class="chip" href="${ctx}/counter-orders">Về hôm nay</a></c:if>
                <c:if test="${isToday}"><span class="muted" style="font-size:12px">Tự làm mới mỗi 30 giây</span></c:if>
            </form>
        </section>

        <c:choose>
            <c:when test="${empty orders}">
                <section class="panel empty" style="margin-top:16px">Không có đơn tại quầy nào phù hợp.</section>
            </c:when>
            <c:otherwise>
                <div class="grid">
                    <c:forEach var="o" items="${orders}">
                        <article class="order st-${o.status}">
                            <div class="order-head">
                                <div>
                                    <span class="code">#<c:out value="${o.orderCode}" /></span>
                                    <div class="meta" style="margin-top:6px">
                                        <span class="type-tag">
                                            <span class="material-symbols-outlined" style="font-size:15px">${o.orderType eq 'DINE_IN' ? 'table_restaurant' : 'takeout_dining'}</span>
                                            ${o.orderType eq 'DINE_IN' ? 'Tại quán' : 'Mang đi'}
                                            <c:if test="${not empty o.tableNumber}"> · Bàn <c:out value="${o.tableNumber}" /></c:if>
                                        </span>
                                        · ${o.createdTimeText}
                                    </div>
                                </div>
                                <span class="badge-st b-${o.status}">
                                    <c:choose>
                                        <c:when test="${o.status eq 'PENDING'}">Chờ thanh toán</c:when>
                                        <c:when test="${o.status eq 'CONFIRMED'}">Chờ pha</c:when>
                                        <c:when test="${o.status eq 'PREPARING'}">Đang pha</c:when>
                                        <c:when test="${o.status eq 'READY'}">Sẵn sàng</c:when>
                                        <c:when test="${o.status eq 'COMPLETED'}">Đã giao khách</c:when>
                                        <c:when test="${o.status eq 'CANCELLED'}">Đã huỷ</c:when>
                                        <c:otherwise>${o.status}</c:otherwise>
                                    </c:choose>
                                </span>
                            </div>
                            <c:if test="${not empty o.recipientName}"><div class="meta">Khách: <c:out value="${o.recipientName}" /></div></c:if>
                            <div class="items">
                                <c:forEach var="it" items="${o.items}">
                                    <div style="display:flex;justify-content:space-between;gap:8px">
                                        <span><strong>${it.quantity}x</strong> <c:out value="${it.productName}" /></span>
                                        <span class="meta"><c:out value="${it.optionsText}" /></span>
                                    </div>
                                </c:forEach>
                            </div>
                            <c:if test="${not empty o.orderNote}"><div class="note">Ghi chú: <c:out value="${o.orderNote}" /></div></c:if>
                            <div class="order-foot">
                                <span class="meta">${o.paymentMethodLabel}</span>
                                <span class="total"><fmt:formatNumber value="${o.finalAmount}" pattern="#,##0" />đ</span>
                            </div>
                        </article>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </main>

    <script>
        (() => {
            // Giữ nguyên bộ lọc trên URL; tránh làm mới khi đang nhập bộ lọc.
            <c:if test="${isToday}">
            setInterval(() => { if (document.activeElement && document.activeElement.closest("form")) return; location.reload(); }, 30000);
            </c:if>
        })();
    </script>
</body>
</html>
