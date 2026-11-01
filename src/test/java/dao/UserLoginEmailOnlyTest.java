package dao;

import model.User;
import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class UserLoginEmailOnlyTest {
    private int connections;
    private String sql;
    private String email;
    private String status = "ACTIVE";
    private boolean found = true;

    private UserDAO dao() {
        return new UserDAO() {
            @Override protected Connection getConnection() {
                connections++;
                return mock(Connection.class, (p,m,a) -> {
                    if (!m.getName().equals("prepareStatement")) return null;
                    sql = (String) a[0];
                    return mock(PreparedStatement.class, (sp,sm,sa) -> {
                        if (sm.getName().equals("setString")) {
                            assertEquals(1, sa[0]); email = (String) sa[1];
                        }
                        if (sm.getName().equals("executeQuery")) {
                            return mock(ResultSet.class, (rp,rm,ra) -> switch (rm.getName()) {
                                case "next" -> found;
                                case "getInt" -> 7;
                                case "getString" -> Map.of("status", status, "password_hash", "test-secret",
                                        "full_name", "Test Customer", "phone", "0909000101",
                                        "email", "test@example.com", "role", "CUSTOMER", "position", "").get(ra[0]);
                                case "getMetaData" -> mock(ResultSetMetaData.class,
                                        (mp,mm,ma) -> mm.getName().equals("getColumnCount") ? 0 : null);
                                default -> null;
                            });
                        }
                        return null;
                    });
                });
            }
        };
    }

    @Test void rejectsPhoneAndPrefixBeforeConnecting() {
        for (String value : new String[]{"0909000101", "test", "", "test@", "test example@example.com"}) {
            assertNull(dao().login(value, "test-secret"));
        }
        assertEquals(0, connections);
    }
    @Test void authenticatesByEmailOnlyAndKeepsRegisteredPhone() {
        User user = dao().login(" test@example.com ", "test-secret");
        assertNotNull(user);
        assertEquals("SELECT * FROM users WHERE email = ?", sql);
        assertEquals("test@example.com", email);
        assertEquals("0909000101", user.getPhone());
    }
    @Test void rejectsWrongPassword() {
        assertNull(dao().login("test@example.com", "wrong"));
    }
    @Test void rejectsInactiveAccount() {
        status = "INACTIVE";
        assertNull(dao().login("test@example.com", "test-secret"));
    }
    @Test void rejectsUnknownEmail() {
        found = false;
        assertNull(dao().login("missing@example.com", "test-secret"));
    }
    @Test void rejectsNullCredentialsWithoutConnecting() {
        assertNull(dao().login(null, "test-secret"));
        assertNull(dao().login("test@example.com", null));
        assertEquals(0, connections);
    }
    private static <T> T mock(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
