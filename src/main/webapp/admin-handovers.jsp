<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty sessionScope.user or sessionScope.user.role ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lịch sử bàn giao ca | Aura Café</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-shifts.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-handovers.css" rel="stylesheet">
</head>
<body class="admin-body">
    <jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="handovers" /></jsp:include>
    <main class="admin-main-wrapper shift-page handover-page">
        <header class="shift-page-header">
            <div class="shift-heading-row"><div>
                <span class="shift-table-ref">Bảng: <strong>shift_handovers</strong></span>
                <h1>Lịch sử bàn giao ca</h1>
                <p>Biên bản được hoàn tất ngay khi Thu ngân chốt ca. Admin chỉ xem lịch sử, không cần xác nhận.</p>
            </div></div>
        </header>
        <section class="shift-list-card" aria-label="Lịch sử biên bản bàn giao">
            <c:choose>
                <c:when test="${not empty handovers}">
                    <div class="table-responsive"><table class="shift-table handover-table">
                        <thead><tr><th>Ca làm</th><th>Thu ngân</th><th class="text-end">Đầu ca</th><th class="text-end">Thu tiền mặt</th><th class="text-end">Hệ thống</th><th class="text-end">Thực tế</th><th class="text-end">Chênh lệch</th><th>Ghi chú</th><th>Trạng thái</th></tr></thead>
                        <tbody><c:forEach items="${handovers}" var="h"><tr>
                            <td><div class="shift-name-cell"><c:out value="${h.shiftName}" /> · ${h.workDateDisplay}</div><small class="handover-sub">${h.shiftTime} · chốt ${h.createdAtDisplay}</small></td>
                            <td><c:out value="${h.cashierName}" /></td>
                            <td class="text-end handover-money"><fmt:formatNumber value="${h.openingCash}" pattern="#,##0" />đ</td>
                            <td class="text-end handover-money"><fmt:formatNumber value="${h.cashSales}" pattern="#,##0" />đ</td>
                            <td class="text-end handover-money"><fmt:formatNumber value="${h.expectedCash}" pattern="#,##0" />đ</td>
                            <td class="text-end handover-money"><fmt:formatNumber value="${h.actualCash}" pattern="#,##0" />đ</td>
                            <td class="text-end"><span class="handover-diff diff-${h.difference.signum()}">${h.differenceType}<c:if test="${h.difference.signum() ne 0}"> <c:if test="${h.difference.signum() > 0}">+</c:if><fmt:formatNumber value="${h.difference}" pattern="#,##0" />đ</c:if></span></td>
                            <td class="handover-note-cell"><c:out value="${empty h.note ? '—' : h.note}" /></td>
                            <td><span class="handover-status status-CONFIRMED">Đã chốt ca</span></td>
                        </tr></c:forEach></tbody>
                    </table></div>
                </c:when>
                <c:otherwise><div class="shift-empty-state"><span class="material-symbols-outlined">assignment_turned_in</span><strong>Chưa có biên bản bàn giao</strong><span>Biên bản xuất hiện sau khi Thu ngân chốt ca.</span></div></c:otherwise>
            </c:choose>
        </section>
        <p class="shift-footnote">Chênh lệch = Thực tế − (Đầu ca + Thu tiền mặt). Nếu lệch két, Thu ngân phải nhập ghi chú khi chốt ca.</p>
    </main>
</body>
</html>
