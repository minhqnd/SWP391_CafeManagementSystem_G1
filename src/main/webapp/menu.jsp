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
                <link
                    href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Inter:wght@400;500;600;700&display=swap"
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
                        font-family: 'Plus Jakarta Sans', 'Inter', sans-serif;
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
                        gap: 0.45rem;
                        padding: 0.55rem 1.15rem;
                        border-radius: 999px;
                        font-size: 0.88rem;
                        font-weight: 500;
                        color: #475569;
                        background-color: #ffffff;
                        border: 1px solid #e2e8f0;
                        text-decoration: none;
                        white-space: nowrap;
                        transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
                    }

                    .filter-pill:hover {
                        color: var(--primary);
                        background-color: var(--primary-soft);
                        border-color: rgba(139, 21, 40, 0.25);
                    }

                    .filter-pill.active {
                        background-color: var(--primary);
                        color: #ffffff !important;
                        border-color: var(--primary);
                        font-weight: 600;
                        box-shadow: 0 4px 14px rgba(139, 21, 40, 0.22);
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
                        width: 38px;
                        height: 38px;
                        border-radius: 50%;
                        background-color: var(--primary-soft);
                        color: var(--primary);
                        border: 1px solid rgba(139, 21, 40, 0.15);
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        transition: all 0.2s ease;
                    }

                    .btn-add-circle:hover {
                        background-color: var(--primary);
                        color: #ffffff;
                        transform: scale(1.1);
                        border-color: var(--primary);
                        box-shadow: 0 4px 12px rgba(139, 21, 40, 0.3);
                    }

                    /* Sticky Bottom Mini Cart Bar */
                    .sticky-cart-bar {
                        position: fixed;
                        bottom: 24px;
                        left: 50%;
                        transform: translateX(-50%);
                        background: #0f172a;
                        color: #ffffff;
                        padding: 0.65rem 1.25rem;
                        border-radius: 999px;
                        box-shadow: 0 12px 36px rgba(15, 23, 42, 0.35);
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        gap: 1.75rem;
                        z-index: 1040;
                        min-width: 350px;
                        max-width: 90vw;
                        animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
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
                                <span class="material-symbols-outlined fs-5">${not empty cat.icon ? cat.icon :
                                    'local_cafe'}</span>
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
                                                    ${not empty p.description ? p.description : 'Đồ uống tinh hoa được
                                                    tuyển chọn từ vùng nguyên liệu đặc sản.'}
                                                </p>

                                                <!-- Price & Link to Detail -->
                                                <div class="product-card-footer">
                                                    <span class="product-price">${p.formattedPrice}</span>
                                                    <a href="${pageContext.request.contextPath}/product-detail?id=${p.id}"
                                                        class="btn-add-circle text-decoration-none"
                                                        title="Xem chi tiết món"
                                                        onclick="event.stopPropagation()">
                                                        <span class="material-symbols-outlined fs-5">arrow_forward</span>
                                                    </a>
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

                <!-- Selected Items Preview Modal (Món đang chọn - Giao diện hiện đại, sang trọng) -->
                <div class="modal fade" id="selectedItemsModal" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-centered" style="max-width: 480px;">
                        <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden bg-white">
                            <div class="modal-header border-0 pb-2 pt-4 px-4 d-flex align-items-center justify-content-between">
                                <div>
                                    <div class="d-flex align-items-center gap-2">
                                        <h5 class="modal-title fw-bold text-dark mb-0" style="font-size: 1.15rem; letter-spacing: -0.01em;">Món đang chọn</h5>
                                        <span id="selectedModalBadge" class="badge rounded-pill bg-danger-subtle text-danger fw-bold" style="font-size: 0.75rem;">0 món</span>
                                    </div>
                                    <p class="text-muted small mb-0 mt-1" style="font-size: 0.8rem;">Các món bạn vừa chọn tại Aura Café</p>
                                </div>
                                <button type="button" class="btn btn-sm btn-light rounded-circle p-1 d-flex align-items-center justify-content-center text-muted border-0 shadow-none" 
                                        style="width: 34px; height: 34px; transition: background 0.2s;" data-bs-dismiss="modal" aria-label="Close">
                                    <span class="material-symbols-outlined fs-5">close</span>
                                </button>
                            </div>
                            <div class="modal-body px-4 py-3">
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
                                <div id="cartItemList" class="d-flex flex-column gap-2.5 mb-3" style="max-height: 320px; overflow-y: auto; padding-right: 2px;"></div>
                                <div id="cartSummary" class="border-top pt-3 d-none">
                                    <a href="${pageContext.request.contextPath}/cart"
                                        class="btn btn-danger w-100 py-2.5 fw-semibold rounded-pill d-flex align-items-center justify-content-center gap-2 shadow-sm text-decoration-none"
                                        style="background: linear-gradient(135deg, #8b1528 0%, #a81c31 100%); font-size: 0.95rem;">
                                        <span>Xem giỏ hàng</span>
                                        <span class="material-symbols-outlined fs-5">arrow_forward</span>
                                    </a>
                                    <button type="button" class="btn btn-link text-muted small text-decoration-none w-100 mt-2 py-0 d-block text-center" data-bs-dismiss="modal">
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
                    var cart = JSON.parse(localStorage.getItem('aura_cart') || '[]');

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

                    function openSelectedItemsModal() {
                        var body = document.getElementById('cartItemList');
                        var empty = document.getElementById('cartEmptyState');
                        var summary = document.getElementById('cartSummary');
                        var badge = document.getElementById('selectedModalBadge');

                        var totalItems = 0;
                        cart.forEach(function (item) {
                            totalItems += (item.quantity || 1);
                        });

                        if (badge) {
                            badge.innerText = totalItems + ' món';
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
                                html += '<div class="p-3 rounded-4 bg-light border d-flex align-items-center justify-content-between gap-3" style="transition: all 0.2s;">'
                                    + '  <div class="d-flex align-items-center gap-3 min-w-0">'
                                    + '    <img src="' + item.imageUrl + '" style="width: 52px; height: 52px; object-fit: cover; border-radius: 12px;" class="border bg-white shadow-xs flex-shrink-0" onerror="this.src=\'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80\'">'
                                    + '    <div class="min-w-0">'
                                    + '      <div class="fw-bold text-dark text-truncate mb-1" style="font-size: 0.94rem;">' + item.name + '</div>'
                                    + '      <div class="d-flex align-items-center gap-1.5 flex-wrap">'
                                    + '        <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill" style="font-size: 0.7rem; font-weight: 600;">Size ' + item.size + '</span>'
                                    + (toppingsText ? '        <span class="text-muted small text-truncate" style="font-size: 0.75rem; max-width: 170px;">+ ' + toppingsText + '</span>' : '')
                                    + '      </div>'
                                    + '    </div>'
                                    + '  </div>'
                                    + '  <div class="d-flex align-items-center gap-2 flex-shrink-0">'
                                    + '    <div class="d-flex align-items-center border rounded-pill bg-white px-1.5 py-0.5 shadow-2xs">'
                                    + '      <button type="button" class="btn btn-sm btn-link p-0 text-muted" style="width: 22px; height: 22px; text-decoration: none; font-weight: bold; line-height: 1;" onclick="changeItemQty(' + idx + ', -1)">-</button>'
                                    + '      <span class="px-2 fw-bold text-dark small" style="min-width: 18px; text-align: center;">' + item.quantity + '</span>'
                                    + '      <button type="button" class="btn btn-sm btn-link p-0 text-muted" style="width: 22px; height: 22px; text-decoration: none; font-weight: bold; line-height: 1;" onclick="changeItemQty(' + idx + ', 1)">+</button>'
                                    + '    </div>'
                                    + '    <button class="btn btn-sm btn-light rounded-circle p-1 d-flex align-items-center justify-content-center text-muted border-0" style="width: 32px; height: 32px;" title="Xoá món" onclick="removeFromCart(' + idx + ')">'
                                    + '      <span class="material-symbols-outlined fs-5">delete_outline</span>'
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