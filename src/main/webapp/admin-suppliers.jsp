<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty sessionScope.user or sessionScope.user.role ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Aura Café | Quản lý nhà cung cấp</title>
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
        .overview-card { padding:20px; }
        .overview-heading { display:flex; align-items:flex-start; justify-content:space-between; gap:18px; }
        .table-ref { display:inline-block; padding:3px 8px; border-radius:6px; background:#f5f5f4; color:#78716c; font:700 10px/1.5 ui-monospace,monospace; }
        .page-title { margin:5px 0 2px; color:#1c1917; font-size:1.45rem; font-weight:800; letter-spacing:-.025em; }
        .page-subtitle { margin:0; color:#78716c; font-size:.82rem; }
        .stats-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:10px; padding-top:14px; margin-top:16px; border-top:1px solid #f5f5f4; }
        .stat-item { padding:10px 14px; border:1px solid #e7e5e4; border-radius:12px; background:#fafaf9; }
        .stat-item span { display:block; color:#78716c; font-size:10px; text-transform:uppercase; font-weight:800; }
        .stat-item strong { display:block; margin-top:2px; color:#1c1917; font:800 1.25rem ui-monospace,monospace; }
        .stat-item.active { background:#ecfdf5; border-color:#a7f3d0; }
        .stat-item.active strong { color:#047857; }
        .btn-aura { color:#fff; background:#900b1a; border-color:#900b1a; font-weight:600; font-size:.85rem; border-radius:10px; padding:.5rem 1rem; }
        .btn-aura:hover { color:#fff; background:#7a121d; border-color:#7a121d; }
        .custom-table > :not(caption) > * > * { padding:.75rem .8rem; vertical-align:middle; font-size:.85rem; }
        .custom-table thead th { color:#78716c; font-size:11px; font-weight:800; letter-spacing:.04em; text-transform:uppercase; background:#fafaf9; }
        .badge-active { display:inline-flex; align-items:center; gap:5px; background:#ecfdf5; color:#047857; border:1px solid #a7f3d0; border-radius:999px; padding:3px 9px; font-size:11px; font-weight:700; }
        .badge-active::before { content:''; width:6px; height:6px; border-radius:50%; background:#10b981; }
        .badge-inactive { display:inline-flex; align-items:center; gap:5px; background:#f5f5f4; color:#78716c; border:1px solid #e7e5e4; border-radius:999px; padding:3px 9px; font-size:11px; font-weight:700; }
        .badge-inactive::before { content:''; width:6px; height:6px; border-radius:50%; background:#a8a29e; }
        .mobile-admin-bar { display:none; }
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
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="suppliers" /></jsp:include>

<main class="admin-main-wrapper page-container">
    <c:if test="${not empty supplierMessage}">
        <div class="alert alert-success alert-dismissible fade show mb-0" role="alert">
            <span class="material-symbols-outlined align-middle me-1">check_circle</span>
            <c:out value="${supplierMessage}" />
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>
    <c:if test="${not empty supplierError}">
        <div class="alert alert-danger alert-dismissible fade show mb-0" role="alert">
            <span class="material-symbols-outlined align-middle me-1">error</span>
            <c:out value="${supplierError}" />
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <div class="overview-card">
        <div class="overview-heading">
            <div>
                <span class="table-ref">Bảng: suppliers</span>
                <h1 class="page-title">Quản lý nhà cung cấp</h1>
                <p class="page-subtitle">Danh sách các đối tác cung cấp nguyên vật liệu cho chuỗi cửa hàng.</p>
            </div>
            <button type="button" class="btn btn-aura d-flex align-items-center gap-2" data-bs-toggle="modal" data-bs-target="#supplierModal" onclick="prepareCreateSupplier()">
                <span class="material-symbols-outlined fs-5">add_business</span>
                <span>Thêm nhà cung cấp</span>
            </button>
        </div>
        <div class="stats-grid">
            <div class="stat-item">
                <span>Tổng nhà cung cấp</span>
                <strong><c:out value="${totalSuppliers}" default="0" /></strong>
            </div>
            <div class="stat-item active">
                <span>Đang hợp tác (Active)</span>
                <strong><c:out value="${activeSuppliers}" default="0" /></strong>
            </div>
            <div class="stat-item">
                <span>Ngừng hợp tác (Inactive)</span>
                <strong><c:out value="${inactiveSuppliers}" default="0" /></strong>
            </div>
        </div>
    </div>

    <div class="data-card p-3">
        <div class="table-responsive">
            <table class="table custom-table table-hover align-middle mb-0">
                <thead>
                    <tr>
                        <th style="width: 60px;">ID</th>
                        <th>Tên nhà cung cấp</th>
                        <th>Số điện thoại</th>
                        <th>Địa chỉ</th>
                        <th class="text-center">Số nguyên liệu cấp</th>
                        <th class="text-center">Trạng thái</th>
                        <th class="text-end" style="width: 120px;">Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="s" items="${suppliers}">
                        <tr>
                            <td class="fw-bold text-muted">#<c:out value="${s.id}" /></td>
                            <td><strong class="text-dark"><c:out value="${s.name}" /></strong></td>
                            <td><c:out value="${s.phone}" default="-" /></td>
                            <td class="text-muted"><c:out value="${s.address}" default="-" /></td>
                            <td class="text-center">
                                <span class="badge bg-light text-dark border px-2 py-1"><c:out value="${s.ingredientCount}" /> loại</span>
                            </td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${s.status eq 'ACTIVE'}">
                                        <span class="badge-active">Hoạt động</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge-inactive">Tạm ngừng</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-end">
                                <button type="button" class="btn btn-sm btn-light border me-1" title="Chỉnh sửa"
                                        onclick="prepareEditSupplier(${s.id}, '<c:out value="${s.name}" />', '<c:out value="${s.phone}" />', '<c:out value="${s.address}" />', '<c:out value="${s.status}" />')">
                                    <span class="material-symbols-outlined fs-6 align-middle text-secondary">edit</span>
                                </button>
                                <form method="post" action="${ctx}/admin/suppliers" class="d-inline" onsubmit="return confirm('Bạn có chắc muốn đổi trạng thái nhà cung cấp này?');">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.supplierCsrfToken}">
                                    <input type="hidden" name="action" value="toggle">
                                    <input type="hidden" name="id" value="${s.id}">
                                    <input type="hidden" name="currentStatus" value="${s.status}">
                                    <button type="submit" class="btn btn-sm btn-light border" title="${s.status eq 'ACTIVE' ? 'Tắt hoạt động' : 'Kích hoạt lại'}">
                                        <span class="material-symbols-outlined fs-6 align-middle ${s.status eq 'ACTIVE' ? 'text-warning' : 'text-success'}">
                                            ${s.status eq 'ACTIVE' ? 'block' : 'check_circle'}
                                        </span>
                                    </button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty suppliers}">
                        <tr>
                            <td colspan="7" class="text-center py-4 text-muted">Chưa có nhà cung cấp nào. Nhấn "+ Thêm nhà cung cấp" để bắt đầu.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</main>

<!-- Modal Thêm / Sửa Nhà Cung Cấp -->
<div class="modal fade" id="supplierModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <form method="post" action="${ctx}/admin/suppliers" id="supplierForm">
                <input type="hidden" name="csrfToken" value="${sessionScope.supplierCsrfToken}">
                <input type="hidden" name="action" id="modalAction" value="create">
                <input type="hidden" name="id" id="modalSupplierId" value="0">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold" id="modalSupplierTitle">Thêm nhà cung cấp</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Tên nhà cung cấp <span class="text-danger">*</span></label>
                        <input type="text" name="name" id="modalSupplierName" class="form-control" required placeholder="Ví dụ: Công ty Cổ phần Sữa Ba Vì">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Số điện thoại</label>
                        <input type="text" name="phone" id="modalSupplierPhone" class="form-control" placeholder="Ví dụ: 0912345678">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Địa chỉ</label>
                        <textarea name="address" id="modalSupplierAddress" class="form-control" rows="2" placeholder="Địa chỉ chi tiết kho/văn phòng"></textarea>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-secondary">Trạng thái</label>
                        <select name="status" id="modalSupplierStatus" class="form-select">
                            <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                            <option value="INACTIVE">Tạm ngừng (INACTIVE)</option>
                        </select>
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
    function prepareCreateSupplier() {
        document.getElementById('supplierForm').reset();
        document.getElementById('modalAction').value = 'create';
        document.getElementById('modalSupplierId').value = '0';
        document.getElementById('modalSupplierTitle').innerText = 'Thêm nhà cung cấp mới';
    }

    function prepareEditSupplier(id, name, phone, address, status) {
        document.getElementById('modalAction').value = 'update';
        document.getElementById('modalSupplierId').value = id;
        document.getElementById('modalSupplierTitle').innerText = 'Chỉnh sửa nhà cung cấp';
        document.getElementById('modalSupplierName').value = name;
        document.getElementById('modalSupplierPhone').value = phone || '';
        document.getElementById('modalSupplierAddress').value = address || '';
        document.getElementById('modalSupplierStatus').value = status || 'ACTIVE';
        new bootstrap.Modal(document.getElementById('supplierModal')).show();
    }
</script>
</body>
</html>
