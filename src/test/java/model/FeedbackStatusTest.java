package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Feedback status workflow")
class FeedbackStatusTest {

    @Test
    @DisplayName("Pending and processing tickets can move forward")
    void activeTicketsCanMoveForward() {
        assertTrue(FeedbackStatus.PENDING.canReplyWith(FeedbackStatus.PROCESSING));
        assertTrue(FeedbackStatus.PENDING.canReplyWith(FeedbackStatus.RESOLVED));
        assertTrue(FeedbackStatus.PENDING.canReplyWith(FeedbackStatus.REJECTED));
        assertTrue(FeedbackStatus.PROCESSING.canReplyWith(FeedbackStatus.PROCESSING));
        assertTrue(FeedbackStatus.PROCESSING.canReplyWith(FeedbackStatus.RESOLVED));
        assertTrue(FeedbackStatus.PROCESSING.canReplyWith(FeedbackStatus.REJECTED));
    }

    @Test
    @DisplayName("Replying cannot move a ticket back to pending")
    void replyCannotReturnToPending() {
        assertFalse(FeedbackStatus.PENDING.canReplyWith(FeedbackStatus.PENDING));
        assertFalse(FeedbackStatus.PROCESSING.canReplyWith(FeedbackStatus.PENDING));
    }

    @Test
    @DisplayName("Resolved and rejected tickets are terminal")
    void closedTicketsAreTerminal() {
        assertTrue(FeedbackStatus.RESOLVED.isTerminal());
        assertTrue(FeedbackStatus.REJECTED.isTerminal());
        assertFalse(FeedbackStatus.RESOLVED.canReplyWith(FeedbackStatus.PROCESSING));
        assertFalse(FeedbackStatus.REJECTED.canReplyWith(FeedbackStatus.RESOLVED));
    }

    @Test
    @DisplayName("Invalid or missing status values are rejected")
    void invalidStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FeedbackStatus.from(null));
        assertThrows(IllegalArgumentException.class, () -> FeedbackStatus.from(""));
        assertThrows(IllegalArgumentException.class, () -> FeedbackStatus.from("DONE"));
    }
}
