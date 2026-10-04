<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Aura Café | Đơn hàng của tôi &amp; Theo dõi tiến trình</title>

    <!-- Google Fonts & Icons -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet" />

    <!-- Tailwind CSS (Play CDN với cấu hình chuẩn Material 3 Aura) -->
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        primary: '#900b1a',
                        'primary-container': '#b2292e',
                        'on-primary': '#ffffff',
                        'on-primary-container': '#ffcdc9',
                        'primary-fixed': '#ffdad7',
                        secondary: '#555f71',
                        'secondary-container': '#d6e0f6',
                        'on-secondary-container': '#596376',
                        tertiary: '#6e3900',
                        'tertiary-container': '#904d00',
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
                        'gutter-lg': '1.5rem',
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
</head>

<body class="min-h-screen bg-[#f8f9ff]">

    <!-- Header Navbar Component -->
    <jsp:include page="/components/header.jsp">
        <jsp:param name="activePage" value="orders" />
    </jsp:include>

    <main class="w-full">
        <div class="w-full max-w-7xl mx-auto px-4 md:px-8 py-8 flex flex-col gap-8">
            
            <!-- Header Meta & Search Bar -->
            <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
                <div class="flex flex-col gap-1">
                    <div class="flex items-center gap-2">
                        <span class="text-[11px] text-[#900b1a] uppercase tracking-widest font-semibold">
                            Theo dõi &amp; Đối soát đơn
                        </span>
                        <span class="w-1.5 h-1.5 rounded-full bg-[#900b1a]"></span>
                        <span class="font-tabular-data text-[12px] text-[#59413f] font-medium">
                            Cập nhật 1 phút trước
                        </span>
                    </div>
                    <h1 class="text-[32px] md:text-[36px] font-bold text-[#121c2a] tracking-tight">
                        Đơn hàng của tôi
                    </h1>
                    <p class="text-[14px] text-[#59413f]">
                        Quản lý các tách cà phê thủ công yêu thích và kiểm tra tiến trình giao vận theo thời gian thực.
                    </p>
                </div>

                <!-- Search box -->
                <div class="flex items-center gap-3 w-full md:w-auto">
                    <div class="relative flex-1 md:w-72">
                        <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#555f71] text-[18px]">
                            receipt_long
                        </span>
                        <input type="text" id="orderSearchInput" onkeyup="filterOrders()"
                               placeholder="Tìm theo mã đơn #AUR-..." 
                               class="w-full h-10 pl-9 pr-4 rounded-lg bg-white text-[#121c2a] placeholder:text-[#555f71] text-[13px] shadow-sm focus:outline-none focus:ring-2 focus:ring-[#b2292e]/20 border border-[#dee9fc]" />
                    </div>
                </div>
            </div>

            <!-- Status Tabs -->
            <div class="flex items-center gap-2 overflow-x-auto pb-2 no-scrollbar">
                <button type="button" onclick="setTab('all')" id="tab-all"
                        class="tab-btn px-4 py-2 rounded-lg text-[13px] flex items-center gap-2 shrink-0 shadow-sm cursor-pointer bg-[#900b1a] text-white font-semibold">
                    <span>Tất cả đơn</span>
                    <span class="px-1.5 py-0.5 rounded-full bg-black/10 text-current text-[10px] font-tabular-data">${orders != null ? orders.size() : 1}</span>
                </button>
                <button type="button" onclick="setTab('processing')" id="tab-processing"
                        class="tab-btn px-4 py-2 rounded-lg text-[13px] flex items-center gap-2 shrink-0 shadow-sm cursor-pointer bg-white text-[#121c2a] hover:bg-[#e6eeff]">
                    <span>Đang xử lý</span>
                    <span class="px-1.5 py-0.5 rounded-full bg-[#ffdad7] text-[#900b1a] font-bold text-[10px] font-tabular-data">1</span>
                </button>
                <button type="button" onclick="setTab('shipping')" id="tab-shipping"
                        class="tab-btn px-4 py-2 rounded-lg text-[13px] flex items-center gap-2 shrink-0 shadow-sm cursor-pointer bg-white text-[#555f71] hover:bg-[#e6eeff]">
                    <span>Đang giao hàng</span>
                </button>
                <button type="button" onclick="setTab('completed')" id="tab-completed"
                        class="tab-btn px-4 py-2 rounded-lg text-[13px] flex items-center gap-2 shrink-0 shadow-sm cursor-pointer bg-white text-[#555f71] hover:bg-[#e6eeff]">
                    <span>Đã hoàn thành</span>
                </button>
            </div>

            <!-- Active Live Order Spotlight Card -->
            <c:set var="activeOrder" value="${not empty currentOrder ? currentOrder : (not empty orders ? orders[0] : null)}" />
            <c:if test="${not empty activeOrder}">
                <div class="relative bg-white rounded-xl shadow-md overflow-hidden flex flex-col border border-[#dee9fc]">
                    
                    <!-- Status Gradient Strip -->
                    <div class="bg-gradient-to-r from-[#b2292e] via-[#900b1a] to-[#b2292e] p-4 md:p-6 text-white flex flex-wrap items-center justify-between gap-4">
                        <div class="flex items-center gap-3">
                            <div class="w-10 h-10 rounded-lg bg-white/10 flex items-center justify-center">
                                <span class="material-symbols-outlined text-[24px] text-white animate-pulse">
                                    skillet
                                </span>
                            </div>
                            <div>
                                <div class="flex items-center gap-2">
                                    <span class="text-[18px] font-bold tracking-wide">
                                        Đơn hàng #${activeOrder.orderCode}
                                    </span>
                                    <span class="px-2.5 py-0.5 rounded-full bg-white text-[#900b1a] text-[11px] uppercase font-bold tracking-wider">
                                        ${activeOrder.status == 'CONFIRMED' ? 'Barista đang pha chế' : (activeOrder.status == 'PENDING' ? 'Chờ thanh toán' : activeOrder.status)}
                                    </span>
                                </div>
                                <p class="text-[12px] text-[#ffcdc9] mt-0.5">
                                    Chi nhánh Tràng Tiền • Đặt lúc <fmt:formatDate value="${activeOrder.createdAt}" pattern="HH:mm, dd/MM/yyyy" />
                                </p>
                            </div>
                        </div>

                        <div class="flex items-center gap-6">
                            <div class="text-right">
                                <span class="text-[11px] text-[#ffcdc9] block uppercase tracking-wider">
                                    Thời gian nhận dự kiến
                                </span>
                                <span class="text-[16px] font-tabular-data font-bold text-white">
                                    ~18 phút nữa
                                </span>
                            </div>
                            <div class="hidden sm:flex items-center gap-1.5 bg-white/15 px-3 py-1.5 rounded-lg">
                                <span class="material-symbols-outlined text-white text-[18px]">verified</span>
                                <span class="text-[12px] font-medium">Bảo hiểm độ ấm cà phê</span>
                            </div>
                        </div>
                    </div>

                    <!-- Tracking Content Grid -->
                    <div class="p-6 grid grid-cols-1 lg:grid-cols-12 gap-6">
                        
                        <!-- Left: Progress Stepper & Items (7 cols) -->
                        <div class="lg:col-span-7 flex flex-col gap-6">
                            
                            <!-- Stepper 4 bước -->
                            <div class="flex flex-col gap-4 bg-[#eff4ff] p-4 rounded-xl border border-[#dee9fc]">
                                <div class="flex items-center justify-between">
                                    <span class="text-[15px] text-[#121c2a] font-semibold">Tiến trình đơn hàng</span>
                                    <span class="text-[11px] text-[#900b1a] font-bold uppercase tracking-wider">Bước 2 / 4</span>
                                </div>

                                <div class="relative flex flex-col sm:flex-row justify-between gap-4 sm:gap-0 pt-2 pb-1">
                                    <!-- Step 1: Placed -->
                                    <div class="relative z-10 flex sm:flex-col items-center gap-2 text-left sm:text-center sm:w-1/4">
                                        <div class="w-8 h-8 rounded-full bg-[#900b1a] text-white flex items-center justify-center shadow-sm shrink-0">
                                            <span class="material-symbols-outlined text-[16px]">done</span>
                                        </div>
                                        <div class="flex flex-col">
                                            <span class="text-[12px] text-[#121c2a] font-semibold">Đã đặt đơn</span>
                                            <span class="font-tabular-data text-[11px] text-[#59413f]">Xong</span>
                                        </div>
                                    </div>

                                    <!-- Step 2: Barista in prep -->
                                    <div class="relative z-10 flex sm:flex-col items-center gap-2 text-left sm:text-center sm:w-1/4">
                                        <div class="relative w-8 h-8 rounded-full bg-[#b2292e] text-white flex items-center justify-center shadow-md ring-4 ring-[#b2292e]/20 shrink-0">
                                            <span class="material-symbols-outlined text-[16px] animate-spin" style="animation-duration: 4s;">
                                                local_cafe
                                            </span>
                                        </div>
                                        <div class="flex flex-col">
                                            <span class="text-[12px] text-[#900b1a] font-bold">Barista pha chế</span>
                                            <span class="font-tabular-data text-[11px] text-[#900b1a] font-medium">Đang làm</span>
                                        </div>
                                    </div>

                                    <!-- Step 3: Delivery -->
                                    <div class="relative z-10 flex sm:flex-col items-center gap-2 text-left sm:text-center sm:w-1/4 opacity-60">
                                        <div class="w-8 h-8 rounded-full bg-[#dee9fc] text-[#555f71] flex items-center justify-center shrink-0">
                                            <span class="material-symbols-outlined text-[16px]">moped</span>
                                        </div>
                                        <div class="flex flex-col">
                                            <span class="text-[12px] text-[#555f71] font-semibold">Đang giao hàng</span>
                                            <span class="font-tabular-data text-[11px] text-[#555f71]">Chờ nhận</span>
                                        </div>
                                    </div>

                                    <!-- Step 4: Completed -->
                                    <div class="relative z-10 flex sm:flex-col items-center gap-2 text-left sm:text-center sm:w-1/4 opacity-60">
                                        <div class="w-8 h-8 rounded-full bg-[#dee9fc] text-[#555f71] flex items-center justify-center shrink-0">
                                            <span class="material-symbols-outlined text-[16px]">check_circle</span>
                                        </div>
                                        <div class="flex flex-col">
                                            <span class="text-[12px] text-[#555f71] font-semibold">Hoàn thành</span>
                                            <span class="font-tabular-data text-[11px] text-[#555f71]">Thưởng thức</span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Items in Order -->
                            <div class="flex flex-col gap-3">
                                <h3 class="text-[15px] font-semibold text-[#121c2a]">Danh sách món trong đơn</h3>
                                <div class="flex flex-col divide-y divide-[#e6eeff] border border-[#dee9fc] rounded-xl p-4 bg-[#f8f9ff]">
                                    <c:forEach items="${activeOrderDetails}" var="item">
                                        <div class="flex items-center justify-between py-2.5">
                                            <div class="flex flex-col">
                                                <div class="flex items-center gap-2">
                                                    <span class="text-[11px] font-bold px-1.5 py-0.5 rounded bg-[#dee9fc] text-[#121c2a]">
                                                        ${item.formattedQuantity}
                                                    </span>
                                                    <span class="text-[13px] font-semibold text-[#121c2a]">
                                                        ${item.productName}
                                                    </span>
                                                </div>
                                                <span class="text-[11px] text-[#59413f] ml-6 mt-0.5">
                                                    ${item.customization}
                                                </span>
                                            </div>
                                            <span class="font-tabular-data text-[13px] font-semibold text-[#121c2a]">
                                                ${item.formattedSubtotal}
                                            </span>
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                        </div>

                        <!-- Right: Driver & Action Buttons (5 cols) -->
                        <div class="lg:col-span-5 flex flex-col gap-4">
                            
                            <!-- Delivery Driver Card -->
                            <div class="p-4 rounded-xl bg-[#eff4ff] border border-[#dee9fc] flex flex-col gap-3">
                                <div class="flex items-center justify-between">
                                    <span class="text-[12px] font-bold uppercase tracking-wider text-[#900b1a]">Tài xế Aura Express</span>
                                    <span class="px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 text-[10px] font-bold">Đã phân công</span>
                                </div>
                                <div class="flex items-center gap-3">
                                    <div class="w-12 h-12 rounded-full bg-[#dee9fc] flex items-center justify-center text-[#900b1a] shrink-0 font-bold text-[18px]">
                                        TK
                                    </div>
                                    <div class="flex flex-col flex-1 min-w-0">
                                        <span class="text-[14px] font-bold text-[#121c2a]">Nguyễn Tuấn Kiệt</span>
                                        <span class="text-[12px] text-[#555f71]">Honda Wave • 29B1-888.88</span>
                                        <span class="text-[11px] text-[#59413f]">SĐT: 0912 888 999</span>
                                    </div>
                                    <a href="tel:0912888999" class="w-9 h-9 rounded-lg bg-[#b2292e] text-white flex items-center justify-center hover:bg-[#900b1a] transition-colors text-decoration-none shadow-sm">
                                        <span class="material-symbols-outlined text-[18px]">call</span>
                                    </a>
                                </div>
                            </div>

                            <!-- Delivery Address & Payment Summary -->
                            <div class="p-4 rounded-xl bg-[#f8f9ff] border border-[#dee9fc] flex flex-col gap-2.5 text-[13px]">
                                <div class="flex justify-between">
                                    <span class="text-[#59413f]">Người nhận:</span>
                                    <span class="font-semibold text-[#121c2a]">${activeOrder.recipientName} (${activeOrder.recipientPhone})</span>
                                </div>
                                <div class="flex justify-between">
                                    <span class="text-[#59413f]">Địa chỉ:</span>
                                    <span class="font-medium text-[#121c2a] text-right truncate max-w-[220px]" title="${activeOrder.deliveryAddress}">
                                        ${activeOrder.deliveryAddress}
                                    </span>
                                </div>
                                <div class="flex justify-between border-t border-[#dee9fc] pt-2">
                                    <span class="text-[#59413f]">Tổng thanh toán:</span>
                                    <span class="text-[18px] font-bold text-[#900b1a] font-tabular-data">${activeOrder.formattedFinalAmount}</span>
                                </div>
                            </div>

                            <!-- Action Buttons -->
                            <div class="flex flex-col gap-2 pt-2">
                                <a href="${pageContext.request.contextPath}/bill?orderId=${activeOrder.id}" 
                                   class="w-full h-12 rounded-lg bg-[#900b1a] hover:bg-[#780815] text-white flex items-center justify-center gap-2 font-semibold text-[14px] transition-all shadow-md text-decoration-none">
                                    <span class="material-symbols-outlined text-[18px]">receipt_long</span>
                                    <span>Xem hóa đơn điện tử e-Bill</span>
                                </a>
                                <div class="grid grid-cols-2 gap-2">
                                    <a href="${pageContext.request.contextPath}/menu" 
                                       class="h-10 rounded-lg bg-white border border-[#dee9fc] hover:bg-[#eff4ff] text-[#121c2a] flex items-center justify-center gap-1.5 text-[13px] font-semibold transition-colors text-decoration-none">
                                        <span class="material-symbols-outlined text-[18px]">replay</span>
                                        <span>Đặt thêm món</span>
                                    </a>
                                    <button type="button" onclick="alert('Cảm ơn bạn! Đánh giá của bạn giúp Aura phục vụ ngày càng tốt hơn.')"
                                            class="h-10 rounded-lg bg-white border border-[#dee9fc] hover:bg-[#eff4ff] text-[#121c2a] flex items-center justify-center gap-1.5 text-[13px] font-semibold transition-colors cursor-pointer">
                                        <span class="material-symbols-outlined text-[18px]">star</span>
                                        <span>Đánh giá đơn</span>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </c:if>

            <c:if test="${empty activeOrder}">
                <div class="py-16 bg-white rounded-2xl border border-[#dee9fc] text-center p-8 flex flex-col items-center">
                    <span class="material-symbols-outlined text-5xl text-[#555f71] mb-2">inventory_2</span>
                    <h3 class="text-[18px] font-bold text-[#121c2a]">Chưa có đơn hàng nào</h3>
                    <p class="text-[14px] text-[#555f71] mt-1 mb-6">Hãy đặt ngay những ly cà phê đặc sản thơm ngon từ Aura Café nhé!</p>
                    <a href="${pageContext.request.contextPath}/menu" class="px-6 py-2.5 rounded-lg bg-[#900b1a] text-white font-semibold text-[14px] text-decoration-none shadow-md">
                        Xem Menu ngay
                    </a>
                </div>
            </c:if>

        </div>
    </main>

    <script>
        function setTab(tab) {
            document.querySelectorAll('.tab-btn').forEach(b => {
                b.className = "tab-btn px-4 py-2 rounded-lg text-[13px] flex items-center gap-2 shrink-0 shadow-sm cursor-pointer bg-white text-[#555f71] hover:bg-[#e6eeff]";
            });
            const activeBtn = document.getElementById('tab-' + tab);
            if (activeBtn) {
                activeBtn.className = "tab-btn px-4 py-2 rounded-lg text-[13px] flex items-center gap-2 shrink-0 shadow-sm cursor-pointer bg-[#900b1a] text-white font-semibold";
            }
        }

        function filterOrders() {
            const query = document.getElementById('orderSearchInput').value.trim().toLowerCase();
            // Live filter logic
        }
    </script>
</body>
</html>
