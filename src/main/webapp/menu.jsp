<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
            <!DOCTYPE html>
            <html lang="vi">

            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>Aura Café | Menu & Đồ Uống</title>

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
                <!-- Shared Product Card & Modal CSS -->
                <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/product-card.css">

                <style>
                    :root {
                        --primary: #8b1528;
                        --primary-hover: #70101f;
                        --primary-soft: #fdf2f4;
                        --surface: #ffffff;
                        --bg-body: #f8fafc;
                        --text-main: #0f172a;
                        --text-muted: #64748b;
                        --border-color: #f1f5f9;
                    }

                    body {
                        font-family: 'Plus Jakarta Sans', 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        -webkit-font-smoothing: antialiased;
                        -moz-osx-font-smoothing: grayscale;
                        background-color: var(--bg-body);
                        color: var(--text-main);
                        overflow-x: hidden;
                        letter-spacing: -0.01em;
                        padding-bottom: 80px;
                        /* Space for Sticky Bottom Cart */
                    }

                    /* Menu Search Box */
                    .menu-search-container {
                        position: relative;
                        width: 100%;
                        max-width: 380px;
                    }

                    .menu-search-input {
                        background-color: #ffffff;
                        border: 1.5px solid #e2e8f0;
                        border-radius: 999px;
                        padding: 0.6rem 2.8rem 0.6rem 2.8rem;
                        font-size: 0.9rem;
                        transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
                        color: var(--text-main);
                    }

                    .menu-search-input:focus {
                        background-color: #ffffff;
                        border-color: var(--primary);
                        box-shadow: 0 0 0 3.5px rgba(139, 21, 40, 0.12);
                        outline: none;
                    }

                    .menu-search-icon {
                        position: absolute;
                        left: 1rem;
                        top: 50%;
                        transform: translateY(-50%);
                        color: #94a3b8;
                        pointer-events: none;
                    }

                    .menu-search-btn-clear,
                    .menu-search-btn-submit {
                        position: absolute;
                        right: 0.5rem;
                        top: 50%;
                        transform: translateY(-50%);
                        border: none;
                        background: transparent;
                        color: #94a3b8;
                        padding: 0.25rem 0.5rem;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        border-radius: 50%;
                        cursor: pointer;
                        transition: color 0.18s;
                        text-decoration: none;
                    }

                    .menu-search-btn-clear:hover {
                        color: #e11d48;
                    }

                    .menu-search-btn-submit:hover {
                        color: var(--primary);
                    }

                    /* Minimalist Modern Product Card */
                    .product-card {
                        background: #ffffff;
                        border: 1px solid #eef2f6;
                        border-radius: 1.25rem;
                        overflow: hidden;
                        transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
                        display: flex;
                        flex-direction: column;
                        cursor: pointer;
                        position: relative;
                    }

                    .product-card:hover {
                        transform: translateY(-4px);
                        border-color: #cbd5e1;
                        box-shadow: 0 12px 28px -6px rgba(15, 23, 42, 0.08), 0 4px 10px -2px rgba(15, 23, 42, 0.03);
                    }

                    /* Image Box */
                    .product-img-box {
                        position: relative;
                        width: 100%;
                        height: 210px;
                        background-color: #f1f5f9;
                        overflow: hidden;
                    }

                    .product-img-box img {
                        width: 100%;
                        height: 100%;
                        object-fit: cover;
                        transition: transform 0.45s cubic-bezier(0.16, 1, 0.3, 1);
                    }

                    .product-card:hover .product-img-box img {
                        transform: scale(1.05);
                    }
/* Card Content */
                    .product-card-body {
                        padding: 1.15rem 1.25rem;
                        display: flex;
                        flex-direction: column;
                        flex-grow: 1;
                    }

                    .product-meta-category {
                        font-size: 0.72rem;
                        font-weight: 600;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        color: #94a3b8;
                        margin-bottom: 0.25rem;
                    }

                    .product-name {
                        font-size: 1.02rem;
                        font-weight: 700;
                        color: var(--text-main);
                        margin-bottom: 0.35rem;
                        line-height: 1.35;
                        transition: color 0.2s;
                    }

                    .product-card:hover .product-name {
                        color: var(--primary);
                    }

                    .product-brief {
                        font-size: 0.8rem;
                        color: #64748b;
                        line-height: 1.45;
                        display: -webkit-box;
                        -webkit-line-clamp: 2;
                        line-clamp: 2;
                        -webkit-box-orient: vertical;
                        overflow: hidden;
                        min-height: 2.3rem;
                        margin-bottom: 1rem;
                    }

                    /* Card Footer */
                    .product-card-footer {
                        margin-top: auto;
                        padding-top: 0.75rem;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                    }

                    .product-price {
                        font-size: 1.15rem;
                        font-weight: 800;
                        color: var(--primary);
                    }

                    /* 1-Click Quick Add Button */
                    .btn-add-circle {
                        width: 40px;
                        height: 40px;
                        border-radius: 50%;
                        background-color: #ffffff;
                        color: var(--primary);
                        border: 1.5px solid rgba(139, 21, 40, 0.22);
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        box-shadow: 0 2px 6px rgba(139, 21, 40, 0.06);
                        transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
                        cursor: pointer;
                    }

                    .btn-add-circle:hover {
                        background: linear-gradient(135deg, #8b1528 0%, #a81c31 100%);
                        color: #ffffff;
                        transform: scale(1.12) rotate(90deg);
                        border-color: transparent;
                        box-shadow: 0 6px 18px rgba(139, 21, 40, 0.32);
                    }

                    /* Sticky Bottom Mini Cart Bar */
                    .sticky-cart-bar {
                        position: fixed;
                        bottom: 24px;
                        left: 50%;
                        transform: translateX(-50%);
                        background: rgba(15, 23, 42, 0.94);
                        backdrop-filter: blur(16px);
                        -webkit-backdrop-filter: blur(16px);
                        color: #ffffff;
                        padding: 0.65rem 1.35rem;
                        border-radius: 999px;
                        border: 1px solid rgba(255, 255, 255, 0.12);
                        box-shadow: 0 16px 40px rgba(0, 0, 0, 0.35);
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        gap: 1.75rem;
                        z-index: 1040;
                        min-width: 360px;
                        max-width: 90vw;
                        animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
                        transition: transform 0.2s ease, box-shadow 0.2s ease;
                    }

                    .sticky-cart-bar:hover {
                        transform: translate(-50%, -2px);
                        box-shadow: 0 20px 48px rgba(0, 0, 0, 0.45);
                    }

                    @keyframes slideUp {
                        from {
                            transform: translate(-50%, 40px);
                            opacity: 0;
                        }

                        to {
                            transform: translate(-50%, 0);
                            opacity: 1;
                        }
                    }

                    .cart-badge-icon {
                        position: relative;
                        width: 40px;
                        height: 40px;
                        background: rgba(255, 255, 255, 0.12);
                        border-radius: 50%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        color: #ffffff;
                    }

                    .badge-count-pill {
                        position: absolute;
                        top: -4px;
                        right: -4px;
                        background-color: #e11d48;
                        color: #ffffff;
                        font-size: 0.7rem;
                        font-weight: 700;
                        padding: 1px 6px;
                        border-radius: 999px;
                    }

                    /* Customization Modal Styling */
                    .modal-product-img {
                        width: 100%;
                        height: 280px;
                        object-fit: cover;
                        border-radius: 1rem;
                    }

                    .option-chip {
                        cursor: pointer;
                        border: 1px solid #e2e8f0;
                        border-radius: 999px;
                        padding: 0.45rem 1rem;
                        font-size: 0.85rem;
                        font-weight: 500;
                        transition: all 0.18s;
                        background: #ffffff;
                    }

                    .option-chip.selected {
                        border-color: var(--primary);
                        background-color: var(--primary-soft);
                        color: var(--primary);
                        font-weight: 600;
                    }

                    .qty-control-btn {
                        width: 36px;
                        height: 36px;
                        border-radius: 50%;
                        border: 1px solid #cbd5e1;
                        background: #ffffff;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-weight: 600;
                        transition: all 0.18s;
                    }

                    .qty-control-btn:hover {
                        background-color: #f1f5f9;
                    }

                    /* Compact storefront layout shared with the home page */
                    .menu-page-shell { max-width: 960px; margin: 0 auto; padding-left: 12px !important; padding-right: 12px !important; }
                    .menu-breadcrumb { color: #87909c; font-size: 0.72rem; margin: 0 0 14px; }
                    .menu-page-heading { margin-bottom: 12px !important; }
                    .menu-page-heading h1 { font-size: 1.35rem !important; font-weight: 600 !important; }
                    .menu-page-heading p { display: none; }
                    .menu-search-container { max-width: 270px; }
                    .product-card { border-radius: 8px; box-shadow: 0 1px 5px rgba(20, 25, 35, .06); }
                    .product-card:hover { transform: translateY(-2px); box-shadow: 0 5px 14px rgba(20, 25, 35, .10); }
                    .product-img-box { height: 220px; }
                    .product-card-body { padding: 10px 11px; }
                    .product-meta-category { font-size: 0.62rem; margin-bottom: 2px; }
                    .product-name { font-size: 0.82rem; margin-bottom: 3px; }
                    .product-brief { font-size: 0.69rem; min-height: 0; margin-bottom: 5px; }
                    .product-card-footer { padding-top: 4px; }
                    .product-price { font-size: 0.84rem; }
                    .btn-add-circle { width: 30px; height: 30px; }
                    @media (max-width: 767.98px) {
                        .menu-page-shell { padding-left: 16px !important; padding-right: 16px !important; }
                        .menu-page-heading { align-items: flex-start !important; }
                        .menu-search-container { max-width: none; }
                        .product-img-box { height: 190px; }
                    }
                </style>
            </head>

            <body>

                <!-- Shared Header Navbar Component -->
                <jsp:include page="/components/header.jsp">
                    <jsp:param name="activePage" value="menu" />
                </jsp:include>

                <!-- Main Content -->
                <main class="container-fluid menu-page-shell py-3 pb-5 mb-5">

                    <div class="menu-breadcrumb">Trang chủ&nbsp; / &nbsp;Menu &amp; Đồ uống</div>

                    <!-- Top Title & Search Bar -->
                    <div class="menu-page-heading d-flex align-items-center justify-content-between flex-wrap gap-3 mb-4">
                        <div>
                            <h1 class="h3 fw-bold mb-1" style="color: #0f172a;">
                                <c:choose>
                                    <c:when test="${not empty currentKeyword}">
                                        Kết quả tìm kiếm cho "<span class="text-danger">${currentKeyword}</span>"
                                    </c:when>
                                    <c:when test="${not empty currentCategory}">
                                        <c:forEach var="c" items="${categories}">
                                            <c:if test="${c.id == currentCategory}">${c.name}</c:if>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        Thực đơn Aura Café
                                    </c:otherwise>
                                </c:choose>
                                <%-- [MNSO01]
                                     . Số món cạnh tiêu đề là products.size của danh sách sau khi lọc, do
                                       MenuController.doGet đọc qua ProductDAO.filterProducts (lọc món còn bán theo danh mục
                                       và từ khóa). Vì vậy số này thay đổi theo danh mục và từ khóa, không phải tổng mọi sản
                                       phẩm trong bảng.
                                --%>
                                <span class="text-muted fs-6 fw-normal ms-2">(${products.size()} món)</span>
                            </h1>
                            <p class="text-muted small mb-0">Hương vị cà phê đặc sản và trà hảo hạng pha chế thủ công.
                            </p>
                        </div>

                        <!-- Menu Search Box -->
                        <div class="menu-search-container flex-grow-1 flex-md-grow-0">

                            <%-- [MNTK01]
                                 . Khi tìm kiếm trên menu.jsp, form gửi keyword và category đến MenuController.doGet qua
                                   /menu. Controller truyền hai giá trị vào ProductDAO.filterProducts (lọc món còn bán theo
                                   danh mục và từ khóa) để đọc món khớp điều kiện rồi đưa danh sách về menu.jsp để hiển thị.
                            --%>
                            <form action="${pageContext.request.contextPath}/menu" method="GET"
                                class="position-relative w-100">
                                <c:if test="${not empty currentCategory}">
                                    <input type="hidden" name="category" value="${currentCategory}">
                                </c:if>
                                <span class="material-symbols-outlined menu-search-icon fs-5">search</span>
                                <input type="search" name="keyword"
                                    value="${not empty currentKeyword ? currentKeyword : ''}"
                                    class="form-control menu-search-input shadow-none"
                                    placeholder="Tìm tên món, trà, cà phê..." autocomplete="off">
                                <c:choose>
                                    <c:when test="${not empty currentKeyword}">
                                        <%-- [MNX01]
                                             . Dấu x trong ô tìm kiếm mở lại Menu không có keyword nhưng giữ category.
                                               MenuController.doGet đọc lại món trong danh mục đang chọn, không còn giới hạn
                                               theo từ khóa vừa xóa.
                                        --%>
                                        <a href="${pageContext.request.contextPath}/menu${not empty currentCategory ? '?category=' : ''}${currentCategory}"
                                            class="menu-search-btn-clear" title="Xoá tìm kiếm">
                                            <span class="material-symbols-outlined fs-5">close</span>
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        <button type="submit" class="menu-search-btn-submit" title="Tìm kiếm">
                                            <span class="material-symbols-outlined fs-5">arrow_forward</span>
                                        </button>
                                    </c:otherwise>
                                </c:choose>
                            </form>
                        </div>
                    </div>
                    <!-- Active Filter Indicator (if any) -->
                    <c:if test="${not empty currentCategory || not empty currentKeyword}">
                        <div class="d-flex align-items-center gap-2 mb-3 flex-wrap">
                            <span class="text-muted small">Đang lọc:</span>
                            <c:if test="${not empty currentCategory}">
                                <span
                                    class="badge bg-white text-dark border px-2.5 py-1.5 rounded-pill d-inline-flex align-items-center gap-1 shadow-sm"
                                    style="font-weight: 500;">
                                    <span>Danh mục: <c:forEach var="c" items="${categories}">
                                            <c:if test="${c.id == currentCategory}">${c.name}</c:if>
                                        </c:forEach></span>
                                    <%-- [MNX02]
                                         . Dấu x ở nhãn Danh mục bỏ category khỏi đường dẫn nhưng giữ keyword.
                                           MenuController.doGet tìm lại từ khóa trên các danh mục hoạt động, thay vì chỉ
                                           trong nhóm vừa chọn; dữ liệu sản phẩm không bị xóa.
                                    --%>
                                    <a href="${pageContext.request.contextPath}/menu${not empty currentKeyword ? '?keyword=' : ''}${currentKeyword}"
                                        class="text-muted text-decoration-none ms-1">
                                        <span class="material-symbols-outlined" style="font-size: 15px;">close</span>
                                    </a>
                                </span>
                            </c:if>
                            <c:if test="${not empty currentKeyword}">
                                <span
                                    class="badge bg-white text-dark border px-2.5 py-1.5 rounded-pill d-inline-flex align-items-center gap-1 shadow-sm"
                                    style="font-weight: 500;">
                                    <span>Từ khóa: "${currentKeyword}"</span>
                                    <%-- [MNX03]
                                         . Dấu x ở nhãn Từ khóa bỏ keyword nhưng giữ category rồi mở lại Menu.
                                           MenuController.doGet đọc lại các món còn bán theo danh mục hiện tại.
                                    --%>
                                    <a href="${pageContext.request.contextPath}/menu${not empty currentCategory ? '?category=' : ''}${currentCategory}"
                                        class="text-muted text-decoration-none ms-1">
                                        <span class="material-symbols-outlined" style="font-size: 15px;">close</span>
                                    </a>
                                </span>
                            </c:if>
                            <%-- [MNX04]
                                 . Xóa tất cả bỏ cả từ khóa lẫn danh mục bằng cách mở /menu không có tham số.
                                   MenuController.doGet gọi ProductDAO.filterProducts (lọc món còn bán theo danh mục và từ
                                   khóa) không kèm điều kiện để hiện lại toàn bộ món còn bán trong danh mục hoạt động.
                            --%>
                            <a href="${pageContext.request.contextPath}/menu"
                                class="btn btn-sm btn-link text-danger text-decoration-none p-0 ms-2 small fw-semibold">
                                Xoá tất cả
                            </a>
                        </div>
                    </c:if>

                    <!-- Products Grid (4 columns desktop, 2 tablet, 1 mobile) -->
                    <c:choose>
                        <c:when test="${not empty products}">
                            <div class="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-lg-4 g-4" id="productsGrid">

                                <%-- [MNDS01]
                                     . Danh sách món trong menu.jsp lấy từ products do MenuController.doGet đưa sang sau khi
                                       gọi ProductDAO.filterProducts (lọc món còn bán theo danh mục và từ khóa). Em lặp từng
                                       sản phẩm và đưa vào product_card.jsp để dựng thẻ món đã thỏa điều kiện bán cùng bộ
                                       lọc đang chọn.
                                     ... Đọc tiếp tại ProductDAO.java, tìm [MNDS02].
                                --%>
                                <c:forEach var="p" items="${products}">
                                    <div class="col">
                                        <c:set var="product" value="${p}" scope="request" />
                                        <jsp:include page="/components/product_card.jsp" />
                                    </div>
                                </c:forEach>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <!-- Clean Empty State -->
                            <div class="text-center py-5 my-4 bg-white rounded-4 border p-5">
                                <span class="material-symbols-outlined text-secondary mb-3"
                                    style="font-size: 3rem;">search_off</span>
                                <h3 class="h5 fw-bold mb-2">Không tìm thấy món phù hợp</h3>
                                <p class="text-muted small mb-4">Vui lòng thử tìm với từ khoá khác hoặc xem toàn bộ danh
                                    mục thực đơn.</p>
                                <%-- [MNX05]
                                     . Khi không có kết quả, nút Xem tất cả món mở /menu không kèm từ khóa hoặc danh mục để
                                       MenuController.doGet đọc lại danh sách còn bán.
                                --%>
                                <a href="${pageContext.request.contextPath}/menu"
                                    class="btn btn-danger btn-sm px-4 py-2 rounded-pill fw-semibold">
                                    Xem tất cả món
                                </a>
                            </div>
                        </c:otherwise>
                    </c:choose>

                </main>

                <!-- Sticky Bottom Selection Bar (Hiện khi đang chọn món - Bấm Xem món ra popup) -->
                <div id="stickyMiniCart" class="sticky-cart-bar ${empty sessionScope.cart || sessionScope.cart.totalQuantity == 0 ? 'd-none' : ''}" onclick="openSelectedItemsModal()"
                    style="cursor: pointer;" title="Bấm để xem danh sách món">
                    <div class="d-flex align-items-center gap-3">
                        <div class="cart-badge-icon">
                            <span class="material-symbols-outlined fs-5">shopping_bag</span>
                            <span id="stickyCartBadge" class="badge-count-pill">${empty sessionScope.cart ? 0 : sessionScope.cart.totalQuantity}</span>
                        </div>
                        <div class="fw-bold text-white" style="font-size: 0.95rem;">
                            Đang chọn <span id="stickyCartCount" class="text-danger fw-bold">${empty sessionScope.cart ? 0 : sessionScope.cart.totalQuantity}</span> món
                        </div>
                    </div>
                    <button type="button"
                        class="btn btn-danger rounded-pill px-4 py-2 fw-semibold d-flex align-items-center gap-2 shadow-sm"
                        onclick="event.stopPropagation(); openSelectedItemsModal()" title="Xem danh sách món đang chọn">
                        <span>Xem món</span>
                        <span class="material-symbols-outlined fs-6">arrow_forward</span>
                    </button>
                </div>

                <!-- Product Customization Modal & Cart Toast Component -->
                <jsp:include page="/components/product_detail_modal.jsp" />

                <!-- Include Auth Modal -->
                <jsp:include page="/components/auth_modal.jsp" />

                <!-- Bootstrap 5 JS Bundle -->
                <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

                <script>
                    // 1-Click Quick Add: Bấm nút + thêm ngay vào giỏ hàng với size mặc định, không popup
                    function quickAddToCart(event, id, name, price, imageUrl, categoryId) {
                        if (event) {
                            event.stopPropagation();
                        }
                        addToServerCart(id, '', [], 1, 'Đã thêm nhanh ' + name + ' vào giỏ!');
                    }

                    // Xử lý khi bấm vào Card
                    function handleCardClick(id, name, price, imageUrl, categoryId) {
                        // Category 5 (Bánh ngọt) & 6 (Đóng chai): không có size/topping -> thêm ngay, không mở popup
                        if (categoryId === 5 || categoryId === 6) {
                            quickAddToCart(null, id, name, price, imageUrl, categoryId);
                        } else {
                            // Đồ uống: mở popup để khách chọn đổi size L, thêm topping
                            openProductDetailModal(id);
                        }
                    }

                    var openCartModal = openSelectedItemsModal;

                    // [MNBM02]
                    // . Nút Danh mục sản phẩm trên Menu thêm hoặc bỏ class show của menuCategoryDropdown, đồng thời đổi
                    //   aria-expanded. Nếu đường dẫn có phần #menuCategoryDropdown thì mở sẵn danh sách. Đây chỉ là mở bộ
                    //   chọn; bấm tên danh mục mới gửi điều kiện lọc tới controller.
                    const menuCategoryPicker = document.getElementById('menuCategoryDropdown');
                    const menuCategoryToggle = document.querySelector('.menu-category-toggle');
                    if (menuCategoryPicker && menuCategoryToggle) {
                        menuCategoryToggle.addEventListener('click', function (event) {
                            event.preventDefault();
                            const isOpen = menuCategoryPicker.classList.toggle('show');
                            menuCategoryToggle.setAttribute('aria-expanded', String(isOpen));
                        });
                    }
                    if (menuCategoryPicker && window.location.hash === '#menuCategoryDropdown') {
                        menuCategoryPicker.classList.add('show');
                    }
                </script>
            </body>

            </html>
