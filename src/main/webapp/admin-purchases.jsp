<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%><%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="workflowAdmin" value="${true}"/>
<!doctype html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Aura Café | Đơn mua hàng</title><link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
<link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
<style>.box{background:#fff;border:1px solid #e5e7eb;border-radius:14px;padding:18px}.aura{background:#a51225;color:#fff}.restock-mobile-nav{display:none}@media(max-width:768px){.restock-mobile-nav{display:flex;justify-content:space-between;gap:12px;padding:12px 16px;background:#fff;border-bottom:1px solid #e5e7eb}}</style></head><body class="admin-body"><jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="purchases"/></jsp:include><main class="admin-main-wrapper"><nav class="d-flex flex-wrap gap-3 mb-3" aria-label="Kho và nhập hàng"><a href="${ctx}/${workflowAdmin ? 'admin' : 'barista'}/restock-requests">Đề xuất nguyên liệu</a><a href="${ctx}/${workflowAdmin ? 'admin' : 'barista'}/purchase-receipts">Kiểm nhận hàng</a><c:if test="${workflowAdmin}"><a href="${ctx}/admin/purchases">Đơn mua nhà cung cấp</a></c:if></nav><c:if test="${not empty workflowMessage}"><div class="alert alert-success" role="status"><c:out value="${workflowMessage}"/></div></c:if>
<c:if test="${not empty workflowError}"><div class="alert alert-danger" role="alert"><c:out value="${workflowError}"/></div></c:if><h1 class="h3">Đơn mua nhà cung cấp</h1><p>Đơn mua ghi số lượng đặt. Tồn kho chỉ tăng khi quản lý duyệt phiếu kiểm nhận.</p>
<details class="box mb-3"><summary>Tạo đơn mua</summary>
<form method="post" class="mt-3" id="createPurchaseForm">
    <input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}">
    <input type="hidden" name="action" value="create">
    <label class="form-label" for="purchaseRequest">Đề xuất đã duyệt (không bắt buộc)</label>
    <select class="form-select mb-2" name="restockRequestId" id="purchaseRequest">
        <option value="">Không liên kết</option>
        <c:forEach var="r" items="${approvedRequests}">
            <option value="${r.id}">RR-${r.id}: <c:out value="${r.itemsSummary}"/></option>
        </c:forEach>
    </select>
    <label class="form-label" for="purchaseSupplier">Nhà cung cấp</label>
    <select class="form-select mb-3" name="supplierId" id="purchaseSupplier" required>
        <c:forEach var="s" items="${activeSuppliers}"><option value="${s.id}"><c:out value="${s.name}"/></option></c:forEach>
    </select>
    <p class="text-muted">Chọn đề xuất để nạp đủ nguyên liệu; nhập tiền lô riêng cho từng dòng. Số lượng đặt có thể điều chỉnh.</p>
    <div id="purchaseLines"><jsp:include page="/components/purchase-order-line.jsp"/></div>
    <button type="button" class="btn btn-outline-secondary mb-3" id="addPurchaseLine">Thêm nguyên liệu</button>
    <label class="form-label d-block">Ghi chú<textarea class="form-control mb-2" name="note" maxlength="500"></textarea></label>
    <button class="btn aura">Tạo nháp</button>
</form>
</details>
<template id="purchaseLineTemplate"><jsp:include page="/components/purchase-order-line.jsp"/></template>
<c:forEach var="r" items="${approvedRequests}">
    <template id="purchaseRequest-${r.id}">
        <c:forEach var="it" items="${r.items}">
            <fieldset class="purchase-line border rounded p-3 mb-3">
                <legend class="float-none w-auto fs-6"><c:out value="${it.ingredientName}"/> (<c:out value="${it.unit}"/>)</legend>
                <input type="hidden" name="ingredientId" value="${it.ingredientId}">
                <label class="form-label d-block">Số lượng đặt
                    <input class="form-control" name="quantity" type="number" min="0.001" max="999999999.999" step="0.001" value="${it.quantity}" required>
                </label>
                <label class="form-label d-block">Tổng tiền lô của nguyên liệu này (đ)
                    <input class="form-control" name="totalPrice" type="number" min="0" max="9999999999.99" step="0.01" required>
                </label>
            </fieldset>
        </c:forEach>
    </template>
</c:forEach>
<script id="purchase-form-script">
(() => {
    const request = document.getElementById('purchaseRequest');
    const lines = document.getElementById('purchaseLines');
    const add = document.getElementById('addPurchaseLine');
    const blank = document.getElementById('purchaseLineTemplate');
    function selectRequest() {
        const template = request.value
            ? document.getElementById('purchaseRequest-' + request.value) : blank;
        if (!template) return;
        lines.replaceChildren(template.content.cloneNode(true));
        add.disabled = Boolean(request.value);
    }
    request.addEventListener('change', selectRequest);
    add.addEventListener('click', () => {
        if (!request.value && lines.querySelectorAll('.purchase-line').length < 100)
            lines.appendChild(blank.content.cloneNode(true));
    });
    lines.addEventListener('click', event => {
        const button = event.target.closest('[data-remove-line]');
        if (button && !request.value && lines.querySelectorAll('.purchase-line').length > 1)
            button.closest('.purchase-line').remove();
    });
    // Reconcile a request selection restored by the browser after a refresh/back navigation.
    if (request.value) selectRequest();
})();
</script>
<c:forEach var="po" items="${purchaseOrders}"><article class="box mb-3"><h2 class="h5">PO-${po.id} · <c:out value="${po.status}"/></h2><p><c:out value="${po.supplierName}"/> · Tổng tiền đặt: <fmt:formatNumber value="${po.totalCost}"/> đ · RR-${po.restockRequestId}</p><p><c:out value="${po.note}"/></p>
<table class="table"><thead><tr><th>Nguyên liệu</th><th>Đặt</th><th>Đã nhận (sau điều chỉnh)</th><th>Còn thiếu</th></tr></thead><tbody><c:forEach var="it" items="${po.items}"><tr><td><c:out value="${it.ingredientName}"/> (${it.unit})</td><td>${it.quantity}</td><td>${it.receivedQuantity}</td><td>${it.remainingQuantity}</td></tr></c:forEach></tbody></table>
<c:if test="${po.status == 'DRAFT'}"><c:if test="${po.items.size() == 1}"><details><summary>Sửa đơn nháp</summary><form method="post"><input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}"><input type="hidden" name="action" value="edit"><input type="hidden" name="id" value="${po.id}"><label class="form-label">Nhà cung cấp</label><select class="form-select mb-2" name="supplierId" required><c:forEach var="s" items="${activeSuppliers}"><option value="${s.id}" ${po.supplierId == s.id ? 'selected' : ''}><c:out value="${s.name}"/></option></c:forEach></select><label class="form-label">Số lượng đặt</label><input class="form-control mb-2" name="quantity" value="${po.items[0].quantity}" type="number" step="0.001" min="0.001" required><label class="form-label">Tổng tiền lô</label><input class="form-control mb-2" name="totalPrice" value="${po.totalCost}" type="number" min="0" step="0.01" required><label class="form-label">Ghi chú</label><textarea class="form-control mb-2" name="note" maxlength="500"><c:out value="${po.note}"/></textarea><label class="form-label">Lý do sửa</label><input class="form-control mb-2" name="editReason" maxlength="500" required><button class="btn btn-primary">Lưu thay đổi</button></form></details></c:if>
<form method="post" class="mt-2"><input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}"><input type="hidden" name="id" value="${po.id}"><button class="btn btn-primary" name="action" value="order">Đặt hàng</button></form></c:if>
<c:if test="${po.status == 'DRAFT' or po.status == 'ORDERED' or po.status == 'PARTIALLY_RECEIVED'}"><form method="post" class="mt-2"><input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}"><input type="hidden" name="id" value="${po.id}"><label class="form-label">Lý do hủy / đóng phần còn lại</label><input class="form-control mb-2" name="reason" maxlength="500" required><c:if test="${po.status != 'PARTIALLY_RECEIVED'}"><button class="btn btn-outline-danger" name="action" value="cancel">Hủy đơn</button></c:if> <c:if test="${po.status != 'DRAFT'}"><button class="btn btn-outline-secondary" name="action" value="close">Đóng phần còn thiếu</button> <a class="btn btn-success" href="${ctx}/admin/purchase-receipts">Kiểm nhận / duyệt nhận hàng</a></c:if></form></c:if></article></c:forEach><jsp:include page="/components/inventory-history.jsp"/></main></body></html>
