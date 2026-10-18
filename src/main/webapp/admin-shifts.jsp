<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Khung giờ ca làm việc | Aura Café</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-shifts.css" rel="stylesheet">
</head>
<body class="admin-body">
    <jsp:include page="/components/admin-sidebar.jsp">
        <jsp:param name="activeMenu" value="shifts" />
    </jsp:include>

    <main class="admin-main-wrapper shift-page">
        <c:if test="${not empty shiftSuccessMsg}">
            <div class="alert alert-success alert-dismissible fade show shift-alert" role="alert">
                <span class="material-symbols-outlined">check_circle</span>
                <span><c:out value="${shiftSuccessMsg}" /></span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
            </div>
        </c:if>
        <c:if test="${not empty shiftErrorMsg}">
            <div class="alert alert-danger alert-dismissible fade show shift-alert" role="alert">
                <span class="material-symbols-outlined">error</span>
                <span><c:out value="${shiftErrorMsg}" /></span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
            </div>
        </c:if>

        <header class="shift-page-header">
            <h1>Khung giờ ca làm việc</h1>
            <p>Có 3 ca cố định (MORNING, AFTERNOON, EVENING). Giờ của từng ca được lưu trong shop_settings.</p>
            <%-- Số lượt xếp ca tính theo tuần, cùng cách tính tuần với màn Lịch phân ca --%>
            <nav class="shift-week-nav" aria-label="Chọn tuần thống kê">
                <a class="shift-week-step" href="${ctx}/admin/shifts?date=${prevWeekDate}" title="Tuần trước">
                    <span class="material-symbols-outlined" aria-hidden="true">chevron_left</span>
                </a>
                <span class="shift-week-label">
                    Thống kê tuần <strong>${weekStartDisplay} – ${weekEndDisplay}</strong>
                    <c:if test="${isCurrentWeek}"><span class="shift-week-badge">Tuần này</span></c:if>
                </span>
                <a class="shift-week-step" href="${ctx}/admin/shifts?date=${nextWeekDate}" title="Tuần sau">
                    <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
                </a>
                <c:if test="${not isCurrentWeek}">
                    <a class="shift-week-today" href="${ctx}/admin/shifts">Về tuần này</a>
                </c:if>
            </nav>
        </header>

        <section class="shift-card-grid" aria-label="Khung giờ 3 ca làm việc">
            <c:forEach items="${shiftStats}" var="stat">
                <c:set var="shiftIcon" value="${stat.shiftName == 'MORNING' ? 'wb_twilight'
                        : (stat.shiftName == 'AFTERNOON' ? 'light_mode' : 'dark_mode')}" />
                <article class="shift-card shift-${fn:toLowerCase(stat.shiftName)}">
                    <div class="shift-card-head">
                        <span class="material-symbols-outlined shift-card-icon" aria-hidden="true">${shiftIcon}</span>
                        <div>
                            <h2>${stat.displayName}</h2>
                            <span class="shift-card-meta">${stat.shiftName} • ${stat.shiftKey}</span>
                        </div>
                    </div>

                    <div class="shift-card-time">
                        <c:choose>
                            <c:when test="${not empty stat.timeRange}"><c:out value="${stat.timeRange}" /></c:when>
                            <c:otherwise><span class="is-unset">Chưa cấu hình</span></c:otherwise>
                        </c:choose>
                    </div>

                    <dl class="shift-card-stats">
                        <div class="stat-total">
                            <dt>Tổng lượt xếp ca</dt>
                            <dd>${stat.totalCount}</dd>
                        </div>
                        <div class="stat-scheduled">
                            <dt>Đã xếp lịch</dt>
                            <dd>${stat.scheduledCount}</dd>
                        </div>
                        <div class="stat-present">
                            <dt>Có mặt</dt>
                            <dd>${stat.presentCount}</dd>
                        </div>
                        <div class="stat-completed">
                            <dt>Hoàn thành</dt>
                            <dd>${stat.completedCount}</dd>
                        </div>
                        <div class="stat-absent">
                            <dt>Vắng</dt>
                            <dd>${stat.absentCount}</dd>
                        </div>
                    </dl>

                    <div class="shift-card-footer">
                        <a href="${ctx}/admin/schedules?date=${selectedDate}" class="shift-card-link">
                            <span>Xem lịch phân ca</span>
                            <span class="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
                        </a>
                        <button type="button" class="shift-edit-button"
                                data-bs-toggle="modal" data-bs-target="#shiftEditModal"
                                data-shift="${stat.shiftName}"
                                data-key="${stat.shiftKey}"
                                data-name="${stat.displayName}"
                                data-start="${fn:escapeXml(fn:substringBefore(stat.timeRange, '-'))}"
                                data-end="${fn:escapeXml(fn:substringAfter(stat.timeRange, '-'))}">
                            <span class="material-symbols-outlined" aria-hidden="true">edit</span>
                            <span>Sửa giờ ca</span>
                        </button>
                    </div>
                </article>
            </c:forEach>
        </section>
    </main>

    <div class="modal fade" id="shiftEditModal" tabindex="-1" aria-labelledby="shiftEditTitle" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content shift-modal-content">
                <form id="shiftEditForm" method="post" action="${ctx}/admin/shifts" novalidate>
                    <input type="hidden" name="shift" id="shiftEditShift">
                    <input type="hidden" name="returnDate" value="${selectedDate}">

                    <div class="modal-header shift-modal-header">
                        <h2 class="modal-title" id="shiftEditTitle">Giờ ca</h2>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                    </div>

                    <div class="modal-body shift-modal-body">
                        <div class="row g-3">
                            <div class="col-sm-6">
                                <label class="form-label" for="shiftEditStart">Bắt đầu <span>*</span></label>
                                <input class="form-control" type="time" id="shiftEditStart" name="start" step="60" required>
                                <div class="invalid-feedback">Vui lòng nhập giờ bắt đầu.</div>
                            </div>
                            <div class="col-sm-6">
                                <label class="form-label" for="shiftEditEnd">Kết thúc <span>*</span></label>
                                <input class="form-control" type="time" id="shiftEditEnd" name="end" step="60" required>
                                <div class="invalid-feedback" id="shiftEditEndFeedback">Vui lòng nhập giờ kết thúc.</div>
                            </div>
                        </div>
                        <p class="shift-modal-note">
                            Lưu vào shop_settings.<span id="shiftEditKey"></span> dạng "HH:mm-HH:mm"
                        </p>
                    </div>

                    <div class="modal-footer shift-modal-footer">
                        <button type="button" class="shift-cancel-button" data-bs-dismiss="modal">Huỷ</button>
                        <button type="submit" class="shift-save-button">
                            <span class="material-symbols-outlined">save</span>
                            <span>Lưu giờ ca</span>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        (() => {
            const form = document.getElementById('shiftEditForm');
            const shiftInput = document.getElementById('shiftEditShift');
            const startInput = document.getElementById('shiftEditStart');
            const endInput = document.getElementById('shiftEditEnd');
            const endFeedback = document.getElementById('shiftEditEndFeedback');
            const titleLabel = document.getElementById('shiftEditTitle');
            const keyLabel = document.getElementById('shiftEditKey');

            const END_REQUIRED = 'Vui lòng nhập giờ kết thúc.';
            const END_AFTER_START = 'Giờ kết thúc phải sau giờ bắt đầu.';

            document.querySelectorAll('.shift-edit-button').forEach((button) => {
                button.addEventListener('click', () => {
                    form.classList.remove('was-validated');
                    endInput.setCustomValidity('');
                    endFeedback.textContent = END_REQUIRED;

                    shiftInput.value = button.dataset.shift;
                    titleLabel.textContent = 'Giờ ' + button.dataset.name;
                    keyLabel.textContent = button.dataset.key;
                    startInput.value = button.dataset.start;
                    endInput.value = button.dataset.end;
                });
            });

            // Kiểm tra nhanh phía trình duyệt; server vẫn kiểm tra lại toàn bộ
            form.addEventListener('submit', (event) => {
                endInput.setCustomValidity('');
                endFeedback.textContent = END_REQUIRED;

                if (startInput.value && endInput.value && endInput.value <= startInput.value) {
                    endInput.setCustomValidity(END_AFTER_START);
                    endFeedback.textContent = END_AFTER_START;
                }

                if (!form.checkValidity()) {
                    event.preventDefault();
                    event.stopPropagation();
                    const firstInvalid = form.querySelector(':invalid');
                    if (firstInvalid) {
                        firstInvalid.focus();
                    }
                }
                form.classList.add('was-validated');
            });
        })();
    </script>
</body>
</html>
