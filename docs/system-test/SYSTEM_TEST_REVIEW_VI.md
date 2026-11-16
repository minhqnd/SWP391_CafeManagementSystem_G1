# Kết quả rà soát System Test Aura Café

Ngày rà soát: 15/11/2026.

File đã chỉnh: `AuraCafe_System-Test_Reviewed_BasicEnglish.xlsx`.

Bản mới có **48 ca trong 15 luồng nghiệp vụ**: viết lại 32 ca gốc và bổ sung 16 ca. Giữ các sheet theo luồng, ID ca gốc và cấu trúc ba vòng kiểm thử. Nội dung ca test dùng tiếng Anh cơ bản, với các cột mô tả, bước thực hiện, kết quả mong đợi, điều kiện trước khi chạy và ghi chú yêu cầu.

Đây là rà soát nội dung và cấu trúc file, đối chiếu mã nguồn; **chưa chạy kiểm thử hệ thống**. File gốc mang tên “Executed” nhưng 32 ca gốc đều Pending, chưa có ngày chạy, người chạy hoặc ảnh. Bản mới giữ các trường thực thi gốc và để các ca mới Pending. Nhóm tự thêm ảnh vào cột Evidence sau khi chạy.

## Các lỗi trong file gốc đã sửa

| Nội dung | Vấn đề trong bản gốc | Cách sửa |
| --- | --- | --- |
| Môi trường | Ghi frontend 5173, API 5000, Node và MongoDB | Đổi ứng dụng chính thành Java 17, Servlet/JSP, Maven WAR, Tomcat 10.1, MySQL 8; đường dẫn tương đối theo URL deploy thực tế. Payment Gateway là dịch vụ phụ riêng. |
| Đăng ký và OTP | Email giữa các bước không thống nhất, có `[your Gmail]` | Dùng QA_EMAIL do tester chọn, mailbox truy cập được và thông tin đăng ký nhất quán. Không bắt buộc Gmail. |
| Nhân viên | Yêu cầu nhập lương giờ không có trong form | Bỏ trường lương; kiểm tra role STAFF, position CASHIER và trạng thái ACTIVE. |
| Ca làm việc | Mong đợi trang `/my-schedule` hoạt động đầy đủ | Kiểm tra lưu ca tại trang Admin; không coi trang lịch cá nhân dạng placeholder là chức năng hoàn chỉnh. |
| Chấm công | Tự suy ra check-in sẽ mở khóa POS và giờ làm bằng độ dài ca | Kiểm tra giờ vào/ra, trạng thái, phút đi muộn và số liệu theo các bản ghi thực tế. |
| Đề xuất nhập hàng | Cashier tạo đề xuất tại đường dẫn không có trong dự án | Dùng Barista tại `/barista/restock-requests`, sau đó Admin duyệt. |
| Nhập kho | Bấm nhận hàng là tăng kho ngay | Tách đơn mua, phiếu kiểm nhận và Admin duyệt phiếu. Kiểm tra nhận 600 + 400 g và chống cộng kho trùng. |
| Dữ liệu và đơn vị | Có tên món không được chuẩn bị, dùng kg/g tùy ý | Chuẩn hóa E2E Latte, Coffee E2E đơn vị g, Milk E2E đơn vị ml; ghi rõ dữ liệu cần tạo và giá cần thiết. |
| Giá món | Các ca dùng giá cơ bản và tổng tiền không thống nhất | E2E Latte giá gốc 45000; L +10000; Pearl +8000; L + Pearl = 63000 VND. |
| Giá vốn | Dùng “giá tồn kho” không rõ nguồn | Lấy giá nhập gần nhất đã nhận thực; ví dụ 20 g × 200 + 150 ml × 30 = 8500 VND. |
| Cài đặt | Mong thay tên quán, giờ mở cửa trong footer và bán kính giao hàng | Dùng các cài đặt có triển khai: phí giao, freeship và thời gian giao. Đánh dấu riêng yêu cầu phí theo quận còn lệch SRS. |
| Checkout | Chọn COD hoặc VietQR tại checkout | Tạo đơn PENDING tại `/checkout`, sau đó chọn hoặc xử lý thanh toán tại `/payment`. |
| Chuyển khoản online | Bấm “Tôi đã chuyển khoản” là thành công | Chỉ thành công sau callback hợp lệ, đúng số tiền, đúng chữ ký; thiếu môi trường gateway thì để Pending. |
| COD | Sau chọn COD vẫn “chờ xác nhận” hoặc coi đã thanh toán | Order CONFIRMED nhưng payment CASH/PENDING; khi giao thành công và xác nhận thu đủ tiền mới SUCCESS và EARN. |
| Điểm | Giả định 20 điểm luôn đổi 20000 và bỏ qua phí giao | Đặt quy tắc cụ thể trước test; tách tiền hàng sau giảm và phí giao; kiểm tra REDEEM, EARN, REFUND. |
| Pha chế | Dùng nút và trạng thái theo ứng dụng khác | Dùng thao tác item hiện có, chuyển READY và kiểm tra trừ kho đúng một lần. |
| Giao hàng | Luồng nhận rồi bắt đầu giao không khớp self-claim hiện tại | Dùng claim-and-deliver cho đơn READY chưa nhận; kiểm tra đơn, payment và điểm của đúng order ID. |
| Nộp COD | Có `/delivery/cod`, nhập số tiền tùy ý và Cashier duyệt | Dùng `/delivery?tab=cod`, chọn Cashier nhận; hệ thống ghi nhận toàn bộ tiền đang giữ. Không tự thêm bước duyệt chưa triển khai. |
| POS | Thu tiền xong coi order COMPLETED | Sau thanh toán là CONFIRMED; Barista làm READY; Cashier bàn giao mới COMPLETED. |
| QR tại quầy | Bấm xác nhận tiền rồi coi như đã kiểm tra tích hợp thật | Mô tả rõ test mode của POS; không dùng kết quả mô phỏng để xác nhận live gateway. |
| Bàn giao ca | Gộp trạng thái ca và trạng thái biên bản | Khi chốt: schedule COMPLETED, handover SUBMITTED; Admin duyệt: handover CONFIRMED. |
| Feedback | Có hai bộ sao chất lượng/phục vụ | Dùng một trường rating 1–5; kiểm tra PENDING, PROCESSING, RESOLVED và khóa ticket đã đóng. |
| Báo cáo | Doanh thu bằng tổng order COMPLETED, COD là phương thức riêng | Đối chiếu SUCCESS payments theo paid_at. COD được lưu là CASH. Số đơn có thanh toán là distinct order IDs. |
| Thống kê | Ghi 152 kiểm tra chi tiết và cross-flow security không tồn tại | Đổi đúng 48 ca thực tế; công thức đếm theo phạm vi ca từng sheet; ghi rõ phạm vi chưa được bao phủ. |

## Các chỗ SRS và code chưa thống nhất

Các điểm dưới đây được phát hiện khi đọc nguồn. Chưa thể ghi Passed/Failed của một lần chạy UI chỉ từ việc đọc code. Bản test ghi chú khác biệt để nhóm quyết định sửa SRS hay sửa ứng dụng theo yêu cầu đã được duyệt.

| Điểm cần chốt | SRS | Code hiện tại / ảnh hưởng đến test |
| --- | --- | --- |
| Phí giao theo quận | GB-03, UC71 yêu cầu khu vực ACTIVE và phí từng quận | `CheckoutController`, `Cart.getDeliveryFee()` dùng địa chỉ nhập và một `delivery_fee`. Test freeship không chứng minh yêu cầu phí theo quận đã đạt. |
| Phân công Shipper | BF04/GB-11 chủ yếu mô tả Admin gán, nhận, lấy hàng | `DeliveryController` có pool chờ nhận và `claim`, chuyển sang DELIVERING. Có cả nhánh start cho đơn gán sẵn. Cần cập nhật BF04 theo luồng nhóm dùng. |
| Kiểm nhận kho | Mô tả cũ có nhận kho 1-click | `PurchaseReceiptController`, `InventoryWorkflowDAO` và `INVENTORY_WORKFLOW.md` tách người kiểm nhận và Admin duyệt; chỉ cộng lượng chấp nhận khi duyệt. |
| Trạng thái đang pha | Luồng đơn SRS có PREPARING được theo dõi | `BaristaPreparationService.updateItems()` giữ các tick và startedOrders trong bộ nhớ; `getOrders()` có thể hiển thị PREPARING trên KDS mà chưa lưu trạng thái DB. `OrderDAO` có API cập nhật PREPARING riêng. Trong ST-BF01-002 vẫn giữ kỳ vọng theo SRS: cần kiểm tra thực tế Customer có thấy PREPARING hay không. |
| Độ bền tick pha chế | SRS nói giữ tiến độ trong khi đơn hoạt động | Service ghi rõ restart/redeploy sẽ xóa các tick trong bộ nhớ. Test refresh trang không chứng minh phục hồi sau restart. |
| Két đầu ca | SRS mô tả số tiền đầu ca tại lịch được phân | `CashierShiftHandoverController` có lấy actual_cash của biên bản trước; cần chốt quy tắc tiền chuyển ca so với tiền nhập tại schedule. |
| Thanh toán mô phỏng | Các đoạn trong SRS mô tả nút test mode không thống nhất | `PaymentController` đã bỏ mô phỏng online qua nút; POS vẫn có mô phỏng có chữ ký khi `payment_test_mode=true`. Hai luồng cần test riêng. |
| Live POS QR | SRS có lấy tiền tại quầy qua VietQR | POS không có lời gọi đăng ký gateway tương tự online trong `CounterOrderController`. Bản test chỉ xác nhận test mode; live POS cần một ca tích hợp riêng khi có môi trường. |

## Các nhánh đã bổ sung

16 ca mới kiểm tra: đăng ký trùng số điện thoại; ca trùng giờ; chấm công thiếu hoặc sai giờ ra; tự duyệt đề xuất và duyệt phiếu trùng; danh mục ẩn; phí giao không hợp lệ; hoàn điểm khi hủy và chống hoàn trùng; checkout sai số điện thoại; đặt lại theo giá hiện tại; callback sai chữ ký/sai số tiền/gửi lặp; thiếu nguyên liệu; COD chưa xác nhận thu đủ; bàn giao đơn quầy khi chưa READY; từ chối và gửi lại bàn giao ca; feedback trùng hoặc ticket đóng; truy cập Admin và bill sai chủ sở hữu.

Một ca có nhiều biến thể lỗi được viết theo thứ tự. Nếu một biến thể lỗi, ghi chính xác biến thể và kết quả thực tế trong Note; không đánh Passed cho cả ca khi chưa kiểm tra đủ.

## Mức độ sử dụng và phần cần làm tiếp

Bản mới có cấu trúc phù hợp cho kiểm thử thủ công theo luồng: có dữ liệu mẫu, điều kiện chạy, bước làm, kỳ vọng đo được, ca nối nhiều vai trò và tham chiếu UC. ST-BF01-002 nối toàn bộ đường đi online bằng cùng order ID; các ca kho và bàn giao cũng nối nhiều vai trò.

Tuy nhiên, chưa thể gọi đây là bộ kiểm thử đầy đủ toàn bộ SRS. Chưa bao phủ mọi UC: ví dụ UC71 chưa khớp triển khai, UC75 đối soát ngân hàng chưa có ca riêng, chưa kiểm tra toàn bộ nhánh trả hàng/nhận thừa, ca qua đêm, lỗi giao hàng, CSRF/OTP, phân quyền mọi endpoint, rollback do lỗi DB, nhiều người thao tác đồng thời, tải hệ thống hoặc tương thích nhiều trình duyệt. Các ca gửi lặp chỉ kiểm tra idempotency, không thay thế kiểm tra concurrency bằng hai phiên đồng thời.

Nhóm nên chốt các khác biệt SRS/code trước khi ghi kết luận nghiệm thu. Khi chạy: ghi ngày, người chạy, kết quả đúng vòng; thêm ảnh ở Evidence; ghi actual result và mã lỗi trong Note nếu Failed. Giữ Pending nếu thiếu dịch vụ hoặc dữ liệu cần thiết. Không ghi Passed chỉ vì code có điều kiện kiểm tra.

## Nguồn đã đối chiếu

- SRS do nhóm cung cấp: `SE2064_G1_CafeManagementSystem_SRS_01 (1).docx`, đặc biệt business flows, use cases, functional requirements, GB-01 đến GB-21 và giới hạn release.
- Workbook do nhóm cung cấp: `AuraCafe_System-Test_Executed.xlsx`; cấu trúc mẫu `Template3_System-Test (1).xlsx`.
- `pom.xml`, `README.md`, `docs/INVENTORY_WORKFLOW.md`, `docs/redesign/uml/08-delivery-self-claim-activity.puml`.
- Các controller Authentication/Profile, Checkout/Payment/Orders, CounterOrder, Barista, Delivery, AdminEmployees/Shift/Schedule/Attendance, Restock/Purchase/PurchaseReceipt, Settings, Feedback, CashierShiftHandover/AdminHandover và các DAO/service liên quan trong `src/main/java`.
- Các trang tương ứng trong `src/main/webapp`; đặc biệt không suy ra rằng footer là động nếu trường đang được ghi cố định.

Không sửa mã nguồn ứng dụng hoặc chạy reset database trong quá trình làm tài liệu này.
