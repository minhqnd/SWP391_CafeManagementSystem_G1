package utils;

/** Keeps account-field errors separate from mail/database failures. */
public final class RecoveryFailure extends IllegalArgumentException {
    private final String field;
    public RecoveryFailure(String field, String message) {
        super(message);
        this.field = field;
    }
    public String field() { return field; }
}
