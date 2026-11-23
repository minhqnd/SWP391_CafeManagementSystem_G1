package utils;

public final class LoyaltyValidation {
    public static final long MAX_RULE_MONEY = 100_000_000L;
    public static final int MAX_POINTS = 1_000_000;

    private LoyaltyValidation() { }

    // [TDQT03]
    // . Từ ruleValue trong AdminLoyaltyController.java, em gọi wholeNumber trong LoyaltyValidation.java để
    //   kiểm tra số nguyên. Em truyền nội dung ô, tên hiển thị và hai giới hạn; hợp lệ thì trả số về
    //   ruleValue, sai thì báo lỗi để controller gắn đúng ô và dừng lưu.
    // ... Quay lại ruleValue trong AdminLoyaltyController.java ngay sau LoyaltyValidation.wholeNumber; ruleValue
    //     trả số cho updateRules hoặc giữ lỗi của ô, rồi updateRules xét errors của cả ba ô.
    public static long wholeNumber(String raw, String label, long min, long max) {
        // . Em bỏ khoảng trắng đầu cuối để kiểm tra đúng phần số người dùng nhập.
        String value = raw == null ? "" : raw.trim();

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
