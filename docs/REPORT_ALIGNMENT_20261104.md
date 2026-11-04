# Đồng bộ báo cáo SRS và SDS ngày 04 11 2026

Nhánh thực hiện: `feature/counter-order-list`. SRS/SDS Word được cập nhật tại `G1/Report doc/`; thư mục G1 tiếp tục được ignore theo yêu cầu, nên các file Word chưa nằm trong Git.

## Phạm vi

- BF-08, UC50–53: Barista tạo/sửa/gửi đề xuất, Admin sửa có lý do và lịch sử, duyệt/yêu cầu sửa/từ chối, không tự duyệt.
- Tách đơn mua khỏi phiếu kiểm nhận. Chỉ cộng lượng đạt sau duyệt; giao thiếu/giao bù, hàng thừa phải xác nhận, đóng phần thiếu; sau duyệt dùng điều chỉnh/trả hàng có dấu và giữ phiếu gốc.
- Sơ đồ luồng nhập hàng, lớp và quan hệ receipt/audit; quy tắc số lượng, trạng thái, quyền, transaction và chống gửi trùng.
- UC20: ACTIVE Cashier/Barista, đơn tại quầy qua mọi thu ngân, ngày/loại/trạng thái/từ khóa, bộ đếm và tự làm mới hôm nay.
- Đăng nhập bằng số điện thoại hoặc email đầy đủ; không dùng email prefix.
- Giới hạn giữ nguyên: form mua mới/sửa chỉ một nguyên liệu; đơn nhiều dòng cũ vẫn kiểm nhận theo dòng; chưa có công nợ/hoàn tiền/điều chỉnh giá nhà cung cấp.

Schema canonical hiện có 28 bảng. Báo cáo cũ còn có thiết kế `delivery_areas` ngoài schema và source hiện tại; ghi rõ ranh giới này thay vì mô tả thành bảng đã triển khai.

Không tự đánh PASS cho manual System/Integration Test, thay đổi tracking, slide hoặc điền trải nghiệm cá nhân vào AI Usage Report. Các tài liệu này cần kết quả chạy và minh chứng thật.
