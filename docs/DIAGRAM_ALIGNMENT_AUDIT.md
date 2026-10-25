# Diagram Alignment Audit

> Thiết kế dài hạn đã được chốt tại [TARGET_SYSTEM_DESIGN.md](./TARGET_SYSTEM_DESIGN.md). File audit này chỉ dùng để so sánh code hiện tại với thiết kế đích; nếu có khác biệt, TARGET_SYSTEM_DESIGN.md được ưu tiên.

Ngày đối chiếu: 24/10/2026

Tài liệu này là nguồn tham chiếu khi vẽ **swimlane**, **sequence diagram** và **class diagram**. Không xem JSP mẫu hoặc lớp có `TODO` là nghiệp vụ đã triển khai.

## 1. Quy ước trạng thái

- **LIVE**: Controller gọi DAO và đọc/ghi database thật.
- **PARTIAL**: có code thật nhưng thiếu một nhánh use case.
- **SCAFFOLD**: đã có route, Controller, DAO/model/JSP nhưng phương thức chính còn `TODO`.
- **UI ONLY**: dữ liệu đang viết tĩnh trong JSP.

## 2. Chuỗi truy vết theo phân hệ

| Phân hệ | Actor | Route | Controller | DAO chính | Bảng DB | Trạng thái |
|---|---|---|---|---|---|---|
| Đăng ký/đăng nhập | Guest/Customer/Staff/Admin | `/register`, `/login` | RegisterController, LoginController | UserDAO | users | LIVE |
| Hồ sơ khách | Customer | `/profile` | ProfileController | UserDAO | users, loyalty_transactions | LIVE |
| Menu/chi tiết món | Guest/Customer | `/menu`, `/product-detail` | MenuController, ProductDetailController | ProductDAO, CategoryDAO | categories, products, product_images | LIVE |
| Giỏ hàng | Guest/Customer | `/cart` | CartController | ProductDAO qua CartService | session + products | LIVE, còn code đổi điểm cần bỏ |
| Đặt online | Customer | `/checkout` | CheckoutController | OrderDAO | orders, order_items, ingredients, inventory_transactions | LIVE |
| Thanh toán online | Customer | `/payment` | PaymentController | PaymentDAO | payments, orders, loyalty_transactions, users | LIVE nội bộ, không có callback ngân hàng |
| Theo dõi đơn | Customer | `/orders` | OrdersController | OrderDAO | orders, order_items, payments | LIVE |
| POS tại quầy | Cashier | `/cashier`, `/counter-order` | CounterOrderController | OrderDAO, UserDAO | orders, order_items, payments, users | PARTIAL |
| Pha chế | Barista | `/barista` | BaristaController | OrderDAO, ProductIngredientDAO | orders, order_items, product_ingredients | LIVE |
| Giao vận | Shipper | `/delivery` | DeliveryController | DeliveryDAO | orders, payments; chưa có bảng sự cố/COD | SCAFFOLD/UI ONLY |
| Danh mục | Admin | `/admin/categories` | AdminCategoriesController | CategoryDAO | categories, category_images | LIVE |
| Sản phẩm | Admin | `/admin/products` | AdminProductsController | ProductDAO | products, product_images | LIVE |
| Công thức | Admin | `/admin/recipes` | AdminRecipeController | ProductIngredientDAO | product_ingredients | PARTIAL |
| Nguyên liệu | Admin | `/admin/ingredients` | AdminIngredientController | IngredientDAO | ingredients, suppliers | LIVE |
| Tồn kho | Admin | `/admin/inventory` | AdminInventoryController | InventoryDAO | ingredients, inventory_transactions | LIVE |
| Đề xuất nhập | Admin | `/admin/restock-requests` | AdminRestockController | RestockRequestDAO | purchase_orders, purchase_order_items | LIVE nhưng đang dùng chung bảng PO |
| Phiếu nhập | Admin | `/admin/purchases` | AdminPurchaseController | PurchaseOrderDAO | purchase_orders, purchase_order_items | SCAFFOLD/UI ONLY |
| Nhà cung cấp | Admin | `/admin/suppliers` | AdminSupplierController | SupplierDAO | suppliers | LIVE |
| Nhân viên | Admin | `/admin/employees` | AdminEmployeesController | EmployeeDAO | users | LIVE |
| Ca/lịch/chấm công | Admin | `/admin/shifts`, `/admin/schedules`, `/admin/attendance` | AdminShiftController, AdminScheduleController, AdminAttendanceController | ShopSettingDAO, WorkScheduleDAO | shop_settings, work_schedules | LIVE |
| Lịch/chấm công cá nhân | Staff | `/staff/schedule`, `/staff/attendance` | StaffScheduleController, StaffAttendanceController | WorkScheduleDAO dự kiến | work_schedules | SCAFFOLD |
| Bàn giao ca | Cashier | `/cashier/handover` | CashierShiftHandoverController | WorkScheduleDAO | work_schedules, orders, payments | LIVE |
| Khách hàng | Admin | `/admin/customers` | AdminCustomersController | AdminCustomerDAO | users, orders | LIVE |
| Tích điểm SĐT | Admin | `/admin/loyalty` | AdminLoyaltyController | LoyaltyTransactionDAO/UserDAO | users, loyalty_transactions | SCAFFOLD; PaymentDAO đã cộng điểm thật |
| Feedback khách | Customer | `/feedback` | FeedbackController | FeedbackDAO | customer_reviews, users, orders | LIVE |
| Feedback Admin | Admin | `/admin/feedback` | AdminFeedbackController | FeedbackDAO dự kiến | customer_reviews | SCAFFOLD/UI ONLY |
| Báo cáo | Admin | `/admin/reports` | AdminReportsController | ReportDAO | orders, order_items, payments, ingredients | SCAFFOLD/UI ONLY |
| Quản lý đơn Admin | Admin | `/admin/orders` | AdminOrdersController | AdminOrderDAO | orders, order_items, payments, users | LIVE |
| Cài đặt | Admin | `/admin/settings` | AdminSettingsController | ShopSettingDAO | shop_settings | LIVE |

## 3. Luồng trạng thái chuẩn

### Online order

`PENDING → CONFIRMED → PREPARING → READY → DELIVERING → COMPLETED`

- `CANCELLED` chỉ là nhánh kết thúc.
- PaymentDAO chuyển `PENDING → CONFIRMED` sau thanh toán thành công.
- Barista xử lý `CONFIRMED → PREPARING → READY`.
- Delivery xử lý `READY → DELIVERING → COMPLETED`.

### Counter order

`PENDING/CONFIRMED → PREPARING → READY → COMPLETED`, tùy thời điểm ghi nhận thanh toán.

### Purchase order

Schema chỉ cho phép: `DRAFT → ORDERED → RECEIVED` hoặc `CANCELLED`.

Không dùng trạng thái `REJECTED` trong sequence/class diagram vì ENUM của DB không có giá trị này.

### Feedback

`PENDING → PROCESSING → RESOLVED | REJECTED`.

## 4. Sequence chuẩn để vẽ

### UC11 đặt hàng online

1. Customer chọn món.
2. JSP gọi CartController.
3. CartController gọi CartService.
4. CartService đọc ProductDAO để xác minh giá, trạng thái và variant.
5. Cart được lưu trong HTTP session.
6. Customer mở Checkout.
7. CheckoutController kiểm tra role CUSTOMER, giỏ, tồn kho và thông tin giao hàng.
8. CheckoutController gọi OrderDAO.placeOrder.
9. OrderDAO mở transaction, ghi orders và order_items, kiểm tra/trừ kho theo BOM, ghi inventory_transactions.
10. Commit thành công, Controller redirect tới PaymentController.
11. PaymentController gọi PaymentDAO.
12. PaymentDAO khóa order, ghi payments, cập nhật order CONFIRMED, cộng điểm theo SĐT, rồi commit.
13. BaristaController đọc đơn CONFIRMED.

Nhánh lỗi phải thể hiện rollback và không chuyển sang Payment nếu OrderDAO trả lỗi tồn kho.

### UC23–24 pha chế

1. Barista mở `/barista`.
2. BaristaController lấy đơn đủ điều kiện từ OrderDAO.
3. JSP hiển thị đơn chờ.
4. Barista chọn bắt đầu; Controller kiểm tra trạng thái.
5. OrderDAO cập nhật PREPARING.
6. Barista hoàn tất; OrderDAO cập nhật READY.
7. Đơn online đi sang Delivery; đơn tại quầy đi sang Cashier/hoàn tất.

### UC50–53 nhập hàng

Luồng đang chạy thật nằm trong AdminRestockController và RestockRequestDAO:

1. Admin xem nguyên liệu dưới ngưỡng.
2. Controller lấy ingredients và purchase_orders.
3. Admin tạo phiếu DRAFT.
4. DAO ghi purchase_orders và purchase_order_items trong transaction.
5. Admin chuyển DRAFT → ORDERED.
6. Khi nhận hàng, DAO khóa phiếu, cộng ingredients.current_stock, ghi inventory_transactions loại IN_PURCHASE và chuyển RECEIVED.

AdminPurchaseController/PurchaseOrderDAO hiện là khung tách màn hình, chưa thay thế luồng trên.

### UC62 tích điểm theo số điện thoại

1. Customer/Cashier cung cấp số điện thoại.
2. UserDAO tìm CUSTOMER theo phone.
3. Sau khi thanh toán thành công, PaymentDAO tính `floor(tiền hàng / 10.000)`.
4. PaymentDAO ghi loyalty_transactions loại EARN.
5. PaymentDAO cộng users.total_points trong cùng transaction.

Không có voucher. Khách được dùng điểm giảm từng đơn theo points_step và points_step_value trong shop_settings.

## 5. Quan hệ class/database chính

- User 1—N Order qua customer_id.
- User 1—N Order qua cashier_id.
- User 1—N Order qua shipper_id.
- Order 1—N OrderItem.
- Order 1—N Payment theo schema; nghiệp vụ thường dùng một payment thành công.
- Product N—1 Category.
- Product N—N Ingredient qua ProductIngredient.
- Supplier 1—N Ingredient.
- Supplier 1—N PurchaseOrder.
- PurchaseOrder 1—N PurchaseOrderItem.
- Ingredient 1—N PurchaseOrderItem.
- Ingredient 1—N InventoryTransaction.
- Customer(User) 1—N LoyaltyTransaction.
- Customer(User) 1—N CustomerReview.
- Order 0..1—N CustomerReview theo schema hiện tại.
- User 1—N WorkSchedule.

## 6. Sai lệch phải xử lý trước khi chốt sơ đồ

### Critical

1. **Luồng dùng điểm đã có nhưng chưa đồng bộ toàn bộ giao diện quản trị.** OrderDAO và các màn đặt hàng đã có nhánh REDEEM/REFUND; màn quản trị phải đọc các giá trị cấu hình từ `shop_settings`, không hard-code.
2. **Purchase Order có hai hướng DAO.** RestockRequestDAO đang triển khai thật; PurchaseOrderDAO mới là scaffold. Class diagram chỉ nên chọn một hướng đích.
3. **DeliveryIncident và DeliveryCodRecord chưa có bảng DB.** Không được vẽ là entity lưu trữ nếu chưa bổ sung DDL; COD có thể suy ra từ payments.

### High

4. **Định lượng công thức lệch kiểu:** ProductIngredient.quantityRequired là double nhưng product_ingredients.quantity_required là INT UNSIGNED. Nên đổi DB sang DECIMAL(12,3) hoặc đổi model/form sang số nguyên.
5. **purchase_orders không có note** nhưng modal tạo phiếu có trường note.
6. **Ảnh có hai nguồn:** categories/products còn image_url trong khi category_images/product_images lưu BLOB. Cần quy định bảng BLOB là nguồn chính và image_url chỉ fallback, hoặc bỏ image_url.
7. **Work Shift không có bảng riêng.** Ca làm việc nằm trong shop_settings còn lịch/chấm công nằm trong work_schedules; class diagram không nên tạo entity WorkShift nếu giữ thiết kế này.

### Medium

8. Feedback model chưa ánh xạ đầy đủ các cột mở rộng như category, title, images, contact_method, progress_percent và resolution.
9. Payment ghi BANK_TRANSFER thành công ngay khi khách xác nhận; chưa có callback/verification từ ngân hàng.
10. Các JSP UI mẫu chứa số liệu tĩnh; sequence phải đánh dấu là planned cho tới khi Controller bind dữ liệu thật.

## 7. Quy tắc vẽ

- Sequence của chức năng LIVE dùng tên lớp và phương thức thật.
- Sequence của SCAFFOLD dùng stereotype `<<planned>>`.
- Class diagram entity chỉ đưa trường tồn tại trong aura_cafe.sql hoặc đánh dấu `<<transient>>`.
- Swimlane luôn tách Actor, JSP/UI, Controller, DAO/Service và Database.
- Mọi cập nhật nhiều bảng phải có nhánh commit/rollback.
- Không vẽ thao tác trực tiếp từ JSP tới DAO hoặc database.
