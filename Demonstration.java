import java.time.LocalDate;
import java.util.List;

/** Scripted end-to-end run (menu option 9). Uses its OWN objects so live menu state is untouched. */
public final class Demonstration {

    private static final String COURSE = "4CTGC2011";
    private static final String CENTER = "CMR-CENTER-01";
    /** Synthetic credentials only: "DEMO-TICKET-<USN>". Not real data. */
    private static final String PREFIX = "DEMO-TICKET-";
    private static int runCounter = 0;

    private Demonstration() { }

    public static void run() {
        runCounter++;
        String sessionId = "DEMO-" + runCounter;
        CandidateRegistry registry = new CandidateRegistry();
        ExamHall hall = new ExamHall("Room 302", CENTER, 5, 6);
        ExaminationSession session = new ExaminationSession(sessionId, COURSE, LocalDate.of(2026, 12, 15), hall);
        SeatingArrangementEngine engine = new SeatingArrangementEngine("CSE", "ECE");
        HallTicketValidator validator = new HallTicketValidator(registry);

        step("1. Register synthetic candidates (12 CSE + 10 ECE)");
        String[] cseNames = {"Arjun", "Bhavana", "Charan", "Divya", "Rahul", "Farhan",
                             "Gayathri", "Harsha", "Isha", "Jayanth", "Kavya", "Lokesh"};
        String[] eceNames = {"Meera", "Nikhil", "Omkar", "Pooja", "Qadir",
                             "Rhea", "Suresh", "Tanvi", "Uday", "Vani"};
        for (int i = 0; i < cseNames.length; i++) {
            register(registry, session, "1CR24CS" + String.format("%03d", 41 + i), cseNames[i], "CSE");
        }
        for (int i = 0; i < eceNames.length; i++) {
            register(registry, session, "1CR24EC" + String.format("%03d", 1 + i), eceNames[i], "ECE");
        }
        System.out.println("Registered: " + registry.size() + " | audit registered: " + session.getRegisteredCount());

        step("2. Try to register Rahul (1CR24CS045) a second time");
        try {
            registry.register(newCandidate("1CR24CS045", "Rahul", "CSE"));
            System.out.println("ERROR: duplicate was accepted!");
        } catch (IllegalArgumentException e) {
            System.out.println("Rejected -> " + e.getMessage());
        }
        System.out.println("Registered still: " + registry.size() + " | audit registered still: "
                + session.getRegisteredCount());

        step("3-4. Generate and display the seating plan");
        AllocationResult result = engine.allocate(registry.getAllCandidates(), hall);
        System.out.print(engine.formatSeatingMatrix(hall));
        System.out.println(result.getSummary());

        step("5. Assigned desk of every candidate");
        for (Candidate c : registry.getAllCandidates()) {
            System.out.println(c.getDetails());
        }

        step("6-9. Valid scan for Rahul: credential, center, then admit");
        String rahulUsn = "1CR24CS045";
        try {
            Candidate rahul = validator.validateEntry(rahulUsn, PREFIX + rahulUsn, hall.getExamCenterId());
            System.out.println("Credential OK, center OK for " + rahul.getStudentName()
                    + " (desk " + rahul.getAssignedDesk() + ")");
            System.out.println("Admission: " + session.admit(rahul));
        } catch (InvalidHallTicketException | WrongExamCenterException e) {
            System.out.println("Unexpected rejection: " + e.getMessage());
        }

        step("10-11. Invalid hall ticket (wrong credential) for Rahul");
        attempt(validator, rahulUsn, "DEMO-WRONG-0000", hall.getExamCenterId());

        step("11b. Malformed (too short / bad characters) credential");
        attempt(validator, rahulUsn, "!!", hall.getExamCenterId());

        step("11c. Unregistered candidate");
        attempt(validator, "1CR24CS999", PREFIX + "1CR24CS999", hall.getExamCenterId());

        step("12-13. Valid candidate (1CR24EC003) at the wrong center");
        attempt(validator, "1CR24EC003", PREFIX + "1CR24EC003", "CMR-CENTER-02");

        step("14. Duplicate admission for Rahul");
        try {
            Candidate rahul = validator.validateEntry(rahulUsn, PREFIX + rahulUsn, hall.getExamCenterId());
            System.out.println("Admission: " + session.admit(rahul).getDescription());
        } catch (InvalidHallTicketException | WrongExamCenterException e) {
            System.out.println("Unexpected rejection: " + e.getMessage());
        }
        System.out.println("Admitted count unchanged: " + session.getAdmittedCount());

        step("Admit 4 more candidates (each one fully validated)");
        List<Candidate> all = registry.getAllCandidates();
        for (Candidate c : all) {
            if (session.getAdmittedCount() >= 5) {
                break;
            }
            if (!session.isAdmitted(c.getUsn())) {
                attemptAdmit(validator, session, c.getUsn(), hall.getExamCenterId());
            }
        }

        step("15-17. Finalize session, absentees, audit report");
        int absentees = session.finalizeSession();
        System.out.println("Absentees = registered " + session.getRegisteredCount()
                + " - admitted " + session.getAdmittedCount() + " = " + absentees);
        session.printAttendanceReport();
        ExamAudit.printOverallReport();
        System.out.println("Note: the overall report also includes any other sessions in this run.");
        System.out.println("Demo credentials are synthetic. Real scanner integration is out of scope.");
    }

    private static void step(String title) {
        System.out.println("\n##### " + title + " #####");
    }

    private static Candidate newCandidate(String usn, String name, String dept) {
        return new Candidate(usn, name, dept, COURSE, CENTER, HallTicketValidator.sha256Hex(PREFIX + usn));
    }

    private static void register(CandidateRegistry registry, ExaminationSession session,
                                 String usn, String name, String dept) {
        Candidate c = newCandidate(usn, name, dept);
        registry.register(c);
        session.enroll(c);
    }

    private static void attempt(HallTicketValidator validator, String usn, String credential, String center) {
        try {
            validator.validateEntry(usn, credential, center);
            System.out.println("Entry allowed (unexpected for this scenario).");
        } catch (InvalidHallTicketException e) {
            System.out.println("InvalidHallTicketException [" + e.getReason() + "] -> " + e.getMessage());
        } catch (WrongExamCenterException e) {
            System.out.println("WrongExamCenterException -> " + e.getMessage());
        }
    }

    private static void attemptAdmit(HallTicketValidator validator, ExaminationSession session,
                                     String usn, String center) {
        try {
            Candidate c = validator.validateEntry(usn, PREFIX + usn, center);
            System.out.println(usn + " -> " + session.admit(c));
        } catch (InvalidHallTicketException | WrongExamCenterException e) {
            System.out.println(usn + " rejected: " + e.getMessage());
        }
    }
}
