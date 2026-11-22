package utils;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import support.ServletMocks;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class AdminFormFeedbackTest {
    @Test void onlyRealUploadLimitFailureMentionsTheFileSize() {
        for (boolean oversized : new boolean[]{true, false}) {
            ServletMocks mock = new ServletMocks();
            HttpServletRequest request = ServletMocks.proxy(HttpServletRequest.class, (p,m,a) -> switch (m.getName()) {
                case "getContentType" -> "multipart/form-data";
                case "getParts" -> throw oversized ? new SizeLimitExceededException() : new IllegalStateException("private internal failure");
                default -> null;
            });
            assertFalse(AdminFormFeedback.readMultipart(request, mock.session, "product"));
            String message = mock.sessionValues.get("productError").toString();
            assertEquals(oversized, message.contains("5 MB"));
            assertTrue(message.contains("chưa được lưu"));
            assertFalse(message.contains("private"));
        }
    }

    @Test void integrityFailureIsNotAutomaticallyReportedAsDuplicateName() {
        assertTrue(AdminFormFeedback.databaseError(new SQLException("private", "23000", 1062), "save", "sản phẩm").contains("đã tồn tại"));
        String other = AdminFormFeedback.databaseError(new SQLException("private", "23000", 1452), "save", "sản phẩm");
        assertFalse(other.contains("đã tồn tại")); assertFalse(other.contains("private"));
        assertTrue(AdminFormFeedback.databaseError(new SQLException("private", "23000", 1451), "delete", "danh mục").contains("Lịch sử đơn hàng"));
    }

    private static class SizeLimitExceededException extends IllegalStateException { }
}
