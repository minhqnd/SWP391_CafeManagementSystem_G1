<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Aura Café | Menu & Đồ Uống</title>

    <!-- Google Fonts & Icons -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet"/>
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet"/>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">

    <style>
        :root {
            --primary: #900b1a;
            --primary-dark: #6e0813;
            --primary-light: #fff0f1;
            --primary-container: #b2292e;
            --secondary: #555f71;
            --surface-bg: #f8f9fa;
            --card-border: #f0f0f0;
        }

        body {
            font-family: 'Inter', sans-serif;
            background-color: var(--surface-bg);
            color: #1a1c1e;
            overflow-x: hidden;
        }

        .text-primary-custom { color: var(--primary) !important; }
        .bg-primary-custom { background-color: var(--primary) !important; color: white !important; }

        /* Navbar Active Tab Indicator */
        .nav-link {
            color: #333;
            position: relative;
            padding: 0.5rem 0.75rem !important;
            transition: color 0.2s ease;
        }
        .nav-link:hover {
            color: var(--primary);
        }
        .nav-link.active-menu {
            color: var(--primary) !important;
            font-weight: 700;
        }
        .nav-link.active-menu::after {
            content: '';
            position: absolute;
            bottom: -8px;
            left: 10%;
            width: 80%;
            height: 3px;
            background-color: var(--primary);
            border-radius: 2px;
        }

        /* Search Bar */
        .search-container {
            position: relative;
            max-width: 480px;
            width: 100%;
        }
        .search-input {
            background-color: #f1f3f5;
            border: 1px solid transparent;
            border-radius: 999px;
            padding: 0.6rem 1rem 0.6rem 2.8rem;
            font-size: 0.9rem;
            transition: all 0.25s ease;
        }
        .search-input:focus {
            background-color: #ffffff;
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(144, 11, 26, 0.12);
        }
        .search-icon {
            position: absolute;
            left: 1rem;
            top: 50%;
            transform: translateY(-50%);
            color: #868e96;
            pointer-events: none;
        }

        /* Category Filter Tabs Bar */
        .category-scroll-container {
            background-color: #ffffff;
            border-radius: 1rem;
            padding: 0.85rem 1.25rem;
            box-shadow: 0 2px 8px rgba(0,0,0,0.03);
            border: 1px solid #eaeaea;
        }
        .cat-pill {
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            padding: 0.55rem 1.15rem;
            border-radius: 999px;
            font-size: 0.88rem;
            font-weight: 500;
            text-decoration: none;
            color: #495057;
            background-color: #f8f9fa;
            border: 1px solid #e9ecef;
            white-space: nowrap;
            transition: all 0.2s ease;
        }
        .cat-pill:hover {
            color: var(--primary);
            background-color: var(--primary-light);
            border-color: rgba(144, 11, 26, 0.2);
        }
        .cat-pill.active {
            background-color: var(--primary);
            color: #ffffff !important;
            border-color: var(--primary);
            font-weight: 600;
            box-shadow: 0 4px 12px rgba(144, 11, 26, 0.25);
        }
        .cat-pill .badge-count {
            background-color: rgba(255, 255, 255, 0.25);
            color: #ffffff;
            font-size: 0.72rem;
            padding: 2px 7px;
            border-radius: 999px;
        }
        .cat-pill:not(.active) .badge-count {
            background-color: #dee2e6;
            color: #495057;
        }

        /* Product Card */
        .product-card {
            background: #ffffff;
            border: 1px solid var(--card-border);
            border-radius: 1.25rem;
            overflow: hidden;
            transition: transform 0.3s cubic-bezier(0.165, 0.84, 0.44, 1), box-shadow 0.3s cubic-bezier(0.165, 0.84, 0.44, 1);
            display: flex;
            flex-direction: column;
        }
        .product-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 16px 30px rgba(0, 0, 0, 0.08);
            border-color: rgba(144, 11, 26, 0.15);
        }
        .product-thumb-wrapper {
            position: relative;
            width: 100%;
            height: 220px;
            overflow: hidden;
            background-color: #eaeaea;
        }
        .product-thumb {
            width: 100%;
            height: 100%;
            object-fit: cover;
            transition: transform 0.6s ease;
        }
        .product-card:hover .product-thumb {
            transform: scale(1.08);
        }

        /* Badges over Image */
        .badge-tag-top {
            position: absolute;
            top: 12px;
            left: 12px;
            padding: 4px 10px;
            font-size: 0.72rem;
            font-weight: 700;
            letter-spacing: 0.5px;
            text-transform: uppercase;
            border-radius: 6px;
            z-index: 2;
        }
        .badge-banchay {
            background-color: #900b1a;
            color: #ffffff;
        }
        .badge-monmoi {
            background-color: #0b7285;
            color: #ffffff;
        }
        .badge-tag-bottom {
            position: absolute;
            bottom: 12px;
            right: 12px;
            padding: 3px 9px;
            font-size: 0.72rem;
            font-weight: 600;
            background: rgba(20, 20, 20, 0.7);
            backdrop-filter: blur(4px);
            color: #ffffff;
            border-radius: 6px;
            z-index: 2;
        }

        /* Product Card Body */
        .card-body-content {
            padding: 1.25rem;
            display: flex;
            flex-direction: column;
            flex-grow: 1;
        }
        .product-title {
            font-size: 1.05rem;
            font-weight: 700;
            color: #1a1c1e;
            margin-bottom: 0.4rem;
            line-height: 1.35;
            transition: color 0.2s;
        }
        .product-card:hover .product-title {
            color: var(--primary);
        }
        .product-desc {
            font-size: 0.82rem;
            color: #6c757d;
            line-height: 1.45;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            text-overflow: ellipsis;
            min-height: 2.4rem;
            margin-bottom: 1.25rem;
        }

        /* Card Bottom Actions */
        .card-footer-box {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-top: auto;
            padding-top: 0.75rem;
            border-top: 1px dashed #f1f3f5;
        }
        .price-label {
            font-size: 0.72rem;
            color: #868e96;
            margin-bottom: 2px;
        }
        .price-amount {
            font-size: 1.15rem;
            font-weight: 800;
            color: #1a1c1e;
        }
        .btn-order-action {
            background-color: var(--primary);
            color: #ffffff;
            border: none;
            padding: 0.55rem 0.95rem;
            border-radius: 8px;
            font-size: 0.8rem;
            font-weight: 600;
            display: inline-flex;
            align-items: center;
            gap: 0.35rem;
            transition: all 0.2s ease;
            text-decoration: none;
            cursor: pointer;
        }
        .btn-order-action:hover {
            background-color: var(--primary-dark);
            color: #ffffff;
            transform: translateX(2px);
        }

        /* Pulse live indicator */
        .craft-indicator {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            font-size: 0.85rem;
            color: #495057;
            font-weight: 500;
        }
        .craft-dot {
            width: 8px;
            height: 8px;
            background-color: #d9480f;
            border-radius: 50%;
            animation: pulse-ring 2s infinite;
        }
        @keyframes pulse-ring {
            0% { transform: scale(0.95); opacity: 1; }
            50% { transform: scale(1.3); opacity: 0.6; }
            100% { transform: scale(0.95); opacity: 1; }
        }

        /* Quick View Modal Styles */
        .modal-product-img {
            width: 100%;
            height: 320px;
            object-fit: cover;
            border-radius: 1rem;
        }
        .option-chip {
            cursor: pointer;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 0.5rem 1rem;
            font-size: 0.88rem;
            transition: all 0.2s;
        }
        .option-chip.selected {
            border-color: var(--primary);
            background-color: var(--primary-light);
            color: var(--primary);
            font-weight: 600;
        }
        .qty-btn {
            width: 36px;
            height: 36px;
            border-radius: 8px;
            border: 1px solid #ced4da;
            background: #ffffff;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            transition: background-color 0.2s;
        }
        .qty-btn:hover {
            background-color: #e9ecef;
        }

        /* Floating Back To Top or Switch Mode button from screenshot */
        .floating-mode-switch {
            position: fixed;
            bottom: 24px;
            right: 24px;
            background-color: #1a1c1e;
            color: #ffffff;
            padding: 0.65rem 1.25rem;
            border-radius: 999px;
            box-shadow: 0 8px 24px rgba(0,0,0,0.2);
            font-size: 0.85rem;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 0.5rem;
            z-index: 1000;
            text-decoration: none;
            transition: transform 0.2s;
        }
        .floating-mode-switch:hover {
            transform: scale(1.05);
            color: #ffffff;
        }
    </style>
</head>
<body>

    <!-- Header Navbar -->
    <nav class="navbar navbar-expand-lg navbar-light bg-white shadow-sm py-3 sticky-top z-3">
        <div class="container-fluid px-4 px-lg-5">
            <!-- Brand Logo -->
            <a class="navbar-brand fw-bold text-danger d-flex align-items-center gap-2 me-4" href="${pageContext.request.contextPath}/">
                <div class="bg-danger text-white rounded-3 p-2 d-flex align-items-center justify-content-center" style="width: 40px; height: 40px;">
                    <span class="material-symbols-outlined fs-4">local_cafe</span>
                </div>
                <div class="d-flex flex-column">
                    <span class="fs-4 lh-1 text-dark fw-bold">Aura Café</span>
                    <span class="text-secondary fw-semibold" style="font-size: 0.65rem; letter-spacing: 1px; text-transform: uppercase;">Specialty Coffee</span>
                </div>
            </a>
            
            <button class="navbar-toggler border-0 shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#mainNavbar">
                <span class="material-symbols-outlined">menu</span>
            </button>

            <div class="collapse navbar-collapse" id="mainNavbar">
                <!-- Navigation Links -->
                <ul class="navbar-nav me-auto mb-2 mb-lg-0 gap-1 gap-lg-3 fw-medium" style="font-size: 0.95rem;">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/">Trang chủ</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link active-menu" href="${pageContext.request.contextPath}/menu">Menu & Đồ uống</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link text-muted" href="#" onclick="showFeatureNotice('Đơn hàng của tôi')">Đơn hàng của tôi</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link text-muted" href="#" onclick="showFeatureNotice('Hồ sơ & Điểm tích luỹ')">Hồ sơ & Điểm tích luỹ</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link text-muted" href="#" onclick="showFeatureNotice('Phản ánh & Khiếu nại')">Phản ánh & Khiếu nại</a>
                    </li>
                </ul>

                <!-- Live Search Bar -->
                <form action="${pageContext.request.contextPath}/menu" method="GET" class="d-flex mx-auto mx-lg-3 my-3 my-lg-0 flex-grow-1 search-container">
                    <c:if test="${not empty currentCategory}">
                        <input type="hidden" name="category" value="${currentCategory}">
                    </c:if>
                    <span class="material-symbols-outlined search-icon fs-5">search</span>
                    <input type="search" name="keyword" value="${currentKeyword}" 
                           class="form-control search-input shadow-none" 
                           id="menuSearchInput"
                           placeholder="Tìm kiếm đồ uống, bánh ngọt hoặc combo..."
                           autocomplete="off">
                </form>

                <!-- Right Actions: Cart & Profile -->
                <div class="d-flex align-items-center gap-3">
                    <!-- Cart Button -->
                    <a href="javascript:void(0)" onclick="openCartModal()" 
                       class="text-decoration-none text-dark d-flex align-items-center gap-2 px-3 py-2 rounded-pill bg-light border hover-shadow"
                       style="transition: background-color 0.2s;">
                        <span class="material-symbols-outlined fs-5 text-secondary">shopping_bag</span>
                        <span class="fw-semibold" style="font-size: 0.88rem;">Giỏ hàng</span>
                        <span id="headerCartBadge" class="badge rounded-pill bg-danger" style="font-size: 0.72rem;">0</span>
                    </a>

                    <!-- Profile / Auth Button -->
                    <c:choose>
                        <c:when test="${not empty sessionScope.user}">
                            <div class="dropdown">
                                <a href="#" class="d-flex align-items-center gap-2 text-decoration-none text-dark" data-bs-toggle="dropdown">
                                    <div class="rounded-circle bg-light d-flex align-items-center justify-content-center border" style="width: 40px; height: 40px;">
                                        <span class="material-symbols-outlined text-danger">account_circle</span>
                                    </div>
                                    <div class="d-none d-xl-flex flex-column text-start">
                                        <span class="fw-semibold text-truncate" style="font-size: 0.85rem; max-width: 120px;">${sessionScope.user.fullName}</span>
                                        <span class="text-danger fw-bold" style="font-size: 0.7rem;">${sessionScope.user.role}</span>
                                    </div>
                                </a>
                                <ul class="dropdown-menu dropdown-menu-end shadow border-0 mt-2">
                                    <li class="px-3 py-2 border-bottom">
                                        <div class="fw-bold">${sessionScope.user.fullName}</div>
                                        <div class="small text-muted">${sessionScope.user.phone}</div>
                                    </li>
                                    <li><a class="dropdown-item fw-medium py-2 text-danger" href="${pageContext.request.contextPath}/login?action=logout">
                                        <span class="material-symbols-outlined align-middle fs-6 me-1">logout</span> Đăng xuất
                                    </a></li>
                                </ul>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <button type="button" class="btn btn-outline-danger rounded-circle p-2 d-flex align-items-center justify-content-center" 
                                    style="width: 42px; height: 42px;" data-bs-toggle="modal" data-bs-target="#authModal" title="Đăng nhập / Đăng ký">
                                <span class="material-symbols-outlined fs-5">person</span>
                            </button>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </nav>

    <!-- Main Menu Content Container -->
    <div class="container-fluid px-4 px-lg-5 py-4">

        <!-- Category Filter Pills Bar -->
        <div class="category-scroll-container mb-4">
            <div class="d-flex align-items-center gap-2 flex-wrap">
                <!-- All categories -->
                <a href="${pageContext.request.contextPath}/menu${not empty currentKeyword ? '?keyword=' : ''}${currentKeyword}" 
                   class="cat-pill ${empty currentCategory ? 'active' : ''}">
                    <span class="material-symbols-outlined fs-5">restaurant_menu</span>
                    <span>Tất cả thực đơn</span>
                    <span class="badge-count">${totalCount}</span>
                </a>

                <!-- Dynamic Categories from Database -->
                <c:forEach var="cat" items="${categories}">
                    <c:url var="catUrl" value="/menu">
                        <c:param name="category" value="${cat.id}"/>
                        <c:if test="${not empty currentKeyword}">
                            <c:param name="keyword" value="${currentKeyword}"/>
                        </c:if>
                    </c:url>
                    <a href="${catUrl}" class="cat-pill ${currentCategory == cat.id ? 'active' : ''}">
                        <span class="material-symbols-outlined fs-5">${not empty cat.icon ? cat.icon : 'local_cafe'}</span>
                        <span>${cat.name}</span>
                        <c:if test="${cat.productCount > 0}">
                            <span class="badge-count">${cat.productCount}</span>
                        </c:if>
                    </a>
                </c:forEach>
            </div>
        </div>

        <!-- Subheader: Results count & Live Status Notice -->
        <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-4">
            <div class="d-flex align-items-center gap-3">
                <span class="text-uppercase fw-bold text-secondary" style="font-size: 0.88rem; letter-spacing: 0.5px;">
                    HIỂN THỊ KẾT QUẢ: <strong class="text-dark">${products.size()} món</strong>
                </span>

                <!-- Active filters tag if any -->
                <c:if test="${not empty currentCategory || not empty currentKeyword}">
                    <a href="${pageContext.request.contextPath}/menu" class="badge rounded-pill bg-secondary text-white text-decoration-none px-3 py-2 d-inline-flex align-items-center gap-1" style="font-size: 0.75rem;">
                        <span>Xoá bộ lọc</span>
                        <span class="material-symbols-outlined fs-6">close</span>
                    </a>
                </c:if>
            </div>

            <div class="craft-indicator">
                <span class="craft-dot"></span>
                <span>Pha chế thủ công ngay khi nhận yêu cầu</span>
            </div>
        </div>

        <!-- Products Grid -->
        <c:choose>
            <c:when test="${not empty products}">
                <div class="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-lg-4 g-4" id="productsGrid">
                    <c:forEach var="p" items="${products}">
                        <div class="col product-item-col" data-name="${p.name.toLowerCase()}" data-category="${p.categoryId}">
                            <div class="card h-100 product-card shadow-sm">
                                <!-- Image Thumbnail with Badges -->
                                <div class="product-thumb-wrapper">
                                    <!-- Badge Top Left: BÁN CHẠY / MÓN MỚI -->
                                    <c:if test="${not empty p.badge}">
                                        <span class="badge-tag-top ${p.badge == 'BÁN CHẠY' ? 'badge-banchay' : 'badge-monmoi'}">
                                            ${p.badge}
                                        </span>
                                    </c:if>

                                    <!-- Subtitle Tag Bottom Right: Phin Cổ Điển / Espresso Base... -->
                                    <c:if test="${not empty p.subtitle}">
                                        <span class="badge-tag-bottom">
                                            ${p.subtitle}
                                        </span>
                                    </c:if>

                                    <img src="${p.imageUrl}" alt="${p.name}" class="product-thumb" loading="lazy"
                                         onerror="this.src='https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80'">
                                </div>

                                <!-- Card Body -->
                                <div class="card-body-content">
                                    <h3 class="product-title" title="${p.name}">${p.name}</h3>
                                    <p class="product-desc" title="${p.description}">
                                        ${not empty p.description ? p.description : 'Đồ uống tinh hoa được tuyển chọn từ các vùng nguyên liệu đặc sản.'}
                                    </p>

                                    <!-- Price & Action Button -->
                                    <div class="card-footer-box">
                                        <div>
                                            <div class="price-label">Giá tiêu chuẩn</div>
                                            <div class="price-amount text-danger">${p.formattedPrice}</div>
                                        </div>

                                        <button type="button" class="btn-order-action" onclick="openProductDetailModal(${p.id})">
                                            <span>Xem chi tiết / Chọn món</span>
                                            <span class="material-symbols-outlined fs-6">arrow_forward</span>
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:when>
            <c:otherwise>
                <!-- Empty State -->
                <div class="text-center py-5 my-5">
                    <div class="rounded-circle bg-light d-inline-flex p-4 mb-3 border">
                        <span class="material-symbols-outlined text-secondary" style="font-size: 3.5rem;">search_off</span>
                    </div>
                    <h4 class="fw-bold mb-2">Không tìm thấy món phù hợp</h4>
                    <p class="text-muted mb-4">Vui lòng thử tìm kiếm với từ khoá khác hoặc khám phá tất cả các danh mục đồ uống của Aura Café.</p>
                    <a href="${pageContext.request.contextPath}/menu" class="btn btn-danger px-4 py-2 fw-semibold rounded-pill">
                        Xem tất cả thực đơn
                    </a>
                </div>
            </c:otherwise>
        </c:choose>

    </div>

    <!-- Floating System Mode Button (Giống hệt screenshot mockup) -->
    <a href="javascript:void(0)" onclick="switchViewInfo()" class="floating-mode-switch">
        <span class="material-symbols-outlined fs-5">grid_view</span>
        <span>Chuyển Phân Hệ (23 Màn hình)</span>
    </a>

    <!-- Product Customization / Detail Modal -->
    <div class="modal fade" id="productDetailModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content border-0 shadow-lg" style="border-radius: 1.5rem; overflow: hidden;">
                <div class="modal-header border-0 pb-0">
                    <button type="button" class="btn-close" data-bs-toggle="modal"></button>
                </div>
                <div class="modal-body p-4 pt-1">
                    <div class="row g-4">
                        <!-- Left: Product Image -->
                        <div class="col-md-5">
                            <div class="position-relative h-100">
                                <img id="modalProductImg" src="" alt="Product" class="modal-product-img">
                                <span id="modalProductBadge" class="badge-tag-top badge-banchay d-none"></span>
                                <span id="modalProductSubtitle" class="badge-tag-bottom d-none"></span>
                            </div>
                        </div>

                        <!-- Right: Customization Options -->
                        <div class="col-md-7 d-flex flex-column">
                            <div class="small text-danger fw-bold text-uppercase" id="modalProductCategory">Danh mục</div>
                            <h2 class="fs-4 fw-bold mb-2" id="modalProductName">Tên món</h2>
                            <p class="text-muted small mb-3" id="modalProductDesc">Mô tả sản phẩm</p>
                            
                            <div class="d-flex align-items-baseline gap-2 mb-3 pb-3 border-bottom">
                                <span class="fs-3 fw-bold text-danger" id="modalProductTotalPrice">0đ</span>
                                <span class="text-muted small">/ Giá tiêu chuẩn</span>
                            </div>

                            <!-- Size Options -->
                            <div class="mb-3" id="sizeOptionsContainer">
                                <label class="form-label fw-bold small text-secondary">CHỌN SIZE (KÍCH CỠ):</label>
                                <div class="d-flex gap-2 flex-wrap" id="sizeList">
                                    <!-- Dynamic sizes -->
                                </div>
                            </div>

                            <!-- Topping Options -->
                            <div class="mb-3" id="toppingOptionsContainer">
                                <label class="form-label fw-bold small text-secondary">THÊM TOPPING & YÊU CẦU:</label>
                                <div class="d-flex flex-column gap-2" id="toppingList">
                                    <!-- Dynamic toppings -->
                                </div>
                            </div>

                            <!-- Quantity & Add to Cart Action -->
                            <div class="mt-auto pt-3 border-top d-flex align-items-center justify-content-between gap-3">
                                <div class="d-flex align-items-center gap-2 border rounded-3 p-1">
                                    <button type="button" class="qty-btn" onclick="adjustModalQty(-1)">-</button>
                                    <span id="modalProductQty" class="fw-bold px-2" style="min-width: 28px; text-align: center;">1</span>
                                    <button type="button" class="qty-btn" onclick="adjustModalQty(1)">+</button>
                                </div>

                                <button type="button" class="btn btn-danger flex-grow-1 py-2 fw-semibold d-flex align-items-center justify-content-center gap-2 rounded-3"
                                        onclick="confirmAddToCart()">
                                    <span class="material-symbols-outlined fs-5">shopping_bag</span>
                                    <span>Thêm vào giỏ hàng • <span id="modalBtnTotalPrice">0đ</span></span>
                                </button>
                            </div>

                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Shopping Cart Preview Modal -->
    <div class="modal fade" id="cartPreviewModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg rounded-4">
                <div class="modal-header border-0 pb-0">
                    <h5 class="modal-title fw-bold d-flex align-items-center gap-2">
                        <span class="material-symbols-outlined text-danger">shopping_bag</span>
                        Giỏ hàng của bạn
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4" id="cartModalBody">
                    <div id="cartEmptyState" class="text-center py-4">
                        <span class="material-symbols-outlined text-muted fs-1 mb-2">remove_shopping_cart</span>
                        <p class="text-muted mb-0">Giỏ hàng của bạn đang trống.</p>
                    </div>
                    <div id="cartItemList" class="d-flex flex-column gap-3 mb-3"></div>
                    <div id="cartSummary" class="border-top pt-3 d-none">
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <span class="fw-bold">Tổng thanh toán tạm tính:</span>
                            <span class="fs-5 fw-bold text-danger" id="cartTotalSum">0đ</span>
                        </div>
                        <button type="button" class="btn btn-danger w-100 py-2 fw-semibold rounded-3" onclick="showFeatureNotice('Tiến hành Đặt hàng (Online Ordering)')">
                            Tiến hành thanh toán
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Toast Notification for Add to Cart -->
    <div class="toast-container position-fixed bottom-0 start-50 translate-middle-x p-3 z-3">
        <div id="cartToast" class="toast align-items-center text-white bg-dark border-0 shadow-lg rounded-pill" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex px-3 py-2 align-items-center">
                <span class="material-symbols-outlined text-success me-2 fs-5">check_circle</span>
                <div class="toast-body p-0 fw-medium small" id="cartToastMessage">
                    Đã thêm món vào giỏ hàng thành công!
                </div>
                <button type="button" class="btn-close btn-close-white ms-auto me-1" data-bs-dismiss="toast"></button>
            </div>
        </div>
    </div>

    <!-- Include Auth Modal for Authentication -->
    <jsp:include page="/components/auth_modal.jsp" />

    <!-- Bootstrap 5 JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

    <!-- Custom Menu Interactive Script -->
    <script>
        // State variables for Product Detail Modal
        var currentProduct = null;
        var selectedSizePrice = 0;
        var selectedToppingPrice = 0;
        var modalQty = 1;
        var cart = JSON.parse(localStorage.getItem('aura_cart') || '[]');

        function updateCartBadge() {
            var totalItems = 0;
            for (var i = 0; i < cart.length; i++) {
                totalItems += cart[i].quantity;
            }
            var badge = document.getElementById('headerCartBadge');
            if (badge) badge.innerText = totalItems;
            localStorage.setItem('aura_cart', JSON.stringify(cart));
        }

        // Initialize cart badge on load
        document.addEventListener('DOMContentLoaded', function() {
            updateCartBadge();
        });

        // Open Product Customization Modal
        function openProductDetailModal(productId) {
            fetch('${pageContext.request.contextPath}/product-detail?id=' + productId)
                .then(function(res) { return res.json(); })
                .then(function(data) {
                    if (data.error) {
                        alert(data.error);
                        return;
                    }
                    currentProduct = data;
                    modalQty = 1;
                    selectedSizePrice = 0;
                    selectedToppingPrice = 0;

                    document.getElementById('modalProductImg').src = data.imageUrl;
                    document.getElementById('modalProductName').innerText = data.name;
                    document.getElementById('modalProductCategory').innerText = data.categoryName;
                    document.getElementById('modalProductDesc').innerText = data.description || '';
                    document.getElementById('modalProductQty').innerText = 1;

                    // Badges
                    var badgeElem = document.getElementById('modalProductBadge');
                    if (data.badge) {
                        badgeElem.innerText = data.badge;
                        badgeElem.className = 'badge-tag-top ' + (data.badge === 'BÁN CHẠY' ? 'badge-banchay' : 'badge-monmoi');
                        badgeElem.classList.remove('d-none');
                    } else {
                        badgeElem.classList.add('d-none');
                    }

                    var subtitleElem = document.getElementById('modalProductSubtitle');
                    if (data.subtitle) {
                        subtitleElem.innerText = data.subtitle;
                        subtitleElem.classList.remove('d-none');
                    } else {
                        subtitleElem.classList.add('d-none');
                    }

                    // Parse variants config
                    var config = data.variantsConfig || {};
                    var sizes = config.sizes || { "M": 0, "L": 10000 };
                    var toppings = config.toppings || [];

                    // Render sizes
                    var sizeListHtml = '';
                    var sizeKeys = Object.keys(sizes);
                    sizeKeys.forEach(function(sName, index) {
                        var extraPrice = sizes[sName];
                        var isFirst = index === 0;
                        if (isFirst) selectedSizePrice = extraPrice;
                        sizeListHtml += '<div class="option-chip ' + (isFirst ? 'selected' : '') + '" onclick="selectSize(this, ' + extraPrice + ')">'
                                      + sName + (extraPrice > 0 ? ' (+' + extraPrice.toLocaleString('vi-VN') + 'đ)' : '')
                                      + '</div>';
                    });
                    document.getElementById('sizeList').innerHTML = sizeListHtml;

                    // Render toppings
                    var toppingListHtml = '';
                    if (Array.isArray(toppings) && toppings.length > 0) {
                        document.getElementById('toppingOptionsContainer').classList.remove('d-none');
                        toppings.forEach(function(t, idx) {
                            var tName = typeof t === 'string' ? t : t.name;
                            var tPrice = typeof t === 'object' && t.price ? t.price : 8000;
                            toppingListHtml += '<div class="form-check d-flex align-items-center justify-content-between p-2 border rounded-3">'
                                             + '  <div>'
                                             + '    <input class="form-check-input ms-0 me-2" type="checkbox" id="top_' + idx + '" value="' + tPrice + '" onchange="calculateModalTotal()">'
                                             + '    <label class="form-check-label fw-medium" for="top_' + idx + '">' + tName + '</label>'
                                             + '  </div>'
                                             + '  <span class="text-danger fw-bold small">+' + tPrice.toLocaleString('vi-VN') + 'đ</span>'
                                             + '</div>';
                        });
                        document.getElementById('toppingList').innerHTML = toppingListHtml;
                    } else {
                        document.getElementById('toppingOptionsContainer').classList.add('d-none');
                    }

                    calculateModalTotal();

                    var myModal = new bootstrap.Modal(document.getElementById('productDetailModal'));
                    myModal.show();
                })
                .catch(function(err) {
                    console.error('Error loading product:', err);
                });
        }

        function selectSize(element, extraPrice) {
            var chips = document.querySelectorAll('#sizeList .option-chip');
            chips.forEach(function(c) { c.classList.remove('selected'); });
            element.classList.add('selected');
            selectedSizePrice = extraPrice;
            calculateModalTotal();
        }

        function adjustModalQty(delta) {
            modalQty = Math.max(1, modalQty + delta);
            document.getElementById('modalProductQty').innerText = modalQty;
            calculateModalTotal();
        }

        function calculateModalTotal() {
            if (!currentProduct) return;
            var checkboxes = document.querySelectorAll('#toppingList input[type="checkbox"]:checked');
            var topPrice = 0;
            checkboxes.forEach(function(cb) {
                topPrice += parseFloat(cb.value) || 0;
            });
            var singleItemPrice = currentProduct.basePrice + selectedSizePrice + topPrice;
            var total = singleItemPrice * modalQty;

            var formatted = total.toLocaleString('vi-VN') + 'đ';
            document.getElementById('modalProductTotalPrice').innerText = formatted;
            document.getElementById('modalBtnTotalPrice').innerText = formatted;
        }

        function confirmAddToCart() {
            if (!currentProduct) return;

            var activeSize = document.querySelector('#sizeList .option-chip.selected');
            var sizeName = activeSize ? activeSize.innerText.split(' ')[0] : 'Tiêu chuẩn';

            var toppingNames = [];
            var checkboxes = document.querySelectorAll('#toppingList input[type="checkbox"]:checked');
            checkboxes.forEach(function(cb) {
                var label = cb.parentElement.querySelector('label');
                if (label) toppingNames.push(label.innerText);
            });

            var singlePrice = currentProduct.basePrice + selectedSizePrice;
            checkboxes.forEach(function(cb) { singlePrice += parseFloat(cb.value) || 0; });

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

            // Hide modal
            var modalInstance = bootstrap.Modal.getInstance(document.getElementById('productDetailModal'));
            if (modalInstance) modalInstance.hide();

            // Show Toast notification
            document.getElementById('cartToastMessage').innerText = 'Đã thêm ' + modalQty + 'x ' + currentProduct.name + ' vào giỏ hàng!';
            var toast = new bootstrap.Toast(document.getElementById('cartToast'));
            toast.show();
        }

        function openCartModal() {
            var body = document.getElementById('cartItemList');
            var empty = document.getElementById('cartEmptyState');
            var summary = document.getElementById('cartSummary');

            if (cart.length === 0) {
                body.innerHTML = '';
                empty.classList.remove('d-none');
                summary.classList.add('d-none');
            } else {
                empty.classList.add('d-none');
                summary.classList.remove('d-none');

                var html = '';
                var sum = 0;
                cart.forEach(function(item, idx) {
                    sum += item.subtotal;
                    html += '<div class="d-flex align-items-center justify-content-between p-2 border-bottom">'
                          + '  <div class="d-flex align-items-center gap-3">'
                          + '    <img src="' + item.imageUrl + '" style="width: 50px; height: 50px; object-fit: cover; border-radius: 8px;">'
                          + '    <div>'
                          + '      <div class="fw-bold small">' + item.name + ' <span class="badge bg-secondary">' + item.size + '</span></div>'
                          + '      <div class="text-muted" style="font-size: 0.75rem;">' + (item.toppings.length ? item.toppings.join(', ') : 'Không topping') + '</div>'
                          + '      <div class="text-danger fw-semibold small">' + item.unitPrice.toLocaleString('vi-VN') + 'đ x ' + item.quantity + '</div>'
                          + '    </div>'
                          + '  </div>'
                          + '  <button class="btn btn-sm btn-outline-danger p-1 border-0" onclick="removeFromCart(' + idx + ')">'
                          + '    <span class="material-symbols-outlined fs-6">delete</span>'
                          + '  </button>'
                          + '</div>';
                });
                body.innerHTML = html;
                document.getElementById('cartTotalSum').innerText = sum.toLocaleString('vi-VN') + 'đ';
            }

            var modal = new bootstrap.Modal(document.getElementById('cartPreviewModal'));
            modal.show();
        }

        function removeFromCart(index) {
            cart.splice(index, 1);
            updateCartBadge();
            openCartModal(); // Refresh cart modal
        }

        function showFeatureNotice(featureName) {
            alert('Tính năng "' + featureName + '" đang được hoàn thiện theo tiến độ của nhóm!');
        }

        function switchViewInfo() {
            alert('Aura Café ERP: Đang xem màn hình Menu Screen (#4 do MinhNDQ phụ trách).');
        }
    </script>
</body>
</html>
