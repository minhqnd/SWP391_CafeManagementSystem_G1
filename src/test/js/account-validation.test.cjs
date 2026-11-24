// Run with Node and Playwright available in NODE_PATH. Uses actual form markup, mocked HTTP, no DB.
const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const path = require('node:path');
const {chromium} = require('playwright');
const webRoot = path.resolve(__dirname, '../../main/webapp');
const auth = readFileSync(path.join(webRoot, 'components/auth_modal.jsp'), 'utf8');
const profile = readFileSync(path.join(webRoot, 'profile.jsp'), 'utf8');
const script = path.join(webRoot, 'assets/js/account-validation.js');
function actualForm(endpoint) {
    const action = auth.indexOf(`request.contextPath}/${endpoint}"`);
    assert.ok(action > 0, `Form ${endpoint} exists`);
    const start = auth.lastIndexOf('<form', action);
    return auth.slice(start, auth.indexOf('</form>', action) + 7)
        .replace(/<%[\s\S]*?%>/g, '')
        .replace(/\$\{pageContext.request.contextPath\}/g, '/cafe');
}
function actualProfilePasswordForm() {
    const start = profile.indexOf('<form id="passwordForm"');
    assert.ok(start > 0);
    return profile.slice(start, profile.indexOf('</form>', start) + 7)
        .replace(/<%[\s\S]*?%>/g, '').replace(/\$\{pageContext.request.contextPath\}/g, '/cafe');
}
async function fixture(run) {
    const browser = await chromium.launch({channel: 'msedge', headless: true});
    try {
        const page = await browser.newPage();
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        let lookups = 0;
        let registrations = 0;
        await page.route('http://account.test/**', async route => {
            const request = route.request();
            if (request.url().endsWith('/account-availability')) {
                lookups++;
                const values = new URLSearchParams(request.postData());
                const exists = ['used@gmail.com', '0987111222'].includes(values.get('value'));
                await route.fulfill({json: {exists}});
            } else if (request.method() === 'POST') {
                registrations++;
                await route.fulfill({body: '<p>submitted</p>'});
            } else {
                const forms = ['register', 'login', 'forgot-password', 'verify-otp', 'reset-password'].map(actualForm).join('') + actualProfilePasswordForm();
                await route.fulfill({contentType: 'text/html', body: `<!doctype html><html><body>${forms}</body></html>`});
            }
        });
        await page.goto('http://account.test/cafe/');
        await page.addScriptTag({path: script});
        const register = page.locator('form[action="/cafe/register"]');
        await run({page, register, counts: () => ({lookups, registrations})});
        assert.deepEqual(errors, []);
    } finally { await browser.close(); }
}
test('waits until submit, reports every duplicate and password error, then clears corrected fields', async () => {
    await fixture(async ({page, register, counts}) => {
        await register.locator('[name=fullname]').fill('Demo User');
        await register.locator('[name=phone]').fill('0987111222');
        await register.locator('[name=email]').fill('used@gmail.com');
        await register.locator('[name=password]').fill('12345678');
        await register.locator('[name=confirm_password]').fill('12345678');
        await register.locator('[name=email]').focus();
        await page.waitForTimeout(600);
        assert.equal(await register.locator('.is-invalid').count(), 0);
        assert.equal(counts().lookups, 0);
        await register.locator('button[type=submit]').click();
        await page.waitForFunction(() => document.querySelector('#account-register-email-error')?.textContent.includes('tồn tại'));
        assert.match(await page.locator('#account-register-email-error').textContent(), /Email đã tồn tại/);
        assert.match(await page.locator('#account-register-phone-error').textContent(), /Số điện thoại đã tồn tại/);
        assert.match(await page.locator('#account-register-password-error').textContent(), /chữ cái/);
        assert.equal(await register.locator('.is-invalid').count(), 3);
        assert.equal(counts().registrations, 0);
        await register.locator('[name=email]').fill('fresh@gmail.com');
        await register.locator('[name=phone]').fill('0987333444');
        await register.locator('[name=password]').fill('Abcd1234');
        await register.locator('[name=confirm_password]').fill('Abcd1234');
        await page.waitForTimeout(700);
        assert.equal(await register.locator('.is-invalid').count(), 0);
        await register.locator('button[type=submit]').click();
        await page.waitForURL('**/register');
        assert.equal(counts().registrations, 1);
    });
});
test('empty registration shows all five errors and password eyes never submit the form', async () => {
    await fixture(async ({register, page, counts}) => {
        await register.locator('button[type=submit]').click();
        assert.equal(await register.locator('.is-invalid').count(), 5);
        const allPasswordFields = page.locator('[data-password-field]');
        const toggles = page.locator('.account-password-toggle');
        assert.equal(await allPasswordFields.count(), 8);
        assert.equal(await toggles.count(), 8);
        for (let i = 0; i < await toggles.count(); i++) {
            const toggle = toggles.nth(i);
            const fieldId = await toggle.getAttribute('aria-controls');
            await toggle.click();
            assert.equal(await page.locator(`#${fieldId}`).getAttribute('type'), 'text');
            assert.equal(await toggle.getAttribute('aria-pressed'), 'true');
            await toggle.click();
            assert.equal(await page.locator(`#${fieldId}`).getAttribute('type'), 'password');
        }
        assert.equal(counts().registrations, 0);
    });
});
test('all new-password forms reject short, letters-only and numbers-only passwords', async () => {
    await fixture(async ({page, register, counts}) => {
        const reset = page.locator('form[action="/cafe/reset-password"]');
        const change = page.locator('#passwordForm');
        await register.locator('[name=fullname]').fill('Demo User');
        await register.locator('[name=phone]').fill('0987333444');
        await register.locator('[name=email]').fill('fresh@gmail.com');
        await change.locator('[name=oldPassword]').fill('Current123');
        for (const password of ['Abc1234', 'abcdefgh', '12345678']) {
            for (const [form, main, confirmation, key] of [
                [register, 'password', 'confirm_password', 'register'],
                [reset, 'password', 'confirm_password', 'reset-password'],
                [change, 'newPassword', 'confirmPassword', 'profile-password']
            ]) {
                await form.locator(`[name=${main}]`).fill(password);
                await form.locator(`[name=${confirmation}]`).fill(password);
                await form.locator('button[type=submit]').click();
                const message = await page.locator(`#account-${key}-${main}-error`).textContent();
                assert.match(message, password === 'Abc1234' ? /8 đến 255/ : /chữ cái/);
            }
        }
        assert.equal(counts().registrations, 0);
    });
});
test('phone allows typing extra digits and reports an invalid format', async () => {
    await fixture(async ({page, register, counts}) => {
        await register.locator('[name=fullname]').fill('Demo User');
        await register.locator('[name=email]').fill('fresh@gmail.com');
        await register.locator('[name=password]').fill('Abcd1234');
        await register.locator('[name=confirm_password]').fill('Abcd1234');
        const phone = register.locator('[name=phone]');
        assert.equal(await phone.getAttribute('maxlength'), null);
        for (const invalid of ['098711122', '09871112223', '09871112a2']) {
            await phone.evaluate((field, value) => {field.value = value; field.dispatchEvent(new Event('input', {bubbles:true}));}, invalid);
            await register.locator('button[type=submit]').click();
            assert.match(await page.locator('#account-register-phone-error').textContent(), /sai định dạng/);
            assert.equal(counts().registrations, 0);
        }
        await phone.fill('0987333444');
        await register.locator('button[type=submit]').click();
        await page.waitForURL('**/register');
        assert.equal(counts().registrations, 1);
    });
});
test('register and recovery reject mistyped Gmail domains; uppercase Gmail remains valid', async () => {
    await fixture(async ({page, register, counts}) => {
        await register.locator('[name=fullname]').fill('Demo User');
        await register.locator('[name=phone]').fill('0987333444');
        await register.locator('[name=password]').fill('Abcd1234');
        await register.locator('[name=confirm_password]').fill('Abcd1234');
        const forgot = page.locator('form[action="/cafe/forgot-password"]');
        for (const value of ['nguyen@gmaill.comm', 'nguyen@gmail.comm', 'nguyen@gmail.com.evil.com', 'nguyen@yahoo.com']) {
            for (const [form, key] of [[register, 'register'], [forgot, 'forgot-password']]) {
                await form.locator('[name=email]').fill(value);
                await form.locator('button[type=submit]').click();
                await page.waitForFunction(key => document.getElementById(`account-${key}-email-error`)?.textContent === 'Sai định dạng email.', key);
                assert.match(await page.locator(`#account-${key}-email-error`).textContent(), /Sai định dạng email\./,
                    `${key}: ${value}; current input=${await form.locator('[name=email]').inputValue()}`);
                assert.equal(counts().registrations, 0);
            }
        }
        await forgot.locator('[name=email]').fill('NGUYEN@GMAIL.COM');
        await forgot.locator('button[type=submit]').click();
        await page.waitForURL('**/forgot-password');
        assert.equal(counts().registrations, 1);
    });
});

test('login and current-password length errors match the server messages', async () => {
    await fixture(async ({page, counts}) => {
        const login = page.locator('form[action="/cafe/login"]');
        const change = page.locator('#passwordForm');
        await login.locator('[name=username]').fill('qa@gmail.com');
        await change.locator('[name=newPassword]').fill('Secret123');
        await change.locator('[name=confirmPassword]').fill('Secret123');
        for (const [form, name, key, expected] of [
            [login, 'password', 'login', 'Mật khẩu không được vượt quá 255 ký tự.'],
            [change, 'oldPassword', 'profile-password', 'Mật khẩu hiện tại tối đa 255 ký tự.']
        ]) {
            await form.locator(`[name=${name}]`).evaluate(field => { field.value = 'x'.repeat(256); });
            await form.locator('button[type=submit]').click();
            assert.equal(await page.locator(`#account-${key}-${name}-error`).textContent(), expected);
        }
        assert.equal(counts().registrations, 0);
    });
});

test('empty OTP and malformed OTP have different messages and never submit', async () => {
    await fixture(async ({page, counts}) => {
        const form = page.locator('form[action="/cafe/verify-otp"]');
        for (const [value, expected] of [['', 'Vui lòng nhập mã OTP.'], ['   ', 'Vui lòng nhập mã OTP.'], ['12a456', 'Mã OTP phải gồm đúng 6 chữ số.']]) {
            await form.locator('[name=otp]').fill(value);
            await form.locator('button[type=submit]:not([form])').click();
            assert.equal(await page.locator('#account-verify-otp-otp-error').textContent(), expected);
        }
        assert.equal(counts().registrations, 0);
    });
});

test('changing password clears an obsolete confirmation error returned from the server', async () => {
    await fixture(async ({page, register}) => {
        await page.evaluate(() => {
            const form = document.querySelector('form[action="/cafe/register"]');
            const api = form.accountFeedback;
            form.elements.password.value = 'OldSecret123';
            form.elements.confirm_password.value = 'NewSecret123';
            api.serverErrors.set('confirm_password', 'Mật khẩu xác nhận không khớp.');
            api.markSubmitted(); api.refresh();
        });
        assert.equal(await register.locator('[name=confirm_password]').getAttribute('aria-invalid'), 'true');
        await register.locator('[name=password]').fill('NewSecret123');
        assert.equal(await register.locator('[name=confirm_password]').getAttribute('aria-invalid'), 'false');
        assert.equal(await page.locator('#account-register-confirm_password-error').textContent(), '');
    });
});

test('registration blank-looking fields use the same required messages as the server', async () => {
    await fixture(async ({page, register, counts}) => {
        await register.locator('[name=fullname]').fill('\u00a0');
        await register.locator('[name=email]').fill('\ufeff');
        await register.locator('[name=phone]').fill('().-');
        await register.locator('[name=password]').fill('Secret123');
        await register.locator('[name=confirm_password]').fill('\u2003');
        await register.locator('button[type=submit]').click();
        for (const [name, expected] of [['fullname', 'Vui lòng nhập họ và tên.'], ['email', 'Vui lòng nhập email.'],
            ['phone', 'Vui lòng nhập số điện thoại.'], ['confirm_password', 'Vui lòng nhập mật khẩu xác nhận.']])
            assert.equal(await page.locator(`#account-register-${name}-error`).textContent(), expected);
        assert.equal(counts().registrations, 0);
        assert.equal(counts().lookups, 0);
    });
});

test('registration password rejects 7 and 256 characters and accepts exact 8 and 255 boundaries', async () => {
    await fixture(async ({page, register, counts}) => {
        await register.locator('[name=fullname]').fill('QA');
        await register.locator('[name=email]').fill('bad'); // Keeps the page open for the matrix.
        await register.locator('[name=phone]').fill('0965999999');
        await register.locator('button[type=submit]').click();
        for (const size of [7, 8, 254, 255, 256]) {
            const value = 'A1' + 'x'.repeat(size - 2);
            await register.locator('[name=password]').evaluate((field, value) => {
                field.value = value; field.dispatchEvent(new Event('input', {bubbles: true}));
            }, value);
            await register.locator('[name=confirm_password]').evaluate((field, value) => {
                field.value = value; field.dispatchEvent(new Event('input', {bubbles: true}));
            }, value);
            assert.equal(await page.locator('#account-register-password-error').textContent(), size < 8 || size > 255 ? 'Mật khẩu phải từ 8 đến 255 ký tự.' : '');
        }
        assert.equal(counts().registrations, 0);
    });
});
