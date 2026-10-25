<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<%-- Nút thao tác theo trạng thái giao hàng (UC27, UC30). Đầu vào: requestScope.card (model.Delivery) --%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="csrf" value="${fn:escapeXml(sessionScope.deliveryCsrfToken)}" />
<c:set var="d" value="${requestScope.card}" />
<c:choose>
    <c:when test="${d.assigned}">
        <form method="post" action="${ctx}/delivery" data-confirm="Nhận giao đơn #${fn:escapeXml(d.orderCode)}?">
            <input type="hidden" name="csrfToken" value="${csrf}">
            <input type="hidden" name="action" value="accept">
            <input type="hidden" name="id" value="${d.id}">
            <button type="submit" class="primary"><span class="material-symbols-outlined">assignment_turned_in</span>Nhận đơn</button>
        </form>
    </c:when>
    <c:when test="${d.accepted}">
        <c:if test="${not d.orderReady}"><span class="hint">Quán đang pha chế, chờ "Sẵn sàng" để lấy hàng</span></c:if>
        <form method="post" action="${ctx}/delivery" data-confirm="Đã lấy hàng đơn #${fn:escapeXml(d.orderCode)} và bắt đầu giao?">
            <input type="hidden" name="csrfToken" value="${csrf}">
            <input type="hidden" name="action" value="pickup">
            <input type="hidden" name="id" value="${d.id}">
            <button type="submit" class="primary" ${d.orderReady ? '' : 'disabled'}>
                <span class="material-symbols-outlined">two_wheeler</span>Lấy hàng &amp; bắt đầu giao
            </button>
        </form>
    </c:when>
    <c:when test="${d.delivering}">
        <form method="post" action="${ctx}/delivery" class="deliver-form"
              data-confirm="Xác nhận đã giao thành công đơn #${fn:escapeXml(d.orderCode)}?">
            <input type="hidden" name="csrfToken" value="${csrf}">
            <input type="hidden" name="action" value="deliver">
            <input type="hidden" name="id" value="${d.id}">
            <c:if test="${d.cod}">
                <label class="hint" for="cod-${d.id}">Tiền đã thu</label>
                <input type="number" class="form-control form-control-sm" id="cod-${d.id}" name="codCollected"
                       min="0" step="1000" required placeholder="<fmt:formatNumber value='${d.codAmount}' pattern='#,##0' />">
            </c:if>
            <button type="submit" class="primary">
                <span class="material-symbols-outlined">task_alt</span>
                Giao thành công<c:if test="${d.cod}"> &amp; thu <fmt:formatNumber value="${d.codAmount}" pattern="#,##0" />đ</c:if>
            </button>
        </form>
    </c:when>
    <c:when test="${d.delivered}">
        <span class="badge-st st-DELIVERED">Đã giao lúc ${d.deliveredAtDisplay}</span>
    </c:when>
</c:choose>
