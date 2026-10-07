-- =========================================================
-- Đổi điểm Aura -> Voucher riêng của khách, dùng ở Giỏ hàng
-- Chạy SAU update_rewards.sql (cần có bảng reward_items, user_rewards). Chỉ chạy 1 lần.
--
-- Quy tắc:
--   * Điểm tích luỹ chỉ dùng để đổi voucher, KHÔNG trừ thẳng vào giá.
--   * Mỗi lần đổi điểm sinh ra 1 voucher (dùng 1 lần, có hạn) của riêng khách đó.
--   * Voucher giảm giá được chọn ở Giỏ hàng; Thanh toán chỉ chuyển khoản VietQR.
--   * Thanh toán thành công: cộng điểm = (tiền hàng - giảm giá) / 10.000đ.
-- =========================================================
SET NAMES utf8mb4;
USE aura_cafe_erp;

-- 1. Voucher của riêng một khách (NULL = mã công khai, ai cũng dùng được)
ALTER TABLE vouchers
    ADD COLUMN customer_id INT NULL AFTER code,
    ADD CONSTRAINT fk_voucher_customer
        FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE;

-- 2. Quà đổi điểm là voucher giảm tiền: mức giảm, đơn tối thiểu, số ngày hiệu lực
ALTER TABLE reward_items
    ADD COLUMN discount_value DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER points_cost,
    ADD COLUMN min_order_value DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER discount_value,
    ADD COLUMN valid_days INT NOT NULL DEFAULT 30 AFTER min_order_value;

-- 3. Mỗi lần đổi điểm lưu lại voucher đã sinh ra
ALTER TABLE user_rewards
    ADD COLUMN voucher_id INT NULL AFTER reward_id,
    ADD CONSTRAINT fk_user_reward_voucher
        FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE SET NULL;

-- 4. Gian hàng đổi điểm chỉ còn voucher giảm giá
INSERT INTO reward_items (id, name, description, points_cost, discount_value, min_order_value, valid_days, category, image_icon, stock, status) VALUES
(1, 'Voucher giảm 15.000đ', 'Cho đơn từ 60.000đ • HSD 30 ngày kể từ ngày đổi', 100, 15000, 60000, 30, 'Phổ biến', 'local_activity', 50, 'ACTIVE'),
(2, 'Voucher giảm 30.000đ', 'Cho đơn từ 120.000đ • HSD 30 ngày kể từ ngày đổi', 180, 30000, 120000, 30, 'Best Match', 'confirmation_number', 30, 'ACTIVE'),
(3, 'Voucher giảm 50.000đ', 'Cho đơn từ 200.000đ • HSD 45 ngày kể từ ngày đổi', 300, 50000, 200000, 45, 'Hạng Vàng', 'card_giftcard', 20, 'ACTIVE'),
(4, 'Voucher giảm 100.000đ', 'Cho đơn từ 350.000đ • HSD 60 ngày kể từ ngày đổi', 600, 100000, 350000, 60, 'Hạng Kim Cương', 'workspace_premium', 10, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    points_cost = VALUES(points_cost),
    discount_value = VALUES(discount_value),
    min_order_value = VALUES(min_order_value),
    valid_days = VALUES(valid_days),
    category = VALUES(category),
    image_icon = VALUES(image_icon),
    stock = VALUES(stock),
    status = VALUES(status);
