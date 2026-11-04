package dao;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AdminOrderDAOTest {
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    @Test void dateBoundsAreParameterizedAndIncludeEndOfDay() throws Exception {
        Map<Integer, Timestamp> parameters = new HashMap<>();
        String[] query = new String[1];
        ResultSet result = proxy(ResultSet.class, (p, m, a) -> m.getName().equals("next") ? false : null);
        PreparedStatement statement = proxy(PreparedStatement.class, (p, m, a) -> {
            if (m.getName().equals("setTimestamp")) parameters.put((Integer) a[0], (Timestamp) a[1]);
            if (m.getName().equals("executeQuery")) return result;
            return null;
        });
        Connection connection = proxy(Connection.class, (p, m, a) -> {
            if (m.getName().equals("prepareStatement")) { query[0] = (String) a[0]; return statement; }
            return null;
        });
        AdminOrderDAO dao = new AdminOrderDAO() { @Override protected Connection getConnection() { return connection; } };
        assertTrue(dao.findAll(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30)).isEmpty());
        assertTrue(query[0].contains("o.created_at >= ?"));
        assertTrue(query[0].contains("o.created_at < ?"));
        assertEquals(Timestamp.valueOf("2026-10-01 00:00:00"), parameters.get(2));
        assertEquals(Timestamp.valueOf("2026-10-31 00:00:00"), parameters.get(4));
        dao.findAll(null, null);
        assertNull(parameters.get(2));
        assertNull(parameters.get(4));
    }
}
