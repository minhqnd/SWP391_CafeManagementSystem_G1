package service;

import java.math.BigDecimal;

/** Shared validation for persisted DECIMAL quantities and workflow text. */
public final class InventoryRules {
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
    public static void receipt(BigDecimal delivered, BigDecimal accepted) {
        quantity(delivered,false);quantity(accepted,false);
        if(accepted.compareTo(delivered)>0)throw new IllegalArgumentException("Số lượng nhận đạt không được vượt số lượng giao.");
    }
    public static boolean editableRequest(String status) {return "DRAFT".equals(status)||"CHANGES_REQUESTED".equals(status)||"PENDING_APPROVAL".equals(status);}
}
