-- Update sample data for Cashier Tran Thi Binh (user_id = 3) on 2026-10-03 Afternoon Shift
USE aura_cafe_erp;

-- Clean up any existing orders between 12:00 and 17:00 on 2026-10-03 for cashier 3
DELETE p FROM payments p JOIN orders o ON p.order_id = o.id 
WHERE o.cashier_id = 3 AND o.created_at >= '2026-10-03 12:00:00' AND o.created_at <= '2026-10-03 17:00:00';

DELETE FROM order_items WHERE order_id IN (
    SELECT id FROM (SELECT id FROM orders WHERE cashier_id = 3 AND created_at >= '2026-10-03 12:00:00' AND created_at <= '2026-10-03 17:00:00') t
);

DELETE FROM orders 
WHERE cashier_id = 3 AND created_at >= '2026-10-03 12:00:00' AND created_at <= '2026-10-03 17:00:00';

-- Insert 6 orders for afternoon shift (Total = 611,000đ)
-- Order 1: 96,000 CASH
INSERT INTO orders (order_code, customer_id, cashier_id, order_type, status, total_amount, discount_amount, final_amount, created_at)
VALUES ('ORD-20261003-01', 5, 3, 'DINE_IN', 'COMPLETED', 96000, 0, 96000, '2026-10-03 12:30:00');
SET @o1 = LAST_INSERT_ID();
INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at)
VALUES (@o1, 'CASH', 'CSH-1003-01', 96000, 'SUCCESS', '2026-10-03 12:32:00');

-- Order 2: 120,000 CASH
INSERT INTO orders (order_code, customer_id, cashier_id, order_type, status, total_amount, discount_amount, final_amount, created_at)
VALUES ('ORD-20261003-02', 5, 3, 'TAKEAWAY', 'COMPLETED', 120000, 0, 120000, '2026-10-03 13:45:00');
SET @o2 = LAST_INSERT_ID();
INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at)
VALUES (@o2, 'CASH', 'CSH-1003-02', 120000, 'SUCCESS', '2026-10-03 13:47:00');

-- Order 3: 80,000 CASH
INSERT INTO orders (order_code, customer_id, cashier_id, order_type, status, total_amount, discount_amount, final_amount, created_at)
VALUES ('ORD-20261003-03', 5, 3, 'DINE_IN', 'COMPLETED', 80000, 0, 80000, '2026-10-03 15:15:00');
SET @o3 = LAST_INSERT_ID();
INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at)
VALUES (@o3, 'CASH', 'CSH-1003-03', 80000, 'SUCCESS', '2026-10-03 15:16:00');

-- Order 4: 110,000 VNPAY
INSERT INTO orders (order_code, customer_id, cashier_id, order_type, status, total_amount, discount_amount, final_amount, created_at)
VALUES ('ORD-20261003-04', 5, 3, 'DINE_IN', 'COMPLETED', 110000, 0, 110000, '2026-10-03 14:10:00');
SET @o4 = LAST_INSERT_ID();
INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at)
VALUES (@o4, 'VNPAY', 'VNP-1003-01', 110000, 'SUCCESS', '2026-10-03 14:12:00');

-- Order 5: 105,000 MOMO
INSERT INTO orders (order_code, customer_id, cashier_id, order_type, status, total_amount, discount_amount, final_amount, created_at)
VALUES ('ORD-20261003-05', 5, 3, 'TAKEAWAY', 'COMPLETED', 105000, 0, 105000, '2026-10-03 14:50:00');
SET @o5 = LAST_INSERT_ID();
INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at)
VALUES (@o5, 'MOMO', 'MOM-1003-01', 105000, 'SUCCESS', '2026-10-03 14:51:00');

-- Order 6: 100,000 MOMO
INSERT INTO orders (order_code, customer_id, cashier_id, order_type, status, total_amount, discount_amount, final_amount, created_at)
VALUES ('ORD-20261003-06', 5, 3, 'DINE_IN', 'COMPLETED', 100000, 0, 100000, '2026-10-03 16:20:00');
SET @o6 = LAST_INSERT_ID();
INSERT INTO payments (order_id, payment_method, transaction_code, amount, status, paid_at)
VALUES (@o6, 'MOMO', 'MOM-1003-02', 100000, 'SUCCESS', '2026-10-03 16:22:00');

-- Ensure work_schedule #2 is PRESENT with 500k starting cash
UPDATE work_schedules 
SET status = 'PRESENT', starting_cash = 500000.00, check_in_time = '2026-10-03 12:00:00', check_out_time = NULL, ending_cash_system = NULL, ending_cash_actual = NULL, note = 'Cashier shift'
WHERE id = 2;
