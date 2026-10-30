import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Dependency-free test runner (JUnit needs a download). Prints PASS/FAIL per case, exit code 1 on failure. */
public class TestRunner {

    private static final String SUBJECT = "4CTGC2011";
    private static final String CENTER = "CMR-CENTER-01";
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        t01Registration();
        t02Duplicate();
        t03to05Allocation();
        t06Unequal();
        t07Empty();
        t08Capacity();
        t09to13Validation();
        t14Duplicate();
        t15Absentees();
        t16MenuInput();
        t17OneSeat();
        t18MultiSession();
        System.out.println("\nPassed: " + passed + "  Failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    // ---------- helpers ----------
    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (ok) passed++; else failed++;
    }

    private static String secret(String usn) { return "SYN-" + usn; }

    private static Candidate cand(String usn, String dept) {
        return new Candidate(usn, "Student", dept, SUBJECT, CENTER, HallTicketHelper.hash(secret(usn)));
    }

    /** Tiny wrapper so tests read clearly. */
    private static final class HallTicketHelper {
        static String hash(String s) { return HallTicketValidator.sha256Hex(s); }
    }

    private static List<Candidate> make(String dept, String prefix, int count) {
        List<Candidate> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(cand("1CR24" + prefix + String.format("%03d", i), dept));
        }
        return list;
    }

    private static List<Candidate> mix(int cse, int ece) {
        List<Candidate> list = make("CSE", "CS", cse);
        list.addAll(make("ECE", "EC", ece));
        return list;
    }

    private static ExaminationSession session(String id, ExamHall hall) {
        return new ExaminationSession(id, SUBJECT, LocalDate.of(2026, 12, 15), hall);
    }

    /** Independent re-check of the matrix (does not use the engine's own counter). */
    private static int[] adjacentSameDept(ExamHall hall) {
        int horizontal = 0, vertical = 0;
        for (int r = 0; r < hall.getRows(); r++) {
            for (int c = 0; c < hall.getColumns(); c++) {
                Candidate a = hall.getSeat(r, c);
                if (a == null) continue;
                if (c + 1 < hall.getColumns() && hall.getSeat(r, c + 1) != null
                        && hall.getSeat(r, c + 1).getDepartment().equals(a.getDepartment())) horizontal++;
                if (r + 1 < hall.getRows() && hall.getSeat(r + 1, c) != null
                        && hall.getSeat(r + 1, c).getDepartment().equals(a.getDepartment())) vertical++;
            }
        }
        return new int[] {horizontal, vertical};
    }

    // ---------- tests ----------
    private static void t01Registration() {
        CandidateRegistry reg = new CandidateRegistry();
        reg.register(cand("1CR24CS045", "CSE"));
        check("T01 registration succeeds", reg.size() == 1 && reg.isRegistered("1cr24cs045"));
    }

    private static void t02Duplicate() {
        CandidateRegistry reg = new CandidateRegistry();
        reg.register(cand("1CR24CS045", "CSE"));
        boolean rejected = false;
        try { reg.register(cand("1CR24CS045", "CSE")); } catch (IllegalArgumentException e) { rejected = true; }
        check("T02 duplicate USN rejected, size stays 1", rejected && reg.size() == 1);
    }

    private static void t03to05Allocation() {
        ExamHall hall = new ExamHall("Room 302", CENTER, 5, 6);
        AllocationResult res = new SeatingArrangementEngine("CSE", "ECE").allocate(mix(10, 10), hall);
        int[] adj = adjacentSameDept(hall);
        check("T03 allocation seats all 20, none unallocated",
                res.getSeatedCount() == 20 && res.getUnallocated().isEmpty() && hall.getOccupiedSeats() == 20);
        check("T04 no horizontal same-department neighbours", adj[0] == 0);
        check("T05 no vertical same-department neighbours", adj[1] == 0 && res.isConstraintSatisfied());
    }

    private static void t06Unequal() {
        ExamHall h1 = new ExamHall("R1", CENTER, 5, 6);
        AllocationResult ok = new SeatingArrangementEngine("CSE", "ECE").allocate(mix(12, 10), h1);
        check("T06a 12 CSE + 10 ECE: all seated, rule kept via empty desks",
                ok.getSeatedCount() == 22 && ok.isConstraintSatisfied() && h1.getOccupiedSeats() == 22);
        ExamHall h2 = new ExamHall("R2", CENTER, 5, 6);
        AllocationResult fb = new SeatingArrangementEngine("CSE", "ECE").allocate(mix(20, 5), h2);
        int[] adj = adjacentSameDept(h2);
        check("T06b 20 CSE + 5 ECE: all 25 seated by fallback, violation reported honestly",
                fb.getSeatedCount() == 25 && fb.getSeatedByFallback() == 5
                        && !fb.isConstraintSatisfied() && adj[0] + adj[1] == fb.getAdjacentSameDepartmentPairs());
        ExamHall h3 = new ExamHall("R3", CENTER, 5, 6);
        AllocationResult strict = new SeatingArrangementEngine("CSE", "ECE", false).allocate(mix(20, 5), h3);
        check("T06c strict mode: rule kept, 5 CSE reported as not seated",
                strict.isConstraintSatisfied() && strict.getUnallocated().size() == 5);
    }

    private static void t07Empty() {
        ExamHall hall = new ExamHall("R", CENTER, 5, 6);
        AllocationResult res = new SeatingArrangementEngine("CSE", "ECE").allocate(new ArrayList<>(), hall);
        check("T07 empty list: no crash, nothing seated", res.getSeatedCount() == 0 && hall.getOccupiedSeats() == 0
                && new SeatingArrangementEngine("CSE", "ECE").formatSeatingMatrix(hall).contains("[EMPTY]"));
    }

    private static void t08Capacity() {
        ExamHall hall = new ExamHall("R", CENTER, 5, 6);
        List<Candidate> all = mix(20, 20);
        AllocationResult res = new SeatingArrangementEngine("CSE", "ECE").allocate(all, hall);
        int withDesk = 0;
        for (Candidate c : all) if (c.hasAssignedDesk()) withDesk++;
        check("T08 capacity exceeded: 30 seated, 10 reported, none lost",
                res.getSeatedCount() == 30 && res.getUnallocated().size() == 10
                        && withDesk == 30 && res.getSeatedCount() + res.getUnallocated().size() == 40);
    }

    private static void t09to13Validation() {
        CandidateRegistry reg = new CandidateRegistry();
        reg.register(cand("1CR24CS045", "CSE"));
        HallTicketValidator v = new HallTicketValidator(reg);
        boolean ok = false;
        try { ok = v.validateEntry("1CR24CS045", secret("1CR24CS045"), CENTER).getUsn().equals("1CR24CS045"); }
        catch (Exception e) { ok = false; }
        check("T09 correct credential accepted / T12 correct center accepted", ok);

        InvalidHallTicketException.Reason r = null;
        try { v.validateEntry("1CR24CS045", "SYN-WRONG-1", CENTER); } catch (InvalidHallTicketException e) { r = e.getReason();
            check("T10b message hides hash", !e.getMessage().contains(reg.find("1CR24CS045").get().getHallTicketHash())); }
        catch (WrongExamCenterException e) { }
        check("T10 wrong credential -> HASH_MISMATCH", r == InvalidHallTicketException.Reason.HASH_MISMATCH);

        r = null;
        try { v.validateEntry("1CR24CS045", "  ", CENTER); } catch (InvalidHallTicketException e) { r = e.getReason(); }
        catch (WrongExamCenterException e) { }
        check("T10c empty credential -> MALFORMED_CREDENTIAL", r == InvalidHallTicketException.Reason.MALFORMED_CREDENTIAL);

        r = null;
        try { v.validateEntry("1CR24CS999", secret("1CR24CS999"), CENTER); } catch (InvalidHallTicketException e) { r = e.getReason(); }
        catch (WrongExamCenterException e) { }
        check("T11 unregistered candidate -> NOT_REGISTERED", r == InvalidHallTicketException.Reason.NOT_REGISTERED);

        boolean wrongCenter = false;
        try { v.validateEntry("1CR24CS045", secret("1CR24CS045"), "CMR-CENTER-02"); }
        catch (WrongExamCenterException e) { wrongCenter = true; } catch (InvalidHallTicketException e) { }
        check("T13 valid candidate at wrong center -> WrongExamCenterException", wrongCenter);

        boolean wrongCredWrongCenter = false;
        try { v.validateEntry("1CR24CS045", "SYN-WRONG-1", "CMR-CENTER-02"); }
        catch (InvalidHallTicketException e) { wrongCredWrongCenter = true; } catch (WrongExamCenterException e) { }
        check("T13b bad credential at wrong center -> reported as invalid ticket, not center", wrongCredWrongCenter);
    }

    private static void t14Duplicate() {
        ExamAudit.resetAll();
        ExamHall hall = new ExamHall("R", CENTER, 5, 6);
        ExaminationSession s = session("T14", hall);
        List<Candidate> list = mix(2, 2);
        for (Candidate c : list) s.enroll(c);
        new SeatingArrangementEngine("CSE", "ECE").allocate(list, hall);
        Candidate first = list.get(0);
        AdmissionResult a = s.admit(first);
        AdmissionResult b = s.admit(first);
        check("T14 duplicate admission blocked, count stays 1",
                a == AdmissionResult.ADMITTED && b == AdmissionResult.ALREADY_ADMITTED && s.getAdmittedCount() == 1);
        check("T14b re-enrolling same candidate does not inflate registered count",
                !s.enroll(first) && s.getRegisteredCount() == 4);
        Candidate noSeat = cand("1CR24CS777", "CSE");
        s.enroll(noSeat);
        check("T14c candidate without desk is not admitted", s.admit(noSeat) == AdmissionResult.NO_SEAT_ASSIGNED);
    }

    private static void t15Absentees() {
        ExamAudit.resetAll();
        ExamHall hall = new ExamHall("R", CENTER, 5, 6);
        ExaminationSession s = session("T15", hall);
        List<Candidate> list = mix(3, 2);
        for (Candidate c : list) s.enroll(c);
        new SeatingArrangementEngine("CSE", "ECE").allocate(list, hall);
        s.admit(list.get(0));
        s.admit(list.get(1));
        int absent = s.finalizeSession();
        int again = s.finalizeSession();
        check("T15 absentees = 5 - 2 = 3, finalize twice does not double count",
                absent == 3 && again == 3 && ExamAudit.getTotalAbsentees() == 3);
        check("T15b admission after finalize refused", s.admit(list.get(2)) == AdmissionResult.SESSION_CLOSED);
    }

    private static void t16MenuInput() {
        check("T16 invalid menu input returns -1",
                Main.parseMenuChoice("abc") == -1 && Main.parseMenuChoice("0") == -1
                        && Main.parseMenuChoice("11") == -1 && Main.parseMenuChoice("") == -1
                        && Main.parseMenuChoice(null) == -1 && Main.parseMenuChoice(" 7 ") == 7);
    }

    private static void t17OneSeat() {
        ExamHall hall = new ExamHall("R", CENTER, 5, 6);
        List<Candidate> list = mix(12, 10);
        new SeatingArrangementEngine("CSE", "ECE").allocate(list, hall);
        boolean exactlyOnce = true;
        for (Candidate c : list) {
            int found = 0;
            for (int r = 0; r < hall.getRows(); r++)
                for (int col = 0; col < hall.getColumns(); col++)
                    if (hall.getSeat(r, col) == c) {
                        found++;
                        if (!c.getAssignedDesk().equals(ExamHall.deskId(r, col))) exactlyOnce = false;
                    }
            if (found != 1) exactlyOnce = false;
        }
        // Re-running allocation must not create second seats either.
        new SeatingArrangementEngine("CSE", "ECE").allocate(list, hall);
        check("T17 each candidate on exactly one desk (also after re-allocation)",
                exactlyOnce && hall.getOccupiedSeats() == 22);
    }

    private static void t18MultiSession() {
        ExamAudit.resetAll();
        ExamHall h1 = new ExamHall("R1", CENTER, 5, 6);
        ExamHall h2 = new ExamHall("R2", CENTER, 5, 6);
        ExaminationSession s1 = session("S1", h1);
        ExaminationSession s2 = session("S2", h2);
        Candidate shared = cand("1CR24CS045", "CSE");
        s1.enroll(shared);
        s2.enroll(shared);
        s1.enroll(cand("1CR24EC001", "ECE"));
        new SeatingArrangementEngine("CSE", "ECE").allocate(new ArrayList<>(List.of(shared)), h1);
        s1.admit(shared);
        s1.finalizeSession();
        boolean s2StillOpen = !s2.isFinalized() && s2.getAdmittedCount() == 0;
        check("T18 per-session counters independent; totals = sum over sessions",
                s1.getRegisteredCount() == 2 && s2.getRegisteredCount() == 1 && s2StillOpen
                        && ExamAudit.getTotalRegistered() == 3 && ExamAudit.getTotalAdmitted() == 1
                        && ExamAudit.getTotalAbsentees() == 1);
        ExamAudit.resetSession("S1");
        check("T18b resetSession removes only that session's numbers",
                ExamAudit.getTotalRegistered() == 1 && ExamAudit.getTotalAdmitted() == 0
                        && ExamAudit.getTotalAbsentees() == 0);
    }
}
