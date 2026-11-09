<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty sessionScope.user or sessionScope.user.role ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Aura Café | Giao dịch ngân hàng (SePay)</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-orders.css?v=2" rel="stylesheet">
    <style>
        .bank-stat-card {
            background: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 12px;
            padding: 18px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            box-shadow: 0 1px 3px rgba(0,0,0,0.02);
        }
        .bank-stat-title {
            font-size: 0.85rem;
            color: #64748b;
            font-weight: 600;
            margin-bottom: 4px;
        }
        .bank-stat-val {
            font-size: 1.5rem;
            font-weight: 800;
        }
        .bank-icon-circle {
            width: 42px;
            height: 42px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        /* Tùy chỉnh bảng giao dịch ngân hàng */
        .bank-tx-table {
            width: 100%;
            table-layout: auto;
            border-collapse: collapse;
        }
        .bank-tx-table > :not(caption) > * > * {
            padding: 0.85rem 1rem;
            vertical-align: middle;
            border-bottom: 1px solid #f1f5f9;
        }
        .bank-tx-table thead th {
            background: #fafaf9;
            color: #64748b;
            font-size: 0.72rem;
            font-weight: 800;
            text-transform: uppercase;
            letter-spacing: 0.03em;
            white-space: nowrap;
            border-bottom: 1px solid #e2e8f0;
        }
        .bank-tx-table tbody tr:hover {
            background-color: #f8fafc;
        }
        .tx-content-box {
            font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
            font-size: 0.78rem;
            color: #334155;
            background: #f8fafc;
            padding: 6px 10px;
            border-radius: 6px;
            border: 1px solid #e2e8f0;
            white-space: normal;
            word-break: break-all;
            line-height: 1.45;
            max-width: 320px;
            min-width: 220px;
            display: block;
        }
        .tx-ref-code {
            font-family: 'SFMono-Regular', Consolas, monospace;
            font-size: 0.8rem;
            font-weight: 600;
            color: #475569;
            white-space: nowrap;
        }
        .badge-reconcile {
            padding: 4px 10px;
            border-radius: 6px;
            font-weight: 700;
            font-size: 0.76rem;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            white-space: nowrap;
        }
        .badge-matched {
            background-color: #ecfdf5;
            color: #059669;
            border: 1px solid #a7f3d0;
        }
        .badge-overpaid {
            background-color: #f5f3ff;
            color: #7c3aed;
            border: 1px solid #ddd6fe;
        }
        .badge-underpaid {
            background-color: #fff7ed;
            color: #c2410c;
            border: 1px solid #fed7aa;
        }
        .badge-duplicate {
            background-color: #fef2f2;
            color: #dc2626;
            border: 1px solid #fecaca;
        }
        .badge-notfound {
            background-color: #fdf2f8;
            color: #db2777;
            border: 1px solid #fbcfe8;
        }
        .badge-cancelled {
            background-color: #f1f5f9;
            color: #475569;
            border: 1px solid #cbd5e1;
        }
        .badge-nocode {
            background-color: #f8fafc;
            color: #64748b;
            border: 1px solid #e2e8f0;
        }
        .reconcile-subtext {
            font-size: 0.72rem;
            color: #64748b;
            margin-top: 4px;
            line-height: 1.35;
            white-space: normal;
            max-width: 240px;
        }
        .order-code-link {
            font-family: 'SFMono-Regular', Consolas, monospace;
            font-weight: 700;
            color: inherit;
            text-decoration: underline;
            text-underline-offset: 2px;
        }
        .sepay-tag {
            color: #dc2626;
            font-weight: 700;
            font-size: 0.82rem;
            display: inline-flex;
            align-items: center;
            gap: 2px;
            white-space: nowrap;
        }
    </style>
</head>
<body class="admin-body">
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="bank-transactions" /></jsp:include>
<main class="admin-main-wrapper orders-page">
    <header class="orders-overview d-flex justify-content-between align-items-center flex-wrap gap-3">
        <div>
            <span class="table-ref">Tích hợp: SePay Bank Hub · Cloudflare Worker Webhooks</span>
            <h1>Giao dịch ngân hàng (SePay)</h1>
            <p>Nhật ký biến động số dư thực tế từ tài khoản ngân hàng quán cà phê và đối soát đơn hàng.</p>
        </div>
        <div class="d-flex align-items-center gap-2">
            <span class="badge bg-white text-secondary border px-3 py-2 d-flex align-items-center gap-2 shadow-sm" id="pollingBadge" style="font-size: 0.78rem; border-radius: 10px;">
                <span class="spinner-grow text-success" role="status" style="width: 8px; height: 8px;"></span>
                <span id="pollingStatusText">Tự động làm mới: <strong>5s</strong></span>
            </span>
            <button type="button" class="btn btn-outline-dark d-flex align-items-center gap-1 py-2 px-3 fw-semibold shadow-sm" style="border-radius: 10px;" id="btnRefresh" onclick="manualRefresh()">
                <span class="material-symbols-outlined fs-5" id="refreshIcon">refresh</span>
                <span>Làm mới ngay</span>
            </button>
        </div>
    </header>

    <!-- Thống kê biến động số dư -->
    <div class="row g-3 mb-4" id="statsContainer">
        <div class="col-md-4">
            <div class="bank-stat-card">
                <div>
                    <div class="bank-stat-title">Tiền vào (Đã nhận)</div>
                    <div class="bank-stat-val text-success">
                        +<fmt:formatNumber value="${totalInAmount}" pattern="#,##0" /> đ
                    </div>
                </div>
                <div class="bank-icon-circle bg-success bg-opacity-10 text-success">
                    <span class="material-symbols-outlined">arrow_downward</span>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="bank-stat-card">
                <div>
                    <div class="bank-stat-title">Số giao dịch ghi nhận</div>
                    <div class="bank-stat-val text-dark">
                        ${totalCount} <span style="font-size: 0.9rem; font-weight: 600; color: #64748b;">giao dịch</span>
                    </div>
                </div>
                <div class="bank-icon-circle bg-secondary bg-opacity-10 text-secondary">
                    <span class="material-symbols-outlined">receipt_long</span>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="bank-stat-card">
                <div>
                    <div class="bank-stat-title">Tài khoản thụ hưởng</div>
                    <div class="bank-stat-val text-primary" style="font-size: 1.25rem;">
                        <c:out value="${bankAccountNo}" />
                    </div>
                    <small class="text-muted"><c:out value="${bankName}" /></small>
                </div>
                <div class="bank-icon-circle bg-primary bg-opacity-10 text-primary">
                    <span class="material-symbols-outlined">account_balance</span>
                </div>
            </div>
        </div>
    </div>

    <!-- Bảng danh sách sao kê ngân hàng SePay -->
    <section class="order-table-card" id="tableCardContainer">
        <div class="d-flex justify-content-between align-items-center p-3 border-bottom">
            <div class="fw-bold fs-6">Danh sách biến động số dư ngân hàng (${totalCount} giao dịch)</div>
            <div class="text-muted small">Cổng kết nối: <code style="color: #0284c7;">${fn:escapeXml(gatewayUrl)}</code></div>
        </div>
        <div class="table-responsive">
            <table class="table bank-tx-table mb-0 align-middle">
                <thead>
                    <tr>
                        <th style="width: 95px;">ID SePay</th>
                        <th style="width: 145px;">Thời gian</th>
                        <th style="width: 135px;">Tài khoản</th>
                        <th class="text-end" style="width: 125px;">Số tiền</th>
                        <th style="min-width: 260px;">Nội dung chuyển khoản</th>
                        <th style="width: 155px;">Mã tham chiếu (FT)</th>
                        <th style="min-width: 220px;">Đối soát hệ thống</th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${empty transactions}">
                            <tr>
                                <td colspan="7" class="text-center text-muted py-5">
                                    <span class="material-symbols-outlined fs-1 text-secondary d-block mb-2">account_balance_wallet</span>
                                    Chưa có giao dịch biến động số dư nào được ghi nhận từ SePay.
                                </td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="tx" items="${transactions}">
                                <tr>
                                    <td>
                                        <span class="fw-bold font-monospace text-secondary" style="font-size: 0.82rem;">${tx.sepayId}</span>
                                    </td>
                                    <td class="text-muted text-nowrap font-monospace" style="font-size: 0.82rem;">
                                        <c:out value="${tx.transactionDate}" />
                                    </td>
                                    <td>
                                        <span class="sepay-tag">
                                            <span class="material-symbols-outlined fs-6" style="color: #ef4444;">star</span>
                                            <c:out value="${tx.accountNumber}" />
                                        </span>
                                        <div style="font-size: 0.72rem; color: #94a3b8;"><c:out value="${tx.gateway}" /></div>
                                    </td>
                                    <td class="text-end text-nowrap">
                                        <span class="text-success fw-bold font-monospace fs-6">${tx.formattedAmount}</span>
                                    </td>
                                    <td>
                                        <div class="tx-content-box" title="${fn:escapeXml(tx.content)}">
                                            <c:out value="${tx.content}" />
                                        </div>
                                    </td>
                                    <td class="text-nowrap">
                                        <span class="tx-ref-code"><c:out value="${tx.referenceCode}" /></span>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${tx.reconcileStatus eq 'MATCHED'}">
                                                <span class="badge-reconcile badge-matched" title="Đã khớp chuẩn và thanh toán đủ số tiền đơn hàng">
                                                    <span class="material-symbols-outlined" style="font-size: 15px;">verified</span>
                                                    Khớp chuẩn: <a href="${ctx}/admin/orders?keyword=${tx.matchedOrderCode}" target="_blank" class="order-code-link">#${tx.matchedOrderCode}</a>
                                                </span>
                                                <div class="reconcile-subtext text-success">
                                                    <c:out value="${tx.reconcileNote}" />
                                                </div>
                                            </c:when>
                                            <c:when test="${tx.reconcileStatus eq 'OVERPAID'}">
                                                <span class="badge-reconcile badge-overpaid" title="Khách chuyển thừa tiền so với giá trị đơn hàng">
                                                    <span class="material-symbols-outlined" style="font-size: 15px;">add_circle</span>
                                                    Thừa tiền: <a href="${ctx}/admin/orders?keyword=${tx.matchedOrderCode}" target="_blank" class="order-code-link">#${tx.matchedOrderCode}</a>
                                                </span>
                                                <div class="reconcile-subtext" style="color: #6d28d9; font-weight: 500;">
                                                    <c:out value="${tx.reconcileNote}" /> · <span class="fw-bold">Cần hoàn tiền thừa</span>
                                                </div>
                                            </c:when>
                                            <c:when test="${tx.reconcileStatus eq 'UNDERPAID'}">
                                                <span class="badge-reconcile badge-underpaid" title="Khách chuyển thiếu tiền">
                                                    <span class="material-symbols-outlined" style="font-size: 15px;">warning</span>
                                                    Thiếu tiền: <a href="${ctx}/admin/orders?keyword=${tx.matchedOrderCode}" target="_blank" class="order-code-link">#${tx.matchedOrderCode}</a>
                                                </span>
                                                <div class="reconcile-subtext" style="color: #c2410c; font-weight: 500;">
                                                    <c:out value="${tx.reconcileNote}" /> · <span class="fw-bold">Chưa đủ đơn</span>
                                                </div>
                                            </c:when>
                                            <c:when test="${tx.reconcileStatus eq 'DUPLICATE'}">
                                                <span class="badge-reconcile badge-duplicate" title="Đơn đã được thanh toán đủ ở giao dịch trước, khách chuyển thêm hoặc chuyển trùng lần 2">
                                                    <span class="material-symbols-outlined" style="font-size: 15px;">content_copy</span>
                                                    Trùng đơn: <a href="${ctx}/admin/orders?keyword=${tx.matchedOrderCode}" target="_blank" class="order-code-link">#${tx.matchedOrderCode}</a>
                                                </span>
                                                <div class="reconcile-subtext text-danger" style="font-weight: 500;">
                                                    <c:out value="${tx.reconcileNote}" /> · <span class="fw-bold">Cần hoàn lại</span>
                                                </div>
                                            </c:when>
                                            <c:when test="${tx.reconcileStatus eq 'ORDER_CANCELLED'}">
                                                <span class="badge-reconcile badge-cancelled" title="Đơn hàng đã bị hủy">
                                                    <span class="material-symbols-outlined" style="font-size: 15px;">cancel</span>
                                                    Đã hủy: <a href="${ctx}/admin/orders?keyword=${tx.matchedOrderCode}" target="_blank" class="order-code-link">#${tx.matchedOrderCode}</a>
                                                </span>
                                                <div class="reconcile-subtext text-secondary">
                                                    <c:out value="${tx.reconcileNote}" />
                                                </div>
                                            </c:when>
                                            <c:when test="${tx.reconcileStatus eq 'ORDER_NOT_FOUND'}">
                                                <span class="badge-reconcile badge-notfound" title="Mã đơn trong nội dung chuyển khoản không tồn tại trên hệ thống">
                                                    <span class="material-symbols-outlined" style="font-size: 15px;">search_off</span>
                                                    Không có đơn #${tx.matchedOrderCode}
                                                </span>
                                                <div class="reconcile-subtext text-danger">
                                                    <c:out value="${tx.reconcileNote}" />
                                                </div>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge-reconcile badge-nocode" title="Nội dung chuyển khoản không tìm thấy cú pháp mã đơn hàng">
                                                    <span class="material-symbols-outlined" style="font-size: 15px;">help_outline</span>
                                                    Sai nội dung
                                                </span>
                                                <div class="reconcile-subtext text-muted">
                                                    Không tìm thấy mã đơn hàng
                                                </div>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
    </section>
</main>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    let isFetching = false;

    async function fetchBankData(isManual = false) {
        if (isFetching) return;
        isFetching = true;

        const refreshIcon = document.getElementById('refreshIcon');
        const statusText = document.getElementById('pollingStatusText');
        if (refreshIcon) refreshIcon.style.animation = 'spin 0.8s linear infinite';
        if (statusText && isManual) statusText.innerHTML = 'Đang đồng bộ...';

        try {
            const resp = await fetch(window.location.href, {
                headers: { 'Cache-Control': 'no-cache' }
            });
            if (resp.ok) {
                const text = await resp.text();
                const parser = new DOMParser();
                const doc = parser.parseFromString(text, 'text/html');

                const newStats = doc.getElementById('statsContainer');
                const newTableCard = doc.getElementById('tableCardContainer');

                const currentStats = document.getElementById('statsContainer');
                const currentTableCard = document.getElementById('tableCardContainer');

                if (newStats && currentStats) {
                    currentStats.innerHTML = newStats.innerHTML;
                }
                if (newTableCard && currentTableCard) {
                    currentTableCard.innerHTML = newTableCard.innerHTML;
                }
            }
        } catch (e) {
            console.error('Lỗi tự động cập nhật giao dịch:', e);
        } finally {
            isFetching = false;
            if (refreshIcon) refreshIcon.style.animation = '';
            if (statusText) statusText.innerHTML = 'Tự động làm mới: <strong>5s</strong>';
        }
    }

    function manualRefresh() {
        fetchBankData(true);
    }

    // Polling tự động mỗi 5 giây khi người dùng đang mở tab
    setInterval(() => {
        if (!document.hidden) {
            fetchBankData(false);
        }
    }, 5000);

    // Khi người dùng chuyển tab và quay lại, tự động đồng bộ ngay
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) {
            fetchBankData(false);
        }
    });
</script>
<style>
    @keyframes spin {
        from { transform: rotate(0deg); }
        to { transform: rotate(360deg); }
    }
</style>
</body>
</html>
