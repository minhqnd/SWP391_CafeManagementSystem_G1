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
    <title>Aura Café | Tồn kho & Lịch sử xuất nhập</title>
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
        .stat-item small { display:block; margin-top:2px; color:#a8a29e; font-size:11px; }
        .stat-item.danger strong { color:#e11d48; }
        .stat-item.danger span { color:#e11d48; }
        .btn-aura { color:#fff; background:#900b1a; border-color:#900b1a; font-weight:700; font-size:.85rem; border-radius:10px; padding:.55rem 1.15rem; transition:all .2s; }
        .btn-aura:hover { color:#fff; background:#7a121d; border-color:#7a121d; }
        .btn-outline-custom { color:#292524; background:#fff; border:1px solid #e7e5e4; font-weight:700; font-size:.85rem; border-radius:10px; padding:.55rem 1.15rem; }
        .btn-outline-custom:hover { background:#fafaf9; border-color:#d6d3d1; }
        .section-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; }
        .section-title { font-size:1.05rem; font-weight:800; color:#1c1917; margin:0; }
        .custom-table > :not(caption) > * > * { padding:.75rem .6rem; vertical-align:middle; font-size:.84rem; }
        .custom-table thead th { color:#78716c; font-size:10.5px; font-weight:800; letter-spacing:.04em; text-transform:uppercase; background:#fafaf9; }
        .badge-safe { background:#ecfdf5; color:#047857; border:1px solid #a7f3d0; border-radius:999px; padding:3px 8px; font-size:10.5px; font-weight:700; }
        .badge-warning { background:#fffbeb; color:#b45309; border:1px solid #fde68a; border-radius:999px; padding:3px 8px; font-size:10.5px; font-weight:700; }
        .badge-danger { background:#fff1f2; color:#be123c; border:1px solid #fecdd3; border-radius:999px; padding:3px 8px; font-size:10.5px; font-weight:700; }

        /* Transaction Feed Items */
        .tx-feed { display:flex; flex-direction:column; gap:10px; max-height:560px; overflow-y:auto; padding-right:4px; }
        .tx-item { background:#fafaf9; border:1px solid #e7e5e4; border-radius:12px; padding:12px 14px; display:flex; align-items:center; justify-content:space-between; }
        .tx-tag { font-size:10px; font-weight:800; border-radius:6px; padding:2px 6px; display:inline-block; margin-bottom:3px; }
        .tx-tag-in { background:#ecfdf5; color:#047857; border:1px solid #a7f3d0; }
        .tx-tag-out { background:#fef3c7; color:#92400e; border:1px solid #fde68a; }
        .tx-tag-adj { background:#eff6ff; color:#1d4ed8; border:1px solid #bfdbfe; }
        .tx-tag-spoil { background:#fff1f2; color:#be123c; border:1px solid #fecdd3; }
        .tx-title { font-weight:800; font-size:.87rem; color:#1c1917; margin:0; }
        .tx-meta { font-size:11px; color:#78716c; margin-top:2px; }
        .tx-qty-pos { font-size:.95rem; font-weight:800; color:#047857; font-family:ui-monospace,monospace; }
        .tx-qty-neg { font-size:.95rem; font-weight:800; color:#e11d48; font-family:ui-monospace,monospace; }
        .mobile-admin-bar { display:none; }
        @media(max-width:991.98px) {
            .stats-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }
            .split-view { flex-direction:column; }
        }
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
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="inventory" /></jsp:include>

<main class="admin-main-wrapper page-container">
    <c:if test="${not empty inventoryMessage}">
        <div class="alert alert-success alert-dismissible fade show mb-0" role="alert">
            <span class="material-symbols-outlined align-middle me-1">check_circle</span>
            <c:out value="${inventoryMessage}" />
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>
    <c:if test="${not empty inventoryError}">
        <div class="alert alert-danger alert-dismissible fade show mb-0" role="alert">
            <span class="material-symbols-outlined align-middle me-1">error</span>
            <c:out value="${inventoryError}" />
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <div class="overview-card">
        <div class="overview-heading">
            <div>
                <span class="table-ref">Bảng: ingredients, inventory_transactions</span>
                <h1 class="page-title">Tồn kho &amp; lịch sử xuất nhập</h1>
                <p class="page-subtitle">Tồn kho hiện tại và mọi biến động: nhập mua hàng, xuất theo đơn bán, điều chỉnh, hao hụt.</p>
            </div>
            <div class="d-flex align-items-center gap-2">
                <a href="${ctx}/admin/restock-requests" class="btn btn-outline-custom d-flex align-items-center gap-2">
                    <span class="material-symbols-outlined fs-5">notification_important</span>
                    <span>Đề xuất nhập hàng</span>
                </a>
                <button type="button" class="btn btn-aura d-flex align-items-center gap-2" data-bs-toggle="modal" data-bs-target="#adjustmentModal">
                    <span class="material-symbols-outlined fs-5">tune</span>
                    <span>Ghi phiếu điều chỉnh</span>
                </button>
            </div>
        </div>
        <div class="stats-grid">
            <div class="stat-item">
                <span>Giá trị tồn ước tính</span>
                <strong><fmt:formatNumber value="${estimatedValue}" pattern="#,##0" />đ</strong>
                <small>Theo giá nhập gần nhất</small>
            </div>
            <div class="stat-item">
                <span>Số nguyên liệu</span>
                <strong><c:out value="${totalIngredients}" default="0" /></strong>
            </div>
            <div class="stat-item ${underMinStockCount > 0 ? 'danger' : ''}">
                <span>Dưới mức tối thiểu</span>
                <strong><c:out value="${underMinStockCount}" default="0" /></strong>
            </div>
            <div class="stat-item">
                <span>Phiếu xuất nhập</span>
                <strong><c:out value="${totalTransactions}" default="0" /></strong>
            </div>
        </div>
    </div>

    <div class="row g-3 split-view">
        <!-- Cột trái: Tồn kho hiện tại -->
        <div class="col-lg-7">
            <div class="data-card p-3 h-100">
                <div class="section-header">
                    <h2 class="section-title">Tồn kho hiện tại</h2>
                    <form method="get" action="${ctx}/admin/inventory" id="statusFilterForm">
                        <input type="hidden" name="txType" value="${selectedTxType}">
                        <select name="status" class="form-select form-select-sm" style="width: 140px;" onchange="this.form.submit()">
                            <option value="all" ${selectedStatus eq 'all' ? 'selected' : ''}>Tất cả</option>
                            <option value="low_stock" ${selectedStatus eq 'low_stock' ? 'selected' : ''}>Sắp hết</option>
                            <option value="safe" ${selectedStatus eq 'safe' ? 'selected' : ''}>An toàn</option>
                            <option value="out_of_stock" ${selectedStatus eq 'out_of_stock' ? 'selected' : ''}>Hết hàng</option>
                        </select>
                    </form>
                </div>
                <div class="table-responsive">
                    <table class="table custom-table table-hover align-middle mb-0">
                        <thead>
                            <tr>
                                <th>Nguyên liệu</th>
                                <th class="text-end">Tồn</th>
                                <th class="text-end">Tối thiểu</th>
                                <th class="text-end">Giá trị</th>
                                <th class="text-center">Trạng thái</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${stockList}">
                                <tr>
                                    <td>
                                        <div class="fw-bold text-dark"><c:out value="${item.name}" /></div>
                                        <small class="text-muted"><c:out value="${item.supplierName}" default="Chưa gắn NCC" /></small>
                                    </td>
                                    <td class="text-end fw-bold text-success font-monospace">
                                        <fmt:formatNumber value="${item.currentStock}" pattern="#,##0.###" /> ${item.unit}
                                    </td>
                                    <td class="text-end text-muted font-monospace">
                                        <fmt:formatNumber value="${item.minStockLevel}" pattern="#,##0.###" />
                                    </td>
                                    <td class="text-end text-muted font-monospace">
                                        <c:choose>
                                            <c:when test="${not empty item.lastPurchasePrice and item.currentStock > 0}">
                                                <fmt:formatNumber value="${item.currentStock * item.lastPurchasePrice}" pattern="#,##0" />đ
                                            </c:when>
                                            <c:otherwise>—</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-center">
                                        <c:choose>
                                            <c:when test="${item.stockStatus eq 'Đủ hàng'}">
                                                <span class="badge-safe">An toàn</span>
                                            </c:when>
                                            <c:when test="${item.stockStatus eq 'Sắp hết'}">
                                                <span class="badge-warning">Cảnh báo</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge-danger">Hết hàng</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty stockList}">
                                <tr><td colspan="5" class="text-center py-4 text-muted">Không có dữ liệu tồn kho.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Cột phải: Lịch sử xuất nhập -->
        <div class="col-lg-5">
            <div class="data-card p-3 h-100">
                <div class="section-header">
                    <h2 class="section-title">Lịch sử xuất nhập</h2>
                    <form method="get" action="${ctx}/admin/inventory" id="txFilterForm">
                        <input type="hidden" name="status" value="${selectedStatus}">
                        <select name="txType" class="form-select form-select-sm" style="width: 150px;" onchange="this.form.submit()">
                            <option value="all" ${selectedTxType eq 'all' ? 'selected' : ''}>Tất cả loại</option>
                            <option value="IN_PURCHASE" ${selectedTxType eq 'IN_PURCHASE' ? 'selected' : ''}>Nhập mua hàng</option>
                            <option value="OUT_ORDER" ${selectedTxType eq 'OUT_ORDER' ? 'selected' : ''}>Xuất đơn bán</option>
                            <option value="ADJUSTMENT" ${selectedTxType eq 'ADJUSTMENT' ? 'selected' : ''}>Điều chỉnh kho</option>
                            <option value="SPOILAGE" ${selectedTxType eq 'SPOILAGE' ? 'selected' : ''}>Hao hụt / Hủy</option>
                        </select>
                    </form>
                </div>
                <div class="tx-feed">
                    <c:forEach var="tx" items="${transactions}">
                        <div class="tx-item">
                            <div>
                                <c:choose>
                                    <c:when test="${tx.transactionType eq 'IN_PURCHASE'}">
                                        <span class="tx-tag tx-tag-in">Nhập mua hàng</span>
                                    </c:when>
                                    <c:when test="${tx.transactionType eq 'OUT_ORDER'}">
                                        <span class="tx-tag tx-tag-out">Xuất theo đơn bán</span>
                                    </c:when>
                                    <c:when test="${tx.transactionType eq 'ADJUSTMENT'}">
                                        <span class="tx-tag tx-tag-adj">Điều chỉnh kho</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="tx-tag tx-tag-spoil">Hao hụt / Hủy</span>
                                    </c:otherwise>
                                </c:choose>
                                <span class="text-muted small">#${tx.id}</span>
                                <h4 class="tx-title"><c:out value="${tx.ingredientName}" /></h4>
                                <div class="tx-meta">
                                    <fmt:formatDate value="${tx.createdAt}" pattern="dd/MM/yyyy HH:mm" />
                                    · <c:out value="${tx.referenceType}" /> ${tx.referenceId != null ? tx.referenceId : ''}
                                </div>
                            </div>
                            <div>
                                <c:choose>
                                    <c:when test="${tx.transactionType eq 'OUT_ORDER' or tx.transactionType eq 'SPOILAGE'}">
                                        <span class="tx-qty-neg">${tx.formattedQuantity}</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="tx-qty-pos">${tx.formattedQuantity}</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </c:forEach>
                    <c:if test="${empty transactions}">
                        <div class="text-center py-4 text-muted">Chưa có giao dịch xuất nhập kho nào.</div>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
</main>

<!-- Modal Ghi Phiếu Điều Chỉnh / Cân Kho -->
<div class="modal fade" id="adjustmentModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <form method="post" action="${ctx}/admin/inventory">
                <input type="hidden" name="csrfToken" value="${sessionScope.inventoryCsrfToken}">
                <input type="hidden" name="action" value="adjust">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold">Ghi phiếu điều chỉnh tồn kho</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Nguyên liệu cần chỉnh <span class="text-danger">*</span></label>
                        <select name="ingredientId" class="form-select" required>
                            <option value="">-- Chọn nguyên liệu --</option>
                            <c:forEach var="ing" items="${allIngredients}">
                                <option value="${ing.id}">
                                    <c:out value="${ing.name}" /> (Hiện còn: <fmt:formatNumber value="${ing.currentStock}" pattern="#,##0.###" /> ${ing.unit})
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Loại điều chỉnh <span class="text-danger">*</span></label>
                        <select name="transactionType" id="adjustTxType" class="form-select" onchange="toggleDirectionField()">
                            <option value="ADJUSTMENT">Điều chỉnh kiểm kho (Cân kho định kỳ)</option>
                            <option value="SPOILAGE">Hao hụt / Đổ vỡ / Hỏng hết hạn</option>
                        </select>
                    </div>
                    <div class="mb-3" id="directionGroup">
                        <label class="form-label fw-bold small text-secondary">Hướng điều chỉnh</label>
                        <div class="d-flex gap-4">
                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="direction" id="dirIncrease" value="INCREASE" checked>
                                <label class="form-check-label" for="dirIncrease">Cộng thêm (+)</label>
                            </div>
                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="direction" id="dirDecrease" value="DECREASE">
                                <label class="form-check-label" for="dirDecrease">Trừ bớt (-)</label>
                            </div>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Số lượng thay đổi <span class="text-danger">*</span></label>
                        <input type="number" step="0.001" name="quantity" class="form-control" required placeholder="Nhập số lượng dương (ví dụ: 100, 50.5...)">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Lý do / Ghi chú</label>
                        <textarea name="reason" class="form-control" rows="2" placeholder="Ví dụ: Kiểm kho phát hiện dư, làm đổ bình syrup..."></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light border" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-aura">Xác nhận điều chỉnh</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    function toggleDirectionField() {
        const type = document.getElementById('adjustTxType').value;
        const dirGroup = document.getElementById('directionGroup');
        if (type === 'SPOILAGE') {
            dirGroup.style.display = 'none';
        } else {
            dirGroup.style.display = 'block';
        }
    }
</script>
</body>
</html>
