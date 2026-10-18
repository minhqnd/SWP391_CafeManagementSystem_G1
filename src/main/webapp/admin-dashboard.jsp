<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty sessionScope.user or sessionScope.user.role ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Aura Café | Tổng quan Admin</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <style>
        .admin-main-wrapper { max-width:1600px; }
        .dashboard-page { display:flex; flex-direction:column; gap:20px; }
        .dashboard-welcome, .dashboard-section { background:#fff; border:1px solid #e7e5e4; border-radius:18px; box-shadow:0 1px 2px rgba(28,25,23,.04); }
        .dashboard-welcome { padding:24px; display:flex; align-items:center; justify-content:space-between; gap:20px; }
        .dashboard-eyebrow { color:#900b1a; font-size:10px; letter-spacing:.09em; text-transform:uppercase; font-weight:800; }
        .dashboard-title { color:#1c1917; font-size:1.65rem; letter-spacing:-.03em; font-weight:800; margin:5px 0 4px; }
        .dashboard-subtitle { color:#78716c; margin:0; font-size:.86rem; }
        .welcome-mark { width:56px;height:56px; border-radius:16px; background:#fbf0f1; color:#900b1a; display:grid; place-items:center; flex:0 0 auto; }
        .welcome-mark .material-symbols-outlined { font-size:30px; }
        .dashboard-section { padding:20px; }
        .section-heading { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:15px; }
        .section-heading h2 { margin:0; color:#292524; font-size:1rem; font-weight:800; }
        .section-heading p { margin:3px 0 0; color:#78716c; font-size:.76rem; }
        .module-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; }
        .module-card { display:flex; align-items:flex-start; gap:14px; min-height:128px; padding:16px; color:inherit; text-decoration:none; background:#fff; border:1px solid #e7e5e4; border-radius:14px; transition:transform .16s ease,box-shadow .16s ease,border-color .16s ease; }
        .module-card:hover { transform:translateY(-2px); border-color:#d6b4b8; box-shadow:0 7px 18px rgba(41,37,36,.07); color:inherit; }
        .module-icon { width:42px;height:42px; display:grid;place-items:center;flex:0 0 auto; border-radius:12px;background:#fbf0f1;color:#900b1a; }
        .module-icon .material-symbols-outlined { font-size:22px; }
        .module-copy { display:block; }
        .module-card h3 { margin:1px 0 5px; font-size:.9rem; font-weight:800; color:#292524; }
        .module-card p { margin:0 0 10px; color:#78716c; font-size:.76rem; line-height:1.5; }
        .module-action { color:#900b1a; font-size:.72rem; font-weight:800; }
        .module-status { display:inline-flex;align-items:center;gap:5px;color:#047857;background:#ecfdf5;border:1px solid #a7f3d0;border-radius:999px;padding:3px 8px;font-size:10px;font-weight:800;white-space:nowrap; }
        .module-status::before { content:''; width:6px;height:6px;border-radius:50%;background:#10b981; }
        .coming-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:10px; }
        .coming-item { display:flex; align-items:center; gap:10px; padding:12px; border:1px solid #e7e5e4; border-radius:12px; background:#fafaf9; color:#78716c; }
        .coming-item .material-symbols-outlined { color:#a8a29e; font-size:19px; }
        .coming-name { flex:1; font-size:.76rem; font-weight:700; color:#57534e; }
        .coming-label { color:#a8a29e; font-size:9px; font-weight:800; text-transform:uppercase; white-space:nowrap; }
        @media(max-width:991.98px) { .coming-grid { grid-template-columns:repeat(2,minmax(0,1fr)); } }
        @media(max-width:767.98px) { .dashboard-welcome { padding:17px; } .dashboard-title { font-size:1.28rem; } .module-grid,.coming-grid { grid-template-columns:1fr; } .dashboard-section { padding:15px; } }
    </style>
</head>
<body class="admin-body">
<div class="d-md-none d-flex align-items-center justify-content-between p-3 bg-white border-bottom">
    <a href="${ctx}/admin-dashboard.jsp" class="text-decoration-none fw-bold text-danger"><span class="material-symbols-outlined align-middle">local_cafe</span> AURA CAFÉ · ADMIN</a>
    <a href="${ctx}/login?action=logout" class="btn btn-sm btn-light">Đăng xuất</a>
</div>
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="dashboard" /></jsp:include>
<main class="admin-main-wrapper dashboard-page">
    <header class="dashboard-welcome">
        <div>
            <div class="dashboard-eyebrow">Aura Café · Khu vực quản trị</div>
            <h1 class="dashboard-title">Xin chào, <c:out value="${sessionScope.user.fullName}" /></h1>
            <p class="dashboard-subtitle">Chọn một chức năng để bắt đầu quản lý cửa hàng.</p>
        </div>
        <div class="welcome-mark"><span class="material-symbols-outlined">dashboard</span></div>
    </header>

    <section class="dashboard-section" aria-labelledby="workingModules">
        <div class="section-heading"><div><h2 id="workingModules">Chức năng quản trị</h2><p>Các màn hình đã sẵn sàng và liên kết trực tiếp tới đúng chức năng.</p></div><span class="module-status">Đang hoạt động</span></div>
        <div class="module-grid">
            <a class="module-card" href="${ctx}/admin/categories"><span class="module-icon"><span class="material-symbols-outlined">category</span></span><div class="module-copy"><h3>Danh mục thực đơn</h3><p>Xem danh sách từ database, thêm, sửa, ẩn/hiện hoặc xóa danh mục.</p><span class="module-action">Mở quản lý danh mục →</span></div></a>
            <a class="module-card" href="${ctx}/admin/products"><span class="module-icon"><span class="material-symbols-outlined">local_cafe</span></span><div class="module-copy"><h3>Sản phẩm &amp; giá bán</h3><p>Thêm món, gán danh mục, cập nhật giá, ảnh, size, topping và trạng thái bán.</p><span class="module-action">Mở quản lý sản phẩm →</span></div></a>
            <a class="module-card" href="${ctx}/admin/recipes"><span class="module-icon"><span class="material-symbols-outlined">menu_book</span></span><div class="module-copy"><h3>Công thức &amp; định lượng</h3><p>Thiết lập hướng dẫn pha chế và lượng nguyên liệu cho từng sản phẩm.</p><span class="module-action">Mở quản lý công thức →</span></div></a>
            <a class="module-card" href="${ctx}/admin/settings"><span class="module-icon"><span class="material-symbols-outlined">tune</span></span><div class="module-copy"><h3>Cài đặt bán hàng online</h3><p>Thiết lập mức freeship, phí giao hàng, thời gian giao và điểm Aura.</p><span class="module-action">Mở cài đặt →</span></div></a>
            <a class="module-card" href="${ctx}/admin/employees"><span class="module-icon"><span class="material-symbols-outlined">group</span></span><div class="module-copy"><h3>Nhân viên &amp; tài khoản</h3><p>Thêm, sửa tài khoản nội bộ: vai trò, chức vụ, ngày vào làm, khóa / mở khóa.</p><span class="module-action">Mở quản lý nhân viên →</span></div></a>
            <a class="module-card" href="${ctx}/admin/shifts"><span class="module-icon"><span class="material-symbols-outlined">schedule</span></span><div class="module-copy"><h3>Khung giờ ca làm việc</h3><p>Quản lý thời gian các ca sáng, chiều và tối.</p><span class="module-action">Mở khung giờ ca →</span></div></a>
            <a class="module-card" href="${ctx}/admin/schedules"><span class="module-icon"><span class="material-symbols-outlined">calendar_month</span></span><div class="module-copy"><h3>Lịch phân ca nhân viên</h3><p>Xem lịch tuần, phân ca và quản lý lịch làm việc.</p><span class="module-action">Mở lịch phân ca →</span></div></a>
            <a class="module-card" href="${ctx}/admin/attendance"><span class="module-icon"><span class="material-symbols-outlined">badge</span></span><div class="module-copy"><h3>Chấm công nhân viên</h3><p>Theo dõi giờ vào/ra và trạng thái làm việc theo ngày.</p><span class="module-action">Mở chấm công →</span></div></a>
            <a class="module-card" href="${ctx}/admin/ingredients"><span class="module-icon"><span class="material-symbols-outlined">inventory_2</span></span><div class="module-copy"><h3>Nguyên vật liệu</h3><p>Danh mục nguyên liệu, đơn vị tính, nhà cung cấp và ngưỡng an toàn.</p><span class="module-action">Mở quản lý nguyên liệu →</span></div></a>
            <a class="module-card" href="${ctx}/admin/inventory"><span class="module-icon"><span class="material-symbols-outlined">inventory</span></span><div class="module-copy"><h3>Tồn kho &amp; Xuất nhập</h3><p>Theo dõi tồn kho thực tế, lịch sử xuất bán và ghi phiếu điều chỉnh.</p><span class="module-action">Mở tồn kho →</span></div></a>
            <a class="module-card" href="${ctx}/admin/restock-requests"><span class="module-icon"><span class="material-symbols-outlined">notification_important</span></span><div class="module-copy"><h3>Đề xuất nhập hàng</h3><p>Cảnh báo nguyên liệu thiếu hụt và duyệt phiếu đề xuất nhập.</p><span class="module-action">Mở đề xuất nhập hàng →</span></div></a>
            <a class="module-card" href="${ctx}/admin/suppliers"><span class="module-icon"><span class="material-symbols-outlined">local_shipping</span></span><div class="module-copy"><h3>Nhà cung cấp</h3><p>Quản lý danh sách đối tác cung ứng nguyên vật liệu.</p><span class="module-action">Mở nhà cung cấp →</span></div></a>
        </div>
    </section>

    <section id="comingSoon" class="dashboard-section" aria-labelledby="comingSoon">
        <div class="section-heading"><div><h2 id="comingSoon">Đang phát triển</h2><p>Các mục này chưa có màn hình quản lý, nên hiện chưa dẫn sang trang khác.</p></div></div>
        <div class="coming-grid">
            <div class="coming-item"><span class="material-symbols-outlined">receipt_long</span><span class="coming-name">Quản lý đơn hàng</span><span class="coming-label">Đang phát triển</span></div>
            <div class="coming-item"><span class="material-symbols-outlined">description</span><span class="coming-name">Phiếu nhập hàng (Purchase Order)</span><span class="coming-label">Đang phát triển</span></div>
            <div class="coming-item"><span class="material-symbols-outlined">group</span><span class="coming-name">Khách hàng</span><span class="coming-label">Đang phát triển</span></div>
            <div class="coming-item"><span class="material-symbols-outlined">loyalty</span><span class="coming-name">Tích điểm thành viên</span><span class="coming-label">Đang phát triển</span></div>
            <div class="coming-item"><span class="material-symbols-outlined">rate_review</span><span class="coming-name">Đánh giá và khiếu nại</span><span class="coming-label">Đang phát triển</span></div>
        </div>
    </section>
</main>
</body>
</html>
