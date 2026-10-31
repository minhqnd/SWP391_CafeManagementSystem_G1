package model;

import java.io.Serializable;

/**
 * DeliveryArea - Quận/Huyện quán giao tới và phí giao cố định của quận đó (bảng delivery_areas).
 * Admin chỉnh ở /admin/delivery-areas. Phí giao của đơn = deliveryFee của quận khách chọn
 * (miễn phí khi tiền hàng đạt mức freeship).
 */
public class DeliveryArea implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String district;
    private double deliveryFee;
    private boolean active = true;

    public DeliveryArea() {
    }

    public DeliveryArea(int id, String district, double deliveryFee, boolean active) {
        this.id = id;
        this.district = district;
        this.deliveryFee = deliveryFee;
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
