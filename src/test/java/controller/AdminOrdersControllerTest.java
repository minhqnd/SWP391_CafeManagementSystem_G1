package controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AdminOrdersControllerTest {
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    @Test void adminMutationRequestsAreForbiddenRegardlessOfActionOrToken() throws Exception {
        User admin = new User();
        admin.setRole("ADMIN");
        HttpSession session = proxy(HttpSession.class, (p, m, a) -> m.getName().equals("getAttribute") ? admin : null);
        HttpServletRequest request = proxy(HttpServletRequest.class, (p, m, a) -> m.getName().equals("getSession") ? session : null);
        AtomicInteger status = new AtomicInteger();
        HttpServletResponse response = proxy(HttpServletResponse.class, (p, m, a) -> {
            if (m.getName().equals("sendError")) status.set((Integer) a[0]);
            return null;
        });
        new AdminOrdersController().doPost(request, response);
        assertEquals(403, status.get());
    }

    @Test void dateParsingRequiresRealIsoDates() {
        assertEquals(LocalDate.of(2026, 10, 30), AdminOrdersController.parseDate("2026-10-30"));
        assertNull(AdminOrdersController.parseDate(""));
        assertThrows(IllegalArgumentException.class, () -> AdminOrdersController.parseDate("2026-02-30"));
        assertThrows(IllegalArgumentException.class, () -> AdminOrdersController.parseDate("30/10/2026"));
    }

    @Test void reversedDateRangeIsRejectedBeforeAnyDatabaseAccess() throws Exception {
        User admin = new User();
        admin.setRole("ADMIN");
        HttpSession session = proxy(HttpSession.class, (p, m, a) ->
                m.getName().equals("getAttribute") && "user".equals(a[0]) ? admin : null);
        HttpServletRequest request = proxy(HttpServletRequest.class, (p, m, a) -> {
            if (m.getName().equals("getSession")) return session;
            if (m.getName().equals("getParameter")) {
                if ("from".equals(a[0])) return "2026-10-30";
                if ("to".equals(a[0])) return "2026-10-01";
            }
            return null;
        });
        AtomicInteger status = new AtomicInteger();
        HttpServletResponse response = proxy(HttpServletResponse.class, (p, m, a) -> {
            if (m.getName().equals("sendError")) status.set((Integer) a[0]);
            return null;
        });
        new AdminOrdersController().doGet(request, response);
        assertEquals(400, status.get());
    }
}
