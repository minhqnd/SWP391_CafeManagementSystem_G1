# UC20 — Tra cứu đơn tại quầy

Đối chiếu SRS II.3.1.2: `/counter-orders?date=yyyy-MM-dd&status=ALL&type=ALL&q=`.

- Chỉ STAFF đang ACTIVE với vị trí CASHIER hoặc BARISTA được xem; vai trò khác nhận 403. Khách chưa đăng nhập được chuyển đến đăng nhập.
- Danh sách chỉ đọc, gồm đơn DINE_IN / TAKEAWAY do các thu ngân tạo trong ngày được chọn. Mặc định là hôm nay; không giới hạn theo thu ngân đang đăng nhập.
- Lọc loại đơn và trạng thái, tìm mã đơn hoặc số bàn không phân biệt hoa thường. Bộ đếm trạng thái áp dụng ngày, loại và từ khóa hiện tại trước khi lọc trạng thái.
- Mỗi đơn hiển thị mã, thời gian, hình thức, bàn/khách, món và tùy chọn, tổng tiền, trạng thái.
- Điều hướng từ cả POS và Barista. Tab Đơn hôm nay trong POS tiếp tục dành cho đơn của thu ngân đang đăng nhập.
- Trang hôm nay làm mới mỗi 30 giây, giữ bộ lọc trong URL và tạm ngừng khi đang nhập bộ lọc. Không có thao tác thay đổi đơn tại màn tra cứu.

## Kiểm tra

`mvn -Dtest=CounterOrderListControllerTest,CounterOrderControllerTest,BaristaControllerTest test`

Test controller kiểm tra quyền, ngày, kết hợp bộ lọc, tìm mã/bàn, bảng trống và bộ đếm trạng thái. Trước khi cập nhật tracking thành Done, cần nghiệm thu trên database thực với đơn của nhiều thu ngân, ngày quá khứ và cả hai vai trò.

SRS đã mô tả route và chức năng này. Các tài liệu G1 nằm ngoài Git; chưa sửa trạng thái tracking hay báo cáo nghiệm thu trong các tệp đó.
