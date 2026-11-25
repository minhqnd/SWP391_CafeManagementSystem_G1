<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, dao.CategoryDAO, dao.ShopSettingDAO, model.Category, model.ShopSettings" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%
    // [TCFT01]
    // . Em đọc danh mục và bộ thiết lập để chân trang hiện các nhóm món cùng cách đổi điểm đang áp dụng. Nếu
    //   trang đã có danh mục thì dùng lại danh sách đó.
    //
    // Tra cứu: Danh mục từ CategoryDAO.java / MNDM01; thiết lập qua getSettings gọi ShopSettingDAO.java /
    //          TDQT07. Đọc xong quay lại đúng lời gọi tại TCFT01 rồi hiển thị chân trang; kết thúc.
    List<Category> footerCategories = (List<Category>) request.getAttribute("landingCategories");
    if (footerCategories == null) footerCategories = (List<Category>) request.getAttribute("categories");
    if (footerCategories == null) footerCategories = new CategoryDAO().getAllActiveCategoriesWithCount();
    request.setAttribute("footerCategories", footerCategories);

    ShopSettings footerSettings = (ShopSettings) request.getAttribute("homeSettings");
    if (footerSettings == null) footerSettings = new ShopSettingDAO().getSettings();
    request.setAttribute("footerSettings", footerSettings);
%>
<style>
    .aura-footer { background:#0f172a; color:#94a3b8; font-family:'Inter',sans-serif; font-size:.88rem; }
    .aura-footer a { color:#94a3b8; text-decoration:none; transition:color .2s ease; }
    .aura-footer a:hover { color:#fff; }
    .aura-footer-heading { color:#fff; font-size:.98rem; font-weight:700; margin-bottom:1rem; }
    .aura-footer-list { list-style:none; padding:0; margin:0; }
    .aura-footer-list li { margin-bottom:.65rem; }
    .footer-divider { border-color:rgba(255,255,255,.12); }
</style>
<footer class="aura-footer pt-5 pb-4 mt-auto">
    <div class="container pt-3">
        <div class="row g-4 pb-4">
            <div class="col-lg-4 col-md-6">
                <div class="d-flex align-items-center gap-2 mb-3">
                    <div class="bg-danger text-white rounded-3 d-flex align-items-center justify-content-center" style="width:40px;height:40px">
                        <span class="material-symbols-outlined fs-4">local_cafe</span>
                    </div>
                    <div class="d-flex flex-column">
                        <span class="fs-4 lh-1 text-white fw-bold">Aura Café</span>
                        <span class="text-white-50 fw-semibold" style="font-size:.65rem;letter-spacing:1.5px;text-transform:uppercase">Specialty Coffee</span>
                    </div>
                </div>
                <h4 class="aura-footer-heading">Chính sách giao hàng</h4>
                <ul class="aura-footer-list small">
                    <li>Miễn phí giao hàng: <c:choose><c:when test="${footerSettings.freeshipThreshold == 0}">mọi đơn hàng</c:when><c:otherwise>đơn từ <strong class="text-white"><fmt:formatNumber value="${footerSettings.freeshipThreshold}" pattern="#,##0"/>đ</strong></c:otherwise></c:choose></li>
                    <li>Phí giao hàng: <strong class="text-white"><fmt:formatNumber value="${footerSettings.deliveryFee}" pattern="#,##0"/>đ</strong></li>
                    <li>Thời gian dự kiến: <strong class="text-white">${footerSettings.deliveryMinutes} phút</strong></li>
                </ul>
            </div>
            <div class="col-lg-3 col-md-6 col-6">
                <h4 class="aura-footer-heading">Danh mục</h4>

                <ul class="aura-footer-list">
                    <c:forEach var="footerCategory" items="${footerCategories}">
                        <li><a href="${pageContext.request.contextPath}/menu?category=${footerCategory.id}">${footerCategory.name}</a></li>
                    </c:forEach>
                </ul>
            </div>
            <div class="col-lg-3 col-md-6 col-6">
                <h4 class="aura-footer-heading">Tài khoản & hỗ trợ</h4>
                <ul class="aura-footer-list">
                    <li><a href="${pageContext.request.contextPath}/profile">Tài khoản thành viên</a></li>
                    <li><a href="${pageContext.request.contextPath}/orders">Tra cứu đơn hàng</a></li>
                    <li><a href="${pageContext.request.contextPath}/feedback">Gửi góp ý</a></li>
                    <li><a href="${pageContext.request.contextPath}/menu">Đặt hàng</a></li>
                </ul>
            </div>
            <div class="col-lg-2 col-md-6">
                <h4 class="aura-footer-heading">Tích điểm Aura</h4>

                <p class="small mb-0">Mỗi ${footerSettings.pointsStep} điểm đổi <strong class="text-white"><fmt:formatNumber value="${footerSettings.pointsStepValue}" pattern="#,##0"/>đ</strong>.</p>
            </div>
        </div>
        <hr class="footer-divider my-4">
        <div class="text-center text-white-50 small">© <%= java.time.Year.now().getValue() %> Aura Café · Đồ án SWP391 - Nhóm G1</div>
    </div>
</footer>
