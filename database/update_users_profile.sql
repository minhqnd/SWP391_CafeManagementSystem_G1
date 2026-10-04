-- Thêm cột birthday, address cho bảng users (dùng cho trang Profile và câu query đăng nhập).
-- Chạy file này nếu DB được tạo từ aura_cafe.sql bản cũ (chưa có 2 cột này),
-- nếu không đăng nhập sẽ lỗi "Unknown column 'birthday'".
USE aura_cafe_erp;

ALTER TABLE users
    ADD COLUMN birthday DATE NULL AFTER password_hash,
    ADD COLUMN address TEXT NULL AFTER birthday;
