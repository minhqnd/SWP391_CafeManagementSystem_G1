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

class UserLoginIdentifierTest {
    private static final String HASH = utils.PasswordHasher.hash("test-secret");
    private int connections;
    private String sql;
    private final Map<Integer,String> bindings = new java.util.HashMap<>();
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
                            bindings.put((Integer)sa[0], (String)sa[1]);
                        }
                        if (sm.getName().equals("executeQuery")) {
                            return mock(ResultSet.class, (rp,rm,ra) -> switch (rm.getName()) {
                                case "next" -> found && ("test@example.com".equals(bindings.get(1)) || "0909000101".equals(bindings.get(2)));
                                case "getInt" -> 7;
                                case "getString" -> Map.of("status", status, "password_hash", HASH,
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

    @Test void rejectsUnknownIdentifiers() {
        for (String value : new String[]{"unknown", "", "test@", "missing@example.com", "0909999999"}) {
            assertNull(dao().login(value, "test-secret"));
        }
    }
    @Test void authenticatesByEmailOrPhoneAndKeepsRegisteredPhone() {
        for (String identifier : new String[]{" test@example.com ", " 0909000101 "}) {
            User user = dao().login(identifier, "test-secret");
            assertNotNull(user);
            assertEquals("SELECT * FROM users WHERE (email = ? OR phone = ?)", sql);
            assertEquals(identifier.trim(), bindings.get(1));
            assertEquals(identifier.trim(), bindings.get(2));
            assertEquals("0909000101", user.getPhone());
        }
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
