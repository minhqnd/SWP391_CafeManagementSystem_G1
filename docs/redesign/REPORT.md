# Báo cáo thiết kế lại theo SRS – phần của Nhiên

> Nhánh: `feature/employee-management` (tách từ `develop` = `9cc543a`). **Chưa commit.**
> Cập nhật: 24/10/2026 (phiên 3). Dùng SRS gốc trên Drive.

## 0. Tóm tắt nhanh

| Hạng mục | Trạng thái |
|---|---|
| Khung giờ ca theo tuần (góp ý của cô) | **Đã làm + test** |
| Lịch phân ca / Chấm công dùng ca theo tuần | **Đã sửa + test** |
| Bàn giao ca có Admin duyệt (SRS 2.5 / UC60, bảng `shift_handovers`) | **Đã làm + test** (làm thay phần của Đôn để trọn luồng) |
| Đặt hàng: 3 hình thức nhận, địa chỉ tách, hẹn giờ (SRS 2.3.2, 2.3.3) | **Đã làm + test** |
| Nhãn chức vụ `MANAGER` | Đổi thành **"Quản lý nhân viên"** |
| UML (PlantUML) cho từng luồng | **Đã có** 7 file trong `uml/` (không xuất ảnh, tự vẽ lại diagram) |
| Thanh toán xác nhận qua webhook VnPay/SePay (SRS API-02/03) | **Để sau** – tích hợp sau |

Kiểm tra: `mvn package` thành công, **76/76 test pass**. Kiểm thử trên trình duyệt (Edge headless): 16/16 bước Khung giờ ca, 8/8 bước Đặt hàng, **16/16 bước Bàn giao ca**.

## 1. Khung giờ ca theo tuần (UC55) – theo góp ý của cô

| Góp ý | Đã làm |
|---|---|
| Có dấu **+** để thêm ca | Nút "+ Thêm ca" mở form (tên ca, giờ bắt đầu, giờ kết thúc). Thêm / sửa / xoá được ca. |
| Sửa giờ ca tuần này **không ảnh hưởng tuần trước**, lưu DB theo từng tuần | Bảng mới `work_shifts(week_start, shift_name, start_time, end_time)`, khoá `(week_start, shift_name)`. Mọi thao tác chỉ đụng tới tuần đang chọn. Tuần đã qua chỉ xem. Tuần trống có nút "Sao chép ca từ tuần gần nhất". |
| Không cần đếm có mặt / hoàn thành | Bỏ hết thống kê khỏi màn Khung giờ ca. |

Quy tắc kiểm tra ở server: tên ca bắt buộc (≤ 50 ký tự), không trùng tên trong tuần, giờ đúng HH:mm, giờ kết thúc sau giờ bắt đầu, **không chồng giờ** với ca khác cùng tuần, không xoá ca đã có nhân viên được xếp lịch, có CSRF token.

Ảnh hưởng sang màn khác (đã sửa):
- **Lịch phân ca**: hiện đúng các ca của tuần đang xem (không còn cố định 3 ca); chọn ca theo `shift_id`; ca phải thuộc tuần của ngày làm việc; tuần chưa có ca thì cảnh báo + link tạo ca. Sửa luôn lỗi tiêu đề "Tuần - " bị trống.
- **Chấm công**: tính đi muộn theo giờ bắt đầu ca của **đúng tuần đó**.
- Bỏ `shift_morning / shift_afternoon / shift_evening` khỏi `shop_settings`, bỏ model `ShiftStat`.

## 2. Bàn giao ca có Admin duyệt (SRS 2.5 / UC60)

| Bước | Đã làm |
|---|---|
| Thu ngân chốt ca | Màn `/cashier/handover`: hệ thống tính tiền két = tiền đầu ca + tiền mặt bán trong ca; Thu ngân nhập tiền thực tế + ghi chú. **Lệch két thì bắt buộc ghi chú** (kiểm tra cả trình duyệt và server). |
| Lập biên bản | Trong **1 transaction**: `work_schedules` → `COMPLETED` (chỉ khi ca còn `PRESENT` và đúng Thu ngân) + thêm dòng `shift_handovers` trạng thái `SUBMITTED`. Gửi 2 lần không tạo 2 biên bản. |
| Admin xem lại | Màn mới **`/admin/handovers`** (sidebar "Nhân sự & ca làm" → "Bàn giao ca", có card trên Tổng quan): lọc Chờ duyệt / Đã xác nhận / Bị từ chối / Tất cả; bảng tiền đầu ca, tiền mặt bán, hệ thống, thực tế, chênh lệch (Khớp/Thừa/Thiếu), ghi chú. |
| Xác nhận / Từ chối | Xác nhận (ý kiến không bắt buộc) → `CONFIRMED`; Từ chối **bắt buộc lý do** → `REJECTED`. Lưu người duyệt + thời gian. Chỉ duyệt được biên bản đang chờ (chống 2 admin duyệt cùng lúc). Có CSRF token. |
| Thu ngân giải trình | Biên bản bị từ chối hiện lý do + ô giải trình → "Gửi lại" → về `SUBMITTED`. |

Phân quyền: chỉ Thu ngân (STAFF + CASHIER + ACTIVE) vào màn chốt ca; chỉ ADMIN vào `/admin/handovers` (Thu ngân mở → 403).

## 3. Giỏ hàng & Đặt hàng (UC10, UC11) – theo SRS 2.3.2, 2.3.3

| SRS | Trước | Đã làm |
|---|---|---|
| 2.3.2 Chọn hình thức: Giao tận nơi / Tự đến lấy / Dùng tại quán | Chỉ giao tận nơi | 3 thẻ chọn; `order_type` = `ONLINE` / `TAKEAWAY` / `DINE_IN`. Phí giao **chỉ tính khi giao tận nơi** (vẫn miễn phí khi đạt mức freeship). Tổng tiền đổi ngay khi đổi hình thức. |
| 2.3.3 Tỉnh / Quận / Phường / Địa chỉ chi tiết | 1 ô địa chỉ | Tỉnh = Hà Nội, Quận/Huyện chọn từ 12 quận nội thành, Phường/Xã, địa chỉ chi tiết → ghép vào `delivery_address`. |
| 2.3.3 Giao ngay / Hẹn giờ | Không có | "Càng sớm càng tốt" hoặc "Hẹn giờ hôm nay" (07:00 – 21:30, sau hiện tại ≥ 30 phút) → cột mới `orders.scheduled_at`. |
| Dùng tại quán | – | Ô số bàn (không bắt buộc) → `orders.table_number`. |

Trang Thanh toán hiện thêm "Hình thức nhận" và "Thời gian nhận". Thanh toán qua webhook để tích hợp sau (hiện khách tự bấm "Tôi đã chuyển khoản").

## 4. Thay đổi database (đã gộp vào `database/aura_cafe.sql` – chạy lại 1 lần)

- **Mới** `work_shifts` + dữ liệu mẫu: tuần 28/09, 19/10, 26/10 (tuần 26/10 Ca chiều 12:30-17:30 để minh hoạ giờ ca riêng theo tuần).
- **Sửa** `work_schedules`: `shift_name ENUM(...)` → `shift_id INT FK work_shifts(id)`, UNIQUE `(user_id, work_date, shift_id)`.
- **Mới** `shift_handovers`: `schedule_id` (UNIQUE, FK, xoá lịch thì xoá biên bản), `cashier_id`, `opening_cash`, `cash_sales`, `expected_cash`, `actual_cash`, `difference`, `note`, `status` SUBMITTED/CONFIRMED/REJECTED, `reviewed_by`, `review_note`, `created_at`, `reviewed_at`.
- **Sửa** `orders`: thêm `scheduled_at DATETIME NULL`.
- **Bỏ** 3 dòng `shift_morning/afternoon/evening` trong `shop_settings`.
- Script đã chạy lại từ đầu: không lỗi. Tổng **20 bảng**.

## 5. File đã tạo / sửa

| Loại | File |
|---|---|
| Mới | `model/WorkShift.java`, `dao/WorkShiftDAO.java`, `model/ShiftHandover.java`, `dao/ShiftHandoverDAO.java`, `controller/AdminHandoverController.java`, `admin-handovers.jsp`, `assets/css/admin-handovers.css`, `docs/redesign/**` |
| Viết lại | `controller/AdminShiftController.java`, `admin-shifts.jsp`, `assets/css/admin-shifts.css`, `test/dao/WorkScheduleDAOTest.java` |
| Sửa | `dao/WorkScheduleDAO.java` (bỏ `closeCashierShift`, chuyển sang `ShiftHandoverDAO`), `model/WorkSchedule.java`, `model/User.java`, `controller/AdminScheduleController.java`, `admin-schedules.jsp`, `admin-employees.jsp`, `controller/AdminAttendanceController.java`, `controller/CashierShiftHandoverController.java`, `cashier-shift-handover.jsp`, `assets/css/cashier-shift-handover.css`, `components/admin-sidebar.jsp`, `dao/ShopSettingDAO.java`, `model/ShopSettings.java`, `controller/CheckoutController.java`, `model/Cart.java`, `model/Order.java`, `dao/OrderDAO.java`, `checkout.jsp`, `payment.jsp`, `assets/css/cart.css`, `admin-dashboard.jsp`, `test/model/CartTest.java`, `database/aura_cafe.sql` |
| Xoá | `model/ShiftStat.java` |

Lưu ý: Lịch phân ca, Chấm công, Bàn giao ca vốn là màn của **DonVu0112** – báo Đôn trước khi merge để tránh xung đột.

## 6. UML cho từng luồng (`docs/redesign/uml/*.puml`, không xuất ảnh)

| File | Diagram | Luồng |
|---|---|---|
| 01 | Activity (swimlane) | Thêm vào giỏ + Đặt hàng + Thanh toán |
| 02 | Sequence | Như trên, đúng tên class/hàm |
| 03 | Activity | Khung giờ ca theo tuần |
| 04 | Sequence | Thêm / sửa / xoá / sao chép ca |
| 05 | Activity | Bàn giao ca: chốt ca → Admin xác nhận / từ chối → gửi lại |
| 06 | ERD | `users`, `work_shifts`, `work_schedules`, `shift_handovers` |
| 07 | Sequence | Bàn giao ca, đúng tên class/hàm |

Tất cả khớp code hiện tại; đã kiểm tra cú pháp bằng PlantUML 1.2024.6 (`-checkonly`). Mở bằng plantuml.com hoặc extension PlantUML trong IntelliJ để vẽ lại.

## 7. Còn lại

1. **Thanh toán webhook** (SRS API-02/03): tích hợp sau, cần tài khoản SePay/VnPay sandbox.
2. **Môi trường**: thư mục Tomcat trong `Downloads` đã bị xoá → IntelliJ (Run config "Tomcat 10.1.59") đang trỏ vào thư mục không còn → cần cài lại Tomcat 10.1 và chọn lại trong IntelliJ.
3. Chạy lại `database/aura_cafe.sql` trước khi test (có bảng mới `shift_handovers`).
