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
    <title>Aura Café | Quản lý nguyên vật liệu</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <style>
        .admin-main-wrapper { max-width:1600px; }
        .page-container { display:flex; flex-direction:column; gap:18px; }
        .overview-card, .data-card, .search-card { background:#fff; border:1px solid #e7e5e4; border-radius:16px; box-shadow:0 1px 2px rgba(28,25,23,.04); }
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
        .btn-aura:hover { color:#fff; background:#7a121d; border-color:#7a121d; transform:translateY(-1px); box-shadow:0 4px 10px rgba(144,11,26,.15); }
        .search-card { padding:14px 18px; }
        .search-input-group { position:relative; }
        .search-input-group .material-symbols-outlined { position:absolute; left:12px; top:50%; transform:translateY(-50%); color:#a8a29e; font-size:20px; }
        .search-input-group input { padding-left:40px; border-radius:10px; border-color:#e7e5e4; font-size:.86rem; background:#fafaf9; }
        .search-input-group input:focus { border-color:#900b1a; background:#fff; box-shadow:0 0 0 3px rgba(144,11,26,.08); }
        .custom-table > :not(caption) > * > * { padding:.8rem .75rem; vertical-align:middle; font-size:.86rem; }
        .custom-table thead th { color:#78716c; font-size:11px; font-weight:800; letter-spacing:.04em; text-transform:uppercase; background:#fafaf9; }
        .badge-safe { background:#ecfdf5; color:#047857; border:1px solid #a7f3d0; border-radius:999px; padding:3px 8px; font-size:11px; font-weight:700; }
        .badge-warning { background:#fffbeb; color:#b45309; border:1px solid #fde68a; border-radius:999px; padding:3px 8px; font-size:11px; font-weight:700; }
        .badge-danger { background:#fff1f2; color:#be123c; border:1px solid #fecdd3; border-radius:999px; padding:3px 8px; font-size:11px; font-weight:700; }
        .mobile-admin-bar { display:none; }
        @media(max-width:991.98px) { .stats-grid { grid-template-columns:repeat(2,minmax(0,1fr)); } }
        @media(max-width:767.98px) {
            .mobile-admin-bar { display:flex; align-items:center; justify-content:space-between; padding:10px 15px; background:#fff; border-bottom:1px solid #e7e5e4; }
            .stats-grid { grid-template-columns:1fr; }
        }
    </style>
</head>
<body class="admin-body">
<div class="mobile-admin-bar">
    <a href="${ctx}/admin-dashboard.jsp" class="text-decoration-none fw-bold text-danger"><span class="material-symbols-outlined align-middle">local_cafe</span> AURA CAFÉ · ADMIN</a>
    <a href="${ctx}/login?action=logout" class="btn btn-sm btn-light">Đăng xuất</a>
</div>
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="ingredients" /></jsp:include>

<main class="admin-main-wrapper page-container">
    <c:if test="${not empty ingredientMessage}">
        <div class="alert alert-success alert-dismissible fade show mb-0" role="alert">
            <span class="material-symbols-outlined align-middle me-1">check_circle</span>
            <c:out value="${ingredientMessage}" />
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>
    <c:if test="${not empty ingredientError}">
        <div class="alert alert-danger alert-dismissible fade show mb-0" role="alert">
            <span class="material-symbols-outlined align-middle me-1">error</span>
            <c:out value="${ingredientError}" />
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <div class="overview-card">
        <div class="overview-heading">
            <div>
                <span class="table-ref">Bảng: ingredients</span>
                <h1 class="page-title">Quản lý nguyên vật liệu</h1>
                <p class="page-subtitle">Danh mục nguyên liệu, đơn vị tính, nhà cung cấp và ngưỡng tồn kho tối thiểu.</p>
            </div>
            <button type="button" class="btn btn-aura d-flex align-items-center gap-2" data-bs-toggle="modal" data-bs-target="#ingredientModal" onclick="prepareCreateIngredient()">
                <span class="material-symbols-outlined fs-5">add_circle</span>
                <span>Thêm nguyên liệu</span>
            </button>
        </div>
        <div class="stats-grid">
            <div class="stat-item">
                <span>Tổng nguyên liệu</span>
                <strong><c:out value="${totalIngredients}" default="0" /></strong>
            </div>
            <div class="stat-item">
                <span>Nhà cung cấp</span>
                <strong><c:out value="${totalSuppliers}" default="0" /></strong>
            </div>
            <div class="stat-item ${underMinStockCount > 0 ? 'danger' : ''}">
                <span>Dưới mức tối thiểu</span>
                <strong><c:out value="${underMinStockCount}" default="0" /></strong>
            </div>
            <div class="stat-item">
                <span>Chưa dùng trong công thức</span>
                <strong><c:out value="${unusedRecipesCount}" default="0" /></strong>
            </div>
        </div>
    </div>

    <div class="search-card">
        <form method="get" action="${ctx}/admin/ingredients" class="row g-2 align-items-center">
            <div class="col-md-8 search-input-group">
                <span class="material-symbols-outlined">search</span>
                <input type="text" name="q" class="form-control" placeholder="Tìm nguyên liệu theo tên hoặc nhà cung cấp..." value="<c:out value="${searchKeyword}" />">
            </div>
            <div class="col-md-3">
                <select name="supplierId" class="form-select" onchange="this.form.submit()">
                    <option value="0">Tất cả nhà cung cấp</option>
                    <c:forEach var="sup" items="${suppliers}">
                        <option value="${sup.id}" ${selectedSupplierId eq sup.id ? 'selected' : ''}>
                            <c:out value="${sup.name}" />
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-1">
                <button type="submit" class="btn btn-light border w-100">Lọc</button>
            </div>
        </form>
    </div>

    <div class="data-card p-3">
        <div class="table-responsive">
            <table class="table custom-table table-hover align-middle mb-0">
                <thead>
                    <tr>
                        <th style="width: 55px;">ID</th>
                        <th>Tên nguyên liệu</th>
                        <th>ĐVT</th>
                        <th>Nhà cung cấp</th>
                        <th class="text-end">Tồn hiện tại</th>
                        <th class="text-end">Tối thiểu</th>
                        <th class="text-end">Giá nhập gần nhất</th>
                        <th class="text-center">Dùng trong</th>
                        <th class="text-center">Trạng thái</th>
                        <th class="text-end" style="width: 100px;">Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="item" items="${ingredients}">
                        <tr>
                            <td class="text-danger fw-bold">#<c:out value="${item.id}" /></td>
                            <td><strong class="text-dark"><c:out value="${item.name}" /></strong></td>
                            <td class="text-muted"><c:out value="${item.unit}" /></td>
                            <td><c:out value="${item.supplierName}" default="—" /></td>
                            <td class="text-end fw-bold">
                                <fmt:formatNumber value="${item.currentStock}" pattern="#,##0.###" />
                            </td>
                            <td class="text-end text-muted">
                                <fmt:formatNumber value="${item.minStockLevel}" pattern="#,##0.###" />
                            </td>
                            <td class="text-end text-muted">
                                <c:choose>
                                    <c:when test="${not empty item.lastPurchasePrice}">
                                        <fmt:formatNumber value="${item.lastPurchasePrice}" pattern="#,##0" />đ/${item.unit}
                                    </c:when>
                                    <c:otherwise>—</c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-center">
                                <span class="badge bg-light text-dark border px-2 py-1"><c:out value="${item.usedInRecipesCount}" /> món</span>
                            </td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${item.stockStatus eq 'Đủ hàng'}">
                                        <span class="badge-safe">Đủ hàng</span>
                                    </c:when>
                                    <c:when test="${item.stockStatus eq 'Sắp hết'}">
                                        <span class="badge-warning">Sắp hết</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge-danger">Hết hàng</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-end">
                                <button type="button" class="btn btn-sm btn-light border me-1" title="Chỉnh sửa"
                                        onclick="prepareEditIngredient(${item.id}, '<c:out value="${item.name}" />', '<c:out value="${item.unit}" />', ${item.supplierId != null ? item.supplierId : 0}, ${item.minStockLevel})">
                                    <span class="material-symbols-outlined fs-6 align-middle text-secondary">edit</span>
                                </button>
                                <form method="post" action="${ctx}/admin/ingredients" class="d-inline" onsubmit="return confirm('Bạn có chắc muốn xóa nguyên liệu này?');">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.ingredientCsrfToken}">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="id" value="${item.id}">
                                    <button type="submit" class="btn btn-sm btn-light border text-danger" title="Xóa">
                                        <span class="material-symbols-outlined fs-6 align-middle">delete</span>
                                    </button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty ingredients}">
                        <tr>
                            <td colspan="10" class="text-center py-4 text-muted">Không tìm thấy nguyên vật liệu nào.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</main>

<!-- Modal Thêm / Sửa Nguyên Liệu -->
<div class="modal fade" id="ingredientModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <form method="post" action="${ctx}/admin/ingredients" id="ingredientForm">
                <input type="hidden" name="csrfToken" value="${sessionScope.ingredientCsrfToken}">
                <input type="hidden" name="action" id="modalAction" value="create">
                <input type="hidden" name="id" id="modalIngredientId" value="0">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold" id="modalIngredientTitle">Thêm nguyên vật liệu</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Tên nguyên liệu <span class="text-danger">*</span></label>
                        <input type="text" name="name" id="modalIngredientName" class="form-control" required placeholder="Ví dụ: Coffee beans, Fresh milk...">
                    </div>
                    <div class="row g-2 mb-3">
                        <div class="col-6">
                            <label class="form-label fw-bold small text-secondary">Đơn vị tính (ĐVT) <span class="text-danger">*</span></label>
                            <input type="text" name="unit" id="modalIngredientUnit" class="form-control" required placeholder="gram, ml, piece, lon...">
                        </div>
                        <div class="col-6">
                            <label class="form-label fw-bold small text-secondary">Ngưỡng tối thiểu</label>
                            <input type="number" step="0.001" name="minStockLevel" id="modalIngredientMinStock" class="form-control" value="0" placeholder="0">
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Nhà cung cấp</label>
                        <select name="supplierId" id="modalIngredientSupplier" class="form-select">
                            <option value="0">-- Chọn nhà cung cấp --</option>
                            <c:forEach var="sup" items="${suppliers}">
                                <option value="${sup.id}"><c:out value="${sup.name}" /></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-3" id="initialStockGroup">
                        <label class="form-label fw-bold small text-secondary">Tồn kho ban đầu</label>
                        <input type="number" step="0.001" name="currentStock" id="modalIngredientStock" class="form-control" value="0">
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light border" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-aura">Lưu thông tin</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    function prepareCreateIngredient() {
        document.getElementById('ingredientForm').reset();
        document.getElementById('modalAction').value = 'create';
        document.getElementById('modalIngredientId').value = '0';
        document.getElementById('modalIngredientTitle').innerText = 'Thêm nguyên vật liệu mới';
        document.getElementById('initialStockGroup').style.display = 'block';
    }

    function prepareEditIngredient(id, name, unit, supplierId, minStock) {
        document.getElementById('modalAction').value = 'update';
        document.getElementById('modalIngredientId').value = id;
        document.getElementById('modalIngredientTitle').innerText = 'Chỉnh sửa nguyên vật liệu';
        document.getElementById('modalIngredientName').value = name;
        document.getElementById('modalIngredientUnit').value = unit;
        document.getElementById('modalIngredientSupplier').value = supplierId || '0';
        document.getElementById('modalIngredientMinStock').value = minStock || '0';
        document.getElementById('initialStockGroup').style.display = 'none';
        new bootstrap.Modal(document.getElementById('ingredientModal')).show();
    }
</script>
</body>
</html>
