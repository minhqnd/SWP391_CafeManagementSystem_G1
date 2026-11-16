<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<fmt:setLocale value="vi_VN" />
<fmt:formatNumber value="${settings.pointsEarnUnit}" pattern="0" var="pointsEarnUnitValue" />
<fmt:formatNumber value="${settings.pointsStepValue}" pattern="0" var="pointsStepValue" />
<c:set var="pointsStepInputValue" value="${settings.pointsStep}" />
<c:if test="${not empty loyaltyRulesDraft}">
    <c:set var="pointsEarnUnitValue" value="${loyaltyRulesDraft.pointsEarnUnit}" />
    <c:set var="pointsStepInputValue" value="${loyaltyRulesDraft.pointsStep}" />
    <c:set var="pointsStepValue" value="${loyaltyRulesDraft.pointsStepValue}" />
</c:if>
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
        .loyalty-input.is-invalid { border-color:#dc3545; }
        .loyalty-input.is-invalid:focus { box-shadow:0 0 0 2px rgba(220,53,69,.15); }
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
    <%-- MS02: . Vùng thông báo dùng chung cho lưu quy tắc và điều chỉnh điểm.
         .. Giá trị thông báo được c:out escape trước khi hiển thị.
         ... Thành công tại [MS03]; lỗi tại [MS04]. --%>
    <%-- MS03: [SLBF12] Show success message
         . Chỉ hiển thị thông báo đã lưu sau khi controller nhận kết quả thành công.
         .. Đây là kết quả trả về, không tự lưu thêm dữ liệu. --%>
    <c:if test="${not empty loyaltyMessage}"><div class="alert alert-success py-2 small"><c:out value="${loyaltyMessage}" /></div></c:if>
    <%-- MS04: [SLBF12] Show error message
         . Hiển thị lỗi kiểm tra, gửi lặp hoặc lưu thất bại.
         .. Sai ở controller thì chưa gọi DAO; sai trong transaction thì DAO rollback [LP15].
         ... Quy tắc sai giữ bản nháp [RU24]; cộng/trừ lại bắt đầu tại [LP01]. --%>
    <c:if test="${not empty loyaltyError}"><div class="alert alert-danger py-2 small"><c:out value="${loyaltyError}" /></div></c:if>
    <section class="loyalty-panel loyalty-header">
        <span class="db-ref">Bảng: loyalty_transactions, users.total_points, shop_settings</span>
        <h1>Tích điểm theo số điện thoại</h1>
        <p>Khách hàng được nhận diện bằng số điện thoại; điểm tích khi đơn hoàn thành và đổi điểm để giảm giá.</p>

        <div class="stats-grid">
            <%-- ST05: . hiển thị số khách đang có điểm.
                 .. Chỉ khách có số dư lớn hơn không được tính.
                 Nguồn: Nguồn số đếm: countCustomersWithPoints() [ST01]. --%>
            <div class="stat-card"><span>Khách hàng có điểm</span><strong><fmt:formatNumber value="${customerCountWithPoints}" pattern="#,##0" /></strong></div>
            <%-- ST06: . hiển thị tổng số điểm các khách đang giữ.
                 .. Đây là cộng số dư để thống kê, không thưởng thêm điểm.
                 Nguồn: Nguồn tổng điểm: LoyaltyTransactionDAO.getTotalPoints() [ST02]. --%>
            <div class="stat-card primary"><span>Tổng điểm lưu hành</span><strong><fmt:formatNumber value="${totalPoints}" pattern="#,##0" /></strong></div>
            <%-- ST07: . hiển thị tiền giảm ước tính của tổng điểm lưu hành.
                 .. Giá trị này phụ thuộc quy tắc đổi điểm hiện tại.
                 Nguồn: Controller tính tại [ST03] → totalPointValue. --%>
            <div class="stat-card"><span>Giá trị quy đổi</span><strong><fmt:formatNumber value="${totalPointValue}" pattern="#,##0" />đ</strong></div>
            <%-- ST08: . hiển thị số giao dịch điểm đã ghi nhận.
                 .. Mỗi dòng lịch sử được tính là một giao dịch.
                 Nguồn: Nguồn số đếm: countTransactions() [ST04]. --%>
            <div class="stat-card"><span>Giao dịch điểm</span><strong><fmt:formatNumber value="${transactionCount}" pattern="#,##0" /></strong></div>
        </div>
    </section>

    <div class="content-grid">
        <%-- RU04: [SLBF12] Enter earn and redeem rules
             . Form chỉnh ba quy tắc; action=updateRules, phone chỉ giữ khách đang xem.
             .. Chưa ghi dữ liệu khi nhập; dữ liệu sai được giữ trong bản nháp.
             ... Điền lần lượt mức tích [RU05], mốc đổi [RU06], tiền giảm [RU07], rồi bấm Lưu [RU08]. --%>
        <form class="loyalty-panel content-card" method="post" action="${ctx}/admin/loyalty" novalidate>
            <input type="hidden" name="action" value="updateRules">
            <input type="hidden" name="csrfToken" value="${sessionScope.loyaltyCsrfToken}">
            <input type="hidden" name="operationToken" value="${sessionScope.loyaltyOperationToken}">
            <input type="hidden" name="phone" value="${fn:escapeXml(searchPhone)}">
            <h2 class="section-title">Quy tắc tích &amp; đổi điểm</h2>
            <div class="rules-grid">
                <label class="field-label">Số tiền chi tiêu để nhận 1 điểm (VNĐ)<span class="field-column">points_earn_unit</span>
                    <%-- RU05: . Ô pointsEarnUnit nhận mức chi tiêu để được 1 điểm, từ 1 đến 100.000.000.
                         .. Được nhập quá giới hạn để server báo đỏ; chưa lưu khi nhập.
                         ... Nhập tiếp mốc đổi [RU06]; bấm Lưu tại [RU08], server đọc trường này tại [RU15]. --%>
                    <input class="loyalty-input ${not empty loyaltyFieldErrors.pointsEarnUnit ? 'is-invalid' : ''}" name="pointsEarnUnit" type="text" inputmode="numeric" value="${fn:escapeXml(pointsEarnUnitValue)}" aria-invalid="${not empty loyaltyFieldErrors.pointsEarnUnit}" aria-describedby="earnUnitError" required>
                    <div id="earnUnitError" class="text-danger small"><c:out value="${loyaltyFieldErrors.pointsEarnUnit}" /></div>
                </label>
                <label class="field-label">Số điểm cho mỗi lần đổi<span class="field-column">points_step</span>
                    <%-- RU06: . Ô pointsStep nhận số điểm mỗi mốc, từ 1 đến 1.000.000.
                         .. Đây là số nguyên theo mốc đổi, không phải số điểm cộng/trừ thủ công.
                         ... Nhập tiền giảm [RU07]; gửi bằng nút [RU08], server đọc tại [RU16]. --%>
                    <input class="loyalty-input ${not empty loyaltyFieldErrors.pointsStep ? 'is-invalid' : ''}" name="pointsStep" type="text" inputmode="numeric" value="${fn:escapeXml(pointsStepInputValue)}" aria-invalid="${not empty loyaltyFieldErrors.pointsStep}" aria-describedby="pointsStepError" required>
                    <div id="pointsStepError" class="text-danger small"><c:out value="${loyaltyFieldErrors.pointsStep}" /></div>
                </label>
                <label class="field-label">Số tiền giảm cho mỗi lần đổi (VNĐ)<span class="field-column">points_step_value</span>
                    <%-- RU07: . Ô pointsStepValue nhận tiền giảm cho mỗi mốc, từ 0 đến 100.000.000.
                         .. Cho phép 0; không nhận số âm, số lẻ, chữ hoặc dạng 1e3.
                         ... Bấm Lưu quy tắc [RU08]; server đọc trường tại [RU17]. --%>
                    <input class="loyalty-input ${not empty loyaltyFieldErrors.pointsStepValue ? 'is-invalid' : ''}" name="pointsStepValue" type="text" inputmode="numeric" value="${fn:escapeXml(pointsStepValue)}" aria-invalid="${not empty loyaltyFieldErrors.pointsStepValue}" aria-describedby="stepValueError" required>
                    <div id="stepValueError" class="text-danger small"><c:out value="${loyaltyFieldErrors.pointsStepValue}" /></div>
                </label>
            </div>
            <p class="rule-example">
                Tích điểm: chi <strong><fmt:formatNumber value="${settings.pointsEarnUnit}" pattern="#,##0" />đ</strong> nhận <strong>1 điểm</strong>.
                Đổi điểm: dùng <strong>${settings.pointsStep} điểm</strong> được giảm
                <strong><fmt:formatNumber value="${settings.pointsStepValue}" pattern="#,##0" />đ</strong>.
            </p>
            <div class="save-row">
                <%-- RU08: [SLBF12] Click "Save Rules"
                     . Submit ba ô quy tắc cùng csrfToken và operationToken; form novalidate để server báo lỗi từng ô.
                     .. Không có JavaScript xử lý submit quy tắc; trình duyệt gửi POST trực tiếp.
                     ... AdminLoyaltyController.doPost() [RU09] nhận POST /admin/loyalty. --%>
                <button class="save-btn" type="submit"><span class="material-symbols-outlined" style="font-size:15px">save</span>Lưu quy tắc</button>
            </div>
        </form>

        <section class="loyalty-panel content-card">
            <h2 class="section-title">Tra cứu &amp; điều chỉnh điểm</h2>
            <%-- CU01: . Form tra cứu dùng GET, chỉ đọc khách hàng và không đổi điểm.
                 ... Nhập ô SĐT [CU02] rồi bấm Tra cứu [CU03]. --%>
            <form class="search-form" method="get" action="${ctx}/admin/loyalty" novalidate>
                <%-- CU02: [SLBF12] Enter customer phone number
                     . Ô phone nhận SĐT cần tra; cho nhập dữ liệu sai để server báo lỗi.
                     ... Bấm Tra cứu [CU03] mới gửi dữ liệu; doGet() kiểm tra tại [CU04]. --%>
                <input class="loyalty-input ${not empty lookupError ? 'is-invalid' : ''}" type="tel" name="phone" value="${fn:escapeXml(searchPhone)}" aria-label="Số điện thoại" aria-invalid="${not empty lookupError}" placeholder="Nhập số điện thoại..." required>
                <%-- CU03: [SLBF12] Click "Search"
                     . Submit GET /admin/loyalty?phone=...; không chạy JavaScript trước khi gửi.
                     ... AdminLoyaltyController.doGet() xử lý SĐT tại [CU04]. --%>
                <button class="search-btn" type="submit">Tra cứu</button>
            </form>
            <c:if test="${not empty lookupError}"><div class="alert alert-warning py-2 px-3 mt-2 mb-0 small"><c:out value="${lookupError}" /></div></c:if>
            <c:if test="${not empty activeCustomer}">
              <div class="customer-box">
                <%-- CU10: [SLBF12] Show customer information and point balance
                     . Hiển thị tên, SĐT và email của activeCustomer do controller trả về.
                     .. Các dữ liệu hiển thị được escape; chỉ có khách mới hiện nút Cộng/Trừ.
                     ... Xem số dư [LP18] và chi tiêu [CU11]; điều chỉnh bắt đầu từ nút [LP01]. --%>
                <h3><c:out value="${activeCustomer.fullName}" /></h3>
                <p class="customer-contact"><c:out value="${activeCustomer.phone}" /> • <c:out value="${activeCustomer.email}" /></p>
                <%-- LP18: [SLBF12] Show updated balance and transaction history
                     . Hiển thị số dư mới của activeCustomer và tiền quy đổi sau khi đọc lại database.
                     .. Nhật ký bên dưới là 50 giao dịch toàn hệ thống, không chỉ riêng khách đang tra.
                     ... Các dòng nhật ký hiển thị tại [HI05]; điều chỉnh tiếp bắt đầu từ [LP01]. --%>
                <div class="customer-metrics">
                    <div class="customer-metric"><span>Điểm hiện có</span><strong style="color:#a51225"><fmt:formatNumber value="${activeCustomer.totalPoints}" pattern="#,##0" /></strong><small>= <fmt:formatNumber value="${customerPointValue}" pattern="#,##0" />đ</small></div>
                    <%-- CU11: . Hiển thị tổng chi tiêu và thời điểm đơn gần nhất của đúng khách đang xem.
                         .. Đây là dữ liệu tham khảo, không phải số tiền dùng để tính điểm tại trang này.
                         ... Dữ liệu từ DAO [CU07]; thao tác điều chỉnh qua [LP01]. --%>
                    <div class="customer-metric"><span>Tổng chi tiêu</span><strong><fmt:formatNumber value="${customerActivity.totalSpent}" pattern="#,##0" />đ</strong><small><c:choose><c:when test="${not empty customerActivity.lastOrderAt}">Đơn gần nhất <fmt:formatDate value="${customerActivity.lastOrderAt}" pattern="dd/MM/yyyy HH:mm" /></c:when><c:otherwise>Chưa có đơn</c:otherwise></c:choose></small></div>
                </div>
                <%-- LP01: [SLBF12] Select "Add Points" or "Subtract Points"
                     . Hai nút gọi submitAdjustment(add/subtract), chưa tự sửa số dư.
                     ... submitAdjustment() [LP02] mở hộp nhập số điểm. --%>
                <div class="adjust-row">
                    <button class="adjust-btn adjust-add" type="button" onclick="submitAdjustment('add')">Cộng điểm</button>
                    <button class="adjust-btn adjust-subtract" type="button" onclick="submitAdjustment('subtract')">Trừ điểm</button>
                </div>
                <%-- LP03: . Form ẩn gửi action=adjust, customerId, phone, adjustType và points cùng hai token.
                     .. customerId quyết định khách cần sửa; phone chỉ giữ kết quả tra cứu sau redirect.
                     ... POST /admin/loyalty tới AdminLoyaltyController.doPost() [RU09] để kiểm tra quyền và chọn nhánh. --%>
                <form id="adjustPointsForm" method="post" action="${ctx}/admin/loyalty">
                    <input type="hidden" name="action" value="adjust">
                    <input type="hidden" name="csrfToken" value="${sessionScope.loyaltyCsrfToken}">
                    <input type="hidden" name="operationToken" value="${sessionScope.loyaltyOperationToken}">
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
                <%-- HI05: . Lặp transactions do controller nạp, tối đa 50 dòng mới nhất toàn hệ thống.
                     .. Không có giao dịch thì hiện trạng thái danh sách trống.
                     ... Mỗi dòng hiển thị tại [HI06]. --%>
                <c:forEach var="tx" items="${transactions}">
                    <tr>
                        <%-- HI06: . Hiển thị id, thời gian, tên/SĐT khách và mã đơn của từng giao dịch.
                             .. Các ô lấy dữ liệu DAO đã JOIN, tên/SĐT và mã đơn được escape.
                             ... Loại giao dịch [HI07], điểm có dấu và số dư sau [HI08]. --%>
                        <td class="mono text-muted">#${tx.id}</td>
                        <td class="mono text-muted"><fmt:formatDate value="${tx.createdAt}" pattern="dd/MM/yyyy HH:mm" /></td>
                        <td><span class="customer-name"><c:out value="${tx.customerName}" /></span><span class="customer-phone mono"><c:out value="${tx.customerPhone}" /></span></td>
                        <%-- HI07: . Phân biệt EARN, REDEEM, REFUND và ADJUST từ transactionType.
                             .. Điểm tăng hiện dấu +, điểm giảm giữ dấu âm; trang này chỉ ghi ADJUST khi điều chỉnh.
                             ... Số dư sau giao dịch tại [HI08]; tích/đổi tự động thuộc luồng thanh toán, không chạy ở trang này. --%>
                        <td><span class="type-badge ${tx.pointsChange gt 0 ? 'type-earn' : 'type-redeem'}"><c:choose><c:when test="${tx.transactionType eq 'EARN'}">EARN - Tích điểm</c:when><c:when test="${tx.transactionType eq 'REDEEM'}">REDEEM - Đổi điểm</c:when><c:when test="${tx.transactionType eq 'REFUND'}">REFUND - Hoàn điểm</c:when><c:otherwise>ADJUST - Điều chỉnh</c:otherwise></c:choose></span></td>
                        <td class="text-end mono ${tx.pointsChange gt 0 ? 'points-plus' : 'points-minus'}">${tx.pointsChange gt 0 ? '+' : ''}${tx.pointsChange}</td>
                        <%-- HI08: . Hiển thị balanceAfter được suy ngược tại DAO [HI04].
                             .. Giao dịch không gắn đơn hiển thị Điều chỉnh thủ công ở cột Đơn hàng. --%>
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
        // LP02: [SLBF12] Enter adjustment points
        // . Hộp prompt nhận số nguyên từ 1 đến 1.000.000; Cancel dừng thao tác.
        // .. Sai định dạng/ngoài giới hạn thì alert và chưa gửi POST.
        // ... Hợp lệ gán adjustType và points vào form [LP03] rồi submit tới doPost() [RU09].
        const label = type === 'add' ? 'cộng' : 'trừ';
        const raw = window.prompt('Nhập số điểm muốn ' + label + ':', '10');
        if (raw === null) return;
        const points = Number(raw);
        if (!/^[0-9]+$/.test(raw.trim()) || raw.trim().length > 19
                || !Number.isSafeInteger(points) || points <= 0 || points > 1000000) {
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
