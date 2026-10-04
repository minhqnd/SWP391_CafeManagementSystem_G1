<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Aura Café | Thanh toán đơn hàng #${order.orderCode}</title>

    <!-- Google Fonts & Icons -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet" />

    <!-- Tailwind CSS (Play CDN với cấu hình theme chính xác của dự án) -->
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
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
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(-4px); }
            to { opacity: 1; transform: translateY(0); }
        }
        .animate-fadeIn { animation: fadeIn 0.25s ease-out forwards; }
    </style>
</head>

<body class="min-h-screen bg-[#f8f9ff]">

    <!-- Header Navbar Component -->
    <jsp:include page="/components/header.jsp">
        <jsp:param name="activePage" value="payment" />
    </jsp:include>

    <div class="flex flex-col w-full">
        <div class="w-full px-4 md:px-8 py-6 max-w-7xl mx-auto">

            <!-- Step Breadcrumb Tracker (1. Giỏ hàng -> 2. Xác nhận đơn -> 3. Thanh toán) -->
            <div class="w-full mb-8">
                <div class="flex items-center justify-between max-w-2xl mx-auto relative">
                    <div class="absolute left-0 top-1/2 -translate-y-1/2 h-1 w-full bg-[#e6eeff] rounded-full -z-0"></div>
                    <div class="absolute left-0 top-1/2 -translate-y-1/2 h-1 w-full bg-[#b2292e] rounded-full -z-0 transition-all duration-500"></div>

                    <!-- Step 1 -->
                    <a href="${pageContext.request.contextPath}/cart" class="flex flex-col items-center gap-1 relative z-10 text-decoration-none group cursor-pointer">
                        <div class="w-10 h-10 rounded-full bg-[#b2292e] text-white flex items-center justify-center shadow-md">
                            <span class="material-symbols-outlined text-[20px]">check</span>
                        </div>
                        <span class="text-[12px] text-[#900b1a] font-semibold">1. Giỏ hàng</span>
                    </a>

                    <!-- Step 2 -->
                    <a href="${pageContext.request.contextPath}/checkout" class="flex flex-col items-center gap-1 relative z-10 text-decoration-none group cursor-pointer">
                        <div class="w-10 h-10 rounded-full bg-[#b2292e] text-white flex items-center justify-center shadow-md">
                            <span class="material-symbols-outlined text-[20px]">check</span>
                        </div>
                        <span class="text-[12px] text-[#900b1a] font-semibold">2. Xác nhận đơn</span>
                    </a>

                    <!-- Step 3 (Active) -->
                    <div class="flex flex-col items-center gap-1 relative z-10">
                        <div class="w-10 h-10 rounded-full bg-[#b2292e] text-white ring-4 ring-[#b2292e]/20 flex items-center justify-center shadow-md">
                            <span class="material-symbols-outlined text-[20px]">payments</span>
                        </div>
                        <span class="text-[14px] text-[#900b1a] font-bold">3. Thanh toán</span>
                    </div>
                </div>
            </div>

            <!-- Toast Thông báo lỗi (nếu vi phạm BR-09 hoặc lỗi giao dịch) -->
            <c:if test="${not empty error}">
                <div class="mb-space-md bg-red-600 text-white px-space-lg py-space-md rounded-xl shadow-lg flex items-center justify-between gap-space-sm animate-bounce">
                    <div class="flex items-center gap-space-sm">
                        <span class="material-symbols-outlined text-[24px]">error</span>
                        <span class="font-label-lg">${error}</span>
                    </div>
                    <button type="button" onclick="this.parentElement.remove()" class="text-white hover:opacity-80">
                        <span class="material-symbols-outlined">close</span>
                    </button>
                </div>
            </c:if>

            <!-- Top Progress & Meta Bar -->
            <div class="bg-surface-container-lowest p-space-md md:p-space-lg rounded-xl shadow-sm mb-space-lg flex flex-col md:flex-row items-start md:items-center justify-between gap-space-md border border-surface-container-high">
                <div class="flex items-center gap-space-md">
                    <div class="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
                        <span class="material-symbols-outlined text-primary text-[28px]">
                            receipt_long
                        </span>
                    </div>
                    <div class="flex flex-col">
                        <div class="flex items-center gap-space-sm flex-wrap">
                            <h1 class="font-headline-lg text-headline-lg text-on-surface font-bold">
                                Thanh toán đơn hàng #${order.orderCode}
                            </h1>
                            <span class="px-space-sm py-0.5 rounded bg-[#FEF3C7] text-[#92400E] font-label-sm text-label-sm font-semibold flex items-center gap-1">
                                <span class="w-1.5 h-1.5 rounded-full bg-[#D97706] animate-pulse"></span>
                                Chờ thanh toán
                            </span>
                        </div>
                        <span class="font-body-md text-body-md text-on-surface-variant">
                            Vui lòng hoàn tất thanh toán để bar bắt đầu chuẩn bị món ngon của bạn
                        </span>
                    </div>
                </div>

                <div class="flex items-center gap-space-md w-full md:w-auto justify-between md:justify-end bg-surface-container-low px-space-md py-space-sm rounded-lg border border-surface-container-high">
                    <div class="flex items-center gap-space-xs text-primary">
                        <span class="material-symbols-outlined text-[20px] animate-spin" style="animation-duration: 4s;">
                            timelapse
                        </span>
                        <span class="font-label-md text-label-md font-medium text-on-surface-variant">
                            Thời gian giữ đơn:
                        </span>
                    </div>
                    <div id="countdownTimer" class="font-headline-sm text-headline-sm font-tabular-data text-primary font-bold tracking-tight">
                        14:58
                    </div>
                </div>
            </div>

            <!-- Form xử lý gửi dữ liệu thanh toán POST lên /payment -->
            <form id="paymentForm" action="${pageContext.request.contextPath}/payment" method="POST">
                <input type="hidden" name="orderId" value="${order.id}" />
                <!-- Trường số tiền truyền lên để Servlet kiểm tra theo luật BR-09 -->
                <input type="hidden" id="amountInput" name="amount" value="${order.finalAmount}" />

                <!-- Main Payment & Bill Split Layout -->
                <div class="grid grid-cols-1 lg:grid-cols-12 gap-gutter-lg items-start">

                    <!-- LEFT COLUMN: Payment Methods (7 cols) -->
                    <section class="lg:col-span-7 flex flex-col gap-space-lg">
                        
                        <!-- Loyalty Points Deduct Card -->
                        <div class="bg-surface-container-lowest p-space-md md:p-space-lg rounded-xl shadow-sm border border-surface-container-high">
                            <div class="flex items-center justify-between">
                                <div class="flex items-center gap-space-sm">
                                    <div class="w-9 h-9 rounded-lg bg-amber-100 flex items-center justify-center">
                                        <span class="material-symbols-outlined text-amber-800 text-[20px]">stars</span>
                                    </div>
                                    <div class="flex flex-col">
                                        <span class="font-label-lg text-label-lg text-on-surface font-semibold">
                                            Aura Points Reward Club
                                        </span>
                                        <span class="font-body-sm text-body-sm text-on-surface-variant">
                                            Ưu đãi thành viên tích lũy điểm Aura trên mỗi hóa đơn thanh toán
                                        </span>
                                    </div>
                                </div>

                                <span class="px-space-sm py-1 rounded-full bg-emerald-50 text-emerald-700 text-xs font-semibold">
                                    Đang hoạt động
                                </span>
                            </div>
                        </div>

                        <!-- Payment Methods Selection Box -->
                        <div class="bg-surface-container-lowest p-space-md md:p-space-lg rounded-xl shadow-sm flex flex-col gap-space-md border border-surface-container-high">
                            <div class="flex items-center justify-between">
                                <h2 class="font-headline-md text-headline-md text-on-surface font-bold">
                                    Phương thức thanh toán
                                </h2>
                                <span class="font-label-sm text-label-sm text-on-surface-variant font-medium">
                                    Mã hoá PCI-DSS Level 1
                                </span>
                            </div>

                            <!-- Method 1: VietQR (Default / Recommended) -->
                            <div id="method-box-vietqr" class="rounded-lg overflow-hidden transition-all duration-200 border-2 border-primary/40 bg-surface-container-low shadow-sm">
                                <label class="p-space-md flex items-center justify-between cursor-pointer hover:bg-surface-container-low transition-colors" onclick="selectPaymentMethod('vietqr')">
                                    <div class="flex items-center gap-space-md">
                                        <input type="radio" name="payment_method" value="vietqr" checked class="w-4 h-4 text-primary accent-primary cursor-pointer" onchange="selectPaymentMethod('vietqr')">
                                        <div class="flex flex-col">
                                            <div class="flex items-center gap-space-xs">
                                                <span class="font-label-lg text-label-lg text-on-surface font-semibold">
                                                    Quét mã VietQR / Chuyển khoản Ngân hàng
                                                </span>
                                                <span class="px-space-xs py-0.5 rounded bg-red-100 text-primary font-label-sm text-label-sm font-bold uppercase tracking-wider">
                                                    Khuyên dùng
                                                </span>
                                            </div>
                                            <span class="font-body-sm text-body-sm text-on-surface-variant">
                                                Tự động khớp lệnh và xác nhận đơn chỉ trong 3 giây
                                            </span>
                                        </div>
                                    </div>
                                    <span class="px-2 py-1 bg-surface-container-lowest rounded font-tabular-data text-label-sm font-semibold text-secondary">
                                        Vietcombank
                                    </span>
                                </label>

                                <div id="method-content-vietqr" class="p-space-md md:p-space-lg bg-surface-container-lowest border-t border-surface-container-high animate-fadeIn">
                                    <div class="grid grid-cols-1 sm:grid-cols-12 gap-space-lg items-center">
                                        <!-- Visual Dynamic QR Code -->
                                        <div class="sm:col-span-5 flex flex-col items-center justify-center p-space-md bg-surface-bright rounded-lg border border-surface-container-high">
                                            <div class="relative w-44 h-44 bg-surface-container-lowest p-2 rounded shadow-sm flex items-center justify-center border border-surface-container-high">
                                                <svg class="w-full h-full text-on-surface" viewBox="0 0 100 100" fill="currentColor">
                                                    <rect x="5" y="5" width="28" height="28" rx="2" fill="#121C2A" />
                                                    <rect x="10" y="10" width="18" height="18" fill="#FFFFFF" />
                                                    <rect x="14" y="14" width="10" height="10" fill="#B2292E" />

                                                    <rect x="67" y="5" width="28" height="28" rx="2" fill="#121C2A" />
                                                    <rect x="72" y="10" width="18" height="18" fill="#FFFFFF" />
                                                    <rect x="76" y="14" width="10" height="10" fill="#B2292E" />

                                                    <rect x="5" y="67" width="28" height="28" rx="2" fill="#121C2A" />
                                                    <rect x="10" y="72" width="18" height="18" fill="#FFFFFF" />
                                                    <rect x="14" y="76" width="10" height="10" fill="#B2292E" />

                                                    <rect x="38" y="8" width="5" height="5" />
                                                    <rect x="48" y="14" width="6" height="6" />
                                                    <rect x="58" y="8" width="5" height="5" />
                                                    <rect x="38" y="24" width="7" height="4" />
                                                    <rect x="48" y="28" width="4" height="6" />
                                                    <rect x="12" y="38" width="6" height="5" />
                                                    <rect x="22" y="44" width="5" height="6" />
                                                    <rect x="32" y="38" width="8" height="8" />
                                                    <rect x="45" y="40" width="10" height="10" fill="#B2292E" />
                                                    <rect x="60" y="38" width="7" height="6" />
                                                    <rect x="72" y="44" width="6" height="8" />
                                                    <rect x="84" y="38" width="5" height="5" />
                                                    <rect x="38" y="56" width="6" height="8" />
                                                    <rect x="50" y="54" width="7" height="6" />
                                                    <rect x="62" y="60" width="8" height="6" />
                                                    <rect x="76" y="58" width="5" height="5" />
                                                    <rect x="85" y="68" width="6" height="6" />
                                                    <rect x="38" y="72" width="5" height="5" />
                                                    <rect x="48" y="70" width="8" height="6" />
                                                    <rect x="60" y="74" width="5" height="7" />
                                                    <rect x="70" y="80" width="6" height="6" />
                                                    <rect x="82" y="82" width="8" height="8" />
                                                    <rect x="42" y="86" width="10" height="4" />
                                                    <rect x="56" y="88" width="7" height="5" />
                                                </svg>
                                                <div class="absolute inset-0 flex items-center justify-center pointer-events-none">
                                                    <span class="w-8 h-8 rounded-full bg-primary-container text-white flex items-center justify-center shadow-md">
                                                        <span class="material-symbols-outlined text-[16px]">local_cafe</span>
                                                    </span>
                                                </div>
                                            </div>
                                            <span class="font-label-sm text-label-sm text-on-surface-variant mt-space-sm flex items-center gap-1 font-medium">
                                                <span class="material-symbols-outlined text-[14px] text-primary">sensors</span> Mở app ngân hàng để quét
                                            </span>
                                        </div>

                                        <!-- Transfer Details -->
                                        <div class="sm:col-span-7 flex flex-col gap-space-sm">
                                            <div class="bg-surface-bright p-space-sm rounded-lg flex items-center justify-between border border-surface-container-high">
                                                <div>
                                                    <div class="font-label-sm text-label-sm text-on-surface-variant">Ngân hàng thụ hưởng</div>
                                                    <div class="font-label-lg text-label-lg font-semibold text-on-surface">Vietcombank (CN Hà Nội)</div>
                                                </div>
                                                <span class="px-2 py-0.5 rounded bg-surface-container font-label-sm text-label-sm font-medium text-secondary">VCB</span>
                                            </div>

                                            <div class="bg-surface-bright p-space-sm rounded-lg flex items-center justify-between border border-surface-container-high">
                                                <div>
                                                    <div class="font-label-sm text-label-sm text-on-surface-variant">Số tài khoản</div>
                                                    <div class="font-tabular-data text-headline-sm font-bold text-on-surface tracking-wider">9888 823 456</div>
                                                </div>
                                                <button type="button" onclick="copyText('9888823456', this)" class="px-space-sm py-1 rounded bg-surface-container-highest hover:bg-surface-container text-on-surface font-label-sm text-label-sm transition-colors cursor-pointer">
                                                    Sao chép
                                                </button>
                                            </div>

                                            <div class="bg-surface-bright p-space-sm rounded-lg flex items-center justify-between border border-surface-container-high">
                                                <div>
                                                    <div class="font-label-sm text-label-sm text-on-surface-variant">Số tiền chính xác</div>
                                                    <div class="font-tabular-data text-headline-sm font-bold text-primary">${order.formattedFinalAmount}</div>
                                                </div>
                                                <button type="button" onclick="copyText('${order.finalAmount}', this)" class="px-space-sm py-1 rounded bg-surface-container-highest hover:bg-surface-container text-on-surface font-label-sm text-label-sm transition-colors cursor-pointer">
                                                    Sao chép
                                                </button>
                                            </div>

                                            <div class="bg-primary/5 p-space-sm rounded-lg flex items-center justify-between border border-primary/20">
                                                <div>
                                                    <div class="font-label-sm text-label-sm text-primary font-medium">Nội dung chuyển khoản (Bắt buộc)</div>
                                                    <div class="font-tabular-data text-headline-sm font-bold text-primary tracking-widest">${order.orderCode}</div>
                                                </div>
                                                <button type="button" onclick="copyText('${order.orderCode}', this)" class="px-space-sm py-1 rounded bg-primary text-white font-label-sm text-label-sm transition-colors cursor-pointer font-semibold shadow-sm">
                                                    Sao chép
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Method 2: E-Wallets (MoMo, ZaloPay) -->
                            <div id="method-box-ewallet" class="rounded-lg overflow-hidden transition-all duration-200 border-2 border-transparent bg-surface-bright">
                                <label class="p-space-md flex items-center justify-between cursor-pointer hover:bg-surface-container-low transition-colors" onclick="selectPaymentMethod('ewallet')">
                                    <div class="flex items-center gap-space-md">
                                        <input type="radio" name="payment_method" value="momo" class="w-4 h-4 text-primary accent-primary cursor-pointer" onchange="selectPaymentMethod('ewallet')">
                                        <div class="flex flex-col">
                                            <span class="font-label-lg text-label-lg text-on-surface font-semibold">
                                                Ví điện tử liên kết
                                            </span>
                                            <span class="font-body-sm text-body-sm text-on-surface-variant">
                                                MoMo, ZaloPay (Hoàn tiền Aura 2%)
                                            </span>
                                        </div>
                                    </div>
                                    <div class="flex items-center gap-space-xs">
                                        <span class="px-2 py-0.5 rounded bg-[#A50064] text-white font-label-sm text-label-sm font-bold">MoMo</span>
                                        <span class="px-2 py-0.5 rounded bg-[#0068FF] text-white font-label-sm text-label-sm font-bold">ZaloPay</span>
                                    </div>
                                </label>

                                <div id="method-content-ewallet" class="p-space-md bg-surface-container-lowest border-t border-surface-container-high hidden animate-fadeIn">
                                    <span class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider block mb-space-sm font-semibold">
                                        Chọn ứng dụng ví để tiếp tục:
                                    </span>
                                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-space-sm">
                                        <div class="p-space-md rounded-lg text-left flex items-center gap-space-sm bg-surface-container border border-primary shadow-sm">
                                            <div class="w-8 h-8 rounded-full bg-[#A50064] text-white flex items-center justify-center font-bold text-xs shrink-0">M</div>
                                            <div>
                                                <div class="font-label-md text-label-md font-bold text-on-surface">Ví MoMo</div>
                                                <div class="font-label-sm text-label-sm text-secondary">Tự động chuyển tiếp</div>
                                            </div>
                                        </div>
                                        <div class="p-space-md rounded-lg text-left flex items-center gap-space-sm bg-surface hover:bg-surface-container-low border border-transparent">
                                            <div class="w-8 h-8 rounded-full bg-[#0068FF] text-white flex items-center justify-center font-bold text-xs shrink-0">Z</div>
                                            <div>
                                                <div class="font-label-md text-label-md font-bold text-on-surface">ZaloPay</div>
                                                <div class="font-label-sm text-label-sm text-secondary">Giảm thêm 10.000đ</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Method 3: Card (Visa / MasterCard) -->
                            <div id="method-box-card" class="rounded-lg overflow-hidden transition-all duration-200 border-2 border-transparent bg-surface-bright">
                                <label class="p-space-md flex items-center justify-between cursor-pointer hover:bg-surface-container-low transition-colors" onclick="selectPaymentMethod('card')">
                                    <div class="flex items-center gap-space-md">
                                        <input type="radio" name="payment_method" value="card" class="w-4 h-4 text-primary accent-primary cursor-pointer" onchange="selectPaymentMethod('card')">
                                        <div class="flex flex-col">
                                            <span class="font-label-lg text-label-lg text-on-surface font-semibold">
                                                Thẻ quốc tế Visa / MasterCard / JCB
                                            </span>
                                            <span class="font-body-sm text-body-sm text-on-surface-variant">
                                                Bảo vệ qua cổng 3D Secure OTP ngân hàng
                                            </span>
                                        </div>
                                    </div>
                                    <span class="material-symbols-outlined text-[24px] text-on-surface-variant">credit_card</span>
                                </label>

                                <div id="method-content-card" class="p-space-md md:p-space-lg bg-surface-container-lowest border-t border-surface-container-high hidden gap-space-md animate-fadeIn flex-col">
                                    <div class="flex flex-col gap-space-xs">
                                        <label class="font-label-md text-label-md text-on-surface font-semibold">Số thẻ</label>
                                        <input type="text" placeholder="4123 4567 8901 2345" class="w-full h-10 px-space-md rounded-lg bg-surface font-tabular-data text-body-md text-on-surface border border-surface-container-highest focus:outline-none" />
                                    </div>
                                </div>
                            </div>

                            <!-- Method 4: COD (Tiền mặt khi nhận hàng) -->
                            <div id="method-box-cod" class="rounded-lg overflow-hidden transition-all duration-200 border-2 border-transparent bg-surface-bright">
                                <label class="p-space-md flex items-center justify-between cursor-pointer hover:bg-surface-container-low transition-colors" onclick="selectPaymentMethod('cod')">
                                    <div class="flex items-center gap-space-md">
                                        <input type="radio" name="payment_method" value="cod" class="w-4 h-4 text-primary accent-primary cursor-pointer" onchange="selectPaymentMethod('cod')">
                                        <div class="flex flex-col">
                                            <span class="font-label-lg text-label-lg text-on-surface font-semibold">
                                                Thanh toán khi nhận hàng (COD / Tiền mặt)
                                            </span>
                                            <span class="font-body-sm text-body-sm text-on-surface-variant">
                                                Trả tiền mặt trực tiếp cho đối tác giao nhận khi kiểm tra đồ uống
                                            </span>
                                        </div>
                                    </div>
                                    <span class="material-symbols-outlined text-secondary text-[24px]">payments</span>
                                </label>

                                <div id="method-content-cod" class="p-space-md bg-surface-container-lowest border-t border-surface-container-high hidden animate-fadeIn">
                                    <div class="p-space-sm bg-surface rounded-lg text-on-surface-variant font-body-sm text-body-sm flex items-start gap-space-sm border border-surface-container-high">
                                        <span class="material-symbols-outlined text-primary text-[18px] shrink-0 mt-0.5">info</span>
                                        <span>Tài xế Aura Express sẽ mang theo tiền lẻ trả lại. Vui lòng giữ liên lạc điện thoại khi đơn hàng được giao.</span>
                                    </div>
                                </div>
                            </div>

                        </div>

                        <!-- Trust Badges -->
                        <div class="flex items-center justify-around py-space-sm text-on-surface-variant text-label-sm font-label-sm border-t border-surface-container-high">
                            <div class="flex items-center gap-1 font-medium">
                                <span class="material-symbols-outlined text-[16px] text-primary">verified_user</span>
                                <span>Thanh toán an toàn 100%</span>
                            </div>
                            <div class="flex items-center gap-1 font-medium">
                                <span class="material-symbols-outlined text-[16px] text-primary">history</span>
                                <span>Hoàn tiền tự động nếu hủy đơn</span>
                            </div>
                            <div class="flex items-center gap-1 font-medium">
                                <span class="material-symbols-outlined text-[16px] text-primary">support_agent</span>
                                <span>Hotline: 1900 6868</span>
                            </div>
                        </div>

                    </section>

                    <!-- RIGHT COLUMN: Official Electronic Bill Preview (5 cols) -->
                    <aside class="lg:col-span-5 flex flex-col gap-space-md">
                        <div class="bg-surface-container-lowest rounded-xl shadow-md p-space-md md:p-space-lg relative overflow-hidden flex flex-col border border-surface-container-high">

                            <!-- Top Header -->
                            <div class="flex items-center justify-between pb-space-md border-b border-surface-container-high">
                                <div class="flex items-center gap-space-sm">
                                    <div class="w-10 h-10 rounded-lg bg-primary-container flex items-center justify-center text-white">
                                        <span class="material-symbols-outlined text-[22px]">local_cafe</span>
                                    </div>
                                    <div class="flex flex-col">
                                        <span class="font-headline-sm text-headline-sm text-primary tracking-tight font-bold">
                                            Aura Café
                                        </span>
                                        <span class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider font-semibold">
                                            Hóa đơn điện tử e-Bill
                                        </span>
                                    </div>
                                </div>
                                <div class="flex flex-col items-end">
                                    <span class="font-label-sm text-label-sm text-on-surface-variant uppercase">
                                        Mã đơn đặt hàng
                                    </span>
                                    <span class="font-tabular-data text-headline-sm text-primary font-bold">
                                        #${order.orderCode}
                                    </span>
                                </div>
                            </div>

                            <!-- Metadata Grid -->
                            <div class="grid grid-cols-2 gap-space-xs py-space-sm bg-surface-bright px-space-md rounded-lg my-space-sm font-body-sm text-body-sm border border-surface-container-high">
                                <div class="flex flex-col">
                                    <span class="text-on-surface-variant">Thời gian xuất:</span>
                                    <span class="font-tabular-data font-medium text-on-surface">
                                        <fmt:formatDate value="${order.createdAt}" pattern="dd/MM/yyyy HH:mm" />
                                    </span>
                                </div>
                                <div class="flex flex-col">
                                    <span class="text-on-surface-variant">Hình thức / Vị trí:</span>
                                    <span class="font-medium text-on-surface">
                                        <c:choose>
                                            <c:when test="${not empty order.tableNumber}">Bàn ${order.tableNumber}</c:when>
                                            <c:otherwise>${order.orderType}</c:otherwise>
                                        </c:choose>
                                    </span>
                                </div>
                                <div class="flex flex-col mt-space-xs">
                                    <span class="text-on-surface-variant">Khách hàng:</span>
                                    <span class="font-medium text-on-surface">${order.recipientName}</span>
                                </div>
                                <div class="flex flex-col mt-space-xs">
                                    <span class="text-on-surface-variant">Số điện thoại:</span>
                                    <span class="font-tabular-data font-medium text-on-surface">${order.recipientPhone}</span>
                                </div>
                            </div>

                            <!-- Items Breakdown (Danh sách món) -->
                            <div class="flex flex-col my-space-xs divide-y divide-surface-container">
                                <div class="flex justify-between py-space-xs text-on-surface-variant font-label-sm text-label-sm uppercase tracking-wider font-semibold">
                                    <span>Món / Dịch vụ</span>
                                    <span class="text-right">Thành tiền</span>
                                </div>

                                <c:forEach items="${orderDetails}" var="item" varStatus="loop">
                                    <div class="flex items-start justify-between py-space-sm">
                                        <div class="flex flex-col">
                                            <div class="flex items-center gap-space-xs">
                                                <span class="font-label-sm text-label-sm px-1.5 py-0.5 rounded bg-surface-container font-bold text-on-surface">
                                                    ${item.formattedQuantity}
                                                </span>
                                                <span class="font-label-md text-label-md text-on-surface font-semibold">
                                                    ${item.productName}
                                                </span>
                                            </div>
                                            <span class="font-body-sm text-body-sm text-on-surface-variant ml-7">
                                                ${item.customization}
                                            </span>
                                        </div>
                                        <span class="font-tabular-data text-tabular-data font-medium text-on-surface">
                                            ${item.formattedSubtotal}
                                        </span>
                                    </div>
                                </c:forEach>
                            </div>

                            <!-- Calculations Breakdown -->
                            <div class="flex flex-col gap-space-xs py-space-sm my-space-xs bg-surface-container-low px-space-md rounded-lg border border-surface-container-high">
                                <div class="flex justify-between font-body-sm text-body-sm text-on-surface-variant">
                                    <span>Tạm tính đồ uống & bánh:</span>
                                    <span class="font-tabular-data text-on-surface">${order.formattedTotalAmount}</span>
                                </div>
                                <div class="flex justify-between font-body-sm text-body-sm text-on-surface-variant">
                                    <span>Phí vận chuyển:</span>
                                    <span class="font-tabular-data text-on-surface">${order.formattedDeliveryFee}</span>
                                </div>

                                <c:if test="${order.discountAmount > 0}">
                                    <div class="flex justify-between font-body-sm text-body-sm text-primary font-medium">
                                        <span class="flex items-center gap-1">
                                            <span class="material-symbols-outlined text-[16px]">sell</span>
                                            Voucher giảm giá (${order.voucherCode != null ? order.voucherCode : 'Áp dụng'}):
                                        </span>
                                        <span class="font-tabular-data">-${order.formattedDiscountAmount}</span>
                                    </div>
                                </c:if>

                                <div class="flex justify-between font-body-sm text-body-sm text-on-surface-variant pt-space-xs">
                                    <span class="text-xs">VAT đã tính (8%):</span>
                                    <span class="font-tabular-data text-xs text-secondary">${order.formattedVatAmount}</span>
                                </div>
                            </div>

                            <!-- Total Due -->
                            <div class="flex items-baseline justify-between pt-space-md mb-space-md">
                                <div>
                                    <span class="font-headline-sm text-headline-sm text-on-surface block font-bold">
                                        Tổng thanh toán
                                    </span>
                                    <span class="font-label-sm text-label-sm text-secondary">
                                        Đã bao gồm thuế và phí dịch vụ
                                    </span>
                                </div>
                                <div class="font-display-lg text-display-lg text-primary font-bold font-tabular-data tracking-tight">
                                    ${order.formattedFinalAmount}
                                </div>
                            </div>

                            <!-- Actions & CTA Buttons -->
                            <div class="flex flex-col gap-space-sm mt-auto">
                                <button type="submit" class="w-full h-12 bg-primary hover:bg-[#982126] text-white rounded-lg font-label-lg text-label-lg flex items-center justify-center gap-space-sm shadow-md transition-all cursor-pointer font-semibold">
                                    <span>Xác nhận đã thanh toán</span>
                                    <span class="material-symbols-outlined text-[20px]">arrow_forward</span>
                                </button>

                                <div class="grid grid-cols-2 gap-space-sm">
                                    <button type="button" onclick="window.print()" class="h-10 rounded-lg bg-surface hover:bg-surface-container-high text-on-surface font-label-md text-label-md flex items-center justify-center gap-space-xs transition-colors cursor-pointer border border-surface-container-highest">
                                        <span class="material-symbols-outlined text-[18px]">print</span>
                                        <span>In hóa đơn PDF</span>
                                    </button>
                                    <button type="button" onclick="alert('Đã tải hóa đơn điện tử e-Bill #${order.orderCode} về máy của bạn!')" class="h-10 rounded-lg bg-surface hover:bg-surface-container-high text-on-surface font-label-md text-label-md flex items-center justify-center gap-space-xs transition-colors cursor-pointer border border-surface-container-highest">
                                        <span class="material-symbols-outlined text-[18px]">download</span>
                                        <span>Tải e-Bill</span>
                                    </button>
                                </div>
                            </div>

                            <!-- Footnote -->
                            <div class="mt-space-md pt-space-sm text-center border-t border-surface-container">
                                <span class="font-label-sm text-label-sm text-secondary tracking-widest uppercase">
                                    Aura Specialty Coffee JSC • Tra cứu tại invoice.auracafe.vn
                                </span>
                            </div>

                        </div>

                        <!-- Note prompt -->
                        <div class="p-space-md bg-surface-container-lowest rounded-xl shadow-sm flex items-center gap-space-md border border-surface-container-high">
                            <span class="w-10 h-10 rounded-full bg-secondary-container text-on-secondary-container flex items-center justify-center shrink-0">
                                <span class="material-symbols-outlined text-[20px]">coffee_maker</span>
                            </span>
                            <div class="flex flex-col">
                                <span class="font-label-md text-label-md text-on-surface font-semibold">
                                    Cần ghi chú pha chế thêm?
                                </span>
                                <span class="font-body-sm text-body-sm text-on-surface-variant">
                                    ${order.orderNote != null ? order.orderNote : 'Bạn có thể nhắn trực tiếp với Barista sau khi thanh toán thành công.'}
                                </span>
                            </div>
                        </div>

                    </aside>
                </div>
            </form>
        </div>
    </div>

    <!-- Script điều khiển chuyển tab thanh toán, copy clipboard và đồng hồ đếm ngược -->
    <script>
        function selectPaymentMethod(method) {
            const methods = ['vietqr', 'ewallet', 'card', 'cod'];
            methods.forEach(function(m) {
                const box = document.getElementById('method-box-' + m);
                const content = document.getElementById('method-content-' + m);
                const radio = box ? box.querySelector('input[type="radio"]') : null;

                if (m === method) {
                    if (box) box.className = 'rounded-lg overflow-hidden transition-all duration-200 border-2 border-primary/40 bg-surface-container-low shadow-sm';
                    if (content) {
                        content.classList.remove('hidden');
                        content.classList.add('flex');
                    }
                    if (radio) radio.checked = true;
                } else {
                    if (box) box.className = 'rounded-lg overflow-hidden transition-all duration-200 border-2 border-transparent bg-surface-bright';
                    if (content) {
                        content.classList.add('hidden');
                        content.classList.remove('flex');
                    }
                    if (radio) radio.checked = false;
                }
            });
        }

        function copyText(text, btnElement) {
            navigator.clipboard.writeText(text).then(function() {
                const oldText = btnElement.innerText;
                btnElement.innerText = 'Đã sao chép';
                btnElement.classList.add('bg-emerald-600', 'text-white');
                setTimeout(function() {
                    btnElement.innerText = oldText;
                    btnElement.classList.remove('bg-emerald-600', 'text-white');
                }, 2000);
            });
        }

        // Countdown timer 15 phút
        (function startTimer() {
            let timeLeft = 14 * 60 + 58;
            const timerElem = document.getElementById('countdownTimer');
            if (!timerElem) return;

            setInterval(function() {
                if (timeLeft > 0) {
                    timeLeft--;
                    const mins = Math.floor(timeLeft / 60).toString().padStart(2, '0');
                    const secs = (timeLeft % 60).toString().padStart(2, '0');
                    timerElem.innerText = mins + ':' + secs;
                }
            }, 1000);
        })();
    </script>
</body>
</html>
