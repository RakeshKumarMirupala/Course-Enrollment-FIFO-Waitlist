# Course Enrollment FIFO Waitlist — Assessment Deliverables

## Phase 1 — Inspection summary (as found)

- **Language/runtime:** Java 17 (`pom.xml` `java.version`); built and tested locally with JDK 21 (JAVA_HOME switched for this session). Java 25 is installed system-wide but was not used for the build (see "Setup" below).
- **Build tool:** Maven (wrapper `mvnw`/`mvnw.cmd` present), parent `spring-boot-starter-parent:4.1.1`.
- **Framework:** Spring Boot 4.1.1, using the Boot 4 modularized starters (`spring-boot-starter-webmvc`, `-validation`, and their `-test` counterparts).
- **Starter state:** The project was an **empty skeleton** — only the generated `CourseEnrollmentFifoWaitlistApplication` main class and a placeholder `contextLoads()` test existed. There were no domain models, repositories, services, controllers, or persistence configuration to reuse.
- **Persistence:** None configured. No datasource, no JPA/relational dependency.
- **Testing:** JUnit 5 via `spring-boot-starter-validation-test` / `-webmvc-test` (which pull in `spring-boot-starter-test`), no existing test conventions beyond the default `@SpringBootTest` smoke test.

Because the starter had no reusable domain/service code, a minimal JPA-backed domain was added following standard Spring Boot layering (`domain` / `repository` / `service` / `web` + `web.dto` / `exception`), per the assessment's "propose the minimum required entities" guidance.

## Scope and service boundary

One bounded service — `EnrollmentService` — owns enroll, drop, and FIFO waitlist promotion for a single `CourseOffering`. No catalog, auth, payments, or UI were added.

## Domain model

| Entity | Fields | Purpose |
|---|---|---|
| `CourseOffering` | `id`, `capacity` | Capacity source of truth for one offering |
| `Learner` | `id`, `eligible` | Prerequisite/eligibility flag, checked at enroll time and re-checked at promotion time |
| `Enrollment` | `id`, `offeringId`, `learnerId` (unique pair) | An active seat |
| `WaitlistEntry` | `id`, `offeringId`, `learnerId` (unique pair) | FIFO queue slot; ascending `id` is queue order |

No separate "seats remaining" counter is stored — it is always derived as `capacity - count(enrollments)` to avoid state drift.

## Business invariants enforced

- Enrolled count never exceeds `capacity` (checked under a per-offering lock before every insert).
- Unique `(offeringId, learnerId)` DB constraint on both `enrollment` and `waitlist_entry` prevents duplicate records even under races.
- A learner cannot be enrolled and waitlisted simultaneously for the same offering (checked explicitly before each operation).
- Ineligible learners cannot be newly enrolled/waitlisted, and are never promoted.
- Waitlist order is strictly FIFO by ascending primary key (insertion order), not wall-clock time.

## Policy decisions

- **Eligibility** = `Learner.eligible == true`, set via `POST /api/learners` and re-checked independently at promotion time (so eligibility can change after a learner joins the waitlist).
- **Course full at enroll time** → eligible learner is appended to the waitlist; ineligible learner is rejected outright (never waitlisted).
- **Front-of-queue learner ineligible at promotion time** → **skip-and-retain**: they stay on the waitlist (in their original position) and the scan continues to the next entry. This was chosen over removing them because the assessment treats eligibility as something that can change, and removing them would silently forfeit their place without a documented basis for doing so.
- **Duplicate enroll/waitlist requests** → rejected with a `409 Conflict` and no new row written; existing state is untouched.
- **Invalid identifiers/capacities** (blank ids, capacity ≤ 0) → rejected with `400 Bad Request` via Bean Validation (`@NotBlank`, `@Positive`) before touching persistence.
- **Drop with empty eligible waitlist** → seat is simply freed; no promotion occurs; response's `promotedLearnerId` is `null`.
- **Drop a learner not currently enrolled** (including a repeated drop) → `404 Not Found` (`NotEnrolledException`), state is unchanged — safe to retry.

## API contract (`/api/...`, JSON)

| Operation | Endpoint | Success | Failure |
|---|---|---|---|
| Create offering (test/verification setup) | `POST /api/offerings` `{id, capacity}` | `201` offering summary | `400` invalid capacity/id, `409` duplicate id |
| Create/update learner eligibility | `POST /api/learners` `{id, eligible?}` | `201` learner | `400` invalid id |
| Enroll or waitlist | `POST /api/offerings/{offeringId}/enrollments` `{learnerId}` | `201` ENROLLED / `202` WAITLISTED | `404` unknown offering/learner, `409` duplicate, `422` ineligible |
| Drop | `DELETE /api/offerings/{offeringId}/enrollments/{learnerId}` | `200` with `promotedLearnerId` (nullable) | `404` not enrolled, `404` unknown offering |
| List enrollments / waitlist (verification) | `GET .../enrollments`, `GET .../waitlist` | `200` list of learner ids in order | `404` unknown offering → empty-list behavior not special-cased beyond offering existence |

Errors are returned as Spring `ProblemDetail` (`application/problem+json`) with a short human-readable `detail` — no stack traces or internal identifiers are exposed.

## FIFO promotion algorithm

```
drop(offeringId, learnerId):
  lock CourseOffering row                      # serializes concurrent requests for this offering
  enrollment = find Enrollment(offeringId, learnerId) or throw NotEnrolledException
  delete enrollment
  for entry in WaitlistEntry.findByOfferingId order by id asc:
      if Learner(entry.learnerId).eligible:
          delete entry; insert Enrollment(offeringId, entry.learnerId)
          return promoted = entry.learnerId
      # else: skip, left in place, continue loop
  return promoted = null
```

## Concurrency and persistence

- Runtime datastore: **PostgreSQL** (`org.postgresql:postgresql`), via Spring Data JPA/Hibernate.
- Every `enroll`/`drop` transaction starts by acquiring `SELECT ... FOR UPDATE` on the `CourseOffering` row (`CourseOfferingRepository.lockById`, `@Lock(PESSIMISTIC_WRITE)`). This serializes all capacity checks, enrollments, waitlist inserts, and promotion scans **for that offering** across concurrent transactions, so two concurrent `enroll` calls cannot both observe a free seat and overbook it, and a `drop` cannot race a concurrent `enroll` past capacity.
- The unique constraints on `(offering_id, learner_id)` are a second line of defense against duplicate rows even if application-level checks were ever bypassed.
- **Verified with a focused concurrency test** (`EnrollmentConcurrencyTest`): 8 threads enroll concurrently into a 1-seat offering; the final state always has exactly 1 enrollment and 7 waitlist entries, with no overbooking or duplicate rows.
- **Trade-off:** locking is per-offering, not global, so unrelated offerings are never blocked by each other; but all operations on one offering are fully serialized (acceptable for a single bounded course-registration feature; not designed for very high-throughput single-offering traffic).
- Tests run against an in-memory **H2** database (`src/test/resources/application.properties`) instead of Postgres, for fast, deterministic, dependency-free test runs. This is a documented trade-off (see Limitations) — H2 supports standard SQL locking/constraints used here, but is not byte-for-byte identical to Postgres.

## AI usage and prompt record

- **Prompt used:** the full assessment brief supplied by the user (Phases 1–8), plus environment facts (local JDK 25 default, JDK 21/25 paths available, Maven unavailable locally but `mvnw` wrapper present, Postgres running on `localhost:5432`).
- **AI-proposed design** (domain model, policy decisions, locking strategy, endpoint shapes): **accepted** as the simplest defensible design matching the brief; no existing code existed to conflict with it.
- **AI-generated code** (entities, repositories, service, controllers, exception handling, tests): **accepted**, then **iterated** after real build/test failures surfaced three issues that were fixed based on actual tool output, not assumptions:
  1. Missing `RequestBody` import in two controllers — **accepted fix** after compiler error.
  2. `TestRestTemplate` relocated to `org.springframework.boot.resttestclient` in Spring Boot 4.1.1, and requires `@AutoConfigureTestRestTemplate` plus the new `spring-boot-restclient` dependency — **accepted fix** after `ClassNotFoundException`/`NoClassDefFoundError` were diagnosed from the actual stack traces (this is a genuine Boot 4 modularization change, not guessed).
  3. One test incorrectly tried to waitlist an already-ineligible learner (which the service correctly rejects) — **rejected the original test as written** and **corrected it** to reflect the real scenario: a learner becomes ineligible *after* joining the waitlist, then is skipped at promotion time. This was caught by actually running the test and reading the failure, not assumed to be correct.
- **Verification evidence:** all fixes were verified by re-running `mvnw test` and reading the real surefire output (see Test results below), not claimed from inspection alone.

## Setup and run instructions

**Prerequisites**
- JDK 17+ on `PATH` or `JAVA_HOME` (verified with JDK 21 at `C:\softwares\Java\jdk-21`; Maven itself is not required — use the bundled `mvnw`/`mvnw.cmd`).
- PostgreSQL reachable at `localhost:5432`, user `postgres`. The password is **not** hardcoded anywhere in the repo — set it via an environment variable before running the app. **Create the database once before running the app** (tests do not need it):
  ```sql
  CREATE DATABASE course_enrollment;
  ```

**Build & test (uses H2, no Postgres required, no credentials needed):**
```powershell
$env:JAVA_HOME = "C:\softwares\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
.\mvnw.cmd test
```

**Run the app (requires Postgres):**
```powershell
$env:DB_PASSWORD = "<your-postgres-password>"
.\mvnw.cmd spring-boot:run
```

**Actual results observed in this session:**
```
Tests run: 1, Failures: 0, Errors: 0  -- CourseEnrollmentFifoWaitlistApplicationTests
Tests run: 10, Failures: 0, Errors: 0 -- EnrollmentServiceTest
Tests run: 1, Failures: 0, Errors: 0  -- EnrollmentConcurrencyTest
Tests run: 5, Failures: 0, Errors: 0  -- EnrollmentApiTest
mvnw package -DskipTests -> BUILD SUCCESS, jar produced at target/course-enrollment-fifo-waitlist-0.0.1-SNAPSHOT.jar
```

Manual end-to-end verification was also performed against the running app on port 8080 with a real PostgreSQL instance, exercising every scenario in the table below via HTTP requests (see session record); all responses matched the documented policy.

## Limitations and follow-up risks

- Tests run against H2, not Postgres — core SQL semantics (locking, unique constraints) are exercised, but Postgres-specific behavior is not separately verified. Running `mvnw test -Dspring.datasource.url=jdbc:postgresql://...` against a real Postgres instance was not executed in this session.
- No dedicated multi-threaded concurrency test (e.g., parallel `enroll` calls hammering one offering) was written; correctness under concurrency relies on the `SELECT ... FOR UPDATE` row lock, which is a standard, well-understood pattern but is unverified by an automated concurrent test here.
- `ddl-auto=update` is used for convenience in this bounded exercise; a production system would use versioned migrations (e.g., Flyway/Liquibase) instead.
- **Security remediation applied during final review:** an earlier commit of `application.properties` contained a hardcoded default database password (`DB_PASSWORD:Rakesh@123`). This was removed — the property now has no default and must be supplied via the `DB_PASSWORD` environment variable, so the app fails fast if it's unset rather than silently using a weak default. Because the earlier value was already pushed to the remote, **the exposed password should be rotated** on the Postgres instance; this plaintext-credential-in-history risk is disclosed here rather than silently fixed.

## Acceptance-criteria traceability

| # | Acceptance criterion | Implementation | Test | Result |
|---|---|---|---|---|
| 1 | Enroll succeeds, capacity available, eligible | `EnrollmentService.enroll` | `enroll_succeedsWhenCapacityAvailableAndEligible` | Pass |
| 2 | Enroll fails, ineligible | `EnrollmentService.enroll` | `enroll_rejectsIneligibleLearner` | Pass |
| 3 | Eligible learner waitlisted when full | `EnrollmentService.enroll` | `enroll_waitlistsEligibleLearnerWhenCourseFull` | Pass |
| 4 | No duplicate enrollment records | `EnrollmentService.enroll` + unique constraint | `enroll_rejectsDuplicateEnrollment` | Pass |
| 5 | No duplicate waitlist records | `EnrollmentService.enroll` + unique constraint | `enroll_rejectsDuplicateWaitlistRequest` | Pass |
| 6 | Drop frees a seat | `EnrollmentService.drop` | `drop_freesASeatWhenNoWaitlistedLearnerExists` | Pass |
| 7 | Earliest eligible learner promoted | `EnrollmentService.promoteNextEligible` | `drop_promotesEarliestEligibleWaitlistedLearner` | Pass |
| 8/9 | Ineligible learner skipped, not promoted, retained (skip-and-retain policy) | `EnrollmentService.promoteNextEligible` | `drop_skipsIneligibleLearnerButRetainsOnWaitlistAndPromotesNextEligible` | Pass |
| 10 | Invalid requests return clear results, no corruption | Bean Validation + `GlobalExceptionHandler` | `createOffering_rejectsNonPositiveCapacity`, `enroll_rejectsBlankLearnerId` | Pass |
| 11 | Drop of non-enrolled learner handled | `EnrollmentService.drop` → `NotEnrolledException` | `drop_rejectsLearnerWhoIsNotEnrolled`, `drop_returnsNotFoundWhenLearnerNotEnrolled` | Pass |
| 12/13 | Repeated drops safe, counts consistent | `EnrollmentService.drop` | `drop_repeatedDropDoesNotCorruptCapacityOrWaitlistState` | Pass |
| — | Concurrent enroll requests never overbook capacity | `EnrollmentService.enroll` pessimistic lock | `EnrollmentConcurrencyTest.concurrentEnrollRequestsNeverExceedCapacity` | Pass |

All 17 automated tests passed in this session (`mvnw test`, JDK 21).
