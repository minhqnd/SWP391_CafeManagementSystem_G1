<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<style>
    /* Shared Header Navbar Styling */
    .aura-navbar {
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
</style>

<nav class="navbar navbar-expand-lg aura-navbar py-3">
    <div class="container-fluid px-4 px-lg-5">
        <!-- Brand Logo -->
        <a class="navbar-brand fw-bold text-danger d-flex align-items-center gap-2 me-4" href="${pageContext.request.contextPath}/">
            <div class="bg-danger text-white rounded-3 p-2 d-flex align-items-center justify-content-center" style="width: 40px; height: 40px;">
                <span class="material-symbols-outlined fs-4">local_cafe</span>
            </div>
            <div class="d-flex flex-column text-start">
                <span class="fs-4 lh-1 text-dark fw-bold">Aura Café</span>
                <span class="text-secondary fw-semibold" style="font-size: 0.65rem; letter-spacing: 1px; text-transform: uppercase;">Specialty Coffee</span>
            </div>
        </a>

        <!-- Mobile Toggler -->
        <button class="navbar-toggler border-0 shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#mainNavbar">
            <span class="material-symbols-outlined">menu</span>
        </button>

        <div class="collapse navbar-collapse" id="mainNavbar">
            <!-- Navigation Links -->
            <ul class="navbar-nav me-auto mb-2 mb-lg-0 gap-1 gap-lg-2" style="font-size: 0.95rem;">
                <li class="nav-item">
                    <a class="nav-link aura-nav-link ${param.activePage == 'home' ? 'active-nav' : ''}" 
                       href="${pageContext.request.contextPath}/">Trang chủ</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link aura-nav-link ${param.activePage == 'menu' ? 'active-nav' : ''}" 
                       href="${pageContext.request.contextPath}/menu">Menu & Đồ uống</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link aura-nav-link text-muted" href="#" onclick="alert('Tính năng Đơn hàng của tôi đang được phát triển!')">Đơn hàng của tôi</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link aura-nav-link text-muted" href="#" onclick="alert('Tính năng Hồ sơ & Điểm tích luỹ đang được phát triển!')">Hồ sơ & Điểm tích luỹ</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link aura-nav-link text-muted" href="#" onclick="alert('Tính năng Phản ánh & Khiếu nại đang được phát triển!')">Phản ánh & Khiếu nại</a>
                </li>
            </ul>

            <!-- Right Actions: Cart & Profile -->
            <div class="d-flex align-items-center gap-3 ms-lg-auto">
                <!-- Cart Button -->
                <a href="${pageContext.request.contextPath}/cart" 
                   class="text-decoration-none text-dark d-flex align-items-center gap-2 px-3 py-2 rounded-pill bg-light border"
                   style="transition: background-color 0.2s;" title="Giỏ hàng">
                    <span class="material-symbols-outlined fs-5 text-secondary">shopping_bag</span>
                    <span class="fw-semibold d-none d-sm-inline" style="font-size: 0.88rem;">Giỏ hàng</span>
                    <span id="headerCartBadge" class="badge rounded-pill bg-danger" style="font-size: 0.72rem;">${empty sessionScope.cart ? 0 : sessionScope.cart.totalQuantity}</span>
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
                                    <span class="fw-semibold text-truncate" style="font-size: 0.85rem; max-width: 130px;">${sessionScope.user.fullName}</span>
                                    <span class="text-danger fw-bold" style="font-size: 0.7rem;">${sessionScope.user.role}</span>
                                </div>
                            </a>
                            <ul class="dropdown-menu dropdown-menu-end shadow border-0 mt-2">
                                <li class="px-3 py-2 border-bottom">
                                    <div class="fw-bold">${sessionScope.user.fullName}</div>
                                    <div class="small text-muted">${sessionScope.user.phone}</div>
                                </li>
                                <li>
                                    <a class="dropdown-item fw-medium py-2 text-danger" href="${pageContext.request.contextPath}/login?action=logout">
                                        <span class="material-symbols-outlined align-middle fs-6 me-1">logout</span> Đăng xuất
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
