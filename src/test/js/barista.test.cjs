const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const Module = require("node:module");
const jsp = fs.readFileSync(require.resolve("../../main/webapp/barista-dashboard.jsp"), "utf8");
const source = jsp.match(/<script id="barista-script">([\s\S]*?)<\/script>/)[1];
const scriptModule = new Module(__filename);
scriptModule._compile(source, "barista-inline.js");
const kds = scriptModule.exports;

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
test("group by product across orders, keep every cup's options in oldest-first lines, exclude READY", () => {
    const groups = kds.groups(sample());
    assert.equal(groups.length, 1);
    assert.equal(groups[0].quantity, 6);
    assert.deepEqual(groups[0].itemIds, [11, 21, 22]);
    // Đơn 1 đặt lúc 09:00 đứng trước đơn 2 lúc 09:01; size L vẫn là một dòng riêng
    assert.deepEqual(groups[0].lines.map(l => l.order.id), [1, 2, 2]);
    assert.equal(kds.options(groups[0].lines[2].item.customization).size, "L");
});
test("1 cup 100% sugar and 1 cup 50% sugar show as ×2 with both details", () => {
    const orders = [{ id: 5, status: "CONFIRMED", createdAt: "2026-10-26T08:00:00", items: [
        { itemId: 51, productId: 3, productName: "Trà chanh", quantity: 1, customization: '{"size":"M","notes":"100% đường, 100% đá"}', isPrepared: false },
        { itemId: 52, productId: 3, productName: "Trà chanh", quantity: 1, customization: '{"size":"M","notes":"50% đường, 100% đá"}', isPrepared: false }
    ] }];
    const groups = kds.groups(orders);
    assert.equal(groups.length, 1);
    assert.equal(groups[0].quantity, 2);
    assert.deepEqual(groups[0].lines.map(l => kds.options(l.item.customization).notes),
        ["100% đường, 100% đá", "50% đường, 100% đá"]);
});
test("checking a quantity-three line updates progress and grouping; unchecking restores it", () => {
    const orders = sample();
    kds.markItems(orders, [11], true);
    assert.equal(orders[1].status, "PREPARING");
    assert.deepEqual(kds.progress(orders[1]), { done: 1, total: 1, remaining: 0 });
    assert.equal(kds.groups(orders)[0].quantity, 3);
    kds.markItems(orders, [11], false);
    assert.equal(kds.groups(orders)[0].quantity, 6);
});
test("group checking updates every line of that product and removes the group", () => {
    const orders = sample();
    kds.markItems(orders, kds.groups(orders)[0].itemIds, true);
    assert.equal(kds.groups(orders).length, 0);
    assert.deepEqual(kds.progress(orders[0]), { done: 2, total: 2, remaining: 0 });
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
