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
        .badge-matched {
            background-color: #ecfdf5;
            color: #059669;
            border: 1px solid #a7f3d0;
            padding: 5px 10px;
            border-radius: 6px;
            font-weight: 700;
            font-size: 0.78rem;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            white-space: nowrap;
        }
        .badge-duplicate {
            background-color: #fffbeb;
            color: #d97706;
            border: 1px solid #fde68a;
            padding: 5px 10px;
            border-radius: 6px;
            font-weight: 700;
            font-size: 0.78rem;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            white-space: nowrap;
        }
        .badge-unmatched {
            background-color: #f1f5f9;
            color: #64748b;
            border: 1px solid #e2e8f0;
            padding: 5px 10px;
            border-radius: 6px;
            font-weight: 600;
            font-size: 0.78rem;
            display: inline-flex;
            align-items: center;
            white-space: nowrap;
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
    <header class="orders-overview">
        <div>
            <span class="table-ref">Tích hợp: SePay Bank Hub · Cloudflare Worker Webhooks</span>
            <h1>Giao dịch ngân hàng (SePay)</h1>
            <p>Nhật ký biến động số dư thực tế từ tài khoản ngân hàng quán cà phê và đối soát đơn hàng.</p>
        </div>
        <div>
            <button type="button" class="btn btn-outline-dark d-flex align-items-center gap-1 py-2 px-3 fw-semibold" style="border-radius: 10px;" onclick="location.reload()">
                <span class="material-symbols-outlined fs-5">refresh</span>
                <span>Làm mới dữ liệu</span>
            </button>
        </div>
    </header>

    <!-- Thống kê biến động số dư -->
    <div class="row g-3 mb-4">
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
                    <div class="bank-stat-title">Tiền ra</div>
                    <div class="bank-stat-val text-secondary">-0 đ</div>
                </div>
                <div class="bank-icon-circle bg-danger bg-opacity-10 text-danger">
                    <span class="material-symbols-outlined">arrow_upward</span>
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
    <section class="order-table-card">
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
                        <th style="width: 180px;">Đối soát hệ thống</th>
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
                                    <td class="text-nowrap">
                                        <c:choose>
                                            <c:when test="${tx.reconcileStatus eq 'MATCHED'}">
                                                <span class="badge-matched" title="Đã khớp và thanh toán đơn hàng">
                                                    <span class="material-symbols-outlined" style="font-size: 15px;">check_circle</span>
                                                    Khớp đơn #${tx.matchedOrderCode}
                                                </span>
                                            </c:when>
                                            <c:when test="${tx.reconcileStatus eq 'DUPLICATE'}">
                                                <span class="badge-duplicate" title="Khách chuyển thừa hoặc chuyển trùng lần 2 cho cùng 1 đơn">
                                                    <span class="material-symbols-outlined" style="font-size: 15px;">warning</span>
                                                    Chuyển thừa #${tx.matchedOrderCode}
                                                </span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge-unmatched">
                                                    Chưa gắn đơn
                                                </span>
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
</body>
</html>
