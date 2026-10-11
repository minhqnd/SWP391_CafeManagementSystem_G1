<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Aura Café | Phản ánh & Đánh giá dịch vụ</title>
    <meta name="description" content="Trung tâm tiếp nhận đánh giá và khiếu nại khách hàng Aura Café - Cam kết dịch vụ vàng 100% hài lòng." />

    <!-- Google Fonts & Icons -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link
        href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Inter:wght@400;500;600;700;800&display=swap"
        rel="stylesheet" />
    <link
        href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200"
        rel="stylesheet" />

    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">

    <style>
        :root {
            --primary: #8b1528;
            --primary-hover: #70101f;
            --primary-soft: #fdf2f4;
            --primary-border: #f9d2d9;
            --surface: #ffffff;
            --bg-body: #f8fafc;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --border-color: #e2e8f0;
        }

        body {
            font-family: 'Plus Jakarta Sans', 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background-color: var(--bg-body);
            color: var(--text-main);
            letter-spacing: -0.01em;
            overflow-x: hidden;
        }

        /* Breadcrumb */
        .feedback-breadcrumb {
            padding: 1rem 0 0.5rem;
            font-size: 0.825rem;
        }
        .feedback-breadcrumb a {
            color: var(--text-muted);
            text-decoration: none;
            font-weight: 500;
            transition: color 0.2s;
        }
        .feedback-breadcrumb a:hover {
            color: var(--primary);
        }
        .feedback-breadcrumb .separator {
            color: #cbd5e1;
            margin: 0 0.5rem;
        }
        .feedback-breadcrumb .current {
            color: #1e293b;
            font-weight: 600;
        }

        /* Hero Banner */
        .hero-care-banner {
            background: linear-gradient(135deg, #1c1917 0%, #8b1528 50%, #1c1917 100%);
            border-radius: 1.25rem;
            padding: 1.75rem 2rem;
            color: #ffffff;
            margin-bottom: 2rem;
            box-shadow: 0 10px 25px -5px rgba(139, 21, 40, 0.25);
            position: relative;
            overflow: hidden;
        }
        .hero-care-banner::after {
            content: '';
            position: absolute;
            top: -50%;
            right: -10%;
            width: 320px;
            height: 320px;
            background: radial-gradient(circle, rgba(255, 255, 255, 0.08) 0%, transparent 70%);
            border-radius: 50%;
            pointer-events: none;
        }
        .hero-agent-icon {
            width: 3.5rem;
            height: 3.5rem;
            border-radius: 1rem;
            background: rgba(255, 255, 255, 0.12);
            backdrop-filter: blur(8px);
            display: flex;
            align-items: center;
            justify-content: center;
            color: #fcd34d;
            border: 1px solid rgba(255, 255, 255, 0.2);
            flex-shrink: 0;
        }

        /* Form Card */
        .feedback-card {
            background: #ffffff;
            border-radius: 1.25rem;
            border: 1px solid #e2e8f0;
            padding: 2rem;
            box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.03);
        }
        .feedback-form-title {
            font-size: 1.15rem;
            font-weight: 800;
            color: #0f172a;
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }
        .form-label-custom {
            display: block;
            font-size: 0.8rem;
            font-weight: 700;
            color: #334155;
            margin-bottom: 0.45rem;
        }
        .form-control-custom, .form-select-custom {
            width: 100%;
            padding: 0.65rem 0.95rem;
            font-size: 0.85rem;
            border-radius: 0.75rem;
            border: 1px solid #cbd5e1;
            background-color: #f8fafc;
            font-weight: 500;
            color: #1e293b;
            transition: all 0.2s ease;
        }
        .form-control-custom:focus, .form-select-custom:focus {
            outline: none;
            border-color: var(--primary);
            background-color: #ffffff;
            box-shadow: 0 0 0 3px rgba(139, 21, 40, 0.08);
        }

        /* Issue Type Switch Buttons */
        .type-toggle-btn {
            border-radius: 0.75rem;
            padding: 0.7rem 1rem;
            font-size: 0.85rem;
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            transition: all 0.2s ease;
            cursor: pointer;
            border: 2px solid transparent;
        }
        .type-toggle-btn.btn-complaint {
            background-color: #fef2f2;
            color: #991b1b;
            border-color: #fecaca;
        }
        .type-toggle-btn.btn-complaint.active {
            background-color: #dc2626;
            color: #ffffff;
            border-color: #b91c1c;
            box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
        }
        .type-toggle-btn.btn-rating {
            background-color: #fffbeb;
            color: #92400e;
            border-color: #fde68a;
        }
        .type-toggle-btn.btn-rating.active {
            background-color: #d97706;
            color: #ffffff;
            border-color: #b45309;
            box-shadow: 0 4px 12px rgba(217, 119, 6, 0.25);
        }

        /* Star Rating */
        .star-rating-box {
            display: flex;
            align-items: center;
            gap: 0.35rem;
        }
        .star-btn {
            background: none;
            border: none;
            padding: 0;
            cursor: pointer;
            font-size: 1.85rem;
            color: #cbd5e1;
            transition: transform 0.15s, color 0.15s;
            line-height: 1;
        }
        .star-btn:hover {
            transform: scale(1.2);
        }
        .star-btn.filled {
            color: #f59e0b;
            font-variation-settings: 'FILL' 1;
        }
        .star-rating-label {
            font-size: 0.8rem;
            color: #d97706;
            font-weight: 700;
            margin-left: 0.65rem;
        }

        /* Submit Button */
        .btn-submit-feedback {
            width: 100%;
            padding: 0.85rem;
            border-radius: 0.75rem;
            background-color: var(--primary);
            color: #ffffff;
            font-weight: 700;
            font-size: 0.925rem;
            border: none;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            box-shadow: 0 4px 14px rgba(139, 21, 40, 0.25);
            transition: all 0.2s ease;
        }
        .btn-submit-feedback:hover {
            background-color: var(--primary-hover);
            transform: translateY(-1px);
        }
        .btn-submit-feedback:active {
            transform: scale(0.99);
        }

        .btn-submit-feedback:disabled,
        .form-select-custom:disabled {
            cursor: not-allowed;
            opacity: 0.6;
        }

        /* Ticket Cards */
        .ticket-item {
            padding: 1.15rem;
            border-radius: 0.85rem;
            border: 1px solid #e2e8f0;
            background: #ffffff;
            transition: all 0.2s ease;
            margin-bottom: 0.85rem;
        }
        .ticket-item:hover {
            border-color: #cbd5e1;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
        }

        .badge-issue-complaint {
            background-color: #fef2f2;
            color: #dc2626;
            border: 1px solid #fecaca;
            font-size: 0.65rem;
            font-weight: 700;
            padding: 0.25rem 0.5rem;
            border-radius: 0.5rem;
        }
        .badge-issue-rating {
            background-color: #fffbeb;
            color: #d97706;
            border: 1px solid #fde68a;
            font-size: 0.65rem;
            font-weight: 700;
            padding: 0.25rem 0.5rem;
            border-radius: 0.5rem;
        }

        .status-badge-pending {
            background-color: #fef3c7;
            color: #92400e;
            font-size: 0.685rem;
            font-weight: 700;
            padding: 0.25rem 0.65rem;
            border-radius: 9999px;
            display: inline-flex;
            align-items: center;
            gap: 0.25rem;
        }
        .status-badge-resolved {
            background-color: #dcfce7;
            color: #166534;
            font-size: 0.685rem;
            font-weight: 700;
            padding: 0.25rem 0.65rem;
            border-radius: 9999px;
            display: inline-flex;
            align-items: center;
            gap: 0.25rem;
        }
        .status-badge-rejected {
            background-color: #f1f5f9;
            color: #64748b;
            font-size: 0.685rem;
            font-weight: 700;
            padding: 0.25rem 0.65rem;
            border-radius: 9999px;
            display: inline-flex;
            align-items: center;
            gap: 0.25rem;
        }

        /* Admin Response Box (admin_response) */
        .admin-response-box {
            background-color: #f0fdf4;
            border: 1px solid #bbf7d0;
            border-radius: 0.65rem;
            padding: 0.75rem 0.95rem;
            font-size: 0.785rem;
            color: #166534;
            margin-top: 0.65rem;
        }

        /* Stars Mini Display */
        .stars-mini {
            color: #f59e0b;
            font-size: 0.95rem;
            display: inline-flex;
            align-items: center;
            gap: 1px;
        }

        /* Golden Rules Card */
        .golden-rules-card {
            background: #1c1917;
            border-radius: 1.25rem;
            padding: 1.75rem;
            color: #ffffff;
            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15);
        }
        .rule-check-icon {
            color: #f59e0b;
            font-size: 1.15rem;
            flex-shrink: 0;
            margin-top: 1px;
        }
    </style>
</head>

<body>

    <!-- Header dùng chung -->
    <jsp:include page="/components/header.jsp">
        <jsp:param name="activePage" value="feedback" />
    </jsp:include>

    <div class="container py-4">
        <!-- Breadcrumb -->
        <div class="feedback-breadcrumb d-flex align-items-center mb-2">
            <a href="${pageContext.request.contextPath}/">Trang chủ</a>
            <span class="material-symbols-outlined separator" style="font-size: 14px;">chevron_right</span>
            <span class="current">Phản ánh & Đánh giá dịch vụ</span>
        </div>

        <!-- Hero Care Banner -->
        <div class="hero-care-banner">
            <div class="d-flex align-items-center gap-3">
                <div class="hero-agent-icon">
                    <span class="material-symbols-outlined" style="font-size: 2rem;">rate_review</span>
                </div>
                <div>
                    <h1 class="h4 fw-bold mb-1 text-white">Aura Customer Care & Reviews</h1>
                    <p class="text-white-50 mb-0 small" style="max-width: 680px;">
                        Lắng nghe mọi phản hồi để hoàn thiện hương vị cà phê và chất lượng phục vụ. Cam kết hỗ trợ nhanh chóng và minh bạch.
                    </p>
                </div>
            </div>
        </div>

        <!-- Bố cục chính 2 Cột -->
        <div class="row g-4">
            <!-- Cột trái: Form gửi (7 cols) - Chỉ chứa các trường có trong DB: issue_type, order_id, rating, content -->
            <div class="col-lg-7">
                <div class="feedback-card">
                    <div class="border-bottom pb-3 mb-4">
                        <h2 class="feedback-form-title mb-1">
                            <span class="material-symbols-outlined text-danger">rate_review</span>
                            <span id="formTitleText">Gửi khiếu nại sự cố</span>
                        </h2>
                        <p class="text-muted small mb-0">
                            Thông tin phản ánh sẽ được lưu trực tiếp vào hệ thống để đội ngũ Aura xử lý.
                        </p>
                    </div>

                    <form id="feedbackForm" action="${pageContext.request.contextPath}/feedback" method="POST">
                        <!-- Trường DB: issue_type ENUM('RATING', 'COMPLAINT') -->
                        <input type="hidden" name="issueType" id="selectedIssueType" value="COMPLAINT">
                        <!-- Trường DB: rating INT NULL (chỉ có khi RATING) -->
                        <input type="hidden" name="rating" id="selectedRating" value="">

                        <!-- 1. Loại phản hồi (issue_type) -->
                        <div class="mb-4">
                            <label class="form-label-custom">Loại phản hồi (issue_type)</label>
                            <div class="row g-2">
                                <div class="col-6">
                                    <button type="button" id="btnTypeComplaint" class="w-100 type-toggle-btn btn-complaint active" onclick="setIssueType('COMPLAINT')">
                                        <span class="material-symbols-outlined fs-5">report_problem</span>
                                        Khiếu nại (COMPLAINT)
                                    </button>
                                </div>
                                <div class="col-6">
                                    <button type="button" id="btnTypeRating" class="w-100 type-toggle-btn btn-rating" onclick="setIssueType('RATING')">
                                        <span class="material-symbols-outlined fs-5">hotel_class</span>
                                        Đánh giá (RATING)
                                    </button>
                                </div>
                            </div>
                        </div>

                        <!-- 2. Chọn đơn hàng (order_id) -->
                        <div class="mb-4">
                            <label class="form-label-custom" for="orderCodeSelect">
                                Đơn hàng liên quan (order_id)
                            </label>
                            <select class="form-select-custom" id="orderCodeSelect" name="orderCode" required ${empty userOrders ? 'disabled' : ''}>
                                <c:choose>
                                    <c:when test="${not empty userOrders}">
                                        <c:forEach items="${userOrders}" var="uo">
                                            <option value="${uo[1]}" ${param.orderCode == uo[1] ? 'selected' : ''}>Đơn hàng #${uo[1]} (${uo[2]} - <fmt:formatNumber value="${uo[3]}" pattern="#,###" />đ)</option>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <option value="">Không còn đơn hàng chưa phản hồi</option>
                                    </c:otherwise>
                                </c:choose>
                            </select>
                            <c:if test="${empty userOrders}">
                                <p class="small text-muted mt-2 mb-0">Mỗi đơn hàng chỉ được gửi phản hồi một lần.</p>
                            </c:if>
                        </div>

                        <!-- 3. Số sao đánh giá (rating) - Chỉ hiển thị khi issue_type = 'RATING' tuân theo CHECK constraint DB -->
                        <div class="mb-4" id="starRatingContainer" style="display: none;">
                            <label class="form-label-custom">
                                Số sao đánh giá (rating: 1 - 5)
                            </label>
                            <div class="star-rating-box">
                                <c:forEach var="star" begin="1" end="5">
                                    <button type="button" class="star-btn filled" data-star="${star}" onclick="setRating(${star})" onmouseenter="previewRating(${star})" onmouseleave="resetRatingPreview()">
                                        <span class="material-symbols-outlined">star</span>
                                    </button>
                                </c:forEach>
                                <span class="star-rating-label" id="ratingText">5 sao - Tuyệt vời</span>
                            </div>
                        </div>

                        <!-- 4. Nội dung phản ánh / khiếu nại (content) -->
                        <div class="mb-4">
                            <label class="form-label-custom" for="contentInput">
                                Nội dung chi tiết (content)
                            </label>
                            <textarea class="form-control-custom" id="contentInput" name="content" rows="5" 
                                placeholder="Mô tả cụ thể sự cố bạn gặp phải với đơn hàng..." required></textarea>
                        </div>

                        <!-- Nút gửi -->
                        <div class="pt-2">
                            <button type="submit" class="btn-submit-feedback" id="submitBtn" ${empty userOrders ? 'disabled' : ''}>
                                <span class="material-symbols-outlined" style="font-size: 1.2rem;">send</span>
                                <span id="submitBtnText">Gửi khiếu nại</span>
                            </button>
                            <p class="text-muted text-center mt-2 small mb-0 d-flex align-items-center justify-content-center gap-1" style="font-size: 0.75rem;">
                                <span class="material-symbols-outlined text-success" style="font-size: 15px;">verified</span>
                                Dữ liệu được lưu trữ trực tiếp vào hệ thống quản lý Aura Café
                            </p>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Cột phải: Danh sách phản ánh & Quy chuẩn dịch vụ (5 cols) - Chỉ hiển thị các trường có trong DB -->
            <div class="col-lg-5">
                <div class="d-flex flex-column gap-4">
                    <!-- Danh sách phiếu hỗ trợ (Lấy từ bảng customer_reviews) -->
                    <div class="feedback-card">
                        <div class="d-flex align-items-center justify-content-between pb-3 mb-3 border-bottom">
                            <h3 class="h6 fw-bold mb-0 text-dark d-flex align-items-center gap-2">
                                <span class="material-symbols-outlined text-danger fs-5">receipt_long</span>
                                Lịch sử phản ánh & đánh giá (<span id="ticketCount">${empty feedbacks ? 0 : feedbacks.size()}</span>)
                            </h3>
                            <span class="badge bg-light text-secondary border fw-normal" style="font-size: 0.65rem;">customer_reviews</span>
                        </div>

                        <div id="ticketsListContainer" class="d-flex flex-column">
                            <c:choose>
                                <c:when test="${not empty feedbacks}">
                                    <c:forEach items="${feedbacks}" var="fb">
                                        <div class="ticket-item" id="ticket-item-${fb.id}">
                                            <!-- Dòng đầu: Loại (issue_type), Mã phiếu (id), Đơn hàng (order_id), Trạng thái (status) -->
                                            <div class="d-flex align-items-start justify-content-between gap-2 mb-2">
                                                <div>
                                                    <div class="d-flex align-items-center gap-2 flex-wrap">
                                                        <c:choose>
                                                            <c:when test="${fb.issueType == 'RATING'}">
                                                                <span class="badge-issue-rating">ĐÁNH GIÁ (RATING)</span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span class="badge-issue-complaint">KHIẾU NẠI (COMPLAINT)</span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                        <span class="font-monospace fw-bold small text-dark">${fb.ticketCode}</span>
                                                        <c:if test="${not empty fb.orderCode}">
                                                            <span class="text-muted small">#${fb.orderCode}</span>
                                                        </c:if>
                                                    </div>
                                                </div>

                                                <!-- Trạng thái: status -->
                                                <div>
                                                    <c:choose>
                                                        <c:when test="${fb.resolved}">
                                                            <span class="status-badge-resolved">
                                                                <span class="material-symbols-outlined" style="font-size: 13px;">check_circle</span>
                                                                Đã giải quyết
                                                            </span>
                                                        </c:when>
                                                        <c:when test="${fb.rejected}">
                                                            <span class="status-badge-rejected">
                                                                <span class="material-symbols-outlined" style="font-size: 13px;">cancel</span>
                                                                Từ chối
                                                            </span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="status-badge-pending">
                                                                <span class="material-symbols-outlined" style="font-size: 13px;">schedule</span>
                                                                Đang chờ xử lý
                                                            </span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </div>
                                            </div>

                                            <!-- Hiển thị số sao nếu là RATING (rating) -->
                                            <c:if test="${fb.issueType == 'RATING' && fb.rating > 0}">
                                                <div class="stars-mini mb-2">
                                                    <c:forEach var="s" begin="1" end="${fb.rating}">
                                                        <span class="material-symbols-outlined" style="font-size: 16px; font-variation-settings: 'FILL' 1;">star</span>
                                                    </c:forEach>
                                                    <span class="ms-1 small text-muted fw-semibold" style="font-size: 0.725rem;">(${fb.rating}/5 sao)</span>
                                                </div>
                                            </c:if>

                                            <!-- Nội dung phản hồi: content -->
                                            <p class="text-secondary small mb-2" style="font-size: 0.825rem; line-height: 1.5; white-space: pre-line;">${fb.content}</p>

                                            <!-- Phản hồi từ quản trị viên: admin_response (nếu có) -->
                                            <c:if test="${not empty fb.adminResponse}">
                                                <div class="admin-response-box">
                                                    <div class="d-flex align-items-center gap-1 fw-bold mb-1">
                                                        <span class="material-symbols-outlined" style="font-size: 15px;">reply</span>
                                                        Phản hồi từ Quản trị viên:
                                                    </div>
                                                    <p class="mb-0" style="line-height: 1.45;">${fb.adminResponse}</p>
                                                </div>
                                            </c:if>

                                            <!-- Thời gian tạo: created_at -->
                                            <div class="d-flex align-items-center justify-content-between pt-2 mt-2 border-top text-muted" style="font-size: 0.72rem;">
                                                <span>Thời gian: ${fb.formattedCreatedAt}</span>
                                                <c:if test="${not empty fb.customerName}">
                                                    <span class="text-secondary">${fb.customerName}</span>
                                                </c:if>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <div class="text-center py-4 text-muted small">
                                        <span class="material-symbols-outlined fs-2 text-secondary mb-1">inbox</span>
                                        <p class="mb-0">Chưa có phản hồi nào trong hệ thống.</p>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <!-- Thẻ Cam kết chất lượng dịch vụ Aura -->
                    <div class="golden-rules-card">
                        <h3 class="h6 fw-bold text-warning d-flex align-items-center gap-2 mb-3">
                            <span class="material-symbols-outlined" style="font-size: 1.25rem;">verified</span>
                            Quy chuẩn tiếp nhận & xử lý
                        </h3>
                        <ul class="list-unstyled d-flex flex-column gap-3 mb-0 small text-light" style="font-size: 0.8rem; line-height: 1.5;">
                            <li class="d-flex align-items-start gap-2">
                                <span class="material-symbols-outlined rule-check-icon">check</span>
                                <span>Tiếp nhận 100% phản ánh từ các đơn hàng đã đặt trong tài khoản của quý khách.</span>
                            </li>
                            <li class="d-flex align-items-start gap-2">
                                <span class="material-symbols-outlined rule-check-icon">check</span>
                                <span>Ban quản trị kiểm tra và gửi phản hồi giải quyết (admin_response) trực tiếp trên từng phiếu.</span>
                            </li>
                            <li class="d-flex align-items-start gap-2">
                                <span class="material-symbols-outlined rule-check-icon">check</span>
                                <span>Bảo vệ quyền lợi khách hàng với chính sách đổi mới hoặc hoàn tiền thỏa đáng.</span>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Toast Notification -->
    <div class="toast-container position-fixed bottom-0 start-50 translate-middle-x p-3" style="z-index: 2000;">
        <div id="feedbackToast" class="toast align-items-center text-white bg-dark border-0 shadow-lg rounded-pill" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex px-3 py-2 align-items-center">
                <span class="material-symbols-outlined text-success me-2 fs-5">check_circle</span>
                <div class="toast-body p-0 fw-medium small" id="feedbackToastMessage">
                    Đã gửi thành công!
                </div>
                <button type="button" class="btn-close btn-close-white ms-auto me-1" data-bs-dismiss="toast"></button>
            </div>
        </div>
    </div>

    <!-- Auth Modal -->
    <jsp:include page="/components/auth_modal.jsp" />

    <!-- Bootstrap 5 JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        var currentRating = 5;
        var ratingLabels = {
            1: '1 sao - Rất không hài lòng',
            2: '2 sao - Không hài lòng',
            3: '3 sao - Bình thường / Cần cải thiện',
            4: '4 sao - Hài lòng',
            5: '5 sao - Tuyệt vời'
        };

        // Chuyển đổi issue_type: COMPLAINT vs RATING
        function setIssueType(type) {
            document.getElementById('selectedIssueType').value = type;
            var btnC = document.getElementById('btnTypeComplaint');
            var btnR = document.getElementById('btnTypeRating');
            var starBox = document.getElementById('starRatingContainer');
            var formTitle = document.getElementById('formTitleText');
            var submitText = document.getElementById('submitBtnText');
            var contentInput = document.getElementById('contentInput');

            if (type === 'RATING') {
                btnR.classList.add('active');
                btnC.classList.remove('active');
                starBox.style.display = 'block';
                formTitle.innerText = 'Gửi đánh giá dịch vụ';
                submitText.innerText = 'Gửi đánh giá';
                contentInput.placeholder = 'Chia sẻ cảm nhận của bạn về hương vị đồ uống, thời gian giao hàng...';
                setRating(5);
            } else {
                btnC.classList.add('active');
                btnR.classList.remove('active');
                starBox.style.display = 'none';
                formTitle.innerText = 'Gửi khiếu nại sự cố';
                submitText.innerText = 'Gửi khiếu nại';
                contentInput.placeholder = 'Mô tả cụ thể sự cố bạn gặp phải với đơn hàng...';
                document.getElementById('selectedRating').value = '';
            }
        }

        // Star Rating Logic
        function setRating(val) {
            currentRating = val;
            document.getElementById('selectedRating').value = val;
            renderStars(val);
            document.getElementById('ratingText').innerText = ratingLabels[val] || (val + ' sao');
        }

        function previewRating(val) {
            renderStars(val);
            document.getElementById('ratingText').innerText = ratingLabels[val] || (val + ' sao');
        }

        function resetRatingPreview() {
            renderStars(currentRating);
            document.getElementById('ratingText').innerText = ratingLabels[currentRating] || (currentRating + ' sao');
        }

        function renderStars(val) {
            var buttons = document.querySelectorAll('.star-btn');
            buttons.forEach(function(btn) {
                var s = parseInt(btn.getAttribute('data-star'));
                if (s <= val) {
                    btn.classList.add('filled');
                } else {
                    btn.classList.remove('filled');
                }
            });
        }

        // Toast Trigger
        function showToast(msg) {
            var toastEl = document.getElementById('feedbackToast');
            document.getElementById('feedbackToastMessage').innerText = msg;
            var toast = new bootstrap.Toast(toastEl, { delay: 4000 });
            toast.show();
        }

        // AJAX Form Submission
        document.getElementById('feedbackForm').addEventListener('submit', function(e) {
            e.preventDefault();
            var content = document.getElementById('contentInput').value.trim();
            if (!content) {
                showToast('Vui lòng nhập nội dung chi tiết!');
                return;
            }

            var submitBtn = document.getElementById('submitBtn');
            var submitText = document.getElementById('submitBtnText');
            var originalText = submitText.innerText;
            submitBtn.disabled = true;
            submitText.innerText = 'Đang gửi...';

            var formData = new FormData(this);
            formData.append('ajax', '1');

            var params = new URLSearchParams(formData).toString();

            fetch('${pageContext.request.contextPath}/feedback', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: params
            })
            .then(function(res) {
                return res.json().then(function(data) {
                    if (!res.ok || !data.success) {
                        throw new Error(data.message || 'Không thể lưu phản hồi.');
                    }
                    return data;
                });
            })
            .then(function(data) {
                // Tải lại từ server để dropdown và lịch sử luôn phản ánh dữ liệu trong DB.
                window.location.assign(data.redirectUrl || '${pageContext.request.contextPath}/feedback?success=1');
            })
            .catch(function(err) {
                submitBtn.disabled = false;
                submitText.innerText = originalText;
                showToast(err.message || 'Có lỗi kết nối, vui lòng thử lại!');
            });
        });

        <c:if test="${param.success == '1'}">
        showToast('Đã lưu phản hồi. Đơn hàng đã được loại khỏi danh sách!');
        </c:if>
    </script>
</body>

</html>
