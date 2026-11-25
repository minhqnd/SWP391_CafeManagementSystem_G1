
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%

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

        <%-- [ATM02]
             . Em đặt phần đăng nhập, đăng ký và khôi phục mật khẩu trong cùng một hộp để người dùng
               chuyển qua lại ngay trên trang đang xem. Chọn tab thì đổi form đang hiển thị, còn bấm X thì
               đóng hộp; các thao tác này chưa gửi thông tin tài khoản để xử lý.

             Tra cứu: Đăng nhập: DNL01. Đăng ký: DKL01. Quên mật khẩu: OTG01. Đóng hộp là kết thúc thao tác mở
                      giao diện.
        --%>
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

                                <%-- [TKTB01]
                                     . Em dùng vùng này hiện kết quả đăng ký, đăng nhập hoặc khôi phục mật
                                       khẩu. Trang mở lại đúng bước để người dùng tiếp tục trên form vừa
                                       thao tác.

                                     Tra cứu: Kết thúc thông báo; form đăng nhập DNL01, đăng ký DKL01, khôi
                                              phục OTG01, xác nhận OTXM01 hoặc mật khẩu mới OTDM01.
                                --%>
                                <% if (displayError !=null) { %>
                                    <div data-account-notice role="alert" class="alert alert-danger p-2 mb-3 text-center" style="font-size: 0.875rem;">
                                        <c:out value="<%= displayError %>" />
                                    </div>
                                    <% } %>

                                        <% if (displaySuccess !=null && displayError == null) { %>
                                            <div data-account-notice role="status" class="alert alert-success p-2 mb-3 text-center"
                                                style="font-size: 0.875rem;">
                                                <c:out value="<%= displaySuccess %>" />
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

                                                    <div class="tab-pane fade <%= isLoginTab ? " show active" : "" %>"

                                                        id="modal-login-pane">

                                                        <div id="subview-login" class="<%= isForgotTab ? "d-none" : "" %>">
                                                            <div class="mb-3">
                                                                <h2 class="fs-4 fw-semibold mb-1">Chào mừng trở lại</h2>
                                                                <p class="text-muted" style="font-size: 0.85rem;">Vui lòng nhập định danh tài khoản</p>
                                                            </div>

                                                            <%-- [DNL01]
                                                                 . Ở form đăng nhập, em cho người dùng
                                                                   nhập email hoặc số điện thoại cùng mật
                                                                   khẩu. Bấm Đăng nhập thì các ô được kiểm
                                                                   tra trên trang; nếu nhập đúng định
                                                                   dạng, form gửi tới /login để doPost
                                                                   trong LoginController đối chiếu với tài
                                                                   khoản đang lưu.

                                                                 Tra tiếp: LoginController.java / DNL02.
                                                            --%>
                                                            <form action="${pageContext.request.contextPath}/login" method="POST">
                                                            <input type="hidden" name="csrfToken" value="<%= accountCsrf %>">
                                                                <% if (authRedirect !=null) { %>
                                                                    <input type="hidden" name="redirect" value="<%= authRedirect %>">
                                                                <% } %>
                                                                <div class="mb-3">
                                                                    <label for="modal-login-username" class="form-label fw-medium" style="font-size: 0.85rem;">Email hoặc số điện thoại</label>

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

                                                                <button type="submit"
                                                                    class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-2"
                                                                    style="background-color: var(--primary-container); height: 48px;">
                                                                    Đăng nhập <span class="material-symbols-outlined fs-5">arrow_forward</span>
                                                                </button>
                                                            </form>
                                                        </div>

                                                        <div id="subview-forgot" class="<%= isForgotTab ? "" : "d-none" %>">
                                                            <div class="mb-3">
                                                                <h2 class="fs-4 fw-semibold mb-1">Khôi phục quyền truy cập</h2>
                                                                <p class="text-muted" style="font-size: 0.85rem;">Hệ thống gửi mã OTP qua email để bạn đặt mật khẩu mới</p>
                                                            </div>

                                                            <%-- [OTG01]
                                                                 . Người dùng nhập email đã liên kết với
                                                                   tài khoản cần khôi phục. Khi bấm Gửi
                                                                   mã, form được kiểm tra trên trang rồi
                                                                   chuyển tới doPost của
                                                                   ForgotPasswordController để bắt đầu tìm
                                                                   tài khoản và gửi mã xác nhận.

                                                                 Tra tiếp: ForgotPasswordController.java / OTG02.
                                                            --%>
                                                            <form action="${pageContext.request.contextPath}/forgot-password" method="POST">
                                                            <input type="hidden" name="csrfToken" value="<%= accountCsrf %>">
                                                                <div class="mb-3">
                                                                    <label class="form-label fw-medium" style="font-size: 0.85rem;">Email đã liên kết</label>

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

                                                    <div class="tab-pane fade <%= isRegisterTab ? " show active" : "" %>" id="modal-register-pane">
                                                        <div class="mb-3">
                                                            <h2 class="fs-4 fw-semibold mb-1">Tạo tài khoản Aura</h2>
                                                            <p class="text-muted" style="font-size: 0.85rem;">Đăng ký thành viên để nhận ưu đãi và tích điểm</p>
                                                        </div>

                                                        <%-- [DKL01]
                                                             . Ở form đăng ký, em đặt họ tên, số điện
                                                               thoại, email, mật khẩu và xác nhận mật khẩu
                                                               cùng với nút Tạo tài khoản. Khi người dùng
                                                               bấm nút, các ô được kiểm tra trên trang
                                                               trước. Nếu nhập đúng, form gửi tới
                                                               /register để doPost trong
                                                               RegisterController tiếp nhận và xử lý việc
                                                               tạo tài khoản.

                                                             Tra tiếp: RegisterController.java / DKL02.
                                                        --%>
                                                        <form action="${pageContext.request.contextPath}/register" method="POST">
                                                            <input type="hidden" name="csrfToken" value="<%= accountCsrf %>">
                                                            <% if (authRedirect != null) { %>
                                                                <input type="hidden" name="redirect" value="<%= authRedirect %>">
                                                            <% } %>
                                                            <div class="row g-2 mb-3">
                                                                <div class="col-12">
                                                                    <label class="form-label fw-medium" style="font-size: 0.85rem;">Họ và tên</label>

                                                                    <input type="text" class="form-control bg-light border-0 p-2" name="fullname" placeholder="Nguyễn Văn A" required>
                                                                </div>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Số điện thoại</label>

                                                                <input type="tel" class="form-control bg-light border-0 p-2" name="phone" placeholder="VD: 0912345678" pattern="0[0-9]{9}" inputmode="numeric" title="Nhập 10 chữ số và bắt đầu bằng số 0" required>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Email</label>

                                                                <input type="email" class="form-control bg-light border-0 p-2" name="email" maxlength="150" placeholder="Email của bạn" required>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Mật khẩu</label>

                                                                <input type="password" id="reg-password" class="form-control bg-light border-0 p-2" name="password" maxlength="255" placeholder="Ít nhất 8 ký tự, gồm chữ và số" minlength="8" required>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Xác nhận mật khẩu</label>

                                                                <input type="password" id="reg-confirm-password" class="form-control bg-light border-0 p-2" name="confirm_password" maxlength="255" placeholder="Nhập lại mật khẩu" minlength="8" required>
                                                            </div>

                                                            <button type="submit" class="btn w-100 fw-semibold text-white d-flex align-items-center justify-content-center gap-2 mt-4" style="background-color: var(--primary-container); height: 48px;">
                                                                Tạo tài khoản mới <span class="material-symbols-outlined fs-5">how_to_reg</span>
                                                            </button>
                                                        </form>
                                                    </div>

                                                    <div class="tab-pane fade <%= isOtpTab ? " show active" : "" %>" id="modal-otp-pane">
                                                        <div class="mb-3">
                                                            <h2 class="fs-4 fw-semibold mb-1">Xác thực Email</h2>
                                                            <p class="text-muted" style="font-size: 0.85rem;">Nhập mã OTP đã được gửi đến email của bạn</p>
                                                        </div>

                                                        <%-- [OTXM01]
                                                             . Người dùng nhập mã gồm sáu chữ số đã nhận
                                                               qua email. Khi bấm Xác nhận, form gửi mã
                                                               tới doPost của VerifyOtpController để kiểm
                                                               tra mã có đúng với lần khôi phục đang làm
                                                               hay không.

                                                             Tra tiếp: VerifyOtpController.java / OTXM02.
                                                        --%>
                                                        <form action="${pageContext.request.contextPath}/verify-otp" method="POST">
                                                            <input type="hidden" name="csrfToken" value="<%= accountCsrf %>">
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
                                                                style="background-color: var(--primary-container)
                                                                ; height: 48px;">
                                                                Xác nhận OTP <span class="material-symbols-outlined fs-5">check_circle</span>
                                                            </button>
                                                            <div class="d-flex justify-content-between align-items-center mt-3">

                                                                <%-- [OTUI02]
                                                                     . Khi người dùng bấm Quay lại ở form nhập mã xác nhận, em mở
                                                                       trang chủ kèm lựa chọn hiển thị phần Quên mật khẩu. Trang
                                                                       dùng lựa chọn này để mở lại hộp tài khoản; nút Quay lại chỉ
                                                                       đổi bước hiển thị, chưa gửi mã mới hoặc đổi mật khẩu.

                                                                     Tra tiếp: auth_modal.jsp / ATM03.
                                                                --%>
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
                                                                    <%-- [OTGL01]
                                                                         . Khi bấm Gửi lại, form gửi yêu
                                                                           cầu tới ResendOtpController và
                                                                           dùng email của lần khôi phục
                                                                           đang làm. Người dùng không phải
                                                                           nhập email lại, còn controller
                                                                           sẽ kiểm tra có được phép gửi mã
                                                                           tiếp hay chưa.

                                                                         Tra tiếp: ResendOtpController.java / OTGL02.
                                                                    --%>
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

                                                    <form id="resendOtpForm" action="${pageContext.request.contextPath}/resend-otp" method="POST">
                                                        <input type="hidden" name="csrfToken" value="<%= accountCsrf %>">
                                                    </form>

                                                    <div class="tab-pane fade <%= isResetTab ? " show active" : "" %>" id="modal-reset-pane">
                                                        <div class="mb-3">
                                                            <h2 class="fs-4 fw-semibold mb-1">Thiết lập mật khẩu mới</h2>
                                                            <p class="text-muted" style="font-size: 0.85rem;">Vui lòng tạo mật khẩu mới cho tài khoản của bạn</p>
                                                        </div>

                                                        <%-- [OTDM01]
                                                             . Người dùng nhập mật khẩu mới và nhập lại để
                                                               xác nhận. Khi bấm Cập nhật, form gửi hai ô
                                                               tới doPost của ResetPasswordController để
                                                               đổi mật khẩu cho tài khoản đã xác nhận
                                                               email.

                                                             Tra tiếp: ResetPasswordController.java / OTDM02.
                                                        --%>
                                                        <form action="${pageContext.request.contextPath}/reset-password" method="POST">
                                                            <input type="hidden" name="csrfToken" value="<%= accountCsrf %>">
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Mật khẩu mới</label>

                                                                <input type="password" id="reset-password-input"
                                                                    class="form-control bg-light border-0 p-2"
                                                                    name="password" maxlength="255"
                                                                    placeholder="Ít nhất 8 ký tự, gồm chữ và số"
                                                                    minlength="8" required>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label fw-medium" style="font-size: 0.85rem;">Xác nhận mật khẩu</label>

                                                                <input type="password" id="reset-confirm-password-input"
                                                                    class="form-control bg-light border-0 p-2"
                                                                    name="confirm_password" maxlength="255"
                                                                    placeholder="Nhập lại mật khẩu mới"
                                                                    minlength="8" required>
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

        <jsp:include page="/components/account_feedback.jsp" />
        <script id="auth-modal-script"
            data-auto-open="<%= authModalAutoOpen %>"
            data-otp-remaining="<%= otpRemainingTime %>">

            // [OTUI01]
            // . Khi chọn Quên mật khẩu hoặc Quay lại đăng nhập, em đổi form đang hiển thị trong hộp tài
            //   khoản. Người dùng có thể chuyển sang đúng phần cần dùng mà chưa gửi email hay thay đổi
            //   mật khẩu.
            //
            // Tra cứu: Quên mật khẩu mở OTG01; Quay lại mở DNL01. Kết thúc thao tác đổi giao diện.
            function showLoginSubView() {
                document.querySelectorAll('#authModal [data-account-notice]').forEach(notice => notice.classList.add('d-none'));
                var loginView = document.getElementById('subview-login');
                var forgotView = document.getElementById('subview-forgot');
                if (loginView) loginView.classList.remove('d-none');
                if (forgotView) forgotView.classList.add('d-none');
            }

            function showForgotSubView() {
                document.querySelectorAll('#authModal [data-account-notice]').forEach(notice => notice.classList.add('d-none'));
                var loginView = document.getElementById('subview-login');
                var forgotView = document.getElementById('subview-forgot');
                if (loginView) loginView.classList.add('d-none');
                if (forgotView) forgotView.classList.remove('d-none');
            }

            // [ATM03]
            // . Khi trang mở lại sau một thao tác tài khoản, em đọc bước cần hiển thị để mở đúng form vừa
            //   dùng. Nhờ vậy người dùng thấy thông báo và biết mình cần đăng nhập, nhập mã hay đặt mật
            //   khẩu mới để tiếp tục.
            //
            // Tra tiếp: TKTB01.
            document.addEventListener("DOMContentLoaded", function () {
                const authModal = document.getElementById('authModal');
                const clearNotices = () => authModal?.querySelectorAll('[data-account-notice]').forEach(notice => notice.classList.add('d-none'));
                authModal?.addEventListener('hidden.bs.modal', clearNotices);
                document.getElementById('modalAuthTab')?.addEventListener('shown.bs.tab', clearNotices);

                var hasErrorOrSuccess = document.getElementById('auth-modal-script').dataset.autoOpen === 'true';
                if (hasErrorOrSuccess) {
                    var myModal = new bootstrap.Modal(document.getElementById('authModal'), {
                        keyboard: true
                    });
                    myModal.show();
                }
            });

            let modalRemaining = Number(document.getElementById('auth-modal-script').dataset.otpRemaining);
            if (modalRemaining > 0) {
                const countdownSpan = document.getElementById("modal-countdown-sec");
                const timerWrapper = document.getElementById("modal-resend-timer");
                const resendBtn = document.getElementById("modal-resend-btn");

                // [OTTG01]
                // . Sau khi gửi mã khôi phục, em hiển thị số giây còn phải chờ trên nút Gửi lại. Hết thời
                //   gian chờ thì nút mới được bật để người dùng yêu cầu mã mới. Khi yêu cầu được gửi,
                //   phần xử lý khôi phục vẫn kiểm tra lại thời gian chờ trước khi cho gửi thư.
                //
                // Tra cứu: Kết thúc tại nút OTGL01; chỉ khi bấm nút mới gửi yêu cầu tiếp theo.
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
