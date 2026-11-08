# Payment reconciliation — bản thử

Branch: `feature/payment-reconciliation` (từ develop). Chưa deploy, chưa thay dữ liệu thật.

## Quy tắc nghiệp vụ

Một đơn được thanh toán một lần; mỗi lần chuyển tiền thật là một giao dịch riêng.
Webhook lặp cùng SePay ID không phải chuyển tiền lần hai. Giữ nguyên giao dịch gốc.
Phạm vi chỉ gồm tiền vào; webhook tiền ra được bỏ qua, không tạo mục đối soát.
Không đối chiếu tài khoản nhận trong bản này.

| Tình huống | Kết quả |
|---|---|
| Đúng mã, đúng tiền | MATCHED; tự xác nhận payment |
| Chuyển thừa | OVERPAID; xác nhận phần phải trả, lưu phần thừa chờ hoàn |
| Chuyển thiếu | UNDERPAID; không tự cộng dồn; chờ đối soát |
| Đơn đã nhận tiền, có giao dịch mới | DUPLICATE_PAYMENT; lưu toàn bộ khoản mới chờ xử lý |
| Không có mã đơn hợp lệ | UNMATCHED; không tạo đơn SUCCESS |
| Nội dung khớp nhiều đơn | AMBIGUOUS; không tự chọn một đơn |
| Tiền về cho đơn không còn payable | NOT_PAYABLE; chờ xử lý |

## Bước 1 — Gateway ledger (đã code thử)

- Bảng D1 `bank_transactions` lưu mã SePay, nội dung gốc, thực nhận, phân bổ cho đơn,
  phần thừa và kết quả đối soát. Unique SePay ID; retry không sửa bản ghi đầu tiên.
- Chỉ ghép toàn bộ mã đơn không phân biệt hoa thường. Giữ dấu phân cách; không ghép gần
  đúng hoặc chọn đơn đầu tiên khi mơ hồ. Nội dung ngân hàng bị biến dạng sẽ chờ đối soát.
- Ghi ledger và xác nhận đơn cùng D1 batch transaction; lỗi lưu trữ trả 503 để retry.
- Đăng ký lại cùng thông tin không reset SUCCESS; khác số tiền/mã giao dịch trả 409.
- API GET `/api/reconciliation/events?afterId=0` trả tối đa 100 sự kiện theo cursor.
  Bắt buộc Bearer `RECONCILIATION_API_KEY`, chỉ backend Java được giữ khóa này.
- Hợp đồng callback cũ vẫn ký số tiền đơn (allocated), không ký tiền thừa thay cho tiền đơn.
  Ledger mới là nguồn số tiền thực nhận. Java hiện chưa đồng bộ ledger này.
- Test HTTP handler với SQLite thật trong bộ chuyển đổi D1 cục bộ; chưa phải nghiệm thu
  runtime Cloudflare/D1 thật hoặc giao dịch ngân hàng thật. Cần Node 24+ để chạy test.

D1 dùng `payment-gateway/schema.sql` hiện có, không tạo thêm file migration. MySQL Java
chưa đổi. Khi đến bước 2, mọi thay đổi MySQL gộp vào `database/aura_cafe.sql`.

## Bước 2 — Java và Admin (chưa code)

- Java gọi feed bằng khóa server, đồng bộ idempotent theo gateway event ID; lưu cursor
  cùng transaction ghi dữ liệu, chỉ tăng cursor khi ghi thành công; không bỏ qua lỗi.
- Lưu giao dịch thực nhận riêng với payment của đơn; khoản hoàn và quyết định đối soát
  có lịch sử actor/time/reason. Không sửa/xóa raw bank event.
- Admin: lịch sử, lọc ngày/đơn/mã ngân hàng/trạng thái, chi tiết, gắn giao dịch chưa khớp,
  xác nhận các khoản thiếu đã đủ, ghi nhận hoàn thủ công (mã chuyển/bằng chứng).
- Mọi quyết định đối soát kiểm tra quyền ACTIVE ADMIN, CSRF, số tiền chưa phân bổ,
  khóa DB, chống gửi trùng; payment/order/điểm cập nhật một transaction.
- Chưa tự gọi API hoàn tiền ngân hàng. Nút đã hoàn chỉ ghi nhận thao tác bên ngoài.

## Bước 3 — Tích hợp và nghiệm thu (chưa code/chưa chạy)

- Đồng bộ lifecycle Java -> gateway: hủy/hết hạn/đổi COD; hiện gateway không biết đơn
  đã hủy ở Java, nên NOT_PAYABLE chỉ áp dụng trạng thái mà gateway đã biết.
- Xác minh HMAC của từng sự kiện/decision nếu dùng chúng để xác nhận thủ công; feed
  hiện phục vụ đọc dữ liệu, chưa được Java sử dụng để thay đổi payment.
- Kiểm tra cả trường hợp đăng ký đơn đến sau webhook; giao dịch chưa khớp không được
  tự biến thành paid, cần retry/đối soát có kiểm soát.
- Làm chặt auth registration/status/dashboard hiện hữu, tắt simulation khi triển khai,
  cấu hình secret SePay. Đây chưa phải bản sẵn sàng deploy.
- Test hai lần chuyển đồng thời trên D1 runtime, callback lặp/sai chữ ký, lỗi mạng/DB,
  đơn hủy, thiếu + chuyển bù, thừa/trùng + hoàn; xác nhận không cộng điểm hai lần.
- Cập nhật SRS/SDS, test report và ảnh thật sau khi luồng chạy xuyên suốt. Không gắn
  nhãn hoàn tất nghiệp vụ chỉ từ test gateway.

## Chạy kiểm tra bản thử

Trong `payment-gateway/`: `npm test`, `npm run typecheck`.
Trước khi test runtime cục bộ, áp dụng schema D1 vào database local riêng và cấu hình
SePay API key/HMAC test. Không chạy lệnh remote/deploy từ bản thử này.
