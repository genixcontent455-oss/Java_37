/** Outcome of trying to admit a candidate into an examination session. */
public enum AdmissionResult {
    ADMITTED("Admitted."),
    ALREADY_ADMITTED("Duplicate admission blocked: candidate is already admitted for this session."),
    NOT_ENROLLED("Candidate is not enrolled in this examination session."),
    NO_SEAT_ASSIGNED("Candidate has no desk yet (seating not generated, or hall was full)."),
    SESSION_CLOSED("Session is already finalized; no more admissions.");

    private final String description;

    AdmissionResult(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
