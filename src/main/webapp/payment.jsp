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
    <title>Aura Café | Thanh toán đơn hàng #<c:out value="${order.orderCode}" /></title>

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

    <div class="no-print">
        <jsp:include page="/components/header.jsp">
            <jsp:param name="activePage" value="cart" />
        </jsp:include>
    </div>

    <main class="container-xl px-3 px-lg-4 py-4">

        <!-- Các bước đặt hàng -->
        <div class="checkout-stepper mb-4 no-print">
            <div class="step done">
                <span class="step-circle"><span class="material-symbols-outlined">check</span></span>
                <span class="step-label">1. Giỏ hàng</span>
            </div>
            <div class="step-line done"></div>
            <div class="step done">
                <span class="step-circle"><span class="material-symbols-outlined">check</span></span>
                <span class="step-label">2. Xác nhận đơn &amp; Giao nhận</span>
            </div>
            <div class="step-line done"></div>
            <div class="step active">
                <span class="step-circle"><span class="material-symbols-outlined">payments</span></span>
                <span class="step-label">3. Thanh toán</span>
            </div>
        </div>

        <c:if test="${not empty paymentError}">
            <div class="alert alert-danger d-flex align-items-center gap-2 no-print">
                <span class="material-symbols-outlined">error</span> <c:out value="${paymentError}" />
            </div>
        </c:if>

        <!-- Tiêu đề đơn -->
        <div class="aura-card p-3 p-md-4 mb-4 d-flex align-items-center gap-3 no-print">
            <span class="section-icon section-icon-lg"><span class="material-symbols-outlined">receipt_long</span></span>
            <div>
                <div class="d-flex align-items-center gap-2 flex-wrap">
                    <h1 class="h4 fw-bold mb-0">Thanh toán đơn hàng #<c:out value="${order.orderCode}" /></h1>
                    <span class="status-badge status-pending">Chờ thanh toán</span>
                </div>
                <div class="text-muted small">Vui lòng hoàn tất thanh toán để bar bắt đầu chuẩn bị món ngon của bạn</div>
            </div>
        </div>

        <div class="row g-4 align-items-start">

            <%-- ===== Cột trái: Chuyển khoản VietQR ===== --%>
            <div class="col-lg-7 no-print">
                <div class="aura-card p-3 p-md-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h2 class="h5 fw-bold mb-0">Phương thức thanh toán</h2>
                        <span class="badge-step">Chuyển khoản ngân hàng</span>
                    </div>

                    <div class="pay-method-box">
                        <div class="d-flex justify-content-between align-items-center gap-2 mb-3">
                            <div class="d-flex align-items-center gap-2">
                                <span class="pay-radio"></span>
                                <div>
                                    <div class="fw-bold">Quét mã VietQR / Chuyển khoản ngân hàng</div>
                                    <div class="text-muted" style="font-size: 0.78rem;">Dùng được với mọi ứng dụng ngân hàng hỗ trợ VietQR</div>
                                </div>
                            </div>
                            <span class="bank-pill"><c:out value="${bankName}" /></span>
                        </div>

                        <div class="row g-3">
                            <div class="col-md-5">
                                <div class="qr-frame">
                                    <img src="${fn:escapeXml(qrUrl)}" alt="Mã VietQR thanh toán đơn ${fn:escapeXml(order.orderCode)}" class="img-fluid">
                                    <div class="text-muted mt-2 d-flex align-items-center justify-content-center gap-1" style="font-size: 0.75rem;">
                                        <span class="material-symbols-outlined fs-6">photo_camera</span> Mở app ngân hàng để quét
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-7 d-flex flex-column gap-2">
                                <div class="bank-field">
                                    <div>
                                        <div class="bank-label">Ngân hàng thụ hưởng</div>
                                        <div class="bank-value"><c:out value="${bankName}" /></div>
                                        <div class="text-muted" style="font-size: 0.75rem;"><c:out value="${accountName}" /></div>
                                    </div>
                                </div>
                                <div class="bank-field">
                                    <div>
                                        <div class="bank-label">Số tài khoản</div>
                                        <div class="bank-value"><c:out value="${accountNo}" /></div>
                                    </div>
                                    <button type="button" class="btn-copy" onclick="copyText('${accountNo}', this)">Sao chép</button>
                                </div>
                                <div class="bank-field">
                                    <div>
                                        <div class="bank-label">Số tiền chính xác</div>
                                        <div class="bank-value"><fmt:formatNumber value="${order.finalAmount}" pattern="#,##0" /> đ</div>
                                    </div>
                                    <button type="button" class="btn-copy" onclick="copyText('<fmt:formatNumber value="${order.finalAmount}" pattern="0" />', this)">Sao chép</button>
                                </div>
                                <div class="bank-field bank-field-highlight">
                                    <div>
                                        <div class="bank-label text-aura">Nội dung chuyển khoản (bắt buộc)</div>
                                        <div class="bank-value text-aura"><c:out value="${order.orderCode}" /></div>
                                    </div>
                                    <button type="button" class="btn-copy btn-copy-primary" onclick="copyText('${fn:escapeXml(order.orderCode)}', this)">Sao chép</button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="small-label mt-4">Hướng dẫn thanh toán</div>
                    <ol class="pay-guide mb-0">
                        <li>Mở ứng dụng ngân hàng, chọn <b>Quét mã QR</b> và quét mã bên trên.</li>
                        <li>Kiểm tra đúng <b>số tiền</b> và <b>nội dung chuyển khoản</b> là mã đơn <b><c:out value="${order.orderCode}" /></b>.</li>
                        <li>Chuyển khoản xong, bấm <b>Xác nhận đã thanh toán</b> để bar bắt đầu pha chế.</li>
                    </ol>
                </div>
            </div>

            <%-- ===== Cột phải: Hóa đơn điện tử ===== --%>
            <div class="col-lg-5">
                <div class="aura-card p-3 p-md-4 ebill">
                    <div class="d-flex justify-content-between align-items-start gap-2 pb-3 mb-3 border-bottom">
                        <div class="d-flex align-items-center gap-2">
                            <span class="ebill-logo"><span class="material-symbols-outlined">local_cafe</span></span>
                            <div>
                                <div class="fw-bold text-aura lh-1">Aura Café</div>
                                <div class="text-muted text-uppercase" style="font-size: 0.68rem; letter-spacing: 0.5px;">Hóa đơn điện tử e-Bill</div>
                            </div>
                        </div>
                        <div class="text-end">
                            <div class="text-muted text-uppercase" style="font-size: 0.68rem;">Mã đơn đặt hàng</div>
                            <div class="fw-bold text-aura">#<c:out value="${order.orderCode}" /></div>
                        </div>
                    </div>

                    <div class="ebill-info mb-3">
                        <div>
                            <div class="bank-label">Thời gian đặt</div>
                            <div class="fw-semibold">${order.createdDateText} ${order.createdTimeText}</div>
                        </div>
                        <div>
                            <div class="bank-label">Hình thức nhận</div>
                            <div class="fw-semibold">${order.delivery ? 'Giao tận nơi' : 'Tự đến lấy tại quầy'}</div>
                        </div>
                        <div>
                            <div class="bank-label">Khách hàng</div>
                            <div class="fw-semibold"><c:out value="${order.recipientName}" /></div>
                        </div>
                        <div>
                            <div class="bank-label">Số điện thoại</div>
                            <div class="fw-semibold"><c:out value="${order.recipientPhone}" /></div>
                        </div>
                        <div class="ebill-info-full">
                            <div class="bank-label">${order.delivery ? 'Địa chỉ giao hàng' : 'Nhận tại quầy'}</div>
                            <div class="fw-semibold"><c:out value="${order.delivery ? order.deliveryAddress : storeAddress}" /></div>
                        </div>
                        <c:if test="${not empty order.orderNote}">
                            <div class="ebill-info-full">
                                <div class="bank-label">Ghi chú</div>
                                <div class="fw-semibold"><c:out value="${order.orderNote}" /></div>
                            </div>
                        </c:if>
                    </div>

                    <div class="d-flex justify-content-between small-label mb-2">
                        <span>Món / Dịch vụ</span><span>Thành tiền</span>
                    </div>
                    <c:forEach var="item" items="${order.items}">
                        <div class="ebill-item">
                            <span class="qty-badge"><fmt:formatNumber value="${item.quantity}" minIntegerDigits="2" /></span>
                            <div class="flex-grow-1" style="min-width: 0;">
                                <div class="fw-semibold"><c:out value="${item.productName}" /></div>
                                <c:if test="${not empty item.optionsText}">
                                    <div class="text-muted" style="font-size: 0.75rem;"><c:out value="${item.optionsText}" /></div>
                                </c:if>
                                <div class="text-muted" style="font-size: 0.75rem;"><fmt:formatNumber value="${item.unitPrice}" pattern="#,##0" />đ / món</div>
                            </div>
                            <div class="fw-semibold text-nowrap"><fmt:formatNumber value="${item.subtotal}" pattern="#,##0" /> đ</div>
                        </div>
                    </c:forEach>

                    <div class="ebill-totals mt-3">
                        <div class="summary-row">
                            <span class="label">Tạm tính (${order.itemCount} món):</span>
                            <span><fmt:formatNumber value="${order.totalAmount}" pattern="#,##0" /> đ</span>
                        </div>
                        <div class="summary-row">
                            <span class="label">Phí giao hàng:</span>
                            <c:choose>
                                <c:when test="${!order.delivery}"><span>Nhận tại quầy</span></c:when>
                                <c:when test="${order.deliveryFee == 0}"><span class="text-success">Miễn phí</span></c:when>
                                <c:otherwise><span><fmt:formatNumber value="${order.deliveryFee}" pattern="#,##0" /> đ</span></c:otherwise>
                            </c:choose>
                        </div>
                        <c:if test="${order.discountAmount > 0}">
                            <div class="summary-row text-aura">
                                <span class="d-flex align-items-center gap-1">
                                    <span class="material-symbols-outlined" style="font-size: 17px;">sell</span>
                                    Mã giảm <c:out value="${order.voucherCode}" />:
                                </span>
                                <span class="fw-semibold">-<fmt:formatNumber value="${order.discountAmount}" pattern="#,##0" /> đ</span>
                            </div>
                        </c:if>
                    </div>

                    <div class="d-flex justify-content-between align-items-end gap-2 mt-3 mb-1">
                        <div>
                            <div class="fw-bold fs-5">Tổng thanh toán</div>
                            <div class="text-muted" style="font-size: 0.72rem;">Đã gồm phí giao hàng và giảm giá voucher</div>
                        </div>
                        <div class="grand-total"><fmt:formatNumber value="${order.finalAmount}" pattern="#,##0" /> đ</div>
                    </div>
                    <c:if test="${order.rewardPoints > 0}">
                        <div class="saving-note mt-2 mb-3">
                            <span class="material-symbols-outlined fs-6" style="color: var(--aura-gold);">stars</span>
                            Nhận <b>+${order.rewardPoints} điểm Aura</b> sau khi thanh toán
                        </div>
                    </c:if>

                    <div class="no-print">
                        <form method="post" action="${ctx}/payment" class="mt-3"
                              onsubmit="return confirm('Bạn đã chuyển khoản đúng số tiền và nội dung ${fn:escapeXml(order.orderCode)}?');">
                            <input type="hidden" name="orderId" value="${order.id}">
                            <button type="submit" class="btn btn-aura w-100 py-3">
                                Xác nhận đã thanh toán
                                <span class="material-symbols-outlined fs-5">arrow_forward</span>
                            </button>
                        </form>
                        <button type="button" class="btn btn-soft w-100 py-2 mt-2" onclick="window.print()">
                            <span class="material-symbols-outlined fs-6">print</span> In hóa đơn
                        </button>
                        <div class="text-center mt-3">
                            <a href="${ctx}/orders" class="small text-muted">Thanh toán sau, xem Đơn hàng của tôi</a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <!-- Bootstrap 5 JS Bundle (dropdown, menu mobile) -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Sao chép số tài khoản / số tiền / nội dung chuyển khoản
        function copyText(text, button) {
            navigator.clipboard.writeText(text).then(function () {
                var oldText = button.innerText;
                button.innerText = 'Đã chép';
                setTimeout(function () { button.innerText = oldText; }, 1500);
            });
        }
    </script>
</body>
</html>
