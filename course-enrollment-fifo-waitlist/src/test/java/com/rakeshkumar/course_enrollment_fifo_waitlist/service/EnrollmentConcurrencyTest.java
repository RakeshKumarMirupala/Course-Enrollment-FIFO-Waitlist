package com.rakeshkumar.course_enrollment_fifo_waitlist.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.CourseOffering;
import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.Learner;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.CourseOfferingRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.EnrollmentRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.LearnerRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.WaitlistEntryRepository;

/**
 * Verifies that the per-offering row lock in {@link EnrollmentService#enroll} prevents
 * overbooking when multiple learners race to enroll in a single-seat offering at the same time.
 */
@SpringBootTest
class EnrollmentConcurrencyTest {

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

    @Test
    void concurrentEnrollRequestsNeverExceedCapacity() throws InterruptedException {
        int capacity = 1;
        int learnerCount = 8;
        courseOfferingRepository.save(new CourseOffering("CS-CONCURRENT", capacity));
        for (int i = 0; i < learnerCount; i++) {
            learnerRepository.save(new Learner("CONC-L" + i, true));
        }

        CountDownLatch startLine = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(learnerCount);
        try {
            List<Future<?>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < learnerCount; i++) {
                String learnerId = "CONC-L" + i;
                futures.add(pool.submit(() -> {
                    startLine.await();
                    enrollmentService.enroll("CS-CONCURRENT", learnerId);
                    return null;
                }));
            }
            startLine.countDown();
            for (Future<?> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (Exception ignoredDuplicateOrRaceRejection) {
                    // Some requests are expected to be rejected by the service's own invariants;
                    // the assertions below verify final state regardless of per-request outcome.
                }
            }
        } finally {
            pool.shutdown();
        }

        assertThat(enrollmentRepository.countByOfferingId("CS-CONCURRENT")).isEqualTo(capacity);
        assertThat(waitlistEntryRepository.countByOfferingId("CS-CONCURRENT")).isEqualTo(learnerCount - capacity);
    }
}
