<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!-- ========================================================
     SHARED PRODUCT CUSTOMIZATION MODAL & CART TOAST
     ======================================================== -->

<!-- Product Customization Modal -->
<div class="modal fade" id="productDetailModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content border-0 shadow-lg position-relative"
            style="border-radius: 1.5rem; overflow: hidden;">
            <!-- Close Button -->
            <button type="button" class="btn-close position-absolute top-0 end-0 m-3 shadow-none"
                style="z-index: 1060; cursor: pointer; padding: 0.75rem;" data-bs-dismiss="modal"
                aria-label="Close" onclick="closeProductDetailModal()"></button>
            <div class="modal-body p-4 pt-3">
                <div class="row g-4 align-items-center">
                    <!-- Left: Image -->
                    <div class="col-md-5">
                        <img id="modalProductImg" src="" alt="Product"
                            class="modal-product-img shadow-sm">
                    </div>

                    <!-- Right: Details & Options -->
                    <div class="col-md-7 d-flex flex-column">
                        <span class="text-uppercase fw-bold text-danger"
                            style="font-size: 0.75rem; letter-spacing: 0.5px;"
                            id="modalProductCategory">Danh mục</span>
                        <h2 class="fs-4 fw-bold mb-1" id="modalProductName">Tên món</h2>
                        <p class="text-muted small mb-3" id="modalProductDesc">Mô tả sản phẩm</p>

                        <div class="d-flex align-items-baseline gap-2 mb-3 pb-2 border-bottom">
                            <span class="fs-3 fw-bold text-danger" id="modalProductTotalPrice">0đ</span>
                            <span class="text-muted small">/ Giá tiêu chuẩn</span>
                        </div>

                        <!-- Sizes -->
                        <div class="mb-3" id="sizeOptionsContainer">
                            <label class="form-label fw-bold small text-muted mb-2">CHỌN SIZE (KÍCH CỠ):</label>
                            <div class="d-flex gap-2 flex-wrap" id="sizeList"></div>
                        </div>

                        <!-- Toppings -->
                        <div class="mb-3" id="toppingOptionsContainer">
                            <label class="form-label fw-bold small text-muted mb-2">TOPPING THÊM:</label>
                            <div class="d-flex flex-column gap-2" id="toppingList"></div>
                        </div>

                        <!-- Quantity & Action -->
                        <div class="mt-3 pt-3 border-top d-flex align-items-center justify-content-between gap-3">
                            <div class="d-flex align-items-center gap-2 border rounded-pill px-2 py-1">
                                <button type="button" class="qty-control-btn border-0"
                                    onclick="adjustModalQty(-1)">-</button>
                                <span id="modalProductQty" class="fw-bold px-2"
                                    style="min-width: 24px; text-align: center;">1</span>
                                <button type="button" class="qty-control-btn border-0"
                                    onclick="adjustModalQty(1)">+</button>
                            </div>

                            <button type="button"
                                class="btn btn-danger flex-grow-1 py-2 fw-semibold d-flex align-items-center justify-content-center gap-2 rounded-pill shadow-sm"
                                style="background-color: #900b1a; border-color: #900b1a;"
                                onclick="confirmAddToCart()">
                                <span class="material-symbols-outlined fs-5">add_circle</span>
                                <span>Chọn món này &bull; <span id="modalBtnTotalPrice">0đ</span></span>
                            </button>
                        </div>

                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Selected Items Preview Modal (Món đang chọn - Hiện popup trước khi chuyển giỏ hàng) -->
<div class="modal fade" id="selectedItemsModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered" style="max-width: 550px;">
        <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden bg-white">
            <div class="modal-header border-0 pt-4 px-4 pb-2 d-flex align-items-center justify-content-between">
                <div>
                    <div class="d-flex align-items-center gap-2">
                        <h5 class="modal-title fw-bold text-dark mb-0" style="font-size: 1.25rem; letter-spacing: -0.01em;">Món đang chọn</h5>
                        <span id="selectedModalBadge" class="badge rounded-pill bg-danger-subtle text-danger fw-bold" style="font-size: 0.8rem; padding: 4px 10px;">0 món</span>
                    </div>
                    <p class="text-muted small mb-0 mt-1" style="font-size: 0.85rem;">Các món bạn vừa chọn tại Aura Café</p>
                </div>
                <div class="d-flex align-items-center gap-2">
                    <button type="button" class="btn btn-sm btn-link text-danger text-decoration-none p-1 small fw-semibold" style="font-size: 0.85rem;" onclick="clearAllCart()">
                        Xoá tất cả
                    </button>
                    <button type="button" class="btn btn-light rounded-circle p-1 d-flex align-items-center justify-content-center text-muted border-0 shadow-none"
                            style="width: 36px; height: 36px; transition: background 0.2s;" data-bs-dismiss="modal" aria-label="Close">
                        <span class="material-symbols-outlined fs-5">close</span>
                    </button>
                </div>
            </div>
            <div class="modal-body px-4 pb-4 pt-2" style="overflow-x: hidden !important;">
                <div id="cartEmptyState" class="text-center py-5 d-none">
                    <div class="bg-light rounded-circle p-3 d-inline-flex align-items-center justify-content-center text-muted mb-3" style="width: 60px; height: 60px;">
                        <span class="material-symbols-outlined fs-2">local_cafe</span>
                    </div>
                    <h6 class="fw-bold text-dark mb-1">Chưa có món nào được chọn</h6>
                    <p class="text-muted small mb-3">Hãy chọn các món yêu thích từ thực đơn nhé.</p>
                    <button type="button" class="btn btn-sm btn-outline-danger rounded-pill px-4 py-2 fw-semibold" data-bs-dismiss="modal">
                        Xem thực đơn
                    </button>
                </div>
                <div id="cartItemList" class="d-flex flex-column gap-3 mb-4" style="max-height: 360px; overflow-y: auto; overflow-x: hidden !important; padding: 2px 2px 2px 0;"></div>
                <div id="cartSummary" class="border-top pt-3 d-none">
                    <a href="${pageContext.request.contextPath}/cart"
                        class="btn btn-danger w-100 py-3 fw-bold rounded-pill d-flex align-items-center justify-content-center gap-2 shadow-sm text-decoration-none"
                        style="background: linear-gradient(135deg, #8b1528 0%, #a81c31 100%); font-size: 1rem; box-shadow: 0 6px 20px rgba(139, 21, 40, 0.28) !important;">
                        <span>Xem giỏ hàng</span>
                        <span class="material-symbols-outlined fs-5">arrow_forward</span>
                    </a>
                    <button type="button" class="btn btn-link text-muted small text-decoration-none w-100 mt-2 py-2 d-block text-center fw-medium" data-bs-dismiss="modal" style="font-size: 0.9rem;">
                        + Chọn thêm món khác
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Toast Notification -->
<div class="toast-container position-fixed bottom-0 start-50 translate-middle-x p-3 z-3">
    <div id="cartToast"
        class="toast align-items-center text-white bg-dark border-0 shadow-lg rounded-pill" role="alert"
        aria-live="assertive" aria-atomic="true">
        <div class="d-flex px-3 py-2 align-items-center">
            <span class="material-symbols-outlined text-success me-2 fs-5">check_circle</span>
            <div class="toast-body p-0 fw-medium small" id="cartToastMessage">
                Đã thêm món vào giỏ hàng thành công!
            </div>
            <button type="button" class="btn-close btn-close-white ms-auto me-1"
                data-bs-dismiss="toast"></button>
        </div>
    </div>
</div>

<script>
    var currentProduct = null;
    var selectedSizePrice = 0;
    var modalQty = 1;
    var cartCount = Number('${empty sessionScope.cart ? 0 : sessionScope.cart.totalQuantity}');

    function updateCartBadge() {
        var headerBadge = document.getElementById('headerCartBadge');
        if (headerBadge) {
            headerBadge.innerText = cartCount;
            headerBadge.style.transform = 'scale(1.3)';
            headerBadge.style.transition = 'transform 0.2s';
            setTimeout(function() { headerBadge.style.transform = 'scale(1)'; }, 200);
        }
        var stickyBadge = document.getElementById('stickyCartBadge');
        if (stickyBadge) {
            stickyBadge.innerText = cartCount;
        }
        var stickyCount = document.getElementById('stickyCartCount');
        if (stickyCount) {
            stickyCount.innerText = cartCount;
        }
        var stickyBar = document.getElementById('stickyMiniCart');
        if (stickyBar) {
            if (cartCount > 0) stickyBar.classList.remove('d-none');
            else stickyBar.classList.add('d-none');
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        updateCartBadge();
    });

    // [TCGIO02]
    // . Ở openSelectedItemsModal (đọc giỏ rồi mở danh sách món đã chọn), em đọc components/cart_api.jsp để
    //   lấy các dòng, số lượng và tổng tiền của giỏ trong session. Em đưa dữ liệu cho
    //   renderSelectedItemsModal (đưa các món trong giỏ lên hộp danh sách) dựng nội dung rồi Bootstrap mở
    //   hộp selectedItemsModal. Nếu không đọc được thì chuyển về /cart để xem bằng trang giỏ hàng.
    function openSelectedItemsModal() {
        fetch('${pageContext.request.contextPath}/components/cart_api.jsp')
            .then(function(res) { return res.json(); })
            .then(function(data) {
                renderSelectedItemsModal(data);
                var modalEl = document.getElementById('selectedItemsModal');
                if (modalEl && window.bootstrap) {
                    bootstrap.Modal.getOrCreateInstance(modalEl).show();
                }
            })
            .catch(function(err) {
                console.error('Error fetching cart:', err);
                window.location.href = '${pageContext.request.contextPath}/cart';
            });
    }

    function renderSelectedItemsModal(data) {
        if (!data) return;
        cartCount = data.totalQuantity || 0;
        updateCartBadge();

        var badge = document.getElementById('selectedModalBadge');
        if (badge) badge.innerText = (data.totalQuantity || 0) + ' món';

        var totalPriceEl = document.getElementById('selectedModalTotalPrice');
        if (totalPriceEl) totalPriceEl.innerText = (data.totalAmount || 0).toLocaleString('vi-VN') + 'đ';

        var emptyEl = document.getElementById('cartEmptyState');
        var listEl = document.getElementById('cartItemList');
        var summaryEl = document.getElementById('cartSummary');

        var items = data.items || [];
        if (items.length === 0) {
            if (emptyEl) emptyEl.classList.remove('d-none');
            if (listEl) listEl.innerHTML = '';
            if (summaryEl) summaryEl.classList.add('d-none');
        } else {
            if (emptyEl) emptyEl.classList.add('d-none');
            if (summaryEl) summaryEl.classList.remove('d-none');

            var html = '';
            items.forEach(function(item) {
                var toppingsText = (item.toppings && item.toppings.length > 0) ? item.toppings.join(', ') : '';
                var imgUrl = item.imageUrl || 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80';

                html += '<div class="d-flex align-items-center justify-content-between p-3 rounded-4 bg-light border gap-3" style="transition: all 0.2s;">'
                    + '  <div class="d-flex align-items-center gap-3 overflow-hidden flex-grow-1" style="min-width: 0;">'
                    + '    <img src="' + imgUrl + '" class="rounded-3 object-fit-cover flex-shrink-0" style="width: 58px; height: 58px;" alt="' + item.name + '">'
                    + '    <div class="overflow-hidden flex-grow-1" style="min-width: 0;">'
                    + '      <div class="fw-bold text-dark text-truncate" style="font-size: 0.98rem; line-height: 1.3;" title="' + item.name + '">' + item.name + '</div>'
                    + '      <div class="d-flex align-items-center gap-2 flex-wrap mt-1.5">'
                    + (item.size ? '        <span class="badge bg-danger-subtle text-danger rounded-pill" style="font-size: 0.75rem; font-weight: 600; padding: 3px 10px;">' + (item.sizeLabel || ('Size ' + item.size)) + '</span>' : '')
                    + (toppingsText ? '        <span class="text-muted small text-truncate" style="font-size: 0.8rem; max-width: 200px;">+ ' + toppingsText + '</span>' : '')
                    + '      </div>'
                    + '    </div>'
                    + '  </div>'
                    + '  <div class="d-flex align-items-center gap-2.5 flex-shrink-0">'
                    + '    <div class="d-flex align-items-center border rounded-pill bg-white px-2 py-1 shadow-2xs">'
                    + '      <button type="button" class="btn btn-sm btn-link p-0 text-secondary d-flex align-items-center justify-content-center" style="width: 28px; height: 28px; text-decoration: none; font-size: 1.25rem; font-weight: 700; line-height: 1;" onclick="changeCartItemQty(' + item.lineId + ', -1)">−</button>'
                    + '      <span class="px-2 fw-bold text-dark" style="min-width: 22px; text-align: center; font-size: 0.95rem;">' + item.quantity + '</span>'
                    + '      <button type="button" class="btn btn-sm btn-link p-0 text-secondary d-flex align-items-center justify-content-center" style="width: 28px; height: 28px; text-decoration: none; font-size: 1.25rem; font-weight: 700; line-height: 1;" onclick="changeCartItemQty(' + item.lineId + ', 1)">+</button>'
                    + '    </div>'
                    + '    <button class="btn btn-light rounded-circle p-0 d-flex align-items-center justify-content-center text-muted border-0" style="width: 36px; height: 36px;" title="Xoá món" onclick="removeCartItem(' + item.lineId + ')">'
                    + '      <span class="material-symbols-outlined" style="font-size: 20px;">delete_outline</span>'
                    + '    </button>'
                    + '  </div>'
                    + '</div>';
            });
            if (listEl) listEl.innerHTML = html;
        }
    }

    function changeCartItemQty(lineId, delta) {
        var action = delta > 0 ? 'increase' : 'decrease';
        var params = new URLSearchParams();
        params.append('action', action);
        params.append('lineId', lineId);
        params.append('format', 'json');

        fetch('${pageContext.request.contextPath}/components/cart_api.jsp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: params.toString()
        })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            renderSelectedItemsModal(data);
        })
        .catch(function(err) {
            console.error('Error changing qty:', err);
        });
    }

    function removeCartItem(lineId) {
        var params = new URLSearchParams();
        params.append('action', 'remove');
        params.append('lineId', lineId);
        params.append('format', 'json');

        fetch('${pageContext.request.contextPath}/components/cart_api.jsp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: params.toString()
        })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            renderSelectedItemsModal(data);
        })
        .catch(function(err) {
            console.error('Error removing item:', err);
        });
    }

    function clearAllCart() {
        var params = new URLSearchParams();
        params.append('action', 'clear');
        params.append('format', 'json');

        fetch('${pageContext.request.contextPath}/components/cart_api.jsp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: params.toString()
        })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            renderSelectedItemsModal(data);
        })
        .catch(function(err) {
            console.error('Error clearing cart:', err);
        });
    }

    function addToServerCart(productId, size, toppings, quantity, successMessage) {
        var params = new URLSearchParams();
        params.append('productId', productId);
        params.append('quantity', quantity);
        params.append('size', size || '');
        toppings.forEach(function (t) { params.append('topping', t); });

        fetch('${pageContext.request.contextPath}/cart/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: params.toString()
        })
            .then(function (response) { return response.json(); })
            .then(function (data) {
                if (!data.success) {
                    alert(data.error || 'Không thể thêm món vào giỏ hàng.');
                    return;
                }
                cartCount = data.totalItems;
                updateCartBadge();

                var toastMsg = document.getElementById('cartToastMessage');
                if (toastMsg) toastMsg.innerText = successMessage;
                var toastEl = document.getElementById('cartToast');
                if (toastEl && window.bootstrap) {
                    bootstrap.Toast.getOrCreateInstance(toastEl).show();
                }
            })
            .catch(function () {
                alert('Không thể kết nối máy chủ. Vui lòng thử lại!');
            });
    }

    // [MNCT02]
    // . Ở openProductDetailModal (đọc thông tin rồi mở hộp lựa chọn món), em nhận mã từ nút dấu cộng và đọc
    //   /menu?action=detail&id theo mã đó. MenuController.doGet gọi handleProductDetailApi (đọc và trả thông
    //   tin món cho hộp lựa chọn) để đọc món còn bán rồi trả tên, ảnh, giá và variantsConfig. Khi nhận được,
    //   em dựng lựa chọn size/topping và mở productDetailModal.
    // ... Đọc tiếp tại MenuController.java, tìm [MNCT03].
    function openProductDetailModal(productId) {
        fetch('${pageContext.request.contextPath}/menu?action=detail&id=' + productId)
            .then(function (res) {
                if (!res.ok) {
                    throw new Error('Mạng hoặc máy chủ phản hồi lỗi: ' + res.status);
                }
                return res.json();
            })
            .then(function (data) {
                if (data.error) {
                    alert(data.error);
                    return;
                }
                currentProduct = data;
                modalQty = 1;
                selectedSizePrice = 0;

                var imgEl = document.getElementById('modalProductImg');
                if (imgEl) imgEl.src = data.imageUrl || 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80';

                var nameEl = document.getElementById('modalProductName');
                if (nameEl) nameEl.innerText = data.name || '';

                var catEl = document.getElementById('modalProductCategory');
                if (catEl) catEl.innerText = data.categoryName || '';

                var descEl = document.getElementById('modalProductDesc');
                if (descEl) descEl.innerText = data.description || '';

                var qtyEl = document.getElementById('modalProductQty');
                if (qtyEl) qtyEl.innerText = 1;

                var config = data.variantsConfig || {};
                if (typeof config === 'string') {
                    try {
                        config = JSON.parse(config);
                    } catch (e) {
                        config = {};
                    }
                }
                var sizes = config.sizes;
                if (!sizes || typeof sizes !== 'object' || Object.keys(sizes).length === 0) {
                    sizes = { "Tiêu chuẩn": 0 };
                }
                var toppings = Array.isArray(config.toppings) ? config.toppings : [];

                // Sizes
                var sizeListHtml = '';
                var sizeKeys = Object.keys(sizes);
                var defaultSizeName = sizeKeys.find(function (name) {
                    return Number(sizes[name]) === 0;
                }) || sizeKeys.reduce(function (cheapest, name) {
                    return Number(sizes[name]) < Number(sizes[cheapest]) ? name : cheapest;
                });
                sizeKeys.forEach(function (sName, index) {
                    var extraPrice = Number(sizes[sName]) || 0;
                    var isDefault = sName === defaultSizeName;
                    if (isDefault) selectedSizePrice = extraPrice;
                    sizeListHtml += '<div class="option-chip ' + (isDefault ? 'selected' : '') + '" data-size="' + sName.replace(/"/g, '&quot;') + '" onclick="selectSize(this, ' + extraPrice + ')">'
                        + sName + (extraPrice > 0 ? ' (+' + extraPrice.toLocaleString('vi-VN') + 'đ)' : '')
                        + '</div>';
                });
                var sizeListEl = document.getElementById('sizeList');
                if (sizeListEl) sizeListEl.innerHTML = sizeListHtml;

                // Toppings
                var toppingContainer = document.getElementById('toppingOptionsContainer');
                var toppingListEl = document.getElementById('toppingList');
                if (toppings.length > 0) {
                    if (toppingContainer) toppingContainer.classList.remove('d-none');
                    var toppingListHtml = '';
                    toppings.forEach(function (t, idx) {
                        var tName = typeof t === 'string' ? t : t.name;
                        var tPrice = typeof t === 'object' && t.price != null ? Number(t.price) : 8000;
                        toppingListHtml += '<div class="form-check d-flex align-items-center justify-content-between p-2 border rounded-3">'
                            + '  <div>'
                            + '    <input class="form-check-input ms-0 me-2" type="checkbox" id="top_' + idx + '" value="' + tPrice + '" onchange="calculateModalTotal()">'
                            + '    <label class="form-check-label small fw-medium" for="top_' + idx + '">' + tName + '</label>'
                            + '  </div>'
                            + '  <span class="text-danger fw-bold small">+' + tPrice.toLocaleString('vi-VN') + 'đ</span>'
                            + '</div>';
                    });
                    if (toppingListEl) toppingListEl.innerHTML = toppingListHtml;
                } else {
                    if (toppingContainer) toppingContainer.classList.add('d-none');
                    if (toppingListEl) toppingListEl.innerHTML = '';
                }

                calculateModalTotal();

                var modalEl = document.getElementById('productDetailModal');
                if (modalEl && window.bootstrap) {
                    var myModal = bootstrap.Modal.getOrCreateInstance(modalEl);
                    myModal.show();
                }
            })
            .catch(function (err) {
                console.error('Error loading product:', err);
                alert('Không thể tải thông tin món. Vui lòng thử lại!');
            });
    }

    function closeProductDetailModal() {
        var modalEl = document.getElementById('productDetailModal');
        if (modalEl && window.bootstrap) {
            var modalInstance = bootstrap.Modal.getInstance(modalEl) || bootstrap.Modal.getOrCreateInstance(modalEl);
            if (modalInstance) {
                modalInstance.hide();
            }
        }
    }

    function selectSize(element, extraPrice) {
        var chips = document.querySelectorAll('#sizeList .option-chip');
        chips.forEach(function (c) { c.classList.remove('selected'); });
        element.classList.add('selected');
        selectedSizePrice = extraPrice;
        calculateModalTotal();
    }

    function adjustModalQty(delta) {
        modalQty = Math.max(1, modalQty + delta);
        var qtyEl = document.getElementById('modalProductQty');
        if (qtyEl) qtyEl.innerText = modalQty;
        calculateModalTotal();
    }

    function calculateModalTotal() {
        if (!currentProduct) return;
        var checkboxes = document.querySelectorAll('#toppingList input[type="checkbox"]:checked');
        var topPrice = 0;
        checkboxes.forEach(function (cb) {
            topPrice += parseFloat(cb.value) || 0;
        });
        var base = parseFloat(currentProduct.basePrice) || 0;
        var singleItemPrice = base + (selectedSizePrice || 0) + topPrice;
        var total = singleItemPrice * modalQty;

        var formatted = total.toLocaleString('vi-VN') + 'đ';
        var totalEl = document.getElementById('modalProductTotalPrice');
        if (totalEl) totalEl.innerText = formatted;
        var btnPriceEl = document.getElementById('modalBtnTotalPrice');
        if (btnPriceEl) btnPriceEl.innerText = formatted;
    }

    function confirmAddToCart() {
        if (!currentProduct) return;

        var activeSize = document.querySelector('#sizeList .option-chip.selected');
        var sizeName = activeSize ? activeSize.getAttribute('data-size') : '';

        var toppingNames = [];
        var checkboxes = document.querySelectorAll('#toppingList input[type="checkbox"]:checked');
        checkboxes.forEach(function (cb) {
            var label = cb.parentElement.querySelector('label');
            if (label) toppingNames.push(label.innerText);
        });

        addToServerCart(currentProduct.id, sizeName, toppingNames, modalQty,
            'Đã thêm ' + modalQty + 'x ' + currentProduct.name + ' vào giỏ hàng!');

        var modalEl = document.getElementById('productDetailModal');
        if (modalEl && window.bootstrap) {
            var modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();
        }
    }
</script>
