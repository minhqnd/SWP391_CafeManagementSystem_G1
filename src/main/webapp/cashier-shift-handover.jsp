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

    <!-- Thanh header dùng chung cho các màn Thu ngân -->
    <jsp:include page="/components/cashier-header.jsp">
        <jsp:param name="active" value="${closeMode ? 'close' : 'handover'}" />
    </jsp:include>

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
                            • ${headerShift.positionDisplayName}
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

                            <!-- RIGHT COLUMN: CÁC TRƯỜNG LƯU TRONG WORK_SCHEDULES -->
                            <div class="pos-card">
                                <div class="pos-card-header">
                                    <h3 class="pos-card-title">
                                        <span class="material-symbols-outlined">fact_check</span>
                                        Xác nhận số liệu cuối ca
                                    </h3>
                                </div>

                                <form method="post" action="${ctx}/cashier/handover" id="closeShiftForm">
                                    <input type="hidden" name="action" value="close-shift">
                                    <input type="hidden" name="scheduleId" value="${activeShift.id}">

                                    <label class="database-field-label" for="endingCashActual">
                                        Tiền mặt thực tế cuối ca
                                        <span>shift_handovers.actual_cash</span>
                                    </label>
                                    <div class="cash-input-wrap">
                                        <input type="number" name="endingCashActual" id="endingCashActual"
                                               class="cash-actual-input" min="0" max="9999999999" step="1000"
                                               value="0" required inputmode="numeric">
                                        <span>đ</span>
                                    </div>
                                    <p class="field-help">Nhập tổng tiền đã kiểm đếm, theo bội số 1.000đ.</p>

                                    <div id="diffBox" class="diff-status-box diff-shortage">
                                        <span id="diffLabel">Thiếu tiền</span>
                                        <span id="diffAmount" class="diff-amount">
                                            -<fmt:formatNumber value="${closePreview.expectedCash}" pattern="#,##0" />đ
                                        </span>
                                    </div>

                                    <label class="database-field-label" for="handoverNote">
                                        Ghi chú bàn giao
                                        <span>shift_handovers.note</span>
                                    </label>
                                    <textarea name="note" id="handoverNote" class="note-input-field"
                                              placeholder="Nhập ghi chú nếu có..." maxlength="500" rows="4"></textarea>
                                    <p class="field-help" id="noteRequiredHelp" hidden>Két đang lệch so với hệ thống, bắt buộc ghi chú giải trình.</p>

                                    <button type="submit" class="btn-submit-close">
                                        <span class="material-symbols-outlined">verified_user</span>
                                        Chốt ca và gửi biên bản cho Admin
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
                                        Ngày ${completedShift.workDateDisplay}
                                        • Giờ vào ca: ${completedShift.checkInFormatted} 
                                        • Giờ ra ca: ${completedShift.checkOutFormatted}
                                        • Biên bản: <strong>${not empty handoverRecord ? handoverRecord.statusLabel : 'Chưa lập biên bản'}</strong>
                                    </p>
                                </div>
                                <div class="handover-actions-top">
                                    <a href="${ctx}/cashier" class="btn-action-primary">
                                        <span class="material-symbols-outlined">point_of_sale</span> Về POS
                                    </a>
                                </div>
                            </div>

                            <!-- Trạng thái duyệt biên bản (shift_handovers.status) -->
                            <c:if test="${not empty handoverRecord}">
                                <div class="handover-review-box review-${handoverRecord.status}">
                                    <div class="handover-review-head">
                                        <span class="material-symbols-outlined">
                                            <c:choose>
                                                <c:when test="${handoverRecord.status eq 'CONFIRMED'}">verified</c:when>
                                                <c:when test="${handoverRecord.status eq 'REJECTED'}">cancel</c:when>
                                                <c:otherwise>hourglass_top</c:otherwise>
                                            </c:choose>
                                        </span>
                                        <strong>${handoverRecord.statusLabel}</strong>
                                        <span class="handover-review-meta">
                                            Gửi lúc ${handoverRecord.createdAtDisplay}
                                            <c:if test="${not empty handoverRecord.reviewerName}">
                                                • Người duyệt: <c:out value="${handoverRecord.reviewerName}" /> (${handoverRecord.reviewedAtDisplay})
                                            </c:if>
                                        </span>
                                    </div>
                                    <c:if test="${not empty handoverRecord.reviewNote}">
                                        <p class="handover-review-note">
                                            <strong>${handoverRecord.rejected ? 'Lý do từ chối' : 'Ý kiến Admin'}:</strong>
                                            <c:out value="${handoverRecord.reviewNote}" />
                                        </p>
                                    </c:if>
                                    <c:if test="${handoverRecord.rejected}">
                                        <form method="post" action="${ctx}/cashier/handover" class="handover-resubmit-form">
                                            <input type="hidden" name="action" value="resubmit">
                                            <input type="hidden" name="handoverId" value="${handoverRecord.id}">
                                            <label for="explanation">Giải trình và gửi lại cho Admin</label>
                                            <textarea name="explanation" id="explanation" class="note-input-field" rows="3"
                                                      maxlength="500" required placeholder="Nhập giải trình về khoản chênh lệch..."><c:out value="${handoverRecord.note}" /></textarea>
                                            <button type="submit" class="btn-action-primary">
                                                <span class="material-symbols-outlined">send</span> Gửi lại biên bản
                                            </button>
                                        </form>
                                    </c:if>
                                </div>
                            </c:if>

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
                                                <c:if test="${handoverSummary.cashDifference > 0}">+</c:if>
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

                                <!-- Right: fields backed by work_schedules/users -->
                                <div class="handover-parties-card">
                                    <div class="parties-title">THÔNG TIN CA ĐÃ LƯU</div>

                                    <div class="party-row">
                                        <div class="party-avatar" style="background: var(--aura-maroon); color: #fff;">
                                            ${cashierInitials}
                                        </div>
                                        <div class="party-info">
                                            <div class="party-role">Người bàn giao (Thu ngân)</div>
                                            <div class="party-name"><c:out value="${completedShift.userName}" /></div>
                                        </div>
                                    </div>

                                    <div class="handover-record-list">
                                        <div><span>Mã ca</span><strong>#${completedShift.id}</strong></div>
                                        <div><span>Loại ca</span><strong>${completedShift.shiftDisplayName}</strong></div>
                                        <div><span>Ngày làm</span><strong>${completedShift.workDateDisplay}</strong></div>
                                        <div><span>Giờ vào / ra</span><strong>${completedShift.checkInFormatted} – ${completedShift.checkOutFormatted}</strong></div>
                                        <div><span>Trạng thái</span><strong>${completedShift.statusDisplayName}</strong></div>
                                    </div>

                                    <%-- Ưu tiên ghi chú/giải trình mới nhất trong biên bản (shift_handovers.note) --%>
                                    <c:set var="handoverNoteText" value="${not empty handoverRecord ? handoverRecord.note : completedShift.note}" />
                                    <c:if test="${not empty handoverNoteText}">
                                        <div class="handover-note-box">
                                            <strong>Ghi chú bàn giao:</strong>
                                            <p><c:out value="${handoverNoteText}" /></p>
                                        </div>
                                    </c:if>
                                </div>
                            </div>

                        </div>
                    </c:otherwise>
                </c:choose>
            </c:otherwise>
        </c:choose>

        <!-- Bottom Shift History Switcher -->
        <c:if test="${not closeMode and not empty completedShift}">
            <div class="shift-history-toolbar">
                <div class="shift-history-label">
                    <span class="material-symbols-outlined">history</span>
                    <span>Lịch sử các ca gần đây của bạn:</span>
                </div>
                <select class="shift-history-select" onchange="window.location.href='${ctx}/cashier/handover?mode=${closeMode ? 'close' : 'handover'}&amp;scheduleId=' + this.value">
                    <c:forEach items="${recentShifts}" var="rs">
                        <c:if test="${rs.status eq 'COMPLETED' and not empty rs.endingCashActual and not empty rs.endingCashSystem}">
                            <option value="${rs.id}" ${headerShift.id == rs.id ? 'selected' : ''}>
                                ${rs.workDateDisplay} - ${rs.shiftDisplayName} (Đã hoàn thành)
                            </option>
                        </c:if>
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

        // 2. Reconcile the database-backed ending_cash_actual value.
        const configEl = document.getElementById('handoverConfig');
        const EXPECTED_CASH = configEl ? (parseFloat(configEl.getAttribute('data-expected-cash')) || 0) : 0;
        const actualCashInput = document.getElementById('endingCashActual');
        const diffBox = document.getElementById('diffBox');
        const diffLabel = document.getElementById('diffLabel');
        const diffAmount = document.getElementById('diffAmount');

        const formatVND = num => num.toLocaleString('vi-VN') + 'đ';

        const noteInput = document.getElementById('handoverNote');
        const noteRequiredHelp = document.getElementById('noteRequiredHelp');

        function recalculateCash() {
            const actual = actualCashInput ? (parseInt(actualCashInput.value, 10) || 0) : 0;
            const diff = actual - EXPECTED_CASH;
            const absDiff = Math.abs(diff);

            // Lệch két thì bắt buộc ghi chú (server cũng kiểm tra lại)
            if (noteInput) noteInput.required = diff !== 0;
            if (noteRequiredHelp) noteRequiredHelp.hidden = diff === 0;

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

        if (actualCashInput) {
            actualCashInput.addEventListener('input', () => {
                if (Number(actualCashInput.value) < 0) actualCashInput.value = 0;
                recalculateCash();
            });
            recalculateCash();
        }

        const form = document.getElementById('closeShiftForm');
        if (form) {
            form.addEventListener('submit', e => {
                const actual = actualCashInput ? (parseInt(actualCashInput.value, 10) || 0) : 0;
                if (actual === 0 && EXPECTED_CASH > 0) {
                    if (!confirm('Số tiền thực tế đang là 0đ. Bạn có chắc chắn muốn hoàn tất bàn giao không?')) {
                        e.preventDefault();
                        return;
                    }
                } else {
                    if (!confirm('Xác nhận chốt ca và gửi biên bản bàn giao cho Admin duyệt?')) {
                        e.preventDefault();
                        return;
                    }
                }
            });
        }
    })();
    </script>
</body>
</html>
