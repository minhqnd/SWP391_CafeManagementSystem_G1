package service;

import model.CartItem;
import model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CartService - Tính giá món theo variants_config")
public class CartServiceTest {

    // Định dạng MySQL trả về cho cột JSON (khoá được sắp xếp lại, có khoảng trắng)
    private static final String CONFIG = "{\"sizes\": {\"L\": 10000, \"M\": 0}, "
            + "\"toppings\": [{\"name\": \"Extra kem muối\", \"price\": 8000}, {\"name\": \"Extra shot espresso\", \"price\": 10000}]}";

    private final CartService cartService = new CartService();

    private Product product(String variantsConfig) {
        return new Product(1, 1, "Cà phê máy", "Cà phê Muối Aura", 55000, null, null, null,
                variantsConfig, "https://example.com/coffee.jpg", true);
    }

    private CartItem priced(String variantsConfig, String size, String... toppings) {
        CartItem item = new CartItem(1, size, new ArrayList<>(List.of(toppings)), 2);
        cartService.applyProduct(item, product(variantsConfig));
        return item;
    }

    @Test
    @DisplayName("Đọc size và topping từ variants_config")
    void testParseOptions() {
        Map<String, Double> sizes = CartService.getSizeOptions(CONFIG);
        assertEquals(2, sizes.size());
        assertEquals(10000.0, sizes.get("L"));
        assertEquals(0.0, sizes.get("M"));

        Map<String, Double> toppings = CartService.getToppingOptions(CONFIG);
        assertEquals(8000.0, toppings.get("Extra kem muối"));
        assertEquals(10000.0, toppings.get("Extra shot espresso"));
    }

    @Test
    @DisplayName("Topping dạng chuỗi dùng giá mặc định 8.000đ")
    void testStringToppingsUseDefaultPrice() {
        Map<String, Double> toppings = CartService.getToppingOptions("{\"sizes\":{\"M\":0},\"toppings\":[\"Pearl\",\"Pudding\"]}");
        assertEquals(CartService.DEFAULT_TOPPING_PRICE, toppings.get("Pearl"));
        assertEquals(CartService.DEFAULT_TOPPING_PRICE, toppings.get("Pudding"));
    }

    @Test
    @DisplayName("Đơn giá = giá gốc + phụ thu size + topping")
    void testUnitPrice() {
        CartItem item = priced(CONFIG, "L", "Extra kem muối");

        assertEquals(55000 + 10000 + 8000, item.getUnitPrice());
        assertEquals(2 * 73000, item.getSubtotal());
        assertEquals("Cà phê Muối Aura", item.getProductName());
        assertEquals("Cà phê máy", item.getCategoryName());
    }

    @Test
    @DisplayName("Size không hợp lệ dùng size rẻ nhất, topping lạ bị bỏ qua")
    void testInvalidOptionsAreNormalized() {
        CartItem item = priced(CONFIG, "XXL", "Topping không tồn tại", "Extra shot espresso");

        assertEquals("M", item.getSize());
        assertEquals(List.of("Extra shot espresso"), item.getToppings());
        assertEquals(55000 + 10000, item.getUnitPrice());
    }

    @Test
    @DisplayName("Món chưa cấu hình size dùng Tiêu chuẩn / M / L như trang Menu")
    void testDefaultSizesWhenNotConfigured() {
        assertEquals(CartService.STANDARD_SIZE, priced(null, "").getSize());
        assertEquals(55000, priced(null, "M").getUnitPrice());
        assertEquals(65000, priced(null, "L").getUnitPrice());
    }
}
