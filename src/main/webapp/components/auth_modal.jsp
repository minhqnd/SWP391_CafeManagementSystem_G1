<%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <% /* Xy ly activeTab tu session/request */ String activeTab=request.getParameter("tab"); if (activeTab==null) {
        activeTab=(String) request.getAttribute("activeTab"); } if (activeTab==null) { activeTab="login" ; } boolean
        isLoginTab="login" .equals(activeTab); boolean isRegisterTab="register" .equals(activeTab); boolean
        isForgotTab="forgot" .equals(activeTab);
        /* Trang cần quay lại sau khi đăng nhập / đăng ký (VD: /checkout), do Controller đặt sẵn */
        String authRedirect=(String) request.getAttribute("authRedirect"); %>
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
                            class="col-12 col-md-5 modal-auth-left position-relative d-flex flex-column justify-content-between p-4">
                            <div class="position-absolute w-100 h-100 top-0 start-0"
                                style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuAGFUqokLpPQhYrL16Lx1MmWKuiDJT-ODG6DB0BHusP12uZytq7EXIlX8NPaQjkCjGB962XBMQtan6ezZNms1rtoX6miKboAymKvO9ofSFSV355L6uD3GniaPpqO7EkrnrQHFympfYpI2XS2W_2wxEJHvbcy2DOo0Ycop5LZ7iuoWV3pcYpESD0Eo_0xCKo_FdIzK8HVEvMlyK6s2Vnxxnjxl9kVFsG3tT8-M5dD_clWwQiVacYM5ot'); background-size: cover; background-position: center; opacity: 0.3; mix-blend-mode: multiply;">
                            </div>
                            <div class="position-absolute rounded-circle"
                                style="width: 14rem; height: 14rem; top: -4rem; left: -4rem; background-color: rgba(144, 11, 26, 0.1); filter: blur(40px);">
                            </div>

                            <div class="position-relative z-1">
                                <div
                                    class="d-inline-flex align-items-center gap-2 rounded-pill px-3 py-1 shadow-sm glass-badge">
                                    <span class="pulse-dot"></span>Hệ Thống Vận Hành 2.4
                                </div>
                            </div>

                            <div class="position-relative z-1 my-auto py-4">
                                <div class="d-flex align-items-center gap-2 mb-2">
                                    <div class="rounded-3 d-flex align-items-center justify-content-center shadow-sm"
                                        style="width: 40px; height: 40px; background-color: var(--primary-container); color: white;">
                                        <span class="material-symbols-outlined">local_cafe</span>
                                    </div>
                                    <div>
                                        <h1 class="m-0 fw-bold fs-3 text-dark">Aura <span
                                                style="color: var(--primary-container);">Café</span></h1>
                                        <p class="m-0 text-muted" style="font-size: 0.75rem;">Crafting Digital Moments
                                        </p>
                                    </div>
                                </div>
                                <p class="text-muted mt-3 d-none d-md-block" style="font-size: 0.85rem;">
                                    Nền tảng quản trị đồng bộ đa vai trò: Thu ngân, Pha chế KDS, Shipper và Quản lý
                                    chuỗi chi nhánh thời gian thực.
                                </p>
                            </div>

                            <div class="position-relative z-1 d-flex justify-content-between text-muted"
                                style="font-size: 0.7rem;">
                                <span class="text-uppercase fw-semibold">Chi nhánh Sài Gòn • Hà Nội</span>
                                <span class="fw-medium">v2.4.9-PRO</span>
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

                                                <ul class="nav nav-pills-custom nav-justified" id="modalAuthTab">
                                                    <li class="nav-item">
                                                        <button class="nav-link <%= isLoginTab ? " active" : "" %>"
                                                            data-bs-toggle="pill"
                                                            data-bs-target="#modal-login-pane">Đăng nhập</button>
                                                    </li>
                                                    <li class="nav-item">
                                                        <button class="nav-link <%= isRegisterTab ? " active" : "" %>"
                                                            data-bs-toggle="pill"
                                                            data-bs-target="#modal-register-pane">Đăng ký</button>
                                                    </li>
                                                    <li class="nav-item">
                                                        <button class="nav-link <%= isForgotTab ? " active" : "" %>"
                                                            data-bs-toggle="pill"
                                                            data-bs-target="#modal-forgot-pane">Quên mật khẩu</button>
                                                    </li>
                                                </ul>

                                                <div class="tab-content">
                                                    <!-- LOGIN -->
                                                    <div class="tab-pane fade <%= isLoginTab ? " show active" : "" %>"
                                                        id="modal-login-pane">
                                                        <div class="mb-3">
                                                            <h2 class="fs-4 fw-semibold mb-1">Chào mừng trở lại</h2>
                                                            <p class="text-muted" style="font-size: 0.85rem;">Vui lòng
                                                                nhập định danh tài khoản</p>
                                                        </div>
                                                        <form action="${pageContext.request.contextPath}/login"
                                                            method="POST">
                                                            <% if (authRedirect != null) { %>
                                                                <input type="hidden" name="redirect" value="<%= authRedirect %>">
                                                            <% } %>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium"
                                                                    style="font-size: 0.85rem;">Số điện thoại /
                                                                    Email</label>
                                                                <input type="text"
                                                                    class="form-control bg-light border-0 p-2"
                                                                    name="username"
                                                                    placeholder="barista.nguyen@auracafe.vn" required>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium"
                                                                    style="font-size: 0.85rem;">Mật khẩu</label>
                                                                <input type="password"
                                                                    class="form-control bg-light border-0 p-2"
                                                                    name="password" placeholder="••••••••••••" required>
                                                            </div>
                                                            <button type="submit"
                                                                class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-4"
                                                                style="background-color: var(--primary-container); height: 48px;">
                                                                Đăng nhập hệ thống <span
                                                                    class="material-symbols-outlined fs-5">arrow_forward</span>
                                                            </button>
                                                        </form>
                                                    </div>

                                                    <!-- REGISTER -->
                                                    <div class="tab-pane fade <%= isRegisterTab ? " show active" : "" %>
                                                        " id="modal-register-pane">
                                                        <div class="mb-3">
                                                            <h2 class="fs-4 fw-semibold mb-1">Tạo tài khoản Aura</h2>
                                                            <p class="text-muted" style="font-size: 0.85rem;">Đăng ký
                                                                tài khoản nội bộ chi nhánh hoặc đối tác</p>
                                                        </div>
                                                        <form action="${pageContext.request.contextPath}/register"
                                                            method="POST">
                                                            <% if (authRedirect != null) { %>
                                                                <input type="hidden" name="redirect" value="<%= authRedirect %>">
                                                            <% } %>
                                                            <div class="row g-2 mb-3">
                                                                <div class="col-12">
                                                                    <label class="form-label fw-medium"
                                                                        style="font-size: 0.85rem;">Họ tên</label>
                                                                    <input type="text"
                                                                        class="form-control bg-light border-0 p-2"
                                                                        name="fullname" placeholder="Nguyễn Văn A"
                                                                        required>
                                                                </div>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium"
                                                                    style="font-size: 0.85rem;">SĐT / Email</label>
                                                                <input type="text"
                                                                    class="form-control bg-light border-0 p-2"
                                                                    name="username" placeholder="SĐT hoặc Email"
                                                                    required>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium"
                                                                    style="font-size: 0.85rem;">Mật khẩu</label>
                                                                <input type="password"
                                                                    class="form-control bg-light border-0 p-2"
                                                                    name="password" placeholder="Tối thiểu 8 ký tự"
                                                                    required>
                                                            </div>
                                                            <button type="submit"
                                                                class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-4"
                                                                style="background-color: var(--primary-container); height: 48px;">
                                                                Tạo tài khoản mới <span
                                                                    class="material-symbols-outlined fs-5">how_to_reg</span>
                                                            </button>
                                                        </form>
                                                    </div>

                                                    <!-- FORGOT PWD -->
                                                    <div class="tab-pane fade <%= isForgotTab ? " show active" : "" %>"
                                                        id="modal-forgot-pane">
                                                        <div class="mb-3">
                                                            <h2 class="fs-4 fw-semibold mb-1">Khôi phục quyền truy cập
                                                            </h2>
                                                            <p class="text-muted" style="font-size: 0.85rem;">Hệ thống
                                                                sẽ cấp lại mật khẩu ngẫu nhiên qua Email của bạn</p>
                                                        </div>
                                                        <form
                                                            action="${pageContext.request.contextPath}/forgot-password"
                                                            method="POST">
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium"
                                                                    style="font-size: 0.85rem;">Email đã liên
                                                                    kết</label>
                                                                <input type="email" name="email"
                                                                    class="form-control bg-light border-0 p-2"
                                                                    placeholder="name@auracafe.vn" required>
                                                            </div>

                                                            <div class="alert alert-info d-flex gap-2 p-3 mt-3"
                                                                style="font-size: 0.8rem; line-height: 1.5;">
                                                                <span class="material-symbols-outlined text-info"
                                                                    style="font-size: 1.25rem;">info</span>
                                                                <div>
                                                                    <strong>Lưu ý:</strong> Đối với tài khoản đăng ký
                                                                    bằng <strong>Số điện thoại</strong> hoặc tài khoản
                                                                    <strong>Quản trị (Admin)</strong>, vui lòng liên hệ
                                                                    bộ phận CSKH qua Hotline <strong>1900 6868</strong>
                                                                    hoặc trực tiếp tại chi nhánh để được hỗ trợ cấp lại
                                                                    mật khẩu.
                                                                </div>
                                                            </div>

                                                            <button type="submit"
                                                                class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-4"
                                                                style="background-color: var(--primary-container); height: 48px;">
                                                                Gửi mật khẩu mới <span
                                                                    class="material-symbols-outlined fs-5">send</span>
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
        </script>