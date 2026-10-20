// Browser boundary tests without dependencies or access to the real database.
const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");
const jsp = fs.readFileSync(require.resolve("../../main/webapp/barista-dashboard.jsp"), "utf8");
const source = jsp.match(/<script id="barista-script">([\s\S]*?)<\/script>/)[1];

class Element {
    constructor(tag = "div") {
        this.tagName = tag; this.children = []; this.dataset = {}; this.listeners = {};
        this.attributes = {}; this._text = ""; this.scrollTop = 0; this.disabled = false;
    }
    set textContent(value) { this._text = String(value); this.children = []; }
    get textContent() { return this._text + this.children.map(c => c.textContent).join(""); }
    append(...nodes) { this.children.push(...nodes); }
    replaceChildren(...nodes) { this._text = ""; this.children = nodes; }
    setAttribute(key, value) { this.attributes[key] = value; }
    addEventListener(key, value) { this.listeners[key] = value; }
    focus() {}
}
function walk(node) { return [node, ...node.children.flatMap(walk)]; }
function fixture() {
    return [
        { id: 1, orderCode: "ORDER-1", status: "CONFIRMED", orderType: "DINE_IN", createdAt: "2026-10-19T09:00:00", items: [
            { itemId: 11, productId: 7, productName: "<img onerror=alert(1)>", quantity: 3, customization: '{"size":"M"}', isPrepared: false }
        ] },
        { id: 2, orderCode: "ORDER-2", status: "PREPARING", orderType: "ONLINE", createdAt: "2026-10-19T09:01:00", items: [
            { itemId: 21, productId: 7, productName: "<img onerror=alert(1)>", quantity: 2, customization: '{"size":"M"}', isPrepared: false }
        ] }
    ];
}
function harness(orders = fixture(), view = "active") {
    const nodes = Object.fromEntries(["order-list", "group-list", "barista-error", "barista-initial-data"].map(id => [id, new Element()]));
    nodes["barista-initial-data"].textContent = JSON.stringify({ orders });
    if (view === "completed") delete nodes["group-list"];
    const body = new Element("body");
    body.dataset = { baristaView: view, contextPath: "/cafe", csrfToken: "session-token" };
    const document = {
        body, activeElement: null,
        createElement: tag => new Element(tag),
        getElementById: id => nodes[id] || null,
        querySelectorAll: () => Object.values(nodes).flatMap(walk).filter(n => n.dataset.focusKey)
    };
    const requests = [];
    const window = { document, confirm: () => true, addEventListener() {}, clearInterval() {},
        setInterval(callback, delay) { assert.equal(delay, 5000); window.poll = callback; return 1; } };
    const fetch = (url, options) => new Promise(resolve => requests.push({ url, options, resolve }));
    vm.runInNewContext(source, { window, fetch, URLSearchParams });
    const controls = () => Object.values(nodes).flatMap(walk);
    const byKey = key => controls().find(n => n.dataset.focusKey === key);
    const response = (index, data, status = 200) => requests[index].resolve({ ok: status < 400, json: async () => data });
    return { nodes, requests, window, controls, byKey, response };
}
const flush = () => new Promise(resolve => setImmediate(resolve));

test("item checkbox immediately updates grouping, sends CSRF, and ignores an older polling response", async () => {
    const h = harness();
    const check = h.byKey("item-11");
    check.checked = true; check.listeners.change();
    assert.ok(h.nodes["order-list"].textContent.includes("1/1 món đã pha"));
    assert.ok(h.nodes["group-list"].textContent.includes("×2"));
    const post = h.requests[1];
    assert.equal(post.url, "/cafe/barista");
    assert.equal(post.options.body.get("csrfToken"), "session-token");
    assert.equal(post.options.body.get("itemIds"), "11");
    assert.equal(post.options.body.get("isPrepared"), "true");
    h.response(0, { orders: fixture() });
    await flush();
    assert.equal(h.byKey("item-11").checked, true);
    h.response(1, { success: true });
    await flush();
    assert.equal(h.byKey("item-11").checked, true);
});

test("group action checks matching order cards; failed save restores the whole group", async () => {
    const h = harness();
    h.response(0, { orders: fixture() }); await flush();
    const group = h.controls().find(n => (n.dataset.focusKey || "").startsWith("group-"));
    group.checked = true; group.listeners.change();
    assert.equal(h.byKey("item-11").checked, true);
    assert.equal(h.byKey("item-21").checked, true);
    assert.ok(h.nodes["group-list"].textContent.includes("Không còn món chưa tích."));
    assert.equal(h.requests[1].options.body.get("itemIds"), "11,21");
    h.response(1, { message: "Đơn đã thay đổi" }, 409); await flush();
    assert.equal(h.byKey("item-11").checked, false);
    assert.equal(h.byKey("item-21").checked, false);
    assert.ok(h.nodes["group-list"].textContent.includes("×5"));
    assert.equal(h.nodes["barista-error"].textContent, "Đơn đã thay đổi");
});

test("incomplete completion confirms, sends READY, and removes the card only after success", async () => {
    const h = harness();
    h.response(0, { orders: fixture() }); await flush();
    h.window.confirm = () => false;
    h.byKey("complete-1").listeners.click();
    assert.equal(h.requests.length, 1);
    h.window.confirm = () => true;
    h.byKey("complete-1").listeners.click();
    assert.ok(h.nodes["order-list"].textContent.includes("ORDER-1"));
    assert.equal(h.requests[1].options.body.get("status"), "READY");
    h.response(1, { success: true }); await flush();
    assert.equal(h.nodes["order-list"].textContent.includes("ORDER-1"), false);
    assert.equal(h.nodes["order-list"].textContent.includes("ORDER-2"), true);
    const completed = fixture()[0];
    completed.status = "READY"; completed.readyAt = "2026-10-19T09:02:00";
    completed.items[0].isPrepared = true;
    const readOnly = harness([completed], "completed");
    assert.ok(readOnly.nodes["order-list"].textContent.includes("ORDER-1"));
    assert.ok(readOnly.nodes["order-list"].textContent.includes("Đã pha"));
    assert.equal(readOnly.controls().some(n => n.tagName === "input" || n.tagName === "button"), false);
    assert.equal(readOnly.controls().some(n => n.tagName === "img"), false);
});

test("stock error keeps the order and remains visible after refresh", async () => {
    const h = harness();
    h.response(0, { orders: fixture() }); await flush();
    h.window.confirm = () => true;
    h.byKey("complete-1").listeners.click();
    const message = "Không đủ tồn kho để hoàn thành đơn";
    h.response(1, { message }, 409); await flush();
    h.response(2, { orders: fixture() }); await flush();
    assert.ok(h.nodes["order-list"].textContent.includes("ORDER-1"));
    assert.equal(h.nodes["barista-error"].textContent, message);
    assert.equal(h.nodes["barista-error"].hidden, false);
});

test("completed empty state uses the required text", () => {
    const h = harness([], "completed");
    assert.equal(h.nodes["order-list"].textContent, "Chưa có đơn nào đã pha.");
});
