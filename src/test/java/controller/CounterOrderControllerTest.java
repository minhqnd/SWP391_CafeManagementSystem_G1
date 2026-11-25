package controller;

import model.Order;
import model.OrderDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CounterOrderController Unit Tests")
public class CounterOrderControllerTest {

    @Test
    void qrPollingRequiresAConfirmedBankTransferAndRejectsCancelledOrders() {
        Order order = new Order();
        order.setPaymentMethod("BANK_TRANSFER");
        order.setStatus("CONFIRMED");
        assertFalse(CounterOrderController.isVerifiedQrPayment(order));

        order.setPaidAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        assertTrue(CounterOrderController.isVerifiedQrPayment(order));

        order.setStatus("CANCELLED");
        assertFalse(CounterOrderController.isVerifiedQrPayment(order));

        order.setStatus("COMPLETED");
        assertTrue(CounterOrderController.isVerifiedQrPayment(order));
        order.setPaymentMethod("CASH");
        assertFalse(CounterOrderController.isVerifiedQrPayment(order));
    }

    @Test
    @DisplayName("TC-POS-01: Kiểm tra tính toán đơn POS tại quầy và định dạng tiền")
    void testPosOrderCalculation() {
        Order order = new Order();
        order.setId(201);
        order.setOrderCode("ORD-261005-0001");
        order.setOrderType("DINE_IN");
        order.setTableNumber("Bàn 03");
        order.setStatus("CONFIRMED");

        // Thêm 2 món
        OrderDetail item1 = new OrderDetail();
        item1.setProductId(1);
        item1.setProductName("Cà phê Muối");
        item1.setQuantity(2);
        item1.setUnitPrice(BigDecimal.valueOf(45000));
        item1.setSubtotal(BigDecimal.valueOf(90000));
        item1.setCustomization("Size L | 70% đường");
        order.getItems().add(item1);

        OrderDetail item2 = new OrderDetail();
        item2.setProductId(2);
        item2.setProductName("Trà Đào Cam Sả");
        item2.setQuantity(1);
        item2.setUnitPrice(BigDecimal.valueOf(50000));
        item2.setSubtotal(BigDecimal.valueOf(50000));
        order.getItems().add(item2);

        order.setTotalAmount(140000);
        order.setDiscountAmount(10000); // Dùng 10 điểm hội viên
        order.setDeliveryFee(0);
        // VAT 8% của (140.000 - 10.000) = 10.400 -> Tổng = 140.400
        order.setFinalAmount(140400);

        assertEquals("ORD-261005-0001", order.getOrderCode());
        assertEquals("Bàn 03", order.getTableNumber());
        assertEquals(3, order.getItemCount());
        assertEquals(140400, order.getFinalAmount());
        assertTrue(order.isPaid());
        assertEquals("Đã thanh toán", order.getStatusLabel());
    }

    @Test
    @DisplayName("TC-POS-02: Kiểm tra tóm tắt món cho hiển thị danh sách đơn quầy")
    void testPosItemSummary() {
        Order order = new Order();
        OrderDetail item1 = new OrderDetail();
        item1.setProductName("Bạc Xỉu Đá");
        item1.setQuantity(2);
        order.getItems().add(item1);

        OrderDetail item2 = new OrderDetail();
        item2.setProductName("Croissant Trứng Muối");
        item2.setQuantity(1);
        order.getItems().add(item2);

        assertEquals("2x Bạc Xỉu Đá, 1x Croissant Trứng Muối", order.getItemSummary());
    }

    @Test
    @DisplayName("TC-POS-03: Kiểm tra trạng thái đơn mang về và điểm tích lũy")
    void testPosTakeawayRewardPoints() {
        Order order = new Order();
        order.setOrderType("TAKEAWAY");
        order.setTotalAmount(120000);
        order.setDiscountAmount(0);
        order.setFinalAmount(129600);

        // Mỗi 10.000đ tiền hàng = 1 điểm
        assertEquals(12, order.getRewardPoints());
        assertFalse(order.isDelivery());
    }

    @Test
    void validateSearchPhoneChecksEmptyLengthAndFormat() {
        assertEquals("Vui lòng nhập số điện thoại khách hàng.", CounterOrderController.validateSearchPhone("  "));
        assertTrue(CounterOrderController.validateSearchPhone("0912").contains("không hợp lệ"));
        assertTrue(CounterOrderController.validateSearchPhone("abc123456").contains("không hợp lệ"));
        assertTrue(CounterOrderController.validateSearchPhone("0".repeat(21)).contains("20 ký tự"));
        assertNull(CounterOrderController.validateSearchPhone("0900000004"));
        assertNull(CounterOrderController.validateSearchPhone("+84 900-000.004"));
    }

    @Test
    void validateOrderFieldsAndItemQuantities() {
        assertEquals("Ghi chú đơn hàng không được vượt quá 500 ký tự.",
                CounterOrderController.validateOrderNote("a".repeat(501)));
        assertNull(CounterOrderController.validateOrderNote("Ít đá, thêm đường"));

        assertEquals("Số bàn không được vượt quá 20 ký tự.",
                CounterOrderController.validateTableNumber("a".repeat(21)));
        assertNull(CounterOrderController.validateTableNumber("Bàn 05"));

        OrderDetail badQty = new OrderDetail();
        badQty.setProductId(1);
        badQty.setQuantity(100);
        assertEquals("Số lượng mỗi món phải từ 1 đến 99.",
                CounterOrderController.validateItemQuantities(List.of(badQty)));

        OrderDetail valid = new OrderDetail();
        valid.setProductId(1);
        valid.setQuantity(2);
        valid.setCustomization("Size M | 100% đường | 100% đá");
        assertNull(CounterOrderController.validateItemQuantities(List.of(valid)));

        assertNotNull(CounterOrderController.validateOrderTotal(-1));
        assertNotNull(CounterOrderController.validateOrderTotal(10_000_000_000.00));
        assertNull(CounterOrderController.validateOrderTotal(150_000));
    }

    @Test
    void jsonParserHandlesEscapedQuotesAndBracesInNotes() {
        String payload = "{\"orderType\":\"DINE_IN\",\"orderNote\":\"Ghi chú \\\"đặc biệt\\\" } [test]\",\"cashGiven\":-5000,\"items\":[{\"productId\":1,\"productName\":\"Trà Đào\",\"quantity\":2,\"unitPrice\":45000,\"subtotal\":90000,\"customization\":\"Size M | Lưu ý: ly } \\\"riêng\\\"\"}]}";
        assertEquals("Ghi chú \"đặc biệt\" } [test]",
                CounterOrderController.extractJsonString(payload, "orderNote", ""));
        assertEquals(-5000.0, CounterOrderController.extractJsonDouble(payload, "cashGiven", 0));

        List<OrderDetail> items = CounterOrderController.parseOrderItemsFromJson(payload);
        assertEquals(1, items.size());
        assertEquals(1, items.get(0).getProductId());
        assertEquals(2, items.get(0).getQuantity());
        assertEquals("Size M | Lưu ý: ly } \"riêng\"", items.get(0).getCustomization());
    }
}
