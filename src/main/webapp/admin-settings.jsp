<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty settingsPageLoaded}"><c:redirect url="/admin/settings" /></c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Aura Café | Cài đặt hệ thống</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/cart.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-settings.css?v=3" rel="stylesheet">
</head>
<body class="admin-body aura-cart-page">
    <div class="settings-mobile-bar">
        <a href="${ctx}/admin-dashboard.jsp" class="text-decoration-none fw-bold text-danger"><span class="material-symbols-outlined align-middle">local_cafe</span> AURA CAFÉ · ADMIN</a>
        <a href="${ctx}/login?action=logout" class="btn btn-sm btn-light">Đăng xuất</a>
    </div>
    <jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="settings" /></jsp:include>
    <main class="admin-main-wrapper settings-page">
        <div class="settings-page-heading mb-4">
            <h1 class="h3 fw-bold mb-0">CÀI ĐẶT HỆ THỐNG</h1>
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
        <c:choose>
            <c:when test="${empty settingGroups}">
                <div class="alert alert-warning" role="alert">Không có cấu hình cửa hàng để hiển thị.</div>
            </c:when>
            <c:otherwise>
                <form method="post" action="${ctx}/admin/settings">
                    <input type="hidden" name="csrfToken" value="${sessionScope.settingsCsrfToken}">
                    <c:forEach var="group" items="${settingGroups}">
                        <section class="aura-card settings-group p-3 p-md-4 mb-4">
                            <h2 class="section-title">
                                <span class="section-icon"><span class="material-symbols-outlined"><c:choose>
                                    <c:when test="${group.key == 'Dùng điểm Aura trừ vào đơn'}">stars</c:when>
                                    <c:when test="${group.key == 'Giao hàng & Freeship'}">local_shipping</c:when>
                                    <c:when test="${group.key == 'Thông tin ngân hàng'}">account_balance</c:when>
                                    <c:when test="${group.key == 'Thanh toán'}">payments</c:when>
                                    <c:when test="${group.key == 'Ca làm việc'}">schedule</c:when>
                                    <c:otherwise>tune</c:otherwise>
                                </c:choose></span></span>
                                <c:out value="${group.key}" />
                            </h2>
                            <div class="row g-3">
                                <c:forEach var="setting" items="${group.value}">
                                    <c:set var="key" value="${setting.key}" />
                                    <c:set var="label" value="${fn:replace(key, '_', ' ')}" />
                                    <c:set var="unit" value="" />
                                    <c:set var="numeric" value="false" />
                                    <c:set var="min" value="0" />
                                    <c:set var="max" value="" />
                                    <c:set var="step" value="1" />
                                    <c:choose>
                                        <c:when test="${key == 'points_step'}"><c:set var="label" value="Mốc điểm" /><c:set var="unit" value="điểm" /><c:set var="numeric" value="true" /><c:set var="min" value="1" /><c:set var="max" value="100000" /></c:when>
                                        <c:when test="${key == 'points_step_value'}"><c:set var="label" value="Mỗi mốc được giảm" /><c:set var="unit" value="đ" /><c:set var="numeric" value="true" /><c:set var="min" value="1000" /><c:set var="max" value="10000000" /></c:when>
                                        <c:when test="${key == 'points_earn_unit'}"><c:set var="label" value="Chi tiêu để nhận 1 điểm" /><c:set var="unit" value="đ / điểm" /><c:set var="numeric" value="true" /><c:set var="min" value="1000" /><c:set var="max" value="100000000" /><c:set var="step" value="1000" /></c:when>
                                        <c:when test="${key == 'freeship_threshold'}"><c:set var="label" value="Giá trị đơn để được Freeship" /><c:set var="unit" value="đ" /><c:set var="numeric" value="true" /><c:set var="max" value="100000000" /></c:when>
                                        <c:when test="${key == 'delivery_fee'}"><c:set var="label" value="Phí giao hàng (cố định mọi đơn)" /><c:set var="unit" value="đ" /><c:set var="numeric" value="true" /><c:set var="max" value="1000000" /></c:when>
                                        <c:when test="${key == 'delivery_minutes'}"><c:set var="label" value="Thời gian giao dự kiến" /><c:set var="unit" value="phút" /><c:set var="numeric" value="true" /><c:set var="min" value="5" /><c:set var="max" value="300" /></c:when>
                                        <c:when test="${key == 'payment_bank_id'}"><c:set var="label" value="Mã ngân hàng" /></c:when>
                                        <c:when test="${key == 'payment_bank_name'}"><c:set var="label" value="Tên ngân hàng" /></c:when>
                                        <c:when test="${key == 'payment_account_no'}"><c:set var="label" value="Số tài khoản" /></c:when>
                                        <c:when test="${key == 'payment_account_name'}"><c:set var="label" value="Tên chủ tài khoản" /></c:when>
                                        <c:when test="${key == 'payment_gateway_url'}"><c:set var="label" value="Địa chỉ cổng thanh toán" /></c:when>
                                        <c:when test="${key == 'payment_callback_secret'}"><c:set var="label" value="Mã xác thực thanh toán" /></c:when>
                                        <c:when test="${key == 'payment_test_mode'}"><c:set var="label" value="Chế độ thử nghiệm thanh toán" /></c:when>
                                    </c:choose>
                                    <div class="col-md-6 col-lg-4">
                                        <label for="setting_${fn:escapeXml(key)}" class="form-label small fw-semibold mb-1"><c:out value="${label}" /></label>
                                        <div class="input-group">
                                            <c:choose>
                                                <c:when test="${numeric}"><input type="number" id="setting_${fn:escapeXml(key)}" name="setting_${fn:escapeXml(key)}" class="form-control aura-input" min="${min}" max="${max}" step="${step}" required value="${fn:escapeXml(form[key])}"></c:when>
                                                <c:otherwise><input type="text" id="setting_${fn:escapeXml(key)}" name="setting_${fn:escapeXml(key)}" class="form-control aura-input" maxlength="255" required value="${fn:escapeXml(form[key])}" <c:if test="${fn:startsWith(key, 'shift_')}">pattern="(?:[01][0-9]|2[0-3]):[0-5][0-9]-(?:[01][0-9]|2[0-3]):[0-5][0-9]" placeholder="HH:mm-HH:mm"</c:if>></c:otherwise>
                                            </c:choose>
                                            <c:if test="${not empty unit}"><span class="input-group-text"><c:out value="${unit}" /></span></c:if>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                        </section>
                    </c:forEach>
                    <div class="d-flex flex-wrap gap-2 justify-content-end">
                        <a href="${ctx}/admin/settings" class="btn btn-soft px-4 py-2">Hoàn tác</a>
                        <button type="submit" class="btn btn-aura px-4 py-2"><span class="material-symbols-outlined fs-5">save</span> Lưu cấu hình</button>
                    </div>
                </form>
            </c:otherwise>
        </c:choose>
    </main>
</body>
</html>
