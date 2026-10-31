# Aura Café — Target System Design

**Trạng thái:** Baseline dài hạn cho tài liệu và triển khai  
**Ngày chốt:** 24/10/2026  
**Mục đích:** Đây là nguồn sự thật để vẽ Use Case, Swimlane, Sequence, Class Diagram và thiết kế Database. Code hiện tại phải được chỉnh theo tài liệu này, không sửa tài liệu để chạy theo các màn hình mẫu hoặc dữ liệu hard-code.

## 1. Nguyên tắc kiến trúc

1. Mọi luồng theo thứ tự: **Actor → JSP/UI → Controller → Service/DAO → Database**.
2. JSP không gọi DAO, không chứa dữ liệu nghiệp vụ viết cứng và không tự tính giá/điểm/tồn kho.
3. Controller kiểm tra đăng nhập, vai trò, dữ liệu đầu vào và trạng thái trước khi gọi tầng nghiệp vụ.
4. DAO chỉ thực hiện truy vấn; nghiệp vụ nhiều bảng phải chạy trong một transaction.
5. Giá bán, phụ thu, điểm, phí giao hàng, tồn kho và tổng thanh toán luôn được tính lại phía server.
6. Không có voucher/promotion code.
7. Ảnh danh mục và sản phẩm lưu trong database bằng BLOB.
8. Báo cáo là dữ liệu tổng hợp từ bảng nghiệp vụ, không lưu số liệu báo cáo viết cứng.

## 2. Actor chính

| Actor | Quyền chính |
|---|---|
| Guest | Xem trang chủ, menu, chi tiết món, đăng ký, đăng nhập |
| Customer | Quản lý hồ sơ, giỏ hàng, đặt online, thanh toán, theo dõi đơn, tích/dùng điểm, gửi feedback |
| Cashier | Tạo và quản lý đơn tại quầy, nhận diện khách qua SĐT, thanh toán, bàn giao ca |
| Barista | Nhận đơn pha chế, xem công thức, bắt đầu và hoàn tất món |
| Shipper | Nhận đơn giao, cập nhật giao hàng, báo sự cố, thu và đối soát COD |
| Admin | Quản trị danh mục, sản phẩm, công thức, kho, nhập hàng, nhân sự, khách hàng, feedback, báo cáo và cài đặt |

## 3. Loyalty theo số điện thoại

### Quy tắc

- Mỗi Customer có một `users.phone` duy nhất.
- Số dư hiện tại lưu tại `users.total_points`.
- Lịch sử lưu tại `loyalty_transactions`.
- Admin cấu hình:
  - `points_earn_unit`: số tiền hàng để nhận 1 điểm.
  - `points_step`: mốc điểm được sử dụng mỗi lần.
  - `points_step_value`: số tiền giảm cho mỗi mốc.
- Không có voucher.

### Luồng tích điểm

1. Payment thành công.
2. Server lấy tiền hàng sau giảm, không tính phí giao hàng.
3. Điểm nhận = `floor(goods_amount / points_earn_unit)`.
4. Ghi giao dịch `EARN`.
5. Cộng `users.total_points`.
6. Tất cả thực hiện trong cùng transaction với payment/order.

### Luồng dùng điểm

1. Customer hoặc Cashier nhận diện Customer bằng tài khoản/SĐT.
2. Server đọc số dư và cấu hình từ DB.
3. Điểm dùng phải là bội số của `points_step`.
4. Tiền giảm = số mốc × `points_step_value`, không vượt quá tiền hàng.
5. Khi tạo đơn, khóa Customer, trừ điểm và ghi `REDEEM` trong transaction.
6. Nếu hủy đơn hợp lệ trước khi hoàn tất, hoàn điểm bằng `REFUND`.
7. Admin có thể sửa sai bằng `ADJUST`; mọi điều chỉnh phải có lịch sử.

## 4. Order và Payment

### Trạng thái Order

`PENDING → CONFIRMED → PREPARING → READY → DELIVERING → COMPLETED`

- DINE_IN/TAKEAWAY có thể đi từ READY sang COMPLETED.
- ONLINE phải qua DELIVERING trước COMPLETED.
- CANCELLED là nhánh kết thúc từ các trạng thái được nghiệp vụ cho phép.

### Thanh toán

- Phương thức: CASH, BANK_TRANSFER, VNPAY, MOMO.
- Mỗi lần thử thanh toán là một Payment.
- Chỉ Payment SUCCESS mới xác nhận đơn và cộng điểm.
- Callback/cổng thanh toán phải xác minh giao dịch trước khi ghi SUCCESS.
- COD dùng Payment có `payment_method=CASH`; không tạo bảng COD riêng.

## 5. Kho, đề xuất nhập và phiếu nhập

### Restock Request

- Là đề nghị nội bộ khi nguyên liệu thiếu.
- Bảng riêng: `restock_requests`, `restock_request_items`.
- Trạng thái: `DRAFT → PENDING_APPROVAL → APPROVED | REJECTED | CANCELLED`.
- APPROVED có thể được chuyển thành Purchase Order.

### Purchase Order

- Là đơn mua chính thức gửi nhà cung cấp.
- Bảng: `purchase_orders`, `purchase_order_items`.
- Trạng thái: `DRAFT → ORDERED → RECEIVED` hoặc `CANCELLED`.
- Khi RECEIVED:
  1. Khóa PO.
  2. Cộng `ingredients.current_stock`.
  3. Ghi `inventory_transactions(IN_PURCHASE)`.
  4. Giá cost gần nhất lấy từ `purchase_order_items.unit_price`.
  5. Commit cùng transaction.
- `purchase_orders` có cột `note`.

### Cost nguyên liệu

- Cost công thức dùng **giá nhập gần nhất đã RECEIVED** của nguyên liệu.
- Không dùng bình quân tất cả lần nhập.
- `product_ingredients.quantity_required` dùng `DECIMAL(12,3)`.

## 6. Delivery

- Shipper chỉ nhìn thấy đơn ONLINE ở READY/DELIVERING phù hợp.
- Nhận đơn gắn `orders.shipper_id`.
- Luồng: nhận đơn → DELIVERING → giao thành công → COMPLETED.
- COD lấy từ Payment CASH.
- Sự cố lưu riêng trong `delivery_incidents` để một đơn có nhiều lần báo.
- Trạng thái sự cố: `OPEN → PROCESSING → RESOLVED | REJECTED`.

## 7. Feedback

- Bảng `customer_reviews` dùng chung đánh giá và khiếu nại.
- RATING bắt buộc rating từ 1 đến 5.
- COMPLAINT không bắt buộc rating.
- Luồng: `PENDING → PROCESSING → RESOLVED | REJECTED`.
- Admin response và thông tin xử lý lấy từ DB.
- Ảnh đính kèm phải lưu bằng bảng ảnh riêng hoặc BLOB, không lưu đường dẫn máy cục bộ.

## 8. Sản phẩm, ảnh và công thức

- Category 1—N Product.
- Ảnh chính lưu tại `category_images` và `product_images`.
- `image_url` chỉ được giữ trong giai đoạn migration; thiết kế đích bỏ hai cột URL.
- Variant của Product lưu JSON có cấu trúc được server kiểm tra.
- Product N—N Ingredient qua ProductIngredient.
- Recipe có trạng thái ACTIVE/INACTIVE hoặc kế thừa trạng thái Product; phải chọn một quy tắc duy nhất khi triển khai.

## 9. Nhân sự và ca làm

- Thiết kế đích có bảng `work_shifts` cho UC55.
- `work_schedules.shift_id` tham chiếu `work_shifts.id`.
- WorkShift có tên, giờ bắt đầu, giờ kết thúc và trạng thái.
- WorkSchedule lưu nhân viên, ngày, ca, check-in/out, trạng thái và thông tin két nếu là Cashier.
- Staff chỉ xem lịch/chấm công của mình; Admin được quản lý toàn bộ.

## 10. Báo cáo

- Không tạo bảng chứa số liệu dashboard.
- ReportDAO tổng hợp từ orders, order_items, payments, ingredients và inventory_transactions.
- Bộ lọc ngày do Controller kiểm tra.
- Doanh thu chỉ tính Payment SUCCESS/Order COMPLETED theo định nghĩa đã thống nhất.
- Mọi KPI, biểu đồ và danh sách top sản phẩm phải do Controller bind vào JSP.

## 11. Class diagram đích

Các entity bắt buộc:

`User, WorkShift, WorkSchedule, Category, CategoryImage, Product, ProductImage, ProductIngredient, Ingredient, Supplier, RestockRequest, RestockRequestItem, PurchaseOrder, PurchaseOrderItem, InventoryTransaction, Order, OrderItem, Payment, LoyaltyTransaction, CustomerReview, DeliveryIncident, ShopSetting`.

Các lớp điều khiển/DAO đặt tên tương ứng chức năng. Không tạo một Controller chung cho Customer hoặc Staff nếu từng use case đã có Controller riêng.

## 12. Quy tắc dữ liệu trên UI

- Dữ liệu danh sách dùng `c:forEach` từ request attribute.
- KPI dùng dữ liệu Controller/DTO.
- Select nhà cung cấp, nguyên liệu, trạng thái và ngày phải lấy từ Controller hoặc enum dùng chung.
- JSP chỉ được hard-code: nhãn, tiêu đề, placeholder, tên cột và thông báo hướng dẫn.
- Không hard-code tên người, SĐT, mã đơn, giá tiền, số lượng, KPI hoặc ngày báo cáo.
- Khi chưa có dữ liệu, hiển thị empty state; không hiển thị bản ghi mẫu.

## 13. Quy tắc dùng tài liệu

- Use Case và Swimlane mô tả hành vi trong file này.
- Sequence Diagram dùng lớp đích trong file này; lớp chưa code được ghi `<<planned>>`.
- Class Diagram dùng entity và quan hệ đích trong file này.
- ERD dùng schema đích; mọi thay đổi DB phải cập nhật duy nhất trong `database/aura_cafe.sql`.
- Ma trận triển khai chỉ theo dõi tiến độ, không thay đổi nghiệp vụ đã chốt.
