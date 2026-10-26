# Universal Payment Gateway & Webhook Hub

Một dịch vụ microservice Gateway độc lập chạy trên **Cloudflare Workers** + **Cloudflare D1 (SQLite)**, đóng vai trò làm trạm trung chuyển điều phối thanh toán tự động qua **SePay / VietQR**, có thể dùng chung cho nhiều dự án khác nhau.

---

## 1. Tính năng chính
- Nhận Webhook tự động từ **SePay** khi có biến động số dư chuyển khoản ngân hàng (VietQR).
- Xác thực chữ ký bảo mật Webhook: hỗ trợ cả **HMAC-SHA256** và **API Key**.
- Chống xử lý trùng lặp giao dịch (Idempotency) bằng `sepay_id`.
- Tự động đối soát mã đơn hàng (`order_code`) và số tiền (`amount`).
- Cung cấp API Polling thời gian thực cho Client/Frontend.
- Sinh chữ ký xác thực chuẩn `HMAC-SHA256` khi thanh toán thành công để báo về Backend lõi (Java / Node / Python...).
- Giao diện Dashboard trực quan xem lịch sử giao dịch tại route `/`.

---

## 2. Hướng dẫn Deploy lên Cloudflare trong 3 phút

### Bước 1: Cài đặt dependencies
```bash
cd payment-gateway
npm install
```

### Bước 2: Tạo Cloudflare D1 Database
```bash
npx wrangler d1 create payment-gateway-db
```
*Lệnh trên sẽ in ra `database_id` của bạn (ví dụ: `xxxx-xxxx-xxxx`).*
Mở file `wrangler.toml` và thay thế giá trị `database_id = "..."` tương ứng:
```toml
[[d1_databases]]
binding = "DB"
database_name = "payment-gateway-db"
database_id = "xxxx-xxxx-xxxx" # Dán ID vừa tạo vào đây
```

### Bước 3: Khởi tạo bảng dữ liệu trên Cloudflare D1
```bash
npx wrangler d1 execute payment-gateway-db --remote --file=./schema.sql
```

### Bước 4: Deploy Worker
```bash
npx wrangler deploy
```
*Sau khi deploy thành công, bạn sẽ nhận được một URL công khai dạng:*
```text
https://payment-gateway.<your-subdomain>.workers.dev
```

---

## 3. Cấu hình trên SePay Dashboard (my.sepay.vn)

1. Đăng nhập vào [my.sepay.vn](https://my.sepay.vn).
2. Vào mục **Webhooks** ➔ **Thêm Webhook**:
   - **URL Webhook**: `https://payment-gateway.<your-subdomain>.workers.dev/api/webhooks/sepay`
   - **Loại giao dịch**: Tất cả (hoặc Chỉ tiền vào `in`)
   - **Phương thức xác thực**: Chọn **HMAC-SHA256** (hoặc **API Key**)
3. Nếu chọn **HMAC-SHA256**:
   - Copy Secret Key trên SePay, chạy lệnh sau để gán bí mật vào Cloudflare:
     ```bash
     npx wrangler secret put SEPAY_WEBHOOK_SECRET
     # Dán secret key vào và Enter
     ```
4. Bấm **Gửi thử** trên SePay để kiểm tra. Kết quả báo `Thành công` là hoàn tất!

---

## 4. Tài liệu API (Dành cho các ứng dụng kết nối)

### A. Đăng ký đơn hàng cần thanh toán
- **URL**: `POST /api/orders/register`
- **Body (JSON)**:
  ```json
  {
    "appId": "aura_cafe",
    "orderCode": "AUR-10023",
    "amount": 45000,
    "transactionCode": "TX-10023-17290000"
  }
  ```

### B. Kiểm tra trạng thái đơn (Polling)
- **URL**: `GET /api/orders/:orderCode/status`
- **Response**:
  ```json
  {
    "success": true,
    "orderCode": "AUR-10023",
    "transactionCode": "TX-10023-17290000",
    "amount": 45000,
    "status": "SUCCESS",
    "signature": "a1b2c3d4...",
    "gatewayReference": "FT24012345678",
    "paidAt": "2026-10-26 14:00:00"
  }
  ```

### C. Mô phỏng thanh toán thành công (Test Mode)
- **URL**: `POST /api/orders/:orderCode/simulate-pay`
