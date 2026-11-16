<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:if test="${empty requestScope.workflowAdmin}"><c:redirect url="/login"/></c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Aura Café | Đề xuất nhập hàng</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <style>
        .admin-main-wrapper { max-width: 1600px; display: flex; flex-direction: column; gap: 18px; }
        .overview-card, .data-card, .low-stock-card { background: #fff; border: 1px solid #e7e5e4; border-radius: 16px; box-shadow: 0 1px 2px rgba(28,25,23,.04); }
        .overview-card { padding: 22px; }
        .overview-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; flex-wrap: wrap; }
        .table-ref { display: inline-block; padding: 3px 8px; border-radius: 6px; background: #f5f5f4; color: #78716c; font: 700 11px/1.5 ui-monospace,monospace; }
        .page-title { margin: 6px 0 3px; color: #1c1917; font-size: 1.55rem; font-weight: 800; letter-spacing: -.025em; }
        .page-subtitle { margin: 0; color: #78716c; font-size: .84rem; }
        .stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; padding-top: 16px; margin-top: 18px; border-top: 1px solid #f5f5f4; }
        .stat-item { padding: 12px 16px; border: 1px solid #e7e5e4; border-radius: 12px; background: #fafaf9; }
        .stat-item span { display: block; color: #78716c; font-size: 10.5px; text-transform: uppercase; font-weight: 800; letter-spacing: .03em; }
        .stat-item strong { display: block; margin-top: 3px; color: #1c1917; font: 800 1.4rem ui-monospace,monospace; }
        .stat-item.danger strong { color: #e11d48; }
        .stat-item.warning strong { color: #d97706; }
        .stat-item.success strong { color: #059669; }
        .btn-aura { color: #fff; background: #900b1a; border-color: #900b1a; font-weight: 700; font-size: .85rem; border-radius: 10px; padding: .55rem 1.15rem; transition: all .2s; }
        .btn-aura:hover { color: #fff; background: #7a121d; border-color: #7a121d; }
        .low-stock-card { padding: 16px 20px; border-color: #fecdd3; background: #fff1f2; }
        .low-stock-pill { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; border-radius: 999px; background: #fff; border: 1px solid #fda4af; color: #9f1239; font-size: 0.8rem; font-weight: 700; cursor: pointer; transition: all .15s; }
        .low-stock-pill:hover { background: #ffe4e6; transform: translateY(-1px); }
        .rr-table > :not(caption) > * > * { padding: .85rem .8rem; vertical-align: middle; font-size: .85rem; }
        .rr-table thead th { color: #78716c; font-size: 11px; font-weight: 800; letter-spacing: .04em; text-transform: uppercase; background: #fafaf9; white-space: nowrap; }
        .rr-badge { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; border-radius: 999px; font-size: 11px; font-weight: 800; white-space: nowrap; }
        .rr-pending { background: #fffbeb; color: #b45309; border: 1px solid #fde68a; }
        .rr-approved { background: #ecfdf5; color: #047857; border: 1px solid #a7f3d0; }
        .rr-rejected { background: #fff1f2; color: #be123c; border: 1px solid #fecdd3; }
        .rr-draft { background: #f1f5f9; color: #475569; border: 1px solid #cbd5e1; }
    </style>
</head>
<body class="admin-body">
<c:choose>
    <c:when test="${workflowAdmin}">
        <jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="restock"/></jsp:include>
    </c:when>
    <c:otherwise>
        <jsp:include page="/components/header.jsp"><jsp:param name="navbarMode" value="barista"/><jsp:param name="activePage" value="inventory"/></jsp:include>
    </c:otherwise>
</c:choose>

<main class="${workflowAdmin ? 'admin-main-wrapper' : 'container py-4 d-flex flex-column gap-3'}">
    <c:if test="${not empty workflowMessage}">
        <div class="alert alert-success alert-dismissible fade show mb-0" role="status">
            <span class="material-symbols-outlined align-middle me-1">check_circle</span>
            <c:out value="${workflowMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>
    <c:if test="${not empty workflowError}">
        <div class="alert alert-danger alert-dismissible fade show mb-0" role="alert">
            <span class="material-symbols-outlined align-middle me-1">error</span>
            <c:out value="${workflowError}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <div class="overview-card">
        <div class="overview-heading">
            <div>
                <span class="table-ref">Bảng: restock_requests · restock_request_items</span>
                <h1 class="page-title">Đề xuất nhập nguyên liệu</h1>
                <p class="page-subtitle">Barista gửi đề xuất bổ sung nguyên liệu. Khi Admin duyệt, hệ thống mở form lập phiếu mua.</p>
            </div>
            <div class="d-flex align-items-center gap-2">
                <c:if test="${workflowAdmin}">
                    <a href="${ctx}/admin/purchases" class="btn btn-light border fw-semibold d-flex align-items-center gap-1" style="border-radius: 10px;">
                        <span class="material-symbols-outlined fs-5">description</span>
                        <span>Sang Phiếu nhập hàng</span>
                    </a>
                </c:if>
                <button type="button" class="btn btn-aura d-flex align-items-center gap-2" data-bs-toggle="modal" data-bs-target="#createRestockModal">
                    <span class="material-symbols-outlined fs-5">add_circle</span>
                    <span>Gửi đề xuất nhập hàng</span>
                </button>
            </div>
        </div>
        <div class="stats-grid">
            <div class="stat-item ${restockStats.lowStock > 0 ? 'danger' : ''}">
                <span>Nguyên liệu dưới định mức</span>
                <strong><c:out value="${restockStats.lowStock}" default="0" /></strong>
            </div>
            <div class="stat-item warning">
                <span>Đang chờ duyệt</span>
                <strong><c:out value="${restockStats.pendingCount}" default="0" /></strong>
            </div>
            <div class="stat-item success">
                <span>Đã duyệt</span>
                <strong><c:out value="${restockStats.approvedCount}" default="0" /></strong>
            </div>
            <div class="stat-item">
                <span>Tổng đề xuất</span>
                <strong><c:out value="${fn:length(requests)}" default="0" /></strong>
            </div>
        </div>
    </div>

    <c:if test="${not empty lowStockList}">
        <div class="low-stock-card">
            <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-2">
                <div class="fw-bold text-danger d-flex align-items-center gap-1">
                    <span class="material-symbols-outlined fs-5">warning</span>
                    <span>Nguyên liệu đang dưới mức tối thiểu (Nhấn vào nguyên liệu để tạo đề xuất nhanh):</span>
                </div>
            </div>
            <div class="d-flex flex-wrap gap-2">
                <c:forEach var="i" items="${lowStockList}">
                    <c:set var="suggestQty" value="${i.minStockLevel - i.currentStock > 0 ? (i.minStockLevel - i.currentStock + 5) : 10}" />
                    <button type="button" class="low-stock-pill"
                            onclick="quickRestock(${i.id}, '${suggestQty}', 'Bổ sung ${fn:escapeXml(i.name)} đang dưới ngưỡng tối thiểu')">
                        <span><c:out value="${i.name}"/></span>
                        <span class="badge bg-danger text-white">Tồn: <fmt:formatNumber value="${i.currentStock}" pattern="#,##0.###" /> / <fmt:formatNumber value="${i.minStockLevel}" pattern="#,##0.###" /> ${i.unit}</span>
                        <span>+ Đề xuất ngay</span>
                    </button>
                </c:forEach>
            </div>
        </div>
    </c:if>

    <div class="data-card p-3">
        <div class="table-responsive">
            <table class="table rr-table table-hover align-middle mb-0">
                <thead>
                    <tr>
                        <th style="width: 100px;">Mã phiếu</th>
                        <th style="width: 170px;">Người đề xuất</th>
                        <th>Nguyên liệu &amp; Số lượng cần</th>
                        <th>Lý do &amp; Ghi chú</th>
                        <th class="text-center" style="width: 150px;">Trạng thái</th>
                        <th class="text-end" style="min-width: 240px;">Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="r" items="${requests}">
                        <tr>
                            <td>
                                <span class="fw-bold font-monospace text-danger">#RR-${r.id}</span>
                                <div class="text-muted" style="font-size: 11px;">
                                    <fmt:formatDate value="${r.createdAt}" pattern="dd/MM/yyyy HH:mm" />
                                </div>
                            </td>
                            <td>
                                <strong class="text-dark"><c:out value="${r.requesterName}"/></strong>
                                <c:if test="${not empty r.reviewerName}">
                                    <div class="text-muted small">Duyệt bởi: <c:out value="${r.reviewerName}"/></div>
                                </c:if>
                            </td>
                            <td>
                                <span class="fw-bold text-dark"><c:out value="${r.itemsSummary}"/></span>
                            </td>
                            <td>
                                <div><c:out value="${r.reason}"/></div>
                                <c:if test="${not empty r.note}">
                                    <small class="text-muted d-block"><c:out value="${r.note}"/></small>
                                </c:if>
                                <c:if test="${not empty r.reviewNote}">
                                    <small class="text-primary d-block mt-1">Phản hồi: <c:out value="${r.reviewNote}"/></small>
                                </c:if>
                            </td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${r.status eq 'PENDING_APPROVAL'}">
                                        <span class="rr-badge rr-pending">Chờ duyệt</span>
                                    </c:when>
                                    <c:when test="${r.status eq 'APPROVED'}">
                                        <span class="rr-badge rr-approved">Đã duyệt</span>
                                    </c:when>
                                    <c:when test="${r.status eq 'REJECTED'}">
                                        <span class="rr-badge rr-rejected">Từ chối</span>
                                    </c:when>
                                    <c:when test="${r.status eq 'CANCELLED'}">
                                        <span class="rr-badge rr-draft">Đã kết thúc</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="rr-badge rr-draft"><c:out value="${r.status}"/></span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-end">
                                <div class="d-inline-flex flex-wrap justify-content-end gap-1">
                                    <c:if test="${workflowAdmin and r.status eq 'PENDING_APPROVAL'}">
                                        <form method="post" class="d-inline-flex flex-wrap justify-content-end gap-1">
                                            <input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}">
                                            <input type="hidden" name="id" value="${r.id}">
                                            <button type="submit" class="btn btn-sm btn-success fw-semibold" name="action" value="approveAndOrder">
                                                Duyệt &amp; Lập phiếu mua
                                            </button>
                                            <button type="submit" class="btn btn-sm btn-outline-danger" name="action" value="reject">
                                                Từ chối
                                            </button>
                                        </form>
                                    </c:if>

                                    <c:if test="${workflowAdmin and r.status eq 'APPROVED' and empty r.purchaseOrderId}">
                                        <a href="${ctx}/admin/purchases?fromRequestId=${r.id}" class="btn btn-sm btn-aura py-1 px-2 d-inline-flex align-items-center gap-1">
                                            <span class="material-symbols-outlined" style="font-size: 15px;">shopping_cart_checkout</span>
                                            <span>Lập phiếu mua</span>
                                        </a>
                                    </c:if>

                                    <c:if test="${(workflowAdmin or r.requesterId == sessionScope.user.id) and (r.status eq 'DRAFT' or r.status eq 'PENDING_APPROVAL' or r.status eq 'CHANGES_REQUESTED')}">
                                        <c:if test="${r.status eq 'DRAFT' or r.status eq 'CHANGES_REQUESTED'}">
                                            <form method="post" class="d-inline">
                                                <input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}">
                                                <input type="hidden" name="id" value="${r.id}">
                                                <button type="submit" class="btn btn-sm btn-outline-primary" name="action" value="submit">Gửi duyệt</button>
                                            </form>
                                        </c:if>
                                        <button type="button" class="btn btn-sm btn-light border"
                                                data-bs-toggle="modal" data-bs-target="#editRestockModal-${r.id}">
                                            Sửa
                                        </button>
                                    </c:if>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty requests}">
                        <tr>
                            <td colspan="6" class="text-center py-5 text-muted">Chưa có đề xuất nhập hàng nào.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</main>

<!-- MODAL TẠO ĐỀ XUẤT MỚI -->
<div class="modal fade" id="createRestockModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <form method="post">
                <input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}">
                <div class="modal-header bg-light">
                    <h5 class="modal-title fw-bold">Gửi đề xuất nhập nguyên liệu</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Nguyên liệu cần nhập <span class="text-danger">*</span></label>
                        <select class="form-select" name="ingredientId" id="restockIngSelect" required>
                            <c:forEach var="i" items="${allIngredients}">
                                <option value="${i.id}">
                                    <c:out value="${i.name}"/> (${i.unit}) · Tồn: <fmt:formatNumber value="${i.currentStock}" pattern="#,##0.###" />
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Số lượng đề xuất <span class="text-danger">*</span></label>
                        <input class="form-control" name="quantity" id="restockQtyInput" type="number" min="0.001" step="0.001" placeholder="VD: 10" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Lý do đề xuất <span class="text-danger">*</span></label>
                        <input class="form-control" name="reason" id="restockReasonInput" value="Bổ sung nguyên liệu sắp hết cho quầy pha chế" maxlength="500" required>
                    </div>
                    <div>
                        <label class="form-label fw-bold small text-secondary">Ghi chú thêm</label>
                        <textarea class="form-control" name="note" rows="2" maxlength="500" placeholder="Ghi chú độ ưu tiên hoặc nhãn hiệu..."></textarea>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-light border" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-aura" name="action" value="create">Gửi đề xuất</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- MODAL SỬA ĐỀ XUẤT -->
<c:forEach var="r" items="${requests}">
    <c:if test="${not empty r.items}">
        <div class="modal fade" id="editRestockModal-${r.id}" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow">
                    <form method="post">
                        <input type="hidden" name="csrfToken" value="${sessionScope.inventoryWorkflowToken}">
                        <input type="hidden" name="id" value="${r.id}">
                        <input type="hidden" name="action" value="edit">
                        <div class="modal-header bg-light">
                            <h5 class="modal-title fw-bold">Chỉnh sửa đề xuất #RR-${r.id}</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body p-4">
                            <c:forEach var="it" items="${r.items}">
                                <div class="mb-3">
                                    <label class="form-label fw-bold small text-secondary">Nguyên liệu</label>
                                    <select class="form-select" name="ingredientId" required>
                                        <option value="${it.ingredientId}"><c:out value="${it.ingredientName}"/></option>
                                        <c:forEach var="i" items="${allIngredients}">
                                            <c:if test="${i.id != it.ingredientId}">
                                                <option value="${i.id}"><c:out value="${i.name}"/> (${i.unit})</option>
                                            </c:if>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-bold small text-secondary">Số lượng cần</label>
                                    <input class="form-control" name="quantity" type="number" step="0.001" min="0.001" value="${it.quantity}" required>
                                </div>
                            </c:forEach>
                            <div class="mb-3">
                                <label class="form-label fw-bold small text-secondary">Lý do đề xuất</label>
                                <input class="form-control" name="reason" value="<c:out value='${r.reason}'/>" maxlength="500" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label fw-bold small text-secondary">Ghi chú</label>
                                <textarea class="form-control" name="note" rows="2" maxlength="500"><c:out value="${r.note}"/></textarea>
                            </div>
                            <input type="hidden" name="editReason" value="Cập nhật số lượng đề xuất">
                        </div>
                        <div class="modal-footer bg-light">
                            <button type="button" class="btn btn-light border" data-bs-dismiss="modal">Đóng</button>
                            <button type="submit" class="btn btn-aura">Lưu thay đổi</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </c:if>
</c:forEach>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    function quickRestock(ingId, qty, reason) {
        const ingSelect = document.getElementById('restockIngSelect');
        const qtyInput = document.getElementById('restockQtyInput');
        const reasonInput = document.getElementById('restockReasonInput');
        if (ingSelect) ingSelect.value = ingId;
        if (qtyInput) qtyInput.value = parseFloat(qty) > 0 ? parseFloat(qty).toFixed(3).replace(/\.?0+$/, '') : '10';
        if (reasonInput && reason) reasonInput.value = reason;
        new bootstrap.Modal(document.getElementById('createRestockModal')).show();
    }
</script>
</body>
</html>
