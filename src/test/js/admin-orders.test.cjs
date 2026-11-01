const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

test('filters combine, preserve criteria, and validate dates before navigation', () => {
    const jsp = fs.readFileSync('src/main/webapp/admin-orders.jsp', 'utf8');
    const source = jsp.match(/<script>\s*\(\(\) => \{([\s\S]*?)\}\)\(\);\s*<\/script>/)[0]
        .replace(/<\/?script>/g, '').replace(/<c:if[^>]*>.*?<\/c:if>/g, '');
    class Node {
        constructor(tagName = 'DIV') { this.tagName = tagName; this.value = ''; this.dataset = {}; this.events = {}; this.hidden = false; }
        addEventListener(name, fn) { this.events[name] = fn; }
        setAttribute() {}
        contains(node) { return node === this; }
        checkValidity() { return true; }
        focus() {}
    }
    const ids = Object.fromEntries(['orderSearch', 'orderTypeFilter', 'orderStatusFilter', 'dateFilter', 'dateTrigger', 'datePopover', 'dateFrom', 'dateTo', 'dateError', 'dateLabel', 'dateApply', 'dateClear', 'noMatchingOrders'].map(id => [id, new Node()]));
    ids.orderSearch.tagName = 'INPUT';
    ids.datePopover.hidden = true;
    const rows = [new Node(), new Node()];
    rows[0].dataset = { search: 'ord-1 an 123', type: 'ONLINE', status: 'READY' };
    rows[1].dataset = { search: 'ord-2 binh 456', type: 'DINE_IN', status: 'READY' };
    const link = new Node(); link.dataset.orderId = '1';
    const events = {};
    const document = { getElementById: id => ids[id], querySelectorAll: selector => selector === '.order-row' ? rows : [link],
        querySelector: () => null, addEventListener: (name, fn) => { events[name] = fn; } };
    let navigated = '', replaced = '';
    const location = { search: '?q=an&type=ONLINE&status=READY', pathname: '/admin/orders', assign: value => { navigated = value; } };
    const history = { replaceState: (_, __, value) => { replaced = value; } };
    vm.runInNewContext(source, { document, location, history, URLSearchParams });
    assert.equal(rows[0].hidden, false);
    assert.equal(rows[1].hidden, true);
    assert.match(link.href, /q=an.*type=ONLINE.*status=READY.*detailId=1/);
    ids.dateTrigger.events.click();
    assert.equal(ids.datePopover.hidden, false);
    ids.dateFrom.value = '2026-10-30'; ids.dateTo.value = '2026-10-01';
    ids.dateApply.events.click();
    assert.equal(navigated, '');
    assert.equal(ids.dateError.hidden, false);
    ids.dateTo.value = '2026-10-31';
    ids.dateApply.events.click();
    assert.match(navigated, /q=an.*type=ONLINE.*status=READY.*from=2026-10-30.*to=2026-10-31/);
    events.keydown({ key: 'Escape' });
    assert.equal(ids.datePopover.hidden, true);
    ids.orderSearch.value = 'không có'; ids.orderSearch.events.input();
    assert.equal(ids.noMatchingOrders.hidden, false);
    assert.match(replaced, /type=ONLINE/);
});
