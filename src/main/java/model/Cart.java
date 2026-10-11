package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Cart - Giỏ hàng của khách, lưu trong HttpSession (khách chưa đăng nhập vẫn dùng được)
 */
public class Cart implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "cart";
    public static final double FREESHIP_THRESHOLD = 150000;
    public static final double DELIVERY_FEE = 18000;
    public static final double MONEY_PER_POINT = 10000;
    public static final int MAX_QUANTITY = 99;

    public static final double DISCOUNT_PER_POINT = 500;

    private final List<CartItem> items = new ArrayList<>();
    private int nextLineId = 1;
    private int pointsUsed = 0;

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

    public int getPointsUsed() {
        return pointsUsed;
    }

    public void setPointsUsed(int pointsUsed) {
        this.pointsUsed = pointsUsed;
    }

    public double getDiscount() {
        return pointsUsed * DISCOUNT_PER_POINT;
    }

    /** Tổng tiền hàng sau giảm giá (chưa gồm phí giao hàng) */
    public double getTotal() {
        return getSelectedSubtotal() - getDiscount();
    }

    /** Mỗi 10.000đ chi tiêu tương đương 1 điểm Aura */
    public int getRewardPoints() {
        return (int) Math.floor(getTotal() / MONEY_PER_POINT);
    }

    public boolean isFreeshipReached() {
        return getSelectedSubtotal() >= FREESHIP_THRESHOLD;
    }

    public double getFreeshipRemaining() {
        return Math.max(0, FREESHIP_THRESHOLD - getSelectedSubtotal());
    }

    public int getFreeshipPercent() {
        return (int) Math.min(100, Math.floor(getSelectedSubtotal() * 100 / FREESHIP_THRESHOLD));
    }

    /** Phí giao hàng: miễn phí khi lấy tại quầy hoặc đơn từ 150.000đ */
    public double getDeliveryFee(boolean homeDelivery) {
        if (!homeDelivery || isFreeshipReached()) {
            return 0;
        }
        return DELIVERY_FEE;
    }
}
