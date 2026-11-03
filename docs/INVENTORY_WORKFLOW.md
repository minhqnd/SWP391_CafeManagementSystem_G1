# Luồng đề xuất và kiểm nhận nguyên liệu

Admin và Manager dùng chung tài khoản `role=ADMIN`, `position=MANAGER`. Barista dùng `STAFF/BARISTA`. Cả hai phải ACTIVE. Thu ngân không có quyền trong luồng này.

- Barista vào **Đề xuất & nhận hàng** trên thanh điều hướng: `/barista/restock-requests`, `/barista/purchase-receipts`.
- Admin dùng `/admin/restock-requests`, `/admin/purchases`, `/admin/purchase-receipts`.
- Các thao tác ghi kiểm tra quyền trong controller và trạng thái tài khoản trong database, có CSRF và transaction. Barista chỉ xem/sửa đề xuất của mình; các phiếu kiểm nhận của người khác không hiển thị trong danh sách của Barista.

## Đề xuất nguyên liệu

`DRAFT → PENDING_APPROVAL → APPROVED | REJECTED | CHANGES_REQUESTED`.

Barista sửa nháp hoặc đề xuất được yêu cầu sửa, rồi gửi lại. Đề xuất đang chờ không cho Barista chỉnh. Admin sửa nội dung trước khi duyệt, bắt buộc lý do và ghi cả trước/sau vào lịch sử. Yêu cầu sửa và từ chối phải có lý do; REJECTED là kết thúc, cần tạo đề xuất mới. Người tạo không được tự duyệt. Admin có thể tạo đề xuất nhưng cần một Admin khác duyệt.

Đề xuất được duyệt không làm tăng tồn. Có thể hủy nháp/chờ duyệt/yêu cầu sửa. Đề xuất đã duyệt hoặc kết thúc không sửa trực tiếp.

## Đơn mua

Admin lập đơn với nhà cung cấp ACTIVE và nguyên liệu đang dùng. Có thể liên kết một đề xuất APPROVED, chỉ dùng một lần. Khi liên kết, tập nguyên liệu phải khớp đề xuất. Số lượng và giá mua là quyết định của Admin, được lưu trong đơn mua và lịch sử.

`DRAFT → ORDERED → PARTIALLY_RECEIVED → RECEIVED`.

Admin sửa nhà cung cấp, số lượng, giá và ghi chú khi còn nháp; bắt buộc lý do. Form tạo/sửa đơn mua hiện hỗ trợ một nguyên liệu mỗi đơn. Đơn nhiều nguyên liệu cũ vẫn hiển thị và kiểm nhận từng dòng, không sửa bằng form một dòng.

Có thể hủy đơn DRAFT/ORDERED. Đơn ORDERED/PARTIALLY_RECEIVED có thể đóng phần chưa nhận thành CLOSED, bắt buộc lý do. Phải xử lý hết phiếu kiểm nhận đang chờ trước khi hủy/đóng.

## Phiếu kiểm nhận

Barista hoặc Admin ghi từng lần giao: số lượng giao, số lượng đạt chấp nhận, biên bản. Chênh lệch `giao - chấp nhận` là hàng hỏng/từ chối. Các dòng không giao lần này để 0. Chưa cộng kho khi gửi phiếu.

Admin duyệt hoặc từ chối phiếu, bắt buộc ý kiến. Chỉ cộng lượng chấp nhận khi duyệt. Nếu lũy kế vượt số lượng đặt, phải chọn xác nhận chấp nhận phần thừa. Phiếu bị từ chối không cộng kho; người kiểm nhận lập phiếu thay thế. Giao thiếu có thể nhận bổ sung nhiều lần hoặc Admin đóng phần thiếu. Phiếu gốc được giữ nguyên.

Ví dụ đặt 10 kg, giao 10 kg nhưng chỉ 8 kg đạt: duyệt chỉ cộng 8 kg; đơn PARTIALLY_RECEIVED. Lần sau nhận đạt 2 kg thì cộng 2 kg và chuyển RECEIVED. Nhận 12 kg đạt phải được Admin xác nhận phần thừa.

Khóa đơn mua và trạng thái phiếu trong transaction ngăn xác nhận hai lần. UUID duy nhất cho mỗi lần gửi phiếu/điều chỉnh ngăn gửi trùng. Mỗi lần nhận thực được ghi IN_PURCHASE riêng để lần nhận bổ sung không bị bỏ qua.

## Sau xác nhận

Admin lập điều chỉnh gắn với dòng phiếu gốc: số âm để giảm/trả hàng, số dương để sửa số nhận ghi thiếu. Bắt buộc lý do; lưu người thực hiện, thời gian và số lượng có dấu. Không sửa đè phiếu đã duyệt.

Không giảm quá số lượng nhận còn lại của dòng phiếu hoặc làm âm tồn kho. Điều chỉnh cập nhật lại tiến độ nhận của đơn; CLOSED vẫn giữ trạng thái đóng. Đây là xử lý số lượng kho, chưa quản lý công nợ, hoàn tiền hoặc điều chỉnh giá nhà cung cấp.

## Database và xác minh

Mọi thay đổi DB nằm trong `database/aura_cafe.sql`. Đầu file có khối nâng cấp tùy chọn giữ dữ liệu; toàn bộ file vẫn là bản reset và nạp dữ liệu mẫu, không chạy cả file lên DB đang dùng nếu cần giữ dữ liệu.

Test MySQL độc lập: `InventoryWorkflowIntegrationTest` chỉ chạy khi truyền `-Dinventory.test.url=jdbc:mysql://.../schema_thu_nghiem`. Test dùng root không mật khẩu và tạo dữ liệu riêng; chỉ trỏ vào database thử nghiệm. Không dùng cấu hình DB ứng dụng để chạy test này.
