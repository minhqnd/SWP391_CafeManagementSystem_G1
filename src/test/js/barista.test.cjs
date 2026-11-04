const { test, beforeEach } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const Module = require("node:module");
const jsp = fs.readFileSync(require.resolve("../../main/webapp/barista-dashboard.jsp"), "utf8");
const source = jsp.match(/<script id="barista-script">([\s\S]*?)<\/script>/)[1];
const scriptModule = new Module(__filename);
scriptModule._compile(source, "barista-inline.js");
const kds = scriptModule.exports;
beforeEach(() => kds.partialCups.clear());

function sample() {
    return [
        {
            id: 2,
            status: "PREPARING",
            createdAt: "2026-10-19T09:01:00",
            items: [
                {
                    itemId: 21,
                    productId: 7,
                    productName: "Cà phê",
                    quantity: 2,
                    customization: '{"ice":"ít","size":"M"}',
                    isPrepared: false,
                },
                {
                    itemId: 22,
                    productId: 7,
                    productName: "Cà phê",
                    quantity: 1,
                    customization: '{"size":"L","ice":"ít"}',
                    isPrepared: false,
                },
            ],
        },
        {
            id: 1,
            status: "CONFIRMED",
            createdAt: "2026-10-19T09:00:00",
            items: [
                {
                    itemId: 11,
                    productId: 7,
                    productName: "Cà phê",
                    quantity: 3,
                    customization: '{"size":"M","ice":"ít"}',
                    isPrepared: false,
                },
            ],
        },
        {
            id: 3,
            status: "READY",
            createdAt: "2026-10-19T08:00:00",
            readyAt: "2026-10-19T09:02:00",
            items: [
                {
                    itemId: 31,
                    productId: 7,
                    productName: "Cà phê",
                    quantity: 10,
                    customization: '{"size":"M","ice":"ít"}',
                    isPrepared: true,
                },
            ],
        },
    ];
}
test("group by product across orders, keep every cup's options in oldest-first lines, exclude READY", () => {
    const groups = kds.groups(sample());
    assert.equal(groups.length, 1);
    assert.equal(groups[0].quantity, 6);
    assert.deepEqual(groups[0].itemIds, [11, 21, 22]);
    // Đơn 1 đặt lúc 09:00 đứng trước đơn 2 lúc 09:01; size L vẫn là một dòng riêng
    assert.deepEqual(
        groups[0].lines.map((l) => l.order.id),
        [1, 2, 2],
    );
    assert.equal(kds.options(groups[0].lines[2].item.customization).size, "L");
});
test("1 cup 100% sugar and 1 cup 50% sugar show as ×2 with both details", () => {
    const orders = [
        {
            id: 5,
            status: "CONFIRMED",
            createdAt: "2026-10-26T08:00:00",
            items: [
                {
                    itemId: 51,
                    productId: 3,
                    productName: "Trà chanh",
                    quantity: 1,
                    customization: '{"size":"M","notes":"100% đường, 100% đá"}',
                    isPrepared: false,
                },
                {
                    itemId: 52,
                    productId: 3,
                    productName: "Trà chanh",
                    quantity: 1,
                    customization: '{"size":"M","notes":"50% đường, 100% đá"}',
                    isPrepared: false,
                },
            ],
        },
    ];
    const groups = kds.groups(orders);
    assert.equal(groups.length, 1);
    assert.equal(groups[0].quantity, 2);
    assert.deepEqual(
        groups[0].lines.map((l) => kds.options(l.item.customization).notes),
        ["100% đường, 100% đá", "50% đường, 100% đá"],
    );
});
test("checking a quantity-three line updates progress and grouping; unchecking restores it", () => {
    const orders = sample();
    kds.markItems(orders, [11], true);
    assert.equal(orders[1].status, "PREPARING");
    assert.deepEqual(kds.progress(orders[1]), { done: 3, total: 3, remaining: 0 });
    assert.equal(kds.groups(orders)[0].quantity, 6);
    kds.markItems(orders, [11], false);
    assert.equal(kds.groups(orders)[0].quantity, 6);
});
test("completed group remains accessible for modal rendering but is marked complete", () => {
    const orders = sample();
    kds.markItems(orders, kds.groups(orders)[0].itemIds, true);
    assert.equal(kds.groups(orders).length, 1);
    assert.equal(kds.groupComplete(kds.groups(orders)[0]), true);
    kds.markItems(orders, [21], false);
    assert.equal(kds.groupComplete(kds.groups(orders)[0]), false);
    kds.markItems(orders, [21], true);
    assert.deepEqual(kds.progress(orders[0]), { done: 3, total: 3, remaining: 0 });
    assert.deepEqual(kds.progress(orders[1]), { done: 3, total: 3, remaining: 0 });
});
test("completion checks remaining lines and moves order to read-only READY selection", () => {
    const orders = sample();
    kds.markReady(orders, 2);
    assert.equal(
        kds.activeOrders(orders).some((o) => o.id === 2),
        false,
    );
    assert.equal(
        kds.completedOrders(orders, new Date("2026-10-19T03:00:00Z")).some((o) => o.id === 2),
        true,
    );
    assert.equal(
        orders[0].items.every((i) => i.isPrepared),
        true,
    );
    kds.markItems(orders, [21], false);
    assert.equal(orders[0].items[0].isPrepared, true);
});
test("oldest active first, latest completion first regardless of creation date", () => {
    const orders = sample();
    assert.deepEqual(
        kds.activeOrders(orders).map((o) => o.id),
        [1, 2],
    );
    kds.markReady(orders, 1);
    orders[1].readyAt = "2026-10-19T09:01:00";
    assert.deepEqual(
        kds.completedOrders(orders, new Date("2026-10-19T03:00:00Z")).map((o) => o.id),
        [3, 1],
    );
});
test("completed orders reset at Vietnam midnight and use completion date before creation date", () => {
    const orders = [
        { id: 1, status: "READY", createdAt: "2026-10-18T23:00:00", readyAt: "2026-10-19T23:59:59" },
        { id: 2, status: "READY", createdAt: "2026-10-19T23:00:00", readyAt: "2026-10-20T00:00:00" },
        { id: 3, status: "READY", createdAt: "2026-10-19T10:00:00" },
        { id: 4, status: "READY" },
        { id: 5, status: "PREPARING", createdAt: "2026-10-20T00:00:00" },
    ];
    assert.deepEqual(kds.completedOrders(orders, new Date("2026-10-19T16:59:59Z")).map(o => o.id), [1, 3]);
    assert.deepEqual(kds.completedOrders(orders, new Date("2026-10-19T17:00:00Z")).map(o => o.id), [2]);
    assert.deepEqual(kds.activeOrders(orders).map(o => o.id), [5]);
});

test("same product ID but different snapshot name or customization stays separate", () => {
    const a = { productId: 7, productName: "Cà phê", customization: '{"size":"M"}' };
    assert.notEqual(kds.groupKey(a), kds.groupKey({ ...a, productName: "Cà phê sữa" }));
    assert.notEqual(kds.groupKey(a), kds.groupKey({ ...a, customization: '{"size":"M","sugar":"ít"}' }));
    assert.equal(kds.groupKey({ ...a, customization: null }), kds.groupKey({ ...a, customization: "{}" }));
});

test("order checkboxes combine identical options and quantities only within the same order", () => {
    const orders = sample();
    const order = orders[0];
    order.items.push({ ...order.items[0], itemId: 23, quantity: 3,
        customization: '{"size":"M","ice":"ít"}' });
    const groups = kds.orderItemGroups(order);
    assert.equal(groups.length, 2);
    assert.equal(groups[0].quantity, 5);
    assert.deepEqual(groups[0].itemIds, [21, 23]);
    assert.deepEqual(groups[1].itemIds, [22]);
    kds.markItems(orders, groups[0].itemIds, true);
    assert.equal(kds.groupComplete(groups[0]), true);
    assert.deepEqual(kds.progress(order), { done: 5, total: 6, remaining: 1 });
    assert.equal(kds.progress(orders[1]).done, 0);
    kds.markItems(orders, groups[0].itemIds, false);
    assert.equal(kds.progress(order).done, 0);
});

test("different sugar, ice, toppings and notes retain separate order checkboxes", () => {
    const item = sample()[0].items[0];
    const base = { size: "M", sugar: "50%", ice: "50%", toppings: ["A"], note: "Ly riêng" };
    const variants = [base, { ...base, sugar: "100%" }, { ...base, ice: "100%" },
        { ...base, toppings: ["B"] }, { ...base, note: "Mang đi" }];
    assert.equal(kds.orderItemGroups({ items: variants.map((customization, index) =>
        ({ ...item, itemId: index + 1, customization: JSON.stringify(customization) })) }).length, 5);
});

test("partial cups have stable IDs, independent states and accurate order totals", () => {
    const orders = sample();
    const item = orders[1].items[0];
    assert.deepEqual(
        kds.cups(item).map((c) => c.key),
        ["11:0", "11:1", "11:2"],
    );
    kds.partialCups.set(11, [false, true, false]);
    assert.deepEqual(kds.progress(orders[1]), { done: 1, total: 3, remaining: 2 });
    assert.deepEqual(
        kds.cups(item).map((c) => c.prepared),
        [false, true, false],
    );
    assert.equal(kds.progress(orders[0]).done, 0);
    kds.partialCups.set(11, [false, false, false]);
    assert.equal(kds.progress(orders[1]).done, 0);
});
test("same product across orders stays visible until every independent cup is prepared", () => {
    const orders = sample();
    kds.markItems(orders, [11, 21], true);
    let group = kds.groups(orders)[0];
    assert.equal(kds.groupComplete(group), false);
    assert.equal(kds.progress(orders[1]).done, 3);
    assert.equal(kds.progress(orders[0]).done, 2);
    kds.markItems(orders, [22], true);
    group = kds.groups(orders)[0];
    assert.equal(kds.groupComplete(group), true);
    kds.markItems(orders, [11], false);
    kds.partialCups.set(11, [true, false, true]);
    assert.equal(kds.groupComplete(kds.groups(orders)[0]), false);
    assert.equal(kds.progress(orders[1]).done, 2);
    assert.equal(kds.progress(orders[0]).done, 3);
});
test("polling preserves partial cups, reconciles server completion and prunes removed rows", () => {
    kds.partialCups.set(11, [true, false, false]);
    const orders = sample();
    kds.reconcile(orders);
    assert.equal(kds.progress(orders[1]).done, 1);
    orders[1].items[0].isPrepared = true;
    kds.reconcile(orders);
    assert.equal(kds.partialCups.has(11), false);
    assert.equal(kds.progress(orders[1]).done, 3);
    kds.partialCups.set(21, [true, false]);
    kds.reconcile([]);
    assert.equal(kds.partialCups.size, 0);
});
test("product ID groups snapshot names and options together", () => {
    const orders = sample();
    orders[1].items[0].productName = "T?n c?";
    assert.equal(kds.groups(orders).length, 1);
    assert.equal(kds.groups(orders)[0].quantity, 6);
});

test("UI syncs modal, cards and groups; blocks early finish and rolls back failed API updates", async () => {
    const vm = require("node:vm");
    class Node {
        constructor(tag) {
            this.tag = tag;
            this.children = [];
            this.dataset = {};
            this.events = {};
            this.textContent = "";
        }
        append(...nodes) {
            this.children.push(...nodes);
        }
        replaceChildren(...nodes) {
            this.children = nodes;
        }
        setAttribute() {}
        addEventListener(event, callback) {
            this.events[event] = callback;
        }
        focus() {
            document.activeElement = this;
        }
        showModal() {
            this.open = true;
        }
        close() {
            this.open = false;
            this.events.close?.();
        }
    }
    const serverOrders = sample().slice(1, 2);
    serverOrders[0].items[0].customization = JSON.stringify({
        size: "M",
        notes: "50% đường, Ít đá, Lưu ý: Ly cá nhân",
        topping: "Sốt caramel thêm, Kem whipping",
    });
    serverOrders[0].items.push({ ...serverOrders[0].items[0], itemId: 12, quantity: 2 });
    const body = new Node("body");
    body.dataset = { baristaView: "active", contextPath: "", csrfToken: "token" };
    const nodes = Object.fromEntries(
        ["order-list", "group-list", "barista-error", "barista-initial-data"].map((id) => [id, new Node("div")]),
    );
    nodes["barista-initial-data"].textContent = JSON.stringify({ orders: serverOrders });
    body.append(...Object.values(nodes));
    const walk = (node) => [node, ...node.children.flatMap(walk)];
    const document = {
        body,
        createElement: (tag) => new Node(tag),
        getElementById: (id) => nodes[id],
        querySelectorAll: () => walk(body).filter((n) => n.dataset.focusKey),
    };
    let poll,
        fail = false;
    const posts = [];
    const window = {
        document,
        setInterval: (callback) => {
            poll = callback;
        },
        clearInterval() {},
        addEventListener() {},
    };
    const fetch = async (_, config) => {
        if (config.method === "POST") {
            const params = Object.fromEntries(config.body);
            posts.push(params);
            if (fail) return { ok: false, json: async () => ({ message: "Save failed" }) };
            if (params.action === "update-items") {
                const ids = params.itemIds.split(",").map(Number);
                serverOrders[0].items.filter((item) => ids.includes(item.itemId))
                    .forEach((item) => { item.isPrepared = params.isPrepared === "true"; });
            }
            if (params.action === "update-order-status") serverOrders[0].status = "READY";
            return { ok: true, json: async () => ({ success: true }) };
        }
        return { ok: true, json: async () => JSON.parse(JSON.stringify({ orders: serverOrders })) };
    };
    vm.runInNewContext(source, { window, fetch, URLSearchParams });
    const settle = () => new Promise((resolve) => setImmediate(resolve));
    const find = (key) => walk(body).find((n) => n.dataset.focusKey === key);
    const change = (key, checked) => {
        const input = find(key);
        input.checked = checked;
        input.events.change();
    };
    await settle();
    assert.equal(walk(nodes["order-list"]).filter((n) => n.tag === "input").length, 1);
    assert.equal(walk(nodes["order-list"]).find((n) => n.className === "item-quantity").textContent, "×5");
    assert.equal(find("complete-1").disabled, true);
    find("complete-1").events.click();
    assert.equal(posts.length, 0);
    find("open-7").events.click();
    const dialog = walk(body).find((n) => n.tag === "dialog");
    assert.equal(dialog.open, true);
    assert.equal(walk(dialog).filter((n) => n.tag === "input").length, 5);
    assert.equal(walk(dialog).filter((n) => n.tag === "progress").length, 0);
    const firstCard = walk(dialog).find((n) => n.className === "group-line");
    const details = firstCard.children.find((n) => n.className === "cup-details");
    assert.equal(details.children[0].className, "option-tags");
    assert.equal(details.children[1].className, "cup-special-note");
    assert.deepEqual(
        details.children[1].children.map((n) => n.textContent),
        ["Lưu ý của khách:", "Ly cá nhân"],
    );
    assert.equal(
        walk(firstCard.children[0]).some((n) => n.className === "cup-status"),
        false,
    );
    assert.equal(firstCard.children[0].children[0].textContent, "#1");
    change("modal-11:0", true);
    assert.equal(find("item-11").checked, false);
    assert.equal(find("group-7").checked, false);
    assert.equal(posts.length, 0);
    await poll();
    assert.equal(find("modal-11:0").checked, true);
    dialog.close();
    find("open-7").events.click();
    assert.equal(find("modal-11:0").checked, true);
    fail = true;
    change("item-11", true);
    await settle();
    assert.equal(posts.at(-1).itemIds, "11,12");
    assert.equal(find("item-11").checked, false);
    assert.equal(find("modal-11:2").checked, false);
    assert.equal(find("modal-11:0").checked, true);
    assert.equal(find("complete-1").disabled, true);
    fail = false;
    change("item-11", true);
    await settle();
    assert.equal(find("item-11").checked, true);
    assert.equal(serverOrders[0].items.every((item) => item.isPrepared), true);
    assert.equal(walk(dialog).filter((n) => n.tag === "input").every((n) => n.checked), true);
    assert.equal(walk(nodes["order-list"]).some((n) => n.textContent === "5/5 ly đã pha"), true);
    assert.equal(find("complete-1").disabled, false);
    await poll();
    assert.equal(find("item-11").checked, true);
    assert.equal(find("group-7"), undefined);
    assert.equal(dialog.open, true);
    assert.equal(serverOrders[0].status, "CONFIRMED");
    change("modal-11:2", false);
    await settle();
    assert.equal(find("group-7").checked, false);
    change("group-7", true);
    await settle();
    assert.equal(find("group-7"), undefined);
    assert.equal(dialog.open, true);
    change("item-11", false);
    await settle();
    assert.equal(find("group-7").checked, false);
    assert.equal(find("modal-11:1").checked, false);
    assert.equal(find("modal-11:0").checked, false);
    change("group-7", false);
    await settle();
    assert.equal(find("item-11").checked, false);
    dialog.close();
    change("group-7", true);
    await settle();
    assert.equal(dialog.open, false);
    assert.equal(find("group-7"), undefined);
    find("complete-1").events.click();
    await settle();
    assert.equal(serverOrders[0].status, "READY");
    assert.equal(find("complete-1"), undefined);
});
