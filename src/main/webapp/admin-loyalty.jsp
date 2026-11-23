<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<fmt:setLocale value="vi_VN" />
<fmt:formatNumber value="${settings.pointsEarnUnit}" pattern="0" var="pointsEarnUnitValue" />
<fmt:formatNumber value="${settings.pointsStepValue}" pattern="0" var="pointsStepValue" />
<c:set var="pointsStepInputValue" value="${settings.pointsStep}" />
<%-- . Nếu lần lưu trước có lỗi, điền lại những gì người dùng đã nhập để họ sửa tiếp. --%>
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

    <%-- [TDQT09]
         . Khi admin-loyalty.jsp nhận dữ liệu trở lại, em hiện thông báo và điền ba ô bằng bộ quy tắc đã đọc.
           Nếu lần lưu bị lỗi thì dùng bản nhập dở đã giữ cho các ô, để người dùng sửa rồi bấm Lưu quy tắc lần
           nữa.
         . Kết thúc Lưu quy tắc khi trang hiện kết quả và bộ quy tắc đã đọc lại. Với Cộng/Trừ điểm,
           trang hiện kết quả tại đây, còn số dư và lịch sử mới được doGet đưa vào các phần bên dưới.
    --%>
    <c:if test="${not empty loyaltyMessage and empty loyaltyError}"><div data-form-notice class="alert alert-success py-2 small" role="status"><c:out value="${loyaltyMessage}" /></div></c:if>

    <c:if test="${not empty loyaltyError}"><div data-form-notice class="alert alert-danger py-2 small" role="alert"><c:out value="${loyaltyError}" /></div></c:if>
    <section class="loyalty-panel loyalty-header">
        <span class="db-ref">Bảng: loyalty_transactions, users.total_points, shop_settings</span>
        <h1>Tích điểm theo số điện thoại</h1>
        <p>Khách hàng được nhận diện bằng số điện thoại; điểm tích khi đơn hoàn thành và đổi điểm để giảm giá.</p>


        <div class="stats-grid">

            <%-- [TDM04]
                 . Bốn thẻ trên admin-loyalty.jsp nhận số khách có điểm, tổng điểm, tiền quy đổi và số giao dịch từ
                   AdminLoyaltyController.doGet. Các giá trị customerCountWithPoints, totalPoints, totalPointValue và
                   transactionCount đã được đọc hoặc tính trước khi đưa sang trang.
                 ... Kết thúc phần tổng quan khi bốn thẻ số liệu đã hiện trên admin-loyalty.jsp.
            --%>
            <div class="stat-card"><span>Khách hàng có điểm</span><strong><fmt:formatNumber value="${customerCountWithPoints}" pattern="#,##0" /></strong></div>

            <div class="stat-card primary"><span>Tổng điểm lưu hành</span><strong><fmt:formatNumber value="${totalPoints}" pattern="#,##0" /></strong></div>

            <div class="stat-card"><span>Giá trị quy đổi</span><strong><fmt:formatNumber value="${totalPointValue}" pattern="#,##0" />đ</strong></div>

            <div class="stat-card"><span>Giao dịch điểm</span><strong><fmt:formatNumber value="${transactionCount}" pattern="#,##0" /></strong></div>
        </div>
    </section>

    <div class="content-grid">


        <form data-admin-form="loyalty" class="loyalty-panel content-card" method="post" action="${ctx}/admin/loyalty" novalidate>
            <input type="hidden" name="action" value="updateRules">
            <input type="hidden" name="csrfToken" value="${sessionScope.loyaltyCsrfToken}">
            <input type="hidden" name="operationToken" value="${sessionScope.loyaltyOperationToken}">
            <input type="hidden" name="phone" value="${fn:escapeXml(searchPhone)}">
            <h2 class="section-title">Quy tắc tích &amp; đổi điểm</h2>
            <div class="rules-grid">
                <label class="field-label">Số tiền chi tiêu để nhận 1 điểm (VNĐ)<span class="field-column">points_earn_unit</span>

                    <%-- [TDTICH01]
                         . Ô Tiền chi tiêu nhận một điểm trong admin-loyalty.jsp gửi giá trị bằng tên pointsEarnUnit. Khi
                           bấm Lưu quy tắc, doPost trong AdminLoyaltyController.java gọi updateRules (kiểm tra và lưu quy
                           tắc tích/đổi điểm) rồi ruleValue (đọc và kiểm tra một ô quy tắc) để kiểm tra số này trước khi lưu
                           cùng hai ô còn lại.
                         ... Đọc tiếp tại AdminLoyaltyController.java, tìm [TDTICH02].
                    --%>
                    <input class="loyalty-input ${not empty loyaltyFieldErrors.pointsEarnUnit ? 'is-invalid' : ''}" name="pointsEarnUnit" type="text" inputmode="numeric" value="${fn:escapeXml(pointsEarnUnitValue)}" aria-invalid="${not empty loyaltyFieldErrors.pointsEarnUnit}" aria-describedby="earnUnitError" required>
                    <div id="earnUnitError" class="text-danger small"><c:out value="${loyaltyFieldErrors.pointsEarnUnit}" /></div>
                </label>
                <label class="field-label">Số điểm cho mỗi lần đổi<span class="field-column">points_step</span>

                    <%-- [TDMOC01]
                         . Ô Số điểm mỗi lần đổi trong admin-loyalty.jsp gửi bằng tên pointsStep. Khi bấm Lưu quy tắc,
                           doPost trong AdminLoyaltyController.java gọi updateRules (kiểm tra và lưu quy tắc tích/đổi điểm)
                           rồi ruleValue (đọc và kiểm tra một ô quy tắc) để kiểm tra mốc đổi hợp lệ trước khi thay bộ quy
                           tắc.
                         ... Đọc tiếp tại AdminLoyaltyController.java, tìm [TDMOC02].
                    --%>
                    <input class="loyalty-input ${not empty loyaltyFieldErrors.pointsStep ? 'is-invalid' : ''}" name="pointsStep" type="text" inputmode="numeric" value="${fn:escapeXml(pointsStepInputValue)}" aria-invalid="${not empty loyaltyFieldErrors.pointsStep}" aria-describedby="pointsStepError" required>
                    <div id="pointsStepError" class="text-danger small"><c:out value="${loyaltyFieldErrors.pointsStep}" /></div>
                </label>
                <label class="field-label">Số tiền giảm cho mỗi lần đổi (VNĐ)<span class="field-column">points_step_value</span>

                    <%-- [TDGT01]
                         . Ô Tiền giảm mỗi lần đổi trong admin-loyalty.jsp gửi bằng tên pointsStepValue. Khi bấm Lưu quy
                           tắc, doPost trong AdminLoyaltyController.java gọi updateRules (kiểm tra và lưu quy tắc tích/đổi
                           điểm) rồi ruleValue (đọc và kiểm tra một ô quy tắc) để kiểm tra tiền giảm trước khi lưu cùng hai
                           quy tắc còn lại.
                         ... Đọc tiếp tại AdminLoyaltyController.java, tìm [TDGT02].
                    --%>
                    <input class="loyalty-input ${not empty loyaltyFieldErrors.pointsStepValue ? 'is-invalid' : ''}" name="pointsStepValue" type="text" inputmode="numeric" value="${fn:escapeXml(pointsStepValue)}" aria-invalid="${not empty loyaltyFieldErrors.pointsStepValue}" aria-describedby="stepValueError" required>
                    <div id="stepValueError" class="text-danger small"><c:out value="${loyaltyFieldErrors.pointsStepValue}" /></div>
                </label>
            </div>
            <%-- [TDVD01]
         . Dòng ví dụ dưới form dùng bộ settings đã đọc từ ShopSettingDAO.getSettingsStrict (đọc các thiết lập đang
                   lưu) để diễn giải quy tắc đang lưu: chi bao nhiêu nhận một điểm, đổi bao nhiêu điểm được giảm bao
                   nhiêu tiền. Khi nhập sai, các ô giữ bản đang nhập nhưng dòng ví dụ vẫn phản ánh quy tắc cũ chưa bị
                   thay.
                 ... Kết thúc phần diễn giải tại dòng ví dụ của bộ quy tắc đang lưu trên admin-loyalty.jsp.
            --%>
            <p class="rule-example">
                Tích điểm: chi <strong><fmt:formatNumber value="${settings.pointsEarnUnit}" pattern="#,##0" />đ</strong> nhận <strong>1 điểm</strong>.
                Đổi điểm: dùng <strong>${settings.pointsStep} điểm</strong> được giảm
                <strong><fmt:formatNumber value="${settings.pointsStepValue}" pattern="#,##0" />đ</strong>.
            </p>
            <div class="save-row">

                <%-- [TDQT01]
         . Em bắt đầu từ nút Lưu quy tắc trong admin-loyalty.jsp. Khi bấm, admin-form-validation.js kiểm tra
           ba ô ngay trên trang. Đúng thì form gửi tiền chi tiêu nhận một điểm, mốc đổi và tiền giảm tới
           AdminLoyaltyController.doPost qua /admin/loyalty, kèm action=updateRules để chọn thao tác lưu.
                     ... Đọc tiếp tại AdminLoyaltyController.java, tìm [TDQT02].
                --%>
                <button class="save-btn" type="submit"><span class="material-symbols-outlined" style="font-size:15px">save</span>Lưu quy tắc</button>
            </div>
        </form>

        <section class="loyalty-panel content-card">
            <h2 class="section-title">Tra cứu &amp; điều chỉnh điểm</h2>


            <form class="search-form" method="get" action="${ctx}/admin/loyalty" novalidate>

                <%-- [TDSDT01]
                     . Ô Số điện thoại gửi phone khi bấm Tra cứu. AdminLoyaltyController.doGet bỏ các dấu phân cách được hỗ
                       trợ rồi kiểm tra đủ 10 chữ số, bắt đầu bằng 0. Số đúng mới dùng để tìm khách; sai thì hiện
                       lookupError và không chọn nhầm khách mặc định.
                     ... Đọc tiếp tại AdminLoyaltyController.java, tìm [TDSDT02].
                --%>
                <input class="loyalty-input ${not empty lookupError ? 'is-invalid' : ''}" type="tel" name="phone" value="${fn:escapeXml(searchPhone)}" aria-label="Số điện thoại" aria-invalid="${not empty lookupError}" placeholder="Nhập số điện thoại..." required>

                <%-- [TDTK01]
                     . Em bắt đầu từ nút Tra cứu trong admin-loyalty.jsp. Khi bấm, form gửi phone tới
                       AdminLoyaltyController.doGet qua /admin/loyalty để kiểm tra số và gọi
                       LoyaltyTransactionDAO.findCustomerByPhone tìm đúng khách cần xem điểm.
                     ... Đọc tiếp tại AdminLoyaltyController.java, tìm [TDTK02].
                --%>
                <button class="search-btn" type="submit">Tra cứu</button>
            </form>
            <c:if test="${not empty lookupError}"><div class="alert alert-warning py-2 px-3 mt-2 mb-0 small"><c:out value="${lookupError}" /></div></c:if>
            <c:if test="${not empty activeCustomer}">
              <div class="customer-box">

                <%-- [TDTK05]
                     . Khi admin-loyalty.jsp nhận activeCustomer, em hiện tên, số điện thoại và email của khách tìm được.
                       Em đồng thời đặt mã của khách vào form điều chỉnh để các lần bấm Cộng hoặc Trừ gửi đúng người đang
                       hiển thị.
                     . Kết thúc tra cứu thành công khi trang hiện đúng khách cùng số dư và thông tin liên quan.
                --%>
                <h3><c:out value="${activeCustomer.fullName}" /></h3>
                <p class="customer-contact"><c:out value="${activeCustomer.phone}" /> • <c:out value="${activeCustomer.email}" /></p>

                <div class="customer-metrics">
                    <%-- [TDSD02]
                         . Ở admin-loyalty.jsp, em lấy số dư từ activeCustomer.totalPoints và tiền có thể đổi từ
                           customerPointValue. doGet trong AdminLoyaltyController.java đã tính số tiền theo quy tắc hiện tại
                           rồi đưa sang, để người dùng đối chiếu điểm với giá trị được giảm.
                         ... Kết thúc xem số dư khi điểm và giá trị có thể đổi của đúng khách đã hiện trên
                             admin-loyalty.jsp.
                    --%>
                    <div class="customer-metric"><span>Điểm hiện có</span><strong style="color:#a51225"><fmt:formatNumber value="${activeCustomer.totalPoints}" pattern="#,##0" /></strong><small>= <fmt:formatNumber value="${customerPointValue}" pattern="#,##0" />đ</small></div>

                    <%-- [TDCT03]
                         . Phần hoạt động mua trong admin-loyalty.jsp lấy dữ liệu từ customerActivity. Trước đó
                           AdminLoyaltyController.doGet đã gọi LoyaltyTransactionDAO.getCustomerActivity (đọc tổng tiền mua
                           và ngày có đơn gần nhất) theo mã khách để đọc tổng tiền đã mua và ngày có đơn gần nhất rồi đưa
                           sang đây.
                         ... Kết thúc phần hoạt động mua tại tổng chi tiêu và ngày mua gần nhất của khách đang xem
                             trên admin-loyalty.jsp.
                    --%>
                    <div class="customer-metric"><span>Tổng chi tiêu</span><strong><fmt:formatNumber value="${customerActivity.totalSpent}" pattern="#,##0" />đ</strong><small><c:choose><c:when test="${not empty customerActivity.lastOrderAt}">Đơn gần nhất <fmt:formatDate value="${customerActivity.lastOrderAt}" pattern="dd/MM/yyyy HH:mm" /></c:when><c:otherwise>Chưa có đơn</c:otherwise></c:choose></small></div>
                </div>

                <div class="adjust-row">
                    <%-- [TDC01]
                         . Khi bấm Cộng điểm trong admin-loyalty.jsp, em gọi submitAdjustment ngay trong file và truyền add
                           để nhập rồi gửi số điểm cần cộng. Hàm này dùng chung với nút Trừ điểm; loại thao tác quyết định
                           sẽ cộng hay trừ cho khách đang xem.
                         ... Trong admin-loyalty.jsp, đọc phần định nghĩa submitAdjustment ở cuối file; add quyết
                             định cộng điểm, rồi form adjustPointsForm được gửi tới AdminLoyaltyController.doPost.
                    --%>
                    <button class="adjust-btn adjust-add" type="button" onclick="submitAdjustment('add')">Cộng điểm</button>
                    <%-- [TDT01]
                         . Khi bấm Trừ điểm trong admin-loyalty.jsp, em gọi submitAdjustment ngay trong file và truyền
                           subtract để nhập rồi gửi số điểm cần trừ. Hàm dùng cùng mã khách đang xem; phía xử lý sẽ kiểm tra
                           số dư trước khi cho phép trừ.
                         ... Trong admin-loyalty.jsp, đọc phần định nghĩa submitAdjustment ở cuối file; subtract
                             quyết định trừ điểm, rồi form adjustPointsForm được gửi tới
                             AdminLoyaltyController.doPost.
                    --%>
                    <button class="adjust-btn adjust-subtract" type="button" onclick="submitAdjustment('subtract')">Trừ điểm</button>
                </div>

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

                <%-- [TDLS01]
                     . Bảng lịch sử trong admin-loyalty.jsp lặp danh sách transactions do AdminLoyaltyController.doGet đưa
                       sang. Danh sách này được đọc bằng LoyaltyTransactionDAO.findRecent (đọc các giao dịch điểm gần nhất)
                       với giới hạn 50 giao dịch mới nhất, để người dùng kiểm tra lịch sử chung đã ghi.
                     ... Kết thúc đọc lịch sử chung khi bảng giao dịch trong admin-loyalty.jsp đã hiện danh sách do
                         controller trả về.
                --%>
                <c:forEach var="tx" items="${transactions}">
                    <tr>

                        <%-- [TDLS03]
                             . Mã, thời gian, tên và số điện thoại của dòng này lấy từ tx trong transactions.
                               LoyaltyTransactionDAO.findDetailed (đọc lịch sử điểm kèm thông tin khách và đơn) đọc mã và
                               thời gian từ loyalty_transactions, ghép users để có tên và số điện thoại rồi controller đưa
                               danh sách về trang.
                             ... Kết thúc phần nhận diện giao dịch tại mã, thời gian và khách của đúng hàng trong
                                 bảng lịch sử.
                        --%>
                        <td class="mono text-muted">#${tx.id}</td>
                        <td class="mono text-muted"><fmt:formatDate value="${tx.createdAt}" pattern="dd/MM/yyyy HH:mm" /></td>
                        <td><span class="customer-name"><c:out value="${tx.customerName}" /></span><span class="customer-phone mono"><c:out value="${tx.customerPhone}" /></span></td>

                        <%-- [TDLS04]
                             . Loại giao dịch lấy từ transactionType do findDetailed (đọc lịch sử điểm kèm thông tin khách
                               và đơn) đọc: EARN là tích điểm, REDEEM là đổi điểm, REFUND là hoàn điểm, còn nhánh khác hiện
                               điều chỉnh. Màu nền dựa theo dấu của pointsChange, không quyết định lại loại giao dịch.
                             ... Kết thúc phân loại tại nhãn giao dịch và màu nền của hàng đang xem trên
                                 admin-loyalty.jsp.
                        --%>
                        <td><span class="type-badge ${tx.pointsChange gt 0 ? 'type-earn' : 'type-redeem'}"><c:choose><c:when test="${tx.transactionType eq 'EARN'}">EARN - Tích điểm</c:when><c:when test="${tx.transactionType eq 'REDEEM'}">REDEEM - Đổi điểm</c:when><c:when test="${tx.transactionType eq 'REFUND'}">REFUND - Hoàn điểm</c:when><c:otherwise>ADJUST - Điều chỉnh</c:otherwise></c:choose></span></td>
                        <%-- [TDLS05]
                             . Điểm biến động lấy từ points_change của giao dịch. Số dương có dấu cộng và màu cộng điểm, số
                               âm giữ dấu trừ và màu trừ điểm. Đây là lượng thay đổi của riêng giao dịch này, khác số dư
                               điểm hiện tại của khách.
                             ... Kết thúc phần biến động tại số điểm có dấu cộng hoặc trừ của chính giao dịch đang
                                 xem.
                        --%>
                        <td class="text-end mono ${tx.pointsChange gt 0 ? 'points-plus' : 'points-minus'}">${tx.pointsChange gt 0 ? '+' : ''}${tx.pointsChange}</td>

                        <%-- [TDSAU01]
                             . Cột Số dư sau hiện tx.balanceAfter do LoyaltyTransactionDAO.findDetailed (đọc lịch sử điểm
                               kèm thông tin khách và đơn) tính. Hàm đọc giao dịch mới nhất trước, bắt đầu từ điểm hiện tại
                               của từng khách rồi lần ngược qua các biến động để tính số dư sau từng giao dịch.
                             ... Đọc tiếp tại LoyaltyTransactionDAO.java, tìm [TDSAU02].
                        --%>
                        <td class="text-end mono fw-bold"><fmt:formatNumber value="${tx.balanceAfter}" pattern="#,##0" /></td>
                        <%-- [TDLS06]
                             . Mã đơn lấy từ orderCode mà findDetailed (đọc lịch sử điểm kèm thông tin khách và đơn) ghép
                               thêm từ bảng orders bằng order_id. Có mã thì trang hiện mã đơn; không có thì giao diện hiện
                               Điều chỉnh thủ công. Thao tác cộng/trừ tại màn hình này ghi ADJUST và không gắn mã đơn.
                             ... Kết thúc phần liên kết đơn tại mã đơn hoặc chữ Điều chỉnh thủ công của chính hàng
                                 lịch sử đó.
                        --%>
                        <td class="text-muted"><c:choose><c:when test="${not empty tx.orderCode}"><c:out value="${tx.orderCode}" /></c:when><c:otherwise>Điều chỉnh thủ công</c:otherwise></c:choose></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty transactions}"><tr><td colspan="7" class="text-center text-muted py-4">Chưa có giao dịch điểm.</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </section>
</main>
<%-- . Ba ô quy tắc được kiểm tra ngay trên màn hình bằng tệp bên dưới, trước khi gửi sang controller.
     ... Xem phần kiểm tra số quy tắc tại assets/js/admin-form-validation.js, tìm [TDKT01].
--%>
<script src="${ctx}/assets/js/admin-form-validation.js"></script>
<script>
    // [TDDC01]
    // . Trong submitAdjustment của admin-loyalty.jsp, em nhận add hoặc subtract rồi mở hộp hỏi số điểm. Bấm
    //   Hủy thì dừng; nhập số nguyên từ 1 đến 1.000.000 thì em điền mã khách, điểm và loại thao tác vào
    //   form, gửi đến AdminLoyaltyController.doPost để kiểm tra và cập nhật.
    // ... Đọc tiếp tại AdminLoyaltyController.java, tìm [TDDC02].
    function submitAdjustment(type) {

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
