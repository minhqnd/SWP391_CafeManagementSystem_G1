
-- =============================================
-- AURA CAFE MANAGEMENT SYSTEM
-- MySQL 8.0+
-- =============================================

DROP DATABASE IF EXISTS aura_cafe;

CREATE DATABASE aura_cafe
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE aura_cafe;

-- =============================================
-- 1. ROLES & USERS
-- =============================================

CREATE TABLE roles (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id INT NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(150) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    date_of_birth DATE,
    gender ENUM('Male','Female','Other'),
    status ENUM('Active','Inactive','Locked') DEFAULT 'Active',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

-- =============================================
-- 2. BRANCHES & EMPLOYEES
-- =============================================

CREATE TABLE branches (
    branch_id INT AUTO_INCREMENT PRIMARY KEY,
    branch_name VARCHAR(120) NOT NULL,
    address VARCHAR(255) NOT NULL,
    district VARCHAR(100),
    city VARCHAR(100),
    phone VARCHAR(20),
    opening_time TIME,
    closing_time TIME,
    status ENUM('Open','Closed') DEFAULT 'Open'
);

CREATE TABLE employees (
    employee_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    branch_id INT,
    position ENUM(
        'Admin','Staff','Barista','Cashier','Delivery Staff'
    ) NOT NULL,
    hire_date DATE,
    salary DECIMAL(12,2) DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
);

CREATE TABLE work_shifts (
    shift_id INT AUTO_INCREMENT PRIMARY KEY,
    shift_name VARCHAR(50) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL
);

CREATE TABLE employee_schedules (
    schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    shift_id INT NOT NULL,
    work_date DATE NOT NULL,
    note VARCHAR(255),
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
    FOREIGN KEY (shift_id) REFERENCES work_shifts(shift_id),
    UNIQUE (employee_id, shift_id, work_date)
);

CREATE TABLE attendance (
    attendance_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    check_in DATETIME,
    check_out DATETIME,
    note VARCHAR(255),
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
    UNIQUE (employee_id, attendance_date)
);

-- =============================================
-- 3. CUSTOMER ADDRESSES
-- =============================================

CREATE TABLE addresses (
    address_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    ward VARCHAR(100),
    district VARCHAR(100),
    city VARCHAR(100),
    is_default BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =============================================
-- 4. CATEGORIES & PRODUCTS
-- =============================================

CREATE TABLE categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    image_url VARCHAR(500),
    status ENUM('Active','Inactive') DEFAULT 'Active'
);

CREATE TABLE products (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    base_price DECIMAL(12,2) NOT NULL,
    status ENUM('Available','Unavailable') DEFAULT 'Available',
    is_featured BOOLEAN DEFAULT FALSE,
    is_seasonal BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(category_id),
    CHECK (base_price >= 0)
);

CREATE TABLE product_variants (
    variant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    variant_name VARCHAR(50) NOT NULL,
    extra_price DECIMAL(12,2) DEFAULT 0,
    status BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    UNIQUE (product_id, variant_name)
);

CREATE TABLE product_options (
    option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    option_name VARCHAR(100) NOT NULL,
    option_type ENUM('Single','Multiple') DEFAULT 'Single',
    is_required BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

CREATE TABLE option_values (
    value_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    option_id BIGINT NOT NULL,
    value_name VARCHAR(100) NOT NULL,
    extra_price DECIMAL(12,2) DEFAULT 0,
    FOREIGN KEY (option_id) REFERENCES product_options(option_id),
    UNIQUE (option_id, value_name)
);

-- =============================================
-- 5. CART
-- =============================================

CREATE TABLE carts (
    cart_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE cart_items (
    cart_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    variant_id BIGINT,
    quantity INT NOT NULL DEFAULT 1,
    note VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES carts(cart_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id),
    CHECK (quantity > 0)
);

CREATE TABLE cart_item_options (
    cart_item_option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_item_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    value_id BIGINT NOT NULL,
    FOREIGN KEY (cart_item_id)
        REFERENCES cart_items(cart_item_id) ON DELETE CASCADE,
    FOREIGN KEY (option_id) REFERENCES product_options(option_id),
    FOREIGN KEY (value_id) REFERENCES option_values(value_id)
);

-- =============================================
-- 6. MEMBERSHIP & LOYALTY
-- =============================================

CREATE TABLE membership_tiers (
    tier_id INT AUTO_INCREMENT PRIMARY KEY,
    tier_name VARCHAR(50) NOT NULL UNIQUE,
    min_points INT NOT NULL,
    max_points INT,
    point_multiplier DECIMAL(4,2) DEFAULT 1.00,
    description TEXT
);

CREATE TABLE memberships (
    membership_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    tier_id INT NOT NULL,
    total_points INT DEFAULT 0,
    available_points INT DEFAULT 0,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (tier_id) REFERENCES membership_tiers(tier_id)
);

CREATE TABLE loyalty_transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    membership_id BIGINT NOT NULL,
    order_id BIGINT,
    points_change INT NOT NULL,
    transaction_type ENUM(
        'Earn','Redeem','Adjustment','Expire'
    ) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (membership_id)
        REFERENCES memberships(membership_id)
);

-- =============================================
-- 7. PROMOTIONS & VOUCHERS
-- =============================================

CREATE TABLE promotions (
    promotion_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    promotion_name VARCHAR(150) NOT NULL,
    description TEXT,
    discount_type ENUM('Fixed','Percentage') NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    start_date DATETIME,
    end_date DATETIME,
    status ENUM('Active','Inactive','Expired') DEFAULT 'Active'
);

CREATE TABLE promotion_products (
    promotion_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (promotion_id, product_id),
    FOREIGN KEY (promotion_id)
        REFERENCES promotions(promotion_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id)
        REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE vouchers (
    voucher_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    voucher_code VARCHAR(50) NOT NULL UNIQUE,
    voucher_name VARCHAR(150) NOT NULL,
    discount_type ENUM('Fixed','Percentage') NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    min_order_value DECIMAL(12,2) DEFAULT 0,
    max_discount DECIMAL(12,2),
    quantity INT DEFAULT 0,
    start_date DATETIME,
    end_date DATETIME,
    status ENUM('Active','Inactive','Expired') DEFAULT 'Active'
);

-- =============================================
-- 8. ORDERS
-- =============================================

CREATE TABLE orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_code VARCHAR(30) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    branch_id INT,
    address_id BIGINT,
    order_type ENUM(
        'Delivery','Pickup','DineIn','Counter'
    ) NOT NULL,
    order_status ENUM(
        'Pending','Confirmed','Preparing','Ready',
        'Delivering','Completed','Cancelled'
    ) DEFAULT 'Pending',
    payment_status ENUM(
        'Unpaid','Paid','Failed','Refunded','Partially Refunded'
    ) DEFAULT 'Unpaid',
    subtotal DECIMAL(12,2) DEFAULT 0,
    discount_amount DECIMAL(12,2) DEFAULT 0,
    delivery_fee DECIMAL(12,2) DEFAULT 0,
    total_amount DECIMAL(12,2) DEFAULT 0,
    customer_note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id),
    FOREIGN KEY (address_id) REFERENCES addresses(address_id)
);

CREATE TABLE order_items (
    order_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(150) NOT NULL,
    variant_name VARCHAR(50),
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    note VARCHAR(255),
    line_total DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (order_id)
        REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id)
        REFERENCES products(product_id) ON DELETE SET NULL
);

CREATE TABLE order_item_options (
    item_option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    option_name VARCHAR(100) NOT NULL,
    value_name VARCHAR(100) NOT NULL,
    extra_price DECIMAL(12,2) DEFAULT 0,
    FOREIGN KEY (order_item_id)
        REFERENCES order_items(order_item_id) ON DELETE CASCADE
);

CREATE TABLE order_vouchers (
    order_id BIGINT NOT NULL,
    voucher_id BIGINT NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (order_id, voucher_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id)
);

CREATE TABLE voucher_redemptions (
    redemption_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    voucher_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    redeemed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- =============================================
-- 9. PAYMENTS
-- =============================================

CREATE TABLE payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_method ENUM(
        'Cash','BankTransfer','EWallet','Card'
    ) NOT NULL,
    transaction_code VARCHAR(100) UNIQUE,
    amount DECIMAL(12,2) NOT NULL,
    payment_status ENUM(
        'Pending','Success','Failed','Refunded'
    ) DEFAULT 'Pending',
    paid_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE TABLE order_status_history (
    history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    changed_by BIGINT,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    note TEXT,
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id)
        REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by)
        REFERENCES users(user_id) ON DELETE SET NULL
);

-- =============================================
-- 10. DELIVERY
-- =============================================

CREATE TABLE deliveries (
    delivery_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    employee_id BIGINT,
    delivery_status ENUM(
        'Waiting','Assigned','Delivering','Delivered','Failed'
    ) DEFAULT 'Waiting',
    assigned_at DATETIME,
    delivered_at DATETIME,
    delivery_note TEXT,
    FOREIGN KEY (order_id)
        REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (employee_id)
        REFERENCES employees(employee_id) ON DELETE SET NULL
);

CREATE TABLE delivery_status_history (
    delivery_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    delivery_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    note TEXT,
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (delivery_id)
        REFERENCES deliveries(delivery_id) ON DELETE CASCADE
);

-- =============================================
-- 11. CANCELLATION & REFUNDS
-- =============================================

CREATE TABLE cancellation_requests (
    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    request_status ENUM(
        'Pending','Approved','Rejected'
    ) DEFAULT 'Pending',
    reviewed_by BIGINT,
    reviewed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
);

CREATE TABLE refund_requests (
    refund_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_id BIGINT,
    requested_by BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    reason TEXT,
    refund_status ENUM(
        'Pending','Approved','Rejected','Completed'
    ) DEFAULT 'Pending',
    reviewed_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id),
    FOREIGN KEY (requested_by) REFERENCES users(user_id),
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
);

-- =============================================
-- 12. COMPLAINTS
-- =============================================

CREATE TABLE complaints (
    complaint_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    subject VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    complaint_status ENUM(
        'Pending','Processing','Resolved','Rejected'
    ) DEFAULT 'Pending',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE TABLE complaint_responses (
    response_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    response_content TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (complaint_id)
        REFERENCES complaints(complaint_id) ON DELETE CASCADE,
    FOREIGN KEY (admin_id) REFERENCES users(user_id)
);

-- =============================================
-- 13. NOTIFICATIONS & SETTINGS
-- =============================================

CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE system_settings (
    setting_id INT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    description VARCHAR(255),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

-- =============================================
-- SAMPLE DATA
-- =============================================

-- ROLES
INSERT INTO roles (role_name, description) VALUES
('Admin', 'System administrator'),
('Customer', 'Customer account'),
('Staff', 'Cafe staff'),
('Barista', 'Drink preparation'),
('Cashier', 'Payment and counter service'),
('Delivery Staff', 'Order delivery');

-- BRANCHES
INSERT INTO branches
(branch_name, address, district, city, phone,
 opening_time, closing_time)
VALUES
('Aura Trang Tien', '28 Pho Trang Tien',
 'Hoan Kiem', 'Ha Noi', '19006868',
 '07:00:00', '22:30:00'),
('Aura Hoang Hoa Tham', '120 Hoang Hoa Tham',
 'Ba Dinh', 'Ha Noi', '19006868',
 '07:00:00', '22:30:00'),
('Aura Le Duan', '45 Le Duan',
 'Ben Nghe, District 1', 'Ho Chi Minh City',
 '19006868', '07:00:00', '22:30:00'),
('Aura Thao Dien', '88 Thao Dien',
 'Thu Duc', 'Ho Chi Minh City',
 '19006868', '06:30:00', '23:00:00');

-- USERS
-- Password hashes are demo placeholders.
-- Replace with secure hashes in your application.
INSERT INTO users
(role_id, full_name, email, phone, password_hash, status)
VALUES
(1, 'Aura Admin', 'admin@auracafe.local',
 '0900000001', 'DEMO_HASH', 'Active'),
(2, 'Nguyen Minh Anh', 'customer@auracafe.local',
 '0900000002', 'DEMO_HASH', 'Active'),
(3, 'Tran Van Nam', 'staff@auracafe.local',
 '0900000003', 'DEMO_HASH', 'Active'),
(4, 'Le Thu Ha', 'barista@auracafe.local',
 '0900000004', 'DEMO_HASH', 'Active'),
(5, 'Pham Quoc Bao', 'cashier@auracafe.local',
 '0900000005', 'DEMO_HASH', 'Active'),
(6, 'Do Thanh Long', 'delivery@auracafe.local',
 '0900000006', 'DEMO_HASH', 'Active');

-- EMPLOYEES
INSERT INTO employees
(user_id, branch_id, position, hire_date, salary)
VALUES
(1, 1, 'Admin', '2025-01-01', 0),
(3, 1, 'Staff', '2025-02-01', 8000000),
(4, 1, 'Barista', '2025-02-01', 8500000),
(5, 1, 'Cashier', '2025-02-01', 8000000),
(6, 1, 'Delivery Staff', '2025-02-01', 7500000);

-- ADDRESSES
INSERT INTO addresses
(user_id, recipient_name, phone, address_line,
 ward, district, city, is_default)
VALUES
(2, 'Nguyen Minh Anh', '0900000002',
 '12 Hang Bai', 'Trang Tien',
 'Hoan Kiem', 'Ha Noi', TRUE);

-- CATEGORIES
INSERT INTO categories
(category_name, description)
VALUES
('Ca phe truyen thong', 'Traditional coffee'),
('Tra moc trai cay', 'Fruit and herbal tea'),
('Cold Brew Series', 'Cold brew coffee'),
('Ca phe sang tao', 'Creative coffee'),
('Seasonal Releases', 'Seasonal specialty drinks'),
('Banh ngot', 'Bakery and desserts'),
('Combo', 'Food and drink combos');

-- PRODUCTS
INSERT INTO products
(category_id, product_name, description,
 base_price, status, is_featured, is_seasonal)
VALUES
(1, 'Ca Phe Muoi Aura',
 'Ca phe phin voi lop kem beo man',
 55000, 'Available', TRUE, FALSE),
(2, 'Tra Sen Vang',
 'Tra O Long, hat sen Hue va cu nang',
 62000, 'Available', TRUE, FALSE),
(3, 'Cold Brew Cam Que',
 'Cold brew voi cam say va que',
 65000, 'Available', TRUE, FALSE),
(4, 'Bac Xiu Kem Trung',
 'Sua dac, robusta va kem trung',
 59000, 'Available', TRUE, FALSE),
(5, 'Latte Hat De Cuoi',
 'Sua hat de cuoi va espresso Colombia',
 69000, 'Available', FALSE, TRUE),
(5, 'Cascara Sparkling Nho Do',
 'Tra vo ca phe, nho do va soda',
 68000, 'Available', FALSE, TRUE),
(5, 'Mocha Bo Dau Phong',
 'Socola Bi, kem bo dau phong va espresso',
 72000, 'Available', FALSE, TRUE),
(1, 'Espresso',
 'Espresso single origin',
 45000, 'Available', FALSE, FALSE),
(1, 'Americano',
 'Espresso voi nuoc',
 49000, 'Available', FALSE, FALSE),
(2, 'Tra Dao Cam Sa',
 'Tra dao, cam va sa',
 58000, 'Available', FALSE, FALSE),
(6, 'Tiramisu',
 'Banh tiramisu',
 55000, 'Available', FALSE, FALSE),
(7, 'Combo Coffee and Cake',
 'Mot do uong va mot banh tuy chon',
 99000, 'Available', FALSE, FALSE);

-- PRODUCT VARIANTS
INSERT INTO product_variants
(product_id, variant_name, extra_price)
VALUES
(1, 'Regular', 0),
(1, 'Large', 10000),
(2, 'Regular', 0),
(2, 'Large', 10000),
(3, 'Regular', 0),
(3, 'Large', 10000),
(4, 'Regular', 0),
(4, 'Large', 10000),
(5, 'Hot', 0),
(5, 'Iced', 0),
(6, 'Iced', 0),
(7, 'Hot', 0),
(7, 'Iced', 0);

-- PRODUCT OPTIONS
INSERT INTO product_options
(product_id, option_name, option_type, is_required)
VALUES
(1, 'Sugar level', 'Single', FALSE),
(1, 'Ice level', 'Single', FALSE),
(5, 'Milk choice', 'Single', FALSE),
(10, 'Sugar level', 'Single', FALSE);

-- OPTION VALUES
INSERT INTO option_values
(option_id, value_name, extra_price)
VALUES
(1, '0%', 0),
(1, '50%', 0),
(1, '100%', 0),
(2, 'No ice', 0),
(2, 'Less ice', 0),
(2, 'Regular ice', 0),
(3, 'Regular milk', 0),
(3, 'Oat milk', 10000),
(3, 'Almond milk', 12000),
(4, '0%', 0),
(4, '50%', 0),
(4, '100%', 0);

-- MEMBERSHIP TIERS
INSERT INTO membership_tiers
(tier_name, min_points, max_points,
 point_multiplier, description)
VALUES
('Silver', 0, 299, 1.00,
 'Tich diem co ban va uu dai sinh nhat'),
('Gold', 300, 799, 1.50,
 'He so tich diem 1.5 va dac quyen Gold'),
('Diamond', 800, NULL, 2.00,
 'He so tich diem 2.0 va dac quyen Diamond');

-- MEMBERSHIPS
INSERT INTO memberships
(user_id, tier_id, total_points, available_points)
VALUES
(2, 1, 120, 120);

-- PROMOTIONS
INSERT INTO promotions
(promotion_name, description, discount_type,
 discount_value, start_date, end_date, status)
VALUES
('Aura Seasonal', 'Seasonal promotion',
 'Percentage', 10,
 '2026-10-01 00:00:00',
 '2026-10-31 23:59:59',
 'Active');

INSERT INTO promotion_products (promotion_id, product_id)
VALUES (1, 5), (1, 6), (1, 7);

-- VOUCHERS
INSERT INTO vouchers
(voucher_code, voucher_name, discount_type,
 discount_value, min_order_value, max_discount,
 quantity, start_date, end_date, status)
VALUES
('WELCOME50', 'Welcome 50K', 'Fixed', 50000,
 100000, NULL, 1000,
 '2026-01-01 00:00:00',
 '2026-12-31 23:59:59', 'Active'),
('AURA20', 'Aura discount 20%', 'Percentage', 20,
 150000, 50000, 500,
 '2026-01-01 00:00:00',
 '2026-12-31 23:59:59', 'Active');

-- CART
INSERT INTO carts (user_id) VALUES (2);

INSERT INTO cart_items
(cart_id, product_id, variant_id, quantity, note)
VALUES
(1, 1, 1, 1, 'Less ice');

INSERT INTO cart_item_options
(cart_item_id, option_id, value_id)
VALUES
(1, 2, 5);

-- ORDERS
INSERT INTO orders
(order_code, user_id, branch_id, address_id,
 order_type, order_status, payment_status,
 subtotal, discount_amount, delivery_fee,
 total_amount, customer_note)
VALUES
('AURA-DEMO-0001', 2, 1, 1,
 'Delivery', 'Completed', 'Paid',
 55000, 0, 15000, 70000,
 'Demo order');

-- ORDER ITEMS
INSERT INTO order_items
(order_id, product_id, product_name,
 variant_name, unit_price, quantity,
 note, line_total)
VALUES
(1, 1, 'Ca Phe Muoi Aura',
 'Regular', 55000, 1, 'Less ice', 55000);

INSERT INTO order_item_options
(order_item_id, option_name, value_name, extra_price)
VALUES
(1, 'Ice level', 'Less ice', 0);

-- PAYMENTS
INSERT INTO payments
(order_id, payment_method, transaction_code,
 amount, payment_status, paid_at)
VALUES
(1, 'EWallet', 'DEMO-TXN-0001',
 70000, 'Success', CURRENT_TIMESTAMP);

-- DELIVERIES
INSERT INTO deliveries
(order_id, employee_id, delivery_status,
 assigned_at, delivered_at, delivery_note)
VALUES
(1, 5, 'Delivered',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
 'Demo delivery');

INSERT INTO delivery_status_history
(delivery_id, status, note)
VALUES
(1, 'Waiting', 'Created delivery'),
(1, 'Assigned', 'Assigned to delivery staff'),
(1, 'Delivering', 'Out for delivery'),
(1, 'Delivered', 'Delivery completed');

-- ORDER STATUS HISTORY
INSERT INTO order_status_history
(order_id, changed_by, old_status, new_status, note)
VALUES
(1, 3, NULL, 'Pending', 'Order created'),
(1, 3, 'Pending', 'Confirmed', 'Order confirmed'),
(1, 4, 'Confirmed', 'Preparing', 'Preparation started'),
(1, 4, 'Preparing', 'Ready', 'Drink ready'),
(1, 6, 'Ready', 'Delivering', 'Out for delivery'),
(1, 6, 'Delivering', 'Completed', 'Delivered');

-- VOUCHER REDEMPTION
INSERT INTO order_vouchers
(order_id, voucher_id, discount_amount)
VALUES (1, 1, 0);

INSERT INTO voucher_redemptions
(voucher_id, user_id, order_id)
VALUES (1, 2, 1);

-- LOYALTY TRANSACTIONS
INSERT INTO loyalty_transactions
(membership_id, order_id, points_change,
 transaction_type, description)
VALUES
(1, 1, 7, 'Earn', 'Demo points from order');

-- COMPLAINTS
INSERT INTO complaints
(user_id, order_id, subject, content, complaint_status)
VALUES
(2, 1, 'Demo feedback',
 'Chat luong don hang tot',
 'Resolved');

INSERT INTO complaint_responses
(complaint_id, admin_id, response_content)
VALUES
(1, 1, 'Aura Cafe cam on ban da gui phan hoi');

-- CANCELLATION REQUESTS
INSERT INTO cancellation_requests
(order_id, user_id, reason, request_status,
 reviewed_by, reviewed_at)
VALUES
(1, 2, 'Demo cancellation request',
 'Rejected', 1, CURRENT_TIMESTAMP);

-- REFUND REQUESTS
INSERT INTO refund_requests
(order_id, payment_id, requested_by, amount,
 reason, refund_status, reviewed_by)
VALUES
(1, 1, 2, 10000,
 'Demo refund request', 'Rejected', 1);

-- WORK SHIFTS
INSERT INTO work_shifts
(shift_name, start_time, end_time)
VALUES
('Morning', '07:00:00', '13:00:00'),
('Afternoon', '13:00:00', '18:00:00'),
('Evening', '18:00:00', '22:30:00');

-- EMPLOYEE SCHEDULES
INSERT INTO employee_schedules
(employee_id, shift_id, work_date)
VALUES
(2, 1, '2026-10-05'),
(3, 1, '2026-10-05'),
(4, 2, '2026-10-05'),
(5, 2, '2026-10-05');

-- ATTENDANCE
INSERT INTO attendance
(employee_id, attendance_date, check_in,
 check_out, note)
VALUES
(2, '2026-10-01',
 '2026-10-01 07:00:00',
 '2026-10-01 13:00:00',
 'Demo attendance');

-- NOTIFICATIONS
INSERT INTO notifications
(user_id, title, message, is_read)
VALUES
(2, 'Welcome to Aura Cafe',
 'Chao mung ban den voi Aura Rewards', FALSE),
(3, 'New order',
 'Co don hang moi can xu ly', FALSE);

-- SYSTEM SETTINGS
INSERT INTO system_settings
(setting_key, setting_value, description)
VALUES
('cafe_name', 'Aura Cafe', 'Displayed cafe name'),
('delivery_radius_km', '5', 'Delivery radius in kilometers'),
('loyalty_points_per_amount', '10000',
 'Spending amount for one base point'),
('support_hotline', '19006868',
 'Customer support hotline');
 
USE aura_cafe;

-- View product catalog
CREATE VIEW v_product_catalog AS
SELECT
    p.product_id,
    p.product_name,
    c.category_name,
    p.description,
    p.base_price,
    p.image_url,
    p.status,
    p.is_featured,
    p.is_seasonal
FROM products p
JOIN categories c
    ON p.category_id = c.category_id;

-- View order summary
CREATE VIEW v_order_summary AS
SELECT
    o.order_id,
    o.order_code,
    u.full_name AS customer_name,
    o.order_type,
    o.order_status,
    o.payment_status,
    o.subtotal,
    o.discount_amount,
    o.delivery_fee,
    o.total_amount,
    o.created_at
FROM orders o
JOIN users u
    ON o.user_id = u.user_id;

-- View revenue by day
CREATE VIEW v_daily_revenue AS
SELECT
    DATE(created_at) AS order_date,
    COUNT(*) AS total_orders,
    SUM(total_amount) AS total_revenue
FROM orders
WHERE payment_status = 'Paid'
  AND order_status <> 'Cancelled'
GROUP BY DATE(created_at);

-- Check tables
SHOW TABLES;

-- Check sample data
SELECT * FROM roles;
SELECT * FROM users;
SELECT * FROM branches;
SELECT * FROM categories;
SELECT * FROM products;
SELECT * FROM membership_tiers;
SELECT * FROM memberships;
SELECT * FROM orders;
SELECT * FROM order_items;
SELECT * FROM payments;

-- Check views
SELECT * FROM v_product_catalog;
SELECT * FROM v_order_summary;
SELECT * FROM v_daily_revenue;