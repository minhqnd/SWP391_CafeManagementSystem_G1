-- =========================================================
-- Dữ liệu mẫu để test: Giỏ hàng -> Dùng điểm Aura -> Thanh toán VietQR -> Đơn hàng của tôi
-- Chạy SAU: update_menu_data.sql, update_shop_settings.sql
-- Chạy lại nhiều lần được (xoá dữ liệu test cũ trước khi tạo mới).
--
-- Tài khoản test:  test@auracafe.vn  /  123456   (hoặc SĐT 0911222333)
--
-- Đơn mẫu (đều giao tận nơi, giá theo thực đơn hiện tại, cấu hình mặc định: freeship từ 150.000đ,
-- phí ship 18.000đ, mỗi 50 điểm giảm 10.000đ):
--   AURATEST00  COMPLETED  Đặt tiệc văn phòng 2.420.000đ                          -> +242 điểm
--   AURATEST01  COMPLETED  161.000đ (freeship)                                     -> +16 điểm
--   AURATEST02  COMPLETED  141.000đ - 100 điểm (-20.000đ) + ship 18.000đ = 139.000đ -> -100, +12 điểm
--   AURATEST03  CANCELLED  Hủy trước khi thanh toán 110.000đ
--   AURATEST04  PREPARING  107.000đ + ship 18.000đ                                 -> +10 điểm
--   AURATEST05  PENDING    166.000đ - 50 điểm (-10.000đ), freeship = 156.000đ
--                          -> test "Thanh toán ngay" (+15 điểm) hoặc "Hủy đơn" (hoàn lại 50 điểm)
-- => total_points = 242 + 16 - 100 + 12 + 10 - 50 = 130 (bằng đúng tổng loyalty_transactions)
-- =========================================================
SET NAMES utf8mb4;
USE aura_cafe_erp;

-- ---------- Xoá dữ liệu test cũ ----------
SET @old_user = (SELECT id FROM users WHERE email = 'test@auracafe.vn' LIMIT 1);
DELETE FROM orders WHERE customer_id = @old_user;       -- kéo theo order_items, payments
DELETE FROM users WHERE id = @old_user;                 -- kéo theo loyalty_transactions

-- ---------- Khách hàng test ----------
INSERT INTO users (full_name, phone, email, password_hash, address, role, status, tier_name, total_points)
VALUES ('Nguyễn Văn Test', '0911222333', 'test@auracafe.vn', '123456',
        '15 Ngõ 2 Phố Huế, Hai Bà Trưng, Hà Nội', 'CUSTOMER', 'ACTIVE', 'SILVER', 0);
SET @uid = LAST_INSERT_ID();

-- ---------- AURATEST00: Đặt tiệc văn phòng (đã giao) ----------
INSERT INTO orders (order_code, customer_id, order_type, status, total_amount, discount_amount, delivery_fee, final_amount,
                    recipient_name, recipient_phone, delivery_address, order_note, created_at)
VALUES ('AURATEST00', @uid, 'ONLINE', 'COMPLETED', 2420000, 0, 0, 2420000,
        'Nguyễn Văn Test', '0911222333', 'Tầng 5, 18 Lý Thường Kiệt, Hoàn Kiếm, Hà Nội', 'Tiệc sinh nhật văn phòng',
        NOW() - INTERVAL 30 DAY);
SET @o0 = LAST_INSERT_ID();
INSERT INTO order_items (order_id, product_id, product_name_at_time, unit_price, customization, quantity, subtotal) VALUES
(@o0, 4, 'Cà Phê Sữa Đá Sài Gòn Nâu', 39000, '{"size":"M","toppings":[]}', 30, 1170000),
(@o0, 6, 'Bánh Croissant Bơ Pháp', 35000, '{"size":"Tiêu chuẩn","toppings":[]}', 20, 700000),
(@o0, 12, 'Bánh Cheesecake Việt Quất', 55000, '{"size":"1 Phần","toppings":[]}', 10, 550000);
INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at, payment_note)
VALUES (@o0, 'BANK_TRANSFER', 'VIETQR-AURATEST00', 2420000, 'SUCCESS', NOW() - INTERVAL 30 DAY + INTERVAL 3 MINUTE,
        'Khách chuyển khoản VietQR, nội dung AURATEST00');
INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type, created_at)
VALUES (@uid, @o0, 242, 'EARN', NOW() - INTERVAL 30 DAY + INTERVAL 3 MINUTE);

-- ---------- AURATEST01: Đủ freeship (đã giao) ----------
INSERT INTO orders (order_code, customer_id, order_type, status, total_amount, discount_amount, delivery_fee, final_amount,
                    recipient_name, recipient_phone, delivery_address, order_note, created_at)
VALUES ('AURATEST01', @uid, 'ONLINE', 'COMPLETED', 161000, 0, 0, 161000,
        'Nguyễn Văn Test', '0911222333', '15 Ngõ 2 Phố Huế, Hai Bà Trưng, Hà Nội', NULL,
        NOW() - INTERVAL 10 DAY);
SET @o1 = LAST_INSERT_ID();
INSERT INTO order_items (order_id, product_id, product_name_at_time, unit_price, customization, quantity, subtotal) VALUES
(@o1, 1, 'Cà Phê Muối Aura Đặc Biệt', 63000, '{"size":"L","toppings":["Extra kem muối"]}', 2, 126000),
(@o1, 6, 'Bánh Croissant Bơ Pháp', 35000, '{"size":"Tiêu chuẩn","toppings":[]}', 1, 35000);
INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at, payment_note)
VALUES (@o1, 'BANK_TRANSFER', 'VIETQR-AURATEST01', 161000, 'SUCCESS', NOW() - INTERVAL 10 DAY + INTERVAL 2 MINUTE,
        'Khách chuyển khoản VietQR, nội dung AURATEST01');
INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type, created_at)
VALUES (@uid, @o1, 16, 'EARN', NOW() - INTERVAL 10 DAY + INTERVAL 2 MINUTE);

-- ---------- AURATEST02: Dùng 100 điểm (-20.000đ), chưa đủ freeship (đã giao) ----------
INSERT INTO orders (order_code, customer_id, order_type, status, total_amount, discount_amount, delivery_fee, final_amount,
                    recipient_name, recipient_phone, delivery_address, order_note, created_at)
VALUES ('AURATEST02', @uid, 'ONLINE', 'COMPLETED', 141000, 20000, 18000, 139000,
        'Nguyễn Văn Test', '0911222333', '15 Ngõ 2 Phố Huế, Hai Bà Trưng, Hà Nội', 'Ít đá',
        NOW() - INTERVAL 5 DAY);
SET @o2 = LAST_INSERT_ID();
INSERT INTO order_items (order_id, product_id, product_name_at_time, unit_price, customization, quantity, subtotal) VALUES
(@o2, 5, 'Trà Đào Cam Sả Tươi', 53000, '{"size":"M","toppings":["Thạch đào giòn"]}', 2, 106000),
(@o2, 9, 'Americano Đá Rang Đậm', 35000, '{"size":"M","toppings":[]}', 1, 35000);
INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type, created_at)
VALUES (@uid, @o2, -100, 'REDEEM', NOW() - INTERVAL 5 DAY);
INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at, payment_note)
VALUES (@o2, 'BANK_TRANSFER', 'VIETQR-AURATEST02', 139000, 'SUCCESS', NOW() - INTERVAL 5 DAY + INTERVAL 1 MINUTE,
        'Khách chuyển khoản VietQR, nội dung AURATEST02');
INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type, created_at)
VALUES (@uid, @o2, 12, 'EARN', NOW() - INTERVAL 5 DAY + INTERVAL 1 MINUTE);

-- ---------- AURATEST03: Hủy trước khi thanh toán ----------
INSERT INTO orders (order_code, customer_id, order_type, status, total_amount, discount_amount, delivery_fee, final_amount,
                    recipient_name, recipient_phone, delivery_address, order_note, created_at)
VALUES ('AURATEST03', @uid, 'ONLINE', 'CANCELLED', 110000, 0, 18000, 128000,
        'Nguyễn Văn Test', '0911222333', '15 Ngõ 2 Phố Huế, Hai Bà Trưng, Hà Nội', NULL,
        NOW() - INTERVAL 3 DAY);
SET @o3 = LAST_INSERT_ID();
INSERT INTO order_items (order_id, product_id, product_name_at_time, unit_price, customization, quantity, subtotal) VALUES
(@o3, 2, 'Caramel Macchiato Đặc Sản', 55000, '{"size":"M","toppings":[]}', 1, 55000),
(@o3, 12, 'Bánh Cheesecake Việt Quất', 55000, '{"size":"1 Phần","toppings":[]}', 1, 55000);

-- ---------- AURATEST04: Đã thanh toán, đang pha chế ----------
INSERT INTO orders (order_code, customer_id, order_type, status, total_amount, discount_amount, delivery_fee, final_amount,
                    recipient_name, recipient_phone, delivery_address, order_note, created_at)
VALUES ('AURATEST04', @uid, 'ONLINE', 'PREPARING', 107000, 0, 18000, 125000,
        'Nguyễn Văn Test', '0911222333', '15 Ngõ 2 Phố Huế, Hai Bà Trưng, Hà Nội', 'Gọi trước khi giao',
        NOW() - INTERVAL 20 MINUTE);
SET @o4 = LAST_INSERT_ID();
INSERT INTO order_items (order_id, product_id, product_name_at_time, unit_price, customization, quantity, subtotal) VALUES
(@o4, 10, 'Latte Nghệ Thuật Hạt Phỉ', 62000, '{"size":"L","toppings":[]}', 1, 62000),
(@o4, 4, 'Cà Phê Sữa Đá Sài Gòn Nâu', 45000, '{"size":"M","toppings":["Thạch cà phê"]}', 1, 45000);
INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at, payment_note)
VALUES (@o4, 'BANK_TRANSFER', 'VIETQR-AURATEST04', 125000, 'SUCCESS', NOW() - INTERVAL 18 MINUTE,
        'Khách chuyển khoản VietQR, nội dung AURATEST04');
INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type, created_at)
VALUES (@uid, @o4, 10, 'EARN', NOW() - INTERVAL 18 MINUTE);

-- ---------- AURATEST05: Chờ thanh toán, đã dùng 50 điểm (test "Thanh toán ngay" / "Hủy đơn") ----------
INSERT INTO orders (order_code, customer_id, order_type, status, total_amount, discount_amount, delivery_fee, final_amount,
                    recipient_name, recipient_phone, delivery_address, order_note, created_at)
VALUES ('AURATEST05', @uid, 'ONLINE', 'PENDING', 166000, 10000, 0, 156000,
        'Nguyễn Văn Test', '0911222333', '15 Ngõ 2 Phố Huế, Hai Bà Trưng, Hà Nội', NULL,
        NOW() - INTERVAL 2 MINUTE);
SET @o5 = LAST_INSERT_ID();
INSERT INTO order_items (order_id, product_id, product_name_at_time, unit_price, customization, quantity, subtotal) VALUES
(@o5, 8, 'Trà Sen Vàng Long Nhãn', 49000, '{"size":"M","toppings":[]}', 2, 98000),
(@o5, 3, 'Cold Brew Đóng Chai Aura Reserve', 68000, '{"size":"Chai 250ml","toppings":[]}', 1, 68000);
INSERT INTO loyalty_transactions (customer_id, order_id, points_change, transaction_type, created_at)
VALUES (@uid, @o5, -50, 'REDEEM', NOW() - INTERVAL 2 MINUTE);

-- ---------- Điểm hiện có = tổng lịch sử tích / tiêu điểm ----------
UPDATE users
SET total_points = (SELECT COALESCE(SUM(points_change), 0) FROM loyalty_transactions WHERE customer_id = @uid)
WHERE id = @uid;

SELECT id, email, total_points FROM users WHERE id = @uid;
