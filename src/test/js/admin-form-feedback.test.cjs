// Checks the real validation script against browser form events; HTTP is mocked, no DB writes.
const {test} = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const {readFileSync} = require('node:fs');
const {chromium} = require('playwright');
const webRoot = path.resolve(__dirname, '../../main/webapp');
const script = path.join(webRoot, 'assets/js/admin-form-validation.js');

async function fixture(markup, run) {
    const browser = await chromium.launch({channel: 'msedge', headless: true});
    try {
        const page = await browser.newPage();
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        await page.setContent(markup);
        await page.addScriptTag({path: script});
        await page.evaluate(() => {
            window.sent = 0;
            document.querySelectorAll('form').forEach(form => form.addEventListener('submit', event => {
                if (!event.defaultPrevented) window.sent++;
                event.preventDefault();
            }));
        });
        await run(page);
        assert.deepEqual(errors, []);
    } finally { await browser.close(); }
}

const productForm = `<form data-admin-form="product" novalidate>
    <input name="name" value="Cà phê"><select name="categoryId"><option value="1">Cà phê</option></select>
    <input name="basePrice" value="25000"><textarea name="description"></textarea><input name="imageFile" type="file">
    <div id="sizesList"><div class="variant-row"><input name="sizeName" value="M"><input name="sizeDelta" value="0"></div>
    <div class="variant-row"><input name="sizeName" value=""><input name="sizeDelta" value="1"></div></div>
    <button id="addSize" type="button">Thêm size</button><div id="toppingsList"></div><button type="submit">Lưu</button>
    </form>`;

test('product errors identify missing fields in row 2, clear when corrected and permit zero surcharge', async () => {
    await fixture(productForm, async page => {
        const submit = page.locator('button[type=submit]');
        await submit.click();
        assert.equal(await page.locator('.invalid-feedback.d-block').textContent(), 'Vui lòng nhập tên size dòng 2.');
        assert.equal(await page.evaluate(() => window.sent), 0);
        await page.locator('[name=sizeName]').nth(1).fill('L');
        await page.locator('[name=sizeDelta]').nth(1).fill('');
        assert.equal(await page.locator('.invalid-feedback.d-block').textContent(), 'Vui lòng nhập phụ thu size dòng 2; nhập 0 nếu không phụ thu.');
        await page.locator('[name=sizeDelta]').nth(1).fill('0');
        assert.equal(await page.locator('.is-invalid').count(), 0);
        await submit.click();
        assert.equal(await page.evaluate(() => window.sent), 1);
    });
});

test('selected empty images are rejected before submit in product and category forms', async () => {
    for (const kind of ['product', 'category']) {
        const markup = kind === 'product' ? productForm
            : '<form data-admin-form="category" novalidate><input name="name" value="Cà phê"><select name="status"><option>ACTIVE</option></select><input type="file" name="imageFile"><button type="submit">Lưu</button></form>';
        await fixture(markup, async page => {
            if (kind === 'product') await page.locator('[name=sizeName]').nth(1).fill('L');
            await page.locator('[name=imageFile]').setInputFiles({name: 'empty.png', mimeType: 'image/png', buffer: Buffer.alloc(0)});
            await page.locator('button[type=submit]').click();
            assert.equal(await page.locator('.invalid-feedback.d-block').textContent(), 'Tệp ảnh đã chọn trống. Vui lòng chọn ảnh khác.');
            assert.equal(await page.evaluate(() => window.sent), 0);
        });
    }
});

test('recipe duplicate errors and the previous notice disappear after removing the duplicate row', async () => {
    const row = '<div class="ingredient-row"><select name="ingredientId"><option value="1">Sữa</option></select><input name="quantity" value="1"><button type="button" class="remove">×</button></div>';
    await fixture(`<div data-form-notice class="alert-danger">Dòng 2: nguyên liệu bị chọn trùng.</div>
        <form data-admin-form="recipe" novalidate>${row}${row}<button id="addIngredient" type="button">Thêm</button><button type="submit">Lưu</button></form>`, async page => {
        await page.locator('button[type=submit]').click();
        assert.match(await page.locator('.invalid-feedback.d-block').textContent(), /Dòng 2: nguyên liệu bị chọn trùng/);
        await page.locator('.remove').nth(1).evaluate(button => button.addEventListener('click', () => button.closest('.ingredient-row').remove()));
        await page.locator('.remove').nth(1).click();
        assert.equal(await page.locator('[data-form-notice]').isHidden(), true);
        assert.equal(await page.locator('.is-invalid').count(), 0);
        await page.locator('button[type=submit]').click();
        assert.equal(await page.evaluate(() => window.sent), 1);
    });
});

test('phone lookup blocks blank and malformed values and clears the old lookup notice on correction', async () => {
    const css = readFileSync(path.join(webRoot, 'admin-loyalty.jsp'), 'utf8').match(/<style>([\s\S]*?)<\/style>/)[1];
    await fixture(`<style>${css}</style><div data-lookup-notice>Không tìm thấy khách hàng.</div><div data-form-notice>Lỗi lưu quy tắc khác</div>
        <form data-admin-form="loyalty-lookup" class="search-form" novalidate><input name="phone" class="loyalty-input is-invalid"><button type="submit">Tra cứu</button></form>`, async page => {
        const phone = page.locator('[name=phone]');
        for (const [value, expected] of [['', 'Vui lòng nhập số điện thoại.'], ['096599999', 'Số điện thoại sai định dạng.'], ['09659999999', 'Số điện thoại sai định dạng.'], ['abc', 'Số điện thoại sai định dạng.'], ['0'.repeat(65), 'Số điện thoại tối đa 64 ký tự.']]) {
            await phone.fill(value);
            await page.locator('button').click();
            assert.equal(await page.locator('.invalid-feedback.d-block').textContent(), expected);
            assert.equal(await page.evaluate(() => window.sent), 0);
            const inputBox = await phone.boundingBox();
            const errorBox = await page.locator('.invalid-feedback.d-block').boundingBox();
            assert.ok(errorBox.y >= inputBox.y + inputBox.height, 'The phone error appears below the input');
        }
        await phone.fill('(096) 599-9999');
        assert.equal(await phone.getAttribute('aria-invalid'), 'false');
        assert.equal(await page.locator('[data-lookup-notice]').isHidden(), true);
        assert.equal(await page.locator('[data-form-notice]').isVisible(), true);
        await page.locator('button').click();
        assert.equal(await page.evaluate(() => window.sent), 1);
    });
});

test('point prompt uses the same required, integer and range errors as the server', async () => {
    const jsp = readFileSync(path.join(webRoot, 'admin-loyalty.jsp'), 'utf8');
    const inline = [...jsp.matchAll(/<script>([\s\S]*?)<\/script>/g)].at(-1)[1];
    await fixture('<form id="adjustPointsForm"><input id="adjustType"><input id="adjustPoints"></form>', async page => {
        await page.addScriptTag({content: inline});
        const cases = [
            ['', 'Vui lòng nhập số điểm điều chỉnh.'], ['\u00a0', 'Vui lòng nhập số điểm điều chỉnh.'],
            ['1.5', 'Số điểm điều chỉnh phải là số nguyên từ 1 đến 1000000.'], ['-1', 'Số điểm điều chỉnh phải là số nguyên từ 1 đến 1000000.'],
            ['0', 'Số điểm điều chỉnh phải từ 1 đến 1000000.'], ['1000001', 'Số điểm điều chỉnh phải từ 1 đến 1000000.'],
            ['9223372036854775808', 'Số điểm điều chỉnh phải từ 1 đến 1000000.'], ['1', null], ['999999', null], ['1000000', null]
        ];
        const result = await page.evaluate(cases => {
            const results = [];
            const form = document.getElementById('adjustPointsForm');
            for (const [raw] of cases) {
                let message = null, sent = false;
                window.prompt = () => raw;
                window.alert = value => message = value;
                form.submit = () => sent = true;
                submitAdjustment('subtract');
                results.push({message, sent, type: document.getElementById('adjustType').value});
            }
            return results;
        }, cases);
        cases.forEach(([raw, expected], index) => {
            assert.equal(result[index].message, expected, raw);
            assert.equal(result[index].sent, expected === null, raw);
            if (expected === null) assert.equal(result[index].type, 'subtract');
        });
    });
});

test('dynamic rows accept 50 size or topping rows and reject row 51; recipes reject row 101', async () => {
    await fixture(productForm, async page => {
        await page.locator('[name=sizeName]').nth(1).fill('L');
        for (const type of ['size', 'topping']) {
            for (const count of [49, 50, 51]) {
                await page.evaluate(({type, count}) => {
                    const container = document.getElementById(type === 'size' ? 'sizesList' : 'toppingsList');
                    container.replaceChildren();
                    for (let i = 0; i < count; i++) {
                        const row = document.createElement('div'); row.className = 'variant-row';
                        row.innerHTML = `<input name="${type}Name" value="N${i}"><input name="${type === 'size' ? 'sizeDelta' : 'toppingPrice'}" value="0">`;
                        container.append(row);
                    }
                }, {type, count});
                await page.locator('button[type=submit]').click();
                assert.equal(await page.locator('.invalid-feedback.d-block').count(), count > 50 ? 1 : 0);
                if (count > 50) assert.equal(await page.locator('.invalid-feedback.d-block').textContent(), `Không được thêm quá 50 ${type}.`);
            }
            await page.evaluate(() => document.getElementById('sizesList').lastElementChild.remove());
        }
        assert.equal(await page.evaluate(() => window.sent), 4);
    });
    await fixture('<form data-admin-form="recipe" novalidate><div id="rows"></div><button id="addIngredient" type="button">Thêm</button><button type="submit">Lưu</button></form>', async page => {
        for (const count of [99,100,101]) {
            await page.evaluate(count => {
                const rows = document.getElementById('rows'); rows.replaceChildren();
                for (let i = 0; i < count; i++) {
                    const row = document.createElement('div'); row.className = 'ingredient-row';
                    row.innerHTML = `<select name="ingredientId"><option value="${i + 1}">Nguyên liệu ${i + 1}</option></select><input name="quantity" value="0.001">`;
                    rows.append(row);
                }
            }, count);
            await page.locator('button[type=submit]').click();
            assert.equal(await page.locator('.invalid-feedback.d-block').count(), count > 100 ? 1 : 0);
            if (count > 100) assert.equal(await page.locator('.invalid-feedback.d-block').textContent(), 'Không được thêm quá 100 dòng nguyên liệu.');
        }
        assert.equal(await page.evaluate(() => window.sent), 2);
    });
});

test('cancel and blank adjustment never send a request; blank input has a required message', async () => {
    const jsp = readFileSync(path.join(webRoot, 'admin-loyalty.jsp'), 'utf8');
    const inline = [...jsp.matchAll(/<script>([\s\S]*?)<\/script>/g)].at(-1)[1];
    await fixture('<form id="adjustPointsForm"><input id="adjustType"><input id="adjustPoints"></form>', async page => {
        await page.addScriptTag({content: inline});
        const result = await page.evaluate(() => {
            const messages = [];
            const form = document.getElementById('adjustPointsForm');
            form.submit = () => window.sent++;
            window.alert = message => messages.push(message);
            window.prompt = () => null;
            submitAdjustment('add');
            window.prompt = () => '   ';
            submitAdjustment('subtract');
            return {messages, sent: window.sent};
        });
        assert.deepEqual(result, {messages: ['Vui lòng nhập số điểm điều chỉnh.'], sent: 0});
    });
});
