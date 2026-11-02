<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<fmt:setLocale value="vi_VN" />
<fmt:formatNumber value="${settings.pointsEarnUnit}" pattern="0" var="pointsEarnUnitValue" />
<fmt:formatNumber value="${settings.pointsStepValue}" pattern="0" var="pointsStepValue" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Aura Café | Tích điểm theo số điện thoại</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Material+Symbols+Outlined" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <style>
        .loyalty-page { color:#292524; }
        .loyalty-panel { background:#fff; border:1px solid #e7e5e4; border-radius:16px; box-shadow:0 1px 2px rgba(28,25,23,.04); }
        .loyalty-header { padding:16px; }
        .db-ref { display:inline-block; padding:3px 8px; border-radius:5px; background:#f5f5f4; color:#78716c; font:700 9px ui-monospace,monospace; }
        .loyalty-header h1 { margin:7px 0 2px; font-size:20px; font-weight:800; }
        .loyalty-header p { margin:0; color:#78716c; font-size:11px; }
        .stats-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:10px; padding-top:12px; margin-top:12px; border-top:1px solid #f5f5f4; }
        .stat-card { padding:12px; border:1px solid #e7e5e4; border-radius:12px; background:#fafaf9; }
        .stat-card.primary { color:#a51225; border-color:#fecdd3; background:#fff7f7; }
        .stat-card span { display:block; color:#78716c; font-size:9px; font-weight:800; text-transform:uppercase; }
        .stat-card strong { display:block; margin-top:3px; font:900 18px ui-monospace,monospace; }
        .content-grid { display:grid; grid-template-columns:minmax(0,7fr) minmax(340px,5fr); gap:20px; margin:14px 0; }
        .content-card { padding:20px; }
        .section-title { padding-bottom:12px; margin:0 0 14px; border-bottom:1px solid #f5f5f4; font-size:13px; font-weight:800; }
        .rules-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:12px; }
        .field-label { display:block; color:#44403c; font-size:10px; font-weight:700; line-height:1.35; }
        .field-column { float:right; color:#a8a29e; font:8px ui-monospace,monospace; }
        .loyalty-input { width:100%; height:38px; margin-top:5px; padding:0 11px; border:1px solid #d6d3d1; border-radius:11px; background:#fafaf9; outline:none; font-size:11px; }
        .loyalty-input:focus { border-color:#a51225; box-shadow:0 0 0 2px rgba(165,18,37,.08); }
        .rule-example { margin:13px 0 0; color:#78716c; font-size:10px; }
        .save-row { display:flex; justify-content:flex-end; margin-top:14px; }
        .save-btn { display:flex; align-items:center; gap:6px; padding:9px 16px; border:0; border-radius:11px; background:#a51225; color:#fff; font-size:10px; font-weight:800; }
        .search-form { display:flex; gap:7px; }
        .search-form .loyalty-input { flex:1; margin:0; font-family:ui-monospace,monospace; }
        .search-btn { padding:0 13px; border:0; border-radius:10px; background:#1c1917; color:#fff; font-size:10px; font-weight:800; }
        .customer-box { padding:13px; margin-top:12px; border:1px solid #e7e5e4; border-radius:11px; background:rgba(250,250,249,.7); }
        .customer-box h3 { margin:0; font-size:12px; font-weight:800; }
        .customer-contact { margin:2px 0 10px; color:#78716c; font:8px ui-monospace,monospace; }
        .customer-metrics { display:grid; grid-template-columns:1fr 1fr; gap:7px; }
        .customer-metric { padding:9px; border:1px solid #e7e5e4; border-radius:8px; background:#fff; }
        .customer-metric span { display:block; color:#a8a29e; font-size:8px; font-weight:700; }
        .customer-metric strong { display:block; margin:2px 0; font:900 13px ui-monospace,monospace; }
        .customer-metric small { display:block; color:#78716c; font-size:8px; }
        .adjust-row { display:grid; grid-template-columns:1fr 1fr; gap:7px; margin-top:9px; }
        .adjust-btn { padding:7px; border:0; border-radius:10px; font-size:9px; font-weight:800; }
        .adjust-add { background:#059669; color:#fff; }
        .adjust-subtract { background:#e7e5e4; color:#292524; }
        .log-panel { overflow:hidden; }
        .log-head { padding:16px 20px; border-bottom:1px solid #f5f5f4; }
        .log-head h2 { margin:0; font-size:14px; font-weight:800; }
        .log-table { width:100%; margin:0; font-size:9px; }
        .log-table th { padding:12px 11px; border-bottom:1px solid #e7e5e4; background:#fafaf9; color:#78716c; font-size:8px; text-transform:uppercase; }
        .log-table td { padding:13px 11px; border-bottom:1px solid #f5f5f4; vertical-align:middle; }
        .log-table tbody tr:last-child td { border-bottom:0; }
        .mono { font-family:ui-monospace,monospace; }
        .customer-name { display:block; color:#1c1917; font-weight:800; }
        .customer-phone { color:#a8a29e; }
        .type-badge { display:inline-block; padding:3px 7px; border-radius:4px; font-size:7px; font-weight:800; }
        .type-earn { color:#047857; background:#d1fae5; }
        .type-redeem { color:#a16207; background:#fef3c7; }
        .points-plus { color:#059669; font-weight:900; }
        .points-minus { color:#e11d48; font-weight:900; }
        @media (max-width:991px) { .content-grid { grid-template-columns:1fr; } }
        @media (max-width:767px) { .stats-grid { grid-template-columns:repeat(2,1fr); } .rules-grid { grid-template-columns:1fr; } }
    </style>
</head>
<body class="admin-body">
<jsp:include page="/components/admin-sidebar.jsp">
    <jsp:param name="activeMenu" value="loyalty" />
</jsp:include>

<main class="admin-main-wrapper loyalty-page">
    <c:if test="${not empty loyaltyMessage}"><div class="alert alert-success py-2 small"><c:out value="${loyaltyMessage}" /></div></c:if>
    <c:if test="${not empty loyaltyError}"><div class="alert alert-danger py-2 small"><c:out value="${loyaltyError}" /></div></c:if>
    <section class="loyalty-panel loyalty-header">
        <span class="db-ref">Bảng: loyalty_transactions, users.total_points, shop_settings</span>
        <h1>Tích điểm theo số điện thoại</h1>
        <p>Khách hàng được nhận diện bằng số điện thoại; điểm tích khi đơn hoàn thành và đổi điểm để giảm giá.</p>

        <div class="stats-grid">
            <div class="stat-card"><span>Khách hàng có điểm</span><strong><fmt:formatNumber value="${customerCountWithPoints}" pattern="#,##0" /></strong></div>
            <div class="stat-card primary"><span>Tổng điểm lưu hành</span><strong><fmt:formatNumber value="${totalPoints}" pattern="#,##0" /></strong></div>
            <div class="stat-card"><span>Giá trị quy đổi</span><strong><fmt:formatNumber value="${totalPointValue}" pattern="#,##0" />đ</strong></div>
            <div class="stat-card"><span>Giao dịch điểm</span><strong><fmt:formatNumber value="${transactionCount}" pattern="#,##0" /></strong></div>
        </div>
    </section>

    <div class="content-grid">
        <%-- BF12 - LƯU QUY TẮC: POST /admin/loyalty -> doPost() -> updateRules() -> ShopSettingDAO. --%>
        <form class="loyalty-panel content-card" method="post" action="${ctx}/admin/loyalty">
            <input type="hidden" name="action" value="updateRules">
            <input type="hidden" name="csrfToken" value="${sessionScope.loyaltyCsrfToken}">
            <input type="hidden" name="phone" value="${fn:escapeXml(searchPhone)}">
            <h2 class="section-title">Quy tắc tích &amp; đổi điểm</h2>
            <div class="rules-grid">
                <label class="field-label">Số tiền chi tiêu để nhận 1 điểm (VNĐ)<span class="field-column">points_earn_unit</span>
                    <input class="loyalty-input" name="pointsEarnUnit" type="number" min="1" value="${pointsEarnUnitValue}" required>
                </label>
                <label class="field-label">Số điểm cho mỗi lần đổi<span class="field-column">points_step</span>
                    <input class="loyalty-input" name="pointsStep" type="number" min="1" value="${settings.pointsStep}" required>
                </label>
                <label class="field-label">Số tiền giảm cho mỗi lần đổi (VNĐ)<span class="field-column">points_step_value</span>
                    <input class="loyalty-input" name="pointsStepValue" type="number" min="0" value="${pointsStepValue}" required>
                </label>
            </div>
            <p class="rule-example">
                Tích điểm: chi <strong><fmt:formatNumber value="${settings.pointsEarnUnit}" pattern="#,##0" />đ</strong> nhận <strong>1 điểm</strong>.
                Đổi điểm: dùng <strong>${settings.pointsStep} điểm</strong> được giảm
                <strong><fmt:formatNumber value="${settings.pointsStepValue}" pattern="#,##0" />đ</strong>.
            </p>
            <div class="save-row">
                <button class="save-btn" type="submit"><span class="material-symbols-outlined" style="font-size:15px">save</span>Lưu quy tắc</button>
            </div>
        </form>

        <section class="loyalty-panel content-card">
            <h2 class="section-title">Tra cứu &amp; điều chỉnh điểm</h2>
            <%-- BF12 - TRA CỨU: GET gửi phone -> doGet() -> findCustomerByPhone() -> activeCustomer. --%>
            <form class="search-form" method="get" action="${ctx}/admin/loyalty">
                <input class="loyalty-input" name="phone" value="${fn:escapeXml(searchPhone)}" aria-label="Số điện thoại" placeholder="Nhập số điện thoại..." required>
                <button class="search-btn" type="submit">Tra cứu</button>
            </form>
            <c:if test="${not empty lookupError}"><div class="alert alert-warning py-2 px-3 mt-2 mb-0 small"><c:out value="${lookupError}" /></div></c:if>
            <c:if test="${not empty activeCustomer}">
              <div class="customer-box">
                <h3><c:out value="${activeCustomer.fullName}" /></h3>
                <p class="customer-contact"><c:out value="${activeCustomer.phone}" /> • <c:out value="${activeCustomer.email}" /></p>
                <%-- Hiển thị User do Controller gán vào request; JSP không trực tiếp truy vấn DB. --%>
                <div class="customer-metrics">
                    <div class="customer-metric"><span>Điểm hiện có</span><strong style="color:#a51225"><fmt:formatNumber value="${activeCustomer.totalPoints}" pattern="#,##0" /></strong><small>= <fmt:formatNumber value="${customerPointValue}" pattern="#,##0" />đ</small></div>
                    <div class="customer-metric"><span>Tổng chi tiêu</span><strong><fmt:formatNumber value="${customerActivity.totalSpent}" pattern="#,##0" />đ</strong><small><c:choose><c:when test="${not empty customerActivity.lastOrderAt}">Đơn gần nhất <fmt:formatDate value="${customerActivity.lastOrderAt}" pattern="dd/MM/yyyy HH:mm" /></c:when><c:otherwise>Chưa có đơn</c:otherwise></c:choose></small></div>
                </div>
                <div class="adjust-row">
                    <button class="adjust-btn adjust-add" type="button" onclick="submitAdjustment('add')">Cộng điểm</button>
                    <button class="adjust-btn adjust-subtract" type="button" onclick="submitAdjustment('subtract')">Trừ điểm</button>
                </div>
                <%-- BF12 - CỘNG/TRỪ: JavaScript điền form -> doPost() action=adjust -> adjustPoints(). --%>
                <form id="adjustPointsForm" method="post" action="${ctx}/admin/loyalty">
                    <input type="hidden" name="action" value="adjust">
                    <input type="hidden" name="csrfToken" value="${sessionScope.loyaltyCsrfToken}">
                    <input type="hidden" name="customerId" value="${activeCustomer.id}">
                    <input type="hidden" name="phone" value="${fn:escapeXml(activeCustomer.phone)}">
                    <input type="hidden" id="adjustType" name="adjustType">
                    <input type="hidden" id="adjustPoints" name="points">
                </form>
              </div>
            </c:if>
        </section>
    </div>

    <section class="loyalty-panel log-panel">
        <div class="log-head"><h2>Nhật ký tích &amp; đổi điểm</h2></div>
        <div class="table-responsive">
            <table class="log-table">
                <thead><tr><th>ID</th><th>Thời gian</th><th>Khách hàng</th><th>Loại</th><th class="text-end">Điểm biến động</th><th class="text-end">Số dư sau</th><th>Đơn hàng</th></tr></thead>
                <tbody>
                <%-- Nhật ký chung từ loyaltyDAO.findRecent(50), gồm cả giao dịch của khách khác. --%>
                <c:forEach var="tx" items="${transactions}">
                    <tr>
                        <td class="mono text-muted">#${tx.id}</td>
                        <td class="mono text-muted"><fmt:formatDate value="${tx.createdAt}" pattern="dd/MM/yyyy HH:mm" /></td>
                        <td><span class="customer-name"><c:out value="${tx.customerName}" /></span><span class="customer-phone mono"><c:out value="${tx.customerPhone}" /></span></td>
                        <td><span class="type-badge ${tx.pointsChange gt 0 ? 'type-earn' : 'type-redeem'}"><c:choose><c:when test="${tx.transactionType eq 'EARN'}">EARN - Tích điểm</c:when><c:when test="${tx.transactionType eq 'REDEEM'}">REDEEM - Đổi điểm</c:when><c:when test="${tx.transactionType eq 'REFUND'}">REFUND - Hoàn điểm</c:when><c:otherwise>ADJUST - Điều chỉnh</c:otherwise></c:choose></span></td>
                        <td class="text-end mono ${tx.pointsChange gt 0 ? 'points-plus' : 'points-minus'}">${tx.pointsChange gt 0 ? '+' : ''}${tx.pointsChange}</td>
                        <td class="text-end mono fw-bold"><fmt:formatNumber value="${tx.balanceAfter}" pattern="#,##0" /></td>
                        <td class="text-muted"><c:choose><c:when test="${not empty tx.orderCode}"><c:out value="${tx.orderCode}" /></c:when><c:otherwise>Điều chỉnh thủ công</c:otherwise></c:choose></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty transactions}"><tr><td colspan="7" class="text-center text-muted py-4">Chưa có giao dịch điểm.</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </section>
</main>
<script>
    function submitAdjustment(type) {
        // Chỉ nhận số và gửi form; kiểm tra số dư và lưu DB nằm ở Controller/DAO, không ở JavaScript.
        const label = type === 'add' ? 'cộng' : 'trừ';
        const raw = window.prompt('Nhập số điểm muốn ' + label + ':', '10');
        if (raw === null) return;
        const points = Number(raw);
        if (!Number.isInteger(points) || points <= 0 || points > 1000000) {
            window.alert('Số điểm phải là số nguyên từ 1 đến 1.000.000.');
            return;
        }
        document.getElementById('adjustType').value = type;
        document.getElementById('adjustPoints').value = String(points);
        document.getElementById('adjustPointsForm').submit();
    }
</script>
</body>
</html>
