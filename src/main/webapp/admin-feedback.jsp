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
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/admin-schedule.css" rel="stylesheet">
    <style>
        .feedback-page { max-width: 1320px; color: #1e293b; font-family: 'Plus Jakarta Sans', sans-serif; }
        .feedback-head, .feedback-table { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03), 0 6px 16px rgba(0, 0, 0, 0.02); }
        .feedback-head { padding: 22px 24px; }
        .table-ref { padding: 4px 10px; border-radius: 6px; background: #f1f5f9; color: #64748b; font: 700 11px ui-monospace, monospace; display: inline-block; }
        .feedback-head h1 { font-size: 24px; font-weight: 800; letter-spacing: -0.025em; margin: 8px 0 4px; color: #0f172a; }
        .feedback-head p { font-size: 13px; color: #64748b; margin: 0; }
        
        .filter-select { height: 38px; padding: 0 34px 0 14px; border: 1px solid #cbd5e1; border-radius: 12px; background: #f8fafc url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E") no-repeat right 12px center; appearance: none; -webkit-appearance: none; font-size: 12.5px; font-weight: 600; color: #334155; cursor: pointer; transition: all 0.2s; }
        .filter-select:focus { outline: none; border-color: #be123c; box-shadow: 0 0 0 3px rgba(190, 18, 60, 0.12); background-color: #fff; }
        
        .feedback-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; padding-top: 16px; margin-top: 16px; border-top: 1px solid #f1f5f9; }
        .stat-box { padding: 14px 18px; border: 1px solid #e2e8f0; border-radius: 12px; background: #f8fafc; transition: all 0.2s; }
        .stat-box:hover { transform: translateY(-1px); box-shadow: 0 3px 8px rgba(0, 0, 0, 0.04); }
        .stat-box.danger { background: #fff1f2; border-color: #fecdd3; color: #be123c; }
        .stat-box.primary { background: #fff7f7; border-color: #fecdd3; color: #a51225; }
        .stat-box span { display: block; font-size: 10.5px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.05em; opacity: 0.8; }
        .stat-box strong { display: block; margin-top: 4px; font: 900 23px ui-monospace, monospace; }
        
        .feedback-table { overflow-x: auto; -webkit-overflow-scrolling: touch; }
        .feedback-table table { font-size: 12.5px; margin: 0; min-width: 1050px; width: 100%; border-collapse: separate; border-spacing: 0; }
        .feedback-table th { padding: 15px 16px; background: #f8fafc; color: #64748b; font-weight: 700; border-bottom: 1px solid #e2e8f0; white-space: nowrap; font-size: 11.5px; text-transform: uppercase; letter-spacing: 0.04em; }
        .feedback-table td { padding: 16px 16px; border-bottom: 1px solid #f1f5f9; vertical-align: middle; }
        .feedback-table tbody tr { transition: background 0.15s; }
        .feedback-table tbody tr:hover { background: #f8fafc; }
        
        .code { font-family: ui-monospace, monospace; font-weight: 700; }
        .order-code { color: #be123c; font-size: 12px; letter-spacing: 0.02em; }
        .customer-name { font-size: 13px; font-weight: 700; color: #0f172a; }
        .customer-phone { font-size: 11px; color: #64748b; font-family: ui-monospace, monospace; margin-top: 2px; }
        .created { font-size: 11px; color: #94a3b8; }
        .stars { color: #f59e0b; font-size: 14.5px; white-space: nowrap; letter-spacing: 2px; }
        
        /* Pills & Badges */
        .pill { display: inline-flex; align-items: center; justify-content: center; padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 700; white-space: nowrap; }
        .pill-blue { background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe; }
        .pill-amber { background: #fffbeb; color: #d97706; border: 1px solid #fde68a; }
        .pill-pending { background: #fff1f2; color: #e11d48; border: 1px solid #fecdd3; }
        .pill-processing { background: #f0f9ff; color: #0284c7; border: 1px solid #bae6fd; }
        .pill-resolved { background: #ecfdf5; color: #059669; border: 1px solid #a7f3d0; }
        .pill-rejected { background: #f1f5f9; color: #475569; border: 1px solid #cbd5e1; }
        
        /* Content & Reply columns */
        .review-content { font-size: 12.5px; color: #1e293b; line-height: 1.45; font-weight: 500; }
        .admin-reply-box { font-size: 12px; color: #1e293b; line-height: 1.45; background: #f8fafc; border: 1px solid #e2e8f0; border-left: 3px solid #10b981; padding: 6px 10px; border-radius: 0 8px 8px 0; display: inline-block; max-width: 100%; word-break: break-word; }
        
        /* Modern Action Column & Buttons */
        .th-action { width: 150px; min-width: 150px; text-align: center !important; }
        .td-action { width: 150px; min-width: 150px; text-align: center !important; }
        
        /* Nút Xử lý (Nổi bật - Hành động ngay) */
        .btn-action-process {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 5px;
            padding: 7px 16px;
            background: linear-gradient(135deg, #be123c 0%, #9f1239 100%);
            color: #ffffff !important;
            border: 1px solid #9f1239;
            border-radius: 20px;
            font-size: 11.5px;
            font-weight: 700;
            box-shadow: 0 2px 5px rgba(190, 18, 60, 0.22);
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            cursor: pointer;
            white-space: nowrap;
            text-decoration: none;
        }
        .btn-action-process:hover {
            background: linear-gradient(135deg, #9f1239 0%, #881337 100%);
            transform: translateY(-1px);
            box-shadow: 0 4px 10px rgba(190, 18, 60, 0.32);
        }
        .btn-action-process:active {
            transform: translateY(0);
        }
        
        /* Nút Sửa phản hồi (Thanh lịch - Trang nhã) */
        .btn-action-edit {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 5px;
            padding: 6px 14px;
            background: #ffffff;
            color: #475569 !important;
            border: 1px solid #cbd5e1;
            border-radius: 20px;
            font-size: 11.5px;
            font-weight: 600;
            box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            cursor: pointer;
            white-space: nowrap;
            text-decoration: none;
        }
        .btn-action-edit:hover {
            background: #f8fafc;
            color: #0f172a !important;
            border-color: #94a3b8;
            transform: translateY(-1px);
            box-shadow: 0 3px 8px rgba(0, 0, 0, 0.08);
        }
        .btn-action-edit:active {
            transform: translateY(0);
        }
        .action-complete {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 5px;
            color: #059669;
            font-size: 11.5px;
            font-weight: 700;
            white-space: nowrap;
        }
        .action-complete.rejected { color: #64748b; }
        
        /* Modal & Dialog */
        .modal-content { border: 0; border-radius: 20px; overflow: hidden; box-shadow: 0 25px 30px -5px rgba(0, 0, 0, 0.15), 0 15px 15px -5px rgba(0, 0, 0, 0.04); }
        .modal-header { background: #f8fafc; border-color: #e2e8f0; padding: 18px 24px; }
        .modal-title { font-size: 15px; font-weight: 800; color: #0f172a; }
        .review-quote { padding: 14px 16px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; font-size: 12.5px; }
        .review-quote small { display: block; color: #64748b; font-size: 10px; font-weight: 800; text-transform: uppercase; }
        .review-quote p { margin: 4px 0 0; font-style: italic; color: #1e293b; line-height: 1.45; }
        .form-label { font-size: 12px; font-weight: 700; color: #475569; }
        .db-column { float: right; font: 400 10.5px ui-monospace, monospace; color: #94a3b8; }
        .form-control, .form-select { border-radius: 12px; background: #f8fafc; border-color: #cbd5e1; font-size: 13px; padding: 10px 14px; }
        .form-control:focus, .form-select:focus { border-color: #be123c; box-shadow: 0 0 0 3px rgba(190, 18, 60, 0.12); background: #ffffff; }
        .terminal-note { margin-top: 8px; color: #64748b; font-size: 11px; line-height: 1.45; }
        .btn-aura { background: linear-gradient(135deg, #be123c 0%, #9f1239 100%); color: #fff; font-size: 12.5px; font-weight: 700; border-radius: 12px; border: none; padding: 10px 18px; transition: all 0.2s; }
        .btn-aura:hover { background: linear-gradient(135deg, #9f1239 0%, #881337 100%); color: #fff; box-shadow: 0 4px 12px rgba(190, 18, 60, 0.3); }
        .btn-soft { background: #f1f5f9; color: #475569; font-size: 12.5px; font-weight: 700; border-radius: 12px; border: none; padding: 10px 18px; transition: all 0.2s; }
        .btn-soft:hover { background: #e2e8f0; color: #0f172a; }
        
        /* Alert Notification */
        .alert-banner { border-radius: 12px; padding: 12px 18px; font-size: 13px; font-weight: 600; display: flex; align-items: center; gap: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02); }
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
                        <th style="width: 50px; text-align: center;">Mã</th>
                        <th style="width: 120px;">Đơn hàng</th>
                        <th style="width: 160px;">Khách hàng</th>
                        <th style="width: 110px; text-align: center;">Loại</th>
                        <th style="width: 110px; text-align: center;">Đánh giá</th>
                        <th style="min-width: 220px;">Nội dung</th>
                        <th style="min-width: 180px;">Phản hồi</th>
                        <th style="width: 120px; text-align: center;">Trạng thái</th>
                        <th class="th-action">Thao tác</th>
                    </tr>
                </thead>
                <tbody id="feedbackTableBody">
                    <c:forEach var="fb" items="${feedbacks}">
                        <tr class="feedback-row" data-type="${fb.issueType}" data-status="${fb.status}">
                            <td class="code text-center">#${fb.id}</td>
                            <td class="code order-code">${fb.orderCode}</td>
                            <td>
                                <div class="customer-name"><c:out value="${fb.customerName}" /></div>
                                <div class="customer-phone"><c:out value="${fb.customerPhone}" /></div>
                            </td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${fb.issueType == 'RATING'}">
                                        <span class="pill pill-blue">Đánh giá</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pill pill-amber">Khiếu nại</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-center">
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
                            <td>
                                <div class="review-content"><c:out value="${fb.content}" /></div>
                                <span class="created d-block mt-1">${fb.formattedDate}</span>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${not empty fb.adminResponse}">
                                        <div class="admin-reply-box"><c:out value="${fb.adminResponse}" /></div>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="text-muted">—</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${fb.status == 'PENDING'}">
                                        <span class="pill pill-pending">Chờ xử lý</span>
                                    </c:when>
                                    <c:when test="${fb.status == 'PROCESSING'}">
                                        <span class="pill pill-processing">Đang xử lý</span>
                                    </c:when>
                                    <c:when test="${fb.status == 'RESOLVED'}">
                                        <span class="pill pill-resolved">Đã giải quyết</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pill pill-rejected">Từ chối</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="td-action">
                                <c:choose>
                                    <c:when test="${fb.status == 'PENDING' or fb.status == 'PROCESSING'}">
                                        <button type="button" class="btn-action-process btn-open-reply"
                                                data-id="${fb.id}"
                                                data-type="${fb.issueType}"
                                                data-type-label="${fb.issueTypeLabel}"
                                                data-content="<c:out value="${fb.content}" />"
                                                data-status="${fb.status}"
                                                data-response="<c:out value="${fb.adminResponse}" />"
                                                data-bs-toggle="modal" data-bs-target="#replyModal">
                                            <span class="material-symbols-outlined" style="font-size: 15px;">reply</span>
                                            <span>${empty fb.adminResponse ? 'Xử lý' : 'Tiếp tục xử lý'}</span>
                                        </button>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="action-complete ${fb.status == 'REJECTED' ? 'rejected' : ''}">
                                            <span class="material-symbols-outlined" style="font-size: 16px;">task_alt</span>
                                            <span>Đã hoàn tất</span>
                                        </span>
                                    </c:otherwise>
                                </c:choose>
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
                                <option value="PROCESSING">PROCESSING - Đang xử lý</option>
                                <option value="RESOLVED">RESOLVED - Đã giải quyết (đóng phiếu)</option>
                                <option value="REJECTED">REJECTED - Từ chối (đóng phiếu)</option>
                            </select>
                            <span class="terminal-note">Phiếu Đã giải quyết hoặc Từ chối sẽ được đóng và không thể sửa hay mở lại.</span>
                        </label>

                        <label class="form-label w-100 mt-3">
                            Nội dung phản hồi * <span class="db-column">admin_response</span>
                            <textarea name="adminResponse" id="modalAdminResponse" class="form-control mt-1" rows="4" maxlength="2000" placeholder="Nhập câu trả lời hoặc phương án xử lý gửi tới khách hàng..." required></textarea>
                        </label>

                        <div class="d-flex gap-3 mt-4">
                            <button type="button" class="btn btn-soft flex-fill" data-bs-dismiss="modal">Hủy bỏ</button>
                            <button type="submit" class="btn btn-aura flex-fill" id="replySubmitButton">Gửi phản hồi</button>
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
                    let status = this.getAttribute('data-status') || 'PROCESSING';
                    const response = this.getAttribute('data-response') || '';

                    // Gửi phản hồi là bắt đầu xử lý; đóng phiếu phải là lựa chọn có chủ đích.
                    if (status === 'PENDING') {
                        status = 'PROCESSING';
                    }

                    document.getElementById('modalFeedbackId').value = id;
                    document.getElementById('replyModalTitle').textContent = 'Phản hồi phiếu #' + id;
                    document.getElementById('modalQuoteType').textContent = 'Ý kiến khách hàng (' + typeLabel + '):';
                    document.getElementById('modalQuoteContent').textContent = '“' + content + '”';
                    document.getElementById('modalStatus').value = status;
                    document.getElementById('modalAdminResponse').value = response;
                });
            });

            document.getElementById('replyForm').addEventListener('submit', function () {
                const submitButton = document.getElementById('replySubmitButton');
                submitButton.disabled = true;
                submitButton.textContent = 'Đang gửi...';
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
