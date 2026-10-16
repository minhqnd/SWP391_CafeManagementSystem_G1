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
    <title>Lịch phân ca nhân viên | Aura Café Admin</title>

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
                        <li class="breadcrumb-item active fw-semibold text-dark" aria-current="page">Lịch phân ca</li>
                    </ol>
                </nav>
                <h1 class="schedule-title">Lịch phân ca nhân viên</h1>
                <p class="schedule-subtitle">Xếp ca làm việc, điểm danh và quản lý lịch trực cho nhân sự toàn hệ thống</p>
            </div>

            <div class="d-flex align-items-center gap-3">
                <!-- View Mode Switch -->
                <div class="btn-group p-1 bg-white rounded-3 border" role="group">
                    <a href="${ctx}/admin/schedules?view=calendar&date=${selectedDate}&shift=${shiftFilter}&position=${positionFilter}&status=${statusFilter}&staffId=${staffIdFilter}"
                       class="btn btn-sm ${view == 'calendar' ? 'btn-dark fw-bold' : 'btn-light border-0 text-muted'}">
                        <span class="material-symbols-outlined fs-6 align-middle me-1">calendar_view_week</span> Lịch tuần
                    </a>
                    <a href="${ctx}/admin/schedules?view=table&date=${selectedDate}&shift=${shiftFilter}&position=${positionFilter}&status=${statusFilter}&staffId=${staffIdFilter}"
                       class="btn btn-sm ${view == 'table' ? 'btn-dark fw-bold' : 'btn-light border-0 text-muted'}">
                        <span class="material-symbols-outlined fs-6 align-middle me-1">table_rows</span> Danh sách
                    </a>
                </div>

                <!-- Button Phân ca mới -->
                <button type="button" class="btn-aura-primary" onclick="openAddScheduleModal()">
                    <span class="material-symbols-outlined fs-5">add</span> Phân ca mới
                </button>
            </div>
        </div>

        <!-- Filter & Week Navigation Toolbar -->
        <div class="filter-toolbar-card">
            <!-- Week Navigator -->
            <div class="week-navigation-group">
                <a href="${ctx}/admin/schedules?view=${view}&date=${prevWeekDate}&shift=${shiftFilter}&position=${positionFilter}&status=${statusFilter}&staffId=${staffIdFilter}"
                   class="btn-week-nav" title="Tuần trước">
                    <span class="material-symbols-outlined fs-6">chevron_left</span> Tuần trước
                </a>
                <a href="${ctx}/admin/schedules?view=${view}&date=&shift=${shiftFilter}&position=${positionFilter}&status=${statusFilter}&staffId=${staffIdFilter}"
                   class="btn-week-nav" title="Trở về tuần hiện tại">
                    Tuần này
                </a>
                <a href="${ctx}/admin/schedules?view=${view}&date=${nextWeekDate}&shift=${shiftFilter}&position=${positionFilter}&status=${statusFilter}&staffId=${staffIdFilter}"
                   class="btn-week-nav" title="Tuần sau">
                    Tuần sau <span class="material-symbols-outlined fs-6">chevron_right</span>
                </a>
                <span class="week-range-text">
                    <span class="text-muted fw-normal">Tuần:</span> ${monday} <span class="text-muted">➔</span> ${sunday}
                </span>
            </div>

            <!-- Filter Controls Form -->
            <form method="get" action="${ctx}/admin/schedules" class="d-flex align-items-center gap-2 flex-wrap m-0">
                <input type="hidden" name="view" value="${view}">
                
                <!-- Quick Date Picker -->
                <input type="date" name="date" class="form-control form-control-sm" style="width: 140px;"
                       value="${selectedDate}" onchange="this.form.submit()" title="Chọn ngày để nhảy đến tuần đó">

                <!-- Filter Shift -->
                <select name="shift" class="form-select form-select-sm" style="width: 130px;" onchange="this.form.submit()">
                    <option value="ALL" ${shiftFilter == 'ALL' ? 'selected' : ''}>Tất cả ca</option>
                    <option value="MORNING" ${shiftFilter == 'MORNING' ? 'selected' : ''}>Ca sáng</option>
                    <option value="AFTERNOON" ${shiftFilter == 'AFTERNOON' ? 'selected' : ''}>Ca chiều</option>
                    <option value="EVENING" ${shiftFilter == 'EVENING' ? 'selected' : ''}>Ca tối</option>
                </select>

                <!-- Filter Position -->
                <select name="position" class="form-select form-select-sm" style="width: 135px;" onchange="this.form.submit()">
                    <option value="ALL" ${positionFilter == 'ALL' ? 'selected' : ''}>Tất cả vị trí</option>
                    <option value="BARISTA" ${positionFilter == 'BARISTA' ? 'selected' : ''}>Pha chế</option>
                    <option value="CASHIER" ${positionFilter == 'CASHIER' ? 'selected' : ''}>Thu ngân</option>
                    <option value="SHIPPER" ${positionFilter == 'SHIPPER' ? 'selected' : ''}>Giao hàng</option>
                    <option value="MANAGER" ${positionFilter == 'MANAGER' ? 'selected' : ''}>Quản lý</option>
                </select>

                <!-- Filter Status -->
                <select name="status" class="form-select form-select-sm" style="width: 140px;" onchange="this.form.submit()">
                    <option value="ALL" ${statusFilter == 'ALL' ? 'selected' : ''}>Tất cả trạng thái</option>
                    <option value="SCHEDULED" ${statusFilter == 'SCHEDULED' ? 'selected' : ''}>Đã xếp lịch</option>
                    <option value="PRESENT" ${statusFilter == 'PRESENT' ? 'selected' : ''}>Có mặt</option>
                    <option value="COMPLETED" ${statusFilter == 'COMPLETED' ? 'selected' : ''}>Hoàn thành</option>
                    <option value="ABSENT" ${statusFilter == 'ABSENT' ? 'selected' : ''}>Vắng mặt</option>
                </select>

                <!-- Reset Filters -->
                <c:if test="${shiftFilter != 'ALL' || positionFilter != 'ALL' || statusFilter != 'ALL' || not empty staffIdFilter}">
                    <a href="${ctx}/admin/schedules?view=${view}&date=${selectedDate}" class="btn btn-outline-secondary btn-sm" title="Xóa bộ lọc">
                        <span class="material-symbols-outlined fs-6 align-middle">close</span>
                    </a>
                </c:if>
            </form>
        </div>

        <!-- ========================================== -->
        <!-- VIEW 1: WEEKLY CALENDAR GRID               -->
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
                        <!-- ROW 1: CA SÁNG -->
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
                                                    <!-- Actions Dropdown -->
                                                    <div class="dropdown d-inline-block">
                                                        <button class="slot-action-dots" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                                                            <span class="material-symbols-outlined fs-6">more_vert</span>
                                                        </button>
                                                        <ul class="dropdown-menu dropdown-menu-end shadow-sm small">
                                                            <li><h6 class="dropdown-header">Điểm danh</h6></li>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0">
                                                                    <input type="hidden" name="action" value="quick_status">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="status" value="PRESENT">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="calendar">
                                                                    <button type="submit" class="dropdown-item text-success"><span class="material-symbols-outlined fs-6 align-middle">check</span> Có mặt</button>
                                                                </form>
                                                            </li>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0">
                                                                    <input type="hidden" name="action" value="quick_status">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="status" value="COMPLETED">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="calendar">
                                                                    <button type="submit" class="dropdown-item text-primary"><span class="material-symbols-outlined fs-6 align-middle">task_alt</span> Hoàn thành ca</button>
                                                                </form>
                                                            </li>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0">
                                                                    <input type="hidden" name="action" value="quick_status">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="status" value="ABSENT">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="calendar">
                                                                    <button type="submit" class="dropdown-item text-danger"><span class="material-symbols-outlined fs-6 align-middle">close</span> Vắng mặt</button>
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
                                                                    <input type="hidden" name="returnView" value="calendar">
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
                                                <c:if test="${not empty item.note}">
                                                    <div class="text-muted text-truncate" style="font-size: 0.72rem;" title="${item.note}">
                                                        📝 ${item.note}
                                                    </div>
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

                        <!-- ROW 2: CA CHIỀU -->
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
                                                            <li><h6 class="dropdown-header">Điểm danh</h6></li>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0">
                                                                    <input type="hidden" name="action" value="quick_status">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="status" value="PRESENT">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="calendar">
                                                                    <button type="submit" class="dropdown-item text-success"><span class="material-symbols-outlined fs-6 align-middle">check</span> Có mặt</button>
                                                                </form>
                                                            </li>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0">
                                                                    <input type="hidden" name="action" value="quick_status">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="status" value="COMPLETED">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="calendar">
                                                                    <button type="submit" class="dropdown-item text-primary"><span class="material-symbols-outlined fs-6 align-middle">task_alt</span> Hoàn thành ca</button>
                                                                </form>
                                                            </li>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0">
                                                                    <input type="hidden" name="action" value="quick_status">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="status" value="ABSENT">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="calendar">
                                                                    <button type="submit" class="dropdown-item text-danger"><span class="material-symbols-outlined fs-6 align-middle">close</span> Vắng mặt</button>
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
                                                                    <input type="hidden" name="returnView" value="calendar">
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
                                                <c:if test="${not empty item.note}">
                                                    <div class="text-muted text-truncate" style="font-size: 0.72rem;" title="${item.note}">
                                                        📝 ${item.note}
                                                    </div>
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

                        <!-- ROW 3: CA TỐI -->
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
                                                            <li><h6 class="dropdown-header">Điểm danh</h6></li>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0">
                                                                    <input type="hidden" name="action" value="quick_status">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="status" value="PRESENT">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="calendar">
                                                                    <button type="submit" class="dropdown-item text-success"><span class="material-symbols-outlined fs-6 align-middle">check</span> Có mặt</button>
                                                                </form>
                                                            </li>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0">
                                                                    <input type="hidden" name="action" value="quick_status">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="status" value="COMPLETED">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="calendar">
                                                                    <button type="submit" class="dropdown-item text-primary"><span class="material-symbols-outlined fs-6 align-middle">task_alt</span> Hoàn thành ca</button>
                                                                </form>
                                                            </li>
                                                            <li>
                                                                <form method="post" action="${ctx}/admin/schedules" class="m-0">
                                                                    <input type="hidden" name="action" value="quick_status">
                                                                    <input type="hidden" name="id" value="${item.id}">
                                                                    <input type="hidden" name="status" value="ABSENT">
                                                                    <input type="hidden" name="returnDate" value="${selectedDate}">
                                                                    <input type="hidden" name="returnView" value="calendar">
                                                                    <button type="submit" class="dropdown-item text-danger"><span class="material-symbols-outlined fs-6 align-middle">close</span> Vắng mặt</button>
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
                                                                    <input type="hidden" name="returnView" value="calendar">
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
                                                <c:if test="${not empty item.note}">
                                                    <div class="text-muted text-truncate" style="font-size: 0.72rem;" title="${item.note}">
                                                        📝 ${item.note}
                                                    </div>
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
        <!-- VIEW 2: TABLE LIST VIEW                    -->
        <!-- ========================================== -->
        <c:if test="${view == 'table'}">
            <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-4">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light text-muted small text-uppercase">
                            <tr>
                                <th class="ps-3 py-3">Ngày làm</th>
                                <th>Ca trực</th>
                                <th>Nhân viên</th>
                                <th>Vị trí</th>
                                <th>Điểm danh</th>
                                <th>Trạng thái</th>
                                <th>Bàn giao tiền</th>
                                <th>Ghi chú</th>
                                <th class="text-end pe-3">Thao tác</th>
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
                                        <c:if test="${not empty item.startingCash}">
                                            <div>Đầu ca: <fmt:formatNumber value="${item.startingCash}" pattern="#,###" />đ</div>
                                        </c:if>
                                        <c:if test="${not empty item.endingCashActual}">
                                            <div>Thực tế: <fmt:formatNumber value="${item.endingCashActual}" pattern="#,###" />đ</div>
                                        </c:if>
                                        <c:if test="${empty item.startingCash && empty item.endingCashActual}">
                                            <span class="text-muted">--</span>
                                        </c:if>
                                    </td>
                                    <td class="text-muted">${empty item.note ? '--' : item.note}</td>
                                    <td class="text-end pe-3">
                                        <div class="btn-group btn-group-sm">
                                            <button type="button" class="btn btn-outline-secondary" title="Sửa ca"
                                                    onclick="openEditScheduleModal('${item.id}', '${item.userId}', '${item.workDateFormatted}', '${item.shiftName}', '${item.status}', '${item.note}', '${item.startingCash}', '${item.endingCashActual}')">
                                                <span class="material-symbols-outlined fs-6 align-middle">edit</span>
                                            </button>
                                            <form method="post" action="${ctx}/admin/schedules" class="d-inline m-0" onsubmit="return confirm('Bạn có chắc muốn hủy ca làm này?');">
                                                <input type="hidden" name="action" value="delete">
                                                <input type="hidden" name="id" value="${item.id}">
                                                <input type="hidden" name="returnDate" value="${selectedDate}">
                                                <input type="hidden" name="returnView" value="table">
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
                                    <td colspan="9" class="text-center py-5 text-muted">
                                        <span class="material-symbols-outlined fs-1 text-secondary mb-2">event_busy</span>
                                        <div>Không tìm thấy ca làm việc nào phù hợp với bộ lọc trong tuần này.</div>
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
                            <label for="addStartingCash" class="form-label small fw-semibold">Tiền bàn giao đầu ca (VNĐ)</label>
                            <input type="number" class="form-control aura-input" id="addStartingCash" name="startingCash" placeholder="VD: 500000" min="0" step="1000">
                            <div class="form-text">Dành cho thu ngân nhận ca quản lý két tiền mặt.</div>
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

    <!-- Modal 2: Chỉnh sửa ca làm việc -->
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
