<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty sessionScope.user or sessionScope.user.role ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<c:set var="csrf" value="${fn:escapeXml(sessionScope.shiftCsrfToken)}" />
<c:set var="form" value="${shiftForm}" />

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
        <c:if test="${not empty shiftErrorMsg and empty form}">
            <div class="alert alert-danger alert-dismissible fade show shift-alert" role="alert">
                <span class="material-symbols-outlined">error</span>
                <span><c:out value="${shiftErrorMsg}" /></span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
            </div>
        </c:if>

        <header class="shift-page-header">
            <div class="shift-heading-row">
                <div>
                    <span class="shift-table-ref">Bảng: <strong>work_shifts</strong></span>
                    <h1>Khung giờ ca làm việc</h1>
                    <p>Mỗi tuần có bộ ca riêng. Sửa giờ ca của tuần này không làm thay đổi các tuần trước.</p>
                </div>
                <c:if test="${not isPastWeek}">
                    <button type="button" class="shift-add-button" id="shiftAddButton"
                            data-bs-toggle="modal" data-bs-target="#shiftEditModal">
                        <span class="material-symbols-outlined" aria-hidden="true">add</span>
                        <span>Thêm ca</span>
                    </button>
                </c:if>
            </div>

            <nav class="shift-week-nav" aria-label="Chọn tuần">
                <a class="shift-week-step" href="${ctx}/admin/shifts?date=${prevWeekDate}" title="Tuần trước">
                    <span class="material-symbols-outlined" aria-hidden="true">chevron_left</span>
                </a>
                <span class="shift-week-label">
                    Tuần <strong>${weekStartDisplay} – ${weekEndDisplay}</strong>
                    <c:if test="${isCurrentWeek}"><span class="shift-week-badge">Tuần này</span></c:if>
                    <c:if test="${isPastWeek}"><span class="shift-week-badge is-past">Đã qua · chỉ xem</span></c:if>
                </span>
                <a class="shift-week-step" href="${ctx}/admin/shifts?date=${nextWeekDate}" title="Tuần sau">
                    <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
                </a>
                <c:if test="${not isCurrentWeek}">
                    <a class="shift-week-today" href="${ctx}/admin/shifts">Về tuần này</a>
                </c:if>
            </nav>
        </header>

        <section class="shift-list-card" aria-label="Danh sách ca của tuần">
            <c:choose>
                <c:when test="${not empty shifts}">
                    <div class="table-responsive">
                        <table class="shift-table">
                            <thead>
                                <tr>
                                    <th scope="col">Tên ca</th>
                                    <th scope="col">Bắt đầu</th>
                                    <th scope="col">Kết thúc</th>
                                    <c:if test="${not isPastWeek}"><th scope="col" class="text-end">Thao tác</th></c:if>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach items="${shifts}" var="shift">
                                    <tr>
                                        <td class="shift-name-cell"><c:out value="${shift.shiftName}" /></td>
                                        <td class="shift-time-cell">${shift.startTimeText}</td>
                                        <td class="shift-time-cell">${shift.endTimeText}</td>
                                        <c:if test="${not isPastWeek}">
                                            <td class="text-end text-nowrap">
                                                <button type="button" class="shift-action-button shift-edit-button"
                                                        data-bs-toggle="modal" data-bs-target="#shiftEditModal"
                                                        data-id="${shift.id}" data-name="${fn:escapeXml(shift.shiftName)}"
                                                        data-start="${shift.startTimeText}" data-end="${shift.endTimeText}">
                                                    <span class="material-symbols-outlined" aria-hidden="true">edit</span> Sửa
                                                </button>
                                                <form method="post" action="${ctx}/admin/shifts" class="d-inline shift-delete-form"
                                                      data-name="${fn:escapeXml(shift.shiftName)}">
                                                    <input type="hidden" name="csrfToken" value="${csrf}">
                                                    <input type="hidden" name="action" value="delete">
                                                    <input type="hidden" name="weekStart" value="${weekStart}">
                                                    <input type="hidden" name="id" value="${shift.id}">
                                                    <button type="submit" class="shift-action-button is-danger"
                                                            ${shift.scheduleCount > 0 ? 'disabled' : ''}
                                                            title="${shift.scheduleCount > 0 ? 'Ca đã có nhân viên được xếp lịch, không xoá được' : 'Xoá ca'}">
                                                        <span class="material-symbols-outlined" aria-hidden="true">delete</span> Xoá
                                                    </button>
                                                </form>
                                            </td>
                                        </c:if>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="shift-empty-state">
                        <span class="material-symbols-outlined" aria-hidden="true">event_busy</span>
                        <strong>Tuần này chưa có khung ca</strong>
                        <c:choose>
                            <c:when test="${isPastWeek}">
                                <span>Tuần đã qua, không thêm ca được nữa.</span>
                            </c:when>
                            <c:otherwise>
                                <span>Bấm "Thêm ca" để tạo ca mới<c:if test="${not empty copySourceDisplay}">, hoặc sao chép nhanh các ca của tuần ${copySourceDisplay}</c:if>.</span>
                                <c:if test="${not empty copySourceDisplay}">
                                    <form method="post" action="${ctx}/admin/shifts">
                                        <input type="hidden" name="csrfToken" value="${csrf}">
                                        <input type="hidden" name="action" value="copy">
                                        <input type="hidden" name="weekStart" value="${weekStart}">
                                        <button type="submit" class="shift-copy-button">
                                            <span class="material-symbols-outlined" aria-hidden="true">content_copy</span>
                                            Sao chép ca từ tuần ${copySourceDisplay}
                                        </button>
                                    </form>
                                </c:if>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <p class="shift-footnote">
            Xếp nhân viên vào ca ở màn <a href="${ctx}/admin/schedules?date=${weekStart}">Lịch phân ca</a>.
            Ca đã có nhân viên được xếp lịch thì không xoá được.
        </p>
    </main>

    <c:if test="${not isPastWeek}">
    <div class="modal fade" id="shiftEditModal" tabindex="-1" aria-labelledby="shiftEditTitle" aria-hidden="true"
         data-reopen="${not empty form}">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content shift-modal-content">
                <form id="shiftEditForm" method="post" action="${ctx}/admin/shifts" novalidate>
                    <input type="hidden" name="csrfToken" value="${csrf}">
                    <input type="hidden" name="weekStart" value="${weekStart}">
                    <input type="hidden" name="action" id="shiftEditAction" value="${not empty form.id ? 'update' : 'add'}">
                    <input type="hidden" name="id" id="shiftEditId" value="${fn:escapeXml(form.id)}">

                    <div class="modal-header shift-modal-header">
                        <div>
                            <h2 class="modal-title" id="shiftEditTitle">${not empty form.id ? 'Sửa ca' : 'Thêm ca'}</h2>
                            <p class="shift-modal-subtitle">Áp dụng cho tuần ${weekStartDisplay} – ${weekEndDisplay}</p>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                    </div>

                    <div class="modal-body shift-modal-body">
                        <c:if test="${not empty shiftErrorMsg and not empty form}">
                            <div class="alert alert-danger py-2" role="alert" id="shiftFormError"><c:out value="${shiftErrorMsg}" /></div>
                        </c:if>
                        <div class="mb-3">
                            <label class="form-label" for="shiftEditName">Tên ca <span>*</span></label>
                            <input class="form-control" id="shiftEditName" name="shiftName" required maxlength="50"
                                   placeholder="VD: Ca sáng" value="${fn:escapeXml(form.shiftName)}">
                            <div class="invalid-feedback">Vui lòng nhập tên ca.</div>
                        </div>
                        <div class="row g-3">
                            <div class="col-sm-6">
                                <label class="form-label" for="shiftEditStart">Bắt đầu <span>*</span></label>
                                <input class="form-control" type="time" id="shiftEditStart" name="startTime" step="60" required
                                       value="${fn:escapeXml(form.startTime)}">
                                <div class="invalid-feedback">Vui lòng nhập giờ bắt đầu.</div>
                            </div>
                            <div class="col-sm-6">
                                <label class="form-label" for="shiftEditEnd">Kết thúc <span>*</span></label>
                                <input class="form-control" type="time" id="shiftEditEnd" name="endTime" step="60" required
                                       value="${fn:escapeXml(form.endTime)}">
                                <div class="invalid-feedback" id="shiftEditEndFeedback">Vui lòng nhập giờ kết thúc.</div>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer shift-modal-footer">
                        <button type="button" class="shift-cancel-button" data-bs-dismiss="modal">Huỷ</button>
                        <button type="submit" class="shift-save-button">
                            <span class="material-symbols-outlined">save</span>
                            <span id="shiftEditSubmit">${not empty form.id ? 'Lưu thay đổi' : 'Thêm ca'}</span>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    </c:if>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        (() => {
            document.querySelectorAll('.shift-delete-form').forEach((deleteForm) => {
                deleteForm.addEventListener('submit', (event) => {
                    if (!confirm('Xoá ' + deleteForm.dataset.name + ' khỏi tuần này?')) event.preventDefault();
                });
            });

            const modalEl = document.getElementById('shiftEditModal');
            if (!modalEl) return; // Tuần đã qua: chỉ xem

            const form = document.getElementById('shiftEditForm');
            const byId = (id) => document.getElementById(id);
            const actionInput = byId('shiftEditAction');
            const idInput = byId('shiftEditId');
            const nameInput = byId('shiftEditName');
            const startInput = byId('shiftEditStart');
            const endInput = byId('shiftEditEnd');
            const endFeedback = byId('shiftEditEndFeedback');
            const END_REQUIRED = 'Vui lòng nhập giờ kết thúc.';
            const END_AFTER_START = 'Giờ kết thúc phải sau giờ bắt đầu.';

            const open = (isEdit, values) => {
                form.classList.remove('was-validated');
                const serverError = byId('shiftFormError');
                if (serverError) serverError.remove();
                endInput.setCustomValidity('');
                endFeedback.textContent = END_REQUIRED;
                actionInput.value = isEdit ? 'update' : 'add';
                idInput.value = values.id || '';
                nameInput.value = values.name || '';
                startInput.value = values.start || '';
                endInput.value = values.end || '';
                byId('shiftEditTitle').textContent = isEdit ? 'Sửa ca' : 'Thêm ca';
                byId('shiftEditSubmit').textContent = isEdit ? 'Lưu thay đổi' : 'Thêm ca';
            };

            const addButton = byId('shiftAddButton');
            if (addButton) addButton.addEventListener('click', () => open(false, {}));
            document.querySelectorAll('.shift-edit-button').forEach((button) => {
                button.addEventListener('click', () => open(true, button.dataset));
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
                    if (firstInvalid) firstInvalid.focus();
                }
                form.classList.add('was-validated');
            });

            // Server báo lỗi: mở lại modal với dữ liệu vừa nhập
            if (modalEl.dataset.reopen === 'true') {
                bootstrap.Modal.getOrCreateInstance(modalEl).show();
            }
        })();
    </script>
</body>
</html>
