<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Aura Café | Xác nhận đơn hàng & Giao nhận</title>

    <!-- Google Fonts & Icons -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet" />

    <!-- Tailwind CSS (Play CDN với cấu hình Aura chuẩn Material 3) -->
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
                        'primary-fixed-dim': '#ffb3af',
                        'on-primary-fixed': '#410005',
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
                        error: '#ba1a1a',
                        'error-container': '#ffdad6',
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
        <jsp:param name="activePage" value="checkout" />
    </jsp:include>

    <main class="w-full">
        <div class="relative w-full max-w-7xl mx-auto px-4 md:px-8 py-6">
            
            <!-- Step Breadcrumb Tracker (1. Giỏ hàng -> 2. Xác nhận đơn & Giao nhận -> 3. Thanh toán) -->
            <div class="w-full mb-8">
                <div class="flex items-center justify-between max-w-2xl mx-auto relative">
                    <div class="absolute left-0 top-1/2 -translate-y-1/2 h-1 w-full bg-[#e6eeff] rounded-full -z-0"></div>
                    <div class="absolute left-0 top-1/2 -translate-y-1/2 h-1 w-1/2 bg-[#b2292e] rounded-full -z-0 transition-all duration-500"></div>

                    <!-- Step 1: Giỏ hàng -->
                    <a href="${pageContext.request.contextPath}/cart" class="flex flex-col items-center gap-1 relative z-10 text-decoration-none group cursor-pointer">
                        <div class="w-10 h-10 rounded-full bg-[#b2292e] text-white flex items-center justify-center shadow-md">
                            <span class="material-symbols-outlined text-[20px]">check</span>
                        </div>
                        <span class="text-[12px] text-[#900b1a] font-semibold">1. Giỏ hàng</span>
                    </a>

                    <!-- Step 2: Xác nhận đơn & Giao nhận (Active) -->
                    <div class="flex flex-col items-center gap-1 relative z-10">
                        <div class="w-10 h-10 rounded-full bg-[#b2292e] text-white ring-4 ring-[#b2292e]/20 flex items-center justify-center shadow-md">
                            <span class="material-symbols-outlined text-[20px]">local_shipping</span>
                        </div>
                        <span class="text-[14px] text-[#900b1a] font-bold">2. Xác nhận đơn & Giao nhận</span>
                    </div>

                    <!-- Step 3: Thanh toán (Pending) -->
                    <div class="flex flex-col items-center gap-1 relative z-10 opacity-60">
                        <div class="w-10 h-10 rounded-full bg-[#dee9fc] text-[#555f71] flex items-center justify-center">
                            <span class="material-symbols-outlined text-[20px]">payments</span>
                        </div>
                        <span class="text-[12px] text-[#555f71]">3. Thanh toán</span>
                    </div>
                </div>
            </div>

            <!-- Main Operational Split Layout -->
            <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
                
                <!-- Left Column: Delivery & Fulfillment Specifics (7 Cols) -->
                <div class="lg:col-span-7 flex flex-col gap-6">
                    
                    <!-- Fulfillment Mode Selector Card -->
                    <div class="bg-white rounded-xl p-6 shadow-sm border border-[#dee9fc]">
                        <div class="flex items-center justify-between mb-4">
                            <div class="flex items-center gap-2">
                                <span class="w-8 h-8 rounded-lg bg-[#e6eeff] flex items-center justify-center text-[#900b1a]">
                                    <span class="material-symbols-outlined text-[20px]">swap_horiz</span>
                                </span>
                                <h2 class="text-[16px] text-[#121c2a] font-semibold">Phương thức nhận hàng</h2>
                            </div>
                            <span class="text-[11px] text-[#555f71] bg-[#e6eeff] px-2.5 py-0.5 rounded-full font-semibold">
                                Bước 2 / 3
                            </span>
                        </div>

                        <!-- Tabs: Giao tận nơi vs Đến lấy tại quầy -->
                        <div class="grid grid-cols-2 p-1.5 bg-[#e6eeff] rounded-lg gap-2">
                            <button type="button" id="btnModeDelivery" onclick="setFulfillmentMode('delivery')"
                                    class="flex items-center justify-center gap-2 py-2.5 px-4 rounded-md text-[14px] transition-all cursor-pointer bg-white text-[#900b1a] shadow-sm font-semibold">
                                <span class="material-symbols-outlined text-[20px]">moped</span>
                                <span>Giao tận nơi (Delivery)</span>
                            </button>
                            <button type="button" id="btnModePickup" onclick="setFulfillmentMode('pickup')"
                                    class="flex items-center justify-center gap-2 py-2.5 px-4 rounded-md text-[14px] transition-all cursor-pointer text-[#555f71] hover:text-[#121c2a]">
                                <span class="material-symbols-outlined text-[20px]">storefront</span>
                                <span>Đến lấy tại quầy (Take-away)</span>
                            </button>
                        </div>
                    </div>

                    <!-- Delivery Address Panel -->
                    <div id="addressPanel" class="bg-white rounded-xl p-6 shadow-sm flex flex-col gap-4 border border-[#dee9fc] transition-opacity">
                        <div class="flex items-center justify-between">
                            <div class="flex items-center gap-2">
                                <span class="w-8 h-8 rounded-lg bg-[#ffdad7] flex items-center justify-center text-[#900b1a]">
                                    <span class="material-symbols-outlined text-[20px]">pin_drop</span>
                                </span>
                                <h3 class="text-[16px] text-[#121c2a] font-semibold">Thông tin người nhận & Địa chỉ</h3>
                            </div>
                        </div>

                        <!-- Form inputs for Recipient & Address -->
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div class="flex flex-col gap-1">
                                <label class="text-[12px] font-medium text-[#59413f]">Họ và tên người nhận *</label>
                                <input type="text" id="recipientName" 
                                       value="${not empty sessionScope.user ? sessionScope.user.fullName : 'Nguyễn Văn An'}"
                                       placeholder="Họ và tên..." 
                                       class="w-full h-10 px-3.5 rounded-lg bg-[#f8f9ff] border border-[#d9e3f6] text-[#121c2a] text-[14px] focus:outline-none focus:border-[#b2292e] focus:ring-2 focus:ring-[#b2292e]/20" />
                            </div>
                            <div class="flex flex-col gap-1">
                                <label class="text-[12px] font-medium text-[#59413f]">Số điện thoại liên hệ *</label>
                                <input type="text" id="recipientPhone" 
                                       value="${not empty sessionScope.user ? sessionScope.user.phone : '0908 123 456'}"
                                       placeholder="Số điện thoại..." 
                                       class="w-full h-10 px-3.5 rounded-lg bg-[#f8f9ff] border border-[#d9e3f6] text-[#121c2a] text-[14px] focus:outline-none focus:border-[#b2292e] focus:ring-2 focus:ring-[#b2292e]/20" />
                            </div>
                        </div>

                        <div class="flex flex-col gap-1">
                            <label class="text-[12px] font-medium text-[#59413f]">Địa chỉ giao hàng chi tiết *</label>
                            <input type="text" id="deliveryAddress" 
                                   value="Tầng 8, Tòa nhà Landmark 81, P. 22, Q. Bình Thạnh, TP. Hồ Chí Minh"
                                   placeholder="Số nhà, tên đường, phường, quận..." 
                                   class="w-full h-10 px-3.5 rounded-lg bg-[#f8f9ff] border border-[#d9e3f6] text-[#121c2a] text-[14px] focus:outline-none focus:border-[#b2292e] focus:ring-2 focus:ring-[#b2292e]/20" />
                        </div>

                        <!-- Store Routing Indicator -->
                        <div class="p-4 rounded-lg bg-[#f8f9ff] flex items-center gap-4 border border-[#dee9fc]">
                            <div class="w-10 h-10 rounded-full bg-[#dee9fc] flex items-center justify-center text-[#900b1a] shrink-0">
                                <span class="material-symbols-outlined text-[22px]">store</span>
                            </div>
                            <div class="flex flex-col flex-1 min-w-0">
                                <div class="flex items-center gap-1.5">
                                    <span class="text-[11px] text-[#6e3900] uppercase font-bold">Chi nhánh điều phối tự động</span>
                                    <span class="w-1 h-1 rounded-full bg-[#555f71]"></span>
                                    <span class="text-[11px] text-[#555f71]">Khoảng cách ~2.1km</span>
                                </div>
                                <span class="text-[14px] text-[#121c2a] font-semibold truncate">
                                    Chi nhánh 01: 28 Phố Tràng Tiền, Hoàn Kiếm, Hà Nội
                                </span>
                                <span class="text-[12px] text-[#59413f]">
                                    Dự kiến pha chế và giao tới bạn trong 25 - 30 phút
                                </span>
                            </div>
                        </div>
                    </div>

                    <!-- Delivery Schedule Time Selection -->
                    <div class="bg-white rounded-xl p-6 shadow-sm flex flex-col gap-4 border border-[#dee9fc]">
                        <div class="flex items-center gap-2">
                            <span class="w-8 h-8 rounded-lg bg-[#e6eeff] flex items-center justify-center text-[#900b1a]">
                                <span class="material-symbols-outlined text-[20px]">schedule</span>
                            </span>
                            <h3 class="text-[16px] text-[#121c2a] font-semibold">Thời gian nhận hàng</h3>
                        </div>

                        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <!-- Option 1: ASAP -->
                            <label onclick="setScheduled(false)" class="flex items-start gap-2.5 p-4 rounded-lg cursor-pointer transition-colors border-2 bg-[#eff4ff] border-[#900b1a]/30" id="labelAsap">
                                <input type="radio" name="delivery_time" id="radioAsap" checked class="mt-1 w-4 h-4 accent-[#900b1a]" />
                                <div class="flex flex-col">
                                    <span class="text-[14px] text-[#121c2a] font-semibold">Giao ngay lập tức</span>
                                    <span class="text-[12px] text-[#59413f]">Ưu tiên Barista pha chế & giao trong 25 - 30 phút</span>
                                </div>
                            </label>

                            <!-- Option 2: Schedule -->
                            <label onclick="setScheduled(true)" class="flex items-start gap-2.5 p-4 rounded-lg cursor-pointer transition-colors border-2 bg-[#f8f9ff] border-transparent hover:bg-[#eff4ff]" id="labelSchedule">
                                <input type="radio" name="delivery_time" id="radioSchedule" class="mt-1 w-4 h-4 accent-[#900b1a]" />
                                <div class="flex flex-col">
                                    <span class="text-[14px] text-[#121c2a] font-semibold">Hẹn giờ giao cụ thể</span>
                                    <span class="text-[12px] text-[#59413f]">Chọn khung giờ tiện nhất trong ngày hôm nay</span>
                                </div>
                            </label>
                        </div>

                        <!-- Schedule time slots -->
                        <div id="scheduleSlotsContainer" class="hidden flex-col gap-2 pt-1">
                            <span class="text-[12px] text-[#59413f] font-medium">Chọn khung giờ mong muốn:</span>
                            <div class="grid grid-cols-3 gap-2">
                                <button type="button" onclick="selectSlot(this, '10:30 - 11:00')" class="slot-btn py-2 px-3 rounded text-[12px] font-semibold transition-colors bg-[#900b1a] text-white">10:30 - 11:00</button>
                                <button type="button" onclick="selectSlot(this, '14:00 - 14:30')" class="slot-btn py-2 px-3 rounded text-[12px] font-semibold transition-colors bg-[#e6eeff] text-[#121c2a] hover:bg-[#dee9fc]">14:00 - 14:30</button>
                                <button type="button" onclick="selectSlot(this, '16:30 - 17:00')" class="slot-btn py-2 px-3 rounded text-[12px] font-semibold transition-colors bg-[#e6eeff] text-[#121c2a] hover:bg-[#dee9fc]">16:30 - 17:00</button>
                            </div>
                        </div>
                    </div>

                    <!-- Delivery Notes -->
                    <div class="bg-white rounded-xl p-6 shadow-sm flex flex-col gap-4 border border-[#dee9fc]">
                        <div class="flex items-center gap-2">
                            <span class="w-8 h-8 rounded-lg bg-[#e6eeff] flex items-center justify-center text-[#900b1a]">
                                <span class="material-symbols-outlined text-[20px]">edit_note</span>
                            </span>
                            <h3 class="text-[16px] text-[#121c2a] font-semibold">Ghi chú giao hàng</h3>
                        </div>
                        <textarea rows="3" id="orderNote" 
                                  placeholder="Ví dụ: Gửi lễ tân tầng trệt, vui lòng gọi điện trước khi đến 5 phút..." 
                                  class="w-full p-3.5 rounded-lg bg-[#f8f9ff] border border-[#d9e3f6] text-[#121c2a] placeholder:text-[#555f71] text-[14px] focus:outline-none focus:ring-2 focus:ring-[#b2292e]/20 focus:border-[#b2292e] resize-none">Gửi bảo vệ tầng hầm B1 gọi điện thoại trước khi đến nhận hàng.</textarea>
                    </div>

                    <!-- Corporate VAT Invoice -->
                    <div class="bg-white rounded-xl p-6 shadow-sm flex flex-col gap-4 border border-[#dee9fc]">
                        <div class="flex items-center justify-between">
                            <label class="flex items-center gap-2.5 cursor-pointer select-none">
                                <input type="checkbox" id="chkIsVat" onchange="toggleVatSection()" class="w-4 h-4 accent-[#900b1a] rounded cursor-pointer" />
                                <span class="text-[16px] text-[#121c2a] font-semibold">Yêu cầu xuất hoá đơn điện tử VAT (Doanh nghiệp)</span>
                            </label>
                            <span class="text-[11px] text-[#555f71] bg-[#f8f9ff] px-2 py-0.5 rounded font-semibold border border-[#d9e3f6]">
                                e-Invoice
                            </span>
                        </div>

                        <div id="vatSection" class="hidden flex-col gap-4 pt-2">
                            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div class="flex flex-col gap-1">
                                    <label class="text-[12px] font-medium text-[#59413f]">Tên công ty / Doanh nghiệp *</label>
                                    <input type="text" id="vatCompany" placeholder="Công ty TNHH Aura Specialty Coffee..." class="w-full h-10 px-3.5 rounded-lg bg-[#f8f9ff] border border-[#d9e3f6] text-[#121c2a] text-[14px] focus:outline-none focus:border-[#b2292e]" />
                                </div>
                                <div class="flex flex-col gap-1">
                                    <label class="text-[12px] font-medium text-[#59413f]">Mã số thuế (MST) *</label>
                                    <input type="text" id="vatTaxId" placeholder="0312345678" class="w-full h-10 px-3.5 rounded-lg bg-[#f8f9ff] border border-[#d9e3f6] text-[#121c2a] text-[14px] focus:outline-none focus:border-[#b2292e]" />
                                </div>
                            </div>
                            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div class="flex flex-col gap-1">
                                    <label class="text-[12px] font-medium text-[#59413f]">Địa chỉ trụ sở theo ĐKKD *</label>
                                    <input type="text" id="vatAddress" placeholder="Số 123 Đường Nam Kỳ Khởi Nghĩa..." class="w-full h-10 px-3.5 rounded-lg bg-[#f8f9ff] border border-[#d9e3f6] text-[#121c2a] text-[14px] focus:outline-none focus:border-[#b2292e]" />
                                </div>
                                <div class="flex flex-col gap-1">
                                    <label class="text-[12px] font-medium text-[#59413f]">Email nhận hoá đơn điện tử (PDF/XML) *</label>
                                    <input type="email" id="vatEmail" placeholder="ketoan@doanhnghiep.vn" class="w-full h-10 px-3.5 rounded-lg bg-[#f8f9ff] border border-[#d9e3f6] text-[#121c2a] text-[14px] focus:outline-none focus:border-[#b2292e]" />
                                </div>
                            </div>
                            <p class="text-[12px] text-[#555f71] italic">
                                Hóa đơn điện tử sẽ được khởi tạo và gửi tự động qua email sau khi đơn hàng được ghi nhận thanh toán hoàn tất.
                            </p>
                        </div>
                    </div>
                </div>

                <!-- Right Column: Condensed Order Summary & Final Confirmation Trigger (5 Cols) -->
                <div class="lg:col-span-5 flex flex-col gap-6 lg:sticky lg:top-24">
                    <div class="bg-white rounded-xl p-6 shadow-sm flex flex-col gap-6 border border-[#dee9fc]">
                        
                        <!-- Summary Header -->
                        <div class="flex items-center justify-between pb-3 border-b border-[#dee9fc]">
                            <div class="flex items-center gap-2">
                                <span class="material-symbols-outlined text-[#900b1a] text-[22px]">receipt_long</span>
                                <h3 class="text-[16px] text-[#121c2a] font-semibold">
                                    Kiểm tra đơn hàng (<span id="summaryItemCount">0</span> món)
                                </h3>
                            </div>
                            <a href="${pageContext.request.contextPath}/cart" class="text-[12px] text-[#900b1a] hover:underline font-semibold text-decoration-none">
                                Sửa giỏ
                            </a>
                        </div>

                        <!-- Item Mini List -->
                        <div id="checkoutMiniList" class="flex flex-col gap-3 divide-y divide-[#e6eeff] max-h-80 overflow-y-auto pr-1">
                            <!-- Render bằng JS từ localStorage -->
                        </div>

                        <!-- Pricing Breakdown -->
                        <div class="flex flex-col gap-2.5 pt-4 border-t border-[#e6eeff]">
                            <div class="flex justify-between items-center text-[#59413f] text-[14px]">
                                <span>Tổng tiền món:</span>
                                <span id="summarySubtotal" class="font-tabular-data font-semibold text-[#121c2a]">0đ</span>
                            </div>

                            <div class="flex justify-between items-center text-[#59413f] text-[14px]">
                                <div class="flex items-center gap-1">
                                    <span>Phí giao hàng</span>
                                    <span id="shippingLabel" class="text-[11px] text-[#555f71]">(2.1km)</span>
                                </div>
                                <span id="summaryShipping" class="font-tabular-data font-semibold text-[#121c2a]">18.000đ</span>
                            </div>

                            <div id="voucherRow" class="hidden justify-between items-center text-[14px] text-[#900b1a] font-medium">
                                <div class="flex items-center gap-1">
                                    <span class="material-symbols-outlined text-[16px]">sell</span>
                                    <span>Voucher giảm giá (<span id="voucherCodeText">WELCOME50K</span>):</span>
                                </div>
                                <span id="summaryVoucher" class="font-tabular-data font-bold">-50.000đ</span>
                            </div>

                            <!-- Total Final Row -->
                            <div class="flex justify-between items-baseline pt-3 border-t border-[#d9e3f6] mt-1">
                                <div class="flex flex-col">
                                    <span class="text-[16px] text-[#121c2a] font-bold">Tổng thanh toán</span>
                                    <span class="text-[12px] text-[#555f71]">Đã bao gồm thuế GTGT 8%</span>
                                </div>
                                <div class="text-right">
                                    <span id="summaryGrandTotal" class="text-[28px] font-bold text-[#900b1a] font-tabular-data tracking-tight">
                                        0đ
                                    </span>
                                </div>
                            </div>
                        </div>

                        <!-- Call to Action Buttons -->
                        <div class="flex flex-col gap-2.5 pt-1">
                            <button type="button" id="btnConfirmToPayment" onclick="handleProceedToPayment()"
                                    class="w-full h-14 rounded-lg bg-[#b2292e] text-white flex items-center justify-center gap-2 shadow-md hover:bg-[#900b1a] transition-all text-[16px] font-semibold tracking-wide cursor-pointer">
                                <span>Xác nhận đơn & Đến bước Thanh toán</span>
                                <span class="material-symbols-outlined text-[20px]">arrow_forward</span>
                            </button>
                            <a href="${pageContext.request.contextPath}/cart"
                               class="w-full h-11 rounded-lg bg-[#f8f9ff] hover:bg-[#e6eeff] flex items-center justify-center gap-1.5 text-[#555f71] hover:text-[#121c2a] transition-colors text-[14px] font-semibold text-decoration-none">
                                <span class="material-symbols-outlined text-[18px]">arrow_back</span>
                                <span>Quay lại giỏ hàng</span>
                            </a>
                        </div>

                        <!-- Guarantee Badges -->
                        <div class="grid grid-cols-2 gap-2 pt-3 border-t border-[#dee9fc]">
                            <div class="flex items-center gap-1 text-[#59413f]">
                                <span class="material-symbols-outlined text-[#900b1a] text-[18px]">verified_user</span>
                                <span class="text-[11px] font-medium">Bảo đảm vị ngon 100%</span>
                            </div>
                            <div class="flex items-center gap-1 text-[#59413f]">
                                <span class="material-symbols-outlined text-[#900b1a] text-[18px]">timer</span>
                                <span class="text-[11px] font-medium">Giao chuẩn 30 phút</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <script>
        let currentMode = 'delivery';
        let isScheduled = false;
        let selectedTimeSlot = '10:30 - 11:00';
        let calculatedSubtotal = 0;
        let calculatedShipping = 18000;
        let calculatedDiscount = 0;

        function getCart() {
            try {
                return JSON.parse(localStorage.getItem('aura_cart')) || [];
            } catch (e) {
                return [];
            }
        }

        function renderCheckout() {
            const cart = getCart();
            const container = document.getElementById('checkoutMiniList');
            const itemCountElem = document.getElementById('summaryItemCount');

            if (!cart || cart.length === 0) {
                container.innerHTML = '<p class="text-center text-[#555f71] py-4 text-[13px]">Giỏ hàng đang trống!</p>';
                itemCountElem.innerText = '0';
                return;
            }

            let html = '';
            let totalQty = 0;
            calculatedSubtotal = 0;

            cart.forEach(item => {
                const qty = item.quantity || 1;
                const price = item.unitPrice || item.price || 45000;
                const itemTotal = qty * price;
                calculatedSubtotal += itemTotal;
                totalQty += qty;

                const sizeText = item.size ? ('Size ' + item.size) : 'Tiêu chuẩn';

                html += `
                    <div class="flex items-center gap-3 pt-2">
                        <div class="w-12 h-12 rounded-lg bg-[#e6eeff] overflow-hidden shrink-0 border border-[#d9e3f6]">
                            <img src="\${item.imageUrl || item.image || 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600'}"
                                 alt="\${item.name}" class="w-full h-full object-cover" />
                        </div>
                        <div class="flex-1 min-w-0">
                            <div class="flex items-center justify-between">
                                <h4 class="text-[13px] text-[#121c2a] font-semibold truncate">\${item.name}</h4>
                                <span class="text-[13px] font-semibold text-[#121c2a] font-tabular-data">\${itemTotal.toLocaleString('vi-VN')}đ</span>
                            </div>
                            <div class="flex items-center gap-1.5 mt-0.5">
                                <span class="text-[11px] text-[#900b1a] font-bold">\${qty}x</span>
                                <span class="text-[11px] text-[#555f71] truncate">\${sizeText}</span>
                            </div>
                        </div>
                    </div>
                `;
            });

            container.innerHTML = html;
            itemCountElem.innerText = totalQty;
            updateTotals();
        }

        function setFulfillmentMode(mode) {
            currentMode = mode;
            const btnDeliv = document.getElementById('btnModeDelivery');
            const btnPick = document.getElementById('btnModePickup');
            const addrPanel = document.getElementById('addressPanel');
            const shipLabel = document.getElementById('shippingLabel');

            if (mode === 'delivery') {
                btnDeliv.className = "flex items-center justify-center gap-2 py-2.5 px-4 rounded-md text-[14px] transition-all cursor-pointer bg-white text-[#900b1a] shadow-sm font-semibold";
                btnPick.className = "flex items-center justify-center gap-2 py-2.5 px-4 rounded-md text-[14px] transition-all cursor-pointer text-[#555f71] hover:text-[#121c2a]";
                addrPanel.classList.remove('opacity-40', 'pointer-events-none');
                calculatedShipping = calculatedSubtotal >= 150000 ? 0 : 18000;
                shipLabel.innerText = "(2.1km)";
            } else {
                btnPick.className = "flex items-center justify-center gap-2 py-2.5 px-4 rounded-md text-[14px] transition-all cursor-pointer bg-white text-[#900b1a] shadow-sm font-semibold";
                btnDeliv.className = "flex items-center justify-center gap-2 py-2.5 px-4 rounded-md text-[14px] transition-all cursor-pointer text-[#555f71] hover:text-[#121c2a]";
                addrPanel.classList.add('opacity-40', 'pointer-events-none');
                calculatedShipping = 0;
                shipLabel.innerText = "(Tại quầy - 28 Tràng Tiền)";
            }
            updateTotals();
        }

        function setScheduled(schedule) {
            isScheduled = schedule;
            const container = document.getElementById('scheduleSlotsContainer');
            const lblAsap = document.getElementById('labelAsap');
            const lblSchedule = document.getElementById('labelSchedule');
            const radioAsap = document.getElementById('radioAsap');
            const radioSchedule = document.getElementById('radioSchedule');

            if (schedule) {
                container.classList.remove('hidden');
                container.classList.add('flex');
                lblSchedule.className = "flex items-start gap-2.5 p-4 rounded-lg cursor-pointer transition-colors border-2 bg-[#eff4ff] border-[#900b1a]/30";
                lblAsap.className = "flex items-start gap-2.5 p-4 rounded-lg cursor-pointer transition-colors border-2 bg-[#f8f9ff] border-transparent hover:bg-[#eff4ff]";
                radioSchedule.checked = true;
            } else {
                container.classList.add('hidden');
                container.classList.remove('flex');
                lblAsap.className = "flex items-start gap-2.5 p-4 rounded-lg cursor-pointer transition-colors border-2 bg-[#eff4ff] border-[#900b1a]/30";
                lblSchedule.className = "flex items-start gap-2.5 p-4 rounded-lg cursor-pointer transition-colors border-2 bg-[#f8f9ff] border-transparent hover:bg-[#eff4ff]";
                radioAsap.checked = true;
            }
        }

        function selectSlot(btn, slot) {
            selectedTimeSlot = slot;
            document.querySelectorAll('.slot-btn').forEach(b => {
                b.className = "slot-btn py-2 px-3 rounded text-[12px] font-semibold transition-colors bg-[#e6eeff] text-[#121c2a] hover:bg-[#dee9fc]";
            });
            btn.className = "slot-btn py-2 px-3 rounded text-[12px] font-semibold transition-colors bg-[#900b1a] text-white";
        }

        function toggleVatSection() {
            const chk = document.getElementById('chkIsVat');
            const sec = document.getElementById('vatSection');
            if (chk.checked) {
                sec.classList.remove('hidden');
                sec.classList.add('flex');
            } else {
                sec.classList.add('hidden');
                sec.classList.remove('flex');
            }
        }

        function updateTotals() {
            document.getElementById('summarySubtotal').innerText = calculatedSubtotal.toLocaleString('vi-VN') + 'đ';
            document.getElementById('summaryShipping').innerText = calculatedShipping === 0 ? '0đ (Miễn phí)' : calculatedShipping.toLocaleString('vi-VN') + 'đ';

            const grandTotal = Math.max(0, calculatedSubtotal + calculatedShipping - calculatedDiscount);
            document.getElementById('summaryGrandTotal').innerText = grandTotal.toLocaleString('vi-VN') + 'đ';
        }

        function handleProceedToPayment() {
            const cart = getCart();
            if (!cart || cart.length === 0) {
                alert('Giỏ hàng của bạn đang trống! Vui lòng chọn món trước.');
                window.location.href = '${pageContext.request.contextPath}/menu';
                return;
            }

            const recipientName = document.getElementById('recipientName').value.trim();
            const recipientPhone = document.getElementById('recipientPhone').value.trim();
            const deliveryAddress = currentMode === 'pickup' 
                ? 'Nhận trực tiếp tại quầy: 28 Phố Tràng Tiền, Hoàn Kiếm, Hà Nội' 
                : document.getElementById('deliveryAddress').value.trim();
            let note = document.getElementById('orderNote').value.trim();

            if (isScheduled) {
                note = '[Hẹn giờ giao: ' + selectedTimeSlot + '] ' + note;
            }

            if (!recipientName || !recipientPhone) {
                alert('Vui lòng nhập họ tên và số điện thoại người nhận!');
                return;
            }

            const btn = document.getElementById('btnConfirmToPayment');
            btn.disabled = true;
            btn.innerHTML = '<span class="material-symbols-outlined animate-spin text-[20px]">progress_activity</span> Đang tạo đơn hàng...';

            const payload = {
                items: cart,
                recipientName: recipientName,
                recipientPhone: recipientPhone,
                deliveryAddress: deliveryAddress,
                orderType: currentMode === 'pickup' ? 'TAKEAWAY' : 'ONLINE',
                orderNote: note,
                deliveryFee: calculatedShipping
            };

            fetch('${pageContext.request.contextPath}/checkout', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json;charset=UTF-8'
                },
                body: JSON.stringify(payload)
            })
            .then(res => res.json())
            .then(data => {
                if (data.success && data.redirectUrl) {
                    window.location.href = data.redirectUrl;
                } else {
                    alert('Lỗi tạo đơn hàng: ' + (data.error || 'Vui lòng thử lại!'));
                    btn.disabled = false;
                    btn.innerHTML = '<span>Xác nhận đơn & Đến bước Thanh toán</span><span class="material-symbols-outlined text-[20px]">arrow_forward</span>';
                }
            })
            .catch(err => {
                console.error(err);
                alert('Có lỗi kết nối máy chủ!');
                btn.disabled = false;
                btn.innerHTML = '<span>Xác nhận đơn & Đến bước Thanh toán</span><span class="material-symbols-outlined text-[20px]">arrow_forward</span>';
            });
        }

        document.addEventListener('DOMContentLoaded', renderCheckout);
    </script>
</body>
</html>
