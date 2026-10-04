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
    <title>Aura Café | Giỏ hàng của bạn</title>

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
        <jsp:param name="activePage" value="cart" />
    </jsp:include>

    <main class="container-xl px-3 px-lg-4 py-4">

        <!-- Breadcrumb & Tiêu đề -->
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb small mb-2">
                <li class="breadcrumb-item">
                    <a href="${ctx}/" class="d-inline-flex align-items-center gap-1">
                        <span class="material-symbols-outlined" style="font-size: 16px;">home</span> Trang chủ
                    </a>
                </li>
                <li class="breadcrumb-item active fw-semibold text-dark" aria-current="page">Giỏ hàng của bạn</li>
            </ol>
        </nav>
        <div class="d-flex align-items-center gap-2 mb-4">
            <h1 class="h3 fw-bold mb-0">Giỏ hàng của bạn</h1>
            <span class="badge-soft">${fn:length(cart.items)} món</span>
        </div>

        <!-- Thông báo -->
        <c:if test="${not empty cartMessage}">
            <div class="alert alert-success d-flex align-items-center gap-2 py-2 small" role="alert">
                <span class="material-symbols-outlined fs-5">check_circle</span> <c:out value="${cartMessage}" />
            </div>
        </c:if>
        <c:if test="${not empty cartError}">
            <div class="alert alert-danger d-flex align-items-center gap-2 py-2 small" role="alert">
                <span class="material-symbols-outlined fs-5">error</span> <c:out value="${cartError}" />
            </div>
        </c:if>

        <c:choose>
            <%-- ===== Giỏ hàng trống ===== --%>
            <c:when test="${empty cart.items}">
                <div class="aura-card text-center py-5 px-3">
                    <div class="empty-cart-icon mb-3">
                        <span class="material-symbols-outlined" style="font-size: 40px;">shopping_bag</span>
                    </div>
                    <h2 class="h5 fw-bold mb-2">Giỏ hàng của bạn đang trống</h2>
                    <p class="text-muted mb-4">Hãy chọn vài món yêu thích từ thực đơn Aura Café nhé.</p>
                    <a href="${ctx}/menu" class="btn btn-aura px-4 py-2">
                        <span class="material-symbols-outlined fs-5">restaurant_menu</span> Xem thực đơn
                    </a>
                </div>
            </c:when>

            <c:otherwise>
                <div class="row g-4">

                    <%-- ===== Cột trái: Danh sách món ===== --%>
                    <div class="col-lg-8">

                        <!-- Chọn tất cả / Xoá món đã chọn -->
                        <div class="aura-card px-3 py-3 mb-3 d-flex align-items-center justify-content-between gap-2">
                            <form method="post" action="${ctx}/cart" class="m-0">
                                <input type="hidden" name="action" value="selectAll">
                                <label class="d-flex align-items-center gap-2 m-0 fw-semibold" style="cursor: pointer;">
                                    <input class="form-check-input m-0" type="checkbox" onchange="this.form.submit()"
                                           ${cart.allSelected ? 'checked' : ''}>
                                    Chọn tất cả (${fn:length(cart.items)} món)
                                </label>
                            </form>
                            <form method="post" action="${ctx}/cart" class="m-0">
                                <input type="hidden" name="action" value="removeSelected">
                                <button type="submit" class="btn btn-sm btn-link text-muted text-decoration-none d-flex align-items-center gap-1 small"
                                        onclick="return confirm('Xoá các món đã chọn khỏi giỏ hàng?')"
                                        ${empty cart.selectedItems ? 'disabled' : ''}>
                                    <span class="material-symbols-outlined" style="font-size: 18px;">delete_sweep</span>
                                    Xóa các món đã chọn
                                </button>
                            </form>
                        </div>

                        <!-- Danh sách món -->
                        <c:forEach var="item" items="${cart.items}">
                            <div class="aura-card cart-item p-3 mb-3 ${item.available ? '' : 'is-unavailable'}">
                                <div class="d-flex gap-3">
                                    <form method="post" action="${ctx}/cart" class="m-0 pt-1">
                                        <input type="hidden" name="action" value="toggle">
                                        <input type="hidden" name="lineId" value="${item.lineId}">
                                        <input class="form-check-input m-0" type="checkbox" aria-label="Chọn món"
                                               onchange="this.form.submit()"
                                               ${item.selected && item.available ? 'checked' : ''}
                                               ${item.available ? '' : 'disabled'}>
                                    </form>

                                    <c:choose>
                                        <c:when test="${not empty item.imageUrl}">
                                            <img src="${fn:escapeXml(item.imageUrl)}" alt="${fn:escapeXml(item.productName)}" class="cart-item-img">
                                        </c:when>
                                        <c:otherwise>
                                            <div class="cart-item-img cart-item-img-placeholder">
                                                <span class="material-symbols-outlined">local_cafe</span>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>

                                    <div class="flex-grow-1" style="min-width: 0;">
                                        <div class="d-flex justify-content-between align-items-start gap-2">
                                            <div style="min-width: 0;">
                                                <div class="item-category"><c:out value="${item.categoryName}" /></div>
                                                <div class="item-name"><c:out value="${item.productName}" /></div>
                                            </div>
                                            <form method="post" action="${ctx}/cart" class="m-0">
                                                <input type="hidden" name="action" value="remove">
                                                <input type="hidden" name="lineId" value="${item.lineId}">
                                                <button type="submit" class="btn-icon" title="Xoá món">
                                                    <span class="material-symbols-outlined fs-5">close</span>
                                                </button>
                                            </form>
                                        </div>

                                        <div class="d-flex flex-wrap gap-1 mt-2">
                                            <c:if test="${not empty item.sizeLabel}">
                                                <span class="option-chip"><c:out value="${item.sizeLabel}" /></span>
                                            </c:if>
                                            <c:forEach var="topping" items="${item.toppings}">
                                                <span class="option-chip option-chip-topping">+ Thêm <c:out value="${topping}" /></span>
                                            </c:forEach>
                                        </div>

                                        <c:choose>
                                            <c:when test="${item.available}">
                                                <div class="d-flex align-items-end justify-content-between flex-wrap gap-2 mt-3">
                                                    <div>
                                                        <div class="text-muted" style="font-size: 0.75rem;">Đơn giá</div>
                                                        <div class="fw-semibold small"><fmt:formatNumber value="${item.unitPrice}" pattern="#,##0" />đ</div>
                                                    </div>
                                                    <div class="d-flex align-items-center gap-3">
                                                        <form method="post" action="${ctx}/cart" class="qty-stepper">
                                                            <input type="hidden" name="lineId" value="${item.lineId}">
                                                            <button type="submit" name="action" value="decrease" title="Giảm"
                                                                    ${item.quantity <= 1 ? 'disabled' : ''}>&minus;</button>
                                                            <span class="qty-value">${item.quantity}</span>
                                                            <button type="submit" name="action" value="increase" title="Tăng"
                                                                    ${item.quantity >= 99 ? 'disabled' : ''}>+</button>
                                                        </form>
                                                        <div class="line-total"><fmt:formatNumber value="${item.subtotal}" pattern="#,##0" />đ</div>
                                                    </div>
                                                </div>
                                            </c:when>
                                            <c:otherwise>
                                                <div class="text-danger small fw-semibold mt-3">
                                                    Món này hiện đã ngừng phục vụ, vui lòng xoá khỏi giỏ hàng.
                                                </div>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>

                        <a href="${ctx}/menu" class="d-inline-flex align-items-center gap-2 text-decoration-none fw-semibold text-aura mt-1">
                            <span class="material-symbols-outlined fs-5">arrow_back</span> Tiếp tục chọn món thêm
                        </a>
                    </div>

                    <%-- ===== Cột phải: Freeship, Voucher, Tóm tắt ===== --%>
                    <div class="col-lg-4">

                        <!-- Tiến độ Freeship -->
                        <div class="aura-card p-3 mb-3">
                            <div class="d-flex justify-content-between align-items-center gap-2 mb-2 small fw-semibold">
                                <span class="d-flex align-items-center gap-2 text-aura">
                                    <span class="material-symbols-outlined fs-5">local_shipping</span>
                                    <c:choose>
                                        <c:when test="${cart.freeshipReached}">Đơn hàng đủ điều kiện FREESHIP!</c:when>
                                        <c:otherwise>Mua thêm <fmt:formatNumber value="${cart.freeshipRemaining}" pattern="#,##0" />đ để được FREESHIP</c:otherwise>
                                    </c:choose>
                                </span>
                                <span>${cart.freeshipPercent}%</span>
                            </div>
                            <div class="progress aura-progress" role="progressbar" aria-valuenow="${cart.freeshipPercent}" aria-valuemin="0" aria-valuemax="100">
                                <div class="progress-bar" style="width: ${cart.freeshipPercent}%"></div>
                            </div>
                            <div class="text-muted mt-2" style="font-size: 0.78rem;">
                                <c:choose>
                                    <c:when test="${cart.freeshipReached}">Bạn đã đạt mốc <b>150.000đ</b> để nhận miễn phí giao hàng.</c:when>
                                    <c:otherwise>Miễn phí giao hàng cho đơn từ <b>150.000đ</b> (tính trên các món đã chọn).</c:otherwise>
                                </c:choose>
                            </div>
                        </div>

                        <!-- Voucher -->
                        <div class="aura-card p-3 mb-3">
                            <div class="section-title">
                                <span class="material-symbols-outlined text-aura">confirmation_number</span>
                                Mã khuyến mãi / Voucher Aura
                            </div>
                            <form method="post" action="${ctx}/cart" class="d-flex gap-2 mb-3">
                                <input type="hidden" name="action" value="applyVoucher">
                                <input type="text" name="voucherCode" class="form-control aura-input text-uppercase fw-semibold"
                                       placeholder="Nhập mã khuyến mãi" maxlength="50"
                                       value="${fn:escapeXml(cart.voucher.code)}">
                                <button type="submit" class="btn btn-aura px-3 text-nowrap">Áp dụng</button>
                            </form>

                            <div class="small-label">Ưu đãi khả dụng</div>
                            <c:forEach var="v" items="${vouchers}">
                                <c:set var="isActive" value="${cart.voucher != null && cart.voucher.id == v.id}" />
                                <c:set var="isEligible" value="${v.isEligible(cart.selectedSubtotal)}" />
                                <div class="voucher-option ${isActive ? 'active' : ''} ${isEligible || isActive ? '' : 'muted'}">
                                    <span class="material-symbols-outlined voucher-icon">${v.personal ? 'redeem' : (v.percent ? 'percent' : 'sell')}</span>
                                    <div class="flex-grow-1" style="min-width: 0;">
                                        <div class="d-flex align-items-center gap-2 flex-wrap">
                                            <span class="fw-bold"><c:out value="${v.code}" /></span>
                                            <c:if test="${v.personal}"><span class="badge-personal">Đổi từ điểm</span></c:if>
                                            <c:if test="${isActive}"><span class="badge-using">Đang dùng</span></c:if>
                                        </div>
                                        <div class="text-muted" style="font-size: 0.75rem;">
                                            <c:choose>
                                                <c:when test="${v.percent}">
                                                    Giảm <fmt:formatNumber value="${v.discountValue}" pattern="#,##0" />%<c:if test="${v.maxDiscount != null}"> (tối đa <fmt:formatNumber value="${v.maxDiscount}" pattern="#,##0" />đ)</c:if>
                                                </c:when>
                                                <c:otherwise>
                                                    Giảm <fmt:formatNumber value="${v.discountValue}" pattern="#,##0" />đ
                                                </c:otherwise>
                                            </c:choose>
                                            cho đơn từ <fmt:formatNumber value="${v.minOrderValue}" pattern="#,##0" />đ<c:if test="${v.personal}"> • HSD ${v.validUntilText}</c:if>
                                        </div>
                                        <%-- Số tiền mã này giảm được cho các món đang chọn --%>
                                        <div class="voucher-saving">
                                            <c:choose>
                                                <c:when test="${isEligible}">
                                                    Giảm <b><fmt:formatNumber value="${v.calculateDiscount(cart.selectedSubtotal)}" pattern="#,##0" />đ</b> cho giỏ hàng này
                                                </c:when>
                                                <c:otherwise>
                                                    Chọn thêm <fmt:formatNumber value="${v.minOrderValue - cart.selectedSubtotal}" pattern="#,##0" />đ để dùng mã
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                    <form method="post" action="${ctx}/cart" class="m-0">
                                        <c:choose>
                                            <c:when test="${isActive}">
                                                <input type="hidden" name="action" value="removeVoucher">
                                                <button type="submit" class="btn-link-aura">Gỡ</button>
                                            </c:when>
                                            <c:otherwise>
                                                <input type="hidden" name="action" value="applyVoucher">
                                                <input type="hidden" name="voucherCode" value="${fn:escapeXml(v.code)}">
                                                <button type="submit" class="btn-link-aura" ${isEligible ? '' : 'disabled'}>Chọn</button>
                                            </c:otherwise>
                                        </c:choose>
                                    </form>
                                </div>
                            </c:forEach>
                            <c:if test="${empty vouchers}">
                                <div class="text-muted small">Hiện chưa có ưu đãi nào khả dụng.</div>
                            </c:if>
                            <div class="text-muted mt-2" style="font-size: 0.75rem;">
                                Điểm Aura không trừ trực tiếp vào giá, hãy
                                <a href="${ctx}/profile" class="text-aura fw-semibold">đổi điểm lấy voucher</a> rồi chọn mã tại đây.
                            </div>
                        </div>

                        <!-- Tóm tắt đơn hàng -->
                        <div class="aura-card p-3 p-md-4">
                            <h2 class="h6 fw-bold mb-3">Tóm tắt đơn hàng</h2>
                            <div class="summary-row">
                                <span class="label">Tổng tiền hàng (${fn:length(cart.selectedItems)} món)</span>
                                <span class="fw-semibold"><fmt:formatNumber value="${cart.selectedSubtotal}" pattern="#,##0" />đ</span>
                            </div>
                            <c:if test="${cart.voucher != null}">
                                <div class="summary-row">
                                    <span class="label">Giảm giá voucher (<c:out value="${cart.voucher.code}" />)</span>
                                    <span class="fw-semibold text-aura">-<fmt:formatNumber value="${cart.discount}" pattern="#,##0" />đ</span>
                                </div>
                                <c:if test="${!cart.voucherEligible}">
                                    <div class="alert alert-warning py-1 px-2 small mb-2">
                                        Chọn thêm <fmt:formatNumber value="${cart.voucher.minOrderValue - cart.selectedSubtotal}" pattern="#,##0" />đ
                                        để dùng mã <c:out value="${cart.voucher.code}" />.
                                    </div>
                                </c:if>
                            </c:if>
                            <div class="summary-row">
                                <span class="label d-flex align-items-center gap-1">
                                    <span class="material-symbols-outlined" style="font-size: 18px; color: var(--aura-gold);">stars</span>
                                    Tích luỹ Aura Rewards
                                </span>
                                <span class="fw-semibold" style="color: var(--aura-gold);">+${cart.rewardPoints} điểm</span>
                            </div>

                            <hr class="my-3">

                            <div class="d-flex justify-content-between align-items-end gap-2 mb-3">
                                <div>
                                    <div class="fw-bold">Tổng thanh toán</div>
                                    <div class="text-muted" style="font-size: 0.72rem;">Chưa gồm phí giao hàng (nếu có)</div>
                                </div>
                                <div class="grand-total"><fmt:formatNumber value="${cart.total}" pattern="#,##0" />đ</div>
                            </div>
                            <c:if test="${cart.discount > 0}">
                                <div class="saving-note mb-3">
                                    <span class="material-symbols-outlined fs-6">savings</span>
                                    Voucher <c:out value="${cart.voucher.code}" /> giúp bạn tiết kiệm
                                    <b><fmt:formatNumber value="${cart.discount}" pattern="#,##0" />đ</b>
                                </div>
                            </c:if>

                            <c:choose>
                                <c:when test="${empty cart.selectedItems}">
                                    <button type="button" class="btn btn-aura w-100 py-3" disabled>Chọn món để đặt hàng</button>
                                </c:when>
                                <c:otherwise>
                                    <a href="${ctx}/checkout" class="btn btn-aura w-100 py-3">
                                        Tiến hành đặt hàng (${fn:length(cart.selectedItems)} món)
                                        <span class="material-symbols-outlined fs-5">arrow_forward</span>
                                    </a>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>
    </main>

    <!-- Popup đăng nhập cho nút tài khoản trên header -->
    <c:if test="${empty sessionScope.user}">
        <jsp:include page="/components/auth_modal.jsp" />
    </c:if>

    <!-- Bootstrap 5 JS Bundle (modal, dropdown, menu mobile) -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
