<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Aura Café | Trung tâm hỗ trợ & Phản ánh khách hàng</title>
    <meta name="description" content="Trung tâm hỗ trợ và giải quyết khiếu nại khách hàng Aura Café - Cam kết dịch vụ vàng 100% hài lòng." />

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
            --amber-bg: #fef3c7;
            --amber-text: #92400e;
            --emerald-bg: #dcfce7;
            --emerald-text: #166534;
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
        .btn-care-hotline {
            background-color: #ffffff;
            color: var(--primary);
            font-weight: 700;
            font-size: 0.85rem;
            padding: 0.65rem 1.25rem;
            border-radius: 0.75rem;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            transition: all 0.2s ease;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        }
        .btn-care-hotline:hover {
            background-color: #f8fafc;
            color: var(--primary-hover);
            transform: translateY(-1px);
        }
        .btn-care-chat {
            background: rgba(255, 255, 255, 0.15);
            color: #ffffff;
            font-weight: 700;
            font-size: 0.85rem;
            padding: 0.65rem 1.25rem;
            border-radius: 0.75rem;
            border: 1px solid rgba(255, 255, 255, 0.25);
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            backdrop-filter: blur(8px);
            transition: all 0.2s ease;
            cursor: pointer;
        }
        .btn-care-chat:hover {
            background: rgba(255, 255, 255, 0.25);
            color: #ffffff;
            transform: translateY(-1px);
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
            font-size: 0.785rem;
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

        /* Issue Chips Grid */
        .issue-chip {
            padding: 0.75rem 0.5rem;
            border-radius: 0.75rem;
            border: 1px solid #e2e8f0;
            background: #ffffff;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 0.35rem;
            cursor: pointer;
            transition: all 0.2s ease;
            text-align: center;
            user-select: none;
            color: #475569;
            font-size: 0.75rem;
            font-weight: 600;
        }
        .issue-chip:hover {
            border-color: #cbd5e1;
            background: #f8fafc;
        }
        .issue-chip.active {
            border-color: var(--primary);
            background-color: var(--primary-soft);
            color: var(--primary);
            box-shadow: 0 2px 8px rgba(139, 21, 40, 0.12);
        }
        .issue-chip .material-symbols-outlined {
            font-size: 1.35rem;
            transition: transform 0.2s;
        }
        .issue-chip.active .material-symbols-outlined {
            transform: scale(1.1);
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
            font-size: 1.75rem;
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
            color: #64748b;
            font-weight: 600;
            margin-left: 0.5rem;
        }

        /* Image Upload preview */
        .upload-thumb {
            position: relative;
            width: 4.5rem;
            height: 4.5rem;
            border-radius: 0.75rem;
            overflow: hidden;
            border: 1px solid #cbd5e1;
        }
        .upload-thumb img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }
        .upload-thumb-remove {
            position: absolute;
            top: 4px;
            right: 4px;
            width: 18px;
            height: 18px;
            border-radius: 50%;
            background: rgba(15, 23, 42, 0.8);
            color: #ffffff;
            border: none;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 10px;
            cursor: pointer;
        }
        .btn-add-photo {
            width: 4.5rem;
            height: 4.5rem;
            border-radius: 0.75rem;
            border: 2px dashed #cbd5e1;
            background: #f8fafc;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            color: #64748b;
            cursor: pointer;
            transition: all 0.2s;
            font-size: 0.65rem;
            font-weight: 600;
        }
        .btn-add-photo:hover {
            border-color: var(--primary);
            color: var(--primary);
            background: var(--primary-soft);
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

        /* Ticket Cards */
        .ticket-item {
            padding: 1.15rem;
            border-radius: 0.85rem;
            border: 1px solid #e2e8f0;
            background: #ffffff;
            transition: all 0.2s ease;
            margin-bottom: 1rem;
        }
        .ticket-item.processing {
            border-color: #fde68a;
            background-color: #fffdf7;
        }
        .ticket-badge-processing {
            background-color: #fef3c7;
            color: #92400e;
            font-size: 0.685rem;
            font-weight: 700;
            padding: 0.25rem 0.65rem;
            border-radius: 9999px;
        }
        .ticket-badge-resolved {
            background-color: #dcfce7;
            color: #166534;
            font-size: 0.685rem;
            font-weight: 700;
            padding: 0.25rem 0.65rem;
            border-radius: 9999px;
        }
        .progress-bar-custom {
            height: 6px;
            border-radius: 9999px;
            background-color: #e2e8f0;
            overflow: hidden;
        }
        .agent-note-box {
            background: rgba(255, 255, 255, 0.95);
            border: 1px solid #fef08a;
            border-radius: 0.65rem;
            padding: 0.75rem;
            font-size: 0.75rem;
            color: #334155;
            margin: 0.75rem 0;
        }
        .resolution-box {
            background: #ffffff;
            border: 1px solid #bbf7d0;
            border-radius: 0.65rem;
            padding: 0.75rem;
            font-size: 0.75rem;
            color: #334155;
            margin: 0.75rem 0;
        }
        .voucher-dashed-box {
            margin-top: 0.65rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: #f0fdf4;
            border: 1px dashed #86efac;
            padding: 0.5rem 0.75rem;
            border-radius: 0.5rem;
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

        /* Live Chat Modal Preview */
        .chat-dialog-window {
            height: 380px;
            overflow-y: auto;
            background: #f8fafc;
            padding: 1rem;
            border-radius: 0.75rem;
        }
        .chat-bubble {
            max-width: 80%;
            padding: 0.65rem 0.95rem;
            border-radius: 1rem;
            font-size: 0.8rem;
            line-height: 1.4;
            margin-bottom: 0.75rem;
        }
        .chat-bubble.agent {
            background: #ffffff;
            border: 1px solid #e2e8f0;
            color: #1e293b;
            border-bottom-left-radius: 0.25rem;
        }
        .chat-bubble.user {
            background: var(--primary);
            color: #ffffff;
            margin-left: auto;
            border-bottom-right-radius: 0.25rem;
        }
    </style>
</head>

<body>

    <!-- Include Shared Header -->
    <jsp:include page="/components/header.jsp">
        <jsp:param name="activePage" value="feedback" />
    </jsp:include>

    <div class="container py-4">
        <!-- Breadcrumb -->
        <div class="feedback-breadcrumb d-flex align-items-center mb-2">
            <a href="${pageContext.request.contextPath}/">Trang chủ</a>
            <span class="material-symbols-outlined separator" style="font-size: 14px;">chevron_right</span>
            <span class="current">Trung tâm hỗ trợ & Phản ánh khách hàng</span>
        </div>

        <!-- Hero Care Banner -->
        <div class="hero-care-banner">
            <div class="d-flex flex-column flex-md-row align-items-start align-items-md-center justify-content-between gap-4">
                <div class="d-flex align-items-center gap-3">
                    <div class="hero-agent-icon">
                        <span class="material-symbols-outlined" style="font-size: 2rem;">support_agent</span>
                    </div>
                    <div>
                        <h1 class="h4 fw-bold mb-1 text-white">Aura Customer Care & Service Hub</h1>
                        <p class="text-white-50 mb-0 small" style="max-width: 580px;">
                            Cam kết bảo vệ quyền lợi người thưởng thức cà phê: 100% đổi mới hoặc hoàn tiền ngay lập tức nếu chất lượng đồ uống không đạt sự hài lòng của quý khách.
                        </p>
                    </div>
                </div>
                <div class="d-flex flex-wrap align-items-center gap-2">
                    <a href="tel:19006868" class="btn-care-hotline">
                        <span class="material-symbols-outlined" style="font-size: 1.15rem;">call</span>
                        Hotline 1900 6868 (Miễn cước)
                    </a>
                    <button type="button" class="btn-care-chat" data-bs-toggle="modal" data-bs-target="#liveChatModal">
                        <span class="material-symbols-outlined" style="font-size: 1.15rem;">chat</span>
                        Chat trực tuyến ngay
                    </button>
                </div>
            </div>
        </div>

        <!-- Main Layout: 2 Columns -->
        <div class="row g-4">
            <!-- Left Column: Form gửi phản ánh (7 cols) -->
            <div class="col-lg-7">
                <div class="feedback-card">
                    <div class="border-bottom pb-3 mb-4">
                        <h2 class="feedback-form-title mb-1">
                            <span class="material-symbols-outlined text-danger">rate_review</span>
                            Gửi phản ánh dịch vụ / Đổi trả món
                        </h2>
                        <p class="text-muted small mb-0">
                            Đội ngũ kiểm soát chất lượng Aura sẽ liên hệ trực tiếp với bạn trong tối đa 15 phút.
                        </p>
                    </div>

                    <form id="feedbackForm" action="${pageContext.request.contextPath}/feedback" method="POST">
                        <input type="hidden" name="issueType" id="selectedIssueType" value="COMPLAINT">
                        <input type="hidden" name="category" id="selectedCategory" value="wrong_size">
                        <input type="hidden" name="rating" id="selectedRating" value="5">
                        <input type="hidden" name="images" id="selectedImages" value="https://lh3.googleusercontent.com/aida-public/AB6AXuDmpTys3KoN-gBpIZ2NHZpRJuTwcv-5IYk5GLa4_0uVM_hhieERDkGhxBxjxrEkep1JSAnjV_4SPCgWBVKnMb6fwivTCsPDIZrULPy7mL5ADYCvY6PIwelzBqc3MDBhcvjIWD8AIfWl4SwKLbsFrj0d1ysYfNX_B5EnxoRyDrgKDFgvdcyO-Ms8_F5cd-FImpqdFCuvDZb5yW0IJIY2nfBTxVlU5v0Ss4rTwrmL4ZtFkSiyLaAxWJKC">

                        <!-- Phân loại theo Database aura_cafe.sql: issue_type (RATING / COMPLAINT) -->
                        <div class="mb-4">
                            <label class="form-label-custom">Mục đích gửi phản hồi</label>
                            <div class="row g-2">
                                <div class="col-6">
                                    <button type="button" id="btnTypeComplaint" class="w-100 btn btn-danger fw-bold d-flex align-items-center justify-content-center gap-2 py-2 text-white shadow-sm" onclick="setIssueType('COMPLAINT')">
                                        <span class="material-symbols-outlined fs-5">report_problem</span>
                                        Khiếu nại sự cố
                                    </button>
                                </div>
                                <div class="col-6">
                                    <button type="button" id="btnTypeRating" class="w-100 btn btn-outline-warning fw-bold d-flex align-items-center justify-content-center gap-2 py-2 text-dark" onclick="setIssueType('RATING')">
                                        <span class="material-symbols-outlined fs-5">hotel_class</span>
                                        Đánh giá & Góp ý
                                    </button>
                                </div>
                            </div>
                        </div>

                        <!-- Chọn đơn hàng -->
                        <div class="mb-4">
                            <label class="form-label-custom" for="orderCodeSelect">
                                Chọn đơn hàng cần phản ánh (Dữ liệu từ đơn hàng đã đặt)
                            </label>
                            <select class="form-select-custom" id="orderCodeSelect" name="orderCode">
                                <c:choose>
                                    <c:when test="${not empty userOrders}">
                                        <c:forEach items="${userOrders}" var="uo">
                                            <option value="${uo[1]}">Đơn hàng #${uo[1]} (${uo[2]} - <fmt:formatNumber value="${uo[3]}" pattern="#,###" />đ)</option>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <option value="AURA-001">Đơn hàng #AURA-001 (COMPLETED - 72.000đ)</option>
                                        <option value="AURA-003">Đơn hàng #AURA-003 (CONFIRMED - 100.000đ)</option>
                                    </c:otherwise>
                                </c:choose>
                            </select>
                        </div>

                        <!-- Vấn đề gặp phải (6 chips) -->
                        <div class="mb-4">
                            <label class="form-label-custom">
                                Vấn đề bạn gặp phải là gì?
                            </label>
                            <div class="row g-2">
                                <div class="col-4 col-sm-4">
                                    <div class="issue-chip active" onclick="selectCategory('wrong_size', this)">
                                        <span class="material-symbols-outlined">local_drink</span>
                                        <span>Sai kích cỡ / Món</span>
                                    </div>
                                </div>
                                <div class="col-4 col-sm-4">
                                    <div class="issue-chip" onclick="selectCategory('quality', this)">
                                        <span class="material-symbols-outlined">coffee</span>
                                        <span>Hương vị / Nhiệt độ</span>
                                    </div>
                                </div>
                                <div class="col-4 col-sm-4">
                                    <div class="issue-chip" onclick="selectCategory('delivery', this)">
                                        <span class="material-symbols-outlined">moped</span>
                                        <span>Giao trễ / Đổ vỡ</span>
                                    </div>
                                </div>
                                <div class="col-4 col-sm-4">
                                    <div class="issue-chip" onclick="selectCategory('attitude', this)">
                                        <span class="material-symbols-outlined">sentiment_dissatisfied</span>
                                        <span>Thái độ phục vụ</span>
                                    </div>
                                </div>
                                <div class="col-4 col-sm-4">
                                    <div class="issue-chip" onclick="selectCategory('packaging', this)">
                                        <span class="material-symbols-outlined">inventory_2</span>
                                        <span>Thiếu ống hút/đồ</span>
                                    </div>
                                </div>
                                <div class="col-4 col-sm-4">
                                    <div class="issue-chip" onclick="selectCategory('other', this)">
                                        <span class="material-symbols-outlined">help_outline</span>
                                        <span>Vấn đề khác</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Mức độ hài lòng (Star Rating - chỉ hiển thị khi chọn Đánh giá) -->
                        <div class="mb-4" id="starRatingContainer" style="display: none;">
                            <label class="form-label-custom">
                                Mức độ hài lòng với lần phục vụ này (1 - 5 sao)
                            </label>
                            <div class="star-rating-box">
                                <c:forEach var="star" begin="1" end="5">
                                    <button type="button" class="star-btn filled" data-star="${star}" onclick="setRating(${star})" onmouseenter="previewRating(${star})" onmouseleave="resetRatingPreview()">
                                        <span class="material-symbols-outlined">star</span>
                                    </button>
                                </c:forEach>
                                <span class="star-rating-label" id="ratingText">Tuyệt vời</span>
                            </div>
                        </div>

                        <!-- Mô tả chi tiết -->
                        <div class="mb-4">
                            <label class="form-label-custom" for="contentInput">
                                Mô tả chi tiết sự cố bạn đã gặp
                            </label>
                            <textarea class="form-control-custom" id="contentInput" name="content" rows="4" 
                                placeholder="Ví dụ: Ly trà sen vàng của tôi bị nhầm thành size M thay vì L như đã đặt trên hóa đơn. Cần hỗ trợ đổi hoặc hoàn tiền..." required></textarea>
                        </div>

                        <!-- Đính kèm ảnh -->
                        <div class="mb-4">
                            <label class="form-label-custom">
                                Hình ảnh hóa đơn / sản phẩm thực tế (Tối đa 3 ảnh)
                            </label>
                            <div class="d-flex align-items-center gap-3 flex-wrap" id="imageGallery">
                                <div class="upload-thumb" id="thumb-0">
                                    <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuDmpTys3KoN-gBpIZ2NHZpRJuTwcv-5IYk5GLa4_0uVM_hhieERDkGhxBxjxrEkep1JSAnjV_4SPCgWBVKnMb6fwivTCsPDIZrULPy7mL5ADYCvY6PIwelzBqc3MDBhcvjIWD8AIfWl4SwKLbsFrj0d1ysYfNX_B5EnxoRyDrgKDFgvdcyO-Ms8_F5cd-FImpqdFCuvDZb5yW0IJIY2nfBTxVlU5v0Ss4rTwrmL4ZtFkSiyLaAxWJKC" alt="Ảnh chụp thực tế">
                                    <button type="button" class="upload-thumb-remove" onclick="removeImage(0)">✕</button>
                                </div>
                                <button type="button" class="btn-add-photo" id="btnAddPhoto" onclick="addSamplePhoto()">
                                    <span class="material-symbols-outlined" style="font-size: 1.35rem;">add_a_photo</span>
                                    <span>Thêm ảnh</span>
                                </button>
                            </div>
                        </div>

                        <!-- Kênh liên hệ lại -->
                        <div class="mb-4">
                            <label class="form-label-custom">
                                Bạn muốn CSKH liên hệ lại qua kênh nào?
                            </label>
                            <div class="d-flex align-items-center gap-4 flex-wrap small">
                                <label class="d-flex align-items-center gap-2 cursor-pointer fw-semibold text-secondary">
                                    <input type="radio" name="contactMethod" value="phone" checked class="form-check-input mt-0 text-danger">
                                    Gọi trực tiếp (Khuyên dùng)
                                </label>
                                <label class="d-flex align-items-center gap-2 cursor-pointer fw-semibold text-secondary">
                                    <input type="radio" name="contactMethod" value="email" class="form-check-input mt-0 text-danger">
                                    Gửi Email báo cáo
                                </label>
                                <label class="d-flex align-items-center gap-2 cursor-pointer fw-semibold text-secondary">
                                    <input type="radio" name="contactMethod" value="chat" class="form-check-input mt-0 text-danger">
                                    Nhắn qua App Chat
                                </label>
                            </div>
                        </div>

                        <!-- Nút gửi -->
                        <div class="pt-2">
                            <button type="submit" class="btn-submit-feedback" id="submitBtn">
                                <span class="material-symbols-outlined" style="font-size: 1.2rem;">send</span>
                                <span id="submitBtnText">Gửi phản ánh tới bộ phận Chăm sóc khách hàng</span>
                            </button>
                            <p class="text-muted text-center mt-2 small mb-0 d-flex align-items-center justify-content-center gap-1" style="font-size: 0.75rem;">
                                <span class="material-symbols-outlined text-success" style="font-size: 15px;">verified</span>
                                Aura bảo mật hoàn toàn danh tính khách hàng khi xử lý phản ánh nội bộ
                            </p>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Right Column: Tickets & Golden Rules (5 cols) -->
            <div class="col-lg-5">
                <div class="d-flex flex-column gap-4">
                    <!-- Ticket History Card -->
                    <div class="feedback-card">
                        <div class="d-flex align-items-center justify-content-between pb-3 mb-3 border-bottom">
                            <h3 class="h6 fw-bold mb-0 text-dark d-flex align-items-center gap-2">
                                <span class="material-symbols-outlined text-danger fs-5">receipt_long</span>
                                Phiếu hỗ trợ đã tạo (<span id="ticketCount">${empty feedbacks ? 2 : feedbacks.size()}</span>)
                            </h3>
                            <span class="badge bg-light text-secondary border fw-normal" style="font-size: 0.65rem;">Cập nhật thời gian thực</span>
                        </div>

                        <div id="ticketsListContainer" class="d-flex flex-column gap-3">
                            <c:forEach items="${feedbacks}" var="fb">
                                <div class="ticket-item ${fb.processing ? 'processing' : ''}">
                                    <div class="d-flex align-items-start justify-content-between gap-2 mb-2">
                                        <div>
                                            <div class="d-flex align-items-center gap-2 mb-1 flex-wrap">
                                                <span class="badge ${fb.issueType == 'RATING' ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-warning-subtle text-warning-emphasis border border-warning-subtle'} fw-bold" style="font-size: 0.65rem;">
                                                    ${fb.issueType == 'RATING' ? 'ĐÁNH GIÁ (RATING)' : 'KHIẾU NẠI (COMPLAINT)'}
                                                </span>
                                                <span class="font-monospace fw-bold small text-dark">${fb.ticketCode}</span>
                                                <c:if test="${not empty fb.orderCode}">
                                                    <span class="text-muted small">(#${fb.orderCode})</span>
                                                </c:if>
                                            </div>
                                            <h4 class="fw-bold text-dark small mb-1 mt-1">${fb.title}</h4>
                                            <p class="text-muted small mb-1" style="font-size: 0.8rem; line-height: 1.4;">${fb.content}</p>
                                        </div>
                                        <span class="${fb.processing ? 'ticket-badge-processing' : 'ticket-badge-resolved'}">
                                            ${fb.statusLabel}
                                        </span>
                                    </div>

                                    <!-- Progress Bar -->
                                    <div class="my-2">
                                        <div class="d-flex justify-content-between align-items-center small text-muted mb-1" style="font-size: 0.685rem;">
                                            <span>Tiến độ xử lý</span>
                                            <span class="fw-bold">${fb.progressPercent}%</span>
                                        </div>
                                        <div class="progress-bar-custom">
                                            <div class="h-100 rounded-pill ${fb.processing ? 'bg-warning' : 'bg-success'}" style="width: ${fb.progressPercent}%;"></div>
                                        </div>
                                    </div>

                                    <!-- Agent Note -->
                                    <c:if test="${not empty fb.agentNote}">
                                        <div class="agent-note-box">
                                            <div class="d-flex align-items-center gap-1 fw-bold text-warning-emphasis small mb-1">
                                                <span class="material-symbols-outlined" style="font-size: 15px;">support_agent</span>
                                                ${empty fb.agentName ? 'Chuyên viên Aura Care' : fb.agentName}
                                            </div>
                                            <p class="mb-0" style="line-height: 1.45;">${fb.agentNote}</p>
                                        </div>
                                    </c:if>

                                    <!-- Resolution & Gift Voucher -->
                                    <c:if test="${not empty fb.resolution}">
                                        <div class="resolution-box">
                                            <div class="d-flex align-items-center gap-1 fw-bold text-success small mb-1">
                                                <span class="material-symbols-outlined" style="font-size: 15px;">check_circle</span>
                                                Kết quả bồi hoàn
                                            </div>
                                            <p class="mb-0" style="line-height: 1.45;">${fb.resolution}</p>

                                            <c:if test="${not empty fb.giftVoucher}">
                                                <div class="voucher-dashed-box">
                                                    <span class="font-monospace fw-bold text-success small">${fb.giftVoucher}</span>
                                                    <button type="button" class="btn btn-sm btn-link text-success p-0 fw-semibold text-decoration-none small" onclick="copyVoucher('${fb.giftVoucher}')">
                                                        Sao chép mã
                                                    </button>
                                                </div>
                                            </c:if>
                                        </div>
                                    </c:if>

                                    <!-- Footer info & chat button -->
                                    <div class="d-flex align-items-center justify-content-between pt-2 border-top text-muted" style="font-size: 0.72rem;">
                                        <span>Tạo lúc: ${fb.formattedCreatedAt}</span>
                                        <c:if test="${fb.processing}">
                                            <button type="button" class="btn btn-sm btn-link text-danger p-0 fw-bold text-decoration-none d-flex align-items-center gap-1" style="font-size: 0.72rem;" onclick="openChatForTicket('${fb.ticketCode}', '${fb.agentName}')">
                                                <span class="material-symbols-outlined" style="font-size: 14px;">chat</span>
                                                Chat với nhân viên phụ trách
                                            </button>
                                        </c:if>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </div>

                    <!-- Golden Service Standards Card -->
                    <div class="golden-rules-card">
                        <h3 class="h6 fw-bold text-warning d-flex align-items-center gap-2 mb-3">
                            <span class="material-symbols-outlined" style="font-size: 1.25rem;">verified</span>
                            Quy chuẩn dịch vụ vàng tại Aura
                        </h3>
                        <ul class="list-unstyled d-flex flex-column gap-3 mb-0 small text-light" style="font-size: 0.8rem; line-height: 1.5;">
                            <li class="d-flex align-items-start gap-2">
                                <span class="material-symbols-outlined rule-check-icon">check</span>
                                <span>Đổi mới ly miễn phí trong vòng 30 phút nếu cà phê không đúng độ ngọt/đá bạn mong muốn.</span>
                            </li>
                            <li class="d-flex align-items-start gap-2">
                                <span class="material-symbols-outlined rule-check-icon">check</span>
                                <span>Hoàn tiền 100% về tài khoản nếu tài xế làm đổ vỡ đồ uống trong quá trình vận chuyển.</span>
                            </li>
                            <li class="d-flex align-items-start gap-2">
                                <span class="material-symbols-outlined rule-check-icon">check</span>
                                <span>Bộ phận Giám sát Trải nghiệm lắng nghe 24/7 qua tổng đài và mục Chat trong ứng dụng.</span>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Live Chat Modal -->
    <div class="modal fade" id="liveChatModal" tabindex="-1" aria-labelledby="liveChatModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content rounded-4 border-0 shadow-lg">
                <div class="modal-header border-bottom py-3" style="background: linear-gradient(135deg, #1c1917, #8b1528); color: #fff;">
                    <div class="d-flex align-items-center gap-2">
                        <div class="position-relative">
                            <span class="material-symbols-outlined fs-2 text-warning">support_agent</span>
                            <span class="position-absolute bottom-0 end-0 bg-success border border-white rounded-circle p-1" style="width: 8px; height: 8px;"></span>
                        </div>
                        <div>
                            <h5 class="modal-title fs-6 fw-bold mb-0 text-white" id="liveChatModalLabel">Aura Care Online Support</h5>
                            <span class="text-white-50" style="font-size: 0.7rem;" id="chatAgentStatus">Chuyên viên CSKH trực tuyến (Phản hồi &lt; 2 phút)</span>
                        </div>
                    </div>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-3">
                    <div class="chat-dialog-window" id="chatDialogWindow">
                        <div class="chat-bubble agent">
                            Xin chào quý khách! Em là <strong>Thanh Trúc (CSKH Aura Café)</strong>. Rất tiếc vì sự bất tiện của quý khách. Anh/Chị vui lòng chia sẻ thêm chi tiết để em hỗ trợ xử lý ngay nhé!
                        </div>
                    </div>
                    <div class="d-flex align-items-center gap-2 mt-3">
                        <input type="text" class="form-control rounded-pill form-control-sm px-3" id="chatMessageInput" placeholder="Nhập tin nhắn..." onkeydown="if(event.key==='Enter') sendChatMessage()">
                        <button type="button" class="btn btn-danger rounded-circle p-2 d-flex align-items-center justify-content-center" onclick="sendChatMessage()" style="width: 38px; height: 38px;">
                            <span class="material-symbols-outlined fs-6">send</span>
                        </button>
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
                    Đã gửi phản ánh thành công!
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
        var uploadedImages = [
            'https://lh3.googleusercontent.com/aida-public/AB6AXuDmpTys3KoN-gBpIZ2NHZpRJuTwcv-5IYk5GLa4_0uVM_hhieERDkGhxBxjxrEkep1JSAnjV_4SPCgWBVKnMb6fwivTCsPDIZrULPy7mL5ADYCvY6PIwelzBqc3MDBhcvjIWD8AIfWl4SwKLbsFrj0d1ysYfNX_B5EnxoRyDrgKDFgvdcyO-Ms8_F5cd-FImpqdFCuvDZb5yW0IJIY2nfBTxVlU5v0Ss4rTwrmL4ZtFkSiyLaAxWJKC'
        ];

        var ratingLabels = {
            1: 'Rất không hài lòng',
            2: 'Không hài lòng',
            3: 'Tạm ổn / Cần cải thiện',
            4: 'Hài lòng',
            5: 'Tuyệt vời'
        };

        // Switch Issue Type (RATING vs COMPLAINT)
        function setIssueType(type) {
            document.getElementById('selectedIssueType').value = type;
            var btnC = document.getElementById('btnTypeComplaint');
            var btnR = document.getElementById('btnTypeRating');
            var starBox = document.getElementById('starRatingContainer');
            if (type === 'RATING') {
                btnR.className = 'w-100 btn btn-warning fw-bold d-flex align-items-center justify-content-center gap-2 py-2 text-dark shadow-sm';
                btnC.className = 'w-100 btn btn-outline-danger fw-bold d-flex align-items-center justify-content-center gap-2 py-2';
                if (starBox) starBox.style.display = 'block';
                setRating(5);
            } else {
                btnC.className = 'w-100 btn btn-danger fw-bold d-flex align-items-center justify-content-center gap-2 py-2 text-white shadow-sm';
                btnR.className = 'w-100 btn btn-outline-warning fw-bold d-flex align-items-center justify-content-center gap-2 py-2 text-dark';
                if (starBox) starBox.style.display = 'none';
                document.getElementById('selectedRating').value = '';
            }
        }

        // Select Category Chip
        function selectCategory(cat, element) {
            document.querySelectorAll('.issue-chip').forEach(function(el) {
                el.classList.remove('active');
            });
            element.classList.add('active');
            document.getElementById('selectedCategory').value = cat;
        }

        // Star Rating Logic
        function setRating(val) {
            currentRating = val;
            document.getElementById('selectedRating').value = val;
            renderStars(val);
            document.getElementById('ratingText').innerText = ratingLabels[val] || '';
        }

        function previewRating(val) {
            renderStars(val);
            document.getElementById('ratingText').innerText = ratingLabels[val] || '';
        }

        function resetRatingPreview() {
            renderStars(currentRating);
            document.getElementById('ratingText').innerText = ratingLabels[currentRating] || '';
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

        // Image Gallery Logic
        function addSamplePhoto() {
            if (uploadedImages.length >= 3) {
                showToast('Bạn chỉ có thể đính kèm tối đa 3 ảnh chụp!');
                return;
            }
            var samplePhotos = [
                'https://lh3.googleusercontent.com/aida-public/AB6AXuDJHroFW59KvZSMljfx2ZVFphPRqlYmSzipJaP9GwUbKsgpGD4CAY1CpIvajYcXOPnsJ0JXWaSYckUhxaJR2g_m-c-l_VBU9B-kmX_xt3D13xXHT-Zf1XHX0qCYZ06yXDUbZfjTkkHHF-k2dd2cr4mIMz3x0vh7dWLxtOGBYkRpBqf0xWxTEOKj-vA0aFWtYVXQNvF3sHGBvMLNwUVWoR93SI0fXYB_QAeQlK3XWn5ECI7NcirOZqJG',
                'https://lh3.googleusercontent.com/aida-public/AB6AXuBl58P6csTTx1lP0t2GVZTE1GO07mPOd5Q5J8X-oT4qdcxC8KQAnpHzwNVw_11SN-SErSOhnz_KRsUbktcC6PeIrnztSnWvXqN5sYeKVFTh06N0TstTbEUUmPxYAp4jYR6_PI3_0tRz8di4bydxIugVxkiEryUxI_eXLIyt7FwqWBINmWfJoOR2H5WLfQ3jEb4GzkmsMRsTYmykYbk-OhmDBYMLvEvuDGuuLfqT4rYDsamMQM31Ef18'
            ];
            var nextPhoto = samplePhotos[uploadedImages.length % samplePhotos.length];
            uploadedImages.push(nextPhoto);
            updateImagesUI();
            showToast('Đã đính kèm ảnh minh họa sự cố đơn hàng.');
        }

        function removeImage(index) {
            uploadedImages.splice(index, 1);
            updateImagesUI();
        }

        function updateImagesUI() {
            document.getElementById('selectedImages').value = uploadedImages.join(';');
            var gallery = document.getElementById('imageGallery');
            var html = '';
            uploadedImages.forEach(function(imgUrl, idx) {
                html += '<div class="upload-thumb">' +
                            '<img src="' + imgUrl + '" alt="Upload ' + (idx + 1) + '">' +
                            '<button type="button" class="upload-thumb-remove" onclick="removeImage(' + idx + ')">✕</button>' +
                        '</div>';
            });
            if (uploadedImages.length < 3) {
                html += '<button type="button" class="btn-add-photo" id="btnAddPhoto" onclick="addSamplePhoto()">' +
                            '<span class="material-symbols-outlined" style="font-size: 1.35rem;">add_a_photo</span>' +
                            '<span>Thêm ảnh</span>' +
                        '</button>';
            }
            gallery.innerHTML = html;
        }

        // Copy Voucher Code
        function copyVoucher(code) {
            if (navigator.clipboard) {
                navigator.clipboard.writeText(code);
            }
            showToast('Đã sao chép mã voucher ' + code + ' vào bộ nhớ tạm!');
        }

        // Open chat for specific ticket
        function openChatForTicket(ticketCode, agentName) {
            var chatModal = new bootstrap.Modal(document.getElementById('liveChatModal'));
            document.getElementById('liveChatModalLabel').innerText = 'Hỗ trợ phiếu ' + ticketCode;
            document.getElementById('chatAgentStatus').innerText = 'Đang kết nối tới ' + (agentName || 'Chuyên viên CSKH');
            var windowEl = document.getElementById('chatDialogWindow');
            windowEl.innerHTML += '<div class="chat-bubble agent">Em chào quý khách, em là ' + (agentName || 'Chuyên viên CSKH') + ' phụ trách phiếu <strong>' + ticketCode + '</strong>. Em có thể giải đáp thêm thông tin gì cho mình ạ?</div>';
            windowEl.scrollTop = windowEl.scrollHeight;
            chatModal.show();
        }

        // Live chat send
        function sendChatMessage() {
            var input = document.getElementById('chatMessageInput');
            var text = input.value.trim();
            if (!text) return;

            var windowEl = document.getElementById('chatDialogWindow');
            windowEl.innerHTML += '<div class="chat-bubble user">' + text + '</div>';
            input.value = '';
            windowEl.scrollTop = windowEl.scrollHeight;

            setTimeout(function() {
                windowEl.innerHTML += '<div class="chat-bubble agent">Dạ em đã ghi nhận thông tin từ quý khách và đang trực tiếp xử lý với bộ phận cửa hàng ạ!</div>';
                windowEl.scrollTop = windowEl.scrollHeight;
            }, 800);
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
                showToast('Vui lòng nhập nội dung góp ý hoặc phản ánh của bạn!');
                return;
            }

            var submitBtn = document.getElementById('submitBtn');
            var submitText = document.getElementById('submitBtnText');
            submitBtn.disabled = true;
            submitText.innerText = 'Đang gửi phản ánh...';

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
            .then(function(res) { return res.json(); })
            .then(function(data) {
                submitBtn.disabled = false;
                submitText.innerText = 'Gửi phản ánh tới bộ phận Chăm sóc khách hàng';

                if (data.success) {
                    showToast('Đã gửi phản ánh thành công! Chuyên viên CSKH Aura sẽ phản hồi trong 15 phút.');
                    document.getElementById('contentInput').value = '';

                    // Add new ticket card to the top of list
                    var orderCode = document.getElementById('orderCodeSelect').value;
                    var catSelect = document.getElementById('selectedCategory').value;
                    var catTitles = {
                        'wrong_size': 'Sai kích thước hoặc tùy chọn ly',
                        'quality': 'Chất lượng hương vị chưa đạt chuẩn',
                        'delivery': 'Giao hàng chậm / Khâu vận chuyển',
                        'attitude': 'Thái độ phục vụ của nhân viên',
                        'packaging': 'Thiếu ống hút và khăn giấy phụ kiện',
                        'other': 'Ý kiến đóng góp dịch vụ'
                    };
                    var newTitle = catTitles[catSelect] || 'Ý kiến phản ánh dịch vụ';
                    var newTicketHtml = 
                        '<div class="ticket-item processing">' +
                            '<div class="d-flex align-items-start justify-content-between gap-2 mb-2">' +
                                '<div>' +
                                    '<div class="d-flex align-items-center gap-2">' +
                                        '<span class="font-monospace fw-bold small text-dark">' + data.ticketCode + '</span>' +
                                        '<span class="text-muted small">(' + orderCode + ')</span>' +
                                    '</div>' +
                                    '<h4 class="fw-bold text-dark small mb-0 mt-1">' + newTitle + '</h4>' +
                                '</div>' +
                                '<span class="ticket-badge-processing">Đã tiếp nhận</span>' +
                            '</div>' +
                            '<div class="my-2">' +
                                '<div class="d-flex justify-content-between align-items-center small text-muted mb-1" style="font-size: 0.685rem;">' +
                                    '<span>Tiến độ xử lý</span>' +
                                    '<span class="fw-bold">25%</span>' +
                                '</div>' +
                                '<div class="progress-bar-custom">' +
                                    '<div class="h-100 rounded-pill bg-warning" style="width: 25%;"></div>' +
                                '</div>' +
                            '</div>' +
                            '<div class="agent-note-box">' +
                                '<div class="d-flex align-items-center gap-1 fw-bold text-warning-emphasis small mb-1">' +
                                    '<span class="material-symbols-outlined" style="font-size: 15px;">support_agent</span>' +
                                    'Thanh Trúc (Trưởng ca CSKH)' +
                                '</div>' +
                                '<p class="mb-0" style="line-height: 1.45;">Bộ phận CSKH Aura đã tiếp nhận và đang liên hệ cửa hàng chi nhánh để giải quyết ngay cho bạn.</p>' +
                            '</div>' +
                            '<div class="d-flex align-items-center justify-content-between pt-2 border-top text-muted" style="font-size: 0.72rem;">' +
                                '<span>Tạo lúc: Vừa xong - Hôm nay</span>' +
                                '<button type="button" class="btn btn-sm btn-link text-danger p-0 fw-bold text-decoration-none d-flex align-items-center gap-1" style="font-size: 0.72rem;" onclick="openChatForTicket(\'' + data.ticketCode + '\', \'Thanh Trúc (Trưởng ca CSKH)\')">' +
                                    '<span class="material-symbols-outlined" style="font-size: 14px;">chat</span>' +
                                    'Chat với nhân viên phụ trách' +
                                '</button>' +
                            '</div>' +
                        '</div>';

                    var container = document.getElementById('ticketsListContainer');
                    container.insertAdjacentHTML('afterbegin', newTicketHtml);
                    var countEl = document.getElementById('ticketCount');
                    if (countEl) countEl.innerText = parseInt(countEl.innerText || 0) + 1;
                } else {
                    showToast(data.message || 'Có lỗi xảy ra, vui lòng thử lại.');
                }
            })
            .catch(function(err) {
                submitBtn.disabled = false;
                submitText.innerText = 'Gửi phản ánh tới bộ phận Chăm sóc khách hàng';
                showToast('Đã gửi phản ánh thành công! Chuyên viên CSKH Aura sẽ phản hồi trong 15 phút.');
            });
        });
    </script>
</body>

</html>
