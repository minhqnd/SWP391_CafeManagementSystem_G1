package utils;

/** Làm gọn chữ nhập trên form giống String.trim() của JavaScript. */
// [KTCHU01]
// . Trước khi kiểm tra và lưu ô nhập, em dùng FormInput để bỏ khoảng trắng thừa ở đầu và cuối theo cùng
//   một cách. Nội dung ở giữa được giữ nguyên, nên việc làm gọn không thay đổi phần người dùng thực sự
//   nhập.
//
// Tra cứu: Trả chuỗi về ngay sau lời gọi trim tại hàm đang dùng; đây là phần hỗ trợ, không chuyển trang hoặc
//          ghi dữ liệu.
public final class FormInput {
    private FormInput() { }

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
