import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Static audit counters, kept correct by tracking WHO registered/was admitted per session.
 *
 * A counter only goes up when a (sessionId, USN) pair is seen for the FIRST time (Set.add
 * returns true), so registering or admitting the same candidate again changes nothing.
 * Counters are per-session; the static totals are the sum over all sessions.
 * Reset: resetSession(id) removes one session and subtracts its numbers; resetAll() clears everything.
 * Not thread-safe (single-threaded console app).
 */
public final class ExamAudit {

    /** Everything we know about one session. */
    private static final class SessionRecord {
        final Set<String> registered = new LinkedHashSet<>();
        final Set<String> admitted = new LinkedHashSet<>();
        boolean finalized = false;
        int absentees = 0;
    }

    private static final Map<String, SessionRecord> SESSIONS = new LinkedHashMap<>();
    private static int totalRegistered = 0;
    private static int totalAdmitted = 0;
    private static int totalAbsentees = 0;   // only added once, when a session is finalized

    private ExamAudit() { }

    public static void openSession(String sessionId) {
        if (SESSIONS.containsKey(sessionId)) {
            throw new IllegalArgumentException("Session ID already exists: " + sessionId);
        }
        SESSIONS.put(sessionId, new SessionRecord());
    }

    /** @return true only if this USN was newly registered for this session */
    public static boolean recordRegistration(String sessionId, String usn) {
        SessionRecord record = get(sessionId);
        if (record.finalized) {
            return false;
        }
        if (record.registered.add(usn)) {
            totalRegistered++;
            return true;
        }
        return false;
    }

    /** @return true only for a first admission of a USN that is registered in this session */
    public static boolean recordAdmission(String sessionId, String usn) {
        SessionRecord record = get(sessionId);
        if (record.finalized || !record.registered.contains(usn)) {
            return false;
        }
        if (record.admitted.add(usn)) {
            totalAdmitted++;
            return true;
        }
        return false;
    }

    /**
     * Absentees = registered - admitted, never below zero. Calling it again returns the stored
     * value, so absentees are never counted twice.
     */
    public static int finalizeSession(String sessionId) {
        SessionRecord record = get(sessionId);
        if (!record.finalized) {
            record.absentees = Math.max(0, record.registered.size() - record.admitted.size());
            totalAbsentees += record.absentees;
            record.finalized = true;
        }
        return record.absentees;
    }

    public static boolean isRegistered(String sessionId, String usn) {
        return get(sessionId).registered.contains(usn);
    }

    public static boolean isAdmitted(String sessionId, String usn) {
        return get(sessionId).admitted.contains(usn);
    }

    public static boolean isFinalized(String sessionId) {
        return get(sessionId).finalized;
    }

    public static int getRegistered(String sessionId) {
        return get(sessionId).registered.size();
    }

    public static int getAdmitted(String sessionId) {
        return get(sessionId).admitted.size();
    }

    /** Final value after finalizeSession; before that, the "not yet admitted" number. */
    public static int getAbsentees(String sessionId) {
        SessionRecord record = get(sessionId);
        return record.finalized ? record.absentees
                : Math.max(0, record.registered.size() - record.admitted.size());
    }

    /** USNs registered but not admitted. */
    public static List<String> getAbsenteeUsns(String sessionId) {
        SessionRecord record = get(sessionId);
        List<String> result = new ArrayList<>();
        for (String usn : record.registered) {
            if (!record.admitted.contains(usn)) {
                result.add(usn);
            }
        }
        return result;
    }

    /** USNs admitted so far, in admission order. */
    public static List<String> getAdmittedUsns(String sessionId) {
        return new ArrayList<>(get(sessionId).admitted);
    }

    public static int getTotalRegistered() { return totalRegistered; }
    public static int getTotalAdmitted() { return totalAdmitted; }
    public static int getTotalAbsentees() { return totalAbsentees; }

    /** Removes one session and takes its contribution out of the static totals. */
    public static void resetSession(String sessionId) {
        SessionRecord record = SESSIONS.remove(sessionId);
        if (record != null) {
            totalRegistered -= record.registered.size();
            totalAdmitted -= record.admitted.size();
            if (record.finalized) {
                totalAbsentees -= record.absentees;
            }
        }
    }

    public static void resetAll() {
        SESSIONS.clear();
        totalRegistered = 0;
        totalAdmitted = 0;
        totalAbsentees = 0;
    }

    public static String buildSessionReport(String sessionId) {
        SessionRecord record = get(sessionId);
        StringBuilder sb = new StringBuilder();
        sb.append("--- AUDIT: session ").append(sessionId).append(" ---\n");
        sb.append("Status               : ").append(record.finalized ? "FINALIZED" : "OPEN").append('\n');
        sb.append("Registered candidates: ").append(record.registered.size()).append('\n');
        sb.append("Admitted candidates  : ").append(record.admitted.size()).append('\n');
        if (record.finalized) {
            sb.append("Absentee candidates  : ").append(record.absentees).append('\n');
        } else {
            sb.append("Not yet admitted     : ").append(getAbsentees(sessionId))
              .append(" (absentees are fixed when the session is finalized)\n");
        }
        return sb.toString();
    }

    public static String buildOverallReport() {
        return "=== OVERALL AUDIT (all sessions: " + SESSIONS.size() + ") ===\n"
                + "Total registered : " + totalRegistered + '\n'
                + "Total admitted   : " + totalAdmitted + '\n'
                + "Total absentees  : " + totalAbsentees + " (finalized sessions only)\n";
    }

    public static void printSessionReport(String sessionId) {
        System.out.print(buildSessionReport(sessionId));
    }

    public static void printOverallReport() {
        System.out.print(buildOverallReport());
    }

    private static SessionRecord get(String sessionId) {
        SessionRecord record = SESSIONS.get(sessionId);
        if (record == null) {
            throw new IllegalArgumentException("Unknown session: " + sessionId);
        }
        return record;
    }
}
