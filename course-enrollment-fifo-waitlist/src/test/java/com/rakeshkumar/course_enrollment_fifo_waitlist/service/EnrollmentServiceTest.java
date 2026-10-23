package com.rakeshkumar.course_enrollment_fifo_waitlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.CourseOffering;
import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.Learner;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.DuplicateEnrollmentException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.DuplicateWaitlistException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.IneligibleLearnerException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.NotEnrolledException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.CourseOfferingRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.EnrollmentRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.LearnerRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.WaitlistEntryRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.DropResponse;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.EnrollmentResponse;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.EnrollmentStatus;

/**
 * Exercises {@link EnrollmentService} directly against a real (H2) relational database so
 * uniqueness constraints and the capacity/FIFO logic are verified together.
 */
@SpringBootTest
class EnrollmentServiceTest {

    @Autowired
    private EnrollmentService enrollmentService;
    @Autowired
    private CourseOfferingRepository courseOfferingRepository;
    @Autowired
    private LearnerRepository learnerRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

    @BeforeEach
    void cleanDatabase() {
        waitlistEntryRepository.deleteAll();
        enrollmentRepository.deleteAll();
        learnerRepository.deleteAll();
        courseOfferingRepository.deleteAll();
    }

    private void givenOffering(String offeringId, int capacity) {
        courseOfferingRepository.save(new CourseOffering(offeringId, capacity));
    }

    private void givenLearner(String learnerId, boolean eligible) {
        learnerRepository.save(new Learner(learnerId, eligible));
    }

    // 1. Enrollment succeeds when capacity is available and prerequisites are satisfied.
    @Test
    void enroll_succeedsWhenCapacityAvailableAndEligible() {
        givenOffering("CS101", 2);
        givenLearner("L1", true);

        EnrollmentResponse response = enrollmentService.enroll("CS101", "L1");

        assertThat(response.status()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(enrollmentRepository.countByOfferingId("CS101")).isEqualTo(1);
    }

    // 2. Enrollment fails appropriately when prerequisites are not satisfied.
    @Test
    void enroll_rejectsIneligibleLearner() {
        givenOffering("CS101", 2);
        givenLearner("L1", false);

        assertThatThrownBy(() -> enrollmentService.enroll("CS101", "L1"))
                .isInstanceOf(IneligibleLearnerException.class);
        assertThat(enrollmentRepository.countByOfferingId("CS101")).isZero();
        assertThat(waitlistEntryRepository.countByOfferingId("CS101")).isZero();
    }

    // 3. An eligible learner joins the waitlist when the course is full.
    @Test
    void enroll_waitlistsEligibleLearnerWhenCourseFull() {
        givenOffering("CS101", 1);
        givenLearner("L1", true);
        givenLearner("L2", true);
        enrollmentService.enroll("CS101", "L1");

        EnrollmentResponse response = enrollmentService.enroll("CS101", "L2");

        assertThat(response.status()).isEqualTo(EnrollmentStatus.WAITLISTED);
        assertThat(response.waitlistPosition()).isEqualTo(1L);
        assertThat(enrollmentRepository.countByOfferingId("CS101")).isEqualTo(1);
    }

    // 4. Duplicate enrollment requests do not create duplicate records.
    @Test
    void enroll_rejectsDuplicateEnrollment() {
        givenOffering("CS101", 2);
        givenLearner("L1", true);
        enrollmentService.enroll("CS101", "L1");

        assertThatThrownBy(() -> enrollmentService.enroll("CS101", "L1"))
                .isInstanceOf(DuplicateEnrollmentException.class);
        assertThat(enrollmentRepository.countByOfferingId("CS101")).isEqualTo(1);
    }

    // 5. Duplicate waitlist requests do not create duplicate entries.
    @Test
    void enroll_rejectsDuplicateWaitlistRequest() {
        givenOffering("CS101", 1);
        givenLearner("L1", true);
        givenLearner("L2", true);
        enrollmentService.enroll("CS101", "L1");
        enrollmentService.enroll("CS101", "L2");

        assertThatThrownBy(() -> enrollmentService.enroll("CS101", "L2"))
                .isInstanceOf(DuplicateWaitlistException.class);
        assertThat(waitlistEntryRepository.countByOfferingId("CS101")).isEqualTo(1);
    }

    // 6. Dropping an enrolled learner frees a seat.
    @Test
    void drop_freesASeatWhenNoWaitlistedLearnerExists() {
        givenOffering("CS101", 1);
        givenLearner("L1", true);
        enrollmentService.enroll("CS101", "L1");

        DropResponse response = enrollmentService.drop("CS101", "L1");

        assertThat(response.promotedLearnerId()).isNull();
        assertThat(enrollmentRepository.countByOfferingId("CS101")).isZero();
    }

    // 7. The earliest eligible learner is promoted.
    @Test
    void drop_promotesEarliestEligibleWaitlistedLearner() {
        givenOffering("CS101", 1);
        givenLearner("L1", true);
        givenLearner("L2", true);
        givenLearner("L3", true);
        enrollmentService.enroll("CS101", "L1");
        enrollmentService.enroll("CS101", "L2");
        enrollmentService.enroll("CS101", "L3");

        DropResponse response = enrollmentService.drop("CS101", "L1");

        assertThat(response.promotedLearnerId()).isEqualTo("L2");
        assertThat(enrollmentRepository.existsByOfferingIdAndLearnerId("CS101", "L2")).isTrue();
        assertThat(waitlistEntryRepository.existsByOfferingIdAndLearnerId("CS101", "L3")).isTrue();
    }

    // 8 & 9. An ineligible learner is skipped (not promoted) but retained on the waitlist.
    // A waitlisted learner can become ineligible after joining (eligibility is re-checked at promotion time).
    @Test
    void drop_skipsIneligibleLearnerButRetainsOnWaitlistAndPromotesNextEligible() {
        givenOffering("CS101", 1);
        givenLearner("L1", true);
        givenLearner("L2", true);
        givenLearner("L3", true);
        enrollmentService.enroll("CS101", "L1");
        enrollmentService.enroll("CS101", "L2");
        enrollmentService.enroll("CS101", "L3");
        givenLearner("L2", false);

        DropResponse response = enrollmentService.drop("CS101", "L1");

        assertThat(response.promotedLearnerId()).isEqualTo("L3");
        assertThat(waitlistEntryRepository.existsByOfferingIdAndLearnerId("CS101", "L2")).isTrue();
        assertThat(enrollmentRepository.existsByOfferingIdAndLearnerId("CS101", "L2")).isFalse();
    }

    // 11. Dropping a learner who is not enrolled is handled correctly.
    @Test
    void drop_rejectsLearnerWhoIsNotEnrolled() {
        givenOffering("CS101", 1);
        givenLearner("L1", true);

        assertThatThrownBy(() -> enrollmentService.drop("CS101", "L1"))
                .isInstanceOf(NotEnrolledException.class);
    }

    // 12 & 13. Repeated drops do not corrupt state; counts stay consistent.
    @Test
    void drop_repeatedDropDoesNotCorruptCapacityOrWaitlistState() {
        givenOffering("CS101", 1);
        givenLearner("L1", true);
        enrollmentService.enroll("CS101", "L1");
        enrollmentService.drop("CS101", "L1");

        assertThatThrownBy(() -> enrollmentService.drop("CS101", "L1"))
                .isInstanceOf(NotEnrolledException.class);
        assertThat(enrollmentRepository.countByOfferingId("CS101")).isZero();
        assertThat(waitlistEntryRepository.countByOfferingId("CS101")).isZero();
    }
}
