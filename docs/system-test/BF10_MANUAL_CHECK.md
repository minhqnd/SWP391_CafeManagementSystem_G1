# Chạy thử BF10 sau khi sửa code

Đợt này chỉ sửa Account / Profile / Password Recovery. BF11 và BF12 chờ bạn kiểm tra BF10 trước. File System Test chưa được đổi sang Passed.

## Chuẩn bị một lần

1. Dừng ứng dụng. Trong MySQL Workbench, chọn database test đang dùng rồi chạy **chỉ** `database/migrations/20261115_bf10_account_validation.sql`. Script giữ dữ liệu, chuẩn hóa email, thêm UNIQUE email và đổi marker mật khẩu của tài khoản demo sang hash cho mật khẩu `123456`. **Không chạy lại toàn bộ `database/aura_cafe.sql` trên DB đang có dữ liệu.**
2. Nếu migration báo email trùng, script dừng trước khi sửa dữ liệu. Kiểm tra bằng câu lệnh dưới đây và xử lý đúng chủ tài khoản; không tự xóa/ghép khách hàng. Sau đó chạy lại migration.

```sql
SELECT LOWER(TRIM(email)) AS normalized_email, GROUP_CONCAT(id) AS user_ids, COUNT(*) AS total
FROM users
WHERE email IS NOT NULL AND TRIM(email) <> ''
GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1;
```

3. Deploy lại `target/SWP391_CafeManagementSystem.war` hoặc chạy lại bằng IDE. Dùng JDK 17 trở lên, Tomcat 10.1 và MySQL 8. Mở trang mới/đăng xuất các phiên cũ để nhận CSRF token mới.
4. OTP cần `AURA_SMTP_EMAIL` và `AURA_SMTP_PASSWORD` trong môi trường của tiến trình Tomcat. Nếu chạy Tomcat từ IDE, đặt biến môi trường trong cấu hình chạy hoặc khởi động IDE từ môi trường đã cấu hình. Không ghi mật khẩu SMTP vào source. Nếu thiếu SMTP, hệ thống báo không gửi được email và không cho reset bằng mã lấy từ màn hình/log.
5. Chuẩn bị một email A truy cập được, một email B khác đã có tài khoản test và một số điện thoại chưa đăng ký. Tài khoản demo trong DB vừa nâng cấp dùng `123456`; không còn dùng chuỗi `demo_hash` hay các mật khẩu dự phòng cũ.

Ví dụ URL: `http://localhost:8080/SWP391_CafeManagementSystem/`. Dùng đúng context URL mà máy bạn deploy.

## Test lần lượt

| Bước | Bạn làm | Kết quả cần thấy |
| --- | --- | --- |
| 1 | Đăng ký QA Customer, email A, SĐT mới, mật khẩu `AuraTest123!`, xác nhận giống nhau. | Tạo tài khoản thành công; đăng nhập bằng email A hoặc SĐT. |
| 2 | Thử đăng ký một account khác dùng lại SĐT, rồi thử lại bằng email A nhưng SĐT khác. | Cả hai lần đều bị chặn; không có tài khoản trùng. |
| 3 | Thử xác nhận mật khẩu khác, mật khẩu 5 ký tự, email `a@b`, tên chỉ có dấu cách. | Form/server báo lỗi, không ghi account. Kiểm tra bỏ field xác nhận bằng request là ca kỹ thuật bổ sung, unit test đã bao phủ. |
| 4 | Mở Profile, Chỉnh sửa, lưu tên và địa chỉ mới; reload. | Dữ liệu được giữ sau reload. Nội dung có dấu/nháy hiển thị như văn bản. |
| 5 | Thử ngày sinh tương lai, ngày không tồn tại hoặc địa chỉ trên 500 ký tự. | Bị chặn; hồ sơ cũ không bị ghi đè. |
| 6 | Thử đổi email mà không nhập mật khẩu hiện tại; sau đó nhập đúng mật khẩu và thử dùng email đã thuộc tài khoản B. | Lần đầu yêu cầu mật khẩu; lần sau báo email trùng. Email A vẫn thuộc đúng tài khoản. |
| 7 | Mở Đổi mật khẩu; thử mật khẩu hiện tại sai, mật khẩu mới ngắn hoặc xác nhận khác. | Không đổi mật khẩu. |
| 8 | Đổi đúng sang `AuraNew456!`, logout rồi thử lần lượt mật khẩu cũ và mới. | Mật khẩu cũ thất bại, mật khẩu mới thành công. |
| 9 | Logout. Quên mật khẩu, nhập email A và bấm **Gửi mã OTP**. | Mailbox nhận mã 6 số. Màn hình và server log không chứa mã. |
| 10 | Bấm gửi lại ngay hoặc gửi lại yêu cầu mới cho A trong 60 giây. | Bị giới hạn. Sau ít nhất 60 giây có thể yêu cầu mã mới. OTP chưa xác minh bị thay khi gửi mã mới thành công. |
| 11 | Nhập sai OTP 5 lần rồi nhập mã đúng cũ. | Phiên bị hủy sau lần sai thứ 5; mã cũ không dùng được. Yêu cầu lại mã sau thời gian chờ. |
| 12 | Yêu cầu một mã khác, chờ đủ 5 phút rồi xác nhận. | Báo hết hạn, không mở quyền đổi mật khẩu. |
| 13 | Yêu cầu mã mới, xác nhận đúng rồi đặt mật khẩu mới và xác nhận giống nhau. | Chỉ tài khoản A được đổi mật khẩu. Mã/quyền reset đã dùng không dùng lại được. |
| 14 | Xác minh OTP của A nhưng chưa lưu mật khẩu. Mở tab khác, bắt đầu khôi phục cho tài khoản B. Quay về form reset cũ và thử lưu. | Không được đổi mật khẩu B bằng quyền của A. B phải được xác minh bằng OTP của B. Dùng hai account test do bạn kiểm soát. |
| 15 | Xác minh mã, sau đó chờ 5 phút trước khi lưu mật khẩu mới. | Quyền reset hết hạn riêng; yêu cầu xác minh lại. |
| 16 | Đang đăng nhập Customer, dùng Admin ở trình duyệt khác khóa account đó; quay lại Profile và thử reload/lưu. | Phiên không còn được đọc/sửa hồ sơ; yêu cầu đăng nhập lại. |
| 17 | Logout rồi mở trực tiếp `/profile`. | Không thấy dữ liệu cá nhân, được yêu cầu đăng nhập. |

Giới hạn bổ sung: mỗi nguồn gửi tối đa 10 yêu cầu OTP trong 10 phút. Khi chủ động test nhiều nhánh OTP, có thể cần chờ cửa sổ này hết. Giới hạn và trạng thái OTP hiện nằm trong bộ nhớ của một tiến trình ứng dụng; restart làm mất phiên OTP. Đây chưa phải bộ giới hạn phân tán cho nhiều server.

Muốn xác nhận DB lưu hash: chỉ kiểm tra prefix, không chụp toàn bộ hash hay mật khẩu vào evidence.

```sql
SELECT id, email, LEFT(password_hash, 14) AS password_format
FROM users WHERE email = 'email-test-cua-ban@example.com';
-- Sau đăng ký/đổi/reset hoặc login nâng cấp tài khoản cũ: pbkdf2_sha256$
```

Ghi lại bước lỗi, tài khoản test, thông báo thực tế và ảnh nếu cần gửi để sửa tiếp. Chưa điền Passed trong System Test cho đến khi bạn đã kiểm tra giao diện và môi trường thật.

## Thay đổi đã thực hiện

- Register/Profile dùng chung validation phía server; bắt buộc confirm password, giới hạn tên/email/mật khẩu/địa chỉ và kiểm tra ngày sinh.
- CSRF cho login, register, quên mật khẩu, verify, resend, reset và Profile. Gửi lại OTP chuyển sang POST. Link `login.jsp` cũ chuyển về dialog đang được duy trì.
- OTP dùng SecureRandom, TTL 5 phút, tối đa 5 lần sai; cooldown email 60 giây dùng chung giữa các phiên; có giới hạn theo IP.
- Quyền reset chứa user ID/email đã xác minh, hết hạn riêng, chỉ dùng một lần và không chuyển được sang email khác. DB update theo ID, email đã xác minh và ACTIVE.
- Password mới lưu PBKDF2 có salt; mật khẩu legacy thật được nâng cấp khi login thành công. Không còn đăng nhập bằng marker demo/pending. Thao tác đổi mật khẩu lấy ID từ session và kiểm tra lại mật khẩu hiện tại.
- Profile tải lại ACTIVE từ DB, yêu cầu mật khẩu hiện tại khi đổi email, chặn trùng email và escape nội dung hồ sơ/header. Email mới vẫn là email do người dùng nhập; chưa có luồng xác minh mailbox mới riêng.
- Xóa credential SMTP ghi cứng và log OTP. Nếu credential cũ từng được dùng thật, chủ tài khoản SMTP cần thu hồi/đổi credential đó.

Migration DB và gửi email SMTP thật chưa được thực thi trên database/mailbox của bạn trong lần sửa code này.
