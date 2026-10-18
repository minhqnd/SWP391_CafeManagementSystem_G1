package service;

import dao.OrderDAO;
import model.Order;
import model.OrderDetail;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BaristaPreparationServiceTest {
    static class FakeDAO extends OrderDAO {
        Map<Integer, String> statuses = new LinkedHashMap<>(Map.of(1, "CONFIRMED", 2, "PREPARING"));
        int writes;
        boolean fail, conflict;
        @Override public List<Order> getOrdersForBarista() {
            List<Order> orders = new ArrayList<>();
            statuses.forEach((id, status) -> {
                if (!List.of("CONFIRMED", "PREPARING", "READY").contains(status)) return;
                Order order = new Order(); order.setId(id); order.setStatus(status);
                OrderDetail item = new OrderDetail(); item.setId(id * 10); item.setQuantity(3);
                order.getItems().add(item); orders.add(order);
            });
            return orders;
        }
        @Override public boolean updateBaristaOrderStatus(int id, String status) throws SQLException {
            writes++;
            if (fail) throw new SQLException("simulated failure");
            if (conflict) return false;
            statuses.put(id, status); return true;
        }
    }
    static Order find(BaristaPreparationService service, int id) throws Exception {
        return service.getOrders().stream().filter(o -> o.getId() == id).findFirst().orElseThrow();
    }
    @Test void groupCheckAndUncheckSurviveFreshReadsWithoutDatabaseWrites() throws Exception {
        FakeDAO dao = new FakeDAO();
        BaristaPreparationService service = new BaristaPreparationService(dao);
        assertTrue(service.updateItems(List.of(10, 20), true));
        for (Order order : service.getOrders()) {
            assertTrue(order.getItems().get(0).isPrepared());
            assertEquals("PREPARING", order.getStatus());
        }
        assertTrue(service.updateItems(List.of(10), false));
        assertFalse(find(service, 1).getItems().get(0).isPrepared());
        assertTrue(find(service, 2).getItems().get(0).isPrepared());
        assertEquals(0, dao.writes);
        assertEquals("CONFIRMED", dao.statuses.get(1));
    }
    @Test void invalidOrCompletedLineRejectsWholeGroup() throws Exception {
        FakeDAO dao = new FakeDAO(); dao.statuses.put(2, "READY");
        BaristaPreparationService service = new BaristaPreparationService(dao);
        assertFalse(service.updateItems(List.of(10, 20), true));
        assertFalse(find(service, 1).getItems().get(0).isPrepared());
        assertFalse(service.updateItems(List.of(10, 999), true));
        assertFalse(find(service, 1).getItems().get(0).isPrepared());
        assertEquals(0, dao.writes);
    }
    @Test void readyUsesExistingStatusAndMarksAllLinesWithRuntimeTimestamp() throws Exception {
        FakeDAO dao = new FakeDAO();
        BaristaPreparationService service = new BaristaPreparationService(dao);
        assertTrue(service.updateStatus(1, "READY"));
        Order order = find(service, 1);
        assertEquals("READY", order.getStatus());
        assertTrue(order.getItems().get(0).isPrepared());
        assertNotNull(order.getReadyAt());
        assertFalse(service.updateItems(List.of(10), false));
        assertEquals(1, dao.writes);
    }
    @Test void failedCompletionDoesNotInventReadyStateOrLoseCheckboxes() throws Exception {
        FakeDAO dao = new FakeDAO();
        BaristaPreparationService service = new BaristaPreparationService(dao);
        service.updateItems(List.of(10), true);
        dao.fail = true;
        assertThrows(SQLException.class, () -> service.updateStatus(1, "READY"));
        assertNull(find(service, 1).getReadyAt());
        assertTrue(find(service, 1).getItems().get(0).isPrepared());
        dao.fail = false; dao.conflict = true;
        assertFalse(service.updateStatus(1, "READY"));
        assertEquals("PREPARING", find(service, 1).getStatus());
    }
    @Test void restartClearsTransientStateButKeepsDatabaseReadyOrders() throws Exception {
        FakeDAO dao = new FakeDAO();
        BaristaPreparationService service = new BaristaPreparationService(dao);
        service.updateItems(List.of(10), true);
        service.updateStatus(2, "READY");
        BaristaPreparationService restarted = new BaristaPreparationService(dao);
        assertFalse(find(restarted, 1).getItems().get(0).isPrepared());
        assertEquals("READY", find(restarted, 2).getStatus());
        assertTrue(find(restarted, 2).getItems().get(0).isPrepared());
        assertNull(find(restarted, 2).getReadyAt());
    }
    @Test void removedOrdersDoNotKeepStaleCheckboxes() throws Exception {
        FakeDAO dao = new FakeDAO();
        BaristaPreparationService service = new BaristaPreparationService(dao);
        service.updateItems(List.of(10), true);
        dao.statuses.put(1, "CANCELLED"); service.getOrders();
        dao.statuses.put(1, "CONFIRMED");
        assertFalse(find(service, 1).getItems().get(0).isPrepared());
    }
    @Test void rejectsInvalidIds() {
        BaristaPreparationService service = new BaristaPreparationService(new FakeDAO());
        assertThrows(IllegalArgumentException.class, () -> service.updateItems(List.of(), true));
        assertThrows(IllegalArgumentException.class, () -> service.updateItems(List.of(-1), true));
    }
}
