-- =========================================================
-- AURA CAFE MANAGEMENT SYSTEM (CMS) - DATABASE V2
-- MySQL 8.0+
-- Baseline: latest Use Case + Screen List
-- Demo accounts use password: 123456
-- =========================================================

DROP DATABASE IF EXISTS aura_cafe_management_system;
CREATE DATABASE aura_cafe_management_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE aura_cafe_management_system;

-- =========================================================
-- 1. ACCOUNT
-- =========================================================
CREATE TABLE account (
    account_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Active',
    reset_token VARCHAR(255) NULL,
    reset_token_expired_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

-- =========================================================
-- 2. CUSTOMER
-- =========================================================
CREATE TABLE customer (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    loyalty_points INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_customer_loyalty
        CHECK (loyalty_points >= 0),
    CONSTRAINT fk_customer_account
        FOREIGN KEY (account_id)
        REFERENCES account(account_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================================================
-- 3. STAFF
-- =========================================================
CREATE TABLE staff (
    staff_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Active',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_account
        FOREIGN KEY (account_id)
        REFERENCES account(account_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================================================
-- 4. CATEGORY
-- =========================================================
CREATE TABLE category (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'Active'
);

-- =========================================================
-- 5. PRODUCT
-- =========================================================
CREATE TABLE product (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Available',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_product_price
        CHECK (price >= 0),
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id)
        REFERENCES category(category_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================================================
-- 6. PRODUCT IMAGE
-- =========================================================
CREATE TABLE product_image (
    image_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_image_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

-- =========================================================
-- 7. PROMOTION
-- =========================================================
CREATE TABLE promotion (
    promotion_id INT AUTO_INCREMENT PRIMARY KEY,
    promotion_name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    discount_type VARCHAR(30) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Active',
    CONSTRAINT chk_promotion_value
        CHECK (discount_value >= 0),
    CONSTRAINT chk_promotion_date
        CHECK (end_date >= start_date)
);

-- =========================================================
-- 8. VOUCHER
-- =========================================================
CREATE TABLE voucher (
    voucher_id INT AUTO_INCREMENT PRIMARY KEY,
    promotion_id INT NOT NULL,
    voucher_code VARCHAR(50) NOT NULL UNIQUE,
    usage_limit INT NULL,
    used_count INT NOT NULL DEFAULT 0,
    minimum_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'Active',
    CONSTRAINT chk_voucher_usage
        CHECK (usage_limit IS NULL OR usage_limit >= 0),
    CONSTRAINT chk_voucher_used_count
        CHECK (used_count >= 0),
    CONSTRAINT chk_voucher_minimum_order
        CHECK (minimum_order_amount >= 0),
    CONSTRAINT fk_voucher_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotion(promotion_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================================================
-- 9. CUSTOMER ADDRESS
-- =========================================================
CREATE TABLE customer_address (
    address_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    ward VARCHAR(100),
    district VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'Active',
    CONSTRAINT fk_customer_address_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

-- =========================================================
-- 10. LOYALTY PROGRAM
-- =========================================================
CREATE TABLE loyalty_program (
    loyalty_program_id INT AUTO_INCREMENT PRIMARY KEY,
    program_name VARCHAR(150) NOT NULL,
    points_per_amount DECIMAL(10,2) NOT NULL DEFAULT 1,
    redeem_amount_per_point DECIMAL(10,2) NOT NULL DEFAULT 1000,
    status VARCHAR(30) NOT NULL DEFAULT 'Active',
    start_date DATE,
    end_date DATE,
    CONSTRAINT chk_loyalty_points_rate
        CHECK (points_per_amount > 0),
    CONSTRAINT chk_loyalty_redeem_rate
        CHECK (redeem_amount_per_point > 0)
);

-- =========================================================
-- 11. CUSTOMER ORDER
-- =========================================================
CREATE TABLE customer_order (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NULL,
    cashier_id INT NULL,
    voucher_id INT NULL,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    order_type VARCHAR(40) NOT NULL,
    subtotal_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    delivery_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'Pending',
    note VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_order_amounts
        CHECK (
            subtotal_amount >= 0
            AND discount_amount >= 0
            AND delivery_fee >= 0
            AND total_amount >= 0
        ),
    CONSTRAINT fk_order_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,
    CONSTRAINT fk_order_cashier
        FOREIGN KEY (cashier_id)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,
    CONSTRAINT fk_order_voucher
        FOREIGN KEY (voucher_id)
        REFERENCES voucher(voucher_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

-- =========================================================
-- 12. ORDER ITEM
-- =========================================================
CREATE TABLE order_item (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    preparation_status VARCHAR(30) NOT NULL DEFAULT 'Pending',
    note VARCHAR(500),
    CONSTRAINT chk_order_item_price
        CHECK (unit_price >= 0),
    CONSTRAINT chk_order_item_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_order_item_subtotal
        CHECK (subtotal >= 0),
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id)
        REFERENCES customer_order(order_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================================================
-- 13. PAYMENT
-- =========================================================
CREATE TABLE payment (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL UNIQUE,
    payment_method VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Pending',
    transaction_id VARCHAR(150),
    paid_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_amount
        CHECK (amount >= 0),
    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id)
        REFERENCES customer_order(order_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================================================
-- 14. DELIVERY
-- =========================================================
CREATE TABLE delivery (
    delivery_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL UNIQUE,
    delivery_staff_id INT NULL,
    address_id INT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    delivery_address_snapshot VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Unassigned',
    failure_reason VARCHAR(500),
    delivered_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_order
        FOREIGN KEY (order_id)
        REFERENCES customer_order(order_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_staff
        FOREIGN KEY (delivery_staff_id)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,
    CONSTRAINT fk_delivery_address
        FOREIGN KEY (address_id)
        REFERENCES customer_address(address_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

-- =========================================================
-- 15. COMPLAINT
-- =========================================================
CREATE TABLE complaint (
    complaint_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    order_id INT NOT NULL,
    complaint_type VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Pending',
    resolution TEXT NULL,
    responded_by INT NULL,
    responded_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_complaint_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_complaint_order
        FOREIGN KEY (order_id)
        REFERENCES customer_order(order_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_complaint_staff
        FOREIGN KEY (responded_by)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

-- =========================================================
-- 16. CANCELLATION REQUEST
-- =========================================================
CREATE TABLE cancellation_request (
    cancellation_request_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Pending',
    processed_by INT NULL,
    processed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cancellation_order
        FOREIGN KEY (order_id)
        REFERENCES customer_order(order_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_cancellation_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_cancellation_staff
        FOREIGN KEY (processed_by)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

-- =========================================================
-- 17. REFUND
-- =========================================================
CREATE TABLE refund (
    refund_id INT AUTO_INCREMENT PRIMARY KEY,
    payment_id INT NOT NULL,
    cancellation_request_id INT NULL,
    processed_by INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Pending',
    transaction_id VARCHAR(150),
    processed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_refund_amount
        CHECK (amount >= 0),
    CONSTRAINT fk_refund_payment
        FOREIGN KEY (payment_id)
        REFERENCES payment(payment_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_refund_cancellation
        FOREIGN KEY (cancellation_request_id)
        REFERENCES cancellation_request(cancellation_request_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,
    CONSTRAINT fk_refund_staff
        FOREIGN KEY (processed_by)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================================================
-- 18. LOYALTY TRANSACTION
-- =========================================================
CREATE TABLE loyalty_transaction (
    loyalty_transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    order_id INT NULL,
    processed_by INT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    points INT NOT NULL,
    note VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loyalty_transaction_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_loyalty_transaction_order
        FOREIGN KEY (order_id)
        REFERENCES customer_order(order_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,
    CONSTRAINT fk_loyalty_transaction_staff
        FOREIGN KEY (processed_by)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

-- =========================================================
-- 19. WORK SCHEDULE
-- =========================================================
CREATE TABLE work_schedule (
    schedule_id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL,
    work_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    CONSTRAINT fk_schedule_staff
        FOREIGN KEY (staff_id)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================================================
-- 20. ATTENDANCE
-- =========================================================
CREATE TABLE attendance (
    attendance_id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL,
    check_in DATETIME NULL,
    check_out DATETIME NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PRESENT',
    CONSTRAINT fk_attendance_staff
        FOREIGN KEY (staff_id)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================================================
-- 21. NOTIFICATION
-- =========================================================
CREATE TABLE notification (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    created_by INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_created_by
        FOREIGN KEY (created_by)
        REFERENCES account(account_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

-- =========================================================
-- 22. NOTIFICATION RECIPIENT
-- =========================================================
CREATE TABLE notification_recipient (
    notification_id INT NOT NULL,
    account_id INT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME NULL,
    PRIMARY KEY (notification_id, account_id),
    CONSTRAINT fk_notification_recipient_notification
        FOREIGN KEY (notification_id)
        REFERENCES notification(notification_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_notification_recipient_account
        FOREIGN KEY (account_id)
        REFERENCES account(account_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

-- =========================================================
-- 23. SYSTEM SETTING
-- =========================================================
CREATE TABLE system_setting (
    setting_id INT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(1000) NOT NULL,
    description VARCHAR(500),
    updated_by INT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_setting_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES account(account_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

-- =========================================================
-- SAMPLE DATA
-- Demo password for all accounts: 123456
-- =========================================================

-- Accounts
INSERT INTO account
(account_id, username, email, password_hash, status)
VALUES
(1, 'admin',     'admin@auracafe.vn',      '123456', 'Active'),
(2, 'cashier1',  'cashier1@auracafe.vn',   '123456', 'Active'),
(3, 'cashier2',  'cashier2@auracafe.vn',   '123456', 'Active'),
(4, 'barista1',  'barista1@auracafe.vn',   '123456', 'Active'),
(5, 'barista2',  'barista2@auracafe.vn',   '123456', 'Active'),
(6, 'delivery1', 'delivery1@auracafe.vn',  '123456', 'Active'),
(7, 'delivery2', 'delivery2@auracafe.vn',  '123456', 'Active'),
(8, 'customer1', 'customer1@gmail.com',    '123456', 'Active'),
(9, 'customer2', 'customer2@gmail.com',    '123456', 'Active'),
(10,'customer3', 'customer3@gmail.com',    '123456', 'Active'),
(11,'customer4', 'customer4@gmail.com',    '123456', 'Active');

-- Staff
INSERT INTO staff
(staff_id, account_id, full_name, role, status)
VALUES
(1, 1, 'Nguyễn Minh Anh', 'Admin', 'Active'),
(2, 2, 'Trần Quốc Huy', 'Cashier', 'Active'),
(3, 3, 'Lê Thu Hà', 'Cashier', 'Active'),
(4, 4, 'Phạm Đức Long', 'Barista', 'Active'),
(5, 5, 'Đỗ Khánh Linh', 'Barista', 'Active'),
(6, 6, 'Nguyễn Văn Nam', 'Delivery Staff', 'Active'),
(7, 7, 'Vũ Hoàng Sơn', 'Delivery Staff', 'Active');

-- Customers
INSERT INTO customer
(customer_id, account_id, full_name, phone, loyalty_points)
VALUES
(1, 8,  'Nguyễn Quốc Nguyên', '0901234567', 1250),
(2, 9,  'Trần Minh Anh',      '0912345678', 680),
(3, 10, 'Lê Hoàng Nam',       '0987654321', 320),
(4, 11, 'Phạm Thu Trang',     '0934567890', 2100);

-- Categories
INSERT INTO category
(category_id, category_name, status)
VALUES
(1, 'Cà phê', 'Active'),
(2, 'Cold Brew', 'Active'),
(3, 'Trà', 'Active'),
(4, 'Bánh ngọt', 'Active');

-- Products
INSERT INTO product
(product_id, category_id, product_name, description, price, status)
VALUES
(1, 1, 'Cà phê sữa đá', 'Cà phê rang xay, sữa đặc, đá viên', 39000, 'Available'),
(2, 1, 'Bạc xỉu', 'Cà phê sữa với tỷ lệ sữa cao hơn', 42000, 'Available'),
(3, 1, 'Americano', 'Espresso kết hợp nước nóng hoặc lạnh', 45000, 'Available'),
(4, 2, 'Cold Brew Original', 'Cold Brew ủ lạnh hương vị cân bằng', 49000, 'Available'),
(5, 2, 'Cold Brew Cam', 'Cold Brew kết hợp cam tươi', 55000, 'Available'),
(6, 3, 'Trà đào cam sả', 'Trà đào, cam tươi và sả', 52000, 'Available'),
(7, 3, 'Trà ô long kem cheese', 'Trà ô long kết hợp lớp kem cheese', 59000, 'Available'),
(8, 4, 'Croissant bơ', 'Bánh croissant bơ nướng giòn', 35000, 'Available');

-- Product Images
INSERT INTO product_image
(product_id, image_url, is_primary)
VALUES
(1, 'https://images.example.com/aura-ca-phe-sua-da.jpg', TRUE),
(2, 'https://images.example.com/aura-bac-xiu.jpg', TRUE),
(3, 'https://images.example.com/aura-americano.jpg', TRUE),
(4, 'https://images.example.com/aura-cold-brew-original.jpg', TRUE),
(5, 'https://images.example.com/aura-cold-brew-cam.jpg', TRUE),
(6, 'https://images.example.com/aura-tra-dao-cam-sa.jpg', TRUE),
(7, 'https://images.example.com/aura-tra-oolong-cheese.jpg', TRUE),
(8, 'https://images.example.com/aura-croissant.jpg', TRUE);

-- Promotions
INSERT INTO promotion
(promotion_id, promotion_name, description, discount_type, discount_value,
 start_date, end_date, status)
VALUES
(1, 'Aura Welcome', 'Giảm 10% cho khách hàng mới', 'PERCENT', 10,
 '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'Active'),
(2, 'Free Ship Aura', 'Giảm phí giao hàng cho đơn đủ điều kiện', 'FIXED', 15000,
 '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'Active');

-- Vouchers
INSERT INTO voucher
(voucher_id, promotion_id, voucher_code, usage_limit, used_count,
 minimum_order_amount, status)
VALUES
(1, 1, 'AURAWELCOME10', 1000, 25, 100000, 'Active'),
(2, 2, 'FREESHIP15K', 500, 40, 120000, 'Active');

-- Customer Addresses
INSERT INTO customer_address
(address_id, customer_id, recipient_name, phone, address_line,
 ward, district, city, is_default, status)
VALUES
(1, 1, 'Nguyễn Quốc Nguyên', '0901234567',
 '12 Trần Đại Nghĩa', 'Bách Khoa', 'Hai Bà Trưng', 'Hà Nội', TRUE, 'Active'),
(2, 1, 'Nguyễn Quốc Nguyên', '0901234567',
 '25 Chùa Bộc', 'Quang Trung', 'Đống Đa', 'Hà Nội', FALSE, 'Active'),
(3, 2, 'Trần Minh Anh', '0912345678',
 '88 Nguyễn Chí Thanh', 'Láng Thượng', 'Đống Đa', 'Hà Nội', TRUE, 'Active'),
(4, 3, 'Lê Hoàng Nam', '0987654321',
 '15 Lê Văn Lương', 'Nhân Chính', 'Thanh Xuân', 'Hà Nội', TRUE, 'Active'),
(5, 4, 'Phạm Thu Trang', '0934567890',
 '20 Trần Hưng Đạo', 'Phan Chu Trinh', 'Hoàn Kiếm', 'Hà Nội', TRUE, 'Active');

-- Loyalty Program
INSERT INTO loyalty_program
(loyalty_program_id, program_name, points_per_amount,
 redeem_amount_per_point, status, start_date, end_date)
VALUES
(1, 'Aura Rewards', 1, 1000, 'Active', '2026-01-01', '2026-12-31');

-- Orders
INSERT INTO customer_order
(order_id, customer_id, cashier_id, voucher_id, invoice_number, order_type,
 subtotal_amount, discount_amount, delivery_fee, total_amount, status, note, created_at)
VALUES
(1, 1, NULL, 1, 'INV-20261001-0001', 'ONLINE_DELIVERY',
 177000, 17700, 0, 159300, 'Completed',
 'Giao trong giờ hành chính', '2026-10-01 09:15:00'),

(2, 2, 2, NULL, 'INV-20261001-0002', 'COUNTER_TAKEAWAY',
 94000, 0, 0, 94000, 'Ready',
 NULL, '2026-10-01 10:30:00'),

(3, 3, NULL, NULL, 'INV-20261001-0003', 'ONLINE_DELIVERY',
 104000, 0, 15000, 119000, 'Delivering',
 NULL, '2026-10-01 11:20:00'),

(4, 4, 3, NULL, 'INV-20261001-0004', 'COUNTER_DINE_IN',
 149000, 0, 0, 149000, 'Completed',
 NULL, '2026-10-01 13:05:00'),

(5, 1, NULL, 2, 'INV-20261001-0005', 'ONLINE_PICKUP',
 104000, 15000, 0, 89000, 'Cancelled',
 'Khách yêu cầu hủy', '2026-10-01 15:40:00'),

(6, 2, NULL, NULL, 'INV-20261002-0001', 'ONLINE_DELIVERY',
 140000, 0, 15000, 155000, 'Preparing',
 NULL, '2026-10-02 08:45:00');

-- Order Items
INSERT INTO order_item
(order_item_id, order_id, product_id, unit_price, quantity, subtotal,
 preparation_status, note)
VALUES
(1, 1, 1, 39000, 2, 78000, 'Completed', 'Ít đá'),
(2, 1, 6, 52000, 1, 52000, 'Completed', NULL),
(3, 1, 8, 35000, 1, 35000, 'Completed', NULL),

(4, 2, 2, 42000, 1, 42000, 'Completed', NULL),
(5, 2, 8, 35000, 1, 35000, 'Completed', NULL),
(6, 2, 1, 39000, 1, 39000, 'Completed', NULL),

(7, 3, 4, 49000, 1, 49000, 'Completed', NULL),
(8, 3, 6, 52000, 1, 52000, 'Completed', NULL),

(9, 4, 7, 59000, 1, 59000, 'Completed', NULL),
(10,4, 5, 55000, 1, 55000, 'Completed', NULL),
(11,4, 8, 35000, 1, 35000, 'Completed', NULL),

(12,5, 3, 45000, 1, 45000, 'Pending', NULL),
(13,5, 7, 59000, 1, 59000, 'Pending', NULL),

(14,6, 1, 39000, 1, 39000, 'Preparing', NULL),
(15,6, 4, 49000, 1, 49000, 'Preparing', NULL),
(16,6, 3, 45000, 1, 45000, 'Pending', NULL);

-- Payments
INSERT INTO payment
(payment_id, order_id, payment_method, amount, status, transaction_id, paid_at)
VALUES
(1, 1, 'MoMo', 159300, 'Paid', 'MOMO-20261001-0001', '2026-10-01 09:16:30'),
(2, 2, 'Cash', 94000, 'Paid', 'CASH-20261001-0002', '2026-10-01 10:31:00'),
(3, 3, 'VietQR', 119000, 'Paid', 'VQR-20261001-0003', '2026-10-01 11:21:10'),
(4, 4, 'Cash', 149000, 'Paid', 'CASH-20261001-0004', '2026-10-01 13:06:20'),
(5, 5, 'ZaloPay', 89000, 'Refunded', 'ZLP-20261001-0005', '2026-10-01 15:41:30'),
(6, 6, 'VietQR', 155000, 'Paid', 'VQR-20261002-0001', '2026-10-02 08:46:00');

-- Deliveries
INSERT INTO delivery
(delivery_id, order_id, delivery_staff_id, address_id,
 recipient_name, recipient_phone, delivery_address_snapshot,
 status, failure_reason, delivered_at)
VALUES
(1, 1, 6, 1, 'Nguyễn Quốc Nguyên', '0901234567',
 '12 Trần Đại Nghĩa, Bách Khoa, Hai Bà Trưng, Hà Nội',
 'Delivered', NULL, '2026-10-01 10:05:00'),

(2, 3, 7, 4, 'Lê Hoàng Nam', '0987654321',
 '15 Lê Văn Lương, Nhân Chính, Thanh Xuân, Hà Nội',
 'Delivering', NULL, NULL),

(3, 6, 6, 3, 'Trần Minh Anh', '0912345678',
 '88 Nguyễn Chí Thanh, Láng Thượng, Đống Đa, Hà Nội',
 'Assigned', NULL, NULL);

-- Complaints
INSERT INTO complaint
(complaint_id, customer_id, order_id, complaint_type, content,
 status, resolution, responded_by, responded_at)
VALUES
(1, 1, 1, 'Order Quality',
 'Đồ uống đến nơi hơi nguội và đá tan nhiều.',
 'Resolved',
 'Aura đã ghi nhận phản hồi và gửi ưu đãi cho khách hàng.',
 1, '2026-10-01 12:10:00'),

(2, 3, 3, 'Delivery',
 'Đơn giao đang chậm hơn thời gian dự kiến.',
 'Pending',
 NULL, NULL, NULL);

-- Cancellation Requests
INSERT INTO cancellation_request
(cancellation_request_id, order_id, customer_id, reason,
 status, processed_by, processed_at)
VALUES
(1, 5, 1, 'Khách đặt nhầm sản phẩm và muốn hủy đơn.',
 'Approved', 1, '2026-10-01 15:50:00');

-- Refunds
INSERT INTO refund
(refund_id, payment_id, cancellation_request_id, processed_by,
 amount, status, transaction_id, processed_at)
VALUES
(1, 5, 1, 1, 89000, 'Completed', 'REF-20261001-0005',
 '2026-10-01 15:55:00');

-- Loyalty Transactions
INSERT INTO loyalty_transaction
(loyalty_transaction_id, customer_id, order_id, processed_by,
 transaction_type, points, note)
VALUES
(1, 1, 1, NULL, 'EARN', 159,
 'Tích điểm từ đơn hàng INV-20261001-0001'),
(2, 1, NULL, 2, 'ADJUST', 100,
 'Thưởng điểm tại quầy'),
(3, 2, 2, 2, 'EARN', 94,
 'Tích điểm từ đơn hàng INV-20261001-0002'),
(4, 3, 3, NULL, 'EARN', 119,
 'Tích điểm từ đơn hàng INV-20261001-0003'),
(5, 4, 4, 3, 'EARN', 149,
 'Tích điểm từ đơn hàng INV-20261001-0004'),
(6, 1, NULL, 2, 'REDEEM', -150,
 'Đổi điểm khi mua hàng');

-- Work Schedules
INSERT INTO work_schedule
(schedule_id, staff_id, work_date, start_time, end_time, status)
VALUES
(1, 2, '2026-10-02', '07:00:00', '15:00:00', 'ASSIGNED'),
(2, 3, '2026-10-02', '14:00:00', '22:00:00', 'ASSIGNED'),
(3, 4, '2026-10-02', '07:00:00', '15:00:00', 'ASSIGNED'),
(4, 5, '2026-10-02', '14:00:00', '22:00:00', 'ASSIGNED'),
(5, 6, '2026-10-02', '08:00:00', '16:00:00', 'ASSIGNED'),
(6, 7, '2026-10-02', '14:00:00', '22:00:00', 'ASSIGNED');

-- Attendance
INSERT INTO attendance
(attendance_id, staff_id, check_in, check_out, status)
VALUES
(1, 2, '2026-10-02 06:55:00', '2026-10-02 15:02:00', 'PRESENT'),
(2, 3, '2026-10-02 13:58:00', NULL, 'PRESENT'),
(3, 4, '2026-10-02 06:58:00', '2026-10-02 15:01:00', 'PRESENT'),
(4, 5, '2026-10-02 14:05:00', NULL, 'LATE'),
(5, 6, '2026-10-02 07:55:00', '2026-10-02 16:03:00', 'PRESENT'),
(6, 7, '2026-10-02 14:00:00', NULL, 'PRESENT');

-- Notifications
INSERT INTO notification
(notification_id, title, message, notification_type, created_by)
VALUES
(1, 'Đơn hàng mới',
 'Đơn INV-20261002-0001 đang chờ Barista tiếp nhận.',
 'ORDER', 1),
(2, 'Yêu cầu hủy đơn',
 'Đơn INV-20261001-0005 đã được Admin phê duyệt hủy.',
 'CANCELLATION', 1),
(3, 'Lịch làm việc',
 'Lịch làm việc ngày 02/10/2026 đã được cập nhật.',
 'SCHEDULE', 1),
(4, 'Phản hồi khiếu nại',
 'Khiếu nại của bạn đã được xử lý. Vui lòng kiểm tra phản hồi.',
 'COMPLAINT', 1);

INSERT INTO notification_recipient
(notification_id, account_id, is_read, read_at)
VALUES
(1, 4, FALSE, NULL),
(1, 5, FALSE, NULL),
(2, 8, TRUE, '2026-10-01 15:58:00'),
(3, 2, TRUE, '2026-10-01 20:10:00'),
(3, 3, TRUE, '2026-10-01 20:12:00'),
(3, 4, FALSE, NULL),
(3, 5, FALSE, NULL),
(3, 6, FALSE, NULL),
(3, 7, FALSE, NULL),
(4, 8, FALSE, NULL);

-- System settings
INSERT INTO system_setting
(setting_id, setting_key, setting_value, description, updated_by)
VALUES
(1, 'system_name', 'Aura Café Management System', 'Tên hệ thống', 1),
(2, 'store_name', 'Aura Café', 'Tên thương hiệu', 1),
(3, 'store_phone', '1900 6868', 'Số điện thoại cửa hàng', 1),
(4, 'store_address', '25 Trần Đại Nghĩa, Hai Bà Trưng, Hà Nội', 'Địa chỉ cửa hàng', 1),
(5, 'delivery_radius_km', '5', 'Phạm vi giao hàng tối đa', 1),
(6, 'currency', 'VND', 'Đơn vị tiền tệ', 1),
(7, 'default_language', 'vi-VN', 'Ngôn ngữ mặc định', 1),
(8, 'payment_methods', 'Cash,VietQR,MoMo,ZaloPay,Card',
 'Phương thức thanh toán hỗ trợ', 1),
(9, 'primary_color', '#B2292E', 'Màu nhận diện chính', 1);

-- =========================================================
-- REPORTING VIEWS
-- =========================================================

CREATE OR REPLACE VIEW vw_order_summary AS
SELECT
    co.order_id,
    co.invoice_number,
    co.customer_id,
    c.full_name AS customer_name,
    co.order_type,
    co.subtotal_amount,
    co.discount_amount,
    co.delivery_fee,
    co.total_amount,
    co.status AS order_status,
    p.payment_method,
    p.status AS payment_status,
    co.created_at
FROM customer_order co
LEFT JOIN customer c
    ON co.customer_id = c.customer_id
LEFT JOIN payment p
    ON co.order_id = p.order_id;

CREATE OR REPLACE VIEW vw_product_sales AS
SELECT
    p.product_id,
    p.product_name,
    SUM(oi.quantity) AS total_quantity_sold,
    SUM(oi.subtotal) AS total_sales
FROM order_item oi
JOIN product p
    ON oi.product_id = p.product_id
JOIN customer_order co
    ON oi.order_id = co.order_id
WHERE co.status NOT IN ('Cancelled')
GROUP BY p.product_id, p.product_name;
