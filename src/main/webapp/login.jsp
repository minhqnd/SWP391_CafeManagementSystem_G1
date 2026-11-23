<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    // [ATCU01]
    // . login.jsp chuyển người dùng về Trang chủ kèm tab tài khoản để auth_modal.jsp mở đúng form. Đây là
    //   đường dẫn chuyển tiếp; việc đăng nhập chỉ được xử lý ở LoginController.doPost khi người dùng gửi
    //   form thực tế.
    // ... Kết thúc đường dẫn chuyển tiếp khi Trang chủ mở auth_modal.jsp đúng tab tài khoản; nút gửi của form mới
    //     bắt đầu đăng nhập hoặc đăng ký.
    String legacyTab = request.getParameter("tab");
    if (!java.util.Set.of("login", "register", "forgot", "otp", "reset_password").contains(legacyTab == null ? "" : legacyTab)) legacyTab = "login";
    response.sendRedirect(request.getContextPath() + "/?tab=" + legacyTab);
    if (true) return;
%>

<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <title>Aura Café | Đăng nhập</title>

        <!-- Google Fonts -->
        <link rel="preconnect" href="https://fonts.googleapis.com">
        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet"/>
        <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet"/>

        <!-- Bootstrap 5 CSS -->
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">

        <style>
            :root {
                --surface: #f8f9ff;
                --surface-container-lowest: #ffffff;
                --surface-container-low: #eff4ff;
                --surface-container: #e6eeff;
                --surface-container-high: #dee9fc;
                --surface-container-highest: #d9e3f6;
                --on-surface: #121c2a;
                --secondary: #555f71;
                --primary: #900b1a;
                --primary-container: #b2292e;
                --on-primary: #ffffff;
                --on-primary-container: #ffcdc9;
            }

            body {
                font-family: 'Inter', sans-serif;
                background-color: var(--surface);
                color: var(--on-surface);
            }

            /* Layout overrides */
            .auth-wrapper {
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 1.5rem;
            }

            .auth-card {
                width: 100%;
                max-width: 900px;
                background-color: var(--surface-container-lowest);
                border: none;
                border-radius: 1rem;
                box-shadow: 0 1rem 3rem rgba(0,0,0,0.1);
                overflow: hidden;
            }

            .auth-left {
                background-color: var(--surface-container);
                min-height: 260px;
                padding: 2rem;
                display: flex;
                flex-direction: column;
                justify-content: space-between;
                position: relative;
                overflow: hidden;
            }

            @media (min-width: 768px) {
                .auth-left {
                    min-height: 580px;
                    padding: 2.5rem;
                }
                .auth-right {
                    padding: 2.5rem !important;
                }
            }

            .auth-bg-img {
                position: absolute;
                inset: 0;
                background-size: cover;
                background-position: center;
                opacity: 0.3;
                mix-blend-mode: multiply;
            }

            .auth-glow-1, .auth-glow-2 {
                position: absolute;
                width: 14rem;
                height: 14rem;
                border-radius: 50%;
                pointer-events: none;
            }
            .auth-glow-1 {
                top: -4rem;
                left: -4rem;
                background-color: rgba(144, 11, 26, 0.1);
                filter: blur(40px);
            }
            .auth-glow-2 {
                bottom: -4rem;
                right: -4rem;
                background-color: rgba(178, 41, 46, 0.15);
                filter: blur(30px);
            }

            .glass-badge {
                background: rgba(255, 255, 255, 0.8);
                backdrop-filter: blur(4px);
                padding: 0.25rem 1rem;
                border-radius: 50rem;
                display: inline-flex;
                align-items: center;
                gap: 0.5rem;
                font-size: 0.6875rem;
                font-weight: 600;
                color: var(--primary);
                text-transform: uppercase;
                letter-spacing: 0.1em;
                box-shadow: 0 1px 2px rgba(0,0,0,0.05);
            }

            .pulse-dot {
                width: 10px;
                height: 10px;
                border-radius: 50%;
                background-color: var(--primary-container);
                animation: pulse 2s infinite;
            }

            @keyframes pulse {
                0% {
                    transform: scale(0.95);
                    box-shadow: 0 0 0 0 rgba(178, 41, 46, 0.7);
                }
                70% {
                    transform: scale(1);
                    box-shadow: 0 0 0 6px rgba(178, 41, 46, 0);
                }
                100% {
                    transform: scale(0.95);
                    box-shadow: 0 0 0 0 rgba(178, 41, 46, 0);
                }
            }

            .icon-box {
                width: 40px;
                height: 40px;
                border-radius: 0.5rem;
                background-color: var(--primary-container);
                color: var(--on-primary);
                display: flex;
                align-items: center;
                justify-content: center;
                box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            }

            .metric-chip {
                background: rgba(255, 255, 255, 0.7);
                backdrop-filter: blur(4px);
                padding: 0.5rem;
                border-radius: 0.5rem;
                display: inline-flex;
                align-items: center;
                gap: 1rem;
                box-shadow: 0 1px 2px rgba(0,0,0,0.05);
                margin-top: 1.5rem;
            }

            /* Nav Tabs Override */
            .nav-pills-custom {
                background-color: var(--surface-container-high);
                padding: 0.25rem;
                border-radius: 0.5rem;
                margin-bottom: 1.5rem;
                display: flex;
            }
            .nav-pills-custom .nav-link {
                flex: 1;
                text-align: center;
                color: var(--secondary);
                font-weight: 600;
                border-radius: 0.25rem;
                padding: 0.5rem 1rem;
                transition: all 0.2s;
            }
            .nav-pills-custom .nav-link:hover {
                color: var(--on-surface);
            }
            .nav-pills-custom .nav-link.active {
                background-color: var(--surface-container-lowest);
                color: var(--on-surface);
                box-shadow: 0 1px 2px rgba(0,0,0,0.05);
            }

            /* Form controls */
            .form-label {
                font-size: 0.75rem;
                font-weight: 500;
                margin-bottom: 0.25rem;
            }
            .input-group-text {
                background-color: var(--surface-container-low);
                border: none;
                color: var(--secondary);
            }
            .form-control, .form-select {
                background-color: var(--surface-container-low);
                border: none;
                height: 44px;
                color: var(--on-surface);
                font-size: 0.875rem;
            }
            .form-control:focus, .form-select:focus {
                background-color: var(--surface-container-lowest);
                box-shadow: 0 0 0 2px rgba(178, 41, 46, 0.2);
                outline: none;
            }
            .input-group > .form-control {
                border-left: none;
                padding-left: 0;
            }
            .input-group > .input-group-text:first-child {
                padding-right: 0.5rem;
            }

            .btn-primary-custom {
                background-color: var(--primary-container);
                color: var(--on-primary);
                height: 48px;
                font-weight: 600;
                border: none;
                border-radius: 0.25rem;
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 0.5rem;
                transition: all 0.2s;
            }
            .btn-primary-custom:hover {
                background-color: var(--primary);
                color: var(--on-primary);
            }
            .btn-primary-custom:active {
                transform: scale(0.99);
            }

            .btn-social {
                background-color: var(--surface-container-low);
                color: var(--on-surface);
                height: 44px;
                font-size: 0.75rem;
                font-weight: 500;
                border: none;
                border-radius: 0.25rem;
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 0.5rem;
                transition: all 0.2s;
                width: 100%;
            }
            .btn-social:hover {
                background-color: var(--surface-container);
            }

            .divider {
                display: flex;
                align-items: center;
                text-align: center;
                margin: 1.5rem 0;
            }
            .divider::before, .divider::after {
                content: '';
                flex: 1;
                border-bottom: 1px solid var(--surface-container-highest);
            }
            .divider span {
                padding: 0 1rem;
                font-size: 0.6875rem;
                color: var(--secondary);
                text-transform: uppercase;
                letter-spacing: 0.05em;
            }

            a {
                color: var(--primary);
                text-decoration: none;
            }
            a:hover {
                text-decoration: underline;
            }

            .form-check-input:checked {
                background-color: var(--primary-container);
                border-color: var(--primary-container);
            }

            .text-secondary-custom {
                color: var(--secondary);
            }
            .text-primary-custom {
                color: var(--primary);
            }
        </style>
    </head>
    <body>

        <div class="auth-wrapper">
            <div class="row auth-card m-0 g-0">
                <!-- Left Atmospheric Brand Showcase -->
                <div class="col-12 col-md-5 auth-left">
                    <div class="auth-bg-img" style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuAGFUqokLpPQhYrL16Lx1MmWKuiDJT-ODG6DB0BHusP12uZytq7EXIlX8NPaQjkCjGB962XBMQtan6ezZNms1rtoX6miKboAymKvO9ofSFSV355L6uD3GniaPpqO7EkrnrQHFympfYpI2XS2W_2wxEJHvbcy2DOo0Ycop5LZ7iuoWV3pcYpESD0Eo_0xCKo_FdIzK8HVEvMlyK6s2Vnxxnjxl9kVFsG3tT8-M5dD_clWwQiVacYM5ot')"></div>

                    <div class="auth-glow-1"></div>
                    <div class="auth-glow-2"></div>

                    <!-- Brand Header -->
                    <div class="position-relative z-1">
                        <div class="glass-badge">
                            <span class="pulse-dot"></span>
                            Hệ Thống Vận Hành 2.4
                        </div>
                    </div>

                    <!-- Mid Narrative & Micro Metric Badge -->
                    <div class="position-relative z-1 my-auto py-4">
                        <div class="d-flex align-items-center gap-2 mb-2">
                            <div class="icon-box">
                                <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">local_cafe</span>
                            </div>
                            <div>
                                <h1 class="m-0 fw-bold fs-3" style="letter-spacing:-0.02em; line-height:1;">
                                    Aura <span style="color: var(--primary-container)">Café</span>
                                </h1>
                                <p class="m-0 text-secondary-custom" style="font-size: 0.6875rem; font-weight:600; letter-spacing:0.04em;">Crafting Digital Moments</p>
                            </div>
                        </div>
                        <p class="text-secondary-custom mt-3 d-none d-md-block" style="font-size: 0.875rem; line-height:1.5;">
                            Nền tảng quản trị đồng bộ đa vai trò: Thu ngân, Pha chế KDS, Shipper và Quản lý chuỗi chi nhánh thời gian thực.
                        </p>

                        <!-- Metric Inline Chip -->
                        <div class="metric-chip d-none d-md-inline-flex">
                            <div class="p-1 rounded text-primary-custom" style="background-color: var(--surface-container-high);">
                                <span class="material-symbols-outlined fs-6 d-block">bolt</span>
                            </div>
                            <div>
                                <div class="text-secondary-custom" style="font-size: 0.6875rem; font-weight:600;">Tốc độ xử lý đơn POS</div>
                                <div class="fw-bold" style="font-size: 0.8125rem;">&lt; 1.8 giây / đơn</div>
                            </div>
                        </div>
                    </div>

                    <!-- Bottom Brand Metadata -->
                    <div class="position-relative z-1 d-flex justify-content-between text-secondary-custom pt-3" style="font-size: 0.6875rem;">
                        <span class="text-uppercase tracking-wider fw-semibold">Chi nhánh Sài Gòn • Hà Nội</span>
                        <span class="fw-medium">v2.4.9-PRO</span>
                    </div>
                </div>

                <!-- Right Interactive Form Container -->
                <div class="col-12 col-md-7 p-4 auth-right d-flex flex-column justify-content-center">

                    <!-- Error message from Servlet -->
                    <% if (request.getAttribute("error") != null) { %>
                    <div class="alert alert-danger p-2 mb-3 text-center" role="alert" style="font-size: 0.875rem;">
                        <%= request.getAttribute("error") %>
                    </div>
                    <% } %>

                    <!-- Success message from Servlet -->
                    <% if (request.getAttribute("success") != null) { %>
                    <div class="alert alert-success p-2 mb-3 text-center" role="alert" style="font-size: 0.875rem;">
                        <%= request.getAttribute("success") %>
                    </div>
                    <% } %>

                    <%
                        String activeTab = request.getParameter("tab");
                        if (activeTab == null) {
                            activeTab = (String) request.getAttribute("activeTab");
                        }
                        if (activeTab == null) activeTab = "login";

                        boolean isOtpTab = "otp".equals(activeTab);
                        boolean isResetTab = "reset_password".equals(activeTab);

                        /* Tính thời gian đếm ngược gửi lại OTP */
                        Long lastOtpSentTime = (Long) session.getAttribute("lastOtpSentTime");
                        long otpRemainingTime = 0;
                        if (lastOtpSentTime != null) {
                            long elapsed = System.currentTimeMillis() - lastOtpSentTime;
                            if (elapsed < 60000) {
                                otpRemainingTime = (60000 - elapsed) / 1000;
                            }
                        }
                    %>

                    <!-- Interactive Tab Selector -->
                    <% if (!isOtpTab && !isResetTab) { %>
                    <ul class="nav nav-pills-custom nav-justified" id="authTab" role="tablist">
                        <li class="nav-item" role="presentation">
                            <button class="nav-link <%= "login".equals(activeTab) ? "active" : "" %>" id="login-tab" data-bs-toggle="pill" data-bs-target="#login-pane" type="button" role="tab">Đăng nhập</button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link <%= "register".equals(activeTab) ? "active" : "" %>" id="register-tab" data-bs-toggle="pill" data-bs-target="#register-pane" type="button" role="tab">Đăng ký</button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link <%= "forgot".equals(activeTab) ? "active" : "" %>" id="forgot-tab" data-bs-toggle="pill" data-bs-target="#forgot-pane" type="button" role="tab">Quên mật khẩu</button>
                        </li>
                    </ul>
                    <% } %>

                    <div class="tab-content" id="authTabContent">
                        <!-- TAB 1: LOGIN FORM -->
                        <div class="tab-pane fade <%= "login".equals(activeTab) ? "show active" : "" %>" id="login-pane" role="tabpanel" tabindex="0">
                            <div class="mb-3">
                                <h2 class="fs-4 fw-semibold mb-1">Chào mừng trở lại</h2>
                                <p class="text-secondary-custom fs-6 mb-0" style="font-size: 0.75rem !important;">Vui lòng nhập định danh tài khoản vận hành Aura Café</p>
                            </div>

                            <form action="${pageContext.request.contextPath}/login" method="POST">
                                <div class="mb-3">
                                    <label for="login-username" class="form-label">Email hoặc số điện thoại</label>
                                    <div class="input-group">
                                        <span class="input-group-text">
                                            <span class="material-symbols-outlined fs-5">account_circle</span>
                                        </span>
                                        <input type="text" id="login-username" class="form-control" name="username" placeholder="Nhập email hoặc số điện thoại" required autocomplete="username">
                                    </div>
                                </div>

                                <div class="mb-3">
                                    <div class="d-flex justify-content-between align-items-center mb-1">
                                        <label class="form-label mb-0">Mật khẩu</label>
                                        <a href="#" onclick="document.getElementById('forgot-tab').click(); return false;" class="text-decoration-none" style="font-size: 0.6875rem; font-weight: 600;">Cần trợ giúp?</a>
                                    </div>
                                    <div class="input-group">
                                        <span class="input-group-text">
                                            <span class="material-symbols-outlined fs-5">lock</span>
                                        </span>
                                        <input type="password" class="form-control border-end-0" name="password" id="login-pwd" placeholder="••••••••••••" required autocomplete="current-password">
                                        <button class="input-group-text bg-surface-container-low border-start-0" type="button" onclick="togglePasswordVisibility('login-pwd', 'login-pwd-icon')">
                                            <span class="material-symbols-outlined fs-5" id="login-pwd-icon">visibility</span>
                                        </button>
                                    </div>
                                </div>

                                <div class="mb-3 form-check">
                                    <input type="checkbox" class="form-check-input" id="rememberMe">
                                    <label class="form-check-label text-secondary-custom" for="rememberMe" style="font-size: 0.75rem;">Ghi nhớ phiên đăng nhập trên thiết bị này</label>
                                </div>

                                <button type="submit" class="btn btn-primary-custom w-100">
                                    <span>Đăng nhập hệ thống</span>
                                    <span class="material-symbols-outlined">arrow_forward</span>
                                </button>
                            </form>

                            <div class="divider">
                                <span>Hoặc tiếp tục với</span>
                            </div>

                            <div class="row g-3">
                                <div class="col-6">
                                    <button type="button" class="btn btn-social">
                                        <svg style="width:16px;height:16px;" viewBox="0 0 24 24">
                                        <path d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.17z" fill="#4285F4"></path>
                                        <path d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.34 24 12 24z" fill="#34A853"></path>
                                        <path d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.18 0 9.99 0 12s.45 3.82 1.25 5.42l4.03-3.15z" fill="#FBBC05"></path>
                                        <path d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.34 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.93 6.72-4.93z" fill="#EA4335"></path>
                                        </svg>
                                        <span>Google</span>
                                    </button>
                                </div>
                                <div class="col-6">
                                    <button type="button" class="btn btn-social" onclick="alert('Tính năng đang phát triển')">
                                        <span class="material-symbols-outlined text-primary-custom" style="font-size:18px;">sms</span>
                                        <span>Mã OTP</span>
                                    </button>
                                </div>
                            </div>
                        </div>

                        <!-- TAB 2: REGISTER FORM -->
                        <div class="tab-pane fade <%= "register".equals(activeTab) ? "show active" : "" %>" id="register-pane" role="tabpanel" tabindex="0">
                            <div class="mb-3">
                                <h2 class="fs-4 fw-semibold mb-1">Tạo tài khoản Aura</h2>
                                <p class="text-secondary-custom fs-6 mb-0" style="font-size: 0.75rem !important;">Đăng ký tài khoản nội bộ chi nhánh hoặc đối tác</p>
                            </div>

                            <form action="${pageContext.request.contextPath}/register" method="POST">
                                <div class="row g-3 mb-3">
                                    <div class="col-12">
                                        <label class="form-label">Họ và tên</label>
                                        <input type="text" class="form-control" name="fullname" placeholder="Nguyễn Văn A" required>
                                    </div>
                                </div>

                                <div class="mb-3">
                                    <label class="form-label">Số điện thoại</label>
                                    <div class="input-group">
                                        <span class="input-group-text">
                                            <span class="material-symbols-outlined fs-5">call</span>
                                        </span>
                                        <input type="tel" class="form-control" name="phone" placeholder="VD: 0912345678" pattern="0[0-9]{9}" inputmode="numeric" title="Nhập 10 chữ số và bắt đầu bằng số 0" required>
                                    </div>
                                </div>

                                <div class="mb-3">
                                    <label class="form-label">Email</label>
                                    <div class="input-group">
                                        <span class="input-group-text">
                                            <span class="material-symbols-outlined fs-5">alternate_email</span>
                                        </span>
                                        <input type="email" class="form-control" name="email" placeholder="Ví dụ: nguyen@example.com" required>
                                    </div>
                                </div>

                                <div class="mb-3">
                                    <label class="form-label">Mật khẩu bảo mật</label>
                                    <div class="input-group">
                                        <span class="input-group-text">
                                            <span class="material-symbols-outlined fs-5">key</span>
                                        </span>
                                        <input type="password" class="form-control border-end-0" id="reg-pwd" name="password" placeholder="Tối thiểu 8 ký tự" required>
                                        <button class="input-group-text bg-surface-container-low border-start-0" type="button" onclick="togglePasswordVisibility('reg-pwd', 'reg-pwd-icon')">
                                            <span class="material-symbols-outlined fs-5" id="reg-pwd-icon">visibility</span>
                                        </button>
                                    </div>
                                </div>

                                <div class="mb-4 form-check">
                                    <input type="checkbox" class="form-check-input" id="terms" required>
                                    <label class="form-check-label text-secondary-custom" for="terms" style="font-size: 0.75rem;">
                                        Tôi đồng ý với <a href="#">Quy chế bảo mật</a> và điều khoản.
                                    </label>
                                </div>

                                <button type="submit" class="btn btn-primary-custom w-100 mb-3">
                                    <span>Tạo tài khoản mới</span>
                                    <span class="material-symbols-outlined">how_to_reg</span>
                                </button>

                                <div class="text-center">
                                    <a href="#" class="text-secondary-custom text-decoration-none" style="font-size: 0.75rem;" onclick="document.getElementById('login-tab').click(); return false;">
                                        Đã có tài khoản? <span class="fw-semibold text-primary-custom">Đăng nhập ngay</span>
                                    </a>
                                </div>
                            </form>
                        </div>

                        <!-- TAB 4: OTP VERIFICATION -->
                        <div class="tab-pane fade <%= isOtpTab ? "show active" : "" %>" id="otp-pane" role="tabpanel" tabindex="0">
                            <div class="mb-3">
                                <h2 class="fs-4 fw-semibold mb-1">Xác thực Email</h2>
                                <p class="text-secondary-custom fs-6 mb-0" style="font-size: 0.75rem !important;">Nhập mã OTP đã được gửi đến email của bạn</p>
                            </div>

                            <form action="${pageContext.request.contextPath}/verify-otp" method="POST">
                                <div class="mb-3">
                                    <label class="form-label">Mã xác thực (OTP)</label>
                                    <input type="text" class="form-control text-center fw-bold fs-4" name="otp" placeholder="------" maxlength="6" autocomplete="off" required>
                                </div>

                                <div class="p-3 mb-4 rounded d-flex align-items-start gap-2" style="background-color: var(--surface-container-low); font-size: 0.75rem; color: var(--secondary);">
                                    <span class="material-symbols-outlined text-primary-custom fs-5">info</span>
                                    <span>Vui lòng kiểm tra hòm thư đến (hoặc thư rác) để lấy mã xác thực gồm 6 chữ số.</span>
                                </div>

                                <button type="submit" class="btn btn-primary-custom w-100 mb-2">
                                    <span>Xác nhận OTP</span>
                                    <span class="material-symbols-outlined">check_circle</span>
                                </button>

                                <div class="d-flex justify-content-between align-items-center mt-3">
                                    <a href="${pageContext.request.contextPath}/login.jsp?tab=forgot" class="text-secondary-custom text-decoration-none d-inline-flex align-items-center gap-1" style="font-size: 0.75rem;">
                                        <span class="material-symbols-outlined fs-6">arrow_back</span>
                                        <span>Quay lại</span>
                                    </a>
                                    <div>
                                        <span id="login-resend-timer" style="font-size: 0.75rem; color: var(--secondary); <%= otpRemainingTime > 0 ? "" : "display: none;" %>">
                                            Gửi lại sau <strong id="login-countdown-sec"><%= otpRemainingTime %></strong>s
                                        </span>
                                        <a href="${pageContext.request.contextPath}/resend-otp" id="login-resend-btn" class="text-decoration-none fw-semibold text-primary-custom" style="font-size: 0.75rem; <%= otpRemainingTime > 0 ? "display: none;" : "" %>">
                                            Gửi lại mã OTP
                                        </a>
                                    </div>
                                </div>
                            </form>
                        </div>

                        <!-- TAB 5: RESET PASSWORD FORM -->
                        <div class="tab-pane fade <%= isResetTab ? "show active" : "" %>" id="reset-pane" role="tabpanel" tabindex="0">
                            <div class="mb-3">
                                <h2 class="fs-4 fw-semibold mb-1">Thiết lập mật khẩu mới</h2>
                                <p class="text-secondary-custom fs-6 mb-0" style="font-size: 0.75rem !important;">Vui lòng tạo mật khẩu mới cho tài khoản của bạn</p>
                            </div>

                            <form action="${pageContext.request.contextPath}/reset-password" method="POST">
                                <div class="mb-3">
                                    <label class="form-label">Mật khẩu mới</label>
                                    <div class="input-group">
                                        <span class="input-group-text">
                                            <span class="material-symbols-outlined fs-5">lock</span>
                                        </span>
                                        <input type="password" id="reset-pwd" class="form-control border-end-0" name="password" placeholder="Ít nhất 8 ký tự, gồm chữ và số" minlength="8" required>
                                        <button class="input-group-text bg-surface-container-low border-start-0" type="button" onclick="togglePasswordVisibility('reset-pwd', 'reset-pwd-icon')">
                                            <span class="material-symbols-outlined fs-5" id="reset-pwd-icon">visibility</span>
                                        </button>
                                    </div>
                                </div>

                                <div class="mb-4">
                                    <label class="form-label">Xác nhận mật khẩu</label>
                                    <div class="input-group">
                                        <span class="input-group-text">
                                            <span class="material-symbols-outlined fs-5">lock_reset</span>
                                        </span>
                                        <input type="password" id="reset-confirm-pwd" class="form-control border-end-0" name="confirm_password" placeholder="Nhập lại mật khẩu mới" minlength="8" required>
                                        <button class="input-group-text bg-surface-container-low border-start-0" type="button" onclick="togglePasswordVisibility('reset-confirm-pwd', 'reset-confirm-pwd-icon')">
                                            <span class="material-symbols-outlined fs-5" id="reset-confirm-pwd-icon">visibility</span>
                                        </button>
                                    </div>
                                </div>

                                <button type="submit" class="btn btn-primary-custom w-100 mb-2">
                                    <span>Cập nhật mật khẩu</span>
                                    <span class="material-symbols-outlined">save</span>
                                </button>
                            </form>
                        </div>

                        <!-- TAB 3: FORGOT PASSWORD FORM -->
                        <div class="tab-pane fade <%= "forgot".equals(activeTab) ? "show active" : "" %>" id="forgot-pane" role="tabpanel" tabindex="0">
                            <div class="mb-3">
                                <h2 class="fs-4 fw-semibold mb-1">Khôi phục quyền truy cập</h2>
                                <p class="text-secondary-custom fs-6 mb-0" style="font-size: 0.75rem !important;">Hệ thống sẽ gửi liên kết xác minh để đặt lại mật khẩu</p>
                            </div>

                            <form onsubmit="event.preventDefault(); alert('Tính năng đang phát triển');">
                                <div class="mb-3">
                                    <label class="form-label">Email</label>
                                    <div class="input-group">
                                        <span class="input-group-text">
                                            <span class="material-symbols-outlined fs-5">mail</span>
                                        </span>
                                        <input type="email" class="form-control" placeholder="Nhập email của bạn" required>
                                    </div>
                                </div>

                                <div class="p-3 mb-4 rounded d-flex align-items-start gap-2" style="background-color: var(--surface-container-low); font-size: 0.75rem; color: var(--secondary);">
                                    <span class="material-symbols-outlined text-primary-custom fs-5">info</span>
                                    <span>Đối với tài khoản Quản trị (Admin), vui lòng liên hệ bộ phận IT qua hotline 1900 6868 nếu không nhận được OTP.</span>
                                </div>

                                <button type="submit" class="btn btn-primary-custom w-100 mb-2">
                                    <span>Gửi mã khôi phục</span>
                                    <span class="material-symbols-outlined">send</span>
                                </button>

                                <button type="button" class="btn btn-social" onclick="document.getElementById('login-tab').click();">
                                    <span class="material-symbols-outlined fs-5">arrow_back</span>
                                    <span>Quay lại Đăng nhập</span>
                                </button>
                            </form>
                        </div>
                    </div>

                    <!-- Operational Security Badge Footer -->
                    <div class="mt-4 pt-3 d-flex justify-content-between align-items-center text-secondary-custom border-top" style="font-size: 0.6875rem;">
                        <div class="d-flex align-items-center gap-1">
                            <span class="material-symbols-outlined fs-6">verified_user</span>
                            <span>Bảo mật mã hóa 256-bit SSL</span>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <a href="#" class="text-secondary-custom">Hỗ trợ</a>
                            <span>•</span>
                            <a href="#" class="text-secondary-custom">Trợ giúp POS</a>
                        </div>
                    </div>

                </div>
            </div>
        </div>

        <!-- Bootstrap 5 JS Bundle -->
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
        <script>
                                    function togglePasswordVisibility(inputId, iconId) {
                                        const input = document.getElementById(inputId);
                                        const icon = document.getElementById(iconId);
                                        if (input.type === 'password') {
                                            input.type = 'text';
                                            icon.textContent = 'visibility_off';
                                        } else {
                                            input.type = 'password';
                                            icon.textContent = 'visibility';
                                        }
                                    }

                                    document.addEventListener("DOMContentLoaded", function () {
                                        const resetPassword = document.getElementById("reset-pwd");
                                        const resetConfirm = document.getElementById("reset-confirm-pwd");
                                        if (resetPassword && resetConfirm) {
                                            function validateResetPassword() {
                                                if (resetPassword.value != resetConfirm.value) {
                                                    resetConfirm.setCustomValidity("Mật khẩu xác nhận không khớp");
                                                } else {
                                                    resetConfirm.setCustomValidity('');
                                                }
                                            }
                                            resetPassword.addEventListener("change", validateResetPassword);
                                            resetConfirm.addEventListener("keyup", validateResetPassword);
                                        }

                                        // Xử lý bộ đếm ngược gửi lại OTP
                                        let loginRemaining = <%= otpRemainingTime %>;
                                        if (loginRemaining > 0) {
                                            const countdownSpan = document.getElementById("login-countdown-sec");
                                            const timerWrapper = document.getElementById("login-resend-timer");
                                            const resendBtn = document.getElementById("login-resend-btn");

                                            const interval = setInterval(() => {
                                                loginRemaining--;
                                                if (loginRemaining <= 0) {
                                                    clearInterval(interval);
                                                    if (timerWrapper)
                                                        timerWrapper.style.display = "none";
                                                    if (resendBtn)
                                                        resendBtn.style.display = "inline";
                                                } else {
                                                    if (countdownSpan)
                                                        countdownSpan.innerText = loginRemaining;
                                                }
                                            }, 1000);
                                        }
                                    });
        </script>
    </body>
</html>
