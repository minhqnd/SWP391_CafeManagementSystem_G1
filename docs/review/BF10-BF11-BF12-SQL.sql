-- TRA CUU SQL KHI REVIEW BF10 / BF11 / BF12
-- Mở đúng database của web trong MySQL Workbench rồi chọn đoạn cần đối chiếu để chạy.
-- Mỗi mã chỉ xuất hiện một lần trong file này và một lần tại chỗ xử lý trong source.
-- Tên biến @... là giá trị đang demo; sửa phần THAM SO bên dưới trước khi chạy.
-- ID sản phẩm/danh mục có trên hàng đang chọn ở web. Không dùng tên thay cho ID.
-- Với câu có dấu ?, bản chạy thử bên dưới đã thay bằng biến hoặc giới hạn cụ thể.
-- INSERT/UPDATE/DELETE được giữ trong chú thích để giải thích thao tác của web.
-- SELECT sau phần đó dùng đọc lại kết quả khi bạn vừa thao tác trên web.
-- Các mục tính bằng Java/JavaScript ghi rõ vị trí hàm; chúng không có câu SQL riêng.
-- File này chưa được chạy trên database hiện tại của bạn.

-- THAM SO: email và điện thoại theo tài khoản bạn đã chọn để demo.
SET @email = 'tuynhocthoi@gmail.com';
SET @phone = '0965999999';
SET @identifier = @email;
SET @user_id = (SELECT id FROM users WHERE email=@email LIMIT 1);
SET @customer_id = @user_id;
-- Điền ID/tên đang demo. NULL nghĩa là chưa chọn đối tượng, nên câu đọc theo ID sẽ chưa có dòng.
SET @category_id = NULL;
SET @product_id = NULL;
SET @ingredient_id = NULL;
SET @category_name = '';
SET @product_name = '';
SET @keyword = '';
SET @term = CONCAT('%', TRIM(@keyword), '%');

-- ============================================================================
-- BF10: 18 vị trí SQL hoặc tính số liệu
-- ============================================================================

-- [SQL10-01] Đăng nhập: tìm tài khoản theo email hoặc số điện thoại.
-- Source: src/main/java/dao/UserDAO.java:42 | Hàm: login
-- Thứ tự dấu ?: 1=định danh đăng nhập; 2=cùng định danh đăng nhập.
-- SQL chỉ tìm tài khoản; Java kiểm tra ACTIVE và gọi PasswordHasher.verify để đối chiếu mật khẩu.
SELECT *
FROM users
WHERE (email = @identifier OR phone = @identifier);

-- [SQL10-02] Đăng nhập bằng mật khẩu cũ: thay giá trị cũ bằng chuỗi băm.
-- Source: src/main/java/dao/UserDAO.java:66 | Hàm: login
-- Thứ tự dấu ?: 1=chuỗi băm mới; 2=mã người dùng; 3=giá trị mật khẩu cũ trong database.
-- Chỉ chạy sau khi Java đối chiếu mật khẩu thành công; không lấy mật khẩu nhập để so trực tiếp bằng SQL.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
UPDATE users SET password_hash=?
WHERE id=? AND password_hash=? AND status='ACTIVE';
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, email, status, CHAR_LENGTH(password_hash) AS stored_password_length FROM users WHERE id=@user_id;

-- [SQL10-03] Tạo tài khoản: thêm tên, liên hệ, vai trò, vị trí và mật khẩu đã băm.
-- Source: src/main/java/dao/UserDAO.java:123 | Hàm: register
-- Thứ tự dấu ?: 1=họ tên; 2=số điện thoại; 3=email; 4=vai trò; 5=vị trí, có thể NULL; 6=chuỗi băm mật khẩu.
-- Trạng thái ACTIVE được ghi cố định; chuỗi băm do PasswordHasher.hash tạo, không phải mật khẩu gõ trên web.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
INSERT INTO users (full_name, phone, email, role, position, status, password_hash) VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?);
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, full_name, phone, email, role, position, status FROM users WHERE email=@email;

-- [SQL10-04] Ô Email đăng ký: tìm email đã được dùng.
-- Source: src/main/java/dao/UserDAO.java:164 | Hàm: checkEmailExists
-- Thứ tự dấu ?: 1=email đã làm gọn và chuyển chữ thường.
SELECT id
FROM users
WHERE email = @email;

-- [SQL10-05] Ô Số điện thoại đăng ký: tìm số đã được dùng.
-- Source: src/main/java/dao/UserDAO.java:189 | Hàm: checkPhoneExists
-- Thứ tự dấu ?: 1=số điện thoại đã làm gọn.
SELECT id
FROM users
WHERE phone = @phone;

-- [SQL10-06] Kiểm tra liên hệ ngay trên form: tìm trùng và bỏ qua tài khoản đang sửa.
-- Source: src/main/java/dao/UserDAO.java:215 | Hàm: contactExists
-- Thứ tự dấu ?: 1=liên hệ cần kiểm tra; 2=mã tài khoản bỏ qua; đăng ký dùng 0.
-- Tên cột do Java chọn: email dùng LOWER(TRIM(email)); điện thoại dùng phone.
-- Bản đầu kiểm tra email; bản sau kiểm tra số điện thoại. Java chọn đúng cột theo ô nhập.
SELECT id
FROM users
WHERE LOWER(TRIM(email))=@email AND id<>@user_id
LIMIT 1;
-- Khi ô cần kiểm tra là số điện thoại:
SELECT id FROM users WHERE phone=@phone AND id<>@user_id LIMIT 1;

-- [SQL10-07] Quên mật khẩu: tìm tài khoản của email trước khi gửi OTP.
-- Source: src/main/java/dao/UserDAO.java:235 | Hàm: findActiveAccountByEmail
-- Thứ tự dấu ?: 1=email đã làm gọn và chuyển chữ thường.
-- Câu SQL lấy tối đa 2 dòng; Java từ chối email trùng nhiều tài khoản hoặc tài khoản không ACTIVE.
SELECT id,email,status
FROM users
WHERE LOWER(TRIM(email))=@email
LIMIT 2;

-- [SQL10-08] Mở hồ sơ: đọc thông tin và số điểm hiện tại của người đang đăng nhập.
-- Source: src/main/java/dao/UserDAO.java:270 | Hàm: getActiveUserById
-- Thứ tự dấu ?: 1=mã người dùng trong phiên đăng nhập.
SELECT id,full_name,phone,email,role,position,status,birthday,address,total_points
FROM users
WHERE id=@user_id AND status='ACTIVE';

-- [SQL10-09] Đặt lại mật khẩu sau OTP: ghi mật khẩu mới cho đúng tài khoản.
-- Source: src/main/java/dao/UserDAO.java:292 | Hàm: resetPasswordById
-- Thứ tự dấu ?: 1=chuỗi băm mật khẩu mới; 2=mã tài khoản đã xác minh; 3=email đã xác minh.
-- Java phải xác minh OTP trước khi gọi hàm này; bảng users không lưu mã OTP.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
UPDATE users SET password_hash=?
WHERE id=? AND LOWER(TRIM(email))=? AND status='ACTIVE';
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, email, status, CHAR_LENGTH(password_hash) AS stored_password_length FROM users WHERE id=@user_id;

-- [SQL10-10] Đổi mật khẩu: đọc mật khẩu đang lưu của tài khoản ACTIVE.
-- Source: src/main/java/dao/UserDAO.java:313 | Hàm: changePasswordById
-- Thứ tự dấu ?: 1=mã người dùng.
-- FOR UPDATE giữ dòng trong lần đổi mật khẩu; Java dùng PasswordHasher.verify để so mật khẩu hiện tại.
-- Câu trong code có FOR UPDATE để giữ dòng trong lần lưu. Bản đọc thử bỏ phần khóa dòng này.
SELECT password_hash
FROM users
WHERE id=@user_id AND status='ACTIVE';

-- [SQL10-11] Đổi mật khẩu: ghi chuỗi băm mới sau khi mật khẩu hiện tại đã đúng.
-- Source: src/main/java/dao/UserDAO.java:323 | Hàm: changePasswordById
-- Thứ tự dấu ?: 1=chuỗi băm mật khẩu mới; 2=mã người dùng.
-- Đọc và ghi nằm trong cùng lần lưu; chỉ xác nhận khi cập nhật đúng một tài khoản.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
UPDATE users SET password_hash=?
WHERE id=? AND status='ACTIVE';
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, email, status, CHAR_LENGTH(password_hash) AS stored_password_length FROM users WHERE id=@user_id;

-- [SQL10-12] Lưu thay đổi hồ sơ: cập nhật các trường của đúng tài khoản ACTIVE.
-- Source: src/main/java/dao/UserDAO.java:350 | Hàm: updateUserProfile
-- Thứ tự dấu ?: 1=họ tên; 2=số điện thoại; 3=email; 4=ngày sinh hoặc NULL; 5=địa chỉ; 6=mã người dùng.
-- Java kiểm tra định dạng và tra trùng liên hệ trước khi thực hiện lệnh cập nhật này.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
UPDATE users SET full_name = ?, phone = ?, email = ?, birthday = ?, address = ?
WHERE id = ? AND status='ACTIVE';
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, full_name, phone, email, role, status, birthday, address, total_points FROM users WHERE id=@user_id;

-- [SQL10-13] Lưu hồ sơ: kiểm tra email có thuộc người khác không.
-- Source: src/main/java/dao/UserDAO.java:361 | Hàm: updateUserProfile
-- Thứ tự dấu ?: 1=email đã chuẩn hóa; 2=mã người dùng hiện tại.
SELECT id
FROM users
WHERE LOWER(TRIM(email))=@email AND id<>@user_id
LIMIT 1;

-- [SQL10-14] Lưu hồ sơ: kiểm tra số điện thoại có thuộc người khác không.
-- Source: src/main/java/dao/UserDAO.java:367 | Hàm: updateUserProfile
-- Thứ tự dấu ?: 1=số điện thoại; 2=mã người dùng hiện tại.
SELECT id
FROM users
WHERE phone=@phone AND id<>@user_id
LIMIT 1;

-- [SQL10-15] Đọc riêng số điểm của tài khoản khi cần làm mới thông tin đăng nhập.
-- Source: src/main/java/dao/UserDAO.java:403 | Hàm: getTotalPoints
-- Thứ tự dấu ?: 1=mã người dùng.
-- Trang hồ sơ còn nhận total_points từ getActiveUserById; không phải mọi lần mở hồ sơ đều gọi hàm này.
SELECT total_points
FROM users
WHERE id = @user_id;

-- [SQL10-16] Tab Lịch sử tích điểm trong hồ sơ: đọc giao dịch của người đang đăng nhập.
-- Source: src/main/java/dao/UserDAO.java:433 | Hàm: getLoyaltyHistoryByUserId
-- Thứ tự dấu ?: 1=mã người dùng.
-- Sắp xếp theo thời gian mới trước; truy vấn này không tự tính số dư sau mỗi giao dịch.
SELECT id, customer_id, order_id, points_change, transaction_type, created_at
FROM loyalty_transactions
WHERE customer_id = @user_id
ORDER BY created_at DESC;

-- [SQL10-17] Nút Xác minh OTP: Java đối chiếu mã và trạng thái khôi phục giữ trong phiên đăng nhập.
-- Source: src/main/java/service/PasswordRecoveryService.java:129 | Hàm: verify
-- Không có câu SELECT OTP trong MySQL; hạn dùng và số lần nhập sai cũng được kiểm tra trong dịch vụ này.
-- Đọc đoạn tính ngay dưới mã trong source. Không có câu SQL riêng cho thao tác/con số này.

-- [SQL10-18] Nút Đăng xuất: hủy phiên đăng nhập rồi chuyển về Trang chủ.
-- Source: src/main/java/controller/LoginController.java:38 | Hàm: doGet
-- Không DELETE tài khoản trong users; thao tác này không có câu SQL riêng.
-- Đọc đoạn tính ngay dưới mã trong source. Không có câu SQL riêng cho thao tác/con số này.

-- ============================================================================
-- BF11: 49 vị trí SQL hoặc tính số liệu
-- ============================================================================

-- [SQL11-01] Bảng danh mục và lựa chọn danh mục ở quản lý sản phẩm: đọc nhóm, trạng thái, ảnh và số món.
-- Source: src/main/java/dao/CategoryDAO.java:26 | Hàm: getAllCategoriesWithCount
-- COUNT(p.id) đếm sản phẩm của từng nhóm, gồm cả món tạm ngừng; LEFT JOIN giữ cả nhóm chưa có món.
SELECT c.id, c.name, c.status, c.image_url, (ci.category_id IS NOT NULL) AS has_stored_image, COUNT(p.id) AS product_count
FROM categories c
LEFT JOIN products p ON p.category_id = c.id
LEFT JOIN category_images ci ON ci.category_id = c.id
GROUP BY c.id, c.name, c.status, c.image_url, ci.category_id
ORDER BY c.id ASC;

-- [SQL11-02] Thẻ Tổng sản phẩm trên trang danh mục: đếm toàn bộ dòng products.
-- Source: src/main/java/dao/CategoryDAO.java:53 | Hàm: getProductCount
SELECT COUNT(*)
FROM products;

-- [SQL11-03] Đếm số sản phẩm thuộc một danh mục.
-- Source: src/main/java/dao/CategoryDAO.java:65 | Hàm: getProductCountByCategory
-- Thứ tự dấu ?: 1=mã danh mục.
SELECT COUNT(*)
FROM products
WHERE category_id = @category_id;

-- [SQL11-04] Nút Xóa danh mục: đếm món đã xuất hiện trong đơn hàng.
-- Source: src/main/java/dao/CategoryDAO.java:86 | Hàm: getOrderedProductCountByCategory
-- Thứ tự dấu ?: 1=mã danh mục.
-- COUNT DISTINCT tránh đếm một món nhiều lần; có kết quả lớn hơn 0 thì controller chặn xóa để giữ lịch sử.
SELECT COUNT(DISTINCT p.id)
FROM products p
JOIN order_items oi ON oi.product_id = p.id
WHERE p.category_id = @category_id;

-- [SQL11-05] Ô Tên danh mục: kiểm tra trùng tên, bỏ qua danh mục đang sửa.
-- Source: src/main/java/dao/CategoryDAO.java:112 | Hàm: categoryNameExists
-- Thứ tự dấu ?: 1=tên danh mục; 2=mã danh mục đang sửa; thêm mới dùng 0.
-- LOWER/TRIM bỏ khác biệt hoa thường và khoảng trắng đầu/cuối.
SELECT 1
FROM categories
WHERE LOWER(TRIM(name)) = LOWER(TRIM(@category_name)) AND id <> COALESCE(@category_id,0)
LIMIT 1;

-- [SQL11-06] Nút Lưu danh mục: cập nhật tên và trạng thái.
-- Source: src/main/java/dao/CategoryDAO.java:178 | Hàm: saveCategory
-- Thứ tự dấu ?: 1=tên danh mục; 2=ACTIVE hoặc INACTIVE; 3=mã danh mục.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
UPDATE categories SET name = ?, status = ?
WHERE id = ?;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, name, status, image_url FROM categories WHERE id=@category_id;

-- [SQL11-07] Lưu danh mục không đổi giá trị: kiểm tra danh mục vẫn tồn tại.
-- Source: src/main/java/dao/CategoryDAO.java:186 | Hàm: saveCategory
-- Thứ tự dấu ?: 1=mã danh mục.
-- Được dùng khi UPDATE báo không đổi dòng nào; có dòng vẫn được xem là lưu đúng danh mục.
SELECT 1
FROM categories
WHERE id = @category_id;

-- [SQL11-08] Nút Tạo danh mục: thêm nhóm mới và lấy mã vừa tạo.
-- Source: src/main/java/dao/CategoryDAO.java:200 | Hàm: saveCategory
-- Thứ tự dấu ?: 1=tên danh mục; 2=ACTIVE hoặc INACTIVE.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
INSERT INTO categories (name, status) VALUES (?, ?);
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, name, status FROM categories WHERE name=@category_name;

-- [SQL11-09] Ảnh danh mục: thêm ảnh mới hoặc thay ảnh của nhóm đã có.
-- Source: src/main/java/dao/CategoryDAO.java:220 | Hàm: saveImage
-- Thứ tự dấu ?: 1=mã danh mục; 2=dữ liệu nhị phân của ảnh; 3=loại ảnh; 4=tên tệp.
-- Tham số ảnh là dữ liệu tệp do Java đọc; khi kiểm chứng trên MySQL nên SELECT thông tin ảnh thay vì dán dữ liệu nhị phân.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
INSERT INTO category_images (category_id, image_data, content_type, file_name) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE image_data = VALUES(image_data), content_type = VALUES(content_type), file_name = VALUES(file_name), updated_at = CURRENT_TIMESTAMP;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT category_id, content_type, file_name, OCTET_LENGTH(image_data) AS image_bytes FROM category_images WHERE category_id=@category_id;

-- [SQL11-10] Ô Xóa ảnh hiện tại của danh mục: xóa ảnh đang lưu.
-- Source: src/main/java/dao/CategoryDAO.java:237 | Hàm: deleteImage
-- Thứ tự dấu ?: 1=mã danh mục.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
DELETE
FROM category_images
WHERE category_id = ?;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT category_id, content_type, file_name, OCTET_LENGTH(image_data) AS image_bytes FROM category_images WHERE category_id=@category_id;

-- [SQL11-11] Thay hoặc xóa ảnh danh mục: bỏ đường dẫn ảnh cũ.
-- Source: src/main/java/dao/CategoryDAO.java:246 | Hàm: clearLegacyImageUrl
-- Thứ tự dấu ?: 1=mã danh mục.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
UPDATE categories SET image_url = NULL
WHERE id = ?;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, name, status, image_url FROM categories WHERE id=@category_id;

-- [SQL11-12] Nút Ẩn/Hiện danh mục: đổi trạng thái của đúng nhóm.
-- Source: src/main/java/dao/CategoryDAO.java:262 | Hàm: updateCategoryStatus
-- Thứ tự dấu ?: 1=ACTIVE hoặc INACTIVE; 2=mã danh mục.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
UPDATE categories SET status = ?
WHERE id = ?;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, name, status, image_url FROM categories WHERE id=@category_id;

-- [SQL11-13] Xóa danh mục: xóa các sản phẩm thuộc nhóm trước.
-- Source: src/main/java/dao/CategoryDAO.java:284 | Hàm: deleteCategoryAndProducts
-- Thứ tự dấu ?: 1=mã danh mục.
-- Controller đã kiểm tra lịch sử đơn hàng; lệnh này và lệnh xóa danh mục phải cùng thành công mới xác nhận.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
DELETE
FROM products
WHERE category_id = ?;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, category_id, name FROM products WHERE category_id=@category_id;

-- [SQL11-14] Xóa danh mục: xóa nhóm sau khi xóa được các sản phẩm.
-- Source: src/main/java/dao/CategoryDAO.java:289 | Hàm: deleteCategoryAndProducts
-- Thứ tự dấu ?: 1=mã danh mục.
-- Lỗi thì hủy cả lần xóa; controller kiểm tra điều kiện trước khi gọi hàm này.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
DELETE
FROM categories
WHERE id = ?;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, name, status, image_url FROM categories WHERE id=@category_id;

-- [SQL11-15] Trang chủ/Menu: đọc danh mục ACTIVE và số món đang bán của từng nhóm.
-- Source: src/main/java/dao/CategoryDAO.java:321 | Hàm: getAllActiveCategoriesWithCount
-- Điều kiện món đang bán được đặt ở JOIN; nhóm ACTIVE chưa có món vẫn có thể được trả về.
SELECT c.id, c.name, c.status, c.image_url, (ci.category_id IS NOT NULL) AS has_stored_image, COUNT(p.id) AS product_count
FROM categories c
LEFT JOIN products p ON c.id = p.category_id AND p.is_available = 1
LEFT JOIN category_images ci ON ci.category_id = c.id
WHERE c.status = 'ACTIVE'
GROUP BY c.id, c.name, c.status, c.image_url, ci.category_id
ORDER BY c.id ASC;

-- [SQL11-16] Nút Sửa danh mục hoặc chọn danh mục cho món: đọc đúng một nhóm.
-- Source: src/main/java/dao/CategoryDAO.java:353 | Hàm: getCategoryById
-- Thứ tự dấu ?: 1=mã danh mục.
SELECT c.id, c.name, c.status, c.image_url, (ci.category_id IS NOT NULL) AS has_stored_image
FROM categories c
LEFT JOIN category_images ci ON ci.category_id = c.id
WHERE c.id = @category_id;

-- [SQL11-17] Ảnh danh mục trên web: đọc dữ liệu ảnh và loại ảnh.
-- Source: src/main/java/dao/CategoryDAO.java:375 | Hàm: getCategoryImage
-- Thứ tự dấu ?: 1=mã danh mục.
SELECT image_data, content_type
FROM category_images
WHERE category_id = @category_id;

-- [SQL11-18] Bảng sản phẩm và bộ lọc quản trị: ghép câu đọc theo danh mục, từ khóa.
-- Source: src/main/java/dao/ProductDAO.java:30 | Hàm: getProductsForAdmin
-- Có chọn nhóm thì thêm category_id; có từ khóa thì thêm 4 dấu ? LIKE dùng cùng chuỗi %từ khóa%; cuối cùng ORDER BY p.id DESC.
-- Câu chạy thử đầu là trường hợp chưa lọc; xem thêm các trường hợp ngay bên dưới.
SELECT p.*, c.name AS category_name, EXISTS(SELECT 1
FROM product_images pi
WHERE pi.product_id=p.id) AS has_stored_image
FROM products p
LEFT JOIN categories c ON c.id=p.category_id
WHERE 1=1
ORDER BY p.id DESC;
-- Chỉ lọc danh mục:
SELECT p.*, c.name AS category_name, EXISTS(SELECT 1
FROM product_images pi
WHERE pi.product_id=p.id) AS has_stored_image
FROM products p
LEFT JOIN categories c ON c.id=p.category_id
WHERE 1=1 AND p.category_id=@category_id
ORDER BY p.id DESC;
-- Chỉ lọc từ khóa:
SELECT p.*, c.name AS category_name, EXISTS(SELECT 1
FROM product_images pi
WHERE pi.product_id=p.id) AS has_stored_image
FROM products p
LEFT JOIN categories c ON c.id=p.category_id
WHERE 1=1 AND (p.name LIKE @term OR CAST(p.id AS CHAR) LIKE @term OR COALESCE(p.description,'') LIKE @term OR COALESCE(c.name,'') LIKE @term)
ORDER BY p.id DESC;
-- Lọc cả danh mục và từ khóa:
SELECT p.*, c.name AS category_name, EXISTS(SELECT 1
FROM product_images pi
WHERE pi.product_id=p.id) AS has_stored_image
FROM products p
LEFT JOIN categories c ON c.id=p.category_id
WHERE 1=1 AND p.category_id=@category_id AND (p.name LIKE @term OR CAST(p.id AS CHAR) LIKE @term OR COALESCE(p.description,'') LIKE @term OR COALESCE(c.name,'') LIKE @term)
ORDER BY p.id DESC;

-- [SQL11-19] Ba thẻ sản phẩm: SQL trả tổng món và số đang bán.
-- Source: src/main/java/dao/ProductDAO.java:68 | Hàm: getProductAdminStats
-- Số tạm ngừng được controller lấy tổng trừ số đang bán; thống kê này không phụ thuộc bộ lọc của bảng.
SELECT COUNT(*) AS total, COALESCE(SUM(is_available = 1), 0) AS available
FROM products;

-- [SQL11-20] Nút Sửa sản phẩm/chọn món làm công thức: đọc món theo mã, kể cả món tạm ngừng.
-- Source: src/main/java/dao/ProductDAO.java:87 | Hàm: getProductForAdmin
-- Thứ tự dấu ?: 1=mã sản phẩm.
SELECT p.*, c.name AS category_name, EXISTS(SELECT 1
FROM product_images pi
WHERE pi.product_id=p.id) AS has_stored_image
FROM products p
LEFT JOIN categories c ON c.id = p.category_id
WHERE p.id = @product_id;

-- [SQL11-21] Ô Tên sản phẩm: tìm tên trùng, bỏ qua món đang sửa.
-- Source: src/main/java/dao/ProductDAO.java:106 | Hàm: productNameExists
-- Thứ tự dấu ?: 1=tên sản phẩm; 2=mã sản phẩm đang sửa; thêm mới dùng 0.
SELECT 1
FROM products
WHERE LOWER(TRIM(name)) = LOWER(TRIM(@product_name)) AND id <> COALESCE(@product_id,0)
LIMIT 1;

-- [SQL11-22] Nút Lưu sản phẩm: cập nhật món hiện có.
-- Source: src/main/java/dao/ProductDAO.java:169 | Hàm: saveProduct
-- Thứ tự dấu ?: 1=mã danh mục; 2=tên món; 3=giá cơ bản; 4=chuỗi JSON size/topping; 5=mô tả; 6=1 đang bán hoặc 0 tạm ngừng; 7=mã sản phẩm.
-- Size và topping được ghép vào variants_config; không có bảng size/topping riêng trong thao tác này.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
UPDATE products SET category_id = ?, name = ?, base_price = ?, variants_config = ?, description = ?, is_available = ?
WHERE id = ?;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, category_id, name, base_price, variants_config, description, is_available, image_url FROM products WHERE id=@product_id;

-- [SQL11-23] Lưu sản phẩm không đổi giá trị: kiểm tra món vẫn tồn tại.
-- Source: src/main/java/dao/ProductDAO.java:179 | Hàm: saveProduct
-- Thứ tự dấu ?: 1=mã sản phẩm.
SELECT 1
FROM products
WHERE id = @product_id;

-- [SQL11-24] Nút Thêm sản phẩm: tạo món mới và lấy mã vừa tạo.
-- Source: src/main/java/dao/ProductDAO.java:190 | Hàm: saveProduct
-- Thứ tự dấu ?: 1=mã danh mục; 2=tên món; 3=giá cơ bản; 4=chuỗi JSON size/topping; 5=mô tả; 6=1 đang bán hoặc 0 tạm ngừng.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
INSERT INTO products (category_id, name, base_price, variants_config, description, is_available) VALUES (?, ?, ?, ?, ?, ?);
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, category_id, name, base_price, variants_config, description, is_available FROM products WHERE name=@product_name;

-- [SQL11-25] Ảnh sản phẩm: thêm hoặc thay ảnh lưu trong database.
-- Source: src/main/java/dao/ProductDAO.java:215 | Hàm: saveImage
-- Thứ tự dấu ?: 1=mã sản phẩm; 2=dữ liệu nhị phân của ảnh; 3=loại ảnh; 4=tên tệp.
-- Java đọc tệp đã kiểm tra; trên MySQL có thể SELECT tên tệp, loại ảnh và số byte để đối chiếu.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
INSERT INTO product_images (product_id, image_data, content_type, file_name) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE image_data=VALUES(image_data), content_type=VALUES(content_type), file_name=VALUES(file_name), updated_at=CURRENT_TIMESTAMP;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT product_id, content_type, file_name, OCTET_LENGTH(image_data) AS image_bytes FROM product_images WHERE product_id=@product_id;

-- [SQL11-26] Ô Xóa ảnh hiện tại của sản phẩm: xóa ảnh đang lưu.
-- Source: src/main/java/dao/ProductDAO.java:231 | Hàm: deleteImage
-- Thứ tự dấu ?: 1=mã sản phẩm.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
DELETE
FROM product_images
WHERE product_id = ?;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT product_id, content_type, file_name, OCTET_LENGTH(image_data) AS image_bytes FROM product_images WHERE product_id=@product_id;

-- [SQL11-27] Thay hoặc xóa ảnh sản phẩm: bỏ đường dẫn ảnh cũ.
-- Source: src/main/java/dao/ProductDAO.java:240 | Hàm: clearLegacyImageUrl
-- Thứ tự dấu ?: 1=mã sản phẩm.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
UPDATE products SET image_url = NULL
WHERE id = ?;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, category_id, name, base_price, variants_config, description, is_available, image_url FROM products WHERE id=@product_id;

-- [SQL11-28] Nút Đang bán/Tạm ngừng: đổi trạng thái bán của món.
-- Source: src/main/java/dao/ProductDAO.java:257 | Hàm: updateProductAvailability
-- Thứ tự dấu ?: 1=1 đang bán hoặc 0 tạm ngừng; 2=mã sản phẩm.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
UPDATE products SET is_available = ?
WHERE id = ?;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, category_id, name, base_price, variants_config, description, is_available, image_url FROM products WHERE id=@product_id;

-- [SQL11-29] Nút Xóa sản phẩm: chỉ xóa món tạm ngừng và chưa có trong order_items.
-- Source: src/main/java/dao/ProductDAO.java:273 | Hàm: deleteProduct
-- Thứ tự dấu ?: 1=mã sản phẩm cần xóa; 2=cùng mã sản phẩm để kiểm tra lịch sử.
-- NOT EXISTS là điều kiện chặn xóa món đã có trong đơn; không xóa lịch sử đơn hàng.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
DELETE
FROM products
WHERE id = ? AND is_available = 0 AND NOT EXISTS (SELECT 1
FROM order_items
WHERE product_id = ?);
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, category_id, name, base_price, variants_config, description, is_available, image_url FROM products WHERE id=@product_id;
SELECT id, order_id, product_id FROM order_items WHERE product_id=@product_id;

-- [SQL11-30] Ảnh sản phẩm trên web: đọc ảnh đang lưu theo mã món.
-- Source: src/main/java/dao/ProductDAO.java:318 | Hàm: getProductImage
-- Thứ tự dấu ?: 1=mã sản phẩm.
SELECT image_data, content_type
FROM product_images
WHERE product_id = @product_id;

-- [SQL11-31] Tìm/lọc trên Menu: chỉ đọc món đang bán trong danh mục ACTIVE.
-- Source: src/main/java/dao/ProductDAO.java:440 | Hàm: filterProducts
-- Có nhóm thì thêm category_id; có từ khóa thì thêm 3 dấu ? LIKE cho tên món, nhóm, mô tả cùng dùng %từ khóa%; ORDER BY p.id ASC.
-- Câu chạy thử đầu là trường hợp chưa lọc; xem thêm các trường hợp ngay bên dưới.
SELECT p.*, c.name AS category_name, EXISTS(SELECT 1
FROM product_images pi
WHERE pi.product_id=p.id) AS has_stored_image
FROM products p
JOIN categories c ON p.category_id=c.id
WHERE p.is_available=1 AND c.status='ACTIVE'
ORDER BY p.id ASC;
-- Chỉ lọc danh mục:
SELECT p.*, c.name AS category_name, EXISTS(SELECT 1
FROM product_images pi
WHERE pi.product_id=p.id) AS has_stored_image
FROM products p
JOIN categories c ON p.category_id=c.id
WHERE p.is_available=1 AND c.status='ACTIVE' AND p.category_id=@category_id
ORDER BY p.id ASC;
-- Chỉ lọc từ khóa:
SELECT p.*, c.name AS category_name, EXISTS(SELECT 1
FROM product_images pi
WHERE pi.product_id=p.id) AS has_stored_image
FROM products p
JOIN categories c ON p.category_id=c.id
WHERE p.is_available=1 AND c.status='ACTIVE' AND (p.name LIKE @term OR c.name LIKE @term OR p.description LIKE @term)
ORDER BY p.id ASC;
-- Lọc cả danh mục và từ khóa:
SELECT p.*, c.name AS category_name, EXISTS(SELECT 1
FROM product_images pi
WHERE pi.product_id=p.id) AS has_stored_image
FROM products p
JOIN categories c ON p.category_id=c.id
WHERE p.is_available=1 AND c.status='ACTIVE' AND p.category_id=@category_id AND (p.name LIKE @term OR c.name LIKE @term OR p.description LIKE @term)
ORDER BY p.id ASC;

-- [SQL11-32] Mở chi tiết món trên Menu: chỉ trả món đang bán thuộc nhóm ACTIVE.
-- Source: src/main/java/dao/ProductDAO.java:514 | Hàm: getAvailableProductById
-- Thứ tự dấu ?: 1=mã sản phẩm.
SELECT p.*, c.name AS category_name, EXISTS(SELECT 1
FROM product_images pi
WHERE pi.product_id=p.id) AS has_stored_image
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE p.id = @product_id AND p.is_available = 1 AND c.status = 'ACTIVE';

-- [SQL11-33] Con số tổng món ở Menu: đếm products có is_available=1.
-- Source: src/main/java/dao/ProductDAO.java:530 | Hàm: getTotalAvailableCount
-- Câu đếm hiện tại không lọc trạng thái danh mục; danh sách món và số kết quả sau lọc được lấy riêng.
SELECT COUNT(*)
FROM products
WHERE is_available = 1;

-- [SQL11-34] Phần Món liên quan: chọn món cùng nhóm, bỏ món đang xem.
-- Source: src/main/java/dao/ProductDAO.java:550 | Hàm: getRelatedProducts
-- Thứ tự dấu ?: 1=mã danh mục; 2=mã món đang xem; 3=giới hạn, trang chi tiết truyền 4.
-- ORDER BY RAND chọn ngẫu nhiên nên các món được gợi ý có thể thay đổi mỗi lần mở.
SELECT p.*, c.name AS category_name, EXISTS(SELECT 1
FROM product_images pi
WHERE pi.product_id=p.id) AS has_stored_image
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE p.is_available = 1 AND c.status = 'ACTIVE' AND p.category_id = @category_id AND p.id != @product_id
ORDER BY RAND()
LIMIT 4;

-- [SQL11-35] Banner/Sản phẩm yêu thích: xếp món theo tổng số lượng bán.
-- Source: src/main/java/dao/ProductDAO.java:587 | Hàm: getBestSellingProducts
-- Thứ tự dấu ?: 1=giới hạn, Trang chủ truyền 4.
-- Chỉ cộng đơn CONFIRMED, PREPARING, READY, DELIVERING, COMPLETED; loại món ngừng bán và danh mục không ACTIVE.
SELECT p.*, c.name AS category_name, EXISTS(SELECT 1
FROM product_images pi
WHERE pi.product_id=p.id) AS has_stored_image, COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN oi.quantity ELSE 0 END), 0) as total_sold
FROM products p
JOIN categories c ON p.category_id = c.id
LEFT JOIN order_items oi ON p.id = oi.product_id
LEFT JOIN orders o ON oi.order_id = o.id AND o.status IN ('CONFIRMED', 'PREPARING', 'READY', 'DELIVERING', 'COMPLETED')
WHERE p.is_available = 1 AND c.status = 'ACTIVE'
GROUP BY p.id
ORDER BY total_sold DESC, p.id ASC
LIMIT 4;

-- [SQL11-36] Nút Lưu công thức: xóa các dòng công thức cũ của món.
-- Source: src/main/java/dao/ProductIngredientDAO.java:18 | Hàm: saveRecipe
-- Thứ tự dấu ?: 1=mã sản phẩm.
-- Chưa xác nhận riêng bước xóa; phải thêm đủ công thức mới mới commit, lỗi thì rollback để giữ công thức cũ.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
DELETE
FROM product_ingredients
WHERE product_id = ?;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT pi.product_id, p.name AS product_name, pi.ingredient_id, i.name AS ingredient_name, i.unit, pi.quantity_required FROM product_ingredients pi JOIN products p ON p.id=pi.product_id JOIN ingredients i ON i.id=pi.ingredient_id WHERE pi.product_id=@product_id ORDER BY i.name;

-- [SQL11-37] Nút Lưu công thức: thêm từng dòng nguyên liệu và định lượng mới.
-- Source: src/main/java/dao/ProductIngredientDAO.java:23 | Hàm: saveRecipe
-- Thứ tự dấu ?: 1=mã sản phẩm; 2=mã nguyên liệu; 3=định lượng.
-- Java điền lại 3 giá trị cho từng dòng rồi executeBatch; không phải chỉ lưu một nguyên liệu.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_required) VALUES (?, ?, ?);
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT pi.product_id, p.name AS product_name, pi.ingredient_id, i.name AS ingredient_name, i.unit, pi.quantity_required FROM product_ingredients pi JOIN products p ON p.id=pi.product_id JOIN ingredients i ON i.id=pi.ingredient_id WHERE pi.product_id=@product_id ORDER BY i.name;

-- [SQL11-38] Trước khi lưu công thức: đọc các nguyên liệu đã có.
-- Source: src/main/java/dao/ProductIngredientDAO.java:36 | Hàm: saveRecipe
-- Thứ tự dấu ?: 1=mã sản phẩm.
-- DAO dùng danh sách này để nhận biết nguyên liệu mới; controller vẫn kiểm tra dữ liệu được gửi trước khi gọi DAO.
SELECT ingredient_id
FROM product_ingredients
WHERE product_id = @product_id;

-- [SQL11-39] Đọc riêng công thức một món kèm tên và đơn vị nguyên liệu.
-- Source: src/main/java/dao/ProductIngredientDAO.java:94 | Hàm: getIngredientsByProductId
-- Thứ tự dấu ?: 1=mã sản phẩm.
SELECT pi.product_id, pi.ingredient_id, pi.quantity_required, i.name AS ingredient_name, i.unit
FROM product_ingredients pi
JOIN ingredients i ON pi.ingredient_id = i.id
WHERE pi.product_id = @product_id
ORDER BY i.name ASC;

-- [SQL11-40] Trang công thức: đọc các dòng định mức của toàn bộ món.
-- Source: src/main/java/dao/ProductIngredientDAO.java:131 | Hàm: getAllProductIngredientsMap
-- Java nhóm các dòng theo product_id để mở form, đếm món đã có công thức và tính giá vốn.
SELECT pi.product_id, pi.ingredient_id, pi.quantity_required, i.name AS ingredient_name, i.unit
FROM product_ingredients pi
JOIN ingredients i ON pi.ingredient_id = i.id
ORDER BY pi.product_id, i.name ASC;

-- [SQL11-41] Ô Chọn nguyên liệu: đọc mã, tên, đơn vị và trạng thái.
-- Source: src/main/java/dao/IngredientDAO.java:21 | Hàm: getAllIngredients
-- Câu này đọc cả nguyên liệu ngừng hoạt động; controller/JavaScript quyết định cho chọn hay giữ dữ liệu cũ thế nào.
SELECT id, name, unit, is_active
FROM ingredients
ORDER BY name ASC;

-- [SQL11-42] Giá nhập/Cost mỗi phần: lấy đơn giá của lần nhập đã nhận gần nhất cho từng nguyên liệu.
-- Source: src/main/java/dao/IngredientDAO.java:54 | Hàm: getLatestPurchasePrices
-- ROW_NUMBER chia theo nguyên liệu, xếp lần nhận mới nhất rồi lấy row_num=1; giá vốn còn phải nhân định lượng trong Java.
SELECT ingredient_id, unit_price
FROM (  SELECT poi.ingredient_id, poi.unit_price,          ROW_NUMBER() OVER (PARTITION BY poi.ingredient_id
ORDER BY COALESCE(tx.created_at, po.created_at) DESC,                       COALESCE(tx.id, 0) DESC, poi.id DESC) AS row_num
FROM purchase_order_items poi
JOIN purchase_orders po ON po.id = poi.po_id AND po.status IN ('PARTIALLY_RECEIVED','RECEIVED','CLOSED')
LEFT JOIN inventory_transactions tx     ON tx.reference_type = 'PURCHASE_ORDER' AND tx.reference_id = po.id    AND tx.ingredient_id = poi.ingredient_id AND tx.transaction_type = 'IN_PURCHASE'
WHERE tx.id IS NOT NULL OR po.status = 'RECEIVED') ranked
WHERE row_num = 1;

-- [SQL11-43] Trước khi thêm nguyên liệu mới vào công thức: đọc và kiểm tra trạng thái.
-- Source: src/main/java/dao/IngredientDAO.java:269 | Hàm: requireActive
-- Thứ tự dấu ?: 1=mã nguyên liệu.
-- FOR UPDATE giữ dòng trong lần lưu công thức; không tìm thấy hoặc ngừng hoạt động thì báo lỗi.
-- Câu trong code có FOR UPDATE để giữ dòng trong lần lưu. Bản đọc thử bỏ phần khóa dòng này.
SELECT is_active
FROM ingredients
WHERE id = @ingredient_id;

-- [SQL11-44] Giá vốn công thức đang mở: Java cộng giá nhập mới nhất nhân định lượng từng dòng.
-- Source: src/main/java/controller/AdminRecipeController.java:115 | Hàm: doGet
-- Nguồn là getAllProductIngredientsMap và getLatestPurchasePrices; thiếu giá nhập thì đánh dấu chưa đủ dữ liệu.
-- Đọc đoạn tính ngay dưới mã trong source. Không có câu SQL riêng cho thao tác/con số này.

-- [SQL11-45] Thẻ Có/Chưa có định mức: Java đếm món có danh sách nguyên liệu rồi lấy tổng món trừ số đã có.
-- Source: src/main/java/controller/AdminRecipeController.java:143 | Hàm: doGet
-- Không có câu COUNT riêng trong SQL cho hai thẻ này; nguồn là danh sách sản phẩm và bản nhóm công thức.
-- Đọc đoạn tính ngay dưới mã trong source. Không có câu SQL riêng cho thao tác/con số này.

-- [SQL11-46] Cột Giá vốn trên bảng món: Java tính riêng tổng giá nhập nhân định lượng của từng sản phẩm.
-- Source: src/main/java/controller/AdminRecipeController.java:161 | Hàm: doGet
-- Không lấy giá bán làm giá vốn; món thiếu giá nhập được đánh dấu thay vì kết luận chi phí bằng 0.
-- Đọc đoạn tính ngay dưới mã trong source. Không có câu SQL riêng cho thao tác/con số này.

-- [SQL11-47] Thẻ Tổng/Hoạt động/Tạm ẩn danh mục: Java đếm từ danh sách categories đã đọc.
-- Source: src/main/java/controller/AdminCategoriesController.java:63 | Hàm: doGet
-- Tổng là categories.size; hoạt động và tạm ẩn đếm theo status. Nguồn SQL là getAllCategoriesWithCount.
-- Đọc đoạn tính ngay dưới mã trong source. Không có câu SQL riêng cho thao tác/con số này.

-- [SQL11-48] Thẻ Tạm ngừng sản phẩm: Java lấy tổng món trừ số đang bán.
-- Source: src/main/java/controller/AdminProductsController.java:109 | Hàm: doGet
-- Hai số được getProductAdminStats trả về; không đếm từ các hàng đang thấy sau khi lọc.
-- Đọc đoạn tính ngay dưới mã trong source. Không có câu SQL riêng cho thao tác/con số này.

-- [SQL11-49] Giá vốn xem trước khi sửa công thức: trình duyệt nhân lượng đang nhập với giá đã tải rồi cộng lại.
-- Source: src/main/webapp/admin-recipes.jsp:416 | Hàm: recalculateCost
-- Chưa bấm Lưu thì chưa thay database; Java vẫn đọc và tính lại giá vốn khi tải trang sau khi lưu.
-- Đọc đoạn tính ngay dưới mã trong source. Không có câu SQL riêng cho thao tác/con số này.

-- ============================================================================
-- BF12: 15 vị trí SQL hoặc tính số liệu
-- ============================================================================

-- [SQL12-01] Bảng lịch sử điểm: ghép khách, giao dịch và mã đơn, mới nhất trước.
-- Source: src/main/java/dao/LoyaltyTransactionDAO.java:47 | Hàm: findDetailed
-- Trang quản trị gọi findRecent(50), không lọc một khách; dấu ? là giới hạn 50 dòng. findByCustomer hỗ trợ đọc riêng một khách nhưng hiện chưa có controller gọi hàm đó.
-- Câu đầu là lịch sử chung đang dùng trên trang quản trị; câu sau là ví dụ lọc một khách để đối chiếu.
SELECT lt.id, lt.customer_id, lt.order_id, lt.points_change, lt.transaction_type, lt.created_at, u.full_name, u.phone, u.total_points, o.order_code
FROM loyalty_transactions lt
JOIN users u ON u.id=lt.customer_id
LEFT JOIN orders o ON o.id=lt.order_id
ORDER BY lt.created_at DESC, lt.id DESC
LIMIT 50;
-- Ví dụ lọc một khách theo findByCustomer (hàm này hiện chưa được controller gọi):
SELECT lt.id, lt.customer_id, lt.order_id, lt.points_change, lt.transaction_type, lt.created_at, u.full_name, u.phone, u.total_points, o.order_code
FROM loyalty_transactions lt
JOIN users u ON u.id=lt.customer_id
LEFT JOIN orders o ON o.id=lt.order_id
WHERE lt.customer_id=@customer_id
ORDER BY lt.created_at DESC, lt.id DESC
LIMIT 100;

-- [SQL12-02] Thẻ Khách hàng có điểm: đếm CUSTOMER có total_points lớn hơn 0.
-- Source: src/main/java/dao/LoyaltyTransactionDAO.java:116 | Hàm: countCustomersWithPoints
SELECT COUNT(*)
FROM users
WHERE role='CUSTOMER' AND total_points > 0;

-- [SQL12-03] Thẻ Tổng điểm lưu hành: cộng số dư hiện tại của các CUSTOMER.
-- Source: src/main/java/dao/LoyaltyTransactionDAO.java:122 | Hàm: getTotalPoints
-- Không cộng lại points_change của lịch sử; COALESCE trả 0 khi không có điểm để cộng.
SELECT COALESCE(SUM(total_points),0)
FROM users
WHERE role='CUSTOMER';

-- [SQL12-04] Thẻ Giao dịch điểm: đếm toàn bộ dòng lịch sử điểm.
-- Source: src/main/java/dao/LoyaltyTransactionDAO.java:129 | Hàm: countTransactions
SELECT COUNT(*)
FROM loyalty_transactions;

-- [SQL12-05] Nút Tìm kiếm theo số điện thoại: đọc thông tin và điểm của CUSTOMER.
-- Source: src/main/java/dao/LoyaltyTransactionDAO.java:150 | Hàm: findCustomerByPhone
-- Thứ tự dấu ?: 1=số điện thoại đã chuẩn hóa.
SELECT id, full_name, phone, email, role, position, status, total_points
FROM users
WHERE role='CUSTOMER' AND phone=@phone
LIMIT 1;

-- [SQL12-06] Khách hiển thị ban đầu: chọn CUSTOMER có điểm cao nhất.
-- Source: src/main/java/dao/LoyaltyTransactionDAO.java:172 | Hàm: findDefaultCustomer
-- Chỉ dùng khi chưa gửi số điện thoại; không dùng để thay thế một số đã tìm nhưng không có khách.
SELECT id, full_name, phone, email, role, position, status, total_points
FROM users
WHERE role='CUSTOMER'
ORDER BY total_points DESC, id
LIMIT 1;

-- [SQL12-07] Thông tin khách: tổng tiền đã mua và ngày đặt đơn gần nhất.
-- Source: src/main/java/dao/LoyaltyTransactionDAO.java:198 | Hàm: getCustomerActivity
-- Thứ tự dấu ?: 1=mã khách hàng.
-- Tiền chỉ cộng đơn COMPLETED; MAX(created_at) lấy đơn gần nhất của mọi trạng thái, không chỉ đơn hoàn thành.
SELECT COALESCE(SUM(CASE WHEN status='COMPLETED' THEN final_amount ELSE 0 END),0) total_spent, MAX(created_at) last_order_at
FROM orders
WHERE customer_id=@customer_id;

-- [SQL12-08] Nút Cộng/Trừ điểm: đọc số dư hiện tại và giữ dòng đang cập nhật.
-- Source: src/main/java/dao/LoyaltyTransactionDAO.java:234 | Hàm: adjustPoints
-- Thứ tự dấu ?: 1=mã khách hàng.
-- Java cộng pointsChange, chặn số dư âm hoặc vượt giới hạn trước khi ghi; FOR UPDATE không tự tính số dư mới.
-- Câu trong code có FOR UPDATE để giữ dòng trong lần lưu. Bản đọc thử bỏ phần khóa dòng này.
SELECT total_points
FROM users
WHERE id=@customer_id AND role='CUSTOMER';

-- [SQL12-09] Nút Cộng/Trừ điểm: ghi số dư mới đã kiểm tra.
-- Source: src/main/java/dao/LoyaltyTransactionDAO.java:239 | Hàm: adjustPoints
-- Thứ tự dấu ?: 1=số dư mới; 2=mã khách hàng.
-- Lệnh cập nhật số dư này và lệnh ghi lịch sử phải cùng thành công mới xác nhận lưu.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
UPDATE users SET total_points=?
WHERE id=?;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, full_name, phone, total_points FROM users WHERE id=@customer_id;
SELECT id, customer_id, order_id, points_change, transaction_type, created_at FROM loyalty_transactions WHERE customer_id=@customer_id ORDER BY created_at DESC, id DESC LIMIT 10;

-- [SQL12-10] Nút Cộng/Trừ điểm: ghi giao dịch điều chỉnh ADJUST.
-- Source: src/main/java/dao/LoyaltyTransactionDAO.java:243 | Hàm: adjustPoints
-- Thứ tự dấu ?: 1=mã khách hàng; 2=điểm thay đổi; cộng dương, trừ âm.
-- order_id là NULL vì điều chỉnh thủ công; ghi số dư và lịch sử cùng thành công mới commit.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
INSERT INTO loyalty_transactions(customer_id,order_id,points_change,transaction_type) VALUES (?,NULL,?,'ADJUST');
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT id, full_name, phone, total_points FROM users WHERE id=@customer_id;
SELECT id, customer_id, order_id, points_change, transaction_type, created_at FROM loyalty_transactions WHERE customer_id=@customer_id ORDER BY created_at DESC, id DESC LIMIT 10;

-- [SQL12-11] Ba ô quy tắc tích/đổi điểm: đọc tên và giá trị đang lưu.
-- Source: src/main/java/dao/ShopSettingDAO.java:91 | Hàm: getSettingsStrict
-- Câu SQL đọc mọi thiết lập; applyValue chọn đúng ba khóa điểm để điền form. Thiếu khóa hoặc giá trị không đổi được sang số giữ mặc định.
SELECT setting_key, setting_value
FROM shop_settings;

-- [SQL12-12] Nút Lưu quy tắc: thêm hoặc cập nhật từng quy tắc trong shop_settings.
-- Source: src/main/java/dao/ShopSettingDAO.java:130 | Hàm: saveLoyaltyRules
-- Thứ tự dấu ?: 1=tên khóa; 2=giá trị mới; 3=lặp lại giá trị mới khi khóa đã có.
-- addValue gọi 3 lần cho tiền tích một điểm, mốc đổi và tiền giảm; executeBatch rồi commit cả bộ.
-- Câu ghi dữ liệu trong code (mẫu có ?; thao tác ghi được thực hiện qua web):
/*
INSERT INTO shop_settings (setting_key, setting_value) VALUES (?, ?) ON DUPLICATE KEY UPDATE setting_value = ?;
*/
-- Đọc lại dữ liệu để đối chiếu sau thao tác trên web:
SELECT setting_key, setting_value FROM shop_settings WHERE setting_key IN ('points_earn_unit','points_step','points_step_value') ORDER BY setting_key;

-- [SQL12-13] Thẻ Giá trị quy đổi: Java lấy tổng điểm chia nguyên cho mốc đổi rồi nhân tiền giảm.
-- Source: src/main/java/controller/AdminLoyaltyController.java:72 | Hàm: doGet
-- Nguồn là getTotalPoints và getSettingsStrict; Math.round làm tròn tiền trước khi hiển thị.
-- Đọc đoạn tính ngay dưới mã trong source. Không có câu SQL riêng cho thao tác/con số này.

-- [SQL12-14] Tiền có thể đổi của một khách: Java dùng số điểm riêng của khách với bộ quy tắc hiện tại.
-- Source: src/main/java/controller/AdminLoyaltyController.java:142 | Hàm: doGet
-- Không lấy giá trị quy đổi của toàn hệ thống để hiển thị cho từng khách.
-- Đọc đoạn tính ngay dưới mã trong source. Không có câu SQL riêng cho thao tác/con số này.

-- [SQL12-15] Cột Số dư sau: Java tính ngược từ số dư hiện tại theo lịch sử mới tới cũ.
-- Source: src/main/java/dao/LoyaltyTransactionDAO.java:75 | Hàm: findDetailed
-- SQL không có cột balance_after; Java giữ số dư riêng từng khách rồi trừ points_change để đi về giao dịch cũ.
-- Đọc đoạn tính ngay dưới mã trong source. Không có câu SQL riêng cho thao tác/con số này.
