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
</style>

<nav class="navbar navbar-expand-lg aura-navbar py-3">
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

        <!-- Mobile Toggler -->
        <button class="navbar-toggler border-0 shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#mainNavbar"
                aria-controls="mainNavbar" aria-expanded="false" aria-label="Mở điều hướng">
            <span class="material-symbols-outlined">menu</span>
        </button>

        <div class="collapse navbar-collapse" id="mainNavbar">
            <!-- Navigation Links -->
            <ul class="navbar-nav me-auto mb-2 mb-lg-0 gap-1 gap-lg-2" style="font-size: 0.95rem;">
                <c:choose>
                    <c:when test="${param.navbarMode == 'barista'}">
                        <li class="nav-item">
                            <a id="orders-link"
                               class="nav-link aura-nav-link ${param.activePage == 'barista-orders' ? 'active-nav' : ''}"
                               href="${pageContext.request.contextPath}/barista">
                                Đơn pha chế <span class="d-none d-sm-inline">(F1)</span>
                            </a>
                        </li>
                        <li class="nav-item">
                            <a id="recipes-link"
                               class="nav-link aura-nav-link ${param.activePage == 'barista-recipes' ? 'active-nav' : ''}"
                               href="${pageContext.request.contextPath}/barista?action=recipes">
                                Công thức &amp; Định lượng <span class="d-none d-sm-inline">(F2)</span>
                            </a>
                        </li>
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

            <!-- Right Actions: Cart & Profile -->
            <div class="d-flex align-items-center gap-3 ms-lg-auto">
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
                        <div class="dropdown">
                            <a href="#" class="d-flex align-items-center gap-2 text-decoration-none text-dark" data-bs-toggle="dropdown">
                                <div class="rounded-circle bg-light d-flex align-items-center justify-content-center border" style="width: 40px; height: 40px;">
                                    <span class="material-symbols-outlined text-danger">account_circle</span>
                                </div>
                                <div class="d-none d-xl-flex flex-column text-start">
                                    <span class="fw-semibold text-truncate" style="font-size: 0.85rem; max-width: 130px;">${sessionScope.user.fullName}</span>
                                    <span class="text-danger fw-bold" style="font-size: 0.7rem;">${sessionScope.user.role}</span>
                                </div>
                            </a>
                            <ul class="dropdown-menu dropdown-menu-end user-dropdown-menu">
                                <!-- Header Info -->
                                <li class="user-dropdown-header">
                                    <div class="d-flex align-items-center justify-content-between mb-1">
                                        <span class="fw-bold text-dark text-truncate" style="max-width: 150px; font-size: 0.92rem;">${sessionScope.user.fullName}</span>
                                        <c:choose>
                                            <c:when test="${not empty sessionScope.user.tierName}">
                                                <span class="badge bg-danger-subtle text-danger" style="font-size: 0.68rem; font-weight: 700;">${sessionScope.user.tierName}</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-light text-secondary border" style="font-size: 0.68rem;">${sessionScope.user.role}</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div class="d-flex align-items-center justify-content-between text-muted" style="font-size: 0.78rem;">
                                        <span>${not empty sessionScope.user.phone ? sessionScope.user.phone : sessionScope.user.email}</span>
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
                                    <a class="user-item-btn logout-btn" href="${pageContext.request.contextPath}/login?action=logout">
                                        <span class="material-symbols-outlined item-icon" style="color: #dc2626;">logout</span>
                                        <span class="text-danger fw-semibold">Đăng xuất</span>
                                    </a>
                                </li>
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
