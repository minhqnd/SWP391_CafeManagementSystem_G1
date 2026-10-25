<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty sessionScope.user or sessionScope.user.role ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<%-- Trang chỉ hiển thị qua AdminDeliveryAreaController --%>
<c:if test="${empty settings}"><c:redirect url="/admin/delivery-areas" /></c:if>
<c:set var="csrf" value="${fn:escapeXml(sessionScope.areaCsrfToken)}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Phí giao theo quận | Aura Café</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-shifts.css" rel="stylesheet">
</head>
<body class="admin-body">
    <jsp:include page="/components/admin-sidebar.jsp">
        <jsp:param name="activeMenu" value="deliveryAreas" />
    </jsp:include>

    <main class="admin-main-wrapper shift-page">
        <c:if test="${not empty areaSuccessMsg}">
            <div class="alert alert-success alert-dismissible fade show shift-alert" role="alert">
                <span class="material-symbols-outlined">check_circle</span>
                <span><c:out value="${areaSuccessMsg}" /></span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
            </div>
        </c:if>
        <c:if test="${not empty areaErrorMsg}">
            <div class="alert alert-danger alert-dismissible fade show shift-alert" role="alert">
                <span class="material-symbols-outlined">error</span>
                <span><c:out value="${areaErrorMsg}" /></span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
            </div>
        </c:if>

        <header class="shift-page-header">
            <span class="shift-table-ref">Bảng: <strong>delivery_areas</strong></span>
            <h1 class="h3 fw-bold mt-1 mb-1">Phí giao hàng theo quận</h1>
            <p class="text-muted mb-0" style="font-size: 0.85rem;">
                Mỗi quận một mức phí cố định. Khách chọn quận ở bước đặt hàng thì phí giao = phí của quận đó.
                Đơn có tiền hàng từ <strong><fmt:formatNumber value="${settings.freeshipThreshold}" pattern="#,##0" />đ</strong>
                được miễn phí giao (chỉnh ở <a href="${ctx}/admin/settings">Cài đặt cửa hàng</a>).
                Bỏ tick "Nhận giao" thì khách không chọn được quận đó.
            </p>
        </header>

        <section class="shift-list-card">
            <form method="post" action="${ctx}/admin/delivery-areas">
                <input type="hidden" name="csrfToken" value="${csrf}">
                <input type="hidden" name="action" value="save">
                <div class="table-responsive">
                    <table class="shift-table">
                        <thead>
                            <tr>
                                <th scope="col">Quận / Huyện</th>
                                <th scope="col">Phí giao (đ)</th>
                                <th scope="col" class="text-center">Nhận giao</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="a" items="${areas}">
                                <tr>
                                    <td class="shift-name-cell"><c:out value="${a.district}" /></td>
                                    <td style="max-width: 220px;">
                                        <div class="input-group input-group-sm">
                                            <input type="number" class="form-control" name="fee_${a.id}" min="0" max="1000000" step="1000"
                                                   required value="<fmt:formatNumber value='${a.deliveryFee}' pattern='0' />"
                                                   aria-label="Phí giao ${fn:escapeXml(a.district)}">
                                            <span class="input-group-text">đ</span>
                                        </div>
                                    </td>
                                    <td class="text-center">
                                        <input type="checkbox" class="form-check-input" name="active_${a.id}" ${a.active ? 'checked' : ''}
                                               aria-label="Nhận giao ${fn:escapeXml(a.district)}">
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div class="d-flex justify-content-end p-3">
                    <button type="submit" class="shift-save-button">
                        <span class="material-symbols-outlined">save</span> Lưu phí giao
                    </button>
                </div>
            </form>
        </section>

        <section class="shift-page-header">
            <h2 class="h6 fw-bold mb-3">Thêm quận / huyện</h2>
            <form method="post" action="${ctx}/admin/delivery-areas" class="row g-2 align-items-end">
                <input type="hidden" name="csrfToken" value="${csrf}">
                <input type="hidden" name="action" value="add">
                <div class="col-sm-5">
                    <label class="form-label small fw-semibold" for="newDistrict">Tên quận / huyện</label>
                    <input class="form-control" id="newDistrict" name="district" maxlength="50" required placeholder="VD: Gia Lâm">
                </div>
                <div class="col-sm-4">
                    <label class="form-label small fw-semibold" for="newFee">Phí giao (đ)</label>
                    <input class="form-control" type="number" id="newFee" name="deliveryFee" min="0" max="1000000" step="1000" required placeholder="VD: 35000">
                </div>
                <div class="col-sm-3">
                    <button type="submit" class="shift-add-button w-100 justify-content-center">
                        <span class="material-symbols-outlined">add</span> Thêm
                    </button>
                </div>
            </form>
        </section>
    </main>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
