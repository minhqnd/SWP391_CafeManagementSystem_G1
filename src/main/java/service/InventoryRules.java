package service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared validation for persisted DECIMAL quantities and workflow text. */
public final class InventoryRules {
    public static final int PURCHASE_PRICE_SCALE = 12;
    private static final BigDecimal MAX_MONEY = new BigDecimal("9999999999.99");
    private InventoryRules() {}
    public static BigDecimal quantity(BigDecimal value, boolean positive) {
        if(value==null || value.signum()<0 || (positive && value.signum()==0) || value.scale()>3 || value.compareTo(new BigDecimal("999999999.999"))>0)
            throw new IllegalArgumentException("Số lượng không hợp lệ (tối đa 3 chữ số thập phân).");
        return value;
    }
    public static String text(String value, boolean required) {
        String s=value==null?"":value.trim();
        if(s.length()>500 || (required && s.isEmpty()))throw new IllegalArgumentException("Vui lòng nhập lý do/ghi chú, tối đa 500 ký tự.");
        return s;
    }
    public static BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.scale() > 2 || value.compareTo(MAX_MONEY) > 0)
            throw new IllegalArgumentException("Tổng tiền không hợp lệ (tối đa 2 chữ số thập phân).");
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
    public static BigDecimal purchaseUnitPrice(BigDecimal quantity, BigDecimal total) {
        quantity(quantity, true);
        money(total);
        // 12 fractional digits keep rounding error below half a cent even at the maximum quantity.
        return total.divide(quantity, PURCHASE_PRICE_SCALE, RoundingMode.HALF_UP);
    }
    public static void receipt(BigDecimal delivered, BigDecimal accepted) {
        quantity(delivered,false);quantity(accepted,false);
        if(accepted.compareTo(delivered)>0)throw new IllegalArgumentException("Số lượng nhận đạt không được vượt số lượng giao.");
    }
    public static boolean editableRequest(String status) {return "DRAFT".equals(status)||"CHANGES_REQUESTED".equals(status)||"PENDING_APPROVAL".equals(status);}
}
