<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- Mở trực tiếp file JSP: chuyển qua controller để kiểm tra quyền Admin và nạp cấu hình --%>
<c:if test="${empty settings}">
    <c:redirect url="/admin/settings" />
</c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Aura Café | Cài đặt bán hàng online</title>

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

    <!-- Thanh điều hướng Admin -->
    <nav class="admin-topbar">
        <div class="container-xl px-3 px-lg-4 d-flex align-items-center justify-content-between gap-3">
            <a href="${ctx}/admin-dashboard.jsp" class="d-flex align-items-center gap-2 text-decoration-none">
                <span class="ebill-logo"><span class="material-symbols-outlined">local_cafe</span></span>
                <span>
                    <span class="d-block fw-bold text-aura lh-1">Aura Café</span>
                    <span class="d-block text-muted" style="font-size: 0.72rem;">Trang quản trị</span>
                </span>
            </a>
            <div class="d-flex align-items-center gap-3 small">
                <span class="text-muted d-none d-sm-inline">Xin chào, <b><c:out value="${sessionScope.user.fullName}" /></b></span>
                <a href="${ctx}/login?action=logout" class="btn btn-soft btn-sm px-3">
                    <span class="material-symbols-outlined fs-6">logout</span> Đăng xuất
                </a>
            </div>
        </div>
    </nav>

    <main class="container-xl px-3 px-lg-4 py-4" style="max-width: 980px;">

        <nav aria-label="breadcrumb">
            <ol class="breadcrumb small mb-2">
                <li class="breadcrumb-item"><a href="${ctx}/admin-dashboard.jsp">Dashboard</a></li>
                <li class="breadcrumb-item active fw-semibold text-dark" aria-current="page">Cài đặt bán hàng online</li>
            </ol>
        </nav>
        <div class="mb-4">
            <div class="eyebrow">Cài đặt hệ thống</div>
            <h1 class="h3 fw-bold mb-1">Bán hàng online</h1>
            <div class="text-muted small">
                Áp dụng cho Giỏ hàng, Xác nhận đơn và Thanh toán của khách. Thay đổi có hiệu lực ngay ở lần tải trang tiếp theo
                (đơn đã đặt giữ nguyên số tiền lúc đặt).
            </div>
        </div>

        <c:if test="${not empty settingsMessage}">
            <div class="alert alert-success d-flex align-items-center gap-2 py-2 small" role="alert">
                <span class="material-symbols-outlined fs-5">check_circle</span> <c:out value="${settingsMessage}" />
            </div>
        </c:if>
        <c:if test="${not empty settingsError}">
            <div class="alert alert-danger d-flex align-items-center gap-2 py-2 small" role="alert">
                <span class="material-symbols-outlined fs-5">error</span> <c:out value="${settingsError}" />
            </div>
        </c:if>

        <form method="post" action="${ctx}/admin/settings">

            <!-- Giao hàng & Freeship -->
            <div class="aura-card p-3 p-md-4 mb-4">
                <div class="section-title">
                    <span class="section-icon"><span class="material-symbols-outlined">local_shipping</span></span>
                    Giao hàng &amp; Freeship
                </div>
                <div class="row g-3">
                    <div class="col-md-4">
                        <label for="freeshipThreshold" class="form-label small fw-semibold mb-1">Mua từ bao nhiêu thì được Freeship</label>
                        <div class="input-group">
                            <input type="number" id="freeshipThreshold" name="freeshipThreshold" class="form-control aura-input"
                                   min="0" max="100000000" step="1" required value="${fn:escapeXml(form.freeshipThreshold)}">
                            <span class="input-group-text">đ</span>
                        </div>
                        <div class="form-text">Tính trên tiền các món khách chọn (trước khi trừ điểm). Nhập 0 = miễn phí giao hàng mọi đơn.</div>
                    </div>
                    <div class="col-md-4">
                        <label for="deliveryFee" class="form-label small fw-semibold mb-1">Phí giao hàng khi chưa đủ Freeship</label>
                        <div class="input-group">
                            <input type="number" id="deliveryFee" name="deliveryFee" class="form-control aura-input"
                                   min="0" max="1000000" step="1" required value="${fn:escapeXml(form.deliveryFee)}">
                            <span class="input-group-text">đ</span>
                        </div>
                        <div class="form-text">Cộng vào tổng thanh toán của đơn chưa đạt mức Freeship.</div>
                    </div>
                    <div class="col-md-4">
                        <label for="deliveryMinutes" class="form-label small fw-semibold mb-1">Thời gian giao dự kiến</label>
                        <div class="input-group">
                            <input type="number" id="deliveryMinutes" name="deliveryMinutes" class="form-control aura-input"
                                   min="5" max="300" step="1" required value="${fn:escapeXml(form.deliveryMinutes)}">
                            <span class="input-group-text">phút</span>
                        </div>
                        <div class="form-text">Khách thấy "Giao trong khoảng ... phút" ở bước Xác nhận đơn.</div>
                    </div>
                </div>
            </div>

            <!-- Điểm Aura -->
            <div class="aura-card p-3 p-md-4 mb-4">
                <div class="section-title">
                    <span class="section-icon"><span class="material-symbols-outlined">stars</span></span>
                    Dùng điểm Aura trừ vào đơn
                </div>
                <div class="row g-3">
                    <div class="col-md-6">
                        <label for="pointsStep" class="form-label small fw-semibold mb-1">Mốc điểm</label>
                        <div class="input-group">
                            <input type="number" id="pointsStep" name="pointsStep" class="form-control aura-input"
                                   min="1" max="100000" step="1" required value="${fn:escapeXml(form.pointsStep)}">
                            <span class="input-group-text">điểm</span>
                        </div>
                        <div class="form-text">Khách chỉ dùng được bội số của mốc này (VD mốc 10: 10, 20, 30... điểm - là bước của thanh kéo ở Giỏ hàng).</div>
                    </div>
                    <div class="col-md-6">
                        <label for="pointsStepValue" class="form-label small fw-semibold mb-1">Mỗi mốc được giảm</label>
                        <div class="input-group">
                            <input type="number" id="pointsStepValue" name="pointsStepValue" class="form-control aura-input"
                                   min="1000" max="10000000" step="1" required value="${fn:escapeXml(form.pointsStepValue)}">
                            <span class="input-group-text">đ</span>
                        </div>
                        <div class="form-text">Số tiền giảm không vượt quá tiền hàng của đơn.</div>
                    </div>
                </div>

                <div class="info-box small mt-3 mb-0">
                    <div class="fw-semibold mb-2">Đang áp dụng cho khách:</div>
                    <div class="d-flex flex-wrap gap-2 mb-2">
                        <c:forEach var="i" begin="1" end="4">
                            <span class="points-chip">
                                ${i * settings.pointsStep} điểm = -<fmt:formatNumber value="${i * settings.pointsStepValue}" pattern="#,##0" />đ
                            </span>
                        </c:forEach>
                        <span class="text-muted align-self-center">...</span>
                    </div>
                    <div class="text-muted">
                        Tích điểm (cố định): mỗi <fmt:formatNumber value="${moneyPerPoint}" pattern="#,##0" />đ tiền hàng sau khi trừ điểm = 1 điểm,
                        cộng khi khách thanh toán. Điểm dùng cho đơn được trừ lúc đặt và hoàn lại nếu khách hủy đơn chưa thanh toán.
                    </div>
                </div>
            </div>

            <div class="d-flex flex-wrap gap-2 justify-content-end">
                <a href="${ctx}/admin/settings" class="btn btn-soft px-4 py-2">Hoàn tác</a>
                <button type="submit" class="btn btn-aura px-4 py-2">
                    <span class="material-symbols-outlined fs-5">save</span> Lưu cấu hình
                </button>
            </div>
        </form>
    </main>

    <!-- Bootstrap 5 JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
