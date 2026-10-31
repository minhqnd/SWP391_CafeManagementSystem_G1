package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShopSettings - Mốc điểm Aura do Admin cài đặt")
public class ShopSettingsTest {

    private ShopSettings settings(int pointsStep, double pointsStepValue) {
        ShopSettings settings = new ShopSettings();
        settings.setPointsStep(pointsStep);
        settings.setPointsStepValue(pointsStepValue);
        return settings;
    }

    @Test
    @DisplayName("Chỉ tính tiền giảm cho các mốc trọn vẹn")
    void testPointsDiscountUsesWholeSteps() {
        ShopSettings settings = settings(100, 15000);

        assertEquals(0, settings.getPointsDiscount(0));
        assertEquals(0, settings.getPointsDiscount(99));
        assertEquals(15000, settings.getPointsDiscount(100));
        assertEquals(15000, settings.getPointsDiscount(199));
        assertEquals(30000, settings.getPointsDiscount(200));
    }

    @Test
    @DisplayName("Điểm tối đa: không vượt số điểm đang có và tiền hàng")
    void testMaxRedeemablePoints() {
        ShopSettings settings = settings(50, 10000);

        assertEquals(150, settings.getMaxRedeemablePoints(180, 300000));  // giới hạn bởi số điểm
        assertEquals(100, settings.getMaxRedeemablePoints(500, 25000));   // giới hạn bởi tiền hàng
        assertEquals(50, settings.getMaxRedeemablePoints(500, 10000));    // giảm đúng bằng tiền hàng
        assertEquals(0, settings.getMaxRedeemablePoints(49, 300000));     // chưa đủ 1 mốc
        assertEquals(0, settings.getMaxRedeemablePoints(500, 9000));      // tiền hàng nhỏ hơn 1 mốc
        assertEquals(0, settings.getMaxRedeemablePoints(500, 0));
    }

    @Test
    @DisplayName("Cấu hình mặc định khi Database chưa có bảng shop_settings")
    void testDefaults() {
        ShopSettings settings = new ShopSettings();

        assertEquals(150000, settings.getFreeshipThreshold());
        assertEquals(25000, settings.getDeliveryFee());
        assertEquals(30, settings.getDeliveryMinutes());
        assertEquals(10, settings.getPointsStep());
        assertEquals(2000, settings.getPointsStepValue());
    }

}
