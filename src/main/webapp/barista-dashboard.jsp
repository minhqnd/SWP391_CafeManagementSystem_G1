<%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <!DOCTYPE html>
    <html lang="vi">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Aura Café - KDS Barista</title>
        <link
            href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Inter:wght@400;500;600;700;800&display=swap"
            rel="stylesheet">
        <link
            href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200"
            rel="stylesheet">
        <style>
            :root {
                --red: #8b1528;
                --red-dark: #70101f;
                --red-soft: #fdf2f4;
                --orange: #d97706;
                --green: #087f5b;
                --green-soft: #e8fff5;
                --blue: #2563eb;
                --gray-50: #f8fafc;
                --gray-100: #f1f5f9;
                --gray-200: #e2e8f0;
                --gray-500: #64748b;
                --gray-700: #0f172a;
                --shadow: 0 2px 10px rgba(15, 23, 42, .04);
            }

            * {
                box-sizing: border-box;
            }

            body {
                margin: 0;
                background: var(--gray-50);
                color: var(--gray-700);
                font-family: 'Plus Jakarta Sans', 'Inter', sans-serif;
                letter-spacing: -0.01em;
            }

            button {
                font: inherit;
            }

            .topbar {
                height: 76px;
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: 20px;
                padding: 0 28px;
                background: #fff;
                border-bottom: 1px solid var(--gray-200);
            }

            .brand {
                display: flex;
                align-items: center;
                gap: 12px;
                min-width: 245px;
            }

            .brand-name {
                color: var(--red);
                font-size: 23px;
                font-weight: 800;
                letter-spacing: -0.03em;
            }

            .brand-badge {
                padding: 7px 10px;
                border-radius: 5px;
                color: white;
                background: var(--red);
                font-size: 11px;
                font-weight: 800;
                letter-spacing: .04em;
            }

            .station-info {
                flex: 1;
                min-width: 180px;
            }

            .station-info strong {
                display: block;
                font-size: 15px;
            }

            .station-info span {
                color: var(--gray-500);
                font-size: 12px;
            }

            .mode-switch {
                display: flex;
                align-items: center;
                gap: 4px;
                padding: 4px;
                border-radius: 14px;
                background: var(--gray-100);
            }

            .mode-switch>span {
                padding: 10px 14px;
                border-radius: 10px;
                color: var(--gray-500);
                font-size: 13px;
                font-weight: 700;
                white-space: nowrap;
            }

            .mode-switch>span.active {
                color: white;
                background: var(--red);
                box-shadow: 0 4px 10px rgba(139, 21, 40, .18);
            }

            .mode-switch .material-symbols-outlined {
                padding: 0;
                color: inherit;
            }

            .shift-info {
                min-width: 145px;
                text-align: right;
            }

            .shift-info strong,
            .shift-info span {
                display: block;
            }

            .shift-info strong {
                font-size: 13px;
            }

            .shift-info span {
                color: var(--gray-500);
                font-size: 11px;
            }

            .online {
                display: inline-flex;
                align-items: center;
                gap: 7px;
                margin-left: 14px;
                padding: 8px 12px;
                border: 1px solid #93e7c5;
                border-radius: 20px;
                color: var(--green);
                background: var(--green-soft);
                font-size: 13px;
                font-weight: 700;
            }

            .online::before {
                content: "";
                width: 8px;
                height: 8px;
                border-radius: 50%;
                background: #25b986;
            }

            .user-actions {
                display: flex;
                align-items: center;
                gap: 10px;
                white-space: nowrap;
            }

            .user-actions span {
                font-size: 13px;
                color: var(--gray-500);
            }

            .outline-button,
            .logout-button {
                padding: 9px 13px;
                border-radius: 8px;
                cursor: pointer;
                font-weight: 700;
            }

            .outline-button {
                border: 1px solid var(--gray-200);
                color: var(--gray-700);
                background: white;
                transition: .2s ease;
            }

            .outline-button:hover {
                color: var(--red);
                border-color: rgba(139, 21, 40, .3);
                background: var(--red-soft);
            }

            .logout-button {
                border: 0;
                color: white;
                background: var(--red);
                text-decoration: none;
            }

            .logout-button:hover {
                color: white;
                background: var(--red-dark);
            }

            .dashboard {
                padding: 20px 28px 30px;
            }

            .summary {
                display: grid;
                grid-template-columns: repeat(3, minmax(170px, 1fr));
                gap: 14px;
                margin-bottom: 18px;
            }

            .summary-card {
                padding: 16px 18px;
                border: 1px solid var(--gray-200);
                border-radius: 16px;
                background: white;
                box-shadow: var(--shadow);
            }

            .summary-card p {
                margin: 0 0 6px;
                color: var(--gray-500);
                font-size: 13px;
                font-weight: 700;
                text-transform: uppercase;
            }

            .summary-card strong {
                font-size: 30px;
            }

            .summary-card.waiting strong {
                color: var(--red);
            }

            .summary-card.preparing strong {
                color: var(--orange);
            }

            .summary-card.ready strong {
                color: var(--green);
            }

            .toolbar {
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: 14px;
                margin-bottom: 16px;
                padding: 10px;
                border: 1px solid var(--gray-200);
                border-radius: 12px;
                background: white;
            }

            .filters {
                display: flex;
                gap: 8px;
                flex-wrap: wrap;
            }

            .filter-button {
                padding: 9px 14px;
                border: 0;
                border: 1px solid transparent;
                border-radius: 999px;
                color: var(--gray-700);
                background: white;
                cursor: pointer;
                font-weight: 700;
            }

            .filter-button:hover {
                color: var(--red);
                border-color: rgba(139, 21, 40, .25);
                background: var(--red-soft);
            }

            .filter-button.active {
                color: white;
                background: var(--red);
            }

            .refresh-info {
                color: var(--gray-500);
                font-size: 13px;
                white-space: nowrap;
            }

            .refresh-info strong {
                color: var(--red);
            }

            .columns {
                display: grid;
                grid-template-columns: repeat(3, minmax(280px, 1fr));
                gap: 16px;
                align-items: start;
            }

            .column {
                min-height: 350px;
                padding: 12px;
                border-radius: 14px;
                background: #f1f5f9;
            }

            .column-header {
                display: flex;
                align-items: center;
                justify-content: space-between;
                margin: 0 4px 12px;
            }

            .column-header h2 {
                margin: 0;
                font-size: 16px;
            }

            .column-header span {
                min-width: 26px;
                padding: 4px 8px;
                border-radius: 12px;
                color: white;
                background: var(--gray-500);
                text-align: center;
                font-size: 12px;
                font-weight: 800;
            }

            .column.waiting {
                border-top: 4px solid var(--red);
            }

            .column.preparing {
                border-top: 4px solid var(--orange);
            }

            .column.ready {
                border-top: 4px solid var(--green);
            }

            .order-card {
                margin-bottom: 12px;
                overflow: hidden;
                border: 1px solid var(--gray-200);
                border-radius: 16px;
                background: white;
                box-shadow: var(--shadow);
            }

            .order-card-header {
                padding: 14px;
                border-bottom: 1px solid var(--gray-200);
            }

            .order-title-row,
            .order-meta-row {
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: 8px;
            }

            .order-title-row strong {
                font-size: 17px;
            }

            .order-time {
                color: var(--gray-500);
                font-size: 12px;
            }

            .order-type {
                display: inline-block;
                margin-top: 9px;
                padding: 5px 8px;
                border-radius: 6px;
                color: var(--gray-700);
                background: var(--gray-100);
                font-size: 12px;
                font-weight: 700;
            }

            .order-meta-row {
                justify-content: flex-start;
                margin-top: 8px;
                color: var(--gray-500);
                font-size: 13px;
            }

            .order-note {
                margin-top: 10px;
                padding: 9px 10px;
                border-left: 3px solid var(--orange);
                border-radius: 5px;
                color: #7c4a03;
                background: #fff8e6;
                font-size: 13px;
                line-height: 1.4;
            }

            .order-items {
                padding: 12px;
            }

            .item-row {
                display: flex;
                align-items: flex-start;
                justify-content: space-between;
                gap: 10px;
                margin-bottom: 9px;
                padding: 11px;
                border: 1px solid var(--gray-200);
                border-radius: 10px;
                background: #fff;
            }

            .item-row:last-child {
                margin-bottom: 0;
            }

            .item-info {
                min-width: 0;
            }

            .item-name {
                color: #252525;
                font-size: 14px;
                font-weight: 800;
                line-height: 1.35;
            }

            .item-quantity {
                margin-right: 5px;
                color: var(--red);
                font-weight: 900;
            }

            .customization {
                display: flex;
                flex-wrap: wrap;
                gap: 5px;
                margin-top: 7px;
            }

            .customization span {
                padding: 3px 6px;
                border: 1px solid var(--gray-200);
                border-radius: 5px;
                color: var(--gray-500);
                font-size: 11px;
            }

            .item-action {
                flex: 0 0 auto;
                min-width: 92px;
                padding: 9px 8px;
                border: 0;
                border-radius: 10px;
                color: #5f6368;
                background: #e9ecef;
                cursor: pointer;
                font-size: 11px;
                font-weight: 900;
            }

            .item-action:hover:not(:disabled) {
                background: #dfe3e6;
            }

            .item-action.done {
                color: var(--green);
                background: var(--green-soft);
                cursor: default;
            }

            .item-action:disabled:not(.done) {
                cursor: not-allowed;
                opacity: .65;
            }

            .empty-column {
                padding: 40px 12px;
                color: var(--gray-500);
                text-align: center;
                font-size: 13px;
            }

            .error-message {
                display: none;
                margin-bottom: 14px;
                padding: 12px 14px;
                border: 1px solid #ffc9c9;
                border-radius: 8px;
                color: #a8071a;
                background: #fff1f1;
            }

            @media (max-width: 1050px) {
                .topbar {
                    height: auto;
                    min-height: 76px;
                    flex-wrap: wrap;
                    padding: 15px 18px;
                }

                .station-info {
                    order: 3;
                    flex-basis: 100%;
                }

                .dashboard {
                    padding: 16px;
                }

                .columns {
                    overflow-x: auto;
                    grid-template-columns: repeat(3, minmax(300px, 1fr));
                }
            }

            @media (max-width: 650px) {
                .summary {
                    grid-template-columns: 1fr;
                }

                .toolbar {
                    align-items: flex-start;
                    flex-direction: column;
                }

                .brand-badge {
                    display: none;
                }

                .user-actions span {
                    display: none;
                }
            }
        </style>
    </head>

    <body>
        <header class="topbar">
            <div class="brand">
                <span class="brand-name">Aura Café</span>
                <span class="brand-badge">BARISTA KDS TERMINAL</span>
            </div>

            <div class="station-info">
                <strong>Trạm 01 · Pha chế chính</strong>
                <span>${sessionScope.user.fullName} · Barista</span>
                <span class="online">KDS Online</span>
            </div>

            <div class="mode-switch" aria-label="Chế độ màn hình">
                <span class="active">
                    <span class="material-symbols-outlined"
                        style="font-size: 17px; vertical-align: middle;">point_of_sale</span>
                    Màn hình KDS (F1)
                </span>
                <span>
                    <span class="material-symbols-outlined"
                        style="font-size: 17px; vertical-align: middle;">menu_book</span>
                    Công thức & Định lượng (F2)
                </span>
            </div>

            <div class="shift-info">
                <strong>Ca sáng: 06:30 - 14:30</strong>
                <span>Đang trong ca làm việc</span>
            </div>

            <div class="user-actions">
                <span id="last-updated">Đang tải dữ liệu...</span>
                <button id="refresh-button" class="outline-button" type="button">Làm mới</button>
                <a class="logout-button" href="${pageContext.request.contextPath}/login?action=logout">Đăng xuất</a>
            </div>
        </header>

        <main class="dashboard">
            <section class="summary" aria-label="Thống kê đơn hàng">
                <div class="summary-card waiting">
                    <p>Đang chờ pha</p>
                    <strong id="waiting-count">0</strong>
                </div>
                <div class="summary-card preparing">
                    <p>Đang thực hiện</p>
                    <strong id="preparing-count">0</strong>
                </div>
                <div class="summary-card ready">
                    <p>Hoàn tất trong ca</p>
                    <strong id="ready-count">0</strong>
                </div>
            </section>

            <div id="error-message" class="error-message"></div>

            <section class="toolbar">
                <div class="filters" aria-label="Lọc nhóm sản phẩm">
                    <button class="filter-button active" data-group="all" type="button">Tất cả sản phẩm</button>
                    <button class="filter-button" data-group="coffee" type="button">Cà phê</button>
                    <button class="filter-button" data-group="tea" type="button">Trà & Đá xay</button>
                    <button class="filter-button" data-group="bakery" type="button">Bánh</button>
                    <button class="filter-button" data-group="bottled" type="button">Đóng chai</button>
                </div>
                <div class="refresh-info">Tự cập nhật mỗi <strong>5 giây</strong></div>
            </section>

            <section class="columns">
                <div class="column waiting">
                    <div class="column-header">
                        <h2>Đang chờ pha</h2>
                        <span id="waiting-column-count">0</span>
                    </div>
                    <div id="waiting-orders"></div>
                </div>

                <div class="column preparing">
                    <div class="column-header">
                        <h2>Đang thực hiện</h2>
                        <span id="preparing-column-count">0</span>
                    </div>
                    <div id="preparing-orders"></div>
                </div>

                <div class="column ready">
                    <div class="column-header">
                        <h2>Đã hoàn tất</h2>
                        <span id="ready-column-count">0</span>
                    </div>
                    <div id="ready-orders"></div>
                </div>
            </section>
        </main>

        <script>
            const contextPath = '${pageContext.request.contextPath}';
            const refreshIntervalMs = 5000;
            let activeGroup = 'all';
            let isLoading = false;

            const waitingOrders = document.getElementById('waiting-orders');
            const preparingOrders = document.getElementById('preparing-orders');
            const readyOrders = document.getElementById('ready-orders');
            const errorMessage = document.getElementById('error-message');

            document.querySelectorAll('.filter-button').forEach(button => {
                button.addEventListener('click', () => {
                    activeGroup = button.dataset.group;

                    document.querySelectorAll('.filter-button').forEach(item => {
                        item.classList.toggle('active', item === button);
                    });

                    loadOrders();
                });
            });

            document.getElementById('refresh-button').addEventListener('click', loadOrders);

            async function loadOrders() {
                if (isLoading) {
                    return;
                }

                isLoading = true;
                hideError();

                try {
                    const url = contextPath
                        + '/barista?action=orders&group='
                        + encodeURIComponent(activeGroup);

                    const response = await fetch(url, {
                        method: 'GET',
                        headers: { 'Accept': 'application/json' },
                        cache: 'no-store'
                    });

                    const data = await response.json();

                    if (!response.ok) {
                        throw new Error(data.message || 'Không thể tải danh sách đơn.');
                    }

                    updateStatistics(data.statistics || {});
                    renderOrders(data.orders || []);
                    document.getElementById('last-updated').textContent =
                        'Cập nhật lúc ' + new Date().toLocaleTimeString('vi-VN');
                } catch (error) {
                    showError(error.message);
                } finally {
                    isLoading = false;
                }
            }

            function updateStatistics(statistics) {
                document.getElementById('waiting-count').textContent = statistics.waiting || 0;
                document.getElementById('preparing-count').textContent = statistics.preparing || 0;
                document.getElementById('ready-count').textContent = statistics.ready || 0;
            }

            function renderOrders(orders) {
                clearOrders();

                const activeOrders = orders.filter(order =>
                    order.status === 'CONFIRMED' || order.status === 'PREPARING'
                );

                // Backend cũng kiểm tra FIFO. Biến này chỉ giúp khóa nút sớm trên giao diện.
                const firstActionableOrderId = activeOrders.length > 0
                    ? activeOrders[0].id
                    : null;

                let waitingTotal = 0;
                let preparingTotal = 0;
                let readyTotal = 0;

                orders.forEach(order => {
                    const card = createOrderCard(
                        order,
                        order.id === firstActionableOrderId
                    );

                    if (order.status === 'CONFIRMED') {
                        waitingOrders.appendChild(card);
                        waitingTotal++;
                    } else if (order.status === 'PREPARING') {
                        preparingOrders.appendChild(card);
                        preparingTotal++;
                    } else if (order.status === 'READY') {
                        readyOrders.appendChild(card);
                        readyTotal++;
                    }
                });

                setEmptyState(waitingOrders, waitingTotal === 0, 'Không có đơn đang chờ.');
                setEmptyState(preparingOrders, preparingTotal === 0, 'Không có đơn đang thực hiện.');
                setEmptyState(readyOrders, readyTotal === 0, 'Chưa có đơn hoàn tất.');

                document.getElementById('waiting-column-count').textContent = waitingTotal;
                document.getElementById('preparing-column-count').textContent = preparingTotal;
                document.getElementById('ready-column-count').textContent = readyTotal;
            }

            function clearOrders() {
                waitingOrders.replaceChildren();
                preparingOrders.replaceChildren();
                readyOrders.replaceChildren();
            }

            function createOrderCard(order, canAct) {
                const card = document.createElement('article');
                card.className = 'order-card';

                const header = document.createElement('div');
                header.className = 'order-card-header';

                const titleRow = document.createElement('div');
                titleRow.className = 'order-title-row';

                const title = document.createElement('strong');
                title.textContent = '#' + (order.orderCode || order.id);

                const time = document.createElement('span');
                time.className = 'order-time';
                time.textContent = formatCreatedAt(order.createdAt);

                titleRow.append(title, time);

                const type = document.createElement('span');
                type.className = 'order-type';
                type.textContent = getOrderTypeLabel(order);

                const meta = document.createElement('div');
                meta.className = 'order-meta-row';
                meta.textContent = getOrderMeta(order);

                header.append(titleRow, type, meta);

                if (order.orderNote && order.orderNote.trim()) {
                    const note = document.createElement('div');
                    note.className = 'order-note';
                    note.textContent = 'Ghi chú: ' + order.orderNote;
                    header.appendChild(note);
                }

                const itemContainer = document.createElement('div');
                itemContainer.className = 'order-items';

                (order.items || []).forEach(item => {
                    itemContainer.appendChild(createItemRow(item, canAct, order.status));
                });

                card.append(header, itemContainer);
                return card;
            }

            function createItemRow(item, canAct, orderStatus) {
                const row = document.createElement('div');
                row.className = 'item-row';

                const info = document.createElement('div');
                info.className = 'item-info';

                const name = document.createElement('div');
                name.className = 'item-name';

                const quantity = document.createElement('span');
                quantity.className = 'item-quantity';
                quantity.textContent = item.quantity + 'x';

                const productName = document.createElement('span');
                productName.textContent = ' ' + (item.productName || 'Sản phẩm');

                name.append(quantity, productName);
                info.appendChild(name);

                const customization = parseCustomization(item.customization);
                if (customization.length > 0) {
                    const options = document.createElement('div');
                    options.className = 'customization';

                    customization.forEach(value => {
                        const option = document.createElement('span');
                        option.textContent = value;
                        options.appendChild(option);
                    });

                    info.appendChild(options);
                }

                const action = document.createElement('button');
                action.type = 'button';
                action.className = 'item-action';

                const isReady = item.preparationStatus === 'READY';

                if (isReady) {
                    action.textContent = 'XONG';
                    action.classList.add('done');
                    action.disabled = true;
                } else {
                    action.textContent = 'ĐANG LÀM';
                    action.disabled = !canAct || orderStatus === 'READY';

                    action.addEventListener('click', () => {
                        updateItemStatus(item.id, action);
                    });
                }

                row.append(info, action);
                return row;
            }

            async function updateItemStatus(itemId, button) {
                button.disabled = true;

                try {
                    const body = new URLSearchParams();
                    body.append('action', 'update-item-status');
                    body.append('itemId', itemId);

                    const response = await fetch(contextPath + '/barista', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded',
                            'Accept': 'application/json'
                        },
                        body
                    });

                    const data = await response.json();

                    if (!response.ok) {
                        throw new Error(data.message || 'Không thể cập nhật món.');
                    }

                    await loadOrders();
                } catch (error) {
                    button.disabled = false;
                    showError(error.message);
                }
            }

            function parseCustomization(customization) {
                if (!customization) {
                    return [];
                }

                try {
                    const parsed = JSON.parse(customization);
                    const values = [];

                    Object.entries(parsed).forEach(([key, value]) => {
                        if (Array.isArray(value)) {
                            value.forEach(item => values.push(String(item)));
                        } else {
                            values.push(key + ': ' + value);
                        }
                    });

                    return values;
                } catch (error) {
                    return [customization];
                }
            }

            function getOrderTypeLabel(order) {
                if (order.orderType === 'DINE_IN') {
                    return 'Dùng tại quán';
                }

                if (order.orderType === 'ONLINE') {
                    return 'Giao hàng nội bộ';
                }

                return 'Mua mang đi';
            }

            function getOrderMeta(order) {
                if (order.orderType === 'DINE_IN') {
                    return order.tableNumber
                        ? 'Bàn ' + order.tableNumber
                        : 'Khách dùng tại quán';
                }

                if (order.orderType === 'ONLINE') {
                    return order.recipientName
                        ? 'Khách: ' + order.recipientName
                        : 'Nhân viên quán giao';
                }

                return 'Khách mua mang đi';
            }

            function formatCreatedAt(createdAt) {
                if (!createdAt) {
                    return '';
                }

                const date = new Date(createdAt);
                if (Number.isNaN(date.getTime())) {
                    return createdAt;
                }

                return date.toLocaleTimeString('vi-VN', {
                    hour: '2-digit',
                    minute: '2-digit'
                });
            }

            function setEmptyState(container, isEmpty, message) {
                if (!isEmpty) {
                    return;
                }

                const empty = document.createElement('div');
                empty.className = 'empty-column';
                empty.textContent = message;
                container.appendChild(empty);
            }

            function showError(message) {
                errorMessage.textContent = message;
                errorMessage.style.display = 'block';
            }

            function hideError() {
                errorMessage.textContent = '';
                errorMessage.style.display = 'none';
            }

            loadOrders();
            setInterval(loadOrders, refreshIntervalMs);
        </script>
    </body>

    </html>