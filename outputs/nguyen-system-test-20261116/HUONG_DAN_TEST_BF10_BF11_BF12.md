# Hướng dẫn test gọn BF10, BF11, BF12

Mỗi luồng có 4 ca, tổng 12 ca. Mỗi ca tối đa 3–4 ảnh theo cột Note.

Tài khoản: tuynhocthoi@gmail.com — SĐT 0965999999. Dùng mật khẩu hiện tại, ghi riêng khi đổi. Ca đăng ký cần Gmail/SĐT mới chưa tồn tại.

Chạy BF10 → BF11 → BF12, mỗi luồng từ 001 đến 004. Giữ E2E Latte để test điểm. Dùng phiên Admin/Customer/Cashier riêng. Thiếu điều kiện thì để Pending và ghi lý do.

Round 1: F = kết quả, G = ngày, H = người test; O = ghi chú và mốc ảnh, P = bằng chứng. Chỉ đánh Passed khi đã đối chiếu đủ Expected Results. Ảnh chưa có sẽ để trống.

## ST-BF10-001 — Register, log in, update Profile and log out.

1. Đăng ký bằng Gmail/SĐT mới, đăng nhập và chụp thông báo thành công.
2. Dùng tuynhocthoi@gmail.com, sửa tên QA Customer Updated và địa chỉ 123 Pho Hue. Lưu, tải lại rồi chụp.
3. Đăng xuất/đăng nhập lại để kiểm tra dữ liệu. Đăng xuất, mở lại Hồ sơ và chụp yêu cầu đăng nhập.

Mốc ảnh:
Photo 1: Registration success.
Photo 2: Saved profile after reload.
Photo 3: Login required after logout.

## ST-BF10-002 — Recover a forgotten password by email OTP.

1. Quên mật khẩu: nhập tuynhocthoi@gmail.com, gửi OTP và chụp thông báo gửi.
2. Nhập mã nhận được trong 5 phút, đặt mật khẩu mới và chụp thông báo thành công.
3. Thử mật khẩu cũ phải bị từ chối; mật khẩu mới vào đúng hồ sơ. Chụp hồ sơ, ghi lại mật khẩu hiện tại riêng.

Mốc ảnh:
Photo 1: OTP sent notice (hide the code).
Photo 2: Password-reset success.
Photo 3: Same profile after new login.

## ST-BF10-003 — Change password from Profile.

1. Trong Đổi mật khẩu, nhập mật khẩu hiện tại sai và chụp lỗi.
2. Nhập lại mật khẩu hiện tại đúng, đặt mật khẩu mới rồi chụp thành công.
3. Đăng xuất, thử mật khẩu cũ và mới; chụp đúng hồ sơ sau đăng nhập bằng mật khẩu mới.

Mốc ảnh:
Photo 1: Wrong-current-password error.
Photo 2: Change success.
Photo 3: Same profile after new login.

## ST-BF10-004 — Reject invalid and duplicate account data; allow correction.

1. Đăng ký: thử SĐT 1234567890, email nguyen@gmaill.comm và xác nhận mật khẩu khác nhau; chụp lỗi.
2. Thử riêng email trùng và SĐT trùng; chụp một lỗi, ghi kết quả lỗi còn lại vào Note.
3. Hồ sơ: thử tên 101 ký tự, chụp lỗi. Tải lại kiểm tra dữ liệu cũ còn; sửa tên hợp lệ, lưu và chụp.

Mốc ảnh:
Photo 1: Registration field errors.
Photo 2: One duplicate-field error; record the other in Note.
Photo 3: Profile name-limit error.
Photo 4: Saved valid correction.

## ST-BF11-001 — Create category, product and recipe; verify the public menu.

1. Tạo QA Coffee hoạt động và chụp. Tạo E2E Latte giá 45000, M +0, L +10000, Pearl +8000; chụp.
2. Lưu công thức bằng hai nguyên liệu khác nhau, ghi đơn vị/định lượng. Mở lại, đối chiếu giá vốn và chụp.
3. Mở Menu như khách, chọn L + Pearl; chụp giá 63000đ.

Mốc ảnh:
Photo 1: Saved category.
Photo 2: Saved product/options.
Photo 3: Reopened recipe and cost.
Photo 4: Public product with L + Pearl = 63000 VND.

## ST-BF11-002 — Edit product and hide/show its category.

1. Đổi giá gốc thành 46000, lưu và chụp Menu L + Pearl = 64000đ.
2. Ẩn QA Coffee, tải lại Menu kiểm tra mất món; chụp trạng thái ẩn trong Admin.
3. Bật lại danh mục, trả giá 45000. Chụp Menu giá 63000đ và kiểm tra công thức vẫn còn.

Mốc ảnh:
Photo 1: Updated public price.
Photo 2: Hidden category in Admin.
Photo 3: Restored product and original price on Menu.

## ST-BF11-003 — Reject invalid product and recipe information.

1. Thử tên sản phẩm trùng, giá -1 và danh mục ẩn; chụp một lỗi, ghi các kết quả còn lại vào Note.
2. Công thức: thử hai dòng cùng nguyên liệu rồi thử định lượng 0; chụp từng lỗi.
3. Đóng/mở lại kiểm tra công thức cũ không mất. Sửa đúng, lưu và chụp công thức đã lưu.

Mốc ảnh:
Photo 1: Product validation error.
Photo 2: Duplicate-ingredient error.
Photo 3: Zero-quantity error.
Photo 4: Correct saved recipe after reopening.

## ST-BF11-004 — Delete disposable data and protect order history.

1. Chỉ dùng danh mục/món test chưa có đơn: thử xóa món rồi Cancel, chụp dữ liệu còn. Xác nhận xóa danh mục và chụp kết quả.
2. Kiểm tra món/công thức/Menu không còn dữ liệu test; món khác vẫn còn.
3. Thử xóa món đã có đơn và danh mục chứa nó, chụp lỗi. Mở lại đúng hóa đơn, chụp lịch sử còn nguyên.

Mốc ảnh:
Photo 1: Disposable category/product before deletion.
Photo 2: Deletion result.
Photo 3: Ordered-data deletion rejection.
Photo 4: Unchanged existing bill.

## ST-BF12-001 — Save loyalty rules and reject an invalid rule.

1. Ghi ba quy tắc gốc. Thử chi tiêu/điểm = 0, lưu, chụp lỗi và tải lại kiểm tra không đổi.
2. Lưu bộ 10000 / 20 / 20000, chụp thành công.
3. F5, chụp đủ ba giá trị. Giữ tới hết BF12-004 rồi khôi phục.

Mốc ảnh:
Photo 1: Invalid-rule error.
Photo 2: Save success.
Photo 3: Reloaded values and rule summary.

## ST-BF12-002 — Look up the Customer, adjust points and prevent a negative balance.

1. Admin tra 0965999999, đối chiếu đúng khách và điểm P với Hồ sơ; chụp.
2. Cộng 20 một lần. F5 hai lần, chụp Hồ sơ có P + 20 và giao dịch; không được cộng lặp.
3. Thử trừ quá số dư, chụp lỗi. Kiểm tra điểm không đổi và giữ ít nhất 20 điểm cho ca sau.

Mốc ảnh:
Photo 1: Correct Customer and starting balance.
Photo 2: Profile balance/history after +20.
Photo 3: Excessive-deduction error.

## ST-BF12-003 — Redeem points in checkout and refund once after cancellation.

1. Giỏ gồm M 45000 + L 55000, không topping. Đổi 20 điểm giảm 20000đ và chụp.
2. Tạo một đơn chờ thanh toán, chụp mã đơn/tổng; chụp điểm đã trừ và REDEEM đúng mã đơn.
3. Hủy đúng đơn đó, chụp hoàn 20 điểm. F5 để kiểm tra chỉ hoàn một lần.

Mốc ảnh:
Photo 1: Cart discount.
Photo 2: Pending order code and total.
Photo 3: REDEEM history / deducted balance.
Photo 4: Cancelled order REFUND / restored balance.

## ST-BF12-004 — Earn points from a paid counter order and verify history.

1. Ghi điểm P. Cashier tra 0965999999, chọn đúng khách, thêm M + L tổng 100000đ, không đổi điểm; chụp.
2. Thu tiền mặt test 100000đ đúng một lần, chụp hóa đơn/mã đơn.
3. Chụp Hồ sơ/đối chiếu Admin: P + 10, EARN +10 đúng mã đơn. Khôi phục quy tắc gốc và chụp.

Mốc ảnh:
Photo 1: Customer and counter-order subtotal.
Photo 2: Paid bill with order code.
Photo 3: Customer points and EARN history.
Photo 4: Restored original rules.

Ảnh đặt tên theo ca, ví dụ ST-BF10-001_P01.png. Gửi ảnh kèm mã ca để gắn đúng chỗ. Không hiển thị mật khẩu/OTP trong ảnh. Giữ nguyên kết quả thực tế; không tự điền Passed khi chưa test.
