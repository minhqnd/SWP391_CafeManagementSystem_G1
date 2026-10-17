<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${closeMode ? 'Chốt ca làm việc' : 'Biên bản bàn giao ca'} | Aura Café POS</title>
    
    <!-- Google Fonts & Material Symbols -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    
    <!-- Custom CSS -->
    <link rel="stylesheet" href="${ctx}/assets/css/cashier-shift-handover.css">
</head>
<body>

    <!-- ========================================================
         TOP NAVBAR
         ======================================================== -->
    <header class="cashier-header">
        <div class="header-left">
            <a class="brand" href="${ctx}/cashier" title="Quay lại POS">
                <span class="brand-mark material-symbols-outlined">coffee</span>
                <span class="brand-copy">
                    <strong>AURA CAFÉ</strong>
                    <small>POS THU NGÂN</small>
                </span>
            </a>

            <div class="shift-pill">
                <span class="material-symbols-outlined">schedule</span>
                <c:choose>
                    <c:when test="${not empty headerShift}">
                        <span>${headerShift.shiftDisplayName} (${headerShift.shiftTime}) - ${headerShift.workDateFormatted}</span>
                        <span class="divider">•</span>
                        <span>Thu ngân: <strong><c:out value="${sessionScope.user.fullName}" /></strong></span>
                    </c:when>
                    <c:otherwise>
                        <span>Chưa có ca làm việc - ${currentDate}</span>
                        <span class="divider">•</span>
                        <span>Thu ngân: <strong><c:out value="${sessionScope.user.fullName}" /></strong></span>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Center Nav: Chốt ca & Bàn giao ca liên kết -->
        <nav class="cashier-nav" aria-label="Điều hướng thu ngân">
            <a href="${ctx}/cashier">
                <span class="material-symbols-outlined">point_of_sale</span>
                <span>Tạo đơn tại quầy</span>
            </a>
            <a href="${ctx}/cashier?tab=orders">
                <span class="material-symbols-outlined">receipt_long</span>
                <span>Danh sách đơn</span>
            </a>
            <a class="${closeMode ? 'active' : ''}" 
               href="${ctx}/cashier/handover?mode=close<c:if test='${not empty headerShift}'>&scheduleId=${headerShift.id}</c:if>">
                <span class="material-symbols-outlined">calculate</span>
                <span>Chốt ca</span>
            </a>
            <a class="${not closeMode ? 'active' : ''}" 
               href="${ctx}/cashier/handover?mode=handover<c:if test='${not empty headerShift}'>&scheduleId=${headerShift.id}</c:if>">
                <span class="material-symbols-outlined">assignment_turned_in</span>
                <span>Bàn giao ca</span>
            </a>
        </nav>

        <div class="header-right">
            <div class="clock-block">
                <strong id="liveClock">${currentTime}</strong>
                <small>${currentDate}</small>
            </div>
            <div class="cashier-avatar" title="${sessionScope.user.fullName}">
                ${cashierInitials}
            </div>
        </div>
    </header>

    <!-- ========================================================
         MAIN CONTENT CONTAINER
         ======================================================== -->
    <main class="handover-page">

        <!-- Flash Messages -->
        <c:if test="${not empty handoverSuccessMsg}">
            <div class="alert alert-success">
                <span class="material-symbols-outlined">check_circle</span>
                <span><c:out value="${handoverSuccessMsg}" /></span>
            </div>
        </c:if>
        <c:if test="${not empty handoverErrorMsg}">
            <div class="alert alert-error">
                <span class="material-symbols-outlined">error</span>
                <span><c:out value="${handoverErrorMsg}" /></span>
            </div>
        </c:if>

        <c:if test="${not empty headerShift}">
            <!-- Top Shift Info Banner Card Matching Mockup -->
            <div class="shift-banner-card">
                <div class="shift-banner-left">
                    <div class="shift-banner-icon">
                        <span class="material-symbols-outlined">calendar_today</span>
                    </div>
                    <div>
                        <div class="shift-banner-title-line">
                            <span class="shift-banner-title">${headerShift.shiftDisplayName} (${headerShift.shiftTime})</span>
                            <c:choose>
                                <c:when test="${headerShift.status eq 'PRESENT'}">
                                    <span class="badge-present">Có mặt</span>
                                </c:when>
                                <c:when test="${headerShift.status eq 'COMPLETED'}">
                                    <span class="badge-completed">Đã hoàn thành</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge-id">Đã xếp lịch</span>
                                </c:otherwise>
                            </c:choose>
                            <span class="badge-id">Lịch #${headerShift.id}</span>
                        </div>
                        <div class="shift-banner-sub">
                            Ngày: ${headerShift.workDateFormatted} 
                            <c:if test="${not empty headerShift.checkInFormatted}">
                                • Vào ca: ${headerShift.checkInFormatted}
                            </c:if>
                            • Cashier shift
                        </div>
                    </div>
                </div>

                <div class="shift-banner-metrics">
                    <div class="banner-metric-item">
                        <div class="banner-metric-label">TIỀN ĐẦU CA</div>
                        <div class="banner-metric-value">
                            <fmt:formatNumber value="${headerShift.startingCash}" pattern="#,##0" />đ
                        </div>
                    </div>
                    <div class="banner-metric-item">
                        <div class="banner-metric-label">GIAO DỊCH TRONG CA</div>
                        <div class="banner-metric-value">
                            <c:choose>
                                <c:when test="${closeMode}">
                                    ${closePreview.successfulOrderCount}
                                </c:when>
                                <c:otherwise>
                                    ${handoverSummary.successfulOrderCount}
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </div>
        </c:if>

        <!-- ========================================================
             BRANCH 1: MÀN CHỐT CA (closeMode == true)
             ======================================================== -->
        <c:choose>
            <c:when test="${closeMode}">
                <c:choose>
                    <c:when test="${empty activeShift}">
                        <div class="empty-handover-box">
                            <span class="material-symbols-outlined">event_busy</span>
                            <h2>Không có ca nào đang mở để chốt két</h2>
                            <p>Tất cả ca làm việc của bạn đã được chốt hoặc bạn chưa bắt đầu ca mới.</p>
                            <div class="d-flex justify-content-center gap-2">
                                <a href="${ctx}/cashier" class="btn-action-primary">Về màn hình POS</a>
                                <a href="${ctx}/cashier/handover?mode=handover" class="btn-action-outline">Xem biên bản ca đã bàn giao</a>
                            </div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <!-- Two-Column Closing Shift Layout Matching Mockup -->
                        <div class="shift-close-grid">
                            
                            <!-- LEFT COLUMN: DOANH THU & TIỀN MẶT HỆ THỐNG -->
                            <div class="pos-card">
                                <div class="pos-card-header">
                                    <h3 class="pos-card-title">
                                        <span class="material-symbols-outlined">payments</span>
                                        Doanh thu theo phương thức thanh toán
                                    </h3>
                                </div>

                                <!-- Big Highlight Revenue Banner -->
                                <div class="total-revenue-banner">
                                    <div class="total-revenue-label">TỔNG DOANH THU ĐÃ THU</div>
                                    <div class="total-revenue-amount">
                                        <fmt:formatNumber value="${closePreview.totalSales}" pattern="#,##0" />đ
                                    </div>
                                </div>

                                <!-- Payment Breakdown Table -->
                                <table class="payment-breakdown-table">
                                    <thead>
                                        <tr>
                                            <th style="text-align: left;">PHƯƠNG THỨC</th>
                                            <th style="text-align: center;">SỐ GIAO DỊCH</th>
                                            <th style="text-align: right;">SỐ TIỀN</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr>
                                            <td>
                                                <div class="payment-method-cell">
                                                    <span class="material-symbols-outlined">payments</span>
                                                    <span>Tiền mặt</span>
                                                </div>
                                            </td>
                                            <td class="payment-count-cell">${closePreview.cashCount}</td>
                                            <td class="payment-amount-cell">
                                                <fmt:formatNumber value="${closePreview.cashSales}" pattern="#,##0" />đ
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <div class="payment-method-cell">
                                                    <span class="material-symbols-outlined">account_balance</span>
                                                    <span>Chuyển khoản</span>
                                                </div>
                                            </td>
                                            <td class="payment-count-cell">${closePreview.bankCount}</td>
                                            <td class="payment-amount-cell">
                                                <fmt:formatNumber value="${closePreview.bankSales}" pattern="#,##0" />đ
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <div class="payment-method-cell">
                                                    <span class="material-symbols-outlined">qr_code_scanner</span>
                                                    <span>VNPAY</span>
                                                </div>
                                            </td>
                                            <td class="payment-count-cell">${closePreview.vnpayCount}</td>
                                            <td class="payment-amount-cell">
                                                <fmt:formatNumber value="${closePreview.vnpaySales}" pattern="#,##0" />đ
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <div class="payment-method-cell">
                                                    <span class="material-symbols-outlined">account_balance_wallet</span>
                                                    <span>Ví MoMo</span>
                                                </div>
                                            </td>
                                            <td class="payment-count-cell">${closePreview.momoCount}</td>
                                            <td class="payment-amount-cell">
                                                <fmt:formatNumber value="${closePreview.momoSales}" pattern="#,##0" />đ
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>

                                <!-- Sub-box: Tiền mặt hệ thống (ENDING_CASH_SYSTEM) -->
                                <div class="system-cash-box">
                                    <div class="system-cash-title">TIỀN MẶT HỆ THỐNG (ENDING_CASH_SYSTEM)</div>
                                    <div class="system-cash-grid">
                                        <div class="system-cash-item">
                                            <div class="system-cash-label">Tiền đầu ca</div>
                                            <div class="system-cash-val">
                                                <fmt:formatNumber value="${closePreview.startingCash}" pattern="#,##0" />đ
                                            </div>
                                        </div>
                                        <div class="system-cash-item">
                                            <div class="system-cash-label">+ Thu tiền mặt</div>
                                            <div class="system-cash-val val-green">
                                                <fmt:formatNumber value="${closePreview.cashSales}" pattern="#,##0" />đ
                                            </div>
                                        </div>
                                        <div class="system-cash-item">
                                            <div class="system-cash-label">= Phải có trong két</div>
                                            <div class="system-cash-val val-maroon">
                                                <fmt:formatNumber value="${closePreview.expectedCash}" pattern="#,##0" />đ
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- RIGHT COLUMN: KIỂM ĐẾM TIỀN MẶT ĐÓNG CA -->
                            <div class="pos-card">
                                <div class="pos-card-header">
                                    <h3 class="pos-card-title">
                                        Kiểm đếm tiền mặt đóng ca
                                    </h3>
                                    <button type="button" class="btn-reset-denoms" onclick="resetDenominations()">
                                        <span class="material-symbols-outlined" style="font-size: 16px;">refresh</span>
                                        Nhập lại
                                    </button>
                                </div>

                                <!-- Denomination list (9 rows: 500k to 1k) -->
                                <div class="denomination-list">
                                    <div class="denom-row" data-denom="500000">
                                        <div class="denom-label">500.000đ</div>
                                        <div class="denom-controls">
                                            <button type="button" class="btn-denom-qty btn-minus">-</button>
                                            <input type="number" class="denom-input" min="0" max="9999" value="0">
                                            <button type="button" class="btn-denom-qty btn-plus">+</button>
                                        </div>
                                        <div class="denom-subtotal">0đ</div>
                                    </div>

                                    <div class="denom-row" data-denom="200000">
                                        <div class="denom-label">200.000đ</div>
                                        <div class="denom-controls">
                                            <button type="button" class="btn-denom-qty btn-minus">-</button>
                                            <input type="number" class="denom-input" min="0" max="9999" value="0">
                                            <button type="button" class="btn-denom-qty btn-plus">+</button>
                                        </div>
                                        <div class="denom-subtotal">0đ</div>
                                    </div>

                                    <div class="denom-row" data-denom="100000">
                                        <div class="denom-label">100.000đ</div>
                                        <div class="denom-controls">
                                            <button type="button" class="btn-denom-qty btn-minus">-</button>
                                            <input type="number" class="denom-input" min="0" max="9999" value="0">
                                            <button type="button" class="btn-denom-qty btn-plus">+</button>
                                        </div>
                                        <div class="denom-subtotal">0đ</div>
                                    </div>

                                    <div class="denom-row" data-denom="50000">
                                        <div class="denom-label">50.000đ</div>
                                        <div class="denom-controls">
                                            <button type="button" class="btn-denom-qty btn-minus">-</button>
                                            <input type="number" class="denom-input" min="0" max="9999" value="0">
                                            <button type="button" class="btn-denom-qty btn-plus">+</button>
                                        </div>
                                        <div class="denom-subtotal">0đ</div>
                                    </div>

                                    <div class="denom-row" data-denom="20000">
                                        <div class="denom-label">20.000đ</div>
                                        <div class="denom-controls">
                                            <button type="button" class="btn-denom-qty btn-minus">-</button>
                                            <input type="number" class="denom-input" min="0" max="9999" value="0">
                                            <button type="button" class="btn-denom-qty btn-plus">+</button>
                                        </div>
                                        <div class="denom-subtotal">0đ</div>
                                    </div>

                                    <div class="denom-row" data-denom="10000">
                                        <div class="denom-label">10.000đ</div>
                                        <div class="denom-controls">
                                            <button type="button" class="btn-denom-qty btn-minus">-</button>
                                            <input type="number" class="denom-input" min="0" max="9999" value="0">
                                            <button type="button" class="btn-denom-qty btn-plus">+</button>
                                        </div>
                                        <div class="denom-subtotal">0đ</div>
                                    </div>

                                    <div class="denom-row" data-denom="5000">
                                        <div class="denom-label">5.000đ</div>
                                        <div class="denom-controls">
                                            <button type="button" class="btn-denom-qty btn-minus">-</button>
                                            <input type="number" class="denom-input" min="0" max="9999" value="0">
                                            <button type="button" class="btn-denom-qty btn-plus">+</button>
                                        </div>
                                        <div class="denom-subtotal">0đ</div>
                                    </div>

                                    <div class="denom-row" data-denom="2000">
                                        <div class="denom-label">2.000đ</div>
                                        <div class="denom-controls">
                                            <button type="button" class="btn-denom-qty btn-minus">-</button>
                                            <input type="number" class="denom-input" min="0" max="9999" value="0">
                                            <button type="button" class="btn-denom-qty btn-plus">+</button>
                                        </div>
                                        <div class="denom-subtotal">0đ</div>
                                    </div>

                                    <div class="denom-row" data-denom="1000">
                                        <div class="denom-label">1.000đ</div>
                                        <div class="denom-controls">
                                            <button type="button" class="btn-denom-qty btn-minus">-</button>
                                            <input type="number" class="denom-input" min="0" max="9999" value="0">
                                            <button type="button" class="btn-denom-qty btn-plus">+</button>
                                        </div>
                                        <div class="denom-subtotal">0đ</div>
                                    </div>
                                </div>

                                <!-- Total Actual Cash Bar -->
                                <div class="actual-cash-bar">
                                    <span class="actual-cash-label">TIỀN THỰC TẾ (ENDING_CASH_ACTUAL)</span>
                                    <span id="actualCashDisplay" class="actual-cash-amount">0đ</span>
                                </div>

                                <!-- Dynamic Difference Box -->
                                <div id="diffBox" class="diff-status-box diff-shortage">
                                    <span id="diffLabel">Thiếu tiền</span>
                                    <span id="diffAmount" class="diff-amount">
                                        -<fmt:formatNumber value="${closePreview.expectedCash}" pattern="#,##0" />đ
                                    </span>
                                </div>

                                <!-- Submission Form Linking Chốt ca -> Bàn giao ca -->
                                <form method="post" action="${ctx}/cashier/handover" id="closeShiftForm">
                                    <input type="hidden" name="action" value="close-shift">
                                    <input type="hidden" name="scheduleId" value="${activeShift.id}">
                                    <input type="hidden" name="endingCashActual" id="hiddenEndingCashActual" value="0">

                                    <input type="text" name="note" id="handoverNote" class="note-input-field"
                                           placeholder="Ghi chú chốt ca (lưu vào work_schedules.note)..." maxlength="500">

                                    <button type="submit" class="btn-submit-close">
                                        <span class="material-symbols-outlined">verified_user</span>
                                        Chốt ca &amp; chuyển sang bàn giao
                                    </button>
                                </form>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </c:when>
            <c:otherwise>
                <c:choose>
                    <c:when test="${empty completedShift}">
                        <div class="empty-handover-box">
                            <span class="material-symbols-outlined">assignment_late</span>
                            <h2>Chưa có ca nào được chốt két để bàn giao</h2>
                            <p>Hãy thực hiện kiểm đếm và chốt ca trước khi xuất biên bản bàn giao.</p>
                            <a href="${ctx}/cashier/handover?mode=close" class="btn-action-primary">Đến màn hình chốt ca</a>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <!-- Shift Handover Report (Biên bản bàn giao ca) -->
                        <div class="handover-report-shell">
                            
                            <!-- Header Row with Actions -->
                            <div class="handover-header-row">
                                <div class="handover-header-title">
                                    <h2>Biên bản bàn giao ca - ${completedShift.shiftDisplayName}</h2>
                                    <p>
                                        Ngày ${completedShift.workDateFormatted} 
                                        • Giờ vào ca: ${completedShift.checkInFormatted} 
                                        • Giờ ra ca: ${completedShift.checkOutFormatted}
                                        • Trạng thái: <strong>Đã hoàn thành</strong>
                                    </p>
                                </div>
                                <div class="handover-actions-top">
                                    <button type="button" class="btn-action-outline" onclick="window.print()">
                                        <span class="material-symbols-outlined">print</span> In biên bản
                                    </button>
                                    <a href="${ctx}/cashier/handover?mode=close&scheduleId=${completedShift.id}" class="btn-action-outline" title="Xem lại số liệu kiểm đếm">
                                        <span class="material-symbols-outlined">calculate</span> Xem chốt két
                                    </a>
                                    <a href="${ctx}/cashier" class="btn-action-primary">
                                        <span class="material-symbols-outlined">point_of_sale</span> Về POS
                                    </a>
                                </div>
                            </div>

                            <!-- 5 Handover Metrics Strip -->
                            <div class="handover-summary-strip">
                                <div class="handover-strip-card">
                                    <div class="handover-strip-label">TIỀN ĐẦU CA</div>
                                    <div class="handover-strip-val">
                                        <fmt:formatNumber value="${handoverSummary.startingCash}" pattern="#,##0" />đ
                                    </div>
                                </div>
                                <div class="handover-strip-card">
                                    <div class="handover-strip-label">DOANH THU TIỀN MẶT</div>
                                    <div class="handover-strip-val" style="color: var(--success);">
                                        <fmt:formatNumber value="${handoverSummary.cashSales}" pattern="#,##0" />đ
                                    </div>
                                </div>
                                <div class="handover-strip-card">
                                    <div class="handover-strip-label">DOANH THU KHÔNG TIỀN MẶT</div>
                                    <div class="handover-strip-val">
                                        <fmt:formatNumber value="${handoverSummary.nonCashSales}" pattern="#,##0" />đ
                                    </div>
                                </div>
                                <div class="handover-strip-card">
                                    <div class="handover-strip-label">TỔNG DOANH THU</div>
                                    <div class="handover-strip-val highlight-maroon">
                                        <fmt:formatNumber value="${handoverSummary.totalSales}" pattern="#,##0" />đ
                                    </div>
                                </div>
                                <div class="handover-strip-card">
                                    <div class="handover-strip-label">ĐƠN HOÀN THÀNH</div>
                                    <div class="handover-strip-val">
                                        ${handoverSummary.successfulOrderCount} đơn
                                    </div>
                                </div>
                            </div>

                            <!-- Detail Content Grid -->
                            <div class="handover-content-grid">
                                
                                <!-- Left: Reconciliation & Payment Details -->
                                <div>
                                    <!-- Reconciliation Card -->
                                    <div class="handover-reconciliation-card">
                                        <div>
                                            <div class="reconcile-item-label">Tiền két hệ thống</div>
                                            <div class="reconcile-item-val">
                                                <fmt:formatNumber value="${handoverSummary.expectedCash}" pattern="#,##0" />đ
                                            </div>
                                        </div>
                                        <span class="material-symbols-outlined reconcile-arrow">compare_arrows</span>
                                        <div>
                                            <div class="reconcile-item-label">Tiền két thực tế</div>
                                            <div class="reconcile-item-val">
                                                <fmt:formatNumber value="${handoverSummary.actualCash}" pattern="#,##0" />đ
                                            </div>
                                        </div>
                                        <div class="reconcile-diff-tag ${handoverSummary.differenceType}">
                                            <small>
                                                <c:choose>
                                                    <c:when test="${handoverSummary.differenceType eq 'BALANCED'}">Khớp két</c:when>
                                                    <c:when test="${handoverSummary.differenceType eq 'SURPLUS'}">Thừa tiền</c:when>
                                                    <c:otherwise>Thiếu tiền</c:otherwise>
                                                </c:choose>
                                            </small>
                                            <strong>
                                                <c:choose>
                                                    <c:when test="${handoverSummary.cashDifference > 0}">+</c:when>
                                                    <c:when test="${handoverSummary.cashDifference < 0}">-</c:when>
                                                </c:choose>
                                                <fmt:formatNumber value="${handoverSummary.cashDifference}" pattern="#,##0" />đ
                                            </strong>
                                        </div>
                                    </div>

                                    <!-- Payment Methods Breakdown Table -->
                                    <div class="pos-card" style="padding: 16px;">
                                        <div class="pos-card-title" style="margin-bottom: 12px; font-size: 14px;">
                                            <span class="material-symbols-outlined">receipt_long</span>
                                            Chi tiết doanh thu theo hình thức thanh toán
                                        </div>
                                        <table class="payment-breakdown-table" style="margin-bottom: 0;">
                                            <thead>
                                                <tr>
                                                    <th style="text-align: left;">PHƯƠNG THỨC</th>
                                                    <th style="text-align: center;">SỐ GIAO DỊCH</th>
                                                    <th style="text-align: right;">SỐ TIỀN</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <tr>
                                                    <td>
                                                        <div class="payment-method-cell">
                                                            <span class="material-symbols-outlined">payments</span>
                                                            <span>Tiền mặt (CASH)</span>
                                                        </div>
                                                    </td>
                                                    <td class="payment-count-cell">${handoverSummary.cashCount}</td>
                                                    <td class="payment-amount-cell">
                                                        <fmt:formatNumber value="${handoverSummary.cashSales}" pattern="#,##0" />đ
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>
                                                        <div class="payment-method-cell">
                                                            <span class="material-symbols-outlined">account_balance</span>
                                                            <span>Chuyển khoản (BANK)</span>
                                                        </div>
                                                    </td>
                                                    <td class="payment-count-cell">${handoverSummary.bankCount}</td>
                                                    <td class="payment-amount-cell">
                                                        <fmt:formatNumber value="${handoverSummary.bankSales}" pattern="#,##0" />đ
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>
                                                        <div class="payment-method-cell">
                                                            <span class="material-symbols-outlined">qr_code_scanner</span>
                                                            <span>VNPAY QR</span>
                                                        </div>
                                                    </td>
                                                    <td class="payment-count-cell">${handoverSummary.vnpayCount}</td>
                                                    <td class="payment-amount-cell">
                                                        <fmt:formatNumber value="${handoverSummary.vnpaySales}" pattern="#,##0" />đ
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>
                                                        <div class="payment-method-cell">
                                                            <span class="material-symbols-outlined">account_balance_wallet</span>
                                                            <span>Ví MoMo</span>
                                                        </div>
                                                    </td>
                                                    <td class="payment-count-cell">${handoverSummary.momoCount}</td>
                                                    <td class="payment-amount-cell">
                                                        <fmt:formatNumber value="${handoverSummary.momoSales}" pattern="#,##0" />đ
                                                    </td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>

                                <!-- Right: Handover Parties & Note -->
                                <div class="handover-parties-card">
                                    <div class="parties-title">THÔNG TIN BÀN GIAO CA</div>
                                    
                                    <!-- Giao ca -->
                                    <div class="party-row">
                                        <div class="party-avatar" style="background: var(--aura-maroon); color: #fff;">
                                            ${cashierInitials}
                                        </div>
                                        <div class="party-info">
                                            <div class="party-role">Người bàn giao (Thu ngân)</div>
                                            <div class="party-name"><c:out value="${completedShift.userName}" /></div>
                                        </div>
                                    </div>

                                    <!-- Nhận ca -->
                                    <div class="party-row">
                                        <div class="party-avatar">
                                            <span class="material-symbols-outlined" style="font-size: 18px;">person</span>
                                        </div>
                                        <div class="party-info" style="flex: 1;">
                                            <div class="party-role">Người tiếp nhận bàn giao</div>
                                            <select class="form-select form-select-sm mt-1" style="font-size: 13px; font-weight: 600; padding: 4px 8px; border: 1px solid var(--line); border-radius: 6px; width: 100%;">
                                                <option value="">-- Chọn nhân viên ca sau / Quản lý --</option>
                                                <c:forEach items="${staffList}" var="st">
                                                    <c:if test="${st.id ne completedShift.userId}">
                                                        <option value="${st.id}"><c:out value="${st.fullName}" /> (${st.position})</option>
                                                    </c:if>
                                                </c:forEach>
                                            </select>
                                        </div>
                                    </div>

                                    <!-- Ghi chú bàn giao -->
                                    <c:if test="${not empty completedShift.note}">
                                        <div class="handover-note-box">
                                            <strong>Ghi chú lúc chốt két:</strong>
                                            <p><c:out value="${completedShift.note}" /></p>
                                        </div>
                                    </c:if>
                                </div>
                            </div>

                            <!-- Signatures for Handover Receipt -->
                            <div class="handover-signatures">
                                <div class="sig-box">
                                    <div class="sig-title">BÊN GIAO CA (Ký &amp; ghi rõ họ tên)</div>
                                    <div class="sig-name"><c:out value="${completedShift.userName}" /></div>
                                </div>
                                <div class="sig-box">
                                    <div class="sig-title">BÊN NHẬN CA (Ký &amp; ghi rõ họ tên)</div>
                                    <div class="sig-name">Người tiếp nhận</div>
                                </div>
                            </div>

                        </div>
                    </c:otherwise>
                </c:choose>
            </c:otherwise>
        </c:choose>

        <!-- Bottom Shift History Switcher -->
        <c:if test="${not empty recentShifts}">
            <div class="shift-history-toolbar">
                <div class="shift-history-label">
                    <span class="material-symbols-outlined">history</span>
                    <span>Lịch sử các ca gần đây của bạn:</span>
                </div>
                <select class="shift-history-select" onchange="window.location.href='${ctx}/cashier/handover?mode=${closeMode ? 'close' : 'handover'}&amp;scheduleId=' + this.value">
                    <c:forEach items="${recentShifts}" var="rs">
                        <option value="${rs.id}" ${headerShift.id == rs.id ? 'selected' : ''}>
                            ${rs.workDateFormatted} - ${rs.shiftDisplayName} (${rs.status eq 'COMPLETED' ? 'Đã hoàn thành' : (rs.status eq 'PRESENT' ? 'Đang có mặt' : 'Đã xếp lịch')})
                        </option>
                    </c:forEach>
                </select>
            </div>
        </c:if>

        <div id="handoverConfig" 
             data-expected-cash="${not empty closePreview ? closePreview.expectedCash : (not empty handoverSummary ? handoverSummary.expectedCash : 0)}"
             style="display: none;"></div>

    </main>

    <!-- ========================================================
         JAVASCRIPT LOGIC
         ======================================================== -->
    <script>
    (() => {
        // 1. Live Clock
        const clock = document.getElementById('liveClock');
        const updateClock = () => {
            const now = new Date();
            if (clock) {
                clock.textContent = [now.getHours(), now.getMinutes(), now.getSeconds()]
                    .map(v => String(v).padStart(2, '0')).join(':');
            }
        };
        updateClock();
        setInterval(updateClock, 1000);

        // 2. Denomination Calculation
        const configEl = document.getElementById('handoverConfig');
        const EXPECTED_CASH = configEl ? (parseFloat(configEl.getAttribute('data-expected-cash')) || 0) : 0;
        const actualDisplay = document.getElementById('actualCashDisplay');
        const hiddenActualInput = document.getElementById('hiddenEndingCashActual');
        const diffBox = document.getElementById('diffBox');
        const diffLabel = document.getElementById('diffLabel');
        const diffAmount = document.getElementById('diffAmount');

        const formatVND = num => num.toLocaleString('vi-VN') + 'đ';

        function recalculateCash() {
            let total = 0;
            document.querySelectorAll('.denom-row').forEach(row => {
                const denom = parseInt(row.getAttribute('data-denom'), 10) || 0;
                const qtyInput = row.querySelector('.denom-input');
                const qty = parseInt(qtyInput.value, 10) || 0;
                const subtotal = denom * qty;
                const subDisplay = row.querySelector('.denom-subtotal');
                if (subDisplay) subDisplay.textContent = formatVND(subtotal);
                total += subtotal;
            });

            if (actualDisplay) actualDisplay.textContent = formatVND(total);
            if (hiddenActualInput) hiddenActualInput.value = total;

            const diff = total - EXPECTED_CASH;
            const absDiff = Math.abs(diff);

            if (diffBox && diffLabel && diffAmount) {
                diffBox.className = 'diff-status-box';
                if (diff === 0) {
                    diffBox.classList.add('diff-balanced');
                    diffLabel.textContent = 'Khớp két';
                    diffAmount.textContent = '0đ';
                } else if (diff > 0) {
                    diffBox.classList.add('diff-surplus');
                    diffLabel.textContent = 'Thừa tiền';
                    diffAmount.textContent = '+' + formatVND(absDiff);
                } else {
                    diffBox.classList.add('diff-shortage');
                    diffLabel.textContent = 'Thiếu tiền';
                    diffAmount.textContent = '-' + formatVND(absDiff);
                }
            }
        }

        // Attach listeners to denomination plus/minus and input
        document.querySelectorAll('.denom-row').forEach(row => {
            const qtyInput = row.querySelector('.denom-input');
            const minusBtn = row.querySelector('.btn-minus');
            const plusBtn = row.querySelector('.btn-plus');

            if (qtyInput) {
                qtyInput.addEventListener('input', () => {
                    if (parseInt(qtyInput.value, 10) < 0) qtyInput.value = 0;
                    recalculateCash();
                });
            }
            if (minusBtn && qtyInput) {
                minusBtn.addEventListener('click', () => {
                    const cur = parseInt(qtyInput.value, 10) || 0;
                    if (cur > 0) qtyInput.value = cur - 1;
                    recalculateCash();
                });
            }
            if (plusBtn && qtyInput) {
                plusBtn.addEventListener('click', () => {
                    const cur = parseInt(qtyInput.value, 10) || 0;
                    qtyInput.value = cur + 1;
                    recalculateCash();
                });
            }
        });

        // Reset button
        window.resetDenominations = function() {
            document.querySelectorAll('.denom-input').forEach(input => input.value = 0);
            recalculateCash();
        };

        // Form Submit Confirmation
        const form = document.getElementById('closeShiftForm');
        if (form) {
            form.addEventListener('submit', e => {
                const actual = parseInt(hiddenActualInput.value, 10) || 0;
                if (actual === 0 && EXPECTED_CASH > 0) {
                    if (!confirm('Số tiền kiểm đếm đang là 0đ. Bạn có chắc chắn muốn chốt két và chuyển sang bàn giao ca không?')) {
                        e.preventDefault();
                        return;
                    }
                } else {
                    if (!confirm('Xác nhận chốt két ca làm việc này? Sau khi chốt ca, hệ thống sẽ tự động tạo biên bản bàn giao ca.')) {
                        e.preventDefault();
                        return;
                    }
                }
            });
        }

        // Initial calculation
        recalculateCash();
    })();
    </script>
</body>
</html>
