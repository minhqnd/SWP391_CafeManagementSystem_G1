# Cafe Management System (SWP391 - Group 1)

He thong Quan ly Quan Ca phe Aura Cafe duoc xay dung tren nen tang Java Web Servlet/JSP theo mo hinh MVC, su dung co che quan ly thu vien bang Apache Maven.

---

## 1. Yeu cau moi truong

- **JDK**: Java Development Kit 17 (JDK 17)
- **Web Server**: Apache Tomcat 10.1.x (Jakarta EE 10 / Servlet 6.0)
- **Database**: MySQL 8.x
- **Build Tool**: Apache Maven 3.8+
- **IDE khuyen dung**: Apache NetBeans IDE 17+, IntelliJ IDEA, hoac Eclipse

---

## 2. Huong dan thiet lap Co so du lieu (Database Setup)

Toan bo cau truc co so du lieu gom 16 bang nghiep vu, rang buoc khoa ngoai, chi muc va toan bo du lieu mau (12 san pham dac san, 6 danh muc, cau hinh sizes & toppings JSON, voucher giam gia, kho qua doi diem thuong, tai khoan nhan su va khach hang) da duoc hop nhat vao **duy nhat mot file script**:

- File khoi tao: `database/aura_cafe.sql`
- Ten database mac dinh: `aura_cafe_erp`

Thanh vien moi hoac khi muon dat lai toan bo du lieu chi can thuc hien theo mot trong hai cach sau:

### Cach 1: Chay truc tiep qua Terminal / Command Prompt (Nhanh nhat)

```bash
# Nhap mat khau root MySQL khi duoc hoi:
mysql -u root -p < database/aura_cafe.sql
```

Neu su dung thiet lap mac dinh (user `root`, mat khau `1234`):
```bash
mysql -u root -p1234 < database/aura_cafe.sql
```

### Cach 2: Chay qua MySQL Workbench / DBeaver / Navicat

1. Mo ung dung quan tri co so du lieu (MySQL Workbench, DBeaver, hoac DataGrip).
2. Mo tep `database/aura_cafe.sql` (`File` -> `Open SQL Script...`).
3. Bam **Execute** (hoac to hop phim `Ctrl + Shift + Enter` / `Cmd + Shift + Enter`) de thuc thi toan bo script. Script da tu dong chua cac lenh `CREATE DATABASE IF NOT EXISTS aura_cafe_erp;` va `USE aura_cafe_erp;`.

### Luu y cau hinh ket noi Database trong du an

Kiem tra va chinh sua thong tin ket noi tai file `src/main/java/context/DBContext.java` cho phu hop voi moi truong may ca nhan:

```java
private static final String HOST = "localhost";
private static final String PORT = "3306";
private static final String DB_NAME = "aura_cafe_erp";
private static final String USER = "root";
private static final String PASSWORD = "1234"; // Thay doi thanh mat khau MySQL tren may cua ban
```

---

## 3. Danh sach tai khoan dang nhap thu nghiem (Demo Accounts)

Mat khau mac dinh cho tat ca tai khoan: `123456` (hoac `1234`).  
He thong ho tro dang nhap linh hoat bang **Email**, **So dien thoai**, hoac **Ten dang nhap** (phan tien to truoc `@`).

| Vai tro (Role) | Vi tri (Position) | Ho va ten | Email dang nhap | So dien thoai | Muc dich su dung |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ADMIN** | Quan ly (Manager) | Aura Admin | `admin@auracafe.vn` | `0900000001` | Bao cao doanh thu, quan ly thuc don, nhan su, kho |
| **STAFF** | Thu ngan (Cashier) | Tran Thi Binh | `binh@auracafe.vn` | `0900000003` | POS ban hang tai quay, tao don, ap dung voucher, thu tien |
| **STAFF** | Pha che (Barista) | Nguyen Van An | `an@auracafe.vn` | `0900000002` | Man hinh KDS pha che, tiep nhan mon, cap nhat trang thai READY |
| **STAFF** | Giao hang (Shipper) | Hoang Van Nam | `nam@auracafe.vn` | `0900000006` | Man hinh giao hang, nhan don van chuyen, hoan tat giao hang |
| **CUSTOMER** | Hoi vien Bac (Silver) | Le Minh Anh | `anh@gmail.com` | `0900000004` | Dat mon truc tuyen, tich diem (120 diem), doi qua tang voucher |
| **CUSTOMER** | Hoi vien Dong (Bronze) | Pham Quang Huy | `huy@gmail.com` | `0900000005` | Dat mon truc tuyen, tich diem (50 diem) |

---

## 4. Danh sach cac duong dan truy cap chuc nang (URLs)

Sau khi chay ung dung tai `http://localhost:8080/SWP391_CafeManagementSystem/`:

- **Khach hang (Customer)**:
  - Trang chu & San pham ban chay: `/`
  - Thuc don & Loc theo danh muc: `/menu`
  - Gio hang & Chon topping/size: `/cart`
  - Dat hang & Ap dung Voucher / Diem: `/checkout`
  - Theo doi don hang: `/orders` (hoac `/orders?orderId={id}`)
  - Cua hang doi thuong: `/rewards`
  - Thong tin ca nhan & Doi mat khau: `/profile`

- **Nhan vien & Quan ly (Internal Staff)**:
  - Man hinh Thu ngan (POS): `/cashier`
  - Man hinh Pha che (KDS): `/barista`
  - Man hinh Giao hang: `/delivery-dashboard.jsp`
  - Man hinh Quan tri tong quan: `/admin-dashboard.jsp`
  - Man hinh Khung gio ca lam viec: `/admin/shifts`
  - Man hinh Lich phan ca nhan vien: `/admin/schedules`

---

## 5. Luong van hanh don hang (Order Processing Workflow)

1. **Khoi tao don**:
   - Khach hang dat tren web (`TAKE_AWAY` / `DELIVERY`) hoac Thu ngan tao tai quay (`DINE_IN`).
   - Trang thai ban dau cua don hang: `PENDING` (Chua thanh toan) hoac `CONFIRMED` / `PREPARING` (Da thanh toan, chuyen sang bo phan pha che).
2. **KDS Pha che**:
   - Man hinh Barista hien thi danh sach cac don can pha che kem chi tiet size, topping va ghi chu.
   - Barista co the bat dau pha che don bat ky (khong bat buoc theo thu tu FIFO cung nhac).
   - Khi pha che xong, Barista an nut **San sang (READY)**. Don hang chuyen sang trang thai da san sang phuc vu.
3. **Hoan tat don**:
   - Voi don tai quan (`DINE_IN`) va mang ve (`TAKE_AWAY`): Thu ngan kiem tra don da `READY` va bam hoan tat de giao cho khach.
   - Voi don giao hang (`DELIVERY`): Nhan vien giao hang tiep nhan va xac nhan khi giao thanh cong.

---

## 6. Cau truc ma nguon du an

```
SWP391_CafeManagementSystem_G1/
├── pom.xml                                   # Quan ly thu vien Maven (Jakarta EE 10, MySQL Connector, Gson, JSTL)
├── run.sh                                    # Script khoi chay tu dong (clean compile, package va deploy)
├── database/
│   ├── aura_cafe.sql                         # File SQL duy nhat: Khoi tao Schema & Data toan dien
│   ├── update_menu_data.sql                  # Script mau cap nhat thuc don
│   └── update_rewards.sql                    # Script mau cap nhat he thong diem thuong
└── src/
    └── main/
        ├── java/
        │   ├── context/                      # Quan ly ket noi JDBC (DBContext.java)
        │   ├── dao/                          # Data Access Objects (UserDAO, ProductDAO, OrderDAO, RewardDAO, v.v.)
        │   ├── model/                        # Entity POJO (User, Product, Order, OrderDetail, Voucher, v.v.)
        │   ├── service/                      # Logic nghiep vu & tich hop thanh toan
        │   └── controller/                   # Servlet Controllers (Home, Menu, Cart, Cashier, Barista, v.v.)
        └── webapp/
            ├── index.jsp                     # Trang chu ung dung
            ├── menu.jsp                      # Giao dien thuc don
            ├── cart.jsp                      # Giao dien gio hang
            ├── checkout.jsp                  # Giao dien thanh toan
            ├── orders.jsp                    # Giao dien theo doi don hang
            ├── rewards.jsp                   # Giao dien doi qua diem thuong
            ├── cashier-dashboard.jsp         # Giao dien POS Thu ngan
            ├── barista-dashboard.jsp         # Giao dien KDS Pha che
            ├── components/                   # Cac thanh phan dung chung (header, footer, modal)
            └── assets/                       # CSS, JS, hinh anh tinh
```

---

## 7. Huong dan khoi chay du an

### Cach 1: Su dung script tu dong (Khuyen dung cho macOS / Linux)

Chay file script duoc chuan bi san tai thu muc goc cua du an:
```bash
./run.sh
```
Script se tu dong:
- Kiem tra ket noi MySQL.
- Goi Maven de build file WAR (`mvn clean package -DskipTests`).
- Sao chep file WAR vao thu muc `webapps` cua Apache Tomcat 10.1.x va khoi dong may chu.
- Mo trinh duyet den `http://localhost:8080/SWP391_CafeManagementSystem/`.

### Cach 2: Chay tren Apache NetBeans 17

1. Mo NetBeans, chon `File` -> `Open Project...` va chon thu muc `SWP391_CafeManagementSystem_G1`.
2. Chuot phai vao Project -> chon `Properties` -> muc `Run`.
3. Chon Server: `Apache Tomcat 10.1.x` (neu chua co, them trong tab `Services` -> `Servers`).
4. Kiem tra Java Platform la `JDK 17`.
5. Bam chuot phai vao Project chon `Run` (hoac phim tat `F6`).
