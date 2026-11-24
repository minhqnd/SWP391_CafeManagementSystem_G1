const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const scope = {};
vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/webapp/assets/js/admin-form-validation.js'), 'utf8'), scope);
const validation = scope.AuraAdminValidation;

test('money accepts 0 and exact maximum; reasons for rejection stay separate', () => {
    assert.equal(validation.money('0', 'Giá'), '');
    assert.equal(validation.money('9999999999.99', 'Giá'), '');
    assert.equal(validation.money('', 'Giá'), 'Vui lòng nhập giá.');
    assert.equal(validation.money('-1', 'Giá'), 'Giá không được âm.');
    assert.match(validation.money('10000000000', 'Giá'), /9\.999\.999\.999,99/);
    assert.match(validation.money('1.001', 'Giá'), /2 chữ số thập phân/);
    for (const value of ['1e3', 'NaN', 'Infinity', '+1', '1,5', '9'.repeat(10000)])
        assert.match(validation.money(value, 'Giá'), /phải là số/);
});

test('quantity and money compare exact decimals at the upper boundary', () => {
    assert.equal(validation.quantity('0.001', 'Định lượng'), '');
    assert.equal(validation.quantity('999999999.999', 'Định lượng'), '');
    assert.match(validation.quantity('999999999.9991', 'Định lượng'), /vượt quá/);
    assert.match(validation.money('9999999999.9901', 'Giá'), /vượt quá/);
    assert.match(validation.quantity('1.0001', 'Định lượng'), /3 chữ số thập phân/);
    assert.match(validation.quantity('0', 'Định lượng'), /lớn hơn 0/);
});

test('names and integer rules reject empty, control characters and invalid limits', () => {
    assert.equal(validation.requiredName(' Cà phê ', 'Tên món', 150), '');
    assert.match(validation.requiredName('A\u0085B', 'Tên món', 150), /ký tự/);
    assert.match(validation.requiredName('x'.repeat(151), 'Tên món', 150), /150/);
    assert.equal(validation.wholeNumber('0', 'Tiền giảm', 0, 100000000), '');
    for (const value of ['', '-1', '1.5', '1e3', '100000001', '9223372036854775808'])
        assert.notEqual(validation.wholeNumber(value, 'Tiền giảm', 0, 100000000), '');
});
