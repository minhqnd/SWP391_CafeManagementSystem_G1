package utils;

/**
 * RedirectUtil - Kiểm tra đường dẫn quay lại sau khi đăng nhập / đăng ký
 */
public class RedirectUtil {

    private RedirectUtil() {
    }

    /**
     * Chỉ chấp nhận đường dẫn nội bộ đơn giản như "/checkout" để tránh chuyển hướng sang trang web khác
     */
    public static boolean isSafeLocalPath(String path) {
        return path != null && path.matches("/[A-Za-z0-9_\\-/]*") && !path.startsWith("//");
    }
}
