package controller;

import dao.OrderDAO;
import model.Order;
import model.OrderDetail;
import model.User;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BaristaControllerTest {
    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    static class FakeDAO extends OrderDAO {
        int reads;
        boolean result = true;
        int orderId;
        String status;
        public boolean updateBaristaOrderStatus(int id, String status) {
            this.orderId = id; this.status = status; return result;
        }
        public List<Order> getOrdersForBarista() {
            reads++;
            Order order = new Order();
            order.setId(10); order.setStatus("CONFIRMED");
            for (int id : List.of(11, 21)) {
                OrderDetail item = new OrderDetail();
                item.setId(id); item.setQuantity(1);
                order.getItems().add(item);
            }
            return List.of(order);
        }
    }

    static class Exchange {
        final Map<String, Object> sessionValues = new HashMap<>();
        final Map<String, String> params = new HashMap<>();
        final Map<String, Object> attributes = new HashMap<>();
        final StringWriter body = new StringWriter();
        int status = 200;
        String forwarded, redirected;
        boolean hasSession = true;
        final HttpSession session = proxy(HttpSession.class, (p, m, a) -> {
            if (m.getName().equals("getAttribute")) return sessionValues.get(a[0]);
            if (m.getName().equals("setAttribute")) sessionValues.put((String) a[0], a[1]);
            return null;
        });
        final HttpServletRequest request = proxy(HttpServletRequest.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getSession": return hasSession ? session : null;
                case "getParameter": return params.get(a[0]);
                case "getServletPath": return "/barista";
                case "getContextPath": return "/cafe";
                case "setAttribute": attributes.put((String) a[0], a[1]); return null;
                case "getRequestDispatcher":
                    forwarded = (String) a[0];
                    return proxy(RequestDispatcher.class, (x, y, z) -> null);
                default: return null;
            }
        });
        final HttpServletResponse response = proxy(HttpServletResponse.class, (p, m, a) -> {
            switch (m.getName()) {
                case "setStatus": case "sendError": status = (Integer) a[0]; break;
                case "getWriter": return new PrintWriter(body);
                case "sendRedirect": redirected = (String) a[0]; break;
            }
            return null;
        });
        Exchange() {
            User user = new User();
            user.setRole("STAFF"); user.setPosition("BARISTA"); user.setStatus("ACTIVE");
            sessionValues.put("user", user);
            sessionValues.put("baristaCsrfToken", "test-token");
            params.put("csrfToken", "test-token");
        }
    }

    @Test void projectionContainsInMemoryCheckboxAndCompletionTime() {
        Order order = new Order();
        order.setId(1); order.setStatus("READY"); order.setReadyAt(LocalDateTime.of(2026, 10, 19, 9, 0));
        OrderDetail item = new OrderDetail();
        item.setId(7); item.setProductId(3); item.setQuantity(3); item.setPrepared(true);
        item.setProductName("</script><script>alert(1)</script>");
        order.getItems().add(item);
        String json = BaristaController.json(BaristaController.dashboardData(List.of(order)));
        assertTrue(json.contains("\"itemId\":7"));
        assertTrue(json.contains("\"isPrepared\":true"));
        assertTrue(json.contains("\"quantity\":3"));
        assertTrue(json.contains("\"readyAt\":\"2026-10-19T09:00\""));
        assertFalse(json.contains("<script>"));
        assertFalse(json.contains("statistics"));
    }

    @Test void acceptsBulkCheckAndUncheck() throws Exception {
        FakeDAO dao = new FakeDAO();
        Exchange x = new Exchange();
        x.params.putAll(Map.of("action", "update-items", "itemIds", "11,21", "isPrepared", "true"));
        BaristaController controller = new BaristaController(dao);
        controller.doPost(x.request, x.response);
        assertEquals(200, x.status);
        Exchange read = new Exchange();
        read.params.put("action", "orders");
        controller.doGet(read.request, read.response);
        assertTrue(read.body.toString().contains("\"isPrepared\":true"));
        assertFalse(read.body.toString().contains("\"isPrepared\":false"));
        x.params.put("isPrepared", "false");
        controller.doPost(x.request, x.response);
        Exchange reread = new Exchange();
        reread.params.put("action", "orders");
        controller.doGet(reread.request, reread.response);
        assertFalse(reread.body.toString().contains("\"isPrepared\":true"));
        assertEquals(0, dao.orderId, "Checkboxes must not write to the database");
    }

    @Test void requiresSessionRoleAndCsrfBeforeMutation() throws Exception {
        for (String invalid : List.of("session", "role", "csrf")) {
            FakeDAO dao = new FakeDAO();
            Exchange x = new Exchange();
            x.params.putAll(Map.of("action", "update-items", "itemIds", "1", "isPrepared", "true"));
            if (invalid.equals("session")) x.hasSession = false;
            if (invalid.equals("role")) ((User) x.sessionValues.get("user")).setPosition("CASHIER");
            if (invalid.equals("csrf")) x.params.put("csrfToken", "wrong");
            new BaristaController(dao).doPost(x.request, x.response);
            assertEquals(invalid.equals("session") ? 401 : 403, x.status);
            assertEquals(0, dao.reads);
        }
    }

    @Test void rejectsMalformedItemRequests() throws Exception {
        for (Map<String, String> values : List.of(
                Map.of("itemIds", "1,", "isPrepared", "true"),
                Map.of("itemIds", "abc", "isPrepared", "true"),
                Map.of("itemIds", "1", "isPrepared", "yes"))) {
            FakeDAO dao = new FakeDAO();
            Exchange x = new Exchange();
            x.params.put("action", "update-items"); x.params.putAll(values);
            new BaristaController(dao).doPost(x.request, x.response);
            assertEquals(400, x.status);
            assertEquals(0, dao.reads);
        }
    }

    @Test void staleOrderReturnsConflictAndCompletionKeepsExistingEndpoint() throws Exception {
        FakeDAO dao = new FakeDAO();
        Exchange x = new Exchange();
        x.params.putAll(Map.of("action", "update-order-status", "orderId", "10", "status", "READY"));
        BaristaController controller = new BaristaController(dao);
        controller.doPost(x.request, x.response);
        assertEquals(10, dao.orderId);
        assertEquals("READY", dao.status);
        assertEquals(200, x.status);
        dao.result = false;
        controller.doPost(x.request, x.response);
        assertEquals(409, x.status);
    }

    @Test void activePageUsesUnifiedJsp() throws Exception {
        Exchange x = new Exchange();
        new BaristaController(new FakeDAO()).doGet(x.request, x.response);
        assertEquals("/barista-dashboard.jsp", x.forwarded);
        assertEquals("active", x.attributes.get("baristaView"));
        assertNotNull(x.attributes.get("baristaInitialData"));
    }

    @Test void completedPageUsesUnifiedJspAndInitializesCsrf() throws Exception {
        Exchange x = new Exchange();
        x.sessionValues.remove("baristaCsrfToken");
        x.params.put("action", "completed");
        new BaristaController(new FakeDAO()).doGet(x.request, x.response);
        assertEquals("/barista-dashboard.jsp", x.forwarded);
        assertEquals("completed", x.attributes.get("baristaView"));
        assertNotNull(x.sessionValues.get("baristaCsrfToken"));
        x.params.put("action", "recipes");
        new BaristaController(new FakeDAO()).doGet(x.request, x.response);
        assertEquals("/cafe/barista?action=completed", x.redirected);
    }
}
