package model;

import java.io.Serializable;

/**
 * Voucher - Mã khuyến mãi áp dụng trên tổng tiền hàng (bảng vouchers)
 */
public class Voucher implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TYPE_PERCENT = "PERCENT";
    public static final String TYPE_FIXED_AMOUNT = "FIXED_AMOUNT";

    private int id;
    private String code;
    private String discountType;
    private double discountValue;
    private double minOrderValue;
    private Double maxDiscount;

    public Voucher() {
    }

    public Voucher(int id, String code, String discountType, double discountValue,
                   double minOrderValue, Double maxDiscount) {
        this.id = id;
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderValue = minOrderValue;
        this.maxDiscount = maxDiscount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }

    public double getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(double minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public Double getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(Double maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public boolean isPercent() {
        return TYPE_PERCENT.equals(discountType);
    }

    /**
     * Tổng tiền hàng đã đạt giá trị tối thiểu để dùng mã hay chưa
     */
    public boolean isEligible(double subtotal) {
        return subtotal > 0 && subtotal >= minOrderValue;
    }

    /**
     * Số tiền được giảm trên tổng tiền hàng (0 nếu chưa đủ điều kiện)
     */
    public double calculateDiscount(double subtotal) {
        if (!isEligible(subtotal)) {
            return 0;
        }
        double discount;
        if (isPercent()) {
            discount = Math.floor(subtotal * discountValue / 100);
            if (maxDiscount != null) {
                discount = Math.min(discount, maxDiscount);
            }
        } else {
            discount = discountValue;
        }
        return Math.min(discount, subtotal);
    }
}
