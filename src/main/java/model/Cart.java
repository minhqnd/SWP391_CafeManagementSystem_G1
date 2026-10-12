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
    public static final double MONEY_PER_POINT = 10000;
    public static final int MAX_QUANTITY = 99;

    private final List<CartItem> items = new ArrayList<>();
    private int nextLineId = 1;
    private ShopSettings settings = new ShopSettings();
    private int availablePoints;   // điểm Aura khách đang có (0 nếu chưa đăng nhập)
    private int pointsUsed;        // số điểm khách chọn trừ vào đơn, luôn là bội số của mốc điểm

    public List<CartItem> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Thêm món vào giỏ, gộp số lượng nếu đã có dòng cùng món, size và topping
     */
    public void addItem(CartItem newItem) {
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

    public CartItem findItem(int lineId) {
        for (CartItem item : items) {
            if (item.getLineId() == lineId) {
                return item;
            }
        }
        return null;
    }

    public void changeQuantity(int lineId, int delta) {
        CartItem item = findItem(lineId);
        if (item != null) {
            int newQuantity = item.getQuantity() + delta;
            item.setQuantity(Math.max(1, Math.min(MAX_QUANTITY, newQuantity)));
        }
    }

    public void removeItem(int lineId) {
        items.removeIf(item -> item.getLineId() == lineId);
    }

    public void removeSelectedItems() {
        items.removeIf(CartItem::isSelected);
    }

    public void toggleSelected(int lineId) {
        CartItem item = findItem(lineId);
        if (item != null) {
            item.setSelected(!item.isSelected());
        }
    }

    public void setAllSelected(boolean selected) {
        for (CartItem item : items) {
            item.setSelected(selected);
        }
    }

    public boolean isAllSelected() {
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
    public int getTotalQuantity() {
        int total = 0;
        for (CartItem item : items) {
            total += item.getQuantity();
        }
        return total;
    }

    /** Các món được tích chọn và vẫn còn phục vụ - là các món sẽ được đặt */
    public List<CartItem> getSelectedItems() {
        List<CartItem> selectedItems = new ArrayList<>();
        for (CartItem item : items) {
            if (item.isSelected() && item.isAvailable()) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }

    public int getSelectedQuantity() {
        int total = 0;
        for (CartItem item : getSelectedItems()) {
            total += item.getQuantity();
        }
        return total;
    }

    public double getSelectedSubtotal() {
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
        return (int) Math.floor(getTotal() / MONEY_PER_POINT);
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

    /** Phí giao hàng: miễn phí khi tiền hàng đạt mức freeship Admin cài đặt */
    public double getDeliveryFee() {
        return isFreeshipReached() ? 0 : settings.getDeliveryFee();
    }

    /** Khách cần trả = tiền hàng - điểm + phí giao hàng */
    public double getGrandTotal() {
        return getTotal() + getDeliveryFee();
    }
}
