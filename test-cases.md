# Test Cases

## Test execution

The project includes a dependency-free test runner at `test/TestRunner.java`.

Compile and execute it from the repository root:

```bash
mkdir -p out
javac -d out src/*.java test/TestRunner.java
java -cp out TestRunner
```

On Windows Command Prompt:

```bat
if not exist out mkdir out
javac -d out src\*.java test\TestRunner.java
java -cp out TestRunner
```

The test runner prints an individual PASS/FAIL line for each assertion and exits with status code 1 if one or more checks fail.

## Automated test inventory

The following expected behaviours are implemented in `TestRunner.java`:

| Test | Scenario | Expected behaviour |
|---|---|---|
| T01 | Register one candidate | Registration succeeds and case-insensitive USN lookup works. |
| T02 | Register the same USN twice | Duplicate is rejected; registry size remains one. |
| T03 | Allocate 10 CSE and 10 ECE candidates in a 5×6 hall | All 20 are seated; none are unallocated. |
| T04 | Check horizontal neighbours | No same-department horizontal neighbours. |
| T05 | Check vertical neighbours | No same-department vertical neighbours; constraint is satisfied. |
| T06a | Allocate 12 CSE and 10 ECE candidates | All 22 are seated while preserving the rule. |
| T06b | Allocate 20 CSE and 5 ECE candidates with fallback | All 25 are seated; fallback is used and any rule violation is reported. |
| T06c | Same 20/5 mix in strict mode | Rule remains satisfied; five candidates are reported unallocated. |
| T07 | Allocate an empty candidate list | No crash; no candidates seated; empty desks are shown. |
| T08 | Allocate 40 candidates into 30 desks | 30 seated and 10 reported unallocated; no candidate silently lost. |
| T09 | Validate correct credential at correct centre | Authoritative registered candidate is returned. |
| T10 | Wrong credential | `InvalidHallTicketException` with reason `HASH_MISMATCH`. |
| T10b | Check wrong-credential error message | Stored hash is not exposed in the message. |
| T10c | Empty/malformed credential | `InvalidHallTicketException` with reason `MALFORMED_CREDENTIAL`. |
| T11 | Validate unregistered USN | `InvalidHallTicketException` with reason `NOT_REGISTERED`. |
| T13 | Valid candidate requests wrong centre | `WrongExamCenterException` is thrown. |
| T13b | Bad credential and wrong centre together | Invalid-ticket failure is reported before centre mismatch. |
| T14 | Admit the same candidate twice | Second admission returns `ALREADY_ADMITTED`; count remains one. |
| T14b | Enrol same candidate twice | Registration count is not inflated. |
| T14c | Admit candidate without a desk | Returns `NO_SEAT_ASSIGNED`. |
| T15 | Finalise session with five registered and two admitted | Absentees equal three; repeated finalisation does not double-count. |
| T15b | Admit after finalisation | Returns `SESSION_CLOSED`. |
| T16 | Parse invalid menu input | Invalid values return -1; a valid trimmed menu choice is accepted. |
| T17 | Allocate the same candidate list again | Each candidate remains represented once and hall occupancy remains correct. |
| T18 | Use one candidate in two sessions | Per-session counts remain independent and aggregate totals equal session contributions. |
| T18b | Reset one session | Only that session's contribution is removed from aggregate totals. |

## Verified result in the packaging environment

The original source files were compiled with the available JDK and the existing test runner was executed before packaging:

```text
Passed: 26  Failed: 0
```

This is the result of the test run performed while preparing this repository. Re-run the commands above after downloading or cloning the repository to verify your local environment.

## Manual menu checks documented by the original project

The original `test-cases.md` also documents a manual menu session that exercised invalid menu input, empty-hall display, unknown USN lookup, candidate registration, duplicate registration rejection, seating, wrong-ticket rejection, valid admission, duplicate admission blocking, seat lookup, attendance report, and session finalisation.

## Not tested / not supported

- Interactive hidden-password input is not covered by the automated runner because it requires a real console.
- Very large halls beyond the dimensions rejected by the source are not tested.
- Concurrent use is not tested and is not supported; the application is single-threaded.
