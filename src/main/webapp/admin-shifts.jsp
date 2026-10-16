<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Khung giờ ca làm việc | Aura Café Admin</title>

    <!-- Google Fonts & Icons -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">

    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">

    <!-- Admin Schedule CSS -->
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
</head>
<body class="admin-body">

    <!-- Admin Sidebar Include -->
    <jsp:include page="/components/admin-sidebar.jsp">
        <jsp:param name="activeMenu" value="shifts" />
    </jsp:include>

    <!-- Main Content Area -->
    <main class="admin-main-wrapper">
        
        <!-- Flash Messages -->
        <c:if test="${not empty shiftSuccessMsg}">
            <div class="alert alert-success alert-dismissible fade show d-flex align-items-center gap-2 mb-4" role="alert">
                <span class="material-symbols-outlined fs-5">check_circle</span>
                <div><c:out value="${shiftSuccessMsg}" /></div>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        </c:if>
        <c:if test="${not empty shiftErrorMsg}">
            <div class="alert alert-danger alert-dismissible fade show d-flex align-items-center gap-2 mb-4" role="alert">
                <span class="material-symbols-outlined fs-5">error</span>
                <div><c:out value="${shiftErrorMsg}" /></div>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        </c:if>

        <!-- Top Info Banner -->
        <div class="shift-info-banner">
            <span class="code-ref-pill">Bảng: work_schedules.shift_name + shop_settings</span>
            <h1 class="shift-banner-title">Khung giờ ca làm việc</h1>
            <p class="shift-banner-desc">Database có 3 ca cố định (MORNING, AFTERNOON, EVENING). Giờ của từng ca được lưu trong shop_settings.</p>
        </div>

        <!-- 3 Shift Cards Grid -->
        <div class="shift-cards-grid">

            <!-- Card 1: Ca sáng -->
            <c:set var="morning" value="${shiftStats['MORNING']}" />
            <div class="shift-card">
                <div>
                    <div class="shift-card-header">
                        <div>
                            <div class="shift-name-title">${morning.displayName}</div>
                            <div class="shift-key-code">MORNING • ${morning.shiftKey}</div>
                        </div>
                        <div class="shift-clock-btn">
                            <span class="material-symbols-outlined">schedule</span>
                        </div>
                    </div>

                    <div class="shift-hours-display">${morning.timeRange}</div>

                    <ul class="shift-stats-list">
                        <li class="shift-stat-item">
                            <span>Tổng lượt xếp ca:</span>
                            <span class="stat-value">${morning.totalCount}</span>
                        </li>
                        <li class="shift-stat-item">
                            <span>Đã xếp lịch:</span>
                            <span class="stat-value">${morning.scheduledCount}</span>
                        </li>
                        <li class="shift-stat-item">
                            <span>Có mặt:</span>
                            <span class="stat-value">${morning.presentCount}</span>
                        </li>
                        <li class="shift-stat-item">
                            <span>Hoàn thành:</span>
                            <span class="stat-value">${morning.completedCount}</span>
                        </li>
                        <li class="shift-stat-item">
                            <span>Vắng:</span>
                            <span class="stat-value">${morning.absentCount}</span>
                        </li>
                    </ul>
                </div>

                <div class="shift-card-footer">
                    <a href="${ctx}/admin/schedules?shift=MORNING" class="btn-view-schedule">Xem lịch phân ca</a>
                    <button type="button" class="btn-edit-hours" onclick="openEditHoursModal('shift_morning', 'Ca sáng', '${morning.timeRange}')">Sửa giờ ca</button>
                </div>
            </div>

            <!-- Card 2: Ca chiều -->
            <c:set var="afternoon" value="${shiftStats['AFTERNOON']}" />
            <div class="shift-card">
                <div>
                    <div class="shift-card-header">
                        <div>
                            <div class="shift-name-title">${afternoon.displayName}</div>
                            <div class="shift-key-code">AFTERNOON • ${afternoon.shiftKey}</div>
                        </div>
                        <div class="shift-clock-btn">
                            <span class="material-symbols-outlined">schedule</span>
                        </div>
                    </div>

                    <div class="shift-hours-display">${afternoon.timeRange}</div>

                    <ul class="shift-stats-list">
                        <li class="shift-stat-item">
                            <span>Tổng lượt xếp ca:</span>
                            <span class="stat-value">${afternoon.totalCount}</span>
                        </li>
                        <li class="shift-stat-item">
                            <span>Đã xếp lịch:</span>
                            <span class="stat-value">${afternoon.scheduledCount}</span>
                        </li>
                        <li class="shift-stat-item">
                            <span>Có mặt:</span>
                            <span class="stat-value">${afternoon.presentCount}</span>
                        </li>
                        <li class="shift-stat-item">
                            <span>Hoàn thành:</span>
                            <span class="stat-value">${afternoon.completedCount}</span>
                        </li>
                        <li class="shift-stat-item">
                            <span>Vắng:</span>
                            <span class="stat-value">${afternoon.absentCount}</span>
                        </li>
                    </ul>
                </div>

                <div class="shift-card-footer">
                    <a href="${ctx}/admin/schedules?shift=AFTERNOON" class="btn-view-schedule">Xem lịch phân ca</a>
                    <button type="button" class="btn-edit-hours" onclick="openEditHoursModal('shift_afternoon', 'Ca chiều', '${afternoon.timeRange}')">Sửa giờ ca</button>
                </div>
            </div>

            <!-- Card 3: Ca tối -->
            <c:set var="evening" value="${shiftStats['EVENING']}" />
            <div class="shift-card">
                <div>
                    <div class="shift-card-header">
                        <div>
                            <div class="shift-name-title">${evening.displayName}</div>
                            <div class="shift-key-code">EVENING • ${evening.shiftKey}</div>
                        </div>
                        <div class="shift-clock-btn">
                            <span class="material-symbols-outlined">schedule</span>
                        </div>
                    </div>

                    <div class="shift-hours-display">${evening.timeRange}</div>

                    <ul class="shift-stats-list">
                        <li class="shift-stat-item">
                            <span>Tổng lượt xếp ca:</span>
                            <span class="stat-value">${evening.totalCount}</span>
                        </li>
                        <li class="shift-stat-item">
                            <span>Đã xếp lịch:</span>
                            <span class="stat-value">${evening.scheduledCount}</span>
                        </li>
                        <li class="shift-stat-item">
                            <span>Có mặt:</span>
                            <span class="stat-value">${evening.presentCount}</span>
                        </li>
                        <li class="shift-stat-item">
                            <span>Hoàn thành:</span>
                            <span class="stat-value">${evening.completedCount}</span>
                        </li>
                        <li class="shift-stat-item">
                            <span>Vắng:</span>
                            <span class="stat-value">${evening.absentCount}</span>
                        </li>
                    </ul>
                </div>

                <div class="shift-card-footer">
                    <a href="${ctx}/admin/schedules?shift=EVENING" class="btn-view-schedule">Xem lịch phân ca</a>
                    <button type="button" class="btn-edit-hours" onclick="openEditHoursModal('shift_evening', 'Ca tối', '${evening.timeRange}')">Sửa giờ ca</button>
                </div>
            </div>

        </div>

    </main>

    <!-- Modal: Sửa giờ ca -->
    <div class="modal fade modal-aura" id="editHoursModal" tabindex="-1" aria-labelledby="editHoursModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" style="max-width: 420px;">
            <div class="modal-content">
                <form method="post" action="${ctx}/admin/shifts">
                    <input type="hidden" name="action" value="update_hours">
                    <input type="hidden" name="shiftKey" id="modalShiftKey" value="">

                    <div class="modal-header">
                        <h5 class="modal-title fw-bold" id="editHoursModalLabel">Cập nhật khung giờ ca</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>

                    <div class="modal-body">
                        <p class="text-muted small mb-3">Điều chỉnh thời gian bắt đầu và kết thúc của <b id="modalShiftNameTitle" class="text-dark"></b>. Khung giờ này sẽ hiển thị trên lịch làm việc của nhân viên.</p>

                        <div class="row g-3">
                            <div class="col-6">
                                <label for="modalStartTime" class="form-label small fw-semibold">Giờ bắt đầu</label>
                                <input type="time" class="form-control aura-input" id="modalStartTime" name="startTime" required>
                            </div>
                            <div class="col-6">
                                <label for="modalEndTime" class="form-label small fw-semibold">Giờ kết thúc</label>
                                <input type="time" class="form-control aura-input" id="modalEndTime" name="endTime" required>
                            </div>
                        </div>

                        <div class="mt-3 p-2 bg-light rounded text-center small text-muted">
                            Khung giờ định dạng: <b id="modalTimePreview" class="text-dark">--:--</b>
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

    <!-- Bootstrap 5 Bundle JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        const editHoursModal = new bootstrap.Modal(document.getElementById('editHoursModal'));
        const modalShiftKey = document.getElementById('modalShiftKey');
        const modalShiftNameTitle = document.getElementById('modalShiftNameTitle');
        const modalStartTime = document.getElementById('modalStartTime');
        const modalEndTime = document.getElementById('modalEndTime');
        const modalTimePreview = document.getElementById('modalTimePreview');

        function openEditHoursModal(shiftKey, shiftDisplayName, currentRange) {
            modalShiftKey.value = shiftKey;
            modalShiftNameTitle.textContent = shiftDisplayName;

            if (currentRange && currentRange.includes('-')) {
                const parts = currentRange.split('-');
                modalStartTime.value = parts[0].trim();
                modalEndTime.value = parts[1].trim();
            } else {
                modalStartTime.value = '08:00';
                modalEndTime.value = '12:00';
            }

            updatePreview();
            editHoursModal.show();
        }

        function updatePreview() {
            const start = modalStartTime.value || '--:--';
            const end = modalEndTime.value || '--:--';
            modalTimePreview.textContent = start + ' - ' + end;
        }

        modalStartTime.addEventListener('input', updatePreview);
        modalEndTime.addEventListener('input', updatePreview);
    </script>
</body>
</html>
