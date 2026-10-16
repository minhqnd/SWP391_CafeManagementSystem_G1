<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Ca làm việc &amp; Bàn giao két | Aura Café</title>

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
        <jsp:param name="activeMenu" value="schedules" />
    </jsp:include>

    <!-- Main Content Area -->
    <main class="admin-main-wrapper">

        <!-- Flash Messages -->
        <c:if test="${not empty scheduleSuccessMsg}">
            <div class="alert alert-success alert-dismissible fade show d-flex align-items-center gap-2 mb-4" role="alert">
                <span class="material-symbols-outlined fs-5">check_circle</span>
                <div><c:out value="${scheduleSuccessMsg}" /></div>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        </c:if>
        <c:if test="${not empty scheduleErrorMsg}">
            <div class="alert alert-danger alert-dismissible fade show d-flex align-items-center gap-2 mb-4" role="alert">
                <span class="material-symbols-outlined fs-5">error</span>
                <div><c:out value="${scheduleErrorMsg}" /></div>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        </c:if>

        <!-- Top Header Bar -->
        <div class="schedule-header-bar">
            <div>
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb small mb-1">
                        <li class="breadcrumb-item"><a href="${ctx}/admin-dashboard.jsp" class="text-decoration-none text-muted">Admin</a></li>
                        <li class="breadcrumb-item"><a href="${ctx}/admin/shifts" class="text-decoration-none text-muted">Nhân sự &amp; Ca làm</a></li>
                        <li class="breadcrumb-item active fw-semibold text-dark" aria-current="page">Quản lý Ca làm việc</li>
                    </ol>
                </nav>
                <h1 class="schedule-title">Quản lý Ca làm việc (Work Shift Management)</h1>
                <p class="schedule-subtitle">Điều phối nhân sự (Barista, Thu ngân, Shipper), giám sát điểm danh và kiểm toán bàn giao két tiền mặt</p>
            </div>

            <div class="d-flex align-items-center gap-3">
                <!-- View Mode Switch -->
                <div class="btn-group p-1 bg-white rounded-3 border" role="group">
                    <a href="${ctx}/admin/schedules?view=calendar&date=${selectedDate}&shift=${shiftFilter}&position=${positionFilter}&status=${statusFilter}&staffId=${staffIdFilter}"
                       class="btn btn-sm ${view == 'calendar' ? 'btn-dark fw-bold' : 'btn-light border-0 text-muted'}">
                        <span class="material-symbols-outlined fs-6 align-middle me-1">calendar_view_week</span> Lịch tuần
                    </a>
                    <a href="${ctx}/admin/schedules?view=board&date=${selectedDate}&shift=${shiftFilter}&position=${positionFilter}&status=${statusFilter}&staffId=${staffIdFilter}"
                       class="btn btn-sm ${view == 'board' ? 'btn-dark fw-bold' : 'btn-light border-0 text-muted'}">
                        <span class="material-symbols-outlined fs-6 align-middle me-1">view_column</span> Ca theo ngày
                    </a>
                    <a href="${ctx}/admin/schedules?view=table&date=${selectedDate}&shift=${shiftFilter}&position=${positionFilter}&status=${statusFilter}&staffId=${staffIdFilter}"
                       class="btn btn-sm ${view == 'table' ? 'btn-dark fw-bold' : 'btn-light border-0 text-muted'}">
                        <span class="material-symbols-outlined fs-6 align-middle me-1">table_rows</span> Kiểm toán két
                    </a>
                </div>

                <!-- Button Phân ca mới -->
                <button type="button" class="btn-aura-primary" onclick="openAddScheduleModal('${selectedDate}')">
                    <span class="material-symbols-outlined fs-5">add</span> Phân ca mới
                </button>
            </div>
        </div>

        <!-- KPI Metrics Grid (Real-time Shift & Cash Stats) -->
        <div class="kpi-metrics-grid">
            <!-- Metric 1: Total Shifts -->
            <div class="kpi-metric-card">
                <div class="kpi-icon-box kpi-icon-blue">
                    <span class="material-symbols-outlined fs-4">schedule</span>
                </div>
                <div>
                    <div class="kpi-val">${totalShifts}</div>
                    <div class="kpi-label">Tổng lượt ca phân bổ</div>
                </div>
            </div>

            <!-- Metric 2: Live Active Staff -->
            <div class="kpi-metric-card">
                <div class="kpi-icon-box kpi-icon-amber">
                    <span class="material-symbols-outlined fs-4">badge</span>
                </div>
                <div>
                    <div class="kpi-val text-warning d-flex align-items-center gap-2">
                        <span>${liveActiveCount}</span>
                        <c:if test="${liveActiveCount > 0}">
                            <span class="spinner-grow spinner-grow-sm text-warning" role="status"></span>
                        </c:if>
                    </div>
                    <div class="kpi-label">Đang có mặt trực ca</div>
                </div>
            </div>

            <!-- Metric 3: Completed Shifts -->
            <div class="kpi-metric-card">
                <div class="kpi-icon-box kpi-icon-green">
                    <span class="material-symbols-outlined fs-4">task_alt</span>
                </div>
                <div>
                    <div class="kpi-val text-success">${completedCount}</div>
                    <div class="kpi-label">Ca đã hoàn thành</div>
                </div>
            </div>

            <!-- Metric 4: Cash Handover & Discrepancies -->
            <div class="kpi-metric-card">
                <div class="kpi-icon-box ${cashDiscrepancyCount > 0 ? 'kpi-icon-rose' : 'kpi-icon-green'}">
                    <span class="material-symbols-outlined fs-4">point_of_sale</span>
                </div>
                <div>
                    <div class="kpi-val">
                        <fmt:formatNumber value="${totalCashHandover}" pattern="#,###" />đ
                    </div>
                    <div class="kpi-label">
                        <c:choose>
                            <c:when test="${cashDiscrepancyCount > 0}">
                                <span class="text-danger fw-bold">⚠️ Có ${cashDiscrepancyCount} ca lệch két!</span>
                            </c:when>
                            <c:otherwise>
                                <span>Tiền két bàn giao (Khớp 100%)</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
        </div>

        <!-- Filter & Date Navigator Toolbar -->
        <div class="filter-toolbar-card">
            <div class="week-navigation-group">
                <a href="${ctx}/admin/schedules?view=${view}&date=${prevWeekDate}&shift=${shiftFilter}&position=${positionFilter}&status=${statusFilter}&staffId=${staffIdFilter}"
                   class="btn-week-nav" title="Tuần trước">
                    <span class="material-symbols-outlined fs-6">chevron_left</span> Tuần trước
                </a>
                <a href="${ctx}/admin/schedules?view=${view}&date=&shift=${shiftFilter}&position=${positionFilter}&status=${statusFilter}&staffId=${staffIdFilter}"
                   class="btn-week-nav" title="Trở về ngày hiện tại">
                    Hôm nay
                </a>
                <a href="${ctx}/admin/schedules?view=${view}&date=${nextWeekDate}&shift=${shiftFilter}&position=${positionFilter}&status=${statusFilter}&staffId=${staffIdFilter}"
                   class="btn-week-nav" title="Tuần sau">
                    Tuần sau <span class="material-symbols-outlined fs-6">chevron_right</span>
                </a>
                <span class="week-range-text">
                    <span class="text-muted fw-normal">Đang xem:</span> <b>${selectedDate}</b>
                </span>
            </div>

            <form method="get" action="${ctx}/admin/schedules" class="d-flex align-items-center gap-2 flex-wrap m-0">
                <input type="hidden" name="view" value="${view}">
                
                <input type="date" name="date" class="form-control form-control-sm" style="width: 140px;"
                       value="${selectedDate}" onchange="this.form.submit()" title="Chọn ngày">

                <select name="shift" class="form-select form-select-sm" style="width: 130px;" onchange="this.form.submit()">
                    <option value="ALL" ${shiftFilter == 'ALL' ? 'selected' : ''}>Tất cả ca</option>
                    <option value="MORNING" ${shiftFilter == 'MORNING' ? 'selected' : ''}>Ca sáng</option>
                    <option value="AFTERNOON" ${shiftFilter == 'AFTERNOON' ? 'selected' : ''}>Ca chiều</option>
                    <option value="EVENING" ${shiftFilter == 'EVENING' ? 'selected' : ''}>Ca tối</option>
                </select>

                <select name="position" class="form-select form-select-sm" style="width: 135px;" onchange="this.form.submit()">
                    <option value="ALL" ${positionFilter == 'ALL' ? 'selected' : ''}>Tất cả vị trí</option>
                    <option value="BARISTA" ${positionFilter == 'BARISTA' ? 'selected' : ''}>Pha chế</option>
                    <option value="CASHIER" ${positionFilter == 'CASHIER' ? 'selected' : ''}>Thu ngân</option>
                    <option value="SHIPPER" ${positionFilter == 'SHIPPER' ? 'selected' : ''}>Giao hàng</option>
                    <option value="MANAGER" ${positionFilter == 'MANAGER' ? 'selected' : ''}>Quản lý</option>
                </select>

                <select name="status" class="form-select form-select-sm" style="width: 140px;" onchange="this.form.submit()">
                    <option value="ALL" ${statusFilter == 'ALL' ? 'selected' : ''}>Tất cả trạng thái</option>
                    <option value="SCHEDULED" ${statusFilter == 'SCHEDULED' ? 'selected' : ''}>Đã xếp lịch</option>
                    <option value="PRESENT" ${statusFilter == 'PRESENT' ? 'selected' : ''}>Có mặt</option>
                    <option value="COMPLETED" ${statusFilter == 'COMPLETED' ? 'selected' : ''}>Hoàn thành</option>
                    <option value="ABSENT" ${statusFilter == 'ABSENT' ? 'selected' : ''}>Vắng mặt</option>
                </select>

                <c:if test="${shiftFilter != 'ALL' || positionFilter != 'ALL' || statusFilter != 'ALL' || not empty staffIdFilter}">
                    <a href="${ctx}/admin/schedules?view=${view}&date=${selectedDate}" class="btn btn-outline-secondary btn-sm" title="Xóa bộ lọc">
                        <span class="material-symbols-outlined fs-6 align-middle">close</span>
                    </a>
                </c:if>
            </form>
        </div>

        <!-- ========================================== -->
        <!-- VIEW 1: DAY SHIFT LANES (BOARD VIEW)       -->
        <!-- ========================================== -->
        <c:if test="${view == 'board'}">
            <div class="shift-lanes-container">
                <!-- Lane 1: Ca sáng -->
                <div class="shift-lane">
                    <div class="shift-lane-header">
                        <div>
                            <div class="fw-bold text-dark">Ca sáng</div>
                            <div class="text-muted small">${settings.shiftMorning}</div>
                        </div>
                        <button type="button" class="btn btn-sm btn-outline-dark" onclick="openAddScheduleModal('${selectedDate}', 'MORNING')">
                            + Thêm
                        </button>
                    </div>
                    <div class="shift-lane-body">
                        <c:set var="mList" value="${calendarSlotMap[selectedDate.concat('_MORNING')]}" />
                        <c:forEach items="${mList}" var="item">
                            <div class="lane-staff-card">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <div class="d-flex align-items-center gap-2">
                                        <div class="staff-avatar-initials">${item.initials}</div>
                                        <div>
                                            <div class="fw-bold text-dark lh-1">${item.userName}</div>
                                            <span class="position-pill position-${fn:toLowerCase(item.userPosition)}">${item.positionDisplayName}</span>
                                        </div>
                                    </div>
                                    <span class="${item.statusBadgeClass}">${item.statusDisplayName}</span>
                                </div>

                                <div class="small text-muted mb-2">
                                    <span>Vào: <b>${item.checkInFormatted}</b></span> &bull; 
                                    <span>Ra: <b>${item.checkOutFormatted}</b></span>
                                </div>

                                <c:if test="${not empty item.startingCash || not empty item.endingCashActual}">
                                    <div class="cash-handover-box">
                                        <div class="cash-flow-line">
                                            <span>Két đầu ca:</span>
                                            <b><fmt:formatNumber value="${item.startingCash}" pattern="#,###" />đ</b>
                                        </div>
                                        <c:if test="${not empty item.endingCashActual}">
                                            <div class="cash-flow-line">
                                                <span>Két kết ca:</span>
                                                <b><fmt:formatNumber value="${item.endingCashActual}" pattern="#,###" />đ</b>
                                            </div>
                                            <div class="mt-1">${item.cashStatusBadge}</div>
                                        </c:if>
                                    </div>
                                </c:if>

                                <div class="d-flex align-items-center justify-content-end gap-2 mt-3 pt-2 border-top">
                                    <c:if test="${item.status == 'SCHEDULED'}">
                                        <button type="button" class="btn btn-sm btn-success py-1 px-2"
                                                onclick="openCheckInModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                            Check-in
                                        </button>
                                    </c:if>
                                    <c:if test="${item.status == 'PRESENT'}">
                                        <button type="button" class="btn btn-sm btn-primary py-1 px-2"
                                                onclick="openCloseShiftModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                            Chốt ca &amp; Két
                                        </button>
                                    </c:if>
                                    <button type="button" class="btn btn-sm btn-light border py-1 px-2"
                                            onclick="openEditScheduleModal('${item.id}', '${item.userId}', '${item.workDateFormatted}', '${item.shiftName}', '${item.status}', '${item.note}', '${item.startingCash}', '${item.endingCashActual}')">
                                        Sửa
                                    </button>
                                </div>
                            </div>
                        </c:forEach>
                        <c:if test="${empty mList}">
                            <div class="text-center py-4 text-muted small">Chưa xếp nhân sự cho ca sáng.</div>
                        </c:if>
                    </div>
                </div>

                <!-- Lane 2: Ca chiều -->
                <div class="shift-lane">
                    <div class="shift-lane-header">
                        <div>
                            <div class="fw-bold text-dark">Ca chiều</div>
                            <div class="text-muted small">${settings.shiftAfternoon}</div>
                        </div>
                        <button type="button" class="btn btn-sm btn-outline-dark" onclick="openAddScheduleModal('${selectedDate}', 'AFTERNOON')">
                            + Thêm
                        </button>
                    </div>
                    <div class="shift-lane-body">
                        <c:set var="aList" value="${calendarSlotMap[selectedDate.concat('_AFTERNOON')]}" />
                        <c:forEach items="${aList}" var="item">
                            <div class="lane-staff-card">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <div class="d-flex align-items-center gap-2">
                                        <div class="staff-avatar-initials">${item.initials}</div>
                                        <div>
                                            <div class="fw-bold text-dark lh-1">${item.userName}</div>
                                            <span class="position-pill position-${fn:toLowerCase(item.userPosition)}">${item.positionDisplayName}</span>
                                        </div>
                                    </div>
                                    <span class="${item.statusBadgeClass}">${item.statusDisplayName}</span>
                                </div>

                                <div class="small text-muted mb-2">
                                    <span>Vào: <b>${item.checkInFormatted}</b></span> &bull; 
                                    <span>Ra: <b>${item.checkOutFormatted}</b></span>
                                </div>

                                <c:if test="${not empty item.startingCash || not empty item.endingCashActual}">
                                    <div class="cash-handover-box">
                                        <div class="cash-flow-line">
                                            <span>Két đầu ca:</span>
                                            <b><fmt:formatNumber value="${item.startingCash}" pattern="#,###" />đ</b>
                                        </div>
                                        <c:if test="${not empty item.endingCashActual}">
                                            <div class="cash-flow-line">
                                                <span>Két kết ca:</span>
                                                <b><fmt:formatNumber value="${item.endingCashActual}" pattern="#,###" />đ</b>
                                            </div>
                                            <div class="mt-1">${item.cashStatusBadge}</div>
                                        </c:if>
                                    </div>
                                </c:if>

                                <div class="d-flex align-items-center justify-content-end gap-2 mt-3 pt-2 border-top">
                                    <c:if test="${item.status == 'SCHEDULED'}">
                                        <button type="button" class="btn btn-sm btn-success py-1 px-2"
                                                onclick="openCheckInModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                            Check-in
                                        </button>
                                    </c:if>
                                    <c:if test="${item.status == 'PRESENT'}">
                                        <button type="button" class="btn btn-sm btn-primary py-1 px-2"
                                                onclick="openCloseShiftModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                            Chốt ca &amp; Két
                                        </button>
                                    </c:if>
                                    <button type="button" class="btn btn-sm btn-light border py-1 px-2"
                                            onclick="openEditScheduleModal('${item.id}', '${item.userId}', '${item.workDateFormatted}', '${item.shiftName}', '${item.status}', '${item.note}', '${item.startingCash}', '${item.endingCashActual}')">
                                        Sửa
                                    </button>
                                </div>
                            </div>
                        </c:forEach>
                        <c:if test="${empty aList}">
                            <div class="text-center py-4 text-muted small">Chưa xếp nhân sự cho ca chiều.</div>
                        </c:if>
                    </div>
                </div>

                <!-- Lane 3: Ca tối -->
                <div class="shift-lane">
                    <div class="shift-lane-header">
                        <div>
                            <div class="fw-bold text-dark">Ca tối</div>
                            <div class="text-muted small">${settings.shiftEvening}</div>
                        </div>
                        <button type="button" class="btn btn-sm btn-outline-dark" onclick="openAddScheduleModal('${selectedDate}', 'EVENING')">
                            + Thêm
                        </button>
                    </div>
                    <div class="shift-lane-body">
                        <c:set var="eList" value="${calendarSlotMap[selectedDate.concat('_EVENING')]}" />
                        <c:forEach items="${eList}" var="item">
                            <div class="lane-staff-card">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <div class="d-flex align-items-center gap-2">
                                        <div class="staff-avatar-initials">${item.initials}</div>
                                        <div>
                                            <div class="fw-bold text-dark lh-1">${item.userName}</div>
                                            <span class="position-pill position-${fn:toLowerCase(item.userPosition)}">${item.positionDisplayName}</span>
                                        </div>
                                    </div>
                                    <span class="${item.statusBadgeClass}">${item.statusDisplayName}</span>
                                </div>

                                <div class="small text-muted mb-2">
                                    <span>Vào: <b>${item.checkInFormatted}</b></span> &bull; 
                                    <span>Ra: <b>${item.checkOutFormatted}</b></span>
                                </div>

                                <c:if test="${not empty item.startingCash || not empty item.endingCashActual}">
                                    <div class="cash-handover-box">
                                        <div class="cash-flow-line">
                                            <span>Két đầu ca:</span>
                                            <b><fmt:formatNumber value="${item.startingCash}" pattern="#,###" />đ</b>
                                        </div>
                                        <c:if test="${not empty item.endingCashActual}">
                                            <div class="cash-flow-line">
                                                <span>Két kết ca:</span>
                                                <b><fmt:formatNumber value="${item.endingCashActual}" pattern="#,###" />đ</b>
                                            </div>
                                            <div class="mt-1">${item.cashStatusBadge}</div>
                                        </c:if>
                                    </div>
                                </c:if>

                                <div class="d-flex align-items-center justify-content-end gap-2 mt-3 pt-2 border-top">
                                    <c:if test="${item.status == 'SCHEDULED'}">
                                        <button type="button" class="btn btn-sm btn-success py-1 px-2"
                                                onclick="openCheckInModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                            Check-in
                                        </button>
                                    </c:if>
                                    <c:if test="${item.status == 'PRESENT'}">
                                        <button type="button" class="btn btn-sm btn-primary py-1 px-2"
                                                onclick="openCloseShiftModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                            Chốt ca &amp; Két
                                        </button>
                                    </c:if>
                                    <button type="button" class="btn btn-sm btn-light border py-1 px-2"
                                            onclick="openEditScheduleModal('${item.id}', '${item.userId}', '${item.workDateFormatted}', '${item.shiftName}', '${item.status}', '${item.note}', '${item.startingCash}', '${item.endingCashActual}')">
                                        Sửa
                                    </button>
                                </div>
                            </div>
                        </c:forEach>
                        <c:if test="${empty eList}">
                            <div class="text-center py-4 text-muted small">Chưa xếp nhân sự cho ca tối.</div>
                        </c:if>
                    </div>
                </div>
            </div>
        </c:if>

        <!-- ========================================== -->
        <!-- VIEW 2: WEEKLY CALENDAR MATRIX             -->
        <!-- ========================================== -->
        <c:if test="${view == 'calendar'}">
            <div class="calendar-grid-container mb-4">
                <table class="calendar-table">
                    <thead>
                        <tr>
                            <th class="shift-row-header" style="width: 150px;">
                                <div class="text-uppercase text-muted small fw-bold">Khung ca / Thứ</div>
                            </th>
                            <c:forEach items="${weekDays}" var="day" varStatus="loop">
                                <c:set var="isToday" value="${day.toString() == selectedDate}" />
                                <th class="${isToday ? 'day-header-today' : ''}" style="width: calc((100% - 150px) / 7);">
                                    <div class="day-header-title">
                                        <c:choose>
                                            <c:when test="${loop.index == 0}">Thứ 2</c:when>
                                            <c:when test="${loop.index == 1}">Thứ 3</c:when>
                                            <c:when test="${loop.index == 2}">Thứ 4</c:when>
                                            <c:when test="${loop.index == 3}">Thứ 5</c:when>
                                            <c:when test="${loop.index == 4}">Thứ 6</c:when>
                                            <c:when test="${loop.index == 5}">Thứ 7</c:when>
                                            <c:when test="${loop.index == 6}">Chủ Nhật</c:when>
                                        </c:choose>
                                        <c:if test="${isToday}">
                                            <span class="badge bg-danger ms-1" style="font-size: 0.65rem;">Hôm nay</span>
                                        </c:if>
                                    </div>
                                    <div class="day-header-date">${day.toString()}</div>
                                </th>
                            </c:forEach>
                        </tr>
                    </thead>
                    <tbody>
                        <!-- CA SÁNG -->
                        <c:if test="${shiftFilter == 'ALL' || shiftFilter == 'MORNING'}">
                            <tr>
                                <td class="shift-row-header">
                                    <div class="shift-header-name">Ca sáng</div>
                                    <div class="shift-header-time">${settings.shiftMorning}</div>
                                </td>
                                <c:forEach items="${weekDays}" var="day">
                                    <c:set var="slotKey" value="${day.toString()}_MORNING" />
                                    <c:set var="slotList" value="${calendarSlotMap[slotKey]}" />
                                    <td class="calendar-slot-cell ${day.toString() == selectedDate ? 'today-cell' : ''}">
                                        <c:forEach items="${slotList}" var="item">
                                            <div class="slot-staff-card">
                                                <div class="staff-name-line">
                                                    <span><c:out value="${item.userName}" /></span>
                                                    <div class="dropdown d-inline-block">
                                                        <button class="slot-action-dots" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                                                            <span class="material-symbols-outlined fs-6">more_vert</span>
                                                        </button>
                                                        <ul class="dropdown-menu dropdown-menu-end shadow-sm small">
                                                            <li><h6 class="dropdown-header">Thao tác ca</h6></li>
                                                            <c:if test="${item.status == 'SCHEDULED'}">
                                                                <li>
                                                                    <a class="dropdown-item text-success" href="javascript:void(0)"
                                                                       onclick="openCheckInModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                                                        <span class="material-symbols-outlined fs-6 align-middle">login</span> Check-in trực ca
                                                                    </a>
                                                                </li>
                                                            </c:if>
                                                            <c:if test="${item.status == 'PRESENT'}">
                                                                <li>
                                                                    <a class="dropdown-item text-primary" href="javascript:void(0)"
                                                                       onclick="openCloseShiftModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                                                        <span class="material-symbols-outlined fs-6 align-middle">point_of_sale</span> Chốt ca &amp; Két
                                                                    </a>
                                                                </li>
                                                            </c:if>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0">
                                                                    <input type="hidden" name="action" value="quick_status">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="status" value="ABSENT">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="${view}">
                                                                    <button type="submit" class="dropdown-item text-danger"><span class="material-symbols-outlined fs-6 align-middle">close</span> Báo vắng mặt</button>
                                                                </form>
                                                            </li>
                                                            <li><hr class="dropdown-divider"></li>
                                                            <li>
                                                                <a class="dropdown-item" href="javascript:void(0)"
                                                                   onclick="openEditScheduleModal('${item.id}', '${item.userId}', '${item.workDateFormatted}', '${item.shiftName}', '${item.status}', '${item.note}', '${item.startingCash}', '${item.endingCashActual}')">
                                                                    <span class="material-symbols-outlined fs-6 align-middle">edit</span> Chỉnh sửa
                                                                </a>
                                                            </li>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0" onsubmit="return confirm('Bạn có chắc muốn hủy ca làm này?');">
                                                                    <input type="hidden" name="action" value="delete">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="${view}">
                                                                    <button type="submit" class="dropdown-item text-danger"><span class="material-symbols-outlined fs-6 align-middle">delete</span> Hủy ca</button>
                                                                </form>
                                                            </li>
                                                        </ul>
                                                    </div>
                                                </div>
                                                <div class="d-flex align-items-center gap-1 mb-1">
                                                    <span class="position-pill position-${fn:toLowerCase(item.userPosition)}">${item.positionDisplayName}</span>
                                                    <span class="${item.statusBadgeClass}">${item.statusDisplayName}</span>
                                                </div>
                                                <c:if test="${not empty item.startingCash}">
                                                    <div class="text-muted" style="font-size: 0.72rem;">Két: <fmt:formatNumber value="${item.startingCash}" pattern="#,###" />đ</div>
                                                </c:if>
                                            </div>
                                        </c:forEach>
                                        <button type="button" class="btn-slot-quick-add" onclick="openAddScheduleModal('${day.toString()}', 'MORNING')">
                                            <span class="material-symbols-outlined" style="font-size: 14px;">add</span> Xếp ca
                                        </button>
                                    </td>
                                </c:forEach>
                            </tr>
                        </c:if>

                        <!-- CA CHIỀU -->
                        <c:if test="${shiftFilter == 'ALL' || shiftFilter == 'AFTERNOON'}">
                            <tr>
                                <td class="shift-row-header">
                                    <div class="shift-header-name">Ca chiều</div>
                                    <div class="shift-header-time">${settings.shiftAfternoon}</div>
                                </td>
                                <c:forEach items="${weekDays}" var="day">
                                    <c:set var="slotKey" value="${day.toString()}_AFTERNOON" />
                                    <c:set var="slotList" value="${calendarSlotMap[slotKey]}" />
                                    <td class="calendar-slot-cell ${day.toString() == selectedDate ? 'today-cell' : ''}">
                                        <c:forEach items="${slotList}" var="item">
                                            <div class="slot-staff-card">
                                                <div class="staff-name-line">
                                                    <span><c:out value="${item.userName}" /></span>
                                                    <div class="dropdown d-inline-block">
                                                        <button class="slot-action-dots" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                                                            <span class="material-symbols-outlined fs-6">more_vert</span>
                                                        </button>
                                                        <ul class="dropdown-menu dropdown-menu-end shadow-sm small">
                                                            <li><h6 class="dropdown-header">Thao tác ca</h6></li>
                                                            <c:if test="${item.status == 'SCHEDULED'}">
                                                                <li>
                                                                    <a class="dropdown-item text-success" href="javascript:void(0)"
                                                                       onclick="openCheckInModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                                                        <span class="material-symbols-outlined fs-6 align-middle">login</span> Check-in trực ca
                                                                    </a>
                                                                </li>
                                                            </c:if>
                                                            <c:if test="${item.status == 'PRESENT'}">
                                                                <li>
                                                                    <a class="dropdown-item text-primary" href="javascript:void(0)"
                                                                       onclick="openCloseShiftModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                                                        <span class="material-symbols-outlined fs-6 align-middle">point_of_sale</span> Chốt ca &amp; Két
                                                                    </a>
                                                                </li>
                                                            </c:if>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0">
                                                                    <input type="hidden" name="action" value="quick_status">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="status" value="ABSENT">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="${view}">
                                                                    <button type="submit" class="dropdown-item text-danger"><span class="material-symbols-outlined fs-6 align-middle">close</span> Báo vắng mặt</button>
                                                                </form>
                                                            </li>
                                                            <li><hr class="dropdown-divider"></li>
                                                            <li>
                                                                <a class="dropdown-item" href="javascript:void(0)"
                                                                   onclick="openEditScheduleModal('${item.id}', '${item.userId}', '${item.workDateFormatted}', '${item.shiftName}', '${item.status}', '${item.note}', '${item.startingCash}', '${item.endingCashActual}')">
                                                                    <span class="material-symbols-outlined fs-6 align-middle">edit</span> Chỉnh sửa
                                                                </a>
                                                            </li>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0" onsubmit="return confirm('Bạn có chắc muốn hủy ca làm này?');">
                                                                    <input type="hidden" name="action" value="delete">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="${view}">
                                                                    <button type="submit" class="dropdown-item text-danger"><span class="material-symbols-outlined fs-6 align-middle">delete</span> Hủy ca</button>
                                                                </form>
                                                            </li>
                                                        </ul>
                                                    </div>
                                                </div>
                                                <div class="d-flex align-items-center gap-1 mb-1">
                                                    <span class="position-pill position-${fn:toLowerCase(item.userPosition)}">${item.positionDisplayName}</span>
                                                    <span class="${item.statusBadgeClass}">${item.statusDisplayName}</span>
                                                </div>
                                                <c:if test="${not empty item.startingCash}">
                                                    <div class="text-muted" style="font-size: 0.72rem;">Két: <fmt:formatNumber value="${item.startingCash}" pattern="#,###" />đ</div>
                                                </c:if>
                                            </div>
                                        </c:forEach>
                                        <button type="button" class="btn-slot-quick-add" onclick="openAddScheduleModal('${day.toString()}', 'AFTERNOON')">
                                            <span class="material-symbols-outlined" style="font-size: 14px;">add</span> Xếp ca
                                        </button>
                                    </td>
                                </c:forEach>
                            </tr>
                        </c:if>

                        <!-- CA TỐI -->
                        <c:if test="${shiftFilter == 'ALL' || shiftFilter == 'EVENING'}">
                            <tr>
                                <td class="shift-row-header">
                                    <div class="shift-header-name">Ca tối</div>
                                    <div class="shift-header-time">${settings.shiftEvening}</div>
                                </td>
                                <c:forEach items="${weekDays}" var="day">
                                    <c:set var="slotKey" value="${day.toString()}_EVENING" />
                                    <c:set var="slotList" value="${calendarSlotMap[slotKey]}" />
                                    <td class="calendar-slot-cell ${day.toString() == selectedDate ? 'today-cell' : ''}">
                                        <c:forEach items="${slotList}" var="item">
                                            <div class="slot-staff-card">
                                                <div class="staff-name-line">
                                                    <span><c:out value="${item.userName}" /></span>
                                                    <div class="dropdown d-inline-block">
                                                        <button class="slot-action-dots" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                                                            <span class="material-symbols-outlined fs-6">more_vert</span>
                                                        </button>
                                                        <ul class="dropdown-menu dropdown-menu-end shadow-sm small">
                                                            <li><h6 class="dropdown-header">Thao tác ca</h6></li>
                                                            <c:if test="${item.status == 'SCHEDULED'}">
                                                                <li>
                                                                    <a class="dropdown-item text-success" href="javascript:void(0)"
                                                                       onclick="openCheckInModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                                                        <span class="material-symbols-outlined fs-6 align-middle">login</span> Check-in trực ca
                                                                    </a>
                                                                </li>
                                                            </c:if>
                                                            <c:if test="${item.status == 'PRESENT'}">
                                                                <li>
                                                                    <a class="dropdown-item text-primary" href="javascript:void(0)"
                                                                       onclick="openCloseShiftModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                                                        <span class="material-symbols-outlined fs-6 align-middle">point_of_sale</span> Chốt ca &amp; Két
                                                                    </a>
                                                                </li>
                                                            </c:if>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0">
                                                                    <input type="hidden" name="action" value="quick_status">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="status" value="ABSENT">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="${view}">
                                                                    <button type="submit" class="dropdown-item text-danger"><span class="material-symbols-outlined fs-6 align-middle">close</span> Báo vắng mặt</button>
                                                                </form>
                                                            </li>
                                                            <li><hr class="dropdown-divider"></li>
                                                            <li>
                                                                <a class="dropdown-item" href="javascript:void(0)"
                                                                   onclick="openEditScheduleModal('${item.id}', '${item.userId}', '${item.workDateFormatted}', '${item.shiftName}', '${item.status}', '${item.note}', '${item.startingCash}', '${item.endingCashActual}')">
                                                                    <span class="material-symbols-outlined fs-6 align-middle">edit</span> Chỉnh sửa
                                                                </a>
                                                            </li>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0" onsubmit="return confirm('Bạn có chắc muốn hủy ca làm này?');">
                                                                    <input type="hidden" name="action" value="delete">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="${view}">
                                                                    <button type="submit" class="dropdown-item text-danger"><span class="material-symbols-outlined fs-6 align-middle">delete</span> Hủy ca</button>
                                                                </form>
                                                            </li>
                                                        </ul>
                                                    </div>
                                                </div>
                                                <div class="d-flex align-items-center gap-1 mb-1">
                                                    <span class="position-pill position-${fn:toLowerCase(item.userPosition)}">${item.positionDisplayName}</span>
                                                    <span class="${item.statusBadgeClass}">${item.statusDisplayName}</span>
                                                </div>
                                                <c:if test="${not empty item.startingCash}">
                                                    <div class="text-muted" style="font-size: 0.72rem;">Két: <fmt:formatNumber value="${item.startingCash}" pattern="#,###" />đ</div>
                                                </c:if>
                                            </div>
                                        </c:forEach>
                                        <button type="button" class="btn-slot-quick-add" onclick="openAddScheduleModal('${day.toString()}', 'EVENING')">
                                            <span class="material-symbols-outlined" style="font-size: 14px;">add</span> Xếp ca
                                        </button>
                                    </td>
                                </c:forEach>
                            </tr>
                        </c:if>

                    </tbody>
                </table>
            </div>
        </c:if>

        <!-- ========================================== -->
        <!-- VIEW 3: CASH HANDOVER & AUDIT TABLE        -->
        <!-- ========================================== -->
        <c:if test="${view == 'table'}">
            <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-4">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light text-muted small text-uppercase">
                            <tr>
                                <th class="ps-3 py-3">Ngày làm</th>
                                <th>Ca trực &amp; Giờ</th>
                                <th>Nhân viên</th>
                                <th>Vị trí</th>
                                <th>Điểm danh</th>
                                <th>Trạng thái</th>
                                <th>Két đầu ca</th>
                                <th>Két chốt thực tế</th>
                                <th>Chênh lệch két</th>
                                <th class="text-end pe-3">Hành động</th>
                            </tr>
                        </thead>
                        <tbody class="small">
                            <c:forEach items="${scheduleList}" var="item">
                                <tr>
                                    <td class="ps-3 fw-bold text-dark">${item.workDateDisplay}</td>
                                    <td>
                                        <span class="fw-semibold text-dark">${item.shiftDisplayName}</span>
                                        <div class="text-muted" style="font-size: 0.75rem;">
                                            <c:choose>
                                                <c:when test="${item.shiftName == 'MORNING'}">${settings.shiftMorning}</c:when>
                                                <c:when test="${item.shiftName == 'AFTERNOON'}">${settings.shiftAfternoon}</c:when>
                                                <c:when test="${item.shiftName == 'EVENING'}">${settings.shiftEvening}</c:when>
                                            </c:choose>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="fw-bold text-dark"><c:out value="${item.userName}" /></div>
                                        <div class="text-muted" style="font-size: 0.75rem;">SĐT: ${item.userPhone}</div>
                                    </td>
                                    <td>
                                        <span class="position-pill position-${fn:toLowerCase(item.userPosition)}">${item.positionDisplayName}</span>
                                    </td>
                                    <td>
                                        <div>Vào: <span class="fw-semibold text-dark">${item.checkInFormatted}</span></div>
                                        <div>Ra: <span class="fw-semibold text-dark">${item.checkOutFormatted}</span></div>
                                    </td>
                                    <td>
                                        <span class="${item.statusBadgeClass}">${item.statusDisplayName}</span>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty item.startingCash}">
                                                <b><fmt:formatNumber value="${item.startingCash}" pattern="#,###" />đ</b>
                                            </c:when>
                                            <c:otherwise><span class="text-muted">--</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty item.endingCashActual}">
                                                <b><fmt:formatNumber value="${item.endingCashActual}" pattern="#,###" />đ</b>
                                            </c:when>
                                            <c:otherwise><span class="text-muted">Chưa chốt</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        ${item.cashStatusBadge}
                                    </td>
                                    <td class="text-end pe-3">
                                        <div class="btn-group btn-group-sm">
                                            <c:if test="${item.status == 'SCHEDULED'}">
                                                <button type="button" class="btn btn-outline-success" title="Check-in"
                                                        onclick="openCheckInModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                                    <span class="material-symbols-outlined fs-6 align-middle">login</span>
                                                </button>
                                            </c:if>
                                            <c:if test="${item.status == 'PRESENT'}">
                                                <button type="button" class="btn btn-outline-primary" title="Chốt ca &amp; két"
                                                        onclick="openCloseShiftModal('${item.id}', '${item.userName}', '${item.startingCash}')">
                                                    <span class="material-symbols-outlined fs-6 align-middle">point_of_sale</span>
                                                </button>
                                            </c:if>
                                            <button type="button" class="btn btn-outline-secondary" title="Sửa ca"
                                                    onclick="openEditScheduleModal('${item.id}', '${item.userId}', '${item.workDateFormatted}', '${item.shiftName}', '${item.status}', '${item.note}', '${item.startingCash}', '${item.endingCashActual}')">
                                                <span class="material-symbols-outlined fs-6 align-middle">edit</span>
                                            </button>
                                            <form method="post" action="${ctx}/admin/schedules" class="d-inline m-0" onsubmit="return confirm('Bạn có chắc muốn hủy ca làm này?');">
                                                <input type="hidden" name="action" value="delete">
                                                <input type="hidden" name="id" value="${item.id}">
                                                <input type="hidden" name="returnDate" value="${selectedDate}">
                                                <input type="hidden" name="returnView" value="${view}">
                                                <button type="submit" class="btn btn-outline-danger" title="Xóa ca">
                                                    <span class="material-symbols-outlined fs-6 align-middle">delete</span>
                                                </button>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty scheduleList}">
                                <tr>
                                    <td colspan="10" class="text-center py-5 text-muted">
                                        <span class="material-symbols-outlined fs-1 text-secondary mb-2">event_busy</span>
                                        <div>Không tìm thấy ca làm việc nào phù hợp với bộ lọc trong khoảng thời gian này.</div>
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </c:if>

    </main>

    <!-- Modal 1: Phân ca mới -->
    <div class="modal fade modal-aura" id="addScheduleModal" tabindex="-1" aria-labelledby="addScheduleModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" style="max-width: 480px;">
            <div class="modal-content">
                <form method="post" action="${ctx}/admin/schedules">
                    <input type="hidden" name="action" value="add">
                    <input type="hidden" name="returnDate" value="${selectedDate}">
                    <input type="hidden" name="returnView" value="${view}">

                    <div class="modal-header">
                        <h5 class="modal-title fw-bold" id="addScheduleModalLabel">Phân ca làm việc mới</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>

                    <div class="modal-body">
                        <!-- Chọn nhân viên -->
                        <div class="mb-3">
                            <label for="addUserId" class="form-label small fw-semibold">Nhân viên trực ca <span class="text-danger">*</span></label>
                            <select class="form-select aura-input" id="addUserId" name="userId" required>
                                <option value="">-- Chọn nhân viên --</option>
                                <c:forEach items="${staffList}" var="staff">
                                    <option value="${staff.id}">
                                        ${staff.fullName} (${staff.position} - ${staff.phone})
                                    </option>
                                </c:forEach>
                            </select>
                        </div>

                        <!-- Chọn ngày làm việc -->
                        <div class="mb-3">
                            <label for="addWorkDate" class="form-label small fw-semibold">Ngày làm việc <span class="text-danger">*</span></label>
                            <input type="date" class="form-control aura-input" id="addWorkDate" name="workDate" required value="${selectedDate}">
                        </div>

                        <!-- Chọn ca làm việc -->
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Khung giờ ca <span class="text-danger">*</span></label>
                            <div class="d-flex flex-column gap-2">
                                <label class="border rounded-3 p-2 px-3 d-flex align-items-center justify-content-between cursor-pointer">
                                    <div class="d-flex align-items-center gap-2">
                                        <input type="radio" name="shiftName" value="MORNING" id="shiftMorningRadio" checked>
                                        <span class="fw-semibold text-dark">Ca sáng</span>
                                    </div>
                                    <span class="text-muted small">${settings.shiftMorning}</span>
                                </label>
                                <label class="border rounded-3 p-2 px-3 d-flex align-items-center justify-content-between cursor-pointer">
                                    <div class="d-flex align-items-center gap-2">
                                        <input type="radio" name="shiftName" value="AFTERNOON" id="shiftAfternoonRadio">
                                        <span class="fw-semibold text-dark">Ca chiều</span>
                                    </div>
                                    <span class="text-muted small">${settings.shiftAfternoon}</span>
                                </label>
                                <label class="border rounded-3 p-2 px-3 d-flex align-items-center justify-content-between cursor-pointer">
                                    <div class="d-flex align-items-center gap-2">
                                        <input type="radio" name="shiftName" value="EVENING" id="shiftEveningRadio">
                                        <span class="fw-semibold text-dark">Ca tối</span>
                                    </div>
                                    <span class="text-muted small">${settings.shiftEvening}</span>
                                </label>
                            </div>
                        </div>

                        <!-- Tiền bàn giao đầu ca (áp dụng cho Thu ngân) -->
                        <div class="mb-3">
                            <label for="addStartingCash" class="form-label small fw-semibold">Tiền bàn giao két đầu ca (VNĐ)</label>
                            <input type="number" class="form-control aura-input" id="addStartingCash" name="startingCash" placeholder="VD: 500000" min="0" step="1000">
                            <div class="form-text">Bàn giao tiền lẻ cho Thu ngân đầu ca quản lý két tiền.</div>
                        </div>

                        <!-- Ghi chú -->
                        <div class="mb-2">
                            <label for="addNote" class="form-label small fw-semibold">Ghi chú công việc</label>
                            <textarea class="form-control aura-input" id="addNote" name="note" rows="2" placeholder="Nhiệm vụ đặc biệt, đổi ca, hỗ trợ..."></textarea>
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

    <!-- Modal 2: Check-in xác nhận đầu ca -->
    <div class="modal fade modal-aura" id="checkInModal" tabindex="-1" aria-labelledby="checkInModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" style="max-width: 420px;">
            <div class="modal-content">
                <form method="post" action="${ctx}/admin/schedules">
                    <input type="hidden" name="action" value="check_in">
                    <input type="hidden" name="id" id="checkInId" value="">
                    <input type="hidden" name="returnDate" value="${selectedDate}">
                    <input type="hidden" name="returnView" value="${view}">

                    <div class="modal-header">
                        <h5 class="modal-title fw-bold" id="checkInModalLabel">Check-in nhận ca</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>

                    <div class="modal-body">
                        <p class="text-muted small mb-3">Xác nhận nhân viên <b id="checkInStaffName" class="text-dark"></b> đã có mặt tại quán để bắt đầu ca trực.</p>

                        <div class="mb-3">
                            <label for="checkInStartingCash" class="form-label small fw-semibold">Tiền bàn giao két đầu ca (đ)</label>
                            <input type="number" class="form-control aura-input" id="checkInStartingCash" name="startingCash" min="0" step="1000">
                            <div class="form-text">Tiền thối bàn giao cho nhân viên đầu ca.</div>
                        </div>
                    </div>

                    <div class="modal-footer">
                        <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-success">Xác nhận Check-in</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal 3: Chốt ca & Bàn giao két tiền mặt cuối ca -->
    <div class="modal fade modal-aura" id="closeShiftModal" tabindex="-1" aria-labelledby="closeShiftModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" style="max-width: 460px;">
            <div class="modal-content">
                <form method="post" action="${ctx}/admin/schedules">
                    <input type="hidden" name="action" value="close_shift">
                    <input type="hidden" name="id" id="closeShiftId" value="">
                    <input type="hidden" name="returnDate" value="${selectedDate}">
                    <input type="hidden" name="returnView" value="${view}">

                    <div class="modal-header">
                        <h5 class="modal-title fw-bold" id="closeShiftModalLabel">Chốt ca &amp; Bàn giao két tiền</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>

                    <div class="modal-body">
                        <div class="p-3 bg-light rounded-3 mb-3">
                            <div class="d-flex justify-content-between small mb-1">
                                <span class="text-muted">Nhân viên:</span>
                                <b id="closeShiftStaffName" class="text-dark"></b>
                            </div>
                            <div class="d-flex justify-content-between small">
                                <span class="text-muted">Tiền két đầu ca:</span>
                                <b id="closeShiftStartCashDisplay" class="text-dark">--</b>
                            </div>
                        </div>

                        <!-- Tiền hệ thống tính toán -->
                        <div class="mb-3">
                            <label for="closeShiftSystemCash" class="form-label small fw-semibold">Tiền hệ thống ghi nhận (đ)</label>
                            <input type="number" class="form-control aura-input" id="closeShiftSystemCash" name="endingCashSystem" placeholder="VD: 2500000" min="0" step="1000">
                            <div class="form-text">Tổng tiền thối đầu ca + doanh thu tiền mặt bán được trong ca.</div>
                        </div>

                        <!-- Tiền kiểm đếm thực tế -->
                        <div class="mb-3">
                            <label for="closeShiftActualCash" class="form-label small fw-semibold">Tiền thực tế kiểm đếm được <span class="text-danger">*</span></label>
                            <input type="number" class="form-control aura-input" id="closeShiftActualCash" name="endingCashActual" required placeholder="VD: 2500000" min="0" step="1000">
                        </div>

                        <!-- Live Difference Preview -->
                        <div class="p-2 px-3 rounded text-center small mb-3" id="diffPreviewBox" style="background: #f8fafc; border: 1px solid #e2e8f0;">
                            Chênh lệch két tiền: <b id="diffPreviewText">0đ (Khớp két)</b>
                        </div>

                        <!-- Ghi chú giải trình -->
                        <div class="mb-2">
                            <label for="closeShiftNote" class="form-label small fw-semibold">Ghi chú bàn giao / Giải trình nếu lệch</label>
                            <textarea class="form-control aura-input" id="closeShiftNote" name="note" rows="2" placeholder="Giải trình lý do nếu tiền thực tế bị lệch với hệ thống..."></textarea>
                        </div>
                    </div>

                    <div class="modal-footer">
                        <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-aura-primary">Hoàn tất chốt ca</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal 4: Chỉnh sửa ca làm việc -->
    <div class="modal fade modal-aura" id="editScheduleModal" tabindex="-1" aria-labelledby="editScheduleModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" style="max-width: 480px;">
            <div class="modal-content">
                <form method="post" action="${ctx}/admin/schedules">
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="id" id="editId" value="">
                    <input type="hidden" name="returnDate" value="${selectedDate}">
                    <input type="hidden" name="returnView" value="${view}">

                    <div class="modal-header">
                        <h5 class="modal-title fw-bold" id="editScheduleModalLabel">Chỉnh sửa ca làm việc</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>

                    <div class="modal-body">
                        <!-- Nhân viên -->
                        <div class="mb-3">
                            <label for="editUserId" class="form-label small fw-semibold">Nhân viên trực ca <span class="text-danger">*</span></label>
                            <select class="form-select aura-input" id="editUserId" name="userId" required>
                                <c:forEach items="${staffList}" var="staff">
                                    <option value="${staff.id}">${staff.fullName} (${staff.position})</option>
                                </c:forEach>
                            </select>
                        </div>

                        <!-- Ngày làm việc -->
                        <div class="row g-2 mb-3">
                            <div class="col-6">
                                <label for="editWorkDate" class="form-label small fw-semibold">Ngày làm <span class="text-danger">*</span></label>
                                <input type="date" class="form-control aura-input" id="editWorkDate" name="workDate" required>
                            </div>
                            <div class="col-6">
                                <label for="editShiftName" class="form-label small fw-semibold">Ca trực <span class="text-danger">*</span></label>
                                <select class="form-select aura-input" id="editShiftName" name="shiftName" required>
                                    <option value="MORNING">Ca sáng (${settings.shiftMorning})</option>
                                    <option value="AFTERNOON">Ca chiều (${settings.shiftAfternoon})</option>
                                    <option value="EVENING">Ca tối (${settings.shiftEvening})</option>
                                </select>
                            </div>
                        </div>

                        <!-- Trạng thái ca -->
                        <div class="mb-3">
                            <label for="editStatus" class="form-label small fw-semibold">Trạng thái điểm danh <span class="text-danger">*</span></label>
                            <select class="form-select aura-input" id="editStatus" name="status" required>
                                <option value="SCHEDULED">Đã xếp lịch (SCHEDULED)</option>
                                <option value="PRESENT">Có mặt (PRESENT)</option>
                                <option value="COMPLETED">Hoàn thành ca (COMPLETED)</option>
                                <option value="ABSENT">Vắng mặt (ABSENT)</option>
                            </select>
                        </div>

                        <!-- Bàn giao tiền két -->
                        <div class="row g-2 mb-3">
                            <div class="col-6">
                                <label for="editStartingCash" class="form-label small fw-semibold">Tiền đầu ca (đ)</label>
                                <input type="number" class="form-control aura-input" id="editStartingCash" name="startingCash" min="0" step="1000">
                            </div>
                            <div class="col-6">
                                <label for="editEndingCashActual" class="form-label small fw-semibold">Tiền cuối ca (đ)</label>
                                <input type="number" class="form-control aura-input" id="editEndingCashActual" name="endingCashActual" min="0" step="1000">
                            </div>
                        </div>

                        <!-- Ghi chú -->
                        <div class="mb-2">
                            <label for="editNote" class="form-label small fw-semibold">Ghi chú</label>
                            <textarea class="form-control aura-input" id="editNote" name="note" rows="2"></textarea>
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
        const addScheduleModal = new bootstrap.Modal(document.getElementById('addScheduleModal'));
        const checkInModal = new bootstrap.Modal(document.getElementById('checkInModal'));
        const closeShiftModal = new bootstrap.Modal(document.getElementById('closeShiftModal'));
        const editScheduleModal = new bootstrap.Modal(document.getElementById('editScheduleModal'));

        function openAddScheduleModal(dateStr, shiftName) {
            if (dateStr) {
                document.getElementById('addWorkDate').value = dateStr;
            }
            if (shiftName) {
                if (shiftName === 'MORNING') document.getElementById('shiftMorningRadio').checked = true;
                if (shiftName === 'AFTERNOON') document.getElementById('shiftAfternoonRadio').checked = true;
                if (shiftName === 'EVENING') document.getElementById('shiftEveningRadio').checked = true;
            }
            addScheduleModal.show();
        }

        function openCheckInModal(id, staffName, startingCash) {
            document.getElementById('checkInId').value = id;
            document.getElementById('checkInStaffName').textContent = staffName;
            document.getElementById('checkInStartingCash').value = startingCash && startingCash !== 'null' ? startingCash : '';
            checkInModal.show();
        }

        function openCloseShiftModal(id, staffName, startingCash) {
            document.getElementById('closeShiftId').value = id;
            document.getElementById('closeShiftStaffName').textContent = staffName;
            
            const startVal = startingCash && startingCash !== 'null' ? Number(startingCash) : 0;
            document.getElementById('closeShiftStartCashDisplay').textContent = startVal > 0 ? startVal.toLocaleString('vi-VN') + 'đ' : '--';
            document.getElementById('closeShiftSystemCash').value = startVal > 0 ? startVal : '';
            document.getElementById('closeShiftActualCash').value = '';
            document.getElementById('closeShiftNote').value = '';

            calculateDiff();
            closeShiftModal.show();
        }

        function calculateDiff() {
            const sys = Number(document.getElementById('closeShiftSystemCash').value || 0);
            const act = Number(document.getElementById('closeShiftActualCash').value || 0);
            const box = document.getElementById('diffPreviewBox');
            const txt = document.getElementById('diffPreviewText');

            if (!document.getElementById('closeShiftActualCash').value) {
                txt.textContent = 'Chưa nhập tiền thực tế';
                box.className = 'p-2 px-3 rounded text-center small mb-3 bg-light text-muted border';
                return;
            }

            const diff = act - sys;
            if (Math.abs(diff) <= 1) {
                txt.textContent = '0đ (Khớp két 100%)';
                box.className = 'p-2 px-3 rounded text-center small mb-3 bg-success-subtle text-success border border-success-subtle';
            } else if (diff > 0) {
                txt.textContent = 'Thừa +' + diff.toLocaleString('vi-VN') + 'đ (Thừa tiền)';
                box.className = 'p-2 px-3 rounded text-center small mb-3 bg-warning-subtle text-warning border border-warning-subtle';
            } else {
                txt.textContent = 'Thiếu ' + diff.toLocaleString('vi-VN') + 'đ (Thụt két tiền!)';
                box.className = 'p-2 px-3 rounded text-center small mb-3 bg-danger-subtle text-danger border border-danger-subtle';
            }
        }

        document.getElementById('closeShiftSystemCash').addEventListener('input', calculateDiff);
        document.getElementById('closeShiftActualCash').addEventListener('input', calculateDiff);

        function openEditScheduleModal(id, userId, workDate, shiftName, status, note, startingCash, endingCashActual) {
            document.getElementById('editId').value = id;
            document.getElementById('editUserId').value = userId;
            document.getElementById('editWorkDate').value = workDate;
            document.getElementById('editShiftName').value = shiftName;
            document.getElementById('editStatus').value = status;
            document.getElementById('editNote').value = note && note !== 'null' ? note : '';
            document.getElementById('editStartingCash').value = startingCash && startingCash !== 'null' ? startingCash : '';
            document.getElementById('editEndingCashActual').value = endingCashActual && endingCashActual !== 'null' ? endingCashActual : '';

            editScheduleModal.show();
        }
    </script>
</body>
</html>
