package utils;

/** Server-side numeric boundaries shared by loyalty controllers and persistence. */
public final class LoyaltyValidation {
    public static final long MAX_RULE_MONEY = 100_000_000L;
    public static final int MAX_POINTS = 1_000_000;

    private LoyaltyValidation() { }

    // RU19: . Chỉ nhận chuỗi chữ số ASCII sau trim, tối đa 19 ký tự rồi parse long.
    // .. Rỗng, âm, số lẻ, dạng mũ, chữ và tràn long đều bị từ chối.
    // ... requireRange() [RU20] kiểm tra biên; kết quả về ruleValue() [RU18] hoặc adjustPoints() [LP05].
    public static long wholeNumber(String raw, String label, long min, long max) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || value.length() > 19 || !value.matches("[0-9]+")) {
            throw new IllegalArgumentException(label + " phải là số nguyên từ " + min + " đến " + max + ".");
        }
        try {
            long parsed = Long.parseLong(value);
            requireRange(parsed, label, min, max);
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " vượt quá giới hạn cho phép.");
        }
    }

    // RU20: . Kiểm tra min <= value <= max trước khi lưu hoặc chuyển kiểu số.
    // .. Ngoài giới hạn thì ném IllegalArgumentException; chưa truy vấn database.
    // ... Trả về nơi gọi ở [RU19], saveLoyaltyRules() [RU22] hoặc adjustPoints() [LP09].
    public static void requireRange(long value, String label, long min, long max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(label + " phải từ " + min + " đến " + max + ".");
        }
    }
}
