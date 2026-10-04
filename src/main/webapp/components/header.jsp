<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<style>
/* Scoped Aura Header Component - Đồng bộ 100% giao diện Header.tsx, hoạt động hoàn hảo trên cả trang Bootstrap và Tailwind */
.aura-header {
    position: sticky !important;
    top: 0 !important;
    left: 0 !important;
    right: 0 !important;
    width: 100% !important;
    height: 76px !important;
    background-color: #ffffff !important;
    border-bottom: 1px solid #d9e3f6 !important;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04) !important;
    z-index: 1050 !important;
    display: flex !important;
    align-items: center !important;
    font-family: 'Plus Jakarta Sans', 'Inter', -apple-system, BlinkMacSystemFont, sans-serif !important;
    box-sizing: border-box !important;
}

.aura-header * {
    box-sizing: border-box !important;
}

.aura-header-container {
    width: 100% !important;
    max-width: 1280px !important;
    margin: 0 auto !important;
    padding: 0 1.25rem !important;
    display: flex !important;
    align-items: center !important;
    justify-content: space-between !important;
    gap: 1rem !important;
    height: 100% !important;
}

/* Brand Logo */
.aura-brand-link {
    display: flex !important;
    align-items: center !important;
    gap: 0.65rem !important;
    text-decoration: none !important;
    color: inherit !important;
    flex-shrink: 0 !important;
}

.aura-brand-icon {
    width: 38px !important;
    height: 38px !important;
    border-radius: 9px !important;
    background-color: #b2292e !important;
    color: #ffffff !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    box-shadow: 0 2px 6px rgba(178, 41, 46, 0.25) !important;
}

.aura-brand-title {
    font-size: 1.15rem !important;
    font-weight: 800 !important;
    color: #900b1a !important;
    letter-spacing: -0.01em !important;
    line-height: 1.15 !important;
    margin: 0 !important;
}

.aura-brand-subtitle {
    font-size: 0.65rem !important;
    font-weight: 700 !important;
    color: #59413f !important;
    text-transform: uppercase !important;
    letter-spacing: 0.08em !important;
    margin: 0 !important;
    line-height: 1 !important;
}

/* Nav Menu */
.aura-nav-menu {
    display: flex !important;
    align-items: center !important;
    gap: 1.5rem !important;
    height: 100% !important;
    margin: 0 !important;
    padding: 0 !important;
    list-style: none !important;
}

.aura-nav-item {
    height: 100% !important;
    display: flex !important;
    align-items: center !important;
}

.aura-nav-item-link {
    display: flex !important;
    align-items: center !important;
    height: 100% !important;
    font-size: 0.88rem !important;
    font-weight: 500 !important;
    color: #555f71 !important;
    text-decoration: none !important;
    position: relative !important;
    transition: color 0.2s ease !important;
    white-space: nowrap !important;
    padding: 0 0.25rem !important;
}

.aura-nav-item-link:hover {
    color: #900b1a !important;
}

.aura-nav-item-link.aura-nav-active {
    color: #900b1a !important;
    font-weight: 700 !important;
}

.aura-nav-item-link.aura-nav-active::after {
    content: '' !important;
    position: absolute !important;
    bottom: 0 !important;
    left: 0 !important;
    right: 0 !important;
    height: 3px !important;
    background-color: #b2292e !important;
    border-radius: 3px 3px 0 0 !important;
}

/* Search Bar */
.aura-search-form {
    display: flex !important;
    align-items: center !important;
    position: relative !important;
    flex: 1 !important;
    max-width: 380px !important;
    margin: 0 0.5rem !important;
}

.aura-search-icon {
    position: absolute !important;
    left: 0.85rem !important;
    top: 50% !important;
    transform: translateY(-50%) !important;
    color: #555f71 !important;
    font-size: 1.15rem !important;
    pointer-events: none !important;
}

.aura-search-input {
    width: 100% !important;
    height: 40px !important;
    padding: 0.4rem 0.9rem 0.4rem 2.6rem !important;
    background-color: #f8f9ff !important;
    border: 1px solid #d9e3f6 !important;
    border-radius: 8px !important;
    font-size: 0.85rem !important;
    color: #121c2a !important;
    outline: none !important;
    transition: all 0.2s ease !important;
}

.aura-search-input:focus {
    background-color: #ffffff !important;
    border-color: #b2292e !important;
    box-shadow: 0 0 0 3px rgba(178, 41, 46, 0.12) !important;
}

/* Right Controls */
.aura-header-actions {
    display: flex !important;
    align-items: center !important;
    gap: 0.75rem !important;
    flex-shrink: 0 !important;
}

.aura-cart-btn {
    display: inline-flex !important;
    align-items: center !important;
    gap: 0.45rem !important;
    height: 40px !important;
    padding: 0 0.85rem !important;
    border-radius: 8px !important;
    border: 1px solid #d9e3f6 !important;
    background-color: #f8f9ff !important;
    color: #121c2a !important;
    text-decoration: none !important;
    font-size: 0.82rem !important;
    font-weight: 600 !important;
    transition: all 0.2s ease !important;
}

.aura-cart-btn:hover {
    background-color: #e6eeff !important;
    border-color: #dee9fc !important;
}

.aura-cart-btn.aura-cart-active {
    background-color: #ffdad7 !important;
    border-color: rgba(144, 11, 26, 0.2) !important;
    color: #900b1a !important;
}

.aura-cart-badge {
    display: inline-flex !important;
    align-items: center !important;
    justify-content: center !important;
    height: 20px !important;
    min-width: 20px !important;
    padding: 0 5px !important;
    border-radius: 999px !important;
    background-color: #b2292e !important;
    color: #ffffff !important;
    font-size: 0.68rem !important;
    font-weight: 700 !important;
    line-height: 1 !important;
}

.aura-user-btn {
    width: 38px !important;
    height: 38px !important;
    border-radius: 999px !important;
    background-color: #900b1a !important;
    color: #ffffff !important;
    display: inline-flex !important;
    align-items: center !important;
    justify-content: center !important;
    border: none !important;
    cursor: pointer !important;
    text-decoration: none !important;
    box-shadow: 0 2px 6px rgba(144, 11, 26, 0.2) !important;
    transition: transform 0.15s ease, background-color 0.2s ease !important;
}

.aura-user-btn:hover {
    background-color: #b2292e !important;
    transform: scale(1.05) !important;
}

/* Mobile Toggle Button */
.aura-mobile-toggle {
    display: none !important;
    width: 40px !important;
    height: 40px !important;
    border-radius: 8px !important;
    border: 1px solid #d9e3f6 !important;
    background-color: transparent !important;
    color: #121c2a !important;
    align-items: center !important;
    justify-content: center !important;
    cursor: pointer !important;
}

/* User Dropdown */
.aura-dropdown-menu {
    position: absolute;
    right: 0;
    top: 48px;
    background: #ffffff;
    border: 1px solid #d9e3f6;
    border-radius: 10px;
    box-shadow: 0 10px 25px rgba(0,0,0,0.08);
    min-width: 180px;
    padding: 0.5rem 0;
    display: none;
    z-index: 1060;
}
.aura-dropdown-open {
    display: block !important;
}

@media (min-width: 1024px) {
    .aura-user-meta-desk {
        display: flex !important;
    }
}

@media (max-width: 991px) {
    .aura-nav-menu {
        display: none !important;
        position: absolute !important;
        top: 76px !important;
        left: 0 !important;
        right: 0 !important;
        background-color: #ffffff !important;
        flex-direction: column !important;
        height: auto !important;
        padding: 1rem 1.25rem !important;
        border-bottom: 1px solid #d9e3f6 !important;
        box-shadow: 0 10px 20px rgba(0,0,0,0.06) !important;
        gap: 0.5rem !important;
    }
    .aura-nav-menu.aura-nav-open {
        display: flex !important;
    }
    .aura-nav-item {
        width: 100% !important;
        height: 40px !important;
    }
    .aura-nav-item-link {
        width: 100% !important;
        height: 100% !important;
        border-radius: 6px !important;
        padding: 0 0.75rem !important;
    }
    .aura-nav-item-link.aura-nav-active::after {
        display: none !important;
    }
    .aura-nav-item-link.aura-nav-active {
        background-color: #ffdad7 !important;
    }
    .aura-mobile-toggle {
        display: inline-flex !important;
    }
    .aura-search-form {
        display: none !important;
    }
}
</style>

<header class="aura-header">
    <div class="aura-header-container">
        <!-- Brand Logo & Main Nav -->
        <div style="display: flex; align-items: center; gap: 1.5rem; height: 100%;">
            <a href="${pageContext.request.contextPath}/" class="aura-brand-link">
                <span class="aura-brand-icon">
                    <span class="material-symbols-outlined" style="font-size: 20px;">local_cafe</span>
                </span>
                <div style="display: flex; flex-direction: column;">
                    <span class="aura-brand-title">Aura Café</span>
                    <span class="aura-brand-subtitle">Specialty Coffee</span>
                </div>
            </a>

            <!-- Navigation Links (Đồng bộ 100% Header.tsx và hệ thống gốc) -->
            <ul class="aura-nav-menu" id="auraNavMenu">
                <li class="aura-nav-item">
                    <a href="${pageContext.request.contextPath}/" 
                       class="aura-nav-item-link ${param.activePage == 'home' ? 'aura-nav-active' : ''}">
                        Trang chủ
                    </a>
                </li>
                <li class="aura-nav-item">
                    <a href="${pageContext.request.contextPath}/menu" 
                       class="aura-nav-item-link ${param.activePage == 'menu' ? 'aura-nav-active' : ''}">
                        Menu &amp; Đồ uống
                    </a>
                </li>
                <li class="aura-nav-item">
                    <a href="${pageContext.request.contextPath}/orders" 
                       class="aura-nav-item-link ${param.activePage == 'orders' ? 'aura-nav-active' : ''}">
                        Đơn hàng của tôi
                    </a>
                </li>
                <li class="aura-nav-item">
                    <a href="#" onclick="alert('Tính năng Hồ sơ & Điểm tích luỹ đang được phát triển!')" 
                       class="aura-nav-item-link">
                        Hồ sơ &amp; Điểm tích luỹ
                    </a>
                </li>
                <li class="aura-nav-item">
                    <a href="#" onclick="alert('Tính năng Phản ánh & Khiếu nại đang được phát triển!')" 
                       class="aura-nav-item-link">
                        Phản ánh &amp; Khiếu nại
                    </a>
                </li>
            </ul>
        </div>

        <!-- Global Search Bar (Đồng bộ name='keyword' cho menu.jsp) -->
        <form action="${pageContext.request.contextPath}/menu" method="GET" class="aura-search-form">
            <span class="material-symbols-outlined aura-search-icon">search</span>
            <input type="text" name="keyword" 
                   value="${not empty param.keyword ? param.keyword : (not empty currentKeyword ? currentKeyword : '')}" 
                   placeholder="Tìm kiếm đồ uống, bánh ngọt hoặc combo..."
                   class="aura-search-input" />
        </form>

        <!-- Right Actions: Cart & Profile -->
        <div class="aura-header-actions">
            <!-- Mobile Menu Toggle -->
            <button type="button" class="aura-mobile-toggle" onclick="toggleAuraMobileNav()" title="Menu">
                <span class="material-symbols-outlined" style="font-size: 20px;">menu</span>
            </button>

            <!-- Cart Button (Đồng bộ với modal nếu ở menu.jsp hoặc trang /cart) -->
            <a href="${pageContext.request.contextPath}/cart" 
               class="aura-cart-btn ${param.activePage == 'cart' || param.activePage == 'checkout' || param.activePage == 'payment' ? 'aura-cart-active' : ''}" 
               title="Giỏ hàng">
                <span class="material-symbols-outlined" style="font-size: 20px; color: #555f71;">shopping_bag</span>
                <span style="font-size: 13px; font-weight: 600;">Giỏ hàng</span>
                <span id="headerCartBadge" class="aura-cart-badge">0</span>
            </a>

            <!-- User Profile / Auth Button -->
            <c:choose>
                <c:when test="${not empty sessionScope.user}">
                    <div style="position: relative; display: flex; align-items: center; gap: 0.5rem;">
                        <button type="button" onclick="toggleAuraUserDropdown(event)" class="aura-user-btn" title="${sessionScope.user.fullName}">
                            <span class="material-symbols-outlined" style="font-size: 18px; color: #ffffff;">person</span>
                        </button>
                        <div onclick="toggleAuraUserDropdown(event)" style="display: none; cursor: pointer; flex-direction: column;" class="aura-user-meta-desk">
                            <span style="font-size: 0.82rem; font-weight: 700; color: #121c2a; line-height: 1.1;">${sessionScope.user.fullName}</span>
                            <span style="font-size: 0.68rem; font-weight: 700; color: #900b1a; text-transform: uppercase;">${sessionScope.user.role}</span>
                        </div>
                        <div id="auraUserDropdown" class="aura-dropdown-menu">
                            <div style="padding: 0.5rem 1rem; border-bottom: 1px solid #d9e3f6;">
                                <div style="font-weight: 700; font-size: 0.85rem; color: #121c2a;">${sessionScope.user.fullName}</div>
                                <div style="font-size: 0.75rem; color: #555f71;">${sessionScope.user.phone}</div>
                            </div>
                            <a href="${pageContext.request.contextPath}/orders" 
                               style="display: flex; align-items: center; gap: 0.5rem; padding: 0.6rem 1rem; color: #121c2a; text-decoration: none; font-size: 0.85rem; font-weight: 500;">
                                <span class="material-symbols-outlined" style="font-size: 16px; color: #555f71;">receipt_long</span> Đơn hàng của tôi
                            </a>
                            <a href="${pageContext.request.contextPath}/login?action=logout" 
                               style="display: flex; align-items: center; gap: 0.5rem; padding: 0.6rem 1rem; color: #900b1a; text-decoration: none; font-size: 0.85rem; font-weight: 600; border-top: 1px solid #f1f5f9;">
                                <span class="material-symbols-outlined" style="font-size: 16px;">logout</span> Đăng xuất
                            </a>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <button type="button" class="aura-user-btn" 
                            data-bs-toggle="modal" data-bs-target="#authModal"
                            onclick="handleAuraAuthClick(event)"
                            title="Đăng nhập tài khoản">
                        <span class="material-symbols-outlined" style="font-size: 18px; color: #ffffff;">person</span>
                    </button>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</header>

<script>
    // Toggle Mobile Navigation
    function toggleAuraMobileNav() {
        var menu = document.getElementById('auraNavMenu');
        if (menu) {
            menu.classList.toggle('aura-nav-open');
        }
    }

    // Toggle User Dropdown
    function toggleAuraUserDropdown(e) {
        e.stopPropagation();
        var dd = document.getElementById('auraUserDropdown');
        if (dd) {
            dd.classList.toggle('aura-dropdown-open');
        }
    }
    document.addEventListener('click', function() {
        var dd = document.getElementById('auraUserDropdown');
        if (dd) dd.classList.remove('aura-dropdown-open');
    });

    // Handle Auth Click (nếu trang có Bootstrap modal thì mở modal, nếu không thì sang trang /login)
    function handleAuraAuthClick(e) {
        var modalElem = document.getElementById('authModal');
        if (modalElem && typeof bootstrap !== 'undefined' && bootstrap.Modal) {
            e.preventDefault();
            var modal = bootstrap.Modal.getInstance(modalElem) || new bootstrap.Modal(modalElem);
            modal.show();
        } else {
            window.location.href = '${pageContext.request.contextPath}/login';
        }
    }

    // Tự động đồng bộ số lượng giỏ hàng trên tất cả các trang từ localStorage
    (function syncHeaderCartBadge() {
        try {
            var raw = localStorage.getItem('aura_cart');
            if (raw) {
                var cart = JSON.parse(raw);
                if (Array.isArray(cart)) {
                    var totalQty = cart.reduce(function(acc, item) { return acc + (item.quantity || 1); }, 0);
                    var badge = document.getElementById('headerCartBadge');
                    if (badge) badge.innerText = totalQty;
                }
            }
        } catch (e) {}
    })();
</script>
