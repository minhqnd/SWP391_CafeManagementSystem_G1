SET NAMES utf8mb4;

UPDATE reward_items SET 
    name = 'Voucher Giảm 15.000đ cho đơn từ 60.000đ', 
    description = 'HSD: 30 ngày kể từ ngày đổi', 
    category = 'Phổ biến' 
WHERE points_cost = 100;

UPDATE reward_items SET 
    name = 'Free 01 Bánh Croissant Bơ Pháp', 
    description = 'HSD: 14 ngày kể từ ngày đổi', 
    category = 'Best Match' 
WHERE points_cost = 180;

UPDATE reward_items SET 
    name = 'Voucher Giảm 50.000đ', 
    description = 'HSD: 45 ngày', 
    category = 'Hạng Vàng' 
WHERE points_cost = 300;

UPDATE reward_items SET 
    name = 'Ly giữ nhiệt Aura Eco Tumbler 500ml Edition', 
    description = 'Nhận tại quầy Aura bất kỳ', 
    category = 'Quà tặng hiện vật' 
WHERE points_cost = 600;
