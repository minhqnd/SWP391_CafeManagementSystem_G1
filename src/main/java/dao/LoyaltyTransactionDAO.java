package dao;

import model.LoyaltyTransaction;
import model.User;
import utils.LoyaltyValidation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoyaltyTransactionDAO extends BaseDAO {

    public List<LoyaltyTransaction> findByCustomer(int customerId) throws SQLException {
        return findDetailed("WHERE lt.customer_id = ?", customerId, 100);
    }

    // [TDM03]
    // . Trong hàm findRecent của LoyaltyTransactionDAO.java, em nhận giới hạn 50 từ
    //   AdminLoyaltyController.doGet. Em gọi findDetailed ngay trong file để đọc 50 giao dịch gần nhất,
    //   rồi trả danh sách về controller để hiện bảng lịch sử chung.
    // ... Đọc tiếp ngay trong LoyaltyTransactionDAO.java, tìm [TDLS02] ở hàm findDetailed để xem cách đọc
    //     các dòng lịch sử.
    public List<LoyaltyTransaction> findRecent(int limit) throws SQLException {

        return findDetailed("", null, limit);
    }

    // [TDLS02]
    // . Để lấy lịch sử, AdminLoyaltyController.doGet gọi findRecent, rồi findRecent gọi findDetailed trong
    //   LoyaltyTransactionDAO.java. Em đọc giao dịch từ loyalty_transactions, lấy thêm tên và số điện
    //   thoại trong users cùng mã đơn trong orders. Danh sách được xếp mới nhất trước rồi trả qua
    //   findRecent về doGet để hiện bảng lịch sử chung.
    // ... Danh sách qua findRecent quay về AdminLoyaltyController.doGet; đọc kết quả tại
    //     admin-loyalty.jsp, tìm [TDLS01].
    private List<LoyaltyTransaction> findDetailed(String whereClause, Integer customerId, int limit)
            throws SQLException {

        // [SQL12-01] Bảng lịch sử điểm: ghép khách, giao dịch và mã đơn, mới nhất trước.
        // . Trang quản trị gọi findRecent(50): không lọc một khách, dấu ? là giới hạn 50 dòng.
        //   findByCustomer hỗ trợ đọc riêng một khách nhưng hiện chưa có controller gọi hàm đó.
        String sql = "SELECT lt.id, lt.customer_id, lt.order_id, lt.points_change, "
                + "lt.transaction_type, lt.created_at, u.full_name, u.phone, u.total_points, o.order_code "
                + "FROM loyalty_transactions lt JOIN users u ON u.id = lt.customer_id "
                + "LEFT JOIN orders o ON o.id = lt.order_id "
                + whereClause + " ORDER BY lt.created_at DESC, lt.id DESC LIMIT ?";

        List<LoyaltyTransaction> rows = new ArrayList<>();
        Map<Integer, Integer> runningBalance = new HashMap<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            if (customerId != null) ps.setInt(index++, customerId);
            ps.setInt(index, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int ownerId = rs.getInt("customer_id");
                    // [TDSAU02]
                    // . Với mỗi khách, em bắt đầu từ điểm hiện tại trong users.total_points để điền số dư sau giao dịch
                    //   mới nhất. Sau đó, em trừ pointsChange của dòng vừa đọc để có số dư của dòng cũ hơn, giữ riêng kết
                    //   quả cho từng khách. Ví dụ cộng 10 xong còn 100 thì trước lần đó là 90 điểm.
                    // ... findDetailed thêm giao dịch đã tính balanceAfter vào danh sách rồi trả qua findRecent về doGet.
                    //     Xem bảng tại admin-loyalty.jsp, tìm mã TDLS01, cột Số dư sau; hiện xong là kết thúc lần tính
                    //     này.

                    // [SQL12-15] Cột Số dư sau: Java tính ngược từ số dư hiện tại theo lịch sử mới tới cũ.
                    // . SQL không có cột balance_after; Java giữ số dư riêng từng khách rồi trừ points_change
                    //   để đi về giao dịch cũ.
                    int balanceAfter = runningBalance.computeIfAbsent(ownerId,
                            ignored -> getIntQuietly(rs, "total_points"));
                    LoyaltyTransaction row = new LoyaltyTransaction(
                            rs.getInt("id"), ownerId, rs.getInt("order_id"),
                            rs.getInt("points_change"), rs.getString("transaction_type"),
                            rs.getTimestamp("created_at"));
                    row.setCustomerName(rs.getString("full_name"));
                    row.setCustomerPhone(rs.getString("phone"));
                    row.setOrderCode(rs.getString("order_code"));

                    row.setBalanceAfter(balanceAfter);
                    runningBalance.put(ownerId, balanceAfter - row.getPointsChange());
                    rows.add(row);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
        return rows;
    }

    private int getIntQuietly(ResultSet rs, String column) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            return 0;
        }
    }

    // [TDTS02]
    // . Để cung cấp số liệu cho AdminLoyaltyController.doGet, các hàm trong LoyaltyTransactionDAO.java lần
    //   lượt đếm khách có điểm, cộng tổng điểm và đếm giao dịch. Em trả các số về controller qua
    //   countCustomersWithPoints, getTotalPoints và countTransactions để hiện thẻ tổng quan.
    // ... Trả từng số thống kê về AdminLoyaltyController.doGet sau lời gọi đếm. Controller tính tiền quy
    //     đổi rồi đưa lên bốn thẻ; xem kết quả tại admin-loyalty.jsp, tìm [TDM04].
    public long countCustomersWithPoints() throws SQLException {

        // [SQL12-02] Thẻ Khách hàng có điểm: đếm CUSTOMER có total_points lớn hơn 0.
        return queryLong("SELECT COUNT(*) FROM users WHERE role='CUSTOMER' AND total_points > 0");
    }

    public long getTotalPoints() throws SQLException {

        // [SQL12-03] Thẻ Tổng điểm lưu hành: cộng số dư hiện tại của các CUSTOMER.
        // . Không cộng lại points_change của lịch sử; COALESCE trả 0 khi không có điểm để cộng.
        return queryLong("SELECT COALESCE(SUM(total_points),0) FROM users WHERE role='CUSTOMER'");
    }

    public long countTransactions() throws SQLException {

        // [SQL12-04] Thẻ Giao dịch điểm: đếm toàn bộ dòng lịch sử điểm.
        return queryLong("SELECT COUNT(*) FROM loyalty_transactions");
    }

    private long queryLong(String sql) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
    }

    // [TDTK03]
    // . Trong LoyaltyTransactionDAO.findCustomerByPhone, em nhận số điện thoại đã kiểm tra và tìm tài
    //   khoản CUSTOMER tương ứng trong users. Có khách thì trả User về doGet; không có thì trả null để
    //   controller báo không tìm thấy.
    // ... Đọc tiếp tại AdminLoyaltyController.java, tìm [TDTK04].
    public User findCustomerByPhone(String phone) throws SQLException {

        // [SQL12-05] Nút Tìm kiếm theo số điện thoại: đọc thông tin và điểm của CUSTOMER.
        // . Dấu ? từ trái sang phải: 1=số điện thoại đã chuẩn hóa.
        String sql = "SELECT id, full_name, phone, email, role, position, status, total_points "
                + "FROM users WHERE role='CUSTOMER' AND phone=? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizePhone(phone));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapCustomer(rs) : null;
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
    }

    // [TDMD01]
    // . Khi người dùng chưa nhập số điện thoại, AdminLoyaltyController.doGet gọi findDefaultCustomer trong
    //   LoyaltyTransactionDAO.java. Em chọn khách có điểm cao nhất và trả về controller để trang có thông
    //   tin ban đầu trước khi người dùng tra cứu khách khác.
    // ... Trả khách mặc định về AdminLoyaltyController.doGet, chỉ khi chưa gửi phone. Controller đặt
    //     activeCustomer rồi hiển thị tại admin-loyalty.jsp, tìm mã TDTK05; không có khách thì trang hiện
    //     thông báo tương ứng.
    public User findDefaultCustomer() throws SQLException {

        // [SQL12-06] Khách hiển thị ban đầu: chọn CUSTOMER có điểm cao nhất.
        // . Chỉ dùng khi chưa gửi số điện thoại; không dùng để thay thế một số đã tìm nhưng không có khách.
        String sql = "SELECT id, full_name, phone, email, role, position, status, total_points "
                + "FROM users WHERE role='CUSTOMER' ORDER BY total_points DESC, id LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapCustomer(rs) : null;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
    }

    private User mapCustomer(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("full_name"), rs.getString("phone"),
                rs.getString("email"), rs.getString("role"), rs.getString("position"),
                rs.getString("status"), "", "", rs.getInt("total_points"));
    }

    // [TDCT01]
    // . Trong LoyaltyTransactionDAO.getCustomerActivity, em nhận mã khách từ AdminLoyaltyController.doGet.
    //   Em cộng tiền của các đơn COMPLETED và lấy ngày đặt đơn gần nhất của khách ở mọi trạng thái. Hai
    //   kết quả được trả về controller để hiện hoạt động mua của đúng người đang xem.
    // ... Trả tổng chi tiêu và ngày mua gần nhất về doGet sau getCustomerActivity. Đọc điểm đặt
    //     customerActivity để đưa lên trang tại AdminLoyaltyController.java, tìm [TDCT02].
    public CustomerActivity getCustomerActivity(int customerId) throws SQLException {

        // [SQL12-07] Thông tin khách: tổng tiền đã mua và ngày đặt đơn gần nhất.
        // . Dấu ? từ trái sang phải: 1=mã khách hàng.
        // . Tiền chỉ cộng đơn COMPLETED; MAX(created_at) lấy đơn gần nhất của mọi trạng thái, không chỉ đơn
        //   hoàn thành.
        String sql = "SELECT COALESCE(SUM(CASE WHEN status='COMPLETED' THEN final_amount ELSE 0 END),0) total_spent, "
                + "MAX(created_at) last_order_at FROM orders WHERE customer_id=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CustomerActivity(rs.getBigDecimal("total_spent"), rs.getTimestamp("last_order_at"));
                }
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
        return new CustomerActivity(BigDecimal.ZERO, null);
    }

    // [TDDC03]
    // . Trong LoyaltyTransactionDAO.adjustPoints, em đọc số điểm hiện tại của khách rồi cộng với
    //   pointsChange để tính số dư mới trong next. Nếu next âm thì báo không đủ điểm và dừng trước khi
    //   ghi. Ví dụ đang có 20 điểm mà trừ 30 thì bị chặn; khách vẫn giữ 20 điểm và chưa có giao dịch trừ.
    // .. Số dư mới cũng phải không vượt mức lưu được. Khi lưu hợp lệ, số dư và lịch sử phải cùng thành
    //    công; lỗi thì hủy cả lần điều chỉnh.
    // ... Ngay trong LoyaltyTransactionDAO.java, đọc dòng long next và điều kiện if (next < 0) trước bước
    //     lưu. Hợp lệ mới đọc tiếp [TDDC04].
    public boolean adjustPoints(int customerId, int pointsChange) throws SQLException {

        LoyaltyValidation.requireRange(customerId, "Khách hàng", 1, Integer.MAX_VALUE);
        if (pointsChange == 0 || pointsChange < -LoyaltyValidation.MAX_POINTS
                || pointsChange > LoyaltyValidation.MAX_POINTS) {
            throw new IllegalArgumentException("Số điểm điều chỉnh phải từ 1 đến 1.000.000.");
        }
        // [SQL12-08] Nút Cộng/Trừ điểm: đọc số dư hiện tại và giữ dòng đang cập nhật.
        // . Dấu ? từ trái sang phải: 1=mã khách hàng.
        // . Java cộng pointsChange, chặn số dư âm hoặc vượt giới hạn trước khi ghi; FOR UPDATE không tự tính số
        //   dư mới.
        String lockSql = "SELECT total_points FROM users WHERE id=? AND role='CUSTOMER' FOR UPDATE";
        // [SQL12-09] Nút Cộng/Trừ điểm: ghi số dư mới đã kiểm tra.
        // . Dấu ? từ trái sang phải: 1=số dư mới; 2=mã khách hàng.
        // . Lệnh cập nhật số dư này và lệnh ghi lịch sử phải cùng thành công mới xác nhận lưu.
        String updateSql = "UPDATE users SET total_points=? WHERE id=?";
        // [SQL12-10] Nút Cộng/Trừ điểm: ghi giao dịch điều chỉnh ADJUST.
        // . Dấu ? từ trái sang phải: 1=mã khách hàng; 2=điểm thay đổi; cộng dương, trừ âm.
        // . order_id là NULL vì điều chỉnh thủ công; ghi số dư và lịch sử cùng thành công mới commit.
        String insertSql = "INSERT INTO loyalty_transactions(customer_id,order_id,points_change,transaction_type) "
                + "VALUES (?,NULL,?,'ADJUST')";
        try (Connection conn = getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {

                // . Trong LoyaltyTransactionDAO.adjustPoints, em đọc users.total_points theo mã khách và giữ dòng đang
                //   xử lý để lần cập nhật khác không chen vào. Em dùng số dư vừa đọc để tính lần cộng hoặc trừ này.
                int current;
                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setInt(1, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy khách hàng.");
                        current = rs.getInt(1);
                    }
                }

                // . Em lấy số dư hiện tại cộng pointsChange để tính next trong LoyaltyTransactionDAO.adjustPoints. Em
                //   kiểm tra số dư mới không âm và còn nằm trong mức lưu được rồi mới ghi, tránh trừ quá điểm hoặc làm
                //   số dư quá lớn.
                long next = (long) current + pointsChange;
                if (next < 0) throw new IllegalArgumentException("Khách hàng chỉ có " + current + " điểm. Không thể trừ " + Math.abs((long) pointsChange) + " điểm.");
                if (next > Integer.MAX_VALUE) throw new IllegalArgumentException("Tổng điểm sau điều chỉnh không được vượt quá 2.147.483.647 điểm.");

                // [TDDC04]
                // . Số dư mới đã đúng thì em ghi next vào users.total_points theo mã khách. Tiếp theo, em còn phải ghi
                //   lịch sử của chính lần cộng/trừ này; chỉ cập nhật số dư xong chưa được báo hoàn tất.
                // ... Đọc tiếp ngay trong LoyaltyTransactionDAO.java, tìm [TDDC05] để xem bước ghi lịch sử của cùng
                //     lần điều chỉnh.
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, (int) next);
                    ps.setInt(2, customerId);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không cập nhật được số dư khách hàng.");
                }

                // [TDDC05]
                // . Sau số dư, em thêm một giao dịch ADJUST vào loyalty_transactions, gồm mã khách và số điểm thay đổi
                //   của lần này. Cả số dư lẫn lịch sử đều ghi được thì mới xác nhận lưu và trả thành công về
                //   controller. Có lỗi thì hủy các thay đổi của lần điều chỉnh.
                // ... Ghi lịch sử xong thì đọc conn.commit ngay dưới để xác nhận cả số dư và lịch sử. Sau return true,
                //     kết quả về controller tại AdminLoyaltyController.java, tìm mã TDDC06. Có lỗi thì rollback và trả
                //     lỗi, chưa báo thành công.
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, pointsChange);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không ghi được lịch sử điều chỉnh điểm.");
                }

                // . Ghi được cả số dư và lịch sử, em gọi conn.commit để xác nhận lưu rồi trả true về controller.
                //   AdminLoyaltyController.doPost nhận kết quả thành công mới đặt thông báo.
                // ... Đọc tiếp tại AdminLoyaltyController.java, tìm [TDDC06].
                conn.commit();
                return true;
            } catch (Exception e) {
                // . Nếu tính điểm hoặc ghi dữ liệu gặp lỗi, em gọi conn.rollback để hủy thay đổi của lần điều chỉnh.
                //   Lỗi được báo về controller; số dư và lịch sử của lần này cùng được giữ nguyên.
                conn.rollback();
                if (e instanceof SQLException) throw (SQLException) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new SQLException(e);
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
    }

    public boolean earnForCompletedOrder(int customerId, int orderId, int points) throws SQLException {

        if (points <= 0) return false;
        String existsSql = "SELECT 1 FROM loyalty_transactions WHERE order_id=? AND transaction_type='EARN'";
        String updateSql = "UPDATE users SET total_points=total_points+? WHERE id=? AND role='CUSTOMER'";
        String insertSql = "INSERT INTO loyalty_transactions(customer_id,order_id,points_change,transaction_type) "
                + "VALUES (?,?,?,'EARN')";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement exists = conn.prepareStatement(existsSql)) {
                exists.setInt(1, orderId);
                try (ResultSet rs = exists.executeQuery()) {
                    if (rs.next()) return false;
                }
                try (PreparedStatement update = conn.prepareStatement(updateSql);
                     PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    update.setInt(1, points);
                    update.setInt(2, customerId);
                    if (update.executeUpdate() != 1) throw new SQLException("Không tìm thấy khách hàng.");
                    insert.setInt(1, customerId);
                    insert.setInt(2, orderId);
                    insert.setInt(3, points);
                    insert.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException) throw (SQLException) e;
                throw new SQLException(e);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tải được driver MySQL.", e);
        }
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s.-]", "").trim();
    }

    public static final class CustomerActivity {
        private final BigDecimal totalSpent;
        private final Timestamp lastOrderAt;

        public CustomerActivity(BigDecimal totalSpent, Timestamp lastOrderAt) {
            this.totalSpent = totalSpent != null ? totalSpent : BigDecimal.ZERO;
            this.lastOrderAt = lastOrderAt;
        }

        public BigDecimal getTotalSpent() { return totalSpent; }
        public Timestamp getLastOrderAt() { return lastOrderAt; }
    }
}
