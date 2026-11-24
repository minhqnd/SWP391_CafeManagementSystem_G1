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
    <title>Aura Café | Quản lý danh mục</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <style>
        .admin-main-wrapper { max-width:1600px; }
        .categories-page { display:flex; flex-direction:column; gap:18px; }
        .categories-overview, .category-card { background:#fff; border:1px solid #e7e5e4; border-radius:16px; box-shadow:0 1px 2px rgba(28,25,23,.04); }
        .categories-overview { padding:20px; }
        .categories-heading { display:flex; align-items:flex-start; justify-content:space-between; gap:18px; }
        .table-ref { display:inline-block; padding:3px 8px; border-radius:6px; background:#f5f5f4; color:#78716c; font:700 10px/1.5 ui-monospace,monospace; }
        .page-title { margin:5px 0 2px; color:#1c1917; font-size:1.45rem; font-weight:800; letter-spacing:-.025em; }
        .page-subtitle { margin:0; color:#78716c; font-size:.82rem; }
        .category-stats { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:10px; padding-top:14px; margin-top:16px; border-top:1px solid #f5f5f4; }
        .category-stat { padding:10px 12px; border:1px solid #e7e5e4; border-radius:12px; background:#fafaf9; }
        .category-stat span { display:block; color:#78716c; font-size:10px; text-transform:uppercase; font-weight:800; }
        .category-stat strong { display:block; margin-top:2px; color:#1c1917; font:800 1.25rem ui-monospace,monospace; }
        .category-stat.active { background:#ecfdf5; border-color:#a7f3d0; }
        .category-stat.active strong { color:#047857; }
        .category-stat.inactive strong { color:#78716c; }
        .category-panel-title { padding:14px 16px; margin:0; border-bottom:1px solid #f5f5f4; font-size:.88rem; font-weight:800; }
        .category-editor { padding:17px; }
        .category-editor .form-label { margin-bottom:5px; color:#57534e; font-size:.76rem; font-weight:700; }
        .category-editor .form-control, .category-editor .form-select { border-color:#e7e5e4; border-radius:10px; background:#fafaf9; font-size:.83rem; }
        .category-editor .form-control:focus, .category-editor .form-select:focus { border-color:#900b1a; box-shadow:0 0 0 .18rem rgba(144,11,26,.1); }
        .category-form-field { margin-bottom:13px; }
        .category-image, .category-image-placeholder { width:48px; height:42px; }
        .category-image { object-fit:cover; border-radius:8px; background:#f5f5f4; }
        .category-image-placeholder { display:flex; align-items:center; justify-content:center; border:1px dashed #d6d3d1; border-radius:8px; color:#78716c; background:#fafaf9; font-size:9px; text-align:center; }
        .category-image-preview { width:100%; max-width:240px; height:120px; object-fit:cover; border:1px solid #e7e5e4; border-radius:10px; background:#fafaf9; }
        .category-table > :not(caption) > * > * { padding:.7rem .6rem; vertical-align:middle; }
        .category-table thead th { color:#78716c; font-size:10px; font-weight:800; letter-spacing:.04em; white-space:nowrap; }
        .category-table tbody tr:hover { background:#fafaf9; }
        .btn-aura { color:#fff; background:#900b1a; border-color:#900b1a; }
        .btn-aura:hover { color:#fff; background:#7a121d; border-color:#7a121d; }
        .mobile-admin-bar { display:none; }
        @media(max-width:767.98px) {
            .mobile-admin-bar { display:flex; align-items:center; justify-content:space-between; padding:10px 15px; background:#fff; border-bottom:1px solid #e7e5e4; }
            .categories-overview { padding:15px; }
            .category-stats { grid-template-columns:repeat(2,minmax(0,1fr)); }
            .page-title { font-size:1.2rem; }
            .categories-heading { gap:10px; }
        }
    </style>
</head>
<body class="admin-body">
<div class="mobile-admin-bar">

    <a href="${ctx}/admin-dashboard.jsp" class="text-decoration-none fw-bold text-danger"><span class="material-symbols-outlined align-middle">local_cafe</span> AURA CAFÉ · ADMIN</a>
    <%-- Nút Đăng xuất dùng chung xử lý tài khoản. Tra tiếp: components/admin-sidebar.jsp / DNX03. --%>
    <a href="${ctx}/login?action=logout" class="btn btn-sm btn-light">Đăng xuất</a>
</div>
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="categories" /></jsp:include>

<main class="admin-main-wrapper categories-page">
    <header class="categories-overview">
        <div class="categories-heading">
            <div>
                <span class="table-ref">Bảng: categories</span>
                <h1 class="page-title">Quản lý danh mục thực đơn</h1>
                <p class="page-subtitle">Danh mục hiển thị trên menu khách hàng, POS và KDS.</p>
            </div>
        </div>
        <%-- [DMM04]
             . Ở đầu trang quản lý danh mục, em hiển thị tổng danh mục, số đang hoạt động, số tạm ẩn và
               tổng sản phẩm. Bảng bên dưới hiển thị tên nhóm, số món và các nút thao tác; mỗi hàng gắn
               với đúng danh mục để Admin chọn nhóm cần xử lý.

             Kết thúc: mở danh sách danh mục.
        --%>
        <div class="category-stats" aria-label="Thống kê danh mục">
            <div class="category-stat"><span>Tổng danh mục</span><strong><c:out value="${categoryTotal}" /></strong></div>
            <div class="category-stat active"><span>Đang hoạt động</span><strong><c:out value="${categoryActive}" /></strong></div>
            <div class="category-stat inactive"><span>Tạm ẩn</span><strong><c:out value="${categoryInactive}" /></strong></div>
            <div class="category-stat"><span>Tổng sản phẩm</span><strong><c:out value="${productTotal}" /></strong></div>
        </div>
    </header>

    <%-- [DML07]
         . Khi trang quản lý danh mục mở lại, em hiển thị kết quả thêm, sửa, đổi trạng thái hoặc xóa danh
           mục. Nếu lưu chưa được, form giữ tên và trạng thái đang nhập để Admin sửa tiếp; ảnh mới cần
           chọn lại.

         Kết thúc: thao tác tại thông báo và danh sách danh mục hiện tại.
    --%>
    <c:if test="${not empty categoryMessage and empty categoryError}"><div data-form-notice class="alert alert-success py-2 mb-0" role="status"><c:out value="${categoryMessage}" /></div></c:if>
    <c:if test="${not empty categoryError}"><div data-form-notice class="alert alert-danger py-2 mb-0" role="alert"><c:out value="${categoryError}" /></div></c:if>

    <div class="row g-4 align-items-start">
        <section class="col-12 col-xl-8">
            <div class="category-card overflow-hidden">
                <h2 class="category-panel-title">Danh sách danh mục</h2>
                <div class="table-responsive">
                    <table class="table table-hover category-table mb-0">
                        <thead class="table-light text-uppercase">
                            <tr><th>ID</th><th>Ảnh</th><th>Tên danh mục</th><th class="text-end">Số món</th><th>Trạng thái</th><th class="text-end">Thao tác</th></tr>
                        </thead>
                        <tbody>

                        <c:forEach var="category" items="${categories}">
                            <tr>
                                <td class="font-monospace text-muted">#<c:out value="${category.id}" /></td>
                                <td><c:choose><c:when test="${category.storedImage}"><img class="category-image" src="${ctx}/category-image?id=${category.id}" alt="Ảnh danh mục"></c:when><c:when test="${not empty category.imageUrl}"><img class="category-image" src="${fn:escapeXml(category.imageUrl)}" alt="Ảnh danh mục"></c:when><c:otherwise><div class="category-image-placeholder">Chưa có ảnh</div></c:otherwise></c:choose></td>
                                <td class="fw-bold text-dark"><c:out value="${category.name}" /></td>

                                <td class="text-end fw-bold font-monospace"><c:out value="${category.productCount}" /></td>
                                <td><c:choose><c:when test="${category.status eq 'ACTIVE'}"><span class="badge rounded-pill text-bg-success">Hoạt động</span></c:when><c:otherwise><span class="badge rounded-pill text-bg-secondary">Tạm ẩn</span></c:otherwise></c:choose></td>
                                <td class="text-end text-nowrap">

                                    <%-- [DMS01]
                                         . Khi bấm Sửa trên một hàng, em gửi mã danh mục của hàng đó tới
                                           doGet của AdminCategoriesController. Controller đọc thông tin
                                           hiện tại và điền vào form, để Admin sửa đúng nhóm vừa chọn.

                                         Tra tiếp: AdminCategoriesController.java / DMS02.
                                    --%>
                                    <a class="btn btn-sm btn-outline-primary" href="${ctx}/admin/categories?editId=${category.id}" title="Sửa"><span class="material-symbols-outlined fs-6 align-middle">edit</span></a>

                                    <form method="post" action="${ctx}/admin/categories" class="d-inline">
                                        <%-- [DMAH01]
                                             . Khi bấm Ẩn hoặc Hiện, form gửi mã danh mục cùng trạng thái
                                               cần đổi tới AdminCategoriesController. Trong doPost, em gọi
                                               toggle để kiểm tra và cập nhật đúng danh mục đang chọn.

                                             Tra tiếp: AdminCategoriesController.java / DMAH02.
                                        --%>
                                        <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.categoryCsrfToken)}"><input type="hidden" name="action" value="toggle"><input type="hidden" name="id" value="${category.id}">
                                        <c:choose><c:when test="${category.status eq 'ACTIVE'}"><input type="hidden" name="status" value="INACTIVE"><button class="btn btn-sm btn-outline-secondary" title="Tạm ẩn"><span class="material-symbols-outlined fs-6 align-middle">visibility_off</span></button></c:when><c:otherwise><input type="hidden" name="status" value="ACTIVE"><button class="btn btn-sm btn-outline-success" title="Hiện lại"><span class="material-symbols-outlined fs-6 align-middle">visibility</span></button></c:otherwise></c:choose>
                                    </form>

                                    <form method="post" action="${ctx}/admin/categories" class="d-inline"
                                          onsubmit="return confirm('Xóa vĩnh viễn danh mục này và tất cả sản phẩm thuộc danh mục? Nếu có sản phẩm đã bán, hệ thống sẽ từ chối để giữ lịch sử đơn hàng.');">
                                        <%-- [DMX01]
                                             . Khi bấm Xóa, em hỏi xác nhận trước khi gửi mã danh mục tới
                                               AdminCategoriesController. Hàm doPost gọi delete để kiểm
                                               tra lịch sử đơn hàng trước khi cho xóa nhóm cùng các sản
                                               phẩm thuộc nhóm.

                                             Tra tiếp: AdminCategoriesController.java / DMX02.
                                        --%>
                                        <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.categoryCsrfToken)}"><input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${category.id}">
                                        <button class="btn btn-sm btn-outline-danger" title="Xóa danh mục"><span class="material-symbols-outlined fs-6 align-middle">delete_forever</span></button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty categories}"><tr><td colspan="6" class="text-center text-muted py-5">Chưa có danh mục nào trong database.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </section>

        <section class="col-12 col-xl-4">
            <div class="category-card category-editor">
                <div class="d-flex justify-content-between align-items-center border-bottom pb-3 mb-3">
                    <h2 class="h6 fw-bold mb-0"><c:choose><c:when test="${selectedCategory.id gt 0}">Sửa danh mục</c:when><c:otherwise>Thêm danh mục mới</c:otherwise></c:choose></h2>
                    <c:if test="${selectedCategory.id gt 0}"><span class="badge text-bg-light font-monospace">#<c:out value="${selectedCategory.id}" /></span></c:if>
                </div>

                <%-- [DML01]
                     . Ở form danh mục, em đặt tên, ảnh, lựa chọn xóa ảnh và trạng thái cùng nút Tạo hoặc
                       Lưu. Có mã danh mục thì form dùng để sửa, chưa có mã thì dùng để thêm mới. Khi
                       Admin bấm nút, phần kiểm tra trong admin-form-validation.js sẽ kiểm tra các ô và
                       ảnh trước khi gửi form.

                     Tra tiếp: admin-form-validation.js / DML02.
                --%>
                <form data-admin-form="category" method="post" action="${ctx}/admin/categories" enctype="multipart/form-data" novalidate>
                    <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.categoryCsrfToken)}"><input type="hidden" name="action" value="save">
                    <input type="hidden" name="id" value="${empty selectedCategory ? 0 : selectedCategory.id}">

                    <div class="category-form-field"><label for="name" class="form-label">Tên danh mục *</label><input id="name" name="name" class="form-control" required maxlength="100" value="${fn:escapeXml(selectedCategory.name)}"></div>
                    <div class="category-form-field">
                        <label for="imageFile" class="form-label">Ảnh đại diện</label>

                        <input id="imageFile" name="imageFile" type="file" accept="image/jpeg,image/png,image/gif,image/webp" class="form-control">
                        <div class="form-text">JPG, PNG, GIF hoặc WebP; tối đa 5 MB. Ảnh được lưu trực tiếp trong database.</div>
                    </div>
                    <div class="mt-2">
                        <c:set var="selectedHasImage" value="${not empty selectedCategory and (selectedCategory.storedImage or not empty selectedCategory.imageUrl)}" />
                        <c:choose><c:when test="${selectedCategory.storedImage}"><c:set var="selectedImageSrc" value="${ctx}/category-image?id=${selectedCategory.id}" /></c:when><c:otherwise><c:set var="selectedImageSrc" value="${selectedCategory.imageUrl}" /></c:otherwise></c:choose>
                        <img id="categoryImagePreview" class="category-image-preview" src="${fn:escapeXml(selectedImageSrc)}" alt="Ảnh danh mục preview" <c:if test="${not selectedHasImage}">hidden</c:if>>
                        <div id="categoryImagePlaceholder" class="category-image-placeholder" style="width:100%;max-width:240px;height:120px" <c:if test="${selectedHasImage}">hidden</c:if>>Xem trước ảnh</div>
                    </div>

                    <c:if test="${selectedHasImage}"><div class="form-check mt-2 mb-3"><input id="removeImage" name="removeImage" value="true" type="checkbox" class="form-check-input"><label for="removeImage" class="form-check-label small">Xóa ảnh hiện tại</label></div></c:if>

                    <div class="category-form-field"><label for="status" class="form-label">Trạng thái</label><select id="status" name="status" class="form-select">
                        <option value="ACTIVE" <c:if test="${empty selectedCategory or selectedCategory.status eq 'ACTIVE'}">selected</c:if>>ACTIVE - Hoạt động</option>
                        <option value="INACTIVE" <c:if test="${selectedCategory.status eq 'INACTIVE'}">selected</c:if>>INACTIVE - Tạm ẩn</option>
                    </select></div>

                    <button type="submit" class="btn btn-aura fw-bold w-100"><span class="material-symbols-outlined fs-6 align-middle">check</span> <c:choose><c:when test="${selectedCategory.id gt 0}">Lưu thay đổi</c:when><c:otherwise>Tạo danh mục</c:otherwise></c:choose></button>
                    <%-- [DMH01]
                         . Khi Admin bấm Hủy sửa danh mục, em mở lại trang quản lý mà không kèm mã danh
                           mục đang sửa. Form trở về chế độ thêm mới và phần vừa nhập chưa được lưu vào dữ
                           liệu.

                         Tra cứu: Mở lại tại AdminCategoriesController.java / DMM02, form DML01. Kết thúc Hủy
                                  sửa.
                    --%>
                    <c:if test="${not empty selectedCategory}"><a href="${ctx}/admin/categories" class="btn btn-light w-100 mt-2">Hủy sửa</a></c:if>
                </form>
            </div>
        </section>
    </div>
</main>

<script src="${ctx}/assets/js/admin-form-validation.js"></script>
<script>
(() => {
    const input = document.getElementById('imageFile');
    const preview = document.getElementById('categoryImagePreview');
    const placeholder = document.getElementById('categoryImagePlaceholder');
    const remove = document.getElementById('removeImage');
    if (!input || !preview || !placeholder) return;
    // [DMAN01]
    // . Khi Admin chọn ảnh danh mục, em hiển thị ảnh vừa chọn để xem trước khi lưu. Nếu đánh dấu xóa ảnh,
    //   phần xem trước được đổi theo lựa chọn đó. Ảnh đang lưu chỉ thay đổi sau khi Admin bấm Lưu và form
    //   được xử lý thành công.
    //
    // Kết thúc: xem trước ảnh trên form; bấm Lưu mới ghi ảnh.
    const showPlaceholder = () => { preview.hidden = true; placeholder.hidden = false; };
    preview.addEventListener('error', showPlaceholder);
    input.addEventListener('change', () => {
        const file = input.files && input.files[0];
        if (!file) return;
        if (remove) remove.checked = false;
        placeholder.hidden = true;
        preview.hidden = false;
        preview.src = URL.createObjectURL(file);
    });
    if (remove) remove.addEventListener('change', () => {
        if (remove.checked) { input.value = ''; showPlaceholder(); }
    });
})();
</script></body>
</html>
