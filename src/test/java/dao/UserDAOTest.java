package dao;

import model.User;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserDAO Authentication Tests")
public class UserDAOTest {

    private final UserDAO userDAO = new UserDAO();

    @BeforeEach
    void checkDatabaseConnection() {
        try (Connection conn = context.DBContext.getConnection()) {
            Assumptions.assumeTrue(conn != null, "Skipping UserDAO tests because database is unavailable in CI environment");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                Assumptions.assumeTrue(rs.next() && rs.getInt(1) > 0, "Skipping UserDAO tests because users table is not seeded");
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Skipping UserDAO tests because database is unavailable in CI environment: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("TC-USER-01: Không cho đăng nhập bằng số điện thoại")
    void testLoginWithPhone() {
        User user = userDAO.login("0900000002", "123456");
        assertNull(user, "Chỉ được đăng nhập bằng email đầy đủ");
    }

    @Test
    @DisplayName("TC-USER-02: Đăng nhập thành công với Email")
    void testLoginWithEmail() {
        User user = userDAO.login("an@auracafe.vn", "demo_hash");
        assertNotNull(user, "User Barista phải đăng nhập thành công với Email");
        assertEquals("BARISTA", user.getPosition());
    }

    @Test
    @DisplayName("TC-USER-03: Không cho đăng nhập bằng email prefix")
    void testLoginWithUsernamePrefix() {
        User user = userDAO.login("an", "123456");
        assertNull(user, "Email prefix không phải thông tin đăng nhập hợp lệ");
    }

    @Test
    @DisplayName("TC-USER-04: Đăng nhập thất bại khi sai mật khẩu")
    void testLoginWrongPassword() {
        User user = userDAO.login("an@auracafe.vn", "sai_mat_khau_999");
        assertNull(user, "Phải trả về null khi sai mật khẩu");
    }

    @Test
    @DisplayName("TC-USER-05: Đăng nhập Admin thành công")
    void testLoginAdmin() {
        User user = userDAO.login("admin@auracafe.vn", "123456");
        assertNotNull(user);
        assertEquals("ADMIN", user.getRole());
    }
}
