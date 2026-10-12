SET NAMES utf8mb4;
USE aura_cafe_erp;

-- 1. Cập nhật danh mục chuẩn theo thiết kế
INSERT INTO categories (id, name, status, icon) VALUES
(1, 'Cà phê máy', 'ACTIVE', 'coffee_maker'),
(2, 'Cà phê truyền thống', 'ACTIVE', 'local_cafe'),
(3, 'Trà trái cây', 'ACTIVE', 'emoji_food_beverage'),
(4, 'Đá xay & Matcha', 'ACTIVE', 'icecream'),
(5, 'Bánh ngọt', 'ACTIVE', 'bakery_dining'),
(6, 'Đóng chai', 'ACTIVE', 'liquor')
ON DUPLICATE KEY UPDATE 
name = VALUES(name), status = VALUES(status), icon = VALUES(icon);

-- 2. Cập nhật và bổ sung 12 món ăn khớp 100% UI Figma/Vercel
INSERT INTO products (id, category_id, name, base_price, description, badge, subtitle, variants_config, image_url, is_available) VALUES
(1, 2, 'Cà Phê Muối Aura Đặc Biệt', 45000, 
 'Fine Robusta ủ phin truyền thống phối kem sữa mằn mặn béo ngậy, cân bằng hậu vị ngọt đầm.', 
 'BÁN CHẠY', 'Phin Cổ Điển', 
 '{"sizes":{"M":0,"L":10000},"toppings":[{"name":"Extra kem muối","price":8000},{"name":"Extra shot espresso","price":10000}]}',
 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80', 1),

(2, 1, 'Caramel Macchiato Đặc Sản', 55000, 
 'Lớp sữa tươi nóng thanh dịu, quyện cùng espresso kép Arabica Cầu Đất đậm đà và vân sốt caramel thơm lừng.', 
 'BÁN CHẠY', 'Espresso Base', 
 '{"sizes":{"M":0,"L":10000},"toppings":[{"name":"Sốt caramel thêm","price":5000},{"name":"Kem whipping","price":8000}]}',
 'https://images.unsplash.com/photo-1485808191679-5f86510681a2?w=600&auto=format&fit=crop&q=80', 1),

(3, 6, 'Cold Brew Đóng Chai Aura Reserve', 68000, 
 'Chiết xuất lạnh 16 tiếng từ 100% Arabica Bourbon, hương hoa quả nhiệt đới tươi mát, giữ trọn vị nguyên bản.', 
 'BÁN CHẠY', 'Ủ Lạnh 16 Tiếng', 
 '{"sizes":{"Chai 250ml":0,"Chai 500ml":35000},"toppings":[]}',
 'https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?w=600&auto=format&fit=crop&q=80', 1),

(4, 2, 'Cà Phê Sữa Đá Sài Gòn Nâu', 39000, 
 'Hạt Fine Robusta rang nâu bóng mượt, chiết xuất phin chậm kết hợp sữa đặc ngọt êm béo ngậy truyền thống.', 
 NULL, 'Đậm Vị Phin', 
 '{"sizes":{"M":0,"L":8000},"toppings":[{"name":"Thạch cà phê","price":6000},{"name":"Extra shot phin","price":8000}]}',
 'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=600&auto=format&fit=crop&q=80', 1),

(5, 3, 'Trà Đào Cam Sả Tươi', 45000, 
 'Nước cốt trà đen Ceylon thanh dịu kết hợp miếng đào giòn ngọt mọng nước cùng sả tươi ấm nồng.', 
 'MÓN MỚI', 'Fresh Brew', 
 '{"sizes":{"M":0,"L":10000},"toppings":[{"name":"Thạch đào giòn","price":8000},{"name":"Hạt chia","price":5000}]}',
 'https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=600&auto=format&fit=crop&q=80', 1),

(6, 5, 'Bánh Croissant Bơ Pháp', 35000, 
 'Bột ngàn lớp thủ công giòn rụm bên ngoài, mềm xốp ngậy hương bơ Elle & Vire hảo hạng bên trong.', 
 'BÁN CHẠY', 'Bơ Elle & Vire', 
 '{"sizes":{"Tiêu chuẩn":0},"toppings":[{"name":"Mứt dâu tây","price":6000},{"name":"Sốt sôcôla","price":6000}]}',
 'https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=600&auto=format&fit=crop&q=80', 1),

(7, 4, 'Matcha Latte Uji Kyoto Đá Xay', 58000, 
 'Bột trà xanh Uji trứ danh cố đô Kyoto xay cùng sữa hạt và kem whipping bông xốp béo mịn thượng hạng.', 
 'BÁN CHẠY', 'Uji Kyoto Grade', 
 '{"sizes":{"M":0,"L":12000},"toppings":[{"name":"Kem whipping","price":8000},{"name":"Thạch matcha","price":8000}]}',
 'https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=600&auto=format&fit=crop&q=80', 1),

(8, 3, 'Trà Sen Vàng Long Nhãn', 49000, 
 'Hạt sen Huế ninh mềm dẻo, nhãn nhục ngọt thanh quyện cùng trà ô long hảo hạng và lớp kem củ năng giòn.', 
 NULL, 'Trà Đen Ceylon', 
 '{"sizes":{"M":0,"L":10000},"toppings":[{"name":"Hạt sen thêm","price":10000},{"name":"Củ năng giòn","price":8000}]}',
 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600&auto=format&fit=crop&q=80', 1),

(9, 1, 'Americano Đá Rang Đậm', 35000, 
 'Espresso kép chiết xuất áp suất cao từ hạt cà phê đặc sản, vị chua thanh thanh tao và hậu ngọt dài lâu.', 
 NULL, 'Espresso Base', 
 '{"sizes":{"M":0,"L":10000},"toppings":[{"name":"Extra shot espresso","price":10000}]}',
 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=600&auto=format&fit=crop&q=80', 1),

(10, 1, 'Latte Nghệ Thuật Hạt Phỉ', 52000, 
 'Hòa quyện tinh tế giữa espresso thơm ngát, sữa tươi đánh bọt mịn màng và siro hạt phỉ thơm lừng.', 
 'MÓN MỚI', 'Espresso Base', 
 '{"sizes":{"M":0,"L":10000},"toppings":[{"name":"Siro hạt phỉ","price":6000},{"name":"Sữa yến mạch (Oat milk)","price":12000}]}',
 'https://images.unsplash.com/photo-1534778101976-62847782c213?w=600&auto=format&fit=crop&q=80', 1),

(11, 4, 'Sôcôla Bỉ Đá Xay Hạnh Nhân', 55000, 
 'Bột cacao nguyên chất nhập khẩu Bỉ xay nhuyễn cùng sữa đặc, rắc hạnh nhân nướng giòn tan.', 
 NULL, 'Chocolate Rich', 
 '{"sizes":{"M":0,"L":10000},"toppings":[{"name":"Kem whipping","price":8000},{"name":"Hạnh nhân vụn","price":6000}]}',
 'https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=600&auto=format&fit=crop&q=80', 1),

(12, 5, 'Bánh Cheesecake Việt Quất', 55000, 
 'Cream cheese Philadelphia béo ngậy kết hợp mứt việt quất chua ngọt dịu và đế bánh quy nướng thơm lừng.', 
 'BÁN CHẠY', 'New York Style', 
 '{"sizes":{"1 Phần":0},"toppings":[]}',
 'https://images.unsplash.com/photo-1533134242443-d4fd215305ad?w=600&auto=format&fit=crop&q=80', 1)
ON DUPLICATE KEY UPDATE 
category_id = VALUES(category_id),
name = VALUES(name),
base_price = VALUES(base_price),
description = VALUES(description),
badge = VALUES(badge),
subtitle = VALUES(subtitle),
variants_config = VALUES(variants_config),
image_url = VALUES(image_url),
is_available = VALUES(is_available);
