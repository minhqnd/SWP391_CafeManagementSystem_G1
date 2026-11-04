<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty sessionScope.user or fn:toUpperCase(sessionScope.user.role) ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Aura Café | Công thức & định mức</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <style>
        .admin-main-wrapper { max-width:1500px; }
        .recipe-page { display:flex; flex-direction:column; gap:16px; }
        .recipe-heading,.recipe-card,.recipe-stat { background:#fff; border:1px solid #e7e5e4; border-radius:16px; box-shadow:0 1px 2px rgba(28,25,23,.04); }
        .recipe-heading { padding:20px; }
        .recipe-heading h1 { margin:4px 0; font-size:1.35rem; font-weight:800; color:#1c1917; }
        .recipe-heading p { margin:0; color:#78716c; font-size:.8rem; }
        .recipe-kicker { color:#900b1a; font-size:10px; letter-spacing:.08em; font-weight:800; text-transform:uppercase; }
        .recipe-stats { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:12px; }
        .recipe-stat { padding:13px 15px; background:#fafaf9; }
        .recipe-stat span { display:block; color:#78716c; font-size:.66rem; font-weight:800; text-transform:uppercase; }
        .recipe-stat strong { display:block; margin-top:3px; color:#292524; font-size:1.3rem; }
        .recipe-toolbar { display:flex; align-items:center; justify-content:space-between; gap:12px; }
        .recipe-toolbar h2 { margin:0; color:#292524; font-size:1rem; font-weight:800; }
        .recipe-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; }
        .recipe-card { display:flex; flex-direction:column; justify-content:space-between; gap:12px; padding:16px; min-width:0; }
        .recipe-product-head { display:flex; align-items:flex-start; justify-content:flex-start; gap:12px; }
        .recipe-product-info { display:flex; align-items:center; gap:11px; min-width:0; }
        .recipe-product-image { width:48px; height:48px; flex:none; border-radius:12px; object-fit:cover; background:#f5f5f4; }
        .recipe-product-placeholder { display:grid; place-items:center; color:#900b1a; }
        .recipe-product-id { color:#900b1a; font:700 .68rem ui-monospace,monospace; }
        .recipe-product-name { margin:1px 0; color:#1c1917; font-size:.92rem; font-weight:800; line-height:1.3; }
        .recipe-product-category { color:#a8a29e; font-size:.68rem; }
        .recipe-financials { display:grid; grid-template-columns:1fr 1fr; gap:8px; margin-top:13px; padding:10px; border-radius:11px; background:#fafaf9; }
        .recipe-financials span { display:block; color:#78716c; font-size:.64rem; }
        .recipe-financials strong { display:block; margin-top:2px; color:#292524; font-size:.83rem; }
        .recipe-financials .cost { color:#900b1a; }
        .recipe-lines { margin-top:8px; font-size:.72rem; }
        .recipe-line { display:flex; align-items:center; justify-content:space-between; gap:10px; padding:7px 0; border-bottom:1px solid #f5f5f4; }
        .recipe-line:last-child { border-bottom:0; }
        .recipe-line-name { color:#44403c; font-weight:600; }
        .recipe-line-qty { text-align:right; color:#78716c; white-space:nowrap; }
        .recipe-line-cost { color:#a8a29e; }
        .recipe-card-footer { display:flex; align-items:center; justify-content:space-between; gap:10px; padding-top:10px; border-top:1px solid #f5f5f4; }
        .recipe-badge { display:inline-flex; padding:4px 8px; border-radius:6px; font-size:.62rem; font-weight:800; }
        .recipe-badge.has { background:#d1fae5; color:#065f46; }
        .recipe-badge.empty { background:#ffe4e6; color:#9f1239; }
        .recipe-edit { display:inline-flex; align-items:center; gap:4px; padding:7px 10px; border:0; border-radius:9px; background:#f5f5f4; color:#44403c; font-size:.72rem; font-weight:800; text-decoration:none; }
        .recipe-edit:hover { background:#e7e5e4; color:#1c1917; }
        .recipe-empty { padding:32px; border:1px dashed #d6d3d1; border-radius:14px; color:#78716c; text-align:center; }
        .recipe-modal-backdrop { position:fixed; inset:0; z-index:1050; display:grid; place-items:center; padding:16px; background:rgba(28,25,23,.52); backdrop-filter:blur(3px); }
        .recipe-modal { display:flex; flex-direction:column; width:min(760px,100%); max-height:92vh; overflow:hidden; border-radius:16px; background:#fff; box-shadow:0 24px 70px rgba(0,0,0,.22); }
        .recipe-modal-head { display:flex; align-items:center; justify-content:space-between; padding:15px 18px; border-bottom:1px solid #f5f5f4; background:#fafaf9; }
        .recipe-modal-head h2 { margin:0; color:#292524; font-size:.95rem; font-weight:800; }
        .recipe-modal-body { overflow-y:auto; padding:18px; }
        .recipe-editor .form-label { color:#57534e; font-size:.75rem; font-weight:700; }
        .recipe-editor .form-control,.recipe-editor .form-select { border-color:#e7e5e4; border-radius:9px; font-size:.8rem; }
        .ingredient-row { display:grid; grid-template-columns:minmax(0,1fr) 115px 105px 32px; gap:8px; align-items:center; margin-bottom:8px; }
        .ingredient-row .form-control,.ingredient-row .form-select { min-width:0; }
        .ingredient-unit-cost,.quantity-unit { min-height:15px; padding:3px 2px 0; color:#78716c; font-size:.63rem; }
        .ingredient-quantity { min-width:0; }
        .ingredient-line-cost { color:#292524; font-size:.72rem; font-weight:700; text-align:right; }
        .ingredient-table-labels { display:grid; grid-template-columns:minmax(0,1fr) 115px 105px 32px; gap:8px; margin-bottom:6px; color:#78716c; font-size:.61rem; font-weight:800; text-transform:uppercase; }
        .recipe-cost-card { display:flex; align-items:center; justify-content:space-between; gap:14px; margin-top:14px; padding:13px; border:1px solid #ead8da; border-radius:11px; background:#fff8f8; }
        .recipe-cost-card strong { display:block; color:#900b1a; font-size:1.2rem; }
        .recipe-cost-card small { display:block; color:#78716c; font-size:.65rem; }
        .btn-aura { color:#fff; background:#900b1a; border-color:#900b1a; }
        .btn-aura:hover { color:#fff; background:#7a121d; border-color:#7a121d; }
        @media(max-width:900px) { .recipe-grid { grid-template-columns:1fr; } }
        @media(max-width:700px) { .recipe-stats { grid-template-columns:repeat(2,minmax(0,1fr)); } .recipe-toolbar { align-items:flex-start; flex-direction:column; } }
        @media(max-width:575px) { .recipe-heading { padding:16px; } .recipe-card { padding:13px; } .ingredient-row,.ingredient-table-labels { grid-template-columns:minmax(0,1fr) 78px 80px 27px; gap:4px; } .ingredient-row .form-control,.ingredient-row .form-select { padding:.4rem; font-size:.7rem; } }
    </style>
</head>
<body class="admin-body">
<div class="d-md-none d-flex align-items-center justify-content-between p-3 bg-white border-bottom">
    <a href="${ctx}/admin-dashboard.jsp" class="text-decoration-none fw-bold text-danger">Aura Café · Admin</a>
    <a href="${ctx}/login?action=logout" class="btn btn-sm btn-light">Đăng xuất</a>
</div>
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="recipes" /></jsp:include>

<main class="admin-main-wrapper recipe-page">
    <header class="recipe-heading">
        <div class="d-flex flex-wrap align-items-start justify-content-between gap-3">
            <div>
                <div class="recipe-kicker">Sản phẩm &amp; định lượng</div>
                <h1>Công thức &amp; định mức nguyên liệu (BOM)</h1>
                <p>Nguyên liệu cần cho một phần món; giá vốn tính từ các phiếu nhập đã nhận.</p>
            </div>
            <span class="badge text-bg-light border text-secondary">Bảng: product_ingredients</span>
        </div>
        <div class="recipe-stats mt-3">
            <div class="recipe-stat"><span>Tổng sản phẩm</span><strong><c:out value="${fn:length(products)}" /></strong></div>
            <div class="recipe-stat"><span>Đã có công thức</span><strong class="text-success"><c:out value="${productsWithRecipe}" /></strong></div>
            <div class="recipe-stat"><span>Chưa có công thức</span><strong class="text-danger"><c:out value="${productsWithoutRecipe}" /></strong></div>
            <div class="recipe-stat"><span>Dòng định mức</span><strong><c:out value="${recipeLineCount}" /></strong></div>
        </div>
    </header>

    <c:if test="${not empty recipeMessage}"><div class="alert alert-success mb-0"><c:out value="${recipeMessage}" /></div></c:if>
    <c:if test="${not empty recipeError and empty selectedProduct}"><div class="alert alert-danger mb-0" role="alert"><c:out value="${recipeError}" /></div></c:if>

    <div class="recipe-toolbar">
        <h2>Định mức theo sản phẩm</h2>
        <select id="recipeFilter" class="form-select form-select-sm" style="width:auto;min-width:190px" aria-label="Lọc sản phẩm theo công thức">
            <option value="all">Tất cả sản phẩm</option>
            <option value="with">Đã có công thức</option>
            <option value="without">Chưa có công thức</option>
        </select>
    </div>

    <c:choose>
        <c:when test="${empty productSummaries}"><div class="recipe-empty">Chưa có sản phẩm. Thêm sản phẩm trước khi khai báo công thức.</div></c:when>
        <c:otherwise>
            <div class="recipe-grid" id="recipeGrid">
                <c:forEach items="${productSummaries}" var="summary">
                    <c:set var="product" value="${summary.product}" />
                    <article class="recipe-card" data-has-recipe="${summary.hasRecipe}">
                        <div>
                            <div class="recipe-product-head">
                                <div class="recipe-product-info">
                                    <c:choose>
                                        <c:when test="${product.storedImage}"><img class="recipe-product-image" src="${pageContext.request.contextPath}/product-image?id=${product.id}" alt=""></c:when>
                                        <c:when test="${not empty product.imageUrl}"><img class="recipe-product-image" src="<c:out value='${product.imageUrl}' />" alt=""></c:when>
                                        <c:otherwise><div class="recipe-product-image recipe-product-placeholder"><span class="material-symbols-outlined">local_cafe</span></div></c:otherwise>
                                    </c:choose>
                                    <div class="min-w-0">
                                        <span class="recipe-product-id">#<c:out value="${product.id}" /></span>
                                        <h3 class="recipe-product-name"><c:out value="${product.name}" /></h3>
                                        <span class="recipe-product-category"><c:out value="${product.categoryName}" /></span>
                                    </div>
                                </div>
                            </div>
                            <div class="recipe-financials">
                                <div><span>Giá bán</span><strong><c:out value="${product.formattedPrice}" /></strong></div>
                                <div><span>Giá vốn theo định mức</span><strong class="cost"><c:choose><c:when test="${summary.costComplete}"><fmt:formatNumber value="${summary.recipeCost}" pattern="#,##0.##" />đ</c:when><c:when test="${summary.hasRecipe}"><fmt:formatNumber value="${summary.recipeCost}" pattern="#,##0.##" />đ · thiếu giá</c:when><c:otherwise>Chưa có định mức</c:otherwise></c:choose></strong></div>
                            </div>
                            <div class="recipe-lines">
                                <c:choose>
                                    <c:when test="${empty summary.ingredientLines}"><p class="py-2 mb-0 text-secondary">Chưa khai báo nguyên liệu.</p></c:when>
                                    <c:otherwise>
                                        <c:forEach items="${summary.ingredientLines}" var="line">
                                            <div class="recipe-line">
                                                <span class="recipe-line-name"><c:out value="${line.name}" /></span>
                                                <span class="recipe-line-qty"><fmt:formatNumber value="${line.quantity}" pattern="#,##0.###" /> <c:out value="${line.unit}" /><br><span class="recipe-line-cost"><c:choose><c:when test="${line.costAvailable}"><fmt:formatNumber value="${line.lineCost}" pattern="#,##0.##" />đ</c:when><c:otherwise>chưa có giá</c:otherwise></c:choose></span></span>
                                            </div>
                                        </c:forEach>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                        <div class="recipe-card-footer">
                            <c:choose><c:when test="${summary.hasRecipe}"><span class="recipe-badge has">Đã có công thức</span></c:when><c:otherwise><span class="recipe-badge empty">Chưa có công thức</span></c:otherwise></c:choose>
                            <a class="recipe-edit" href="${ctx}/admin/recipes?productId=${product.id}"><span class="material-symbols-outlined" style="font-size:16px">edit</span><c:choose><c:when test="${summary.hasRecipe}">Sửa định mức</c:when><c:otherwise>Thêm định mức</c:otherwise></c:choose></a>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</main>

<c:if test="${not empty selectedProduct}">
    <div class="recipe-modal-backdrop">
        <section class="recipe-modal" role="dialog" aria-modal="true" aria-labelledby="recipeModalTitle">
            <header class="recipe-modal-head">
                <h2 id="recipeModalTitle">Định mức: <c:out value="${selectedProduct.name}" /></h2>
                <a href="${ctx}/admin/recipes" class="btn-close" aria-label="Đóng"></a>
            </header>
            <div class="recipe-modal-body">
                <c:if test="${not empty recipeError}">
                    <div id="recipeFormError" class="alert alert-danger" role="alert" tabindex="-1">
                        <c:out value="${recipeError}" />
                        <div class="small mt-1">Công thức chưa được lưu. Hãy sửa dữ liệu bên dưới rồi thử lại.</div>
                    </div>
                </c:if>
                <%-- BF11 - LƯU BOM: POST productId, ingredientId[], quantity[] -> AdminRecipeController -> saveRecipe(). --%>
                <%-- Nếu lỗi: recipeDraft giữ dữ liệu vừa nhập, recipeError được hiển thị trong popup để sửa lại. --%>
                <form method="post" action="${ctx}/admin/recipes" class="recipe-editor" id="recipeForm">
                    <input type="hidden" name="action" value="save">
                    <input type="hidden" name="productId" value="${selectedProduct.id}">
                    <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.recipeCsrfToken)}">
                    <div class="mb-3">
                        <label class="form-label" for="selectedProductName">Sản phẩm</label>
                        <input class="form-control" id="selectedProductName" value="${fn:escapeXml(selectedProduct.name)}" readonly>
                    </div>
                    <div class="d-flex align-items-center justify-content-between gap-2 mb-2">
                        <div><h3 class="h6 fw-bold mb-1">Nguyên liệu cho một phần</h3><div class="text-muted" style="font-size:.68rem">Nhập số nguyên theo đơn vị hiện dưới ô định lượng.</div></div>
                        <button type="button" class="btn btn-sm btn-outline-danger" id="addIngredient" <c:if test="${empty ingredients}">disabled</c:if>>+ Thêm nguyên liệu</button>
                    </div>
                    <c:choose>
                        <c:when test="${empty ingredients}"><div class="alert alert-warning small">Chưa có nguyên liệu trong database.</div></c:when>
                        <c:otherwise>
                            <div class="ingredient-table-labels"><span>Nguyên liệu</span><span>Định lượng</span><span class="text-end">Cost / phần</span><span></span></div>
                            <div id="ingredientRows">
                                <c:choose>
                                    <c:when test="${not empty recipeFormRows}">
                                        <c:forEach items="${recipeFormRows}" var="recipeIngredient">
                                            <div class="ingredient-row">
                                                <div><select class="form-select" name="ingredientId" aria-label="Nguyên liệu"><option value="">Chọn nguyên liệu</option><c:forEach items="${ingredients}" var="ingredient"><c:if test="${ingredient.active or ingredient.id eq recipeIngredient.ingredientId}"><option value="${ingredient.id}" data-unit="${fn:escapeXml(ingredient.unit)}" data-unit-cost="${ingredientUnitCosts[ingredient.id]}" ${ingredient.id eq recipeIngredient.ingredientId ? 'selected' : ''}><c:out value="${ingredient.name}" /> (<c:out value="${ingredient.unit}" />)<c:if test="${not ingredient.active}"> · Ngừng sử dụng (dòng cũ)</c:if></option></c:if></c:forEach></select><div class="ingredient-unit-cost"></div></div>
                                                <div class="ingredient-quantity"><input class="form-control" type="number" name="quantity" min="0.001" max="999999999.999" step="0.001" value="${fn:escapeXml(recipeIngredient.quantity)}" placeholder="0.001" aria-label="Định lượng"><div class="quantity-unit"></div></div>
                                                <div class="ingredient-line-cost">—</div>
                                                <button type="button" class="btn btn-sm btn-light text-danger remove-ingredient" aria-label="Xóa nguyên liệu">×</button>
                                            </div>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="ingredient-row">
                                            <div><select class="form-select" name="ingredientId" aria-label="Nguyên liệu"><option value="">Chọn nguyên liệu</option><c:forEach items="${ingredients}" var="ingredient"><c:if test="${ingredient.active}"><option value="${ingredient.id}" data-unit="${fn:escapeXml(ingredient.unit)}" data-unit-cost="${ingredientUnitCosts[ingredient.id]}"><c:out value="${ingredient.name}" /> (<c:out value="${ingredient.unit}" />)</option></c:if></c:forEach></select><div class="ingredient-unit-cost"></div></div>
                                            <div class="ingredient-quantity"><input class="form-control" type="number" name="quantity" min="0.001" max="999999999.999" step="0.001" placeholder="0.001" aria-label="Định lượng"><div class="quantity-unit"></div></div>
                                            <div class="ingredient-line-cost">—</div>
                                            <button type="button" class="btn btn-sm btn-light text-danger remove-ingredient" aria-label="Xóa nguyên liệu">×</button>
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                            <template id="ingredientRowTemplate"><div class="ingredient-row"><div><select class="form-select" name="ingredientId" aria-label="Nguyên liệu"><option value="">Chọn nguyên liệu</option><c:forEach items="${ingredients}" var="ingredient"><c:if test="${ingredient.active}"><option value="${ingredient.id}" data-unit="${fn:escapeXml(ingredient.unit)}" data-unit-cost="${ingredientUnitCosts[ingredient.id]}"><c:out value="${ingredient.name}" /> (<c:out value="${ingredient.unit}" />)</option></c:if></c:forEach></select><div class="ingredient-unit-cost"></div></div><div class="ingredient-quantity"><input class="form-control" type="number" name="quantity" min="0.001" max="999999999.999" step="0.001" placeholder="0.001" aria-label="Định lượng"><div class="quantity-unit"></div></div><div class="ingredient-line-cost">—</div><button type="button" class="btn btn-sm btn-light text-danger remove-ingredient" aria-label="Xóa nguyên liệu">×</button></div></template>
                        </c:otherwise>
                    </c:choose>
                    <div class="recipe-cost-card" id="recipeCostCard">
                            <div><span class="text-muted small fw-bold">GIÁ VỐN NGUYÊN LIỆU / PHẦN</span><strong id="recipeCostValue"><c:choose><c:when test="${recipeCostComplete}"><fmt:formatNumber value="${recipeCost}" pattern="#,##0.##" />đ</c:when><c:when test="${recipeHasIngredients}"><fmt:formatNumber value="${recipeCost}" pattern="#,##0.##" />đ · thiếu giá</c:when><c:otherwise>Chưa có định lượng</c:otherwise></c:choose></strong><small id="recipeCostNote"><c:choose><c:when test="${not empty missingCostIngredients}">Chưa có giá nhập đã nhận: <c:forEach items="${missingCostIngredients}" var="missingName" varStatus="loop"><c:if test="${loop.index gt 0}">, </c:if><c:out value="${missingName}" /></c:forEach>.</c:when><c:otherwise>Đơn giá của lần nhập đã nhận gần nhất; chưa gồm bao bì, nhân công, hao hụt.</c:otherwise></c:choose></small></div>
                    </div>
                    <div class="d-flex justify-content-end gap-2 mt-3"><a href="${ctx}/admin/recipes" class="btn btn-light">Hủy</a><button type="submit" class="btn btn-aura fw-bold" <c:if test="${empty ingredients}">disabled</c:if>>Lưu công thức</button></div>
                </form>
            </div>
        </section>
    </div>
</c:if>

<script>
(() => {
    const filter = document.getElementById('recipeFilter');
    filter?.addEventListener('change', () => {
        document.querySelectorAll('.recipe-card').forEach(card => {
            const hasRecipe = card.dataset.hasRecipe === 'true';
            card.hidden = filter.value === 'with' ? !hasRecipe : filter.value === 'without' ? hasRecipe : false;
        });
    });

    const rows = document.getElementById('ingredientRows');
    const template = document.getElementById('ingredientRowTemplate');
    const costValue = document.getElementById('recipeCostValue');
    const costNote = document.getElementById('recipeCostNote');
    const money = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });

    function recalculateCost() {
        // BF11: chỉ cập nhật số tiền xem trước từ lượng nhập và giá đã nạp; server tính lại khi tải trang.
        if (!rows || !costValue) return;
        let total = 0;
        let hasLine = false;
        let complete = true;
        rows.querySelectorAll('.ingredient-row').forEach(row => {
            const select = row.querySelector('select[name="ingredientId"]');
            const quantityInput = row.querySelector('input[name="quantity"]');
            const selectedOption = select?.selectedOptions[0];
            const rawQuantity = quantityInput?.value.trim() || '';
            const amount = Number(rawQuantity);
            const unitCost = Number(selectedOption?.dataset.unitCost);
            const unit = selectedOption?.dataset.unit || '';
            const hasSelection = Boolean(select?.value);
            const unitCostLabel = row.querySelector('.ingredient-unit-cost');
            const quantityUnitLabel = row.querySelector('.quantity-unit');
            const lineCostLabel = row.querySelector('.ingredient-line-cost');
            if (quantityUnitLabel) quantityUnitLabel.textContent = hasSelection ? 'Đơn vị: ' + unit : '';
            if (unitCostLabel) unitCostLabel.textContent = hasSelection ? (Number.isFinite(unitCost) ? 'Giá nhập mới nhất: ' + money.format(unitCost) + 'đ/' + unit : 'Chưa có giá nhập đã nhận') : '';
            if (!hasSelection && !rawQuantity) { if (lineCostLabel) lineCostLabel.textContent = '—'; return; }
            hasLine = true;
            if (!hasSelection || !rawQuantity || !Number.isFinite(amount) || amount <= 0 || !Number.isFinite(unitCost)) {
                complete = false;
                if (lineCostLabel) lineCostLabel.textContent = Number.isFinite(unitCost) ? 'Nhập lượng' : 'Thiếu giá';
                return;
            }
            const lineCost = amount * unitCost;
            total += lineCost;
            if (lineCostLabel) lineCostLabel.textContent = money.format(lineCost) + 'đ';
        });
        if (!hasLine) {
            costValue.textContent = 'Chưa có định lượng';
            costNote.textContent = 'Thêm nguyên liệu và định lượng để ước tính giá vốn.';
            return;
        }
        costValue.textContent = complete ? money.format(total) + 'đ' : money.format(total) + 'đ · chưa đủ giá';
        costNote.textContent = complete ? 'Đơn giá của lần nhập đã nhận gần nhất; chưa gồm bao bì, nhân công, hao hụt.' : 'Có nguyên liệu thiếu định lượng hoặc chưa có phiếu nhập đã nhận; số tiền là phần đã tính được.';
    }

    document.getElementById('addIngredient')?.addEventListener('click', () => { if (rows && template) rows.appendChild(template.content.cloneNode(true)); });
    rows?.addEventListener('input', recalculateCost);
    rows?.addEventListener('change', recalculateCost);
    rows?.addEventListener('click', event => {
        const button = event.target.closest('.remove-ingredient');
        if (button) { button.closest('.ingredient-row')?.remove(); recalculateCost(); }
    });
    recalculateCost();
    document.getElementById('recipeFormError')?.focus();
})();
</script>
</body>
</html>
