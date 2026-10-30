import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Checks a presented hall-ticket credential (simulated barcode text) against the stored SHA-256 hash.
 *
 * Order of checks: credential format -> candidate registered -> hash matches -> center matches.
 * The center is checked LAST so WrongExamCenterException only ever concerns a genuine candidate.
 *
 * NOTE: plain SHA-256 is a teaching demo, not production authentication (see README).
 */
public class HallTicketValidator {

    /** Scanner text must be 6-64 characters: letters, digits, '-' or '_'. */
    private static final Pattern CREDENTIAL_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{6,64}$");

    private final CandidateRegistry registry;

    public HallTicketValidator(CandidateRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("Registry is required.");
        }
        this.registry = registry;
    }

    /**
     * @return the authoritative Candidate record if entry is allowed
     * @throws InvalidHallTicketException bad format, unregistered USN, or wrong credential
     * @throws WrongExamCenterException   valid candidate, but requestedCenter is not theirs
     */
    public Candidate validateEntry(String candidateUSN, String presentedCredential,
                                   String requestedCenter)
            throws InvalidHallTicketException, WrongExamCenterException {

        // 1. Reject empty / malformed credentials (trim: scanners often append a newline).
        String credential = (presentedCredential == null) ? "" : presentedCredential.trim();
        if (!CREDENTIAL_PATTERN.matcher(credential).matches()) {
            throw new InvalidHallTicketException(InvalidHallTicketException.Reason.MALFORMED_CREDENTIAL,
                    "Hall ticket credential is empty or malformed.");
        }

        // 2. Always use the registry's record - never a Candidate object supplied by the caller.
        Candidate candidate = registry.find(candidateUSN).orElseThrow(() ->
                new InvalidHallTicketException(InvalidHallTicketException.Reason.NOT_REGISTERED,
                        "Candidate is not registered. Entry denied."));

        // 3. Hash the presented credential and compare in constant time.
        byte[] presentedHash = sha256(credential);
        byte[] expectedHash = HexFormat.of().parseHex(candidate.getHallTicketHash());
        if (!MessageDigest.isEqual(presentedHash, expectedHash)) {
            throw new InvalidHallTicketException(InvalidHallTicketException.Reason.HASH_MISMATCH,
                    "Hall ticket verification failed. Entry denied.");
        }

        // 4. Valid person - now make sure they are at their own examination center.
        if (requestedCenter == null
                || !candidate.getExamCenterId().equalsIgnoreCase(requestedCenter.trim())) {
            throw new WrongExamCenterException(candidate.getExamCenterId(),
                    requestedCenter == null ? "(none)" : requestedCenter.trim().toUpperCase());
        }
        return candidate;
    }

    /** SHA-256 of the UTF-8 bytes of the text. */
    public static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform must provide SHA-256, so this cannot normally happen.
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }

    /** SHA-256 as 64 lowercase hex characters (used when registering a candidate). */
    public static String sha256Hex(String text) {
        return HexFormat.of().formatHex(sha256(text));
    }
}
