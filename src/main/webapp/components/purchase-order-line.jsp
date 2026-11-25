<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<fieldset class="purchase-line border rounded p-3 mb-3">
    <legend class="float-none w-auto fs-6 fw-bold px-1">Nguyên liệu đặt mua</legend>
    <div class="mb-3">
        <label class="form-label fw-bold small text-secondary">Nguyên liệu <span class="text-danger">*</span></label>
        <select class="form-select" name="ingredientId" required>
            <c:forEach var="ingredient" items="${allIngredients}">
                <option value="${ingredient.id}"><c:out value="${ingredient.name}"/> (<c:out value="${ingredient.unit}"/>)</option>
            </c:forEach>
        </select>
    </div>
    <div class="row g-2">
        <div class="col-md-6">
            <label class="form-label fw-bold small text-secondary">Số lượng đặt <span class="text-danger">*</span></label>
            <input class="form-control" name="quantity" type="number" min="0.001" max="999999999.999" step="0.001" required>
        </div>
        <div class="col-md-6">
            <label class="form-label fw-bold small text-secondary">Tổng tiền lô (đ) <span class="text-danger">*</span></label>
            <input class="form-control" name="totalPrice" type="number" min="0" max="9999999999.99" step="0.01" required>
        </div>
    </div>
</fieldset>
