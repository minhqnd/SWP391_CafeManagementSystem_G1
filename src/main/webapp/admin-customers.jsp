<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty sessionScope.user or sessionScope.user.role ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Aura Café | Quản lý khách hàng</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-customers.css" rel="stylesheet">
</head>
<body class="admin-body">
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="customers" /></jsp:include>

<main class="admin-main-wrapper customers-page">
    <header class="customer-overview">
        <div class="customer-heading">
            <div>
                <h1>Quản lý khách hàng</h1>
                <p>Thông tin khách hàng, điểm tích lũy và tổng chi tiêu từ đơn hàng đã hoàn thành.</p>
            </div>
            <button class="btn-customer-primary" data-bs-toggle="modal" data-bs-target="#customerModal">
                <span class="material-symbols-outlined">person_add</span>Thêm khách hàng
            </button>
        </div>
        <div class="customer-stats">
            <div class="customer-stat"><span>Tổng khách hàng</span><strong><fmt:formatNumber value="${customerTotal}" /></strong></div>
            <div class="customer-stat success"><span>Đang hoạt động</span><strong><fmt:formatNumber value="${customerActive}" /></strong></div>
            <div class="customer-stat primary"><span>Tổng điểm lưu hành</span><strong><fmt:formatNumber value="${pointsTotal}" /></strong></div>
            <div class="customer-stat"><span>Doanh thu từ thành viên</span><strong><fmt:formatNumber value="${memberRevenue}" pattern="#,##0" />đ</strong></div>
        </div>
    </header>

    <c:if test="${not empty customerMessage}"><div class="alert alert-success py-2 mb-0"><c:out value="${customerMessage}" /></div></c:if>
    <c:if test="${not empty customerError}"><div class="alert alert-danger py-2 mb-0"><c:out value="${customerError}" /></div></c:if>

    <section class="customer-toolbar">
        <div class="customer-search">
            <span class="material-symbols-outlined">search</span>
            <input id="customerSearch" type="search" placeholder="Tìm theo tên, số điện thoại hoặc email..." autocomplete="off">
        </div>
        <span id="customerResultCount" class="result-count"><c:out value="${customerTotal}" /> khách hàng</span>
    </section>

    <section class="customer-table-card">
        <div class="table-responsive">
            <table class="table customer-table mb-0">
                <thead><tr><th>Mã KH</th><th>Khách hàng</th><th>Số điện thoại</th><th>Email</th><th>Ngày sinh</th><th class="text-center">Điểm</th><th class="text-center">Số đơn</th><th class="text-end">Tổng chi tiêu</th><th>Ngày tạo</th><th class="text-center">Trạng thái</th><th class="text-end">Thao tác</th></tr></thead>
                <tbody id="customerTableBody">
                <c:forEach var="customer" items="${customers}">
                    <tr class="customer-row" data-search="${fn:escapeXml(fn:toLowerCase(customer.fullName))} ${fn:escapeXml(customer.phone)} ${fn:escapeXml(fn:toLowerCase(customer.email))}">
                        <td class="customer-code"><c:out value="${customer.customerCode}" /></td>
                        <td><strong><c:out value="${customer.fullName}" /></strong><c:if test="${not empty customer.address}"><small><c:out value="${customer.address}" /></small></c:if></td>
                        <td class="font-monospace"><c:out value="${customer.phone}" /></td>
                        <td class="text-muted"><c:out value="${empty customer.email ? '—' : customer.email}" /></td>
                        <td><c:out value="${customer.birthdayText}" /></td>
                        <td class="text-center points"><fmt:formatNumber value="${customer.totalPoints}" /></td>
                        <td class="text-center font-monospace"><c:out value="${customer.orderCount}" /></td>
                        <td class="text-end money"><fmt:formatNumber value="${customer.totalSpent}" pattern="#,##0" />đ</td>
                        <td><c:out value="${customer.createdAtText}" /></td>
                        <td class="text-center"><c:choose><c:when test="${customer.status eq 'ACTIVE'}"><span class="status-badge active">Hoạt động</span></c:when><c:otherwise><span class="status-badge locked">Đã khóa</span></c:otherwise></c:choose></td>
                        <td class="text-end text-nowrap">
                            <form method="post" action="${ctx}/admin/customers" class="d-inline" onsubmit="return confirm('Xác nhận thay đổi trạng thái khách hàng này?')">
                                <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.customerCsrfToken)}">
                                <input type="hidden" name="action" value="toggle"><input type="hidden" name="id" value="${customer.id}">
                                <input type="hidden" name="status" value="${customer.status eq 'LOCKED' ? 'ACTIVE' : 'LOCKED'}">
                                <button class="customer-action ${customer.status eq 'LOCKED' ? 'unlock' : 'lock'}">${customer.status eq 'LOCKED' ? 'Mở khóa' : 'Khóa'}</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty customers}"><tr><td colspan="11" class="text-center text-muted py-5">Chưa có khách hàng trong database.</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </section>
</main>

<div class="modal fade" id="customerModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered"><div class="modal-content customer-modal"><form method="post" action="${ctx}/admin/customers">
        <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.customerCsrfToken)}"><input type="hidden" name="action" value="create">
        <div class="modal-header"><div><small>USERS · CUSTOMER</small><h2 class="modal-title">Thêm khách hàng</h2></div><button class="btn-close" type="button" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <div class="customer-field"><label for="fullName">Họ và tên *</label><input id="fullName" name="fullName" required maxlength="120"></div>
            <div class="row g-3"><div class="col-sm-6 customer-field"><label for="phone">Số điện thoại *</label><input id="phone" name="phone" type="tel" required maxlength="10" pattern="[0-9]{10}" inputmode="numeric" title="Nhập đúng 10 chữ số"></div><div class="col-sm-6 customer-field"><label for="birthday">Ngày sinh</label><input id="birthday" name="birthday" type="date"></div></div>
            <div class="customer-field"><label for="email">Email</label><input id="email" name="email" type="email" maxlength="150"></div>
            <div class="customer-field"><label for="address">Địa chỉ</label><input id="address" name="address"></div>
            <div class="customer-field"><label for="initialPoints">Điểm tặng khởi tạo</label><input id="initialPoints" name="initialPoints" type="number" min="0" value="0"><small>Được ghi vào loyalty_transactions với loại EARN.</small></div>
        </div>
        <div class="modal-footer"><button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button><button class="btn-customer-primary" type="submit">Lưu khách hàng</button></div>
    </form></div></div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
(() => {
    const input = document.getElementById('customerSearch');
    const rows = [...document.querySelectorAll('.customer-row')];
    const count = document.getElementById('customerResultCount');
    input.addEventListener('input', () => {
        const query = input.value.trim().toLocaleLowerCase('vi');
        let visible = 0;
        rows.forEach(row => { const show = !query || row.dataset.search.includes(query); row.hidden = !show; if (show) visible++; });
        count.textContent = visible + ' khách hàng';
    });
})();
</script>
</body>
</html>
