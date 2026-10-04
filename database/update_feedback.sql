SET NAMES utf8mb4;
USE aura_cafe_erp;

CREATE TABLE IF NOT EXISTS customer_feedbacks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_code VARCHAR(30) NOT NULL UNIQUE,
    customer_id INT NULL,
    customer_name VARCHAR(120) NULL,
    customer_phone VARCHAR(20) NULL,
    order_code VARCHAR(50) NULL,
    category VARCHAR(50) NOT NULL,
    category_label VARCHAR(100) NOT NULL,
    rating INT NOT NULL DEFAULT 5,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    images TEXT NULL,
    contact_method VARCHAR(30) NOT NULL DEFAULT 'phone',
    status ENUM('PENDING', 'PROCESSING', 'RESOLVED', 'REJECTED') NOT NULL DEFAULT 'PROCESSING',
    status_label VARCHAR(50) NOT NULL DEFAULT 'Đã tiếp nhận',
    progress_percent INT NOT NULL DEFAULT 25,
    agent_name VARCHAR(100) NULL,
    agent_note TEXT NULL,
    resolution TEXT NULL,
    gift_voucher VARCHAR(50) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_feedback_cust (customer_id),
    INDEX idx_feedback_ticket (ticket_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chèn dữ liệu mẫu chuẩn theo link tham khảo
INSERT INTO customer_feedbacks 
(ticket_code, customer_id, customer_name, customer_phone, order_code, category, category_label, rating, title, content, images, contact_method, status, status_label, progress_percent, agent_name, agent_note, resolution, gift_voucher, created_at)
VALUES
('#FB-2024-089', 1, 'Nguyễn Văn An', '0908123456', '#AUR-86410', 'wrong_size', 'Sai kích cỡ / Món', 2, 'Giao sai size ly từ L xuống M', 'Ly Bạc Xỉu Kem Trứng của tôi bị nhầm thành size M thay vì size L như đã đặt trên hóa đơn.', 'https://lh3.googleusercontent.com/aida-public/AB6AXuDmpTys3KoN-gBpIZ2NHZpRJuTwcv-5IYk5GLa4_0uVM_hhieERDkGhxBxjxrEkep1JSAnjV_4SPCgWBVKnMb6fwivTCsPDIZrULPy7mL5ADYCvY6PIwelzBqc3MDBhcvjIWD8AIfWl4SwKLbsFrj0d1ysYfNX_B5EnxoRyDrgKDFgvdcyO-Ms8_F5cd-FImpqdFCuvDZb5yW0IJIY2nfBTxVlU5v0Ss4rTwrmL4ZtFkSiyLaAxWJKC', 'phone', 'PROCESSING', 'Đang xử lý', 65, 'Thu Hà', 'Chuyên viên CSKH Thu Hà đang kiểm tra camera quầy đóng gói Chi nhánh Tràng Tiền để làm rõ sự sai lệch kích cỡ ly.', NULL, NULL, '2024-10-20 14:45:00'),
('#FB-2024-042', 1, 'Nguyễn Văn An', '0908123456', '#AUR-78219', 'packaging', 'Thiếu ống hút/đồ ăn', 3, 'Thiếu ống hút và khăn giấy', 'Đơn hàng giao thiếu ống hút và khăn giấy khi tôi nhận tại văn phòng.', '', 'chat', 'RESOLVED', 'Đã giải quyết xong', 100, 'Thanh Trúc (Trưởng ca CSKH)', 'Cửa hàng đã kiểm điểm khâu kiểm tra trước khi bàn giao cho shipper.', 'Aura chân thành xin lỗi quý khách về sự bất tiện trên. Cửa hàng đã kiểm điểm khâu kiểm tra trước khi bàn giao cho shipper và xin gửi tặng E-voucher 30.000đ trực tiếp vào ví Aura Rewards của bạn.', '#AURA-CARE-30K', '2024-10-16 10:15:00');
