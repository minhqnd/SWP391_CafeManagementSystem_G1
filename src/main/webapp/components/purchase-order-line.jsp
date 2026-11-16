<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<fieldset class="purchase-line border rounded p-3 mb-3">
    <legend class="float-none w-auto fs-6">Nguyên liệu đặt mua</legend>
    <label class="form-label d-block">Nguyên liệu
        <select class="form-select" name="ingredientId" required>
            <c:forEach var="ingredient" items="${allIngredients}">
                <option value="${ingredient.id}"><c:out value="${ingredient.name}"/> (<c:out value="${ingredient.unit}"/>)</option>
            </c:forEach>
        </select>
    </label>
    <label class="form-label d-block">Số lượng đặt
        <input class="form-control" name="quantity" type="number" min="0.001" max="999999999.999" step="0.001" required>
    </label>
    <label class="form-label d-block">Tổng tiền lô của nguyên liệu này (đ)
        <input class="form-control" name="totalPrice" type="number" min="0" max="9999999999.99" step="0.01" required>
    </label>
    <button type="button" class="btn btn-outline-danger btn-sm" data-remove-line>Xóa dòng</button>
</fieldset>
