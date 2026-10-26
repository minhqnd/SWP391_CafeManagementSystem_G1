<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page import="dao.ProductDAO" %>
<%@ page import="dao.CategoryDAO" %>
<%@ page import="dao.ShopSettingDAO" %>
<%@ page import="model.ShopSettings" %>
<%@ page import="model.Product" %>
<%@ page import="model.Category" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.DecimalFormat" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Aura Café | Trang Chủ</title>
    
    <!-- Google Fonts & Icons -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet"/>
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet"/>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Shared Product Card & Modal CSS -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/product-card.css">
    
    <style>
        :root {
            --primary: #900b1a;
            --primary-container: #b2292e;
            --secondary: #555f71;
            --surface-container-lowest: #ffffff;
            --surface-container-low: #f8f9ff;
            --surface-container: #eff4ff;
            --surface-container-high: #dee9fc;
            --surface-container-highest: #d1e0fa;
            --tertiary: #6b5c3f;
            --tertiary-container: #95815a;
        }
        body { font-family: 'Inter', sans-serif; background-color: var(--surface-container-lowest); color: #1a1c1e; overflow-x: hidden; }
        
        .bg-primary-custom { background-color: var(--primary) !important; color: white !important; }
        .text-primary-custom { color: var(--primary) !important; }
        .text-secondary-custom { color: var(--secondary) !important; }
        
        /* Hero Section */
        .hero-section { background-color: var(--surface-container-low); position: relative; padding: 5rem 0; overflow: hidden; }
        .blur-blob-1 { position: absolute; right: -5rem; top: -6rem; width: 24rem; height: 24rem; border-radius: 50%; background-color: rgba(144, 11, 26, 0.1); filter: blur(3rem); pointer-events: none; }
        .blur-blob-2 { position: absolute; left: 25%; bottom: -4rem; width: 20rem; height: 20rem; border-radius: 50%; background-color: rgba(149, 129, 90, 0.1); filter: blur(3rem); pointer-events: none; }
        
        .hero-title { font-size: 3.25rem; font-weight: 800; line-height: 1.15; letter-spacing: -0.025em; color: #0f172a; }
        @media (max-width: 768px) { .hero-title { font-size: 2.25rem; } }
        
        .btn-custom-primary { background-color: var(--primary); color: white; border-radius: 0.75rem; font-weight: 600; padding: 0.85rem 1.75rem; transition: all 0.2s ease; border: none; display: inline-flex; align-items: center; justify-content: center; text-decoration: none; box-shadow: 0 4px 14px rgba(144, 11, 26, 0.2); }
        .btn-custom-primary:hover { opacity: 0.95; color: white; transform: translateY(-1px); box-shadow: 0 6px 20px rgba(144, 11, 26, 0.28); }
        .btn-custom-secondary { background-color: #ffffff; color: #1e293b; border-radius: 0.75rem; font-weight: 600; padding: 0.85rem 1.75rem; transition: all 0.2s ease; border: 1px solid #e2e8f0; display: inline-flex; align-items: center; justify-content: center; text-decoration: none; box-shadow: 0 2px 8px rgba(0,0,0,0.03); }
        .btn-custom-secondary:hover { background-color: #f8fafc; color: #0f172a; border-color: #cbd5e1; transform: translateY(-1px); }
        
        .stats-box { background: #ffffff; border: 1px solid rgba(0, 0, 0, 0.06); border-radius: 1rem; padding: 1.25rem 1.5rem; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
        .stat-value { font-size: 1.35rem; font-weight: 800; color: var(--primary); letter-spacing: -0.02em; line-height: 1.2; margin-bottom: 0.25rem; }
        .stat-label { font-size: 0.8rem; color: #64748b; font-weight: 500; }
        
        .hero-img-container { position: relative; height: 440px; border-radius: 1.5rem; overflow: hidden; box-shadow: 0 1rem 3rem rgba(0,0,0,0.1); }
        .hero-img-bg { background-size: cover; background-position: center; width: 100%; height: 100%; transition: transform 0.7s; }
        .hero-img-container:hover .hero-img-bg { transform: scale(1.05); }
        .hero-img-overlay { position: absolute; inset: 0; background: linear-gradient(to top, rgba(0,0,0,0.85) 0%, rgba(0,0,0,0) 65%); display: flex; flex-direction: column; justify-content: flex-end; padding: 2rem; color: white; }
        
        
        /* Category Section */
        .category-section { background-color: var(--surface-container-low); padding: 5rem 0; }
        .cat-card {
            background: #ffffff;
            border-radius: 1.25rem;
            overflow: hidden;
            border: 1px solid var(--surface-container-high);
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
            text-decoration: none;
            display: flex;
            flex-direction: column;
            height: 100%;
            transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
        }
        .cat-card:hover {
            transform: translateY(-6px);
            box-shadow: 0 16px 32px rgba(144, 11, 26, 0.1);
            border-color: rgba(144, 11, 26, 0.35);
        }
        .cat-img-box {
            height: 170px;
            position: relative;
            overflow: hidden;
            background-color: #f1f5f9;
        }
        .cat-img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
        }
        .cat-card:hover .cat-img {
            transform: scale(1.08);
        }
        .cat-icon-badge {
            position: absolute;
            bottom: 12px;
            left: 14px;
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(8px);
            color: var(--primary);
            width: 38px;
            height: 38px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
        }
        .cat-body {
            padding: 1.25rem;
            display: flex;
            flex-direction: column;
            flex-grow: 1;
            justify-content: space-between;
        }
        .cat-title {
            font-size: 1.125rem;
            font-weight: 700;
            color: #1a1c1e;
            margin-bottom: 0.25rem;
            transition: color 0.2s;
        }
        .cat-card:hover .cat-title {
            color: var(--primary);
        }
        .cat-meta {
            font-size: 0.85rem;
            color: #64748b;
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-top: 0.75rem;
            padding-top: 0.75rem;
            border-top: 1px solid #f1f5f9;
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

        /* Homepage shop layout inspired by the reference storefront */
        .home-store-layout { max-width:960px; padding-top:14px; }
        .home-category-panel { min-height:385px; border:1px solid #e1e1e1; border-radius:3px; background:#fff; overflow:hidden; }
        .home-category-list[hidden] { display:none !important; }
        .home-category-link { display:flex; align-items:center; gap:9px; padding:9px 13px; color:#26313d; text-decoration:none; font-size:.82rem; border-bottom:1px solid #f3f3f3; transition:background .15s,color .15s; }
        .home-category-link:last-child { border-bottom:0; }
        .home-category-link:hover { color:var(--primary); background:#fff5f4; }
        .home-category-image { width:38px; height:32px; flex:0 0 38px; object-fit:cover; border-radius:4px; background:#f4ece7; }
        .home-category-link > span:nth-child(2) { min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
        .home-category-count { margin-left:auto; color:#8a929d; font-size:.72rem; }
        .home-banner { height:385px; overflow:hidden; border-radius:3px; background:#6b3429; position:relative; }
        .home-banner-slide { position:absolute; inset:0; opacity:0; visibility:hidden; transition:opacity .55s ease; background-size:cover; background-position:center; }
        .home-banner-slide.active { opacity:1; visibility:visible; }
        .home-banner-slide::after { content:''; position:absolute; inset:0; background:linear-gradient(90deg,rgba(15,10,8,.68),rgba(15,10,8,.2) 58%,rgba(15,10,8,.06)); }
        .home-banner-copy { position:absolute; z-index:1; left:7%; bottom:13%; max-width:56%; color:white; text-shadow:0 2px 14px #0008; }
        .home-banner-copy .eyebrow { text-transform:uppercase; letter-spacing:2px; font-size:.76rem; color:#ffd7be; font-weight:700; }
        .home-banner-copy h1 { font-size:clamp(1.7rem,3vw,2.7rem); font-weight:800; line-height:1.1; margin:.5rem 0; }
        .home-banner-copy p { font-size:.9rem; color:#f2e9e4; margin-bottom:1rem; }
        .home-banner-arrow { position:absolute; z-index:2; top:50%; transform:translateY(-50%); width:38px; height:38px; border:0; border-radius:50%; background:#fffD; color:#5d2825; font-size:1.35rem; }
        .home-banner-arrow:hover { background:#fff; }
        .home-banner-prev { left:14px; } .home-banner-next { right:14px; }
        .home-banner-dots { position:absolute; z-index:2; bottom:14px; left:50%; transform:translateX(-50%); display:flex; gap:7px; }
        .home-banner-dot { width:18px; height:4px; padding:0; border:0; background:#fff8; }
        .home-banner-dot.active { background:#fff; }
        .home-perks { padding:24px 0 30px; }
        .home-perk { height:100%; display:flex; align-items:center; gap:12px; padding:14px; border:1px solid #e8e8e8; border-radius:5px; background:#fff; box-shadow:0 2px 7px #18202a0a; }
        .home-perk-icon { width:52px; height:52px; flex:0 0 52px; display:grid; place-items:center; border-radius:4px; color:#a5212a; background:#fff0e8; }
        .home-perk-icon .material-symbols-outlined { font-size:27px; }
        .home-perk strong { display:block; font-size:.79rem; color:#252525; text-transform:uppercase; }
        .home-perk span { display:block; font-size:.73rem; color:#6b7280; line-height:1.35; margin-top:3px; }
        .home-products-section { padding:8px 0 42px; }
        .home-products-heading { border-bottom:1px solid #e6e6e6; padding-bottom:13px; margin-bottom:18px; }
        .home-products-heading h2 { margin:0; color:#252525; font-size:1.35rem; font-weight:700; }
        .home-products-heading a { color:var(--primary); font-weight:600; text-decoration:none; font-size:.84rem; }
        @media(max-width:767.98px) {
            .home-store-layout { padding-top:10px; }
        .home-category-panel { min-height:0; margin-bottom:12px; }
        .home-category-list[hidden] { display:none !important; }
            .home-category-list { display:flex; overflow:auto; }
            .home-category-link { flex:0 0 auto; border:0; padding:8px 10px; }
            .home-category-count { display:none; }
            .home-category-image { width:42px; height:34px; flex-basis:42px; }
            .home-banner { height:290px; }
            .home-banner-copy { max-width:76%; left:7%; bottom:12%; }
            .home-banner-copy h1 { font-size:1.55rem; }
            .home-banner-copy p { font-size:.8rem; }
            .home-perks { padding:17px 0 20px; }
            .home-perk { padding:10px; }
            .home-perk-icon { width:42px; height:42px; flex-basis:42px; }
            .home-products-heading h2 { font-size:1.15rem; }
        }
    </style>
</head>
<body>

    <!-- Shared Header Navbar Component -->
    <jsp:include page="/components/header.jsp">
        <jsp:param name="activePage" value="home" />
    </jsp:include>

    <!-- Fetch Data for Hero and Best Sellers -->
    <%
        ProductDAO productDAO = new ProductDAO();
        List<Product> productsList = productDAO.getBestSellingProducts(4);
        request.setAttribute("bestSellerProducts", productsList);
        if (productsList != null && !productsList.isEmpty()) {
            request.setAttribute("heroProduct", productsList.get(0));
        }
        DecimalFormat formatter = new DecimalFormat("###,###,###");
    %>

    <%
        CategoryDAO categoryDAO = new CategoryDAO();
        List<Category> landingCategories = categoryDAO.getAllActiveCategoriesWithCount();
        request.setAttribute("landingCategories", landingCategories);
        ShopSettings homeSettings = new ShopSettingDAO().getSettings();
        request.setAttribute("homeSettings", homeSettings);
    %>
    <main class="container home-store-layout">
        <div class="row g-2 g-lg-3 align-items-stretch home-hero-row">
            <aside class="col-md-3 col-lg-2" id="homeCategories">
                <div class="home-category-panel">
                    <div class="home-category-list" id="homeCategoryList">
                        <c:forEach var="cat" items="${landingCategories}">
                            <a class="home-category-link" href="${pageContext.request.contextPath}/menu?category=${cat.id}">
                                <c:url var="storedCategoryImage" value="/category-image"><c:param name="id" value="${cat.id}" /></c:url>
                                <img class="home-category-image" src="${cat.storedImage ? storedCategoryImage : (not empty cat.imageUrl ? cat.imageUrl : 'https://images.unsplash.com/photo-1511920170033-f8396924c348?w=160&h=120&fit=crop')}" alt="${cat.name}" loading="lazy" />
                                <span>${cat.name}</span>
                                <span class="home-category-count">${cat.productCount}</span>
                            </a>
                        </c:forEach>
                        <c:if test="${empty landingCategories}">
                            <a class="home-category-link" href="${pageContext.request.contextPath}/menu"><img class="home-category-image" src="https://images.unsplash.com/photo-1511920170033-f8396924c348?w=160&amp;h=120&amp;fit=crop" alt="Thực đơn Aura"/><span>Xem thực đơn</span></a>
                        </c:if>
                    </div>
                </div>
            </aside>
            <div class="col-md-9 col-lg-10">
                <section class="home-banner" id="auraHomeBanner" aria-label="Món nổi bật Aura">
                    <c:forEach var="bannerProduct" items="${bestSellerProducts}" varStatus="slide">
                        <div class="home-banner-slide ${slide.first ? 'active' : ''}" style="background-image:url('${bannerProduct.displayImageUrl}')" aria-hidden="${!slide.first}">
                            <div class="home-banner-copy">
                                <span class="eyebrow">${slide.first ? 'Hương vị đặc trưng Aura' : 'Được khách hàng yêu thích'}</span>
                                <h1>${bannerProduct.name}</h1>
                                <p>${bannerProduct.description}</p>
                                <a class="btn btn-light btn-sm fw-bold px-3 py-2" href="${pageContext.request.contextPath}/menu">Khám phá menu</a>
                            </div>
                        </div>
                    </c:forEach>
                    <c:if test="${empty bestSellerProducts}">
                        <div class="home-banner-slide active" style="background:linear-gradient(120deg,#4c211d,#b64c35)">
                            <div class="home-banner-copy"><span class="eyebrow">Chào mừng đến Aura Café</span><h1>Một chất phin, trọn vị thân quen</h1><p>Khám phá đồ uống và món ngon được chuẩn bị mỗi ngày.</p><a class="btn btn-light btn-sm fw-bold px-3 py-2" href="${pageContext.request.contextPath}/menu">Khám phá menu</a></div>
                        </div>
                    </c:if>
                    <button class="home-banner-arrow home-banner-prev" type="button" aria-label="Ảnh trước">‹</button>
                    <button class="home-banner-arrow home-banner-next" type="button" aria-label="Ảnh tiếp theo">›</button>
                    <div class="home-banner-dots" aria-label="Chọn ảnh"></div>
                </section>
            </div>
        </div>

        <section class="home-perks" aria-label="Thông tin dịch vụ Aura">
            <div class="row row-cols-1 row-cols-sm-2 row-cols-xl-4 g-3">
                <div class="col"><div class="home-perk"><div class="home-perk-icon"><span class="material-symbols-outlined">local_shipping</span></div><div><strong>Miễn phí giao hàng</strong><span><c:choose><c:when test="${homeSettings.freeshipThreshold == 0}">Áp dụng cho mọi đơn giao hàng</c:when><c:otherwise>Cho đơn từ <fmt:formatNumber value="${homeSettings.freeshipThreshold}" pattern="#,##0"/>đ</c:otherwise></c:choose></span></div></div></div>
                <div class="col"><div class="home-perk"><div class="home-perk-icon"><span class="material-symbols-outlined">payments</span></div><div><strong>Phí giao hàng</strong><span><fmt:formatNumber value="${homeSettings.deliveryFee}" pattern="#,##0"/>đ nếu đơn chưa đạt mức miễn phí</span></div></div></div>
                <div class="col"><div class="home-perk"><div class="home-perk-icon"><span class="material-symbols-outlined">schedule</span></div><div><strong>Giao hàng dự kiến</strong><span>Khoảng ${homeSettings.deliveryMinutes} phút sau khi xác nhận</span></div></div></div>
                <div class="col"><div class="home-perk"><div class="home-perk-icon"><span class="material-symbols-outlined">loyalty</span></div><div><strong>Tích điểm Aura</strong><span>Mỗi ${homeSettings.pointsStep} điểm đổi ${homeSettings.pointsStepValue}đ ưu đãi</span></div></div></div>
            </div>
        </section>

        <section class="home-products-section" id="homeFeaturedProducts">
            <div class="home-products-heading d-flex flex-column flex-sm-row justify-content-between align-items-sm-end gap-2">
                <div><div class="text-primary-custom fw-bold text-uppercase mb-1" style="font-size:.72rem;letter-spacing:1.5px">Một chất phin từ Aura</div><h2>Sản phẩm được yêu thích</h2></div>
                <a href="${pageContext.request.contextPath}/menu">Xem toàn bộ thực đơn <span class="material-symbols-outlined align-middle" style="font-size:18px">chevron_right</span></a>
            </div>
            <div class="row row-cols-2 row-cols-md-3 row-cols-lg-4 g-3 g-lg-4">
                <c:forEach var="p" items="${bestSellerProducts}">
                    <div class="col"><c:set var="product" value="${p}" scope="request"/><jsp:include page="/components/product_card.jsp"/></div>
                </c:forEach>
            </div>
        </section>
    </main>
    <script>
    (function () {
        const banner = document.getElementById('auraHomeBanner');
        if (!banner) return;
        const slides = Array.from(banner.querySelectorAll('.home-banner-slide'));
        const dots = banner.querySelector('.home-banner-dots');
        let current = 0;
        slides.forEach((slide, index) => {
            const dot = document.createElement('button');
            dot.type = 'button'; dot.className = 'home-banner-dot' + (index === 0 ? ' active' : '');
            dot.setAttribute('aria-label', 'Hiển thị ảnh ' + (index + 1));
            dot.addEventListener('click', () => show(index)); dots.appendChild(dot);
        });
        const dotButtons = Array.from(dots.querySelectorAll('button'));
        function show(index) {
            current = (index + slides.length) % slides.length;
            slides.forEach((slide, i) => { slide.classList.toggle('active', i === current); slide.setAttribute('aria-hidden', i !== current); });
            dotButtons.forEach((dot, i) => dot.classList.toggle('active', i === current));
        }
        banner.querySelector('.home-banner-prev').addEventListener('click', () => show(current - 1));
        banner.querySelector('.home-banner-next').addEventListener('click', () => show(current + 1));
        if (slides.length > 1) window.setInterval(() => show(current + 1), 6000);
    })();
    </script>


    <!-- Shared Footer Component -->
    <jsp:include page="/components/footer.jsp" />

    <!-- Sticky Bottom Selection Bar (Hiện khi đang chọn món - Bấm Xem món ra popup) -->
    <div id="stickyMiniCart" class="sticky-cart-bar ${empty sessionScope.cart || sessionScope.cart.totalQuantity == 0 ? 'd-none' : ''}" onclick="openSelectedItemsModal()"
        style="cursor: pointer;" title="Bấm để xem danh sách món">
        <div class="d-flex align-items-center gap-3">
            <div class="cart-badge-icon">
                <span class="material-symbols-outlined fs-5">shopping_bag</span>
                <span id="stickyCartBadge" class="badge-count-pill">${empty sessionScope.cart ? 0 : sessionScope.cart.totalQuantity}</span>
            </div>
            <div class="fw-bold text-white" style="font-size: 0.95rem;">
                Đang chọn <span id="stickyCartCount" class="text-danger fw-bold">${empty sessionScope.cart ? 0 : sessionScope.cart.totalQuantity}</span> món
            </div>
        </div>
        <button type="button"
            class="btn btn-danger rounded-pill px-4 py-2 fw-semibold d-flex align-items-center gap-2 shadow-sm"
            onclick="event.stopPropagation(); openSelectedItemsModal()" title="Xem danh sách món đang chọn">
            <span>Xem món</span>
            <span class="material-symbols-outlined fs-6">arrow_forward</span>
        </button>
    </div>

    <!-- Reusable Product Detail & Customization Modal Component -->
    <jsp:include page="/components/product_detail_modal.jsp" />

    <!-- Nhúng Component Auth Modal -->
    <% if(session.getAttribute("user") == null || "login".equals(request.getParameter("tab"))) { %>
        <jsp:include page="components/auth_modal.jsp" />
    <% } %>

    <!-- Bootstrap 5 JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

    <!-- Script Check Login URL Param -->
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            // Check for login query parameter
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.get('login') === 'true') {
                const authModal = new bootstrap.Modal(document.getElementById('authModal'));
                authModal.show();
                // Remove parameter from URL without refreshing
                window.history.replaceState({}, document.title, window.location.pathname);
            }
        });
    </script>
</body>
</html>
