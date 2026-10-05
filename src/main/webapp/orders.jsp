<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Aura Café | Đơn hàng của tôi</title>

    <!-- Google Fonts & Icons -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet" />
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/cart.css" rel="stylesheet">

    <style>
        /* Scoped Enhancements for Orders Page */
        .orders-page-header {
            margin-bottom: 1.5rem;
        }
        .orders-title {
            font-size: 1.65rem;
            font-weight: 800;
            color: #1e293b;
            letter-spacing: -0.02em;
        }
        .orders-subtitle {
            color: #64748b;
            font-size: 0.88rem;
        }

        /* Filter Bar */
        .orders-filter-bar {
            display: flex;
            align-items: center;
            gap: 0.5rem;
            flex-wrap: nowrap;
        }
        .filter-search-box {
            position: relative;
            min-width: 220px;
        }
        .filter-search-box .filter-icon {
            position: absolute;
            left: 0.75rem;
            top: 50%;
            transform: translateY(-50%);
            color: #94a3b8;
            font-size: 18px;
        }
        .filter-search-box .form-control {
            height: 38px;
            padding-left: 2.25rem;
            font-size: 0.85rem;
            border-radius: 8px;
            border: 1px solid #cbd5e1;
            background: #fff;
        }
        .filter-date-input {
            height: 38px;
            font-size: 0.82rem;
            border-radius: 8px;
            border: 1px solid #cbd5e1;
            background: #fff;
            width: 140px;
        }
        .btn-filter-submit {
            height: 38px;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 0 1rem;
            border-radius: 8px;
            font-size: 0.85rem;
            font-weight: 600;
            background: #900b1a;
            color: #fff;
            border: none;
            transition: all 0.15s ease;
        }
        .btn-filter-submit:hover {
            background: #780815;
            color: #fff;
        }
        .btn-filter-clear {
            height: 38px;
            display: inline-flex;
            align-items: center;
            padding: 0 0.85rem;
            border-radius: 8px;
            font-size: 0.82rem;
            font-weight: 600;
            background: #f1f5f9;
            color: #475569;
            text-decoration: none;
            border: 1px solid #e2e8f0;
            white-space: nowrap;
        }
        .btn-filter-clear:hover {
            background: #e2e8f0;
            color: #1e293b;
        }

        /* Modern Tabs */
        .orders-tabs-nav {
            display: flex;
            gap: 0.4rem;
            overflow-x: auto;
            padding-bottom: 0.25rem;
            border-bottom: 1px solid #e2e8f0;
            margin-bottom: 1.5rem;
        }
        .orders-tab-btn {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 0.55rem 0.95rem;
            font-size: 0.85rem;
            font-weight: 600;
            color: #64748b;
            text-decoration: none;
            border-radius: 8px 8px 0 0;
            border-bottom: 2px solid transparent;
            white-space: nowrap;
            transition: all 0.15s ease;
        }
        .orders-tab-btn:hover {
            color: #1e293b;
            background: #f8fafc;
        }
        .orders-tab-btn.active {
            color: #900b1a;
            border-bottom-color: #900b1a;
            font-weight: 700;
            background: rgba(144, 11, 26, 0.04);
        }
        .tab-badge {
            font-size: 0.72rem;
            padding: 1px 7px;
            border-radius: 999px;
            background: #e2e8f0;
            color: #475569;
            font-weight: 600;
        }
        .orders-tab-btn.active .tab-badge {
            background: #900b1a;
            color: #fff;
        }

        /* Active In-Progress Card */
        .aura-active-card {
            background: #fff;
            border-radius: 14px;
            border: 1px solid #e2e8f0;
            box-shadow: 0 4px 16px -2px rgba(0,0,0,0.04);
            overflow: hidden;
            margin-bottom: 2rem;
        }
        .aura-active-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 0.75rem;
            padding: 1rem 1.25rem;
            background: #fcfdfe;
            border-bottom: 1px solid #edf2f7;
        }
        .order-code-tag {
            font-size: 0.92rem;
            font-weight: 800;
            color: #900b1a;
            background: #fdf2f2;
            padding: 4px 10px;
            border-radius: 6px;
            letter-spacing: 0.2px;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }
        .order-type-pill {
            font-size: 0.76rem;
            font-weight: 600;
            padding: 3px 8px;
            border-radius: 6px;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }
        .order-type-pill.delivery {
            background: #eff6ff;
            color: #1d4ed8;
        }
        .order-type-pill.pickup {
            background: #f0fdf4;
            color: #15803d;
        }
        .live-status-badge {
            font-size: 0.8rem;
            font-weight: 700;
            padding: 4px 10px;
            border-radius: 999px;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: #f0fdf4;
            color: #166534;
            border: 1px solid #bbf7d0;
        }
        .pulse-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #22c55e;
            animation: pulse-ring 1.8s cubic-bezier(0.4, 0, 0.6, 1) infinite;
        }
        @keyframes pulse-ring {
            0%, 100% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.4; transform: scale(0.85); }
        }

        /* Modern Stepper */
        .stepper-container {
            padding: 1.25rem;
            background: #f8fafc;
            border-radius: 10px;
            border: 1px solid #f1f5f9;
            margin-bottom: 1.25rem;
        }
        .stepper-grid {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            position: relative;
            text-align: center;
        }
        .stepper-item {
            position: relative;
            z-index: 1;
        }
        .stepper-item::before {
            content: '';
            position: absolute;
            top: 18px;
            left: -50%;
            width: 100%;
            height: 3px;
            background: #e2e8f0;
            z-index: 0;
        }
        .stepper-item:first-child::before { display: none; }
        .stepper-item.done::before,
        .stepper-item.current::before {
            background: #900b1a;
        }
        .stepper-node {
            position: relative;
            z-index: 2;
            width: 36px;
            height: 36px;
            border-radius: 50%;
            margin: 0 auto 0.4rem;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background: #fff;
            border: 2px solid #cbd5e1;
            color: #94a3b8;
            transition: all 0.2s ease;
        }
        .stepper-item.done .stepper-node {
            background: #900b1a;
            border-color: #900b1a;
            color: #fff;
        }
        .stepper-item.current .stepper-node {
            background: #fff;
            border-color: #900b1a;
            color: #900b1a;
            box-shadow: 0 0 0 4px rgba(144, 11, 26, 0.15);
        }
        .stepper-title {
            font-size: 0.78rem;
            font-weight: 700;
            color: #334155;
            margin-bottom: 2px;
        }
        .stepper-item.current .stepper-title {
            color: #900b1a;
        }
        .stepper-sub {
            font-size: 0.7rem;
            color: #64748b;
        }

        /* Order Items List */
        .order-product-row {
            display: flex;
            align-items: center;
            gap: 0.85rem;
            padding: 0.65rem 0;
            border-bottom: 1px solid #f1f5f9;
        }
        .order-product-img {
            width: 48px;
            height: 48px;
            border-radius: 8px;
            object-fit: cover;
            flex-shrink: 0;
            border: 1px solid #e2e8f0;
            background: #f8fafc;
        }
        .order-product-title {
            font-size: 0.88rem;
            font-weight: 700;
            color: #1e293b;
            margin-bottom: 2px;
        }
        .order-product-opt {
            font-size: 0.76rem;
            color: #64748b;
        }
        .order-product-price {
            font-size: 0.88rem;
            font-weight: 700;
            color: #1e293b;
            text-align: right;
        }
        .order-product-qty {
            font-size: 0.75rem;
            color: #94a3b8;
        }

        /* Side Info Panel */
        .order-side-panel {
            background: #f8fafc;
            border-radius: 10px;
            padding: 1.1rem;
            border: 1px solid #e2e8f0;
            height: 100%;
            display: flex;
            flex-column;
            justify-content: space-between;
        }
        .side-panel-title {
            font-size: 0.72rem;
            font-weight: 800;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: #94a3b8;
            margin-bottom: 0.85rem;
        }
        .side-info-item {
            display: flex;
            align-items: flex-start;
            gap: 0.6rem;
            margin-bottom: 0.75rem;
        }
        .side-info-icon {
            font-size: 18px;
            color: #900b1a;
            margin-top: 1px;
            flex-shrink: 0;
        }
        .side-info-label {
            font-size: 0.72rem;
            color: #64748b;
            text-transform: uppercase;
            font-weight: 600;
            letter-spacing: 0.3px;
        }
        .side-info-val {
            font-size: 0.82rem;
            font-weight: 600;
            color: #1e293b;
            line-height: 1.35;
        }

        /* History Card */
        .aura-history-card {
            background: #fff;
            border: 1px solid #e2e8f0;
            border-radius: 12px;
            padding: 1.1rem 1.25rem;
            margin-bottom: 0.85rem;
            transition: all 0.15s ease;
        }
        .aura-history-card:hover {
            border-color: #cbd5e1;
            box-shadow: 0 4px 12px -2px rgba(0,0,0,0.04);
        }
        .history-card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 0.5rem;
            padding-bottom: 0.75rem;
            border-bottom: 1px dashed #e2e8f0;
            margin-bottom: 0.75rem;
        }
        .history-card-body {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 1rem;
        }
        .history-img-box {
            width: 52px;
            height: 52px;
            border-radius: 10px;
            object-fit: cover;
            border: 1px solid #e2e8f0;
            background: #f8fafc;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            color: #900b1a;
            flex-shrink: 0;
        }
        .history-items-text {
            font-size: 0.9rem;
            font-weight: 700;
            color: #1e293b;
            margin-bottom: 3px;
        }
        .history-meta-text {
            font-size: 0.78rem;
            color: #64748b;
            display: flex;
            flex-wrap: wrap;
            gap: 0.75rem;
            align-items: center;
        }
        .history-price-tag {
            font-size: 1.1rem;
            font-weight: 800;
            color: #900b1a;
            text-align: right;
            white-space: nowrap;
        }
        .history-card-footer {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 0.5rem;
            padding-top: 0.75rem;
            margin-top: 0.75rem;
            border-top: 1px solid #f1f5f9;
        }

        /* Status Badges */
        .status-pill {
            font-size: 0.75rem;
            font-weight: 700;
            padding: 3px 8px;
            border-radius: 6px;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }
        .status-pill.completed { background: #dcfce7; color: #15803d; }
        .status-pill.cancelled { background: #fee2e2; color: #b91c1c; }
        .status-pill.pending { background: #fef9c3; color: #854d0e; }
        .status-pill.processing { background: #e0f2fe; color: #0369a1; }
        .status-pill.delivering { background: #f3e8ff; color: #6b21a8; }

        @media (max-width: 767.98px) {
            .orders-filter-bar {
                flex-wrap: wrap;
                width: 100%;
            }
            .filter-search-box {
                flex: 1 1 100%;
            }
            .stepper-grid {
                grid-template-columns: repeat(2, 1fr);
                gap: 1rem 0;
            }
            .stepper-item::before { display: none; }
        }
    </style>
</head>
<body class="aura-cart-page">

    <jsp:include page="/components/header.jsp">
        <jsp:param name="activePage" value="orders" />
    </jsp:include>

    <main class="container-xl px-3 px-lg-4 py-4">

        <!-- Tiêu đề & Thanh tìm kiếm -->
        <div class="orders-page-header d-flex flex-column flex-md-row justify-content-between align-items-md-end gap-3">
            <div>
                <h1 class="orders-title mb-1">Đơn hàng của tôi</h1>
                <div class="orders-subtitle">Theo dõi tiến trình pha chế, giao nhận và lịch sử các đơn đã đặt.</div>
            </div>
            <form method="get" action="${ctx}/orders" class="orders-filter-bar">
                <input type="hidden" name="tab" value="${tab}">
                <div class="filter-search-box">
                    <span class="material-symbols-outlined filter-icon">search</span>
                    <input type="text" name="q" value="${fn:escapeXml(keyword)}" maxlength="30"
                           class="form-control" placeholder="Tìm theo mã đơn...">
                </div>
                <input type="date" name="date" value="${date}" class="filter-date-input" title="Lọc theo ngày đặt">
                <button type="submit" class="btn-filter-submit">
                    <span class="material-symbols-outlined" style="font-size: 17px;">filter_alt</span>
                    <span>Lọc</span>
                </button>
                <c:if test="${not empty keyword || not empty date}">
                    <a href="${ctx}/orders?tab=${tab}" class="btn-filter-clear">Xoá lọc</a>
                </c:if>
            </form>
        </div>

        <c:if test="${not empty ordersMessage}">
            <div class="alert alert-success d-flex align-items-center gap-2 py-2 px-3 mb-3" style="font-size: 0.88rem; border-radius: 8px;">
                <span class="material-symbols-outlined text-success" style="font-size: 20px;">check_circle</span>
                <span><c:out value="${ordersMessage}" /></span>
            </div>
        </c:if>
        <c:if test="${not empty ordersError}">
            <div class="alert alert-danger d-flex align-items-center gap-2 py-2 px-3 mb-3" style="font-size: 0.88rem; border-radius: 8px;">
                <span class="material-symbols-outlined text-danger" style="font-size: 20px;">error</span>
                <span><c:out value="${ordersError}" /></span>
            </div>
        </c:if>

        <!-- Tab trạng thái -->
        <div class="orders-tabs-nav">
            <c:forEach var="t" items="${tabCounts}">
                <c:url var="tabUrl" value="/orders">
                    <c:param name="tab" value="${t.key}" />
                    <c:if test="${not empty keyword}"><c:param name="q" value="${keyword}" /></c:if>
                    <c:if test="${not empty date}"><c:param name="date" value="${date}" /></c:if>
                </c:url>
                <a href="${tabUrl}" class="orders-tab-btn ${tab == t.key ? 'active' : ''}">
                    <span>
                        <c:choose>
                            <c:when test="${t.key == 'all'}">Tất cả đơn</c:when>
                            <c:when test="${t.key == 'pending'}">Chờ thanh toán</c:when>
                            <c:when test="${t.key == 'processing'}">Đang xử lý</c:when>
                            <c:when test="${t.key == 'delivering'}">Đang giao hàng</c:when>
                            <c:when test="${t.key == 'completed'}">Đã hoàn thành</c:when>
                            <c:otherwise>Đã hủy</c:otherwise>
                        </c:choose>
                    </span>
                    <span class="tab-badge">${t.value}</span>
                </a>
            </c:forEach>
        </div>

        <%-- ===== Đơn đang thực hiện (CONFIRMED, PREPARING, READY, DELIVERING) ===== --%>
        <c:forEach var="o" items="${activeOrders}">
            <div class="aura-active-card">
                <!-- Header -->
                <div class="aura-active-header">
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                        <span class="order-code-tag">
                            <span class="material-symbols-outlined" style="font-size: 15px;">tag</span>
                            <span>#<c:out value="${o.orderCode}" /></span>
                        </span>
                        <c:choose>
                            <c:when test="${o.delivery}">
                                <span class="order-type-pill delivery">
                                    <span class="material-symbols-outlined" style="font-size: 14px;">delivery_dining</span>
                                    <span>Giao tận nơi</span>
                                </span>
                            </c:when>
                            <c:otherwise>
                                <span class="order-type-pill pickup">
                                    <span class="material-symbols-outlined" style="font-size: 14px;">storefront</span>
                                    <span>Nhận tại quầy</span>
                                </span>
                            </c:otherwise>
                        </c:choose>
                        <span class="text-muted" style="font-size: 0.78rem;">Đặt lúc ${o.createdTimeText}, ${o.createdDateText}</span>
                    </div>

                    <div class="d-flex align-items-center gap-3">
                        <div class="live-status-badge">
                            <span class="pulse-dot"></span>
                            <span>${o.statusLabel}</span>
                        </div>
                        <c:if test="${not empty o.estimatedReceiveText}">
                            <div class="text-end d-none d-sm-block">
                                <div class="text-muted" style="font-size: 0.68rem; text-transform: uppercase; font-weight: 600;">Dự kiến xong</div>
                                <div class="fw-bold" style="font-size: 0.95rem; color: #1e293b;">${o.estimatedReceiveText}</div>
                            </div>
                        </c:if>
                    </div>
                </div>

                <!-- Body -->
                <div class="p-3 p-md-4">
                    <div class="row g-4">
                        <!-- Cột trái: Tiến trình & Món ăn -->
                        <div class="col-lg-7">
                            <!-- Stepper -->
                            <div class="stepper-container">
                                <div class="stepper-grid">
                                    <!-- Bước 1: Đã thanh toán -->
                                    <div class="stepper-item done">
                                        <div class="stepper-node">
                                            <span class="material-symbols-outlined" style="font-size: 18px;">receipt_long</span>
                                        </div>
                                        <div class="stepper-title">Đã thanh toán</div>
                                        <div class="stepper-sub">${not empty o.paidTimeText ? o.paidTimeText : o.createdTimeText}</div>
                                    </div>
                                    <!-- Bước 2: Pha chế -->
                                    <div class="stepper-item ${o.progressStep > 2 ? 'done' : (o.progressStep == 2 ? 'current' : '')}">
                                        <div class="stepper-node">
                                            <span class="material-symbols-outlined" style="font-size: 18px;">coffee_maker</span>
                                        </div>
                                        <div class="stepper-title">Pha chế</div>
                                        <div class="stepper-sub">
                                            ${o.progressStep == 2 ? 'Đang làm' : (o.progressStep > 2 ? 'Đã làm xong' : 'Chờ làm')}
                                        </div>
                                    </div>
                                    <!-- Bước 3: Sẵn sàng / Giao hàng -->
                                    <div class="stepper-item ${o.progressStep > 3 ? 'done' : (o.progressStep == 3 ? 'current' : '')}">
                                        <div class="stepper-node">
                                            <span class="material-symbols-outlined" style="font-size: 18px;">${o.delivery ? 'moped' : 'storefront'}</span>
                                        </div>
                                        <div class="stepper-title">${o.delivery ? 'Đang giao' : 'Sẵn sàng'}</div>
                                        <div class="stepper-sub">
                                            ${o.progressStep == 3 ? (o.delivery ? 'Đang trên đường' : 'Mời nhận món') : ''}
                                        </div>
                                    </div>
                                    <!-- Bước 4: Hoàn thành -->
                                    <div class="stepper-item ${o.progressStep == 4 ? 'done' : ''}">
                                        <div class="stepper-node">
                                            <span class="material-symbols-outlined" style="font-size: 18px;">task_alt</span>
                                        </div>
                                        <div class="stepper-title">Hoàn thành</div>
                                        <div class="stepper-sub">
                                            <c:if test="${not empty o.estimatedReceiveText}">Dự kiến ${o.estimatedReceiveText}</c:if>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Danh sách món -->
                            <div class="mb-3">
                                <div class="fw-bold mb-2 text-slate-800" style="font-size: 0.82rem; text-transform: uppercase; letter-spacing: 0.3px;">Món trong đơn:</div>
                                <c:forEach var="item" items="${o.items}">
                                    <div class="order-product-row">
                                        <c:choose>
                                            <c:when test="${not empty item.imageUrl}">
                                                <img src="${fn:escapeXml(item.imageUrl)}" alt="${fn:escapeXml(item.productName)}" class="order-product-img">
                                            </c:when>
                                            <c:otherwise>
                                                <div class="order-product-img d-flex align-items-center justify-content-center text-secondary">
                                                    <span class="material-symbols-outlined" style="font-size: 20px;">local_cafe</span>
                                                </div>
                                            </c:otherwise>
                                        </c:choose>
                                        <div class="flex-grow-1" style="min-width: 0;">
                                            <div class="order-product-title"><c:out value="${item.productName}" /></div>
                                            <c:if test="${not empty item.optionsText}">
                                                <div class="order-product-opt"><c:out value="${item.optionsText}" /></div>
                                            </c:if>
                                        </div>
                                        <div class="text-end">
                                            <div class="order-product-price"><fmt:formatNumber value="${item.subtotal}" pattern="#,##0" />đ</div>
                                            <div class="order-product-qty">Số lượng: ${item.quantity}</div>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>

                            <!-- Tổng tiền tóm tắt -->
                            <div class="d-flex justify-content-between align-items-center pt-2">
                                <div>
                                    <span class="fw-bold text-slate-700" style="font-size: 0.9rem;">Tổng thanh toán:</span>
                                    <c:if test="${o.discountAmount > 0}">
                                        <span class="badge bg-danger-subtle text-danger ms-2" style="font-size: 0.72rem;">Đã giảm <fmt:formatNumber value="${o.discountAmount}" pattern="#,##0" />đ</span>
                                    </c:if>
                                    <c:if test="${o.delivery}">
                                        <span class="badge bg-light text-secondary ms-1" style="font-size: 0.72rem;">Giao tận nơi</span>
                                    </c:if>
                                </div>
                                <div class="fw-bold fs-5" style="color: #900b1a;">
                                    <fmt:formatNumber value="${o.finalAmount}" pattern="#,##0" />đ
                                </div>
                            </div>
                        </div>

                        <!-- Cột phải: Thông tin giao nhận & hóa đơn -->
                        <div class="col-lg-5">
                            <div class="order-side-panel">
                                <div>
                                    <div class="side-panel-title">Thông tin giao nhận & thanh toán</div>
                                    
                                    <!-- Địa điểm nhận -->
                                    <div class="side-info-item">
                                        <span class="material-symbols-outlined side-info-icon">
                                            ${o.delivery ? 'location_on' : 'storefront'}
                                        </span>
                                        <div>
                                            <div class="side-info-label">${o.delivery ? 'Địa chỉ giao hàng' : 'Nhận tại cửa hàng'}</div>
                                            <div class="side-info-val">
                                                <c:out value="${o.delivery ? o.deliveryAddress : storeAddress}" />
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Người nhận (chỉ hiện khi có tên hoặc sđt) -->
                                    <c:if test="${not empty o.recipientName || not empty o.recipientPhone}">
                                        <div class="side-info-item">
                                            <span class="material-symbols-outlined side-info-icon">person</span>
                                            <div>
                                                <div class="side-info-label">Người nhận</div>
                                                <div class="side-info-val">
                                                    <c:out value="${not empty o.recipientName ? o.recipientName : 'Khách hàng'}" />
                                                    <c:if test="${not empty o.recipientPhone}"> • <c:out value="${o.recipientPhone}" /></c:if>
                                                </div>
                                            </div>
                                        </div>
                                    </c:if>

                                    <!-- Phương thức thanh toán -->
                                    <div class="side-info-item">
                                        <span class="material-symbols-outlined side-info-icon">payments</span>
                                        <div>
                                            <div class="side-info-label">Phương thức thanh toán</div>
                                            <div class="side-info-val">
                                                ${o.paymentMethodLabel}
                                                <c:if test="${not empty o.paidAtText}">
                                                    <span class="text-muted fw-normal" style="font-size: 0.75rem;">(${o.paidTimeText})</span>
                                                </c:if>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Voucher -->
                                    <c:if test="${not empty o.voucherCode}">
                                        <div class="side-info-item">
                                            <span class="material-symbols-outlined side-info-icon">sell</span>
                                            <div>
                                                <div class="side-info-label">Khuyến mãi áp dụng</div>
                                                <div class="side-info-val text-success">
                                                    <c:out value="${o.voucherCode}" /> (-<fmt:formatNumber value="${o.discountAmount}" pattern="#,##0" />đ)
                                                </div>
                                            </div>
                                        </div>
                                    </c:if>

                                    <!-- Ghi chú -->
                                    <c:if test="${not empty o.orderNote}">
                                        <div class="side-info-item">
                                            <span class="material-symbols-outlined side-info-icon">sticky_note_2</span>
                                            <div>
                                                <div class="side-info-label">Ghi chú từ khách</div>
                                                <div class="side-info-val text-muted fst-italic">
                                                    "<c:out value="${o.orderNote}" />"
                                                </div>
                                            </div>
                                        </div>
                                    </c:if>

                                    <!-- Điểm thưởng -->
                                    <c:if test="${o.rewardPoints > 0}">
                                        <div class="side-info-item">
                                            <span class="material-symbols-outlined side-info-icon" style="color: #d97706;">stars</span>
                                            <div>
                                                <div class="side-info-label">Tích luỹ hội viên</div>
                                                <div class="side-info-val text-warning-emphasis">
                                                    +${o.rewardPoints} điểm Aura
                                                </div>
                                            </div>
                                        </div>
                                    </c:if>
                                </div>

                                <!-- Nút e-Bill -->
                                <div class="pt-3 border-top mt-2">
                                    <a href="${ctx}/bill?orderId=${o.id}" class="btn btn-outline-secondary btn-sm w-100 d-inline-flex align-items-center justify-content-center gap-1" target="_blank" style="height: 38px; border-radius: 8px; font-weight: 600;">
                                        <span class="material-symbols-outlined" style="font-size: 17px;">receipt_long</span>
                                        <span>Xem hóa đơn điện tử e-Bill</span>
                                    </a>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </c:forEach>

        <%-- ===== Lịch sử đơn hàng (historyOrders) ===== --%>
        <c:if test="${not empty historyOrders}">
            <div class="d-flex align-items-center justify-content-between mb-3 mt-4">
                <div class="fw-bold fs-6 text-slate-800 d-flex align-items-center gap-2">
                    <span>Lịch sử đơn hàng</span>
                    <span class="badge bg-secondary-subtle text-secondary" style="font-size: 0.75rem;">${fn:length(historyOrders)} đơn</span>
                </div>
            </div>

            <c:forEach var="o" items="${historyOrders}">
                <div class="aura-history-card">
                    <!-- Top: Mã đơn & Trạng thái -->
                    <div class="history-card-header">
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <span class="fw-bold text-slate-900" style="font-size: 0.95rem;">#<c:out value="${o.orderCode}" /></span>
                            <span class="text-muted" style="font-size: 0.78rem;">• ${o.createdDateText} lúc ${o.createdTimeText}</span>
                            <c:choose>
                                <c:when test="${o.delivery}">
                                    <span class="order-type-pill delivery" style="font-size: 0.72rem;">Giao tận nơi</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="order-type-pill pickup" style="font-size: 0.72rem;">Nhận tại quầy</span>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div>
                            <span class="status-pill ${o.statusGroup}">${o.statusLabel}</span>
                        </div>
                    </div>

                    <!-- Middle: Danh sách món & Giá -->
                    <div class="history-card-body">
                        <div class="d-flex align-items-center gap-3" style="min-width: 0; flex: 1;">
                            <c:choose>
                                <c:when test="${o.cancelled}">
                                    <div class="history-img-box" style="background: #fee2e2; color: #b91c1c;">
                                        <span class="material-symbols-outlined">cancel</span>
                                    </div>
                                </c:when>
                                <c:when test="${not empty o.firstImageUrl}">
                                    <img src="${fn:escapeXml(o.firstImageUrl)}" alt="Đơn ${fn:escapeXml(o.orderCode)}" class="history-img-box">
                                </c:when>
                                <c:otherwise>
                                    <div class="history-img-box">
                                        <span class="material-symbols-outlined">local_cafe</span>
                                    </div>
                                </c:otherwise>
                            </c:choose>

                            <div style="min-width: 0;">
                                <div class="history-items-text text-truncate ${o.cancelled ? 'text-decoration-line-through text-muted' : ''}">
                                    <c:out value="${o.itemSummary}" />
                                </div>
                                <div class="history-meta-text">
                                    <span>${o.paymentMethodLabel}</span>
                                    <c:if test="${not empty o.voucherCode}">
                                        <span class="text-success">• Mã: <c:out value="${o.voucherCode}" /></span>
                                    </c:if>
                                    <c:if test="${o.completed && o.rewardPoints > 0}">
                                        <span class="text-warning-emphasis">• +${o.rewardPoints} điểm</span>
                                    </c:if>
                                </div>
                            </div>
                        </div>

                        <!-- Giá tiền -->
                        <div class="history-price-tag ${o.cancelled ? 'text-muted text-decoration-line-through' : ''}">
                            <div class="text-muted" style="font-size: 0.7rem; font-weight: 500; text-transform: uppercase;">Tổng cộng</div>
                            <div><fmt:formatNumber value="${o.finalAmount}" pattern="#,##0" />đ</div>
                        </div>
                    </div>

                    <!-- Footer: Thông báo hoặc Nút thao tác -->
                    <div class="history-card-footer">
                        <div class="text-muted" style="font-size: 0.78rem;">
                            <c:if test="${o.pending}">
                                <span class="text-warning-emphasis fw-semibold">Đang chờ bạn hoàn tất thanh toán.</span>
                            </c:if>
                            <c:if test="${o.cancelled}">
                                <span class="text-muted">Đơn hàng đã được hủy an toàn.</span>
                            </c:if>
                            <c:if test="${o.completed}">
                                <span class="text-success-emphasis">Đã hoàn thành và nhận đơn thành công.</span>
                            </c:if>
                        </div>

                        <div class="d-flex align-items-center gap-2">
                            <!-- Chờ thanh toán -->
                            <c:if test="${o.pending}">
                                <form method="post" action="${ctx}/orders" class="m-0"
                                      onsubmit="return confirm('Bạn chắc chắn muốn hủy đơn #${fn:escapeXml(o.orderCode)}?');">
                                    <input type="hidden" name="action" value="cancel">
                                    <input type="hidden" name="orderId" value="${o.id}">
                                    <input type="hidden" name="tab" value="${tab}">
                                    <button type="submit" class="btn btn-outline-danger btn-sm px-3" style="border-radius: 6px; font-weight: 600;">Hủy đơn</button>
                                </form>
                                <a href="${ctx}/payment?orderId=${o.id}" class="btn btn-aura btn-sm px-3" style="border-radius: 6px; font-weight: 600;">
                                    <span class="material-symbols-outlined" style="font-size: 16px;">qr_code_2</span> Thanh toán
                                </a>
                            </c:if>

                            <!-- Hóa đơn điện tử e-Bill -->
                            <c:if test="${o.completed || not empty o.paidAt}">
                                <a href="${ctx}/bill?orderId=${o.id}" class="btn btn-outline-secondary btn-sm px-3 d-inline-flex align-items-center gap-1" target="_blank" style="border-radius: 6px; font-weight: 600;">
                                    <span class="material-symbols-outlined" style="font-size: 16px;">receipt_long</span>
                                    <span>Hóa đơn</span>
                                </a>
                            </c:if>

                            <!-- Đánh giá -->
                            <c:if test="${o.completed}">
                                <c:url var="feedbackUrl" value="/feedback">
                                    <c:param name="orderCode" value="${o.orderCode}" />
                                </c:url>
                                <a href="${feedbackUrl}" class="btn btn-outline-secondary btn-sm px-3 d-inline-flex align-items-center gap-1" style="border-radius: 6px; font-weight: 600;">
                                    <span class="material-symbols-outlined" style="font-size: 16px;">rate_review</span>
                                    <span>Đánh giá</span>
                                </a>
                            </c:if>

                            <!-- Đặt lại đơn này -->
                            <c:if test="${o.completed || o.cancelled}">
                                <form method="post" action="${ctx}/orders" class="m-0">
                                    <input type="hidden" name="action" value="reorder">
                                    <input type="hidden" name="orderId" value="${o.id}">
                                    <input type="hidden" name="tab" value="${tab}">
                                    <button type="submit" class="btn btn-aura btn-sm px-3 d-inline-flex align-items-center gap-1" style="border-radius: 6px; font-weight: 600;">
                                        <span class="material-symbols-outlined" style="font-size: 16px;">replay</span>
                                        <span>Đặt lại</span>
                                    </button>
                                </form>
                            </c:if>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </c:if>

        <%-- ===== Không có đơn nào ===== --%>
        <c:if test="${empty activeOrders && empty historyOrders}">
            <div class="aura-card p-5 text-center my-4" style="border-radius: 14px; border: 1px dashed #cbd5e1; background: #fff;">
                <span class="material-symbols-outlined text-muted mb-3" style="font-size: 54px; color: #94a3b8;">receipt_long</span>
                <c:choose>
                    <c:when test="${not empty keyword || not empty date || tab != 'all'}">
                        <h2 class="h5 fw-bold text-slate-800">Không tìm thấy đơn hàng phù hợp</h2>
                        <p class="text-muted small">Thử thay đổi tab trạng thái, mã đơn hoặc ngày lọc khác.</p>
                        <a href="${ctx}/orders" class="btn btn-outline-secondary btn-sm px-4" style="border-radius: 8px; font-weight: 600;">Xem tất cả đơn</a>
                    </c:when>
                    <c:otherwise>
                        <h2 class="h5 fw-bold text-slate-800">Bạn chưa có đơn hàng nào</h2>
                        <p class="text-muted small">Chọn ngay đồ uống yêu thích và trải nghiệm hương vị đặc sắc của Aura nhé!</p>
                        <a href="${ctx}/menu" class="btn btn-aura btn-sm px-4" style="border-radius: 8px; font-weight: 600;">Khám phá thực đơn</a>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <%-- ===== Phân trang ===== --%>
        <c:if test="${totalOrders > 0}">
            <div class="d-flex justify-content-between align-items-center mt-3 flex-wrap gap-2 text-muted small">
                <span>Hiển thị ${shownOrders} / ${totalOrders} đơn hàng</span>
                <c:if test="${shownOrders < totalOrders}">
                    <c:url var="moreUrl" value="/orders">
                        <c:param name="tab" value="${tab}" />
                        <c:param name="limit" value="${limit + pageSize}" />
                        <c:if test="${not empty keyword}"><c:param name="q" value="${keyword}" /></c:if>
                        <c:if test="${not empty date}"><c:param name="date" value="${date}" /></c:if>
                    </c:url>
                    <a href="${moreUrl}" class="btn btn-outline-secondary btn-sm px-3" style="border-radius: 8px; font-weight: 600;">
                        Xem thêm đơn cũ hơn
                    </a>
                </c:if>
            </div>
        </c:if>
    </main>

    <!-- Bootstrap 5 JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

    <c:if test="${not empty activeOrders}">
    <script>
        // Tự động làm mới trang sau 8s để cập nhật tiến trình đơn hàng (pha chế, giao hàng)
        setTimeout(function() {
            if (!document.querySelector('input:focus')) {
                window.location.reload();
            }
        }, 8000);
    </script>
    </c:if>
</body>
</html>
