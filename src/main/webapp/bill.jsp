<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Aura Café | Hóa đơn điện tử #${order.orderCode}</title>

    <!-- Google Fonts & Icons -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet" />

    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">

    <!-- Tailwind CSS (Play CDN với cấu hình theme chuẩn, tắt preflight để không đè vỡ header Bootstrap) -->
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            corePlugins: {
                preflight: false,
            },
            theme: {
                extend: {
                    colors: {
                        primary: '#900b1a',
                        'primary-container': '#b2292e',
                        'on-primary': '#ffffff',
                        'primary-fixed': '#ffdad7',
                        secondary: '#555f71',
                        'secondary-container': '#d6e0f6',
                        'on-secondary-container': '#596376',
                        tertiary: '#6e3900',
                        'tertiary-fixed': '#ffdcc3',
                        surface: '#f8f9ff',
                        'surface-bright': '#f8f9ff',
                        'surface-container-lowest': '#ffffff',
                        'surface-container-low': '#eff4ff',
                        'surface-container': '#e6eeff',
                        'surface-container-high': '#dee9fc',
                        'surface-container-highest': '#d9e3f6',
                        'on-surface': '#121c2a',
                        'on-surface-variant': '#59413f',
                    },
                    spacing: {
                        'gutter': '1rem',
                        'gutter-lg': '1.5rem',
                        'margin': '1rem',
                        'margin-lg': '2rem',
                        'space-xs': '0.25rem',
                        'space-sm': '0.5rem',
                        'space-md': '1rem',
                        'space-lg': '1.5rem',
                        'space-xl': '2rem',
                    }
                }
            }
        }
    </script>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/aura-theme.css">
    <style>
        body {
            font-family: 'Plus Jakarta Sans', 'Inter', sans-serif;
            background-color: #f8f9ff;
            color: #121c2a;
        }
        .text-headline-lg { font-size: 24px; line-height: 32px; font-weight: 600; }
        .text-headline-md { font-size: 20px; line-height: 28px; font-weight: 600; }
        .text-headline-sm { font-size: 16px; line-height: 24px; font-weight: 600; }
        .text-body-md { font-size: 14px; line-height: 20px; }
        .text-body-sm { font-size: 12px; line-height: 16px; }
        .text-label-lg { font-size: 14px; line-height: 20px; font-weight: 600; }
        .text-label-md { font-size: 12px; line-height: 16px; }
        .text-label-sm { font-size: 11px; line-height: 16px; }
        .text-display-lg { font-size: 32px; line-height: 40px; font-weight: 700; }
        .font-tabular-data { font-variant-numeric: tabular-nums; }
        
        @media print {
            .no-print {
                display: none !important;
            }
            body {
                background: white;
            }
            #billContent {
                box-shadow: none !important;
                border: 1px solid #ccc !important;
                max-width: 100% !important;
                width: 100% !important;
            }
        }
    </style>
</head>

<body class="min-h-screen bg-[#f8f9ff] flex flex-col">

    <!-- Header Component (ẩn khi in) -->
    <div class="no-print">
        <jsp:include page="/components/header.jsp">
            <jsp:param name="activePage" value="orders" />
        </jsp:include>
    </div>

    <main class="flex-grow w-full px-margin md:px-margin-lg py-space-lg max-w-4xl mx-auto">

        <!-- Banner thông báo thanh toán thành công (ẩn khi in) -->
        <div class="no-print mb-space-lg bg-emerald-50 border border-emerald-200 text-emerald-800 p-space-md md:p-space-lg rounded-2xl shadow-sm flex items-center justify-between gap-space-md">
            <div class="flex items-center gap-space-md">
                <div class="w-12 h-12 rounded-full bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-md">
                    <span class="material-symbols-outlined text-[28px]">check_circle</span>
                </div>
                <div class="flex flex-col">
                    <h2 class="font-headline-md font-bold text-emerald-950">
                        Thanh toán thành công!
                    </h2>
                    <span class="font-body-md text-emerald-800">
                        Đơn hàng #${order.orderCode} đã được ghi nhận vào hệ thống và chuyển ngay tới quầy Barista.
                    </span>
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/menu" class="hidden sm:inline-flex items-center gap-1 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-semibold rounded-lg transition-colors shadow-sm">
                <span>Đặt thêm món</span>
                <span class="material-symbols-outlined text-[18px]">add</span>
            </a>
        </div>

        <!-- Electronic Bill Card -->
        <div id="billContent" class="bg-surface-container-lowest rounded-2xl shadow-md p-space-md md:p-space-xl border border-surface-container-high flex flex-col gap-space-md">

            <!-- Bill Top Header -->
            <div class="flex items-center justify-between pb-space-md border-b border-surface-container-high">
                <div class="flex items-center gap-space-sm">
                    <div class="w-12 h-12 rounded-xl bg-primary-container flex items-center justify-center text-white shadow-sm">
                        <span class="material-symbols-outlined text-[26px]">local_cafe</span>
                    </div>
                    <div class="flex flex-col">
                        <span class="font-headline-lg text-headline-lg text-primary tracking-tight font-bold">
                            Aura Café
                        </span>
                        <span class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider font-semibold">
                            Hóa đơn điện tử e-Bill chính thức
                        </span>
                    </div>
                </div>

                <div class="flex flex-col items-end">
                    <div class="flex items-center gap-2">
                        <span class="px-space-sm py-0.5 rounded bg-emerald-100 text-emerald-800 font-label-sm text-label-sm font-semibold flex items-center gap-1">
                            <span class="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
                            ĐÃ THANH TOÁN
                        </span>
                    </div>
                    <span class="font-tabular-data text-headline-sm text-primary font-bold mt-1">
                        #${order.orderCode}
                    </span>
                </div>
            </div>

            <!-- Metadata Grid -->
            <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-space-sm py-space-md bg-surface-bright px-space-md rounded-xl my-space-xs font-body-sm text-body-sm border border-surface-container-high">
                <div class="flex flex-col">
                    <span class="text-on-surface-variant text-xs">Mã hóa đơn:</span>
                    <span class="font-tabular-data font-bold text-on-surface">${order.orderCode}</span>
                </div>
                <div class="flex flex-col">
                    <span class="text-on-surface-variant text-xs">Thời gian tạo:</span>
                    <span class="font-tabular-data font-medium text-on-surface">${order.createdTimeText} ${order.createdDateText}</span>
                </div>
                <div class="flex flex-col">
                    <span class="text-on-surface-variant text-xs">Khách hàng:</span>
                    <span class="font-semibold text-on-surface">${order.recipientName}</span>
                </div>
                <div class="flex flex-col">
                    <span class="text-on-surface-variant text-xs">Số điện thoại:</span>
                    <span class="font-tabular-data font-medium text-on-surface">${order.recipientPhone}</span>
                </div>
            </div>

            <!-- Thông tin giao dịch thanh toán (Payment Details) -->
            <div class="bg-primary/5 p-space-md rounded-xl border border-primary/20 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-space-sm">
                <div class="flex items-center gap-space-sm">
                    <span class="material-symbols-outlined text-primary text-[28px]">payments</span>
                    <div class="flex flex-col">
                        <span class="font-label-sm text-label-sm text-primary font-bold uppercase tracking-wider">
                            Phương thức thanh toán:
                        </span>
                        <span class="font-body-md text-body-md font-semibold text-on-surface">
                            <c:choose>
                                <c:when test="${not empty payment}">
                                    ${payment.paymentMethodDisplay}
                                </c:when>
                                <c:otherwise>Chuyển khoản VietQR</c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                </div>

                <div class="flex flex-col sm:items-end">
                    <span class="font-label-sm text-label-sm text-on-surface-variant">Mã giao dịch đối soát:</span>
                    <span class="font-tabular-data font-mono font-bold text-primary text-sm">
                        <c:choose>
                            <c:when test="${not empty payment and not empty payment.transactionCode}">
                                ${payment.transactionCode}
                            </c:when>
                            <c:otherwise>PAY-${order.orderCode}-CONFIRMED</c:otherwise>
                        </c:choose>
                    </span>
                </div>
            </div>

            <!-- Items Breakdown Table -->
            <div class="flex flex-col my-space-xs divide-y divide-surface-container">
                <div class="flex justify-between py-space-sm text-on-surface-variant font-label-sm text-label-sm uppercase tracking-wider font-semibold">
                    <span>Món / Dịch vụ</span>
                    <div class="flex gap-12 text-right">
                        <span class="w-16">Đơn giá</span>
                        <span class="w-24">Thành tiền</span>
                    </div>
                </div>

                <c:forEach items="${orderDetails}" var="item" varStatus="loop">
                    <div class="flex items-start justify-between py-space-md">
                        <div class="flex flex-col">
                            <div class="flex items-center gap-space-xs">
                                <span class="font-label-sm text-label-sm px-2 py-0.5 rounded bg-surface-container font-bold text-on-surface">
                                    ${item.formattedQuantity}
                                </span>
                                <span class="font-label-lg text-label-lg text-on-surface font-bold">
                                    ${item.productName}
                                </span>
                            </div>
                            <c:if test="${not empty item.optionsText}">
                                <span class="font-body-sm text-body-sm text-on-surface-variant ml-8 mt-0.5">
                                    ${item.optionsText}
                                </span>
                            </c:if>
                        </div>
                        <div class="flex gap-12 text-right items-center">
                            <span class="font-tabular-data text-body-sm text-on-surface-variant w-16">
                                ${item.formattedUnitPrice}
                            </span>
                            <span class="font-tabular-data font-semibold text-on-surface w-24">
                                ${item.formattedSubtotal}
                            </span>
                        </div>
                    </div>
                </c:forEach>
            </div>

            <!-- Calculations Breakdown -->
            <div class="flex flex-col gap-space-xs py-space-md my-space-xs bg-surface-container-low px-space-md md:px-space-lg rounded-xl border border-surface-container-high">
                <div class="flex justify-between font-body-sm text-body-sm text-on-surface-variant">
                    <span>Tạm tính đồ uống & bánh:</span>
                    <span class="font-tabular-data font-medium text-on-surface">${order.formattedTotalAmount}</span>
                </div>
                <div class="flex justify-between font-body-sm text-body-sm text-on-surface-variant">
                    <span>Phí vận chuyển giao tận nơi:</span>
                    <span class="font-tabular-data font-medium text-on-surface">${order.formattedDeliveryFee}</span>
                </div>

                <c:if test="${order.discountAmount > 0}">
                    <div class="flex justify-between font-body-sm text-body-sm text-primary font-medium">
                        <span class="flex items-center gap-1">
                            <span class="material-symbols-outlined text-[16px]">redeem</span>
                            Dùng ${order.pointsUsed} điểm Aura:
                        </span>
                        <span class="font-tabular-data font-semibold">-${order.formattedDiscountAmount}</span>
                    </div>
                </c:if>

                <div class="flex justify-between font-body-sm text-body-sm text-on-surface-variant pt-space-xs border-t border-surface-container">
                    <span class="text-xs">VAT đã tính (8%):</span>
                    <span class="font-tabular-data text-xs text-secondary">${order.formattedVatAmount}</span>
                </div>
            </div>

            <!-- Total Final Amount -->
            <div class="flex items-baseline justify-between pt-space-sm mb-space-sm border-t border-surface-container-high">
                <div>
                    <span class="font-headline-md text-headline-md text-on-surface block font-bold">
                        Tổng cộng thanh toán
                    </span>
                    <span class="font-label-sm text-label-sm text-secondary">
                        Đã thanh toán đủ (Đã bao gồm VAT)
                    </span>
                </div>
                <div class="font-display-lg text-display-lg text-primary font-bold font-tabular-data tracking-tight">
                    ${order.formattedFinalAmount}
                </div>
            </div>

            <!-- In / Theo dõi tiến trình Buttons (ẩn khi in) -->
            <div class="no-print flex flex-col sm:flex-row gap-3 pt-4 border-t border-surface-container">
                <button type="button" onclick="window.print()" class="flex-1 h-12 bg-primary hover:bg-[#780815] text-white rounded-xl font-label-lg flex items-center justify-center gap-2 shadow-sm transition-all cursor-pointer font-semibold border-0">
                    <span class="material-symbols-outlined text-[20px]">print</span>
                    <span>In hóa đơn</span>
                </button>
                <a href="${pageContext.request.contextPath}/orders?orderId=${order.id}" class="flex-1 h-12 bg-[#eff4ff] hover:bg-[#dee9fc] text-[#900b1a] rounded-xl font-label-lg flex items-center justify-center gap-2 border border-[#dee9fc] transition-colors font-semibold text-decoration-none">
                    <span class="material-symbols-outlined text-[20px]">moped</span>
                    <span>Theo dõi tiến trình</span>
                </a>
            </div>

        </div>

    </main>

    <!-- Bootstrap 5 JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        // Khi thanh toán thành công và mở trang Hóa đơn điện tử, tự động xóa giỏ hàng
        document.addEventListener('DOMContentLoaded', function() {
            try {
                localStorage.removeItem('aura_cart');
                var badge = document.getElementById('headerCartBadge');
                if (badge) badge.innerText = '0';
            } catch (e) {}
        });

        <c:if test="${order.inProgress}">
        // Tự động làm mới trang sau 8s để cập nhật tiến trình pha chế / giao hàng
        setTimeout(function() {
            window.location.reload();
        }, 8000);
        </c:if>
    </script>
</body>
</html>
