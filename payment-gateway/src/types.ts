// Định nghĩa các biến môi trường và tài nguyên Cloudflare cung cấp cho Worker
export type Bindings = {
    // Cơ sở dữ liệu SQLite D1 dùng để lưu thông tin đơn hàng và nhật ký thanh toán
    DB: D1Database;

    // Cờ bật/tắt tính năng giả lập thanh toán nhanh khi kiểm thử (true / false)
    ALLOW_TEST_SIMULATION?: string;
};
