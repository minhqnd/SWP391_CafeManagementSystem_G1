const { test } = require("node:test");
const assert = require("node:assert/strict");
const kds = require("../../main/webapp/assets/js/barista.js");

function sample() {
    return [
        { id: 2, status: "PREPARING", createdAt: "2026-10-19T09:01:00", items: [
            { itemId: 21, productId: 7, productName: "Cà phê", quantity: 2, customization: '{"ice":"ít","size":"M"}', isPrepared: false },
            { itemId: 22, productId: 7, productName: "Cà phê", quantity: 1, customization: '{"size":"L","ice":"ít"}', isPrepared: false }
        ] },
        { id: 1, status: "CONFIRMED", createdAt: "2026-10-19T09:00:00", items: [
            { itemId: 11, productId: 7, productName: "Cà phê", quantity: 3, customization: '{"size":"M","ice":"ít"}', isPrepared: false }
        ] },
        { id: 3, status: "READY", createdAt: "2026-10-19T08:00:00", readyAt: "2026-10-19T09:02:00", items: [
            { itemId: 31, productId: 7, productName: "Cà phê", quantity: 10, customization: '{"size":"M","ice":"ít"}', isPrepared: true }
        ] }
    ];
}
test("group identical options across orders, preserving distinct sizes and excluding READY", () => {
    const groups = kds.groups(sample());
    assert.equal(groups.length, 2);
    assert.equal(groups[0].quantity, 5);
    assert.deepEqual(groups[0].itemIds, [11, 21]);
    assert.equal(groups[1].quantity, 1);
});
test("checking a quantity-three line updates progress and grouping; unchecking restores it", () => {
    const orders = sample();
    kds.markItems(orders, [11], true);
    assert.equal(orders[1].status, "PREPARING");
    assert.deepEqual(kds.progress(orders[1]), { done: 1, total: 1, remaining: 0 });
    assert.equal(kds.groups(orders)[0].quantity, 2);
    kds.markItems(orders, [11], false);
    assert.equal(kds.groups(orders)[0].quantity, 5);
});
test("group checking updates every matching line and removes only that group", () => {
    const orders = sample();
    kds.markItems(orders, kds.groups(orders)[0].itemIds, true);
    assert.equal(kds.groups(orders).length, 1);
    assert.deepEqual(kds.progress(orders[0]), { done: 1, total: 2, remaining: 1 });
    assert.deepEqual(kds.progress(orders[1]), { done: 1, total: 1, remaining: 0 });
});
test("completion checks remaining lines and moves order to read-only READY selection", () => {
    const orders = sample();
    kds.markReady(orders, 2);
    assert.equal(kds.activeOrders(orders).some(o => o.id === 2), false);
    assert.equal(kds.completedOrders(orders).some(o => o.id === 2), true);
    assert.equal(orders[0].items.every(i => i.isPrepared), true);
    kds.markItems(orders, [21], false);
    assert.equal(orders[0].items[0].isPrepared, true);
});
test("oldest active first, latest completion first regardless of creation date", () => {
    const orders = sample();
    assert.deepEqual(kds.activeOrders(orders).map(o => o.id), [1, 2]);
    kds.markReady(orders, 1);
    orders[1].readyAt = "2026-10-19T09:01:00";
    assert.deepEqual(kds.completedOrders(orders).map(o => o.id), [3, 1]);
});
test("same product ID but different snapshot name or customization stays separate", () => {
    const a = { productId: 7, productName: "Cà phê", customization: '{"size":"M"}' };
    assert.notEqual(kds.groupKey(a), kds.groupKey({ ...a, productName: "Cà phê sữa" }));
    assert.notEqual(kds.groupKey(a), kds.groupKey({ ...a, customization: '{"size":"M","sugar":"ít"}' }));
    assert.equal(kds.groupKey({ ...a, customization: null }), kds.groupKey({ ...a, customization: "{}" }));
});
