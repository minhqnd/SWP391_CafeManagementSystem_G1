<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, dao.CategoryDAO, dao.ShopSettingDAO, model.Category, model.ShopSettings" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%
    // [TCFT01]
    // . Phần cuối trang dùng danh mục đã đọc ở Trang chủ hoặc Menu; nếu chưa có thì gọi CategoryDAO để đọc
    //   các nhóm hoạt động. Tương tự, em dùng homeSettings hoặc ShopSettingDAO.getSettings để hiện phí
    //   giao hàng, thời gian và quy tắc đổi điểm từ thiết lập đang lưu.
    // ... Kết thúc dựng cuối trang khi danh mục và các thiết lập đã được hiển thị trong footer.jsp.
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
                <%-- [TCFT02]
                     . Bấm một danh mục cuối trang mở /menu kèm category để MenuController.doGet lọc món. Tài khoản
                       thành viên mở /profile, còn Đặt hàng mở Menu; các đường dẫn này dùng chung xử lý với nút tương
                       ứng trên đầu trang, không tự ghi thêm dữ liệu.
                     ... Bấm danh mục hoặc Đặt hàng thì theo phần nhận Menu tại MenuController.java, tìm mã MNM02. Bấm
                         Tài khoản thành viên thì theo ProfileController.java, tìm mã HSM02. Trang đã mở là kết thúc
                         liên kết, chưa gửi đơn hay cập nhật hồ sơ.
                --%>
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
                <%-- [TCFT03]
                     . Dòng Mỗi bao nhiêu điểm đổi bao nhiêu tiền lấy pointsStep và pointsStepValue từ footerSettings.
                       ShopSettingDAO.getSettings đọc các quy tắc đang lưu, nên sau khi Admin lưu bộ mới và tải lại
                       trang, phần này hiển thị mức đổi mới.
                     ... Kết thúc phần quy đổi tại số điểm và tiền giảm đang hiển thị ở footer.jsp.
                --%>
                <p class="small mb-0">Mỗi ${footerSettings.pointsStep} điểm đổi <strong class="text-white"><fmt:formatNumber value="${footerSettings.pointsStepValue}" pattern="#,##0"/>đ</strong>.</p>
            </div>
        </div>
        <hr class="footer-divider my-4">
        <div class="text-center text-white-50 small">© <%= java.time.Year.now().getValue() %> Aura Café · Đồ án SWP391 - Nhóm G1</div>
    </div>
</footer>
