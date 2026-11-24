package utils;

import java.math.BigDecimal;

/** Các điều kiện chung cho danh mục, sản phẩm và công thức. */
public final class MenuValidation {
    public static final BigDecimal MAX_MONEY = new BigDecimal("9999999999.99");
    public static final BigDecimal MAX_QUANTITY = new BigDecimal("999999999.999");

    private MenuValidation() { }

    // [DMT03]
    // . Trong requiredName của MenuValidation.java, em nhận tên vừa nhập, tên ô và giới hạn từ hàm save
    //   của controller danh mục hoặc sản phẩm. Em bỏ khoảng trắng hai đầu, kiểm tra tên trống, quá dài
    //   hoặc có ký tự không hỗ trợ. Đúng thì trả tên đã làm gọn về save; sai thì báo rõ ô cần sửa và
    //   chưa lưu.
    // ... Quay về save ngay sau requiredName. Tên đúng mới kiểm tra trùng: danh mục đọc
    //     AdminCategoriesController.java, tìm [DMTR01], sản phẩm đọc AdminProductsController.java, tìm
    //     mã SPTR01. Tên sai thì doPost giữ form và hiện lỗi, chưa gọi ghi.
    public static String requiredName(String raw, String label, int max) {
        String value = FormInput.trim(raw);
        if (value.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập " + label.toLowerCase(java.util.Locale.ROOT) + ".");
        if (value.length() > max) throw new IllegalArgumentException(label + " tối đa " + max + " ký tự.");
        if (value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(label + " chứa ký tự không được hỗ trợ.");
        return value;
    }

    // [SPG03]
    // . AdminProductsController.save gọi money trong MenuValidation.java để kiểm tra giá cơ bản. Phụ thu
    //   size và giá topping cũng gọi hàm này qua parseNonNegative.
    // . Em gọi decimal cùng file để kiểm tra số không âm, giới hạn tiền và tối đa hai chữ số thập phân.
    //   Đúng thì trả số về save hoặc parseNonNegative để ghép vào Product; sai thì báo lỗi và dừng lưu.
    // ... Quay về đúng lời gọi MenuValidation.money: save nhận giá cơ bản; parseNonNegative trả phụ thu
    //     hoặc giá topping về buildVariantsConfig. Ghép xong Product mới tới điểm ghi tại
    //     AdminProductsController.java, tìm mã SPL05; số sai thì dừng và trả form lỗi.
    public static BigDecimal money(String raw, String label) {
        return decimal(raw, label, 2, MAX_MONEY, "9.999.999.999,99", false);
    }

    // [CTDL02]
    // . Từ parseIngredients trong AdminRecipeController.java, em gọi quantity để kiểm tra định lượng của
    //   từng dòng. Em gọi decimal ngay bên dưới với điều kiện lớn hơn 0, tối đa 999.999.999,999 và ba số
    //   thập phân. Hợp lệ thì trả số về để ghép với nguyên liệu; sai thì lời báo ghi cả số dòng cần sửa.
    // ... quantity trả lượng hợp lệ về parseIngredients. Hàm ghép lượng với đúng mã nguyên liệu; xét hết
    //     các dòng thì về doPost. Đọc điểm gọi lưu tại AdminRecipeController.java, tìm [CTL05].
    public static BigDecimal quantity(String raw, String label) {
        return decimal(raw, label, 3, MAX_QUANTITY, "999.999.999,999", true);
    }

    private static BigDecimal decimal(String raw, String label, int scale, BigDecimal max,
                                      String displayMax, boolean positive) {
        // . Em nhận nội dung từ money hoặc quantity, bỏ khoảng trắng rồi kiểm tra ô trống và cách viết số.
        //   Tên ô được giữ trong label để lời báo chỉ rõ giá bán, phụ thu, topping hay dòng định lượng đang lỗi.
        String value = FormInput.trim(raw);
        if (value.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập " + label.toLowerCase(java.util.Locale.ROOT) + ".");
        // . Chuỗi quá dài báo giới hạn độ dài; nhập chữ, số mũ hoặc dấu phẩy báo sai cách viết số.
        //   Em chỉ đổi thành số sau khi nội dung đã qua hai bước kiểm tra này.
        if (value.length() > 64)
            throw new IllegalArgumentException(label + " tối đa 64 ký tự.");
        if (!value.matches("-?[0-9]+(?:\\.[0-9]+)?"))
            throw new IllegalArgumentException(label + " phải là số, dùng dấu chấm nếu có phần thập phân.");
        // . Đổi được thành số, em kiểm tra lần lượt số âm hoặc bằng 0, giới hạn lớn nhất và số chữ số sau
        //   dấu chấm. Mỗi điều kiện có lời báo riêng; qua hết mới trả số về hàm vừa gọi để tiếp tục lưu.
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
