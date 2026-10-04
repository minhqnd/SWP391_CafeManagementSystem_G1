<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Aura Café | ${product.name}</title>
    <meta name="description" content="${product.description}" />

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
            --primary-light: #f9e0e5;
            --surface: #ffffff;
            --bg-body: #f8fafc;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --border-color: #f1f5f9;
            --accent-warm: #fef3c7;
            --accent-warm-text: #92400e;
        }

        body {
            font-family: 'Plus Jakarta Sans', 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
            background-color: var(--bg-body);
            color: var(--text-main);
            overflow-x: hidden;
            letter-spacing: -0.01em;
        }

        /* ============== Breadcrumb ============== */
        .detail-breadcrumb {
            padding: 1rem 0 0.5rem;
        }
        .detail-breadcrumb a {
            color: var(--text-muted);
            text-decoration: none;
            font-size: 0.85rem;
            font-weight: 500;
            transition: color 0.2s;
        }
        .detail-breadcrumb a:hover {
            color: var(--primary);
        }
        .detail-breadcrumb .separator {
            color: #cbd5e1;
            margin: 0 0.5rem;
            font-size: 0.75rem;
        }
        .detail-breadcrumb .current {
            color: var(--text-main);
            font-weight: 600;
            font-size: 0.85rem;
        }

        /* ============== Product Hero Section ============== */
        .product-detail-section {
            padding: 1.5rem 0 3rem;
        }

        /* Image Gallery */
        .product-gallery {
            position: relative;
            border-radius: 1.5rem;
            overflow: hidden;
            background: #f1f5f9;
            aspect-ratio: 1 / 1;
            box-shadow: 0 8px 30px -8px rgba(15, 23, 42, 0.1);
        }
        .product-gallery img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
        }
        .product-gallery:hover img {
            transform: scale(1.04);
        }
        .gallery-badge {
            position: absolute;
            top: 1.25rem;
            left: 1.25rem;
            padding: 0.4rem 1rem;
            font-size: 0.72rem;
            font-weight: 700;
            letter-spacing: 0.5px;
            text-transform: uppercase;
            border-radius: 999px;
            z-index: 2;
            backdrop-filter: blur(8px);
        }
        .gallery-badge-hot {
            background: rgba(225, 29, 72, 0.92);
            color: #fff;
        }
        .gallery-badge-new {
            background: rgba(14, 165, 233, 0.92);
            color: #fff;
        }
        .gallery-category-tag {
            position: absolute;
            bottom: 1.25rem;
            left: 1.25rem;
            background: rgba(255,255,255,0.92);
            backdrop-filter: blur(10px);
            padding: 0.35rem 0.85rem;
            border-radius: 999px;
            font-size: 0.75rem;
            font-weight: 600;
            color: var(--text-main);
            display: flex;
            align-items: center;
            gap: 0.35rem;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
        }

        /* Info Panel */
        .product-info-panel {
            display: flex;
            flex-direction: column;
            height: 100%;
        }
        .product-category-label {
            font-size: 0.72rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 1.2px;
            color: var(--primary);
            margin-bottom: 0.5rem;
            display: flex;
            align-items: center;
            gap: 0.4rem;
        }
        .product-category-label .dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: var(--primary);
            display: inline-block;
        }
        .product-detail-name {
            font-size: 2.2rem;
            font-weight: 800;
            color: var(--text-main);
            line-height: 1.2;
            letter-spacing: -0.03em;
            margin-bottom: 0.75rem;
        }
        .product-detail-desc {
            font-size: 0.95rem;
            color: var(--text-muted);
            line-height: 1.7;
            margin-bottom: 1.5rem;
        }

        /* Price Display */
        .price-display {
            background: linear-gradient(135deg, var(--primary-soft) 0%, #fff5f7 100%);
            border: 1px solid var(--primary-light);
            border-radius: 1rem;
            padding: 1.25rem 1.5rem;
            margin-bottom: 1.75rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .price-main {
            font-size: 2rem;
            font-weight: 800;
            color: var(--primary);
            line-height: 1;
        }
        .price-note {
            font-size: 0.78rem;
            color: var(--text-muted);
            font-weight: 500;
        }
        .price-from-label {
            font-size: 0.7rem;
            color: var(--text-muted);
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 0.15rem;
        }

        /* Options Sections */
        .options-section {
            margin-bottom: 1.5rem;
        }
        .options-label {
            font-size: 0.78rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.8px;
            color: var(--text-muted);
            margin-bottom: 0.75rem;
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }
        .options-label .material-symbols-outlined {
            font-size: 1rem;
            color: var(--primary);
        }

        /* Size Chips */
        .size-chip {
            cursor: pointer;
            border: 1.5px solid #e2e8f0;
            border-radius: 0.85rem;
            padding: 0.65rem 1.35rem;
            font-size: 0.88rem;
            font-weight: 600;
            transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
            background: #fff;
            color: #475569;
            text-align: center;
            min-width: 100px;
            position: relative;
        }
        .size-chip:hover {
            border-color: rgba(139, 21, 40, 0.35);
            background: var(--primary-soft);
            color: var(--primary);
        }
        .size-chip.selected {
            border-color: var(--primary);
            background: var(--primary);
            color: #fff;
            box-shadow: 0 4px 14px rgba(139, 21, 40, 0.25);
        }
        .size-chip .size-extra {
            display: block;
            font-size: 0.7rem;
            font-weight: 500;
            opacity: 0.8;
            margin-top: 2px;
        }
        .size-chip.selected .size-extra {
            color: rgba(255,255,255,0.85);
        }

        /* Topping Cards */
        .topping-card {
            border: 1.5px solid #e2e8f0;
            border-radius: 0.85rem;
            padding: 0.75rem 1rem;
            cursor: pointer;
            transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
            background: #fff;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 0.75rem;
        }
        .topping-card:hover {
            border-color: rgba(139, 21, 40, 0.3);
            background: var(--primary-soft);
        }
        .topping-card.selected {
            border-color: var(--primary);
            background: var(--primary-soft);
        }
        .topping-card .topping-check {
            width: 22px;
            height: 22px;
            border-radius: 6px;
            border: 2px solid #cbd5e1;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.2s;
            flex-shrink: 0;
        }
        .topping-card.selected .topping-check {
            background: var(--primary);
            border-color: var(--primary);
        }
        .topping-card .topping-price {
            font-size: 0.82rem;
            font-weight: 700;
            color: var(--primary);
            white-space: nowrap;
        }

        /* Quantity Control */
        .qty-control {
            display: inline-flex;
            align-items: center;
            border: 1.5px solid #e2e8f0;
            border-radius: 999px;
            padding: 0.25rem;
            background: #fff;
        }
        .qty-btn {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            border: none;
            background: transparent;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            font-size: 1.15rem;
            color: var(--text-muted);
            cursor: pointer;
            transition: all 0.18s;
        }
        .qty-btn:hover {
            background: #f1f5f9;
            color: var(--text-main);
        }
        .qty-value {
            min-width: 44px;
            text-align: center;
            font-size: 1.1rem;
            font-weight: 700;
            color: var(--text-main);
        }

        /* Add to Cart Button */
        .btn-add-to-cart {
            background: linear-gradient(135deg, var(--primary) 0%, #a81c31 100%);
            color: #fff;
            border: none;
            border-radius: 999px;
            padding: 1rem 2rem;
            font-size: 1rem;
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.65rem;
            transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
            box-shadow: 0 6px 20px rgba(139, 21, 40, 0.3);
            width: 100%;
            cursor: pointer;
        }
        .btn-add-to-cart:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 28px rgba(139, 21, 40, 0.4);
            background: linear-gradient(135deg, #70101f 0%, #8b1528 100%);
        }
        .btn-add-to-cart:active {
            transform: translateY(0);
        }

        /* Divider */
        .section-divider {
            border: none;
            border-top: 1px solid #f1f5f9;
            margin: 1.25rem 0;
        }

        /* Info Tags */
        .info-tags {
            display: flex;
            gap: 0.5rem;
            flex-wrap: wrap;
            margin-bottom: 1.5rem;
        }
        .info-tag {
            display: inline-flex;
            align-items: center;
            gap: 0.35rem;
            padding: 0.4rem 0.85rem;
            border-radius: 999px;
            font-size: 0.75rem;
            font-weight: 600;
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            color: var(--text-muted);
        }
        .info-tag .material-symbols-outlined {
            font-size: 0.95rem;
        }
        .info-tag-available {
            background: #f0fdf4;
            border-color: #bbf7d0;
            color: #166534;
        }

        /* ============== Related Products ============== */
        .related-section {
            padding: 3rem 0 4rem;
        }
        .related-title {
            font-size: 1.5rem;
            font-weight: 800;
            color: var(--text-main);
            letter-spacing: -0.02em;
        }
        .related-subtitle {
            font-size: 0.88rem;
            color: var(--text-muted);
        }

        /* Related Product Card */
        .related-card {
            background: #fff;
            border: 1px solid #eef2f6;
            border-radius: 1.25rem;
            overflow: hidden;
            transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
            cursor: pointer;
            text-decoration: none;
            display: block;
        }
        .related-card:hover {
            transform: translateY(-4px);
            border-color: #cbd5e1;
            box-shadow: 0 12px 28px -6px rgba(15, 23, 42, 0.08);
        }
        .related-card-img {
            width: 100%;
            height: 180px;
            object-fit: cover;
            transition: transform 0.45s cubic-bezier(0.16, 1, 0.3, 1);
        }
        .related-card:hover .related-card-img {
            transform: scale(1.05);
        }
        .related-card-body {
            padding: 1rem 1.15rem;
        }
        .related-card-category {
            font-size: 0.68rem;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: #94a3b8;
        }
        .related-card-name {
            font-size: 0.95rem;
            font-weight: 700;
            color: var(--text-main);
            margin: 0.2rem 0 0.5rem;
            transition: color 0.2s;
        }
        .related-card:hover .related-card-name {
            color: var(--primary);
        }
        .related-card-price {
            font-size: 1.05rem;
            font-weight: 800;
            color: var(--primary);
        }

        /* ============== Action Bar (Sticky bottom on mobile) ============== */
        .action-bar-sticky {
            display: none;
        }
        @media (max-width: 767.98px) {
            .product-detail-name {
                font-size: 1.6rem;
            }
            .price-main {
                font-size: 1.5rem;
            }
            .product-gallery {
                aspect-ratio: 4 / 3;
                border-radius: 1rem;
            }
            .action-bar-sticky {
                display: flex;
                position: fixed;
                bottom: 0;
                left: 0;
                right: 0;
                background: #fff;
                border-top: 1px solid #f1f5f9;
                padding: 0.75rem 1rem;
                z-index: 1040;
                align-items: center;
                justify-content: space-between;
                gap: 1rem;
                box-shadow: 0 -4px 20px rgba(0,0,0,0.06);
            }
            .action-bar-sticky .price-main {
                font-size: 1.25rem;
            }
            body {
                padding-bottom: 85px;
            }
        }

        /* ============== Animations ============== */
        @keyframes fadeInUp {
            from { opacity: 0; transform: translateY(20px); }
            to { opacity: 1; transform: translateY(0); }
        }
        .animate-in {
            animation: fadeInUp 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }
        .animate-delay-1 { animation-delay: 0.1s; opacity: 0; }
        .animate-delay-2 { animation-delay: 0.2s; opacity: 0; }
        .animate-delay-3 { animation-delay: 0.3s; opacity: 0; }
        .animate-delay-4 { animation-delay: 0.4s; opacity: 0; }

        /* Toast */
        .toast-container { z-index: 1080 !important; }
    </style>
</head>

<body>

    <!-- Shared Header -->
    <jsp:include page="/components/header.jsp">
        <jsp:param name="activePage" value="menu" />
    </jsp:include>

    <div class="container-fluid px-4 px-lg-5">

        <!-- Breadcrumb -->
        <nav class="detail-breadcrumb animate-in">
            <a href="${pageContext.request.contextPath}/">Trang chủ</a>
            <span class="separator material-symbols-outlined" style="font-size: 14px; vertical-align: middle;">chevron_right</span>
            <a href="${pageContext.request.contextPath}/menu">Menu & Đồ uống</a>
            <span class="separator material-symbols-outlined" style="font-size: 14px; vertical-align: middle;">chevron_right</span>
            <span class="current">${product.name}</span>
        </nav>

        <!-- Product Detail Hero -->
        <section class="product-detail-section">
            <div class="row g-4 g-lg-5 align-items-start">

                <!-- Left: Product Image -->
                <div class="col-12 col-md-6 col-lg-5 animate-in animate-delay-1">
                    <div class="product-gallery">
                        <c:if test="${not empty product.badge}">
                            <span class="gallery-badge ${product.badge == 'BÁN CHẠY' ? 'gallery-badge-hot' : 'gallery-badge-new'}">
                                ${product.badge}
                            </span>
                        </c:if>
                        <img id="productMainImg" src="${product.imageUrl}" alt="${product.name}"
                             onerror="this.src='https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=800&auto=format&fit=crop&q=80'">
                        <div class="gallery-category-tag">
                            <span class="material-symbols-outlined" style="font-size: 14px;">local_cafe</span>
                            ${product.categoryName}
                        </div>
                    </div>
                </div>

                <!-- Right: Product Info & Actions -->
                <div class="col-12 col-md-6 col-lg-7 animate-in animate-delay-2">
                    <div class="product-info-panel">

                        <!-- Category Label -->
                        <div class="product-category-label">
                            <span class="dot"></span>
                            ${product.categoryName}
                        </div>

                        <!-- Product Name -->
                        <h1 class="product-detail-name">${product.name}</h1>

                        <!-- Info Tags -->
                        <div class="info-tags">
                            <c:if test="${product.available}">
                                <span class="info-tag info-tag-available">
                                    <span class="material-symbols-outlined">check_circle</span>
                                    Còn phục vụ
                                </span>
                            </c:if>
                            <c:if test="${not empty product.subtitle}">
                                <span class="info-tag">
                                    <span class="material-symbols-outlined">star</span>
                                    ${product.subtitle}
                                </span>
                            </c:if>
                            <span class="info-tag">
                                <span class="material-symbols-outlined">local_shipping</span>
                                Giao hàng tận nơi
                            </span>
                        </div>

                        <!-- Description -->
                        <p class="product-detail-desc">
                            ${not empty product.description ? product.description : 'Đồ uống tinh hoa được tuyển chọn từ vùng nguyên liệu đặc sản, pha chế thủ công bởi đội ngũ barista chuyên nghiệp tại Aura Café.'}
                        </p>

                        <!-- Price -->
                        <div class="price-display">
                            <div>
                                <div class="price-from-label">Giá từ</div>
                                <span class="price-main" id="displayTotalPrice">${product.formattedPrice}</span>
                            </div>
                            <div class="text-end">
                                <span class="price-note">Đã bao gồm VAT</span>
                            </div>
                        </div>

                        <!-- Size Options -->
                        <div class="options-section" id="sizeSection">
                            <div class="options-label">
                                <span class="material-symbols-outlined">straighten</span>
                                Chọn kích cỡ
                            </div>
                            <div class="d-flex gap-2 flex-wrap" id="sizeOptionsContainer"></div>
                        </div>

                        <!-- Topping Options -->
                        <div class="options-section" id="toppingSection">
                            <div class="options-label">
                                <span class="material-symbols-outlined">add_circle</span>
                                Topping thêm
                            </div>
                            <div class="d-flex flex-column gap-2" id="toppingOptionsContainer"></div>
                        </div>

                        <hr class="section-divider">

                        <!-- Quantity & Add to Cart (Desktop) -->
                        <div class="d-none d-md-block animate-in animate-delay-3">
                            <div class="d-flex align-items-center gap-3 mb-3">
                                <div class="options-label mb-0" style="white-space: nowrap;">
                                    <span class="material-symbols-outlined">shopping_cart</span>
                                    Số lượng
                                </div>
                                <div class="qty-control">
                                    <button type="button" class="qty-btn" onclick="adjustQty(-1)">−</button>
                                    <span class="qty-value" id="qtyValue">1</span>
                                    <button type="button" class="qty-btn" onclick="adjustQty(1)">+</button>
                                </div>
                            </div>

                            <button type="button" class="btn-add-to-cart" onclick="addToCart()" id="btnAddToCart">
                                <span class="material-symbols-outlined fs-5">shopping_bag</span>
                                <span>Thêm vào giỏ hàng</span>
                                <span>•</span>
                                <span id="btnTotalPrice">${product.formattedPrice}</span>
                            </button>
                        </div>

                    </div>
                </div>
            </div>
        </section>

        <!-- Related Products -->
        <c:if test="${not empty relatedProducts}">
            <section class="related-section animate-in animate-delay-4">
                <hr class="section-divider mb-4">
                <div class="d-flex align-items-end justify-content-between mb-4">
                    <div>
                        <h2 class="related-title mb-1">Có thể bạn cũng thích</h2>
                        <p class="related-subtitle mb-0">Các món cùng danh mục ${product.categoryName}</p>
                    </div>
                    <a href="${pageContext.request.contextPath}/menu?category=${product.categoryId}"
                       class="btn btn-sm btn-outline-danger rounded-pill px-3 py-1.5 fw-semibold d-none d-md-inline-flex align-items-center gap-1 text-decoration-none"
                       style="font-size: 0.85rem;">
                        Xem tất cả
                        <span class="material-symbols-outlined" style="font-size: 16px;">arrow_forward</span>
                    </a>
                </div>

                <div class="row row-cols-2 row-cols-md-3 row-cols-lg-4 g-3 g-lg-4">
                    <c:forEach var="rp" items="${relatedProducts}">
                        <div class="col">
                            <a href="${pageContext.request.contextPath}/product-detail?id=${rp.id}" class="related-card h-100">
                                <div style="overflow: hidden;">
                                    <img src="${rp.imageUrl}" alt="${rp.name}" class="related-card-img" loading="lazy"
                                         onerror="this.src='https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80'">
                                </div>
                                <div class="related-card-body">
                                    <div class="related-card-category">${rp.categoryName}</div>
                                    <div class="related-card-name">${rp.name}</div>
                                    <div class="related-card-price">${rp.formattedPrice}</div>
                                </div>
                            </a>
                        </div>
                    </c:forEach>
                </div>
            </section>
        </c:if>

    </div>

    <!-- Mobile Sticky Action Bar -->
    <div class="action-bar-sticky">
        <div>
            <div style="font-size: 0.7rem; color: var(--text-muted); font-weight: 600; text-transform: uppercase; letter-spacing: 0.3px;">Tổng cộng</div>
            <span class="price-main" id="mobileDisplayPrice">${product.formattedPrice}</span>
        </div>
        <div class="d-flex align-items-center gap-2">
            <div class="qty-control" style="transform: scale(0.88);">
                <button type="button" class="qty-btn" onclick="adjustQty(-1)">−</button>
                <span class="qty-value" id="mobileQtyValue">1</span>
                <button type="button" class="qty-btn" onclick="adjustQty(1)">+</button>
            </div>
            <button type="button" class="btn-add-to-cart" style="width: auto; padding: 0.75rem 1.5rem; font-size: 0.9rem;" onclick="addToCart()">
                <span class="material-symbols-outlined" style="font-size: 1.15rem;">add</span>
                Thêm
            </button>
        </div>
    </div>

    <!-- Toast Notification -->
    <div class="toast-container position-fixed bottom-0 start-50 translate-middle-x p-3">
        <div id="cartToast"
            class="toast align-items-center text-white bg-dark border-0 shadow-lg rounded-pill" role="alert"
            aria-live="assertive" aria-atomic="true">
            <div class="d-flex px-3 py-2 align-items-center">
                <span class="material-symbols-outlined text-success me-2 fs-5">check_circle</span>
                <div class="toast-body p-0 fw-medium small" id="cartToastMessage">
                    Đã thêm vào giỏ hàng!
                </div>
                <button type="button" class="btn-close btn-close-white ms-auto me-1" data-bs-dismiss="toast"></button>
            </div>
        </div>
    </div>

    <!-- Include Auth Modal -->
    <jsp:include page="/components/auth_modal.jsp" />

    <!-- Bootstrap 5 JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        // ===== Product Data from Server =====
        var productData = {
            id: Number('${product.id}'),
            name: '${product.name}',
            categoryName: '${product.categoryName}',
            categoryId: Number('${product.categoryId}'),
            basePrice: Number('${product.basePrice}'),
            imageUrl: '${product.imageUrl}',
            variantsConfig: '${product.variantsConfig != null ? product.variantsConfig : "{}"}'
        };

        // Giỏ hàng lưu trong session phía server, trang chỉ giữ tổng số món để hiển thị
        var cartCount = Number('${empty sessionScope.cart ? 0 : sessionScope.cart.totalQuantity}');
        var quantity = 1;
        var selectedSizePrice = 0;
        var selectedSizeName = '';
        var selectedToppings = [];

        // ===== Initialize Options =====
        document.addEventListener('DOMContentLoaded', function () {
            initOptions();
            updateCartBadge();
        });

        function initOptions() {
            var config = {};
            try {
                config = JSON.parse(productData.variantsConfig);
            } catch (e) {
                config = {};
            }

            var sizes = config.sizes;
            var toppings = Array.isArray(config.toppings) ? config.toppings : [];

            // Build Size Chips
            var sizeContainer = document.getElementById('sizeOptionsContainer');
            var sizeSection = document.getElementById('sizeSection');

            if (sizes && typeof sizes === 'object' && Object.keys(sizes).length > 0) {
                sizeSection.style.display = '';
                var sizeKeys = Object.keys(sizes);
                var html = '';
                sizeKeys.forEach(function (sName, idx) {
                    var extraPrice = sizes[sName] || 0;
                    var isFirst = idx === 0;
                    if (isFirst) {
                        selectedSizePrice = extraPrice;
                        selectedSizeName = sName;
                    }
                    html += '<div class="size-chip ' + (isFirst ? 'selected' : '') + '" onclick="selectSize(this, \'' + sName + '\', ' + extraPrice + ')">'
                        + '<span class="fw-bold">' + sName + '</span>'
                        + (extraPrice > 0 ? '<span class="size-extra">+' + extraPrice.toLocaleString('vi-VN') + 'đ</span>' : '<span class="size-extra">Tiêu chuẩn</span>')
                        + '</div>';
                });
                sizeContainer.innerHTML = html;
            } else {
                sizeSection.style.display = 'none';
                selectedSizeName = 'Tiêu chuẩn';
            }

            // Build Topping Cards
            var toppingContainer = document.getElementById('toppingOptionsContainer');
            var toppingSection = document.getElementById('toppingSection');

            if (toppings.length > 0) {
                toppingSection.style.display = '';
                var tHtml = '';
                toppings.forEach(function (t, idx) {
                    var tName = typeof t === 'string' ? t : t.name;
                    var tPrice = typeof t === 'object' && t.price ? t.price : 8000;
                    tHtml += '<div class="topping-card" onclick="toggleTopping(this, \'' + tName + '\', ' + tPrice + ')" data-name="' + tName + '" data-price="' + tPrice + '">'
                        + '  <div class="d-flex align-items-center gap-2">'
                        + '    <div class="topping-check"><span class="material-symbols-outlined" style="font-size: 14px; color: #fff;">check</span></div>'
                        + '    <span class="fw-medium" style="font-size: 0.9rem;">' + tName + '</span>'
                        + '  </div>'
                        + '  <span class="topping-price">+' + tPrice.toLocaleString('vi-VN') + 'đ</span>'
                        + '</div>';
                });
                toppingContainer.innerHTML = tHtml;
            } else {
                toppingSection.style.display = 'none';
            }

            calculateTotal();
        }

        function selectSize(el, sizeName, extraPrice) {
            document.querySelectorAll('.size-chip').forEach(function (c) { c.classList.remove('selected'); });
            el.classList.add('selected');
            selectedSizePrice = extraPrice;
            selectedSizeName = sizeName;
            calculateTotal();
        }

        function toggleTopping(el, name, price) {
            el.classList.toggle('selected');
            // Rebuild selectedToppings
            selectedToppings = [];
            document.querySelectorAll('.topping-card.selected').forEach(function (card) {
                selectedToppings.push({
                    name: card.getAttribute('data-name'),
                    price: parseFloat(card.getAttribute('data-price'))
                });
            });
            calculateTotal();
        }

        function adjustQty(delta) {
            quantity = Math.max(1, quantity + delta);
            var qtyEl = document.getElementById('qtyValue');
            var mobileQtyEl = document.getElementById('mobileQtyValue');
            if (qtyEl) qtyEl.innerText = quantity;
            if (mobileQtyEl) mobileQtyEl.innerText = quantity;
            calculateTotal();
        }

        function calculateTotal() {
            var base = productData.basePrice;
            var toppingTotal = 0;
            selectedToppings.forEach(function (t) { toppingTotal += t.price; });

            var singlePrice = base + selectedSizePrice + toppingTotal;
            var total = singlePrice * quantity;
            var formatted = total.toLocaleString('vi-VN') + 'đ';

            var displayEl = document.getElementById('displayTotalPrice');
            var btnEl = document.getElementById('btnTotalPrice');
            var mobileEl = document.getElementById('mobileDisplayPrice');

            if (displayEl) displayEl.innerText = formatted;
            if (btnEl) btnEl.innerText = formatted;
            if (mobileEl) mobileEl.innerText = formatted;
        }

        function addToCart() {
            // Gửi món vào giỏ hàng (CartController - /cart/add), giá được tính lại ở server
            var params = new URLSearchParams();
            params.append('productId', productData.id);
            params.append('quantity', quantity);
            params.append('size', selectedSizeName || '');
            selectedToppings.forEach(function (t) { params.append('topping', t.name); });

            fetch('${pageContext.request.contextPath}/cart/add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: params.toString()
            })
                .then(function (response) { return response.json(); })
                .then(function (data) {
                    if (!data.success) {
                        alert(data.error || 'Không thể thêm món vào giỏ hàng.');
                        return;
                    }
                    cartCount = data.totalItems;
                    updateCartBadge();
                    showAddedFeedback();
                })
                .catch(function () {
                    alert('Không thể kết nối máy chủ. Vui lòng thử lại!');
                });
        }

        function showAddedFeedback() {
            // Show toast
            var toastMsg = document.getElementById('cartToastMessage');
            if (toastMsg) {
                toastMsg.innerText = 'Đã thêm ' + quantity + 'x ' + productData.name + ' vào giỏ hàng!';
            }
            var toastEl = document.getElementById('cartToast');
            if (toastEl && window.bootstrap) {
                var toast = bootstrap.Toast.getOrCreateInstance(toastEl);
                toast.show();
            }

            // Button animation feedback
            var btn = document.getElementById('btnAddToCart');
            if (btn) {
                btn.innerHTML = '<span class="material-symbols-outlined fs-5">check_circle</span><span>Đã thêm!</span>';
                btn.style.background = '#16a34a';
                btn.style.boxShadow = '0 6px 20px rgba(22, 163, 74, 0.3)';
                setTimeout(function () {
                    btn.innerHTML = '<span class="material-symbols-outlined fs-5">shopping_bag</span><span>Thêm vào giỏ hàng</span><span>•</span><span id="btnTotalPrice">' + (productData.basePrice + selectedSizePrice).toLocaleString('vi-VN') + 'đ</span>';
                    btn.style.background = '';
                    btn.style.boxShadow = '';
                    calculateTotal();
                }, 1500);
            }
        }

        function updateCartBadge() {
            var headerBadge = document.getElementById('headerCartBadge');
            if (headerBadge) headerBadge.innerText = cartCount;
        }
    </script>

</body>
</html>
