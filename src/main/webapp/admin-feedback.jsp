<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty feedbacks and empty feedbackTotal}">
    <c:redirect url="/admin/feedback" />
</c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Aura Café | Đánh giá &amp; khiếu nại</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <style>
        .feedback-page { max-width: 1280px; color: #292524; font-family: 'Plus Jakarta Sans', sans-serif; }
        .feedback-head, .feedback-table { background: #fff; border: 1px solid #e7e5e4; border-radius: 16px; box-shadow: 0 1px 3px rgba(28, 25, 23, 0.04); }
        .feedback-head { padding: 22px; }
        .table-ref { padding: 4px 10px; border-radius: 6px; background: #f5f5f4; color: #78716c; font: 700 11px ui-monospace, monospace; display: inline-block; }
        .feedback-head h1 { font-size: 24px; font-weight: 800; letter-spacing: -0.025em; margin: 8px 0 4px; color: #1c1917; }
        .feedback-head p { font-size: 13px; color: #78716c; margin: 0; }
        .filter-select { height: 38px; padding: 0 34px 0 14px; border: 1px solid #e7e5e4; border-radius: 12px; background: #fafaf9 url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2378716c' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E") no-repeat right 12px center; appearance: none; -webkit-appearance: none; font-size: 12.5px; font-weight: 600; color: #44403c; cursor: pointer; transition: all 0.15s; }
        .filter-select:focus { outline: none; border-color: #a51225; box-shadow: 0 0 0 3px rgba(165, 18, 37, 0.1); }
        .feedback-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; padding-top: 16px; margin-top: 16px; border-top: 1px solid #f5f5f4; }
        .stat-box { padding: 14px 16px; border: 1px solid #e7e5e4; border-radius: 12px; background: #fafaf9; }
        .stat-box.danger { background: #fff1f2; border-color: #fecdd3; color: #be123c; }
        .stat-box.primary { background: #fff7f7; border-color: #fecdd3; color: #a51225; }
        .stat-box span { display: block; font-size: 10px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.05em; opacity: 0.76; }
        .stat-box strong { display: block; margin-top: 4px; font: 900 22px ui-monospace, monospace; }
        .feedback-table { overflow: hidden; }
        .feedback-table table { font-size: 12.5px; margin: 0; }
        .feedback-table th { padding: 16px 18px; background: #fafaf9; color: #78716c; font-weight: 700; border-bottom: 1px solid #f5f5f4; white-space: nowrap; font-size: 12px; text-transform: uppercase; letter-spacing: 0.03em; }
        .feedback-table td { padding: 16px 18px; border-color: #f5f5f4; vertical-align: top; }
        .feedback-table tbody tr { transition: background 0.15s; }
        .feedback-table tbody tr:hover { background: #fafaf980; }
        .code { font-family: ui-monospace, monospace; font-weight: 700; }
        .order-code { color: #a51225; font-size: 12px; }
        .customer-phone, .created { font-size: 11px; color: #a8a29e; }
        .stars { color: #f59e0b; font-size: 14px; white-space: nowrap; letter-spacing: 2px; }
        .pill { display: inline-block; padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 700; white-space: nowrap; }
        .pill-blue { background: #dbeafe; color: #1e40af; }
        .pill-amber { background: #fef3c7; color: #92400e; }
        .pill-pending { background: #fff1f2; color: #be123c; }
        .pill-resolved { background: #dcfce7; color: #166534; }
        .pill-rejected { background: #f3f4f6; color: #4b5563; }
        .reply-btn { border: 0; border-radius: 8px; padding: 6px 12px; background: #fff1f2; color: #a51225; font-size: 11.5px; font-weight: 800; display: inline-flex; align-items: center; gap: 5px; white-space: nowrap; cursor: pointer; transition: all 0.2s; }
        .reply-btn:hover { background: #a51225; color: #fff; }
        .reply-btn .material-symbols-outlined { font-size: 15px; }
        .modal-content { border: 0; border-radius: 18px; overflow: hidden; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04); }
        .modal-header { background: #fafaf9; border-color: #f5f5f4; padding: 18px 24px; }
        .modal-title { font-size: 15px; font-weight: 800; color: #1c1917; }
        .review-quote { padding: 14px 16px; background: #fafaf9; border: 1px solid #e7e5e4; border-radius: 12px; font-size: 12.5px; }
        .review-quote small { display: block; color: #78716c; font-size: 10px; font-weight: 800; text-transform: uppercase; }
        .review-quote p { margin: 4px 0 0; font-style: italic; color: #292524; line-height: 1.45; }
        .form-label { font-size: 12px; font-weight: 700; color: #57534e; }
        .db-column { float: right; font: 400 10.5px ui-monospace, monospace; color: #a8a29e; }
        .form-control, .form-select { border-radius: 12px; background: #fafaf9; border-color: #e7e5e4; font-size: 13px; padding: 10px 14px; }
        .form-control:focus, .form-select:focus { border-color: #a51225; box-shadow: 0 0 0 3px rgba(165, 18, 37, 0.1); background: #fff; }
        .btn-aura { background: #a51225; color: #fff; font-size: 12.5px; font-weight: 700; border-radius: 12px; border: none; padding: 10px 18px; transition: all 0.2s; }
        .btn-aura:hover { background: #880e1f; color: #fff; }
        .btn-soft { background: #f5f5f4; color: #44403c; font-size: 12.5px; font-weight: 700; border-radius: 12px; border: none; padding: 10px 18px; transition: all 0.2s; }
        .btn-soft:hover { background: #e7e5e4; color: #1c1917; }
        .review-content { font-size: 12.5px; color: #292524; line-height: 1.45; }
        .admin-reply-text { font-size: 12px; color: #44403c; line-height: 1.4; display: inline-block; background: #fdf2f4; border-left: 3px solid #a51225; padding: 6px 10px; border-radius: 0 6px 6px 0; }
        .alert-banner { border-radius: 12px; padding: 12px 18px; font-size: 13px; font-weight: 600; display: flex; align-items: center; gap: 8px; margin-bottom: 20px; }
        .alert-success-aura { background: #ecfdf5; border: 1px solid #a7f3d0; color: #065f46; }
        .alert-error-aura { background: #fff1f2; border: 1px solid #fecdd3; color: #be123c; }
        @media (max-width: 900px) {
            .feedback-stats { grid-template-columns: repeat(2, 1fr); }
        }
    </style>
</head>
<body class="admin-body">
    <jsp:include page="/components/admin-sidebar.jsp">
        <jsp:param name="activeMenu" value="feedback" />
    </jsp:include>

    <main class="admin-main-wrapper feedback-page">
        <!-- Notification Banners -->
        <c:if test="${not empty feedbackMessage}">
            <div class="alert-banner alert-success-aura alert-dismissible fade show" role="alert">
                <span class="material-symbols-outlined fs-5">check_circle</span>
                <span><c:out value="${feedbackMessage}" /></span>
                <button type="button" class="btn-close ms-auto" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        </c:if>
        <c:if test="${not empty feedbackError}">
            <div class="alert-banner alert-error-aura alert-dismissible fade show" role="alert">
                <span class="material-symbols-outlined fs-5">error</span>
                <span><c:out value="${feedbackError}" /></span>
                <button type="button" class="btn-close ms-auto" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        </c:if>

        <!-- Header section -->
        <section class="feedback-head mb-4">
            <div class="d-flex flex-column flex-lg-row align-items-lg-center justify-content-between gap-3">
                <div>
                    <span class="table-ref">Bảng: customer_reviews</span>
                    <h1>Đánh giá &amp; khiếu nại</h1>
                    <p>Tiếp nhận đánh giá sao (RATING) và khiếu nại (COMPLAINT) theo từng đơn hàng; phản hồi trực tiếp tới khách.</p>
                </div>
                <div class="d-flex gap-2 flex-wrap">
                    <select class="filter-select" id="filterType" onchange="onFilterChange()">
                        <option value="ALL" ${selectedType == 'ALL' ? 'selected' : ''}>Tất cả loại</option>
                        <option value="RATING" ${selectedType == 'RATING' ? 'selected' : ''}>Đánh giá</option>
                        <option value="COMPLAINT" ${selectedType == 'COMPLAINT' ? 'selected' : ''}>Khiếu nại</option>
                    </select>
                    <select class="filter-select" id="filterStatus" onchange="onFilterChange()">
                        <option value="ALL" ${selectedStatus == 'ALL' ? 'selected' : ''}>Tất cả trạng thái</option>
                        <option value="PENDING" ${selectedStatus == 'PENDING' ? 'selected' : ''}>Chờ xử lý</option>
                        <option value="PROCESSING" ${selectedStatus == 'PROCESSING' ? 'selected' : ''}>Đang xử lý</option>
                        <option value="RESOLVED" ${selectedStatus == 'RESOLVED' ? 'selected' : ''}>Đã giải quyết</option>
                        <option value="REJECTED" ${selectedStatus == 'REJECTED' ? 'selected' : ''}>Từ chối</option>
                    </select>
                </div>
            </div>

            <!-- 4 Stat Boxes -->
            <div class="feedback-stats">
                <div class="stat-box">
                    <span>Tổng phiếu</span>
                    <strong id="statTotal">${feedbackTotal != null ? feedbackTotal : 0}</strong>
                </div>
                <div class="stat-box danger">
                    <span>Chờ xử lý</span>
                    <strong id="statPending">${pendingTotal != null ? pendingTotal : 0}</strong>
                </div>
                <div class="stat-box">
                    <span>Khiếu nại</span>
                    <strong id="statComplaint">${complaintTotal != null ? complaintTotal : 0}</strong>
                </div>
                <div class="stat-box primary">
                    <span>Điểm đánh giá TB</span>
                    <strong id="statRating">${avgRatingFormatted != null ? avgRatingFormatted : '--'} ★</strong>
                </div>
            </div>
        </section>

        <!-- Feedback Table -->
        <section class="feedback-table table-responsive">
            <table class="table align-middle" id="feedbackTable">
                <thead>
                    <tr>
                        <th>Mã</th>
                        <th>Đơn hàng</th>
                        <th>Khách hàng</th>
                        <th>Loại</th>
                        <th>Đánh giá</th>
                        <th>Nội dung</th>
                        <th>Phản hồi</th>
                        <th>Trạng thái</th>
                        <th class="text-end">Thao tác</th>
                    </tr>
                </thead>
                <tbody id="feedbackTableBody">
                    <c:forEach var="fb" items="${feedbacks}">
                        <tr class="feedback-row" data-type="${fb.issueType}" data-status="${fb.status}">
                            <td class="code">#${fb.id}</td>
                            <td class="code order-code">${fb.orderCode}</td>
                            <td>
                                <strong><c:out value="${fb.customerName}" /></strong><br>
                                <span class="customer-phone code"><c:out value="${fb.customerPhone}" /></span>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${fb.issueType == 'RATING'}">
                                        <span class="pill pill-blue">Đánh giá</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pill pill-amber">Khiếu nại</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${fb.rating > 0}">
                                        <span class="stars" title="${fb.rating}/5 sao">
                                            <c:forEach begin="1" end="${fb.rating}">★</c:forEach><c:forEach begin="${fb.rating + 1}" end="5">☆</c:forEach>
                                        </span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="text-muted code">--</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td style="min-width: 220px; max-width: 320px;">
                                <div class="review-content"><c:out value="${fb.content}" /></div>
                                <span class="created d-block mt-1">${fb.formattedDate}</span>
                            </td>
                            <td style="min-width: 180px; max-width: 260px;">
                                <c:choose>
                                    <c:when test="${not empty fb.adminResponse}">
                                        <span class="admin-reply-text"><c:out value="${fb.adminResponse}" /></span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="text-muted">—</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${fb.status == 'PENDING'}">
                                        <span class="pill pill-pending">Chờ xử lý</span>
                                    </c:when>
                                    <c:when test="${fb.status == 'PROCESSING'}">
                                        <span class="pill pill-blue">Đang xử lý</span>
                                    </c:when>
                                    <c:when test="${fb.status == 'RESOLVED'}">
                                        <span class="pill pill-resolved">Đã giải quyết</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pill pill-rejected">Từ chối</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-end">
                                <button type="button" class="reply-btn btn-open-reply"
                                        data-id="${fb.id}"
                                        data-type="${fb.issueType}"
                                        data-type-label="${fb.issueTypeLabel}"
                                        data-content="<c:out value="${fb.content}" />"
                                        data-status="${fb.status}"
                                        data-response="<c:out value="${fb.adminResponse}" />"
                                        data-bs-toggle="modal" data-bs-target="#replyModal">
                                    <span class="material-symbols-outlined">reply</span>
                                    <c:choose>
                                        <c:when test="${empty fb.adminResponse and (fb.status == 'PENDING' or fb.status == 'PROCESSING')}">
                                            Xử lý
                                        </c:when>
                                        <c:otherwise>
                                            Sửa phản hồi
                                        </c:otherwise>
                                    </c:choose>
                                </button>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty feedbacks}">
                        <tr id="emptyRow">
                            <td colspan="9" class="text-center py-5 text-muted">
                                <span class="material-symbols-outlined fs-2 text-muted mb-2 d-block">inbox</span>
                                Không có đánh giá &amp; khiếu nại nào phù hợp.
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </section>
    </main>

    <!-- Modal Xử lý / Phản hồi -->
    <div class="modal fade" id="replyModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header">
                    <h2 class="modal-title" id="replyModalTitle">Phản hồi phiếu #</h2>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <form id="replyForm" method="POST" action="${ctx}/admin/feedback">
                        <input type="hidden" name="csrfToken" value="${sessionScope.feedbackCsrfToken}" />
                        <input type="hidden" name="action" value="reply" />
                        <input type="hidden" name="id" id="modalFeedbackId" value="" />

                        <div class="review-quote mb-3">
                            <small id="modalQuoteType">Ý kiến khách hàng:</small>
                            <p id="modalQuoteContent">“...”</p>
                        </div>

                        <label class="form-label w-100">
                            Trạng thái xử lý <span class="db-column">status</span>
                            <select name="status" id="modalStatus" class="form-select mt-1">
                                <option value="RESOLVED">RESOLVED - Đã giải quyết</option>
                                <option value="PROCESSING">PROCESSING - Đang xử lý</option>
                                <option value="REJECTED">REJECTED - Từ chối</option>
                                <option value="PENDING">PENDING - Chờ xử lý</option>
                            </select>
                        </label>

                        <label class="form-label w-100 mt-3">
                            Nội dung phản hồi * <span class="db-column">admin_response</span>
                            <textarea name="adminResponse" id="modalAdminResponse" class="form-control mt-1" rows="4" placeholder="Nhập câu trả lời hoặc phương án xử lý gửi tới khách hàng..." required></textarea>
                        </label>

                        <div class="d-flex gap-3 mt-4">
                            <button type="button" class="btn btn-soft flex-fill" data-bs-dismiss="modal">Hủy bỏ</button>
                            <button type="submit" class="btn btn-aura flex-fill">Gửi phản hồi</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        document.addEventListener('DOMContentLoaded', function () {
            // Đăng ký sự kiện mở modal phản hồi
            document.querySelectorAll('.btn-open-reply').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    const id = this.getAttribute('data-id') || '';
                    const typeLabel = this.getAttribute('data-type-label') || 'Đánh giá / Khiếu nại';
                    const content = this.getAttribute('data-content') || '';
                    let status = this.getAttribute('data-status') || 'RESOLVED';
                    const response = this.getAttribute('data-response') || '';

                    // Nếu đang PENDING, mặc định gợi ý chuyển sang RESOLVED khi phản hồi
                    if (status === 'PENDING') {
                        status = 'RESOLVED';
                    }

                    document.getElementById('modalFeedbackId').value = id;
                    document.getElementById('replyModalTitle').textContent = 'Phản hồi phiếu #' + id;
                    document.getElementById('modalQuoteType').textContent = 'Ý kiến khách hàng (' + typeLabel + '):';
                    document.getElementById('modalQuoteContent').textContent = '“' + content + '”';
                    document.getElementById('modalStatus').value = status;
                    document.getElementById('modalAdminResponse').value = response;
                });
            });
        });

        // Hỗ trợ lọc cả frontend tức thì lẫn qua URL
        function onFilterChange() {
            const selectedType = document.getElementById('filterType').value;
            const selectedStatus = document.getElementById('filterStatus').value;

            // Lọc trực tiếp các hàng trên giao diện tức thì
            const rows = document.querySelectorAll('.feedback-row');
            let visibleCount = 0;

            rows.forEach(function (row) {
                const rowType = row.getAttribute('data-type');
                const rowStatus = row.getAttribute('data-status');

                const matchType = (selectedType === 'ALL' || rowType === selectedType);
                const matchStatus = (selectedStatus === 'ALL' || rowStatus === selectedStatus);

                if (matchType && matchStatus) {
                    row.style.display = '';
                    visibleCount++;
                } else {
                    row.style.display = 'none';
                }
            });

            // Cập nhật dòng thông báo trống nếu không có kết quả
            let emptyMsg = document.getElementById('dynamicEmptyRow');
            if (visibleCount === 0) {
                if (!emptyMsg) {
                    emptyMsg = document.createElement('tr');
                    emptyMsg.id = 'dynamicEmptyRow';
                    emptyMsg.innerHTML = '<td colspan="9" class="text-center py-5 text-muted">' +
                        '<span class="material-symbols-outlined fs-2 text-muted mb-2 d-block">filter_alt_off</span>' +
                        'Không có phiếu nào phù hợp với bộ lọc hiện tại.</td>';
                    document.getElementById('feedbackTableBody').appendChild(emptyMsg);
                } else {
                    emptyMsg.style.display = '';
                }
            } else if (emptyMsg) {
                emptyMsg.style.display = 'none';
            }

            // Đồng bộ URL query parameters
            const url = new URL(window.location.href);
            if (selectedType === 'ALL') {
                url.searchParams.delete('type');
            } else {
                url.searchParams.set('type', selectedType);
            }
            if (selectedStatus === 'ALL') {
                url.searchParams.delete('status');
            } else {
                url.searchParams.set('status', selectedStatus);
            }
            window.history.replaceState({}, '', url.toString());
        }
    </script>
</body>
</html>
