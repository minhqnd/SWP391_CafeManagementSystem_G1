package controller;

import controller.AdminBankTransactionsController.OrderSummary;
import model.BankTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AdminBankTransactions - Thuật toán đối soát & xử lý dữ liệu Gateway")
class AdminBankTransactionsTest {

    private final AdminBankTransactionsController controller = new AdminBankTransactionsController();

    @Test
    @DisplayName("TC-PAY-01: Trích xuất chính xác mã đơn hàng từ các dạng nội dung chuyển khoản")
    void testExtractOrderCode() throws Exception {
        Method method = AdminBankTransactionsController.class.getDeclaredMethod("extractOrderCode", String.class);
        method.setAccessible(true);

        // TH1: Nội dung từ SePay / MoMo dài có mã đơn AURA...
        String content1 = "153607292773-AURA261109114947194-CHUYEN TIEN-OQCH000NNckP-MOMO";
        assertEquals("AURA261109114947194", method.invoke(controller, content1));

        // TH2: Khách ghi thường có tiền tố AURA
        String content2 = "chuyen tien don hang aura261109114947194 cam on";
        assertEquals("AURA261109114947194", method.invoke(controller, content2));

        // TH3: Mã đơn dạng ORD-...
        String content3 = "THANH TOAN ORD-261109-1001";
        assertEquals("ORD-261109-1001", method.invoke(controller, content3));

        // TH4: Không có mã đơn hợp lệ
        String content4 = "Nguyen Van A chuyen tien mua cafe";
        assertNull(method.invoke(controller, content4));
    }

    @Test
    @DisplayName("TC-PAY-02: Chuyển đổi múi giờ UTC từ Cloudflare Worker sang giờ Việt Nam (+7 tiếng)")
    void testAdjustUtcToVietnamTime() throws Exception {
        Method method = AdminBankTransactionsController.class.getDeclaredMethod("adjustUtcToVietnamTime", String.class);
        method.setAccessible(true);

        // 04:50:14 UTC -> 11:50:14 ICT
        String utcTime = "2026-11-09 04:50:14";
        assertEquals("2026-11-09 11:50:14", method.invoke(controller, utcTime));

        // Xử lý chuỗi rỗng / null an toàn
        assertEquals("", method.invoke(controller, ""));
        assertEquals("", method.invoke(controller, (Object) null));
    }

    @Test
    @DisplayName("TC-PAY-03: Đối soát - Khớp chuẩn 100% khi số tiền chuyển khớp chính xác giá trị đơn")
    void testReconcileMatched() {
        BankTransaction tx = new BankTransaction();
        tx.setContent("AURA261109114947194 CHUYEN TIEN");
        tx.setTransferAmount(64000.0);

        Map<String, OrderSummary> orders = Map.of(
            "AURA261109114947194", new OrderSummary(1, "AURA261109114947194", 64000.0, "CONFIRMED")
        );

        List<BankTransaction> list = new ArrayList<>(List.of(tx));
        controller.reconcileTransactionsWithOrders(list, orders);

        assertEquals("MATCHED", tx.getReconcileStatus());
        assertEquals(0.0, tx.getDiffAmount());
        assertTrue(tx.getReconcileNote().contains("Khớp chuẩn 100%"));
    }

    @Test
    @DisplayName("TC-PAY-04: Đối soát - Chuyển thừa tiền (OVERPAID)")
    void testReconcileOverpaid() {
        BankTransaction tx = new BankTransaction();
        tx.setContent("AURA261109114947194 CHUYEN TIEN");
        tx.setTransferAmount(100000.0);

        Map<String, OrderSummary> orders = Map.of(
            "AURA261109114947194", new OrderSummary(1, "AURA261109114947194", 64000.0, "CONFIRMED")
        );

        List<BankTransaction> list = new ArrayList<>(List.of(tx));
        controller.reconcileTransactionsWithOrders(list, orders);

        assertEquals("OVERPAID", tx.getReconcileStatus());
        assertEquals(36000.0, tx.getDiffAmount());
        assertTrue(tx.getReconcileNote().contains("Thừa +36.000"));
    }

    @Test
    @DisplayName("TC-PAY-05: Đối soát - Chuyển thiếu tiền (UNDERPAID)")
    void testReconcileUnderpaid() {
        BankTransaction tx = new BankTransaction();
        tx.setContent("AURA261109114947194 CHUYEN TIEN");
        tx.setTransferAmount(50000.0);

        Map<String, OrderSummary> orders = Map.of(
            "AURA261109114947194", new OrderSummary(1, "AURA261109114947194", 64000.0, "CONFIRMED")
        );

        List<BankTransaction> list = new ArrayList<>(List.of(tx));
        controller.reconcileTransactionsWithOrders(list, orders);

        assertEquals("UNDERPAID", tx.getReconcileStatus());
        assertEquals(-14000.0, tx.getDiffAmount());
        assertTrue(tx.getReconcileNote().contains("Thiếu -14.000"));
    }

    @Test
    @DisplayName("TC-PAY-06: Đối soát - Chuyển trùng lặp (DUPLICATE) khi đơn đã thanh toán đủ")
    void testReconcileDuplicate() {
        BankTransaction tx1 = new BankTransaction();
        tx1.setContent("AURA261109114947194 LAN 1");
        tx1.setTransferAmount(64000.0);

        BankTransaction tx2 = new BankTransaction();
        tx2.setContent("AURA261109114947194 LAN 2 (CHUYEN TRUNG)");
        tx2.setTransferAmount(64000.0);

        // Danh sách theo thứ tự hiển thị: mới nhất (tx2) trước, cũ hơn (tx1) sau
        List<BankTransaction> list = new ArrayList<>(List.of(tx2, tx1));

        Map<String, OrderSummary> orders = Map.of(
            "AURA261109114947194", new OrderSummary(1, "AURA261109114947194", 64000.0, "CONFIRMED")
        );

        controller.reconcileTransactionsWithOrders(list, orders);

        assertEquals("MATCHED", tx1.getReconcileStatus());
        assertEquals("DUPLICATE", tx2.getReconcileStatus());
        assertTrue(tx2.getReconcileNote().contains("Trùng đơn"));
    }

    @Test
    @DisplayName("TC-PAY-07: Đối soát - Giao dịch không có mã đơn hàng gán nhãn NO_CODE")
    void testReconcileNoCode() {
        BankTransaction tx = new BankTransaction();
        tx.setContent("Chuyen tien ung ho quan cafe");
        tx.setTransferAmount(50000);

        List<BankTransaction> list = new ArrayList<>(List.of(tx));
        controller.reconcileTransactionsWithOrders(list, Map.of());

        assertEquals("NO_CODE", tx.getReconcileStatus());
        assertTrue(tx.getReconcileNote().contains("Không chứa mã đơn hàng"));
    }

    @Test
    @DisplayName("TC-PAY-08: Đối soát - Mã đơn không tồn tại trên hệ thống gán ORDER_NOT_FOUND")
    void testReconcileOrderNotFound() {
        BankTransaction tx = new BankTransaction();
        tx.setContent("AURA9999999999 thanh toan don");
        tx.setTransferAmount(50000);

        List<BankTransaction> list = new ArrayList<>(List.of(tx));
        controller.reconcileTransactionsWithOrders(list, Map.of());

        assertEquals("ORDER_NOT_FOUND", tx.getReconcileStatus());
        assertTrue(tx.getReconcileNote().contains("không có trên hệ thống"));
    }

    @Test
    @DisplayName("TC-PAY-09: Đối soát - Đơn hàng đã bị hủy gán nhãn ORDER_CANCELLED")
    void testReconcileOrderCancelled() {
        BankTransaction tx = new BankTransaction();
        tx.setContent("AURA261109114947194 CHUYEN TIEN");
        tx.setTransferAmount(64000.0);

        Map<String, OrderSummary> orders = Map.of(
            "AURA261109114947194", new OrderSummary(1, "AURA261109114947194", 64000.0, "CANCELLED")
        );

        List<BankTransaction> list = new ArrayList<>(List.of(tx));
        controller.reconcileTransactionsWithOrders(list, orders);

        assertEquals("ORDER_CANCELLED", tx.getReconcileStatus());
        assertTrue(tx.getReconcileNote().contains("đã bị hủy"));
    }

    @Test
    @DisplayName("TC-PAY-10: Trừ tiền thừa đã hoàn khỏi tổng tiền thực nhận (doanh thu)")
    void testNetInAmountWithOverpaidRefund() {
        BankTransaction tx = new BankTransaction();
        tx.setTransferAmount(100000.0);
        tx.setRefunded(true);
        tx.setRefundedAmount(30000.0);

        List<BankTransaction> list = List.of(tx);
        assertEquals(70000.0, controller.calculateNetInAmount(list));
        assertEquals(30000.0, controller.calculateTotalRefunded(list));
    }

    @Test
    @DisplayName("TC-PAY-11: Hoàn toàn bộ giao dịch thì tiền thực nhận bằng 0")
    void testNetInAmountWithFullRefund() {
        BankTransaction tx = new BankTransaction();
        tx.setTransferAmount(70000.0);
        tx.setRefunded(true);
        tx.setRefundedAmount(70000.0);

        List<BankTransaction> list = List.of(tx);
        assertEquals(0.0, controller.calculateNetInAmount(list));
        assertEquals(70000.0, controller.calculateTotalRefunded(list));
    }

    @Test
    @DisplayName("TC-PAY-12: Đối soát không phân biệt hoa thường với mã đơn có chữ thường")
    void testReconcileCaseInsensitiveOrderCode() {
        BankTransaction tx = new BankTransaction();
        tx.setContent("AURA2611161402cac5a CHUYEN TIEN THANH TOAN DON HANG");
        tx.setTransferAmount(70000.0);

        Map<String, OrderSummary> orders = Map.of(
            "AURA2611161402cac5a", new OrderSummary(1, "AURA2611161402cac5a", 70000.0, "CONFIRMED")
        );

        List<BankTransaction> list = new ArrayList<>(List.of(tx));
        controller.reconcileTransactionsWithOrders(list, orders);

        assertEquals("MATCHED", tx.getReconcileStatus());
    }

    @Test
    @DisplayName("TC-PAY-13: Validate hoàn tiền - Chấp nhận hoàn toàn bộ và hoàn một phần hợp lệ")
    void testValidateRefundValidCases() {
        assertNull(AdminBankTransactionsController.validateRefundInput(
                "FT26329030480203", "FT26329030480203", "AURA261125105027B6F",
                "ORDER_CANCELLED", "80000", "80000", "Hoàn tiền đơn hủy"
        ));
        assertNull(AdminBankTransactionsController.validateRefundInput(
                "SEPAY-102401220", "", "",
                "OVERPAID", "30000.50", "100000", ""
        ));
    }

    @Test
    @DisplayName("TC-PAY-14: Validate hoàn tiền - Chặn txKey rỗng hoặc vượt giới hạn")
    void testValidateRefundTxKey() {
        assertNotNull(AdminBankTransactionsController.validateRefundInput(
                "   ", "FT123", "", "NO_CODE", "50000", "50000", ""
        ));
        String tooLongKey = "K".repeat(AdminBankTransactionsController.MAX_TX_KEY_LENGTH + 1);
        assertNotNull(AdminBankTransactionsController.validateRefundInput(
                tooLongKey, "FT123", "", "NO_CODE", "50000", "50000", ""
        ));
    }

    @Test
    @DisplayName("TC-PAY-15: Validate hoàn tiền - Chặn số tiền rỗng, sai định dạng, <= 0, quá thập phân hoặc vượt MAX")
    void testValidateRefundAmountBounds() {
        assertNotNull(AdminBankTransactionsController.validateRefundInput(
                "FT123", "FT123", "", "NO_CODE", "", "50000", ""
        ));
        assertNotNull(AdminBankTransactionsController.validateRefundInput(
                "FT123", "FT123", "", "NO_CODE", "abc", "50000", ""
        ));
        assertNotNull(AdminBankTransactionsController.validateRefundInput(
                "FT123", "FT123", "", "NO_CODE", "0", "50000", ""
        ));
        assertNotNull(AdminBankTransactionsController.validateRefundInput(
                "FT123", "FT123", "", "NO_CODE", "-1000", "50000", ""
        ));
        assertNotNull(AdminBankTransactionsController.validateRefundInput(
                "FT123", "FT123", "", "NO_CODE", "1000.123", "50000", ""
        ));
        assertNotNull(AdminBankTransactionsController.validateRefundInput(
                "FT123", "FT123", "", "NO_CODE", "10000000000.00", "10000000000.00", ""
        ));
    }

    @Test
    @DisplayName("TC-PAY-16: Validate hoàn tiền - Chặn hoàn vượt quá số tiền giao dịch gốc")
    void testValidateRefundExceedsTransferAmount() {
        String err = AdminBankTransactionsController.validateRefundInput(
                "FT123", "FT123", "AURA1", "OVERPAID", "90000", "80000", "Hoàn quá tay"
        );
        assertNotNull(err);
        assertTrue(err.contains("vượt quá số tiền của giao dịch"));

        assertNotNull(AdminBankTransactionsController.validateRefundInput(
                "FT123", "FT123", "AURA1", "OVERPAID", "50000", "0", ""
        ));
    }

    @Test
    @DisplayName("TC-PAY-17: Validate hoàn tiền - Chặn ghi chú quá 255 ký tự và trạng thái đối soát lạ")
    void testValidateRefundNoteAndStatus() {
        String longNote = "N".repeat(AdminBankTransactionsController.MAX_REFUND_NOTE_LENGTH + 1);
        assertNotNull(AdminBankTransactionsController.validateRefundInput(
                "FT123", "FT123", "", "NO_CODE", "50000", "50000", longNote
        ));
        assertNotNull(AdminBankTransactionsController.validateRefundInput(
                "FT123", "FT123", "", "UNKNOWN_STATUS", "50000", "50000", "OK"
        ));
    }

    @Test
    @DisplayName("TC-PAY-18: Dịch lỗi SQL an toàn, không lộ thông tin kỹ thuật thô ra UI")
    void testRefundErrorMessageTranslation() {
        SQLException tooLong = new SQLException("Data truncation: Data too long for column 'refund_note'", "22001", 1406);
        String msg1 = AdminBankTransactionsController.refundErrorMessage(tooLong);
        assertFalse(msg1.contains("Data truncation"));
        assertTrue(msg1.contains("độ dài"));

        SQLException outOfRange = new SQLException("Out of range value for column 'refund_amount'", "22003", 1264);
        String msg2 = AdminBankTransactionsController.refundErrorMessage(outOfRange);
        assertFalse(msg2.contains("Out of range"));

        SQLException constraint = new SQLException("Duplicate entry", "23000", 1062);
        String msg3 = AdminBankTransactionsController.refundErrorMessage(constraint);
        assertFalse(msg3.contains("Duplicate entry"));

        RuntimeException generic = new RuntimeException("Communications link failure to jdbc:mysql://localhost:3306");
        String msg4 = AdminBankTransactionsController.refundErrorMessage(generic);
        assertFalse(msg4.contains("jdbc:mysql"));
    }
}
