<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chấm công | Aura Café</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-attendance.css" rel="stylesheet">
</head>
<body class="admin-body">
    <jsp:include page="/components/admin-sidebar.jsp">
        <jsp:param name="activeMenu" value="attendance" />
    </jsp:include>

    <main class="admin-main-wrapper attendance-page">
        <c:if test="${not empty attendanceSuccessMsg}">
            <div class="alert alert-success alert-dismissible fade show attendance-alert" role="alert">
                <span class="material-symbols-outlined">check_circle</span>
                <span><c:out value="${attendanceSuccessMsg}" /></span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
            </div>
        </c:if>
        <c:if test="${not empty attendanceErrorMsg}">
            <div class="alert alert-danger alert-dismissible fade show attendance-alert" role="alert">
                <span class="material-symbols-outlined">error</span>
                <span><c:out value="${attendanceErrorMsg}" /></span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
            </div>
        </c:if>

        <section class="attendance-summary" aria-labelledby="attendancePageTitle">
            <div class="attendance-heading-row">
                <div>
                    <span class="attendance-table-ref">Bảng: <strong>work_schedules</strong></span>
                    <h1 id="attendancePageTitle">Chấm công</h1>
                    <p>Giờ vào/ra ca và trạng thái làm việc thực tế của nhân viên.</p>
                </div>

                <form class="attendance-date-form" method="get" action="${ctx}/admin/attendance">
                    <label for="attendanceDate" class="visually-hidden">Ngày chấm công</label>
                    <input id="attendanceDate" type="date" name="date" value="${selectedDate}"
                           aria-label="Chọn ngày chấm công" onchange="this.form.submit()">
                </form>
            </div>

            <div class="attendance-kpis" aria-label="Thống kê chấm công ngày ${selectedDateDisplay}">
                <article class="attendance-kpi kpi-total">
                    <span>Lượt ca trong ngày</span>
                    <strong>${totalShiftCount}</strong>
                </article>
                <article class="attendance-kpi kpi-present">
                    <span>Có mặt / Hoàn thành</span>
                    <strong>${presentCompletedCount}</strong>
                </article>
                <article class="attendance-kpi kpi-late">
                    <span>Đi muộn</span>
                    <strong>${lateCount}</strong>
                </article>
                <article class="attendance-kpi kpi-absent">
                    <span>Vắng</span>
                    <strong>${absentCount}</strong>
                </article>
            </div>
        </section>

        <section class="attendance-table-card" aria-labelledby="attendanceTableTitle">
            <h2 id="attendanceTableTitle" class="visually-hidden">Danh sách chấm công ngày ${selectedDateDisplay}</h2>
            <div class="attendance-table-scroll">
                <table class="attendance-table">
                    <thead>
                        <tr>
                            <th scope="col">Nhân viên</th>
                            <th scope="col">Ca làm việc</th>
                            <th scope="col">Giờ vào (check_in_time)</th>
                            <th scope="col">Giờ ra (check_out_time)</th>
                            <th scope="col">Trạng thái</th>
                            <th scope="col">Ghi chú</th>
                            <th scope="col" class="attendance-action-heading">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${attendanceList}" var="item">
                            <tr>
                                <td>
                                    <div class="attendance-employee-name"><c:out value="${item.userName}" /></div>
                                    <div class="attendance-muted">${item.positionDisplayName}</div>
                                </td>
                                <td>
                                    <div class="attendance-shift-name">${item.shiftDisplayName}</div>
                                    <div class="attendance-muted">${item.shiftTime}</div>
                                </td>
                                <td>
                                    <span class="attendance-time ${item.late ? 'is-late' : ''}">${item.checkInFormatted}</span>
                                    <c:if test="${item.late}">
                                        <span class="late-label">(muộn ${item.lateMinutes} phút)</span>
                                    </c:if>
                                </td>
                                <td><span class="attendance-time">${item.checkOutFormatted}</span></td>
                                <td>
                                    <span class="attendance-status status-${fn:toLowerCase(item.status)}">
                                        ${item.statusDisplayName}
                                    </span>
                                </td>
                                <td class="attendance-note">
                                    <c:choose>
                                        <c:when test="${not empty item.note}"><c:out value="${item.note}" /></c:when>
                                        <c:otherwise><span class="attendance-muted">—</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="attendance-action-cell">
                                    <button type="button" class="attendance-edit-button"
                                            data-bs-toggle="modal" data-bs-target="#attendanceEditModal"
                                            data-id="${item.id}"
                                            data-name="${fn:escapeXml(item.userName)}"
                                            data-shift="${fn:escapeXml(item.shiftDisplayName)} (${fn:escapeXml(item.shiftTime)})"
                                            data-status="${item.status}"
                                            data-overnight="${item.overnightShift}"
                                            data-shift-start="${item.shiftStartFormatted}"
                                            data-check-in="${empty item.checkInTime ? '' : item.checkInFormatted}"
                                            data-check-out="${empty item.checkOutTime ? '' : item.checkOutFormatted}"
                                            data-note="${fn:escapeXml(item.note)}">
                                        <span class="material-symbols-outlined" aria-hidden="true">edit</span>
                                        <span>Sửa</span>
                                    </button>
                                </td>
                            </tr>
                        </c:forEach>

                        <c:if test="${empty attendanceList}">
                            <tr>
                                <td colspan="7">
                                    <div class="attendance-empty-state">
                                        <span class="material-symbols-outlined">event_busy</span>
                                        <strong>Chưa có ca làm việc trong ngày ${selectedDateDisplay}</strong>
                                        <span>Hãy phân ca cho nhân viên trước khi thực hiện chấm công.</span>
                                        <a href="${ctx}/admin/schedules?date=${selectedDate}">Mở lịch phân ca</a>
                                    </div>
                                </td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </section>
    </main>

    <div class="modal fade" id="attendanceEditModal" tabindex="-1" aria-labelledby="attendanceEditTitle" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content attendance-modal-content">
                <form id="attendanceEditForm" method="post" action="${ctx}/admin/attendance" novalidate>
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.attendanceCsrfToken)}">
                    <input type="hidden" name="id" id="attendanceScheduleId">
                    <input type="hidden" name="returnDate" value="${selectedDate}">

                    <div class="modal-header attendance-modal-header">
                        <div>
                            <h2 class="modal-title" id="attendanceEditTitle">Cập nhật chấm công</h2>
                            <p id="attendanceModalEmployee"></p>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                    </div>

                    <div class="modal-body attendance-modal-body">
                        <div class="attendance-modal-shift">
                            <span class="material-symbols-outlined">schedule</span>
                            <span id="attendanceModalShift"></span>
                        </div>

                        <div class="mb-3">
                            <label class="form-label" for="attendanceStatus">Trạng thái <span>*</span></label>
                            <select class="form-select" id="attendanceStatus" name="status" required>
                                <option value="SCHEDULED">Đã xếp lịch</option>
                                <option value="PRESENT">Có mặt</option>
                                <option value="COMPLETED">Hoàn thành</option>
                                <option value="ABSENT">Vắng mặt</option>
                            </select>
                            <div class="invalid-feedback">Vui lòng chọn trạng thái.</div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-sm-6">
                                <label class="form-label" for="attendanceCheckIn">Giờ vào</label>
                                <input class="form-control" type="time" id="attendanceCheckIn" name="checkInTime" step="60">
                                <div class="invalid-feedback">Vui lòng nhập giờ vào.</div>
                            </div>
                            <div class="col-sm-6">
                                <label class="form-label" for="attendanceCheckOut">Giờ ra</label>
                                <input class="form-control" type="time" id="attendanceCheckOut" name="checkOutTime" step="60">
                                <div class="invalid-feedback" id="attendanceTimeFeedback">Vui lòng nhập giờ ra.</div>
                            </div>
                        </div>

                        <div>
                            <div class="attendance-label-row">
                                <label class="form-label" for="attendanceNote">Ghi chú</label>
                                <span id="attendanceNoteCount">0/500</span>
                            </div>
                            <textarea class="form-control" id="attendanceNote" name="note" rows="3" maxlength="500"
                                      placeholder="Nhiệm vụ hoặc lưu ý trong ca..."></textarea>
                            <div class="invalid-feedback">Ghi chú không được vượt quá 500 ký tự.</div>
                        </div>
                    </div>

                    <div class="modal-footer attendance-modal-footer">
                        <button type="button" class="attendance-cancel-button" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="attendance-save-button">
                            <span class="material-symbols-outlined">save</span>
                            <span>Lưu chấm công</span>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        (() => {
            const form = document.getElementById('attendanceEditForm');
            const idInput = document.getElementById('attendanceScheduleId');
            const statusInput = document.getElementById('attendanceStatus');
            const checkInInput = document.getElementById('attendanceCheckIn');
            const checkOutInput = document.getElementById('attendanceCheckOut');
            const timeFeedback = document.getElementById('attendanceTimeFeedback');
            const noteInput = document.getElementById('attendanceNote');
            const noteCount = document.getElementById('attendanceNoteCount');
            const employeeLabel = document.getElementById('attendanceModalEmployee');
            const shiftLabel = document.getElementById('attendanceModalShift');

            const updateNoteCount = () => {
                noteCount.textContent = `${noteInput.value.length}/500`;
            };

            // Giờ vào/ra bị xoá khi đổi sang trạng thái không dùng tới - nhớ lại để điền lại nếu đổi về
            // (VD: Hoàn thành -> lỡ chọn Vắng mặt -> chọn lại Hoàn thành thì không mất giờ vào/ra)
            const savedTimes = new Map();

            const clearOrRestore = (input) => {
                if (input.disabled) {
                    if (input.value) savedTimes.set(input, input.value);
                    input.value = '';
                } else if (!input.value && savedTimes.has(input)) {
                    input.value = savedTimes.get(input);
                }
            };

            const applyStatusRules = (clearIncompatibleValues) => {
                const status = statusInput.value;
                const hasNoAttendance = status === 'SCHEDULED' || status === 'ABSENT';
                const isPresent = status === 'PRESENT';
                const isCompleted = status === 'COMPLETED';

                checkInInput.disabled = hasNoAttendance;
                checkOutInput.disabled = hasNoAttendance || isPresent;
                checkInInput.required = isPresent || isCompleted;
                checkOutInput.required = isCompleted;

                if (clearIncompatibleValues) {
                    clearOrRestore(checkInInput);
                    clearOrRestore(checkOutInput);
                }
                checkOutInput.setCustomValidity('');
                timeFeedback.textContent = 'Vui lòng nhập giờ ra.';
            };

            document.querySelectorAll('.attendance-edit-button').forEach((button) => {
                button.addEventListener('click', () => {
                    form.classList.remove('was-validated');
                    savedTimes.clear();
                    idInput.value = button.dataset.id;
                    employeeLabel.textContent = button.dataset.name;
                    shiftLabel.textContent = button.dataset.shift;
                    statusInput.value = button.dataset.status;
                    // Chưa chấm giờ vào thì mặc định theo đúng giờ bắt đầu của ca.
                    // Bản ghi đã có giờ vào thực tế luôn được giữ nguyên.
                    checkInInput.value = button.dataset.checkIn || button.dataset.shiftStart || '';
                    checkOutInput.value = button.dataset.checkOut;
                    noteInput.value = button.dataset.note || '';
                    form.dataset.overnight = button.dataset.overnight;
                    applyStatusRules(false);
                    updateNoteCount();
                });
            });

            statusInput.addEventListener('change', () => applyStatusRules(true));
            noteInput.addEventListener('input', updateNoteCount);

            form.addEventListener('submit', (event) => {
                checkOutInput.setCustomValidity('');
                timeFeedback.textContent = 'Vui lòng nhập giờ ra.';

                // Ca qua đêm (VD 23:00 - 02:00): giờ ra nhỏ hơn giờ vào là sáng hôm sau, server tự kiểm tra
                if (statusInput.value === 'COMPLETED' && checkInInput.value && checkOutInput.value
                        && form.dataset.overnight !== 'true'
                        && checkOutInput.value <= checkInInput.value) {
                    checkOutInput.setCustomValidity('Giờ ra phải sau giờ vào.');
                    timeFeedback.textContent = 'Giờ ra phải sau giờ vào.';
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
