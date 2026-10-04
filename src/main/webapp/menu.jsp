<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
            <!DOCTYPE html>
            <html lang="vi">

            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>Aura Café | Menu & Đồ Uống</title>

                <!-- Google Fonts & Icons -->
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link
                    href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Inter:wght@400;500;600;700;800&display=swap"
                    rel="stylesheet" />
                <link
                    href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200"
                    rel="stylesheet" />
                <!-- Bootstrap 5 CSS -->
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">

                <style>
                    :root {
                        --primary: #8b1528;
                        --primary-hover: #70101f;
                        --primary-soft: #fdf2f4;
                        --surface: #ffffff;
                        --bg-body: #f8fafc;
                        --text-main: #0f172a;
                        --text-muted: #64748b;
                        --border-color: #f1f5f9;
                    }

                    body {
                        font-family: 'Plus Jakarta Sans', 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        -webkit-font-smoothing: antialiased;
                        -moz-osx-font-smoothing: grayscale;
                        background-color: var(--bg-body);
                        color: var(--text-main);
                        overflow-x: hidden;
                        letter-spacing: -0.01em;
                        padding-bottom: 80px;
                        /* Space for Sticky Bottom Cart */
                    }

                    /* Menu Search Box */
                    .menu-search-container {
                        position: relative;
                        width: 100%;
                        max-width: 380px;
                    }

                    .menu-search-input {
                        background-color: #ffffff;
                        border: 1.5px solid #e2e8f0;
                        border-radius: 999px;
                        padding: 0.6rem 2.8rem 0.6rem 2.8rem;
                        font-size: 0.9rem;
                        transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
                        color: var(--text-main);
                    }

                    .menu-search-input:focus {
                        background-color: #ffffff;
                        border-color: var(--primary);
                        box-shadow: 0 0 0 3.5px rgba(139, 21, 40, 0.12);
                        outline: none;
                    }

                    .menu-search-icon {
                        position: absolute;
                        left: 1rem;
                        top: 50%;
                        transform: translateY(-50%);
                        color: #94a3b8;
                        pointer-events: none;
                    }

                    .menu-search-btn-clear,
                    .menu-search-btn-submit {
                        position: absolute;
                        right: 0.5rem;
                        top: 50%;
                        transform: translateY(-50%);
                        border: none;
                        background: transparent;
                        color: #94a3b8;
                        padding: 0.25rem 0.5rem;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        border-radius: 50%;
                        cursor: pointer;
                        transition: color 0.18s;
                        text-decoration: none;
                    }

                    .menu-search-btn-clear:hover {
                        color: #e11d48;
                    }

                    .menu-search-btn-submit:hover {
                        color: var(--primary);
                    }

                    /* Category Filter Pills */
                    .category-filter-bar {
                        display: flex;
                        align-items: center;
                        gap: 0.6rem;
                        overflow-x: auto;
                        padding: 0.5rem 0.25rem 0.75rem 0.25rem;
                        scrollbar-width: none;
                    }

                    .category-filter-bar::-webkit-scrollbar {
                        display: none;
                    }

                    .filter-pill {
                        display: inline-flex;
                        align-items: center;
                        gap: 0.55rem;
                        padding: 0.6rem 1.25rem;
                        border-radius: 999px;
                        font-size: 0.9rem;
                        font-weight: 600;
                        color: #334155;
                        background: #ffffff;
                        border: 1.5px solid #e2e8f0;
                        text-decoration: none;
                        white-space: nowrap;
                        box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
                        transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
                    }

                    .filter-pill .material-symbols-outlined {
                        font-size: 1.2rem;
                        color: #64748b;
                        transition: color 0.2s, transform 0.2s;
                    }

                    .filter-pill:hover {
                        color: var(--primary);
                        background-color: #ffffff;
                        border-color: rgba(139, 21, 40, 0.35);
                        transform: translateY(-2px);
                        box-shadow: 0 6px 16px rgba(139, 21, 40, 0.1);
                    }

                    .filter-pill:hover .material-symbols-outlined {
                        color: var(--primary);
                        transform: scale(1.1);
                    }

                    .filter-pill.active {
                        background: linear-gradient(135deg, #8b1528 0%, #a81c31 100%);
                        color: #ffffff !important;
                        border-color: transparent;
                        font-weight: 700;
                        transform: translateY(-1px);
                        box-shadow: 0 6px 18px rgba(139, 21, 40, 0.32);
                    }

                    .filter-pill.active .material-symbols-outlined {
                        color: #ffffff !important;
                    }

                    /* Minimalist Modern Product Card */
                    .product-card {
                        background: #ffffff;
                        border: 1px solid #eef2f6;
                        border-radius: 1.25rem;
                        overflow: hidden;
                        transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
                        display: flex;
                        flex-direction: column;
                        cursor: pointer;
                        position: relative;
                    }

                    .product-card:hover {
                        transform: translateY(-4px);
                        border-color: #cbd5e1;
                        box-shadow: 0 12px 28px -6px rgba(15, 23, 42, 0.08), 0 4px 10px -2px rgba(15, 23, 42, 0.03);
                    }

                    /* Image Box */
                    .product-img-box {
                        position: relative;
                        width: 100%;
                        height: 210px;
                        background-color: #f1f5f9;
                        overflow: hidden;
                    }

                    .product-img-box img {
                        width: 100%;
                        height: 100%;
                        object-fit: cover;
                        transition: transform 0.45s cubic-bezier(0.16, 1, 0.3, 1);
                    }

                    .product-card:hover .product-img-box img {
                        transform: scale(1.05);
                    }

                    /* Clean Badge */
                    .pill-badge {
                        position: absolute;
                        top: 12px;
                        left: 12px;
                        padding: 4px 10px;
                        font-size: 0.7rem;
                        font-weight: 700;
                        letter-spacing: 0.3px;
                        text-transform: uppercase;
                        border-radius: 999px;
                        z-index: 2;
                    }

                    .pill-badge-hot {
                        background-color: rgba(225, 29, 72, 0.95);
                        color: #ffffff;
                    }

                    .pill-badge-new {
                        background-color: rgba(14, 165, 233, 0.95);
                        color: #ffffff;
                    }

                    /* Card Content */
                    .product-card-body {
                        padding: 1.15rem 1.25rem;
                        display: flex;
                        flex-direction: column;
                        flex-grow: 1;
                    }

                    .product-meta-subtitle {
                        font-size: 0.72rem;
                        font-weight: 600;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        color: #94a3b8;
                        margin-bottom: 0.25rem;
                    }

                    .product-name {
                        font-size: 1.02rem;
                        font-weight: 700;
                        color: var(--text-main);
                        margin-bottom: 0.35rem;
                        line-height: 1.35;
                        transition: color 0.2s;
                    }

                    .product-card:hover .product-name {
                        color: var(--primary);
                    }

                    .product-brief {
                        font-size: 0.8rem;
                        color: #64748b;
                        line-height: 1.45;
                        display: -webkit-box;
                        -webkit-line-clamp: 2;
                        line-clamp: 2;
                        -webkit-box-orient: vertical;
                        overflow: hidden;
                        min-height: 2.3rem;
                        margin-bottom: 1rem;
                    }

                    /* Card Footer */
                    .product-card-footer {
                        margin-top: auto;
                        padding-top: 0.75rem;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                    }

                    .product-price {
                        font-size: 1.15rem;
                        font-weight: 800;
                        color: var(--primary);
                    }

                    /* 1-Click Quick Add Button */
                    .btn-add-circle {
                        width: 40px;
                        height: 40px;
                        border-radius: 50%;
                        background-color: #ffffff;
                        color: var(--primary);
                        border: 1.5px solid rgba(139, 21, 40, 0.22);
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        box-shadow: 0 2px 6px rgba(139, 21, 40, 0.06);
                        transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
                        cursor: pointer;
                    }

                    .btn-add-circle:hover {
                        background: linear-gradient(135deg, #8b1528 0%, #a81c31 100%);
                        color: #ffffff;
                        transform: scale(1.12) rotate(90deg);
                        border-color: transparent;
                        box-shadow: 0 6px 18px rgba(139, 21, 40, 0.32);
                    }

                    /* Sticky Bottom Mini Cart Bar */
                    .sticky-cart-bar {
                        position: fixed;
                        bottom: 24px;
                        left: 50%;
                        transform: translateX(-50%);
                        background: rgba(15, 23, 42, 0.94);
                        backdrop-filter: blur(16px);
                        -webkit-backdrop-filter: blur(16px);
                        color: #ffffff;
                        padding: 0.65rem 1.35rem;
                        border-radius: 999px;
                        border: 1px solid rgba(255, 255, 255, 0.12);
                        box-shadow: 0 16px 40px rgba(0, 0, 0, 0.35);
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        gap: 1.75rem;
                        z-index: 1040;
                        min-width: 360px;
                        max-width: 90vw;
                        animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
                        transition: transform 0.2s ease, box-shadow 0.2s ease;
                    }

                    .sticky-cart-bar:hover {
                        transform: translate(-50%, -2px);
                        box-shadow: 0 20px 48px rgba(0, 0, 0, 0.45);
                    }

                    @keyframes slideUp {
                        from {
                            transform: translate(-50%, 40px);
                            opacity: 0;
                        }

                        to {
                            transform: translate(-50%, 0);
                            opacity: 1;
                        }
                    }

                    .cart-badge-icon {
                        position: relative;
                        width: 40px;
                        height: 40px;
                        background: rgba(255, 255, 255, 0.12);
                        border-radius: 50%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        color: #ffffff;
                    }

                    .badge-count-pill {
                        position: absolute;
                        top: -4px;
                        right: -4px;
                        background-color: #e11d48;
                        color: #ffffff;
                        font-size: 0.7rem;
                        font-weight: 700;
                        padding: 1px 6px;
                        border-radius: 999px;
                    }

                    /* Customization Modal Styling */
                    .modal-product-img {
                        width: 100%;
                        height: 280px;
                        object-fit: cover;
                        border-radius: 1rem;
                    }

                    .option-chip {
                        cursor: pointer;
                        border: 1px solid #e2e8f0;
                        border-radius: 999px;
                        padding: 0.45rem 1rem;
                        font-size: 0.85rem;
                        font-weight: 500;
                        transition: all 0.18s;
                        background: #ffffff;
                    }

                    .option-chip.selected {
                        border-color: var(--primary);
                        background-color: var(--primary-soft);
                        color: var(--primary);
                        font-weight: 600;
                    }

                    .qty-control-btn {
                        width: 36px;
                        height: 36px;
                        border-radius: 50%;
                        border: 1px solid #cbd5e1;
                        background: #ffffff;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-weight: 600;
                        transition: all 0.18s;
                    }

                    .qty-control-btn:hover {
                        background-color: #f1f5f9;
                    }
                </style>
            </head>

            <body>

                <!-- Shared Header Navbar Component -->
                <jsp:include page="/components/header.jsp">
                    <jsp:param name="activePage" value="menu" />
                </jsp:include>

                <!-- Main Content -->
                <main class="container-fluid px-4 px-lg-5 py-4">

                    <!-- Top Title & Search Bar -->
                    <div class="d-flex align-items-center justify-content-between flex-wrap gap-3 mb-4">
                        <div>
                            <h1 class="h3 fw-bold mb-1" style="color: #0f172a;">
                                <c:choose>
                                    <c:when test="${not empty currentKeyword}">
                                        Kết quả tìm kiếm cho "<span class="text-danger">${currentKeyword}</span>"
                                    </c:when>
                                    <c:when test="${not empty currentCategory}">
                                        <c:forEach var="c" items="${categories}">
                                            <c:if test="${c.id == currentCategory}">${c.name}</c:if>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        Thực đơn Aura Café
                                    </c:otherwise>
                                </c:choose>
                                <span class="text-muted fs-6 fw-normal ms-2">(${products.size()} món)</span>
                            </h1>
                            <p class="text-muted small mb-0">Hương vị cà phê đặc sản và trà hảo hạng pha chế thủ công.
                            </p>
                        </div>

                        <!-- Menu Search Box -->
                        <div class="menu-search-container flex-grow-1 flex-md-grow-0">
                            <form action="${pageContext.request.contextPath}/menu" method="GET"
                                class="position-relative w-100">
                                <c:if test="${not empty currentCategory}">
                                    <input type="hidden" name="category" value="${currentCategory}">
                                </c:if>
                                <span class="material-symbols-outlined menu-search-icon fs-5">search</span>
                                <input type="search" name="keyword"
                                    value="${not empty currentKeyword ? currentKeyword : ''}"
                                    class="form-control menu-search-input shadow-none"
                                    placeholder="Tìm tên món, trà, cà phê..." autocomplete="off">
                                <c:choose>
                                    <c:when test="${not empty currentKeyword}">
                                        <a href="${pageContext.request.contextPath}/menu${not empty currentCategory ? '?category=' : ''}${currentCategory}"
                                            class="menu-search-btn-clear" title="Xoá tìm kiếm">
                                            <span class="material-symbols-outlined fs-5">close</span>
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        <button type="submit" class="menu-search-btn-submit" title="Tìm kiếm">
                                            <span class="material-symbols-outlined fs-5">arrow_forward</span>
                                        </button>
                                    </c:otherwise>
                                </c:choose>
                            </form>
                        </div>
                    </div>

                    <!-- Active Filter Indicator (if any) -->
                    <c:if test="${not empty currentCategory || not empty currentKeyword}">
                        <div class="d-flex align-items-center gap-2 mb-3 flex-wrap">
                            <span class="text-muted small">Đang lọc:</span>
                            <c:if test="${not empty currentCategory}">
                                <span
                                    class="badge bg-white text-dark border px-2.5 py-1.5 rounded-pill d-inline-flex align-items-center gap-1 shadow-sm"
                                    style="font-weight: 500;">
                                    <span>Danh mục: <c:forEach var="c" items="${categories}">
                                            <c:if test="${c.id == currentCategory}">${c.name}</c:if>
                                        </c:forEach></span>
                                    <a href="${pageContext.request.contextPath}/menu${not empty currentKeyword ? '?keyword=' : ''}${currentKeyword}"
                                        class="text-muted text-decoration-none ms-1">
                                        <span class="material-symbols-outlined" style="font-size: 15px;">close</span>
                                    </a>
                                </span>
                            </c:if>
                            <c:if test="${not empty currentKeyword}">
                                <span
                                    class="badge bg-white text-dark border px-2.5 py-1.5 rounded-pill d-inline-flex align-items-center gap-1 shadow-sm"
                                    style="font-weight: 500;">
                                    <span>Từ khóa: "${currentKeyword}"</span>
                                    <a href="${pageContext.request.contextPath}/menu${not empty currentCategory ? '?category=' : ''}${currentCategory}"
                                        class="text-muted text-decoration-none ms-1">
                                        <span class="material-symbols-outlined" style="font-size: 15px;">close</span>
                                    </a>
                                </span>
                            </c:if>
                            <a href="${pageContext.request.contextPath}/menu"
                                class="btn btn-sm btn-link text-danger text-decoration-none p-0 ms-2 small fw-semibold">
                                Xoá tất cả
                            </a>
                        </div>
                    </c:if>

                    <!-- Minimalist Category Filter Pills Bar -->
                    <div class="category-filter-bar mb-4">
                        <!-- All -->
                        <a href="${pageContext.request.contextPath}/menu${not empty currentKeyword ? '?keyword=' : ''}${currentKeyword}"
                            class="filter-pill ${empty currentCategory ? 'active' : ''}">
                            <span class="material-symbols-outlined fs-5">apps</span>
                            <span>Tất cả</span>
                        </a>

                        <!-- Dynamic Categories -->
                        <c:forEach var="cat" items="${categories}">
                            <c:url var="catUrl" value="/menu">
                                <c:param name="category" value="${cat.id}" />
                                <c:if test="${not empty currentKeyword}">
                                    <c:param name="keyword" value="${currentKeyword}" />
                                </c:if>
                            </c:url>
                            <a href="${catUrl}" class="filter-pill ${currentCategory == cat.id ? 'active' : ''}">
                                <span class="material-symbols-outlined fs-5">${not empty cat.icon ? cat.icon : 'local_cafe'}</span>
                                <span>${cat.name}</span>
                            </a>
                        </c:forEach>
                    </div>

                    <!-- Products Grid (4 columns desktop, 2 tablet, 1 mobile) -->
                    <c:choose>
                        <c:when test="${not empty products}">
                            <div class="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-lg-4 g-4" id="productsGrid">
                                <c:forEach var="p" items="${products}">
                                    <div class="col">
                                        <!-- Card: Bấm vào ảnh/tên chuyển sang trang chi tiết của DonVQ, nút + mở modal thêm nhanh -->
                                        <div class="product-card h-100"
                                            onclick="window.location.href='${pageContext.request.contextPath}/product-detail?id=${p.id}'">
                                            <!-- Image Box -->
                                            <div class="product-img-box">
                                                <c:if test="${not empty p.badge}">
                                                    <span
                                                        class="pill-badge ${p.badge == 'BÁN CHẠY' ? 'pill-badge-hot' : 'pill-badge-new'}">
                                                        ${p.badge}
                                                    </span>
                                                </c:if>
                                                <img src="${p.imageUrl}" alt="${p.name}" loading="lazy"
                                                    onerror="this.src='https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80'">
                                            </div>

                                            <!-- Body -->
                                            <div class="product-card-body">
                                                <div class="product-meta-subtitle">
                                                    ${not empty p.subtitle ? p.subtitle : p.categoryName}
                                                </div>
                                                <h2 class="product-name" title="${p.name}">
                                                    <a href="${pageContext.request.contextPath}/product-detail?id=${p.id}"
                                                        class="text-decoration-none text-dark"
                                                        onclick="event.stopPropagation()">
                                                        ${p.name}
                                                    </a>
                                                </h2>
                                                <p class="product-brief" title="${p.description}">
                                                    ${not empty p.description ? p.description : 'Đồ uống tinh hoa được tuyển chọn từ vùng nguyên liệu đặc sản.'}
                                                </p>

                                                <!-- Price & Add Button -->
                                                <div class="product-card-footer">
                                                    <span class="product-price">${p.formattedPrice}</span>
                                                    <button type="button" class="btn-add-circle"
                                                        title="Tùy chọn & Thêm món"
                                                        onclick="event.stopPropagation(); openProductDetailModal('${p.id}')">
                                                        <span class="material-symbols-outlined fs-5">add</span>
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <!-- Clean Empty State -->
                            <div class="text-center py-5 my-4 bg-white rounded-4 border p-5">
                                <span class="material-symbols-outlined text-secondary mb-3"
                                    style="font-size: 3rem;">search_off</span>
                                <h3 class="h5 fw-bold mb-2">Không tìm thấy món phù hợp</h3>
                                <p class="text-muted small mb-4">Vui lòng thử tìm với từ khoá khác hoặc xem toàn bộ danh
                                    mục thực đơn.</p>
                                <a href="${pageContext.request.contextPath}/menu"
                                    class="btn btn-danger btn-sm px-4 py-2 rounded-pill fw-semibold">
                                    Xem tất cả món
                                </a>
                            </div>
                        </c:otherwise>
                    </c:choose>

                </main>

                <!-- Sticky Bottom Selection Bar (Hiện khi đang chọn món - Bấm Xem món ra popup) -->
                <div id="stickyMiniCart" class="sticky-cart-bar d-none" onclick="openSelectedItemsModal()"
                    style="cursor: pointer;" title="Bấm để xem danh sách món">
                    <div class="d-flex align-items-center gap-3">
                        <div class="cart-badge-icon">
                            <span class="material-symbols-outlined fs-5">shopping_bag</span>
                            <span id="stickyCartBadge" class="badge-count-pill">0</span>
                        </div>
                        <div class="fw-bold text-white" style="font-size: 0.95rem;">
                            Đang chọn <span id="stickyCartCount" class="text-danger fw-bold">0</span> món
                        </div>
                    </div>
                    <button type="button"
                        class="btn btn-danger rounded-pill px-4 py-2 fw-semibold d-flex align-items-center gap-2 shadow-sm"
                        onclick="event.stopPropagation(); openSelectedItemsModal()" title="Xem danh sách món đang chọn">
                        <span>Xem món</span>
                        <span class="material-symbols-outlined fs-6">arrow_forward</span>
                    </button>
                </div>

                <!-- Product Customization Modal (Mở khi bấm vào đồ uống để đổi size/topping) -->
                <div class="modal fade" id="productDetailModal" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-centered modal-lg">
                        <div class="modal-content border-0 shadow-lg position-relative"
                            style="border-radius: 1.5rem; overflow: hidden;">
                            <!-- Close Button (Always on top with high z-index) -->
                            <button type="button" class="btn-close position-absolute top-0 end-0 m-3 shadow-none"
                                style="z-index: 1060; cursor: pointer; padding: 0.75rem;" data-bs-dismiss="modal"
                                aria-label="Close" onclick="closeProductDetailModal()"></button>
                            <div class="modal-body p-4 pt-3">
                                <div class="row g-4 align-items-center">
                                    <!-- Left: Image -->
                                    <div class="col-md-5">
                                        <img id="modalProductImg" src="" alt="Product"
                                            class="modal-product-img shadow-sm">
                                    </div>

                                    <!-- Right: Details & Options -->
                                    <div class="col-md-7 d-flex flex-column">
                                        <span class="text-uppercase fw-bold text-danger"
                                            style="font-size: 0.75rem; letter-spacing: 0.5px;"
                                            id="modalProductCategory">Danh mục</span>
                                        <h2 class="fs-4 fw-bold mb-1" id="modalProductName">Tên món</h2>
                                        <p class="text-muted small mb-3" id="modalProductDesc">Mô tả sản phẩm</p>

                                        <div class="d-flex align-items-baseline gap-2 mb-3 pb-2 border-bottom">
                                            <span class="fs-3 fw-bold text-danger" id="modalProductTotalPrice">0đ</span>
                                            <span class="text-muted small">/ Giá tiêu chuẩn</span>
                                        </div>

                                        <!-- Sizes -->
                                        <div class="mb-3" id="sizeOptionsContainer">
                                            <label class="form-label fw-bold small text-muted mb-2">CHỌN SIZE (KÍCH
                                                CỠ):</label>
                                            <div class="d-flex gap-2 flex-wrap" id="sizeList"></div>
                                        </div>

                                        <!-- Toppings -->
                                        <div class="mb-3" id="toppingOptionsContainer">
                                            <label class="form-label fw-bold small text-muted mb-2">TOPPING
                                                THÊM:</label>
                                            <div class="d-flex flex-column gap-2" id="toppingList"></div>
                                        </div>

                                        <!-- Quantity & Action -->
                                        <div
                                            class="mt-3 pt-3 border-top d-flex align-items-center justify-content-between gap-3">
                                            <div class="d-flex align-items-center gap-2 border rounded-pill px-2 py-1">
                                                <button type="button" class="qty-control-btn border-0"
                                                    onclick="adjustModalQty(-1)">-</button>
                                                <span id="modalProductQty" class="fw-bold px-2"
                                                    style="min-width: 24px; text-align: center;">1</span>
                                                <button type="button" class="qty-control-btn border-0"
                                                    onclick="adjustModalQty(1)">+</button>
                                            </div>

                                            <button type="button"
                                                class="btn btn-danger flex-grow-1 py-2 fw-semibold d-flex align-items-center justify-content-center gap-2 rounded-pill shadow-sm"
                                                onclick="confirmAddToCart()">
                                                <span class="material-symbols-outlined fs-5">add_circle</span>
                                                <span>Chọn món này &bull; <span id="modalBtnTotalPrice">0đ</span></span>
                                            </button>
                                        </div>

                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Selected Items Preview Modal (Món đang chọn - Giao diện hiện đại, sang trọng) -->
                <div class="modal fade" id="selectedItemsModal" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-centered" style="max-width: 490px;">
                        <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden bg-white">
                            <div class="modal-header border-0 pb-2 pt-4 px-4 d-flex align-items-center justify-content-between">
                                <div>
                                    <div class="d-flex align-items-center gap-2">
                                        <h5 class="modal-title fw-bold text-dark mb-0" style="font-size: 1.15rem; letter-spacing: -0.01em;">Món đang chọn</h5>
                                        <span id="selectedModalBadge" class="badge rounded-pill bg-danger-subtle text-danger fw-bold" style="font-size: 0.75rem;">0 món</span>
                                    </div>
                                    <p class="text-muted small mb-0 mt-1" style="font-size: 0.8rem;">Các món bạn vừa chọn tại Aura Café</p>
                                </div>
                                <div class="d-flex align-items-center gap-2">
                                    <button type="button" class="btn btn-sm btn-link text-danger text-decoration-none p-0 small fw-semibold" style="font-size: 0.8rem;" onclick="clearAllCart()">
                                        Xoá tất cả
                                    </button>
                                    <button type="button" class="btn btn-sm btn-light rounded-circle p-1 d-flex align-items-center justify-content-center text-muted border-0 shadow-none" 
                                            style="width: 34px; height: 34px; transition: background 0.2s;" data-bs-dismiss="modal" aria-label="Close">
                                        <span class="material-symbols-outlined fs-5">close</span>
                                    </button>
                                </div>
                            </div>
                            <div class="modal-body px-4 py-3" style="overflow-x: hidden !important;">
                                <div id="cartEmptyState" class="text-center py-5">
                                    <div class="bg-light rounded-circle p-3 d-inline-flex align-items-center justify-content-center text-muted mb-3" style="width: 60px; height: 60px;">
                                        <span class="material-symbols-outlined fs-2">local_cafe</span>
                                    </div>
                                    <h6 class="fw-bold text-dark mb-1">Chưa có món nào được chọn</h6>
                                    <p class="text-muted small mb-3">Hãy chọn các món yêu thích từ thực đơn bên dưới nhé.</p>
                                    <button type="button" class="btn btn-sm btn-outline-danger rounded-pill px-4 py-1.5 fw-semibold" data-bs-dismiss="modal">
                                        Xem thực đơn
                                    </button>
                                </div>
                                <div id="cartItemList" class="d-flex flex-column gap-2 mb-3" style="max-height: 330px; overflow-y: auto; overflow-x: hidden !important; padding: 2px 2px 2px 0;"></div>
                                <div id="cartSummary" class="border-top pt-3 d-none">
                                    <div class="d-flex align-items-center justify-content-between mb-3 px-1">
                                        <span class="text-muted small fw-medium">Tổng tiền tạm tính:</span>
                                        <span id="selectedModalTotalPrice" class="fw-bold text-danger fs-5">0đ</span>
                                    </div>
                                    <a href="${pageContext.request.contextPath}/cart"
                                        class="btn btn-danger w-100 py-2.5 fw-bold rounded-pill d-flex align-items-center justify-content-center gap-2 shadow-sm text-decoration-none"
                                        style="background: linear-gradient(135deg, #8b1528 0%, #a81c31 100%); font-size: 0.95rem; box-shadow: 0 6px 20px rgba(139, 21, 40, 0.28) !important;">
                                        <span>Xem giỏ hàng</span>
                                        <span class="material-symbols-outlined fs-5">arrow_forward</span>
                                    </a>
                                    <button type="button" class="btn btn-link text-muted small text-decoration-none w-100 mt-2 py-0 d-block text-center fw-medium" data-bs-dismiss="modal">
                                        + Chọn thêm món khác
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Toast Notification -->
                <div class="toast-container position-fixed bottom-0 start-50 translate-middle-x p-3 z-3">
                    <div id="cartToast"
                        class="toast align-items-center text-white bg-dark border-0 shadow-lg rounded-pill" role="alert"
                        aria-live="assertive" aria-atomic="true">
                        <div class="d-flex px-3 py-2 align-items-center">
                            <span class="material-symbols-outlined text-success me-2 fs-5">check_circle</span>
                            <div class="toast-body p-0 fw-medium small" id="cartToastMessage">
                                Đã thêm món vào giỏ hàng thành công!
                            </div>
                            <button type="button" class="btn-close btn-close-white ms-auto me-1"
                                data-bs-dismiss="toast"></button>
                        </div>
                    </div>
                </div>

                <!-- Include Auth Modal -->
                <jsp:include page="/components/auth_modal.jsp" />

                <!-- Bootstrap 5 JS Bundle -->
                <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

                <script>
                    var currentProduct = null;
                    var selectedSizePrice = 0;
                    var modalQty = 1;
                    var cart = JSON.parse(localStorage.getItem('aura_cart') || '[]');
                    // Tự động dọn dẹp các món lỗi font/ký tự ? cũ từ localStorage
                    cart = cart.filter(function(item) {
                        return item && item.name && item.name.indexOf('?') === -1;
                    });
                    localStorage.setItem('aura_cart', JSON.stringify(cart));

                    function updateCartBadge() {
                        var totalItems = 0;
                        var totalPrice = 0;
                        for (var i = 0; i < cart.length; i++) {
                            var q = (cart[i].quantity || 1);
                            totalItems += q;
                            totalPrice += (cart[i].subtotal || (cart[i].unitPrice * q));
                        }

                        // Header badge
                        var headerBadge = document.getElementById('headerCartBadge');
                        if (headerBadge) headerBadge.innerText = totalItems;

                        // Sticky Bottom Selection Bar
                        var stickyCart = document.getElementById('stickyMiniCart');
                        if (stickyCart) {
                            if (totalItems > 0) {
                                stickyCart.classList.remove('d-none');
                                var countEl = document.getElementById('stickyCartCount');
                                if (countEl) countEl.innerText = totalItems;
                                var badgeEl = document.getElementById('stickyCartBadge');
                                if (badgeEl) badgeEl.innerText = totalItems;
                            } else {
                                stickyCart.classList.add('d-none');
                            }
                        }

                        localStorage.setItem('aura_cart', JSON.stringify(cart));
                    }

                    document.addEventListener('DOMContentLoaded', function () {
                        updateCartBadge();
                    });

                    // 1-Click Quick Add: Bấm nút + thêm ngay vào giỏ hàng với Size M / Tiêu chuẩn, không popup
                    function quickAddToCart(event, id, name, price, imageUrl, categoryId) {
                        if (event) {
                            event.stopPropagation();
                        }

                        var defaultSize = (categoryId === 5 || categoryId === 6) ? 'Tiêu chuẩn' : 'M';

                        // Tìm xem món này đã có trong giỏ với cấu hình mặc định chưa
                        var existingIndex = -1;
                        for (var i = 0; i < cart.length; i++) {
                            if (cart[i].id === id && cart[i].size === defaultSize && (!cart[i].toppings || cart[i].toppings.length === 0)) {
                                existingIndex = i;
                                break;
                            }
                        }

                        if (existingIndex > -1) {
                            cart[existingIndex].quantity += 1;
                            cart[existingIndex].subtotal = cart[existingIndex].unitPrice * cart[existingIndex].quantity;
                        } else {
                            cart.push({
                                id: id,
                                name: name,
                                imageUrl: imageUrl,
                                size: defaultSize,
                                toppings: [],
                                unitPrice: price,
                                quantity: 1,
                                subtotal: price
                            });
                        }

                        updateCartBadge();

                        // Hiển thị Toast thông báo nhanh
                        document.getElementById('cartToastMessage').innerText = 'Đã thêm nhanh ' + name + ' vào giỏ!';
                        var toast = new bootstrap.Toast(document.getElementById('cartToast'));
                        toast.show();
                    }

                    // Xử lý khi bấm vào Card
                    function handleCardClick(id, name, price, imageUrl, categoryId) {
                        // Category 5 (Bánh ngọt) & 6 (Đóng chai): không có size/topping -> thêm ngay, không mở popup
                        if (categoryId === 5 || categoryId === 6) {
                            quickAddToCart(null, id, name, price, imageUrl, categoryId);
                        } else {
                            // Đồ uống: mở popup để khách chọn đổi size L, thêm topping
                            openProductDetailModal(id);
                        }
                    }

                    // Mở Modal tùy biến chi tiết
                    function openProductDetailModal(productId) {
                        fetch('${pageContext.request.contextPath}/menu?action=detail&id=' + productId)
                            .then(function (res) {
                                if (!res.ok) {
                                    throw new Error('Mạng hoặc máy chủ phản hồi lỗi: ' + res.status);
                                }
                                return res.json();
                            })
                            .then(function (data) {
                                if (data.error) {
                                    alert(data.error);
                                    return;
                                }
                                currentProduct = data;
                                modalQty = 1;
                                selectedSizePrice = 0;

                                var imgEl = document.getElementById('modalProductImg');
                                if (imgEl) imgEl.src = data.imageUrl || 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80';
                                
                                var nameEl = document.getElementById('modalProductName');
                                if (nameEl) nameEl.innerText = data.name || '';
                                
                                var catEl = document.getElementById('modalProductCategory');
                                if (catEl) catEl.innerText = data.categoryName || '';
                                
                                var descEl = document.getElementById('modalProductDesc');
                                if (descEl) descEl.innerText = data.description || '';
                                
                                var qtyEl = document.getElementById('modalProductQty');
                                if (qtyEl) qtyEl.innerText = 1;

                                var config = data.variantsConfig || {};
                                if (typeof config === 'string') {
                                    try {
                                        config = JSON.parse(config);
                                    } catch (e) {
                                        config = {};
                                    }
                                }
                                var sizes = config.sizes;
                                if (!sizes || typeof sizes !== 'object' || Object.keys(sizes).length === 0) {
                                    sizes = { "M": 0, "L": 10000 };
                                }
                                var toppings = Array.isArray(config.toppings) ? config.toppings : [];

                                // Sizes với Smart Default (Size đầu tiên tự chọn)
                                var sizeListHtml = '';
                                var sizeKeys = Object.keys(sizes);
                                sizeKeys.forEach(function (sName, index) {
                                    var extraPrice = sizes[sName] || 0;
                                    var isFirst = index === 0;
                                    if (isFirst) selectedSizePrice = extraPrice;
                                    sizeListHtml += '<div class="option-chip ' + (isFirst ? 'selected' : '') + '" onclick="selectSize(this, ' + extraPrice + ')">'
                                        + sName + (extraPrice > 0 ? ' (+' + extraPrice.toLocaleString('vi-VN') + 'đ)' : '')
                                        + '</div>';
                                });
                                var sizeListEl = document.getElementById('sizeList');
                                if (sizeListEl) sizeListEl.innerHTML = sizeListHtml;

                                // Toppings
                                var toppingContainer = document.getElementById('toppingOptionsContainer');
                                var toppingListEl = document.getElementById('toppingList');
                                if (toppings.length > 0) {
                                    if (toppingContainer) toppingContainer.classList.remove('d-none');
                                    var toppingListHtml = '';
                                    toppings.forEach(function (t, idx) {
                                        var tName = typeof t === 'string' ? t : t.name;
                                        var tPrice = typeof t === 'object' && t.price ? t.price : 8000;
                                        toppingListHtml += '<div class="form-check d-flex align-items-center justify-content-between p-2 border rounded-3">'
                                            + '  <div>'
                                            + '    <input class="form-check-input ms-0 me-2" type="checkbox" id="top_' + idx + '" value="' + tPrice + '" onchange="calculateModalTotal()">'
                                            + '    <label class="form-check-label small fw-medium" for="top_' + idx + '">' + tName + '</label>'
                                            + '  </div>'
                                            + '  <span class="text-danger fw-bold small">+' + tPrice.toLocaleString('vi-VN') + 'đ</span>'
                                            + '</div>';
                                    });
                                    if (toppingListEl) toppingListEl.innerHTML = toppingListHtml;
                                } else {
                                    if (toppingContainer) toppingContainer.classList.add('d-none');
                                    if (toppingListEl) toppingListEl.innerHTML = '';
                                }

                                calculateModalTotal();

                                var modalEl = document.getElementById('productDetailModal');
                                if (modalEl) {
                                    var myModal = bootstrap.Modal.getOrCreateInstance(modalEl);
                                    myModal.show();
                                }
                            })
                            .catch(function (err) {
                                console.error('Error loading product:', err);
                                alert('Không thể tải thông tin món. Vui lòng thử lại!');
                            });
                    }

                    // Đóng Modal tùy biến
                    function closeProductDetailModal() {
                        var modalEl = document.getElementById('productDetailModal');
                        if (modalEl) {
                            var modalInstance = bootstrap.Modal.getInstance(modalEl) || bootstrap.Modal.getOrCreateInstance(modalEl);
                            if (modalInstance) {
                                modalInstance.hide();
                            }
                        }
                    }

                    function selectSize(element, extraPrice) {
                        var chips = document.querySelectorAll('#sizeList .option-chip');
                        chips.forEach(function (c) { c.classList.remove('selected'); });
                        element.classList.add('selected');
                        selectedSizePrice = extraPrice;
                        calculateModalTotal();
                    }

                    function adjustModalQty(delta) {
                        modalQty = Math.max(1, modalQty + delta);
                        var qtyEl = document.getElementById('modalProductQty');
                        if (qtyEl) qtyEl.innerText = modalQty;
                        calculateModalTotal();
                    }

                    function calculateModalTotal() {
                        if (!currentProduct) return;
                        var checkboxes = document.querySelectorAll('#toppingList input[type="checkbox"]:checked');
                        var topPrice = 0;
                        checkboxes.forEach(function (cb) {
                            topPrice += parseFloat(cb.value) || 0;
                        });
                        var base = parseFloat(currentProduct.basePrice) || 0;
                        var singleItemPrice = base + (selectedSizePrice || 0) + topPrice;
                        var total = singleItemPrice * modalQty;

                        var formatted = total.toLocaleString('vi-VN') + 'đ';
                        var totalEl = document.getElementById('modalProductTotalPrice');
                        if (totalEl) totalEl.innerText = formatted;
                        var btnPriceEl = document.getElementById('modalBtnTotalPrice');
                        if (btnPriceEl) btnPriceEl.innerText = formatted;
                    }

                    function confirmAddToCart() {
                        if (!currentProduct) return;

                        var activeSize = document.querySelector('#sizeList .option-chip.selected');
                        var sizeName = activeSize ? activeSize.innerText.split(' ')[0] : 'Tiêu chuẩn';

                        var toppingNames = [];
                        var checkboxes = document.querySelectorAll('#toppingList input[type="checkbox"]:checked');
                        checkboxes.forEach(function (cb) {
                            var label = cb.parentElement.querySelector('label');
                            if (label) toppingNames.push(label.innerText);
                        });

                        var base = parseFloat(currentProduct.basePrice) || 0;
                        var singlePrice = base + (selectedSizePrice || 0);
                        checkboxes.forEach(function (cb) { singlePrice += parseFloat(cb.value) || 0; });

                        cart.push({
                            id: currentProduct.id,
                            name: currentProduct.name,
                            imageUrl: currentProduct.imageUrl,
                            size: sizeName,
                            toppings: toppingNames,
                            unitPrice: singlePrice,
                            quantity: modalQty,
                            subtotal: singlePrice * modalQty
                        });

                        updateCartBadge();

                        var modalEl = document.getElementById('productDetailModal');
                        if (modalEl) {
                            var modalInstance = bootstrap.Modal.getInstance(modalEl);
                            if (modalInstance) modalInstance.hide();
                        }

                        var toastMsg = document.getElementById('cartToastMessage');
                        if (toastMsg) {
                            toastMsg.innerText = 'Đã thêm ' + modalQty + 'x ' + currentProduct.name + ' vào danh sách chọn!';
                        }
                        var toastEl = document.getElementById('cartToast');
                        if (toastEl && window.bootstrap) {
                            var toast = bootstrap.Toast.getOrCreateInstance(toastEl);
                            toast.show();
                        }
                    }

                    function changeItemQty(index, delta) {
                        if (!cart[index]) return;
                        var newQty = (cart[index].quantity || 1) + delta;
                        if (newQty <= 0) {
                            cart.splice(index, 1);
                        } else {
                            cart[index].quantity = newQty;
                            cart[index].subtotal = cart[index].unitPrice * newQty;
                        }
                        updateCartBadge();
                        openSelectedItemsModal();
                    }

                    function clearAllCart() {
                        cart = [];
                        localStorage.setItem('aura_cart', JSON.stringify(cart));
                        updateCartBadge();
                        openSelectedItemsModal();
                    }

                    function openSelectedItemsModal() {
                        // Tự động dọn dẹp các món lỗi font/ký tự ? cũ từ localStorage
                        cart = cart.filter(function(item) {
                            return item && item.name && item.name.indexOf('?') === -1;
                        });
                        localStorage.setItem('aura_cart', JSON.stringify(cart));

                        var body = document.getElementById('cartItemList');
                        var empty = document.getElementById('cartEmptyState');
                        var summary = document.getElementById('cartSummary');
                        var badge = document.getElementById('selectedModalBadge');
                        var totalPriceEl = document.getElementById('selectedModalTotalPrice');

                        var totalItems = 0;
                        var totalPrice = 0;
                        cart.forEach(function (item) {
                            var q = (item.quantity || 1);
                            totalItems += q;
                            totalPrice += (item.subtotal || (item.unitPrice * q));
                        });

                        if (badge) {
                            badge.innerText = totalItems + ' món';
                        }
                        if (totalPriceEl) {
                            totalPriceEl.innerText = totalPrice.toLocaleString('vi-VN') + 'đ';
                        }

                        if (cart.length === 0) {
                            body.innerHTML = '';
                            empty.classList.remove('d-none');
                            summary.classList.add('d-none');
                        } else {
                            empty.classList.add('d-none');
                            summary.classList.remove('d-none');

                            var html = '';
                            cart.forEach(function (item, idx) {
                                var toppingsText = (item.toppings && item.toppings.length > 0) ? item.toppings.join(', ') : '';
                                var itemPriceFormatted = ((item.unitPrice || 0) * (item.quantity || 1)).toLocaleString('vi-VN') + 'đ';

                                html += '<div class="p-2.5 px-3 rounded-4 bg-light border border-light-subtle d-flex align-items-center justify-content-between gap-2.5" style="transition: all 0.2s ease;">'
                                    + '  <div class="d-flex align-items-center gap-2.5 min-w-0 flex-grow-1" style="overflow: hidden;">'
                                    + '    <img src="' + item.imageUrl + '" style="width: 48px; height: 48px; object-fit: cover; border-radius: 12px;" class="border bg-white shadow-2xs flex-shrink-0" onerror="this.src=\'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80\'">'
                                    + '    <div class="min-w-0 flex-grow-1" style="overflow: hidden;">'
                                    + '      <div class="fw-bold text-dark text-truncate mb-0.5" style="font-size: 0.92rem;" title="' + item.name + '">' + item.name + '</div>'
                                    + '      <div class="d-flex align-items-center gap-1.5 flex-wrap">'
                                    + '        <span class="badge bg-danger-subtle text-danger rounded-pill" style="font-size: 0.68rem; font-weight: 700; padding: 2px 7px;">Size ' + item.size + '</span>'
                                    + (toppingsText ? '        <span class="text-muted small text-truncate" style="font-size: 0.72rem; max-width: 140px;">+ ' + toppingsText + '</span>' : '')
                                    + '      </div>'
                                    + '      <div class="text-danger fw-bold mt-1" style="font-size: 0.84rem;">' + itemPriceFormatted + '</div>'
                                    + '    </div>'
                                    + '  </div>'
                                    + '  <div class="d-flex align-items-center gap-2 flex-shrink-0">'
                                    + '    <div class="d-flex align-items-center border rounded-pill bg-white px-1.5 py-0.5 shadow-2xs">'
                                    + '      <button type="button" class="btn btn-sm btn-link p-0 text-muted" style="width: 22px; height: 22px; text-decoration: none; font-weight: bold; line-height: 1;" onclick="changeItemQty(' + idx + ', -1)">-</button>'
                                    + '      <span class="px-1.5 fw-bold text-dark small" style="min-width: 18px; text-align: center;">' + item.quantity + '</span>'
                                    + '      <button type="button" class="btn btn-sm btn-link p-0 text-muted" style="width: 22px; height: 22px; text-decoration: none; font-weight: bold; line-height: 1;" onclick="changeItemQty(' + idx + ', 1)">+</button>'
                                    + '    </div>'
                                    + '    <button class="btn btn-sm btn-light rounded-circle p-1 d-flex align-items-center justify-content-center text-muted border-0" style="width: 30px; height: 30px;" title="Xoá món" onclick="removeFromCart(' + idx + ')">'
                                    + '      <span class="material-symbols-outlined" style="font-size: 18px;">delete_outline</span>'
                                    + '    </button>'
                                    + '  </div>'
                                    + '</div>';
                            });
                            body.innerHTML = html;
                        }

                        var modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('selectedItemsModal'));
                        modal.show();
                    }

                    var openCartModal = openSelectedItemsModal;

                    function removeFromCart(index) {
                        cart.splice(index, 1);
                        updateCartBadge();
                        openSelectedItemsModal();
                    }
                </script>
            </body>

            </html>