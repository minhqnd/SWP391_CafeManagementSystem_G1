package dao;

import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;
import support.ServletMocks;
import utils.PasswordHasher;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserDAO Authentication Tests")
public class UserDAOTest {

    private static final String PASSWORD = "DemoTest123!";
    private static final String HASH = PasswordHasher.hash(PASSWORD);
    private UserDAO userDAO;

    @BeforeEach
    void setUp() {
        // Isolated JDBC fixtures: no dependency on current application accounts or passwords.
        userDAO = new UserDAO() {
            @Override protected Connection getConnection() {
                return ServletMocks.proxy(Connection.class, (connection, method, args) -> {
                    if (!method.getName().equals("prepareStatement")) return null;
                    Map<Integer,String> parameters = new HashMap<>();
                    return ServletMocks.proxy(PreparedStatement.class, (statement, call, values) -> {
                        if (call.getName().equals("setString")) parameters.put((Integer) values[0], (String) values[1]);
                        if (call.getName().equals("executeUpdate")) fail("Authentication fixture must not write to a database");
                        if (!call.getName().equals("executeQuery")) return null;
                        boolean admin = "admin@auracafe.vn".equals(parameters.get(1));
                        boolean found = admin || "an@auracafe.vn".equals(parameters.get(1))
                                || "0900000002".equals(parameters.get(2));
                        boolean[] consumed = {false};
                        Map<String,String> row = Map.of("status", "ACTIVE", "password_hash", HASH,
                                "full_name", admin ? "Test Admin" : "Test Barista",
                                "phone", admin ? "0900000001" : "0900000002",
                                "email", admin ? "admin@auracafe.vn" : "an@auracafe.vn",
                                "role", admin ? "ADMIN" : "STAFF", "position", admin ? "MANAGER" : "BARISTA");
                        return ServletMocks.proxy(ResultSet.class, (result, read, columns) -> switch (read.getName()) {
                            case "next" -> { boolean next = found && !consumed[0]; consumed[0] = true; yield next; }
                            case "getInt" -> admin ? 1 : 2;
                            case "getString" -> row.get(columns[0]);
                            case "getMetaData" -> ServletMocks.proxy(ResultSetMetaData.class,
                                    (metadata, metaCall, metaArgs) -> metaCall.getName().equals("getColumnCount") ? 0 : null);
                            default -> null;
                        });
                    });
                });
            }
        };
    }

    @Test
    @DisplayName("TC-USER-01: Đăng nhập thành công bằng số điện thoại")
    void testLoginWithPhone() {
        User user = userDAO.login("0900000002", PASSWORD);
        assertNotNull(user, "Số điện thoại đã đăng ký phải đăng nhập được");
        assertEquals("0900000002", user.getPhone());
        assertEquals("BARISTA", user.getPosition());
    }

    @Test
    @DisplayName("TC-USER-02: Đăng nhập thành công với Email")
    void testLoginWithEmail() {
        User user = userDAO.login("an@auracafe.vn", PASSWORD);
        assertNotNull(user, "User Barista phải đăng nhập thành công với Email");
        assertEquals("BARISTA", user.getPosition());
    }

    @Test
    @DisplayName("TC-USER-03: Không cho đăng nhập bằng email prefix")
    void testLoginWithUsernamePrefix() {
        User user = userDAO.login("an", PASSWORD);
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
        User user = userDAO.login("admin@auracafe.vn", PASSWORD);
        assertNotNull(user);
        assertEquals("ADMIN", user.getRole());
    }
}
