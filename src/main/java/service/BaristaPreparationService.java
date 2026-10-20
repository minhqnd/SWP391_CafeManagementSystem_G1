package service;

import dao.OrderDAO;
import model.Order;
import model.OrderDetail;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Shared by all requests to the Barista servlet in one application instance.
 * Checkboxes and completion timestamps live only in memory, never in the database.
 * Restart/redeploy clears them. Existing orders.status remains authoritative.
 */
public class BaristaPreparationService {
    private final OrderDAO orderDAO;
    private final Set<Integer> preparedItems = new HashSet<>();
    private final Set<Integer> startedOrders = new HashSet<>();
    private final Map<Integer, LocalDateTime> completionTimes = new HashMap<>();

    public BaristaPreparationService(OrderDAO orderDAO) { this.orderDAO = orderDAO; }

    private static boolean active(Order order) {
        return "CONFIRMED".equals(order.getStatus()) || "PREPARING".equals(order.getStatus());
    }

    public synchronized List<Order> getOrders() throws SQLException, ClassNotFoundException {
        List<Order> orders = orderDAO.getOrdersForBarista();
        Set<Integer> activeItems = new HashSet<>();
        Set<Integer> activeIds = new HashSet<>();
        Set<Integer> readyIds = new HashSet<>();
        for (Order order : orders) {
            if (active(order)) {
                activeIds.add(order.getId());
                for (OrderDetail item : order.getItems()) activeItems.add(item.getId());
            } else if ("READY".equals(order.getStatus())) {
                readyIds.add(order.getId());
            }
        }
        preparedItems.retainAll(activeItems);
        startedOrders.retainAll(activeIds);
        completionTimes.keySet().retainAll(readyIds);
        for (Order order : orders) {
            boolean ready = "READY".equals(order.getStatus());
            order.setReadyAt(ready ? completionTimes.get(order.getId()) : null);
            for (OrderDetail item : order.getItems()) {
                item.setPrepared(ready || preparedItems.contains(item.getId()));
            }
            if ("CONFIRMED".equals(order.getStatus()) && startedOrders.contains(order.getId())) {
                order.setStatus("PREPARING");
            }
        }
        return orders;
    }

    public synchronized boolean updateItems(List<Integer> itemIds, boolean prepared)
            throws SQLException, ClassNotFoundException {
        if (itemIds == null || itemIds.isEmpty() || itemIds.size() > 1000
                || itemIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Invalid item IDs");
        }
        Map<Integer, Integer> eligible = new HashMap<>();
        for (Order order : getOrders()) {
            if (active(order)) {
                for (OrderDetail item : order.getItems()) eligible.put(item.getId(), order.getId());
            }
        }
        // Validate the whole group first; a stale or foreign line cannot partially change it.
        if (!eligible.keySet().containsAll(itemIds)) return false;
        if (prepared) {
            preparedItems.addAll(itemIds);
            for (int id : itemIds) startedOrders.add(eligible.get(id));
        } else {
            preparedItems.removeAll(itemIds);
        }
        return true;
    }

    public synchronized boolean updateStatus(int orderId, String status)
            throws SQLException, ClassNotFoundException {
        // Reuse the existing conditional status update. No new SQL fields or item writes.
        if (!orderDAO.updateBaristaOrderStatus(orderId, status)) return false;
        if ("READY".equals(status)) {
            completionTimes.put(orderId, LocalDateTime.now());
            startedOrders.remove(orderId);
        }
        return true;
    }
}
