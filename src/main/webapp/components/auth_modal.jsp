
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // . Lấy token bảo vệ form và đọc tab do URL/Controller yêu cầu.
    // .. Tab quyết định hiển thị đăng ký, đăng nhập, OTP hoặc đặt mật khẩu mới.
    // ... AccountCsrf.token(); lỗi trả về qua payload payload trong account_feedback.jsp.
    String accountCsrf = utils.AccountCsrf.token(session);
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
                                 boolean authModalAutoOpen = displayError != null || displaySuccess != null || request.getParameter("tab") != null;
                                %>

                                <% if (displayError !=null) { %>
                                    <div class="alert alert-danger p-2 mb-3 text-center" style="font-size: 0.875rem;">
                                        <%= displayError %>
                                    </div>
                                    <% } %>
                                        <%-- RG11: [SLBF10] Show registration success message
                                             . Hiển thị thông báo đăng ký thành công từ successMsg.
                                             .. Vùng này cũng dùng chung cho các thông báo xác thực do Controller đặt.
                                             ... Người dùng tiếp tục chọn Đăng nhập [LG01]. --%>
                                        <% if (displaySuccess !=null) { %>
                                            <div class="alert alert-success p-2 mb-3 text-center"
                                                style="font-size: 0.875rem;">
                                                <%= displaySuccess %>
                                            </div>
                                            <% } %>

                                                <% if (!isOtpTab && !isResetTab) { %>
                                                <ul class="nav nav-pills-custom nav-justified" id="modalAuthTab">
                                                    <li class="nav-item">
                                                        <%-- LG01: [SLBF10] Select "Login"
                                                             . Nút Đăng nhập chọn modal-login-pane; showLoginSubView() mở đúng phần đăng nhập.
                                                             .. Chỉ đổi phần hiển thị, chưa xác thực tài khoản.
                                                             ... Form [LG02], nút gửi [LG03]. --%>
                                                        <button class="nav-link <%= isLoginTab ? " active" : "" %>"
                                                            data-bs-toggle="pill"
                                                            data-bs-target="#modal-login-pane"
                                                            id="tab-btn-login"
                                                            onclick="showLoginSubView()">Đăng nhập</button>
                                                    </li>
                                                    <li class="nav-item">
                                                        <%-- RG01: [SLBF10] Select "Register"
                                                             . Nút Đăng ký chọn modal-register-pane bằng Bootstrap.
                                                             .. Chỉ đổi tab, chưa gửi dữ liệu hoặc tạo tài khoản.
                                                             ... Form đăng ký [RG02]; nút Tạo tài khoản mới [RG03]. --%>
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
                                                        <div id="subview-login" class="<%= isForgotTab ? "d-none" : "" %>">
                                                            <div class="mb-3">
                                                                <h2 class="fs-4 fw-semibold mb-1">Chào mừng trở lại</h2>
                                                                <p class="text-muted" style="font-size: 0.85rem;">Vui lòng nhập định danh tài khoản</p>
                                                            </div>
                                                            
                                                            
                                                            <%-- LG02: [SLBF10] Enter email and password
                                                                 . Hiển thị form đăng nhập, gồm username là email/SĐT và password.
                                                                 .. Nhập dữ liệu chưa tạo phiên đăng nhập.
                                                                 ... Nút Đăng nhập [LG03] bắt đầu gửi thông tin.
                                                                 OT19: . Sau khi đặt lại mật khẩu thành công, cùng form này được mở để đăng nhập lại.
                                                                 ... Nhập thông tin mới rồi bấm nút đăng nhập [LG03]. --%>
                                                            <form action="${pageContext.request.contextPath}/login" method="POST">
                                                            <input type="hidden" name="csrfToken" value="<%= accountCsrf %>">
                                                                <% if (authRedirect !=null) { %>
                                                                    <input type="hidden" name="redirect" value="<%= authRedirect %>">
                                                                <% } %>
                                                                <div class="mb-3">
                                                                    <label for="modal-login-username" class="form-label fw-medium" style="font-size: 0.85rem;">Email hoặc số điện thoại</label>
                                                                    
                                                                    <%-- LG02a: . Ô định danh nhận name=username là email hoặc số điện thoại.
                                                                         .. Bắt buộc nhập; Controller bỏ khoảng trắng đầu/cuối.
                                                                         ... validate() trong account-validation.js → LoginController [LG05a] → UserDAO.login() [LG06]. --%>
                                                                    <input type="text"
                                                                        id="modal-login-username"
                                                                        class="form-control bg-light border-0 p-2"
                                                                        name="username"
                                                                        placeholder="Nhập email hoặc số điện thoại"
                                                                        autocomplete="username"
                                                                        required>
                                                                </div>
                                                                <div class="mb-2">
                                                                    <label class="form-label fw-medium" style="font-size: 0.85rem;">Mật khẩu</label>
                                                                    
                                                                    <%-- LG02b: . Ô mật khẩu nhận name=password; giữ nguyên ký tự để đối chiếu dữ liệu đã lưu.
                                                                         .. JavaScript kiểm tra bắt buộc và giới hạn trường; quy tắc tạo mật khẩu mới không áp ở form login.
                                                                         ... LoginController [LG05b] → UserDAO.login() [LG06] → PasswordHasher.verify(). --%>
                                                                    <input type="password"
                                                                        class="form-control bg-light border-0 p-2"
                                                                        name="password" maxlength="255" placeholder="••••••••••••"
                                                                        minlength="8" required>
                                                                </div>
                                                                <div class="d-flex justify-content-end mb-3">
                                                                    <a href="javascript:void(0)" onclick="showForgotSubView()"
                                                                        class="text-decoration-none text-muted"
                                                                        style="font-size: 0.825rem;">
                                                                        Quên mật khẩu?
                                                                    </a>
                                                                </div>
                                                                
                                                                <%-- LG03: [SLBF10] Click "Login"
                                                                     . Bấm Đăng nhập để gửi thông tin từ form [LG02].
                                                                     .. Trình duyệt kiểm tra trước, server đối chiếu tài khoản sau khi nhận POST.
                                                                     ... account-validation.js [LG04] → LoginController.doPost() [LG05]. --%>
                                                                <button type="submit"
                                                                    class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-2"
                                                                    style="background-color: var(--primary-container); height: 48px;">
                                                                    Đăng nhập <span class="material-symbols-outlined fs-5">arrow_forward</span>
                                                                </button>
                                                            </form>
                                                        </div>

                                                        <!-- SUBVIEW 2: QUÊN MẬT KHẨU -->
                                                        <div id="subview-forgot" class="<%= isForgotTab ? "" : "d-none" %>">
                                                            <div class="mb-3">
                                                                <h2 class="fs-4 fw-semibold mb-1">Khôi phục quyền truy cập</h2>
                                                                <p class="text-muted" style="font-size: 0.85rem;">Hệ thống gửi mã OTP qua email để bạn đặt mật khẩu mới</p>
                                                            </div>
                                                            
                                                            
                                                            <%-- OT02: . Hiển thị form email sau khi bấm Quên mật khẩu [OT01].
                                                                 .. Nhập email chưa gửi OTP.
                                                                 ... Bấm Gửi mã OTP [OT03] để tiếp tục. --%>
                                                            <form action="${pageContext.request.contextPath}/forgot-password" method="POST">
                                                            <input type="hidden" name="csrfToken" value="<%= accountCsrf %>">
                                                                <div class="mb-3">
                                                                    <label class="form-label fw-medium" style="font-size: 0.85rem;">Email đã liên kết</label>
                                                                    
                                                                    <%-- OT02a: . Ô Email khôi phục nhận name=email, yêu cầu đúng định dạng và tối đa 150 ký tự.
                                                                         ... validate() kiểm tra email trong validate() → ForgotPasswordController [OT05] → PasswordRecoveryService.start() [OT06]. --%>
                                                                    <input type="email" name="email" maxlength="150"
                                                                        class="form-control bg-light border-0 p-2"
                                                                        placeholder="name@auracafe.vn" required>
                                                                </div>

                                                                <div class="alert alert-info d-flex gap-2 p-3 mt-3" style="font-size: 0.8rem; line-height: 1.5;">
                                                                    <span class="material-symbols-outlined text-info" style="font-size: 1.25rem;">info</span>
                                                                    <div>
                                                                        <strong>Lưu ý:</strong> Vui lòng nhập email đã liên kết với tài khoản Aura. Mã OTP có hiệu lực 5 phút.
                                                                    </div>
                                                                </div>

                                                                
                                                                <%-- OT03: . Bấm Gửi mã OTP để submit email từ form [OT02].
                                                                     ... account-validation.js, sự kiện submit [OT04] kiểm tra trước khi gửi. --%>
                                                                <button type="submit"
                                                                    class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-4"
                                                                    style="background-color: var(--primary-container); height: 48px;">
                                                                    Gửi mã OTP <span class="material-symbols-outlined fs-5">send</span>
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
                                                            <p class="text-muted" style="font-size: 0.85rem;">Đăng ký thành viên để nhận ưu đãi và tích điểm</p>
                                                        </div>

                                                        <%-- RG02: [SLBF10] Enter registration information
                                                             . Hiển thị form đăng ký với fullname, phone, email, password và confirm_password.
                                                             .. Dữ liệu mới nhập chưa được lưu vào database.
                                                             ... Nút Tạo tài khoản mới [RG03] → account-validation.js [RG04] → RegisterController.doPost() [RG05]. --%>
                                                        <form action="${pageContext.request.contextPath}/register" method="POST">
                                                            <input type="hidden" name="csrfToken" value="<%= accountCsrf %>">
                                                            <% if (authRedirect != null) { %>
                                                                <input type="hidden" name="redirect" value="<%= authRedirect %>">
                                                            <% } %>
                                                            <div class="row g-2 mb-3">
                                                                <div class="col-12">
                                                                    <label class="form-label fw-medium" style="font-size: 0.85rem;">Họ và tên</label>
                                                                    <%-- RG02a: . Ô Họ và tên nhận name=fullname.
                                                                         .. Bắt buộc từ 1 đến 100 ký tự sau chuẩn hóa.
                                                                         ... validate() kiểm tra họ tên trong validate() → RegisterController [RG06a] → AccountValidation.nameError(). --%>
                                                                    <input type="text" class="form-control bg-light border-0 p-2" name="fullname" placeholder="Nguyễn Văn A" required>
                                                                </div>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Số điện thoại</label>
                                                                <%-- RG02b: . Ô SĐT nhận name=phone; sau chuẩn hóa phải có 10 số và bắt đầu bằng 0.
                                                                     ... validate() kiểm tra SĐT trong validate() → RegisterController [RG06b] → AccountValidation.phoneError(); kiểm tra trùng [RG07a]. --%>
                                                                <input type="tel" class="form-control bg-light border-0 p-2" name="phone" placeholder="VD: 0912345678" pattern="0[0-9]{9}" inputmode="numeric" title="Nhập 10 chữ số và bắt đầu bằng số 0" required>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Email</label>
                                                                <%-- RG02c: . Ô Email nhận name=email; yêu cầu đúng định dạng, tối đa 150 ký tự.
                                                                     ... validate() kiểm tra email trong validate() → RegisterController [RG06c] → AccountValidation.emailError(); kiểm tra trùng [RG07b]. --%>
                                                                <input type="email" class="form-control bg-light border-0 p-2" name="email" maxlength="150" placeholder="Email của bạn" required>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Mật khẩu</label>
                                                                <%-- RG02d: . Ô Mật khẩu nhận name=password; mật khẩu mới cần 8–255 ký tự, có chữ và số.
                                                                     ... passwordError() trong account-validation.js → RegisterController [RG06d] → AccountValidation.passwordErrors(). --%>
                                                                <input type="password" id="reg-password" class="form-control bg-light border-0 p-2" name="password" maxlength="255" placeholder="Ít nhất 8 ký tự, gồm chữ và số" minlength="8" required>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Xác nhận mật khẩu</label>
                                                                <%-- RG02e: . Ô Xác nhận nhận name=confirm_password và phải khớp password.
                                                                     ... validate() kiểm tra xác nhận trong validate() → RegisterController [RG06d] → AccountValidation.passwordErrors(). --%>
                                                                <input type="password" id="reg-confirm-password" class="form-control bg-light border-0 p-2" name="confirm_password" maxlength="255" placeholder="Nhập lại mật khẩu" minlength="8" required>
                                                            </div>
                                                            <%-- RG03: [SLBF10] Click "Create Account"
                                                                 . Bấm Tạo tài khoản mới để submit form đăng ký [RG02].
                                                                 .. Có lỗi thì giữ form và cho sửa; hợp lệ mới gửi POST /register.
                                                                 ... account-validation.js [RG04] → RegisterController.doPost() [RG05]. --%>
                                                            <button type="submit" class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-4" style="background-color: var(--primary-container); height: 48px;">
                                                                Tạo tài khoản mới <span class="material-symbols-outlined fs-5">how_to_reg</span>
                                                            </button>
                                                        </form>
                                                    </div>

                                                    <!-- OTP VERIFICATION -->
                                                    <div class="tab-pane fade <%= isOtpTab ? " show active" : "" %>" id="modal-otp-pane">
                                                        <div class="mb-3">
                                                            <h2 class="fs-4 fw-semibold mb-1">Xác thực Email</h2>
                                                            <p class="text-muted" style="font-size: 0.85rem;">Nhập mã OTP đã được gửi đến email của bạn</p>
                                                        </div>
                                                        
                                                        
                                                        <%-- OT07: . Hiển thị form nhập OTP sau khi gửi mã thành công.
                                                             .. Chưa đổi mật khẩu khi mới nhập mã; có thể gửi lại theo nhánh [OT07b].
                                                             ... Bấm Xác nhận OTP [OT08] để tiếp tục luồng chính. --%>
                                                        <form action="${pageContext.request.contextPath}/verify-otp" method="POST">
                                                            <input type="hidden" name="csrfToken" value="<%= accountCsrf %>">
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Mã xác thực (OTP)</label>
                                                                
                                                                <%-- OT07a: . Ô mã nhận name=otp và yêu cầu đúng 6 chữ số.
                                                                     .. Định dạng được kiểm tra ở trình duyệt; đúng mã/thời hạn/số lần sai do server kiểm tra.
                                                                     ... validate() kiểm tra OTP trong validate() → VerifyOtpController [OT10] → PasswordRecoveryService.verify() [OT11]. --%>
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
                                                            <%-- OT08: . Bấm Xác nhận OTP để gửi mã vừa nhập.
                                                                 ... account-validation.js, sự kiện submit [OT09] → Controller [OT10]. --%>
                                                            <button type="submit"
                                                                class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-4"
                                                                style="background-color: var(--primary-container)
                                                                ; height: 48px;">
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
                                                                    <span id="modal-resend-timer" class="<%= otpRemainingTime > 0 ? "" : "d-none" %>" style="font-size: 0.85rem; color: var(--secondary);">
                                                                        Gửi lại sau <strong id="modal-countdown-sec"><%= otpRemainingTime %></strong>s
                                                                    </span>
                                                                    <button type="submit" form="resendOtpForm"
                                                                        id="modal-resend-btn"
                                                                        class="btn btn-link p-0 text-decoration-none fw-semibold <%= otpRemainingTime > 0 ? "d-none" : "" %>"
                                                                        style="font-size: 0.85rem; color: var(--primary-container);">
                                                                        Gửi lại mã OTP
                                                                    </button>
                                                                </div>
                                                            </div>
                                                        </form>
                                                    </div>

                                                    
                                                    <%-- OT07b: . Nút Gửi lại mã dùng form=resendOtpForm để gửi POST /resend-otp.
                                                         .. Server quyết định đã đủ thời gian gửi lại; đồng hồ giao diện chỉ hỗ trợ hiển thị.
                                                         ... ResendOtpController.doPost() [OT07c] → PasswordRecoveryService.resend() [OT07d]. --%>
                                                    <form id="resendOtpForm" action="${pageContext.request.contextPath}/resend-otp" method="POST">
                                                        <input type="hidden" name="csrfToken" value="<%= accountCsrf %>">
                                                    </form>

                                                    <!-- RESET PASSWORD -->
                                                    <div class="tab-pane fade <%= isResetTab ? " show active" : "" %>" id="modal-reset-pane">
                                                        <div class="mb-3">
                                                            <h2 class="fs-4 fw-semibold mb-1">Thiết lập mật khẩu mới</h2>
                                                            <p class="text-muted" style="font-size: 0.85rem;">Vui lòng tạo mật khẩu mới cho tài khoản của bạn</p>
                                                        </div>
                                                        
                                                        
                                                        <%-- OT12: . Hiển thị form mật khẩu mới và xác nhận sau khi OTP được xác minh.
                                                             .. Server vẫn phải kiểm tra quyền khôi phục còn hạn khi nhận yêu cầu lưu.
                                                             ... Bấm Cập nhật mật khẩu [OT13] để tiếp tục. --%>
                                                        <form action="${pageContext.request.contextPath}/reset-password" method="POST">
                                                            <input type="hidden" name="csrfToken" value="<%= accountCsrf %>">
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Mật khẩu mới</label>
                                                                
                                                                <%-- OT12a: . Ô mật khẩu mới nhận name=password, cần 8–255 ký tự và có chữ lẫn số.
                                                                     ... passwordError() trong account-validation.js → PasswordRecoveryService.reset() [OT16] → AccountValidation AccountValidation.passwordError(). --%>
                                                                <input type="password" id="reset-password-input"
                                                                    class="form-control bg-light border-0 p-2"
                                                                    name="password" maxlength="255"
                                                                    placeholder="Ít nhất 8 ký tự, gồm chữ và số"
                                                                    minlength="8" required>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Xác nhận mật khẩu</label>
                                                                
                                                                <%-- OT12b: . Ô xác nhận nhận name=confirm_password và phải khớp mật khẩu mới.
                                                                     ... validate() kiểm tra xác nhận trong validate() → ResetPasswordController [OT15]; lỗi tách theo ô bằng AccountValidation.passwordErrors(). --%>
                                                                <input type="password" id="reset-confirm-password-input"
                                                                    class="form-control bg-light border-0 p-2"
                                                                    name="confirm_password" maxlength="255"
                                                                    placeholder="Nhập lại mật khẩu mới"
                                                                    minlength="8" required>
                                                            </div>
                                                            
                                                            <%-- OT13: . Bấm Cập nhật mật khẩu để gửi mật khẩu mới và xác nhận.
                                                                 ... account-validation.js, sự kiện submit [OT14] → Controller [OT15]. --%>
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

        <jsp:include page="/components/account_feedback.jsp" />
        <script id="auth-modal-script"
            data-auto-open="<%= authModalAutoOpen %>"
            data-otp-remaining="<%= otpRemainingTime %>">

            // . Mở phần đăng nhập và ẩn phần Quên mật khẩu trong popup.
            // .. Chỉ đổi giao diện, không gửi dữ liệu; nút Login thực hiện tại [LG03].
            function showLoginSubView() {
                var loginView = document.getElementById('subview-login');
                var forgotView = document.getElementById('subview-forgot');
                if (loginView) loginView.classList.remove('d-none');
                if (forgotView) forgotView.classList.add('d-none');
            }

            // OT01: . Bấm Quên mật khẩu thì ẩn đăng nhập và mở form email khôi phục.
            // ... Form Quên mật khẩu [OT02]; bấm Gửi mã OTP mới gửi dữ liệu [OT03].
            function showForgotSubView() {
                var loginView = document.getElementById('subview-login');
                var forgotView = document.getElementById('subview-forgot');
                if (loginView) loginView.classList.add('d-none');
                if (forgotView) forgotView.classList.remove('d-none');
            }

            document.addEventListener("DOMContentLoaded", function () {
                // . Tự mở popup khi Controller trả thông báo hoặc URL có tham số tab.
                // .. Người dùng thấy lại đúng bước sau redirect, gồm form đăng ký lỗi hoặc form OTP.
                var hasErrorOrSuccess = document.getElementById('auth-modal-script').dataset.autoOpen === 'true';
                if (hasErrorOrSuccess) {
                    var myModal = new bootstrap.Modal(document.getElementById('authModal'), {
                        keyboard: true
                    });
                    myModal.show();
                }
            });

            // . Đếm ngược thời gian hiển thị nút Gửi lại mã theo dữ liệu server.
            // .. Hết đếm thì hiện nút; server vẫn kiểm tra giới hạn tại [OT07d], [OT06a].
            let modalRemaining = Number(document.getElementById('auth-modal-script').dataset.otpRemaining);
            if (modalRemaining > 0) {
                const countdownSpan = document.getElementById("modal-countdown-sec");
                const timerWrapper = document.getElementById("modal-resend-timer");
                const resendBtn = document.getElementById("modal-resend-btn");
                
                const interval = setInterval(() => {
                    modalRemaining--;
                    if (modalRemaining <= 0) {
                        clearInterval(interval);
                        if (timerWrapper) timerWrapper.classList.add('d-none');
                        if (resendBtn) resendBtn.classList.remove('d-none');
                    } else {
                        if (countdownSpan) countdownSpan.innerText = modalRemaining;
                    }
                }, 1000);
            }
        </script>
