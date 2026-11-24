<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty sessionScope.user or fn:toUpperCase(sessionScope.user.role) ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Aura Café | Quản lý sản phẩm</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <style>
        .admin-main-wrapper { max-width:1600px; }
        .products-page { display:flex; flex-direction:column; gap:18px; }
        .products-overview, .product-card { background:#fff; border:1px solid #e7e5e4; border-radius:16px; box-shadow:0 1px 2px rgba(28,25,23,.04); }
        .products-overview { padding:20px; }
        .products-heading { display:flex; align-items:flex-start; justify-content:space-between; gap:18px; }
        .table-ref { display:inline-block; padding:3px 8px; border-radius:6px; background:#f5f5f4; color:#78716c; font:700 10px/1.5 ui-monospace,monospace; }
        .page-title { margin:5px 0 2px; color:#1c1917; font-size:1.45rem; font-weight:800; letter-spacing:-.025em; }
        .page-subtitle { margin:0; color:#78716c; font-size:.82rem; }
        .product-stats { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:10px; padding-top:14px; margin-top:16px; border-top:1px solid #f5f5f4; }
        .product-stat { padding:10px 12px; border:1px solid #e7e5e4; border-radius:12px; background:#fafaf9; }
        .product-stat span { display:block; color:#78716c; font-size:10px; text-transform:uppercase; font-weight:800; }
        .product-stat strong { display:block; margin-top:2px; color:#1c1917; font:800 1.25rem ui-monospace,monospace; }
        .product-stat.active { background:#ecfdf5; border-color:#a7f3d0; }
        .product-stat.active strong { color:#047857; }
        .product-stat.paused strong { color:#78716c; }
        .product-panel-title { padding:14px 16px; margin:0; border-bottom:1px solid #f5f5f4; font-size:.88rem; font-weight:800; }
        .product-editor { padding:17px; }
        .product-editor .form-label { margin-bottom:5px; color:#57534e; font-size:.76rem; font-weight:700; }
        .product-editor .form-control, .product-editor .form-select { border-color:#e7e5e4; border-radius:10px; background:#fafaf9; font-size:.83rem; }
        .product-editor .form-control:focus, .product-editor .form-select:focus { border-color:#900b1a; box-shadow:0 0 0 .18rem rgba(144,11,26,.1); }
        .product-form-field { margin-bottom:13px; }
        .product-image, .product-image-placeholder { width:52px; height:48px; }
        .product-image { object-fit:cover; border-radius:9px; background:#f5f5f4; }
        .product-image-placeholder { display:flex; align-items:center; justify-content:center; border:1px dashed #d6d3d1; border-radius:9px; color:#78716c; background:#fafaf9; font-size:9px; text-align:center; }
        .product-image-preview { width:100%; max-width:250px; height:130px; object-fit:cover; border:1px solid #e7e5e4; border-radius:10px; background:#fafaf9; }
        .product-table > :not(caption) > * > * { padding:.7rem .6rem; vertical-align:middle; }
        .product-table thead th { color:#78716c; font-size:10px; font-weight:800; letter-spacing:.04em; white-space:nowrap; }
        .product-table tbody tr:hover { background:#fafaf9; }
        .variant-editor { padding:12px; border:1px solid #e7e5e4; border-radius:12px; background:#fafaf9; }
        .variant-row { display:grid; grid-template-columns:1fr 130px auto; gap:8px; align-items:center; margin-top:8px; }
        .variant-row .form-control { background:#fff; }
        .variant-heading { display:flex; justify-content:space-between; align-items:center; gap:8px; color:#57534e; font-size:.78rem; font-weight:800; }
        .product-layout .product-editor { position:sticky; top:16px; }
        .product-list-card { min-width:0; }
        .product-list-card .product-table { min-width:700px; }
        .product-editor .product-form-grid { --bs-gutter-y: .75rem; }
        .product-editor .variant-editor { margin-top:0; }
        .btn-aura { color:#fff; background:#900b1a; border-color:#900b1a; }
        .btn-aura:hover { color:#fff; background:#7a121d; border-color:#7a121d; }
        .mobile-admin-bar { display:none; }
        @media(max-width:767.98px) {
            .mobile-admin-bar { display:flex; align-items:center; justify-content:space-between; padding:10px 15px; background:#fff; border-bottom:1px solid #e7e5e4; }
            .products-overview { padding:15px; }
            .product-stats { grid-template-columns:repeat(3,minmax(0,1fr)); }
            .page-title { font-size:1.2rem; }
            .products-heading { gap:10px; }
            .variant-row { grid-template-columns:1fr 92px auto; }
            .product-layout .product-editor { position:static; }
        }
    </style>
</head>
<body class="admin-body">
<div class="mobile-admin-bar">
    <%-- [SPHD01]
         . Liên kết AURA CAFÉ mở admin-dashboard.jsp để quay về trang quản trị. Nút Đăng xuất bên cạnh mở
           /login?action=logout, do LoginController.doGet gọi session.invalidate rồi chuyển về Trang chủ. Hai
           nút này không gửi form lưu sản phẩm, nên thông tin đang chỉnh dở chưa được ghi.
         ... Kết thúc tại trang quản trị khi bấm logo, hoặc tại Trang chủ sau khi LoginController.doGet hủy
             phiên nếu bấm Đăng xuất.
    --%>
    <a href="${ctx}/admin-dashboard.jsp" class="text-decoration-none fw-bold text-danger"><span class="material-symbols-outlined align-middle">local_cafe</span> AURA CAFÉ · ADMIN</a>
    <a href="${ctx}/login?action=logout" class="btn btn-sm btn-light">Đăng xuất</a>
</div>
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="products" /></jsp:include>


<main class="admin-main-wrapper products-page">
    <header class="products-overview">
        <div class="products-heading">
            <div>
                <span class="table-ref">Bảng: products</span>
                <h1 class="page-title">Quản lý sản phẩm &amp; giá bán</h1>
                <p class="page-subtitle">Thêm món, gán vào danh mục, cập nhật ảnh, giá, size, topping và trạng thái bán.</p>
            </div>
        </div>
        <%-- [SPM04]
             . Ba thẻ trên admin-products.jsp dùng productTotal, productAvailable và productPaused mà
               AdminProductsController.doGet đưa sang. Em hiện lần lượt tổng món, số đang bán và số tạm ngừng để
               người dùng nắm tình trạng sản phẩm trước khi thao tác.
             ... Kết thúc phần thống kê khi ba số tổng quan đã hiện trên admin-products.jsp.
        --%>
        <div class="product-stats" aria-label="Thống kê sản phẩm">
            <div class="product-stat"><span>Tổng sản phẩm</span><strong><c:out value="${productTotal}" /></strong></div>
            <div class="product-stat active"><span>Đang bán</span><strong><c:out value="${productAvailable}" /></strong></div>
            <div class="product-stat paused"><span>Tạm ngưng</span><strong><c:out value="${productPaused}" /></strong></div>
        </div>
    </header>

    <%-- [SPL08]
         . Khi trang mở lại, admin-products.jsp hiện productMessage hoặc productError để báo kết quả. Với
           lỗi xảy ra sau khi đọc được form, controller giữ tên, danh mục, giá, mô tả và các dòng
           size/topping để sửa tiếp, kể cả dòng trùng hoặc giá sai. Ảnh mới phải chọn lại; ô Xóa ảnh hiện
           tại cần đánh dấu lại.
         . Kết thúc lần lưu khi trang hiện kết quả cùng danh sách và form đã đọc lại.
         ... Thông báo và danh sách mới là kết thúc thao tác sản phẩm. Nếu cần xem cách giữ form khi lưu
             lỗi, đọc AdminProductsController.java, tìm [SPL09]; người dùng sửa rồi bấm Lưu mới gửi lại.
    --%>
    <c:if test="${not empty productMessage and empty productError}"><div data-form-notice class="alert alert-success py-2 mb-0" role="status"><c:out value="${productMessage}" /></div></c:if>
    <c:if test="${not empty productError}"><div data-form-notice class="alert alert-danger py-2 mb-0" role="alert"><c:out value="${productError}" /></div></c:if>
    <c:if test="${empty categories}"><div class="alert alert-warning py-2 mb-0">Chưa có danh mục. Hãy tạo danh mục trước khi thêm món.</div></c:if>

    <div class="row g-4 align-items-start product-layout">
        <section class="col-12 col-xl-8">
            <div class="product-card p-3 mb-3">

                <%-- [SPLOC01]
                     . Khi bấm Lọc trong admin-products.jsp, form gửi từ khóa q và categoryId đến doGet trong
                       AdminProductsController.java. Controller truyền hai điều kiện vào ProductDAO.getProductsForAdmin
                       rồi đưa danh sách khớp về trang để hiển thị.
                     ... Form gửi q và categoryId tới doGet. Đọc phần nhận hai điều kiện tại
                         AdminProductsController.java, tìm mã SPM02 rồi theo chỉ dẫn để đọc danh sách và bảng kết quả.
                --%>
                <form method="get" action="${ctx}/admin/products" class="row g-2 align-items-center">
                    <div class="col-12 col-lg-5"><input name="q" value="${fn:escapeXml(searchKeyword)}" class="form-control form-control-sm" placeholder="Tìm tên, mã, mô tả..."></div>
                    <div class="col-12 col-sm-8 col-lg-4">
                        <select name="categoryId" class="form-select form-select-sm">
                            <option value="all">Tất cả danh mục</option>
                            <c:forEach var="category" items="${categories}"><option value="${category.id}" <c:if test="${selectedCategoryId eq category.id}">selected</c:if>><c:out value="${category.name}" /><c:if test="${category.status ne 'ACTIVE'}"> · Tạm ẩn</c:if></option></c:forEach>
                        </select>
                    </div>
                    <div class="col-12 col-sm-4 col-lg-2 d-grid"><button class="btn btn-sm btn-outline-secondary" type="submit"><span class="material-symbols-outlined fs-6 align-middle">search</span> Lọc</button></div>
                </form>
                <%-- [SPBL01]
                     . Khi bấm Xóa bộ lọc, em mở lại /admin/products không kèm q và categoryId.
                       AdminProductsController.doGet gọi ProductDAO.getProductsForAdmin không có điều kiện để đọc lại
                       toàn bộ món; thao tác này không xóa sản phẩm nào.
                     ... Mở lại /admin/products không có q và categoryId. Theo doGet tại AdminProductsController.java,
                         tìm mã SPM02 để đọc lại danh sách; không xóa sản phẩm.
                --%>
                <c:if test="${not empty searchKeyword or selectedCategoryId gt 0}"><a class="small d-inline-block mt-2" href="${ctx}/admin/products">Xóa bộ lọc</a></c:if>
            </div>

            <section class="product-card product-list-card overflow-hidden">
                <h2 class="product-panel-title">Danh sách sản phẩm <span class="text-muted fw-normal">(<c:out value="${fn:length(products)}" />)</span></h2>
                <div class="table-responsive">
                    <table class="table table-hover product-table mb-0">
                        <thead class="table-light text-uppercase"><tr><th>Sản phẩm</th><th>Danh mục</th><th>Size / Topping</th><th class="text-end">Giá bán</th><th class="text-center">Trạng thái</th><th class="text-end">Thao tác</th></tr></thead>
                        <tbody>
                        <%-- [SPD01]
                             . Để hiển thị bảng trong admin-products.jsp, em lặp danh sách products mà
                               AdminProductsController.doGet đưa sang. Tên, giá, trạng thái và mã của các nút đều lấy từ cùng một
                               sản phẩm, nên thao tác Sửa, Ẩn hoặc Xóa sẽ tác động đúng hàng.
                             ... Kết thúc mở danh sách khi các hàng sản phẩm đã hiện trên admin-products.jsp; nút của mỗi hàng mở
                                 thao tác riêng với mã của hàng đó.
                        --%>
                        <c:forEach var="product" items="${products}">
                    <tr data-variants="${fn:escapeXml(product.variantsConfig)}">
                        <td><div class="d-flex align-items-center gap-2">
                            <c:choose><c:when test="${product.storedImage}"><img class="product-image" src="${ctx}/product-image?id=${product.id}" alt="Ảnh ${fn:escapeXml(product.name)}"></c:when><c:when test="${not empty product.imageUrl}"><img class="product-image" src="${fn:escapeXml(product.imageUrl)}" alt="Ảnh ${fn:escapeXml(product.name)}"></c:when><c:otherwise><div class="product-image-placeholder">Chưa có ảnh</div></c:otherwise></c:choose>
                            <div class="min-w-0"><strong class="d-block text-dark"><c:out value="${product.name}" /></strong><div class="small text-muted text-truncate"><span class="font-monospace text-danger">#<c:out value="${product.id}" /></span></div></div>
                        </div></td>
                        <td><span class="badge rounded-pill text-bg-light border"><c:out value="${product.categoryName}" /></span></td>
                        <td><div class="small variant-summary"><span class="variant-sizes text-dark"></span><span class="variant-toppings d-block text-muted"></span></div></td>
                        <%-- [SPDINH01]
                             . Giá bán trong bảng dùng product.formattedPrice, tức gọi getFormattedPrice trong Product.java trên
                               giá base_price đã đọc từ cơ sở dữ liệu. Hàm thêm dấu phân cách hàng nghìn và chữ đ để hiển thị;
                               không ghi lại hay thay đổi giá đang lưu.
                             ... Đọc tiếp tại Product.java, tìm [SPDINH02].
                        --%>
                        <td class="text-end fw-bold text-nowrap"><c:out value="${product.formattedPrice}" /></td>
                        <td class="text-center"><c:choose><c:when test="${product.available}"><span class="badge rounded-pill text-bg-success">Đang bán</span></c:when><c:otherwise><span class="badge rounded-pill text-bg-secondary">Tạm ngưng</span></c:otherwise></c:choose></td>
                        <td class="text-end text-nowrap">

                            <%-- [SPS01]
                                 . Khi người dùng bấm Sửa ở một hàng sản phẩm, em mở /admin/products kèm editId của món đó.
                                   AdminProductsController.doGet dùng mã này gọi ProductDAO.getProductForAdmin để lấy thông tin món,
                                   size và topping rồi điền vào form Sửa.
                                 ... Đọc tiếp tại AdminProductsController.java, tìm [SPS02].
                            --%>
                            <a class="btn btn-sm btn-outline-primary" href="${ctx}/admin/products?editId=${product.id}#productEditor" title="Sửa"><span class="material-symbols-outlined fs-6 align-middle">edit</span></a>

                            <form method="post" action="${ctx}/admin/products" class="d-inline" onsubmit="return this.elements.available.value === 'true' || confirm('Ngừng bán món này? Món sẽ được ẩn khỏi menu, lịch sử đơn hàng vẫn được giữ.');">
                                <%-- [SPAH01]
                                     . Khi người dùng bấm Ngừng bán hoặc Khôi phục bán trong admin-products.jsp, form gửi mã món, trạng
                                       thái available mới và action=toggle tới AdminProductsController.doPost. Em gọi toggle để kiểm tra
                                       và ghi trạng thái cho đúng sản phẩm vừa bấm.
                                     ... Đọc tiếp tại AdminProductsController.java, tìm [SPAH02].
                                --%>
                                <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.productCsrfToken)}"><input type="hidden" name="action" value="toggle"><input type="hidden" name="id" value="${product.id}">
                                <input type="hidden" name="available" value="${!product.available}">
                                <c:choose><c:when test="${product.available}"><button class="btn btn-sm btn-outline-secondary" title="Ngừng bán"><span class="material-symbols-outlined fs-6 align-middle">visibility_off</span></button></c:when><c:otherwise><button class="btn btn-sm btn-outline-success" title="Khôi phục bán"><span class="material-symbols-outlined fs-6 align-middle">visibility</span></button></c:otherwise></c:choose>
                            </form>
                            <c:if test="${not product.available}">

                                <form method="post" action="${ctx}/admin/products" class="d-inline" onsubmit="return confirm('Xóa vĩnh viễn sản phẩm này? Chỉ tiếp tục nếu sản phẩm chưa có trong đơn hàng.');">
                                    <%-- [SPX01]
                                         . Khi người dùng bấm Xóa sản phẩm, em hỏi xác nhận trước. Đồng ý thì form gửi mã món và
                                           action=delete tới AdminProductsController.doPost. Em gọi delete để kiểm tra sản phẩm đủ điều kiện
                                           rồi chuyển sang ProductDAO.deleteProduct để xóa và báo kết quả.
                                         .. Nút chỉ hiện khi món đã ngừng bán. Bấm Hủy ở hộp xác nhận thì dừng gửi form, sản phẩm vẫn được
                                            giữ nguyên.
                                         ... Đọc tiếp tại AdminProductsController.java, tìm [SPX02].
                                    --%>
                                    <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.productCsrfToken)}"><input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${product.id}">
                                    <button class="btn btn-sm btn-outline-danger" title="Xóa sản phẩm"><span class="material-symbols-outlined fs-6 align-middle">delete_forever</span></button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                        </c:forEach>
                        <c:if test="${empty products}"><tr><td colspan="6" class="text-center text-muted py-5">Không tìm thấy sản phẩm nào.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </section>

        <section class="col-12 col-xl-4" id="productEditor">
        <div class="product-card product-editor">
        <div class="d-flex justify-content-between align-items-center border-bottom pb-3 mb-3">
            <div><h2 class="h6 fw-bold mb-0"><c:choose><c:when test="${not empty selectedProduct and selectedProduct.id gt 0}">Sửa sản phẩm</c:when><c:otherwise>Thêm sản phẩm mới</c:otherwise></c:choose></h2><p class="small text-muted mb-0 mt-1">Danh mục, giá và ảnh tại đây sẽ dùng trên menu khách hàng.</p></div>
            <c:if test="${not empty selectedProduct and selectedProduct.id gt 0}"><span class="badge text-bg-light font-monospace">#<c:out value="${selectedProduct.id}" /></span></c:if>
        </div>


        <%-- [SPS03]
             . Form trong admin-products.jsp dùng selectedProduct và bản nhập dở từ
               AdminProductsController.doGet. Có sản phẩm được chọn thì điền form Sửa; không có thì dùng id=0
               cho yêu cầu thêm mới. Nếu lưu lỗi thì giữ thông tin để tiếp tục sửa.
             ... Các ô đã điền xong là kết thúc mở form Sửa. Khi người dùng bấm Lưu thay đổi, theo
                 admin-products.jsp, tìm [SPL01].
        --%>
        <form id="productForm" data-admin-form="product" method="post" action="${ctx}/admin/products" enctype="multipart/form-data" novalidate>
            <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.productCsrfToken)}"><input type="hidden" name="action" value="save"><input type="hidden" name="id" value="${empty selectedProduct ? 0 : selectedProduct.id}">
            <input type="hidden" id="variantsConfig" value="${fn:escapeXml(selectedProduct.variantsConfig)}">
                <div class="row product-form-grid">

                <%-- [SPT01]
                     . Ô Tên món trong admin-products.jsp gửi giá trị qua name khi lưu form. Hàm save trong
                       AdminProductsController.java kiểm tra nội dung và gọi ProductDAO.productNameExists để chặn tên
                       thuộc món khác trước khi ghi sản phẩm.
                     ... Đọc tiếp tại AdminProductsController.java, tìm [SPT02].
                --%>
                <div class="col-12"><label for="name" class="form-label">Tên món *</label><input id="name" name="name" class="form-control" required maxlength="150" value="${fn:escapeXml(selectedProduct.name)}"></div>

                <%-- [SPDM01]
                     . Ô Danh mục trong admin-products.jsp gửi mã được chọn qua categoryId. Hàm save trong
                       AdminProductsController.java truyền mã này vào CategoryDAO.getCategoryById để chỉ cho lưu món vào
                       danh mục còn tồn tại và đang hoạt động.
                     ... Đọc tiếp tại AdminProductsController.java, tìm [SPDM02].
                --%>
                <div class="col-12"><label for="categoryId" class="form-label">Danh mục *</label><select id="categoryId" name="categoryId" class="form-select" required <c:if test="${empty categories}">disabled</c:if>><option value="">Chọn danh mục</option><c:forEach var="category" items="${categories}"><c:if test="${category.status eq 'ACTIVE'}"><option value="${category.id}" <c:if test="${selectedProduct.categoryId eq category.id}">selected</c:if>><c:out value="${category.name}" /></option></c:if></c:forEach></select></div>

                <%-- [SPG01]
                     . Ô Giá cơ bản trong admin-products.jsp gửi giá trị qua basePrice. Khi lưu, hàm save trong
                       AdminProductsController.java đổi thành số, kiểm tra giới hạn rồi đưa giá vào Product để
                       ProductDAO.saveProduct ghi cùng thông tin món.
                     ... Đọc tiếp tại AdminProductsController.java, tìm [SPG02].
                --%>
                <div class="col-12"><label for="basePrice" class="form-label">Giá bán cơ bản (VNĐ) *</label><input id="basePrice" name="basePrice" type="text" inputmode="decimal" class="form-control" required value="${hasProductDraft ? fn:escapeXml(productDraftBasePrice) : (empty selectedProduct ? '' : selectedProduct.basePriceInputValue)}"></div>

                <%-- [SPMT01]
                     . Ô Mô tả trong admin-products.jsp gửi giá trị qua description khi lưu. Hàm save trong
                       AdminProductsController.java kiểm tra độ dài; đúng thì đưa vào thông tin món để ghi, sai thì trả
                       lỗi về form cho người dùng sửa.
                     ... Đọc tiếp tại AdminProductsController.java, tìm [SPMT02].
                --%>
                <div class="col-12"><label for="description" class="form-label">Mô tả</label><textarea id="description" name="description" rows="2" maxlength="5000" class="form-control">${fn:escapeXml(selectedProduct.description)}</textarea></div>
                <div class="col-12">
                    <label for="imageFile" class="form-label">Ảnh sản phẩm</label>

                    <%-- [SPAN01]
                         . Ở admin-products.jsp, ảnh mới gửi qua imageFile và lựa chọn xóa ảnh gửi qua removeImage. Hàm save
                           trong AdminProductsController.java kiểm tra tệp rồi chuyển cho ProductDAO.saveProduct để thay hoặc
                           xóa ảnh cùng lần lưu sản phẩm.
                         ... Đọc tiếp tại AdminProductsController.java, tìm [SPAN02].
                    --%>
                    <input id="imageFile" name="imageFile" type="file" accept="image/jpeg,image/png,image/gif,image/webp" class="form-control">
                    <div class="form-text">JPG, PNG, GIF hoặc WebP; tối đa 5 MB. Ảnh được lưu trực tiếp trong database.</div>
                    <c:set var="selectedHasImage" value="${not empty selectedProduct and (selectedProduct.storedImage or not empty selectedProduct.imageUrl)}" />
                    <c:choose><c:when test="${selectedProduct.storedImage}"><c:set var="selectedImageSrc" value="${ctx}/product-image?id=${selectedProduct.id}" /></c:when><c:otherwise><c:set var="selectedImageSrc" value="${selectedProduct.imageUrl}" /></c:otherwise></c:choose>
                    <div class="mt-2"><img id="productImagePreview" class="product-image-preview" src="${fn:escapeXml(selectedImageSrc)}" alt="Xem trước ảnh sản phẩm" <c:if test="${not selectedHasImage}">hidden</c:if>><div id="productImagePlaceholder" class="product-image-placeholder" style="width:100%;max-width:250px;height:130px" <c:if test="${selectedHasImage}">hidden</c:if>>Chưa có ảnh</div></div>
                    <%-- [SPXA01]
                         . Chọn Xóa ảnh hiện tại mới bỏ ảnh ở phần xem trước. Khi bấm Lưu thay đổi, form gửi removeImage tới
                           AdminProductsController.save rồi ProductDAO.saveProduct để xóa ảnh đang lưu. Nếu chọn ảnh mới thì
                           ưu tiên lưu ảnh mới và bỏ lựa chọn xóa ảnh.
                         ... Đánh dấu removeImage chỉ đổi lựa chọn trên form. Bấm Lưu thay đổi tại admin-products.jsp, tìm mã
                             SPL01 thì controller mới nhận yêu cầu xóa ảnh.
                    --%>
                    <c:if test="${selectedHasImage}"><div class="form-check mt-2"><input id="removeImage" name="removeImage" value="true" type="checkbox" class="form-check-input"><label for="removeImage" class="form-check-label small">Xóa ảnh hiện tại</label></div></c:if>
                </div>

                <%-- [SPSZ01]
                     . Khi người dùng bấm Thêm size trong admin-products.jsp, em gọi addVariantRow ngay trong file. Em
                       truyền vùng size cùng hai tên ô sizeName và sizeDelta để hàm tạo dòng nhập tên size và phụ thu.
                       Các dòng này được gửi cùng sản phẩm khi bấm Lưu.
                     ... Nút gọi addVariantRow cùng file để tạo dòng size. Đọc tại admin-products.jsp, tìm [SPBT01]; thêm
                         xong chưa lưu sản phẩm.
                --%>
                <div class="col-12"><div class="variant-editor"><div class="variant-heading"><span>Size &amp; phụ thu</span><button type="button" class="btn btn-sm btn-outline-primary" id="addSize"><span class="material-symbols-outlined fs-6 align-middle">add</span> Thêm size</button></div><div id="sizesList"></div></div></div>

                <%-- [SPTP01]
                     . Khi người dùng bấm Thêm topping trong admin-products.jsp, em gọi addVariantRow ngay trong file. Em
                       truyền vùng topping cùng toppingName và toppingPrice để tạo dòng tên và giá topping. Người dùng
                       nhập xong rồi bấm Lưu sản phẩm mới gửi các dòng đi.
                     ... Nút gọi addVariantRow cùng file để tạo dòng topping. Đọc tại admin-products.jsp, tìm mã SPBT01;
                         thêm xong chưa lưu sản phẩm.
                --%>
                <div class="col-12"><div class="variant-editor"><div class="variant-heading"><span>Topping &amp; giá</span><button type="button" class="btn btn-sm btn-outline-primary" id="addTopping"><span class="material-symbols-outlined fs-6 align-middle">add</span> Thêm topping</button></div><div id="toppingsList"></div><div class="form-text">Không bắt buộc.</div></div></div>
                </div>


            <div class="d-flex flex-wrap gap-2 mt-4">
                <%-- [SPL01]
                     . Em bắt đầu từ nút Thêm sản phẩm hoặc Lưu thay đổi trong admin-products.jsp. Khi bấm,
                       admin-form-validation.js kiểm tra các ô, ảnh, size và topping. Đúng thì form gửi tới
                       /admin/products để AdminProductsController.doPost kiểm tra lại và lưu. id=0 là thêm mới; có mã sản
                       phẩm thì sửa đúng món đó.
                     ... Đọc tiếp tại assets/js/admin-form-validation.js, tìm [SPL02].
                --%>
                <button type="submit" class="btn btn-aura fw-bold" <c:if test="${empty categories}">disabled</c:if>><span class="material-symbols-outlined fs-6 align-middle">check</span> <c:choose><c:when test="${not empty selectedProduct and selectedProduct.id gt 0}">Lưu thay đổi</c:when><c:otherwise>Thêm sản phẩm</c:otherwise></c:choose></button>
                <c:if test="${not empty selectedProduct and selectedProduct.id gt 0}">
                    <%-- [SPH01]
                         . Khi bấm Hủy sửa trong admin-products.jsp, em mở lại /admin/products mà không có editId. doGet
                           trong AdminProductsController.java mở form thêm mới để thoát phần đang sửa; thao tác này không lưu
                           những thay đổi vừa nhập.
                         ... Form thêm mới đã mở lại là kết thúc Hủy sửa. Người dùng nhập món mới rồi bấm Thêm sản phẩm thì
                             theo admin-products.jsp, tìm mã SPL01.
                    --%>
                    <a href="${ctx}/admin/products" class="btn btn-light">Hủy sửa</a>
                </c:if>
            </div>
        </form>
        </div>
        </section>
    </div>
</main>
<script src="${ctx}/assets/js/admin-form-validation.js"></script>
<script>
(() => {
    const sizesList = document.getElementById('sizesList');
    const toppingsList = document.getElementById('toppingsList');
    const configInput = document.getElementById('variantsConfig');
    const form = document.getElementById('productForm');
    const imageInput = document.getElementById('imageFile');
    const preview = document.getElementById('productImagePreview');
    const placeholder = document.getElementById('productImagePlaceholder');
    const removeImage = document.getElementById('removeImage');
    const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));

    // [SPBT01]
    // . Hàm addVariantRow nhận vùng cần thêm dòng và tên của hai ô tên, tiền. Em tạo các ô đúng tên gửi,
    //   gắn nút xóa của dòng rồi thêm dòng vào vùng size hoặc topping. Nhờ vậy, khi lưu, controller đọc
    //   được các cặp tên và tiền tương ứng.
    // ... Dòng được gắn vào vùng size hoặc topping là kết thúc Thêm dòng. Người dùng bấm lưu sản phẩm thì
    //     theo admin-products.jsp, tìm mã SPL01; kiểm tra tên/giá bên controller nằm tại SPBT02.
    const addVariantRow = (container, name, amount, nameField, amountField, namePlaceholder, amountPlaceholder) => {
        const row = document.createElement('div');
        row.className = 'variant-row';
        // [SPTEN01]
        // . Ở addVariantRow, em tạo ô tên bằng nameField: sizeName cho size hoặc toppingName cho topping. Khi
        //   lưu, buildVariantsConfig trong AdminProductsController.java đọc các tên này, kiểm tra không trùng
        //   và giới hạn 30 ký tự cho size, 50 cho topping rồi gọi validateVariantName.
        // ... Đọc tiếp tại AdminProductsController.java, tìm [SPTEN02].
        row.innerHTML = '<input class="form-control form-control-sm" name="' + escapeHtml(nameField) + '" maxlength="50" value="' + escapeHtml(name) + '" placeholder="' + escapeHtml(namePlaceholder) + '">' +
            // [SPTIEN01]
            // . Ô tiền được tạo bằng amountField: sizeDelta là phụ thu size, toppingPrice là giá topping. Khi lưu,
            //   buildVariantsConfig trong AdminProductsController.java ghép tiền với đúng tên cùng dòng rồi gọi
            //   parseNonNegative để kiểm tra số. Nhập 0 là không phụ thu hoặc miễn phí.
            // ... Đọc tiếp tại AdminProductsController.java, tìm [SPTIEN02].
            '<input class="form-control form-control-sm" name="' + escapeHtml(amountField) + '" type="text" inputmode="decimal" value="' + escapeHtml(amount ?? '') + '" placeholder="' + escapeHtml(amountPlaceholder) + '">' +
            '<button type="button" class="btn btn-sm btn-outline-danger" aria-label="Xóa dòng"><span class="material-symbols-outlined fs-6">delete</span></button>';
        // [SPXR01]
        // . Khi người dùng bấm thùng rác ở một dòng size hoặc topping, em gọi row.remove để bỏ đúng dòng đó
        //   khỏi form. Các dòng còn lại sẽ được gửi khi bấm Lưu; lúc đó controller kiểm tra rồi
        //   ProductDAO.saveProduct mới ghi thông tin lựa chọn mới.
        // ... Xóa dòng khỏi form là kết thúc nút thùng rác, chưa đổi sản phẩm đang lưu. Bấm nút lưu tại
        //     admin-products.jsp, tìm mã SPL01 mới gửi các dòng còn lại.
        row.querySelector('button').addEventListener('click', () => row.remove());
        container.appendChild(row);
    };
    document.getElementById('addSize').addEventListener('click', () => addVariantRow(sizesList, '', null, 'sizeName', 'sizeDelta', 'Tên size', 'Phụ thu'));
    document.getElementById('addTopping').addEventListener('click', () => addVariantRow(toppingsList, '', null, 'toppingName', 'toppingPrice', 'Tên topping', 'Giá'));
    let config = {};
    try { config = JSON.parse(configInput.value || '{}'); } catch (ignored) { config = {}; }
    const sizes = config.sizes && typeof config.sizes === 'object' ? Object.entries(config.sizes) : [];
    const toppings = Array.isArray(config.toppings) ? config.toppings : [];
    sizes.forEach(([name, delta]) => addVariantRow(sizesList, name, delta, 'sizeName', 'sizeDelta', 'Tên size', 'Phụ thu'));
    toppings.forEach(t => addVariantRow(toppingsList, typeof t === 'string' ? t : t.name, typeof t === 'string' ? null : t.price, 'toppingName', 'toppingPrice', 'Tên topping', 'Giá'));
    (config.draftSizes || []).forEach(t => addVariantRow(sizesList, t.name, t.amount, 'sizeName', 'sizeDelta', 'Tên size', 'Phụ thu'));
    (config.draftToppings || []).forEach(t => addVariantRow(toppingsList, t.name, t.amount, 'toppingName', 'toppingPrice', 'Tên topping', 'Giá'));

    // Việc kiểm tra từng ô, size và topping được thực hiện trong admin-form-validation.js.
    // [SPXTR01]
    // . Khi chọn ảnh trong admin-products.jsp, sự kiện imageInput.change gọi URL.createObjectURL để đưa
    //   tệp vào productImagePreview cho người dùng xem trước. Khi bấm lưu, tệp mới tới
    //   AdminProductsController.save để kiểm tra rồi ProductDAO.saveProduct để ghi vào cơ sở dữ liệu.
    // ... Ảnh đã hiện là kết thúc xem trước. Người dùng bấm Thêm sản phẩm hoặc Lưu thay đổi tại
    //     admin-products.jsp, tìm mã SPL01 mới gửi ảnh và các ô đi.
    const showPlaceholder = () => { preview.hidden = true; placeholder.hidden = false; };
    if (imageInput && preview && placeholder) {
        preview.addEventListener('error', showPlaceholder);
        if (preview.getAttribute('src') && preview.complete && preview.naturalWidth === 0) showPlaceholder();
        imageInput.addEventListener('change', () => {
            const file = imageInput.files && imageInput.files[0];
            if (!file) return;
            if (removeImage) removeImage.checked = false;
            placeholder.hidden = true;
            preview.hidden = false;
            preview.src = URL.createObjectURL(file);
        });
        if (removeImage) removeImage.addEventListener('change', () => {
            if (removeImage.checked) { imageInput.value = ''; showPlaceholder(); }
        });
    }
    // [SPHT01]
    // . Cột Size / Topping trong bảng lấy cấu hình đã lưu từ data-variants của từng sản phẩm. Em đọc danh
    //   sách size để hiện tên và phụ thu, đếm topping rồi điền vào variant-sizes và variant-toppings. Đây
    //   là cấu hình do ProductDAO.getProductsForAdmin đọc và controller đưa về, không phải dòng nhập dở.
    // ... Kết thúc dựng cột khi tên size, phụ thu và số topping đã hiện trên đúng hàng sản phẩm.
    document.querySelectorAll('tr[data-variants]').forEach(row => {
        let data = {};
        try { data = JSON.parse(row.dataset.variants || '{}'); } catch (ignored) { data = {}; }
        const sizes = data.sizes && typeof data.sizes === 'object' ? Object.entries(data.sizes) : [];
        row.querySelector('.variant-sizes').textContent = sizes.map(([name, delta]) => name + (Number(delta) ? ' (+' + Number(delta).toLocaleString('vi-VN') + 'đ)' : '')).join(', ') || 'Chưa cấu hình size';
        const toppingCount = Array.isArray(data.toppings) ? data.toppings.length : 0;
        row.querySelector('.variant-toppings').textContent = toppingCount ? toppingCount + ' topping' : 'Chưa có topping';
    });
})();
</script>
</body>
</html>
