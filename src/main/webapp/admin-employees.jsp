<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty sessionScope.user or sessionScope.user.role ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<fmt:setLocale value="vi_VN" />
<c:set var="csrf" value="${fn:escapeXml(sessionScope.employeeCsrfToken)}" />
<%-- Dữ liệu form khi server báo lỗi (không bao giờ chứa mật khẩu); mặc định khi thêm mới --%>
<c:set var="form" value="${employeeForm}" />
<c:set var="fRole" value="${empty form ? 'STAFF' : form.role}" />
<c:set var="fPosition" value="${empty form ? 'CASHIER' : form.position}" />
<c:set var="fStatus" value="${empty form ? 'ACTIVE' : form.status}" />
<c:set var="fHireDate" value="${empty form ? today : form.hireDate}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Aura Café | Quản lý nhân viên</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,400,0,0" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-employees.css" rel="stylesheet">
</head>
<body class="admin-body">
<div class="mobile-admin-bar">
    <a href="${ctx}/admin-dashboard.jsp" class="text-decoration-none fw-bold text-danger"><span class="material-symbols-outlined align-middle">local_cafe</span> AURA CAFÉ · ADMIN</a>
    <a href="${ctx}/login?action=logout" class="btn btn-sm btn-light">Đăng xuất</a>
</div>
<jsp:include page="/components/admin-sidebar.jsp"><jsp:param name="activeMenu" value="employees" /></jsp:include>

<main class="admin-main-wrapper employees-page">
    <header class="employee-overview">
        <div class="employee-heading">
            <div>
                <span class="table-ref">Bảng: users (role ADMIN, STAFF)</span>
                <h1 class="page-title">Quản lý nhân viên</h1>
                <p class="page-subtitle">Tài khoản nội bộ: vai trò, chức vụ, lương cơ bản và ngày vào làm.</p>
            </div>
            <button type="button" id="employeeAddButton" class="btn btn-aura btn-sm fw-bold text-nowrap"
                    data-bs-toggle="modal" data-bs-target="#employeeModal">
                <span class="material-symbols-outlined fs-6 align-middle">person_add</span> Thêm nhân viên
            </button>
        </div>
        <div class="employee-stats" aria-label="Thống kê nhân viên">
            <div class="employee-stat"><span>Tổng nhân sự</span><strong><c:out value="${employeeStats.total}" /></strong></div>
            <div class="employee-stat active"><span>Đang làm việc</span><strong><c:out value="${employeeStats.active}" /></strong></div>
            <div class="employee-stat locked"><span>Bị khóa / nghỉ</span><strong><c:out value="${employeeStats.notActive}" /></strong></div>
            <div class="employee-stat"><span>Quỹ lương cơ bản</span><strong><fmt:formatNumber value="${employeeStats.salaryFund}" pattern="#,##0" />đ</strong></div>
        </div>
    </header>

    <c:if test="${not empty employeeMessage}"><div class="alert alert-success py-2 mb-0" role="status"><c:out value="${employeeMessage}" /></div></c:if>
    <c:if test="${not empty employeeError}"><div class="alert alert-danger py-2 mb-0" role="alert"><c:out value="${employeeError}" /></div></c:if>

    <section class="employee-card" aria-labelledby="employeeListTitle">
        <div class="employee-card-head">
            <h2 id="employeeListTitle">Danh sách nhân viên</h2>
            <form method="get" action="${ctx}/admin/employees" class="employee-filter">
                <label for="positionFilter" class="visually-hidden">Lọc theo chức vụ</label>
                <select id="positionFilter" name="position" class="form-select form-select-sm" onchange="this.form.submit()">
                    <option value="ALL">Tất cả chức vụ</option>
                    <option value="MANAGER" ${positionFilter eq 'MANAGER' ? 'selected' : ''}>Quản lý</option>
                    <option value="BARISTA" ${positionFilter eq 'BARISTA' ? 'selected' : ''}>Pha chế</option>
                    <option value="CASHIER" ${positionFilter eq 'CASHIER' ? 'selected' : ''}>Thu ngân</option>
                    <option value="SHIPPER" ${positionFilter eq 'SHIPPER' ? 'selected' : ''}>Giao hàng</option>
                </select>
                <noscript><button type="submit" class="btn btn-sm btn-light">Lọc</button></noscript>
            </form>
        </div>
        <div class="table-responsive">
            <table class="table table-hover employee-table mb-0">
                <thead class="table-light text-uppercase">
                    <tr>
                        <th scope="col">Họ và tên</th>
                        <th scope="col">SĐT / Email</th>
                        <th scope="col">Vai trò</th>
                        <th scope="col">Chức vụ</th>
                        <th scope="col" class="text-end">Lương cơ bản</th>
                        <th scope="col">Ngày vào làm</th>
                        <th scope="col">Trạng thái</th>
                        <th scope="col" class="text-end">Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                <c:forEach var="emp" items="${employees}">
                    <c:set var="isMe" value="${emp.id == sessionScope.user.id}" />
                    <tr>
                        <td>
                            <span class="fw-bold text-dark"><c:out value="${emp.fullName}" /></span>
                            <c:if test="${isMe}"><span class="me-badge">Bạn</span></c:if>
                        </td>
                        <td>
                            <div class="font-monospace"><c:out value="${emp.phone}" /></div>
                            <div class="employee-muted"><c:out value="${empty emp.email ? '—' : emp.email}" /></div>
                        </td>
                        <td>${emp.roleDisplayName}</td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty emp.position}"><span class="position-badge position-${fn:toLowerCase(emp.position)}">${emp.positionDisplayName}</span></c:when>
                                <c:otherwise><span class="employee-muted">—</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="text-end font-monospace"><fmt:formatNumber value="${emp.baseSalary}" pattern="#,##0.##" />đ</td>
                        <td>${empty emp.hireDateDisplay ? '—' : emp.hireDateDisplay}</td>
                        <td><span class="status-badge status-${fn:toLowerCase(emp.status)}">${emp.statusDisplayName}</span></td>
                        <td class="text-end text-nowrap">
                            <button type="button" class="btn btn-sm btn-outline-primary employee-edit-button"
                                    data-bs-toggle="modal" data-bs-target="#employeeModal"
                                    data-id="${emp.id}"
                                    data-full-name="${fn:escapeXml(emp.fullName)}"
                                    data-phone="${fn:escapeXml(emp.phone)}"
                                    data-email="${fn:escapeXml(emp.email)}"
                                    data-role="${emp.role}"
                                    data-position="${emp.position}"
                                    data-base-salary="${empty emp.baseSalary ? '0' : emp.baseSalary.stripTrailingZeros().toPlainString()}"
                                    data-hire-date="${emp.hireDate}"
                                    data-birthday="${emp.birthday}"
                                    data-status="${emp.status}"
                                    data-address="${fn:escapeXml(emp.address)}">Sửa</button>
                            <form method="post" action="${ctx}/admin/employees" class="d-inline employee-lock-form"
                                  data-name="${fn:escapeXml(emp.fullName)}" data-lock="${emp.status ne 'LOCKED'}">
                                <input type="hidden" name="csrfToken" value="${csrf}">
                                <input type="hidden" name="id" value="${emp.id}">
                                <input type="hidden" name="returnPosition" value="${positionFilter}">
                                <c:choose>
                                    <c:when test="${emp.status eq 'LOCKED'}">
                                        <input type="hidden" name="action" value="unlock">
                                        <button type="submit" class="btn btn-sm btn-outline-success">Mở khóa</button>
                                    </c:when>
                                    <c:otherwise>
                                        <input type="hidden" name="action" value="lock">
                                        <button type="submit" class="btn btn-sm btn-outline-danger" ${isMe ? 'disabled' : ''}
                                                title="${isMe ? 'Không thể tự khóa tài khoản đang đăng nhập' : 'Khóa tài khoản'}">Khóa</button>
                                    </c:otherwise>
                                </c:choose>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty employees}">
                    <tr><td colspan="8" class="text-center text-muted py-5">Không có nhân viên nào phù hợp bộ lọc.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </section>
</main>

<div class="modal fade" id="employeeModal" tabindex="-1" aria-labelledby="employeeModalTitle" aria-hidden="true"
     data-reopen="${not empty form}">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content employee-modal">
            <form id="employeeForm" method="post" action="${ctx}/admin/employees" novalidate data-today="${today}">
                <input type="hidden" name="csrfToken" value="${csrf}">
                <input type="hidden" name="action" value="save">
                <input type="hidden" name="returnPosition" value="${positionFilter}">
                <input type="hidden" name="id" id="employeeId" value="${empty form.id ? '0' : fn:escapeXml(form.id)}">

                <div class="modal-header">
                    <h2 class="modal-title" id="employeeModalTitle">Thêm nhân viên</h2>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>

                <div class="modal-body">
                    <c:if test="${not empty employeeFormError}">
                        <div class="alert alert-danger py-2" role="alert" id="employeeFormError"><c:out value="${employeeFormError}" /></div>
                    </c:if>
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label" for="employeeFullName">Họ và tên <span>*</span></label>
                            <input class="form-control" id="employeeFullName" name="fullName" required maxlength="120"
                                   value="${fn:escapeXml(form.fullName)}">
                            <div class="invalid-feedback">Vui lòng nhập họ và tên.</div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="employeePhone">Số điện thoại (đăng nhập) <span>*</span></label>
                            <input class="form-control font-monospace" id="employeePhone" name="phone" required
                                   inputmode="numeric" pattern="[0-9]{9,11}" maxlength="11" autocomplete="off"
                                   value="${fn:escapeXml(form.phone)}">
                            <div class="invalid-feedback">Số điện thoại gồm 9-11 chữ số.</div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="employeeEmail">Email</label>
                            <input class="form-control" type="email" id="employeeEmail" name="email" maxlength="150"
                                   autocomplete="off" value="${fn:escapeXml(form.email)}">
                            <div class="invalid-feedback">Email không đúng định dạng.</div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="employeePassword" id="employeePasswordLabel">Mật khẩu <span>*</span></label>
                            <input class="form-control" type="password" id="employeePassword" name="password"
                                   minlength="6" maxlength="255" autocomplete="new-password">
                            <div class="invalid-feedback">Mật khẩu phải từ 6 ký tự trở lên.</div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="employeeRole">Vai trò</label>
                            <select class="form-select" id="employeeRole" name="role">
                                <option value="STAFF" ${fRole eq 'STAFF' ? 'selected' : ''}>Nhân viên (STAFF)</option>
                                <option value="ADMIN" ${fRole eq 'ADMIN' ? 'selected' : ''}>Quản trị (ADMIN)</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="employeePosition">Chức vụ</label>
                            <select class="form-select" id="employeePosition" name="position">
                                <option value="MANAGER" ${fPosition eq 'MANAGER' ? 'selected' : ''}>Quản lý</option>
                                <option value="BARISTA" ${fPosition eq 'BARISTA' ? 'selected' : ''}>Pha chế</option>
                                <option value="CASHIER" ${fPosition eq 'CASHIER' ? 'selected' : ''}>Thu ngân</option>
                                <option value="SHIPPER" ${fPosition eq 'SHIPPER' ? 'selected' : ''}>Giao hàng</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="employeeBaseSalary">Lương cơ bản (VNĐ)</label>
                            <input class="form-control font-monospace" type="number" id="employeeBaseSalary" name="baseSalary"
                                   min="0" max="9999999999.99" step="any" value="${fn:escapeXml(form.baseSalary)}">
                            <div class="invalid-feedback">Lương cơ bản không được âm.</div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="employeeHireDate">Ngày vào làm</label>
                            <input class="form-control" type="date" id="employeeHireDate" name="hireDate"
                                   value="${fn:escapeXml(fHireDate)}">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="employeeBirthday">Ngày sinh</label>
                            <input class="form-control" type="date" id="employeeBirthday" name="birthday" max="${today}"
                                   value="${fn:escapeXml(form.birthday)}">
                            <div class="invalid-feedback">Ngày sinh không được ở tương lai.</div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="employeeStatus">Trạng thái</label>
                            <select class="form-select" id="employeeStatus" name="status">
                                <option value="ACTIVE" ${fStatus eq 'ACTIVE' ? 'selected' : ''}>Hoạt động</option>
                                <option value="INACTIVE" ${fStatus eq 'INACTIVE' ? 'selected' : ''}>Ngừng hoạt động</option>
                                <option value="LOCKED" ${fStatus eq 'LOCKED' ? 'selected' : ''}>Đã khóa</option>
                            </select>
                        </div>
                        <div class="col-12">
                            <label class="form-label" for="employeeAddress">Địa chỉ</label>
                            <textarea class="form-control" id="employeeAddress" name="address" rows="2"
                                      maxlength="500"><c:out value="${form.address}" /></textarea>
                        </div>
                    </div>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-aura fw-bold">
                        <span class="material-symbols-outlined fs-6 align-middle">check</span>
                        <span id="employeeSubmitLabel">Tạo tài khoản</span>
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
(() => {
    const modalEl = document.getElementById('employeeModal');
    const form = document.getElementById('employeeForm');
    const byId = (id) => document.getElementById(id);
    const fields = {
        id: byId('employeeId'), fullName: byId('employeeFullName'), phone: byId('employeePhone'),
        email: byId('employeeEmail'), password: byId('employeePassword'), role: byId('employeeRole'),
        position: byId('employeePosition'), baseSalary: byId('employeeBaseSalary'), hireDate: byId('employeeHireDate'),
        birthday: byId('employeeBirthday'), status: byId('employeeStatus'), address: byId('employeeAddress')
    };
    const title = byId('employeeModalTitle');
    const passwordLabel = byId('employeePasswordLabel');
    const submitLabel = byId('employeeSubmitLabel');

    // Thêm mới: mật khẩu bắt buộc. Sửa: bỏ trống mật khẩu = giữ nguyên mật khẩu cũ.
    const setMode = (isEdit) => {
        title.textContent = isEdit ? 'Sửa nhân viên' : 'Thêm nhân viên';
        passwordLabel.innerHTML = isEdit ? 'Mật khẩu mới (bỏ trống nếu không đổi)' : 'Mật khẩu <span>*</span>';
        fields.password.required = !isEdit;
        submitLabel.textContent = isEdit ? 'Lưu thay đổi' : 'Tạo tài khoản';
    };

    const resetState = () => {
        form.classList.remove('was-validated');
        const serverError = byId('employeeFormError');
        if (serverError) serverError.remove();
        fields.password.value = '';
    };

    const fill = (values) => {
        Object.keys(values).forEach((key) => { fields[key].value = values[key] || ''; });
    };

    byId('employeeAddButton').addEventListener('click', () => {
        resetState();
        fill({ id: '0', fullName: '', phone: '', email: '', role: 'STAFF', position: 'CASHIER', baseSalary: '',
               hireDate: form.dataset.today, birthday: '', status: 'ACTIVE', address: '' });
        setMode(false);
    });

    document.querySelectorAll('.employee-edit-button').forEach((button) => {
        button.addEventListener('click', () => {
            const d = button.dataset;
            resetState();
            fill({ id: d.id, fullName: d.fullName, phone: d.phone, email: d.email, role: d.role,
                   position: d.position, baseSalary: d.baseSalary, hireDate: d.hireDate, birthday: d.birthday,
                   status: d.status, address: d.address });
            setMode(true);
        });
    });

    document.querySelectorAll('.employee-lock-form').forEach((lockForm) => {
        lockForm.addEventListener('submit', (event) => {
            if (lockForm.dataset.lock === 'true' && !confirm('Khóa tài khoản ' + lockForm.dataset.name + '? Nhân viên sẽ không đăng nhập được.')) {
                event.preventDefault();
            }
        });
    });

    form.addEventListener('submit', (event) => {
        if (!form.checkValidity()) {
            event.preventDefault();
            event.stopPropagation();
            const firstInvalid = form.querySelector(':invalid');
            if (firstInvalid) firstInvalid.focus();
        }
        form.classList.add('was-validated');
    });

    // Server báo lỗi: mở lại modal với dữ liệu vừa nhập
    setMode(fields.id.value !== '' && fields.id.value !== '0');
    if (modalEl.dataset.reopen === 'true') {
        bootstrap.Modal.getOrCreateInstance(modalEl).show();
    }
})();
</script>
</body>
</html>
