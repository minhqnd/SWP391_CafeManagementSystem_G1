package utils;

public final class LoyaltyValidation {
    // [SUA12-04] Luyện sửa: Giới hạn số tiền trong quy tắc còn tối đa 1000000đ.
    // . Hằng này dùng cho tiền tích một điểm và tiền giảm mỗi lần đổi; đổi cùng giới hạn trong
    //   admin-form-validation.js.
    // . Đây là mức lớn nhất được phép nhập, không phải giá trị quy tắc đang lưu trong shop_settings.
    public static final long MAX_RULE_MONEY = 100_000_000L;
    // [SUA12-03] Luyện sửa: Mỗi lần điều chỉnh chỉ cho tối đa 1000 điểm thay vì 1000000.
    // . Hằng này còn giới hạn mốc đổi điểm trong updateRules và saveLoyaltyRules; nếu chỉ đổi điều chỉnh thì
    //   cần giới hạn riêng.
    // . Đồng bộ kiểm tra trong admin-loyalty.jsp, admin-form-validation.js và lời báo 1.000.000 trong
    //   LoyaltyTransactionDAO.
    public static final int MAX_POINTS = 1_000_000;

    private LoyaltyValidation() { }

    // [TDQT03]
    // . Để kiểm tra mỗi ô, ruleValue trong AdminLoyaltyController.java gọi wholeNumber của
    //   LoyaltyValidation.java, truyền nội dung nhập, tên ô và giới hạn được phép. Em bỏ khoảng trắng đầu
    //   và cuối, kiểm tra số nguyên rồi xét giới hạn. Đúng thì trả số về ruleValue; sai thì trả lỗi để gắn
    //   vào đúng ô đang kiểm tra.
    // ... Trả số hoặc lời báo về ruleValue trong controller. Kiểm tra xong cả ba ô, updateRules xét errors
    //     tại AdminLoyaltyController.java, tìm mã TDQT04 để quyết định lưu hay trả form.
    public static long wholeNumber(String raw, String label, long min, long max) {
        // . Em bỏ khoảng trắng đầu cuối để kiểm tra đúng phần số người dùng nhập.
        String value = FormInput.trim(raw);

        // . Ô trống thì yêu cầu nhập; có chữ, dấu âm hoặc dấu thập phân thì báo phải nhập số nguyên.
        //   Chuỗi số quá dài cũng bị chặn trước khi đổi thành số để tránh lỗi khi đọc giá trị.
        if (value.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập " + label.toLowerCase(java.util.Locale.ROOT) + ".");
        if (!value.matches("[0-9]+")) {
            throw new IllegalArgumentException(label + " phải là số nguyên từ " + min + " đến " + max + ".");
        }
        if (value.length() > 19) throw new IllegalArgumentException(label + " phải từ " + min + " đến " + max + ".");
        try {
            // . Em gọi Long.parseLong để đổi nội dung ô thành số, rồi kiểm tra giới hạn được phép bên dưới.
            long parsed = Long.parseLong(value);

            // . Em truyền số vừa đọc, tên ô và hai giới hạn cho requireRange cùng file. Ngoài khoảng thì báo
            //   lỗi mang tên ô; đúng khoảng thì trả số về chỗ gọi để tiếp tục xử lý.
            requireRange(parsed, label, min, max);

            // . Với Lưu quy tắc, số quay về ruleValue trong AdminLoyaltyController.java để updateRules xét
            //   đủ ba ô trước khi lưu. Với Cộng/Trừ điểm, số quay về adjustPoints để kiểm tra lần điều chỉnh.
            // ... Đọc tiếp tại AdminLoyaltyController.java, tìm [TDQT04].
            return parsed;
        } catch (NumberFormatException e) {
            // . Nếu chuỗi số vẫn quá lớn để đọc, em đổi lỗi đọc số thành lời báo giới hạn của ô cần sửa.
            throw new IllegalArgumentException(label + " phải từ " + min + " đến " + max + ".");
        }
    }

    public static void requireRange(long value, String label, long min, long max) {
        // . Số nhỏ hơn mức tối thiểu hoặc lớn hơn mức tối đa thì em báo lỗi để nơi gọi dừng trước khi lưu.
        if (value < min || value > max) {
            throw new IllegalArgumentException(label + " phải từ " + min + " đến " + max + ".");
        }
    }
}
