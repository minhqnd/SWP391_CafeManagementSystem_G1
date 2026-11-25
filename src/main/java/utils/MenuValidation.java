package utils;

import java.math.BigDecimal;

/** Các điều kiện chung cho danh mục, sản phẩm và công thức. */
// [QLKT01]
// . Em dùng MenuValidation kiểm tra tên, giá tiền và định lượng theo điều kiện của từng loại. Hàm đang lưu
//   danh mục, sản phẩm hoặc công thức truyền tên ô cần kiểm tra, nên khi có lỗi người dùng biết rõ cần sửa
//   phần nào trước khi lưu lại.
//
// Tra cứu: Kiểm tra xong quay về ngay sau requiredName, money hoặc quantity tại
//          AdminCategoriesController.java / DML04, AdminProductsController.java / SPL04,
//          AdminProductsController.java / SPBT03 hoặc AdminRecipeController.java / CTL04; tiếp tục đường
//          review của chức năng đó.
public final class MenuValidation {
    public static final BigDecimal MAX_MONEY = new BigDecimal("9999999999.99");
    public static final BigDecimal MAX_QUANTITY = new BigDecimal("999999999.999");

    private MenuValidation() { }

    // Kiểm tra tên bắt buộc và độ dài theo loại tên đang được nhập.
    public static String requiredName(String raw, String label, int max) {
        String value = FormInput.trim(raw);
        if (value.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập " + label.toLowerCase(java.util.Locale.ROOT) + ".");
        if (value.length() > max) throw new IllegalArgumentException(label + " tối đa " + max + " ký tự.");
        if (value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(label + " chứa ký tự không được hỗ trợ.");
        return value;
    }

    // Kiểm tra số tiền không âm và nằm trong giới hạn lưu được.
    public static BigDecimal money(String raw, String label) {
        return decimal(raw, label, 2, MAX_MONEY, "9.999.999.999,99", false);
    }

    // Kiểm tra định lượng dương và số chữ số lẻ được phép.
    public static BigDecimal quantity(String raw, String label) {
        return decimal(raw, label, 3, MAX_QUANTITY, "999.999.999,999", true);
    }

    private static BigDecimal decimal(String raw, String label, int scale, BigDecimal max,
                                      String displayMax, boolean positive) {

        String value = FormInput.trim(raw);
        if (value.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập " + label.toLowerCase(java.util.Locale.ROOT) + ".");

        if (value.length() > 64)
            throw new IllegalArgumentException(label + " tối đa 64 ký tự.");
        if (!value.matches("-?[0-9]+(?:\\.[0-9]+)?"))
            throw new IllegalArgumentException(label + " phải là số, dùng dấu chấm nếu có phần thập phân.");

        BigDecimal amount = new BigDecimal(value);
        if (amount.signum() < 0 || (positive && amount.signum() == 0))
            throw new IllegalArgumentException(label + (positive ? " phải lớn hơn 0." : " không được âm."));
        if (amount.compareTo(max) > 0)
            throw new IllegalArgumentException(label + " không được vượt quá " + displayMax + ".");
        if (amount.scale() > scale)
            throw new IllegalArgumentException(label + " có tối đa " + scale + " chữ số thập phân.");
        return amount;
    }
}
