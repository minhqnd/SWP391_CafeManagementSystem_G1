-- BF10/BF11: select the application's existing database before running this file.
-- Only add missing unique indexes. Do not update, rename or delete application records.
-- Stop if old data conflicts with the rules; never resolve duplicates automatically.
DELIMITER $$
DROP PROCEDURE IF EXISTS bf10_bf11_ensure_unique_guards$$
CREATE PROCEDURE bf10_bf11_ensure_unique_guards()
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email IS NOT NULL
               GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate emails found; no account records were changed.';
    END IF;
    IF EXISTS (SELECT 1 FROM products
               GROUP BY LOWER(TRIM(name)) HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate product names found; no product records were changed.';
    END IF;
    IF NOT EXISTS (
        SELECT index_name FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'users' AND non_unique = 0
        GROUP BY index_name HAVING COUNT(*) = 1 AND MAX(column_name) = 'email'
    ) THEN
        ALTER TABLE users ADD UNIQUE KEY uq_users_email (email);
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
CALL bf10_bf11_ensure_unique_guards();
DROP PROCEDURE bf10_bf11_ensure_unique_guards;
