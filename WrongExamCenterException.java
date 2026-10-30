/**
 * Thrown when a VALID, registered candidate tries to enter a hall that belongs to a
 * different examination center than the one on their record.
 */
public class WrongExamCenterException extends Exception {
    private static final long serialVersionUID = 1L;

    private final String assignedCenterId;
    private final String requestedCenterId;

    public WrongExamCenterException(String message) {
        super(message);
        this.assignedCenterId = null;
        this.requestedCenterId = null;
    }

    public WrongExamCenterException(String message, Throwable cause) {
        super(message, cause);
        this.assignedCenterId = null;
        this.requestedCenterId = null;
    }

    public WrongExamCenterException(String assignedCenterId, String requestedCenterId) {
        super("Wrong examination center. This candidate must report to center "
                + assignedCenterId + ", not " + requestedCenterId + ".");
        this.assignedCenterId = assignedCenterId;
        this.requestedCenterId = requestedCenterId;
    }

    public String getAssignedCenterId() { return assignedCenterId; }

    public String getRequestedCenterId() { return requestedCenterId; }
}
