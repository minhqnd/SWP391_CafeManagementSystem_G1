package utils;

/** Lỗi đọc/ghi tài khoản, khác với thông tin người dùng nhập sai. */
public final class AccountDataException extends RuntimeException {
    public AccountDataException(Throwable cause) {
        super("Không xử lý được dữ liệu tài khoản lúc này. Vui lòng thử lại sau.", cause);
    }
}
