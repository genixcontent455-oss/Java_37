import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fills an ExamHall with a checkerboard of two departments.
 *
 * ALGORITHM
 *  Pass 1 (strict): nested loops visit every desk (row, col). Desks where (row+col) is even
 *    take the "even" department, odd desks take the other one. Because neighbours always
 *    differ in (row+col) parity, left/right and up/down neighbours always differ in department.
 *    If the needed department has run out, the desk stays EMPTY (this keeps the rule true).
 *  Pass 2 (documented fallback): candidates still waiting (the larger department) are put on
 *    remaining empty desks, choosing the desk with the FEWEST same-department neighbours.
 *    This may break the rule, so the result reports the exact number of violating pairs.
 *  If the hall is full, remaining candidates are returned as "unallocated", never dropped.
 *
 * Department names are constructor parameters, not hard-coded.
 */
public class SeatingArrangementEngine {

    private static final int[] ROW_STEP = {-1, 1, 0, 0};
    private static final int[] COL_STEP = {0, 0, -1, 1};

    private final String firstDepartment;
    private final String secondDepartment;
    private final boolean fallbackEnabled;

    public SeatingArrangementEngine(String firstDepartment, String secondDepartment) {
        this(firstDepartment, secondDepartment, true);
    }

    /** @param fallbackEnabled false = strict mode: never place same-department neighbours */
    public SeatingArrangementEngine(String firstDepartment, String secondDepartment,
                                    boolean fallbackEnabled) {
        if (firstDepartment == null || secondDepartment == null
                || firstDepartment.trim().isEmpty() || secondDepartment.trim().isEmpty()) {
            throw new IllegalArgumentException("Both department names are required.");
        }
        this.firstDepartment = firstDepartment.trim().toUpperCase();
        this.secondDepartment = secondDepartment.trim().toUpperCase();
        if (this.firstDepartment.equals(this.secondDepartment)) {
            throw new IllegalArgumentException("The two departments must be different.");
        }
        this.fallbackEnabled = fallbackEnabled;
    }

    public AllocationResult allocate(List<Candidate> candidates, ExamHall hall) {
        if (candidates == null || hall == null) {
            throw new IllegalArgumentException("Candidate list and hall are required.");
        }
        hall.clearSeating();

        Deque<Candidate> firstQueue = new ArrayDeque<>();
        Deque<Candidate> secondQueue = new ArrayDeque<>();
        List<Candidate> unallocated = new ArrayList<>();
        Set<String> seenUsns = new HashSet<>();
        int duplicateInputs = 0;
        int unknownDepartment = 0;

        // Sort candidates into one queue per department; guarantees one desk per USN.
        for (Candidate candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            if (!seenUsns.add(candidate.getUsn())) {
                duplicateInputs++;
                continue;
            }
            candidate.clearAssignedDesk();
            if (candidate.getDepartment().equals(firstDepartment)) {
                firstQueue.add(candidate);
            } else if (candidate.getDepartment().equals(secondDepartment)) {
                secondQueue.add(candidate);
            } else {
                unallocated.add(candidate);
                unknownDepartment++;
            }
        }

        // The larger department gets the "even" desks (there are never fewer even desks).
        boolean secondIsLarger = secondQueue.size() > firstQueue.size();
        Deque<Candidate> evenQueue = secondIsLarger ? secondQueue : firstQueue;
        Deque<Candidate> oddQueue = secondIsLarger ? firstQueue : secondQueue;

        int seated = 0;
        // ---- Pass 1: strict checkerboard ----
        for (int r = 0; r < hall.getRows(); r++) {
            for (int c = 0; c < hall.getColumns(); c++) {
                Deque<Candidate> source = ((r + c) % 2 == 0) ? evenQueue : oddQueue;
                if (!source.isEmpty()) {
                    seat(hall, r, c, source.poll());
                    seated++;
                }
            }
        }

        // ---- Pass 2: fallback for whoever is left ----
        List<Candidate> leftovers = new ArrayList<>(evenQueue);
        leftovers.addAll(oddQueue);
        int seatedByFallback = 0;
        for (Candidate candidate : leftovers) {
            int[] desk = fallbackEnabled ? findLeastConflictingEmptyDesk(hall, candidate) : null;
            if (desk == null) {
                unallocated.add(candidate);          // reported, never silently discarded
            } else {
                seat(hall, desk[0], desk[1], candidate);
                seated++;
                seatedByFallback++;
            }
        }

        return new AllocationResult(seated, seatedByFallback, unallocated, unknownDepartment,
                duplicateInputs, countAdjacentSameDepartmentPairs(hall));
    }

    /** Puts the candidate in the matrix AND records the desk on the Candidate object. */
    private void seat(ExamHall hall, int row, int column, Candidate candidate) {
        hall.assignSeat(row, column, candidate);
        candidate.setAssignedDesk(ExamHall.deskId(row, column));
    }

    /** Scans all desks (nested loops) for the empty one with the fewest same-department neighbours. */
    private int[] findLeastConflictingEmptyDesk(ExamHall hall, Candidate candidate) {
        int[] best = null;
        int bestConflicts = Integer.MAX_VALUE;
        for (int r = 0; r < hall.getRows(); r++) {
            for (int c = 0; c < hall.getColumns(); c++) {
                if (hall.getSeat(r, c) != null) {
                    continue;
                }
                int conflicts = countSameDepartmentNeighbours(hall, r, c, candidate.getDepartment());
                if (conflicts < bestConflicts) {
                    bestConflicts = conflicts;
                    best = new int[] {r, c};
                }
            }
        }
        return best;
    }

    private int countSameDepartmentNeighbours(ExamHall hall, int row, int column, String department) {
        int count = 0;
        for (int i = 0; i < ROW_STEP.length; i++) {
            int nr = row + ROW_STEP[i];
            int nc = column + COL_STEP[i];
            if (nr < 0 || nr >= hall.getRows() || nc < 0 || nc >= hall.getColumns()) {
                continue;
            }
            Candidate neighbour = hall.getSeat(nr, nc);
            if (neighbour != null && neighbour.getDepartment().equals(department)) {
                count++;
            }
        }
        return count;
    }

    /** Counts each adjacent pair once (checks only the right and down neighbour of every desk). */
    public int countAdjacentSameDepartmentPairs(ExamHall hall) {
        int pairs = 0;
        for (int r = 0; r < hall.getRows(); r++) {
            for (int c = 0; c < hall.getColumns(); c++) {
                Candidate current = hall.getSeat(r, c);
                if (current == null) {
                    continue;
                }
                if (c + 1 < hall.getColumns() && sameDepartment(current, hall.getSeat(r, c + 1))) {
                    pairs++;
                }
                if (r + 1 < hall.getRows() && sameDepartment(current, hall.getSeat(r + 1, c))) {
                    pairs++;
                }
            }
        }
        return pairs;
    }

    private boolean sameDepartment(Candidate a, Candidate b) {
        return b != null && a.getDepartment().equals(b.getDepartment());
    }

    /** Readable table. Cell = "DEPT last-3-of-USN"; empty desks show [EMPTY]. */
    public String formatSeatingMatrix(ExamHall hall) {
        final int width = 11;
        StringBuilder sb = new StringBuilder();
        sb.append(hall.getInfo()).append('\n');
        sb.append("Row\\Col ");
        for (int c = 0; c < hall.getColumns(); c++) {
            sb.append(String.format("%-" + width + "d", c + 1));
        }
        sb.append('\n');
        for (int r = 0; r < hall.getRows(); r++) {
            sb.append(String.format("   %c    ", (char) ('A' + r)));
            for (int c = 0; c < hall.getColumns(); c++) {
                Candidate seatHolder = hall.getSeat(r, c);
                String cell = (seatHolder == null)
                        ? "[EMPTY]"
                        : seatHolder.getDepartment() + " "
                          + seatHolder.getUsn().substring(seatHolder.getUsn().length() - 3);
                sb.append(String.format("%-" + width + "s", cell));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public void displaySeatingMatrix(ExamHall hall) {
        System.out.print(formatSeatingMatrix(hall));
    }
}
