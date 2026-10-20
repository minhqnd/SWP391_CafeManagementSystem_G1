<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${empty requestScope.baristaView}">
    <c:redirect url="/barista"/>
</c:if>
<c:set var="completedView" value="${requestScope.baristaView == 'completed'}"/>
<c:set var="pageTitle" value="${completedView ? 'Đơn đã pha' : 'Đơn pha chế'}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Aura Café | <c:out value="${pageTitle}"/></title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/barista.css" rel="stylesheet">
</head>
<body data-context-path="${pageContext.request.contextPath}"
      data-csrf-token="${sessionScope.baristaCsrfToken}" data-barista-view="${completedView ? 'completed' : 'active'}">
    <jsp:include page="/components/header.jsp">
        <jsp:param name="navbarMode" value="barista"/>
        <jsp:param name="showCart" value="false"/>
        <jsp:param name="activePage" value="${completedView ? 'barista-completed' : 'barista-orders'}"/>
    </jsp:include>
    <main class="barista-main">
        <h1><c:out value="${pageTitle}"/></h1>
        <p id="barista-error" class="alert alert-danger" role="alert"<c:if test="${empty requestScope.baristaInitialError}"> hidden</c:if>><c:out value="${requestScope.baristaInitialError}"/></p>
        <div class="${completedView ? '' : 'kds-layout'}">
            <c:if test="${not completedView}">
            <aside class="group-panel" aria-labelledby="group-heading">
                <h2 id="group-heading">Món đang pha</h2>
                <div id="group-list" class="group-list"></div>
            </aside>
            </c:if>
            <section class="orders-panel" aria-labelledby="orders-heading">
                <h2 id="orders-heading" class="${completedView ? 'visually-hidden' : ''}">${completedView ? 'Đơn đã pha' : 'Đơn đang pha'}</h2>
                <div id="order-list" class="order-grid"></div>
            </section>
        </div>
    </main>
    <script id="barista-initial-data" type="application/json"><c:out value="${requestScope.baristaInitialData}" escapeXml="false" default="{}"/></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script id="barista-script">
// NEW: Shared state helpers keep item grouping, progress and both pages consistent.
(function (root) {
    "use strict";
    function options(value) {
        if (value == null || value === "") return {};
        if (typeof value !== "string") return value;
        try { return JSON.parse(value) || {}; } catch (_) { return value; }
    }
    function canonical(value) {
        if (Array.isArray(value)) return value.map(canonical).sort((a, b) => JSON.stringify(a).localeCompare(JSON.stringify(b)));
        if (value && typeof value === "object") {
            return Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])]));
        }
        return value;
    }
    function groupKey(item) {
        return JSON.stringify([item.productId, item.productName, canonical(options(item.customization))]);
    }
    function activeOrders(orders) {
        return orders.filter(o => ["CONFIRMED", "PREPARING"].includes(o.status))
            .sort((a, b) => String(a.createdAt || "").localeCompare(String(b.createdAt || "")) || a.id - b.id);
    }
    function completedOrders(orders) {
        return orders.filter(o => o.status === "READY").sort((a, b) =>
            String(b.readyAt || "").localeCompare(String(a.readyAt || ""))
            || String(b.createdAt || "").localeCompare(String(a.createdAt || "")) || b.id - a.id);
    }
    function groups(orders) {
        const result = new Map();
        activeOrders(orders).forEach(order => order.items.forEach(item => {
            if (item.isPrepared) return;
            const key = groupKey(item);
            if (!result.has(key)) result.set(key, { key, item, quantity: 0, itemIds: [], orders: new Set() });
            const group = result.get(key);
            group.quantity += item.quantity;
            group.itemIds.push(item.itemId);
            group.orders.add(order.orderCode || "#" + order.id);
        }));
        return [...result.values()];
    }
    function progress(order) {
        const done = order.items.filter(item => item.isPrepared).length;
        return { done, total: order.items.length, remaining: order.items.length - done };
    }
    function markItems(orders, ids, prepared) {
        const selected = new Set(ids);
        activeOrders(orders).forEach(order => {
            let touched = false;
            order.items.forEach(item => {
                if (selected.has(item.itemId)) { item.isPrepared = prepared; touched = true; }
            });
            if (touched && prepared && order.status === "CONFIRMED") order.status = "PREPARING";
        });
    }
    function markReady(orders, id) {
        const order = orders.find(o => o.id === id);
        if (order) {
            order.items.forEach(item => { item.isPrepared = true; });
            order.status = "READY";
            // Server supplies the authoritative completion timestamp on the next fetch.
        }
    }
    const helpers = { options, groupKey, activeOrders, completedOrders, groups, progress, markItems, markReady };
    if (typeof module !== "undefined" && module.exports) module.exports = helpers;
    if (!root.document) return;

    // NEW: Render with textContent only; database names, notes and options are never HTML.
    const doc = root.document;
    const completed = doc.body.dataset.baristaView === "completed";
    const endpoint = doc.body.dataset.contextPath + "/barista";
    const list = doc.getElementById("order-list");
    const groupList = doc.getElementById("group-list");
    const errorBox = doc.getElementById("barista-error");
    let orders = [], busy = false, loading = false, revision = 0, actionError = "";
    try { orders = JSON.parse(doc.getElementById("barista-initial-data").textContent).orders || []; }
    catch (_) { showError("Không đọc được dữ liệu đơn hàng."); }

    function element(tag, className, text) {
        const node = doc.createElement(tag);
        if (className) node.className = className;
        if (text != null) node.textContent = text;
        return node;
    }
    function showError(message) { errorBox.textContent = message; errorBox.hidden = !message; }
    function tags(item) {
        const box = element("div", "option-tags");
        const value = options(item.customization);
        const labels = { size: "Size", ice: "Đá", sugar: "Đường", topping: "Topping", toppings: "Topping", note: "Ghi chú" };
        function describe(v) {
            if (Array.isArray(v)) return v.map(describe).join(", ");
            if (v && typeof v === "object") return Object.entries(v).map(([k, x]) => k + ": " + describe(x)).join(", ");
            return String(v);
        }
        const entries = value && typeof value === "object" && !Array.isArray(value)
            ? Object.entries(value).map(([key, v]) => v == null || v === "" ? "" : (labels[key] || key) + ": " + describe(v))
            : [describe(value)];
        entries.filter(Boolean).forEach(text => box.append(element("span", "option-tag", text)));
        return box;
    }
    function checkbox(checked, key, change, labelText) {
        const label = element("label", "prepared-toggle");
        const input = element("input");
        input.type = "checkbox";
        input.checked = checked;
        input.disabled = busy;
        input.dataset.focusKey = key;
        input.setAttribute("aria-label", labelText);
        input.addEventListener("change", () => change(input.checked));
        label.append(input, element("span", "", "Đã pha"));
        return label;
    }
    function orderCard(order) {
        const card = element("article", "order-card");
        const heading = element("div", "order-heading");
        heading.append(element("h3", "order-code", order.orderCode || "#" + order.id));
        const meta = element("div", "order-meta");
        meta.append(element("span", "order-type", { DINE_IN: "Tại quán", TAKEAWAY: "Mang đi", ONLINE: "Online" }[order.orderType] || order.orderType));
        if (completed) meta.append(element("span", "ready-badge", "✓ Đã pha"));
        heading.append(meta);
        const person = [order.tableNumber ? "Bàn " + order.tableNumber : "", order.recipientName || ""].filter(Boolean).join(" · ");
        if (person) heading.append(element("p", "order-person", person));
        card.append(heading);
        const items = element("ul", "item-list");
        order.items.forEach(item => {
            const row = element("li", "order-item" + (!completed && item.isPrepared ? " is-prepared" : ""));
            const top = element("div", "item-top");
            top.append(element("span", "item-quantity", "×" + item.quantity), element("span", "item-name", item.productName));
            row.append(top, tags(item));
            if (!completed) row.append(checkbox(item.isPrepared, "item-" + item.itemId,
                value => updateItems([item.itemId], value), "Đã pha " + item.productName + " của đơn " + (order.orderCode || order.id)));
            items.append(row);
        });
        card.append(items);
        if (order.orderNote) card.append(element("p", "order-note", "Ghi chú: " + order.orderNote));
        if (!completed) {
            const state = progress(order);
            const actions = element("div", "order-actions");
            actions.append(element("p", "order-progress", state.done + "/" + state.total + " món đã pha"));
            if (state.remaining) actions.append(element("p", "order-warning", "Còn " + state.remaining + " món chưa tích"));
            const button = element("button", "complete-button" + (state.remaining ? "" : " all-prepared"), "Hoàn thành đơn");
            button.type = "button";
            button.disabled = busy;
            button.dataset.focusKey = "complete-" + order.id;
            button.addEventListener("click", () => finish(order));
            actions.append(button);
            card.append(actions);
        }
        return card;
    }
    function render() {
        const focusKey = doc.activeElement && doc.activeElement.dataset.focusKey;
        const groupScroll = groupList ? groupList.scrollTop : 0;
        list.replaceChildren();
        const visible = completed ? completedOrders(orders) : activeOrders(orders);
        visible.forEach(order => list.append(orderCard(order)));
        if (!visible.length) list.append(element("p", "empty-state", completed ? "Chưa có đơn nào đã pha." : "Không có đơn đang chờ pha chế."));
        if (groupList) {
            groupList.replaceChildren();
            const pending = groups(orders);
            pending.forEach(group => {
                const card = element("div", "group-card");
                const title = element("div", "group-title");
                title.append(element("span", "group-quantity", "×" + group.quantity), element("span", "item-name", group.item.productName));
                card.append(title, tags(group.item), element("p", "group-orders", [...group.orders].join(" · ")),
                    checkbox(false, "group-" + group.key, () => updateItems(group.itemIds, true), "Đã pha tất cả " + group.quantity + " " + group.item.productName));
                groupList.append(card);
            });
            if (!pending.length) groupList.append(element("p", "empty-state", "Không còn món chưa tích."));
            groupList.scrollTop = groupScroll;
        }
        if (focusKey) {
            const next = [...doc.querySelectorAll("[data-focus-key]")].find(node => node.dataset.focusKey === focusKey);
            if (next && !next.disabled) next.focus({ preventScroll: true });
        }
    }

    // NEW: Serialize mutations and discard polling responses that predate a local change.
    async function responseData(response) {
        let data;
        try { data = await response.json(); } catch (_) { throw new Error("Máy chủ trả về dữ liệu không hợp lệ. Vui lòng kiểm tra phiên đăng nhập."); }
        if (!response.ok) throw new Error(data.message || "Không thực hiện được thao tác.");
        return data;
    }
    async function refresh(clearError = true) {
        if (busy || loading) return;
        loading = true;
        const expected = revision;
        try {
            const response = await fetch(endpoint + "?action=orders", { credentials: "same-origin", cache: "no-store" });
            const data = await responseData(response);
            if (!Array.isArray(data.orders)) throw new Error("Dữ liệu đơn hàng không hợp lệ.");
            if (!busy && expected === revision) {
                orders = data.orders;
                if (clearError && !actionError) showError("");
                render();
            }
        } catch (error) {
            if (!busy && expected === revision && !actionError) {
                showError(error.message || "Không kết nối được máy chủ.");
            }
        } finally { loading = false; }
    }
    async function mutate(parameters, optimistic, onSuccess) {
        if (busy) return;
        busy = true;
        revision++;
        const previous = JSON.parse(JSON.stringify(orders));
        actionError = "";
        showError("");
        optimistic();
        render();
        let success = false;
        try {
            const response = await fetch(endpoint, {
                method: "POST", credentials: "same-origin",
                body: new URLSearchParams({ ...parameters, csrfToken: doc.body.dataset.csrfToken })
            });
            const data = await responseData(response);
            if (data.success !== true) throw new Error("Máy chủ chưa xác nhận thay đổi.");
            if (onSuccess) onSuccess();
            success = true;
        } catch (error) {
            orders = previous;
            actionError = error.message || "Không lưu được thay đổi. Vui lòng thử lại.";
            showError(actionError);
        } finally {
            busy = false;
            render();
            await refresh(success);
        }
    }
    function updateItems(ids, prepared) {
        return mutate({ action: "update-items", itemIds: ids.join(","), isPrepared: String(prepared) },
            () => markItems(orders, ids, prepared));
    }
    function finish(order) {
        if (busy) return;
        if (progress(order).remaining && !root.confirm("Đơn vẫn còn món chưa đánh dấu hoàn thành. Bạn có muốn hoàn tất toàn bộ đơn không?")) return;
        // Keep the card until the server confirms the existing READY status update.
        return mutate({ action: "update-order-status", orderId: String(order.id), status: "READY" },
            () => {}, () => markReady(orders, order.id));
    }
    render();
    refresh(!errorBox.textContent.trim());
    const timer = root.setInterval(() => refresh(), 5000);
    root.addEventListener("pagehide", () => root.clearInterval(timer), { once: true });
})(typeof window !== "undefined" ? window : globalThis);
    </script>
</body>
</html>
