package controller;

import model.Order;
import model.ShopSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaymentController - Sinh mã VietQR & Tiện ích thanh toán")
class PaymentControllerTest {

    private final PaymentController controller = new PaymentController();

    @Test
    @DisplayName("TC-PAY-05: Sinh URL VietQR chuẩn với đầy đủ tham số ngân hàng và mã đơn")
    void testBuildQrUrl() throws Exception {
        ShopSettings settings = new ShopSettings();
        settings.setPaymentBankId("970422"); // MBBank
        settings.setPaymentAccountNo("234556688");
        settings.setPaymentAccountName("NGUYEN DUC MINH");

        Order order = new Order();
        order.setOrderCode("AURA261109114947194");
        order.setFinalAmount(64000.0);

        Method method = PaymentController.class.getDeclaredMethod("buildQrUrl", Order.class, ShopSettings.class);
        method.setAccessible(true);
        String qrUrl = (String) method.invoke(controller, order, settings);

        assertNotNull(qrUrl);
        assertTrue(qrUrl.startsWith("https://vietqr.app/img?"));
        assertTrue(qrUrl.contains("bank=970422"));
        assertTrue(qrUrl.contains("acc=234556688"));
        assertTrue(qrUrl.contains("amount=64000"));
        assertTrue(qrUrl.contains("des=AURA261109114947194"));
        assertTrue(qrUrl.contains("template=compact"));
    }

    @Test
    @DisplayName("TC-PAY-06: Escape JSON và Parse số an toàn")
    void testUtilityMethods() throws Exception {
        Method escapeMethod = PaymentController.class.getDeclaredMethod("escapeJson", String.class);
        escapeMethod.setAccessible(true);

        assertEquals("test\\\"quote", escapeMethod.invoke(null, "test\"quote"));
        assertEquals("path\\\\file", escapeMethod.invoke(null, "path\\file"));
        assertEquals("", escapeMethod.invoke(null, (Object) null));

        Method parseMethod = PaymentController.class.getDeclaredMethod("parseInt", String.class);
        parseMethod.setAccessible(true);

        assertEquals(123, parseMethod.invoke(controller, "123"));
        assertEquals(456, parseMethod.invoke(controller, "  456  "));
        assertEquals(-1, parseMethod.invoke(controller, "abc"));
        assertEquals(-1, parseMethod.invoke(controller, (Object) null));
    }
}
