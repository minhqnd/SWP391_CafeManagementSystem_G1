# Rà soát validation BF10, BF11 và BF12

Ngày: 15/11/2026. Phạm vi: code hiện tại trong workspace. Chưa sửa mã nguồn hay kết quả System Test.

**Kết luận: đã có nhiều validation cơ bản, nhưng chưa đủ để kết luận ba luồng được kiểm tra chặt chẽ.** Những điểm cần ưu tiên là ràng buộc quyền reset mật khẩu với đúng tài khoản, email duy nhất, bảo vệ OTP, trạng thái tài khoản và chống gửi lặp khi điều chỉnh điểm.

Kết quả kiểm tra tự động: 33 test, 0 failures, 0 errors, 0 skipped, thuộc `AdminRecipeControllerTest` (9), `UserLoginIdentifierTest` (6), `CartTest` (10), `ShopSettingsTest` (3), `CartServiceTest` (5). Đây là các test có sẵn; không đồng nghĩa những lỗi dưới đây đã có regression test hoặc đã kiểm tra bằng trình duyệt. Các nhận định lỗi được xác định từ đường đi code; concurrency và database thực tế cần chạy thêm trong môi trường test.

## BF10 — Account và Profile

### 1. Ưu tiên cao: quyền reset chưa gắn chặt với đúng email đã xác minh

- `VerifyOtpController.java:44` đặt `canResetPassword=true`.
- `ForgotPasswordController.java:52` có thể gán lại `forgotEmail` khi bắt đầu một yêu cầu mới, nhưng không xóa quyền `canResetPassword` cũ.
- `ResetPasswordController.java:30` chỉ kiểm tra boolean này và email hiện có trong session; không kiểm tra email hiện tại có đúng email đã xác minh hay thời hạn của quyền reset.
- Hệ quả theo code: một phiên đã xác minh OTP có thể giữ quyền reset khi đổi mục tiêu khôi phục. Việc hết hạn OTP sau 5 phút không tự khắc phục quyền reset đã cấp.
- Cần bổ sung: xóa toàn bộ trạng thái xác minh cũ khi bắt đầu/gửi lại mã; gắn quyền reset với user ID/email đã xác minh, thời điểm cấp và hạn dùng; tiêu thụ quyền một lần sau khi đổi mật khẩu. Khi OTP hết hạn cũng phải xóa quyền cũ.
- Ca kiểm tra: dùng hai tài khoản test do nhóm sở hữu, xác minh tài khoản A rồi bắt đầu yêu cầu cho B; B phải cần OTP mới trước khi reset.

### 2. Ưu tiên cao: email chưa được bảo đảm duy nhất và cập nhật mật khẩu theo email

- Schema `database/aura_cafe.sql:835` khai báo UNIQUE cho phone, chưa có UNIQUE cho email.
- `UserDAO.updateUserProfile()` tại dòng 195 cập nhật email nhưng không kiểm tra email thuộc tài khoản khác trước khi UPDATE. Bắt lỗi khóa duy nhất không bảo vệ email nếu schema chưa có khóa đó.
- `UserDAO.updatePassword()` tại dòng 179 dùng `WHERE email=?`; nếu có hai tài khoản cùng email thì có thể cập nhật nhiều tài khoản.
- Đăng ký có kiểm tra email trước INSERT, nhưng kiểm tra đó không thay thế ràng buộc DB khi hai yêu cầu đến đồng thời.
- Cần bổ sung: chuẩn hóa email nhất quán, kiểm tra trùng khi sửa hồ sơ, ràng buộc UNIQUE trong DB sau khi rà dữ liệu trùng, đổi mật khẩu theo user ID đã xác minh. Schema trong DB đang chạy cần kiểm tra riêng.

### 3. Ưu tiên cao: OTP có thể lộ khi SMTP lỗi, thiếu giới hạn thử và gửi mã lần đầu

- `ForgotPasswordController.java:61` và `ResendOtpController.java:56` đưa OTP vào thông báo lỗi khi gửi mail thất bại.
- `VerifyOtpController` chưa đếm số lần nhập sai. Cooldown 60 giây chỉ có ở `/resend-otp`; gửi lại `/forgot-password` chưa có kiểm tra tương ứng.
- Mã được tạo bằng `java.util.Random`, không phải bộ sinh số ngẫu nhiên dành cho thông tin xác thực.
- Cần bổ sung: không trả OTP cho trình duyệt khi SMTP lỗi; giới hạn thử sai, giới hạn yêu cầu theo tài khoản và nguồn gửi; dùng `SecureRandom`; áp dụng cooldown cho cả yêu cầu đầu và gửi lại.
- Đề xuất để nhóm chốt: giữ OTP 5 phút như hiện tại, tối đa 5 lần sai và tối thiểu 60 giây giữa các lần gửi. Các con số là đề xuất, chưa được áp vào code.

### 4. Ưu tiên cao: mật khẩu lưu và so sánh trực tiếp

- `UserDAO.java:61`, `:128`, `:179` so sánh/lưu trực tiếp mật khẩu; nhánh `demo_hash` chấp nhận các mật khẩu demo.
- Cần bổ sung: hash mật khẩu bằng cơ chế phù hợp, xác minh hash khi đăng nhập, bỏ nhánh mật khẩu demo khỏi luồng dùng thật và có phương án chuyển dữ liệu tài khoản cũ.
- `LoginController` và `UserDAO.login()` còn trim mật khẩu; nên thống nhất việc giữ nguyên chuỗi mật khẩu ở đăng ký, đăng nhập, đổi và reset để tránh đăng ký được nhưng không đăng nhập được với khoảng trắng đầu/cuối.

### 5. Thiếu kiểm tra bắt buộc, giới hạn và định dạng phía server

- `RegisterController.java:75` chỉ so sánh confirm_password nếu trường này được gửi và không rỗng. Bỏ trường xác nhận vẫn có thể qua kiểm tra server.
- Register/Profile chưa giới hạn fullName theo cột 120 ký tự, email theo 150 ký tự; chưa kiểm tra đủ ngày sinh hợp lệ/không ở tương lai và giới hạn địa chỉ tại controller.
- Email regex hiện tại cho phép chuỗi rất yếu như `a@b`; chính sách điện thoại cũng khác nhau giữa đăng ký, hồ sơ và HTML.
- Cần bổ sung: bắt buộc xác nhận mật khẩu; dùng chung bộ chuẩn hóa/validation; kiểm tra giới hạn trước khi ghi DB; kiểm tra ngày tháng rõ ràng; thông báo lỗi theo trường. Không chỉ dựa vào `required`, `maxlength` hoặc JavaScript trong form.

### 6. Profile chưa có CSRF và chưa xác thực lại trạng thái tài khoản trong DB

- `ProfileController.doPost()` không kiểm tra CSRF cho sửa hồ sơ/đổi mật khẩu. Không thấy filter CSRF chung thay thế trong phạm vi code đã đọc.
- ACTIVE lấy từ đối tượng user trong session; tài khoản bị khóa sau khi đăng nhập có thể chưa được nhận biết trong phiên đang mở.
- Cần bổ sung: token cho các POST thay đổi dữ liệu và kiểm tra trạng thái tài khoản hiện tại trước thao tác quan trọng. Nếu đổi email dùng cho khôi phục, nên có bước xác minh quyền sở hữu email mới.

## BF11 — Menu và Recipe

### 7. Danh mục ẩn chưa được chặn nhất quán ở truy vấn sản phẩm và thêm giỏ

- `ProductDAO.getProductById()` tại dòng 371 chỉ lọc product ID; nhiều truy vấn công khai chỉ kiểm tra `p.is_available=1`, không kiểm tra `categories.status='ACTIVE'`.
- `CartService.createItem()` tại dòng 45 và `refreshCart()` tại dòng 65 chỉ nhìn trạng thái available của product.
- Theo đường đi code, ẩn category không đủ bảo đảm sản phẩm biến mất khỏi mọi danh sách hoặc bị từ chối khi thêm từ trang cũ.
- Cần bổ sung điều kiện category ACTIVE ở toàn bộ đường công khai, gồm xem danh sách/chi tiết, thêm giỏ, làm mới giỏ và xác nhận đơn. Admin vẫn cần xem danh mục/món bị ẩn để quản lý.
- Ca System Test liên quan: ST-BF11-004; chưa chạy thì giữ Pending, không đổi kỳ vọng để che lỗi.

### 8. Trùng tên size/topping và giá topping 0 chưa xử lý đúng

- `AdminProductsController.java:203` dựng JSON cho size mà không phát hiện tên size trùng. Ví dụ M=0 và M=10000 cùng được chấp nhận; kiểm tra đã có size 0 có thể qua trước khi khóa JSON trùng bị ghi đè.
- `:216` cũng chưa chặn topping trùng tên.
- Server cho lưu topping giá 0, nhưng `CartService.java:168` biến mọi giá không lớn hơn 0 thành mặc định 8000. Vì vậy topping miễn phí có thể bị tính tiền 8000.
- Cần bổ sung: kiểm tra tên trùng sau trim, quy định rõ so sánh hoa/thường; giữ giá 0 nếu chính sách cho topping miễn phí, chỉ dùng mặc định khi thực sự thiếu giá. Đồng bộ cách đọc options trên Menu, Cart và POS.
- Không nên parse cấu hình JSON bằng regex nếu cần xử lý đúng tên có ký tự quote/escape; dùng một cách đọc JSON nhất quán.

### 9. Admin guard và giới hạn giá cần bổ sung

- Guard trong `AdminCategoriesController.java:232`, `AdminProductsController.java:315`, `AdminRecipeController.java:294` chỉ kiểm tra role ADMIN, không kiểm tra ACTIVE ngay cả trên đối tượng session.
- Giá cơ bản và phụ thu có kiểm tra không âm/tối đa 2 số lẻ, nhưng chưa đặt trần tương thích kiểu tiền trong DB và tổng giá của một món; giá quá lớn hoặc số dạng exponent có thể đi sâu tới lỗi chuyển đổi/DB.
- Cần bổ sung: kiểm tra ADMIN đang ACTIVE, kiểm tra lại DB cho POST; giới hạn giá, tổng phụ thu và số lượng dòng options theo quy tắc đã thống nhất; báo lỗi trước khi lưu.
- Không kết luận sản phẩm phải có tên duy nhất nếu SRS chưa yêu cầu; kiểm tra trùng size/topping là vấn đề khác vì chúng được dùng làm khóa lựa chọn.

## BF12 — Phone Loyalty

### 10. Chống gửi lặp chưa an toàn khi hai yêu cầu đồng thời

- `AdminLoyaltyController.java:104` đọc và so token; sau `adjustPoints()`/`updateRules()` mới thay token tại dòng 133.
- Hai request cùng token có thể cùng vượt bước so sánh trước khi một request thay token. Row lock trong DAO giữ số dư nhất quán nhưng không ngăn cùng một thao tác bị áp dụng hai lần.
- Đây là rủi ro từ thứ tự thao tác trong code; chưa chạy hai request đồng thời để xác nhận trong môi trường thực tế.
- Cần bổ sung: tiêu thụ token nguyên tử hoặc idempotency key duy nhất được ghi cùng transaction với thay đổi điểm. Không chỉ kiểm tra token rồi rotate sau khi ghi DB.
- Ca kiểm tra cần hai request đồng thời cùng operationToken; kiểm tra chỉ một thay đổi điểm và một ADJUST được ghi.

### 11. Kiểm tra long trước khi ép int còn thiếu cận dưới

- `AdminLoyaltyController.java:180` đọc customerId/points dạng long; dòng 182 chỉ loại giá trị lớn hơn Integer.MAX_VALUE rồi ép sang int tại 185–186.
- Trong Java, giá trị âm rất lớn như -4294967295 khi ép int trở thành 1, sau đó có thể qua kiểm tra số dương.
- Cần kiểm tra miền hợp lệ ngay trên long: customerId từ 1 đến Integer.MAX_VALUE, points từ 1 đến 1000000; sau đó mới dùng Math.toIntExact. Kiểm tra user đích đúng vai trò và chính sách trạng thái.

### 12. Lưu quy tắc loyalty có thể ghi đè cài đặt giao hàng khi có người sửa đồng thời

- `AdminLoyaltyController.updateRules()` tải toàn bộ ShopSettings rồi gọi `ShopSettingDAO.saveSettings()`.
- DAO lưu cả ba khóa giao hàng và ba khóa loyalty, dù Admin tại màn hình này chỉ thay loyalty. Một bản đọc cũ có thể ghi đè thay đổi giao hàng của một Admin khác.
- Cần tách hàm chỉ cập nhật ba khóa loyalty hoặc dùng kiểm soát phiên bản cho cập nhật cài đặt. Transaction bảo đảm cùng lưu/rollback nhưng không tự ngăn ghi đè dữ liệu cũ.

## Những kiểm tra hiện đã có

- BF10: chỉ ACTIVE đăng nhập, role đăng ký do server đặt, đổi session khi đăng nhập; OTP có TTL 300000 ms và bị xóa khi dùng đúng; reset hiện đã kiểm tra mật khẩu ít nhất 6 ký tự và khớp xác nhận. Một số comment cũ nói chưa có TTL/min-length là không còn đúng với điều kiện code hiện tại.
- BF11: tên danh mục/sản phẩm có giới hạn; category status được whitelist; kiểm tra CSRF; ảnh tối đa 5 MB với MIME/chữ ký tệp; size không âm, có ít nhất một size 0; BOM chống trùng ingredient, lượng dương, tối đa 3 số lẻ và có trần; thay BOM trong transaction và rollback khi lỗi. DAO chặn thêm ingredient INACTIVE mới vào BOM, nhưng cho giữ dòng INACTIVE cũ.
- BF12: Admin guard có kiểm tra ACTIVE trong session; CSRF và operation token; giới hạn quy tắc; điều chỉnh điểm dùng transaction và khóa user; chặn số dư âm và vượt int; điểm đổi được giới hạn theo balance, mốc và tiền hàng; thanh toán/hoàn điểm có các guard riêng theo order.

## Quyết định cần từ nhóm

1. Có bổ sung toàn bộ 12 nhóm kiểm tra trên, ưu tiên BF10 trước, rồi tính giá/ẩn danh mục BF11 và chống lặp BF12 không?
2. Có thống nhất OTP 5 phút, tối đa 5 lần nhập sai và cooldown gửi mã 60 giây cho cả yêu cầu đầu lẫn gửi lại không?
3. Topping giá 0 có được phép không? Code form hiện cho phép, nên đề xuất giữ miễn phí và sửa phần tính giỏ để thống nhất. Không tự đổi chính sách này khi chưa chốt.

Sau khi code được sửa, cần thêm regression test cho từng lỗi và cập nhật các ca âm/biên trong ba sheet tương ứng. Chưa đánh dấu bất cứ ca System Test nào Passed chỉ dựa vào lần rà soát này.
