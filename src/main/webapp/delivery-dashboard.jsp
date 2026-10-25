<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- Trang chỉ hiển thị qua DeliveryController (/delivery) --%>
<c:if test="${empty tab}"><c:redirect url="/delivery" /></c:if>
<c:set var="csrf" value="${fn:escapeXml(sessionScope.deliveryCsrfToken)}" />
<c:set var="me" value="${sessionScope.user}" />
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
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/delivery.css" rel="stylesheet">
</head>
<body>
<div class="layout">
    <aside class="ship-sidebar">
        <div class="brand">Aura Café<span class="portal">Delivery Portal</span></div>
        <div class="profile">
            <div class="d-flex gap-3 align-items-center">
                <div class="avatar"><c:out value="${fn:substring(me.fullName, 0, 1)}" /></div>
                <div>
                    <strong><c:out value="${me.fullName}" /></strong>
                    <div class="muted font-monospace"><c:out value="${me.phone}" /></div>
                </div>
            </div>
            <hr>
            <div class="d-flex justify-content-between"><span class="muted">Chức vụ:</span><strong>Giao hàng</strong></div>
            <div class="d-flex justify-content-between mt-1"><span class="muted">Hôm nay:</span><strong>${todayDisplay}</strong></div>
        </div>
        <nav>
            <a class="nav-link ${tab eq 'active' and empty detail ? 'active' : ''}" href="${ctx}/delivery">
                <span class="material-symbols-outlined">local_shipping</span>Đơn được phân công
                <span class="count">${fn:length(activeDeliveries)}</span>
            </a>
            <a class="nav-link ${tab eq 'done' and empty detail ? 'active' : ''}" href="${ctx}/delivery?tab=done">
                <span class="material-symbols-outlined">task_alt</span>Đã giao
                <span class="count">${deliveredTodayCount}</span>
            </a>
            <a class="nav-link ${tab eq 'cod' and empty detail ? 'active' : ''}" href="${ctx}/delivery?tab=cod">
                <span class="material-symbols-outlined">payments</span>Tiền COD
                <span class="count">${fn:length(codHolding)}</span>
            </a>
        </nav>
        <a class="logout" href="${ctx}/login?action=logout"><span class="material-symbols-outlined">logout</span>Đăng xuất</a>
    </aside>

    <main class="main">
        <c:if test="${not empty deliverySuccessMsg}">
            <div class="flash ok"><span class="material-symbols-outlined">check_circle</span><c:out value="${deliverySuccessMsg}" /></div>
        </c:if>
        <c:if test="${not empty deliveryErrorMsg}">
            <div class="flash err"><span class="material-symbols-outlined">error</span><c:out value="${deliveryErrorMsg}" /></div>
        </c:if>

        <section class="head mb-4">
            <span class="screen">Screen 14 • Giao hàng</span>
            <h1>Quản lý giao hàng</h1>
            <p class="muted mb-0">Nhận đơn được phân công, lấy hàng khi quán pha xong, giao cho khách, báo sự cố và nộp tiền COD cho Thu ngân.</p>
            <div class="stats">
                <div class="stat"><span>Chờ nhận đơn</span><strong>${assignedCount} đơn</strong></div>
                <div class="stat"><span>Đã nhận / đang giao</span><strong>${inProgressCount} đơn</strong></div>
                <div class="stat"><span>Đã giao hôm nay</span><strong>${deliveredTodayCount} đơn</strong></div>
                <div class="stat highlight"><span>Tiền COD đang giữ</span><strong><fmt:formatNumber value="${codHoldingTotal}" pattern="#,##0" />đ</strong></div>
            </div>
        </section>

        <c:choose>
            <%-- ================= UC28: CHI TIẾT ĐƠN GIAO ================= --%>
            <c:when test="${not empty detail}">
                <c:set var="d" value="${detail}" />
                <a class="action mb-3" href="${ctx}/delivery${d.delivered ? '?tab=done' : ''}">
                    <span class="material-symbols-outlined">arrow_back</span>Quay lại danh sách
                </a>
                <div class="row g-4 align-items-start">
                    <div class="col-lg-7">
                        <section class="panel p-4">
                            <div class="d-flex flex-wrap justify-content-between gap-2 border-bottom pb-3 mb-3">
                                <div class="d-flex flex-wrap gap-2 align-items-center">
                                    <span class="code">#<c:out value="${d.orderCode}" /></span>
                                    <span class="badge-st st-${d.status}">${d.statusLabel}</span>
                                    <span class="badge-st ost ost-${d.orderStatus}">Quán: ${d.orderStatusLabel}</span>
                                </div>
                                <span class="muted font-monospace">Đặt lúc ${d.orderCreatedAtDisplay}</span>
                            </div>

                            <h2 class="recipient"><c:out value="${d.recipientName}" /></h2>
                            <p><a class="tel" href="tel:${fn:escapeXml(d.recipientPhone)}"><span class="material-symbols-outlined">call</span> <c:out value="${d.recipientPhone}" /></a>
                                <c:if test="${not empty d.customerName and d.customerName ne d.recipientName}">
                                    <span class="muted">· Người đặt: <c:out value="${d.customerName}" /></span>
                                </c:if>
                            </p>
                            <c:url var="mapUrl" value="https://www.google.com/maps/search/"><c:param name="api" value="1" /><c:param name="query" value="${d.deliveryAddress}" /></c:url>
                            <p class="address mt-2"><span class="material-symbols-outlined">location_on</span>
                                <c:out value="${d.deliveryAddress}" /> · <a href="${mapUrl}" target="_blank" rel="noopener">Mở bản đồ</a></p>
                            <p><span class="material-symbols-outlined">schedule</span>
                                Thời gian nhận: <strong>${empty d.scheduledAt ? 'Càng sớm càng tốt' : 'Hẹn '.concat(d.scheduledAtDisplay)}</strong></p>
                            <c:if test="${not empty d.orderNote}">
                                <p class="note-box mt-2">Ghi chú của khách: <c:out value="${d.orderNote}" /></p>
                            </c:if>

                            <h3 class="h6 fw-bold mt-4">Món trong đơn (${d.itemCount})</h3>
                            <div class="items">
                                <c:forEach var="it" items="${d.items}">
                                    <div class="d-flex justify-content-between gap-2">
                                        <span><strong>${it.quantity}x</strong> <c:out value="${it.productName}" /></span>
                                        <span class="muted"><c:out value="${it.optionsText}" /></span>
                                    </div>
                                </c:forEach>
                            </div>

                            <div class="row g-3 mt-1">
                                <div class="col-sm-6">
                                    <div class="cod ${d.cod ? '' : 'paid'}">
                                        <small>${d.cod ? 'Thu tiền mặt (COD)' : 'Thanh toán'}</small>
                                        <strong>
                                            <c:choose>
                                                <c:when test="${d.cod}"><fmt:formatNumber value="${d.codAmount}" pattern="#,##0" />đ</c:when>
                                                <c:otherwise>Đã thanh toán online</c:otherwise>
                                            </c:choose>
                                        </strong>
                                        <small>Tổng đơn <fmt:formatNumber value="${d.finalAmount}" pattern="#,##0" />đ (phí giao <fmt:formatNumber value="${d.deliveryFee}" pattern="#,##0" />đ)</small>
                                    </div>
                                </div>
                                <div class="col-sm-6">
                                    <ul class="timeline">
                                        <li><span>Được phân công</span><strong>${d.assignedAtDisplay}</strong></li>
                                        <li class="${empty d.acceptedAt ? 'pending' : ''}"><span>Nhận đơn</span><strong>${empty d.acceptedAt ? '—' : d.acceptedAtDisplay}</strong></li>
                                        <li class="${empty d.pickedUpAt ? 'pending' : ''}"><span>Lấy hàng</span><strong>${empty d.pickedUpAt ? '—' : d.pickedUpAtDisplay}</strong></li>
                                        <li class="${empty d.deliveredAt ? 'pending' : ''}"><span>Giao xong</span><strong>${empty d.deliveredAt ? '—' : d.deliveredAtDisplay}</strong></li>
                                    </ul>
                                </div>
                            </div>
                            <c:if test="${not empty d.deliveryNote}">
                                <p class="muted mt-3">Ghi chú giao hàng: <c:out value="${d.deliveryNote}" /></p>
                            </c:if>

                            <div class="actions">
                                <c:set var="card" value="${d}" scope="request" />
                                <jsp:include page="/components/delivery-actions.jsp" />
                            </div>
                        </section>
                    </div>

                    <div class="col-lg-5 d-grid gap-4">
                        <c:if test="${not d.delivered}">
                            <section class="panel p-4" id="report">
                                <h2 class="h6 fw-bold mb-3"><span class="material-symbols-outlined text-danger">report_problem</span> Báo sự cố giao hàng</h2>
                                <form method="post" action="${ctx}/delivery">
                                    <input type="hidden" name="csrfToken" value="${csrf}">
                                    <input type="hidden" name="action" value="report">
                                    <input type="hidden" name="id" value="${d.id}">
                                    <label class="form-label fw-semibold" for="issueType">Loại sự cố</label>
                                    <select class="form-select mb-3" id="issueType" name="issueType" required>
                                        <option value="">-- Chọn loại sự cố --</option>
                                        <c:forEach var="t" items="${issueTypes}">
                                            <option value="${t.key}">${t.value}</option>
                                        </c:forEach>
                                    </select>
                                    <label class="form-label fw-semibold" for="issueDescription">Mô tả</label>
                                    <textarea class="form-control mb-3" id="issueDescription" name="description" rows="3" maxlength="500" required
                                              placeholder="VD: Gọi 3 lần không nghe máy, đã chờ 10 phút tại toà nhà..."></textarea>
                                    <button type="submit" class="primary w-100 justify-content-center">
                                        <span class="material-symbols-outlined">send</span>Gửi báo cáo sự cố
                                    </button>
                                </form>
                            </section>
                        </c:if>
                        <section class="panel p-4">
                            <h2 class="h6 fw-bold mb-3">Sự cố đã báo (${fn:length(d.issues)})</h2>
                            <c:choose>
                                <c:when test="${empty d.issues}"><p class="muted mb-0">Chưa có sự cố nào.</p></c:when>
                                <c:otherwise>
                                    <c:forEach var="issue" items="${d.issues}">
                                        <div class="issue">
                                            <div class="d-flex justify-content-between gap-2">
                                                <strong>${issue.issueTypeLabel}</strong>
                                                <span class="muted">${issue.createdAtDisplay}</span>
                                            </div>
                                            <div><c:out value="${issue.description}" /></div>
                                        </div>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </section>
                    </div>
                </div>
            </c:when>

            <%-- ================= UC26/UC27: ĐƠN ĐƯỢC PHÂN CÔNG ================= --%>
            <c:when test="${tab eq 'active'}">
                <div class="section-title"><h2>Đơn được phân công (${fn:length(activeDeliveries)})</h2></div>
                <c:choose>
                    <c:when test="${empty activeDeliveries}">
                        <section class="panel empty">
                            <span class="material-symbols-outlined">inventory_2</span>
                            Chưa có đơn nào được phân công. Quản lý gán đơn ở màn Quản lý đơn hàng.
                        </section>
                    </c:when>
                    <c:otherwise>
                        <div class="d-grid gap-3">
                            <c:forEach var="d" items="${activeDeliveries}">
                                <section class="order ${d.delivering ? 'is-delivering' : ''}">
                                    <div class="d-flex flex-wrap justify-content-between gap-2 border-bottom pb-2 mb-3">
                                        <div class="d-flex flex-wrap gap-2 align-items-center">
                                            <span class="code">#<c:out value="${d.orderCode}" /></span>
                                            <span class="badge-st st-${d.status}">${d.statusLabel}</span>
                                            <span class="badge-st ost ost-${d.orderStatus}">Quán: ${d.orderStatusLabel}</span>
                                        </div>
                                        <span class="muted font-monospace">${empty d.scheduledAt ? 'Giao ngay' : 'Hẹn '.concat(d.scheduledAtDisplay)} · đặt ${d.orderCreatedAtDisplay}</span>
                                    </div>
                                    <div class="row g-3">
                                        <div class="col-md-8">
                                            <p class="recipient"><c:out value="${d.recipientName}" />
                                                <a class="tel small ms-1" href="tel:${fn:escapeXml(d.recipientPhone)}">☎ <c:out value="${d.recipientPhone}" /></a></p>
                                            <p class="address"><strong>Địa chỉ:</strong> <c:out value="${d.deliveryAddress}" /></p>
                                            <c:if test="${not empty d.orderNote}"><p class="note-box mt-2">Ghi chú: <c:out value="${d.orderNote}" /></p></c:if>
                                            <div class="items mt-2">
                                                <c:forEach var="it" items="${d.items}">
                                                    <div class="d-flex justify-content-between gap-2">
                                                        <span><strong>${it.quantity}x</strong> <c:out value="${it.productName}" /></span>
                                                        <span class="muted"><c:out value="${it.optionsText}" /></span>
                                                    </div>
                                                </c:forEach>
                                            </div>
                                        </div>
                                        <div class="col-md-4">
                                            <div class="cod ${d.cod ? '' : 'paid'}">
                                                <small>${d.cod ? 'Thu tiền mặt (COD)' : 'Thanh toán'}</small>
                                                <strong>
                                                    <c:choose>
                                                        <c:when test="${d.cod}"><fmt:formatNumber value="${d.codAmount}" pattern="#,##0" />đ</c:when>
                                                        <c:otherwise>Đã thanh toán online</c:otherwise>
                                                    </c:choose>
                                                </strong>
                                                <small>Tổng đơn <fmt:formatNumber value="${d.finalAmount}" pattern="#,##0" />đ</small>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="actions">
                                        <a class="action" href="${ctx}/delivery?id=${d.id}"><span class="material-symbols-outlined">info</span>Chi tiết</a>
                                        <a class="action danger" href="${ctx}/delivery?id=${d.id}#report"><span class="material-symbols-outlined">report_problem</span>Báo sự cố</a>
                                        <c:set var="card" value="${d}" scope="request" />
                                        <jsp:include page="/components/delivery-actions.jsp" />
                                    </div>
                                </section>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </c:when>

            <%-- ================= ĐÃ GIAO ================= --%>
            <c:when test="${tab eq 'done'}">
                <section class="panel p-4">
                    <div class="section-title">
                        <h2>Đơn đã giao ngày ${selectedDateDisplay} (${fn:length(deliveredList)})</h2>
                        <form method="get" action="${ctx}/delivery" class="d-flex gap-2">
                            <input type="hidden" name="tab" value="done">
                            <input type="date" class="form-control form-control-sm" name="date" value="${selectedDate}">
                            <button class="action" type="submit">Xem</button>
                        </form>
                    </div>
                    <c:choose>
                        <c:when test="${empty deliveredList}"><p class="muted mb-0">Không có đơn nào được giao trong ngày này.</p></c:when>
                        <c:otherwise>
                            <div class="table-responsive">
                                <table class="table-ship">
                                    <thead><tr><th>Mã đơn</th><th>Người nhận</th><th>Địa chỉ</th><th>Giao lúc</th><th class="text-end">COD đã thu</th><th>Nộp tiền</th></tr></thead>
                                    <tbody>
                                    <c:forEach var="d" items="${deliveredList}">
                                        <tr>
                                            <td><a href="${ctx}/delivery?id=${d.id}" class="fw-bold text-decoration-none" style="color:#a51225">#<c:out value="${d.orderCode}" /></a></td>
                                            <td><c:out value="${d.recipientName}" /><div class="muted"><c:out value="${d.recipientPhone}" /></div></td>
                                            <td><c:out value="${d.deliveryAddress}" /></td>
                                            <td>${d.deliveredAtDisplay}</td>
                                            <td class="text-end money">
                                                <c:choose>
                                                    <c:when test="${d.cod}"><fmt:formatNumber value="${d.codCollected}" pattern="#,##0" />đ</c:when>
                                                    <c:otherwise><span class="muted">Đã trả online</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${not d.cod}">—</c:when>
                                                    <c:when test="${d.codHolding}"><span class="badge-st st-ASSIGNED">Đang giữ</span></c:when>
                                                    <c:otherwise><span class="badge-st st-DELIVERED">Đã nộp</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>
            </c:when>

            <%-- ================= UC30: TIỀN COD ================= --%>
            <c:otherwise>
                <div class="row g-4 align-items-start">
                    <div class="col-lg-7">
                        <section class="panel p-4">
                            <div class="section-title"><h2>Tiền COD đang giữ (${fn:length(codHolding)} đơn)</h2></div>
                            <c:choose>
                                <c:when test="${empty codHolding}"><p class="muted mb-0">Bạn không giữ khoản tiền COD nào.</p></c:when>
                                <c:otherwise>
                                    <div class="table-responsive">
                                        <table class="table-ship">
                                            <thead><tr><th>Mã đơn</th><th>Người nhận</th><th>Giao lúc</th><th class="text-end">Đã thu</th></tr></thead>
                                            <tbody>
                                            <c:forEach var="d" items="${codHolding}">
                                                <tr>
                                                    <td>#<c:out value="${d.orderCode}" /></td>
                                                    <td><c:out value="${d.recipientName}" /></td>
                                                    <td>${d.deliveredAtDisplay}</td>
                                                    <td class="text-end money"><fmt:formatNumber value="${d.codCollected}" pattern="#,##0" />đ</td>
                                                </tr>
                                            </c:forEach>
                                            </tbody>
                                        </table>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </section>
                    </div>
                    <div class="col-lg-5">
                        <section class="panel p-4">
                            <h2 class="h6 fw-bold">Nộp tiền COD về quỹ</h2>
                            <div class="cod my-3"><small>Tổng cần nộp</small><strong><fmt:formatNumber value="${codHoldingTotal}" pattern="#,##0" />đ</strong></div>
                            <form method="post" action="${ctx}/delivery" id="handoverForm"
                                  data-total="<fmt:formatNumber value='${codHoldingTotal}' pattern='#,##0' />">
                                <input type="hidden" name="csrfToken" value="${csrf}">
                                <input type="hidden" name="action" value="handover">
                                <label class="form-label fw-semibold" for="cashierId">Thu ngân nhận tiền</label>
                                <select class="form-select mb-3" id="cashierId" name="cashierId" required ${empty codHolding ? 'disabled' : ''}>
                                    <option value="">-- Chọn Thu ngân --</option>
                                    <c:forEach var="cs" items="${cashiers}">
                                        <option value="${cs.id}"><c:out value="${cs.fullName}" /></option>
                                    </c:forEach>
                                </select>
                                <button type="submit" class="primary w-100 justify-content-center" ${empty codHolding ? 'disabled' : ''}>
                                    <span class="material-symbols-outlined">payments</span>Xác nhận đã nộp tiền
                                </button>
                            </form>
                        </section>
                    </div>
                </div>

                <section class="panel p-4 mt-4">
                    <div class="section-title"><h2>Lịch sử nộp tiền COD</h2></div>
                    <c:choose>
                        <c:when test="${empty codHistory}"><p class="muted mb-0">Chưa có lần nộp tiền nào.</p></c:when>
                        <c:otherwise>
                            <div class="table-responsive">
                                <table class="table-ship">
                                    <thead><tr><th>Mã đơn</th><th>Đã thu</th><th>Nộp lúc</th><th>Thu ngân nhận</th></tr></thead>
                                    <tbody>
                                    <c:forEach var="d" items="${codHistory}">
                                        <tr>
                                            <td>#<c:out value="${d.orderCode}" /></td>
                                            <td class="money"><fmt:formatNumber value="${d.codCollected}" pattern="#,##0" />đ</td>
                                            <td>${d.codHandedOverAtDisplay}</td>
                                            <td><c:out value="${d.codReceiverName}" /></td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>
            </c:otherwise>
        </c:choose>
    </main>
</div>

<script>
    (() => {
        // Xác nhận trước các thao tác làm thay đổi trạng thái đơn
        document.querySelectorAll('form[data-confirm]').forEach((form) => {
            form.addEventListener('submit', (event) => {
                if (!confirm(form.dataset.confirm)) event.preventDefault();
            });
        });
        const handover = document.getElementById('handoverForm');
        if (handover) {
            handover.addEventListener('submit', (event) => {
                if (!confirm('Xác nhận đã nộp ' + handover.dataset.total + 'đ tiền COD cho Thu ngân?')) event.preventDefault();
            });
        }
    })();
</script>
</body>
</html>
