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
    public static final String KEY_POINTS_EARN_UNIT = "points_earn_unit";
    public static final String KEY_PAYMENT_TEST_MODE = "payment_test_mode";
    public static final String KEY_PAYMENT_GATEWAY_URL = "payment_gateway_url";
    public static final String KEY_PAYMENT_CALLBACK_SECRET = "payment_callback_secret";
    public static final String KEY_PAYMENT_BANK_ID = "payment_bank_id";
    public static final String KEY_PAYMENT_BANK_NAME = "payment_bank_name";
    public static final String KEY_PAYMENT_ACCOUNT_NO = "payment_account_no";
    public static final String KEY_PAYMENT_ACCOUNT_NAME = "payment_account_name";
    // Khung giờ ca không còn lưu ở đây: mỗi tuần có bộ ca riêng trong bảng work_shifts (/admin/shifts)

    private double freeshipThreshold = 150000;  // tiền hàng tối thiểu để miễn phí giao hàng (0 = mọi đơn)
    private double deliveryFee = 15000;         // phí giao tạm tính khi khách chưa chọn quận (phí thật theo delivery_areas)
    private int deliveryMinutes = 30;           // thời gian giao dự kiến kể từ khi thanh toán
    private int pointsStep = 10;                // mốc điểm: khách dùng 10, 20, 30... điểm
    private double pointsStepValue = 2000;      // số tiền giảm cho mỗi mốc điểm
    private double pointsEarnUnit = 10000;      // số tiền hàng sau giảm để nhận 1 điểm
    private boolean paymentTestMode = true;
    private String paymentGatewayUrl = "https://payment-gateway.zemorts.workers.dev"; // Live Cloudflare Worker URL
    private String paymentCallbackSecret = "AURA-LOCAL-DEMO-SECRET";
    private String paymentBankId = "VCB";
    private String paymentBankName = "Vietcombank";
    private String paymentAccountNo = "9888823456";
    private String paymentAccountName = "CONG TY TNHH AURA CAFE";

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

    public double getPointsEarnUnit() { return pointsEarnUnit; }
    public void setPointsEarnUnit(double pointsEarnUnit) { this.pointsEarnUnit = pointsEarnUnit; }

    public boolean isPaymentTestMode() { return paymentTestMode; }
    public void setPaymentTestMode(boolean paymentTestMode) { this.paymentTestMode = paymentTestMode; }

    public String getPaymentGatewayUrl() { return paymentGatewayUrl; }
    public void setPaymentGatewayUrl(String paymentGatewayUrl) {
        if (paymentGatewayUrl != null && !paymentGatewayUrl.isBlank()) {
            this.paymentGatewayUrl = paymentGatewayUrl.trim().replaceAll("/+$", "");
        }
    }

    public String getPaymentCallbackSecret() { return paymentCallbackSecret; }
    public void setPaymentCallbackSecret(String paymentCallbackSecret) {
        if (paymentCallbackSecret != null && !paymentCallbackSecret.isBlank()) {
            this.paymentCallbackSecret = paymentCallbackSecret;
        }
    }
    public String getPaymentBankId(){return paymentBankId;} public void setPaymentBankId(String v){if(v!=null&&!v.isBlank())paymentBankId=v;}
    public String getPaymentBankName(){return paymentBankName;} public void setPaymentBankName(String v){if(v!=null&&!v.isBlank())paymentBankName=v;}
    public String getPaymentAccountNo(){return paymentAccountNo;} public void setPaymentAccountNo(String v){if(v!=null&&!v.isBlank())paymentAccountNo=v;}
    public String getPaymentAccountName(){return paymentAccountName;} public void setPaymentAccountName(String v){if(v!=null&&!v.isBlank())paymentAccountName=v;}

    public int calculateEarnedPoints(double goodsAmountAfterDiscount) {
        if (goodsAmountAfterDiscount <= 0 || pointsEarnUnit <= 0) return 0;
        return (int) Math.floor(goodsAmountAfterDiscount / pointsEarnUnit);
    }
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
