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
    @DisplayName("Voucher phần trăm có giới hạn mức giảm tối đa")
    void testPercentVoucherWithCap() {
        cart.addItem(item(1, "M", 100000, 3));
        cart.setVoucher(new Voucher(1, "WELCOME10", Voucher.TYPE_PERCENT, 10, 50000, 25000.0));

        assertEquals(300000, cart.getSelectedSubtotal());
        assertEquals(25000, cart.getDiscount());
        assertEquals(275000, cart.getTotal());
        assertEquals(27, cart.getRewardPoints());
    }

    @Test
    @DisplayName("Voucher chưa đạt giá trị tối thiểu thì không được giảm")
    void testVoucherBelowMinimumOrder() {
        cart.addItem(item(1, "M", 45000, 1));
        cart.setVoucher(new Voucher(2, "AURA20000", Voucher.TYPE_FIXED_AMOUNT, 20000, 100000, null));

        assertFalse(cart.isVoucherEligible());
        assertEquals(0, cart.getDiscount());
        assertEquals(45000, cart.getTotal());
    }

    @Test
    @DisplayName("Phí giao hàng: miễn phí từ 150.000đ hoặc khi lấy tại quầy")
    void testDeliveryFee() {
        cart.addItem(item(1, "M", 50000, 2));
        assertEquals(Cart.DELIVERY_FEE, cart.getDeliveryFee(true));
        assertEquals(0, cart.getDeliveryFee(false));
        assertEquals(50000, cart.getFreeshipRemaining());
        assertEquals(66, cart.getFreeshipPercent());

        cart.addItem(item(2, "M", 50000, 1));
        assertTrue(cart.isFreeshipReached());
        assertEquals(0, cart.getDeliveryFee(true));
        assertEquals(100, cart.getFreeshipPercent());
    }
}
