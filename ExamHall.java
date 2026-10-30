/**
 * One examination hall. hallId (e.g. "Room 302") and examCenterId (e.g. "CMR-CENTER-01")
 * are different things: a center is a site that contains many halls.
 */
public class ExamHall {

    public static final int MAX_ROWS = 26;      // desk letters A..Z
    public static final int MAX_COLUMNS = 50;

    private final String hallId;
    private final String examCenterId;
    private final int rows;
    private final int columns;
    private Candidate[][] seatingMatrix;
    private String sessionInfo = "No session attached";

    public ExamHall(String hallId, String examCenterId, int rows, int columns) {
        if (hallId == null || hallId.trim().isEmpty()) {
            throw new IllegalArgumentException("Hall ID is required.");
        }
        if (examCenterId == null || examCenterId.trim().isEmpty()) {
            throw new IllegalArgumentException("Examination center ID is required.");
        }
        if (rows < 1 || rows > MAX_ROWS) {
            throw new IllegalArgumentException("Rows must be between 1 and " + MAX_ROWS + ".");
        }
        if (columns < 1 || columns > MAX_COLUMNS) {
            throw new IllegalArgumentException("Columns must be between 1 and " + MAX_COLUMNS + ".");
        }
        this.hallId = hallId.trim();
        this.examCenterId = examCenterId.trim().toUpperCase();
        this.rows = rows;
        this.columns = columns;
        this.seatingMatrix = new Candidate[rows][columns];   // all null = all desks empty
    }

    /** Desk label for a zero-based position: (0,0) -> "A-1", (1,2) -> "B-3". */
    public static String deskId(int row, int column) {
        return (char) ('A' + row) + "-" + (column + 1);
    }

    public String getHallId() { return hallId; }
    public String getExamCenterId() { return examCenterId; }
    public int getRows() { return rows; }
    public int getColumns() { return columns; }
    public String getSessionInfo() { return sessionInfo; }
    public void setSessionInfo(String sessionInfo) { this.sessionInfo = sessionInfo; }

    public int getCapacity() {
        return rows * columns;
    }

    public int getOccupiedSeats() {
        int count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (seatingMatrix[r][c] != null) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getAvailableSeats() {
        return getCapacity() - getOccupiedSeats();
    }

    public boolean hasCapacityFor(int candidateCount) {
        return candidateCount <= getAvailableSeats();
    }

    public Candidate getSeat(int row, int column) {
        checkBounds(row, column);
        return seatingMatrix[row][column];
    }

    /** Places a candidate on an empty desk; refuses to overwrite or exceed the hall. */
    public void assignSeat(int row, int column, Candidate candidate) {
        checkBounds(row, column);
        if (candidate == null) {
            throw new IllegalArgumentException("Candidate must not be null.");
        }
        if (seatingMatrix[row][column] != null) {
            throw new IllegalStateException("Desk " + deskId(row, column) + " is already occupied.");
        }
        seatingMatrix[row][column] = candidate;
    }

    /** Empties every desk (candidates' own assignedDesk fields are reset by the engine). */
    public void clearSeating() {
        seatingMatrix = new Candidate[rows][columns];
    }

    /** Copy of the matrix, so outside code cannot modify seating without assignSeat(). */
    public Candidate[][] getSeatingMatrix() {
        Candidate[][] copy = new Candidate[rows][];
        for (int r = 0; r < rows; r++) {
            copy[r] = seatingMatrix[r].clone();
        }
        return copy;
    }

    public String getInfo() {
        return String.format("Hall %s | Center %s | %d x %d = %d desks | occupied %d | %s",
                hallId, examCenterId, rows, columns, getCapacity(), getOccupiedSeats(), sessionInfo);
    }

    public void displayInfo() {
        System.out.println(getInfo());
    }

    private void checkBounds(int row, int column) {
        if (row < 0 || row >= rows || column < 0 || column >= columns) {
            throw new IndexOutOfBoundsException("Desk position outside the hall.");
        }
    }
}
