import java.io.Console;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

/** Console menu. Business logic lives in the other classes; Main only reads input and prints. */
public class Main {

    private static final String SUBJECT = "4CTGC2011";
    private static final Scanner SCANNER = new Scanner(System.in);

    // Live session state - kept between menu choices.
    private static final CandidateRegistry REGISTRY = new CandidateRegistry();
    private static final ExamHall HALL = new ExamHall("Room 302", "CMR-CENTER-01", 5, 6);
    private static final ExaminationSession SESSION =
            new ExaminationSession("LIVE-" + SUBJECT, SUBJECT, LocalDate.of(2026, 12, 15), HALL);
    private static final SeatingArrangementEngine ENGINE = new SeatingArrangementEngine("CSE", "ECE");
    private static final HallTicketValidator VALIDATOR = new HallTicketValidator(REGISTRY);
    private static boolean seatingGenerated = false;

    public static void main(String[] args) {
        System.out.println("University Examination Seating Plan & Hall Ticket Validator");
        System.out.println("Course " + SUBJECT + " | CMR University | " + HALL.getInfo());
        boolean running = true;
        while (running) {
            try {
                printMenu();
                int choice = parseMenuChoice(SCANNER.nextLine());
                running = handle(choice);
            } catch (NoSuchElementException e) {          // input stream closed (Ctrl+D / end of piped input)
                System.out.println("\nInput closed. Exiting.");
                running = false;
            } catch (RuntimeException e) {                // last safety net: never crash the menu
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    /** @return 1-10, or -1 when the text is not a valid menu number */
    static int parseMenuChoice(String text) {
        if (text == null) {
            return -1;
        }
        try {
            int value = Integer.parseInt(text.trim());
            return (value >= 1 && value <= 10) ? value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void printMenu() {
        System.out.println("\n===== MENU =====");
        System.out.println("1. Register Candidate");
        System.out.println("2. Display Registered Candidates");
        System.out.println("3. Generate Seating Arrangement");
        System.out.println("4. Display Seating Plan");
        System.out.println("5. Validate Hall Ticket");
        System.out.println("6. Admit Candidate");
        System.out.println("7. Display Candidate Seat");
        System.out.println("8. Display Attendance Report");
        System.out.println("9. Run Complete Demonstration");
        System.out.println("10. Exit");
        System.out.print("Choose (1-10): ");
    }

    private static boolean handle(int choice) {
        switch (choice) {
            case 1: registerCandidate(); break;
            case 2: displayCandidates(); break;
            case 3: generateSeating(); break;
            case 4: displaySeating(); break;
            case 5: validateTicket(false); break;
            case 6: validateTicket(true); break;
            case 7: displaySeat(); break;
            case 8: attendanceReport(); break;
            case 9: Demonstration.run(); break;
            case 10: System.out.println("Goodbye."); return false;
            default: System.out.println("Invalid choice. Please enter a number from 1 to 10.");
        }
        return true;
    }

    private static void registerCandidate() {
        if (SESSION.isFinalized()) {
            System.out.println("Session is finalized; registration is closed.");
            return;
        }
        try {
            String usn = ask("USN (e.g. 1CR24CS045): ");
            String name = ask("Student name: ");
            String dept = ask("Department (CSE/ECE): ");
            String subject = ask("Subject code [" + SUBJECT + "]: ");
            if (subject.isEmpty()) subject = SUBJECT;
            String center = ask("Examination center [" + HALL.getExamCenterId() + "]: ");
            if (center.isEmpty()) center = HALL.getExamCenterId();
            String secret = readSecret("Hall ticket credential (6-64 letters/digits/-/_): ");
            // The raw secret is hashed immediately and never stored or printed.
            Candidate candidate = new Candidate(usn, name, dept, subject, center,
                    HallTicketValidator.sha256Hex(secret.trim()));
            if (!SUBJECT.equals(candidate.getSubjectCode())) {
                System.out.println("Rejected: this session is for subject " + SUBJECT + ".");
                return;
            }
            REGISTRY.register(candidate);
            SESSION.enroll(candidate);
            System.out.println("Registered " + candidate.getUsn() + ".");
            if (seatingGenerated) {
                System.out.println("Note: regenerate the seating (option 3) to give this candidate a desk.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Registration rejected: " + e.getMessage());
        }
    }

    private static void displayCandidates() {
        if (REGISTRY.size() == 0) {
            System.out.println("No candidates registered yet.");
            return;
        }
        for (Candidate c : REGISTRY.getAllCandidates()) {
            c.displayDetails();
        }
        System.out.println("Total: " + REGISTRY.size());
    }

    private static void generateSeating() {
        if (SESSION.isFinalized()) {
            System.out.println("Session is finalized; seating cannot change.");
            return;
        }
        if (SESSION.getAdmittedCount() > 0) {
            System.out.println("Seating is locked: candidates have already been admitted.");
            return;
        }
        List<Candidate> toSeat = new ArrayList<>(REGISTRY.getAllCandidates());
        AllocationResult result = ENGINE.allocate(toSeat, HALL);
        seatingGenerated = true;
        if (toSeat.isEmpty()) {
            System.out.println("No candidates registered; hall left empty.");
        }
        System.out.println(result.getSummary());
    }

    private static void displaySeating() {
        if (!seatingGenerated) {
            System.out.println("Generate the seating arrangement first (option 3).");
            return;
        }
        ENGINE.displaySeatingMatrix(HALL);
    }

    /** Option 5 (admit=false) only validates; option 6 (admit=true) validates and then admits. */
    private static void validateTicket(boolean admit) {
        String usn = ask("Candidate USN: ");
        String credential = readSecret("Scan hall ticket (type credential; barcode scanner simulated): ");
        try {
            Candidate candidate = VALIDATOR.validateEntry(usn, credential, HALL.getExamCenterId());
            System.out.println("Hall ticket VALID for " + candidate.getStudentName() + ".");
            if (admit) {
                AdmissionResult outcome = SESSION.admit(candidate);
                System.out.println(outcome == AdmissionResult.ADMITTED
                        ? "Admitted. Desk: " + candidate.getAssignedDesk()
                        : "Not admitted: " + outcome.getDescription());
            }
        } catch (InvalidHallTicketException e) {
            System.out.println("DENIED (invalid hall ticket): " + e.getMessage());
        } catch (WrongExamCenterException e) {
            System.out.println("DENIED (wrong center): " + e.getMessage());
        }
    }

    private static void displaySeat() {
        Optional<Candidate> found = REGISTRY.find(ask("Candidate USN: "));
        if (found.isPresent()) {
            Candidate c = found.get();
            System.out.println(c.getStudentName() + " -> desk " + c.getAssignedDesk());
        } else {
            System.out.println("No such registered candidate.");
        }
    }

    private static void attendanceReport() {
        if (!SESSION.isFinalized()) {
            String answer = ask("Finalize the session now? Admissions will close (y/n): ");
            if (answer.equalsIgnoreCase("y")) {
                int absent = SESSION.finalizeSession();
                System.out.println("Session finalized. Absentees: " + absent);
            }
        }
        SESSION.printAttendanceReport();
    }

    private static String ask(String label) {
        System.out.print(label);
        return SCANNER.nextLine().trim();
    }

    /** Hides typing when a real console exists; otherwise falls back to a visible prompt. */
    private static String readSecret(String label) {
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword("%s", label);
            if (chars == null) {
                throw new NoSuchElementException();
            }
            String value = new String(chars);
            Arrays.fill(chars, ' ');
            return value;
        }
        System.out.print(label + "(visible) ");
        return SCANNER.nextLine();
    }
}
