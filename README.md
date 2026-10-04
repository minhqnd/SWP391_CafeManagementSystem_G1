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
│   └── schema.sql                            # Khởi tạo Database MySQL
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── cafe/
        │           ├── context/              # [Data Layer] Context
        │           │   └── DBContext.java    # Kết nối JDBC MySQL
        │           ├── dao/                  # [Data Layer] DAO
        │           │   ├── BaseDAO.java
        │           │   ├── UserDAO.java
        │           │   ├── ProductDAO.java
        │           │   └── OrderDAO.java
        │           ├── model/                # [Model Layer] POJO classes
        │           │   ├── User.java
        │           │   ├── Role.java
        │           │   ├── Product.java
        │           │   ├── Order.java
        │           │   ├── OrderDetail.java
        │           │   └── Payment.java
        │           ├── service/
        │           │   └── payment/          # [External Services]
        │           │       └── PaymentService.java
        │           └── controller/           # [Application / Business Layer]
        │               ├── AuthController.java
        │               ├── CustomerController.java
        │               ├── StaffController.java
        │               ├── BaristaController.java
        │               ├── DeliveryController.java
        │               └── AdminController.java
        └── webapp/                           # [Presentation Layer]
            ├── index.jsp                     # Trang chủ / Welcome
            ├── WEB-INF/
            │   ├── web.xml                   # Cấu hình Web Servlet 6.0
            │   └── views/                    # Chứa các file JSP theo module
            └── assets/
                ├── css/                      # Chứa Bootstrap / Custom CSS
                └── js/                       # Chứa JavaScript
```

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
   - Tomcat sẽ khởi động và mở trình duyệt tại địa chỉ: `http://localhost:8080/SWP391_CafeManagementSystem` (hoặc context path được cấu hình).
