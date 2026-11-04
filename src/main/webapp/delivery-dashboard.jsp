<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- Trang chỉ hiển thị qua DeliveryController (/delivery) --%>
<c:if test="${empty tab}"><c:redirect url="/delivery" /></c:if>
<c:set var="csrf" value="${fn:escapeXml(sessionScope.deliveryCsrfToken)}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Giao hàng | Aura Café</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <link href="${ctx}/assets/css/delivery.css" rel="stylesheet">
</head>
<body>
<header class="topbar">
    <div class="brand">Aura Café <span>Giao hàng</span></div>
    <div class="who">
        <span class="material-symbols-outlined">account_circle</span><c:out value="${sessionScope.user.fullName}" />
        <a class="logout" href="${ctx}/login?action=logout" title="Đăng xuất"><span class="material-symbols-outlined">logout</span></a>
    </div>
</header>

<main class="page">
    <%-- Quy trình 3 bước, mỗi bước là 1 tab --%>
    <nav class="steps" aria-label="Quy trình giao hàng">
        <a class="step ${tab eq 'available' ? 'active' : ''}" href="${ctx}/delivery?tab=available">
            <b>1</b><span>Nhận đơn<small>${fn:length(availableOrders)} đơn chờ</small></span>
        </a>
        <a class="step ${tab eq 'delivering' ? 'active' : ''}" href="${ctx}/delivery?tab=delivering">
            <b>2</b><span>Giao &amp; thu tiền<small>${fn:length(activeDeliveries)} đơn của tôi</small></span>
        </a>
        <a class="step ${tab eq 'cod' ? 'active' : ''}" href="${ctx}/delivery?tab=cod">
            <b>3</b><span>Nộp tiền COD<small><fmt:formatNumber value="${codHoldingTotal}" pattern="#,##0" />đ đang giữ</small></span>
        </a>
    </nav>

    <c:if test="${not empty deliverySuccessMsg}">
        <div class="flash ok"><span class="material-symbols-outlined">check_circle</span><c:out value="${deliverySuccessMsg}" /></div>
    </c:if>
    <c:if test="${not empty deliveryErrorMsg}">
        <div class="flash err"><span class="material-symbols-outlined">error</span><c:out value="${deliveryErrorMsg}" /></div>
    </c:if>

    <c:choose>
        <%-- ============ BƯỚC 1: ĐƠN CHỜ NHẬN (UC26, UC27) ============ --%>
        <c:when test="${tab eq 'available'}">
            <p class="guide">Đơn quán đã pha xong, chưa ai nhận. Đang ở quán thì bấm <b>Nhận đơn &amp; đi giao</b> rồi mang hàng đi luôn.
                <span class="muted">Trang tự làm mới mỗi 30 giây.</span></p>
            <c:if test="${empty availableOrders}">
                <div class="empty"><span class="material-symbols-outlined">coffee</span>Chưa có đơn nào pha xong chờ giao.</div>
            </c:if>
            <c:forEach var="o" items="${availableOrders}">
                <article class="card">
                    <div class="card-head">
                        <span class="code">#<c:out value="${o.orderCode}" /></span>
                        <span class="muted">Đặt lúc ${o.orderCreatedAtDisplay}</span>
                    </div>
                    <p class="name"><c:out value="${o.recipientName}" /></p>
                    <p class="line"><span class="material-symbols-outlined">location_on</span><c:out value="${o.deliveryAddress}" /></p>
                    <div class="items">
                        <c:forEach var="it" items="${o.items}">
                            <div><b>${it.quantity}x</b> <c:out value="${it.productName}" /> <span class="muted"><c:out value="${it.optionsText}" /></span></div>
                        </c:forEach>
                    </div>
                    <c:if test="${not empty o.orderNote}"><p class="note">Ghi chú: <c:out value="${o.orderNote}" /></p></c:if>
                    <div class="money ${o.cod ? 'cod' : 'paid'}">
                        <c:choose>
                            <c:when test="${o.cod}">Thu của khách <b><fmt:formatNumber value="${o.codAmount}" pattern="#,##0" />đ</b></c:when>
                            <c:otherwise>Khách đã trả online - <b>không thu tiền</b></c:otherwise>
                        </c:choose>
                    </div>
                    <form method="post" action="${ctx}/delivery" data-confirm="Nhận đơn #${fn:escapeXml(o.orderCode)} và mang đi giao?">
                        <input type="hidden" name="csrfToken" value="${csrf}">
                        <input type="hidden" name="action" value="claim">
                        <input type="hidden" name="orderId" value="${o.orderId}">
                        <button type="submit" class="btn primary"><span class="material-symbols-outlined">two_wheeler</span>Nhận đơn &amp; đi giao</button>
                    </form>
                </article>
            </c:forEach>
        </c:when>

        <%-- ============ BƯỚC 2: ĐANG GIAO - GIAO XONG + THU COD, BÁO SỰ CỐ (UC27, UC28, UC29) ============ --%>
        <c:when test="${tab eq 'delivering'}">
            <p class="guide">Giao tới khách rồi bấm <b>Đã giao xong</b>. Đơn thu tiền mặt phải tick <b>đã thu đủ tiền</b> trước.
                Không giao được thì bấm <b>Gặp sự cố</b>: đơn sẽ bị <b>huỷ</b> và mang hàng về quán.</p>
            <c:if test="${empty activeDeliveries}">
                <div class="empty"><span class="material-symbols-outlined">two_wheeler</span>
                    Bạn chưa nhận đơn nào. Sang <a href="${ctx}/delivery?tab=available">bước 1</a> để nhận đơn.</div>
            </c:if>
            <c:forEach var="d" items="${activeDeliveries}">
                <article class="card ${d.delivering ? 'on-the-way' : ''}">
                    <div class="card-head">
                        <span class="code">#<c:out value="${d.orderCode}" /></span>
                        <span class="badge ${d.delivering ? 'go' : 'wait'}">${d.delivering ? 'Đang giao' : 'Chờ lấy hàng'}</span>
                    </div>
                    <p class="name"><c:out value="${d.recipientName}" /></p>
                    <p class="line"><span class="material-symbols-outlined">call</span><b><c:out value="${d.recipientPhone}" /></b></p>
                    <p class="line"><span class="material-symbols-outlined">location_on</span><c:out value="${d.deliveryAddress}" /></p>
                    <div class="items">
                        <c:forEach var="it" items="${d.items}">
                            <div><b>${it.quantity}x</b> <c:out value="${it.productName}" /> <span class="muted"><c:out value="${it.optionsText}" /></span></div>
                        </c:forEach>
                    </div>
                    <c:if test="${not empty d.orderNote}"><p class="note">Ghi chú: <c:out value="${d.orderNote}" /></p></c:if>
                    <div class="money ${d.cod ? 'cod' : 'paid'}">
                        <c:choose>
                            <c:when test="${d.cod}">Thu của khách <b><fmt:formatNumber value="${d.codAmount}" pattern="#,##0" />đ</b></c:when>
                            <c:otherwise>Khách đã trả online - <b>không thu tiền</b></c:otherwise>
                        </c:choose>
                    </div>

                    <c:choose>
                        <c:when test="${d.delivering}">
                            <form method="post" action="${ctx}/delivery" class="done-form" data-confirm="Xác nhận đã giao xong đơn #${fn:escapeXml(d.orderCode)}?">
                                <input type="hidden" name="csrfToken" value="${csrf}">
                                <input type="hidden" name="action" value="deliver">
                                <input type="hidden" name="id" value="${d.id}">
                                <c:if test="${d.cod}">
                                    <label class="check"><input type="checkbox" name="codConfirmed" required>
                                        Tôi đã thu đủ <b><fmt:formatNumber value="${d.codAmount}" pattern="#,##0" />đ</b> tiền mặt</label>
                                </c:if>
                                <button type="submit" class="btn primary"><span class="material-symbols-outlined">task_alt</span>Đã giao xong</button>
                            </form>
                        </c:when>
                        <c:when test="${d.orderReady}">
                            <form method="post" action="${ctx}/delivery" data-confirm="Nhận đơn #${fn:escapeXml(d.orderCode)} và mang đi giao?">
                                <input type="hidden" name="csrfToken" value="${csrf}">
                                <input type="hidden" name="action" value="start">
                                <input type="hidden" name="id" value="${d.id}">
                                <button type="submit" class="btn primary"><span class="material-symbols-outlined">two_wheeler</span>Nhận đơn &amp; đi giao</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <p class="waiting"><span class="material-symbols-outlined">hourglass_top</span>Quán đang pha chế (${d.orderStatusLabel}), pha xong mới lấy hàng được.</p>
                        </c:otherwise>
                    </c:choose>

                    <details class="issue-box">
                        <summary><span class="material-symbols-outlined">report_problem</span>Gặp sự cố?
                            <c:if test="${not empty d.issues}"><span class="muted">(đã báo ${fn:length(d.issues)})</span></c:if></summary>
                        <c:forEach var="issue" items="${d.issues}">
                            <p class="issue"><b>${issue.issueTypeLabel}</b> · ${issue.createdAtDisplay}<br><c:out value="${issue.description}" /></p>
                        </c:forEach>
                        <form method="post" action="${ctx}/delivery" data-confirm="Báo sự cố sẽ HUỶ đơn #${fn:escapeXml(d.orderCode)} (không giao nữa). Tiếp tục?">
                            <input type="hidden" name="csrfToken" value="${csrf}">
                            <input type="hidden" name="action" value="report">
                            <input type="hidden" name="id" value="${d.id}">
                            <select name="issueType" required aria-label="Loại sự cố">
                                <option value="">-- Chọn loại sự cố --</option>
                                <c:forEach var="t" items="${issueTypes}"><option value="${t.key}">${t.value}</option></c:forEach>
                            </select>
                            <textarea name="description" rows="2" maxlength="500" required aria-label="Mô tả sự cố"
                                      placeholder="VD: Gọi 3 lần không nghe máy, đã chờ 10 phút..."></textarea>
                            <button type="submit" class="btn danger"><span class="material-symbols-outlined">cancel</span>Báo sự cố &amp; huỷ đơn</button>
                        </form>
                    </details>
                </article>
            </c:forEach>
        </c:when>

        <%-- ============ BƯỚC 3: NỘP TIỀN COD (UC30) ============ --%>
        <c:otherwise>
            <p class="guide">Cuối ca, đưa toàn bộ tiền mặt đã thu cho Thu ngân rồi bấm <b>Đã nộp tiền</b>.</p>
            <section class="card">
                <div class="holding">
                    <span>Tiền mặt đang giữ (${fn:length(codHolding)} đơn)</span>
                    <b><fmt:formatNumber value="${codHoldingTotal}" pattern="#,##0" />đ</b>
                </div>
                <c:choose>
                    <c:when test="${empty codHolding}"><p class="muted center">Bạn không giữ khoản tiền nào cần nộp.</p></c:when>
                    <c:otherwise>
                        <form method="post" action="${ctx}/delivery" class="handover"
                              data-confirm="Xác nhận đã nộp <fmt:formatNumber value='${codHoldingTotal}' pattern='#,##0' />đ cho Thu ngân?">
                            <input type="hidden" name="csrfToken" value="${csrf}">
                            <input type="hidden" name="action" value="handover">
                            <select name="cashierId" required aria-label="Thu ngân nhận tiền">
                                <option value="">-- Chọn Thu ngân nhận tiền --</option>
                                <c:forEach var="cs" items="${cashiers}"><option value="${cs.id}"><c:out value="${cs.fullName}" /></option></c:forEach>
                            </select>
                            <button type="submit" class="btn primary"><span class="material-symbols-outlined">payments</span>Đã nộp tiền</button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </section>

            <%-- Đơn đã giao xem theo từng ngày (mặc định hôm nay) --%>
            <form method="get" action="${ctx}/delivery" class="day-filter">
                <input type="hidden" name="tab" value="cod">
                <h2 class="sub">Đơn đã giao ngày</h2>
                <input type="date" name="date" value="${selectedDate}" max="${today}" aria-label="Chọn ngày" onchange="this.form.submit()">
                <c:if test="${not isToday}"><a href="${ctx}/delivery?tab=cod">Hôm nay</a></c:if>
            </form>
            <p class="day-sum">${selectedDateDisplay}: <b>${fn:length(deliveredList)} đơn</b>
                · thu COD <b><fmt:formatNumber value="${deliveredCodTotal}" pattern="#,##0" />đ</b></p>
            <c:if test="${empty deliveredList}"><p class="muted">Không có đơn nào giao xong trong ngày này.</p></c:if>
            <c:forEach var="d" items="${deliveredList}">
                <div class="row-done">
                    <div><b>#<c:out value="${d.orderCode}" /></b> · <c:out value="${d.recipientName}" /><br>
                        <span class="muted">Giao lúc ${d.deliveredAtDisplay}</span></div>
                    <div class="right">
                        <c:choose>
                            <c:when test="${not d.cod}"><span class="badge paid">Trả online</span></c:when>
                            <c:otherwise>
                                <b><fmt:formatNumber value="${d.codCollected}" pattern="#,##0" />đ</b><br>
                                <span class="badge ${d.codHolding ? 'wait' : 'paid'}">${d.codHolding ? 'Đang giữ' : 'Đã nộp'}</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</main>

<script>
    (() => {
        // Hỏi lại trước các thao tác đổi trạng thái đơn / nộp tiền
        document.querySelectorAll('form[data-confirm]').forEach((form) => {
            form.addEventListener('submit', (event) => {
                if (!confirm(form.dataset.confirm)) event.preventDefault();
            });
        });
        // Bước 1: tự tải lại để thấy đơn Barista vừa pha xong
        <c:if test="${tab eq 'available'}">
        setTimeout(() => location.reload(), 30000);
        </c:if>
    })();
</script>
</body>
</html>
