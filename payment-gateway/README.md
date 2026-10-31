# ⚡ Universal Payment Gateway & Webhook Hub

> **Microservice Cổng thanh toán & Điều phối Webhook tập trung** chạy trên nền tảng **Cloudflare Workers** + **Cloudflare D1 (SQLite)**. Dùng chung được cho nhiều ứng dụng độc lập (*Aura Café, Web bán hàng, Mobile App, v.v.*) với dịch vụ thanh toán tự động qua **SePay / VietQR**.

---

## 📌 1. Thông tin triển khai hiện tại

- **Trạng thái:** `Operational` (Đang hoạt động 24/7)
- **Domain Gateway:** `https://payment-gateway.zemorts.workers.dev`
- **Dashboard xem giao dịch trực quan:** Mở trực tiếp [https://payment-gateway.zemorts.workers.dev](https://payment-gateway.zemorts.workers.dev)
- **Database Cloudflare D1:** `payment-gateway-db` (ID: `969b7f48-a740-45bf-ac46-48493a240202`)
- **SePay Webhook URL:** `https://payment-gateway.zemorts.workers.dev/api/webhooks/sepay`

---

## 🏗️ 2. Mô hình kiến trúc & Luồng hoạt động

```text
               ┌───────────────────────┐
               │    Ngân hàng / App    │
               │   (Quét mã VietQR)    │
               └──────────┬────────────┘
                          │ (Tiền vào tài khoản)
                          ▼
               ┌───────────────────────┐
               │    SePay Service      │
               └──────────┬────────────┘
                          │ (1) Gửi HTTP POST Webhook
                          ▼
         ┌───────────────────────────────────┐
         │     CLOUDFLARE WORKER GATEWAY     │
         │  (payment-gateway.workers.dev)    │
         │   • Xác thực chữ ký SePay         │
         │   • Chống trùng lặp (Idempotent)  │
         │   • Khớp mã đơn & số tiền         │
         │   • Ký chữ ký HMAC-SHA256         │
         └─────────────┬─────────────────────┘
                       │ Lưu trữ giao dịch
                       ▼
              [ Cloudflare D1 SQL ]
                       ▲
                       │ (2) Polling mỗi 2s (GET /status)
                       │     (Nhận SUCCESS + Signature)
                       │
             ┌─────────┴─────────┐
             │  Client Browser   │
             │  (Trang checkout) │
             └─────────┬─────────┘
                       │ (3) POST Callback kèm Signature
                       ▼
         ┌───────────────────────────┐
         │    CORE BACKEND SERVER    │
         │   (Java / Node / Python)  │
         │   • Verify HMAC Signature │
         │   • Chốt đơn & Cộng điểm  │
         └───────────────────────────┘
```

---

## 🚀 3. Hướng dẫn kết nối với SePay (my.sepay.vn)

### A. Chế độ Test / Demo (Không cần xác thực)
Dành cho việc test nhanh mà không cần cấu hình khóa bí mật:
1. Đăng nhập [my.sepay.vn](https://my.sepay.vn) ➔ Vào menu **Webhooks** ➔ **Thêm webhook**.
2. **URL Webhook:** `https://payment-gateway.zemorts.workers.dev/api/webhooks/sepay`
3. **Loại giao dịch:** Chọn `Tất cả` (hoặc `Tiền vào in`).
4. **Phương thức xác thực:** Chọn **Không xác thực**.
5. Bấm **Lưu** ➔ Bấm **Gửi thử** để kiểm tra (kết quả trả về `Thành công` là xong).

### B. Chế độ Production (Bảo mật tối đa với HMAC-SHA256)
1. Khi tạo/sửa Webhook trên SePay, chọn **Phương thức xác thực: HMAC-SHA256**.
2. Copy chuỗi **Secret Key** do SePay hiển thị.
3. Mở terminal tại thư mục này và gán secret vào Worker:
   ```bash
   cd payment-gateway
   npx wrangler secret put SEPAY_WEBHOOK_SECRET
   # Dán Secret Key vào rồi nhấn Enter
   ```
4. Khi đã gán key, Worker sẽ tự động kiểm tra chữ ký `X-SePay-Signature` và chống tấn công Replay Attack (quá 5 phút).

---

## 🔌 4. Tài liệu chi tiết các API của Gateway

### 1. Đăng ký đơn hàng mới (`POST /api/orders/register`)
Được gọi từ **Backend** (Java, Node, v.v.) ngay khi khách tạo đơn hàng để báo cho Gateway biết đơn cần chờ thanh toán:

- **Endpoint:** `POST /api/orders/register`
- **Headers:** `Content-Type: application/json`
- **Body:**
  ```json
  {
    "appId": "aura_cafe",
    "orderCode": "AUR-10023",
    "amount": 45000,
    "transactionCode": "TX-10023-17290000"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Order registered successfully",
    "orderCode": "AUR-10023",
    "transactionCode": "TX-10023-17290000",
    "amount": 45000
  }
  ```

---

### 2. Kiểm tra trạng thái đơn hàng (`GET /api/orders/:orderCode/status`)
Được gọi từ **Frontend (Javascript)** hoặc **Backend** định kỳ (Polling mỗi 2 giây) để biết đơn đã thanh toán hay chưa:

- **Endpoint:** `GET /api/orders/AUR-10023/status`
- **Response khi đang chờ (PENDING):**
  ```json
  {
    "success": true,
    "orderCode": "AUR-10023",
    "transactionCode": "TX-10023-17290000",
    "amount": 45000,
    "status": "PENDING",
    "signature": null,
    "gatewayReference": null,
    "paidAt": null
  }
  ```
- **Response khi đã thanh toán thành công (SUCCESS):**
  ```json
  {
    "success": true,
    "orderCode": "AUR-10023",
    "transactionCode": "TX-10023-17290000",
    "amount": 45000,
    "status": "SUCCESS",
    "signature": "bc6ef036961b8076de47e8b6db24e851d331c67c1195c1ef562253ece4cdb289",
    "gatewayReference": "FT24012345678",
    "paidAt": "2026-10-26 14:20:00"
  }
  ```

> **Ghi chú về `signature`:** Chữ ký được Worker sinh ra bằng thuật toán `HMAC-SHA256` dựa trên chuỗi chuẩn:  
> `canonical = orderCode + "|" + transactionCode + "|" + amount + "|SUCCESS"`  
> Backend của bạn chỉ cần đối soát chữ ký này bằng `SECRET_KEY` là đảm bảo an toàn tuyệt đối, chống làm giả.

---

### 3. Endpoint nhận Webhook từ SePay (`POST /api/webhooks/sepay`)
Endpoint công khai dành riêng cho SePay gửi thông báo khi có giao dịch ngân hàng:

- **Endpoint:** `POST /api/webhooks/sepay`
- **Payload chuẩn từ SePay:**
  ```json
  {
    "id": 92704,
    "gateway": "MBBank",
    "transactionDate": "2026-10-26 14:20:00",
    "accountNumber": "234556688",
    "code": "AURA261026001",
    "content": "AURA261026001 chuyen tien",
    "transferType": "in",
    "transferAmount": 45000,
    "referenceCode": "FT24012345678"
  }
  ```
- **Phản hồi của Worker:** Luôn trả về `{"success": true}` trong vòng chưa tới 10ms.

---

### 4. Giả lập thanh toán thành công (`POST /api/orders/:orderCode/simulate-pay`)
Dành riêng cho dev / trình diễn đồ án khi không muốn chuyển tiền thật:

- **Endpoint:** `POST /api/orders/AUR-10023/simulate-pay`
- **Tác dụng:** Chuyển ngay đơn hàng sang `SUCCESS` và tự động sinh chữ ký HMAC hợp lệ trong D1.

---

### 5. Xem lịch sử tất cả giao dịch (`GET /api/transactions`)
- **Endpoint:** `GET /api/transactions`
- Trả về danh sách 50 đơn hàng và 50 log webhook gần nhất lưu trong Cloudflare D1.

---

## 💻 5. Cách tích hợp vào dự án mới trong tương lai

Dịch vụ này được thiết kế theo chuẩn **đa dự án (Multi-tenant)** thông qua tham số `appId`.

Khi làm dự án mới (ví dụ: `project_bán_quần_áo`), bạn chỉ cần:
1. Gửi `appId = "shop_fashion"` khi đăng ký đơn:
   ```json
   POST /api/orders/register
   { "appId": "shop_fashion", "orderCode": "FASHION-999", "amount": 250000 }
   ```
2. Cho khách quét mã VietQR có nội dung `FASHION-999`.
3. Frontend polling: `GET /api/orders/FASHION-999/status`.
4. Không cần deploy thêm bất kỳ Worker hay Database nào khác!

---

## 🛠️ 6. Các lệnh quản trị thông dụng

```bash
# Xem log request thời gian thực của Worker
npx wrangler tail

# Chạy thử nghiệm Worker tại Localhost (cổng 8787)
npm run dev

# Chạy câu lệnh SQL trực tiếp trên Cloudflare D1
npx wrangler d1 execute payment-gateway-db --remote --command="SELECT * FROM orders LIMIT 10"

# Cập nhật code mới lên Cloudflare
npx wrangler deploy
```
