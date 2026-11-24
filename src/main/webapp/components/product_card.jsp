<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="p" value="${requestScope.product}" />
<c:if test="${not empty p}">
    <!-- Reusable Product Card Component -->
    <%-- [MNTHE01]
         . Thẻ món trong product_card.jsp nhận sản phẩm để hiện ảnh, tên và giá. Khi bấm thẻ, mã món được gửi
           qua /product-detail tới ProductDetailController.doGet để đọc lại món còn bán rồi đưa về
           product-detail.jsp, mở chi tiết đúng sản phẩm vừa chọn.
         ... Đọc tiếp tại ProductDetailController.java, tìm [MNTHE02].
    --%>
    <div class="product-card h-100"
        onclick="window.location.href='${pageContext.request.contextPath}/product-detail?id=${p.id}'">
        <!-- Image Box -->
        <div class="product-img-box">
            <%-- [SPHA01]
                 . Ảnh thẻ món lấy đường dẫn từ p.displayImageUrl. Product.getDisplayImageUrl chọn ảnh đang lưu trong
                   cơ sở dữ liệu hoặc imageUrl của món; nếu trình duyệt tải ảnh lỗi, onerror đổi sang ảnh mặc định để
                   thẻ vẫn có hình đại diện.
                 ... Đọc tiếp tại Product.java, tìm [SPHA02].
            --%>
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
                     . Nút dấu cộng trên thẻ gọi openProductDetailModal trong product_detail_modal.jsp, truyền mã món để
                       đọc thông tin, size và topping rồi mở hộp lựa chọn. Khác thao tác bấm tên mở trang chi tiết, nút
                       này mở hộp ngay trên trang và chưa thêm món vào giỏ.
                     ... Đọc tiếp tại product_detail_modal.jsp, tìm [MNCT02].
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
