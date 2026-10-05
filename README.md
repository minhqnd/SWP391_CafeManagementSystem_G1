# Café Management System (SWP391)

Khung dự án Java Web chuẩn Maven dành cho môn học SWP391.

## 🛠 Yêu cầu môi trường
- **JDK**: Java Development Kit 17 (JDK 17)
- **Web Server**: Apache Tomcat 10.1.x (Jakarta EE 10)
- **IDE**: Apache NetBeans IDE 17
- **Database**: MySQL 8.x

---

## Danh sách tài khoản đăng nhập thử nghiệm (Demo Accounts)

Mật khẩu mặc định cho tất cả tài khoản bên dưới: `123456` (hoặc `1234`).  
Hệ thống cho phép đăng nhập bằng **Email**, **Số điện thoại**, hoặc **Tên đăng nhập** (tiền tố trước `@`).

| Vai trò (Role) | Vị trí (Position) | Họ và tên | Email đăng nhập | Số điện thoại | Đường dẫn truy cập chính |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ADMIN** | Quản lý (Manager) | Aura Admin | `admin@auracafe.vn` | `0900000001` | `/admin-dashboard.jsp` |
| **STAFF** | Thu ngân (Cashier) | Tran Thi Binh | `binh@auracafe.vn` | `0900000003` | `/cashier` (POS bán hàng tại quầy) |
| **STAFF** | Pha chế (Barista) | Nguyen Van An | `an@auracafe.vn` | `0900000002` | `/barista` (Màn hình KDS pha chế) |
| **STAFF** | Giao hàng (Shipper) | Hoang Van Nam | `nam@auracafe.vn` | `0900000006` | `/delivery-dashboard.jsp` |
| **CUSTOMER** | Hội viên Bạc (Silver) | Le Minh Anh | `anh@gmail.com` | `0900000004` | `/` (Trang chủ, 120 điểm tích lũy) |
| **CUSTOMER** | Hội viên Đồng (Bronze) | Pham Quang Huy | `huy@gmail.com` | `0900000005` | `/` (Trang chủ, 50 điểm tích lũy) |

### Đường dẫn truy cập nhanh (chạy qua ./run.sh):
- Trang chủ: `http://localhost:8080/SWP391_CafeManagementSystem/`
- Màn hình Thu ngân POS: `http://localhost:8080/SWP391_CafeManagementSystem/cashier`
- Màn hình Barista KDS: `http://localhost:8080/SWP391_CafeManagementSystem/barista`

---

## 🏛 Sơ đồ kiến trúc & Cấu trúc thư mục

Cấu trúc thư mục được khởi tạo đồng bộ 100% với sơ đồ kiến trúc:

```
SWP391_CafeManagementSystem_G1/
├── pom.xml                                   # Quản lý dependencies (Jakarta Servlet 6.0, JSP, JSTL, MySQL)
├── database/
│   └── aura_cafe.sql                         # File SQL duy nhất: Tạo DB, bảng và nạp dữ liệu mẫu
└── src/
    └── main/
        ├── java/
        │   ├── context/                      # Kết nối JDBC MySQL (DBContext.java)
        │   ├── dao/                          # Các lớp DAO truy vấn cơ sở dữ liệu
        │   ├── model/                        # Các lớp POJO Entity
        │   ├── service/                      # Xử lý logic dịch vụ
        │   └── controller/                   # Các Servlet điều hướng MVC
        └── webapp/                           # Giao diện JSP, CSS, JS
```

---

## 💾 Hướng dẫn thiết lập Cơ sở dữ liệu (Database Setup)

Toàn bộ cấu trúc bảng, khóa ngoại, ràng buộc và dữ liệu mẫu (12 món menu mới, 6 danh mục, topping/size variants JSON, voucher, quà tặng điểm thưởng, tài khoản phân quyền) đã được hợp nhất vào **duy nhất một file**:
`database/aura_cafe.sql`

Thành viên mới hoặc khi muốn reset lại toàn bộ DB chỉ cần làm theo 1 trong 2 cách sau:

### Cách 1: Chạy lệnh trực tiếp qua Terminal (Khuyên dùng)
```bash
# Thay 'root' và nhập mật khẩu MySQL của bạn khi được nhắc:
mysql -u root -p < database/aura_cafe.sql
```
*(Nếu dùng script run.sh mặc định với user root mật khẩu 1234: `mysql -u root -p1234 < database/aura_cafe.sql`)*

### Cách 2: Chạy qua MySQL Workbench / DBeaver / Navicat / NetBeans
1. Mở công cụ quản trị MySQL (ví dụ MySQL Workbench hoặc DBeaver).
2. Mở file `database/aura_cafe.sql` (`File` -> `Open SQL Script...`).
3. Bấm **Execute** (hình tia sét) để chạy toàn bộ file. Script đã tích hợp sẵn lệnh `CREATE DATABASE IF NOT EXISTS aura_cafe_erp;` và `USE aura_cafe_erp;`.

### ⚠️ Lưu ý quan trọng về kết nối DB:
Kiểm tra cấu hình trong file `src/main/java/context/DBContext.java`:
- `PORT`: mặc định `3306`
- `DB_NAME`: mặc định `aura_cafe_erp`
- `USER`: tài khoản MySQL của bạn (mặc định `root`)
- `PASSWORD`: mật khẩu MySQL trên máy của bạn (nếu máy bạn dùng mật khẩu khác `1234`, hãy đổi lại cho khớp).

---

## 🚀 Hướng dẫn mở dự án trên Apache NetBeans 17

1. **Mở NetBeans 17**: Chọn `File` -> `Open Project...`
2. Trỏ đến thư mục `SWP391_CafeManagementSystem_G1`. NetBeans sẽ nhận diện biểu tượng dự án Maven tự động.
3. **Cấu hình Server**:
   - Chuột phải vào Project -> chọn **Properties**.
   - Chọn mục **Run**.
   - Tại mục **Server**, chọn **Apache Tomcat 10.1.x** (nếu chưa có, vào tab `Services` -> `Servers` -> `Add Server` -> chọn Tomcat 10.1.x).
   - Kiểm tra **Java Platform**: chọn **JDK 17**.
4. **Chạy ứng dụng**:
   - Chuột phải vào Project -> **Run** (hoặc bấm phím `F6`).
   - Hoặc chạy nhanh bằng script terminal: `./run.sh`
   - Tomcat sẽ khởi động và mở ứng dụng tại: `http://localhost:8080/SWP391_CafeManagementSystem/`

