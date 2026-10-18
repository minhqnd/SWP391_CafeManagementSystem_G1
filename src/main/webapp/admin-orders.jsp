<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" /><c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty sessionScope.user or sessionScope.user.role ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Aura Café | Quản lý đơn hàng</title>
    <link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet"><link href="${ctx}/assets/css/admin-orders.css" rel="stylesheet">
</head>
<body class="admin-body">
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="orders" /></jsp:include>
<main class="admin-main-wrapper orders-page">
    <header class="orders-overview">
        <div><span class="table-ref">Bảng: orders · order_items · payments</span><h1>Quản lý đơn hàng</h1><p>Toàn bộ đơn tại quán, mang đi và giao hàng.</p></div>
        <div class="order-stats">
            <div class="order-stat"><span>Tổng đơn</span><strong><fmt:formatNumber value="${orderTotal}" /></strong></div>
            <div class="order-stat primary"><span>Đang xử lý</span><strong><fmt:formatNumber value="${orderProcessing}" /></strong></div>
            <div class="order-stat danger"><span>Đã hủy</span><strong><fmt:formatNumber value="${orderCancelled}" /></strong></div>
            <div class="order-stat success"><span>Doanh thu hoàn thành</span><strong><fmt:formatNumber value="${completedRevenue}" pattern="#,##0" />đ</strong></div>
        </div>
    </header>
    <c:if test="${not empty orderAdminMessage}"><div class="alert alert-success py-2 mb-0"><c:out value="${orderAdminMessage}" /></div></c:if>
    <c:if test="${not empty orderAdminError}"><div class="alert alert-danger py-2 mb-0"><c:out value="${orderAdminError}" /></div></c:if>
    <section class="order-toolbar">
        <div class="order-search"><span class="material-symbols-outlined">search</span><input id="orderSearch" type="search" placeholder="Tìm theo mã đơn, tên hoặc SĐT khách..."></div>
        <select id="orderTypeFilter"><option value="">Tất cả hình thức</option><option value="DINE_IN">Tại quán</option><option value="TAKEAWAY">Mang đi</option><option value="ONLINE">Giao hàng</option></select>
        <select id="orderStatusFilter"><option value="">Tất cả trạng thái</option><option value="PENDING">Chờ thanh toán</option><option value="CONFIRMED">Đã xác nhận</option><option value="PREPARING">Đang pha chế</option><option value="READY">Sẵn sàng</option><option value="DELIVERING">Đang giao</option><option value="COMPLETED">Hoàn thành</option><option value="CANCELLED">Đã hủy</option></select>
    </section>
    <section class="order-table-card"><div class="table-responsive"><table class="table order-table mb-0"><thead><tr><th>Mã đơn</th><th>Thời gian</th><th>Hình thức</th><th>Khách hàng</th><th>Thu ngân / Shipper</th><th class="text-end">Thành tiền</th><th>Thanh toán</th><th>Trạng thái</th><th class="text-end">Chi tiết</th></tr></thead><tbody>
    <c:forEach var="order" items="${orders}">
        <tr class="order-row ${order.status eq 'CANCELLED' ? 'cancelled' : ''}" data-search="${fn:escapeXml(fn:toLowerCase(order.orderCode))} ${fn:escapeXml(fn:toLowerCase(order.customerName))} ${fn:escapeXml(order.customerPhone)}" data-type="${order.orderType}" data-status="${order.status}">
            <td class="order-code"><c:out value="${order.orderCode}" /></td><td class="text-muted"><c:out value="${order.createdAtText}" /></td>
            <td><span class="type-badge"><c:choose><c:when test="${order.orderType eq 'DINE_IN'}">Tại quán</c:when><c:when test="${order.orderType eq 'TAKEAWAY'}">Mang đi</c:when><c:otherwise>Giao hàng</c:otherwise></c:choose></span><c:if test="${not empty order.tableNumber}"> <small>Bàn <c:out value="${order.tableNumber}" /></small></c:if></td>
            <td><strong><c:out value="${order.customerName}" /></strong><small><c:out value="${order.customerPhone}" /></small></td>
            <td><c:out value="${order.cashierName}" /><c:if test="${not empty order.shipperName}"><small>Ship: <c:out value="${order.shipperName}" /></small></c:if></td>
            <td class="text-end order-money"><fmt:formatNumber value="${order.finalAmount}" pattern="#,##0" />đ</td>
            <td><c:choose><c:when test="${empty order.paymentMethod}"><span class="text-muted">Chưa thanh toán</span></c:when><c:otherwise><strong><c:out value="${order.paymentMethod}" /></strong><small class="payment-${fn:toLowerCase(order.paymentStatus)}"><c:out value="${order.paymentStatus}" /></small></c:otherwise></c:choose></td>
            <td><span class="order-status status-${fn:toLowerCase(order.status)}"><c:out value="${order.status}" /></span></td>
            <td class="text-end"><a class="view-order" href="${ctx}/admin/orders?detailId=${order.id}">Xem</a></td>
        </tr>
    </c:forEach>
    <c:if test="${empty orders}"><tr><td colspan="9" class="text-center text-muted py-5">Chưa có đơn hàng trong database.</td></tr></c:if>
    </tbody></table></div></section>
</main>

<c:if test="${not empty detailOrder}">
<div class="modal fade" id="orderDetailModal" tabindex="-1"><div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable"><div class="modal-content order-modal">
    <div class="modal-header"><div><small>ORDER DETAIL</small><h2 class="modal-title">Đơn hàng <c:out value="${detailOrder.orderCode}" /></h2></div><a class="btn-close" href="${ctx}/admin/orders" aria-label="Đóng"></a></div>
    <div class="modal-body">
        <div class="detail-summary"><span>Hình thức: <strong><c:out value="${detailOrder.orderType}" /></strong></span><span>Thời gian: <strong><c:out value="${detailOrder.createdTimeText} ${detailOrder.createdDateText}" /></strong></span><span>Khách hàng: <strong><c:out value="${detailSummary.customerName}" /></strong></span><span>Thu ngân: <strong><c:out value="${detailSummary.cashierName}" /></strong></span><c:if test="${not empty detailOrder.tableNumber}"><span>Bàn: <strong><c:out value="${detailOrder.tableNumber}" /></strong></span></c:if><c:if test="${not empty detailOrder.deliveryAddress}"><span class="wide">Giao tới: <strong><c:out value="${detailOrder.recipientName}" /> · <c:out value="${detailOrder.recipientPhone}" /> · <c:out value="${detailOrder.deliveryAddress}" /></strong></span></c:if><c:if test="${not empty detailOrder.orderNote}"><span class="wide">Ghi chú: <c:out value="${detailOrder.orderNote}" /></span></c:if></div>
        <div class="table-responsive"><table class="table detail-table"><thead><tr><th>Món</th><th class="text-center">SL</th><th class="text-end">Đơn giá</th><th class="text-end">Thành tiền</th><th class="text-center">Đã pha</th></tr></thead><tbody><c:forEach var="item" items="${detailOrder.items}"><tr><td><strong><c:out value="${item.productName}" /></strong><small><c:out value="${item.optionsText}" /></small></td><td class="text-center"><c:out value="${item.quantity}" /></td><td class="text-end"><c:out value="${item.formattedUnitPrice}" /></td><td class="text-end fw-bold"><c:out value="${item.formattedSubtotal}" /></td><td class="text-center">${detailOrder.status eq 'READY' or detailOrder.status eq 'DELIVERING' or detailOrder.status eq 'COMPLETED' ? '✓' : '—'}</td></tr></c:forEach></tbody></table></div>
        <div class="amount-grid"><span>total_amount: <strong><c:out value="${detailOrder.formattedTotalAmount}" /></strong></span><span>discount_amount: <strong>-<c:out value="${detailOrder.formattedDiscountAmount}" /></strong></span><span>delivery_fee: <strong><c:out value="${detailOrder.formattedDeliveryFee}" /></strong></span><span>final_amount: <strong><c:out value="${detailOrder.formattedFinalAmount}" /></strong></span></div>
        <div class="order-edit-grid">
            <form method="post" action="${ctx}/admin/orders"><input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.orderAdminCsrfToken)}"><input type="hidden" name="action" value="status"><input type="hidden" name="id" value="${detailOrder.id}"><label>Trạng thái đơn (status)</label><div class="input-group"><select name="status" class="form-select"><c:forEach var="status" items="${['PENDING','CONFIRMED','PREPARING','READY','DELIVERING','COMPLETED','CANCELLED']}"><option value="${status}" ${detailOrder.status eq status ? 'selected' : ''}>${status}</option></c:forEach></select><button class="btn-order-save">Cập nhật</button></div></form>
            <c:if test="${detailOrder.orderType eq 'ONLINE'}"><form method="post" action="${ctx}/admin/orders"><input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.orderAdminCsrfToken)}"><input type="hidden" name="action" value="shipper"><input type="hidden" name="id" value="${detailOrder.id}"><label>Shipper (shipper_id)</label><div class="input-group"><select name="shipperId" class="form-select"><option value="">-- Chưa gán --</option><c:forEach var="shipper" items="${shippers}"><option value="${shipper.id}" ${detailOrder.shipperId eq shipper.id ? 'selected' : ''}><c:out value="${shipper.fullName}" /></option></c:forEach></select><button class="btn-order-save">Cập nhật</button></div></form></c:if>
        </div>
    </div>
    <div class="modal-footer"><a href="${ctx}/bill?orderId=${detailOrder.id}" class="btn-order-save"><span class="material-symbols-outlined">receipt</span>Xem hóa đơn</a></div>
</div></div></div>
</c:if>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
(() => {
    const search = document.getElementById('orderSearch'), type = document.getElementById('orderTypeFilter'), status = document.getElementById('orderStatusFilter'), rows = [...document.querySelectorAll('.order-row')];
    const filter = () => { const q = search.value.trim().toLocaleLowerCase('vi'); rows.forEach(row => row.hidden = !!((q && !row.dataset.search.includes(q)) || (type.value && row.dataset.type !== type.value) || (status.value && row.dataset.status !== status.value))); };
    [search, type, status].forEach(el => el.addEventListener(el.tagName === 'INPUT' ? 'input' : 'change', filter));
    <c:if test="${not empty detailOrder}">new bootstrap.Modal(document.getElementById('orderDetailModal')).show();</c:if>
})();
</script>
</body></html>
