import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Honest report of what the seating engine did (nothing is hidden or silently dropped). */
public class AllocationResult {

    private final int seatedCount;
    private final int seatedByFallback;
    private final List<Candidate> unallocated;
    private final int unknownDepartmentCount;
    private final int duplicateInputCount;
    private final int adjacentSameDepartmentPairs;

    public AllocationResult(int seatedCount, int seatedByFallback, List<Candidate> unallocated,
                            int unknownDepartmentCount, int duplicateInputCount,
                            int adjacentSameDepartmentPairs) {
        this.seatedCount = seatedCount;
        this.seatedByFallback = seatedByFallback;
        this.unallocated = new ArrayList<>(unallocated);
        this.unknownDepartmentCount = unknownDepartmentCount;
        this.duplicateInputCount = duplicateInputCount;
        this.adjacentSameDepartmentPairs = adjacentSameDepartmentPairs;
    }

    public int getSeatedCount() { return seatedCount; }
    public int getSeatedByFallback() { return seatedByFallback; }
    public List<Candidate> getUnallocated() { return Collections.unmodifiableList(unallocated); }
    public int getUnknownDepartmentCount() { return unknownDepartmentCount; }
    public int getDuplicateInputCount() { return duplicateInputCount; }
    public int getAdjacentSameDepartmentPairs() { return adjacentSameDepartmentPairs; }

    /** True only if NO two horizontally/vertically adjacent occupied desks share a department. */
    public boolean isConstraintSatisfied() {
        return adjacentSameDepartmentPairs == 0;
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Seated candidates      : ").append(seatedCount).append('\n');
        sb.append("Not seated             : ").append(unallocated.size()).append('\n');
        if (unknownDepartmentCount > 0) {
            sb.append("  (unsupported department: ").append(unknownDepartmentCount).append(")\n");
        }
        if (duplicateInputCount > 0) {
            sb.append("Duplicate inputs ignored: ").append(duplicateInputCount).append('\n');
        }
        sb.append("Seated by fallback     : ").append(seatedByFallback).append('\n');
        sb.append("Adjacent same-dept pairs: ").append(adjacentSameDepartmentPairs).append('\n');
        sb.append(isConstraintSatisfied()
                ? "Checkerboard constraint : SATISFIED"
                : "Checkerboard constraint : NOT fully satisfied (fallback was needed)");
        if (!unallocated.isEmpty()) {
            sb.append("\nNOT SEATED (action needed - another hall required):");
            for (Candidate c : unallocated) {
                sb.append("\n  - ").append(c.getUsn()).append(' ').append(c.getStudentName())
                  .append(" (").append(c.getDepartment()).append(')');
            }
        }
        return sb.toString();
    }
}
