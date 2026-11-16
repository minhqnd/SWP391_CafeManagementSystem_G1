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
                                                     data-status="${item.status}" data-note="${fn:escapeXml(item.note)}">
                                                <div class="employee-card-name"><c:out value="${item.userName}" /></div>
                                                <div class="employee-card-meta">${item.positionDisplayName} · ${item.statusDisplayName}</div>
                                                <c:if test="${item.status == 'SCHEDULED'}">
                                                    <form method="post" action="${ctx}/admin/schedules" class="employee-card-delete"
                                                          onsubmit="event.stopPropagation(); return confirm('Bạn có chắc muốn hủy ca làm này?');">
                                                        <input type="hidden" name="action" value="delete">
                                                        <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.scheduleCsrfToken)}">
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
                    <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.scheduleCsrfToken)}">
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
                    <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.scheduleCsrfToken)}">
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
                                <option value="">Chọn nhân viên</option>
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
                            <select class="form-select aura-input" id="editStatus" disabled aria-describedby="editStatusError">
                                <option value="SCHEDULED">Đã xếp lịch</option>
                                <option value="PRESENT">Có mặt</option>
                                <option value="ABSENT">Vắng mặt</option>
                                <option value="COMPLETED">Hoàn thành</option>
                            </select>
                            <div class="form-text" id="editStatusError">Trạng thái và giờ vào/ra được cập nhật tại màn Chấm công.</div>
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

        // Dữ liệu lịch đầy đủ của tuần, không phụ thuộc bộ lọc đang hiển thị.
        // Backend vẫn kiểm tra trùng lần nữa khi POST để chống request thủ công.
        const occupiedSchedules = [
            <c:forEach items="${occupiedWeekSchedules}" var="item">
            { id: ${item.id}, userId: ${item.userId}, workDate: '${item.workDateFormatted}', shiftId: ${item.shiftId} },
            </c:forEach>
        ];

        function filterAvailableEmployees(select, workDate, shiftId, excludedScheduleId) {
            const normalizedShiftId = Number(shiftId || 0);
            const normalizedExcludedId = Number(excludedScheduleId || 0);
            let availableCount = 0;

            Array.from(select.options).forEach((option, index) => {
                if (index === 0 || !option.value) return;

                const userId = Number(option.value);
                const alreadyScheduled = occupiedSchedules.some((schedule) =>
                    schedule.userId === userId
                    && schedule.workDate === workDate
                    && schedule.shiftId === normalizedShiftId
                    && schedule.id !== normalizedExcludedId
                );

                // hidden loại nhân viên khỏi dropdown; disabled là lớp bảo vệ bổ sung.
                option.hidden = alreadyScheduled;
                option.disabled = alreadyScheduled;
                if (!alreadyScheduled) availableCount++;
            });

            const placeholder = select.options[0];
            placeholder.textContent = availableCount > 0
                ? 'Chọn nhân viên'
                : 'Không còn nhân viên trống trong ca này';

            const selectedOption = select.options[select.selectedIndex];
            if (selectedOption && selectedOption.disabled) select.value = '';
            select.setCustomValidity('');
        }

        function refreshAddEmployeeOptions() {
            filterAvailableEmployees(
                document.getElementById('addUserId'),
                document.getElementById('addWorkDate').value,
                document.getElementById('addShiftId').value,
                0
            );
        }

        function refreshEditEmployeeOptions() {
            filterAvailableEmployees(
                document.getElementById('editUserId'),
                document.getElementById('editWorkDate').value,
                document.getElementById('editShiftId').value,
                document.getElementById('editId').value
            );
        }

        function resetValidation(form) {
            form.classList.remove('was-validated');
            form.querySelectorAll('input, select, textarea').forEach((field) => field.setCustomValidity(''));
        }

        function updateNoteCounter(noteInput, counter) {
            counter.textContent = noteInput.value.length + '/500';
            noteInput.setCustomValidity(noteInput.value.length <= 500 ? '' : 'Ghi chú quá dài');
        }

        function setupFormValidation(form, prefix) {
            const noteInput = document.getElementById(prefix + 'Note');
            const noteCounter = document.getElementById(prefix + 'NoteCount');

            noteInput.addEventListener('input', () => updateNoteCounter(noteInput, noteCounter));

            form.addEventListener('submit', (event) => {
                updateNoteCounter(noteInput, noteCounter);

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

        document.querySelectorAll('.add-schedule-trigger').forEach((button) => {
            button.addEventListener('click', () => {
                addScheduleForm.reset();
                resetValidation(addScheduleForm);
                document.getElementById('addWorkDate').value = button.dataset.date || '${selectedDate}';
                const addShiftSelect = document.getElementById('addShiftId');
                if (button.dataset.shiftId) addShiftSelect.value = button.dataset.shiftId;
                refreshAddEmployeeOptions();
                updateNoteCounter(document.getElementById('addNote'), document.getElementById('addNoteCount'));
                addScheduleModal.show();
            });
        });

        document.getElementById('addWorkDate').addEventListener('change', refreshAddEmployeeOptions);
        document.getElementById('addShiftId').addEventListener('change', refreshAddEmployeeOptions);

        function openEditSchedule(card) {
            document.getElementById('editId').value = card.dataset.id;
            document.getElementById('editUserId').value = card.dataset.userId;
            document.getElementById('editWorkDate').value = card.dataset.workDate;
            document.getElementById('editShiftId').value = card.dataset.shiftId;
            document.getElementById('editStatus').value = card.dataset.status;
            document.getElementById('editNote').value = card.dataset.note || '';

            resetValidation(editScheduleForm);
            refreshEditEmployeeOptions();
            // Lịch đang sửa được loại khỏi phép kiểm tra nên nhân viên hiện tại vẫn được chọn.
            document.getElementById('editUserId').value = card.dataset.userId;
            updateNoteCounter(document.getElementById('editNote'), document.getElementById('editNoteCount'));
            editScheduleModal.show();
        }

        document.getElementById('editWorkDate').addEventListener('change', refreshEditEmployeeOptions);
        document.getElementById('editShiftId').addEventListener('change', refreshEditEmployeeOptions);

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
