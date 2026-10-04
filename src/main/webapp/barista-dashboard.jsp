<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8"
         language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>Aura Café - Barista</title>

    <link
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
        rel="stylesheet">

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
            font-family: Arial, sans-serif;
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

        /* NAVBAR */

        .barista-navbar {
            background: #fff;
            border-bottom: 1px solid var(--aura-border);
        }

        .navbar-layout {
            display: grid;
            grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
            align-items: center;
            gap: 20px;
            min-height: 86px;
            padding: 12px 0;
        }

        .brand-name {
            color: var(--aura-red);
            font-size: 24px;
            font-weight: 800;
            white-space: nowrap;
        }

        .terminal-badge {
            background: var(--aura-red);
            color: #fff;
            font-size: 10px;
            padding: 8px 10px;
            white-space: nowrap;
        }

        .barista-navigation {
            justify-self: center;
            flex-wrap: nowrap;
            padding: 5px;
            gap: 5px;
            background: #f5f5f4;
            border-radius: 16px;
        }

        .barista-navigation .nav-link {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 9px;
            padding: 13px 16px;
            border-radius: 11px;
            color: #625e58;
            font-size: 14px;
            font-weight: 700;
            white-space: nowrap;
        }

        .barista-navigation .nav-link.active {
            background: var(--aura-red);
            color: #fff;
        }

        .barista-navigation .nav-link:hover:not(.active) {
            background: #eae7e5;
        }

        .employee {
            justify-self: end;
            min-width: 0;
        }

        .employee-name {
            max-width: 210px;
            overflow-wrap: anywhere;
            font-size: 14px;
            font-weight: 700;
        }

        .employee-role {
            color: var(--aura-muted);
            font-size: 12px;
            margin-top: 3px;
        }

        .employee-avatar {
            width: 44px;
            height: 44px;
            display: grid;
            place-items: center;
            flex-shrink: 0;
            border-radius: 50%;
            background: var(--aura-red);
            color: #fff;
            font-weight: 800;
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

        @media (max-width: 1199.98px) {
            .navbar-layout {
                grid-template-columns: minmax(0, 1fr) auto;
                gap: 12px;
            }

            .barista-navigation {
                grid-column: 1 / -1;
                grid-row: 2;
            }

            .employee {
                grid-column: 2;
                grid-row: 1;
            }
        }

        @media (max-width: 575.98px) {
            .terminal-badge {
                display: none;
            }

            .brand-name {
                font-size: 21px;
            }

            .employee-name {
                max-width: 135px;
                font-size: 12px;
            }

            .employee-avatar {
                width: 36px;
                height: 36px;
            }

            .barista-navigation {
                width: 100%;
            }

            .barista-navigation .nav-link {
                flex: 1;
                padding: 11px 7px;
                font-size: 11px;
                white-space: normal;
                text-align: center;
            }

            .shortcut {
                display: none;
            }

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

<body data-context-path="${pageContext.request.contextPath}">

<!-- NAVBAR -->
<header class="barista-navbar">
    <div class="container-fluid px-3 px-xl-4">
        <div class="navbar-layout">

            <div class="d-flex align-items-center gap-3">
                <span class="brand-name">Aura Café</span>

                <span class="badge terminal-badge">
                    BARISTA KDS TERMINAL
                </span>
            </div>

            <nav class="nav nav-pills barista-navigation"
                 aria-label="Điều hướng Barista">

                <a id="orders-link"
                   class="nav-link active"
                   aria-current="page"
                   href="${pageContext.request.contextPath}/barista">

                    <svg width="22" height="22" viewBox="0 0 24 24"
                         fill="none" stroke="currentColor"
                         stroke-width="1.8" stroke-linecap="round"
                         stroke-linejoin="round" aria-hidden="true">
                        <path d="M4 8h12v7a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4V8Z"/>
                        <path d="M16 9h2a3 3 0 1 1 0 6h-2"/>
                        <path d="M7 3v2M11 3v2M3 22h16"/>
                    </svg>

                    <span>
                        Đơn pha chế <span class="shortcut">(F1)</span>
                    </span>
                </a>

                <a id="recipes-link"
                   class="nav-link"
                   href="${pageContext.request.contextPath}/barista?action=recipes">

                    <svg width="23" height="23" viewBox="0 0 24 24"
                         fill="none" stroke="currentColor"
                         stroke-width="1.8" stroke-linecap="round"
                         stroke-linejoin="round" aria-hidden="true">
                        <path d="M12 5v16"/>
                        <path d="M12 5C9 3 5 3 2 4v16c3-1 7-1 10 1"/>
                        <path d="M12 5c3-2 7-2 10-1v16c-3-1-7-1-10 1"/>
                    </svg>

                    <span>
                        Công thức & Định lượng
                        <span class="shortcut">(F2)</span>
                    </span>
                </a>
            </nav>

            <div class="employee d-flex align-items-center gap-3">
                <div class="text-end">
                    <div id="employee-name" class="employee-name">
                        <c:out value="${sessionScope.user.fullName}"
                               default="Nhân viên"/>
                    </div>

                    <div class="employee-role">
                        Nhân viên pha chế
                    </div>
                </div>

                <div id="employee-avatar"
                     class="employee-avatar"
                     aria-hidden="true">
                    NV
                </div>
            </div>

        </div>
    </div>
</header>

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
                    <div class="stat-label">Đang thực hiện</div>
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
                    <div class="stat-label">Hoàn tất trong ca</div>
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
        <div class="d-flex flex-wrap gap-2" aria-label="Lọc nhóm sản phẩm">
            <button class="btn filter-button active"
                    data-group="all" aria-pressed="true" type="button">
                Tất cả sản phẩm
            </button>

            <button class="btn filter-button"
                    data-group="coffee" aria-pressed="false" type="button">
                Cà phê
            </button>

            <button class="btn filter-button"
                    data-group="tea" aria-pressed="false" type="button">
                Trà & Đá xay
            </button>

            <button class="btn filter-button"
                    data-group="bakery" aria-pressed="false" type="button">
                Bánh
            </button>

            <button class="btn filter-button"
                    data-group="bottled" aria-pressed="false" type="button">
                Đóng chai
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

                <div id="waiting-orders"></div>
            </div>
        </div>

        <div class="col-12 col-lg-4">
            <div class="order-column column-preparing">
                <div class="d-flex align-items-center
                            justify-content-between mb-3 px-1">
                    <h2 class="h6 fw-bold mb-0">Đang thực hiện</h2>
                    <span id="preparing-column-count"
                          class="badge text-bg-warning rounded-pill">0</span>
                </div>

                <div id="preparing-orders"></div>
            </div>
        </div>

        <div class="col-12 col-lg-4">
            <div class="order-column column-ready">
                <div class="d-flex align-items-center
                            justify-content-between mb-3 px-1">
                    <h2 class="h6 fw-bold mb-0">Đã hoàn tất</h2>
                    <span id="ready-column-count"
                          class="badge text-bg-success rounded-pill">0</span>
                </div>

                <div id="ready-orders"></div>
            </div>
        </div>

    </section>
</main>

<script>
    "use strict";

    const contextPath = document.body.dataset.contextPath;
    const refreshIntervalMs = 5000;

    let activeGroup = "all";
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

    const employeeName = document.getElementById("employee-name")
        .textContent.trim();

    document.getElementById("employee-avatar").textContent =
        employeeName.split(/\s+/)
            .filter(Boolean)
            .slice(-2)
            .map(part => part.charAt(0))
            .join("")
            .toLocaleUpperCase("vi-VN") || "NV";

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
                order.status === "CONFIRMED"
                || order.status === "PREPARING";

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
                + "Kiểm tra BaristaServlet hoặc phiên đăng nhập."
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

    async function fetchOrders(group, signal) {
        const response = await fetch(
            contextPath
                + "/barista?action=orders&group="
                + encodeURIComponent(group),
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

        const requestedGroup = activeGroup;

        try {
            // Luôn lấy hàng đợi đầy đủ để:
            // 1. Phát hiện đơn mới.
            // 2. Xác định FIFO không bị ảnh hưởng bởi bộ lọc.
            const allRequest = fetchOrders("all", controller.signal);

            const filteredRequest = requestedGroup === "all"
                ? allRequest
                : fetchOrders(requestedGroup, controller.signal);

            const [allData, filteredData] = await Promise.all([
                allRequest,
                filteredRequest
            ]);

            if (loadController !== controller) {
                return;
            }

            const allOrders = sortOldestFirst(allData.orders);
            const displayOrders = sortOldestFirst(filteredData.orders);

            updateStatistics(allData.statistics || {});
            renderOrders(displayOrders, allOrders);
            detectNewOrders(allOrders);

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

    document.querySelectorAll(".filter-button").forEach(button => {
        button.addEventListener("click", () => {
            activeGroup = button.dataset.group;

            document.querySelectorAll(".filter-button").forEach(item => {
                const selected = item === button;

                item.classList.toggle("active", selected);
                item.setAttribute("aria-pressed", String(selected));
            });

            loadOrders();
        });
    });

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
                - new Date(b.createdAt).getTime()
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
            ["PREPARING", "preparing-column-count", "Không có đơn đang thực hiện."],
            ["READY", "ready-column-count", "Chưa có đơn hoàn tất."]
        ];

        settings.forEach(([status, countId, message]) => {
            document.getElementById(countId).textContent = counts[status];

            if (!counts[status]) {
                containers[status].appendChild(
                    element("div", "empty-column", message)
                );
            }
        });
    }

    function createOrderCard(order, canAct) {
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
                formatCreatedAt(order.createdAt)
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
                createItemRow(item, canAct, order.status)
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

        card.append(header, itemsContainer);

        return card;
    }

    function createItemRow(item, canAct, orderStatus) {
        const row = element(
            "div",
            "item-row d-flex align-items-start justify-content-between gap-2"
        );

        const info = element("div", "flex-grow-1 min-width-zero");
        const name = element("div", "item-name");

        name.append(
            element(
                "span",
                "item-quantity me-1",
                (item.quantity ?? 0) + "×"
            ),
            element("span", "", item.productName || "Sản phẩm")
        );

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

        const action = element(
            "button",
            "btn item-action flex-shrink-0"
        );

        action.type = "button";

        const isReady = item.preparationStatus === "READY";

        if (isReady) {
            action.textContent = "XONG ✓";
            action.classList.add("btn-outline-success");
            action.disabled = true;

        } else {
            action.textContent = "ĐANG LÀM";
            action.classList.add("btn-light", "border");

            action.disabled =
                !canAct || orderStatus === "READY" || isUpdating;

            action.title = canAct
                ? "Đánh dấu món đã pha xong"
                : "Xử lý đơn đến trước trước khi cập nhật món này";

            action.addEventListener("click", () => {
                updateItemStatus(item.id);
            });
        }

        row.append(info, action);

        return row;
    }

    // =====================================================
    // CẬP NHẬT TRẠNG THÁI MÓN
    // =====================================================

    async function updateItemStatus(itemId) {
        if (isUpdating) {
            return;
        }

        if (itemId === null || itemId === undefined) {
            showError("Món thiếu itemId nên không thể cập nhật.");
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

        try {
            const body = new URLSearchParams();
            body.append("action", "update-item-status");
            body.append("itemId", String(itemId));

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
            showError(error.message);

        } finally {
            isUpdating = false;

            if (!succeeded) {
                buttons.forEach((button, index) => {
                    button.disabled = previousStates[index];
                });

                scheduleRefresh();
            }
        }

        if (succeeded) {
            // Lấy lại trạng thái do backend quyết định.
            await loadOrders();
        }
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
                        values.push(String(entry));
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
            return "Giao hàng nội bộ";
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