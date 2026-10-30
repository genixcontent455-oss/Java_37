import java.time.LocalDate;

/**
 * One examination sitting (one subject, one date, one hall).
 * Registration/admission facts live in ExamAudit (single source of truth); this class adds
 * the session's own rules: subject match, seat required, no changes after finalization.
 */
public class ExaminationSession {

    private final String sessionId;
    private final String subjectCode;
    private final LocalDate examDate;
    private final ExamHall hall;

    public ExaminationSession(String sessionId, String subjectCode, LocalDate examDate, ExamHall hall) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID is required.");
        }
        if (subjectCode == null || subjectCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject code is required.");
        }
        if (examDate == null || hall == null) {
            throw new IllegalArgumentException("Exam date and hall are required.");
        }
        this.sessionId = sessionId.trim();
        this.subjectCode = subjectCode.trim().toUpperCase();
        this.examDate = examDate;
        this.hall = hall;
        ExamAudit.openSession(this.sessionId);
        hall.setSessionInfo("Session " + this.sessionId + " (" + this.subjectCode + ", " + examDate + ")");
    }

    public String getSessionId() { return sessionId; }
    public String getSubjectCode() { return subjectCode; }
    public LocalDate getExamDate() { return examDate; }
    public ExamHall getHall() { return hall; }

    /**
     * Enrols a candidate for this session.
     * @return true if newly enrolled, false if already enrolled (counter unchanged)
     * @throws IllegalArgumentException if the candidate's subject is different
     * @throws IllegalStateException    if the session is already finalized
     */
    public boolean enroll(Candidate candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("Candidate must not be null.");
        }
        if (isFinalized()) {
            throw new IllegalStateException("Session is finalized; cannot enrol candidates.");
        }
        if (!subjectCode.equals(candidate.getSubjectCode())) {
            throw new IllegalArgumentException("Candidate's subject does not match this session.");
        }
        return ExamAudit.recordRegistration(sessionId, candidate.getUsn());
    }

    /** Call this only AFTER the hall ticket has been validated. */
    public AdmissionResult admit(Candidate candidate) {
        if (isFinalized()) {
            return AdmissionResult.SESSION_CLOSED;
        }
        if (!ExamAudit.isRegistered(sessionId, candidate.getUsn())) {
            return AdmissionResult.NOT_ENROLLED;
        }
        if (!candidate.hasAssignedDesk()) {
            return AdmissionResult.NO_SEAT_ASSIGNED;
        }
        if (ExamAudit.isAdmitted(sessionId, candidate.getUsn())) {
            return AdmissionResult.ALREADY_ADMITTED;
        }
        ExamAudit.recordAdmission(sessionId, candidate.getUsn());
        return AdmissionResult.ADMITTED;
    }

    public boolean isEnrolled(String usn) { return ExamAudit.isRegistered(sessionId, usn); }
    public boolean isAdmitted(String usn) { return ExamAudit.isAdmitted(sessionId, usn); }
    public boolean isFinalized() { return ExamAudit.isFinalized(sessionId); }
    public int getRegisteredCount() { return ExamAudit.getRegistered(sessionId); }
    public int getAdmittedCount() { return ExamAudit.getAdmitted(sessionId); }

    /** Ends the session and returns the absentee count (registered - admitted, never negative). */
    public int finalizeSession() {
        return ExamAudit.finalizeSession(sessionId);
    }

    public String buildAttendanceReport() {
        java.util.List<String> admitted = ExamAudit.getAdmittedUsns(sessionId);
        StringBuilder sb = new StringBuilder();
        sb.append("========== ATTENDANCE REPORT ==========\n");
        sb.append("Session : ").append(sessionId).append(" | Subject: ").append(subjectCode)
          .append(" | Date: ").append(examDate).append('\n');
        sb.append("Hall    : ").append(hall.getHallId()).append(" | Center: ")
          .append(hall.getExamCenterId()).append('\n');
        sb.append(ExamAudit.buildSessionReport(sessionId));
        sb.append("Admitted USNs: ").append(admitted.isEmpty() ? "none" : String.join(", ", admitted)).append('\n');
        if (isFinalized()) {
            java.util.List<String> absent = ExamAudit.getAbsenteeUsns(sessionId);
            sb.append("Absent USNs  : ").append(absent.isEmpty() ? "none" : String.join(", ", absent)).append('\n');
        }
        sb.append("=======================================\n");
        return sb.toString();
    }

    public void printAttendanceReport() {
        System.out.print(buildAttendanceReport());
    }
}
