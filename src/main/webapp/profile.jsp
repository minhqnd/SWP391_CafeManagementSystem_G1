<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<c:if test="${empty sessionScope.user}">
    <c:redirect url="/?login=true" />
</c:if>
<c:if test="${empty currentDisplayTier}">
    <c:redirect url="/profile" />
</c:if>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ & Điểm tích luỹ - Aura Café</title>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <!-- Material Symbols -->
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    
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

        .text-primary-custom { color: var(--primary) !important; }
        .bg-primary-custom { background-color: var(--primary) !important; }

        /* Loyalty Card */
        .loyalty-card {
            background: linear-gradient(135deg, #900b1a 0%, #4a040b 100%);
            border-radius: 1rem;
            color: white;
            padding: 2rem;
            position: relative;
            overflow: hidden;
            box-shadow: 0 10px 20px rgba(144, 11, 26, 0.15);
            height: 100%;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
        }
        .loyalty-card::after {
            content: '';
            position: absolute;
            top: -50%; right: -20%;
            width: 400px; height: 400px;
            background: radial-gradient(circle, rgba(255,215,0,0.1) 0%, rgba(255,255,255,0) 70%);
            border-radius: 50%;
            pointer-events: none;
        }
        .tier-badge-gold {
            background-color: var(--gold);
            color: #4a040b;
            font-weight: 800;
            padding: 0.4rem 1rem;
            border-radius: 2rem;
            font-size: 0.8rem;
            display: inline-flex;
            align-items: center;
            gap: 0.3rem;
            box-shadow: 0 4px 10px rgba(255, 215, 0, 0.3);
        }
        .tier-badge-silver {
            background-color: #e2e8f0;
            color: #334155;
            box-shadow: 0 4px 10px rgba(0,0,0,0.15);
        }
        .tier-badge-diamond {
            background-color: #f3e5f5;
            color: #7b1fa2;
            box-shadow: 0 4px 10px rgba(156, 39, 176, 0.3);
        }

        .progress-bar-custom {
            height: 6px;
            background-color: rgba(255,255,255,0.2);
            border-radius: 3px;
            overflow: hidden;
            margin: 0.75rem 0;
        }
        .progress-fill {
            height: 100%;
            background-color: var(--gold);
            width: 56%; /* 450/800 */
            border-radius: 3px;
        }

        /* Barcode Card */
        .barcode-card {
            background: white;
            border-radius: 1rem;
            padding: 1.5rem;
            box-shadow: 0 4px 15px rgba(0,0,0,0.04);
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

        /* Profile Menu Bar */
        .profile-nav-bar {
            background: white;
            border-radius: 0.75rem;
            padding: 0.5rem;
            box-shadow: 0 2px 10px rgba(0,0,0,0.03);
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

        /* Info Cards */
        .info-card {
            background: white;
            border-radius: 1rem;
            padding: 2rem;
            box-shadow: 0 4px 15px rgba(0,0,0,0.04);
            border: 1px solid #e9ecef;
            height: 100%;
        }

        /* Avatar Section */
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
            box-shadow: 0 4px 15px rgba(0,0,0,0.1);
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
            box-shadow: 0 2px 5px rgba(0,0,0,0.2);
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

        /* Form styling */
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
    </style>
</head>
<body>

    <!-- Header Component -->
    <jsp:include page="/components/header.jsp">
        <jsp:param name="activePage" value="profile" />
    </jsp:include>

    <main class="py-5">
        <div class="container">
            <!-- Top Section: Loyalty & Barcode -->
            <div class="row g-4 mb-4">
                <!-- Loyalty Card -->
                <div class="col-lg-8">
                    <div class="loyalty-card">
                        <div class="d-flex justify-content-between align-items-start mb-4">
                            <div class="d-flex align-items-center gap-3">
                                <div class="rounded-circle bg-white bg-opacity-10 d-flex align-items-center justify-content-center border border-white border-opacity-25" style="width: 48px; height: 48px;">
                                    <span class="material-symbols-outlined text-white fs-4">workspace_premium</span>
                                </div>
                                <div>
                                    <h3 class="fs-5 fw-bold mb-0 text-uppercase" style="letter-spacing: 1px;">AURA PRIVILEGE CLUB</h3>
                                    <span class="text-white-50" style="font-size: 0.85rem;">Thành viên thân thiết cấp cao</span>
                                </div>
                            </div>
                            <div class="tier-badge-gold ${currentDisplayTier == 'SILVER' ? 'tier-badge-silver' : (currentDisplayTier == 'DIAMOND' ? 'tier-badge-diamond' : '')}">
                                <span class="material-symbols-outlined fs-6">stars</span> HẠNG ${currentDisplayTier != null ? currentDisplayTier : 'MEMBER'}
                            </div>
                        </div>

                        <div class="d-flex justify-content-between align-items-end mb-4">
                            <div>
                                <div class="text-white-50 mb-1" style="font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1px;">Chủ thẻ</div>
                                <h2 class="fs-3 fw-bold mb-1">${sessionScope.user.fullName}</h2>
                                <div class="d-flex align-items-center gap-2 text-white-50">
                                    <span style="font-size: 0.9rem; font-family: monospace;">#AURA-${sessionScope.user.id != null ? sessionScope.user.id + 9900 : 9981}</span>
                                    <span class="material-symbols-outlined fs-6" style="cursor: pointer;" title="Sao chép mã" onclick="copyMemberId('#AURA-${sessionScope.user.id != null ? sessionScope.user.id + 9900 : 9981}')">content_copy</span>
                                </div>
                            </div>
                            <div class="text-end bg-white bg-opacity-10 p-3 rounded-3 border border-white border-opacity-25">
                                <div class="text-white-50 mb-1" style="font-size: 0.75rem; text-transform: uppercase;">Điểm tích luỹ Aura</div>
                                <div class="fs-2 fw-bold text-white lh-1 mb-1">${sessionScope.user.totalPoints != null ? sessionScope.user.totalPoints : 0} <span class="fs-6 fw-normal">pts</span></div>
                                <div class="text-white-50" style="font-size: 0.7rem;">Dùng điểm để đổi voucher giảm giá</div>
                            </div>
                        </div>

                        <div>
                            <div class="d-flex justify-content-between align-items-end mb-1 text-white" style="font-size: 0.85rem;">
                                <c:choose>
                                    <c:when test="${nextTierName == 'VIP'}">
                                        <span>Bạn đã đạt hạng <strong>Kim Cương</strong> tối đa</span>
                                        <span class="fw-bold">${sessionScope.user.totalPoints != null ? sessionScope.user.totalPoints : 0}</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span>Cần <strong style="color: var(--gold);">${pointsNeeded != null ? pointsNeeded : 0} điểm</strong> nữa để thăng hạng <strong>${nextTierName != null ? nextTierName : 'Vàng'}</strong></span>
                                        <span class="fw-bold">${sessionScope.user.totalPoints != null ? sessionScope.user.totalPoints : 0} / ${nextPoints != null ? nextPoints : 300}</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                            <c:set var="fillPct" value="${empty progressPercent ? 0 : progressPercent}" />
                            <div class="progress-bar-custom">
                                <div class="progress-fill" style="width: ${fillPct}%;"></div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Barcode Card -->
                <div class="col-lg-4">
                    <div class="barcode-card justify-content-between">
                        <div>
                            <div class="d-flex align-items-center justify-content-between mb-3">
                                <h3 class="fs-6 fw-bold mb-0 text-dark d-flex align-items-center gap-2">
                                    <span class="material-symbols-outlined text-danger" style="font-size: 20px;">qr_code_2</span>
                                    Mã tích điểm tại quầy
                                </h3>
                                <span class="badge bg-success-subtle text-success border border-success-subtle fw-semibold rounded-pill" style="font-size: 0.65rem;">Tự động làm mới</span>
                            </div>
                            <div class="barcode-box">
                                <div class="bg-white py-2 px-3 rounded shadow-sm d-flex justify-content-center align-items-center">
                                    <div class="d-flex align-items-center justify-content-center gap-1 w-100" style="height: 52px; max-width: 220px;">
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 3px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 1px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 5px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 2px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 4px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 6px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 2px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 3px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 5px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 1px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 4px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 2px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 3px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 5px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 2px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 4px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 6px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 3px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 2px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 5px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 1px;"></span>
                                        <span class="d-inline-block bg-dark h-100 rounded-1" style="width: 4px;"></span>
                                    </div>
                                </div>
                                <p class="font-monospace text-dark fw-bold mt-2 mb-0" style="font-size: 0.85rem; letter-spacing: 0.2em;">8934567${sessionScope.user.id != null ? sessionScope.user.id + 9900 : 9981}</p>
                            </div>
                        </div>
                        <p class="text-muted small text-center mb-0" style="font-size: 0.72rem; line-height: 1.4;">
                            Đưa mã này cho thu ngân tại quầy thanh toán để tích điểm hoặc áp dụng ưu đãi hội viên.
                        </p>
                    </div>
                </div>
            </div>

            <c:if test="${not empty successMsg}">
                <div class="alert alert-success fw-bold border-0 py-3 mb-4 shadow-sm d-flex align-items-center gap-2">
                    <span class="material-symbols-outlined">check_circle</span>
                    ${successMsg}
                </div>
            </c:if>
            <c:if test="${not empty errorMsg}">
                <div class="alert alert-danger fw-bold border-0 py-3 mb-4 shadow-sm d-flex align-items-center gap-2">
                    <span class="material-symbols-outlined">error</span>
                    ${errorMsg}
                </div>
            </c:if>

            <!-- Menu Bar -->
            <ul class="nav profile-nav-bar mb-4" id="profileTab" role="tablist">
                <li class="nav-item flex-fill text-center" role="presentation">
                    <button class="nav-link profile-nav-item w-100 justify-content-center" id="rewards-tab" data-bs-toggle="tab" data-bs-target="#rewards" type="button" role="tab"><span class="material-symbols-outlined fs-5">redeem</span> Đổi quà & Ưu đãi</button>
                </li>
                <li class="nav-item flex-fill text-center" role="presentation">
                    <button class="nav-link profile-nav-item w-100 justify-content-center" id="history-tab" data-bs-toggle="tab" data-bs-target="#history" type="button" role="tab"><span class="material-symbols-outlined fs-5">history</span> Lịch sử tích điểm</button>
                </li>
                <li class="nav-item flex-fill text-center" role="presentation">
                    <button class="nav-link profile-nav-item w-100 justify-content-center" id="privileges-tab" data-bs-toggle="tab" data-bs-target="#privileges" type="button" role="tab"><span class="material-symbols-outlined fs-5">military_tech</span> Đặc quyền thứ hạng</button>
                </li>
                <li class="nav-item flex-fill text-center" role="presentation">
                    <button class="nav-link profile-nav-item active w-100 justify-content-center" id="account-tab" data-bs-toggle="tab" data-bs-target="#account" type="button" role="tab"><span class="material-symbols-outlined fs-5">manage_accounts</span> Thông tin tài khoản</button>
                </li>
            </ul>

            <div class="tab-content" id="profileTabContent">
                <!-- Tab: Account Info -->
                <div class="tab-pane fade show active" id="account" role="tabpanel" tabindex="0">
                    <div class="row g-4">
                <!-- Left: Avatar & Status -->
                <div class="col-lg-4">
                    <div class="info-card text-center d-flex flex-column">
                        <div class="avatar-wrapper mt-4">
                            <img src="https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=200" alt="Avatar" class="avatar-img">
                            <div class="avatar-edit-btn">
                                <span class="material-symbols-outlined" style="font-size: 1.1rem;">photo_camera</span>
                            </div>
                        </div>
                        
                        <h2 class="fs-4 fw-bold mb-1">${sessionScope.user.fullName}</h2>
                        <p class="text-muted mb-3" style="font-size: 0.85rem; font-family: monospace;">#AURA-${sessionScope.user.id != null ? sessionScope.user.id + 9900 : 9981}</p>
                        
                        <div class="mb-4">
                            <span class="badge-member-since">Thành viên Aura từ Tháng 11/2022</span>
                        </div>

                        <hr class="w-100 border-secondary-subtle my-auto">

                        <div class="w-100 text-start mt-4">
                            <div class="d-flex justify-content-between align-items-center mb-3">
                                <span class="text-muted" style="font-size: 0.85rem;">Trạng thái tài khoản:</span>
                                <span class="text-success fw-bold d-flex align-items-center gap-1" style="font-size: 0.85rem;">
                                    <span class="material-symbols-outlined fs-6">check_circle</span> Đã xác thực
                                </span>
                            </div>
                            <div class="d-flex justify-content-between align-items-center">
                                <span class="text-muted" style="font-size: 0.85rem;">Xác thực 2 yếu tố (2FA):</span>
                                <span class="text-dark fw-bold" style="font-size: 0.85rem;">Bật (SMS OTP)</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Right: Form Info -->
                <div class="col-lg-8">
                    <div class="info-card">
                        <div class="d-flex justify-content-between align-items-start mb-4 pb-3 border-bottom">
                            <div>
                                <h2 class="fs-5 fw-bold mb-1">Thông tin cá nhân & Địa chỉ</h2>
                                <p class="text-muted mb-0" style="font-size: 0.85rem;">Quản lý dữ liệu để nhận quà sinh nhật và giao hàng nhanh</p>
                            </div>
                            <button type="button" id="btnEditProfile" class="btn-outline-primary-custom d-flex align-items-center gap-2">
                                <span class="material-symbols-outlined fs-5">edit</span> Chỉnh sửa
                            </button>
                        </div>

                        <form id="profileForm" action="${pageContext.request.contextPath}/profile?action=update" method="POST">
                            <div class="row g-4">
                                <div class="col-md-6">
                                    <label class="form-label-custom">Họ và tên</label>
                                    <input type="text" name="fullName" class="form-control-custom w-100 profile-input" value="${sessionScope.user.fullName}" minlength="2" readonly required>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label-custom">Số điện thoại</label>
                                    <input type="tel" name="phone" class="form-control-custom w-100 profile-input" value="${sessionScope.user.phone}" pattern="^(84|0)[3|5|7|8|9][0-9]{8}$" title="Vui lòng nhập số điện thoại Việt Nam hợp lệ (VD: 0912345678)" readonly required>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label-custom">Địa chỉ Email</label>
                                    <input type="email" name="email" class="form-control-custom w-100 profile-input" value="${sessionScope.user.email}" readonly required>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label-custom">Ngày sinh nhật</label>
                                    <input type="date" name="birthday" class="form-control-custom w-100 profile-input" value="${sessionScope.user.birthday}" readonly>
                                </div>
                                <div class="col-12">
                                    <label class="form-label-custom">Địa chỉ nhận hàng mặc định</label>
                                    <input type="text" name="address" class="form-control-custom w-100 profile-input" value="${sessionScope.user.address}" readonly>
                                </div>
                            </div>
                            
                            <div id="editActions" class="mt-4 pt-3 border-top d-none justify-content-end gap-2">
                                <button type="button" id="btnCancelEdit" class="btn btn-light fw-bold px-4">Hủy</button>
                                <button type="submit" id="btnSaveProfile" class="btn btn-secondary px-4 fw-bold" disabled>Lưu thay đổi</button>
                            </div>
                        </form>

                        <hr class="my-5 border-secondary-subtle">

                        <!-- Change Password Section -->
                        <div class="d-flex justify-content-between align-items-start mb-4 pb-3 border-bottom">
                            <div>
                                <h3 class="fs-5 fw-bold mb-1">Bảo mật & Đổi mật khẩu</h3>
                                <p class="text-muted mb-0" style="font-size: 0.85rem;">Bảo vệ tài khoản của bạn bằng mật khẩu an toàn</p>
                            </div>
                            <span class="material-symbols-outlined text-muted fs-4">lock</span>
                        </div>

                        <form id="passwordForm" action="${pageContext.request.contextPath}/profile?action=changePassword" method="POST">
                            <div class="row g-4">
                                <div class="col-12">
                                    <label class="form-label-custom">Mật khẩu hiện tại</label>
                                    <div class="input-group">
                                        <input type="password" name="oldPassword" class="form-control form-control-custom bg-white border-end-0" placeholder="Nhập mật khẩu hiện tại" required>
                                        <span class="input-group-text bg-white border-start-0 cursor-pointer" onclick="togglePassword(this)" style="cursor: pointer; border-color: #e9ecef;">
                                            <span class="material-symbols-outlined fs-5 text-muted">visibility_off</span>
                                        </span>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label-custom">Mật khẩu mới</label>
                                    <div class="input-group">
                                        <input type="password" id="newPassword" name="newPassword" class="form-control form-control-custom bg-white border-end-0" placeholder="Nhập mật khẩu mới" minlength="6" required>
                                        <span class="input-group-text bg-white border-start-0 cursor-pointer" onclick="togglePassword(this)" style="cursor: pointer; border-color: #e9ecef;">
                                            <span class="material-symbols-outlined fs-5 text-muted">visibility_off</span>
                                        </span>
                                    </div>
                                    <div class="invalid-feedback" id="newPasswordFeedback">Mật khẩu phải từ 6 ký tự trở lên.</div>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label-custom">Xác nhận mật khẩu mới</label>
                                    <div class="input-group">
                                        <input type="password" id="confirmPassword" name="confirmPassword" class="form-control form-control-custom bg-white border-end-0" placeholder="Nhập lại mật khẩu mới" required>
                                        <span class="input-group-text bg-white border-start-0 cursor-pointer" onclick="togglePassword(this)" style="cursor: pointer; border-color: #e9ecef;">
                                            <span class="material-symbols-outlined fs-5 text-muted">visibility_off</span>
                                        </span>
                                    </div>
                                    <div class="invalid-feedback" id="confirmPasswordFeedback">Mật khẩu xác nhận không khớp.</div>
                                </div>
                            </div>
                            
                            <div class="mt-4 pt-3 border-top d-flex justify-content-end">
                                <button type="submit" class="btn btn-outline-primary-custom px-4 fw-bold">Cập nhật mật khẩu</button>
                            </div>
                        </form>

                    </div>
                </div>
                </div>
                </div>
                
                <!-- Tab: Rewards -->
                <div class="tab-pane fade" id="rewards" role="tabpanel" tabindex="0">
                    <!-- Aura Loyalty Matrix Banner -->
                    <div class="info-card mb-4 bg-dark text-white rounded-4 overflow-hidden position-relative" style="border: 1px solid rgba(255,255,255,0.1);">
                        <div class="position-absolute top-0 end-0 h-100 w-50" style="background: linear-gradient(90deg, transparent, rgba(255,193,7,0.1) 50%, rgba(255,193,7,0.2));"></div>
                        <div class="d-flex justify-content-between align-items-center position-relative z-1 p-2">
                            <div>
                                <h3 class="fs-4 fw-bold mb-2 text-warning d-flex align-items-center gap-2"><span class="material-symbols-outlined">auto_awesome</span> Aura Loyalty Matrix</h3>
                                <p class="mb-2 fw-semibold fs-6">Ưu đãi điểm thưởng tích luỹ Member</p>
                                <p class="mb-0 text-white-50 small" style="line-height: 1.6;">Mỗi 10.000đ chi tiêu tương đương 1 Điểm Aura (A-Point).<br>Nâng cấp hạng để nhận gấp bội đặc quyền.</p>
                            </div>
                            <button class="btn btn-warning fw-bold rounded-pill px-4 d-flex align-items-center gap-2 shadow-sm" onclick="document.getElementById('privileges-tab').click();">
                                Xem chi tiết hạng <span class="material-symbols-outlined fs-6">arrow_forward</span>
                            </button>
                        </div>
                    </div>

                    <div class="info-card">
                        <div class="d-flex justify-content-between align-items-end mb-4 pb-3 border-bottom">
                            <div>
                                <h3 class="fs-5 fw-bold mb-1">Gian hàng đổi điểm Aura Rewards</h3>
                                <p class="text-muted mb-0" style="font-size: 0.85rem;">Đổi điểm tích luỹ lấy voucher giảm giá và quà tặng thương hiệu</p>
                            </div>
                            <div class="text-muted d-flex align-items-center gap-2" style="font-size: 0.85rem;">
                                Số điểm hiện có: <span class="badge bg-danger-subtle text-danger fw-bold fs-6 px-3 py-2 border border-danger-subtle rounded-pill">${sessionScope.user.totalPoints} pts</span>
                            </div>
                        </div>

                        <div class="row g-4">
                            <c:forEach var="reward" items="${rewardsList}">
                                <div class="col-lg-3 col-md-6">
                                    <div class="card h-100 border rounded-3 shadow-sm position-relative ${reward.stock == 0 ? 'bg-light' : ''}">
                                        <span class="position-absolute top-0 end-0 mt-3 me-3 ${reward.stock == 0 ? 'text-muted' : 'text-danger'} small fw-bold text-uppercase" style="font-size:0.7rem;">${reward.category}</span>
                                        <div class="card-body d-flex flex-column ${reward.stock == 0 ? 'opacity-75' : ''}">
                                            <div class="rounded ${reward.stock == 0 ? 'bg-secondary-subtle' : 'bg-danger-subtle'} d-inline-flex p-2 mb-3" style="width:fit-content;">
                                                <span class="material-symbols-outlined ${reward.stock == 0 ? 'text-secondary' : 'text-danger'}">${reward.imageIcon}</span>
                                            </div>
                                            <h5 class="fw-bold fs-6 mb-2">${reward.name}</h5>
                                            <p class="text-muted mb-4" style="font-size: 0.8rem;">${reward.description}</p>
                                            <div class="mt-auto d-flex justify-content-between align-items-end">
                                                <div>
                                                    <div class="text-muted" style="font-size: 0.7rem;">Điểm cần:</div>
                                                    <div class="fw-bold ${reward.stock == 0 ? 'text-secondary' : 'text-danger'} fs-5">${reward.pointsCost} <span class="fs-6">pts</span></div>
                                                </div>
                                                <c:choose>
                                                    <c:when test="${reward.stock == 0}">
                                                        <button class="btn btn-secondary btn-sm fw-bold px-3 py-2 rounded-pill" disabled>Hết hàng</button>
                                                    </c:when>
                                                    <c:when test="${sessionScope.user.totalPoints < reward.pointsCost}">
                                                        <button class="btn btn-outline-secondary btn-sm fw-bold px-3 py-2 rounded-pill" disabled>Thiếu điểm</button>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <form action="${pageContext.request.contextPath}/profile?action=redeemReward" method="POST" class="m-0">
                                                            <input type="hidden" name="rewardId" value="${reward.id}">
                                                            <button type="submit" class="btn btn-primary-custom btn-sm fw-bold px-3 py-2 rounded-pill" onclick="return confirm('Bạn có chắc chắn muốn dùng ${reward.pointsCost} điểm để đổi quà này?');">Đổi ngay</button>
                                                        </form>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                </div>

                <!-- Tab: History -->
                <div class="tab-pane fade" id="history" role="tabpanel" tabindex="0">
                    <div class="info-card">
                        <div class="d-flex justify-content-between align-items-start mb-4 pb-3 border-bottom">
                            <div>
                                <h3 class="fs-5 fw-bold mb-1">Lịch sử biến động điểm thưởng</h3>
                                <p class="text-muted mb-0" style="font-size: 0.85rem;">Theo dõi chi tiết các lần tích và tiêu điểm của bạn</p>
                            </div>
                            <button class="btn btn-outline-danger btn-sm d-flex align-items-center gap-1 fw-bold rounded-pill px-3 py-2">
                                <span class="material-symbols-outlined fs-6">download</span> Xuất sao kê
                            </button>
                        </div>

                        <div class="d-flex flex-column">
                            <c:choose>
                                <c:when test="${empty loyaltyHistory}">
                                    <div class="text-center py-5 text-muted">
                                        <span class="material-symbols-outlined fs-1 mb-2 text-black-50">history_toggle_off</span>
                                        <p class="mb-0">Bạn chưa có lịch sử tích/tiêu điểm nào.</p>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="txn" items="${loyaltyHistory}" varStatus="loop">
                                        <div class="d-flex justify-content-between align-items-center py-3 ${not loop.last ? 'border-bottom' : ''}">
                                            <div class="d-flex align-items-center gap-3">
                                                <span class="material-symbols-outlined ${txn.pointsChange >= 0 ? 'text-success border-success' : 'text-danger border-danger'} border rounded-circle p-1">
                                                    ${txn.pointsChange >= 0 ? 'add' : 'remove'}
                                                </span>
                                                <div>
                                                    <div class="fw-bold fs-6">
                                                        <c:choose>
                                                            <c:when test="${txn.transactionType == 'EARN'}">
                                                                Tích điểm đơn hàng #AUR-${txn.orderId}
                                                            </c:when>
                                                            <c:otherwise>
                                                                Sử dụng điểm đổi ưu đãi
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </div>
                                                    <div class="text-muted small"><fmt:formatDate value="${txn.createdAt}" pattern="dd/MM/yyyy HH:mm" /></div>
                                                </div>
                                            </div>
                                            <div class="text-end">
                                                <div class="fw-bold ${txn.pointsChange >= 0 ? 'text-success' : 'text-danger'} fs-5">
                                                    ${txn.pointsChange > 0 ? '+' : ''}${txn.pointsChange}
                                                </div>
                                                <div class="text-muted" style="font-size: 0.75rem;">Mã GD: #${txn.id}</div>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>

                <!-- Tab: Privileges -->
                <div class="tab-pane fade" id="privileges" role="tabpanel" tabindex="0">
                    <div class="row g-4">
                        <!-- Silver -->
                        <div class="col-md-4">
                            <div class="info-card h-100 position-relative ${sessionScope.user.totalPoints < 300 ? 'border-warning shadow' : ''}" ${sessionScope.user.totalPoints < 300 ? 'style="border-width: 2px;"' : ''}>
                                <c:if test="${sessionScope.user.totalPoints < 300}">
                                    <span class="position-absolute top-0 end-0 mt-3 me-3 badge bg-warning text-dark fw-bold rounded-pill">HẠNG HIỆN TẠI CỦA BẠN</span>
                                </c:if>
                                <div class="d-flex align-items-center mb-3">
                                    <div class="rounded-circle bg-light d-flex align-items-center justify-content-center me-3" style="width: 45px; height: 45px;">
                                        <span class="material-symbols-outlined text-secondary fs-4">military_tech</span>
                                    </div>
                                </div>
                                <h4 class="fw-bold mb-1">Hạng Bạc (Silver)</h4>
                                <p class="text-muted small">Dành cho khách hàng mới gia nhập</p>
                                <div class="fw-bold mb-4">0 - 299 Điểm tích lũy</div>
                                <hr class="mb-4">
                                <ul class="list-unstyled mb-0 d-flex flex-column gap-3">
                                    <li class="d-flex align-items-start gap-2"><span class="material-symbols-outlined text-success fs-5">check_circle</span> <span class="small text-muted">Tích luỹ 5% trên mỗi hóa đơn thanh toán</span></li>
                                    <li class="d-flex align-items-start gap-2"><span class="material-symbols-outlined text-success fs-5">check_circle</span> <span class="small text-muted">Tặng E-voucher 20.000đ mừng gia nhập</span></li>
                                    <li class="d-flex align-items-start gap-2"><span class="material-symbols-outlined text-success fs-5">check_circle</span> <span class="small text-muted">Nhận tin ưu đãi độc quyền qua ứng dụng</span></li>
                                </ul>
                            </div>
                        </div>
                        <!-- Gold -->
                        <div class="col-md-4">
                            <div class="info-card h-100 position-relative ${sessionScope.user.totalPoints >= 300 && sessionScope.user.totalPoints < 800 ? 'border-warning shadow' : ''}" ${sessionScope.user.totalPoints >= 300 && sessionScope.user.totalPoints < 800 ? 'style="border-width: 2px;"' : ''}>
                                <c:if test="${sessionScope.user.totalPoints >= 300 && sessionScope.user.totalPoints < 800}">
                                    <span class="position-absolute top-0 end-0 mt-3 me-3 badge bg-warning text-dark fw-bold rounded-pill">HẠNG HIỆN TẠI CỦA BẠN</span>
                                </c:if>
                                <div class="d-flex align-items-center mb-3">
                                    <div class="rounded-circle bg-warning bg-opacity-25 d-flex align-items-center justify-content-center me-3" style="width: 45px; height: 45px;">
                                        <span class="material-symbols-outlined text-warning fs-4">stars</span>
                                    </div>
                                </div>
                                <h4 class="fw-bold mb-1">Hạng Vàng (Gold)</h4>
                                <p class="text-muted small">Dành cho tín đồ cà phê Aura thân thiết</p>
                                <div class="fw-bold mb-4 text-warning">300 - 799 Điểm tích lũy</div>
                                <hr class="mb-4 border-warning">
                                <ul class="list-unstyled mb-0 d-flex flex-column gap-3">
                                    <li class="d-flex align-items-start gap-2"><span class="material-symbols-outlined text-warning fs-5">check_circle</span> <span class="small fw-medium">Tích luỹ 10% trên mỗi đơn hàng</span></li>
                                    <li class="d-flex align-items-start gap-2"><span class="material-symbols-outlined text-warning fs-5">check_circle</span> <span class="small fw-medium">Tặng 01 bánh ngọt miễn phí vào tháng sinh nhật</span></li>
                                    <li class="d-flex align-items-start gap-2"><span class="material-symbols-outlined text-warning fs-5">check_circle</span> <span class="small fw-medium">Miễn phí nâng cấp kích cỡ ly (Upsize) thứ 3 hàng tuần</span></li>
                                    <li class="d-flex align-items-start gap-2"><span class="material-symbols-outlined text-warning fs-5">check_circle</span> <span class="small fw-medium">Ưu tiên pha chế và xử lý đơn hỏa tốc</span></li>
                                </ul>
                            </div>
                        </div>
                        <!-- Diamond -->
                        <div class="col-md-4">
                            <div class="info-card h-100 position-relative ${sessionScope.user.totalPoints >= 800 ? 'border-warning shadow' : ''}" ${sessionScope.user.totalPoints >= 800 ? 'style="border-width: 2px;"' : ''}>
                                <c:if test="${sessionScope.user.totalPoints >= 800}">
                                    <span class="position-absolute top-0 end-0 mt-3 me-3 badge bg-warning text-dark fw-bold rounded-pill">HẠNG HIỆN TẠI CỦA BẠN</span>
                                </c:if>
                                <div class="d-flex align-items-center mb-3">
                                    <div class="rounded-circle d-flex align-items-center justify-content-center me-3" style="width: 45px; height: 45px; background-color: #f3e5f5;">
                                        <span class="material-symbols-outlined fs-4" style="color: #9c27b0;">diamond</span>
                                    </div>
                                </div>
                                <h4 class="fw-bold mb-1">Hạng Kim Cương (Diamond)</h4>
                                <p class="text-muted small">Đặc quyền tối thượng dành riêng cho VIP</p>
                                <div class="fw-bold mb-4" style="color: #9c27b0;">Từ 800 Điểm trở lên</div>
                                <hr class="mb-4">
                                <ul class="list-unstyled mb-0 d-flex flex-column gap-3">
                                    <li class="d-flex align-items-start gap-2"><span class="material-symbols-outlined fs-5" style="color: #9c27b0;">check_circle</span> <span class="small text-muted">Tích luỹ 15% không giới hạn giá trị</span></li>
                                    <li class="d-flex align-items-start gap-2"><span class="material-symbols-outlined fs-5" style="color: #9c27b0;">check_circle</span> <span class="small text-muted">Miễn phí giao hàng trọn đời mọi đơn từ 50k</span></li>
                                    <li class="d-flex align-items-start gap-2"><span class="material-symbols-outlined fs-5" style="color: #9c27b0;">check_circle</span> <span class="small text-muted">Mời tham dự workshop thử nếm cà phê đặc sản cao cấp</span></li>
                                    <li class="d-flex align-items-start gap-2"><span class="material-symbols-outlined fs-5" style="color: #9c27b0;">check_circle</span> <span class="small text-muted">Đường dây nóng CSKH hỗ trợ riêng biệt 24/7</span></li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </main>

    <!-- Auth Modal Component (For completeness) -->
    <jsp:include page="/components/auth_modal.jsp" />

    <!-- Bootstrap 5 JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        document.addEventListener('DOMContentLoaded', function() {
            const btnEdit = document.getElementById('btnEditProfile');
            const btnCancel = document.getElementById('btnCancelEdit');
            const btnSave = document.getElementById('btnSaveProfile');
            const editActions = document.getElementById('editActions');
            const profileInputs = document.querySelectorAll('.profile-input');
            
            // Store original values
            const originalValues = {};

            // Check if form changed
            function checkFormChanged() {
                let isChanged = false;
                profileInputs.forEach(input => {
                    if (input.value !== originalValues[input.name]) {
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

            btnEdit.addEventListener('click', function() {
                // Enable inputs
                profileInputs.forEach(input => {
                    originalValues[input.name] = input.value;
                    input.removeAttribute('readonly');
                    input.style.backgroundColor = 'white';
                    input.style.borderColor = 'var(--primary)';
                });
                // Focus first input
                if(profileInputs.length > 0) profileInputs[0].focus();
                
                // Show actions, hide edit button
                editActions.classList.remove('d-none');
                editActions.classList.add('d-flex');
                btnEdit.classList.add('d-none');
                checkFormChanged(); // Reset button state
            });

            btnCancel.addEventListener('click', function() {
                // Restore inputs
                profileInputs.forEach(input => {
                    if (originalValues[input.name] !== undefined) {
                        input.value = originalValues[input.name];
                    }
                    input.setAttribute('readonly', 'readonly');
                    input.style.backgroundColor = '';
                    input.style.borderColor = '';
                    // Remove valid/invalid classes if any
                    input.classList.remove('is-invalid');
                });
                
                // Hide actions, show edit button
                editActions.classList.remove('d-flex');
                editActions.classList.add('d-none');
                btnEdit.classList.remove('d-none');
            });
        });

        // Toast notification helper
        function showToast(message) {
            const toastEl = document.getElementById('profileToast');
            const toastMsg = document.getElementById('profileToastMsg');
            if (toastEl && toastMsg) {
                toastMsg.textContent = message;
                const toast = new bootstrap.Toast(toastEl, { delay: 2500 });
                toast.show();
            }
        }

        // Copy Member ID / Barcode to clipboard
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

        // Toggle password visibility
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

        // Validate password form before submission
        document.getElementById('passwordForm').addEventListener('submit', function(event) {
            const newPassword = document.getElementById('newPassword');
            const confirmPassword = document.getElementById('confirmPassword');
            let isValid = true;

            if (newPassword.value.length < 6) {
                newPassword.classList.add('is-invalid');
                isValid = false;
            } else {
                newPassword.classList.remove('is-invalid');
            }

            if (newPassword.value !== confirmPassword.value) {
                confirmPassword.classList.add('is-invalid');
                isValid = false;
            } else {
                confirmPassword.classList.remove('is-invalid');
            }

            if (!isValid) {
                event.preventDefault(); // Stop form submission
            }
        });

        // Handle direct navigation to tabs via URL hash
        window.addEventListener('DOMContentLoaded', (event) => {
            const hash = window.location.hash;
            if (hash) {
                // Determine the correct tab button id based on hash
                // Example: hash #privileges -> id privileges-tab
                const tabId = hash.substring(1) + '-tab';
                const tabElement = document.getElementById(tabId);
                if (tabElement) {
                    tabElement.click();
                    
                    // Scroll into view slightly to show the tab content
                    setTimeout(() => {
                        document.querySelector('.profile-nav-bar').scrollIntoView({ behavior: 'smooth', block: 'start' });
                    }, 300);
                }
            }
        });
    </script>

    <!-- Toast Notification Container -->
    <div class="toast-container position-fixed bottom-0 end-0 p-3" style="z-index: 1100;">
        <div id="profileToast" class="toast align-items-center text-bg-dark border-0 shadow" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body d-flex align-items-center gap-2">
                    <span class="material-symbols-outlined text-success fs-5">check_circle</span>
                    <span id="profileToastMsg">Đã sao chép!</span>
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    </div>
</body>
</html>
