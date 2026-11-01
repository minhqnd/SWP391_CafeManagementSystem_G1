<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%><%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!doctype html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Aura Café | Đề xuất nhập hàng</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
<link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
<style>.box{background:#fff;border:1px solid #e5e7eb;border-radius:14px;padding:18px}.aura{background:#a51225;color:#fff}.restock-mobile-nav{display:none}@media(max-width:768px){.restock-mobile-nav{display:flex;justify-content:space-between;gap:12px;padding:12px 16px;background:#fff;border-bottom:1px solid #e5e7eb}}</style></head><body class="admin-body">
<nav class="restock-mobile-nav" aria-label="Điều hướng quản trị"><a href="${ctx}/admin-dashboard.jsp">Aura Café · Admin</a><a href="${ctx}/login?action=logout">Đăng xuất</a></nav>
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="restock"/></jsp:include><main class="admin-main-wrapper"><div class="d-flex flex-wrap gap-2 justify-content-between mb-4"><div><h1 class="h3 fw-bold">Đề xuất nhập hàng</h1><p class="text-muted">Nhu cầu nội bộ; chưa phải phiếu mua và không cập nhật tồn kho.</p></div><a class="btn btn-outline-dark align-self-start" href="${ctx}/admin/purchases">Sang Phiếu nhập hàng</a></div>
<c:if test="${not empty restockMessage}"><div class="alert alert-success"><c:out value="${restockMessage}"/></div></c:if><c:if test="${not empty restockError}"><div class="alert alert-danger"><c:out value="${restockError}"/></div></c:if>
<div class="row g-3"><div class="col-lg-4"><section class="box"><h2 class="h5">Tạo đề xuất</h2><form method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.restockCsrfToken}"><label class="form-label">Nguyên liệu</label><select class="form-select mb-2" name="ingredientId" required><c:forEach var="i" items="${allIngredients}"><option value="${i.id}"><c:out value="${i.name}"/> (${i.unit})</option></c:forEach></select>
<label class="form-label">Số lượng cần</label><input class="form-control mb-2" type="number" step="0.001" min="0.001" name="quantity" required>
<label class="form-label">Lý do</label><input class="form-control mb-2" name="reason" maxlength="500" required><label class="form-label">Ghi chú</label><textarea class="form-control mb-3" name="note" maxlength="500"></textarea>
<div class="d-flex gap-2"><button class="btn btn-light" name="action" value="saveDraft">Lưu nháp</button><button class="btn aura" name="action" value="create">Gửi duyệt</button></div></form></section>
<section class="box mt-3"><h2 class="h6">Nguyên liệu sắp hết</h2><c:forEach var="i" items="${lowStockList}"><div class="border-top py-2"><b><c:out value="${i.name}"/></b><div class="small text-danger">Còn ${i.currentStock} / mức tối thiểu ${i.minStockLevel} ${i.unit}</div></div></c:forEach></section></div>
<div class="col-lg-8"><section class="box"><h2 class="h5">Danh sách đề xuất</h2><div class="table-responsive"><table class="table align-middle"><thead><tr><th>Mã</th><th>Nguyên liệu</th><th>Người tạo</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
<c:forEach var="r" items="${requests}"><tr><td>RR-<fmt:formatNumber value="${r.id}" pattern="0000"/></td><td><c:forEach var="it" items="${r.items}"><div><c:out value="${it.ingredientName}"/>: ${it.quantity} ${it.unit}</div></c:forEach><small class="text-muted"><c:out value="${r.reason}"/></small></td><td><c:out value="${r.requesterName}"/></td><td><span class="badge text-bg-light">${r.status}</span></td><td>
<c:if test="${r.status == 'DRAFT'}"><form method="post" class="d-inline"><input type="hidden" name="csrfToken" value="${sessionScope.restockCsrfToken}"><input type="hidden" name="id" value="${r.id}"><button class="btn btn-sm btn-primary" name="action" value="submit">Gửi duyệt</button></form></c:if>
<c:if test="${r.status == 'PENDING_APPROVAL'}"><form method="post" class="d-inline"><input type="hidden" name="csrfToken" value="${sessionScope.restockCsrfToken}"><input type="hidden" name="id" value="${r.id}"><button class="btn btn-sm btn-success" name="action" value="approve">Duyệt</button><button class="btn btn-sm btn-outline-danger" name="action" value="reject">Từ chối</button></form></c:if>
</td></tr></c:forEach><c:if test="${empty requests}"><tr><td colspan="5" class="text-center text-muted">Chưa có đề xuất.</td></tr></c:if>
</tbody></table></div></section></div></div></main></body></html>

