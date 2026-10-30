import java.util.regex.Pattern;

/**
 * One registered examination candidate (encapsulated: all fields are private).
 *
 * The raw hall-ticket secret is NEVER stored; only its SHA-256 hash (64 hex characters).
 * Extra field vs. the brief: examCenterId, needed for the "wrong examination center" rule.
 */
public class Candidate {

    /** Value of assignedDesk until the seating engine allocates a real desk. */
    public static final String DEFAULT_DESK = "NOT_ASSIGNED";

    private static final Pattern USN_PATTERN = Pattern.compile("^[0-9][A-Z]{2}[0-9]{2}[A-Z]{2}[0-9]{3}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z .'-]{0,59}$");
    private static final Pattern DEPT_PATTERN = Pattern.compile("^[A-Z]{2,10}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{4,15}$");
    private static final Pattern CENTER_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9-]{1,29}$");
    private static final Pattern HASH_PATTERN = Pattern.compile("^[0-9a-f]{64}$");
    private static final Pattern DESK_PATTERN = Pattern.compile("^[A-Z]-[0-9]{1,3}$");

    private final String usn;            // unique identifier, cannot change after creation
    private String studentName;
    private String department;
    private String subjectCode;
    private String examCenterId;
    private String hallTicketHash;       // expected SHA-256 hash, lowercase hex
    private String assignedDesk = DEFAULT_DESK;

    public Candidate(String usn, String studentName, String department, String subjectCode,
                     String examCenterId, String hallTicketHash) {
        this.usn = validate(usn, "USN", USN_PATTERN, true);
        setStudentName(studentName);
        setDepartment(department);
        setSubjectCode(subjectCode);
        setExamCenterId(examCenterId);
        setHallTicketHash(hallTicketHash);
    }

    /** Trim, optionally upper-case, then check against a pattern. Never echoes secret values. */
    private static String validate(String value, String field, Pattern pattern, boolean upperCase) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        String cleaned = value.trim();
        if (upperCase) {
            cleaned = cleaned.toUpperCase();
        }
        if (!pattern.matcher(cleaned).matches()) {
            throw new IllegalArgumentException(field + " has an invalid format.");
        }
        return cleaned;
    }

    public String getUsn() { return usn; }
    public String getStudentName() { return studentName; }
    public String getDepartment() { return department; }
    public String getSubjectCode() { return subjectCode; }
    public String getExamCenterId() { return examCenterId; }
    public String getHallTicketHash() { return hallTicketHash; }
    public String getAssignedDesk() { return assignedDesk; }

    public final void setStudentName(String studentName) {
        this.studentName = validate(studentName, "Student name", NAME_PATTERN, false);
    }

    public final void setDepartment(String department) {
        this.department = validate(department, "Department", DEPT_PATTERN, true);
    }

    public final void setSubjectCode(String subjectCode) {
        this.subjectCode = validate(subjectCode, "Subject code", CODE_PATTERN, true);
    }

    public final void setExamCenterId(String examCenterId) {
        this.examCenterId = validate(examCenterId, "Examination center ID", CENTER_PATTERN, true);
    }

    public final void setHallTicketHash(String hallTicketHash) {
        this.hallTicketHash = validate(hallTicketHash, "Hall ticket hash", HASH_PATTERN, false);
    }

    public void setAssignedDesk(String desk) {
        this.assignedDesk = validate(desk, "Desk", DESK_PATTERN, true);
    }

    /** Puts the candidate back to the "no desk" state (used before re-allocation). */
    public void clearAssignedDesk() {
        this.assignedDesk = DEFAULT_DESK;
    }

    public boolean hasAssignedDesk() {
        return !DEFAULT_DESK.equals(assignedDesk);
    }

    /** Human-readable details. The hash is deliberately left out. */
    public String getDetails() {
        return String.format("USN: %-11s Name: %-10s Dept: %-4s Subject: %-10s Center: %-14s Desk: %s",
                usn, studentName, department, subjectCode, examCenterId, assignedDesk);
    }

    public void displayDetails() {
        System.out.println(getDetails());
    }

    @Override
    public String toString() {
        return getDetails();
    }

    /** Two candidates are the same person if the USN matches. */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Candidate)) return false;
        return usn.equals(((Candidate) other).usn);
    }

    @Override
    public int hashCode() {
        return usn.hashCode();
    }
}
