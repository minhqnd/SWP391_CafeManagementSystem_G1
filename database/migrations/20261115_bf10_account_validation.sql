-- BF10: select the existing test schema before running this file.
-- No DROP DATABASE / data reset. Pause account writes while applying this migration.
-- Duplicate non-empty emails stop the migration; inspect and resolve them manually.
DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_bf10_account_validation$$
CREATE PROCEDURE migrate_bf10_account_validation()
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email IS NOT NULL AND TRIM(email) <> ''
               GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate emails found. Resolve them before applying BF10 migration.';
    END IF;
    UPDATE users SET email = NULLIF(LOWER(TRIM(email)), '');
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'users'
                     AND column_name = 'email' AND non_unique = 0) THEN
        ALTER TABLE users ADD UNIQUE KEY uq_users_email (email);
    END IF;
    -- Demo seed accounts receive the documented test password 123456, stored as PBKDF2.
    -- Real plaintext passwords are upgraded individually after their next successful login.
    UPDATE users SET password_hash = 'pbkdf2_sha256$210000$PgqIWBb67UpNF4kUYejeRg==$/ErxuXKzQRbPvGdSMI3b+7Sj2EkttMgF50GBOVX1HlM=' WHERE password_hash = 'demo_hash';
END$$
DELIMITER ;
CALL migrate_bf10_account_validation();
DROP PROCEDURE migrate_bf10_account_validation;
