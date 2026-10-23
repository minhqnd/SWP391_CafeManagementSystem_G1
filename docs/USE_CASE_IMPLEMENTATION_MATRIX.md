# Cafe Management System — đối chiếu use case và code

Ngày rà soát: 24/10/2026. Trạng thái được xác định từ controller, DAO, JSP và schema hiện có.

Quy ước: **Đã có** = có luồng xử lý và dữ liệu thật; **Một phần** = đã có một phần nghiệp vụ nhưng thiếu nhánh trong mô tả; **UI mẫu** = giao diện lấy từ `C:\Users\nguye\Desktop\cafe_system`, chưa có backend.

| # | Màn hình | UC | Trạng thái | Route / code hiện tại | Phần còn thiếu hoặc lưu ý khi viết tài liệu |
|---:|---|---|---|---|---|
| 1 | Authentication | UC01–04 | Đã có | `/register`, `/login`, `/profile`, logout tại `/login?action=logout` | `AuthController` cũ là placeholder, không dùng trong luồng thật. |
| 2 | Customer Profile | UC05 | Đã có | `/profile` | Xem/sửa thông tin, đổi mật khẩu, xem điểm. |
| 3 | Homepage | UC06 | Đã có | `/`, `index.jsp` | Chỉ mô tả nội dung/featured products; không ghi là có quản trị voucher. |
| 4 | Menu | UC07–08 | Đã có | `/menu` | Lọc danh mục, tìm tên, giá, tình trạng bán. |
| 5 | Product Detail | UC09 | Đã có | `/product-detail`, `/menu?action=detail` | Size mặc định là size phụ thu 0đ. |
| 6 | Shopping Cart | UC10, UC64 | Đã có | `/cart`, `/cart/add`, `/cart/api` | Có đổi số lượng/xóa/đổi điểm; không có voucher. |
| 7 | Online Ordering | UC11 | Đã có | `/checkout` | Chỉ CUSTOMER được đặt; nhận địa chỉ, người nhận, ghi chú. |
| 8 | Counter Ordering | UC18,19,21,22 | Một phần | `/cashier`, `/counter-order` | Có tạo/xử lý/hủy; chưa sửa danh sách món của đơn đã lưu. Lý do hủy chưa lấy từ UI. |
| 9 | Payment | UC12–13 | Một phần | `/payment`, Counter POS | Có CASH/BANK_TRANSFER nội bộ; chưa tích hợp callback/cổng ngân hàng thật. |
| 10 | Bill | UC14 | Đã có | `/bill` | Hóa đơn theo order và quyền truy cập. |
| 11 | Customer Order Management | UC15–17,25 | Đã có | `/orders` | Theo dõi, lịch sử, chi tiết, hủy khi đủ điều kiện. |
| 12 | Counter Order List | UC20 | Đã có | `/barista`, `/cashier?tab=orders` | Barista/Cashier có danh sách theo vai trò. |
| 13 | Drink Preparation | UC23–24 | Đã có | `/barista`, `/barista/orders` | Nhận pha, đang làm, hoàn tất và xem công thức. |
| 14 | Delivery Management | UC26–30 | UI mẫu | `/delivery`, `delivery-dashboard.jsp` | Giao diện có đơn giao/sự cố/COD; chưa có phân công shipper và xử lý database. |
| 15 | Category Management | UC31–34 | Đã có | `/admin/categories` | CRUD logic theo hướng vô hiệu hóa, ảnh lưu DB. |
| 16 | Product Management | UC35–39 | Đã có | `/admin/products` | Giá, ảnh DB, trạng thái, size/topping. |
| 17 | Recipe Management | UC40–43 | Một phần | `/admin/recipes` | Có tạo/xem/sửa BOM; chưa có trạng thái riêng để deactivate recipe. |
| 18 | Ingredient Management | UC44–47 | Đã có | `/admin/ingredients` | CRUD và trạng thái/xóa theo quy tắc hiện tại. |
| 19 | Inventory Management | UC48–49 | Đã có | `/admin/inventory` | Xem tồn/cảnh báo/lịch sử và điều chỉnh kho. |
| 20 | Restock Request | UC50–51 | Một phần | `/admin/restock-requests` | Luồng có thật nhưng hiện giới hạn Admin; SRS ghi Cashier tạo/xem. |
| 21 | Supplier Management | UC52 | Đã có | `/admin/suppliers` | Tạo/sửa/xem/vô hiệu hóa. |
| 22 | Purchase Order | UC53 | Đã có chung màn | `/admin/restock-requests` | DRAFT → ORDERED/REJECTED → RECEIVED; nhận hàng tăng kho. |
| 23 | Employee Management | UC54 | Đã có | `/admin/employees` | Tài khoản, vị trí, lương, trạng thái. |
| 24 | Work Shift Management | UC55 | Đã có | `/admin/shifts` | Tạo/sửa/vô hiệu hóa khung ca. |
| 25 | Employee Schedule | UC56–57 | Một phần | `/admin/schedules` | Admin phân ca có thật; thư mục giao diện nguồn không có màn JSP riêng để Staff tự xem. |
| 26 | Attendance | UC58–59 | Một phần | `/admin/attendance` | Admin ghi/xem có thật; thư mục giao diện nguồn không có màn JSP lịch sử cá nhân. |
| 27 | Shift Handover | UC60 | Đã có | `/cashier/handover` | Mở ca/chốt ca và lưu bàn giao Cashier. |
| 28 | Customer Management | UC61 | Một phần | `/admin/customers`, tìm khách ở `/cashier` | Admin quản lý có thật; Cashier chỉ tìm và gắn khách vào đơn. |
| 29 | Phone Loyalty | UC62–64 | Một phần + UI mẫu | cộng/đổi điểm trong order; `/admin-loyalty.jsp` | Có giao dịch điểm; giao diện quản trị lấy từ `admin_loyalty.jsp`, chưa nối backend. |
| 30 | Feedback & Complaints | UC65 | Một phần + UI mẫu | `/feedback`, `/admin-feedback.jsp` | Khách gửi được; giao diện Admin dựa trên `AdminComplaintsPage.tsx`, chưa nối backend. |
| 31 | Reports Dashboard | UC66 | UI mẫu | `/admin-reports.jsp` | Giao diện lấy từ `admin_dashboard.jsp`; số liệu đang là dữ liệu trình bày mẫu. |
| 32 | Admin Order Management | UC67 | Đã có | `/admin/orders` | Danh sách, lọc, chi tiết và cập nhật trạng thái theo code hiện tại. |
| 33 | Shop Settings | UC68 | Đã có | `/admin/settings` | Payment methods, tỷ lệ điểm, phí giao hàng và cấu hình cửa hàng. |

## Swimlane chuẩn — UC11 Place Online Order

| Bước | Customer | Web/UI | Controller/Service | Database / Payment |
|---:|---|---|---|---|
| 1 | Chọn món, size, topping, số lượng | Menu/Product Detail gửi yêu cầu thêm | `CartController` đọc product từ DB, chuẩn hóa lựa chọn và tính giá server-side | Đọc `products.variants_config`; giỏ nằm trong HTTP session |
| 2 | Mở giỏ hàng | Hiển thị từng dòng, tạm tính, điểm có thể đổi | `CartService.refreshCart` tải lại giá và tình trạng bán | Đọc product, shop settings và điểm Customer |
| 3 | Chọn dùng điểm (nếu có) | Kiểm tra giới hạn và cập nhật tổng tiền | Server chuẩn hóa số điểm được dùng | Chưa tạo order, chưa trừ điểm |
| 4 | Bấm thanh toán | Nếu chưa đăng nhập thì chuyển Login; tài khoản không phải CUSTOMER bị từ chối | `CheckoutController` kiểm tra session, role, cart không rỗng | Không ghi DB khi validation thất bại |
| 5 | Nhập người nhận, SĐT, địa chỉ, ghi chú | Form kiểm tra trường bắt buộc | Controller kiểm tra lại dữ liệu và refresh cart lần cuối | Đọc cấu hình phí giao hàng/tỷ lệ điểm |
| 6 | Xác nhận đơn | Gửi POST `/checkout` | Tạo order ONLINE và order items trong transaction | Ghi `orders`, `order_items`; điểm chỉ xử lý theo quy tắc transaction |
| 7 | Chọn phương thức thanh toán | Điều hướng `/payment?orderId=...` | `PaymentController` kiểm tra chủ sở hữu và trạng thái order | Tạo/cập nhật payment tương ứng |
| 8A | Thanh toán thành công | Hiển thị kết quả, cho xem hóa đơn/trạng thái | Server chuyển order sang trạng thái được phép chuẩn bị | Ghi payment thành công và trạng thái order |
| 8B | Thanh toán thất bại | Hiển thị lỗi, cho thử lại | Không đưa đơn vào pha chế | Payment/order giữ trạng thái phù hợp để retry/cancel |
| 9 | Theo dõi đơn | Trang `/orders` polling/xem chi tiết | `OrdersController` chỉ lấy đơn thuộc Customer | Đọc order, items, payment và trạng thái |
| 10 | Hủy nếu còn đủ điều kiện | Nút hủy chỉ hiện ở trạng thái cho phép | Server kiểm tra lại trạng thái trước khi hủy | Cập nhật CANCELLED trong transaction; hoàn điểm nếu quy tắc yêu cầu |

## Quy tắc trạng thái dùng cho các swimlane đơn hàng

1. UI chỉ đề nghị hành động; Controller luôn kiểm tra lại quyền, chủ sở hữu và trạng thái.
2. Giá bán, phụ thu size/topping, điểm và tổng thanh toán phải tính lại ở server.
3. Luồng pha chế chỉ nhận đơn đã đáp ứng điều kiện thanh toán.
4. Chuyển trạng thái phải đi theo thứ tự: xác nhận/thanh toán → chờ pha → đang pha → sẵn sàng → giao/hoàn tất.
5. Online order có nhánh giao hàng; DINE_IN/TAKEAWAY kết thúc tại quầy.
6. Mọi nhánh lỗi phải không tạo bản ghi dở dang; các thay đổi nhiều bảng cần transaction.

## Cách dùng các màn UI mẫu

Các màn UI mẫu chỉ phục vụ đối chiếu tài liệu và dựng swimlane. Nút/form đã được chặn submit hoặc chỉ thay đổi giao diện tại trình duyệt, không ghi database. Khi triển khai backend, giữ nguyên tên màn hình/use case để sơ đồ không phải đổi tên.
