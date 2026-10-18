<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty sessionScope.user or sessionScope.user.role ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Aura Café | Đề xuất nhập hàng</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <style>
        .admin-main-wrapper { max-width:1600px; }
        .page-container { display:flex; flex-direction:column; gap:18px; }
        .overview-card, .data-card { background:#fff; border:1px solid #e7e5e4; border-radius:16px; box-shadow:0 1px 2px rgba(28,25,23,.04); }
        .overview-card { padding:22px; }
        .overview-heading { display:flex; align-items:flex-start; justify-content:space-between; gap:18px; }
        .table-ref { display:inline-block; padding:3px 8px; border-radius:6px; background:#f5f5f4; color:#78716c; font:700 11px/1.5 ui-monospace,monospace; }
        .page-title { margin:6px 0 3px; color:#1c1917; font-size:1.55rem; font-weight:800; letter-spacing:-.025em; }
        .page-subtitle { margin:0; color:#78716c; font-size:.84rem; }
        .stats-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:12px; padding-top:16px; margin-top:18px; border-top:1px solid #f5f5f4; }
        .stat-item { padding:12px 16px; border:1px solid #e7e5e4; border-radius:12px; background:#fafaf9; }
        .stat-item span { display:block; color:#78716c; font-size:10.5px; text-transform:uppercase; font-weight:800; letter-spacing:.03em; }
        .stat-item strong { display:block; margin-top:3px; color:#1c1917; font:800 1.45rem ui-monospace,monospace; }
        .stat-item.danger strong { color:#e11d48; }
        .stat-item.danger span { color:#e11d48; }
        .btn-aura { color:#fff; background:#900b1a; border-color:#900b1a; font-weight:700; font-size:.85rem; border-radius:10px; padding:.55rem 1.15rem; transition:all .2s; }
        .btn-aura:hover { color:#fff; background:#7a121d; border-color:#7a121d; }
        .btn-outline-custom { color:#292524; background:#fff; border:1px solid #e7e5e4; font-weight:700; font-size:.85rem; border-radius:10px; padding:.55rem 1.15rem; }
        .btn-outline-custom:hover { background:#fafaf9; border-color:#d6d3d1; }
        .section-title { font-size:1.05rem; font-weight:800; color:#1c1917; margin-bottom:12px; }
        .custom-table > :not(caption) > * > * { padding:.8rem .75rem; vertical-align:middle; font-size:.86rem; }
        .custom-table thead th { color:#78716c; font-size:11px; font-weight:800; letter-spacing:.04em; text-transform:uppercase; background:#fafaf9; }
        .badge-draft { background:#f5f5f4; color:#57534e; border:1px solid #e7e5e4; border-radius:999px; padding:3px 9px; font-size:11px; font-weight:700; }
        .btn-approve { background:#047857; color:#fff; font-weight:700; font-size:12px; border-radius:8px; padding:4px 10px; border:none; }
        .btn-approve:hover { background:#065f46; color:#fff; }
        .btn-reject { background:#fff; color:#78716c; font-weight:700; font-size:12px; border-radius:8px; padding:4px 10px; border:1px solid #e7e5e4; }
        .btn-reject:hover { background:#f5f5f4; color:#1c1917; }
        .mobile-admin-bar { display:none; }
        @media(max-width:991.98px) { .stats-grid { grid-template-columns:repeat(2,minmax(0,1fr)); } }
        @media(max-width:767.98px) {
            .mobile-admin-bar { display:flex; align-items:center; justify-content:space-between; padding:10px 15px; background:#fff; border-bottom:1px solid #e7e5e4; }
            .stats-grid { grid-template-columns:1fr; }
            .overview-heading { flex-direction:column; }
        }
    </style>
</head>
<body class="admin-body">
<div class="mobile-admin-bar">
    <a href="${ctx}/admin-dashboard.jsp" class="text-decoration-none fw-bold text-danger"><span class="material-symbols-outlined align-middle">local_cafe</span> AURA CAFÉ · ADMIN</a>
    <a href="${ctx}/login?action=logout" class="btn btn-sm btn-light">Đăng xuất</a>
</div>
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="restock" /></jsp:include>

<main class="admin-main-wrapper page-container">
    <c:if test="${not empty restockMessage}">
        <div class="alert alert-success alert-dismissible fade show mb-0" role="alert">
            <span class="material-symbols-outlined align-middle me-1">check_circle</span>
            <c:out value="${restockMessage}" />
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>
    <c:if test="${not empty restockError}">
        <div class="alert alert-danger alert-dismissible fade show mb-0" role="alert">
            <span class="material-symbols-outlined align-middle me-1">error</span>
            <c:out value="${restockError}" />
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <div class="overview-card">
        <div class="overview-heading">
            <div>
                <span class="table-ref">Bảng: purchase_orders (DRAFT)</span>
                <h1 class="page-title">Đề xuất nhập hàng</h1>
                <p class="page-subtitle">Nguyên liệu dưới mức tối thiểu và các phiếu nhập đang ở dạng nháp chờ duyệt.</p>
            </div>
            <div class="d-flex align-items-center gap-2">
                <a href="${ctx}/admin-dashboard.jsp#comingSoon" class="btn btn-outline-custom d-flex align-items-center gap-2">
                    <span class="material-symbols-outlined fs-5">description</span>
                    <span>Phiếu nhập hàng</span>
                </a>
                <button type="button" class="btn btn-aura d-flex align-items-center gap-2" data-bs-toggle="modal" data-bs-target="#restockModal">
                    <span class="material-symbols-outlined fs-5">post_add</span>
                    <span>Tạo đề xuất</span>
                </button>
            </div>
        </div>
        <div class="stats-grid">
            <div class="stat-item ${stats.lowStockCount > 0 ? 'danger' : ''}">
                <span>Dưới mức tối thiểu</span>
                <strong><c:out value="${stats.lowStockCount}" default="0" /></strong>
            </div>
            <div class="stat-item">
                <span>Đề xuất chờ duyệt</span>
                <strong><c:out value="${stats.draftCount}" default="0" /></strong>
            </div>
            <div class="stat-item">
                <span>Đã đặt hàng</span>
                <strong><c:out value="${stats.orderedCount}" default="0" /></strong>
            </div>
            <div class="stat-item">
                <span>Giá trị chờ duyệt</span>
                <strong><fmt:formatNumber value="${stats.draftTotalCost}" pattern="#,##0" />đ</strong>
            </div>
        </div>
    </div>

    <!-- Phần 1: Nguyên liệu cần nhập (current_stock <= min_stock_level) -->
    <div class="data-card p-4">
        <h2 class="section-title">Nguyên liệu cần nhập (current_stock &le; min_stock_level)</h2>
        <c:choose>
            <c:when test="${not empty lowStockList}">
                <div class="table-responsive">
                    <table class="table custom-table table-hover align-middle mb-0">
                        <thead>
                            <tr>
                                <th>Nguyên liệu</th>
                                <th>Nhà cung cấp</th>
                                <th class="text-end">Tồn hiện tại</th>
                                <th class="text-end">Mức tối thiểu</th>
                                <th class="text-end">Cần bù ít nhất</th>
                                <th class="text-end">Hành động</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="low" items="${lowStockList}">
                                <tr>
                                    <td><strong class="text-danger"><c:out value="${low.name}" /></strong></td>
                                    <td><c:out value="${low.supplierName}" default="—" /></td>
                                    <td class="text-end text-danger fw-bold"><fmt:formatNumber value="${low.currentStock}" pattern="#,##0.###" /> ${low.unit}</td>
                                    <td class="text-end text-muted"><fmt:formatNumber value="${low.minStockLevel}" pattern="#,##0.###" /> ${low.unit}</td>
                                    <td class="text-end text-success fw-bold">+<fmt:formatNumber value="${low.minStockLevel - low.currentStock}" pattern="#,##0.###" /> ${low.unit}</td>
                                    <td class="text-end">
                                        <button type="button" class="btn btn-sm btn-light border text-danger fw-bold"
                                                onclick="quickRestock(${low.supplierId != null ? low.supplierId : 0}, ${low.id}, ${low.minStockLevel - low.currentStock})">
                                            + Tạo đề xuất
                                        </button>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-muted small py-2">Tất cả nguyên liệu đang trên mức tối thiểu.</div>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- Phần 2: Đề xuất chờ duyệt -->
    <div class="data-card p-4">
        <h2 class="section-title">Đề xuất chờ duyệt (<c:out value="${stats.draftCount}" default="0" />)</h2>
        <div class="table-responsive">
            <table class="table custom-table table-hover align-middle mb-0">
                <thead>
                    <tr>
                        <th style="width: 90px;">Mã phiếu</th>
                        <th>Nhà cung cấp</th>
                        <th>Nguyên liệu</th>
                        <th class="text-end">Tổng tiền</th>
                        <th>Người tạo / Ngày</th>
                        <th>Ghi chú</th>
                        <th class="text-center">Trạng thái</th>
                        <th class="text-end" style="width: 140px;">Duyệt</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="req" items="${draftRequests}">
                        <tr>
                            <td class="text-danger fw-bold font-monospace">
                                PO-<fmt:formatNumber value="${req.id}" pattern="0000" />
                            </td>
                            <td><strong><c:out value="${req.supplierName}" /></strong></td>
                            <td><c:out value="${req.itemsSummary}" /></td>
                            <td class="text-end fw-bold font-monospace">
                                <fmt:formatNumber value="${req.totalCost}" pattern="#,##0" />đ
                            </td>
                            <td>
                                <div><c:out value="${req.creatorName}" default="Admin" /></div>
                                <small class="text-muted"><fmt:formatDate value="${req.createdAt}" pattern="dd/MM/yyyy HH:mm" /></small>
                            </td>
                            <td class="text-muted"><c:out value="${req.notes}" default="—" /></td>
                            <td class="text-center">
                                <span class="badge-draft">Nháp / Đề xuất</span>
                            </td>
                            <td class="text-end">
                                <form method="post" action="${ctx}/admin/restock-requests" class="d-inline" onsubmit="return confirm('Duyệt đề xuất và chuyển sang Đã đặt hàng?');">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.restockCsrfToken}">
                                    <input type="hidden" name="action" value="approve">
                                    <input type="hidden" name="id" value="${req.id}">
                                    <button type="submit" class="btn-approve me-1">Duyệt</button>
                                </form>
                                <form method="post" action="${ctx}/admin/restock-requests" class="d-inline" onsubmit="return confirm('Từ chối và hủy đề xuất này?');">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.restockCsrfToken}">
                                    <input type="hidden" name="action" value="reject">
                                    <input type="hidden" name="id" value="${req.id}">
                                    <button type="submit" class="btn-reject">Từ chối</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty draftRequests}">
                        <tr>
                            <td colspan="8" class="text-center py-4 text-muted">Hiện không có đề xuất nào đang chờ duyệt.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</main>

<!-- Modal Tạo Đề Xuất Nhập Hàng -->
<div class="modal fade" id="restockModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <form method="post" action="${ctx}/admin/restock-requests" id="restockForm">
                <input type="hidden" name="csrfToken" value="${sessionScope.restockCsrfToken}">
                <input type="hidden" name="action" value="create">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold">Tạo đề xuất nhập hàng</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Nhà cung cấp <span class="text-danger">*</span></label>
                        <select name="supplierId" id="restockSupplier" class="form-select" required>
                            <option value="">-- Chọn nhà cung cấp --</option>
                            <c:forEach var="sup" items="${activeSuppliers}">
                                <option value="${sup.id}"><c:out value="${sup.name}" /></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Nguyên liệu cần nhập <span class="text-danger">*</span></label>
                        <select name="ingredientId" id="restockIngredient" class="form-select" required>
                            <option value="">-- Chọn nguyên liệu --</option>
                            <c:forEach var="ing" items="${allIngredients}">
                                <option value="${ing.id}" data-supplier="${ing.supplierId}">
                                    <c:out value="${ing.name}" /> (ĐVT: ${ing.unit})
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="row g-2 mb-3">
                        <div class="col-6">
                            <label class="form-label fw-bold small text-secondary">Số lượng đề xuất <span class="text-danger">*</span></label>
                            <input type="number" step="0.001" name="quantity" id="restockQuantity" class="form-control" required placeholder="Ví dụ: 10, 20...">
                        </div>
                        <div class="col-6">
                            <label class="form-label fw-bold small text-secondary">Đơn giá dự kiến (VNĐ) <span class="text-danger">*</span></label>
                            <input type="number" step="100" name="unitPrice" id="restockPrice" class="form-control" required placeholder="Ví dụ: 40000">
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light border" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-aura">Tạo đề xuất</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    function quickRestock(supplierId, ingredientId, diffQty) {
        if (supplierId && supplierId > 0) {
            document.getElementById('restockSupplier').value = supplierId;
        }
        document.getElementById('restockIngredient').value = ingredientId;
        if (diffQty && diffQty > 0) {
            document.getElementById('restockQuantity').value = diffQty;
        }
        new bootstrap.Modal(document.getElementById('restockModal')).show();
    }
</script>
</body>
</html>
