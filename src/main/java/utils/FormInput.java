package utils;

/** Làm gọn chữ nhập trên form giống String.trim() của JavaScript. */
public final class FormInput {
    private FormInput() { }

    // . Chỉ bỏ khoảng trắng hai đầu; giữ nguyên chữ và khoảng trắng bên trong.
    // . Mật khẩu chỉ dùng kết quả này để nhận biết ô toàn khoảng trắng, không thay nội dung đem lưu.
    public static String trim(String raw) {
        if (raw == null) return "";
        int start = 0, end = raw.length();
        while (start < end && isSpace(raw.charAt(start))) start++;
        while (end > start && isSpace(raw.charAt(end - 1))) end--;
        return raw.substring(start, end);
    }

    private static boolean isSpace(char value) {
        return Character.isSpaceChar(value) || (value >= 0x09 && value <= 0x0D) || value == 0xFEFF;
    }
}
