# Aura Café Payment Gateway

Cloudflare Worker (TypeScript + Hono), dùng D1 để đăng ký yêu cầu thanh toán,
nhận webhook SePay, ký kết quả cho Java và lưu từng giao dịch để đối soát.

Nhánh `feature/payment-reconciliation` chứa bản thử, chưa được nghiệm thu xuyên
suốt với Java và ngân hàng. Tài liệu này mô tả source hiện tại; không khẳng định
code mới đã được deploy. Xem [kế hoạch triển khai](../docs/PAYMENT_RECONCILIATION_PLAN.md).

## 1. Luồng và phạm vi hiện tại

1. Java đăng ký mã đơn, mã payment và số tiền nguyên VND lên gateway.
2. SePay gửi webhook. Gateway xác thực bằng HMAC hoặc API key.
3. Chỉ xử lý tiền vào; tiền ra được bỏ qua, không ghi ledger.
4. Ghép mã đơn đầy đủ, không phân biệt hoa thường, giữ dấu phân cách.
5. Ghi ledger `bank_transactions` và cập nhật đơn gateway trong cùng transaction.
6. Trang Payment lấy trạng thái/chữ ký rồi gửi callback về Java. Java xác minh
   chữ ký và kiểm tra payment/đơn trước khi cập nhật đơn, cộng điểm một lần.

| Tình huống | Hành vi |
|---|---|
| Đúng mã, đủ tiền | MATCHED; gateway chuyển đơn sang SUCCESS |
| Chuyển thừa | OVERPAID; SUCCESS, lưu riêng thực nhận/phân bổ/phần thừa |
| Chuyển thiếu | UNDERPAID; đơn vẫn PENDING, chưa tự cộng dồn chuyển bù |
| Chuyển tiền thật lần hai | DUPLICATE_PAYMENT; giữ giao dịch mới, không trả đơn lần nữa |
| Webhook lặp cùng SePay ID | Không ghi hoặc xử lý lần hai |
| Sai mã/nội dung | UNMATCHED; không tự tạo đơn SUCCESS |
| Khớp nhiều đơn | AMBIGUOUS; không tự chọn một đơn |
| Đơn gateway không còn payable | NOT_PAYABLE; không xác nhận |
| Lỗi DB | Rollback, trả 503 để bên gửi retry |

Không đối chiếu tài khoản nhận trong phiên bản này. HMAC/API key chỉ xác thực
nguồn webhook; không thay thế đối soát nghiệp vụ.

**Chưa có:** đồng bộ ledger vào Java, trang Admin đối soát, phân bổ tiền thiếu,
hoàn tiền, ghép lại giao dịch đến trước đăng ký đơn, đồng bộ hủy/hết hạn từ Java.
NOT_PAYABLE chỉ căn cứ trạng thái gateway đã biết. Dashboard cũ chỉ hiển thị đơn
đăng ký gần đây; chưa hiển thị sổ đối soát mới. `webhook_logs` là dữ liệu cũ, webhook
mới ghi vào `bank_transactions`.

## 2. Cấu trúc source

```text
src/
  index.ts                    # App, CORS, health và gắn route
  types.ts                    # Binding Cloudflare
  lib/signatures.ts           # HMAC và định dạng callback
  routes/
    orders.ts                 # Đăng ký, trạng thái, mô phỏng
    webhooks.ts               # Xác thực SePay, tìm đơn, gọi đối soát
    reconciliation.ts         # Feed giao dịch dành cho backend
    dashboard.ts              # Dashboard và API lịch sử cũ
  services/reconciliation.ts  # Kiểm tra, ghép mã, transaction ledger/đơn
  views/dashboard.ts          # HTML/CSS/JavaScript dashboard
schema.sql                    # Schema canonical D1, không phải MySQL của Java
wrangler.toml                 # Worker và binding DB
test/reconciliation.test.mjs  # Test HTTP + SQL qua adapter D1 dùng SQLite local
```

Đọc luồng tiền vào từ `routes/webhooks.ts` tới `services/reconciliation.ts`.
Test cần Node.js 24+; test SQLite không thay thế test trên runtime D1 thật.

## 3. Cài đặt và chạy local

Mở terminal tại thư mục `payment-gateway` của repo:

```bash
cd payment-gateway
npm ci
npm test
npm run typecheck
npm run db:init:local
npm run dev
```

Wrangler tự bundle TypeScript; không cần chạy Maven để build Worker.
Local thường chạy ở `http://localhost:8787`. `.dev.vars` đã được gitignore;
chỉ đặt khóa test trong file này:

```dotenv
SECRET_KEY=local-test-callback-key
SEPAY_API_KEY=local-test-sepay-key
RECONCILIATION_API_KEY=local-test-feed-key
ALLOW_TEST_SIMULATION=true
```

Ví dụ test webhook bằng khóa local ở trên, sau khi đã đăng ký đơn local:

```bash
curl -i http://localhost:8787/api/webhooks/sepay \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Apikey local-test-sepay-key' \
  --data '{"id":10001,"gateway":"TestBank","transactionDate":"2026-11-04 16:00:00","accountNumber":"TEST","transferType":"in","transferAmount":100000,"content":"AURA-TEST-001"}'
```

Không gửi webhook giả hoặc gọi simulate-pay lên gateway đang nhận tiền thật.

## 4. Chuẩn bị cấu hình Cloudflare

`wrangler.toml` hiện trỏ tới:

- Worker: `payment-gateway`.
- D1: `payment-gateway-db`, binding `DB`.
- URL được cấu hình trong ứng dụng Java: `https://payment-gateway.zemorts.workers.dev`.
  URL thực tế sau deploy lấy từ đầu ra Wrangler.

Chưa khai báo environment staging. Không dùng `--env staging` nếu chưa tự tạo
Worker/D1 riêng và bổ sung environment tương ứng. Các lệnh `--remote` bên dưới
và `npm run deploy` tác động tài nguyên đang chỉ định trong `wrangler.toml`.

Cấu hình source dùng khóa callback demo và tắt mô phỏng mặc định.
Trước khi dùng cho tiền thật:

1. Trong `[vars]`, đổi `ALLOW_TEST_SIMULATION = "false"`.
2. Bỏ các dòng `SECRET_KEY`, `SEPAY_API_KEY`, `SEPAY_WEBHOOK_SECRET` khỏi `[vars]`
   nếu chuyển chúng sang Worker secrets. Không commit khóa thật vào TOML.
3. Đặt secret callback giống `payment_callback_secret` phía Java; đổi đồng bộ hai bên.
4. Chọn một phương thức xác thực webhook theo cấu hình SePay thực tế:
   API key hoặc HMAC. Code ưu tiên HMAC nếu cả hai secret cùng tồn tại.

| Biến | Vai trò |
|---|---|
| SECRET_KEY | Ký callback; phải khớp Java |
| SEPAY_API_KEY | Kiểm tra header `Authorization: Apikey ...` |
| SEPAY_WEBHOOK_SECRET | Kiểm tra HMAC và timestamp webhook |
| RECONCILIATION_API_KEY | Bearer key của feed; chỉ backend giữ, không đưa vào browser |
| ALLOW_TEST_SIMULATION | Chỉ bật `true` trên môi trường thử |

Nếu cả hai khóa SePay chưa được cấu hình, Worker trả HTTP 503 và bỏ qua giao dịch.
Cấu hình cùng khóa ở SePay và Worker trước khi chuyển sang bản này. Registration hiện chỉ kiểm
tra chữ ký nếu request có gửi; status/dashboard cũ chưa có kiểm soát truy cập Admin.
Đây là các giới hạn còn phải xử lý trước khi triển khai production.

## 5. Build và deploy bằng Wrangler

Quy trình dưới đây cập nhật Worker hiện có. Chạy trong thư mục `payment-gateway`.

### Kiểm tra tài khoản và build

```bash
npx wrangler login
npx wrangler whoami
npm ci
npm test
npm run typecheck
npx wrangler deploy --dry-run
```

Nếu đã đăng nhập đúng tài khoản thì có thể bỏ `login`. Dry-run kiểm tra bundle,
không deploy Worker và không tự tạo bảng D1.

### Cập nhật D1 trước khi đưa webhook mới vào chạy

```bash
npm run db:init:remote
npx wrangler d1 execute payment-gateway-db --remote \
  --command="SELECT name FROM sqlite_master WHERE type='table' AND name IN ('orders','webhook_logs','bank_transactions')"
```

Script `db:init:remote` áp dụng `schema.sql` bằng `CREATE TABLE/INDEX IF NOT EXISTS`:
không xóa dữ liệu cũ, không reset DB, không chuyển log cũ thành ledger mới.
Nó cũng không tự thay đổi cấu trúc một bảng đã tồn tại; thay đổi schema về sau cần
được kiểm tra riêng. Không import `database/aura_cafe.sql` của Java vào D1.

### Đặt secret

Sau khi chỉnh `[vars]` như mục 4, dùng prompt để nhập khóa, không ghi khóa vào lệnh:

```bash
npx wrangler secret put SECRET_KEY
npx wrangler secret put RECONCILIATION_API_KEY
```

Chọn **một** trong hai lệnh sau theo chế độ SePay đang dùng:

```bash
npx wrangler secret put SEPAY_API_KEY
# Hoặc:
npx wrangler secret put SEPAY_WEBHOOK_SECRET
```

Secret đã đúng thì không cần đặt lại mỗi lần deploy. Lệnh quản lý secret cũng
thay đổi cấu hình Worker trên Cloudflare; không phải bước local-only.
Giữ secret SePay hiện hữu nếu đang dùng đúng chế độ, tránh đổi nhầm xác thực.

### Deploy

```bash
npm run deploy
```

Script này gọi `wrangler deploy`, tự bundle và upload Worker. Không cần upload
mỗi file TypeScript riêng. Đặt URL webhook trong SePay là:

```text
https://<worker-domain>/api/webhooks/sepay
```

Với chế độ API key, SePay phải gửi `Authorization: Apikey <key>`.
Với HMAC, handler hiện yêu cầu `X-SePay-Timestamp` (Unix giây, lệch tối đa 5 phút)
và `X-SePay-Signature: sha256=<hex>` ký chuỗi `timestamp.rawBody`.
Phải xác nhận dịch vụ SePay đang dùng gửi đúng hợp đồng này trước khi chọn HMAC.

## 6. Kiểm tra sau deploy

Thay `<worker-domain>` bằng domain Wrangler vừa trả về:

```bash
curl -i https://<worker-domain>/api/health
npx wrangler tail
```

Health phải trả HTTP 200 với `status: "ok"`; điều này chưa chứng minh kết nối
Java/SePay đã đúng. Kiểm tra truy vấn chỉ đọc khi cần:

```bash
npx wrangler d1 execute payment-gateway-db --remote \
  --command="SELECT id,sepay_id,order_id,received_amount,allocated_amount,excess_amount,reconciliation_status FROM bank_transactions ORDER BY id DESC LIMIT 10"
```

Trên môi trường thử riêng, chạy các tình huống đủ/thiếu/thừa/chuyển hai lần,
webhook lặp, sai nội dung, nhiều mã và lỗi retry. Khi nối Java, xác nhận payment,
trạng thái đơn và điểm khách khớp, không cộng lần hai. Gateway SUCCESS chưa có nghĩa
Java đã nhận và áp dụng callback thành công.

## 7. Hợp đồng API

| Method | URL | Hành vi |
|---|---|---|
| GET | /api/health | Health check |
| POST | /api/orders/register | Đăng ký appId/orderCode/transactionCode/amount; chi tiết khác bản đã đăng ký trả 409 |
| POST | /api/webhooks/sepay | Nhận webhook và ghi ledger |
| GET | /api/orders/:orderCode/status | Trạng thái payment và chữ ký; chưa có đơn cũng trả PENDING |
| GET | /api/reconciliation/events?afterId=0 | Bearer key bắt buộc; tối đa 100 event, trả nextCursor |
| POST | /api/orders/:orderCode/simulate-pay | Chỉ khi ALLOW_TEST_SIMULATION=true; không tạo bank ledger |
| GET | /api/transactions | API cũ: 50 đơn và 50 webhook_logs gần nhất |
| GET | / | Dashboard cũ, chưa phải trang đối soát Admin |

Registration Java ký chuỗi `appId|orderCode|amount` bằng HMAC-SHA256 (hex), gửi
qua `X-Signature` hoặc trường `signature`. Nên luôn gửi transactionCode ổn định
qua mọi lần đăng ký lại. Code chưa cô lập hoàn chỉnh nhiều app: status lookup chưa
lọc appId; không dùng bản này như gateway multi-tenant production.

Callback ký `orderCode|transactionCode|amount|STATUS`. Với chuyển thừa, amount
trong callback vẫn là tiền phải trả cho đơn; tiền thực nhận nằm trong ledger.
Feed hiện là API đọc có Bearer key; Java chưa đồng bộ hoặc dùng feed để xử lý
đối soát/hoàn tiền. Không công bố RECONCILIATION_API_KEY cho frontend.

## 8. Lỗi thường gặp

| Lỗi | Kiểm tra |
|---|---|
| `no such table: bank_transactions` | Áp dụng schema trên đúng D1 local hoặc remote |
| Webhook 401 | Khóa, phương thức auth; nếu HMAC kiểm tra timestamp/raw body |
| Feed 401 | Secret RECONCILIATION_API_KEY và Bearer header |
| Webhook 503 | Xem tail, kiểm tra binding/schema/DB rồi retry cùng SePay ID |
| Registration 409 | Mã đơn đã gắn số tiền/mã payment khác; không ghi đè đơn đã trả |
| UNMATCHED | Mã phải nguyên vẹn; bỏ dấu phân cách không còn được tự ghép |
| Gateway SUCCESS, Java chưa xác nhận | Callback secret, transactionCode, trạng thái đơn và log Java |
| Dashboard không thấy webhook mới | Dashboard đọc webhook_logs cũ; xem bank_transactions/feed |

Nếu deploy lỗi, xem phiên bản và rollback Worker bằng Wrangler; rollback Worker
không rollback schema hay dữ liệu D1. Không xóa ledger để xử lý lỗi deploy.

## Tài liệu Cloudflare

- [Lệnh Worker: deploy, secret, tail](https://developers.cloudflare.com/workers/wrangler/commands/workers/)
- [Lệnh D1: execute và export](https://developers.cloudflare.com/d1/wrangler-commands/)
