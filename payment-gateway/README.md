# ⚡ Universal Payment Gateway & SePay Webhook Hub

Hệ thống Cổng thanh toán & Điều phối Webhook SePay siêu nhẹ được xây dựng bằng **TypeScript**, framework **Hono**, chạy trên nền tảng **Cloudflare Workers** và sử dụng cơ sở dữ liệu **Cloudflare D1 (SQLite)**.

Hệ thống được thiết kế theo phong cách đơn giản, trực quan, dễ bảo trì, phục vụ việc nhận tiền chuyển khoản qua mã VietQR từ SePay và đối soát với ứng dụng Aura Café Management System.

---

## 1. Cơ chế hoạt động

```
[Khách hàng]
     │ Quét mã VietQR và chuyển khoản ngân hàng
     ▼
[Ngân hàng] ──(Biến động số dư)──► [SePay]
                                      │
                                      ▼ Bắn Webhook (POST /api/webhooks/sepay)
                             [Cloudflare Worker]
                             ┌──────────────────────────────────────┐
                             │ 1. Lưu log vào bảng webhook_logs     │
                             │ 2. Dò mã đơn trong nội dung CK       │
                             │ 3. Cộng dồn tiền, khớp -> SUCCESS   │
                             └──────────────────────────────────────┘
                                      ▲
                                      │ Polling hỏi trạng thái (GET /api/orders/:code/status)
                             [Backend Java (Tomcat)]
                             ┌──────────────────────────────────────┐
                             │ Thấy SUCCESS -> Chốt đơn & Cộng điểm │
                             └──────────────────────────────────────┘
```

1. **Khách đặt hàng**: Hệ thống Java tạo đơn hàng và gọi API `POST /api/orders/register` lên Gateway để đăng ký đơn chờ thanh toán (`PENDING`).
2. **Khách quét mã QR và chuyển khoản**: SePay nhận được tiền từ ngân hàng sẽ gửi Webhook `POST /api/webhooks/sepay` sang Gateway.
3. **Gateway xử lý**:
   - Lưu vết giao dịch vào bảng `webhook_logs` (tự động loại trừ trùng lặp).
   - Quét tìm đơn hàng `PENDING` có mã đơn trùng khớp với nội dung chuyển khoản.
   - Tính tổng số tiền khách đã chuyển (hỗ trợ chuyển nhiều lần cộng dồn). Nếu đủ tiền, cập nhật trạng thái đơn sang `SUCCESS`.
4. **Java cập nhật**: Backend Java định kỳ gọi `GET /api/orders/:code/status`, khi thấy `SUCCESS` sẽ chốt đơn sang `CONFIRMED` và cộng điểm tích luỹ cho khách.

---

## 2. Cấu trúc Source Code

```text
payment-gateway/
├── src/
│   ├── index.ts              # Khởi tạo Hono app, cấu hình CORS, Health Check và gắn Route
│   ├── types.ts              # Khai báo biến môi trường Cloudflare (Bindings: DB, ALLOW_TEST_SIMULATION)
│   ├── routes/
│   │   ├── orders.ts         # API đăng ký đơn hàng, kiểm tra trạng thái và giả lập
│   │   ├── webhooks.ts       # API nhận Webhook SePay, ghi log & tự động khớp mã đơn
│   │   └── dashboard.ts      # API lấy danh sách giao dịch & trang chủ Dashboard
│   └── views/
│       └── dashboard.ts      # Giao diện HTML Dashboard trực quan xem lịch sử đơn hàng
├── test/
│   └── reconciliation.test.mjs  # Unit test kiểm tra luồng thanh toán & cộng dồn tiền
├── schema.sql                # Cấu trúc cơ sở dữ liệu SQLite D1 (orders, webhook_logs)
├── wrangler.toml             # File cấu hình deploy Cloudflare Worker & D1 Database
└── package.json              # Khai báo thư viện và các script lệnh
```

---

## 3. Hướng dẫn chạy thử ở Local (Local Development)

### Bước 1: Cài đặt thư viện
Mở Terminal tại thư mục `payment-gateway`:
```bash
cd payment-gateway
npm install
```

### Bước 2: Chạy kiểm tra lỗi kiểu dữ liệu & Unit test
```bash
npm run typecheck
npm test
```
*(Cả 3 test case tự động sẽ chạy trên cơ sở dữ liệu SQLite in-memory và báo Pass).*

### Bước 3: Khởi tạo cơ sở dữ liệu SQLite D1 ở local
```bash
npm run db:init:local
```
Lệnh này sẽ chạy file `schema.sql` để tạo các bảng `orders` và `webhook_logs` tại môi trường local của Wrangler.

### Bước 4: Chạy Gateway ở Local
```bash
npm run dev
```
Server local sẽ khởi động tại: `http://localhost:8787`
- Mở trình duyệt truy cập `http://localhost:8787` để xem Dashboard trực quan.
- Kiểm tra Health check: `http://localhost:8787/api/health`

### Bước 5: Bật giả lập thanh toán khi test local (Tùy chọn)
Nếu muốn test giả lập thanh toán mà không cần chuyển tiền thật:
1. Tạo file `.dev.vars` trong thư mục `payment-gateway` với nội dung:
   ```env
   ALLOW_TEST_SIMULATION=true
   ```
2. Chạy lại `npm run dev`. Khi đó bạn có thể dùng cURL để đánh dấu đơn hàng thành công ngay lập tức:
   ```bash
   curl -X POST http://localhost:8787/api/orders/AUR-20261109-001/simulate-pay
   ```

---

## 4. Hướng dẫn Deploy lên Cloudflare Workers

Khi muốn triển khai lên server Cloudflare để nhận Webhook thật từ SePay ngoài Internet:

### Bước 1: Đăng nhập vào tài khoản Cloudflare
Chạy lệnh sau trên terminal để đăng nhập qua trình duyệt:
```bash
npx wrangler login
```
Kiểm tra xem đã đăng nhập đúng tài khoản chưa:
```bash
npx wrangler whoami
```

### Bước 2: Kiểm tra cấu hình cơ sở dữ liệu Cloudflare D1
Trong file [wrangler.toml](wrangler.toml) đã có sẵn ID của database D1:
```toml
[[d1_databases]]
binding = "DB"
database_name = "payment-gateway-db"
database_id = "969b7f48-a740-45bf-ac46-48493a240202"
```

Khởi tạo các bảng trên cơ sở dữ liệu D1 Cloudflare thật (chỉ cần chạy một lần duy nhất hoặc khi có thay đổi cấu trúc bảng):
```bash
npm run db:init:remote
```

### Bước 3: Deploy Worker lên Cloudflare
Chạy lệnh:
```bash
npm run deploy
```
Wrangler sẽ tự động build mã TypeScript và đẩy lên mạng lưới toàn cầu của Cloudflare.
Sau khi chạy xong, terminal sẽ in ra đường dẫn công khai của Worker, ví dụ:
```text
Uploaded payment-gateway (X.XX sec)
Deployed payment-gateway triggers (X.XX sec)
  https://payment-gateway.zemorts.workers.dev
```

### Bước 4: Xem log thời gian thực khi có lỗi (Debug)
Nếu muốn theo dõi log khi SePay gửi webhook tới Worker trên production:
```bash
npx wrangler tail
```

---

## 5. Cấu hình Webhook trên trang quản trị SePay

1. Đăng nhập vào tài khoản SePay: [https://my.sepay.vn](https://my.sepay.vn).
2. Vào mục **Tích hợp Webhook** -> **Thêm Webhook mới**.
3. Điền các thông tin:
   - **URL Webhook**: `https://<domain-worker-cua-ban>/api/webhooks/sepay`
     *(Ví dụ: `https://payment-gateway.zemorts.workers.dev/api/webhooks/sepay`)*
   - **Sự kiện (Event)**: Chọn biến động số dư tiền vào tài khoản.
   - **Kiểu dữ liệu**: JSON.
4. Bấm **Lưu** và thử tính năng **Gửi thử Webhook** từ giao diện SePay để kiểm tra kết quả trả về `HTTP 200 {"success":true}`.

---

## 6. Danh sách các API Endpoints

| Phương thức | Đường dẫn API | Mô tả | Ai gọi |
|---|---|---|---|
| `GET` | `/api/health` | Kiểm tra server có đang hoạt động bình thường không | Mọi người |
| `GET` | `/` | Giao diện Dashboard xem 50 đơn hàng và 50 webhook mới nhất | Quản trị viên / Trình duyệt |
| `GET` | `/api/transactions` | Lấy dữ liệu JSON cho Dashboard | Frontend Dashboard |
| `POST` | `/api/orders/register` | Đăng ký đơn hàng mới với trạng thái `PENDING` | Backend Java khi tạo đơn |
| `GET` | `/api/orders/:orderCode/status` | Kiểm tra trạng thái đơn (`PENDING` hoặc `SUCCESS`) | Backend Java kiểm tra tiền về |
| `POST` | `/api/webhooks/sepay` | Nhận thông báo tiền về từ ngân hàng | SePay tự động gọi |
| `POST` | `/api/orders/:orderCode/simulate-pay` | Giả lập khách đã thanh toán thành công (khi test local) | Tester / Developer |

---

## 7. Ví dụ gọi thử API bằng cURL

### 1. Đăng ký đơn hàng mới
```bash
curl -X POST http://localhost:8787/api/orders/register \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "aura_cafe",
    "orderCode": "AUR-101",
    "transactionCode": "TX-AUR-101",
    "amount": 55000
  }'
```

### 2. Kiểm tra trạng thái đơn
```bash
curl http://localhost:8787/api/orders/AUR-101/status
```

### 3. Bắn Webhook giả lập SePay báo tiền về
```bash
curl -X POST http://localhost:8787/api/webhooks/sepay \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1001,
    "gateway": "MBBank",
    "accountNumber": "0987654321",
    "transferType": "in",
    "transferAmount": 55000,
    "content": "Chuyen tien ca phe don hang AUR-101",
    "referenceCode": "MB-123456",
    "transactionDate": "2026-11-09 11:30:00"
  }'
```
Sau lệnh trên, kiểm tra lại trạng thái đơn sẽ thấy chuyển sang `SUCCESS`!
