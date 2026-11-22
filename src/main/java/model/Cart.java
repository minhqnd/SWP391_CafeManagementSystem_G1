package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Cart - Giỏ hàng của khách, lưu trong HttpSession (khách chưa đăng nhập vẫn dùng được).
 * Mức freeship, phí giao hàng và mốc điểm lấy từ cấu hình Admin (CartService.refreshCart nạp lại mỗi lần xem giỏ).
 */
public class Cart implements Serializable {
    private static final long serialVersionUID = 2L;

    public static final String SESSION_KEY = "cart";
    /** Legacy default only; runtime earning uses ShopSettings.pointsEarnUnit. */
    public static final double MONEY_PER_POINT = 10000;
    public static final int MAX_QUANTITY = 99;

    private final List<CartItem> items = new ArrayList<>();
    private int nextLineId = 1;
    private ShopSettings settings = new ShopSettings();
    private int availablePoints;   // điểm Aura khách đang có (0 nếu chưa đăng nhập)
    private int pointsUsed;        // số điểm khách chọn trừ vào đơn, luôn là bội số của mốc điểm

    public synchronized List<CartItem> getItems() {
        return items;
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Thêm món vào giỏ, gộp số lượng nếu đã có dòng cùng món, size và topping
     */
    public synchronized void addItem(CartItem newItem) {
        for (CartItem item : items) {
            if (item.hasSameOptions(newItem)) {
                item.setQuantity(Math.min(MAX_QUANTITY, item.getQuantity() + newItem.getQuantity()));
                item.setSelected(true);
                return;
            }
        }
        newItem.setQuantity(Math.min(MAX_QUANTITY, newItem.getQuantity()));
        newItem.setLineId(nextLineId++);
        items.add(newItem);
    }

    public synchronized CartItem findItem(int lineId) {
        for (CartItem item : items) {
            if (item.getLineId() == lineId) {
                return item;
            }
        }
        return null;
    }

    public synchronized void changeQuantity(int lineId, int delta) {
        CartItem item = findItem(lineId);
        if (item != null) {
            int newQuantity = item.getQuantity() + delta;
            item.setQuantity(Math.max(1, Math.min(MAX_QUANTITY, newQuantity)));
        }
    }

    public synchronized void removeItem(int lineId) {
        items.removeIf(item -> item.getLineId() == lineId);
    }

    public synchronized void removeSelectedItems() {
        items.removeIf(CartItem::isSelected);
    }

    public synchronized void clear() {
        items.clear();
        pointsUsed = 0;
    }

    public synchronized void toggleSelected(int lineId) {
        CartItem item = findItem(lineId);
        if (item != null) {
            item.setSelected(!item.isSelected());
        }
    }

    public synchronized void setAllSelected(boolean selected) {
        for (CartItem item : items) {
            item.setSelected(selected);
        }
    }

    public synchronized boolean isAllSelected() {
        if (items.isEmpty()) {
            return false;
        }
        for (CartItem item : items) {
            if (!item.isSelected()) {
                return false;
            }
        }
        return true;
    }

    /** Tổng số ly/phần trong giỏ (hiển thị trên badge header) */
    public synchronized int getTotalQuantity() {
        int total = 0;
        for (CartItem item : items) {
            total += item.getQuantity();
        }
        return total;
    }

    /** Các món được tích chọn và vẫn còn phục vụ - là các món sẽ được đặt */
    public synchronized List<CartItem> getSelectedItems() {
        List<CartItem> selectedItems = new ArrayList<>();
        for (CartItem item : items) {
            if (item.isSelected() && item.isAvailable()) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }

    public synchronized int getSelectedQuantity() {
        int total = 0;
        for (CartItem item : getSelectedItems()) {
            total += item.getQuantity();
        }
        return total;
    }

    public synchronized double getSelectedSubtotal() {
        double total = 0;
        for (CartItem item : getSelectedItems()) {
            total += item.getSubtotal();
        }
        return total;
    }

    public ShopSettings getSettings() {
        return settings;
    }

    public void setSettings(ShopSettings settings) {
        this.settings = settings != null ? settings : new ShopSettings();
    }

    public int getAvailablePoints() {
        return availablePoints;
    }

    public void setAvailablePoints(int availablePoints) {
        this.availablePoints = Math.max(0, availablePoints);
    }

    public int getPointsUsed() {
        return pointsUsed;
    }

    public void setPointsUsed(int pointsUsed) {
        this.pointsUsed = Math.max(0, pointsUsed);
    }

    /** Số điểm tối đa dùng được cho các món đang chọn (bội số của mốc điểm) */
    public int getMaxRedeemablePoints() {
        return settings.getMaxRedeemablePoints(availablePoints, getSelectedSubtotal());
    }

    /**
     * Đưa số điểm đang dùng về mốc hợp lệ gần nhất sau khi giỏ hàng, số điểm hiện có hoặc cấu hình mốc điểm thay đổi.
     * Trả về false nếu đã phải điều chỉnh.
     */
    public boolean normalizePointsUsed() {

        int validPoints = Math.min(pointsUsed, getMaxRedeemablePoints());
        if (settings.getPointsStep() > 0) {
            validPoints -= validPoints % settings.getPointsStep();
        }
        validPoints = Math.max(0, validPoints);
        boolean unchanged = validPoints == pointsUsed;
        pointsUsed = validPoints;
        return unchanged;
    }

    /** Số tiền giảm nhờ dùng điểm Aura (không vượt quá tiền hàng) */
    public double getDiscount() {
        return Math.min(settings.getPointsDiscount(pointsUsed), getSelectedSubtotal());
    }

    /** Tổng tiền hàng sau khi trừ điểm (chưa gồm phí giao hàng) */
    public double getTotal() {
        return getSelectedSubtotal() - getDiscount();
    }

    /** Mỗi 10.000đ chi tiêu tương đương 1 điểm Aura */
    public int getRewardPoints() {
        return settings.calculateEarnedPoints(getTotal());
    }

    /** Freeship tính trên tiền các món đã chọn (trước khi trừ điểm) */
    public boolean isFreeshipReached() {
        return getSelectedSubtotal() >= settings.getFreeshipThreshold();
    }

    public double getFreeshipRemaining() {
        return Math.max(0, settings.getFreeshipThreshold() - getSelectedSubtotal());
    }

    public int getFreeshipPercent() {
        if (settings.getFreeshipThreshold() <= 0) {
            return 100;
        }
        return (int) Math.min(100, Math.floor(getSelectedSubtotal() * 100 / settings.getFreeshipThreshold()));
    }

    // ===== Hình thức nhận hàng (SRS 2.3.2): DELIVERY = giao tận nơi, PICKUP = tự đến lấy, DINE_IN = dùng tại quán =====
    public static final String FULFILLMENT_DELIVERY = "DELIVERY";
    public static final String FULFILLMENT_PICKUP = "PICKUP";
    public static final String FULFILLMENT_DINE_IN = "DINE_IN";

    private String fulfillment = FULFILLMENT_DELIVERY;

    public String getFulfillment() {
        return fulfillment == null ? FULFILLMENT_DELIVERY : fulfillment;
    }

    /** Giá trị lạ thì giữ mặc định Giao tận nơi */
    public void setFulfillment(String fulfillment) {
        if (FULFILLMENT_PICKUP.equals(fulfillment) || FULFILLMENT_DINE_IN.equals(fulfillment)) {
            this.fulfillment = fulfillment;
        } else {
            this.fulfillment = FULFILLMENT_DELIVERY;
        }
    }

    public boolean isDelivery() {
        return FULFILLMENT_DELIVERY.equals(getFulfillment());
    }

    /** orders.order_type tương ứng: ONLINE (giao tận nơi), TAKEAWAY (tự đến lấy), DINE_IN (tại quán) */
    public String getOrderType() {
        if (FULFILLMENT_PICKUP.equals(getFulfillment())) return "TAKEAWAY";
        if (FULFILLMENT_DINE_IN.equals(getFulfillment())) return "DINE_IN";
        return "ONLINE";
    }

    /**
     * Phí giao hàng: chỉ tính khi giao tận nơi, miễn phí khi tiền hàng đạt mức freeship Admin cài đặt;
     * còn lại = 1 mức phí cố định cho mọi địa chỉ (delivery_fee, Admin chỉnh ở Cài đặt).
     */
    public double getDeliveryFee() {
        if (!isDelivery()) {
            return 0;
        }
        if (isFreeshipReached()) {
            return 0;
        }
        return settings.getDeliveryFee();
    }

    /** Khách cần trả = tiền hàng - điểm + phí giao hàng */
    public double getGrandTotal() {
        return getTotal() + getDeliveryFee();
    }
}
