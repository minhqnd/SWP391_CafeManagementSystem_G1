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

                <%-- [HSM04]
                     . Khi ProfileController.renderProfile mở profile.jsp, em dùng tài khoản đang giữ trong session.user
                       cùng lịch sử điểm vừa đọc để hiện hồ sơ. Nhờ vậy, thông tin trên màn hình thuộc đúng người đang
                       đăng nhập.
                     ... Thông tin đã hiện là kết thúc tải hồ sơ. Người dùng bấm Chỉnh sửa thì theo profile.jsp, tìm
                         [HSS01]; chọn lịch sử điểm thì theo profile.jsp, tìm [HSLS01]. Đây là các lần bấm tiếp theo.
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
                                                    <%-- [HSSD01]
                                                         . Số điểm trên profile.jsp lấy từ sessionScope.user.totalPoints. Trước khi dựng trang, authorize
                                                           trong ProfileController.java đã gọi UserDAO.getActiveUserById và cập nhật session.user để phần này
                                                           hiện điểm mới nhất của người đang đăng nhập.
                                                         ... Kết thúc phần hiển thị tại số điểm của người đang đăng nhập trên profile.jsp.
                                                    --%>
                                                    ${sessionScope.user.totalPoints != null ? sessionScope.user.totalPoints : 0}
                                                </h2>
                                                <span class="fs-5 fw-bold text-muted">pts</span>
                                            </div>
                                            <div class="text-muted mt-1" style="font-size: 0.8rem;">
                                                <%-- [HSGIO01]
                                                     . Khi bấm Giỏ hàng tại phần điểm của profile.jsp, em mở /cart để
                                                       CartController.doGet chuẩn bị trang giỏ. Đây là đường sang bước
                                                       đặt hàng để người dùng chọn dùng điểm; chỉ bấm liên kết này chưa
                                                       trừ điểm khỏi tài khoản.
                                                     ... Kết thúc liên kết từ hồ sơ khi trình duyệt mở trang giỏ do
                                                         CartController.doGet chuẩn bị; thao tác dùng điểm tiếp theo thuộc bước đặt
                                                         hàng.
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
                                            <%-- [HSTV02]
                                                 . Mã thành viên trên profile.jsp được ghép từ tiền tố #AURA- và sessionScope.user.id. Mã tài khoản
                                                   này do ProfileController.authorize đọc lại qua UserDAO.getActiveUserById, nên không cần lưu thêm
                                                   một cột mã thành viên riêng.
                                                 ... Kết thúc phần mã thành viên tại chuỗi #AURA- đang hiện trên profile.jsp.
                                            --%>
                                            <span id="memberIdBadge" style="font-family: monospace; font-size: 1rem; background: #f8f9fa; padding: 0.4rem 0.8rem; border-radius: 0.5rem; border: 1px dashed #ced4da; font-weight: 600; color: var(--primary);">#AURA-${sessionScope.user.id != null ? sessionScope.user.id : ''}</span>
                                            <%-- [HSSC01]
                                                 . Khi người dùng bấm biểu tượng sao chép mã thành viên trong profile.jsp, em lấy mã đang hiện ở
                                                   memberIdBadge và truyền cho copyMemberId ngay trong file. Hàm này chép mã vào bộ nhớ tạm để người
                                                   dùng có thể dán sang chỗ khác.
                                                 ... Đọc tiếp ngay trong profile.jsp, tìm [HSSC02] để xem cách sao chép mã và báo kết quả.
                                            --%>
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

                        <%-- [MKL05]
                             . Khi profile.jsp nhận kết quả trở lại, em hiện successMsg hoặc errorMsg để người dùng biết đã đổi
                               mật khẩu được chưa. Nếu có lỗi ở từng ô, AccountFormFeedback.show đưa thông tin cho
                               account_feedback.jsp và account-validation.js hiện lại đúng chỗ cần sửa.
                             ... Thông báo đã hiện là kết thúc đổi mật khẩu. Muốn xem cách hiện lỗi từng ô, đọc
                                 account_feedback.jsp, tìm mã BMTV02. Sửa xong rồi bấm Cập nhật mật khẩu mới gửi lần khác.
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
                                     . Khi người dùng chọn Lịch sử tích điểm trong profile.jsp, em hiện danh sách loyaltyHistory đã được
                                       chuẩn bị khi mở hồ sơ. Trước đó, ProfileController.renderProfile đã gọi
                                       UserDAO.getLoyaltyHistoryByUserId bằng mã người đang đăng nhập để chỉ đọc lịch sử của chính người
                                       đó.
                                     ... Để xem dữ liệu bảng đã được chuẩn bị từ đâu, đọc UserDAO.java, tìm [HSLS02].
                                --%>
                                <button class="nav-link profile-nav-item w-100 justify-content-center" id="history-tab"
                                    data-bs-toggle="tab" data-bs-target="#history" type="button" role="tab"><span
                                        class="material-symbols-outlined fs-5">history</span> Lịch sử tích điểm</button>
                            </li>
                            <li class="nav-item flex-fill text-center" role="presentation">
                                <%-- [HSTAB01]
                                     . Khi bấm Thông tin tài khoản, Bootstrap hiện vùng account và ẩn tab lịch sử. Hồ sơ đã được
                                       ProfileController.renderProfile chuẩn bị cho trang này; đổi tab không gửi form hay ghi lại thông
                                       tin.
                                     ... Kết thúc đổi tab khi vùng Thông tin tài khoản đã hiện và vùng lịch sử đã được ẩn.
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
                                                     . Ảnh đại diện trong profile.jsp đang lấy từ đường dẫn cố định ở thẻ img. Biểu tượng máy ảnh hiện
                                                       mới có giao diện, chưa có phần chọn tệp và lưu ảnh cho tài khoản. Vì vậy ở nút này chưa có luồng
                                                       đổi ảnh để chạy tiếp.
                                                     ... Kết thúc phần giải thích tại đây: nút máy ảnh hiện chưa có đoạn chọn và lưu ảnh để đọc tiếp.
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
                                                     . Em bắt đầu từ nút Chỉnh sửa trong profile.jsp. Nút có id là btnEditProfile và được lấy vào biến
                                                       btnEdit trong đoạn script của cùng file. Khi người dùng bấm, phần xử lý của nút giữ thông tin cũ,
                                                       mở các ô và hiện Lưu thay đổi cùng Hủy để người dùng sửa hồ sơ.
                                                     ... Đọc tiếp ngay trong profile.jsp, tìm [HSS02] để xem cách mở khóa form và giữ giá trị trước khi
                                                         sửa.
                                                --%>
                                                <button type="button" id="btnEditProfile"
                                                    class="btn-outline-primary-custom d-flex align-items-center gap-2">
                                                    <span class="material-symbols-outlined fs-5">edit</span> Chỉnh sửa
                                                </button>
                                            </div>

                                            <%-- [HSL07]
                                                 . Trang profile.jsp mở lại thì em lấy thông tin mới từ session.user để điền các ô. Người dùng thấy
                                                   hồ sơ vừa lưu cùng thông báo kết quả. Nếu lần lưu có lỗi, account-validation.js điền lại phần đang
                                                   nhập và hiện lời báo tại ô cần sửa.
                                                 ... Hồ sơ và kết quả đã hiện là kết thúc cập nhật. Muốn xem cách điền lại ô sau lỗi, đọc
                                                     account_feedback.jsp, tìm mã BMTV02; muốn sửa tiếp thì người dùng bấm Chỉnh sửa.
                                            --%>
                                            <form id="profileForm"
                                                action="${pageContext.request.contextPath}/profile?action=update"
                                                method="POST">
                                                <input type="hidden" name="csrfToken" value="${sessionScope.accountCsrfToken}">
                                                <div class="row g-4">
                                                    <div class="col-md-6">
                                                        <label class="form-label-custom">Họ và tên</label>

                                                        <%-- [HSHT01]
                                                             . Ô Họ tên trong profile.jsp gửi giá trị qua fullName. validate trong account-validation.js kiểm tra
                                                               trước; khi bấm Lưu thay đổi, ProfileController.doPost gọi update rồi AccountValidation.nameError
                                                               để kiểm tra lại tên trước khi cập nhật hồ sơ.
                                                             ... Đọc tiếp tại ProfileController.java, tìm [HSHT02] để xem cách nhận họ tên trước khi kiểm tra và
                                                                 lưu.
                                                        --%>
                                                        <input type="text" name="fullName"
                                                            class="form-control-custom w-100 profile-input"
                                                            value="${fn:escapeXml(sessionScope.user.fullName)}" minlength="1" readonly
                                                            required>
                                                    </div>
                                                    <div class="col-md-6">
                                                        <label class="form-label-custom">Số điện thoại</label>

                                                        <%-- [HSDT01]
                                                             . Ô Số điện thoại trong profile.jsp gửi phone cùng form tới ProfileController.doPost. Controller gọi
                                                               update để chuẩn hóa và dùng AccountValidation.phoneError kiểm tra định dạng, rồi chuyển số hợp lệ
                                                               cho UserDAO.updateUserProfile để tìm trùng và lưu.
                                                             ... Đọc tiếp tại ProfileController.java, tìm [HSDT02] để xem cách nhận và chuẩn hóa số điện thoại.
                                                        --%>
                                                        <input type="tel" name="phone"
                                                            class="form-control-custom w-100 profile-input"
                                                            value="${fn:escapeXml(sessionScope.user.phone)}"
                                                            pattern="0[0-9]{9}" inputmode="numeric"
                                                            title="Nhập 10 chữ số và bắt đầu bằng số 0"
                                                            readonly required>
                                                    </div>
                                                    <div class="col-md-6">
                                                        <label class="form-label-custom">Địa chỉ Email</label>

                                                        <%-- [HSEM01]
                                                             . Khi người dùng sửa Email trong profile.jsp, em dùng validate của account-validation.js kiểm tra
                                                               định dạng, rồi dùng checkContact tìm email đã có người dùng. Đúng thì form gửi email tới
                                                               ProfileController.doPost. Tại đây, update gọi AccountValidation.emailError để kiểm tra lại rồi đưa
                                                               email cho UserDAO.updateUserProfile; hàm này còn kiểm tra trùng với tài khoản khác trước khi lưu.
                                                             ... Đọc tiếp tại ProfileController.java, tìm [HSEM02] để xem cách nhận email trước khi kiểm tra và
                                                                 lưu.
                                                        --%>
                                                        <input type="email" name="email" maxlength="150"
                                                            class="form-control-custom w-100 profile-input"
                                                            value="${fn:escapeXml(sessionScope.user.email)}" readonly required>
                                                    </div>
                                                    <div class="col-md-6">
                                                        <label class="form-label-custom">Ngày sinh nhật</label>
                                                        <div class="d-flex gap-2">

                                                            <%-- [HSNS01]
                                                                 . Khi người dùng đổi ngày, tháng hoặc năm trên Hồ sơ, em dùng updateDays để tạo các ngày phù hợp với
                                                                   tháng và năm đã chọn. Sau đó, syncBirthday ghép lựa chọn thành ngày sinh sẽ gửi. Khi bấm Lưu,
                                                                   ProfileController.update nhận các ô này và kiểm tra lại trước khi ghi hồ sơ.
                                                                 .. Người dùng được để trống cả ba ô; nếu chọn thì phải đủ và là ngày có thật từ năm 1900 đến hôm
                                                                    nay.
                                                                 ... Đọc tiếp ngay trong profile.jsp, tìm [HSNS02] để xem cách tạo các ngày hợp lệ theo tháng và năm.
                                                            --%>
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

                                                        <%-- [HSDC01]
                                                             . Ô Địa chỉ trong profile.jsp gửi giá trị qua address và được validate trong account-validation.js
                                                               kiểm tra trước. Khi bấm Lưu thay đổi, ProfileController.doPost gọi update để nhận địa chỉ, kiểm
                                                               tra lại rồi chuyển cho UserDAO.updateUserProfile ghi cùng hồ sơ.
                                                             .. Địa chỉ được phép bỏ trống; nếu nhập thì tối đa 500 ký tự và không có ký tự điều khiển.
                                                             ... Đọc tiếp tại account-validation.js, tìm [HSDC02].
                                                        --%>
                                                        <input type="text" name="address" maxlength="500"
                                                            class="form-control-custom w-100 profile-input"
                                                            value="${fn:escapeXml(sessionScope.user.address)}" readonly>
                                                    </div>
                                                </div>

                                                <div id="editActions"
                                                    class="mt-4 pt-3 border-top d-none justify-content-end gap-2">
                                                    <%-- [HSH01]
                                                         . Nếu người dùng muốn bỏ phần vừa sửa, họ bấm Hủy trong profile.jsp. Nút này chạy phần xử lý
                                                           btnCancel trong cùng file để trả các ô về thông tin trước khi sửa, rồi khóa form lại.
                                                         ... Đọc tiếp ngay trong profile.jsp, tìm [HSH02] để xem cách khôi phục các ô trước lúc sửa.
                                                    --%>
                                                    <button type="button" id="btnCancelEdit"
                                                        class="btn btn-light fw-bold px-4">Hủy</button>

                                                    <%-- [HSL01]
                                                         . Sau khi sửa thông tin, người dùng bấm Lưu thay đổi trong profile.jsp. Em dùng
                                                           account-validation.js để kiểm tra các ô trước; đúng hết thì form gửi họ tên, số điện thoại, email,
                                                           ngày sinh và địa chỉ tới /profile?action=update. Đường dẫn này được ProfileController.doPost nhận
                                                           để kiểm tra lại và cập nhật đúng tài khoản đang dùng.
                                                         ... Đọc tiếp tại ProfileController.java, tìm [HSL02].
                                                    --%>
                                                    <button type="submit" id="btnSaveProfile"
                                                        class="btn btn-secondary px-4 fw-bold" disabled>Lưu thay
                                                        đổi</button>
                                                </div>
                                            </form>

                                            <hr class="my-5 border-secondary-subtle">

                                            <%-- [HSMK01]
                                                 . Khi người dùng bấm Bảo mật & Đổi mật khẩu trong profile.jsp, em dùng phần mở/thu của Bootstrap để
                                                   hiện hoặc ẩn ba ô mật khẩu trong passwordCollapse. Người dùng nhập xong rồi bấm Cập nhật mật khẩu
                                                   mới bắt đầu yêu cầu đổi.
                                                 ... Mở vùng mật khẩu xong thì chờ người dùng nhập. Khi bấm Cập nhật mật khẩu, theo profile.jsp, tìm
                                                     [MKL01].
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


                                                <%-- [HSMAT01]
                                                     . Ba nút mắt của form đổi mật khẩu dùng xử lý chung trong
                                                       account-validation.js. Khi trang mở, mã này bỏ nút gọi togglePassword
                                                       cũ rồi tạo nút cho từng ô; bấm chỉ đổi che hoặc hiện mật khẩu, không
                                                       gửi form hay lưu mật khẩu.
                                                     ... Đọc tiếp tại account-validation.js, tìm [BMMAT01].
                                                --%>
                                                <form id="passwordForm"
                                                    action="${pageContext.request.contextPath}/profile?action=changePassword"
                                                    method="POST">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope.accountCsrfToken}">
                                                    <div class="row g-4">
                                                        <div class="col-12">
                                                            <label class="form-label-custom">Mật khẩu hiện tại</label>
                                                            <div class="input-group">

                                                                <%-- [MKCU01]
                                                                     . Ô Mật khẩu hiện tại trong profile.jsp gửi giá trị qua oldPassword khi bấm đổi mật khẩu.
                                                                       ProfileController.doPost truyền mật khẩu này cùng mã tài khoản cho UserDAO.changePasswordById để
                                                                       đối chiếu; chỉ đúng mật khẩu cũ mới được đổi sang mật khẩu mới.
                                                                     ... Khi bấm Cập nhật mật khẩu, theo ProfileController.java, tìm mã MKL02 để xem doPost gọi
                                                                         UserDAO.changePasswordById, truyền mật khẩu hiện tại để kiểm tra trước khi đổi.
                                                                --%>
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

                                                                <%-- [MKMOI01]
                                                                     . Ô Mật khẩu mới trong profile.jsp được passwordError của account-validation.js kiểm tra trước khi
                                                                       gửi. ProfileController.doPost kiểm tra lại rồi truyền newPassword cho UserDAO.changePasswordById
                                                                       để băm và ghi sau khi đã đối chiếu đúng mật khẩu hiện tại.
                                                                     ... Khi bấm Cập nhật mật khẩu, theo ProfileController.java, tìm mã MKL02; doPost kiểm tra hai ô mật
                                                                         khẩu mới rồi gọi UserDAO.changePasswordById nếu không còn lỗi.
                                                                --%>
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

                                                                <%-- [MKXN01]
                                                                     . Ô Xác nhận mật khẩu trong profile.jsp gửi qua confirmPassword và được validate trong
                                                                       account-validation.js so với newPassword. Khi gửi đổi mật khẩu, ProfileController.doPost còn gọi
                                                                       AccountValidation.passwordErrors để kiểm tra hai ô khớp trước khi lưu.
                                                                     ... Khi bấm Cập nhật mật khẩu, theo ProfileController.java, tìm mã MKL02; doPost gọi passwordErrors
                                                                         để kiểm tra ô xác nhận trước khi đổi.
                                                                --%>
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

                                                        <%-- [MKL01]
                                                             . Em bắt đầu từ nút Cập nhật mật khẩu trong profile.jsp. Sau khi kiểm tra các ô trên trang, form gửi
                                                               mật khẩu hiện tại, mật khẩu mới và xác nhận tới /profile?action=changePassword.
                                                               ProfileController.doPost nhận đường dẫn này để kiểm tra rồi đổi mật khẩu cho người đang đăng nhập.
                                                             ... Đọc tiếp tại ProfileController.java, tìm [MKL02].
                                                        --%>
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
                                                     . Mỗi dòng dưới đây lấy một giao dịch trong loyaltyHistory do UserDAO.getLoyaltyHistoryByUserId đọc
                                                       cho người đang đăng nhập. Em dùng createdAt để hiện thời gian, pointsChange để hiện số điểm và màu
                                                       cộng/trừ, id để hiện mã giao dịch. Nhãn hiện tại dựa vào transactionType và orderId: EARN là tích
                                                       điểm; loại khác có mã đơn thì hiện dùng điểm cho đơn, không có mã đơn thì hiện đổi ưu đãi.
                                                     ... Bảng giao dịch của chính tài khoản đã hiện là kết thúc xem lịch sử. Người dùng chọn Thông tin
                                                         tài khoản để quay lại thì theo profile.jsp, tìm [HSTAB01].
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

                        // [HSTD01]
                        // . Hàm checkFormChanged so sánh các giá trị đang nhập với originalValues đã giữ trước lúc sửa. Có
                        //   thay đổi thì em bật nút Lưu; các giá trị vẫn như cũ thì em tắt nút. Mỗi lần người dùng nhập hoặc
                        //   đổi lựa chọn, phần này lại kiểm tra để cập nhật nút Lưu.
                        // ... Kiểm tra xong thì nút Lưu được bật hoặc tắt, chưa gửi hồ sơ đi. Khi người dùng bấm Lưu thay đổi,
                        //     theo profile.jsp, tìm [HSL01].
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

                        // [HSNS02]
                        // . Hàm updateDays lấy tháng và năm đang chọn để tạo lại danh sách ngày, có xét cả năm nhuận. Nếu ngày
                        //   cũ không có trong tháng mới thì em bỏ lựa chọn đó. Sau bước này, phần xử lý đổi lựa chọn gọi
                        //   syncBirthday để ghép ngày sinh sẽ gửi.
                        // ... Sau updateDays, sự kiện đổi tháng hoặc năm gọi syncBirthday để ghép ngày sẽ gửi. Đọc tiếp tại
                        //     profile.jsp, tìm [HSNS03].
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

                        // [HSNS03]
                        // . Hàm syncBirthday đọc ba ô ngày, tháng và năm rồi điền ngày đã ghép vào birthdayHidden. Đây là giá
                        //   trị đang chuẩn bị trên form. Khi người dùng bấm Lưu thay đổi, ProfileController.update còn nhận
                        //   các ô ngày sinh để ghép và kiểm tra lại.
                        // ... Ghép xong birthdayHidden thì chưa lưu. Khi bấm Lưu thay đổi, ngày sinh được kiểm tra lại tại
                        //     ProfileController.java, tìm [HSNS04].
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
                        // . Khi nút Chỉnh sửa được bấm, em giữ lại thông tin cũ trong originalValues để có thể khôi phục nếu
                        //   người dùng chọn Hủy. Tiếp theo, em mở khóa các ô, hiện nút Lưu thay đổi và Hủy, đồng thời ẩn nút
                        //   Chỉnh sửa. Sau đó, em gọi checkFormChanged ngay trong profile.jsp để kiểm tra có thông tin nào
                        //   thay đổi chưa.
                        // ... Mở khóa xong, em gọi checkFormChanged để quyết định bật nút Lưu. Đọc tiếp tại profile.jsp, tìm
                        //     [HSTD01].
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
                        // . Đến phần xử lý nút Hủy, em lấy thông tin trong originalValues để điền lại các ô và ngày sinh, đồng
                        //   thời bỏ lỗi đang hiện và khóa form. Vì các ô vừa sửa chưa được gửi đi, hồ sơ đã lưu vẫn được giữ
                        //   nguyên.
                        // ... Kết thúc Hủy khi các ô trở về giá trị cũ và form đã khóa. Muốn sửa lại thì bấm Chỉnh sửa, theo
                        //     profile.jsp, tìm mã HSS01.
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

                    // [HSSC02]
                    // . Trong copyMemberId, em gọi navigator.clipboard.writeText để chép mã thành viên. Nếu trình duyệt
                    //   không dùng được cách này, em gọi fallbackCopy trong cùng file để thử cách dự phòng. Sau đó,
                    //   showToast hiện thông báo cho người dùng biết đã chép được hay chưa.
                    // ... Chép xong bằng clipboard hoặc fallbackCopy, em gọi showToast để báo kết quả. Đọc phần thông báo
                    //     và nút đóng tại profile.jsp, tìm [HSTB01]; tới đó là kết thúc sao chép.
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
                                 . Sau khi sao chép mã thành viên, showToast trong profile.jsp hiện thông báo ngắn.
                                   Bấm X thì Bootstrap dùng data-bs-dismiss="toast" để ẩn thông báo ngay trên trang;
                                   nút không gửi form, không đổi hồ sơ hay xóa mã vừa sao chép.
                                 ... Kết thúc đóng thông báo khi profileToast đã ẩn trên profile.jsp.
                            --%>
                            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                                aria-label="Close"></button>
                        </div>
                    </div>
                </div>
            </body>

            </html>
