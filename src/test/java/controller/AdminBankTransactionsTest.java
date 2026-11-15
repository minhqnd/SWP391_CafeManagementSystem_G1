package controller;

import controller.AdminBankTransactionsController.OrderSummary;
import model.BankTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
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
}
