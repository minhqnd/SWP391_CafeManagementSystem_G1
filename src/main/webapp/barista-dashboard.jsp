<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <c:if test="${empty requestScope.baristaDashboard}">
            <c:redirect url="/barista" />
        </c:if>
        <!DOCTYPE html>
        <html lang="vi">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Aura Café | KDS Barista</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
            <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap"
                rel="stylesheet">
            <link
                href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200"
                rel="stylesheet">
            <style>
                :root {
                    --aura-primary: #900b1a;
                    --aura-primary-dark: #710916;
                    --stone-50: #fafaf9;
                    --stone-100: #f5f5f4;
                    --stone-200: #e7e5e4;
                    --stone-500: #78716c;
                    --stone-800: #292524;
                }

                * {
                    box-sizing: border-box;
                }

                body {
                    min-height: 100vh;
                    margin: 0;
                    padding-bottom: 42px;
                    background: var(--stone-100);
                    color: var(--stone-800);
                    font-family: "Inter", sans-serif;
                    font-size: 14px;
                }

                .material-symbols-outlined {
                    font-size: 20px;
                    line-height: 1;
                    vertical-align: middle;
                }

                .kds-toolbar {
                    position: sticky;
                    top: 72px;
                    z-index: 1020;
                    border-bottom: 1px solid var(--stone-200);
                    background: rgba(255, 255, 255, .96);
                    box-shadow: 0 2px 8px rgba(41, 37, 36, .04);
                    backdrop-filter: blur(10px);
                }

                .metric-card {
                    min-width: 155px;
                    padding: 9px 13px;
                    border: 1px solid var(--stone-200);
                    border-radius: 13px;
                    background: var(--stone-50);
                }

                .metric-card.waiting {
                    border-color: #fecdd3;
                    background: #fff1f2;
                    color: #9f1239;
                }

                .metric-card.preparing {
                    border-color: #fde68a;
                    background: #fffbeb;
                    color: #92400e;
                }

                .metric-card.ready {
                    border-color: #a7f3d0;
                    background: #ecfdf5;
                    color: #047857;
                }

                .metric-label {
                    font-size: 10px;
                    font-weight: 800;
                    letter-spacing: .04em;
                    text-transform: uppercase;
                }

                .metric-value {
                    font-size: 17px;
                    font-weight: 800;
                    line-height: 1.15;
                }

                .live-dot {
                    width: 9px;
                    height: 9px;
                    border-radius: 50%;
                    background: #e11d48;
                    box-shadow: 0 0 0 5px rgba(225, 29, 72, .1);
                }

                .toolbar-btn {
                    min-height: 38px;
                    border-radius: 11px;
                    font-size: 12px;
                    font-weight: 700;
                }

                .btn-aura {
                    --bs-btn-color: #fff;
                    --bs-btn-bg: var(--aura-primary);
                    --bs-btn-border-color: var(--aura-primary);
                    --bs-btn-hover-color: #fff;
                    --bs-btn-hover-bg: var(--aura-primary-dark);
                    --bs-btn-hover-border-color: var(--aura-primary-dark);
                }

                .station-scroll {
                    scrollbar-width: thin;
                }

                .station-btn {
                    flex: 0 0 auto;
                    padding: 7px 12px;
                    border: 0;
                    border-radius: 9px;
                    background: var(--stone-100);
                    color: #57534e;
                    font-size: 12px;
                    font-weight: 700;
                    white-space: nowrap;
                }

                .station-btn:hover {
                    background: #e7e5e4;
                }

                .station-btn.active {
                    background: var(--aura-primary);
                    color: #fff;
                    box-shadow: 0 2px 5px rgba(144, 11, 26, .2);
                }

                .ticket-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
                    gap: 16px;
                    align-items: start;
                }

                .ticket {
                    overflow: hidden;
                    border: 1px solid var(--stone-200);
                    border-radius: 16px;
                    background: #fff;
                    box-shadow: 0 2px 7px rgba(41, 37, 36, .05);
                    transition: transform .18s ease, box-shadow .18s ease;
                }

                .ticket:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 8px 22px rgba(41, 37, 36, .09);
                }

                .ticket.is-overdue {
                    border-color: #fda4af;
                    box-shadow: 0 0 0 3px rgba(225, 29, 72, .1);
                }

                .ticket.is-ready {
                    opacity: .78;
                }

                .ticket-ribbon {
                    height: 6px;
                }

                .ticket-ribbon.waiting {
                    background: #e11d48;
                }

                .ticket-ribbon.preparing {
                    background: #f59e0b;
                }

                .ticket-ribbon.ready {
                    background: #10b981;
                }

                .ticket-header {
                    padding: 13px 14px;
                    border-bottom: 1px solid #f5f5f4;
                    background: rgba(250, 250, 249, .75);
                }

                .ticket.is-overdue .ticket-header {
                    background: #fff1f2;
                }

                .ticket-code {
                    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
                    font-size: 15px;
                    font-weight: 800;
                }

                .status-badge {
                    padding: 4px 7px;
                    border-radius: 6px;
                    font-size: 9px;
                    font-weight: 800;
                    letter-spacing: .04em;
                }

                .status-badge.waiting {
                    background: #ffe4e6;
                    color: #be123c;
                }

                .status-badge.preparing {
                    background: #fef3c7;
                    color: #92400e;
                }

                .status-badge.ready {
                    background: #d1fae5;
                    color: #047857;
                }

                .ticket.is-overdue .status-badge {
                    background: #e11d48;
                    color: #fff;
                }

                .ticket-target {
                    margin-top: 4px;
                    color: #44403c;
                    font-size: 12px;
                    font-weight: 700;
                }

                .ticket-time {
                    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
                    font-size: 12px;
                    font-weight: 800;
                }

                .ticket.is-overdue .ticket-time,
                .ticket.is-overdue .ticket-code {
                    color: #be123c;
                }

                .ticket-body {
                    padding: 13px 14px;
                }

                .ticket-item {
                    padding: 10px;
                    border: 1px solid var(--stone-200);
                    border-radius: 11px;
                    background: rgba(250, 250, 249, .7);
                }

                .item-qty {
                    min-width: 26px;
                    color: var(--aura-primary);
                    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
                    font-size: 12px;
                    font-weight: 800;
                }

                .item-name {
                    font-size: 12px;
                    font-weight: 800;
                    line-height: 1.35;
                }

                .item-tag {
                    padding: 3px 6px;
                    border: 1px solid var(--stone-200);
                    border-radius: 5px;
                    background: #fff;
                    color: #57534e;
                    font-size: 9px;
                }

                .ticket-note {
                    padding: 9px 10px;
                    border: 1px solid #fecdd3;
                    border-radius: 10px;
                    background: #fff1f2;
                    color: #881337;
                    font-size: 10px;
                    font-weight: 600;
                    line-height: 1.5;
                }

                .ticket-footer {
                    padding: 11px 13px;
                    border-top: 1px solid var(--stone-200);
                    background: var(--stone-50);
                }

                .ticket-action {
                    min-height: 39px;
                    border-radius: 10px;
                    font-size: 11px;
                    font-weight: 800;
                    letter-spacing: .02em;
                }

                .empty-board {
                    grid-column: 1 / -1;
                    padding: 70px 20px;
                    border: 2px dashed #d6d3d1;
                    border-radius: 18px;
                    color: var(--stone-500);
                    text-align: center;
                }

                .kds-footer {
                    position: fixed;
                    right: 0;
                    bottom: 0;
                    left: 0;
                    z-index: 1030;
                    height: 42px;
                    border-top: 1px solid var(--stone-200);
                    background: rgba(255, 255, 255, .96);
                    color: var(--stone-500);
                    font-size: 11px;
                    backdrop-filter: blur(10px);
                }

                .kbd {
                    padding: 2px 6px;
                    border: 1px solid var(--stone-200);
                    border-radius: 5px;
                    background: var(--stone-100);
                    color: #57534e;
                    font-family: ui-monospace, monospace;
                }

                .toast-container {
                    z-index: 1090;
                }

                @media (min-width: 1500px) {
                    .ticket-grid {
                        grid-template-columns: repeat(5, minmax(0, 1fr));
                    }
                }

                @media (max-width: 991.98px) {
                    .kds-toolbar {
                        top: 68px;
                    }
                }

                @media (max-width: 575.98px) {
                    .metric-card {
                        min-width: calc(50% - 4px);
                        flex: 1;
                    }

                    .ticket-grid {
                        grid-template-columns: 1fr;
                    }

                    .kds-footer .shortcut-help {
                        display: none !important;
                    }
                }
            </style>
        </head>

        <body data-context-path="${pageContext.request.contextPath}" data-csrf-token="${sessionScope.baristaCsrfToken}">
            <jsp:include page="/components/header.jsp">
                <jsp:param name="navbarMode" value="barista" />
                <jsp:param name="showCart" value="false" />
                <jsp:param name="activePage" value="barista-orders" />
            </jsp:include>

            <section class="kds-toolbar px-3 px-lg-4 py-3" aria-label="Điều khiển KDS">
                <div class="d-flex flex-wrap align-items-center justify-content-between gap-3">
                    <div class="d-flex flex-wrap gap-2">
                        <div class="metric-card waiting d-flex align-items-center gap-3">
                            <span class="live-dot"></span>
                            <div>
                                <div class="metric-label">Đang chờ pha</div>
                                <div class="metric-value"><span id="waiting-count">0</span> đơn</div>
                            </div>
                        </div>
                        <div class="metric-card preparing d-flex align-items-center gap-3">
                            <span class="material-symbols-outlined">local_cafe</span>
                            <div>
                                <div class="metric-label">Đang thực hiện</div>
                                <div class="metric-value"><span id="preparing-count">0</span> đơn</div>
                            </div>
                        </div>
                        <div class="metric-card ready d-flex align-items-center gap-3">
                            <span class="material-symbols-outlined">check_circle</span>
                            <div>
                                <div class="metric-label">Đã hoàn tất</div>
                                <div class="metric-value"><span id="ready-count">0</span> đơn</div>
                            </div>
                        </div>
                    </div>
                    <div class="d-flex flex-wrap gap-2">
                        <button id="sound-button"
                            class="btn btn-light border toolbar-btn d-flex align-items-center gap-2" type="button"
                            aria-pressed="false">
                            <span id="sound-icon" class="material-symbols-outlined text-danger">volume_off</span><span
                                id="sound-label">Âm chuông: Tắt</span>
                        </button>
                        <button id="refresh-button"
                            class="btn btn-light border toolbar-btn d-flex align-items-center gap-2" type="button">
                            <span class="material-symbols-outlined">refresh</span><span>Làm mới</span>
                        </button>
                        <button id="fullscreen-button" class="btn btn-aura toolbar-btn d-flex align-items-center gap-2"
                            type="button">
                            <span class="material-symbols-outlined">fullscreen</span><span
                                class="d-none d-sm-inline">Toàn màn hình</span>
                        </button>
                    </div>
                </div>
                <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 border-top mt-3 pt-3">
                    <div id="category-filters" class="station-scroll d-flex gap-2 overflow-x-auto pb-1"
                        aria-label="Lọc trạm pha chế">
                        <button class="station-btn active" data-group="all" aria-pressed="true" type="button">Tất cả
                            trạm pha chế</button>
                    </div>
                    <div class="small text-secondary text-nowrap">Tự động làm mới: <strong
                            class="text-danger font-monospace">05s</strong></div>
                </div>
            </section>

            <main class="container-fluid px-3 px-lg-4 py-4">
                <div id="error-message" class="alert alert-danger" role="alert" hidden></div>
                <section id="ticket-board" class="ticket-grid" aria-label="Danh sách đơn pha chế"></section>
            </main>

            <div class="toast-container position-fixed top-0 end-0 p-3">
                <div id="kds-toast" class="toast border-0 shadow" role="status" aria-live="polite">
                    <div class="d-flex">
                        <div id="toast-message" class="toast-body fw-semibold"></div><button type="button"
                            class="btn-close me-2 m-auto" data-bs-dismiss="toast"></button>
                    </div>
                </div>
            </div>

            <footer class="kds-footer d-flex align-items-center justify-content-between px-3 px-lg-4">
                <div class="d-flex align-items-center gap-2 text-success fw-semibold"><span
                        class="material-symbols-outlined">cloud_done</span><span>KDS trực tuyến · Dữ liệu đồng bộ</span>
                </div>
                <div class="shortcut-help d-flex align-items-center gap-3"><span><span class="kbd">R</span> Làm
                        mới</span><span><span class="kbd">F</span> Toàn màn hình</span></div>
            </footer>

            <script id="barista-initial-data"
                type="application/json"><c:out value="${requestScope.baristaInitialData}" escapeXml="false" default="{}"/></script>
            <span id="barista-initial-error" hidden>
                <c:out value="${requestScope.baristaInitialError}" />
            </span>

            <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
            <script>
                "use strict";
                const contextPath = document.body.dataset.contextPath;
                const refreshIntervalMs = 5000;
                const board = document.getElementById("ticket-board");
                const errorMessage = document.getElementById("error-message");
                const toast = bootstrap.Toast.getOrCreateInstance(document.getElementById("kds-toast"), { delay: 2600 });
                let activeGroup = "all";
                let currentOrders = [];
                let isUpdating = false;
                let loadController = null;
                let refreshTimer = null;
                let soundEnabled = false;
                let audioContext = null;
                let notificationInitialized = false;
                const seenOrderIds = new Set();

                function node(tag, className, text) {
                    const result = document.createElement(tag);
                    if (className) result.className = className;
                    if (text !== undefined && text !== null) result.textContent = text;
                    return result;
                }

                function showToast(message) {
                    document.getElementById("toast-message").textContent = message;
                    toast.show();
                }

                function showError(message) {
                    errorMessage.textContent = message;
                    errorMessage.hidden = false;
                }

                function hideError() {
                    errorMessage.textContent = "";
                    errorMessage.hidden = true;
                }

                function updateSoundButton() {
                    document.getElementById("sound-label").textContent = "Âm chuông: " + (soundEnabled ? "Bật" : "Tắt");
                    document.getElementById("sound-icon").textContent = soundEnabled ? "volume_up" : "volume_off";
                    document.getElementById("sound-button").setAttribute("aria-pressed", String(soundEnabled));
                }

                function playNotificationSound() {
                    if (!soundEnabled || !audioContext || audioContext.state !== "running") return;
                    [660, 880].forEach((frequency, index) => {
                        const oscillator = audioContext.createOscillator();
                        const gain = audioContext.createGain();
                        const start = audioContext.currentTime + index * .16;
                        oscillator.frequency.value = frequency;
                        gain.gain.setValueAtTime(.001, start);
                        gain.gain.exponentialRampToValueAtTime(.12, start + .02);
                        gain.gain.exponentialRampToValueAtTime(.001, start + .14);
                        oscillator.connect(gain); gain.connect(audioContext.destination);
                        oscillator.start(start); oscillator.stop(start + .15);
                    });
                }

                document.getElementById("sound-button").addEventListener("click", async () => {
                    if (soundEnabled) {
                        soundEnabled = false;
                    } else {
                        try {
                            const AudioContextClass = window.AudioContext || window.webkitAudioContext;
                            if (!AudioContextClass) throw new Error("Trình duyệt không hỗ trợ âm thanh");
                            audioContext = audioContext || new AudioContextClass();
                            await audioContext.resume();
                            soundEnabled = audioContext.state === "running";
                            if (soundEnabled) playNotificationSound();
                        } catch (error) {
                            showToast(error.message);
                        }
                    }
                    updateSoundButton();
                });

                document.getElementById("fullscreen-button").addEventListener("click", async () => {
                    try {
                        if (document.fullscreenElement) await document.exitFullscreen();
                        else await document.documentElement.requestFullscreen();
                    } catch (error) { showToast("Không thể chuyển chế độ toàn màn hình"); }
                });

                document.getElementById("refresh-button").addEventListener("click", () => {
                    loadOrders();
                    showToast("Đang làm mới dữ liệu KDS");
                });

                document.addEventListener("keydown", event => {
                    if (event.target.matches("input, textarea, select") || event.ctrlKey || event.altKey || event.metaKey) return;
                    if (event.key.toLowerCase() === "r") { event.preventDefault(); loadOrders(); }
                    if (event.key.toLowerCase() === "f") { event.preventDefault(); document.getElementById("fullscreen-button").click(); }
                });

                document.getElementById("category-filters").addEventListener("click", event => {
                    const button = event.target.closest(".station-btn");
                    if (!button) return;
                    activeGroup = button.dataset.group;
                    document.querySelectorAll(".station-btn").forEach(item => {
                        const selected = item === button;
                        item.classList.toggle("active", selected);
                        item.setAttribute("aria-pressed", String(selected));
                    });
                    renderOrders();
                });

                async function readJson(response) {
                    const text = await response.text();
                    let data;
                    try { data = text ? JSON.parse(text) : {}; }
                    catch (error) {
                        const contentType = response.headers.get("content-type") || "";
                        if (response.redirected || contentType.includes("text/html")) {
                            throw new Error("Phiên đăng nhập hoặc bản triển khai đã hết hạn. Hãy tải lại trang và đăng nhập lại.");
                        }
                        throw new Error("Máy chủ không trả về dữ liệu đơn hàng hợp lệ");
                    }
                    if (!response.ok) throw new Error(data.message || "Yêu cầu thất bại (HTTP " + response.status + ")");
                    return data;
                }

                async function loadOrders() {
                    if (isUpdating) return;
                    clearTimeout(refreshTimer);
                    if (loadController) loadController.abort();
                    const controller = new AbortController();
                    loadController = controller;
                    try {
                        const response = await fetch(contextPath + "/barista/orders", {
                            headers: { "Accept": "application/json" }, credentials: "same-origin", cache: "no-store", signal: controller.signal
                        });
                        const data = await readJson(response);
                        if (!Array.isArray(data.orders)) throw new Error("Phản hồi thiếu danh sách đơn hàng");
                        currentOrders = sortOrders(data.orders);
                        renderFilters(data.categories || []);
                        updateStatistics(data.statistics || {});
                        renderOrders();
                        detectNewOrders();
                        hideError();
                    } catch (error) {
                        if (error.name !== "AbortError") showError(error.message);
                    } finally {
                        if (loadController === controller) {
                            loadController = null;
                            refreshTimer = setTimeout(loadOrders, refreshIntervalMs);
                        }
                    }
                }

                function sortOrders(orders) {
                    return orders.slice().sort((a, b) => {
                        const statusRank = { PREPARING: 0, CONFIRMED: 1, READY: 2 };
                        const rank = (statusRank[a.status] ?? 9) - (statusRank[b.status] ?? 9);
                        if (rank) return rank;
                        const timeA = new Date(a.createdAt).getTime();
                        const timeB = new Date(b.createdAt).getTime();
                        if (Number.isFinite(timeA) && Number.isFinite(timeB)) return a.status === "READY" ? timeB - timeA : timeA - timeB;
                        return a.id - b.id;
                    });
                }

                function renderFilters(categories) {
                    if (activeGroup !== "all" && !categories.some(category => String(category.id) === activeGroup)) activeGroup = "all";
                    const container = document.getElementById("category-filters");
                    container.replaceChildren();
                    [{ id: "all", name: "Tất cả trạm pha chế" }, ...categories].forEach(category => {
                        const count = category.id === "all"
                            ? currentOrders.length
                            : currentOrders.filter(order => order.items.some(item => String(item.categoryId) === String(category.id))).length;
                        const button = node("button", "station-btn", category.name + " (" + count + ")");
                        button.type = "button";
                        button.dataset.group = String(category.id);
                        const selected = button.dataset.group === activeGroup;
                        button.classList.toggle("active", selected);
                        button.setAttribute("aria-pressed", String(selected));
                        container.appendChild(button);
                    });
                }

                function updateStatistics(statistics) {
                    document.getElementById("waiting-count").textContent = statistics.waiting ?? 0;
                    document.getElementById("preparing-count").textContent = statistics.preparing ?? 0;
                    document.getElementById("ready-count").textContent = statistics.ready ?? 0;
                }

                function renderOrders() {
                    const orders = activeGroup === "all" ? currentOrders : currentOrders.filter(order =>
                        order.items.some(item => String(item.categoryId) === activeGroup));
                    board.replaceChildren();
                    if (!orders.length) {
                        const empty = node("div", "empty-board");
                        empty.append(node("span", "material-symbols-outlined d-block mb-2 fs-1", "coffee"), node("strong", "d-block mb-1", "Chưa có đơn tại trạm này"), node("span", "small", "Đơn mới sẽ tự động xuất hiện sau khi thanh toán thành công."));
                        board.appendChild(empty);
                        return;
                    }
                    const firstActionable = currentOrders.find(order => order.status === "PREPARING")
                        || currentOrders.find(order => order.status === "CONFIRMED");
                    orders.forEach(order => board.appendChild(createTicket(order, firstActionable && order.id === firstActionable.id)));
                }

                function createTicket(order, canAct) {
                    const ageMinutes = getAgeMinutes(order.createdAt);
                    const overdue = order.status !== "READY" && ageMinutes >= 5;
                    const tone = order.status === "CONFIRMED" ? "waiting" : order.status === "PREPARING" ? "preparing" : "ready";
                    const ticket = node("article", "ticket" + (overdue ? " is-overdue" : "") + (order.status === "READY" ? " is-ready" : ""));
                    ticket.appendChild(node("div", "ticket-ribbon " + tone));

                    const header = node("div", "ticket-header");
                    const top = node("div", "d-flex align-items-start justify-content-between gap-2");
                    const identity = node("div", "min-w-0");
                    const identityRow = node("div", "d-flex align-items-center flex-wrap gap-2");
                    identityRow.append(node("span", "ticket-code", "#" + (order.orderCode || order.id)), node("span", "status-badge " + tone, overdue ? "KHẨN CẤP" : getStatusLabel(order.status)));
                    identity.append(identityRow, node("div", "ticket-target", getOrderTarget(order)));
                    const timer = node("div", "text-end flex-shrink-0");
                    timer.append(node("span", "ticket-time d-block", formatDuration(ageMinutes)), node("small", "text-secondary", overdue ? "SLA: 05:00" : "Mục tiêu: 05:00"));
                    top.append(identity, timer);
                    header.appendChild(top);
                    if (order.recipientName && order.orderType !== "DINE_IN") header.appendChild(node("div", "small text-secondary mt-2", "Khách: " + order.recipientName));

                    const body = node("div", "ticket-body d-flex flex-column gap-2");
                    (order.items || []).forEach(item => body.appendChild(createItem(item)));
                    if (order.orderNote && String(order.orderNote).trim()) {
                        const note = node("div", "ticket-note d-flex align-items-start gap-2");
                        note.append(node("span", "material-symbols-outlined flex-shrink-0", "warning"), node("span", "", order.orderNote));
                        body.appendChild(note);
                    }

                    const footer = node("div", "ticket-footer");
                    if (order.status === "READY") {
                        const done = node("div", "d-flex align-items-center justify-content-center gap-2 text-success fw-bold py-2");
                        done.append(node("span", "material-symbols-outlined", "task_alt"), node("span", "", "ĐƠN ĐÃ SẴN SÀNG"));
                        footer.appendChild(done);
                    } else {
                        const action = node("button", "btn btn-aura ticket-action w-100 d-flex align-items-center justify-content-center gap-2");
                        action.type = "button";
                        action.disabled = !canAct || isUpdating;
                        action.title = canAct ? "Cập nhật trạng thái đơn" : "Cần xử lý đơn đến trước theo thứ tự";
                        action.append(node("span", "material-symbols-outlined", order.status === "CONFIRMED" ? "local_cafe" : "check"), node("span", "", order.status === "CONFIRMED" ? "BẮT ĐẦU PHA" : "HOÀN TẤT ĐƠN"));
                        action.addEventListener("click", () => updateOrderStatus(order.id, order.status === "CONFIRMED" ? "PREPARING" : "READY"));
                        footer.appendChild(action);
                        if (!canAct) footer.appendChild(node("div", "text-center text-secondary mt-2", "Đang chờ theo thứ tự FIFO"));
                    }
                    ticket.append(header, body, footer);
                    return ticket;
                }

                function createItem(item) {
                    const row = node("div", "ticket-item d-flex align-items-start gap-2");
                    row.appendChild(node("span", "item-qty", (item.quantity ?? 0) + "×"));
                    const detail = node("div", "flex-grow-1 min-width-0");
                    detail.appendChild(node("div", "item-name", item.productName || "Sản phẩm"));
                    const customizations = parseCustomization(item.customization);
                    if (customizations.length) {
                        const tags = node("div", "d-flex flex-wrap gap-1 mt-2");
                        customizations.forEach(value => tags.appendChild(node("span", "item-tag", value)));
                        detail.appendChild(tags);
                    }
                    row.appendChild(detail);
                    return row;
                }

                async function updateOrderStatus(orderId, status) {
                    if (isUpdating) return;
                    isUpdating = true;
                    clearTimeout(refreshTimer);
                    renderOrders();
                    try {
                        const body = new URLSearchParams({ action: "update-order-status", orderId: String(orderId), status, csrfToken: document.body.dataset.csrfToken });
                        const response = await fetch(contextPath + "/barista", {
                            method: "POST", credentials: "same-origin",
                            headers: { "Content-Type": "application/x-www-form-urlencoded", "Accept": "application/json" }, body
                        });
                        await readJson(response);
                        showToast(status === "PREPARING" ? "Đã bắt đầu pha chế đơn" : "Đơn đã hoàn tất và sẵn sàng bàn giao");
                    } catch (error) {
                        showError(error.message);
                    } finally {
                        isUpdating = false;
                        await loadOrders();
                    }
                }

                function detectNewOrders() {
                    let hasNew = false;
                    currentOrders.forEach(order => {
                        const id = String(order.id);
                        if (notificationInitialized && order.status === "CONFIRMED" && !seenOrderIds.has(id)) hasNew = true;
                        seenOrderIds.add(id);
                    });
                    if (hasNew) { playNotificationSound(); showToast("Có đơn mới vừa vào hàng chờ pha chế"); }
                    notificationInitialized = true;
                }

                function parseCustomization(customization) {
                    if (!customization) return [];
                    let value = customization;
                    if (typeof value === "string") {
                        try { value = JSON.parse(value); } catch (error) { return [customization]; }
                    }
                    if (Array.isArray(value)) return value.map(String);
                    if (value && typeof value === "object") {
                        const labels = { size: "Size", sugar: "Đường", ice: "Đá", toppings: "Topping" };
                        return Object.entries(value).flatMap(([key, entry]) => {
                            if (entry === null || entry === undefined || entry === "") return [];
                            const prefix = labels[key] || key;
                            return Array.isArray(entry) ? entry.map(item => prefix + ": " + (item.name || item)) : [prefix + ": " + entry];
                        });
                    }
                    return [String(value)];
                }

                function getOrderTarget(order) {
                    if (order.orderType === "DINE_IN") return order.tableNumber ? "Bàn " + order.tableNumber + " (Tại quán)" : "Dùng tại quán";
                    if (order.orderType === "ONLINE") return "Đơn giao hàng (Online)";
                    return "Mang đi (Takeaway)";
                }

                function getStatusLabel(status) {
                    return status === "CONFIRMED" ? "MỚI NHẬN" : status === "PREPARING" ? "ĐANG PHA" : "ĐÃ XONG";
                }

                function getAgeMinutes(createdAt) {
                    const time = new Date(createdAt).getTime();
                    return Number.isFinite(time) ? Math.max(0, Math.floor((Date.now() - time) / 60000)) : 0;
                }

                function formatDuration(totalMinutes) {
                    const hours = Math.floor(totalMinutes / 60);
                    const minutes = totalMinutes % 60;
                    return (hours ? String(hours).padStart(2, "0") + ":" : "") + String(minutes).padStart(2, "0") + ":00";
                }

                updateSoundButton();
                const initialError = document.getElementById("barista-initial-error").textContent.trim();
                try {
                    const initialData = JSON.parse(document.getElementById("barista-initial-data").textContent || "{}");
                    if (Array.isArray(initialData.orders)) {
                        currentOrders = sortOrders(initialData.orders);
                        renderFilters(initialData.categories || []);
                        updateStatistics(initialData.statistics || {});
                        renderOrders();
                        detectNewOrders();
                    }
                } catch (error) {
                    showError("Không đọc được dữ liệu đơn hàng ban đầu.");
                }
                if (initialError) showError(initialError);
                refreshTimer = setTimeout(loadOrders, refreshIntervalMs);
            </script>
        </body>

        </html>