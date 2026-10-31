package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cart - Giỏ hàng trong session")
public class CartTest {

    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = new Cart();
    }

    private CartItem item(int productId, String size, double unitPrice, int quantity, String... toppings) {
        CartItem item = new CartItem(productId, size, new ArrayList<>(List.of(toppings)), quantity);
        item.setUnitPrice(unitPrice);
        return item;
    }

    @Test
    @DisplayName("Thêm cùng món, cùng size, cùng topping thì gộp số lượng")
    void testAddSameItemMergesQuantity() {
        cart.addItem(item(1, "M", 55000, 1));
        cart.addItem(item(1, "M", 55000, 2));

        assertEquals(1, cart.getItems().size());
        assertEquals(3, cart.getItems().get(0).getQuantity());
    }

    @Test
    @DisplayName("Khác size hoặc topping thì tách thành dòng riêng")
    void testDifferentOptionsCreateNewLine() {
        cart.addItem(item(1, "M", 55000, 1));
        cart.addItem(item(1, "L", 65000, 1));
        cart.addItem(item(1, "M", 63000, 1, "Kem muối"));

        assertEquals(3, cart.getItems().size());
        assertEquals(3, cart.getTotalQuantity());
        assertNotEquals(cart.getItems().get(0).getLineId(), cart.getItems().get(1).getLineId());
    }

    @Test
    @DisplayName("Số lượng mỗi dòng luôn nằm trong khoảng 1 - 99")
    void testQuantityBounds() {
        cart.addItem(item(1, "M", 10000, 1));
        int lineId = cart.getItems().get(0).getLineId();

        cart.changeQuantity(lineId, -1);
        assertEquals(1, cart.findItem(lineId).getQuantity());

        cart.changeQuantity(lineId, 500);
        assertEquals(Cart.MAX_QUANTITY, cart.findItem(lineId).getQuantity());
    }

    @Test
    @DisplayName("Chỉ tính tiền các món được chọn và còn phục vụ")
    void testSelectedSubtotal() {
        cart.addItem(item(1, "M", 55000, 1));
        cart.addItem(item(2, "L", 72000, 2));
        cart.addItem(item(3, "M", 52000, 1));

        cart.toggleSelected(cart.getItems().get(2).getLineId());
        assertEquals(55000 + 144000, cart.getSelectedSubtotal());

        cart.getItems().get(1).setAvailable(false);
        assertEquals(55000, cart.getSelectedSubtotal());
        assertEquals(1, cart.getSelectedItems().size());
    }

    @Test
    @DisplayName("Chọn tất cả / Xoá các món đã chọn")
    void testSelectAllAndRemoveSelected() {
        cart.addItem(item(1, "M", 55000, 1));
        cart.addItem(item(2, "M", 45000, 1));
        assertTrue(cart.isAllSelected());

        cart.setAllSelected(false);
        assertFalse(cart.isAllSelected());
        assertTrue(cart.getSelectedItems().isEmpty());

        cart.toggleSelected(cart.getItems().get(0).getLineId());
        cart.removeSelectedItems();
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getProductId());
    }

    @Test
    @DisplayName("Dùng điểm Aura theo mốc: trừ thẳng vào tiền hàng")
    void testPointsDiscount() {
        cart.addItem(item(1, "M", 100000, 3));
        cart.setAvailablePoints(180);
        cart.setPointsUsed(100);

        // Mặc định mỗi 10 điểm giảm 2.000đ
        assertEquals(20000, cart.getDiscount());
        assertEquals(280000, cart.getTotal());
        assertEquals(28, cart.getRewardPoints());
        assertEquals(180, cart.getMaxRedeemablePoints());
        assertTrue(cart.normalizePointsUsed());
    }

    @Test
    @DisplayName("Số điểm đang dùng tự giảm về mốc hợp lệ khi tiền hàng / số điểm thay đổi")
    void testNormalizePointsUsed() {
        cart.addItem(item(1, "M", 25000, 1));
        cart.setAvailablePoints(500);
        assertEquals(120, cart.getMaxRedeemablePoints());   // giảm tối đa 24.000đ <= 25.000đ tiền hàng

        cart.setPointsUsed(150);
        assertFalse(cart.normalizePointsUsed());
        assertEquals(120, cart.getPointsUsed());

        cart.setAvailablePoints(65);                        // chỉ tính các mốc trọn vẹn: 60 điểm
        assertFalse(cart.normalizePointsUsed());
        assertEquals(60, cart.getPointsUsed());

        cart.setAvailablePoints(0);
        assertFalse(cart.normalizePointsUsed());
        assertEquals(0, cart.getPointsUsed());
        assertEquals(0, cart.getDiscount());
    }

    @Test
    @DisplayName("Phí giao hàng & mức Freeship theo cấu hình Admin")
    void testDeliveryFee() {
        cart.addItem(item(1, "M", 50000, 2));
        // Phí giao cố định 1 mức cho mọi địa chỉ
        assertEquals(25000, cart.getDeliveryFee());
        assertEquals(50000, cart.getFreeshipRemaining());
        assertEquals(66, cart.getFreeshipPercent());
        assertEquals(125000, cart.getGrandTotal());

        cart.addItem(item(2, "M", 50000, 1));
        assertTrue(cart.isFreeshipReached());
        assertEquals(0, cart.getDeliveryFee());
        assertEquals(100, cart.getFreeshipPercent());

        // Admin nâng mức Freeship và phí giao hàng
        ShopSettings settings = new ShopSettings();
        settings.setFreeshipThreshold(200000);
        settings.setDeliveryFee(25000);
        cart.setSettings(settings);
        assertFalse(cart.isFreeshipReached());
        assertEquals(25000, cart.getDeliveryFee());
        assertEquals(50000, cart.getFreeshipRemaining());
        assertEquals(175000, cart.getGrandTotal());

        // Mức Freeship = 0: mọi đơn đều miễn phí giao hàng
        settings.setFreeshipThreshold(0);
        assertEquals(0, cart.getDeliveryFee());
        assertEquals(100, cart.getFreeshipPercent());
    }

    @Test
    @DisplayName("Freeship tính trên tiền hàng trước khi trừ điểm")
    void testFreeshipBeforePoints() {
        cart.addItem(item(1, "M", 150000, 1));
        cart.setAvailablePoints(100);
        cart.setPointsUsed(100);

        assertEquals(130000, cart.getTotal());
        assertTrue(cart.isFreeshipReached());
        assertEquals(130000, cart.getGrandTotal());
    }

    @Test
    @DisplayName("Phí giao chỉ tính khi Giao tận nơi; Tự đến lấy / Dùng tại quán miễn phí (SRS 2.3.2)")
    void testDeliveryFeeOnlyForDelivery() {
        cart.addItem(item(1, "M", 45000, 1)); // dưới mức freeship 150.000đ

        assertEquals(Cart.FULFILLMENT_DELIVERY, cart.getFulfillment());
        assertEquals("ONLINE", cart.getOrderType());
        assertEquals(25000, cart.getDeliveryFee());
        assertEquals(70000, cart.getGrandTotal());

        cart.setFulfillment(Cart.FULFILLMENT_PICKUP);
        assertEquals("TAKEAWAY", cart.getOrderType());
        assertEquals(0, cart.getDeliveryFee());
        assertEquals(45000, cart.getGrandTotal());

        cart.setFulfillment(Cart.FULFILLMENT_DINE_IN);
        assertEquals("DINE_IN", cart.getOrderType());
        assertEquals(0, cart.getDeliveryFee());

        cart.setFulfillment("ABC"); // giá trị lạ -> mặc định Giao tận nơi
        assertEquals(Cart.FULFILLMENT_DELIVERY, cart.getFulfillment());
    }
}
