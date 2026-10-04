<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Aura Café | Đơn hàng của tôi</title>

    <!-- Google Fonts & Icons -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet" />
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/cart.css" rel="stylesheet">
</head>
<body class="aura-cart-page">

    <jsp:include page="/components/header.jsp">
        <jsp:param name="activePage" value="orders" />
    </jsp:include>

    <main class="container-xl px-3 px-lg-4 py-4">

        <!-- Tiêu đề & Bộ lọc -->
        <div class="d-flex flex-column flex-lg-row justify-content-between align-items-lg-end gap-3 mb-3">
            <div>
                <div class="eyebrow">Theo dõi &amp; đối soát đơn</div>
                <h1 class="h3 fw-bold mb-1">Đơn hàng của tôi</h1>
                <div class="text-muted small">Theo dõi tiến trình pha chế, giao nhận và xem lại các đơn bạn đã đặt.</div>
            </div>
            <form method="get" action="${ctx}/orders" class="orders-filter">
                <input type="hidden" name="tab" value="${tab}">
                <div class="position-relative flex-grow-1">
                    <span class="material-symbols-outlined filter-icon">search</span>
                    <input type="text" name="q" value="${fn:escapeXml(keyword)}" maxlength="30"
                           class="form-control aura-input ps-5" placeholder="Tìm theo mã đơn">
                </div>
                <input type="date" name="date" value="${date}" class="form-control aura-input" title="Lọc theo ngày đặt" style="max-width: 170px;">
                <button type="submit" class="btn btn-aura px-3">
                    <span class="material-symbols-outlined fs-5">tune</span> Lọc
                </button>
                <c:if test="${not empty keyword || not empty date}">
                    <a href="${ctx}/orders?tab=${tab}" class="btn btn-soft px-3">Xoá lọc</a>
                </c:if>
            </form>
        </div>

        <c:if test="${not empty ordersMessage}">
            <div class="alert alert-success d-flex align-items-center gap-2">
                <span class="material-symbols-outlined">check_circle</span> <c:out value="${ordersMessage}" />
            </div>
        </c:if>
        <c:if test="${not empty ordersError}">
            <div class="alert alert-danger d-flex align-items-center gap-2">
                <span class="material-symbols-outlined">error</span> <c:out value="${ordersError}" />
            </div>
        </c:if>

        <!-- Tab trạng thái -->
        <div class="orders-tabs mb-4">
            <c:forEach var="t" items="${tabCounts}">
                <c:url var="tabUrl" value="/orders">
                    <c:param name="tab" value="${t.key}" />
                    <c:if test="${not empty keyword}"><c:param name="q" value="${keyword}" /></c:if>
                    <c:if test="${not empty date}"><c:param name="date" value="${date}" /></c:if>
                </c:url>
                <a href="${tabUrl}" class="orders-tab ${tab == t.key ? 'active' : ''}">
                    <c:choose>
                        <c:when test="${t.key == 'all'}">Tất cả đơn</c:when>
                        <c:when test="${t.key == 'pending'}">Chờ thanh toán</c:when>
                        <c:when test="${t.key == 'processing'}">Đang xử lý</c:when>
                        <c:when test="${t.key == 'delivering'}">Đang giao hàng</c:when>
                        <c:when test="${t.key == 'completed'}">Đã hoàn thành</c:when>
                        <c:otherwise>Đã hủy</c:otherwise>
                    </c:choose>
                    <span class="tab-count">${t.value}</span>
                </a>
            </c:forEach>
        </div>

        <%-- ===== Đơn đang thực hiện (đã thanh toán, đang pha chế / giao) ===== --%>
        <c:forEach var="o" items="${activeOrders}">
            <div class="aura-card track-card mb-4">
                <div class="track-header">
                    <div class="d-flex align-items-center gap-3">
                        <span class="material-symbols-outlined fs-2">${o.delivery ? 'delivery_dining' : 'local_cafe'}</span>
                        <div>
                            <div class="d-flex align-items-center gap-2 flex-wrap">
                                <span class="fw-bold fs-5">Đơn hàng #<c:out value="${o.orderCode}" /></span>
                                <span class="status-badge status-on-dark">${o.statusLabel}</span>
                            </div>
                            <div class="small opacity-75">
                                ${o.delivery ? 'Giao tận nơi' : 'Tự đến lấy tại quầy'} • Đặt lúc ${o.createdTimeText}, ${o.createdDateText}
                            </div>
                        </div>
                    </div>
                    <div class="text-lg-end">
                        <div class="text-uppercase opacity-75" style="font-size: 0.7rem; letter-spacing: 0.5px;">Thời gian nhận dự kiến</div>
                        <div class="fw-bold fs-4 lh-1">${o.estimatedReceiveText}</div>
                    </div>
                </div>

                <div class="p-3 p-md-4">
                    <div class="row g-4">
                        <div class="col-lg-8">
                            <div class="track-box">
                                <div class="d-flex justify-content-between align-items-center mb-3">
                                    <span class="fw-bold">Tiến trình đơn hàng</span>
                                    <span class="text-aura fw-bold" style="font-size: 0.75rem;">BƯỚC ${o.progressStep} / 4</span>
                                </div>
                                <div class="tracker">
                                    <%-- Đơn ở đây đều đã thanh toán; vừa thanh toán xong thì đang chờ Barista nhận đơn --%>
                                    <div class="tracker-step done">
                                        <span class="tracker-dot"><span class="material-symbols-outlined">receipt_long</span></span>
                                        <div class="tracker-label">Đã thanh toán</div>
                                        <div class="tracker-sub">${o.paidTimeText}</div>
                                    </div>
                                    <div class="tracker-step ${o.progressStep > 2 ? 'done' : 'current'}">
                                        <span class="tracker-dot"><span class="material-symbols-outlined">coffee_maker</span></span>
                                        <div class="tracker-label">Barista pha chế</div>
                                        <div class="tracker-sub">${o.progressStep == 2 ? 'Đang làm' : (o.progressStep > 2 ? 'Xong' : 'Chờ pha chế')}</div>
                                    </div>
                                    <div class="tracker-step ${o.progressStep > 3 ? 'done' : (o.progressStep == 3 ? 'current' : '')}">
                                        <span class="tracker-dot"><span class="material-symbols-outlined">${o.delivery ? 'two_wheeler' : 'storefront'}</span></span>
                                        <div class="tracker-label">${o.delivery ? 'Đang giao hàng' : 'Sẵn sàng tại quầy'}</div>
                                        <div class="tracker-sub">${o.status == 'READY' && o.delivery ? 'Chờ shipper' : ''}</div>
                                    </div>
                                    <div class="tracker-step ${o.progressStep == 4 ? 'done' : ''}">
                                        <span class="tracker-dot"><span class="material-symbols-outlined">${o.delivery ? 'flag' : 'task_alt'}</span></span>
                                        <div class="tracker-label">${o.delivery ? 'Giao thành công' : 'Đã nhận món'}</div>
                                        <div class="tracker-sub">Dự kiến ${o.estimatedReceiveText}</div>
                                    </div>
                                </div>
                            </div>

                            <div class="small-label mt-4">Danh sách món trong đơn</div>
                            <c:forEach var="item" items="${o.items}">
                                <div class="order-line">
                                    <c:choose>
                                        <c:when test="${not empty item.imageUrl}">
                                            <img src="${fn:escapeXml(item.imageUrl)}" alt="${fn:escapeXml(item.productName)}" class="order-item-img">
                                        </c:when>
                                        <c:otherwise>
                                            <span class="order-item-img d-inline-flex align-items-center justify-content-center text-aura">
                                                <span class="material-symbols-outlined">local_cafe</span>
                                            </span>
                                        </c:otherwise>
                                    </c:choose>
                                    <div class="flex-grow-1" style="min-width: 0;">
                                        <div class="fw-semibold"><c:out value="${item.productName}" /></div>
                                        <div class="text-muted" style="font-size: 0.78rem;"><c:out value="${item.optionsText}" /></div>
                                    </div>
                                    <div class="text-end">
                                        <div class="fw-semibold"><fmt:formatNumber value="${item.subtotal}" pattern="#,##0" />đ</div>
                                        <div class="text-muted" style="font-size: 0.75rem;">SL: <fmt:formatNumber value="${item.quantity}" minIntegerDigits="2" /></div>
                                    </div>
                                </div>
                            </c:forEach>
                            <div class="d-flex justify-content-between align-items-center pt-3">
                                <span class="text-muted small">
                                    Tổng thanh toán
                                    <c:choose>
                                        <c:when test="${o.delivery && o.deliveryFee > 0}">(đã gồm phí ship <fmt:formatNumber value="${o.deliveryFee}" pattern="#,##0" />đ</c:when>
                                        <c:when test="${o.delivery}">(miễn phí giao hàng</c:when>
                                        <c:otherwise>(nhận tại quầy</c:otherwise>
                                    </c:choose><c:if test="${o.discountAmount > 0}">, giảm <fmt:formatNumber value="${o.discountAmount}" pattern="#,##0" />đ</c:if>):
                                </span>
                                <span class="fw-bold fs-5 text-aura"><fmt:formatNumber value="${o.finalAmount}" pattern="#,##0" />đ</span>
                            </div>
                        </div>

                        <div class="col-lg-4">
                            <div class="track-info">
                                <div class="info-row">
                                    <span class="material-symbols-outlined">person</span>
                                    <div>
                                        <div class="bank-label">Người nhận</div>
                                        <div class="fw-semibold"><c:out value="${o.recipientName}" /> • <c:out value="${o.recipientPhone}" /></div>
                                    </div>
                                </div>
                                <div class="info-row">
                                    <span class="material-symbols-outlined">${o.delivery ? 'location_on' : 'storefront'}</span>
                                    <div>
                                        <div class="bank-label">${o.delivery ? 'Giao đến' : 'Nhận tại quầy'}</div>
                                        <div class="fw-semibold"><c:out value="${o.delivery ? o.deliveryAddress : storeAddress}" /></div>
                                    </div>
                                </div>
                                <div class="info-row">
                                    <span class="material-symbols-outlined">qr_code_2</span>
                                    <div>
                                        <div class="bank-label">Thanh toán</div>
                                        <div class="fw-semibold">${o.paymentMethodLabel}</div>
                                        <div class="text-muted" style="font-size: 0.75rem;">Lúc ${o.paidAtText}</div>
                                    </div>
                                </div>
                                <c:if test="${not empty o.voucherCode}">
                                    <div class="info-row">
                                        <span class="material-symbols-outlined">sell</span>
                                        <div>
                                            <div class="bank-label">Voucher</div>
                                            <div class="fw-semibold"><c:out value="${o.voucherCode}" /> (-<fmt:formatNumber value="${o.discountAmount}" pattern="#,##0" />đ)</div>
                                        </div>
                                    </div>
                                </c:if>
                                <c:if test="${not empty o.orderNote}">
                                    <div class="info-row">
                                        <span class="material-symbols-outlined">sticky_note_2</span>
                                        <div>
                                            <div class="bank-label">Ghi chú</div>
                                            <div class="fw-semibold"><c:out value="${o.orderNote}" /></div>
                                        </div>
                                    </div>
                                </c:if>
                                <div class="info-row">
                                    <span class="material-symbols-outlined" style="color: var(--aura-gold);">stars</span>
                                    <div>
                                        <div class="bank-label">Điểm Aura</div>
                                        <div class="fw-semibold">+${o.rewardPoints} điểm đã cộng</div>
                                    </div>
                                </div>
                                <div class="pt-2 border-top mt-2">
                                    <a href="${ctx}/bill?orderId=${o.id}" class="btn btn-outline-secondary btn-sm w-100 d-inline-flex align-items-center justify-content-center gap-1" target="_blank">
                                        <span class="material-symbols-outlined fs-6">receipt_long</span> Xem hóa đơn điện tử
                                    </a>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </c:forEach>

        <%-- ===== Lịch sử đơn hàng ===== --%>
        <c:if test="${not empty historyOrders}">
            <div class="d-flex justify-content-between align-items-center mb-3 mt-2">
                <h2 class="h5 fw-bold mb-0 d-flex align-items-center gap-2">
                    Lịch sử đơn hàng <span class="badge-step">${fn:length(historyOrders)} đơn</span>
                </h2>
            </div>

            <c:forEach var="o" items="${historyOrders}">
                <div class="aura-card history-card p-3 mb-3 ${o.cancelled ? 'is-cancelled' : ''}">
                    <div class="d-flex flex-column flex-md-row gap-3">
                        <c:choose>
                            <c:when test="${o.cancelled}">
                                <span class="history-img history-img-cancel"><span class="material-symbols-outlined">cancel</span></span>
                            </c:when>
                            <c:when test="${not empty o.firstImageUrl}">
                                <img src="${fn:escapeXml(o.firstImageUrl)}" alt="Đơn ${fn:escapeXml(o.orderCode)}" class="history-img">
                            </c:when>
                            <c:otherwise>
                                <span class="history-img"><span class="material-symbols-outlined">local_cafe</span></span>
                            </c:otherwise>
                        </c:choose>

                        <div class="flex-grow-1" style="min-width: 0;">
                            <div class="d-flex align-items-center gap-2 flex-wrap mb-1">
                                <span class="fw-bold">#<c:out value="${o.orderCode}" /></span>
                                <span class="status-badge status-${o.statusGroup}">${o.statusLabel}</span>
                                <span class="text-muted small">• ${o.createdDateText} - ${o.createdTimeText}</span>
                            </div>
                            <div class="history-items"><c:out value="${o.itemSummary}" /></div>
                            <div class="text-muted" style="font-size: 0.78rem;">
                                ${o.delivery ? 'Giao đến' : 'Nhận tại quầy'}: <c:out value="${o.delivery ? o.deliveryAddress : storeAddress}" />
                                • Thanh toán: ${o.paymentMethodLabel}
                                <c:if test="${not empty o.voucherCode}"> • Voucher <c:out value="${o.voucherCode}" /> (-<fmt:formatNumber value="${o.discountAmount}" pattern="#,##0" />đ)</c:if>
                                <c:if test="${o.completed && o.rewardPoints > 0}"> • Đã tích luỹ +${o.rewardPoints} điểm Aura</c:if>
                            </div>
                            <c:if test="${o.pending}">
                                <div class="info-box small mt-2 mb-0">
                                    Đơn đang chờ bạn chuyển khoản. Bar sẽ bắt đầu pha chế ngay sau khi thanh toán.
                                </div>
                            </c:if>
                            <c:if test="${o.cancelled}">
                                <div class="info-box small mt-2 mb-0">
                                    <c:choose>
                                        <c:when test="${not empty o.paidAt}">Đơn đã hủy sau khi thanh toán, vui lòng liên hệ quầy để được hoàn tiền.</c:when>
                                        <c:otherwise>Đơn đã hủy trước khi thanh toán nên không phát sinh chi phí.</c:otherwise>
                                    </c:choose>
                                    <c:if test="${not empty o.voucherCode}"> Mã <c:out value="${o.voucherCode}" /> đã được hoàn lại lượt dùng.</c:if>
                                </div>
                            </c:if>
                        </div>

                        <div class="text-md-end d-flex flex-column justify-content-between gap-2 flex-shrink-0">
                            <div>
                                <div class="text-muted text-uppercase" style="font-size: 0.68rem;">Tổng tiền</div>
                                <div class="fw-bold fs-5 ${o.cancelled ? 'text-muted text-decoration-line-through' : 'text-aura'}">
                                    <fmt:formatNumber value="${o.finalAmount}" pattern="#,##0" />đ
                                </div>
                            </div>
                            <div class="d-flex gap-2 justify-content-md-end flex-wrap">
                                <c:if test="${o.pending}">
                                    <form method="post" action="${ctx}/orders" class="m-0"
                                          onsubmit="return confirm('Hủy đơn #${fn:escapeXml(o.orderCode)}?');">
                                        <input type="hidden" name="action" value="cancel">
                                        <input type="hidden" name="orderId" value="${o.id}">
                                        <input type="hidden" name="tab" value="${tab}">
                                        <button type="submit" class="btn btn-soft btn-sm px-3">Hủy đơn</button>
                                    </form>
                                    <a href="${ctx}/payment?orderId=${o.id}" class="btn btn-aura btn-sm px-3">
                                        <span class="material-symbols-outlined fs-6">qr_code_2</span> Thanh toán ngay
                                    </a>
                                </c:if>
                                <c:if test="${o.completed}">
                                    <c:url var="feedbackUrl" value="/feedback">
                                        <c:param name="orderCode" value="${o.orderCode}" />
                                    </c:url>
                                    <a href="${feedbackUrl}" class="btn btn-soft btn-sm px-3">
                                        <span class="material-symbols-outlined fs-6">rate_review</span> Đánh giá
                                    </a>
                                </c:if>
                                <c:if test="${o.completed || not empty o.paidAt}">
                                    <a href="${ctx}/bill?orderId=${o.id}" class="btn btn-soft btn-sm px-3 d-inline-flex align-items-center gap-1" target="_blank">
                                        <span class="material-symbols-outlined fs-6">receipt_long</span> Hóa đơn
                                    </a>
                                </c:if>
                                <c:if test="${o.completed || o.cancelled}">
                                    <form method="post" action="${ctx}/orders" class="m-0">
                                        <input type="hidden" name="action" value="reorder">
                                        <input type="hidden" name="orderId" value="${o.id}">
                                        <input type="hidden" name="tab" value="${tab}">
                                        <button type="submit" class="btn btn-aura btn-sm px-3">
                                            <span class="material-symbols-outlined fs-6">replay</span> Đặt lại đơn này
                                        </button>
                                    </form>
                                </c:if>
                            </div>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </c:if>

        <%-- ===== Không có đơn ===== --%>
        <c:if test="${empty activeOrders && empty historyOrders}">
            <div class="aura-card p-5 text-center">
                <span class="empty-cart-icon mx-auto mb-3"><span class="material-symbols-outlined">receipt_long</span></span>
                <c:choose>
                    <c:when test="${not empty keyword || not empty date || tab != 'all'}">
                        <h2 class="h5 fw-bold">Không tìm thấy đơn hàng phù hợp</h2>
                        <p class="text-muted">Thử đổi tab, mã đơn hoặc ngày đặt khác.</p>
                        <a href="${ctx}/orders" class="btn btn-soft px-4">Xem tất cả đơn</a>
                    </c:when>
                    <c:otherwise>
                        <h2 class="h5 fw-bold">Bạn chưa có đơn hàng nào</h2>
                        <p class="text-muted">Chọn món yêu thích và đặt đơn đầu tiên nhé!</p>
                        <a href="${ctx}/menu" class="btn btn-aura px-4">Khám phá thực đơn</a>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <%-- ===== Phân trang ===== --%>
        <c:if test="${totalOrders > 0}">
            <div class="d-flex justify-content-between align-items-center mt-3 flex-wrap gap-2">
                <span class="text-muted small">Hiển thị ${shownOrders} / ${totalOrders} đơn hàng</span>
                <c:if test="${shownOrders < totalOrders}">
                    <c:url var="moreUrl" value="/orders">
                        <c:param name="tab" value="${tab}" />
                        <c:param name="limit" value="${limit + pageSize}" />
                        <c:if test="${not empty keyword}"><c:param name="q" value="${keyword}" /></c:if>
                        <c:if test="${not empty date}"><c:param name="date" value="${date}" /></c:if>
                    </c:url>
                    <a href="${moreUrl}" class="btn btn-soft btn-sm px-3">
                        Xem các đơn cũ hơn <span class="material-symbols-outlined fs-6">expand_more</span>
                    </a>
                </c:if>
            </div>
        </c:if>
    </main>

    <!-- Bootstrap 5 JS Bundle (dropdown, menu mobile) -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
