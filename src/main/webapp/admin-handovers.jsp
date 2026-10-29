<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty sessionScope.user or sessionScope.user.role ne 'ADMIN'}"><c:redirect url="/?tab=login" /></c:if>
<c:set var="csrf" value="${fn:escapeXml(sessionScope.handoverCsrfToken)}" />

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bàn giao ca | Aura Café</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-shifts.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-handovers.css" rel="stylesheet">
</head>
<body class="admin-body">
    <jsp:include page="/components/admin-sidebar.jsp">
        <jsp:param name="activeMenu" value="handovers" />
    </jsp:include>

    <main class="admin-main-wrapper shift-page handover-page">
        <c:if test="${not empty handoverSuccessMsg}">
            <div class="alert alert-success alert-dismissible fade show shift-alert" role="alert">
                <span class="material-symbols-outlined">check_circle</span>
                <span><c:out value="${handoverSuccessMsg}" /></span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
            </div>
        </c:if>
        <c:if test="${not empty handoverErrorMsg}">
            <div class="alert alert-danger alert-dismissible fade show shift-alert" role="alert">
                <span class="material-symbols-outlined">error</span>
                <span><c:out value="${handoverErrorMsg}" /></span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
            </div>
        </c:if>

        <header class="shift-page-header">
            <div class="shift-heading-row">
                <div>
                    <span class="shift-table-ref">Bảng: <strong>shift_handovers</strong></span>
                    <h1>Bàn giao ca</h1>
                    <p>Thu ngân chốt ca sẽ gửi biên bản tiền két. Admin xác nhận, hoặc từ chối kèm lý do để Thu ngân giải trình và gửi lại.</p>
                </div>
            </div>

            <nav class="handover-filter" aria-label="Lọc theo trạng thái">
                <a class="handover-filter-chip ${statusFilter eq 'SUBMITTED' ? 'is-active' : ''}" href="${ctx}/admin/handovers?status=SUBMITTED">
                    Chờ duyệt <span>${statusCounts['SUBMITTED']}</span>
                </a>
                <a class="handover-filter-chip ${statusFilter eq 'CONFIRMED' ? 'is-active' : ''}" href="${ctx}/admin/handovers?status=CONFIRMED">
                    Đã xác nhận <span>${statusCounts['CONFIRMED']}</span>
                </a>
                <a class="handover-filter-chip ${statusFilter eq 'REJECTED' ? 'is-active' : ''}" href="${ctx}/admin/handovers?status=REJECTED">
                    Bị từ chối <span>${statusCounts['REJECTED']}</span>
                </a>
                <a class="handover-filter-chip ${statusFilter eq 'ALL' ? 'is-active' : ''}" href="${ctx}/admin/handovers?status=ALL">
                    Tất cả
                </a>
            </nav>
        </header>

        <section class="shift-list-card" aria-label="Danh sách biên bản bàn giao">
            <c:choose>
                <c:when test="${not empty handovers}">
                    <div class="table-responsive">
                        <table class="shift-table handover-table">
                            <thead>
                                <tr>
                                    <th scope="col">Ca làm</th>
                                    <th scope="col">Thu ngân</th>
                                    <th scope="col" class="text-end">Đầu ca</th>
                                    <th scope="col" class="text-end">Thu tiền mặt</th>
                                    <th scope="col" class="text-end">Hệ thống</th>
                                    <th scope="col" class="text-end">Thực tế</th>
                                    <th scope="col" class="text-end">Chênh lệch</th>
                                    <th scope="col">Ghi chú</th>
                                    <th scope="col">Trạng thái</th>
                                    <th scope="col" class="text-end">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach items="${handovers}" var="h">
                                    <tr>
                                        <td>
                                            <div class="shift-name-cell"><c:out value="${h.shiftName}" /> · ${h.workDateDisplay}</div>
                                            <small class="handover-sub">${h.shiftTime} · gửi ${h.createdAtDisplay}</small>
                                        </td>
                                        <td><c:out value="${h.cashierName}" /></td>
                                        <td class="text-end handover-money"><fmt:formatNumber value="${h.openingCash}" pattern="#,##0" />đ</td>
                                        <td class="text-end handover-money"><fmt:formatNumber value="${h.cashSales}" pattern="#,##0" />đ</td>
                                        <td class="text-end handover-money"><fmt:formatNumber value="${h.expectedCash}" pattern="#,##0" />đ</td>
                                        <td class="text-end handover-money"><fmt:formatNumber value="${h.actualCash}" pattern="#,##0" />đ</td>
                                        <td class="text-end">
                                            <span class="handover-diff diff-${h.difference.signum()}">
                                                ${h.differenceType}
                                                <c:if test="${h.difference.signum() ne 0}">
                                                    <c:if test="${h.difference.signum() > 0}">+</c:if><fmt:formatNumber value="${h.difference}" pattern="#,##0" />đ
                                                </c:if>
                                            </span>
                                        </td>
                                        <td class="handover-note-cell">
                                            <c:out value="${empty h.note ? '—' : h.note}" />
                                            <c:if test="${not empty h.reviewNote}">
                                                <div class="handover-review-line">
                                                    <strong>Admin:</strong> <c:out value="${h.reviewNote}" />
                                                </div>
                                            </c:if>
                                        </td>
                                        <td>
                                            <span class="handover-status status-${h.status}">${h.statusLabel}</span>
                                            <c:if test="${not empty h.reviewerName}">
                                                <small class="handover-sub d-block"><c:out value="${h.reviewerName}" /> · ${h.reviewedAtDisplay}</small>
                                            </c:if>
                                        </td>
                                        <td class="text-end text-nowrap">
                                            <c:if test="${h.submitted}">
                                                <button type="button" class="shift-action-button handover-review-button"
                                                        data-bs-toggle="modal" data-bs-target="#handoverReviewModal"
                                                        data-id="${h.id}" data-mode="confirm"
                                                        data-label="${fn:escapeXml(h.shiftName)} ngày ${h.workDateDisplay} - ${fn:escapeXml(h.cashierName)}">
                                                    <span class="material-symbols-outlined" aria-hidden="true">check</span> Xác nhận
                                                </button>
                                                <button type="button" class="shift-action-button is-danger handover-review-button"
                                                        data-bs-toggle="modal" data-bs-target="#handoverReviewModal"
                                                        data-id="${h.id}" data-mode="reject"
                                                        data-label="${fn:escapeXml(h.shiftName)} ngày ${h.workDateDisplay} - ${fn:escapeXml(h.cashierName)}">
                                                    <span class="material-symbols-outlined" aria-hidden="true">close</span> Từ chối
                                                </button>
                                            </c:if>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="shift-empty-state">
                        <span class="material-symbols-outlined" aria-hidden="true">assignment_turned_in</span>
                        <strong>Không có biên bản nào</strong>
                        <span>Biên bản xuất hiện khi Thu ngân chốt ca ở màn Bàn giao ca.</span>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <p class="shift-footnote">
            Chênh lệch = Thực tế − (Đầu ca + Thu tiền mặt). Lệch két thì Thu ngân bắt buộc ghi chú giải trình khi chốt ca.
        </p>
    </main>

    <div class="modal fade" id="handoverReviewModal" tabindex="-1" aria-labelledby="handoverReviewTitle" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content shift-modal-content">
                <form id="handoverReviewForm" method="post" action="${ctx}/admin/handovers" novalidate>
                    <input type="hidden" name="csrfToken" value="${csrf}">
                    <input type="hidden" name="status" value="${statusFilter}">
                    <input type="hidden" name="action" id="handoverReviewAction" value="confirm">
                    <input type="hidden" name="id" id="handoverReviewId" value="">

                    <div class="modal-header shift-modal-header">
                        <div>
                            <h2 class="modal-title" id="handoverReviewTitle">Xác nhận biên bản</h2>
                            <p class="shift-modal-subtitle" id="handoverReviewLabel"></p>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                    </div>

                    <div class="modal-body shift-modal-body">
                        <label class="form-label" for="handoverReviewNote" id="handoverReviewNoteLabel">Ý kiến (không bắt buộc)</label>
                        <textarea class="form-control" id="handoverReviewNote" name="reviewNote" rows="3" maxlength="500"></textarea>
                        <div class="invalid-feedback">Vui lòng nhập lý do từ chối.</div>
                    </div>

                    <div class="modal-footer shift-modal-footer">
                        <button type="button" class="shift-cancel-button" data-bs-dismiss="modal">Huỷ</button>
                        <button type="submit" class="shift-save-button" id="handoverReviewSubmit">Xác nhận</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        (() => {
            const form = document.getElementById('handoverReviewForm');
            const byId = (id) => document.getElementById(id);
            const noteInput = byId('handoverReviewNote');

            document.querySelectorAll('.handover-review-button').forEach((button) => {
                button.addEventListener('click', () => {
                    const reject = button.dataset.mode === 'reject';
                    form.classList.remove('was-validated');
                    byId('handoverReviewAction').value = button.dataset.mode;
                    byId('handoverReviewId').value = button.dataset.id;
                    byId('handoverReviewLabel').textContent = button.dataset.label;
                    byId('handoverReviewTitle').textContent = reject ? 'Từ chối biên bản' : 'Xác nhận biên bản';
                    byId('handoverReviewNoteLabel').textContent = reject ? 'Lý do từ chối *' : 'Ý kiến (không bắt buộc)';
                    byId('handoverReviewSubmit').textContent = reject ? 'Từ chối' : 'Xác nhận';
                    noteInput.required = reject;
                    noteInput.value = '';
                });
            });

            // Từ chối bắt buộc lý do; server vẫn kiểm tra lại
            form.addEventListener('submit', (event) => {
                noteInput.value = noteInput.value.trim();
                if (!form.checkValidity()) {
                    event.preventDefault();
                    event.stopPropagation();
                    noteInput.focus();
                }
                form.classList.add('was-validated');
            });
        })();
    </script>
</body>
</html>
