<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="p" value="${requestScope.product}" />
<c:if test="${not empty p}">
    <!-- Reusable Product Card Component -->
    <div class="product-card h-100"
        onclick="window.location.href='${pageContext.request.contextPath}/product-detail?id=${p.id}'">
        <!-- Image Box -->
        <div class="product-img-box">
            <c:if test="${not empty p.badge}">
                <span class="pill-badge ${p.badge == 'BÁN CHẠY' ? 'pill-badge-hot' : 'pill-badge-new'}">
                    ${p.badge}
                </span>
            </c:if>
            <img src="${p.imageUrl}" alt="${p.name}" loading="lazy"
                onerror="this.src='https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80'">
        </div>

        <!-- Body -->
        <div class="product-card-body">
            <div class="product-meta-subtitle">
                ${not empty p.subtitle ? p.subtitle : p.categoryName}
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
                <button type="button" class="btn-add-circle"
                    title="Tùy chọn & Thêm món"
                    onclick="event.stopPropagation(); openProductDetailModal('${p.id}')">
                    <span class="material-symbols-outlined fs-5">add</span>
                </button>
            </div>
        </div>
    </div>
</c:if>
