<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<aside class="aura-admin-sidebar">
    <!-- Brand Header -->
    <div class="sidebar-brand-wrapper">
        <a href="${ctx}/admin-dashboard.jsp" class="sidebar-brand text-decoration-none">
            <div class="brand-badge-box">
                <span class="material-symbols-outlined">local_cafe</span>
            </div>
            <div class="brand-text-box">
                <div class="brand-main-title">AURA CAFÉ</div>
                <div class="brand-sub-title">QUẢN TRỊ HỆ THỐNG</div>
            </div>
        </a>
    </div>

    <!-- Navigation Menu -->
    <div class="sidebar-menu-scroll">
        <!-- GROUP: BÁO CÁO -->
        <div class="menu-group-header">BÁO CÁO</div>
        <ul class="nav flex-column menu-nav">
            <li class="nav-item">
                <a href="${ctx}/admin-dashboard.jsp" class="nav-link ${param.activeMenu == 'dashboard' ? 'active' : ''}">
                    <span class="material-symbols-outlined">grid_view</span>
                    <span>Tổng quan</span>
                </a>
            </li>
            <li class="nav-item">
                <a href="${ctx}/admin/reports" class="nav-link ${param.activeMenu == 'reports' ? 'active' : ''}">
                    <span class="material-symbols-outlined">analytics</span>
                    <span>Báo cáo chi tiết</span>
                </a>
            </li>
        </ul>

        <!-- GROUP: BÁN HÀNG -->
        <div class="menu-group-header">BÁN HÀNG</div>
        <ul class="nav flex-column menu-nav">
            <li class="nav-item">
                <a href="${ctx}/admin/orders" class="nav-link ${param.activeMenu == 'orders' ? 'active' : ''}">
                    <span class="material-symbols-outlined">receipt_long</span>
                    <span>Đơn hàng</span>
                </a>
            </li>
            <li class="nav-item">
                <a href="${ctx}/admin/bank-transactions" class="nav-link ${param.activeMenu == 'bank-transactions' ? 'active' : ''}">
                    <span class="material-symbols-outlined">account_balance</span>
                    <span>Giao dịch ngân hàng</span>
                </a>
            </li>
            <li class="nav-item">

                <%-- [DMM01]
                     . Khi Admin bấm Quản lý danh mục, trình duyệt gửi yêu cầu tới doGet của
                       AdminCategoriesController. Hàm này đọc danh mục và các số liệu cần thiết rồi đưa về
                       trang quản lý, để Admin xem danh sách hoặc thêm, sửa nhóm món.

                     Tra tiếp: AdminCategoriesController.java / DMM02.
                --%>
                <a href="${ctx}/admin/categories" class="nav-link ${param.activeMenu == 'categories' ? 'active' : ''}">
                    <span class="material-symbols-outlined">category</span>
                    <span>Danh mục</span>
                </a>
            </li>
            <li class="nav-item">

                <%-- [SPM01]
                     . Khi Admin bấm Quản lý sản phẩm, trình duyệt gửi yêu cầu tới doGet của
                       AdminProductsController. Controller chuẩn bị danh mục, danh sách món và các số tổng
                       rồi đưa về trang để Admin xem và quản lý sản phẩm.

                     Tra tiếp: AdminProductsController.java / SPM02.
                --%>
                <a href="${ctx}/admin/products" class="nav-link ${param.activeMenu == 'products' ? 'active' : ''}">
                    <span class="material-symbols-outlined">coffee</span>
                    <span>Sản phẩm</span>
                </a>
            </li>
            <li class="nav-item">

                <%-- [CTM01]
                     . Khi Admin bấm Quản lý công thức, trình duyệt gửi yêu cầu tới doGet của
                       AdminRecipeController. Controller đọc món, nguyên liệu và công thức, tính chi phí
                       rồi đưa về trang để Admin xem hoặc thiết lập định mức cho từng món.

                     Tra tiếp: AdminRecipeController.java / CTM02.
                --%>
                <a href="${ctx}/admin/recipes" class="nav-link ${param.activeMenu == 'recipes' ? 'active' : ''}">
                    <span class="material-symbols-outlined">menu_book</span>
                    <span>Công thức (BOM)</span>
                </a>
            </li>
        </ul>

        <!-- GROUP: KHO & NHẬP HÀNG -->
        <div class="menu-group-header">KHO &amp; NHẬP HÀNG</div>
        <ul class="nav flex-column menu-nav">
            <li class="nav-item">
                <a href="${ctx}/admin/ingredients" class="nav-link ${param.activeMenu == 'ingredients' ? 'active' : ''}">
                    <span class="material-symbols-outlined">inventory_2</span>
                    <span>Nguyên liệu</span>
                </a>
            </li>
            <li class="nav-item">
                <a href="${ctx}/admin/inventory" class="nav-link ${param.activeMenu == 'inventory' ? 'active' : ''}">
                    <span class="material-symbols-outlined">inventory</span>
                    <span>Tồn kho &amp; Xuất nhập</span>
                </a>
            </li>
            <li class="nav-item">
                <a href="${ctx}/admin/restock-requests" class="nav-link ${param.activeMenu == 'restock' ? 'active' : ''}">
                    <span class="material-symbols-outlined">notification_important</span>
                    <span>Đề xuất nhập hàng</span>
                </a>
            </li>
            <li class="nav-item">
                <a href="${ctx}/admin/suppliers" class="nav-link ${param.activeMenu == 'suppliers' ? 'active' : ''}">
                    <span class="material-symbols-outlined">local_shipping</span>
                    <span>Nhà cung cấp</span>
                </a>
            </li>
            <li class="nav-item">
                <a href="${ctx}/admin/purchases" class="nav-link ${param.activeMenu == 'purchases' ? 'active' : ''}">
                    <span class="material-symbols-outlined">description</span>
                    <span>Phiếu nhập hàng</span>
                </a>
            </li>
        </ul>

        <!-- GROUP: NHÂN SỰ & CA LÀM -->
        <div class="menu-group-header">NHÂN SỰ &amp; CA LÀM</div>
        <ul class="nav flex-column menu-nav">
            <li class="nav-item">
                <a href="${ctx}/admin/employees" class="nav-link ${param.activeMenu == 'employees' ? 'active' : ''}">
                    <span class="material-symbols-outlined">group</span>
                    <span>Nhân viên</span>
                </a>
            </li>
            <li class="nav-item">
                <a href="${ctx}/admin/shifts" class="nav-link ${param.activeMenu == 'shifts' ? 'active' : ''}">
                    <span class="material-symbols-outlined">schedule</span>
                    <span>Khung giờ ca</span>
                </a>
            </li>
            <li class="nav-item">
                <a href="${ctx}/admin/schedules" class="nav-link ${param.activeMenu == 'schedules' ? 'active' : ''}">
                    <span class="material-symbols-outlined">calendar_month</span>
                    <span>Lịch phân ca</span>
                </a>
            </li>
            <li class="nav-item">
                <a href="${ctx}/admin/attendance" class="nav-link ${param.activeMenu == 'attendance' ? 'active' : ''}">
                    <span class="material-symbols-outlined">badge</span>
                    <span>Chấm công</span>
                </a>
            </li>
            <li class="nav-item">
                <a href="${ctx}/admin/handovers" class="nav-link ${param.activeMenu == 'handovers' ? 'active' : ''}">
                    <span class="material-symbols-outlined">assignment_turned_in</span>
                    <span>Lịch sử bàn giao</span>
                </a>
            </li>
        </ul>

        <!-- GROUP: KHÁCH HÀNG -->
        <div class="menu-group-header">KHÁCH HÀNG</div>
        <ul class="nav flex-column menu-nav">
            <li class="nav-item">
                <a href="${ctx}/admin/customers" class="nav-link ${param.activeMenu == 'customers' ? 'active' : ''}">
                    <span class="material-symbols-outlined">person</span>
                    <span>Khách hàng</span>
                </a>
            </li>
            <li class="nav-item">

                <%-- [TDM01]
                     . Khi Admin bấm Quản lý tích điểm theo số điện thoại, trình duyệt gửi yêu cầu tới
                       doGet của AdminLoyaltyController. Controller chuẩn bị quy tắc, số liệu tổng, khách
                       hàng và lịch sử rồi đưa về cùng màn hình để Admin tra cứu hoặc điều chỉnh điểm.

                     Tra tiếp: AdminLoyaltyController.java / TDM02.
                --%>
                <a href="${ctx}/admin/loyalty" class="nav-link ${param.activeMenu == 'loyalty' ? 'active' : ''}">
                    <span class="material-symbols-outlined">loyalty</span>
                    <span>Tích điểm</span>
                </a>
            </li>
            <li class="nav-item">
                <a href="${ctx}/admin/feedback" class="nav-link ${param.activeMenu == 'feedback' ? 'active' : ''}">
                    <span class="material-symbols-outlined">rate_review</span>
                    <span>Đánh giá &amp; Khiếu nại</span>
                </a>
            </li>
        </ul>
    </div>

    <div class="menu-group-header">HỆ THỐNG</div>
    <ul class="nav flex-column menu-nav">
        <li class="nav-item">
            <a href="${ctx}/admin/settings" class="nav-link ${param.activeMenu == 'settings' ? 'active' : ''}">
                <span class="material-symbols-outlined">settings</span>
                <span>Cài đặt hệ thống</span>
            </a>
        </li>
    </ul>

    <!-- Sidebar Bottom: User & Logout -->
    <div class="sidebar-footer">
        <div class="d-flex align-items-center justify-content-between">
            <div class="d-flex align-items-center gap-2 overflow-hidden">
                <div class="avatar-sm">
                    <span class="material-symbols-outlined fs-5">account_circle</span>
                </div>
                <div class="text-truncate">
                    <div class="small fw-bold text-dark text-truncate">${sessionScope.user.fullName}</div>
                    <div class="text-muted" style="font-size: 0.72rem;">Quản trị viên</div>
                </div>
            </div>
            <%-- [DNX03]
                 . Nút Đăng xuất của quản trị cũng gửi yêu cầu tới LoginController như phía khách hàng. Em
                   dùng chung cách xử lý để cả hai nơi đều kết thúc tài khoản đang dùng rồi trở về trang
                   chủ.

                 Tra tiếp: LoginController.java / DNX02.
            --%>
            <a href="${ctx}/login?action=logout" class="btn-logout" title="Đăng xuất">
                <span class="material-symbols-outlined fs-5">logout</span>
            </a>
        </div>
    </div>
</aside>
