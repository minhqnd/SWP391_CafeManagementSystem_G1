
(() => {
    'use strict';

    // [BMMK01]
    // . Ở passwordError (kiểm tra điều kiện mật khẩu) trong account-validation.js, em nhận mật khẩu từ hàm
    //   validate (kiểm tra giá trị của một ô nhập) để kiểm tra ngay trên màn hình. Em yêu cầu 8–255 ký tự,
    //   có chữ và số, không chứa ký tự điều khiển. Hàm trả lời báo về validate rồi feedback (hiển thị lời
    //   báo tại ô nhập) hiện lỗi dưới ô để người dùng sửa trước khi gửi.
    // ... Quay lại hàm validate trong account-validation.js để dùng lời báo vừa nhận; đọc tiếp tại
    //     account-validation.js, tìm [BMKT01].
    const passwordError = value => {
        if (!value) return 'Vui lòng nhập mật khẩu.';
        if (!value.trim()) return 'Mật khẩu không được chỉ có khoảng trắng.';
        // [SUA10-04] Luyện sửa: Đổi độ dài mật khẩu kiểm tra ngay trên màn hình.
        // . Giới hạn và lời báo phải giống AccountValidation.passwordError; sửa JavaScript riêng sẽ chưa đổi
        //   kiểm tra khi lưu.
        // . Dùng mật khẩu ngắn hơn giới hạn một ký tự và vừa đủ giới hạn để kiểm tra hai phía.
        if (value.length < 8 || value.length > 255)
            return 'Mật khẩu phải từ 8 đến 255 ký tự.';
        if (/[\u0000-\u001F\u007F-\u009F]/.test(value))
            return 'Mật khẩu không được chứa ký tự điều khiển.';
        if (!/[a-z]/i.test(value) || !/[0-9]/.test(value))
            return 'Mật khẩu phải có ít nhất một chữ cái (a-z, A-Z) và một chữ số (0-9).';
        return '';
    };

    const formKey = form => {
        const url = new URL(form.action, location.href);
        const path = url.pathname.split('/').pop();
        return path === 'profile' ? `profile-${url.searchParams.get('action') === 'changePassword' ? 'password' : 'update'}` : path;
    };
    const init = () => {
        const forms = new Map();
        document.querySelectorAll('form').forEach(form => {
            const key = formKey(form);
            if (!['register', 'login', 'forgot-password', 'verify-otp', 'reset-password', 'profile-update', 'profile-password'].includes(key)) return;
            forms.set(key, form);
            form.noValidate = true;
            let submitted = false;
            const serverErrors = new Map();
            const controls = [...form.elements].filter(field => field.matches('input,select,textarea') && field.type !== 'hidden');

            controls.filter(field => field.type === 'password').forEach((field, index) => {
                if (!field.id) field.id = `account-${key}-password-${index}`;
                field.dataset.passwordField = 'true';
                let group = field.closest('.input-group');
                if (!group) {
                    group = document.createElement('div');
                    group.className = 'input-group';
                    field.before(group);
                    group.append(field);
                }
                group.classList.add('account-password-group');
                field.classList.remove('w-100');
                group.querySelectorAll('[onclick^="togglePassword"]').forEach(toggle => toggle.remove());
                // [BMMAT01]
                // . Trong account-validation.js, em tạo nút mắt cho từng ô mật khẩu và gắn sự kiện click. Mỗi nút giữ
                //   đúng ô của nó để khi bấm chỉ che hoặc hiện mật khẩu tại ô đó, không gửi form hay đổi nội dung đã
                //   nhập.
                // ... Đọc tiếp tại account-validation.js, tìm [BMMAT02].

                const toggle = document.createElement('button');
                toggle.type = 'button';
                toggle.className = 'btn btn-light border account-password-toggle';
                toggle.setAttribute('aria-controls', field.id);
                toggle.setAttribute('aria-label', 'Hiện mật khẩu');
                toggle.setAttribute('aria-pressed', 'false');
                const icon = document.createElement('span');
                icon.className = 'material-symbols-outlined fs-5';
                icon.setAttribute('aria-hidden', 'true');
                icon.textContent = 'visibility';
                toggle.append(icon);
                // [BMMAT02]
                // . Khi bấm nút mắt, sự kiện toggle.click trong account-validation.js đổi field.type giữa password và
                //   text rồi đổi biểu tượng. Nhờ vậy người dùng xem hoặc che cùng một mật khẩu, còn giá trị trong ô vẫn
                //   giữ nguyên.
                // ... Kết thúc thao tác tại ô mật khẩu vừa đổi cách che hoặc hiện trên cùng màn hình.
                toggle.addEventListener('click', () => {
                    const visible = field.type === 'password';
                    field.type = visible ? 'text' : 'password';
                    icon.textContent = visible ? 'visibility_off' : 'visibility';
                    toggle.setAttribute('aria-label', visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
                    toggle.setAttribute('aria-pressed', String(visible));
                });
                group.append(toggle);
            });
            const find = name => form.elements.namedItem(name) || form.querySelector(`#${name}`);

            // [BMLOI01]
            // . Ở feedback (hiển thị lời báo tại ô nhập) trong account-validation.js, em nhận ô nhập và lời báo từ
            //   bước kiểm tra hoặc kết quả sau khi gửi form. Em đặt viền lỗi và thông báo vào đúng ô đó, rồi cập
            //   nhật khi người dùng sửa để họ biết cần sửa phần nào.
            // ... Kết thúc hiển thị lời báo khi viền và thông báo của đúng ô đã được cập nhật trên form; người
            //     dùng sửa ô hoặc gửi lại để bắt đầu lần kiểm tra tiếp.
            const feedback = (field, message) => {
                if (!field) return;
                const fieldKey = field.name || field.id;
                const id = `account-${key}-${fieldKey}-error`;
                let box = document.getElementById(id);
                if (!box) {
                    box = document.createElement('div');
                    box.id = id;
                    box.className = 'invalid-feedback';
                    box.setAttribute('aria-live', 'polite');
                    const anchor = field.closest('.input-group') || field;
                    anchor.insertAdjacentElement('afterend', box);
                }
                if (field.id === 'newPassword' || field.id === 'confirmPassword') {
                    const legacy = document.getElementById(`${field.id}Feedback`);
                    if (legacy) legacy.remove();
                }
                box.textContent = message;
                box.classList.toggle('d-block', Boolean(message));
                field.classList.toggle('is-invalid', Boolean(message));
                field.setAttribute('aria-invalid', String(Boolean(message)));
                const descriptions = new Set((field.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean));
                descriptions.add(id);
                field.setAttribute('aria-describedby', [...descriptions].join(' '));
            };

            // [BMKT01]
            // . Ở validate (kiểm tra giá trị của một ô nhập) trong account-validation.js, em nhận ô đang nhập hoặc
            //   sắp gửi, đọc tên ô để chọn cách kiểm tra phù hợp. Em trả lời báo cho feedback (hiển thị lời báo tại
            //   ô nhập) để hiện đúng vị trí; nếu đang gửi form mà còn lỗi thì bước gửi sẽ dừng lại.
            // ... Quay lại sự kiện input hoặc submit trong account-validation.js sau validate; feedback hiện lời
            //     báo và phần submit quyết định dừng hay gửi form.
            const validate = field => {
                if (field.disabled || field.readOnly || ['submit', 'button'].includes(field.type)) return '';
                const name = field.name || field.id;
                const raw = field.value;
                const value = raw.trim();
                if (serverErrors.has(name)) return serverErrors.get(name);

                if (['birthDay', 'birthMonth', 'birthYear'].includes(name)) {
                    const parts = ['birthDay', 'birthMonth', 'birthYear'].map(part => find(part).value);
                    const count = parts.filter(Boolean).length;
                    if (count && count !== 3) return 'Vui lòng chọn đủ ngày, tháng và năm sinh hoặc để trống cả ba.';
                    if (count === 3) {
                        const [day, month, year] = parts.map(Number);
                        const date = new Date(year, month - 1, day);
                        if (year < 1900 || date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day || date > new Date())
                            return 'Ngày sinh phải là ngày có thật, từ năm 1900 đến hôm nay.';
                    }
                    return '';
                }

                const requiredMessages = {
                    username: 'Vui lòng nhập email hoặc số điện thoại.',
                    fullname: 'Vui lòng nhập họ và tên.', fullName: 'Vui lòng nhập họ và tên.',
                    email: 'Vui lòng nhập email.', phone: 'Vui lòng nhập số điện thoại.',
                    password: 'Vui lòng nhập mật khẩu.', newPassword: 'Vui lòng nhập mật khẩu.',
                    oldPassword: 'Vui lòng nhập mật khẩu hiện tại.',
                    confirm_password: 'Vui lòng nhập mật khẩu xác nhận.', confirmPassword: 'Vui lòng nhập mật khẩu xác nhận.',
                    otp: 'Vui lòng nhập mã OTP.'
                };
                if (field.required && !raw) return requiredMessages[name] || 'Vui lòng nhập thông tin này.';
                if (!raw && !field.required) return '';
                // [DNTK02]
                // . Trong validate (kiểm tra giá trị của một ô nhập) của account-validation.js, em kiểm tra ô username
                //   của form Đăng nhập để báo lỗi sớm. Khi gửi, email hoặc số điện thoại này đi cùng mật khẩu đến doPost
                //   trong LoginController.java để kiểm tra lại rồi gọi UserDAO.login (đối chiếu tài khoản để đăng nhập)
                //   đối chiếu tài khoản.
                // ... Đọc tiếp tại LoginController.java, tìm [DNTK03].
                if (name === 'username' && !value) return 'Vui lòng nhập email hoặc số điện thoại.';
                if (name === 'username' && value.length > 150) return 'Email hoặc số điện thoại tối đa 150 ký tự.';
                if (name === 'password' && key === 'login' && raw.length > 255) return 'Mật khẩu không được vượt quá 255 ký tự.';
                if (name === 'oldPassword' && raw.length > 255) return 'Mật khẩu hiện tại tối đa 255 ký tự.';

                // [DKHT02]
                // . Trong validate (kiểm tra giá trị của một ô nhập) của account-validation.js, em nhận fullname hoặc
                //   fullName để kiểm tra họ tên trên form Đăng ký hoặc Hồ sơ. Khi gửi đăng ký, tên tiếp tục tới
                //   RegisterController.doPost rồi AccountValidation.nameError (kiểm tra họ tên và trả lời báo lỗi) để
                //   kiểm tra lại trước khi lưu.
                // ... Đọc tiếp tại RegisterController.java, tìm [DKHT03].
                if (['fullname', 'fullName'].includes(name)) {
                    if (!value) return 'Vui lòng nhập họ và tên.';
                    if (value.length > 100) return 'Họ và tên tối đa 100 ký tự.';
                    if (/[\u0000-\u001F\u007F-\u009F]/.test(value)) return 'Họ và tên chứa ký tự không được hỗ trợ.';
                }

                // [DKDT02]
                // . Trong validate (kiểm tra giá trị của một ô nhập) của account-validation.js, em nhận phone để kiểm
                //   tra định dạng số điện thoại trên form. Khi gửi đăng ký, RegisterController.doPost nhận số rồi gọi
                //   AccountValidation.phoneError (kiểm tra định dạng số điện thoại) kiểm tra lại trước khi tìm trùng và
                //   tạo tài khoản.
                // ... Đọc tiếp tại RegisterController.java, tìm [DKDT03].
                if (name === 'phone' && !value.replace(/[\s.()-]/g, '')) return 'Vui lòng nhập số điện thoại.';
                if (name === 'phone' && !/^0[0-9]{9}$/.test(value.replace(/[\s.()-]/g, ''))) return 'Số điện thoại sai định dạng.';

                // [DKEM02]
                // . Trong validate (kiểm tra giá trị của một ô nhập) của account-validation.js, em kiểm tra email đúng
                //   dạng Gmail và giới hạn độ dài. Sau đó checkContact (kiểm tra liên hệ trùng ngay trên form) tìm email
                //   đã dùng; khi gửi đăng ký, RegisterController.doPost còn gọi AccountValidation.emailError (kiểm tra
                //   định dạng và độ dài email) kiểm tra lại trước khi lưu.
                // ... Đọc tiếp tại RegisterController.java, tìm [DKEM03].
                if (name === 'email') {
                    if (!value) return 'Vui lòng nhập email.';
                    if (value.length > 150) return 'Email tối đa 150 ký tự.';
                    // [SUA10-06] Luyện sửa: Đổi đuôi email được nhận ngay trên form.
                    // . Quy tắc ở đây phải giống AccountValidation.emailError; thử cả đăng ký, hồ sơ và quên
                    //   mật khẩu sau khi thay đổi.
                    if (!/^[A-Za-z0-9_+%-]+(?:\.[A-Za-z0-9_+%-]+)*@gmail\.com$/i.test(value)) return 'Sai định dạng email.';
                }
                // [DKMK02]
                // . Trong validate (kiểm tra giá trị của một ô nhập) của account-validation.js, em gọi passwordError
                //   (kiểm tra điều kiện mật khẩu) cùng file để kiểm tra mật khẩu mới trước khi gửi. Khi form Đăng ký
                //   được gửi, RegisterController.doPost nhận mật khẩu và xác nhận rồi gọi
                //   AccountValidation.passwordErrors (kiểm tra mật khẩu và xác nhận theo từng ô) kiểm tra lại trước khi
                //   tạo tài khoản.
                // ... Đọc tiếp tại RegisterController.java, tìm [DKMK03].
                if ((name === 'password' && key !== 'login') || name === 'newPassword') return passwordError(raw);

                // [DKXN02]
                // . Trong validate (kiểm tra giá trị của một ô nhập) của account-validation.js, em lấy mật khẩu của cùng
                //   form để so với ô xác nhận. Nếu không khớp thì báo lỗi ngay tại ô này. Khi gửi đăng ký,
                //   RegisterController.doPost nhận cả hai và gọi AccountValidation.passwordErrors (kiểm tra mật khẩu và
                //   xác nhận theo từng ô) để so lại trước khi lưu.
                // ... Trong account-validation.js, trả lời báo về phần submit đang gọi validate; lỗi thì dừng tại
                //     ô xác nhận, hết lỗi mới gửi form Đăng ký tới RegisterController.doPost.
                if (name === 'confirm_password' || name === 'confirmPassword') {
                    if (!value) return 'Vui lòng nhập mật khẩu xác nhận.';
                    const password = find(key === 'profile-password' ? 'newPassword' : 'password');
                    return raw !== password.value ? 'Mật khẩu xác nhận không khớp.' : '';
                }

                // [OTN02]
                // . Trong validate (kiểm tra giá trị của một ô nhập) của account-validation.js, em kiểm tra ô otp phải
                //   có đúng sáu chữ số. Nếu sai thì feedback (hiển thị lời báo tại ô nhập) hiện lỗi tại ô và dừng gửi.
                //   Nếu đúng, form tới VerifyOtpController.doPost để gọi PasswordRecoveryService.verify (xác minh mã
                //   OTP) kiểm tra mã thật đã gửi cho tài khoản này.
                // ... Đọc tiếp tại PasswordRecoveryService.java, tìm [OTN03].
                if (name === 'otp' && !value) return 'Vui lòng nhập mã OTP.';
                if (name === 'otp' && !/^[0-9]{6}$/.test(raw)) return 'Mã OTP phải gồm đúng 6 chữ số.';

                // [HSDC02]
                // . Trong validate (kiểm tra giá trị của một ô nhập) của account-validation.js, em kiểm tra address để
                //   báo lỗi địa chỉ ngay trên form Hồ sơ. Khi bấm Lưu thay đổi, dữ liệu tới update (kiểm tra và cập nhật
                //   hồ sơ) trong ProfileController.java để kiểm tra lại trước khi gọi UserDAO.updateUserProfile (kiểm
                //   tra liên hệ rồi ghi hồ sơ mới) ghi thông tin.
                // ... Đọc tiếp tại ProfileController.java, tìm [HSDC03].
                if (name === 'address' && value.length > 500) return 'Địa chỉ tối đa 500 ký tự.';
                if (name === 'address' && /[\u0000-\u001F\u007F-\u009F]/.test(value)) return 'Địa chỉ chứa ký tự không được hỗ trợ.';
                if (field.maxLength > 0 && raw.length > field.maxLength) return `Thông tin này tối đa ${field.maxLength} ký tự.`;
                return '';
            };
            const normalizeContact = field => field.name === 'email' ? field.value.trim().toLowerCase() : field.value.trim().replace(/[\s.()-]/g, '');
            const contactState = new Map();
            const contactTimers = new Map();
            let checkingSubmit = false;

            // [BMLH01]
            // . Ở checkContact (kiểm tra liên hệ trùng ngay trên form) trong account-validation.js, em nhận ô email
            //   hoặc số điện thoại rồi gửi loại ô, giá trị và phạm vi kiểm tra tới /account-availability. doPost
            //   trong AccountAvailabilityController.java gọi UserDAO.contactExists (tìm email hoặc số điện thoại
            //   trùng) để tìm trùng, sau đó trả exists về đây để báo lỗi tại ô nhập.
            // ... Đọc tiếp tại AccountAvailabilityController.java, tìm [BMLH02].
            const checkContact = async field => {
                if (!['register', 'profile-update'].includes(key) || !['email', 'phone'].includes(field.name) || !field.value.trim() || field.disabled || field.readOnly) return;
                const value = normalizeContact(field);
                const savedError = serverErrors.get(field.name);
                serverErrors.delete(field.name);
                const localError = validate(field);
                if (savedError) serverErrors.set(field.name, savedError);
                if (localError) return;
                const previous = contactState.get(field.name);
                if (previous?.value === value) return previous.promise;
                const state = {value};
                contactState.set(field.name, state);
                field.setAttribute('aria-busy', 'true');
                state.promise = (async () => {
                    try {
                        const body = new URLSearchParams({field: field.name, value, scope: key === 'profile-update' ? 'profile' : 'register', csrfToken: find('csrfToken').value});
                        const url = new URL(form.action, location.href);
                        url.pathname = url.pathname.replace(/[^/]+$/, 'account-availability');
                        url.search = '';
                        const response = await fetch(url, {method: 'POST', body, credentials: 'same-origin', headers: {'Accept': 'application/json'}});
                        if (!response.ok) throw new Error('availability');

                        const result = await response.json();
                        if (contactState.get(field.name) !== state || normalizeContact(field) !== value) return;
                        serverErrors.delete(field.name);
                        if (result.exists) serverErrors.set(field.name, field.name === 'email' ? 'Email đã tồn tại trong hệ thống.' : 'Số điện thoại đã tồn tại trong hệ thống.');
                        feedback(field, validate(field));
                    } catch (_) {
                        if (contactState.get(field.name) === state) contactState.delete(field.name);
                    } finally {
                        if (!contactState.has(field.name) || contactState.get(field.name) === state) field.removeAttribute('aria-busy');
                    }
                })();
                return state.promise;
            };
            const refresh = () => controls.forEach(field => {
                if (submitted) feedback(field, validate(field));
            });
            controls.forEach(field => {

                const changed = () => {
                    serverErrors.delete(field.name || field.id);
                    if (field.name === 'password' || field.name === 'newPassword') {
                        serverErrors.delete('confirm_password');
                        serverErrors.delete('confirmPassword');
                    }
                    if (['birthDay', 'birthMonth', 'birthYear'].includes(field.name))
                        ['birthDay', 'birthMonth', 'birthYear'].forEach(name => serverErrors.delete(name));
                    if (field.dataset.feedbackRestore !== 'true') {
                        const scope = form.closest('#authModal') || document;
                        scope.querySelectorAll('[data-account-notice]').forEach(notice => { notice.classList.add('d-none'); });
                    }
                    if (submitted && ['email', 'phone'].includes(field.name)) {
                        contactState.delete(field.name);
                        clearTimeout(contactTimers.get(field.name));
                        contactTimers.set(field.name, setTimeout(() => checkContact(field), 500));
                    }
                    refresh();
                };
                field.addEventListener('input', changed);
                field.addEventListener('change', changed);
            });

            // [DKL02]
            // . Trước khi gửi form, em gọi validate để kiểm tra từng ô và chờ checkContact tìm email, số điện thoại
            //   trùng. Có lỗi thì hiện lời báo và đưa con trỏ đến ô cần sửa; đúng hết mới gửi form Đăng ký tới
            //   RegisterController.doPost qua /register.
            // ... Đọc tiếp tại RegisterController.java, tìm [DKL03].
            form.addEventListener('submit', async event => {
                event.preventDefault();
                if (checkingSubmit) return;
                submitted = true;
                refresh();
                checkingSubmit = true;
                try {
                    await Promise.all(controls.filter(field => ['email', 'phone'].includes(field.name)).map(checkContact));
                } finally { checkingSubmit = false; }
                refresh();
                const firstInvalid = controls.find(field => validate(field));
                if (firstInvalid) firstInvalid.focus();
                else HTMLFormElement.prototype.submit.call(form);
            });
            form.accountFeedback = {find, feedback, serverErrors, refresh, markSubmitted: () => { submitted = true; }};
        });

        // [BMTV01]
        // . Trong account-validation.js, em đọc data-account-feedback mà account_feedback.jsp đưa ra sau lần xử
        //   lý lỗi. Em tìm đúng form và ô để điền lại giá trị rồi gọi feedback (hiển thị lời báo tại ô nhập)
        //   hiện lời báo, giúp người dùng sửa tiếp thông tin vừa nhập.
        // ... Đọc tiếp tại account-validation.js, tìm [BMLOI01] để xem cách hiện lời báo; kết thúc khôi phục form
        //     khi các ô và lỗi cũ đã hiện đúng vị trí.
        document.querySelectorAll('[data-account-feedback]').forEach(payload => {
            const key = payload.dataset.form;
            const form = forms.get(key);
            if (!form) return;
            if (key === 'profile-update') document.getElementById('btnEditProfile')?.click();
            if (key === 'profile-password' && window.bootstrap) bootstrap.Collapse.getOrCreateInstance(document.getElementById('passwordCollapse'), {toggle: false}).show();
            const api = form.accountFeedback;
            payload.querySelectorAll('[data-value-field]').forEach(item => {
                const field = api.find(item.dataset.valueField);
                if (field) {
                    field.value = item.dataset.value;
                    field.dataset.feedbackRestore = 'true';
                    field.dispatchEvent(new Event('input', {bubbles: true}));
                    delete field.dataset.feedbackRestore;
                }
            });
            payload.querySelectorAll('[data-field]').forEach(item => {
                api.serverErrors.set(item.dataset.field, item.dataset.error);
                api.feedback(api.find(item.dataset.field), item.dataset.error);
            });
            api.markSubmitted();
        });
    };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
    else init();
})();
