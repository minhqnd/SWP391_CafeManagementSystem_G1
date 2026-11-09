<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Thanh header dùng chung cho các màn Thu ngân: Bán tại quầy (/cashier),
    Chốt ca / Bàn giao ca (/cashier/handover).
    param.active : pos | orders | close | handover
    param.onPos  : true khi đặt trên màn Bán tại quầy - 2 mục đầu là nút chuyển tab ngay trên trang
                   (id tabBtnPos / tabBtnOrders dùng bởi switchMainTab), ở màn khác là link về /cashier.
    param.orderCount : số đơn hôm nay (chỉ màn Bán tại quầy có)
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
<style>
    .cashier-topbar { position: sticky; top: 0; height: 56px; background: #fff; border-bottom: 1px solid #e2e8f0;
        display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 0 16px; z-index: 1000;
        font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif; color: #0f172a; box-sizing: border-box; }
    .cashier-topbar * { box-sizing: border-box; }
    .cashier-topbar .material-symbols-outlined { font-size: 20px; line-height: 1; vertical-align: middle; }
    .cashier-topbar .ct-brand { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
    .cashier-topbar .ct-brand-title { display: flex; align-items: center; gap: 6px; font-size: 17px; font-weight: 800;
        letter-spacing: -0.3px; color: #900b1a; text-decoration: none; }
    .cashier-topbar .ct-badge { background: #fff1f2; color: #900b1a; font-size: 11px; font-weight: 700; padding: 3px 8px;
        border-radius: 6px; border: 1px solid #fecdd3; text-transform: uppercase; white-space: nowrap; }
    .cashier-topbar .ct-tabs { display: flex; gap: 4px; background: #f1f5f9; padding: 3px; border-radius: 8px; border: 1px solid #e2e8f0; }
    .cashier-topbar .nav-tab-btn { border: none; background: transparent; padding: 6px 14px; border-radius: 6px; font: 600 13px 'Inter', sans-serif;
        color: #475569; display: flex; align-items: center; gap: 6px; cursor: pointer; text-decoration: none; white-space: nowrap;
        transition: all 0.15s ease; margin: 0; }
    .cashier-topbar .nav-tab-btn:hover { color: #0f172a; }
    .cashier-topbar .nav-tab-btn.active { background: #fff; color: #900b1a; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08); }
    .cashier-topbar .ct-meta { display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
    .cashier-topbar .ct-clock { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #475569; }
    .cashier-topbar .ct-clock b { font-weight: 700; color: #1e293b; font-variant-numeric: tabular-nums; }
    .cashier-topbar .ct-user { display: flex; align-items: center; gap: 6px; background: #f8fafc; padding: 5px 12px; border-radius: 20px;
        border: 1px solid #e2e8f0; font-weight: 600; font-size: 13px; white-space: nowrap; }
    .cashier-topbar .ct-logout { display: flex; align-items: center; gap: 4px; color: #475569; text-decoration: none; font-weight: 600;
        font-size: 13px; padding: 6px 10px; border-radius: 6px; white-space: nowrap; }
    .cashier-topbar .ct-logout:hover { background: #f1f5f9; color: #900b1a; }
    @media (max-width: 1280px) { .cashier-topbar .ct-clock, .cashier-topbar .ct-badge { display: none; } }
    @media (max-width: 1000px) { .cashier-topbar .nav-tab-btn span:not(.material-symbols-outlined) { display: none; } }
</style>
<header class="cashier-topbar">
    <div class="ct-brand">
        <a href="${ctx}/cashier" class="ct-brand-title">
            <span class="material-symbols-outlined">coffee</span>
            <span>AURA CAFÉ</span>
        </a>
        <span class="ct-badge">POS THU NGÂN</span>
    </div>

    <nav class="ct-tabs" aria-label="Điều hướng thu ngân">
        <c:choose>
            <c:when test="${param.onPos eq 'true'}">
                <button type="button" class="nav-tab-btn ${param.active eq 'pos' ? 'active' : ''}" id="tabBtnPos" onclick="switchMainTab('pos')">
                    <span class="material-symbols-outlined">point_of_sale</span><span>Tạo đơn tại quầy</span>
                </button>
                <button type="button" class="nav-tab-btn ${param.active eq 'orders' ? 'active' : ''}" id="tabBtnOrders" onclick="switchMainTab('orders')">
                    <span class="material-symbols-outlined">receipt_long</span><span>Đơn hôm nay (<span id="todayOrderCount">${param.orderCount}</span>)</span>
                </button>
            </c:when>
            <c:otherwise>
                <a class="nav-tab-btn" href="${ctx}/cashier">
                    <span class="material-symbols-outlined">point_of_sale</span><span>Tạo đơn tại quầy</span>
                </a>
                <a class="nav-tab-btn" href="${ctx}/cashier?tab=orders">
                    <span class="material-symbols-outlined">receipt_long</span><span>Đơn hôm nay</span>
                </a>
            </c:otherwise>
        </c:choose>
        <a class="nav-tab-btn ${param.active eq 'counter' ? 'active' : ''}" href="${ctx}/counter-orders">
            <span class="material-symbols-outlined">search</span><span>Tra cứu đơn tại quầy</span>
        </a>
        <a class="nav-tab-btn ${param.active eq 'close' ? 'active' : ''}" href="${ctx}/cashier/handover?mode=close">
            <span class="material-symbols-outlined">calculate</span><span>Chốt ca</span>
        </a>
        <a class="nav-tab-btn ${param.active eq 'handover' ? 'active' : ''}" href="${ctx}/cashier/handover?mode=handover">
            <span class="material-symbols-outlined">assignment_turned_in</span><span>Bàn giao ca</span>
        </a>
    </nav>

    <div class="ct-meta">
        <div class="ct-clock">
            <span class="material-symbols-outlined">schedule</span>
            <b id="clockDisplay">--:--:--</b>
            <span>|</span>
            <span id="cashierDateDisplay"></span>
        </div>
        <div class="ct-user">
            <span class="material-symbols-outlined">account_circle</span>
            <span><c:out value="${sessionScope.user.fullName}" /></span>
        </div>
        <a href="${ctx}/login?action=logout" class="ct-logout" title="Đăng xuất khỏi hệ thống">
            <span class="material-symbols-outlined">logout</span><span>Đăng xuất</span>
        </a>
    </div>
</header>
<script>
    // Đồng hồ trên header (màn Bán tại quầy cũng tự cập nhật #clockDisplay - chạy song song không ảnh hưởng)
    (() => {
        const clock = document.getElementById('clockDisplay');
        const date = document.getElementById('cashierDateDisplay');
        const pad = (n) => String(n).padStart(2, '0');
        const tick = () => {
            const now = new Date();
            clock.textContent = pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds());
            date.textContent = pad(now.getDate()) + '/' + pad(now.getMonth() + 1) + '/' + now.getFullYear();
        };
        tick();
        setInterval(tick, 1000);
    })();
</script>
