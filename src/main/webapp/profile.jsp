<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    if (request.getDispatcherType() != jakarta.servlet.DispatcherType.FORWARD) {
        response.sendRedirect(request.getContextPath() + "/profile");
        if (true) return;
    }
%>
<% utils.AccountCsrf.token(session); %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
            <fmt:setLocale value="vi_VN" />

            <c:if test="${empty sessionScope.user}">
                <c:redirect url="/?login=true" />
            </c:if>
            <!DOCTYPE html>
            <html lang="vi">

            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Hồ sơ & Điểm tích luỹ - Aura Café</title>

                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">

                <link
                    href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap"
                    rel="stylesheet">

                <link
                    href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200"
                    rel="stylesheet">

                <style>
                    :root {
                        --primary: #900b1a;
                        --primary-hover: #7a0915;
                        --surface-bg: #f8f9fa;
                        --font-family: 'Plus Jakarta Sans', sans-serif;
                        --gold: #FFD700;
                    }

                    body {
                        font-family: var(--font-family);
                        background-color: var(--surface-bg);
                        color: #1a1c1e;
                    }

                    .text-primary-custom {
                        color: var(--primary) !important;
                    }

                    .bg-primary-custom {
                        background-color: var(--primary) !important;
                    }

                    .points-dashboard-card {
                        background: white;
                        border-radius: 1rem;
                        padding: 2rem 2.5rem;
                        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.04);
                        border: 1px solid #e9ecef;
                    }

                    .barcode-card {
                        background: white;
                        border-radius: 1rem;
                        padding: 1.5rem;
                        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.04);
                        border: 1px solid #e9ecef;
                        height: 100%;
                        display: flex;
                        flex-direction: column;
                    }

                    .barcode-box {
                        background-color: #f8f9fa;
                        border: 1px dashed #dee2e6;
                        border-radius: 0.75rem;
                        padding: 1.5rem;
                        text-align: center;
                        margin: 1rem 0;
                    }

                    .profile-nav-bar {
                        background: white;
                        border-radius: 0.75rem;
                        padding: 0.5rem;
                        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
                        display: flex;
                        gap: 0.5rem;
                        border: 1px solid #e9ecef;
                        overflow-x: auto;
                        flex-wrap: nowrap;
                    }

                    .profile-nav-item {
                        color: #6c757d;
                        font-weight: 600;
                        padding: 0.75rem 1.25rem;
                        border-radius: 0.5rem;
                        text-decoration: none;
                        display: inline-flex;
                        align-items: center;
                        gap: 0.5rem;
                        transition: all 0.2s;
                        border: none;
                        background: transparent;
                        white-space: nowrap;
                    }

                    .profile-nav-item:hover {
                        background-color: #f8f9fa;
                        color: #1a1c1e;
                    }

                    .profile-nav-item.active {
                        background-color: var(--primary);
                        color: white;
                    }

                    .info-card {
                        background: white;
                        border-radius: 1rem;
                        padding: 2rem;
                        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.04);
                        border: 1px solid #e9ecef;
                        height: 100%;
                    }

                    .avatar-wrapper {
                        position: relative;
                        width: 120px;
                        height: 120px;
                        margin: 0 auto 1.5rem auto;
                    }

                    .avatar-img {
                        width: 100%;
                        height: 100%;
                        border-radius: 50%;
                        object-fit: cover;
                        border: 4px solid white;
                        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
                    }

                    .avatar-edit-btn {
                        position: absolute;
                        bottom: 0;
                        right: 0;
                        width: 36px;
                        height: 36px;
                        background-color: var(--primary);
                        color: white;
                        border-radius: 50%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        border: 3px solid white;
                        cursor: pointer;
                        box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
                    }

                    .badge-member-since {
                        background-color: #fff8e1;
                        color: #b8860b;
                        border: 1px solid #ffe082;
                        padding: 0.35rem 1rem;
                        border-radius: 2rem;
                        font-size: 0.75rem;
                        font-weight: 700;
                    }

                    .form-label-custom {
                        font-size: 0.85rem;
                        font-weight: 600;
                        color: #495057;
                        margin-bottom: 0.4rem;
                    }

                    .form-control-custom {
                        background-color: #f8f9fa;
                        border: 1px solid #e9ecef;
                        border-radius: 0.5rem;
                        padding: 0.75rem 1rem;
                        font-size: 0.95rem;
                        color: #495057;
                    }

                    .form-control-custom:focus {
                        background-color: white;
                        border-color: var(--primary);
                        box-shadow: 0 0 0 0.25rem rgba(144, 11, 26, 0.1);
                        outline: none;
                    }

                    .btn-outline-primary-custom {
                        color: var(--primary);
                        border: 1px solid var(--primary);
                        background: transparent;
                        font-weight: 600;
                        border-radius: 0.5rem;
                        padding: 0.5rem 1rem;
                        transition: all 0.2s;
                    }

                    .btn-outline-primary-custom:hover {
                        background: var(--primary);
                        color: white;
                    }

                    .btn-primary-custom {
                        background-color: var(--primary);
                        color: white;
                        border: 1px solid var(--primary);
                        font-weight: 600;
                        border-radius: 0.5rem;
                        transition: all 0.2s;
                    }

                    .btn-primary-custom:hover {
                        background-color: var(--primary-hover);
                        color: white;
                    }

                    .toggle-icon {
                        transition: transform 0.3s ease;
                    }

                    [aria-expanded="true"] .toggle-icon {
                        transform: rotate(180deg);
                    }
                </style>
            </head>

            <body>

                <jsp:include page="/components/header.jsp">
                    <jsp:param name="activePage" value="profile" />
                </jsp:include>

                <%-- [HSM05]
                     . Khi nhận thông tin hồ sơ đã chuẩn bị, profile.jsp hiển thị điểm, mã thành viên,
                       thông tin cá nhân và phần bảo mật của tài khoản. Người dùng chọn các tab để xem
                       thông tin tài khoản hoặc lịch sử điểm ngay trên cùng trang.

                     Kết thúc: mở hồ sơ tại nội dung trang.
                --%>
                <main class="py-5">
                    <div class="container">

                        <div class="row g-4 mb-4">
                            <div class="col-lg-12">
                                <div class="points-dashboard-card d-flex justify-content-between align-items-center">
                                    <div class="d-flex align-items-center gap-4">
                                        <div class="rounded-circle bg-primary-custom d-flex align-items-center justify-content-center shadow-sm"
                                            style="width: 60px; height: 60px;">
                                            <span class="material-symbols-outlined text-white fs-2">toll</span>
                                        </div>
                                        <div>
                                            <div class="text-muted mb-1" style="font-size: 0.85rem; font-weight: 600; text-transform: uppercase;">
                                                Điểm Aura tích luỹ
                                            </div>
                                            <div class="d-flex align-items-baseline gap-2">
                                                <h2 class="fs-1 fw-bold text-dark mb-0 lh-1">

                                                    ${sessionScope.user.totalPoints != null ? sessionScope.user.totalPoints : 0}
                                                </h2>
                                                <span class="fs-5 fw-bold text-muted">pts</span>
                                            </div>
                                            <div class="text-muted mt-1" style="font-size: 0.8rem;">
                                                <%-- [HSGIO01]
                                                     . Em cho liên kết Giỏ hàng mở trang giỏ để người dùng
                                                       áp dụng điểm khi đặt hàng.

                                                     Tra cứu: Nhận mở giỏ tại CartController.java / HSGIO02. Phần
                                                              đặt hàng tiếp tục ở chức năng giỏ hàng.
                                                --%>
                                                Dùng điểm trừ thẳng vào đơn ở <a href="${pageContext.request.contextPath}/cart" class="text-primary-custom fw-semibold">Giỏ hàng</a>:
                                                mỗi ${shopSettings.pointsStep} điểm giảm <fmt:formatNumber value="${shopSettings.pointsStepValue}" pattern="#,##0" />đ
                                            </div>
                                        </div>
                                    </div>
                                    <div class="text-end border-start ps-4">
                                        <div class="text-muted mb-2" style="font-size: 0.9rem; font-weight: 500;">
                                            Chủ tài khoản: <span class="text-dark fw-bold">${fn:escapeXml(sessionScope.user.fullName)}</span>
                                        </div>
                                        <div class="d-flex align-items-center justify-content-end gap-2 text-muted">

                                            <span id="memberIdBadge" style="font-family: monospace; font-size: 1rem; background: #f8f9fa; padding: 0.4rem 0.8rem; border-radius: 0.5rem; border: 1px dashed #ced4da; font-weight: 600; color: var(--primary);">#AURA-${sessionScope.user.id != null ? sessionScope.user.id : ''}</span>

                                            <span class="material-symbols-outlined fs-5" style="cursor: pointer; padding: 0.3rem; border-radius: 0.3rem;"
                                                title="Sao chép mã"
                                                onclick="copyMemberId(document.getElementById('memberIdBadge').innerText.trim())"
                                                onmouseover="this.style.background='#e9ecef'"
                                                onmouseout="this.style.background='transparent'">content_copy</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <%-- [HSL05]
                             . Khi trang hồ sơ được hiển thị lại, vùng thông báo này cho người dùng biết
                               đã lưu hồ sơ hoặc đổi mật khẩu thành công hay chưa. Nếu có lỗi theo từng ô,
                               lời báo được đưa về đúng form để người dùng sửa tiếp.

                             Kết thúc: lưu hồ sơ hoặc đổi mật khẩu tại thông báo trên trang.
                        --%>
                        <c:if test="${not empty successMsg and empty errorMsg}">
                            <div data-account-notice role="status"
                                class="alert alert-success fw-bold border-0 py-3 mb-4 shadow-sm d-flex align-items-center gap-2">
                                <span class="material-symbols-outlined">check_circle</span>
                                <c:out value="${successMsg}" />
                            </div>
                        </c:if>
                        <c:if test="${not empty errorMsg}">
                            <div data-account-notice role="alert"
                                class="alert alert-danger fw-bold border-0 py-3 mb-4 shadow-sm d-flex align-items-center gap-2">
                                <span class="material-symbols-outlined">error</span>
                                <c:out value="${errorMsg}" />
                            </div>
                        </c:if>

                        <ul class="nav profile-nav-bar mb-4" id="profileTab" role="tablist">
                            <li class="nav-item flex-fill text-center" role="presentation">
                                <%-- [HSLS01]
                                     . Tab này mở bảng lịch sử đã được chuẩn bị cùng trang hồ sơ, nên bấm
                                       tab chỉ đổi nội dung đang xem. Nguồn dữ liệu của bảng là
                                       getLoyaltyHistoryByUserId trong UserDAO, được renderProfile của
                                       ProfileController gọi khi dựng trang.

                                     Tra tiếp: UserDAO.java / HSLS02.
                                --%>
                                <button class="nav-link profile-nav-item w-100 justify-content-center" id="history-tab"
                                    data-bs-toggle="tab" data-bs-target="#history" type="button" role="tab"><span
                                        class="material-symbols-outlined fs-5">history</span> Lịch sử tích điểm</button>
                            </li>
                            <li class="nav-item flex-fill text-center" role="presentation">

                                <%-- [HSTAB01]
                                     . Khi người dùng bấm Thông tin tài khoản, em dùng phần chuyển tab có sẵn
                                       trên trang để hiện lại hồ sơ và phần bảo mật, đồng thời ẩn bảng lịch
                                       sử. Thông tin đã được chuẩn bị khi mở hồ sơ, nên bấm tab không gửi yêu
                                       cầu lưu hay đọc lại tài khoản.

                                     Kết thúc: hiển thị tab Thông tin tài khoản ngay trong profile.jsp.
                                --%>
                                <button class="nav-link profile-nav-item active w-100 justify-content-center"
                                    id="account-tab" data-bs-toggle="tab" data-bs-target="#account" type="button"
                                    role="tab"><span class="material-symbols-outlined fs-5">manage_accounts</span> Thông
                                    tin tài khoản</button>
                            </li>
                        </ul>

                        <div class="tab-content" id="profileTabContent">

                            <div class="tab-pane fade show active" id="account" role="tabpanel" tabindex="0">
                                <div class="row g-4">

                                    <div class="col-lg-4">
                                        <div class="info-card text-center d-flex flex-column">
                                            <div class="avatar-wrapper mt-4">
                                                <img src="https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=200"
                                                    alt="Avatar" class="avatar-img">
                                                <%-- [HSAN01]
                                                     . Em đặt biểu tượng máy ảnh tại khu vực ảnh đại diện
                                                       của hồ sơ. Hiện biểu tượng này mới có phần hiển
                                                       thị, chưa có chức năng chọn và lưu ảnh đại diện
                                                       mới.

                                                     Kết thúc: Chỉ có giao diện, chưa có xử lý lưu ảnh đại diện
                                                               mới.
                                                --%>
                                                <div class="avatar-edit-btn">
                                                    <span class="material-symbols-outlined"
                                                        style="font-size: 1.1rem;">photo_camera</span>
                                                </div>
                                            </div>

                                            <h2 class="fs-4 fw-bold mb-1">${fn:escapeXml(sessionScope.user.fullName)}</h2>
                                            <p class="text-muted mb-3"
                                                style="font-size: 0.85rem; font-family: monospace;">
                                                #AURA-${sessionScope.user.id != null ? sessionScope.user.id : ''}</p>

                                        </div>
                                    </div>

                                    <div class="col-lg-8">
                                        <div class="info-card">
                                            <div
                                                class="d-flex justify-content-between align-items-start mb-4 pb-3 border-bottom">
                                                <div>
                                                    <h2 class="fs-5 fw-bold mb-1">Thông tin cá nhân & Địa chỉ</h2>
                                                    <p class="text-muted mb-0" style="font-size: 0.85rem;">Quản lý dữ
                                                        liệu để nhận quà sinh nhật và giao hàng nhanh</p>
                                                </div>

                                                <%-- [HSS01]
                                                     . Khi người dùng bấm Chỉnh sửa, sự kiện của nút ngay
                                                       trong profile.jsp sẽ mở các ô được phép sửa. Em xử
                                                       lý việc mở form trên trang trước, nên lúc này chưa
                                                       có thông tin nào được gửi để lưu.

                                                     Tra tiếp: HSS02.
                                                --%>
                                                <button type="button" id="btnEditProfile"
                                                    class="btn-outline-primary-custom d-flex align-items-center gap-2">
                                                    <span class="material-symbols-outlined fs-5">edit</span> Chỉnh sửa
                                                </button>
                                            </div>

                                            <%-- [HSL01]
                                                 . Ở form hồ sơ, em đặt họ tên, số điện thoại, email, ngày
                                                   sinh và địa chỉ; riêng số điện thoại đang được khóa
                                                   trên trang. Khi người dùng bấm Lưu, các ô được kiểm tra
                                                   rồi gửi tới ProfileController. Controller gọi update để
                                                   xử lý thông tin của chính tài khoản đang đăng nhập.

                                                 Tra tiếp: ProfileController.java / HSL02.
                                            --%>
                                            <form id="profileForm"
                                                action="${pageContext.request.contextPath}/profile?action=update"
                                                method="POST">
                                                <input type="hidden" name="csrfToken" value="${sessionScope.accountCsrfToken}">
                                                <div class="row g-4">
                                                    <div class="col-md-6">
                                                        <label class="form-label-custom">Họ và tên</label>

                                                        <input type="text" name="fullName"
                                                            class="form-control-custom w-100 profile-input"
                                                            value="${fn:escapeXml(sessionScope.user.fullName)}" minlength="1" readonly
                                                            required>
                                                    </div>
                                                    <div class="col-md-6">
                                                        <label class="form-label-custom">Số điện thoại</label>

                                                        <input type="tel" name="phone"
                                                            class="form-control-custom w-100 profile-input"
                                                            value="${fn:escapeXml(sessionScope.user.phone)}"
                                                            pattern="0[0-9]{9}" inputmode="numeric"
                                                            title="Nhập 10 chữ số và bắt đầu bằng số 0"
                                                            readonly required>
                                                    </div>
                                                    <div class="col-md-6">
                                                        <label class="form-label-custom">Địa chỉ Email</label>

                                                        <input type="email" name="email" maxlength="150"
                                                            class="form-control-custom w-100 profile-input"
                                                            value="${fn:escapeXml(sessionScope.user.email)}" readonly required>
                                                    </div>
                                                    <div class="col-md-6">
                                                        <label class="form-label-custom">Ngày sinh nhật</label>
                                                        <div class="d-flex gap-2">

                                                            <select id="birthDay" name="birthDay"
                                                                class="form-select form-control-custom w-100 profile-input"
                                                                disabled></select>
                                                            <select id="birthMonth" name="birthMonth"
                                                                class="form-select form-control-custom w-100 profile-input"
                                                                disabled></select>
                                                            <select id="birthYear" name="birthYear"
                                                                class="form-select form-control-custom w-100 profile-input"
                                                                disabled></select>
                                                        </div>

                                                        <input type="hidden" name="birthday" id="birthdayHidden"
                                                            value="${fn:escapeXml(sessionScope.user.birthday)}">
                                                    </div>
                                                    <div class="col-12">
                                                        <label class="form-label-custom">Địa chỉ nhận hàng mặc
                                                            định</label>

                                                        <input type="text" name="address" maxlength="500"
                                                            class="form-control-custom w-100 profile-input"
                                                            value="${fn:escapeXml(sessionScope.user.address)}" readonly>
                                                    </div>
                                                </div>

                                                <div id="editActions"
                                                    class="mt-4 pt-3 border-top d-none justify-content-end gap-2">
                                                    <%-- [HSH01]
                                                         . Khi người dùng bấm Hủy, em gọi sự kiện xử lý
                                                           ngay trong profile.jsp để bỏ phần đang chỉnh
                                                           sửa. Sự kiện này đưa các ô về thông tin ban đầu
                                                           và khóa form trở lại.

                                                         Tra tiếp: HSH02.
                                                    --%>
                                                    <button type="button" id="btnCancelEdit"
                                                        class="btn btn-light fw-bold px-4">Hủy</button>

                                                    <button type="submit" id="btnSaveProfile"
                                                        class="btn btn-secondary px-4 fw-bold" disabled>Lưu thay
                                                        đổi</button>
                                                </div>
                                            </form>

                                            <hr class="my-5 border-secondary-subtle">

                                            <%-- [HSBM01]
                                                 . Khi người dùng bấm Bảo mật & Đổi mật khẩu, em dùng phần mở
                                                   rộng và thu gọn ngay trên trang để hiện hoặc ẩn ba ô mật
                                                   khẩu. Thao tác này chỉ mở phần nhập; mật khẩu chưa thay đổi
                                                   cho đến khi người dùng điền thông tin và bấm Cập nhật.

                                                 Kết thúc: mở hoặc thu gọn phần nhập. Nếu bấm Cập nhật, tra profile.jsp / MKL01.
                                            --%>
                                            <div class="d-flex justify-content-between align-items-center mb-0 pb-3 border-bottom cursor-pointer"
                                                data-bs-toggle="collapse" data-bs-target="#passwordCollapse"
                                                aria-expanded="false" aria-controls="passwordCollapse"
                                                style="cursor: pointer; user-select: none;">
                                                <div>
                                                    <h3 class="fs-5 fw-bold mb-1 d-flex align-items-center gap-2">
                                                        Bảo mật & Đổi mật khẩu
                                                    </h3>
                                                    <p class="text-muted mb-0" style="font-size: 0.85rem;">Bảo vệ tài
                                                        khoản của bạn bằng mật khẩu an toàn</p>
                                                </div>
                                                <div class="d-flex align-items-center gap-2">
                                                    <span class="material-symbols-outlined text-muted fs-4">lock</span>
                                                    <span
                                                        class="material-symbols-outlined text-muted fs-4 toggle-icon">expand_more</span>
                                                </div>
                                            </div>

                                            <div class="collapse mt-4" id="passwordCollapse">

                                                <%-- [MKL01]
                                                     . Ở phần Đổi mật khẩu, em cho người dùng nhập mật
                                                       khẩu hiện tại, mật khẩu mới và xác nhận mật khẩu
                                                       mới. Bấm Cập nhật thì các ô được kiểm tra trên
                                                       trang trước, sau đó gửi tới doPost của
                                                       ProfileController để đối chiếu mật khẩu hiện tại và
                                                       xử lý việc đổi.

                                                     Tra tiếp: ProfileController.java / MKL02.
                                                --%>
                                                <form id="passwordForm"
                                                    action="${pageContext.request.contextPath}/profile?action=changePassword"
                                                    method="POST">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope.accountCsrfToken}">
                                                    <div class="row g-4">
                                                        <div class="col-12">
                                                            <label class="form-label-custom">Mật khẩu hiện tại</label>
                                                            <div class="input-group">

                                                                <input type="password" name="oldPassword" maxlength="255"
                                                                    class="form-control form-control-custom bg-white border-end-0"
                                                                    placeholder="Nhập mật khẩu hiện tại" required>
                                                                <span
                                                                    class="input-group-text bg-white border-start-0 cursor-pointer"
                                                                    onclick="togglePassword(this)"
                                                                    style="cursor: pointer; border-color: #e9ecef;">
                                                                    <span
                                                                        class="material-symbols-outlined fs-5 text-muted">visibility_off</span>
                                                                </span>
                                                            </div>
                                                        </div>
                                                        <div class="col-md-6">
                                                            <label class="form-label-custom">Mật khẩu mới</label>
                                                            <div class="input-group">

                                                                <input type="password" id="newPassword"
                                                                    name="newPassword" maxlength="255"
                                                                    class="form-control form-control-custom bg-white border-end-0"
                                                                    placeholder="Nhập mật khẩu mới" minlength="8"
                                                                    required>
                                                                <span
                                                                    class="input-group-text bg-white border-start-0 cursor-pointer"
                                                                    onclick="togglePassword(this)"
                                                                    style="cursor: pointer; border-color: #e9ecef;">
                                                                    <span
                                                                        class="material-symbols-outlined fs-5 text-muted">visibility_off</span>
                                                                </span>
                                                            </div>
                                                            <div class="invalid-feedback" id="newPasswordFeedback">Mật
                                                                khẩu phải có ít nhất 8 ký tự, gồm chữ và số.</div>
                                                        </div>
                                                        <div class="col-md-6">
                                                            <label class="form-label-custom">Xác nhận mật khẩu
                                                                mới</label>
                                                            <div class="input-group">

                                                                <input type="password" id="confirmPassword"
                                                                    name="confirmPassword" maxlength="255"
                                                                    class="form-control form-control-custom bg-white border-end-0"
                                                                    placeholder="Nhập lại mật khẩu mới" required>
                                                                <span
                                                                    class="input-group-text bg-white border-start-0 cursor-pointer"
                                                                    onclick="togglePassword(this)"
                                                                    style="cursor: pointer; border-color: #e9ecef;">
                                                                    <span
                                                                        class="material-symbols-outlined fs-5 text-muted">visibility_off</span>
                                                                </span>
                                                            </div>
                                                            <div class="invalid-feedback" id="confirmPasswordFeedback">
                                                                Mật khẩu xác nhận không khớp.</div>
                                                        </div>
                                                    </div>

                                                    <div class="mt-4 pt-3 border-top d-flex justify-content-end">

                                                        <button type="submit"
                                                            class="btn btn-outline-primary-custom px-4 fw-bold">Cập nhật
                                                            mật khẩu</button>
                                                    </div>
                                                </form>
                                            </div>

                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="tab-pane fade" id="history" role="tabpanel" tabindex="0">
                                <div class="info-card">
                                    <div
                                        class="d-flex justify-content-between align-items-start mb-4 pb-3 border-bottom">
                                        <div>
                                            <h3 class="fs-5 fw-bold mb-1">Lịch sử biến động điểm thưởng</h3>
                                            <p class="text-muted mb-0" style="font-size: 0.85rem;">Theo dõi chi tiết các
                                                lần tích và tiêu điểm của bạn</p>
                                        </div>
                                    </div>

                                    <div class="d-flex flex-column">
                                        <c:choose>
                                            <c:when test="${not empty loyaltyHistoryError}">
                                                <div class="alert alert-warning" role="alert"><c:out value="${loyaltyHistoryError}" /></div>
                                            </c:when>
                                            <c:when test="${empty loyaltyHistory}">
                                                <div class="text-center py-5 text-muted">
                                                    <span
                                                        class="material-symbols-outlined fs-1 mb-2 text-black-50">history_toggle_off</span>
                                                    <p class="mb-0">Bạn chưa có lịch sử tích/tiêu điểm nào.</p>
                                                </div>
                                            </c:when>
                                            <c:otherwise>

                                                <%-- [HSLS03]
                                                     . Khi mở tab Lịch sử tích điểm, người dùng thấy thời
                                                       gian, loại giao dịch và số điểm thay đổi từ danh
                                                       sách đã đọc cùng trang hồ sơ. Các thông tin này
                                                       giúp đối chiếu từng lần tích điểm hoặc sử dụng điểm
                                                       của tài khoản.

                                                     Kết thúc: xem lịch sử tại bảng giao dịch.
                                                --%>
                                                <c:forEach var="txn" items="${loyaltyHistory}" varStatus="loop">
                                                    <div
                                                        class="d-flex justify-content-between align-items-center py-3 ${not loop.last ? 'border-bottom' : ''}">
                                                        <div class="d-flex align-items-center gap-3">
                                                            <span
                                                                class="material-symbols-outlined ${txn.pointsChange >= 0 ? 'text-success border-success' : 'text-danger border-danger'} border rounded-circle p-1">
                                                                ${txn.pointsChange >= 0 ? 'add' : 'remove'}
                                                            </span>
                                                            <div>
                                                                <div class="fw-bold fs-6">
                                                                    <c:choose>
                                                                        <c:when test="${txn.transactionType == 'EARN'}">
                                                                            Tích điểm từ đơn hàng #${txn.orderId}
                                                                        </c:when>
                                                                        <c:when test="${txn.orderId > 0}">
                                                                            Dùng điểm giảm giá đơn hàng #${txn.orderId}
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                            Sử dụng điểm đổi ưu đãi
                                                                        </c:otherwise>
                                                                    </c:choose>
                                                                </div>
                                                                <div class="text-muted small">
                                                                    <fmt:formatDate value="${txn.createdAt}"
                                                                        pattern="dd/MM/yyyy HH:mm" />
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div class="text-end">
                                                            <div
                                                                class="fw-bold ${txn.pointsChange >= 0 ? 'text-success' : 'text-danger'} fs-5">
                                                                ${txn.pointsChange > 0 ? '+' : ''}${txn.pointsChange}
                                                            </div>
                                                            <div class="text-muted" style="font-size: 0.75rem;">Mã GD:
                                                                #${txn.id}</div>
                                                        </div>
                                                    </div>
                                                </c:forEach>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                            </div>

                        </div>
                    </div>
                </main>

                <jsp:include page="/components/auth_modal.jsp" />

                <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

                <script>
                    document.addEventListener('DOMContentLoaded', function () {
                        const btnEdit = document.getElementById('btnEditProfile');
                        const btnCancel = document.getElementById('btnCancelEdit');
                        const btnSave = document.getElementById('btnSaveProfile');
                        const editActions = document.getElementById('editActions');
                        const profileInputs = document.querySelectorAll('.profile-input');

                        const originalValues = {};

                        // So thông tin đang sửa với bản cũ để bật nút Lưu khi có thay đổi.
                        function checkFormChanged() {
                            let isChanged = false;
                            profileInputs.forEach(input => {
                                if (input.value !== originalValues[input.name || input.id]) {
                                    isChanged = true;
                                }
                            });

                            if (isChanged) {
                                btnSave.removeAttribute('disabled');
                                btnSave.classList.remove('btn-secondary');
                                btnSave.classList.add('btn-primary-custom');
                            } else {
                                btnSave.setAttribute('disabled', 'disabled');
                                btnSave.classList.remove('btn-primary-custom');
                                btnSave.classList.add('btn-secondary');
                            }
                        }

                        profileInputs.forEach(input => {
                            input.addEventListener('input', checkFormChanged);
                            input.addEventListener('change', checkFormChanged);
                        });

                        const birthDay = document.getElementById('birthDay');
                        const birthMonth = document.getElementById('birthMonth');
                        const birthYear = document.getElementById('birthYear');
                        const birthdayHidden = document.getElementById('birthdayHidden');

                        const curYear = new Date().getFullYear();
                        birthYear.innerHTML = '<option value="">Năm</option>';
                        for (let i = curYear; i >= 1900; i--) {
                            birthYear.innerHTML += '<option value="' + i + '">' + i + '</option>';
                        }

                        birthMonth.innerHTML = '<option value="">Tháng</option>';
                        for (let i = 1; i <= 12; i++) {
                            birthMonth.innerHTML += '<option value="' + i + '">Tháng ' + i + '</option>';
                        }

                        // [HSNS01]
                        // . Ở phần ngày sinh, em dùng ba ô ngày, tháng và năm rồi ghép thành một ngày để
                        //   gửi cùng form hồ sơ. Khi tháng hoặc năm thay đổi, em cập nhật số ngày được
                        //   chọn để người dùng chọn đúng ngày có thật.
                        //
                        // Tra cứu: Kết thúc chọn ngày trên form HSL01. Bấm Lưu mới gửi tới
                        //          ProfileController.java / HSL02.
                        function updateDays() {
                            const d = birthDay.value;
                            const m = parseInt(birthMonth.value) || 1;
                            const y = parseInt(birthYear.value) || 2000;
                            const daysInMonth = new Date(y, m, 0).getDate();

                            birthDay.innerHTML = '<option value="">Ngày</option>';
                            for (let i = 1; i <= daysInMonth; i++) {
                                birthDay.innerHTML += '<option value="' + i + '">' + i + '</option>';
                            }

                            if (d && parseInt(d) <= daysInMonth) {
                                birthDay.value = d;
                            }
                        }

                        birthMonth.addEventListener('change', updateDays);
                        birthYear.addEventListener('change', updateDays);

                        // Ghép ngày, tháng, năm thành một ngày sinh để gửi cùng hồ sơ.
                        function syncBirthday() {
                            const selected = [birthYear.value, birthMonth.value, birthDay.value].filter(Boolean).length;
                            birthDay.setCustomValidity(selected === 0 || selected === 3 ? '' : 'Vui lòng chọn đủ ngày, tháng và năm sinh.');
                            if (birthYear.value && birthMonth.value && birthDay.value) {
                                const y = birthYear.value;
                                const m = birthMonth.value.padStart(2, '0');
                                const d = birthDay.value.padStart(2, '0');
                                birthdayHidden.value = y + '-' + m + '-' + d;
                            } else {
                                birthdayHidden.value = '';
                            }
                            checkFormChanged();
                        }

                        birthDay.addEventListener('change', syncBirthday);
                        birthMonth.addEventListener('change', syncBirthday);
                        birthYear.addEventListener('change', syncBirthday);

                        updateDays();
                        if (birthdayHidden.value) {
                            const parts = birthdayHidden.value.split('-'); // YYYY-MM-DD
                            if (parts.length === 3) {
                                birthYear.value = parts[0];
                                birthMonth.value = parseInt(parts[1]).toString();
                                updateDays(); // Cập nhật lại số ngày theo tháng/năm
                                birthDay.value = parseInt(parts[2]).toString();
                            }
                        }
                        originalValues['birthdayHidden'] = birthdayHidden.value;
                        originalValues['birthDay'] = birthDay.value;
                        originalValues['birthMonth'] = birthMonth.value;
                        originalValues['birthYear'] = birthYear.value;
                        profileInputs.forEach(input => { originalValues[input.name || input.id] = input.value; });

                        // [HSS02]
                        // . Khi mở phần chỉnh sửa hồ sơ, em giữ lại thông tin ban đầu rồi cho phép nhập
                        //   vào các ô được sửa. Hàm checkFormChanged so nội dung đang nhập với bản ban
                        //   đầu để chỉ bật nút Lưu khi có thay đổi. Bản thông tin này cũng được dùng để
                        //   khôi phục các ô nếu người dùng bấm Hủy.
                        //
                        // Kết thúc: mở sửa; các ô được mở nhưng thông tin chưa được lưu.
                        btnEdit.addEventListener('click', function () {
                            profileInputs.forEach(input => {
                                if (input.id !== 'birthdayHidden') { // Bỏ qua input ảo
                                    if (input.tagName === 'SELECT') {
                                        input.removeAttribute('disabled');
                                    } else {
                                        input.removeAttribute('readonly');
                                    }
                                    input.style.backgroundColor = 'white';
                                    input.style.borderColor = 'var(--primary)';
                                }
                                if (originalValues[input.name] === undefined && input.name) {
                                    originalValues[input.name] = input.value;
                                }
                            });

                            if (profileInputs.length > 0 && profileInputs[0].tagName !== 'SELECT') profileInputs[0].focus();

                            editActions.classList.remove('d-none');
                            editActions.classList.add('d-flex');
                            btnEdit.classList.add('d-none');
                            checkFormChanged(); // Reset button state
                        });

                        // [HSH02]
                        // . Trong sự kiện Hủy, em điền lại các giá trị đã giữ trước khi mở sửa rồi khóa
                        //   form. Vì không gửi form tới ProfileController nên hồ sơ đang lưu không bị
                        //   thay đổi bởi phần nhập dở.
                        //
                        // Kết thúc: Hủy; các ô về thông tin cũ, không ghi dữ liệu.
                        btnCancel.addEventListener('click', function () {
                            profileInputs.forEach(input => {
                                if (input.name && originalValues[input.name] !== undefined) {
                                    input.value = originalValues[input.name];
                                } else if (input.id && originalValues[input.id] !== undefined) {
                                    input.value = originalValues[input.id];
                                }

                                if (input.tagName === 'SELECT') {
                                    input.setAttribute('disabled', 'disabled');
                                } else {
                                    input.setAttribute('readonly', 'readonly');
                                }
                                input.style.backgroundColor = '';
                                input.style.borderColor = '';
                                input.classList.remove('is-invalid');
                            });

                            birthdayHidden.value = originalValues['birthdayHidden'];
                            birthDay.setCustomValidity('');

                            editActions.classList.remove('d-flex');
                            editActions.classList.add('d-none');
                            btnEdit.classList.remove('d-none');
                        });
                    });

                    function showToast(message) {
                        const toastEl = document.getElementById('profileToast');
                        const toastMsg = document.getElementById('profileToastMsg');
                        if (toastEl && toastMsg) {
                            toastMsg.textContent = message;
                            const toast = new bootstrap.Toast(toastEl, { delay: 2500 });
                            toast.show();
                        }
                    }

                    // [HSBT01]
                    // . Khi người dùng bấm sao chép mã thành viên, em lấy mã đang hiển thị và sao chép để
                    //   họ dùng lại mà không phải gõ. Kết quả sao chép được báo ngay trên trang để người
                    //   dùng biết thao tác có thực hiện được hay không.
                    //
                    // Kết thúc: khi trang báo kết quả sao chép mã thành viên.
                    function copyMemberId(text) {
                        if (navigator.clipboard && window.isSecureContext) {
                            navigator.clipboard.writeText(text).then(() => {
                                showToast('Đã sao chép: ' + text);
                            }).catch(() => {
                                fallbackCopy(text);
                            });
                        } else {
                            fallbackCopy(text);
                        }
                    }

                    function fallbackCopy(text) {
                        const textArea = document.createElement("textarea");
                        textArea.value = text;
                        textArea.style.position = "fixed";
                        textArea.style.opacity = "0";
                        document.body.appendChild(textArea);
                        textArea.focus();
                        textArea.select();
                        try {
                            document.execCommand('copy');
                            showToast('Đã sao chép: ' + text);
                        } catch (err) {
                            showToast('Đã sao chép: ' + text);
                        }
                        document.body.removeChild(textArea);
                    }

                    function togglePassword(element) {
                        const input = element.previousElementSibling;
                        const icon = element.querySelector('.material-symbols-outlined');
                        if (input.type === 'password') {
                            input.type = 'text';
                            icon.textContent = 'visibility';
                            icon.classList.remove('text-muted');
                            icon.classList.add('text-primary-custom');
                        } else {
                            input.type = 'password';
                            icon.textContent = 'visibility_off';
                            icon.classList.add('text-muted');
                            icon.classList.remove('text-primary-custom');
                        }
                    }

                    window.addEventListener('DOMContentLoaded', (event) => {
                        const hash = window.location.hash;
                        if (hash) {

                            const tabId = hash.substring(1) + '-tab';
                            const tabElement = document.getElementById(tabId);
                            if (tabElement) {
                                tabElement.click();

                                setTimeout(() => {
                                    document.querySelector('.profile-nav-bar').scrollIntoView({ behavior: 'smooth', block: 'start' });
                                }, 300);
                            }
                        }
                    });
                </script>

                <div class="toast-container position-fixed bottom-0 end-0 p-3" style="z-index: 1100;">
                    <div id="profileToast" class="toast align-items-center text-bg-dark border-0 shadow" role="alert"
                        aria-live="assertive" aria-atomic="true">
                        <div class="d-flex">
                            <div class="toast-body d-flex align-items-center gap-2">
                                <span class="material-symbols-outlined text-success fs-5">check_circle</span>
                                <span id="profileToastMsg">Đã sao chép!</span>
                            </div>

                            <%-- [HSTB01]
                                 . Khi người dùng bấm X trên thông báo sao chép mã thành viên, em dùng thao
                                   tác đóng thông báo ngay trên trang để ẩn hộp đang hiện. Việc đóng này không
                                   thay đổi mã thành viên hoặc nội dung đã sao chép.

                                 Kết thúc: thông báo sao chép được ẩn trên profile.jsp.
                            --%>
                            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                                aria-label="Close"></button>
                        </div>
                    </div>
                </div>
            </body>

            </html>
