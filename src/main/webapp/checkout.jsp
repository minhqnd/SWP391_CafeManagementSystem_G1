<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="isCustomer" value="${not empty sessionScope.user and fn:toUpperCase(sessionScope.user.role) eq 'CUSTOMER'}" />
<c:set var="isGuest" value="${not isCustomer}" />
<c:set var="isInternalAccount" value="${not empty sessionScope.user and not isCustomer}" />
<c:set var="shop" value="${cart.settings}" />
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
                <span class="step-label">2. Xác nhận đơn &amp; Giao hàng</span>
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
                    <div class="fw-bold">
                        <c:choose>
                            <c:when test="${isInternalAccount}">Cần tài khoản khách hàng để đặt món</c:when>
                            <c:otherwise>Đăng nhập để tiếp tục đặt hàng</c:otherwise>
                        </c:choose>
                    </div>
                    <div class="text-muted small">
                        <c:choose>
                            <c:when test="${isInternalAccount}">
                                Tài khoản <c:out value="${sessionScope.user.role}" /> hiện tại chỉ dùng cho nghiệp vụ nội bộ. Hãy đăng nhập bằng tài khoản CUSTOMER.
                            </c:when>
                            <c:otherwise>
                                Bạn có thể nhập trước thông tin giao hàng. Hãy đăng nhập bằng tài khoản khách hàng để xác nhận đơn, dùng điểm Aura và chuyển sang bước thanh toán.
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
                <button type="button" class="btn btn-aura px-4 py-2 text-nowrap" data-bs-toggle="modal" data-bs-target="#authModal">
                    <span class="material-symbols-outlined fs-5">login</span> Đăng nhập tài khoản khách hàng
                </button>
            </div>
        </c:if>

        <c:if test="${not empty checkoutError}">
            <div class="alert alert-danger d-flex align-items-center gap-2 py-2 small" role="alert">
                <span class="material-symbols-outlined fs-5">error</span> <c:out value="${checkoutError}" />
            </div>
        </c:if>

        <form method="post" action="${ctx}/checkout">

            <div class="row g-4">

                <%-- ===== Cột trái: Cho phép nhập trước; nút xác nhận và server vẫn yêu cầu tài khoản CUSTOMER ===== --%>
                <div class="col-lg-7">
                    <fieldset>

                        <%-- Đặt hàng online chỉ có Giao tận nơi nên không hiện ô chọn hình thức nhận hàng --%>
                        <input type="hidden" name="fulfillment" value="DELIVERY">

                        <!-- Thông tin người nhận & địa chỉ (SRS 2.3.3) -->
                        <div class="aura-card p-3 p-md-4 mb-4">
                            <div class="d-flex justify-content-between align-items-center">
                                <div class="section-title">
                                    <span class="section-icon"><span class="material-symbols-outlined">person</span></span>
                                    Thông tin người nhận
                                </div>
                                <span class="badge-step mb-3">Bước 2 / 3</span>
                            </div>
                            <div class="info-box mb-3">
                                <div class="row g-3">
                                    <div class="col-md-6">
                                        <label for="recipientName" class="form-label small fw-semibold mb-1">Họ tên người nhận *</label>
                                        <input type="text" id="recipientName" name="recipientName" class="form-control aura-input bg-white"
                                               maxlength="100" required placeholder="Nguyễn Văn An"
                                               value="${fn:escapeXml(recipientName)}">
                                    </div>
                                    <div class="col-md-6">
                                        <label for="recipientPhone" class="form-label small fw-semibold mb-1">Số điện thoại *</label>
                                        <input type="tel" id="recipientPhone" name="recipientPhone" class="form-control aura-input bg-white"
                                               maxlength="15" required placeholder="0908 123 456"
                                               value="${fn:escapeXml(recipientPhone)}">
                                    </div>

                                    <%-- Địa chỉ giao (phí giao cố định, không phụ thuộc quận) --%>
                                    <div class="col-md-4">
                                        <label for="city" class="form-label small fw-semibold mb-1">Tỉnh / Thành phố</label>
                                        <input type="text" id="city" class="form-control aura-input bg-white" value="${city}" readonly>
                                    </div>
                                    <div class="col-md-4">
                                        <label for="district" class="form-label small fw-semibold mb-1">Quận / Huyện *</label>
                                        <input type="text" id="district" name="district" class="form-control aura-input bg-white"
                                               minlength="2" maxlength="100" placeholder="VD: Hoàn Kiếm" required
                                               value="${fn:escapeXml(district)}">
                                    </div>
                                    <div class="col-md-4">
                                        <label for="ward" class="form-label small fw-semibold mb-1">Phường / Xã *</label>
                                        <input type="text" id="ward" name="ward" class="form-control aura-input bg-white"
                                               minlength="2" maxlength="100" placeholder="VD: Tràng Tiền" required
                                               value="${fn:escapeXml(ward)}">
                                    </div>
                                    <div class="col-12">
                                        <label for="addressDetail" class="form-label small fw-semibold mb-1">Địa chỉ chi tiết *</label>
                                        <input type="text" id="addressDetail" name="addressDetail" class="form-control aura-input bg-white"
                                               minlength="5" maxlength="150" required
                                               placeholder="Số nhà, tên đường, toà nhà, tầng / phòng"
                                               value="${fn:escapeXml(addressDetail)}">
                                    </div>
                                </div>
                            </div>

                        </div>

                        <!-- Ghi chú -->
                        <div class="aura-card p-3 p-md-4">
                            <div class="section-title">
                                <span class="section-icon"><span class="material-symbols-outlined">edit_note</span></span>
                                Ghi chú cho quán &amp; shipper
                            </div>
                            <textarea name="orderNote" class="form-control aura-input" rows="3" maxlength="500"
                                      placeholder="VD: Ít đá, gửi bảo vệ tầng hầm B1, gọi điện thoại trước khi giao."><c:out value="${orderNote}" /></textarea>
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
                        <%-- Phí giao cố định 1 mức (Admin chỉnh ở Cài đặt), miễn phí khi đạt mức freeship --%>
                        <div class="summary-row">
                            <span class="label">Phí giao hàng:</span>
                            <span class="fw-semibold">
                                <c:choose>
                                    <c:when test="${cart.deliveryFee == 0}"><span class="text-success">Miễn phí</span></c:when>
                                    <c:otherwise><fmt:formatNumber value="${cart.deliveryFee}" pattern="#,##0" />đ</c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                        <c:if test="${cart.pointsUsed > 0}">
                            <div class="summary-row">
                                <span class="label d-flex align-items-center gap-1">
                                    <span class="material-symbols-outlined" style="font-size: 17px; color: var(--aura-gold);">stars</span>
                                    Dùng ${cart.pointsUsed} điểm Aura:
                                </span>
                                <span class="fw-semibold text-aura">-<fmt:formatNumber value="${cart.discount}" pattern="#,##0" />đ</span>
                            </div>
                        </c:if>

                        <hr class="my-3">

                        <div class="d-flex justify-content-between align-items-end gap-2 mb-3">
                            <div class="fw-bold fs-5">Tổng thanh toán</div>
                            <div class="grand-total"><fmt:formatNumber value="${cart.grandTotal}" pattern="#,##0" />đ</div>
                        </div>
                        <div class="info-box small mb-3 d-flex align-items-center gap-2">
                            <span class="material-symbols-outlined fs-5 text-aura">qr_code_2</span>
                            <span>
                                Thanh toán bằng <b>chuyển khoản VietQR</b> ở bước tiếp theo. Đơn được cộng <b>+${cart.rewardPoints} điểm Aura</b> sau khi thanh toán.
                                <c:if test="${cart.pointsUsed > 0}">
                                    ${cart.pointsUsed} điểm dùng cho đơn được trừ ngay khi xác nhận và hoàn lại nếu bạn hủy đơn chưa thanh toán.
                                </c:if>
                            </span>
                        </div>

                        <c:choose>
                            <c:when test="${isGuest}">
                                <button type="button" class="btn btn-aura w-100 py-3" data-bs-toggle="modal" data-bs-target="#authModal">
                                    <span class="material-symbols-outlined fs-5">login</span> Đăng nhập tài khoản khách hàng
                                </button>
                            </c:when>
                            <c:otherwise>
                                <button type="submit" class="btn btn-aura w-100 py-3" ${checkoutStockBlocked ? 'disabled' : ''}>
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
