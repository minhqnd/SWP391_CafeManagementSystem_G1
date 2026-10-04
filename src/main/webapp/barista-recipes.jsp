<%@ page contentType="text/html;charset=UTF-8"
          pageEncoding="UTF-8"
          language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${empty requestScope.baristaRecipes}">
    <c:redirect url="/barista?action=recipes"/>
</c:if>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Aura Café - Sổ Tay Công Thức Pha Chế (SOP)</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">

    <style>
        :root {
            --aura-red: #900b1a;
            --aura-red-dark: #710916;
            --aura-border: #e7e5e4;
            --aura-muted: #78716c;
        }

        body {
            margin: 0;
            background: #f5f5f4;
            color: #292524;
            font-family: 'Inter', sans-serif;
            font-size: 14px;
        }

        .btn-aura {
            --bs-btn-color: #fff;
            --bs-btn-bg: var(--aura-red);
            --bs-btn-border-color: var(--aura-red);
            --bs-btn-hover-color: #fff;
            --bs-btn-hover-bg: var(--aura-red-dark);
            --bs-btn-hover-border-color: var(--aura-red-dark);
        }

        .toolbar {
            background: #fff;
            border-bottom: 1px solid var(--aura-border);
        }

        .filter-button {
            padding: 8px 16px;
            border-radius: 9999px;
            background: #f5f5f4;
            color: #57534e;
            border: 1px solid var(--aura-border);
            font-size: 13px;
            font-weight: 600;
            transition: all 0.2s ease;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }

        .filter-button:hover {
            background: #e7e5e4;
            color: #292524;
        }

        .filter-button.active {
            background: var(--aura-red);
            color: #fff;
            border-color: var(--aura-red);
        }

        .recipe-card {
            background: #fff;
            border: 1px solid var(--aura-border);
            border-radius: 16px;
            overflow: hidden;
            transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
        }

        .recipe-card:hover {
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
            border-color: #d6d3d1;
        }

        .recipe-card.highlighted-product {
            border: 2px solid var(--aura-red) !important;
            box-shadow: 0 0 0 4px rgba(144, 11, 26, 0.18), 0 8px 24px rgba(144, 11, 26, 0.12) !important;
            animation: pulse-glow 2s infinite;
        }

        @keyframes pulse-glow {
            0% {
                box-shadow: 0 0 0 0 rgba(144, 11, 26, 0.4);
            }
            70% {
                box-shadow: 0 0 0 10px rgba(144, 11, 26, 0);
            }
            100% {
                box-shadow: 0 0 0 0 rgba(144, 11, 26, 0);
            }
        }

        .bom-table th {
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: #78716c;
            background: #fafaf9;
            padding: 8px 12px;
            font-weight: 700;
        }

        .bom-table td {
            padding: 9px 12px;
            font-size: 13px;
            vertical-align: middle;
        }

        .instruction-box {
            background: #fffdfa;
            border: 1px solid #fef3c7;
            border-left: 4px solid #d97706;
            border-radius: 10px;
            padding: 12px 14px;
            font-size: 13px;
            line-height: 1.6;
            color: #451a03;
            white-space: pre-line;
        }

        .search-box {
            position: relative;
            max-width: 380px;
        }

        .search-box input {
            padding-left: 38px;
            border-radius: 12px;
            border: 1px solid var(--aura-border);
            font-size: 13px;
        }

        .search-box .material-symbols-outlined {
            position: absolute;
            left: 10px;
            top: 50%;
            transform: translateY(-50%);
            color: #a8a29e;
            pointer-events: none;
        }
    </style>
</head>

<body data-context-path="${pageContext.request.contextPath}">

<!-- Shared Header with Barista navigation -->
<jsp:include page="/components/header.jsp">
    <jsp:param name="navbarMode" value="barista"/>
    <jsp:param name="showCart" value="false"/>
    <jsp:param name="activePage" value="barista-recipes"/>
</jsp:include>

<!-- HEADER TOOLBAR: TITLE & CONTROLS -->
<section class="toolbar px-3 px-xl-4 py-3" aria-label="Thanh công cụ công thức">
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3">
        <div>
            <div class="d-flex align-items-center gap-2">
                <span class="material-symbols-outlined text-danger fs-4">menu_book</span>
                <h1 class="h5 mb-0 fw-bold text-dark">Sổ Tay Công Thức Pha Chế &amp; Định Lượng Kỹ Thuật (SOP)</h1>
            </div>
            <p class="text-muted small mb-0 mt-1">
                Tra cứu nhanh thành phần BOM và các bước thao tác chuẩn khi nhận đơn từ Barista KDS.
            </p>
        </div>

        <div class="d-flex align-items-center gap-2">
            <div class="search-box">
                <span class="material-symbols-outlined fs-5">search</span>
                <input type="text" id="recipeSearchInput" class="form-control" placeholder="Tìm theo tên món hoặc nguyên liệu... (Phím /)">
            </div>
            <a href="${pageContext.request.contextPath}/barista" class="btn btn-outline-danger d-inline-flex align-items-center gap-1 rounded-3 px-3 py-2 fw-semibold" title="Quay lại danh sách đơn hàng (F1)">
                <span class="material-symbols-outlined fs-5">receipt_long</span>
                <span>Đơn pha chế (F1)</span>
            </a>
        </div>
    </div>

    <!-- CATEGORY FILTER PILLS -->
    <div class="d-flex flex-wrap align-items-center gap-2 mt-3 pt-2 border-top">
        <span class="small fw-bold text-muted me-1">Danh mục:</span>
        <button type="button" class="filter-button active" data-category="ALL" onclick="filterByCategory('ALL', this)">
            Tất cả (${products.size()})
        </button>
        <c:forEach items="${categories}" var="cat">
            <button type="button" class="filter-button" data-category="${cat.id}" onclick="filterByCategory('${cat.id}', this)">
                ${cat.name}
            </button>
        </c:forEach>
    </div>
</section>

<!-- MAIN CONTENT: RECIPES GRID -->
<main class="container-fluid px-3 px-xl-4 py-4">
    <c:if test="${not empty selectedProductId}">
        <div id="selectedNotice" class="alert alert-warning alert-dismissible fade show d-flex align-items-center gap-2 rounded-3 mb-4" role="alert">
            <span class="material-symbols-outlined text-warning-emphasis">info</span>
            <div>
                Bạn đang tra cứu công thức cho sản phẩm được chọn từ đơn pha chế.
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <div class="row g-4" id="recipesGrid">
        <c:forEach items="${products}" var="p">
            <div class="col-12 col-lg-6 recipe-card-wrapper" data-category="${p.categoryId}" data-product-id="${p.id}" data-search-text="${p.name.toLowerCase()} ${p.categoryName.toLowerCase()}">
                <article class="recipe-card h-100 d-flex flex-column ${selectedProductId == p.id ? 'highlighted-product' : ''}" id="recipe-card-${p.id}">
                    <!-- Card Header: Image & Product Info -->
                    <div class="p-3 border-bottom d-flex align-items-center gap-3 bg-white">
                        <c:choose>
                            <c:when test="${not empty p.imageUrl}">
                                <img src="${p.imageUrl}" alt="${p.name}" class="rounded-3 border object-fit-cover flex-shrink-0" style="width: 72px; height: 72px;">
                            </c:when>
                            <c:otherwise>
                                <div class="rounded-3 bg-light border d-flex align-items-center justify-content-center flex-shrink-0" style="width: 72px; height: 72px;">
                                    <span class="material-symbols-outlined text-secondary fs-2">local_cafe</span>
                                </div>
                            </c:otherwise>
                        </c:choose>

                        <div class="flex-grow-1 min-width-0">
                            <div class="d-flex align-items-center justify-content-between gap-2">
                                <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-2 py-1" style="font-size: 11px;">
                                    ${p.categoryName}
                                </span>
                                <span class="text-muted small">Mã món: #${p.id}</span>
                            </div>
                            <h2 class="h6 fw-bold text-dark mb-1 mt-1 text-truncate" title="${p.name}">
                                ${p.name}
                            </h2>
                            <div class="text-muted small">
                                Giá niêm yết: <strong class="text-danger">${p.formattedPrice}</strong>
                                <c:if test="${not empty p.subtitle}">
                                    <span class="mx-1">•</span> <span>${p.subtitle}</span>
                                </c:if>
                            </div>
                        </div>
                    </div>

                    <!-- Card Body: BOM & Instructions -->
                    <div class="p-3 d-flex flex-column gap-3 flex-grow-1">
                        <!-- 1. BẢNG ĐỊNH LƯỢNG KỸ THUẬT (BOM) -->
                        <div>
                            <div class="d-flex align-items-center justify-content-between mb-2">
                                <span class="fw-bold text-dark d-flex align-items-center gap-1" style="font-size: 13px;">
                                    <span class="material-symbols-outlined text-secondary fs-6">inventory_2</span>
                                    Bảng định lượng nguyên liệu (BOM)
                                </span>
                                <span class="badge bg-light text-muted border" style="font-size: 11px;">
                                    ${p.ingredients.size()} thành phần
                                </span>
                            </div>

                            <c:choose>
                                <c:when test="${not empty p.ingredients}">
                                    <div class="border rounded-3 overflow-hidden">
                                        <table class="table table-sm table-striped mb-0 bom-table">
                                            <thead>
                                                <tr>
                                                    <th>Nguyên liệu</th>
                                                    <th class="text-end">Định lượng chuẩn</th>
                                                    <th class="text-center w-25">Đơn vị</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <c:forEach items="${p.ingredients}" var="ing">
                                                    <tr>
                                                        <td class="fw-medium text-dark">${ing.ingredientName}</td>
                                                        <td class="text-end fw-bold text-danger font-monospace">
                                                            ${ing.quantityRequired}
                                                        </td>
                                                        <td class="text-center text-muted small">${ing.unit}</td>
                                                    </tr>
                                                </c:forEach>
                                            </tbody>
                                        </table>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="p-3 bg-light rounded-3 text-center text-muted small border">
                                        Chưa thiết lập bảng định lượng cho món này.
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <!-- 2. QUY TRÌNH & HƯỚNG DẪN PHA CHẾ (SOP) -->
                        <div class="mt-auto">
                            <span class="fw-bold text-dark d-flex align-items-center gap-1 mb-2" style="font-size: 13px;">
                                <span class="material-symbols-outlined text-warning fs-6">emoji_objects</span>
                                Quy trình pha chế chuẩn (SOP)
                            </span>

                            <c:set var="instructionsText" value="${p.recipeInstructions}" />
                            <c:choose>
                                <c:when test="${not empty instructionsText}">
                                    <div class="instruction-box">
                                        <c:out value="${instructionsText}"/>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="p-3 bg-light rounded-3 text-muted small border">
                                        <span class="fst-italic">Theo quy trình pha chế tiêu chuẩn của Barista Aura Café cho nhóm món <strong>${p.categoryName}</strong>.</span>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </article>
            </div>
        </c:forEach>
    </div>

    <!-- Empty search result state -->
    <div id="noResultsState" class="text-center py-5 d-none">
        <span class="material-symbols-outlined text-muted" style="font-size: 56px;">search_off</span>
        <h3 class="h6 fw-bold text-dark mt-3">Không tìm thấy công thức phù hợp</h3>
        <p class="text-muted small">Thử tìm kiếm với từ khóa khác hoặc chuyển danh mục về "Tất cả".</p>
        <button type="button" class="btn btn-sm btn-outline-danger mt-2" onclick="resetFilters()">
            Xóa bộ lọc
        </button>
    </div>
</main>

<script>
    let activeCategory = 'ALL';
    const searchInput = document.getElementById('recipeSearchInput');
    const wrappers = document.querySelectorAll('.recipe-card-wrapper');
    const noResults = document.getElementById('noResultsState');

    function filterByCategory(categoryId, btn) {
        activeCategory = categoryId;
        document.querySelectorAll('.filter-button').forEach(b => b.classList.remove('active'));
        if (btn) btn.classList.add('active');
        applyFilters();
    }

    function applyFilters() {
        const query = (searchInput.value || '').trim().toLowerCase();
        let visibleCount = 0;

        wrappers.forEach(wrapper => {
            const matchesCategory = (activeCategory === 'ALL' || wrapper.getAttribute('data-category') === activeCategory);
            const searchText = wrapper.getAttribute('data-search-text') || '';
            const matchesQuery = query === '' || searchText.includes(query);

            if (matchesCategory && matchesQuery) {
                wrapper.classList.remove('d-none');
                visibleCount++;
            } else {
                wrapper.classList.add('d-none');
            }
        });

        if (visibleCount === 0) {
            noResults.classList.remove('d-none');
        } else {
            noResults.classList.add('d-none');
        }
    }

    function resetFilters() {
        searchInput.value = '';
        filterByCategory('ALL', document.querySelector('.filter-button[data-category="ALL"]'));
    }

    searchInput.addEventListener('input', applyFilters);

    // Keyboard Shortcuts: F1 (Orders), / (Search), Esc (Clear)
    document.addEventListener('keydown', event => {
        if (event.key === 'F1') {
            event.preventDefault();
            window.location.assign('${pageContext.request.contextPath}/barista');
        } else if (event.key === '/' && document.activeElement !== searchInput) {
            event.preventDefault();
            searchInput.focus();
            searchInput.select();
        } else if (event.key === 'Escape' && document.activeElement === searchInput) {
            searchInput.value = '';
            applyFilters();
            searchInput.blur();
        }
    });

    // Auto scroll & highlight product if selectedProductId is passed
    window.addEventListener('DOMContentLoaded', () => {
        const highlightedCard = document.querySelector('.recipe-card.highlighted-product');
        if (highlightedCard) {
            setTimeout(() => {
                highlightedCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }, 300);
        }
    });
</script>

</body>
</html>
