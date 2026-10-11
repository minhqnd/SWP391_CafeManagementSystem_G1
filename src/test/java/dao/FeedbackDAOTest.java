package dao;

import context.DBContext;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("Feedback order eligibility tests")
class FeedbackDAOTest {

    private final FeedbackDAO feedbackDAO = new FeedbackDAO();

    @Test
    @DisplayName("An order with an existing review is removed from the related-order list")
    void reviewedOrderIsNotListedAgain() throws Exception {
        try (Connection conn = openDatabaseOrSkip()) {
            ReviewedOrder reviewedOrder = findReviewedOrder(conn);
            Assumptions.assumeTrue(reviewedOrder != null, "No reviewed order is available in the test database");

            List<String[]> availableOrders = feedbackDAO.getRecentOrdersForCustomer(reviewedOrder.customerId);

            assertFalse(availableOrders.stream()
                    .anyMatch(order -> Integer.parseInt(order[0]) == reviewedOrder.orderId));
            assertEquals(0, feedbackDAO.findOrderId(reviewedOrder.orderCode, reviewedOrder.customerId));
        }
    }

    private Connection openDatabaseOrSkip() throws Exception {
        try {
            return DBContext.getConnection();
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Database is unavailable: " + e.getMessage());
            throw e;
        }
    }

    private ReviewedOrder findReviewedOrder(Connection conn) throws Exception {
        String sql = "SELECT cr.customer_id, cr.order_id, o.order_code "
                + "FROM customer_reviews cr JOIN orders o ON o.id = cr.order_id LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            return new ReviewedOrder(rs.getInt(1), rs.getInt(2), rs.getString(3));
        }
    }

    private static class ReviewedOrder {
        private final int customerId;
        private final int orderId;
        private final String orderCode;

        private ReviewedOrder(int customerId, int orderId, String orderCode) {
            this.customerId = customerId;
            this.orderId = orderId;
            this.orderCode = orderCode;
        }
    }
}
