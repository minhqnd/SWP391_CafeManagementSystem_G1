SET NAMES utf8mb4;
USE aura_cafe_erp;

CREATE TABLE IF NOT EXISTS reward_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    points_cost INT NOT NULL,
    category VARCHAR(50),
    image_icon VARCHAR(50) DEFAULT 'card_giftcard',
    stock INT NOT NULL DEFAULT -1,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_rewards (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    reward_id INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reward_id) REFERENCES reward_items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Nạp dữ liệu quà tặng đổi điểm
INSERT INTO reward_items (id, name, description, points_cost, category, image_icon, stock, status) VALUES
(1, 'Voucher Giảm 15.000đ cho đơn từ 60.000đ', 'HSD: 30 ngày kể từ ngày đổi', 100, 'Phổ biến', 'local_activity', 50, 'ACTIVE'),
(2, 'Free 01 Bánh Croissant Bơ Pháp khi mua đồ uống', 'HSD: 14 ngày kể từ ngày đổi', 180, 'Best Match', 'bakery_dining', 30, 'ACTIVE'),
(3, 'Voucher Giảm 50.000đ hóa đơn cà phê rang xay', 'HSD: 45 ngày', 300, 'Hạng Vàng', 'card_giftcard', 20, 'ACTIVE'),
(4, 'Ly giữ nhiệt Aura Eco Tumbler 500ml Edition', 'Nhận tại quầy Aura bất kỳ', 600, 'Quà tặng hiện vật', 'coffee', 0, 'ACTIVE')
ON DUPLICATE KEY UPDATE 
    name = VALUES(name), 
    description = VALUES(description), 
    points_cost = VALUES(points_cost), 
    category = VALUES(category), 
    image_icon = VALUES(image_icon), 
    stock = VALUES(stock), 
    status = VALUES(status);
