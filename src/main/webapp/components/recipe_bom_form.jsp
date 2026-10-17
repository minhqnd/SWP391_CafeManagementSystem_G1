<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="jakarta.tags.core" prefix="c" %>

<div class="bg-white p-6 rounded-2xl border border-stone-200 shadow-sm mt-4">
    <h3 class="font-bold text-lg text-stone-900 mb-4 border-b pb-2">Công Thức & Định Lượng Kỹ Thuật (SOP)</h3>

    <!-- Theo SRS: Entity Recipe phải có "instructions" -->
    <div class="mb-8">
        <label class="block text-sm font-bold text-stone-800 mb-2">Hướng dẫn pha chế (Recipe Instructions)</label>
        <textarea name="instructions" rows="4" class="w-full p-3 border border-stone-200 rounded-xl focus:border-primary focus:outline-none" placeholder="Nhập các bước pha chế (VD: Bước 1: Cho 30ml cốt cafe...)"></textarea>
        <p class="text-xs text-stone-500 mt-1">Nội dung này tương đương thuộc tính 'instructions' của Entity 'Recipe' trong bản tả SRS.</p>
    </div>

    <!-- Theo SRS: Entity RecipeIngredient phải có "quantity required" -->
    <div>
        <div class="flex items-center justify-between mb-3">
            <h4 class="font-bold text-md text-stone-800">Bảng thành phần định lượng (Recipe Ingredients)</h4>
            <button type="button" onclick="addIngredientRow()" class="px-3 py-1.5 text-xs bg-emerald-100 text-emerald-800 font-bold rounded-lg hover:bg-emerald-200">
                + Thêm nguyên liệu
            </button>
        </div>
        
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse" id="bomTable">
                <thead class="bg-stone-50 text-stone-600 text-xs uppercase border-y border-stone-200">
                    <tr>
                        <th class="py-2 px-3">Tên Nguyên Liệu</th>
                        <th class="py-2 px-3 w-1/3 text-right">Định lượng (Quantity)</th>
                        <th class="py-2 px-3 w-16 text-center">Xóa</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-stone-100 text-sm" id="bomTableBody">
                    <tr class="hover:bg-stone-50 transition-colors">
                        <td class="py-2 px-3">
                            <select name="ingredientId[]" class="w-full p-2 border border-stone-200 rounded focus:border-primary focus:outline-none" required>
                                <option value="">-- Chọn nguyên liệu --</option>
                                <c:forEach items="${allIngredients}" var="ing">
                                    <option value="${ing.id}">${ing.name} (${ing.unit})</option>
                                </c:forEach>
                            </select>
                        </td>
                        <td class="py-2 px-3 text-right">
                            <input type="number" step="0.01" min="0" name="quantity[]" class="w-full p-2 border border-stone-200 rounded text-right focus:border-primary focus:outline-none" placeholder="VD: 50.5" required>
                        </td>
                        <td class="py-2 px-3 text-center">
                            <button type="button" onclick="removeRow(this)" class="text-rose-500 hover:text-rose-700 font-bold px-2 py-1 bg-rose-50 rounded">✕</button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script>
    function addIngredientRow() {
        const tbody = document.getElementById('bomTableBody');
        const firstRow = tbody.querySelector('tr');
        if(firstRow) {
            const newRow = firstRow.cloneNode(true);
            newRow.querySelector('input').value = '';
            newRow.querySelector('select').selectedIndex = 0;
            tbody.appendChild(newRow);
        } else {
            alert("Không thể thêm, vui lòng tải lại trang.");
        }
    }

    function removeRow(btn) {
        const tbody = document.getElementById('bomTableBody');
        if(tbody.children.length > 1) {
            btn.closest('tr').remove();
        } else {
            alert("Phải có ít nhất 1 nguyên liệu trong công thức!");
        }
    }
</script>
