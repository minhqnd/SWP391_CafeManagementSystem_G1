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
    <link href="${pageContext.request.contextPath}/assets/css/barista.css?v=20261030-3" rel="stylesheet">
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
        try {
            return JSON.parse(value) || {};
        } catch (_) {
            return value;
        }
    }
    function canonical(value) {
        if (Array.isArray(value))
            return value.map(canonical).sort((a, b) => JSON.stringify(a).localeCompare(JSON.stringify(b)));
        if (value && typeof value === "object") {
            return Object.fromEntries(
                Object.keys(value)
                    .sort()
                    .map((key) => [key, canonical(value[key])]),
            );
        }
        return value;
    }
    function groupKey(item) {
        return JSON.stringify([item.productId, item.productName, canonical(options(item.customization))]);
    }
    function activeOrders(orders) {
        return orders
            .filter((o) => ["CONFIRMED", "PREPARING"].includes(o.status))
            .sort((a, b) => String(a.createdAt || "").localeCompare(String(b.createdAt || "")) || a.id - b.id);
    }
    function completedOrders(orders, now = new Date()) {
        const today = new Intl.DateTimeFormat("en-CA", {
            timeZone: "Asia/Ho_Chi_Minh", year: "numeric", month: "2-digit", day: "2-digit",
        }).format(now);
        return orders
            // Legacy orders have no persisted completion time; fall back to creation date.
            .filter((o) => o.status === "READY" && String(o.readyAt || o.createdAt || "").slice(0, 10) === today)
            .sort(
                (a, b) =>
                    String(b.readyAt || "").localeCompare(String(a.readyAt || "")) ||
                    String(b.createdAt || "").localeCompare(String(a.createdAt || "")) ||
                    b.id - a.id,
            );
    }
    // Gom theo món (VD "×2 Trà chanh"); từng cốc vẫn giữ tuỳ chọn riêng (size, đường, đá, topping) trong lines.
    // activeOrders đã xếp đơn sớm trước, nên nhóm và các dòng trong nhóm đều theo thứ tự ưu tiên pha.
    function productKey(item) {
        return JSON.stringify(item.productId);
    }
    function groups(orders) {
        const result = new Map();
        activeOrders(orders).forEach((order) =>
            order.items.forEach((item) => {
                const key = productKey(item);
                if (!result.has(key))
                    result.set(key, { key, item, quantity: 0, itemIds: [], orders: new Set(), lines: [] });
                const group = result.get(key);
                group.quantity += item.quantity;
                group.itemIds.push(item.itemId);
                group.orders.add(order.orderCode || "#" + order.id);
                group.lines.push({ item, order });
            }),
        );
        return [...result.values()];
    }
    // Partial rows exist only in this page. Complete rows reuse the server's in-memory flag.
    const partialCups = new Map();
    function cups(item) {
        const local = partialCups.get(item.itemId);
        return Array.from({ length: item.quantity }, (_, index) => ({
            key: item.itemId + ":" + index,
            index,
            prepared: local ? !!local[index] : !!item.isPrepared,
        }));
    }
    function groupComplete(group) {
        return group.lines.every(({ item }) => cups(item).every((cup) => cup.prepared));
    }
    function reconcile(nextOrders) {
        const active = new Map(activeOrders(nextOrders).flatMap((o) => o.items.map((i) => [i.itemId, i])));
        for (const [id, state] of partialCups) {
            const item = active.get(id);
            if (!item || item.isPrepared || item.quantity !== state.length) partialCups.delete(id);
        }
    }
    function progress(order) {
        const all = order.items.flatMap((item) => cups(item));
        const done = all.filter((cup) => cup.prepared).length;
        return { done, total: all.length, remaining: all.length - done };
    }
    function markItems(orders, ids, prepared) {
        const selected = new Set(ids);
        activeOrders(orders).forEach((order) => {
            let touched = false;
            order.items.forEach((item) => {
                if (selected.has(item.itemId)) {
                    item.isPrepared = prepared;
                    touched = true;
                }
            });
            if (touched && prepared && order.status === "CONFIRMED") order.status = "PREPARING";
        });
    }
    function markReady(orders, id) {
        const order = orders.find((o) => o.id === id);
        if (order) {
            order.items.forEach((item) => {
                item.isPrepared = true;
            });
            order.status = "READY";
            // Server supplies the authoritative completion timestamp on the next fetch.
        }
    }
    const helpers = {
        cups,
        partialCups,
        reconcile,
        options,
        groupKey,
        productKey,
        activeOrders,
        completedOrders,
        groups,
        groupComplete,
        progress,
        markItems,
        markReady,
    };
    if (typeof module !== "undefined" && module.exports) module.exports = helpers;
    if (!root.document) return;

    // NEW: Render with textContent only; database names, notes and options are never HTML.
    const doc = root.document;
    const completed = doc.body.dataset.baristaView === "completed";
    const endpoint = doc.body.dataset.contextPath + "/barista";
    const list = doc.getElementById("order-list");
    const groupList = doc.getElementById("group-list");
    const errorBox = doc.getElementById("barista-error");
    let orders = [],
        busy = false,
        loading = false,
        revision = 0,
        actionError = "";
    try {
        orders = JSON.parse(doc.getElementById("barista-initial-data").textContent).orders || [];
    } catch (_) {
        showError("Không đọc được dữ liệu đơn hàng.");
    }

    function element(tag, className, text) {
        const node = doc.createElement(tag);
        if (className) node.className = className;
        if (text != null) node.textContent = text;
        return node;
    }
    function showError(message) {
        errorBox.textContent = message;
        errorBox.hidden = !message;
    }
    function tags(item, separateNotes = false) {
        const box = element("div", "option-tags");
        const value = options(item.customization);
        const labels = {
            size: "Size",
            ice: "Đá",
            sugar: "Đường",
            topping: "Topping",
            toppings: "Topping",
            note: "Ghi chú",
        };
        function describe(v) {
            if (Array.isArray(v)) return v.map(describe).join(", ");
            if (v && typeof v === "object")
                return Object.entries(v)
                    .map(([k, x]) => k + ": " + describe(x))
                    .join(", ");
            return String(v);
        }
        // "notes" (đơn tại quầy) đã là chữ đầy đủ như "50% đường", "Lưu ý: ít ngọt" -> mỗi ý một nhãn, không thêm tiền tố
        const entries =
            value && typeof value === "object" && !Array.isArray(value)
                ? Object.entries(value).flatMap(([key, v]) => {
                      if (v == null || v === "" || (Array.isArray(v) && !v.length)) return [];
                      if (key === "notes") {
                          // VD "50% đường, Ít đá, Lưu ý: ít ngọt, thêm ống hút" -> "Lưu ý: ..." luôn đứng cuối, giữ nguyên
                          const text = String(v);
                          const noteAt = text.indexOf("Lưu ý:");
                          const head = noteAt >= 0 ? text.slice(0, noteAt) : text;
                          const parts = head
                              .split(",")
                              .map((part) => part.trim())
                              .filter(Boolean);
                          if (noteAt >= 0) parts.push(text.slice(noteAt).trim());
                          return parts;
                      }
                      return [(labels[key] || key) + ": " + describe(v)];
                  })
                : [describe(value)];
        const notes = [];
        entries.filter(Boolean).forEach((text) => {
            if (separateNotes && /^(Ghi chú|Lưu ý):/i.test(text)) {
                const content = text.replace(/^(Ghi chú|Lưu ý):\s*/i, "").trim();
                if (content) notes.push(content);
            } else {
                box.append(element("span", "option-tag", text));
            }
        });
        if (!separateNotes) return box;
        const details = element("div", "cup-details");
        if (box.children.length) details.append(box);
        if (notes.length) {
            const note = element("div", "cup-special-note");
            note.append(element("strong", "", "Lưu ý của khách:"), element("div", "", notes.join("\n")));
            details.append(note);
        }
        return details;
    }
    function checkbox(checked, key, change, labelText, display = "Đã pha") {
        const label = element("label", "prepared-toggle");
        const input = element("input");
        input.type = "checkbox";
        input.checked = checked;
        input.disabled = busy;
        input.dataset.focusKey = key;
        input.setAttribute("aria-label", labelText);
        input.addEventListener("change", () => change(input.checked));
        label.append(input, element("span", "", display));
        return label;
    }
    let selectedGroup = null;
    const dialog = element("dialog", "cup-dialog");
    dialog.setAttribute("aria-labelledby", "cup-dialog-title");
    const dialogHeader = element("div", "cup-dialog-header");
    const dialogTitle = element("h2", "", "Chi tiết món");
    dialogTitle.id = "cup-dialog-title";
    const closeButton = element("button", "cup-close", "×");
    closeButton.type = "button";
    closeButton.setAttribute("aria-label", "Đóng chi tiết món");
    closeButton.addEventListener("click", () => dialog.close());
    dialogHeader.append(dialogTitle, closeButton);
    const dialogBody = element("div", "cup-dialog-body");
    const dialogProgress = element("div", "cup-dialog-progress");
    dialog.append(dialogHeader, dialogProgress, dialogBody);
    if (!completed) doc.body.append(dialog);
    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) dialog.close();
    });
    dialog.addEventListener("close", () => {
        selectedGroup = null;
    });
    function renderDialog() {
        if (!dialog.open) return;
        const group = groups(orders).find((g) => g.key === selectedGroup);
        if (!group) {
            dialog.close();
            return;
        }
        const scroll = dialogBody.scrollTop;
        dialogTitle.textContent = group.item.productName + " × " + group.quantity;
        const done = group.lines.reduce((n, line) => n + cups(line.item).filter((c) => c.prepared).length, 0);
        const count = element("p", "order-progress", "Đã pha " + done + "/" + group.quantity + " ly");
        count.setAttribute("aria-live", "polite");
        dialogProgress.replaceChildren(count);
        dialogBody.replaceChildren();
        group.lines.forEach(({ item, order }) =>
            cups(item).forEach((cup) => {
                const row = element("div", "group-line" + (cup.prepared ? " is-prepared" : ""));
                const head = element("div", "cup-card-header");
                head.append(element("strong", "cup-card-title", order.orderCode || "#" + order.id));
                row.append(
                    head,
                    tags(item, true),
                    checkbox(
                        cup.prepared,
                        "modal-" + cup.key,
                        (value) => updateCups(item, cup.index, value),
                        "Đã pha ly " + (cup.index + 1) + " đơn " + (order.orderCode || order.id),
                    ),
                );
                dialogBody.append(row);
            }),
        );
        dialogBody.scrollTop = scroll;
    }
    function orderCard(order) {
        const card = element("article", "order-card");
        const heading = element("div", "order-heading");
        heading.append(element("h3", "order-code", order.orderCode || "#" + order.id));
        const meta = element("div", "order-meta");
        meta.append(
            element(
                "span",
                "order-type",
                { DINE_IN: "Tại quán", TAKEAWAY: "Mang đi", ONLINE: "Online" }[order.orderType] || order.orderType,
            ),
        );
        if (completed) meta.append(element("span", "ready-badge", "✓ Đã pha"));
        heading.append(meta);
        const person = [order.tableNumber ? "Bàn " + order.tableNumber : "", order.recipientName || ""]
            .filter(Boolean)
            .join(" · ");
        if (person) heading.append(element("p", "order-person", person));
        card.append(heading);
        const items = element("ul", "item-list");
        order.items.forEach((item) => {
            const row = element(
                "li",
                "order-item" + (!completed && cups(item).every((cup) => cup.prepared) ? " is-prepared" : ""),
            );
            const top = element("div", "item-top");
            top.append(
                element("span", "item-quantity", "×" + item.quantity),
                element("span", "item-name", item.productName),
            );
            row.append(top, tags(item));
            if (!completed) {
                const toggles = element("div", "cup-toggles");
                cups(item).forEach((cup) => {
                    const toggle = checkbox(
                        cup.prepared,
                        "cup-" + cup.key,
                        (value) => updateCups(item, cup.index, value),
                        "Đã pha ly " + (cup.index + 1) + " " + item.productName,
                        "Đã pha",
                    );
                    if (cup.prepared) toggle.className += " is-prepared";
                    toggles.append(toggle);
                });
                row.append(toggles);
            }
            items.append(row);
        });
        card.append(items);
        if (order.orderNote) card.append(element("p", "order-note", "Ghi chú: " + order.orderNote));
        if (!completed) {
            const state = progress(order);
            const actions = element("div", "order-actions");
            actions.append(element("p", "order-progress", state.done + "/" + state.total + " ly đã pha"));
            const button = element(
                "button",
                "complete-button" + (state.remaining ? "" : " all-prepared"),
                "Hoàn thành đơn",
            );
            button.type = "button";
            button.disabled = busy || state.remaining > 0 || state.total === 0;
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
        visible.forEach((order) => list.append(orderCard(order)));
        if (!visible.length)
            list.append(
                element("p", "empty-state", completed ? "Chưa có đơn nào đã pha trong ngày hôm nay." : "Không có đơn đang chờ pha chế."),
            );
        if (groupList) {
            groupList.replaceChildren();
            const pending = groups(orders).filter((group) => !groupComplete(group));
            pending.forEach((group) => {
                const card = element("div", "group-card");
                const title = element("button", "group-title group-open");
                title.type = "button";
                title.dataset.focusKey = "open-" + group.key;
                title.setAttribute("aria-haspopup", "dialog");
                title.append(
                    element("span", "item-name", group.item.productName),
                    element("span", "group-quantity", "× " + group.quantity),
                );
                title.addEventListener("click", () => {
                    selectedGroup = group.key;
                    dialog.showModal();
                    renderDialog();
                });
                card.append(
                    title,
                    checkbox(
                        groupComplete(group),
                        "group-" + group.key,
                        (value) => updateItems(group.itemIds, value),
                        "Đã pha tất cả " + group.quantity + " " + group.item.productName,
                        "Đã pha",
                    ),
                );
                groupList.append(card);
            });
            if (!pending.length) groupList.append(element("p", "empty-state", "Không còn món chưa tích."));
            groupList.scrollTop = groupScroll;
        }
        renderDialog();
        if (focusKey) {
            const next = [...doc.querySelectorAll("[data-focus-key]")].find(
                (node) => node.dataset.focusKey === focusKey,
            );
            if (next && !next.disabled) next.focus({ preventScroll: true });
        }
    }

    // NEW: Serialize mutations and discard polling responses that predate a local change.
    async function responseData(response) {
        let data;
        try {
            data = await response.json();
        } catch (_) {
            throw new Error("Máy chủ trả về dữ liệu không hợp lệ. Vui lòng kiểm tra phiên đăng nhập.");
        }
        if (!response.ok) throw new Error(data.message || "Không thực hiện được thao tác.");
        return data;
    }
    async function refresh(clearError = true) {
        if (busy || loading) return;
        loading = true;
        const expected = revision;
        try {
            const response = await fetch(endpoint + "?action=orders", {
                credentials: "same-origin",
                cache: "no-store",
            });
            const data = await responseData(response);
            if (!Array.isArray(data.orders)) throw new Error("Dữ liệu đơn hàng không hợp lệ.");
            if (!busy && expected === revision) {
                reconcile(data.orders);
                orders = data.orders;
                if (clearError && !actionError) showError("");
                render();
            }
        } catch (error) {
            if (!busy && expected === revision && !actionError) {
                showError(error.message || "Không kết nối được máy chủ.");
            }
        } finally {
            loading = false;
        }
    }
    async function mutate(parameters, optimistic, onSuccess) {
        if (busy) return;
        busy = true;
        revision++;
        const previous = JSON.parse(JSON.stringify(orders));
        const previousCups = new Map([...partialCups].map(([id, state]) => [id, [...state]]));
        actionError = "";
        showError("");
        optimistic();
        render();
        let success = false;
        try {
            const response = await fetch(endpoint, {
                method: "POST",
                credentials: "same-origin",
                body: new URLSearchParams({ ...parameters, csrfToken: doc.body.dataset.csrfToken }),
            });
            const data = await responseData(response);
            if (data.success !== true) throw new Error("Máy chủ chưa xác nhận thay đổi.");
            if (onSuccess) onSuccess();
            success = true;
        } catch (error) {
            orders = previous;
            partialCups.clear();
            previousCups.forEach((state, id) => partialCups.set(id, state));
            actionError = error.message || "Không lưu được thay đổi. Vui lòng thử lại.";
            showError(actionError);
        } finally {
            busy = false;
            render();
            await refresh(success);
        }
    }
    function updateItems(ids, prepared) {
        return mutate({ action: "update-items", itemIds: ids.join(","), isPrepared: String(prepared) }, () => {
            ids.forEach((id) => partialCups.delete(id));
            markItems(orders, ids, prepared);
        });
    }
    function updateCups(item, index, prepared) {
        if (busy) return;
        const state = cups(item).map((c) => c.prepared);
        state[index] = prepared;
        const allDone = state.every(Boolean);
        const apply = () => {
            partialCups.set(item.itemId, state);
            markItems(orders, [item.itemId], allDone);
        };
        if (allDone !== !!item.isPrepared) {
            return mutate({ action: "update-items", itemIds: String(item.itemId), isPrepared: String(allDone) }, apply);
        }
        revision++;
        apply();
        render();
    }
    function finish(order) {
        if (busy) return;
        const state = progress(order);
        if (state.remaining || !state.total) return;
        // Keep the card until the server confirms the existing READY status update.
        return mutate(
            { action: "update-order-status", orderId: String(order.id), status: "READY" },
            () => {},
            () => markReady(orders, order.id),
        );
    }
    render();
    refresh(!errorBox.textContent.trim());
    const timer = root.setInterval(() => refresh(), 5000);
    root.addEventListener("pagehide", () => root.clearInterval(timer), { once: true });
})(typeof window !== "undefined" ? window : globalThis);
    </script>
</body>
</html>
