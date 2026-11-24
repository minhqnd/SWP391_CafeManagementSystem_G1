
(() => {
    'use strict';

    // Kiểm tra điều kiện mật khẩu mới dùng chung trên các form tài khoản.
    const passwordError = value => {
        if (!value) return 'Vui lòng nhập mật khẩu.';
        if (!value.trim()) return 'Mật khẩu không được chỉ có khoảng trắng.';
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
                // [TKMAT01]
                // . Em tạo nút mắt gắn với từng ô mật khẩu để người nhập xem hoặc che nội dung của đúng
                //   ô. Nút chỉ đổi cách hiển thị, không đổi mật khẩu đã nhập.
                //
                // Kết thúc: xem/che mật khẩu ngay trên ô; không gửi form.

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

            // [TKLOI01]
            // . Em đặt viền và lời báo ngay dưới ô sai để người nhập biết cần sửa gì. Sửa đúng thì lời
            //   báo của ô đó được bỏ.
            //
            // Tra cứu: Trở về phần kiểm tra hoặc sự kiện đang gọi feedback trong cùng file; kết thúc hiển thị
            //          lỗi của ô.
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

            // [TKKT02]
            // . Em dùng chung hàm validate để kiểm tra các ô của form tài khoản. Hàm dựa vào tên ô và
            //   form đang dùng để chọn điều kiện phù hợp, rồi trả lời báo nếu nhập sai. Phần xử lý gửi
            //   form dùng kết quả này để giữ lại các ô cần sửa hoặc cho gửi tiếp.
            //
            // Tra cứu: Được gọi từ TKGUI01 và lúc người dùng sửa ô; kiểm tra xong quay lại ngay sau validate
            //          tại nơi gọi. Hiện lỗi tại TKLOI01.
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

                if (name === 'username' && !value) return 'Vui lòng nhập email hoặc số điện thoại.';
                if (name === 'username' && value.length > 150) return 'Email hoặc số điện thoại tối đa 150 ký tự.';
                if (name === 'password' && key === 'login' && raw.length > 255) return 'Mật khẩu không được vượt quá 255 ký tự.';
                if (name === 'oldPassword' && raw.length > 255) return 'Mật khẩu hiện tại tối đa 255 ký tự.';

                if (['fullname', 'fullName'].includes(name)) {
                    if (!value) return 'Vui lòng nhập họ và tên.';
                    if (value.length > 100) return 'Họ và tên tối đa 100 ký tự.';
                    if (/[\u0000-\u001F\u007F-\u009F]/.test(value)) return 'Họ và tên chứa ký tự không được hỗ trợ.';
                }

                if (name === 'phone' && !value.replace(/[\s.()-]/g, '')) return 'Vui lòng nhập số điện thoại.';
                if (name === 'phone' && !/^0[0-9]{9}$/.test(value.replace(/[\s.()-]/g, ''))) return 'Số điện thoại sai định dạng.';

                if (name === 'email') {
                    if (!value) return 'Vui lòng nhập email.';
                    if (value.length > 150) return 'Email tối đa 150 ký tự.';
                    if (!/^[A-Za-z0-9_+%-]+(?:\.[A-Za-z0-9_+%-]+)*@gmail\.com$/i.test(value)) return 'Sai định dạng email.';
                }

                if ((name === 'password' && key !== 'login') || name === 'newPassword') return passwordError(raw);

                if (name === 'confirm_password' || name === 'confirmPassword') {
                    if (!value) return 'Vui lòng nhập mật khẩu xác nhận.';
                    const password = find(key === 'profile-password' ? 'newPassword' : 'password');
                    return raw !== password.value ? 'Mật khẩu xác nhận không khớp.' : '';
                }

                if (name === 'otp' && !value) return 'Vui lòng nhập mã OTP.';
                if (name === 'otp' && !/^[0-9]{6}$/.test(raw)) return 'Mã OTP phải gồm đúng 6 chữ số.';

                if (name === 'address' && value.length > 500) return 'Địa chỉ tối đa 500 ký tự.';
                if (name === 'address' && /[\u0000-\u001F\u007F-\u009F]/.test(value)) return 'Địa chỉ chứa ký tự không được hỗ trợ.';
                if (field.maxLength > 0 && raw.length > field.maxLength) return `Thông tin này tối đa ${field.maxLength} ký tự.`;
                return '';
            };
            const normalizeContact = field => field.name === 'email' ? field.value.trim().toLowerCase() : field.value.trim().replace(/[\s.()-]/g, '');
            const contactState = new Map();
            const contactTimers = new Map();
            let checkingSubmit = false;

            // [TKTR01]
            // . Em gửi email hoặc số điện thoại của form đăng ký hay hồ sơ để hỏi có tài khoản khác đang
            //   dùng chưa. Kết quả được gắn lại đúng ô đang nhập trước khi cho gửi form.
            //
            // Tra cứu: Nhận tại AccountAvailabilityController.java / TKTR02, đọc tại UserDAO.java / TKTR03
            //          rồi quay về sau phần nhận kết quả trong TKTR01. Cuối cùng tiếp tục TKGUI01 nếu đang
            //          bấm gửi.
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

            // [TKGUI01]
            // . Khi người dùng bấm gửi một form tài khoản, em kiểm tra các ô và chờ kết quả liên hệ trùng
            //   nếu chức năng cần kiểm tra. Còn ô sai thì em giữ form và đưa người dùng tới ô đó; đúng
            //   hết mới gửi form tới controller của chức năng đang dùng.
            //
            // Tra cứu: Kiểm tra ô tại TKKT02, liên hệ tại TKTR01 rồi quay lại sau lời gọi ở TKGUI01. Hợp lệ
            //          gửi tới RegisterController.java / DKL02, LoginController.java / DNL02,
            //          ForgotPasswordController.java / OTG02, VerifyOtpController.java / OTXM02,
            //          ResetPasswordController.java / OTDM02, ProfileController.java / HSL02 hoặc
            //          ProfileController.java / MKL02 theo form đang bấm.
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

        // [TKTB03]
        // . Khi trang nhận lại thông tin sau lỗi, em tìm đúng form, điền các ô đã giữ rồi đặt lời báo vào
        //   đúng ô sai. Nếu đang sửa hồ sơ, em mở lại phần chỉnh sửa để người dùng tiếp tục mà không phải
        //   nhập lại từ đầu.
        //
        // Tra cứu: Kết thúc tại form đang thao tác: auth_modal.jsp / DKL01, auth_modal.jsp / DNL01,
        //          auth_modal.jsp / OTG01, auth_modal.jsp / OTXM01, auth_modal.jsp / OTDM01, profile.jsp /
        //          HSL01 hoặc profile.jsp / MKL01.
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
