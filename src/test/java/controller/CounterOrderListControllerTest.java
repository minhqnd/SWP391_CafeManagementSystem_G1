package controller;

import dao.OrderDAO;
import model.Order;
import model.User;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CounterOrderListControllerTest {
    static class FakeDAO extends OrderDAO {
        int reads;
        LocalDate date;
        public List<Order> getCounterOrdersByDate(LocalDate date) {
            reads++;
            this.date = date;
            return List.of(order("POS-1", "T01", "DINE_IN", "READY"),
                    order("POS-2", null, "TAKEAWAY", "COMPLETED"),
                    order("POS-3", "T01", "DINE_IN", "CANCELLED"));
        }
    }

    static Order order(String code, String table, String type, String status) {
        Order o = new Order();
        o.setOrderCode(code); o.setTableNumber(table); o.setOrderType(type); o.setStatus(status);
        return o;
    }

    @Test void combinesDateTypeStatusAndCaseInsensitiveSearch() throws Exception {
        FakeDAO dao = new FakeDAO();
        var x = new BaristaControllerTest.Exchange();
        x.params.putAll(Map.of("date", "2026-10-28", "type", "DINE_IN", "status", "READY", "q", " t01 "));
        new CounterOrderListController(dao).doGet(x.request, x.response);
        assertEquals(LocalDate.of(2026, 10, 28), dao.date);
        var rows = (List<?>) x.attributes.get("orders");
        assertEquals(1, rows.size());
        assertEquals("POS-1", ((Order) rows.get(0)).getOrderCode());
        assertEquals(2L, ((Map<?, ?>) x.attributes.get("statusCounts")).get("ALL"));
        assertEquals("t01", x.attributes.get("searchQuery"));
        assertEquals("/counter-orders.jsp", x.forwarded);
    }

    @Test void cashierCanSearchOrderCodeWithNullTableAndInvalidFiltersDefault() throws Exception {
        FakeDAO dao = new FakeDAO();
        var x = new BaristaControllerTest.Exchange();
        ((User) x.sessionValues.get("user")).setPosition("CASHIER");
        x.params.putAll(Map.of("q", "pos-2", "date", "invalid", "status", "invalid", "type", "ONLINE"));
        new CounterOrderListController(dao).doGet(x.request, x.response);
        assertEquals(LocalDate.now(), dao.date);
        assertEquals("ALL", x.attributes.get("statusFilter"));
        assertEquals("ALL", x.attributes.get("typeFilter"));
        assertEquals(1, ((List<?>) x.attributes.get("orders")).size());
        assertEquals(false, x.attributes.get("isBarista"));
    }

    @Test void unauthorizedRolesAndInactiveStaffCannotReadOrders() throws Exception {
        for (String[] identity : List.of(new String[]{"ADMIN", "CASHIER", "ACTIVE"},
                new String[]{"CUSTOMER", "", "ACTIVE"}, new String[]{"STAFF", "DELIVERY", "ACTIVE"},
                new String[]{"STAFF", "BARISTA", "INACTIVE"})) {
            FakeDAO dao = new FakeDAO();
            var x = new BaristaControllerTest.Exchange();
            User user = (User) x.sessionValues.get("user");
            user.setRole(identity[0]); user.setPosition(identity[1]); user.setStatus(identity[2]);
            int[] error = {0};
            HttpServletResponse response = BaristaControllerTest.proxy(HttpServletResponse.class, (p, m, a) -> {
                if (m.getName().equals("sendError")) error[0] = (int) a[0];
                return null;
            });
            new CounterOrderListController(dao).doGet(x.request, response);
            assertEquals(403, error[0]);
            assertEquals(0, dao.reads);
        }
    }

    @Test void guestRedirectsBeforeDatabaseAccessAndEmptySearchResultIsValid() throws Exception {
        FakeDAO dao = new FakeDAO();
        var x = new BaristaControllerTest.Exchange();
        x.hasSession = false;
        new CounterOrderListController(dao).doGet(x.request, x.response);
        assertNotNull(x.redirected);
        assertEquals(0, dao.reads);
        x.hasSession = true;
        x.params.put("q", "missing");
        new CounterOrderListController(dao).doGet(x.request, x.response);
        assertTrue(((List<?>) x.attributes.get("orders")).isEmpty());
    }
}
