package model;

import java.util.Locale;

/**
 * Lifecycle states for a customer feedback ticket.
 * RESOLVED and REJECTED are terminal: a closed ticket cannot be edited or reopened.
 */
public enum FeedbackStatus {
    PENDING(false),
    PROCESSING(false),
    RESOLVED(true),
    REJECTED(true);

    private final boolean terminal;

    FeedbackStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }

    /**
     * A reply must move a pending ticket forward, or continue/finalize a ticket
     * that is already being processed. PENDING is therefore not a reply target.
     */
    public boolean canReplyWith(FeedbackStatus target) {
        return !terminal && target != null && target != PENDING;
    }

    public static FeedbackStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn trạng thái xử lý.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Trạng thái xử lý không hợp lệ.");
        }
    }
}
