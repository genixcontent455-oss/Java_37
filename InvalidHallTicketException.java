/**
 * Thrown when a hall-ticket credential is rejected or the candidate is not registered.
 * Checked exception (extends Exception) so callers are forced to handle it.
 * The messages are user-facing and NEVER contain hashes or raw credentials.
 */
public class InvalidHallTicketException extends Exception {
    private static final long serialVersionUID = 1L;

    /** Why the entry was refused; lets the caller distinguish the cases without parsing text. */
    public enum Reason { MALFORMED_CREDENTIAL, NOT_REGISTERED, HASH_MISMATCH, UNSPECIFIED }

    private final Reason reason;

    public InvalidHallTicketException(String message) {
        super(message);
        this.reason = Reason.UNSPECIFIED;
    }

    public InvalidHallTicketException(String message, Throwable cause) {
        super(message, cause);
        this.reason = Reason.UNSPECIFIED;
    }

    public InvalidHallTicketException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
