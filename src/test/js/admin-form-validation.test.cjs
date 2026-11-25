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
    for (const value of ['1e3', 'NaN', 'Infinity', '+1', '1,5'])
        assert.match(validation.money(value, 'Giá'), /phải là số/);
    assert.equal(validation.money('9'.repeat(10000), 'Giá'), 'Giá tối đa 64 ký tự.');
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

test('names, money and quantity report the exact reason on adjacent boundaries', () => {
    for (const max of [30, 50, 100, 150]) {
        for (const size of [max - 1, max, max + 1])
            assert.equal(validation.requiredName('x'.repeat(size), 'Tên', max), size > max ? `Tên tối đa ${max} ký tự.` : '');
    }
    for (const value of ['', ' ', '\u00a0', '\u2003', '\ufeff']) {
        assert.equal(validation.requiredName(value, 'Tên', 100), 'Vui lòng nhập tên.');
        assert.equal(validation.money(value, 'Giá'), 'Vui lòng nhập giá.');
    }
    assert.equal(validation.money('\u00a01.50\u00a0', 'Giá'), '');
    assert.equal(validation.money('0'.repeat(63) + '1', 'Giá'), '');
    assert.equal(validation.money('0'.repeat(64) + '1', 'Giá'), 'Giá tối đa 64 ký tự.');
    for (const value of ['0', '0.01', '9999999999.98', '9999999999.99']) assert.equal(validation.money(value, 'Giá'), '');
    assert.equal(validation.money('10000000000', 'Giá'), 'Giá không được vượt quá 9.999.999.999,99.');
    assert.equal(validation.money('-0.01', 'Giá'), 'Giá không được âm.');
    assert.equal(validation.money('1.001', 'Giá'), 'Giá có tối đa 2 chữ số thập phân.');
    for (const value of ['0.001', '999999999.998', '999999999.999']) assert.equal(validation.quantity(value, 'Định lượng'), '');
    assert.equal(validation.quantity('0.0001', 'Định lượng'), 'Định lượng có tối đa 3 chữ số thập phân.');
    assert.equal(validation.quantity('1000000000', 'Định lượng'), 'Định lượng không được vượt quá 999.999.999,999.');
});

test('each loyalty rule checks min minus one, min, max and max plus one with exact messages', () => {
    for (const [min, max] of [[1, 100000000], [1, 1000000], [0, 100000000]]) {
        for (const value of [min - 1, min, min + 1, max - 1, max, max + 1]) {
            const expected = value < 0 ? `Quy tắc phải là số nguyên từ ${min} đến ${max}.`
                : value < min || value > max ? `Quy tắc phải từ ${min} đến ${max}.` : '';
            assert.equal(validation.wholeNumber(String(value), 'Quy tắc', min, max), expected);
        }
    }
});
