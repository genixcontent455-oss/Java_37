# Class Diagram

The Mermaid class diagram below documents classes and relationships present in the Java source. The project does not declare any interfaces, so none are shown.

```mermaid
classDiagram
    class Main {
      -Scanner SCANNER
      -CandidateRegistry REGISTRY
      -ExamHall HALL
      -ExaminationSession SESSION
      -SeatingArrangementEngine ENGINE
      -HallTicketValidator VALIDATOR
      +main(String[] args)
    }

    class Candidate {
      -String usn
      -String studentName
      -String department
      -String subjectCode
      -String examCenterId
      -String hallTicketHash
      -String assignedDesk
      +getUsn()
      +getStudentName()
      +setStudentName(String)
      +setDepartment(String)
      +setAssignedDesk(String)
      +equals(Object) boolean
      +hashCode() int
      +toString() String
    }

    class CandidateRegistry {
      -List~Candidate~ candidates
      +register(Candidate)
      +find(String) Optional
      +isRegistered(String) boolean
      +getAllCandidates() List
      +size() int
    }

    class ExamHall {
      -String hallId
      -String examCenterId
      -int rows
      -int columns
      -Candidate[][] seatingMatrix
      +assignSeat(int, int, Candidate)
      +getSeat(int, int) Candidate
      +clearSeating()
      +getCapacity() int
    }

    class SeatingArrangementEngine {
      -String firstDepartment
      -String secondDepartment
      -boolean fallbackEnabled
      +allocate(List~Candidate~, ExamHall) AllocationResult
      +countAdjacentSameDepartmentPairs(ExamHall) int
      +formatSeatingMatrix(ExamHall) String
    }

    class AllocationResult {
      -int seatedCount
      -int seatedByFallback
      -List~Candidate~ unallocated
      -int unknownDepartmentCount
      -int duplicateInputCount
      -int adjacentSameDepartmentPairs
      +isConstraintSatisfied() boolean
      +getSummary() String
    }

    class HallTicketValidator {
      -CandidateRegistry registry
      +validateEntry(String, String, String) Candidate
      +sha256(String) byte[]
      +sha256Hex(String) String
    }

    class InvalidHallTicketException {
      <<exception>>
      -Reason reason
      +getReason() Reason
    }

    class WrongExamCenterException {
      <<exception>>
      -String assignedCenterId
      -String requestedCenterId
      +getAssignedCenterId() String
      +getRequestedCenterId() String
    }

    class ExaminationSession {
      -String sessionId
      -String subjectCode
      -LocalDate examDate
      -ExamHall hall
      +enroll(Candidate) boolean
      +admit(Candidate) AdmissionResult
      +finalizeSession() int
      +buildAttendanceReport() String
    }

    class ExamAudit {
      -Map~String, SessionRecord~ SESSIONS
      -int totalRegistered
      -int totalAdmitted
      -int totalAbsentees
      +openSession(String)
      +recordRegistration(String, String) boolean
      +recordAdmission(String, String) boolean
      +finalizeSession(String) int
      +resetSession(String)
      +resetAll()
    }

    class AdmissionResult {
      <<enumeration>>
      ADMITTED
      ALREADY_ADMITTED
      NOT_ENROLLED
      NO_SEAT_ASSIGNED
      SESSION_CLOSED
    }

    class Demonstration {
      +run()$
    }

    class TestRunner {
      +main(String[])$
    }

    class Exception {
      <<Java standard class>>
    }

    Main --> CandidateRegistry : owns static registry
    Main --> ExamHall : owns static hall
    Main --> ExaminationSession : owns static session
    Main --> SeatingArrangementEngine : owns static engine
    Main --> HallTicketValidator : owns static validator
    Main ..> Demonstration : option 9 calls
    CandidateRegistry "1" o-- "*" Candidate : stores
    ExamHall "1" o-- "0..*" Candidate : seating matrix
    SeatingArrangementEngine ..> Candidate : allocates
    SeatingArrangementEngine ..> ExamHall : fills
    SeatingArrangementEngine ..> AllocationResult : returns
    AllocationResult o-- "*" Candidate : unallocated list
    HallTicketValidator --> CandidateRegistry : looks up candidate
    HallTicketValidator ..> Candidate : returns
    HallTicketValidator ..> InvalidHallTicketException : throws
    HallTicketValidator ..> WrongExamCenterException : throws
    ExaminationSession --> ExamHall : has one hall
    ExaminationSession ..> Candidate : enrols / admits
    ExaminationSession ..> ExamAudit : delegates audit
    ExaminationSession ..> AdmissionResult : returns
    InvalidHallTicketException --|> Exception
    WrongExamCenterException --|> Exception
    Demonstration ..> CandidateRegistry
    Demonstration ..> ExaminationSession
    Demonstration ..> SeatingArrangementEngine
    Demonstration ..> HallTicketValidator
    TestRunner ..> Candidate
    TestRunner ..> ExaminationSession
    TestRunner ..> SeatingArrangementEngine
```

## Relationship notes

- `CandidateRegistry` stores a list of `Candidate` objects.
- `ExamHall` stores candidates in its two-dimensional seating matrix.
- `SeatingArrangementEngine` operates on candidates and an `ExamHall`, then returns an `AllocationResult`.
- `HallTicketValidator` depends on `CandidateRegistry` and returns the registered `Candidate`; it can throw the two custom checked exceptions.
- `ExaminationSession` has an `ExamHall` and delegates registration/admission accounting to static methods in `ExamAudit`.
- Both custom exceptions inherit from Java's `Exception`.
- `AdmissionResult` is an enum, not an exception.
- There are no user-defined interfaces in the source.
