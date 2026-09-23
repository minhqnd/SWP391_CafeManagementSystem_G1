-- =====================================================
-- 1. CREATE DATABASE
-- =====================================================

CREATE DATABASE IF NOT EXISTS cafe_management_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE cafe_management_db;


-- =====================================================
-- 2. ROLE
-- =====================================================

CREATE TABLE roles (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =====================================================
-- 3. CUSTOMER
-- =====================================================

CREATE TABLE customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    address VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);


-- =====================================================
-- 4. STAFF
-- =====================================================

CREATE TABLE staff (
    staff_id INT AUTO_INCREMENT PRIMARY KEY,
    role_id INT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    hire_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_staff_role
        FOREIGN KEY (role_id)
        REFERENCES roles(role_id)
);


-- =====================================================
-- 5. CATEGORY
-- =====================================================

CREATE TABLE categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =====================================================
-- 6. PRODUCT
-- =====================================================

CREATE TABLE products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    image_url VARCHAR(500),
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_product_price
        CHECK (price >= 0),

    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id)
        REFERENCES categories(category_id)
);


-- =====================================================
-- 7. RECIPE
-- =====================================================

CREATE TABLE recipes (
    recipe_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL UNIQUE,
    instructions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_recipe_product
        FOREIGN KEY (product_id)
        REFERENCES products(product_id)
        ON DELETE CASCADE
);


-- =====================================================
-- 8. INGREDIENT
-- =====================================================

CREATE TABLE ingredients (
    ingredient_id INT AUTO_INCREMENT PRIMARY KEY,
    ingredient_name VARCHAR(100) NOT NULL UNIQUE,
    unit VARCHAR(30) NOT NULL,
    stock_quantity DECIMAL(10,2) DEFAULT 0,
    minimum_stock DECIMAL(10,2) DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_stock_quantity
        CHECK (stock_quantity >= 0),

    CONSTRAINT chk_minimum_stock
        CHECK (minimum_stock >= 0)
);


-- =====================================================
-- 9. RECIPE INGREDIENT
-- =====================================================

CREATE TABLE recipe_ingredients (
    recipe_id INT NOT NULL,
    ingredient_id INT NOT NULL,
    quantity_required DECIMAL(10,2) NOT NULL,

    PRIMARY KEY (recipe_id, ingredient_id),

    CONSTRAINT chk_quantity_required
        CHECK (quantity_required > 0),

    CONSTRAINT fk_recipe_ingredient_recipe
        FOREIGN KEY (recipe_id)
        REFERENCES recipes(recipe_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_recipe_ingredient_ingredient
        FOREIGN KEY (ingredient_id)
        REFERENCES ingredients(ingredient_id)
);


-- =====================================================
-- 10. CUSTOMER ORDER
-- =====================================================

CREATE TABLE customer_orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NULL,
    staff_id INT NULL,

    order_type ENUM('ONLINE', 'COUNTER') NOT NULL,
    fulfillment_type ENUM('PICKUP', 'DELIVERY') NOT NULL,

    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,

    order_status ENUM(
        'PENDING',
        'CONFIRMED',
        'PREPARING',
        'READY',
        'OUT_FOR_DELIVERY',
        'COMPLETED',
        'CANCELLED'
    ) DEFAULT 'PENDING',

    delivery_address VARCHAR(255),
    note VARCHAR(500),

    CONSTRAINT chk_order_total
        CHECK (total_amount >= 0),

    CONSTRAINT fk_order_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id),

    CONSTRAINT fk_order_staff
        FOREIGN KEY (staff_id)
        REFERENCES staff(staff_id)
);


-- =====================================================
-- 11. ORDER DETAIL
-- =====================================================

CREATE TABLE order_details (
    order_detail_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,

    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,

    CONSTRAINT chk_order_detail_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_detail_price
        CHECK (unit_price >= 0),

    CONSTRAINT chk_order_detail_subtotal
        CHECK (subtotal >= 0),

    CONSTRAINT fk_order_detail_order
        FOREIGN KEY (order_id)
        REFERENCES customer_orders(order_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_detail_product
        FOREIGN KEY (product_id)
        REFERENCES products(product_id)
);


-- =====================================================
-- 12. PAYMENT
-- =====================================================

CREATE TABLE payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL UNIQUE,

    amount DECIMAL(10,2) NOT NULL,
    payment_method ENUM(
        'CASH',
        'BANK_TRANSFER',
        'VNPAY',
        'SEPAY'
    ) NOT NULL,

    transaction_id VARCHAR(100) UNIQUE,
    payment_date TIMESTAMP NULL,

    payment_status ENUM(
        'PENDING',
        'SUCCESS',
        'FAILED',
        'REFUNDED'
    ) DEFAULT 'PENDING',

    CONSTRAINT chk_payment_amount
        CHECK (amount >= 0),

    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id)
        REFERENCES customer_orders(order_id)
);


-- =====================================================
-- 13. REFUND
-- =====================================================

CREATE TABLE refunds (
    refund_id INT AUTO_INCREMENT PRIMARY KEY,
    payment_id INT NOT NULL,
    processed_by INT NOT NULL,

    amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(500),
    refund_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    refund_status ENUM(
        'PENDING',
        'APPROVED',
        'COMPLETED',
        'REJECTED'
    ) DEFAULT 'PENDING',

    CONSTRAINT chk_refund_amount
        CHECK (amount >= 0),

    CONSTRAINT fk_refund_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(payment_id),

    CONSTRAINT fk_refund_staff
        FOREIGN KEY (processed_by)
        REFERENCES staff(staff_id)
);


-- =====================================================
-- 14. DELIVERY
-- =====================================================

CREATE TABLE deliveries (
    delivery_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL UNIQUE,
    delivery_staff_id INT NULL,

    delivery_address VARCHAR(255) NOT NULL,
    assigned_at TIMESTAMP NULL,
    accepted_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,

    delivery_status ENUM(
        'WAITING',
        'ASSIGNED',
        'ACCEPTED',
        'DELIVERING',
        'DELIVERED',
        'FAILED',
        'CANCELLED'
    ) DEFAULT 'WAITING',

    problem_description VARCHAR(500),

    CONSTRAINT fk_delivery_order
        FOREIGN KEY (order_id)
        REFERENCES customer_orders(order_id),

    CONSTRAINT fk_delivery_staff
        FOREIGN KEY (delivery_staff_id)
        REFERENCES staff(staff_id)
);


-- =====================================================
-- 15. COMPLAINT
-- =====================================================

CREATE TABLE complaints (
    complaint_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    order_id INT NULL,
    reviewed_by INT NULL,

    description TEXT NOT NULL,
    complaint_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    complaint_status ENUM(
        'SUBMITTED',
        'IN_REVIEW',
        'RESOLVED',
        'REJECTED'
    ) DEFAULT 'SUBMITTED',

    resolution TEXT,
    resolved_at TIMESTAMP NULL,

    CONSTRAINT fk_complaint_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id),

    CONSTRAINT fk_complaint_order
        FOREIGN KEY (order_id)
        REFERENCES customer_orders(order_id),

    CONSTRAINT fk_complaint_staff
        FOREIGN KEY (reviewed_by)
        REFERENCES staff(staff_id)
);


-- =====================================================
-- 16. LOYALTY POINT
-- =====================================================

CREATE TABLE loyalty_points (
    loyalty_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL UNIQUE,

    points INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_loyalty_points
        CHECK (points >= 0),

    CONSTRAINT fk_loyalty_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id)
        ON DELETE CASCADE
);


-- =====================================================
-- 17. WORK SCHEDULE
-- =====================================================

CREATE TABLE work_schedules (
    schedule_id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL,

    work_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    schedule_status ENUM(
        'ASSIGNED',
        'COMPLETED',
        'CANCELLED'
    ) DEFAULT 'ASSIGNED',

    CONSTRAINT fk_schedule_staff
        FOREIGN KEY (staff_id)
        REFERENCES staff(staff_id)
        ON DELETE CASCADE
);


-- =====================================================
-- 18. ATTENDANCE
-- =====================================================

CREATE TABLE attendances (
    attendance_id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL,

    check_in TIMESTAMP NULL,
    check_out TIMESTAMP NULL,

    attendance_status ENUM(
        'PRESENT',
        'LATE',
        'ABSENT',
        'LEAVE'
    ) DEFAULT 'PRESENT',

    attendance_date DATE NOT NULL,

    CONSTRAINT fk_attendance_staff
        FOREIGN KEY (staff_id)
        REFERENCES staff(staff_id)
        ON DELETE CASCADE
);


-- =====================================================
-- 19. RESTOCK REQUEST
-- =====================================================

CREATE TABLE restock_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    requested_by INT NOT NULL,
    reviewed_by INT NULL,

    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    request_status ENUM(
        'PENDING',
        'REVIEWING',
        'APPROVED',
        'REJECTED',
        'COMPLETED'
    ) DEFAULT 'PENDING',

    note VARCHAR(500),

    CONSTRAINT fk_restock_requested_by
        FOREIGN KEY (requested_by)
        REFERENCES staff(staff_id),

    CONSTRAINT fk_restock_reviewed_by
        FOREIGN KEY (reviewed_by)
        REFERENCES staff(staff_id)
);


-- =====================================================
-- 20. RESTOCK DETAIL
-- =====================================================

CREATE TABLE restock_details (
    request_detail_id INT AUTO_INCREMENT PRIMARY KEY,
    request_id INT NOT NULL,
    ingredient_id INT NOT NULL,

    quantity DECIMAL(10,2) NOT NULL,

    CONSTRAINT chk_restock_quantity
        CHECK (quantity > 0),

    CONSTRAINT fk_restock_detail_request
        FOREIGN KEY (request_id)
        REFERENCES restock_requests(request_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_restock_detail_ingredient
        FOREIGN KEY (ingredient_id)
        REFERENCES ingredients(ingredient_id)
);


-- =====================================================
-- 21. INVENTORY TRANSACTION
-- =====================================================

CREATE TABLE inventory_transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    ingredient_id INT NOT NULL,
    staff_id INT NOT NULL,

    transaction_type ENUM(
        'RECEIVE',
        'USE',
        'ADJUST',
        'RETURN'
    ) NOT NULL,

    quantity DECIMAL(10,2) NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    note VARCHAR(500),

    CONSTRAINT chk_inventory_transaction_quantity
        CHECK (quantity > 0),

    CONSTRAINT fk_inventory_transaction_ingredient
        FOREIGN KEY (ingredient_id)
        REFERENCES ingredients(ingredient_id),

    CONSTRAINT fk_inventory_transaction_staff
        FOREIGN KEY (staff_id)
        REFERENCES staff(staff_id)
);


-- =====================================================
-- 22. SAMPLE ROLES
-- =====================================================

INSERT INTO roles (role_name, description)
VALUES
('ADMIN', 'Manages the whole café management system'),
('STAFF', 'Handles counter orders and customer support'),
('BARISTA', 'Prepares drinks and updates preparation status'),
('DELIVERY_STAFF', 'Accepts and delivers customer orders');


-- =====================================================
-- 23. SAMPLE CATEGORIES
-- =====================================================

INSERT INTO categories (category_name, description)
VALUES
('Vietnamese Coffee', 'Traditional Vietnamese coffee products'),
('Espresso', 'Espresso-based coffee products'),
('Tea', 'Tea and fruit tea products'),
('Pastry', 'Cakes and bakery products');