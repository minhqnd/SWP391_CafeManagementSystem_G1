package utils;

// [TDSO01]
// . Em dùng LoyaltyValidation kiểm tra số nguyên cho cả quy tắc và điều chỉnh điểm. Hàm đang xử lý truyền nội
//   dung ô, tên ô và giới hạn; em trả lại số hợp lệ hoặc lời báo có tên ô, để yêu cầu chỉ được lưu khi giá
//   trị đúng.
//
// Tra cứu: Với quy tắc, quay về sau wholeNumber trong ruleValue ngay dưới AdminLoyaltyController.java /
//          TDQT04. Với điều chỉnh, quay về sau wholeNumber tại AdminLoyaltyController.java / TDDC03. Với phần
//          ghi, quay về sau requireRange tại ShopSettingDAO.java / TDQT05 hoặc LoyaltyTransactionDAO.java /
//          TDDC04.
public final class LoyaltyValidation {
    public static final long MAX_RULE_MONEY = 100_000_000L;
    public static final int MAX_POINTS = 1_000_000;

    private LoyaltyValidation() { }

    public static long wholeNumber(String raw, String label, long min, long max) {

        String value = FormInput.trim(raw);

        if (value.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập " + label.toLowerCase(java.util.Locale.ROOT) + ".");
        if (!value.matches("[0-9]+")) {
            throw new IllegalArgumentException(label + " phải là số nguyên từ " + min + " đến " + max + ".");
        }
        if (value.length() > 19) throw new IllegalArgumentException(label + " phải từ " + min + " đến " + max + ".");
        try {

            long parsed = Long.parseLong(value);

            requireRange(parsed, label, min, max);

            return parsed;
        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(label + " phải từ " + min + " đến " + max + ".");
        }
    }

    public static void requireRange(long value, String label, long min, long max) {

        if (value < min || value > max) {
            throw new IllegalArgumentException(label + " phải từ " + min + " đến " + max + ".");
        }
    }
}
