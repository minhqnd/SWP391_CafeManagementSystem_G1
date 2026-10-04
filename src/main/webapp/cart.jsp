<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Aura Café | Giỏ hàng của bạn</title>

    <!-- Google Fonts & Icons -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet" />

    <!-- Tailwind CSS (Play CDN) -->
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        primary: '#900b1a',
                        'primary-container': '#b2292e',
                        'on-primary': '#ffffff',
                        secondary: '#555f71',
                        'secondary-container': '#d6e0f6',
                        tertiary: '#6e3900',
                        'tertiary-container': '#904d00',
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
        .text-display-lg { font-size: 32px; line-height: 40px; font-weight: 700; }
        .text-headline-sm { font-size: 16px; line-height: 24px; font-weight: 600; }
        .text-body-md { font-size: 14px; line-height: 20px; }
        .text-body-sm { font-size: 12px; line-height: 16px; }
        .text-label-lg { font-size: 14px; line-height: 20px; font-weight: 600; }
        .text-label-md { font-size: 12px; line-height: 16px; }
        .text-label-sm { font-size: 11px; line-height: 16px; }
        .font-tabular-data { font-variant-numeric: tabular-nums; }
    </style>
</head>

<body class="min-h-screen bg-[#f8f9ff] flex flex-col">

    <!-- Header Component -->
    <jsp:include page="/components/header.jsp">
        <jsp:param name="activePage" value="cart" />
    </jsp:include>

    <div class="flex flex-col w-full flex-grow">
        <div class="w-full max-w-7xl mx-auto px-margin-lg py-space-lg">

            <!-- Breadcrumb & Header -->
            <section class="flex flex-col md:flex-row md:items-end justify-between gap-space-md mb-space-lg">
                <div class="flex flex-col gap-space-xs">
                    <nav aria-label="Breadcrumb" class="flex items-center gap-space-xs text-secondary font-label-md text-label-md">
                        <a href="${pageContext.request.contextPath}/" class="hover:text-primary transition-colors flex items-center gap-1">
                            <span class="material-symbols-outlined text-[16px]">home</span>
                            <span>Trang chủ</span>
                        </a>
                        <span class="material-symbols-outlined text-[14px] text-secondary">chevron_right</span>
                        <a href="${pageContext.request.contextPath}/menu" class="hover:text-primary transition-colors">Menu</a>
                        <span class="material-symbols-outlined text-[14px] text-secondary">chevron_right</span>
                        <span class="text-on-surface font-semibold">Giỏ hàng của bạn</span>
                    </nav>
                    <div class="flex items-baseline gap-space-sm mt-1">
                        <h1 class="font-display-lg text-display-lg text-on-surface tracking-tight font-bold">
                            Giỏ hàng của bạn
                        </h1>
                        <span id="cartCountBadge" class="font-label-lg text-label-lg px-2.5 py-0.5 rounded-full bg-surface-container-high text-primary font-bold">
                            0 món
                        </span>
                    </div>
                </div>

                <!-- Free shipping progress -->
                <div class="bg-surface-container-lowest p-space-md rounded-xl shadow-sm max-w-sm w-full border border-surface-container-high">
                    <div class="flex items-center justify-between font-label-sm text-label-sm mb-1.5">
                        <span id="freeshipText" class="flex items-center gap-1 text-primary font-semibold">
                            <span class="material-symbols-outlined text-[16px]">local_shipping</span>
                            Đơn hàng từ 150.000đ được FREESHIP
                        </span>
                        <span id="freeshipPercent" class="text-on-surface font-tabular-data font-bold">
                            0%
                        </span>
                    </div>
                    <div class="w-full h-1.5 bg-surface-container-highest rounded-full overflow-hidden">
                        <div id="freeshipBar" class="h-full bg-primary transition-all duration-300" style="width: 0%;"></div>
                    </div>
                </div>
            </section>

            <!-- Main Cart Grid -->
            <div id="cartContentGrid" class="grid grid-cols-1 lg:grid-cols-12 gap-gutter-lg items-start">

                <!-- Left Column: Items List -->
                <div class="lg:col-span-8 flex flex-col gap-space-md">
                    <div class="bg-surface-container-lowest rounded-xl shadow-sm border border-surface-container-high p-space-md md:p-space-lg flex flex-col gap-space-md">
                        <div class="flex items-center justify-between pb-space-sm border-b border-surface-container-high">
                            <span class="font-headline-sm font-semibold text-on-surface">Danh sách món ăn & đồ uống</span>
                            <button type="button" onclick="clearCart()" class="text-xs text-red-600 hover:underline font-medium">
                                Xóa tất cả
                            </button>
                        </div>

                        <!-- Danh sách món render từ localStorage -->
                        <div id="cartItemsContainer" class="flex flex-col divide-y divide-surface-container"></div>
                    </div>
                </div>

                <!-- Right Column: Order Summary & Checkout Action -->
                <div class="lg:col-span-4 flex flex-col gap-space-md">
                    <div class="bg-surface-container-lowest rounded-xl shadow-sm border border-surface-container-high p-space-md md:p-space-lg flex flex-col gap-space-md">
                        <h2 class="font-headline-sm font-semibold text-on-surface">Tóm tắt đơn hàng</h2>

                        <div class="flex flex-col gap-2.5 font-body-md text-body-md">
                            <div class="flex justify-between items-center text-on-surface-variant">
                                <span>Tạm tính (<span id="summaryItemCount">0</span> món)</span>
                                <span id="summarySubtotal" class="font-tabular-data font-semibold text-on-surface">0đ</span>
                            </div>
                            <div class="flex justify-between items-center text-on-surface-variant">
                                <span>Phí vận chuyển</span>
                                <span id="summaryShipping" class="font-tabular-data font-semibold text-on-surface">18.000đ</span>
                            </div>
                            <div class="flex justify-between items-center text-tertiary font-medium">
                                <span class="flex items-center gap-1">
                                    <span class="material-symbols-outlined text-[16px]">stars</span>
                                    <span>Tích luỹ Aura Rewards</span>
                                </span>
                                <span id="summaryPoints" class="font-tabular-data font-bold text-tertiary-container">+0 điểm</span>
                            </div>
                        </div>

                        <div class="w-full h-px bg-surface-container-highest my-1"></div>

                        <div class="flex items-baseline justify-between pt-1">
                            <div class="flex flex-col">
                                <span class="font-headline-sm text-on-surface font-bold">Tổng thanh toán</span>
                                <span class="font-label-sm text-secondary">Đã bao gồm VAT & phí dịch vụ</span>
                            </div>
                            <div class="text-right">
                                <span id="summaryGrandTotal" class="font-display-lg text-primary font-tabular-data tracking-tight font-bold">0đ</span>
                            </div>
                        </div>

                        <!-- NÚT TIẾN HÀNH THANH TOÁN (Gửi dữ liệu lên Backend tạo đơn và chuyển sang /payment) -->
                        <button type="button" id="btnCheckout" onclick="proceedToPayment()" class="w-full h-12 bg-primary hover:bg-[#780815] text-white rounded-xl font-label-lg flex items-center justify-center gap-2 shadow-md transition-all cursor-pointer font-semibold mt-2">
                            <span>Tiến hành thanh toán</span>
                            <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
                        </button>
                    </div>

                    <!-- Cam kết dịch vụ -->
                    <div class="p-space-md rounded-xl bg-surface-bright border border-surface-container-high flex flex-col gap-2.5 text-xs text-on-surface-variant">
                        <div class="flex items-start gap-2">
                            <span class="material-symbols-outlined text-primary text-[18px] shrink-0">verified_user</span>
                            <span><strong>Cam kết hoàn hảo:</strong> Miễn phí 1 đổi 1 ngay lập tức nếu đổ vỡ hoặc sai công thức.</span>
                        </div>
                        <div class="flex items-start gap-2">
                            <span class="material-symbols-outlined text-primary text-[18px] shrink-0">thermostat</span>
                            <span><strong>Giữ nhiệt chuẩn gu:</strong> Đóng gói túi nhôm cách nhiệt chuyên dụng, đá tách riêng theo yêu cầu.</span>
                        </div>
                    </div>
                </div>

            </div>

            <!-- Empty Cart State (Ẩn khi có món) -->
            <div id="cartEmptyState" class="hidden flex-col items-center justify-center py-16 bg-white rounded-2xl border border-slate-200 text-center p-8">
                <div class="w-20 h-20 rounded-full bg-red-50 text-primary flex items-center justify-center mb-4">
                    <span class="material-symbols-outlined text-4xl">shopping_cart</span>
                </div>
                <h3 class="text-xl font-bold text-slate-800 mb-2">Giỏ hàng của bạn đang trống</h3>
                <p class="text-slate-500 max-w-md mb-6">Hãy khám phá thực đơn thức uống và bánh ngọt tuyệt hảo của Aura Café nhé!</p>
                <a href="${pageContext.request.contextPath}/menu" class="px-6 py-3 bg-primary hover:bg-[#780815] text-white font-semibold rounded-xl transition-all shadow-md">
                    Khám phá Menu ngay
                </a>
            </div>

        </div>
    </div>

    <!-- Script tương tác Giỏ hàng & Checkout -->
    <script>
        function getCart() {
            try {
                return JSON.parse(localStorage.getItem('aura_cart') || '[]');
            } catch (e) {
                return [];
            }
        }

        function saveCart(cart) {
            localStorage.setItem('aura_cart', JSON.stringify(cart));
            renderCart();
            // Cập nhật badge header
            const headerBadge = document.getElementById('headerCartBadge');
            if (headerBadge) {
                const total = cart.reduce((sum, item) => sum + (item.quantity || 1), 0);
                headerBadge.innerText = total;
            }
        }

        function renderCart() {
            const cart = getCart();
            const container = document.getElementById('cartItemsContainer');
            const grid = document.getElementById('cartContentGrid');
            const empty = document.getElementById('cartEmptyState');
            const countBadge = document.getElementById('cartCountBadge');
            const summaryCount = document.getElementById('summaryItemCount');
            const summarySubtotal = document.getElementById('summarySubtotal');
            const summaryShipping = document.getElementById('summaryShipping');
            const summaryPoints = document.getElementById('summaryPoints');
            const summaryGrandTotal = document.getElementById('summaryGrandTotal');
            const freeshipText = document.getElementById('freeshipText');
            const freeshipPercent = document.getElementById('freeshipPercent');
            const freeshipBar = document.getElementById('freeshipBar');

            if (!cart || cart.length === 0) {
                grid.classList.add('hidden');
                empty.classList.remove('hidden');
                empty.classList.add('flex');
                if (countBadge) countBadge.innerText = '0 món';
                return;
            }

            grid.classList.remove('hidden');
            empty.classList.add('hidden');
            empty.classList.remove('flex');

            let totalItems = 0;
            let subtotal = 0;
            let html = '';

            cart.forEach((item, index) => {
                const qty = item.quantity || 1;
                const unitPrice = item.unitPrice || item.price || 45000;
                const itemTotal = unitPrice * qty;
                totalItems += qty;
                subtotal += itemTotal;

                const toppingsText = (item.toppings && item.toppings.length > 0) ? item.toppings.join(', ') : '';
                const sizeText = item.size ? ('Size ' + item.size) : 'Tiêu chuẩn';

                html += `
                    <div class="py-4 flex items-center justify-between gap-4">
                        <div class="flex items-center gap-4 min-w-0">
                            <img src="\${item.imageUrl || 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600'}" class="w-16 h-16 rounded-xl object-cover border border-slate-200 shrink-0" onerror="this.src='https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600'"/>
                            <div class="min-w-0">
                                <h4 class="font-bold text-slate-800 text-sm md:text-base truncate">\${item.name}</h4>
                                <div class="flex items-center gap-2 mt-1">
                                    <span class="px-2 py-0.5 rounded bg-red-100 text-primary text-xs font-semibold">\${sizeText}</span>
                                    \${toppingsText ? '<span class="text-xs text-slate-500 truncate">+ ' + toppingsText + '</span>' : ''}
                                </div>
                                <div class="text-primary font-bold text-sm mt-1">\${itemTotal.toLocaleString('vi-VN')}đ</div>
                            </div>
                        </div>

                        <div class="flex items-center gap-3 shrink-0">
                            <div class="flex items-center border border-slate-200 rounded-lg bg-white px-2 py-1 shadow-sm">
                                <button type="button" onclick="updateQty(\${index}, -1)" class="w-6 h-6 flex items-center justify-center font-bold text-slate-600 hover:text-primary">-</button>
                                <span class="px-2 font-bold text-sm min-w-[24px] text-center">\${qty}</span>
                                <button type="button" onclick="updateQty(\${index}, 1)" class="w-6 h-6 flex items-center justify-center font-bold text-slate-600 hover:text-primary">+</button>
                            </div>
                            <button type="button" onclick="removeItem(\${index})" class="text-slate-400 hover:text-red-600 p-1.5 rounded-lg hover:bg-red-50 transition-colors" title="Xóa món">
                                <span class="material-symbols-outlined text-[20px]">delete</span>
                            </button>
                        </div>
                    </div>
                `;
            });

            container.innerHTML = html;
            countBadge.innerText = totalItems + ' món';
            summaryCount.innerText = totalItems;
            summarySubtotal.innerText = subtotal.toLocaleString('vi-VN') + 'đ';

            // Phí ship: freeship nếu >= 150.000đ
            const shipping = subtotal >= 150000 ? 0 : 18000;
            summaryShipping.innerText = shipping === 0 ? 'MIỄN PHÍ' : '18.000đ';
            const grandTotal = subtotal + shipping;
            summaryGrandTotal.innerText = grandTotal.toLocaleString('vi-VN') + 'đ';

            const points = Math.floor(grandTotal / 10000);
            summaryPoints.innerText = '+' + points + ' điểm';

            // Thanh Freeship
            const percent = Math.min(100, Math.round((subtotal / 150000) * 100));
            freeshipPercent.innerText = percent + '%';
            freeshipBar.style.width = percent + '%';
            if (percent >= 100) {
                freeshipText.innerHTML = '<span class="material-symbols-outlined text-[16px]">local_shipping</span> Đơn hàng đủ điều kiện FREESHIP!';
            } else {
                const diff = (150000 - subtotal).toLocaleString('vi-VN');
                freeshipText.innerHTML = '<span class="material-symbols-outlined text-[16px]">local_shipping</span> Thêm ' + diff + 'đ để nhận FREESHIP';
            }
        }

        function updateQty(index, delta) {
            const cart = getCart();
            if (!cart[index]) return;
            cart[index].quantity = (cart[index].quantity || 1) + delta;
            if (cart[index].quantity <= 0) {
                cart.splice(index, 1);
            }
            saveCart(cart);
        }

        function removeItem(index) {
            const cart = getCart();
            cart.splice(index, 1);
            saveCart(cart);
        }

        function clearCart() {
            if (confirm('Bạn có chắc chắn muốn xóa toàn bộ giỏ hàng?')) {
                saveCart([]);
            }
        }

        // TIẾN HÀNH ĐẶT HÀNG: Chuyển sang bước 2 (Xác nhận đơn & Giao nhận - Checkout)
        function proceedToPayment() {
            const cart = getCart();
            if (!cart || cart.length === 0) {
                alert('Giỏ hàng của bạn đang trống! Vui lòng chọn món trước.');
                window.location.href = '${pageContext.request.contextPath}/menu';
                return;
            }
            window.location.href = '${pageContext.request.contextPath}/checkout';
        }

        document.addEventListener('DOMContentLoaded', renderCart);
    </script>

</body>
</html>
