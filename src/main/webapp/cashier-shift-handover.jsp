<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${closeMode ? 'Chốt ca' : 'Bàn giao ca'} - Aura Café</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/assets/css/cashier-shift-handover.css">
</head>
<body>
    <header class="cashier-header">
        <div class="header-left">
            <a class="brand" href="${ctx}/cashier" aria-label="Aura Café POS Thu ngân">
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
                        <span>${headerShift.shiftDisplayName} (${headerShift.shiftTime})</span>
                        <span class="divider">•</span>
                        <span>${headerShift.workDateFormatted}</span>
                        <span class="divider">•</span>
                        <span>Thu ngân: <strong><c:out value="${sessionScope.user.fullName}" /></strong></span>
                    </c:when>
                    <c:otherwise>
                        <span>Chưa có ca làm việc • ${currentDate}</span>
                        <span class="divider">•</span>
                        <span>Thu ngân: <strong><c:out value="${sessionScope.user.fullName}" /></strong></span>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <nav class="cashier-nav" aria-label="Điều hướng Thu ngân">
            <a href="${ctx}/cashier">
                <span class="material-symbols-outlined">point_of_sale</span>
                <span>Tạo đơn tại quầy</span>
            </a>
            <a href="${ctx}/cashier?tab=orders">
                <span class="material-symbols-outlined">receipt_long</span>
                <span>Danh sách đơn</span>
            </a>
            <a class="${closeMode ? 'active' : ''}" href="${ctx}/cashier/handover?mode=close">
                <span class="material-symbols-outlined">calculate</span>
                <span>Chốt ca</span>
            </a>
            <a class="${not closeMode ? 'active' : ''}" href="${ctx}/cashier/handover">
                <span class="material-symbols-outlined">assignment_turned_in</span>
                <span>Bàn giao ca</span>
            </a>
        </nav>

        <div class="header-right">
            <div class="clock-block">
                <strong id="liveClock">${currentTime}</strong>
                <small>${currentDate}</small>
            </div>
            <div class="cashier-avatar" title="${sessionScope.user.fullName}">${cashierInitials}</div>
        </div>
    </header>

    <main class="handover-page">
        <c:if test="${not empty handoverSuccessMsg}">
            <div class="alert alert-success" role="status">
                <span class="material-symbols-outlined">check_circle</span>
                <c:out value="${handoverSuccessMsg}" />
            </div>
        </c:if>
        <c:if test="${not empty handoverErrorMsg}">
            <div class="alert alert-error" role="alert">
                <span class="material-symbols-outlined">error</span>
                <c:out value="${handoverErrorMsg}" />
            </div>
        </c:if>

        <c:choose>
            <c:when test="${closeMode}">
                <c:choose>
                    <c:when test="${empty activeShift}">
                        <section class="empty-state">
                            <span class="empty-icon material-symbols-outlined">event_busy</span>
                            <h1>Không có ca đang làm việc</h1>
                            <p>Bạn cần được xếp ca và check-in trước khi thực hiện chốt két.</p>
                            <a class="primary-button" href="${ctx}/cashier">
                                <span class="material-symbols-outlined">arrow_back</span>
                                Về màn hình POS
                            </a>
                        </section>
                    </c:when>
                    <c:when test="${activeShift.status ne 'PRESENT'}">
                        <section class="empty-state">
                            <span class="empty-icon material-symbols-outlined">pending_actions</span>
                            <h1>Ca làm việc chưa bắt đầu</h1>
                            <p>Ca ${activeShift.shiftDisplayName} chưa check-in nên chưa thể chốt két.</p>
                            <a class="primary-button" href="${ctx}/cashier">
                                <span class="material-symbols-outlined">arrow_back</span>
                                Về màn hình POS
                            </a>
                        </section>
                    </c:when>
                    <c:otherwise>
                        <section class="content-shell close-shift-shell">
                            <div class="page-heading">
                                <div>
                                    <span class="eyebrow">KIỂM ĐẾM CUỐI CA</span>
                                    <h1>Chốt ca ${activeShift.shiftDisplayName}</h1>
                                    <p>${activeShift.workDateDisplay} • ${activeShift.shiftTime} • Check-in ${activeShift.checkInFormatted}</p>
                                </div>
                                <span class="status-chip status-present"><span></span>Đang làm việc</span>
                            </div>

                            <div class="summary-grid">
                                <article class="summary-card">
                                    <span class="summary-icon material-symbols-outlined">account_balance_wallet</span>
                                    <div>
                                        <span>Tiền đầu ca</span>
                                        <strong><fmt:formatNumber value="${closePreview.startingCash}" pattern="#,##0" />đ</strong>
                                    </div>
                                </article>
                                <article class="summary-card">
                                    <span class="summary-icon material-symbols-outlined">payments</span>
                                    <div>
                                        <span>Thu tiền mặt</span>
                                        <strong><fmt:formatNumber value="${closePreview.cashSales}" pattern="#,##0" />đ</strong>
                                    </div>
                                </article>
                                <article class="summary-card emphasized">
                                    <span class="summary-icon material-symbols-outlined">point_of_sale</span>
                                    <div>
                                        <span>Tiền két theo hệ thống</span>
                                        <strong><fmt:formatNumber value="${closePreview.expectedCash}" pattern="#,##0" />đ</strong>
                                    </div>
                                </article>
                            </div>

                            <form class="close-form" method="post" action="${ctx}/cashier/handover" id="closeShiftForm">
                                <input type="hidden" name="action" value="close-shift">
                                <input type="hidden" name="scheduleId" value="${activeShift.id}">

                                <div class="form-section-heading">
                                    <span class="material-symbols-outlined">fact_check</span>
                                    <div>
                                        <h2>Xác nhận tiền két thực tế</h2>
                                        <p>Kiểm đếm toàn bộ tiền mặt trong két trước khi xác nhận.</p>
                                    </div>
                                </div>

                                <label for="endingCashActual">Tiền két thực tế (VNĐ) <b>*</b></label>
                                <div class="money-input">
                                    <span class="material-symbols-outlined">payments</span>
                                    <input id="endingCashActual" name="endingCashActual" type="number"
                                           min="0" max="9999999999" step="1000" inputmode="numeric"
                                           placeholder="Ví dụ: 1250000" required>
                                    <span>VNĐ</span>
                                </div>
                                <small class="field-hint">Số tiền phải là bội số của 1.000đ.</small>

                                <label for="handoverNote">Ghi chú bàn giao</label>
                                <textarea id="handoverNote" name="note" maxlength="500" rows="4"
                                          placeholder="Ghi nhận tình trạng két, chứng từ hoặc nội dung cần lưu ý..."></textarea>
                                <div class="note-meta"><span>Không bắt buộc</span><span id="noteCounter">0/500</span></div>

                                <div class="form-actions">
                                    <a class="secondary-button" href="${ctx}/cashier">Hủy</a>
                                    <button class="primary-button" type="submit">
                                        <span class="material-symbols-outlined">lock</span>
                                        Xác nhận chốt ca
                                    </button>
                                </div>
                            </form>
                        </section>
                    </c:otherwise>
                </c:choose>
            </c:when>

            <c:when test="${empty completedShift}">
                <section class="empty-state">
                    <span class="empty-icon material-symbols-outlined">assignment_late</span>
                    <h1>Chưa có ca nào được chốt</h1>
                    <p>Hãy chốt ca và kiểm đếm két trước khi bàn giao.</p>
                    <a class="primary-button" href="${ctx}/cashier/handover?mode=close">Đến màn hình chốt ca</a>
                </section>
            </c:when>

            <c:otherwise>
                <section class="content-shell handover-shell">
                    <div class="page-heading">
                        <div>
                            <span class="eyebrow">BIÊN BẢN BÀN GIAO CA</span>
                            <h1>${completedShift.shiftDisplayName} đã được chốt</h1>
                            <p>${completedShift.workDateDisplay} • ${completedShift.shiftTime}</p>
                        </div>
                        <span class="status-chip status-completed"><span></span>Đã hoàn thành</span>
                    </div>

                    <div class="handover-person">
                        <div class="cashier-avatar large">${cashierInitials}</div>
                        <div>
                            <span>Thu ngân bàn giao</span>
                            <strong><c:out value="${completedShift.userName}" /></strong>
                        </div>
                        <div class="time-range">
                            <span>${completedShift.checkInFormatted}</span>
                            <i></i>
                            <span>${completedShift.checkOutFormatted}</span>
                        </div>
                    </div>

                    <div class="summary-grid four-columns">
                        <article class="summary-card">
                            <span>Tiền đầu ca</span>
                            <strong><fmt:formatNumber value="${handoverSummary.startingCash}" pattern="#,##0" />đ</strong>
                        </article>
                        <article class="summary-card">
                            <span>Doanh thu tiền mặt</span>
                            <strong><fmt:formatNumber value="${handoverSummary.cashSales}" pattern="#,##0" />đ</strong>
                        </article>
                        <article class="summary-card">
                            <span>Doanh thu không tiền mặt</span>
                            <strong><fmt:formatNumber value="${handoverSummary.nonCashSales}" pattern="#,##0" />đ</strong>
                        </article>
                        <article class="summary-card">
                            <span>Đơn thanh toán thành công</span>
                            <strong>${handoverSummary.successfulOrderCount} đơn</strong>
                        </article>
                    </div>

                    <div class="reconciliation-card">
                        <div>
                            <span>Tiền két theo hệ thống</span>
                            <strong><fmt:formatNumber value="${handoverSummary.expectedCash}" pattern="#,##0" />đ</strong>
                        </div>
                        <span class="material-symbols-outlined">compare_arrows</span>
                        <div>
                            <span>Tiền két thực tế</span>
                            <strong><fmt:formatNumber value="${handoverSummary.actualCash}" pattern="#,##0" />đ</strong>
                        </div>
                        <div class="difference ${handoverSummary.differenceType}">
                            <span>Chênh lệch</span>
                            <strong><fmt:formatNumber value="${handoverSummary.cashDifference}" pattern="#,##0" />đ</strong>
                        </div>
                    </div>

                    <c:if test="${not empty completedShift.note}">
                        <div class="handover-note">
                            <span class="material-symbols-outlined">sticky_note_2</span>
                            <div><strong>Ghi chú bàn giao</strong><p><c:out value="${completedShift.note}" /></p></div>
                        </div>
                    </c:if>

                    <div class="handover-actions">
                        <a class="secondary-button" href="${ctx}/cashier">Về màn hình POS</a>
                        <button class="primary-button" type="button" onclick="window.print()">
                            <span class="material-symbols-outlined">print</span>
                            In biên bản
                        </button>
                    </div>
                </section>
            </c:otherwise>
        </c:choose>
    </main>

    <script>
        (() => {
            const clock = document.getElementById('liveClock');
            const updateClock = () => {
                const now = new Date();
                clock.textContent = [now.getHours(), now.getMinutes(), now.getSeconds()]
                    .map(value => String(value).padStart(2, '0')).join(':');
            };
            updateClock();
            setInterval(updateClock, 1000);

            const note = document.getElementById('handoverNote');
            const counter = document.getElementById('noteCounter');
            if (note && counter) {
                note.addEventListener('input', () => counter.textContent = note.value.length + '/500');
            }

            const form = document.getElementById('closeShiftForm');
            if (form) {
                form.addEventListener('submit', event => {
                    if (!window.confirm('Xác nhận chốt ca? Sau thao tác này, số liệu bàn giao sẽ được khóa.')) {
                        event.preventDefault();
                    }
                });
            }
        })();
    </script>
</body>
</html>
