<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8"
         language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${empty requestScope.baristaDashboard}">
    <c:redirect url="/barista"/>
</c:if>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>Aura Café - Barista</title>

    <link
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
        rel="stylesheet">

    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">

    <style>
        :root {
            --aura-red: #900b1a;
            --aura-red-dark: #710916;
            --aura-border: #e7e5e4;
            --aura-muted: #78716c;
        }

        body {
            margin: 0;
            background: #f5f5f4;
            color: #292524;
            font-family: 'Inter', sans-serif;
            font-size: 14px;
        }

        .btn-aura {
            --bs-btn-color: #fff;
            --bs-btn-bg: var(--aura-red);
            --bs-btn-border-color: var(--aura-red);
            --bs-btn-hover-color: #fff;
            --bs-btn-hover-bg: var(--aura-red-dark);
            --bs-btn-hover-border-color: var(--aura-red-dark);
        }

        /* THANH THỐNG KÊ */

        .toolbar {
            background: #fff;
            border-bottom: 1px solid var(--aura-border);
        }

        .stat-box {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 11px 16px;
            border: 1px solid var(--aura-border);
            border-radius: 16px;
            background: #fafaf9;
        }

        .stat-waiting {
            background: #fff1f2;
            border-color: #fecdd3;
            color: #9f1239;
        }

        .stat-preparing {
            background: #fffbeb;
            border-color: #fde68a;
            color: #92400e;
        }

        .stat-label {
            font-size: 11px;
            font-weight: 800;
            text-transform: uppercase;
        }

        .stat-value {
            font-size: 20px;
            font-weight: 800;
        }

        /* Chấm đỏ tĩnh, không dùng animation */
        .waiting-dot {
            width: 11px;
            height: 11px;
            flex-shrink: 0;
            border-radius: 50%;
            background: #e11d48;
        }

        .sound-button {
            padding: 12px 16px;
            border-radius: 14px;
            font-size: 13px;
            font-weight: 700;
        }

        .filter-button {
            padding: 9px 14px;
            border-radius: 10px;
            background: #f5f5f4;
            color: #57534e;
            border: 1px solid transparent;
            font-size: 13px;
            font-weight: 700;
        }

        .filter-button:hover {
            border-color: #d6d3d1;
        }

        .filter-button.active {
            background: var(--aura-red);
            color: #fff;
        }

        /* DANH SÁCH ĐƠN */

        .order-column {
            min-height: 350px;
            padding: 12px;
            border-radius: 14px;
            background: #eeecea;
            border-top: 4px solid;
        }

        .order-list {
            max-height: 65vh;
            overflow-y: auto;
            overflow-x: hidden;
            scroll-behavior: smooth;
            scrollbar-gutter: stable;
            padding-right: 4px;
        }

        .column-waiting {
            border-top-color: var(--aura-red);
        }

        .column-preparing {
            border-top-color: #d97706;
        }

        .column-ready {
            border-top-color: #059669;
        }

        .order-card {
            overflow: hidden;
            border: 1px solid var(--aura-border);
            border-radius: 14px;
            background: #fff;
            box-shadow: 0 2px 6px rgb(0 0 0 / 3%);
        }

        .order-code {
            font-size: 16px;
            font-weight: 800;
        }

        .order-time,
        .order-meta {
            color: var(--aura-muted);
            font-size: 12px;
        }

        .order-note {
            padding: 9px 10px;
            border-left: 3px solid #d97706;
            border-radius: 6px;
            background: #fff8e6;
            color: #7c4a03;
            font-size: 12px;
        }

        .item-row {
            padding: 10px;
            border: 1px solid var(--aura-border);
            border-radius: 10px;
            background: #fafaf9;
        }

        .item-name {
            font-size: 13px;
            font-weight: 700;
        }

        .item-quantity {
            color: var(--aura-red);
            font-weight: 800;
        }

        .item-tag {
            padding: 3px 6px;
            border: 1px solid var(--aura-border);
            border-radius: 5px;
            background: #fff;
            color: #57534e;
            font-size: 10px;
        }

        .item-action {
            min-width: 86px;
            font-size: 10px;
            font-weight: 800;
            white-space: nowrap;
        }

        .empty-column {
            padding: 35px 10px;
            text-align: center;
            color: var(--aura-muted);
            font-size: 13px;
        }

        .min-width-zero {
            min-width: 0;
        }

        .recipe-lookup-link {
            transition: color 0.15s ease;
        }

        .recipe-lookup-link:hover {
            color: var(--aura-red) !important;
            text-decoration: underline !important;
        }

        .recipe-sop-badge {
            transition: all 0.15s ease;
        }

        .recipe-sop-badge:hover {
            background-color: #fee2e2 !important;
            color: var(--aura-red) !important;
            border-color: #fca5a5 !important;
        }

        @media (max-width: 575.98px) {
            .stat-box {
                flex: 1 1 140px;
                padding: 10px;
            }

            .stat-label {
                font-size: 10px;
            }
        }
    </style>
</head>

<body data-context-path="${pageContext.request.contextPath}"
      data-csrf-token="${sessionScope.baristaCsrfToken}">

<!-- Shared Customer header with Barista navigation and a non-clickable brand. -->
<jsp:include page="/components/header.jsp">
    <jsp:param name="navbarMode" value="barista"/>
    <jsp:param name="showCart" value="false"/>
    <jsp:param name="activePage" value="barista-orders"/>
</jsp:include>

<!-- THỐNG KÊ VÀ BỘ LỌC -->
<section class="toolbar px-3 px-xl-4 py-3"
         aria-label="Thống kê và bộ lọc">

    <div class="d-flex flex-wrap align-items-center
                justify-content-between gap-3">

        <div class="d-flex flex-wrap gap-2">

            <div class="stat-box stat-waiting">
                <span class="waiting-dot" aria-hidden="true"></span>

                <div>
                    <div class="stat-label">Đang chờ pha</div>
                    <div>
                        <strong id="waiting-count" class="stat-value">0</strong>
                        <span>đơn</span>
                    </div>
                </div>
            </div>

            <div class="stat-box stat-preparing">
                <svg width="28" height="28" viewBox="0 0 24 24"
                     fill="none" stroke="currentColor"
                     stroke-width="1.8" aria-hidden="true">
                    <path d="M4 5h12v10a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4V5Z"/>
                    <path d="M16 6h2a3 3 0 1 1 0 6h-2M3 22h16"/>
                </svg>

                <div>
                    <div class="stat-label">Đang pha</div>
                    <div>
                        <strong id="preparing-count" class="stat-value">0</strong>
                        <span>đơn</span>
                    </div>
                </div>
            </div>

            <div class="stat-box">
                <svg width="28" height="28" viewBox="0 0 24 24"
                     fill="none" stroke="#059669"
                     stroke-width="2" stroke-linecap="round"
                     stroke-linejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="9"/>
                    <path d="m8 12 3 3 5-6"/>
                </svg>

                <div>
                    <div class="stat-label">Đã xong</div>
                    <div>
                        <strong id="ready-count" class="stat-value">0</strong>
                        <span>đơn</span>
                    </div>
                </div>
            </div>

        </div>

        <button id="sound-button"
                class="btn btn-light border sound-button
                       d-flex align-items-center gap-2"
                type="button"
                aria-pressed="false">

            <svg width="22" height="22" viewBox="0 0 24 24"
                 fill="none" stroke="#900b1a"
                 stroke-width="1.8" stroke-linecap="round"
                 stroke-linejoin="round" aria-hidden="true">
                <path d="M11 5 6 9H3v6h3l5 4V5Z"/>
                <path d="M15 8a6 6 0 0 1 0 8"/>
                <path d="M18 5a10 10 0 0 1 0 14"/>
            </svg>

            <span id="sound-label">Âm chuông: Tắt</span>
        </button>
    </div>

    <div class="border-top mt-3 pt-3">
        <div id="category-filters" class="d-flex flex-wrap gap-2" aria-label="Lọc nhóm sản phẩm">
            <button class="btn filter-button active"
                    data-group="all" aria-pressed="true" type="button">
                Tất cả sản phẩm
            </button>
        </div>
    </div>
</section>

<main class="container-fluid px-3 px-xl-4 py-4">

    <div id="error-message"
         class="alert alert-danger"
         role="alert"
         hidden>
    </div>

    <div id="sound-message"
         class="alert alert-warning"
         role="status"
         hidden>
    </div>

    <section class="row g-3" aria-label="Danh sách đơn pha chế">

        <div class="col-12 col-lg-4">
            <div class="order-column column-waiting">
                <div class="d-flex align-items-center
                            justify-content-between mb-3 px-1">
                    <h2 class="h6 fw-bold mb-0">Đang chờ pha</h2>
                    <span id="waiting-column-count"
                          class="badge rounded-pill"
                          style="background: #900b1a">0</span>
                </div>

                <div id="waiting-orders" class="order-list" tabindex="0" role="region" aria-label="Đang chờ pha"></div>
            </div>
        </div>

        <div class="col-12 col-lg-4">
            <div class="order-column column-preparing">
                <div class="d-flex align-items-center
                            justify-content-between mb-3 px-1">
                    <h2 class="h6 fw-bold mb-0">Đang pha</h2>
                    <span id="preparing-column-count"
                          class="badge text-bg-warning rounded-pill">0</span>
                </div>

                <div id="preparing-orders" class="order-list" tabindex="0" role="region" aria-label="Đang pha"></div>
            </div>
        </div>

        <div class="col-12 col-lg-4">
            <div class="order-column column-ready">
                <div class="d-flex align-items-center
                            justify-content-between mb-3 px-1">
                    <h2 class="h6 fw-bold mb-0">Đã xong</h2>
                    <span id="ready-column-count"
                          class="badge text-bg-success rounded-pill">0</span>
                </div>

                <div id="ready-orders" class="order-list" tabindex="0" role="region" aria-label="Đã xong"></div>
            </div>
        </div>

    </section>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    "use strict";

    const contextPath = document.body.dataset.contextPath;
    const refreshIntervalMs = 5000;

    let activeGroup = "all";
    let currentOrders = [];
    let isUpdating = false;
    let loadController = null;
    let refreshTimer = null;

    // Chuông luôn theo dõi hàng đợi đầy đủ, không theo bộ lọc.
    let notificationInitialized = false;
    const seenOrderIds = new Set();

    let soundEnabled = false;
    let audioContext = null;

    const containers = {
        CONFIRMED: document.getElementById("waiting-orders"),
        PREPARING: document.getElementById("preparing-orders"),
        READY: document.getElementById("ready-orders")
    };

    const errorMessage = document.getElementById("error-message");
    const soundMessage = document.getElementById("sound-message");

    // =====================================================
    // NAVBAR
    // =====================================================

    document.addEventListener("keydown", event => {
        if (event.altKey || event.ctrlKey || event.metaKey) {
            return;
        }

        const linkId = event.key === "F1"
            ? "orders-link"
            : event.key === "F2"
                ? "recipes-link"
                : null;

        if (linkId) {
            event.preventDefault();
            window.location.assign(
                document.getElementById(linkId).href
            );
        }
    });

    // =====================================================
    // ÂM CHUÔNG
    // =====================================================

    function updateSoundButton() {
        document.getElementById("sound-label").textContent =
            "Âm chuông: " + (soundEnabled ? "Bật" : "Tắt");

        document.getElementById("sound-button").setAttribute(
            "aria-pressed",
            String(soundEnabled)
        );
    }

    function playNotificationSound() {
        if (!soundEnabled || !audioContext) {
            return;
        }

        if (audioContext.state !== "running") {
            soundEnabled = false;
            updateSoundButton();

            soundMessage.textContent =
                "Âm thanh đang bị trình duyệt tạm dừng. "
                + "Bấm bật chuông để kích hoạt lại.";

            soundMessage.hidden = false;
            return;
        }

        const start = audioContext.currentTime;

        // Hai nốt ngắn, âm lượng nhẹ.
        [660, 880].forEach((frequency, index) => {
            const oscillator = audioContext.createOscillator();
            const gain = audioContext.createGain();

            const time = start + index * 0.18;

            oscillator.type = "sine";
            oscillator.frequency.value = frequency;

            gain.gain.setValueAtTime(0, time);
            gain.gain.linearRampToValueAtTime(0.12, time + 0.02);
            gain.gain.exponentialRampToValueAtTime(0.001, time + 0.16);

            oscillator.connect(gain);
            gain.connect(audioContext.destination);

            oscillator.start(time);
            oscillator.stop(time + 0.18);

            oscillator.onended = () => {
                oscillator.disconnect();
                gain.disconnect();
            };
        });
    }

    document.getElementById("sound-button").addEventListener(
        "click",
        async () => {
            soundMessage.hidden = true;

            if (soundEnabled) {
                soundEnabled = false;
                updateSoundButton();
                return;
            }

            try {
                const AudioContextClass =
                    window.AudioContext || window.webkitAudioContext;

                if (!AudioContextClass) {
                    throw new Error("Trình duyệt không hỗ trợ âm thanh.");
                }

                if (!audioContext) {
                    audioContext = new AudioContextClass();
                }

                await audioContext.resume();

                if (audioContext.state !== "running") {
                    throw new Error("Không thể kích hoạt âm thanh.");
                }

                soundEnabled = true;
                updateSoundButton();

                // Tiếng thử khi nhân viên chủ động bật chuông.
                playNotificationSound();

            } catch (error) {
                soundEnabled = false;
                updateSoundButton();

                soundMessage.textContent =
                    "Không bật được chuông. " + error.message;

                soundMessage.hidden = false;
            }
        }
    );

    function detectNewOrders(allOrders) {
        let hasNewOrder = false;

        allOrders.forEach(order => {
            if (order.id === null || order.id === undefined) {
                return;
            }

            const id = String(order.id);

            const inQueue =
                order.status === "CONFIRMED";

            if (notificationInitialized
                    && inQueue
                    && !seenOrderIds.has(id)) {
                hasNewOrder = true;
            }

            // Vẫn ghi nhận khi tắt chuông.
            // Bật lại sẽ không phát lại các đơn đã nhìn thấy.
            seenOrderIds.add(id);
        });

        if (notificationInitialized && hasNewOrder) {
            playNotificationSound();
        }

        notificationInitialized = true;
    }

    // =====================================================
    // API
    // =====================================================

    async function readJsonResponse(response, allowEmpty) {
        if (response.redirected) {
            throw new Error(
                "Yêu cầu bị chuyển hướng. Vui lòng kiểm tra đăng nhập."
            );
        }

        const text = await response.text();

        if (!text.trim()) {
            if (response.ok && allowEmpty) {
                return {};
            }

            throw new Error(
                "Máy chủ trả về nội dung rỗng (HTTP "
                + response.status + ")."
            );
        }

        let data;

        try {
            data = JSON.parse(text);
        } catch (error) {
            throw new Error(
                "Máy chủ không trả về JSON hợp lệ. "
                + "Kiểm tra BaristaController hoặc phiên đăng nhập."
            );
        }

        if (!response.ok) {
            throw new Error(
                data && data.message
                    ? data.message
                    : "Yêu cầu thất bại: HTTP " + response.status
            );
        }

        if (!data || typeof data !== "object" || Array.isArray(data)) {
            throw new Error("Cấu trúc JSON không hợp lệ.");
        }

        return data;
    }

    async function fetchOrders(signal) {
        const response = await fetch(
            contextPath
                + "/barista?action=orders",
            {
                method: "GET",
                headers: { "Accept": "application/json" },
                credentials: "same-origin",
                cache: "no-store",
                signal: signal
            }
        );

        const data = await readJsonResponse(response, false);

        if (!Array.isArray(data.orders)) {
            throw new Error("Phản hồi thiếu mảng orders.");
        }

        return data;
    }

    function scheduleRefresh() {
        clearTimeout(refreshTimer);

        refreshTimer = setTimeout(() => {
            loadOrders();
        }, refreshIntervalMs);
    }

    async function loadOrders() {
        if (isUpdating) {
            return;
        }

        clearTimeout(refreshTimer);

        if (loadController) {
            loadController.abort();
        }

        const controller = new AbortController();
        loadController = controller;

        try {
            const data = await fetchOrders(controller.signal);
            if (loadController !== controller) {
                return;
            }
            currentOrders = sortDashboardOrders(data.orders);
            renderCategoryFilters(data.categories || []);
            updateStatistics(data.statistics || {});
            renderFilteredOrders();
            detectNewOrders(currentOrders);

            hideError();

        } catch (error) {
            if (error.name !== "AbortError"
                    && loadController === controller) {
                showError(error.message);
            }

        } finally {
            if (loadController === controller) {
                loadController = null;
                scheduleRefresh();
            }
        }
    }

    // =====================================================
    // BỘ LỌC VÀ SẮP XẾP
    // =====================================================

    document.getElementById("category-filters").addEventListener("click", event => {
        const button = event.target.closest(".filter-button");
        if (!button) return;
        activeGroup = button.dataset.group;
        document.querySelectorAll(".filter-button").forEach(item => {
            const selected = item === button;
            item.classList.toggle("active", selected);
            item.setAttribute("aria-pressed", String(selected));
        });
        renderFilteredOrders();
    });

    function renderCategoryFilters(categories) {
        if (activeGroup !== "all"
                && !categories.some(category => String(category.id) === activeGroup)) {
            activeGroup = "all";
        }
        const container = document.getElementById("category-filters");
        container.replaceChildren();
        [{id: "all", name: "Tất cả sản phẩm"}, ...categories].forEach(category => {
            const button = element("button", "btn filter-button", category.name);
            button.type = "button";
            button.dataset.group = String(category.id);
            const selected = button.dataset.group === activeGroup;
            button.classList.toggle("active", selected);
            button.setAttribute("aria-pressed", String(selected));
            container.appendChild(button);
        });
    }

    function renderFilteredOrders() {
        // Match orders by category but show ALL their items: the action completes the whole order.
        const displayOrders = activeGroup === "all" ? currentOrders
            : currentOrders.filter(order => order.items.some(item =>
                String(item.categoryId) === activeGroup));
        renderOrders(displayOrders, currentOrders);
    }

    function sortDashboardOrders(orders) {
        const working = sortOldestFirst(orders.filter(order =>
            order.status === "CONFIRMED" || order.status === "PREPARING"));
        const ready = orders.filter(order => order.status === "READY");
        if (ready.every(order => order.createdAt
                && Number.isFinite(new Date(order.createdAt).getTime()))) {
            ready.sort((a, b) => new Date(b.createdAt).getTime()
                - new Date(a.createdAt).getTime() || b.id - a.id);
        }
        return [...working, ...ready.slice(0, 3)];
    }

    function sortOldestFirst(orders) {
        const result = orders.slice();

        // Nếu thiếu thời gian hợp lệ, giữ thứ tự backend cung cấp.
        const validDates = result.every(order =>
            order.createdAt
            && Number.isFinite(new Date(order.createdAt).getTime())
        );

        if (validDates) {
            result.sort((a, b) =>
                new Date(a.createdAt).getTime()
                - new Date(b.createdAt).getTime() || a.id - b.id
            );
        }

        return result;
    }

    // =====================================================
    // HIỂN THỊ
    // =====================================================

    function element(tag, className, text) {
        const node = document.createElement(tag);
        node.className = className;

        if (text !== undefined && text !== null) {
            node.textContent = text;
        }

        return node;
    }

    function updateStatistics(statistics) {
        document.getElementById("waiting-count").textContent =
            statistics.waiting ?? 0;

        document.getElementById("preparing-count").textContent =
            statistics.preparing ?? 0;

        document.getElementById("ready-count").textContent =
            statistics.ready ?? 0;
    }

    function renderOrders(displayOrders, allOrders) {
        const scrollPositions = Object.fromEntries(Object.entries(containers)
            .map(([status, container]) => [status, container.scrollTop]));
        Object.values(containers).forEach(container => {
            container.replaceChildren();
        });

        // Giữ quy tắc FIFO của JSP cũ, nhưng xét toàn bộ hàng đợi.
        // Backend vẫn phải kiểm tra quyền cập nhật và FIFO.
        const firstActionableOrder = allOrders.find(order =>
            order.status === "CONFIRMED"
            || order.status === "PREPARING"
        );

        const firstId = firstActionableOrder
            ? String(firstActionableOrder.id)
            : null;

        const counts = {
            CONFIRMED: 0,
            PREPARING: 0,
            READY: 0
        };

        displayOrders.forEach(order => {
            const container = containers[order.status];

            if (!container) {
                return;
            }

            const canAct = String(order.id) === firstId;

            container.appendChild(createOrderCard(order, canAct));
            counts[order.status]++;
        });

        const settings = [
            ["CONFIRMED", "waiting-column-count", "Không có đơn đang chờ."],
            ["PREPARING", "preparing-column-count", "Không có đơn đang pha."],
            ["READY", "ready-column-count", "Chưa có đơn đã xong."]
        ];

        settings.forEach(([status, countId, message]) => {
            document.getElementById(countId).textContent = counts[status];

            if (!counts[status]) {
                containers[status].appendChild(
                    element("div", "empty-column", message)
                );
            }
        });
        Object.entries(containers).forEach(([status, container]) => {
            container.scrollTop = scrollPositions[status];
        });
    }

    function createReadyCard(order) {
        const card = element("article", "card order-card mb-3");
        const header = element("div", "card-header bg-white p-3");
        header.append(
            element("strong", "order-code", "#" + (order.orderCode || order.id)),
            element("div", "order-time mt-1", "Giờ đặt: " + formatCreatedAt(order.createdAt))
        );
        const body = element("div", "card-body p-3 d-flex flex-column gap-2");
        (Array.isArray(order.items) ? order.items : []).forEach(item => {
            const row = element("div", "item-name d-flex align-items-center justify-content-between");
            const left = element("span", "");
            left.append(
                element("span", "item-quantity me-1", (item.quantity ?? 0) + "×")
            );
            const link = element(
                "a",
                "text-decoration-none text-dark recipe-lookup-link",
                item.productName || "Sản phẩm"
            );
            if (item.productId) {
                link.href = contextPath + "/barista?action=recipes&productId=" + item.productId;
                link.title = "Bấm để xem công thức & định lượng (SOP)";
            }
            left.appendChild(link);
            row.appendChild(left);
            body.appendChild(row);
        });
        card.append(header, body);
        return card;
    }

    function createOrderCard(order, canAct) {
        if (order.status === "READY") {
            return createReadyCard(order);
        }
        const card = element("article", "card order-card mb-3");
        const header = element("div", "card-header bg-white p-3");

        const titleRow = element(
            "div",
            "d-flex flex-wrap justify-content-between align-items-center gap-2"
        );

        titleRow.append(
            element(
                "strong",
                "order-code",
                "#" + (order.orderCode || order.id)
            ),
            element(
                "span",
                "order-time",
                "Giờ đặt: " + formatCreatedAt(order.createdAt)
            )
        );

        header.append(
            titleRow,
            element(
                "span",
                "badge text-bg-light border mt-2",
                getOrderTypeLabel(order)
            ),
            element(
                "div",
                "order-meta mt-2",
                getOrderMeta(order)
            )
        );

        if (order.orderNote && String(order.orderNote).trim()) {
            header.appendChild(
                element(
                    "div",
                    "order-note mt-2",
                    "Ghi chú: " + order.orderNote
                )
            );
        }

        const itemsContainer = element(
            "div",
            "card-body p-3 d-flex flex-column gap-2"
        );

        const items = Array.isArray(order.items) ? order.items : [];

        items.forEach(item => {
            itemsContainer.appendChild(
                createItemRow(item)
            );
        });

        if (!items.length) {
            itemsContainer.appendChild(
                element(
                    "div",
                    "small text-secondary",
                    "Chưa có thông tin món."
                )
            );
        }

        const footer = element("div", "card-footer bg-white p-3");
        footer.appendChild(element("div", "order-meta mb-2",
            "Tổng: " + order.itemCount + " món • Cập nhật áp dụng cho toàn bộ đơn"));
        const action = element("button", "btn btn-aura item-action w-100");
        action.type = "button";
        action.disabled = !canAct || order.status === "READY" || isUpdating;
        action.textContent = order.status === "CONFIRMED" ? "BẮT ĐẦU PHA" : "HOÀN THÀNH";
        action.title = canAct ? "Cập nhật trạng thái toàn bộ đơn"
            : "Xử lý đơn đến trước trước khi cập nhật đơn này";
        action.addEventListener("click", () => updateOrderStatus(order.id,
            order.status === "CONFIRMED" ? "PREPARING" : "READY"));
        footer.appendChild(action);
        card.append(header, itemsContainer, footer);

        return card;
    }

    function createItemRow(item) {
        const row = element(
            "div",
            "item-row d-flex align-items-start justify-content-between gap-2"
        );

        const info = element("div", "flex-grow-1 min-width-zero");
        const name = element("div", "item-name d-flex align-items-center justify-content-between gap-1");

        const nameLeft = element("span", "text-truncate");
        nameLeft.append(
            element(
                "span",
                "item-quantity me-1",
                (item.quantity ?? 0) + "×"
            )
        );

        const nameLink = element(
            "a",
            "text-decoration-none text-dark fw-bold recipe-lookup-link",
            item.productName || "Sản phẩm"
        );
        if (item.productId) {
            nameLink.href = contextPath + "/barista?action=recipes&productId=" + item.productId;
            nameLink.title = "Bấm để xem công thức & định lượng (SOP)";
        }
        nameLeft.appendChild(nameLink);
        name.appendChild(nameLeft);

        if (item.productId) {
            const recipeBadge = element(
                "a",
                "badge bg-light text-secondary border text-decoration-none d-inline-flex align-items-center gap-1 px-1 py-0.5 flex-shrink-0 recipe-sop-badge",
                ""
            );
            recipeBadge.href = contextPath + "/barista?action=recipes&productId=" + item.productId;
            recipeBadge.title = "Tra cứu công thức (SOP)";
            recipeBadge.innerHTML = '<span class="material-symbols-outlined" style="font-size: 13px;">menu_book</span><span style="font-size: 10px; font-weight: 700;">SOP</span>';
            name.appendChild(recipeBadge);
        }

        info.appendChild(name);

        const customizations = parseCustomization(item.customization);

        if (customizations.length) {
            const options = element(
                "div",
                "d-flex flex-wrap gap-1 mt-2"
            );

            customizations.forEach(value => {
                options.appendChild(
                    element("span", "item-tag", value)
                );
            });

            info.appendChild(options);
        }

        row.append(info);

        return row;
    }

    // =====================================================
    // CẬP NHẬT TRẠNG THÁI ĐƠN
    // =====================================================

    async function updateOrderStatus(orderId, status) {
        if (isUpdating) {
            return;
        }

        if (orderId === null || orderId === undefined) {
            showError("Đơn thiếu orderId nên không thể cập nhật.");
            return;
        }

        isUpdating = true;
        clearTimeout(refreshTimer);

        if (loadController) {
            loadController.abort();
            loadController = null;
        }

        const buttons = Array.from(
            document.querySelectorAll(".item-action")
        );

        const previousStates = buttons.map(button => button.disabled);

        buttons.forEach(button => {
            button.disabled = true;
        });

        hideError();

        let succeeded = false;
        let updateError = null;

        try {
            const body = new URLSearchParams();
            body.append("action", "update-order-status");
            body.append("orderId", String(orderId));
            body.append("status", status);
            body.append("csrfToken", document.body.dataset.csrfToken);

            const response = await fetch(contextPath + "/barista", {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                    "Accept": "application/json"
                },
                body: body
            });

            await readJsonResponse(response, response.status === 204);
            succeeded = true;

        } catch (error) {
            updateError = error.message;
            showError(updateError);

        } finally {
            isUpdating = false;

            if (!succeeded) {
                buttons.forEach((button, index) => {
                    button.disabled = previousStates[index];
                });

                scheduleRefresh();
            }
        }

        // Reload after conflicts/errors too, since another terminal may have changed the queue.
        await loadOrders();
        if (updateError) showError(updateError);
    }

    // =====================================================
    // HÀM HỖ TRỢ
    // =====================================================

    function parseCustomization(customization) {
        if (!customization) {
            return [];
        }

        let parsed = customization;

        if (typeof parsed === "string") {
            try {
                parsed = JSON.parse(parsed);
            } catch (error) {
                return [customization];
            }
        }

        if (parsed === null) {
            return [];
        }

        if (Array.isArray(parsed)) {
            return parsed.map(value => String(value));
        }

        if (typeof parsed === "object") {
            const values = [];

            Object.entries(parsed).forEach(([key, value]) => {
                if (value === null || value === undefined) {
                    return;
                }

                if (Array.isArray(value)) {
                    value.forEach(entry => {
                        values.push(typeof entry === "object" && entry !== null
                            ? (entry.name || JSON.stringify(entry)) : String(entry));
                    });
                } else {
                    values.push(key + ": " + String(value));
                }
            });

            return values;
        }

        return [String(parsed)];
    }

    function getOrderTypeLabel(order) {
        if (order.orderType === "DINE_IN") {
            return "Dùng tại quán";
        }

        if (order.orderType === "ONLINE") {
            return "Online";
        }

        return "Mua mang đi";
    }

    function getOrderMeta(order) {
        if (order.orderType === "DINE_IN") {
            return order.tableNumber
                ? "Bàn " + order.tableNumber
                : "Khách dùng tại quán";
        }

        if (order.orderType === "ONLINE") {
            return order.recipientName
                ? "Khách: " + order.recipientName
                : "Nhân viên quán giao";
        }

        return "Khách mua mang đi";
    }

    function formatCreatedAt(createdAt) {
        if (!createdAt) {
            return "";
        }

        const date = new Date(createdAt);

        if (Number.isNaN(date.getTime())) {
            return String(createdAt);
        }

        return date.toLocaleTimeString("vi-VN", {
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.hidden = false;
    }

    function hideError() {
        errorMessage.textContent = "";
        errorMessage.hidden = true;
    }

    loadOrders();
</script>

</body>
</html>
