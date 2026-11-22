(() => {
    'use strict';
    const controlChars = /[\u0000-\u001f\u007f-\u009f]/;
    // . validate gọi requiredName để kiểm tra tên ngay trên màn hình. Em nhận nội dung, tên ô và độ dài
    //   cho phép, rồi trả lời báo nếu trống, quá dài hoặc có ký tự không hỗ trợ; đúng thì trả chuỗi rỗng.
    const requiredName = (raw, label, max) => {
        const value = String(raw ?? '').trim();
        if (!value) return 'Vui lòng nhập ' + label.toLocaleLowerCase('vi') + '.';
        if (value.length > max) return label + ' tối đa ' + max + ' ký tự.';
        return controlChars.test(value) ? label + ' chứa ký tự không được hỗ trợ.' : '';
    };
    // . money và quantity gọi decimal để kiểm tra cách viết số, số âm, giới hạn và phần thập phân.
    //   Em so sánh chính xác cả phần lẻ để không lọt giá trị vượt giới hạn; lời báo trả về có tên ô đang lỗi.
    const decimal = (raw, label, scale, maximum, displayMax, positive = false) => {
        const value = String(raw ?? '').trim();
        if (!value) return 'Vui lòng nhập ' + label.toLocaleLowerCase('vi') + '.';
        if (value.length > 64 || !/^-?[0-9]+(?:\.[0-9]+)?$/.test(value))
            return label + ' phải là số, dùng dấu chấm nếu có phần thập phân.';
        // So sánh bằng số nguyên đã bỏ dấu chấm để giữ chính xác tại giá trị biên.
        const [integer, fraction = ''] = value.replace(/^-/, '').split('.');
        const [maxInteger, maxFraction = ''] = maximum.split('.');
        const places = Math.max(fraction.length, maxFraction.length);
        const normalized = BigInt(integer + fraction.padEnd(places, '0'));
        const max = BigInt(maxInteger + maxFraction.padEnd(places, '0'));
        if ((value.startsWith('-') && normalized !== 0n) || (positive && normalized === 0n))
            return label + (positive ? ' phải lớn hơn 0.' : ' không được âm.');
        if (normalized > max) return label + ' không được vượt quá ' + displayMax + '.';
        if (fraction.length > scale) return label + ' có tối đa ' + scale + ' chữ số thập phân.';
        return '';
    };
    const money = (raw, label) => decimal(raw, label, 2, '9999999999.99', '9.999.999.999,99');
    const quantity = (raw, label) => decimal(raw, label, 3, '999999999.999', '999.999.999,999', true);
    // . Với ba ô quy tắc tích điểm, validate gọi wholeNumber để chỉ nhận số nguyên trong giới hạn của
    //   từng ô. Kết quả là lời báo cho ô sai hoặc chuỗi rỗng để form có thể tiếp tục gửi.
    const wholeNumber = (raw, label, min, max) => {
        const value = String(raw ?? '').trim();
        if (!value) return 'Vui lòng nhập ' + label.toLocaleLowerCase('vi') + '.';
        if (!/^[0-9]+$/.test(value)) return label + ' phải là số nguyên từ ' + min + ' đến ' + max + '.';
        if (value.length > 19 || BigInt(value) < BigInt(min) || BigInt(value) > BigInt(max))
            return label + ' phải từ ' + min + ' đến ' + max + '.';
        return '';
    };
    globalThis.AuraAdminValidation = {requiredName, money, quantity, wholeNumber};
    if (typeof document === 'undefined') return;

    // . Em tìm các form danh mục, sản phẩm, công thức và quy tắc theo data-admin-form để chọn cách kiểm
    //   tra phù hợp. Dữ liệu vẫn được kiểm tra lại ở controller khi form gửi đi.
    document.querySelectorAll('form[data-admin-form]').forEach(form => {
        const kind = form.dataset.adminForm;
        const notices = [...document.querySelectorAll('[data-form-notice]')];
        let submitted = notices.some(notice => notice.classList.contains('alert-danger'));
        let serial = 0;
        const field = name => form.elements.namedItem(name);
        // . Sau khi kiểm tra, em gọi mark bằng ô nhập và lời báo để đặt viền đỏ, hiện lỗi ngay dưới ô.
        //   Khi ô đã đúng, lời báo và viền lỗi được bỏ đi; hàm này chỉ đổi hiển thị, chưa lưu dữ liệu.
        const mark = (input, message) => {
            if (!input) return;
            const wasInvalid = input.classList.contains('is-invalid');
            input.classList.toggle('is-invalid', Boolean(message));
            input.setAttribute('aria-invalid', String(Boolean(message)));
            let box = input._validationBox;
            if (!box && kind === 'loyalty') box = document.getElementById(input.getAttribute('aria-describedby'));
            if (!box && !message && !wasInvalid) return;
            if (!box) {
                box = document.createElement('div');
                box.id = 'admin-field-error-' + kind + '-' + (++serial);
                box.className = 'invalid-feedback';
                box.style.gridColumn = '1 / -1';
                box.setAttribute('aria-live', 'polite');
                const row = input.closest('.variant-row');
                if (row) row.append(box);
                else (input.closest('.input-group') || input).after(box);
                input._validationBox = box;
                input.setAttribute('aria-describedby', ((input.getAttribute('aria-describedby') || '') + ' ' + box.id).trim());
            }
            box.textContent = message;
            box.classList.toggle('d-block', Boolean(message));
        };
        const validate = () => {
            const errors = new Map();
            const check = (input, message) => { if (input && message && !errors.has(input)) errors.set(input, message); };
            if (kind === 'category' || kind === 'product') {
                // [DMKT01]
                // . Khi form danh mục gửi, validate đọc tên, trạng thái và ảnh để kiểm tra ngay trên trang;
                //   form sản phẩm dùng chung phần tên và ảnh. Lời báo được giữ theo đúng ô để mark hiển thị.
                //   Có lỗi thì chặn gửi; đúng thì form danh mục đến AdminCategoriesController.doPost để lưu.
                check(field('name'), requiredName(field('name')?.value, kind === 'category' ? 'Tên danh mục' : 'Tên món', kind === 'category' ? 100 : 150));
                if (kind === 'category' && !['ACTIVE', 'INACTIVE'].includes(field('status')?.value))
                    check(field('status'), 'Vui lòng chọn trạng thái Hoạt động hoặc Tạm ẩn.');
                const image = field('imageFile')?.files?.[0];
                if (image?.size > 5 * 1024 * 1024) check(field('imageFile'), 'Ảnh ' + (kind === 'category' ? 'danh mục' : 'sản phẩm') + ' không được lớn hơn 5 MB.');
                else if (image && !['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(image.type))
                    check(field('imageFile'), 'Chỉ chấp nhận ảnh JPG, PNG, GIF hoặc WebP.');
            }
            if (kind === 'product') {
                // [SPL02]
                // . Từ productForm trong admin-products.jsp, em kiểm tra tên, danh mục, giá và từng dòng size,
                //   topping trước khi gửi. Size phải có ít nhất một dòng và một phụ thu 0. Ô sai hiện lỗi ngay
                //   bên dưới; hợp lệ thì form mới đến doPost của AdminProductsController.java để kiểm tra lại và lưu.
                // ... Đọc tiếp tại AdminProductsController.java, tìm [SPL03].
                if (!field('categoryId')?.value) check(field('categoryId'), 'Vui lòng chọn danh mục.');
                check(field('basePrice'), money(field('basePrice')?.value, 'Giá bán cơ bản'));
                if (field('description')?.value.trim().length > 5000) check(field('description'), 'Mô tả không được dài quá 5000 ký tự.');
                for (const [container, nameField, amountField, type, amountLabel, maxName] of [
                    ['sizesList', 'sizeName', 'sizeDelta', 'size', 'Phụ thu size', 30],
                    ['toppingsList', 'toppingName', 'toppingPrice', 'topping', 'Giá topping', 50]
                ]) {
                    const rows = [...form.querySelectorAll('#' + container + ' .variant-row')];
                    const names = new Set();
                    let count = 0, base = false;
                    rows.forEach((row, index) => {
                        const name = row.querySelector('[name="' + nameField + '"]');
                        const amount = row.querySelector('[name="' + amountField + '"]');
                        if (!name.value.trim() && !amount.value.trim()) return;
                        count++;
                        check(name, requiredName(name.value, 'Tên ' + type, maxName));
                        if (/[\\"'{}<>`]/.test(name.value)) check(name, 'Tên ' + type + ' chứa ký tự không được hỗ trợ.');
                        const normalized = name.value.trim().toLowerCase();
                        if (normalized && names.has(normalized)) check(name, 'Không được trùng tên ' + type + '.');
                        names.add(normalized);
                        const amountError = money(amount.value, amountLabel + ' dòng ' + (index + 1));
                        check(amount, amountError);
                        if (!amountError && Number(amount.value) === 0) base = true;
                    });
                    if (rows.length > 50) check(rows[50]?.querySelector('input'), 'Không được thêm quá 50 ' + type + '.');
                    if (type === 'size' && !count) check(document.getElementById('addSize'), 'Sản phẩm cần có ít nhất một size.');
                    else if (type === 'size' && !base && !errors.size) check(rows[0]?.querySelector('[name="sizeDelta"]'), 'Cần có một size phụ thu 0đ để giá mặc định đúng bằng giá bán cơ bản.');
                }
            }
            if (kind === 'recipe') {
                // [CTKT01]
                // . Khi bấm Lưu công thức, validate kiểm tra từng cặp nguyên liệu, định lượng trên form.
                //   Em bỏ qua dòng trống hoàn toàn, chặn dòng nhập thiếu, nguyên liệu trùng, định lượng sai
                //   và quá 100 dòng; lời báo gắn đúng ô.
                //   Qua kiểm tra, form mới gửi đến AdminRecipeController.doPost để kiểm tra lại và lưu.
                const rows = [...form.querySelectorAll('.ingredient-row')];
                const seen = new Set();
                let count = 0;
                rows.forEach((row, index) => {
                    const ingredient = row.querySelector('[name="ingredientId"]');
                    const amount = row.querySelector('[name="quantity"]');
                    if (!ingredient.value && !amount.value.trim()) return;
                    count++;
                    const label = 'Dòng ' + (index + 1) + ': ';
                    if (!ingredient.value) check(ingredient, label + 'vui lòng chọn nguyên liệu.');
                    else if (seen.has(ingredient.value)) check(ingredient, label + 'nguyên liệu bị chọn trùng với dòng trước. Mỗi nguyên liệu chỉ chọn một lần.');
                    seen.add(ingredient.value);
                    check(amount, quantity(amount.value, 'Định lượng dòng ' + (index + 1)));
                });
                if (!count) check(document.getElementById('addIngredient'), 'Công thức phải có ít nhất một nguyên liệu.');
                if (rows.length > 100) check(document.getElementById('addIngredient'), 'Không được thêm quá 100 dòng nguyên liệu.');
            }
            if (kind === 'loyalty') {
                // [TDKT01]
                // . Khi bấm Lưu quy tắc, validate gọi wholeNumber ở đầu file cho từng ô tiền tích điểm,
                //   mốc đổi và tiền giảm. Số sai được báo ngay dưới ô; tiền giảm được phép bằng 0. Đúng cả ba
                //   thì form đến AdminLoyaltyController.doPost, nơi updateRules kiểm tra lại rồi lưu.
                [['pointsEarnUnit', 'Số tiền chi tiêu để nhận 1 điểm', 1, 100000000],
                    ['pointsStep', 'Số điểm cho mỗi lần đổi', 1, 1000000],
                    ['pointsStepValue', 'Số tiền giảm cho mỗi lần đổi', 0, 100000000]]
                    .forEach(([name, label, min, max]) => check(field(name), wholeNumber(field(name)?.value, label, min, max)));
            }
            form.querySelectorAll('input:not([type="hidden"]),select,textarea,button').forEach(input => mark(input, errors.get(input) || ''));
            return errors;
        };
        // . Khi người dùng gửi form, em gọi validate để kiểm tra các ô và hiện lỗi qua mark. Còn lỗi thì
        //   dừng gửi và đưa con trỏ đến ô đầu tiên; không có lỗi thì trình duyệt gửi tới địa chỉ của form.
        form.addEventListener('submit', event => {
            submitted = true;
            const errors = validate();
            if (errors.size) { event.preventDefault(); errors.keys().next().value.focus(); }
        });
        // . Khi sửa ô hoặc thêm/xóa dòng, em ẩn kết quả của lần gửi trước. Nếu đã từng kiểm tra thì kiểm
        //   tra lại các ô để lời báo hiện tại khớp với dữ liệu vừa sửa, không giữ nguyên lỗi cũ.
        const changed = () => {
            notices.forEach(notice => { notice.hidden = true; });
            if (submitted) validate();
        };
        form.addEventListener('input', changed);
        form.addEventListener('change', changed);
        form.addEventListener('click', event => {
            if (event.target.closest('button[type="button"]')) queueMicrotask(changed);
        });
    });
})();
