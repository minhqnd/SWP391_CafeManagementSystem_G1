-- =========================================================
-- Cấu hình bán hàng online do Admin chỉnh ở /admin/settings
-- Chạy được nhiều lần (không ghi đè giá trị Admin đã lưu).
--
-- Quy tắc:
--   * Không còn voucher: điểm Aura trừ thẳng vào tiền hàng ở Giỏ hàng, theo các mốc chẵn
--     (mặc định mỗi 10 điểm giảm 2.000đ: kéo thanh chọn 10, 20, 30... điểm), tiền giảm không vượt quá tiền hàng.
--   * Điểm bị trừ khi khách xác nhận đơn (loyalty_transactions REDEEM gắn order_id),
--     hoàn lại nếu khách hủy đơn chưa thanh toán.
--   * Chỉ giao tận nơi (không còn tự đến lấy tại quầy / hẹn giờ nhận):
--     miễn phí giao hàng khi tiền hàng đạt freeship_threshold, ngược lại tính delivery_fee.
--   * Tích điểm giữ nguyên: thanh toán thành công được (tiền hàng - tiền giảm) / 10.000đ điểm.
-- =========================================================
SET NAMES utf8mb4;
USE aura_cafe_erp;

CREATE TABLE IF NOT EXISTS shop_settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO shop_settings (setting_key, setting_value) VALUES
('freeship_threshold', '150000'),  -- tiền hàng tối thiểu để được Freeship (0 = mọi đơn)
('delivery_fee', '18000'),         -- phí giao hàng khi chưa đủ Freeship
('delivery_minutes', '30'),        -- thời gian giao dự kiến (phút) kể từ khi thanh toán
('points_step', '10'),             -- mốc điểm Aura (bước của thanh kéo ở Giỏ hàng)
('points_step_value', '2000'),     -- số tiền giảm cho mỗi mốc điểm
('shift_morning', '07:00-12:00'),  -- khung giờ ca sáng
('shift_afternoon', '12:00-17:00'),-- khung giờ ca chiều
('shift_evening', '17:00-22:00');  -- khung giờ ca tối

SELECT setting_key, setting_value FROM shop_settings;
