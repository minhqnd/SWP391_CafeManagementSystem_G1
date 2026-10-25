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
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-settings.css" rel="stylesheet">
    <style>
        .admin-main-wrapper.settings-page { max-width:1280px; }
        .settings-page .breadcrumb { margin-bottom:12px; }
        .settings-page > .mb-4 { padding:20px; border:1px solid #e7e5e4; border-radius:16px; background:#fff; }
        .settings-page .aura-card { border:1px solid #e7e5e4; border-radius:16px; box-shadow:0 1px 2px rgba(28,25,23,.04); }
        .settings-page .aura-card .section-title { font-weight:800; }
        .settings-mobile-bar { display:none; }
        @media(max-width:767.98px) { .settings-mobile-bar { display:flex; align-items:center; justify-content:space-between; padding:10px 15px; background:#fff; border-bottom:1px solid #e7e5e4; } }
    </style>
</head>
<body class="admin-body aura-cart-page">

    <div class="settings-mobile-bar">
        <a href="${ctx}/admin-dashboard.jsp" class="text-decoration-none fw-bold text-danger"><span class="material-symbols-outlined align-middle">local_cafe</span> AURA CAFÉ · ADMIN</a>
        <a href="${ctx}/login?action=logout" class="btn btn-sm btn-light">Đăng xuất</a>
    </div>
    <jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="settings" /></jsp:include>

    <main class="admin-main-wrapper settings-page">

        <nav aria-label="breadcrumb">
            <ol class="breadcrumb small mb-2">
                <li class="breadcrumb-item"><a href="${ctx}/admin-dashboard.jsp">Dashboard</a></li>
                <li class="breadcrumb-item active fw-semibold text-dark" aria-current="page">Cài đặt bán hàng online</li>
            </ol>
        </nav>
        <div class="mb-4">
            <div class="d-flex justify-content-between align-items-start gap-3">
                <div>
            <div class="eyebrow">Cài đặt hệ thống</div>
            <h1 class="h3 fw-bold mb-1">Bán hàng online</h1>
            <div class="text-muted small">
                Áp dụng cho Giỏ hàng, Xác nhận đơn và Thanh toán của khách. Thay đổi có hiệu lực ngay ở lần tải trang tiếp theo
                (đơn đã đặt giữ nguyên số tiền lúc đặt).
            </div>
                </div>
                <button type="button" class="btn-add-setting flex-shrink-0" data-bs-toggle="modal" data-bs-target="#addSettingModal">
                    <span class="material-symbols-outlined">add</span> Thêm khóa cài đặt
                </button>
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
                        <label for="deliveryFee" class="form-label small fw-semibold mb-1">Phí giao tạm tính (khi chưa chọn quận)</label>
                        <div class="input-group">
                            <input type="number" id="deliveryFee" name="deliveryFee" class="form-control aura-input"
                                   min="0" max="1000000" step="1" required value="${fn:escapeXml(form.deliveryFee)}">
                            <span class="input-group-text">đ</span>
                        </div>
                        <div class="form-text">Phí giao thật tính theo quận khách chọn, chỉnh ở màn <a href="${pageContext.request.contextPath}/admin/delivery-areas">Phí giao theo quận</a>.</div>
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
                        <label for="pointsEarnUnit" class="form-label small fw-semibold mb-1">Chi tiêu để nhận 1 điểm</label>
                        <div class="input-group">
                            <input type="number" id="pointsEarnUnit" name="pointsEarnUnit" class="form-control aura-input"
                                   min="1000" max="100000000" step="1000" required value="${fn:escapeXml(form.pointsEarnUnit)}">
                            <span class="input-group-text">đ / điểm</span>
                        </div>
                        <div class="form-text">Tính trên tiền hàng sau khi trừ điểm; Admin có thể thay đổi, hệ thống không hard-code.</div>
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
                        Tích điểm theo cấu hình: mỗi <fmt:formatNumber value="${moneyPerPoint}" pattern="#,##0" />đ tiền hàng sau khi trừ điểm = 1 điểm,
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

        <%-- Phần mở rộng: giữ nguyên form bán hàng online phía trên và bổ sung quản lý toàn bộ khóa phía dưới. --%>
        <section class="all-settings-section mt-5">
            <div class="mb-3">
                <div class="eyebrow">SHOP_SETTINGS</div>
                <h2 class="h5 fw-bold mb-1">Tất cả khóa cài đặt</h2>
                <p class="text-muted small mb-0">Quản lý giờ ca và các khóa tùy chỉnh đang có trong cơ sở dữ liệu.</p>
            </div>
            <form method="post" action="${ctx}/admin/settings">
                <input type="hidden" name="action" value="saveAllSettings">
                <div class="settings-grid">
                    <c:forEach var="group" items="${settingGroups}">
                        <section class="settings-card">
                            <div class="settings-card-title">
                                <span class="group-icon material-symbols-outlined">
                                    <c:choose>
                                        <c:when test="${group.key == 'Giao hàng'}">local_shipping</c:when>
                                        <c:when test="${group.key == 'Tích điểm'}">stars</c:when>
                                        <c:when test="${group.key == 'Ca làm việc'}">schedule</c:when>
                                        <c:otherwise>tune</c:otherwise>
                                    </c:choose>
                                </span>
                                <h2><c:out value="${group.key}" /></h2>
                            </div>
                            <div class="settings-fields">
                                <c:forEach var="setting" items="${group.value}">
                                    <div class="setting-field">
                                        <label for="setting_${setting.key}"><c:out value="${setting.key}" /></label>
                                        <input id="setting_${setting.key}" name="setting_${setting.key}" maxlength="255" required
                                               value="${fn:escapeXml(setting.value)}"
                                               placeholder="${fn:startsWith(setting.key, 'shift_') ? 'HH:mm-HH:mm' : ''}">
                                        <div class="field-meta">
                                            <code><c:out value="${setting.key}" /></code>
                                            <span>Cập nhật lúc <c:out value="${setting.updatedAtLabel}" /></span>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                        </section>
                    </c:forEach>
                </div>
                <div class="settings-actions">
                    <a href="${ctx}/admin/settings" class="btn-cancel">Hoàn tác</a>
                    <button type="submit" class="btn-save"><span class="material-symbols-outlined">save</span>Lưu tất cả khóa</button>
                </div>
            </form>
        </section>
    </main>

    <div class="modal fade" id="addSettingModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content settings-modal">
                <form method="post" action="${ctx}/admin/settings">
                    <input type="hidden" name="action" value="addSetting">
                    <div class="modal-header">
                        <div><small>SHOP_SETTINGS</small><h2 class="modal-title">Thêm khóa cài đặt</h2></div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                    </div>
                    <div class="modal-body">
                        <div class="setting-field mb-3">
                            <label for="newKey">Khóa cài đặt</label>
                            <input id="newKey" name="newKey" required maxlength="50" pattern="[a-z0-9_]+"
                                   value="${fn:escapeXml(newKey)}" placeholder="Ví dụ: tax_rate">
                            <div class="input-help">Chỉ dùng chữ thường, số và dấu gạch dưới.</div>
                        </div>
                        <div class="setting-field">
                            <label for="newValue">Giá trị</label>
                            <input id="newValue" name="newValue" required maxlength="255"
                                   value="${fn:escapeXml(newValue)}" placeholder="Nhập giá trị cài đặt">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn-cancel" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn-save"><span class="material-symbols-outlined">add</span>Thêm</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Bootstrap 5 JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <c:if test="${openAddModal}">
        <script>new bootstrap.Modal(document.getElementById('addSettingModal')).show();</script>
    </c:if>
</body>
</html>
