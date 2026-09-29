CREATE DATABASE IF NOT EXISTS cafe_management_system;
USE cafe_management_system;

-- =====================================================
-- 1. ACCOUNT
-- =====================================================

CREATE TABLE account (
    account_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

-- =====================================================
-- 2. CUSTOMER
-- =====================================================

CREATE TABLE customer (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    loyalty_points INT NOT NULL DEFAULT 0,

    CONSTRAINT chk_customer_loyalty
        CHECK (loyalty_points >= 0),

    CONSTRAINT fk_customer_account
        FOREIGN KEY (account_id)
        REFERENCES account(account_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- 3. STAFF
-- =====================================================

CREATE TABLE staff (
    staff_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,

    CONSTRAINT fk_staff_account
        FOREIGN KEY (account_id)
        REFERENCES account(account_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- 4. CATEGORY
-- =====================================================

CREATE TABLE category (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL
);

-- =====================================================
-- 5. PRODUCT
-- =====================================================

CREATE TABLE product (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL,

    CONSTRAINT chk_product_price
        CHECK (price >= 0),

    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id)
        REFERENCES category(category_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- 6. RECIPE
-- =====================================================

CREATE TABLE recipe (
    recipe_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,

    CONSTRAINT fk_recipe_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- 7. INGREDIENT
-- =====================================================

CREATE TABLE ingredient (
    ingredient_id INT AUTO_INCREMENT PRIMARY KEY,
    ingredient_name VARCHAR(150) NOT NULL UNIQUE,
    unit VARCHAR(30) NOT NULL,
    current_stock DECIMAL(10,2) NOT NULL DEFAULT 0,
    minimum_stock DECIMAL(10,2) NOT NULL DEFAULT 0,

    CONSTRAINT chk_ingredient_stock
        CHECK (current_stock >= 0),

    CONSTRAINT chk_ingredient_minimum_stock
        CHECK (minimum_stock >= 0)
);

-- =====================================================
-- 8. RECIPE INGREDIENT
-- =====================================================

CREATE TABLE recipe_ingredient (
    recipe_id INT NOT NULL,
    ingredient_id INT NOT NULL,
    required_quantity DECIMAL(10,2) NOT NULL,

    PRIMARY KEY (recipe_id, ingredient_id),

    CONSTRAINT chk_required_quantity
        CHECK (required_quantity > 0),

    CONSTRAINT fk_recipe_ingredient_recipe
        FOREIGN KEY (recipe_id)
        REFERENCES recipe(recipe_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_recipe_ingredient_ingredient
        FOREIGN KEY (ingredient_id)
        REFERENCES ingredient(ingredient_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- 9. CUSTOMER ORDER
-- =====================================================

CREATE TABLE customer_order (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    cashier_id INT NULL,
    order_type VARCHAR(30) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_order_total
        CHECK (total_amount >= 0),

    CONSTRAINT fk_order_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_order_cashier
        FOREIGN KEY (cashier_id)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

-- =====================================================
-- 10. ORDER ITEM
-- =====================================================

CREATE TABLE order_item (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,

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

-- =====================================================
-- 11. PAYMENT
-- =====================================================

CREATE TABLE payment (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL UNIQUE,
    payment_method VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    transaction_id VARCHAR(150),
    paid_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_payment_amount
        CHECK (amount >= 0),

    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id)
        REFERENCES customer_order(order_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- 12. DELIVERY
-- =====================================================

CREATE TABLE delivery (
    delivery_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL UNIQUE,
    delivery_staff_id INT NULL,
    status VARCHAR(30) NOT NULL,
    delivered_at DATETIME,

    CONSTRAINT fk_delivery_order
        FOREIGN KEY (order_id)
        REFERENCES customer_order(order_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_delivery_staff
        FOREIGN KEY (delivery_staff_id)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

-- =====================================================
-- 13. COMPLAINT
-- =====================================================

CREATE TABLE complaint (
    complaint_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    order_id INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    resolution TEXT,
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
        ON DELETE RESTRICT
);

-- =====================================================
-- 14. REFUND
-- =====================================================

CREATE TABLE refund (
    refund_id INT AUTO_INCREMENT PRIMARY KEY,
    payment_id INT NOT NULL,
    processed_by INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    processed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_refund_amount
        CHECK (amount >= 0),

    CONSTRAINT fk_refund_payment
        FOREIGN KEY (payment_id)
        REFERENCES payment(payment_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_refund_staff
        FOREIGN KEY (processed_by)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- 15. RESTOCK REQUEST
-- =====================================================

CREATE TABLE restock_request (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    ingredient_id INT NOT NULL,
    created_by INT NOT NULL,
    approved_by INT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    request_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_restock_quantity
        CHECK (quantity > 0),

    CONSTRAINT fk_restock_ingredient
        FOREIGN KEY (ingredient_id)
        REFERENCES ingredient(ingredient_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_restock_creator
        FOREIGN KEY (created_by)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_restock_approver
        FOREIGN KEY (approved_by)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

-- =====================================================
-- 16. WORK SCHEDULE
-- =====================================================

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

-- =====================================================
-- 17. ATTENDANCE
-- =====================================================

CREATE TABLE attendance (
    attendance_id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL,
    check_in DATETIME,
    check_out DATETIME,
    status VARCHAR(30) NOT NULL DEFAULT 'PRESENT',

    CONSTRAINT fk_attendance_staff
        FOREIGN KEY (staff_id)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- 18. SHIFT
-- =====================================================

CREATE TABLE shift (
    shift_id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL,
    shift_date DATE NOT NULL,
    starting_cash DECIMAL(12,2) NOT NULL DEFAULT 0,
    actual_cash DECIMAL(12,2) DEFAULT 0,
    system_cash DECIMAL(12,2) DEFAULT 0,
    difference_amount DECIMAL(12,2) DEFAULT 0,
    status VARCHAR(30) NOT NULL,

    CONSTRAINT chk_shift_starting_cash
        CHECK (starting_cash >= 0),

    CONSTRAINT chk_shift_actual_cash
        CHECK (actual_cash >= 0),

    CONSTRAINT chk_shift_system_cash
        CHECK (system_cash >= 0),

    CONSTRAINT fk_shift_staff
        FOREIGN KEY (staff_id)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- 19. SHIFT HANDOVER
-- =====================================================

CREATE TABLE shift_handover (
    handover_id INT AUTO_INCREMENT PRIMARY KEY,
    shift_id INT NOT NULL,
    outgoing_staff_id INT NOT NULL,
    incoming_staff_id INT NOT NULL,
    handover_amount DECIMAL(12,2) NOT NULL,
    difference_amount DECIMAL(12,2) DEFAULT 0,
    difference_reason VARCHAR(255),
    confirmed_at DATETIME,

    CONSTRAINT chk_handover_amount
        CHECK (handover_amount >= 0),

    CONSTRAINT fk_handover_shift
        FOREIGN KEY (shift_id)
        REFERENCES shift(shift_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_handover_outgoing
        FOREIGN KEY (outgoing_staff_id)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_handover_incoming
        FOREIGN KEY (incoming_staff_id)
        REFERENCES staff(staff_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);