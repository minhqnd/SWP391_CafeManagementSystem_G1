package model;

import java.io.Serializable;

/**
 * ShopSettings - Cấu hình bán hàng online do Admin chỉnh ở /admin/settings (bảng shop_settings).
 * Các giá trị khởi tạo bên dưới là mặc định khi Database chưa có bảng / chưa có dòng cấu hình.
 */
public class ShopSettings implements Serializable {
    private static final long serialVersionUID = 1L;

    // Khoá trong bảng shop_settings
    public static final String KEY_FREESHIP_THRESHOLD = "freeship_threshold";
    public static final String KEY_DELIVERY_FEE = "delivery_fee";
    public static final String KEY_DELIVERY_MINUTES = "delivery_minutes";
    public static final String KEY_POINTS_STEP = "points_step";
    public static final String KEY_POINTS_STEP_VALUE = "points_step_value";

    private double freeshipThreshold = 150000;  // tiền hàng tối thiểu để miễn phí giao hàng (0 = mọi đơn)
    private double deliveryFee = 18000;         // phí giao hàng khi chưa đủ mức freeship
    private int deliveryMinutes = 30;           // thời gian giao dự kiến kể từ khi thanh toán
    private int pointsStep = 10;                // mốc điểm: khách dùng 10, 20, 30... điểm
    private double pointsStepValue = 2000;      // số tiền giảm cho mỗi mốc điểm

    public double getFreeshipThreshold() { return freeshipThreshold; }
    public void setFreeshipThreshold(double freeshipThreshold) { this.freeshipThreshold = freeshipThreshold; }

    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }

    public int getDeliveryMinutes() { return deliveryMinutes; }
    public void setDeliveryMinutes(int deliveryMinutes) { this.deliveryMinutes = deliveryMinutes; }

    public int getPointsStep() { return pointsStep; }
    public void setPointsStep(int pointsStep) { this.pointsStep = pointsStep; }

    public double getPointsStepValue() { return pointsStepValue; }
    public void setPointsStepValue(double pointsStepValue) { this.pointsStepValue = pointsStepValue; }

    /** Số tiền giảm khi dùng số điểm này - chỉ tính các mốc trọn vẹn (VD mốc 50: 120 điểm = 2 mốc) */
    public double getPointsDiscount(int points) {
        if (points <= 0 || pointsStep <= 0) {
            return 0;
        }
        return (points / pointsStep) * pointsStepValue;
    }

    /**
     * Số điểm tối đa được dùng cho 1 đơn (luôn là bội số của mốc điểm):
     * không vượt quá số điểm khách đang có và số tiền giảm không vượt quá tiền hàng.
     */
    public int getMaxRedeemablePoints(int availablePoints, double subtotal) {
        if (pointsStep <= 0 || pointsStepValue <= 0 || availablePoints <= 0 || subtotal <= 0) {
            return 0;
        }
        int stepsByPoints = availablePoints / pointsStep;
        int stepsBySubtotal = (int) Math.floor(subtotal / pointsStepValue);
        return Math.min(stepsByPoints, stepsBySubtotal) * pointsStep;
    }
}
