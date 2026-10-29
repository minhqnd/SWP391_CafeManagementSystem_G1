<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="weekStartLabel" value="${fn:substring(monday, 8, 10)}/${fn:substring(monday, 5, 7)}/${fn:substring(monday, 0, 4)}" />
<c:set var="weekEndLabel" value="${fn:substring(sunday, 8, 10)}/${fn:substring(sunday, 5, 7)}/${fn:substring(sunday, 0, 4)}" />

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lịch phân ca nhân viên | Aura Café</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
</head>
<body class="admin-body">
    <jsp:include page="/components/admin-sidebar.jsp">
        <jsp:param name="activeMenu" value="schedules" />
    </jsp:include>

    <main class="admin-main-wrapper employee-schedule-page">
        <c:if test="${not empty scheduleSuccessMsg}">
            <div class="alert alert-success alert-dismissible fade show schedule-alert" role="alert">
                <span class="material-symbols-outlined">check_circle</span>
                <span><c:out value="${scheduleSuccessMsg}" /></span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
            </div>
        </c:if>
        <c:if test="${not empty scheduleErrorMsg}">
            <div class="alert alert-danger alert-dismissible fade show schedule-alert" role="alert">
                <span class="material-symbols-outlined">error</span>
                <span><c:out value="${scheduleErrorMsg}" /></span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
            </div>
        </c:if>

        <section class="employee-schedule-header" aria-labelledby="schedulePageTitle">
            <div class="schedule-heading-copy">
                <span class="schedule-table-ref">Bảng: <strong>work_schedules</strong></span>
                <h1 id="schedulePageTitle">Lịch phân ca nhân viên</h1>
                <p>Tuần ${weekStartLabel} - ${weekEndLabel}</p>
            </div>

            <div class="schedule-header-actions">
                <a class="schedule-nav-button" href="${ctx}/admin/schedules?date=${prevWeekDate}" aria-label="Xem tuần trước">
                    <span class="material-symbols-outlined">chevron_left</span>
                    <span>Tuần trước</span>
                </a>
                <a class="schedule-nav-button" href="${ctx}/admin/schedules?date=${nextWeekDate}" aria-label="Xem tuần sau">
                    <span class="material-symbols-outlined">chevron_right</span>
                    <span>Tuần sau</span>
                </a>
                <button type="button" class="schedule-create-button add-schedule-trigger" data-date="${selectedDate}">
                    <span class="material-symbols-outlined">event_upcoming</span>
                    <span>Phân ca mới</span>
                </button>
            </div>
        </section>

        <c:if test="${empty weekShifts}">
            <div class="alert alert-warning d-flex align-items-center gap-2" role="alert">
                <span class="material-symbols-outlined">schedule</span>
                <span>Tuần ${weekStartLabel} - ${weekEndLabel} chưa có khung ca nên chưa xếp lịch được.
                    <a href="${ctx}/admin/shifts?date=${monday}" class="alert-link">Tạo khung ca cho tuần này</a>.</span>
            </div>
        </c:if>

        <section class="weekly-schedule-card" aria-labelledby="weeklyScheduleTitle">
            <div class="weekly-schedule-toolbar">
                <h2 id="weeklyScheduleTitle">Bảng xếp lịch tuần theo ca</h2>
                <div class="position-legend" aria-label="Chú thích vị trí nhân viên">
                    <span class="legend-pill legend-manager">Quản lý nhân viên</span>
                    <span class="legend-pill legend-barista">Pha chế</span>
                    <span class="legend-pill legend-cashier">Thu ngân</span>
                    <span class="legend-pill legend-shipper">Giao hàng</span>
                </div>
            </div>

            <div class="weekly-calendar-scroll">
                <div class="weekly-calendar" role="table" aria-label="Lịch phân ca theo tuần">
                    <div class="weekly-calendar-head" role="row">
                        <c:forEach items="${weekDays}" var="day" varStatus="dayLoop">
                            <c:set var="dayIso" value="${day.toString()}" />
                            <c:set var="dayDisplay" value="${fn:substring(dayIso, 8, 10)}/${fn:substring(dayIso, 5, 7)}/${fn:substring(dayIso, 0, 4)}" />
                            <div class="weekly-day-heading ${dayIso == selectedDate ? 'is-selected' : ''}" role="columnheader">
                                <strong>
                                    <c:choose>
                                        <c:when test="${dayLoop.index == 0}">Thứ 2</c:when>
                                        <c:when test="${dayLoop.index == 1}">Thứ 3</c:when>
                                        <c:when test="${dayLoop.index == 2}">Thứ 4</c:when>
                                        <c:when test="${dayLoop.index == 3}">Thứ 5</c:when>
                                        <c:when test="${dayLoop.index == 4}">Thứ 6</c:when>
                                        <c:when test="${dayLoop.index == 5}">Thứ 7</c:when>
                                        <c:otherwise>Chủ nhật</c:otherwise>
                                    </c:choose>
                                </strong>
                                <span>${dayDisplay}</span>
                            </div>
                        </c:forEach>
                    </div>

                    <div class="weekly-day-columns" role="row">
                        <c:forEach items="${weekDays}" var="day">
                            <c:set var="dayIso" value="${day.toString()}" />
                            <div class="weekly-day-column ${dayIso == selectedDate ? 'is-selected' : ''}" role="cell">
                                <%-- Các ca của tuần lấy từ work_shifts (mỗi tuần có bộ ca riêng) --%>
                                <c:forEach items="${weekShifts}" var="shift">
                                    <c:set var="slotKey" value="${dayIso}_${shift.id}" />
                                    <div class="day-shift-section">
                                        <div class="day-shift-label"><c:out value="${shift.shiftName}" /> <span>(${shift.timeRange})</span></div>
                                        <c:forEach items="${calendarSlotMap[slotKey]}" var="item">
                                            <article class="employee-shift-card role-${fn:toLowerCase(item.userPosition)} edit-schedule-trigger"
                                                     tabindex="0" role="button" aria-label="Sửa ca của ${fn:escapeXml(item.userName)}"
                                                     data-id="${item.id}" data-user-id="${item.userId}"
                                                     data-work-date="${item.workDateFormatted}" data-shift-id="${item.shiftId}"
                                                     data-status="${item.status}" data-note="${fn:escapeXml(item.note)}"
                                                     data-starting-cash="${item.startingCash}" data-ending-cash="${item.endingCashActual}">
                                                <div class="employee-card-name"><c:out value="${item.userName}" /></div>
                                                <div class="employee-card-meta">${item.positionDisplayName} · ${item.statusDisplayName}</div>
                                                <c:if test="${item.status == 'SCHEDULED'}">
                                                    <form method="post" action="${ctx}/admin/schedules" class="employee-card-delete"
                                                          onsubmit="event.stopPropagation(); return confirm('Bạn có chắc muốn hủy ca làm này?');">
                                                        <input type="hidden" name="action" value="delete">
                                                        <input type="hidden" name="id" value="${item.id}">
                                                        <input type="hidden" name="returnDate" value="${selectedDate}">
                                                        <input type="hidden" name="returnView" value="calendar">
                                                        <button type="submit" onclick="event.stopPropagation()" aria-label="Hủy ca của ${fn:escapeXml(item.userName)}">
                                                            <span class="material-symbols-outlined">close</span>
                                                        </button>
                                                    </form>
                                                </c:if>
                                            </article>
                                        </c:forEach>
                                        <button type="button" class="empty-shift-slot add-schedule-trigger" data-date="${dayIso}" data-shift-id="${shift.id}">+ Xếp ca</button>
                                    </div>
                                </c:forEach>
                                <c:if test="${empty weekShifts}">
                                    <div class="day-shift-section">
                                        <div class="day-shift-label">Chưa có khung ca</div>
                                    </div>
                                </c:if>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>
        </section>
    </main>

    <div class="modal fade modal-aura" id="addScheduleModal" tabindex="-1" aria-labelledby="addScheduleModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <form method="post" action="${ctx}/admin/schedules" id="addScheduleForm" novalidate>
                    <input type="hidden" name="action" value="add">
                    <input type="hidden" name="returnDate" value="${selectedDate}">
                    <input type="hidden" name="returnView" value="calendar">

                    <div class="modal-header">
                        <div>
                            <h2 class="modal-title" id="addScheduleModalLabel">Phân ca làm việc mới</h2>
                            <p class="modal-subtitle">Lịch được lưu vào bảng work_schedules.</p>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="addUserId" class="form-label">Nhân viên <span class="text-danger">*</span></label>
                            <select class="form-select aura-input" id="addUserId" name="userId" required aria-describedby="addUserError">
                                <option value="">Chọn nhân viên</option>
                                <c:forEach items="${staffList}" var="staff">
                                    <option value="${staff.id}" data-position="${staff.position}"><c:out value="${staff.fullName}" /> — ${staff.position} (${staff.phone})</option>
                                </c:forEach>
                            </select>
                            <div class="invalid-feedback" id="addUserError">Vui lòng chọn nhân viên.</div>
                        </div>
                        <div class="row g-3 mb-3">
                            <div class="col-sm-6">
                                <label for="addWorkDate" class="form-label">Ngày làm việc <span class="text-danger">*</span></label>
                                <input type="date" class="form-control aura-input" id="addWorkDate" name="workDate" required min="${monday}" max="${sunday}" value="${selectedDate}" aria-describedby="addWorkDateError">
                                <div class="invalid-feedback" id="addWorkDateError">Vui lòng chọn ngày làm việc.</div>
                            </div>
                            <div class="col-sm-6">
                                <label for="addShiftId" class="form-label">Ca làm việc <span class="text-danger">*</span></label>
                                <select class="form-select aura-input" id="addShiftId" name="shiftId" required aria-describedby="addShiftError">
                                    <c:forEach items="${weekShifts}" var="shift">
                                        <option value="${shift.id}"><c:out value="${shift.label}" /></option>
                                    </c:forEach>
                                </select>
                                <div class="invalid-feedback" id="addShiftError">Vui lòng chọn ca làm việc.</div>
                            </div>
                        </div>
                        <div class="mb-3 cash-field-group" data-cash-group="add">
                            <label for="addStartingCash" class="form-label">Tiền két đầu ca (VNĐ)</label>
                            <input type="number" class="form-control aura-input" id="addStartingCash" name="startingCash"
                                   min="0" max="9999999999" step="1000" inputmode="numeric"
                                   placeholder="Chỉ áp dụng cho Thu ngân" aria-describedby="addCashHint addCashError">
                            <div class="field-hint" id="addCashHint">Chọn nhân viên Thu ngân để nhập tiền két.</div>
                            <div class="invalid-feedback" id="addCashError">Số tiền phải từ 0 đến 9.999.999.999đ và là bội số của 1.000đ.</div>
                        </div>
                        <div>
                            <div class="field-label-row">
                                <label for="addNote" class="form-label">Ghi chú</label>
                                <span class="character-count" id="addNoteCount">0/500</span>
                            </div>
                            <textarea class="form-control aura-input" id="addNote" name="note" rows="3" maxlength="500"
                                      placeholder="Nhiệm vụ hoặc lưu ý trong ca..." aria-describedby="addNoteCount addNoteError"></textarea>
                            <div class="invalid-feedback" id="addNoteError">Ghi chú không được vượt quá 500 ký tự.</div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-aura-primary">Xác nhận phân ca</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <div class="modal fade modal-aura" id="editScheduleModal" tabindex="-1" aria-labelledby="editScheduleModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <form method="post" action="${ctx}/admin/schedules" id="editScheduleForm" novalidate>
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="id" id="editId">
                    <input type="hidden" name="returnDate" value="${selectedDate}">
                    <input type="hidden" name="returnView" value="calendar">

                    <div class="modal-header">
                        <div>
                            <h2 class="modal-title" id="editScheduleModalLabel">Cập nhật ca làm việc</h2>
                            <p class="modal-subtitle">Thay đổi nhân sự, ca và trạng thái điểm danh.</p>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="editUserId" class="form-label">Nhân viên <span class="text-danger">*</span></label>
                            <select class="form-select aura-input" id="editUserId" name="userId" required aria-describedby="editUserError">
                                <c:forEach items="${staffList}" var="staff">
                                    <option value="${staff.id}" data-position="${staff.position}"><c:out value="${staff.fullName}" /> — ${staff.position} (${staff.phone})</option>
                                </c:forEach>
                            </select>
                            <div class="invalid-feedback" id="editUserError">Vui lòng chọn nhân viên.</div>
                        </div>
                        <div class="row g-3 mb-3">
                            <div class="col-sm-6">
                                <label for="editWorkDate" class="form-label">Ngày làm việc <span class="text-danger">*</span></label>
                                <input type="date" class="form-control aura-input" id="editWorkDate" name="workDate" required min="${monday}" max="${sunday}" aria-describedby="editWorkDateError">
                                <div class="invalid-feedback" id="editWorkDateError">Vui lòng chọn ngày làm việc.</div>
                            </div>
                            <div class="col-sm-6">
                                <label for="editShiftId" class="form-label">Ca làm việc <span class="text-danger">*</span></label>
                                <select class="form-select aura-input" id="editShiftId" name="shiftId" required aria-describedby="editShiftError">
                                    <c:forEach items="${weekShifts}" var="shift">
                                        <option value="${shift.id}"><c:out value="${shift.label}" /></option>
                                    </c:forEach>
                                </select>
                                <div class="invalid-feedback" id="editShiftError">Vui lòng chọn ca làm việc.</div>
                            </div>
                        </div>
                        <div class="mb-3">
                            <label for="editStatus" class="form-label">Trạng thái <span class="text-danger">*</span></label>
                            <select class="form-select aura-input" id="editStatus" name="status" required aria-describedby="editStatusError">
                                <option value="SCHEDULED">Đã xếp lịch</option>
                                <option value="PRESENT">Có mặt</option>
                                <option value="ABSENT">Vắng mặt</option>
                                <option value="COMPLETED">Hoàn thành</option>
                            </select>
                            <div class="invalid-feedback" id="editStatusError">Vui lòng chọn trạng thái ca làm việc.</div>
                        </div>
                        <div class="row g-3 mb-3 cash-field-group" data-cash-group="edit">
                            <div class="col-sm-6">
                                <label for="editStartingCash" class="form-label">Tiền két đầu ca</label>
                                <input type="number" class="form-control aura-input" id="editStartingCash" name="startingCash"
                                       min="0" max="9999999999" step="1000" inputmode="numeric" aria-describedby="editStartingCashError">
                                <div class="invalid-feedback" id="editStartingCashError">Số tiền phải hợp lệ và là bội số của 1.000đ.</div>
                            </div>
                            <div class="col-sm-6">
                                <label for="editEndingCashActual" class="form-label">Tiền két thực tế cuối ca</label>
                                <input type="number" class="form-control aura-input" id="editEndingCashActual" name="endingCashActual"
                                       min="0" max="9999999999" step="1000" inputmode="numeric" aria-describedby="editEndingCashError">
                                <div class="invalid-feedback" id="editEndingCashError">Số tiền phải hợp lệ và là bội số của 1.000đ.</div>
                            </div>
                            <div class="col-12 pt-0"><div class="field-hint" id="editCashHint">Thông tin két chỉ áp dụng cho Thu ngân.</div></div>
                        </div>
                        <div>
                            <div class="field-label-row">
                                <label for="editNote" class="form-label">Ghi chú</label>
                                <span class="character-count" id="editNoteCount">0/500</span>
                            </div>
                            <textarea class="form-control aura-input" id="editNote" name="note" rows="3" maxlength="500"
                                      aria-describedby="editNoteCount editNoteError"></textarea>
                            <div class="invalid-feedback" id="editNoteError">Ghi chú không được vượt quá 500 ký tự.</div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-aura-primary">Lưu thay đổi</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        const addScheduleModal = new bootstrap.Modal(document.getElementById('addScheduleModal'));
        const editScheduleModal = new bootstrap.Modal(document.getElementById('editScheduleModal'));
        const addScheduleForm = document.getElementById('addScheduleForm');
        const editScheduleForm = document.getElementById('editScheduleForm');
        const maxCashAmount = 9999999999;

        function getSelectedPosition(selectId) {
            const select = document.getElementById(selectId);
            return select.options[select.selectedIndex]?.dataset.position || '';
        }

        function resetValidation(form) {
            form.classList.remove('was-validated');
            form.querySelectorAll('input, select, textarea').forEach((field) => field.setCustomValidity(''));
        }

        function validateCashInput(input) {
            input.setCustomValidity('');
            if (input.disabled || input.value.trim() === '') return;

            const amount = input.valueAsNumber;
            if (!Number.isFinite(amount) || amount < 0 || amount > maxCashAmount || !Number.isInteger(amount) || amount % 1000 !== 0) {
                input.setCustomValidity('Số tiền không hợp lệ');
            }
        }

        function updateNoteCounter(noteInput, counter) {
            counter.textContent = noteInput.value.length + '/500';
            noteInput.setCustomValidity(noteInput.value.length <= 500 ? '' : 'Ghi chú quá dài');
        }

        function updateCashAvailability(prefix) {
            const position = getSelectedPosition(prefix + 'UserId');
            const isCashier = position === 'CASHIER';
            const group = document.querySelector('[data-cash-group="' + prefix + '"]');
            const cashInputs = group.querySelectorAll('input[type="number"]');

            group.classList.toggle('is-disabled', !isCashier);
            cashInputs.forEach((input) => {
                if (!isCashier) input.value = '';
                input.disabled = !isCashier;
                input.setCustomValidity('');
            });

            const hint = document.getElementById(prefix + 'CashHint');
            if (hint) {
                hint.textContent = isCashier
                    ? 'Không bắt buộc. Số tiền nhập theo bội số 1.000đ.'
                    : 'Chỉ nhân viên Thu ngân mới được nhập thông tin tiền két.';
            }
        }

        function setupFormValidation(form, prefix) {
            const noteInput = document.getElementById(prefix + 'Note');
            const noteCounter = document.getElementById(prefix + 'NoteCount');
            const userSelect = document.getElementById(prefix + 'UserId');
            const cashInputs = form.querySelectorAll('.cash-field-group input[type="number"]');

            userSelect.addEventListener('change', () => updateCashAvailability(prefix));
            noteInput.addEventListener('input', () => updateNoteCounter(noteInput, noteCounter));
            cashInputs.forEach((input) => input.addEventListener('input', () => validateCashInput(input)));

            form.addEventListener('submit', (event) => {
                updateCashAvailability(prefix);
                updateNoteCounter(noteInput, noteCounter);
                cashInputs.forEach(validateCashInput);

                if (!form.checkValidity()) {
                    event.preventDefault();
                    event.stopPropagation();
                    form.querySelector(':invalid')?.focus();
                }
                form.classList.add('was-validated');
            });
        }

        setupFormValidation(addScheduleForm, 'add');
        setupFormValidation(editScheduleForm, 'edit');
        updateCashAvailability('add');

        document.querySelectorAll('.add-schedule-trigger').forEach((button) => {
            button.addEventListener('click', () => {
                addScheduleForm.reset();
                resetValidation(addScheduleForm);
                document.getElementById('addWorkDate').value = button.dataset.date || '${selectedDate}';
                const addShiftSelect = document.getElementById('addShiftId');
                if (button.dataset.shiftId) addShiftSelect.value = button.dataset.shiftId;
                updateNoteCounter(document.getElementById('addNote'), document.getElementById('addNoteCount'));
                updateCashAvailability('add');
                addScheduleModal.show();
            });
        });

        function openEditSchedule(card) {
            document.getElementById('editId').value = card.dataset.id;
            document.getElementById('editUserId').value = card.dataset.userId;
            document.getElementById('editWorkDate').value = card.dataset.workDate;
            document.getElementById('editShiftId').value = card.dataset.shiftId;
            document.getElementById('editStatus').value = card.dataset.status;
            document.getElementById('editNote').value = card.dataset.note || '';
            document.getElementById('editStartingCash').value = card.dataset.startingCash && card.dataset.startingCash !== 'null'
                ? card.dataset.startingCash : '';
            document.getElementById('editEndingCashActual').value = card.dataset.endingCash && card.dataset.endingCash !== 'null'
                ? card.dataset.endingCash : '';

            resetValidation(editScheduleForm);
            updateNoteCounter(document.getElementById('editNote'), document.getElementById('editNoteCount'));
            updateCashAvailability('edit');
            editScheduleModal.show();
        }

        document.querySelectorAll('.edit-schedule-trigger').forEach((card) => {
            card.addEventListener('click', (event) => {
                if (!event.target.closest('.employee-card-delete')) openEditSchedule(card);
            });
            card.addEventListener('keydown', (event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    openEditSchedule(card);
                }
            });
        });
    </script>
</body>
</html>
