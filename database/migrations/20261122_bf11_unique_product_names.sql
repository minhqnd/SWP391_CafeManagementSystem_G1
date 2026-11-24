-- Chặn hai lần lưu đồng thời tạo sản phẩm trùng tên.
-- Không đổi tên hoặc xóa sản phẩm đang có. Nếu dữ liệu cũ trùng thì dừng để xử lý riêng.
DELIMITER $$
DROP PROCEDURE IF EXISTS bf11_add_unique_product_name$$
CREATE PROCEDURE bf11_add_unique_product_name()
BEGIN
    IF EXISTS (SELECT 1 FROM products GROUP BY LOWER(TRIM(name)) HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Existing product names are duplicated; no records were changed.';
    END IF;
    IF NOT EXISTS (
        SELECT index_name FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'products' AND non_unique = 0
        GROUP BY index_name HAVING COUNT(*) = 1 AND MAX(column_name) = 'name'
    ) THEN
        ALTER TABLE products ADD UNIQUE KEY uq_products_name (name);
    END IF;
END$$
DELIMITER ;
CALL bf11_add_unique_product_name();
DROP PROCEDURE bf11_add_unique_product_name;
