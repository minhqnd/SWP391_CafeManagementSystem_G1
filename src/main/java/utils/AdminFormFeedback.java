package utils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

public final class AdminFormFeedback {
    private AdminFormFeedback() { }

    // . Đầu doPost của trang sản phẩm hoặc danh mục, em gọi clear để bỏ kết quả và bản nhập dở của lần gửi
    //   trước. Lần xử lý mới sẽ đặt đúng thông báo và giữ lại form của chính lần này nếu có lỗi.
    public static void clear(HttpSession session, String prefix) {
        session.removeAttribute(prefix + "Message");
        session.removeAttribute(prefix + "Error");
        session.removeAttribute(prefix + "Draft");
    }

    // [DMAN04]
    // . Trước khi đọc các ô và ảnh, controller gọi readMultipart trong AdminFormFeedback.java để đọc biểu
    //   mẫu có tệp đính kèm. Quá giới hạn thì báo dung lượng; lỗi đọc khác thì báo chưa đọc được biểu mẫu.
    //   Em trả false để controller quay về trang và chưa lưu, true thì controller tiếp tục nhận dữ liệu.
    // ... Quay lại doPost trong AdminCategoriesController.java hoặc AdminProductsController.java ngay sau
    //     AdminFormFeedback.readMultipart; false thì chuyển về trang báo lỗi, true mới đọc các ô để xử lý.
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

    // . Khi lưu hoặc xóa gặp lỗi dữ liệu, controller gọi databaseError với thao tác và tên đối tượng.
    //   Em phân biệt trùng tên, không xóa được vì có dữ liệu liên quan và lỗi lưu khác, rồi trả lời báo
    //   phù hợp về controller để hiển thị; chi tiết lỗi kỹ thuật không được đưa ra cho người dùng.
    public static String databaseError(SQLException error, String action, String object) {
        if (error.getErrorCode() == 1062)
            return "Tên " + object + " đã tồn tại. Vui lòng chọn tên khác.";
        if ("delete".equals(action) && error.getSQLState() != null && error.getSQLState().startsWith("23"))
            return "Không thể xóa " + object + " vì đang có dữ liệu liên quan. Lịch sử đơn hàng được giữ nguyên.";
        return "Không thể " + ("delete".equals(action) ? "xóa " : "lưu thay đổi ") + object + " lúc này. Vui lòng thử lại sau.";
    }
}
