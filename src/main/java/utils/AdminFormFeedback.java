package utils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

// [QLLOI01]
// . Em dùng AdminFormFeedback để nhận thông tin từ các form có ảnh và đổi lỗi ghi dữ liệu thành lời báo
//   phù hợp với thao tác. Các màn hình quản trị dùng chung phần này để người dùng biết yêu cầu chưa lưu
//   được và cần xử lý phần nào.
//
// Tra cứu: Trở về AdminCategoriesController.java / DML03 hoặc AdminProductsController.java / SPL03 ngay sau
//          lời gọi readMultipart hay databaseError, rồi mở lại trang và hiển thị admin-categories.jsp / DML07
//          hoặc admin-products.jsp / SPL07.
public final class AdminFormFeedback {
    private AdminFormFeedback() { }

    public static void clear(HttpSession session, String prefix) {
        session.removeAttribute(prefix + "Message");
        session.removeAttribute(prefix + "Error");
        session.removeAttribute(prefix + "Draft");
    }

    public static boolean readMultipart(HttpServletRequest request, HttpSession session, String prefix) {
        if (request.getContentType() == null || !request.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("multipart/")) return true;
        try {
            request.getParts();
            return true;
        } catch (IllegalStateException | ServletException | IOException e) {
            boolean tooLarge = false;
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                String name = cause.getClass().getSimpleName();
                if (name.contains("SizeLimitExceeded") || name.contains("FileTooLarge")) tooLarge = true;
            }
            String message = tooLarge
                    ? "Ảnh tối đa 5 MB; toàn bộ biểu mẫu tối đa 6 MB. Vui lòng chọn lại ảnh nhỏ hơn."
                    : "Không đọc được biểu mẫu hoặc tệp ảnh. Vui lòng tải lại trang rồi gửi lại.";
            session.setAttribute(prefix + "Error", message + " Dữ liệu chưa được lưu.");
            return false;
        }
    }

    public static String databaseError(SQLException error, String action, String object) {
        if (error.getErrorCode() == 1062)
            return "Tên " + object + " đã tồn tại. Vui lòng chọn tên khác.";
        if ("delete".equals(action) && error.getSQLState() != null && error.getSQLState().startsWith("23"))
            return "Không thể xóa " + object + " vì đang có dữ liệu liên quan. Lịch sử đơn hàng được giữ nguyên.";
        return "Không thể " + ("delete".equals(action) ? "xóa " : "lưu thay đổi ") + object + " lúc này. Vui lòng thử lại sau.";
    }
}
