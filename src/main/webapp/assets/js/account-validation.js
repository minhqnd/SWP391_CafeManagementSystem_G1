/* Dùng chung kiểm tra trường và hiển thị lỗi cho các form BF10; server vẫn là nơi xác nhận cuối cùng. */
(() => {
    'use strict';
    // . Mật khẩu mới dài 8–255 ký tự, có chữ và số; quy tắc này không áp lại cho mật khẩu đăng nhập.
    const passwordError = value => {
        if (value.length < 8 || value.length > 255 || !value.trim())
            return 'Mật khẩu phải từ 8 đến 255 ký tự.';
        if (!/[a-z]/i.test(value) || !/[0-9]/.test(value))
            return 'Mật khẩu phải có ít nhất một chữ cái (a-z, A-Z) và một chữ số (0-9).';
        return '';
    };
    // . Đọc action của form để phân biệt đăng ký, đăng nhập, khôi phục, sửa hồ sơ và đổi mật khẩu.
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
            // . Thêm nút mắt bên phải mỗi ô mật khẩu; bấm mắt đổi kiểu password/text.
            // .. Nút type=button nên không gửi form và không đổi mật khẩu trong database.
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
            // . Gắn nội dung lỗi dưới đúng ô nhập và bật viền đỏ.
            // .. Không còn lỗi thì bỏ viền đỏ và xóa thông báo của ô đó.
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
                // . Kiểm tra từng trường của form đang thao tác; bỏ qua ô bị khóa hoặc không được sửa.
            // .. Trả thông báo khi sai, trả chuỗi rỗng khi hợp lệ; chưa gửi dữ liệu lên server.
            const validate = field => {
                if (field.disabled || field.readOnly || ['submit', 'button'].includes(field.type)) return '';
                const name = field.name || field.id;
                const raw = field.value;
                const value = raw.trim();
                if (serverErrors.has(name)) return serverErrors.get(name);
                // . Ngày sinh để trống cả ba hoặc chọn đủ ngày/tháng/năm; ngày phải tồn tại, từ 1900 và không ở tương lai.
                if (['birthDay', 'birthMonth', 'birthYear'].includes(name)) {
                    const parts = ['birthDay', 'birthMonth', 'birthYear'].map(part => find(part).value);
                    const count = parts.filter(Boolean).length;
                    if (count && count !== 3) return 'Vui lòng chọn đủ ngày, tháng và năm sinh.';
                    if (count === 3) {
                        const [day, month, year] = parts.map(Number);
                        const date = new Date(year, month - 1, day);
                        if (year < 1900 || date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day || date > new Date())
                            return 'Ngày sinh không hợp lệ hoặc ở trong tương lai.';
                    }
                    return '';
                }
                // . Trường bắt buộc không được để trống; profilePassword chỉ cần nhập khi đổi email.
                if (field.required && !value) return 'Vui lòng nhập thông tin này.';
                if (name === 'profilePassword' && find('email').value.trim().toLowerCase() !== form.dataset.originalEmail && !raw) return 'Vui lòng nhập mật khẩu hiện tại để thay đổi email.';
                if (!raw && !field.required) return '';
                // . Họ tên 1–100 ký tự; SĐT đúng 10 số; email đúng định dạng và tối đa 150 ký tự.
                if (['fullname', 'fullName'].includes(name) && (!value || value.length > 100)) return 'Họ và tên phải từ 1 đến 100 ký tự.';
                if (name === 'phone' && !/^0[0-9]{9}$/.test(value.replace(/[\s.()-]/g, ''))) return 'Số điện thoại sai định dạng.';
                if (name === 'email' && (value.length > 150 || !/^[A-Za-z0-9_+%-]+(?:\.[A-Za-z0-9_+%-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$/.test(value))) return 'Địa chỉ email không hợp lệ hoặc dài quá 150 ký tự.';
                if ((name === 'password' && key !== 'login') || name === 'newPassword') return passwordError(raw);
                // . Xác nhận đối chiếu với mật khẩu mới của đúng form; không so với mật khẩu cũ trong DB.
                if (name === 'confirm_password' || name === 'confirmPassword') {
                    const password = find(key === 'profile-password' ? 'newPassword' : 'password');
                    return raw !== password.value ? 'Mật khẩu xác nhận không khớp.' : '';
                }
                // . OTP phải có 6 chữ số; địa chỉ tối đa 500 ký tự. Mã OTP đúng hay sai do server xác minh.
                if (name === 'otp' && !/^[0-9]{6}$/.test(raw)) return 'Mã OTP phải gồm đúng 6 chữ số.';
                if (name === 'address' && value.length > 500) return 'Địa chỉ tối đa 500 ký tự.';
                if (field.maxLength > 0 && raw.length > field.maxLength) return `Thông tin này tối đa ${field.maxLength} ký tự.`;
                return '';
            };
            if (key === 'profile-update') form.dataset.originalEmail = find('email').value.trim().toLowerCase();
            const normalizeContact = field => field.name === 'email' ? field.value.trim().toLowerCase() : field.value.trim().replace(/[\s.()-]/g, '');
            const contactState = new Map();
            const contactTimers = new Map();
            let checkingSubmit = false;
                // . Khi người dùng nhập email/SĐT ở form đăng ký hoặc hồ sơ, lấy giá trị đúng định dạng để kiểm tra trùng.
             // .. Việc kiểm tra được trì hoãn ngắn sau lần nhập cuối; chưa lưu dữ liệu.
             // ... fetch POST /account-availability -> AccountAvailabilityController.doPost().
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
                        // .. Nhận exists từ server; trùng thì báo đỏ email/SĐT, phản hồi của giá trị đã bị sửa sẽ bị bỏ qua.
                        // ... feedback() hiển thị lỗi dưới trường vừa kiểm tra.
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
                // . Chưa bấm submit thì chưa tô đỏ; sau submit, sửa trường nào sẽ xóa lỗi cũ và kiểm tra lại trường đó.
                const changed = () => {
                    serverErrors.delete(field.name || field.id);
                    if (submitted && ['email', 'phone'].includes(field.name)) {
                        contactState.delete(field.name);
                        clearTimeout(contactTimers.get(field.name));
                        contactTimers.set(field.name, setTimeout(() => checkContact(field), 500));
                    }
                    if (field.name === 'email' && key === 'profile-update') serverErrors.delete('profilePassword');
                    refresh();
                };
                field.addEventListener('input', changed);
                field.addEventListener('change', changed);
            });
            // . Nhận thao tác bấm Đăng nhập/Tạo tài khoản mới/Gửi mã OTP/Xác nhận OTP/Lưu mật khẩu/Lưu thay đổi.
            // . Chặn gửi ngay, kiểm tra các trường và chờ kiểm tra trùng email/SĐT nếu form cần.
            // .. Có lỗi thì giữ form, tô đỏ và focus ô sai đầu tiên; hết lỗi mới gửi đến action của form.
            // ... [RG02] Form đăng ký -> POST /register -> RegisterController.doPost().
            // ... [LG02] Form đăng nhập -> POST /login -> LoginController.doPost().
            // ... [OT02] Form email khôi phục -> POST /forgot-password -> ForgotPasswordController.doPost().
            // ... [OT05] Form OTP -> POST /verify-otp -> VerifyOtpController.doPost().
            // ... [OT07] Form mật khẩu khôi phục -> POST /reset-password -> ResetPasswordController.doPost().
            // ... [PF03] Form hồ sơ/đổi mật khẩu -> POST /profile với action tương ứng -> ProfileController.doPost().
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
        // . Đọc lỗi và dữ liệu điền lại từ account_feedback.jsp; mở đúng form nếu đang bị thu gọn.
        // .. Chỉ điền lại dữ liệu an toàn, không điền lại mật khẩu/OTP.
        // ... feedback() gắn lỗi vào đúng trường.
        document.querySelectorAll('[data-account-feedback]').forEach(payload => {
            const key = payload.dataset.form;
            const form = forms.get(key);
            if (!form) return;
            if (key === 'profile-update') document.getElementById('btnEditProfile')?.click();
            if (key === 'profile-password' && window.bootstrap) bootstrap.Collapse.getOrCreateInstance(document.getElementById('passwordCollapse'), {toggle: false}).show();
            const api = form.accountFeedback;
            payload.querySelectorAll('[data-value-field]').forEach(item => {
                const field = api.find(item.dataset.valueField);
                if (field) { field.value = item.dataset.value; field.dispatchEvent(new Event('input', {bubbles: true})); }
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
