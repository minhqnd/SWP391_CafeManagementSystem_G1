<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<style>
    /* Footer Styling */
    .aura-footer {
        background-color: #0f172a;
        color: #94a3b8;
        font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
        font-size: 0.9rem;
        position: relative;
        overflow: hidden;
    }
    .aura-footer a {
        color: #94a3b8;
        text-decoration: none;
        transition: color 0.2s ease, transform 0.2s ease;
        display: inline-block;
    }
    .aura-footer a:hover {
        color: #ffffff;
        transform: translateX(3px);
    }
    .aura-footer-heading {
        color: #ffffff;
        font-size: 1rem;
        font-weight: 700;
        letter-spacing: -0.01em;
        margin-bottom: 1.25rem;
        position: relative;
    }
    .aura-footer-heading::after {
        content: '';
        position: absolute;
        bottom: -6px;
        left: 0;
        width: 24px;
        height: 2px;
        background-color: #900b1a;
        border-radius: 2px;
    }
    .aura-footer-list {
        list-style: none;
        padding-left: 0;
        margin-bottom: 0;
    }
    .aura-footer-list li {
        margin-bottom: 0.75rem;
    }
    .footer-social-btn {
        width: 38px;
        height: 38px;
        border-radius: 50%;
        background-color: rgba(255, 255, 255, 0.08);
        color: #ffffff !important;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s ease !important;
    }
    .footer-social-btn:hover {
        background-color: #900b1a;
        transform: translateY(-3px) !important;
    }
    .footer-divider {
        border-color: rgba(255, 255, 255, 0.08);
    }
</style>

<footer class="aura-footer pt-5 pb-4 mt-auto">
    <div class="container pt-3">
        <div class="row g-4 pb-4">
            <!-- Brand & About -->
            <div class="col-lg-4 col-md-6 pe-lg-5">
                <div class="d-flex align-items-center gap-2 mb-3">
                    <div class="bg-danger text-white rounded-3 p-2 d-flex align-items-center justify-content-center" style="width: 40px; height: 40px;">
                        <span class="material-symbols-outlined fs-4">local_cafe</span>
                    </div>
                    <div class="d-flex flex-column text-start">
                        <span class="fs-4 lh-1 text-white fw-bold">Aura Café</span>
                        <span class="text-white-50 fw-semibold" style="font-size: 0.65rem; letter-spacing: 1.5px; text-transform: uppercase;">Specialty Coffee</span>
                    </div>
                </div>
                <p class="text-secondary small mb-4" style="line-height: 1.7;">
                    Không gian cà phê thủ công chọn lọc từ 100% hạt Arabica Cầu Đất và Robusta Lâm Đồng danh tiếng. Trải nghiệm hương vị chuẩn mực cùng dịch vụ giao tận nơi nhanh chóng.
                </p>
                <div class="d-flex flex-column gap-2 small">
                    <div class="d-flex align-items-center gap-2">
                        <span class="material-symbols-outlined text-danger fs-5">location_on</span>
                        <span>Khu Công nghệ cao Hòa Lạc, Thạch Thất, Hà Nội</span>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <span class="material-symbols-outlined text-danger fs-5">call</span>
                        <span>Hotline đặt hàng: <strong class="text-white">1900 6868</strong></span>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <span class="material-symbols-outlined text-danger fs-5">schedule</span>
                        <span>Mở cửa: 07:00 - 22:30 (Thứ 2 - Chủ Nhật)</span>
                    </div>
                </div>
            </div>

            <!-- Menu Quick Links -->
            <div class="col-lg-2 col-md-6 col-6">
                <h4 class="aura-footer-heading">Thực đơn</h4>
                <ul class="aura-footer-list">
                    <li><a href="${pageContext.request.contextPath}/menu?category=1">Cà phê máy Espresso</a></li>
                    <li><a href="${pageContext.request.contextPath}/menu?category=2">Cà phê phin truyền thống</a></li>
                    <li><a href="${pageContext.request.contextPath}/menu?category=3">Trà trái cây nhiệt đới</a></li>
                    <li><a href="${pageContext.request.contextPath}/menu?category=4">Đá xay & Matcha</a></li>
                    <li><a href="${pageContext.request.contextPath}/menu?category=5">Bánh ngọt thơm lừng</a></li>
                    <li><a href="${pageContext.request.contextPath}/menu?category=6">Cold Brew đóng chai</a></li>
                </ul>
            </div>

            <!-- Customer & Privileges -->
            <div class="col-lg-3 col-md-6 col-6">
                <h4 class="aura-footer-heading">Khách hàng & Hội viên</h4>
                <ul class="aura-footer-list">
                    <li><a href="${pageContext.request.contextPath}/profile">Thẻ thành viên Aura Privilege</a></li>
                    <li><a href="${pageContext.request.contextPath}/profile#privileges">Chính sách tích điểm A-Point</a></li>
                    <li><a href="${pageContext.request.contextPath}/orders">Tra cứu trạng thái đơn hàng</a></li>
                    <li><a href="${pageContext.request.contextPath}/feedback">Góp ý & Phản ánh chất lượng</a></li>
                    <li><a href="${pageContext.request.contextPath}/menu">Đặt hàng giao tận nơi</a></li>
                </ul>
            </div>

            <!-- Social & Newsletter -->
            <div class="col-lg-3 col-md-6">
                <h4 class="aura-footer-heading">Kết nối với Aura</h4>
                <p class="small text-secondary mb-3">Theo dõi fanpage và cập nhật các công thức món mới cùng ưu đãi độc quyền.</p>
                <div class="d-flex gap-2 mb-4">
                    <a href="#" class="footer-social-btn" title="Facebook"><span class="material-symbols-outlined fs-5">thumb_up</span></a>
                    <a href="#" class="footer-social-btn" title="Instagram"><span class="material-symbols-outlined fs-5">photo_camera</span></a>
                    <a href="#" class="footer-social-btn" title="TikTok"><span class="material-symbols-outlined fs-5">music_note</span></a>
                    <a href="#" class="footer-social-btn" title="Email"><span class="material-symbols-outlined fs-5">mail</span></a>
                </div>
                <div class="p-3 rounded-3" style="background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(255, 255, 255, 0.08);">
                    <div class="d-flex align-items-center gap-2 text-white small fw-semibold mb-1">
                        <span class="material-symbols-outlined text-warning fs-5">verified</span>
                        <span>Cam kết 100% hạt sạch</span>
                    </div>
                    <span class="text-white-50" style="font-size: 0.75rem;">Cà phê tuyển chọn mộc, rang xay tươi mới mỗi ngày.</span>
                </div>
            </div>
        </div>

        <hr class="footer-divider my-4">

        <!-- Copyright & Disclaimers -->
        <div class="d-flex flex-column flex-md-row align-items-center justify-content-between gap-3 text-white-50 small">
            <div>
                © 2026 Aura Café. Đồ án SWP391 - Nhóm G1. Bảo lưu mọi quyền.
            </div>
            <div class="d-flex align-items-center gap-3">
                <span class="d-flex align-items-center gap-1"><span class="material-symbols-outlined fs-6 text-success">lock</span> Thanh toán an toàn</span>
                <span>•</span>
                <span class="d-flex align-items-center gap-1"><span class="material-symbols-outlined fs-6 text-info">local_shipping</span> Giao nóng 15 phút</span>
                <span>•</span>
                <span class="d-flex align-items-center gap-1"><span class="material-symbols-outlined fs-6 text-danger">workspace_premium</span> Chất lượng chuẩn quán</span>
            </div>
        </div>
    </div>
</footer>
