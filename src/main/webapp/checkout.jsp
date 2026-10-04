<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="isGuest" value="${empty sessionScope.user}" />
<c:set var="isDelivery" value="${method == 'DELIVERY'}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Aura Café | Xác nhận đơn hàng</title>

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

        <!-- Các bước đặt hàng -->
        <div class="checkout-stepper mb-4">
            <div class="step done">
                <span class="step-circle"><span class="material-symbols-outlined">check</span></span>
                <span class="step-label">1. Giỏ hàng</span>
            </div>
            <div class="step-line done"></div>
            <div class="step active">
                <span class="step-circle"><span class="material-symbols-outlined">local_shipping</span></span>
                <span class="step-label">2. Xác nhận đơn &amp; Giao nhận</span>
            </div>
            <div class="step-line"></div>
            <div class="step">
                <span class="step-circle"><span class="material-symbols-outlined">payments</span></span>
                <span class="step-label">3. Thanh toán</span>
            </div>
        </div>

        <!-- Yêu cầu đăng nhập -->
        <c:if test="${isGuest}">
            <div class="aura-card login-required p-3 mb-4 d-flex flex-column flex-md-row align-items-md-center gap-3">
                <span class="section-icon"><span class="material-symbols-outlined">lock</span></span>
                <div class="flex-grow-1">
                    <div class="fw-bold">Đăng nhập để tiếp tục đặt hàng</div>
                    <div class="text-muted small">
                        Bạn cần đăng nhập để chọn hình thức nhận hàng, địa chỉ, thời gian nhận và chuyển sang bước thanh toán.
                    </div>
                </div>
                <button type="button" class="btn btn-aura px-4 py-2 text-nowrap" data-bs-toggle="modal" data-bs-target="#authModal">
                    <span class="material-symbols-outlined fs-5">login</span> Đăng nhập / Đăng ký
                </button>
            </div>
        </c:if>

        <c:if test="${not empty checkoutError}">
            <div class="alert alert-danger d-flex align-items-center gap-2 py-2 small" role="alert">
                <span class="material-symbols-outlined fs-5">error</span> <c:out value="${checkoutError}" />
            </div>
        </c:if>

        <form method="post" action="${ctx}/checkout">
            <input type="hidden" name="method" value="${method}">
            <c:if test="${!isGuest}">
                <!-- Nút mặc định khi nhấn Enter trong ô nhập: Xác nhận đơn (không phải đổi hình thức nhận hàng) -->
                <button type="submit" class="visually-hidden" tabindex="-1" aria-hidden="true">Xác nhận đơn</button>
            </c:if>

            <div class="row g-4">

                <%-- ===== Cột trái: Thông tin giao nhận (khoá khi chưa đăng nhập) ===== --%>
                <div class="col-lg-7">
                    <fieldset class="${isGuest ? 'checkout-locked' : ''}" ${isGuest ? 'disabled' : ''}>

                        <!-- Phương thức nhận hàng -->
                        <div class="aura-card p-3 p-md-4 mb-4">
                            <div class="d-flex justify-content-between align-items-center">
                                <div class="section-title">
                                    <span class="section-icon"><span class="material-symbols-outlined">swap_horiz</span></span>
                                    Phương thức nhận hàng
                                </div>
                                <span class="badge-step mb-3">Bước 2 / 3</span>
                            </div>
                            <div class="method-switch">
                                <button type="submit" name="switchTo" value="DELIVERY" formnovalidate
                                        class="method-option ${isDelivery ? 'active' : ''}">
                                    <span class="material-symbols-outlined fs-5">delivery_dining</span> Giao tận nơi (Delivery)
                                </button>
                                <button type="submit" name="switchTo" value="TAKEAWAY" formnovalidate
                                        class="method-option ${isDelivery ? '' : 'active'}">
                                    <span class="material-symbols-outlined fs-5">storefront</span> Đến lấy tại quầy (Take-away)
                                </button>
                            </div>
                        </div>

                        <!-- Địa chỉ giao hàng / Thông tin người nhận -->
                        <div class="aura-card p-3 p-md-4 mb-4">
                            <div class="section-title">
                                <span class="section-icon">
                                    <span class="material-symbols-outlined">${isDelivery ? 'location_on' : 'person'}</span>
                                </span>
                                ${isDelivery ? 'Địa chỉ giao hàng' : 'Thông tin người nhận'}
                            </div>

                            <div class="info-box mb-3">
                                <div class="row g-3">
                                    <div class="col-md-6">
                                        <label for="recipientName" class="form-label small fw-semibold mb-1">Họ tên người nhận</label>
                                        <input type="text" id="recipientName" name="recipientName" class="form-control aura-input bg-white"
                                               maxlength="100" required placeholder="Nguyễn Văn An"
                                               value="${fn:escapeXml(recipientName)}">
                                    </div>
                                    <div class="col-md-6">
                                        <label for="recipientPhone" class="form-label small fw-semibold mb-1">Số điện thoại</label>
                                        <input type="tel" id="recipientPhone" name="recipientPhone" class="form-control aura-input bg-white"
                                               maxlength="15" required placeholder="0908 123 456"
                                               value="${fn:escapeXml(recipientPhone)}">
                                    </div>
                                    <c:if test="${isDelivery}">
                                        <div class="col-12">
                                            <label for="deliveryAddress" class="form-label small fw-semibold mb-1">Địa chỉ nhận hàng</label>
                                            <input type="text" id="deliveryAddress" name="deliveryAddress" class="form-control aura-input bg-white"
                                                   minlength="10" maxlength="255" required
                                                   placeholder="Số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành phố"
                                                   value="${fn:escapeXml(deliveryAddress)}">
                                        </div>
                                    </c:if>
                                </div>
                            </div>

                            <div class="store-box">
                                <span class="store-icon"><span class="material-symbols-outlined">store</span></span>
                                <div>
                                    <div class="item-category">${isDelivery ? 'Cửa hàng xử lý đơn' : 'Địa điểm nhận hàng'}</div>
                                    <div class="fw-bold">Aura Café &ndash; <c:out value="${storeAddress}" /></div>
                                    <div class="text-muted" style="font-size: 0.78rem;">
                                        ${isDelivery ? 'Dự kiến hoàn thiện & giao tới bạn trong 25 - 30 phút' : 'Đồ uống sẵn sàng sau 10 - 15 phút kể từ khi thanh toán'}
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Thời gian nhận hàng -->
                        <div class="aura-card p-3 p-md-4 mb-4">
                            <div class="section-title">
                                <span class="section-icon"><span class="material-symbols-outlined">schedule</span></span>
                                Thời gian nhận hàng
                            </div>
                            <div class="row g-3">
                                <!-- Radio đứng ngay trước khung .time-option để CSS tô sáng khung đang chọn (input:checked + .time-option) -->
                                <div class="col-md-6">
                                    <div class="time-choice">
                                        <input class="form-check-input" type="radio" name="deliveryTime" value="NOW" id="deliveryTimeNow"
                                               ${deliveryTime != 'SCHEDULED' ? 'checked' : ''}>
                                        <label class="time-option" for="deliveryTimeNow">
                                            <span class="d-block fw-semibold small">${isDelivery ? 'Giao ngay lập tức' : 'Lấy ngay khi pha xong'}</span>
                                            <span class="d-block text-muted" style="font-size: 0.75rem;">
                                                ${isDelivery ? 'Ưu tiên Barista pha chế & giao trong 25 - 30 phút' : 'Barista pha chế ngay sau khi bạn thanh toán'}
                                            </span>
                                        </label>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="time-choice">
                                        <input class="form-check-input" type="radio" name="deliveryTime" value="SCHEDULED" id="deliveryTimeScheduled"
                                               ${deliveryTime == 'SCHEDULED' ? 'checked' : ''}>
                                        <div class="time-option">
                                            <label class="d-block" for="deliveryTimeScheduled">
                                                <span class="d-block fw-semibold small">${isDelivery ? 'Hẹn giờ giao cụ thể' : 'Hẹn giờ lấy cụ thể'}</span>
                                                <span class="d-block text-muted" style="font-size: 0.75rem;">Chọn khung giờ tiện nhất trong ngày hôm nay</span>
                                            </label>
                                            <input type="time" name="scheduledTime" class="form-control form-control-sm aura-input bg-white mt-2"
                                                   aria-label="Giờ hẹn nhận hàng" value="${fn:escapeXml(scheduledTime)}">
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Ghi chú -->
                        <div class="aura-card p-3 p-md-4">
                            <div class="section-title">
                                <span class="section-icon"><span class="material-symbols-outlined">edit_note</span></span>
                                ${isDelivery ? 'Ghi chú giao hàng' : 'Ghi chú cho quán'}
                            </div>
                            <textarea name="orderNote" class="form-control aura-input" rows="3" maxlength="500"
                                      placeholder="${isDelivery ? 'VD: Gửi bảo vệ tầng hầm B1, gọi điện thoại trước khi đến nhận hàng.' : 'VD: Ít đá, đóng gói mang đi cẩn thận.'}"><c:out value="${orderNote}" /></textarea>
                        </div>
                    </fieldset>
                </div>

                <%-- ===== Cột phải: Kiểm tra đơn hàng ===== --%>
                <div class="col-lg-5">
                    <div class="aura-card p-3 p-md-4 sticky-lg-top" style="top: 96px; z-index: 1;">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <div class="section-title mb-0">
                                <span class="material-symbols-outlined text-aura">receipt_long</span>
                                Kiểm tra đơn hàng (${fn:length(cart.selectedItems)} món)
                            </div>
                            <a href="${ctx}/cart" class="btn-link-aura">Sửa giỏ</a>
                        </div>

                        <c:forEach var="item" items="${cart.selectedItems}">
                            <div class="d-flex align-items-center gap-3 py-2">
                                <c:choose>
                                    <c:when test="${not empty item.imageUrl}">
                                        <img src="${fn:escapeXml(item.imageUrl)}" alt="${fn:escapeXml(item.productName)}" class="order-item-img">
                                    </c:when>
                                    <c:otherwise>
                                        <div class="order-item-img cart-item-img-placeholder">
                                            <span class="material-symbols-outlined">local_cafe</span>
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                                <div class="flex-grow-1" style="min-width: 0;">
                                    <div class="fw-bold small"><c:out value="${item.productName}" /></div>
                                    <div class="text-muted" style="font-size: 0.75rem;">
                                        <span class="text-aura fw-bold">${item.quantity}x</span>
                                        <c:out value="${item.sizeLabel}" /><c:forEach var="topping" items="${item.toppings}"> &bull; <c:out value="${topping}" /></c:forEach>
                                    </div>
                                </div>
                                <div class="fw-bold small text-nowrap"><fmt:formatNumber value="${item.subtotal}" pattern="#,##0" />đ</div>
                            </div>
                        </c:forEach>

                        <hr class="my-3">

                        <div class="summary-row">
                            <span class="label">Tổng tiền món (${fn:length(cart.selectedItems)} món):</span>
                            <span class="fw-semibold"><fmt:formatNumber value="${cart.selectedSubtotal}" pattern="#,##0" />đ</span>
                        </div>
                        <div class="summary-row">
                            <span class="label">Phí giao hàng:</span>
                            <c:choose>
                                <c:when test="${!isDelivery}"><span class="fw-semibold text-muted">Nhận tại quầy</span></c:when>
                                <c:when test="${deliveryFee == 0}"><span class="fw-semibold text-success">Miễn phí</span></c:when>
                                <c:otherwise><span class="fw-semibold"><fmt:formatNumber value="${deliveryFee}" pattern="#,##0" />đ</span></c:otherwise>
                            </c:choose>
                        </div>
                        <c:if test="${cart.voucherEligible}">
                            <div class="summary-row">
                                <span class="label d-flex align-items-center gap-1">
                                    <span class="material-symbols-outlined" style="font-size: 17px;">sell</span>
                                    Voucher giảm giá (<c:out value="${cart.voucher.code}" />)
                                </span>
                                <span class="fw-semibold text-aura">-<fmt:formatNumber value="${cart.discount}" pattern="#,##0" />đ</span>
                            </div>
                        </c:if>

                        <hr class="my-3">

                        <div class="d-flex justify-content-between align-items-end gap-2 mb-3">
                            <div class="fw-bold fs-5">Tổng thanh toán</div>
                            <div class="grand-total"><fmt:formatNumber value="${grandTotal}" pattern="#,##0" />đ</div>
                        </div>

                        <c:choose>
                            <c:when test="${isGuest}">
                                <button type="button" class="btn btn-aura w-100 py-3" data-bs-toggle="modal" data-bs-target="#authModal">
                                    <span class="material-symbols-outlined fs-5">login</span> Đăng nhập để tiếp tục
                                </button>
                            </c:when>
                            <c:otherwise>
                                <button type="submit" class="btn btn-aura w-100 py-3">
                                    Xác nhận đơn &amp; Đến bước Thanh toán
                                    <span class="material-symbols-outlined fs-5">arrow_forward</span>
                                </button>
                            </c:otherwise>
                        </c:choose>
                        <a href="${ctx}/cart" class="btn btn-soft w-100 py-2 mt-2">
                            <span class="material-symbols-outlined fs-6">arrow_back</span> Quay lại giỏ hàng
                        </a>
                    </div>
                </div>
            </div>
        </form>
    </main>

    <!-- Popup đăng nhập / đăng ký (đăng nhập xong quay lại trang này) -->
    <c:if test="${isGuest}">
        <jsp:include page="/components/auth_modal.jsp" />
    </c:if>

    <!-- Bootstrap 5 JS Bundle (modal, dropdown, menu mobile) -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
