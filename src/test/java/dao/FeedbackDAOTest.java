package dao;

import context.DBContext;
import model.FeedbackStatus;
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

    @Test
    @DisplayName("Admin can retrieve all feedbacks with customer and order details")
    void testGetAdminFeedbacks() throws Exception {
        try (Connection conn = openDatabaseOrSkip()) {
            List<model.Feedback> list = feedbackDAO.getAdminFeedbacks(null, null);
            org.junit.jupiter.api.Assertions.assertNotNull(list);
            if (!list.isEmpty()) {
                model.Feedback first = list.get(0);
                org.junit.jupiter.api.Assertions.assertTrue(first.getId() > 0);
                org.junit.jupiter.api.Assertions.assertNotNull(first.getIssueType());
            }

            List<model.Feedback> ratings = feedbackDAO.getAdminFeedbacks("RATING", null);
            for (model.Feedback fb : ratings) {
                assertEquals("RATING", fb.getIssueType());
            }

            List<model.Feedback> complaints = feedbackDAO.getAdminFeedbacks("COMPLAINT", null);
            for (model.Feedback fb : complaints) {
                assertEquals("COMPLAINT", fb.getIssueType());
            }
        }
    }

    @Test
    @DisplayName("A closed feedback cannot be edited or reopened")
    void closedFeedbackCannotBeUpdated() throws Exception {
        try (Connection conn = openDatabaseOrSkip()) {
            List<model.Feedback> list = feedbackDAO.getAdminFeedbacks(null, null);
            model.Feedback target = list.stream()
                    .filter(model.Feedback::isTerminal)
                    .findFirst()
                    .orElse(null);
            Assumptions.assumeTrue(target != null, "No closed review is available in the test database");

            FeedbackDAO.ResponseUpdateResult result = feedbackDAO.updateFeedbackResponse(
                    target.getId(), FeedbackStatus.PROCESSING, "This update must not be saved");

            assertEquals(FeedbackDAO.ResponseUpdateResult.ALREADY_CLOSED, result);
            model.Feedback unchanged = feedbackDAO.getAdminFeedbacks(null, null).stream()
                    .filter(item -> item.getId() == target.getId())
                    .findFirst()
                    .orElseThrow();
            assertEquals(target.getStatus(), unchanged.getStatus());
            assertEquals(target.getAdminResponse(), unchanged.getAdminResponse());
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
