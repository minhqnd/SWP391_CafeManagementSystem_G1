package controller;

import model.Order;
import model.OrderDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BaristaController Unit Tests")
public class BaristaControllerTest {

    @Test
    @DisplayName("TC-BARISTA-01: Kiểm tra dashboardData chứa productId để tạo link xem công thức")
    void testDashboardDataContainsProductId() {
        Order order = new Order();
        order.setId(101);
        order.setOrderCode("ORD-101");
        order.setStatus("CONFIRMED");

        OrderDetail detail = new OrderDetail(
                1, 101, 5, "Cà phê Muối Aura", BigDecimal.valueOf(45000),
                "{\"size\":\"M\"}", 2, BigDecimal.valueOf(90000), 1, "Cà phê máy"
        );
        order.getItems().add(detail);

        Map<String, Object> data = BaristaController.dashboardData(Collections.singletonList(order));
        assertNotNull(data);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ordersList = (List<Map<String, Object>>) data.get("orders");
        assertEquals(1, ordersList.size());

        Map<String, Object> orderMap = ordersList.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsList = (List<Map<String, Object>>) orderMap.get("items");
        assertEquals(1, itemsList.size());

        Map<String, Object> itemMap = itemsList.get(0);
        assertTrue(itemMap.containsKey("productId"), "Item trong order phải chứa productId để Barista có thể bấm chuyển sang trang công thức");
        assertEquals(5, itemMap.get("productId"));
        assertEquals("Cà phê Muối Aura", itemMap.get("productName"));
    }

    @Test
    @DisplayName("TC-BARISTA-02: Kiểm tra trích xuất Recipe Instructions trên Model Product")
    void testProductRecipeInstructionsExtraction() {
        model.Product p = new model.Product();
        p.setVariantsConfig("{\"isAvailableOnline\": true, \"instructions\": \"Bước 1: Cho 25g cafe.\\nBước 2: Rót 40ml nước sôi.\"}");

        String instructions = p.getRecipeInstructions();
        assertNotNull(instructions);
        assertTrue(instructions.contains("Bước 1: Cho 25g cafe."));
        assertTrue(instructions.contains("Bước 2: Rót 40ml nước sôi."));
    }
}
