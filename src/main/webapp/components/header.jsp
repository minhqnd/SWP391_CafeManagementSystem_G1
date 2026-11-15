<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<style>
    /* Shared Header Navbar Styling */
    .aura-navbar {
        font-family: 'Inter', sans-serif;
        font-size: 1rem;
        line-height: 1.5;
        background-color: #ffffff;
        box-shadow: 0 2px 10px rgba(0,0,0,0.04);
        position: sticky;
        top: 0;
        z-index: 1030;
    }
    .aura-nav-link {
        color: #495057;
        position: relative;
        font-weight: 500;
        padding: 0.5rem 0.85rem !important;
        transition: color 0.2s ease;
        text-decoration: none;
    }
    .aura-nav-link:hover {
        color: #900b1a;
    }
    .aura-nav-link.active-nav {
        color: #900b1a !important;
        font-weight: 700;
    }
    .aura-nav-link.active-nav::after {
        content: '';
        position: absolute;
        bottom: -6px;
        left: 15%;
        width: 70%;
        height: 3px;
        background-color: #900b1a;
        border-radius: 2px;
    }

    /* User Profile Dropdown Styling */
    .user-dropdown-menu {
        min-width: 250px;
        padding: 0.5rem;
        border-radius: 14px;
        border: 1px solid #e2e8f0;
        box-shadow: 0 12px 32px -4px rgba(0, 0, 0, 0.12);
        animation: userDropFade 0.18s cubic-bezier(0.16, 1, 0.3, 1);
        margin-top: 0.65rem !important;
    }
    @keyframes userDropFade {
        from { opacity: 0; transform: translateY(-6px); }
        to { opacity: 1; transform: translateY(0); }
    }
    .user-dropdown-header {
        padding: 0.75rem 0.85rem;
        background: #f8fafc;
        border-radius: 10px;
        margin-bottom: 0.4rem;
        border: 1px solid #f1f5f9;
    }
    .user-item-btn {
        display: flex;
        align-items: center;
        gap: 0.65rem;
        padding: 0.55rem 0.85rem;
        border-radius: 8px;
        font-size: 0.86rem;
        font-weight: 500;
        color: #334155;
        transition: all 0.15s ease;
        text-decoration: none;
    }
    .user-item-btn:hover {
        background: #f1f5f9;
        color: #900b1a;
    }
    .user-item-btn.logout-btn:hover {
        background: #fef2f2;
        color: #dc2626;
    }
    .user-item-btn .item-icon {
        font-size: 19px;
        color: #64748b;
        transition: color 0.15s ease;
    }
    .user-item-btn:hover .item-icon {
        color: #900b1a;
    }
    .user-item-btn.logout-btn:hover .item-icon {
        color: #dc2626;
    }

    /* Homepage header styled as a compact storefront */
    .aura-home-navbar { padding:0 !important; min-height:64px; }
    .aura-home-navbar > .container-fluid { max-width:960px; min-height:64px; margin:0 auto; padding-left:12px !important; padding-right:12px !important; display:flex; align-items:center; }
    .aura-home-navbar .navbar-brand { margin:0 24px 0 0 !important; }
    .aura-home-navbar .navbar-brand .bg-danger { border-radius:7px !important; }
    .aura-home-search { width:min(360px, 36vw); margin:0 auto; display:flex; align-items:center; height:36px; background:#f2f2f2; border:1px solid #e7e7e7; border-radius:20px; overflow:hidden; }
    .aura-home-search input { flex:1; min-width:0; border:0; outline:0; background:transparent; padding:0 14px; font-size:.78rem; color:#495057; }
    .aura-home-search button { width:42px; height:100%; border:0; background:#e8dfd5; color:#1769cc; display:grid; place-items:center; }
    .aura-home-navbar #mainNavbar { flex-grow:0; }
    .aura-home-secondary { min-height:40px; background:#b32830; }
    .aura-home-secondary .container { max-width:960px; min-height:40px; display:flex; align-items:stretch; gap:0; position:relative; }
    .aura-home-secondary .menu-category-dropdown { display:none; position:absolute; z-index:1060; top:100%; left:0; width:220px; max-height:min(70vh, 440px); overflow-y:auto; background:#fff; border:1px solid #dedede; border-top:0; box-shadow:0 8px 20px rgba(20,20,20,.16); }
    .aura-home-secondary .menu-category-dropdown.show { display:block; }
    .aura-home-secondary .menu-category-option { min-height:36px; display:flex; align-items:center; gap:8px; padding:7px 10px; color:#27313d; text-decoration:none; font-size:.78rem; font-weight:500; border-bottom:1px solid #f0f0f0; }
    .aura-home-secondary .menu-category-option:hover, .aura-home-secondary .menu-category-option.active { color:#a51f28; background:#fff5f4; }
    .aura-home-secondary .menu-category-option .material-symbols-outlined { font-size:18px; }
    .aura-home-secondary .menu-category-option img { width:30px; height:28px; flex:0 0 30px; object-fit:cover; border-radius:4px; background:#f4ece7; }
    .aura-home-secondary .menu-category-option > span:nth-child(2) { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
    .aura-home-secondary .menu-category-count { margin-left:auto; color:#818995; font-size:.7rem; }
    .aura-home-secondary a { min-height:40px; display:inline-flex; align-items:center; gap:7px; padding:0 13px; color:#fff; text-decoration:none; font-size:.82rem; font-weight:600; white-space:nowrap; }
    .aura-home-secondary a:hover, .aura-home-secondary a.active-home-nav { background:#9e2028; color:#fff; }
    .aura-home-secondary .home-category-static { background:#fff; color:#a51f28; border-radius:4px 4px 0 0; min-width:172px; }
    .aura-home-secondary .home-category-static:hover { background:#fff; color:#8e111d; }
    @media(max-width:991.98px) {
        .aura-home-navbar > .container-fluid { max-width:100%; }
        .aura-home-search { width:min(300px, 35vw); }
        .aura-home-secondary .container { max-width:100%; overflow-x:auto; }
    }
    @media(max-width:575.98px) {
        .aura-home-navbar > .container-fluid { flex-wrap:wrap; padding-top:8px; padding-bottom:8px; }
        .aura-home-navbar .navbar-brand { margin-right:auto !important; }
        .aura-home-search { order:3; width:100%; margin:8px 0 0; }
        .aura-home-navbar #mainNavbar { margin-left:auto; }
    }
</style>

<nav class="navbar navbar-expand-lg aura-navbar py-3 ${param.navbarMode != 'barista' ? 'aura-home-navbar' : 'barista-topbar'}">
    <div class="container-fluid px-4 px-lg-5">
        <!-- Brand Logo -->
        <c:set var="auraBrandMarkup">
            <div class="bg-danger text-white rounded-3 p-2 d-flex align-items-center justify-content-center" style="width: 40px; height: 40px;">
                <span class="material-symbols-outlined fs-4">local_cafe</span>
            </div>
            <div class="d-flex flex-column text-start">
                <span class="fs-4 lh-1 text-dark fw-bold">Aura Café</span>
                <span class="text-secondary fw-semibold" style="font-size: 0.65rem; letter-spacing: 1px; text-transform: uppercase;">Specialty Coffee</span>
            </div>
        </c:set>
        <c:choose>
            <c:when test="${param.navbarMode == 'barista'}">
                <span class="navbar-brand fw-bold text-danger d-flex align-items-center gap-2 me-4">
                    ${auraBrandMarkup}
                </span>
            </c:when>
            <c:otherwise>
                <a class="navbar-brand fw-bold text-danger d-flex align-items-center gap-2 me-4"
                   href="${pageContext.request.contextPath}/">
                    ${auraBrandMarkup}
                </a>
            </c:otherwise>
        </c:choose>

        <c:if test="${param.navbarMode != 'barista'}">
            <form class="aura-home-search" action="${pageContext.request.contextPath}/menu" method="get" role="search">
                <input type="search" name="keyword" placeholder="Xin chào, bạn cần tìm gì?" aria-label="Tìm món trong thực đơn" />
                <button type="submit" aria-label="Tìm kiếm"><span class="material-symbols-outlined">search</span></button>
            </form>
        </c:if>

        <!-- Mobile Toggler -->
        <button class="navbar-toggler border-0 shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#mainNavbar"
                aria-controls="mainNavbar" aria-expanded="false" aria-label="Mở điều hướng">
            <span class="material-symbols-outlined">menu</span>
        </button>

        <div class="collapse navbar-collapse" id="mainNavbar">
            <!-- Navigation Links -->
            <c:if test="${param.navbarMode == 'barista'}">
            <ul class="navbar-nav me-auto mb-2 mb-lg-0 gap-1 gap-lg-2 barista-tabs" style="font-size: 0.95rem;">
                <c:choose>
                    <c:when test="${param.navbarMode == 'barista'}">

                <li class="nav-item">
                            <a id="orders-link"
                               class="nav-link aura-nav-link ${param.activePage == 'barista-orders' ? 'active-nav' : ''}"
                               href="${pageContext.request.contextPath}/barista">
                                Đơn pha chế
                            </a>
                        </li>
                        <li class="nav-item">
                            <a id="completed-link"
                               class="nav-link aura-nav-link ${param.activePage == 'barista-completed' ? 'active-nav' : ''}"
                               href="${pageContext.request.contextPath}/barista?action=completed">
                                Đơn đã pha
                            </a>
                        </li>
                        <li class="nav-item"><a class="nav-link aura-nav-link ${param.activePage == 'inventory' ? 'active-nav' : ''}" href="${pageContext.request.contextPath}/barista/restock-requests">Đề xuất &amp; nhận hàng</a></li>
                    </c:when>
                    <c:otherwise>


                <li class="nav-item">
                    <a class="nav-link aura-nav-link ${param.activePage == 'home' ? 'active-nav' : ''}" 
                       href="${pageContext.request.contextPath}/">Trang chủ</a>
                </li>

                <li class="nav-item">
                    <a class="nav-link aura-nav-link ${param.activePage == 'menu' ? 'active-nav' : ''}" 
                       href="${pageContext.request.contextPath}/menu">Menu & Đồ uống</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link aura-nav-link ${param.activePage == 'feedback' ? 'active-nav' : ''}" 
                       href="${pageContext.request.contextPath}/feedback">Phản ánh & Khiếu nại</a>
                </li>

                    </c:otherwise>
                </c:choose>
            </ul>
        </c:if>

            <!-- Right Actions: Cart & Profile -->
            <div class="d-flex align-items-center gap-3 ms-lg-auto header-actions">
                <!-- Cart Button -->
                <c:if test="${param.showCart != 'false' && param.navbarMode != 'barista'}">
                <a href="${pageContext.request.contextPath}/cart" 
                   class="text-decoration-none text-dark d-flex align-items-center gap-2 px-3 py-2 rounded-pill bg-light border"
                   style="transition: background-color 0.2s;" title="Giỏ hàng">
                    <span class="material-symbols-outlined fs-5 text-secondary">shopping_bag</span>
                    <span class="fw-semibold d-none d-sm-inline" style="font-size: 0.88rem;">Giỏ hàng</span>
                    <span id="headerCartBadge" class="badge rounded-pill bg-danger" style="font-size: 0.72rem;">${empty sessionScope.cart ? 0 : sessionScope.cart.totalQuantity}</span>
                </a>

                </c:if>
                <!-- Profile / Auth Button -->
                <c:choose>
                    <c:when test="${not empty sessionScope.user}">
                        <c:choose>
                        <c:when test="${param.navbarMode == 'barista'}">
                            <div class="d-flex align-items-center gap-2 text-dark header-user">
                                <div class="rounded-circle bg-light d-flex align-items-center justify-content-center border" style="width: 40px; height: 40px;">
                                    <span class="material-symbols-outlined text-danger">account_circle</span>
                                </div>
                                <div class="d-none d-xl-flex flex-column text-start">
                                    <span class="fw-semibold text-truncate" style="font-size: 0.85rem; max-width: 130px;"><c:out value="${sessionScope.user.fullName}" /></span>
                                </div>
                            </div>
                        </c:when>
                        <c:otherwise>
                        <div class="dropdown">
                            <a href="#" class="d-flex align-items-center gap-2 text-decoration-none text-dark header-user" data-bs-toggle="dropdown">
                                <div class="rounded-circle bg-light d-flex align-items-center justify-content-center border" style="width: 40px; height: 40px;">
                                    <span class="material-symbols-outlined text-danger">account_circle</span>
                                </div>
                                <div class="d-none d-xl-flex flex-column text-start">
                                    <span class="fw-semibold text-truncate" style="font-size: 0.85rem; max-width: 130px;"><c:out value="${sessionScope.user.fullName}" /></span>
                                    <span class="text-danger fw-bold" style="font-size: 0.7rem;">${sessionScope.user.role}</span>
                                </div>
                            </a>
                            <ul class="dropdown-menu dropdown-menu-end user-dropdown-menu">
                                <!-- Header Info -->
                                <li class="user-dropdown-header">
                                    <div class="d-flex align-items-center justify-content-between mb-1">
                                        <span class="fw-bold text-dark text-truncate" style="max-width: 150px; font-size: 0.92rem;"><c:out value="${sessionScope.user.fullName}" /></span>
                                        <c:if test="${sessionScope.user.role != 'CUSTOMER'}">
                                            <span class="badge bg-danger-subtle text-danger" style="font-size: 0.68rem; font-weight: 700;">${sessionScope.user.role}</span>
                                        </c:if>
                                    </div>
                                    <div class="d-flex align-items-center justify-content-between text-muted" style="font-size: 0.78rem;">
                                        <span><c:out value="${not empty sessionScope.user.phone ? sessionScope.user.phone : sessionScope.user.email}" /></span>
                                        <c:if test="${sessionScope.user.totalPoints > 0}">
                                            <span class="text-warning-emphasis fw-bold">• ${sessionScope.user.totalPoints} điểm</span>
                                        </c:if>
                                    </div>
                                </li>

                                <!-- Đơn hàng của tôi -->
                                <li>
                                    <a class="user-item-btn" href="${pageContext.request.contextPath}/orders">
                                        <span class="material-symbols-outlined item-icon">receipt_long</span>
                                        <span class="flex-grow-1">Đơn hàng của tôi</span>
                                        <span class="material-symbols-outlined" style="font-size: 16px; color: #94a3b8;">chevron_right</span>
                                    </a>
                                </li>

                                <!-- Hồ sơ & Điểm tích luỹ -->
                                <li>
                                    
                                    <%-- PF01: [SLBF10] Select "Personal Profile"
                                         . Chọn Hồ sơ & Điểm tích luỹ trong menu tài khoản đã đăng nhập.
                                         ... GET /profile → ProfileController.doGet() [PF02] → profile.jsp [PF06]. --%>
                                    <a class="user-item-btn" href="${pageContext.request.contextPath}/profile">
                                        <span class="material-symbols-outlined item-icon">account_circle</span>
                                        <span class="flex-grow-1">Hồ sơ & Điểm tích luỹ</span>
                                        <span class="material-symbols-outlined" style="font-size: 16px; color: #94a3b8;">chevron_right</span>
                                    </a>
                                </li>

                                <!-- Phím tắt nhanh cho Staff / Barista / Cashier / Admin -->
                                <c:if test="${sessionScope.user.role eq 'ADMIN' || sessionScope.user.role eq 'STAFF'}">
                                    <li><hr class="dropdown-divider my-1 border-light-subtle"></li>
                                    <c:if test="${sessionScope.user.position eq 'Cashier' || sessionScope.user.role eq 'ADMIN'}">
                                        <li>
                                            <a class="user-item-btn" href="${pageContext.request.contextPath}/cashier">
                                                <span class="material-symbols-outlined item-icon">point_of_sale</span>
                                                <span class="flex-grow-1">Màn hình Thu ngân POS</span>
                                            </a>
                                        </li>
                                    </c:if>
                                    <c:if test="${sessionScope.user.position eq 'Barista' || sessionScope.user.role eq 'ADMIN'}">
                                        <li>
                                            <a class="user-item-btn" href="${pageContext.request.contextPath}/barista">
                                                <span class="material-symbols-outlined item-icon">coffee_maker</span>
                                                <span class="flex-grow-1">Màn hình Pha chế KDS</span>
                                            </a>
                                        </li>
                                    </c:if>
                                </c:if>

                                <li><hr class="dropdown-divider my-1 border-light-subtle"></li>

                                <!-- Đăng xuất -->
                                <li>
                                    
                                    <%-- LG09: [SLBF10] Click "Logout"
                                         . Bấm Đăng xuất trong menu tài khoản để kết thúc phiên.
                                         ... GET /login?action=logout → LoginController.doGet() [LG10] → Homepage. --%>
                                    <a class="user-item-btn logout-btn" href="${pageContext.request.contextPath}/login?action=logout">
                                        <span class="material-symbols-outlined item-icon" style="color: #dc2626;">logout</span>
                                        <span class="text-danger fw-semibold">Đăng xuất</span>
                                    </a>
                                </li>
                            </ul>
                        </div>
                        </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:otherwise>
                        <%-- . Bấm biểu tượng tài khoản ở header khi chưa đăng nhập để mở authModal.
                             .. Bootstrap chỉ mở hộp thoại; chưa gửi dữ liệu tài khoản.
                             ... auth_modal.jsp: chọn Đăng ký [RG01] hoặc Đăng nhập [LG01]. --%>
                        <button type="button" class="btn btn-outline-danger rounded-circle p-2 d-flex align-items-center justify-content-center" 
                                style="width: 42px; height: 42px;" data-bs-toggle="modal" data-bs-target="#authModal" title="Đăng nhập / Đăng ký">
                            <span class="material-symbols-outlined fs-5">person</span>
                        </button>
                    </c:otherwise>
                </c:choose>
                <c:if test="${param.navbarMode == 'barista' && not empty sessionScope.user}">
                    <a href="${pageContext.request.contextPath}/login?action=logout" class="barista-logout" title="Đăng xuất khỏi hệ thống">
                        <span class="material-symbols-outlined">logout</span><span>Đăng xuất</span>
                    </a>
                </c:if>
            </div>
        </div>
    </div>
</nav>
<c:if test="${param.navbarMode != 'barista'}">
    <nav class="aura-home-secondary" aria-label="Điều hướng cửa hàng">
        <div class="container">
                        <c:choose>
                <c:when test="${param.activePage == 'home'}"><a class="home-category-static" href="#homeCategoryList"><span class="material-symbols-outlined">grid_view</span> Danh mục sản phẩm</a></c:when>
                <c:when test="${param.activePage == 'menu'}"><a class="menu-category-toggle" href="#menuCategoryDropdown" aria-controls="menuCategoryDropdown"><span class="material-symbols-outlined">menu</span> Danh mục sản phẩm</a></c:when>
                <c:otherwise><a href="${pageContext.request.contextPath}/menu#menuCategoryDropdown"><span class="material-symbols-outlined">menu</span> Danh mục sản phẩm</a></c:otherwise>
            </c:choose>
            <%-- MN01: [SLBF11] Open Menu
                 . Bấm Menu & Đồ uống gửi GET /menu; không yêu cầu đăng nhập.
                 ... MenuController.doGet() [MN02] tải danh mục và sản phẩm. --%>
            <a class="${param.activePage == 'menu' ? 'active-home-nav' : ''}" href="${pageContext.request.contextPath}/menu">Menu &amp; Đồ uống</a>
            <a class="${param.activePage == 'feedback' ? 'active-home-nav' : ''}" href="${pageContext.request.contextPath}/feedback">Liên hệ &amp; Góp ý</a>
            <a href="${pageContext.request.contextPath}/profile"><span class="material-symbols-outlined">stars</span> Thành viên Aura</a>
            <c:if test="${param.activePage == 'menu'}">
                <div class="menu-category-dropdown" id="menuCategoryDropdown">
                    <c:forEach var="cat" items="${categories}">
                        <c:url var="menuCategoryUrl" value="/menu">
                            <c:param name="category" value="${cat.id}" />
                            <c:if test="${not empty currentKeyword}"><c:param name="keyword" value="${currentKeyword}" /></c:if>
                        </c:url>
                        <a class="menu-category-option ${currentCategory == cat.id ? 'active' : ''}" href="${menuCategoryUrl}">
                            <c:url var="storedCategoryImage" value="/category-image"><c:param name="id" value="${cat.id}" /></c:url>
                            <img src="${cat.storedImage ? storedCategoryImage : (not empty cat.imageUrl ? cat.imageUrl : 'https://images.unsplash.com/photo-1511920170033-f8396924c348?w=80&amp;h=80&amp;fit=crop')}" alt="${cat.name}" loading="lazy" />
                            <span>${cat.name}</span>
                            <span class="menu-category-count">${cat.productCount}</span>
                        </a>
                    </c:forEach>
                </div>
            </c:if>
        </div>
    </nav>
</c:if>
