import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** The authoritative store of registered candidates. USNs are unique. */
public class CandidateRegistry {

    private final List<Candidate> candidates = new ArrayList<>();

    /** @throws IllegalArgumentException if the candidate is null or the USN already exists */
    public void register(Candidate candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("Candidate must not be null.");
        }
        if (find(candidate.getUsn()).isPresent()) {
            throw new IllegalArgumentException("Duplicate USN rejected: " + candidate.getUsn());
        }
        candidates.add(candidate);
    }

    /** Look up by USN (case-insensitive, whitespace ignored). */
    public Optional<Candidate> find(String usn) {
        if (usn == null) {
            return Optional.empty();
        }
        String key = usn.trim().toUpperCase();
        for (Candidate candidate : candidates) {
            if (candidate.getUsn().equals(key)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public boolean isRegistered(String usn) {
        return find(usn).isPresent();
    }

    /** Read-only view; callers cannot add or remove candidates behind the registry's back. */
    public List<Candidate> getAllCandidates() {
        return Collections.unmodifiableList(candidates);
    }

    public int size() {
        return candidates.size();
    }
}
