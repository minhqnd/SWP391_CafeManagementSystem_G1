<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="p" value="${requestScope.product}" />
<c:if test="${not empty p}">
    <!-- Reusable Product Card Component -->
    <%-- [MNTHE01]
         . Trên thẻ món, em hiển thị tên, ảnh và giá của cùng một sản phẩm. Khi khách bấm thẻ, mã sản phẩm
           được dùng để mở trang chi tiết. Nếu bấm nút tròn bên trong thẻ, em mở hộp chi tiết ngay trên
           trang đang xem.

         Tra tiếp: ProductDetailController.java / MNTHE02.
    --%>
    <div class="product-card h-100"
        onclick="window.location.href='${pageContext.request.contextPath}/product-detail?id=${p.id}'">
        <!-- Image Box -->
        <div class="product-img-box">

            <img src="${p.displayImageUrl}" alt="${p.name}" loading="lazy"
                onerror="this.src='https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80'">
        </div>

        <!-- Body -->
        <div class="product-card-body">
            <div class="product-meta-category">
                ${p.categoryName}
            </div>
            <h2 class="product-name" title="${p.name}">
                <a href="${pageContext.request.contextPath}/product-detail?id=${p.id}"
                    class="text-decoration-none text-dark"
                    onclick="event.stopPropagation()">
                    ${p.name}
                </a>
            </h2>
            <p class="product-brief" title="${p.description}">
                ${not empty p.description ? p.description : 'Đồ uống tinh hoa được tuyển chọn từ vùng nguyên liệu đặc sản.'}
            </p>

            <!-- Price & Add Button -->
            <div class="product-card-footer">
                <span class="product-price">${p.formattedPrice}</span>
                <%-- [MNCT01]
                     . Khi người dùng bấm nút tròn trên thẻ, em truyền mã món cho openProductDetailModal.
                       Hàm này lấy thông tin món rồi mở hộp chi tiết ngay trên trang đang xem.

                     Tra tiếp: product_detail_modal.jsp / MNCT02.
                --%>
                <button type="button" class="btn-add-circle"
                    title="Tùy chọn & Thêm món"
                    onclick="event.stopPropagation(); openProductDetailModal('${p.id}')">
                    <span class="material-symbols-outlined fs-5">add</span>
                </button>
            </div>
        </div>
    </div>
</c:if>
