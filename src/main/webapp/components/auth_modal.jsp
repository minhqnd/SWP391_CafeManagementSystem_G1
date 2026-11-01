<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    /* Xử lý activeTab từ session hoặc request */
    String activeTab = request.getParameter("tab");
    if (activeTab == null) {
        activeTab = (String) request.getAttribute("activeTab");
    }
    if (activeTab == null) {
        activeTab = "login";
    }
    boolean isRegisterTab = "register".equals(activeTab);
    boolean isForgotTab = "forgot".equals(activeTab);
    boolean isOtpTab = "otp".equals(activeTab);
    boolean isResetTab = "reset_password".equals(activeTab);
    boolean isLoginTab = !isRegisterTab && !isOtpTab && !isResetTab; // Quên mật khẩu cũng hiển thị dưới tab Đăng nhập
    
    /* Tính thời gian đếm ngược gửi lại OTP */
    Long lastOtpSentTime = (Long) session.getAttribute("lastOtpSentTime");
    long otpRemainingTime = 0;
    if (lastOtpSentTime != null) {
        long elapsed = System.currentTimeMillis() - lastOtpSentTime;
        if (elapsed < 60000) {
            otpRemainingTime = (60000 - elapsed) / 1000;
        }
    }
    /* Trang cần quay lại sau khi đăng nhập / đăng ký (VD: /checkout) */
    String authRedirect = (String) request.getAttribute("authRedirect");
%>
        <style>
            :root {
                --surface-container-lowest: #ffffff;
                --surface-container-low: #eff4ff;
                --surface-container: #e6eeff;
                --surface-container-high: #dee9fc;
                --secondary: #555f71;
                --primary: #900b1a;
                --primary-container: #b2292e;
            }

            .modal-auth-left {
                background-color: var(--surface-container);
                min-height: 500px;
            }

            .glass-badge {
                background: rgba(255, 255, 255, 0.8);
                backdrop-filter: blur(4px);
                font-size: 0.6875rem;
                font-weight: 600;
                color: var(--primary);
                text-transform: uppercase;
                letter-spacing: 0.1em;
            }

            .pulse-dot {
                width: 8px;
                height: 8px;
                background-color: var(--primary-container);
                border-radius: 50%;
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

            .nav-pills-custom .nav-link.active {
                background-color: var(--surface-container-lowest);
                color: #121c2a;
                box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
            }
        </style>

        <!-- Auth Modal -->
        <div class="modal fade" id="authModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered" style="max-width: 900px;">
                <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden"
                    style="background-color: var(--surface-container-lowest);">

                    <button type="button" class="btn-close position-absolute top-0 end-0 m-3 z-3"
                        data-bs-dismiss="modal"
                        style="background-color: white; padding: 0.5rem; opacity: 0.8;"></button>

                    <div class="row g-0">
                        <div
                            class="col-12 col-md-5 modal-auth-left position-relative d-flex flex-column justify-content-center p-4 p-md-5">
                            <div class="position-absolute w-100 h-100 top-0 start-0"
                                style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuAGFUqokLpPQhYrL16Lx1MmWKuiDJT-ODG6DB0BHusP12uZytq7EXIlX8NPaQjkCjGB962XBMQtan6ezZNms1rtoX6miKboAymKvO9ofSFSV355L6uD3GniaPpqO7EkrnrQHFympfYpI2XS2W_2wxEJHvbcy2DOo0Ycop5LZ7iuoWV3pcYpESD0Eo_0xCKo_FdIzK8HVEvMlyK6s2Vnxxnjxl9kVFsG3tT8-M5dD_clWwQiVacYM5ot'); background-size: cover; background-position: center; opacity: 0.3; mix-blend-mode: multiply;">
                            </div>
                            <div class="position-absolute rounded-circle"
                                style="width: 14rem; height: 14rem; top: -4rem; left: -4rem; background-color: rgba(144, 11, 26, 0.1); filter: blur(40px);">
                            </div>

                            <div class="position-relative z-1 py-3">
                                <div class="d-flex align-items-center gap-2 mb-2">
                                    <div class="rounded-3 d-flex align-items-center justify-content-center shadow-sm"
                                        style="width: 42px; height: 42px; background-color: var(--primary-container); color: white;">
                                        <span class="material-symbols-outlined">local_cafe</span>
                                    </div>
                                    <div>
                                        <h1 class="m-0 fw-bold fs-3 text-dark">Aura <span
                                                style="color: var(--primary-container);">Café</span></h1>
                                        <p class="m-0 text-muted" style="font-size: 0.75rem; letter-spacing: 0.05em; text-transform: uppercase; font-weight: 600;">Specialty Coffee</p>
                                    </div>
                                </div>
                                <p class="text-muted mt-3 mb-0 d-none d-md-block" style="font-size: 0.875rem; line-height: 1.6;">
                                    Khám phá không gian cà phê nghệ thuật và hương vị nguyên bản từ những hạt cà phê chọn lọc. Đăng ký thành viên để nhận ưu đãi và tích luỹ điểm thưởng trên mỗi đơn hàng.
                                </p>
                            </div>
                        </div>

                        <div class="col-12 col-md-7 p-4 p-md-5 d-flex flex-column justify-content-center">

                            <% /* Get attributes safely */ String sessionSuccess=(String)
                                session.getAttribute("successMsg"); String sessionError=(String)
                                session.getAttribute("errorMsg"); if (sessionSuccess !=null) {
                                session.removeAttribute("successMsg"); } if (sessionError !=null) {
                                session.removeAttribute("errorMsg"); } String reqError=(String)
                                request.getAttribute("error"); String reqSuccess=(String)
                                request.getAttribute("success"); String displayError=(reqError !=null) ? reqError :
                                sessionError; String displaySuccess=(reqSuccess !=null) ? reqSuccess : sessionSuccess;
                                %>

                                <% if (displayError !=null) { %>
                                    <div class="alert alert-danger p-2 mb-3 text-center" style="font-size: 0.875rem;">
                                        <%= displayError %>
                                    </div>
                                    <% } %>
                                        <% if (displaySuccess !=null) { %>
                                            <div class="alert alert-success p-2 mb-3 text-center"
                                                style="font-size: 0.875rem;">
                                                <%= displaySuccess %>
                                            </div>
                                            <% } %>

                                                <% if (!isOtpTab && !isResetTab) { %>
                                                <ul class="nav nav-pills-custom nav-justified" id="modalAuthTab">
                                                    <li class="nav-item">
                                                        <button class="nav-link <%= isLoginTab ? " active" : "" %>"
                                                            data-bs-toggle="pill"
                                                            data-bs-target="#modal-login-pane"
                                                            id="tab-btn-login"
                                                            onclick="showLoginSubView()">Đăng nhập</button>
                                                    </li>
                                                    <li class="nav-item">
                                                        <button class="nav-link <%= isRegisterTab ? " active" : "" %>"
                                                            data-bs-toggle="pill"
                                                            data-bs-target="#modal-register-pane"
                                                            id="tab-btn-register">Đăng ký</button>
                                                    </li>
                                                </ul>
                                                <% } %>

                                                <div class="tab-content">
                                                    <!-- LOGIN & FORGOT PANE -->
                                                    <div class="tab-pane fade <%= isLoginTab ? " show active" : "" %>"
                                                        id="modal-login-pane">

                                                        <!-- SUBVIEW 1: ĐĂNG NHẬP -->
                                                        <div id="subview-login" style="<%= isForgotTab ? "display: none;" : "" %>">
                                                            <div class="mb-3">
                                                                <h2 class="fs-4 fw-semibold mb-1">Chào mừng trở lại</h2>
                                                                <p class="text-muted" style="font-size: 0.85rem;">Vui lòng nhập định danh tài khoản</p>
                                                            </div>
                                                            <form action="${pageContext.request.contextPath}/login" method="POST">
                                                                <% if (authRedirect !=null) { %>
                                                                    <input type="hidden" name="redirect" value="<%= authRedirect %>">
                                                                <% } %>
                                                                <div class="mb-3">
                                                                    <label class="form-label fw-medium" style="font-size: 0.85rem;">Email</label>
                                                                    <input type="text"
                                                                        class="form-control bg-light border-0 p-2"
                                                                        name="username"
                                                                        placeholder="email@domain.com"
                                                                        required>
                                                                </div>
                                                                <div class="mb-2">
                                                                    <label class="form-label fw-medium" style="font-size: 0.85rem;">Mật khẩu</label>
                                                                    <input type="password"
                                                                        class="form-control bg-light border-0 p-2"
                                                                        name="password" placeholder="••••••••••••"
                                                                        minlength="6" required>
                                                                </div>
                                                                <div class="d-flex justify-content-end mb-3">
                                                                    <a href="javascript:void(0)" onclick="showForgotSubView()"
                                                                        class="text-decoration-none text-muted"
                                                                        style="font-size: 0.825rem;">
                                                                        Quên mật khẩu?
                                                                    </a>
                                                                </div>
                                                                <button type="submit"
                                                                    class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-2"
                                                                    style="background-color: var(--primary-container); height: 48px;">
                                                                    Đăng nhập <span class="material-symbols-outlined fs-5">arrow_forward</span>
                                                                </button>
                                                            </form>
                                                        </div>

                                                        <!-- SUBVIEW 2: QUÊN MẬT KHẨU -->
                                                        <div id="subview-forgot" style="<%= isForgotTab ? "" : "display: none;" %>">
                                                            <div class="mb-3">
                                                                <h2 class="fs-4 fw-semibold mb-1">Khôi phục quyền truy cập</h2>
                                                                <p class="text-muted" style="font-size: 0.85rem;">Hệ thống sẽ cấp lại mật khẩu ngẫu nhiên qua Email của bạn</p>
                                                            </div>
                                                            <form action="${pageContext.request.contextPath}/forgot-password" method="POST">
                                                                <div class="mb-3">
                                                                    <label class="form-label fw-medium" style="font-size: 0.85rem;">Email đã liên kết</label>
                                                                    <input type="email" name="email"
                                                                        class="form-control bg-light border-0 p-2"
                                                                        placeholder="name@auracafe.vn" required>
                                                                </div>

                                                                <div class="alert alert-info d-flex gap-2 p-3 mt-3" style="font-size: 0.8rem; line-height: 1.5;">
                                                                    <span class="material-symbols-outlined text-info" style="font-size: 1.25rem;">info</span>
                                                                    <div>
                                                                        <strong>Lưu ý:</strong> Vui lòng nhập địa chỉ Email hoặc Số điện thoại đã liên kết với tài khoản Aura để nhận mật khẩu mới.
                                                                    </div>
                                                                </div>

                                                                <button type="submit"
                                                                    class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-4"
                                                                    style="background-color: var(--primary-container); height: 48px;">
                                                                    Gửi mật khẩu mới <span class="material-symbols-outlined fs-5">send</span>
                                                                </button>

                                                                <div class="text-center mt-3">
                                                                    <a href="javascript:void(0)" onclick="showLoginSubView()"
                                                                        class="text-decoration-none fw-semibold d-inline-flex align-items-center gap-1"
                                                                        style="font-size: 0.85rem; color: var(--primary-container);">
                                                                        <span class="material-symbols-outlined fs-6">arrow_back</span>
                                                                        <span>Quay lại đăng nhập</span>
                                                                    </a>
                                                                </div>
                                                            </form>
                                                        </div>

                                                    </div>

                                                    <!-- REGISTER -->
                                                    <div class="tab-pane fade <%= isRegisterTab ? " show active" : "" %>" id="modal-register-pane">
                                                        <div class="mb-3">
                                                            <h2 class="fs-4 fw-semibold mb-1">Tạo tài khoản Aura</h2>
                                                            <p class="text-muted" style="font-size: 0.85rem;">Đăng ký
                                                                thành viên để nhận ưu đãi và tích điểm</p>
                                                        </div>
                                                        <form action="${pageContext.request.contextPath}/register"
                                                            method="POST">
                                                            <% if (authRedirect !=null) { %>
                                                                <input type="hidden" name="redirect"
                                                                    value="<%= authRedirect %>">
                                                                <% } %>
                                                                    <div class="row g-2 mb-3">
                                                                        <div class="col-12">
                                                                            <label class="form-label fw-medium"
                                                                                style="font-size: 0.85rem;">Họ
                                                                                tên</label>
                                                                            <input type="text"
                                                                                class="form-control bg-light border-0 p-2"
                                                                                name="fullname"
                                                                                placeholder="Nguyễn Văn A" required>
                                                                        </div>
                                                                    </div>
                                                                    <div class="mb-3">
                                                                        <label class="form-label fw-medium"
                                                                            style="font-size: 0.85rem;">Số điện thoại</label>
                                                                        <input type="tel"
                                                                            class="form-control bg-light border-0 p-2"
                                                                            name="phone" placeholder="VD: 0912345678"
                                                                            pattern="^(84|0)[3|5|7|8|9][0-9]{8}$"
                                                                            title="Vui lòng nhập số điện thoại Việt Nam hợp lệ (VD: 0912345678)"
                                                                            required>
                                                                    </div>
                                                                    <div class="mb-3">
                                                                        <label class="form-label fw-medium"
                                                                            style="font-size: 0.85rem;">Email</label>
                                                                        <input type="email"
                                                                            class="form-control bg-light border-0 p-2"
                                                                            name="email" placeholder="Email của bạn"
                                                                            required>
                                                                    </div>
                                                                    <div class="mb-3">
                                                                        <label class="form-label fw-medium"
                                                                            style="font-size: 0.85rem;">Mật khẩu</label>
                                                                        <input type="password" id="reg-password"
                                                                            class="form-control bg-light border-0 p-2"
                                                                            name="password"
                                                                            placeholder="Tối thiểu 6 ký tự"
                                                                            minlength="6" required>
                                                                    </div>
                                                                    <div class="mb-3">
                                                                        <label class="form-label fw-medium"
                                                                            style="font-size: 0.85rem;">Xác nhận mật
                                                                            khẩu</label>
                                                                        <input type="password" id="reg-confirm-password"
                                                                            class="form-control bg-light border-0 p-2"
                                                                            name="confirm_password"
                                                                            placeholder="Nhập lại mật khẩu"
                                                                            minlength="6" required>
                                                                    </div>
                                                                    <button type="submit"
                                                                        class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-4"
                                                                        style="background-color: var(--primary-container); height: 48px;">
                                                                        Tạo tài khoản mới <span
                                                                            class="material-symbols-outlined fs-5">how_to_reg</span>
                                                                    </button>
                                                        </form>
                                                    </div>

                                                    <!-- OTP VERIFICATION -->
                                                    <div class="tab-pane fade <%= isOtpTab ? " show active" : "" %>" id="modal-otp-pane">
                                                        <div class="mb-3">
                                                            <h2 class="fs-4 fw-semibold mb-1">Xác thực Email</h2>
                                                            <p class="text-muted" style="font-size: 0.85rem;">Nhập mã OTP đã được gửi đến email của bạn</p>
                                                        </div>
                                                        <form action="${pageContext.request.contextPath}/verify-otp" method="POST">
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Mã xác thực (OTP)</label>
                                                                <input type="text"
                                                                    class="form-control bg-light border-0 p-2 text-center fw-bold fs-4"
                                                                    name="otp"
                                                                    placeholder="------"
                                                                    maxlength="6"
                                                                    autocomplete="off"
                                                                    required>
                                                            </div>
                                                            <div class="alert alert-info d-flex gap-2 p-3 mt-3" style="font-size: 0.8rem; line-height: 1.5;">
                                                                <span class="material-symbols-outlined text-info" style="font-size: 1.25rem;">info</span>
                                                                <div>
                                                                    Vui lòng kiểm tra hòm thư đến (hoặc thư rác) để lấy mã xác thực gồm 6 chữ số.
                                                                </div>
                                                            </div>
                                                            <button type="submit"
                                                                class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-4"
                                                                style="background-color: var(--primary-container); height: 48px;">
                                                                Xác nhận OTP <span class="material-symbols-outlined fs-5">check_circle</span>
                                                            </button>
                                                            <div class="d-flex justify-content-between align-items-center mt-3">
                                                                <a href="${pageContext.request.contextPath}/?tab=forgot"
                                                                    class="text-decoration-none text-muted d-inline-flex align-items-center gap-1"
                                                                    style="font-size: 0.85rem;">
                                                                    <span class="material-symbols-outlined fs-6">arrow_back</span>
                                                                    <span>Quay lại</span>
                                                                </a>
                                                                <div>
                                                                    <span id="modal-resend-timer" style="font-size: 0.85rem; color: var(--secondary); <%= otpRemainingTime > 0 ? "" : "display: none;" %>">
                                                                        Gửi lại sau <strong id="modal-countdown-sec"><%= otpRemainingTime %></strong>s
                                                                    </span>
                                                                    <a href="${pageContext.request.contextPath}/resend-otp"
                                                                        id="modal-resend-btn"
                                                                        class="text-decoration-none fw-semibold"
                                                                        style="font-size: 0.85rem; color: var(--primary-container); <%= otpRemainingTime > 0 ? "display: none;" : "" %>">
                                                                        Gửi lại mã OTP
                                                                    </a>
                                                                </div>
                                                            </div>
                                                        </form>
                                                    </div>

                                                    <!-- RESET PASSWORD -->
                                                    <div class="tab-pane fade <%= isResetTab ? " show active" : "" %>" id="modal-reset-pane">
                                                        <div class="mb-3">
                                                            <h2 class="fs-4 fw-semibold mb-1">Thiết lập mật khẩu mới</h2>
                                                            <p class="text-muted" style="font-size: 0.85rem;">Vui lòng tạo mật khẩu mới cho tài khoản của bạn</p>
                                                        </div>
                                                        <form action="${pageContext.request.contextPath}/reset-password" method="POST">
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Mật khẩu mới</label>
                                                                <input type="password" id="reset-password-input"
                                                                    class="form-control bg-light border-0 p-2"
                                                                    name="password"
                                                                    placeholder="Tối thiểu 6 ký tự"
                                                                    minlength="6" required>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Xác nhận mật khẩu</label>
                                                                <input type="password" id="reset-confirm-password-input"
                                                                    class="form-control bg-light border-0 p-2"
                                                                    name="confirm_password"
                                                                    placeholder="Nhập lại mật khẩu mới"
                                                                    minlength="6" required>
                                                            </div>
                                                            <button type="submit"
                                                                class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-4"
                                                                style="background-color: var(--primary-container); height: 48px;">
                                                                Cập nhật mật khẩu <span class="material-symbols-outlined fs-5">save</span>
                                                            </button>
                                                        </form>
                                                    </div>

                                                </div>
                        </div>
                    </div>

                </div>
            </div>
        </div>

        <script>
            function showLoginSubView() {
                var loginView = document.getElementById('subview-login');
                var forgotView = document.getElementById('subview-forgot');
                if (loginView) loginView.style.display = 'block';
                if (forgotView) forgotView.style.display = 'none';
            }

            function showForgotSubView() {
                var loginView = document.getElementById('subview-login');
                var forgotView = document.getElementById('subview-forgot');
                if (loginView) loginView.style.display = 'none';
                if (forgotView) forgotView.style.display = 'block';
            }

            // Tự động bật Modal nếu có lỗi, thành công, hoặc có param tab trên URL
            document.addEventListener("DOMContentLoaded", function () {
                var hasErrorOrSuccess = <%= (displayError != null || displaySuccess != null || request.getParameter("tab") != null) %>;
                if (hasErrorOrSuccess) {
                    var myModal = new bootstrap.Modal(document.getElementById('authModal'), {
                        keyboard: true
                    });
                    myModal.show();
                }
            });

            // Validate mật khẩu khớp nhau khi đăng ký
            const regPassword = document.getElementById("reg-password");
            const regConfirm = document.getElementById("reg-confirm-password");
            if (regPassword && regConfirm) {
                function validatePassword() {
                    if (regPassword.value != regConfirm.value) {
                        regConfirm.setCustomValidity("Mật khẩu xác nhận không khớp");
                    } else {
                        regConfirm.setCustomValidity('');
                    }
                }
                regPassword.addEventListener("change", validatePassword);
                regConfirm.addEventListener("keyup", validatePassword);
            }

            const resetPassword = document.getElementById("reset-password-input");
            const resetConfirm = document.getElementById("reset-confirm-password-input");
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
            let modalRemaining = <%= otpRemainingTime %>;
            if (modalRemaining > 0) {
                const countdownSpan = document.getElementById("modal-countdown-sec");
                const timerWrapper = document.getElementById("modal-resend-timer");
                const resendBtn = document.getElementById("modal-resend-btn");
                
                const interval = setInterval(() => {
                    modalRemaining--;
                    if (modalRemaining <= 0) {
                        clearInterval(interval);
                        if (timerWrapper) timerWrapper.style.display = "none";
                        if (resendBtn) resendBtn.style.display = "inline";
                    } else {
                        if (countdownSpan) countdownSpan.innerText = modalRemaining;
                    }
                }, 1000);
            }
        </script>
