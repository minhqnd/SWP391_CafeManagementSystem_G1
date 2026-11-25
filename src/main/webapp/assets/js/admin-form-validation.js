(() => {
    'use strict';
    const controlChars = /[\u0000-\u001f\u007f-\u009f]/;

    // Kiểm tra tên theo giới hạn của ô đang nhập.
    const requiredName = (raw, label, max) => {
        const value = String(raw ?? '').trim();
        if (!value) return 'Vui lòng nhập ' + label.toLocaleLowerCase('vi') + '.';
        if (value.length > max) return label + ' tối đa ' + max + ' ký tự.';
        return controlChars.test(value) ? label + ' chứa ký tự không được hỗ trợ.' : '';
    };

    // Kiểm tra số tiền hoặc định lượng theo giới hạn và số chữ số lẻ của từng loại.
    const decimal = (raw, label, scale, maximum, displayMax, positive = false) => {
        const value = String(raw ?? '').trim();
        if (!value) return 'Vui lòng nhập ' + label.toLocaleLowerCase('vi') + '.';
        if (value.length > 64) return label + ' tối đa 64 ký tự.';
        if (!/^-?[0-9]+(?:\.[0-9]+)?$/.test(value))
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

    // Kiểm tra số nguyên theo khoảng được phép của quy tắc.
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

    document.querySelectorAll('form[data-admin-form]').forEach(form => {
        const kind = form.dataset.adminForm;
        const notices = [...document.querySelectorAll(kind === 'loyalty-lookup' ? '[data-lookup-notice]' : '[data-form-notice]')];
        let submitted = notices.some(notice => notice.classList.contains('alert-danger')) || Boolean(form.querySelector('.is-invalid'));
        let serial = 0;
        const field = name => form.elements.namedItem(name);

        // Đặt hoặc xóa lời báo ngay dưới đúng ô đang nhập.
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
                // [DML02]
                // . Trước khi gửi form danh mục, em kiểm tra tên, trạng thái và ảnh để báo đúng phần cần
                //   sửa. Khi thông tin hợp lệ, form gửi tới /admin/categories để doPost trong
                //   AdminCategoriesController tiếp nhận và xử lý việc lưu.
                //
                // Tra tiếp: AdminCategoriesController.java / DML03.
                check(field('name'), requiredName(field('name')?.value, kind === 'category' ? 'Tên danh mục' : 'Tên món', kind === 'category' ? 100 : 150));
                if (kind === 'category' && !['ACTIVE', 'INACTIVE'].includes(field('status')?.value))
                    check(field('status'), 'Vui lòng chọn trạng thái Hoạt động hoặc Tạm ẩn.');
                const image = field('imageFile')?.files?.[0];
                if (image?.size === 0) check(field('imageFile'), 'Tệp ảnh đã chọn trống. Vui lòng chọn ảnh khác.');
                else if (image?.size > 5 * 1024 * 1024) check(field('imageFile'), 'Ảnh ' + (kind === 'category' ? 'danh mục' : 'sản phẩm') + ' không được lớn hơn 5 MB.');
                else if (image && !['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(image.type))
                    check(field('imageFile'), 'Chỉ chấp nhận ảnh JPG, PNG, GIF hoặc WebP.');
            }
            if (kind === 'product') {
                // [SPL02]
                // . Trước khi gửi form sản phẩm, em kiểm tra các ô, ảnh và từng dòng size, topping để báo
                //   đúng phần nhập sai. Size phải có ít nhất một dòng và một lựa chọn không phụ thu;
                //   topping có thể để trống. Khi các phần đều đúng, form gửi tới /admin/products để
                //   doPost trong AdminProductsController xử lý tiếp.
                //
                // Tra tiếp: AdminProductsController.java / SPL03.
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
                        check(name, requiredName(name.value, 'Tên ' + type + ' dòng ' + (index + 1), maxName));
                        if (/[\\"'{}<>`]/.test(name.value)) check(name, 'Tên ' + type + ' dòng ' + (index + 1) + ' chứa ký tự không được hỗ trợ.');
                        const normalized = name.value.trim().toLowerCase();
                        if (normalized && names.has(normalized)) check(name, 'Không được trùng tên ' + type + ' (dòng ' + (index + 1) + ').');
                        names.add(normalized);
                        const amountError = !amount.value.trim()
                            ? 'Vui lòng nhập ' + amountLabel.toLocaleLowerCase('vi') + ' dòng ' + (index + 1)
                                + (type === 'size' ? '; nhập 0 nếu không phụ thu.' : '; nhập 0 nếu miễn phí.')
                            : money(amount.value, amountLabel + ' dòng ' + (index + 1));
                        check(amount, amountError);
                        if (!amountError && Number(amount.value) === 0) base = true;
                    });
                    if (rows.length > 50) check(rows[50]?.querySelector('input'), 'Không được thêm quá 50 ' + type + '.');
                    if (type === 'size' && !count) check(document.getElementById('addSize'), 'Sản phẩm cần có ít nhất một size.');
                    else if (type === 'size' && !base && !errors.size) check(rows[0]?.querySelector('[name="sizeDelta"]'), 'Cần có một size phụ thu 0đ để giá mặc định đúng bằng giá bán cơ bản.');
                }
            }
            if (kind === 'recipe') {
                // [CTL02]
                // . Trước khi gửi công thức, em kiểm tra đã có nguyên liệu, không chọn trùng và định
                //   lượng phải dương. Nếu có lỗi, em báo đúng dòng để Admin sửa. Khi các dòng đều hợp lệ,
                //   form gửi tới /admin/recipes để doPost trong AdminRecipeController xử lý việc lưu.
                //
                // Tra tiếp: AdminRecipeController.java / CTL03.
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
                    check(amount, !amount.value.trim() ? label + 'vui lòng nhập định lượng.' : quantity(amount.value, 'Định lượng dòng ' + (index + 1)));
                });
                if (!count) check(document.getElementById('addIngredient'), 'Công thức phải có ít nhất một nguyên liệu.');
                if (rows.length > 100) check(document.getElementById('addIngredient'), 'Không được thêm quá 100 dòng nguyên liệu.');
            }
            if (kind === 'loyalty') {
                // [TDQT02]
                // . Trước khi gửi form Lưu quy tắc, em kiểm tra ba ô là số nguyên và nằm trong giới hạn
                //   cho phép. Ô nào nhập sai thì được báo để Admin sửa ngay trên trang. Khi cả ba ô đều
                //   đúng, form gửi tới /admin/loyalty để doPost trong AdminLoyaltyController nhận và xử
                //   lý bộ quy tắc.
                //
                // Tra tiếp: AdminLoyaltyController.java / TDQT03.

                [['pointsEarnUnit', 'Số tiền chi tiêu để nhận 1 điểm', 1, 100000000],
                    ['pointsStep', 'Số điểm cho mỗi lần đổi', 1, 1000000],
                    ['pointsStepValue', 'Số tiền giảm cho mỗi lần đổi', 0, 100000000]]
                    .forEach(([name, label, min, max]) => check(field(name), wholeNumber(field(name)?.value, label, min, max)));
            }
            // [TDTK02]
            // . Trước khi gửi yêu cầu tra cứu, em kiểm tra ô điện thoại đã được nhập và đúng định dạng.
            //   Nếu sai, em báo tại ô để Admin sửa. Nếu đúng, form gửi tới /admin/loyalty để doGet trong
            //   AdminLoyaltyController nhận số điện thoại và tìm khách.
            //
            // Tra tiếp: AdminLoyaltyController.java / TDTK03.
            if (kind === 'loyalty-lookup') {
                const raw = field('phone')?.value || '';
                const phone = raw.trim().replace(/[\s.()-]/g, '');
                check(field('phone'), raw.length > 64 ? 'Số điện thoại tối đa 64 ký tự.'
                    : !phone ? 'Vui lòng nhập số điện thoại.' : !/^0[0-9]{9}$/.test(phone) ? 'Số điện thoại sai định dạng.' : '');
            }
            form.querySelectorAll('input:not([type="hidden"]),select,textarea,button').forEach(input => mark(input, errors.get(input) || ''));
            return errors;
        };
        // [QLGUI01]
        // . Khi Admin bấm gửi form, em chạy phần kiểm tra tương ứng với danh mục, sản phẩm, công thức,
        //   quy tắc hoặc tra cứu. Có lỗi thì form được giữ lại để sửa; không có lỗi mới gửi thông tin tới
        //   controller của đúng chức năng.
        //
        // Tra cứu: Danh mục kiểm tra tại DML02, sản phẩm SPL02, công thức CTL02, quy tắc TDQT02, tra cứu
        //          TDTK02. Mỗi phần xong trở về sau validate ở QLGUI01; hợp lệ gửi tới phần nhận ghi ở
        //          comment của form.
        form.addEventListener('submit', event => {
            submitted = true;
            const errors = validate();
            if (errors.size) { event.preventDefault(); errors.keys().next().value.focus(); }
        });

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
