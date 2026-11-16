/* Dùng chung kiểm tra trường và hiển thị lỗi cho các form BF10; server vẫn là nơi xác nhận cuối cùng. */
(() => {
    'use strict';
    // . Kiểm tra mật khẩu mới 8–255 ký tự, có chữ và số tại trình duyệt.
    // .. Trả chuỗi rỗng nếu hợp lệ; không áp quy tắc tạo mật khẩu mới cho ô đăng nhập.
    // ... Server kiểm tra lại bằng AccountValidation.passwordError().
    const passwordError = value => {
        if (value.length < 8 || value.length > 255 || !value.trim())
            return 'Mật khẩu phải từ 8 đến 255 ký tự.';
        if (/[\u0000-\u001F\u007F]/.test(value))
            return 'Mật khẩu không được chứa ký tự điều khiển.';
        if (!/[a-z]/i.test(value) || !/[0-9]/.test(value))
            return 'Mật khẩu phải có ít nhất một chữ cái (a-z, A-Z) và một chữ số (0-9).';
        return '';
    };
    // . Đọc action của form để nhận diện đăng ký, đăng nhập, khôi phục, hồ sơ hoặc đổi mật khẩu.
    // .. Chỉ gắn xử lý chung cho các form BF10 được liệt kê trong init().
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
            // . Tạo một nút mắt cho từng ô mật khẩu; bỏ nút toggle cũ trong nhóm nếu có.
            // .. Bấm mắt chỉ đổi password/text, không gửi form hoặc lưu dữ liệu.
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
            // . Gắn thông báo dưới đúng trường và bật viền đỏ khi có lỗi.
            // .. Không còn lỗi thì xóa thông báo, bỏ viền đỏ; đồng bộ aria-invalid và aria-describedby.
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
            // . Kiểm tra từng trường theo tên trong form; bỏ qua ô bị khóa và nút bấm.
            // .. Trả thông báo lỗi hoặc chuỗi rỗng; server vẫn kiểm tra lại khi nhận POST.
            // ... refresh() gọi feedback() trong account-validation.js; submit có lỗi thì dừng tại sự kiện submit trong account-validation.js.
            const validate = field => {
                if (field.disabled || field.readOnly || ['submit', 'button'].includes(field.type)) return '';
                const name = field.name || field.id;
                const raw = field.value;
                const value = raw.trim();
                if (serverErrors.has(name)) return serverErrors.get(name);
                // . Kiểm tra ba ô ngày sinh từ [PF06d]: cùng trống hoặc đủ ba phần, ngày thật và không ở tương lai.
                // ... ProfileController.update() [PF12d] kiểm tra lại ngày sau khi gửi.
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
                // . Trường required không được rỗng; đổi email hồ sơ thì yêu cầu profilePassword.
                // .. Trường không bắt buộc và để trống được bỏ qua các bước kiểm tra tiếp theo.
                if (field.required && !value) return 'Vui lòng nhập thông tin này.';
                if (name === 'profilePassword' && find('email').value.trim().toLowerCase() !== form.dataset.originalEmail && !raw) return 'Vui lòng nhập mật khẩu hiện tại để thay đổi email.';
                if (!raw && !field.required) return '';
                if (name === 'username' && value.length > 150) return 'Email hoặc số điện thoại tối đa 150 ký tự.';
                // . Họ tên đăng ký [RG02a]/hồ sơ [PF06a] có từ 1 đến 100 ký tự sau trim.
                // ... AccountValidation.nameError() xác nhận lại tại server.
                if (['fullname', 'fullName'].includes(name)
                        && (!value || value.length > 100 || /[\u0000-\u001F\u007F]/.test(value))) {
                    return 'Họ và tên phải từ 1 đến 100 ký tự và không chứa ký tự điều khiển.';
                }
                // . SĐT từ [RG02b]/[PF06b] sau khi bỏ dấu phân cách phải bắt đầu bằng 0 và đủ 10 số.
                // ... AccountValidation.phoneError() kiểm tra lại tại server.
                if (name === 'phone' && !/^0[0-9]{9}$/.test(value.replace(/[\s.()-]/g, ''))) return 'Số điện thoại sai định dạng.';
                // . Email từ [RG02c]/[PF06c]/[OT02a] phải đúng định dạng và tối đa 150 ký tự.
                // ... AccountValidation.emailError() kiểm tra lại; kiểm tra trùng qua checkContact() trong account-validation.js.
                if (name === 'email' && (value.length > 150 || !/^[A-Za-z0-9_+%-]+(?:\.[A-Za-z0-9_+%-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$/.test(value))) return 'Địa chỉ email không hợp lệ hoặc dài quá 150 ký tự.';
                if ((name === 'password' && key !== 'login') || name === 'newPassword') return passwordError(raw);
                // . So xác nhận với mật khẩu mới trong cùng form; không so với mật khẩu cũ trong database.
                // .. Khác nhau thì báo lỗi ngay dưới ô xác nhận.
                if (name === 'confirm_password' || name === 'confirmPassword') {
                    const password = find(key === 'profile-password' ? 'newPassword' : 'password');
                    return raw !== password.value ? 'Mật khẩu xác nhận không khớp.' : '';
                }
                // . OTP phải gồm đúng 6 chữ số; trình duyệt chỉ kiểm tra định dạng.
                // ... PasswordRecoveryService.verify() [OT11] kiểm tra mã đúng/sai và thời hạn.
                if (name === 'otp' && !/^[0-9]{6}$/.test(raw)) return 'Mã OTP phải gồm đúng 6 chữ số.';
                // . Địa chỉ hồ sơ [PF06e] không quá 500 ký tự sau trim.
                // ... ProfileController.update() [PF12e] kiểm tra lại trước khi ghi database.
                if (name === 'address' && value.length > 500) return 'Địa chỉ tối đa 500 ký tự.';
                if (name === 'address' && /[\u0000-\u001F\u007F]/.test(raw)) return 'Địa chỉ chứa ký tự không được hỗ trợ.';
                if (field.maxLength > 0 && raw.length > field.maxLength) return `Thông tin này tối đa ${field.maxLength} ký tự.`;
                return '';
            };
            if (key === 'profile-update') form.dataset.originalEmail = find('email').value.trim().toLowerCase();
            const normalizeContact = field => field.name === 'email' ? field.value.trim().toLowerCase() : field.value.trim().replace(/[\s.()-]/g, '');
            const contactState = new Map();
            const contactTimers = new Map();
            let checkingSubmit = false;
            // RG04b: . Khi submit đăng ký, kiểm tra trùng email/SĐT đúng định dạng trước khi gửi form chính.
            // ... POST /account-availability → Controller [RG04c] → DAO [RG04d]; nhận JSON rồi tiếp tục [RG05].
            // PF10b: . Khi submit hồ sơ, dùng cùng checkContact() với scope=profile.
            // ... Controller [PF10c] loại trừ tài khoản đang sửa; kết quả về để quyết định gửi tiếp [PF11].
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
                        // . Nhận JSON exists; bỏ phản hồi cũ nếu người dùng đã sửa giá trị.
                        // .. Trùng thì gắn lỗi email/phone; không trùng thì xóa lỗi kiểm tra trùng cũ.
                        // ... feedback() trong account-validation.js cập nhật thông báo ngay dưới ô.
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
                // [SLBF10] Correct the information
                // . Khi sửa trường, xóa lỗi server cũ và kiểm tra lại nếu đã từng submit.
                // .. Chưa submit thì chưa tô đỏ; sau submit, email/SĐT được kiểm tra trùng sau 500 ms ngừng nhập.
                // ... checkContact() trong account-validation.js và refresh() → feedback() trong account-validation.js.
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
            // RG04: [SLBF10] Check registration information
            // . Nhận submit đăng ký từ [RG03], kiểm tra các trường và chờ kiểm tra trùng email/SĐT.
            // .. Có lỗi thì giữ form, focus ô sai; hợp lệ mới gửi POST /register tới [RG05].
            // LG04: [SLBF10] Check login information
            // . Nhận submit đăng nhập từ [LG03], kiểm tra dữ liệu bắt buộc và giới hạn trường.
            // ... Hợp lệ gửi POST /login tới LoginController.doPost() [LG05].
            // PF10: [SLBF10] Check updated information
            // . Nhận submit Lưu thay đổi từ [PF09], kiểm tra từng trường và kiểm tra trùng email/SĐT.
            // ... Hợp lệ gửi POST /profile?action=update tới ProfileController.doPost() [PF11].
            // PW03: . Nhận submit đổi mật khẩu từ [PW02], kiểm tra mật khẩu mới và xác nhận.
            // ... Hợp lệ gửi POST /profile?action=changePassword tới Controller [PW04].
            // OT04: . Nhận email khôi phục từ [OT03], kiểm tra email trước khi gửi.
            // ... POST /forgot-password tới ForgotPasswordController.doPost() [OT05].
            // OT09: . Nhận OTP từ [OT08], kiểm tra định dạng 6 chữ số.
            // ... POST /verify-otp tới VerifyOtpController.doPost() [OT10].
            // OT14: . Nhận mật khẩu mới/xác nhận từ [OT13], kiểm tra trường trước khi gửi.
            // ... POST /reset-password tới ResetPasswordController.doPost() [OT15].
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
        // . Đọc payload từ payload trong account_feedback.jsp và mở lại đúng form bị lỗi.
        // .. Chỉ điền các giá trị an toàn được server gửi; không điền lại mật khẩu/OTP.
        // ... serverErrors giữ lỗi theo tên trường → feedback() trong account-validation.js tô đỏ đúng ô.
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
