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
    <title>Aura Café | Quản lý giao dịch thanh toán</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-orders.css?v=2" rel="stylesheet">
    <style>
        .badge-qr {
            background-color: #e0f2fe;
            color: #0369a1;
            font-weight: 600;
            padding: 4px 8px;
            border-radius: 6px;
            font-size: 0.8rem;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }
        .badge-cash {
            background-color: #fef3c7;
            color: #b45309;
            font-weight: 600;
            padding: 4px 8px;
            border-radius: 6px;
            font-size: 0.8rem;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }
        .gateway-ref {
            font-family: monospace;
            background: #f1f5f9;
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 0.8rem;
            color: #475569;
        }
    </style>
</head>
<body class="admin-body">
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="payments" /></jsp:include>
<main class="admin-main-wrapper orders-page">
    <header class="orders-overview">
        <div>
            <span class="table-ref">Bảng: payments · orders · SePay Gateway</span>
            <h1>Lịch sử &amp; Đối soát thanh toán</h1>
            <p>Tổng hợp toàn bộ giao dịch chuyển khoản ngân hàng (VietQR/SePay) và thanh toán tiền mặt.</p>
        </div>
        <div class="order-stats">
            <div class="order-stat">
                <span>Tổng giao dịch</span>
                <strong><fmt:formatNumber value="${totalCount}" /></strong>
            </div>
            <div class="order-stat success">
                <span>Đã thu thành công</span>
                <strong><fmt:formatNumber value="${successRevenue}" pattern="#,##0" />đ</strong>
            </div>
            <div class="order-stat primary">
                <span>Chuyển khoản QR</span>
                <strong><fmt:formatNumber value="${bankTransferCount}" /></strong>
            </div>
            <div class="order-stat">
                <span>Tiền mặt</span>
                <strong><fmt:formatNumber value="${cashCount}" /></strong>
            </div>
        </div>
    </header>

    <!-- Bộ lọc & Tìm kiếm -->
    <section class="order-toolbar">
        <form method="get" action="${ctx}/admin/payments" class="d-flex align-items-center gap-2 flex-grow-1 flex-wrap">
            <div class="order-search flex-grow-1">
                <span class="material-symbols-outlined">search</span>
                <input name="search" type="search" value="${fn:escapeXml(currentSearch)}" placeholder="Tìm mã giao dịch, mã đơn, tên/SĐT khách, mã ngân hàng...">
            </div>
            <select name="method" onchange="this.form.submit()">
                <option value="ALL" ${currentMethod eq 'ALL' ? 'selected' : ''}>Tất cả phương thức</option>
                <option value="BANK_TRANSFER" ${currentMethod eq 'BANK_TRANSFER' ? 'selected' : ''}>Chuyển khoản VietQR</option>
                <option value="CASH" ${currentMethod eq 'CASH' ? 'selected' : ''}>Tiền mặt (POS/COD)</option>
            </select>
            <select name="status" onchange="this.form.submit()">
                <option value="ALL" ${currentStatus eq 'ALL' ? 'selected' : ''}>Tất cả trạng thái</option>
                <option value="SUCCESS" ${currentStatus eq 'SUCCESS' ? 'selected' : ''}>Thành công (SUCCESS)</option>
                <option value="PENDING" ${currentStatus eq 'PENDING' ? 'selected' : ''}>Chờ thanh toán (PENDING)</option>
                <option value="FAILED" ${currentStatus eq 'FAILED' ? 'selected' : ''}>Thất bại (FAILED)</option>
            </select>
            <button type="submit" class="btn btn-dark btn-sm px-3" style="height: 38px; border-radius: 8px;">Lọc</button>
            <c:if test="${not empty currentSearch or currentMethod ne 'ALL' or currentStatus ne 'ALL'}">
                <a href="${ctx}/admin/payments" class="btn btn-outline-secondary btn-sm px-2" style="height: 38px; line-height: 24px; border-radius: 8px;">Xóa lọc</a>
            </c:if>
        </form>
    </section>

    <!-- Bảng danh sách giao dịch -->
    <section class="order-table-card">
        <div class="table-responsive">
            <table class="table order-table mb-0">
                <thead>
                    <tr>
                        <th>Mã giao dịch</th>
                        <th>Mã đơn hàng</th>
                        <th>Khách hàng</th>
                        <th>Phương thức</th>
                        <th class="text-end">Số tiền</th>
                        <th>Mã đối soát Ngân hàng</th>
                        <th>Thời gian</th>
                        <th>Trạng thái</th>
                        <th class="text-end">Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${empty payments}">
                            <tr>
                                <td colspan="9" class="text-center text-muted py-4">
                                    Không tìm thấy giao dịch nào phù hợp với điều kiện tìm kiếm.
                                </td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="p" items="${payments}">
                                <tr class="order-row">
                                    <td>
                                        <span class="font-monospace fw-semibold text-dark"><c:out value="${p.transactionCode}" /></span>
                                    </td>
                                    <td class="order-code">
                                        <a href="${ctx}/admin/orders?detailId=${p.orderId}" title="Xem chi tiết đơn hàng" class="text-decoration-none fw-bold">
                                            #<c:out value="${p.orderCode}" />
                                        </a>
                                    </td>
                                    <td>
                                        <strong><c:out value="${p.customerName}" /></strong>
                                        <c:if test="${not empty p.customerPhone}">
                                            <small><c:out value="${p.customerPhone}" /></small>
                                        </c:if>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${p.paymentMethod eq 'BANK_TRANSFER' or p.paymentMethod eq 'VIETQR'}">
                                                <span class="badge-qr">
                                                    <span class="material-symbols-outlined" style="font-size: 15px;">qr_code_2</span>
                                                    VietQR
                                                </span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge-cash">
                                                    <span class="material-symbols-outlined" style="font-size: 15px;">payments</span>
                                                    Tiền mặt
                                                </span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-end order-money">
                                        <fmt:formatNumber value="${p.amount}" pattern="#,##0" />đ
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty p.gatewayReference}">
                                                <span class="gateway-ref"><c:out value="${p.gatewayReference}" /></span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted" style="font-size: 0.8rem;">—</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-muted">
                                        <c:out value="${p.formattedPaidAt}" />
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${p.status eq 'SUCCESS'}">
                                                <span class="order-status status-completed">Thành công</span>
                                            </c:when>
                                            <c:when test="${p.status eq 'PENDING'}">
                                                <span class="order-status status-pending">Chờ xử lý</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="order-status status-cancelled">Thất bại</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-end">
                                        <a class="view-order" href="${ctx}/admin/orders?detailId=${p.orderId}">
                                            Xem đơn
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
    </section>
</main>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
