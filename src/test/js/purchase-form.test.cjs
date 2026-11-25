const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const jsp = fs.readFileSync('src/main/webapp/admin-purchases.jsp', 'utf8');
const lineJsp = fs.readFileSync('src/main/webapp/components/purchase-order-line.jsp', 'utf8');
const source = jsp.match(/<script id="purchase-form-script">([\s\S]*?)<\/script>/)[1];

function setup(selected = '') {
    const events = {};
    const request = { value: selected, addEventListener: (type, fn) => { events.select = fn; } };
    const lines = {
        rows: [{ id: 'blank' }],
        replaceChildren(fragment) { this.rows = fragment; }
    };
    const template = ids => ({ content: { cloneNode: () => ids.map(id => ({ id })) } });
    const nodes = {
        purchaseRequest: request,
        purchaseLines: lines,
        purchaseLineTemplate: template(['blank']),
        'purchaseRequest-12': template([7, 8])
    };
    vm.runInNewContext(source, { document: { getElementById: id => nodes[id] } });
    return { request, lines, events };
}

test('create purchase modal stays single-ingredient without add/remove line buttons', () => {
    assert.equal(jsp.includes('addPurchaseLine'), false);
    assert.equal(lineJsp.includes('data-remove-line'), false);
});

test('linked request loads request ingredients and preserves hidden ingredientId', () => {
    const s = setup();
    s.request.value = '12';
    s.events.select();
    assert.deepEqual(s.lines.rows.map(r => r.id), [7, 8]);
    assert.match(jsp, /name="ingredientId" value="\$\{it\.ingredientId\}"/);
});

test('clearing request resets to single blank line and restored selections are reconciled', () => {
    const s = setup('12');
    assert.deepEqual(s.lines.rows.map(r => r.id), [7, 8]);
    s.request.value = '';
    s.events.select();
    assert.deepEqual(s.lines.rows.map(r => r.id), ['blank']);
});
